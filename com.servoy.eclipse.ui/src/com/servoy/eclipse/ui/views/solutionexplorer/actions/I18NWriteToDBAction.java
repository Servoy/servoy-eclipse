/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.util.ArrayList;
import java.util.TreeMap;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.persistence.I18NUtil;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;

public class I18NWriteToDBAction extends Action
{
	/**
	 * Creates a new open action that uses the given solution view.
	 *
	 * @param sev the solution view to use.
	 */
	public I18NWriteToDBAction()
	{
		setText("Write to DB");
		setToolTipText("Write messages files to the i18n database tables");
	}

	@Override
	public void run()
	{
		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		if (activeProject != null)
		{
			String i18nServer = activeProject.getSolution().getI18nServerName();
			String i18nTable = activeProject.getSolution().getI18nTableName();
			if (i18nServer != null && i18nTable != null)
			{
				MessageDialogWithToggle dlg = MessageDialogWithToggle.open(MessageDialog.CONFIRM, UIUtils.getActiveShell(), "Write I18N to DB",
					"This will insert new and replace existing keys from the workspace into the database.",
					"Delete keys from database that are not in the workspace", false, null, null, SWT.NONE);

				if (dlg.getReturnCode() == Window.OK) writeI18NToDB(activeProject, dlg.getToggleState());
			}
		}
	}

	private void writeI18NToDB(final ServoyProject servoyProject, final boolean deleteNonExistingKeys)
	{
		WorkspaceJob writingI18NJob = new WorkspaceJob("Writing I18N files to database tables")
		{
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				IRepository repository = ApplicationServerRegistry.get().getDeveloperRepository();
				IDataServer dataServer = ApplicationServerRegistry.get().getDataServer();
				String clientID = ApplicationServerRegistry.get().getClientId();
				IFileAccess workspace = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());

				Solution[] modules = servoyProject.getModules();
				Solution[] allSolutions = new Solution[modules.length + 1];
				allSolutions[0] = servoyProject.getSolution();
				System.arraycopy(modules, 0, allSolutions, 1, modules.length);
				ArrayList<String> allI18NDatasources = new ArrayList<String>();
				for (Solution s : allSolutions)
				{
					String i18nDataSource = s.getI18nDataSource();
					if (i18nDataSource != null && allI18NDatasources.indexOf(i18nDataSource) == -1) allI18NDatasources.add(i18nDataSource);
				}
				for (final String i18nDatasource : allI18NDatasources)
				{
					String[] serverTableNames = DataSourceUtils.getDBServernameTablename(i18nDatasource);
					try
					{
						TreeMap<String, I18NUtil.MessageEntry> messages = EclipseMessages.readMessages(serverTableNames[0], serverTableNames[1], workspace);
						I18NUtil.writeMessagesToRepository(serverTableNames[0], serverTableNames[1], repository, dataServer, clientID, messages, false,
							!deleteNonExistingKeys, null, null, null);
					}
					catch (final Exception ex)
					{
						ServoyLog.logError(ex);
						Display.getDefault().syncExec(new Runnable()
						{
							public void run()
							{
								MessageDialog.openError(UIUtils.getActiveShell(), "Error",
									"Cannot write I18N to database : " + i18nDatasource + ".\n" + ex.getMessage());
							}
						});
					}
				}

				return Status.OK_STATUS;
			}
		};
		writingI18NJob.setUser(false);
		writingI18NJob.schedule();
	}

	@Override
	public boolean isEnabled()
	{
		return super.isEnabled() && ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject() != null;
	}
}
