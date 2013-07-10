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

import javax.swing.JLabel;

import org.eclipse.swt.graphics.ImageData;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.DesignApplication;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.IApplication;

/** 
 * Base class for painting components to an swt image.
 * 
 * @author rgansevles
 */

public abstract class AbstractImageNotifier implements IImageNotifier, IImageListener
{
	protected final IApplication application;
	private ImageData imageData;
	private JLabel label = null; // for painting
	private volatile boolean isWaitingStart = false;

	ImageNotifierSupport imSupport;

	public AbstractImageNotifier(IApplication application)
	{
		this.application = application;
	}

	public void addImageListener(IImageListener listener)
	{
		if (imSupport == null)
		{
			imSupport = new ImageNotifierSupport();
			imSupport.addImageListener(this); // for caching imageData
		}
		imSupport.addImageListener(listener);
	}

	public void removeImageListener(IImageListener listener)
	{
		if (imSupport != null)
		{
			imSupport.removeImageListener(listener);
		}
	}

	public void invalidateImage()
	{
		imageData = null;
	}

	public void refreshImage()
	{
		if (imageData == null)
		{
			if (isWaitingStart)
			{
				// am already waiting to paint the component, no need to paint again 
				return;
			}
			isWaitingStart = true;

			UIUtils.invokeLaterOnAWT(new Runnable()
			{
				public void run()
				{
					try
					{
						Component component = createComponent();
						if (component == null)
						{
							imSupport.fireImageChanged(null);
							return;
						}

						if (label == null)
						{
							label = new JLabel()
							{
								@Override
								public boolean isShowing()
								{
									// make sure all awt components will paint
									return true;
								}
							};
						}
						label.removeAll();
						label.setOpaque(false);
						label.setVisible(true);

						((DesignApplication)application).getEditLabel().add(label);
						isWaitingStart = false;
						label.add(component);
						label.setSize(component.getWidth(), component.getHeight());
						component.setLocation(0, 0); // paint in left-upper corner
						label.doLayout();

						createImageDataCollector(imSupport).start(label, component.getWidth(), component.getHeight(), handleAlpha(component));
					}
					catch (Exception e)
					{
						isWaitingStart = false;
						ServoyLog.logError(e);
						if (label != null) ((DesignApplication)application).getEditLabel().remove(label); // normally done in imageChanged()
					}
				}
			});
		}
		else
		{
			imSupport.fireImageChanged(imageData);
		}
	}

	protected abstract Component createComponent();

	/**
	 * @param component  
	 */
	protected float handleAlpha(Component component)
	{
		return 1.0f;
	}

	public void imageChanged(ImageData data)
	{
		imageData = data;
		if (label != null)
		{
			UIUtils.invokeLaterOnAWT(new Runnable()
			{
				public void run()
				{
					((DesignApplication)application).getEditLabel().remove(label);
					((DesignApplication)application).getEditLabel().repaint();
				}
			});
		}
	}

	public boolean hasImageListeners()
	{
		return true; // not used
	}

	protected ImageDataCollector createImageDataCollector(ImageNotifierSupport imageNotifierSupport)
	{
		return new ImageDataCollector(imageNotifierSupport);
	}
}
