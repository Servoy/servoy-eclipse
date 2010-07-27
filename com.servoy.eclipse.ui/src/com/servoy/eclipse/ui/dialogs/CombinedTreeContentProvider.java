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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.servoy.eclipse.ui.util.IKeywordChecker;
import com.servoy.eclipse.ui.views.IMaxDepthTreeContentProvider;
import com.servoy.j2db.util.Utils;

/**
 * Combine 2 tree content providers into 1
 * 
 * @author rgansevles
 * 
 */
public class CombinedTreeContentProvider implements ITreeContentProvider, IKeywordChecker, IMaxDepthTreeContentProvider
{
	private final ITreeContentProvider contentProvider1;
	private final ITreeContentProvider contentProvider2;


	public CombinedTreeContentProvider(ITreeContentProvider contentProvider1, ITreeContentProvider contentProvider2)
	{
		this.contentProvider1 = contentProvider1;
		this.contentProvider2 = contentProvider2;
	}

	public Object[] getChildren(Object parentElement)
	{
		return Utils.arrayJoin(contentProvider1.getChildren(parentElement), contentProvider2.getChildren(parentElement));
	}

	public Object getParent(Object element)
	{
		Object parent1 = contentProvider1.getParent(element);
		if (parent1 != null)
		{
			return parent1;
		}
		return contentProvider2.getParent(element);
	}

	public boolean hasChildren(Object element)
	{
		return contentProvider1.hasChildren(element) || contentProvider2.hasChildren(element);
	}

	public Object[] getElements(Object inputElement)
	{
		return Utils.arrayJoin(contentProvider1.getElements(inputElement), contentProvider2.getElements(inputElement));
	}

	public void dispose()
	{
		contentProvider1.dispose();
		contentProvider2.dispose();
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
		contentProvider1.inputChanged(viewer, oldInput, newInput);
		contentProvider2.inputChanged(viewer, oldInput, newInput);
	}

	public boolean isKeyword(Object element)
	{
		if (contentProvider1 instanceof IKeywordChecker && ((IKeywordChecker)contentProvider1).isKeyword(element))
		{
			return true;
		}
		return (contentProvider2 instanceof IKeywordChecker && ((IKeywordChecker)contentProvider2).isKeyword(element));
	}

	public boolean searchLimitReached(Object element, int maxDepth)
	{
		boolean limitReached = false;
		if (contentProvider1 instanceof IMaxDepthTreeContentProvider)
		{
			limitReached = ((IMaxDepthTreeContentProvider)contentProvider1).searchLimitReached(element, maxDepth);
			if (!limitReached && contentProvider2 instanceof IMaxDepthTreeContentProvider)
			{
				limitReached = ((IMaxDepthTreeContentProvider)contentProvider2).searchLimitReached(element, maxDepth);
			}
		}
		return limitReached;
	}
}
