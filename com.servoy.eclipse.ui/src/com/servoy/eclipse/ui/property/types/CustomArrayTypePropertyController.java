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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ICustomType;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.RagtestRegistry;
import com.servoy.eclipse.ui.RagtestRegistry.EditorRagtestActions;
import com.servoy.eclipse.ui.RagtestRegistry.EditorRagtestHandler;
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
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.util.Utils;

/**
 * Property controller to be used in properties view for arrays of custom types.
 *
 * @author acostescu
 */
public class CustomArrayTypePropertyController extends ArrayTypePropertyController
{
	protected final PropertyDescription webComponentPropertyDescription;
	protected final PersistContext persistContext;

	public CustomArrayTypePropertyController(Object id, String displayName, PersistContext persistContext, PropertyDescription webComponentPropertyDescription)
	{
		super(id, displayName);
		this.webComponentPropertyDescription = webComponentPropertyDescription;
		this.persistContext = persistContext;
	}

	public PropertyDescription getArrayElementPD()
	{
		return ((ICustomType< ? >)webComponentPropertyDescription.getType()).getCustomJSONTypeDefinition();
	}

	@Override
	protected void createNewElement()
	{
		EditorRagtestHandler handler = RagtestRegistry.getRagtestHandler(EditorRagtestActions.CREATE_COMPONENT_RAGTEST);
		if (handler == null)
		{
			ServoyLog.logWarning("No handler registered for adding component " + EditorRagtestActions.CREATE_COMPONENT_RAGTEST, null);
		}
		else
		{
			Object id = getId();
			String parentKey;
			if (id instanceof ArrayPropertyChildId)
			{
				parentKey = String.valueOf(((ArrayPropertyChildId)id).arrayPropId);
			}
			else
			{
				parentKey = String.valueOf(id);
			}


			handler.createComponent(persistContext.getPersist().getUUID(), parentKey, getTypeName());

			System.err.println("RAGTEST2 ");
		}
	}

	protected String getTypeName()
	{
		return webComponentPropertyDescription.getType().getName().indexOf(".") > 0 ? webComponentPropertyDescription.getType().getName().split("\\.")[1]
			: webComponentPropertyDescription.getType().getName();
	}

	// RAGTEST weg
	private WebCustomType getNewElementValue(int index)
	{
		// when user adds/inserts a new item in the array normally a null is inserted
		// but for custom object properties most of the time the uses will want to have an object so that it can be directly expanded (without clicking one more time to make it {} from null)
		PropertyDescription arrayElementPD = getArrayElementPD();
		if (arrayElementPD.getType() instanceof ICustomType< ? >)
		{
			String typeName = webComponentPropertyDescription.getType().getName().indexOf(".") > 0
				? webComponentPropertyDescription.getType().getName().split("\\.")[1]
				: webComponentPropertyDescription.getType().getName();
			IBasicWebObject parent = (IBasicWebObject)persistContext.getPersist();
			WebCustomType customType = WebCustomType.createNewInstance(parent, arrayElementPD, webComponentPropertyDescription.getName(), index, true);
			customType.setTypeName(typeName);
			// Do not add customType to parent here, this method should be without side-effect for undo/redo to work properly
			return customType;
		}
		return null;
	}

	// RAGTEST weg
	@Override
	protected WebCustomType getNewElementInitialValue()
	{
		return getNewElementValue(0);
	}

	// RAGTEST weg

	@Override
	protected CustomArrayPropertySource getArrayElementPropertySource(ComplexProperty<Object> complexProperty)
	{
		return new CustomArrayPropertySource(complexProperty);
	}

	public class CustomArrayPropertySource extends ArrayPropertySource
	{
		private final class CustomArrayItemPropertyDescriptorWrapper extends ArrayItemPropertyDescriptorWrapper
		{
			/**
			 * @param basePD
			 * @param index
			 * @param arrayPropertySource
			 */
			private CustomArrayItemPropertyDescriptorWrapper(IPropertyDescriptor basePD, int index, ArrayPropertySource arrayPropertySource)
			{
				super(basePD, index, arrayPropertySource);
			}

			@Override
			protected void addNewItemAfter(Object oldValue)
			{
//				// RAGTEST naar aparte klasse
				EditorRagtestHandler handler = RagtestRegistry.getRagtestHandler(EditorRagtestActions.CREATE_COMPONENT_RAGTEST);
				if (handler == null)
				{
					ServoyLog.logWarning("No handler registered for adding component " + EditorRagtestActions.CREATE_COMPONENT_RAGTEST, null);
				}
				else
				{
					Object id = getId();
					String parentKey;
					if (id instanceof ArrayPropertyChildId)
					{
						parentKey = String.valueOf(((ArrayPropertyChildId)id).arrayPropId);
					}
					else
					{
						parentKey = String.valueOf(id);
					}

					if (oldValue instanceof IPersist)
					{
						handler.createComponent(((IPersist)oldValue).getUUID(), parentKey, getTypeName());
					}
					System.err.println("RAGTEST1x ");
				}
			}
		}

		public CustomArrayPropertySource(ComplexProperty<Object> complexProperty)
		{
			super(complexProperty);
		}

		@Override
		protected ArrayPropertyChildId getIdFromIndex(int idx)
		{
			return new ArrayPropertyChildId(getId(), idx);
		}

		@Override
		protected void addChildPropertyDescriptors(Object arrayV)
		{
			if (arrayV == null) return;
			Object[] arrayValue = (Object[])arrayV;

			FlattenedSolution flattenedEditingSolution = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(), persistContext.getContext());
			Form form = (Form)(Utils.isInheritedFormElement(persistContext.getPersist(), persistContext.getContext()) ? persistContext.getContext()
				: persistContext.getPersist()).getAncestor(IRepository.FORMS);
			List<IPropertyDescriptor> createdPDs = new ArrayList<>();
			Object arrayElementInPersist = ((IBasicWebObject)persistContext.getPersist()).getProperty(webComponentPropertyDescription.getName());

			for (int i = 0; i < arrayValue.length; i++)
			{
				try
				{
					// array element can be either some value or it could be nested object types - in which case the persistContext of the
					// child needs to use the child IBasicWebObject
					PersistContext persistContextForElement = persistContext;
					if (arrayElementInPersist instanceof IChildWebObject[])
					{
						IChildWebObject childPersist = ((IChildWebObject[])arrayElementInPersist)[i];
						if (childPersist != null) persistContextForElement = PersistContext.create(childPersist, persistContext.getContext());
					}
					PropertyDescriptorWrapper propertyDescriptorWrapper = new PersistPropertySource.PropertyDescriptorWrapper(
						PDPropertySource.createPropertyHandlerFromSpec(getArrayElementPD(), persistContext), arrayValue[i]);
					createdPDs.add(createArrayItemPropertyDescriptor(
						PersistPropertySource.createPropertyDescriptor(this, getIdFromIndex(i), persistContextForElement, readOnly,
							propertyDescriptorWrapper, "[" + i + "]", flattenedEditingSolution, form),
						i));
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
			ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, persistContext.getPersist(), false);
			elementPropertyDescriptors = createdPDs.toArray(new IPropertyDescriptor[createdPDs.size()]);
		}

		@Override
		protected IPropertyDescriptor createArrayItemPropertyDescriptor(IPropertyDescriptor propertyDescriptor, int index)
		{
			return new CustomArrayItemPropertyDescriptorWrapper(propertyDescriptor, index, this);
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

		@Override
		public void defaultResetProperty(Object id)
		{
			defaultSetProperty(id, getDefaultElementProperty(id));
		}

		@Override
		protected Object insertNewElementAfterIndex(int idx)
		{
			return insertElementAtIndex(idx + 1, getNewElementValue(idx + 1), getEditableValue());
		}

		@Override
		protected Object deleteElementAtIndex(int idx)
		{
			Object[] arrayValue = (Object[])getEditableValue();
			Object[] newArrayValue = (Object[])Array.newInstance(arrayValue.getClass().getComponentType(), arrayValue.length - 1);
			System.arraycopy(arrayValue, 0, newArrayValue, 0, idx);
			System.arraycopy(arrayValue, idx + 1, newArrayValue, idx, arrayValue.length - idx - 1);
			resetIndexes(newArrayValue);
			return newArrayValue;
		}

		@Override
		protected void defaultSetElement(Object value, int idx)
		{
			if (idx < 0 || idx >= ((Object[])getEditableValue()).length) return;
			if (value == null && getEditableValue() instanceof IChildWebObject[])
			{
				((Object[])getEditableValue())[idx] = getNewElementValue(idx);
			}
			else
			{
				((Object[])getEditableValue())[idx] = value;
			}
		}

		@Override
		protected Object defaultGetElement(int idx)
		{
			return ((Object[])getEditableValue())[idx];
		}

		@Override
		protected boolean defaultIsElementSet(int idx)
		{
			return ((Object[])getEditableValue()).length > idx;
		}

		@Override
		public void resetPropertyValue(Object id)
		{
			try
			{
				int idx = getIndexFromId((ArrayPropertyChildId)id);
				PersistPropertySource.adjustPropertyValueAndReset(id, getPropertyDescriptors()[idx], this);
			}
			catch (NumberFormatException e)
			{
				ServoyLog.logError(e);
			}
		}

		protected void defaultElementWasSet(Object newMainValue)
		{
			((IBasicWebObject)persistContext.getPersist()).setProperty(webComponentPropertyDescription.getName(), newMainValue);
		}

		protected Object getDefaultElementProperty(Object id)
		{
			return getArrayElementPD().getDefaultValue();
		}

	}

	@Override
	public void resetPropertyValue(ISetterAwarePropertySource propertySource)
	{
		propertySource.defaultResetProperty(getId());
	}

	@Override
	protected String getLabelText(Object element)
	{
		return ((element instanceof Object[]) ? (((Object[])element).length == 0 ? "[]" : "[...]") : "null");
	}

	@Override
	protected IObjectTextConverter getMainObjectTextConverter()
	{
		return null;
	}

	@Override
	protected boolean isNotSet(Object value)
	{
		return value == null;
	}

	@Override
	protected Object createEmptyPropertyValue()
	{
		PropertyDescription arrayElementPD = getArrayElementPD();
		if (arrayElementPD.getType() instanceof ICustomType< ? >)
		{
			return new WebCustomType[] { getNewElementInitialValue() };
		}
		return new Object[] { getNewElementInitialValue() };
	}

	@Override
	protected Object insertElementAtIndex(int i, Object elementValue, Object oldMainValue)
	{ // RAGTEST alleen voor json array
		Object[] arrayValue = (Object[])oldMainValue;
		Object[] newArrayValue = (Object[])Array.newInstance(arrayValue.getClass().getComponentType(), arrayValue.length + 1);
		System.arraycopy(arrayValue, 0, newArrayValue, 0, i);
		newArrayValue[i] = elementValue;
		System.arraycopy(arrayValue, i, newArrayValue, i + 1, arrayValue.length - i);
		resetIndexes(newArrayValue);
		return newArrayValue;
	}

	private void resetIndexes(Object[] newArrayValue)
	{
		if (newArrayValue instanceof IChildWebObject[])
		{
			for (int j = 0; j < newArrayValue.length; j++)
			{
				if (newArrayValue[j] != null) ((IChildWebObject)newArrayValue[j]).setIndex(j);
			}
		}
	}

}
