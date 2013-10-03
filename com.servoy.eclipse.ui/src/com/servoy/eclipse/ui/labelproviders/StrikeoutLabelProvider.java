/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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


import java.util.HashMap;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * Label provider with strikeout support
 * @author gboros
 *
 */
public abstract class StrikeoutLabelProvider extends StyledCellLabelProvider implements ILabelProvider
{
	private final HashMap<Image, Image> imageCache = new HashMap<Image, Image>();

	@Override
	public void update(ViewerCell cell)
	{
		Object element = cell.getElement();

		String newText = getText(element);
		if (isStrikeout(element) && newText != null && newText.length() > 0)
		{
			StyleRange[] cellStyleRanges = cell.getStyleRanges();
			if (cellStyleRanges == null || cellStyleRanges.length == 0)
			{
				cellStyleRanges = new StyleRange[] { new StyleRange() };
				cell.setStyleRanges(cellStyleRanges);
			}
			cellStyleRanges[0].strikeout = true;
			cellStyleRanges[0].start = 0;
			cellStyleRanges[0].length = newText.length();
		}
		else cell.setStyleRanges(null);
		cell.setText(newText);
		Image image = getImage(element);
		boolean scaled = false;
		if (image != null)
		{
			int width = image.getBounds().width;
			int height = image.getBounds().height;
			if (height > 16 || width > 16)
			{
				Image scaledImage = null;
				if (imageCache.get(image) == null)
				{
					int largest = width > height ? width : height;
					double zoom = 16d / largest;
					int scaledWidth = (int)(width * zoom) < 16 ? 16 : (int)(width * zoom);
					int scaledHeight = (int)(height * zoom) < 16 ? 16 : (int)(height * zoom);
					scaledImage = new Image(Display.getDefault(), image.getImageData().scaledTo(scaledWidth, scaledHeight));
					imageCache.put(image, scaledImage);
				}
				else
				{
					scaledImage = imageCache.get(image);
				}
				cell.setImage(scaledImage);
				scaled = true;
			}
		}
		if (!scaled) cell.setImage(image);

		super.update(cell);
	}

	public abstract boolean isStrikeout(Object element);

	@Override
	public void dispose()
	{
		clearImageCache(imageCache);
	}

	public void clearImageCache(HashMap<Image, Image> imageCache)
	{
		for (Image image : imageCache.values())
		{
			image.dispose();
		}
	}
}
