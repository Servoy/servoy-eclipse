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

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.mozilla.javascript.Function;

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
import com.servoy.j2db.dataprocessing.BufferedDataSetInternal;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.datasource.JSDataSource;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.DummyValidator;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.scripting.solutionmodel.JSMedia;
import com.servoy.j2db.scripting.solutionmodel.JSRelation;
import com.servoy.j2db.scripting.solutionmodel.JSValueList;
import com.servoy.j2db.scripting.solutionmodel.developer.IJSDeveloperSolutionModel;
import com.servoy.j2db.scripting.solutionmodel.developer.JSDeveloperMenu;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Class that is a special interface in javascript only there in the developer that bridges between the runtime client and the developers workspace
 *
 * @author jcompagner
 * @since 6.0
 */
public class JSDeveloperSolutionModel implements IJSDeveloperSolutionModel
{

	private final IDebugClient state;
	private final Map<UUID, Integer> foreignElementUUIDs = new HashMap<UUID, Integer>();

	public JSDeveloperSolutionModel(IDebugClient state)
	{
		this.state = state;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.core.IJSDeveloperSolutionModel#js_save()
	 */
	@Override
	public void save()
	{
		save(false);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.core.IJSDeveloperSolutionModel#js_save(boolean)
	 */
	@Override
	public void save(final boolean override)
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
								memServer.syncTableObjWithDB(memTable, false, true);
							}
							else if (override && dataModelManager != null)
							{
								((MemTable)memTable).setColumns(dataModelManager.serializeTable(table));
							}
							else continue;

							objectsToSave.add(servoyProject.getEditingSolution().getOrCreateTableNode(dataSourceName));

						}
					}
					Collection<String> solutionModelScopes = solutionCopy.getScopeNames();
					List<IPersist> clones = new ArrayList<IPersist>();
					if (solutionModelScopes != null)
					{
						// make sure sm solution has all variables and methods
						Solution mainSolution = state.getFlattenedSolution().getSolution();
						for (String scopeName : solutionModelScopes)
						{
							Iterator<ScriptMethod> it = mainSolution.getScriptMethods(scopeName, false);
							while (it.hasNext())
							{
								ScriptMethod sm = it.next();
								clones.add(sm.clonePersist(solutionCopy));
							}
							Iterator<ScriptVariable> it2 = mainSolution.getScriptVariables(scopeName, false);
							while (it2.hasNext())
							{
								ScriptVariable sv = it2.next();
								clones.add(sv.clonePersist(solutionCopy));
							}
						}
					}
					eclipseRepository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
					eclipseRepository.loadForeignElementsIDs(loadForeignElementsIDs(solutionCopy));
					List<String> scriptPaths = new ArrayList<String>();
					for (IPersist persist : objectsToSave)
					{
						checkParent(persist);
						if (persist instanceof IScriptElement && persist.getParent() == solutionCopy)
						{
							String path = SolutionSerializer.getScriptPath(persist, false);
							if (!scriptPaths.contains(path))
							{
								scriptPaths.add(path);
							}
						}
						else
						{
							SolutionSerializer.writePersist(persist, wfa, ApplicationServerRegistry.get().getDeveloperRepository(), true, false, true);
						}
						if (persist instanceof AbstractBase)
						{
							((AbstractBase)persist).setParent(solutionCopy);
						}
					}
					for (String scriptPath : scriptPaths)
					{
						IFileAccess fileAccess = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
						String content = SolutionSerializer.generateScriptFile(solutionCopy, scriptPath, eclipseRepository, null);
						OutputStream fos = null;
						try
						{
							fos = fileAccess.getOutputStream(scriptPath);
							fos.write(content.getBytes("UTF8"));
						}
						catch (IOException e)
						{
							ServoyLog.logError(e);
						}
						finally
						{
							Utils.closeOutputStream(fos);
						}

					}
					for (IPersist clone : clones)
					{
						solutionCopy.removeChild(clone);
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

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.core.IJSDeveloperSolutionModel#js_save(java.lang.Object)
	 */
	@Override
	public void save(Object obj)
	{
		save(obj, false);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.core.IJSDeveloperSolutionModel#js_save(java.lang.Object, boolean)
	 */
	@Override
	public void save(Object obj, final boolean override)
	{
		save(obj, null, override, null, null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.core.IJSDeveloperSolutionModel#js_save(java.lang.Object, java.lang.String)
	 */
	@Override
	public void save(Object obj, String solutionName)
	{
		save(obj, solutionName, false, null, null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.core.IJSDeveloperSolutionModel#js_updateInMemDataSource(java.lang.Object, com.servoy.j2db.dataprocessing.JSDataSet,
	 * java.lang.Object)
	 */
	@Override
	public void updateInMemDataSource(Object dataSource, JSDataSet dataSet, Object types)
	{
		save(dataSource, null, true, dataSet, types);
	}

	private void save(Object obj, String solutionName, final boolean override, JSDataSet updateDataSet, Object updateDataSetTypes)
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
		else if (obj instanceof JSValueList)
		{
			name = ((JSValueList)obj).getName();
		}
		else if (obj instanceof JSRelation)
		{
			name = ((JSRelation)obj).getName();
		}
		else if (obj instanceof JSMedia)
		{
			name = ((JSMedia)obj).getName();
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
					EclipseRepository eclipseRepository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
					try
					{
						AbstractBase saveObj = null;
						if (objName.startsWith(DataSourceUtils.INMEM_DATASOURCE_SCHEME_COLON))
						{
							ITable table = state.getFoundSetManager().getTable(objName);
							if (table == null) throw new IllegalArgumentException("Datasource is not a solution model created/altered datasource");

							String tableName = DataSourceUtils.getDataSourceTableName(objName);
							ServoyProject servoyProject = searchTable(tableName);
							if (solutionName != null && servoyProject != null && !solutionName.equals(servoyProject.getProject().getName()))
							{
								throw new IllegalArgumentException("Solution name should not be specified for existing tables. Table '" + tableName +
									"' already exists in solution '" + servoyProject.getProject().getName() + "'");
							}

							if (servoyProject == null)
							{
								servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(
									solutionName != null ? solutionName : state.getSolutionName());
							}
							solutionCopy = servoyProject.getEditingSolution();
							MemServer memServer = servoyProject.getMemServer();

							ITable memTable = memServer.getTable(tableName);
							if (memTable == null)
							{
								memTable = memServer.createNewTable(ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(), table,
									tableName);
								memServer.syncTableObjWithDB(memTable, false, true);
							}
							else if (override)
							{
								DataModelManager dataModelManager = ServoyModelFinder.getServoyModel().getDataModelManager();
								if (dataModelManager != null)
								{
									if (updateDataSet != null)
									{
										ColumnType[] columnTypes = null;
										if (updateDataSetTypes instanceof Object[])
										{
											columnTypes = new ColumnType[((Object[])updateDataSetTypes).length];
											for (int i = 0; i < ((Object[])updateDataSetTypes).length; i++)
											{
												columnTypes[i] = ColumnType.getColumnType(Utils.getAsInteger(((Object[])updateDataSetTypes)[i]));
											}
										}
										if (columnTypes == null) columnTypes = BufferedDataSetInternal.getColumnTypeInfo(updateDataSet.getDataSet());

										if (columnTypes.length == updateDataSet.getDataSet().getColumnCount())
										{
											List<String> tableColumnNames = Arrays.asList(memTable.getColumnNames());
											List<String> updateColumnNames = Arrays.asList(updateDataSet.getDataSet().getColumnNames());

											for (String columnName : tableColumnNames)
											{
												if (updateColumnNames.indexOf(columnName) == -1)
												{
													memTable.removeColumn(memTable.getColumn(columnName));
												}
											}
											tableColumnNames = Arrays.asList(memTable.getColumnNames());
											for (int i = 0; i < updateColumnNames.size(); i++)
											{
												String columnName = updateColumnNames.get(i);
												if (tableColumnNames.indexOf(columnName) == -1)
												{
													memTable.createNewColumn(DummyValidator.INSTANCE, columnName, columnTypes[i]);
												}
												else if (memTable.getColumn(columnName).getColumnType().getSqlType() != columnTypes[i].getSqlType())
												{
													state.reportJSWarning("In memory table '" + memTable.getDataSource() + "' column '" + columnName +
														"' has SQL type " + memTable.getColumn(columnName).getColumnType().getSqlType() +
														" while in the update it has " + columnTypes[i].getSqlType());
												}
											}
										}
										else
										{
											state.reportJSError("Can't update in memory table '" + memTable.getDataSource() +
												"' as the number of column types does not match the number of columns of the dataset", null);
										}
										memServer.syncTableObjWithDB(memTable, false, false);
									}
									else
									{
										((MemTable)memTable).setColumns(dataModelManager.serializeTable(table));
									}
								}
							}
							else
							{
								state.reportJSError(
									"The in memory table '" + tableName + "' already exists. Please delete it, or use the 'updateInMemDataSource' method.",
									null);
								return Status.CANCEL_STATUS;
							}
							saveObj = solutionCopy.getOrCreateTableNode(objName);
						}
						else
						{
							solutionCopy = state.getFlattenedSolution().getSolutionCopy();
							if (obj instanceof JSForm || obj instanceof String)
							{
								saveObj = solutionCopy.getForm(objName);
							}
							else if (obj instanceof JSValueList)
							{
								saveObj = solutionCopy.getValueList(objName);
							}
							else if (obj instanceof JSRelation)
							{
								saveObj = solutionCopy.getRelation(objName);
							}
							else if (obj instanceof JSMedia)
							{
								saveObj = solutionCopy.getMedia(objName);
							}
							if (saveObj == null) throw new IllegalArgumentException("The object " + objName + " is not solution model created/altered.");

							if (solutionName != null)
							{
								Solution solution = searchPersist((ISupportName)saveObj);
								if (solution == null)
								{
									solution = (Solution)eclipseRepository.getActiveRootObject(solutionName, IRepository.SOLUTIONS);
								}
								else if (!solutionName.equals(solution.getName()))
								{
									throw new IllegalArgumentException("Solution name should not be specified for existing persists. The object '" + objName +
										"' already exists in solution '" + solution.getName() + "'");
								}
								solutionCopy = eclipseRepository.createSolutionCopy(solution);
							}
						}

						saveObj.setParent(solutionCopy);
						checkParent(saveObj);

						eclipseRepository.loadForeignElementsIDs(loadForeignElementsIDs(saveObj));
						SolutionSerializer.writePersist(saveObj, wfa, ApplicationServerRegistry.get().getDeveloperRepository(), !(saveObj instanceof Media),
							false, true);
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

				private Solution searchPersist(ISupportName saveObj)
				{
					ServoyProject[] projects = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
					for (ServoyProject servoyProject : projects)
					{
						Solution solution = servoyProject.getSolution();
						if (saveObj instanceof Form && solution.getForm(saveObj.getName()) != null ||
							saveObj instanceof ValueList && solution.getValueList(saveObj.getName()) != null ||
							saveObj instanceof Relation && solution.getRelation(saveObj.getName()) != null)
						{
							return solution;
						}
					}
					return null;
				}

				private ServoyProject searchTable(String tableName) throws RepositoryException
				{
					ServoyProject[] projects = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
					for (ServoyProject servoyProject : projects)
					{
						if (servoyProject.getMemServer().getTable(tableName) != null)
						{
							return servoyProject;
						}
					}
					return null;
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

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.core.IJSDeveloperSolutionModel#js_openForm(java.lang.Object)
	 */
	@Override
	public void openForm(Object form)
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
					for (Entry<UUID, Integer> entry : map.entrySet())
					{
						foreignElementUUIDs.put(entry.getKey(), entry.getValue());
					}
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});
		return foreignElementUUIDs;
	}

	@Override
	public void showForm(String formName)
	{
		System.err.println("Showing a this form: " + formName);
	}

	@Override
	public JSDeveloperMenu createMenu(String text)
	{
		return null;
	}

	@Override
	public void registerMenuItem(JSDeveloperMenu menu, int location, Function callback)
	{
	}

	@Override
	public void registerMenuItem(JSDeveloperMenu menu, int location, Function callback, Function enabler)
	{
	}
}
