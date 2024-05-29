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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.internal.ui.SearchPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.BackingStoreException;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.BrowserDialog;
import com.servoy.eclipse.ui.dialogs.ServoyLoginDialog;
import com.servoy.eclipse.ui.preferences.StartupPreferences;
import com.servoy.eclipse.ui.tweaks.IconPreferences;
import com.servoy.eclipse.ui.util.IAutomaticImportWPMPackages;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.AddAsWebPackageAction;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator extends AbstractUIPlugin
{
	/**
	 *
	 */
	public static final String CLOUD_BASE_URL = System.getProperty("servoy.cloud_base.url", "https://admin.servoy-cloud.eu");
	public static final String TUTORIALS_URL = CLOUD_BASE_URL + "/solution/developerWelcome?servoyVersion=" + ClientVersion.getPureVersion() + "&loginToken=";

	/**
	 * The PLUGIN_ID for com.servoy.eclipse.ui.
	 */
	public static final String PLUGIN_ID = "com.servoy.eclipse.ui";

	// The shared instance
	private static Activator plugin;

	private final ISharedTextColors sharedTextColors = new SharedTextColors();

	/**
	 * The path to icons used by this view (relative to the plug-in folder).
	 */
	public static final String ICONS_PATH = "$nl$/icons";
	public static final String DARK_ICONS_PATH = "$nl$/darkicons";
	public static final String DARK_ICONS_FOLDER = "/darkicons/";

	private final static Map<String, ImageDescriptor> imageDescriptorCache = new ConcurrentHashMap<>();

	private final Map<String, Image> imageCacheOld = new HashMap<String, Image>();

	private final Map<String, Image> imageCacheBundle = new HashMap<String, Image>();

	private final Map<String, Image> grayCacheBundle = new HashMap<String, Image>();

	private static BundleContext context;

	private static final String SERVOY_VERSION = "servoy_version";

	@Override
	public void start(BundleContext context) throws Exception
	{
		ModelUtils.assertUINotDisabled(PLUGIN_ID);

		super.start(context);
		plugin = this;
		Activator.context = context;

		//replace Eclipse default text search query provider with Servoy's
		SearchPlugin.getDefault().getPreferenceStore().putValue(SearchPreferencePage.TEXT_SEARCH_QUERY_PROVIDER, "com.servoy.eclipse.ui.search.textSearch");

		// make sure that core is fully initialized; this should also make sure app. server is initialised
		com.servoy.eclipse.core.Activator.getDefault();
		com.servoy.eclipse.ngclient.ui.Activator.getInstance().extractNode();

		ServoyModelManager.getServoyModelManager().getServoyModel().addActiveProjectListener(new IActiveProjectListener()
		{

			@Override
			public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
			{
				return true;
			}

			@Override
			public void activeProjectChanged(final ServoyProject activeProject)
			{
				if (activeProject != null)
				{
					WorkspaceJob findMissingSpecs = new WorkspaceJob("Look for missing component/layout specifications")
					{
						@Override
						public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
						{
							ServoyProject[] modules = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
							final Map<String, Set<String>> processedPackages = new HashMap<>();
							SpecProviderState componentSpecProvider = WebComponentSpecProvider.getSpecProviderState();
							for (final ServoyProject module : modules)
							{
								module.getSolution().acceptVisitor(new IPersistVisitor()
								{

									@Override
									public Object visit(IPersist o)
									{
										String missingPackage = null;
										if (o instanceof WebComponent && ((WebComponent)o).getTypeName() != null)
										{
											WebObjectSpecification spec = componentSpecProvider.getWebObjectSpecification(((WebComponent)o).getTypeName());
											if (spec == null)
											{
												// see if package is there or not; the component is not present
												String packageName = ((WebComponent)o).getTypeName().split("-")[0];
												if (componentSpecProvider.getPackageReader(packageName) == null)
												{
													missingPackage = packageName;
												} // else the component is not there but the package is there; this can happen for example if you use a project package
													// and you edit a spec file and save it with errors; that results in a component that is not recognized, but is there with errors
													// and you also have and error problem marker or more for that; in this case we shouldn't offer to import the package automatically
											}
										}
										if (o instanceof LayoutContainer)
										{
											PackageSpecification<WebLayoutSpecification> pkg = componentSpecProvider.getLayoutSpecifications()
												.get(((LayoutContainer)o).getPackageName());
											if (pkg == null)
											{
												// see if package is there or not; the layout is not present
												String packageName = ((LayoutContainer)o).getPackageName();
												if (componentSpecProvider.getPackageReader(packageName) == null)
												{
													missingPackage = packageName;
												} // else the layout is not there but the package is there; this can happen for example if you use a project package
													// and you edit a spec file and save it with errors; that results in a layout that is not recognized, but is there with errors
													// and you also have and error problem marker or more for that; in this case we shouldn't offer to import the package automatically
											}
										}
										if (missingPackage != null)
										{
											Set<String> list = processedPackages.get(missingPackage);
											if (list == null)
											{
												list = new TreeSet<>();
												processedPackages.put(missingPackage, list);
											}
											list.add(module.getSolution().getName());
										}
										return null;
									}
								});
							}
							if (processedPackages.size() > 0)
							{
								Display.getDefault().syncExec(new Runnable()
								{

									@Override
									public void run()
									{
										try
										{
											Shell active = new Shell(PlatformUI.getWorkbench().getDisplay().getActiveShell(), SWT.PRIMARY_MODAL);
											ArrayList<IProject> packagesSource = new ArrayList<IProject>();
											List<String> packagesHelp = new ArrayList<String>();
											List<String> packagesFinal = new ArrayList<String>();
											List<String> packagesNeeded = processedPackages.entrySet().stream().map(Map.Entry::getKey)
												.collect(Collectors.toList());
											IProject[] allProjects = ServoyModel.getWorkspace().getRoot().getProjects();
											if (allProjects.length > 0)
											{
												for (IProject project : allProjects)
												{
													if (project.isAccessible() && project.hasNature(ServoyNGPackageProject.NATURE_ID))
													{
														String packageName = project.getName();
														if (packagesNeeded.contains(packageName) ||
															(packageName.equals("servoy-extra-components") && packagesNeeded.contains("servoyextra")))
														{
															packagesSource.add(project);
															packagesHelp.add(packageName.equals("servoy-extra-components") ? "servoyextra" : packageName);
														}
													}
												}
												for (String packageName : packagesNeeded)
												{
													packagesFinal.add(packagesHelp.contains(packageName)
														? (packageName +
															" (existing source package project will be used; checking this will download the wpm package)")
														: packageName);
												}
											}
											if (packagesFinal.size() == 0)
											{
												packagesFinal = packagesNeeded;
											}
											final ListSelectionDialog lsd = new ListSelectionDialog(active, packagesFinal, new ArrayContentProvider(),
												new LabelProvider(),
												"The packages listed below are missing from the active solution and it's modules.\nPlease select the ones you want to download using Servoy Package Manager:");
											lsd.setInitialElementSelections(packagesNeeded);
											lsd.setBlockOnOpen(true);
											lsd.setTitle("Solution needs packages, install them from wpm/source");
											int pressedButton = lsd.open();
											if (pressedButton == 0)
											{
												List<IAutomaticImportWPMPackages> defaultImports = ModelUtils.getExtensions(
													IAutomaticImportWPMPackages.EXTENSION_ID);
												List<String> result = Arrays.asList(lsd.getResult()).stream().map(object -> Objects.toString(object, null))
													.toList();
												for (String packageName : packagesFinal)
												{
													String realPackageName = packageName.substring(0,
														packageName.indexOf(" (") != -1 ? packageName.indexOf(" (") : packageName.length());
													if (result.contains(packageName))
													{
														if (defaultImports != null && defaultImports.size() > 0)
														{
															defaultImports.get(0).importPackage(realPackageName);
														}
													}
													else if (packagesSource.size() > 0)
													{
														String solutionName = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution()
															.getName();
														IProject solutionProject = ServoyModel.getWorkspace().getRoot().getProject(solutionName);
														IProjectDescription solutionProjectDescription = solutionProject.getDescription();
														for (IProject project : packagesSource)
														{
															String packName = project.getName();
															if (packName.equals(realPackageName) ||
																(packName.equals("servoy-extra-components") && realPackageName.equals("servoyextra")))
															{
																AddAsWebPackageAction.addReferencedProjectToDescription(project, solutionProjectDescription);
															}
														}
														solutionProject.setDescription(solutionProjectDescription, new NullProgressMonitor());
													}
												}
											}
										}
										catch (CoreException e)
										{
											ServoyLog.logError(e);
										}
									}
								});
							}
							return Status.OK_STATUS;
						}
					};
					findMissingSpecs.setRule(ServoyModel.getWorkspace().getRoot());
					findMissingSpecs.schedule();
				}

			}

			@Override
			public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
			{

			}
		});

		ServoyModelManager.getServoyModelManager().getServoyModel()
			.addDoneListener(() -> com.servoy.eclipse.core.Activator.getDefault().addPostgressCheckedCallback(() -> {
				EclipseCSSThemeListener.getInstance().initThemeListener();
				if (!PlatformUI.getWorkbench().isClosing())
				{
					showLoginAndStart();
				}
			}));
	}


	/**
	 *
	 */
	public void showLoginAndStart()
	{
		try
		{
			//wait some more time for the progress information dialog from solution model to close
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
			ServoyLog.logError(e);
		}
		Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				Shell activeShell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
				if (activeShell == null)
				{
					Shell[] shells = PlatformUI.getWorkbench().getDisplay().getShells();
					for (int i = shells.length; --i >= 0;)
					{
						if (shells[i].getParent() == null && shells[i].isVisible())
						{
							activeShell = shells[i];
							break;
						}
					}
					if (activeShell == null)
					{
						Display.getDefault().asyncExec(this);
						return;
					}
				}
				while (activeShell.getParent() instanceof Shell && activeShell.getParent().isVisible())
				{
					activeShell = (Shell)activeShell.getParent();
				}
				boolean emptyWorkspace = ResourcesPlugin.getWorkspace().getRoot().getProjects().length == 0;
				//new ServoyLoginDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell()).clearSavedInfo();
				String username = null;
				try
				{
					username = SecurePreferencesFactory.getDefault()
						.node(ServoyLoginDialog.SERVOY_LOGIN_STORE_KEY)
						.get(ServoyLoginDialog.SERVOY_LOGIN_USERNAME, null);
				}
				catch (StorageException e)
				{
					ServoyLog.logError(e);
				}
				String loginToken = new ServoyLoginDialog(activeShell).doLogin();
				if (loginToken != null)
				{
					ISecurePreferences node = SecurePreferencesFactory.getDefault().node(ServoyLoginDialog.SERVOY_LOGIN_STORE_KEY);
					try
					{
						com.servoy.eclipse.cloud.Activator.getDefault().checkoutFromCloud(loginToken, node.get(ServoyLoginDialog.SERVOY_LOGIN_USERNAME, null),
							node.get(ServoyLoginDialog.SERVOY_LOGIN_PASSWORD, null));
					}
					catch (StorageException e)
					{
						ServoyLog.logError(e);
					}
				}
				// only show if first login or is not disabled from preferences
				if (username == null || Utils.getAsBoolean(Settings.getInstance().getProperty(StartupPreferences.STARTUP_SHOW_START_PAGE, "true")))
				{
					BrowserDialog dialog = new BrowserDialog(activeShell,
						TUTORIALS_URL + loginToken + "&emptyWorkspace=" + emptyWorkspace, true, true);
					dialog.open(true);
				}

			}
		};
		Display.getDefault().asyncExec(runnable);
	}

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

		sharedTextColors.dispose();

		if (provisioningAgent != null)
		{
			provisioningAgent.stop();
		}

		EclipseCSSThemeListener.getInstance().removeListener();
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

	public Image loadImageFromBundle(final String name, final boolean disabled)
	{
		final String storeName;
		if (disabled)
		{
			storeName = name + "__DISABLED__";
		}
		else
		{
			storeName = name;
		}
		Image img = imageCacheBundle.get(storeName);
		if (img == null)
		{
			Display.getDefault().syncExec(new Runnable()
			{

				@Override
				public void run()
				{
					if (disabled)
					{
						// first just load the normal image from the cache (or create and store it in the cache so it will be disposed)
						Image img = loadImageFromBundle(name, false);
						if (img != null)
						{
							img = new Image(img.getDevice(), img, SWT.IMAGE_DISABLE);
							imageCacheBundle.put(storeName, img);
						}
					}
					else
					{
						ImageDescriptor id = Activator.loadImageDescriptorFromBundle(name);
						if (id != null)
						{
							Image img = id.createImage();
							if (img != null)
							{
								imageCacheBundle.put(storeName, img);
							}
						}
					}
				}
			});
			img = imageCacheBundle.get(storeName);
		}
		return img;
	}

	/**
	 * Loads the image from the bundle cache, so that it will be disposed.
	 * @param name
	 * @return
	 */
	public Image loadImageFromCache(String name)
	{
		return imageCacheBundle.get(name);
	}

	/**
	 * stores the given image to the cache, if there was a previous one with that name then that one will be disposed
	 * @param name
	 * @param image
	 */
	public void putImageInCache(String name, Image image)
	{
		Image prev = loadImageFromCache(name);
		if (prev != null) prev.dispose();
		imageCacheBundle.put(name, image);
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
			storeName = name + "__DISABLED__";
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
		ImageDescriptor id = ImageDescriptor.createFromFile(IApplication.class, "images/" + name);
		if (id.getImageData() != null)
		{
			neededDescriptor = id;
		}
		return neededDescriptor;
	}

	public static ImageDescriptor loadDefaultImageDescriptorFromBundle(String name)
	{
		return getImageDescriptor(ICONS_PATH + "/" + name);
	}

	/**
	 * Get an image with the given name from this plugin's bundle.
	 *
	 * @param name the name of the image file.
	 * @return the image descriptor for the file.
	 */
	public static ImageDescriptor loadImageDescriptorFromBundle(String name)
	{
		ImageDescriptor imageDescriptor = imageDescriptorCache.get(name);
		if (imageDescriptor == null)
		{
			imageDescriptor = getImageDescriptor(
				(IconPreferences.getInstance().getUseDarkThemeIcons() && darkIconExists(name) ? DARK_ICONS_PATH : ICONS_PATH) + "/" + name);
			if (imageDescriptor != null) imageDescriptorCache.put(name, imageDescriptor);
		}
		return imageDescriptor;
	}


	private static boolean darkIconExists(String name)
	{
		return context.getBundle().getEntry(DARK_ICONS_FOLDER + name) != null;
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

	/**
	 * Global (workspace) preferences
	 */
	public IEclipsePreferences getEclipsePreferences()
	{
		return InstanceScope.INSTANCE.getNode(PLUGIN_ID);
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
			ServiceReference< ? > sr = context.getServiceReference(IProvisioningAgentProvider.SERVICE_NAME);
			if (sr != null)
			{
				IProvisioningAgentProvider agentProvider = (IProvisioningAgentProvider)context.getService(sr);

				URI p2URI = new File(ApplicationServerRegistry.get().getServoyApplicationServerDirectory(), "../developer/p2/").toURI();
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
