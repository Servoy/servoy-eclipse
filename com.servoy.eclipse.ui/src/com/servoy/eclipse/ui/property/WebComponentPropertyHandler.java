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

import java.awt.Dimension;
import java.awt.Point;
import java.util.List;

import org.eclipse.ui.views.properties.IPropertySource;
import org.json.JSONObject;
import org.sablo.specification.IYieldingType;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.FunctionPropertyType;
import org.sablo.specification.property.types.StringPropertyType;
import org.sablo.websocket.utils.PropertyUtils;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IDesignValueConverter;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.property.ComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormPropertyType;
import com.servoy.j2db.server.ngclient.property.types.MediaPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGCustomJSONArrayType;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;

/**
 * Property handler for web components
 *
 * @author rgansevles
 *
 */
public class WebComponentPropertyHandler implements IPropertyHandler
{

	private final PropertyDescription propertyDescription;

	public WebComponentPropertyHandler(PropertyDescription propertyDescription)
	{
		this.propertyDescription = propertyDescription;
	}

	@Override
	public String getName()
	{
		return propertyDescription.getName();
	}

	@Override
	public boolean isProperty()
	{
		return true;
	}

	@Override
	public PropertyDescription getPropertyDescription(Object obj, IPropertySource propertySource, PersistContext persistContext)
	{
		return propertyDescription;
	}

	@Override
	public String getDisplayName()
	{
		return getName();
	}

	@Override
	public boolean hasSupportForClientType(Object obj, ClientSupport csp)
	{
		return true;
	}

	@Override
	public Object getValue(Object obj, PersistContext persistContext)
	{
		IBasicWebObject webObject = (IBasicWebObject)obj;
		Object value = webObject.getProperty(getName());
		return getValueInternal(value, propertyDescription, webObject, persistContext.getContext());
	}

	public Object getValueInternal(Object value, PropertyDescription pd, IBasicWebObject webObject, IPersist context)
	{
		try
		{
			IPropertyType< ? > type = pd.getType();
			if (type instanceof IYieldingType) type = ((IYieldingType< ? , ? >)type).getPossibleYieldType();
			if (type instanceof NGCustomJSONArrayType caType && value instanceof Object[] array)
			{
				for (int i = 0; i < array.length; i++)
				{
					array[i] = getValueInternal(array[i], caType.getCustomJSONTypeDefinition(), webObject, context);
				}
			}
			if (type instanceof FunctionPropertyType || type instanceof ValueListPropertyType || type instanceof FormPropertyType ||
				type instanceof MediaPropertyType || type instanceof FormComponentPropertyType)
			{
				if (type instanceof FormComponentPropertyType && value instanceof JSONObject)
				{
					value = ((JSONObject)value).optString(FormComponentPropertyType.SVY_FORM);
				}
				if (value == null) return Integer.valueOf(0);
				if (value instanceof Integer) return value;


				IPersist persist = ModelUtils.getEditingFlattenedSolution(webObject, context).searchPersist((String)value);
				if (persist instanceof AbstractBase)
				{
					return new Integer(persist.getID());
				}

				return Integer.MAX_VALUE;
			}
			else if (value == null && !webObject.hasProperty(getName()) &&
				(propertyDescription.hasDefault() || propertyDescription.getType().defaultValue(propertyDescription) != null)) // default values for persist mapped properties are already handled by WebObjectImpl, so value will not be null here for those
			{
				// if null is coming from parent and is a value that is was set, it is not a default, return it
				if (webObject.getParentComponent().getExtendsID() > 0)
				{
					List<AbstractBase> overrideHierarchy = PersistHelper.getOverrideHierarchy(webObject.getParentComponent());
					if (overrideHierarchy != null && overrideHierarchy.size() > 1) // list should contain as first item the component itself, so start with the first parent
					{
						String propertyName = getName();
						boolean propertyIsSetInHierarchy = false;
						for (int i = 1; i < overrideHierarchy.size(); i++)
						{
							AbstractBase parentObject = overrideHierarchy.get(i);
							if (parentObject.getPropertiesMap().containsKey(propertyName))
							{
								propertyIsSetInHierarchy = true;
								break;
							}
							if (parentObject instanceof WebComponent)
							{
								WebComponent parentWebComponent = (WebComponent)parentObject;
								JSONObject parentWebComponentJson = parentWebComponent.getJson();
								if (parentWebComponentJson != null && parentWebComponentJson.has(propertyName))
								{
									propertyIsSetInHierarchy = true;
									break;
								}
							}
						}
						if (propertyIsSetInHierarchy) return value; // here it returns null
					}
				}
				Object defaultValue = null;
				if (propertyDescription.hasDefault())
				{
					defaultValue = propertyDescription.getDefaultValue();
				}
				else
				{
					defaultValue = propertyDescription.getType().defaultValue(propertyDescription);
				}
				if (propertyDescription.getType() instanceof IDesignValueConverter)
				{
					return ((IDesignValueConverter< ? >)propertyDescription.getType()).fromDesignValue(defaultValue, propertyDescription, webObject);
				}
				return defaultValue;
			}
			if (value != null && type instanceof StringPropertyType)
			{
				value = value.toString();
			}
		}
		catch (Exception e)
		{
			Debug.log("illegal value in web component, ignoring it: " + value);
		}
		return value;
	}

	@Override
	public void setValue(Object obj, Object value, PersistContext persistContext)
	{
		if (obj instanceof WebFormComponentChildType)
		{
			IFormElement fe = ((WebFormComponentChildType)obj).getElement();
			if (StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName().equals(getName()) && value instanceof Point &&
				((fe.getParent() instanceof Form && ((Form)fe.getParent()).getUseCssPosition().booleanValue())))
			{
				BaseComponent formComponent = (BaseComponent)((WebFormComponentChildType)obj).getParent();
				CSSPositionUtils.setLocationEx(formComponent, (WebFormComponentChildType)obj, ((Point)value).x, ((Point)value).y, formComponent.getSize());
				return;
			}
			else if (StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName().equals(getName()) && value instanceof Dimension &&
				((fe.getParent() instanceof Form && ((Form)fe.getParent()).getUseCssPosition().booleanValue())))
			{
				BaseComponent formComponent = (BaseComponent)((WebFormComponentChildType)obj).getParent();
				CSSPositionUtils.setSizeEx(formComponent, (WebFormComponentChildType)obj, ((Dimension)value).width, ((Dimension)value).height,
					formComponent.getSize());
				return;
			}
			// are there other components where we should convert to cssposition?
		}

		IBasicWebObject bean = (IBasicWebObject)obj;
		Object convertedValue = value;

		convertedValue = setValueInternal(bean, convertedValue, persistContext, propertyDescription);

		bean.setProperty(getName(), convertedValue);
	}

	public Object setValueInternal(IBasicWebObject bean, Object value, PersistContext persistContext, PropertyDescription pd)
	{

		Object convertedValue = value;
		IPropertyType< ? > type = pd.getType();
		if (type instanceof IYieldingType) type = ((IYieldingType< ? , ? >)type).getPossibleYieldType();

		if (type instanceof FunctionPropertyType)
		{
			//  value is methodid
			ITable table = null;
			if (persistContext.getContext() instanceof Form)
			{
				FlattenedSolution editingFlattenedSolution = ModelUtils.getEditingFlattenedSolution(bean, persistContext.getContext());
				table = editingFlattenedSolution.getTable(((Form)persistContext.getContext()).getDataSource());
			}
			IScriptProvider scriptMethod = ModelUtils.getScriptMethod(bean, persistContext.getContext(), table, ((Integer)value).intValue());
			convertedValue = scriptMethod == null ? null : scriptMethod.getUUID().toString();
		}
		else if (type instanceof ValueListPropertyType)
		{
			if (value != null)
			{
				if (value instanceof String)
				{
					convertedValue = value;
				}
				else
				{
					ValueList val = ModelUtils.getEditingFlattenedSolution(bean, persistContext.getContext()).getValueList(((Integer)value).intValue());
					convertedValue = (val == null) ? null : val.getUUID().toString();
				}
			}
			else convertedValue = null;
		}
		else if (type instanceof FormPropertyType)
		{
			Form frm = ModelUtils.getEditingFlattenedSolution(bean, persistContext.getContext()).getForm(((Integer)value).intValue());
			convertedValue = (frm == null) ? null : frm.getUUID().toString();
		}
		else if (value != null && type instanceof MediaPropertyType)
		{
			Media media = ModelUtils.getEditingFlattenedSolution(bean, persistContext.getContext()).getMedia(((Integer)value).intValue());
			convertedValue = (media == null) ? null : media.getUUID().toString();
		}
		else if (type instanceof FormComponentPropertyType)
		{
			if (convertedValue != null)
			{
				Form frm = ModelUtils.getEditingFlattenedSolution(bean, persistContext.getContext()).getForm(((Integer)value).intValue());
				if (frm != null)
				{
					JSONObject json = new JSONObject();
					json.put(FormComponentPropertyType.SVY_FORM, frm.getUUID());
					convertedValue = json;
				}
				else
				{
					convertedValue = null;
				}
			}
		}
		else if (type instanceof NGCustomJSONArrayType caType && convertedValue instanceof Object[] array)
		{
			for (int i = 0; i < array.length; i++)
			{
				array[i] = setValueInternal(bean, array[i], persistContext, caType.getCustomJSONTypeDefinition());
			}
		}
		return convertedValue;
	}

	public boolean shouldShow(PersistContext persistContext)
	{
		if (propertyDescription.isDeprecated())
		{
			IBasicWebObject webObject = (IBasicWebObject)persistContext.getPersist();
			if (webObject == null || webObject.getProperty(getName()) == null)
			{
				// if a value is set show it, unset may be needed
				return false;
			}
		}
		if (propertyDescription.getType() instanceof ComponentPropertyType)
		{
			return false;
		}
		if (PropertyUtils.isCustomJSONArrayPropertyType(propertyDescription.getType()) &&
			((CustomJSONArrayType< ? , ? >)propertyDescription.getType()).getCustomJSONTypeDefinition().getType() instanceof ComponentPropertyType)
		{
			return false;
		}
		return true;
	}
}
