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

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.json.JSONException;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.repository.EclipseExportI18NHelper;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableDefinitionUtils;
import com.servoy.eclipse.ui.wizards.EclipseExportUserChannel;
import com.servoy.eclipse.ui.wizards.IExportSolutionModel;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.xmlxport.IMetadataDefManager;
import com.servoy.j2db.util.xmlxport.ITableDefinitionsManager;
import com.servoy.j2db.util.xmlxport.IXMLExporter;

/**
 * @author jcompagner
 */
final public class ExportSolutionJob extends WorkspaceJob
{
	private final IExportSolutionModel exportModel;
	private final boolean dbDown;
	private final IFileAccess workspace;
	private final Solution activeSolution;
	private final boolean exportSolution;

	/**
	 * @param name
	 * @param exportModel
	 */
	public ExportSolutionJob(String name, IExportSolutionModel exportModel, Solution activeSolution, boolean dbDown, boolean exportSolution,
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

		AbstractRepository rep = (AbstractRepository)ServoyModel.getDeveloperRepository();

		final IApplicationServerSingleton as = ApplicationServerRegistry.get();
		IUserManager sm = as.getUserManager();
		EclipseExportUserChannel eeuc = new EclipseExportUserChannel(exportModel, monitor);
		EclipseExportI18NHelper eeI18NHelper = new EclipseExportI18NHelper(workspace);
		IXMLExporter exporter = as.createXMLExporter(rep, sm, eeuc, Settings.getInstance(), as.getDataServer(), as.getClientId(), eeI18NHelper);

		try
		{
			ITableDefinitionsManager tableDefManager = null;
			IMetadataDefManager metadataDefManager = null;
			if (dbDown || exportModel.isExportUsingDbiFileInfoOnly())
			{
				Pair<ITableDefinitionsManager, IMetadataDefManager> defManagers = TableDefinitionUtils.getTableDefinitionsFromDBI(activeSolution,
					exportModel.isExportReferencedModules(), exportModel.isExportI18NData(), exportModel.isExportAllTablesFromReferencedServers(),
					exportModel.isExportMetaData());
				if (defManagers != null)
				{
					tableDefManager = defManagers.getLeft();
					metadataDefManager = defManagers.getRight();
				}
			}
			final String[] warningMessage = new String[] { "" };
			if (exportModel.isExportSampleData() && dbDown)
			{
				warningMessage[0] = "Skipping sample data export, solution or module has non accesible database.";
			}
			if (exportModel.isExportMetaData() && dbDown)
			{
				warningMessage[0] = warningMessage[0] + "\nSkipping metadata export, solution or module has non accesible database.";
			}
			if (!"".equals(warningMessage[0]))
			{
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						MessageDialog.openWarning(Display.getDefault().getActiveShell(), "Export solution from database files (database server not accesible)",
							warningMessage[0]);
					}
				});
			}
			exporter.exportSolutionToFile(activeSolution, new File(exportModel.getFileName()), ClientVersion.getVersion(), ClientVersion.getReleaseNumber(),
				exportModel.isExportMetaData() && !dbDown, exportModel.isExportSampleData() && !dbDown, exportModel.getNumberOfSampleDataExported(),
				exportModel.isExportI18NData(), exportModel.isExportUsers(), exportModel.isExportReferencedModules(), exportModel.isProtectWithPassword(),
				tableDefManager, metadataDefManager, exportSolution);

			monitor.done();

			if (dbDown)
			{
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						MessageDialog.openError(Display.getDefault().getActiveShell(), "Solution exported with errors",
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
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Failed to export the active solution", extraMsg == null ? message
					: (extraMsg + '\n' + message));
			}
		});
	}

}