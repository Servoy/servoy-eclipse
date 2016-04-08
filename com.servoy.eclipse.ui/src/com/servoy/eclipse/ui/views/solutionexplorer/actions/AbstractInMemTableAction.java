/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils.YesYesToAllNoNoToAllAsker;
import com.servoy.eclipse.model.util.IDataSourceWrapper;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.Pair;

/**
 * Base class for rename and delete in mem table actions.
 * @author emera
 */
public abstract class AbstractInMemTableAction extends Action implements ISelectionChangedListener
{
	protected final Shell shell;
	protected Map<IDataSourceWrapper, IServer> selection;
	private final String actionString1;
	private final String actionString2;

	public AbstractInMemTableAction(Shell shell, String actionString1, String actionString2)
	{
		this.shell = shell;
		this.actionString1 = actionString1;
		this.actionString2 = actionString2;
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		setSelection((IStructuredSelection)event.getSelection());
	}

	public void setSelection(IStructuredSelection sel)
	{
		// allow multiple selection
		selection = new HashMap<>();
		boolean state = true;
		Iterator< ? > it = sel.iterator();
		while (it.hasNext() && state)
		{
			Object selected = it.next();
			if (selected instanceof SimpleUserNode)
			{
				SimpleUserNode node = (SimpleUserNode)selected;
				selection.put((IDataSourceWrapper)node.getRealObject(), (IServer)node.parent.getRealObject());
				state = (node.getType() == UserNodeType.INMEMORY_DATASOURCE);
			}
			else if (selected instanceof Pair< ? , ? >)
			{
				Pair<IDataSourceWrapper, IServer> pair = (Pair<IDataSourceWrapper, IServer>)selected;
				selection.put(pair.getLeft(), pair.getRight());
			}
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		if (selection != null && confirm())
		{
			Job job = new WorkspaceJob(actionString2 + " in memory datasource(s)")
			{

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					Iterator<IDataSourceWrapper> it = selection.keySet().iterator();
					IDataSourceWrapper selectedTable;
					final MultiStatus warnings = new MultiStatus(Activator.PLUGIN_ID, 0, "For more information please click 'Details'.", null);

					monitor.beginTask(actionString2 + " table(s)", selection.size());
					try
					{
						while (it.hasNext())
						{
							selectedTable = it.next();
							boolean completeAction = true;
							try
							{
								final IServer server = selection.get(selectedTable);
								final ITable table = server == null ? null : server.getTable(selectedTable.getTableName());
								if (server instanceof IServerInternal)
								{
									ServoyModel sm = ServoyModelManager.getServoyModelManager().getServoyModel();
									// see if the user also wants to delete/rename the existing aggregations/calculations/tableEvents for this table
									// that exist in the active modules (only ask if such info exists)
									FlattenedSolution flatSolution = sm.getFlattenedSolution();
									if (flatSolution != null)
									{
										Iterator<TableNode> tableNodes = flatSolution.getTableNodes(table);

										// first check if there are duplicate definitions
										boolean duplicateDefinitionFound = false;
										boolean definitionFound = false;
										boolean askUser = false;
										while (tableNodes.hasNext())
										{
											if (tableNodes.next().getColumns() != null)
											{
												if (definitionFound) duplicateDefinitionFound = true;
												else definitionFound = true;
											}
											else
											{
												askUser = true;
											}
										}
										if (askUser)
										{
											if (duplicateDefinitionFound)
											{
												duplicateMemTableHandler(server, table, flatSolution);
												completeAction = false;
											}
											else
											{
												YesYesToAllNoNoToAllAsker _EACAsker = new YesYesToAllNoNoToAllAsker(shell, getText());
												_EACAsker.setMessage(
													"Table events, aggregations and/or calculations exist for table '" + selectedTable.getTableName() +
														"' in the active solution and/or modules.\nDo you still want to " + actionString1 + " the table?");
												// we have tableNode(s)... ask user if these should be deleted/renamed as well
												if (!_EACAsker.userSaidYes())
												{
													completeAction = false;
												}
											}

										}
									}

									if (completeAction)
									{
										ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable()
										{

											public void run(IProgressMonitor monitor) throws CoreException
											{
												try
												{
													doAction(server, table);
												}
												catch (SQLException e)
												{
													ServoyLog.logError(e);
													warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
														"Cannot " + actionString1 + " table: " + e.getMessage()));
												}
												catch (RepositoryException e)
												{
													ServoyLog.logError(e);
													warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
														"Cannot " + actionString1 + " table: " + e.getMessage()));
												}
											}
										}, null);

										// EditorUtil.closeEditor(table) needs to be run in an UI thread
										if (Display.getCurrent() != null)
										{
											EditorUtil.closeEditor(table);
										}
										else
										{
											Display.getDefault().asyncExec(new Runnable()
											{
												public void run()
												{
													EditorUtil.closeEditor(table);
												}
											});
										}
									}
								}
								else
								{
									warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
										"Cannot " + actionString1 + " table " + table + " from server " + server));
								}
							}
							catch (RemoteException e)
							{
								ServoyLog.logError(e);
								warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Cannot " + actionString1 + " table: " + e.getMessage()));
							}
							catch (RepositoryException e)
							{
								ServoyLog.logError(e);
								warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Cannot " + actionString1 + " table: " + e.getMessage()));
							}
							monitor.worked(1);
						}
					}
					finally
					{
						monitor.done();
					}
					if (warnings.getChildren().length > 0)
					{
						if (Display.getCurrent() != null)
						{
							ErrorDialog.openError(shell, null, null, warnings);
						}
						else
						{
							final MultiStatus fw = warnings;
							Display.getDefault().asyncExec(new Runnable()
							{
								public void run()
								{
									ErrorDialog.openError(shell, null, null, fw);
								}
							});
						}
					}
					return Status.OK_STATUS;
				}
			};
			job.setUser(true);
			job.schedule();
		}
	}

	protected abstract boolean confirm();

	protected void duplicateMemTableHandler(IServer server, final ITable table, final FlattenedSolution flatSolution)
	{
		final ArrayList<String> userSelection = new ArrayList<>();
		final boolean[] selectedServer = new boolean[1];
		Runnable run = new Runnable()
		{
			@Override
			public void run()
			{
				// duplicate definitions found.
				ListDialog dialog = new ListDialog(shell)
				{
					@Override
					protected int getTableStyle()
					{
						return SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER;
					}
				};
				dialog.setContentProvider(new IStructuredContentProvider()
				{
					@Override
					public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
					{
					}

					@Override
					public void dispose()
					{
					}

					@Override
					public Object[] getElements(Object inputElement)
					{
						if (inputElement instanceof Iterator< ? >)
						{
							ArrayList<String> names = new ArrayList<>();
							Iterator<TableNode> it = (Iterator<TableNode>)inputElement;
							while (it.hasNext())
							{
								TableNode tn = it.next();
								if (tn.getColumns() == null) names.add(tn.getRootObject().getName());
							}
							Collections.sort(names);
							return names.toArray();
						}
						return null;
					}
				});
				dialog.setLabelProvider(new LabelProvider());
				dialog.setTitle("In memory datasource has events or calculations in other solution(s)");
				dialog.setMessage("Select the solution that will also be cleaned up for this in mem datasource (tablesnodes are " + actionString1 + "d)");
				try
				{
					dialog.setInput(flatSolution.getTableNodes(table));
					if (dialog.open() == Window.OK)
					{
						selectedServer[0] = true;
						Object[] result = dialog.getResult();
						for (Object element : result)
						{
							userSelection.add((String)element);
						}
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		};
		if (Display.getCurrent() != null)
		{
			run.run();
		}
		else
		{
			Display.getDefault().syncExec(run);
		}
		if (selectedServer[0])
		{
			try
			{
				doAction(server, table, userSelection);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	protected abstract void doAction(final IServer server, final ITable table) throws SQLException, RepositoryException;

	protected abstract void doAction(IServer server, ITable table, ArrayList<String> userSelection) throws RepositoryException;

}
