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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;

import com.servoy.eclipse.ui.Messages;

public class FontLabelProvider extends LabelProvider
{
	public static FontLabelProvider INSTANCE = new FontLabelProvider();

	@Override
	public String getText(Object element)
	{
		if (element instanceof FontData[])
		{
			FontData[] fontDatas = (FontData[])element;
			if (fontDatas.length == 0)
			{
				return Messages.LabelDefault;
			}

			StringBuffer sb = new StringBuffer();
			for (FontData fd : fontDatas)
			{
				if (sb.length() > 0) sb.append(' ');
				sb.append(fd.getName());
				sb.append(',');
				String style;
				switch (fd.getStyle())
				{
					case SWT.NORMAL :
						style = Messages.FontPlain;
						break;
					case SWT.BOLD :
						style = Messages.FontBold;
						break;
					case SWT.ITALIC :
						style = Messages.FontItalic;
						break;
					case SWT.BOLD + SWT.ITALIC :
						style = Messages.FontBoldItalic;
						break;

					default :
						style = String.valueOf(fd.getStyle());
						break;
				}

				sb.append(style);
				sb.append(',');
				sb.append(fd.getHeight());
			}
			return sb.toString();
		}
		return null;
	}
}
