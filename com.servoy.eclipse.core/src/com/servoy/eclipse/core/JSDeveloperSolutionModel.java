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

package com.servoy.eclipse.core;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.core.util.RunInWorkspaceJob;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.inmemory.MemServer;
import com.servoy.eclipse.model.inmemory.MemTable;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.IDebugClient;
import com.servoy.j2db.IForm;
import com.servoy.j2db.dataprocessing.datasource.JSDataSource;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;

/**
 * Class that is a special interface in javascript only there in the developer that bridges between the runtime client and the developers workspace
 *
 * @author jcompagner
 * @since 6.0
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "servoyDeveloper", scriptingName = "servoyDeveloper")
public class JSDeveloperSolutionModel
{

	private final IDebugClient state;
	private final Map<UUID, Integer> foreignElementUUIDs = new HashMap<UUID, Integer>();


	public JSDeveloperSolutionModel(IDebugClient state)
	{
		this.state = state;
	}

	/**
	 * Saves all changes made through the solution model into the workspace.
	 * Please note that this method only saves the new in memory datasources,
	 * if you would like to override the existing ones use servoyDeveloper.save(true).
	 */
	public void js_save()
	{
		js_save(false);
	}

	/**
	 * Saves all changes made through the solution model into the workspace.
	 *
	 * @param override Override existing in memory tables.
	 */
	public void js_save(final boolean override)
	{
		IWorkspaceRunnable saveJob = new IWorkspaceRunnable()
		{
			@Override
			public void run(IProgressMonitor monitor) throws CoreException
			{
				final IFileAccess wfa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
				Solution solutionCopy = state.getFlattenedSolution().getSolutionCopy();
				EclipseRepository eclipseRepository = null;
				try
				{
					List<IPersist> objectsToSave = new ArrayList<IPersist>(solutionCopy.getAllObjectsAsList());
					if (!state.getFoundSetManager().getInMemDataSourceNames().isEmpty())
					{
						ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(state.getSolutionName());
						MemServer memServer = servoyProject.getMemServer();
						DataModelManager dataModelManager = ServoyModelFinder.getServoyModel().getDataModelManager();
						for (String tableName : state.getFoundSetManager().getInMemDataSourceNames())
						{
							String dataSourceName = DataSourceUtils.INMEM_DATASOURCE_SCHEME_COLON + tableName;
							ITable table = state.getFoundSetManager().getTable(dataSourceName);
							if (table == null)
							{
								Debug.trace("Cannot save the in memory table '" + tableName + "'.");
								continue;
							}
							ITable memTable = memServer.getTable(tableName);
							if (memTable == null)
							{
								memTable = memServer.createNewTable(ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(), table,
									tableName);
								memServer.syncTableObjWithDB(memTable, false, true, null);
							}
							else if (override && dataModelManager != null)
							{
								((MemTable)memTable).setColumns(dataModelManager.serializeTable(table));
							}
							else continue;

							objectsToSave.add(solutionCopy.getOrCreateTableNode(dataSourceName));

						}
					}
					eclipseRepository = (EclipseRepository)ServoyModel.getDeveloperRepository();
					eclipseRepository.loadForeignElementsIDs(loadForeignElementsIDs(solutionCopy));
					for (IPersist persist : objectsToSave)
					{
						checkParent(persist);
						SolutionSerializer.writePersist(persist, wfa, ServoyModel.getDeveloperRepository(), true, false, true);
						if (persist instanceof AbstractBase)
						{
							((AbstractBase)persist).setParent(solutionCopy);
						}
					}
				}
				catch (RepositoryException | SQLException e)
				{
					Debug.error(e);
				}
				finally
				{
					if (eclipseRepository != null) eclipseRepository.clearForeignElementsIds();
				}
			}
		};
		RunInWorkspaceJob job = new RunInWorkspaceJob("Save solution data", saveJob);
		job.setRule(ServoyModel.getWorkspace().getRoot());
		job.setUser(false);
		job.schedule();
	}

	/**
	 * Saves just the given form or in memory datasource into the developers workspace.
	 * This must be a solution created or altered form/in memory datasource.
	 *
	 * @param obj The formname, JSForm, datasource name or JSDataSource object to save.
	 */
	public void js_save(Object obj)
	{
		js_save(obj, false);
	}

	/**
	 * Saves just the given form or in memory datasource into the developers workspace.
	 * This must be a solution created or altered form/in memory datasource.
	 *
	 * @param obj The formname, JSForm, datasource name or JSDataSource object to save.
	 * @param override Override an existing in memory table.
	 */
	public void js_save(Object obj, final boolean override)
	{
		String name = null;
		if (obj instanceof String)
		{
			name = (String)obj;
		}
		else if (obj instanceof JSForm)
		{
			name = ((JSForm)obj).getName();
		}
		else if (obj instanceof JSDataSource)
		{
			name = ((JSDataSource)obj).getDatasource();
		}
		if (name != null)
		{
			final String objName = name;
			WorkspaceJob saveJob = new WorkspaceJob("Save solution data")
			{
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					final IFileAccess wfa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
					Solution solutionCopy = null;
					EclipseRepository eclipseRepository = null;
					try
					{
						AbstractBase saveObj = null;
						if (objName.startsWith(DataSourceUtils.INMEM_DATASOURCE_SCHEME_COLON))
						{
							ITable table = state.getFoundSetManager().getTable(objName);
							if (table == null) throw new IllegalArgumentException("Datasource is not a solution model created/altered datasource");

							ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(state.getSolutionName());
							solutionCopy = servoyProject.getEditingSolution();
							MemServer memServer = servoyProject.getMemServer();
							String tableName = DataSourceUtils.getDataSourceTableName(objName);
							ITable memTable = memServer.getTable(tableName);
							if (memTable == null)
							{
								memTable = memServer.createNewTable(ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(), table,
									tableName);
								memServer.syncTableObjWithDB(memTable, false, true, null);
							}
							else if (override)
							{
								DataModelManager dataModelManager = ServoyModelFinder.getServoyModel().getDataModelManager();
								if (dataModelManager != null)
								{
									((MemTable)memTable).setColumns(dataModelManager.serializeTable(table));
								}
								else
								{
									state.reportError("Internal error.", "Cannot save table " + tableName);
									return Status.CANCEL_STATUS;
								}
							}
							else
							{
								state.reportError("Cannot save", "The in memory table '" + tableName +
									"' already exists. Please delete it, or use the 'save' method with the 'override' flag set to true.");
								return Status.CANCEL_STATUS;
							}
							saveObj = solutionCopy.getOrCreateTableNode(objName);
						}
						else
						{
							solutionCopy = state.getFlattenedSolution().getSolutionCopy();
							saveObj = solutionCopy.getForm(objName);
							if (saveObj == null) throw new IllegalArgumentException("JSForm is not a solution model created/altered form");
						}

						checkParent(saveObj);

						eclipseRepository = (EclipseRepository)ServoyModel.getDeveloperRepository();
						eclipseRepository.loadForeignElementsIDs(loadForeignElementsIDs(saveObj));
						SolutionSerializer.writePersist(saveObj, wfa, ServoyModel.getDeveloperRepository(), true, false, true);

						saveObj.setParent(solutionCopy);
					}
					catch (RepositoryException | SQLException e)
					{
						Debug.error(e);
					}
					finally
					{
						if (eclipseRepository != null) eclipseRepository.clearForeignElementsIds();
					}
					return Status.OK_STATUS;
				}
			};
			saveJob.setUser(false);
			saveJob.setRule(ServoyModel.getWorkspace().getRoot());
			saveJob.schedule();
		}
	}

	/**
	 * @param persist
	 */
	private void checkParent(IPersist persist)
	{
		IPersist realPersist = ServoyModelFinder.getServoyModel().getActiveProject().getEditingSolution().getChild(persist.getUUID());
		if (realPersist == null)
		{
			// the changed form could be in a module.
			Solution[] modules = ServoyModelFinder.getServoyModel().getActiveProject().getModules();
			for (Solution module : modules)
			{
				realPersist = module.getChild(persist.getUUID());
				if (realPersist instanceof AbstractBase)
				{
					// it is found in a module, now rebase the form to that parent so that it is saved in the right location
					((AbstractBase)persist).setParent(realPersist.getParent());
					break;
				}
			}
		}
	}

	/**
	 * Opens the form FormEditor in the developer.
	 *
	 * @param form The form name or JSForm object to open in an editor.
	 */
	public void js_openForm(Object form)
	{
		String name = null;
		if (form instanceof String)
		{
			name = (String)form;
		}
		else if (form instanceof JSForm)
		{
			name = ((JSForm)form).getName();
		}
		else if (form instanceof IForm)
		{
			name = ((IForm)form).getName();
		}
		if (name != null)
		{
			final Form frm = ServoyModelFinder.getServoyModel().getFlattenedSolution().getForm(name);
			if (frm != null)
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						try
						{
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(
								PersistEditorInput.createFormEditorInput(frm).setNew(false),
								PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
									Platform.getContentTypeManager().getContentType(PersistEditorInput.FORM_RESOURCE_ID)).getId());
						}
						catch (PartInitException ex)
						{
							ServoyLog.logError(ex);
						}
					}
				});
			}
			else
			{
				throw new IllegalArgumentException("form " + name + " is not a workspace stored (blueprint) form");
			}
		}
	}

	private Map<UUID, Integer> loadForeignElementsIDs(final IPersist rootObject)
	{
		rootObject.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				foreignElementUUIDs.put(o.getUUID(), new Integer(o.getID()));
				Map<UUID, Integer> map = ((AbstractBase)o).getSerializableRuntimeProperty(AbstractBase.UUIDToIDMapProperty);
				if (map != null)
				{
					Iterator<Map.Entry<UUID, Integer>> it = map.entrySet().iterator();
					while (it.hasNext())
					{
						Map.Entry<UUID, Integer> entry = it.next();
						foreignElementUUIDs.put(entry.getKey(), entry.getValue());
					}
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});
		return foreignElementUUIDs;
	}
}
