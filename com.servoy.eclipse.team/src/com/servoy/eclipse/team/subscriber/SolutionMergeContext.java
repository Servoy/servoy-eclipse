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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;

public class SolutionMergeContext extends SubscriberMergeContext
{

	public SolutionMergeContext(ISynchronizationScopeManager manager)
	{
		super(SolutionSubscriber.getInstance(), manager);
		initialize();
	}

	@Override
	protected void makeInSync(IDiff diff, IProgressMonitor monitor) throws CoreException
	{
		IResource resource = ResourceDiffTree.getResourceFor(diff);
		SolutionSubscriber.getInstance().makeInSync(resource);
	}

	public void markAsMerged(IDiff node, boolean inSyncHint, IProgressMonitor monitor) throws CoreException
	{
		// TODO if inSyncHint is true, we should test to see if the contents match
		IResource resource = ResourceDiffTree.getResourceFor(node);
		SolutionSubscriber.getInstance().markAsMerged(resource, monitor);
	}

	public void reject(IDiff diff, IProgressMonitor monitor) throws CoreException
	{
		markAsMerged(diff, false, monitor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.mapping.provider.MergeContext#getMergeRule(org.eclipse.team.core.diff.IDiff)
	 */
	@Override
	public ISchedulingRule getMergeRule(IDiff node)
	{
		return ResourceDiffTree.getResourceFor(node).getProject();
	}


}
