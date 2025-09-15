/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds the location of Web Custom Type objects in absolute layout forms because they do not have a location property but still need to be compared via their position when being sorted for reordering.
 * @author gganea
 *
 */
public class LocationCache
{
	private static LocationCache INSTANCE;

	private final Map<String, HashMap<String, Point>> locations = new HashMap<String, HashMap<String, Point>>();

	private LocationCache()
	{
		LocationCache.INSTANCE = this;
	}

	/**
	 * @return the iNSTANCE
	 */
	public static LocationCache getINSTANCE()
	{
		if (INSTANCE == null) INSTANCE = new LocationCache();
		return INSTANCE;
	}

	public Point getLocation(String parent, String id)
	{
		return locations.get(parent).get(id);
	}

	public void putLocation(String parent, String id, Point location)
	{
		if (locations.get(parent) == null)
		{
			locations.put(parent, new HashMap<String, Point>());
		}
		this.locations.get(parent).put(id, location);
	}

	public void clearParent(String parent)
	{
		locations.remove(parent);
	}


}
