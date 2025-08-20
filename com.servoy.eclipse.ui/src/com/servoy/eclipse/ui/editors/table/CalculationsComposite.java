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

import java.util.Collection;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.preferences.JSDocScriptTemplates;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.eclipse.ui.editors.table.actions.SearchForDataProvidersReferencesAction;
import com.servoy.eclipse.ui.labelproviders.TextCutoffLabelProvider;
import com.servoy.eclipse.ui.resource.ColorResource;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.DuplicatePersistAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.MoveTableNodeChildAction;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistChangeListener;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValidatorSearchContext;

public class CalculationsComposite extends AbstractTableEditorComposite
{
	private final Button removeButton;
	private final Button openCalculationButton;
	private final MenuItem moveItem;
	private final MenuItem duplicateItem;
	private final MenuItem searchForReferencesItem;

	public static final int CI_NAME = 0;
	public static final int CI_TYPE = 1;
	public static final int CI_CALCULATION = 2;
	public static final int CI_STORED = 3;
	private IPersistChangeListener persistListener;
	private final TableEditor tableEditor;

	public CalculationsComposite(final TableEditor te, Composite parent, FlattenedSolution flattenedSolution, int style)
	{
		super(parent, style, flattenedSolution);

		tableEditor = te;
		final ITable t = te.getTable();
		Tree tree = treeViewer.getTree();
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);

		Menu menu = new Menu(getShell(), SWT.POP_UP);
		MenuItem addItem = new MenuItem(menu, SWT.PUSH);
		addItem.setText("Add calculation");
		addItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				addCalculation(te);
			}
		});
		moveItem = new MenuItem(menu, SWT.PUSH);
		moveItem.setText("Move calculation");
		moveItem.setEnabled(false);
		moveItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TreeItem[] selection = treeViewer.getTree().getSelection();
				if (selection != null && selection.length > 0 && selection[0].getData() instanceof ScriptCalculation)
				{
					MoveTableNodeChildAction action = new MoveTableNodeChildAction(getShell());
					action.setPersist((ScriptCalculation)selection[0].getData());
					action.run();
					treeViewer.refresh();
				}
			}
		});
		duplicateItem = new MenuItem(menu, SWT.PUSH);
		duplicateItem.setText("Duplicate calculation");
		duplicateItem.setEnabled(false);
		duplicateItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TreeItem[] selection = treeViewer.getTree().getSelection();
				if (selection != null && selection.length > 0 && selection[0].getData() instanceof ScriptCalculation)
				{
					DuplicatePersistAction action = new DuplicatePersistAction(getShell());
					action.setPersist((ScriptCalculation)selection[0].getData());
					action.run();
					treeViewer.refresh();
				}
			}
		});
		MenuItem removeItem = new MenuItem(menu, SWT.PUSH);
		removeItem.setText("Remove calculation");
		removeItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				removeSelectedCalculation();
			}
		});

		searchForReferencesItem = new MenuItem(menu, SWT.PUSH);
		searchForReferencesItem.setText("Search for References");
		searchForReferencesItem.setEnabled(true);
		searchForReferencesItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TreeItem[] selection = treeViewer.getTree().getSelection();
				if (selection != null && selection.length > 0 && selection[0].getData() instanceof ScriptCalculation)
				{
					SearchForDataProvidersReferencesAction searchAction = new SearchForDataProvidersReferencesAction((IColumn)selection[0].getData());
					searchAction.run();
				}
			}
		});

		tree.setMenu(menu);

		removeButton = new Button(container, SWT.NONE);
		removeButton.setText("Remove selected");
		removeButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				removeSelectedCalculation();
			}
		});
		removeButton.setEnabled(false);


		openCalculationButton = new Button(container, SWT.NONE);
		openCalculationButton.setText("Open selected calculation");
		openCalculationButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				ScriptCalculation calculation = null;
				TreeItem[] selection = treeViewer.getTree().getSelection();
				if (selection != null && selection.length > 0)
				{
					Object selectedData = null;
					if (selection[0].getData() instanceof Solution && selection[0].getItemCount() > 0)
					{
						selectedData = selection[0].getItem(0).getData();
					}
					else
					{
						selectedData = selection[0].getData();
					}

					if (selectedData instanceof ScriptCalculation)
					{
						calculation = (ScriptCalculation)selectedData;
					}
				}

				if (calculation != null)
				{
					openCalculationScriptEditor(calculation);
				}
				else
				{
					MessageDialog.openError(getShell(), "Error", "You must select a calculation to open.");
				}
			}
		});
		openCalculationButton.setEnabled(false);

		Button addButton;
		addButton = new Button(container, SWT.NONE);
		addButton.setText("Add");
		addButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				addCalculation(te);
			}
		});

		tree.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				treeViewer.getTree().setToolTipText("");
				TreeItem[] selection = treeViewer.getTree().getSelection();
				if (selection != null && selection.length > 0)
				{
					if (selection[0].getData() instanceof ScriptCalculation)
					{
						removeButton.setEnabled(true);
						openCalculationButton.setEnabled(true);
						if (ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject().length > 1)
						{
							moveItem.setEnabled(true);
							duplicateItem.setEnabled(true);
						}
						ScriptCalculation calculation = (ScriptCalculation)selection[0].getData();
						if (!calculation.getName().toLowerCase().equals(calculation.getName()))
						{
							treeViewer.getTree().setToolTipText("Using non lowercase names will make it hard to store a calculation later on.");
						}
					}
					else if (selection[0].getData() instanceof Solution)
					{
						removeButton.setEnabled(false);
						openCalculationButton.setEnabled(selection[0].getItemCount() > 0 && (selection[0].getItem(0).getData() instanceof ScriptCalculation));
					}
				}
				else
				{
					removeButton.setEnabled(false);
					openCalculationButton.setEnabled(false);
					moveItem.setEnabled(false);
					duplicateItem.setEnabled(false);
				}
			}
		});
		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.TRAILING,
			groupLayout.createSequentialGroup().addContainerGap().add(groupLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.LEADING,
				treeContainer, GroupLayout.PREFERRED_SIZE, 482, Short.MAX_VALUE).add(
					groupLayout.createSequentialGroup().add(addButton).addPreferredGap(LayoutStyle.RELATED).add(removeButton).addPreferredGap(
						LayoutStyle.RELATED).add(openCalculationButton)))
				.addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.TRAILING,
			groupLayout.createSequentialGroup().addContainerGap().add(treeContainer, GroupLayout.PREFERRED_SIZE, 323, Short.MAX_VALUE).addPreferredGap(
				LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.BASELINE).add(openCalculationButton).add(removeButton).add(addButton))
				.addContainerGap()));
		container.setLayout(groupLayout);
		//
		createTableColumns(t, te);

		initDataBindings(t);

		myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	@Override
	public void setVisible(boolean visible)
	{
		if (visible && persistListener == null)
		{
			persistListener = new IPersistChangeListener()
			{
				public void persistChanges(Collection<IPersist> changes)
				{
					// For now just refresh if it is a TableNode or ScriptCalculation that changed.
					for (IPersist persist : changes)
					{
						if (persist.getParent() instanceof TableNode || persist instanceof TableNode)
						{
							Display.getDefault().asyncExec(new Runnable()
							{
								public void run()
								{
									if (!treeViewer.getTree().isDisposed()) treeViewer.refresh();
								}
							});
							break;
						}
					}
				}
			};
			ServoyModelManager.getServoyModelManager().getServoyModel().addPersistChangeListener(false, persistListener);
			refresh();
		}
		else if (!visible && persistListener != null)
		{
			ServoyModelManager.getServoyModelManager().getServoyModel().addPersistChangeListener(false, persistListener);
			persistListener = null;
		}
		super.setVisible(visible);
	}

	private void addCalculation(TableEditor te)
	{
		TreeItem[] selection = treeViewer.getTree().getSelection();
		if (selection != null && selection.length > 0)
		{
			IValidateName nameValidator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
			String calcName = "type_here";
			String orgName = calcName;
			try
			{
				Solution solution = null;
				if (selection[0].getData() instanceof Solution) solution = (Solution)selection[0].getData();
				else if (selection[0].getData() instanceof ScriptCalculation)
				{
					solution = (Solution)((ScriptCalculation)selection[0].getData()).getAncestor(IRepository.SOLUTIONS);
				}
				if (solution != null)
				{
					int i = 1;
					boolean isValidName = false;
					ValidatorSearchContext searchContext = new ValidatorSearchContext(te.getTable(), IRepository.SCRIPTCALCULATIONS);
					while (!isValidName)
					{
						try
						{
							nameValidator.checkName(calcName, null, searchContext, false);
							isValidName = true;
						}
						catch (RepositoryException e)
						{
							calcName = orgName + i;
							i++;
						}
					}
					ServoyProject project = ServoyModelFinder.getServoyModel().getServoyProject(solution.getName());
					String userTemplate = JSDocScriptTemplates.getTemplates(project.getProject(), true).getMethodTemplate();
					ScriptCalculation s = solution.createNewScriptCalculation(nameValidator, te.getTable(), calcName, userTemplate);
					s.setType(Column.allDefinedTypes[0]);
					treeViewer.refresh(solution);
					treeViewer.editElement(s, 0);
					removeButton.setEnabled(true);
					openCalculationButton.setEnabled(true);
					moveItem.setEnabled(true);
					duplicateItem.setEnabled(true);
					te.flagModified();
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
		else
		{
			MessageDialog.openError(getShell(), "Error", "You must select a solution or module where to add the calculation.");
		}
	}

	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose()
	{
		if (persistListener != null) ServoyModelManager.getServoyModelManager().getServoyModel().removePersistChangeListener(false, persistListener);
		persistListener = null;
		super.dispose();
	}

	public void refresh()
	{
		if (treeViewer != null) treeViewer.refresh();
	}

	private void openCalculationScriptEditor(ScriptCalculation calculation)
	{
		if (tableEditor.isDirty())
		{
			tableEditor.doSave(null);
		}
		EditorUtil.openScriptEditor(calculation, null, true);
	}

	private void createTableColumns(ITable table, final TableEditor te)
	{
		TreeColumn nameColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, CI_NAME);
		nameColumn.setText("Name");
		TreeViewerColumn nameViewerColumn = new TreeViewerColumn(treeViewer, nameColumn);
		CalculationNameEditingSupport nameEditing = new CalculationNameEditingSupport(treeViewer, table);
		nameEditing.addChangeListener(new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				te.flagModified();
			}
		});
		nameViewerColumn.setEditingSupport(nameEditing);

		TreeColumn typeColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, CI_TYPE);
		typeColumn.setText("Returned type");
		TreeViewerColumn typeViewerColumn = new TreeViewerColumn(treeViewer, typeColumn);
		CalculationTypeEditingSupport typeEditing = new CalculationTypeEditingSupport(treeViewer);
		typeEditing.addChangeListener(new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				te.flagModified();
			}
		});
		typeViewerColumn.setEditingSupport(typeEditing);

		TreeColumn codeColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, CI_CALCULATION);
		codeColumn.setText("Calculation");
		treeViewer.getTree().addListener(SWT.MouseDoubleClick, new Listener()
		{
			public void handleEvent(Event event)
			{
				TreeItem[] selection = treeViewer.getTree().getSelection();

				if (selection.length == 1 && selection[0].getData() instanceof ScriptCalculation)
				{
					ScriptCalculation calculation = (ScriptCalculation)selection[0].getData();
					TreeItem item = selection[0];
					for (int i = 0; i < treeViewer.getTree().getColumnCount(); i++)
					{
						if (item.getBounds(i).contains(event.x, event.y) && i == CI_CALCULATION)
						{
							openCalculationScriptEditor(calculation);
						}
					}
				}
			}
		});

		TreeColumn storedColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, CI_STORED);
		storedColumn.setText("Stored");

		TreeColumnLayout layout = new TreeColumnLayout();
		treeContainer.setLayout(layout);
		layout.setColumnData(nameColumn, new ColumnWeightData(10, 50, true));
		layout.setColumnData(typeColumn, new ColumnPixelData(90, true));
		layout.setColumnData(codeColumn, new ColumnWeightData(10, 50, true));
		layout.setColumnData(storedColumn, new ColumnPixelData(50, true));

		treeViewer.setLabelProvider(new TextCutoffLabelProvider.TableCutoffLabelProvider(
			new CalculationLabelProvider(table, ColorResource.INSTANCE.getColor(new RGB(255, 127, 0))), 100));
	}

	protected void initDataBindings(ITable t)
	{
		TableScriptsContentProvider columnViewContentProvider = new TableScriptsContentProvider(t, IRepository.SCRIPTCALCULATIONS);
		treeViewer.setContentProvider(columnViewContentProvider);
		super.setRows(t);
	}

	private void removeSelectedCalculation()
	{
		TreeItem[] selection = treeViewer.getTree().getSelection();
		if (selection != null && selection.length > 0 && selection[0].getData() instanceof ScriptCalculation)
		{
			ScriptCalculation calculation = (ScriptCalculation)selection[0].getData();
			if (MessageDialog.openConfirm(getShell(), "Delete calculation", "Are you sure you want to delete calculation '" + calculation.getName() + "'?"))
			{
				try
				{
					((IDeveloperRepository)calculation.getRootObject().getRepository()).deleteObject(calculation);
				}
				catch (RepositoryException ex)
				{
					ServoyLog.logError(ex);
				}
				treeViewer.remove(calculation);
				treeViewer.getTree().forceFocus();
				tableEditor.flagModified();
			}
		}
		else
		{
			MessageDialog.openError(getShell(), "Error", "You must select a calculation to delete.");
		}
	}
}
