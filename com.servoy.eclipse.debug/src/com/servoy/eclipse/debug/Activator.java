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
package com.servoy.eclipse.debug;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.dltk.ui.editor.IReconcile;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.Utils;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin implements IStartup
{
	// The plug-in ID
	public static final String PLUGIN_ID = "com.servoy.eclipse.debug";

	// The shared instance
	private static Activator plugin;

	private final List<Image> imageList = Collections.synchronizedList(new ArrayList<Image>());

	/**
	 * The constructor
	 */
	public Activator()
	{
	}

	@Override
	public void start(BundleContext context) throws Exception
	{
		ModelUtils.assertUINotDisabled(PLUGIN_ID);

		super.start(context);
		plugin = this;

		if (Utils.getPlatform() == Utils.PLATFORM_LINUX &&
			"com.sun.java.swing.plaf.gtk.GTKLookAndFeel".equals(System.getProperty("swing.defaultlaf", "com.sun.java.swing.plaf.gtk.GTKLookAndFeel")))
		{
			// GTK LaF causes crashes or hangs on linux in developer
			System.setProperty("swing.defaultlaf", "javax.swing.plaf.metal.MetalLookAndFeel");
		}
		// this is an hack to force a reconcile on a javascript text editor.
		// so that after loading of the full solution the editor doesn't show warnings for references that where not there when the active solution wasn't loaded yet.
		ServoyModelManager.getServoyModelManager().getServoyModel().addDoneListener(() -> {
			try
			{
				IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
				if (editor instanceof IReconcile)
				{
					((IReconcile)editor).reconcile();
				}
			}
			catch (Exception e)
			{
			}
		});
	}


	@Override
	public void stop(BundleContext context) throws Exception
	{
		plugin = null;
		super.stop(context);
		for (Image image : imageList)
		{
			image.dispose();
		}
		imageList.clear();
	}


	public void registerImage(Image image)
	{
		if (image != null) imageList.add(image);
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
	 * @see org.eclipse.ui.IStartup#earlyStartup()
	 */
	public void earlyStartup()
	{
	}

	public List<ShortcutDefinition> getDebugSmartclientShortcuts()
	{
		String extensionId = PLUGIN_ID + ".shortcuts";

		IExtension[] extensions = Platform.getExtensionRegistry().getExtensionPoint(extensionId).getExtensions();

		List<ShortcutDefinition> shortcuts = new ArrayList<Activator.ShortcutDefinition>();
		if (extensions != null)
		{
			for (IExtension ext : extensions)
			{
				IConfigurationElement[] ce = ext.getConfigurationElements();
				if (ce != null)
				{
					for (IConfigurationElement el : ce)
					{
						try
						{
							String name = el.getAttribute("name");
							String keystroke = el.getAttribute("keystroke");
							Action action = (Action)el.createExecutableExtension("class");
							shortcuts.add(new ShortcutDefinition(name, keystroke, action));
						}
						catch (CoreException e)
						{
							ServoyLog.logError("Error reading extension point " + extensionId, e);
						}
					}
				}
			}
		}
		return shortcuts;
	}

	public static class ShortcutDefinition
	{

		public final String name;
		public final String keystroke;
		public final Action action;

		public ShortcutDefinition(String name, String keystroke, Action action)
		{
			this.name = name;
			this.keystroke = keystroke;
			this.action = action;
		}

	}
}
