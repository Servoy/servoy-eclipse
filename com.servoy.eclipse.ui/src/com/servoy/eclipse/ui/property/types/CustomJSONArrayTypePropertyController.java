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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
import com.servoy.eclipse.ui.property.ISetterAwarePropertySource;
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
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;

/**
 * Property controller to be used in properties view for custom json arrays.
 *
 * @author acostescu
 */
// unfortunately here we can't use JSONArray in generics cause the value can also be JSONObject.NULL which would give classcastexceptions...
public class CustomJSONArrayTypePropertyController extends PropertyController<Object, Object> implements IPropertySetter<Object, ISetterAwarePropertySource>
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
	protected IPropertyConverter<Object, Object> createConverter()
	{
		return new CustomJSONArrayPropertyConverter();
	}

	class CustomJSONArrayPropertyConverter extends ComplexPropertyConverter<Object>
	{
		@Override
		public Object convertProperty(Object id, Object value)
		{
			return new ComplexProperty<Object>(value)
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
					return element != null ? (!ServoyJSONObject.isJavascriptNull(element) ? (((JSONArray)element).length() == 0 ? "[]" : "[...]") : "null")
						: Messages.LabelNone; // to suggest to the user that he can click to edit directly
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
						!ServoyJSONObject.isJavascriptNullOrUndefined(value) ? ISharedImages.IMG_TOOL_DELETE : ISharedImages.IMG_OBJ_ADD));
					buttonWidget.setEnabled(true);
					buttonWidget.setToolTipText(value != null ? "Clears the property value."
						: "Creates an empty property value '[]' to be able to expand node.");
				}

				@Override
				protected Object getValueToSetOnClick(Object oldPropertyValue)
				{
					return (!ServoyJSONObject.isJavascriptNullOrUndefined(oldPropertyValue)) ? null : new ServoyJSONArray();
				}

			}, new ButtonCellEditor()
			{

				private Control buttonEditorControl; // actually this is the button control
				private boolean visible = true;

				@Override
				protected Control createControl(Composite parentC)
				{
					Composite buttonVisibilityWrapper = new Composite(parentC, SWT.NONE); // cell editor activate/deactivate force control visibility; but we want to control the button visibility even if the editor is active so we add a wrapper here so that the button's visibility is not directly controlled by the cell editor
					GridLayout gridLayout = new GridLayout();
					gridLayout.marginHeight = 0;
					gridLayout.marginWidth = 0;
					gridLayout.horizontalSpacing = 0;
					gridLayout.verticalSpacing = 0;
					gridLayout.numColumns = 1;
					buttonVisibilityWrapper.setLayout(gridLayout);

					buttonEditorControl = super.createControl(buttonVisibilityWrapper);

					GridData gd = new GridData();
					gd.horizontalAlignment = SWT.FILL;
					gd.grabExcessHorizontalSpace = true;
					gd.grabExcessVerticalSpace = true;
					gd.verticalAlignment = SWT.FILL;
					buttonEditorControl.setLayoutData(gd);

					updateButtonVisibility();

					return buttonVisibilityWrapper;
				}

				@Override
				protected void updateButtonState(Button buttonWidget, Object value)
				{
					buttonWidget.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
					buttonWidget.setToolTipText("Adds a new array item below.");
					buttonWidget.setEnabled(true);

					if (visible != (value != null))
					{
						visible = (value != null); // visibility is not enough - we don't want the space ocuppied at all so we change layout data as well
						updateButtonVisibility();
					}
				}

				private void updateButtonVisibility()
				{
					if (buttonEditorControl != null && buttonEditorControl.getLayoutData() != null)
					{
						if (visible)
						{
							((GridData)buttonEditorControl.getLayoutData()).exclude = false;
						}
						else
						{
							((GridData)buttonEditorControl.getLayoutData()).exclude = true; // layout no longer changes bounds of this control
							buttonEditorControl.setSize(new Point(0, 0));
						}

						// relayout as needed to not show blank area instead of button for no reason
						Composite c = buttonEditorControl.getParent();
						while (c != null && !ComposedCellEditor.isRootComposedCellEditor(c))
							c = c.getParent();
						if (ComposedCellEditor.isRootComposedCellEditor(c)) c.layout(true);
					}
				}

				@Override
				protected Object getValueToSetOnClick(Object oldPropertyValue)
				{
					// insert at position 0 an empty/null value
					return ServoyJSONArray.insertAtIndexInJSONArray((JSONArray)oldPropertyValue, 0, null);
				}

			}, false, true), false, false);
		cellEditor.create(parent);

		return cellEditor;
	}

	public static class JSONArrayTextConverter implements IObjectTextConverter
	{

		public String isCorrectString(String value)
		{
			if (value.length() > 0 && !"null".equals(value))
			{
				try
				{
					new JSONArray(value);
				}
				catch (JSONException e)
				{
					return "Please use valid JSON array content (eg. '[ \"a\", \"b\" ]'). Error: " +
						e.getMessage().replace("'", "''").replace("{", "'{'").replace("}", "'}'"); // the replace is needed as this string will go through eclipse MessageFormatter which has special meaning for { and }
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
			if ("null".equals(value)) return ServoyJSONObject.NULL_FOR_JAVA; // temporary value that shouldn't reach the real JSONObject, but it is meant to not equal null (java-wise), cause JSONObject.NULL.equals(null) is true and then properties view cannot make the distinction correctly

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
			if (ServoyJSONObject.isJavascriptNullOrUndefined(value) || (value instanceof JSONArray))
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
			if (ServoyJSONObject.isJavascriptUndefined(value))
			{
				return "";
			}
			if (ServoyJSONObject.isJavascriptNull(value))
			{
				return "null";
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

	protected class CustomJSONArrayPropertySource extends ComplexPropertySource<Object> implements ISetterAwarePropertySource
	{

		protected IPropertyDescriptor[] elementPropertyDescriptors;

		public CustomJSONArrayPropertySource(ComplexProperty<Object> complexProperty)
		{
			super(complexProperty);
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			if (elementPropertyDescriptors == null)
			{
				Object arrayV = getEditableValue();
				if (!ServoyJSONObject.isJavascriptNullOrUndefined(arrayV))
				{
					JSONArray arrayValue = (JSONArray)arrayV;
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
								PDPropertySource.createPropertyHandlerFromSpec(getArrayElementPD()), arrayValue.opt(i));
							createdPDs.add(PersistPropertySource.createPropertyDescriptor(CustomJSONArrayPropertySource.this, String.valueOf(i),
								persistContextForElement, readOnly, propertyDescriptorWrapper, '[' + String.valueOf(i) + ']', flattenedEditingSolution, form));
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
				final int idx = Integer.valueOf((String)id).intValue();
				return PersistPropertySource.adjustPropertyValueToGet(id, getPropertyDescriptors()[idx], this);
			}
			catch (NumberFormatException e)
			{
				ServoyLog.logError(e);
			}
			return null;
		}

		@Override
		public Object setComplexPropertyValue(final Object id, Object v)
		{
			try
			{
				final int idx = Integer.valueOf((String)id);
				PersistPropertySource.adjustPropertyValueAndSet(id, v, getPropertyDescriptors()[idx], this);
			}
			catch (NumberFormatException e)
			{
				ServoyLog.logError(e);
			}
			return getEditableValue();
		}

		@Override
		public void defaultSetProperty(Object id, Object value)
		{
			Object newValue = getEditableValue();
			Object val = ServoyJSONObject.adjustJavascriptNULLForOrgJSON(value);
			try
			{
				final int idx = Integer.valueOf((String)id);
				((JSONArray)newValue).put(idx, val);
			}
			catch (JSONException | NumberFormatException e)
			{
				ServoyLog.logError(e);
			}
			((IBasicWebObject)persistContext.getPersist()).setJsonSubproperty(propertyDescription.getName(), newValue);
		}

		@Override
		public Object defaultGetProperty(Object id)
		{
			try
			{
				final int idx = Integer.valueOf((String)id);
				return ServoyJSONObject.adjustJavascriptNULLForJava(((JSONArray)getEditableValue()).opt(idx));
			}
			catch (NumberFormatException e)
			{
				ServoyLog.logError(e);
			}
			return null;
		}

		@Override
		public boolean defaultIsPropertySet(Object id)
		{
			try
			{
				final int idx = Integer.valueOf((String)id);
				return ((JSONArray)getEditableValue()).length() > idx;
			}
			catch (NumberFormatException e)
			{
				ServoyLog.logError(e);
			}
			return false;
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
