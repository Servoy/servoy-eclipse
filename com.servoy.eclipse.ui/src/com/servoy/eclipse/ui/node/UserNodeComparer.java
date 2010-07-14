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
package com.servoy.eclipse.ui.node;

/**
 * Class that can test two SimpleUserNode instances for equality using the content. Can also provide a hashCode implementation that will make two SimpleUserNode
 * instances with the same content return the same hashCode.
 * 
 * The class is created in order to avoid copy/paste that could lead to bugs...
 * 
 * @author acostescu
 */
public class UserNodeComparer
{

	private UserNodeComparer()
	{
		// not meant to be instantiated
	}

	/**
	 * Two SimpleUserNode instances with the same content will return the same hash code.
	 * 
	 * @param obj a SimpleUserNode instance. Must not be null.
	 * @return the hash code for the given node.
	 */
	public static int hashCode(Object obj)
	{
		if (obj instanceof SimpleUserNode)
		{
			SimpleUserNode a = (SimpleUserNode)obj;
			final int prime = 31;
			int result = 1;
			result = prime * result + ((a.getRealObject() == null) ? 0 : a.getRealObject().hashCode());
			result = prime * result + ((a.getName() == null) ? 0 : a.getName().hashCode());
			result = prime * result + ((a.getType() == null) ? 0 : a.getType().hashCode());
			return result;
		}
		else
		{
			return obj.hashCode();
		}
	}

	/**
	 * Decides if obj equals b. If the two objects are SimpleUserNode instances, they will be compared by content.
	 * 
	 * @param obj a simple user node instance. Must not be null.
	 * @param b the object to which obj is compared.
	 * @return true if the two objects are equal, false otherwise.
	 */
	public static boolean equals(Object obj, Object b)
	{
		if (obj instanceof SimpleUserNode)
		{
			SimpleUserNode a = (SimpleUserNode)obj;
			if (a == b) return true;
			if (b == null) return false;
			if (obj.getClass() != b.getClass()) return false;
			final SimpleUserNode other = (SimpleUserNode)b;
			if (a.getRealObject() == null)
			{
				if (other.getRealObject() != null) return false;
			}
			else if (!a.getRealObject().equals(other.getRealObject())) return false;
			if (a.getName() == null)
			{
				if (other.getName() != null) return false;
			}
			else if (!a.getName().equals(other.getName())) return false;
			if (a.getType() == null)
			{
				if (other.getType() != null) return false;
			}
			else if (!a.getType().equals(other.getType())) return false;
			return true;
		}
		else
		{
			return obj.equals(b);
		}
	}

}