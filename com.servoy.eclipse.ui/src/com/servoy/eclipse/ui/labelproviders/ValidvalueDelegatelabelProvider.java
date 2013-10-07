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

import java.util.List;

import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Font;

/**
 * Delegate label provider that marks invalid values with a special font
 * 
 * @author rgansevles
 * 
 */

public class ValidvalueDelegatelabelProvider extends DelegateLabelProvider implements IFontProvider
{
	private final List< ? > validValues;
	private final Font invalidFont;
	private final Font validFont;

	public ValidvalueDelegatelabelProvider(ILabelProvider labelProvider, List< ? > validValues, Font validFont, Font invalidFont)
	{
		super(labelProvider);
		this.validValues = validValues;
		this.validFont = validFont;
		this.invalidFont = invalidFont;
	}

	public Font getFont(Object element)
	{
		if (validValues != null && validValues.contains(element))
		{
			// valid value
			if (validFont == null && getLabelProvider() instanceof IFontProvider)
			{
				return ((IFontProvider)getLabelProvider()).getFont(element);
			}
			return validFont;
		}

		// invalid value
		if (invalidFont == null && getLabelProvider() instanceof IFontProvider)
		{
			return ((IFontProvider)getLabelProvider()).getFont(element);
		}
		return invalidFont;
	}

	@Override
	public StrikeoutLabelProvider newInstance()
	{
		return new ValidvalueDelegatelabelProvider((ILabelProvider)getLabelProvider(), validValues, validFont, invalidFont);
	}
}