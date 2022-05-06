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
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
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
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.ui.editors.TagsAndI18NTextCellEditor;
import com.servoy.eclipse.ui.labelproviders.DefaultValueDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.FormLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.TextCutoffLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.RelationPropertyController.RelationPropertyEditor;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ITable;

/**
 * @author emera
 */
public class AutoWizardConfigurationViewer extends TableViewer
{
	ITable table;

	private final List<TextCellEditorSupport> i18nColumns = new ArrayList<>();
	private final List<RelationCellEditorSupport> relationColumns = new ArrayList<>();
	private final PersistContext persistContext;
	private final FlattenedSolution fs;
	private PropertyWizardDialogConfigurator propertiesConfig;

	public AutoWizardConfigurationViewer(Composite parent, PersistContext persistContext, FlattenedSolution fs, ITable table,
		PropertyWizardDialogConfigurator configurator, int style, String propertyName)
	{
		super(parent, style);
		this.persistContext = persistContext;
		this.fs = fs;
		this.table = table;
		this.propertiesConfig = configurator;
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		parent.setLayout(tableColumnLayout);
		getTable().setLinesVisible(true);
		getTable().setHeaderVisible(true);
		getTable().setToolTipText("The selected " + propertyName);
		setContentProvider(ArrayContentProvider.getInstance());

		if (propertiesConfig.getDataproviderProperties().size() > 0)
		{
			TableColumn dataproviderColumn = new TableColumn(getTable(), SWT.LEFT);
			dataproviderColumn.setText(propertyName);
			dataproviderColumn.setToolTipText("The dataprovider for which a column is created");

			TableViewerColumn dataproviderViewerColumn = new TableViewerColumn(this, dataproviderColumn);
			dataproviderViewerColumn.setLabelProvider(new ColumnLabelProvider()
			{
				@Override
				public String getText(Object element)
				{
					Map<String, Object> row = (Map<String, Object>)element;
					for (PropertyDescription pd : propertiesConfig.getDataproviderProperties())
					{
						if (row.get(pd.getName()) != null) return (String)row.get(pd.getName());
					}
					return "";
				}
			});
			tableColumnLayout.setColumnData(dataproviderColumn, getColumnWeightData(null));
		}

		for (PropertyDescription dp : propertiesConfig.getOrderedProperties())
		{
			if (propertiesConfig.getPrefillProperties().contains(dp))
			{
				continue;
			}
			TableColumn col = new TableColumn(getTable(), SWT.CENTER);
			col.setText(dp.getName());
			col.setToolTipText(dp.getDocumentation());
			TableViewerColumn colViewer = new TableViewerColumn(this, col);
			colViewer.setEditingSupport(getEditingSupport(style, dp));
			colViewer.setLabelProvider(getLabelProvider(dp));
			tableColumnLayout.setColumnData(col, getColumnWeightData(dp));
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
								List<Map<String, Object>> input = (List<Map<String, Object>>)getInput();
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
		tableColumnLayout.setColumnData(delete, getColumnWeightData(null));
	}

	private ColumnWeightData getColumnWeightData(PropertyDescription dp)
	{
		if (propertiesConfig.getRelationProperties().contains(dp))
		{
			return new ColumnWeightData(80, 150, true);
		}
		return new ColumnWeightData(40, 100, true);
	}

	private CellLabelProvider getLabelProvider(PropertyDescription dp)
	{
		if (propertiesConfig.getDataproviderProperties().contains(dp))
		{
			return new CentredImageCellLabelProvider()
			{
				@Override
				public Image getImage(Object element)
				{
					Map<String, Object> row = (Map<String, Object>)element;
					return row.get(dp.getName()) != null ? com.servoy.eclipse.ui.editors.table.ColumnLabelProvider.TRUE_RADIO
						: com.servoy.eclipse.ui.editors.table.ColumnLabelProvider.FALSE_RADIO;
				}
			};
		}
		if (propertiesConfig.getFormProperties().contains(dp))
		{
			return new FormColumnLabelProvider(dp, persistContext);
		}
		return new TextColumnLabelProvider(dp);
	}

	private EditingSupport getEditingSupport(int style, PropertyDescription dp)
	{
		if (propertiesConfig.getDataproviderProperties().contains(dp))
		{
			return new DataproviderCellEditor(this, this, style, dp, propertiesConfig.getDataproviderProperties());
		}
		if (propertiesConfig.getFormProperties().contains(dp))
		{
			return new FormCellEditorSupport(this, dp, persistContext,
				new SolutionContextDelegateLabelProvider(new FormLabelProvider(fs, false), persistContext.getContext()), fs, getTable());
		}
		if (propertiesConfig.getRelationProperties().contains(dp))
		{
			RelationCellEditorSupport editingSupport = new RelationCellEditorSupport(this, dp,
				new RelationPropertyEditor(getTable(), persistContext, table, null, true, true, false), fs);
			relationColumns.add(editingSupport);
			return editingSupport;
		}
		if (propertiesConfig.getI18nProperties().contains(dp))
		{
			TextCellEditorSupport editingSupport = new TextCellEditorSupport(this, dp,
				new TagsAndI18NTextCellEditor(getTable(), persistContext, fs, new DefaultValueDelegateLabelProvider(TextCutoffLabelProvider.DEFAULT), table,
					"Edit title/text property", Activator.getDefault().getDesignClient(), false));
			i18nColumns.add(editingSupport);
			return editingSupport;
		}
		return new TextCellEditorSupport(this, dp, new TextCellEditor(getTable()));
	}

	public void setTable(ITable table)
	{
		this.table = table;
		i18nColumns.forEach(editingSupport -> editingSupport.textCellEditor = new TagsAndI18NTextCellEditor(getTable(), persistContext, fs,
			new DefaultValueDelegateLabelProvider(TextCutoffLabelProvider.DEFAULT), table,
			"Edit title/text property", Activator.getDefault().getDesignClient(), false));
		relationColumns
			.forEach(editingSupport -> editingSupport.cellEditor = new RelationPropertyEditor(getTable(), persistContext, table, null, true, true, false));
	}
}