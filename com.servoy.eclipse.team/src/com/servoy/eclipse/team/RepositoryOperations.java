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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.dltk.javascript.core.JavaScriptNature;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.core.variants.ThreeWaySynchronizer;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.IOFileAccess;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.RepositorySettingsDeserializer;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.repository.StringResourceDeserializer;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ResourcesUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.team.subscriber.SolutionResourceVariant;
import com.servoy.eclipse.team.subscriber.SolutionSubscriber;
import com.servoy.eclipse.team.ui.PasswordInputDialog;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.I18NUtil;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ITeamRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StringResource;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

public class RepositoryOperations
{
	private final ServoyTeamProvider servoyTeamProvider;
	private final RepositoryAccessPoint repositoryAP;
	private final TeamProviderProperties projectTeamProperties;

	public RepositoryOperations(ServoyTeamProvider servoyTeamProvider, TeamProviderProperties projectTeamProperties)
	{
		this.servoyTeamProvider = servoyTeamProvider;
		this.projectTeamProperties = projectTeamProperties;
		repositoryAP = RepositoryAccessPoint.getInstance(projectTeamProperties.getServerAddress(), projectTeamProperties.getUser(),
			projectTeamProperties.getPassword());
	}

	public boolean isUsingInprocessApplicationServer()
	{
		return repositoryAP.isInprocessApplicationServer();
	}

	public TeamProviderProperties getTeamProviderProperties()
	{
		return this.projectTeamProperties;
	}

	/*
	 * Checks if the remote repository has been changed
	 */
	public void checkRepositoryChanged() throws Exception
	{
		String repositoryUUID = repositoryAP.getRepository().getRepositoryUUID().toString();
		String teamRepositoryUUID = projectTeamProperties.getRepositoryUUID();

		if (!repositoryUUID.equals(teamRepositoryUUID)) throw new Exception("Repository has been changed on the server.");
	}

	private void checkUserAndPasswd()
	{
		if (projectTeamProperties.getPassword() == null)
		{
			// ask for passwd
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					final PasswordInputDialog repoPasswdDlg = new PasswordInputDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(),
						"Servoy Team Provider", "Please enter password for '" + projectTeamProperties.getUser() + "@" +
							projectTeamProperties.getServerAddress() + "'", "", null);
					repoPasswdDlg.setBlockOnOpen(true);
					if (repoPasswdDlg.open() == Window.OK)
					{
						projectTeamProperties.setPassword(repoPasswdDlg.getValue());
					}
				}
			});
		}
	}

	/**
	 * Checks remote repository for correct UUID and version
	 */
	public void checkRemoteRepository() throws Exception
	{
		try
		{
			checkUserAndPasswd();
			checkRepositoryChanged();
			repositoryAP.checkRemoteRepositoryVersion();
		}
		catch (RepositoryAccessException ex)
		{
			// ask for passwd again on next request
			projectTeamProperties.setPassword(null);
			throw ex;
		}

	}

	public void checkoutProject() throws Exception
	{
		IProject project = servoyTeamProvider.getProject();
		TeamProviderProperties teamProviderProperties = new TeamProviderProperties(new File(project.getWorkspace().getRoot().getLocation().toFile(),
			project.getName()));
		teamProviderProperties.load();

		boolean isResourceProject = false;
		boolean needToSetNature = false;
		if (!project.hasNature(ServoyResourcesProject.NATURE_ID)) // only set nature if it is not an existing resource project
		{
			String projectName = project.getName();
			isResourceProject = projectName.equals(ServoyTeamProvider.generateResourceProjectName(teamProviderProperties.getServerAddress(), null)) ||
				projectName.equals(ServoyTeamProvider.generateResourceProjectName(teamProviderProperties.getServerAddress(),
					teamProviderProperties.getRepositoryUUID()));
			needToSetNature = true;
		}
		else
		{
			isResourceProject = true;
			needToSetNature = false;
		}


		servoyTeamProvider.getProjectToTempDir(isResourceProject);
		writeProjectToDir(new WorkspaceFileAccess(project.getWorkspace()), false, isResourceProject);

		if (repositoryAP.isInprocessApplicationServer())
		{
			IServerManagerInternal eclipseServerManager = ApplicationServerRegistry.get().getServerManager();
			eclipseServerManager.reloadServersTables();
		}

		project.refreshLocal(IResource.DEPTH_INFINITE, null);

		SolutionSubscriber solSub = SolutionSubscriber.getInstance();
		solSub.refresh(new IResource[] { project }, IResource.DEPTH_INFINITE, null);
		servoyTeamProvider.getOperations().setBase(project);

		if (needToSetNature)
		{
			IProjectDescription projectDescription = project.getDescription();
			if (isResourceProject) // find better way
			{
				projectDescription.setNatureIds(new String[] { ServoyResourcesProject.NATURE_ID });
			}
			else
			{
				projectDescription.setNatureIds(new String[] { ServoyProject.NATURE_ID, JavaScriptNature.NATURE_ID });
			}

			project.setDescription(projectDescription, null);
		}
	}

	private void setBase(IResource resource) throws TeamException
	{
		if (Team.isIgnoredHint(resource)) return;

		SolutionSubscriber solSub = SolutionSubscriber.getInstance();
		ThreeWaySynchronizer synchronizer = solSub.getSynchronizer();

		IResourceVariant resourceVariant = servoyTeamProvider.getResourceVariant(resource);
		if (resourceVariant != null)
		{
			synchronizer.setBaseBytes(resource, resourceVariant.asBytes());
		}
		else
		{
			ServoyLog.logWarning("Cannot set base bytes for " + resource, null);
			//throw new TeamException("Cannot set base bytes for " + resource);
		}

		IResource[] children = solSub.members(resource);
		for (IResource child : children)
		{
			if (!child.exists())
			{
				ServoyLog.logError("child does not exist in the workspace", null);
			}
			else
			{
				setBase(child);
			}
		}

	}


	public void writeProjectToDir(IFileAccess fileAccess, boolean isTmpDir, boolean bResourceProject) throws Exception
	{
		writeProjectToDir(servoyTeamProvider.getProject(), fileAccess, isTmpDir, bResourceProject);
	}

	public void writeResourceProjectToDir(IFileAccess fileAccess, boolean isTmpDir) throws Exception
	{
		IProject servoyResourceProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().getProject();
		if (RepositoryProvider.getProvider(servoyResourceProject) instanceof ServoyTeamProvider)
		{
			writeProjectToDir(servoyResourceProject, fileAccess, isTmpDir, true);
		}
	}


	private static void writeStringResource(ITeamRepository teamRepository, IFileAccess fileAccess, String resourcesProjectName, int objectTypeId)
		throws RemoteException, RepositoryException
	{
		RootObjectMetaData[] rootObjectMetaDataA = teamRepository.getRootObjectMetaDatasForType(objectTypeId);
		for (RootObjectMetaData styleMetaData : rootObjectMetaDataA)
		{
			String styleName = styleMetaData.getName();
			StringResource resource = (StringResource)teamRepository.getRootObject(styleName, objectTypeId, styleMetaData.getLatestRelease());

			StringResourceDeserializer.writeStringResource(resource, fileAccess, resourcesProjectName);
		}
	}

	public void writeProjectToDir(IProject project, final IFileAccess fileAccess, boolean isTmpDir, boolean bResourceProject) throws Exception
	{
		TeamProviderProperties pTeamProperties = new TeamProviderProperties(
			new File(project.getWorkspace().getRoot().getLocation().toFile(), project.getName()));
		pTeamProperties.load();
		RepositoryAccessPoint pRepositoryAP = RepositoryAccessPoint.getInstance(pTeamProperties.getServerAddress(), pTeamProperties.getUser(),
			pTeamProperties.getPassword());

		try
		{
			ITeamRepository repo = pRepositoryAP.getRepository();
			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			EclipseRepository eclipseRepository = (EclipseRepository)ServoyModel.getDeveloperRepository();

			if (bResourceProject)
			{
				// settings
				RepositorySettingsDeserializer.writeRepositoryUUID(fileAccess, project.getProject().getName(),
					UUID.fromString(pTeamProperties.getRepositoryUUID()));

				// styles
				writeStringResource(repo, fileAccess, project.getName(), IRepository.STYLES);

				// templates
				writeStringResource(repo, fileAccess, project.getName(), IRepository.TEMPLATES);

				// serialize repository servers tables
				DataModelManager dataModelManager = new DataModelManager(project, ServoyModel.getServerManager());


				String[] remoteServersNames = repo.getServerNames(false);
				String[] localServerNames = eclipseRepository.getServerNames(false);
				String[] commonServerNames = intersect(remoteServersNames, localServerNames);

				IServer remoteServer;
				ITable table;
				List<String> serverTablesNames;
				for (String serverName : commonServerNames)
				{
					try
					{
						remoteServer = repo.getServer(serverName);
						if (remoteServer != null && remoteServer.isValid())
						{
							serverTablesNames = remoteServer.getTableAndViewNames(true);

							for (String serverTableName : serverTablesNames)
							{
								table = remoteServer.getTable(serverTableName);
								dataModelManager.serializeAllColumnInfo((Table)table, fileAccess, project.getName());

							}
						}
						else
						{
							ServoyLog.logError("Cannot get server information from remote server " + serverName + ". The server is probably invalid.", null);
						}
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
				}
				dataModelManager.dispose();

				writeRepositorySecurityInfoToDir(pRepositoryAP, fileAccess, project.getName());

				// write i18n messages
				String[] i18nDatasources = pRepositoryAP.getRepository().getI18NDatasources();
				String[] serverTableNames;
				for (String i18nDatasource : i18nDatasources)
				{
					serverTableNames = DataSourceUtils.getDBServernameTablename(i18nDatasource);
					writeI18NMessages(serverTableNames[0], serverTableNames[1], pRepositoryAP, fileAccess, project);
				}

				//write disabled/invalid servers from local workspace in order to keep the local dbi files
				if (isTmpDir)
				{
					final IResource datasourcesDir = project.findMember(SolutionSerializer.DATASOURCES_DIR_NAME);
					if (datasourcesDir != null)
					{
						datasourcesDir.accept(new IResourceVisitor()
						{
							public boolean visit(IResource resource) throws CoreException
							{
								if (!resource.equals(datasourcesDir))
								{
									String dbServerTempFolder = resource.getFullPath().toOSString();
									if (!fileAccess.exists(dbServerTempFolder))
									{
										try
										{
											fileAccess.createFolder(dbServerTempFolder);
										}
										catch (IOException e)
										{
											ServoyLog.logError(e);
										}
										copyFolderTxtFiles(fileAccess, resource.getLocation().toFile(), dbServerTempFolder);
									}
								}
								return true;
							}

						}, IResource.DEPTH_ONE, false);
					}
				}
			}
			else
			{
				Solution solution = (Solution)repo.getRootObject(pTeamProperties.getSolutionName(), IRepository.SOLUTIONS, pTeamProperties.getSolutionVersion());
				if (solution != null)
				{
					//					solution.setRevisionNumber(projectTeamProperties.getSolutionVersion());

					String protectionPassHash = pTeamProperties.getProtectionPasswordHash();
					if (protectionPassHash != null) ((SolutionMetaData)solution.getMetaData()).setProtectionPassword(protectionPassHash);
					((SolutionMetaData)solution.getMetaData()).setFileVersion(AbstractRepository.repository_version);

					eclipseRepository.loadForeignElementsIDs(solution);

					SolutionSerializer.writePersist(solution, fileAccess, eclipseRepository, true, false, true);
					writeSolutionSecurityInfoToDir(pRepositoryAP, solution, fileAccess);
					eclipseRepository.clearForeignElementsIds();
				}
				else
				{
					fileAccess.createFolder(pTeamProperties.getSolutionName());
				}
			}
		}
		catch (UnmarshalException ex)
		{
			ServoyLog.logError("writeProjectToDir - unmarshal", ex);
			throw new RuntimeException("The remote Servoy version is different from the local one");
		}
		catch (Exception ex)
		{
			ServoyLog.logError("writeProjectToDir", ex);
			throw ex;
		}
	}

	public void writeProjectToRepository(List<IResource> changedResources) throws Exception
	{
		writeProjectToRepository(servoyTeamProvider.getProject(), changedResources);
	}

	private void writeProjectToRepository(IProject project, List<IResource> changedResources) throws Exception
	{
		List<String> projectNatures = Arrays.asList(project.getDescription().getNatureIds());
		boolean bResourceProject = projectNatures.indexOf(ServoyResourcesProject.NATURE_ID) > -1;
		boolean bSolutionProject = projectNatures.indexOf(ServoyProject.NATURE_ID) > -1;

		if (!bResourceProject && !bSolutionProject)
		{
			ServoyLog.logWarning("writeProjectToRepository - project nature is unknown", null);
			return;
		}

		TeamProviderProperties pTeamProperties = new TeamProviderProperties(
			new File(project.getWorkspace().getRoot().getLocation().toFile(), project.getName()));
		pTeamProperties.load();
		RepositoryAccessPoint pRepositoryAP = RepositoryAccessPoint.getInstance(pTeamProperties.getServerAddress(), pTeamProperties.getUser(),
			pTeamProperties.getPassword());

		File tempWsDir = ServoyTeamProvider.getTemporaryDirectory();

		List<File> changedFiles = new ArrayList<File>();
		List<String> changedStyles = new ArrayList<String>();
		List<String> changedTemplates = new ArrayList<String>();
		List<File> changedDBServers = new ArrayList<File>();
		List<File> changedSecurity = new ArrayList<File>();
		List<File> changedMessages = new ArrayList<File>();
		for (IResource resourceFile : changedResources)
		{
			String name = resourceFile.getName();
			if (name.endsWith(SolutionSerializer.STYLE_FILE_EXTENSION))
			{
				changedStyles.add(name.substring(0, name.length() - SolutionSerializer.STYLE_FILE_EXTENSION.length()));
			}
			else if (name.endsWith(SolutionSerializer.TEMPLATE_FILE_EXTENSION))
			{
				changedTemplates.add(name.substring(0, name.length() - SolutionSerializer.TEMPLATE_FILE_EXTENSION.length()));
			}
			else if (name.endsWith(DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT))
			{
				changedDBServers.add(resourceFile.getLocation().toFile());
			}
			else if (name.endsWith(WorkspaceUserManager.SECURITY_FILE_EXTENSION))
			{
				changedSecurity.add(resourceFile.getLocation().toFile());
			}
			else if (name.endsWith(EclipseMessages.MESSAGES_EXTENSION))
			{
				changedMessages.add(resourceFile.getLocation().toFile());
			}
			else
			{
				changedFiles.add(resourceFile.getLocation().toFile());
			}
		}

		try
		{
			ITeamRepository repo = pRepositoryAP.getRepository();
			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			final EclipseRepository eclipseRepository = (EclipseRepository)ServoyModel.getDeveloperRepository();

			if (bResourceProject)
			{
				List<RootObjectMetaData> metaDatas = new ArrayList<RootObjectMetaData>();
				for (RootObjectMetaData metaData : StringResourceDeserializer.deserializeMetadatas(eclipseRepository, new File(tempWsDir, project.getName()),
					project.getName(), IRepository.STYLES))
				{
					if (changedStyles.contains(metaData.getName()))
					{
						changedStyles.remove(metaData.getName());
						metaDatas.add(metaData);
					}
				}
				for (RootObjectMetaData metaData : StringResourceDeserializer.deserializeMetadatas(eclipseRepository, new File(tempWsDir, project.getName()),
					project.getName(), IRepository.TEMPLATES))
				{
					if (changedTemplates.contains(metaData.getName()))
					{
						changedTemplates.remove(metaData.getName());
						metaDatas.add(metaData);
					}
				}
				for (RootObjectMetaData metaData : metaDatas)
				{
					StringResource remoteResource = null;
					RootObjectMetaData remoteRootObjectMetaData = repo.getRootObjectMetaData(metaData.getName(), metaData.getObjectTypeId());
					if (remoteRootObjectMetaData != null) remoteResource = (StringResource)repo.getRootObject(metaData.getName(), metaData.getObjectTypeId(),
						remoteRootObjectMetaData.getLatestRelease());
					StringResource resource = StringResourceDeserializer.readStringResource(eclipseRepository, new File(tempWsDir, project.getName()),
						project.getName(), remoteResource == null ? metaData : (RootObjectMetaData)remoteResource.getMetaData());

					if (remoteResource != null)
					{
						resource.setReleaseNumber(remoteResource.getReleaseNumber());
						resource.setRevisionNumber(remoteResource.getRevisionNumber());
					}
					resource.flagChanged();
					resource.setRepository(null);
					repo.updateTeamRootObject(resource);
				}

				// delete removed styles
				for (String changedStyle : changedStyles)
				{
					repo.deleteStringResource(changedStyle, IRepository.STYLES);
				}
				// delete removed templates
				for (String changedTemplate : changedTemplates)
				{
					repo.deleteStringResource(changedTemplate, IRepository.TEMPLATES);
				}

				// write servers tables
				for (File changedDBServer : changedDBServers)
				{
					String dbTableName = changedDBServer.getName();
					dbTableName = dbTableName.substring(0, dbTableName.length() - DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT.length()); // substract .dbi from name
					String dbServerName = changedDBServer.getParentFile().getName();

					IServer server = eclipseRepository.getServer(dbServerName);

					try
					{
						ITable table = server.getTable(dbTableName);
						repo.updateDataModel(dbServerName, dbTableName, (Table)table);
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
				}

				boolean securityInfoUpdated = false;
				String securityFile = WorkspaceUserManager.SECURITY_DIR + File.separator + WorkspaceUserManager.SECURITY_FILENAME;
				for (File changedSecFile : changedSecurity)
				{
					if (changedSecFile.getAbsolutePath().endsWith(securityFile))
					{
						updateSecurityInfo(pRepositoryAP);
						changedSecurity.remove(changedSecFile);
						securityInfoUpdated = true;
						break;
					}
				}

				if (changedSecurity.size() > 0 && !securityInfoUpdated) updateSecurityInfo(pRepositoryAP); // always update security info if table security is about to commit

				for (File changedSecFile : changedSecurity)
				{
					updateTableSecurityInfo(changedSecFile);
				}

				for (File changedMessageFile : changedMessages)
				{
					updateI18NMessages(pRepositoryAP, changedMessageFile.getName(), new IOFileAccess(tempWsDir));
				}
			}
			else
			{
				final Solution remoteSolution = (Solution)repo.getRootObject(pTeamProperties.getSolutionName(), IRepository.SOLUTIONS,
					pTeamProperties.getSolutionVersion());

				if (remoteSolution != null) eclipseRepository.loadForeignElementsIDs(remoteSolution);
				SolutionDeserializer solutionDeserializer = new SolutionDeserializer(eclipseRepository, null);

				SolutionMetaData smd = SolutionDeserializer.deserializeRootMetaData(eclipseRepository, new File(tempWsDir, pTeamProperties.getSolutionName()),
					pTeamProperties.getSolutionName());
				if (smd != null) smd.flagChanged();
				final Solution solution = solutionDeserializer.readSolution(new File(tempWsDir, smd.getName()), smd, changedFiles, true);
				if (solution != null)
				{
					solution.setRepository(repo);
					solution.setServerProxies(null);
					solution.setReleaseNumber(remoteSolution != null ? remoteSolution.getReleaseNumber() : 1);

					//clear any non changed media to prevent expensive uploads over RMI
					solution.acceptVisitor(new IPersistVisitor()
					{
						public Object visit(IPersist o)
						{
							if (o instanceof Media && !o.isChanged())
							{
								((Media)o).setPermMediaData(null);
							}

							return IPersistVisitor.CONTINUE_TRAVERSAL;
						}
					});

					repo.updateTeamRootObject(solution);
					if (changedSecurity.size() > 0) updateSecurityInfo(pRepositoryAP); // always update security info if form security is about to commit
					for (File changedSecFile : changedSecurity)
					{
						updateSolutionSecurityInfo(pRepositoryAP, solution.getName(), remoteSolution, changedSecFile);
					}
				}
				eclipseRepository.clearForeignElementsIds();
			}
		}
		catch (UnmarshalException ex)
		{
			ServoyLog.logError("writeProjectToRepository - unmarshal", ex);
			throw new RuntimeException("The remote Servoy version is different from the local one");
		}
		catch (Exception ex)
		{
			ServoyLog.logError("writeProjectToRepository", ex);
			throw ex;
		}
	}

	private void writeSolutionSecurityInfoToDir(RepositoryAccessPoint rap, Solution solution, IFileAccess fileAccess) throws Exception
	{
		// get remote user manager and use eclipse user manager for serialize
		IUserManager userManager = rap.getUserManager();
		TeamEclipseUserManager eclipseUserManager = new TeamEclipseUserManager(fileAccess, solution);
		eclipseUserManager.setWriteMode(WorkspaceUserManager.WRITE_MODE_MANUAL);

		// serialize form elements security entries
		IDataSet groups = userManager.getGroups(rap.getClientID());
		int groupsNr = groups.getRowCount();
		String groupsNames[] = new String[groupsNr];

		Object[] row;
		for (int i = 0; i < groupsNr; i++)
		{
			row = groups.getRow(i);
			groupsNames[i] = row[1].toString();
			eclipseUserManager.createGroup(ApplicationServerRegistry.get().getClientId(), groupsNames[i]);
		}

		for (String groupName : groupsNames)
		{
			Map<Object, Integer> securityAccess = userManager.getSecurityAccess(rap.getClientID(), new int[] { solution.getID() },
				new int[] { solution.getReleaseNumber() }, new String[] { groupName });
			Iterator<Map.Entry<Object, Integer>> securityAccessIte = securityAccess.entrySet().iterator();
			Map.Entry<Object, Integer> securityEntry;

			while (securityAccessIte.hasNext())
			{
				securityEntry = securityAccessIte.next();
				if (securityEntry.getKey() instanceof UUID) // form security entry
				{
					eclipseUserManager.setFormSecurityAccess(ApplicationServerRegistry.get().getClientId(), groupName, securityEntry.getValue(),
						(UUID)securityEntry.getKey(), solution.getName());
				}
			}
		}

		eclipseUserManager.writeSolutionSecurityInformationToDir(solution);
	}

	private void writeRepositorySecurityInfoToDir(RepositoryAccessPoint rap, IFileAccess fileAccess, String projectName) throws Exception
	{
		// get remote user manager and use eclipse user manager for serialize
		IUserManager userManager = rap.getUserManager();
		TeamEclipseUserManager eclipseUserManager = new TeamEclipseUserManager(fileAccess, projectName);
		eclipseUserManager.setWriteMode(WorkspaceUserManager.WRITE_MODE_MANUAL);

		// serialize security groups & users
		IDataSet groups = userManager.getGroups(rap.getClientID());
		int groupsNr = groups.getRowCount();
		String groupsNames[] = new String[groupsNr];

		Object[] row;
		for (int i = 0; i < groupsNr; i++)
		{
			row = groups.getRow(i);
			groupsNames[i] = row[1].toString();
			eclipseUserManager.createGroup(ApplicationServerRegistry.get().getClientId(), groupsNames[i]);
		}

		IDataSet users = userManager.getUsers(rap.getClientID());
		int usersNr = users.getRowCount();

		String userName;
		String userPassHash;
		String userUID;
		int userID;
		for (int i = 0; i < usersNr; i++)
		{
			row = users.getRow(i);
			userUID = row[0].toString();
			userName = row[1].toString();
			userPassHash = userManager.getUserPasswordHash(rap.getClientID(), userUID);

			userID = eclipseUserManager.createUser(ApplicationServerRegistry.get().getClientId(), userName, userPassHash, userUID, true);
			String[] userUIDGroups = userManager.getUserGroups(rap.getClientID(), userUID);
			if (userUIDGroups != null)
			{
				for (String userUIDGroup : userUIDGroups)
				{
					eclipseUserManager.addUserToGroup(ApplicationServerRegistry.get().getClientId(), userID,
						eclipseUserManager.getGroupId(ApplicationServerRegistry.get().getClientId(), userUIDGroup));
				}
			}
		}

		// serialize table security entries
		for (String groupName : groupsNames)
		{
			Map<Object, Integer> securityAccess = userManager.getSecurityAccess(rap.getClientID(), new int[0], new int[0], new String[] { groupName });
			Iterator<Map.Entry<Object, Integer>> securityAccessIte = securityAccess.entrySet().iterator();
			Map.Entry<Object, Integer> securityEntry;

			while (securityAccessIte.hasNext())
			{
				securityEntry = securityAccessIte.next();
				if (securityEntry.getKey() instanceof CharSequence) // table security entry
				{
					StringTokenizer tableSecKeyTokenizer = new StringTokenizer(securityEntry.getKey().toString(), ".");
					String connectionName = tableSecKeyTokenizer.nextToken();
					String tableName = tableSecKeyTokenizer.nextToken();
					String columnName = tableSecKeyTokenizer.nextToken();
					eclipseUserManager.setTableSecurityAccess(ApplicationServerRegistry.get().getClientId(), groupName, securityEntry.getValue(),
						connectionName, tableName, columnName);
				}
			}
		}

		eclipseUserManager.writeRepositorySecurityInformationToDir();
	}

	private void updateSecurityInfo(RepositoryAccessPoint rap) throws Exception
	{
		updateGroupsSecurityInfo(rap);
		updateUsersSecurityInfo(rap);
	}

	private void updateGroupsSecurityInfo(RepositoryAccessPoint rap) throws Exception
	{
		// get remote user manager
		IUserManager remoteUserManager = rap.getUserManager();
		// get local user manager
		IUserManager eclipseUserManager = ApplicationServerRegistry.get().getUserManager();

		// delete remote groups if locally were deleted
		String groupName;
		IDataSet remoteGroups = remoteUserManager.getGroups(rap.getClientID());
		int remoteGroupsNr = remoteGroups.getRowCount();
		for (int i = 0; i < remoteGroupsNr; i++)
		{
			Number groupID = (Number)remoteGroups.getRow(i)[0];
			groupName = (String)remoteGroups.getRow(i)[1];
			if (eclipseUserManager.getGroupId(ApplicationServerRegistry.get().getClientId(), groupName) == -1)
			{
				remoteUserManager.deleteGroup(rap.getClientID(), groupID.intValue());
			}
		}

		int remoteGroupID;
		IDataSet localGroups = eclipseUserManager.getGroups(ApplicationServerRegistry.get().getClientId());
		int localGroupsNr = localGroups.getRowCount();
		for (int i = 0; i < localGroupsNr; i++)
		{
			groupName = (String)localGroups.getRow(i)[1];

			if ((remoteGroupID = remoteUserManager.getGroupId(rap.getClientID(), groupName)) == -1)
			{
				// insert new group
				remoteUserManager.createGroup(rap.getClientID(), groupName);
			}
			else
			{
				// update group
				remoteUserManager.changeGroupName(rap.getClientID(), remoteGroupID, groupName);
			}
		}
	}

	private void updateUsersSecurityInfo(RepositoryAccessPoint rap) throws Exception
	{
		// get remote user manager
		IUserManager remoteUserManager = rap.getUserManager();
		// get local user manager
		IUserManager eclipseUserManager = ApplicationServerRegistry.get().getUserManager();


		// delete remote users if locally were deleted
		String userUUID;
		String userName;
		String userPass;
		IDataSet remoteUsers = remoteUserManager.getUsers(rap.getClientID());
		int remoteUsersNr = remoteUsers.getRowCount();
		for (int i = 0; i < remoteUsersNr; i++)
		{
			userUUID = (String)remoteUsers.getRow(i)[0];

			if (eclipseUserManager.getUserIdByUID(ApplicationServerRegistry.get().getClientId(), userUUID) == -1)
			{
				remoteUserManager.deleteUser(rap.getClientID(), userUUID);
			}
		}


		IDataSet localUsers = eclipseUserManager.getUsers(ApplicationServerRegistry.get().getClientId());
		int localUsersNr = localUsers.getRowCount();
		for (int i = 0; i < localUsersNr; i++)
		{
			userUUID = (String)localUsers.getRow(i)[0];
			userName = (String)localUsers.getRow(i)[1];
			userPass = eclipseUserManager.getUserPasswordHash(ApplicationServerRegistry.get().getClientId(), userUUID);

			String[] localUserGroups = eclipseUserManager.getUserGroups(ApplicationServerRegistry.get().getClientId(), userUUID);
			if (remoteUserManager.getUserIdByUID(rap.getClientID(), userUUID) == -1)
			{
				// insert new user
				// TODO CHECK, this call doesn't expect a hashed password!
				int remoteUserID = remoteUserManager.createUser(rap.getClientID(), userName, userPass, userUUID, true);
				if (localUserGroups != null)
				{
					for (String userGroupName : localUserGroups)
					{
						remoteUserManager.addUserToGroup(rap.getClientID(), remoteUserID, remoteUserManager.getGroupId(rap.getClientID(), userGroupName));
					}
				}
			}
			else
			{
				// update user
				remoteUserManager.changeUserName(rap.getClientID(), userUUID, userName);
				remoteUserManager.setPassword(rap.getClientID(), userUUID, userPass, false);


				String[] remoteUserGroups = remoteUserManager.getUserGroups(rap.getClientID(), userUUID);

				for (String remoteUserGroup : remoteUserGroups)
				{
					boolean removed = true;
					for (String localUserGroup : localUserGroups)
					{
						if (remoteUserGroup.equals(localUserGroup))
						{
							removed = false;
							break;
						}
					}
					if (removed)
					{
						remoteUserManager.removeUserFromGroup(rap.getClientID(), remoteUserManager.getUserIdByUID(rap.getClientID(), userUUID),
							remoteUserManager.getGroupId(rap.getClientID(), remoteUserGroup));
					}
				}

				for (String localUserGroup : localUserGroups)
				{
					boolean newUserGroup = true;
					for (String remoteUserGroup : remoteUserGroups)
					{
						if (remoteUserGroup.equals(localUserGroup))
						{
							newUserGroup = false;
							break;
						}
					}
					if (newUserGroup)
					{
						remoteUserManager.addUserToGroup(rap.getClientID(), remoteUserManager.getUserIdByUID(rap.getClientID(), userUUID),
							remoteUserManager.getGroupId(rap.getClientID(), localUserGroup));
					}
				}
			}

		}
	}

	private void updateTableSecurityInfo(File tableSecFile) throws Exception
	{
		String tableNameKey = tableSecFile.getParentFile().getName() + "." + tableSecFile.getName();
		int extIdx = tableNameKey.indexOf(WorkspaceUserManager.SECURITY_FILE_EXTENSION);
		if (extIdx != -1) tableNameKey = tableNameKey.substring(0, extIdx);

		// get remote user manager
		IUserManager remoteUserManager = repositoryAP.getUserManager();
		// get local user manager
		IUserManager eclipseUserManager = ApplicationServerRegistry.get().getUserManager();
		IDataSet groups = eclipseUserManager.getGroups(ApplicationServerRegistry.get().getClientId());
		int groupsNr = groups.getRowCount();
		String groupsNames[] = new String[groupsNr];

		Object[] row;
		for (int i = 0; i < groupsNr; i++)
		{
			row = groups.getRow(i);
			groupsNames[i] = row[1].toString();
		}

		for (String groupName : groupsNames)
		{
			Map<Object, Integer> securityAccess = eclipseUserManager.getSecurityAccess(ApplicationServerRegistry.get().getClientId(), new int[0], new int[0],
				new String[] { groupName });
			Map<Object, Integer> remoteSecurityAccess = remoteUserManager.getSecurityAccess(repositoryAP.getClientID(), new int[0], new int[0],
				new String[] { groupName });
			Iterator<Map.Entry<Object, Integer>> securityAccessIte;
			Map.Entry<Object, Integer> securityEntry;

			//check for removed security entries
			securityAccessIte = remoteSecurityAccess.entrySet().iterator();
			String sSecurityEntryKey;
			while (securityAccessIte.hasNext())
			{
				securityEntry = securityAccessIte.next();
				if (securityEntry.getKey() instanceof CharSequence) // table security entry
				{
					sSecurityEntryKey = securityEntry.getKey().toString();
					if (sSecurityEntryKey.startsWith(tableNameKey) && !securityAccess.containsKey(securityEntry.getKey()))
					{
						StringTokenizer tableSecKeyTokenizer = new StringTokenizer(securityEntry.getKey().toString(), ".");
						String connectionName = tableSecKeyTokenizer.nextToken();
						String tableName = tableSecKeyTokenizer.nextToken();
						String columnName = tableSecKeyTokenizer.nextToken();
						remoteUserManager.setTableSecurityAccess(repositoryAP.getClientID(), groupName, new Integer(IRepository.IMPLICIT_TABLE_ACCESS),
							connectionName, tableName, columnName);
					}
				}
			}

			securityAccessIte = securityAccess.entrySet().iterator();

			while (securityAccessIte.hasNext())
			{
				securityEntry = securityAccessIte.next();
				if (securityEntry.getKey() instanceof CharSequence) // table security entry
				{
					sSecurityEntryKey = securityEntry.getKey().toString();
					if (sSecurityEntryKey.startsWith(tableNameKey))
					{
						StringTokenizer tableSecKeyTokenizer = new StringTokenizer(securityEntry.getKey().toString(), ".");
						String connectionName = tableSecKeyTokenizer.nextToken();
						String tableName = tableSecKeyTokenizer.nextToken();
						String columnName = tableSecKeyTokenizer.nextToken();
						remoteUserManager.setTableSecurityAccess(repositoryAP.getClientID(), groupName, securityEntry.getValue(), connectionName, tableName,
							columnName);
					}
				}
			}
		}
	}

	private void updateSolutionSecurityInfo(RepositoryAccessPoint rap, String solutionName, Solution remoteSolution, File formSecFile) throws Exception
	{
		String formName = formSecFile.getName();
		int extIdx = formName.indexOf(WorkspaceUserManager.SECURITY_FILE_EXTENSION);
		if (extIdx != -1) formName = formName.substring(0, extIdx);


		// get remote user manager
		IUserManager remoteUserManager = rap.getUserManager();
		// get local user manager
		IUserManager eclipseUserManager = ApplicationServerRegistry.get().getUserManager();
		IDataSet groups = eclipseUserManager.getGroups(ApplicationServerRegistry.get().getClientId());
		int groupsNr = groups.getRowCount();
		String groupsNames[] = new String[groupsNr];

		Object[] row;
		for (int i = 0; i < groupsNr; i++)
		{
			row = groups.getRow(i);
			groupsNames[i] = row[1].toString();
		}


		Solution localSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName).getSolution();

		for (String groupName : groupsNames)
		{
			Map<Object, Integer> securityAccess = eclipseUserManager.getSecurityAccess(ApplicationServerRegistry.get().getClientId(),
				new int[] { localSolution.getID() }, new int[] { localSolution.getReleaseNumber() }, new String[] { groupName });
			Map<Object, Integer> remoteSecurityAccess = null;
			if (remoteSolution != null) remoteSecurityAccess = remoteUserManager.getSecurityAccess(rap.getClientID(), new int[] { remoteSolution.getID() },
				new int[] { remoteSolution.getReleaseNumber() }, new String[] { groupName });
			Iterator<Map.Entry<Object, Integer>> securityAccessIte;
			Map.Entry<Object, Integer> securityEntry;


			//check for removed security entries
			if (remoteSecurityAccess != null)
			{
				securityAccessIte = remoteSecurityAccess.entrySet().iterator();
				while (securityAccessIte.hasNext())
				{
					securityEntry = securityAccessIte.next();
					if (securityEntry.getKey() instanceof UUID) // form security entry
					{
						if (!securityAccess.containsKey(securityEntry.getKey()))
						{
							remoteUserManager.setFormSecurityAccess(rap.getClientID(), groupName, new Integer(IRepository.IMPLICIT_FORM_ACCESS),
								(UUID)securityEntry.getKey(), remoteSolution.getName());
						}
					}
				}
			}

			securityAccessIte = securityAccess.entrySet().iterator();
			while (securityAccessIte.hasNext())
			{
				securityEntry = securityAccessIte.next();
				if (securityEntry.getKey() instanceof UUID) // form security entry
				{
					remoteUserManager.setFormSecurityAccess(rap.getClientID(), groupName, securityEntry.getValue(), (UUID)securityEntry.getKey(),
						localSolution.getName());
				}
			}
		}
	}


	private void writeI18NMessages(String i18NServer, String i18NTable, RepositoryAccessPoint rap, IFileAccess fileAccess, IProject resourceProject)
		throws Exception
	{
		TreeMap<String, I18NUtil.MessageEntry> messages = loadSortedMessagesFromRepository(i18NServer, i18NTable, rap);
		EclipseMessages.writeMessages(i18NServer, i18NTable, messages, fileAccess, resourceProject);
	}

	private TreeMap<String, I18NUtil.MessageEntry> loadSortedMessagesFromRepository(String i18NServerName, String i18NTableName,
		RepositoryAccessPoint repositoryAccessPoint) throws Exception
	{
		IRepository repository = repositoryAccessPoint.getRepository();
		IDataServer dataServer = repositoryAccessPoint.getDataServer();
		String clientID = repositoryAccessPoint.getClientID();

		return I18NUtil.loadSortedMessagesFromRepository(repository, dataServer, clientID, i18NServerName, i18NTableName, null, null, null);
	}


	private void updateI18NMessages(RepositoryAccessPoint rap, String messagesFileNameWithEx, IFileAccess workspaceDir) throws Exception
	{
		String messagesFileName = messagesFileNameWithEx.substring(0, messagesFileNameWithEx.indexOf(EclipseMessages.MESSAGES_EXTENSION));
		int dotIdx = messagesFileName.indexOf(".");
		if (dotIdx > 0 && dotIdx < messagesFileName.length() - 1)
		{
			String i18NServer = messagesFileName.substring(0, dotIdx);
			String i18NTable = messagesFileName.substring(dotIdx + 1);
			dotIdx = i18NTable.indexOf(".");
			if (dotIdx != -1) // we have also the language appended to the name
			{
				i18NTable = i18NTable.substring(0, dotIdx);
			}

			TreeMap<String, I18NUtil.MessageEntry> messages = EclipseMessages.readMessages(i18NServer, i18NTable, workspaceDir);
			writeMessagesToRepository(i18NServer, i18NTable, rap, messages);
		}
	}

	private void writeMessagesToRepository(String i18NServerName, String i18NTableName, RepositoryAccessPoint rap,
		TreeMap<String, I18NUtil.MessageEntry> messages) throws Exception
	{
		IRepository repository = rap.getRepository();
		IDataServer dataServer = rap.getDataServer();
		String clientID = rap.getClientID();

		IServer i18NServer = repository.getServer(i18NServerName);
		Table i18NTable = null;
		if (i18NServer != null)
		{
			i18NTable = (Table)i18NServer.getTable(i18NTableName);
		}
		if (i18NTable == null) throw new Exception("Remote repository does not have an i18n table named : " + i18NServerName + "." + i18NTableName +
			"\nYou should create one manually if you want to be able to commit i18n messages.");
		I18NUtil.writeMessagesToRepository(i18NServerName, i18NTableName, repository, dataServer, clientID, messages, false, false, null, null, null);
	}

	/**
	 * Commit the given resources to the given depth by replacing the remote contents with the local workspace contents.
	 * 
	 * @param traversals the traversals that cover the resources to check in
	 * @param overrideIncoming indicate whether incoming remote changes should be replaced
	 * @param progress a progress monitor
	 * @throws TeamException
	 */
	public void commit(ResourceTraversal[] traversals, boolean overrideIncoming, IProgressMonitor monitor, List<IResource> changedResources,
		SynchronizeUpdater synchronizeUpdater) throws TeamException
	{
		try
		{
			// ensure the progress monitor is not null
			monitor = Policy.monitorFor(monitor);
			monitor.beginTask(null, 100 * traversals.length);
			for (ResourceTraversal traversal : traversals)
			{
				commit(traversal.getResources(), traversal.getDepth(), overrideIncoming, new SubProgressMonitor(monitor, 100), changedResources,
					synchronizeUpdater);
			}
		}
		finally
		{
			monitor.done();
		}
	}

	/**
	 * Commit the given resources to the given depth by replacing the remote contents with the local workspace contents.
	 * 
	 * @param resources the resources
	 * @param depth the depth of the operation
	 * @param overrideIncoming indicate whether incoming remote changes should be replaced
	 * @param progress a progress monitor
	 * @throws TeamException
	 */
	public void commit(IResource[] resources, int depth, boolean overrideIncoming, IProgressMonitor progress, List<IResource> changedResources,
		SynchronizeUpdater synchronizeUpdater) throws TeamException
	{
		try
		{
			// ensure the progress monitor is not null
			progress = Policy.monitorFor(progress);
			progress.beginTask("commit", 100);
			// Refresh the subscriber so we have the latest remote state
			SolutionSubscriber.getInstance().refresh(resources, depth, new SubProgressMonitor(progress, 30));
			internalCommit(resources, depth, overrideIncoming, new SubProgressMonitor(progress, 70), changedResources, synchronizeUpdater);
		}
		finally
		{
			progress.done();
		}
	}


	private void internalCommit(IResource[] resources, int depth, boolean overrideIncoming, IProgressMonitor progress, List<IResource> changedResources,
		SynchronizeUpdater synchronizeUpdater) throws TeamException
	{

		// order resources as the deletes are performed first,
		// because in windows we cannot have the same resource with diff
		// case in the same folder
		Arrays.sort(resources, new Comparator<IResource>()
		{

			public int compare(IResource o1, IResource o2)
			{
				if (!o1.exists() && o2.exists()) return -1;
				else if (o1.exists() && !o2.exists()) return 1;

				// if resources are at the same level, consider the IFile first, then the IContainer
				if (o1.getFullPath().segmentCount() == o2.getFullPath().segmentCount())
				{
					if (o1 instanceof IContainer && o2 instanceof IFile)
					{
						return 1;
					}
					else
					{
						return -1;
					}
				}

				// first have the container then the children, we achive this by the pathname compare
				String path1 = o1.getFullPath().toOSString();
				String path2 = o2.getFullPath().toOSString();

				return path1.compareTo(path2);

			}


		});

		// ensure the progress monitor is not null
		progress = Policy.monitorFor(progress);
		progress.beginTask("commit", IProgressMonitor.UNKNOWN);
		for (IResource element : resources)
		{
			Policy.checkCanceled(progress);
			if (element.getType() == IResource.FILE)
			{
				if (internalCommit((IFile)element, overrideIncoming, progress, synchronizeUpdater))
				{
					if (changedResources != null)
					{
						if (changedResources.indexOf(element) == -1) changedResources.add(element);
					}
				}
			}
			else if (depth > 0)
			{ //Assume that resources are either files or containers.
				internalCommit((IContainer)element, depth, overrideIncoming, progress, changedResources, synchronizeUpdater);
			}
			progress.worked(1);
		}
		progress.done();
	}

	/**
	 * Put the file if the sync state allows it.
	 * 
	 * @param localFile the local file
	 * @param overrideIncoming whether incoming changes should be overwritten
	 * @param progress a progress monitor
	 * @return whether the localFile was copied for upload
	 * @throws TeamException
	 */
	private boolean internalCommit(IFile localFile, boolean overrideIncoming, IProgressMonitor progress, SynchronizeUpdater synchronizeUpdater)
		throws TeamException
	{
		if (Team.isIgnoredHint(localFile)) return false;
		ThreeWaySynchronizer synchronizer = SolutionSubscriber.getInstance().getSynchronizer();
		IResourceVariantComparator comparator = SolutionSubscriber.getInstance().getResourceComparator();
		SolutionResourceVariant remote = getResourceVariant(localFile);
		byte[] baseBytes = synchronizer.getBaseBytes(localFile);
		IResourceVariant base = servoyTeamProvider.getResourceVariant(localFile, baseBytes);

		// Check whether we are overriding a remote change
		if (base == null && remote != null && !overrideIncoming)
		{
			if (!localFile.exists() || !SolutionSubscriber.hasSameContent(localFile, remote)) // if they have the same content do a mark as merged & commit
			{
				// The remote is an incoming (or conflicting) addition.
				// Do not replace unless we are overriding
				return false;
			}
		}
		else if (base != null && remote == null)
		{
			// The remote is an incoming deletion
			if (!localFile.exists())
			{
				// Conflicting deletion. Clear the synchronizer.
				if (synchronizeUpdater != null) synchronizeUpdater.addFlush(localFile, IResource.DEPTH_ZERO);
				else synchronizer.flush(localFile, IResource.DEPTH_ZERO);
			}
			else if (!overrideIncoming)
			{
				// Do not override the incoming deletion
				return false;
			}
		}
		else if (base != null && remote != null)
		{
			boolean same = comparator.compare(base, remote);
			if (!isLocallyModified(localFile) && same)
			{
				// The base and remote are the same and there's no local changes
				// so nothing needs to be done
				return false;
			}
			if (!same && !overrideIncoming && !SolutionSubscriber.hasSameContent(localFile, remote))
			{
				// The remote has changed. Only override if specified
				return false;
			}
		}

		// Handle an outgoing deletion
		File diskFile = servoyTeamProvider.getFile(localFile);
		if (!localFile.exists())
		{
			diskFile.delete();
			if (synchronizeUpdater != null) synchronizeUpdater.addFlush(localFile, IResource.DEPTH_ZERO);
			else synchronizer.flush(localFile, IResource.DEPTH_ZERO);
		}
		else
		{
			// Otherwise, upload the contents
			try
			{
				//Copy from the local file to the remote file:
				InputStream in = null;
				FileOutputStream out = null;
				try
				{
					if (!diskFile.getParentFile().exists()) diskFile.getParentFile().mkdirs();

					File diskFileWithSameName = fileExistWithDifferentCase(diskFile);
					if (diskFileWithSameName != null)
					{
						IPath sameResource = localFile.getParent().getFullPath().append(diskFileWithSameName.getName());
						internalCommit(localFile.getWorkspace().getRoot().getFile(sameResource), overrideIncoming, progress, synchronizeUpdater);
					}

					in = localFile.getContents(true);
					out = new FileOutputStream(diskFile);
					//Copy the contents of the local file to the remote file:
					RepositoryOperations.pipe(in, out, diskFile.length(), progress, diskFile.getName());
				}
				finally
				{
					if (in != null) in.close();
					if (out != null) out.close();
				}
				// Update the synchronizer base bytes
				remote = getResourceVariant(localFile);
				if (synchronizeUpdater != null) synchronizeUpdater.addBaseBytes(localFile, remote.asBytes());
				else synchronizer.setBaseBytes(localFile, remote.asBytes());
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}

		return true;
	}


	/*
	 * Get the folder and its children to the depth specified.
	 */
	private void internalCommit(IContainer container, int depth, boolean overrideIncoming, IProgressMonitor progress, List<IResource> changedResources,
		SynchronizeUpdater synchronizeUpdater) throws TeamException
	{
		if (Team.isIgnoredHint(container)) return;
		try
		{
			ThreeWaySynchronizer synchronizer = SolutionSubscriber.getInstance().getSynchronizer();
			// Make the local folder state match the remote folder state
			List<File> toDelete = new ArrayList<File>();
			if (container.getType() == IResource.FOLDER)
			{
				IFolder folder = (IFolder)container;
				File diskFile = servoyTeamProvider.getFile(container);
				SolutionResourceVariant remote = getResourceVariant(container);
				if (!folder.exists() && remote != null)
				{
					// Schedule the folder for removal but delay in
					// case the folder contains incoming changes
					toDelete.add(diskFile);
				}
				else if (folder.exists() && remote == null)
				{
					// Create the remote directory and sync up the local
					File diskFileWithSameName = fileExistWithDifferentCase(diskFile);
					if (diskFileWithSameName != null)
					{
						IPath sameResource = folder.getParent().getFullPath().append(diskFileWithSameName.getName());
						internalCommit(folder.getWorkspace().getRoot().getFile(sameResource), overrideIncoming, progress, synchronizeUpdater);
					}
					diskFile.mkdir();
					if (synchronizeUpdater != null) synchronizeUpdater.addBaseBytes(folder, servoyTeamProvider.getResourceVariant(folder).asBytes());
					else synchronizer.setBaseBytes(folder, servoyTeamProvider.getResourceVariant(folder).asBytes());
				}
			}

			// Get the children
			IResource[] children = synchronizer.members(container);
			if (children.length > 0)
			{
				internalCommit(children, depth == IResource.DEPTH_INFINITE ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO, overrideIncoming, progress,
					changedResources, synchronizeUpdater);
			}

			// Remove any empty folders
			for (File diskFile : toDelete)
			{
				if (diskFile.listFiles().length == 0)
				{
					diskFile.delete();
					if (synchronizeUpdater != null) synchronizeUpdater.addFlush(container, IResource.DEPTH_INFINITE);
					else synchronizer.flush(container, IResource.DEPTH_INFINITE);
				}
			}
		}
		catch (CoreException e)
		{
			throw TeamException.asTeamException(e);
		}
	}

	/**
	 * Make the local state of the traversals match the remote state by getting any out-of-sync resources. The overrideOutgoing flag is used to indicate whether
	 * locally modified files should also be replaced or left alone.
	 * 
	 * @param traversals the traversals that cover the resources to get
	 * @param overrideOutgoing whether locally modified resources should be replaced
	 * @param progress a progress monitor
	 * @throws TeamException
	 */
	public void update(ResourceTraversal[] traversals, boolean overrideOutgoing, IProgressMonitor monitor) throws TeamException
	{
		try
		{
			// ensure the progress monitor is not null
			monitor = Policy.monitorFor(monitor);
			monitor.beginTask(null, 100 * traversals.length);
			for (ResourceTraversal traversal : traversals)
			{
				update(traversal.getResources(), traversal.getDepth(), overrideOutgoing, new SubProgressMonitor(monitor, 100));
			}
		}
		finally
		{
			monitor.done();
		}
	}

	/**
	 * Make the local state of the project match the remote state by getting any out-of-sync resources. The overrideOutgoing flag is used to indicate whether
	 * locally modified files should also be replaced or left alone.
	 * 
	 * @param resources the resources to get
	 * @param depth the depth of the operation
	 * @param overrideOutgoing whether locally modified resources should be replaced
	 * @param progress a progress monitor
	 * @throws TeamException
	 */
	public void update(IResource[] resources, int depth, boolean overrideOutgoing, IProgressMonitor progress) throws TeamException
	{
		try
		{
			// ensure the progress monitor is not null
			progress = Policy.monitorFor(progress);
			progress.beginTask("update", 100);
			// Refresh the subscriber so we have the latest remote state
			SolutionSubscriber.getInstance().refresh(resources, depth, new SubProgressMonitor(progress, 30));

			internalUpdate(resources, depth, overrideOutgoing, new SubProgressMonitor(progress, 70));
		}
		finally
		{
			progress.done();
		}
	}

	private void internalUpdate(IResource[] resources, int depth, boolean overrideOutgoing, IProgressMonitor progress) throws TeamException
	{
		// Traverse the resources and get any that are out-of-sync
		progress.beginTask("update", IProgressMonitor.UNKNOWN);

		Arrays.sort(resources, new Comparator<IResource>()
		{
			// order resources as the deletes are performed first,
			// because in windows we cannot have the same resource with diff
			// case in the same folder
			public int compare(IResource o1, IResource o2)
			{
				if (!o1.exists() && o2.exists()) return 1;
				else if (o1.exists() && !o2.exists()) return -1;

				// if resources are at the same level, consider the IFile first, then the IContainer
				if (o1.getFullPath().segmentCount() == o2.getFullPath().segmentCount())
				{
					if (o1 instanceof IContainer && o2 instanceof IFile)
					{
						return 1;
					}
					else
					{
						return -1;
					}
				}

				// first have the container then the children, we achive this by the pathname compare
				String path1 = o1.getFullPath().toOSString();
				String path2 = o2.getFullPath().toOSString();

				return path1.compareTo(path2);
			}


		});

		for (IResource element : resources)
		{
			Policy.checkCanceled(progress);
			if (element.getType() == IResource.FILE)
			{
				IFile updateFile = (IFile)element;

				if (!updateFile.getName().startsWith(".")) // ignore hidden files
				{
					internalUpdate(updateFile, overrideOutgoing, progress);
				}
			}
			else if (depth != IResource.DEPTH_ZERO)
			{
				internalUpdate((IContainer)element, depth, overrideOutgoing, progress);
			}
			progress.worked(1);
		}
	}

	/*
	 * Get the folder and its children to the depth specified.
	 */
	private void internalUpdate(IContainer container, int depth, boolean overrideOutgoing, IProgressMonitor progress) throws TeamException
	{
		if (Team.isIgnoredHint(container)) return;

		try
		{
			ThreeWaySynchronizer synchronizer = SolutionSubscriber.getInstance().getSynchronizer();
			// Make the local folder state match the remote folder state
			List<IFolder> toDelete = new ArrayList<IFolder>();
			if (container.getType() == IResource.FOLDER)
			{
				IFolder folder = (IFolder)container;
				SolutionResourceVariant remote = getResourceVariant(container);
				if (!folder.exists() && remote != null)
				{
					// Create the local folder
					ResourcesUtils.createParentContainers(folder, false); //folder.create(false, true, progress);					
					synchronizer.setBaseBytes(folder, remote.asBytes());
				}
				else if (folder.exists() && remote == null)
				{
					// Schedule the folder for removal but delay in
					// case the folder contains outgoing changes
					toDelete.add(folder);
				}
			}

			// Get the children
			IResource[] children = synchronizer.members(container);
			if (children.length > 0)
			{
				internalUpdate(children, depth == IResource.DEPTH_INFINITE ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO, overrideOutgoing, progress);
			}

			// Remove any empty folders
			for (IFolder folder : toDelete)
			{
				if (folder.members().length == 0 || overrideOutgoing)
				{
					folder.delete(false, true, progress);
					synchronizer.flush(folder, IResource.DEPTH_INFINITE);
				}
			}
		}
		catch (CoreException e)
		{
			throw TeamException.asTeamException(e);
		}
	}

	private void updateDBServer(IFile localFile, boolean overrideOutgoing, IProgressMonitor progress) throws TeamException
	{
		IPath localFileRelativePath = localFile.getProjectRelativePath();
		String dbTableName = localFileRelativePath.lastSegment();
		dbTableName = dbTableName.substring(0, dbTableName.length() - DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT.length()); // substract .dbi from name
		String dbServerName = localFileRelativePath.segment(localFileRelativePath.segmentCount() - 2);

		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		EclipseRepository eclipseRepository = (EclipseRepository)ServoyModel.getDeveloperRepository();

		ThreeWaySynchronizer synchronizer = SolutionSubscriber.getInstance().getSynchronizer();
		IResourceVariantComparator comparator = SolutionSubscriber.getInstance().getResourceComparator();
		SolutionResourceVariant remote = getResourceVariant(localFile);
		byte[] baseBytes = synchronizer.getBaseBytes(localFile);
		IResourceVariant base = servoyTeamProvider.getResourceVariant(localFile, baseBytes);
		if (!synchronizer.hasSyncBytes(localFile) || (isLocallyModified(localFile) && !overrideOutgoing))
		{
			// Do not overwrite the local modification
			return;
		}
		if (base != null && remote == null)
		{
			// The remote no longer exists so remove the local
			try
			{
				((IServerInternal)eclipseRepository.getServer(dbServerName)).syncWithExternalTable(dbTableName, null);

				localFile.delete(false, true, progress);
				synchronizer.flush(localFile, IResource.DEPTH_ZERO);
				return;
			}
			catch (CoreException e)
			{
				throw TeamException.asTeamException(e);
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
		if (!synchronizer.isLocallyModified(localFile) && base != null && remote != null && comparator.compare(base, remote))
		{
			// The base and remote are the same and there's no local changes
			// so nothing needs to be done
			return;
		}

		try
		{
			ITeamRepository repo = repositoryAP.getRepository();

			IServer remoteServer = repo.getServer(dbServerName);
			if (remoteServer != null)
			{
				Table remoteTable = (Table)remoteServer.getTable(dbTableName);

				IServerInternal localServer = (IServerInternal)eclipseRepository.getServer(dbServerName);
				localServer.syncWithExternalTable(dbTableName, remoteTable);
			}

		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		synchronizer.setBaseBytes(localFile, remote.asBytes());
	}

	/*
	 * Get the file if it is out-of-sync.
	 */
	private void internalUpdate(IFile localFile, boolean overrideOutgoing, IProgressMonitor progress) throws TeamException
	{
		if (Team.isIgnoredHint(localFile)) return;

		ThreeWaySynchronizer synchronizer = SolutionSubscriber.getInstance().getSynchronizer();
		IResourceVariantComparator comparator = SolutionSubscriber.getInstance().getResourceComparator();
		SolutionResourceVariant remote = getResourceVariant(localFile);
		byte[] baseBytes = synchronizer.getBaseBytes(localFile);
		IResourceVariant base = servoyTeamProvider.getResourceVariant(localFile, baseBytes);
		if (!synchronizer.hasSyncBytes(localFile) || (isLocallyModified(localFile) && !overrideOutgoing))
		{
			if (overrideOutgoing)
			{
				try
				{
					localFile.delete(false, true, progress);
					synchronizer.flush(localFile, IResource.DEPTH_ZERO);
					return;
				}
				catch (CoreException e)
				{
					throw TeamException.asTeamException(e);
				}
			}

			// Do not overwrite the local modification
			return;
		}
		if (base != null && remote == null)
		{
			// The remote no longer exists so remove the local
			try
			{
				localFile.delete(false, true, progress);
				synchronizer.flush(localFile, IResource.DEPTH_ZERO);
				return;
			}
			catch (CoreException e)
			{
				throw TeamException.asTeamException(e);
			}
		}
		if (!synchronizer.isLocallyModified(localFile) && base != null && remote != null && comparator.compare(base, remote))
		{
			// The base and remote are the same and there's no local changes
			// so nothing needs to be done
			return;
		}

		if (remote != null)
		{
			try
			{
				//Copy from the remote file to the local file:
				InputStream source = null;
				try
				{
					// Get the remote file content.
					source = remote.getContents();
					// Set the local file content to be the same as the remote file.
					if (localFile.exists()) localFile.setContents(source, true, false, progress);
					else ResourcesUtils.createFileAndParentContainers(localFile, source, false); //localFile.create(source, false, progress);
				}
				finally
				{
					if (source != null) source.close();
				}

				synchronizer.setBaseBytes(localFile, remote.asBytes());
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	private boolean isLocallyModified(IFile localFile) throws TeamException
	{
		ThreeWaySynchronizer synchronizer = SolutionSubscriber.getInstance().getSynchronizer();
		if (!localFile.exists())
		{
			return synchronizer.getBaseBytes(localFile) != null;
		}

		return synchronizer.isLocallyModified(localFile);
	}

	/*
	 * Get the resource variant for the given resource.
	 */
	private SolutionResourceVariant getResourceVariant(IResource resource)
	{
		return (SolutionResourceVariant)servoyTeamProvider.getResourceVariant(resource);
	}

	private final static byte[] COPY_BUFFER = new byte[4096];

	public static void pipe(InputStream in, OutputStream out, long sizeEstimate, IProgressMonitor progress, String title) throws IOException
	{
		// Only show progress for files larger than 25Kb.
		boolean showProgress = (progress != null) && (sizeEstimate > 25000);
		long bytesCopied = 0;

		synchronized (COPY_BUFFER)
		{
			// Read the initial chunk.
			int read = in.read(COPY_BUFFER, 0, COPY_BUFFER.length);

			while (read != -1)
			{
				out.write(COPY_BUFFER, 0, read);

				// Report progress
				if (showProgress)
				{
					bytesCopied = bytesCopied + read;
					/*
					 * progress.subTask( Policy.bind( "filetransfer.monitor", new Object[] { title, new Long(bytesCopied / 1024), kilobytesEstimate }));
					 */
				}

				// Read the next chunk.
				read = in.read(COPY_BUFFER, 0, COPY_BUFFER.length);
			} // end while
		} // end synchronized
	}

	private static String[] intersect(String[] a1, String[] a2)
	{
		List<String> intersectA = new ArrayList<String>();
		for (String s1 : a1)
		{
			for (String s2 : a2)
			{
				if (s1.equals(s2) && intersectA.indexOf(s1) == -1) intersectA.add(s1);
			}
		}

		return intersectA.toArray(new String[intersectA.size()]);
	}

	private static void copyFolderTxtFiles(IFileAccess fileAccess, File srcFolder, String dstFolder)
	{
		File[] srcFiles = srcFolder.listFiles();
		if (srcFiles != null)
		{
			for (File srcFile : srcFiles)
			{
				if (srcFile.isFile())
				{
					try
					{
						String content = Utils.getTXTFileContent(srcFile);
						fileAccess.setUTF8Contents(dstFolder + '/' + srcFile.getName(), content);
					}
					catch (IOException ex)
					{
						ServoyLog.logError(ex);
					}
				}
			}
		}
	}

	private static File fileExistWithDifferentCase(File file)
	{
		File parent = file.getParentFile();
		if (parent != null)
		{
			String[] dirFiles = parent.list();
			if (dirFiles != null)
			{
				String fileName = file.getName();
				for (String dirFile : dirFiles)
				{
					if (fileName.equalsIgnoreCase(dirFile) && !fileName.equals(dirFile)) return new File(parent, dirFile);
				}
			}
		}

		return null;
	}

}