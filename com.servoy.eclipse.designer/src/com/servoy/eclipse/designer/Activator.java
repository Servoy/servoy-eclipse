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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.util.BundleUtility;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.sablo.websocket.IWebsocketSession;
import org.sablo.websocket.IWebsocketSessionFactory;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.eclipse.core.I18NChangeListener;
import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.server.ngclient.WebsocketSessionFactory;
import com.servoy.j2db.server.ngclient.design.DesignNGClient;
import com.servoy.j2db.server.ngclient.design.DesignNGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.design.IDesignerSolutionProvider;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin
{
	// The plug-in ID
	public static final String PLUGIN_ID = "com.servoy.eclipse.designer";
	public static final String SHOW_DATA_IN_ANGULAR_DESIGNER = "showDataInDesigner";

	// The shared instance
	private static Activator plugin;

	private I18NChangeListener i18nChangeListener;

	private final Map<String, ImageIcon> imageIcons = new HashMap<String, ImageIcon>();

	private DesignNGClient client = null;

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
		ModelUtils.assertUINotDisabled(PLUGIN_ID);

		super.start(context);
		plugin = this;
		ServoyModelManager.getServoyModelManager().getServoyModel().addI18NChangeListener(i18nChangeListener = new I18NChangeListener()
		{
			public void i18nChanged()
			{
				com.servoy.eclipse.core.Activator.getDefault().getDesignClient().refreshI18NMessages();

				if (client != null)
				{
					client.refreshI18NMessages();
				}

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

		ServoyModelManager.getServoyModelManager().getServoyModel().addActiveProjectListener(new IActiveProjectListener.ActiveProjectListener()
		{
			@Override
			public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
			{
				com.servoy.eclipse.model.Activator.getDefault().getDesignClient();
				if (client != null)
				{
					if (toProject != null && toProject.getProject().getName().startsWith("import_placeholder") && client.isSolutionLoaded())
					{
						client.closeSolution(true, null);
					}
					client.shutDown(true);
				}
				return true;
			}
		});

		if (ApplicationServerRegistry.getServiceRegistry() != null)
		{
			IDebugClientHandler service = ApplicationServerRegistry.getServiceRegistry().getService(IDebugClientHandler.class);
			if (service != null)
			{
				WebsocketSessionManager.setWebsocketSessionFactory(WebsocketSessionFactory.DESIGN_ENDPOINT, new IWebsocketSessionFactory()
				{
					@Override
					public IWebsocketSession createSession(String uuid) throws Exception
					{
						DesignNGClientWebsocketSession designerSession = new DesignNGClientWebsocketSession(uuid)
						{
							@Override
							public void init() throws Exception
							{
								if (getClient() == null)
								{
									setClient(client = new DesignNGClient(this,
										ApplicationServerRegistry.getServiceRegistry().getService(IDesignerSolutionProvider.class),
										getPreferenceStore().contains(SHOW_DATA_IN_ANGULAR_DESIGNER)
											? getPreferenceStore().getBoolean(SHOW_DATA_IN_ANGULAR_DESIGNER) : true));
								}
							}
						};

						return designerSession;
					}
				});
			}
		}
	}

	public void toggleShowData()
	{
		if (client != null)
		{
			boolean showData = client.getShowData();
			showData = !showData;
			client.setShowData(showData);
			getPreferenceStore().putValue(SHOW_DATA_IN_ANGULAR_DESIGNER, String.valueOf(showData));
			try
			{
				((IPersistentPreferenceStore)getPreferenceStore()).save();
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}
		}
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
