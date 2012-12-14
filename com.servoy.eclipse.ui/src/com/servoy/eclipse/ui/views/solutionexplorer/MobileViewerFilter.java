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

package com.servoy.eclipse.ui.views.solutionexplorer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;

/**
 * This class filters what out what is not allowed in SolutionExplorer for a mobile project.
 * 
 * @author acostache
 *
 */
public class MobileViewerFilter extends ViewerFilter
{
	private List<UserNodeType> allowedParentNodes = null;

	public MobileViewerFilter()
	{
		// adding hardcode node types, because they don't have a corresponding class type that we can check is allowed in mobile
		allowedParentNodes = new ArrayList<UserNodeType>();
		allowedParentNodes.add(UserNodeType.RESOURCES);
		allowedParentNodes.add(UserNodeType.ALL_SOLUTIONS);
		allowedParentNodes.add(UserNodeType.SOLUTION);
		allowedParentNodes.add(UserNodeType.PLUGINS);
	}

	private SimpleUserNode getFirstParent(SimpleUserNode node)
	{
		if (node == null || node.parent == null) return null;
		if (node.parent != null && node.parent.getRealType() == UserNodeType.ARRAY && "root".equals(node.parent.getName())) return node; //$NON-NLS-1$
		else return getFirstParent(node.parent);
	}

	@Override
	public boolean select(Viewer viewer, Object parentNode, Object node)
	{
		if (node instanceof PlatformSimpleUserNode)
		{
			PlatformSimpleUserNode sun = (PlatformSimpleUserNode)node;
			if (sun.isVisibleInMobile()) return true;
			else
			{
				if (sun.getRealType() == UserNodeType.MEDIA)
				{
					return false;
				}

				// special case for plugins: this is a plugin that is not allowed in mobile
				if (sun.parent != null && sun.parent.getRealType() == UserNodeType.PLUGINS)
				{
					return false;
				}

				if (allowedParentNodes.contains(sun.getRealType()))
				{
					return true;
				}
				else
				{
					SimpleUserNode p = getFirstParent(sun);
					if (p != null && allowedParentNodes.contains(p.getRealType())) return true;
				}
			}
		}
		else if (node instanceof SimpleUserNode)
		{
			// filtering the list view
			return ((SimpleUserNode)node).isVisibleInMobile();
		}
		return false;
	}

}
