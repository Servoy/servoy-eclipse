/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

package com.servoy.eclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.json.JSONObject;

import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.scripting.info.EventType;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * @author lvostinar
 *
 */
public class EventTypesDialog extends Dialog
{
	public static final int CI_EVENT_TYPE_NAME = 0;
	public static final int CI_DELETE = 1;

	private static final String[] BASIC_TYPES = new String[] { "Boolean", "Date", "Number", "String" };

	private boolean initializingData = false;
	private final Object value;
	private final List<EventType> model;
	private EventType selectedEventType;
	private Combo persistLinkCombo;
	private Text txtDescription;
	private Combo returnTypeCombo;
	private Text txtReturnDescription;
	private Combo parameterTypeCombo1;
	private Combo parameterTypeCombo2;
	private Combo parameterTypeCombo3;
	private Text txtParameterName1;
	private Text txtParameterName2;
	private Text txtParameterName3;

	public EventTypesDialog(Shell shell, Object value)
	{
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		this.value = value;
		model = fromJSONToModel();
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite composite = (Composite)super.createDialogArea(parent);
		getShell().setText("Edit Application Event Types");

		ScrolledComposite myScrolledComposite = new ScrolledComposite(composite, SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);

		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.verticalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		myScrolledComposite.setLayoutData(gd);

		Composite mainContainer = new Composite(myScrolledComposite, SWT.NONE);
		mainContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		mainContainer.setLayout(new GridLayout(5, false));

		Composite tableContainer = new Composite(mainContainer, SWT.NONE);
		tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		myScrolledComposite.setContent(mainContainer);


		TableViewer tableViewer = new TableViewer(tableContainer, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		Table columnTable = tableViewer.getTable();
		columnTable.setLinesVisible(true);
		columnTable.setHeaderVisible(true);

		TableColumn nameColumn = new TableColumn(columnTable, SWT.LEFT, CI_EVENT_TYPE_NAME);
		nameColumn.setText("Event Type");
		TableViewerColumn nameViewerColumn = new TableViewerColumn(tableViewer, nameColumn);
		nameViewerColumn.setEditingSupport(new EditingSupport(tableViewer)
		{
			private final TextCellEditor editor = new TextCellEditor(tableViewer.getTable());

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

			@Override
			protected Object getValue(Object element)
			{
				return ((EventType)element).getName();
			}

			@Override
			protected void setValue(Object element, Object value)
			{
				if (IdentDocumentValidator.isJavaIdentifier(value.toString()))
				{
					((EventType)element).updateName(value.toString());
				}
				getViewer().update(element, null);
			}

		});

		final TableColumn deleteColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER, CI_DELETE);

		tableViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				StructuredSelection newSelection = (StructuredSelection)event.getSelection();
				selectedEventType = null;
				if (newSelection != null && !newSelection.isEmpty() && newSelection.getFirstElement() instanceof EventType eventType)
				{
					selectedEventType = eventType;
				}
				initializeUIInformation();
			}
		});

		tableViewer.getTable().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDown(MouseEvent event)
			{
				Point pt = new Point(event.x, event.y);
				TableItem item = tableViewer.getTable().getItem(pt);
				if (item != null && item.getBounds(CI_DELETE).contains(pt))
				{
					Object eventType = item.getData();
					if (eventType instanceof EventType eType &&
						MessageDialog.openConfirm(getShell(), "Delete event type",
							"Are you sure you want to delete event type '" + eType.getName() + "'?"))
					{
						model.remove(eType);
						tableViewer.refresh();
						getShell().layout(true, true);
					}

				}
			}
		});

		TableColumnLayout layout = new TableColumnLayout();
		tableContainer.setLayout(layout);
		layout.setColumnData(nameColumn, new ColumnWeightData(1, 50, true));
		layout.setColumnData(deleteColumn, new ColumnPixelData(20, false));

		tableViewer.setLabelProvider(new ITableLabelProvider()
		{
			public Image getColumnImage(Object element, int columnIndex)
			{
				if (columnIndex == CI_DELETE)
				{
					return Activator.getDefault().loadImageFromBundle("delete.png");
				}
				return null;
			}

			public String getColumnText(Object element, int columnIndex)
			{
				if (columnIndex == CI_EVENT_TYPE_NAME) return ((EventType)element).getName();
				return null;
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
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setInput(model);

		Button addEventType = new Button(composite, SWT.NONE);
		addEventType.setText("Add New Event Type");
		addEventType.setToolTipText("Adds a new event type");
		addEventType.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				InputDialog dialog = new InputDialog(getShell(), "Add Event Type", "Event Name", "",
					new IInputValidator()
					{
						public String isValid(String newText)
						{
							boolean valid = IdentDocumentValidator.isJavaIdentifier(newText);
							return valid ? null : (newText.length() == 0 ? "" : "Invalid event type name");
						}
					});
				if (dialog.open() == Window.OK)
				{
					model.add(new EventType(dialog.getValue(), null));
					tableViewer.refresh();
					getShell().layout(true, true);
				}
			}
		});

		Group generalPropertiesGroup = new Group(mainContainer, SWT.NONE);
		generalPropertiesGroup.setText("Event Type Signature Properties");
		generalPropertiesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		generalPropertiesGroup.setLayout(new GridLayout(4, false));

		Label label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("UI Event");
		persistLinkCombo = new Combo(generalPropertiesGroup, SWT.BORDER | SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		persistLinkCombo.setItems(new String[] { "-none-", "Form", "Solution" });
		persistLinkCombo.setLayoutData(data);
		persistLinkCombo.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (selectedEventType != null && !initializingData)
				{
					selectedEventType.setPersistLink(persistLinkCombo.getText());
				}
			}
		});

		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Description");
		txtDescription = new Text(generalPropertiesGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		txtDescription.addModifyListener(e -> {
			if (selectedEventType != null && !initializingData)
			{
				selectedEventType.setDescription(txtDescription.getText());
			}
		});
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 200;
		data.horizontalSpan = 3;
		txtDescription.setLayoutData(data);

		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Return Type");
		returnTypeCombo = new Combo(generalPropertiesGroup, SWT.BORDER);
		returnTypeCombo.setItems(BASIC_TYPES);
		returnTypeCombo.addModifyListener(e -> {
			if (selectedEventType != null && !initializingData)
			{
				selectedEventType.setReturnType(returnTypeCombo.getText());
			}
		});
		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Name");

		txtReturnDescription = new Text(generalPropertiesGroup, SWT.BORDER);
		txtReturnDescription.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		txtReturnDescription.addModifyListener(e -> {
			if (selectedEventType != null && !initializingData)
			{
				selectedEventType.setReturnTypeDescription(txtReturnDescription.getText());
			}
		});

		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Argument1 Type");
		parameterTypeCombo1 = new Combo(generalPropertiesGroup, SWT.BORDER);
		parameterTypeCombo1.setItems(BASIC_TYPES);
		parameterTypeCombo1.addModifyListener(e -> {
			if (selectedEventType != null && !initializingData)
			{
				selectedEventType.setArgumentType(0, parameterTypeCombo1.getText());
			}
		});
		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Name");
		txtParameterName1 = new Text(generalPropertiesGroup, SWT.BORDER);
		txtParameterName1.addModifyListener(e -> {
			if (selectedEventType != null && !initializingData)
			{
				selectedEventType.setArgumentName(0, txtParameterName1.getText());
			}
		});
		txtParameterName1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Argument2 Type");
		parameterTypeCombo2 = new Combo(generalPropertiesGroup, SWT.BORDER);
		parameterTypeCombo2.setItems(BASIC_TYPES);
		parameterTypeCombo2.addModifyListener(e -> {
			if (selectedEventType != null && !initializingData)
			{
				selectedEventType.setArgumentType(1, parameterTypeCombo2.getText());
			}
		});
		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Name");
		txtParameterName2 = new Text(generalPropertiesGroup, SWT.BORDER);
		txtParameterName2.addModifyListener(e -> {
			if (selectedEventType != null && !initializingData)
			{
				selectedEventType.setArgumentName(1, txtParameterName2.getText());
			}
		});
		txtParameterName2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Argument3 Type");
		parameterTypeCombo3 = new Combo(generalPropertiesGroup, SWT.BORDER);
		parameterTypeCombo3.setItems(BASIC_TYPES);
		parameterTypeCombo3.addModifyListener(e -> {
			if (selectedEventType != null && !initializingData)
			{
				selectedEventType.setArgumentType(2, parameterTypeCombo3.getText());
			}
		});
		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Name");
		txtParameterName3 = new Text(generalPropertiesGroup, SWT.BORDER);
		txtParameterName3.addModifyListener(e -> {
			if (selectedEventType != null && !initializingData)
			{
				selectedEventType.setArgumentName(2, txtParameterName3.getText());
			}
		});
		txtParameterName3.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return composite;
	}

	private void initializeUIInformation()
	{
		initializingData = true;
		persistLinkCombo.select(0);
		txtDescription.setText("");
		returnTypeCombo.setText("");
		txtReturnDescription.setText("");
		parameterTypeCombo1.setText("");
		txtParameterName1.setText("");
		parameterTypeCombo2.setText("");
		txtParameterName2.setText("");
		parameterTypeCombo3.setText("");
		txtParameterName3.setText("");
		if (selectedEventType != null)
		{
			if (selectedEventType.getPersistLink() != null)
			{
				persistLinkCombo.setText(selectedEventType.getPersistLink());
			}
			if (selectedEventType.getDescription() != null)
			{
				txtDescription.setText(selectedEventType.getDescription());
			}
			if (selectedEventType.getReturnType() != null)
			{
				returnTypeCombo.setText(selectedEventType.getReturnType());
			}
			if (selectedEventType.getReturnTypeDescription() != null)
			{
				txtReturnDescription.setText(selectedEventType.getReturnTypeDescription());
			}
			if (selectedEventType.getArgumentType(0) != null)
			{
				parameterTypeCombo1.setText(selectedEventType.getArgumentType(0));
			}
			if (selectedEventType.getArgumentName(0) != null)
			{
				txtParameterName1.setText(selectedEventType.getArgumentName(0));
			}
			if (selectedEventType.getArgumentType(1) != null)
			{
				parameterTypeCombo2.setText(selectedEventType.getArgumentType(1));
			}
			if (selectedEventType.getArgumentName(1) != null)
			{
				txtParameterName2.setText(selectedEventType.getArgumentName(1));
			}
			if (selectedEventType.getArgumentType(2) != null)
			{
				parameterTypeCombo3.setText(selectedEventType.getArgumentType(2));
			}
			if (selectedEventType.getArgumentName(2) != null)
			{
				txtParameterName3.setText(selectedEventType.getArgumentName(2));
			}
		}
		initializingData = false;
	}

	private List<EventType> fromJSONToModel()
	{
		List<EventType> retValue = new ArrayList<EventType>();
		if (value instanceof JSONObject json)
		{
			json.keySet().stream().forEach(name -> retValue.add(new EventType(name, json.getJSONObject(name))));
		}
		return retValue;
	}

	private JSONObject toJSONObject()
	{
		JSONObject json = new JSONObject();
		if (model != null)
		{
			for (EventType eventType : model)
			{
				json.put(eventType.getName(), eventType.toJSONObject());
			}
		}
		return json;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell shell)
	{
		super.configureShell(shell);
		shell.setSize(800, 550);
		// Center the dialog
		Rectangle screenSize = Display.getDefault().getBounds();
		int x = (screenSize.width - shell.getSize().x) / 2;
		int y = (screenSize.height - shell.getSize().y) / 2;
		shell.setLocation(x, y);
	}

	/**
	 * @return
	 */
	public Object getValue()
	{
		return toJSONObject();
	}
}
