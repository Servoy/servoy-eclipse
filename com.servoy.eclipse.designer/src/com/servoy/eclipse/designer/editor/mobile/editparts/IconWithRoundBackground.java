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

package com.servoy.eclipse.designer.editor.mobile.editparts;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Draw an icon with a rounded color background.
 * 
 * @author rgansevles
 *
 */
public class IconWithRoundBackground implements Icon
{
	public final static Color DATA_ICON_BG = new Color(148, 148, 148); // TODO: use theme

	private final ImageIcon icon;
	private final Color color;

	public IconWithRoundBackground(ImageIcon icon)
	{
		this(icon, DATA_ICON_BG);
	}

	public IconWithRoundBackground(ImageIcon icon, Color color)
	{
		this.icon = icon;
		this.color = color;
	}

	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		g.setColor(color);
		g.fillOval(x, y, getIconWidth(), getIconHeight());
		icon.paintIcon(c, g, x + 1, y + 1);
	}

	public int getIconWidth()
	{
		return icon.getIconWidth() + 2;
	}

	public int getIconHeight()
	{
		return icon.getIconHeight() + 2;
	}
}