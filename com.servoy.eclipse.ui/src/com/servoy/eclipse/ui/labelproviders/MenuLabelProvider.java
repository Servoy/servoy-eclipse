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

import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.IDeprecationProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportDeprecated;
import com.servoy.j2db.persistence.Menu;

/**
 * Label provider for value lists.
 *
 * @author rgansevles
 */

public class MenuLabelProvider extends LabelProvider implements IFontProvider, IPersistLabelProvider, IDeprecationProvider
{
	public static final int MENU_NONE = 0;
	public static final int MENU_UNRESOLVED = -1;

	private final FlattenedSolution flattenedSolution;

	public MenuLabelProvider(FlattenedSolution flattenedSolution)
	{
		this.flattenedSolution = flattenedSolution;
	}

	@Override
	public String getText(Object value)
	{
		if (value == null) return Messages.LabelNone;

		int vlmId = ((Integer)value).intValue();

		if (vlmId == MENU_NONE)
		{
			return Messages.LabelNone;
		}

		Menu menu = (Menu)getPersist(value);
		if (menu != null)
		{
			return menu.getName();
		}

		return Messages.LabelUnresolved;
	}

	public Font getFont(Object value)
	{
		if (value == null) return FontResource.getDefaultFont(SWT.ITALIC, 0);

		int vlmId = ((Integer)value).intValue();

		if (vlmId == MENU_NONE)
		{
			return FontResource.getDefaultFont(SWT.ITALIC, 0);
		}
		return FontResource.getDefaultFont(SWT.NONE, 0);
	}

	public IPersist getPersist(Object value)
	{
		if (value instanceof Integer)
		{
			return flattenedSolution.getMenu(((Integer)value).intValue());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.util.IDeprecationProvider#isDeprecated(java.lang.Object)
	 */
	@Override
	public Boolean isDeprecated(Object element)
	{
		IPersist persist = getPersist(element);
		if (persist instanceof ISupportDeprecated)
		{
			return Boolean.valueOf(((ISupportDeprecated)persist).getDeprecated() != null);
		}

		return null;
	}
}
