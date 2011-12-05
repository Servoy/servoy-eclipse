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

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Iterator;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils.YesYesToAllNoNoToAllAsker;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;

/**
 * Creates a new delete table action. It can be used to delete the table given by the selected SimpleUserNode.
 * 
 * @author acostescu
 */
public class DeleteTableAction extends Action implements ISelectionChangedListener
{

	private final Shell shell;
	private IStructuredSelection selection;

	/**
	 * Creates a new delete table action.
	 */
	public DeleteTableAction(Shell shell)
	{
		this.shell = shell;
		setText("Delete table");
		setToolTipText("Delete table");
	}

	@Override
	public void run()
	{
		if (selection != null &&
			MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), getText(), "Are you sure you want to delete?"))
		{
			Job job = new WorkspaceJob("Deleting table(s)")
			{

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					Iterator<SimpleUserNode> it = selection.iterator();
					TableWrapper selectedTable;
					MultiStatus warnings = new MultiStatus(Activator.PLUGIN_ID, 0, "For more information please click 'Details'.", null);
					YesYesToAllNoNoToAllAsker deleteEACAsker = null; // asks if you also want to delete table Events, Aggregations, Calculations from active modules

					monitor.beginTask("Deleting table(s)", selection.size());
					try
					{
						while (it.hasNext())
						{
							selectedTable = (TableWrapper)it.next().getRealObject();
							boolean deleteTable = true;
							try
							{
								ServoyModel sm = ServoyModelManager.getServoyModelManager().getServoyModel();
								IDeveloperRepository repository = ServoyModel.getDeveloperRepository();

								IServer server = repository.getServer(selectedTable.getServerName());
								final ITable table = server == null ? null : server.getTable(selectedTable.getTableName());
								if (server instanceof IServerInternal && table instanceof Table)
								{
									// see if the user also wants to delete the existing aggregations/calculations/tableEvents for this table
									// that exist in the active modules (only ask if such info exists)
									FlattenedSolution flatSolution = sm.getFlattenedSolution();
									if (flatSolution != null)
									{
										Iterator<TableNode> tableNodes = flatSolution.getTableNodes(table);

										if (tableNodes.hasNext())
										{
											if (deleteEACAsker == null)
											{
												deleteEACAsker = new YesYesToAllNoNoToAllAsker(shell, getText());
											}
											deleteEACAsker.setMessage("Table events, aggregattions and/or calculations exist for table '" +
												selectedTable.getTableName() +
												"' in the active solution and/or modules.\nDo you still want to delete the table?");
											// we have tableNode(s)... ask user if these should be deleted as well
											if (deleteEACAsker.userSaidYes())
											{
												while (tableNodes.hasNext())
												{
													deletePersist(tableNodes.next());
												}
											}
											else deleteTable = false;
										}
									}

									if (deleteTable)
									{
										((IServerInternal)server).removeTable((Table)table);

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
									warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Cannot delete table " + table + " from server " + server));
								}
							}
							catch (RemoteException e)
							{
								ServoyLog.logError(e);
								warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Cannot delete table: " + e.getMessage()));
							}
							catch (RepositoryException e)
							{
								ServoyLog.logError(e);
								warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Cannot delete table: " + e.getMessage()));
							}
							catch (SQLException e)
							{
								ServoyLog.logError(e);
								warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Cannot delete table: " + e.getMessage()));
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
			job.setRule(ServoyModel.getWorkspace().getRoot());
			job.setUser(true);
			job.schedule();
		}
	}

	private void deletePersist(IPersist persist)
	{
		IRootObject rootObject = persist.getRootObject();

		if (rootObject instanceof Solution)
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(rootObject.getName());
			EclipseRepository repository = (EclipseRepository)rootObject.getRepository();

			try
			{
				IPersist editingNode = servoyProject.getEditingPersist(persist.getUUID());
				repository.deleteObject(editingNode);
				servoyProject.saveEditingSolutionNodes(new IPersist[] { editingNode }, true);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		// allow multiple selection
		selection = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = true;
		Iterator<SimpleUserNode> it = sel.iterator();
		while (it.hasNext() && state)
		{
			SimpleUserNode node = it.next();
			state = (node.getType() == UserNodeType.TABLE);
		}
		if (state)
		{
			selection = sel;
		}
		setEnabled(state);
	}

}
