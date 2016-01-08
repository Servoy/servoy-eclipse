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

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.CustomJSONObjectType;
import org.sablo.specification.property.ICustomType;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.ArrayTypePropertyController;
import com.servoy.eclipse.ui.property.ComplexProperty;
import com.servoy.eclipse.ui.property.ConvertorObjectCellEditor.IObjectTextConverter;
import com.servoy.eclipse.ui.property.ISetterAwarePropertySource;
import com.servoy.eclipse.ui.property.PDPropertySource;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.PersistPropertySource.PropertyDescriptorWrapper;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;

/**
 * Property controller to be used in properties view for arrays of custom types.
 *
 * @author acostescu
 */
public class CustomArrayTypePropertyController extends ArrayTypePropertyController
{

	protected final PropertyDescription propertyDescription;
	protected final PersistContext persistContext;

	public CustomArrayTypePropertyController(Object id, String displayName, PersistContext persistContext, PropertyDescription propertyDescription)
	{
		super(id, displayName);
		this.propertyDescription = propertyDescription;
		this.persistContext = persistContext;
	}

	public PropertyDescription getArrayElementPD()
	{
		return ((ICustomType< ? >)propertyDescription.getType()).getCustomJSONTypeDefinition();
	}

	@Override
	protected Object getNewElementInitialValue()
	{
		// when user adds/inserts a new item in the array normally a null is inserted
		// but for custom object properties most of the time the uses will want to have an object so that it can be directly expanded (without clicking one more time to make it {} from null)
		return (getArrayElementPD().getType() instanceof CustomJSONObjectType< ? , ? >) ? new ServoyJSONObject() : JSONObject.NULL;
	}

	@Override
	protected CustomArrayPropertySource getArrayElementPropertySource(ComplexProperty<Object> complexProperty)
	{
		return new CustomArrayPropertySource(complexProperty);
	}

	public class CustomArrayPropertySource extends ArrayPropertySource
	{

		public CustomArrayPropertySource(ComplexProperty<Object> complexProperty)
		{
			super(complexProperty);
		}

		@Override
		protected int getIndexFromId(String id)
		{
			return Integer.valueOf(id).intValue();
		}

		@Override
		protected String getIdFromIndex(int idx)
		{
			return String.valueOf(idx);
		}

		@Override
		protected void addChildPropertyDescriptors(Object arrayV)
		{
			Object[] arrayValue = (Object[])arrayV;
			FlattenedSolution flattenedEditingSolution = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(), persistContext.getContext());
			Form form = (Form)(Utils.isInheritedFormElement(persistContext.getPersist(), persistContext.getContext()) ? persistContext.getContext()
				: persistContext.getPersist()).getAncestor(IRepository.FORMS);
			ArrayList<IPropertyDescriptor> createdPDs = new ArrayList<IPropertyDescriptor>();
			Object arrayElementInPersist = ((IBasicWebObject)persistContext.getPersist()).getProperty(propertyDescription.getName());

			if (arrayValue == null) return;
			for (int i = 0; i < arrayValue.length; i++)
			{
				try
				{
					// array element can be either some value or it could be nested object types - in which case the persistContext of the
					// child needs to use the child IBasicWebObject
					PersistContext persistContextForElement = persistContext;
					if (arrayElementInPersist instanceof IChildWebObject[])
					{
						persistContextForElement = PersistContext.create(((IChildWebObject[])arrayElementInPersist)[i], persistContext.getContext());
					}
					PropertyDescriptorWrapper propertyDescriptorWrapper = new PersistPropertySource.PropertyDescriptorWrapper(
						PDPropertySource.createPropertyHandlerFromSpec(getArrayElementPD(), persistContext), arrayValue[i]);
					createdPDs.add(addButtonsToPD(PersistPropertySource.createPropertyDescriptor(this, getIdFromIndex(i), persistContextForElement, readOnly,
						propertyDescriptorWrapper, '[' + getIdFromIndex(i) + ']', flattenedEditingSolution, form), i));
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
			elementPropertyDescriptors = createdPDs.toArray(new IPropertyDescriptor[createdPDs.size()]);
		}

		@Override
		protected Object getElementValue(final int idx)
		{
			return PersistPropertySource.adjustPropertyValueToGet(getIdFromIndex(idx), getPropertyDescriptors()[idx], this);
		}

		@Override
		protected Object setComplexElementValueImpl(int idx, Object v)
		{
			PersistPropertySource.adjustPropertyValueAndSet(getIdFromIndex(idx), v, getPropertyDescriptors()[idx], this);
			return getEditableValue();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.eclipse.ui.property.ISetterAwarePropertySource#defaultResetProperty(java.lang.Object)
		 */
		@Override
		public void defaultResetProperty(Object id)
		{
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.eclipse.ui.property.ArrayTypePropertyController.ArrayPropertySource#insertNewElementAfterIndex(int)
		 */
		@Override
		protected Object insertNewElementAfterIndex(int idx)
		{
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.eclipse.ui.property.ArrayTypePropertyController.ArrayPropertySource#deleteElementAtIndex(int)
		 */
		@Override
		protected ServoyJSONArray deleteElementAtIndex(int idx)
		{
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.eclipse.ui.property.ArrayTypePropertyController.ArrayPropertySource#defaultSetElement(java.lang.Object, int)
		 */
		@Override
		protected void defaultSetElement(Object value, int idx)
		{
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.eclipse.ui.property.ArrayTypePropertyController.ArrayPropertySource#defaultGetElement(int)
		 */
		@Override
		protected Object defaultGetElement(int idx)
		{
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.eclipse.ui.property.ArrayTypePropertyController.ArrayPropertySource#defaultIsElementSet(int)
		 */
		@Override
		protected boolean defaultIsElementSet(int idx)
		{
			// TODO Auto-generated method stub
			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.eclipse.ui.property.ArrayTypePropertyController.ArrayPropertySource#resetComplexElementValue(java.lang.Object, int)
		 */
		@Override
		protected Object resetComplexElementValue(Object id, int idx)
		{
			// TODO Auto-generated method stub
			return null;
		}

		//TODO
//		@Override
//		protected void defaultElementWasSet(Object newMainValue)
//		{
//			((IBasicWebObject)persistContext.getPersist()).setJsonSubproperty(propertyDescription.getName(), newMainValue);
//		}
//
//		@Override
//		protected Object getDefaultElementProperty(Object id)
//		{
//			return getArrayElementPD().getDefaultValue();
//		}

	}

	@Override
	public void resetPropertyValue(ISetterAwarePropertySource propertySource)
	{
		if (propertyDescription.hasDefault())
		{
			Object defValue = propertyDescription.getDefaultValue();
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
		else propertySource.defaultResetProperty(getId());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.property.ArrayTypePropertyController#getLabelText(java.lang.Object)
	 */
	@Override
	protected String getLabelText(Object element)
	{
		return ((element != null) ? (((Object[])element).length == 0 ? "[]" : "[...]") : "null");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.property.ArrayTypePropertyController#getMainObjectTextConverter()
	 */
	@Override
	protected IObjectTextConverter getMainObjectTextConverter()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.property.ArrayTypePropertyController#isNotSet(java.lang.Object)
	 */
	@Override
	protected boolean isNotSet(Object value)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.property.ArrayTypePropertyController#createEmptyPropertyValue()
	 */
	@Override
	protected Object createEmptyPropertyValue()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.property.ArrayTypePropertyController#insertElementAtIndex(int, java.lang.Object, java.lang.Object)
	 */
	@Override
	protected Object insertElementAtIndex(int i, Object elementValue, Object oldMainValue)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
