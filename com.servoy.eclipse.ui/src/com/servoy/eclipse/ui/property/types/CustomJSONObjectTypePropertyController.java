/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.ButtonCellEditor;
import com.servoy.eclipse.ui.property.ComplexProperty;
import com.servoy.eclipse.ui.property.ComplexProperty.ComplexPropertyConverter;
import com.servoy.eclipse.ui.property.ComplexPropertySource;
import com.servoy.eclipse.ui.property.ComposedCellEditor;
import com.servoy.eclipse.ui.property.ConvertorObjectCellEditor;
import com.servoy.eclipse.ui.property.ConvertorObjectCellEditor.IObjectTextConverter;
import com.servoy.eclipse.ui.property.IPropertyConverter;
import com.servoy.eclipse.ui.property.IPropertySetter;
import com.servoy.eclipse.ui.property.PDPropertySource;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.PropertyController;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * Property controller to be used in properties view for custom json objects.
 *
 * @author acostescu
 */
public class CustomJSONObjectTypePropertyController extends PropertyController<JSONObject, Object> implements
	IPropertySetter<JSONObject, PersistPropertySource>
{

	private static IObjectTextConverter JSONOBJECT_TEXT_CONVERTER = new JSONObjectTextConverter();
	private static ILabelProvider labelProvider = null;

	private final PersistContext persistContext;
	private final PropertyDescription propertyDescription;

	public CustomJSONObjectTypePropertyController(Object id, String displayName, PersistContext persistContext, PropertyDescription propertyDescription)
	{
		super(id, displayName);
		this.persistContext = persistContext;
		this.propertyDescription = propertyDescription;
	}

	@Override
	protected IPropertyConverter<JSONObject, Object> createConverter()
	{
		return new CustomJSONObjectPropertyConverter();
	}

	class CustomJSONObjectPropertyConverter extends ComplexPropertyConverter<JSONObject>
	{
		@Override
		public Object convertProperty(Object id, JSONObject value)
		{
			return new ComplexProperty<JSONObject>(value)
			{
				@Override
				public IPropertySource getPropertySource()
				{
					return new CustomJSONObjectPropertySource(this);
				}
			};
		}
	}

	@Override
	public ILabelProvider getLabelProvider()
	{
		if (labelProvider == null)
		{
			labelProvider = new LabelProvider()
			{
				@Override
				public String getText(Object element)
				{
					return element != null ? "{...}" : Messages.LabelNone; // to suggest to the user that he can click to edit directly
				}
			};
		}
		return labelProvider;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		ComposedCellEditor cellEditor = new ComposedCellEditor(new ConvertorObjectCellEditor(JSONOBJECT_TEXT_CONVERTER), new ButtonCellEditor()
		{

			@Override
			protected void updateButtonState(Button buttonWidget, Object value)
			{
				buttonWidget.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(
					value != null ? ISharedImages.IMG_TOOL_DELETE : ISharedImages.IMG_OBJ_ADD));
				buttonWidget.setEnabled(true);
				buttonWidget.setToolTipText(value != null ? "Clears the property value." : "Creates an empty property value '{}' to be able to expand node.");
			}

			@Override
			protected Object getValueToSetOnClick(Object oldPropertyValue)
			{
				return oldPropertyValue != null ? null : new ServoyJSONObject();
			}

		}, false);
		cellEditor.create(parent);

		return cellEditor;
	}

	public static class JSONObjectTextConverter implements IObjectTextConverter
	{

		public String isCorrectString(String value)
		{
			if (value.length() > 0)
			{
				try
				{
					new JSONObject(value);
				}
				catch (JSONException e)
				{
					return "Please use valid JSON object content (eg. '{ \"a\" : 1, \"b\" : \"str\" }'). Error: " +
						e.getMessage().replace("{", "'{'").replace("}", "'}'"); // the replace is needed as this string will go through eclipse MessageFormatter which has special meaning for { and }
				}
			}
			return null;
		}

		public Object convertToObject(String value)
		{
			if (value == null || value.trim().length() == 0)
			{
				return null;
			}

			try
			{
				return new ServoyJSONObject(value, false);
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e); // should never get here due to validation above in isCorrectString(...)
				return null;
			}
		}

		public String isCorrectObject(Object value)
		{
			if (value == null || (value instanceof JSONObject))
			{
				return null;
			}
			else
			{
				ServoyLog.logWarning("JSON object property contains non JSONObject content", null);
				return "Value is not a JSONObject as expected";
			}
		}

		public String convertToString(Object value)
		{
			if (value == null)
			{
				return "";
			}
			try
			{
				return ((JSONObject)value).toString(0).replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ');
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e); // should never happen
				return "Cannot convert the JSONObject into a string";
			}
		}

	}

	protected class CustomJSONObjectPropertySource extends ComplexPropertySource<JSONObject>
	{

		protected PDPropertySource underlyingPropertySource;

		public CustomJSONObjectPropertySource(ComplexProperty<JSONObject> complexProperty)
		{
			super(complexProperty);
		}

		protected PDPropertySource getUnderlyingPropertySource()
		{
			IPersist persist = persistContext.getPersist(); // parent persist holding property with propertyDescription
			if (persist instanceof IBasicWebObject)
			{
				persist = (IPersist)((IBasicWebObject)persist).getProperty(propertyDescription.getName());
				if (persist != null)
				{
					if (underlyingPropertySource == null || persist != underlyingPropertySource.getPersist())
					{
						underlyingPropertySource = new PDPropertySource(PersistContext.create(persist, persistContext.getContext()), readOnly,
							propertyDescription);
					}
				}
				else
				{
					// value of this property is null probably - so we don't show nested contents
					underlyingPropertySource = null;
				}
			}
			else
			{
				underlyingPropertySource = null;
				ServoyLog.logError("Unexpected. Persist of custom json object handler is null or not instance of IWebObject: (" + propertyDescription + ", " +
					persistContext + ")", new RuntimeException());
			}
			return underlyingPropertySource;
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			PDPropertySource underlying = getUnderlyingPropertySource();
			return underlying != null ? underlying.getPropertyDescriptors() : new IPropertyDescriptor[0];
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			PDPropertySource underlying = getUnderlyingPropertySource();
			return underlying != null ? underlying.getPropertyValue(id) : null;
		}

		@Override
		public JSONObject setComplexPropertyValue(Object id, Object v)
		{
			PDPropertySource underlying = getUnderlyingPropertySource();
			if (underlying != null) underlying.setPropertyValue(id, v);
			return getEditableValue();
		}

	}

	@Override
	public void setProperty(PersistPropertySource propertySource, JSONObject value)
	{
		propertySource.setPersistPropertyValue(getId(), value);
	}

	@Override
	public JSONObject getProperty(PersistPropertySource propertySource)
	{
		return (JSONObject)propertySource.getPersistPropertyValue(getId());
	}

	@Override
	public boolean isPropertySet(PersistPropertySource propertySource)
	{
		return propertySource.isPersistPropertySet(getId());
	}

	@Override
	public void resetPropertyValue(PersistPropertySource propertySource)
	{
		Object defValue = propertyDescription.getDefaultValue();
		JSONObject toSet = null;
		if (defValue instanceof String)
		{
			try
			{
				toSet = new ServoyJSONObject((String)defValue, false);
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e);
			}
		}
		propertySource.setPropertyValue(getId(), toSet);
	}

}
