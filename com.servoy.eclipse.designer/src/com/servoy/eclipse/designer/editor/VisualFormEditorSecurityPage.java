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
package com.servoy.eclipse.designer.editor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * Tab in form editor for managing form security.
 *
 * @author lvostinar
 */

public class VisualFormEditorSecurityPage extends Composite
{
	private final BaseVisualFormEditor editor;
	private boolean initialised = false;
	private final TableViewer groupViewer;
	private final TableViewer elementsViewer;
	private final Composite tableContainer;
	private final Composite treeContainer;
	final ElementSettingsModel model;
	private boolean doRefresh;
	private Button btnNORights;

	public VisualFormEditorSecurityPage(BaseVisualFormEditor formEditor, Composite parent, int style)
	{
		super(parent, style);
		setLayout(new FormLayout());
		this.editor = formEditor;
		model = new ElementSettingsModel(formEditor.getForm());

		TableColumnLayout elementslayout = new TableColumnLayout();
		TableColumnLayout layout = new TableColumnLayout();

		btnNORights = new Button(this, SWT.CHECK);
		btnNORights.setText("No rights unless explicitly specified");
		btnNORights.setToolTipText(
			"When this is checked, all permissions will have by default no right to view/access the data of any element, unless it is specified by editor. If unchecked, by default all rights are available.");
		btnNORights.setSelection(formEditor.getForm().getImplicitSecurityNoRights());
		btnNORights.addSelectionListener(new SelectionListener()
		{

			@Override
			public void widgetSelected(SelectionEvent e)
			{
				formEditor.getForm().setImplicitSecurityNoRights(btnNORights.getSelection());
				editor.flagModified();
				doRefresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{

			}
		});
		if (formEditor.getForm().getExtendsForm() != null && formEditor.getForm().getExtendsForm().getImplicitSecurityNoRights())
		{
			// if parent form is explicit, shouldn't be able to change it in child
			btnNORights.setEnabled(false);
		}
		FormData formData = new FormData();
		formData.top = new FormAttachment(0, 10);
		formData.right = new FormAttachment(100, -5);
		btnNORights.setLayoutData(formData);

		Button btnToggleAccessible = new Button(this, SWT.NONE);
		btnToggleAccessible.setText("Toggle Accessible");
		btnToggleAccessible.setToolTipText("Toggle accessible value for all elements in the form of selected permission.");
		btnToggleAccessible.addSelectionListener(new SelectionListener()
		{

			@Override
			public void widgetSelected(SelectionEvent e)
			{
				toggleValues(IRepository.ACCESSIBLE);
				editor.flagModified();
				doRefresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{

			}
		});
		formData = new FormData();
		formData.top = new FormAttachment(0, 10);
		formData.right = new FormAttachment(btnNORights, -5);
		btnToggleAccessible.setLayoutData(formData);

		Button btnToggleViewable = new Button(this, SWT.NONE);
		btnToggleViewable.setText("Toggle Viewable");
		btnToggleViewable.setToolTipText("Toggle viewable value for all elements in the form of selected permission.");
		btnToggleViewable.addSelectionListener(new SelectionListener()
		{

			@Override
			public void widgetSelected(SelectionEvent e)
			{
				toggleValues(IRepository.VIEWABLE);
				editor.flagModified();
				doRefresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{

			}
		});
		formData = new FormData();
		formData.top = new FormAttachment(0, 10);
		formData.right = new FormAttachment(btnToggleAccessible, -5);
		btnToggleViewable.setLayoutData(formData);


		final SashForm sashForm = new SashForm(this, SWT.NONE);
		formData = new FormData();
		formData.top = new FormAttachment(btnToggleViewable, 6);
		formData.left = new FormAttachment(0, 10);
		formData.right = new FormAttachment(100, -5);
		formData.bottom = new FormAttachment(100, -5);
		sashForm.setLayoutData(formData);

		tableContainer = new Composite(sashForm, SWT.NONE);
		groupViewer = new TableViewer(tableContainer, SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		groupViewer.getTable().setHeaderVisible(true);
		groupViewer.getTable().setLinesVisible(true);

		groupViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{

			public void selectionChanged(SelectionChangedEvent event)
			{
				ISelection sel = groupViewer.getSelection();
				if (sel instanceof IStructuredSelection && !sel.isEmpty())
				{
					Object first = ((IStructuredSelection)sel).getFirstElement();
					model.setCurrentGroup(first.toString());
					setElements();
				}

			}
		});

		TableColumn nameColumn = new TableColumn(groupViewer.getTable(), SWT.LEFT, CI_NAME);
		nameColumn.setText("Permission");
		layout.setColumnData(nameColumn, new ColumnWeightData(20, 50, true));
		tableContainer.setLayout(layout);

		groupViewer.setLabelProvider(new ITableLabelProvider()
		{
			public Image getColumnImage(Object element, int columnIndex)
			{
				return null;
			}

			public String getColumnText(Object element, int columnIndex)
			{
				return element.toString();
			}

			public void addListener(ILabelProviderListener listener)
			{
			}

			public void dispose()
			{
			}

			public boolean isLabelProperty(Object element, String property)
			{
				return false;
			}

			public void removeListener(ILabelProviderListener listener)
			{
			}

		});
		groupViewer.setContentProvider(new IStructuredContentProvider()
		{
			public void dispose()
			{
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
			{
			}

			public Object[] getElements(Object inputElement)
			{
				IDataSet localGroups = (IDataSet)inputElement;
				int localGroupsNr = localGroups.getRowCount();
				Object[] groupNames = new Object[localGroupsNr];
				for (int i = 0; i < localGroupsNr; i++)
				{
					groupNames[i] = localGroups.getRow(i)[1];
				}
				return groupNames;
			}
		});
		treeContainer = new Composite(sashForm, SWT.NONE);
		elementsViewer = new TableViewer(treeContainer, SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		elementsViewer.getTable().setHeaderVisible(true);
		elementsViewer.getTable().setLinesVisible(true);

		TableColumn elementColumn = new TableColumn(elementsViewer.getTable(), SWT.LEFT, CI_NAME);
		elementslayout.setColumnData(elementColumn, new ColumnWeightData(20, 50, true));

		final TableColumn viewableColumn = new TableColumn(elementsViewer.getTable(), SWT.CENTER, CI_VIEWABLE);
		elementslayout.setColumnData(viewableColumn, new ColumnPixelData(70, true));
		TableViewerColumn viewableViewerColumn = new TableViewerColumn(elementsViewer, viewableColumn);
		final ElementSettingsEditingSupport viewableEditingSupport = new ElementSettingsEditingSupport(IRepository.VIEWABLE, elementsViewer, model);

		final TableColumn accessibleColumn = new TableColumn(elementsViewer.getTable(), SWT.CENTER, CI_ACCESSABLE);
		elementslayout.setColumnData(accessibleColumn, new ColumnPixelData(70, true));
		TableViewerColumn accessableViewerColumn = new TableViewerColumn(elementsViewer, accessibleColumn);
		final ElementSettingsEditingSupport accessableEditingSupport = new ElementSettingsEditingSupport(IRepository.ACCESSIBLE, elementsViewer, model);

		treeContainer.setLayout(elementslayout);

		elementsViewer.setLabelProvider(new ElementSettingsLabelProvider(model));
		elementsViewer.setContentProvider(new ArrayContentProvider());
		sashForm.setWeights(new int[] { 1, 1 });
		elementColumn.setText("Elements");
		viewableColumn.setText("Viewable");
		viewableViewerColumn.setEditingSupport(viewableEditingSupport);
		accessibleColumn.setText("Accessible");
		accessableViewerColumn.setEditingSupport(accessableEditingSupport);

		viewableEditingSupport.addChangeListener(new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				setElements();
				editor.flagModified();
			}
		});

		accessableEditingSupport.addChangeListener(new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				setElements();
				editor.flagModified();
			}
		});

		initialised = true;
		doRefresh();
		IDataSet groups = (IDataSet)groupViewer.getInput();
		if (groups != null && groups.getRowCount() > 0)
		{
			groupViewer.setSelection(new StructuredSelection(new Object[] { groups.getRow(0)[1] }));
		}
	}

	public static final int CI_NAME = 0;
	public static final int CI_VIEWABLE = 1;
	public static final int CI_ACCESSABLE = 2;

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		if (visible && doRefresh)
		{
			doRefresh();
		}
	}

	public void refresh()
	{
		if (!initialised || isDisposed() || editor.getForm() == null) return;
		if (isVisible())
		{
			doRefresh();
		}
		else
		{
			doRefresh = true;
		}
	}

	public void doRefresh()
	{
		IDataSet groups = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().getGroups(ApplicationServerRegistry.get().getClientId());
		groupViewer.setInput(groups);
		if (groups.getRowCount() == 0)
		{
			elementsViewer.setInput(null);
		}
		else
		{
			setElements();
		}

		doRefresh = false;
	}

	private void setElements()
	{
		List<IPersist> formElements = new ArrayList<IPersist>();
		Form flatForm = ModelUtils.getEditingFlattenedSolution(editor.getForm()).getFlattenedForm(editor.getForm());
		List<IFormElement> elements = flatForm.getFlattenedObjects(NameComparator.INSTANCE);
		for (IFormElement elem : elements)
		{
			if (elem.getName() != null && elem.getName().length() != 0)
			{
				formElements.add(elem);
			}
		}
		formElements.add(0, editor.getForm());
		elementsViewer.setInput(formElements);
	}

	public void saveSecurityElements()
	{
		model.saveSecurityElements();
	}

	private void toggleValues(int mask)
	{
		Iterator< ? extends IPersist> it = editor.getForm().isResponsiveLayout() ? editor.getForm().getFlattenedObjects(NameComparator.INSTANCE).iterator()
			: ModelUtils.getEditingFlattenedSolution(editor.getForm()).getFlattenedForm(editor.getForm()).getAllObjects();
		while (it.hasNext())
		{
			IPersist elem = it.next();
			if (elem instanceof IFormElement && ((IFormElement)elem).getName() != null && ((IFormElement)elem).getName().length() != 0)
			{
				model.setRight(!model.hasRight(elem, mask), elem, mask);
			}
		}
	}

}
