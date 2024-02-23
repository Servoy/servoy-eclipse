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

package com.servoy.eclipse.ui.property;

import static com.servoy.eclipse.ui.property.JSONArrayTypePropertyController.JSONArrayPropertySource.JSONArrayItemPropertyDescriptorWrapper.ArrayActions.DELETE_CURRENT_COMMAND_VALUE;
import static com.servoy.eclipse.ui.property.JSONArrayTypePropertyController.JSONArrayPropertySource.JSONArrayItemPropertyDescriptorWrapper.ArrayActions.INSERT_NEW_AFTER_CURRENT_COMMAND_VALUE;
import static com.servoy.eclipse.ui.property.JSONArrayTypePropertyController.JSONArrayPropertySource.JSONArrayItemPropertyDescriptorWrapper.ArrayActions.UNDO_DELETE_CURRENT_COMMAND_VALUE;
import static com.servoy.eclipse.ui.property.JSONArrayTypePropertyController.JSONArrayPropertySource.JSONArrayItemPropertyDescriptorWrapper.ArrayActions.UNDO_INSERT_NEW_AFTER_CURRENT_COMMAND_VALUE;

import java.util.function.Consumer;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.json.JSONArray;
import org.json.JSONException;

import com.servoy.eclipse.core.util.ReturnValueSnippet;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.ConvertingCellEditor.ICellEditorConverter;
import com.servoy.eclipse.ui.property.ConvertorObjectCellEditor.IObjectTextConverter;
import com.servoy.eclipse.ui.property.JSONArrayTypePropertyController.JSONArrayPropertySource.JSONArrayItemPropertyDescriptorWrapper.ArrayActions;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * Property controller to be used in properties view for custom json arrays.
 *
 * @author acostescu
 */
// unfortunately here we can't use JSONArray in generics even if we would make ArrayTypePropertyController generic because the value can also be JSONObject.NULL which would give classcastexceptions...
public abstract class JSONArrayTypePropertyController extends ArrayTypePropertyController implements ICanHandleJSONNullValues
{
	private static IObjectTextConverter JSONARRAY_TEXT_CONVERTER = new JSONArrayTextConverter();

	private Consumer<Object> setParentValueOnItsSource;

	public JSONArrayTypePropertyController(Object id, String displayName)
	{
		super(id, displayName);
	}

	protected abstract Object getNewElementInitialValue();

	protected abstract Object getValueForReset();

	@Override
	protected String getLabelText(Object element)
	{
		return (!ServoyJSONObject.isJavascriptNull(element) ? (((JSONArray)element).length() == 0 ? "[]" : "[...]") : "null");
	}

	@Override
	protected IObjectTextConverter getMainObjectTextConverter()
	{
		return JSONARRAY_TEXT_CONVERTER;
	}

	@Override
	protected boolean isNotSet(Object value)
	{
		return ServoyJSONObject.isJavascriptNullOrUndefined(value);
	}

	@Override
	protected void createNewElement(ButtonCellEditor cellEditor, Object oldValue)
	{
		// insert at position 0 an empty/null value
		Object newValue = ServoyJSONArray.insertAtIndexInJSONArray((JSONArray)oldValue, 0, getNewElementInitialValue());
		cellEditor.applyValue(newValue);
	}

	public abstract class JSONArrayPropertySource extends ArrayPropertySource
	{
		public final class JSONArrayItemPropertyDescriptorWrapper extends ArrayItemPropertyDescriptorWrapper
		{
			enum ArrayActions
			{
				DELETE_CURRENT_COMMAND_VALUE,
				UNDO_DELETE_CURRENT_COMMAND_VALUE,
				INSERT_NEW_AFTER_CURRENT_COMMAND_VALUE,
				UNDO_INSERT_NEW_AFTER_CURRENT_COMMAND_VALUE
			}

			public JSONArrayItemPropertyDescriptorWrapper(IPropertyDescriptor basePD, int index, ArrayPropertySource arrayPropertySource)
			{
				super(basePD, index, arrayPropertySource);
			}

			@Override
			protected IPropertyDescriptor getRootBasePD()
			{
				IPropertyDescriptor base = basePD;
				while (base instanceof IDelegate)
				{
					Object delegate = ((IDelegate)base).getDelegate();
					if (delegate instanceof IPropertyDescriptor)
					{
						base = (IPropertyDescriptor)delegate;
					}
					else
					{
						break;
					}
				}
				return base;
			}

			@Override
			public CellEditor createPropertyEditor(Composite parent)
			{
				ComposedCellEditor cellEditor = new ComposedCellEditor(false, false, 10);

				// make sure our special values don't reach the real editor - as it could lead to exceptions (real editor doesn't expect such values)
				cellEditor.setCellEditor1(new ConvertingCellEditor<Object, Object>(new ReturnValueSnippet<CellEditor, Composite>()
				{
					@Override
					public CellEditor run(Composite arg)
					{
						return basePD.createPropertyEditor(arg);
					}
				}, new ICellEditorConverter<Object, Object>()
				{

					@Override
					public Object convertValueToBaseEditor(Object outsideWorldValue)
					{
						return outsideWorldValue;
					}

					@Override
					public Object convertValueFromBaseEditor(Object baseEditorValue)
					{
						return baseEditorValue;
					}

					@Override
					public boolean allowSetToBaseEditor(Object outsideWorldValue)
					{
						return outsideWorldValue != DELETE_CURRENT_COMMAND_VALUE && outsideWorldValue != INSERT_NEW_AFTER_CURRENT_COMMAND_VALUE;
					}

				}));

				cellEditor.setCellEditor2(new ComposedCellEditor(new ButtonSetValueCellEditor()
				{

					@Override
					protected void updateButtonState(Button buttonWidget, Object value)
					{
						buttonWidget.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_REMOVE));
						buttonWidget.setEnabled(true);
						buttonWidget.setToolTipText("Remove this array item.");
					}

					@Override
					protected Object getValueToSetOnClick(Object oldPropertyValue)
					{
						return DELETE_CURRENT_COMMAND_VALUE;
					}

				}, new ButtonSetValueCellEditor()
				{

					@Override
					protected void updateButtonState(Button buttonWidget, Object value)
					{
						buttonWidget.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
						buttonWidget.setEnabled(true);
						buttonWidget.setToolTipText("Insert a new array item below.");
					}

					@Override
					protected Object getValueToSetOnClick(Object oldPropertyValue)
					{
						return INSERT_NEW_AFTER_CURRENT_COMMAND_VALUE;
					}

				}, false, true, 0));

				cellEditor.create(parent);

				return cellEditor;
			}

			@Override
			public String getCategory()
			{
				return getRootBasePD().getCategory();
			}

			@Override
			public String getDescription()
			{
				return getRootBasePD().getDescription();
			}

			@Override
			public String getDisplayName()
			{
				return getRootBasePD().getDisplayName();
			}

			@Override
			public String[] getFilterFlags()
			{
				return getRootBasePD().getFilterFlags();
			}

			@Override
			public Object getHelpContextIds()
			{
				return getRootBasePD().getHelpContextIds();
			}

			@Override
			public Object getId()
			{
				return getRootBasePD().getId();
			}

			@Override
			public ILabelProvider getLabelProvider()
			{
				return getRootBasePD().getLabelProvider();
			}

			@Override
			public boolean isCompatibleWith(IPropertyDescriptor anotherProperty)
			{
				return getRootBasePD().isCompatibleWith(anotherProperty);
			}

			@Override
			public void setProperty(ISetterAwarePropertySource propertySource, Object value)
			{
				IPropertyDescriptor basePDLocal = getRootBasePD();
				if (basePDLocal instanceof IPropertySetter< ? , ? >) ((IPropertySetter)basePDLocal).setProperty(propertySource, value);
				else propertySource.defaultSetProperty(getId(), value);
			}

			@Override
			public Object getProperty(ISetterAwarePropertySource propertySource)
			{
				IPropertyDescriptor basePDLocal = getRootBasePD();
				if (basePDLocal instanceof IPropertySetter< ? , ? >) return ((IPropertySetter)basePDLocal).getProperty(propertySource);
				else return propertySource.defaultGetProperty(getId());
			}

			@Override
			public boolean isPropertySet(ISetterAwarePropertySource propertySource)
			{
				IPropertyDescriptor basePDLocal = getRootBasePD();
				if (basePDLocal instanceof IPropertySetter< ? , ? >) return ((IPropertySetter)basePDLocal).isPropertySet(propertySource);
				else return propertySource.isPropertySet(getId());
			}

			@Override
			public IPropertyConverter<Object, Object> getConverter()
			{
				return basePD instanceof IPropertyController< ? , ? > ? ((IPropertyController)basePD).getConverter() : null;
			}

			@Override
			public boolean supportsReadonly()
			{
				return basePD instanceof IPropertyController< ? , ? > ? ((IPropertyController)basePD).supportsReadonly() : false;
			}

			@Override
			public boolean isReadOnly()
			{
				return basePD instanceof IPropertyController< ? , ? > ? ((IPropertyController)basePD).isReadOnly() : false;
			}

			@Override
			public void setReadonly(boolean readonly)
			{
				if (basePD instanceof IPropertyController< ? , ? >)
				{
					((IPropertyController)basePD).setReadonly(readonly);
				}
			}

			@Override
			public String getTooltipText()
			{
				if (basePD instanceof IProvidesTooltip) return ((IProvidesTooltip)basePD).getTooltipText();
				else return null;
			}

			@Override
			public Object getAdapter(Class adapter)
			{
				if (Comparable.class == adapter)
				{
					return Integer.valueOf(index);
				}
				return null;
			}

		}

		private ArrayActions undoValue;

		public JSONArrayPropertySource(ComplexProperty<Object> complexProperty)
		{
			super(complexProperty);
		}

		@Override
		protected Object getElementValue(int idx)
		{
			return PersistPropertySource.adjustPropertyValueToGet(getIdFromIndex(idx), getPropertyDescriptors()[idx], this);
		}

		protected abstract Object getDefaultElementProperty(Object id);

		@Override
		protected Object setComplexElementValueImpl(int idx, Object v)
		{
			PersistPropertySource.adjustPropertyValueAndSet(getIdFromIndex(idx), v, getPropertyDescriptors()[idx], this);
			return getEditableValue();
		}

		protected Object deleteElementAtIndex(final int idx)
		{
			return ServoyJSONArray.removeIndexFromJSONArray((JSONArray)getEditableValue(), idx);
		}

		protected Object insertNewElementAfterIndex(int idx)
		{
			return ServoyJSONArray.insertAtIndexInJSONArray((JSONArray)getEditableValue(), idx + 1, getNewElementInitialValue());
		}

		@Override
		protected void defaultSetElement(Object value, final int idx)
		{
			try
			{
				Object newValue = getEditableValue();
				Object val = ServoyJSONObject.adjustJavascriptNULLForOrgJSON(value);
				((JSONArray)newValue).put(idx, val);
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e);
			}
		}

		@Override
		protected Object defaultGetElement(final int idx)
		{
			return ServoyJSONObject.adjustJavascriptNULLForJava(((JSONArray)getEditableValue()).opt(idx));
		}

		@Override
		protected boolean defaultIsElementSet(final int idx)
		{
			return ((JSONArray)getEditableValue()).length() > idx;
		}

		public final void resetPropertyValue(Object id)
		{
			setPropertyValue(id, resetComplexPropertyValue(id));
			JSONArrayTypePropertyController.this.setParentValueOnItsSource.accept(getEditableValue());
		}

		public Object resetComplexPropertyValue(Object id)
		{
			try
			{
				final int idx = getIndexFromId((ArrayPropertyChildId)id);
				return resetComplexElementValue(id, idx);
			}
			catch (NumberFormatException e)
			{
				ServoyLog.logError(e);
			}
			return null;
		}

		protected Object resetComplexElementValue(Object id, final int idx)
		{
			PersistPropertySource.adjustPropertyValueAndReset(id, getPropertyDescriptors()[idx], this);
			return PersistPropertySource.adjustPropertyValueToGet(id, getPropertyDescriptors()[idx], this);
//			return ((JSONArray)getEditableValue()).opt(idx);// this didn't do all the proper conversions (for example it ended up setting JSONObject.NULL instead of null on a controller that doesn't support that)
		}

		@Override
		public void defaultResetProperty(Object id)
		{
			defaultSetProperty(id, getDefaultElementProperty(id)); // if id would be of object or array type in which case we would need to create JSONObject or JSONArray, it shouldn't reach this code; it should be intercepted before this by the IPropertySetter method of the controller of those types; so we just use the default value here directly
		}

		/**
		 * Handle undo, revert handling of the special values
		 */
		@Override
		public boolean undoSetProperty(Object id)
		{
			if (undoValue != null)
			{
				setPropertyValue(id, undoValue);
				return true;
			}

			return false;
		}

		@Override
		public Object setComplexPropertyValue(Object id, Object v)
		{
			try
			{
				undoValue = null;
				int idx = getIndexFromId((ArrayPropertyChildId)id);
				if (v == DELETE_CURRENT_COMMAND_VALUE)
				{
					undoValue = UNDO_DELETE_CURRENT_COMMAND_VALUE;
					return deleteElementAtIndex(idx);
				}
				if (v == INSERT_NEW_AFTER_CURRENT_COMMAND_VALUE)
				{
					undoValue = UNDO_INSERT_NEW_AFTER_CURRENT_COMMAND_VALUE;
					return insertNewElementAfterIndex(idx);
				}

				// undo
				if (v == UNDO_DELETE_CURRENT_COMMAND_VALUE)
				{
					undoValue = DELETE_CURRENT_COMMAND_VALUE;
					return insertNewElementAfterIndex(idx - 1);
				}

				if (v == UNDO_INSERT_NEW_AFTER_CURRENT_COMMAND_VALUE)
				{
					undoValue = INSERT_NEW_AFTER_CURRENT_COMMAND_VALUE;
					return deleteElementAtIndex(idx + 1);
				}
			}
			catch (NumberFormatException e)
			{
				ServoyLog.logError(e);
			}

			return super.setComplexPropertyValue(id, v);
		}
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

	@Override
	public void resetPropertyValue(ISetterAwarePropertySource propertySource)
	{
		Object defValue = getValueForReset();
		JSONArray toSet = null;
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
		else if (defValue instanceof JSONArray) toSet = (JSONArray)defValue;
		propertySource.setPropertyValue(getId(), toSet);
	}

	@Override
	public Object getProperty(final ISetterAwarePropertySource propertySource)
	{
		rememberParentPropertySourceForResetElement(propertySource);

		return super.getProperty(propertySource);
	}

	public void rememberParentPropertySourceForResetElement(final IPropertySource propertySource)
	{
		// propertySource here is the parent property source which we can use when elements of the array are reset-to-default
		// to re-apply a changed value of the whole array - so that it ends up in the parent persist/custom object
		// correctly

		// reset to default expects that a reset call updates the model that the properties view is showing completely
		// so even if we set the default in an element of the array, we need to save the array as well
		// because otherwise a refresh of the properties from the model (that follows a reset) will re-read a new array
		// from the persist containing the old values; so reset to default for an element of the array would not work

		// the same problem does not appear in CustomArrayTypePropertyController, as that one works on persist objects
		// created from the root persist directly (arrays of custom objects) and those are able to save themselves back
		// when a change happens in an element; but in this class the java array is pretty dumb... so we need this
		// setParentValueAgainOnItsSource

		// also this is not needed when an element of the array is being edited in the UI, just when a reset of an element happens,
		// because setting a value in the cell editor goes directly in the set then valueChanged() of the UndoablePropertySheetEntry that then
		// knows to generate a CompoundCommand that sets the values throughout all parents as well (nested PropertySheet instances)
		// so it works a bit differently then reset...
		this.setParentValueOnItsSource = (fullArrayValToBeSet) -> {
			propertySource.setPropertyValue(getId(), fullArrayValToBeSet);
		};
	}

}
