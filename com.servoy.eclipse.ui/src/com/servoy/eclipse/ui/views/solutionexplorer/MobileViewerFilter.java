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
 * This class filters out what is not allowed in SolutionExplorer for a mobile project.
 * 
 * @author acostache
 *
 */
public class MobileViewerFilter extends ViewerFilter
{
	private static final List<UserNodeType> allowedParentNodes = new ArrayList<UserNodeType>();

	static
	{
		// adding hardcode node types, because they don't have a corresponding class type that we can check is allowed in mobile
		allowedParentNodes.add(UserNodeType.RESOURCES);
		allowedParentNodes.add(UserNodeType.ALL_SOLUTIONS);
		allowedParentNodes.add(UserNodeType.SOLUTION);
		allowedParentNodes.add(UserNodeType.PLUGINS);
	}

	public MobileViewerFilter()
	{

	}

	private static SimpleUserNode getFirstParent(SimpleUserNode node)
	{
		if (node == null || node.parent == null) return null;
		if (node.parent != null && node.parent.getRealType() == UserNodeType.ARRAY && "root".equals(node.parent.getName())) return node; //$NON-NLS-1$
		else return getFirstParent(node.parent);
	}

	public static boolean isNodeAllowedInMobile(Object node)
	{
		if (node instanceof PlatformSimpleUserNode)
		{
			PlatformSimpleUserNode sun = (PlatformSimpleUserNode)node;
			if (sun.isVisibleInMobile()) return true;
			else
			{
				if (sun.getRealType() == UserNodeType.MEDIA || sun.getRealType() == UserNodeType.TEMPLATES || sun.getRealType() == UserNodeType.STYLES ||
					sun.getRealType() == UserNodeType.USER_GROUP_SECURITY)
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
			boolean isAllowedNode = (((SimpleUserNode)node).getRealType() == UserNodeType.TABLE || ((SimpleUserNode)node).getRealType() == UserNodeType.VIEW ||
				((SimpleUserNode)node).getRealType() == UserNodeType.STYLE_ITEM || ((SimpleUserNode)node).getRealType() == UserNodeType.I18N_FILE_ITEM ||
				((SimpleUserNode)node).getRealType() == UserNodeType.TEMPLATE_ITEM || ((SimpleUserNode)node).getRealType() == UserNodeType.FORM_METHOD ||
				((SimpleUserNode)node).getRealType() == UserNodeType.GLOBAL_METHOD_ITEM ||
				((SimpleUserNode)node).getRealType() == UserNodeType.TABLE_COLUMNS_ITEM ||
				((SimpleUserNode)node).getRealType() == UserNodeType.CALCULATIONS_ITEM ||
				((SimpleUserNode)node).getRealType() == UserNodeType.GLOBAL_VARIABLE_ITEM ||
				((SimpleUserNode)node).getRealType() == UserNodeType.FORM_VARIABLE_ITEM ||
				((SimpleUserNode)node).getRealType() == UserNodeType.VALUELIST_ITEM || ((SimpleUserNode)node).getRealType() == UserNodeType.RELATION ||
				((SimpleUserNode)node).getRealType() == UserNodeType.FOUNDSET_ITEM || ((SimpleUserNode)node).getRealType() == UserNodeType.FORM_ELEMENTS_ITEM ||
				((SimpleUserNode)node).getRealType() == UserNodeType.FORM_FOUNDSET || ((SimpleUserNode)node).getRealType() == UserNodeType.SECURITY_ITEM ||
				((SimpleUserNode)node).getRealType() == UserNodeType.ARRAY || ((SimpleUserNode)node).getRealType() == UserNodeType.NUMBER ||
				((SimpleUserNode)node).getRealType() == UserNodeType.SPECIAL_OPERATORS || ((SimpleUserNode)node).getRealType() == UserNodeType.JSLIB);

			return (isAllowedNode || ((SimpleUserNode)node).isVisibleInMobile());
		}
		return false;
	}

	@Override
	public boolean select(Viewer viewer, Object parentNode, Object node)
	{
		return isNodeAllowedInMobile(node);
	}
}
