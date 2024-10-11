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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractLayerConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.edit.command.EditCellCommandHandler;
import org.eclipse.nebula.widgets.nattable.edit.command.EditSelectionCommandHandler;
import org.eclipse.nebula.widgets.nattable.edit.config.DefaultEditBindings;
import org.eclipse.nebula.widgets.nattable.edit.event.InlineCellEditEventHandler;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultColumnHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayer;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.CompositeLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.AggregateConfigLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnOverrideLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.config.DefaultSelectionLayerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.ui.dialogs.autowizard.nattable.AutoWizardTableColumnPropertyAccessor;
import com.servoy.eclipse.ui.dialogs.autowizard.nattable.LinkClickConfiguration;
import com.servoy.eclipse.ui.dialogs.autowizard.nattable.PainterConfiguration;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;

/**
 * @author emera
 */
public class AutoWizardPropertiesComposite
{
	private List<Map<String, Object>> treeInput = new ArrayList<>();

	private final PersistContext persistContext;
	private final FlattenedSolution flattenedSolution;
	private final PropertyWizardDialogConfigurator propertiesConfigurator;

	private NatTable natTable;
	private HashMap<String, String> propertyToLabels;
	private List<String> propertyNames;

	private IDataProvider bodyDataProvider;
	private static final String CSS_CLASS_NAME_KEY = "org.eclipse.e4.ui.css.CssClassName";//did not import it to avoid adding dependencies for using one constant from CSSSWTConstants


	public AutoWizardPropertiesComposite(final ScrolledComposite parent, PersistContext persistContext, FlattenedSolution flattenedSolution,
		PropertyWizardDialogConfigurator configurator)
	{
		this.flattenedSolution = flattenedSolution;
		this.persistContext = persistContext;
		this.propertiesConfigurator = configurator;

		this.natTable = null;
		this.propertyNames = null;
		if (configurator.getInput() != null) setInputProperties(configurator.getInput());
		setupNatTable(parent, configurator);
	}

	/**
	 * @param parent
	 * @param configurator
	 */
	private void setupNatTable(final ScrolledComposite parent, PropertyWizardDialogConfigurator configurator)
	{
		this.propertyNames = propertiesConfigurator.getOrderedProperties().stream().filter(pd -> !propertiesConfigurator.getPrefillProperties().contains(pd))
			.map(pd -> pd.getName()).collect(Collectors.toList());
		if (configurator.getDataproviderProperties().size() > 0) propertyNames.add(0, configurator.getAutoPropertyName());
		this.propertyToLabels = new HashMap<>();
		for (String prop : propertyNames)
		{
			propertyToLabels.put(prop, prop.toUpperCase());
		}
		DefaultColumnHeaderDataProvider colHeaderDataProvider = new DefaultColumnHeaderDataProvider(
			propertyNames.toArray(new String[propertyNames.size()]), propertyToLabels);
		this.bodyDataProvider = setupBodyDataProvider();

		BodyLayerStack bodyLayer = new BodyLayerStack(bodyDataProvider);
		ColumnHeaderLayerStack columnHeaderLayer = new ColumnHeaderLayerStack(
			colHeaderDataProvider, bodyLayer);
		CompositeLayer composeLayer = new CompositeLayer(1, 2);
		composeLayer.setChildLayer(GridRegion.COLUMN_HEADER, columnHeaderLayer, 0, 0);
		composeLayer.setChildLayer(GridRegion.BODY, bodyLayer.getSelectionLayer(), 0, 1);
		natTable = new NatTable(parent, SWT.NONE, composeLayer, false);
		ConfigRegistry configRegistry = new ConfigRegistry();
		natTable.setConfigRegistry(configRegistry);
		configRegistry.registerConfigAttribute(
			CellConfigAttributes.RENDER_GRID_LINES,
			Boolean.FALSE);
		natTable.addConfiguration(new DefaultNatTableStyleConfiguration());
		natTable.setLayerPainter(new HorizontalGridLineCellLayerPainter());

		LinkClickConfiguration linkClickConfiguration = new LinkClickConfiguration(bodyLayer.getSelectionLayer());
		PainterConfiguration painterConfiguration = new PainterConfiguration(propertiesConfigurator, linkClickConfiguration, bodyDataProvider, persistContext,
			flattenedSolution);
		natTable.addConfiguration(painterConfiguration);
		natTable.addDisposeListener((e) -> painterConfiguration.dispose());
		natTable.addConfiguration(linkClickConfiguration);
		composeLayer.addConfiguration(new AbstractLayerConfiguration<AbstractLayer>()
		{
			@Override
			public void configureTypedLayer(AbstractLayer layer)
			{
				layer.registerCommandHandler(new EditCellCommandHandler());
				layer.registerEventHandler(new InlineCellEditEventHandler(layer));
				layer.registerCommandHandler(new EditSelectionCommandHandler(bodyLayer.getSelectionLayer()));
			}
		});
		composeLayer.addConfiguration(new DefaultEditBindings());
		natTable.setData(CSS_CLASS_NAME_KEY, "svyNatTable");
		natTable.configure();
		natTable.refresh();
		parent.setContent(natTable);
		parent.setMinSize(natTable.getWidth(), natTable.getHeight());
		parent.update();

		natTable.addListener(SWT.Resize, event -> {
			parent.setMinSize(natTable.getWidth(), natTable.getHeight());
			parent.update();
		});
	}

	private IDataProvider setupBodyDataProvider()
	{
		return new ListDataProvider<>(treeInput,
			new AutoWizardTableColumnPropertyAccessor(this.propertiesConfigurator));
	}

	private void setInputProperties(List<Map<String, Object>> childrenProperties)
	{
		treeInput = childrenProperties;
	}

	public List<Map<String, Object>> getResult()
	{
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> returnValue = ((ListDataProvider<Map<String, Object>>)bodyDataProvider).getList();

		if (propertiesConfigurator.getPrefillProperties().size() > 0)
		{
			returnValue.forEach(row -> {
				propertiesConfigurator.getPrefillProperties().forEach(pd -> {
					if (row.get(pd.getName()) == null || row.get(pd.getName()).equals(pd.getDefaultValue()))
					{
						row.put(pd.getName(), getUniquePrefillValue(pd, returnValue, row));
					}
				});
			});
		}
		return returnValue;
	}

	private Object getUniquePrefillValue(PropertyDescription pd, List<Map<String, Object>> rows, Map<String, Object> row)
	{
		Object value = row.get(((JSONObject)pd.getTag("wizard")).get("prefill"));
		if (value == null)
		{
			value = pd.getName() + rows.indexOf(row);
		}
		int count = rows.indexOf(row) + 1;
		while (!isUniqueValue(pd, rows, value))
		{
			if (value instanceof String)
			{
				value = value + "" + count++;
			}
			else
			{
				break;
			}
		}
		return value;
	}

	private boolean isUniqueValue(PropertyDescription pd, List<Map<String, Object>> rows, Object value)
	{
		return !rows.stream().filter(r -> value.equals(r.get(pd.getName()))).findAny().isPresent();
	}

	@SuppressWarnings("unchecked")
	public void setInput(List<Map<String, Object>> list)
	{
		((ListDataProvider<Map<String, Object>>)bodyDataProvider).getList().clear();
		((ListDataProvider<Map<String, Object>>)bodyDataProvider).getList().addAll(list);
		natTable.refresh(true);
	}

	@SuppressWarnings("unchecked")
	public void addNewRow(Map<String, Object> row)
	{
		((ListDataProvider<Map<String, Object>>)bodyDataProvider).getList().add(row);
		natTable.refresh(true);
		ScrolledComposite parent = (ScrolledComposite)natTable.getParent();
		parent.setMinSize(natTable.getWidth(), natTable.getHeight());
		parent.update();
		parent.setOrigin(0, natTable.getHeight());
	}

	public List<Map<String, Object>> getInput()
	{
		return treeInput;
	}

	public class BodyLayerStack extends AbstractLayerTransform
	{

		private final SelectionLayer selectionLayer;
		public static final String DELETE_LABEL = "delete";

		public BodyLayerStack(IDataProvider dataProvider)
		{
			DataLayer bodyDataLayer = new DataLayer(dataProvider);
			bodyDataLayer.setDefaultRowHeight(40);
			bodyDataLayer.setColumnPercentageSizing(true);
			bodyDataLayer.setDefaultMinColumnWidth(10);
			if (propertyNames.size() > 3)
			{
				//make the column before the delete larger
				bodyDataLayer.setMinColumnWidth(propertyNames.size() - 2, 100);
				bodyDataLayer.setMinColumnWidth(propertyNames.size() - 1, 150);
			}
			bodyDataLayer.setDistributeRemainingRowSpace(true);

			AggregateConfigLabelAccumulator accumulator = new AggregateConfigLabelAccumulator();
			// create the ColumnLabelAccumulator with IDataProvider to be able to
			// tell the CSS engine about the added labels
			accumulator.add(new ColumnLabelAccumulator(dataProvider));

			final ColumnOverrideLabelAccumulator columnLabelAccumulator = new ColumnOverrideLabelAccumulator(
				bodyDataLayer);
			bodyDataLayer.setConfigLabelAccumulator(columnLabelAccumulator);

			registerColumnLabels(columnLabelAccumulator);

			accumulator.add(columnLabelAccumulator);
			bodyDataLayer.setConfigLabelAccumulator(accumulator);

			this.selectionLayer = new SelectionLayer(bodyDataLayer);
			selectionLayer.addConfiguration(new DefaultSelectionLayerConfiguration());
			setUnderlyingLayer(bodyDataLayer);
		}

		private void registerColumnLabels(ColumnOverrideLabelAccumulator columnLabelAccumulator)
		{
			for (int i = 0; i < propertyNames.size(); i++)
			{
				columnLabelAccumulator.registerColumnOverrides(i, propertyNames.get(i));
			}
			columnLabelAccumulator.registerColumnOverrides(propertyNames.size(), DELETE_LABEL, LinkClickConfiguration.LINK_CELL_LABEL);
		}

		public SelectionLayer getSelectionLayer()
		{
			return this.selectionLayer;
		}
	}

	public class ColumnHeaderLayerStack extends AbstractLayerTransform
	{
		public ColumnHeaderLayerStack(IDataProvider dataProvider, BodyLayerStack bodyLayer)
		{
			DataLayer dataLayer = new DataLayer(dataProvider);
			ColumnHeaderLayer colHeaderLayer = new ColumnHeaderLayer(dataLayer,
				bodyLayer, bodyLayer.getSelectionLayer());
			setUnderlyingLayer(colHeaderLayer);
		}
	}

	public class RowHeaderLayerStack extends AbstractLayerTransform
	{
		public RowHeaderLayerStack(IDataProvider dataProvider, BodyLayerStack bodyLayer)
		{
			DataLayer dataLayer = new DataLayer(dataProvider, 50, 20);
			RowHeaderLayer rowHeaderLayer = new RowHeaderLayer(dataLayer,
				bodyLayer, bodyLayer.getSelectionLayer());
			setUnderlyingLayer(rowHeaderLayer);
		}
	}

	public void commitAndCloseActiveCellEditor()
	{
		natTable.commitAndCloseActiveCellEditor();
	}
}