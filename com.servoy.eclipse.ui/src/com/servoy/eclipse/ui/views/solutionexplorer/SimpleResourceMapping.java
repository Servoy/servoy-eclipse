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
package com.servoy.eclipse.ui.views.solutionexplorer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 * A simple resource mapping for converting IResource to ResourceMapping. It uses the resource as the model object and traverses deeply.
 * 
 * @since 3.1
 */
public class SimpleResourceMapping extends ResourceMapping
{
	private final IResource resource;

	public SimpleResourceMapping(IResource resource)
	{
		this.resource = resource;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.mapping.ResourceMapping#contains(org.eclipse.core.resources.mapping.ResourceMapping)
	 */
	@Override
	public boolean contains(ResourceMapping mapping)
	{
		if (mapping.getModelProviderId().equals(this.getModelProviderId()))
		{
			Object object = mapping.getModelObject();
			if (object instanceof IResource)
			{
				IResource other = (IResource)object;
				return resource.getFullPath().isPrefixOf(other.getFullPath());
			}
			if (object instanceof ShallowContainer)
			{
				ShallowContainer sc = (ShallowContainer)object;
				IResource other = sc.getResource();
				return resource.getFullPath().isPrefixOf(other.getFullPath());
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc) Method declared on ResourceMapping.
	 */
	@Override
	public Object getModelObject()
	{
		return resource;
	}

	@Override
	public String getModelProviderId()
	{
		return ModelProvider.RESOURCE_MODEL_PROVIDER_ID;
	}

	/*
	 * (non-Javadoc) Method declared on ResourceMapping.
	 */
	@Override
	public IProject[] getProjects()
	{
		if (resource.getType() == IResource.ROOT) return ((IWorkspaceRoot)resource).getProjects();
		return new IProject[] { resource.getProject() };
	}

	/*
	 * (non-Javadoc) Method declared on ResourceMapping.
	 */
	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor)
	{
		if (resource.getType() == IResource.ROOT)
		{
			return new ResourceTraversal[] { new ResourceTraversal(((IWorkspaceRoot)resource).getProjects(), IResource.DEPTH_INFINITE, IResource.NONE) };
		}
		return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { resource }, IResource.DEPTH_INFINITE, IResource.NONE) };
	}
}
