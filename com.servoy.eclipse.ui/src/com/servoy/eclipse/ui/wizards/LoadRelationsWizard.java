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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.SerialRule;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
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

public class LoadRelationsWizard extends Wizard implements INewWizard
{
	public static class RelationData implements Comparable<RelationData>
	{
		public final String relationName;
		public final Table table;
		public final List<Column> primaryColumns;
		public final List<Column> foreignColumns;
		public final boolean defaultAdd;

		public RelationData(String relationName, Table table, List<Column> primaryColumns, List<Column> foreignColumns, boolean defaultAdd)
		{
			this.relationName = relationName;
			this.table = table;
			this.primaryColumns = primaryColumns;
			this.foreignColumns = foreignColumns;
			this.defaultAdd = defaultAdd;
		}

		public int compareTo(RelationData o)
		{
			return this.relationName.compareTo(o.relationName);
		}
	}

	RelationSelectorWizardPage relationSelectorWizardPage;
	private final List<RelationData> relationData;

	public LoadRelationsWizard(List<RelationData> relationData)
	{
		this.relationData = relationData;
	}

	@Override
	public boolean performFinish()
	{
		// Make copy to later use in workspace job.
		final List<RelationSelectorModel> rsms = new ArrayList<RelationSelectorModel>();
		rsms.addAll(relationSelectorWizardPage.getList());

		WorkspaceJob generateJob = new WorkspaceJob("Generating relations") //$NON-NLS-1$ 
		{
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				monitor.beginTask("Generating relations", rsms.size()); //$NON-NLS-1$
				try
				{
					for (RelationSelectorModel relationSelector : rsms)
					{
						ServoyProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(
							relationSelector.getSolution().getName());
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
						monitor.worked(1);
					}
					monitor.done();
					return Status.OK_STATUS;
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
					monitor.done();
					Display.getDefault().syncExec(new Runnable()
					{
						public void run()
						{
							MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Error occured while generating the relations."); //$NON-NLS-1$ //$NON-NLS-2$
						}
					});
					return Status.CANCEL_STATUS;
				}
			}
		};

		generateJob.setRule(SerialRule.INSTANCE);
		generateJob.setUser(true);
		generateJob.schedule();

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
			for (RelationData rdata : relationData)
			{
				relations.add(new RelationSelectorModel(rdata.relationName,
					ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getEditingSolution(), rdata.table, rdata.primaryColumns,
					rdata.foreignColumns, rdata.defaultAdd));
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
