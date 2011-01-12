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
package com.servoy.eclipse.ui.resource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Display;

/**
 * Manages and caches color for label providers.
 * 
 * @author jcompagner, rgansevles
 */

public class ColorResource
{
	public static ColorResource INSTANCE = new ColorResource();

	/** The display table for colors. */
	private Map<Display, Map<RGB, Color>> fDisplayColorsTable;

	/** The display table for colored images. */
	private Map<Display, Map<ImageKey, Image>> fDisplayImagesTable;

	public Color getColor(RGB rgb)
	{
		if (rgb == null) return null;

		if (fDisplayColorsTable == null) fDisplayColorsTable = new HashMap<Display, Map<RGB, Color>>(2);

		final Display display = Display.getCurrent();

		Map<RGB, Color> colorTable = fDisplayColorsTable.get(display);
		if (colorTable == null)
		{
			colorTable = new HashMap<RGB, Color>(10);
			fDisplayColorsTable.put(display, colorTable);
			display.disposeExec(new Runnable()
			{
				public void run()
				{
					if (fDisplayColorsTable != null) disposeResources(fDisplayColorsTable.remove(display));
				}
			});
		}

		Color color = colorTable.get(rgb);
		if (color != null && color.isDisposed())
		{
			colorTable.remove(rgb);
			color = null;
		}
		if (color == null)
		{
			color = new Color(display, rgb);
			colorTable.put(rgb, color);
		}

		return color;
	}

	public void disposeColors()
	{
		if (fDisplayColorsTable != null)
		{
			Iterator<Map<RGB, Color>> iter = fDisplayColorsTable.values().iterator();
			while (iter.hasNext())
			{
				disposeResources(iter.next());
			}
			fDisplayColorsTable = null;
		}
	}

	public void disposeImages()
	{
		if (fDisplayImagesTable != null)
		{
			Iterator<Map<ImageKey, Image>> iter = fDisplayImagesTable.values().iterator();
			while (iter.hasNext())
			{
				disposeResources(iter.next());
			}
			fDisplayImagesTable = null;
		}
	}

	/**
	 * Disposes the given resources table.
	 * 
	 * @param resourceTable the table that maps to <code>Resource</code>
	 */
	private void disposeResources(Map< ? , ? extends Resource> resourceTable)
	{
		if (resourceTable == null) return;

		Iterator< ? extends Resource> iter = resourceTable.values().iterator();
		while (iter.hasNext())
		{
			iter.next().dispose();
		}

		resourceTable.clear();
	}

	public static RGB ColorAwt2Rgb(java.awt.Color awtColor)
	{
		if (awtColor == null) return null;
		return new RGB(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
	}

	public static java.awt.Color ColoRgb2Awt(RGB rgb)
	{
		if (rgb == null) return null;
		return new java.awt.Color(rgb.red, rgb.green, rgb.blue);
	}

	/**
	 * Get a cached color image.
	 */
	public Image getColorImage(int width, int height, int depth, RGB rgb)
	{
		if (rgb == null)
		{
			return null;
		}

		if (fDisplayImagesTable == null) fDisplayImagesTable = new HashMap<Display, Map<ImageKey, Image>>(2);

		final Display display = Display.getCurrent();

		Map<ImageKey, Image> imagesTable = fDisplayImagesTable.get(display);
		if (imagesTable == null)
		{
			imagesTable = new HashMap<ImageKey, Image>(100);
			fDisplayImagesTable.put(display, imagesTable);
			display.disposeExec(new Runnable()
			{
				public void run()
				{
					if (fDisplayImagesTable != null) disposeResources(fDisplayImagesTable.remove(display));
				}
			});
		}

		ImageKey key = new ImageKey(width, height, depth, rgb);
		Image image = imagesTable.get(key);
		if (image != null && image.isDisposed())
		{
			imagesTable.remove(key);
			image = null;
		}
		if (image == null)
		{
			ImageData imageData = new ImageData(width, height, depth, new PaletteData(new RGB[] { rgb }));
			image = new Image(display, imageData);
			imagesTable.put(key, image);
		}

		return image;
	}

	/**
	 * Key for simple colored images
	 * 
	 * @author rgansevles
	 *
	 */
	public static class ImageKey
	{
		private final int width;
		private final int height;
		private final int depth;
		private final RGB rgb;

		/**
		 * @param width
		 * @param height
		 * @param depth
		 * @param rgb
		 */
		public ImageKey(int width, int height, int depth, RGB rgb)
		{
			this.width = width;
			this.height = height;
			this.depth = depth;
			this.rgb = new RGB(rgb.red, rgb.green, rgb.blue); // RGB is mutable
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + depth;
			result = prime * result + height;
			result = prime * result + ((rgb == null) ? 0 : rgb.hashCode());
			result = prime * result + width;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ImageKey other = (ImageKey)obj;
			if (depth != other.depth) return false;
			if (height != other.height) return false;
			if (rgb == null)
			{
				if (other.rgb != null) return false;
			}
			else if (!rgb.equals(other.rgb)) return false;
			if (width != other.width) return false;
			return true;
		}
	}
}
