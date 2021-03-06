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
package com.servoy.eclipse.designer.internal.core;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.graphics.ImageData;

/**
 * Fire when form element image is done painting.
 * 
 * @author rgansevles
 */

public class ImageNotifierSupport
{
	protected ListenerList imageListeners = null;

	/**
	 * Add image listener.
	 * 
	 * @param aListener
	 */
	public synchronized void addImageListener(IImageListener aListener)
	{
		if (imageListeners == null) imageListeners = new ListenerList(ListenerList.IDENTITY);
		imageListeners.add(aListener);
	}

	/**
	 * Fire image changed notification.
	 * 
	 * @param imageData
	 */
	public void fireImageChanged(ImageData imageData)
	{
		// Probably should make a copy of the notification list to prevent
		// modifications while firing, but we'll see if this gives any problems.
		if (imageListeners != null && !imageListeners.isEmpty())
		{
			Object[] listeners = imageListeners.getListeners();
			for (Object element : listeners)
			{
				((IImageListener)element).imageChanged(imageData);
			}
		}
	}

	/**
	 * Is anyone listening?
	 * 
	 * @return
	 */
	public boolean hasImageListeners()
	{
		return imageListeners != null && !imageListeners.isEmpty();
	}

	/**
	 * Remove listener.
	 * 
	 * @param aListener
	 */
	public synchronized void removeImageListener(IImageListener aListener)
	{
		if (imageListeners != null) imageListeners.remove(aListener);
	}
}
