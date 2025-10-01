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
package com.servoy.eclipse.ui.editors.table;

import java.util.Iterator;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.eclipse.ui.editors.table.actions.SearchForDataProvidersReferencesAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.DuplicatePersistAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.MoveTableNodeChildAction;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.util.Debug;

public class AggregationsComposite extends AbstractTableEditorComposite
{
	private final Button removeButton;
	private final MenuItem moveItem;
	private final MenuItem duplicateItem;
	private final MenuItem searchForReferencesItem;

//	private final DataBindingContext m_bindingContext;
	/**
	 * Create the composite
	 *
	 * @param parent
	 * @param style
	 */
	public AggregationsComposite(final TableEditor te, Composite parent, FlattenedSolution flattenedSolution, int style)
	{
		super(parent, style, flattenedSolution);

		final ITable t = te.getTable();

		Tree tree = treeViewer.getTree();
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);

		Menu menu = new Menu(getShell(), SWT.POP_UP);
		MenuItem addItem = new MenuItem(menu, SWT.PUSH);
		addItem.setText("Add aggregation");
		addItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				addAggregation(te);
			}
		});
		moveItem = new MenuItem(menu, SWT.PUSH);
		moveItem.setText("Move Aggregation");
		moveItem.setEnabled(false);
		moveItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TreeItem[] selection = treeViewer.getTree().getSelection();
				if (selection != null && selection.length > 0 && selection[0].getData() instanceof AggregateVariable)
				{
					MoveTableNodeChildAction action = new MoveTableNodeChildAction(getShell());
					action.setPersist((AggregateVariable)selection[0].getData());
					action.run();
					treeViewer.refresh();
				}
			}
		});
		duplicateItem = new MenuItem(menu, SWT.PUSH);
		duplicateItem.setText("Duplicate aggregation");
		duplicateItem.setEnabled(false);
		duplicateItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TreeItem[] selection = treeViewer.getTree().getSelection();
				if (selection != null && selection.length > 0 && selection[0].getData() instanceof AggregateVariable)
				{
					DuplicatePersistAction action = new DuplicatePersistAction(getShell());
					action.setPersist((AggregateVariable)selection[0].getData());
					action.run();
					treeViewer.refresh();
				}
			}
		});
		searchForReferencesItem = new MenuItem(menu, SWT.PUSH);
		searchForReferencesItem.setText("Search for Referecens");
		searchForReferencesItem.setEnabled(true);
		searchForReferencesItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TreeItem[] selection = treeViewer.getTree().getSelection();
				if (selection != null && selection.length > 0 && selection[0].getData() instanceof AggregateVariable)
				{
					SearchForDataProvidersReferencesAction searchAction = new SearchForDataProvidersReferencesAction((IColumn)selection[0].getData());
					searchAction.run();
				}
			}
		});
		tree.setMenu(menu);

		removeButton = new Button(container, SWT.NONE);
		removeButton.setText("Remove");
		removeButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TreeItem[] selection = treeViewer.getTree().getSelection();
				if (selection != null && selection.length > 0 && selection[0].getData() instanceof AggregateVariable)
				{
					AggregateVariable aggregation = (AggregateVariable)selection[0].getData();
					if (MessageDialog.openConfirm(getShell(), "Delete aggregation",
						"Are you sure you want to delete aggregation '" + aggregation.getName() + "'?"))
					{
						try
						{
							((IDeveloperRepository)aggregation.getRootObject().getRepository()).deleteObject(aggregation);
						}
						catch (RepositoryException ex)
						{
							Debug.error(ex);
						}
						treeViewer.remove(aggregation);
						te.flagModified();
					}
				}
				else
				{
					MessageDialog.openError(getShell(), "Error", "You must select an aggregate to delete.");
				}
			}
		});
		removeButton.setEnabled(false);

		tree.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TreeItem[] selection = treeViewer.getTree().getSelection();
				if (selection != null && selection.length > 0 && selection[0].getData() instanceof AggregateVariable)
				{
					if (ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject().length > 1)
					{
						moveItem.setEnabled(true);
						duplicateItem.setEnabled(true);
					}
					removeButton.setEnabled(true);
				}
				else
				{
					removeButton.setEnabled(false);
					moveItem.setEnabled(false);
					duplicateItem.setEnabled(false);
				}
			}
		});

		Button addButton;
		addButton = new Button(container, SWT.NONE);
		addButton.setText("Add");
		addButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				addAggregation(te);
			}
		});
		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.TRAILING,
			groupLayout.createSequentialGroup().addContainerGap().add(groupLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.LEADING,
				treeContainer, GroupLayout.PREFERRED_SIZE, 482, Short.MAX_VALUE).add(
					groupLayout.createSequentialGroup().add(addButton).addPreferredGap(LayoutStyle.RELATED).add(removeButton)))
				.addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.TRAILING,
			groupLayout.createSequentialGroup().addContainerGap().add(treeContainer, GroupLayout.PREFERRED_SIZE, 323, Short.MAX_VALUE).addPreferredGap(
				LayoutStyle.RELATED).add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(removeButton).add(addButton)).addContainerGap()));


		container.setLayout(groupLayout);
		createTreeColumns(t, te);
		initDataBindings(t);

		myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

	}

	private void addAggregation(TableEditor te)
	{
		TreeItem[] selection = treeViewer.getTree().getSelection();
		if (selection != null && selection.length > 0)
		{
			IValidateName nameValidator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
			String orgName = "type_here";
			String newName = orgName;
			int type = QueryAggregate.ALL_DEFINED_AGGREGATES[0];
			Iterator<Column> it = te.getTable().getColumns().iterator();
			if (it.hasNext()) //we need to make sure there is one column
			{
				Column column = it.next();
				try
				{
					ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
					if (servoyProject != null)
					{
						Solution solution = null;
						if (selection[0].getData() instanceof Solution) solution = (Solution)selection[0].getData();
						else if (selection[0].getData() instanceof AggregateVariable)
						{
							solution = (Solution)((AggregateVariable)selection[0].getData()).getAncestor(IRepository.SOLUTIONS);
						}
						if (solution != null)
						{
							int i = 1;
							boolean isValidName = false;
							ValidatorSearchContext searchContext = new ValidatorSearchContext(te.getTable(), IRepository.AGGREGATEVARIABLES);
							while (!isValidName)
							{
								try
								{
									nameValidator.checkName(newName, null, searchContext, true);
									isValidName = true;
								}
								catch (RepositoryException e)
								{
									newName = orgName + i;
									i++;
								}
							}

							AggregateVariable aggregationVariable = solution.createNewAggregateVariable(nameValidator, te.getTable(), newName, type,
								column.getDataProviderID());
							treeViewer.refresh(solution);
							treeViewer.editElement(aggregationVariable, 0);
							removeButton.setEnabled(true);
							te.flagModified();
						}
					}
				}
				catch (RepositoryException ex)
				{
					ServoyLog.logError(ex);
					MessageDialog.openError(getShell(), "Error", "Save failed: " + ex.getMessage());
				}
				catch (Exception e1)
				{
					ServoyLog.logError(e1);
				}
			}
		}
		else
		{
			MessageDialog.openError(getShell(), "Error", "You must select a solution or module where to add the aggregate.");
		}
	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	public static final int CI_NAME = 0;
	public static final int CI_TYPE = 1;
	static final int CI_COLUMN = 2;

	private void createTreeColumns(ITable table, final TableEditor te)
	{
		TreeColumn nameColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, CI_NAME);
		nameColumn.setText("Name");
		//nameColumn.setWidth(200);
		TreeViewerColumn nameViewerColumn = new TreeViewerColumn(treeViewer, nameColumn);
		AggregationNameEditingSupport nameEditing = new AggregationNameEditingSupport(treeViewer);
		nameEditing.addChangeListener(new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				te.flagModified();
			}
		});
		nameViewerColumn.setEditingSupport(nameEditing);

		TreeColumn typeColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, CI_TYPE);
		typeColumn.setText("Type");
		//typeColumn.setWidth(200);
		TreeViewerColumn typeViewerColumn = new TreeViewerColumn(treeViewer, typeColumn);
		AggregationTypeEditingSupport typeEditing = new AggregationTypeEditingSupport(treeViewer);
		typeEditing.addChangeListener(new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				te.flagModified();
			}
		});
		typeViewerColumn.setEditingSupport(typeEditing);

		TreeColumn lengthColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, CI_COLUMN);
		lengthColumn.setText("Column");
		//lengthColumn.setWidth(200);
		TreeViewerColumn lengthViewerColumn = new TreeViewerColumn(treeViewer, lengthColumn);
		AggregationColumnEditingSupport columnEditing = new AggregationColumnEditingSupport(table, treeViewer);
		columnEditing.addChangeListener(new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				te.flagModified();
			}
		});
		lengthViewerColumn.setEditingSupport(columnEditing);

		TreeColumnLayout layout = new TreeColumnLayout();
		treeContainer.setLayout(layout);
		layout.setColumnData(nameColumn, new ColumnWeightData(10, 50, true));
		layout.setColumnData(typeColumn, new ColumnWeightData(10, 50, true));
		layout.setColumnData(lengthColumn, new ColumnWeightData(10, 50, true));

		treeViewer.setLabelProvider(new AggregationLabelProvider());

	}

	protected void initDataBindings(ITable t)
	{
		AggregationContentProvider columnViewContentProvider = new AggregationContentProvider(t);
		treeViewer.setContentProvider(columnViewContentProvider);
		super.setRows(t);
	}

	public void selectColumn(AggregateVariable column)
	{
		treeViewer.setSelection(new StructuredSelection(column));
	}
}
