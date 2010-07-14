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
import java.util.Map;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

/**
 * Manages and caches fonts for label providers.
 * 
 * @author rgansevles
 */

public class FontResource
{
	private static Map<String, Font> fontMap = new HashMap<String, Font>();

	public static Font getDefaultFont(int style, int relativeSize)
	{
		String key = (String.valueOf(style) + '.') + relativeSize;
		Font font = fontMap.get(key);
		if (font == null || font.isDisposed())
		{
			font = applyStyleToFont(JFaceResources.getDefaultFont(), style, relativeSize);
			fontMap.put(key, font);
		}
		return font;
	}


	public static Font applyStyleToFont(Font font, int style)
	{
		return applyStyleToFont(font, style, 0);
	}

	public static Font applyStyleToFont(Font font, int style, int relativeSize)
	{

		if (style == SWT.NONE && relativeSize == 0) return font;

		FontData[] data = font.getFontData();
		for (FontData element : data)
		{
			element.setStyle(style);
			element.setHeight(element.getHeight() + relativeSize);
		}

		return new Font(Display.getDefault(), data);
	}

	public static Point getTextExtent(Drawable drawable, Font font, String text)
	{
		GC gc = null;
		try
		{
			gc = new GC(drawable);
			gc.setFont(font);
			return gc.textExtent(text);
		}
		finally
		{
			if (gc != null) gc.dispose();
		}
	}
}
