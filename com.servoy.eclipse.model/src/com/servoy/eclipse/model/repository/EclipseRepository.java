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
package com.servoy.eclipse.model.repository;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.AbstractRootObject;
import com.servoy.j2db.persistence.ChangeHandler;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectCache;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.RootObjectReference;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.StringResource;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner,jblok
 */
public class EclipseRepository extends AbstractRepository implements IRepository
{
	public static interface ActivityMonitor
	{
		void loadingRootObject(RootObjectMetaData rootObject);
	}

	private final List<ActivityMonitor> monitors = new ArrayList<ActivityMonitor>();
	private final Properties settings;
	private final IFileAccess wsa;
	private String resourcesProjectName;

	public EclipseRepository(IServerManagerInternal sm, Properties settings)
	{
		super(sm);
		wsa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
		this.settings = settings;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.AbstractRepository#getActiveSolutionModuleMetaDatas(int)
	 */
	@Override
	public List<RootObjectReference> getActiveSolutionModuleMetaDatas(int solutionId) throws RepositoryException
	{
		Solution sol = (Solution)getActiveRootObject(solutionId);
		if (sol != null && !Utils.getTokenElementsAsList(sol.getModulesNames(), ",", true).stream().allMatch(this::isSolutionLoaded))
		{
			ForkJoinPool pool = new ForkJoinPool();
			ConcurrentMap<String, String> checked = new ConcurrentHashMap<String, String>();
			checked.put(sol.getName(), sol.getName());
			loadAllModules(sol, pool, checked);
			pool.awaitQuiescence(15, TimeUnit.MINUTES);
			pool.shutdown();
		}
		return super.getActiveSolutionModuleMetaDatas(solutionId);
	}

	/**
	 * @param sol
	 * @throws RepositoryException
	 */
	private void loadAllModules(Solution sol, ForkJoinPool pool, ConcurrentMap<String, String> checked)
	{
		List<String> moduleNames = Utils.getTokenElementsAsList(sol.getModulesNames(), ",", true);
		for (String name : moduleNames)
		{
			if (checked.putIfAbsent(name, name) == null)
			{
				if (!isSolutionLoaded(name))
				{
					pool.execute(() -> {
						try
						{
							Solution module = (Solution)getRootObject(name, IRepository.SOLUTIONS, 0);
							if (module != null)
							{
								loadAllModules(module, pool, checked);
							}
						}
						catch (RepositoryException e)
						{
							Debug.error(e);
						}
					});
				}
			}
		}
	}

	@Override
	protected ContentSpec loadContentSpec() throws RepositoryException
	{
		return StaticContentSpecLoader.getContentSpec();
	}

	@Override
	public Properties getUserProperties(int user_id) throws RepositoryException
	{
		if (user_id == IRepository.SYSTEM_USER_ID)
		{
			return settings;
		}
		return new Properties();
	}

	public void setUserProperties(int user_id, Map props) throws RepositoryException
	{
		if (user_id == IRepository.SYSTEM_USER_ID)
		{
			settings.putAll(props);
		}
	}

	@Override
	protected Collection<RootObjectMetaData> loadRootObjectMetaDatas() throws Exception
	{
		// TODO: this is the correct way to get the list of projects (it also gets the projects from another location):
		// ResourcesPlugin.getWorkspace().getRoot().getProjects();
		List<RootObjectMetaData> retval = new ArrayList<RootObjectMetaData>();
		String[] dirs = wsa.list();
		for (String element : dirs)
		{
			SolutionMetaData smd = SolutionDeserializer.deserializeRootMetaData(this, wsa.getProjectFile(element), element);
			if (smd != null) retval.add(smd);
		}
		return retval;
	}

	public boolean isSolutionMetaDataLoaded(String solutionName) throws RepositoryException
	{
		if (isRootObjectCacheInitialized())
		{
			return getRootObjectMetaData(solutionName, IRepository.SOLUTIONS) != null;
		}
		return false;
	}

	private int last_element_id = Integer.MAX_VALUE / 2;

	public int getNewElementID(UUID new_uuid)
	{
		synchronized (uuid_element_id_map)
		{
			int element_id = last_element_id++;
			if (new_uuid != null) uuid_element_id_map.put(new_uuid, new Integer(element_id));
			return element_id;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.AbstractRepository#getRootObjectCache()
	 */
	@Override
	protected RootObjectCache getRootObjectCache() throws RepositoryException
	{
		// first just call the servoy model so that we don't get sync block in RootObjectCache
		// that again calls loadRootObject below that wants to load the ServoyModel
		ServoyModelFinder.getServoyModel();
		return super.getRootObjectCache();
	}

	@Override
	protected IRootObject loadRootObject(RootObjectMetaData romd, int release) throws RepositoryException
	{
		notifyLoadingRootObject(romd);
		IRootObject rootObject = null;
		try
		{
			switch (romd.getObjectTypeId())
			{
				case IRepository.STYLES :
				case IRepository.TEMPLATES :

					ServoyResourcesProject resourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
					if (resourcesProject != null)
					{
						rootObject = StringResourceDeserializer.readStringResource(this, wsa.getProjectFile(resourcesProject.getProject().getName()),
							resourcesProject.getProject().getName(), romd);
						if (rootObject == null)
						{
							ServoyLog.logError("Could not read resource " + romd.getName(), null);
						}
						if (romd.getObjectTypeId() == IRepository.STYLES)
						{
							StringResourceDeserializer.fixStyleCssProfile(resourcesProject.getProject().getName(), (Style)rootObject, true);
						}
					}
					else
					{
						ServoyLog.logError("Could not find resources project ", null);
					}
					break;

				case IRepository.SOLUTIONS :

					ServoyProject sp = ServoyModelFinder.getServoyModel().getServoyProject(romd.getName());
					SolutionDeserializer sd = new SolutionDeserializer(this, sp);
					long time = System.currentTimeMillis();
					rootObject = sd.readSolution(wsa.getProjectFile(romd.getName()), (SolutionMetaData)romd, null, false);
					if (rootObject == null)
					{
						ServoyLog.logError("Could not read solution " + romd.getName(), null);
					}
					ServoyLog.logInfo("Time taken to read in the solution " + romd.getName() + ": " + (System.currentTimeMillis() - time) + ", thread: " +
						Thread.currentThread().getName());
					break;
			}
		}
		catch (RepositoryException ex)
		{
			ServoyLog.logError(ex);
		}
		return rootObject;
	}

	public byte[] getMediaBlob(int blob_id) throws RepositoryException
	{
		return null;//should never be called
	}

	public void setRootObjectActiveRelease(int rootObjectId, int releaseNumber) throws RepositoryException
	{
		//should never be called
	}

	public long[] getActiveRootObjectsLastModified(int[] rootObjectIds) throws RepositoryException
	{
		long[] retval = new long[rootObjectIds.length];
		for (int i = 0; i < rootObjectIds.length; i++)
		{
			retval[i] = getActiveRootObject(rootObjectIds[i]).getLastModifiedTime();
		}
		return retval;
	}

	@Override
	public void removeRootObject(int rootObjectId) throws RepositoryException
	{
		super.removeRootObject(rootObjectId);
		//nop, remove project manually in eclipse
	}


	@Override
	public IRootObject createNewRootObject(String name, int objectTypeId, int newElementID, UUID uuid) throws RepositoryException
	{
		return createRootObject(createNewRootObjectMetaData(newElementID, uuid, name, objectTypeId, 1, 1));
	}

	@Override
	public void restoreObjectToCurrentRelease(IPersist object)
	{
	}

	public void registerResourceMetaDatas(String resourcesProjectName, int objectTypeId)
	{
		this.resourcesProjectName = resourcesProjectName;
		// remove all old resources
		try
		{
			RootObjectMetaData[] old = getRootObjectMetaDatas();
			for (RootObjectMetaData meta : old)
			{
				if (meta.getObjectTypeId() == objectTypeId)
				{
					removeRootObject(meta.getRootObjectId());
				}
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("Can not remove old set of resources before registering the new set", e);
		}

		// register new styles/templates
		if (resourcesProjectName != null)
		{
			try
			{
				RootObjectMetaData[] rmds = StringResourceDeserializer.deserializeMetadatas(this, wsa.getProjectFile(resourcesProjectName),
					resourcesProjectName, objectTypeId);
				for (RootObjectMetaData meta : rmds)
				{
					addRootObjectMetaData(meta);
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError("Can not register new resource", ex);
			}
		}
	}

	public RootObjectMetaData[] deserializeMetaDatas(String resourcesProjectName, int objectTypeId) throws RepositoryException
	{
		return StringResourceDeserializer.deserializeMetadatas(this, wsa.getProjectFile(resourcesProjectName), resourcesProjectName, objectTypeId);
	}

	/**
	 * Reads the meta data for the given project and adds it to the cache. This is useful for Servoy-enabling projects in Eclipse (it will allow the creation of
	 * solution objects for that project) during runtime (imported projects, checked-out projects).
	 *
	 * @param projectName the name of the Servoy Eclipse project.
	 * @return the meta data for that project's solution.
	 */
	public RootObjectMetaData registerSolutionMetaData(String projectName)
	{
		SolutionMetaData smd = null;
		try
		{
			smd = SolutionDeserializer.deserializeRootMetaData(this, wsa.getProjectFile(projectName), projectName);
			if (smd != null)
			{
				addRootObjectMetaData(smd);
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("Cannot register new root object meta data to cache; Project " + projectName, e);
		}
		return smd;
	}

	@Override
	public UUID resolveUUIDForElementId(int id) throws RepositoryException
	{
		Integer oID = new Integer(id);
		if (foreignElementUUIDs.containsValue(oID))
		{
			Set<UUID> keys = foreignElementUUIDs.keySet();
			for (UUID key : keys)
			{
				if (foreignElementUUIDs.get(key).equals(oID)) return key;
			}
		}
		return null;
	}

	@Override
	public int resolveIdForElementUuid(UUID id) throws RepositoryException
	{
		return getNewElementID(id);
	}

	@Override
	public int getElementIdForUUID(UUID id) throws RepositoryException
	{
		if (foreignElementUUIDs.containsKey(id)) return foreignElementUUIDs.get(id).intValue();

		return super.getElementIdForUUID(id);
	}

	private final Map<UUID, Integer> foreignElementUUIDs = new HashMap<UUID, Integer>();

	public void loadForeignElementsIDs(Map<UUID, Integer> foreignUUIDs)
	{
		foreignElementUUIDs.putAll(foreignUUIDs);
	}

	public void loadForeignElementsIDs(final IPersist rootObject)
	{
		rootObject.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				EclipseRepository.this.foreignElementUUIDs.put(o.getUUID(), new Integer(o.getID()));
				Map<UUID, Integer> map = ((AbstractBase)o).getSerializableRuntimeProperty(AbstractBase.UUIDToIDMapProperty);
				if (map != null)
				{
					for (Entry<UUID, Integer> entry : map.entrySet())
					{
						foreignElementUUIDs.put(entry.getKey(), entry.getValue());
					}
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});
	}

	public void clearForeignElementsIds()
	{
		foreignElementUUIDs.clear();
	}

	@Override
	public void updateRootObject(IRootObject rootObject) throws RepositoryException
	{
		ChangeHandler rootObjectChangeHandler = rootObject.getChangeHandler();
		if (rootObjectChangeHandler == null)
		{
			rootObjectChangeHandler = new ChangeHandler(this);
			((AbstractRootObject)rootObject).setChangeHandler(rootObjectChangeHandler);
		}

		if (rootObject instanceof Solution)
		{
			updateNodesInWorkspace(new IPersist[] { rootObject }, true);
		}
		else if (rootObject instanceof StringResource)
		{
			StringResourceDeserializer.writeStringResource(((StringResource)rootObject), wsa, resourcesProjectName);
		}
	}

	public void updateNodesInWorkspace(final IPersist[] nodes, final boolean recursive)
	{
		updateNodesInWorkspace(nodes, recursive, true);
	}

	public void updateNodesInWorkspace(final IPersist[] nodes, final boolean recursive, boolean block)
	{
		if (nodes == null || nodes.length == 0) return;

		// Note: some plugins (svn plugin) enforces a refresh which causes resources changes to be picked up
		// in separate chunks in stead of all in 1 set.
		// Now using a listener interface to notify when saving is done.

		final CountDownLatch latch = new CountDownLatch(1);

		WorkspaceJob saveJob = new WorkspaceJob("Save solution data")
		{
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				final IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
				int collectingChangesValue = servoyModel.getResourceChangesHandlerCounter().increment(); // new value, so 1 if we are the first
				try
				{
					updateNodes(nodes, recursive);
				}
				finally
				{
					if (collectingChangesValue > 1)
					{
						servoyModel.getResourceChangesHandlerCounter().decrement();
						latch.countDown();
					}
					else
					{
						// notify work done in separate job in case not all changes have been processed yet.
						WorkspaceJob fireDoneJob = new WorkspaceJob("Save solution data")
						{
							@Override
							public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
							{
								if (servoyModel.getResourceChangesHandlerCounter().decrement() == 0)
								{
									latch.countDown();
								}
								return Status.OK_STATUS;
							}
						};
						fireDoneJob.setRule(getRule());
						fireDoneJob.setUser(isUser());
						fireDoneJob.schedule();
					}
				}
				return Status.OK_STATUS;
			}
		};
		saveJob.setUser(false);
		saveJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
		saveJob.schedule();
		if (block)
		{
			try
			{
				// if saving takes very long it is probably a deadlock
				latch.await(15, TimeUnit.SECONDS);
			}
			catch (InterruptedException e)
			{
				ServoyLog.logError("Interrupted while waiting for save job", e);
			}
		}
		// No need to refresh workspace, files are written using workspace access, so changes are picked up already
	}

	public void updateNodes(IPersist[] nodes, boolean recursive)
	{
		for (final IPersist node : nodes)
		{
			try
			{
				updateNode(node.getRootObject(), false);//check root if to write for fileversion
				updateNode(node, recursive);
			}
			catch (RepositoryException e)
			{
				ServoyModelFinder.getServoyModel().reportSaveError(e);
			}
		}
	}

	/**
	 * Only for nodes that are part of a solution tree (not for styles).
	 */
	public void updateNode(final IPersist node, final boolean recursive) throws RepositoryException
	{
		final Solution solution = (Solution)node.getRootObject();

		// security check, the 'real' solution in the project is read-only.
		IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
		ServoyProject sp = servoyModel.getServoyProject(solution.getName());
		if (sp != null && sp.getSolution() == solution)
		{
			throw new IllegalArgumentException("Main solution must not be updated");
		}

		final boolean nodeDeleted = solution.isRegisteredRemoved(node);

		// Build a map with parents that have deleted children
		final Map<UUID, List<IPersist>> parentsWithDeletedChildren = new HashMap<UUID, List<IPersist>>();
		Iterator<IPersist> it = solution.getRegisteredRemovedObjects();
		while (it.hasNext())
		{
			IPersist removed = it.next();
			UUID parentUuid = removed.getParent().getUUID();
			if (!parentsWithDeletedChildren.containsKey(parentUuid))
			{
				parentsWithDeletedChildren.put(parentUuid, new ArrayList<IPersist>());
			}
			parentsWithDeletedChildren.get(parentUuid).add(removed);
		}

		final List<IPersist> processedNodes = new ArrayList<IPersist>();
		final Set<Integer> processedNodesThatWereCreated = new HashSet<>(); // stores indexes in processedNodes that were new persists
		final Set<Integer> processedNodesThatWereDeleted = new HashSet<>(); // stores indexes in processedNodes that were deleted persists

		final List<IScriptElement> scriptsToRegenerate = new ArrayList<IScriptElement>();

		// visit each node, find deletions and renames
		IPersistVisitor persistVisitor = new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				// delete files from deleted objects
				List<IPersist> deleteList = new ArrayList<IPersist>();
				if (o == node && nodeDeleted)
				{
					deleteList.add(node);
				}
				if (parentsWithDeletedChildren.containsKey(o.getUUID()))
				{
					deleteList.addAll(parentsWithDeletedChildren.get(o.getUUID()));
				}
				while (deleteList.size() > 0)
				{
					IPersist toDelete = deleteList.remove(0);

					if (parentsWithDeletedChildren.containsKey(toDelete.getUUID()))
					{
						deleteList.addAll(parentsWithDeletedChildren.get(toDelete.getUUID()));
					}
					if (toDelete instanceof IScriptElement)
					{
						scriptsToRegenerate.add((IScriptElement)toDelete);
					}
					else if (SolutionSerializer.isCompositeItem(toDelete))
					{
						ISupportChilds parent = toDelete.getParent();
						while (SolutionSerializer.isCompositeItem(parent))
						{
							parent = parent.getParent();
						}
						parent.flagChanged();
					}
					else
					{
						Pair<String, String> filePath = SolutionSerializer.getFilePath(toDelete, true);
						if (filePath != null)
						{
							String fileRelativePath = filePath.getLeft() + filePath.getRight();
							if (wsa.exists(fileRelativePath))
							{
								try
								{
									wsa.delete(fileRelativePath);
									if (SolutionSerializer.isJSONFile(fileRelativePath))
									{
										// check if a script needs to be deleted
										String[] scriptPaths = SolutionSerializer.getScriptPaths(toDelete, true);

										if (scriptPaths != null)
										{
											for (String scriptPath : scriptPaths)
											{
												wsa.delete(scriptPath);
											}
										}

										// forms/orders.obj has now been deleted, also delete the forms/orders directory if it exists
										String elementsDirectory = fileRelativePath.substring(0,
											fileRelativePath.length() - SolutionSerializer.JSON_FILE_EXTENSION_SIZE);
										if (wsa.exists(elementsDirectory))
										{
											wsa.delete(elementsDirectory);
										}
									}
								}
								catch (IOException e)
								{
									ServoyLog.logError("Could not delete file " + fileRelativePath, e);
								}
							}
						}
					}
					processedNodes.add(toDelete);
					processedNodesThatWereDeleted.add(Integer.valueOf(processedNodes.size() - 1));
				}


				// rename files from renamed objects
				if (o.isChanged() && o instanceof AbstractBase && ((AbstractBase)o).getRuntimeProperty(AbstractBase.NameChangeProperty) != null &&
					!(o instanceof IScriptElement) && !SolutionSerializer.isCompositeItem(o))
				{
					Pair<String, String> fileFromPath = SolutionSerializer.getFilePath(o, true);
					String fileRelativePath = fileFromPath.getLeft() + fileFromPath.getRight();
					if (wsa.exists(fileRelativePath))
					{
						// use fileFromPath directory for accessing fileTo. Since we are visiting depth-first the super directory has not been processed yet.
						// In case of rename of a form and a form element in 1 save we rename elementold to elementnew in directory formnameold; directory rename
						// formnameold to formnamenew will be done in the visit step going up the tree.
						String fileToName = SolutionSerializer.getFileName(o, false);
						try
						{
							String path = fileFromPath.getLeft();
							if (o instanceof TableNode)
							{
								path = SolutionSerializer.getRelativePath(o, false);
								if (!wsa.exists(path)) wsa.createFolder(path);
							}
							wsa.move(fileRelativePath, path + fileToName);
							if (SolutionSerializer.isJSONFile(fileToName))
							{
								// check if a script needs to be renamed
								String oldScriptName = SolutionSerializer.getScriptName(o, true);
								String newScriptName = SolutionSerializer.getScriptName(o, false);
								String oldScriptPath = SolutionSerializer.getScriptPath(o, true);
								String newScriptPath = SolutionSerializer.getScriptPath(o, false);
								if ((oldScriptName != null && !oldScriptName.equals(newScriptName)) ||
									(o instanceof TableNode && oldScriptPath != null && !oldScriptPath.equals(newScriptPath)))
								{
									// note that the directory will be renamed below, just need to rename the file
									if (!(o instanceof TableNode))
										newScriptPath = oldScriptPath.substring(0, oldScriptPath.length() - oldScriptName.length()) + newScriptName;
									if (wsa.exists(oldScriptPath))
									{
										try
										{
											wsa.move(oldScriptPath, newScriptPath);
										}
										catch (IOException e)
										{
											ServoyLog.logError("Could not rename script " + oldScriptPath, e);
										}
									}
								}

								// forms/orders.obj has now been renamed, also rename the forms/orders directory if it exists
								String fromElementsDirectory = fileFromPath.getLeft() +
									fileFromPath.getRight().substring(0, fileFromPath.getRight().length() - SolutionSerializer.JSON_FILE_EXTENSION_SIZE);
								if (wsa.exists(fromElementsDirectory))
								{
									String toElementsDirectory = fileFromPath.getLeft() +
										fileToName.substring(0, fileToName.length() - SolutionSerializer.JSON_FILE_EXTENSION_SIZE);
									try
									{
										wsa.move(fromElementsDirectory, toElementsDirectory);
									}
									catch (IOException e)
									{
										ServoyLog.logError("Could not rename directory " + toElementsDirectory, e);
									}
								}
							}

							if (o.getTypeID() == IRepository.FORMS)
							{
								// move form security file
								String oldSecFileRelativePath = fileRelativePath.substring(0,
									fileRelativePath.lastIndexOf(SolutionSerializer.FORM_FILE_EXTENSION)) + DataModelManager.SECURITY_FILE_EXTENSION_WITH_DOT;
								if (wsa.exists(oldSecFileRelativePath))
								{
									wsa.move(oldSecFileRelativePath,
										fileFromPath.getLeft() + fileToName.substring(0, fileToName.lastIndexOf(SolutionSerializer.FORM_FILE_EXTENSION)) +
											DataModelManager.SECURITY_FILE_EXTENSION_WITH_DOT);
								}
								// move form less file
								String oldLessFileRelativePath = fileRelativePath.substring(0,
									fileRelativePath.lastIndexOf(SolutionSerializer.FORM_FILE_EXTENSION)) + SolutionSerializer.FORM_LESS_FILE_EXTENSION;
								if (wsa.exists(oldLessFileRelativePath))
								{
									wsa.move(oldLessFileRelativePath,
										fileFromPath.getLeft() + fileToName.substring(0, fileToName.lastIndexOf(SolutionSerializer.FORM_FILE_EXTENSION)) +
											SolutionSerializer.FORM_LESS_FILE_EXTENSION);
								}
							}
						}
						catch (IOException e)
						{
							ServoyLog.logError("Could not rename file " + fileRelativePath, e);
						}
					}
				}

				processedNodes.add(o);
				if (sp == null || AbstractRepository.searchPersist(sp.getSolution(), o) == null)
				{
					// it is new, only in editing solution
					processedNodesThatWereCreated.add(Integer.valueOf(processedNodes.size() - 1));
				}
				return CONTINUE_TRAVERSAL;
			}
		};
		if (recursive)
		{
			node.acceptVisitorDepthFirst(persistVisitor);
		}
		else
		{
			persistVisitor.visit(node);
		}

		IDeveloperRepository repository = ApplicationServerRegistry.get().getDeveloperRepository(); // why not use "this" instead?
		if (node instanceof IScriptElement && !nodeDeleted && node.isChanged())
		{
			// if the node itself is a method or variable, and has changed content, then we must regenerate
			// the whole script file (just like for deleted script members) - so:
			// 1. deleted script members are ignored by SolutionSerializer.writePersist and they need to be taken care of separately so
			// they are added to scriptsToRegenerate
			// 2. modified script members that have "node" as ancestor will be serialized correctly with SolutionSerializer.writePersist (the else branch of this if)
			// 3. (the case for this branch) the "node" itself is a modified script member => you cannot use SolutionSerializer.writePersist
			// because that would delete all other script members from that script file so you need to regenerate this separately too
			scriptsToRegenerate.add((IScriptElement)node);
		}
		else if (!nodeDeleted || node instanceof Media) // deleted Media, have to rewrite medias.obj
		{
			SolutionSerializer.writePersist(node, wsa, repository, true, true, recursive);
		}

		// regenerate the script files for parents that have deleted scripts
		Set<String> scriptPaths = new HashSet<String>();
		for (IScriptElement scriptToRegenerate : scriptsToRegenerate)
		{
			String scriptPath = SolutionSerializer.getScriptPath(scriptToRegenerate, false);
			if (scriptPaths.add(scriptPath))
			{
				final IFile scriptFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(scriptPath));
				if (scriptFile.exists())
				{
					String fileContent = SolutionSerializer.generateScriptFile(scriptToRegenerate.getParent(), scriptPath, repository, null);
					ModelUtils.getUnexpectedSituationHandler().writeOverExistingScriptFile(scriptFile, fileContent);
				} // else maybe the whole folder that contains the file was deleted
			}
		}

		// remove the processed nodes from the new, renamed and removed lists and clear the changed flags
		for (int i = 0; i < processedNodes.size(); i++)
		{
			IPersist processed = processedNodes.get(i);
			// delete and create are fired directly by AbstractBase; we need to fire the changed ones so that
			// the persist index that listens to it does get updated correctly
			if (!processedNodesThatWereDeleted.contains(Integer.valueOf(i)) && !processedNodesThatWereCreated.contains(Integer.valueOf(i)) &&
				processed.isChanged())
			{
				solution.getChangeHandler().fireIPersistChanged(processed);
			}
		}
		for (IPersist processed : processedNodes)
		{
			solution.clearEditingState(processed);
		}
	}

	public <T extends IPersist> HashMap<T, List<String>> getAllFilesForPersists(List<T> persists)
	{
		HashMap<T, List<String>> allFiles = new HashMap<>();
		for (T persist : persists)
		{
			ArrayList<String> files = new ArrayList<String>();
			Pair<String, String> filePath = SolutionSerializer.getFilePath(persist, true);
			if (filePath != null)
			{
				String fileRelativePath = filePath.getLeft() + filePath.getRight();
				if (wsa.exists(fileRelativePath))
				{
					files.add(fileRelativePath);
					if (SolutionSerializer.isJSONFile(fileRelativePath))
					{
						String[] scriptPaths = SolutionSerializer.getScriptPaths(persist, true);

						if (scriptPaths != null)
						{
							for (String scriptPath : scriptPaths)
							{
								if (wsa.exists(scriptPath)) files.add(scriptPath);
							}
						}

						if (persist instanceof Form frm)
						{
							String lessPath = filePath.getLeft() + frm.getName() + SolutionSerializer.FORM_LESS_FILE_EXTENSION;
							if (wsa.exists(lessPath)) files.add(lessPath);
						}

					}
				}
			}
			allFiles.put(persist, files);
		}
		return allFiles;
	}

	@Override
	public Map<String, Method> getGettersViaIntrospection(Object obj) throws IntrospectionException
	{
		return super.getGettersViaIntrospection(obj);
	}

	@Override
	public Map<String, Method> getSettersViaIntrospection(Object obj) throws IntrospectionException
	{
		return super.getSettersViaIntrospection(obj);
	}

	public void addActivityMonitor(ActivityMonitor monitor)
	{
		if (!monitors.contains(monitor))
		{
			monitors.add(monitor);
		}
	}

	public void removeActivityMonitor(ActivityMonitor monitor)
	{
		monitors.remove(monitor);
	}

	private void notifyLoadingRootObject(RootObjectMetaData romd)
	{
		for (ActivityMonitor m : monitors)
		{
			m.loadingRootObject(romd);
		}
	}

	@Override
	public synchronized UUID getRepositoryUUID() throws RepositoryException
	{
		UUID repositoryUUID;
		if (resourcesProjectName == null) return super.getRepositoryUUID();
		if (!RepositorySettingsDeserializer.existsSettings(wsa, resourcesProjectName))
		{
			repositoryUUID = super.getRepositoryUUID();
			RepositorySettingsDeserializer.writeRepositoryUUID(wsa, resourcesProjectName, repositoryUUID);
		}
		else repositoryUUID = RepositorySettingsDeserializer.readRepositoryUUID(wsa, resourcesProjectName);

		return repositoryUUID;
	}
}