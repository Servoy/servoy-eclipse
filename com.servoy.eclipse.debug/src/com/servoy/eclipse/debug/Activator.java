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


import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import javax.swing.Action;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.sablo.specification.WebComponentPackage;
import org.sablo.specification.WebComponentPackage.DirPackageReader;
import org.sablo.specification.WebComponentPackage.IPackageReader;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IPersistChangeListener;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.design.IDesignerSolutionProvider;
import com.servoy.j2db.server.ngclient.startup.resourceprovider.ResourceProvider;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
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

	private final Map<String, IPackageReader> componentReaders = new HashMap<String, IPackageReader>();
	private final Map<String, IPackageReader> serviceReaders = new HashMap<String, IPackageReader>();

	private IResourceChangeListener resourceChangeListener;

	private IActiveProjectListener activeProjectListenerForRegisteringResources;
	private Job registerResourcesJob;

	/**
	 * The constructor
	 */
	public Activator()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
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

		registerResources(null);

		resourceChangeListener = new IResourceChangeListener()
		{
			@Override
			public void resourceChanged(IResourceChangeEvent event)
			{
				ServoyResourcesProject activeResourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
				if (activeResourcesProject != null)
				{
					IProject resourceProject = activeResourcesProject.getProject();
					IResourceDelta delta = event.getDelta();
					IResourceDelta[] affectedChildren = delta.getAffectedChildren();
					boolean refreshServices = shouldRefresh(resourceProject, affectedChildren, SolutionSerializer.SERVICES_DIR_NAME);
					boolean refreshComponents = shouldRefresh(resourceProject, affectedChildren, SolutionSerializer.COMPONENTS_DIR_NAME);
					if (refreshServices || refreshComponents)
					{
						registerResources(refreshServices && refreshComponents ? null : (refreshComponents ? Boolean.TRUE : Boolean.FALSE));
					}
				}
			}

			/**
			 * @param resourceProject
			 * @param affectedChildren
			 */
			private boolean shouldRefresh(IProject resourceProject, IResourceDelta[] affectedChildren, String parentDir)
			{
				for (IResourceDelta rd : affectedChildren)
				{
					if (rd.getFlags() == IResourceDelta.MARKERS) continue;
					IResource resource = rd.getResource();
					if (resourceProject.equals(resource.getProject()))
					{
						IPath path = resource.getProjectRelativePath();
						if (path.segmentCount() > 1)
						{
							if (path.segment(0).equals(parentDir))
							{
								if (path.segmentCount() == 2 && resource instanceof IFile)
								{
									// a zip is changed refresh
									return true;
								}
								else if (path.lastSegment().equalsIgnoreCase("MANIFEST.MF") || path.lastSegment().toLowerCase().endsWith(".spec"))
								{
									return true;
								}
							}
						}
						if (path.segmentCount() == 0 || (path.segmentCount() > 0 && path.segment(0).equals(parentDir)))
						{
							if (shouldRefresh(resourceProject, rd.getAffectedChildren(), parentDir))
							{
								return true;
							}
						}
					}
				}
				return false;
			}
		};

		ApplicationServerRegistry.getServiceRegistry().registerService(IDesignerSolutionProvider.class, new IDesignerSolutionProvider()
		{
			@Override
			public Solution getActiveEditingSolution()
			{
				return ServoyModelFinder.getServoyModel().getActiveProject().getEditingSolution();
			}

			@Override
			public Solution getEditingSolution(String name)
			{
				ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(name);
				if (servoyProject != null) return servoyProject.getEditingSolution();
				return null;
			}

			@Override
			public void addPersistListener(IPersistChangeListener listener)
			{
				((ServoyModel)ServoyModelFinder.getServoyModel()).addPersistChangeListener(false, listener);
			}

			@Override
			public void removePersistListener(IPersistChangeListener listener)
			{
				((ServoyModel)ServoyModelFinder.getServoyModel()).removePersistChangeListener(false, listener);

			}
		});
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
	}

	/**
	 * @param activeResourcesProject
	 */
	private void registerResources(final Boolean component)
	{
		if (registerResourcesJob == null)
		{
			registerResourcesJob = new Job("registering resources")
			{
				@Override
				public IStatus run(IProgressMonitor monitor)
				{
					try
					{
						if (activeProjectListenerForRegisteringResources == null)
						{
							activeProjectListenerForRegisteringResources = new IActiveProjectListener()
							{
								public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
								{
									return true;
								}

								public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
								{
									// todo maybe fush on certain things?
								}

								public void activeProjectChanged(ServoyProject activeProject)
								{
									registerResources(null);
								}
							};
							((ServoyModel)ServoyModelFinder.getServoyModel()).addActiveProjectListener(activeProjectListenerForRegisteringResources);
						}
						ServoyResourcesProject activeResourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
						if (activeResourcesProject != null)
						{
							if (!Boolean.FALSE.equals(component))
							{
								if (componentReaders.size() > 0)
								{
									ResourceProvider.refreshComponentResources(componentReaders.values());
									componentReaders.clear();
								}
								componentReaders.putAll(readDir(monitor, activeResourcesProject, SolutionSerializer.COMPONENTS_DIR_NAME));
								ResourceProvider.addComponentResources(componentReaders.values());
							}

							if (component == null || Boolean.FALSE.equals(component))
							{
								if (serviceReaders.size() > 0)
								{
									ResourceProvider.refreshServiceResources(serviceReaders.values());
									serviceReaders.clear();
								}
								serviceReaders.putAll(readDir(monitor, activeResourcesProject, SolutionSerializer.SERVICES_DIR_NAME));

								ResourceProvider.addServiceResources(serviceReaders.values());
							}

							com.servoy.eclipse.core.Activator.getDefault().webResourcesChanged(component);
						}
					}
					finally
					{
						registerResourcesJob = null;
					}
					return Status.OK_STATUS;
				}

				/**
				 * @param monitor
				 * @param activeResourcesProject
				 */
				private Map<String, IPackageReader> readDir(IProgressMonitor monitor, ServoyResourcesProject activeResourcesProject, String folderName)
				{
					Map<String, IPackageReader> readers = new HashMap<String, IPackageReader>();
					IFolder folder = activeResourcesProject.getProject().getFolder(folderName);
					if (folder.exists())
					{
						try
						{
							folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
							IResource[] members = folder.members();
							for (IResource resource : members)
							{
								String name = resource.getName();
								int index = name.lastIndexOf('.');
								if (index != -1)
								{
									name = name.substring(0, index);
								}
								if (resource instanceof IFolder)
								{
									if (((IFolder)resource).getFile("META-INF/MANIFEST.MF").exists())
									{
										readers.put(name, new FolderPackageReader(new File(resource.getRawLocationURI()), (IFolder)resource));
									}
								}
								else if (resource instanceof IFile)
								{
									readers.put(name, new WebComponentPackage.JarPackageReader(new File(resource.getRawLocationURI())));
								}
							}
						}
						catch (CoreException e)
						{
							ServoyLog.logError(e);
						}
					}
					return readers;
				}
			};
			registerResourcesJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
			registerResourcesJob.schedule();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception
	{
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		ResourceProvider.removeComponentResources(componentReaders.values());
		ResourceProvider.removeServiceResources(serviceReaders.values());
		if (activeProjectListenerForRegisteringResources != null) ((ServoyModel)ServoyModelFinder.getServoyModel()).removeActiveProjectListener(activeProjectListenerForRegisteringResources);
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

	public static class FolderPackageReader extends DirPackageReader
	{
		private final static String MARKER = "com.servoy.eclipse.debug.SPEC_READ_MARKER";
		private final IFolder folder;

		public FolderPackageReader(File dir, IFolder folder)
		{
			super(dir);
			this.folder = folder;
		}

		@Override
		public Manifest getManifest() throws IOException
		{
			IFile file = folder.getFile("META-INF/MANIFEST.MF");
			try
			{
				file.deleteMarkers(MARKER, false, IResource.DEPTH_ONE);
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
			try
			{
				return super.getManifest();
			}
			catch (IOException ex)
			{
				try
				{
					IMarker marker = file.createMarker(MARKER);
					marker.setAttribute(IMarker.MESSAGE, ex.getMessage());
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
					marker.setAttribute(IMarker.LOCATION, file.getLocation().toString());
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
				throw ex;
			}
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.sablo.specification.WebComponentPackage.DirPackageReader#readTextFile(java.lang.String, java.nio.charset.Charset)
		 */
		@Override
		public String readTextFile(String path, Charset charset) throws IOException
		{
			IFile file = folder.getFile(path);
			if (file != null && file.exists())
			{
				try
				{
					file.deleteMarkers(MARKER, false, IResource.DEPTH_ONE);
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
			}
			return super.readTextFile(path, charset);
		}

		@Override
		public void reportError(String specpath, Exception e)
		{
			super.reportError(specpath, e);
			IFile file = folder.getFile(specpath);
			if (file != null)
			{
				try
				{
					IMarker marker = file.createMarker(MARKER);
					marker.setAttribute(IMarker.MESSAGE, e.getMessage());
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
					marker.setAttribute(IMarker.LOCATION, file.getLocation().toString());
				}
				catch (CoreException ex)
				{
					ServoyLog.logError(ex);
				}
			}
		}

	}


}
