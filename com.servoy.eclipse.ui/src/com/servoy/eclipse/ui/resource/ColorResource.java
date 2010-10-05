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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Manages and caches color for label providers.
 * 
 * @author jcompagner, rgansevles
 */

public class ColorResource
{
	public static ColorResource INSTANCE = new ColorResource();

	/** The display table. */
	private Map<Display, Map<RGB, Color>> fDisplayTable;

	public Color getColor(RGB rgb)
	{
		if (rgb == null) return null;

		if (fDisplayTable == null) fDisplayTable = new HashMap<Display, Map<RGB, Color>>(2);

		final Display display = Display.getCurrent();

		Map<RGB, Color> colorTable = fDisplayTable.get(display);
		if (colorTable == null)
		{
			colorTable = new HashMap<RGB, Color>(10);
			fDisplayTable.put(display, colorTable);
			display.disposeExec(new Runnable()
			{
				public void run()
				{
					dispose(display);
				}
			});
		}

		Color color = colorTable.get(rgb);
		if (color == null)
		{
			color = new Color(display, rgb);
			colorTable.put(rgb, color);
		}

		return color;
	}

	public void dispose()
	{
		if (fDisplayTable == null) return;

		Iterator<Map<RGB, Color>> iter = fDisplayTable.values().iterator();
		while (iter.hasNext())
		{
			dispose(iter.next());
		}
		fDisplayTable = null;
	}

	/**
	 * Disposes the colors for the given display.
	 * 
	 * @param display the display for which to dispose the colors
	 */
	private void dispose(Display display)
	{
		if (fDisplayTable != null) dispose(fDisplayTable.remove(display));
	}

	/**
	 * Disposes the given color table.
	 * 
	 * @param colorTable the color table that maps <code>RGB</code> to <code>Color</code>
	 */
	private void dispose(Map<RGB, Color> colorTable)
	{
		if (colorTable == null) return;

		Iterator<Color> iter = colorTable.values().iterator();
		while (iter.hasNext())
		{
			iter.next().dispose();
		}

		colorTable.clear();
	}

}
