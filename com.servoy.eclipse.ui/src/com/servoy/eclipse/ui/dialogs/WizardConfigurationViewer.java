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

package com.servoy.eclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
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
import com.servoy.j2db.util.Utils;

/**
 * @author emera
 */
public class WizardConfigurationViewer extends TableViewer
{
	private ITable table;

	public abstract class CentredImageCellLabelProvider extends OwnerDrawLabelProvider
	{
		public CentredImageCellLabelProvider()
		{
			super();
		}

		@Override
		protected void measure(Event event, Object element)
		{
			// No action
			event.height = 40;
		}

		@Override
		protected void erase(Event event, Object element)
		{
			// Don't call super.erase() to suppress non-standard selection draw
		}

		@Override
		protected void paint(Event event, Object element)
		{
			TableItem item = (TableItem)event.item;

			Rectangle itemBounds = item.getBounds(event.index);

			GC gc = event.gc;

			Image image = getImage(element);

			Rectangle imageBounds = image.getBounds();

			int x = event.x + Math.max(0, (itemBounds.width - imageBounds.width) / 2);
			int y = event.y + Math.max(0, (itemBounds.height - imageBounds.height) / 2);

			gc.drawImage(image, x, y);
		}

		protected abstract Image getImage(Object element);
	}

	private final class DataproviderCellEditor extends EditingSupport
	{

		private final PropertyDescription dp;
		private final CheckboxCellEditor checkboxCellEditor;
		private final List<PropertyDescription> dataproviderProperties;

		private DataproviderCellEditor(ColumnViewer viewer, int style, PropertyDescription dp,
			List<PropertyDescription> dataproviderProperties)
		{
			super(viewer);
			this.dp = dp;
			this.dataproviderProperties = dataproviderProperties;
			checkboxCellEditor = new CheckboxCellEditor(getTable(), style);
		}

		@Override
		protected CellEditor getCellEditor(Object element)
		{
			return checkboxCellEditor;
		}

		@Override
		protected boolean canEdit(Object element)
		{
			return true;
		}

		@Override
		protected Object getValue(Object element)
		{
			Pair<String, Map<String, Object>> row = (Pair<String, Map<String, Object>>)element;
			return row.getRight().get(dp.getName()) != null; //TODO check equals pd.getDefaultValue()?
		}

		@Override
		protected void setValue(Object element, Object value)
		{
			Pair<String, Map<String, Object>> row = (Pair<String, Map<String, Object>>)element;
			Map<String, Object> rowValue = row.getRight();
			if (Utils.getAsBoolean(value))
			{
				for (PropertyDescription pd : dataproviderProperties)
				{
					if (!pd.getName().equals(dp.getName()))
					{
						rowValue.put(pd.getName(), pd.getDefaultValue());
					}
					else
					{
						String dpValue = row.getLeft();
						rowValue.put(dp.getName(), dpValue);
					}
				}
			}
			getViewer().update(element, null);
		}
	}

	private final class TextCellEditorSupport extends EditingSupport
	{

		private final PropertyDescription dp;
		private TextCellEditor textCellEditor;

		private TextCellEditorSupport(ColumnViewer viewer, PropertyDescription dp, TextCellEditor editor)
		{
			super(viewer);
			this.dp = dp;
			textCellEditor = editor;
		}

		@Override
		protected CellEditor getCellEditor(Object element)
		{
			return textCellEditor;
		}

		@Override
		protected boolean canEdit(Object element)
		{
			return true;
		}

		@Override
		protected Object getValue(Object element)
		{
			Pair<String, Map<String, Object>> row = (Pair<String, Map<String, Object>>)element;
			Object value = row.getRight().get(dp.getName());
			return value == null ? "" : value.toString();
		}

		@Override
		protected void setValue(Object element, Object value)
		{
			Pair<String, Map<String, Object>> row = (Pair<String, Map<String, Object>>)element;
			Map<String, Object> rowValue = row.getRight();
			rowValue.put(dp.getName(), value);
			getViewer().update(element, null);
		}
	}

	/**
	 * @author jcomp
	 *
	 */
	private static final class TextColumnLabelProvider extends ColumnLabelProvider
	{
		private final PropertyDescription dp;

		/**
		 * @param dp
		 */
		public TextColumnLabelProvider(PropertyDescription dp)
		{
			this.dp = dp;
		}

		@Override
		public String getText(Object element)
		{
			Pair<String, Map<String, Object>> row = (Pair<String, Map<String, Object>>)element;
			Object value = row.getRight().get(dp.getName());
			return value == null ? "" : value.toString();
		}

		@Override
		public Image getImage(Object element)
		{
			return null;
		}
	}

	private final List<TextCellEditorSupport> i18nColumns = new ArrayList<>();
	private final PersistContext persistContext;
	private final FlattenedSolution fs;

	public WizardConfigurationViewer(Composite parent, PersistContext persistContext, FlattenedSolution fs, ITable table,
		List<PropertyDescription> dataproviderProperties,
		List<PropertyDescription> styleClassProperties,
		List<PropertyDescription> i18nProperties, List<PropertyDescription> stringProperties, int style)
	{
		super(parent, style);
		this.persistContext = persistContext;
		this.fs = fs;
		this.table = table;
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		parent.setLayout(tableColumnLayout);
		getTable().setLinesVisible(true);
		getTable().setHeaderVisible(true);
		getTable().setToolTipText("The selected columns");
		setContentProvider(ArrayContentProvider.getInstance());

		TableColumn dataproviderColumn = new TableColumn(getTable(), SWT.LEFT);
		dataproviderColumn.setText("Columns");
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
			colViewer.setEditingSupport(new DataproviderCellEditor(this, style, dp, dataproviderProperties));
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