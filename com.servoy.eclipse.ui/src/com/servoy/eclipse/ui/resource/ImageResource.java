/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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
package com.servoy.eclipse.ui.resource;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

import com.servoy.j2db.util.Pair;


/**
 * Manages and caches images.
 * 
 * @author rgansevles
 */

public class ImageResource
{
	public static ImageResource INSTANCE = new ImageResource();

	private final Map<Pair<ImageDescriptor, RGB>, Image> imageCacheDescriptor = new HashMap<Pair<ImageDescriptor, RGB>, Image>();

	public Image getImage(ImageDescriptor imageDescriptor)
	{
		return getImageWithRoundBackground(imageDescriptor, null);
	}

	public Image getImageWithRoundBackground(ImageDescriptor imageDescriptor, RGB rgb)
	{
		if (imageDescriptor == null) return null;

		Pair<ImageDescriptor, RGB> key = new Pair<ImageDescriptor, RGB>(imageDescriptor, rgb);
		Image image = imageCacheDescriptor.get(key);
		if (image == null)
		{
			if (rgb == null)
			{
				image = imageDescriptor.createImage();
			}
			else
			{
				// draw round background
				Image plainImage = getImage(imageDescriptor);
				image = new Image(plainImage.getDevice(), plainImage.getBounds().width + 2, plainImage.getBounds().height + 2);
				GC gc = new GC(image);
				gc.setBackground(ColorResource.INSTANCE.getColor(rgb));
				gc.fillOval(0, 0, plainImage.getBounds().width + 2, plainImage.getBounds().height + 2);
				gc.drawImage(plainImage, 1, 1);
				gc.dispose();
			}

			imageCacheDescriptor.put(key, image);
		}
		return image;
	}
}
