/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.exporter.apps.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.exporter.apps.Activator;
import com.servoy.eclipse.model.DBITableLoader;
import com.servoy.eclipse.model.IPluginBaseClassLoaderProvider;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.repository.EclipseRepositoryFactory;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.persistence.ITableLoader;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.server.shared.IUserManagerFactory;
import com.servoy.j2db.server.starter.IServerStarter;
import com.servoy.j2db.util.Settings;

/**
 * Common implementation for workspace based exporter applications.
 *
 * @author acostescu
 */
public abstract class AbstractWorkspaceExporter<T extends IArgumentChest> implements IApplication, IBundleStopListener
{

	public final static Integer EXIT_STOPPED = Integer.valueOf(1);
	public final static Integer EXIT_EXPORT_FAILED = Integer.valueOf(2);
	public final static Integer EXIT_INVALID_ARGS = Integer.valueOf(3);

	protected boolean verbose;
	protected boolean mustStop;
	protected Integer exitCode;

	private final Object stopLock = new Object();
	private boolean finished;

	private boolean initialAutoBuild = false;

	public Object start(IApplicationContext context)
	{
		Activator.getDefault().addBundleStopListener(this);

		synchronized (stopLock)
		{
			finished = false;
		}

		try
		{
			mustStop = false;
			exitCode = EXIT_OK;
			ModelUtils.setUIDisabled(true);

			T configuration = createArgumentChest(context);
			if (configuration.isInvalid())
			{
				outputError(configuration.getHelpMessage());
				exitCode = EXIT_INVALID_ARGS;
			}
			else if (configuration.mustShowHelp())
			{
				output(configuration.getHelpMessage());
			}
			else
			{
				verbose = configuration.isVerbose();

				// load settings from given file if specified as command line argument
				if (configuration.getSettingsFileName() != null)
				{
					File f = new File(configuration.getSettingsFileName());
					if (f.exists())
					{
						try
						{
							outputExtra("Loading settings from: " + f.getAbsolutePath());
							Settings s = Settings.getInstance();
							s.loadFromFile(f);

							Activator.getDefault().getBundle().getBundleContext().registerService(Settings.class, s, null);
						}
						catch (IOException e)
						{
							ServoyLog.logError(e);
							outputError("Failed to load settings: " + e.getMessage() + ". Check workspace log.");
						}
					}
				}

				if (configuration.getAppServerDir() != null)
				{
					File f = new File(configuration.getAppServerDir());
					if (f.exists() && f.isDirectory())
					{
						// set correct application server dir
						Settings.getInstance().put(J2DBGlobals.SERVOY_APPLICATION_SERVER_DIRECTORY_KEY, f.getAbsolutePath());
					}
					else
					{
						outputError("Incorrect value for application server location. '" + configuration.getAppServerDir() +
							(!f.exists() ? "' does not exist." : " is not a directory"));
					}
				}

				// initialize app server
				initializeApplicationServer(configuration);

				if (ApplicationServerRegistry.get() != null)
				{
					initialAutoBuild = ResourcesPlugin.getWorkspace().isAutoBuilding();

					try
					{
						if (initialAutoBuild)
						{
							outputExtra("Temporarily disabling auto-build.");
							IWorkspaceDescription description = ResourcesPlugin.getWorkspace().getDescription();
							description.setAutoBuilding(false); // this doesn't actually apply
							try
							{
								ResourcesPlugin.getWorkspace().setDescription(description); // apply
							}
							catch (CoreException e)
							{
								ServoyLog.logError(e);
								// continuing would only lead to potential deadlock, if auto-build cannot be turned off
								outputError("EXPORT FAILED. Cannot export solution(s) '" + configuration.getSolutionNamesAsString() +
									"'; unable to turn off auto-build. Check workspace log.");
								exitCode = EXIT_EXPORT_FAILED;
							}
						}

						if (exitCode == EXIT_OK)
						{
							// set up eclipse messages
							Messages.customMessageLoader = new EclipseMessages();

							if (!mustStop)
							{
								// export
								checkAndExportSolutions(configuration);
							}
						}
					}
					finally
					{
						ApplicationServerRegistry.get().doNativeShutdown();
						try
						{
							ResourcesPlugin.getWorkspace().save(true, null);
						}
						catch (CoreException e)
						{
							ServoyLog.logError(e);
						}
					}
				}
			}
		}
		finally
		{
			synchronized (stopLock)
			{
				finished = true;
				stopLock.notify();
			}
		}
		if (exitCode.equals(EXIT_OK)) output("Export DONE.");
		return exitCode;
	}


	protected abstract T createArgumentChest(IApplicationContext context);

	public void bundleStopping(BundleContext context)
	{
		restoreAutoBuildIfNeeded();
	}

	private void restoreAutoBuildIfNeeded()
	{
		// the problem here is that when we try to restore, an auto-build will most likely be scheduled and we get an error in the log
		// that some jobs were still started when the workbench closed; so we try to execute this as late as possible (when bundle is stopped)
		if (initialAutoBuild)
		{
			outputExtra("Re-enabling auto-build.");
			try
			{
				// try to restore the auto-build flag without triggering a build that will load TypeCreate/TypeProvider & hang on instantiating ServoyModel
				IEclipsePreferences node = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
				node.putBoolean(ResourcesPlugin.PREF_AUTO_BUILDING, true);
				node.flush();
			}
			catch (BackingStoreException e)
			{
				ServoyLog.logError(e);
				outputError("Cannot restore auto-build flag. Check workspace log.");
			}
		}
	}

	protected void checkAndExportSolutions(T configuration)
	{
		List<IProject> importedProjects = new ArrayList<IProject>();
		List<IProject> existingClosedProjects = new ArrayList<IProject>();
		try
		{
			outputExtra("Importing existing projects into workspace and opening closed ones if needed. " +
				(configuration.shouldAggregateWorkspace() ? "(checking child folders for projects as well)" : ""));
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			File wr = workspaceRoot.getLocation().toFile();
			importExistingAndOpenClosedProjects(wr, workspaceRoot, importedProjects, existingClosedProjects);

			if (configuration.shouldAggregateWorkspace())
			{
				// also import existing projects in subfolders
				for (File f : FileUtils.listFilesAndDirs(wr, FalseFileFilter.INSTANCE, DirectoryFileFilter.DIRECTORY))
				{
					if (f.getAbsolutePath().contains(".metadata")) continue;
					importExistingAndOpenClosedProjects(f, workspaceRoot, importedProjects, existingClosedProjects);
				}
			}

			outputExtra("Refreshing projects.");
			IProject[] prjs = workspaceRoot.getProjects();
			try
			{
				for (IProject p : prjs)
				{
					if (p.isOpen() && p.exists())
					{
						p.refreshLocal(IResource.DEPTH_INFINITE, null);
					}
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
				outputError("Refresh project roots encountered a problem. Check workspace log.");
			}

			ExportServoyModel sm = ServoyModelProvider.getModel();

			String[] solutionNames = configuration.getSolutionNames();
			for (int i = 0; i < solutionNames.length && exitCode == EXIT_OK; i++)
			{
				String solutionName = solutionNames[i];

				outputExtra("Refreshing and loading projects used by solution " + solutionName + ".");
				sm.initialize(solutionName); // the actual refresh of solution projects happens in the modified EclipseRepository load; this just loads all modules (because it reloads all form security info)

				if (!mustStop)
				{
					if (sm.getActiveProject() != null && sm.getActiveResourcesProject() != null)
					{
						ServoyProject[] modules = sm.getModulesOfActiveProject();

						if (!mustStop)
						{
							// check project markers
							// for solution and/or (some) modules
							List<IMarker> errors = new ArrayList<IMarker>();
							List<IMarker> warnings = new ArrayList<IMarker>();
							if (configuration.skipBuild())
							{
								outputExtra("Servoy build is skipped due to given configuration; make sure your solution is correct when using this option.");
							}
							else
							{
								outputExtra("Checking for problem markers");
								sm.buildActiveProjects(null, true);

								checkProjectMarkers(modules, errors, warnings, configuration);

								// for resources project
								splitMarkers(sm.getActiveResourcesProject().getProject(), errors, warnings);
							}
							boolean returnDueToErrors = false;
							if (errors.size() > 0)
							{
								output("Found error markers in solution " + solutionName);

								if (configuration.shouldIgnoreBuildErrors())
								{
									for (IMarker marker : errors)
									{
										outputMarker(marker, true);
									}
									output("Ignoring error markers. ('-ie' was used)");
									if (!verbose) output("(use -verbose for more information)");
								}
								else
								{
									for (IMarker marker : errors)
									{
										outputMarker(marker, false);
									}
									outputError("EXPORT FAILED. Solution '" + solutionName + "' will NOT be exported. It has error markers.");
									exitCode = EXIT_EXPORT_FAILED;
									returnDueToErrors = true;
								}
							}

							if (!mustStop)
							{

								if (warnings.size() > 0)
								{
									output("Found warning markers in projects for solution " + solutionName);
									if (verbose || returnDueToErrors)
									{
										for (IMarker marker : warnings)
										{
											outputMarker(marker, false);
										}
									}
								}
								if (returnDueToErrors) return;
								// now we really export
								exportActiveSolution(configuration);
							}
						}
					}
					else
					{
						outputError("EXPORT FAILED. Solution '" + solutionName + "' will NOT be exported. It cannot be activated.");
						exitCode = EXIT_EXPORT_FAILED;
					}
				}
			}
		}
		finally
		{
			outputExtra("Restoring closed projects if needed.");
			for (IProject p : existingClosedProjects)
			{
				try
				{
					p.close(null);
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
					outputError("Cannot restore project '" + p.getName() + "' to it's closed state after export. Check workspace log.");
				}
			}
			outputExtra("Removing imported projects from workspace (without removing content) if needed.");
			for (IProject p : importedProjects)
			{
				try
				{
					p.delete(false, true, null);
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
					outputError("Cannot remove project (not content) '" + p.getName() + "' from workspace after export. Check workspace log.");
				}
			}
		}
	}


	protected void importExistingAndOpenClosedProjects(File sourceFolder, IWorkspaceRoot workspaceRoot, List<IProject> importedProjects,
		List<IProject> existingClosedProjects)
	{
		boolean useLinks = !workspaceRoot.getLocation().toFile().equals(sourceFolder);
		File[] files = sourceFolder.listFiles();
		if (files == null) return;
		for (File f : files)
		{
			// this assumes that the name defined in ".project" matches the name of the parent folder;
			// if needed in the future, Workspace.loadProjectDescription(<.project>) can be used before we create the Project instance
			IProject p = workspaceRoot.getProject(f.getName());
			if (f.isDirectory() && new File(f, ".project").exists())
			{
				if (!p.exists() || !p.isOpen())
				{
					outputExtra("Trying to import project '" + f.getName() + "' from location '" + f.getAbsolutePath() + "' into workspace.");
					try
					{
						boolean existed = p.exists();
						if (!existed)
						{
							if (useLinks)
							{
								// create a new project in this workspace linking to the real project location
								IProjectDescription projectDescription = workspaceRoot.getWorkspace().newProjectDescription(p.getName());
								projectDescription.setLocationURI(f.toURI());
								p.create(projectDescription, null);
							}
							else
							{
								// real project location is default - directly inside workspace folder
								p.create(null);
							}
							importedProjects.add(p);
						}
						if (!p.isOpen())
						{
							p.open(null);

							if (existed)
							{
								if (!p.getLocation().toFile().equals(f))
								{
									outputError("Cannot use project in alternate location '" + f.getAbsolutePath() +
										"'. Another project with that name is already present in workspace from location '" +
										p.getLocation().toFile().getAbsolutePath() + "'.");
								}

								// if a previous export operation managed to create the project but failed to open it, subsequent exports
								// should try to temporarily open it again (useful for automatic build systems where it would be hard to know why the projects are not used anymore otherwise)
								existingClosedProjects.add(p);
							}

						}
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
						outputError("Cannot import and open project '" + f.getName() + "' into workspace. Check workspace log.");
					}
				}
				else if (useLinks && !p.getLocation().toFile().equals(f))
				{
					outputError("Cannot use project in alternate location '" + f.getAbsolutePath() +
						"'. Another project with that name is already present in workspace from location '" + p.getLocation().toFile().getAbsolutePath() +
						"'.");
				}
			}
		}
	}

	/**
	 * Can be overridden to change the list of solutions that are checked - to check only some modules for example.
	 */
	protected void checkProjectMarkers(ServoyProject[] solutionProjects, List<IMarker> errors, List<IMarker> warnings, T config)
	{
		for (ServoyProject module : solutionProjects)
		{
			splitMarkers(module.getProject(), errors, warnings);
		}
	}

	protected abstract void exportActiveSolution(T configuration);

	private void splitMarkers(IProject project, List<IMarker> errors, List<IMarker> warnings)
	{
		try
		{
			IMarker[] markers = project.findMarkers(ServoyBuilder.SERVOY_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
			for (IMarker marker : markers)
			{
				if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR)
				{
					errors.add(marker);
				}
				else
				{
					warnings.add(marker);
				}
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
			outputError("Marker check encountered a problem. Check workspace log.");
		}
	}

	public void stop()
	{
		mustStop = true;
		exitCode = EXIT_STOPPED;
		output("Stopping export...");
		synchronized (stopLock)
		{
			if (!finished)
			{
				try
				{
					stopLock.wait();
				}
				catch (InterruptedException e)
				{
					ServoyLog.logWarning("Interrupted while waiting for requested application stop.", e);
				}
			}
		}
	}

	private void initializeApplicationServer(IArgumentChest configuration)
	{
		BundleContext bc = Activator.getDefault().getBundle().getBundleContext();

		//register a table loader so the server will use this if present
		if (configuration.shouldExportUsingDbiFileInfoOnly())
		{
			ITableLoader tableLoader = new DBITableLoader();
			bc.registerService(ITableLoader.class, tableLoader, null);
		}

		ServiceReference<IServerStarter> ref = bc.getServiceReference(IServerStarter.class);
		IServerStarter ss = bc.getService(ref);
		if (ss != null)
		{
			try
			{
				IExtensionRegistry reg = Platform.getExtensionRegistry();
				IExtensionPoint ep = reg.getExtensionPoint(IPluginBaseClassLoaderProvider.EXTENSION_ID);
				IExtension[] extensions = ep.getExtensions();
				if (extensions != null && extensions.length > 0)
				{
					for (IExtension extension : extensions)
					{
						IPluginBaseClassLoaderProvider provider = (IPluginBaseClassLoaderProvider)extension.getConfigurationElements()[0]
							.createExecutableExtension(
								"class");
						ss.setBaseClassloader(provider.getClassLoader());
						break; //we support only one
					}
				}
				else
				{
					throw new IllegalStateException("Could not load plugin base classloader provider");
				}
				ss.init();
				ss.setRepositoryFactory(new EclipseRepositoryFactory());
				ss.setUserManagerFactory(new IUserManagerFactory()
				{
					public IUserManager createUserManager(IDataServer dataServer)
					{
						return new WorkspaceUserManager();
					}
				});
				ss.start(true);
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
				outputError("EXPORT FAILED. Cannot initialize app. server. Check workspace log.");
				exitCode = EXIT_EXPORT_FAILED;
			}

			ServoyModelFinder.initializeServoyModelProvider("exporter");
			ServoyModelFinder.getServoyModel(); // initialise servoy model as well
		}
		else
		{
			outputError("EXPORT FAILED. Cannot initialize exporter due to missing app. server starter extension.");
			exitCode = EXIT_EXPORT_FAILED;
		}
	}

	public void outputExtra(String msg)
	{
		if (verbose) System.out.println(msg);
	}

	public void output(String msg)
	{
		System.out.println(msg);
	}

	public void outputMarker(IMarker marker, boolean outputExtra)
	{
		if (outputExtra && !verbose) return;
		String message = "    -" + marker.getAttribute(IMarker.MESSAGE, "Unknown marker message.");
		Object location = marker.getAttribute(IMarker.LOCATION, null);
		String path = marker.getResource().getLocation().toOSString();
		if (path != null)
		{
			if (location != null)
			{
				path = path + " : " + location;
			}
			message += " (" + path + ")";
		}
		output(message);
	}

	public void outputError(String msg)
	{
		System.err.println(msg);
	}
}
