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
package com.servoy.eclipse.team.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.internal.core.mapping.CompoundResourceTraversal;
import org.eclipse.team.ui.synchronize.ModelOperation;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.team.Activator;
import com.servoy.eclipse.team.ServoyTeamProvider;
import com.servoy.eclipse.team.subscriber.SolutionSubscriber;

public abstract class SolutionOperation extends ModelOperation
{

	/**
	 * Create a scope manager for the solution.
	 * 
	 * @param name the name of the manager
	 * @param inputMappings the input mappings
	 * @return a scope manager
	 */
	public static SubscriberScopeManager createScopeManager(String name, ResourceMapping[] inputMappings)
	{
		return new SubscriberScopeManager(name, inputMappings, SolutionSubscriber.getInstance(), true);
	}

	protected SolutionOperation(IWorkbenchPart part, ISynchronizationScopeManager manager)
	{
		super(part, manager);
		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.ui.synchronize.ModelOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void execute(IProgressMonitor monitor) throws InvocationTargetException
	{
		try
		{
			Map providerToTraversals = getProviderToTraversalsMap();
			monitor.beginTask(getTaskName(), providerToTraversals.size() * 100);
			monitor.setTaskName(getTaskName());
			for (Iterator iter = providerToTraversals.keySet().iterator(); iter.hasNext();)
			{
				ServoyTeamProvider provider = (ServoyTeamProvider)iter.next();
				ResourceTraversal[] traversals = getTraversals(providerToTraversals, provider);
				execute(provider, traversals, new SubProgressMonitor(monitor, 100));
			}
		}
		catch (Exception e)
		{
			throw new InvocationTargetException(e);
		}
		finally
		{
			monitor.done();
		}
	}

	/**
	 * Return a map of ServoyTeamProvider to ResourceTraversals.
	 * 
	 * @return a map of ServoyTeamProvider to ResourceTraversals
	 */
	private Map getProviderToTraversalsMap()
	{
		HashMap result = new HashMap();
		ISynchronizationScope scope = getScope();
		ResourceMapping[] mappings = scope.getMappings();
		for (ResourceMapping mapping : mappings)
		{
			ResourceTraversal[] traversals = scope.getTraversals(mapping);
			for (ResourceTraversal traversal : traversals)
			{
				IResource[] resources = traversal.getResources();
				for (IResource resource : resources)
				{
					recordResourceAndDepth(result, resource, traversal.getDepth());
				}
			}
		}

		return result;
	}

	private void recordResourceAndDepth(HashMap providerToTraversals, IResource resource, int depth)
	{
		ServoyTeamProvider provider = getProviderFor(resource.getProject());
		if (provider != null)
		{
			CompoundResourceTraversal traversal = (CompoundResourceTraversal)providerToTraversals.get(provider);
			if (traversal == null)
			{
				traversal = new CompoundResourceTraversal();
				providerToTraversals.put(provider, traversal);
			}
			traversal.addResource(resource, depth);
		}
	}

	/**
	 * Return servoy team provider associated with the given project or <code>null</code> if the project is not mapped to servoy provider.
	 * 
	 * @param project the project
	 * @return servoy team provider associated with the given project
	 */
	protected ServoyTeamProvider getProviderFor(IProject project)
	{
		return (ServoyTeamProvider)RepositoryProvider.getProvider(project, Activator.getTypeId());
	}

	/**
	 * Return the traversals that were accumulated for the given provider by the {@link #getProviderToTraversalsMap()} method.
	 * 
	 * @param providerToTraversals the provider to traversals map
	 * @param provider the provider
	 * @return the traversals for the given provider
	 */
	private ResourceTraversal[] getTraversals(Map providerToTraversals, ServoyTeamProvider provider)
	{
		CompoundResourceTraversal traversal = (CompoundResourceTraversal)providerToTraversals.get(provider);
		return traversal.asTraversals();
	}

	/**
	 * Execute the operation for the given provider and traversals.
	 * 
	 * @param provider the provider
	 * @param traversals the traversals to be operated on
	 * @param monitor a progress monitor
	 * @throws CoreException
	 */
	protected abstract void execute(ServoyTeamProvider provider, ResourceTraversal[] traversals, IProgressMonitor monitor) throws Exception;

	/**
	 * Return the task name for this operation.
	 * 
	 * @return the task name for this operation
	 */
	protected abstract String getTaskName();

	@Override
	protected ISchedulingRule getSchedulingRule()
	{
		// STP is using the eclipse repository singleton to serialize/deserialize
		// solutions, and during this the solutions ids are loaded into the singleton
		// so synch on ws root
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	@Override
	protected String getJobName()
	{
		return getTaskName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.ui.TeamOperation#canRunAsJob()
	 */
	@Override
	protected boolean canRunAsJob()
	{
		return true;
	}
}
