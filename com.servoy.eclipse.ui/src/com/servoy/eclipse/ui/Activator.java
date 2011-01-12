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
package com.servoy.eclipse.ui;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator extends AbstractUIPlugin
{

	/**
	 * The PLUGIN_ID for com.servoy.eclipse.ui.
	 */
	public static final String PLUGIN_ID = "com.servoy.eclipse.ui"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private final ISharedTextColors sharedTextColors = new SharedTextColors();

	/**
	 * The path to icons used by this view (relative to the plug-in folder).
	 */
	public static final String ICONS_PATH = "$nl$/icons"; //$NON-NLS-1$

	private final Map<String, Image> imageCacheOld = new HashMap<String, Image>();

	private final Map<String, Image> imageCacheBundle = new HashMap<String, Image>();

	private final Map<String, Image> grayCacheBundle = new HashMap<String, Image>();

	private final Map<ImageDescriptor, Image> imageCacheDescriptor = new HashMap<ImageDescriptor, Image>();


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception
	{
		super.start(context);
		plugin = this;

		// make sure that core is fully initialized.
		com.servoy.eclipse.core.Activator.getDefault();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception
	{
		try
		{
			// save any prefs not saved yet
			getEclipsePreferences().flush();
		}
		catch (BackingStoreException e)
		{
			System.err.println(e);
		}

		Iterator<Image> it = imageCacheOld.values().iterator();
		while (it.hasNext())
		{
			it.next().dispose();
		}
		imageCacheOld.clear();

		it = imageCacheBundle.values().iterator();
		while (it.hasNext())
		{
			it.next().dispose();
		}
		imageCacheBundle.clear();

		it = grayCacheBundle.values().iterator();
		while (it.hasNext())
		{
			it.next().dispose();
		}
		grayCacheBundle.clear();

		it = imageCacheDescriptor.values().iterator();
		while (it.hasNext())
		{
			it.next().dispose();
		}
		imageCacheDescriptor.clear();

		sharedTextColors.dispose();

		if (provisioningAgent != null)
		{
			provisioningAgent.stop();
		}

		plugin = null;
		super.stop(context);
	}

	/**
	 * @return the sharedTextColors
	 */
	public ISharedTextColors getSharedTextColors()
	{
		return sharedTextColors;
	}

	public Image loadImageFromBundle(String name)
	{
		return loadImageFromBundle(name, false);
	}

	public Image loadImageFromBundle(String name, boolean disabled)
	{
		String storeName = name;
		if (disabled)
		{
			storeName = name + "__DISABLED__"; //$NON-NLS-1$
		}
		Image img = imageCacheBundle.get(storeName);
		if (img == null)
		{
			ImageDescriptor id = Activator.loadImageDescriptorFromBundle(name);
			if (id != null)
			{
				img = id.createImage();
				if (img != null)
				{
					if (disabled)
					{
						img = new Image(img.getDevice(), img, SWT.IMAGE_GRAY);
					}
					imageCacheBundle.put(storeName, img);
				}
			}
		}
		return img;
	}

	public Image getImage(ImageDescriptor imageDescriptor)
	{
		if (imageDescriptor == null) return null;

		Image image = imageCacheDescriptor.get(imageDescriptor);
		if (image == null)
		{
			image = imageDescriptor.createImage();
			imageCacheDescriptor.put(imageDescriptor, image);
		}
		return image;
	}

	public Image loadImageFromOldLocation(String name)
	{
		return loadImageFromOldLocation(name, false);
	}

	public Image loadImageFromOldLocation(String name, boolean disabled)
	{
		String storeName = name;
		if (disabled)
		{
			storeName = name + "__DISABLED__"; //$NON-NLS-1$
		}

		Image img = imageCacheOld.get(storeName);
		if (img == null)
		{
			ImageDescriptor id = Activator.loadImageDescriptorFromOldLocations(name);
			if (id != null)
			{
				img = id.createImage();
				if (img != null)
				{
					if (disabled)
					{
						img = new Image(img.getDevice(), img, SWT.IMAGE_GRAY);
					}
					imageCacheOld.put(storeName, img);
				}
			}
		}
		return img;
	}

	/**
	 * Tries to find the image with the specified file name in the old application image folder.
	 * 
	 * @param name the filename of the image to load.
	 * @return the loaded image descriptor or null if the image was not found.
	 */
	public static ImageDescriptor loadImageDescriptorFromOldLocations(String name)
	{
		ImageDescriptor neededDescriptor = null;
		ImageDescriptor id = ImageDescriptor.createFromFile(IApplication.class, "images/" + name); //$NON-NLS-1$
		if (id.getImageData() != null)
		{
			neededDescriptor = id;
		}
		return neededDescriptor;
	}

	/**
	 * Get an image with the given name from this plugin's bundle.
	 * 
	 * @param name the name of the image file.
	 * @return the image descriptor for the file.
	 */
	public static ImageDescriptor loadImageDescriptorFromBundle(String name)
	{
		return getImageDescriptor(ICONS_PATH + "/" + name);
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault()
	{
		return plugin;
	}

	public IEclipsePreferences getEclipsePreferences()
	{
		return new InstanceScope().getNode(PLUGIN_ID);
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path.
	 * 
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path)
	{
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	/**
	 * @param icon
	 * @return
	 */
	public Image createGrayImage(String name, Image icon)
	{
		Image gray = grayCacheBundle.get(name);
		if (gray == null)
		{
			gray = new Image(icon.getDevice(), icon, SWT.IMAGE_GRAY);
			grayCacheBundle.put(name, gray);
		}
		return gray;
	}

	private IProvisioningAgent provisioningAgent;

	public IProvisioningAgent getProvisioningAgent()
	{
		if (provisioningAgent == null)
		{
			BundleContext context = getBundle().getBundleContext();
			ServiceReference sr = context.getServiceReference(IProvisioningAgentProvider.SERVICE_NAME);
			if (sr != null)
			{
				IProvisioningAgentProvider agentProvider = (IProvisioningAgentProvider)context.getService(sr);

				URI p2URI = new File(ApplicationServerSingleton.get().getServoyApplicationServerDirectory(), "../developer/p2/").toURI(); //$NON-NLS-1$
				try
				{
					provisioningAgent = agentProvider.createAgent(p2URI);
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
		}

		return provisioningAgent;
	}
}
