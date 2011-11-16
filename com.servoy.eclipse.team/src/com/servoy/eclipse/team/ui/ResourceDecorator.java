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
package com.servoy.eclipse.team.ui;

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.team.Activator;
import com.servoy.eclipse.team.ServoyTeamProvider;
import com.servoy.eclipse.team.subscriber.SolutionSubscriber;

public class ResourceDecorator implements ILightweightLabelDecorator
{
	public static final String ID = "com.servoy.eclipse.team.ui.ResourceDecorator";

	private static ImageDescriptor IMG_CHECKEDIN = new CachedImageDescriptor(TeamImages.getImageDescriptor(ISharedImages.IMG_CHECKEDIN_OVR));
	private static ImageDescriptor IMG_DIRTY = new CachedImageDescriptor(TeamImages.getImageDescriptor(ISharedImages.IMG_DIRTY_OVR));

	private final ArrayList<ILabelProviderListener> listeners = new ArrayList<ILabelProviderListener>();

	static
	{
		TeamPlugin.getPlugin().getPluginPreferences().addPropertyChangeListener(new Preferences.IPropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent event)
			{
				final ResourceDecorator rd = (ResourceDecorator)PlatformUI.getWorkbench().getDecoratorManager().getBaseLabelProvider(ResourceDecorator.ID);
				if (rd != null)
				{
					// redraw resources decoratores
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							rd.fireChanged(null);
						}
					});
				}
			}
		});
	}

	public void decorate(Object element, IDecoration decoration)
	{
		IResource resource = null;
		if (element instanceof IResource)
		{
			resource = (IResource)element;
			if (resource == null || resource.getType() == IResource.ROOT) return;

			// get the team provider
			ServoyTeamProvider servoyTeamProvider = (ServoyTeamProvider)RepositoryProvider.getProvider(resource.getProject(), Activator.getTypeId());
			if (servoyTeamProvider == null) return;

			switch (getDiff(resource))
			{
				case IDiff.ADD :
					return; // no decoration
				case IDiff.REMOVE :
					break;
				case IDiff.CHANGE :
					decoration.addOverlay(IMG_DIRTY, IDecoration.BOTTOM_RIGHT);
					return;
			}

			decoration.addOverlay(IMG_CHECKEDIN, IDecoration.BOTTOM_RIGHT);
		}
		else
		{
			// in case of a mapping check for all resources, and if at least one is dirty mark the mapping dirty
			ResourceMapping resourceMapping = (ResourceMapping)Platform.getAdapterManager().getAdapter(element,
				org.eclipse.core.resources.mapping.ResourceMapping.class);
			if (resourceMapping != null)
			{
				final ImageDescriptor mappingImg[] = { null };
				try
				{
					resourceMapping.accept(ResourceMappingContext.LOCAL_CONTEXT, new IResourceVisitor()
					{

						public boolean visit(IResource r) throws CoreException
						{
							if (r.getType() != IResource.ROOT && mappingImg[0] != IMG_DIRTY)
							{
								if ((ServoyTeamProvider)RepositoryProvider.getProvider(r.getProject(), Activator.getTypeId()) != null)
								{
									int rDiff = getDiff(r);
									if (rDiff == IDiff.ADD || rDiff == IDiff.REMOVE || rDiff == IDiff.CHANGE)
									{
										mappingImg[0] = IMG_DIRTY;
									}
									else mappingImg[0] = IMG_CHECKEDIN;
								}
							}
							return false;
						}

					}, null);
				}
				catch (CoreException ex)
				{
					// annoying message that appears when a project is not open while visiting...
					if (ex.getMessage() == null || !ex.getMessage().contains("is not open")) ServoyLog.logError(ex);
				}

				if (mappingImg[0] != null) decoration.addOverlay(mappingImg[0], IDecoration.BOTTOM_RIGHT);
			}
		}
	}

	public void addListener(ILabelProviderListener listener)
	{
		listeners.add(listener);
	}

	public void fireChanged(IResource[] resource)
	{
		synchronized (listeners)
		{
			for (ILabelProviderListener listener : listeners)
				listener.labelProviderChanged(new LabelProviderChangedEvent(this, resource));
		}
	}

	public void dispose()
	{
		// TODO Auto-generated method stub

	}

	public boolean isLabelProperty(Object element, String property)
	{
		// TODO Auto-generated method stub
		return false;
	}

	public void removeListener(ILabelProviderListener listener)
	{
		synchronized (listeners)
		{
			listeners.remove(listener);
		}
	}


	private int getDiff(IResource resource)
	{
		SolutionSubscriber solutionSubscriber = SolutionSubscriber.getInstance();

		try
		{
			IDiff diff = solutionSubscriber.getDiff(resource);
			int rType = resource.getType();
			if ((rType == IResource.FOLDER || rType == IResource.PROJECT) && (diff == null || diff.getKind() == IDiff.NO_CHANGE))
			{
				if (!resource.exists()) return IDiff.REMOVE;
				// check children
				IResource[] resources = solutionSubscriber.members(resource);
				for (IResource r : resources)
				{
					if (getDiff(r) != IDiff.NO_CHANGE) return IDiff.CHANGE;
				}
			}
			else
			{
				if (diff != null) return diff.getKind();
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}

		return IDiff.NO_CHANGE;
	}

	/*
	 * Define a cached image descriptor which only creates the image data once
	 */
	public static class CachedImageDescriptor extends ImageDescriptor
	{
		ImageDescriptor descriptor;
		ImageData data;

		public CachedImageDescriptor(ImageDescriptor descriptor)
		{
			this.descriptor = descriptor;
		}

		@Override
		public ImageData getImageData()
		{
			if (data == null)
			{
				data = descriptor.getImageData();
			}
			return data;
		}
	}
}