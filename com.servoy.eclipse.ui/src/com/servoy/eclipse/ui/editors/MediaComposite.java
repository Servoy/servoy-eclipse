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
package com.servoy.eclipse.ui.editors;

import java.io.ByteArrayInputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.servoy.j2db.persistence.Media;

/**
 * Composite to view a media image
 * 
 * @author rgansevles
 * 
 */
public class MediaComposite extends ScrolledComposite
{
	private final Label mediaLabel;

	private Point fixedSize = null;
	private String noImageText = "No image";

	public MediaComposite(Composite parent, int style)
	{
		super(parent, style);

		setExpandHorizontal(true);
		setExpandVertical(true);

		Composite comp = new Composite(this, SWT.NONE);
		comp.setLayout(new FillLayout());
		mediaLabel = new Label(comp, SWT.CENTER);

		setContent(comp);
	}


	public void setFixedSize(Point fixedSize)
	{
		this.fixedSize = fixedSize;
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed)
	{
		if (fixedSize == null)
		{
			return super.computeSize(wHint, hHint, changed);
		}
		return fixedSize;
	}

	public void setNoImageText(String noImageText)
	{
		this.noImageText = noImageText;
	}

	/**
	 * Set the media to show
	 * 
	 * @param media
	 */
	public void setMedia(Media media)
	{
		disposeCurrentImage();
		if (media == null)
		{
			mediaLabel.setText(noImageText);
		}
		else
		{
			mediaLabel.setText("Loading image " + media.getName() + " ...");
			byte[] mediaData = media.getMediaData();
			if (mediaData == null)
			{
				mediaLabel.setText("Image not found: " + media.getName());
				return;
			}
			ImageData imageData = new ImageData(new ByteArrayInputStream(mediaData));
			Image image = new Image(Display.getCurrent(), imageData);
			if (fixedSize != null)
			{
				double xscale = ((double)fixedSize.x) / image.getBounds().width;
				double yscale = ((double)fixedSize.y) / image.getBounds().height;
				if (xscale < 1 || yscale < 1)
				{
					double zoom = xscale < yscale ? xscale : yscale;
					imageData = imageData.scaledTo((int)(zoom * image.getBounds().width), (int)(zoom * image.getBounds().height));
					image.dispose();
					image = new Image(Display.getCurrent(), imageData);
				}
			}
			mediaLabel.setImage(image);
		}
		setMinSize(getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	@Override
	public void dispose()
	{
		disposeCurrentImage();
		super.dispose();
	}

	protected void disposeCurrentImage()
	{
		Image currentImage = mediaLabel.getImage();
		mediaLabel.setImage(null);
		if (currentImage != null)
		{
			currentImage.dispose();
		}
	}

}
