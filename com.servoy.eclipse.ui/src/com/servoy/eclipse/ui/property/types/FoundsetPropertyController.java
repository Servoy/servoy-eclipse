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
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.model.ServoyModelFinder;
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
import com.servoy.eclipse.ui.property.ComplexPropertySourceWithStandardReset;
import com.servoy.eclipse.ui.property.DataProviderConverter;
import com.servoy.eclipse.ui.property.ICellEditorFactory;
import com.servoy.eclipse.ui.property.IPropertyConverter;
import com.servoy.eclipse.ui.property.IPropertySetter;
import com.servoy.eclipse.ui.property.ISetterAwarePropertySource;
import com.servoy.eclipse.ui.property.JSONArrayTypePropertyController;
import com.servoy.eclipse.ui.property.JSONObjectTypePropertyController;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.PropertyController;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyTypeConfig;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;


/**
 *
 * Property controller for foundset properties.
 * @author gboros
 *
 */
public class FoundsetPropertyController extends PropertyController<JSONObject, Object> implements IPropertySetter<Object, ISetterAwarePropertySource>
{

	private static final String LOAD_ALL_RECORDS_INITIALLY = "load all records";
	private static final String DATAPROVIDERS = "dataproviders";
	private static final String DP_PREFIX = "dp";

	public static final String FORM_FOUNDSET_TEXT = "(form foundset)";

	private final FlattenedSolution flattenedSolution;
	private final PersistContext persistContext;
	private final FoundsetPropertyTypeConfig config;

	private ILabelProvider labelProvider;
	protected final FoundsetDesignToChooserConverter designToChooserConverter;

	private final ITable formTable;

	public FoundsetPropertyController(Object id, String displayName, FlattenedSolution flattenedSolution, PersistContext persistContext,
		FoundsetPropertyTypeConfig foundsetPropertyTypeConfig)
	{
		super(id, displayName);
		this.flattenedSolution = flattenedSolution;
		this.persistContext = persistContext;
		this.config = foundsetPropertyTypeConfig;
		this.formTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(
			flattenedSolution.getFlattenedForm(persistContext.getPersist()).getDataSource());
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
			labelProvider = FoundsetPropertyEditor.getFoundsetLabelProvider(persistContext, designToChooserConverter, getId().toString());
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
				if (value != null) copy = new ServoyJSONObject(value, ServoyJSONObject.getNames(value), false, false);
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
		return new FoundsetPropertyEditor(parent, persistContext, formTable, null /* foreignTable */, isReadOnly(), designToChooserConverter,
			getId().toString());
	}

	static class FoundsetPropertySource extends ComplexPropertySourceWithStandardReset<JSONObject>
	{
		private final FlattenedSolution flattenedSolution;
		private final PersistContext persistContext;
		private final String[] staticConfigDataproviders;
		private final boolean hasDynamicDataproviders;

		private final ComplexProperty<JSONObject> complexProperty;
		private final ITable formTable;
		private final boolean isSeparateDatasource;

		public FoundsetPropertySource(ComplexProperty<JSONObject> complexProperty, FlattenedSolution flattenedSolution, PersistContext persistContext,
			String[] staticConfigDataproviders, boolean hasDynamicDataproviders, ITable formTable)
		{
			super(complexProperty);
			this.complexProperty = complexProperty;
			this.flattenedSolution = flattenedSolution;
			this.persistContext = persistContext;
			this.staticConfigDataproviders = staticConfigDataproviders;
			this.hasDynamicDataproviders = hasDynamicDataproviders;
			this.formTable = formTable;
			this.isSeparateDatasource = (complexProperty != null && complexProperty.getValue() != null &&
				complexProperty.getValue().has(FoundsetPropertyType.LOAD_ALL_RECORDS_FOR_SEPARATE));
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			if (complexProperty.getValue() == null) return new IPropertyDescriptor[0];

			ArrayList<IPropertyDescriptor> propertyDescriptors = new ArrayList<IPropertyDescriptor>();


			if (isSeparateDatasource) propertyDescriptors.add(new CheckboxPropertyDescriptor(LOAD_ALL_RECORDS_INITIALLY, LOAD_ALL_RECORDS_INITIALLY));

			if (staticConfigDataproviders != null)
			{
				if (getEditableValue() != null)
				{
					propertyDescriptors.add(new JSONObjectTypePropertyController(DATAPROVIDERS, DATAPROVIDERS)
					{

						@Override
						public void resetPropertyValue(ISetterAwarePropertySource propertySource)
						{
							propertySource.setPropertyValue(getId(), new ServoyJSONObject(false, false));
						}


						@Override
						protected ObjectPropertySource getObjectChildPropertySource(ComplexProperty<Object> complexP)
						{
							return new JSONObjectPropertySource(complexP)
							{

								@Override
								protected Object getDefaultElementProperty(Object id)
								{
									return JSONObject.NULL;
								}

								@Override
								public IPropertyDescriptor[] createPropertyDescriptors()
								{
									ArrayList<IPropertyDescriptor> pds = new ArrayList<IPropertyDescriptor>();
									for (String dp : staticConfigDataproviders)
									{
										pds.add(createDataproviderPropertyDescriptor(dp, dp));
									}
									return pds.toArray(new IPropertyDescriptor[pds.size()]);
								}

								@Override
								public Object getPropertyValue(Object id)
								{
									return defaultGetProperty(id);
								}

							};
						}

					});
				}
			}
			else if (hasDynamicDataproviders)
			{
				propertyDescriptors.add(new JSONArrayTypePropertyController(DATAPROVIDERS, DATAPROVIDERS)
				{
					@Override
					protected Object getNewElementInitialValue()
					{
						return JSONObject.NULL;
					}

					@Override
					public void resetPropertyValue(ISetterAwarePropertySource propertySource)
					{
						propertySource.setPropertyValue(getId(), new ServoyJSONArray());
					}

					@Override
					protected ArrayPropertySource getArrayElementPropertySource(ComplexProperty<Object> complexP)
					{
						return new JSONArrayPropertySource(complexP)
						{
							@Override
							protected void addChildPropertyDescriptors(Object arrayV)
							{
								JSONArray arrayValue = (JSONArray)arrayV;
								ArrayList<IPropertyDescriptor> createdPDs = new ArrayList<IPropertyDescriptor>();

								for (int i = 0; i < arrayValue.length(); i++)
								{
									IPropertyDescriptor propertyDescriptor = createDataproviderPropertyDescriptor(getIdFromIndex(i), DP_PREFIX + i);
									createdPDs.add(new JSONArrayItemPropertyDescriptorWrapper(propertyDescriptor, i, this));
								}
								elementPropertyDescriptors = createdPDs.toArray(new IPropertyDescriptor[createdPDs.size()]);
							}

							@Override
							protected Object getDefaultElementProperty(Object id)
							{
								return JSONObject.NULL;
							}
						};
					}

					@Override
					protected Object getValueForReset()
					{
						return new ServoyJSONArray();
					}
				});

			}

			return PropertyController.applySequencePropertyComparator(propertyDescriptors.toArray(new IPropertyDescriptor[propertyDescriptors.size()]));
		}

		IPropertyDescriptor createDataproviderPropertyDescriptor(Object id, String displayName)
		{
			JSONObject v = getEditableValue();
			String foundsetSelector = (String)ServoyJSONObject.jsonNullToNull(v.opt(FoundsetPropertyType.FOUNDSET_SELECTOR));
			Relation[] relations = flattenedSolution.getRelationSequence(foundsetSelector);
			final DataProviderOptions options;
			ITable baseTable = formTable;

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
					baseTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(foundsetSelector);
					if (baseTable == null)
					{
						try
						{
							List<Form> forms = flattenedSolution.getFormsForNamedFoundset(Form.NAMED_FOUNDSET_SEPARATE_PREFIX + foundsetSelector);
							if (forms.size() > 0)
							{
								baseTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(forms.get(0).getDataSource());
							}
						}
						catch (Exception ex)
						{
							ServoyLog.logError(ex);
						}
					}
					if (baseTable == null)
					{
						ServoyLog.logInfo("Cannot find a table with datasource " + foundsetSelector +
							" for a foundset typed property. Using form table in dataprovider chooser.");
						baseTable = formTable;
					}
				}
				options = new DataProviderTreeViewer.DataProviderOptions(true, baseTable != null, baseTable != null, baseTable != null, false, false,
					baseTable != null, baseTable != null, INCLUDE_RELATIONS.NESTED, true, true, null);
			}


			final DataProviderConverter converter = new DataProviderConverter(flattenedSolution, persistContext.getPersist(), baseTable);
			DataProviderLabelProvider showPrefix = new DataProviderLabelProvider(false);
			showPrefix.setConverter(converter);
//			DataProviderLabelProvider hidePrefix = new DataProviderLabelProvider(true);
//			hidePrefix.setConverter(converter);

			final ITable baseTableFinal = baseTable;
			final ILabelProvider labelProviderShowPrefix = new SolutionContextDelegateLabelProvider(
				new FormContextDelegateLabelProvider(showPrefix, persistContext.getContext()));
			PropertyController<String, String> propertyController = new PropertyController<String, String>(id, displayName, null, labelProviderShowPrefix,
				new ICellEditorFactory()
				{
					public CellEditor createPropertyEditor(Composite parent)
					{
						return new DataProviderCellEditor(parent, labelProviderShowPrefix, new DataProviderValueEditor(converter),
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
			else if (DATAPROVIDERS.equals(id))
			{
				Object retVal;
				if (hasDynamicDataproviders)
				{
					ServoyJSONArray dataprovidersArray = new ServoyJSONArray();
					retVal = dataprovidersArray;
					if (v != null)
					{
						JSONObject dataprovidersValues = v.optJSONObject(FoundsetPropertyTypeConfig.DATAPROVIDERS);
						if (dataprovidersValues != null)
						{
							String foundsetSelector = (String)ServoyJSONObject.jsonNullToNull(v.opt(FoundsetPropertyType.FOUNDSET_SELECTOR));

							Iterator< ? > it = dataprovidersValues.keys();
							while (it.hasNext())
							{
								String key = (String)it.next();
								String dpValue = (String)ServoyJSONObject.jsonNullToNull(dataprovidersValues.opt(key));

								try
								{
									// drop the 'dp' prefix from dp0, dp1, ...
									int idx = Integer.parseInt(key.substring(DP_PREFIX.length()));
									if (dpValue != null && !("".equals(foundsetSelector) || isSeparateDatasource) &&
										!hasNamedFoundset(flattenedSolution, foundsetSelector))
									{
										// then it's a relation
										dpValue = foundsetSelector + "." + dpValue;
									}
									dataprovidersArray.put(idx, dpValue);
								}
								catch (Exception e)
								{
									ServoyLog.logError(e);
								}
							}
						}
					}
				}
				else
				{
					// static dataproviders
					ServoyJSONObject dataprovidersObj = new ServoyJSONObject();
					retVal = dataprovidersObj;
					JSONObject dataprovidersValues = v.optJSONObject(FoundsetPropertyTypeConfig.DATAPROVIDERS);
					String foundsetSelector = (String)ServoyJSONObject.jsonNullToNull(v.opt(FoundsetPropertyType.FOUNDSET_SELECTOR));
					for (String staticConfigDataprovider : staticConfigDataproviders)
					{
						try
						{
							String dpValue = null;
							if (dataprovidersValues != null)
							{

								dpValue = (String)ServoyJSONObject.jsonNullToNull(dataprovidersValues.opt(staticConfigDataprovider));

								// drop the 'dp' prefix from dp0, dp1, ...
								if (dpValue != null && !("".equals(foundsetSelector) || isSeparateDatasource) &&
									!hasNamedFoundset(flattenedSolution, foundsetSelector))
								{
									// then it's a relation
									dpValue = foundsetSelector + "." + dpValue;
								}
							}
							dataprovidersObj.put(staticConfigDataprovider, dpValue);
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
				}

				// find the descriptor for dataproviders and convert the value (it needs to convert it to a complex value to be expandable)
				IPropertyDescriptor[] pds = getPropertyDescriptors();
				for (IPropertyDescriptor pd : pds)
				{
					if (DATAPROVIDERS.equals(pd.getId()))
					{
						return PersistPropertySource.convertGetPropertyValue(pd.getId(), pd, retVal);
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
				else if (DATAPROVIDERS.equals(id))
				{
					IPropertyDescriptor[] pds = getPropertyDescriptors();
					for (IPropertyDescriptor pd : pds)
					{
						if (DATAPROVIDERS.equals(pd.getId()))
						{
							v = PersistPropertySource.convertSetPropertyValue(pd.getId(), pd, v);
						}
					}
					if (hasDynamicDataproviders)
					{
						JSONObject previousDataprovidersValues = editableValue.optJSONObject(FoundsetPropertyTypeConfig.DATAPROVIDERS);
						JSONArray newDataprovidersArrayToSet = (JSONArray)v;
						if (newDataprovidersArrayToSet == null) newDataprovidersArrayToSet = new ServoyJSONArray();

						if (previousDataprovidersValues != null)
						{
							Iterator< ? > it = previousDataprovidersValues.keys();
							while (it.hasNext())
							{
								String dp = (String)it.next();
								try
								{
									// drop the 'dp' prefix from dp0, dp1, ...
									int idx = Integer.parseInt(dp.substring(DP_PREFIX.length()));
									if (idx >= newDataprovidersArrayToSet.length())
									{
										it.remove();
									}
								}
								catch (NumberFormatException e)
								{
									ServoyLog.logError(e);
									it.remove(); // we don't know what DP index this is so it shouldn't be in the list
								}
							}
						}

						if (previousDataprovidersValues == null)
						{
							previousDataprovidersValues = new JSONObject();
							editableValue.put(FoundsetPropertyTypeConfig.DATAPROVIDERS, previousDataprovidersValues);
						}

						for (int i = 0; i < newDataprovidersArrayToSet.length(); i++)
						{
							String foundsetSelector = (String)ServoyJSONObject.jsonNullToNull(editableValue.opt(FoundsetPropertyType.FOUNDSET_SELECTOR));
							String dp = (newDataprovidersArrayToSet.isNull(i) ? null : newDataprovidersArrayToSet.getString(i));
							try
							{
								if (dp != null && !("".equals(foundsetSelector) || isSeparateDatasource) &&
									!hasNamedFoundset(flattenedSolution, foundsetSelector))
								{
									// then it's a relation
									dp = dp.substring(foundsetSelector.length() + 1);
								}
							}
							catch (Exception ex)
							{
								ServoyLog.logError(ex);
							}

							previousDataprovidersValues.put(DP_PREFIX + i, dp != null ? dp : JSONObject.NULL);
						}
						complexProperty.setValue(editableValue);
					}
					else
					{
						JSONObject dataprovidersValues = new ServoyJSONObject(false, true);
						JSONObject dataprovidersObj = (JSONObject)v;
						editableValue.put(FoundsetPropertyTypeConfig.DATAPROVIDERS, dataprovidersValues);

						for (String staticConfigDataprovider : staticConfigDataproviders)
						{
							String foundsetSelector = (String)ServoyJSONObject.jsonNullToNull(editableValue.opt(FoundsetPropertyType.FOUNDSET_SELECTOR));
							String dp = (dataprovidersObj == null || dataprovidersObj.isNull(staticConfigDataprovider) ? null
								: dataprovidersObj.getString(staticConfigDataprovider));
							try
							{
								if (dp != null && !("".equals(foundsetSelector) || isSeparateDatasource) &&
									!hasNamedFoundset(flattenedSolution, foundsetSelector))
								{
									// then it's a relation
									dp = dp.substring(foundsetSelector.length() + 1);
								}
							}
							catch (Exception ex)
							{
								ServoyLog.logError(ex);
							}

							dataprovidersValues.put(staticConfigDataprovider, dp != null ? dp : JSONObject.NULL);
						}
						complexProperty.setValue(editableValue);
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

	@Override
	public void setProperty(ISetterAwarePropertySource propertySource, Object value)
	{
		propertySource.defaultSetProperty(getId(), value);
	}

	@Override
	public Object getProperty(ISetterAwarePropertySource propertySource)
	{
		return propertySource.defaultGetProperty(getId());
	}

	@Override
	public boolean isPropertySet(ISetterAwarePropertySource propertySource)
	{
		return propertySource.defaultIsPropertySet(getId());
	}

	@Override
	public void resetPropertyValue(ISetterAwarePropertySource propertySource)
	{
		propertySource.defaultResetProperty(getId());
	}

	private static boolean hasNamedFoundset(FlattenedSolution flattenedSolution, String namedFoundset)
	{
		return namedFoundset != null && flattenedSolution.getFormsForNamedFoundset(Form.NAMED_FOUNDSET_SEPARATE_PREFIX + namedFoundset).size() > 0;
	}
}
