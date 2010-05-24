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
package com.servoy.eclipse.ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * @author jcompagner
 * 
 */
class SharedTextColors implements ISharedTextColors
{

	/** The display table. */
	private Map fDisplayTable;

	/** Creates an returns a shared color manager. */
	public SharedTextColors()
	{
		super();
	}

	/*
	 * @see ISharedTextColors#getColor(RGB)
	 */
	public Color getColor(RGB rgb)
	{
		if (rgb == null) return null;

		if (fDisplayTable == null) fDisplayTable = new HashMap(2);

		final Display display = Display.getCurrent();

		Map colorTable = (Map)fDisplayTable.get(display);
		if (colorTable == null)
		{
			colorTable = new HashMap(10);
			fDisplayTable.put(display, colorTable);
			display.disposeExec(new Runnable()
			{
				public void run()
				{
					dispose(display);
				}
			});
		}

		Color color = (Color)colorTable.get(rgb);
		if (color == null)
		{
			color = new Color(display, rgb);
			colorTable.put(rgb, color);
		}

		return color;
	}

	/*
	 * @see ISharedTextColors#dispose()
	 */
	public void dispose()
	{
		if (fDisplayTable == null) return;

		Iterator iter = fDisplayTable.values().iterator();
		while (iter.hasNext())
			dispose((Map)iter.next());
		fDisplayTable = null;
	}

	/**
	 * Disposes the colors for the given display.
	 * 
	 * @param display the display for which to dispose the colors
	 * @since 3.3
	 */
	private void dispose(Display display)
	{
		if (fDisplayTable != null) dispose((Map)fDisplayTable.remove(display));
	}

	/**
	 * Disposes the given color table.
	 * 
	 * @param colorTable the color table that maps <code>RGB</code> to <code>Color</code>
	 * @since 3.3
	 */
	private void dispose(Map colorTable)
	{
		if (colorTable == null) return;

		Iterator iter = colorTable.values().iterator();
		while (iter.hasNext())
			((Color)iter.next()).dispose();

		colorTable.clear();
	}

}
