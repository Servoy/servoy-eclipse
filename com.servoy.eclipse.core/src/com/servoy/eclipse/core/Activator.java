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

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import net.sourceforge.sqlexplorer.ExplorerException;
import net.sourceforge.sqlexplorer.dbproduct.Alias;
import net.sourceforge.sqlexplorer.dbproduct.AliasManager;
import net.sourceforge.sqlexplorer.dbproduct.ManagedDriver;
import net.sourceforge.sqlexplorer.dbproduct.User;
import net.sourceforge.sqlexplorer.plugin.SQLExplorerPlugin;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.IActivityManager;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.cheatsheets.OpenCheatSheetAction;
import org.eclipse.ui.internal.Perspective;
import org.eclipse.ui.internal.ViewIntroAdapterPart;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.registry.ActionSetRegistry;
import org.eclipse.ui.internal.registry.IActionSetDescriptor;
import org.eclipse.ui.progress.WorkbenchJob;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

import com.servoy.eclipse.core.doc.IDocumentationManagerProvider;
import com.servoy.eclipse.core.resource.FormDescriber;
import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.JSUnitUserManager;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.ClientState;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBeanManager;
import com.servoy.j2db.IBeanManagerInternal;
import com.servoy.j2db.IBrowserLauncher;
import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.IDebugJ2DBClient;
import com.servoy.j2db.IDebugWebClient;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.debug.DebugUtils;
import com.servoy.j2db.debug.RemoteDebugScriptEngine;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IMethodTemplate;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.MethodTemplate;
import com.servoy.j2db.persistence.MethodTemplatesFactory;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.plugins.IMethodTemplatesProvider;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.server.shared.IDebugHeadlessClient;
import com.servoy.j2db.smart.plugins.PluginManager;
import com.servoy.j2db.util.CompositeIterable;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin
{

	private volatile boolean defaultAccessed = false;

	private Boolean sqlExplorerLoaded = null;

	private DesignApplication designClient;

	private IDesignerCallback designerCallback;

	/**
	 * @author jcompagner
	 * 
	 */
	private static final class SQLExplorerAliasCreatorJob extends WorkbenchJob
	{
		/**
		 * @param name
		 */
		private SQLExplorerAliasCreatorJob(String name)
		{
			super(name);
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor)
		{
			IServerManagerInternal serverManager = ServoyModel.getServerManager();

			String[] serverNames = serverManager.getServerNames(true, true, false, false);
			AliasManager aliasManager = SQLExplorerPlugin.getDefault().getAliasManager();

			try
			{
				aliasManager.loadAliases();
			}
			catch (ExplorerException e1)
			{
				ServoyLog.logError(e1);
			}

			for (String serverName : serverNames)
			{
				Alias alias = new Alias(serverName)
				{
					ManagedDriver driver = new ManagedDriver(getName())
					{
						@Override
						public net.sourceforge.sqlexplorer.dbproduct.SQLConnection getConnection(User user) throws java.sql.SQLException
						{
							IServerInternal server = (IServerInternal)ServoyModel.getServerManager().getServer(getId());
							try
							{
								return new net.sourceforge.sqlexplorer.dbproduct.SQLConnection(user, server.getRawConnection(), this, "Servoy server: " + //$NON-NLS-1$
									getId());
							}
							catch (RepositoryException e)
							{
								throw new SQLException(e.getMessage());
							}
						}
					};

					/**
					 * @see net.sourceforge.sqlexplorer.dbproduct.Alias#getDriver()
					 */
					@Override
					public ManagedDriver getDriver()
					{
						return driver;
					}
				};
				alias.setAutoLogon(true);
				alias.setConnectAtStartup(false);
				alias.setHasNoUserName(true);
				try
				{
					aliasManager.addAlias(alias);
				}
				catch (ExplorerException e)
				{
					ServoyLog.logError(e);
				}
			}
			try
			{
				aliasManager.saveAliases();
			}
			catch (ExplorerException e)
			{
				ServoyLog.logError(e);
			}
			return Status.OK_STATUS;
		}
	}

	// The plug-in ID
	public static final String PLUGIN_ID = "com.servoy.eclipse.core"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private IDocumentationManagerProvider docManagerpProvider;

	private IDebuggerStarter debuggerStarter;

	/**
	 * The constructor
	 */
	public Activator()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception
	{
		super.start(context);
		plugin = this;

		IPreferenceStore prefs = PlatformUI.getPreferenceStore();
		prefs.setValue(IWorkbenchPreferenceConstants.SHOW_PROGRESS_ON_STARTUP, true);

		Dictionary<String, String[]> properties = new Hashtable<String, String[]>(1);
		properties.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] { MediaURLStreamHandlerService.PROTOCOL });
		String serviceClass = URLStreamHandlerService.class.getName();
		context.registerService(serviceClass, new MediaURLStreamHandlerService(), properties);

		// We need to hook a listener and detect when the Welcome page is closed.
		// (And for that we need to hook another listener to detect when the workbench window is opened).
		if (ModelUtils.isUIRunning()) PlatformUI.getWorkbench().addWindowListener(new IWindowListener()
		{
			public void windowActivated(IWorkbenchWindow window)
			{
			}

			public void windowClosed(IWorkbenchWindow window)
			{
			}

			public void windowDeactivated(IWorkbenchWindow window)
			{
			}

			public void windowOpened(IWorkbenchWindow window)
			{
				/* Remove redundant activities to reduce UI clutter. */
				String[] activityIds = { "com.servoy.eclipse.activities.javaDevelopment", "org.eclipse.team.cvs", "org.eclipse.antDevelopment", "org.eclipse.javaDevelopment", "org.eclipse.plugInDevelopment", "com.servoy.eclipse.activities.html", "com.servoy.eclipse.activities.xml", "com.servoy.eclipse.activities.dltk", "com.servoy.eclipse.activities.edit", "org.eclipse.equinox.p2.ui.sdk.classicUpdate" };
				IWorkbenchActivitySupport was = PlatformUI.getWorkbench().getActivitySupport();
				IActivityManager wasAM = was.getActivityManager();
				List<String> activitiesToDisable = Arrays.asList(activityIds);
				Set<String> keepEnabled = new HashSet<String>();
				for (Object o : wasAM.getDefinedActivityIds())
				{
					String id = (String)o;
					if (!activitiesToDisable.contains(id)) keepEnabled.add(id);
				}
				was.setEnabledActivityIds(keepEnabled);

				/* Remove the run/debug actions */
				String[] actionIds = { "org.eclipse.debug.ui.launchActionSet" };
				ActionSetRegistry reg = WorkbenchPlugin.getDefault().getActionSetRegistry();
				IActionSetDescriptor[] actionSets = reg.getActionSets();
				for (IActionSetDescriptor element : actionSets)
				{
					for (String actionSetId : actionIds)
					{
						if (Utils.stringSafeEquals(element.getId(), actionSetId))
						{
							IExtension ext = element.getConfigurationElement().getDeclaringExtension();
							reg.removeExtension(ext, new Object[] { element });
						}
					}
				}

				/* Hide the Enxternal Tools set */
				final IEclipsePreferences eclipsePref = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
				final Preferences node = eclipsePref.node("perspectivesAlreadyActivated"); //the activated perspectives will be stored in this node
				final IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

				try
				{
					if (node.keys().length == 0)
					{
						//hide ExternalToolsSet if first time startup
						WorkbenchPage workbenchPage = (WorkbenchPage)workbenchWindow.getActivePage();
						workbenchPage.hideActionSet("org.eclipse.ui.externaltools.ExternalToolsSet");

						//remove ExternalToolsSet from current perspective - if a restart occurs, the action set has to remain removed
						IPerspectiveDescriptor perspectiveDescriptor = workbenchPage.getPerspective();
						if (perspectiveDescriptor != null)
						{
							turnOffExternalToolsActionSet(workbenchWindow, perspectiveDescriptor, node);
						}
					}
				}
				catch (BackingStoreException e)
				{
					ServoyLog.logError("Failed to access node keys.", e);
				}

				//add perspective activated listener to remove External Tools set from any activated perspective
				workbenchWindow.addPerspectiveListener(new PerspectiveAdapter()
				{

					@Override
					public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspectiveDescriptor)
					{
						if (node.getBoolean(perspectiveDescriptor.getId(), true))
						{
							super.perspectiveActivated(page, perspectiveDescriptor);
							turnOffExternalToolsActionSet(workbenchWindow, perspectiveDescriptor, node);
						}
					}
				});


				try
				{
					if (!ApplicationServerSingleton.get().hasDeveloperLicense() ||
						Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.developer.showStartPage", "true")))
					{
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(StartPageBrowserEditor.INPUT,
							StartPageBrowserEditor.STARTPAGE_BROWSER_EDITOR_ID);
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError("Failed to open browser editor.", e); //$NON-NLS-1$
				}
				window.getPartService().addPartListener(new IPartListener()
				{
					public void partActivated(IWorkbenchPart part)
					{
					}

					public void partBroughtToTop(IWorkbenchPart part)
					{
					}

					public void partClosed(IWorkbenchPart part)
					{
						if (part instanceof ViewIntroAdapterPart)
						{
//							showFirstCheatSheet();
						}
					}

					public void partDeactivated(IWorkbenchPart part)
					{
					}

					public void partOpened(IWorkbenchPart part)
					{
					}
				});
			}
		});
	}

	private void turnOffExternalToolsActionSet(IWorkbenchWindow workbenchWindow, IPerspectiveDescriptor perspectiveDescriptor, Preferences node)
	{
		if (workbenchWindow.getActivePage() instanceof WorkbenchPage)
		{
			WorkbenchPage worbenchPage = (WorkbenchPage)workbenchWindow.getActivePage();
			Perspective perspective = worbenchPage.findPerspective(perspectiveDescriptor);
			ArrayList<IActionSetDescriptor> toRemove = new ArrayList<IActionSetDescriptor>();
			if (perspective != null)
			{
				ActionSetRegistry reg = WorkbenchPlugin.getDefault().getActionSetRegistry();
				IActionSetDescriptor[] actionSets = reg.getActionSets();
				for (IActionSetDescriptor actionSetDescriptor : actionSets)
				{
					if (actionSetDescriptor.getId().indexOf("org.eclipse.ui.externaltools.ExternalToolsSet") > -1)
					{
						toRemove.add(actionSetDescriptor);
						break;
					}
				}
				perspective.turnOffActionSets(toRemove.toArray(new IActionSetDescriptor[toRemove.size()]));
			}
		}
		node.putBoolean(perspectiveDescriptor.getId(), false);
		try
		{
			node.flush();
		}
		catch (BackingStoreException e)
		{
			ServoyLog.logError("Failed to persist changes.", e);
		}
	}

	public boolean isSqlExplorerLoaded()
	{
		if (sqlExplorerLoaded == null)
		{
			sqlExplorerLoaded = Boolean.FALSE;
			try
			{
				Class.forName("net.sourceforge.sqlexplorer.plugin.SQLExplorerPlugin", false, getClass().getClassLoader()); //$NON-NLS-1$
				sqlExplorerLoaded = Boolean.TRUE;
				generateSQLExplorerAliasses();
			}
			catch (Exception e)
			{
				// ignore
			}
		}
		return sqlExplorerLoaded.booleanValue();
	}

	/**
	 * 
	 */
	private void generateSQLExplorerAliasses()
	{
		WorkbenchJob job = new SQLExplorerAliasCreatorJob("creating db aliasses"); //$NON-NLS-1$
		job.setSystem(true);
		job.setUser(false);
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.schedule();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception
	{
		defaultAccessed = false;
		if (ServoyModelManager.getServoyModelManager().isServoyModelCreated())
		{
			ServoyModelManager.getServoyModelManager().getServoyModel().dispose();
		}

		plugin = null;
		super.stop(context);
		if (ModelUtils.isUIRunning())
		{
			IApplication debugReadyClient = getDebugClientHandler().getDebugReadyClient();
			if (debugReadyClient != null && debugReadyClient.getScriptEngine() instanceof RemoteDebugScriptEngine &&
				((RemoteDebugScriptEngine)debugReadyClient.getScriptEngine()).isAWTSuspendedRunningScript())
			{
				((RemoteDebugScriptEngine)debugReadyClient.getScriptEngine()).getDebugger().close();
			}

			// shutdown all non-swing clients; no need to run this in AWT EDT
			try
			{
				List<ClientState> nonSwingApps = getDebugClientHandler().getActiveDebugClients();
				for (ClientState application : nonSwingApps)
				{
					ClientInfo ci = application.getClientInfo();
					if (ci != null && !Utils.isSwingClient(ci.getApplicationType())) application.shutDown(true);
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}

			// shutdown swing clients
			boolean interrupted = false;
			try
			{
				Runnable run = new Runnable()
				{
					public void run()
					{
						try
						{
							List<ClientState> swingApps = getDebugClientHandler().getActiveDebugClients();
							for (ClientState application : swingApps)
							{
								ClientInfo ci = application.getClientInfo();
								if (ci != null && Utils.isSwingClient(ci.getApplicationType())) application.shutDown(true);
							}
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
				};
				if (Utils.isAppleMacOS())
				{
					DebugUtils.invokeAndWaitWhileDispatchingOnSWT(run);
				}
				else
				{
					SwingUtilities.invokeAndWait(run);
				}


			}
			catch (InterruptedException e)
			{
				ServoyLog.logWarning("Interrupted while waiting for clients to shut down on stop. Continuing with server shutdown.", null); //$NON-NLS-1$
				interrupted = true;
			}


			Settings.getInstance().save();

			// wait until webserver is stopped for case of
			// restart (webserver cannot re-start when port is still in use, this may even cause a freeze after restart)
			ApplicationServerSingleton.get().shutDown();

			if (interrupted) Thread.interrupted(); // someone is in a hurry, let callers know about that
		}
	}

	/**
	 * 
	 */
	public IDebugClientHandler getDebugClientHandler()
	{
		IDebugClientHandler dch = ApplicationServerSingleton.get().getDebugClientHandler();
		if (designerCallback == null)
		{
			designerCallback = new IDesignerCallback()
			{
				public void showFormInDesigner(Form form)
				{
					FlattenedSolution editingSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getEditingFlattenedSolution();
					final Form testForm = editingSolution.getForm(form.getName());
					if (testForm == null) return;
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							try
							{
								PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(
									new PersistEditorInput(testForm.getName(), testForm.getSolution().getName(), testForm.getUUID()),
									PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
										Platform.getContentTypeManager().getContentType(FormDescriber.getFormContentTypeIdentifier(testForm))).getId());
								PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().forceActive();

							}
							catch (PartInitException ex)
							{
								ServoyLog.logError(ex);
							}
						}
					});
				}

				public void addScriptObjects(ClientState client, Scriptable scope)
				{
					Context.enter();
					try
					{
						scope.put("servoyDeveloper", scope, new NativeJavaObject(scope, new JSDeveloperSolutionModel(client), new InstanceJavaMembers( //$NON-NLS-1$
							scope, JSDeveloperSolutionModel.class)));
					}
					finally
					{
						Context.exit();
					}
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see com.servoy.j2db.IDesignerCallback#testAndStartDebugger()
				 */
				public void testAndStartDebugger()
				{
					IDebuggerStarter starter = getDebuggerStarter();
					if (starter != null)
					{
						starter.testAndStartDebugger();
					}
				}
			};
			dch.setDesignerCallback(designerCallback);
		}
		return dch;
	}

	/**
	 * @return the debugJ2DBClient
	 */
	public IDebugJ2DBClient getDebugJ2DBClient()
	{
		IDebugJ2DBClient debugSmartClient = getDebugClientHandler().getDebugSmartClient();
		if (debugSmartClient.getBrowserLauncher() == null)
		{
			debugSmartClient.setBrowserLauncher(new IBrowserLauncher()
			{
				public boolean showURL(String url)
				{
					try
					{
						IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
						IWebBrowser browser = support.getExternalBrowser();
						browser.openURL(new URL(url));
						return true;
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
						return false;
					}
				}
			});
		}
		return debugSmartClient;
	}

	public IDebugJ2DBClient getJSUnitJ2DBClient()
	{
		return getDebugClientHandler().getJSUnitJ2DBClient(new JSUnitUserManager(ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager()));
	}

	/**
	 * @param form
	 */
	public void showInDebugClients(Form form)
	{
		getDebugClientHandler().showInDebugClients(form);
	}

	/**
	 * @return
	 */
	public IDebugWebClient getDebugWebClient()
	{
		return getDebugClientHandler().getDebugWebClient();
	}

	public IDebugHeadlessClient getDebugHeadlessClient()
	{
		return getDebugClientHandler().getDebugHeadlessClient();
	}

	public DesignApplication getDesignClient()
	{
		if (designClient == null)
		{
			designClient = new DesignApplication();
		}
		return designClient;
	}

	/**
	* Returns the shared instance
	* 
	* @return the shared instance
	*/
	public static Activator getDefault()
	{
		if (plugin != null && !plugin.defaultAccessed)
		{
			plugin.defaultAccessed = true;
			plugin.initialize();
		}
		return plugin;
	}

	@SuppressWarnings("restriction")
	private void initialize()
	{
		defaultAccessed = true;

		XMLScriptObjectAdapterLoader.loadCoreDocumentationFromXML();
		MethodTemplatesLoader.loadMethodTemplatesFromXML();

		// install servoy model listeners in separate job, when ServoyModel is created in bundle.activator thread
		// a deadlock may occur (display thread waits for loading of ui bundle which waits for core bundle 
		// which waits for ServoyModel latch, but the ServoyModel runnable is never running because display thread is blocking in wait)
		if (ModelUtils.isUIRunning())
		{
			new Job("HookupToServoyModel") //$NON-NLS-1$
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					final ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

					IActiveProjectListener apl = new IActiveProjectListener()
					{
						public void activeProjectChanged(final ServoyProject project)
						{
							if (designClient != null)
							{
								designClient.refreshI18NMessages();
							}
							// can this be really later? or should we wait?
							// its much nice to do that later in the event thread.
							UIUtils.invokeLaterOnAWT(new Runnable()
							{
								public void run()
								{
									IDebugClientHandler dch = getDebugClientHandler();
									if (project != null)
									{
										dch.reloadDebugSolution(project.getSolution());
										dch.reloadDebugSolutionSecurity();
									}
									else
									{
										dch.reloadDebugSolution(null);
									}
								}
							});
						}

						public void activeProjectUpdated(final ServoyProject activeProject, int updateInfo)
						{
							if (activeProject == null) return;
							if (updateInfo == IActiveProjectListener.MODULES_UPDATED)
							{
								// in order to have a good module cache in the flattened solution
								UIUtils.invokeLaterOnAWT(new Runnable()
								{
									public void run()
									{
										IDebugClientHandler dch = getDebugClientHandler();
										dch.reloadDebugSolution(activeProject.getSolution());
										dch.reloadDebugSolutionSecurity();
									}
								});
							}
							else if (updateInfo == IActiveProjectListener.RESOURCES_UPDATED_ON_ACTIVE_PROJECT)
							{
								UIUtils.invokeLaterOnAWT(new Runnable()
								{
									public void run()
									{
										IDebugClientHandler dch = getDebugClientHandler();
										dch.reloadDebugSolutionSecurity();
										dch.reloadAllStyles();
									}
								});
							}
							else if (updateInfo == IActiveProjectListener.STYLES_ADDED_OR_REMOVED)
							{
								UIUtils.invokeLaterOnAWT(new Runnable()
								{
									public void run()
									{
										IDebugClientHandler dch = getDebugClientHandler();
										dch.reloadAllStyles();
									}
								});
							}
							else if (updateInfo == IActiveProjectListener.SECURITY_INFO_CHANGED)
							{
								UIUtils.invokeLaterOnAWT(new Runnable()
								{
									public void run()
									{
										IDebugClientHandler dch = getDebugClientHandler();
										dch.reloadDebugSolutionSecurity();
									}
								});
							}
						}

						public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
						{
							return true;
						}
					};

					servoyModel.addActiveProjectListener(apl);
					apl.activeProjectChanged(servoyModel.getActiveProject());

					servoyModel.addPersistChangeListener(true, new IPersistChangeListener()
					{
						public void persistChanges(final Collection<IPersist> changes)
						{
							UIUtils.invokeLaterOnAWT(new Runnable()
							{
								public void run()
								{
									IDebugClientHandler dch = getDebugClientHandler();
									dch.refreshDebugClients(changes);
								}
							});
						}
					});

					// flush bean design instances of changed beans
					servoyModel.addPersistChangeListener(false, new IPersistChangeListener()
					{
						public void persistChanges(final Collection<IPersist> changes)
						{
							for (IPersist persist : changes)
							{
								if (persist instanceof Bean)
								{
									FlattenedSolution editingFlattenedSolution = ModelUtils.getEditingFlattenedSolution(persist);
									if (editingFlattenedSolution != null)
									{
										editingFlattenedSolution.flushBeanDesignInstance((Bean)persist);
									}
								}
							}
						}
					});

					servoyModel.addI18NChangeListener(new I18NChangeListener()
					{
						public void i18nChanged()
						{
							servoyModel.getMessagesManager().removeCachedMessages();
							UIUtils.invokeLaterOnAWT(new Runnable()
							{
								public void run()
								{
									IDebugClientHandler dch = getDebugClientHandler();
									dch.refreshDebugClientsI18N();
								}
							});
						}
					});
					return Status.OK_STATUS;
				}
			}.schedule();
		}

		// Try to load documentation XML from plugin and bean jars.
		PluginManager pluginManager = (PluginManager)getDesignClient().getPluginManager();
		IDocumentationManagerProvider documentationManagerProvider = Activator.getDefault().getDocumentationManagerProvider();
		if (documentationManagerProvider != null)
		{
			XMLScriptObjectAdapterLoader.loadDocumentationForPlugins(pluginManager, documentationManagerProvider);
			IBeanManager beanManager = getDesignClient().getBeanManager();
			if (beanManager instanceof IBeanManagerInternal)
			{
				XMLScriptObjectAdapterLoader.loadDocumentationForBeans((IBeanManagerInternal)beanManager, documentationManagerProvider);
			}
		}

		// Visit all column validators/converters and let them add any method templates to
		// MethodTemplate.
		for (Object conv : new CompositeIterable<Object>(//
			pluginManager.getColumnConverterManager().getConverters().values(),//
			pluginManager.getUIConverterManager().getConverters().values(), //
			pluginManager.getColumnValidatorManager().getValidators().values()))
		{
			if (conv instanceof IMethodTemplatesProvider)
			{
				processMethodTemplates(((IMethodTemplatesProvider)conv).getMethodTemplates(MethodTemplatesFactory.getInstance()));
			}
		}

		if (ModelUtils.isUIRunning())
		{
			String[] actionIds = { "org.eclipse.ui.edit.text.actionSet.convertLineDelimitersTo" }; //$NON-NLS-1$
			ActionSetRegistry reg = WorkbenchPlugin.getDefault().getActionSetRegistry();
			IActionSetDescriptor[] actionSets = reg.getActionSets();
			for (IActionSetDescriptor element : actionSets)
			{
				for (String actionSetId : actionIds)
				{
					if (Utils.stringSafeEquals(element.getId(), actionSetId)) element.setInitiallyVisible(false);
				}
			}
		}
	}


	private void processMethodTemplates(Map<String, IMethodTemplate> templs)
	{
		if (templs != null)
		{
			for (String key : templs.keySet())
			{
				IMethodTemplate src = templs.get(key);
				MethodTemplate.COMMON_TEMPLATES.put(key, new MethodTemplate(src));
			}
		}
	}

	public synchronized IDocumentationManagerProvider getDocumentationManagerProvider()
	{
		if (docManagerpProvider == null)
		{
			IExtensionRegistry reg = Platform.getExtensionRegistry();
			IExtensionPoint ep = reg.getExtensionPoint(IDocumentationManagerProvider.EXTENSION_ID);
			IExtension[] extensions = ep.getExtensions();

			if (extensions == null || extensions.length == 0)
			{
				ServoyLog.logWarning("Could not find documentation provider server starter plugin (extension point " + //$NON-NLS-1$
					IDocumentationManagerProvider.EXTENSION_ID + ")", null); //$NON-NLS-1$
				return null;
			}
			if (extensions.length > 1)
			{
				ServoyLog.logWarning("Multiple documentation manager plugins found (extension point " + //$NON-NLS-1$
					IDocumentationManagerProvider.EXTENSION_ID + ")", null); //$NON-NLS-1$
			}
			IConfigurationElement[] ce = extensions[0].getConfigurationElements();
			if (ce == null || ce.length == 0)
			{
				ServoyLog.logWarning("Could not read documentation provider plugin (extension point " + IDocumentationManagerProvider.EXTENSION_ID + ")", null); //$NON-NLS-1$ //$NON-NLS-2$
				return null;
			}
			if (ce.length > 1)
			{
				ServoyLog.logWarning("Multiple extensions for documentation manager plugins found (extension point " + //$NON-NLS-1$
					IDocumentationManagerProvider.EXTENSION_ID + ")", null); //$NON-NLS-1$
			}
			try
			{
				docManagerpProvider = (IDocumentationManagerProvider)ce[0].createExecutableExtension("class"); //$NON-NLS-1$
			}
			catch (CoreException e)
			{
				ServoyLog.logWarning("Could not create documentation provider plugin (extension point " + IDocumentationManagerProvider.EXTENSION_ID + ")", e); //$NON-NLS-1$ //$NON-NLS-2$
				return null;
			}
			if (docManagerpProvider == null)
			{
				ServoyLog.logWarning("Could not load documentation provider plugin (extension point " + IDocumentationManagerProvider.EXTENSION_ID + ")", null); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return docManagerpProvider;
	}

	public synchronized IDebuggerStarter getDebuggerStarter()
	{
		if (debuggerStarter == null)
		{
			debuggerStarter = (IDebuggerStarter)createExtensionPoint(IDebuggerStarter.EXTENSION_ID);
		}
		return debuggerStarter;
	}

	/**
	 * 
	 */
	public Object createExtensionPoint(String extensionId)
	{
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IExtensionPoint ep = reg.getExtensionPoint(extensionId);
		IExtension[] extensions = ep.getExtensions();

		if (extensions == null || extensions.length == 0)
		{
			ServoyLog.logWarning("Could not find extension point " + //$NON-NLS-1$
				extensionId, null);
			return null;
		}
		if (extensions.length > 1)
		{
			ServoyLog.logWarning("Multiple extensions found for " + //$NON-NLS-1$
				extensionId, null);
		}
		IConfigurationElement[] ce = extensions[0].getConfigurationElements();
		if (ce == null || ce.length == 0)
		{
			ServoyLog.logWarning("Could not read  extension point " + extensionId, null); //$NON-NLS-1$ 
			return null;
		}
		if (ce.length > 1)
		{
			ServoyLog.logWarning("Multiple extensions found for extension point " + //$NON-NLS-1$
				extensionId, null);
		}
		Object extension = null;
		try
		{
			extension = ce[0].createExecutableExtension("class"); //$NON-NLS-1$
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Could not create debug starter (extension point " + extensionId + ")", e); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
		if (extension == null)
		{
			ServoyLog.logWarning("Could not load debug starter (extension point " + extensionId + ")", null); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return extension;
	}

	private void showFirstCheatSheet()
	{
		final String cheatSheetId = "com.servoy.eclipse.ui.cheatsheet.firstcontact"; //$NON-NLS-1$
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				if (page != null)
				{
					for (IViewReference vw : page.getViewReferences())
						page.setPartState(vw, IWorkbenchPage.STATE_MINIMIZED);

					new OpenCheatSheetAction(cheatSheetId).run();

					// Make the cheat sheet view not-maximized, so that it does not fill up the entire window.
					IViewReference vw = page.findViewReference(ICheatSheetResource.CHEAT_SHEET_VIEW_ID);
					if (vw != null) page.setPartState(vw, IWorkbenchPage.STATE_RESTORED);
				}
			}
		});
	}
}
