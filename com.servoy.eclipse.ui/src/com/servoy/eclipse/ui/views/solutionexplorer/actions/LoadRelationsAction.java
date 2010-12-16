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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.SerialRule;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.eclipse.ui.wizards.LoadRelationsWizard;
import com.servoy.eclipse.ui.wizards.LoadRelationsWizard.RelationData;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

public class LoadRelationsAction extends Action implements ISelectionChangedListener
{

	private final SolutionExplorerView viewer;

	/**
	 * Creates a new "duplicate form" action.
	 */
	public LoadRelationsAction(SolutionExplorerView sev)
	{
		viewer = sev;
		setText("Load relations");
		setToolTipText("Load relations");
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
				if (s.isValid() && s.getConfig().isEnabled() && ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != null)
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
			show();
		}
	}

	private void show()
	{
		final IServerInternal server = (IServerInternal)((SimpleUserNode)viewer.getTreeSelection().getFirstElement()).getRealObject();
		final RelationDataLoader dataLoader = new RelationDataLoader();
		WorkspaceJob loadDataAndShowWizardJob = new WorkspaceJob("Loading relations from server '" + server.getName() + "'") //$NON-NLS-1$ 
		{
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				final List<RelationData> relationData = dataLoader.loadData(server, monitor);
				if (!dataLoader.isCanceled())
				{
					Collections.sort(relationData);

					monitor.done();
					Display.getDefault().syncExec(new Runnable()
					{
						public void run()
						{
							if (relationData.size() > 0)
							{
								LoadRelationsWizard loadRelationsWizard = new LoadRelationsWizard(relationData);
								loadRelationsWizard.init(PlatformUI.getWorkbench(), viewer.getTreeSelection());

								WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), loadRelationsWizard);
								dialog.create();
								dialog.open();
							}
							else
							{
								MessageDialog.openInformation(Display.getDefault().getActiveShell(),
									"Load relations", "No relation was found in server '" + server.getName() + "'."); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
							}
						}
					});
					return Status.OK_STATUS;
				}
				else
				{
					return Status.CANCEL_STATUS;
				}
			}

			@Override
			protected void canceling()
			{
				dataLoader.setCanceled(true);
			}
		};

		loadDataAndShowWizardJob.setRule(new SerialRule());
		loadDataAndShowWizardJob.setUser(true);
		loadDataAndShowWizardJob.schedule();
	}

	private class RelationDataLoader
	{
		private boolean canceled;

		public synchronized boolean isCanceled()
		{
			return canceled;
		}

		public synchronized void setCanceled(boolean canceled)
		{
			this.canceled = canceled;
		}

		public List<RelationData> loadData(IServerInternal server, IProgressMonitor monitor)
		{
			List<RelationData> relations = new ArrayList<RelationData>();
			try
			{
				List<String> tableNames = server.getTableNames(true);
				// plus 1 for sorting at the end
				monitor.beginTask("Loading relations", tableNames.size() + 1); //$NON-NLS-1$
				for (String tableName : tableNames)
				{
					if (isCanceled())
					{
						return null;
					}
					Table table = server.getTable(tableName);
					Connection connection = null;
					ResultSet resultSet = null;
					try
					{
						connection = server.getConnection();
						DatabaseMetaData dbmd = connection.getMetaData();
						Map<String, List<List<Object[]>>> relationInfo = new HashMap<String, List<List<Object[]>>>();

						resultSet = dbmd.getExportedKeys(server.getConfig().getCatalog(), server.getConfig().getSchema(), table.getSQLName());
						while (resultSet.next())
						{
							String pcolumnName = resultSet.getString("PKCOLUMN_NAME"); //$NON-NLS-1$
							String ftableName = resultSet.getString("FKTABLE_NAME"); //$NON-NLS-1$
							String fcolumnName = resultSet.getString("FKCOLUMN_NAME"); //$NON-NLS-1$
							String fkname = resultSet.getString("FK_NAME"); //$NON-NLS-1$

							String relname = fkname;
							if (relname == null) relname = table.getSQLName() + "_to_" + ftableName; //$NON-NLS-1$

							int keySeq = resultSet.getInt("KEY_SEQ");
							Debug.trace("Found (export) rel: name: " + relname + "  keyseq = " + keySeq + ' ' + table.getSQLName() + ' ' + pcolumnName +
								" -> " + ftableName + ' ' + fcolumnName);

							List<List<Object[]>> rel_items_list = relationInfo.get(relname);
							if (rel_items_list == null)
							{
								rel_items_list = new ArrayList<List<Object[]>>();
								relationInfo.put(relname, rel_items_list);
								rel_items_list.add(new ArrayList<Object[]>());
							}
							// rel_items_list is a list of items-lists, we are adding items to the last of this list
							rel_items_list.get(rel_items_list.size() - 1).add(new Object[] { pcolumnName, ftableName, fcolumnName });
						}
						resultSet = Utils.closeResultSet(resultSet);

						resultSet = dbmd.getImportedKeys(server.getConfig().getCatalog(), server.getConfig().getSchema(), table.getSQLName());
						int lastKeySeq = Integer.MAX_VALUE;
						while (resultSet.next())
						{
							String pcolumnName = resultSet.getString("PKCOLUMN_NAME"); //$NON-NLS-1$
							String ptableName = resultSet.getString("PKTABLE_NAME"); //$NON-NLS-1$
							String fcolumnName = resultSet.getString("FKCOLUMN_NAME"); //$NON-NLS-1$

							String relname = table.getSQLName() + "_to_" + ptableName; //$NON-NLS-1$

							int keySeq = resultSet.getInt("KEY_SEQ"); //$NON-NLS-1$
							Debug.trace("Found (import) rel: name: " + relname + " keyseq = " + keySeq + ' ' + table.getSQLName() + ' ' + pcolumnName + " -> " +
								ptableName + ' ' + fcolumnName);

							List<List<Object[]>> rel_items_list = relationInfo.get(relname);
							if (rel_items_list == null)
							{
								rel_items_list = new ArrayList<List<Object[]>>();
								relationInfo.put(relname, rel_items_list);
							}

							// assume KEY_SEQ ascending ordered, do not assume 0 or 1 based (jdbc spec is not clear on this).
							// when KEY_SEQ is not increasing, we have a separate constraint between the same tables.
							if (rel_items_list.size() == 0 || keySeq <= lastKeySeq)
							{
								rel_items_list.add(new ArrayList<Object[]>());
							}
							lastKeySeq = keySeq;

							// add the item to the last list of rel_items_list
							rel_items_list.get(rel_items_list.size() - 1).add(new Object[] { fcolumnName, ptableName, pcolumnName });
						}
						resultSet = Utils.closeResultSet(resultSet);

						Iterator<Map.Entry<String, List<List<Object[]>>>> it = relationInfo.entrySet().iterator();
						while (it.hasNext())
						{
							Map.Entry<String, List<List<Object[]>>> entry = it.next();
							String rname = entry.getKey();
							List<List<Object[]>> rel_items_list = entry.getValue();
							// we may have multiple lists of items defined for the same relation name
							for (int l = 0; l < rel_items_list.size(); l++)
							{
								List<Column> primaryColumns = new ArrayList<Column>();
								List<Column> foreignColumns = new ArrayList<Column>();

								List<Object[]> rel_items = rel_items_list.get(l);
								for (int i = 0; i < rel_items.size(); i++)
								{
									Object[] element = rel_items.get(i);

									String pcolumnName = (String)element[0];
									String ftableName = (String)element[1];
									String fcolumnName = (String)element[2];

									Table foreignTable = server.getTable(ftableName);
									if (foreignTable == null) continue;

									Column primaryColumn = table.getColumn(pcolumnName);
									Column foreignColumn = foreignTable.getColumn(fcolumnName);

									if (primaryColumn == null || foreignColumn == null) continue;
									primaryColumns.add(primaryColumn);
									foreignColumns.add(foreignColumn);
								}

								if (primaryColumns.size() != 0)
								{
									// postfix the relation name when there are multiple
									String relationName = rname;
									if (rel_items_list.size() > 1)
									{
										relationName += "_" + (l + 1);
									}

									boolean defaultAdd = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getRelation(
										relationName) == null;

									relations.add(new RelationData(relationName, table, primaryColumns, foreignColumns, defaultAdd));
								}
							}
						}
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
					catch (SQLException e)
					{
						ServoyLog.logError(e);
					}
					finally
					{
						Utils.closeResultSet(resultSet);
						Utils.closeConnection(connection);
					}
					monitor.worked(1);
				}
				monitor.done();
			}
			catch (RepositoryException ex)
			{
				ServoyLog.logError(ex);
			}
			return relations;
		}
	}
}
