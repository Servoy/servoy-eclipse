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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.ComplexProperty.ComplexPropertyConverter;
import com.servoy.eclipse.ui.property.ConvertorObjectCellEditor.IObjectTextConverter;

/**
 * Property controller to be used in properties view for custom json objects.
 *
 * @author acostescu
 */
public abstract class ObjectTypePropertyController extends PropertyController<Object, Object> implements IPropertySetter<Object, ISetterAwarePropertySource>
{

	private static ILabelProvider labelProvider = null;

	public ObjectTypePropertyController(Object id, String displayName)
	{
		super(id, displayName);
	}

	/**
	 * Called if the value is not null; the returned string will be presented in the array property's cell.
	 */
	protected abstract String getLabelText(Object element);

	protected abstract ObjectPropertySource getObjectChildPropertySource(ComplexProperty<Object> complexProperty);

	protected abstract IObjectTextConverter getMainObjectTextConverter();


	@Override
	protected IPropertyConverter<Object, Object> createConverter()
	{
		return new ObjectPropertyConverter();
	}

	class ObjectPropertyConverter extends ComplexPropertyConverter<Object>
	{
		@Override
		public Object convertProperty(Object id, Object value)
		{
			return new ComplexProperty<Object>(value)
			{
				@Override
				public IPropertySource getPropertySource()
				{
					return getObjectChildPropertySource(this);
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
		CellEditor cellEditor = new ButtonSetValueCellEditor()
		{
			@Override
			protected void updateButtonState(Button buttonWidget, Object value)
			{
				buttonWidget.setImage(
					PlatformUI.getWorkbench().getSharedImages().getImage(!isJSONNull(value) ? ISharedImages.IMG_ETOOL_CLEAR : ISharedImages.IMG_OBJ_ADD));
				buttonWidget.setEnabled(true);
				buttonWidget.setToolTipText(
					!isJSONNull(value) ? "Clears the property value." : "Creates an empty property value '{}' to be able to expand node.");
			}

			@Override
			protected Object getValueToSetOnClick(Object oldPropertyValue)
			{
				return toggleValue(oldPropertyValue);
			}

		};
		if (getMainObjectTextConverter() != null)
		{
			cellEditor = new ComposedCellEditor(new ConvertorObjectCellEditor(getMainObjectTextConverter()), cellEditor, false, false, 0);
		}
		cellEditor.create(parent);
		return cellEditor;
	}

	protected abstract Object toggleValue(Object oldPropertyValue);

	protected abstract boolean isJSONNull(Object val);

	protected abstract class ObjectPropertySource extends ComplexPropertySource<Object> implements ISetterAwarePropertySource
	{

		public ObjectPropertySource(ComplexProperty<Object> complexProperty)
		{
			super(complexProperty);
		}

		@Override
		public abstract IPropertyDescriptor[] createPropertyDescriptors();

		@Override
		public abstract Object getPropertyValue(Object id);

		@Override
		public abstract Object setComplexPropertyValue(Object id, Object v);

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
