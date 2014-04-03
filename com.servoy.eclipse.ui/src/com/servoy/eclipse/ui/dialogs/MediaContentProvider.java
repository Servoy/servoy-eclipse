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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.servoy.eclipse.ui.labelproviders.MediaLabelProvider;
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IPersist;

/**
 * Content provider class for media.
 * 
 * @author rgansevles
 * 
 */
public class MediaContentProvider implements ITreeContentProvider
{
	private final FlattenedSolution flattenedSolution;
	private final IPersist context;
	private final IFilter filter;

	public MediaContentProvider(FlattenedSolution flattenedSolution, IPersist context, IFilter filter)
	{
		this.flattenedSolution = flattenedSolution;
		this.context = context;
		this.filter = filter;
	}

	public void dispose()
	{
		// TODO Auto-generated method stub

	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
		// TODO Auto-generated method stub

	}

	public Object[] getElements(Object inputElement)
	{
		if (inputElement instanceof MediaListOptions)
		{
			MediaNode rootMediaNode = new MediaNode(null, null, MediaNode.TYPE.FOLDER, flattenedSolution, context);
			MediaNode[] children = rootMediaNode.getChildren(EnumSet.of(MediaNode.TYPE.IMAGE, MediaNode.TYPE.FOLDER));

			children = filterIfNeeded(children);

			MediaListOptions options = (MediaListOptions)inputElement;
			if (options.includeNone)
			{
				MediaNode[] newChildren = new MediaNode[children.length + 1];
				newChildren[0] = MediaLabelProvider.MEDIA_NODE_NONE;
				System.arraycopy(children, 0, newChildren, 1, children.length);
				children = newChildren;
			}

			return children;
		}

		return new Object[0];
	}

	protected MediaNode[] filterIfNeeded(MediaNode[] children)
	{
		if (filter != null && children != null)
		{
			List<MediaNode> filtered = new ArrayList<MediaNode>(children.length);
			for (MediaNode node : children)
			{
				if (filter.select(node)) filtered.add(node);
			}
			return filtered.toArray(new MediaNode[filtered.size()]);
		}
		else return children;
	}

	public Object[] getChildren(Object parentElement)
	{
		return filterIfNeeded(((MediaNode)parentElement).getChildren(EnumSet.of(MediaNode.TYPE.IMAGE, MediaNode.TYPE.FOLDER)));
	}

	public Object getParent(Object element)
	{
		return ((MediaNode)element).getParent();
	}

	public boolean hasChildren(Object element)
	{
		return ((MediaNode)element).hasChildren(EnumSet.of(MediaNode.TYPE.IMAGE, MediaNode.TYPE.FOLDER));
	}

	public static class MediaListOptions
	{
		public final boolean includeNone;

		public MediaListOptions(boolean includeNone)
		{
			this.includeNone = includeNone;
		}
	}
}
