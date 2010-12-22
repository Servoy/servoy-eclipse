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

import java.awt.Color;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.eclipse.swt.graphics.ImageData;

import com.servoy.eclipse.core.DesignApplication;
import com.servoy.eclipse.core.DesignComponentFactory;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;

/**
 * Handles painting of form editor elements using awt printing.
 * 
 * @author rgansevles
 */

public class PersistImageNotifier implements IImageNotifier, IImageListener
{
	private final IApplication application;
	private final IPersist persist;
	private final Form form;
	private ImageData imageData;
	private JLabel label = null; // for painting
	private volatile boolean isWaitingStart = false;

	ImageNotifierSupport imSupport;

	public PersistImageNotifier(IApplication application, IPersist persist, Form form)
	{
		this.application = application;
		this.persist = persist;
		this.form = form;
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
				// am already waiting to paint the persist, no need to paint again 
				return;
			}
			isWaitingStart = true;
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					try
					{
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
						FlattenedSolution editingFlattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(
							persist);
						isWaitingStart = false;
						Component comp = null;
						if (persist instanceof Bean)
						{
							Object beanInstance = DesignComponentFactory.getBeanDesignInstance(application, editingFlattenedSolution, (Bean)persist, form);
							if (beanInstance instanceof Component)
							{
								comp = (Component)beanInstance;
							}
						}
						if (comp == null)
						{
							comp = DesignComponentFactory.createDesignComponent(application, editingFlattenedSolution, persist, form);
						}

						label.add(comp);
						label.setSize(comp.getWidth(), comp.getHeight());
						comp.setLocation(0, 0); // paint in left-upper corner
						label.doLayout();

						if (persist instanceof AbstractBase)
						{
							Color background = null;
							if (comp instanceof JComponent && ((JComponent)comp).isOpaque() && comp.isBackgroundSet())
							{
								background = comp.getBackground();
							}
							((AbstractBase)persist).setRuntimeProperty(PersistPropertySource.LastPaintedBackgroundProperty, background);
							((AbstractBase)persist).setRuntimeProperty(PersistPropertySource.LastPaintedFontProperty, comp.getFont());
						}
						float alpha;
						if (comp.isVisible())
						{
							alpha = 1.0f;
						}
						else
						{
							// paint 'invisible' elements with some translucency.
							comp.setVisible(true);
							alpha = 0.5f;
						}
						new ImageDataCollector(imSupport).start(label, comp.getWidth(), comp.getHeight(), alpha);
					}
					catch (Exception e)
					{
						isWaitingStart = false;
						ServoyLog.logError(e);
						((DesignApplication)application).getEditLabel().remove(label); // normally done in imageChanged()
					}
				}
			});
		}
		else
		{
			imSupport.fireImageChanged(imageData);
		}
	}

	public void imageChanged(ImageData data)
	{
		imageData = data;
		if (label != null)
		{
			SwingUtilities.invokeLater(new Runnable()
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
}
