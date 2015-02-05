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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.app.IApplicationContext;
import org.json.JSONException;

import com.servoy.eclipse.exporter.apps.common.AbstractWorkspaceExporter;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseExportI18NHelper;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableDefinitionUtils;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.xmlxport.IMetadataDefManager;
import com.servoy.j2db.util.xmlxport.ITableDefinitionsManager;
import com.servoy.j2db.util.xmlxport.IXMLExporter;

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
		IApplicationServerSingleton as = ApplicationServerRegistry.get();
		AbstractRepository rep = (AbstractRepository)as.getDeveloperRepository();
		IUserManager sm = as.getUserManager();

		EclipseExportI18NHelper eeI18NHelper = new EclipseExportI18NHelper(new WorkspaceFileAccess(ResourcesPlugin.getWorkspace()));
		IXMLExporter exporter = as.createXMLExporter(rep, sm, configuration, Settings.getInstance(), as.getDataServer(), as.getClientId(), eeI18NHelper);
		ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
		Solution solution = activeProject.getSolution();

		if (solution != null)
		{
			ITableDefinitionsManager tableDefManager = null;
			IMetadataDefManager metadataDefManager = null;
			if (configuration.shouldExportUsingDbiFileInfoOnly())
			{
				Pair<ITableDefinitionsManager, IMetadataDefManager> defManagers;
				try
				{
					defManagers = TableDefinitionUtils.getTableDefinitionsFromDBI(solution, configuration.shouldExportModules(),
						configuration.shouldExportI18NData(), configuration.getExportAllTablesFromReferencedServers(), configuration.shouldExportMetaData());
				}
				catch (CoreException e)
				{
					Debug.error(e);
					defManagers = null;
				}
				catch (JSONException e)
				{
					Debug.error(e);
					defManagers = null;
				}
				catch (IOException e)
				{
					Debug.error(e);
					defManagers = null;
				}
				if (defManagers != null)
				{
					tableDefManager = defManagers.getLeft();
					metadataDefManager = defManagers.getRight();
				}
			}

			try
			{
				exporter.exportSolutionToFile(solution, new File(configuration.getExportFileName(solution.getName())), ClientVersion.getVersion(),
					ClientVersion.getReleaseNumber(), configuration.shouldExportMetaData(), configuration.shouldExportSampleData(),
					configuration.getNumberOfSampleDataExported(), configuration.shouldExportI18NData(), configuration.shouldExportUsers(),
					configuration.shouldExportModules(), configuration.shouldProtectWithPassword(), tableDefManager, metadataDefManager, true);
			}
			catch (final RepositoryException e)
			{
				ServoyLog.logError("Failed to export solution.", e);
				outputError("Exception while exporting solution. EXPORT FAILED for this solution. Check workspace log.");
				exitCode = EXIT_EXPORT_FAILED;
			}
		}
		else
		{
			outputError("Solution in project '" + activeProject.getProject().getName() + "' is not valid. EXPORT FAILED for this solution.");
			exitCode = EXIT_EXPORT_FAILED;
		}
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
			outputError("Cannot check for error markes in all listed modules; please make sure all listed modules (-modules ...) are actually modules of the exported solution.");
		}

		// don't check import hooks as those are not build with active solution in developer either!
		ServoyProject[] importHooks = sm.getImportHookModulesOfActiveProject();
		for (ServoyProject p : importHooks)
			moduleNames.remove(p.getProject().getName());

		ServoyProject[] toCheck = new ServoyProject[moduleNames.size() + 1];
		toCheck[0] = activeProject;
		for (int i = moduleNames.size() - 1; i >= 0; i--)
		{
			toCheck[i + 1] = sm.getServoyProject(moduleNames.get(i));
		}

		super.checkProjectMarkers(toCheck, errors, warnings, config);
	}
}
