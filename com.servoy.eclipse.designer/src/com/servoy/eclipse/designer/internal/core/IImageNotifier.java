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


/**
 * Interface for image notifiers, notify of changes to images.
 * 
 * @author rgansevles
 */

public interface IImageNotifier
{
	/**
	 * Add an image listener. Do not add an image listener except if you need the actual image. This is because if you are the only listener and you don't need the image it will
	 * still cause an image to be captured. This is a waste. Use {@link IVisualComponentListener#componentValidated()} instead.
	 * 
	 * @param listener
	 * 
	 */
	public void addImageListener(IImageListener listener);

	/**
	 * Is anyone listening?
	 * 
	 * @return
	 */
	public boolean hasImageListeners();

	/**
	 * Invalidate the image. The next time refreshImage is called, if still invalid, it will send out a new image. It will not trigger a new image at this time.
	 */
	public void invalidateImage();

	/**
	 * Refresh the image, get a new one and send notification if image was validated with this request. If the image was already valid, nothing will happen due to this call.
	 */
	public void refreshImage();

	/**
	 * Remove the image listener.
	 * 
	 * @param listener
	 */
	public void removeImageListener(IImageListener listener);

}
