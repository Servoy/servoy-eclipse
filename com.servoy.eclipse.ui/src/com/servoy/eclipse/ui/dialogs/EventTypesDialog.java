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
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
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

	private final Object value;
	private final List<EventType> model;

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

		Composite container = new Composite(myScrolledComposite, SWT.NONE);
		myScrolledComposite.setContent(container);
		container.setLayout(new GridLayout(1, false));

		Composite tableContainer = new Composite(container, SWT.NONE);

		myScrolledComposite.setContent(container);

		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.verticalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		tableContainer.setLayoutData(gd);

		TableViewer tableViewer = new TableViewer(tableContainer, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		Table columnTable = tableViewer.getTable();
		columnTable.setLinesVisible(true);
		columnTable.setHeaderVisible(true);

		TableColumn nameColumn = new TableColumn(columnTable, SWT.LEFT, CI_EVENT_TYPE_NAME);
		nameColumn.setText("Event Type");

		final TableColumn deleteColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER, CI_DELETE);

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
		layout.setColumnData(nameColumn, new ColumnWeightData(1, 350, true));
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

		Button addEventType = new Button(container, SWT.NONE);
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
					model.add(new EventType(dialog.getValue()));
					tableViewer.refresh();
					getShell().layout(true, true);
				}
			}
		});
		// make sure dialog is big enough to clearly see the table items
		myScrolledComposite.setMinSize(500, 500);
		return myScrolledComposite;
	}

	private List<EventType> fromJSONToModel()
	{
		List<EventType> retValue = new ArrayList<EventType>();
		if (value instanceof JSONObject json)
		{
			json.keySet().stream().forEach(name -> retValue.add(new EventType(name)));
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
				JSONObject value = new JSONObject();
				value.put("name", eventType.getName());
				json.put(eventType.getName(), value);
			}
		}
		return json;
	}

	/**
	 * @return
	 */
	public Object getValue()
	{
		return toJSONObject();
	}
}
