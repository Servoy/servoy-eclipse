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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.swt.custom.CCombo;
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
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.FixedComboBoxCellEditor;
import com.servoy.j2db.scripting.info.EventType;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * @author lvostinar
 *
 */
public class EventTypesDialog extends Dialog
{
	public static final int CI_EVENT_TYPE_NAME = 0;
	public static final int CI_DELETE = 1;

	public static final int CI_PARAMETER_NAME = 0;
	public static final int CI_PARAMETER_TYPE = 1;
	public static final int CI_PARAMETER_DELETE = 2;

	private static final String[] BASIC_TYPES = new String[] { "Object", "Boolean", "Date", "Number", "String" };

	private boolean initializingData = false;
	private final Object value;
	private final List<EventType> model;
	private EventType selectedEventType;
	private Combo persistLinkCombo;
	private Text txtDescription;
	private Combo returnTypeCombo;
	private Text txtReturnDescription;
	private TableViewer argumentsTableViewer;

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

		TableColumn deleteColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER, CI_DELETE);

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
					EventType newEventType = new EventType(dialog.getValue(), null);
					model.add(newEventType);
					tableViewer.refresh();
					tableViewer.setSelection(new StructuredSelection(newEventType));
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
		data.heightHint = 100;
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
		label.setText("Description");

		txtReturnDescription = new Text(generalPropertiesGroup, SWT.BORDER);
		txtReturnDescription.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		txtReturnDescription.addModifyListener(e -> {
			if (selectedEventType != null && !initializingData)
			{
				selectedEventType.setReturnTypeDescription(txtReturnDescription.getText());
			}
		});

		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Arguments");

		tableContainer = new Composite(generalPropertiesGroup, SWT.NONE);
		tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

		argumentsTableViewer = new TableViewer(tableContainer, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		columnTable = argumentsTableViewer.getTable();
		columnTable.setLinesVisible(true);
		columnTable.setHeaderVisible(true);

		nameColumn = new TableColumn(columnTable, SWT.LEFT, CI_PARAMETER_NAME);
		nameColumn.setText("Name");
		nameViewerColumn = new TableViewerColumn(argumentsTableViewer, nameColumn);
		nameViewerColumn.setEditingSupport(new EditingSupport(argumentsTableViewer)
		{
			private final TextCellEditor editor = new TextCellEditor(argumentsTableViewer.getTable());

			@Override
			protected CellEditor getCellEditor(Object element)
			{
				return editor;
			}

			@Override
			protected boolean canEdit(Object element)
			{
				return selectedEventType != null && selectedEventType.getArguments().indexOf(element) > 0;
			}

			@Override
			protected Object getValue(Object element)
			{
				return ((Pair<String, String>)element).getLeft();
			}

			@Override
			protected void setValue(Object element, Object value)
			{
				if (!Utils.equalObjects(((Pair<String, String>)element).getLeft(), value))
				{
					if (value == null || value.toString().length() == 0)
					{
						MessageDialog.openError(getShell(), "Invalid Argument Name", "Argument name can't be empty");
						return;
					}
					if (!IdentDocumentValidator.isJavaIdentifier(value.toString()))
					{
						MessageDialog.openError(getShell(), "Invalid Argument Name", "Must be valid identifier");
						return;
					}
					if (selectedEventType.getArguments().stream().anyMatch(arg -> value.toString().equals(arg.getLeft())))
					{
						MessageDialog.openError(getShell(), "Invalid Argument Name", "Argument name already exists");
						return;
					}
					((Pair<String, String>)element).setLeft(value.toString());
					getViewer().update(element, null);
				}
			}

		});

		TableColumn typeColumn = new TableColumn(columnTable, SWT.LEFT, CI_PARAMETER_TYPE);
		typeColumn.setText("Type");
		TableViewerColumn typeViewerColumn = new TableViewerColumn(argumentsTableViewer, typeColumn);
		typeViewerColumn.setEditingSupport(new EditingSupport(argumentsTableViewer)
		{
			private final FixedComboBoxCellEditor editor = new FixedComboBoxCellEditor(argumentsTableViewer.getTable(), BASIC_TYPES, SWT.BORDER);

			@Override
			protected CellEditor getCellEditor(Object element)
			{
				CCombo combo = (CCombo)editor.getControl();
				combo.setItems(BASIC_TYPES);
				return editor;
			}

			@Override
			protected boolean canEdit(Object element)
			{
				return selectedEventType != null && selectedEventType.getArguments().indexOf(element) > 0;
			}

			@Override
			protected Object getValue(Object element)
			{
				String argumentType = ((Pair<String, String>)element).getRight();
				if (argumentType != null && argumentType.length() > 0)
				{
					int index = Arrays.asList(BASIC_TYPES).indexOf(argumentType);
					if (index >= 0)
					{
						return Integer.valueOf(index);
					}
					else
					{
						List<String> resultList = new ArrayList<>(BASIC_TYPES.length + 1);
						Collections.addAll(resultList, new String[] { argumentType });
						Collections.addAll(resultList, BASIC_TYPES);
						CCombo combo = (CCombo)editor.getControl();
						combo.setItems(resultList.toArray(new String[resultList.size()]));
					}
				}
				return Integer.valueOf(0);
			}

			@Override
			protected void setValue(Object element, Object value)
			{
				CCombo combo = (CCombo)editor.getControl();
				String selectedType = combo.getText();
				if (!IdentDocumentValidator.isJavaIdentifier(selectedType))
				{
					MessageDialog.openError(getShell(), "Invalid Argument Type", "Must be valid identifier");
					return;
				}
				((Pair<String, String>)element).setRight(selectedType);
				getViewer().update(element, null);
			}

		});

		deleteColumn = new TableColumn(argumentsTableViewer.getTable(), SWT.CENTER, CI_PARAMETER_DELETE);

		argumentsTableViewer.getTable().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDown(MouseEvent event)
			{
				Point pt = new Point(event.x, event.y);
				TableItem item = argumentsTableViewer.getTable().getItem(pt);
				if (selectedEventType != null && item != null && item.getBounds(CI_PARAMETER_DELETE).contains(pt))
				{
					Object argument = item.getData();
					if (argument instanceof Pair && selectedEventType.getArguments().indexOf(argument) > 0 &&
						MessageDialog.openConfirm(getShell(), "Delete event type",
							"Are you sure you want to delete argument ?"))
					{
						selectedEventType.getArguments().remove(argument);
						argumentsTableViewer.refresh();
						getShell().layout(true, true);
					}

				}
			}
		});

		layout = new TableColumnLayout();
		tableContainer.setLayout(layout);
		layout.setColumnData(nameColumn, new ColumnWeightData(1, 50, true));
		layout.setColumnData(typeColumn, new ColumnWeightData(1, 50, true));
		layout.setColumnData(deleteColumn, new ColumnPixelData(20, false));

		argumentsTableViewer.setLabelProvider(new ITableLabelProvider()
		{
			public Image getColumnImage(Object element, int columnIndex)
			{
				if (columnIndex == CI_PARAMETER_DELETE && selectedEventType != null && selectedEventType.getArguments().indexOf(element) > 0)
				{
					return Activator.getDefault().loadImageFromBundle("delete.png");
				}
				return null;
			}

			public String getColumnText(Object element, int columnIndex)
			{
				if (columnIndex == CI_PARAMETER_NAME) return ((Pair<String, String>)element).getLeft();
				if (columnIndex == CI_PARAMETER_TYPE) return ((Pair<String, String>)element).getRight();
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
		argumentsTableViewer.setContentProvider(new ArrayContentProvider());
		argumentsTableViewer.setInput(selectedEventType != null ? selectedEventType.getArguments() : new ArrayList<Pair<String, String>>());

		Button addArgument = new Button(generalPropertiesGroup, SWT.NONE);
		addArgument.setText("Add Argument");
		addArgument.setToolTipText("Adds a new argument to method signature");
		addArgument.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				if (selectedEventType == null)
				{
					return;
				}
				InputDialog dialog = new InputDialog(getShell(), "Add Argument", "Argument Name", "",
					new IInputValidator()
					{
						public String isValid(String newText)
						{
							if (newText == null || newText.length() == 0)
							{
								return "Please enter an argument name";
							}
							if (!IdentDocumentValidator.isJavaIdentifier(newText))
							{
								return "Invalid argument name";
							}
							if (selectedEventType.getArguments().stream().anyMatch(arg -> Utils.equalObjects(arg.getLeft(), newText)))
							{
								return "Argument name already exists";
							}
							return null;
						}
					});
				if (dialog.open() == Window.OK)
				{
					selectedEventType.getArguments().add(new Pair<String, String>(dialog.getValue(), ""));
					argumentsTableViewer.refresh();
					getShell().layout(true, true);
				}
			}
		});
		if (model.size() > 0)
		{
			tableViewer.setSelection(new StructuredSelection(model.get(0)));
		}
		return composite;
	}

	private void initializeUIInformation()
	{
		initializingData = true;
		persistLinkCombo.select(0);
		txtDescription.setText("");
		returnTypeCombo.setText("");
		txtReturnDescription.setText("");
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
		}
		argumentsTableViewer.setInput(selectedEventType != null ? selectedEventType.getArguments() : new ArrayList<Pair<String, String>>());
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
		// Center the dialog
		Rectangle screenSize = Display.getDefault().getBounds();
		int x = (screenSize.width - shell.getSize().x) / 2;
		int y = (screenSize.height - shell.getSize().y) / 2;
		shell.setLocation(x, y);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings()
	{
		IDialogSettings settings = EditorUtil.getDialogSettings("EventTypesDialog");
		if (settings.get("DIALOG_WIDTH") == null)
		{
			settings.put("DIALOG_WIDTH", 800);
		}
		if (settings.get("DIALOG_HEIGHT") == null)
		{
			settings.put("DIALOG_HEIGHT", 550);
		}
		return settings;
	}

	/**
	 * @return
	 */
	public Object getValue()
	{
		return toJSONObject();
	}
}
