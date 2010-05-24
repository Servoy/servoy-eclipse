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
package com.servoy.eclipse.core.repository;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.dltk.javascript.core.JavaScriptNature;

import com.servoy.eclipse.core.IFileAccess;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.ServoyResourcesProject;
import com.servoy.eclipse.core.WorkspaceFileAccess;
import com.servoy.j2db.persistence.AbstractRootObject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.I18NUtil.MessageEntry;
import com.servoy.j2db.server.ApplicationServerSingleton;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.xmlxport.GroupInfo;
import com.servoy.j2db.util.xmlxport.IXMLImportEngine;
import com.servoy.j2db.util.xmlxport.IXMLImportHandlerVersions11AndHigher;
import com.servoy.j2db.util.xmlxport.IXMLImportUserChannel;
import com.servoy.j2db.util.xmlxport.ImportInfo;
import com.servoy.j2db.util.xmlxport.ImportTransactable;
import com.servoy.j2db.util.xmlxport.RootObjectImportInfo;
import com.servoy.j2db.util.xmlxport.UserInfo;
import com.servoy.j2db.util.xmlxport.GroupInfo.GroupElementInfo;
import com.servoy.j2db.util.xmlxport.RootObjectInfo.RootElementInfo;

public class XMLEclipseWorkspaceImportHandlerVersions11AndHigher implements IXMLImportHandlerVersions11AndHigher
{
	private final EclipseRepository repository;

	protected ServoyResourcesProject resourcesProject;

	private final EclipseUserManager userManager;
	private final IProgressMonitor m;
	private final IXMLImportHandlerVersions11AndHigher x11handler;

	private final List<IProject> createdProjects;

	public XMLEclipseWorkspaceImportHandlerVersions11AndHigher(IXMLImportHandlerVersions11AndHigher x11handler, EclipseRepository repository,
		ServoyResourcesProject resourcesProject, EclipseUserManager userManager, IProgressMonitor monitor, List<IProject> createdProjects)
	{
		this.x11handler = x11handler;
		this.repository = repository;
		this.resourcesProject = resourcesProject;
		this.userManager = userManager;
		m = monitor;
		this.createdProjects = createdProjects;
	}


	public static IRootObject[] importFromJarFile(final IXMLImportEngine importEngine, final IXMLImportHandlerVersions11AndHigher x11handler,
		final IXMLImportUserChannel userChannel, final EclipseRepository repository, final String newResourcesProjectName,
		final ServoyResourcesProject resourcesProject, final IProgressMonitor m, final boolean cleanImport) throws RepositoryException
	{
		final List<IProject> createdProjects = new ArrayList<IProject>();
		try
		{
			m.beginTask("Importing...", 5);
			final EclipseUserManager userManager = new EclipseUserManager()
			{
				@Override
				public void setResourcesProject(IProject project)
				{
					this.resourcesProject = project;
				}
			};
			userManager.setWriteMode(EclipseUserManager.WRITE_MODE_MANUAL);
			userManager.setOperational(true);
			if (resourcesProject != null)
			{
				userManager.setResourcesProject(resourcesProject.getProject());
				userManager.reloadAllSecurityInformation();
			}

			final Exception[] exception = new Exception[1];
			final IProject[] dummySolProject = new IProject[1];
			final IProject[] rProject = new IProject[1];
			final IProject[] mainProject = new IProject[1];
			final IRootObject[][] rootObjects = new IRootObject[1][];
			final boolean[] finishedFlag = new boolean[] { false };
			final IFileAccess wsa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());

			// the following jobs will be executed backwards (starting from 1 not from 3)
			final WorkspaceJob importJob3 = new WorkspaceJob("Finalizing import and activating solution")
			{

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					m.setTaskName(getName());
					m.worked(1);
					try
					{
						ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
						ServoyProject servoyProject = servoyModel.getServoyProject(mainProject[0].getName());
						if (resourcesProject == null) userManager.setResourcesProject(rProject[0]);

						servoyModel.setActiveProject(servoyProject);
						userManager.writeAllSecurityInformation(true);

						dummySolProject[0].delete(true, true, null); // dummy did it's job - to activate correct resources project, now we must remove it
					}
					catch (Exception e)
					{
						exception[0] = e;
					}
					finally
					{
						synchronized (finishedFlag)
						{
							finishedFlag[0] = true;
							finishedFlag.notify();
						}
					}
					m.setTaskName("Finished... updating workbench state");
					m.worked(1);
					return Status.OK_STATUS;
				}

			};
			final WorkspaceJob importJob2 = new WorkspaceJob("Reading solution & modules, updating tables")
			{

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					m.setTaskName(getName());
					m.worked(1);
					boolean nextJobWillStart = false;
					try
					{
						// activate dummy
						ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
						ServoyProject dummyServoyProject = servoyModel.getServoyProject(dummySolProject[0].getName());
						servoyModel.setActiveProject(dummyServoyProject);

						// verify that correct resources project is active
						if (servoyModel.getActiveResourcesProject() == null || servoyModel.getActiveResourcesProject().getProject() != rProject[0])
						{
							throw new RepositoryException("Cannot activate resources project " + rProject[0].getName() + ".");
						}

						// read jar file & import some stuff into resources project
						rootObjects[0] = importEngine.importFromJarFile(new XMLEclipseWorkspaceImportHandlerVersions11AndHigher(x11handler, repository,
							resourcesProject, userManager, m, createdProjects), cleanImport);
						if (rootObjects[0] == null || rootObjects[0].length == 0) throw new RepositoryException("No solution was imported.");

						// create the eclipse solution projects
						for (IRootObject root : rootObjects[0])
						{
							if (root instanceof Solution)
							{
								IProject newProject = ServoyModel.getWorkspace().getRoot().getProject(root.getName());
								if (newProject.exists()) newProject.delete(true, true, null);
								newProject.create(null);
								newProject.open(null);

								IProjectDescription description = newProject.getDescription();
								description.setReferencedProjects(new IProject[] { rProject[0] });
								newProject.setDescription(description, null);
								if (mainProject[0] == null)
								{
									mainProject[0] = newProject;
								}
								createdProjects.add(newProject);
							}
						}

						// activate the main project
						if (mainProject[0] == null) throw new RepositoryException("No solution was imported.");

						m.setTaskName("Writing files");
						m.worked(1);
						// now write everything on disk
						for (IRootObject root : rootObjects[0])
						{
							if (root instanceof Style)
							{
								m.setTaskName("Writing style " + root.getName());
								StringResourceDeserializer.writeStringResource((Style)root, wsa, rProject[0].getName());
							}
							else
							{
								if (SolutionMetaData.isPreImportHook(root.getName()) || SolutionMetaData.isPostImportHook(root.getName()))
								{
									userChannel.info("Execution of hook module '" + root.getName() + "' skipped, it's only supported in repository import.",
										ILogLevel.INFO);
								}
								m.setTaskName("Writing solution/module " + root.getName());
								SolutionSerializer.writePersist(root, wsa, repository, true, false, true);
							}
						}

						for (IProject p : createdProjects)
						{
							if (!p.equals(rProject[0]) && !p.equals(dummySolProject[0]))
							{
								IProjectDescription description = p.getDescription();
								description.setNatureIds(new String[] { ServoyProject.NATURE_ID, JavaScriptNature.NATURE_ID });
								p.setDescription(description, null);
							}
						}

						m.setTaskName("Updating workbench state");
						importJob3.setRule(ServoyModel.getWorkspace().getRoot());
						importJob3.setUser(false);
						importJob3.setSystem(true);
						nextJobWillStart = true;
					}
					catch (Exception e)
					{
						exception[0] = e;
					}
					finally
					{
						synchronized (finishedFlag)
						{
							finishedFlag[0] = !nextJobWillStart;
							if (finishedFlag[0] == true) finishedFlag.notify();
						}
					}
					if (nextJobWillStart) importJob3.schedule();
					return Status.OK_STATUS;
				}

			};
			// need to call following code in a set of jobs because some operations need the resource listeners to update stuff before running other stuff;
			// otherwise we will end up with multiple threads running in the same time (as WorkspaceJobs allow from time to time a resource change event to be triggered)
			WorkspaceJob importJob1 = new WorkspaceJob("Preparing for import (activating needed resources project)")
			{

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					m.setTaskName(getName());
					m.worked(1);
					boolean nextJobWillStart = false;
					try
					{
						// create/use resources project - and make sure it is active before super.importFromJarFile(...) is called because that changes .dbi files - and those changes
						// should be applied to the proper resources project (maybe other stuff as well from resources project, such as i18n)
						if (resourcesProject == null)
						{
							// create the resources project
							// create a new resource project
							rProject[0] = ServoyModel.getWorkspace().getRoot().getProject(newResourcesProjectName);
							rProject[0].create(null);
							rProject[0].open(null);
							IProjectDescription resourceProjectDescription = rProject[0].getDescription();
							resourceProjectDescription.setNatureIds(new String[] { ServoyResourcesProject.NATURE_ID });
							rProject[0].setDescription(resourceProjectDescription, null);
							createdProjects.add(rProject[0]);
						}
						else
						{
							rProject[0] = resourcesProject.getProject();
						}

						// create a dummy solution so that the active resources project can become the right one
						dummySolProject[0] = null;
						for (int i = 0; (dummySolProject[0] == null || dummySolProject[0].exists()); i++)
						{
							if (i == 100)
							{
								// can't go on forever....
								throw new RuntimeException("Could not create a new dummy solution project to use during import");
							}
							dummySolProject[0] = ServoyModel.getWorkspace().getRoot().getProject("import_placeholder" + (i == 0 ? "" : ("_" + i)));
						}
						dummySolProject[0].create(null);
						dummySolProject[0].open(null);
						createdProjects.add(dummySolProject[0]);
						IProjectDescription description = dummySolProject[0].getDescription();
						description.setNatureIds(new String[] { ServoyProject.NATURE_ID, JavaScriptNature.NATURE_ID });
						description.setReferencedProjects(new IProject[] { rProject[0] });
						dummySolProject[0].setDescription(description, null);
						Solution dummySolution = (Solution)repository.createNewRootObject(dummySolProject[0].getName(), IRepository.SOLUTIONS);
						SolutionSerializer.writePersist(dummySolution, wsa, repository, true, false, true);

						m.setTaskName("Updating workbench state");
						importJob2.setRule(ServoyModel.getWorkspace().getRoot());
						importJob2.setUser(false);
						importJob2.setSystem(true);
						nextJobWillStart = true;
					}
					catch (Exception e)
					{
						exception[0] = e;
					}
					finally
					{
						synchronized (finishedFlag)
						{
							finishedFlag[0] = !nextJobWillStart;
							if (finishedFlag[0] == true) finishedFlag.notify();
						}
					}
					if (nextJobWillStart) importJob2.schedule();
					return Status.OK_STATUS;
				}

			};
			importJob1.setRule(ServoyModel.getWorkspace().getRoot());
			importJob1.setUser(false);
			importJob1.setSystem(true);
			importJob1.schedule();

			synchronized (finishedFlag)
			{
				while (finishedFlag[0] == false)
				{
					finishedFlag.wait();
				}
			}

			if (exception[0] != null) throw exception[0];
			return rootObjects[0];
		}
		catch (RepositoryException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new RepositoryException(e);
		}
		finally
		{
			m.done();
		}

	}

	private void rollback(ImportTransactable importTransactable)
	{
		// TODO: more things to roll back here
		for (IProject project : createdProjects)
		{
			try
			{
				project.delete(true, true, null);
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
		importTransactable.processPostRollBack();
	}

	public int getObjectId(boolean cleanImport, String uuid) throws RepositoryException
	{
		return repository.getElementIdForUUIDString(uuid);
	}

	public void getRootObjectNameForImport(ImportInfo importInfo, RootObjectImportInfo rootObjectImportInfo) throws RepositoryException
	{
		// duplicate uuid fails, duplicate name requires user decision
		// checks on selected resources project for styles ( if available)
		UUID uuid = rootObjectImportInfo.uuid;
		String name = rootObjectImportInfo.info.name;
		int typeId = rootObjectImportInfo.info.elementInfo.typeId;
		String objectType = RepositoryHelper.getObjectTypeName(typeId);
		RootObjectMetaData[] metadatas = null;
		RootObjectMetaData rootObjectMetaData = null;
		if (typeId == IRepository.SOLUTIONS)
		{
			rootObjectMetaData = repository.getRootObjectMetaData(uuid);
			if (rootObjectMetaData != null)
			{
				if (importInfo.isProtected)
				{
					// Solution is protected; cannot rename.
					throw new RepositoryException("The " + objectType + " '" + rootObjectMetaData.getName() +
						"' already exists in the repository with the name '" + name + "', but the import is protected. Remove the " + objectType +
						" from the workspace.");
				}
			}
		}
		else if (typeId == IRepository.STYLES && resourcesProject != null)
		{
			try
			{
				metadatas = repository.deserializeMetaDatas(resourcesProject.getProject().getName(), typeId);
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
			if (metadatas != null)
			{
				for (RootObjectMetaData meta : metadatas)
				{
					if (meta.getRootObjectUuid().equals(uuid))
					{
						if (importInfo.isProtected)
						{
							// Solution is protected; cannot rename.
							throw new RepositoryException("The " + objectType + " '" + meta.getName() + "' already exists in the repository with the name '" +
								name + "', but the import is protected. Remove the " + objectType + " from the workspace.");
						}
						rootObjectMetaData = meta;
					}
				}
			}
		}
		if (rootObjectMetaData != null)
		{
			int result = x11handler.getUserChannel().askStyleAlreadyExistsAction(name);
			switch (result)
			{
				case IXMLImportUserChannel.OVERWRITE_ACTION :
					repository.removeRootObject(rootObjectMetaData.getRootObjectId());
					name = rootObjectMetaData.getName();
					break;
				case IXMLImportUserChannel.SKIP_ACTION :
					rootObjectImportInfo.skip = true;
					break;
			}
		}
		else
		{
			if (typeId == IRepository.SOLUTIONS)
			{
				rootObjectMetaData = repository.getRootObjectMetaData(name, typeId);
				if (rootObjectMetaData != null && importInfo.isProtected)
				{
					throw new RepositoryException("A different " + objectType + " with the name '" + name +
						"' already exists, but the import is protected. Rename or remove the " + objectType + " in the repository.");
				}

			}
			else if (typeId == IRepository.STYLES && resourcesProject != null)
			{
				if (metadatas == null) metadatas = repository.deserializeMetaDatas(resourcesProject.getProject().getName(), typeId);
				for (RootObjectMetaData meta : metadatas)
				{
					if (meta.getName().equals(name))
					{
						rootObjectMetaData = meta;
						break;
					}
				}
				if (rootObjectMetaData != null && importInfo.isProtected)
				{
					throw new RepositoryException("A different " + objectType + " with the name '" + name +
						"' already exists, but the import is protected. Rename or remove the " + objectType + " in the repository.");
				}
			}

			while (rootObjectMetaData != null && !rootObjectImportInfo.skip)
			{
				int result = x11handler.getUserChannel().askRenameRootObjectAction(name, typeId);
				switch (result)
				{
					case IXMLImportUserChannel.CANCEL_ACTION :
						// If no new name is specified, give up.
						throw new RepositoryException(ServoyException.InternalCodes.OPERATION_CANCELLED);
					case IXMLImportUserChannel.RENAME_ACTION :
						// If a new name is specified, get it.
						name = x11handler.getUserChannel().getNewName();
						break;
					case IXMLImportUserChannel.SKIP_ACTION :
						rootObjectImportInfo.skip = true;
						break;
					default :
						// Unknown response, treat as error.
						ServoyLog.logError("Unknown reponse from import user channel: " + result, null);
						throw new RepositoryException(ServoyException.INVALID_INPUT);
				}
				// Check again if a root object exists by the newly specified name.
				if (rootObjectMetaData != null)
				{
					if (typeId == IRepository.SOLUTIONS)
					{
						rootObjectMetaData = repository.getRootObjectMetaData(name, typeId);

					}
					else if (typeId == IRepository.STYLES && resourcesProject != null)
					{
						rootObjectMetaData = null;
						if (metadatas == null) metadatas = repository.deserializeMetaDatas(resourcesProject.getProject().getName(), typeId);
						for (RootObjectMetaData meta : metadatas)
						{
							if (meta.getName().equals(name))
							{
								rootObjectMetaData = meta;
							}
						}
					}

				}
			}
		}
		x11handler.addSubstitutionName(importInfo, name, rootObjectImportInfo, typeId);
	}

	public void getRootObjectIdForImport(ImportInfo importInfo, RootObjectImportInfo rootObjectImportInfo) throws RepositoryException
	{
		x11handler.getRootObjectIdForImport(importInfo, rootObjectImportInfo);
		if (rootObjectImportInfo.info.elementInfo.typeId == IRepository.SOLUTIONS && importInfo.main.equals(rootObjectImportInfo))
		{
			if (ApplicationServerSingleton.get().checkSolutionProtection(rootObjectImportInfo))
			{
				// ask for protection password
				String protectionPassword = x11handler.getUserChannel().askProtectionPassword(rootObjectImportInfo.info.name);
				if (protectionPassword == null || !ApplicationServerSingleton.get().checkSolutionPassword(rootObjectImportInfo, protectionPassword))
				{
					throw new RepositoryException("Wrong password, cannot import solution.");
				}
			}
		}
	}

	public IRootObject importRootObject(RootObjectImportInfo rootObjectImportInfo) throws IllegalAccessException, IntrospectionException,
		InvocationTargetException, RepositoryException
	{
		m.setTaskName("Reading " + rootObjectImportInfo.name);
		IRootObject root = x11handler.importRootObject(rootObjectImportInfo);
		if (root instanceof AbstractRootObject)
		{
			((AbstractRootObject)root).setChangeHandler(new EclipseChangeHandler(repository));
		}
		return root;
	}

	public void importSecurityInfo(ImportInfo importInfo, RootObjectImportInfo rootObjectImportInfo) throws ServoyException
	{
		if (rootObjectImportInfo.securityInfoSet == null) return;
		m.setTaskName("Importing security info");

		Iterator<GroupInfo> iterator = rootObjectImportInfo.securityInfoSet.iterator();
		while (iterator.hasNext())
		{
			GroupInfo groupInfo = iterator.next();
			int groupId = userManager.getGroupId(ApplicationServerSingleton.get().getClientId(), groupInfo.name);
			boolean update = false;
			if (groupId > 0)
			{
				if (!importInfo.cleanImport && groupInfo.elementInfoSet.size() == 0)
				{
					continue;
				}
				// If the group already exists we ask if the user wants to 
				// update it.
				int result = x11handler.getUserChannel().askGroupAlreadyExistsAction(groupInfo.name);
				switch (result)
				{
					case IXMLImportUserChannel.CANCEL_ACTION :
						// Don't import this group.
						break;
					case IXMLImportUserChannel.OVERWRITE_ACTION :
						// Update this group.
						update = true;
						break;
					default :
						// Unknown response, treat as error.
						ServoyLog.logError("Unknown reponse from import user channel: " + result, null);
						throw new RepositoryException(ServoyException.INVALID_INPUT);
				}
			}
			// Update or create the group.
			if (groupId < 0 || update)
			{
				// If the group does not yet exist, create it.
				if (groupId < 0)
				{
					userManager.createGroup(ApplicationServerSingleton.get().getClientId(), groupInfo.name);
				}

				// Iterate over the elements in the security element info and
				// insert the corresponding values.
				Iterator<GroupElementInfo> iterator2 = groupInfo.elementInfoSet.iterator();
				while (iterator2.hasNext())
				{
					GroupElementInfo elementInfo = iterator2.next();
					int elementId = -1;
					String target = elementInfo.elementUuid;
					int i = target.indexOf('.');
					if (i > 0)
					{
						String serverName = target.substring(0, i);

						// Get the substitution map for the Repository.SERVERS type.
						Integer key = new Integer(IRepository.SERVERS);
						Map<String, String> connectionMap = importInfo.substitutionMap.get(key);

						// If a connection map exits, see if this connection was renamed.
						if (connectionMap != null)
						{
							String newName = connectionMap.get(serverName);
							if (newName != null)
							{
								// This connection was renamed, use the new name instead.
								serverName = newName;
							}
						}

						target = target.substring(i + 1);
						i = target.indexOf('.');
						if (i > 0)
						{
							String tableName = target.substring(0, i);
							String columnName = target.substring(i + 1);
							if (elementInfo.columnInfoAccess >= 0)
							{
								userManager.setTableSecurityAccess(ApplicationServerSingleton.get().getClientId(), groupInfo.name,
									elementInfo.columnInfoAccess, serverName, tableName, columnName);
							}
						}
						else
						{
							throw new RepositoryException("Invalid value in security element.");
						}
					}
					else
					{
						UUID uuid = null;
						if (importInfo.cleanImport)
						{
							uuid = importInfo.cleanImportUUIDMap.get(elementInfo.elementUuid);
						}
						else
						{
							uuid = UUID.fromString(elementInfo.elementUuid);
						}
						if (uuid != null)
						{
							UUID formUUID = getFormUUID(importInfo, rootObjectImportInfo, uuid);
							userManager.addFormSecurityAccess(groupInfo.name, elementInfo.elementAccess, uuid, formUUID);
						}
					}
				}

			}

		}
	}

	private UUID getFormUUID(ImportInfo importInfo, RootObjectImportInfo rootObjectImportInfo, UUID childUUID)
	{
		for (RootElementInfo info : rootObjectImportInfo.info.elementInfo.children)
		{
			if (info.typeId == IRepository.FORMS)
			{
				if (info.uuid.equals(childUUID.toString()))
				{
					if (importInfo.cleanImport) return importInfo.cleanImportUUIDMap.get(info.uuid);
					return UUID.fromString(info.uuid);
				}
				for (RootElementInfo childInfo : info.children)
				{
					if (childInfo.uuid.equals(childUUID.toString()))
					{
						if (importInfo.cleanImport) return importInfo.cleanImportUUIDMap.get(info.uuid);
						return UUID.fromString(info.uuid);
					}
				}
			}
		}
		return null;
	}

	public boolean checkI18NStorage()
	{
		m.setTaskName("Reading i18n data");
		return x11handler.checkI18NStorage();
	}

	public void handleI18NImport(ImportInfo importInfo, String i18nServerName, String i18nTableName, TreeMap<String, MessageEntry> messages) throws Exception
	{
		m.setTaskName("Importing i18n for table '" + i18nTableName + "'");
		EclipseMessages.writeMessages(i18nServerName, i18nTableName, messages, new WorkspaceFileAccess(ResourcesPlugin.getWorkspace()));

	}

	public void importSampleData(JarFile jarFile, ImportInfo importInfo)
	{
		m.setTaskName("Importing sample data");
		x11handler.importSampleData(jarFile, importInfo);
	}

	public void importBlobs(JarFile jarFile, List<String> blobs, ImportInfo importInfo, Map<String, byte[]> digestMap) throws IOException, RepositoryException,
		NoSuchAlgorithmException
	{
		m.setTaskName("Importing blobs");
		x11handler.importBlobs(jarFile, blobs, importInfo, digestMap);
	}

	public void importDatabaseInfo(ImportInfo importInfo, ImportTransactable importTransactable) throws Exception
	{
		m.setTaskName("Importing database info");
		x11handler.importDatabaseInfo(importInfo, importTransactable);
	}

	public void importUserInfo(Set<UserInfo> userInfoSet)
	{
		try
		{
			if (userInfoSet == null)
			{
				return;
			}
			int mode = x11handler.getUserChannel().askUserImportPolicy();
			if (mode == 0)
			{
				x11handler.getUserChannel().info("Skipped import of user information", ILogLevel.INFO);
				return;
			}
			m.setTaskName("Importing user info");
			boolean addAdmins = x11handler.getUserChannel().getAddUsersToAdministratorGroup();
			HashMap<String, Integer> groupIdCache = new HashMap<String, Integer>();

			Iterator<UserInfo> iterator = userInfoSet.iterator();
			while (iterator.hasNext())
			{
				UserInfo userInfo = iterator.next();

				// Look up the user with the given name.
				int userId = userManager.getUserIdByUserName(ApplicationServerSingleton.get().getClientId(), userInfo.name);

				// If override users is set, delete the user first.
				if (userId != -1 && mode == 2)
				{
					userManager.deleteUser(ApplicationServerSingleton.get().getClientId(), userId);
					userId = -1;
				}

				// If the user does not exist, create.
				if (userId == -1)
				{
					userId = userManager.createUser(ApplicationServerSingleton.get().getClientId(), userInfo.name, userInfo.password, userInfo.uid, true);
					if (userId == -1)
					{
						x11handler.getUserChannel().info("Could not create user with name `" + userInfo.name + "'.", ILogLevel.ERROR);
						continue;
					}
				}

				// Add the user to all of the groups in the groups set.
				Iterator<String> iterator2 = userInfo.groupSet.iterator();
				while (iterator2.hasNext())
				{
					String groupName = iterator2.next();
					Integer groupId = groupIdCache.get(groupName);
					if (groupId == null)
					{
						groupId = new Integer(userManager.getGroupId(ApplicationServerSingleton.get().getClientId(), groupName));
						if (groupId.intValue() == -1)
						{
							if (IRepository.ADMIN_GROUP.equals(groupName))
							{
								// admin group is considered to always exist; but in case of a newly created resources project it isn't there yet
								groupId = new Integer(userManager.createGroup(ApplicationServerSingleton.get().getClientId(), IRepository.ADMIN_GROUP));
							}
							else
							{
								x11handler.getUserChannel().info("Could not find group with name `" + groupName + "', user '" + userInfo.name + "' not added.",
									ILogLevel.ERROR);
								continue;
							}
						}
						groupIdCache.put(groupName, groupId);
					}
					if (IRepository.ADMIN_GROUP.equals(groupName) && !addAdmins)
					{
						x11handler.getUserChannel().info("User '" + userInfo.name + "' not added to '" + IRepository.ADMIN_GROUP + "'.", ILogLevel.INFO);
						continue;
					}
					userManager.addUserToGroup(ApplicationServerSingleton.get().getClientId(), userId, groupId.intValue());
				}
				x11handler.getUserChannel().notifyWorkDone(-1);
			}
		}
		catch (Throwable ex)
		{
			x11handler.getUserChannel().info("User information import failed: " + ex, ILogLevel.ERROR);
			ServoyLog.logError(ex);
		}
	}

	public void addSubstitutionName(ImportInfo importInfo, String name, RootObjectImportInfo rootObjectImportInfo, int typeId)
	{
		x11handler.addSubstitutionName(importInfo, name, rootObjectImportInfo, typeId);
	}

	public IXMLImportUserChannel getUserChannel()
	{
		return x11handler.getUserChannel();
	}

	public void importingDone(ImportInfo importInfo, IRootObject[] rootObjects, ImportTransactable importTransactable) throws RepositoryException
	{
		x11handler.importingDone(importInfo, rootObjects, importTransactable);
	}

	public void importingFailed(ImportInfo importInfo, ImportTransactable importTransactable, Exception e)
	{
		rollback(importTransactable);
		x11handler.importingFailed(importInfo, importTransactable, e);
	}

	public void startImport(ImportTransactable importTransactable) throws RepositoryException
	{
		x11handler.startImport(importTransactable);
	}

	public void importRevisionInfo(ImportInfo importInfo) throws RepositoryException
	{
		x11handler.importRevisionInfo(importInfo);
	}

	public String getPropertyValue(String oldValue)
	{
		return x11handler.getPropertyValue(oldValue);
	}

	public IPersist loadDeletedObjectByElementId(IRootObject rootObject, int elementId, ISupportChilds parent) throws RepositoryException,
		IllegalAccessException, IntrospectionException, InvocationTargetException
	{
		return x11handler.loadDeletedObjectByElementId(rootObject, elementId, parent);
	}

	public void setStyleActiveRelease(IRootObject[] rootObjects) throws RepositoryException
	{
		x11handler.setStyleActiveRelease(rootObjects);
	}

}
