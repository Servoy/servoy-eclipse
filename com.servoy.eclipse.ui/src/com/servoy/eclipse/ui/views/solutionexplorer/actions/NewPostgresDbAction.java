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

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.util.ITransactionConnection;
import com.servoy.j2db.util.IdentDocumentValidator;
import com.servoy.j2db.util.Utils;

public class NewPostgresDbAction extends Action implements ISelectionChangedListener
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

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = false;
		if (sel.size() == 1)
		{
			SimpleUserNode node = (SimpleUserNode)sel.getFirstElement();
			if (node.getRealObject() instanceof IServerInternal)
			{
				IServerInternal s = (IServerInternal)node.getRealObject();
				if (s.getConfig().isPostgresDriver())
				{
					state = true;
				}
			}
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node.getRealObject() instanceof IServerInternal)
		{
			try
			{
				final IServerInternal s = (IServerInternal)node.getRealObject();
				final InputDialog nameDialog = new InputDialog(viewer.getViewSite().getShell(), "Create PostgreSQL Database File", "Supply database name", "",
					new IInputValidator()
					{
						public String isValid(String newText)
						{
							boolean valid = IdentDocumentValidator.isJavaIdentifier(newText);
							return valid ? null : (newText.length() == 0 ? "" : "Invalid database name");
						}
					});
				int res = nameDialog.open();
				if (res == Window.OK)
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
									connection = s.getUnmanagedConnection();
									ps = connection.prepareStatement("CREATE DATABASE " + name + " WITH ENCODING 'UNICODE';");
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
									ServerConfig origConfig = s.getConfig();
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

									IServerManagerInternal serverManager = ServoyModel.getServerManager();
									String configName = name;
									for (int i = 1; serverManager.getServerConfig(configName) != null && i < 100; i++)
									{
										configName = name + i;
									}
									serverManager.getServerConfig(name);
									final ServerConfig serverConfig = new ServerConfig(configName, origConfig.getUserName(), origConfig.getPassword(),
										serverUrl, origConfig.getDriver(), origConfig.getCatalog(), origConfig.getSchema(), origConfig.getMaxActive(),
										origConfig.getMaxIdle(), origConfig.getMaxPreparedStatementsIdle(), origConfig.getConnectionValidationType(),
										origConfig.getValidationQuery(), null, true, false);
									serverManager.saveServerConfig(null, serverConfig);
									Display.getDefault().asyncExec(new Runnable()
									{
										public void run()
										{
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
