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

package com.servoy.eclipse.ui.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.ValuesConfig;
import org.sablo.specification.property.types.ValuesPropertyType;

import com.servoy.eclipse.ui.property.ComplexProperty.ComplexPropertyConverter;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.util.Utils;

/**
 * Sablo {@link PropertyDescription} based property source.
 *
 * @author acostescu
 */
public class PDPropertySource extends PersistPropertySource
{

	private final PropertyDescription propertyDescription;

	public PDPropertySource(PersistContext persistContext, boolean readonly, PropertyDescription propertyDescription)
	{
		super(persistContext, readonly);
		if (!(persistContext.getPersist() instanceof IBasicWebObject) && !(persistContext.getPersist() instanceof LayoutContainer))
		{
			throw new IllegalArgumentException();
		}
		this.propertyDescription = propertyDescription;
	}

	protected PropertyDescription getPropertyDescription()
	{
		return propertyDescription;
	}

	@Override
	protected Object getValueObject(FlattenedSolution flattenedEditingSolution, Form form)
	{
		return persistContext.getPersist();
	}

	@Override
	protected IPropertyHandler[] createPropertyHandlers(Object valueObject)
	{
		return createPropertyHandlersFromSpec(propertyDescription, persistContext);
	}

	public static IPropertyHandler[] createPropertyHandlersFromSpec(PropertyDescription propertyDescription, PersistContext persistContext)
	{
		List<IPropertyHandler> props = new ArrayList<IPropertyHandler>();

		for (PropertyDescription desc : propertyDescription.getProperties().values())
		{
			IPropertyHandler propHandler = createPropertyHandlerFromSpec(desc, persistContext);
			if (propHandler != null) props.add(propHandler);
		}
		if (persistContext.getPersist() instanceof LayoutContainer)
		{
			IPropertyHandler attributesPropertyHandler = new WebComponentPropertyHandler(
				new PropertyDescription("attributes", null, new PropertySetterDelegatePropertyController<Map<String, Object>, PersistPropertySource>(
					new MapEntriesPropertyController("attributes", RepositoryHelper.getDisplayName("attributes", Form.class))
					{ /*
						 * (non-Javadoc)
						 *
						 * @see com.servoy.eclipse.ui.property.PropertyController#createConverter()
						 */
						@Override
						protected ComplexPropertyConverter<Map<String, Object>> createConverter()
						{
							return new ComplexProperty.ComplexPropertyConverter<Map<String, Object>>()
							{
								@Override
								public Object convertProperty(final Object id, Map<String, Object> value)
								{
									return new ComplexProperty<Map<String, Object>>(value)
									{
										@Override
										public IPropertySource getPropertySource()
										{
											return new MapPropertySource(this)
											{
												@Override
												public IPropertyDescriptor[] createPropertyDescriptors()
												{

													IPropertyDescriptor[] propertyDescriptors = super.createPropertyDescriptors();
													IPropertyDescriptor[] result = new IPropertyDescriptor[propertyDescriptors.length - 1];
													int k = 0;
													for (int i = 0; i < propertyDescriptors.length; i++)
													{
														if (!propertyDescriptors[i].getId().equals("class"))
														{
															result[k] = propertyDescriptors[i];
															k++;
														}
													}
													return result;
												}

												/*
												 * (non-Javadoc)
												 *
												 * @see com.servoy.eclipse.ui.property.MapEntriesPropertyController.MapPropertySource#toJSExpression(java.lang.
												 * Object)
												 */
												@Override
												protected String toJSExpression(Object v)
												{
													String result;
													if (v instanceof String && ((String)v).length() > 0)
													{
														result = v.toString();
													}
													else
													{
														result = null;
													}
													return result;
												}
											};
										}
									};
								}
							};
						}

					}, "attributes")
				{
					@SuppressWarnings("unchecked")
					@Override
					public Map<String, Object> getProperty(PersistPropertySource propSource)
					{
						IPersist persist = propSource.getPersist();
						if (persist instanceof LayoutContainer)
						{
							return (Map<String, Object>)((LayoutContainer)persist).getCustomProperty(new String[] { "attributes" }); // returns non-null map with copied/merged values, may be written to
						}
						return null;
					}

					public void setProperty(PersistPropertySource propSource, Map<String, Object> value)
					{
						IPersist persist = propSource.getPersist();
						if (persist instanceof AbstractBase)
						{
							((LayoutContainer)persist).putCustomProperty(new String[] { "attributes" }, value);
						}
					}
				}));
			props.add(attributesPropertyHandler);
		}
		return props.toArray(new IPropertyHandler[props.size()]);
	}

	public static IPropertyHandler createPropertyHandlerFromSpec(PropertyDescription desc, PersistContext persistContext)
	{
		IPropertyHandler createdPropertyHandler = null;
		Object scope = desc.getTag(WebFormComponent.TAG_SCOPE);
		if ("private".equals(scope) || "runtime".equals(scope))
		{
			return null;
		}

		List<Object> values = desc.getValues();
		if (values != null && values.size() > 0 && !desc.getName().equals(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName()))
		{
			ValuesConfig config = new ValuesConfig();
			if (!(values.get(0) instanceof JSONObject))
			{
				config.setValues(values.toArray(new Object[0]));
			}
			else
			{
				List<String> displayValues = new ArrayList<String>();
				List<Object> realValues = new ArrayList<Object>();
				for (Object jsonObject : values)
				{
					if (jsonObject instanceof JSONObject)
					{
						for (Object object : Utils.iterate(((JSONObject)jsonObject).keys()))
						{
							String key = (String)object;
							displayValues.add(key);
							realValues.add(((JSONObject)jsonObject).opt(key));
						}
					}
				}
				config.setValues(realValues.toArray(new Object[realValues.size()]), displayValues.toArray(new String[displayValues.size()]));
			}
			if (desc.hasDefault())
			{
				config.addDefault(desc.getDefaultValue(), null);
			}
			createdPropertyHandler = createWebComponentPropertyHandler(new PropertyDescription(desc.getName(), ValuesPropertyType.INSTANCE, config,
				desc.getDefaultValue(), desc.hasDefault(), null, null, null, false), persistContext);
		}
		else
		{
			createdPropertyHandler = createWebComponentPropertyHandler(desc, persistContext);
		}
		return createdPropertyHandler;
	}

	protected static IPropertyHandler createWebComponentPropertyHandler(PropertyDescription desc, PersistContext persistContext)
	{
		if (persistContext.getPersist() instanceof LayoutContainer)
		{
			return new LayoutContainerPropertyHandler(desc);
		}
		return new WebComponentPropertyHandler(desc);
	}

	@Override
	public String toString()
	{
		return propertyDescription.getName();
	}

}
