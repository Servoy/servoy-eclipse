/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.eclipse.ui.dialogs.autowizard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.ui.editors.TagsAndI18NTextCellEditor;
import com.servoy.eclipse.ui.labelproviders.DefaultValueDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.TextCutoffLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.util.Pair;

/**
 * @author emera
 */
public class AutoWizardConfigurationViewer extends TableViewer
{
	ITable table;

	private final List<TextCellEditorSupport> i18nColumns = new ArrayList<>();
	private final PersistContext persistContext;
	private final FlattenedSolution fs;

	public AutoWizardConfigurationViewer(Composite parent, PersistContext persistContext, FlattenedSolution fs, ITable table,
		List<PropertyDescription> dataproviderProperties,
		List<PropertyDescription> styleClassProperties,
		List<PropertyDescription> i18nProperties, List<PropertyDescription> stringProperties, int style, String propertyName)
	{
		super(parent, style);
		this.persistContext = persistContext;
		this.fs = fs;
		this.table = table;
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		parent.setLayout(tableColumnLayout);
		getTable().setLinesVisible(true);
		getTable().setHeaderVisible(true);
		getTable().setToolTipText("The selected " + propertyName);
		setContentProvider(ArrayContentProvider.getInstance());

		TableColumn dataproviderColumn = new TableColumn(getTable(), SWT.LEFT);
		dataproviderColumn.setText(propertyName);
		dataproviderColumn.setToolTipText("The dataprovider for which a column is created");

		TableViewerColumn dataproviderViewerColumn = new TableViewerColumn(this, dataproviderColumn);
		dataproviderViewerColumn.setLabelProvider(new ColumnLabelProvider()
		{
			@Override
			public String getText(Object element)
			{
				Pair<String, Map<String, Object>> row = (Pair<String, Map<String, Object>>)element;
				return row.getLeft();
			}
		});
		tableColumnLayout.setColumnData(dataproviderColumn, new ColumnWeightData(40, 100, true));

		for (PropertyDescription dp : dataproviderProperties)
		{
			TableColumn col = new TableColumn(getTable(), SWT.CENTER);
			col.setText(dp.getName());
			col.setToolTipText(dp.getDocumentation());

			TableViewerColumn colViewer = new TableViewerColumn(this, col);
			colViewer.setEditingSupport(new DataproviderCellEditor(this, this, style, dp, dataproviderProperties));
			colViewer.setLabelProvider(new CentredImageCellLabelProvider()
			{
				@Override
				public Image getImage(Object element)
				{
					Pair<String, Map<String, Object>> row = (Pair<String, Map<String, Object>>)element;
					return row.getRight().get(dp.getName()) != null ? com.servoy.eclipse.ui.editors.table.ColumnLabelProvider.TRUE_RADIO
						: com.servoy.eclipse.ui.editors.table.ColumnLabelProvider.FALSE_RADIO;
				}
			});
			tableColumnLayout.setColumnData(col, new ColumnWeightData(40, 100, true));
		}

		for (PropertyDescription dp : styleClassProperties)
		{
			TableColumn col = new TableColumn(getTable(), SWT.CENTER);
			col.setText(dp.getName());
			col.setToolTipText(dp.getDocumentation());

			TableViewerColumn colViewer = new TableViewerColumn(this, col);
			colViewer.setEditingSupport(new TextCellEditorSupport(this, dp, new TextCellEditor(getTable())));
			colViewer.setLabelProvider(new TextColumnLabelProvider(dp));
			tableColumnLayout.setColumnData(col, new ColumnWeightData(40, 100, true));
		}

		for (PropertyDescription dp : i18nProperties)
		{
			TableColumn col = new TableColumn(getTable(), SWT.CENTER);
			col.setText(dp.getName());
			col.setToolTipText(dp.getDocumentation());

			TableViewerColumn colViewer = new TableViewerColumn(this, col);
			TextCellEditorSupport editingSupport = new TextCellEditorSupport(this, dp,
				new TagsAndI18NTextCellEditor(getTable(), persistContext, fs, new DefaultValueDelegateLabelProvider(TextCutoffLabelProvider.DEFAULT), table,
					"Edit title/text property", Activator.getDefault().getDesignClient(), false));
			colViewer.setEditingSupport(editingSupport);
			i18nColumns.add(editingSupport);
			colViewer.setLabelProvider(new TextColumnLabelProvider(dp));
			tableColumnLayout.setColumnData(col, new ColumnWeightData(40, 100, true));
		}

		for (PropertyDescription dp : stringProperties)
		{
			Object tag = dp.getTag("wizard");
			if (tag instanceof JSONObject)
			{
				String prefillProperty = ((JSONObject)tag).getString("prefill");
				if (prefillProperty != null)
				{
					continue;
				}
			}
			TableColumn col = new TableColumn(getTable(), SWT.CENTER);
			col.setText(dp.getName());
			col.setToolTipText(dp.getDocumentation());

			TableViewerColumn colViewer = new TableViewerColumn(this, col);
			colViewer.setEditingSupport(new TextCellEditorSupport(this, dp, new TextCellEditor(getTable())));
			colViewer.setLabelProvider(new TextColumnLabelProvider(dp));
			tableColumnLayout.setColumnData(col, new ColumnWeightData(40, 100, true));
		}

		TableColumn delete = new TableColumn(getTable(), SWT.CENTER);
		delete.setText("");
		delete.setToolTipText("Delete the column");
		delete.setData("delete", "true");
		TableViewerColumn deleteViewerColumn = new TableViewerColumn(this, delete);
		deleteViewerColumn.setLabelProvider(new CentredImageCellLabelProvider()
		{
			@Override
			public Image getImage(Object element)
			{
				return com.servoy.eclipse.ui.Activator.getDefault().loadImageFromBundle("delete.png");
			}
		});
		getTable().addListener(SWT.MouseDown, new Listener()
		{
			public void handleEvent(Event event)
			{
				Rectangle clientArea = getTable().getClientArea();
				Point pt = new Point(event.x, event.y);
				int index = getTable().getTopIndex();
				while (index < getTable().getItemCount())
				{
					boolean visible = false;
					TableItem item = getTable().getItem(index);
					for (int i = 0; i < getTable().getColumnCount(); i++)
					{
						Rectangle rect = item.getBounds(i);
						if (rect.contains(pt))
						{
							if (i == getTable().getColumnCount() - 1)
							{
								List<Pair<String, Map<String, Object>>> input = (List<Pair<String, Map<String, Object>>>)getInput();
								input.remove(index);
								refresh();
							}
						}
						if (!visible && rect.intersects(clientArea))
						{
							visible = true;
						}
					}
					if (!visible)
						return;
					index++;
				}
			}
		});
		tableColumnLayout.setColumnData(delete, new ColumnWeightData(40, 100, true));


	}

	/**
	 * @param table
	 */
	public void setTable(ITable table)
	{
		this.table = table;
		i18nColumns.forEach(editingSupport -> editingSupport.textCellEditor = new TagsAndI18NTextCellEditor(getTable(), persistContext, fs,
			new DefaultValueDelegateLabelProvider(TextCutoffLabelProvider.DEFAULT), table,
			"Edit title/text property", Activator.getDefault().getDesignClient(), false));
	}
}