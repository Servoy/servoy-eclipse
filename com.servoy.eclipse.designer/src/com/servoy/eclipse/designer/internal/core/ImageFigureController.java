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

import java.util.List;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.FigureListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;


public class ImageFigureController
{
	protected ImageFigure figure;

	protected IImageNotifier imageNotifier;

	protected Image image; // If a modified image is in use, this will be non-null.

	private ImageData imageData; // The current image data. Only set if we have a region to lighten.

	private byte refreshState = NO_REFRESH_PENDING; // Flag for refresh state

	private boolean crossHatch; // Should we crosshatch the lightened area.
	private static final RGB DEFAULT_LIGHTEN_RGB = new RGB(255, 255, 255);
	private RGB lightenColor = DEFAULT_LIGHTEN_RGB;
	private static final RGB DEFAULT_CROSSHATCH_COLOR = new RGB(128, 128, 128);
	private RGB crossHatchColor = DEFAULT_CROSSHATCH_COLOR;

	private final double DEFAULT_ALPHA = .5;
	private double alpha = DEFAULT_ALPHA;

	private static final byte NO_REFRESH_PENDING = 0x0, // No refresh pending.
		RECREATE_IMAGE = 0x1, // Recreate the image
		FIGURE_MOVED = 0x2; // figure has moved, refresh if lightening

	// This is a private class for listening so as not to pollute the public interface of this class.
	private class Listeners implements IImageListener, FigureListener
	{

		public void imageChanged(ImageData data)
		{
			synchronized (ImageFigureController.this)
			{
				imageData = data;
				scheduleRefresh(RECREATE_IMAGE);
			}
		}

		public void figureMoved(IFigure f)
		{
			scheduleRefresh(FIGURE_MOVED);
		}
	}

	private Listeners listener;

	public ImageFigureController()
	{
	}

	@Override
	public void finalize()
	{
		// Just to be on safe side if we aren't deactivated and just thrown away.
		deactivate();
		// Our Tool Tip points to CodeGen/Editor Part
		// Finalizer hold on to the ImageFigureController that points to the Figure->ToolTip
		// Can not put this in the dispose image, because it is called many times in the life cycle
		// of the image, in which case we will loose the ToolTip
		if (figure != null) figure.setToolTip(null);
	}

	/**
	 * Return the figure the controller is managing.
	 */
	public IFigure getFigure()
	{
		return figure;
	}

	/**
	 * Set the ImageFigure that this controller is managing.
	 */
	public void setImageFigure(ImageFigure figure)
	{
		this.figure = figure;
	}

	/**
	 * Deactivate, clean up. This should be called by the editpart deactivate method.
	 */
	public void deactivate()
	{
		if (imageNotifier != null)
		{
			imageNotifier.removeImageListener(getListener());
			imageNotifier = null;
		}
		refreshState = NO_REFRESH_PENDING;

		disposeImage();
	}

	protected void disposeImage()
	{
		figure.setImage(null);
		if (image != null)
		{
			image.dispose();
			image = null;
		}
		imageData = null;
	}

	protected Listeners getListener()
	{
		if (listener == null) listener = new Listeners();
		return listener;
	}

	/**
	 * Set the image notifier to listen on.
	 */
	public void setImageNotifier(IImageNotifier notifier)
	{
		deactivate();
		if (notifier != null)
		{
			imageNotifier = notifier;
			imageNotifier.addImageListener(getListener());
			imageNotifier.invalidateImage();
			imageNotifier.refreshImage();
		}
	}

	/**
	 * Schedule a refresh. We will queue these up, but the first one that runs will actually do the refresh. The rest will do nothing. this is because many figures could change at
	 * one time.
	 */
	protected synchronized void scheduleRefresh(byte refreshType)
	{
		if (refreshType == FIGURE_MOVED) return; // Don't schedule anything
		refreshState |= refreshType;
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				synchronized (ImageFigureController.this)
				{
					if (refreshState != NO_REFRESH_PENDING)
					{
						if ((refreshState & RECREATE_IMAGE) != 0)
						{
							refreshState = NO_REFRESH_PENDING;
							recreateImage();
						}
					}
				}
			}

			/**
			 * recreate the image from the imageData. Since this is within the runnable, it is within the display thread. It is also automatically synced with ImageFigureController
			 * because it is called only from the sync block within run().
			 */
			private void recreateImage()
			{
				Image newImage = null;
				if (imageData != null) newImage = new Image(Display.getCurrent(), imageData);
				figure.setImage(newImage);
				if (image != null) image.dispose(); // Get rid of the old one.
				image = newImage;
			}

		});
	}

	/*
	 * Set the border enable state. This will go through the figure and its children and disable the border state. It will only do this for ImageFigures that have an OutlineBorder.
	 * Since non-image figures don't really participate in the lightening we don't look at them. @param fig @param b
	 * 
	 * @since 1.0.0
	 */
	private void setBorderEanbleStates(IFigure fig, boolean disableBorder)
	{
		if (fig instanceof ImageFigure)
		{
			Border border = fig.getBorder();
			if (border instanceof OutlineBorder) ((OutlineBorder)border).setOverrideAndDisable(disableBorder);
			List children = fig.getChildren();
			for (int i = 0; i < children.size(); i++)
			{
				setBorderEanbleStates((IFigure)children.get(i), disableBorder);
			}
		}
	}


	/**
	 * Set to crosshatch any lightened area. It will only crosshatch lightened areas, it won't crosshatch regular areas.
	 * 
	 * @param crossHatch
	 *            <code>true</code> to crosshatch it.
	 * @since 1.0.0
	 */
	public void setCrossHatch(boolean crossHatch)
	{
		this.crossHatch = crossHatch;
	}

	/**
	 * Is it set to crosshatch the lightened areas?
	 * 
	 * @return Returns the crossHatch.
	 * @since 1.0.0
	 */
	public boolean isCrossHatch()
	{
		return crossHatch;

	}

	/**
	 * @param lightenColor
	 *            The lightenColor to set. <code>null</code> to reset back to default color.
	 */
	public void setLightenColor(RGB lightenColor)
	{
		if (lightenColor == null) this.lightenColor = DEFAULT_LIGHTEN_RGB;
		else this.lightenColor = lightenColor;
	}

	/**
	 * @return Returns the lightenColor.
	 */
	public RGB getLightenColor()
	{
		return lightenColor;
	}

	/**
	 * @param crossHatchColor
	 *            The crossHatchColor to set. <code>null</code> to reset to default color.
	 */
	public void setCrossHatchColor(RGB crossHatchColor)
	{
		if (crossHatchColor == null) this.crossHatchColor = DEFAULT_CROSSHATCH_COLOR;
		else this.crossHatchColor = crossHatchColor;
	}

	/**
	 * @return Returns the crossHatchColor.
	 */
	public RGB getCrossHatchColor()
	{
		return crossHatchColor;
	}

	/**
	 * @param alpha
	 *            The alpha to set. (0.0 to 1.0 is valid, outside of this range will reset to default alpha).
	 */
	public void setAlpha(double alpha)
	{
		if (alpha < 0.0 || alpha > 1.0) this.alpha = DEFAULT_ALPHA;
		else this.alpha = alpha;
	}

	/**
	 * @return Returns the alpha.
	 */
	public double getAlpha()
	{
		return alpha;
	}

}
