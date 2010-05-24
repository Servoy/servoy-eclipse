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
package com.servoy.eclipse.ui.dialogs;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Tree content provider that caches the children values for a parent element.
 * 
 * @author rob
 *
 */
public abstract class CachingContentProvider extends ArrayContentProvider implements ITreeContentProvider
{
	private final Map<Object, Object[]> cache = new HashMap<Object, Object[]>();

	public final Object[] getChildren(Object parentElement)
	{
		Object[] children = cache.get(parentElement);
		if (children == null)
		{
			children = getChildrenUncached(parentElement);
			cache.put(parentElement, children);
		}
		return children;
	}

	public abstract Object[] getChildrenUncached(Object parentElement);

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
		cache.clear();
		super.inputChanged(viewer, oldInput, newInput);
	}

	@Override
	public void dispose()
	{
		cache.clear();
		super.dispose();
	}
}
