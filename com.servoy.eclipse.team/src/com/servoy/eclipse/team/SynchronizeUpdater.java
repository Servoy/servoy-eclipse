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
package com.servoy.eclipse.team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.ThreeWaySynchronizer;

public class SynchronizeUpdater
{
	private final ThreeWaySynchronizer synchronizer;
	private final HashMap<Integer, ArrayList<IResource>> flushMap = new HashMap<Integer, ArrayList<IResource>>();
	private final HashMap<IResource, byte[]> baseBytesMap = new HashMap<IResource, byte[]>();

	public SynchronizeUpdater(ThreeWaySynchronizer synchronizer)
	{
		this.synchronizer = synchronizer;
	}

	public void addFlush(IResource resource, int depth)
	{
		Integer iDepth = new Integer(depth);
		ArrayList<IResource> r = flushMap.get(iDepth);
		if (r == null)
		{
			r = new ArrayList<IResource>();
			flushMap.put(iDepth, r);
		}
		r.add(resource);
	}

	public void addBaseBytes(IResource resource, byte[] baseBytes)
	{
		baseBytesMap.put(resource, baseBytes);
	}

	public void update() throws TeamException
	{
		flushDepth(IResource.DEPTH_ZERO);
		flushDepth(IResource.DEPTH_ONE);
		flushDepth(IResource.DEPTH_INFINITE);

		Iterator<Map.Entry<IResource, byte[]>> baseBytesMapIte = baseBytesMap.entrySet().iterator();
		Map.Entry<IResource, byte[]> baseBytesMapEntry;
		while (baseBytesMapIte.hasNext())
		{
			baseBytesMapEntry = baseBytesMapIte.next();
			synchronizer.setBaseBytes(baseBytesMapEntry.getKey(), baseBytesMapEntry.getValue());
		}
	}

	private void flushDepth(int depth) throws TeamException
	{
		ArrayList<IResource> r = flushMap.get(new Integer(depth));
		if (r != null)
		{
			Iterator<IResource> rIte = r.iterator();
			while (rIte.hasNext())
				synchronizer.flush(rIte.next(), depth);
		}
	}
}
