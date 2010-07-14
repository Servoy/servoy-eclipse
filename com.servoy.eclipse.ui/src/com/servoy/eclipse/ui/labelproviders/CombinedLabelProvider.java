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

import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

/**
 * Combine 2 label providers. labelProvider1 overrides labelProvider2.
 * 
 * @author rgansevles
 * 
 */
public class CombinedLabelProvider implements ILabelProvider, IFontProvider
{
	private final ILabelProvider labelProvider1;
	private final ILabelProvider labelProvider2;

	public CombinedLabelProvider(ILabelProvider labelProvider1, ILabelProvider labelProvider2)
	{
		this.labelProvider1 = labelProvider1;
		this.labelProvider2 = labelProvider2;
	}

	public void addListener(ILabelProviderListener listener)
	{
		labelProvider1.addListener(listener);
		labelProvider2.addListener(listener);
	}

	public void removeListener(ILabelProviderListener listener)
	{
		labelProvider1.removeListener(listener);
		labelProvider2.removeListener(listener);
	}

	public void dispose()
	{
		labelProvider1.dispose();
		labelProvider2.dispose();
	}

	public Image getImage(Object element)
	{
		Image image = labelProvider1.getImage(element);
		if (image != null)
		{
			return image;
		}
		return labelProvider2.getImage(element);
	}

	public String getText(Object element)
	{
		String text = labelProvider1.getText(element);
		if (text != null && !"".equals(text))
		{
			return text;
		}
		return labelProvider2.getText(element);
	}

	public boolean isLabelProperty(Object element, String property)
	{
		return labelProvider1.isLabelProperty(element, property) || labelProvider2.isLabelProperty(element, property);
	}

	public Font getFont(Object element)
	{
		Font font = null;
		if (labelProvider1 instanceof IFontProvider)
		{
			font = ((IFontProvider)labelProvider1).getFont(element);
		}
		if (font == null && labelProvider2 instanceof IFontProvider)
		{
			font = ((IFontProvider)labelProvider2).getFont(element);
		}
		return font;
	}
}
