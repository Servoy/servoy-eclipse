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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IBuildpathEntry;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.javascript.ast.Comment;
import org.eclipse.dltk.javascript.ast.Expression;
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.Statement;
import org.eclipse.dltk.javascript.ast.VariableDeclaration;
import org.eclipse.dltk.javascript.ast.VariableStatement;
import org.eclipse.dltk.javascript.ast.VoidExpression;
import org.eclipse.dltk.javascript.parser.JavaScriptParserUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.UIJob;
import org.jshybugger.instrumentation.DebugInstrumentator;
import org.jshybugger.instrumentation.JsCodeLoader;
import org.jshybugger.proxy.DebugWebAppService;
import org.jshybugger.proxy.ScriptSourceProvider;
import org.json.JSONObject;
import org.mozilla.javascript.ast.AstRoot;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;

import sj.jsonschemavalidation.builder.JsonSchemaValidationNature;

import com.servoy.eclipse.core.quickfix.ChangeResourcesProjectQuickFix.ResourcesProjectSetupJob;
import com.servoy.eclipse.core.repository.EclipseUserManager;
import com.servoy.eclipse.core.repository.SwitchableEclipseUserManager;
import com.servoy.eclipse.core.util.ReturnValueRunnable;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.extensions.AbstractServoyModel;
import com.servoy.eclipse.model.mobile.exporter.MobileExporter;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.preferences.JSDocScriptTemplates;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.EclipseRepositoryFactory;
import com.servoy.eclipse.model.repository.EclipseSequenceProvider;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.repository.StringResourceDeserializer;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.util.AtomicIntegerWithListener;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.IWorkingSetChangedListener;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ResourcesUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.debug.DebugClientHandler;
import com.servoy.j2db.debug.DebugWebClientSession;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnInfoBasedSequenceProvider;
import com.servoy.j2db.persistence.IColumnListener;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistChangeListener;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISequenceProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerConfigListener;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerListener;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportScope;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ITableListener;
import com.servoy.j2db.persistence.ITableListener.TableListener;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.IVariable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StringResource;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.scripting.ScriptEngine;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.server.shared.IUserManagerFactory;
import com.servoy.j2db.server.shared.IWebClientSessionFactory;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * models servoy as JavaModel in eclipse
 *
 * @author jblok
 */
public class ServoyModel extends AbstractServoyModel
{
	/**
	 *
	 */
	public static final String SERVOY_WORKING_SET_ID = "com.servoy.eclipse.core.ServoyWorkingSet";

	private static final String SERVOY_ACTIVE_PROJECT = "SERVOY_ACTIVE_PROJECT";

	private final AtomicBoolean activatingProject = new AtomicBoolean(false);
	private final List<IActiveProjectListener> activeProjectListeners;
	private final List<IPersistChangeListener> realPersistChangeListeners;
	private final List<IPersistChangeListener> editingPersistChangeListeners;
	private final List<ISolutionMetaDataChangeListener> solutionMetaDataChangeListener;
	private final List<I18NChangeListener> i18nChangeListeners;

	private final Job fireRealPersistchangesJob;
	private List<IPersist> realOutstandingChanges;

	private TeamShareMonitor teamShareMonitor;

	private final List<IResourceDelta> outstandingChangedFiles = new ArrayList<IResourceDelta>();

	private final BackgroundTableLoader backgroundTableLoader;

	private final IServerConfigListener serverConfigSyncer;

	private IValidateName nameValidator;

	private IResourceChangeListener postChangeListener;

	private IResourceChangeListener preChangeListener;

	private IServerListener serverTableListener;
	private ITableListener tableListener;
	private IColumnListener columnListener;

	private final Boolean initRepAsTeamProvider;
	private IPropertyChangeListener workingSetChangeListener;
	private final List<IWorkingSetChangedListener> workingSetChangedListeners = new ArrayList<IWorkingSetChangedListener>();

	protected ServoyModel()
	{
		// hopefully by doing this before problems view has any stored state will allow us to limit visible markers to active solutions;
		// unfortunately there isn't currently a possibility to limit the scope of a filter to a workingSet via extension point - only the user can do it
		PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.USE_WINDOW_WORKING_SET_BY_DEFAULT, true);

		activeProjectListeners = new ArrayList<IActiveProjectListener>();
		realPersistChangeListeners = new ArrayList<IPersistChangeListener>();
		editingPersistChangeListeners = new ArrayList<IPersistChangeListener>();
		solutionMetaDataChangeListener = new ArrayList<ISolutionMetaDataChangeListener>();
		i18nChangeListeners = new ArrayList<I18NChangeListener>();
		fireRealPersistchangesJob = createFireRealPersistchangesJob();
		realOutstandingChanges = new ArrayList<IPersist>();

		startAppServer();

		// the in-process repository is only meant to work by itself - so all servoy related projects in the workspace should
		// either not be attached to team or attached to the in-process repository (because database information
		// and sequence provider are the standard table based ones - using the in-process repository - not the resources project)
		Settings settings = getSettings();
		Preferences pluginPreferences = Activator.getDefault().getPluginPreferences();
		pluginPreferences.setDefault(TeamShareMonitor.WARN_ON_NON_IN_PROCESS_TEAM_SHARE, true);
		initRepAsTeamProvider = Boolean.valueOf(Utils.getAsBoolean(settings.getProperty(Settings.START_AS_TEAMPROVIDER_SETTING,
			String.valueOf(Settings.START_AS_TEAMPROVIDER_DEFAULT))));

		// load in background all servers and all tables needed by current solution
		backgroundTableLoader = new BackgroundTableLoader(getServerManager());
		addActiveProjectListener(backgroundTableLoader);
		backgroundTableLoader.startLoadingOfServers();

		// when server configurations change we want to update the servers in Serclipse.
		getServerManager().addServerConfigListener(serverConfigSyncer = new DeveloperServerConfigSyncer(getServerManager()));

		// project update listener
		addActiveProjectListener(new IActiveProjectListener()
		{
			public void activeProjectChanged(ServoyProject aProject)
			{
				if (aProject != null)
				{
					Solution solution = aProject.getSolution();
					ServoyResourcesProject resourceProject = aProject.getResourcesProject();
					if (solution != null && (solution.getI18nDataSource() != null || aProject.getModules().length > 1) && resourceProject != null &&
						resourceProject.getProject().findMember(EclipseMessages.MESSAGES_DIR) == null) EclipseMessages.writeProjectI18NFiles(aProject, false,
						false);

					boolean isMobile = solution.getSolutionType() == SolutionMetaData.MOBILE;
					if (!isMobile)
					{
						Solution[] modules = aProject.getModules();
						for (Solution module : modules)
						{
							if (module.getSolutionType() == SolutionMetaData.MOBILE_MODULE)
							{
								isMobile = true;
								break;
							}
						}
					}
					if (isMobile)
					{
						try
						{
							DebugWebAppService debugWebAppService = DebugWebAppService.startDebugWebAppService(8889, new ScriptSourceProvider()
							{
								@Override
								public String loadScriptResourceById(String scriptUri, boolean encode) throws IOException
								{
									int index = scriptUri.indexOf("//");
									index = scriptUri.indexOf('/', index + 2);
									return new WorkspaceFileAccess(ResourcesPlugin.getWorkspace()).getUTF8Contents(scriptUri.substring(index + 1));
								}

								public String setScriptSource(String scriptUri, String scriptSource)
								{
									try
									{
										int index = scriptUri.indexOf("//");
										index = scriptUri.indexOf('/', index + 2);
										String workspaceFile = scriptUri.substring(index + 1);
										new WorkspaceFileAccess(ResourcesPlugin.getWorkspace()).setUTF8Contents(workspaceFile, scriptSource);
										Path path = new Path(workspaceFile);
										ServoyProject servoyProject = getServoyProject(path.segment(0));
										if (servoyProject != null && servoyProject.getProject().isOpen())
										{
											String scopeKind = "forms";
											String scopeName = null;
											Solution sol = servoyProject.getSolution();
											Iterator<ScriptMethod> scriptMethods = null;
											if (path.segmentCount() == 2)
											{
												scopeKind = "scopes";
												// globals/scopes
												scopeName = path.segment(1);
												if (scopeName.endsWith(SolutionSerializer.JS_FILE_EXTENSION))
												{
													scopeName = scopeName.substring(0, scopeName.length() - SolutionSerializer.JS_FILE_EXTENSION.length());
												}
												scriptMethods = sol.getScriptMethods(scopeName, false);
											}
											else if (path.segmentCount() == 3 && path.segment(1).equals(SolutionSerializer.FORMS_DIR))
											{
												// forms
												scopeName = path.segment(2);
												if (scopeName.endsWith(SolutionSerializer.JS_FILE_EXTENSION))
												{
													scopeName = scopeName.substring(0, scopeName.length() - SolutionSerializer.JS_FILE_EXTENSION.length());
												}
												scriptMethods = sol.getForm(scopeName).getScriptMethods(false);

											}
											StringBuilder sb = new StringBuilder();
											while (scriptMethods.hasNext())
											{
												ScriptMethod sm = scriptMethods.next();
												sb.append("_ServoyInit_.");
												sb.append(scopeKind);
												sb.append(".");
												sb.append(scopeName);
												sb.append("._sv_pushedfncs['");
												sb.append(sm.getName());
												sb.append("']=");
												sb.append(parseScriptMethod(sm));
												sb.append(";\n");
											}
											if (scopeKind.equals("forms"))
											{
												sb.append("_ServoyUtils_.reloadFormScope('");
												sb.append(scopeName);
												sb.append("')");
											}
											else
											{
												sb.append("_ServoyUtils_.reloadGlobalScope('");
												sb.append(scopeName);
												sb.append("')");
											}
											return sb.toString();
										}
										return null;
									}
									catch (IOException e)
									{
										ServoyLog.logError("error saving chagnes from debugger", e);
									}
									return null;
								}

								private String parseScriptMethod(IScriptProvider method)
								{
									try
									{
										String code = method.getDeclaration();
										String scriptPath = SolutionSerializer.getScriptPath(method, false);
										code = ScriptEngine.extractFunction(code, "function $1");
										byte[] bytes = code.getBytes(Charset.forName("UTF8"));
										ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
										ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length * 2);
										DebugInstrumentator instrumenator = new DebugInstrumentator()
										{
											@Override
											protected void loadFile(AstRoot node)
											{
											}
										};
										JsCodeLoader.instrumentFile(scriptPath, bais, baos, new HashMap<String, Object>(), method.getLineNumberOffset() - 1,
											instrumenator, false);
										code = new String(baos.toByteArray(), Charset.forName("UTF8"));
										return JSONObject.quote(ScriptEngine.extractFunction(code, ""));
									}
									catch (Exception e)
									{
										e.printStackTrace();
									}
									return null;
								}
							});

							debugWebAppService.addHandler("/solution.js", new HttpHandler()
							{
								@Override
								public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl arg2) throws Exception
								{
									MobileExporter exporter = getMobileExporter(request);

									String solutionJs = exporter.doScriptingExport();
									response.header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
									response.content(solutionJs);
									response.end();
								}
							});
							debugWebAppService.addHandler("/solution_json.js", new HttpHandler()
							{
								@Override
								public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl arg2) throws Exception
								{
									MobileExporter exporter = getMobileExporter(request);

									String persist_json = exporter.doPersistExport();
									response.header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
									response.content(persist_json);
									response.end();
								}
							});

						}
						catch (Exception e)
						{
							ServoyLog.logError("Couldn't start the mobile debug service", e);
						}
					}

				}
			}

			public void activeProjectUpdated(final ServoyProject activeProject, int updateInfo)
			{
				if (updateInfo == IActiveProjectListener.MODULES_UPDATED)
				{
					String[] moduleNames = Utils.getTokenElements(activeProject.getSolution().getModulesNames(), ",", true);
					final ArrayList<ServoyProject> modulesToUpdate = new ArrayList<ServoyProject>();
					final StringBuilder sbUpdateModuleNames = new StringBuilder();

					try
					{
						for (String moduleName : moduleNames)
						{
							IProject moduleProject = ServoyModel.getWorkspace().getRoot().getProject(moduleName);
							if (moduleProject != null && moduleProject.isOpen() && ServoyUpdatingProject.needUpdate(moduleProject))
							{
								ServoyProject sp = (ServoyProject)moduleProject.getNature(ServoyProject.NATURE_ID);
								modulesToUpdate.add(sp);
								if (sbUpdateModuleNames.length() > 0) sbUpdateModuleNames.append(", ");
								sbUpdateModuleNames.append(sp.getSolution().getName());
							}
						}

						if (modulesToUpdate.size() > 0)
						{
							Display.getDefault().asyncExec(new Runnable()
							{
								public void run()
								{
									if (MessageDialog.openQuestion(UIUtils.getActiveShell(), "Project update",
										"Before adding as module(s), the following project(s) needs to be updated : " + sbUpdateModuleNames.toString() +
											"\n Do you want to start the update ?\n\nNOTE: If you don't update, they won't be added as module to the solution!"))
									{
										// do update in thread
										try
										{
											PlatformUI.getWorkbench().getProgressService().run(true, false, new IRunnableWithProgress()
											{
												public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
												{
													for (ServoyProject module : modulesToUpdate)
														doUpdate(module, monitor);
													monitor.done();
												}
											});
										}
										catch (Exception ex)
										{
											// cannot update module
											ServoyLog.logError(ex);
											MessageDialog.openError(UIUtils.getActiveShell(), "Project update", ex.toString());
										}
									}
									else
									// remove not updated projects
									{
										Solution editingSolution = activeProject.getEditingSolution();
										if (editingSolution != null)
										{
											String[] modules = Utils.getTokenElements(editingSolution.getModulesNames(), ",", true);
											List<String> modulesList = new ArrayList<String>(Arrays.asList(modules));
											for (ServoyProject updateModule : modulesToUpdate)
												modulesList.remove(updateModule.getSolution().getName());
											String modulesTokenized = ModelUtils.getTokenValue(modulesList.toArray(new String[] { }), ",");
											editingSolution.setModulesNames(modulesTokenized);
											try
											{
												activeProject.saveEditingSolutionNodes(new IPersist[] { editingSolution }, false);
											}
											catch (RepositoryException e)
											{
												ServoyLog.logError("Cannot restore module list for solution " + activeProject.getProject().getName(), e);
											}
										}
									}
								}
							});
						}

						EclipseMessages.writeProjectI18NFiles(activeProject, false, false);
						updateWorkingSet();
						testBuildPathsAndBuild(activeProject, false);
						buildProjects(Arrays.asList(getServoyProjects()));
						testBuildPathsAndBuild(activeProject, false);
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
				}
				else if (updateInfo == IActiveProjectListener.RESOURCES_UPDATED_ON_ACTIVE_PROJECT)
				{
					updateWorkingSet();
					try
					{
						EclipseMessages.writeProjectI18NFiles(activeProject, false, false);
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
				}
			}

			public boolean activeProjectWillChange(ServoyProject activeProject, final ServoyProject toProject)
			{
				// if it has no resource project, let the user choose one
				if (toProject != null && toProject.getResourcesProject() == null && !UIUtils.showChangeResourceProjectDlg(UIUtils.getActiveShell(), toProject)) return false;
				try
				{
					if (toProject != null && ServoyUpdatingProject.needUpdate(toProject.getProject()))
					{

						if (!toProject.getProject().hasNature(ServoyUpdatingProject.NATURE_ID) &&
							MessageDialog.openQuestion(
								UIUtils.getActiveShell(),
								"Project update",
								"Before activating solution '" +
									toProject.getSolution().getName() +
									"', its project structure needs to be updated.\n\nNOTE: If the solution or one of its modules is shared using a team provider, it is recommended that this update is performed by one developer of the team, and the others cancel this update, and do a team update from 'Solution Explorer/All Solutions' for this solution and its modules.\n\nDo you want to update this solution and its modules projects ?"))
						{
							// do update in thread and set as active in the end
							WorkspaceJob updateJob = new WorkspaceJob("Updating project structure ...")
							{
								@Override
								public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
								{
									if (doUpdate(toProject, monitor))
									{
										try
										{
											EclipseMessages.writeProjectI18NFiles(toProject, false, false);
											ServoyModelManager.getServoyModelManager().getServoyModel().setActiveProject(toProject, true);
										}
										catch (Exception ex)
										{
											ServoyLog.logError(ex);
											Display.getDefault().syncExec(new Runnable()
											{
												public void run()
												{
													MessageDialog.openError(UIUtils.getActiveShell(), "Project update", "Error updating solution '" +
														toProject.getSolution().getName() + "'\nTry to activate it again.");
												}
											});
										}
									}
									monitor.done();
									return Status.OK_STATUS;
								}
							};
							updateJob.setUser(true);
							updateJob.schedule();
						}
						return false;
					}

					return true;
				}
				catch (Exception ex)
				{
					// cannot check if project needs update
					ServoyLog.logError(ex);
					MessageDialog.openError(UIUtils.getActiveShell(), "Project update", ex.toString());
					return false;
				}
			}

			private void getUpdatingProjects(ArrayList<IProject> updatingProjects, IProject project) throws CoreException
			{
				if (project != null && updatingProjects.indexOf(project) == -1 && project.isOpen() && ServoyUpdatingProject.needUpdate(project))
				{
					ServoyProject servoyProject = (ServoyProject)project.getNature(ServoyProject.NATURE_ID);
					Solution solution = servoyProject.getSolution();
					String[] modules = Utils.getTokenElements(solution.getModulesNames(), ",", true);
					updatingProjects.add(project);

					for (String moduleName : modules)
						getUpdatingProjects(updatingProjects, ServoyModel.getWorkspace().getRoot().getProject(moduleName));
				}
			}

			private boolean doUpdate(final ServoyProject project, IProgressMonitor progressMonitor)
			{
				try
				{
					ArrayList<IProject> updatingProjects = new ArrayList<IProject>();
					getUpdatingProjects(updatingProjects, project.getProject());
					progressMonitor.beginTask("Updating solution project structure ...", updatingProjects.size());

					for (IProject updatingProject : updatingProjects)
					{
						IProjectDescription projectDesc = updatingProject.getDescription();
						String[] projectNatures = projectDesc.getNatureIds();
						// mark the project as updating, by setting ServoyProject.UPDATING_NATURE_ID
						String[] updatingProjectNatures = new String[projectNatures.length + 1];
						System.arraycopy(projectNatures, 0, updatingProjectNatures, 0, projectNatures.length);
						updatingProjectNatures[updatingProjectNatures.length - 1] = ServoyUpdatingProject.NATURE_ID;
						projectDesc.setNatureIds(updatingProjectNatures);
						updatingProject.setDescription(projectDesc, null);
						progressMonitor.worked(1);
					}
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
					Display.getDefault().syncExec(new Runnable()
					{
						public void run()
						{
							MessageDialog.openError(UIUtils.getActiveShell(), "Project update", "Error updating solution '" + project.getSolution().getName() +
								"'\nTry to activate it again.");
						}
					});
					return false;
				}

				return true;
			}

			/**
			 * @param request
			 * @return
			 */
			private MobileExporter getMobileExporter(HttpRequest request)
			{
				MobileExporter exporter = new MobileExporter();
				exporter.setDebugMode(true);
				exporter.setServerURL(request.queryParam("u"));
				exporter.setSolutionName(request.queryParam("s"));
				exporter.setServiceSolutionName(request.queryParam("ss"));
				String timeout = request.queryParam("t");
				if (timeout != null)
				{
					exporter.setTimeout(Integer.parseInt(timeout));
				}
				String skipConnect = request.queryParam("sc");
				if (skipConnect != null)
				{
					exporter.setSkipConnect(Boolean.parseBoolean(skipConnect));
				}
				return exporter;
			}
		});
		workingSetChangeListener = new IPropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent event)
			{
				writeServoyWorkingSets(event);
			}
		};
		PlatformUI.getWorkbench().getWorkingSetManager().addPropertyChangeListener(workingSetChangeListener);
		installServerTableColumnListener();
	}

	/**
	 * Install listener for changes to tables, clear cached tables when tables change.
	 */
	private void installServerTableColumnListener()
	{
		columnListener = new IColumnListener()
		{

			@Override
			public void iColumnsChanged(Collection<IColumn> columns)
			{
				try
				{
					for (IColumn column : columns)
					{
						flushDataProvidersForTable(column.getTable());
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}

			@Override
			public void iColumnRemoved(IColumn column)
			{
				try
				{
					flushDataProvidersForTable(column.getTable());
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}

			@Override
			public void iColumnCreated(IColumn column)
			{
				try
				{
					flushDataProvidersForTable(column.getTable());
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
		};

		tableListener = new TableListener()
		{
			@Override
			public void tablesRemoved(IServerInternal server, Table[] tables, boolean deleted)
			{
				for (Table table : tables)
				{
					table.removeIColumnListener(columnListener);
					flushDataProvidersForTable(table);
				}
				String[] tableNames = new String[tables.length];
				for (int i = 0; i < tables.length; i++)
				{
					tableNames[i] = tables[i].getName();
				}
				clearCachedTables(tableNames);
			}

			@Override
			public void tablesAdded(IServerInternal server, String[] tableNames)
			{
				clearCachedTables(tableNames);
				try
				{
					for (String tableName : tableNames)
					{
						if (server.isTableLoaded(tableName))
						{
							(server.getTable(tableName)).addIColumnListener(columnListener);
						}
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}

			@Override
			public void tableInitialized(Table t)
			{
				t.addIColumnListener(columnListener);
				clearCachedTables(new String[] { t.getName() });
			}

			private void clearCachedTables(String[] tableNames)
			{
				final List<String> names = Arrays.asList(tableNames);
				IPersistVisitor visitor = new IPersistVisitor()
				{
					public Object visit(IPersist o)
					{
						if (o instanceof Form)
						{
							if (names.contains(((Form)o).getTableName()))
							{
								((Form)o).clearTable();
							}
						}

						else if (o instanceof TableNode)
						{
							if (names.contains(((TableNode)o).getTableName()))
							{
								((TableNode)o).clearTable();
							}
						}

						return o.getParent() instanceof Solution ? CONTINUE_TRAVERSAL : CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
					}
				};

				// flush flattened form caches and clear cached tables
				getFlattenedSolution().flushFlattenedFormCache();
				for (ServoyProject project : getModulesOfActiveProject())
				{
					project.getEditingFlattenedSolution().flushFlattenedFormCache();
					project.getSolution().acceptVisitor(visitor);
					project.getEditingSolution().acceptVisitor(visitor);
				}
			}
		};

		IServerManagerInternal serverManager = getServerManager();

		// add listeners to initial server list
		String[] array = serverManager.getServerNames(false, false, true, true);
		for (String server_name : array)
		{
			((IServerInternal)serverManager.getServer(server_name, false, false)).addTableListener(tableListener);
		}

		// monitor changes in server list
		serverManager.addServerListener(serverTableListener = new IServerListener()
		{
			public void serverAdded(IServerInternal s)
			{
				s.addTableListener(tableListener);
			}

			public void serverRemoved(IServerInternal s)
			{
				s.removeTableListener(tableListener);
				flushAllCachedData();
			}
		});
	}

	public TeamShareMonitor getTeamShareMonitor()
	{
		synchronized (initRepAsTeamProvider)
		{
			if (initRepAsTeamProvider.booleanValue() && teamShareMonitor == null)
			{
				teamShareMonitor = new TeamShareMonitor(this);
			}
		}
		return teamShareMonitor;
	}

	public static void startAppServer()
	{
		PreInitializeTaskHandler.runTasksIfNeeded();

		if (ApplicationServerRegistry.get() != null)
		{
			if (!ApplicationServerRegistry.waitForApplicationServerStarted())
			{
				ServoyLog.logError("App server didnt fully get started", new RuntimeException());
			}
			return;
		}
		try
		{
			Activator.getDefault().startAppServer(new EclipseRepositoryFactory(), new DebugClientHandler(), new IWebClientSessionFactory()
			{
				public Session newSession(Request request, Response response)
				{
					return new DebugWebClientSession(request);
				}
			}, new IUserManagerFactory()
			{
				public IUserManager createUserManager(IDataServer dataServer)
				{
					return new SwitchableEclipseUserManager();
				}
			});
			// set the START_AS_TEAMPROVIDER_SETTING flag as system property, so
			// our team plugin can use it in popupMenu enablement
			System.setProperty(Settings.START_AS_TEAMPROVIDER_SETTING,
				ServoyModel.getSettings().getProperty(Settings.START_AS_TEAMPROVIDER_SETTING, String.valueOf(Settings.START_AS_TEAMPROVIDER_DEFAULT)));
		}
		catch (Exception ex)
		{
			ServoyLog.logError("Failed to start the appserver", ex);
		}
	}

	/**
	 * Returns the user manager reflecting the state of the workspace. When running unit tests, the ApplicationServer.getUserManager() delegates to
	 * and alternate user manager, not to this one.
	 */
	public EclipseUserManager getUserManager()
	{
		startAppServer();
		return ((SwitchableEclipseUserManager)ApplicationServerRegistry.get().getUserManager()).getEclipseUserManager();
	}

	public static IServerManagerInternal getServerManager()
	{
		startAppServer();
		return ApplicationServerRegistry.get().getServerManager();
	}

	public static IDeveloperRepository getDeveloperRepository()
	{
		startAppServer();
		return ApplicationServerRegistry.get().getDeveloperRepository();
	}

	public static IDataServer getDataServer()
	{
		startAppServer();
		return ApplicationServerRegistry.get().getDataServer();
	}

	public static Settings getSettings()
	{
		startAppServer();
		return Settings.getInstance();
	}

	public static boolean isClientRepositoryAccessAllowed(String server_name)
	{
		return isClientRepositoryAccessAllowed() || !IServer.REPOSITORY_SERVER.equals(Utils.toEnglishLocaleLowerCase(server_name));
	}

	public static boolean isClientRepositoryAccessAllowed()
	{
		return Utils.getAsBoolean(getSettings().getProperty(Settings.ALLOW_CLIENT_REPOSITORY_ACCESS_SETTING,
			String.valueOf(Settings.ALLOW_CLIENT_REPOSITORY_ACCESS_DEFAULT)));
	}

	/**
	 * Returns the root object with the given name. string resources are read from the resources project referenced by the current active solution project.
	 *
	 * @param name the name of the object.
	 * @return the root object with the given name.
	 */
	public IRootObject getActiveRootObject(String name, int objectTypeId)
	{
		ServoyModelManager.getServoyModelManager().getServoyModel();
		IDeveloperRepository repository = ServoyModel.getDeveloperRepository();
		if (repository != null)
		{
			try
			{
				return repository.getActiveRootObject(name, objectTypeId);
				// TODO Problem markers for deserialize exception (in servoy builder)
			}
			catch (Exception e)
			{
				ServoyLog.logWarning("Cannot get root object with name " + name + " from " + activeResourcesProject, e);
			}
		}
		else
		{
			MessageDialog.openError(UIUtils.getActiveShell(), "Repository error", "Cannot find Servoy Eclipse repository.");
		}
		return null;
	}

	/**
	 * Returns the style with the given rootObjectId. Styles are read from the styles project referenced by the current active solution project.
	 *
	 * @param rootObjectId the rootObjectId of the style.
	 * @return the style with the given name.
	 */
	public IRootObject getActiveRootObject(int rootObjectId)
	{
		ServoyModelManager.getServoyModelManager().getServoyModel();
		IDeveloperRepository repository = ServoyModel.getDeveloperRepository();
		if (repository != null)
		{
			try
			{
				return repository.getActiveRootObject(rootObjectId);
				// TODO Problem markers for deserialize exception (in servoy builder)
			}
			catch (Exception e)
			{
				ServoyLog.logWarning("Cannot get style object with id " + rootObjectId + " from " + activeResourcesProject, e);
			}
		}
		else
		{
			MessageDialog.openError(UIUtils.getActiveShell(), "Repository error", "Cannot find Servoy Eclipse repository.");
		}
		return null;
	}

	/**
	 * Returns a list with all styles. Styles are read from the styles project referenced by the current active solution project.
	 *
	 * @return the style with the given name.
	 */
	public List<IRootObject> getActiveRootObjects(int type)
	{
		ServoyModelManager.getServoyModelManager().getServoyModel();
		IDeveloperRepository repository = ServoyModel.getDeveloperRepository();
		if (repository != null)
		{
			try
			{
				return repository.getActiveRootObjects(type);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logWarning("Cannot get style objects from " + activeResourcesProject, e);
			}
		}
		else
		{
			MessageDialog.openError(UIUtils.getActiveShell(), "Repository error", "Cannot find Servoy Eclipse repository.");
		}
		return new ArrayList<IRootObject>();
	}

	private void autoSelectActiveProjectIfNull(final boolean buildProject)
	{
		if (activeProject == null && !activatingProject.get())
		{
			ServoyProject autoSetActiveProject = null;
			Preferences pluginPreferences = Activator.getDefault().getPluginPreferences();
			autoSetActiveProject = getServoyProject(pluginPreferences.getString(SERVOY_ACTIVE_PROJECT));
//			if (autoSetActiveProject == null || autoSetActiveProject.getSolution() == null)
//			{
//				ServoyProject[] servoyProjects = getServoyProjects();
//				int i = 0;
//				while ((autoSetActiveProject == null || autoSetActiveProject.getSolution() == null) && i < servoyProjects.length)
//				{
//					autoSetActiveProject = servoyProjects[i++];
//				}
//			}
			if (autoSetActiveProject != null && autoSetActiveProject.getSolution() != null)
			{
				activatingProject.set(true);
				if (Display.getCurrent() != null)
				{
					setActiveProject(autoSetActiveProject, buildProject);
				}
				else
				{
					final ServoyProject toActivateProject = autoSetActiveProject;
					Display.getDefault().asyncExec(new Runnable()
					{

						public void run()
						{
							setActiveProject(toActivateProject, buildProject);
						}
					});
				}
			}
			else
			{
				pluginPreferences.setToDefault(SERVOY_ACTIVE_PROJECT);
				updateWorkingSet();
//				ServoyLog.logInfo("Cannot find any valid solution to activate."); // unneeded logging
			}
		}
	}

	public void setActiveProject(final ServoyProject project, final boolean buildProject)
	{
		// listeners must run in GUI thread to prevent 'Invalid thread access' exceptions;
		// the rest should run in a progress dialog that blocks the workbench but does not make the GUI
		// unresponsive (because lengthy operations such as reading column info / accessing DB server can happen)

		final IRunnableWithProgress op = new IRunnableWithProgress()
		{
			public void run(final IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException
			{
				try
				{
					if (project == null)
					{
						progressMonitor.beginTask("Processing solution deactivation", 7);
					}
					else
					{
						progressMonitor.beginTask("Activating solution \"" + project + "\"", 7);
					}

					progressMonitor.subTask("Loading solution...");
					if (project != null && project.getSolution() == null)
					{
						ServoyLog.logError(
							"Error activating solution. It is not properly initialized. Please check for problems in the underlying file representation.", null);

						Display.getDefault().syncExec(new Runnable()
						{
							public void run()
							{
								MessageDialog.openError(
									UIUtils.getActiveShell(),
									"Error activating solution",
									"Solution " +
										project +
										" cannot be activated. The Solution or some of its modules was created or updated by a newer version of Servoy. Either switch to a newer version of Servoy or rollback the changes made.");
							}
						});
						return;
					}
					else if (project != null && project.getSolutionMetaData().getFileVersion() > AbstractRepository.repository_version)
					{
						Display.getDefault().asyncExec(new Runnable()
						{
							public void run()
							{
								MessageDialog.openWarning(
									UIUtils.getActiveShell(),
									"Warning activating solution",
									"Solution " +
										project +
										" was activated, but with errors! The Solution or one of its modules was created or updated by a newer version of Servoy. You should either switch to a newer version of Servoy or rollback the changes made.");
							}
						});
					}

					progressMonitor.worked(1);
					progressMonitor.subTask("Announcing activation intent...");
					ReturnValueRunnable uiRunnable = new ReturnValueRunnable()
					{
						public void run()
						{
							returnValue = Boolean.valueOf(fireActiveProjectWillChange(project));
						}
					};
					Display.getDefault().syncExec(uiRunnable);
					if (((Boolean)uiRunnable.getReturnValue()).booleanValue())
					{
						progressMonitor.subTask("Closing active editors...");

						Display.getDefault().syncExec(new Runnable()
						{
							public void run()
							{
								IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
								for (IWorkbenchWindow workbenchWindow : workbenchWindows)
								{
									if (workbenchWindow.getActivePage() == null) continue;
									IEditorReference[] editorReferences = workbenchWindow.getActivePage().getEditorReferences();
									workbenchWindow.getActivePage().closeEditors(editorReferences, true);
								}
							}
						});

						progressMonitor.worked(1);

						nameValidator = null;
						if (project != null) // active project was deleted
						{
							resetActiveEditingFlattenedSolutions();
						}
						activeProject = project;
						activatingProject.set(false);

						progressMonitor.subTask("Loading modules...");
						EclipseRepository.ActivityMonitor moduleMonitor = new EclipseRepository.ActivityMonitor()
						{
							public void loadingRootObject(final RootObjectMetaData rootObject)
							{
								if (rootObject.getObjectTypeId() == IRepository.SOLUTIONS)
								{
									Display.getDefault().asyncExec(new Runnable()
									{
										public void run()
										{
											progressMonitor.subTask("Loading modules... Module '" + rootObject.getName() + "'");
										}
									});
								}
							}
						};
						IDeveloperRepository rep = getDeveloperRepository();
						if (rep instanceof EclipseRepository)
						{
							((EclipseRepository)rep).addActivityMonitor(moduleMonitor);
						}
						try
						{
							updateFlattenedSolution();
						}
						finally
						{
							if (rep instanceof EclipseRepository)
							{
								((EclipseRepository)rep).removeActivityMonitor(moduleMonitor);
							}
						}

						progressMonitor.worked(1);
						if (activeProject != null)
						{
							// prefetch the editting solution to minimize ui lags
							progressMonitor.subTask("Preparing solution for editing...");
							if (Display.getCurrent() != null)
							{
								// update the ui if we are the ui thread in this progress dialog
								while (Display.getCurrent().readAndDispatch())
								{
								}
							}
							activeProject.getEditingFlattenedSolution();
							if (Display.getCurrent() != null)
							{
								// update the ui if we are the ui thread in this progress dialog
								while (Display.getCurrent().readAndDispatch())
								{
								}
							}
						}

						Preferences pluginPreferences = Activator.getDefault().getPluginPreferences();
						if (project != null && pluginPreferences != null)
						{
							pluginPreferences.setValue(SERVOY_ACTIVE_PROJECT, project.getProject().getName());
							Activator.getDefault().savePluginPreferences();
						}
						progressMonitor.worked(1);

						updateResources(IActiveProjectListener.RESOURCES_UPDATED_BECAUSE_ACTIVE_PROJECT_CHANGED, new SubProgressMonitor(progressMonitor, 4)); // if the active solution changes, it is possible that the used resources project will change

						progressMonitor.subTask("Announcing activation...");
						Display.getDefault().syncExec(new Runnable()
						{
							public void run()
							{
								fireActiveProjectChanged();
							}
						});
						progressMonitor.worked(1);

						updateWorkingSet();
						testBuildPathsAndBuild(project, buildProject);

						progressMonitor.worked(1);
					}
				}
				finally
				{
					activatingProject.set(false);
					progressMonitor.done();
				}
			}
		};

		if (Display.getCurrent() != null)
		{
			try
			{
				PlatformUI.getWorkbench().getProgressService().run(true, false, op);
			}
			catch (InvocationTargetException e)
			{
				// Operation was canceled
				ServoyLog.logError(e);
			}
			catch (InterruptedException e)
			{
				// Handle the wrapped exception
				ServoyLog.logError(e);
			}
		}
		else
		{
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					try
					{
						PlatformUI.getWorkbench().getProgressService().run(true, false, op);
					}
					catch (InvocationTargetException e)
					{
						// Operation was canceled
						ServoyLog.logError(e);
					}
					catch (InterruptedException e)
					{
						// Handle the wrapped exception
						ServoyLog.logError(e);
					}
				}
			});
		}
	}

	public void buildActiveProjectsInJob()
	{
		if (activeProject != null && getWorkspace().isAutoBuilding())
		{
			Job buildJob = new Job("Building")
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					buildActiveProjects(monitor);
					return Status.OK_STATUS;
				}
			};
			buildJob.setRule(activeProject.getProject().getWorkspace().getRoot());
			buildJob.setSystem(false);
			buildJob.setUser(true);
			buildJob.schedule();
		}
	}

	public void buildProjects(final List<ServoyProject> projects)
	{
		if (projects != null && getWorkspace().isAutoBuilding())
		{
			Job buildJob = new Job("Building")
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					try
					{
						for (ServoyProject project : projects)
						{
							project.getProject().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
						}
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
					return Status.OK_STATUS;
				}
			};
			buildJob.setRule(activeProject.getProject().getWorkspace().getRoot());
			buildJob.schedule();
		}
	}

	// updates current styles and column info
	private void updateResources(final int type, IProgressMonitor progressMonitor)
	{
		final IRunnableWithProgress op = new IRunnableWithProgress()
		{
			public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException
			{
				try
				{
					progressMonitor.beginTask("Reading resources project", 5);
					ServoyResourcesProject old = activeResourcesProject;
					activeResourcesProject = null;
					if (activeProject != null)
					{
						activeResourcesProject = activeProject.getResourcesProject();
					}

					if (old != activeResourcesProject)
					{
						String projectName = null;
						IServerManagerInternal serverManager = getServerManager();
						ISequenceProvider sequenceProvider;
						DataModelManager oldDmm = dataModelManager;
						if (dataModelManager != null)
						{
							dataModelManager.dispose();
						}
						if (activeResourcesProject != null)
						{
							projectName = activeResourcesProject.getProject().getName();
							dataModelManager = new DataModelManager(activeResourcesProject.getProject(), serverManager);
							sequenceProvider = new EclipseSequenceProvider(dataModelManager);
							readWorkingSetsFromResourcesProject();
							activeResourcesProject.setListeners(workingSetChangedListeners);

							try
							{
								if (!activeResourcesProject.getProject().hasNature(JsonSchemaValidationNature.NATURE_ID))
								{
									WorkspaceJob updateJob = new WorkspaceJob("Updating project nature ...")
									{
										@Override
										public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
										{
											if (activeResourcesProject == null) return Status.OK_STATUS;
											
											IProjectDescription description = activeResourcesProject.getProject().getDescription();
											String[] natures = description.getNatureIds();
											String[] newNatures = new String[natures.length + 1];
											System.arraycopy(natures, 0, newNatures, 0, natures.length);
											newNatures[natures.length] = JsonSchemaValidationNature.NATURE_ID;
											description.setNatureIds(newNatures);
											activeResourcesProject.getProject().setDescription(description, null);
											monitor.done();
											return Status.OK_STATUS;
										}
									};
									updateJob.setUser(true);
									updateJob.setRule(getWorkspace().getRoot());
									updateJob.schedule();
								}
							}
							catch (CoreException e)
							{
								ServoyLog.logError(e);
							}
						}
						else
						{
							dataModelManager = null;
							sequenceProvider = null;
						}
						if (old != null)
						{
							old.destroy();
						}

						// refresh all tables - because the column info changed
						progressMonitor.subTask("Loading database column information from resources project");
						if (serverManager != null)
						{
							// we cannot allow background loading of servers/tables to continue
							// while the column info provider is being changed / column info is reloaded
							try
							{
								backgroundTableLoader.pause();
								serverManager.removeGlobalColumnInfoProvider(oldDmm);
								serverManager.addGlobalColumnInfoProvider(dataModelManager);

								// When running the in-process repository, we want to use the column info provider and the sequence
								// provider of the team repository; so in this case do not apply the eclipse sequence provider
								if (!(serverManager.getGlobalSequenceProvider() instanceof IColumnInfoBasedSequenceProvider))
								{
									serverManager.setGlobalSequenceProvider(sequenceProvider);
								}

								reloadAllColumnInfo(serverManager);
							}
							finally
							{
								backgroundTableLoader.resume();
							}
						}
						progressMonitor.worked(1);

						progressMonitor.subTask("Loading user/group/permission information");
						if (activeResourcesProject != null)
						{
							getUserManager().setResourcesProject(activeResourcesProject.getProject());
						}
						else
						{
							getUserManager().setResourcesProject(null);
						}
						progressMonitor.worked(1);

						progressMonitor.subTask("Loading styles from resources project");
						((EclipseRepository)getDeveloperRepository()).registerResourceMetaDatas(projectName, IRepository.STYLES);
						progressMonitor.worked(1);
						progressMonitor.subTask("Loading templates from resources project");
						((EclipseRepository)getDeveloperRepository()).registerResourceMetaDatas(projectName, IRepository.TEMPLATES);
						progressMonitor.worked(1);
						progressMonitor.subTask("Announcing resources project change");
						Display.getDefault().syncExec(new Runnable()
						{
							public void run()
							{
								fireActiveProjectUpdated(type);
							}
						});
						progressMonitor.worked(1);
					}
					else if (type == IActiveProjectListener.RESOURCES_UPDATED_BECAUSE_ACTIVE_PROJECT_CHANGED)
					{
						progressMonitor.subTask("Loading form security information");
						// this means that the active project changed, but the resources project remained the same
						// so we must reload the form security access info
						getUserManager().reloadAllFormInfo();
						progressMonitor.worked(4);
					}
				}
				finally
				{
					progressMonitor.done();
				}
			}
		};

		if (progressMonitor != null)
		{
			try
			{
				op.run(progressMonitor);
			}
			catch (InvocationTargetException e)
			{
				ServoyLog.logError(e);
			}
			catch (InterruptedException e)
			{
				ServoyLog.logError(e);
			}
		}
		else
		{
			if (Display.getCurrent() != null)
			{
				try
				{
					PlatformUI.getWorkbench().getProgressService().run(true, false, op);
				}
				catch (InvocationTargetException e)
				{
					ServoyLog.logError(e);
				}
				catch (InterruptedException e)
				{
					ServoyLog.logError(e);
				}
			}
			else
			{
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						try
						{
							PlatformUI.getWorkbench().getProgressService().run(true, false, op);
						}
						catch (InvocationTargetException e)
						{
							ServoyLog.logError(e);
						}
						catch (InterruptedException e)
						{
							ServoyLog.logError(e);
						}
					}
				});
			}
		}
	}

	private void reloadAllColumnInfo(IServerManagerInternal serverManager)
	{
		String[] servers = serverManager.getServerNames(true, true, false, false);
		for (int i = servers.length - 1; i >= 0; i--)
		{
			IServer s = serverManager.getServer(servers[i]);
			if (s != null && ((IServerInternal)s).isValid() && ((IServerInternal)s).getConfig().isEnabled() && ((IServerInternal)s).isTableListLoaded())
			{
				DataModelManager.reloadAllColumnInfo((IServerInternal)s);
			}
		}
	}

	private void resetActiveEditingFlattenedSolutions()
	{
		for (ServoyProject p : getModulesOfActiveProject())
		{
			p.resetEditingFlattenedSolution(true, true);
		}
	}

	private void testBuildPaths(ServoyProject sp, Set<ServoyProject> processed)
	{
		if (sp == null || sp.getProject() == null || !sp.getProject().exists()) return;
		if (processed.add(sp))
		{
			IScriptProject scriptProject = DLTKCore.create(sp.getProject());
			if (scriptProject == null) return;
			try
			{
				boolean added = false;
				List<IBuildpathEntry> buildPaths = new ArrayList<IBuildpathEntry>();
				IBuildpathEntry[] rawBuildpath = scriptProject.getRawBuildpath();
				for (IBuildpathEntry entry : rawBuildpath)
				{
					if (entry.getEntryKind() == IBuildpathEntry.BPE_SOURCE)
					{
						List<IPath> lst = new ArrayList<IPath>(Arrays.asList(entry.getExclusionPatterns()));
						if (!lst.contains(new Path(ResourcesUtils.STP_DIR + "/")))
						{
							lst.add(new Path(ResourcesUtils.STP_DIR + "/"));
						}
						if (!lst.contains(new Path(SolutionSerializer.MEDIAS_DIR + "/")))
						{
							lst.add(new Path(SolutionSerializer.MEDIAS_DIR + "/"));
						}
						buildPaths.add(DLTKCore.newSourceEntry(sp.getProject().getFullPath(), lst.toArray(new IPath[lst.size()])));
						added = true;
						break;
					}
				}
				if (!added)
				{
					buildPaths.add(DLTKCore.newSourceEntry(sp.getProject().getFullPath(), new IPath[] { new Path(ResourcesUtils.STP_DIR + "/"), new Path(
						SolutionSerializer.MEDIAS_DIR + "/") }));
				}
				String[] moduleNames = Utils.getTokenElements(sp.getSolution().getModulesNames(), ",", true);
				Arrays.sort(moduleNames);
				// test all build paths
				for (String moduleName : moduleNames)
				{
					try
					{
						if (!sp.getProject().getName().equals(moduleName)) // I did see a solution listing itself as a module, causing trouble (don't know how that happened...)
						{
							IProject moduleProject = ServoyModel.getWorkspace().getRoot().getProject(moduleName);
							if (moduleProject.isAccessible())
							{
								ServoyProject servoyModuleProject = (ServoyProject)moduleProject.getNature(ServoyProject.NATURE_ID);
								if (servoyModuleProject != null && servoyModuleProject.getSolution() != null) // maybe it's not a valid solution project
								{
									testBuildPaths(servoyModuleProject, processed);
									buildPaths.add(DLTKCore.newProjectEntry(moduleProject.getFullPath(), true));
								}
							}
						}
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
				scriptProject.setRawBuildpath(buildPaths.toArray(new IBuildpathEntry[buildPaths.size()]), null);
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	private boolean fireActiveProjectWillChange(ServoyProject toProject)
	{
		List<IActiveProjectListener> clone = new ArrayList<IActiveProjectListener>(activeProjectListeners);
		for (IActiveProjectListener listener : clone)
		{
			try
			{
				if (!listener.activeProjectWillChange(activeProject, toProject))
				{
					return false;
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
		return true;
	}

	private void fireActiveProjectChanged()
	{
		refreshGlobalScopes(false);

		if (isActiveSolutionMobile())
		{
			// the enablement/disablement logic of mobile launch toolbar button is in the  exporter plugin
			// (the button is disabled by default)
			Platform.getPlugin("com.servoy.eclipse.exporter.mobile");
		}
		List<IActiveProjectListener> clone = new ArrayList<IActiveProjectListener>(activeProjectListeners);
		for (IActiveProjectListener listener : clone)
		{
			try
			{
				listener.activeProjectChanged(activeProject);
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}

	private void modulesChangedForActiveSolution()
	{
		resetActiveEditingFlattenedSolutions();
		updateFlattenedSolution();
		getUserManager().reloadAllSecurityInformation();
		fireActiveProjectUpdated(IActiveProjectListener.MODULES_UPDATED); // this will also eventually refresh JS build paths and working set
	}

	private void fireActiveProjectUpdated(int updateInfo)
	{
		if (updateInfo == IActiveProjectListener.MODULES_UPDATED)
		{
			refreshGlobalScopes(false);
		}

		List<IActiveProjectListener> clone = new ArrayList<IActiveProjectListener>(activeProjectListeners);
		for (IActiveProjectListener listener : clone)
		{
			try
			{
				listener.activeProjectUpdated(activeProject, updateInfo);
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}

	public void addActiveProjectListener(IActiveProjectListener listener)
	{
		activeProjectListeners.add(listener);
	}

	public void removeActiveProjectListener(IActiveProjectListener listener)
	{
		activeProjectListeners.remove(listener);
	}

	/**
	 * @see firePersistsChanged
	 * @param realSolution
	 * @param persist
	 * @param recursive
	 */
	public void firePersistChanged(boolean realSolution, Object obj, boolean recursive)
	{
		flushFlattenedFormCache(realSolution); // looking up elements in firePersistChangedInternal may use out-of-date flattened forms
		firePersistChangedInternal(realSolution, obj, recursive, new HashSet<Integer>());
	}

	private void firePersistChangedInternal(boolean realSolution, Object obj, boolean recursive, Set<Integer> visited)
	{
		// Protect against cycle in form extends relation.
		if (obj instanceof IPersist && !visited.add(new Integer(((IPersist)obj).getID())))
		{
			return;
		}

		List<IPersist> changed;
		if (recursive)
		{
			changed = new ArrayList<IPersist>();

			List<Object> toAdd = new ArrayList<Object>();
			toAdd.add(obj);
			Iterator< ? > elementsIte;
			while (toAdd.size() > 0)
			{
				Object element = toAdd.remove(0);
				if (element instanceof IPersist)
				{
					changed.add((IPersist)element);
				}

				if (element instanceof ISupportChilds)
				{
					elementsIte = ((ISupportChilds)element).getAllObjects();
				}
				else if (element instanceof FormElementGroup)
				{
					elementsIte = ((FormElementGroup)element).getElements();
				}
				else
				{
					elementsIte = null;
				}
				while (elementsIte != null && elementsIte.hasNext())
				{
					toAdd.add(elementsIte.next());
				}
			}
		}
		else if (obj instanceof IPersist)
		{
			changed = new ArrayList<IPersist>(1);
			changed.add((IPersist)obj);
		}
		else
		{
			return;
		}

		firePersistsChanged(realSolution, changed);

		if (obj instanceof Form) // all inheriting Forms has been changed
		{
			FlattenedSolution fs = realSolution ? getFlattenedSolution() : getEditingFlattenedSolution((Form)obj);
			for (Form form : fs.getDirectlyInheritingForms((Form)obj))
			{
				firePersistChangedInternal(realSolution, form, recursive, visited);
			}
		}
	}

	/**
	 * Create the refresh job for the receiver.
	 *
	 */
	private Job createFireRealPersistchangesJob()
	{
		Job refreshJob = new Job("Refresh Servoy Model")
		{
			/*
			 * (non-Javadoc)
			 *
			 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
			 */
			@Override
			public IStatus run(IProgressMonitor monitor)
			{
				List<IPersist> changes = getOutstandingRealPersistChanges();
				if (changes.size() > 0)
				{
					firePersistsChangedEx(true, new HashSet<IPersist>(changes));
				}
				return Status.OK_STATUS;
			}
		};
		refreshJob.setSystem(true);
		return refreshJob;
	}

	@Override
	protected FlattenedSolution createFlattenedSolution()
	{
		return new FlattenedSolution(true); // flattened form cache will be flushed when persists changes
	}

	public void revertEditingPersist(ServoyProject sp, IPersist persist) throws RepositoryException
	{
		if (sp.getEditingSolution() == null) return;

		if (persist.getRootObject() != sp.getEditingSolution())
		{
			// not in this editing solution, probably in real solution.
			throw new RepositoryException("Object to revert out of sync");
		}

		final List<IPersist> changed = new ArrayList<IPersist>();
		persist.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				if (o.isChanged())
				{
					changed.add(o);
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});
		sp.updateEditingPersist(persist, true);
		firePersistsChanged(false, changed);
	}

	private void flushFlattenedFormCache(boolean realSolution)
	{
		if (realSolution)
		{
			getFlattenedSolution().flushFlattenedFormCache();
		}
		else
		{
			// editing solution, flush all flattened form caches in all servoy projects
			for (ServoyProject project : getModulesOfActiveProject())
			{
				project.getEditingFlattenedSolution().flushFlattenedFormCache();
			}
		}
	}

	private final int[] isCollectingPersistChanges = new int[] { 0, 0 };
	@SuppressWarnings("unchecked")
	private final List<IPersist>[] collectedPersistChanges = new List[] { new ArrayList<IPersist>(), new ArrayList<IPersist>() };

	public void startCollectingPersistChanges(boolean realSolution)
	{
		synchronized (isCollectingPersistChanges)
		{
			isCollectingPersistChanges[realSolution ? 0 : 1]++;
		}
	}

	public boolean isCollectingPersistChanges(boolean realSolution, Collection<IPersist> changes)
	{
		synchronized (isCollectingPersistChanges)
		{
			if (isCollectingPersistChanges[realSolution ? 0 : 1] == 0)
			{
				return false;
			}
			List<IPersist> collected = collectedPersistChanges[realSolution ? 0 : 1];
			for (IPersist persist : changes)
			{
				if (!collected.contains(persist))
				{
					collected.add(persist);
				}
			}
			return true;
		}
	}

	public void stopCollectingPersistChanges(boolean realSolution)
	{
		Collection<IPersist> collected;
		synchronized (isCollectingPersistChanges)
		{
			if (--isCollectingPersistChanges[realSolution ? 0 : 1] > 0 || collectedPersistChanges[realSolution ? 0 : 1].size() == 0)
			{
				return;
			}
			collected = collectedPersistChanges[realSolution ? 0 : 1];
			collectedPersistChanges[realSolution ? 0 : 1] = new ArrayList<IPersist>();
		}
		firePersistsChanged(realSolution, collected);
	}

	/**
	 * Notify listeners of changes to persists. Changes can be notified for the real solutions or the editing solutions.
	 *
	 * @param realSolution
	 * @param changes
	 */
	public void firePersistsChanged(boolean realSolution, Collection<IPersist> changes)
	{
		if (changes.size() == 0 || isCollectingPersistChanges(realSolution, changes)) return;

		flushFlattenedFormCache(realSolution);
		if (realSolution)
		{
			synchronized (fireRealPersistchangesJob)
			{
				realOutstandingChanges.addAll(changes);
				fireRealPersistchangesJob.cancel();
				fireRealPersistchangesJob.schedule(100);// wait .1 sec for more changes before start firing
			}
		}
		else
		{
			firePersistsChangedEx(false, changes);
		}
	}

	/**
	 * @param realSolution
	 * @param changes
	 */
	private void firePersistsChangedEx(boolean realSolution, Collection<IPersist> changes)
	{
		List<IPersistChangeListener> listeners = realSolution ? realPersistChangeListeners : editingPersistChangeListeners;
		for (IPersistChangeListener listener : listeners.toArray(new IPersistChangeListener[listeners.size()]))
		{
			try
			{
				listener.persistChanges(changes);
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}

	public void fireI18NChanged()
	{
		ArrayList<I18NChangeListener> clone = new ArrayList<I18NChangeListener>(i18nChangeListeners);
		for (I18NChangeListener listener : clone)
		{
			try
			{
				listener.i18nChanged();
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}

	public void fireSolutionMetaDataChanged(Solution changedSolution)
	{
		ArrayList<ISolutionMetaDataChangeListener> clone = new ArrayList<ISolutionMetaDataChangeListener>(solutionMetaDataChangeListener);
		for (ISolutionMetaDataChangeListener listener : clone)
		{
			try
			{
				listener.solutionMetaDataChanged(changedSolution);
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}

	private List<IPersist> getOutstandingRealPersistChanges()
	{
		List<IPersist> outstandingChanges;
		synchronized (fireRealPersistchangesJob)
		{
			outstandingChanges = realOutstandingChanges;
			realOutstandingChanges = new ArrayList<IPersist>();
		}
		return outstandingChanges;
	}

	public void addPersistChangeListener(boolean realSolution, IPersistChangeListener listener)
	{
		(realSolution ? realPersistChangeListeners : editingPersistChangeListeners).add(listener);
	}

	public void removePersistChangeListener(boolean realSolution, IPersistChangeListener listener)
	{
		(realSolution ? realPersistChangeListeners : editingPersistChangeListeners).remove(listener);
	}

	public void addSolutionMetaDataChangeListener(ISolutionMetaDataChangeListener listener)
	{
		solutionMetaDataChangeListener.add(listener);
	}

	public void removeSolutionMetaDataChangeListener(ISolutionMetaDataChangeListener listener)
	{
		solutionMetaDataChangeListener.remove(listener);
	}

	public void addI18NChangeListener(I18NChangeListener listener)
	{
		i18nChangeListeners.add(listener);
	}

	public void removeI18NChangeListener(I18NChangeListener listener)
	{
		i18nChangeListeners.remove(listener);
	}


	/**
	 * Returns the workbench associated with this object.
	 */
	public static IWorkspace getWorkspace()
	{
		return ResourcesPlugin.getWorkspace();
	}

	private void resourcesPostChanged(IResourceChangeEvent event)
	{
		if (getDeveloperRepository() == null)
		{
			// change notification at startup, Activator has not finished yet
			return;
		}

		boolean callActiveProject = false;
		// these are the projects
		IResourceDelta[] affectedChildren = event.getDelta().getAffectedChildren();
		boolean moduleListChanged = false; // no use updating flattened solutions/security/building multiple times and so on... just do it once if needed
		boolean resourcesUpdated = false; // no use calling this multiple times either
		for (IResourceDelta element : affectedChildren)
		{
			IResource resource = element.getResource();
			try
			{
				if (resource instanceof IProject && ((!((IProject)resource).isOpen()) || (!((IProject)resource).hasNature(ServoyUpdatingProject.NATURE_ID))))
				{
					final IProject project = (IProject)resource;
					EclipseRepository eclipseRepository = (EclipseRepository)getDeveloperRepository();

					// DO STUFF RELATED TO SOLUTION PROJECTS
					if (element.getKind() != IResourceDelta.REMOVED && project.isOpen() && project.hasNature(ServoyProject.NATURE_ID))
					{
						// so the project is a Servoy project, still exists and is open
						// refresh cached project list if necessary
						if (getServoyProject(resource.getName()) == null)
						{
							// a project that should be added to Servoy project list cache
							refreshServoyProjects();
							if (activeProject != null &&
								activeProject.getProject() != getWorkspace().getRoot().getProject(activeProject.getProject().getName()))
							{
								// in case active project was replaced/overwritten we must update the reference as well (so we don't have trouble when comparing IProject instances)
								activeProject = getServoyProject(activeProject.getProject().getName());
							}
						}
						else if ((element.getFlags() & IResourceDelta.REPLACED) != 0)
						{
							// it is an overwritten ServoyProject who must be updated as well (same name but other IProject instance)
							refreshServoyProjects();
							if (activeProject != null &&
								activeProject.getProject() != getWorkspace().getRoot().getProject(activeProject.getProject().getName()))
							{
								// in case active project was replaced/overwritten we must update the reference as well (so we don't have trouble when comparing IProject instances)
								activeProject = getServoyProject(activeProject.getProject().getName());
							}
						}

						// check if there is a solution for it
						if (eclipseRepository.isSolutionMetaDataLoaded(resource.getName()))
						{
							// if this solution was not needed before and it is not yet deserialized then we need not load it now
							boolean solutionLoaded = eclipseRepository.isSolutionLoaded(resource.getName());
							final List<IResourceDelta> al;
							if (!solutionLoaded && shouldBeModuleOfActiveSolution(resource.getName()))
							{
								// previously invalid module of active solution? it changed, maybe it is valid now
								al = findChangedFiles(element, new ArrayList<IResourceDelta>());
								// do nothing if no file was really changed
								if (al != null && al.size() != 0)
								{
									if (eclipseRepository.getActiveRootObject(resource.getName(), IRepository.SOLUTIONS) != null) // try to deserialize it again
									{
										moduleListChanged = true; // now it's valid and active solution must be aware of this
									}
								}
							}
							else if (solutionLoaded)
							{
								al = findChangedFiles(element, new ArrayList<IResourceDelta>());
								// do nothing if no file was really changed
								if (al == null || al.size() == 0) continue;
								// there is already a solution, update solution resources only after a complete save in case of multiple resources
								if (getResourceChangesHandlerCounter().getValue() > 0)
								{
									synchronized (outstandingChangedFiles)
									{
										outstandingChangedFiles.addAll(al);
									}
								}
								else
								{
									handleOutstandingChangedFiles(al);
								}
							}
						}
						else
						{
							// maybe this is a new ServoyProject (checked out right now, or imported into the workspace...);
							// in this case we need enable it to have a solution - by updating the repository's root object meta data cache
							eclipseRepository.registerSolutionMetaData(resource.getName());

							if (shouldBeModuleOfActiveSolution(resource.getName()))
							{
								if ((Solution)eclipseRepository.getActiveRootObject(resource.getName(), IRepository.SOLUTIONS) != null)
								{
									// it's a new valid module of the active solution
									moduleListChanged = true;
								}
							}
							else if (activeProject == null)
							{
								callActiveProject = true;
							}
						}
					}
					else
					{
						// a project was deleted, closed, or it does not have Servoy nature; see if it is was part of the repository and if so, remove it
						if (getServoyProject(resource.getName()) != null)
						{
							getServoyProject(resource.getName()).resetEditingFlattenedSolution(false, false);
							refreshServoyProjects();
						}
						boolean isLoaded = eclipseRepository.isSolutionMetaDataLoaded(resource.getName());
						if (isLoaded)
						{
							eclipseRepository.removeRootObject(eclipseRepository.getRootObjectMetaData(resource.getName(), IRepository.SOLUTIONS).getRootObjectId());
							// it is unloaded; avoid loading it afterwards when checking for active solutions
							if (activeProject != null && activeProject.getProject().getName().equals(resource.getName()))
							{
								setActiveProject(null, true);
								callActiveProject = true;
							}
							else if (activeProject != null)
							{
								if (shouldBeModuleOfActiveSolution(resource.getName()))
								{
									moduleListChanged = true;
								}
							}
						}
					}

					// DO STUFF RELATED TO RESOURCES PROJECTS
					checkForResourcesProjectRename(element, project);
					if (activeResourcesProject != null && project == activeResourcesProject.getProject() && project.hasNature(ServoyResourcesProject.NATURE_ID))
					{
						modifyDBIFilesToAllClones(element);
					}
					if (element.getKind() != IResourceDelta.REMOVED && project.isOpen())
					{
						// something happened to this project, resulting in a valid open project; see if it is the currently active resources project
						if (activeResourcesProject != null && project == activeResourcesProject.getProject() &&
							project.hasNature(ServoyResourcesProject.NATURE_ID))
						{
							List<IResourceDelta> al = findChangedFiles(element, new ArrayList<IResourceDelta>());
							handleChangedFilesInResourcesProject(al);
						}
						else
						{
							// maybe the resources nature has been just added to or removed from a referenced resources project;
							// so update resources project in this case
							boolean referencedByActiveProject = false;
							if (activeProject != null)
							{
								IProject[] projects = activeProject.getProject().getReferencedProjects();
								for (IProject p : projects)
								{
									if (p == project)
									{
										referencedByActiveProject = true;
										break;
									}
								}
							}
							if (referencedByActiveProject && element.findMember(new Path(".project")) != null)
							{
								resourcesUpdated = true;
							}
						}
					}
					else if (activeResourcesProject != null && project == activeResourcesProject.getProject())
					{
						// the current resources project lost it's capacity to be a resources project
						resourcesUpdated = true;
					}
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
		if (moduleListChanged && activeProject != null) // maybe user deleted/closed more then 1 project
		{
			modulesChangedForActiveSolution();
		}
		if (resourcesUpdated && activeProject != null)
		{
			updateResources(IActiveProjectListener.RESOURCES_UPDATED_ON_ACTIVE_PROJECT, null);
		}
		if (callActiveProject)
		{
			autoSelectActiveProjectIfNull(true);
		}
	}

	/**
	 * Handle all outstanding changes and new changes (null if none)
	 */
	protected void handleOutstandingChangedFiles(List<IResourceDelta> newChanges)
	{
		List<IResourceDelta> lst;
		synchronized (outstandingChangedFiles)
		{
			if (outstandingChangedFiles.isEmpty() && (newChanges == null || newChanges.isEmpty()))
			{
				// nothing to do
				return;
			}
			lst = new ArrayList<IResourceDelta>(outstandingChangedFiles);
			outstandingChangedFiles.clear();
		}

		if (newChanges != null)
		{
			lst.addAll(newChanges);
		}

		refreshGlobalScopes(true);

		// get deltas per project
		Map<IProject, List<IResourceDelta>> projectChanges = new HashMap<IProject, List<IResourceDelta>>();
		for (IResourceDelta delta : lst)
		{
			IProject project = delta.getResource().getProject();
			List<IResourceDelta> changes = projectChanges.get(project);
			if (changes == null)
			{
				changes = new ArrayList<IResourceDelta>();
				projectChanges.put(project, changes);
			}
			changes.add(delta);
		}
		for (Entry<IProject, List<IResourceDelta>> entry : projectChanges.entrySet())
		{
			IProject project = entry.getKey();
			try
			{
				Solution solution = (Solution)getDeveloperRepository().getActiveRootObject(project.getName(), IRepository.SOLUTIONS);
				handleChangedFilesInSolutionProject(project, solution, entry.getValue());
			}
			catch (Exception e)
			{
				ServoyLog.logError("Could not handle changed files in project " + project.getName(), e);
			}
		}
	}

	/**
	 * Refresh the runtime property Solution.SCOPE_NAMES in solutions.
	 * This will include empty scopes (that have to method or variable yet)
	 */
	private void refreshGlobalScopes(boolean fireChange)
	{
		boolean realSolutionScopnamesChanged = false;
		for (ServoyProject project : getModulesOfActiveProject())
		{
			List<String> globalScopenames = project.getGlobalScopenames();
			String[] scopeNames = globalScopenames.toArray(new String[globalScopenames.size()]);

			Solution solution = project.getEditingSolution();
			if (solution != null)
			{
				solution.setRuntimeProperty(Solution.SCOPE_NAMES, scopeNames);
			}

			solution = project.getSolution();
			if (solution != null)
			{
				if (fireChange && !realSolutionScopnamesChanged)
				{
					realSolutionScopnamesChanged = !Utils.equalObjects(solution.getRuntimeProperty(Solution.SCOPE_NAMES), scopeNames);
				}
				solution.setRuntimeProperty(Solution.SCOPE_NAMES, scopeNames);
			}
		}

		if (fireChange && realSolutionScopnamesChanged)
		{
			fireActiveProjectUpdated(IActiveProjectListener.SCOPE_NAMES_CHANGED);
		}
	}

	@Override
	public void reportSaveError(final Exception ex)
	{
		super.reportSaveError(ex);
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				MessageDialog.openError(UIUtils.getActiveShell(), "Save error", ex.getMessage());
			}
		});
	}

	private void modifyDBIFilesToAllClones(final IResourceDelta element)
	{
		List<IResourceDelta> al = findChangedFiles(element, new ArrayList<IResourceDelta>());
		IServerManagerInternal serverManager = getServerManager();
		if (serverManager != null && dataModelManager != null)
		{
			for (IResourceDelta fileRd : al)
			{
				final IFile file = (IFile)fileRd.getResource();
				if (file.getName().endsWith(DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT))
				{
					String serverName = file.getParent().getName();
					final IServer[] servers = serverManager.getDataModelCloneServers(serverName);
					if (servers != null && servers.length > 0)
					{
						final String tableName = file.getName().substring(0,
							file.getName().length() - DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT.length());
						final Job job = new WorkspaceJob("Writing dbi file changes to all clones - " + file.getName())
						{
							@Override
							public IStatus runInWorkspace(IProgressMonitor monitor)
							{
								for (IServer server : servers)
								{
									try
									{
										IFile cloneFile = dataModelManager.getDBIFile(server.getName(), tableName);
										if ((element.getKind() == IResourceDelta.REMOVED || !file.exists()) && cloneFile.exists())
										{
											cloneFile.delete(true, null);
										}
										if (file.exists() && (element.getKind() == IResourceDelta.ADDED || element.getKind() == IResourceDelta.CHANGED))
										{
											InputStream is = file.getContents();
											try
											{
												if (cloneFile.exists())
												{
													cloneFile.setContents(is, true, false, null);
												}
												else
												{
													ResourcesUtils.createFileAndParentContainers(cloneFile, is, true);
												}
											}
											finally
											{
												Utils.closeInputStream(is);
											}
										}
									}
									catch (Exception ex)
									{
										ServoyLog.logError(ex);
									}
								}
								return Status.OK_STATUS;
							}
						};

						job.setUser(false);
						job.setSystem(true);
						job.setRule(getWorkspace().getRoot());
						job.schedule();
					}
				}
			}
		}
	}

	private void checkForResourcesProjectRename(IResourceDelta element, IProject project)
	{
		try
		{
			if (element.getKind() == IResourceDelta.ADDED && ((element.getFlags() & IResourceDelta.MOVED_FROM) != 0) && project.isOpen() &&
				project.hasNature(ServoyResourcesProject.NATURE_ID))
			{
				// if a resources project has been renamed, update all solutions that reference
				// this project so that they have a reference to the new one
				IPath oldPath = element.getMovedFromPath();
				ServoyProject[] servoyProjects = getServoyProjects();
				for (ServoyProject sp : servoyProjects)
				{
					// see if the solution project has reference to oldPath
					IProject[] rps = sp.getProject().getReferencedProjects();
					for (IProject p : rps)
					{
						if (p.getFullPath().equals(oldPath))
						{
							WorkspaceJob job;
							// create new resource project if necessary and reference it from selected solution
							job = new ResourcesProjectSetupJob("Updating resources project for solution '" + sp.getProject().getName() +
								"' due to resources project rename.", project, p, sp.getProject(), true);
							job.setRule(sp.getProject().getWorkspace().getRoot());
							job.setSystem(true);
							job.schedule();
							break;
						}
					}
				}
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Exception while looking into rprn", e);
		}
	}

	private boolean isModule(Solution solution)
	{
		Solution[] modules = getFlattenedSolution().getModules();
		boolean isModule = false;
		if (modules != null)
		{
			for (Solution m : modules)
			{
				if (m == solution)
				{
					isModule = true;
					break;
				}
			}
		}
		return isModule;
	}

	/**
	 * @param solution
	 * @param al
	 * @throws RepositoryException
	 */
	private void handleChangedFilesInSolutionProject(final IProject project, Solution solution, List<IResourceDelta> al) throws RepositoryException
	{
		final SolutionDeserializer.ObjBeforeJSExtensionComparator stringComparer = new SolutionDeserializer.ObjBeforeJSExtensionComparator();
		Collections.sort(al, new Comparator<IResourceDelta>()
		{
			public int compare(IResourceDelta o1, IResourceDelta o2)
			{
				return stringComparer.compare(o1.getFullPath().toString(), o2.getFullPath().toString());
			}
		});

		List<File> changedFiles = new ArrayList<File>();
		for (IResourceDelta fileRd : al)
		{
			IPath filePath = fileRd.getProjectRelativePath();
			if (filePath.segmentCount() > 0 && filePath.segment(0).startsWith(".") && !filePath.segment(0).equals(".project")) continue; // ignore resources starting with dot (ex. .stp, .teamprovider)
			changedFiles.add(fileRd.getResource().getLocation().toFile());
		}

		changedFiles.removeAll(ignoreOnceFiles);
		ignoreOnceFiles.clear();
		if (changedFiles.size() > 0)
		{
			final ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solution.getName());
			final IContainer workspace = project.getParent();

			SolutionDeserializer sd = new SolutionDeserializer(getDeveloperRepository(), servoyProject);
			final Set<IPersist> changedScriptElements = handleChangedFiles(project, solution, changedFiles, servoyProject, sd);
			// Regenerate script files for parents that have changed script elements.
			if (changedScriptElements.size() > 0)
			{
				final Job job = new UIJob("Check changed script files")
				{
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor)
					{
						//if (true) return Status.OK_STATUS;
						Set<IFile> recreatedFiles = new HashSet<IFile>();
						for (IPersist persist : changedScriptElements)
						{
							IFile scriptFile = workspace.getFile(new Path(SolutionSerializer.getScriptPath(persist, false)));
							if (!recreatedFiles.add(scriptFile))
							{
								continue;
							}
							MultiTextEdit textEdit = getScriptFileChanges(persist, scriptFile);

							if (textEdit.getChildrenSize() > 0)
							{
								// ignore the next time once. So that it won't be parsed again.
								addIgnoreFile(scriptFile.getLocation().toFile());

								ITextFileBufferManager textFileBufferManager = FileBuffers.getTextFileBufferManager();
								try
								{
									textFileBufferManager.connect(scriptFile.getFullPath(), LocationKind.IFILE, new SubProgressMonitor(monitor, 1));
								}
								catch (CoreException e)
								{
									ServoyLog.logError(e);
									continue;
								}

								try
								{
									ITextFileBuffer textFileBuffer = textFileBufferManager.getTextFileBuffer(scriptFile.getFullPath(), LocationKind.IFILE);
									IDocument document = textFileBuffer.getDocument();

									FileEditorInput editorInput = new FileEditorInput(scriptFile);
									final IEditorPart openEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null ? null
										: PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findEditor(editorInput);

									boolean dirty = openEditor != null ? openEditor.isDirty() : textFileBuffer.isDirty();

									try
									{
										textEdit.apply(document);
									}
									catch (Exception e)
									{
										ServoyLog.logError(e);
									}

									if (!dirty)
									{
										if (openEditor != null)
										{
											openEditor.doSave(monitor);
										}
										else
										{
											try
											{
												textFileBuffer.commit(monitor, true);
											}
											catch (CoreException e)
											{
												ServoyLog.logError(e);
											}
										}
									}
								}
								finally
								{
									try
									{
										textFileBufferManager.disconnect(scriptFile.getFullPath(), LocationKind.IFILE, new SubProgressMonitor(monitor, 1));
									}
									catch (CoreException e)
									{
										ServoyLog.logError(e);
									}
								}
							}
						}
						return Status.OK_STATUS;
					}
				};

				job.setUser(false);
				job.setSystem(true);
				job.setRule(getWorkspace().getRoot());
				// Schedule the job in the UI thread, the job is only scheduled when the UI thread is released (may be held by
				// EclipseRepository.updateNodesInWorkspace())
				// deadlock situation: scriptjob is waits for UI thread but keeps workspace rule, second job in EclipseRepository.updateNodesInWorkspace
				// is not started because of rule conflicting and main thread (in EclipseRepository.updateNodesInWorkspace) hold UI thread and waits for second job to finish (latch)
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						job.schedule();
					}
				});
			}
		}
	}

	/**
	 * @param project
	 * @param solution
	 * @param changedFiles
	 * @param servoyProject
	 * @param workspace
	 * @param sd
	 * @return
	 * @throws RepositoryException
	 */
	public Set<IPersist> handleChangedFiles(IProject project, Solution solution, List<File> changedFiles, final ServoyProject servoyProject,
		SolutionDeserializer sd) throws RepositoryException
	{
		// TODO refresh modules when solution type was changed
		List<IPersist> strayCats = new ArrayList<IPersist>();
		String oldModules = solution.getModulesNames();
		List<File> nonvistedFiles = sd.updateSolution(project.getLocation().toFile(), solution, changedFiles, strayCats, false, false);

		// see if modules were changed
		String newModules = solution.getModulesNames();
		if (solution.isChanged() && (oldModules != newModules) && (oldModules == null || (!oldModules.equals(newModules))) && activeProject != null &&
			(activeProject.getSolution() == solution || isModule(solution)))
		{
			modulesChangedForActiveSolution();
		}

		final LinkedHashMap<UUID, IPersist> changed = new LinkedHashMap<UUID, IPersist>();
		final LinkedHashMap<UUID, IPersist> changedEditing = new LinkedHashMap<UUID, IPersist>();
		final Set<IPersist> changedScriptElements = new HashSet<IPersist>();
		solution.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist persist)
			{
				if (persist.isChanged())
				{
					// update working copy
					try
					{
						IPersist editingPersist = servoyProject.updateEditingPersist(persist, SolutionSerializer.isCompositeWithItems(persist));
						persist.clearChanged();
						changed.put(persist.getUUID(), persist);
						changedEditing.put(persist.getUUID(), editingPersist);
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}

					// find js-files that may have to be recreated
					if (persist instanceof IScriptElement)
					{
						changedScriptElements.add(persist);
					}
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});

		File projectFile = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace()).getProjectFile(project.getName());
		File mediaDir = new File(project.getLocation().toFile(), SolutionSerializer.MEDIAS_DIR);

		boolean securityInfoChanged = false;
		boolean modulesChanged = false;
		List<Solution> metaDataChanges = null;
		Set<UUID> checkedParents = new HashSet<UUID>();
		for (File file : nonvistedFiles)
		{
			if (file.exists() && file.getName().equals(".project"))
			{
				// maybe referenced projects changed - so update resources project
				updateResources(IActiveProjectListener.RESOURCES_UPDATED_ON_ACTIVE_PROJECT, null);
				// file outside of visited tree, ignore
				continue;
			}
			else if (file.getName().endsWith(WorkspaceUserManager.SECURITY_FILE_EXTENSION) && isSolutionActive(project.getName()))
			{
				// must be a form ".sec" file; find the form for it...
				File projectFolder = project.getLocation().toFile();
				File formsContainer = file.getParentFile();
				if (formsContainer != null && formsContainer.getName().equals(SolutionSerializer.FORMS_DIR) &&
					projectFolder.equals(formsContainer.getParentFile()))
				{
					String formName = file.getName().substring(0, file.getName().length() - WorkspaceUserManager.SECURITY_FILE_EXTENSION.length());
					Form f = solution.getForm(formName);
					if (f != null)
					{
						securityInfoChanged = true;
						if (!getUserManager().isWritingResources())
						{
							// only reload resources if the user manager is not the one modifying the ".sec" file
							getUserManager().reloadSecurityInfo(f);
						}
					}
				}
				// file outside of visited tree, ignore
				continue;
			}
			else if (file.getName().equals(SolutionSerializer.ROOT_METADATA))
			{
				String solutionName = solution.getName();
				// this means that the meta-data has changed on disk for a solution that already had it's meta-data loaded; refresh
				// repository in case the change came as a result of a resource change only (team update, ...) - so not because of an in-memory change + serialisation to disk
				EclipseRepository eclipseRepository = (EclipseRepository)getDeveloperRepository();
				eclipseRepository.registerSolutionMetaData(solutionName);

				boolean isPartOfActiveFlattenedSolution = isSolutionActive(solutionName);
				boolean isImportHook = SolutionMetaData.isImportHook(solution.getSolutionMetaData());
				if (isPartOfActiveFlattenedSolution && isImportHook)
				{
					// this means that an active solution/module's type might have changed to import hook - we show those but
					// they should not be part of the flattened solution and should not be expandable in the tree; update these
					modulesChanged = true;
				}
				else if (!isPartOfActiveFlattenedSolution && !isImportHook && shouldBeModuleOfActiveSolution(solutionName))
				{
					// if it was an import hook of the active solution before (so not part of flattened solution), and
					// now it changed type to non-import hook, flattened solution and tree need to get updated...
					modulesChanged = true;
				}
				else if (isPartOfActiveFlattenedSolution && !isImportHook && Utils.stringSafeEquals(solutionName, getActiveProject().getProject().getName()))
				{
					// something changed in the active solution; find the case when the active solution's solution type changed from import hook type
					// to some other type - in this case import hook modules should change in the tree/flattened solution
					ServoyProject[] importHooks = getImportHookModulesOfActiveProject();
					if (importHooks.length > 0 && isSolutionActive(importHooks[0].getProject().getName()))
					{
						// so an import hook module is expandable although the active solution is not of import hook type; this means that the active solution
						// just changed it's type to non-import hook
						modulesChanged = true;
					}
				}

				// fire these as well just in case something needs to update and only listens to persists
				changed.put(solution.getUUID(), solution);
				ServoyProject sp = getServoyProject(solutionName);
				if (sp != null)
				{
					Solution s = sp.getEditingSolution();
					changedEditing.put(s.getUUID(), s);
				}

				if (metaDataChanges == null) metaDataChanges = new ArrayList<Solution>();
				metaDataChanges.add(solution);
				continue;
			}
			else if (file.getPath().startsWith(mediaDir.getPath() + File.separator))
			{
				String name = file.getPath().substring(mediaDir.getPath().length() + 1).replace('\\', '/');
				Solution editingSolution = servoyProject.getEditingSolution();
				EclipseRepository eclipseRepository = (EclipseRepository)editingSolution.getRepository();

				Media media = editingSolution.getMedia(name);
				if (media == null && file.exists())
				{
					// if there is a media in the modules with the same name, but no file on in the ws, then ignore it, because
					// it is deleting
					int skip_media_id = 0;
					Media moduleMedia = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getMedia(name);
					if (moduleMedia != null)
					{
						Pair<String, String> filePath = SolutionSerializer.getFilePath(moduleMedia, false);
						if (!servoyProject.getProject().getWorkspace().getRoot().getFile(new Path(filePath.getLeft() + filePath.getRight())).exists()) skip_media_id = moduleMedia.getID();
					}

					media = editingSolution.createNewMedia(ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(), name, skip_media_id);
					media.setMimeType(eclipseRepository.getContentType(name));
				}
				if (media != null) eclipseRepository.updateNodesInWorkspace(new IPersist[] { media }, false, false);
			}

			File parentFile = SolutionSerializer.getParentFile(projectFile, file);
			if (parentFile == null || !parentFile.exists())
			{
				// parent also deleted
				continue;
			}
			UUID parentUuid = SolutionDeserializer.getUUID(parentFile);
			if (!checkedParents.add(parentUuid))
			{
				// parent already checked for missing children
				continue;
			}
			IPersist parent = AbstractRepository.searchPersist(solution, parentUuid);
			if (!(parent instanceof ISupportChilds))
			{
				// parent already deleted
				continue;
			}

			// check the files of the old children list (put in array first to prevent concurrent modex.
			for (IPersist child : Utils.asArray(((ISupportChilds)parent).getAllObjects(), IPersist.class))
			{
				Pair<String, String> filePath = SolutionSerializer.getFilePath(child, false);
				if (!new File(projectFile.getParentFile(), filePath.getLeft() + filePath.getRight()).exists())
				{
					// deleted persist
					// delete the persist from the real solution
					child.getParent().removeChild(child);
					// push the delete to the editing solution
					IPersist editingPersist = servoyProject.updateEditingPersist(child, false);

					changed.put(child.getUUID(), child);
					if (editingPersist != null)
					{
						changedEditing.put(child.getUUID(), editingPersist);
					}
				}
			}
		}

		// push the deleted stray cats (deleted methods) to the editing solution
		for (IPersist child : strayCats)
		{
			IPersist editingPersist = servoyProject.updateEditingPersist(child, false);

			changed.put(child.getUUID(), child);
			// if already null, then it was already deleted.
			if (editingPersist != null)
			{
				changedEditing.put(child.getUUID(), editingPersist);
			}
		}

		// update the last modified time for the web client.
		solution.updateLastModifiedTime();

		if (modulesChanged)
		{
			modulesChangedForActiveSolution();
		}

		firePersistsChanged(true, changed.values());
		firePersistsChanged(false, changedEditing.values());

		if (securityInfoChanged)
		{
			fireActiveProjectUpdated(IActiveProjectListener.SECURITY_INFO_CHANGED);
		}

		if (metaDataChanges != null)
		{
			for (Solution s : metaDataChanges)
			{
				fireSolutionMetaDataChanged(s);
			}
		}

		return changedScriptElements;
	}

	private void handleChangedFilesInResourcesProject(List<IResourceDelta> al)
	{
		// split files into categories, to make sure we make the updates in a certain order (for example column info before security - because security uses the DB)
		List<IResourceDelta> styleFiles = new ArrayList<IResourceDelta>();
		List<IResourceDelta> templateFiles = new ArrayList<IResourceDelta>();
		List<IResourceDelta> columnInfoFiles = new ArrayList<IResourceDelta>();
		List<IResourceDelta> securityFiles = new ArrayList<IResourceDelta>();
		List<IResourceDelta> i18nFiles = new ArrayList<IResourceDelta>();

		for (IResourceDelta fileRd : al)
		{
			IFile file = (IFile)fileRd.getResource();
			if (file.getName().endsWith(SolutionSerializer.STYLE_FILE_EXTENSION))
			{
				if (!file.getProjectRelativePath().segment(0).equals(SolutionSerializer.COMPONENTS_DIR_NAME) &&
					!file.getProjectRelativePath().segment(0).equals(SolutionSerializer.SERVICES_DIR_NAME))
				{
					styleFiles.add(fileRd);
				}
			}
			else if (file.getName().endsWith(SolutionSerializer.TEMPLATE_FILE_EXTENSION))
			{
				templateFiles.add(fileRd);
			}
			else if (file.getName().endsWith(DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT))
			{
				columnInfoFiles.add(fileRd);
			}
			else if (file.equals(activeResourcesProject.getProject().getFile(new Path(WorkspaceUserManager.SECURITY_FILE_RELATIVE_TO_PROJECT))) ||
				file.getName().endsWith(WorkspaceUserManager.SECURITY_FILE_EXTENSION))
			{
				securityFiles.add(fileRd);
			}
			else if (file.getName().endsWith(EclipseMessages.MESSAGES_EXTENSION))
			{
				i18nFiles.add(fileRd);
			}
			else if (file.getName().equals(SolutionSerializer.WORKINGSETS_FILE))
			{
				readWorkingSetsFromResourcesProject();
			}
		}

		// STYLES
		final List<IPersist> modifiedStyleList = new ArrayList<IPersist>();
		boolean stylesAddedOrRemoved = handleChangedStringResources(IRepository.STYLES, styleFiles, modifiedStyleList);

		// TEMPLATES
		boolean templatesAddedOrRemoved = handleChangedStringResources(IRepository.TEMPLATES, templateFiles, new ArrayList<IPersist>());

		// COLUMN INFORMATION (.dbi)
		boolean columnInfoChanged = handleChangedColumnInformation(columnInfoFiles);

		// SECURITY
		boolean securityInfoChanged = handleChangedSecurityFiles(securityFiles);

		if (stylesAddedOrRemoved)
		{
			fireActiveProjectUpdated(IActiveProjectListener.STYLES_ADDED_OR_REMOVED);
		}
		if (templatesAddedOrRemoved)
		{
			fireActiveProjectUpdated(IActiveProjectListener.TEMPLATES_ADDED_OR_REMOVED);
		}
		if (columnInfoChanged)
		{
			fireActiveProjectUpdated(IActiveProjectListener.COLUMN_INFO_CHANGED);
		}
		if (securityInfoChanged)
		{
			fireActiveProjectUpdated(IActiveProjectListener.SECURITY_INFO_CHANGED);
		}
		if (modifiedStyleList.size() > 0)
		{
			for (IPersist style : modifiedStyleList)
			{
				ComponentFactory.flushStyle(null, (Style)style);
			}
			firePersistsChanged(true, modifiedStyleList);
			firePersistsChanged(false, modifiedStyleList);
		}
		if (i18nFiles.size() > 0)
		{
			fireI18NChanged();
		}
	}

	private boolean handleChangedSecurityFiles(List<IResourceDelta> securityFiles)
	{
		boolean securityInfoChanged = false;
		if (securityFiles != null && securityFiles.size() > 1)
		{
			if (!getUserManager().isWritingResources())
			{
				getUserManager().reloadAllSecurityInformation();
			}
		}
		else for (IResourceDelta fileRd : securityFiles)
		{
			final IFile file = (IFile)fileRd.getResource();
			if (file.equals(activeResourcesProject.getProject().getFile(new Path(WorkspaceUserManager.SECURITY_FILE_RELATIVE_TO_PROJECT))))
			{
				// users/groups have changed - will reload all security information
				securityInfoChanged = true;
				if (!getUserManager().isWritingResources())
				{
					getUserManager().reloadAllSecurityInformation();
				}
			}
			else if (file.getName().endsWith(WorkspaceUserManager.SECURITY_FILE_EXTENSION))
			{
				IContainer serverContainer = file.getParent();
				if (serverContainer != null && serverContainer.getParent() != null &&
					serverContainer.getParent().getName().equals(SolutionSerializer.DATASOURCES_DIR_NAME) &&
					serverContainer.getParent().getParent() instanceof IProject)
				{
					String serverName = serverContainer.getName();
					IServerManagerInternal sm = getServerManager();
					if (sm != null)
					{
						IServer s = sm.getServer(serverName);
						if (s != null)
						{
							try
							{
								String tableName = file.getName().substring(0, file.getName().length() - WorkspaceUserManager.SECURITY_FILE_EXTENSION.length());
								ITable t = s.getTable(tableName);
								if (t != null)
								{
									securityInfoChanged = true;
									if (!getUserManager().isWritingResources())
									{
										getUserManager().reloadSecurityInfo(serverName, tableName);
									}
								}
							}
							catch (RepositoryException e)
							{
								ServoyLog.logError(e);
							}
							catch (RemoteException e)
							{
								ServoyLog.logError(e);
							}
						}
					}
				}
			}
		}
		return securityInfoChanged;
	}

	private boolean handleChangedColumnInformation(List<IResourceDelta> columnInfoFiles)
	{
		// When running the in-process repository, the first GlobalColumnInfoProvider in the list
		// will be TableBasedColumnInfoProvider; that one will be used to read the column infos,
		// and DataModelManager will only be used to write the in-memory column infos to dbi files.
		// In this case we want to ignore file change events & reloads by the DataModelManager (they would make
		// the use of TableBasedColumnInfoProvider fail because the column info would be changed / id's messed up and so on)
		// As TableBasedColumnInfoProvider and ColumnInfoBasedSequenceProvider are used when running in-process repository
		// we use the sequence provider to identify the case
		IServerManagerInternal serverManager = getServerManager();
		if (serverManager == null || serverManager.getGlobalSequenceProvider() instanceof IColumnInfoBasedSequenceProvider || dataModelManager == null)
		{
			return false;
		}

		boolean columnInfoChanged = false;
		boolean tableOrServerAreNotFound;

		for (IResourceDelta fileRd : columnInfoFiles)
		{
			if (fileRd.getKind() == IResourceDelta.CHANGED && fileRd.getFlags() == IResourceDelta.MARKERS) continue; // this means only markers have changed... (flags is a bit mask) - so we are not interested
			final IFile file = (IFile)fileRd.getResource();
			IContainer serverContainer = file.getParent();
			if (serverContainer != null && serverContainer.getParent() != null &&
				serverContainer.getParent().getName().equals(SolutionSerializer.DATASOURCES_DIR_NAME) &&
				serverContainer.getParent().getParent() instanceof IProject)
			{
				String serverName = serverContainer.getName();
				tableOrServerAreNotFound = true;
				IServer s = serverManager.getServer(serverName);
				String tableName = file.getName().substring(0, file.getName().length() - DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT.length());
				if (s != null && s instanceof IServerInternal)
				{
					try
					{
						IServerInternal server = (IServerInternal)s;
						if (server.hasTable(tableName))
						{
							tableOrServerAreNotFound = false;
							if (!dataModelManager.isWritingMarkerFreeDBIFile(file) &&
								!tableName.toUpperCase().startsWith(DataModelManager.TEMP_UPPERCASE_PREFIX))
							{
								Table table = server.getTable(tableName);
								columnInfoChanged = true;
								dataModelManager.loadAllColumnInfo(table);
							}
						}
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
				}

				// check/update "missing table/missing dbi" marker states
				if (tableOrServerAreNotFound) dataModelManager.updateMarkerStatesForMissingTable(fileRd, serverName, tableName);
			}
		}
		return columnInfoChanged;
	}

	/**
	 * @return filesAddedOrRemoved
	 */
	private boolean handleChangedStringResources(int objectTypeId, List<IResourceDelta> changedFiles, List<IPersist> modifiedResourcesList)
	{
		boolean filesAddedOrRemoved = false;
		for (IResourceDelta fileRd : changedFiles)
		{
			final IFile file = (IFile)fileRd.getResource();
			int dot = file.getName().lastIndexOf('.');
			if (dot <= 0) continue;
			String name = file.getName().substring(0, dot);
			StringResource resource = (StringResource)getActiveRootObject(name, objectTypeId);
			if (fileRd.getKind() == IResourceDelta.CHANGED)
			{
				File f = new File(file.getLocationURI());
				if (resource != null)
				{
					resource.loadFromFile(f);
					resource.updateLastModifiedTime();
					resource.clearChanged();
					modifiedResourcesList.add(resource);
				}
				else
				{
					ServoyLog.logWarning("A resource file was modified, but no corresponding Servoy object was found: " + file, null);
				}
			}
			else if (fileRd.getKind() == IResourceDelta.ADDED)
			{
				filesAddedOrRemoved = true;
				// we have 3 cases here - when the file was written by the serializer (in this case the
				// resource is complete and may or may not be part of the repository) and when the file was created normally (in
				// this case we must add the new resource to the repository and serialize it - to store meta data (.obj file does not exist))
				if (resource == null)
				{
					try
					{
						final EclipseRepository repository = (EclipseRepository)getDeveloperRepository();
						// see if this style already has metadata and only needs loading into the repository
						IResource metaDataFile = file.getParent().findMember(name + SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION);
						RootObjectMetaData md = null;
						if (metaDataFile instanceof IFile)
						{
							md = StringResourceDeserializer.deserializeMetadata(repository, new File(metaDataFile.getLocationURI()), objectTypeId);
						}

						if (md == null)
						{
							// obj file is missing - so create the style as a new persist and save it
							resource = (StringResource)repository.createNewRootObject(name, objectTypeId);
							modifiedResourcesList.add(resource);
							File f = new File(file.getLocationURI());
							resource.loadFromFile(f);// read the contents into memory
							resource.clearChanged();

							// we are running in a resources change listener - where workspace is locked for writes
							// so we must do the write later when we are allowed
							final StringResource res = resource;
							WorkspaceJob writeObjJob = new WorkspaceJob("Updating string persist")
							{
								@Override
								public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
								{
									try
									{
										repository.updateRootObject(res);
										file.getProject().refreshLocal(IResource.DEPTH_INFINITE, null); // as a new file could have been added with java.io - refresh the workspace resource
									}
									catch (RepositoryException e)
									{
										ServoyLog.logError("While trying to add a new manually created text file to the repository, an exception occured", e);
									}
									catch (CoreException e)
									{
										ServoyLog.logError(
											"While trying to refresh resources project after manually creating text file + adding metadata, an exception occured",
											e);
									}
									return Status.OK_STATUS;
								}
							};
							writeObjJob.setUser(false);
							writeObjJob.setSystem(true);
							writeObjJob.schedule();
						}
						else
						{
							// the style is there, only needs to be loaded into the repository
							repository.addRootObjectMetaData(md);
							modifiedResourcesList.add(repository.getActiveRootObject(md.getRootObjectId()));
						}
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError("While trying to add a new manually created text file to the repository, an exception occured", e);
					}
				}
			}
			else if (fileRd.getKind() == IResourceDelta.REMOVED)
			{
				filesAddedOrRemoved = true;
				if (resource != null)
				{
					EclipseRepository repository = (EclipseRepository)getDeveloperRepository();
					try
					{
						modifiedResourcesList.add(resource);
						repository.removeRootObject(resource.getID());
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError("Exception while trying to remove style from repository", e);
					}
				}
			}
		}
		return filesAddedOrRemoved;
	}

	/**
	 * @param affectedChildren
	 * @param al
	 */
	public static List<IResourceDelta> findChangedFiles(IResourceDelta parent, List<IResourceDelta> al)
	{
		IResourceDelta[] affectedChildren = parent.getAffectedChildren();
		for (IResourceDelta child : affectedChildren)
		{
			if (child.getResource() instanceof IFile)
			{
				if (child.getKind() != IResourceDelta.CHANGED ||
					(child.getKind() == IResourceDelta.CHANGED && (child.getFlags() & IResourceDelta.CONTENT) != 0))
				{
					al.add(child);
				}
			}
			else
			{
				findChangedFiles(child, al);
			}
		}
		return al;
	}

	@Override
	public synchronized FlattenedSolution getFlattenedSolution()
	{
		return super.getFlattenedSolution();
	}

	public IValidateName getNameValidator()
	{
		if (nameValidator == null)
		{
			if (getActiveProject() != null)
			{
				nameValidator = new ScriptNameValidator(getActiveProject().getEditingFlattenedSolution());
			}
			else
			{
				nameValidator = new ScriptNameValidator();
			}
		}
		return nameValidator;
	}

	/**
	 * Initializes the ServoyModel's initial state.
	 */
	public void initialize()
	{
		getUserManager().setFormAndTableChangeAware();
		// first auto select active project
		autoSelectActiveProjectIfNull(false);
		// then start listen to changes. Else a deadlock can happen
		postChangeListener = new IResourceChangeListener()
		{
			public void resourceChanged(final IResourceChangeEvent event)
			{
				getResourceChangesHandlerCounter().increment();
				try
				{
					resourcesPostChanged(event);
				}
				finally
				{
					getResourceChangesHandlerCounter().decrement();
				}
			}
		};
		preChangeListener = new ModelPreChangeListener(this);
		getWorkspace().addResourceChangeListener(postChangeListener, IResourceChangeEvent.POST_CHANGE);
		getWorkspace().addResourceChangeListener(preChangeListener, IResourceChangeEvent.PRE_CLOSE);
		getWorkspace().addResourceChangeListener(preChangeListener, IResourceChangeEvent.PRE_DELETE);

		getResourceChangesHandlerCounter().addValueListener(new AtomicIntegerWithListener.IValueListener()
		{
			// handle outstanding files immediately (not using Display.getDefault().asyncExec), callers may depend on them being processed
			public void valueSetToZero()
			{
				handleOutstandingChangedFiles(null);
			}
		});
	}

	/**
	 * @param persist
	 * @return
	 */
	public FlattenedSolution getEditingFlattenedSolution(IPersist persist)
	{
		return ModelUtils.getEditingFlattenedSolution(persist);
	}

	/*
	 * Flush data providers for a table on all flattened solutions
	 */
	public synchronized void flushDataProvidersForTable(Table table)
	{
		getFlattenedSolution().flushDataProvidersForTable(table);
		for (ServoyProject servoyProject : getModulesOfActiveProject())
		{
			servoyProject.getEditingFlattenedSolution().flushDataProvidersForTable(table);
		}
	}

	/*
	 * Flush all cashed data on all flattened solutions
	 */
	public synchronized void flushAllCachedData()
	{
		getFlattenedSolution().flushAllCachedData();
		for (ServoyProject servoyProject : getModulesOfActiveProject())
		{
			servoyProject.getEditingFlattenedSolution().flushAllCachedData();
		}
	}

	public void dispose()
	{
		// TODO add more cleanup to this method
		removeActiveProjectListener(backgroundTableLoader);
		getServerManager().removeServerConfigListener(serverConfigSyncer);
		getWorkspace().removeResourceChangeListener(preChangeListener);
		getWorkspace().removeResourceChangeListener(postChangeListener);

		for (String server_name : getServerManager().getServerNames(false, false, true, true))
		{
			((IServerInternal)getServerManager().getServer(server_name, false, false)).removeTableListener(tableListener);
		}
		getServerManager().removeServerListener(serverTableListener);
	}

	public void testBuildPathsAndBuild(final ServoyProject project, final boolean buildProject)
	{
		WorkspaceJob testBuildPaths = new WorkspaceJob("Test Build Paths")
		{
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				testBuildPaths(project, new HashSet<ServoyProject>());
				if (buildProject) buildActiveProjectsInJob();
				return Status.OK_STATUS;
			}
		};
		testBuildPaths.setRule(getWorkspace().getRoot());
		testBuildPaths.schedule();
	}

	private IProject[] getAllReferencedProjectsOfActiveProject()
	{
		Set<IProject> allModuleProjects = new HashSet<IProject>();
		ServoyProject[] modules = getModulesOfActiveProject();

		List<ServoyProject> importHookModulesToBeIgnored = new ArrayList<ServoyProject>();
		addImportHookModules(getActiveProject(), importHookModulesToBeIgnored);
		if (importHookModulesToBeIgnored.contains(getActiveProject())) importHookModulesToBeIgnored.clear();

		HashSet<String> importHookModuleNamesToBeIgnored = new HashSet<String>(importHookModulesToBeIgnored.size());
		for (ServoyProject p : importHookModulesToBeIgnored)
		{
			importHookModuleNamesToBeIgnored.add(p.getProject().getName());
		}

		for (ServoyProject spm : modules)
		{
			Solution s = spm.getSolution();
			if (s != null)
			{
				String[] moduleNames = Utils.getTokenElements(s.getModulesNames(), ",", true);
				for (String module : moduleNames)
				{
					IProject tmp = ResourcesPlugin.getWorkspace().getRoot().getProject(module);
					if (tmp != null && !importHookModuleNamesToBeIgnored.contains(tmp.getName()))
					{
						allModuleProjects.add(tmp);
					}
				}
			}
		}

		if (activeProject != null)
		{
			allModuleProjects.add(activeProject.getProject());
			if (activeProject.getResourcesProject() != null) allModuleProjects.add(activeProject.getResourcesProject().getProject());
		}

		return allModuleProjects.toArray(new IProject[allModuleProjects.size()]);
	}

	public void updateWorkingSet()
	{
		WorkspaceJob updateServoyWorkingSet = new WorkspaceJob("Servoy active solution workingset updater")
		{
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				if (getActiveProject() != null)
				{
					IAdaptable[] projects = getAllReferencedProjectsOfActiveProject();

					IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
					IWorkingSet ws = workingSetManager.getWorkingSet("Servoy Active Solution");
					if (ws == null)
					{
						ws = workingSetManager.createWorkingSet("Servoy Active Solution", projects);
						workingSetManager.addWorkingSet(ws);
					}
					else
					{
						ws.setElements(projects);
					}
					final IWorkingSet[] wsa = new IWorkingSet[1];
					wsa[0] = ws;
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
//						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().setWorkingSets(null);
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().setWorkingSets(wsa);
						}
					});
				}
				return Status.OK_STATUS;
			}
		};
		updateServoyWorkingSet.schedule();
	}

	public MultiTextEdit getScriptFileChanges(IPersist savedPersist, final IFile scriptFile)
	{
		ISupportChilds parent = savedPersist.getParent();
		String scopeName = null;
		if (savedPersist instanceof ISupportScope)
		{
			scopeName = ((ISupportScope)savedPersist).getScopeName();
		}

		MultiTextEdit textEdit = new MultiTextEdit();

		Script parse = JavaScriptParserUtil.parse(DLTKCore.createSourceModuleFrom(scriptFile));
		List<Statement> statements = parse.getStatements();
		for (Statement statement : statements)
		{
			if (statement instanceof VoidExpression)
			{
				IPersist persist = null;
				Expression expression = ((VoidExpression)statement).getExpression();
				if (expression instanceof VariableStatement)
				{
					VariableDeclaration decl = ((VariableStatement)expression).getVariables().get(0);

					if (parent instanceof Form)
					{
						persist = ((Form)parent).getScriptVariable(decl.getVariableName());
					}
					else if (parent instanceof Solution && scopeName != null)
					{
						persist = ((Solution)parent).getScriptVariable(scopeName, decl.getVariableName());
					}
				}
				else if (expression instanceof FunctionStatement)
				{
					String name = ((FunctionStatement)expression).getName().getName();
					if (parent instanceof Form)
					{
						persist = ((Form)parent).getScriptMethod(name);
					}
					else if (parent instanceof Solution && scopeName != null)
					{
						persist = ((Solution)parent).getScriptMethod(scopeName, name);
					}
					else if (parent instanceof TableNode)
					{
						persist = ((TableNode)parent).getScriptCalculation(name);
						if (persist == null)
						{
							persist = ((TableNode)parent).getFoundsetMethod(name);
						}
					}
				}
				if (persist != null)
				{
					Comment documentation = expression.getDocumentation();

					JSDocScriptTemplates prefs = JSDocScriptTemplates.getTemplates(scriptFile.getProject(), true);
					String userTemplate = (persist instanceof IVariable) ? prefs.getVariableTemplate() : prefs.getMethodTemplate();
					String comment = null;
					try
					{
						comment = SolutionSerializer.getComment(persist, userTemplate, getDeveloperRepository());
					}
					catch (RuntimeException e)
					{
					}
					if (comment == null) continue;
					if (documentation == null || !documentation.getText().equals(comment.trim()))
					{
						// if the jsdoc didn't match make sure that the persist is flagged as changed, because it needs to be regenerated.
						persist.flagChanged();
						if (documentation == null)
						{
							if (!comment.endsWith("\n")) comment += "\n";
							textEdit.addChild(new InsertEdit(statement.sourceStart(), comment));
						}
						else
						{
							textEdit.addChild(new ReplaceEdit(documentation.sourceStart(), documentation.sourceEnd() - documentation.sourceStart(),
								comment.trim()));
						}
					}
				}

			}
		}
		return textEdit;
	}

	private final List<File> ignoreOnceFiles = Collections.synchronizedList(new ArrayList<File>());

	/**
	 * @param file
	 */
	public void addIgnoreFile(File file)
	{
		ignoreOnceFiles.add(file);
	}

	public ClientSupport getActiveSolutionClientType()
	{
		ServoyProject aProject = getActiveProject();
		if (aProject == null)
		{
			return null;
		}

		Solution solution = aProject.getSolution();
		if (solution == null)
		{
			return null;
		}

		switch (solution.getSolutionType())
		{
			case SolutionMetaData.MOBILE :
				return ClientSupport.mc;

			case SolutionMetaData.WEB_CLIENT_ONLY :
				return ClientSupport.wc;

			case SolutionMetaData.SMART_CLIENT_ONLY :
				return ClientSupport.sc;

			case SolutionMetaData.NG_CLIENT_ONLY :
				return ClientSupport.ng;
		}

		return ClientSupport.Default;
	}

	public boolean isActiveSolutionWeb()
	{
		if (getActiveSolutionClientType() != null) return getActiveSolutionClientType().supports(ClientSupport.wc);
		else return false;
	}


	public boolean isActiveSolutionMobile()
	{
		if (getActiveSolutionClientType() != null) return getActiveSolutionClientType().supports(ClientSupport.mc);
		else return false;
	}

	public boolean isActiveSolutionNGClient()
	{
		if (getActiveSolutionClientType() != null) return getActiveSolutionClientType().supports(ClientSupport.ng);
		else return false;
	}

	/**
	 * @return
	 */
	public boolean isActiveSolutionSmartClient()
	{
		if (getActiveSolutionClientType() != null) return getActiveSolutionClientType().supports(ClientSupport.sc);
		else return false;
	}


	private void readWorkingSetsFromResourcesProject()
	{
		PlatformUI.getWorkbench().getWorkingSetManager().removePropertyChangeListener(workingSetChangeListener);
		if (activeResourcesProject != null)
		{
			IFileAccess wsa = new WorkspaceFileAccess(getWorkspace());
			Map<String, List<String>> servoyWorkingSets = SolutionDeserializer.deserializeWorkingSets(wsa, activeResourcesProject.getProject().getName());
			if (servoyWorkingSets != null)
			{
				for (String workingSetName : servoyWorkingSets.keySet())
				{
					IWorkingSet workingSet = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(workingSetName);
					if (workingSet == null)
					{
						workingSet = PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(workingSetName, new IResource[0]);
						workingSet.setId(SERVOY_WORKING_SET_ID);
						PlatformUI.getWorkbench().getWorkingSetManager().addWorkingSet(workingSet);
					}
					List<String> paths = servoyWorkingSets.get(workingSet.getName());
					List<IResource> resources = new ArrayList<IResource>();
					if (paths != null)
					{
						Iterator<String> it = paths.iterator();
						while (it.hasNext())
						{
							String path = it.next();
							IResource resource = getWorkspace().getRoot().getFile(new Path(path));
							resources.add(resource);
							if (!resource.exists())
							{
								it.remove();
							}
						}
					}
					workingSet.setElements(resources.toArray(new IResource[0]));
				}
			}

			IWorkingSet[] workingSets = PlatformUI.getWorkbench().getWorkingSetManager().getAllWorkingSets();
			if (workingSets != null)
			{
				for (IWorkingSet workingSet : workingSets)
				{
					if (workingSet.getId() != null && workingSet.getId().equals(SERVOY_WORKING_SET_ID))
					{
						if (!servoyWorkingSets.containsKey(workingSet.getName()))
						{
							PlatformUI.getWorkbench().getWorkingSetManager().removeWorkingSet(workingSet);
						}
					}
				}
			}
			activeResourcesProject.refreshServoyWorkingSets(servoyWorkingSets);
		}
		PlatformUI.getWorkbench().getWorkingSetManager().addPropertyChangeListener(workingSetChangeListener);
	}

	private void writeServoyWorkingSets(PropertyChangeEvent event)
	{
		if (activeResourcesProject != null &&
			(event.getOldValue() instanceof IWorkingSet && SERVOY_WORKING_SET_ID.equals(((IWorkingSet)event.getOldValue()).getId())) ||
			(event.getNewValue() instanceof IWorkingSet && SERVOY_WORKING_SET_ID.equals(((IWorkingSet)event.getNewValue()).getId())))
		{
			IFileAccess wsa = new WorkspaceFileAccess(getWorkspace());
			if (IWorkingSetManager.CHANGE_WORKING_SET_REMOVE.equals(event.getProperty()))
			{
				if (event.getOldValue() instanceof IWorkingSet && SERVOY_WORKING_SET_ID.equals(((IWorkingSet)event.getOldValue()).getId()))
				{
					activeResourcesProject.removeWorkingSet(wsa, ((IWorkingSet)event.getOldValue()).getName());
				}
			}
			else if (event.getNewValue() instanceof IWorkingSet && SERVOY_WORKING_SET_ID.equals(((IWorkingSet)event.getNewValue()).getId()))
			{
				if (IWorkingSetManager.CHANGE_WORKING_SET_NAME_CHANGE.equals(event.getProperty()))
				{
					Set<String> workingSetNames = activeResourcesProject.getWorkingSetNames();
					String oldName = null;
					for (String workingSetName : workingSetNames)
					{
						IWorkingSet workingSet = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(workingSetName);
						if (workingSet == null)
						{
							oldName = workingSetName;
							break;
						}
					}
					if (oldName != null)
					{
						activeResourcesProject.renameWorkingSet(wsa, oldName, ((IWorkingSet)event.getNewValue()).getName());
					}
				}
				else
				{
					IWorkingSet workingSet = (IWorkingSet)event.getNewValue();
					List<String> paths = new ArrayList<String>();
					IAdaptable[] resources = workingSet.getElements();
					if (resources != null)
					{
						for (IAdaptable resource : resources)
						{
							if (resource instanceof IResource && ((IResource)resource).exists())
							{
								paths.add(((IResource)resource).getFullPath().toString());
							}
						}
					}
					activeResourcesProject.addWorkingSet(wsa, workingSet.getName(), paths);
				}
			}
		}
	}

	public void addWorkingSetChangedListener(IWorkingSetChangedListener workingSetChangedListener)
	{
		if (!workingSetChangedListeners.contains(workingSetChangedListener))
		{
			this.workingSetChangedListeners.add(workingSetChangedListener);
		}
		if (activeResourcesProject != null)
		{
			activeResourcesProject.setListeners(workingSetChangedListeners);
		}
	}

	public void removeWorkingSetChangedListener(IWorkingSetChangedListener workingSetChangedListener)
	{
		this.workingSetChangedListeners.remove(workingSetChangedListener);
		if (activeResourcesProject != null)
		{
			activeResourcesProject.removeListener(workingSetChangedListener);
		}
	}


}
