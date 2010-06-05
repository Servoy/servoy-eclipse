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
package com.servoy.eclipse.designer.property;


import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class PropertySourcePropertyDescriptor extends PropertyDescriptor
{


	public PropertySourcePropertyDescriptor(Object id, String displayName)
	{
		super(id, displayName);
		// TODO Auto-generated constructor stub
	}

//	private final IPropertySource propertySource;

//	public PropertySourcePropertyDescriptor(IPropertySource propertySource)
//	{
//		this.propertySource = propertySource;
//	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		return null;
	}

	@Override
	public String getDescription()
	{
		return getDisplayName() + " properties";
	}


	@Override
	public String[] getFilterFlags()
	{
		return null;
	}

	@Override
	public Object getHelpContextIds()
	{
		return null;
	}

	@Override
	public ILabelProvider getLabelProvider()
	{
		return null;
	}

	@Override
	public boolean isCompatibleWith(IPropertyDescriptor anotherProperty)
	{
		return false;
	}

}
