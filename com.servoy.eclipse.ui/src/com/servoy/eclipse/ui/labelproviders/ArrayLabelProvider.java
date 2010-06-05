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
package com.servoy.eclipse.ui.labelproviders;

import org.eclipse.jface.viewers.LabelProvider;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.IPropertyConverter;

/**
 * Label provider that uses a IPropertyConverter for text conversion
 * 
 * @author rob
 * 
 */
public class ArrayLabelProvider extends LabelProvider
{
	private final IPropertyConverter<String, Object[]> array2stringConverter;

	public ArrayLabelProvider(IPropertyConverter<String, Object[]> array2stringConverter)
	{
		this.array2stringConverter = array2stringConverter;
	}

	@Override
	public String getText(Object element)
	{
		if (element instanceof String)
		{
			return (String)element;
		}
		if (element instanceof Object[] && ((Object[])element).length > 0)
		{
			return array2stringConverter.convertValue(null, (Object[])element);
		}
		return Messages.LabelNone;
	}
}