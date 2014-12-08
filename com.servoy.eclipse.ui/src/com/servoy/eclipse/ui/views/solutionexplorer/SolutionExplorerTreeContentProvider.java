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

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
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
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Icon;

import org.apache.commons.dbcp.DbcpException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.mozilla.javascript.JavaMembers;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.IWebResourceChangedListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.labelproviders.RelationLabelProvider;
import com.servoy.eclipse.ui.node.SimpleDeveloperFeedback;
import com.servoy.eclipse.ui.node.SimpleUserNode;
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
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.documentation.scripting.docs.JSLib;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IMediaProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IDeprecated;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.JSApplication;
import com.servoy.j2db.scripting.JSI18N;
import com.servoy.j2db.scripting.JSSecurity;
import com.servoy.j2db.scripting.JSUnitAssertFunctions;
import com.servoy.j2db.scripting.JSUtils;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
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
public class SolutionExplorerTreeContentProvider implements IStructuredContentProvider, ITreeContentProvider, IWebResourceChangedListener
{
	private static final String IMG_SOLUTION = "solution.gif";
	private static final String IMG_SOLUTION_M = "module.gif";
	private static final String IMG_SOLUTION_MODULE = "solution_module.gif";
	private static final String IMG_SOLUTION_LOGIN = "solution_login.gif";
	private static final String IMG_SOLUTION_AUTHENTICATOR = "solution_auth.gif";
	private static final String IMG_SOLUTION_SMART_ONLY = "solution_smart_only.gif";
	private static final String IMG_SOLUTION_WEB_ONLY = "solution_web_only.gif";
	private static final String IMG_SOLUTION_PREIMPORT = "solution_preimport.png";
	private static final String IMG_SOLUTION_POSTIMPORT = "solution_postimport.png";
	private static Map<IPath, Image> imageCache = new HashMap<IPath, Image>();

	private PlatformSimpleUserNode invisibleRootNode;

	private PlatformSimpleUserNode activeSolutionNode;

	private PlatformSimpleUserNode allSolutionsNode;

	private final PlatformSimpleUserNode databaseManager;

	private final PlatformSimpleUserNode datasources;

	private final PlatformSimpleUserNode solutionModel;

	private final PlatformSimpleUserNode history;

	private final PlatformSimpleUserNode servers;

	private final PlatformSimpleUserNode resources;

	private final PlatformSimpleUserNode stylesNode;

	private final PlatformSimpleUserNode componentsNode;

	private final PlatformSimpleUserNode servicesNode;

	private final PlatformSimpleUserNode templatesNode;

	private final PlatformSimpleUserNode i18nFilesNode;

	private final PlatformSimpleUserNode userGroupSecurityNode;

	private final PlatformSimpleUserNode security;

	private final PlatformSimpleUserNode i18n;

	private final PlatformSimpleUserNode[] scriptingNodes;

	private final PlatformSimpleUserNode[] resourceNodes;

	private final SolutionExplorerView view;

	private Solution solutionOfCalculation;

	private ITable tableOfCalculation;

	private final PlatformSimpleUserNode modulesOfActiveSolution;

	private final Object pluginsBackgroundLoadLock = new Object();

	private final List<String> unreachableServers = new ArrayList<String>();

	private final com.servoy.eclipse.ui.Activator uiActivator = com.servoy.eclipse.ui.Activator.getDefault();

	private final List<Image> imagesConvertedFromSwing = new ArrayList<Image>();

	private static PlatformSimpleUserNode createTypeNode(String displayName, UserNodeType type, Class< ? > realType, PlatformSimpleUserNode parent)
	{
		PlatformSimpleUserNode node = new PlatformSimpleUserNode(displayName, type, null, IconProvider.instance().image(realType), realType);
		node.parent = parent;
		return node;
	}

	SolutionExplorerTreeContentProvider(SolutionExplorerView v)
	{
		view = v;
		invisibleRootNode = new PlatformSimpleUserNode("root", UserNodeType.ARRAY);

		PlatformSimpleUserNode jslib = createTypeNode(Messages.TreeStrings_JSLib, UserNodeType.JSLIB, JSLib.class, invisibleRootNode);

		jslib.children = new PlatformSimpleUserNode[] { //
		createTypeNode(Messages.TreeStrings_Array, UserNodeType.ARRAY, com.servoy.j2db.documentation.scripting.docs.Array.class, jslib),//
		createTypeNode(Messages.TreeStrings_Date, UserNodeType.DATE, com.servoy.j2db.documentation.scripting.docs.Date.class, jslib), //
		createTypeNode(Messages.TreeStrings_String, UserNodeType.STRING, com.servoy.j2db.documentation.scripting.docs.String.class, jslib), //
		createTypeNode(Messages.TreeStrings_Number, UserNodeType.NUMBER, com.servoy.j2db.documentation.scripting.docs.Number.class, jslib), //
		createTypeNode(Messages.TreeStrings_Math, UserNodeType.FUNCTIONS, com.servoy.j2db.documentation.scripting.docs.Math.class, jslib),//
		createTypeNode(Messages.TreeStrings_RegExp, UserNodeType.REGEXP, com.servoy.j2db.documentation.scripting.docs.RegExp.class, jslib), //
		createTypeNode(Messages.TreeStrings_Statements, UserNodeType.STATEMENTS, com.servoy.j2db.documentation.scripting.docs.Statements.class, jslib), //
		createTypeNode(Messages.TreeStrings_SpecialOperators, UserNodeType.SPECIAL_OPERATORS,
			com.servoy.j2db.documentation.scripting.docs.SpecialOperators.class, jslib), //
		createTypeNode(Messages.TreeStrings_JSON, UserNodeType.JSON, com.servoy.j2db.documentation.scripting.docs.JSON.class, jslib), //
		createTypeNode(Messages.TreeStrings_XMLMethods, UserNodeType.XML_METHODS, com.servoy.j2db.documentation.scripting.docs.XML.class, jslib), //
		createTypeNode(Messages.TreeStrings_XMLListMethods, UserNodeType.XML_LIST_METHODS, com.servoy.j2db.documentation.scripting.docs.XMLList.class, jslib) };

		PlatformSimpleUserNode application = createTypeNode(Messages.TreeStrings_Application, UserNodeType.APPLICATION, JSApplication.class, invisibleRootNode);

		addReturnTypeNodes(
			application,
			Utils.arrayJoin(ScriptObjectRegistry.getScriptObjectForClass(JSApplication.class).getAllReturnedTypes(),
				new ServoyException(0).getAllReturnedTypes()));

		resources = new PlatformSimpleUserNode(Messages.TreeStrings_Resources, UserNodeType.RESOURCES, null, uiActivator.loadImageFromBundle("resources.png"));
		resources.parent = invisibleRootNode;

		stylesNode = new PlatformSimpleUserNode(Messages.TreeStrings_Styles, UserNodeType.STYLES, null, uiActivator.loadImageFromBundle("styles.gif"));
		stylesNode.setClientSupport(ClientSupport.ng_wc_sc);
		stylesNode.parent = resources;

		componentsNode = new PlatformSimpleUserNode(Messages.TreeStrings_Components, UserNodeType.COMPONENTS, SolutionSerializer.COMPONENTS_DIR_NAME,
			uiActivator.loadImageFromBundle("coffee_grains.png"));
		componentsNode.setClientSupport(ClientSupport.ng_wc_sc);
		componentsNode.parent = resources;

		servicesNode = new PlatformSimpleUserNode(Messages.TreeStrings_Services, UserNodeType.SERVICES, SolutionSerializer.SERVICES_DIR_NAME,
			uiActivator.loadImageFromBundle("services.gif"));
		servicesNode.setClientSupport(ClientSupport.ng_wc_sc);
		servicesNode.parent = resources;

		userGroupSecurityNode = new PlatformSimpleUserNode(Messages.TreeStrings_UserGroupSecurity, UserNodeType.USER_GROUP_SECURITY, null,
			uiActivator.loadImageFromBundle("lock.gif"));
		userGroupSecurityNode.setClientSupport(ClientSupport.ng_wc_sc);
		userGroupSecurityNode.parent = resources;

		i18nFilesNode = new PlatformSimpleUserNode(Messages.TreeStrings_I18NFiles, UserNodeType.I18N_FILES, null, uiActivator.loadImageFromBundle("i18n.gif"));
		i18nFilesNode.parent = resources;

		templatesNode = new PlatformSimpleUserNode(Messages.TreeStrings_Templates, UserNodeType.TEMPLATES, null,
			uiActivator.loadImageFromBundle("template.gif"));
		templatesNode.setClientSupport(ClientSupport.ng_wc_sc);
		templatesNode.parent = resources;

		activeSolutionNode = new PlatformSimpleUserNode(Messages.TreeStrings_NoActiveSolution, UserNodeType.SOLUTION, null,
			Messages.SolutionExplorerView_activeSolution, null, uiActivator.loadImageFromBundle("solution.gif"));
		activeSolutionNode.parent = invisibleRootNode;
		modulesOfActiveSolution = new PlatformSimpleUserNode(Messages.TreeStrings_Modules, UserNodeType.MODULES, null,
			uiActivator.loadImageFromBundle("modules.gif"));
		modulesOfActiveSolution.parent = activeSolutionNode;

		allSolutionsNode = new PlatformSimpleUserNode(Messages.TreeStrings_AllSolutions, UserNodeType.ALL_SOLUTIONS, null,
			PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));
		allSolutionsNode.parent = invisibleRootNode;

		databaseManager = createTypeNode(Messages.TreeStrings_DatabaseManager, UserNodeType.FOUNDSET_MANAGER, JSDatabaseManager.class, invisibleRootNode);
		addReturnTypeNodes(databaseManager, ScriptObjectRegistry.getScriptObjectForClass(JSDatabaseManager.class).getAllReturnedTypes());

		datasources = createTypeNode(Messages.TreeStrings_Datasources, UserNodeType.DATASOURCES, JSDataSources.class, invisibleRootNode);
		addReturnTypeNodes(datasources, ScriptObjectRegistry.getScriptObjectForClass(JSDataSources.class).getAllReturnedTypes());

		PlatformSimpleUserNode utils = createTypeNode(Messages.TreeStrings_Utils, UserNodeType.UTILS, JSUtils.class, invisibleRootNode);

		PlatformSimpleUserNode jsunit = createTypeNode(Messages.TreeStrings_JSUnit, UserNodeType.JSUNIT, JSUnitAssertFunctions.class, invisibleRootNode);

		solutionModel = createTypeNode(Messages.TreeStrings_SolutionModel, UserNodeType.SOLUTION_MODEL, JSSolutionModel.class, invisibleRootNode);

		addReturnTypeNodes(solutionModel, ScriptObjectRegistry.getScriptObjectForClass(JSSolutionModel.class).getAllReturnedTypes());

		history = createTypeNode(Messages.TreeStrings_History, UserNodeType.HISTORY, HistoryProvider.class, invisibleRootNode);

		security = createTypeNode(Messages.TreeStrings_Security, UserNodeType.SECURITY, JSSecurity.class, invisibleRootNode);
		addReturnTypeNodes(security, ScriptObjectRegistry.getScriptObjectForClass(JSSecurity.class).getAllReturnedTypes());

		i18n = createTypeNode(Messages.TreeStrings_i18n, UserNodeType.I18N, JSI18N.class, invisibleRootNode);
		addReturnTypeNodes(i18n, ScriptObjectRegistry.getScriptObjectForClass(JSI18N.class).getAllReturnedTypes());

		servers = new PlatformSimpleUserNode(Messages.TreeStrings_DBServers, UserNodeType.SERVERS, null, uiActivator.loadImageFromBundle("database_srv.gif"));
		servers.parent = resources;

		final PlatformSimpleUserNode plugins = new PlatformSimpleUserNode(Messages.TreeStrings_Plugins, UserNodeType.PLUGINS, null,
			uiActivator.loadImageFromBundle("plugin.gif"));
		plugins.parent = invisibleRootNode;

		resources.children = new PlatformSimpleUserNode[] { servers, stylesNode, userGroupSecurityNode, i18nFilesNode, templatesNode, componentsNode, servicesNode };

		invisibleRootNode.children = new PlatformSimpleUserNode[] { resources, allSolutionsNode, activeSolutionNode, jslib, application, solutionModel, databaseManager, datasources, utils, history, security, i18n, jsunit, plugins };

		scriptingNodes = new PlatformSimpleUserNode[] { jslib, application, solutionModel, databaseManager, datasources, utils, history, security, i18n, /*
																																						 * exceptions
																																						 * ,
																																						 */jsunit, plugins };
		resourceNodes = new PlatformSimpleUserNode[] { stylesNode, userGroupSecurityNode, i18nFilesNode, templatesNode, componentsNode, servicesNode };

		// we want to load the plugins node in a background low prio job so that it will expand fast
		// when used...
		Job job = new Job("Background loading of plugins node")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				addPluginsNodeChildren(plugins);
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setPriority(Job.LONG);
		job.schedule();

		com.servoy.eclipse.core.Activator.getDefault().addWebComponentChangedListener(this);
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

		// dispose the (plugin) images that were allocated in SWT after conversion from Swing
		for (Image i : imagesConvertedFromSwing)
		{
			i.dispose();
		}
		imagesConvertedFromSwing.clear();
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

	public void setScriptingNodesEnabled(boolean isEnabled)
	{
		setNodesEnabled(scriptingNodes, isEnabled);
	}

	public void setResourceNodesEnabled(boolean isEnabled)
	{
		setNodesEnabled(resourceNodes, isEnabled);
	}

	private void setNodesEnabled(PlatformSimpleUserNode[] nodes, boolean isEnabled)
	{
		for (PlatformSimpleUserNode n : nodes)
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
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
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

		List<ServoyProject> importHookModules = new ArrayList<ServoyProject>();
		servoyModel.addImportHookModules(servoyModel.getActiveProject(), importHookModules);

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
					if (module == servoyProject)
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
						node = new PlatformSimpleUserNode(displayValue, UserNodeType.SOLUTION_ITEM, servoyProject, getServoyProjectImage(servoyProject, false,
							false));
					}
					else
					{
						node = new PlatformSimpleUserNode(displayValue, UserNodeType.SOLUTION_ITEM, servoyProject, getServoyProjectImage(servoyProject, true,
							!expandable));
						node.setEnabled(expandable);
						modulesNodeChildren.add(node);
						node.parent = modulesOfActiveSolution;

						node = new PlatformSimpleUserNode(displayValue, UserNodeType.SOLUTION_ITEM, servoyProject, getServoyProjectImage(servoyProject, true,
							false));
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
					// it's probably a non-active solution/module
					// it can also be an import hook module of the active solution that is not part of the flattened solution - create an un-expandable "module" node as well in this case
					boolean isActiveImportHookModule = importHookModules.contains(servoyProject);

					PlatformSimpleUserNode node = new PlatformSimpleUserNode(displayValue, UserNodeType.SOLUTION_ITEM_NOT_ACTIVE_MODULE, servoyProject,
						getServoyProjectImage(servoyProject, false, !isActiveImportHookModule));
					node.setEnabled(false);
					allSolutionChildren.add(node);
					node.parent = allSolutionsNode;

					//String solutionName = (String)servoyProject.getSolution().getProperty(StaticContentSpecLoader.PROPERTY_TITLETEXT.getPropertyName());
					//if (solutionName == null) solutionName = servoyProject.getSolution().getName();
					// above code would load all solutions
					// do not load all solutions at startup by reading solution directly
					node.setToolTipText(servoyProject.getProject().getName() + "(" + getSolutionTypeAsString(servoyProject) + ")");

					if (isActiveImportHookModule)
					{
						node = new PlatformSimpleUserNode(displayValue, UserNodeType.SOLUTION_ITEM, servoyProject, getServoyProjectImage(servoyProject, true,
							false));
						node.setToolTipText(Messages.TreeStrings_ImportHookTooltip);
						node.setEnabled(false); // it is not expandable
						modulesNodeChildren.add(node);
						node.parent = modulesOfActiveSolution;
					}
				}
			}
		}

		// set active solution node to usable/unusable
		activeSolutionNode.setRealObject(servoyModel.getActiveProject());
		if (activeSolutionNode.getRealObject() != null)
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
				if (((ServoyProject)activeSolutionNode.getRealObject()).getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION) databaseManager.hide();
				else databaseManager.unhide();
				// datasources not allowed in login solution
				if (((ServoyProject)activeSolutionNode.getRealObject()).getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION) datasources.hide();
				else datasources.unhide();
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
						addFormsNodeChildren(un);
					}
					else if (type == UserNodeType.WORKING_SET || (type == UserNodeType.GRAYED_OUT && un.getRealType() == UserNodeType.WORKING_SET))
					{
						addWorkingSetNodeChildren(un);
					}
					else if (type == UserNodeType.FORM || (type == UserNodeType.GRAYED_OUT && un.getRealType() == UserNodeType.FORM))
					{
						addFormNodeChildren(un);
					}
					else if (type == UserNodeType.SERVERS)
					{
						addServersNodeChildren(un);
					}
					else if (type == UserNodeType.PLUGINS)
					{
						addPluginsNodeChildren(un);
					}
					else if (type == UserNodeType.MEDIA_FOLDER)
					{
						addMediaFolderChildrenNodes(un, ((MediaNode)un.getRealObject()).getMediaProvider());
					}
					else if (type == UserNodeType.COMPONENTS)
					{
						WebComponentSpecProvider provider = WebComponentSpecProvider.getInstance();
						Map<String, URL> packages = provider.getPackagesToURLs();
						List<PlatformSimpleUserNode> children = new ArrayList<PlatformSimpleUserNode>();
						Image packageIcon = uiActivator.loadImageFromBundle("package_obj.gif");
						for (String packageName : packages.keySet())
						{
							IResource resource = getResource(packages.get(packageName));
							if (resource != null)
							{
								PlatformSimpleUserNode node = new PlatformSimpleUserNode(packageName, UserNodeType.COMPONENTS_PACKAGE, resource, packageIcon);
								node.parent = un;
								children.add(node);
							}
						}
						un.children = children.toArray(new PlatformSimpleUserNode[children.size()]);
					}
					else if (type == UserNodeType.COMPONENTS_PACKAGE)
					{
						WebComponentSpecProvider provider = WebComponentSpecProvider.getInstance();
						List<String> components = provider.getComponentsInPackage(un.getName());
						List<PlatformSimpleUserNode> children = new ArrayList<PlatformSimpleUserNode>();
						if (components != null)
						{
							IFolder folder = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getResourcesProject().getProject().getFolder(
								SolutionSerializer.COMPONENTS_DIR_NAME);
							Image componentIcon = uiActivator.loadImageFromBundle("bean.gif");
							for (String component : components)
							{
								WebComponentSpecification spec = provider.getWebComponentSpecification(component);
								Image img = loadImageFromFolder(folder, spec.getIcon());
								PlatformSimpleUserNode node = new PlatformSimpleUserNode(spec.getDisplayName(), UserNodeType.COMPONENT, spec, img != null ? img
									: componentIcon);
								node.parent = un;
								children.add(node);
							}
						}
						un.children = children.toArray(new PlatformSimpleUserNode[children.size()]);
					}
					else if (type == UserNodeType.SERVICES)
					{
						WebServiceSpecProvider provider = WebServiceSpecProvider.getInstance();
						Map<String, URL> packages = provider.getPackagesToURLs();
						List<PlatformSimpleUserNode> children = new ArrayList<PlatformSimpleUserNode>();
						Image packageIcon = uiActivator.loadImageFromBundle("package_obj.gif");
						for (String packageName : packages.keySet())
						{
							IResource resource = getResource(packages.get(packageName));
							if (resource != null)
							{
								PlatformSimpleUserNode node = new PlatformSimpleUserNode(packageName, UserNodeType.SERVICES_PACKAGE, resource, packageIcon);
								node.parent = un;
								children.add(node);
							}
						}
						un.children = children.toArray(new PlatformSimpleUserNode[children.size()]);
					}
					else if (type == UserNodeType.SERVICES_PACKAGE)
					{
						WebServiceSpecProvider provider = WebServiceSpecProvider.getInstance();
						List<String> services = provider.getServicesInPackage(un.getName());
						List<PlatformSimpleUserNode> children = new ArrayList<PlatformSimpleUserNode>();
						if (services != null)
						{
							IFolder folder = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getResourcesProject().getProject().getFolder(
								SolutionSerializer.SERVICES_DIR_NAME);
							Image serviceDefaultIcon = uiActivator.loadImageFromBundle("service.png");
							for (String component : services)
							{
								WebComponentSpecification spec = provider.getWebServiceSpecification(component);
								Image img = loadImageFromFolder(folder, spec.getIcon());
								PlatformSimpleUserNode node = new PlatformSimpleUserNode(spec.getDisplayName(), UserNodeType.SERVICE, spec, img != null ? img
									: serviceDefaultIcon);
								node.parent = un;
								children.add(node);
							}
						}
						un.children = children.toArray(new PlatformSimpleUserNode[children.size()]);

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
			return un.children;
		}
		return new Object[0];
	}

	private Image loadImageFromFolder(IFolder folder, String iconPath) throws CoreException
	{
		if (iconPath != null)
		{
			IFile iconFile = folder.getFile(iconPath);
			Image img = imageCache.get(iconFile.getFullPath());
			if (img == null && iconFile.exists())
			{
				InputStream is = iconFile.getContents();
				Display display = Display.getCurrent();
				img = new Image(display, new ImageData(is));
				if (img != null) imageCache.put(iconFile.getFullPath(), img);
			}
			return img;
		}
		return null;
	}

	/**
	 * @param url
	 * @return
	 */
	private IResource getResource(URL packageURL)
	{
		if (packageURL == null) return null;

		try
		{
			IWorkspaceRoot root = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getResourcesProject().getProject().getWorkspace().getRoot();
			IContainer[] dirResource = root.findContainersForLocationURI(packageURL.toURI());
			if (dirResource.length == 1 && dirResource[0].exists()) return dirResource[0];

			IFile[] jarResource = root.findFilesForLocationURI(packageURL.toURI());
			if (jarResource.length == 1 && jarResource[0].exists()) return jarResource[0];
		}
		catch (URISyntaxException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
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
					if (((Solution)un.getRealObject()).getForms(null, false).hasNext()) return true;
					if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject() != null &&
						ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().hasServoyWorkingSets(
							new String[] { ((Solution)un.getRealObject()).getName() })) return true;
					return false;
				}
				else if (un.getType() == UserNodeType.WORKING_SET || (un.getType() == UserNodeType.GRAYED_OUT && un.getRealType() == UserNodeType.WORKING_SET))
				{
					if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject() != null &&
						ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().hasPersistsInServoyWorkingSets(un.getName(),
							new String[] { un.getSolution().getName() })) return true;
					return false;
				}
				else if (un.getType() == UserNodeType.FORM || (un.getType() == UserNodeType.GRAYED_OUT && un.getRealType() == UserNodeType.FORM))
				{
					return true;
				}
				else if (un.getType() == UserNodeType.SERVERS)
				{
					return ServoyModel.getServerManager().getServerNames(false, false, true, true).length > 0;
				}
				else if (un.getType() == UserNodeType.PLUGINS)
				{
					return true;
				}
				else if (un.getType() == UserNodeType.MEDIA_FOLDER)
				{
					return ((MediaNode)un.getRealObject()).hasChildren(EnumSet.of(MediaNode.TYPE.FOLDER));
				}
				else if (un.getType() == UserNodeType.COMPONENTS)
				{
					return resourceFolderHasChildren(SolutionSerializer.COMPONENTS_DIR_NAME);
				}
				else if (un.getType() == UserNodeType.SERVICES)
				{
					return resourceFolderHasChildren(SolutionSerializer.SERVICES_DIR_NAME);
				}
				else if (un.getType() == UserNodeType.COMPONENTS_PACKAGE)
				{
					List<String> components = WebComponentSpecProvider.getInstance().getComponentsInPackage(un.getName());
					return components != null && !components.isEmpty();
				}
				else if (un.getType() == UserNodeType.SERVICES_PACKAGE)
				{
					List<String> services = WebServiceSpecProvider.getInstance().getServicesInPackage(un.getName());
					return services != null && !services.isEmpty();
				}
			}
			else if (un.children.length > 0)
			{
				return true;
			}
			return false;
		}
		return true;
	}

	private boolean resourceFolderHasChildren(String folderName)
	{
		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		if (activeProject == null) return false;
		IFolder folder = activeProject.getResourcesProject().getProject().getFolder(folderName);
		try
		{
			return folder.exists() ? folder.members().length > 0 : false;
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
		return false;
	}


	private void addServersNodeChildren(PlatformSimpleUserNode serversNode)
	{
		List<PlatformSimpleUserNode> serverNodes = new ArrayList<PlatformSimpleUserNode>();
		IServerManagerInternal handler = ServoyModel.getServerManager();
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
				uiActivator.loadImageFromBundle(getServerImageName(server_name, serverObj)));
			serverNodes.add(node);
			node.parent = serversNode;
			handleServerViewsNode(serverObj, node);
		}

		serversNode.children = serverNodes.toArray(new PlatformSimpleUserNode[serverNodes.size()]);
	}

	public static String getServerImageName(String serverName, IServerInternal server)
	{
		if (!server.getConfig().isEnabled())
		{
			return "serverDisabled.gif";
		}
		if (!server.isValid())
		{
			return "serverError.gif";
		}
		if (!server.getName().equals(serverName))
		{
			return "serverDuplicate.gif";
		}

		return "server.gif";
	}

	private void handleServerViewsNode(IServerInternal serverObj, PlatformSimpleUserNode node)
	{
		if (serverObj.getConfig().isEnabled() && serverObj.isValid())
		{
			List<String> views = null;
			try
			{
				views = serverObj.getViewNames(true);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
			if (views != null && views.size() > 0)
			{
				if (node.children == null || node.children.length == 0)
				{
					PlatformSimpleUserNode viewNode = new PlatformSimpleUserNode("Views", UserNodeType.VIEWS, "", "Views", serverObj,
						PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));
					node.children = new PlatformSimpleUserNode[] { viewNode };
					viewNode.parent = node;
				}
			}
			else
			{
				node.children = null;
			}
		}
	}

	private void addPluginsNodeChildren(PlatformSimpleUserNode pluginNode)
	{
		synchronized (pluginsBackgroundLoadLock)
		{
			if (pluginNode.children != null) return;

			ArrayList<PlatformSimpleUserNode> plugins = new ArrayList<PlatformSimpleUserNode>();
			Iterator<IClientPlugin> it = Activator.getDefault().getDesignClient().getPluginManager().getPlugins(IClientPlugin.class).iterator();
			while (it.hasNext())
			{
				IClientPlugin plugin = it.next();
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
						Icon icon = plugin.getImage();
						Image image = null; // will need SWT image
						if (icon != null)
						{
							image = UIUtils.getSWTImageFromSwingIcon(icon, view.getSite().getShell().getDisplay(), 16, 16);
							if (image != null) imagesConvertedFromSwing.add(image);
						}
						if (image == null)
						{
							image = uiActivator.loadImageFromBundle("plugin_conn.gif");
						}

						PlatformSimpleUserNode node = new PlatformSimpleUserNode(plugin.getName(), UserNodeType.PLUGIN, scriptObject, image,
							scriptObject.getClass());
						if (view.isNonEmptyPlugin(node))
						{
							plugins.add(node);
							node.parent = pluginNode;
							if (scriptObject instanceof IReturnedTypesProvider)
							{
								Class< ? >[] clss = ((IReturnedTypesProvider)scriptObject).getAllReturnedTypes();
								addReturnTypeNodes(node, clss);
							}
						}
					}
				}
				catch (Throwable e)
				{
					ServoyLog.logError("Error loading plugin " + plugin.getName() + " exception: ", e);
					PlatformSimpleUserNode node = new PlatformSimpleUserNode(plugin.getName() + " (not loaded!)", UserNodeType.PLUGIN, null, null,
						e.toString(), null, uiActivator.loadImageFromBundle("warning.gif"));
					plugins.add(node);
					node.parent = pluginNode;
				}
			}
			pluginNode.children = plugins.toArray(new PlatformSimpleUserNode[plugins.size()]);
			// It may happen that the Plugins node was disabled before its children were added.
			if (pluginNode.isHidden()) pluginNode.hide();
			else pluginNode.unhide();
			view.refreshTreeNodeFromModel(pluginNode);
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
					PlatformSimpleUserNode n = new PlatformSimpleUserNode(nodeName, UserNodeType.RETURNTYPE, cls, (Image)null, cls);
					JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(cls, null);
					if (IConstantsObject.class.isAssignableFrom(cls) &&
						!(javaMembers instanceof InstanceJavaMembers && javaMembers.getMethodIds(false).size() > 0))
					{
						constantsChildren.add(n);
						n.setIcon(uiActivator.loadImageFromBundle("constant.gif"));
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
					uiActivator.loadImageFromBundle("constant.gif")));
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
			PlatformSimpleUserNode scopesFolder = new PlatformSimpleUserNode(Messages.TreeStrings_Scopes, solutionOfCalculation == null
				? UserNodeType.SCOPES_ITEM : UserNodeType.SCOPES_ITEM_CALCULATION_MODE, solution, uiActivator.loadImageFromBundle("scopes.gif"));
			scopesFolder.parent = projectNode;
			addScopesNodeChildren(scopesFolder);

			PlatformSimpleUserNode forms = new PlatformSimpleUserNode(Messages.TreeStrings_Forms, UserNodeType.FORMS, solution,
				uiActivator.loadImageFromBundle("forms.gif"));
			forms.parent = projectNode;
			PlatformSimpleUserNode allRelations = null;
			if (solutionOfCalculation == null)
			{
				allRelations = new PlatformSimpleUserNode(Messages.TreeStrings_Relations, UserNodeType.ALL_RELATIONS, solution,
					uiActivator.loadImageFromOldLocation("relationsoverview.gif"));
				allRelations.parent = projectNode;
			}
			PlatformSimpleUserNode valuelists = new PlatformSimpleUserNode(Messages.TreeStrings_ValueLists, UserNodeType.VALUELISTS, solution,
				uiActivator.loadImageFromBundle("valuelists.gif"));
			valuelists.parent = projectNode;
			PlatformSimpleUserNode media = new PlatformSimpleUserNode(Messages.TreeStrings_Media, UserNodeType.MEDIA, solution,
				uiActivator.loadImageFromBundle("image.gif"));
			media.parent = projectNode;
			addMediaFolderChildrenNodes(media, solution);


			if (solutionOfCalculation != null)
			{
				// in case of calculation editor
				PlatformSimpleUserNode dataProvidersNode = new PlatformSimpleUserNode(Messages.TreeStrings_DataProviders, UserNodeType.TABLE_COLUMNS,
					tableOfCalculation, solution, uiActivator.loadImageFromBundle("selected_record.gif"));
				allRelations = new PlatformSimpleUserNode(Messages.TreeStrings_Relations, UserNodeType.RELATIONS, tableOfCalculation,
					uiActivator.loadImageFromOldLocation("relationsoverview.gif"));
				addRelationsNodeChildren(allRelations, solution, (Table)tableOfCalculation, UserNodeType.CALC_RELATION);

				dataProvidersNode.parent = projectNode;
				allRelations.parent = projectNode;
				projectNode.children = new PlatformSimpleUserNode[] { scopesFolder, dataProvidersNode, forms, allRelations, valuelists, media };
				forms.hide();
			}
			else
			{
				projectNode.children = new PlatformSimpleUserNode[] { scopesFolder, forms, allRelations, valuelists, media };
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
			MediaNode folderNode = (mediaFolder.getType() == UserNodeType.MEDIA_FOLDER) ? (MediaNode)mediaFolder.getRealObject() : new MediaNode(null, null,
				MediaNode.TYPE.FOLDER, mediaProvider);

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
					uiActivator.loadImageFromBundle("globe.gif"));
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
			uiActivator.loadImageFromBundle("global_variabletree.gif"));
		globalVariables.parent = globalsFolder;

		PlatformSimpleUserNode globalRelations = new PlatformSimpleUserNode(Messages.TreeStrings_relations, UserNodeType.GLOBALRELATIONS, solutionAndScope,
			uiActivator.loadImageFromOldLocation("relationsoverview.gif"));
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
			if (forms != null)
			{
				List<PlatformSimpleUserNode> nodes = new ArrayList<PlatformSimpleUserNode>();
				for (String formName : forms)
				{
					Form form = workingSetNode.getSolution().getForm(formName);
					if (form != null)
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
		PlatformSimpleUserNode node = new PlatformSimpleUserNode(f.getName(), UserNodeType.FORM, f.getName(), f.getDataSource() == null ? "No table"
			: ("Server: " + f.getServerName() + ", Table: " + f.getTableName()), f, ElementUtil.getImageForFormEncapsulation(f));
		nodes.add(node);
		node.parent = parentNode;
	}

	private void addFormsNodeChildren(PlatformSimpleUserNode formsNode)
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
					PlatformSimpleUserNode node = new PlatformSimpleUserNode(workingSet, UserNodeType.WORKING_SET, null, solution,
						uiActivator.loadImageFromBundle("servoy_workingset.gif"));
					nodes.add(node);
					node.parent = formsNode;
				}
			}
		}
		Iterator<Form> it = solution.getForms(null, true);
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
		try
		{
			List<PlatformSimpleUserNode> node = new ArrayList<PlatformSimpleUserNode>();
			PlatformSimpleUserNode functionsNode = new PlatformSimpleUserNode(Messages.TreeStrings_controller, UserNodeType.FORM_CONTROLLER, f,
				uiActivator.loadImageFromBundle("formula.gif"));
			functionsNode.parent = formNode;
			node.add(functionsNode);

			PlatformSimpleUserNode variables = new PlatformSimpleUserNode(Messages.TreeStrings_variables, UserNodeType.FORM_VARIABLES, f,
				uiActivator.loadImageFromBundle("form_variabletree.gif"));
			variables.parent = formNode;
			node.add(variables);

			PlatformSimpleUserNode elementsNode = new PlatformSimpleUserNode(Messages.TreeStrings_elements, UserNodeType.FORM_ELEMENTS, f,
				uiActivator.loadImageFromBundle("elements.gif"));
			elementsNode.parent = formNode;
			node.add(elementsNode);
			addFormElementsChildren(elementsNode);

			if (f.getDataSource() != null)
			{
				PlatformSimpleUserNode columnsNode = null;
				try
				{
					columnsNode = new PlatformSimpleUserNode(Messages.TreeStrings_selectedrecord, UserNodeType.TABLE_COLUMNS, f.getTable(), f,
						uiActivator.loadImageFromBundle("selected_record.gif"));
					columnsNode.parent = formNode;
					node.add(columnsNode);
				}
				catch (DbcpException e)
				{
					ServoyLog.logInfo("Cannot create 'selectedrecord' node for " + formNode.getName() + ": " + e.getMessage());
					disableServer(f.getServerName());
				}

				PlatformSimpleUserNode relationsNode = new PlatformSimpleUserNode(Messages.TreeStrings_relations, UserNodeType.RELATIONS, f, f,
					uiActivator.loadImageFromOldLocation("relationsoverview.gif"));
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
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
	}

	private void disableServer(String serverName)
	{
		try
		{
			if (unreachableServers.contains(serverName))
			{
				if (MessageDialog.openConfirm(view.getSite().getShell(), "Disable server", "Cannot connect to server " + serverName +
					". Do you want to disable it?"))
				{
					IServerInternal server = (IServerInternal)ServoyModel.getServerManager().getServer(serverName, true, false);
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

	private void addFormElementsChildren(PlatformSimpleUserNode elementsNode)
	{
		Form form = (Form)elementsNode.getRealObject();
		addFormElementsChildren(elementsNode, new HashSet<Form>(), form, form);
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
						uiActivator.loadImageFromBundle("elements.gif"));

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
		List<PlatformSimpleUserNode> elements = new SortedList<PlatformSimpleUserNode>(StringComparator.INSTANCE);

		// get all objects ordered alphabetically by name
		Iterator<IPersist> formElementIterator = ancestorForm.getAllObjects();


		boolean mobile = SolutionMetaData.isServoyMobileSolution(ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getSolution());

		// all named form elements must be added, as well as named fields inside
		// portal elements
		PlatformSimpleUserNode node;
		while (formElementIterator.hasNext())
		{
			PlatformSimpleUserNode parentNode = elementsNode;
			IPersist persist = formElementIterator.next();
			if (persist instanceof IFormElement)
			{
				// TODO: fix multiple anonymous groups (use proper content providers and label providers)
				IFormElement element = (IFormElement)persist;
				if (element.getGroupID() != null & !mobile)
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
								ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution(), (Form)persist.getParent()), null },
							originalForm, uiActivator.loadImageFromBundle("group.gif"));
						node.setDeveloperFeedback(new SimpleDeveloperFeedback(element.getName() + ".", null, null));
						elements.add(node);
						node.parent = parentNode;
					}
					parentNode = node; // this field comes under the group node
				}
				if (element.getName() != null && element.getName().trim().length() > 0)
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
							model = new FormElementGroup(element.getGroupID(),
								ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution(), (Form)persist.getParent());
						}
						else
						{
							model = element;
						}

						node = new PlatformSimpleUserNode(element.getName(), UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { model, null }, originalForm,
							uiActivator.loadImageFromBundle("element.gif"));
						node.setDeveloperFeedback(new SimpleDeveloperFeedback(element.getName() + ".", null, null));
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
								node = new PlatformSimpleUserNode(portalElement.getName(), UserNodeType.FORM_ELEMENTS_ITEM,
									new Object[] { portalElement, null }, originalForm, uiActivator.loadImageFromBundle("element.gif"));
								elements.add(node);
								node.parent = parentNode;
							}
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
			if (element == elementsNode && parentElementsNode != null && parentElementsNode.children.length > 0)
			{
				element.children = new PlatformSimpleUserNode[nodeChildren == null ? 1 : (nodeChildren.size() + 1)];
				element.children[0] = parentElementsNode;
				parentElementsNode.parent = element;
				i = 1;
			}
			else
			{
				element.children = new PlatformSimpleUserNode[nodeChildren == null ? 0 : nodeChildren.size()];
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
					bean.getParent(), uiActivator.loadImageFromBundle("element.gif"));
				node.setDeveloperFeedback(new SimpleDeveloperFeedback(bean.getName() + ".", null, null));

				Class< ? > superClass = beanClass.getSuperclass();
				PlatformSimpleUserNode parentClassNode = node;
				PlatformSimpleUserNode currentClassNode;
				while (superClass != null)
				{
					className = superClass.getSimpleName();
					currentClassNode = new PlatformSimpleUserNode(className, UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { bean, superClass },
						bean.getParent(), uiActivator.loadImageFromBundle("element.gif"));
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
					uiActivator.loadImageFromBundle("element.gif"));
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
				addReturnTypeNodes(node, scriptObject.getAllReturnedTypes());
			}
		}
		catch (Throwable e)
		{
			ServoyLog.logWarning("Solution explorer cannot create bean " + bean.getName(), e);
			node = new PlatformSimpleUserNode(bean.getName(), UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { bean, null }, bean.getParent(),
				uiActivator.loadImageFromBundle("element.gif"));
			node.setDeveloperFeedback(new SimpleDeveloperFeedback(bean.getName() + ".", null, null));
		}
		return node;
	}


	/**
	 * @return
	 */
	private PlatformSimpleUserNode createNodeForWebComponentBean(Bean bean)
	{
		PlatformSimpleUserNode node = new PlatformSimpleUserNode(bean.getName(), UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { bean, null },
			bean.getParent(), uiActivator.loadImageFromBundle("element.gif"));
		node.setDeveloperFeedback(new SimpleDeveloperFeedback(bean.getName() + ".", null, null));
		return node;
	}

//	public static boolean isWebcomponentBean(IPersist persist)
//	{
//		return persist instanceof Bean && ((Bean)persist).getBeanClassName() != null && ((Bean)persist).getBeanClassName().indexOf(':') > 0;
//	}

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
		try
		{
			addRelationsNodeChildren(formRelationsNode, f.getSolution(), f.getTable(), UserNodeType.RELATION);
		}
		catch (DbcpException e)
		{
			ServoyLog.logInfo("Cannot create " + formRelationsNode.getName() + " node: " + e.getMessage());
			disableServer(f.getServerName());
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
	}

	private void addRelationsNodeChildren(PlatformSimpleUserNode relationsNode, Solution solution, Table table, UserNodeType type)
	{
		try
		{
			Map<String, Solution> allSolutions = new HashMap<String, Solution>();

			solution.getReferencedModulesRecursive(allSolutions);

			allSolutions.put(solution.getName(), solution);

			List<PlatformSimpleUserNode> relationNodes = new ArrayList<PlatformSimpleUserNode>();
			TreeSet<Relation> relations = new TreeSet<Relation>(NameComparator.INSTANCE);
			Iterator<Solution> solutions = allSolutions.values().iterator();
			while (solutions.hasNext())
			{
				Solution sol = solutions.next();
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

	public void refreshContent(Set<IPersist> persists)
	{
		// optimize a bit so we don't refresh the same thing multiple times
		Iterator<IPersist> it = persists.iterator();
		List<String> solutionsRefreshedForRelations = new ArrayList<String>();
		while (it.hasNext())
		{
			IPersist persist = it.next();
			IRootObject root = persist.getRootObject();
			boolean refreshedFormsNode = false;

			if (persist instanceof IFormElement)
			{
				// don't refresh if we also refresh the solution
				if (persists.contains(root)) continue;

				IPersist parent = persist.getParent();
				if (parent instanceof Form)
				{
					// if the element's form was extended by other forms, the elements of those forms must be refreshed as well...
					FlattenedSolution flatSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();
					refreshElementsForForm(flatSolution, (Form)parent, new HashSet<Form>());
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
								node = (PlatformSimpleUserNode)findChildNode(node, Messages.TreeStrings_Scopes);
								if (node != null)
								{
									String[] scopeNames = ((Solution)root).getRuntimeProperty(Solution.SCOPE_NAMES);
									if (scopeNames != null) // when refreshScopes has not been called yet
									{
										PlatformSimpleUserNode scopeNode = null;
										for (String scopeName : scopeNames)
										{
											scopeNode = (PlatformSimpleUserNode)findChildNode(node, scopeName);
											if (scopeNode != null)
											{
												scopeNode = (PlatformSimpleUserNode)findChildNode(scopeNode, Messages.TreeStrings_relations);
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
							try
							{
								if (!solutionsRefreshedForRelations.contains(s.getName()))
								{
									// refresh all affected form relation nodes
									node = (PlatformSimpleUserNode)findChildNode(getSolutionNode(s.getName()), Messages.TreeStrings_Forms);
									if (node != null && node.children != null)
									{
										PlatformSimpleUserNode relationsNode;
										Form form;
										for (int i = node.children.length - 1; i >= 0; i--)
										{
											form = (Form)node.children[i].getRealObject();
											if (form.getTable() != null)
											{
												relationsNode = (PlatformSimpleUserNode)findChildNode(node.children[i], Messages.TreeStrings_relations);
												if (relationsNode != null)
												{
													addFormRelationsNodeChildren(relationsNode);
													view.refreshTreeNodeFromModel(relationsNode);
												}
											}
										}
									}
								}
								// if in calculation mode, refresh Relations node under
								// solution node
								if (solutionOfCalculation != null)
								{
									node = (PlatformSimpleUserNode)findChildNode(getSolutionNode(s.getName()), Messages.TreeStrings_Relations);
									if (node != null && tableOfCalculation.equals(((Relation)persist).getPrimaryTable()))
									{
										addRelationsNodeChildren(node, solutionOfCalculation, (Table)tableOfCalculation, UserNodeType.CALC_RELATION);
										view.refreshTreeNodeFromModel(node);
									}
								}
							}
							catch (RepositoryException e)
							{
								ServoyLog.logWarning("Exception while trying to refresh relation: " + persist, e);
							}
							if (!solutionsRefreshedForRelations.contains(s.getName())) solutionsRefreshedForRelations.add(s.getName());
						}
						else if (persist instanceof Form)
						{
							// don't refresh if we also refresh the solution
							if (persists.contains(s)) continue;

							node = (PlatformSimpleUserNode)findChildNode(node, Messages.TreeStrings_Forms);
							if (node != null)
							{
								PlatformSimpleUserNode formNode = (PlatformSimpleUserNode)findChildNode(node, ((Form)persist).getName());
								if (formNode == null)
								{
									if (!refreshedFormsNode)
									{
										refreshedFormsNode = true;
										addFormsNodeChildren(node);
									}
									else
									{
										node = null;
									}
								}
								else
								{
									node = formNode;
									node.children = null;
								}
								if (node != null)
								{
									view.refreshTreeNodeFromModel(node);
								}
							}
						}
						else if (persist instanceof Solution)
						{
							PlatformSimpleUserNode solutionChildNode = (PlatformSimpleUserNode)findChildNode(node, Messages.TreeStrings_Forms);
							if (solutionChildNode != null)
							{
								addFormsNodeChildren(solutionChildNode);
								view.refreshTreeNodeFromModel(solutionChildNode);
							}
							solutionChildNode = (PlatformSimpleUserNode)findChildNode(node, Messages.TreeStrings_Media);
							if (solutionChildNode != null)
							{
								addMediaFolderChildrenNodes(solutionChildNode, (Solution)persist);
								view.refreshTreeNodeFromModel(solutionChildNode);
							}
						}
					}
				}
			}
		}
	}

	private void refreshElementsForForm(FlattenedSolution flatSolution, Form form, HashSet<Form> alreadyVisitedForms)
	{
		alreadyVisitedForms.add(form);
		// see children of form
		if (flatSolution != null)
		{
			for (Form childForm : flatSolution.getDirectlyInheritingForms(form))
			{
				if (!alreadyVisitedForms.contains(childForm))
				{
					refreshElementsForForm(flatSolution, childForm, alreadyVisitedForms);
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
				node = (PlatformSimpleUserNode)findChildNode(node, Messages.TreeStrings_Forms);
				if (node != null)
				{
					node = (PlatformSimpleUserNode)findChildNode(node, form.getName());
					if (node != null)
					{
						node = (PlatformSimpleUserNode)findChildNode(node, Messages.TreeStrings_elements);
						if (node != null)
						{
							addFormElementsChildren(node);
							view.refreshTreeNodeFromModel(node);
						}
					}
				}
			}
		}
	}

	public SimpleUserNode findChildNode(SimpleUserNode node, String name)
	{
		SimpleUserNode result = null;
		if (node.children != null)
		{
			for (int i = node.children.length - 1; i >= 0; i--)
			{
				String nodeName = node.children[i].getName();
				if (nodeName != null && nodeName.equals(name))
				{
					result = node.children[i];
					break;
				}
			}
		}
		return result;
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
		return (PlatformSimpleUserNode)findChildNode(modulesOfActiveSolution, solutionName);
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

	public void refreshFormsNode(PlatformSimpleUserNode formsNode)
	{
		addFormsNodeChildren(formsNode);
		view.refreshTreeNodeFromModel(formsNode);
	}

	public void refreshServerViewsNode(IServerInternal server)
	{
		PlatformSimpleUserNode node = (PlatformSimpleUserNode)findChildNode(servers, server.getName());
		if (node != null)
		{
			handleServerViewsNode(server, node);
			view.refreshTreeNodeFromModel(node);
		}
	}

	public TreePath getTreePath(UUID uuid)
	{
		if (activeSolutionNode.getRealObject() != null)
		{
			List<PlatformSimpleUserNode> al = new ArrayList<PlatformSimpleUserNode>();
			if (findNode(activeSolutionNode, uuid, al))
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
				un = findChildNode(un, Messages.TreeStrings_Forms);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.core.IWebResourceChangedListener#changed()
	 */
	@Override
	public void changed()
	{
		Job job = new Job("Refreshing tree")
		{

			@Override
			public IStatus run(IProgressMonitor monitor)
			{
				componentsNode.children = null;
				view.refreshTreeNodeFromModel(componentsNode);
				servicesNode.children = null;
				view.refreshTreeNodeFromModel(servicesNode);
				return Status.OK_STATUS;
			}
		};
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.schedule();
	}

}