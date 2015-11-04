/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.ui.property.types;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.editors.DataProviderCellEditor;
import com.servoy.eclipse.ui.editors.DataProviderCellEditor.DataProviderValueEditor;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.labelproviders.FormContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.CheckboxPropertyDescriptor;
import com.servoy.eclipse.ui.property.ComplexProperty;
import com.servoy.eclipse.ui.property.ComplexProperty.ComplexPropertyConverter;
import com.servoy.eclipse.ui.property.ComplexPropertySource;
import com.servoy.eclipse.ui.property.DataProviderConverter;
import com.servoy.eclipse.ui.property.ICellEditorFactory;
import com.servoy.eclipse.ui.property.IPropertyConverter;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PropertyController;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyTypeConfig;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;


/**
 *
 * Property controller for foundset properties.
 * @author gboros
 *
 */
public class FoundsetPropertyController extends PropertyController<JSONObject, Object>
{

	public static final String FOUNDSET_DP_COUNT = "foundsetDataprovidersCount";
	private static final String LOAD_ALL_RECORDS_INITIALLY = "load all records";

	public static final String FORM_FOUNDSET_TEXT = "(form foundset)";

	private final FlattenedSolution flattenedSolution;
	private final PersistContext persistContext;
	private final FoundsetPropertyTypeConfig config;

	private ILabelProvider labelProvider;
	protected final FoundsetDesignToChooserConverter designToChooserConverter;

	private Table formTable;

	public FoundsetPropertyController(Object id, String displayName, FlattenedSolution flattenedSolution, PersistContext persistContext,
		FoundsetPropertyTypeConfig foundsetPropertyTypeConfig)
	{
		super(id, displayName);
		this.flattenedSolution = flattenedSolution;
		this.persistContext = persistContext;
		this.config = foundsetPropertyTypeConfig;

		try
		{
			formTable = flattenedSolution.getFlattenedForm(persistContext.getPersist()).getTable();
		}
		catch (RepositoryException ex)
		{
			ServoyLog.logError(ex);
		}
		designToChooserConverter = new FoundsetDesignToChooserConverter(flattenedSolution);
	}

	@Override
	protected IPropertyConverter<JSONObject, Object> createConverter()
	{
		return new FoundsetPropertyConverter();
	}

	@Override
	public ILabelProvider getLabelProvider()
	{
		if (labelProvider == null)
		{
			labelProvider = FoundsetPropertyEditor.getFoundsetLabelProvider(persistContext.getContext(), designToChooserConverter);
		}

		return labelProvider;
	}

	class FoundsetPropertyConverter extends ComplexPropertyConverter<JSONObject>
	{
		@Override
		public Object convertProperty(Object id, JSONObject value)
		{
			ServoyJSONObject copy = null;
			try
			{
				if (value != null) copy = new ServoyJSONObject(value, ServoyJSONObject.getNames(value), true, true);
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
			return new ComplexProperty<JSONObject>(copy)
			{
				@Override
				public IPropertySource getPropertySource()
				{
					FoundsetPropertySource foundsetPropertySource = new FoundsetPropertySource(this, flattenedSolution, persistContext, config.dataproviders,
						config.hasDynamicDataproviders, formTable);
					foundsetPropertySource.setReadonly(FoundsetPropertyController.this.isReadOnly());
					return foundsetPropertySource;
				}
			};
		}
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		return new FoundsetPropertyEditor(parent, persistContext, formTable, null /* foreignTable */, isReadOnly(), designToChooserConverter);
	}

	static class FoundsetPropertySource extends ComplexPropertySource<JSONObject>
	{
		private final FlattenedSolution flattenedSolution;
		private final PersistContext persistContext;
		private final String[] dataproviders;
		private final boolean hasDynamicDataproviders;

		private final ComplexProperty<JSONObject> complexProperty;
		private final Table formTable;
		private final boolean isSeparateDatasource;

		public FoundsetPropertySource(ComplexProperty<JSONObject> complexProperty, FlattenedSolution flattenedSolution, PersistContext persistContext,
			String[] dataproviders, boolean hasDynamicDataproviders, Table formTable)
		{
			super(complexProperty);
			this.complexProperty = complexProperty;
			this.flattenedSolution = flattenedSolution;
			this.persistContext = persistContext;
			this.dataproviders = dataproviders;
			this.hasDynamicDataproviders = hasDynamicDataproviders;
			this.formTable = formTable;
			this.isSeparateDatasource = (complexProperty != null && complexProperty.getValue().has(FoundsetPropertyType.LOAD_ALL_RECORDS_FOR_SEPARATE));
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			ArrayList<IPropertyDescriptor> propertyDescriptors = new ArrayList<IPropertyDescriptor>();


			if (isSeparateDatasource) propertyDescriptors.add(new CheckboxPropertyDescriptor(LOAD_ALL_RECORDS_INITIALLY, LOAD_ALL_RECORDS_INITIALLY));

			if (dataproviders != null)
			{
				if (getEditableValue() != null)
				{
					for (String dp : dataproviders)
					{
						propertyDescriptors.add(createDataproviderPropertyDescriptor(dp, dp));
					}
				}
			}
			else if (hasDynamicDataproviders)
			{
				JSONObject v = getEditableValue();
				if (v != null)
				{
					propertyDescriptors.add(new TextPropertyDescriptor(FOUNDSET_DP_COUNT, FOUNDSET_DP_COUNT));
					int foundsetDPCount = v.optInt(FOUNDSET_DP_COUNT);
					for (int i = 1; i <= foundsetDPCount; i++)
					{
						propertyDescriptors.add(createDataproviderPropertyDescriptor("dp" + i, "dp" + i));
					}
				}
			}

			return PropertyController.applySequencePropertyComparator(propertyDescriptors.toArray(new IPropertyDescriptor[propertyDescriptors.size()]));
		}

		IPropertyDescriptor createDataproviderPropertyDescriptor(Object id, String displayName)
		{
			JSONObject v = getEditableValue();
			String foundsetSelector = v.optString(FoundsetPropertyType.FOUNDSET_SELECTOR);
			Relation[] relations = flattenedSolution.getRelationSequence(foundsetSelector);
			final DataProviderOptions options;
			Table baseTable = formTable;

			if (relations != null)
			{
				options = new DataProviderTreeViewer.DataProviderOptions(true, false, false, true /* related calcs */, false, false, false, false,
					INCLUDE_RELATIONS.NESTED, false, true, relations);
			}
			else
			{
				if (!"".equals(foundsetSelector))
				{
					// must be a separate/random dataSource then
					baseTable = (Table)DataSourceUtils.getTable(foundsetSelector, flattenedSolution.getSolution(), ServoyModel.getServerManager());
					if (baseTable == null)
					{
						ServoyLog.logInfo("Cannot find a table with datasource " + foundsetSelector +
							" for a foundset typed property. Using form table in dataprovider chooser.");
						baseTable = formTable;
					}
				}
				options = new DataProviderTreeViewer.DataProviderOptions(true, baseTable != null, baseTable != null, baseTable != null, true, true,
					baseTable != null, baseTable != null, INCLUDE_RELATIONS.NESTED, true, true, null); // not sure all these params are ok - just used what was already used for form foundset
			}


			final DataProviderConverter converter = new DataProviderConverter(flattenedSolution, persistContext.getPersist(), baseTable);
//			DataProviderLabelProvider showPrefix = new DataProviderLabelProvider(false);
//			showPrefix.setConverter(converter);
			DataProviderLabelProvider hidePrefix = new DataProviderLabelProvider(true);
			hidePrefix.setConverter(converter);

			final Table baseTableFinal = baseTable;
			final ILabelProvider labelProviderHidePrefix = new SolutionContextDelegateLabelProvider(new FormContextDelegateLabelProvider(hidePrefix,
				persistContext.getContext()));
			PropertyController<String, String> propertyController = new PropertyController<String, String>(id, displayName, null, labelProviderHidePrefix,
				new ICellEditorFactory()
				{
					public CellEditor createPropertyEditor(Composite parent)
					{
						return new DataProviderCellEditor(parent, labelProviderHidePrefix, new DataProviderValueEditor(converter),
							flattenedSolution.getFlattenedForm(persistContext.getPersist()), flattenedSolution, readOnly, options, converter, baseTableFinal);
					}
				});
			propertyController.setSupportsReadonly(true);
			return propertyController;
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			JSONObject v = getEditableValue();

			if (LOAD_ALL_RECORDS_INITIALLY.equals(id))
			{
				return v == null ? Boolean.FALSE : Boolean.valueOf(v.optBoolean(FoundsetPropertyType.LOAD_ALL_RECORDS_FOR_SEPARATE, false));
			}
			else if (FOUNDSET_DP_COUNT.equals(id))
			{
				return v == null ? "0" : String.valueOf(v.optInt(FOUNDSET_DP_COUNT));
			}
			else
			{
				if (v != null)
				{
					JSONObject dataprovidersValues = v.optJSONObject(FoundsetPropertyTypeConfig.DATAPROVIDERS);
					if (dataprovidersValues != null && dataprovidersValues.has(id.toString()))
					{
						String foundsetSelector = v.optString(FoundsetPropertyType.FOUNDSET_SELECTOR);
						String dp = dataprovidersValues.optString(id.toString());
						if (dp != null && foundsetSelector.length() > 0)
						{
							dp = foundsetSelector + "." + dp;
						}
						return dp;
					}
				}
			}

			return null;
		}

		@Override
		public JSONObject setComplexPropertyValue(Object id, Object v)
		{
			JSONObject editableValue = getEditableValue();
			if (editableValue == null)
			{
				editableValue = new JSONObject();
				complexProperty.setValue(editableValue);
			}
			try
			{
				if (LOAD_ALL_RECORDS_INITIALLY.equals(id))
				{
					editableValue.put(FoundsetPropertyType.LOAD_ALL_RECORDS_FOR_SEPARATE, ((Boolean)v).booleanValue());
				}
				else if (FOUNDSET_DP_COUNT.equals(id))
				{
					int dpCount;
					try
					{
						dpCount = Integer.parseInt(v.toString());
					}
					catch (NumberFormatException ex)
					{
						dpCount = 0;
					}
					int oldValue = editableValue.optInt(FOUNDSET_DP_COUNT);
					if (dpCount > -1 && dpCount < 51)
					{
						editableValue.put(FOUNDSET_DP_COUNT, v.toString());
					}
					if (dpCount < oldValue)
					{
						JSONObject dataprovidersValues = editableValue.optJSONObject(FoundsetPropertyTypeConfig.DATAPROVIDERS);
						if (dataprovidersValues != null)
						{
							Iterator< ? > it = dataprovidersValues.keys();
							while (it.hasNext())
							{
								String dp = (String)it.next();
								int dpid = Utils.getAsInteger(dp.substring(2));
								if (dpid > dpCount)
								{
									it.remove();
								}
							}
						}
					}
				}
				else
				{
					JSONObject dataprovidersValues = editableValue.optJSONObject(FoundsetPropertyTypeConfig.DATAPROVIDERS);
					if (dataprovidersValues == null)
					{
						dataprovidersValues = new JSONObject();
						editableValue.put(FoundsetPropertyTypeConfig.DATAPROVIDERS, dataprovidersValues);

					}
					if (v != null)
					{
						String dpValue = v.toString();
						String foundsetSelector = editableValue.optString(FoundsetPropertyType.FOUNDSET_SELECTOR);
						if (dpValue.startsWith(foundsetSelector + "."))
						{
							dpValue = dpValue.substring(foundsetSelector.length() + 1);
						}
						dataprovidersValues.put(id.toString(), dpValue);
					}
					else
					{
						dataprovidersValues.remove(id.toString());
					}
				}
			}
			catch (JSONException ex)
			{
				ServoyLog.logError(ex);
			}

			return editableValue;
		}
	}
}
