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

import java.util.ArrayList;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Problem decorator
 * 
 * @author gboros
 *
 */
public class ProblemDecorator implements ILightweightLabelDecorator
{
	public static final String ID = "com.servoy.eclipse.ui.views.solutionexplorer.ProblemDecorator"; //$NON-NLS-1$

	private final ImageDescriptor IMG_ERROR = JFaceResources.getImageRegistry().getDescriptor("org.eclipse.jface.fieldassist.IMG_DEC_FIELD_ERROR"); //$NON-NLS-1$
	private final ImageDescriptor IMG_WARNING = JFaceResources.getImageRegistry().getDescriptor("org.eclipse.jface.fieldassist.IMG_DEC_FIELD_WARNING"); //$NON-NLS-1$

	private static int ERRORTICK_NONE = 0;
	private static int ERRORTICK_WARNING = 1;
	private static int ERRORTICK_ERROR = 2;

	private final ArrayList<ILabelProviderListener> listeners = new ArrayList<ILabelProviderListener>();

	/*
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
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

	/*
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	public boolean isLabelProperty(Object element, String property)
	{
		return false;
	}

	/*
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener)
	{
		synchronized (listeners)
		{
			listeners.remove(listener);
		}
	}

	/*
	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
	 */
	public void decorate(Object element, IDecoration decoration)
	{
		if (element instanceof PlatformSimpleUserNode)
		{
			IResource resource = (IResource)((PlatformSimpleUserNode)element).getAdapter(IResource.class);
			if (resource != null && resource.exists())
			{
				try
				{
					IMarker[] markers = resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
					if (resource.getName().toLowerCase().endsWith(SolutionSerializer.FORM_FILE_EXTENSION))
					{
						String resourceName = resource.getName();
						int extIdx = resourceName.lastIndexOf(SolutionSerializer.FORM_FILE_EXTENSION);
						if (extIdx > 0)
						{
							resourceName = resourceName.substring(0, extIdx);
							IResource jsResource = resource.getParent().findMember(resourceName + SolutionSerializer.JS_FILE_EXTENSION);
							if (jsResource != null)
							{
								IMarker[] jsMarkers = jsResource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
								if (jsMarkers.length > 0)
								{
									IMarker[] allMarkers = new IMarker[markers.length + jsMarkers.length];
									System.arraycopy(markers, 0, allMarkers, 0, markers.length);
									System.arraycopy(jsMarkers, 0, allMarkers, markers.length, jsMarkers.length);
									markers = allMarkers;
								}
							}
						}
					}
					int info = ERRORTICK_NONE;
					if (markers != null)
					{
						for (int i = 0; i < markers.length && (info != ERRORTICK_ERROR); i++)
						{
							IMarker curr = markers[i];
							int priority = curr.getAttribute(IMarker.SEVERITY, -1);
							if (priority == IMarker.SEVERITY_WARNING)
							{
								info = ERRORTICK_WARNING;
							}
							else if (priority == IMarker.SEVERITY_ERROR)
							{
								info = ERRORTICK_ERROR;
							}
						}
					}

					ImageDescriptor imgd = null;
					if (info == ERRORTICK_WARNING)
					{
						imgd = IMG_WARNING;
					}
					else if (info == ERRORTICK_ERROR)
					{
						imgd = IMG_ERROR;
					}

					if (imgd != null)
					{
						decoration.addOverlay(imgd, IDecoration.BOTTOM_LEFT);
					}
				}
				catch (CoreException ex)
				{
					ServoyLog.logError(ex);
				}
			}
		}
	}
}
