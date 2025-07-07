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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.persistence.ServerSettings;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * Base class for postgres/sybase database creation action.
 * @author emera
 */
public abstract class AbstractNewDbAction extends Action
{
	protected final SolutionExplorerView viewer;
	protected String dbType;

	/**
	 * @param viewer
	 */
	public AbstractNewDbAction(String type, SolutionExplorerView viewer)
	{
		super();
		this.viewer = viewer;
		this.dbType = type;
		setText("Create " + dbType + "Database");
		setToolTipText(getText());
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("server.png"));
	}

	@Override
	public void run()
	{
		Map<String, ServerConfig> serverMap = getServerMap();

		if (serverMap.isEmpty())
		{
			MessageDialog.openInformation(viewer.getSite().getShell(), "Info", "No existing database connection, at least one is needed to create a new one.");
			return;
		}

		String dbSelection = getDbServerSelection(serverMap);
		if (dbSelection == null) return;

		final IServerInternal serverPrototype = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(
			serverMap.get(dbSelection).getServerName());
		if (serverPrototype == null)
		{
			UIUtils.reportError("Create new database",
				"Could not find server '" + serverMap.get(dbSelection).getServerName() + "', the server should be enabled and valid");
			return;
		}
		final String name = getDatabaseName();
		if (name != null)
		{
			WorkspaceJob createDbJob = new WorkspaceJob("Creating " + dbType + " database")
			{
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					if (createDatabase(serverPrototype, name, monitor))
					{
						setDefaultConfig(serverPrototype, name);
					}
					return Status.OK_STATUS;
				}
			};
			createDbJob.setRule(ServoyModel.getWorkspace().getRoot());
			createDbJob.setUser(true); // we want the progress to be visible in a dialog, not to stay in the status bar
			createDbJob.schedule();
		}
	}


	/**
	 * Set the default configuration for the created database.
	 * @param serverPrototype
	 * @param name
	 */
	protected abstract void setDefaultConfig(IServerInternal serverPrototype, String name);

	/**
	 * Create a database based on an existing server prototype.
	 * @param serverPrototype
	 * @param name the name of the database to create
	 * @param monitor
	 * @return true if the database was created, false otherwise
	 */
	protected abstract boolean createDatabase(IServerInternal serverPrototype, String name, IProgressMonitor monitor);

	/**
	 * Check if the database is of the dbType. e.g check if postgres or sybase
	 * @param sc
	 * @return
	 */
	protected abstract boolean isDbTypeDriver(ServerConfig sc);

	/**
	 * Get the server url (db specific) from a server config
	 * @param sc
	 * @return
	 */
	protected abstract String getServerUrl(ServerConfig sc);

	public boolean setEnabledStatus()
	{
		boolean state = false;
		ServerConfig[] serverConfigs = ApplicationServerRegistry.get().getServerManager().getServerConfigs();
		for (ServerConfig sc : serverConfigs)
		{
			if (sc.isEnabled() && isDbTypeDriver(sc))
			{
				state = true;
				break;
			}
		}

		setEnabled(state);

		return state;
	}

	protected Map<String, ServerConfig> getServerMap()
	{
		ServerConfig[] serverConfigs = ApplicationServerRegistry.get().getServerManager().getServerConfigs();
		Map<String, ServerConfig> serverMap = new HashMap<>();
		for (ServerConfig sc : serverConfigs)
		{
			String serverURL = getServerUrl(sc);
			if (serverURL != null && !serverURL.equals(""))
			{
				serverMap.put(serverURL, sc);
			}
		}
		return serverMap;
	}

	/**
	 * Open a db server selection dialog if we have more servers of dbType.
	 * @param serverMap
	 * @return
	 */
	private String getDbServerSelection(Map<String, ServerConfig> serverMap)
	{
		String dbSelection = null;
		if (serverMap.size() > 1)
		{
			ElementListSelectionDialog selectDBServerDialog = new ElementListSelectionDialog(viewer.getViewSite().getShell(), new LabelProvider());
			selectDBServerDialog.setElements(serverMap.keySet().toArray());
			selectDBServerDialog.setTitle("Select database server");
			selectDBServerDialog.setMultipleSelection(false);
			int dbSelectRes = selectDBServerDialog.open();
			if (dbSelectRes == Window.OK)
			{
				Object[] selection = selectDBServerDialog.getResult();
				if (selection != null && selection.length > 0)
				{
					dbSelection = (String)selectDBServerDialog.getResult()[0];
				}
			}
		}
		else
		{
			dbSelection = (String)serverMap.keySet().toArray()[0];
		}
		return dbSelection;
	}

	private String getDatabaseName()
	{
		final InputDialog nameDialog = new InputDialog(viewer.getViewSite().getShell(), "Create " + dbType + " Database File", "Supply database name", "",
			new IInputValidator()
			{
				public String isValid(String newText)
				{
					boolean valid = IdentDocumentValidator.isJavaIdentifier(newText);
					return valid ? null : (newText.length() == 0 ? "" : "Invalid database name");
				}
			});
		int dbNameRes = nameDialog.open();

		if (dbNameRes == Window.OK)
		{
			return nameDialog.getValue();
		}
		return null;
	}


	protected void displayError(final String message)
	{
		if (viewer != null)
		{
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					MessageDialog.openError(viewer.getSite().getShell(), "Error", message);
				}
			});
		}
	}

	/**
	 * Try to save the default server config. If no errors occur, open server editor.
	 * @param origConfig
	 * @param serverSettings
	 * @param serverUrl
	 * @param serverManager
	 * @param configName
	 */
	protected void saveAndOpenDefaultConfig(ServerConfig origConfig, ServerSettings serverSettings, String serverUrl,
		IServerManagerInternal serverManager, String configName)
	{
		ServerConfig serverConfig = origConfig.newBuilder()
			.setServerName(configName)
			.setServerUrl(serverUrl)
			.setSchema(null)
			.setDataModelCloneFrom(null)
			.setEnabled(true)
			.setSkipSysTables(false)
			.setIdleTimeout(-1)
			.build();


		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				try
				{
					serverManager.saveServerConfig(null, serverConfig);
					serverManager.saveServerSettings(configName, serverSettings);
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
				EditorUtil.openServerEditor(serverConfig, serverSettings);
			}
		});
	}

	public boolean isVisible()
	{
		return true;
	}

}
