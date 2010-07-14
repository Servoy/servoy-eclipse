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

import com.servoy.eclipse.ui.property.IComboboxPropertyModel;

/**
 * Standard ComboBoxLabelProvider with a custom unresolved string.
 * 
 * @author rgansevles
 * 
 */
public class ComboBoxLabelProvider extends LabelProvider
{
	protected final String unresolved;
	private final IComboboxPropertyModel< ? > model;

	public ComboBoxLabelProvider(IComboboxPropertyModel< ? > model, String unresolved)
	{
		this.model = model;
		this.unresolved = unresolved;
	}

	@Override
	public String getText(Object element)
	{
		String[] displayValues = model.getDisplayValues();
		if (element instanceof Integer)
		{
			int index = ((Integer)element).intValue();
			if (index >= 0 && index < displayValues.length)
			{
				return displayValues[index];
			}
		}

		return unresolved;
	}
}
