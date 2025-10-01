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

package com.servoy.eclipse.ui.dialogs.autowizard.nattable;

import java.util.Map;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.edit.editor.CheckBoxCellEditor;
import org.eclipse.nebula.widgets.nattable.edit.editor.TextCellEditor;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.CheckBoxPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.ImagePainter;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.ui.NatEventData;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.nebula.widgets.nattable.widget.EditModeEnum;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.dialogs.FormContentProvider;
import com.servoy.eclipse.ui.dialogs.FormContentProvider.FormListOptions;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider.RelationsWrapper;
import com.servoy.eclipse.ui.dialogs.autowizard.PropertyWizardDialogConfigurator;
import com.servoy.eclipse.ui.labelproviders.FormLabelProvider;
import com.servoy.eclipse.ui.labelproviders.RelationLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.Utils;

/**
 * @author emera
 */
public class PainterConfiguration extends AbstractRegistryConfiguration
{
	PropertyWizardDialogConfigurator propertiesConfig;
	private final LinkClickConfiguration linkClickConfig;
	private final PersistContext context;
	private final FlattenedSolution flattenedSolution;
	private final IDataProvider bodyDataProvider;

	private Font boldFont;
	private FontDescriptor boldDescriptor;

	public PainterConfiguration(PropertyWizardDialogConfigurator propertiesConfig, LinkClickConfiguration linkClickConfiguration,
		IDataProvider bodyDataProvider,
		PersistContext context, FlattenedSolution flattenedSolution)
	{
		super();
		this.propertiesConfig = propertiesConfig;
		this.linkClickConfig = linkClickConfiguration;
		this.context = context;
		this.flattenedSolution = flattenedSolution;
		this.bodyDataProvider = bodyDataProvider;
	}

	@Override
	public void configureRegistry(IConfigRegistry configRegistry)
	{
		configRegistry.registerConfigAttribute(
			CellConfigAttributes.RENDER_GRID_LINES,
			Boolean.FALSE);

		registerColumnHeaderStyle(configRegistry);
		registerPainters(configRegistry);
	}

	private void registerPainters(IConfigRegistry configRegistry)
	{
		configRegistry.registerConfigAttribute(
			EditConfigAttributes.CELL_EDITABLE_RULE,
			IEditableRule.ALWAYS_EDITABLE);

		if (propertiesConfig.getDataproviderProperties().size() > 1)
		{
			configRegistry.registerConfigAttribute(
				CellConfigAttributes.CELL_STYLE,
				new Style(),
				DisplayMode.NORMAL, propertiesConfig.getAutoPropertyName());
		}

		for (PropertyDescription dp : propertiesConfig.getOrderedProperties())
		{
			if (propertiesConfig.getDataproviderProperties().contains(dp))
			{
				registerRadioColumn(configRegistry, dp);
			}
			else if (propertiesConfig.getFormProperties().contains(dp))
			{
				registerFormColumn(configRegistry, dp);
			}
			else if (propertiesConfig.getRelationProperties().contains(dp))
			{
				registerRelationColumn(configRegistry, dp);
			}
			else if (propertiesConfig.getI18nProperties().contains(dp))
			{
				registerI18nColumn(configRegistry, dp);
			}
			else if (propertiesConfig.getStyleProperties().contains(dp))
			{
				registerEditableTextColumn(configRegistry, dp);
			}
			else
				registerTextColumn(configRegistry, dp);
		}
		registerDeleteColumn(configRegistry);
	}

	private void registerDeleteColumn(IConfigRegistry configRegistry)
	{
		Style style = new Style();
		style.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.CENTER);
		configRegistry.registerConfigAttribute(
			CellConfigAttributes.CELL_STYLE,
			style,
			DisplayMode.NORMAL,
			"delete");
		configRegistry.registerConfigAttribute(
			CellConfigAttributes.CELL_STYLE,
			style,
			DisplayMode.SELECT,
			LinkClickConfiguration.LINK_CELL_LABEL);
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
			new ImagePainter(com.servoy.eclipse.ui.Activator.getDefault().loadImageFromBundle("delete.png")), DisplayMode.NORMAL, "delete");
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
			new ImagePainter(com.servoy.eclipse.ui.Activator.getDefault().loadImageFromBundle("trash.png")), DisplayMode.SELECT, "delete");
		linkClickConfig.addClickListener(new IMouseAction()
		{
			@Override
			public void run(NatTable natTable, MouseEvent event)
			{
				NatEventData eventData = NatEventData.createInstanceFromEvent(event);
				deleteRow(natTable, eventData.getRowPosition(), eventData.getColumnPosition());
			}
		});
		linkClickConfig.addKeyListener((NatTable natTable, KeyEvent event) -> {
			ILayerCell selectedCell = linkClickConfig.getSelectionLayer().getSelectedCells().iterator().next();
			deleteRow(natTable, selectedCell.getRowPosition(), selectedCell.getColumnPosition());
		});
	}

	private void registerEditableTextColumn(IConfigRegistry configRegistry, PropertyDescription dp)
	{
		Style style = new Style();
		style.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
		configRegistry.registerConfigAttribute(
			CellConfigAttributes.CELL_STYLE,
			style,
			DisplayMode.NORMAL,
			dp.getName());
		configRegistry.registerConfigAttribute(
			CellConfigAttributes.CELL_STYLE,
			style,
			DisplayMode.EDIT,
			dp.getName());

		configRegistry.registerConfigAttribute(
			EditConfigAttributes.CELL_EDITOR,
			new TextCellEditor(true, true, true), DisplayMode.NORMAL,
			dp.getName());

		configRegistry.registerConfigAttribute(
			EditConfigAttributes.CELL_EDITOR,
			new TextCellEditor(true, true, true)
			{
				@Override
				public Text createEditorControl(Composite parent_)
				{
					int style_ = SWT.LEFT;
					if (this.editMode == EditModeEnum.DIALOG)
					{
						style_ = style_ | SWT.BORDER;
					}

					return super.createEditorControl(parent_, style_);
				}
			}, //
			DisplayMode.EDIT,
			dp.getName());

		configRegistry.registerConfigAttribute(
			EditConfigAttributes.CELL_EDITABLE_RULE,
			IEditableRule.ALWAYS_EDITABLE, DisplayMode.EDIT,
			dp.getName());
	}

	private void registerI18nColumn(IConfigRegistry configRegistry, PropertyDescription dp)
	{
		NatTextDialogCellEditor dialogCellEditor = new I18NTextDialogCellEditor(false, context, Activator.getDefault().getDesignClient(),
			propertiesConfig.getTable(), flattenedSolution,
			"Edit title/text property", com.servoy.eclipse.ui.Activator.getDefault().loadImageFromBundle("i18n.png"));

		Style style = new Style();
		style.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
		configRegistry.registerConfigAttribute(
			CellConfigAttributes.CELL_STYLE,
			style,
			DisplayMode.NORMAL,
			dp.getName());

		configRegistry.registerConfigAttribute(
			EditConfigAttributes.CELL_EDITOR,
			dialogCellEditor, DisplayMode.EDIT,
			dp.getName());

		configRegistry.registerConfigAttribute(
			EditConfigAttributes.CELL_EDITABLE_RULE,
			IEditableRule.ALWAYS_EDITABLE, DisplayMode.EDIT,
			dp.getName());
	}

	private void registerRelationColumn(IConfigRegistry configRegistry, PropertyDescription dp)
	{
		final RelationDisplayConverter relationDisplayConverter = new RelationDisplayConverter(flattenedSolution);
		NatListDialogCellEditor dialogCellEditor = new NatListDialogCellEditor("Select relation",
			new RelationContentProvider(ModelUtils.getEditingFlattenedSolution(context.getPersist(), context.getContext()),
				context.getContext()),
			new SolutionContextDelegateLabelProvider(RelationLabelProvider.INSTANCE_LAST_NAME_ONLY, context.getContext()),
			new RelationContentProvider.RelationListOptions(propertiesConfig.getTable(), null, true, true),
			SWT.NONE, "Select relation")
		{
			@Override
			protected Object getCanonicalValue(Object value)
			{
				return relationDisplayConverter.canonicalToDisplayValue(value);
			}
		};
		dialogCellEditor.setDisplayConverter(relationDisplayConverter);
		dialogCellEditor.setSelectionFilter(new IFilter()
		{
			public boolean select(Object toTest)
			{
				if (toTest == RelationContentProvider.NONE)
				{
					return true;
				}
				if (toTest instanceof RelationsWrapper && ((RelationsWrapper)toTest).relations != null && ((RelationsWrapper)toTest).relations.length > 0)
				{
					return true;
				}
				return false;
			}
		});

		Style style = new Style();
		style.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
		configRegistry.registerConfigAttribute(
			CellConfigAttributes.CELL_STYLE,
			style,
			DisplayMode.NORMAL,
			dp.getName());

		configRegistry.registerConfigAttribute(
			EditConfigAttributes.CELL_EDITOR,
			dialogCellEditor, DisplayMode.NORMAL,
			dp.getName());

		configRegistry.registerConfigAttribute(
			CellConfigAttributes.DISPLAY_CONVERTER,
			relationDisplayConverter,
			DisplayMode.NORMAL,
			dp.getName());

		configRegistry.registerConfigAttribute(
			CellConfigAttributes.DISPLAY_CONVERTER,
			new DisplayConverter()
			{
				@Override
				public Object canonicalToDisplayValue(Object canonicalValue)
				{
					return canonicalValue != null ? relationDisplayConverter.canonicalToDisplayValue(canonicalValue) : "";
				}

				@Override
				public Object displayToCanonicalValue(Object displayValue)
				{
					return Utils.stringIsEmpty((String)displayValue) ? relationDisplayConverter.displayToCanonicalValue(displayValue) : null;
				}
			}, DisplayMode.EDIT,
			dp.getName());

		configRegistry.registerConfigAttribute(
			EditConfigAttributes.CELL_EDITOR,
			dialogCellEditor, DisplayMode.EDIT,
			dp.getName());

		configRegistry.registerConfigAttribute(
			EditConfigAttributes.CELL_EDITABLE_RULE,
			IEditableRule.ALWAYS_EDITABLE, DisplayMode.EDIT,
			dp.getName());
	}

	private void registerFormColumn(IConfigRegistry configRegistry, PropertyDescription dp)
	{
		final FormDisplayConverter formDisplayConverter = new FormDisplayConverter(context);

		NatListDialogCellEditor dialogCellEditor = new NatListDialogCellEditor("Select form dialog",
			new FormContentProvider(flattenedSolution, null /* persist is solution */), new FormLabelProvider(flattenedSolution, false),
			new FormContentProvider.FormListOptions(FormListOptions.FormListType.FORMS, null,
				true, false, false, false, null),
			SWT.NONE, "Select form")
		{
			@Override
			protected Object getCanonicalValue(Object value)
			{
				return formDisplayConverter.displayToCanonicalValue(value);
			}
		};

		Style style = new Style();
		style.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, style, DisplayMode.NORMAL, dp.getName());

		configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR, dialogCellEditor, DisplayMode.NORMAL, dp.getName());

		configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, formDisplayConverter, DisplayMode.NORMAL, dp.getName());
		configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new DisplayConverter()
		{
			@Override
			public Object canonicalToDisplayValue(Object canonicalValue)
			{
				if (canonicalValue instanceof String)
				{
					IPersist persist = flattenedSolution.searchPersist((String)canonicalValue);
					if (persist instanceof AbstractBase)
					{
						return persist.getUUID().toString();
					}
				}
				return null;
			}

			@Override
			public Object displayToCanonicalValue(Object displayValue)
			{
				Form frm = flattenedSolution.getForm(displayValue != null ? displayValue.toString() : null);
				return (frm == null) ? "" : frm.getName();
			}
		}, DisplayMode.EDIT, dp.getName());
		configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR, dialogCellEditor, DisplayMode.EDIT, dp.getName());

		configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE, DisplayMode.EDIT, dp.getName());
	}

	private void registerColumnHeaderStyle(IConfigRegistry configRegistry)
	{
		Style style = new Style();
		if (boldDescriptor == null)
		{
			boldDescriptor = FontDescriptor.createFrom(Display.getDefault().getSystemFont()).setStyle(SWT.BOLD);
			boldFont = boldDescriptor.createFont(Display.getDefault());
		}
		style.setAttributeValue(
			CellStyleAttributes.FONT,
			boldFont);

		configRegistry.registerConfigAttribute(
			CellConfigAttributes.CELL_STYLE,
			style,
			DisplayMode.NORMAL,
			GridRegion.COLUMN_HEADER);
	}

	public void dispose()
	{
		if (boldDescriptor != null)
		{
			boldDescriptor.destroyFont(boldFont);
			boldDescriptor = null;
			boldFont = null;
		}
	}

	private void registerTextColumn(IConfigRegistry configRegistry, PropertyDescription dp)
	{
		Style style = new Style();
		style.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
		configRegistry.registerConfigAttribute(
			CellConfigAttributes.CELL_STYLE,
			style,
			DisplayMode.NORMAL,
			dp.getName());
	}

	private void registerRadioColumn(IConfigRegistry configRegistry, PropertyDescription dp)
	{
		CheckBoxPainter checkboxPainter = new CheckBoxPainter(com.servoy.eclipse.ui.editors.table.ColumnLabelProvider.TRUE_RADIO,
			com.servoy.eclipse.ui.editors.table.ColumnLabelProvider.FALSE_RADIO);
		configRegistry.registerConfigAttribute(
			CellConfigAttributes.CELL_PAINTER,
			checkboxPainter,
			DisplayMode.NORMAL,
			dp.getName());
		configRegistry.registerConfigAttribute(
			CellConfigAttributes.CELL_PAINTER,
			checkboxPainter,
			DisplayMode.SELECT,
			dp.getName());

		// using a CheckBoxCellEditor also needs a Boolean conversion to work correctly
		configRegistry.registerConfigAttribute(
			CellConfigAttributes.DISPLAY_CONVERTER,
			new DisplayConverter()
			{
				@Override
				public Object displayToCanonicalValue(Object displayValue)
				{
					//returning boolean because we don't have access to the row object here, to see which is the dataprovider value
					return Boolean.valueOf(!Utils.stringIsEmpty(displayValue.toString()));
				}

				@Override
				public Object canonicalToDisplayValue(Object canonicalValue)
				{
					return Boolean.valueOf(!Utils.stringIsEmpty((String)canonicalValue));
				}
			},
			DisplayMode.NORMAL,
			dp.getName());

		configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR, new CheckBoxCellEditor(),
			DisplayMode.EDIT, dp.getName());
	}

	public void deleteRow(NatTable natTable, int _rowIndex, int _columnIndex)
	{
		int rowIndex = natTable.getRowIndexByPosition(_rowIndex);
		int columnIndex = natTable.getColumnIndexByPosition(_columnIndex);
		if (columnIndex == natTable.getColumnCount() - 1) // delete
		{
			((ListDataProvider<Map<String, Object>>)bodyDataProvider).getList().remove(rowIndex);
			natTable.refresh(true);
			ScrolledComposite parent = (ScrolledComposite)natTable.getParent();
			parent.setMinSize(natTable.getWidth(), natTable.getHeight());
			parent.update();
		}
	}
}
