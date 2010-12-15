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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.servoy.eclipse.core.I18NChangeListener;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.VisualFormEditor;

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
						if (editor instanceof VisualFormEditor)
						{
							((VisualFormEditor)editor).refreshGraphicalEditor();
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
		ServoyModelManager.getServoyModelManager().getServoyModel().removeI18NChangeListener(i18nChangeListener);
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
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path)
	{
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}


	/**
	 * Get an image with the given name from this plugin's bundle.
	 * 
	 * @param name the name of the image file.
	 * @return the image descriptor for the file.
	 */
	public static ImageDescriptor loadImageDescriptorFromBundle(String name)
	{
		return getImageDescriptor("$nl$/icons/" + name);
	}

}
