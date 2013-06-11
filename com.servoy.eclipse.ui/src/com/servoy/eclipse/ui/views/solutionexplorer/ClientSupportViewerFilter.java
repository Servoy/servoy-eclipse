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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.documentation.ClientSupport;

/**
 * This class filters out what is not allowed in SolutionExplorer for a mobile project.
 * 
 * @author acostache
 *
 */
public class ClientSupportViewerFilter extends ViewerFilter
{
	private ClientSupport clientType;

	public ClientSupportViewerFilter()
	{
	}

	/**
	 * @param activeSolutionClientType
	 */
	public void setClientType(ClientSupport clientType)
	{
		this.clientType = clientType;
	}

	public static boolean isNodeAllowedInClient(ClientSupport clientType, Object object)
	{
		if (clientType == null) return true;

		if (object instanceof SimpleUserNode)
		{
			SimpleUserNode node = (SimpleUserNode)object;
			ClientSupport csp = node.getClientSupport();
			while (csp == null)
			{
				if (node.parent == null || node.parent.getRealType() == UserNodeType.ARRAY && "root".equals(node.parent.getName()))
				{
					// at the root, show by default if we have no ClientSupport
					return true;
				}
				node = node.parent;
				csp = node.getClientSupport();
			}

			return csp.hasSupport(clientType);
		}
		return false;
	}

	@Override
	public boolean select(Viewer viewer, Object parentNode, Object object)
	{
		return isNodeAllowedInClient(clientType, object);
	}
}
