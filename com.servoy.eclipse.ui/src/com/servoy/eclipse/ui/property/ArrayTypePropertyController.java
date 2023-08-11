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

import static com.servoy.eclipse.ui.property.ArrayTypePropertyController.ArrayItemPropertyDescriptorWrapper.ArrayActions.DELETE_CURRENT_COMMAND_VALUE;
import static com.servoy.eclipse.ui.property.ArrayTypePropertyController.ArrayItemPropertyDescriptorWrapper.ArrayActions.INSERT_NEW_AFTER_CURRENT_COMMAND_VALUE;
import static com.servoy.eclipse.ui.property.ArrayTypePropertyController.ArrayItemPropertyDescriptorWrapper.ArrayActions.UNDO_DELETE_CURRENT_COMMAND_VALUE;
import static com.servoy.eclipse.ui.property.ArrayTypePropertyController.ArrayItemPropertyDescriptorWrapper.ArrayActions.UNDO_INSERT_NEW_AFTER_CURRENT_COMMAND_VALUE;

import org.eclipse.core.runtime.IAdaptable;
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

import com.servoy.eclipse.core.util.ReturnValueSnippet;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.ArrayTypePropertyController.ArrayItemPropertyDescriptorWrapper.ArrayActions;
import com.servoy.eclipse.ui.property.ComplexProperty.ComplexPropertyConverter;
import com.servoy.eclipse.ui.property.ConvertingCellEditor.ICellEditorConverter;
import com.servoy.eclipse.ui.property.ConvertorObjectCellEditor.IObjectTextConverter;
import com.servoy.j2db.util.IDelegate;

/**
 * Property controller to be used in properties view for custom json arrays.
 *
 * @author acostescu
 */
public abstract class ArrayTypePropertyController extends PropertyController<Object, Object> implements IPropertySetter<Object, ISetterAwarePropertySource>
{
	protected ILabelProvider labelProvider = null;

	public ArrayTypePropertyController(Object id, String displayName)
	{
		super(id, displayName);
	}

	/**
	 * Called if the value is not null; the returned string will be presented in the array property's cell.
	 */
	protected abstract String getLabelText(Object element);

	protected abstract IObjectTextConverter getMainObjectTextConverter();

	protected abstract boolean isNotSet(Object value);

	protected abstract Object createEmptyPropertyValue();

	/**
	 * @return new main value with the given element inserted.
	 */
	protected abstract Object insertElementAtIndex(int i, Object elementValue, Object oldMainValue); // RAGTEST hier weg?

	protected abstract Object getNewElementInitialValue(); // RAGTEST hier weg?

	protected abstract void createNewElement(); // RAGTEST hier weg?

	protected abstract ArrayPropertySource getArrayElementPropertySource(ComplexProperty<Object> complexProperty);

	@Override
	protected IPropertyConverter<Object, Object> createConverter()
	{
		return new ArrayPropertyConverter();
	}

	class ArrayPropertyConverter extends ComplexPropertyConverter<Object>
	{
		@Override
		public Object convertProperty(Object id, Object value)
		{
			return new ComplexProperty<Object>(value)
			{
				@Override
				public IPropertySource getPropertySource()
				{
					return getArrayElementPropertySource(this);
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
					return element != null ? getLabelText(element) : Messages.LabelNone; // to suggest to the user that he can click to edit directly
				}
			};
		}
		return labelProvider;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		ButtonCellEditor clearButton = new ButtonSetValueCellEditor()
		{

			@Override
			protected void updateButtonState(Button buttonWidget, Object value)
			{
				buttonWidget.setImage(
					PlatformUI.getWorkbench().getSharedImages().getImage(isNotSet(value) ? ISharedImages.IMG_OBJ_ADD : ISharedImages.IMG_ETOOL_CLEAR));
				buttonWidget.setEnabled(true);
				buttonWidget.setToolTipText(isNotSet(value) ? "Creates an empty property value '[]' to be able to expand node." : "Clears the property value.");
			}

			@Override
			protected Object getValueToSetOnClick(Object oldPropertyValue)
			{
				if (!isNotSet(oldPropertyValue)) return null;
				else return createEmptyPropertyValue();
			}

		};

		ButtonCellEditor addButton = new ButtonCellEditor()
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
				buttonWidget.setToolTipText("Prepends a new array item.\n" +
					"To add a new item after another item,\nclick the '+' button on a specific item.");
				buttonWidget.setEnabled(true);

				if (visible == isNotSet(value))
				{
					visible = !isNotSet(value); // visibility is not enough - we don't want the space ocuppied at all so we change layout data as well
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
			protected void buttonClicked()
			{
				createNewElement();
			}

		};

		ComposedCellEditor cellEditor = new ComposedCellEditor(addButton, clearButton, false, true, 0);
		if (getMainObjectTextConverter() != null)
		{
			cellEditor = new ComposedCellEditor(new ConvertorObjectCellEditor(getMainObjectTextConverter()), cellEditor, false, false, 0);
		}
		cellEditor.create(parent);

		return cellEditor;
	}


	public static class ArrayPropertyChildId
	{

		public final Object arrayPropId;
		public final int idx;

		public ArrayPropertyChildId(Object id, int idx)
		{
			this.arrayPropId = id;
			this.idx = idx;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((arrayPropId == null) ? 0 : arrayPropId.hashCode());
			result = prime * result + idx;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ArrayPropertyChildId other = (ArrayPropertyChildId)obj;
			if (arrayPropId == null)
			{
				if (other.arrayPropId != null) return false;
			}
			else if (!arrayPropId.equals(other.arrayPropId)) return false;
			if (idx != other.idx) return false;
			return true;
		}

		@Override
		public String toString()
		{
			return String.valueOf(idx);
		}

	}

	protected abstract class ArrayPropertySource extends ComplexPropertySource<Object> implements ISetterAwarePropertySource
	{

		protected IPropertyDescriptor[] elementPropertyDescriptors;
		private ArrayActions undoValue;

		public ArrayPropertySource(ComplexProperty<Object> complexProperty)
		{
			super(complexProperty);
		}

		protected abstract void addChildPropertyDescriptors(Object arrayV);

		protected abstract Object getElementValue(int idx);

		protected abstract Object insertNewElementAfterIndex(int idx);

		protected abstract Object deleteElementAtIndex(final int idx);

		protected abstract Object setComplexElementValueImpl(int idx, Object v);

		protected abstract void defaultSetElement(Object value, final int idx);

		protected abstract Object defaultGetElement(final int idx);

		protected abstract boolean defaultIsElementSet(final int idx);

		protected int getIndexFromId(ArrayPropertyChildId id)
		{
			return id.idx;
		}

		protected abstract ArrayPropertyChildId getIdFromIndex(int idx);

		//	protected abstract String getTypeName();


		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			if (elementPropertyDescriptors == null)
			{
				Object arrayV = getEditableValue();
				if (!isNotSet(arrayV))
				{
					addChildPropertyDescriptors(arrayV);
				}
				else elementPropertyDescriptors = new IPropertyDescriptor[0];
			}
			return elementPropertyDescriptors;
		}

		/**
		 * Adds the + and - buttons on the right side of the cell editor. Those buttons are needed for inserting/removing items in/from the array.
		 * @param index the index of this child property inside the array.
		 * @param customJSONArrayPropertySource
		 * @param createPropertyDescriptor the real property descriptor that is able to handle the value
		 * @return a wrapper IPropertyDescriptor that forwards everything to the given one, but it alters the cell editor as needed.
		 */
		protected IPropertyDescriptor addButtonsToPD(IPropertyDescriptor realPropertyDescriptor, int index)
		{
			return new ArrayItemPropertyDescriptorWrapper(realPropertyDescriptor, index, this);
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			try
			{
				final int idx = getIndexFromId((ArrayPropertyChildId)id);
				return getElementValue(idx);
			}
			catch (NumberFormatException e)
			{
				ServoyLog.logError(e);
			}
			return null;
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

			return ISetterAwarePropertySource.super.undoSetProperty(id);
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

				return setComplexElementValueImpl(idx, v);
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
			try
			{
				final int idx = getIndexFromId((ArrayPropertyChildId)id);

				defaultSetElement(value, idx);
			}
			catch (NumberFormatException e)
			{
				ServoyLog.logError(e);
			}
		}

		@Override
		public Object defaultGetProperty(Object id)
		{
			try
			{
				final int idx = getIndexFromId((ArrayPropertyChildId)id);
				return defaultGetElement(idx);
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
				final int idx = getIndexFromId((ArrayPropertyChildId)id);
				return defaultIsElementSet(idx);
			}
			catch (NumberFormatException e)
			{
				ServoyLog.logError(e);
			}
			return false;
		}


	}

	/**
	 * Just a proxy/wrapper class that is trying to affect only the cell editor of the given property descriptor.
	 * It will add the insert/remove buttons required for inserting/removing into/from arrays.
	 *
	 * @author acostescu
	 */
	protected static class ArrayItemPropertyDescriptorWrapper
		implements IPropertyController<Object, Object>, IPropertySetter<Object, ISetterAwarePropertySource>, IProvidesTooltip, IAdaptable
	{
		enum ArrayActions
		{
			DELETE_CURRENT_COMMAND_VALUE,
			UNDO_DELETE_CURRENT_COMMAND_VALUE,
			INSERT_NEW_AFTER_CURRENT_COMMAND_VALUE,
			UNDO_INSERT_NEW_AFTER_CURRENT_COMMAND_VALUE
		}

		protected final IPropertyDescriptor basePD;
		protected final String index;
		protected final ArrayPropertySource arrayPropertySource;

		public ArrayItemPropertyDescriptorWrapper(IPropertyDescriptor basePD, int index, ArrayPropertySource arrayPropertySource)
		{
			this.basePD = basePD;
			this.index = String.valueOf(index);
			this.arrayPropertySource = arrayPropertySource;
		}

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
				public CellEditor run(Composite... args)
				{
					return basePD.createPropertyEditor(args[0]);
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
					return DELETE_CURRENT_COMMAND_VALUE; // RAGTEST via handler?
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
//					// RAGTEST naar aparte klasse
//					EditorRagtestHandler handler = RagtestRegistry.getRagtestHandler(EditorRagtestActions.CREATE_COMPONENT_RAGTEST);
//					if (handler == null)
//					{
//						ServoyLog.logWarning("No handler registered for adding component " + EditorRagtestActions.CREATE_COMPONENT_RAGTEST, null);
//					}
//					else
//					{
//						Object id = getId();
//						String parentKey;
//						if (id instanceof ArrayPropertyChildId)
//						{
//							parentKey = String.valueOf(((ArrayPropertyChildId)id).arrayPropId);
//						}
//						else
//						{
//							parentKey = String.valueOf(id);
//						}
//
//						if (oldValue instanceof IPersist)
//						{
//							handler.createComponent(((IPersist)oldValue).getUUID(), parentKey, arrayPropertySource.getTypeName());
//						}
//						System.err.println("RAGTEST1 ");
//					}
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
		public void resetPropertyValue(ISetterAwarePropertySource propertySource)
		{
			IPropertyDescriptor basePDLocal = getRootBasePD();
			if (basePDLocal instanceof IPropertySetter< ? , ? >) ((IPropertySetter)basePDLocal).resetPropertyValue(propertySource);
			else propertySource.defaultResetProperty(getId());
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

		public Object getAdapter(Class adapter)
		{
			if (Comparable.class == adapter)
			{
				return Integer.valueOf(index);
			}
			return null;
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

}
