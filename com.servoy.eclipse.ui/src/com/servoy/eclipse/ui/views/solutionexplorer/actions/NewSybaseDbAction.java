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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
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
import com.servoy.eclipse.core.util.SerialRule;
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

public class NewSybaseDbAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;

	/**
	 * Creates a new action for the given solution view.
	 * 
	 * @param sev the solution view to use.
	 */
	public NewSybaseDbAction(SolutionExplorerView sev)
	{
		viewer = sev;

		setText("Create Sybase Database");
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
				if (s.getConfig().isSybaseDriver())
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
				final InputDialog nameDialog = new InputDialog(viewer.getViewSite().getShell(), "Create Sybase Database File", "Supply database name", "",
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
						WorkspaceJob createDbJob = new WorkspaceJob("Creating Sybase database") //$NON-NLS-1$
						{
							@Override
							public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
							{
								String path = null;
								ITransactionConnection connection = null;
								PreparedStatement ps = null;
								ResultSet rs = null;
								try
								{
									monitor.beginTask("Find out where Sybase is running", 4);
									//1. find out where ASA is running
									try
									{
										connection = s.getUnmanagedConnection();
										ps = connection.prepareStatement("SELECT DB_PROPERTY ( 'File' );");
										rs = ps.executeQuery();
										rs.next();
										path = rs.getString(1);
										int index = path.lastIndexOf("/");
										if (index < 0)
										{
											index = path.lastIndexOf("\\");
										}
										path = path.substring(0, index + 1);
										path += name + ".db";
										rs.close();
										rs = null;
										ps.close();
										ps = null;
									}
									catch (Exception e)
									{
										ServoyLog.logError(e);
										displayError("Could not get the path of the server database file: " + e.getMessage());
										return Status.OK_STATUS;
									}
									monitor.worked(1);

									monitor.setTaskName("Attempting to create database...");
									//2. create the database
									File databaseFile = new File(path);
									if (!databaseFile.exists())
									{
										try
										{
											ps = connection.prepareStatement("create database '" + path + "' collation 'UTF8'");
											ps.execute();
											ps.close();
											ps = null;
										}
										catch (Exception e)
										{
											ServoyLog.logError(e);
											displayError("Could not create database file: " + e.getMessage());
											return Status.OK_STATUS;
										}
									}
									else
									{
										Display.getDefault().syncExec(new Runnable()
										{
											public void run()
											{
												MessageDialog.openWarning(viewer.getSite().getShell(), "Warning",
													"A database with that name already exists, will not create a new one.");
											}
										});
										return Status.OK_STATUS;
									}
									monitor.worked(1);

									monitor.setTaskName("Attempting to start database...");
									//3. attempt to start the database
									try
									{
										ps = connection.prepareStatement("start database '" + path + "'");
										ps.execute();
										ps.close();
										ps = null;
									}
									catch (Exception e)
									{
										ServoyLog.logError(e);
										displayError("Could not start database: " + e.getMessage());
										return Status.OK_STATUS;
									}
									monitor.worked(1);

									monitor.setTaskName("Trying to modify 'sybase.config'");
									//4. Edit the sybase config file
									BufferedWriter writer = null;
									try
									{
										// path is string of db file, assume config file is in sibling directory sybase_db
										File dbFile = new File(path);
										File databaseDirectory = dbFile.getParentFile();
										File sybaseDirectory = databaseDirectory.getParentFile();
										File sybaseConfig = new File(sybaseDirectory, "sybase_db/sybase.config");
										if (sybaseConfig.exists() && sybaseConfig.canWrite())
										{
											writer = new BufferedWriter(new FileWriter(sybaseConfig, true));
											writer.newLine();
											writer.write(databaseDirectory.getName() + '/' + dbFile.getName());
											writer.close();
										}
									}
									catch (Exception e)
									{
										ServoyLog.logError(e);
										displayError("Could not edit sybase.config file: " + e.getMessage());
									}
									finally
									{
										if (writer != null) writer.close();
									}
									monitor.worked(1);
									monitor.done();
									Display.getDefault().syncExec(new Runnable()
									{
										public void run()
										{
											MessageDialog.openInformation(viewer.getSite().getShell(), "Info",
												"Database successfully created. Sybase and developer restart is required.");
										}
									});

								}
								catch (Exception ex)
								{
									displayError(ex.getMessage());
								}
								finally
								{
									Utils.closeConnection(connection);
									Utils.closeStatement(ps);
									Utils.closeResultSet(rs);
								}
								try
								{
									ServerConfig origConfig = s.getConfig();
									String serverUrl = origConfig.getServerUrl().replaceFirst("ServiceName=[a-zA-Z_]*", "ServiceName=" + name); //$NON-NLS-1$ //$NON-NLS-2$
									if (serverUrl.equals(origConfig.getServerUrl()))
									{
										// hmm, no replace, fall back to default
										serverUrl = "jdbc:sybase:Tds:localhost:2638?ServiceName=" + name + "&CHARSET=utf8"; //$NON-NLS-1$ //$NON-NLS-2$
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

						ISchedulingRule rule = new SerialRule();
						createDbJob.setRule(rule);
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
