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
package com.servoy.eclipse.team.subscriber;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;


/**
 * Provide a custom sync info that will report files that exist both locally and remotely as in-sync
 */
public class SolutionSyncInfo extends SyncInfo
{

	public SolutionSyncInfo(IResource local, IResourceVariant base, IResourceVariant remote, IResourceVariantComparator comparator)
	{
		super(local, base, remote, comparator);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.subscribers.SyncInfo#calculateKind(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected int calculateKind() throws TeamException
	{
		if (getLocal().getType() != IResource.FILE && getLocal().exists())
		{
			return IN_SYNC;
		}

		IResource local = getLocal();
		IResourceVariant base = getBase();
		IResourceVariant remote = getRemote();
		IResourceVariantComparator comparator = getComparator();
		int description = IN_SYNC;

		boolean localExists = local.exists();

		if (base == null)
		{
			if (remote == null)
			{
				if (!localExists)
				{
					description = IN_SYNC;
				}
				else
				{
					description = OUTGOING | ADDITION;
				}
			}
			else
			{
				if (!localExists)
				{
					description = INCOMING | ADDITION;
				}
				else
				{
					description = CONFLICTING | ADDITION;
					if (comparator.compare(local, remote))
					{
						description |= PSEUDO_CONFLICT;
					}
				}
			}
		}
		else
		{
			if (!localExists)
			{
				if (remote == null)
				{
					description = CONFLICTING | DELETION | PSEUDO_CONFLICT;
				}
				else
				{
					if (comparator.compare(base, remote)) description = OUTGOING | DELETION;
					else description = CONFLICTING | CHANGE;
				}
			}
			else
			{
				if (remote == null)
				{
					if (comparator.compare(local, base)) description = INCOMING | DELETION;
					else description = CONFLICTING | CHANGE;
				}
				else
				{
					boolean ay = comparator.compare(local, base);
					boolean am = comparator.compare(base, remote);
					if (ay && am)
					{
						// in-sync
					}
					else if (ay && !am)
					{
						description = INCOMING | CHANGE;
					}
					else if (!ay && am)
					{
						description = OUTGOING | CHANGE;
					}
					else
					{
						if (!comparator.compare(local, remote)/* && !SolutionSubscriber.hasSameContent(local, remote) */)
						{
							description = CONFLICTING | CHANGE;
						}
					}
				}
			}
		}
		return description;
	}
}
