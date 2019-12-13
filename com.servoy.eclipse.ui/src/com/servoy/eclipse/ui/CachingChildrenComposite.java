/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public final class CachingChildrenComposite extends Composite
{
	private Control[] children = null;
	private boolean startCaching = false;

	/**
	 * @param parent
	 * @param style
	 */
	public CachingChildrenComposite(Composite parent, int style)
	{
		super(parent, style);
	}

	public void cacheChildren(boolean cache)
	{
		startCaching = cache;
	}

	@Override
	public Control[] getChildren()
	{
		if (!startCaching) return super.getChildren();
		if (children == null)
		{
			children = super.getChildren();
		}
		return children;
	}

	@Override
	public void dispose()
	{
		children = null;
		super.dispose();
	}
}