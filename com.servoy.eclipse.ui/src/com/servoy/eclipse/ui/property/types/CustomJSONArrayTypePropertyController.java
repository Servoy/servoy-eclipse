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

import java.util.ArrayList;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.json.JSONArray;
import org.json.JSONException;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ICustomType;

import com.servoy.eclipse.model.util.ModelUtils;
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
import com.servoy.eclipse.ui.property.PersistPropertySource.PropertyDescriptorWrapper;
import com.servoy.eclipse.ui.property.PropertyController;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.Utils;

/**
 * Property controller to be used in properties view for custom json arrays.
 *
 * @author acostescu
 */
public class CustomJSONArrayTypePropertyController extends PropertyController<JSONArray, Object> implements IPropertySetter<JSONArray, PersistPropertySource>
{

	private static IObjectTextConverter JSONARRAY_TEXT_CONVERTER = new JSONArrayTextConverter();
	private static ILabelProvider labelProvider = null;

	private final PersistContext persistContext;
	private final PropertyDescription propertyDescription;

	public CustomJSONArrayTypePropertyController(Object id, String displayName, PersistContext persistContext, PropertyDescription propertyDescription)
	{
		super(id, displayName);
		this.persistContext = persistContext;
		this.propertyDescription = propertyDescription;
	}

	public PropertyDescription getArrayElementPD()
	{
		return ((ICustomType< ? >)propertyDescription.getType()).getCustomJSONTypeDefinition();
	}

	@Override
	protected IPropertyConverter<JSONArray, Object> createConverter()
	{
		return new CustomJSONArrayPropertyConverter();
	}

	class CustomJSONArrayPropertyConverter extends ComplexPropertyConverter<JSONArray>
	{
		@Override
		public Object convertProperty(Object id, JSONArray value)
		{
			return new ComplexProperty<JSONArray>(value)
			{
				@Override
				public IPropertySource getPropertySource()
				{
					return new CustomJSONArrayPropertySource(this);
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
					return element != null ? (((JSONArray)element).length() == 0 ? "[]" : "[...]") : Messages.LabelNone; // to suggest to the user that he can click to edit directly
				}
			};
		}
		return labelProvider;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		ComposedCellEditor cellEditor = new ComposedCellEditor(new ConvertorObjectCellEditor(JSONARRAY_TEXT_CONVERTER), new ComposedCellEditor(
			new ButtonCellEditor()
			{

				@Override
				protected void updateButtonState(Button buttonWidget, Object value)
				{
					buttonWidget.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(
						value != null ? ISharedImages.IMG_TOOL_DELETE : ISharedImages.IMG_OBJ_ADD));
					buttonWidget.setEnabled(true);
					buttonWidget.setToolTipText(value != null ? "Clears the property value."
						: "Creates an empty property value '[]' to be able to expand node.");
				}

				@Override
				protected Object getValueToSetOnClick(Object oldPropertyValue)
				{
					return oldPropertyValue != null ? null : new ServoyJSONArray();
				}

			}, new ButtonCellEditor()
			{

				@Override
				protected void updateButtonState(Button buttonWidget, Object value)
				{
					buttonWidget.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
					buttonWidget.setVisible(value != null);
					buttonWidget.setToolTipText("Adds a new array item below.");
				}

				@Override
				protected Object getValueToSetOnClick(Object oldPropertyValue)
				{
					// insert at position 0 an empty/null value
					JSONArray previousValue = (JSONArray)oldPropertyValue;
					ServoyJSONArray newValue = new ServoyJSONArray();
					for (int i = previousValue.length(); i >= 0; i--)
					{
						try
						{
							newValue.put(i + 1, previousValue.get(i));
						}
						catch (JSONException e)
						{
							ServoyLog.logError(e);
						}
					}
					return newValue;
				}

			}, false), false);
		cellEditor.create(parent);

		return cellEditor;
	}

	public static class JSONArrayTextConverter implements IObjectTextConverter
	{

		public String isCorrectString(String value)
		{
			if (value.length() > 0)
			{
				try
				{
					new JSONArray(value);
				}
				catch (JSONException e)
				{
					return "Please use valid JSON array content (eg. '[ \"a\", \"b\" ]'). Error: " + e.getMessage().replace("{", "'{'").replace("}", "'}'"); // the replace is needed as this string will go through eclipse MessageFormatter which has special meaning for { and }
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
				return new ServoyJSONArray(value);
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e); // should never get here due to validation above in isCorrectString(...)
				return null;
			}
		}

		public String isCorrectObject(Object value)
		{
			if (value == null || (value instanceof JSONArray))
			{
				return null;
			}
			else
			{
				ServoyLog.logWarning("JSON array property contains non JSONArray content", null);
				return "Value is not a JSONArray as expected";
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
				return ((JSONArray)value).toString(0).replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ');
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e); // should never happen
				return "Cannot convert the JSONArray into a string";
			}
		}

	}

	protected class CustomJSONArrayPropertySource extends ComplexPropertySource<JSONArray>
	{

		protected IPropertyDescriptor[] elementPropertyDescriptors;

		public CustomJSONArrayPropertySource(ComplexProperty<JSONArray> complexProperty)
		{
			super(complexProperty);
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			if (elementPropertyDescriptors == null)
			{
				JSONArray arrayValue = getEditableValue();
				if (arrayValue != null)
				{
					FlattenedSolution flattenedEditingSolution = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(),
						persistContext.getContext());
					Form form = (Form)(Utils.isInheritedFormElement(persistContext.getPersist(), persistContext.getContext()) ? persistContext.getContext()
						: persistContext.getPersist()).getAncestor(IRepository.FORMS);
					ArrayList<IPropertyDescriptor> createdPDs = new ArrayList<IPropertyDescriptor>();
					Object arrayElementInPersist = ((IBasicWebObject)persistContext.getPersist()).getProperty(propertyDescription.getName());

					for (int i = 0; i < arrayValue.length(); i++)
					{
						try
						{
							// array element can be either some value or it could be nested object types - in which case the persistContext of the
							// child needs to use the child IBasicWebObject
							PersistContext persistContextForElement = persistContext;
							if (arrayElementInPersist instanceof WebCustomType[])
							{
								persistContextForElement = PersistContext.create(((WebCustomType[])arrayElementInPersist)[i], persistContext.getContext());
							}
							PropertyDescriptorWrapper propertyDescriptorWrapper = new PersistPropertySource.PropertyDescriptorWrapper(
								PDPropertySource.createPropertyHandlerFromSpec(getArrayElementPD()), arrayValue.get(i));
							createdPDs.add(PersistPropertySource.createPropertyDescriptor(CustomJSONArrayPropertySource.this, String.valueOf(i),
								persistContextForElement, readOnly, propertyDescriptorWrapper, '[' + String.valueOf(i) + ']', flattenedEditingSolution, form));
						}
						catch (JSONException e)
						{
							ServoyLog.logError(e);
						}
						catch (RepositoryException e)
						{
							ServoyLog.logError(e);
						}
					}
					elementPropertyDescriptors = createdPDs.toArray(new IPropertyDescriptor[createdPDs.size()]);
				}
				else elementPropertyDescriptors = new IPropertyDescriptor[0];
			}
			return elementPropertyDescriptors;
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			try
			{
				return getEditableValue().get(Integer.valueOf((String)id));
			}
			catch (NumberFormatException | JSONException e)
			{
				ServoyLog.logError(e);
			}
			return null;
		}

		@Override
		public JSONArray setComplexPropertyValue(Object id, Object v)
		{
			JSONArray newValue = getEditableValue();
			try
			{
				newValue = new ServoyJSONArray(getEditableValue().toString()); // so that when saved in persist it sees it as changed (we make a copy so that it is a reference change)
				newValue.put(Integer.valueOf((String)id), v);
				((IBasicWebObject)persistContext.getPersist()).setJsonSubproperty(propertyDescription.getName(), newValue);
			}
			catch (NumberFormatException | JSONException e)
			{
				ServoyLog.logError(e);
			}
			return newValue;
		}

	}

	@Override
	public void setProperty(PersistPropertySource propertySource, JSONArray value)
	{
		propertySource.setPersistPropertyValue(getId(), value);
	}

	@Override
	public JSONArray getProperty(PersistPropertySource propertySource)
	{
		return (JSONArray)propertySource.getPersistPropertyValue(getId());
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
		ServoyJSONArray toSet = null;
		if (defValue instanceof String)
		{
			try
			{
				toSet = new ServoyJSONArray((String)defValue);
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e);
			}
		}
		propertySource.setPropertyValue(getId(), toSet);
	}

}
