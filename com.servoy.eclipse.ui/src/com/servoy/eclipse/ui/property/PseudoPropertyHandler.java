/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.ValuesConfig;
import org.sablo.specification.property.types.ValuesPropertyType;

import com.servoy.eclipse.ui.property.ComplexProperty.ComplexPropertyConverter;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Solution.AUTHENTICATOR_TYPE;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * Handler for some known pseudo proeties/
 *
 * @author rgansevles
 *
 */
public class PseudoPropertyHandler implements IPropertyHandler
{
	// null type: use property controller internally
	public static final PropertyDescription LOGIN_SOLUTION_DESCRIPTION = new PropertyDescriptionBuilder().withName("loginSolutionName").withConfig(
		new LoginSolutionPropertyController("loginSolutionName", RepositoryHelper.getDisplayName("loginSolutionName", Solution.class))).build();
	public static final PropertyDescription SOLUTION_VERSION_DESCRIPTION = new PropertyDescriptionBuilder().withName("version").withConfig(
		new VersionPropertyController("version", RepositoryHelper.getDisplayName("version", Solution.class))).build();

	public static final PropertyDescription AUTHENTICATOR_VALUES = new PropertyDescriptionBuilder().withName("authenticator").withType(
		ValuesPropertyType.INSTANCE).withConfig(
			new ValuesConfig().setValues(Solution.AUTHENTICATOR_TYPE.values(),
				Stream.of(Solution.AUTHENTICATOR_TYPE.values())
					.map(Enum::name)
					.collect(Collectors.toList()).toArray(new String[0])))
		.build();


	// null type: use property controller internally
	public static final PropertyDescription DESIGN_PROPERTIES_DESCRIPTION = new PropertyDescriptionBuilder().withName("designTimeProperties").withConfig(
		new CustomPropertySetterDelegatePropertyController<Map<String, Object>, PersistPropertySource>(
			new MapEntriesPropertyController("designTimeProperties", RepositoryHelper.getDisplayName("designTimeProperties", Form.class), null, false)
			{
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
										public Map<String, Object> getPersistMap(IPersist persist)
										{
											return (Map<String, Object>)((AbstractBase)persist).getCustomPropertyNonFlattened(
												new String[] { IContentSpecConstants.PROPERTY_DESIGNTIME });
										}
									};
								}
							};
						}
					};
				}

			}, "designTimeProperties", IContentSpecConstants.PROPERTY_DESIGNTIME)
		{

			@Override
			public Map<String, ? > getMergedProperties(IPersist p)
			{
				if (p instanceof AbstractBase)
				{
					return ((AbstractBase)p).getMergedCustomDesignTimeProperties();
				}
				return null;
			}

			@SuppressWarnings("unchecked")
			@Override
			public void setMergedProperties(IPersist p, Map<String, ? > value)
			{
				if (p instanceof AbstractBase)
				{
					((AbstractBase)p).setUnmergedCustomDesignTimeProperties((Map<String, Object>)value);
				}
			}
		}).build();

	public static final PropertyDescription ATTRIBUTES_PROPERTY_DESCRIPTION = new PropertyDescriptionBuilder().withName(
		StaticContentSpecLoader.PROPERTY_ATTRIBUTES).withConfig(
			new CustomPropertySetterDelegatePropertyController<Map<String, ? >, PersistPropertySource>(
				new MapEntriesPropertyController(StaticContentSpecLoader.PROPERTY_ATTRIBUTES, StaticContentSpecLoader.PROPERTY_ATTRIBUTES, null, false)
				{

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

				}, StaticContentSpecLoader.PROPERTY_ATTRIBUTES, IContentSpecConstants.PROPERTY_ATTRIBUTES)
			{

				@Override
				public Map<String, ? > getMergedProperties(IPersist p)
				{
					if (p instanceof BaseComponent)
					{
						return new HashMap<String, String>(((BaseComponent)p).getMergedAttributes());
					}
					return null;
				}

				@SuppressWarnings("unchecked")
				@Override
				public void setMergedProperties(IPersist p, Map<String, ? > value)
				{
					if (p instanceof BaseComponent)
					{
						((BaseComponent)p).putUnmergedAttributes((Map<String, String>)value);
					}
				}
			})
		.build();


	public static abstract class CustomPropertySetterDelegatePropertyController<P extends Map<String, ? >, S extends PersistPropertySource>
		extends PropertySetterDelegatePropertyController<P, S>
	{
		private final String customType;

		/**
		 * @param propertyDescriptor
		 * @param id
		 */
		public CustomPropertySetterDelegatePropertyController(IPropertyDescriptor propertyDescriptor, Object id, String customType)
		{
			super(propertyDescriptor, id);
			this.customType = customType;
		}

		public abstract Map<String, ? > getMergedProperties(IPersist p);

		public abstract void setMergedProperties(IPersist p, Map<String, ? > value);

		@SuppressWarnings("unchecked")
		@Override
		public P getProperty(PersistPropertySource propSource)
		{
			IPersist persist = propSource.getPersist();
			Map<String, Object> mergedCustomDesignProperties = (Map<String, Object>)getMergedProperties(persist);
			if (mergedCustomDesignProperties != null)
			{
				Map<String, Object> mapWithPersistAndValues = new HashMap<>();
				mapWithPersistAndValues.put("persist", persist);
				mapWithPersistAndValues.put("values", mergedCustomDesignProperties);
				return (P)mapWithPersistAndValues;
			}
			return null;
		}

		public void setProperty(PersistPropertySource propSource, P value)
		{
			IPersist persist = propSource.getPersist();
			setMergedProperties(persist, value);
			if (!isPropertySet(propSource))
			{
				((AbstractBase)persist).clearCustomProperty(new String[] { customType });
				propSource.defaultResetProperty(getId());
			}
		}

		@Override
		public void resetPropertyValue(PersistPropertySource propSource)
		{
			setProperty(propSource, null);
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean isPropertySet(PersistPropertySource propSource)
		{
			IPersist persist = propSource.getPersist();
			if (persist instanceof AbstractBase)
			{
				Map<String, String> attributes = (Map<String, String>)((AbstractBase)persist).getCustomPropertyNonFlattened(new String[] { customType });

				return attributes != null && attributes.size() > 0;
			}

			return false;
		}

	}


	private final String name;

	public PseudoPropertyHandler(String name)
	{
		this.name = name;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public boolean isProperty()
	{
		return true;
	}

	@Override
	public String getDisplayName()
	{
		return getName();
	}

	@Override
	public PropertyDescription getPropertyDescription(Object obj, IPropertySource propertySource, PersistContext persistContext)
	{
		if (name.equals("authenticator"))
		{
			return AUTHENTICATOR_VALUES;
		}
		if (name.equals("loginSolutionName"))
		{
			return LOGIN_SOLUTION_DESCRIPTION;
		}
		if (name.equals("version"))
		{
			return SOLUTION_VERSION_DESCRIPTION;
		}

		if (name.equals("designTimeProperties"))
		{
			return DESIGN_PROPERTIES_DESCRIPTION;
		}

		if (name.equals(IContentSpecConstants.PROPERTY_ATTRIBUTES))
		{
			return ATTRIBUTES_PROPERTY_DESCRIPTION;
		}

		return null;
	}

	@Override
	public Object getValue(Object obj, PersistContext persistContext)
	{
		if (name.equals("authenticator") && obj instanceof Solution sol)
		{
			return sol.getAuthenticator();
		}
		return null;
	}

	@Override
	public void setValue(Object obj, Object value, PersistContext persistContext)
	{
		if (name.equals("authenticator") && obj instanceof Solution sol)
		{
			sol.setAuthenticator((AUTHENTICATOR_TYPE)value);
		}
	}

	@Override
	public boolean hasSupportForClientType(Object obj, ClientSupport csp)
	{
		return true;
	}

	public boolean shouldShow(PersistContext persistContext)
	{
		return true;
	}
}
