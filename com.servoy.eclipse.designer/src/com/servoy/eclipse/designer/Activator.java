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
package com.servoy.eclipse.designer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.util.BundleUtility;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.servoy.eclipse.core.I18NChangeListener;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin
{

	// The plug-in ID
	public static final String PLUGIN_ID = "com.servoy.eclipse.designer";

	// The shared instance
	private static Activator plugin;

	private I18NChangeListener i18nChangeListener;

	private final Map<String, ImageIcon> imageIcons = new HashMap<String, ImageIcon>();

	/**
	 * The constructor
	 */
	public Activator()
	{
	}

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
		ServoyModelManager.getServoyModelManager().getServoyModel().addI18NChangeListener(i18nChangeListener = new I18NChangeListener()
		{
			public void i18nChanged()
			{
				com.servoy.eclipse.core.Activator.getDefault().getDesignClient().refreshI18NMessages();

				if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null &&
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null)
				{
					IEditorReference[] editorRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
					for (IEditorReference editorRef : editorRefs)
					{
						IEditorPart editor = editorRef.getEditor(false);
						if (editor instanceof BaseVisualFormEditor)
						{
							((BaseVisualFormEditor)editor).refreshGraphicalEditor();
						}
					}
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception
	{
		plugin = null;
		if (ServoyModelManager.getServoyModelManager().isServoyModelCreated())
		{
			ServoyModelManager.getServoyModelManager().getServoyModel().removeI18NChangeListener(i18nChangeListener);
		}
		imageIcons.clear();
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault()
	{
		return plugin;
	}

	/**
	 * Get an image with the given name from this plugin's bundle.
	 * 
	 * @param name the name of the image file.
	 * @return the image descriptor for the file.
	 */
	public static ImageDescriptor loadImageDescriptorFromBundle(String name)
	{
		return imageDescriptorFromPlugin(PLUGIN_ID, "$nl$/icons/" + name);
	}

	/**
	 * Get an swing image icon with the given name from this plugin's bundle.
	 * 
	 * @param name the name of the image file.
	 * @return the image icon for the file.
	 */
	public ImageIcon loadImageIconFromBundle(String name)
	{
		if (name == null)
		{
			throw new IllegalArgumentException();
		}

		ImageIcon imageIcon = imageIcons.get(name);
		if (imageIcon == null)
		{
			// if the bundle is not ready then there is no image
			Bundle bundle = Platform.getBundle(PLUGIN_ID);
			if (!BundleUtility.isReady(bundle))
			{
				return null;
			}

			String imageFilePath = "$nl$/icons/" + name;
			// look for the image (this will check both the plugin and fragment folders
			URL fullPathString = BundleUtility.find(bundle, imageFilePath);

			if (fullPathString == null)
			{
				try
				{
					fullPathString = new URL(imageFilePath);
				}
				catch (MalformedURLException e)
				{
					return null;
				}
			}

			imageIcons.put(name, imageIcon = new ImageIcon(fullPathString, fullPathString.toExternalForm().intern()));
		}
		return imageIcon;
	}

}
