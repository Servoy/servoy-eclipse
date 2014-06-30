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
package com.servoy.eclipse.appserver;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.wicket.util.file.File;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.update.internal.ui.UpdateUI;
import org.osgi.framework.BundleContext;

import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.persistence.IRepositoryFactory;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.server.shared.IApplicationServerStarter;
import com.servoy.j2db.server.shared.IUserManagerFactory;
import com.servoy.j2db.server.shared.IWebClientSessionFactory;
import com.servoy.j2db.util.Debug;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin
{

	// The plug-in ID
	public static final String PLUGIN_ID = "com.servoy.eclipse.appserver"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

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
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception
	{
		plugin = null;

		IApplicationServerSingleton appServer = ApplicationServerSingleton.get();
		if (appServer != null)
		{
			appServer.doNativeShutdown();
			ApplicationServerSingleton.clear();
		}
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


	public void startAppServer(IRepositoryFactory repositoryFactory, IDebugClientHandler debugClientHandler, IWebClientSessionFactory webClientSessionFactory,
		IUserManagerFactory userManagerFactory) throws Exception
	{
		IApplicationServerSingleton applicationServer = null;
		synchronized (this)
		{
			if (ApplicationServerSingleton.get() != null)
			{
				// already started
				return;
			}

			IExtensionRegistry reg = Platform.getExtensionRegistry();
			IExtensionPoint ep = reg.getExtensionPoint(IApplicationServerStarter.EXTENSION_ID);
			IExtension[] extensions = ep.getExtensions();

			if (extensions == null || extensions.length == 0)
			{
				throw new RuntimeException("Could not find application server starter plugin (extension point " + IApplicationServerStarter.EXTENSION_ID + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (extensions.length > 1)
			{
				getLog().log(new Status(IStatus.WARNING, getBundle().getSymbolicName(), "Multiple application server starter plugins found (extension point " + //$NON-NLS-1$
					IApplicationServerStarter.EXTENSION_ID + ")")); //$NON-NLS-1$
			}
			IConfigurationElement[] ce = extensions[0].getConfigurationElements();
			if (ce == null || ce.length == 0)
			{
				throw new RuntimeException("Could not read application server starter plugin (extension point " + IApplicationServerStarter.EXTENSION_ID + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (ce.length > 1)
			{
				getLog().log(
					new Status(IStatus.WARNING, getBundle().getSymbolicName(), "Multiple extensions for server starter plugins found (extension point " + //$NON-NLS-1$
						IApplicationServerStarter.EXTENSION_ID + ")")); //$NON-NLS-1$
			}
			IApplicationServerStarter applicationServerStarter = (IApplicationServerStarter)ce[0].createExecutableExtension("class"); //$NON-NLS-1$
			if (applicationServerStarter == null)
			{
				throw new RuntimeException("Could not load application server starter plugin (extension point " + IApplicationServerStarter.EXTENSION_ID + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			applicationServer = applicationServerStarter.startApplicationServer(repositoryFactory, debugClientHandler, webClientSessionFactory,
				userManagerFactory);
			ApplicationServerSingleton.set(applicationServer);

		}
		if (applicationServer != null)
		{
			checkApplicationServerVersion(applicationServer);
		}
	}

	private void checkApplicationServerVersion(IApplicationServerSingleton applicationServer)
	{
		// check the app server dir
		final String appServerDir = applicationServer.getServoyApplicationServerDirectory();
		File j2dbLib = new File(appServerDir, "lib/j2db.jar"); //$NON-NLS-1$

		if (!j2dbLib.exists())
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					MessageDialog.openError(Display.getDefault().getActiveShell(), "No Servoy ApplicationServer found!", "No application server found at: " + //$NON-NLS-1$ //$NON-NLS-2$
						appServerDir + "\nPlease make sure that you installed Servoy Developer correctly"); //$NON-NLS-1$
				}
			});
		}
		else
		{
			try
			{
				URLClassLoader classLoader = new URLClassLoader(new URL[] { j2dbLib.toURL() }, ClassLoader.getSystemClassLoader());
				Class< ? > loadClass = classLoader.loadClass("com.servoy.j2db.ClientVersion"); //$NON-NLS-1$
				Method method = loadClass.getMethod("getReleaseNumber", new Class[0]); //$NON-NLS-1$
				Object o = method.invoke(null, new Object[0]);
				if (o instanceof Integer)
				{
					final int version = ((Integer)o).intValue();
					if (version > ClientVersion.getReleaseNumber())
					{
						Display.getDefault().asyncExec(new Runnable()
						{
							public void run()
							{
								MessageDialog.openError(Display.getDefault().getActiveShell(), "Servoy ApplicationServer version check", //$NON-NLS-1$
									"Application Server version (" + version + ") is higher than the developers (" + ClientVersion.getReleaseNumber() + //$NON-NLS-1$ //$NON-NLS-2$
										") \nPlease upgrade the developer Help->Check for updates"); //$NON-NLS-1$
							}
						});
					}
					if (version < ClientVersion.getReleaseNumber())
					{
						Display.getDefault().asyncExec(new Runnable()
						{
							public void run()
							{
								boolean upgrade = MessageDialog.openQuestion(
									Display.getDefault().getActiveShell(),
									"Servoy ApplicationServer version should be upgraded", "The ApplicationServers version (" + version + ") is lower than the Developers version (" + ClientVersion.getReleaseNumber() + ")\n Upgrade the ApplicationServer?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

								if (upgrade)
								{
									try
									{
										final int[] updatedToVersion = new int[] { 0 };
										WorkspaceJob job = new WorkspaceJob("Updating Servoy ApplicationServer")
										{
											@Override
											public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException
											{
												try
												{
													monitor.beginTask("Updating...", IProgressMonitor.UNKNOWN);
													updatedToVersion[0] = ApplicationServerSingleton.get().updateAppServerFromSerclipse(
														new File(appServerDir).getParentFile(), version, ClientVersion.getReleaseNumber(), new ActionListener()
														{
															public void actionPerformed(ActionEvent e)
															{
																monitor.worked(1);
															}
														});
												}
												catch (Exception e)
												{
													getLog().log(new Status(IStatus.ERROR, getBundle().getSymbolicName(), "Unexpected error", e));
												}
												return Status.OK_STATUS;
											}
										};
										job.setUser(true);
										job.schedule();
										job.addJobChangeListener(new JobChangeAdapter()
										{
											@Override
											public void done(IJobChangeEvent event)
											{
												if (updatedToVersion[0] < ClientVersion.getReleaseNumber())
												{
													Display.getDefault().asyncExec(new Runnable()
													{
														public void run()
														{
															MessageDialog.openError(new Shell(), "Servoy update problem", //$NON-NLS-1$
																"Servoy ApplicationServer didn't update, please shutdown developer and to try run the updater on the command line"); //$NON-NLS-1$
														}
													});
												}
												else
												{
													Display.getDefault().asyncExec(new Runnable()
													{
														public void run()
														{
															UpdateUI.requestRestart(true);
														}
													});
												}
											}
										});
									}
									catch (Exception e)
									{
										Debug.error(e);
									}
								}
							}
						});
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				getLog().log(new Status(IStatus.ERROR, getBundle().getSymbolicName(), "Unexpected exception", e));
			}
		}
	}
}
