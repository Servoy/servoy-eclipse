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
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dltk.javascript.core.JavaScriptNature;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.json.JSONException;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.RunInWorkspaceJob;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.repository.StringResourceDeserializer;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.MetaDataUtils;
import com.servoy.j2db.persistence.AbstractRootObject;
import com.servoy.j2db.persistence.ChangeHandler;
import com.servoy.j2db.persistence.I18NUtil.MessageEntry;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.xmlxport.DBIDefinition;
import com.servoy.j2db.util.xmlxport.GroupInfo;
import com.servoy.j2db.util.xmlxport.GroupInfo.GroupElementInfo;
import com.servoy.j2db.util.xmlxport.IXMLImportEngine;
import com.servoy.j2db.util.xmlxport.IXMLImportHandlerVersions11AndHigher;
import com.servoy.j2db.util.xmlxport.IXMLImportUserChannel;
import com.servoy.j2db.util.xmlxport.ImportInfo;
import com.servoy.j2db.util.xmlxport.ImportTransactable;
import com.servoy.j2db.util.xmlxport.MetadataDef;
import com.servoy.j2db.util.xmlxport.RootObjectImportInfo;
import com.servoy.j2db.util.xmlxport.RootObjectInfo.RootElementInfo;
import com.servoy.j2db.util.xmlxport.UserInfo;

public class XMLEclipseWorkspaceImportHandlerVersions11AndHigher implements IXMLImportHandlerVersions11AndHigher
{
	private final EclipseRepository repository;

	protected ServoyResourcesProject resourcesProject;

	private final WorkspaceUserManager userManager;
	private final IProgressMonitor m;
	private final IXMLImportHandlerVersions11AndHigher x11handler;

	private final List<IProject> createdProjects;

	private final boolean reportImportFail;

	public XMLEclipseWorkspaceImportHandlerVersions11AndHigher(IXMLImportHandlerVersions11AndHigher x11handler, EclipseRepository repository,
		ServoyResourcesProject resourcesProject, WorkspaceUserManager userManager, IProgressMonitor monitor, List<IProject> createdProjects,
		boolean reportImportFail)
	{
		this.x11handler = x11handler;
		this.repository = repository;
		this.resourcesProject = resourcesProject;
		this.userManager = userManager;
		m = monitor;
		this.createdProjects = createdProjects;
		this.reportImportFail = reportImportFail;
	}

	public static IRootObject[] importFromJarFile(final IXMLImportEngine importEngine, final IXMLImportHandlerVersions11AndHigher x11handler,
		final IXMLImportUserChannel userChannel, final EclipseRepository repository, final String newResourcesProjectName,
		final ServoyResourcesProject resourcesProject, final IProgressMonitor m, final boolean activateSolution, final boolean cleanImport,
		final boolean forceActivateResourceProject, final boolean keepResourceProjectOpen, final Set<IProject> projectsToDeleteAfterImport)
		throws RepositoryException
	{
		return importFromJarFile(importEngine, x11handler, userChannel, repository, newResourcesProjectName, resourcesProject, m, activateSolution, cleanImport,
			null, false, forceActivateResourceProject, keepResourceProjectOpen, projectsToDeleteAfterImport);
	}


	public static IRootObject[] importFromJarFile(final IXMLImportEngine importEngine, final IXMLImportHandlerVersions11AndHigher x11handler,
		final IXMLImportUserChannel userChannel, final EclipseRepository repository, final String newResourcesProjectName,
		final ServoyResourcesProject resourcesProject, final IProgressMonitor m, final boolean activateSolution, final boolean cleanImport,
		final String projectLocation, final boolean reportImportFail, final boolean forceActivateResourcesProject, final boolean keepResourcesProjectOpen,
		final Set<IProject> projectsToDeleteAfterImport)
		throws RepositoryException
	{
		final List<IProject> createdProjects = new ArrayList<IProject>();
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		try
		{
			m.beginTask("Importing...", 5);
			WorkspaceUserManager userManager = new WorkspaceUserManager();
			userManager.setWriteMode(WorkspaceUserManager.WRITE_MODE_MANUAL);
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
			final ServoyProject previouslyActiveProject = servoyModel.getActiveProject();
			final IFileAccess wsa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());

			// the following jobs will be executed backwards (starting from 1 not from 5)
			final Job importJob5 = new RunInWorkspaceJob(new IWorkspaceRunnable()
			{
				public void run(IProgressMonitor monitor) throws CoreException
				{
					m.setTaskName("Finalizing import");
					m.worked(1);
					try
					{
						if (resourcesProject == null) userManager.setResourcesProject(rProject[0]);
						userManager.writeAllSecurityInformation(true);
//						if (!activateSolution)
//						{
//							// job 6
//							Job job = new WorkspaceImporterActivateSolutionJob("Re-activating previously active solution", previouslyActiveProject.getProject(),
//								null, null, null, null);
//							job.setUser(false);
//							job.setSystem(true);
//							job.schedule();
//						}
					}
					catch (Exception e)
					{
						exception[0] = e;
					}
					finally
					{
						try
						{
							if (dummySolProject[0] != null)
							{
								if (projectsToDeleteAfterImport != null)
								{
									projectsToDeleteAfterImport.add(dummySolProject[0]);
								}
								else if (!keepResourcesProjectOpen)
								{
									// dummy did it's job - to activate correct resources project, now we must remove it
									dummySolProject[0].delete(true, true, null);
								}
							}

							if (projectsToDeleteAfterImport != null && !keepResourcesProjectOpen)
							{
								// dummy did it's job - to activate correct resources project, now we must remove it
								for (IProject projectToDelete : projectsToDeleteAfterImport)
								{
									projectToDelete.delete(true, true, null);
								}
							}
						}
						finally
						{
							synchronized (finishedFlag)
							{
								finishedFlag[0] = true;
								finishedFlag.notify();
							}
						}
					}
					m.setTaskName("Finished... updating workbench state");
					m.worked(1);
				}

			});
			importJob5.setRule(ServoyModel.getWorkspace().getRoot());
			importJob5.setUser(false);
			importJob5.setSystem(true);

			final Job importJob3 = new RunInWorkspaceJob(new IWorkspaceRunnable()
			{
				public void run(IProgressMonitor monitor) throws CoreException
				{
					m.setTaskName("Reading solution & modules, updating tables");
					m.worked(1);
					Job job = null;
					try
					{
						XMLEclipseWorkspaceImportHandlerVersions11AndHigher eclipseWorkspaceImportHandler = new XMLEclipseWorkspaceImportHandlerVersions11AndHigher(
							x11handler, repository, resourcesProject, userManager, m, createdProjects, reportImportFail);
						// read jar file & import some stuff into resources project
						rootObjects[0] = importEngine.importFromJarFile(eclipseWorkspaceImportHandler, cleanImport);
						if (rootObjects[0] == null || rootObjects[0].length == 0) throw new RepositoryException("No solution was imported.");

						// create the eclipse solution projects
						for (IRootObject root : rootObjects[0])
						{
							if (root instanceof Solution)
							{
								IProject newProject = ServoyModel.getWorkspace().getRoot().getProject(root.getName());
								IPath oldPath = null;
								if (newProject.exists())
								{
									oldPath = newProject.getLocation();
									newProject.delete(true, true, null);
								}
								IProjectDescription description = ServoyModel.getWorkspace().newProjectDescription(root.getName());
								if (projectLocation != null)
								{
									IPath path = new Path(projectLocation);
									path = path.append(root.getName());
									description.setLocation(path);
								}
								else if (oldPath != null)
								{
									// create the project in the same place where it was
									description.setLocation(oldPath);
								}
								newProject.create(description, null);
								newProject.open(null);

								if (rProject[0] != null)
								{
									description = newProject.getDescription();
									description.setReferencedProjects(new IProject[] { rProject[0] });
									newProject.setDescription(description, null);
								}
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
								if (SolutionMetaData.isPreImportHook(root) || SolutionMetaData.isPostImportHook(root))
								{
									userChannel.info("Execution of hook module '" + root.getName() + "' skipped, it's only supported in repository import.",
										ILogLevel.INFO);
								}
								m.setTaskName("Writing solution/module " + root.getName());
								SolutionSerializer.writePersist(root, wsa, repository, true, false, true);

								List<Pair<String, byte[]>> webPackages = eclipseWorkspaceImportHandler.getWebPackages(root.getName());
								if (webPackages != null)
								{
									for (Pair<String, byte[]> wp : webPackages)
									{
										m.setTaskName("Writing Servoy package " + wp.getLeft());
										wsa.setContents(root.getName() + "/" + SolutionSerializer.NG_PACKAGES_DIR_NAME + "/" + wp.getLeft(), wp.getRight());
									}
								}
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

						ServoyModelManager.getServoyModelManager().getServoyModel().refreshServoyProjects();

						m.setTaskName("Updating workbench state");

						// job 4
						if (activateSolution)
						{
							job = new WorkspaceImporterActivateSolutionJob("Activating imported solution project", mainProject[0], importJob5, exception,
								finishedFlag, dummySolProject[0]);
							job.setUser(false);
							job.setSystem(true);
						}
						else
						{
							importJob5.schedule();
						}
					}
					catch (Exception e)
					{
						exception[0] = e;
					}
					finally
					{
						synchronized (finishedFlag)
						{
							finishedFlag[0] = job == null;
							if (finishedFlag[0] == true) finishedFlag.notify();
						}
					}
					if (job != null) job.schedule();
				}

			});
			importJob3.setRule(ServoyModel.getWorkspace().getRoot());
			importJob3.setUser(false);
			importJob3.setSystem(true);

			if (!activateSolution && !forceActivateResourcesProject)
			{
				importJob3.schedule();
			}
			else
			{
				// need to call following code in a set of jobs through an IWorkspaceRunnable because some operations need the resource listeners to update stuff before running other stuff;
				// by doing it through IWorkspaceRunnable we really make sure that there are no concurrent threads/jobs running in the workspace (Eclipse Notification Manager)
				final Job importJob1 = new RunInWorkspaceJob(new IWorkspaceRunnable()
				{
					public void run(IProgressMonitor monitor) throws CoreException
					{
						m.setTaskName("Preparing for import (activating needed resources project)");
						m.worked(1);
						Job job = null;
						try
						{
							// create/use resources project - and make sure it is active before super.importFromJarFile(...) is called because that changes .dbi files - and those changes
							// should be applied to the proper resources project (maybe other stuff as well from resources project, such as i18n)
							if (resourcesProject == null)
							{
								if (newResourcesProjectName != null)
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
							if (rProject[0] != null) description.setReferencedProjects(new IProject[] { rProject[0] });
							dummySolProject[0].setDescription(description, null);
							Solution dummySolution = (Solution)repository.createNewRootObject(dummySolProject[0].getName(), IRepository.SOLUTIONS);
							SolutionSerializer.writePersist(dummySolution, wsa, repository, true, false, true);

							ServoyModelManager.getServoyModelManager().getServoyModel().refreshServoyProjects();

							m.setTaskName("Updating workbench state");

							// job 2
							job = new WorkspaceImporterActivateSolutionJob("Activating resources project", dummySolProject[0], importJob3, exception,
								finishedFlag, dummySolProject[0])
							{
								@Override
								protected void runCodeAfterActivation() throws RepositoryException
								{
									// verify that correct resources project is active
									if (rProject[0] != null &&
										ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().getProject() != rProject[0])
									{
										throw new RepositoryException("Cannot activate resources project " + rProject[0].getName() + ".");
									}
								}
							};
							job.setUser(false);
							job.setSystem(true);
						}
						catch (Exception e)
						{
							exception[0] = e;
						}
						finally
						{
							try
							{
								if (job == null && dummySolProject[0] != null) dummySolProject[0].delete(true, true, null);
							}
							finally
							{
								synchronized (finishedFlag)
								{
									finishedFlag[0] = job == null;
									if (finishedFlag[0] == true) finishedFlag.notify();
								}
							}
						}
						if (job != null) job.schedule();
					}
				});

				importJob1.setRule(ServoyModel.getWorkspace().getRoot());
				importJob1.setUser(false);
				importJob1.setSystem(true);
				importJob1.schedule();
			}

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
			RootObjectMetaData[] solutionMetadatas = repository.getRootObjectMetaDatasForType(typeId);
			if (solutionMetadatas != null)
			{
				for (RootObjectMetaData metadata : solutionMetadatas)
				{
					if (!Utils.equalObjects(name, metadata.getName()) && Utils.equalObjects(name.toLowerCase(), metadata.getName().toLowerCase()))
					{
						throw new RepositoryException(
							"A different solution with the name '" + metadata.getName() + "' already exists in the workspace; cannot create solution '" + name +
								"' (same name but different letter casing). Rename or remove the existing solution and try again.");
					}
				}
			}
			rootObjectMetaData = repository.getRootObjectMetaData(uuid);
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
					if (!Utils.equalObjects(name, meta.getName()) && Utils.equalObjects(name.toLowerCase(), meta.getName().toLowerCase()))
					{
						throw new RepositoryException(
							"A different style with the name '" + meta.getName() + "' already exists in the resources project; cannot create style '" + name +
								"' (same name but different letter casing). Rename or remove the existing style and try again.");
					}
					if (meta.getRootObjectUuid().equals(uuid))
					{
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
					throw new RepositoryException(
						"A different " + objectType + " with the name '" + name + "' already exists, but the import is protected. Rename or remove the " +
							objectType + " in the workspace (Servoy cannot automatically rename a protected solution).");
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
					throw new RepositoryException(
						"A different " + objectType + " with the name '" + name + "' already exists, but the import is protected. Rename or remove the " +
							objectType + " in the workspace (Servoy cannot automatically rename a protected style).");
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
	}

	public IRootObject importRootObject(RootObjectImportInfo rootObjectImportInfo)
		throws IllegalAccessException, IntrospectionException, InvocationTargetException, RepositoryException
	{
		m.setTaskName("Reading " + rootObjectImportInfo.name);
		IRootObject root = x11handler.importRootObject(rootObjectImportInfo);
		if (root instanceof AbstractRootObject)
		{
			((AbstractRootObject)root).setChangeHandler(new ChangeHandler(repository));
		}
		return root;
	}

	public void importSecurityInfo(ImportInfo importInfo, RootObjectImportInfo rootObjectImportInfo) throws ServoyException
	{
		if (rootObjectImportInfo.securityInfoSet == null) return;
		m.setTaskName("Importing security info");

		for (GroupInfo groupInfo : rootObjectImportInfo.securityInfoSet)
		{
			int groupId = userManager.getGroupId(ApplicationServerRegistry.get().getClientId(), groupInfo.name);
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
					userManager.createGroup(ApplicationServerRegistry.get().getClientId(), groupInfo.name);
				}

				for (GroupElementInfo elementInfo : groupInfo.elementInfoSet)
				{
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
								userManager.setTableSecurityAccess(ApplicationServerRegistry.get().getClientId(), groupInfo.name, elementInfo.columnInfoAccess,
									serverName, tableName, columnName);
							}
						}
						else
						{
							throw new RepositoryException("Invalid value in security element.");
						}
					}
					else
					{
						UUID uuid = UUID.fromString(elementInfo.elementUuid);
						if (uuid != null)
						{//at this point, on cleanImport, imported elements still contains the old uuid's - the new uuid's are generated but not yet replaced the old ones
							UUID formUUID = getFormUUID(importInfo, rootObjectImportInfo, uuid);
							if (importInfo.cleanImport)
							{//on cleanImport we need to add security data to new uuid
								uuid = importInfo.cleanImportUUIDMap.get(elementInfo.elementUuid);
							}
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
				if (hasChildUUID(importInfo, info, childUUID))
				{
					if (importInfo.cleanImport) return importInfo.cleanImportUUIDMap.get(info.uuid);
					return UUID.fromString(info.uuid);
				}
			}
		}
		return null;
	}

	private boolean hasChildUUID(ImportInfo importInfo, RootElementInfo info, UUID childUUID)
	{
		if (info.uuid.equals(childUUID.toString()))
		{
			return true;
		}
		for (RootElementInfo childInfo : info.children)
		{
			if (hasChildUUID(importInfo, childInfo, childUUID))
			{
				return true;
			}
		}
		return false;
	}

	public boolean checkI18NStorage()
	{
		m.setTaskName("Reading i18n data");
		return x11handler.checkI18NStorage();
	}

	public void handleI18NImport(ImportInfo importInfo, String i18nServerName, String i18nTableName, TreeMap<String, MessageEntry> messages) throws Exception
	{
		m.setTaskName("Importing i18n for table '" + i18nTableName + "'");
		boolean insertNewI18NKeysOnly = getUserChannel().getInsertNewI18NKeysOnly();
		IFileAccess workspaceDir = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
		TreeMap<String, MessageEntry> currentMessages = EclipseMessages.readMessages(i18nServerName, i18nTableName, workspaceDir);

		for (Map.Entry<String, MessageEntry> entry : messages.entrySet())
		{
			if (!currentMessages.containsKey(entry.getKey()) || !insertNewI18NKeysOnly) currentMessages.put(entry.getKey(), entry.getValue());
		}

		EclipseMessages.writeMessages(i18nServerName, i18nTableName, currentMessages, workspaceDir);

	}

	public void importDatasources(ImportInfo importInfo) throws RepositoryException
	{
		m.setTaskName("Importing data sources");

		// save the datasources in the workspace
		if (importInfo.datasourcesMap != null && !importInfo.datasourcesMap.isEmpty())
		{
			boolean importDatasources = x11handler.getUserChannel().askImportDatasources() == IXMLImportUserChannel.OK_ACTION;
			if (!importDatasources)
			{
				x11handler.getUserChannel().info("Skipping data sources import", ILogLevel.INFO);
			}

			DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
			if (dmm == null)
			{
				throw new RepositoryException("Error importing data sources, Cannot find internal data model manager.");
			}

			WorkspaceFileAccess ws = new WorkspaceFileAccess(ServoyModel.getWorkspace());

			for (Entry<String, DBIDefinition> entry : importInfo.datasourcesMap.entrySet())
			{
				String dataSource = entry.getKey();
				DBIDefinition dbiDefinition = entry.getValue();

				String[] server_table = DataSourceUtils.getDBServernameTablename(dataSource);
				String serverName = server_table[0];
				String tableName = server_table[1];

				IFile dbiFile;
				if (tableName == null)
				{
					// server.dbi file
					dbiFile = dmm.getServerDBIFile(serverName);
				}
				else
				{
					// table.dbi file
					dbiFile = dmm.getDBIFile(serverName, tableName);
				}

				if (dbiDefinition.getDbiFileContents() != null)
				{
					try
					{
						ws.setUTF8Contents(dbiFile.getFullPath().toString(), dbiDefinition.getDbiFileContents());
					}
					catch (IOException e)
					{
						ServoyLog.logError("Cannot save datasource dbi file", e);
					}
				}
			}
		}
	}

	public void importMetaData(ImportInfo importInfo) throws RepositoryException
	{
		m.setTaskName("Importing meta data");

		// save the metadata in the workspace
		if (importInfo.metadataMap != null)
		{
			boolean importMetaData = x11handler.getUserChannel().askImportMetaData() == IXMLImportUserChannel.OK_ACTION;
			if (!importMetaData)
			{
				x11handler.getUserChannel().info("Skipping meta data import", ILogLevel.INFO);
				// continue here to set the metadata flag for the table
			}

			DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
			if (dmm == null)
			{
				throw new RepositoryException("Error importing metad data table, Cannot find internal data model manager.");
			}

			WorkspaceFileAccess ws = new WorkspaceFileAccess(ServoyModel.getWorkspace());
			for (Entry<String, Set<MetadataDef>> defs : importInfo.metadataMap.entrySet())
			{
				String serverName = defs.getKey();
				Map<String, String> connectionMap = importInfo.substitutionMap.get(Integer.valueOf(IRepository.SERVERS));
				if (connectionMap != null && connectionMap.containsKey(serverName))
				{
					serverName = connectionMap.get(serverName);
				}
				IServer server = ApplicationServerRegistry.get().getServerManager().getServer(serverName);
				if (server == null)
				{
					throw new RepositoryException("Error importing meta data table, Cannot find server '" + serverName + "'.");
				}

				for (MetadataDef def : defs.getValue())
				{
					String dataSource = def.dataSource;
					ITable table;
					try
					{
						String[] stn = DataSourceUtils.getDBServernameTablename(dataSource);
						if (stn == null)
						{
							throw new RepositoryException("Error importing meta data table, Cannot find table '" + dataSource + "'");
						}
						try
						{
							String tableName = stn[1];
							dataSource = DataSourceUtils.createDBTableDataSource(serverName, tableName); // server may be different from server in def.dataSource
							table = server.getTable(tableName);
						}
						catch (RemoteException e)
						{
							throw new RepositoryException("Error importing meta data table, Cannot find table '" + dataSource + "'", e);
						}
						if (!(table instanceof Table))
						{
							throw new RepositoryException("Error importing meta data table, Cannot find table '" + dataSource + "'.");
						}

						if (!((Table)table).isMarkedAsMetaData() && MetaDataUtils.canBeMarkedAsMetaData(table))
						{
							// mark table as meta, also if user said no to import meta data
							((Table)table).setMarkedAsMetaData(true);
							try
							{
								IFile dbi = dmm.getDBIFile(dataSource);
								if (dbi == null)
								{
									throw new RepositoryException("Error importing meta data table, Cannot find dbi file for datasource '" + dataSource + "'.");
								}
								ws.setUTF8Contents(dbi.getFullPath().toString(), dmm.serializeTable(table));
							}
							catch (JSONException e)
							{
								ServoyLog.logError("Cannot save table dbi file", e);
							}
							catch (IOException e)
							{
								ServoyLog.logError("Cannot save table dbi file", e);
							}
						}
					}
					catch (RepositoryException e)
					{
						if (importMetaData) throw e;
						// do not fail on table errors when user said no to import meta data.
						continue;
					}

					if (!importMetaData)
					{
						continue;
					}

					IFile mdf = dmm.getMetaDataFile(dataSource);
					if (mdf == null)
					{
						throw new RepositoryException("Error importing meta data table, Cannot find meta data file for datasource '" + dataSource + "'.");
					}
					try
					{
						ws.setUTF8Contents(mdf.getFullPath().toString(), def.tableMetaData);
						x11handler.getUserChannel().info("Saved meta data for datasource '" + dataSource + "' in workspace.", ILogLevel.INFO);
					}
					catch (IOException e)
					{
						ServoyLog.logError("Error saving meta data for datasource '" + dataSource + "'", e);
						throw new RepositoryException("Error saving meta data for datasource '" + dataSource + "': " + e.getMessage());
					}

					// save it in the table when it is empty
					try
					{
						QuerySelect query = MetaDataUtils.createTableMetadataQuery(table, null);
						IDataSet ds = ApplicationServerRegistry.get().getDataServer().performQuery(ApplicationServerRegistry.get().getClientId(),
							table.getServerName(), null, query, null, null, false, 0, 1, IDataServer.META_DATA_QUERY, null);
						if (ds.getRowCount() == 0)
						{
							MetaDataUtils.loadMetadataInTable(table, def.tableMetaData);
							x11handler.getUserChannel().info("Loaded meta data for datasource '" + dataSource + "' in database.", ILogLevel.INFO);
						}
						else
						{
							x11handler.getUserChannel().info("Meta data for table '" + table.getName() + "' in server '" + server.getName() +
								"' was not loaded (table not empty), please update table meta data", ILogLevel.INFO);
						}
					}
					catch (Exception e)
					{
						ServoyLog.logError("Error loading meta data for datasource '" + dataSource + "'", e);
						throw new RepositoryException("Error loading meta data for datasource '" + dataSource + "': " + e.getMessage());
					}
				}
			}
		}
	}

	public void importSampleData(JarFile jarFile, ImportInfo importInfo)
	{
		m.setTaskName("Importing sample data");
		x11handler.importSampleData(jarFile, importInfo);
	}

	public void importBlobs(JarFile jarFile, List<String> blobs, ImportInfo importInfo, Map<String, byte[]> digestMap)
		throws IOException, RepositoryException, NoSuchAlgorithmException
	{
		m.setTaskName("Importing blobs");
		x11handler.importBlobs(jarFile, blobs, importInfo, digestMap);
	}

	public void checkDatabaseInfo(ImportInfo importInfo, ImportTransactable importTransactable) throws RepositoryException
	{
		m.setTaskName("Checking database info");
		x11handler.checkDatabaseInfo(importInfo, importTransactable);
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
			if (mode == IXMLImportUserChannel.IMPORT_USER_POLICY_DONT)
			{
				x11handler.getUserChannel().info("Skipped import of user information", ILogLevel.INFO);
				return;
			}
			m.setTaskName("Importing user info");
			boolean addAdmins = x11handler.getUserChannel().getAddUsersToAdministratorGroup();
			HashMap<String, Integer> groupIdCache = new HashMap<String, Integer>();

			for (UserInfo userInfo : userInfoSet)
			{
				// Look up the user with the given name.
				int userId = userManager.getUserIdByUserName(ApplicationServerRegistry.get().getClientId(), userInfo.name);

				// If override users is set, delete the user first.
				if (userId != -1 && mode == IXMLImportUserChannel.IMPORT_USER_POLICY_OVERWRITE_COMPLETELY)
				{
					userManager.deleteUser(ApplicationServerRegistry.get().getClientId(), userId);
					userId = -1;
				}

				// If the user does not exist, create.
				if (userId == -1)
				{
					userId = userManager.createUser(ApplicationServerRegistry.get().getClientId(), userInfo.name, userInfo.password, userInfo.uid, true);
					if (userId == -1)
					{
						x11handler.getUserChannel().info("Could not create user with name `" + userInfo.name + "'.", ILogLevel.ERROR);
						continue;
					}
				}

				for (String groupName : userInfo.groupSet)
				{
					Integer groupId = groupIdCache.get(groupName);
					if (groupId == null)
					{
						groupId = new Integer(userManager.getGroupId(ApplicationServerRegistry.get().getClientId(), groupName));
						if (groupId.intValue() == -1)
						{
							if (IRepository.ADMIN_GROUP.equals(groupName))
							{
								// admin group is considered to always exist; but in case of a newly created resources project it isn't there yet
								groupId = new Integer(userManager.createGroup(ApplicationServerRegistry.get().getClientId(), IRepository.ADMIN_GROUP));
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
					userManager.addUserToGroup(ApplicationServerRegistry.get().getClientId(), userId, groupId.intValue());
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

	public void executePreImport(ImportInfo importInfo, IRootObject[] rootObjects, ImportTransactable importTransactable) throws RepositoryException
	{
		x11handler.executePreImport(importInfo, rootObjects, importTransactable);
	}

	public void importingDone(ImportInfo importInfo, IRootObject[] rootObjects, ImportTransactable importTransactable) throws RepositoryException
	{
		x11handler.importingDone(importInfo, rootObjects, importTransactable);
	}

	public void importingFailed(ImportInfo importInfo, ImportTransactable importTransactable, Exception e)
	{
		if (reportImportFail)
		{
			// show error
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					MessageDialog.openError(UIUtils.getActiveShell(), "Importing " + importInfo.main.name, e.getMessage());
				}
			});
		}
		else
		{
			// old behavior
			ServoyModelManager.getServoyModelManager().getServoyModel().setActiveProject(null, false);
		}
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

	public IPersist loadDeletedObjectByElementId(IRootObject rootObject, int elementId, ISupportChilds parent)
		throws RepositoryException, IllegalAccessException, IntrospectionException, InvocationTargetException
	{
		return x11handler.loadDeletedObjectByElementId(rootObject, elementId, parent);
	}

	public void setStyleActiveRelease(IRootObject[] rootObjects) throws RepositoryException
	{
		x11handler.setStyleActiveRelease(rootObjects);
	}


	/*
	 * @see com.servoy.j2db.util.xmlxport.IXMLImportHandlerVersions11AndHigher#setAskForImportServerName(boolean)
	 */
	public void setAskForImportServerName(boolean askForImportServerName)
	{
		x11handler.setAskForImportServerName(askForImportServerName);
	}


	public void checkMovedObjects(ImportInfo importInfo) throws RepositoryException
	{

	}

	private final Map<String, List<Pair<String, byte[]>>> webPackages = new HashMap<String, List<Pair<String, byte[]>>>();

	/*
	 * @see com.servoy.j2db.util.xmlxport.IXMLImportHandlerVersions11AndHigher#loadWebPackage(java.lang.String, java.lang.String, java.util.jar.JarFile,
	 * java.util.jar.JarEntry)
	 */
	@Override
	public void loadWebPackage(String solutionName, String webPackageName, JarFile jarFile, JarEntry jarEntry)
	{
		List<Pair<String, byte[]>> solutionWebPackages = webPackages.get(solutionName);
		if (solutionWebPackages == null)
		{
			solutionWebPackages = new ArrayList<Pair<String, byte[]>>();
			webPackages.put(solutionName, solutionWebPackages);
		}
		try
		{
			solutionWebPackages.add(new Pair<String, byte[]>(webPackageName, Utils.getBytesFromInputStream(jarFile.getInputStream(jarEntry))));
		}
		catch (IOException ex)
		{
			ServoyLog.logError(ex);
		}
	}


	/*
	 * @see com.servoy.j2db.util.xmlxport.IXMLImportHandlerVersions11AndHigher#getWebPackages(java.lang.String)
	 */
	@Override
	public List<Pair<String, byte[]>> getWebPackages(String solutionName)
	{
		return webPackages.get(solutionName);
	}

	@Override
	public boolean shouldCheckRepositorySolutionsOverride()
	{
		return true;
	}
}