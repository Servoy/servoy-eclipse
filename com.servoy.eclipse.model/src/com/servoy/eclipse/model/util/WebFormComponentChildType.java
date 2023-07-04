/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.model.util;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ICustomType;
import org.sablo.websocket.utils.PropertyUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IBasicWebComponent;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IDesignValueConverter;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportAttributes;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.persistence.WebObjectImpl;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.FormElementHelper.FormComponentCache;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;

/**
 *
 * @author jcompagner
 */
public class WebFormComponentChildType extends AbstractBase implements IBasicWebObject, IParentOverridable, ISupportAttributes
{
	private final PropertyDescription propertyDescription;
	private final String key;
	private final String parentPropertyName;
	private final String[] rest;
	private final FlattenedSolution fs;
	private IFormElement element;

	public WebFormComponentChildType(IBasicWebObject parentWebObject, String key, FlattenedSolution fs)
	{
		super(IRepository.WEBCUSTOMTYPES, parentWebObject, parentWebObject.getID(), UUID.randomUUID());
		this.key = key;
		this.fs = fs;
		int index = key.indexOf('.');
		this.parentPropertyName = key.substring(0, index);
		this.rest = key.substring(index + 1).split("\\.");

		propertyDescription = initializeElement();
	}

	/**
	 * @return the key
	 */
	public String getKey()
	{
		return key;
	}

	public PropertyDescription getPropertyDescription()
	{
		return propertyDescription;
	}

	@Override
	public void setProperty(String propertyName, Object val)
	{
		Object valueFromJavaType = convertFromJavaType(propertyName, val);
		JSONObject updatableJSON = getJson(true, false);
		Object currentPropertyValue = updatableJSON.opt(propertyName);
		if (currentPropertyValue instanceof JSONObject && valueFromJavaType instanceof JSONObject)
		{
			ServoyJSONObject.mergeAndDeepCloneJSON((JSONObject)valueFromJavaType, (JSONObject)currentPropertyValue);
		}
		else if (currentPropertyValue instanceof JSONArray && valueFromJavaType instanceof JSONArray)
		{
			ServoyJSONObject.mergeAndDeepCloneJSON((JSONArray)valueFromJavaType, (JSONArray)currentPropertyValue);
		}
		else
		{
			currentPropertyValue = valueFromJavaType;
		}

		updatableJSON.put(propertyName, currentPropertyValue);
		getParentComponent().flagChanged();
	}

	public void removeProperty()
	{
		JSONObject valuesJSON = getJson(true, false);
		for (String keyToRemove : valuesJSON.keySet())
		{
			valuesJSON.remove(keyToRemove);
		}
		getParentComponent().flagChanged();
	}

	@Override
	protected void setPropertyInternal(String propertyName, Object val)
	{
		// this is an override so that custom properties are set also in the json.
		// this does mean that a webcomponent can't have its own property called "customProperties"
		setProperty(propertyName, val);
	}

	private Object convertToJavaType(String propertyName, Object val)
	{
		Object value = val;
		PropertyDescription pd = propertyDescription.getProperty(propertyName);

		if (pd != null)
		{
			if (PropertyUtils.isCustomJSONObjectProperty(pd.getType()) && value instanceof JSONObject)
			{
				String typeName = pd.getType().getName().indexOf(".") > 0 ? pd.getType().getName().split("\\.")[1] : pd.getType().getName();
				WebCustomType customType = createWebCustomType(pd, propertyName, -1, UUID.fromString(((JSONObject)value).optString(IChildWebObject.UUID_KEY)));
				customType.setTypeName(typeName);
				return customType;
			}
			else if (WebObjectImpl.isArrayOfCustomJSONObject(pd.getType()) && value instanceof JSONArray)
			{
				ArrayList<IChildWebObject> customArray = new ArrayList<IChildWebObject>();
				PropertyDescription elementPD = (pd.getType() instanceof ICustomType< ? >) ? ((ICustomType< ? >)pd.getType()).getCustomJSONTypeDefinition()
					: null;
				String typeName = elementPD.getType().getName().indexOf(".") > 0 ? elementPD.getType().getName().split("\\.")[1]
					: elementPD.getType().getName();
				for (int i = 0; i < ((JSONArray)value).length(); i++)
				{
					JSONObject obj = ((JSONArray)value).getJSONObject(i);
					WebCustomType customType = createWebCustomType(elementPD, propertyName, i, UUID.fromString(obj.optString(IChildWebObject.UUID_KEY)));
					customType.setTypeName(typeName);
					customArray.add(customType);
				}
				return customArray.toArray(new IChildWebObject[customArray.size()]);
			}
			else if (value != null && getConverter(pd) != null)
			{
				value = getConverter(pd).fromDesignValue(value, pd, this);
			}
		}
		return (val != JSONObject.NULL) ? value : null;
	}

	private Object convertFromJavaType(String propertyName, Object value)
	{
		PropertyDescription pd = propertyDescription.getProperty(propertyName);
		if (value instanceof IChildWebObject)
		{
			return ((IChildWebObject)value).getFullJsonInFrmFile();
		}
		else if (value instanceof IChildWebObject[])
		{
			ServoyJSONArray array = new ServoyJSONArray();
			for (IChildWebObject object : (IChildWebObject[])value)
			{
				array.put(object.getFullJsonInFrmFile());
			}
			return array;
		}
		else if (pd != null && getConverter(pd) != null)
		{
			return getConverter(pd).toDesignValue(value, pd);
		}
		return value;
	}

	private IDesignValueConverter< ? > getConverter(PropertyDescription pd)
	{
		return (pd.getType() instanceof IDesignValueConverter< ? >) ? (IDesignValueConverter< ? >)pd.getType() : null;
	}

	private WebCustomType createWebCustomType(Object propertyDescriptionArg, final String jsonKey, final int index, UUID uuidArg)
	{
		Pair<Integer, UUID> idAndUUID = WebObjectImpl.getNewIdAndUUID(this);
		return new WebFormComponentCustomType(this, propertyDescriptionArg, jsonKey, index, idAndUUID.getLeft().intValue(),
			uuidArg != null ? uuidArg : idAndUUID.getRight());
	}

	@Override
	public Map<String, Object> getPropertiesMap()
	{
		HashMap<String, Object> map = new HashMap<>();
		JSONObject json = getJson();
		for (String property : json.keySet())
		{
			map.put(property, json.get(property));
		}
		return map;
	}

	@Override
	public Object getProperty(String propertyName)
	{
		if (StaticContentSpecLoader.PROPERTY_JSON.getPropertyName().equals(propertyName))
		{
			return getJson(false, true);
		}
		return convertToJavaType(propertyName, getJson(false, true).opt(propertyName));
	}

	@Override
	public Object getPropertyDefaultValueClone(String propertyName)
	{
		PropertyDescription pd = getPropertyDescription();
		PropertyDescription propPD = pd != null ? pd.getProperty(propertyName) : null;
		return propPD != null && propPD.hasDefault() ? ServoyJSONObject.deepCloneJSONArrayOrObj(propPD.getDefaultValue()) : null;
	}

	@Override
	public boolean hasProperty(String propertyName)
	{
		return getJson().has(propertyName);
	}

	@Override
	public void clearProperty(String propertyName)
	{
		getJson(true, false).remove(propertyName);
		getParentComponent().flagChanged();
		initializeElement();
	}

	public void clearCustomProperty(String customPropertyName, String propertyName, int propertyIndex)
	{
		Object customProperty = getJson(true, false).get(customPropertyName);
		if (customProperty instanceof JSONObject)
		{
			((JSONObject)customProperty).remove(propertyName);
		}
		else if (customProperty instanceof JSONArray)
		{
			((JSONObject)((JSONArray)customProperty).get(propertyIndex)).remove(propertyName);
		}
		getParentComponent().flagChanged();
		initializeElement();
	}

	public boolean hasCustomProperty(String customPropertyName, String propertyName, int propertyIndex)
	{
		Object customProperty = getJson(true, true).get(customPropertyName);
		if (customProperty instanceof JSONObject)
		{
			return ((JSONObject)customProperty).has(propertyName);
		}
		else if (customProperty instanceof JSONArray)
		{
			return ((JSONObject)((JSONArray)customProperty).get(propertyIndex)).has(propertyName);
		}
		return false;
	}

	private PropertyDescription initializeElement()
	{
		JSONObject propertyValue = (JSONObject)getParentComponent().getProperty(parentPropertyName);
		PropertyDescription pd = FormComponentPropertyType.INSTANCE.getPropertyDescription(parentPropertyName, propertyValue, fs);
		FormElement parentFormElement = FormElementHelper.INSTANCE.getFormElement(getParentComponent(), fs, new PropertyPath(), true);
		PropertyDescription parentPD = pd;
		Form form = FormComponentPropertyType.INSTANCE.getForm(propertyValue, fs);
		StringBuilder name = new StringBuilder();
		name.append("$");
		name.append(parentPropertyName);
		for (String propertyName : rest)
		{
			pd = pd.getProperty(propertyName);
			JSONObject value = propertyValue.optJSONObject(propertyName);
			if (value == null)
			{
				value = new JSONObject();
				propertyValue.put(propertyName, value);
			}
			propertyValue = value;
			if (pd.getType() == FormComponentPropertyType.INSTANCE && form != null)
			{
				// this is a nested form component, try to find that FormElement so we have the full flattened properties.
				String currentName = name.toString();
				FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(parentFormElement, parentPD,
					(JSONObject)parentFormElement.getPropertyValue(parentPD.getName()), form, fs);
				for (FormElement fe : cache.getFormComponentElements())
				{
					String feName = fe.getName();
					int firstDollar = feName.indexOf('$');
					if (currentName.equals(feName.substring(firstDollar)))
					{
						parentFormElement = fe;
						parentPD = pd;
						form = FormComponentPropertyType.INSTANCE.getForm(fe.getPropertyValue(parentPD.getName()), fs);
						break;
					}
				}
			}
			name.append('$');
			name.append(propertyName);
		}
		if (form != null)
		{
			// get the merged/fully flattened form element from the form component cache for the current parent form element.
			String currentName = name.toString();
			String currentNameExtended = parentFormElement.getName() + name.toString();
			FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(parentFormElement, parentPD,
				(JSONObject)parentFormElement.getPropertyValue(parentPD.getName()), form, fs);
			for (FormElement fe : cache.getFormComponentElements())
			{
				String feName = fe.getName();
				int firstDollar = feName.indexOf('$');
				if (currentName.equals(feName.substring(firstDollar)) || currentNameExtended.equals(feName))
				{
					// element is found which is the fully flattened one that has all the properties.
					// this is used in the getJSON() when the flattened form must be returned.
					element = (IFormElement)fe.getPersistIfAvailable();
					break;
				}
			}
		}
		return pd;
	}

	@Override
	public void setName(String arg)
	{
	}

	@Override
	public String getName()
	{
		return null;
	}

	@Override
	public void setTypeName(String arg)
	{
	}

	@Override
	public String getTypeName()
	{
		return propertyDescription.getName();
	}

	@Override
	public JSONObject getFlattenedJson()
	{
		return getJson();
	}

	@Override
	public IBasicWebComponent getParentComponent()
	{
		return (IBasicWebComponent)getParent();
	}

	@Override
	public void updateJSON()
	{
	}

	@Override
	public JSONObject getJson()
	{
		return getJson(false, false);
	}


	private JSONObject getJson(boolean forMutation, boolean flattened)
	{
		JSONObject propertyValue = null;
		if (forMutation)
		{
			JSONObject jsonObject = (JSONObject)((AbstractBase)getParent()).getPropertiesMap().get(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
			if (jsonObject == null || !jsonObject.has(parentPropertyName))
			{
				JSONObject parentObject = new JSONObject();
				JSONObject superValue = (JSONObject)((IBasicWebObject)getParent()).getProperty(parentPropertyName);
				parentObject.put(FormComponentPropertyType.SVY_FORM, superValue.get(FormComponentPropertyType.SVY_FORM));
				// this set property will override the cloned from super parent json in the parent, if the parent has a super persist.
				((IBasicWebObject)getParent()).setProperty(parentPropertyName, parentObject);
				if (!flattened)
				{
					propertyValue = parentObject;
				}
			}
			else if (!flattened)
			{
				propertyValue = jsonObject.getJSONObject(parentPropertyName);
			}
		}

		if (propertyValue == null)
		{
			propertyValue = (JSONObject)((IBasicWebObject)getParent()).getProperty(parentPropertyName);
		}

		for (String propertyName : rest)
		{
			JSONObject value = propertyValue.optJSONObject(propertyName);
			if (value == null)
			{
				value = new JSONObject();
				propertyValue.put(propertyName, value);
			}
			propertyValue = value;
		}
		if (flattened && element != null)
		{
			JSONObject full = new JSONObject();
			Map<String, Object> elementProps = element.getFlattenedPropertiesMap();
			for (Entry<String, Object> entry : elementProps.entrySet())
			{
				if (entry.getKey().equals(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName()) && entry.getValue() instanceof JSONObject)
				{
					JSONObject json = (JSONObject)entry.getValue();
					for (String jsonKey : json.keySet())
					{
						full.put(jsonKey, json.get(jsonKey));
					}
				}
				else full.put(entry.getKey(), convertFromJavaType(entry.getKey(), entry.getValue()));
			}
			ServoyJSONObject.mergeAndDeepCloneJSON(propertyValue, full);
			propertyValue = full;
		}
		return propertyValue;
	}

	@Override
	public IPersist getParentToOverride()
	{
		return getParent();
	}

	@Override
	public IPersist newOverwrittenParent(IPersist newPersist)
	{
		return new WebFormComponentChildType((IBasicWebObject)newPersist, key, fs);
	}

	public IFormElement getElement()
	{
		return element;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this) return true;
		if (obj != null && obj.getClass() == getClass())
		{
			WebFormComponentChildType webFormComponentChildType = (WebFormComponentChildType)obj;
			return getParent().equals(webFormComponentChildType.getParent()) && getKey().equals(webFormComponentChildType.getKey());
		}
		return false;
	}

	@Override
	public String toString()
	{
		return getParentComponent().getName() + "." + getKey();
	}

	@Override
	public List<IPersist> getAllObjectsAsList()
	{
		ArrayList<IPersist> allObjectsAsList = new ArrayList<IPersist>();
		PropertyDescription pd = getPropertyDescription();
		Map<String, PropertyDescription> customMap = pd.getCustomJSONProperties();
		for (PropertyDescription customProperty : customMap.values())
		{
			Object customValue = getProperty(customProperty.getName());
			if (customValue instanceof IPersist)
			{
				allObjectsAsList.add((IPersist)customValue);
			}
			else if (customValue instanceof IPersist[])
			{
				allObjectsAsList.addAll(Arrays.asList((IPersist[])customValue));
			}
		}
		return allObjectsAsList;
	}

	@Override
	public void putAttributes(Map<String, String> value)
	{
		putCustomProperty(new String[] { IContentSpecConstants.PROPERTY_ATTRIBUTES }, value);
	}

	@Override
	public Map<String, String> getAttributes()
	{
		Object customProperty = getCustomProperty(new String[] { IContentSpecConstants.PROPERTY_ATTRIBUTES });
		if (customProperty instanceof Map)
		{
			return Collections.unmodifiableMap((Map<String, String>)customProperty);
		}
		return Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, String> getMergedAttributes()
	{
		return (Map<String, String>)getMergedCustomPropertiesInternal(IContentSpecConstants.PROPERTY_ATTRIBUTES, new HashMap<String, String>());
	}

	@Override
	public void putUnmergedAttributes(Map<String, String> value)
	{
		setUnmergedCustomPropertiesInternal(IContentSpecConstants.PROPERTY_ATTRIBUTES, value);
	}

	class WebFormComponentCustomType extends WebCustomType implements ISupportInheritedPropertyCheck
	{
		String jsonKey;
		int index;

		public WebFormComponentCustomType(IBasicWebObject parentWebObject, Object propertyDescription, String jsonKey, int index, int id, UUID uuid)
		{
			super(parentWebObject, propertyDescription, jsonKey, index, false, id, uuid);
			this.jsonKey = jsonKey;
			this.index = index;
		}

		@Override
		public void setProperty(String propertyName, Object val)
		{
			super.setProperty(propertyName, val);
			if (index < 0)
			{
				WebFormComponentChildType.this.setProperty(jsonKey, this);
			}
			else
			{
				IChildWebObject[] currentValue = (IChildWebObject[])WebFormComponentChildType.this.convertToJavaType(jsonKey,
					WebFormComponentChildType.this.getJson(false, true).opt(jsonKey));
				currentValue[index] = this;
				WebFormComponentChildType.this.setProperty(jsonKey, currentValue);
			}
		}

		@Override
		public void clearProperty(String propertyName)
		{
			super.clearProperty(propertyName);
			WebFormComponentChildType.this.clearCustomProperty(jsonKey, propertyName, index);
		}

		@Override
		public String toString()
		{
			return WebCustomType.class.getSimpleName() + " -> " + webObjectImpl.toString(); //$NON-NLS-1$
		}

		@Override
		public boolean isInheritedProperty(String propertyName)
		{
			return WebFormComponentChildType.this.hasCustomProperty(jsonKey, propertyName, index);
		}
	}
}
