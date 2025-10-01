/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.servoy.eclipse.ui.views.properties.IMergeablePropertyDescriptor;
import com.servoy.eclipse.ui.views.properties.IMergedPropertyDescriptor;
import com.servoy.j2db.util.IDelegate;

/**
 * Property descriptor wrapper.
 * <p>
 * isCompatibleWith() returns false if read-only state is not the same for both property descriptors
 *
 */
public class DelegatePropertyController<P, E> extends PropertyController<P, E> implements IMergeablePropertyDescriptor, IDelegate<IPropertyDescriptor>
{
	protected final IPropertyDescriptor propertyDescriptor;

	public DelegatePropertyController(IPropertyDescriptor propertyDescriptor, Object id, String displayName)
	{
		super(id, displayName);
		this.propertyDescriptor = propertyDescriptor;
	}

	public DelegatePropertyController(IPropertyDescriptor propertyDescriptor, Object id)
	{
		this(propertyDescriptor, id, propertyDescriptor.getDisplayName());
	}

	public DelegatePropertyController(IPropertyDescriptor propertyDescriptor)
	{
		this(propertyDescriptor, propertyDescriptor.getId());
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		return propertyDescriptor.createPropertyEditor(parent);
	}

	@Override
	public String getCategory()
	{
		return propertyDescriptor.getCategory();
	}

	@Override
	public void setCategory(String name)
	{
		if (propertyDescriptor instanceof PropertyDescriptor)
		{
			((PropertyDescriptor)propertyDescriptor).setCategory(name);
		}
		else
		{
			super.setCategory(name);
		}
	}

	@Override
	public String getDescription()
	{
		return propertyDescriptor.getDescription();
	}

	@Override
	public String[] getFilterFlags()
	{
		return propertyDescriptor.getFilterFlags();
	}

	@Override
	public Object getHelpContextIds()
	{
		return propertyDescriptor.getHelpContextIds();
	}

	@Override
	public ILabelProvider getLabelProvider()
	{
		return propertyDescriptor.getLabelProvider();
	}

	@Override
	public IPropertyConverter<P, E> getConverter()
	{
		if (propertyDescriptor instanceof IPropertyController)
		{
			return ((IPropertyController<P, E>)propertyDescriptor).getConverter();
		}
		return super.getConverter();
	}


	@Override
	public boolean supportsReadonly()
	{
		if (propertyDescriptor instanceof IPropertyController)
		{
			return ((IPropertyController<P, E>)propertyDescriptor).supportsReadonly();
		}
		return super.supportsReadonly();
	}

	@Override
	public boolean isReadOnly()
	{
		if (propertyDescriptor instanceof IPropertyController)
		{
			return ((IPropertyController<P, E>)propertyDescriptor).isReadOnly();
		}
		return super.isReadOnly();
	}

	@Override
	public void setReadonly(boolean readonly)
	{
		if (propertyDescriptor instanceof IPropertyController)
		{
			((IPropertyController<P, E>)propertyDescriptor).setReadonly(readonly);
		}
		else
		{
			super.setReadonly(readonly);
		}
	}

	/**
	 * If readonly is not the same, then not compatible.
	 */
	@Override
	public boolean isCompatibleWith(IPropertyDescriptor anotherProperty)
	{
		if (anotherProperty instanceof IPropertyController)
		{
			if (((IPropertyController)anotherProperty).isReadOnly() != isReadOnly())
			{
				return false;
			}
		}
		return propertyDescriptor.isCompatibleWith(anotherProperty);
	}

	public IPropertyDescriptor getDelegate()
	{
		return propertyDescriptor;
	}

	@Override
	public Object getAdapter(Class adapter)
	{
		if (propertyDescriptor instanceof IAdaptable)
		{
			Object adapted = ((IAdaptable)propertyDescriptor).getAdapter(adapter);
			if (adapted != null)
			{
				return adapted;
			}
		}
		return super.getAdapter(adapter);
	}


	public boolean isMergeableWith(IMergeablePropertyDescriptor pd)
	{
		if (propertyDescriptor instanceof IMergeablePropertyDescriptor)
		{
			return ((IMergeablePropertyDescriptor)propertyDescriptor).isMergeableWith(pd);
		}
		return false;
	}

	public IMergedPropertyDescriptor createMergedPropertyDescriptor(IMergeablePropertyDescriptor pd)
	{
		if (propertyDescriptor instanceof IMergeablePropertyDescriptor)
		{
			return ((IMergeablePropertyDescriptor)propertyDescriptor).createMergedPropertyDescriptor(pd);
		}
		return null;
	}

	@Override
	public String getTooltipText()
	{
		return propertyDescriptor instanceof IProvidesTooltip ? ((IProvidesTooltip)propertyDescriptor).getTooltipText() : null;
	}

	@Override
	public String toString()
	{
		return "DelegatePD:" + propertyDescriptor.toString();
	}

}
