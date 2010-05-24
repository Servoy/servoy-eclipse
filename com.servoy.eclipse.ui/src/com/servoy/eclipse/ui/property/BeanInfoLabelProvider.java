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

import java.beans.BeanInfo;

import org.eclipse.jface.viewers.LabelProvider;

import com.servoy.eclipse.ui.Messages;

/**
 * Label provider for BeanInfo.
 * 
 * @author rob
 * 
 */
public class BeanInfoLabelProvider extends LabelProvider
{
	public static final BeanInfoLabelProvider INSTANCE_NAME = new BeanInfoLabelProvider(true);
	public static final BeanInfoLabelProvider INSTANCE_CLASS = new BeanInfoLabelProvider(false);

	private final boolean nameNotClass;

	public BeanInfoLabelProvider(boolean nameNotClass)
	{
		this.nameNotClass = nameNotClass;
	}

	@Override
	public String getText(Object value)
	{
		if (value == null)
		{
			return Messages.LabelNone;
		}
		if (nameNotClass)
		{
			return ((BeanInfo)value).getBeanDescriptor().getName();
		}
		return ((BeanInfo)value).getBeanDescriptor().getBeanClass().getName();
	}
}
