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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.ComplexProperty.ComplexPropertyConverter;
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
	private ArrayPropertySource arrayElementPropertySource;

	public ArrayTypePropertyController(Object id, String displayName)
	{
		super(id, displayName);
	}

	/**
	 * Called if the value is not null; the returned string will be presented in the array property's cell.
	 */
	protected abstract String getLabelText(Object element);

	protected IObjectTextConverter getMainObjectTextConverter()
	{
		return null;
	}

	protected boolean isNotSet(Object value)
	{
		return value == null;
	}

	protected abstract void createNewElement(ButtonCellEditor cellEditor, Object oldValue);

	protected abstract ArrayPropertySource createArrayElementPropertySource(ComplexProperty<Object> complexProperty);

	private ArrayPropertySource getArrayElementPropertySource(ComplexProperty<Object> complexProperty)
	{
		if (arrayElementPropertySource == null || !arrayElementPropertySource.hasComplexProperty(complexProperty))
		{
			arrayElementPropertySource = createArrayElementPropertySource(complexProperty);
		}
		return arrayElementPropertySource;
	}

	@Override
	protected IPropertyConverter<Object, Object> createConverter()
	{
		return new ArrayPropertyConverter();
	}

	private class ArrayPropertyConverter extends ComplexPropertyConverter<Object>
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
		ButtonCellEditor addButton = new ButtonCellEditor()
		{
			private Control buttonEditorControl; // actually this is the button control

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

				updateButtonVisibility();
			}

			private void updateButtonVisibility()
			{
				if (buttonEditorControl != null && buttonEditorControl.getLayoutData() != null)
				{
					((GridData)buttonEditorControl.getLayoutData()).exclude = false;

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
				createNewElement(this, oldValue);
			}
		};

		CellEditor cellEditor = addButton;
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

		public ArrayPropertySource(ComplexProperty<Object> complexProperty)
		{
			super(complexProperty);
		}

		protected abstract void addChildPropertyDescriptors(Object arrayV);

		protected abstract Object getElementValue(int idx);

		protected abstract Object setComplexElementValueImpl(int idx, Object v);

		protected abstract void defaultSetElement(Object value, final int idx);

		protected abstract Object defaultGetElement(final int idx);

		protected abstract boolean defaultIsElementSet(final int idx);

		protected int getIndexFromId(ArrayPropertyChildId id)
		{
			return id.idx;
		}

		protected ArrayPropertyChildId getIdFromIndex(int idx)
		{
			return new ArrayPropertyChildId(getId(), idx);
		}

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

		@Override
		public Object getPropertyValue(Object id)
		{
			try
			{
				int idx = getIndexFromId((ArrayPropertyChildId)id);
				return getElementValue(idx);
			}
			catch (NumberFormatException e)
			{
				ServoyLog.logError(e);
			}
			return null;
		}

		@Override
		public void defaultSetProperty(Object id, Object value)
		{
			try
			{
				int idx = getIndexFromId((ArrayPropertyChildId)id);
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
				int idx = getIndexFromId((ArrayPropertyChildId)id);
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
				int idx = getIndexFromId((ArrayPropertyChildId)id);
				return defaultIsElementSet(idx);
			}
			catch (NumberFormatException e)
			{
				ServoyLog.logError(e);
			}
			return false;
		}

		@Override
		public Object setComplexPropertyValue(Object id, Object v)
		{
			try
			{
				int idx = getIndexFromId((ArrayPropertyChildId)id);
				return setComplexElementValueImpl(idx, v);
			}
			catch (NumberFormatException e)
			{
				ServoyLog.logError(e);
			}
			return getEditableValue();
		}
	}

	/**
	 * Just a proxy/wrapper class that is trying to affect only the cell editor of the given property descriptor.
	 * It will add the insert/remove buttons required for inserting/removing into/from arrays.
	 *
	 * @author acostescu
	 */
	protected abstract static class ArrayItemPropertyDescriptorWrapper
		implements IPropertyController<Object, Object>, IPropertySetter<Object, ISetterAwarePropertySource>, IProvidesTooltip, IAdaptable
	{
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
