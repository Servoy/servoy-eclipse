/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.ui.export;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.json.JSONException;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.export.SolutionExporter;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseExportI18NHelper;
import com.servoy.eclipse.model.repository.EclipseExportUserChannel;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.wizards.ExportSolutionModel;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
final public class ExportSolutionJob extends WorkspaceJob
{
	private final ExportSolutionModel exportModel;
	private final boolean dbDown;
	private final IFileAccess workspace;
	private final Solution activeSolution;
	private final boolean exportSolution;

	/**
	 * @param name
	 * @param exportModel
	 */
	public ExportSolutionJob(String name, ExportSolutionModel exportModel, Solution activeSolution, boolean dbDown, boolean exportSolution,
		IFileAccess workspace)
	{
		super(name);
		this.exportModel = exportModel;
		this.activeSolution = activeSolution;
		this.dbDown = dbDown;
		this.exportSolution = exportSolution;
		this.workspace = workspace;
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
	{
		int totalDuration = IProgressMonitor.UNKNOWN;
		if (exportModel.getModulesToExport() != null) totalDuration = (int)(1.42 * exportModel.getModulesToExport().length); // make the main export be 70% of the time, leave the rest for sample data
		monitor.beginTask("Exporting solution", totalDuration);

		try
		{
			File exportFile = new File(exportModel.getFileName());
			SolutionExporter.exportSolutionToFile(activeSolution, exportFile, exportModel, new EclipseExportI18NHelper(workspace),
				new EclipseExportUserChannel(exportModel, monitor),
				exportModel.isExportReferencedWebPackages() ? getModulesWebPackages() : null, dbDown, true, exportSolution);

			if (exportModel.isSaveImportSettingsToDisk() && exportModel.useImportSettings())
			{
				Utils.writeTXTFile(new File(exportFile.getParent(), exportFile.getName() + "_import_settings.json"),
					exportModel.getImportSettings().toString());
			}
			monitor.done();

			if (dbDown)
			{
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						MessageDialog.openError(UIUtils.getActiveShell(), "Solution exported with errors",
							"Solution has been exported with errors. This may prevent the solution from functioning well.\nOnly minimal database info has been exported.");
					}
				});
			}

			return Status.OK_STATUS;
		}
		catch (RepositoryException e)
		{
			handleExportException(e, null, monitor);
			return Status.CANCEL_STATUS;
		}
		catch (JSONException jsonex)
		{
			handleExportException(jsonex, "Bad JSON file structure.", monitor);
			return Status.CANCEL_STATUS;
		}
		catch (IOException ioex)
		{
			handleExportException(ioex, "Exception getting files.", monitor);
			return Status.CANCEL_STATUS;
		}

	}


	private Map<String, List<File>> getModulesWebPackages()
	{
		Map<String, List<File>> modulesWebPackages = new HashMap<String, List<File>>();

		ArrayList<String> allModules = new ArrayList<String>();
		allModules.add(activeSolution.getName());
		if (exportModel.isExportReferencedModules() && exportModel.getModulesToExport() != null && exportModel.getModulesToExport().length > 0)
		{
			allModules.addAll(Arrays.asList(exportModel.getModulesToExport()));
		}

		for (String module : allModules)
		{
			ServoyProject moduleProject = ServoyModelFinder.getServoyModel().getServoyProject(module);
			if (moduleProject != null)
			{
				ArrayList<File> webPackages = new ArrayList<File>();
				IFolder ngPackagesFolder = moduleProject.getProject().getFolder(SolutionSerializer.NG_PACKAGES_DIR_NAME);
				if (ngPackagesFolder.exists())
				{
					try
					{
						for (IResource r : ngPackagesFolder.members())
						{
							if (r instanceof IFile)
							{
								webPackages.add(((IFile)r).getLocation().toFile());
							}
						}
					}
					catch (CoreException ex)
					{
						ServoyLog.logError(ex);
					}
				}
				if (webPackages.size() > 0)
				{
					modulesWebPackages.put(module, webPackages);
				}
			}
		}

		return modulesWebPackages;
	}

	private void handleExportException(final Exception ex, final String extraMsg, IProgressMonitor monitor)
	{
		ServoyLog.logError("Failed to export solution. " + (extraMsg == null ? "" : extraMsg), ex);
		monitor.done();
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				// Try to be nice with the user when presenting error message.
				String message;
				if (ex.getCause() != null) message = ex.getCause().getMessage();
				else message = ex.getMessage();
				if (message == null) message = ex.toString();
				MessageDialog.openError(UIUtils.getActiveShell(), "Failed to export the active solution",
					extraMsg == null ? message : (extraMsg + '\n' + message));
			}
		});
	}

}