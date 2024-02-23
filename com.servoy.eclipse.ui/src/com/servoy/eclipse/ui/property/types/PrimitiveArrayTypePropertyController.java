/*
	This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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
import java.util.List;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.json.JSONArray;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ICustomType;
import org.sablo.specification.property.types.DefaultPropertyType;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.ButtonCellEditor;
import com.servoy.eclipse.ui.property.ComplexProperty;
import com.servoy.eclipse.ui.property.ConvertorObjectCellEditor.IObjectTextConverter;
import com.servoy.eclipse.ui.property.ISetterAwarePropertySource;
import com.servoy.eclipse.ui.property.JSONArrayTypePropertyController;
import com.servoy.eclipse.ui.property.PDPropertySource;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.PersistPropertySource.PropertyDescriptorWrapper;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Utils;

/**
 * Property controller to be used in properties view for arrays of primitive types.
 *
 * @author rgansevles
 */
public class PrimitiveArrayTypePropertyController extends JSONArrayTypePropertyController
{
	protected final PropertyDescription webComponentPropertyDescription;
	protected final PersistContext persistContext;

	public PrimitiveArrayTypePropertyController(Object id, String displayName, PersistContext persistContext,
		PropertyDescription webComponentPropertyDescription)
	{
		super(id, displayName);
		this.webComponentPropertyDescription = webComponentPropertyDescription;
		this.persistContext = persistContext;
	}

	@Override
	public void resetPropertyValue(ISetterAwarePropertySource propertySource)
	{
		propertySource.setPropertyValue(getId(), getValueForReset());
	}

	@Override
	protected IObjectTextConverter getMainObjectTextConverter()
	{
		return null;
	}

	@Override
	protected String getLabelText(Object element)
	{
		return element == null ? "null" : (((Object[])element).length == 0 ? "[]" : "[...]");
	}

	@Override
	protected void createNewElement(ButtonCellEditor cellEditor, Object oldValue)
	{
		// insert at position 0 an empty/null value
		Object[] newValue = Utils.arrayAdd((Object[])oldValue, getNewElementInitialValue(), false);
		cellEditor.applyValue(newValue);
	}

	@Override
	protected ArrayPropertySource createArrayElementPropertySource(ComplexProperty<Object> complexP)
	{
		return new JSONArrayPropertySource(complexP)
		{
			@Override
			protected void addChildPropertyDescriptors(Object arrayV)
			{
				Object[] arrayValue = (Object[])arrayV;
				if (arrayValue == null) return;
				FlattenedSolution flattenedEditingSolution = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(), persistContext.getContext());
				Form form = (Form)(Utils.isInheritedFormElement(persistContext.getPersist(), persistContext.getContext()) ? persistContext.getContext()
					: persistContext.getPersist()).getAncestor(IRepository.FORMS);
				List<IPropertyDescriptor> createdPDs = new ArrayList<>();

				for (int i = 0; i < arrayValue.length; i++)
				{
					try
					{
						PropertyDescriptorWrapper propertyDescriptorWrapper = new PersistPropertySource.PropertyDescriptorWrapper(
							PDPropertySource.createPropertyHandlerFromSpec(getArrayElementPD(), persistContext), arrayValue[i]);
						createdPDs.add(new JSONArrayItemPropertyDescriptorWrapper(
							PersistPropertySource.createPropertyDescriptor(this, getIdFromIndex(i), persistContext, readOnly,
								propertyDescriptorWrapper, "[" + i + "]", flattenedEditingSolution, form),
							i, this));
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
				}
				ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, persistContext.getPersist(), false);
				elementPropertyDescriptors = createdPDs.toArray(new IPropertyDescriptor[createdPDs.size()]);
			}

			public PropertyDescription getArrayElementPD()
			{
				return ((ICustomType< ? >)webComponentPropertyDescription.getType()).getCustomJSONTypeDefinition();
			}

			@Override
			protected Object getDefaultElementProperty(Object id)
			{
				Object dflt = getArrayElementPD().getDefaultValue();
				if (dflt == null && (getArrayElementPD().getType() instanceof DefaultPropertyType dpt))
					dflt = dpt.defaultValue(getArrayElementPD());

				return dflt;
			}

			@Override
			protected Object deleteElementAtIndex(int idx)
			{
				return Utils.arrayRemoveElement((Object[])getEditableValue(), idx);
			}

			@Override
			protected Object insertNewElementAfterIndex(int idx)
			{
				return Utils.arrayInsert((Object[])getEditableValue(), new Object[] { getNewElementInitialValue() }, idx + 1, 1);
			}

			@Override
			protected void defaultSetElement(Object value, int idx)
			{
				Object[] newValue = (Object[])getEditableValue();
				newValue[idx] = value;
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
		};
	}

	@Override
	protected Object getNewElementInitialValue()
	{
		// normally, one would store only the initial value in the persist's properties;
		// any default values from .spec file or the PropertyType itself don't need to be stored;
		// defaults would be returned by getters anyway, and created/assigned at runtime if needed
		// but that is not the case for arrays... only direct component properties and NGCustomJSONObjectType look at defaults for all their properties (as defined in .soec)
		// but NGCustomJSONArrayType does not - because all indexes of an array (up to length) are always defined; on objects/direct props of components some properties might not be defined => they look at defaults from .spec and property type
		PropertyDescription elementDef = ((ICustomType< ? >)webComponentPropertyDescription.getType()).getCustomJSONTypeDefinition();

		// the following are all primitives, so I don't think we need to worry about conversions (is what we choose below a design value, a sablo value etc.)
		Object valueToSetOnNewElements = elementDef.getInitialValue();
		if (valueToSetOnNewElements == null) valueToSetOnNewElements = elementDef.getDefaultValue();
		if (valueToSetOnNewElements == null && (elementDef.getType() instanceof DefaultPropertyType dpt))
			valueToSetOnNewElements = dpt.defaultValue(elementDef);

		return valueToSetOnNewElements;
	}

	@Override
	protected Object getValueForReset()
	{
		Object defaultValue = webComponentPropertyDescription.getDefaultValue();
		if (defaultValue == null) return null;
		else if (defaultValue instanceof JSONArray jsonArray)
		{
			Object[] javaArray = new Object[jsonArray.length()];
			for (int i = 0; i < javaArray.length; i++)
				javaArray[i] = jsonArray.get(i); // this should work as this class deals with arrays of primitives
			return javaArray;
		}
		else return null; // should never end up here, only if the .spec file is incorrect (it gives a default value for array which is not an array)
	}

}
