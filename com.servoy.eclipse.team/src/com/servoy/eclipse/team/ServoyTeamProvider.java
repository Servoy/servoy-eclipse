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
package com.servoy.eclipse.team;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.rmi.UnmarshalException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.resources.mapping.CompositeResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.team.ResourceRuleFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.IIgnoreInfo;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.core.ResourceVariantCache;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.IOFileAccess;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.ServoyResourcesProject;
import com.servoy.eclipse.core.TeamShareMonitor;
import com.servoy.eclipse.core.WorkspaceFileAccess;
import com.servoy.eclipse.core.TeamShareMonitor.TeamShareMonitorExtension;
import com.servoy.eclipse.core.repository.DataModelManager;
import com.servoy.eclipse.core.repository.EclipseMessages;
import com.servoy.eclipse.core.repository.EclipseUserManager;
import com.servoy.eclipse.core.repository.RepositoryAccessPoint;
import com.servoy.eclipse.core.repository.RepositorySettingsDeserializer;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.team.subscriber.SolutionMergeContext;
import com.servoy.eclipse.team.subscriber.SolutionResourceVariant;
import com.servoy.eclipse.team.subscriber.SolutionSubscriber;
import com.servoy.eclipse.team.ui.NewSolutionWizard;
import com.servoy.eclipse.team.ui.PasswordInputDialog;
import com.servoy.eclipse.team.ui.PreferencesPage;
import com.servoy.eclipse.team.ui.ResourceDecorator;
import com.servoy.eclipse.team.ui.SolutionSynchronizeParticipant;
import com.servoy.eclipse.team.ui.actions.CommitOperation;
import com.servoy.eclipse.team.ui.actions.SolutionOperation;
import com.servoy.eclipse.team.ui.actions.UpdateOperation;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITeamRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

public class ServoyTeamProvider extends RepositoryProvider
{
	private static File temporaryDirectory = null;

	public static final String RESOURCE_PREFIX = "servoy_";
	public static final String RESOURCE_SUFFIX = "_resources";

	private static final String BASE_DIR = ".stp";
	private static final String SCRIPT_BUILDPATH = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><buildpath><buildpathentry excluding=\".stp/\" kind=\"src\" path=\"\"/></buildpath>";

	private RepositoryOperations repositoryOperations;
	private IOFileAccess baseFileAccess;
	private final WorkspaceFileAccess workspaceFileAccess;
	/*
	 * Create a custom rule factory to allow more optimistic concurrency
	 */
	private static final ResourceRuleFactory RESOURCE_RULE_FACTORY = new ResourceRuleFactory()
	{
		private final boolean run = true;

		@Override
		public ISchedulingRule createRule(IResource resource)
		{
			updateResourceDecorator(resource);
			return parent(resource);
		}

		@Override
		public ISchedulingRule deleteRule(IResource resource)
		{
			updateResourceDecorator(resource);
			return parent(resource);
		}

		// Just need a subclass to instantiate
		@Override
		public ISchedulingRule modifyRule(IResource resource)
		{
			updateResourceDecorator(resource);
			return super.modifyRule(resource);
		}


		private void updateResourceDecorator(IResource resource)
		{
			ArrayList<IResource> resourcePathA = new ArrayList<IResource>();

			IResource r = resource;
			while (r != null && r.getType() != IResource.ROOT)
			{
				resourcePathA.add(r);
				r = r.getParent();
			}

			ResourceDecorator rd = (ResourceDecorator)PlatformUI.getWorkbench().getDecoratorManager().getBaseLabelProvider(ResourceDecorator.ID);
			if (rd != null) rd.fireChanged(resourcePathA.toArray(new IResource[0]));
		}
	};

	private static final IResourceChangeListener PROJECT_DELETE_LISTENER = new IResourceChangeListener()
	{

		public void resourceChanged(IResourceChangeEvent event)
		{
			IResource r = event.getResource();
			if (event.getType() == IResourceChangeEvent.PRE_DELETE && r.getType() == IResource.PROJECT)
			{
				RepositoryProvider provider = RepositoryProvider.getProvider((IProject)r);
				if (provider instanceof ServoyTeamProvider) ((ServoyTeamProvider)provider).clearBaseFileAccess();
			}
		}
	};

	public ServoyTeamProvider()
	{
		super();
		workspaceFileAccess = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
	}

	@Override
	public void setProject(final IProject project)
	{
		super.setProject(project);
		baseFileAccess = new IOFileAccess(new File(project.getLocation().toFile(), ServoyTeamProvider.BASE_DIR));
		File baseFile = baseFileAccess.toFile();
		if (!baseFile.exists())
		{
			WorkspaceFileAccess baseWSFileAccess = new WorkspaceFileAccess(project.getWorkspace());
			try
			{
				baseWSFileAccess.createFolder(project.getFullPath().append(ServoyTeamProvider.BASE_DIR).toOSString());
				project.getFolder(ServoyTeamProvider.BASE_DIR).setDerived(true);
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
			//baseFile.mkdir();
		}
		try
		{
			IResource jsBuildPath = getProject().findMember(".buildpath");
			if (jsBuildPath == null)
			{
				jsBuildPath = getProject().getFile(".buildpath");
				((IFile)jsBuildPath).create(new ByteArrayInputStream(SCRIPT_BUILDPATH.getBytes()), false, null);
			}
			if (!jsBuildPath.isTeamPrivateMember())
			{
				jsBuildPath.setTeamPrivateMember(true);
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		if (getOperations() == null) // project has no .teamprovider config file
		{
			try
			{
				RepositoryProvider.unmap(project);
			}
			catch (TeamException ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}

	@Override
	public void configureProject() throws CoreException
	{
		IFile teamProviderResource = getProject().getFile(".teamprovider");
		teamProviderResource.refreshLocal(IResource.DEPTH_ZERO, null);

		if (teamProviderResource.exists())
		{
			teamProviderResource.setTeamPrivateMember(true);
		}

		IResource projectSettings = getProject().findMember(".project");
		if (projectSettings != null)
		{
			projectSettings.setTeamPrivateMember(true);
		}

		if (!getProject().hasNature(ServoyProject.NATURE_ID) && !getProject().hasNature(ServoyResourcesProject.NATURE_ID))
		{
			try
			{
				getOperations().checkoutProject();
			}
			catch (final Exception ex)
			{
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						MessageDialog.openError(UIUtils.getActiveShell(), "Error", "Cannot checkout.\n" + ex.getMessage());
					}
				});
			}
		}
	}

	@Override
	public String getID()
	{
		return Activator.getTypeId();
	}

	public void deconfigure() throws CoreException
	{
		IResource projectSettings = getProject().findMember(".project");
		if (projectSettings != null)
		{
			projectSettings.setTeamPrivateMember(false);
			new WorkspaceJob("Saving workspace ...")
			{
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					getProject().getWorkspace().save(false, null);
					return Status.OK_STATUS;
				}
			}.schedule();
		}
		try
		{
			IResource jsBuildPath = getProject().findMember(".buildpath");
			if (jsBuildPath != null)
			{
				jsBuildPath.setTeamPrivateMember(false);
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		SolutionSubscriber.getInstance().getSynchronizer().flush(getProject(), IResource.DEPTH_INFINITE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.RepositoryProvider#getRuleFactory()
	 */
	@Override
	public IResourceRuleFactory getRuleFactory()
	{
		return RESOURCE_RULE_FACTORY;
	}


	public static boolean isAutomaticResourceUpdateOnCheckout()
	{
		ServoyResourcesProject servoyResourceProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();
		return Activator.getDefault().getPluginPreferences().getBoolean(PreferencesPage.AUTOMATIC_RESOURCE_UPDATE_ON_CHECKOUT_PROPERTY) &&
			servoyResourceProject != null && RepositoryProvider.getProvider(servoyResourceProject.getProject()) instanceof ServoyTeamProvider;
	}

	public static File getTemporaryDirectory()
	{
		// get the temporary directory once, if the user changes the prefs use the old value for cleanup.
		if (temporaryDirectory == null)
		{
			temporaryDirectory = new File(Activator.getDefault().getPluginPreferences().getString(PreferencesPage.TEMP_TEAM_DIRECTORY_PROPERTY));
		}
		return temporaryDirectory;
	}

	public static IProject createSolutionProject(RepositoryAccessPoint repositoryAP, String serverAddress, String user, String passHash, String solutionName,
		int solutionVersion, IProject resourcesProject, SolutionMetaData solutionMetaData, boolean isCheckoutModules) throws Exception
	{
		return createSolutionProject(repositoryAP, serverAddress, user, passHash, solutionName, solutionVersion, resourcesProject, solutionMetaData,
			isCheckoutModules, new ArrayList());
	}

	private static IProject createSolutionProject(RepositoryAccessPoint repositoryAP, String serverAddress, String user, String passHash, String solutionName,
		int solutionVersion, IProject resourcesProject, SolutionMetaData solutionMetaData, boolean isCheckoutModules, ArrayList alreadyCheckoutedSolutions)
		throws Exception
	{
		IProject project = null;


		if (checkProtection(solutionMetaData))
		{
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			project = root.getProject(solutionName);

			if (!project.exists()) project.create(null);
			if (!project.isOpen()) project.open(null);

			TeamProviderProperties teamProviderProperties = new TeamProviderProperties(new File(project.getWorkspace().getRoot().getLocation().toFile(),
				project.getName()));
			teamProviderProperties.setServerAddress(serverAddress);
			teamProviderProperties.setUser(user);
			teamProviderProperties.setPasswordHash(passHash);
			teamProviderProperties.setSolutionName(solutionName);
			teamProviderProperties.setSolutionVersion(solutionVersion);
			teamProviderProperties.setProtectionPasswordHash((solutionMetaData != null && !solutionMetaData.isProtected())
				? solutionMetaData.getProtectionPassword() : null);

			teamProviderProperties.setRepositoryUUID(repositoryAP.getRepository().getRepositoryUUID().toString());

			teamProviderProperties.save();


			if (resourcesProject != null)
			{
				IProjectDescription projectDescription = project.getDescription();
				projectDescription.setReferencedProjects(new IProject[] { resourcesProject });
				project.setDescription(projectDescription, null);
			}


			alreadyCheckoutedSolutions.add(solutionName);
			if (isCheckoutModules)
			{
				String[] modules = getSolutionModules(repositoryAP, solutionName, solutionVersion);

				for (String module : modules)
				{
					if (alreadyCheckoutedSolutions.indexOf(module) != -1) continue;

					ITeamRepository repository = repositoryAP.getRepository();
					SolutionMetaData moduleMetaData = (SolutionMetaData)repository.getRootObjectMetaData(module, IRepository.SOLUTIONS);
					if (moduleMetaData != null)
					{
						int release = solutionVersion == -1 ? -1 : moduleMetaData.getActiveRelease();

						if (moduleMetaData != null)
						{
							ServoyTeamProvider.createSolutionProject(repositoryAP, serverAddress, user, passHash, module, release, resourcesProject,
								moduleMetaData, isCheckoutModules, alreadyCheckoutedSolutions);
						}
					}
				}
			}

			RepositoryProvider.map(project, Activator.getTypeId());
		}

		return project;
	}

	public static IProject createResourcesProject(String name, RepositoryAccessPoint repositoryAP, String serverAddress, String user, String passHash)
		throws Exception
	{
		String projectName = name;
		IProject resourcesProject = null;

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		// create resources project if needed
		if (projectName == null)
		{
			// check if there is already a resource project checked out from that server
			ServoyResourcesProject[] resourceProjects = ServoyModelManager.getServoyModelManager().getServoyModel().getResourceProjects();
			String repositoryUUID = repositoryAP.getRepository().getRepositoryUUID().toString();
			RepositoryProvider repoProvider;
			IProject resourceProject;
			for (ServoyResourcesProject servoyResourceProject : resourceProjects)
			{
				resourceProject = servoyResourceProject.getProject();
				repoProvider = RepositoryProvider.getProvider(resourceProject);
				if (repoProvider instanceof ServoyTeamProvider)
				{
					TeamProviderProperties teamProviderProperties = new TeamProviderProperties(new File(
						resourceProject.getWorkspace().getRoot().getLocation().toFile(), resourceProject.getName()));
					teamProviderProperties.load();
					if (repositoryUUID.equals(teamProviderProperties.getRepositoryUUID()))
					{
						projectName = resourceProject.getName();
						break;
					}
				}
			}
			if (projectName == null)
			{
				projectName = ServoyTeamProvider.generateResourceProjectName(serverAddress, null);
				if (root.getProject(projectName).exists()) projectName = ServoyTeamProvider.generateResourceProjectName(serverAddress, repositoryUUID);
			}
		}
		resourcesProject = root.getProject(projectName);
		if (!resourcesProject.exists()) resourcesProject.create(null);
		if (!resourcesProject.isOpen()) resourcesProject.open(null);

		RepositoryProvider repoProvider = RepositoryProvider.getProvider(resourcesProject);
		if (repoProvider instanceof ServoyTeamProvider)
		{
			if (isAutomaticResourceUpdateOnCheckout()) // do a resource update on checkout
			{
				((ServoyTeamProvider)repoProvider).getProjectToTempDir(true);
				((ServoyTeamProvider)repoProvider).getOperations().update(new IResource[] { resourcesProject }, IResource.DEPTH_INFINITE, false, null);
				//((ServoyTeamProvider)repoProvider).doUpdate(new SimpleResourceMapping[] { new SimpleResourceMapping(resourcesProject) });
			}
		}
		else
		{
			TeamProviderProperties teamProviderProperties = new TeamProviderProperties(new File(
				resourcesProject.getWorkspace().getRoot().getLocation().toFile(), resourcesProject.getName()));
			teamProviderProperties.setServerAddress(serverAddress);
			teamProviderProperties.setUser(user);
			teamProviderProperties.setPasswordHash(passHash);
			teamProviderProperties.setSolutionName(resourcesProject.getName());
			teamProviderProperties.setRepositoryUUID(repositoryAP.getRepository().getRepositoryUUID().toString());

			teamProviderProperties.save();
			WorkspaceFileAccess resourceWorkspaceFileAccess = new WorkspaceFileAccess(resourcesProject.getWorkspace());
			// delete local repo settings, as we'll use the remote settings to avoid conflicts on first share
			RepositorySettingsDeserializer.deleteSettings(resourceWorkspaceFileAccess, projectName);

			RepositoryProvider.map(resourcesProject, Activator.getTypeId());
		}

		return resourcesProject;
	}

	public static void checkoutSelectedSolution(NewSolutionWizard.Result wizardResult) throws Exception
	{
		String serverAddress = wizardResult.getServerAddress();
		String user = wizardResult.getUser();
		String passHash = wizardResult.getPassHash();

		SolutionMetaData selectedSolutionMetaData = wizardResult.getSelectedSolutionMetaData();
		String selectedSolution = wizardResult.getSelectedSolution();
		int selectedVersion = wizardResult.getSelectedVersion();
		boolean isCheckoutModules = wizardResult.isCheckoutModules();


		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(selectedSolution);

		if (project.exists())
		{
			throw new Exception("Project with name '" + selectedSolution + "' already exist in the workspace");
		}
		else
		{
			RepositoryAccessPoint repositoryAP = RepositoryAccessPoint.getInstance(serverAddress, user, passHash);
			repositoryAP.checkRemoteRepositoryVersion();
			IProject resourcesProject = ServoyTeamProvider.createResourcesProject(null, repositoryAP, serverAddress, user, passHash);

			ServoyTeamProvider.createSolutionProject(repositoryAP, serverAddress, user, passHash, selectedSolution, selectedVersion, resourcesProject,
				selectedSolutionMetaData, isCheckoutModules);
		}
	}

	private static boolean bCheckProtection;

	private static boolean checkProtection(final SolutionMetaData solutionMetaData)
	{
		if (solutionMetaData == null || solutionMetaData.isProtected()) return true;

		bCheckProtection = false;
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				final PasswordInputDialog protectionDlg = new PasswordInputDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), "Protection",
					"Please enter protection password for solution : '" + solutionMetaData.getName() + "'", "", null);
				protectionDlg.setBlockOnOpen(true);
				if (protectionDlg.open() == Window.OK)
				{
					String passHashed = ApplicationServerSingleton.get().calculateProtectionPassword(solutionMetaData, protectionDlg.getValue());
					if (passHashed.equals(solutionMetaData.getProtectionPassword()))
					{
						bCheckProtection = true;
					}
					else
					{
						MessageDialog.openError(PlatformUI.getWorkbench().getDisplay().getActiveShell(), "Checkout fails", "Password is incorrect.");
					}
				}
			}
		});

		return bCheckProtection;
	}

	private static String[] getSolutionModules(RepositoryAccessPoint repositoryAP, String solutionName, int release) throws Exception
	{
		List<String> modules = new ArrayList<String>();
		if (repositoryAP != null)
		{
			try
			{
				Solution solution = (Solution)repositoryAP.getRepository().getRootObject(solutionName, IRepository.SOLUTIONS, release);
				String modulesNames = solution.getModulesNames();
				if (modulesNames != null)
				{
					StringTokenizer st = new StringTokenizer(modulesNames, ",");
					while (st.hasMoreTokens())
					{
						modules.add(st.nextToken().trim());
					}
				}
			}
			catch (UnmarshalException ex)
			{
				ServoyLog.logError("getSolutionModules - unmarshal", ex);
				throw new RuntimeException("The remote Servoy version is different from the local one");
			}
			catch (Exception ex)
			{
				ServoyLog.logError("Could not  get module names for solution " + solutionName, ex);
				throw ex;
			}
		}

		return modules.toArray(new String[modules.size()]);
	}

	public void getResourceProjectToTempDir() throws Exception
	{
		ServoyResourcesProject servoyResourceProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();
		if (RepositoryProvider.getProvider(servoyResourceProject.getProject()) instanceof ServoyTeamProvider)
		{
			File resourceTempDir = new File(ServoyTeamProvider.getTemporaryDirectory(), servoyResourceProject.getProject().getName());
			forceDelete(resourceTempDir);

			getOperations().writeResourceProjectToDir(new IOFileAccess(ServoyTeamProvider.getTemporaryDirectory()), true);
		}

	}

	public void getProjectToTempDir(boolean bResourceProject) throws Exception
	{
		deleteProjectFromTempDir();
		if (ServoyTeamProvider.getTemporaryDirectory() != null)
		{
			getOperations().writeProjectToDir(new IOFileAccess(ServoyTeamProvider.getTemporaryDirectory()), true, bResourceProject);
		}
	}

	private void deleteProjectFromTempDir()
	{
		String solutionName = getOperations().getTeamProviderProperties().getSolutionName();

		if (ServoyTeamProvider.getTemporaryDirectory() != null && solutionName != null)
		{
			File solutionTempDir = new File(ServoyTeamProvider.getTemporaryDirectory(), solutionName);
			forceDelete(solutionTempDir);
		}
	}

	public static void cleanup()
	{
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(ServoyTeamProvider.PROJECT_DELETE_LISTENER);
		ServoyTeamProvider.forceDelete(ServoyTeamProvider.getTemporaryDirectory());
		RepositoryAccessPoint.clear();
	}

	public static void startup()
	{
		Preferences store = Activator.getDefault().getPluginPreferences();
		store.setDefault(PreferencesPage.AUTOMATIC_RESOURCE_UPDATE_ON_CHECKOUT_PROPERTY, true);
		store.setDefault(PreferencesPage.TEMP_TEAM_DIRECTORY_PROPERTY, new File(System.getProperty("user.home"), ".tpdir").getPath());

		IIgnoreInfo[] ignores = Team.getAllIgnores();
		ArrayList<String> ignorePatterns = new ArrayList<String>();
		ArrayList<Boolean> ignorePatternsStatus = new ArrayList<Boolean>();
		if (ignores != null)
		{
			String ignorePattern;
			Boolean ignoreStatus;
			for (IIgnoreInfo element : ignores)
			{
				ignorePattern = element.getPattern();
				ignoreStatus = new Boolean(element.getEnabled());

				if (!ignorePattern.equals(".stp") && !ignorePattern.equals("*.obj"))
				{
					ignorePatterns.add(ignorePattern);
					ignorePatternsStatus.add(ignoreStatus);
				}
			}
		}

		ignorePatterns.add(".stp");
		ignorePatternsStatus.add(new Boolean(true));
		ignorePatterns.add("*.obj");
		ignorePatternsStatus.add(new Boolean(false));

		boolean[] bIgnorePatternsStatus = new boolean[ignorePatternsStatus.size()];
		int i = 0;
		for (Boolean b : ignorePatternsStatus)
			bIgnorePatternsStatus[i++] = b.booleanValue();

		Team.setAllIgnores(ignorePatterns.toArray(new String[0]), bIgnorePatternsStatus);
		Team.getFileContentManager().addExtensionMappings(
			new String[] { SolutionSerializer.FORM_FILE_EXTENSION.substring(1), SolutionSerializer.JS_FILE_EXTENSION_WITHOUT_DOT, SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION.substring(1), SolutionSerializer.RELATION_FILE_EXTENSION.substring(1), SolutionSerializer.VALUELIST_FILE_EXTENSION.substring(1), SolutionSerializer.TABLENODE_FILE_EXTENSION.substring(1), SolutionSerializer.STYLE_FILE_EXTENSION.substring(1), EclipseUserManager.SECURITY_FILE_EXTENSION_WITHOUT_DOT, EclipseMessages.MESSAGES_EXTENSION.substring(1), DataModelManager.COLUMN_INFO_FILE_EXTENSION },
			new int[] { Team.TEXT, Team.TEXT, Team.TEXT, Team.TEXT, Team.TEXT, Team.TEXT, Team.TEXT, Team.TEXT, Team.TEXT, Team.TEXT, });


		ResourcesPlugin.getWorkspace().addResourceChangeListener(ServoyTeamProvider.PROJECT_DELETE_LISTENER);

		// Have to run as a separate job because this plugin holds a lock in RepositoryProvider.mappingLock and ServoyModel-create may
		// block on requesting the main swt thread (causes deadlock when Project Explorer is shown at startup)
		new Job("Check local repository")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				checkIfLocalRepositoryIsUsedAndExist();
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	private static void checkIfLocalRepositoryIsUsedAndExist()
	{
		Settings settings = ServoyModel.getSettings();
		String s_initRepAsTeamProvider = settings.getProperty("servoy.application_server.startRepositoryAsTeamProvider", "true");
		boolean initRepAsTeamProvider = Utils.getAsBoolean(s_initRepAsTeamProvider);
		if (initRepAsTeamProvider)
		{
			try
			{
				IServerInternal repository_server = (IServerInternal)ApplicationServerSingleton.get().getServerManager().getServer(IServer.REPOSITORY_SERVER);
				if (repository_server != null && repository_server.getConfig().isEnabled())
				{
					int retries = Utils.getAsInteger(settings.getProperty("developer.maxRepositoryConnectRetries", "5")); //$NON-NLS-1$ //$NON-NLS-2$
					repository_server.testConnection(retries);//wait for db to come online
					repository_server.getRepository();
				}
			}
			catch (RepositoryException ex)
			{
				boolean no_repository_found = ex.getErrorCode() == ServoyException.InternalCodes.ERROR_NO_REPOSITORY_IN_DB;
				boolean old_repository_found = ex.getErrorCode() == ServoyException.InternalCodes.ERROR_OLD_REPOSITORY_IN_DB;
				boolean canUpgrade = (no_repository_found || old_repository_found);
				if (canUpgrade)
				{
					Display d = Display.getCurrent();
					if (d == null) d = Display.getDefault();
					final Display dd = d;
					dd.asyncExec(new Runnable()
					{
						public void run()
						{
							Shell shell = dd.getActiveShell();
							boolean ok = MessageDialog.openConfirm(shell, "Confirm repository upgrade", "Old or no repository version found in '" +
								IServer.REPOSITORY_SERVER +
								"' connection, do you want to upgrade the repository?\nYou might want to backup your database first before continuing.");
							if (ok)
							{
								IServerInternal repository_server = (IServerInternal)ApplicationServerSingleton.get().getServerManager().getServer(
									IServer.REPOSITORY_SERVER);
								try
								{
									repository_server.createRepositoryTables();
									MessageDialog.openInformation(shell, "Repository", "Succesfully completed, restart required!");
								}
								catch (RepositoryException e)
								{
									MessageDialog.openError(shell, "Repository error", "Cannot create/upgrade repository");
									ServoyLog.logError("Failed to create/upgrade repository.", e);
								}
							}
						}
					});
				}
				else
				{
					ServoyLog.logError("Failed local repository check", ex);
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError("Failed local repository check", ex);
			}

			// the in-process repository is only meant to work by itself - so all projects in the workspace should
			// either not be attached to team or attached to the in-process repository (because database information
			// and sequence provider are the standard table based ones - using the in-process repository - not the resources project)
			TeamShareMonitor tsm = ServoyModelManager.getServoyModelManager().getServoyModel().getTeamShareMonitor();
			if (tsm != null)
			{
				// should not be null because initRepAsTeamProvider = true
				tsm.setShareMonitorExtension(new TeamShareMonitorExtension()
				{

					public boolean shouldWarnForProject(IProject project, RepositoryProvider provider)
					{
						if (provider instanceof ServoyTeamProvider)
						{
							return !((ServoyTeamProvider)provider).isUsingInprocessApplicationServer();
						}
						return true;
					}

				});
			}
		}
	}

	/**
	 * Delete file/dir even if dir is not empty
	 * 
	 * @param file/dir to delete
	 */
	private static void forceDelete(File file)
	{
		if (file == null || !file.exists()) return;
		if (file.isDirectory())
		{
			File[] dirFiles = file.listFiles();
			for (File element : dirFiles)
				forceDelete(element);
		}

		file.delete();
	}

	public RepositoryOperations getOperations()
	{
		if (repositoryOperations == null)
		{
			TeamProviderProperties projectTeamProperties = new TeamProviderProperties(new File(getProject().getWorkspace().getRoot().getLocation().toFile(),
				getProject().getName()));
			if (projectTeamProperties.load()) repositoryOperations = new RepositoryOperations(this, projectTeamProperties);
		}

		return repositoryOperations;
	}

	/**
	 * Return the resource variant for the local resource.
	 * 
	 * @param resource the resource
	 * @return the resource variant
	 */
	public IResourceVariant getResourceVariant(IResource resource)
	{
		File file = getFile(resource);
		if (file == null || !fileExists(file)) return null;

		return new SolutionResourceVariant(file);
	}

	// case sensitive file exists
	private boolean fileExists(File file)
	{
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("windows"))
		{
			File parent = file.getParentFile();
			if (parent != null)
			{
				String[] dirFiles = parent.list();
				if (dirFiles != null)
				{
					for (String dirFile : dirFiles)
					{
						if (file.getName().equals(dirFile)) return true;
					}
				}
			}
			return false;
		}
		else return file.exists();
	}


	/**
	 * Return the resource variant for the local resource using the bytes to identify the variant.
	 * 
	 * @param resource the resource
	 * @param bytes the bytes that identify the resource variant
	 * @return the resource variant handle
	 */
	public IResourceVariant getResourceVariant(IResource resource, byte[] bytes)
	{
		if (bytes == null) return null;

		File baseFile = getBaseResourceFile(resource);
		if (baseFile != null)
		{
			SolutionResourceVariant baseVariant = new SolutionResourceVariant(baseFile);
			byte[] baseBytes = baseVariant.asBytes();

			if (baseBytes.length == bytes.length)
			{
				boolean bytesAreEquals = true;
				for (int i = 0; i < baseBytes.length; i++)
				{
					if (baseBytes[i] != bytes[i])
					{
						bytesAreEquals = false;
						break;
					}
				}
				if (bytesAreEquals) return baseVariant;
			}
		}

		File file = getFile(resource);
		if (file == null) return null;
		return new SolutionResourceVariant(file, bytes);
	}

	/**
	 * Return the <code>java.io.File</code> that the given resource maps to. Return <code>null</code> if the resource is not a child of this provider's
	 * project.
	 * 
	 * @param resource the resource
	 * @return the file that the resource maps to.
	 */
	public File getFile(IResource resource)
	{
		if (resource.getProject().equals(getProject()))
		{
			return new File(ServoyTeamProvider.getTemporaryDirectory(), resource.getFullPath().toOSString()); // resource.getProjectRelativePath().toOSString()
		}
		return null;
	}

	synchronized void clearBaseFileAccess()
	{
		baseFileAccess = null;
	}

	public synchronized void writeBaseResource(IResource resource)
	{
		if (baseFileAccess == null) return;
		try
		{
			if (resource.getType() == IResource.FILE)
			{
				byte[] content = workspaceFileAccess.getContents(resource.getFullPath().toOSString());
				baseFileAccess.setContents(resource.getFullPath().toOSString(), content);
			}
			else
			{
				baseFileAccess.createFolder(resource.getFullPath().toOSString());
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}

	public synchronized void deleteBaseResource(IResource resource)
	{
		if (baseFileAccess == null) return;
		baseFileAccess.deleteAll(resource.getFullPath().toOSString());
	}

	public synchronized File getBaseResourceFile(IResource resource)
	{
		if (baseFileAccess != null && baseFileAccess.exists(resource.getFullPath().toOSString()))
		{
			return new File(baseFileAccess.toOSPath(), resource.getFullPath().toOSString());
		}

		return null;
	}

	public static void doSynchronize(final ResourceMapping[] mappings)
	{
		try
		{
			WorkspaceJob synchrnizeJob = new WorkspaceJob("Synchronize")
			{
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					try
					{
						final ArrayList<ResourceMapping> flattenedResourceMapping = new ArrayList<ResourceMapping>();

						for (ResourceMapping rm : mappings)
						{
							if (rm instanceof CompositeResourceMapping)
							{
								CompositeResourceMapping crm = (CompositeResourceMapping)rm;
								ResourceMapping[] crms = crm.getMappings();
								for (ResourceMapping crmi : crms)
								{
									if (flattenedResourceMapping.indexOf(crmi) == -1) flattenedResourceMapping.add(crmi);
								}
							}
							else
							{
								if (flattenedResourceMapping.indexOf(rm) == -1) flattenedResourceMapping.add(rm);
							}
						}

						for (ResourceMapping rm : flattenedResourceMapping)
						{
							if (monitor.isCanceled()) return Status.CANCEL_STATUS;
							IProject mappingProject = ((IResource)rm.getModelObject()).getProject();
							ServoyTeamProvider stp = (ServoyTeamProvider)RepositoryProvider.getProvider(mappingProject);

							stp.getOperations().checkRemoteRepository();
							stp.getProjectToTempDir(mappingProject.hasNature(ServoyResourcesProject.NATURE_ID));
						}

						Display.getDefault().syncExec(new Runnable()
						{
							public void run()
							{
								ResourceVariantCache.disableCache(Activator.PLUGIN_ID);

								SubscriberScopeManager manager = SolutionOperation.createScopeManager(SolutionSubscriber.getInstance().getName(),
									flattenedResourceMapping.toArray(new ResourceMapping[0]));
								SolutionMergeContext context = new SolutionMergeContext(manager);
								SolutionSynchronizeParticipant participant = new SolutionSynchronizeParticipant(context);

								TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] { participant });
								participant.run(getTargetPart());
							}
						});
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
						throw new TeamException(ex.getMessage());
					}
					return Status.OK_STATUS;
				}
			};

			synchrnizeJob.setUser(true);
			// STP is using the eclipse repository singleton to serialize/deserialize
			// solutions, and during this the solutions ids are loaded into the singleton
			// so synch on ws root
			synchrnizeJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
			synchrnizeJob.schedule();
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
			MessageDialog.openError(null, "Error", ex.getMessage());
		}
	}

	public static void doCommit(ResourceMapping[] mappings)
	{
		ResourceMapping[] finalMappings = mappings;
		try
		{
			CommitOperation operation = new CommitOperation(getTargetPart(), SolutionOperation.createScopeManager("commit", finalMappings));
			operation.setOverwriteIncoming(false);
			operation.run();
		}
		catch (Exception ex)
		{
			ServoyLog.logError("Error on commit", ex);
			MessageDialog.openError(null, "Error on commit", ex.getMessage());
		}
	}


	public static void doUpdate(ResourceMapping[] mappings)
	{
		doUpdate(mappings, false);
	}

	public static void doUpdate(ResourceMapping[] mappings, boolean overwriteOutgoing)
	{
		ResourceMapping[] finalMappings = mappings;
		try
		{
			UpdateOperation operation = new UpdateOperation(getTargetPart(), SolutionOperation.createScopeManager("update", finalMappings));
			operation.setOverwriteOutgoing(overwriteOutgoing);
			operation.run();
		}
		catch (Exception ex)
		{
			ServoyLog.logError("Error on update", ex);
			MessageDialog.openError(null, "Error on update", ex.getMessage());
		}
	}


	public boolean isUsingInprocessApplicationServer()
	{
		return getOperations().isUsingInprocessApplicationServer();
	}

	/**
	 * @return IWorkbenchPart
	 */
	private static IWorkbenchPart getTargetPart()
	{
		IWorkbenchPage page = TeamUIPlugin.getActivePage();
		if (page != null)
		{
			return page.getActivePart();
		}

		return null;
	}

	static String generateResourceProjectName(String serverAddress, String serverUUID)
	{
		String serverName;
		int ddotIdx = serverAddress.indexOf(':');
		if (ddotIdx != -1) serverName = serverAddress.substring(0, ddotIdx);
		else serverName = serverAddress;

		StringBuffer sb = new StringBuffer(ServoyTeamProvider.RESOURCE_PREFIX);
		sb.append(serverName);
		if (serverUUID != null) sb.append('_').append(serverUUID);
		sb.append(ServoyTeamProvider.RESOURCE_SUFFIX);

		return sb.toString();
	}
}
