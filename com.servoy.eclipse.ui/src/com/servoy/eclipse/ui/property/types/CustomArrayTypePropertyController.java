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
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ICustomType;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.ReturnValueSnippet;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.EditorActionsRegistry;
import com.servoy.eclipse.ui.EditorActionsRegistry.EditorComponentActionHandler;
import com.servoy.eclipse.ui.EditorActionsRegistry.EditorComponentActions;
import com.servoy.eclipse.ui.property.ArrayTypePropertyController;
import com.servoy.eclipse.ui.property.ButtonCellEditor;
import com.servoy.eclipse.ui.property.ComplexProperty;
import com.servoy.eclipse.ui.property.ComposedCellEditor;
import com.servoy.eclipse.ui.property.ConvertingCellEditor;
import com.servoy.eclipse.ui.property.ConvertingCellEditor.ICellEditorConverter;
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
	private final IPropertySource persistPropertySource;

	public CustomArrayTypePropertyController(Object id, String displayName, IPropertySource persistPropertySource, PersistContext persistContext,
		PropertyDescription webComponentPropertyDescription)
	{
		super(id, displayName);
		this.persistPropertySource = persistPropertySource;
		this.webComponentPropertyDescription = webComponentPropertyDescription;
		this.persistContext = persistContext;
	}

	public PropertyDescription getArrayElementPD()
	{
		return ((ICustomType< ? >)webComponentPropertyDescription.getType()).getCustomJSONTypeDefinition();
	}

	@Override
	protected void createNewElement(ButtonCellEditor cellEditor, Object oldValue)
	{
		callHandler(handler -> {
			Object id = getId();
			if (id instanceof ArrayPropertyChildId)
			{
				String parentKey = String.valueOf(((ArrayPropertyChildId)id).arrayPropId);
				handler.createComponent(persistPropertySource, persistContext.getPersist().getUUID(), parentKey, getTypeName(), null, false);
			}
			else
			{
				String parentKey = String.valueOf(id);
				handler.createComponent(persistPropertySource, persistContext.getPersist().getUUID(), parentKey, getTypeName(), null, false);
				cellEditor.applyValue(persistPropertySource.getPropertyValue(parentKey));
			}
		});
	}

	private static void callHandler(Consumer<EditorComponentActionHandler> call)
	{
		EditorComponentActionHandler handler = EditorActionsRegistry.getHandler(EditorComponentActions.CREATE_CUSTOM_COMPONENT);
		if (handler == null)
		{
			ServoyLog.logWarning("No handler registered for adding component " + EditorComponentActions.CREATE_CUSTOM_COMPONENT, null);
		}
		else
		{
			call.accept(handler);
		}
	}

	protected String getTypeName()
	{
		return webComponentPropertyDescription.getType().getName().indexOf(".") > 0 ? webComponentPropertyDescription.getType().getName().split("\\.")[1]
			: webComponentPropertyDescription.getType().getName();
	}

	@Override
	protected CustomArrayPropertySource getArrayElementPropertySource(ComplexProperty<Object> complexProperty)
	{
		return new CustomArrayPropertySource(complexProperty);
	}

	public class CustomArrayPropertySource extends ArrayPropertySource
	{
		private final class CustomArrayItemPropertyDescriptorWrapper extends ArrayItemPropertyDescriptorWrapper
		{

			private CustomArrayItemPropertyDescriptorWrapper(IPropertyDescriptor basePD, int index, ArrayPropertySource arrayPropertySource)
			{
				super(basePD, index, arrayPropertySource);
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
						return true;
					}
				}));

				cellEditor.setCellEditor2(new ComposedCellEditor(new ButtonCellEditor()
				{
					@Override
					protected void updateButtonState(Button buttonWidget, Object value)
					{
						buttonWidget.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_REMOVE));
						buttonWidget.setEnabled(true);
						buttonWidget.setToolTipText("Remove this array item.");
					}

					@Override
					protected void buttonClicked()
					{
						if (oldValue instanceof IPersist)
						{
							callHandler(handler -> handler.deleteComponent(persistPropertySource, ((IPersist)oldValue).getUUID()));
						}
					}

				}, new ButtonCellEditor()
				{
					@Override
					protected void updateButtonState(Button buttonWidget, Object value)
					{
						buttonWidget.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
						buttonWidget.setEnabled(true);
						buttonWidget.setToolTipText("Insert a new array item below.");
					}

					@Override
					protected void buttonClicked()
					{
						if (oldValue instanceof IPersist persist)
						{
							callHandler(handler -> {
								Object id = getId();
								String parentKey;
								Integer idx = null;
								if (id instanceof ArrayPropertyChildId arrayPropertyChildId)
								{
									idx = Integer.valueOf(arrayPropertyChildId.idx + 1);
									parentKey = String.valueOf(arrayPropertyChildId.arrayPropId);
								}
								else
								{
									parentKey = String.valueOf(id);
								}
								handler.createComponent(persistPropertySource, persist.getParent().getUUID(), parentKey, getTypeName(), idx, true);
							});
						}
					}

				}, false, true, 0));

				cellEditor.create(parent);

				return cellEditor;
			}
		}

		public CustomArrayPropertySource(ComplexProperty<Object> complexProperty)
		{
			super(complexProperty);
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
					IPropertyDescriptor basePD = PersistPropertySource.createPropertyDescriptor(this, getIdFromIndex(i), persistContextForElement,
						readOnly, propertyDescriptorWrapper, "[" + i + "]", flattenedEditingSolution, form);
					createdPDs.add(new CustomArrayItemPropertyDescriptorWrapper(basePD, i, this));
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
		protected void defaultSetElement(Object value, int idx)
		{
			if (idx < 0 || idx >= ((Object[])getEditableValue()).length) return;
			((Object[])getEditableValue())[idx] = value;
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
}
