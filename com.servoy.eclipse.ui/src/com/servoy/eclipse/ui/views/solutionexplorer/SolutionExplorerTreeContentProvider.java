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
package com.servoy.eclipse.ui.views.solutionexplorer;

import static com.servoy.j2db.util.PersistHelper.isOverrideElement;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.Icon;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.mozilla.javascript.JavaMembers;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.Package.ZipPackageReader;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.SpecReloadSubject.ISpecReloadListener;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.specification.property.ICustomType;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IDataSourceManager;
import com.servoy.eclipse.model.inmemory.AbstractMemServer;
import com.servoy.eclipse.model.inmemory.MemServer;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.ngpackages.BaseNGPackageManager.ContainerPackageReader;
import com.servoy.eclipse.model.ngpackages.IAvailableNGPackageProjectsListener;
import com.servoy.eclipse.model.ngpackages.ILoadedNGPackagesListener;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ResourcesUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.view.ViewFoundsetsServer;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.labelproviders.RelationLabelProvider;
import com.servoy.eclipse.ui.node.IDeveloperFeedback;
import com.servoy.eclipse.ui.node.SimpleDeveloperFeedback;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.TreeBuilder;
import com.servoy.eclipse.ui.node.UserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.eclipse.ui.scripting.CalculationModeHandler;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.eclipse.ui.util.IconProvider;
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.EnableServerAction;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormManager.HistoryProvider;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.dataprocessing.datasource.JSDataSources;
import com.servoy.j2db.dataprocessing.datasource.JSViewDataSource;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.documentation.IObjectDocumentation;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.documentation.XMLScriptObjectAdapter;
import com.servoy.j2db.documentation.scripting.docs.JSLib;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IMediaProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Menu;
import com.servoy.j2db.persistence.MenuItem;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.plugins.IIconProvider;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IDeprecated;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.JSApplication;
import com.servoy.j2db.scripting.JSClientUtils;
import com.servoy.j2db.scripting.JSI18N;
import com.servoy.j2db.scripting.JSSecurity;
import com.servoy.j2db.scripting.JSUnitAssertFunctions;
import com.servoy.j2db.scripting.JSUtils;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.scripting.ContainersScope;
import com.servoy.j2db.server.ngclient.scripting.WebServiceScriptable;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.StringComparator;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Content provider for the solution explorer tree.
 *
 * @author jblok
 */
public class SolutionExplorerTreeContentProvider
	implements IStructuredContentProvider, ITreeContentProvider, ILoadedNGPackagesListener, IAvailableNGPackageProjectsListener
{
	private static final String IMG_SOLUTION = "solution.png";
	private static final String IMG_SOLUTION_M = "module.png";
	private static final String IMG_SOLUTION_MODULE = "module.png";
	private static final String IMG_SOLUTION_NG_MODULE = "ng_module.png";
	private static final String IMG_SOLUTION_LOGIN = "solution_login.png";
	private static final String IMG_SOLUTION_AUTHENTICATOR = "solution_auth.png";
	private static final String IMG_SOLUTION_SMART_ONLY = "solution_smart_only.png";
	private static final String IMG_SOLUTION_WEB_ONLY = "solution_web_only.png";
	private static final String IMG_SOLUTION_NG_ONLY = "solution_ng.png";
	private static final String IMG_SOLUTION_MOBILE = "solution_mobile.png";
	private static final String IMG_SOLUTION_PREIMPORT = "solution_preimport.png";
	private static final String IMG_SOLUTION_POSTIMPORT = "solution_postimport.png";
	private static final String IMG_SOLUTION_SERVICE = "servicesolution.png";
	private static final String SERVER_IMAGE = "server.png";
	private static final String SERVER_DUPLICATE_IMAGE = "duplicate_server.png";
	private static final String SERVER_ERROR_IMAGE = "server.png_IMG_DEC_FIELD_ERROR";
	private static Map<IPath, Image> imageCache = new HashMap<IPath, Image>();

	private PlatformSimpleUserNode invisibleRootNode;

	private PlatformSimpleUserNode activeSolutionNode;

	private PlatformSimpleUserNode allSolutionsNode;

	private final PlatformSimpleUserNode allWebPackagesNode;

	private final PlatformSimpleUserNode databaseManager;

	private final PlatformSimpleUserNode solutionModel;

	private final PlatformSimpleUserNode history;

	private final PlatformSimpleUserNode servers;

	private final PlatformSimpleUserNode resources;

	private final PlatformSimpleUserNode stylesNode;

	private final PlatformSimpleUserNode componentsFromResourcesNode;

	private final PlatformSimpleUserNode servicesFromResourcesNode;

	private final PlatformSimpleUserNode templatesNode;

	private final PlatformSimpleUserNode i18nFilesNode;

	private final PlatformSimpleUserNode userGroupSecurityNode;

	private final PlatformSimpleUserNode security;

	private final PlatformSimpleUserNode i18n;

	private final PlatformSimpleUserNode apiexplorer;

	private final PlatformSimpleUserNode[] apiexplorerNodes;

	private final PlatformSimpleUserNode[] scriptingNodes;

	private final PlatformSimpleUserNode[] resourceNodes;

	private final PlatformSimpleUserNode plugins;

	private final SolutionExplorerView view;

	private Solution solutionOfCalculation;

	private ITable tableOfCalculation;

	private final PlatformSimpleUserNode modulesOfActiveSolution;

	private final Object servicesSpecProviderLock = new Object();
	private final Object pluginsLoadLock = new Object();
	private ArrayList<PlatformSimpleUserNode> loadedJavaPluginNodes;

	private final List<String> unreachableServers = new ArrayList<String>();

	private final static com.servoy.eclipse.ui.Activator uiActivator = com.servoy.eclipse.ui.Activator.getDefault();

	private final List<Image> pluginImages = new ArrayList<Image>();

	private boolean includeModules;
	private SpecProviderState componentsSpecProviderState;
	private SpecProviderState servicesSpecProviderState;

	private final ISpecReloadListener specReloadListener = new SpecReloadListener();

	private static PlatformSimpleUserNode createTypeNode(String displayName, UserNodeType type, Class< ? > realType, PlatformSimpleUserNode parent,
		boolean isJSLibNode)
	{
		PlatformSimpleUserNode node = new PlatformSimpleUserNode(displayName, type, null, IconProvider.instance().image(realType), realType,
			isJSLibNode ? new JSLibScriptObjectFeedback(null, realType) : new ScriptObjectFeedback(null, realType));
		node.parent = parent;
		return node;
	}

	SolutionExplorerTreeContentProvider(SolutionExplorerView v)
	{
		view = v;
		invisibleRootNode = new PlatformSimpleUserNode("root", UserNodeType.ARRAY);

		apiexplorer = new PlatformSimpleUserNode(Messages.TreeStrings_APIExplorer, UserNodeType.APIEXPLORER, null,
			uiActivator.loadImageFromBundle("api_explorer.png"));
		apiexplorer.parent = invisibleRootNode;

		PlatformSimpleUserNode jslib = createTypeNode(Messages.TreeStrings_JSLib, UserNodeType.JSLIB, JSLib.class, apiexplorer, true);

		jslib.children = new PlatformSimpleUserNode[] { //
			createTypeNode(Messages.TreeStrings_Array, UserNodeType.ARRAY, com.servoy.j2db.documentation.scripting.docs.Array.class, jslib, true), //
			createTypeNode(Messages.TreeStrings_Date, UserNodeType.DATE, com.servoy.j2db.documentation.scripting.docs.Date.class, jslib, true), //
			createTypeNode(Messages.TreeStrings_String, UserNodeType.STRING, com.servoy.j2db.documentation.scripting.docs.String.class, jslib, true), //
			createTypeNode(Messages.TreeStrings_Number, UserNodeType.NUMBER, com.servoy.j2db.documentation.scripting.docs.Number.class, jslib, true), //
			createTypeNode(Messages.TreeStrings_Object, UserNodeType.OBJECT, com.servoy.j2db.documentation.scripting.docs.Object.class, jslib, true), //
			createTypeNode(Messages.TreeStrings_Map, UserNodeType.MAP, com.servoy.j2db.documentation.scripting.docs.Map.class, jslib, true), //
			createTypeNode(Messages.TreeStrings_Set, UserNodeType.SET, com.servoy.j2db.documentation.scripting.docs.Set.class, jslib, true), //
			createTypeNode(Messages.TreeStrings_Iterator, UserNodeType.ITERATOR, com.servoy.j2db.documentation.scripting.docs.Iterator.class, jslib, true), //
			createTypeNode(Messages.TreeStrings_Iterablevalue, UserNodeType.ITERABELVALUE, com.servoy.j2db.documentation.scripting.docs.IterableValue.class,
				jslib, true), //
			createTypeNode(Messages.TreeStrings_Math, UserNodeType.FUNCTIONS, com.servoy.j2db.documentation.scripting.docs.Math.class, jslib, true), //
			createTypeNode(Messages.TreeStrings_RegExp, UserNodeType.REGEXP, com.servoy.j2db.documentation.scripting.docs.RegExp.class, jslib, true), //
			createTypeNode(Messages.TreeStrings_Statements, UserNodeType.STATEMENTS, com.servoy.j2db.documentation.scripting.docs.Statements.class, jslib,
				true), //
			createTypeNode(Messages.TreeStrings_SpecialOperators, UserNodeType.SPECIAL_OPERATORS,
				com.servoy.j2db.documentation.scripting.docs.SpecialOperators.class, jslib, true), //
			createTypeNode(Messages.TreeStrings_JSON, UserNodeType.JSON, com.servoy.j2db.documentation.scripting.docs.JSON.class, jslib, true), //
			createTypeNode(Messages.TreeStrings_XMLMethods, UserNodeType.XML_METHODS, com.servoy.j2db.documentation.scripting.docs.XML.class, jslib, true), //
			createTypeNode(Messages.TreeStrings_XMLListMethods, UserNodeType.XML_LIST_METHODS, com.servoy.j2db.documentation.scripting.docs.XMLList.class,
				jslib, true) };

		PlatformSimpleUserNode application = createTypeNode(Messages.TreeStrings_Application, UserNodeType.APPLICATION, JSApplication.class, apiexplorer,
			false);

		Class< ? >[] allJSApplicationTypes = ScriptObjectRegistry.getScriptObjectForClass(JSApplication.class).getAllReturnedTypes();
		List<Class< ? >> list = new ArrayList<Class< ? >>();
		for (Class< ? > cl : allJSApplicationTypes)
		{
			if (!(cl.toString().contains("APP_NG_PROPERTY") || cl.toString().contains("APP_UI_PROPERTY")))
			{
				list.add(cl);
			}
		}
		Class< ? >[] arr = new Class< ? >[list.size()];
		arr = list.toArray(arr);
		addReturnTypeNodesPlaceHolder(application, Utils.arrayJoin(arr, new ServoyException(0).getAllReturnedTypes()));

		resources = new PlatformSimpleUserNode(Messages.TreeStrings_Resources, UserNodeType.RESOURCES, null, uiActivator.loadImageFromBundle("resources.png"));
		resources.parent = invisibleRootNode;

		stylesNode = new PlatformSimpleUserNode(Messages.TreeStrings_Styles, UserNodeType.STYLES, null, uiActivator.loadImageFromBundle("styles.png"));
		stylesNode.setClientSupport(ClientSupport.wc_sc);
		stylesNode.parent = resources;

		componentsFromResourcesNode = new PlatformSimpleUserNode(Messages.TreeStrings_Components, UserNodeType.COMPONENTS_FROM_RESOURCES,
			SolutionSerializer.COMPONENTS_DIR_NAME, uiActivator.loadImageFromBundle("ng_components.png"));
		componentsFromResourcesNode.setClientSupport(ClientSupport.ng_wc_sc);
		componentsFromResourcesNode.parent = resources;

		servicesFromResourcesNode = new PlatformSimpleUserNode(Messages.TreeStrings_Services, UserNodeType.SERVICES_FROM_RESOURCES,
			SolutionSerializer.SERVICES_DIR_NAME, uiActivator.loadImageFromBundle("ng_services.png"));
		servicesFromResourcesNode.setClientSupport(ClientSupport.ng_wc_sc);
		servicesFromResourcesNode.parent = resources;

		userGroupSecurityNode = new PlatformSimpleUserNode(Messages.TreeStrings_UserGroupSecurity, UserNodeType.USER_GROUP_SECURITY, null,
			uiActivator.loadImageFromBundle("userandgroupsecurity.png"));
		userGroupSecurityNode.setClientSupport(ClientSupport.ng_wc_sc);
		userGroupSecurityNode.parent = resources;

		i18nFilesNode = new PlatformSimpleUserNode(Messages.TreeStrings_I18NFiles, UserNodeType.I18N_FILES, null, uiActivator.loadImageFromBundle("i18n.png"));
		i18nFilesNode.parent = resources;

		templatesNode = new PlatformSimpleUserNode(Messages.TreeStrings_Templates, UserNodeType.TEMPLATES, null,
			uiActivator.loadImageFromBundle("template.png"));
		templatesNode.setClientSupport(ClientSupport.ng_wc_sc);
		templatesNode.parent = resources;

		activeSolutionNode = new PlatformSimpleUserNode(Messages.TreeStrings_NoActiveSolution, UserNodeType.SOLUTION, null,
			Messages.SolutionExplorerView_activeSolution, null, uiActivator.loadImageFromBundle("solution.png"));
		activeSolutionNode.parent = invisibleRootNode;
		modulesOfActiveSolution = new PlatformSimpleUserNode(Messages.TreeStrings_Modules, UserNodeType.MODULES, null,
			uiActivator.loadImageFromBundle("modules.png"));
		modulesOfActiveSolution.parent = activeSolutionNode;

		allSolutionsNode = new PlatformSimpleUserNode(Messages.TreeStrings_AllSolutions, UserNodeType.ALL_SOLUTIONS, null,
			uiActivator.loadImageFromBundle("solutions.png"));
		allSolutionsNode.parent = invisibleRootNode;

		allWebPackagesNode = new PlatformSimpleUserNode(Messages.TreeStrings_AllWebPackageProjects, UserNodeType.ALL_WEB_PACKAGE_PROJECTS, null,
			uiActivator.loadImageFromBundle("all_packages.png"));
		allWebPackagesNode.parent = invisibleRootNode;

		databaseManager = createTypeNode(Messages.TreeStrings_DatabaseManager, UserNodeType.FOUNDSET_MANAGER, JSDatabaseManager.class, apiexplorer, false);
		addReturnTypeNodesPlaceHolder(databaseManager, ScriptObjectRegistry.getScriptObjectForClass(JSDatabaseManager.class).getAllReturnedTypes());

		PlatformSimpleUserNode utils = createTypeNode(Messages.TreeStrings_Utils, UserNodeType.UTILS, JSUtils.class, apiexplorer, false);

		PlatformSimpleUserNode clientutils = createTypeNode(Messages.TreeStrings_ClientUtils, UserNodeType.CLIENT_UTILS, JSClientUtils.class, apiexplorer,
			false);
		addReturnTypeNodesPlaceHolder(clientutils, ScriptObjectRegistry.getScriptObjectForClass(JSClientUtils.class).getAllReturnedTypes());

		PlatformSimpleUserNode jsunit = createTypeNode(Messages.TreeStrings_JSUnit, UserNodeType.JSUNIT, JSUnitAssertFunctions.class, apiexplorer, false);

		solutionModel = createTypeNode(Messages.TreeStrings_SolutionModel, UserNodeType.SOLUTION_MODEL, JSSolutionModel.class, apiexplorer, false);

		addReturnTypeNodesPlaceHolder(solutionModel, ScriptObjectRegistry.getScriptObjectForClass(JSSolutionModel.class).getAllReturnedTypes());

		history = createTypeNode(Messages.TreeStrings_History, UserNodeType.HISTORY, HistoryProvider.class, apiexplorer, false);

		security = createTypeNode(Messages.TreeStrings_Security, UserNodeType.SECURITY, JSSecurity.class, apiexplorer, false);
		addReturnTypeNodesPlaceHolder(security, ScriptObjectRegistry.getScriptObjectForClass(JSSecurity.class).getAllReturnedTypes());

		i18n = createTypeNode(Messages.TreeStrings_i18n, UserNodeType.I18N, JSI18N.class, apiexplorer, false);
		addReturnTypeNodesPlaceHolder(i18n, ScriptObjectRegistry.getScriptObjectForClass(JSI18N.class).getAllReturnedTypes());

		servers = new PlatformSimpleUserNode(Messages.TreeStrings_DBServers, UserNodeType.SERVERS, null, uiActivator.loadImageFromBundle("database_srv.png"));
		servers.parent = resources;

		plugins = new PlatformSimpleUserNode(Messages.TreeStrings_Plugins, UserNodeType.PLUGINS, null, uiActivator.loadImageFromBundle("plugins.png"));
		plugins.parent = apiexplorer;


		List<PlatformSimpleUserNode> resourcesChildren = new ArrayList<PlatformSimpleUserNode>();

		resourcesChildren.add(servers);
		resourcesChildren.add(stylesNode);
		resourcesChildren.add(userGroupSecurityNode);
		resourcesChildren.add(i18nFilesNode);
		if (templatesNode.children != null && templatesNode.children.length > 0)
		{
			resourcesChildren.add(templatesNode);
		}
		if (hasChildren(componentsFromResourcesNode))
		{
			resourcesChildren.add(componentsFromResourcesNode);
		}

		WebComponentSpecProvider.getSpecReloadSubject().addSpecReloadListener(null, specReloadListener);

		if (hasChildren(servicesFromResourcesNode))
		{
			resourcesChildren.add(servicesFromResourcesNode);
		}

		WebServiceSpecProvider.getSpecReloadSubject().addSpecReloadListener(null, specReloadListener);

		resources.children = resourcesChildren.toArray(new PlatformSimpleUserNode[0]);

		List<PlatformSimpleUserNode> rootChildren = new ArrayList<PlatformSimpleUserNode>();

		List<PlatformSimpleUserNode> apiexplorerChildren = new ArrayList<PlatformSimpleUserNode>();

		rootChildren.add(resources);
		if (hasChildren(allWebPackagesNode)) rootChildren.add(allWebPackagesNode);
		rootChildren.add(allSolutionsNode);
		rootChildren.add(activeSolutionNode);

		apiexplorerChildren.add(jslib);
		apiexplorerChildren.add(application);
		apiexplorerChildren.add(solutionModel);
		apiexplorerChildren.add(databaseManager);
		apiexplorerChildren.add(clientutils);
		apiexplorerChildren.add(utils);
		apiexplorerChildren.add(history);
		apiexplorerChildren.add(security);
		apiexplorerChildren.add(i18n);
		apiexplorerChildren.add(jsunit);
		apiexplorerChildren.add(plugins);

		apiexplorer.children = apiexplorerChildren.toArray(new PlatformSimpleUserNode[0]);

		rootChildren.add(apiexplorer);

		invisibleRootNode.children = rootChildren.toArray(new PlatformSimpleUserNode[0]);// new PlatformSimpleUserNode[] { resources, allWebPackagesNode, allSolutionsNode, activeSolutionNode, jslib, application, solutionModel, databaseManager, utils, history, security, i18n, jsunit, plugins };

		apiexplorerNodes = new PlatformSimpleUserNode[] { apiexplorer };

		scriptingNodes = new PlatformSimpleUserNode[] { jslib, application, solutionModel, databaseManager, clientutils, utils, history, security, i18n, /*
																																							 * exceptions
																																							 * ,
																																							 */jsunit, plugins };
		resourceNodes = new PlatformSimpleUserNode[] { stylesNode, userGroupSecurityNode, i18nFilesNode, templatesNode, componentsFromResourcesNode, servicesFromResourcesNode };

		Job job = Job.create("Background loading of database connections", (ICoreRunnable)monitor -> {
			addServersNodeChildren(servers);
		});
		job.setSystem(true);
		job.setPriority(Job.LONG);
		job.schedule();

		loadJavaPluginNodesInBackground();

		ServoyModelFinder.getServoyModel().getNGPackageManager().addLoadedNGPackagesListener(this);
		ServoyModelFinder.getServoyModel().getNGPackageManager().addAvailableNGPackageProjectsListener(this);
	}

	private void loadJavaPluginNodesInBackground()
	{
		// we want to load (or reload in some cases - for example if ng web services change) the plugins node in a background low prio job so that it will expand fast
		// when used...
		Job job = new Job("Background loading of java plugin nodes for Solution Explorer.")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				while (WebServiceSpecProvider.getInstance() == null)
				{
					try
					{
						Thread.sleep(500);
					}
					catch (InterruptedException e)
					{
					}
				}
				getJavaPluginsNodeChildren(plugins);
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setPriority(Job.LONG);
		job.schedule();
	}

	private SpecProviderState getComponentsSpecProviderState()
	{
		if (componentsSpecProviderState == null && WebComponentSpecProvider.isLoaded())
		{
			componentsSpecProviderState = WebComponentSpecProvider.getSpecProviderState();
		}
		return componentsSpecProviderState;
	}

	public void resetComponentsSpecProviderState()
	{
		componentsSpecProviderState = null;
	}

	private SpecProviderState getServicesSpecProviderState()
	{
		synchronized (servicesSpecProviderLock)
		{
			if (servicesSpecProviderState == null && WebServiceSpecProvider.isLoaded())
			{
				servicesSpecProviderState = WebServiceSpecProvider.getSpecProviderState();
			}

			return servicesSpecProviderState;
		}
	}

	private void resetServicesSpecProviderState()
	{
		synchronized (servicesSpecProviderLock)
		{
			servicesSpecProviderState = null;
		}
	}

	/**
	 * Returns a 'virtual' non shown root node - the parent of all visible first level nodes.
	 * Used for caching.
	 *
	 * @return the invisible root node.
	 */
	public PlatformSimpleUserNode getInvisibleRootNode()
	{
		return invisibleRootNode;
	}

	public void inputChanged(Viewer v, Object oldInput, Object newInput)
	{
		// not needed
	}

	public void dispose()
	{
		invisibleRootNode = null;
		activeSolutionNode = null;
		allSolutionsNode = null;

		if (loadedJavaPluginNodes != null) loadedJavaPluginNodes.clear();
		// dispose the (plugin) images that were allocated in SWT after conversion from Swing
		for (Image i : pluginImages)
		{
			i.dispose();
		}
		pluginImages.clear();

		ServoyModelFinder.getServoyModel().getNGPackageManager().removeLoadedNGPackagesListener(this);
		ServoyModelFinder.getServoyModel().getNGPackageManager().removeAvailableNGPackageProjectsListener(this);
		WebComponentSpecProvider.getSpecReloadSubject().removeSpecReloadListener(null, specReloadListener);
		WebServiceSpecProvider.getSpecReloadSubject().removeSpecReloadListener(null, specReloadListener);
	}

	/**
	 * Empty the references to the root node. This way, when the content provider is asked for root content - it will re-build it based on the given
	 * ServoyProject instances.
	 */
	public void flushCachedData()
	{
		activeSolutionNode.children = null;
		allSolutionsNode.children = null;
		servers.children = null;
	}

	public void setAPIExplorerNodesEnabled(boolean isEnabled)
	{
		setNodesEnabled(apiexplorerNodes, isEnabled);
	}

	public void setScriptingNodesEnabled(boolean isEnabled)
	{
		setNodesEnabled(scriptingNodes, isEnabled);
	}

	public void setResourceNodesEnabled(boolean isEnabled)
	{
		setNodesEnabled(resourceNodes, isEnabled);
	}

	private void setNodesEnabled(SimpleUserNode[] nodes, boolean isEnabled)
	{
		for (SimpleUserNode n : nodes)
		{
			boolean needsRefresh = false;
			if (isEnabled) needsRefresh = n.unhide();
			else needsRefresh = n.hide();
			if (needsRefresh)
			{
				view.refreshTreeNodeFromModel(n);
			}
		}
	}

	// called by setInput & other SWT tree related behaviors (for example during
	// filtering)
	public Object[] getElements(Object parent)
	{
		Object[] result;
		if (parent instanceof ServoyProject[])
		{
			if (allSolutionsNode.children != null)
			{
				return invisibleRootNode.children;
			}
			else
			{
				addSolutionProjects((ServoyProject[])parent);
				result = invisibleRootNode.children;
			}
		}
		else
		{
			result = getChildren(parent); // in order for the tree drill-down
			// adapter to work
		}
		return result;
	}

	private Image getServoyProjectImage(ServoyProject p, boolean isModule, boolean disabled)
	{
		String imgName = isModule ? IMG_SOLUTION_M : IMG_SOLUTION; // default is solution
		if (p != null)
		{
			SolutionMetaData s = p.getSolutionMetaData();
			if (s != null)
			{
				switch (s.getSolutionType())
				{
					case SolutionMetaData.MODULE :
						imgName = IMG_SOLUTION_MODULE;
						break;
					case SolutionMetaData.SERVICE :
						imgName = IMG_SOLUTION_SERVICE;
						break;
					case SolutionMetaData.NG_MODULE :
						imgName = IMG_SOLUTION_NG_MODULE;
						break;
					case SolutionMetaData.LOGIN_SOLUTION :
						imgName = IMG_SOLUTION_LOGIN;
						break;
					case SolutionMetaData.AUTHENTICATOR :
						imgName = IMG_SOLUTION_AUTHENTICATOR;
						break;
					case SolutionMetaData.SMART_CLIENT_ONLY :
						imgName = IMG_SOLUTION_SMART_ONLY;
						break;
					case SolutionMetaData.WEB_CLIENT_ONLY :
						imgName = IMG_SOLUTION_WEB_ONLY;
						break;
					case SolutionMetaData.NG_CLIENT_ONLY :
						imgName = IMG_SOLUTION_NG_ONLY;
						break;
					case SolutionMetaData.MOBILE :
						imgName = IMG_SOLUTION_MOBILE;
						break;
					case SolutionMetaData.PRE_IMPORT_HOOK :
						imgName = IMG_SOLUTION_PREIMPORT;
						break;
					case SolutionMetaData.POST_IMPORT_HOOK :
						imgName = IMG_SOLUTION_POSTIMPORT;
				}
			}
		}

		return uiActivator.loadImageFromBundle(imgName, disabled);
	}

	private void addSolutionProjects(ServoyProject[] projects)
	{
		if (projects.length == 0)
		{
			activeSolutionNode.children = new PlatformSimpleUserNode[0];
			activeSolutionNode.setIcon(uiActivator.loadImageFromBundle(IMG_SOLUTION, true));
			activeSolutionNode.setDisplayName(Messages.TreeStrings_NoActiveSolution);
			activeSolutionNode.setRealObject(null);
			allSolutionsNode.children = new PlatformSimpleUserNode[0];
			return;
		}
		// the set of solutions a user can work with at a given time is determined
		// by the active solution and active editor (in case of a calculation
		// being edited);
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
		Collection<Solution> modulesForCalculation = null;
		if (solutionOfCalculation != null)
		{
			modulesForCalculation = new ArrayList<Solution>();
			// a calculation is being edited
			try
			{
				HashMap<String, Solution> modulesOfCalculationSolution = new HashMap<String, Solution>();
				solutionOfCalculation.getReferencedModulesRecursive(modulesOfCalculationSolution);
				modulesForCalculation.addAll(modulesOfCalculationSolution.values());
				modulesForCalculation.add(solutionOfCalculation);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}

		List<PlatformSimpleUserNode> modulesNodeChildren = new ArrayList<PlatformSimpleUserNode>();
		List<PlatformSimpleUserNode> allSolutionChildren = new ArrayList<PlatformSimpleUserNode>();

		activeSolutionNode.children = null;
		activeSolutionNode.setEnabled(false); // this node will be accessible even if it is not enabled;
		// enabled only tells if the normal solution nodes should be shown or not under it;
		// for example: you could have an active solution, but you are in calculation mode - and the calculation
		// is from a module - in this case you do not want to make active solution content available but you
		// want access to the modules node under it...
		for (ServoyProject servoyProject : projects)
		{
			if (servoyProject.getProject().isOpen())
			{
				String displayValue = servoyProject.getProject().getName();

				boolean isModule = false;
				boolean expandable = false;

				// this means that the only expandable solution nodes will be the
				// active solution and it's referenced modules;
				// all other solutions will appear grayed-out and not-expandable
				// (still allows the user to activate them if necessary
				for (ServoyProject module : modules)
				{
					if (module.getProject().equals(servoyProject.getProject()))
					{
						isModule = true;
						break;
					}
				}
				expandable = isModule;

				// if the active editor edits a calculation, the only expandable
				// solutions are the solution
				// where the calculation is stored and it's modules
				if (modulesForCalculation != null && expandable)
				{
					expandable = false;
					for (Solution s : modulesForCalculation)
					{
						if (s == servoyProject.getSolution())
						{
							expandable = true;
							break;
						}
					}
				}

				if (isModule == true)
				{
					// we are in calculation mode if expandable is false - and this
					// would be a module that is not usable by the active calculation
					// editor's calculation
					PlatformSimpleUserNode node;
					if (servoyProject.isActive())
					{
						activeSolutionNode.setEnabled(expandable);
						node = new PlatformSimpleUserNode(displayValue, UserNodeType.SOLUTION_ITEM, servoyProject,
							getServoyProjectImage(servoyProject, false, false));
					}
					else
					{
						node = new PlatformSimpleUserNode(displayValue, UserNodeType.SOLUTION_ITEM, servoyProject,
							getServoyProjectImage(servoyProject, true, !expandable));
						node.setEnabled(expandable);
						modulesNodeChildren.add(node);
						node.parent = modulesOfActiveSolution;

						node = new PlatformSimpleUserNode(displayValue, UserNodeType.SOLUTION_ITEM, servoyProject,
							getServoyProjectImage(servoyProject, true, false));
					}

					node.setEnabled(false);
					allSolutionChildren.add(node);
					node.parent = allSolutionsNode;

					String solutionName = (String)servoyProject.getSolution().getProperty(StaticContentSpecLoader.PROPERTY_TITLETEXT.getPropertyName());
					if (solutionName == null) solutionName = servoyProject.getSolution().getName();

					node.setToolTipText(solutionName + "(" + getSolutionTypeAsString(servoyProject) + ")");
				}
				else
				{
					PlatformSimpleUserNode node = new PlatformSimpleUserNode(displayValue, UserNodeType.SOLUTION_ITEM_NOT_ACTIVE_MODULE, servoyProject, null)
					{
						@Override
						public String getToolTipText()
						{
							return servoyProject.getProject().getName() + "(" + getSolutionTypeAsString(servoyProject) + ")";
						}

						@Override
						public Image getIcon()
						{
							return getServoyProjectImage(servoyProject, false, true);
						}
					};
					node.setEnabled(false);
					allSolutionChildren.add(node);
					node.parent = allSolutionsNode;
				}
			}
		}

		// set active solution node to usable/unusable
		activeSolutionNode.setRealObject(servoyModel.getActiveProject());
		if (activeSolutionNode.getRealObject() != null && ((ServoyProject)activeSolutionNode.getRealObject()).getSolution() != null)
		{
			activeSolutionNode.setIcon(getServoyProjectImage(((ServoyProject)activeSolutionNode.getRealObject()), false, false));
			String name = ((ServoyProject)activeSolutionNode.getRealObject()).getProject().getName();
			if (solutionOfCalculation != null)
			{
				name += " (calculation mode)";
			}
			else
			{
				// databaseManager not allowed in login solution
				if (((ServoyProject)activeSolutionNode.getRealObject()).getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION)
					databaseManager.hide();
				else databaseManager.unhide();
			}
			activeSolutionNode.setDisplayName(name);

			String solutionName = (String)((ServoyProject)activeSolutionNode.getRealObject()).getSolution().getProperty(
				StaticContentSpecLoader.PROPERTY_TITLETEXT.getPropertyName());
			if (solutionName == null) solutionName = ((ServoyProject)activeSolutionNode.getRealObject()).getSolution().getName();

			activeSolutionNode.setToolTipText(solutionName + "(" + getSolutionTypeAsString((ServoyProject)activeSolutionNode.getRealObject()) + ")");
		}
		else
		{
			activeSolutionNode.setIcon(getServoyProjectImage(((ServoyProject)activeSolutionNode.getRealObject()), false, true));
			activeSolutionNode.setDisplayName(Messages.TreeStrings_NoActiveSolution);
		}

		// add children to nodes...
		modulesOfActiveSolution.children = modulesNodeChildren.toArray(new PlatformSimpleUserNode[modulesNodeChildren.size()]);
		allSolutionsNode.children = allSolutionChildren.toArray(new PlatformSimpleUserNode[allSolutionChildren.size()]);
		Arrays.sort(modulesOfActiveSolution.children, StringComparator.INSTANCE);
		Arrays.sort(allSolutionsNode.children, StringComparator.INSTANCE);
	}

	private String getSolutionTypeAsString(ServoyProject project)
	{
		if (project.getSolutionMetaData() != null)
		{
			for (int i = 0; i < SolutionMetaData.solutionTypes.length; i++)
			{
				if (project.getSolutionMetaData().getSolutionType() == SolutionMetaData.solutionTypes[i]) return SolutionMetaData.solutionTypeNames[i];
			}
		}

		return "unknown type";
	}

	public Object getParent(Object child)
	{
		if (child instanceof PlatformSimpleUserNode)
		{
			return ((PlatformSimpleUserNode)child).parent;
		}
		return null;
	}

	public Object[] getChildren(Object parent)
	{
		if (parent instanceof PlatformSimpleUserNode)
		{
			PlatformSimpleUserNode un = ((PlatformSimpleUserNode)parent);
			if (un.children == null) // lazy load children
			{
				try
				{
					UserNodeType type = un.getType();
					if (type == UserNodeType.SOLUTION)
					{
						if (un.getRealObject() != null)
						{
							if (un.isEnabled()) addSolutionNodeChildren(un);
							if (un.children != null)
							{
								PlatformSimpleUserNode activeSolutionChildren[] = new PlatformSimpleUserNode[un.children.length + 1];
								for (int i = un.children.length - 1; i >= 0; i--)
								{
									activeSolutionChildren[i] = (PlatformSimpleUserNode)un.children[i];
								}
								activeSolutionChildren[activeSolutionChildren.length - 1] = modulesOfActiveSolution;
								un.children = activeSolutionChildren;
							}
							else
							{
								un.children = new PlatformSimpleUserNode[] { modulesOfActiveSolution };
							}
						}
					}
					else if (type == UserNodeType.SOLUTION_ITEM)
					{
						if (un.isEnabled())
						{
							addSolutionNodeChildren(un);
						}
					}
					else if (type == UserNodeType.FORMS || (type == UserNodeType.GRAYED_OUT && un.getRealType() == UserNodeType.FORMS))
					{
						addFormsNodeChildren(un, false);
					}
					else if (type == UserNodeType.COMPONENT_FORMS || (type == UserNodeType.GRAYED_OUT && un.getRealType() == UserNodeType.COMPONENT_FORMS))
					{
						addFormsNodeChildren(un, true);
					}
					else if (type == UserNodeType.WORKING_SET || (type == UserNodeType.GRAYED_OUT && un.getRealType() == UserNodeType.WORKING_SET))
					{
						addWorkingSetNodeChildren(un);
					}
					else if (type == UserNodeType.FORM || (type == UserNodeType.GRAYED_OUT && un.getRealType() == UserNodeType.FORM))
					{
						addFormNodeChildren(un);
					}
					else if (type == UserNodeType.MENUS)
					{
						addMenusNodeChildren(un, UserNodeType.MENU, true);
					}
					else if (type == UserNodeType.MENU_FOUNDSETS)
					{
						addMenusNodeChildren(un, UserNodeType.MENU_FOUNDSET, false);
					}
					else if (type == UserNodeType.SERVERS)
					{
						addServersNodeChildren(un);
					}
					else if (type == UserNodeType.SERVER || un.getType() == UserNodeType.INMEMORY_DATASOURCES || un.getType() == UserNodeType.VIEW_FOUNDSETS)
					{
						UserNodeType t = UserNodeType.TABLE;
						if (un.getType() == UserNodeType.INMEMORY_DATASOURCES)
						{
							t = UserNodeType.INMEMORY_DATASOURCE;
						}
						else if (un.getType() == UserNodeType.VIEW_FOUNDSETS)
						{
							t = UserNodeType.VIEW_FOUNDSET;
						}
						addServerNodeChildren(un, t);
					}
					else if (type == UserNodeType.PLUGINS)
					{
						addPluginsNodeChildren(un);
					}
					else if (type == UserNodeType.MEDIA_FOLDER)
					{
						addMediaFolderChildrenNodes(un, ((MediaNode)un.getRealObject()).getMediaProvider());
					}
					else if (type == UserNodeType.COMPONENTS_FROM_RESOURCES)
					{
						SpecProviderState componentsSpecProviderState = getComponentsSpecProviderState();
						if (componentsSpecProviderState != null) // the package management system might not yet be initialized at developer startup
						{
							Image packageIcon = uiActivator.loadImageFromBundle("components_package.png");
							Image zipPackageIcon = uiActivator.loadImageFromBundle("zip_package.png");
							IPackageReader[] packages = componentsSpecProviderState.getAllPackageReaders();
							List<PlatformSimpleUserNode> children = new ArrayList<PlatformSimpleUserNode>();
							for (IPackageReader entry : packages)
							{
								IResource resource = getResource(entry);
								if (resource != null && !(resource instanceof IProject) && resource.getProject().hasNature(ServoyResourcesProject.NATURE_ID))
								{
									Image nodeIcon = packageIcon;
									if (resource.getName().endsWith(".zip"))
									{
										nodeIcon = zipPackageIcon;
									}
									PlatformSimpleUserNode node = new PlatformSimpleUserNode(entry.getPackageDisplayname(),
										UserNodeType.COMPONENTS_NONPROJECT_PACKAGE, entry, nodeIcon);
									node.parent = un;
									children.add(node);
								}
							}
							un.children = children.toArray(new PlatformSimpleUserNode[children.size()]);
						}
					}
					else if (type == UserNodeType.SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES)
					{
						SpecProviderState componentSpecProvider = getComponentsSpecProviderState();
						SpecProviderState serviceSpecProvider = getServicesSpecProviderState();
						if (componentSpecProvider != null && serviceSpecProvider != null) // the package management system might not yet be initialized at developer startup
						{
							List<PlatformSimpleUserNode> children = getWebProjects(un, componentSpecProvider, "services_package.png",
								UserNodeType.COMPONENTS_PROJECT_PACKAGE, IPackageReader.WEB_COMPONENT);
							List<PlatformSimpleUserNode> layoutProjects = getWebProjects(un, componentSpecProvider, "components_package.png",
								UserNodeType.LAYOUT_PROJECT_PACKAGE, IPackageReader.WEB_LAYOUT);
							List<PlatformSimpleUserNode> servicesProjects = getWebProjects(un, serviceSpecProvider, "services_package.png",
								UserNodeType.SERVICES_PROJECT_PACKAGE, IPackageReader.WEB_SERVICE);
							children.addAll(servicesProjects);
							children.addAll(layoutProjects);
							children.addAll(getBinaryPackages(un, componentSpecProvider, serviceSpecProvider));
							un.children = children.toArray(new PlatformSimpleUserNode[children.size()]);
						}
					}
					else if (type == UserNodeType.COMPONENTS_NONPROJECT_PACKAGE || type == UserNodeType.LAYOUT_NONPROJECT_PACKAGE)
					{
						String packageName = getPackageName(un);
						List<String> components = new ArrayList<>(getComponentsSpecProviderState().getWebObjectsInPackage(packageName));
						List<PlatformSimpleUserNode> children = new ArrayList<PlatformSimpleUserNode>();
						if (components.size() > 0)
						{
							Collections.sort(components);
							Image componentIcon = uiActivator.loadImageFromBundle("ng_component.png");
							for (String component : components)
							{
								WebObjectSpecification spec = getComponentsSpecProviderState().getWebObjectSpecification(component);
								Image img = getIconFromSpec(spec, false);
								PlatformSimpleUserNode node = new PlatformSimpleUserNode(spec.getDisplayName(), UserNodeType.COMPONENT, spec,
									img != null ? img : componentIcon);
								node.parent = un;
								children.add(node);
							}
						}
						List<String> layouts = new ArrayList<>(getComponentsSpecProviderState().getLayoutsInPackage(packageName));
						if (layouts.size() > 0)
						{
							Collections.sort(layouts);
							Image componentIcon = uiActivator.loadImageFromBundle("layout.png");
							for (String layout : layouts)
							{
								WebLayoutSpecification spec = getComponentsSpecProviderState().getLayoutSpecifications().get(packageName).getSpecification(
									layout);
								Image img = getIconFromSpec(spec, false);
								PlatformSimpleUserNode node = new PlatformSimpleUserNode(spec.getDisplayName(), UserNodeType.LAYOUT, spec,
									img != null ? img : componentIcon);
								node.parent = un;
								children.add(node);
							}
						}
						un.children = children.toArray(new PlatformSimpleUserNode[children.size()]);
					}
					else if (type == UserNodeType.COMPONENTS_PROJECT_PACKAGE || type == UserNodeType.LAYOUT_PROJECT_PACKAGE)
					{
						String packageName = getPackageName(un);
						Set<String> folderNames = new HashSet<String>();
						List<String> components = new ArrayList<>(getComponentsSpecProviderState().getWebObjectsInPackage(packageName));
						List<PlatformSimpleUserNode> children = new ArrayList<PlatformSimpleUserNode>();
						if (components.size() > 0)
						{
							Collections.sort(components);
							createWebPackageProjectChildren(un, getComponentsSpecProviderState(), UserNodeType.COMPONENT, folderNames, children, components,
								uiActivator.loadImageFromBundle("ng_component.png"));
						}
						List<String> layouts = new ArrayList<>(getComponentsSpecProviderState().getLayoutsInPackage(packageName));
						if (layouts.size() > 0)
						{
							Collections.sort(layouts);
							Image componentIcon = uiActivator.loadImageFromBundle("layout.png");
							for (String layout : layouts)
							{
								WebLayoutSpecification spec = getComponentsSpecProviderState().getLayoutSpecifications().get(packageName).getSpecification(
									layout);
								String folderName = getFolderNameFromSpec(spec);
								IFile file = ResourcesUtils.findFileWithShortestPathForLocationURI(spec.getSpecURL().toURI());
								if (file != null)
								{
									if (file.getProjectRelativePath().segmentCount() > 1)
									{
										Image img = getIconFromSpec(spec, false);
										PlatformSimpleUserNode node = new PlatformSimpleUserNode(spec.getDisplayName(), UserNodeType.LAYOUT, spec,
											img != null ? img : componentIcon);
										node.parent = un;
										children.add(node);
										folderNames.add(folderName);
									}
								}
							}
						}
						children.addAll(addOtherPackageResources(un, folderNames));
						un.children = children.toArray(new PlatformSimpleUserNode[children.size()]);
					}
					else if (type == UserNodeType.SERVICES_FROM_RESOURCES)
					{
						SpecProviderState provider = getServicesSpecProviderState();
						if (provider != null) // the package management system might not yet be initialized at developer startup
						{
							IPackageReader[] packages = provider.getAllPackageReaders();
							List<PlatformSimpleUserNode> children = new ArrayList<PlatformSimpleUserNode>();
							Image packageIcon = uiActivator.loadImageFromBundle("services_package.png");
							Image zipPackageIcon = uiActivator.loadImageFromBundle("zip_package.png");
							for (IPackageReader entry : packages)
							{
								IResource resource = getResource(entry);
								if (resource != null && !(resource instanceof IProject) && resource.getProject().hasNature(ServoyResourcesProject.NATURE_ID))
								{
									Image nodeIcon = packageIcon;
									if (resource.getName().endsWith(".zip"))
									{
										nodeIcon = zipPackageIcon;
									}
									PlatformSimpleUserNode node = new PlatformSimpleUserNode(entry.getPackageDisplayname(),
										UserNodeType.SERVICES_NONPROJECT_PACKAGE, entry, nodeIcon);
									node.parent = un;
									children.add(node);
								}
							}
							un.children = children.toArray(new PlatformSimpleUserNode[children.size()]);
						}
					}
					else if (type == UserNodeType.SERVICES_NONPROJECT_PACKAGE)
					{
						SpecProviderState provider = getServicesSpecProviderState();
						if (provider != null) // the package management system might not yet be initialized at developer startup
						{
							String packageName = getPackageName(un);
							PackageSpecification<WebObjectSpecification> servicesPackage = provider.getWebObjectSpecifications().get(packageName);
							List<PlatformSimpleUserNode> children = new ArrayList<PlatformSimpleUserNode>();
							if (servicesPackage != null)
							{
								List<String> services = new ArrayList<>(servicesPackage.getSpecifications().keySet());
								Collections.sort(services);
								Image serviceDefaultIcon = uiActivator.loadImageFromBundle("service.png");
								for (String component : services)
								{
									WebObjectSpecification spec = provider.getWebObjectSpecification(component);
									Image img = getIconFromSpec(spec, true);
									PlatformSimpleUserNode node = new PlatformSimpleUserNode(spec.getDisplayName(), UserNodeType.SERVICE, spec,
										img != null ? img : serviceDefaultIcon);
									node.parent = un;
									children.add(node);
								}
							}
							un.children = children.toArray(new PlatformSimpleUserNode[children.size()]);
						}
					}
					else if (type == UserNodeType.SERVICES_PROJECT_PACKAGE)
					{
						SpecProviderState provider = getServicesSpecProviderState();
						if (provider != null) // the package management system might not yet be initialized at developer startup
						{
							String packageName = getPackageName(un);
							Set<String> folderNames = new HashSet<String>();
							PackageSpecification<WebObjectSpecification> servicesPackage = provider.getWebObjectSpecifications().get(packageName);
							List<PlatformSimpleUserNode> children = new ArrayList<PlatformSimpleUserNode>();
							if (servicesPackage != null)
							{
								List<String> services = new ArrayList<>(servicesPackage.getSpecifications().keySet());
								Collections.sort(services);
								Image defaultIcon = uiActivator.loadImageFromBundle("service.png");
								createWebPackageProjectChildren(un, provider, UserNodeType.SERVICE, folderNames, children, services, defaultIcon);
							}
							children.addAll(addOtherPackageResources(un, folderNames));
							un.children = children.toArray(new PlatformSimpleUserNode[children.size()]);
						}
					}
					else if (type == UserNodeType.ALL_WEB_PACKAGE_PROJECTS)
					{
						// this has to be just the projects, can't be the IPackageReaders because for a none referenced project
						// the package reader is not loaded/created.
						List<PlatformSimpleUserNode> children = new ArrayList<PlatformSimpleUserNode>();
						IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
						Image packageIcon = uiActivator.loadImageFromBundle("components_package.png");
						ServoyProject activeSolution = ServoyModelFinder.getServoyModel().getActiveProject();
						List<IProject> solutionAndModuleReferencedProjects = (activeSolution != null ? activeSolution.getSolutionAndModuleReferencedProjects()
							: null);
						for (IProject iProject : projects)
						{
							if (iProject.isAccessible() && iProject.hasNature(ServoyNGPackageProject.NATURE_ID))
							{
								IPackageReader packageType = getPackageType(getComponentsSpecProviderState(), iProject);
								if (packageType == null) packageType = getPackageType(getServicesSpecProviderState(), iProject);
								if (packageType == null && iProject.getFile(new Path("META-INF/MANIFEST.MF")).exists())
								{
									packageType = new ContainerPackageReader(new File(iProject.getLocationURI()), iProject);
								}
								if (packageType == null) continue;
								PlatformSimpleUserNode node = new PlatformSimpleUserNode(resolveWebPackageDisplayName(iProject),
									UserNodeType.WEB_PACKAGE_PROJECT_IN_WORKSPACE, packageType, packageIcon);

								//if it is not loaded, hide it so that it will have the gray icon
								if (solutionAndModuleReferencedProjects == null || !solutionAndModuleReferencedProjects.contains(iProject)) node.hide();

								node.parent = un;
								children.add(node);
							}
						}
						un.children = children.toArray(new PlatformSimpleUserNode[children.size()]);
					}
					else if (un.getType() == UserNodeType.COMPONENT || un.getType() == UserNodeType.SERVICE || un.getType() == UserNodeType.LAYOUT)
					{
						WebObjectSpecification spec = (WebObjectSpecification)un.getRealObject();
						IFolder folder = getFolderFromSpec(getResource((IPackageReader)un.parent.getRealObject()), spec);
						if (folder != null)
						{
							searchFolderChildren(un, folder);
						}
						else
						{
							ServoyLog.logInfo("cannot find web object name from " + spec.getName());
						}
					}
					else if (un.getType() == UserNodeType.WEB_OBJECT_FOLDER)
					{
						searchFolderChildren(un, (IFolder)un.getRealObject());
					}
					if (un.children == null)
					{
						un.children = new PlatformSimpleUserNode[0];
					}
				}
				catch (Exception e)
				{
					ServoyLog.logWarning("Cannot create the children of node " + un.getName(), e);
				}
			}
			else if (un.children.length == 1 && un.children[0].getType() == UserNodeType.RETURNTYPEPLACEHOLDER)
			{
				addReturnTypeNodes(un, (Class< ? >[])un.children[0].getRealObject());
			}
			return un.children;
		}
		return new Object[0];
	}

	private void createWebPackageProjectChildren(PlatformSimpleUserNode un, SpecProviderState provider, UserNodeType type, Set<String> folderNames,
		List<PlatformSimpleUserNode> children, List<String> services, Image defaultIcon)
	{
		for (String component : services)
		{
			WebObjectSpecification spec = provider.getWebObjectSpecification(component);
			String folderName = getFolderNameFromSpec(spec);
			try
			{
				IFile file = ResourcesUtils.findFileWithShortestPathForLocationURI(spec.getSpecURL().toURI());
				if (file != null)
				{
					if (file.getProjectRelativePath().segmentCount() > 1)
					{
						Image img = getIconFromSpec(spec, false);
						PlatformSimpleUserNode node = new PlatformSimpleUserNode(spec.getDisplayName(), type, spec, img != null ? img : defaultIcon);
						node.parent = un;
						children.add(node);
						folderNames.add(folderName);
					}
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	private List<PlatformSimpleUserNode> addOtherPackageResources(PlatformSimpleUserNode un, Set<String> folderNames) throws CoreException
	{
		List<PlatformSimpleUserNode> children = new ArrayList<>();
		IContainer packageFolder = (IContainer)getResource((IPackageReader)un.getRealObject());
		Comparator<IResource> comparator = new Comparator<IResource>()
		{
			@Override
			public int compare(IResource arg0, IResource arg1)
			{
				return arg0.getName().compareTo(arg1.getName());
			}
		};
		Set<IResource> folders = new TreeSet<IResource>(comparator);
		Set<IResource> files = new TreeSet<IResource>(comparator);
		for (IResource res : packageFolder.members())
		{
			if (res instanceof IFolder)
			{
				if (!res.getName().startsWith(".") && !folderNames.contains(res.getName())) folders.add(res);
			}
			else
			{
				if (!".project".equals(res.getName())) files.add(res);
			}
		}
		for (IResource res : folders)
		{
			PlatformSimpleUserNode node = new PlatformSimpleUserNode(res.getName(), UserNodeType.WEB_OBJECT_FOLDER, res,
				uiActivator.loadImageFromBundle("folder.png"));
			node.parent = un;
			children.add(node);
		}
		for (IResource res : files)
		{
			PlatformSimpleUserNode node = new PlatformSimpleUserNode(res.getName(), UserNodeType.COMPONENT_RESOURCE, res,
				SolutionExplorerListContentProvider.getIcon(res.getName()));
			node.parent = un;
			children.add(node);
		}
		return children;
	}

	public static String getFolderNameFromSpec(WebObjectSpecification spec)
	{
		IPath path = new Path(spec.getSpecURL().toExternalForm());
		if (path.segmentCount() > 1)//it should contain at least 1 folder
		{
			return path.segment(path.segmentCount() - 2);
		}
		return null;
	}

	public static IFolder getFolderFromSpec(IResource resource, WebObjectSpecification spec)
	{
		String folderName = getFolderNameFromSpec(spec);
		IFolder folder = null;
		if (resource instanceof IContainer && folderName != null)
		{
			IContainer container = (IContainer)resource;
			folder = container.getFolder(new Path(folderName));
			if (folder == null || !folder.exists())
			{
				try
				{
					IFile specFile = ResourcesUtils.findFileWithShortestPathForLocationURI(spec.getSpecURL().toURI());
					if (specFile != null && specFile.getParent() instanceof IFolder)
					{
						folder = (IFolder)specFile.getParent();
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError("Could not get folder from spec for web object " + spec.getName(), e);
				}
			}
		}
		return folder != null && folder.exists() ? folder : null;
	}

	private void searchFolderChildren(PlatformSimpleUserNode un, IFolder f)
	{
		List<SimpleUserNode> children = new ArrayList<>();
		try
		{
			for (IResource res : f.members(false))
			{
				if (res instanceof IFolder)
				{
					children.add(new PlatformSimpleUserNode(res.getName(), UserNodeType.WEB_OBJECT_FOLDER, res, uiActivator.loadImageFromBundle("folder.png")));
				}
			}
			un.children = children.toArray(new PlatformSimpleUserNode[children.size()]);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	/**
	 * Returns all zip packages contained by the given solution
	 */
	private Collection< ? extends PlatformSimpleUserNode> getBinaryPackages(PlatformSimpleUserNode un, SpecProviderState componentsProvider,
		SpecProviderState servicesProvider)
	{
		List<PlatformSimpleUserNode> result = new ArrayList<PlatformSimpleUserNode>();
		Object realObject = un.getRealObject();
		if (realObject instanceof Solution)
		{
			Solution servoySolution = (Solution)realObject;
			ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(servoySolution.getName());
			IProject eclipseProject = servoyProject.getProject();
			Image packageIcon = uiActivator.loadImageFromBundle("zip_package.png");


			List<IProject> allReferencedProjects;
			if (includeModules)
			{
				try
				{
					allReferencedProjects = servoyProject.getSolutionAndModuleReferencedProjects();
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
					allReferencedProjects = new ArrayList<IProject>(1);
					allReferencedProjects.add(eclipseProject);
				}
			}
			else
			{
				allReferencedProjects = new ArrayList<IProject>(1);
				allReferencedProjects.add(eclipseProject);
			}
			ServoyResourcesProject activeResourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
			if (activeResourcesProject != null)
			{
				allReferencedProjects.remove(activeResourcesProject.getProject());
			}

			addBinaryReferecedPackages(un, result, packageIcon, allReferencedProjects, componentsProvider.getAllPackageReaders());
			addBinaryReferecedPackages(un, result, packageIcon, allReferencedProjects, servicesProvider.getAllPackageReaders());
		}
		return result;
	}

	private void addBinaryReferecedPackages(PlatformSimpleUserNode un, List<PlatformSimpleUserNode> result, Image packageIcon,
		List<IProject> allReferencedProjects, IPackageReader[] packages)
	{
		IWorkspaceRoot root = ServoyModel.getWorkspace().getRoot();
		HashMap<String, IPackageReader> packageReadersMap = new HashMap<String, IPackageReader>();

		for (IPackageReader reader : packages)
		{
			if (reader.getResource() != null && reader.getResource().isFile())
			{
				IPackageReader existingReader = packageReadersMap.get(reader.getPackageName());
				if (existingReader != null && existingReader.getResource() != null && existingReader.getResource().isDirectory())
				{
					continue;
				}
			}
			packageReadersMap.put(reader.getPackageName(), reader);
		}


		for (IPackageReader reader : packageReadersMap.values())
		{
			File resource = reader.getResource();
			if (resource != null && resource.isFile())
			{
				IFile[] files = root.findFilesForLocationURI(resource.toURI());
				for (IFile locatedFile : files) // for example if one would import from git a whole repo as project and then some projects from that repo as also eclipse projects - it can be that the same file is found in multiple places/paths in the workspace
				{
					if (allReferencedProjects.contains(locatedFile.getProject()))
					{
						UserNodeType nodeType = UserNodeType.COMPONENTS_NONPROJECT_PACKAGE;
						String displayName = reader.getPackageDisplayname();
						if (IPackageReader.WEB_LAYOUT.equals(reader.getPackageType()))
						{
							nodeType = UserNodeType.LAYOUT_NONPROJECT_PACKAGE;
						}
						else if (IPackageReader.WEB_SERVICE.equals(reader.getPackageType()))
						{
							nodeType = UserNodeType.SERVICES_NONPROJECT_PACKAGE;
						} // else it's a components package
						PlatformSimpleUserNode node = new PlatformSimpleUserNode(displayName, nodeType, reader, packageIcon);
						node.parent = un;
						result.add(node);
						break;
					}
				}
			}
		}
	}

	private String resolveWebPackageDisplayName(IProject iProject)
	{
		String displayName;

		// the package project could be referenced by active solution/modules or not (we still have to know it's display name)
		if (getComponentsSpecProviderState().getWebObjectSpecifications().containsKey(iProject.getName()))
			displayName = getComponentsSpecProviderState().getPackageDisplayName(iProject.getName());
		else if (getComponentsSpecProviderState().getLayoutSpecifications().containsKey(iProject.getName()))
			displayName = getComponentsSpecProviderState().getPackageDisplayName(iProject.getName());
		else if (getServicesSpecProviderState().getWebObjectSpecifications().containsKey(iProject.getName()))
			displayName = getServicesSpecProviderState().getPackageDisplayName(iProject.getName());
		else
		{
			// now we have to read the package type from the manifest
			displayName = iProject.getName(); // fall-back to project name in case we can't read manifest
			if (iProject.getFile(new Path("META-INF/MANIFEST.MF")).exists())
			{
				displayName = new ContainerPackageReader(new File(iProject.getLocationURI()), iProject).getPackageDisplayname();
			}
		}

		return displayName;
	}

	private List<PlatformSimpleUserNode> getWebProjects(PlatformSimpleUserNode un, SpecProviderState provider, String imageFileName, UserNodeType nodeType,
		String type)
	{
		List<PlatformSimpleUserNode> children = new ArrayList<PlatformSimpleUserNode>();
		Image packageIcon = uiActivator.loadImageFromBundle(imageFileName);
		Object realObject = un.getRealObject();
		if (realObject instanceof Solution)
		{
			Solution servoySolution = (Solution)realObject;
			ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(servoySolution.getName());
			IProject eclipseProject = servoyProject.getProject();
			try
			{
				List<IProject> allReferencedProjects;
				if (includeModules)
				{
					allReferencedProjects = servoyProject.getSolutionAndModuleReferencedProjects();
				}
				else
				{
					allReferencedProjects = Arrays.asList(eclipseProject.getReferencedProjects());
				}

				for (IProject iProject : allReferencedProjects)
				{
					if (iProject.isAccessible() && iProject.hasNature(ServoyNGPackageProject.NATURE_ID))
					{
						IPackageReader packageType = getPackageType(provider, iProject);
						if (packageType == null) continue;

						// we also check that reader resource matches project just in case there are also for example zip references currently loaded
						// with same package name - in which case the project is actually not loaded although it is valid and referenced
						if (type.equals(packageType.getPackageType()) && packageType.getResource().equals(new File(iProject.getLocationURI()))) // package project do not allow packageType.getPackageType() == null (an error marker will/should be generated if that info is missing from the manifest); so if it is null it will just be ignored
						{
							String displayName = packageType.getPackageDisplayname();
							List<IProject> referencingProjects = Arrays.asList(iProject.getReferencingProjects());
							if (referencingProjects.indexOf(eclipseProject) == -1 && referencingProjects.size() > 0)
							{
								displayName = appendModuleName(displayName, referencingProjects.get(0).getName());
							}
							PlatformSimpleUserNode node = new PlatformSimpleUserNode(displayName, nodeType, packageType, packageIcon);
							node.parent = un;
							children.add(node);
						}
					}
				}
			}
			catch (CoreException e)
			{
				Debug.log(e);
			}
		}
		return children;
	}

	private IPackageReader getPackageType(SpecProviderState provider, IProject iProject)
	{
		IPackageReader packageType = null;
		IPackageReader[] allPackageReaders = provider.getAllPackageReaders();
		File projectDir = new File(iProject.getLocationURI());
		for (IPackageReader pr : allPackageReaders)
		{
			if (projectDir.equals(pr.getResource()))
			{
				packageType = pr;
				break;
			}
		}
		if (packageType == null)
		{
			String packageName = iProject.getName();
			packageType = provider.getPackageReader(packageName);
			if (packageType == null || packageType instanceof ZipPackageReader)
			{
				// TODO this is a partial fix for the problem that the project is not the package name
				// see also the method resolveWebPackageDisplayName above.
				if (iProject.getFile(new Path("META-INF/MANIFEST.MF")).exists())
				{
					IPackageReader reader = new ContainerPackageReader(new File(iProject.getLocationURI()), iProject);
					packageName = reader.getPackageName();
					packageType = provider.getPackageReader(packageName);
					if (packageType == null || packageType instanceof ZipPackageReader) packageType = reader;
				}
			}
		}
		return packageType;
	}

	static String appendModuleName(String name, String moduleName)
	{
		return name + " [" + moduleName + "]";
	}

//  this removeModuleName is commented out because code should really identify stuff based on the realObject/type instead of the display name of the node;
//  so for example for a components package node that is suffixed with module (includeFromModules enabled in SolEx) you can use the real object which is an IProject to get the name
//	static String removeModuleName(String name)
//	{
//		int moduleDescriptionIdx = name.indexOf(" [");
//		if (moduleDescriptionIdx != -1) return name.substring(0, moduleDescriptionIdx);
//		return name;
//	}

	private Image loadImageFromFolder(IFolder folder, String iconPath) throws CoreException
	{
		if (iconPath != null)
		{
			IFile iconFile = folder.getFile(iconPath);
			return loadImageFromIFile(iconFile);
		}
		return null;
	}


	private Image loadImageFromProject(IProject project, String iconPath) throws CoreException
	{
		if (project != null && iconPath != null)
		{
			IFile iconFile = project.getFile(iconPath);
			return loadImageFromIFile(iconFile);
		}
		return null;
	}

	private Image loadImageFromIFile(IFile iconFile) throws CoreException
	{
		Image img = imageCache.get(iconFile.getFullPath());
		if (img == null && iconFile.exists())
		{
			try (InputStream is = iconFile.getContents())
			{
				img = loadImageFromInputStream(is, iconFile.getFullPath());
			}
			catch (IOException ex)
			{
				Debug.log(ex);
			}
		}
		return img;
	}

	private Image loadImageFromInputStream(InputStream is, IPath path)
	{
		Image img = imageCache.get(path);
		if (img == null)
		{
			Display.getDefault().syncExec(() -> {
				Display display = Display.getDefault();
				try
				{
					Image image = new Image(display, new ImageData(is).scaledTo(16, 16));
					if (image != null) imageCache.put(path, image);
				}
				catch (SWTException e)
				{
					Debug.log(e);
				}
			});
			img = imageCache.get(path);
		}
		return img;
	}

	public static IResource getResource(IPackageReader reader)
	{
		if (reader == null || reader.getResource() == null) return null;

		File file = reader.getResource();
		IPath path = Path.fromOSString(file.getAbsolutePath());
		IWorkspaceRoot root = ServoyModel.getWorkspace().getRoot();
		if (file.isDirectory())
		{
			return root.getContainerForLocation(path);
		}
		return root.getFileForLocation(path);
	}

	public boolean hasChildren(Object parent)
	{
		if (parent instanceof PlatformSimpleUserNode)
		{
			PlatformSimpleUserNode un = ((PlatformSimpleUserNode)parent);
			if (un.children == null)
			{
				if (un.getType() == UserNodeType.SOLUTION)
				{
					return un.getRealObject() != null;
				}
				else if (un.getType() == UserNodeType.SOLUTION_ITEM)
				{
					return un.isEnabled();
				}
				else if (un.getType() == UserNodeType.FORMS || (un.getType() == UserNodeType.GRAYED_OUT && un.getRealType() == UserNodeType.FORMS))
				{
					if (((Solution)un.getRealObject()).getAllNormalForms(false).hasNext()) return true;
					if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject() != null &&
						ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().hasServoyWorkingSets(
							new String[] { ((Solution)un.getRealObject()).getName() }))
						return true;
					return false;
				}
				else if (un.getType() == UserNodeType.COMPONENT_FORMS ||
					(un.getType() == UserNodeType.GRAYED_OUT && un.getRealType() == UserNodeType.COMPONENT_FORMS))
				{
					if (((Solution)un.getRealObject()).getAllComponentForms(false).hasNext()) return true;
					return false;
				}
				else if (un.getType() == UserNodeType.WORKING_SET || (un.getType() == UserNodeType.GRAYED_OUT && un.getRealType() == UserNodeType.WORKING_SET))
				{
					if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject() != null &&
						ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().hasPersistsInServoyWorkingSets(un.getName(),
							new String[] { un.getSolution().getName() }, un.parent.getType() == UserNodeType.COMPONENT_FORMS))
						return true;
					return false;
				}
				else if (un.getType() == UserNodeType.FORM || (un.getType() == UserNodeType.GRAYED_OUT && un.getRealType() == UserNodeType.FORM))
				{
					return true;
				}
				else if (un.getType() == UserNodeType.SERVERS)
				{
					return ApplicationServerRegistry.get().getServerManager().getServerNames(false, false, true, true).length > 0;
				}
				else if (un.getType() == UserNodeType.SERVER)
				{
					try
					{
						IServerInternal server = ((IServerInternal)un.getRealObject());
						// if tables are not loaded yet, do not load them here
						if (!server.isTableListLoaded()) return true;
						return server.getTableNames(true).size() > 0;
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
						return false;
					}
				}
				else if (un.getType() == UserNodeType.INMEMORY_DATASOURCES || un.getType() == UserNodeType.VIEW_FOUNDSETS)
				{
					ServoyProject servoyProject = ((AbstractMemServer< ? >)un.getRealObject()).getServoyProject();
					try
					{
						List<IProject> allReferencedProjects;
						if (includeModules)
						{
							allReferencedProjects = servoyProject.getSolutionAndModuleReferencedProjects();
						}
						else
						{
							allReferencedProjects = new ArrayList<IProject>(1);
							allReferencedProjects.add(servoyProject.getProject());
						}
						for (IProject module : allReferencedProjects)
						{
							if (module.isOpen() && module.hasNature(ServoyProject.NATURE_ID))
							{
								ServoyProject project = (ServoyProject)module.getNature(ServoyProject.NATURE_ID);
								AbstractMemServer< ? > server = un.getType() == UserNodeType.INMEMORY_DATASOURCES ? project.getMemServer()
									: project.getViewFoundsetsServer();
								if (server.getTableNames(true).size() > 0)
								{
									return true;
								}
							}
						}
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
					return false;
				}
				else if (un.getType() == UserNodeType.PLUGINS)
				{
					return true;
				}
				else if (un.getType() == UserNodeType.MEDIA_FOLDER)
				{
					return ((MediaNode)un.getRealObject()).hasChildren(EnumSet.of(MediaNode.TYPE.FOLDER));
				}
				else if (un.getType() == UserNodeType.COMPONENTS_FROM_RESOURCES)
				{
					SpecProviderState provider = getComponentsSpecProviderState();
					if (provider != null)
					{
						IPackageReader[] packages = provider.getAllPackageReaders();
						for (IPackageReader entry : packages)
						{
							IResource resource = getResource(entry);
							try
							{
								if (resource != null && !(resource instanceof IProject) && resource.getProject().hasNature(ServoyResourcesProject.NATURE_ID))
								{
									return true;
								}
							}
							catch (CoreException e)
							{
								Debug.log(e);
							}
						}
					}
				}
				else if (un.getType() == UserNodeType.SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES)
				{
					return hasWebProjectReferences(un) || containsBinaryPackages(un);
				}
				else if (un.getType() == UserNodeType.SERVICES_FROM_RESOURCES)
				{
					SpecProviderState provider = getServicesSpecProviderState();
					if (provider != null)
					{
						IPackageReader[] packages = provider.getAllPackageReaders();
						for (IPackageReader entry : packages)
						{
							IResource resource = getResource(entry);
							try
							{
								if (resource != null && !(resource instanceof IProject) && (resource.getProject().hasNature(ServoyResourcesProject.NATURE_ID)))
								{
									return true;
								}
							}
							catch (CoreException e)
							{
								Debug.log(e);
							}
						}
					}
				}
				else if (un.getType() == UserNodeType.ALL_WEB_PACKAGE_PROJECTS)
				{
					IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
					for (IProject iProject : projects)
					{
						try
						{
							if (iProject.isAccessible() && iProject.hasNature(ServoyNGPackageProject.NATURE_ID))
							{
								return true;
							}
						}
						catch (CoreException e)
						{
							Debug.log(e);
						}
					}
					return false;
				}
				else if (un.getType() == UserNodeType.COMPONENTS_NONPROJECT_PACKAGE || un.getType() == UserNodeType.LAYOUT_NONPROJECT_PACKAGE)
				{
					return getComponentsSpecProviderState() != null &&
						(!getComponentsSpecProviderState().getWebObjectsInPackage(getPackageName(un)).isEmpty() ||
							!getComponentsSpecProviderState().getLayoutsInPackage(getPackageName(un)).isEmpty());
				}
				else if (un.getType() == UserNodeType.COMPONENTS_PROJECT_PACKAGE || un.getType() == UserNodeType.LAYOUT_PROJECT_PACKAGE)
				{
					return getComponentsSpecProviderState() != null;//a project package always has the META-INF folder as a child
				}
				else if (un.getType() == UserNodeType.SERVICES_PROJECT_PACKAGE)
				{
					return getServicesSpecProviderState() != null;
				}
				else if (un.getType() == UserNodeType.SERVICES_NONPROJECT_PACKAGE)
				{
					SpecProviderState provider = getServicesSpecProviderState();
					if (provider == null) return false;// the package management system might not yet be initialized at developer startup

					PackageSpecification<WebObjectSpecification> services = provider.getWebObjectSpecifications().get(getPackageName(un));
					return services != null && !services.getSpecifications().isEmpty();
				}
				else if (un.getType() == UserNodeType.COMPONENT || un.getType() == UserNodeType.SERVICE || un.getType() == UserNodeType.LAYOUT)
				{
					WebObjectSpecification spec = (WebObjectSpecification)un.getRealObject();
					if ("file".equals(spec.getSpecURL().getProtocol()))
					{
						IFolder folder = getFolderFromSpec(getResource((IPackageReader)un.parent.getRealObject()), spec);
						if (folder != null)
						{
							return hasChildren(folder);
						}
						else
						{
							ServoyLog.logInfo("cannot find web object folder from " + spec.getName());
						}
					}
					return false;
				}
				else if (un.getType() == UserNodeType.WEB_OBJECT_FOLDER)
				{
					return hasChildren((IFolder)un.getRealObject());
				}
				else if (un.getType() == UserNodeType.MENUS || un.getType() == UserNodeType.MENU_FOUNDSETS)
				{
					return (((Solution)un.getRealObject()).getMenus(false).hasNext()) ? true : false;
				}
			}
			else if (un.children.length > 0)
			{
				return true;
			}
			return false;
		}
		else if (parent instanceof UserNode &&
			(((UserNode)parent).getType() == UserNodeType.TABLE || ((UserNode)parent).getType() == UserNodeType.INMEMORY_DATASOURCE) ||
			((UserNode)parent).getType() == UserNodeType.VIEW_FOUNDSET) return false;
		return true;
	}

	private boolean hasChildren(IFolder f)
	{
		try
		{
			if (!f.exists())
			{
				ServoyLog.logInfo("Web object folder " + f.getFullPath().toString() + " does not exist");
				return false;
			}
			for (IResource res : f.members(false))
			{
				if (res instanceof IFolder)
				{
					return true;
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return false;
	}


	private boolean containsBinaryPackages(PlatformSimpleUserNode un)
	{
		SpecProviderState componentProvider = getComponentsSpecProviderState();
		SpecProviderState serviceProvider = getServicesSpecProviderState();
		if (componentProvider == null || serviceProvider == null) return false; // the package management system is not yet initialized; this is probably at developer startup

		Object realObject = un.getRealObject();
		if (realObject instanceof Solution)
		{
			Solution servoySolution = (Solution)realObject;
			ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(servoySolution.getName());

			if (servoyProject != null)
			{
				IProject eclipseProject = servoyProject.getProject();

				List<IProject> allReferencedProjects;
				if (includeModules)
				{
					try
					{
						allReferencedProjects = servoyProject.getSolutionAndModuleReferencedProjects();
					}
					catch (CoreException e)
					{
						Debug.log(e);
						allReferencedProjects = new ArrayList<IProject>(1);
						allReferencedProjects.add(eclipseProject);
					}
				}
				else
				{
					allReferencedProjects = new ArrayList<IProject>(1);
					allReferencedProjects.add(eclipseProject);
				}
				ServoyResourcesProject activeResourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
				if (activeResourcesProject != null)
				{
					allReferencedProjects.remove(activeResourcesProject.getProject());
				}
				ArrayList<IPackageReader> packages = new ArrayList<>(Arrays.asList(componentProvider.getAllPackageReaders()));
				packages.addAll(Arrays.asList(serviceProvider.getAllPackageReaders()));

				for (IPackageReader entry : packages)
				{
					IResource resource = getResource(entry);
					if (resource instanceof IFile && allReferencedProjects.contains(resource.getProject()))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Gets the name of a service/component package based on a node in the tree. It uses the realObject instead of node name
	 * cause node display name cannot be counted on (sometimes it's subfixed with the name of the module it is referenced from).
	 *
	 * @param un the user node representing a package in SolEx.
	 * @return the name of that package.
	 */
	public static String getPackageName(PlatformSimpleUserNode un)
	{
		IPackageReader realObject = (IPackageReader)un.getRealObject();
		return realObject.getPackageName(); // realObject should be an IContainer in this case (project or folder)
	}

	private boolean hasWebProjectReferences(PlatformSimpleUserNode un)
	{
		Object realObject = un.getRealObject();
		if (realObject instanceof Solution)
		{
			Solution servoySolution = (Solution)realObject;
			ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(servoySolution.getName());
			if (servoyProject != null)
			{
				IProject eclipseProject = servoyProject.getProject();
				try
				{
					List<IProject> allReferencedProjects;
					if (includeModules)
					{
						allReferencedProjects = servoyProject.getSolutionAndModuleReferencedProjects();
					}
					else
					{
						allReferencedProjects = Arrays.asList(eclipseProject.getReferencedProjects());
					}

					for (IProject iProject : allReferencedProjects)
					{
						if (iProject.isAccessible() && iProject.hasNature(ServoyNGPackageProject.NATURE_ID)) return true;
					}
				}
				catch (CoreException e)
				{
					Debug.log(e);
				}
			}
		}
		return false;
	}

	private void addMenusNodeChildren(PlatformSimpleUserNode menusNode, UserNodeType childType, boolean addChildren)
	{
		Solution solution = (Solution)menusNode.getRealObject();
		List<PlatformSimpleUserNode> menuNodes = new ArrayList<PlatformSimpleUserNode>();
		Iterator<Menu> it = solution.getMenus(true);
		while (it.hasNext())
		{
			Menu menu = it.next();
			String tooltip = "Menu node " + menu.getName() + " for creating a menu";
			if (childType == UserNodeType.MENU_FOUNDSET)
			{
				tooltip = "Menu based datasource: <b>'menu:" + menu.getName() + "'</b>";
			}

			PlatformSimpleUserNode node = new PlatformSimpleUserNode(menu.getName(), childType, "", tooltip, menu,
				uiActivator.loadImageFromBundle("column.png"));
			menuNodes.add(node);
			node.parent = menusNode;
			if (addChildren)
			{
				addMenuItemsChildren(node, menu);
			}
		}
		menusNode.children = menuNodes.toArray(new PlatformSimpleUserNode[menuNodes.size()]);
	}

	private void addMenuItemsChildren(PlatformSimpleUserNode parentNode, AbstractBase parentPersist)
	{
		List<PlatformSimpleUserNode> menuNodes = new ArrayList<PlatformSimpleUserNode>();
		for (IPersist persist : parentPersist.getAllObjectsAsList())
		{
			if (persist instanceof MenuItem menuItem)
			{
				PlatformSimpleUserNode node = new PlatformSimpleUserNode(menuItem.getName(), UserNodeType.MENU_ITEM, "", "Menu item", menuItem,
					uiActivator.loadImageFromBundle("class.png"));
				menuNodes.add(node);
				node.parent = parentNode;
				addMenuItemsChildren(node, menuItem);
			}

		}
		parentNode.children = menuNodes.toArray(new PlatformSimpleUserNode[menuNodes.size()]);
	}

	private void addServersNodeChildren(PlatformSimpleUserNode serversNode)
	{
		List<PlatformSimpleUserNode> serverNodes = new ArrayList<PlatformSimpleUserNode>();
		IServerManagerInternal handler = ApplicationServerRegistry.get().getServerManager();
		String[] array = handler.getServerNames(false, false, true, true);
		for (String server_name : array)
		{
			IServerInternal serverObj = (IServerInternal)handler.getServer(server_name, false, false);

			String tooltip = serverObj.toHTML();
			if (serverObj.getConfig().isEnabled() && serverObj.isValid() && !serverObj.getName().equals(server_name))
			{
				tooltip = "Duplicate of " + serverObj.getName();
			}
			PlatformSimpleUserNode node = new PlatformSimpleUserNode(server_name, UserNodeType.SERVER, "", tooltip, serverObj,
				getServerImage(server_name, serverObj));
			serverNodes.add(node);
			node.parent = serversNode;
		}

		serversNode.children = serverNodes.toArray(new PlatformSimpleUserNode[serverNodes.size()]);
	}

	private void addServerNodeChildren(PlatformSimpleUserNode serverNode, UserNodeType type)
	{
		// append inmemory ds from modules
		if (serverNode.getType() == UserNodeType.INMEMORY_DATASOURCES)
		{
			serverNode.children = SolutionExplorerListContentProvider.createInMemTables(((MemServer)serverNode.getRealObject()).getServoyProject(),
				includeModules);
		}
		else if (serverNode.getType() == UserNodeType.VIEW_FOUNDSETS)
		{
			serverNode.children = SolutionExplorerListContentProvider.createViewFoundsets(((ViewFoundsetsServer)serverNode.getRealObject()).getServoyProject(),
				includeModules);
		}
		else
		{
			IServerInternal server = (IServerInternal)serverNode.getRealObject();
			handleServerNode(server, serverNode);
		}
		if (serverNode.children != null)
		{
			// can be null is server is disabled/invalid
			for (Object node : serverNode.children)
			{
				if (node instanceof SimpleUserNode)
				{
					((SimpleUserNode)node).parent = serverNode;
				}
			}
		}
	}

	public static Image getServerImage(final String serverName, final IServerInternal server)
	{
		final Image[] result = new Image[1];
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				String imgName = SERVER_IMAGE;
				if (!server.isValid())
				{
					result[0] = uiActivator.loadImageFromCache(SERVER_ERROR_IMAGE);
					if (result[0] == null)
					{
						Image IMG_ERROR = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage();
						if (IMG_ERROR != null)
						{
							result[0] = new DecorationOverlayIcon(uiActivator.loadImageFromBundle("server.png"), ImageDescriptor.createFromImage(IMG_ERROR),
								IDecoration.BOTTOM_LEFT).createImage();
							uiActivator.putImageInCache(SERVER_ERROR_IMAGE, result[0]);
						}
						else
						{
							ServoyLog.logWarning("Could not load the problem decorator for the database server " + serverName,
								new Exception("Cannot load server error image!"));
						}
					}
					return;
				}
				if (!server.getName().equals(serverName))
				{
					imgName = SERVER_DUPLICATE_IMAGE;
				}

				result[0] = uiActivator.loadImageFromBundle(imgName, !server.getConfig().isEnabled());
			}
		});
		return result[0];
	}

	private void handleServerNode(IServerInternal serverObj, PlatformSimpleUserNode node)
	{
		if (serverObj.getConfig().isEnabled() && serverObj.isValid())
		{
			Job job = Job.create("Background loading of tables for server " + serverObj.getName(), (ICoreRunnable)monitor -> {

				ArrayList<SimpleUserNode> nodes = new ArrayList<SimpleUserNode>();

				PlatformSimpleUserNode storedProceduresDataSources = new PlatformSimpleUserNode(Messages.TreeStrings_procedures, UserNodeType.PROCEDURES,
					null, "This node list the stored procedures of server that have this property enabled (see server editor)", serverObj,
					uiActivator.loadImageFromBundle("function.png"));
				storedProceduresDataSources.parent = node;
				nodes.add(storedProceduresDataSources);

				try
				{
					List<String> views = serverObj.getViewNames(true);
					if (views != null && views.size() > 0)
					{
						PlatformSimpleUserNode viewNode = new PlatformSimpleUserNode("Views", UserNodeType.VIEWS, "", "Views", serverObj,
							PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));
						viewNode.parent = node;
						nodes.add(viewNode);
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
				node.children = SolutionExplorerListContentProvider.createTables(serverObj, UserNodeType.TABLE, nodes);

				if (node.children.length == 1) node.children = new SimpleUserNode[0];// it contained only the procedures node we added above, no need to show it
				view.refreshTreeNodeFromModel(node);
			});
			job.setSystem(true);
			job.setPriority(Job.LONG);
			job.schedule();
			node.children = new UserNode[] { new UserNode("Loading...", UserNodeType.LOADING) };
		}
		else
		{
			node.children = null;
		}
	}

	private void addPluginsNodeChildren(PlatformSimpleUserNode pluginNode)
	{
		// java plugins + ng services

		// java plugins that don't change anymore once they were loaded
		ArrayList<PlatformSimpleUserNode> allPluginNodes = new ArrayList<>(getJavaPluginsNodeChildren(pluginNode));

		// ng services that can change
		WebObjectSpecification[] serviceSpecifications = NGUtils.getAllWebServiceSpecificationsThatCanBeAddedToJavaPluginsList(
			getServicesSpecProviderState());
		Arrays.sort(serviceSpecifications, new Comparator<WebObjectSpecification>()
		{
			@Override
			public int compare(WebObjectSpecification o1, WebObjectSpecification o2)
			{
				return o1.getScriptingName().compareTo(o2.getScriptingName());
			}

		});
		for (WebObjectSpecification spec : serviceSpecifications)
		{
			if (spec.isDeprecated()) continue;
			if (spec.getApiFunctions().size() != 0 || spec.getAllPropertiesNames().size() != 0)
			{
				Image icon = getIconFromSpec(spec, true);
				if (icon == null) icon = uiActivator.loadImageFromBundle("plugin.png");
				PlatformSimpleUserNode node = new PlatformSimpleUserNode(spec.getScriptingName(), UserNodeType.PLUGIN, spec,
					icon, WebServiceScriptable.class, new SimpleDeveloperFeedback(
						SolutionExplorerListContentProvider.PLUGIN_PREFIX + "." + spec.getScriptingName(), null,
						spec.getDescriptionProcessed(true, HtmlUtils::applyDescriptionMagic)));
				allPluginNodes.add(node);
				node.parent = pluginNode;
				this.addCustomTypesNodes(node, spec, null);
			}
		}
		pluginNode.children = allPluginNodes.toArray(new PlatformSimpleUserNode[allPluginNodes.size()]);

		// It may happen that the Plugins node was disabled before its children were added.
		if (pluginNode.isHidden()) pluginNode.hide();
		else pluginNode.unhide();

		view.refreshTreeNodeFromModel(pluginNode, false, false);
	}

	private ArrayList<PlatformSimpleUserNode> getJavaPluginsNodeChildren(PlatformSimpleUserNode pluginNode)
	{
		// we load these in background and keep them to not recreate them each time the ngservices change and require a refresh of the same parent "Plugins" tree node
		synchronized (pluginsLoadLock)
		{
			if (loadedJavaPluginNodes == null)
			{
				loadedJavaPluginNodes = new ArrayList<PlatformSimpleUserNode>();
				for (IClientPlugin plugin : Activator.getDefault().getDesignClient().getPluginManager().getPlugins(IClientPlugin.class))
				{
					try
					{
						IScriptable scriptObject = null;
						Method method = plugin.getClass().getMethod("getScriptObject", (Class[])null);
						if (method != null)
						{
							scriptObject = (IScriptable)method.invoke(plugin, (Object[])null);
						}
						if (scriptObject != null)
						{
							PlatformSimpleUserNode node = new PlatformSimpleUserNode(plugin.getName(), UserNodeType.PLUGIN, scriptObject, null,
								scriptObject.getClass(),
								new ScriptObjectFeedback(SolutionExplorerListContentProvider.PLUGIN_PREFIX + "." + plugin.getName(), scriptObject.getClass()))
							{
								@Override
								public Image getIcon()
								{
									Image image = super.getIcon();
									if (image == null)
									{
										if (plugin instanceof IIconProvider && ((IIconProvider)plugin).getIconUrl() != null)
										{
											URL urlLightTheme = ((IIconProvider)plugin).getIconUrl();

											boolean darkTheme = UIUtils.isDarkThemeSelected(false);
											if (darkTheme)
											{
												URL urlDarkTheme = null;
												String urlString = urlLightTheme.toString();
												urlString = UIUtils.replaceLast(urlString, ".", "_dark.");
												try
												{
													urlDarkTheme = new URL(urlString);
												}
												catch (MalformedURLException e)
												{
												}
												if (urlDarkTheme != null && ImageDescriptor.createFromURL(urlDarkTheme).getImageData(100) != null)
												{
													image = ImageDescriptor.createFromURL(urlDarkTheme).createImage();
												} // else use light theme/single plugin icon here as well
											}

											if (image == null)
											{
												image = ImageDescriptor.createFromURL(urlLightTheme).createImage();
											}
										}
										else
										{
											Icon icon = plugin.getImage();
											if (icon != null)
											{
												image = UIUtils.getSWTImageFromSwingIcon(icon, view.getSite().getShell().getDisplay(), 16, 16);
											}
										}
										if (image != null) pluginImages.add(image); // keeping a list so we can dispose them when they are not needed anymore
										if (image == null)
										{
											image = uiActivator.loadImageFromBundle("plugin.png");
										}
										setIcon(image);
										image = super.getIcon();
									}
									return image;
								}
							};
							if (view.isNonEmptyPlugin(node))
							{
								loadedJavaPluginNodes.add(node);
								node.parent = pluginNode;
								if (scriptObject instanceof IReturnedTypesProvider)
								{
									Class< ? >[] clss = ((IReturnedTypesProvider)scriptObject).getAllReturnedTypes();
									addReturnTypeNodesPlaceHolder(node, clss);
								}
							}
						}
					}
					catch (Throwable e)
					{
						ServoyLog.logError("Error loading plugin " + plugin.getName() + " exception: ", e);
						PlatformSimpleUserNode node = new PlatformSimpleUserNode(plugin.getName() + " (not loaded!)", UserNodeType.PLUGIN, null, null,
							e.toString(),
							null)
						{

							@Override
							public Image getIcon()
							{
								// we could give this image directly in constructor but as this call can do a Display.syncExec(...) and the constructor is
								// called inside a sync block that can happen on a separate load plugins thread - there is a risk of deadlock - so get this image later
								return uiActivator.loadImageFromBundle("warning.png");
							}

						};
						loadedJavaPluginNodes.add(node);
						node.parent = pluginNode;
					}
				}
			}
			return loadedJavaPluginNodes;
		}
	}

	private Image getIconFromSpec(WebObjectSpecification spec, boolean isService)
	{
		Image icon = null;
		if (spec.getIcon() != null)
		{
			try
			{
				boolean darkTheme = UIUtils.isDarkThemeSelected(false);
				if (!"file".equals(spec.getSpecURL().getProtocol()))
				{
					SpecProviderState specProvider = isService ? getServicesSpecProviderState() : getComponentsSpecProviderState();
					IPackageReader reader = specProvider.getPackageReader(spec.getPackageName());
					String iconPath = spec.getIcon().replaceFirst(spec.getPackageName() + "/", "");
					if (reader == null || reader.getUrlForPath(iconPath) == null)
					{
						ServoyLog.logError("Cannot get icon " + spec.getIcon() + " for " + spec.getName(), null);
						return null;
					}

					if (darkTheme)
					{
						String darkIconPath = UIUtils.replaceLast(iconPath, ".", "-dark.");
						if (reader.getUrlForPath(darkIconPath) != null)
						{
							iconPath = darkIconPath;
						}
					}
					IPath path = new Path(reader.getUrlForPath(iconPath).toURI().toString());
					icon = imageCache.get(path);
					if (icon == null)
					{
						URI u = reader.getResource().toURI();

						try (ZipFile zip = new ZipFile(new File(u)))
						{
							ZipEntry entry = zip.getEntry(iconPath);
							try (InputStream is = zip.getInputStream(entry))
							{
								icon = loadImageFromInputStream(is, path);
							}
						}
						catch (IOException e)
						{
							ServoyLog.logError(e);
						}
					}
				}
				else
				{
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(spec.getPackageName());
					if (project != null)
					{
						String iconPath = spec.getIcon().replaceFirst(spec.getPackageName(), "");
						if (darkTheme)
						{
							iconPath = UIUtils.replaceLast(iconPath, ".", "-dark.");
						}
						icon = loadImageFromProject(project, iconPath);
						if (darkTheme && icon == null)
						{
							icon = loadImageFromProject(project, spec.getIcon().replaceFirst(spec.getPackageName(), ""));
						}
					}
					else
					{
						IFolder folder = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getResourcesProject().getProject()
							.getFolder(
								isService ? SolutionSerializer.SERVICES_DIR_NAME : SolutionSerializer.COMPONENTS_DIR_NAME);
						icon = loadImageFromFolder(folder, spec.getIcon());
					}
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
		return icon;
	}

	private void addReturnTypeNodesPlaceHolder(PlatformSimpleUserNode node, Class< ? >[] clss)
	{
		if (clss != null)
		{
			node.children = new SimpleUserNode[] { new SimpleUserNode("placeholder", UserNodeType.RETURNTYPEPLACEHOLDER, clss, null) };
		}
	}

	private void addCustomTypesNodes(PlatformSimpleUserNode node, WebObjectSpecification spec, Form originalForm)
	{
		if (node != null && spec != null)
		{
			Map<String, ICustomType< ? >> customTypes = new HashMap<String, ICustomType< ? >>(spec.getDeclaredCustomObjectTypes());
			customTypes.values().removeIf(type -> WebFormComponent.isPrivateProperty(type.getCustomJSONTypeDefinition()));
			if (customTypes.size() > 0)
			{
				PlatformSimpleUserNode customTypesNode = new PlatformSimpleUserNode("CustomTypes", UserNodeType.CUSTOM_TYPE, spec, originalForm,
					uiActivator.loadImageFromBundle("components_package.png"));
				node.children = new SimpleUserNode[] { customTypesNode };

				List<SimpleUserNode> nodes = customTypes.entrySet().stream()
					.map(entry -> new PlatformSimpleUserNode(entry.getKey(), UserNodeType.CUSTOM_TYPE, entry.getValue(), originalForm,
						uiActivator.loadImageFromBundle("js.png")))
					.sorted((node1, node2) -> node1.getName().compareTo(node2.getName()))
					.collect(Collectors.toList());

				customTypesNode.children = nodes.toArray(new SimpleUserNode[0]);
			}
		}
	}

	private void addReturnTypeNodes(PlatformSimpleUserNode node, Class< ? >[] clss)
	{
		if (clss != null)
		{
			List<PlatformSimpleUserNode> children = new ArrayList<PlatformSimpleUserNode>();
			List<PlatformSimpleUserNode> constantsChildren = new ArrayList<PlatformSimpleUserNode>();
			List<PlatformSimpleUserNode> exceptionsChildren = new ArrayList<PlatformSimpleUserNode>();
			for (Class< ? > cls : clss)
			{
				if (cls != null && !IDeprecated.class.isAssignableFrom(cls) && !cls.isAnnotationPresent(Deprecated.class))
				{
					String nodeName = null;
					if (cls.isAnnotationPresent(ServoyDocumented.class))
					{
						ServoyDocumented sd = cls.getAnnotation(ServoyDocumented.class);
						if (sd.scriptingName() != null && sd.scriptingName().trim().length() > 0) nodeName = sd.scriptingName().trim();
					}
					if (nodeName == null)
					{
						int index = cls.getName().lastIndexOf(".");
						int index2 = cls.getName().indexOf("$", index);
						if (index2 != -1)
						{
							index = index2;
						}
						nodeName = cls.getName().substring(index + 1);
					}
					PlatformSimpleUserNode n = new PlatformSimpleUserNode(nodeName, UserNodeType.RETURNTYPE, cls, uiActivator.loadImageFromBundle("class.png"),
						cls, new ScriptObjectFeedback(null, cls));
					JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(cls, null);
					if (IConstantsObject.class.isAssignableFrom(cls) &&
						!(javaMembers instanceof InstanceJavaMembers && javaMembers.getMethodIds(false).size() > 0))
					{
						constantsChildren.add(n);
						n.setIcon(uiActivator.loadImageFromBundle("constant.png"));
					}
					else if (ServoyException.class.isAssignableFrom(cls))
					{
						exceptionsChildren.add(n);
					}
					else
					{
						children.add(n);
					}
					n.parent = node;
				}
			}

			PlatformSimpleUserNode constants = null;
			if (constantsChildren.size() > 0)
			{
				children.add(constants = new PlatformSimpleUserNode("Constants", UserNodeType.RETURNTYPE_CONSTANT, null,
					uiActivator.loadImageFromBundle("constant.png")));
				constants.parent = node;
			}

			if (constants != null)
			{
				constants.children = constantsChildren.toArray(new PlatformSimpleUserNode[constantsChildren.size()]);
				for (SimpleUserNode c : constants.children)
				{
					c.parent = constants;
				}
				constants.checkClientSupportInChildren();
			}

			PlatformSimpleUserNode exceptions = null;
			if (exceptionsChildren.size() > 0)
			{
				children.add(exceptions = new PlatformSimpleUserNode(Messages.TreeStrings_ServoyException, UserNodeType.EXCEPTIONS, null, null));
			}

			if (exceptions != null)
			{
				exceptions.children = exceptionsChildren.toArray(new PlatformSimpleUserNode[exceptionsChildren.size()]);
			}
			if (children.size() > 0)
			{
				Collections.sort(children, new Comparator<PlatformSimpleUserNode>()
				{
					public int compare(PlatformSimpleUserNode o1, PlatformSimpleUserNode o2)
					{
						return o1.getName().compareTo(o2.getName());
					}
				});
			}
			node.children = children.toArray(new PlatformSimpleUserNode[children.size()]);
		}
	}

	private void addSolutionNodeChildren(PlatformSimpleUserNode projectNode)
	{
		ServoyProject servoyProject = (ServoyProject)projectNode.getRealObject();
		Solution solution = servoyProject.getSolution();
		if (solution != null)
		{
			PlatformSimpleUserNode scopesFolder = new PlatformSimpleUserNode(Messages.TreeStrings_Scopes,
				solutionOfCalculation == null ? UserNodeType.SCOPES_ITEM : UserNodeType.SCOPES_ITEM_CALCULATION_MODE, solution,
				uiActivator.loadImageFromBundle("scopes.png"));
			scopesFolder.parent = projectNode;
			addScopesNodeChildren(scopesFolder);

			PlatformSimpleUserNode forms = new PlatformSimpleUserNode(Messages.TreeStrings_Forms, UserNodeType.FORMS, solution,
				uiActivator.loadImageFromBundle("forms.png"));
			forms.parent = projectNode;

			PlatformSimpleUserNode formReferences = new PlatformSimpleUserNode(Messages.TreeStrings_FormComponents, UserNodeType.COMPONENT_FORMS, solution,
				uiActivator.loadImageFromBundle("form_component.png"));
			formReferences.parent = projectNode;
			PlatformSimpleUserNode allRelations = null;
			if (solutionOfCalculation == null)
			{
				allRelations = new PlatformSimpleUserNode(Messages.TreeStrings_Relations, UserNodeType.ALL_RELATIONS, solution,
					uiActivator.loadImageFromBundle("relations.png"));
				allRelations.parent = projectNode;
			}
			PlatformSimpleUserNode valuelists = new PlatformSimpleUserNode(Messages.TreeStrings_ValueLists, UserNodeType.VALUELISTS, solution,
				uiActivator.loadImageFromBundle("valuelists.png"));
			valuelists.parent = projectNode;

			PlatformSimpleUserNode menus = new PlatformSimpleUserNode(Messages.TreeStrings_Menus, UserNodeType.MENUS, solution,
				uiActivator.loadImageFromBundle("column.png"));
			menus.parent = projectNode;

			PlatformSimpleUserNode media = new PlatformSimpleUserNode(Messages.TreeStrings_Media, UserNodeType.MEDIA, solution,
				uiActivator.loadImageFromBundle("media.png"));
			media.parent = projectNode;
			addMediaFolderChildrenNodes(media, solution);

			PlatformSimpleUserNode solutionDataSources = new PlatformSimpleUserNode(Messages.TreeStrings_SolutionDataSources, UserNodeType.SOLUTION_DATASOURCES,
				solution, IconProvider.instance().image(JSDataSources.class));
			solutionDataSources.parent = projectNode;


			PlatformSimpleUserNode solutionMemoryDataSources = new PlatformSimpleUserNode(Messages.TreeStrings_InMemory, UserNodeType.INMEMORY_DATASOURCES,
				servoyProject.getMemServer(), IconProvider.instance().image(JSDataSources.class));
			solutionMemoryDataSources.parent = solutionDataSources;

			PlatformSimpleUserNode viewFoundsets = new PlatformSimpleUserNode(Messages.TreeStrings_ViewFoundsets, UserNodeType.VIEW_FOUNDSETS,
				servoyProject.getViewFoundsetsServer(), IconProvider.instance().image(JSViewDataSource.class));
			viewFoundsets.parent = solutionDataSources;

			PlatformSimpleUserNode menuFoundsets = new PlatformSimpleUserNode(Messages.TreeStrings_MenuFoundsets, UserNodeType.MENU_FOUNDSETS,
				solution, IconProvider.instance().image(JSDataSources.class));
			menuFoundsets.parent = solutionDataSources;

			solutionDataSources.children = new PlatformSimpleUserNode[] { solutionMemoryDataSources, viewFoundsets, menuFoundsets };


			PlatformSimpleUserNode solutionWebPackages = new PlatformSimpleUserNode(Messages.TreeStrings_Web_Packages,
				UserNodeType.SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES, solution, uiActivator.loadImageFromBundle("all_packages.png"));
			solutionWebPackages.parent = projectNode;

			if (solutionOfCalculation != null)
			{
				// in case of calculation editor
				PlatformSimpleUserNode dataProvidersNode = new PlatformSimpleUserNode(Messages.TreeStrings_DataProviders, UserNodeType.TABLE_COLUMNS,
					tableOfCalculation, solution, uiActivator.loadImageFromBundle("selected_record.png"));
				allRelations = new PlatformSimpleUserNode(Messages.TreeStrings_Relations, UserNodeType.RELATIONS, tableOfCalculation,
					uiActivator.loadImageFromBundle("relations.png"));
				addRelationsNodeChildren(allRelations, solution, tableOfCalculation, UserNodeType.CALC_RELATION);

				dataProvidersNode.parent = projectNode;
				allRelations.parent = projectNode;
				projectNode.children = new PlatformSimpleUserNode[] { scopesFolder, dataProvidersNode, forms, formReferences, allRelations, valuelists, menus, media, solutionDataSources, solutionWebPackages };
				forms.hide();
				formReferences.hide();
			}
			else
			{
				projectNode.children = new PlatformSimpleUserNode[] { scopesFolder, forms, formReferences, allRelations, valuelists, menus, media, solutionDataSources, solutionWebPackages };
				// solution's allRelations not allowed in login solutions
				if (activeSolutionNode != null &&
					((ServoyProject)activeSolutionNode.getRealObject()).getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION &&
					allRelations != null)
				{
					allRelations.hide();
				}
			}
		}
	}

	private void addMediaFolderChildrenNodes(PlatformSimpleUserNode mediaFolder, IMediaProvider mediaProvider)
	{
		if (mediaFolder != null)
		{
			MediaNode folderNode = (mediaFolder.getType() == UserNodeType.MEDIA_FOLDER) ? (MediaNode)mediaFolder.getRealObject()
				: new MediaNode(null, null, MediaNode.TYPE.FOLDER, mediaProvider);

			mediaFolder.children = view.createMediaFolderChildrenNodes(folderNode, uiActivator, EnumSet.of(MediaNode.TYPE.FOLDER));
			for (SimpleUserNode n : mediaFolder.children)
			{
				n.parent = mediaFolder;
			}
		}
	}

	private void addScopesNodeChildren(PlatformSimpleUserNode parent)
	{
		Solution solution = (Solution)parent.getRealObject();
		SimpleUserNode project = parent.getAncestorOfType(ServoyProject.class);
		if (project == null)
		{
			return;
		}

		// property Solution.SCOPE_NAMES is maintained by ServoyModel based on global js file names
		String[] scopeNames = solution.getRuntimeProperty(Solution.SCOPE_NAMES);
		List<PlatformSimpleUserNode> nodes = new ArrayList<PlatformSimpleUserNode>(scopeNames == null ? 1 : (scopeNames.length + 1));
		if (scopeNames != null) // when refreshScopes has not been called yet
		{
			for (String scopeName : scopeNames)
			{
				Pair<Solution, String> solutionAndScope = new Pair<Solution, String>(solution, scopeName);
				PlatformSimpleUserNode globalsFolder = new PlatformSimpleUserNode(scopeName, UserNodeType.GLOBALS_ITEM, solutionAndScope,
					uiActivator.loadImageFromBundle("globals.png"));
				globalsFolder.parent = parent;
				nodes.add(globalsFolder);
				addGlobalsNodeChildren(globalsFolder, solutionAndScope);
			}
		}

		parent.children = nodes.toArray(new PlatformSimpleUserNode[nodes.size()]);
	}

	private void addGlobalsNodeChildren(PlatformSimpleUserNode globalsFolder, Pair<Solution, String> solutionAndScope)
	{
		PlatformSimpleUserNode globalVariables = new PlatformSimpleUserNode(Messages.TreeStrings_variables, UserNodeType.GLOBAL_VARIABLES, solutionAndScope,
			uiActivator.loadImageFromBundle("variable_global.png"));
		globalVariables.parent = globalsFolder;

		PlatformSimpleUserNode globalRelations = new PlatformSimpleUserNode(Messages.TreeStrings_relations, UserNodeType.GLOBALRELATIONS, solutionAndScope,
			uiActivator.loadImageFromBundle("relations.png"));
		addGlobalRelationsNodeChildren(globalRelations);
		globalRelations.parent = globalsFolder;
		globalsFolder.children = new PlatformSimpleUserNode[] { globalVariables, globalRelations };

		// globals relations not allowed in login solution

		if (activeSolutionNode != null &&
			((ServoyProject)activeSolutionNode.getRealObject()).getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION &&
			solutionOfCalculation == null) globalRelations.hide();
	}

	private void addWorkingSetNodeChildren(PlatformSimpleUserNode workingSetNode)
	{
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject() != null && workingSetNode != null &&
			workingSetNode.getSolution() != null)
		{
			List<String> forms = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().getWorkingSetPersists(
				workingSetNode.getName(), new String[] { workingSetNode.getSolution().getName() });
			if (forms != null && !forms.isEmpty())
			{
				List<PlatformSimpleUserNode> nodes = new ArrayList<PlatformSimpleUserNode>();
				for (String formName : forms)
				{
					boolean isFormComponentWSParent = workingSetNode.parent.getType() == UserNodeType.COMPONENT_FORMS;
					Form form = workingSetNode.getSolution().getForm(formName);
					if (form != null && (form.isFormComponent().booleanValue() == isFormComponentWSParent))
					{
						addFormNode(form, nodes, workingSetNode);
					}
				}
				workingSetNode.setChildren(nodes.toArray(new PlatformSimpleUserNode[nodes.size()]));
			}
		}
	}

	private void addFormNode(Form f, List<PlatformSimpleUserNode> nodes, PlatformSimpleUserNode parentNode)
	{
		PlatformSimpleUserNode node = new PlatformSimpleUserNode(f.getName(), UserNodeType.FORM, f.getName(),
			f.getDataSource() == null ? "No table" : ("Server: " + f.getServerName() + ", Table: " + f.getTableName()), f,
			ElementUtil.getImageForFormEncapsulation(f));
		nodes.add(node);
		node.parent = parentNode;
	}

	private void addFormsNodeChildren(PlatformSimpleUserNode formsNode, boolean referenceForms)
	{
		Solution solution = (Solution)formsNode.getRealObject();
		List<PlatformSimpleUserNode> nodes = new ArrayList<PlatformSimpleUserNode>();
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject() != null)
		{
			List<String> workingSets = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().getServoyWorkingSets(
				new String[] { solution.getName() });
			if (workingSets != null)
			{
				for (String workingSet : workingSets)
				{
					if (!referenceForms ||
						(referenceForms && (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject() != null &&
							ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().hasPersistsInServoyWorkingSets(workingSet,
								new String[] { solution.getName() }, true))))
					{
						PlatformSimpleUserNode node = new PlatformSimpleUserNode(workingSet, UserNodeType.WORKING_SET, null, solution,
							uiActivator.loadImageFromBundle("servoy_workingset.png"));
						nodes.add(node);
						node.parent = formsNode;
					}
				}
			}
		}
		Iterator<Form> it = null;
		if (!referenceForms) it = solution.getAllNormalForms(true);
		else it = solution.getAllComponentForms(true);
		while (it.hasNext())
		{
			Form f = it.next();
			if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject() == null ||
				!ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().isContainedInWorkingSets(f.getName(),
					new String[] { solution.getName() }))
			{
				addFormNode(f, nodes, formsNode);
			}
		}
		formsNode.setChildren(nodes.toArray(new PlatformSimpleUserNode[nodes.size()]));
	}

	private void addFormNodeChildren(PlatformSimpleUserNode formNode)
	{
		Form f = (Form)formNode.getRealObject();

		// no scripting for form references
		if (f.isFormComponent()) return;

		List<PlatformSimpleUserNode> node = new ArrayList<>();
		PlatformSimpleUserNode functionsNode = new PlatformSimpleUserNode(Messages.TreeStrings_controller, UserNodeType.FORM_CONTROLLER, f,
			uiActivator.loadImageFromBundle("controller.png"));
		functionsNode.parent = formNode;
		node.add(functionsNode);

		PlatformSimpleUserNode variables = new PlatformSimpleUserNode(Messages.TreeStrings_variables, UserNodeType.FORM_VARIABLES, f,
			uiActivator.loadImageFromBundle("variables.png"));
		variables.parent = formNode;
		node.add(variables);

		PlatformSimpleUserNode elementsNode = new PlatformSimpleUserNode(Messages.TreeStrings_elements, UserNodeType.FORM_ELEMENTS, f,
			uiActivator.loadImageFromBundle("elements.png"));
		elementsNode.parent = formNode;
		node.add(elementsNode);
		addFormElementsChildren(elementsNode);

		if (f.isResponsiveLayout())
		{
			PlatformSimpleUserNode containersNode = new PlatformSimpleUserNode(Messages.TreeStrings_containers, UserNodeType.FORM_CONTAINERS, f,
				uiActivator.loadImageFromBundle("layoutcontainer.png"));
			containersNode.parent = formNode;
			node.add(containersNode);
			addFormContainersChildren(containersNode);
		}


		if (f.getDataSource() != null)
		{
			IDataSourceManager dsm = ServoyModelFinder.getServoyModel().getDataSourceManager();
			PlatformSimpleUserNode columnsNode = null;
//			try
			{
				columnsNode = new PlatformSimpleUserNode(Messages.TreeStrings_selectedrecord, UserNodeType.TABLE_COLUMNS, dsm.getDataSource(f.getDataSource()),
					f, uiActivator.loadImageFromBundle("selected_record.png"));
				columnsNode.parent = formNode;
				node.add(columnsNode);
			}

			PlatformSimpleUserNode relationsNode = new PlatformSimpleUserNode(Messages.TreeStrings_relations, UserNodeType.RELATIONS, f, f,
				uiActivator.loadImageFromBundle("relations.png"));
			relationsNode.parent = formNode;
			node.add(relationsNode);
			addFormRelationsNodeChildren(relationsNode);

			// columns & relations not allowed in login solution
			if (activeSolutionNode != null &&
				((ServoyProject)activeSolutionNode.getRealObject()).getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION &&
				solutionOfCalculation == null)
			{
				if (columnsNode != null) columnsNode.hide();
				relationsNode.hide();
			}
		}
		formNode.setChildren(node.toArray(new PlatformSimpleUserNode[node.size()]));
	}

	private void disableServer(String serverName)
	{
		try
		{
			if (unreachableServers.contains(serverName))
			{
				if (MessageDialog.openConfirm(view.getSite().getShell(), "Disable server",
					"Cannot connect to server " + serverName + ". Do you want to disable it?"))
				{
					IServerInternal server = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(serverName, true, false);
					if (server != null)
					{
						EnableServerAction.setServerEnabled(view.getSite().getShell(), server, false);
					}
				}
				else
				{
					unreachableServers.remove(serverName);
				}

			}
			else
			{
				unreachableServers.add(serverName);
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}

	}

	private void addFormContainersChildren(PlatformSimpleUserNode elementsNode)
	{
		Form form = (Form)elementsNode.getRealObject();
		FlattenedSolution flattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();
		Set<String> allLayoutNames = ContainersScope.getAllLayoutNames(
			flattenedSolution.getFlattenedForm(form), flattenedSolution);
		List<PlatformSimpleUserNode> elements = new SortedList<PlatformSimpleUserNode>(StringComparator.INSTANCE);
		for (String name : allLayoutNames)
		{
			PlatformSimpleUserNode node = new PlatformSimpleUserNode(name, UserNodeType.FORM_CONTAINERS_ITEM, null, null,
				uiActivator.loadImageFromBundle("layoutcontainer.png"));
			node.setDeveloperFeedback(new SimpleDeveloperFeedback(name + ".", null, null));
			elements.add(node);
			node.parent = elementsNode;
		}
		elementsNode.children = elements.toArray(new PlatformSimpleUserNode[elements.size()]);
	}

	private void addFormElementsChildren(PlatformSimpleUserNode elementsNode)
	{
		Form form = (Form)elementsNode.getRealObject();
		addFormElementsChildren(elementsNode, new HashSet<>(), form, form);
	}

	private void addFormElementsChildren(PlatformSimpleUserNode elementsNode, Set<Form> inspectedForms, Form originalForm, Form ancestorForm)
	{
		inspectedForms.add(ancestorForm);

		// see if this form extends another form and if so add the parent's elements
		PlatformSimpleUserNode parentElementsNode = null;
		if (ancestorForm.getExtendsID() > 0)
		{
			Form parentForm = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getForm(ancestorForm.getExtendsID());
			if (parentForm != null)
			{
				if (!inspectedForms.contains(parentForm))
				{
					parentElementsNode = new PlatformSimpleUserNode(parentForm.getName(), UserNodeType.FORM_ELEMENTS_INHERITED, null,
						uiActivator.loadImageFromBundle("elements.png"));

					addFormElementsChildren(parentElementsNode, inspectedForms, originalForm, parentForm);
				}
				else
				{
					ServoyLog.logWarning("Cycle detected in form hierarchy - at form " + parentForm.getName(), null);
				}
			}
			else
			{
				ServoyLog.logWarning("Parent of extended form " + ancestorForm.getName() + " not found", null);
			}
		}
		List<PlatformSimpleUserNode> elements = new SortedList<>(StringComparator.INSTANCE);

		// get all objects ordered alphabetically by name
		List<IFormElement> formElements = ancestorForm.getFlattenedObjects(NameComparator.INSTANCE);

		boolean mobile = SolutionMetaData.isServoyMobileSolution(
			ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getSolution());

		// all named form elements must be added, as well as named fields inside
		// portal elements
		PlatformSimpleUserNode node;
		for (IFormElement element : formElements)
		{
			PlatformSimpleUserNode parentNode = elementsNode;
			// TODO: fix multiple anonymous groups (use proper content providers and label providers)
			if (element.getGroupID() != null && !mobile)
			{
				String groupName = FormElementGroup.getName(element.getGroupID());
				String groupLabel = groupName == null ? Messages.LabelAnonymous : groupName;
				node = null;
				for (PlatformSimpleUserNode el : elements)
				{
					if (groupLabel.equals(el.getName()))
					{
						node = el;
						break;
					}
				}
				if (node == null)
				{
					node = new PlatformSimpleUserNode(groupLabel, UserNodeType.FORM_ELEMENTS_GROUP,
						new Object[] { new FormElementGroup(element.getGroupID(),
							ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution(), (Form)element.getParent()), null },
						originalForm, uiActivator.loadImageFromBundle("group.png"));
					node.setDeveloperFeedback(new SimpleDeveloperFeedback(element.getName() + ".", null, null));
					elements.add(node);
					node.parent = parentNode;
				}
				parentNode = node; // this field comes under the group node
			}
			// Do not show override elements here, they will be shown on the super-form in the tree
			if (!isOverrideElement(element) && element.getName() != null && element.getName().trim().length() > 0)
			{
				if (element instanceof Bean)
				{
					// might need to add the sub-type children to node (such as
					// JComponent for a JSplitPane bean)
					// as well as return types (in case of beans that implement
					// IScriptObject)
					node = addBeanAndBeanChildNodes((Bean)element);
					if (node == null) continue;
				}
				else
				{
					Object model;
					if (element instanceof Portal && ((Portal)element).isMobileInsetList())
					{
						model = MobileListModel.create((Form)element.getParent(), (Portal)element);
					}
					else if (mobile && element.getGroupID() != null)
					{
						model = new FormElementGroup(element.getGroupID(), ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution(),
							(Form)element.getParent());
					}
					else
					{
						// If the element was extended on the original form, use that one as model
						model = originalForm.searchForExtendsId(element.getID()).orElse(element);
					}

					String displayName = element.getName();
					if (isOverrideElement(model))
					{
						displayName = Messages.labelOverride(displayName);
					}

					node = new PlatformSimpleUserNode(displayName, UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { model, null }, originalForm,
						uiActivator.loadImageFromBundle("element.png"));

					WebObjectSpecification spec = null;
					if (model instanceof IFormElement)
					{
						String webComponentClassName = FormTemplateGenerator.getComponentTypeName((IFormElement)model);
						spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(webComponentClassName);
						if (spec != null)
						{
							SolutionExplorerListContentProvider.extractApiDocs(spec); // so that we have the main description of the component available in spec
						}
						if (model instanceof WebComponent)
						{
							this.addCustomTypesNodes(node, spec, originalForm);
						}
					}
					node.setDeveloperFeedback(
						new SimpleDeveloperFeedback("elements." + element.getName() + ".", null,
							spec != null ? spec.getDescriptionProcessed(true, HtmlUtils::applyDescriptionMagic) : null));
				}
				elements.add(node);
				node.parent = parentNode;
			}
			if (element instanceof Portal && !((Portal)element).isMobileInsetList())
			{
				// find named child elements
				Iterator<IPersist> portalElementIterator = ((Portal)element).getAllObjects();
				while (portalElementIterator.hasNext())
				{
					IPersist oo = portalElementIterator.next();
					if (oo instanceof IFormElement)
					{
						IFormElement portalElement = (IFormElement)oo;
						if (portalElement.getName() != null && portalElement.getName().trim().length() > 0)
						{
							node = new PlatformSimpleUserNode(portalElement.getName(), UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { portalElement, null },
								originalForm, uiActivator.loadImageFromBundle("element.png"));
							elements.add(node);
							node.parent = parentNode;
						}
					}
				}
			}
		}

		// determine the children for each node
		Map<SimpleUserNode, List<SimpleUserNode>> children = new HashMap<SimpleUserNode, List<SimpleUserNode>>(elements.size());
		for (SimpleUserNode element : elements)
		{
			List<SimpleUserNode> lst = children.get(element.parent);
			if (lst == null)
			{
				lst = new ArrayList<SimpleUserNode>();
				children.put(element.parent, lst);
			}
			lst.add(element);
		}

		// add the child nodes to their parents
		elements.add(elementsNode);
		Object elementRealObject;
		for (SimpleUserNode element : elements)
		{
			elementRealObject = element.getRealObject();
			if (elementRealObject instanceof Object[] && ((Object[])elementRealObject).length > 0 && ((Object[])elementRealObject)[0] instanceof Bean) continue; // children already added
			List<SimpleUserNode> nodeChildren = children.get(element);
			int i;
			if (element == elementsNode && parentElementsNode != null && parentElementsNode.children != null && parentElementsNode.children.length > 0)
			{
				element.children = new PlatformSimpleUserNode[nodeChildren == null ? 1 : (nodeChildren.size() + 1)];
				element.children[0] = parentElementsNode;
				parentElementsNode.parent = element;
				i = 1;
			}
			else
			{
				if (nodeChildren != null)
				{
					element.children = new PlatformSimpleUserNode[nodeChildren.size()];
				}
				i = 0;
			}
			if (nodeChildren != null)
			{
				for (SimpleUserNode n : nodeChildren)
				{
					element.children[i++] = n;
				}
			}
		}
	}

	private PlatformSimpleUserNode addBeanAndBeanChildNodes(Bean bean)
	{
		PlatformSimpleUserNode node;
		try
		{
			IApplication application = Activator.getDefault().getDesignClient();

			if (FormTemplateGenerator.isWebcomponentBean(bean)) return createNodeForWebComponentBean(bean);

			Class< ? > beanClass = ElementUtil.getPersistScriptClass(Activator.getDefault().getDesignClient(), bean);
			if (beanClass == null)
			{
				return null;
			}
			// we have now two situations:
			// 1. bean with js_... stuff in it where these must be used and no
			// sub-types must be shown => InstanceJavaMembers
			// 2. standard beans (without js_... stuff) that will show sub-types =>
			// JavaMembers or something similar
			JavaMembers jm = null;
			if (application != null)
			{
				jm = ScriptObjectRegistry.getJavaMembers(beanClass, null);
			}
			if (jm != null && jm.getClass() != InstanceJavaMembers.class)
			{
				// show all sub-types
				String className = beanClass.getSimpleName();
				node = new PlatformSimpleUserNode(bean.getName() + " (" + className + ')', UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { bean, beanClass },
					bean.getParent(), uiActivator.loadImageFromBundle("element.png"));
				node.setDeveloperFeedback(new SimpleDeveloperFeedback(bean.getName() + ".", null, null));

				Class< ? > superClass = beanClass.getSuperclass();
				PlatformSimpleUserNode parentClassNode = node;
				PlatformSimpleUserNode currentClassNode;
				while (superClass != null)
				{
					className = superClass.getSimpleName();
					currentClassNode = new PlatformSimpleUserNode(className, UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { bean, superClass },
						bean.getParent(), uiActivator.loadImageFromBundle("element.png"));
					currentClassNode.setDeveloperFeedback(new SimpleDeveloperFeedback(bean.getName() + ".", null, null));
					currentClassNode.parent = parentClassNode;
					parentClassNode.children = new PlatformSimpleUserNode[] { currentClassNode };
					parentClassNode = currentClassNode;
					superClass = superClass.getSuperclass();
				}
			}
			else
			{
				// do not show subtypes
				node = new PlatformSimpleUserNode(bean.getName(), UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { bean, null }, bean.getParent(),
					uiActivator.loadImageFromBundle("element.png"));
				node.setDeveloperFeedback(new SimpleDeveloperFeedback(bean.getName() + ".", null, null));
			}

			// add returnTypes if needed
			IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(beanClass);
			if ((scriptObject == null || scriptObject.getAllReturnedTypes() == null) && IReturnedTypesProvider.class.isAssignableFrom(beanClass))
			{
				final Class< ? >[] allReturnedTypes = ((IReturnedTypesProvider)beanClass.newInstance()).getAllReturnedTypes();
				ScriptObjectRegistry.registerReturnedTypesProviderForClass(beanClass, new IReturnedTypesProvider()
				{

					public Class< ? >[] getAllReturnedTypes()
					{
						return allReturnedTypes;
					}
				});
				scriptObject = ScriptObjectRegistry.getScriptObjectForClass(beanClass);
			}
			if (scriptObject != null)
			{
				addReturnTypeNodesPlaceHolder(node, scriptObject.getAllReturnedTypes());
			}
		}
		catch (Throwable e)
		{
			ServoyLog.logWarning("Solution explorer cannot create bean " + bean.getName(), e);
			node = new PlatformSimpleUserNode(bean.getName(), UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { bean, null }, bean.getParent(),
				uiActivator.loadImageFromBundle("element.png"));
			node.setDeveloperFeedback(new SimpleDeveloperFeedback(bean.getName() + ".", null, null));
		}
		return node;
	}

	private PlatformSimpleUserNode createNodeForWebComponentBean(Bean bean)
	{
		PlatformSimpleUserNode node = new PlatformSimpleUserNode(bean.getName(), UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { bean, null }, bean.getParent(),
			uiActivator.loadImageFromBundle("element.png"));
		node.setDeveloperFeedback(new SimpleDeveloperFeedback(bean.getName() + ".", null, null));
		return node;
	}

	private void addGlobalRelationsNodeChildren(PlatformSimpleUserNode globalRelations)
	{
		Pair<Solution, String> solutionAndScope = (Pair<Solution, String>)globalRelations.getRealObject();
		try
		{
			List<PlatformSimpleUserNode> rels = new ArrayList<PlatformSimpleUserNode>();
			Iterator<Relation> it = solutionAndScope.getLeft().getRelations(null, true, true); // returns all global relations
			while (it.hasNext())
			{
				Relation r = it.next();
				if (r.usesScope(solutionAndScope.getRight()))
				{
					PlatformSimpleUserNode un = new PlatformSimpleUserNode(r.getName(), UserNodeType.RELATION, r,
						RelationLabelProvider.INSTANCE_ALL.getImage(r));
					un.parent = globalRelations;
					rels.add(un);
				}
			}
			globalRelations.children = rels.toArray(new PlatformSimpleUserNode[rels.size()]);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	private void addFormRelationsNodeChildren(PlatformSimpleUserNode formRelationsNode)
	{
		Form f = (Form)formRelationsNode.getRealObject();
		ITable table = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(f.getDataSource());
		addRelationsNodeChildren(formRelationsNode, f.getSolution(), table, UserNodeType.RELATION);
	}

	private void addRelationsNodeChildren(PlatformSimpleUserNode relationsNode, Solution solution, ITable table, UserNodeType type)
	{
		try
		{
			Map<String, Solution> allSolutions = new HashMap<String, Solution>();

			solution.getReferencedModulesRecursive(allSolutions);

			allSolutions.put(solution.getName(), solution);

			List<PlatformSimpleUserNode> relationNodes = new ArrayList<PlatformSimpleUserNode>();
			TreeSet<Relation> relations = new TreeSet<Relation>(NameComparator.INSTANCE);
			for (Solution sol : allSolutions.values())
			{
				Iterator<Relation> it = sol.getRelations(table, true, false);

				while (it.hasNext())
				{
					Relation r = it.next();
					if (!r.isGlobal()) //  && !relations.contains(r) todo should it not be the contentEquals
					{
						relations.add(r);
					}
				}
			}

			for (Relation r : relations)
			{
				String displayName;
				Solution rootSolution = (Solution)r.getRootObject();
				if (solution == rootSolution) displayName = r.getName();
				else displayName = r.getName() + " [" + rootSolution.getName() + "]";
				PlatformSimpleUserNode un = new PlatformSimpleUserNode(displayName, type, r, solution, RelationLabelProvider.INSTANCE_ALL.getImage(r));
				un.parent = relationsNode;
				relationNodes.add(un);
			}

			relationsNode.children = relationNodes.toArray(new PlatformSimpleUserNode[relationNodes.size()]);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	public void refreshContent(Map<IPersist, Set<Class< ? extends IPersist>>> persists)
	{
		List<String> solutionsRefreshedForRelations = new ArrayList<String>();
		for (Entry<IPersist, Set<Class< ? extends IPersist>>> entry : persists.entrySet())
		{
			IPersist persist = entry.getKey();
			IRootObject root = persist.getRootObject();
			boolean refreshedFormsNode = false;

			if (persist instanceof IFormElement || persist instanceof LayoutContainer)
			{
				// don't refresh if we also refresh the solution
				if (persists.containsKey(root)) continue;

				IPersist parent = persist.getAncestor(IRepository.FORMS);
				if (parent instanceof Form)
				{
					// if the element's form was extended by other forms, the elements of those forms must be refreshed as well...
					FlattenedSolution flatSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();
					HashSet<Class< ? extends IPersist>> set = new HashSet<>();
					set.add(persist.getClass());
					refreshElementsForForm(flatSolution, (Form)parent, new HashSet<Form>(), set);
				}
			}
			else
			{
				if (root instanceof Solution)
				{
					Solution s = (Solution)root;
					PlatformSimpleUserNode node = getSolutionNode(s.getName());

					if (node != null)
					{
						// find the node and refresh it
						if (persist instanceof Relation && ((Relation)persist).getName() != null)
						{
							if (!solutionsRefreshedForRelations.contains(s.getName()))
							{
								// refresh global relations node - if possible
								node = findChildNode(node, Messages.TreeStrings_Scopes);
								if (node != null)
								{
									String[] scopeNames = ((Solution)root).getRuntimeProperty(Solution.SCOPE_NAMES);
									if (scopeNames != null) // when refreshScopes has not been called yet
									{
										PlatformSimpleUserNode scopeNode = null;
										for (String scopeName : scopeNames)
										{
											scopeNode = findChildNode(node, scopeName);
											if (scopeNode != null)
											{
												scopeNode = findChildNode(scopeNode, Messages.TreeStrings_relations);
											}
											if (scopeNode != null)
											{
												addGlobalRelationsNodeChildren(scopeNode);
												view.refreshTreeNodeFromModel(scopeNode);
											}
										}
									}
								}
							}
							if (!solutionsRefreshedForRelations.contains(s.getName()))
							{
								// refresh all affected form relation nodes
								node = findChildNode(getSolutionNode(s.getName()), Messages.TreeStrings_Forms);
								if (node != null && node.children != null)
								{
									PlatformSimpleUserNode relationsNode;
									Form form;
									for (int i = node.children.length - 1; i >= 0; i--)
									{
										form = (Form)node.children[i].getRealObject();
										if (form.getDataSource() != null)
										{
											relationsNode = findChildNode(node.children[i], Messages.TreeStrings_relations);
											if (relationsNode != null)
											{
												addFormRelationsNodeChildren(relationsNode);
												view.refreshTreeNodeFromModel(relationsNode);
											}
										}
									}
								}
								// if in calculation mode, refresh Relations node under
								// solution node
								if (solutionOfCalculation != null)
								{
									node = findChildNode(getSolutionNode(s.getName()), Messages.TreeStrings_Relations);
									if (node != null && tableOfCalculation.equals(
										ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(((Relation)persist).getPrimaryDataSource())))
									{
										addRelationsNodeChildren(node, solutionOfCalculation, tableOfCalculation, UserNodeType.CALC_RELATION);
										view.refreshTreeNodeFromModel(node);
									}
								}
							}
							if (!solutionsRefreshedForRelations.contains(s.getName())) solutionsRefreshedForRelations.add(s.getName());
						}
						else if (persist instanceof Form)
						{
							// don't refresh if we also refresh the solution
							if (persists.containsKey(s)) continue;
							boolean formAsComponent = ((Form)persist).isFormComponent().booleanValue();

							if (formAsComponent) node = findChildNode(node, Messages.TreeStrings_FormComponents);
							else node = findChildNode(node, Messages.TreeStrings_Forms);

							if (node != null)
							{
								PlatformSimpleUserNode formNode = findChildNode(node, ((Form)persist).getName());
								if (formNode == null)
								{
									if (!refreshedFormsNode)
									{
										refreshedFormsNode = true;
										addFormsNodeChildren(node, formAsComponent);
									}
									else
									{
										node = null;
									}
								}
								else
								{
									node = formNode;
									if (entry.getValue() == null)
									{
										node.children = null;
									}
									else
									{
										// for now this is just layoutcontainers or formelements
										// if the element's form was extended by other forms, the elements of those forms must be refreshed as well...
										FlattenedSolution flatSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();
										refreshElementsForForm(flatSolution, (Form)persist, new HashSet<Form>(), entry.getValue());
									}
								}
								if (node != null)
								{
									view.refreshTreeNodeFromModel(node);
								}
							}
						}
						else if (persist instanceof TableNode)
						{
							// refresh the in mem datasource tree.
							if (DataSourceUtils.getInmemDataSourceName(((TableNode)persist).getDataSource()) != null)
							{
								PlatformSimpleUserNode solutionChildNode = findChildNode(node,
									Messages.TreeStrings_SolutionDataSources);
								PlatformSimpleUserNode inMemNode = findChildNode(solutionChildNode, Messages.TreeStrings_InMemory);
								if (inMemNode != null)
								{
									view.refreshTreeNodeFromModel(inMemNode, true);
								}
							}
							else if (DataSourceUtils.getViewDataSourceName(((TableNode)persist).getDataSource()) != null)
							{
								PlatformSimpleUserNode solutionChildNode = findChildNode(node,
									Messages.TreeStrings_SolutionDataSources);
								PlatformSimpleUserNode inMemNode = findChildNode(solutionChildNode, Messages.TreeStrings_ViewFoundsets);
								if (inMemNode != null)
								{
									view.refreshTreeNodeFromModel(inMemNode, true);
								}
							}

						}
						else if (persist instanceof Solution)
						{
							PlatformSimpleUserNode solutionChildNode = findChildNode(node, Messages.TreeStrings_Forms);
							if (solutionChildNode != null)
							{
								addFormsNodeChildren(solutionChildNode, false);
								view.refreshTreeNodeFromModel(solutionChildNode);
							}
							solutionChildNode = findChildNode(node, Messages.TreeStrings_FormComponents);
							if (solutionChildNode != null)
							{
								addFormsNodeChildren(solutionChildNode, true);
								view.refreshTreeNodeFromModel(solutionChildNode);
							}
							solutionChildNode = findChildNode(node, Messages.TreeStrings_Media);
							if (solutionChildNode != null)
							{
								addMediaFolderChildrenNodes(solutionChildNode, (Solution)persist);
								view.refreshTreeNodeFromModel(solutionChildNode);
							}
						}
						else if (persist instanceof Menu)
						{
							PlatformSimpleUserNode menusNode = findChildNode(node, Messages.TreeStrings_Menus);
							addMenusNodeChildren(menusNode, UserNodeType.MENU, true);
							view.refreshTreeNodeFromModel(menusNode);

							PlatformSimpleUserNode menuFoundsetsNode = findChildNode(node, Messages.TreeStrings_MenuFoundsets);
							addMenusNodeChildren(menuFoundsetsNode, UserNodeType.MENU_FOUNDSET, false);
							view.refreshTreeNodeFromModel(menuFoundsetsNode);
						}
					}
				}
			}
		}
	}

	private void refreshElementsForForm(FlattenedSolution flatSolution, Form form, HashSet<Form> alreadyVisitedForms, Set<Class< ? extends IPersist>> set)
	{
		alreadyVisitedForms.add(form);
		// see children of form
		if (flatSolution != null)
		{
			for (Form childForm : flatSolution.getDirectlyInheritingForms(form))
			{
				if (!alreadyVisitedForms.contains(childForm))
				{
					refreshElementsForForm(flatSolution, childForm, alreadyVisitedForms, set);
				}
				else
				{
					ServoyLog.logWarning("Cycle in form hierarchy at form " + childForm.getName(), null);
				}
			}
		}

		IRootObject root = form.getRootObject();
		if (root instanceof Solution)
		{
			Solution s = (Solution)root;
			PlatformSimpleUserNode node = getSolutionNode(s.getName());

			if (node != null)
			{
				// find the elements node and refresh it
				node = findChildNode(node, Messages.TreeStrings_Forms);
				if (node != null)
				{
					node = findChildNode(node, form.getName());
					if (node != null)
					{
						if (set.stream().anyMatch(cls -> IFormElement.class.isAssignableFrom(cls)))
						{
							PlatformSimpleUserNode elements = findChildNode(node, Messages.TreeStrings_elements);
							if (elements != null)
							{
								addFormElementsChildren(elements);
								view.refreshTreeNodeFromModel(elements);
							}
						}
						if (set.contains(LayoutContainer.class))
						{
							PlatformSimpleUserNode containers = findChildNode(node, Messages.TreeStrings_containers);
							if (containers != null)
							{
								addFormContainersChildren(containers);
								view.refreshTreeNodeFromModel(containers);
							}
						}
					}
				}
			}
		}
	}

	public <T extends SimpleUserNode> T findChildNode(SimpleUserNode node, String name)
	{
		if (node != null && node.children != null)
		{
			for (int i = node.children.length - 1; i >= 0; i--)
			{
				String nodeName = node.children[i].getName();
				if (nodeName != null && nodeName.equals(name))
				{
					return (T)node.children[i];
				}
			}
		}
		return null;
	}

	/**
	 * Starts calculation mode. The tree contents will now change to supply only what is available for the calculations of the given table stored in the given
	 * solution. This will only make available parts of the "Solutions" node: only global variables of solution and it's submodules + relations (global or of
	 * the given table) stored in the solution and it's submodules.
	 *
	 * @param solution the solution in which the calculation(s) is(are) stored.
	 * @param table the table that has the calculation(s).
	 */
	public void startCalculationMode(Solution solution, ITable table)
	{
		solutionOfCalculation = solution;
		tableOfCalculation = table;

		CalculationModeHandler cm = CalculationModeHandler.getInstance();
		for (int i = invisibleRootNode.children.length - 1; i >= 0; i--)
		{
			SimpleUserNode node = invisibleRootNode.children[i];
			if (cm.hide(node.getName()))
			{
				node.hide();
			}
		}
		addSolutionProjects(ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProjects());
		view.refreshTreeNodeFromModel(null);
	}

	/**
	 * Returns tree contents to normal.
	 */
	public void stopCalculationMode()
	{
		if (solutionOfCalculation != null)
		{
			this.solutionOfCalculation = null;
			this.tableOfCalculation = null;

			for (int i = invisibleRootNode.children.length - 1; i >= 0; i--)
			{
				invisibleRootNode.children[i].unhide();
			}
			addSolutionProjects(ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProjects());
			view.refreshTreeNodeFromModel(null);
		}
	}

	/**
	 * Gives the node object that represents the solution with the given name.
	 *
	 * @param solutionName the name of the solution.
	 * @return the node object that represents the solution with the given name.
	 */
	public PlatformSimpleUserNode getSolutionNode(String solutionName)
	{
		if ((activeSolutionNode.getRealObject() != null) && (activeSolutionNode.getName().equals(solutionName)))
		{
			return activeSolutionNode;
		}
		return findChildNode(modulesOfActiveSolution, solutionName);
	}

	/**
	 *
	 * @return the database-servers node
	 */
	public PlatformSimpleUserNode getServers()
	{
		return this.servers;
	}

	/**
	 * Gives the resources project node.
	 *
	 * @return the resources project node.
	 */
	public PlatformSimpleUserNode getResourcesNode()
	{
		return resources;
	}

	public PlatformSimpleUserNode getStylesNode()
	{
		return stylesNode;
	}

	public PlatformSimpleUserNode getUserGroupSecurityNode()
	{
		return userGroupSecurityNode;
	}

	public PlatformSimpleUserNode getI18NFilesNode()
	{
		return i18nFilesNode;
	}

	public PlatformSimpleUserNode getTemplatesNode()
	{
		return templatesNode;
	}

	public PlatformSimpleUserNode getSecurity()
	{
		return security;
	}

	public PlatformSimpleUserNode getI18N()
	{
		return i18n;
	}

	private SimpleUserNode findProjectNodeChild(ServoyProject project, UserNodeType type)
	{
		SimpleUserNode projectNode = null;
		if (activeSolutionNode.getRealObject() != null && activeSolutionNode.getRealObject().equals(project))
		{
			projectNode = activeSolutionNode;
		}
		else if (modulesOfActiveSolution != null && modulesOfActiveSolution.children != null)
		{
			for (SimpleUserNode node : modulesOfActiveSolution.children)
			{
				if (node.getRealObject() != null && node.getRealObject().equals(project))
				{
					projectNode = node;
					break;
				}
			}
		}
		if (projectNode != null)
		{
			if (projectNode.children == null) getChildren(projectNode); // create them
			for (SimpleUserNode child : projectNode.children)
			{
				if (child.getRealType() == type)
				{
					return child;
				}
			}
		}
		return null;
	}

	public SimpleUserNode getForms(ServoyProject project)
	{
		return findProjectNodeChild(project, UserNodeType.FORMS);
	}

	public SimpleUserNode getGlobalsFolder(ServoyProject selectedProject, String scopeName)
	{
		SimpleUserNode userNode = findProjectNodeChild(selectedProject, UserNodeType.SCOPES_ITEM);
		if (userNode != null && userNode.children != null)
		{
			String name = Utils.stringInitCap(scopeName);
			for (SimpleUserNode childNode : userNode.children)
			{
				if (childNode.getName().equals(name))
				{
					return childNode;
				}
			}
		}
		return null;
	}

	public SimpleUserNode getRelations(ServoyProject project)
	{
		return findProjectNodeChild(project, UserNodeType.ALL_RELATIONS);
	}

	public SimpleUserNode getValuelists(ServoyProject project)
	{
		return findProjectNodeChild(project, UserNodeType.VALUELISTS);
	}

	public SimpleUserNode getMedia(ServoyProject project)
	{
		return findProjectNodeChild(project, UserNodeType.MEDIA);
	}

	public SimpleUserNode getMediaFolderNode(MediaNode mediaFolder)
	{
		SimpleUserNode mediaFolderNode = null;
		SimpleUserNode media = null;
		ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(mediaFolder.getMedia().getRootObject().getName());
		if (servoyProject != null)
		{
			media = getMedia(servoyProject);
		}
		if (mediaFolder != null && media != null)
		{
			mediaFolderNode = findMediaFolder(media, mediaFolder);
		}

		return mediaFolderNode != null ? mediaFolderNode : media;
	}

	private SimpleUserNode findMediaFolder(SimpleUserNode mediaFolderNode, MediaNode mediaFolder)
	{
		Object realObject = mediaFolderNode.getRealObject();
		if (mediaFolder.equals(realObject)) return mediaFolderNode;

		if (mediaFolderNode.children != null && mediaFolderNode.children.length > 0)
		{
			SimpleUserNode mfNode;
			for (SimpleUserNode childNode : mediaFolderNode.children)
			{
				mfNode = findMediaFolder(childNode, mediaFolder);
				if (mfNode != null) return mfNode;
			}
		}

		return null;
	}

	public PlatformSimpleUserNode getAllSolutionsNode()
	{
		return allSolutionsNode;
	}

	public SimpleUserNode getSolutionFromAllSolutionsNode(String solutionName)
	{
		if (solutionName != null)
		{
			for (SimpleUserNode solutionNode : allSolutionsNode.children)
			{
				if (solutionName.equals(solutionNode.getName())) return solutionNode;
			}
		}

		return null;
	}

	public void refreshServerList()
	{
		addServersNodeChildren(servers);
		view.refreshTreeNodeFromModel(servers);
	}

	public void refreshFormsNode(PlatformSimpleUserNode formsNode, boolean referenceForms)
	{
		addFormsNodeChildren(formsNode, referenceForms);
		view.refreshTreeNodeFromModel(formsNode);
	}

	public void refreshServerViewsNode(IServerInternal server)
	{
		PlatformSimpleUserNode node = findChildNode(servers, server.getName());
		if (node != null)
		{
			handleServerNode(server, node);
			view.refreshTreeNodeFromModel(node);
		}
	}

	public void refreshTable(ITable table)
	{
		PlatformSimpleUserNode serverNode = findChildNode(servers, table.getServerName());
		if (serverNode != null)
		{
			SimpleUserNode tableNode = findChildNode(serverNode, table.getName());
			// only refresh the tree if something on the table has changed
			if (tableNode != null &&
				tableNode.getFlags() != SolutionExplorerListContentProvider.determineFlags((IServerInternal)serverNode.getRealObject(), table.getName()))
			{
				refreshServerViewsNode((IServerInternal)serverNode.getRealObject());
			}
		}
	}

	public TreePath getTreePath(UUID uuid)
	{
		return getTreePath(uuid, activeSolutionNode);
	}

	public TreePath getTreePath(UUID uuid, PlatformSimpleUserNode startNode)
	{
		if (startNode != null && startNode.getRealObject() != null)
		{
			List<PlatformSimpleUserNode> al = new ArrayList<PlatformSimpleUserNode>();
			if (findNode(startNode, uuid, al))
			{
				Collections.reverse(al);
				return new TreePath(al.toArray());
			}
		}
		return null;
	}

	private boolean findNode(PlatformSimpleUserNode startNode, UUID uuid, List<PlatformSimpleUserNode> lst)
	{
		Object realObject = startNode.getRealObject();

		boolean found = realObject instanceof IPersist && ((IPersist)realObject).getUUID().equals(uuid);

		if (!found && realObject instanceof Object[] && ((Object[])realObject).length > 0)
		{
			found = ((Object[])realObject)[0] instanceof IPersist && ((IPersist)((Object[])realObject)[0]).getUUID().equals(uuid);
		}

		if (!found)
		{
			Object[] elements = getElements(startNode);
			if (elements != null)
			{
				for (Object element : elements)
				{
					if (element instanceof PlatformSimpleUserNode && findNode((PlatformSimpleUserNode)element, uuid, lst))
					{
						found = true;
						break;
					}
				}
			}
		}
		if (found)
		{
			lst.add(startNode);
		}
		return found;
	}

	public Object[] getNodesForPersist(IPersist currentActiveEditorPersist)
	{
		// TODO make this work for other persists as well, when necessary
		SimpleUserNode un = null;
		if (currentActiveEditorPersist instanceof Form)
		{
			un = getSolutionNode(currentActiveEditorPersist.getRootObject().getName());
			if (un != null)
			{
				if (!((Form)currentActiveEditorPersist).isFormComponent().booleanValue())
				{
					un = findChildNode(un, Messages.TreeStrings_Forms);
				}
				else
				{
					un = findChildNode(un, Messages.TreeStrings_FormComponents);
				}

				if (un != null)
				{
					un = findChildNode(un, ((Form)currentActiveEditorPersist).getName());
					return new Object[] { un };
				}
			}
		}
		else if (currentActiveEditorPersist instanceof Relation)
		{
			Relation rel = (Relation)currentActiveEditorPersist;
			un = getSolutionNode(currentActiveEditorPersist.getRootObject().getName());
			if (un != null)
			{
				if (rel.isGlobal())
				{
					//search all scopes for relation
					return findChildRelationNodeForParent((Relation)currentActiveEditorPersist, findChildNode(un, Messages.TreeStrings_Scopes));
				}
				else
				{
					//search all forms for relation
					return findChildRelationNodeForParent((Relation)currentActiveEditorPersist, findChildNode(un, Messages.TreeStrings_Forms));
				}
			}
		}
		return new Object[] { un };
	}

	private Object[] findChildRelationNodeForParent(Relation r, SimpleUserNode parentNode)
	{
		List<SimpleUserNode> relations = new ArrayList<SimpleUserNode>();
		if (parentNode != null && parentNode.children != null)
		{
			for (SimpleUserNode uNode : parentNode.children)
			{
				if (uNode != null)
				{
					SimpleUserNode relsNode = findChildNode(uNode, Messages.TreeStrings_relations);
					if (relsNode != null)
					{
						SimpleUserNode relNode = findChildNode(relsNode, r.getName());
						if (relNode != null) relations.add(relNode);
					}
				}
			}
		}
		else return null;

		return relations.toArray();
	}

	@Override
	public void ngPackagesChanged(CHANGE_REASON changeReason, boolean loadedPackagesAreTheSameAlthoughReferencingModulesChanged)
	{
		// refreshing tree due to ng component/service package changes...
		refreshTreeNode(allWebPackagesNode);
		refreshTreeNode(findChildNode(activeSolutionNode, Messages.TreeStrings_Web_Packages));

		ServoyProject[] activeProjects = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
		if (activeProjects != null)
		{
			for (ServoyProject servoyProject : activeProjects)
			{
				SimpleUserNode moduleNode = findChildNode(modulesOfActiveSolution, servoyProject.getSolution().getName());
				if (moduleNode != null) refreshTreeNode(findChildNode(moduleNode, Messages.TreeStrings_Web_Packages)); // null will be main active solution here which is already handled a few lines above
			}
		}

		/* if (componentsChanged) */ refreshTreeNode(componentsFromResourcesNode);
		/* if (servicesChanged) */ refreshTreeNode(servicesFromResourcesNode);

		refreshTreeNode(plugins);
	}

	private void refreshTreeNode(SimpleUserNode nodeToRefresh)
	{
		if (nodeToRefresh != null)
		{
			view.refreshTreeNodeFromModel(nodeToRefresh, true);
		}
	}

	@Override
	public void ngPackageProjectListChanged(boolean activePackageProjectsChanged)
	{
		// hide or show all web packages node if needed
		boolean hasPackageProjects = false;

		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject iProject : projects)
		{
			try
			{
				if (iProject.isAccessible() && iProject.hasNature(ServoyNGPackageProject.NATURE_ID))
				{
					hasPackageProjects = true;
					break;
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}

		if (hasPackageProjects && !invisibleRootNode.children[1].equals(allWebPackagesNode))
		{
			addAllWebPackagesNode();
		}
		else if (!hasPackageProjects && invisibleRootNode.children[1].equals(allWebPackagesNode))
		{
			removeAllWebPackagesNode();
		}

		// refresh allWebPackagesNode due to ngPackageProject list changed...
		refreshTreeNode(allWebPackagesNode);
	}

	private void addAllWebPackagesNode()
	{
		List<SimpleUserNode> newRootChildren = new ArrayList<SimpleUserNode>();
		newRootChildren.addAll(Arrays.asList(invisibleRootNode.children));
		//add allWebPacakgesNode to second position
		newRootChildren.add(1, allWebPackagesNode);
		invisibleRootNode.children = newRootChildren.toArray(new PlatformSimpleUserNode[0]);
		view.refreshTreeCompletely();
	}

	private void removeAllWebPackagesNode()
	{
		List<SimpleUserNode> newRootChildren = new ArrayList<SimpleUserNode>();
		newRootChildren.addAll(Arrays.asList(invisibleRootNode.children));
		//add allWebPacakgesNode to second position
		newRootChildren.remove(allWebPackagesNode);
		invisibleRootNode.children = newRootChildren.toArray(new PlatformSimpleUserNode[0]);
		view.refreshTreeCompletely();
	}

	public void setIncludeModules(boolean includeModules)
	{
		this.includeModules = includeModules;
	}


	public class SpecReloadListener implements ISpecReloadListener
	{
		@Override
		public void webObjectSpecificationReloaded()
		{
			resetServicesSpecProviderState();
			resetComponentsSpecProviderState();

			if (resources != null && resources.children != null)
			{
				if (componentsFromResourcesNode != null)
				{
					List<SimpleUserNode> resourcesChildren = new ArrayList<SimpleUserNode>(Arrays.asList(resources.children));
					if (hasChildren(componentsFromResourcesNode))
					{
						if (!resourcesChildren.contains(componentsFromResourcesNode))
						{
							resourcesChildren.add(componentsFromResourcesNode);
							resources.children = resourcesChildren.toArray(new PlatformSimpleUserNode[resourcesChildren.size()]);
						}
					}
					else
					{
						if (resourcesChildren.contains(componentsFromResourcesNode))
						{
							resourcesChildren.remove(componentsFromResourcesNode);
							resources.children = resourcesChildren.toArray(new PlatformSimpleUserNode[resourcesChildren.size()]);
						}
					}
				}

				if (servicesFromResourcesNode != null)
				{
					List<SimpleUserNode> resourcesChildren = new ArrayList<SimpleUserNode>(Arrays.asList(resources.children));
					if (hasChildren(servicesFromResourcesNode))
					{
						if (!resourcesChildren.contains(servicesFromResourcesNode))
						{
							resourcesChildren.add(servicesFromResourcesNode);
							resources.children = resourcesChildren.toArray(new PlatformSimpleUserNode[resourcesChildren.size()]);
						}
					}
					else
					{
						if (resourcesChildren.contains(servicesFromResourcesNode))
						{
							resourcesChildren.remove(servicesFromResourcesNode);
							resources.children = resourcesChildren.toArray(new PlatformSimpleUserNode[resourcesChildren.size()]);
						}
					}
				}
			}
		}
	}

	/**
	 * Used to get tooltip text for script objects documented in JSLib.
	 */
	private static class JSLibScriptObjectFeedback extends ScriptObjectFeedback
	{

		JSLibScriptObjectFeedback(String codeSample, Class< ? > scriptObjectClass)
		{
			super(codeSample, scriptObjectClass);
		}

		@Override
		protected IObjectDocumentation getDocObjForClass()
		{
			return TreeBuilder.getDocObjectForJSLibClass(cls);
		}

	}

	/**
	 * Used to get tooltip text for script objects documented in core or plugins.
	 */
	private static class ScriptObjectFeedback implements IDeveloperFeedback
	{
		private final String codeSample;
		protected Class< ? > cls;
		private String tooltip;

		ScriptObjectFeedback(String codeSample, Class< ? > scriptObjectClass)
		{
			this.codeSample = codeSample;
			this.cls = scriptObjectClass;
		}

		public String getSample()
		{
			return null;
		}

		public String getCode()
		{
			return codeSample;
		}

		public String getToolTipText()
		{
			if (tooltip == null && cls != null)
			{
				IObjectDocumentation docObj = getDocObjForClass();
				cls = null; // just to not search again...
				String desc = (docObj != null ? docObj.getDescription(ServoyModelManager.getServoyModelManager().getServoyModel().getActiveSolutionClientType())
					: null);
				if (desc != null)
				{
					tooltip = HtmlUtils.applyDescriptionMagic(desc);
				}
			}

			return tooltip;
		}

		protected IObjectDocumentation getDocObjForClass()
		{
			IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(cls);
			return scriptObject instanceof XMLScriptObjectAdapter soo ? soo.getObjectDocumentation() : null;
		}

	}

	public PlatformSimpleUserNode getAllWebPackagesNode()
	{
		return allWebPackagesNode;
	}
}