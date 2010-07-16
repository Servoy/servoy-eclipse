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
package com.servoy.eclipse.ui.wizards;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.ui.editors.table.ColumnLabelProvider;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.eclipse.ui.util.VerifyingTextCellEditor;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.ISQLJoin;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

public class LoadRelationsWizard extends Wizard implements INewWizard
{
	RelationSelectorWizardPage relationSelectorWizardPage;

	@Override
	public boolean performFinish()
	{
		try
		{
			for (RelationSelectorModel relationSelector : relationSelectorWizardPage.getList())
			{
				ServoyProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(relationSelector.getSolution().getName());
				if (project != null && relationSelector.isAdd())
				{
					Relation relation = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getRelation(
						relationSelector.getRelationName());
					if (relation == null)
					{
						Table table = relationSelector.getPrimaryTable();
						Relation newRelation = createRelation(table, ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(),
							relationSelector.getRelationName(), relationSelector.getSolution(), relationSelector.getPrimaryColumns(),
							relationSelector.getForeignColumns());
						if (newRelation != null)
						{
							project.saveEditingSolutionNodes(new IPersist[] { newRelation }, true);
						}
					}
				}
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		return true;
	}

	public static Relation createRelation(Table table, IValidateName v, String nm, Solution s, List<Column> primaryColumns, List<Column> foreignColumns)
		throws RepositoryException
	{
		Table ft = foreignColumns.get(0).getTable();
		String rname = nm == null ? "db_" + table.getName().replace(' ', '_') + "_to_" + ft.getName().replace(' ', '_') : nm; //$NON-NLS-1$ //$NON-NLS-2$

		Relation relation = s.getRelation(rname);
		if (relation == null)
		{
			relation = s.createNewRelation(v, rname, DataSourceUtils.createDBTableDataSource(table.getServerName(), table.getName()),
				DataSourceUtils.createDBTableDataSource(ft.getServerName(), ft.getName()), ISQLJoin.INNER_JOIN);
			relation.setAllowCreationRelatedRecords(true);
			relation.setExistsInDB(true);
			Column[] parr = new Column[primaryColumns.size()];
			primaryColumns.toArray(parr);
			int[] operators = new int[primaryColumns.size()];
			Column[] farr = new Column[foreignColumns.size()];
			foreignColumns.toArray(farr);
			relation.createNewRelationItems(parr, operators, farr);
		}
		return relation;
	}


	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		relationSelectorWizardPage = new RelationSelectorWizardPage("Load relations",
			(IServerInternal)((SimpleUserNode)selection.getFirstElement()).getRealObject());
	}

	@Override
	public void addPages()
	{
		this.addPage(relationSelectorWizardPage);
	}

	public class RelationSelectorWizardPage extends WizardPage
	{
		private TableViewer tableViewer;
		private final IServerInternal server;

		static final int CI_CREATE = 0;
		static final int CI_NAME = 1;
		static final int CI_SOLUTION = 2;

		protected RelationSelectorWizardPage(String pageName, IServerInternal server)
		{
			super(pageName);
			this.server = server;
			setTitle("Select relations to be created");
			setDescription("");
		}

		public List<RelationSelectorModel> getList()
		{
			return (List<RelationSelectorModel>)tableViewer.getInput();
		}

		public void createControl(Composite parent)
		{
			Composite composite = new Composite(parent, SWT.NONE);
			Composite tableContainer = new Composite(composite, SWT.NONE);
			tableViewer = new TableViewer(tableContainer, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
			tableViewer.getTable().setLinesVisible(true);
			tableViewer.getTable().setHeaderVisible(true);

			final TableColumn createColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_CREATE);
			createColumn.setText("Create");
			TableViewerColumn createViewerColumn = new TableViewerColumn(tableViewer, createColumn);
			createViewerColumn.setEditingSupport(new CreateEditingSupport(tableViewer));

			final TableColumn nameColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_NAME);
			nameColumn.setText("Relation Name");
			TableViewerColumn nameViewerColumn = new TableViewerColumn(tableViewer, nameColumn);
			nameViewerColumn.setEditingSupport(new RelationNameEditingSupport(tableViewer));

			final TableColumn solutionColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_SOLUTION);
			solutionColumn.setText("Solution");
			TableViewerColumn solutionViewerColumn = new TableViewerColumn(tableViewer, solutionColumn);
			solutionViewerColumn.setEditingSupport(new SolutionSelectorEditingSupport(tableViewer));

			final TableColumnLayout layout = new TableColumnLayout();
			tableContainer.setLayout(layout);
			layout.setColumnData(nameColumn, new ColumnWeightData(15, 50, true));
			layout.setColumnData(createColumn, new ColumnPixelData(70, true));
			layout.setColumnData(solutionColumn, new ColumnWeightData(10, 25, true));

			tableViewer.setLabelProvider(new RelationSelectorLabelProvider());
			ObservableListContentProvider relationViewContentProvider = new ObservableListContentProvider();
			tableViewer.setContentProvider(relationViewContentProvider);
			createInput();
			setControl(composite);

			final GroupLayout groupLayout = new GroupLayout(composite);
			groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(
				groupLayout.createSequentialGroup().addContainerGap().add(
					groupLayout.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING, tableContainer, GroupLayout.PREFERRED_SIZE, 450,
						Short.MAX_VALUE)).addContainerGap()));
			groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.TRAILING,
				groupLayout.createSequentialGroup().addContainerGap().add(tableContainer, GroupLayout.PREFERRED_SIZE, 285, Short.MAX_VALUE).addContainerGap()));
			composite.setLayout(groupLayout);
		}

		private void createInput()
		{
			List<RelationSelectorModel> relations = new ArrayList<RelationSelectorModel>();
			try
			{
				for (String tableName : server.getTableNames(true))
				{
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

							int keySeq = resultSet.getInt("KEY_SEQ"); //$NON-NLS-1$
							Debug.trace("Found (export) rel: name: " + relname + "  keyseq = " + keySeq + ' ' + table.getSQLName() + ' ' + pcolumnName + " -> " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								ftableName + ' ' + fcolumnName);

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
							Debug.trace("Found (import) rel: name: " + relname + " keyseq = " + keySeq + ' ' + table.getSQLName() + ' ' + pcolumnName + " -> " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
										relationName += "_" + (l + 1); //$NON-NLS-1$
									}

									boolean defaultAdd = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getRelation(
										relationName) == null;

									relations.add(new RelationSelectorModel(relationName,
										ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getEditingSolution(), table,
										primaryColumns, foreignColumns, defaultAdd));
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
				}
			}
			catch (RepositoryException ex)
			{
				ServoyLog.logError(ex);
			}
			tableViewer.setInput(new WritableList(relations, RelationSelectorModel.class));
		}
	}

	public class RelationSelectorLabelProvider extends LabelProvider implements ITableLabelProvider
	{
		public Image getColumnImage(Object element, int columnIndex)
		{
			if (columnIndex == RelationSelectorWizardPage.CI_CREATE)
			{
				return ((RelationSelectorModel)element).isAdd() ? ColumnLabelProvider.TRUE_IMAGE : ColumnLabelProvider.FALSE_IMAGE;
			}
			return null;
		}

		public String getColumnText(Object element, int columnIndex)
		{
			RelationSelectorModel info = (RelationSelectorModel)element;
			switch (columnIndex)
			{
				case RelationSelectorWizardPage.CI_NAME :
					return info.getRelationName();
				case RelationSelectorWizardPage.CI_SOLUTION :
					return info.getSolution().getName();
				case RelationSelectorWizardPage.CI_CREATE :
					return ""; //$NON-NLS-1$
				default :
					return columnIndex + ": " + element; //$NON-NLS-1$
			}
		}
	}

	public class RelationSelectorModel
	{
		private String relationName;
		private Solution solution;
		private final Table primaryTable;
		private final List<Column> primaryColumns;
		private final List<Column> foreignColumns;
		private boolean add;

		public RelationSelectorModel(String relationName, Solution solution, Table primaryTable, List<Column> primaryColumns, List<Column> foreignColumns,
			boolean add)
		{
			this.relationName = relationName;
			this.solution = solution;
			this.primaryTable = primaryTable;
			this.primaryColumns = primaryColumns;
			this.foreignColumns = foreignColumns;
			this.add = add;
		}

		public String getRelationName()
		{
			return relationName;
		}

		public void setRelationName(String relationName)
		{
			this.relationName = relationName;
		}

		public boolean isAdd()
		{
			return add;
		}

		public void setAdd(boolean add)
		{
			this.add = add;
		}

		public Solution getSolution()
		{
			return solution;
		}

		public void setSolution(Solution solution)
		{
			this.solution = solution;
		}

		public List<Column> getPrimaryColumns()
		{
			return primaryColumns;
		}

		public List<Column> getForeignColumns()
		{
			return foreignColumns;
		}

		public Table getPrimaryTable()
		{
			return primaryTable;
		}
	}

	public class CreateEditingSupport extends EditingSupport
	{
		private final CellEditor editor;

		public CreateEditingSupport(TableViewer tv)
		{
			super(tv);
			editor = new CheckboxCellEditor(tv.getTable());
		}

		@Override
		protected void setValue(Object element, Object value)
		{
			if (element instanceof RelationSelectorModel)
			{
				RelationSelectorModel relSelector = (RelationSelectorModel)element;
				relSelector.setAdd(Boolean.parseBoolean(value.toString()));
				getViewer().update(element, null);
			}
		}

		@Override
		protected Object getValue(Object element)
		{
			if (element instanceof RelationSelectorModel)
			{
				RelationSelectorModel relSelector = (RelationSelectorModel)element;
				return new Boolean(relSelector.isAdd());
			}
			return null;
		}

		@Override
		protected CellEditor getCellEditor(Object element)
		{
			return editor;
		}

		@Override
		protected boolean canEdit(Object element)
		{
			return true;
		}
	}
	public class RelationNameEditingSupport extends EditingSupport
	{
		private final VerifyingTextCellEditor editor;

		public RelationNameEditingSupport(TableViewer tv)
		{
			super(tv);
			editor = new VerifyingTextCellEditor(tv.getTable());
			editor.addVerifyListener(DocumentValidatorVerifyListener.IDENT_SERVOY_VERIFIER);
		}

		@Override
		protected void setValue(Object element, Object value)
		{
			if (element instanceof RelationSelectorModel)
			{
				RelationSelectorModel relSelector = (RelationSelectorModel)element;
				relSelector.setRelationName(value.toString());
				getViewer().update(element, null);
			}
		}

		@Override
		protected Object getValue(Object element)
		{
			if (element instanceof RelationSelectorModel)
			{
				RelationSelectorModel relSelector = (RelationSelectorModel)element;
				return relSelector.getRelationName();
			}
			return null;
		}

		@Override
		protected CellEditor getCellEditor(Object element)
		{
			return editor;
		}

		@Override
		protected boolean canEdit(Object element)
		{
			return true;
		}
	}
	public class SolutionSelectorEditingSupport extends EditingSupport
	{
		private final CellEditor editor;
		private final Solution[] solutions;

		public SolutionSelectorEditingSupport(TableViewer tv)
		{
			super(tv);
			Map<String, Solution> allsolutions = new HashMap<String, Solution>();

			ServoyProject[] modules = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
			if (modules != null && modules.length > 0)
			{
				for (ServoyProject project : modules)
				{
					Solution editingSolution = project.getEditingSolution();
					allsolutions.put(editingSolution.getName(), editingSolution);
				}
			}
			Solution editingSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getEditingSolution();
			allsolutions.put(editingSolution.getName(), editingSolution);

			solutions = allsolutions.values().toArray(new Solution[] { });
			String[] solutionsNames = new String[allsolutions.values().size()];
			Iterator<Solution> it = allsolutions.values().iterator();
			int i = 0;
			while (it.hasNext())
			{
				solutionsNames[i++] = it.next().getName();
			}
			editor = new ComboBoxCellEditor(tv.getTable(), solutionsNames, SWT.READ_ONLY);
		}

		@Override
		protected void setValue(Object element, Object value)
		{
			if (element instanceof RelationSelectorModel)
			{
				RelationSelectorModel relSelector = (RelationSelectorModel)element;
				int index = Integer.parseInt(value.toString());
				Solution solution = solutions[index];
				relSelector.setSolution(solution);
				getViewer().update(element, null);
			}
		}

		@Override
		protected Object getValue(Object element)
		{
			if (element instanceof RelationSelectorModel)
			{
				RelationSelectorModel relSelector = (RelationSelectorModel)element;
				Solution solution = relSelector.getSolution();
				int index = 0;
				for (int i = 0; i < solutions.length; i++)
				{
					if (solutions[i].equals(solution))
					{
						index = i;
						break;
					}
				}
				return new Integer(index);
			}
			return null;
		}

		@Override
		protected CellEditor getCellEditor(Object element)
		{
			return editor;
		}

		@Override
		protected boolean canEdit(Object element)
		{
			return true;
		}
	}

}
