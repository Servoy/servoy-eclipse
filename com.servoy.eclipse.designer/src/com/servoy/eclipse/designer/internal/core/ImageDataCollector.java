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


import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.awt.image.IndexColorModel;
import java.util.Hashtable;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.servoy.j2db.util.Utils;

/**
 * An AWT Image Consumer which will consume an AWT image and write the image back to the client (using the callback output stream). It will transmit commands to
 * tell the client how to build the image on the client side. This is necessary because we need to get it into a format that can easily be converted into an SWT
 * image.
 * 
 * The output stream will end with a CMD_DONE and the status code for completion. It will then be closed.
 * 
 * A simple compression algorithm will be used. If there are duplicate pixels, only the count of the number of dups, plus the pixel will be sent. Otherwise the
 * entire group of non-dups will be sent. This should help some because most images for components will have many dups along a row. They don't usually have a
 * different pixel right next to each other on a continued basis.
 */
public class ImageDataCollector implements ImageConsumer
{
	protected ImageNotifierSupport imSupport;

	protected int fWidth, fHeight;
	protected IndexColorModel fStartingIndexModel;
	protected boolean fIndex;
	protected boolean inProgress = false;
	protected boolean finished = false;

	protected static final int NO_MODEL = -1;
	protected int fDepth = NO_MODEL;

	protected PaletteData palette;
	protected int transparentPixel = -1;
	protected ImageData imageData;

	// fProducer and fEndProductionRequested can only be referenced/changed under synchronized (this) blocks.
	protected ImageProducer fProducer = null;
	protected boolean fEndProductionRequested = false; // There's no way to stop production, so we need to indicate that

	// production end was requested so that we can ignore further
	// data until the end is normally reached and then return the abort flag.

	public ImageDataCollector(ImageNotifierSupport imSupport)
	{
		this.imSupport = imSupport;
	}

	/**
	 * Start collecting on a specific image.
	 */
	protected boolean start(Image image) throws IllegalAccessException, IllegalArgumentException
	{
		return start(image.getSource());
	}

	/**
	 * Start collecting the image of a component. Return whether collection has started or not. It is possible that collection didn't start but this is not an
	 * error. For example the component has a width or height of 0.
	 */
	public boolean start(final Component component, final int maxWidth, final int maxHeight) throws IllegalArgumentException
	{
		// Need to queue the printall off to the UI thread because there could be problems in some
		// versions of the JDK if paint and print are done at the same time.
		// Also we now want (as of 1.1.0) the validate() to be done in UI thread too so that any
		// bounds changes will be batched together and sent in one transaction instead of each
		// individually.
		synchronized (this)
		{
			if (inProgress)
			{
				return false; // Already in progress. We only got this far because two requests were submitted before we got this far to mark one as started.
			}
			inProgress = true;
		}
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				// Now validate the component so that we have a good image to capture.
				// We need to go to top level component so that all validations are done,
				// if on a component, validate only validates that component, so it won't
				// get relayed out. We have to check for Window because we don't want to 
				// go higher than a Window. A Window can have a parent, but that is another
				// window, and validating that window will not validate child windows (this
				// relationship downward is ownership and not child containment).
				Container parent = component.getParent();
				while (parent != null && !(parent instanceof Window) && parent.getParent() != null)
				{
					parent = parent.getParent();
				}
				if (parent == null)
				{
					component.validate(); // There is no parent at all, so validate this one only.
				}
				else
				{
					parent.validate();
				}


				// Get the AWT image of the component
				if (component.getWidth() == 0 || component.getHeight() == 0)
				{
					imageComplete(STATICIMAGEDONE);
					synchronized (ImageDataCollector.this)
					{
						fProducer = null;
						inProgress = false;
						ImageDataCollector.this.notifyAll(); // Let anyone waiting know we are really done.
						return;
					}
				}

				int w = component.getWidth();
				int h = component.getHeight();
				if (w > maxWidth)
				{
					w = maxWidth;
				}
				if (h > maxHeight)
				{
					h = maxHeight;
				}
				final int iWidth = w;
				final int iHeight = h;
				final Image componentImage = new BufferedImage(iWidth, iHeight, BufferedImage.TYPE_INT_ARGB);
				inProgress = true;

				// We have a component image. We may not if it wasn't yet visible.
				Graphics graphics = null;
				try
				{
					try
					{
						graphics = componentImage.getGraphics();
						graphics.setClip(0, 0, iWidth, iHeight);
						component.printAll(graphics);

						if (Utils.isAppleMacOS())
						{
							//TODO//OSXComponentImageDecorator.decorateComponent(component, componentImage, iWidth, iHeight);
						}
					}
					finally
					{
						if (graphics != null)
						{
							graphics.dispose(); // Clear out the resources.
						}
						synchronized (ImageDataCollector.this)
						{
							if (fEndProductionRequested)
							{
								// End requested while retrieving image, means don't bother producing.
								fProducer = null;
								inProgress = false;
								ImageDataCollector.this.notifyAll(); // Let anyone waiting know we are really done.
								return;
							}
						}
					}
					if (!start(componentImage))
					{
						imageComplete(IMAGEERROR);
						synchronized (ImageDataCollector.this)
						{
							fProducer = null;
							inProgress = false;
							ImageDataCollector.this.notifyAll(); // Let anyone waiting know we are really done.
							return;
						}
					}
				}
				catch (Throwable e)
				{
					imageComplete(IMAGEERROR);
					synchronized (ImageDataCollector.this)
					{
						fProducer = null;
						inProgress = false;
						ImageDataCollector.this.notifyAll(); // Let anyone waiting know we are really done.
						return;
					}
				}
			}
		});
		return true;
	}


	/**
	 * Start production with the producer.
	 */
	protected boolean start(final ImageProducer producer) throws IllegalAccessException, IllegalArgumentException
	{
		synchronized (this)
		{
			if (fProducer != null)
			{
				throw new IllegalAccessException("Image collection already in progress."); //$NON-NLS-1$
			}
			if (producer == null)
			{
				throw new IllegalArgumentException("ImageProducer is null"); //$NON-NLS-1$
			}

			fStartingIndexModel = null;
			fIndex = false;
			fWidth = fHeight = -1;
			fDepth = NO_MODEL;
			inProgress = true;
			fProducer = producer;
			fEndProductionRequested = false;
			finished = false;
		}

		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					producer.startProduction(ImageDataCollector.this);
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
				finally
				{
					try
					{
						// In case it did not finish correctly, we need to finish it.
						if (!finished)
						{
							// It failed and didn't call imageComplete, so simulate imageComplete with error.
							imageComplete(IMAGEERROR);
						}
					}
					finally
					{
						synchronized (ImageDataCollector.this)
						{
							fProducer = null;
							inProgress = false;
							ImageDataCollector.this.notifyAll(); // Let anyone waiting know we are really done.
						}
					}
				}
			}
		}).start();

		return true;
	}

	public void setDimensions(int width, int height)
	{
		synchronized (this)
		{
			if (fEndProductionRequested)
			{
				return; // We don't want to bother sending the data
			}
		}

		fWidth = width;
		fHeight = height;
	}

	public void setProperties(Hashtable properties)
	{
		// We do nothing with properties
	}

	/**
	 * set the color model to use.
	 * 
	 * Send the color model to use over to the client.
	 */
	public void setColorModel(ColorModel model)
	{
		synchronized (this)
		{
			if (fEndProductionRequested)
			{
				return; // We don't want to bother sending the data
			}
		}

		if (!(model instanceof IndexColorModel))
		{
			// Not an index model.
			if (fIndex)
			{
				// We have an existing index model, and going to not index. Need to convert image to direct
				setClientDirectColorModel(model.getPixelSize());
			}
			else
			{
				// Tell client of model at the same depth as the incoming model.
				// We will use direct model on the client side since none of the others are known in SWT.
				int newDepth = model.getPixelSize();
				if (newDepth == 15)
				{
					newDepth = 16; // SWT can't handle 15 as a depth, but this
					// is equivalent to 16 bit. And since we
					// use the java.awt.ColorModel to get the pixels
					// out of the word, it gets them out in the
					// necessary 16 bit format (i.e. 8 bits per color,
					// which we shift down to 5 bits required by depth 16).
				}
				if (fDepth != newDepth) setClientDirectColorModel(newDepth);
			}
		}
		else
		{
			// It is an index model, see if different model.
			if (model != fStartingIndexModel) if (fDepth == NO_MODEL)
			{
				// We have no model yet. Use this one.
				setClientIndexColorModel((IndexColorModel)model);
			}
			else if (fIndex)
			{
				// The current model is an index but not the current index, see if we can use
				// If the current model was direct, then use it as is and ignore this setting. We won't convert back to indexed.

				// See if they are compatible, it so use as is.
				boolean compatible = false;
				if (fStartingIndexModel.getPixelSize() == model.getPixelSize())
				{
					int newLength = ((IndexColorModel)model).getMapSize();
					int oldLength = fStartingIndexModel.getMapSize();
					if (newLength == oldLength)
					{
						int[] oldRGBs = new int[oldLength];
						int[] newRGBs = new int[newLength];
						fStartingIndexModel.getRGBs(oldRGBs);
						((IndexColorModel)model).getRGBs(newRGBs);
						compatible = java.util.Arrays.equals(oldRGBs, newRGBs);
					}
				}

				if (!compatible) setClientDirectColorModel(0); // Not compatible, convert to the direct model on the client.
			}
		}
	}


	private static int getCurrentDisplayDepth()
	{
		class DisplayDepthGetter implements Runnable
		{
			int depth;

			public void run()
			{
				// This method can only be called from display thread
				depth = Display.getCurrent().getDepth();
			}
		}

		Display dsp = Display.getCurrent();
		if (dsp == null)
		{
			dsp = Display.getDefault();
		}
		DisplayDepthGetter getter = new DisplayDepthGetter();
		dsp.syncExec(getter);
		return getter.depth;
	}


	protected void setClientDirectColorModel(int depth)
	{
		// Tell the client to use a direct color palette of the specified depth.
		switch (depth)
		{
			case 0 :
				// Need to get the preferred depth from the client.
				depth = getCurrentDisplayDepth();
				break;
			case 16 :
			case 24 :
				break; // Currently can only handle 16 and 24 (on Windows).
			default :
				depth = 24; // Anything else we will use depth of 24 (32 is valid, but there is a bug that it won't accept the palette correctly).
		}

		fDepth = depth;
		fStartingIndexModel = null;
		fIndex = false;

		if (fDepth == 16)
		{
			palette = new PaletteData(0x7C00, 0x3E0, 0x1F);
		}
		else
		{
			// Currently only handle 16 and 24 direct. (32 bit has a bug in OTI).
			palette = new PaletteData(0xFF, 0xFF00, 0xFF0000);
		}

		if (imageData != null && !imageData.palette.isDirect)
		{
			imageData = convertToDirect(depth, imageData, palette);
		}
	}

	protected static ImageData convertToDirect(int depth, ImageData oldImageData, PaletteData palette)
	{
		// Procedure is to get the old data, row by row into an int array. Then use
		// the old color model to take the int's and convert them to RGB and place them
		// into the new imageData.
		// We have something to convert
		ImageData newImageData = new ImageData(oldImageData.width, oldImageData.height, depth, palette);
		// Convert each old pixel into RGB for setting into the data. 
		int[] pixelRow = new int[oldImageData.width];
		RGB[] oldColorModel = oldImageData.palette.getRGBs();
		for (int row = 0; row < oldImageData.height; row++)
		{
			oldImageData.getPixels(0, row, oldImageData.width, pixelRow, 0); // Get one row from old
			for (int col = 0; col < oldImageData.width; col++)
			{
				int pixel = pixelRow[col];
				int red = oldColorModel[pixel].red;
				int green = oldColorModel[pixel].green;
				int blue = oldColorModel[pixel].blue;

				if (depth == 16)
				{
					// They are stored in reverse-byte order as if stored in an integer of RGB in the imageData.					
					// The RGB's are stored as 256 range, so we need to shift down to 5 bits.
					red = (red >> 3) & 0x1F;
					green = (green >> 3) & 0x1F;
					blue = (blue >> 3) & 0x1F;
					pixelRow[col] = (red << 10) | (green << 5) | blue;
				}
				else
				{
					pixelRow[col] = (blue & 0xFF) << 16 | (green & 0xFF) << 8 | (red & 0xFF);
				}
			}
			newImageData.setPixels(0, row, newImageData.width, pixelRow, 0);
		}

		return newImageData;
	}


	protected void setClientIndexColorModel(IndexColorModel model)
	{
		// Tell the client to use an indexed color palette.
		fStartingIndexModel = model;
		fIndex = true;
		fDepth = model.getPixelSize();
		int length = model.getMapSize();
		transparentPixel = model.getTransparentPixel();
		byte[] reds = new byte[length];
		byte[] greens = new byte[length];
		byte[] blues = new byte[length];
		model.getReds(reds);
		model.getGreens(greens);
		model.getBlues(blues);


		RGB[] rgbs = new RGB[length];
		for (int i = 0; i < length; i++)
		{
			// The bytes need to be unsigned, but java doesn't have that, so we need to do it
			// RGB takes ints range 0-255, not -128 to 127
			rgbs[i] = new RGB(reds[i] & 0x000000ff, greens[i] & 0x000000ff, blues[i] & 0x000000ff);
		}
		palette = new PaletteData(rgbs);
	}

	public void setHints(int hintsFlags)
	{
		// We do nothing with hints
	}

	/**
	 * setPixels: Set the pixels. The pixels can fit into a byte. Test for sure, but if it fits into a byte, it will be a color index model.
	 */
	public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off, int scansize)
	{
		synchronized (this)
		{
			if (fEndProductionRequested)
			{
				return; // We don't want to bother sending the data
			}
		}

		if (fIndex)
		{
			// Current palette is an index model, so see if compatible
			if (fStartingIndexModel != model)
			{
				setClientDirectColorModel(0); // Convert over to direct model, easiest way to handle model change.
				setPixels(x, y, w, h, model, pixels, off, scansize); // Now try with the new palette
			}
			else
			{
				// If I'm in bytes, then depth must be 1, 4, or 8. ImageData can handle those.
				// Depth greater than 8 is impossible anyway.
				for (int rowOffset = 0, row = y; rowOffset < h; rowOffset++, row++)
				{
					// Send each row separately.
					if (imageData == null)
					{
						imageData = new ImageData(fWidth, fHeight, fDepth, palette);
						if (transparentPixel != -1)
						{
							imageData.transparentPixel = transparentPixel;
						}
					}
					imageData.setPixels(x, row, w, pixels, off + (scansize * rowOffset));
				}
			}
		}
		else
		{
			if (w <= 0)
			{
				return; // Sanity check. If the width is zero, don't go one.
			}

			// Currently a direct palette, so get the RGB's and place into the data. 
			for (int rowOffset = 0, row = y; rowOffset < h; rowOffset++, row++)
			{
				if (imageData == null)
				{
					imageData = new ImageData(fWidth, fHeight, fDepth, palette);
					if (transparentPixel != -1)
					{
						imageData.transparentPixel = transparentPixel;
					}
				}

				int[] ints = new int[w];
				for (int i = 0; i < w; i++)
				{
					ints[i] = getIntPixel(fDepth, model, pixels[off + (scansize * rowOffset) + i]);
				}
				imageData.setPixels(x, row, w, ints, 0);
			}
		}
	}

	private static int getIntPixel(int depth, ColorModel model, int pixel)
	{
		// Convert each incoming pixel into RGB for setting into the data. Need to do this
		// because Java RGB model doesn't match SWT RGB model.		

		// Currently only handle direct of 16 and 24.
		if (depth == 16)
		{
			// The AWT model scales the colors to 0-255, even when in depth 16, so we need
			// to scale back to 5 bits.
			int red = (model.getRed(pixel) >> 3) & 0x1F;
			int green = (model.getGreen(pixel) >> 3) & 0x1F;
			int blue = (model.getBlue(pixel) >> 3) & 0x1F;

			return (red << 10 | green << 5 | blue);
		}
		else
		{
			return ((model.getBlue(pixel) & 0xFF) << 16 | (model.getGreen(pixel) & 0xFF) << 8 | model.getRed(pixel) & 0xFF);
		}
	}

	public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off, int scansize)
	{
		synchronized (this)
		{
			if (fEndProductionRequested)
			{
				return; // We don't want to bother sending the data
			}
		}

		if (fIndex)
		{
			// Current palette is an index model, so see if compatible
			if (fStartingIndexModel != model)
			{
				setClientDirectColorModel(0); // Convert over to direct model, easiest way to handle model change.
				setPixels(x, y, w, h, model, pixels, off, scansize); // Now try with the new palette
			}
			else
			{
				// If I'm in bytes, then depth must be 1, 4, or 8. ImageData can handle those.
				// Depth greater than 8 is impossible anyway.
				for (int rowOffset = 0, row = y; rowOffset < h; rowOffset++, row++)
				{
					// Send each row separately.
					if (imageData == null)
					{
						imageData = new ImageData(fWidth, fHeight, fDepth, palette);
						if (transparentPixel != -1)
						{
							imageData.transparentPixel = transparentPixel;
						}
					}
					imageData.setPixels(x, row, w, pixels, off + (scansize * rowOffset));
				}
			}
		}
		else
		{
			// Currently a direct palette, so get the RGB's and place into the data.
			// Convert each incoming pixel into RGB for setting into the data. Need to do this
			// because Java RGB model doesn't match SWT RGB model. 
			for (int rowOffset = 0, row = y; rowOffset < h; rowOffset++, row++)
			{
				if (imageData == null)
				{
					imageData = new ImageData(fWidth, fHeight, fDepth, palette);
					if (transparentPixel != -1)
					{
						imageData.transparentPixel = transparentPixel;
					}
				}

				int[] ints = new int[w];
				byte[] alphas = new byte[w];
				for (int i = 0; i < w; i++)
				{
					int pixel = pixels[off + (scansize * rowOffset) + i];
					ints[i] = getIntPixel(fDepth, model, pixel);
					alphas[i] = (byte)((pixel >> 24) & 0xff);
				}
				imageData.setPixels(x, row, w, ints, 0);
				imageData.setAlphas(x, row, w, alphas, 0);
			}
		}

	}

	public void imageComplete(int status)
	{
		try
		{
			ImageProducer producer = null;
			synchronized (this)
			{
				if (fProducer == null)
				{
					return;
				}
				producer = fProducer;
				inProgress = false;
				fEndProductionRequested = false;
				finished = true; // This being set to true indicates that we have gone through a complete. If production ended due to some error 
				// and it didn't go through imageComplete, then this will be non-null and it knows to complete it.																							
			}
			producer.removeConsumer(this);
			imSupport.fireImageChanged(imageData);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public synchronized void waitForCompletion()
	{
		if (!inProgress)
		{
			return;
		}
		while (true)
		{
			try
			{
				wait();
				break;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public synchronized boolean isCollecting()
	{
		return fProducer != null;
	}

	public void abort()
	{
		requestTerminate();
	}

	protected void requestTerminate()
	{
		synchronized (this)
		{
			if (!fEndProductionRequested && inProgress)
			{
				// We're in progress and we haven't already asked for a termination.
				fEndProductionRequested = true;
				inProgress = false;
			}
		}
	}

}
