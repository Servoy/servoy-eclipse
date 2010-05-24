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

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;

import com.servoy.eclipse.ui.Messages;
import com.servoy.j2db.persistence.ISupportScrollbars;

public class ScrollbarSettingLabelProvider extends LabelProvider
{
	public static final ILabelProvider INSTANCE = new ScrollbarSettingLabelProvider();

	@Override
	public String getText(Object element)
	{
		Integer scrollBits = null;
		if (element instanceof ComplexProperty)
		{
			scrollBits = (Integer)((ComplexProperty)element).getValue();
		}
		else if (element instanceof Integer)
		{
			scrollBits = (Integer)element;
		}
		else if (element != null)
		{
			return Messages.LabelUnresolved;
		}
		int scroll = scrollBits == null ? 0 : scrollBits.intValue();
		if (scroll == 0)
		{
			return Messages.LabelDefault;
		}

		StringBuffer retval = new StringBuffer();
		if ((scroll & ISupportScrollbars.VERTICAL_SCROLLBAR_AS_NEEDED) == ISupportScrollbars.VERTICAL_SCROLLBAR_AS_NEEDED)
		{
			retval.append("V:wn");
		}
		else
		{
			if ((scroll & ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS) == ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS)
			{
				retval.append("V:a");
			}
			else if ((scroll & ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER) == ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER)
			{
				retval.append("V:n");
			}
		}
		retval.append(" ");
		if ((scroll & ISupportScrollbars.HORIZONTAL_SCROLLBAR_AS_NEEDED) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_AS_NEEDED)
		{
			retval.append("H:wn");
		}
		else
		{
			if ((scroll & ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS)
			{
				retval.append("H:a");
			}
			else if ((scroll & ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER)
			{
				retval.append("H:n");
			}
		}
		return retval.toString();
	}

}
