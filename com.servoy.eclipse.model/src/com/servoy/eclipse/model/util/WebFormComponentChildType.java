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
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IBasicWebComponent;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IDesignValueConverter;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RuntimeProperty;
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
 * This class is only used inside Servoy Developer code - it represents a combination of child property of formComponent type inside
 * some form-component-container + a child component of the form component that that property uses.<br/><br/>
 *
 * For example if a list form component would point in it's "containedForm" property to a form component that has inside it a
 * button named "b1", then a WebFormComponentChildType would be created for ["containedForm", "b1"]. <br/><br/>
 *
 * So each property of form component type will end up creating as many WebFormComponentChildType as child components are in the form
 * component that a form component property type prop. points to.<br/><br/>
 *
 * It loads this child as a persist in form designer / outline view so that the developer can easily interact with it...<br/>
 * <b>This persist is not available at runtime, in clients</b> - where form component contents are flattened as if they were all on the same form!
 *
 * @author jcompagner
 */
public class WebFormComponentChildType extends BaseComponent implements IBasicWebObject, IParentOverridable
{

	private final PropertyDescription propertyDescription;
	private final String[] fcCompAndPropPath; // this is an array that contains stuff like [ "containedForm" ]
	private final String fcPropAndCompPathAsString; // for the UI in developer and toString/debugging

	private final FlattenedSolution fs;
	private IFormElement element;

	public WebFormComponentChildType(IBasicWebObject parentWebObject, String[] fcPropAndCompPath, FlattenedSolution fs)
	{
		super(IRepository.WEBCUSTOMTYPES, parentWebObject, parentWebObject.getID(), UUID.randomUUID());

		this.fcCompAndPropPath = fcPropAndCompPath; // something like [7C783D6E-8E26-40B9-8BDA-E2DC4F2ECDF8, containedForm, formComponent2, containedForm, n1]; can be nested, more or less
		fcPropAndCompPathAsString = String.join(".", Arrays.copyOfRange(fcPropAndCompPath, 1, fcPropAndCompPath.length)); // would be containedForm.formComponent2.containedForm.n1
		this.fs = fs;

		propertyDescription = initializeElement();
	}

	private String getRootFCPropertyNameInParent()
	{
		return fcCompAndPropPath[1];
	}

	/**
	 * Gives something like [7C783D6E-8E26-40B9-8BDA-E2DC4F2ECDF8, containedForm, formComponent2, containedForm, n1]; can be nested, more or less.<br/>
	 * So it includes even the root form component container name/uuid.
	 */
	public String[] getFcPropAndCompPath()
	{
		return fcCompAndPropPath;
	}

	/**
	 * Please use this for the UI only; for processing - looking at individual entries in this path, use {@link #getFcPropAndCompPath()} instead.
	 * This String does not start with the parent form component container, but from it's FormComponent property name.
	 */
	public String getFcPropAndCompPathAsString()
	{
		return fcPropAndCompPathAsString;
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
					JSONObject obj = ((JSONArray)value).optJSONObject(i);
					if (obj != null)
					{
						WebCustomType customType = createWebCustomType(elementPD, propertyName, i, UUID.fromString(obj.optString(IChildWebObject.UUID_KEY)));
						customType.setTypeName(typeName);
						customArray.add(customType);
					}
					else customArray.add(null);
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
				array.put(object != null ? object.getFullJsonInFrmFile() : JSONObject.NULL);
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

		JSONObject jsonAndPurePersistPropsFlattened = getJson(false, true);
		Object propVal;
		if (!jsonAndPurePersistPropsFlattened.has(propertyName))
		{
			// see if it has a default value if it's a pure persist prop (for example getExtendsID() should return the default int value, not null)
			propVal = convertFromJavaType(propertyName, super.getProperty(propertyName));
			// TODO are default json/.spec/IPropertyType based values handled correctly or should we do that here as well?
		}
		else propVal = jsonAndPurePersistPropsFlattened.opt(propertyName);

		return convertToJavaType(propertyName, propVal);
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
		// the "current" prefix in names is there because we go through the fcPropAndCompPath array - in case of nested form components; so all these variables
		// will point to the the current place in that path that we go through
		FormElement currentNgFormElementOfParentFC = FormElementHelper.INSTANCE.getFormElement(getParentComponent(), fs, new PropertyPath(), true);

		JSONObject currentFCPropertyValue = (JSONObject)getParentComponent().getProperty(getRootFCPropertyNameInParent()); // rootFCPropertyNameInParent is fcPropAndCompPath[1]
		PropertyDescription currentFCCachedPD = FormComponentPropertyType.INSTANCE.getPropertyDescription(getRootFCPropertyNameInParent(),
			currentFCPropertyValue, fs);
		Form currentFCPersist = FormComponentPropertyType.INSTANCE.getForm(currentFCPropertyValue, fs);

		PropertyDescription currentChildComponentSpec = null;
		JSONObject currentChildComponentValue = null;

		// fcPropAndCompPath is something like [7C783D6E-8E26-40B9-8BDA-E2DC4F2ECDF8, containedForm, formComponent2, containedForm, n1]; we already looked at index 1 above and we don't care about index 0 as we have the parent/root element anyway
		int i = 2;
		while (i < fcCompAndPropPath.length)
		{
			// get the child Component
			String childCompName = fcCompAndPropPath[i];
			currentChildComponentSpec = currentFCCachedPD.getProperty(childCompName);

			currentChildComponentValue = currentFCPropertyValue.optJSONObject(childCompName);
			if (currentChildComponentValue == null)
			{
				currentChildComponentValue = new JSONObject();
				currentFCPropertyValue.put(childCompName, currentChildComponentValue);
			}

			i++;

			if (i < fcCompAndPropPath.length)
			{
				// if this was not the last entry in fcPropAndCompPath array, but it's a form component container, then read the form component property of that form component container
				// this means that current child component is a form component container

				// get the current form component property / cache etc.
				String propertyNameInsideCurrentFC = fcCompAndPropPath[i];
				PropertyDescription propertyInsideCurrentFCPD = currentChildComponentSpec.getProperty(propertyNameInsideCurrentFC);

				currentFCPropertyValue = currentChildComponentValue.optJSONObject(propertyNameInsideCurrentFC);
				if (currentFCPropertyValue == null)
				{
					currentFCPropertyValue = new JSONObject();
					currentChildComponentValue.put(propertyNameInsideCurrentFC, currentFCPropertyValue);
				}

				if (propertyInsideCurrentFCPD.getType() == FormComponentPropertyType.INSTANCE && currentFCPersist != null)
				{
					// this is a nested form component, try to find that FormElement so we have the full flattened properties.
					FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(currentNgFormElementOfParentFC, currentFCCachedPD,
						(JSONObject)currentNgFormElementOfParentFC.getPropertyValue(currentFCCachedPD.getName()), currentFCPersist, fs);

					for (FormElement fe : cache.getFormComponentElements())
					{
						String[] feComponentAndPropertyNamePath = ((AbstractBase)fe.getPersistIfAvailable())
							.getRuntimeProperty(FormElementHelper.FC_COMPONENT_AND_PROPERTY_NAME_PATH);
						if (feComponentAndPropertyNamePath != null &&
							Arrays.equals(feComponentAndPropertyNamePath, 1, feComponentAndPropertyNamePath.length, fcCompAndPropPath, 1, i))
						{
							currentNgFormElementOfParentFC = fe;
							currentFCCachedPD = propertyInsideCurrentFCPD;
							currentFCPersist = FormComponentPropertyType.INSTANCE.getForm(fe.getPropertyValue(currentFCCachedPD.getName()), fs);
							break;
						}
					}
				}
				i++;
			}
		}

		if (currentFCPersist != null)
		{
			// get the merged/fully flattened form element from the form component cache for the current parent form element.
			FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(currentNgFormElementOfParentFC, currentFCCachedPD,
				(JSONObject)currentNgFormElementOfParentFC.getPropertyValue(currentFCCachedPD.getName()), currentFCPersist, fs);
			for (FormElement fe : cache.getFormComponentElements())
			{
				String[] feComponentAndPropertyNamePath = ((AbstractBase)fe.getPersistIfAvailable())
					.getRuntimeProperty(FormElementHelper.FC_COMPONENT_AND_PROPERTY_NAME_PATH);

				if (feComponentAndPropertyNamePath != null &&
					Arrays.equals(feComponentAndPropertyNamePath, 1, feComponentAndPropertyNamePath.length, fcCompAndPropPath, 1,
						fcCompAndPropPath.length))
				{
					// element is found which is the fully flattened one that has all the properties.
					// this is used in the getJSON() when the flattened form must be returned.
					element = (IFormElement)fe.getPersistIfAvailable();
					this.type = element.getTypeID(); // so that it is aware of pure persist prop default values better (like extendsID int)
					break;
				}
			}
		}
		return currentChildComponentSpec;
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
			if (jsonObject == null || !jsonObject.has(getRootFCPropertyNameInParent()))
			{
				JSONObject parentObject = new JSONObject();
				JSONObject superValue = (JSONObject)((IBasicWebObject)getParent()).getProperty(getRootFCPropertyNameInParent());
				parentObject.put(FormComponentPropertyType.SVY_FORM, superValue.get(FormComponentPropertyType.SVY_FORM));
				// this set property will override the cloned from super parent json in the parent, if the parent has a super persist.
				((IBasicWebObject)getParent()).setProperty(getRootFCPropertyNameInParent(), parentObject);
				if (!flattened)
				{
					propertyValue = parentObject;
				}
			}
			else if (!flattened)
			{
				propertyValue = jsonObject.getJSONObject(getRootFCPropertyNameInParent());
			}
		}

		if (propertyValue == null)
		{
			propertyValue = (JSONObject)((IBasicWebObject)getParent()).getProperty(getRootFCPropertyNameInParent());
		}

		for (int i = 2; i < fcCompAndPropPath.length; i++)
		{
			String pathToken = fcCompAndPropPath[i]; // can be an form component type property name or the name of a child component in that form component prop's form component
			JSONObject value = propertyValue.optJSONObject(pathToken);
			if (value == null)
			{
				value = new JSONObject();
				propertyValue.put(pathToken, value);
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
		return new WebFormComponentChildType((IBasicWebObject)newPersist, fcCompAndPropPath, fs);
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
			return getParent().equals(webFormComponentChildType.getParent()) && Arrays.equals(fcCompAndPropPath, 1, fcCompAndPropPath.length,
				webFormComponentChildType.fcCompAndPropPath, 1, webFormComponentChildType.fcCompAndPropPath.length);
		}
		return false;
	}

	@Override
	public String toString()
	{
		return getParentComponent().getName() + "." + getFcPropAndCompPathAsString();
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

	@Override
	public <T> T getRuntimeProperty(RuntimeProperty<T> property)
	{
		return (element instanceof AbstractBase elementAB) ? elementAB.getRuntimeProperty(property) : super.getRuntimeProperty(property);
	}

}
