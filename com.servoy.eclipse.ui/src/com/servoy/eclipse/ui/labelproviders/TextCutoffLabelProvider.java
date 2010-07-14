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

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;

/**
 * Wrapper label provider for limiting display of too long texts; newlines are also removed.
 * 
 * @author rgansevles
 * 
 */
public class TextCutoffLabelProvider extends DelegateLabelProvider
{
	public static final TextCutoffLabelProvider DEFAULT = new TextCutoffLabelProvider(new LabelProvider(), 50);

	private final int maxLength;

	public TextCutoffLabelProvider(ILabelProvider labelProvider, int maxLength)
	{
		super(labelProvider);
		this.maxLength = maxLength;
	}

	@Override
	public String getText(Object element)
	{
		String text = super.getText(element);
		if (text == null)
		{
			return null;
		}
		String nonl = text.replaceAll("\n", " ");
		if (nonl.length() <= maxLength)
		{
			return nonl;
		}
		return nonl.substring(0, maxLength - 3) + "...";
	}
}
