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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.ValuesConfig;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.property.types.StyleClassPropertyType;
import org.sablo.specification.property.types.ValuesPropertyType;

import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.eclipse.ui.property.ComplexProperty.ComplexPropertyConverter;
import com.servoy.eclipse.ui.property.PseudoPropertyHandler.CustomPropertySetterDelegatePropertyController;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportAttributes;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.WebComponent;
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

	@Override
	protected void clearAbstractBaseProperty(PropertyDescriptorWrapper propertyDescriptor, Object id, AbstractBase persist)
	{
		// this is called when one property from the properties view is reset to default (and
		// it has a descriptor for that prop. and it should reset a prop in an AbstractBase persist);
		// by default this works directly on persist properties, but
		// if it's a layout container and the property's descriptor is based on a LayoutContainerPropertyHandler
		// we have to reset the attribute, not the property (those work with attributes)
		if (propertyDescriptor.propertyDescriptor instanceof LayoutContainerPropertyHandler)
			((LayoutContainer)persist).clearAttribute((String)id);
		else super.clearAbstractBaseProperty(propertyDescriptor, id, persist);
	}

	public static IPropertyHandler[] createPropertyHandlersFromSpec(PropertyDescription propertyDescription, PersistContext persistContext)
	{
		List<IPropertyHandler> props = new ArrayList<IPropertyHandler>();

		for (PropertyDescription desc : propertyDescription.getProperties().values())
		{
			IPropertyHandler propHandler = createPropertyHandlerFromSpec(desc, persistContext);
			if (propHandler != null) props.add(propHandler);
		}
		if (persistContext.getPersist() instanceof LayoutContainer || persistContext.getPersist() instanceof WebComponent ||
			persistContext.getPersist() instanceof WebFormComponentChildType)
		{
			IPropertyHandler attributesPropertyHandler = new WebComponentPropertyHandler(
				new PropertyDescriptionBuilder().withName(IContentSpecConstants.PROPERTY_ATTRIBUTES).withConfig(
					new CustomPropertySetterDelegatePropertyController<Map<String, ? >, PersistPropertySource>(new MapEntriesPropertyController(
						IContentSpecConstants.PROPERTY_ATTRIBUTES, RepositoryHelper.getDisplayName(IContentSpecConstants.PROPERTY_ATTRIBUTES, Form.class),
						propertyDescription instanceof WebLayoutSpecification ? ((WebLayoutSpecification)propertyDescription).getAttributes() : null, false)
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
									final IPersist p = (IPersist)value.get("persist");
									@SuppressWarnings("unchecked")
									Map<String, Object> v = (Map<String, Object>)value.get("values");
									return new ComplexProperty<Map<String, Object>>(v)
									{
										@Override
										public IPropertySource getPropertySource()
										{
											return new PersistMapPropertySourceWithOverrideSupport(p, this)
											{
												@SuppressWarnings("unchecked")
												@Override
												public Map<String, String> getPersistMap(IPersist persist)
												{
													return (Map<String, String>)((AbstractBase)persist).getCustomPropertyNonFlattened(
														new String[] { IContentSpecConstants.PROPERTY_ATTRIBUTES });
												}

												@Override
												public IPropertyDescriptor[] createPropertyDescriptors()
												{
													// remove "class" property from super-property-descriptors
													IPropertyDescriptor[] propertyDescriptors = super.createPropertyDescriptors();
													List<IPropertyDescriptor> result = new ArrayList<>();
													for (IPropertyDescriptor desc : propertyDescriptors)
													{
														if (!"class".equals(desc.getId()))
														{
															result.add(desc);
														}
													}
													return result.toArray(new IPropertyDescriptor[result.size()]);
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

												@Override
												public Object resetComplexPropertyValue(Object id)
												{
													if (p instanceof WebFormComponentChildType)
													{
														IFormElement el = ((WebFormComponentChildType)p).getElement();
														if (el instanceof BaseComponent)
														{
															Map<String, String> attributes = ((BaseComponent)el).getAttributes();
															if (attributes != null && attributes.containsKey(id))
															{
																return attributes.get(id);
															}
														}
													}
													return super.resetComplexPropertyValue(id);
												}
											};
										}
									};
								}
							};
						}

					}, IContentSpecConstants.PROPERTY_ATTRIBUTES, IContentSpecConstants.PROPERTY_ATTRIBUTES)
					{

						@Override
						public Map<String, ? > getMergedProperties(IPersist p)
						{
							if (p instanceof ISupportAttributes)
							{
								return new HashMap<String, String>(((ISupportAttributes)p).getMergedAttributes());
							}
							return null;
						}

						@SuppressWarnings("unchecked")
						@Override
						public void setMergedProperties(IPersist p, Map<String, ? > value)
						{
							if (p instanceof ISupportAttributes)
							{
								((ISupportAttributes)p).putUnmergedAttributes((Map<String, String>)value);
							}
						}
					}).build());
			props.add(attributesPropertyHandler);
		}
		if (persistContext.getPersist() instanceof WebComponent || persistContext.getPersist() instanceof WebFormComponentChildType)
		{
			props.add(new PseudoPropertyHandler("designTimeProperties"));
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
		if (values != null && values.size() > 0 && !(desc.getType() instanceof StyleClassPropertyType))
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

			createdPropertyHandler = createWebComponentPropertyHandler(
				new PropertyDescriptionBuilder().withName(desc.getName()).withType(ValuesPropertyType.INSTANCE).withConfig(config).withDefaultValue(
					desc.getDefaultValue()).withInitialValue(desc.getInitialValue()).withHasDefault(desc.hasDefault()).withTagsCopiedFrom(desc)
					.withDeprecated(
						desc.getDeprecated())
					.build(),
				persistContext);
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
		String name = propertyDescription.getName();
		if ("".equals(name) || name == null)
		{
			name = persistContext.getPersist().toString();
		}
		return name;
	}

}
