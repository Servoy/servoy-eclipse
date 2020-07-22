/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.eclipse.exporter.apps.solution;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.wicket.util.string.Strings;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.app.IApplicationContext;

import com.servoy.eclipse.exporter.apps.common.AbstractWorkspaceExporter;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.export.SolutionExporter;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseExportI18NHelper;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableDefinitionUtils;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;

/**
 * Eclipse application that can be used for exporting servoy solutions in .servoy format (that can be used to import solutions afterwards in developer/app. server).
 *
 * @author acostescu
 */
public class WorkspaceExporter extends AbstractWorkspaceExporter<ArgumentChest>
{

	@Override
	protected ArgumentChest createArgumentChest(IApplicationContext context)
	{
		return new ArgumentChest((String[])context.getArguments().get(IApplicationContext.APPLICATION_ARGS));
	}

	@Override
	protected void exportActiveSolution(ArgumentChest configuration)
	{
		IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
		ServoyProject activeProject = servoyModel.getActiveProject();
		Solution solution = activeProject != null ? activeProject.getSolution() : null;
		if (solution == null)
		{
			outputError("Solution in project '" + activeProject.getProject().getName() + "' is not valid. EXPORT FAILED for this solution.");
			return;
		}

		try
		{
			boolean exportVersions = checkExportedSolutionsVersions(configuration, servoyModel);
			checkDBsForDataExport(configuration, solution);
			if (exitCode == EXIT_EXPORT_FAILED) return;

			SolutionExporter.exportSolutionToFile(solution, new File(configuration.getFileName()), configuration,
				new EclipseExportI18NHelper(new WorkspaceFileAccess(ResourcesPlugin.getWorkspace())), configuration,
				null, false, exportVersions, true, new NullProgressMonitor());
		}
		catch (final Exception e)
		{
			ServoyLog.logError("Failed to export solution.", e);
			outputError("Exception while exporting solution. EXPORT FAILED for this solution. Check workspace log.");
			exitCode = EXIT_EXPORT_FAILED;
		}
	}

	protected void checkDBsForDataExport(ArgumentChest configuration, Solution solution) throws RepositoryException
	{
		if (configuration.isExportSampleData() || configuration.isExportMetaData() && configuration.isDBMetaDataExport())
		{
			Set<String> servers = TableDefinitionUtils.getNeededServerTables(solution, configuration.isExportReferencedModules(),
				configuration.isExportI18NData()).keySet();
			for (String serverName : servers)
			{
				IServerInternal server = (IServerInternal)ApplicationServerRegistry.get().getServerManager()
					.getServer(serverName, false, false);
				try
				{
					server.getConnection();
				}
				catch (SQLException e)
				{
					Debug.error(e);
					exitCode = EXIT_EXPORT_FAILED;
					if (configuration.isExportSampleData())
					{
						outputError("Cannot connect to the DB server '" + serverName +
							"' to export the sample data. Make sure it is started, or try again without exporting the sample data.");
					}
					if (configuration.isExportMetaData() && configuration.isDBMetaDataExport())
					{
						outputError("Cannot connect to the DB server '" + serverName +
							"' to export the metadata. Make sure it is started, or try again without exporting the metadata.");
					}
				}
			}
		}
	}

	protected boolean checkExportedSolutionsVersions(ArgumentChest configuration, IServoyModel servoyModel)
	{
		boolean exportVersions = true;
		String[] exportedModules = configuration.getModulesToExport();
		if (exportedModules != null)
		{
			for (String module : exportedModules)
			{
				if (Strings.isEmpty(servoyModel.getServoyProject(module).getSolution().getVersion()))
				{
					if (exportVersions)
					{
						output("#############################################################################");
						output(
							"WARNING! For using the exported file with SERVOY DEVELOPER, please set versions for the following solutions, then re-export.");
						exportVersions = false;
					}
					output("Missing version: " + module);
				}
			}
			if (!exportVersions)
			{
				output("You can set the solution versions in the developer properties view.");
				output("#############################################################################");
			}
		}
		else
		{
			//the exported modules should always contain the main solution name, if it is null then an error occurred
			outputError("Exception while exporting solution. EXPORT FAILED for this solution. Check workspace log.");
			exitCode = EXIT_EXPORT_FAILED;
		}
		return exportVersions;
	}


	@Override
	protected void checkProjectMarkers(ServoyProject[] solutionAndAllModuleProjects, List<IMarker> errors, List<IMarker> warnings, ArgumentChest config)
	{
		// check active solution + modules that are going to be exported only

		IServoyModel sm = ServoyModelFinder.getServoyModel();
		ServoyProject activeProject = sm.getActiveProject();

		List<String> moduleNames = new ArrayList<String>();
		for (ServoyProject p : solutionAndAllModuleProjects)
		{
			if (p != activeProject) moduleNames.add(p.getProject().getName());
		}
		moduleNames = config.getModuleIncludeList(moduleNames);
		if (moduleNames == null)
		{
			// this will make the actual export code fail with exception later on that's why we don't set any 'fail' exitcode here; it will catch/set correct exit code anyway
			moduleNames = new ArrayList<String>();
			outputError(
				"Cannot check for error markes in all listed modules; please make sure all listed modules (-modules ...) are actually modules of the exported solution.");
		}

		ServoyProject[] toCheck = new ServoyProject[moduleNames.size() + 1];
		toCheck[0] = activeProject;
		for (int i = moduleNames.size() - 1; i >= 0; i--)
		{
			toCheck[i + 1] = sm.getServoyProject(moduleNames.get(i));
		}

		super.checkProjectMarkers(toCheck, errors, warnings, config);
	}
}
