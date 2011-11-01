/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

/**
 * Composite for the methods-tab in table editor.
 * 
 * @author rgansevles
 */
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.eclipse.ui.labelproviders.TextCutoffLabelProvider;
import com.servoy.eclipse.ui.resource.ColorResource;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;

public class FoundsetMethodsComposite extends Composite
{
	private final TreeViewer treeViewer;
	private ArrayList<Solution> rows;
	private final Composite treeContainer;

	public static final int CI_NAME = 0;
	public static final int CI_CODE = 1;
	private final IPersistChangeListener persistListener;
	private final FlattenedSolution flattenedSolution;

	public FoundsetMethodsComposite(final TableEditor te, Composite parent, FlattenedSolution flattenedSolution, int style)
	{
		super(parent, style);
		this.flattenedSolution = flattenedSolution;
		this.setLayout(new FillLayout());
		ScrolledComposite myScrolledComposite = new ScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);

		Composite container = new Composite(myScrolledComposite, SWT.NONE);

		myScrolledComposite.setContent(container);

		final Table t = te.getTable();
		treeContainer = new Composite(container, SWT.NONE);

		treeViewer = new TreeViewer(treeContainer, SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);

		Tree tree = treeViewer.getTree();
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);

		final Button removeButton;
		removeButton = new Button(container, SWT.NONE);
		removeButton.setText("Remove");
		removeButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TreeItem[] selection = treeViewer.getTree().getSelection();
				if (selection != null && selection.length > 0 && selection[0].getData() instanceof ScriptMethod)
				{
					ScriptMethod method = (ScriptMethod)selection[0].getData();
					if (MessageDialog.openConfirm(getShell(), "Delete method", "Are you sure you want to delete method '" + method.getName() + "'?"))
					{
						try
						{
							((IDeveloperRepository)method.getRootObject().getRepository()).deleteObject(method);
						}
						catch (RepositoryException ex)
						{
							ServoyLog.logError(ex);
						}
						treeViewer.remove(method);
						te.flagModified();
					}
				}
				else
				{
					MessageDialog.openError(getShell(), "Error", "You must select a method to delete.");
				}
			}
		});
		removeButton.setEnabled(false);

		Button addButton;
		addButton = new Button(container, SWT.NONE);
		addButton.setText("Add");
		addButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TreeItem[] selection = treeViewer.getTree().getSelection();
				if (selection != null && selection.length > 0)
				{
					IValidateName nameValidator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
					String methodName = "new_method"; //$NON-NLS-1$
					String orgName = methodName;
					try
					{
						Solution solution = null;
						if (selection[0].getData() instanceof Solution) solution = (Solution)selection[0].getData();
						else if (selection[0].getData() instanceof ScriptMethod)
						{
							solution = (Solution)((ScriptMethod)selection[0].getData()).getAncestor(IRepository.SOLUTIONS);
						}
						if (solution != null)
						{
							ScriptMethod method = ModelUtils.getEditingFlattenedSolution(solution).getFoundsetMethod(methodName, t.getDataSource());
							int i = 1;
							while (method != null)
							{
								methodName = orgName + (i++);
								method = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(solution).getFoundsetMethod(
									methodName, t.getDataSource());
							}
							ScriptMethod s = solution.createNewFoundsetMethod(nameValidator, t.getDataSource(), methodName);
							treeViewer.refresh(solution);
							treeViewer.editElement(s, 0);
							removeButton.setEnabled(true);
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
					MessageDialog.openError(getShell(), "Error", "You must select a solution or module where to add the method.");
				}

			}
		});

		tree.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				treeViewer.getTree().setToolTipText("");
				TreeItem[] selection = treeViewer.getTree().getSelection();
				removeButton.setEnabled(selection != null && selection.length > 0 && selection[0].getData() instanceof ScriptMethod);
			}
		});
		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			GroupLayout.TRAILING,
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING, treeContainer, GroupLayout.PREFERRED_SIZE, 482, Short.MAX_VALUE).add(
					groupLayout.createSequentialGroup().add(addButton).addPreferredGap(LayoutStyle.RELATED).add(removeButton))).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			GroupLayout.TRAILING,
			groupLayout.createSequentialGroup().addContainerGap().add(treeContainer, GroupLayout.PREFERRED_SIZE, 323, Short.MAX_VALUE).addPreferredGap(
				LayoutStyle.RELATED).add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(removeButton).add(addButton)).addContainerGap()));
		container.setLayout(groupLayout);
		//
		createTableColumns(te);

		initDataBindings(t);

		myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		persistListener = new IPersistChangeListener()
		{
			public void persistChanges(Collection<IPersist> changes)
			{
				// For now just refresh if it is a TableNode or ScriptMethod that changed.
				for (IPersist persist : changes)
				{
					if (persist.getParent() instanceof TableNode || persist instanceof TableNode)
					{
						Display.getDefault().asyncExec(new Runnable()
						{
							public void run()
							{
								treeViewer.refresh();
							}
						});
						break;
					}
				}
			}
		};
		ServoyModelManager.getServoyModelManager().getServoyModel().addPersistChangeListener(false, persistListener);
	}

	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel().removePersistChangeListener(false, persistListener);
		super.dispose();
	}

	public void refresh()
	{
		if (treeViewer != null) treeViewer.refresh();
	}

	private void createTableColumns(final TableEditor te)
	{
		TreeColumn nameColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, CI_NAME);
		nameColumn.setText("Name");
		TreeViewerColumn nameViewerColumn = new TreeViewerColumn(treeViewer, nameColumn);
		MethodNameEditingSupport nameEditing = new MethodNameEditingSupport(treeViewer);
		nameEditing.addChangeListener(new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				te.flagModified();
			}
		});
		nameViewerColumn.setEditingSupport(nameEditing);

		TreeColumn codeColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, CI_CODE);
		codeColumn.setText("Source");
		treeViewer.getTree().addListener(SWT.MouseDoubleClick, new Listener()
		{
			public void handleEvent(Event event)
			{
				TreeItem[] selection = treeViewer.getTree().getSelection();

				if (selection.length == 1 && selection[0].getData() instanceof ScriptMethod)
				{
					ScriptMethod method = (ScriptMethod)selection[0].getData();
					TreeItem item = selection[0];
					for (int i = 0; i < treeViewer.getTree().getColumnCount(); i++)
					{
						if (item.getBounds(i).contains(event.x, event.y) && i == CI_CODE)
						{
							if (te.isDirty())
							{
								MessageDialog.openError(getShell(), "Error", "You must save before editing the method code.");
								return;
							}
							EditorUtil.openScriptEditor(method, null, true);
						}
					}
				}
			}
		});

		TreeColumnLayout layout = new TreeColumnLayout();
		treeContainer.setLayout(layout);
		layout.setColumnData(nameColumn, new ColumnWeightData(20, 50, true));
		layout.setColumnData(codeColumn, new ColumnWeightData(80, 50, true));

		treeViewer.setLabelProvider(new TextCutoffLabelProvider.TableCutoffLabelProvider(new FoundsetMethodLabelProvider(
			ColorResource.INSTANCE.getColor(new RGB(255, 127, 0))), 100));
	}

	protected void initDataBindings(Table t)
	{
		TableScriptsContentProvider columnViewContentProvider = new TableScriptsContentProvider(t, IRepository.METHODS);
		treeViewer.setContentProvider(columnViewContentProvider);
		rows = new ArrayList<Solution>();
		try
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(
				flattenedSolution.getSolution().getName());
			if (servoyProject != null)
			{
				Solution solution = servoyProject.getEditingSolution();
				rows.add(solution);
				Solution[] modules = flattenedSolution.getModules();
				if (modules != null && modules.length > 0)
				{
					for (Solution module : modules)
					{
						ServoyProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(module.getName());
						if (project != null)
						{
							solution = (Solution)project.getEditingPersist(module.getUUID());
							if (solution != null)
							{
								rows.add(solution);
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}

		treeViewer.setInput(rows);
		treeViewer.expandAll();
	}
}
