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

import java.sql.PreparedStatement;
import java.util.HashMap;

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
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.util.ITransactionConnection;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

public class NewPostgresDbAction extends Action
{
	private final SolutionExplorerView viewer;

	/**
	 * Creates a new action for the given solution view.
	 * 
	 * @param sev the solution view to use.
	 */
	public NewPostgresDbAction(SolutionExplorerView sev)
	{
		viewer = sev;

		setText("Create PostgreSQL Database");
		setToolTipText(getText());
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("Server.gif"));
	}

	public boolean setEnabledStatus()
	{
		boolean state = false;
		ServerConfig[] serverConfigs = ServoyModel.getServerManager().getServerConfigs();
		for (ServerConfig sc : serverConfigs)
		{
			if (sc.isEnabled() && sc.isPostgresDriver())
			{
				state = true;
				break;
			}
		}
		setEnabled(state);
		return state;
	}

	@Override
	public void run()
	{
		ServerConfig[] serverConfigs = ServoyModel.getServerManager().getServerConfigs();
		HashMap<String, ServerConfig> serverMap = new HashMap<String, ServerConfig>();
		for (ServerConfig sc : serverConfigs)
		{
			String serverURL = sc.getServerUrl();
			if (serverURL.contains("postgresql"))
			{
				serverURL = serverURL.replaceFirst(".*//", "");
				serverURL = serverURL.replaceFirst("/.*", "");
				serverMap.put(serverURL, sc);
			}
		}

		if (serverMap.isEmpty())
		{
			MessageDialog.openInformation(viewer.getSite().getShell(), "Info", "No existing database connection, at least one is needed to create a new one.");
		}

		Object[] dbSelection = null;

		if (serverMap.size() > 1)
		{
			ElementListSelectionDialog selectDBServerDialog = new ElementListSelectionDialog(viewer.getViewSite().getShell(), new LabelProvider());
			selectDBServerDialog.setElements(serverMap.keySet().toArray());
			selectDBServerDialog.setTitle("Select database server");
			selectDBServerDialog.setMultipleSelection(false);
			int dbSelectRes = selectDBServerDialog.open();
			if (dbSelectRes == Window.OK)
			{
				dbSelection = selectDBServerDialog.getResult();
			}
			else return;
		}
		else
		{
			dbSelection = new String[1];
			dbSelection[0] = serverMap.keySet().toArray()[0];
		}

		final InputDialog nameDialog = new InputDialog(viewer.getViewSite().getShell(), "Create PostgreSQL Database File", "Supply database name", "",
			new IInputValidator()
			{
				public String isValid(String newText)
				{
					boolean valid = IdentDocumentValidator.isJavaIdentifier(newText);
					return valid ? null : (newText.length() == 0 ? "" : "Invalid database name");
				}
			});
		int dbNameRes = nameDialog.open();

		final IServerInternal serverPrototype = (IServerInternal)ServoyModel.getServerManager().getServer(serverMap.get(dbSelection[0]).getServerName());
		if (dbNameRes == Window.OK)
		{
			try
			{
				final String name = nameDialog.getValue();
				WorkspaceJob createDbJob = new WorkspaceJob("Creating PostgreSQL database") //$NON-NLS-1$
				{
					@Override
					public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
					{
						ITransactionConnection connection = null;
						PreparedStatement ps = null;
						monitor.beginTask("Creating PostgreSQL database...", 1);
						try
						{
							connection = serverPrototype.getUnmanagedConnection();
							ps = connection.prepareStatement("CREATE DATABASE \"" + name + "\" WITH ENCODING 'UNICODE';");
							ps.execute();
							ps.close();
							ps = null;
							Display.getDefault().syncExec(new Runnable()
							{
								public void run()
								{
									MessageDialog.openInformation(viewer.getSite().getShell(), "Info", "PostgreSQL database created.");
								}
							});
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
							displayError("Could not create database: " + e.getMessage());
						}
						finally
						{
							Utils.closeConnection(connection);
							Utils.closeStatement(ps);
						}
						try
						{
							ServerConfig origConfig = serverPrototype.getConfig();
							String dbname = null;
							String serverUrl = origConfig.getServerUrl();
							int startIndex = serverUrl.lastIndexOf("/");
							int endIndex = serverUrl.indexOf("?", startIndex);
							if (endIndex == -1) endIndex = serverUrl.length();
							dbname = serverUrl.substring(startIndex + 1, endIndex);
							if (dbname != null) serverUrl = serverUrl.replaceFirst("/" + dbname, "/" + name); //$NON-NLS-1$ //$NON-NLS-2$
							if (serverUrl.equals(origConfig.getServerUrl()))
							{
								// hmm, no replace, fall back to default
								serverUrl = "jdbc:postgresql://localhost/" + name;
							}

							final IServerManagerInternal serverManager = ServoyModel.getServerManager();
							String configName = name;
							for (int i = 1; serverManager.getServerConfig(configName) != null && i < 100; i++)
							{
								configName = name + i;
							}
							serverManager.getServerConfig(name);
							final ServerConfig serverConfig = new ServerConfig(configName, origConfig.getUserName(), origConfig.getPassword(), serverUrl,
								origConfig.getConnectionProperties(), origConfig.getDriver(), origConfig.getCatalog(), origConfig.getSchema(),
								origConfig.getMaxActive(), origConfig.getMaxIdle(), origConfig.getMaxPreparedStatementsIdle(),
								origConfig.getConnectionValidationType(), origConfig.getValidationQuery(), null, true, false, -1, origConfig.getDialectClass());
							Display.getDefault().asyncExec(new Runnable()
							{
								public void run()
								{
									try
									{
										serverManager.saveServerConfig(null, serverConfig);
									}
									catch (Exception e)
									{
										ServoyLog.logError(e);
									}
									EditorUtil.openServerEditor(serverConfig);
								}
							});
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
						return Status.OK_STATUS;
					}
				};

				createDbJob.setRule(ServoyModel.getWorkspace().getRoot());
				createDbJob.setUser(true); // we want the progress to be visible in a dialog, not to stay in the status bar
				createDbJob.schedule();
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	private void displayError(final String message)
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
