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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

import com.servoy.eclipse.ui.Messages;

/**
 * PropertyController wrapper to enforce readonlyness.
 * 
 * @author rgansevles
 */

public class ReadonlyPropertyController<P, E> extends DelegatePropertyController<P, E>
{
	public ReadonlyPropertyController(IPropertyDescriptor propertyDescriptor)
	{
		super(propertyDescriptor, propertyDescriptor.getId());
	}

	@Override
	public boolean supportsReadonly()
	{
		return true; // does not need to be wrapped to force read-only
	}

	@Override
	public boolean isReadOnly()
	{
		return true;
	}

	@Override
	final public CellEditor createPropertyEditor(Composite parent)
	{
		return null; // readonly
	}

	@Override
	public String getDescription()
	{
		return Messages.LabelReadonly;
	}
}
