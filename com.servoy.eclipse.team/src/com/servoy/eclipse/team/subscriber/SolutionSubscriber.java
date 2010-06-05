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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.ISynchronizerChangeListener;
import org.eclipse.team.core.variants.ThreeWayRemoteTree;
import org.eclipse.team.core.variants.ThreeWaySubscriber;
import org.eclipse.team.core.variants.ThreeWaySynchronizer;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.team.Activator;
import com.servoy.eclipse.team.ServoyTeamProvider;
import com.servoy.eclipse.team.ui.ResourceDecorator;
import com.servoy.j2db.util.Utils;

public class SolutionSubscriber extends ThreeWaySubscriber
{

	private static SolutionSubscriber instance;

	/**
	 * Return the solution subscriber singleton.
	 * 
	 * @return the solution singleton.
	 */
	public static synchronized SolutionSubscriber getInstance()
	{
		if (instance == null)
		{
			instance = new SolutionSubscriber();
		}
		return instance;
	}

	/**
	 * Create the solution subscriber.
	 */
	private SolutionSubscriber()
	{
		super(new ThreeWaySynchronizer(new QualifiedName(Activator.PLUGIN_ID, "workpsace-sync"))
		{

			private final HashMap<IResource, Long> changedResources = new HashMap<IResource, Long>();

			@Override
			public void flush(IResource resource, int depth) throws TeamException
			{
				super.flush(resource, depth);
				changedResources.remove(resource);
				RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject(), Activator.getTypeId());
				if (provider != null)
				{
					((ServoyTeamProvider)provider).deleteBaseResource(resource);
				}
			}

			@Override
			public void setBaseBytes(IResource resource, byte[] baseBytes) throws TeamException
			{
				super.setBaseBytes(resource, baseBytes);
				changedResources.remove(resource);
				RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject(), Activator.getTypeId());
				if (provider != null)
				{
					((ServoyTeamProvider)provider).writeBaseResource(resource);
				}
			}

			@Override
			public boolean isLocallyModified(IResource resource) throws TeamException
			{
				boolean isLocallyModified = super.isLocallyModified(resource);

				if (isLocallyModified)
				{
					byte[] baseBytes = getBaseBytes(resource);

					if (baseBytes != null && resource.exists())
					{
						Long changedTimestamp = changedResources.get(resource);
						if (changedTimestamp != null && changedTimestamp.longValue() == resource.getModificationStamp()) isLocallyModified = true;
						else
						{
							byte[] resourceContent = getResourceContent(resource);
							if (resourceContent != null)
							{
								byte[] resourceBytes = SolutionResourceVariant.getSynchBytes(resourceContent);
								isLocallyModified = !bytesEquals(baseBytes, resourceBytes);
								if (!isLocallyModified) setBaseBytes(resource, baseBytes); // reset timestamp
								else changedResources.put(resource, new Long(resource.getModificationStamp()));
							}
						}
					}
				}

				return isLocallyModified;
			}
		});
		getSynchronizer().addListener(new ISynchronizerChangeListener()
		{

			public void syncStateChanged(final IResource[] resources)
			{
				final ResourceDecorator rd = (ResourceDecorator)PlatformUI.getWorkbench().getDecoratorManager().getBaseLabelProvider(ResourceDecorator.ID);
				if (rd != null)
				{
					// redraw resources decoratores
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							rd.fireChanged(getAllParentResources(resources));
						}
					});
				}
			}

			private IResource[] getAllParentResources(IResource[] resources)
			{
				if (resources == null) return null;

				ArrayList<IResource> allParentResources = new ArrayList<IResource>();
				allParentResources.addAll(Arrays.asList(resources));

				for (IResource resource : resources)
				{
					IResource parent = resource;
					while ((parent = parent.getParent()) != null)
					{
						if (allParentResources.indexOf(parent) == -1) allParentResources.add(parent);
					}
				}

				return allParentResources.toArray(new IResource[allParentResources.size()]);
			}
		});
	}

	protected SolutionSubscriber(ThreeWaySynchronizer synchronizer)
	{
		super(synchronizer);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected ThreeWayRemoteTree createRemoteTree()
	{
		return new SolutionRemoteTree(this);
	}

	@Override
	public IResourceVariant getResourceVariant(IResource resource, byte[] bytes) throws TeamException
	{
		RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject(), Activator.getTypeId());
		if (provider != null)
		{
			return ((ServoyTeamProvider)provider).getResourceVariant(resource, bytes);
		}

		return null;
	}

	@Override
	public String getName()
	{
		return "Servoy solution subscriber";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.subscribers.Subscriber#roots()
	 */
	@Override
	public IResource[] roots()
	{
		List result = new ArrayList();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects)
		{
			if (project.isAccessible())
			{
				RepositoryProvider provider = RepositoryProvider.getProvider(project, Activator.getTypeId());
				if (provider != null)
				{
					result.add(project);
				}
			}
		}

		return (IProject[])result.toArray(new IProject[result.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.variants.ThreeWaySubscriber#handleRootChanged(org.eclipse.core.resources.IResource, boolean)
	 */
	@Override
	public void handleRootChanged(IResource resource, boolean added)
	{
		// Override to allow ServoyeTeamProvider to signal the addition and removal of roots
		super.handleRootChanged(resource, added);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.variants.ResourceVariantTreeSubscriber#getSyncInfo(org.eclipse.core.resources.IResource,
	 * org.eclipse.team.core.variants.IResourceVariant, org.eclipse.team.core.variants.IResourceVariant)
	 */
	@Override
	protected SyncInfo getSyncInfo(IResource local, IResourceVariant base, IResourceVariant remote) throws TeamException
	{
		// Override to use a custom sync info
		SolutionSyncInfo info = new SolutionSyncInfo(local, base, remote, this.getResourceComparator());
		info.init();
		return info;
	}

	/**
	 * Make the resource in-sync.
	 * 
	 * @param resource the resource
	 * @throws TeamException
	 */
	public void makeInSync(IResource resource) throws TeamException
	{
		ThreeWaySynchronizer synchronizer = getSynchronizer();
		byte[] remoteBytes = synchronizer.getRemoteBytes(resource);
		if (remoteBytes == null)
		{
			//if (!resource.exists()) synchronizer.flush(resource, IResource.DEPTH_ZERO);
			synchronizer.flush(resource, IResource.DEPTH_ZERO);
		}
		else
		{
			synchronizer.setBaseBytes(resource, remoteBytes);
		}
	}

	/**
	 * Make the change an outgoing change
	 * 
	 * @param resource
	 * @throws TeamException
	 */
	public void markAsMerged(IResource resource, IProgressMonitor monitor) throws TeamException
	{
		makeInSync(resource);
		try
		{
			resource.touch(monitor);
		}
		catch (CoreException e)
		{
			throw TeamException.asTeamException(e);
		}
	}

	public static boolean hasSameContent(IResource local, IResourceVariant remote)
	{
		if (local != null && remote != null)
		{
			byte[] resourceContent = getResourceContent(local);
			if (resourceContent != null)
			{
				byte[] resourceBytes = SolutionResourceVariant.getSynchBytes(resourceContent);
				return bytesEquals(resourceBytes, remote.asBytes());
			}
		}

		return false;
	}

	private static byte[] getResourceContent(IResource resource)
	{
		byte[] resourceContent = null;
		IPath resourceLocation = resource.getLocation();
		if (resourceLocation != null) resourceContent = Utils.getFileContent(resource.getLocation().toFile());

		return resourceContent;
	}

	private static boolean bytesEquals(byte[] syncBytes, byte[] oldBytes)
	{
		if (syncBytes.length != oldBytes.length) return false;
		for (int i = 0; i < oldBytes.length; i++)
		{
			if (oldBytes[i] != syncBytes[i]) return false;
		}
		return true;
	}
}
