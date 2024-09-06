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
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.editors.table.ColumnLabelProvider;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.persistence.MenuItem;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Pair;

/**
 * @author lvostinar
 *
 */
public class PermissionsDialog extends Dialog
{
	public static final int CI_PERMISSION_NAME = 0;
	public static final int CI_VIEWABLE = 1;
	public static final int CI_ENABLED = 2;

	private final Object value;
	private PermissionsModel model;

	public PermissionsDialog(Shell shell, Object value)
	{
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		this.value = value;
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText("Edit Menu Item Permissions");

		Composite composite = (Composite)super.createDialogArea(parent);

		Composite tableContainer = new Composite(composite, SWT.NONE);

		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.verticalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		tableContainer.setLayoutData(gd);

		TableViewer tableViewer = new TableViewer(tableContainer, SWT.V_SCROLL | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		Table columnTable = tableViewer.getTable();
		columnTable.setLinesVisible(true);
		columnTable.setHeaderVisible(true);

		TableColumn nameColumn = new TableColumn(columnTable, SWT.LEFT, CI_PERMISSION_NAME);
		nameColumn.setText("Permission");

		final TableColumn viewableColumn = new TableColumn(columnTable, SWT.CENTER, CI_VIEWABLE);
		viewableColumn.setText("Visible");
		TableViewerColumn viewableViewerColumn = new TableViewerColumn(tableViewer, viewableColumn);
		viewableViewerColumn.setEditingSupport(new PermissionEditingSupport(tableViewer, MenuItem.VIEWABLE));

		final TableColumn enabledColumn = new TableColumn(columnTable, SWT.CENTER, CI_ENABLED);
		enabledColumn.setText("Enabled");
		TableViewerColumn enabledViewerColumn = new TableViewerColumn(tableViewer, enabledColumn);
		enabledViewerColumn.setEditingSupport(new PermissionEditingSupport(tableViewer, MenuItem.ENABLED));

		TableColumnLayout layout = new TableColumnLayout();
		tableContainer.setLayout(layout);
		layout.setColumnData(nameColumn, new ColumnWeightData(1, 350, true));
		layout.setColumnData(viewableColumn, new ColumnPixelData(70, true));
		layout.setColumnData(enabledColumn, new ColumnPixelData(70, true));

		model = new PermissionsModel(value);

		tableViewer.setLabelProvider(new ITableLabelProvider()
		{
			public Image getColumnImage(Object element, int columnIndex)
			{
				if (columnIndex == CI_VIEWABLE)
				{
					boolean isViewable = (((Pair<String, Integer>)element).getRight() & MenuItem.VIEWABLE) > 0;
					return isViewable ? ColumnLabelProvider.TRUE_RADIO : ColumnLabelProvider.FALSE_RADIO;
				}
				if (columnIndex == CI_ENABLED)
				{
					boolean isEnabled = (((Pair<String, Integer>)element).getRight() & MenuItem.ENABLED) > 0;
					return isEnabled ? ColumnLabelProvider.TRUE_RADIO : ColumnLabelProvider.FALSE_RADIO;
				}
				return null;
			}

			public String getColumnText(Object element, int columnIndex)
			{
				if (columnIndex == CI_PERMISSION_NAME) return ((Pair<String, Integer>)element).getLeft();
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
		tableViewer.setInput(model.getTableData());
		return composite;
	}

	/**
	 * @return
	 */
	public Object getValue()
	{
		return model.getValue();
	}

	public class PermissionsModel
	{
		private final List<Pair<String, Integer>> tableColumns;

		public PermissionsModel(Object initialData)
		{
			tableColumns = new ArrayList<Pair<String, Integer>>();
			IDataSet groups = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager()
				.getGroups(ApplicationServerRegistry.get().getClientId());
			for (int i = 0; i < groups.getRowCount(); i++)
			{
				String groupName = groups.getRow(i)[1].toString();
				int right = MenuItem.VIEWABLE + MenuItem.ENABLED;
				if (initialData != null)
				{
					right = 0;
					if (initialData instanceof JSONObject json && json.has(groupName))
					{
						right = json.optInt(groupName);
					}
				}
				tableColumns.add(new Pair<String, Integer>(groupName, right));
			}
		}

		public Pair<String, Integer>[] getTableData()
		{
			return tableColumns.toArray(new Pair[tableColumns.size()]);
		}


		public JSONObject getValue()
		{
			JSONObject json = new JSONObject();
			for (Pair<String, Integer> pair : tableColumns)
			{
				json.put(pair.getLeft(), pair.getRight());
			}
			return json;
		}
	}

	public class PermissionEditingSupport extends EditingSupport
	{
		private final CellEditor editor;
		private final int flag;

		public PermissionEditingSupport(TableViewer tv, int flag)
		{
			super(tv);
			editor = new CheckboxCellEditor(tv.getTable());
			this.flag = flag;
		}

		@Override
		protected void setValue(Object element, Object value)
		{
			if (element instanceof Pair)
			{
				Pair<String, Integer> permissionPair = (Pair<String, Integer>)element;
				Boolean permission = Boolean.parseBoolean(value.toString());
				if (permission)
				{
					permissionPair.setRight(permissionPair.getRight() | flag);
				}
				else
				{
					permissionPair.setRight(permissionPair.getRight() & ~flag);
				}
				getViewer().update(element, null);
			}
		}

		@Override
		protected Object getValue(Object element)
		{
			if (element instanceof Pair)
			{
				Pair<String, Integer> permissionPair = (Pair<String, Integer>)element;
				return (permissionPair.getRight() & flag) > 0;
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
