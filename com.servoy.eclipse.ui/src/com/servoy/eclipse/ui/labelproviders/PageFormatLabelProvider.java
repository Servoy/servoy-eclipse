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

import java.awt.print.PageFormat;

import org.eclipse.jface.viewers.LabelProvider;

import com.servoy.eclipse.ui.Messages;

public class PageFormatLabelProvider extends LabelProvider
{
	public static final PageFormatLabelProvider INSTANCE = new PageFormatLabelProvider();

	@Override
	public String getText(Object element)
	{
		if (element == null)
		{
			return Messages.LabelDefault;
		}

		PageFormat format = (PageFormat)element;
		StringBuffer sb = new StringBuffer();
		switch (format.getOrientation())
		{
			case PageFormat.LANDSCAPE :
				sb.append("landscape");
				break;
			case PageFormat.PORTRAIT :
				sb.append("portrait");
				break;
			case PageFormat.REVERSE_LANDSCAPE :
				sb.append("rev. landscape");
				break;
			default :
				sb.append("unknown");
		}
		sb.append(" ");
		sb.append((int)format.getWidth());
		sb.append(",");
		sb.append((int)format.getHeight());
		return sb.toString();
	}
}
