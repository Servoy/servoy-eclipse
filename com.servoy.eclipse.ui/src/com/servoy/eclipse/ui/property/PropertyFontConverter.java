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
package com.servoy.eclipse.ui.property;

import org.eclipse.swt.graphics.FontData;

/**
 * Converter between swing (awt) fonts and swt FontData.
 * 
 * @author rgansevles
 */

public class PropertyFontConverter implements IPropertyConverter<java.awt.Font, FontData[]>
{
	public static final PropertyFontConverter INSTANCE = new PropertyFontConverter();
	public static final FontData[] NULL_VALUE = new FontData[0];

	private PropertyFontConverter()
	{
	}

	/**
	 * Convert AWT font to SWT FontData[]
	 */
	public FontData[] convertProperty(Object id, java.awt.Font awtfont)
	{
		if (awtfont == null) return NULL_VALUE;
		return new FontData[] { new FontData(awtfont.getFamily(), awtfont.getSize(), awtfont.getStyle()) };
	}

	/**
	 * Convert SWT FontData[] to AWT font
	 */
	public java.awt.Font convertValue(Object id, FontData[] swtfont)
	{
		if (swtfont == null || swtfont.length == 0) return null;
		return new java.awt.Font(swtfont[0].getName(), swtfont[0].getStyle(), swtfont[0].getHeight());
	}
}
