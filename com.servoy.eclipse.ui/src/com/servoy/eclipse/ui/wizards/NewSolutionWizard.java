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
package com.servoy.eclipse.ui.wizards;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.dltk.javascript.core.JavaScriptNature;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.EclipseDatabaseUtils;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.RepositorySettingsDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.CreateMediaWebAppManifest;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewPostgresDbAction;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.persistence.ServerSettings;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.ngclient.less.resources.ThemeResourceLoader;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * Wizard used in order to create a new Servoy solution project. Will optionally create linked resource project (with styles & other info).
 *
 * @author acostescu
 */
public class NewSolutionWizard extends Wizard implements INewWizard
{
	public static final String ID = "com.servoy.eclipse.ui.NewSolutionWizard";
	protected GenerateSolutionWizardPage configPage;

	/**
	 * Creates a new wizard.
	 */
	public NewSolutionWizard()
	{
		setWindowTitle("New solution");
		setDefaultPageImageDescriptor(Activator.loadImageDescriptorFromBundle("solution_wizard_description.png"));
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = DialogSettings.getOrCreateSection(workbenchSettings, "NewSolutionWizard");
		setDialogSettings(section);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		configPage = new GenerateSolutionWizardPage("New Solution");
	}

	@Override
	public void addPages()
	{
		addPage(configPage);
	}

	@Override
	public boolean performFinish()
	{
		saveAllSettings();

		final IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

		final List<String> solutions = configPage.getSolutionsToImport();
		final boolean mustAuthenticate = configPage.mustAuthenticate();
		IRunnableWithProgress newSolutionRunnable = new IRunnableWithProgress()
		{

			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				monitor.beginTask("Creating solution and writing files to disk", 4);
				// create Solution object
				EclipseRepository repository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
				try
				{
					Solution solution = (Solution)repository.createNewRootObject(configPage.getNewSolutionName(), IRepository.SOLUTIONS);

					String modulesTokenized = ModelUtils.getTokenValue(solutions.toArray(new String[] { }), ",");
					solution.setModulesNames(modulesTokenized);
					solution.setVersion("1.0");

					monitor.setTaskName("Setting up resource project and reference");
					IProject resourceProject;
					// create Resource project if needed
					if (configPage.getResourceProjectData().getNewResourceProjectName() != null)
					{
						// create a new resource project
						String resourceProjectName = configPage.getResourceProjectData().getNewResourceProjectName();
						resourceProject = ServoyModel.getWorkspace().getRoot().getProject(resourceProjectName);
						String location = configPage.getProjectLocation();
						IProjectDescription description = ServoyModel.getWorkspace().newProjectDescription(resourceProjectName);
						if (location != null)
						{
							IPath path = new Path(location);
							path = path.append(resourceProjectName);
							description.setLocation(path);
						}
						resourceProject.create(description, null);
						resourceProject.open(null);

						// write repositoy UUID into the resource project
						RepositorySettingsDeserializer.writeRepositoryUUID(new WorkspaceFileAccess(ResourcesPlugin.getWorkspace()), resourceProjectName,
							repository.getRepositoryUUID());

						IProjectDescription resourceProjectDescription = resourceProject.getDescription();
						resourceProjectDescription.setNatureIds(new String[] { ServoyResourcesProject.NATURE_ID });
						resourceProject.setDescription(resourceProjectDescription, null);
					}
					else
					{
						resourceProject = configPage.getResourceProjectData().getExistingResourceProject() != null
							? configPage.getResourceProjectData().getExistingResourceProject().getProject() : null;
					}

					monitor.setTaskName("Creating and opening project");
					// as the serialization is done using java.io, we must make sure the Eclipse resource structure
					// stays up to date; we create a project, then we must open it and add servoy solution nature
					IProject newProject = ServoyModel.getWorkspace().getRoot().getProject(configPage.getNewSolutionName());
					String location = configPage.getProjectLocation();
					IProjectDescription description = ServoyModel.getWorkspace().newProjectDescription(configPage.getNewSolutionName());
					if (location != null)
					{
						IPath path = new Path(location);
						path = path.append(configPage.getNewSolutionName());
						description.setLocation(path);
					}
					newProject.create(description, null);
					newProject.open(null);
					monitor.worked(1);

					if (solution != null)
					{
						int solutionType = configPage.getSolutionType();
						solution.setSolutionType(solutionType);

						// serialize Solution object to given project
						repository.updateRootObject(solution);

						//disable must authenticate for now, until we include login form generation, users creation
						//solution.setMustAuthenticate(mustAuthenticate);
						addDefaultThemeIfNeeded(repository, solution);
						addDefaultWAMIfNeeded(repository, solution);
					}
					monitor.worked(1);

					monitor.setTaskName("Registering natures");
					description.setNatureIds(new String[] { ServoyProject.NATURE_ID, JavaScriptNature.NATURE_ID });
					monitor.worked(1);

					// link solution project to the resource project; store project description

					if (resourceProject != null)
					{
						description.setReferencedProjects(new IProject[] { resourceProject });
					}
					newProject.setDescription(description, null);
					monitor.worked(1);
					monitor.done();
				}
				catch (final Exception e)
				{
					monitor.done();
					ServoyLog.logError(e);
					Display.getDefault().syncExec(new Runnable()
					{
						public void run()
						{
							MessageDialog.openError(UIUtils.getActiveShell(), "Cannot create new solution", e.getMessage());
						}
					});
				}
			}

			private void addDefaultThemeIfNeeded(EclipseRepository repository, Solution solution) throws RepositoryException
			{
				if (configPage.shouldAddDefaultTheme())
				{
					Media defaultTheme = addMediaFile(solution, ThemeResourceLoader.getDefaultSolutionLess(), solution.getName() + ".less");
					addMediaFile(solution, ThemeResourceLoader.getCustomProperties(), ThemeResourceLoader.CUSTOM_PROPERTIES_LESS);
					addMediaFile(solution, ThemeResourceLoader.getVariantsFile(), ThemeResourceLoader.VARIANTS_JSON);

					solution.setStyleSheetID(defaultTheme.getID());
					repository.updateRootObject(solution);
				}
			}

			private void addDefaultWAMIfNeeded(EclipseRepository repository, Solution solution) throws RepositoryException, IOException
			{
				if (configPage.shouldAddDefaultWAM())
				{
					addMediaFile(solution, CreateMediaWebAppManifest.createManifest(solution.getName()), CreateMediaWebAppManifest.FILE_NAME);
					addMediaFile(solution, CreateMediaWebAppManifest.getIcon(), CreateMediaWebAppManifest.ICON_NAME);
					repository.updateRootObject(solution);
				}
			}

			private Media addMediaFile(Solution solution, byte[] content, String fileName) throws RepositoryException
			{
				Media defaultTheme = solution.createNewMedia(new ScriptNameValidator(), fileName);
				defaultTheme.setMimeType("text/css");
				defaultTheme.setPermMediaData(content);
				return defaultTheme;
			}
		};

		final ServoyProject activeProject = servoyModel.getActiveProject();
		final Solution activeEditingSolution = (activeProject != null) ? activeProject.getEditingSolution() : null;
		final String jobName;
		final boolean addAsModuleToActiveSolution = shouldAddAsModule(activeEditingSolution);
		if (addAsModuleToActiveSolution)
		{
			jobName = "Adding as module to active solution";
		}
		else
		{
			jobName = "Activating new solution";
		}

		IRunnableWithProgress solutionActivationRunnable = new IRunnableWithProgress()
		{

			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				monitor.beginTask(jobName, 1);
				servoyModel.refreshServoyProjects();
				// set this solution as the new active solution or add it as a module
				ServoyProject newProject = servoyModel.getServoyProject(configPage.getNewSolutionName());
				if (newProject == null)
				{
					servoyModel.refreshServoyProjects();
					newProject = servoyModel.getServoyProject(configPage.getNewSolutionName());
				}
				if (newProject != null)
				{
					if (addAsModuleToActiveSolution)
					{
						boolean doesNotHaveIt = true; // sometimes it might happen that the solution already referenced an unavailable module that was now created (so before there was an error marker); we don't want a double reference

						String modules = activeEditingSolution.getModulesNames();

						if (modules == null) modules = "";
						String[] moduleNames = Utils.getTokenElements(modules, ",", true);
						if (modules.trim().length() > 0) modules = modules + ",";
						for (String moduleName : moduleNames)
						{
							if (newProject.getProject().getName().equals(moduleName))
							{
								doesNotHaveIt = false;
								break;
							}
						}

						if (doesNotHaveIt)
						{
							activeEditingSolution.setModulesNames(modules + newProject.getProject().getName());
							try
							{
								activeProject.saveEditingSolutionNodes(new IPersist[] { activeEditingSolution }, false);
							}
							catch (RepositoryException e)
							{
								ServoyLog.logError(e);
							}
						}
					}
					else
					{
						servoyModel.setActiveProject(newProject, true);
					}
				}
				else
				{
					ServoyLog.logError("cannot activate solution", null);
				}
				monitor.worked(1);
				monitor.done();
			}
		};

		Map<String, SolutionPackageInstallInfo> toImportSolutions = new HashMap<>();
		for (String name : solutions)
		{
			Pair<String, File> solution = NewSolutionWizardDefaultPackages.getInstance().getPackage(name);
			toImportSolutions.put(name, new SolutionPackageInstallInfo(solution.getLeft(), solution.getRight(), false, false));
		}
		IRunnableWithProgress importSolutionsRunnable = importSolutions(toImportSolutions, jobName, configPage.getNewSolutionName(), false, false);

		IRunnableWithProgress importPackagesRunnable = null;
		final List<String> packs = configPage.getWebPackagesToImport();
		if (packs != null && configPage.getSolutionType() != SolutionMetaData.MODULE && configPage.getSolutionType() != SolutionMetaData.NG_MODULE)
		{
			importPackagesRunnable = new IRunnableWithProgress()
			{
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
				{
					ServoyProject newProject = servoyModel.getServoyProject(configPage.getNewSolutionName());
					if (newProject == null)
					{
						servoyModel.refreshServoyProjects();
						newProject = servoyModel.getServoyProject(configPage.getNewSolutionName());
					}
					if (newProject != null)
					{
						IProject project = newProject.getProject();
						try
						{
							project.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
						}
						catch (CoreException e1)
						{
							ServoyLog.logError(e1);
						}
						IFolder folder = project.getFolder(SolutionSerializer.NG_PACKAGES_DIR_NAME);

						try
						{
							folder.create(true, true, new NullProgressMonitor());
						}
						catch (CoreException e)
						{
							ServoyLog.logError(e);
						}

						for (String name : packs)
						{
							File dataFile = NewSolutionWizardDefaultPackages.getInstance().getPackage(name).getRight();
							IFile eclipseFile = folder.getFile(name + ".zip");
							try
							{
								eclipseFile.create(new FileInputStream(dataFile), IResource.NONE, new NullProgressMonitor());
							}
							catch (CoreException | FileNotFoundException e)
							{
								ServoyLog.logError(e);
							}
						}
					}
				}
			};
		}

		try
		{
			IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
			progressService.run(true, false, newSolutionRunnable);
			if (importPackagesRunnable != null) progressService.run(true, false, importPackagesRunnable);
			progressService.run(true, false, solutionActivationRunnable);
			progressService.run(true, false, importSolutionsRunnable);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}

//		RunDesignClientDialog dialog = new RunDesignClientDialog(getShell());
//		dialog.setBlockOnOpen(true);
//		dialog.open();
//		dialog.close();\
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				try
				{
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.eclipse.ui.views.PropertySheet");
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		});
		return true;
	}

	public static IRunnableWithProgress importSolutions(final Map<String, SolutionPackageInstallInfo> solutions, final String jobName, String newSolutionName,
		boolean activateSolution, boolean overwriteModules)
	{
		Set<String> missingServerNames = searchMissingServers(solutions.keySet()).keySet();
		IRunnableWithProgress importSolutionsRunnable = new IRunnableWithProgress()
		{
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				monitor.beginTask(jobName, missingServerNames.size() + solutions.size());
				try
				{
					createMissingDbServers(missingServerNames, monitor);

					HashSet<IProject> projectsToDeleteAfterImport = new HashSet<IProject>();
					IDeveloperServoyModel sm = ServoyModelManager.getServoyModelManager().getServoyModel();
					Boolean[] importDatasources = new Boolean[] { null };
					for (String name : solutions.keySet())
					{
						boolean shouldAskOverwrite = (sm.getServoyProject(name) == null ? false : shouldOverwrite(sm, name));
						if (sm.getServoyProject(name) == null || shouldAskOverwrite)
						{
							importSolution(solutions.get(name), name, newSolutionName, monitor, true,
								shouldAskOverwrite, activateSolution, overwriteModules, importDatasources, projectsToDeleteAfterImport);
							monitor.worked(1);
						}
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
				monitor.done();
			}

			private boolean shouldOverwrite(IDeveloperServoyModel sm, String name)
			{
				ServoyProject solutionProject = sm.getServoyProject(name);
				if (solutionProject != null)
				{
					File wpmPropertiesFile = new File(solutionProject.getProject().getLocation().toFile(), "wpm.properties");
					if (wpmPropertiesFile.exists())
					{
						Properties wpmProperties = new Properties();
						try (FileInputStream wpmfis = new FileInputStream(wpmPropertiesFile))
						{
							wpmProperties.load(wpmfis);
							String version = wpmProperties.getProperty("version");
							if (version != null)
							{
								Pair<String, File> pack = NewSolutionWizardDefaultPackages.getInstance().getPackage(name);
								if (pack != null)
								{
									return !pack.getLeft().equals(version);
								}
							}
							else
							{
								return true;
							}
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
				}
				return true;
			}
		};
		return importSolutionsRunnable;
	}

	public static HashMap<String, List<String>> searchMissingServers(final Collection<String> solutions)
	{
		IDeveloperServoyModel sm = ServoyModelManager.getServoyModelManager().getServoyModel();
		IServerManagerInternal serverHandler = ApplicationServerRegistry.get().getServerManager();
		HashMap<String, List<String>> missingServerNames = new HashMap<>();
		for (String name : solutions)
		{
			if (sm.getServoyProject(name) == null)
			{
				try
				{
					Document doc = NewSolutionWizardDefaultPackages.getInstance().getDatabaseInfo(name);
					if (doc == null) continue;

					NodeList connections = doc.getElementsByTagName("connection");
					for (int i = 0; i < connections.getLength(); i++)
					{
						NodeList n = connections.item(i).getChildNodes();
						for (int j = 0; j < n.getLength(); j++)
						{
							if ("name".equals(n.item(j).getNodeName()))
							{
								String server_name = n.item(j).getTextContent();
								IServerInternal serverObj = (IServerInternal)serverHandler.getServer(server_name, false, false);
								if (serverObj == null)
								{
									if (!missingServerNames.containsKey(server_name))
									{
										missingServerNames.put(server_name, new ArrayList<>());
									}
									missingServerNames.get(server_name).add(name);
								}
								break;
							}
						}
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}
		return missingServerNames;
	}

	protected boolean canCreateMissingServers()
	{
		return Arrays.stream(ApplicationServerRegistry.get().getServerManager().getServerConfigs()).anyMatch(s -> s.isPostgresDriver() && s.isEnabled());
	}

	protected static void createMissingDbServers(Set<String> missingServerNames, IProgressMonitor monitor)
	{
		IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();

		ServerConfig origConfig = Arrays.stream(serverManager.getServerConfigs())
			.filter(
				s -> s.isPostgresDriver() && s.isEnabled())
			.findAny()
			.orElse(null);
		if (origConfig == null)
		{
			ServoyLog.logError(new Exception("Cannot create missing servers. Did not find any Postgres server config"));
			return;
		}

		IServerInternal server = (IServerInternal)serverManager.getServer(origConfig.getServerName());
		if (server == null || !server.isValid())
		{
			ServoyLog.logError(new Exception("Cannot create missing servers. Did not find a valid Postgres server."));
			return;
		}

		NewPostgresDbAction action = new NewPostgresDbAction(null);
		for (String server_name : missingServerNames)
		{
			action.createDatabase(server, server_name, monitor);
			final ServerConfig serverConfig = origConfig.newBuilder()
				.setServerName(server_name)
				.setServerUrl(EclipseDatabaseUtils.getPostgresServerUrl(origConfig, server_name))
				.setSchema(null)
				.setDataModelCloneFrom(null)
				.setEnabled(true).setSkipSysTables(false)
				.setIdleTimeout(-1)
				.build();
			try
			{
				serverManager.testServerConfigConnection(serverConfig, 0);
				serverManager.saveServerConfig(null, serverConfig);
				serverManager.saveServerSettings(serverConfig.getServerName(),
					serverManager.getServerSettings(origConfig.getServerName()).withDefaults(serverConfig));
			}
			catch (Exception ex)
			{
				ServoyLog.logError("Cannot create server '" + server_name + "'", ex);
			}
			monitor.worked(1);
		}
	}

	protected void saveAllSettings()
	{
		IDialogSettings dialogSettings = getDialogSettings();
		dialogSettings.put(getSettingsPrefix() + GenerateSolutionWizardPage.IS_ADVANCED_USER_SETTING, configPage.isAdvancedUser());
		dialogSettings.put(getSettingsPrefix() + GenerateSolutionWizardPage.SELECTED_SOLUTIONS_SETTING, configPage.getSelectedSolutions());
		dialogSettings.put(getSettingsPrefix() + GenerateSolutionWizardPage.SOLUTION_TYPE, configPage.getSolutionType());

		boolean noResourceProject = configPage.getResourceProjectData().getNewResourceProjectName() == null &&
			configPage.getResourceProjectData().getExistingResourceProject() == null;
		dialogSettings.put(getSettingsPrefix() + GenerateSolutionWizardPage.NO_RESOURCE_PROJECT_SETTING, noResourceProject);
		if (!noResourceProject)
		{
			String resourcesProjectName = configPage.getResourceProjectData().getNewResourceProjectName() != null
				? configPage.getResourceProjectData().getNewResourceProjectName()
				: configPage.getResourceProjectData().getExistingResourceProject().getProject().getName();
			dialogSettings.put(getSettingsPrefix() + GenerateSolutionWizardPage.RESOURCE_PROJECT_NAME_SETTING, resourcesProjectName);
		}

		dialogSettings.put(getSettingsPrefix() + GenerateSolutionWizardPage.SHOULD_ADD_DEFAULT_THEME_SETTING, configPage.shouldAddDefaultTheme());
		dialogSettings.put(getSettingsPrefix() + GenerateSolutionWizardPage.SHOULD_ADD_DEFAULT_WAM_SETTING, configPage.shouldAddDefaultWAM());
	}

	public static void importSolution(SolutionPackageInstallInfo packageInfo, final String name, final String targetSolution, IProgressMonitor monitor,
		boolean reportImportFail, boolean shouldAskOverwrite, boolean activateSolution, boolean overwriteModules, Boolean[] importDatasources,
		Set<IProject> projectsToDeleteAfterImport)
		throws IOException
	{
		if (name.equals(targetSolution)) return; // import solution and target can't be the same
		final File importSolutionFile = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(), name + ".servoy");
		if (importSolutionFile.exists())
		{
			importSolutionFile.delete();
		}
		try (FileOutputStream fos = new FileOutputStream(importSolutionFile))
		{
			Utils.streamCopy(new FileInputStream(packageInfo.data), fos);
		}


		// import the .servoy solution into workspace
		ImportSolutionWizard importSolutionWizard = new ImportSolutionWizard();
		importSolutionWizard.setSolutionFilePath(importSolutionFile.getAbsolutePath());
		importSolutionWizard.setAllowSolutionFilePathSelection(false);
		importSolutionWizard.setActivateSolution(activateSolution);
		importSolutionWizard.init(PlatformUI.getWorkbench(), null);
		importSolutionWizard.setReportImportFail(reportImportFail);
		importSolutionWizard.setOverwriteModule(overwriteModules);
		importSolutionWizard.setSkipModulesImport(!shouldAskOverwrite);
		importSolutionWizard.setAllowDataModelChanges(true);
		importSolutionWizard.setImportSampleData(true);
		importSolutionWizard.setImportDatasources(importDatasources[0]);
		importSolutionWizard.shouldAllowSQLKeywords(true);
		importSolutionWizard.showFinishDialog(false);

		String newResourceProjectName = null;
		ServoyResourcesProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();
		if (project == null && packageInfo.forceActivateResourcesProject)
		{
			newResourceProjectName = "resources";
			int counter = 1;
			while (ServoyModel.getWorkspace().getRoot().getProject(newResourceProjectName).exists())
			{
				newResourceProjectName = "resources" + counter++;
			}
		}
		importSolutionWizard.doImport(importSolutionFile, newResourceProjectName, project, false, false, false, null, null,
			monitor, packageInfo.forceActivateResourcesProject, packageInfo.keepResourcesProjectOpen, projectsToDeleteAfterImport);
		importDatasources[0] = importSolutionWizard.shouldImportDatasources();
		// write the wpm version into the new solution project
		String solutionVersion = packageInfo.version;
		if (solutionVersion.length() > 0)
		{
			ServoyProject solutionProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(name);

			Properties wpmProperties = new Properties();
			wpmProperties.put("version", solutionVersion);

			try (ByteArrayOutputStream wpmbos = new ByteArrayOutputStream())
			{
				wpmProperties.store(wpmbos, "");
				byte[] wpmPropertiesBytes = wpmbos.toByteArray();
				WorkspaceFileAccess importedProjectFA = new WorkspaceFileAccess(solutionProject.getProject().getWorkspace());
				importedProjectFA.setContents(solutionProject.getProject().getFullPath().append("wpm.properties").toOSString(), wpmPropertiesBytes);
			}
			catch (Exception ex)
			{
				Debug.log(ex);
			}
		}
		cleanUPImportSolution(importSolutionFile);
	}

	public static void addAsModule(final String name, final String targetSolution, final File importSolutionFile)
	{
		final IProject importedProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(name).getProject();
		final ServoyProject targetServoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(targetSolution);
		if (targetServoyProject != null && importedProject != null && targetServoyProject.getProject().isOpen() && importedProject.isOpen())
		{
			Job job = new WorkspaceJob("Adding '" + name + "' as module of '" + targetSolution + "'...")
			{
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					try
					{
						if (targetServoyProject != null && importedProject != null && targetServoyProject.getProject().isOpen() && importedProject.isOpen())
						{
							Solution editingSolution = targetServoyProject.getEditingSolution();
							if (editingSolution != null)
							{
								String[] modules = Utils.getTokenElements(editingSolution.getModulesNames(), ",", true);
								List<String> modulesList = new ArrayList<String>(Arrays.asList(modules));
								if (!modulesList.contains(name))
								{
									modulesList.add(name);
								}
								String modulesTokenized = ModelUtils.getTokenValue(modulesList.toArray(new String[] { }), ",");
								editingSolution.setModulesNames(modulesTokenized);

								try
								{
									targetServoyProject.saveEditingSolutionNodes(new IPersist[] { editingSolution }, false);
								}
								catch (RepositoryException e)
								{
									ServoyLog.logError("Cannot save new module list for active module " + targetServoyProject.getProject().getName(), e);
								}
							}
						}
						return Status.OK_STATUS;
					}
					finally
					{
						cleanUPImportSolution(importSolutionFile);
					}
				}
			};
			job.setUser(true);
			job.setRule(MultiRule.combine(targetServoyProject.getProject(), importedProject));
			job.schedule();
		}
	}

	private static void cleanUPImportSolution(File importSolutionFile)
	{
		try
		{
			if (importSolutionFile != null && importSolutionFile.exists())
			{
				importSolutionFile.delete();
			}
		}
		catch (RuntimeException e)
		{
			ServoyLog.logError(e);
		}
	}


	/**
	 * @param activeEditingSolution
	 * @return
	 */
	protected boolean shouldAddAsModule(final Solution activeEditingSolution)
	{
		return activeEditingSolution != null &&
			(configPage.getSolutionType() == SolutionMetaData.MODULE || configPage.getSolutionType() == SolutionMetaData.NG_MODULE ||
				configPage.getSolutionType() == SolutionMetaData.PRE_IMPORT_HOOK || configPage.getSolutionType() == SolutionMetaData.POST_IMPORT_HOOK);
	}

	protected String getSettingsPrefix()
	{
		return "sol_";
	}

	protected String getHelpContextID()
	{
		return "com.servoy.eclipse.ui.create_solution";
	}


	@Override
	public boolean canFinish()
	{
		return super.canFinish() && (searchMissingServers(configPage.getSolutionsToImport()).isEmpty() || canCreateMissingServers());
	}


	@Override
	public boolean performCancel()
	{
		Set<String> searchMissingServers = searchMissingServers(configPage.getSolutionsToImport()).keySet();
		if (!canCreateMissingServers() && !searchMissingServers.isEmpty())
		{
			if (MessageDialog.openQuestion(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Cancel",
				"Do you want to create missing database server(s) connections?"))
			{
				ServerConfig origConfig = getValidServerConfig();
				for (String server_name : searchMissingServers)
				{
					final ServerConfig config = origConfig.newBuilder()
						.setServerName(server_name)
						.setSchema(null)
						.setDataModelCloneFrom(null)
						.setEnabled(true).setSkipSysTables(false)
						.setIdleTimeout(-1)
						.build();

					EditorUtil.openServerEditor(config, ServerSettings.DEFAULT, true);
				}
			}
		}
		return super.performCancel();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.wizard.Wizard#setContainer(org.eclipse.jface.wizard.IWizardContainer)
	 */
	@Override
	public void setContainer(IWizardContainer wizardContainer)
	{
		if (wizardContainer != null && wizardContainer.getShell() != null)
		{
			wizardContainer.getShell().setData(CSSSWTConstants.CSS_ID_KEY, "svyeditor");
		}
		super.setContainer(wizardContainer);
	}

	@Override
	public void createPageControls(Composite pageContainer)
	{
		pageContainer.getShell().setData(CSSSWTConstants.CSS_ID_KEY, "svydialog");
		super.createPageControls(pageContainer);
	}

	protected ServerConfig getValidServerConfig()
	{
		return Arrays.stream(ApplicationServerRegistry.get().getServerManager().getServerConfigs())
			.filter(
				s -> s.isEnabled() && ApplicationServerRegistry.get().getServerManager().getServer(s.getServerName()) != null &&
					((IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(s.getServerName())).isValid())
			.findAny()
			.orElse(null);
	}

	public static class SolutionPackageInstallInfo
	{
		public String version;
		public File data;
		public boolean forceActivateResourcesProject;
		public boolean keepResourcesProjectOpen;

		public SolutionPackageInstallInfo(String version, File data, boolean forceActivateResourcesProject, boolean keepResourcesProjectOpen)
		{
			this.version = version;
			this.data = data;
			this.forceActivateResourcesProject = forceActivateResourcesProject;
			this.keepResourcesProjectOpen = keepResourcesProjectOpen;
		}
	}
}