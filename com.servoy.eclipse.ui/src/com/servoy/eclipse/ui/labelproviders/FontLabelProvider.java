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

import java.awt.Font;

import org.eclipse.jface.viewers.LabelProvider;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.PropertyFontConverter;

/**
 * Label provider for Font strings .
 * 
 * @author rgansevles
 */

public class FontLabelProvider extends LabelProvider
{
	public static FontLabelProvider INSTANCE = new FontLabelProvider();

	@Override
	public String getText(Object element)
	{
		if (element instanceof String || element == null)
		{
			Font awtFont = PropertyFontConverter.INSTANCE.convertValue(null, (String)element);
			if (awtFont == null)
			{
				return Messages.LabelDefault;
			}

			StringBuilder sb = new StringBuilder();
			sb.append(awtFont.getName());
			sb.append(',');
			String style;
			switch (awtFont.getStyle())
			{
				case Font.PLAIN :
					style = Messages.FontPlain;
					break;
				case Font.BOLD :
					style = Messages.FontBold;
					break;
				case Font.ITALIC :
					style = Messages.FontItalic;
					break;
				case Font.BOLD + Font.ITALIC :
					style = Messages.FontBoldItalic;
					break;

				default :
					style = String.valueOf(awtFont.getStyle());
					break;
			}

			sb.append(style);
			sb.append(',');
			sb.append(awtFont.getSize());
			return sb.toString();
		}

		return super.getText(element);
	}
}
