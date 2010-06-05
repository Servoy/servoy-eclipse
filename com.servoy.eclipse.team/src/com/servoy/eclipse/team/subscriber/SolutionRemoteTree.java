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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.ThreeWayRemoteTree;
import org.eclipse.team.core.variants.ThreeWaySubscriber;

import com.servoy.eclipse.team.Activator;
import com.servoy.eclipse.team.ServoyTeamProvider;

public class SolutionRemoteTree extends ThreeWayRemoteTree
{

	public SolutionRemoteTree(ThreeWaySubscriber subscriber) {
		super(subscriber);
	}

	@Override
	protected IResourceVariant[] fetchMembers(IResourceVariant variant,
			IProgressMonitor progress) throws TeamException
	{
		return ((SolutionResourceVariant)variant).members();
	}

	@Override
	protected IResourceVariant fetchVariant(IResource resource, int depth,
			IProgressMonitor monitor) throws TeamException
	{
		RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject(),
				Activator.getTypeId());
		if (provider != null)
		{
			return ((ServoyTeamProvider)provider).getResourceVariant(resource);
		}

		return null;
	}

}
