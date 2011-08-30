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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Icon;

import org.apache.commons.dbcp.DbcpException;
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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.mozilla.javascript.JavaMembers;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.node.SimpleDeveloperFeedback;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.scripting.CalculationModeHandler;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.eclipse.ui.util.IconProvider;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.EnableServerAction;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormManager.HistoryProvider;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.documentation.scripting.docs.JSLib;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
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
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.StringComparator;
import com.servoy.j2db.util.UUID;

/**
 * Content provider for the solution explorer tree.
 * 
 * @author jblok
 */
public class SolutionExplorerTreeContentProvider implements IStructuredContentProvider, ITreeContentProvider
{
	private static final String IMG_SOLUTION = "solution.gif"; //$NON-NLS-1$
	private static final String IMG_SOLUTION_M = "module.gif"; //$NON-NLS-1$
	private static final String IMG_SOLUTION_MODULE = "solution_module.gif"; //$NON-NLS-1$
	private static final String IMG_SOLUTION_MODULE_M = "solution_module_m.gif"; //$NON-NLS-1$
	private static final String IMG_SOLUTION_LOGIN = "solution_login.gif"; //$NON-NLS-1$
	private static final String IMG_SOLUTION_LOGIN_M = "solution_login_m.gif"; //$NON-NLS-1$
	private static final String IMG_SOLUTION_AUTHENTICATOR = "solution_auth.gif"; //$NON-NLS-1$
	private static final String IMG_SOLUTION_AUTHENTICATOR_M = "solution_auth_m.gif"; //$NON-NLS-1$
	private static final String IMG_SOLUTION_SMART_ONLY = "solution_smart_only.gif"; //$NON-NLS-1$
	private static final String IMG_SOLUTION_SMART_ONLY_M = "solution_smart_only_m.gif"; //$NON-NLS-1$
	private static final String IMG_SOLUTION_WEB_ONLY = "solution_web_only.gif"; //$NON-NLS-1$
	private static final String IMG_SOLUTION_WEB_ONLY_M = "solution_web_only_m.gif"; //$NON-NLS-1$

	private PlatformSimpleUserNode invisibleRootNode;

	private PlatformSimpleUserNode activeSolutionNode;

	private PlatformSimpleUserNode allSolutionsNode;

	private final PlatformSimpleUserNode databaseManager;

	private final PlatformSimpleUserNode solutionModel;

	private final PlatformSimpleUserNode history;

	private final PlatformSimpleUserNode servers;

	private final PlatformSimpleUserNode resources;

	private final PlatformSimpleUserNode stylesNode;

	private final PlatformSimpleUserNode templatesNode;

	private final PlatformSimpleUserNode i18nFilesNode;

	private final PlatformSimpleUserNode userGroupSecurityNode;

	private final PlatformSimpleUserNode security;

	private final PlatformSimpleUserNode i18n;

	private PlatformSimpleUserNode forms;

	private PlatformSimpleUserNode globalsFolder;

	private PlatformSimpleUserNode allRelations;

	private PlatformSimpleUserNode valuelists;

	private PlatformSimpleUserNode media;

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

	SolutionExplorerTreeContentProvider(SolutionExplorerView v)
	{
		view = v;
		invisibleRootNode = new PlatformSimpleUserNode("root", UserNodeType.ARRAY); //$NON-NLS-1$

		PlatformSimpleUserNode jslib = new PlatformSimpleUserNode(Messages.TreeStrings_JSLib, UserNodeType.JSLIB, null, IconProvider.instance().image(
			JSLib.class));

		PlatformSimpleUserNode jsarray = new PlatformSimpleUserNode(Messages.TreeStrings_Array, UserNodeType.ARRAY, null, IconProvider.instance().image(
			com.servoy.j2db.documentation.scripting.docs.Array.class));
		PlatformSimpleUserNode jsdate = new PlatformSimpleUserNode(Messages.TreeStrings_Date, UserNodeType.DATE, null, null, IconProvider.instance().image(
			com.servoy.j2db.documentation.scripting.docs.Date.class));
		PlatformSimpleUserNode jsstring = new PlatformSimpleUserNode(Messages.TreeStrings_String, UserNodeType.STRING, null, IconProvider.instance().image(
			com.servoy.j2db.documentation.scripting.docs.String.class));
		PlatformSimpleUserNode jsmath = new PlatformSimpleUserNode(Messages.TreeStrings_Math, UserNodeType.FUNCTIONS, null, IconProvider.instance().image(
			com.servoy.j2db.documentation.scripting.docs.Math.class));
		PlatformSimpleUserNode jsstatements = new PlatformSimpleUserNode(Messages.TreeStrings_Statements, UserNodeType.STATEMENTS, null,
			IconProvider.instance().image(com.servoy.j2db.documentation.scripting.docs.Statements.class));
		PlatformSimpleUserNode jsspecialops = new PlatformSimpleUserNode(Messages.TreeStrings_SpecialOperators, UserNodeType.SPECIAL_OPERATORS, null,
			IconProvider.instance().image(com.servoy.j2db.documentation.scripting.docs.SpecialOperators.class));
		PlatformSimpleUserNode jsxml = new PlatformSimpleUserNode(Messages.TreeStrings_XMLMethods, UserNodeType.XML_METHODS, null,
			IconProvider.instance().image(com.servoy.j2db.documentation.scripting.docs.XML.class));
		PlatformSimpleUserNode jsxmllist = new PlatformSimpleUserNode(Messages.TreeStrings_XMLListMethods, UserNodeType.XML_LIST_METHODS, null,
			IconProvider.instance().image(com.servoy.j2db.documentation.scripting.docs.XMLList.class));
		PlatformSimpleUserNode jsregexp = new PlatformSimpleUserNode(Messages.TreeStrings_RegExp, UserNodeType.REGEXP, null, IconProvider.instance().image(
			com.servoy.j2db.documentation.scripting.docs.RegExp.class));
		PlatformSimpleUserNode jsnumber = new PlatformSimpleUserNode(Messages.TreeStrings_Number, UserNodeType.NUMBER, null, IconProvider.instance().image(
			com.servoy.j2db.documentation.scripting.docs.Number.class));

		jslib.children = new PlatformSimpleUserNode[] { jsarray, jsdate, jsstring, jsnumber, jsmath, jsregexp, jsstatements, jsspecialops, jsxml, jsxmllist };
		jsarray.parent = jslib;
		jsdate.parent = jslib;
		jsstring.parent = jslib;
		jsnumber.parent = jslib;
		jsmath.parent = jslib;
		jsstatements.parent = jslib;
		jsspecialops.parent = jslib;
		jsxml.parent = jslib;
		jsxmllist.parent = jslib;


		PlatformSimpleUserNode application = new PlatformSimpleUserNode(Messages.TreeStrings_Application, UserNodeType.APPLICATION, null,
			IconProvider.instance().image(JSApplication.class));

		Class< ? >[] applicationClasses1 = ScriptObjectRegistry.getScriptObjectForClass(JSApplication.class).getAllReturnedTypes();
		Class< ? >[] applicationClasses2 = new ServoyException(0).getAllReturnedTypes();
		Class< ? >[] applicationClasses = new Class[applicationClasses1.length + applicationClasses2.length];
		System.arraycopy(applicationClasses1, 0, applicationClasses, 0, applicationClasses1.length);
		System.arraycopy(applicationClasses2, 0, applicationClasses, applicationClasses1.length, applicationClasses2.length);
		addReturnTypeNodes(application, applicationClasses);

		resources = new PlatformSimpleUserNode(Messages.TreeStrings_Resources, UserNodeType.RESOURCES, null, uiActivator.loadImageFromBundle("resources.png")); //$NON-NLS-1$

		stylesNode = new PlatformSimpleUserNode(Messages.TreeStrings_Styles, UserNodeType.STYLES, null, uiActivator.loadImageFromBundle("styles.gif")); //$NON-NLS-1$
		stylesNode.parent = resources;

		userGroupSecurityNode = new PlatformSimpleUserNode(Messages.TreeStrings_UserGroupSecurity, UserNodeType.USER_GROUP_SECURITY, null,
			uiActivator.loadImageFromBundle("lock.gif")); //$NON-NLS-1$
		userGroupSecurityNode.parent = resources;

		i18nFilesNode = new PlatformSimpleUserNode(Messages.TreeStrings_I18NFiles, UserNodeType.I18N_FILES, null, uiActivator.loadImageFromBundle("i18n.gif")); //$NON-NLS-1$
		i18nFilesNode.parent = resources;

		templatesNode = new PlatformSimpleUserNode(Messages.TreeStrings_Templates, UserNodeType.TEMPLATES, null,
			uiActivator.loadImageFromBundle("template.gif")); //$NON-NLS-1$
		templatesNode.parent = resources;

		activeSolutionNode = new PlatformSimpleUserNode(Messages.TreeStrings_NoActiveSolution, UserNodeType.SOLUTION, null,
			Messages.SolutionExplorerView_activeSolution, null, uiActivator.loadImageFromBundle("solution.gif")); //$NON-NLS-1$
		modulesOfActiveSolution = new PlatformSimpleUserNode(Messages.TreeStrings_Modules, UserNodeType.MODULES, null,
			uiActivator.loadImageFromBundle("modules.gif")); //$NON-NLS-1$

		allSolutionsNode = new PlatformSimpleUserNode(Messages.TreeStrings_AllSolutions, UserNodeType.ALL_SOLUTIONS, null,
			PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));

		databaseManager = new PlatformSimpleUserNode(Messages.TreeStrings_DatabaseManager, UserNodeType.FOUNDSET_MANAGER, null, IconProvider.instance().image(
			JSDatabaseManager.class));
		addReturnTypeNodes(databaseManager, ScriptObjectRegistry.getScriptObjectForClass(JSDatabaseManager.class).getAllReturnedTypes());
		PlatformSimpleUserNode[] children = (PlatformSimpleUserNode[])databaseManager.children;
		PlatformSimpleUserNode[] newChildren = new PlatformSimpleUserNode[children.length + 2];
		System.arraycopy(children, 0, newChildren, 0, children.length);
		newChildren[children.length] = new PlatformSimpleUserNode(FoundSet.JS_FOUNDSET, UserNodeType.RETURNTYPE, FoundSet.class, null);
		newChildren[children.length].parent = databaseManager;
		newChildren[children.length + 1] = new PlatformSimpleUserNode(Record.JS_RECORD, UserNodeType.RETURNTYPE, Record.class, null);
		newChildren[children.length + 1].parent = databaseManager;
		databaseManager.children = newChildren;

		PlatformSimpleUserNode utils = new PlatformSimpleUserNode(Messages.TreeStrings_Utils, UserNodeType.UTILS, null, IconProvider.instance().image(
			JSUtils.class));

		PlatformSimpleUserNode jsunit = new PlatformSimpleUserNode(Messages.TreeStrings_JSUnit, UserNodeType.JSUNIT, null, IconProvider.instance().image(
			JSUnitAssertFunctions.class));

		solutionModel = new PlatformSimpleUserNode(Messages.TreeStrings_SolutionModel, UserNodeType.SOLUTION_MODEL, null, IconProvider.instance().image(
			JSSolutionModel.class));

		addReturnTypeNodes(solutionModel, ScriptObjectRegistry.getScriptObjectForClass(JSSolutionModel.class).getAllReturnedTypes());

		history = new PlatformSimpleUserNode(Messages.TreeStrings_History, UserNodeType.HISTORY, null, IconProvider.instance().image(HistoryProvider.class));

		security = new PlatformSimpleUserNode(Messages.TreeStrings_Security, UserNodeType.SECURITY, null, IconProvider.instance().image(JSSecurity.class));
		addReturnTypeNodes(security, ScriptObjectRegistry.getScriptObjectForClass(JSSecurity.class).getAllReturnedTypes());

		i18n = new PlatformSimpleUserNode(Messages.TreeStrings_i18n, UserNodeType.I18N, null, IconProvider.instance().image(JSI18N.class));
		addReturnTypeNodes(i18n, ScriptObjectRegistry.getScriptObjectForClass(JSI18N.class).getAllReturnedTypes());

		servers = new PlatformSimpleUserNode(Messages.TreeStrings_DBServers, UserNodeType.SERVERS, null, uiActivator.loadImageFromBundle("database_srv.gif")); //$NON-NLS-1$
		final PlatformSimpleUserNode plugins = new PlatformSimpleUserNode(Messages.TreeStrings_Plugins, UserNodeType.PLUGINS, null,
			uiActivator.loadImageFromBundle("plugin.gif")); //$NON-NLS-1$


		resources.children = new PlatformSimpleUserNode[] { servers, stylesNode, userGroupSecurityNode, i18nFilesNode, templatesNode };

		invisibleRootNode.children = new PlatformSimpleUserNode[] { resources, allSolutionsNode, activeSolutionNode, jslib, application, solutionModel, databaseManager, utils, history, security, i18n, jsunit, plugins };
		jslib.parent = invisibleRootNode;
		application.parent = invisibleRootNode;
		resources.parent = invisibleRootNode;
		activeSolutionNode.parent = invisibleRootNode;
		allSolutionsNode.parent = invisibleRootNode;
		databaseManager.parent = invisibleRootNode;
		utils.parent = invisibleRootNode;
		history.parent = invisibleRootNode;
		solutionModel.parent = invisibleRootNode;
		security.parent = invisibleRootNode;
		i18n.parent = invisibleRootNode;
		jsunit.parent = invisibleRootNode;
		servers.parent = resources;
		plugins.parent = invisibleRootNode;
		modulesOfActiveSolution.parent = activeSolutionNode;

		scriptingNodes = new PlatformSimpleUserNode[] { jslib, application, solutionModel, databaseManager, utils, history, security, i18n, /* exceptions, */jsunit, plugins };
		resourceNodes = new PlatformSimpleUserNode[] { stylesNode, userGroupSecurityNode, i18nFilesNode, templatesNode };

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
			if (isEnabled) n.unhide();
			else n.hide();
			view.refreshTreeNodeFromModel(n);
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
						imgName = isModule ? IMG_SOLUTION_MODULE_M : IMG_SOLUTION_MODULE;
						break;
					case SolutionMetaData.LOGIN_SOLUTION :
						imgName = isModule ? IMG_SOLUTION_LOGIN_M : IMG_SOLUTION_LOGIN;
						break;
					case SolutionMetaData.AUTHENTICATOR :
						imgName = isModule ? IMG_SOLUTION_AUTHENTICATOR_M : IMG_SOLUTION_AUTHENTICATOR;
						break;
					case SolutionMetaData.SMART_CLIENT_ONLY :
						imgName = isModule ? IMG_SOLUTION_SMART_ONLY_M : IMG_SOLUTION_SMART_ONLY;
						break;
					case SolutionMetaData.WEB_CLIENT_ONLY :
						imgName = isModule ? IMG_SOLUTION_WEB_ONLY_M : IMG_SOLUTION_WEB_ONLY;
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

					node.setToolTipText(solutionName + "(" + getSolutionTypeAsString(servoyProject) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				else
				{
					PlatformSimpleUserNode node = new PlatformSimpleUserNode(displayValue, UserNodeType.SOLUTION_ITEM_NOT_ACTIVE_MODULE, servoyProject,
						getServoyProjectImage(servoyProject, false, true));
					node.setEnabled(false);
					allSolutionChildren.add(node);
					node.parent = allSolutionsNode;

					String solutionName = (String)servoyProject.getSolution().getProperty(StaticContentSpecLoader.PROPERTY_TITLETEXT.getPropertyName());
					if (solutionName == null) solutionName = servoyProject.getSolution().getName();

					node.setToolTipText(solutionName + "(" + getSolutionTypeAsString(servoyProject) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
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
			}
			activeSolutionNode.setDisplayName(name);

			String solutionName = (String)((ServoyProject)activeSolutionNode.getRealObject()).getSolution().getProperty(
				StaticContentSpecLoader.PROPERTY_TITLETEXT.getPropertyName());
			if (solutionName == null) solutionName = ((ServoyProject)activeSolutionNode.getRealObject()).getSolution().getName();

			activeSolutionNode.setToolTipText(solutionName + "(" + getSolutionTypeAsString((ServoyProject)activeSolutionNode.getRealObject()) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
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
		switch (project.getSolution().getSolutionType())
		{
			case SolutionMetaData.SOLUTION :
				return SolutionMetaData.solutionTypeNames[0];
			case SolutionMetaData.MODULE :
				return SolutionMetaData.solutionTypeNames[1];
			case SolutionMetaData.WEB_CLIENT_ONLY :
				return SolutionMetaData.solutionTypeNames[2];
			case SolutionMetaData.SMART_CLIENT_ONLY :
				return SolutionMetaData.solutionTypeNames[3];
			case SolutionMetaData.LOGIN_SOLUTION :
				return SolutionMetaData.solutionTypeNames[4];
			case SolutionMetaData.AUTHENTICATOR :
				return SolutionMetaData.solutionTypeNames[5];
			case SolutionMetaData.PRE_IMPORT_HOOK :
				return SolutionMetaData.solutionTypeNames[6];
			case SolutionMetaData.POST_IMPORT_HOOK :
				return SolutionMetaData.solutionTypeNames[7];
		}

		return ""; //$NON-NLS-1$
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
					if (un.children == null)
					{
						un.children = new PlatformSimpleUserNode[0];
					}
					return un.children;
				}
				catch (Exception e)
				{
					ServoyLog.logWarning("Cannot create the children of node " + un.getName(), e); //$NON-NLS-1$
				}
			}
			else
			{
				return un.children;
			}
		}
		return new Object[0];
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
					return ((Solution)un.getRealObject()).getForms(null, false).hasNext();
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
			}
			else if (un.children.length > 0)
			{
				return true;
			}
			return false;
		}
		return true;
	}

	private void addServersNodeChildren(PlatformSimpleUserNode serversNode)
	{
		List<PlatformSimpleUserNode> servers = new ArrayList<PlatformSimpleUserNode>();
		IServerManagerInternal handler = ServoyModel.getServerManager();
		String[] array = handler.getServerNames(false, false, true, true);
		for (String server_name : array)
		{
			IServerInternal serverObj = (IServerInternal)handler.getServer(server_name, false, false);
			Pair<String, IServerInternal> serverInfo = new Pair<String, IServerInternal>(server_name, serverObj);

			Object image = uiActivator.loadImageFromBundle("server.gif"); //$NON-NLS-1$
			String tooltip = serverObj.toHTML();


			if (serverObj.getConfig().isEnabled())
			{
				if (serverObj.isValid())
				{
					if (serverObj.getName().equals(serverInfo.getLeft()))
					{
						image = uiActivator.loadImageFromBundle("server.gif"); //$NON-NLS-1$
					}
					else
					{
						image = uiActivator.loadImageFromBundle("serverDuplicate.gif"); //$NON-NLS-1$
						tooltip = "Duplicate of " + serverObj.getName(); //$NON-NLS-1$
					}
				}
				else
				{
					image = uiActivator.loadImageFromBundle("serverError.gif"); //$NON-NLS-1$
				}
			}
			else
			{
				image = uiActivator.loadImageFromBundle("serverDisabled.gif"); //$NON-NLS-1$
			}


			PlatformSimpleUserNode node = new PlatformSimpleUserNode(server_name, UserNodeType.SERVER, "", tooltip, serverObj, image); //$NON-NLS-1$
			servers.add(node);
			node.parent = serversNode;
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
					PlatformSimpleUserNode viewNode = new PlatformSimpleUserNode("Views", UserNodeType.VIEWS, "", "Views", serverObj,
						PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));
					node.children = new PlatformSimpleUserNode[] { viewNode };
					viewNode.parent = node;
				}
			}
		}

		serversNode.children = servers.toArray(new PlatformSimpleUserNode[servers.size()]);
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
					Method method = plugin.getClass().getMethod("getScriptObject", (Class[])null); //$NON-NLS-1$
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
							image = UIUtils.getSWTImageFromSwingIcon(icon, view.getSite().getShell().getDisplay());
							if (image != null) imagesConvertedFromSwing.add(image);
						}
						if (image == null)
						{
							image = uiActivator.loadImageFromBundle("plugin_conn.gif"); //$NON-NLS-1$
						}

						PlatformSimpleUserNode node = new PlatformSimpleUserNode(plugin.getName(), UserNodeType.PLUGIN, scriptObject, image);
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
					ServoyLog.logError("Error loading plugin " + plugin.getName() + " exception: ", e); //$NON-NLS-1$ //$NON-NLS-2$
					PlatformSimpleUserNode node = new PlatformSimpleUserNode(plugin.getName() + " (not loaded!)", UserNodeType.PLUGIN, null, null,
						e.toString(), null, uiActivator.loadImageFromBundle("warning.gif")); //$NON-NLS-1$
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
				if (cls != null && !IDeprecated.class.isAssignableFrom(cls))
				{
					String nodeName = null;
					if (cls.isAnnotationPresent(ServoyDocumented.class))
					{
						ServoyDocumented sd = cls.getAnnotation(ServoyDocumented.class);
						if (sd.scriptingName() != null && sd.scriptingName().trim().length() > 0) nodeName = sd.scriptingName().trim();
					}
					if (nodeName == null)
					{
						int index = cls.getName().lastIndexOf("."); //$NON-NLS-1$
						int index2 = cls.getName().indexOf("$", index); //$NON-NLS-1$
						if (index2 != -1)
						{
							index = index2;
						}
						nodeName = cls.getName().substring(index + 1);
					}
					PlatformSimpleUserNode n = new PlatformSimpleUserNode(nodeName, UserNodeType.RETURNTYPE, cls, null);
					JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(cls, null);
					if (IConstantsObject.class.isAssignableFrom(cls) &&
						!(javaMembers instanceof InstanceJavaMembers && javaMembers.getMethodIds(false).size() > 0))
					{
						constantsChildren.add(n);
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
				children.add(constants = new PlatformSimpleUserNode("Constants", UserNodeType.RETURNTYPE_CONSTANT));
			}

			if (constants != null)
			{
				constants.children = constantsChildren.toArray(new PlatformSimpleUserNode[constantsChildren.size()]);
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
			if (solutionOfCalculation == null)
			{
				globalsFolder = new PlatformSimpleUserNode(Messages.TreeStrings_Globals, UserNodeType.GLOBALS_ITEM, solution,
					uiActivator.loadImageFromBundle("globe.gif")); //$NON-NLS-1$
			}
			else
			{
				globalsFolder = new PlatformSimpleUserNode(Messages.TreeStrings_Globals, UserNodeType.GLOBALS_ITEM_CALCULATION_MODE, solution,
					uiActivator.loadImageFromBundle("globe.gif")); //$NON-NLS-1$
			}
			addGlobalsNodeChildren(globalsFolder, solution);

			allRelations = null;
			forms = new PlatformSimpleUserNode(Messages.TreeStrings_Forms, UserNodeType.FORMS, solution, uiActivator.loadImageFromBundle("forms.gif"));
			forms.parent = projectNode;
			if (solutionOfCalculation == null)
			{
				allRelations = new PlatformSimpleUserNode(Messages.TreeStrings_Relations, UserNodeType.ALL_RELATIONS, solution,
					uiActivator.loadImageFromOldLocation("relationsoverview.gif")); //$NON-NLS-1$
				allRelations.parent = projectNode;
			}
			valuelists = new PlatformSimpleUserNode(Messages.TreeStrings_ValueLists, UserNodeType.VALUELISTS, solution,
				uiActivator.loadImageFromBundle("valuelists.gif")); //$NON-NLS-1$
			media = new PlatformSimpleUserNode(Messages.TreeStrings_Media, UserNodeType.MEDIA, solution, uiActivator.loadImageFromBundle("image.gif"));

			globalsFolder.parent = projectNode;
			valuelists.parent = projectNode;
			media.parent = projectNode;

			if (solutionOfCalculation != null)
			{
				// in case of calculation editor
				PlatformSimpleUserNode dataProvidersNode = new PlatformSimpleUserNode(Messages.TreeStrings_DataProviders, UserNodeType.TABLE_COLUMNS,
					tableOfCalculation, solution, uiActivator.loadImageFromBundle("selected_record.gif")); //$NON-NLS-1$
				allRelations = new PlatformSimpleUserNode(Messages.TreeStrings_Relations, UserNodeType.RELATIONS, tableOfCalculation,
					uiActivator.loadImageFromOldLocation("relationsoverview.gif")); //$NON-NLS-1$
				addRelationsNodeChildren(allRelations, solution, (Table)tableOfCalculation, UserNodeType.CALC_RELATION);

				dataProvidersNode.parent = projectNode;
				allRelations.parent = projectNode;
				projectNode.children = new PlatformSimpleUserNode[] { globalsFolder, dataProvidersNode, forms, allRelations, valuelists, media };
				forms.hide();
			}
			else
			{
				projectNode.children = new PlatformSimpleUserNode[] { globalsFolder, forms, allRelations, valuelists, media };
				// solution's allRelations not allowed in login solutions
				if (activeSolutionNode != null &&
					((ServoyProject)activeSolutionNode.getRealObject()).getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION &&
					allRelations != null) allRelations.hide();
			}

		}
	}

	private void addGlobalsNodeChildren(PlatformSimpleUserNode globalsFolder, Solution solution)
	{
		PlatformSimpleUserNode globalVariables = new PlatformSimpleUserNode(Messages.TreeStrings_variables, UserNodeType.GLOBAL_VARIABLES, solution,
			uiActivator.loadImageFromBundle("global_variabletree.gif")); //$NON-NLS-1$
		globalVariables.parent = globalsFolder;

		PlatformSimpleUserNode currentForm = new PlatformSimpleUserNode(Messages.TreeStrings_currentcontroller, UserNodeType.CURRENT_FORM, null,
			uiActivator.loadImageFromBundle("formula.gif")); //$NON-NLS-1$
		if (solutionOfCalculation != null) currentForm.hide();
		currentForm.parent = globalsFolder;

		PlatformSimpleUserNode globalRelations = new PlatformSimpleUserNode(Messages.TreeStrings_relations, UserNodeType.GLOBALRELATIONS, solution,
			uiActivator.loadImageFromOldLocation("relationsoverview.gif")); //$NON-NLS-1$
		addGlobalRelationsNodeChildren(globalRelations);
		globalRelations.parent = globalsFolder;
		globalsFolder.children = new PlatformSimpleUserNode[] { currentForm, globalVariables, globalRelations };

		// globals relations not allowed in login solution

		if (activeSolutionNode != null &&
			((ServoyProject)activeSolutionNode.getRealObject()).getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION &&
			solutionOfCalculation == null) globalRelations.hide();
	}

	private void addFormsNodeChildren(PlatformSimpleUserNode formsNode)
	{
		Solution solution = (Solution)formsNode.getRealObject();
		List<PlatformSimpleUserNode> forms = new ArrayList<PlatformSimpleUserNode>();
		Iterator<Form> it = solution.getForms(null, true);
		while (it.hasNext())
		{
			Form f = it.next();
			PlatformSimpleUserNode node = new PlatformSimpleUserNode(f.getName(), UserNodeType.FORM, f.getName(), f.getDataSource() == null ? "No table"
				: ("Server: " + f.getServerName() + ", Table: " + f.getTableName()), f, uiActivator.loadImageFromBundle("designer.gif")); //$NON-NLS-3$
			forms.add(node);
			node.parent = formsNode;
		}
		formsNode.setChildren(forms.toArray(new PlatformSimpleUserNode[forms.size()]));
	}

	private void addFormNodeChildren(PlatformSimpleUserNode formNode)
	{
		Form f = (Form)formNode.getRealObject();
		try
		{
			List<PlatformSimpleUserNode> node = new ArrayList<PlatformSimpleUserNode>();
			PlatformSimpleUserNode functionsNode = new PlatformSimpleUserNode(Messages.TreeStrings_controller, UserNodeType.FORM_CONTROLLER, f,
				uiActivator.loadImageFromBundle("formula.gif")); //$NON-NLS-1$
			functionsNode.parent = formNode;
			node.add(functionsNode);

			PlatformSimpleUserNode variables = new PlatformSimpleUserNode(Messages.TreeStrings_variables, UserNodeType.FORM_VARIABLES, f,
				uiActivator.loadImageFromBundle("form_variabletree.gif")); //$NON-NLS-1$
			variables.parent = formNode;
			node.add(variables);

			PlatformSimpleUserNode elementsNode = new PlatformSimpleUserNode(Messages.TreeStrings_elements, UserNodeType.FORM_ELEMENTS, f,
				uiActivator.loadImageFromBundle("elements.gif")); //$NON-NLS-1$
			elementsNode.parent = formNode;
			node.add(elementsNode);
			addFormElementsChildren(elementsNode);

			if (f.getDataSource() != null)
			{
				PlatformSimpleUserNode columnsNode = null;
				try
				{
					columnsNode = new PlatformSimpleUserNode(Messages.TreeStrings_selectedrecord, UserNodeType.TABLE_COLUMNS, f.getTable(), f,
						uiActivator.loadImageFromBundle("selected_record.gif")); //$NON-NLS-1$
					columnsNode.parent = formNode;
					node.add(columnsNode);
				}
				catch (DbcpException e)
				{
					ServoyLog.logInfo("Cannot create 'selectedrecord' node for " + formNode.getName() + ": " + e.getMessage());
					disableServer(f.getServerName());
				}

				PlatformSimpleUserNode relationsNode = new PlatformSimpleUserNode(Messages.TreeStrings_relations, UserNodeType.RELATIONS, f, f,
					uiActivator.loadImageFromOldLocation("relationsoverview.gif")); //$NON-NLS-1$
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
						uiActivator.loadImageFromBundle("elements.gif")); //$NON-NLS-1$

					addFormElementsChildren(parentElementsNode, inspectedForms, originalForm, parentForm);
				}
				else
				{
					ServoyLog.logWarning("Cycle detected in form hierarchy - at form " + parentForm.getName(), null); //$NON-NLS-1$
				}
			}
			else
			{
				ServoyLog.logWarning("Parent of extended form " + ancestorForm.getName() + " not found", null); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		List<PlatformSimpleUserNode> elements = new SortedList<PlatformSimpleUserNode>(StringComparator.INSTANCE);

		// get all objects ordered alphabetically by name
		Iterator<IPersist> formElementIterator = ancestorForm.getAllObjects();

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
				if (element.getGroupID() != null)
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
							originalForm, uiActivator.loadImageFromBundle("group.gif")); //$NON-NLS-1$
						node.setDeveloperFeedback(new SimpleDeveloperFeedback(element.getName() + ".", null, null)); //$NON-NLS-1$
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
						node = new PlatformSimpleUserNode(element.getName(), UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { element, null }, originalForm,
							uiActivator.loadImageFromBundle("element.gif")); //$NON-NLS-1$
						node.setDeveloperFeedback(new SimpleDeveloperFeedback(element.getName() + ".", null, null)); //$NON-NLS-1$
					}
					elements.add(node);
					node.parent = parentNode;
				}
				if (element instanceof Portal)
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
									new Object[] { portalElement, null }, originalForm, uiActivator.loadImageFromBundle("element.gif")); //$NON-NLS-1$
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
				node = new PlatformSimpleUserNode(bean.getName() + " (" + className + ')', UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { bean, beanClass }, //$NON-NLS-1$
					bean.getParent(), uiActivator.loadImageFromBundle("element.gif")); //$NON-NLS-1$
				node.setDeveloperFeedback(new SimpleDeveloperFeedback(bean.getName() + ".", null, null)); //$NON-NLS-1$

				Class< ? > superClass = beanClass.getSuperclass();
				PlatformSimpleUserNode parentClassNode = node;
				PlatformSimpleUserNode currentClassNode;
				while (superClass != null)
				{
					className = superClass.getSimpleName();
					currentClassNode = new PlatformSimpleUserNode(className, UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { bean, superClass },
						bean.getParent(), uiActivator.loadImageFromBundle("element.gif")); //$NON-NLS-1$
					currentClassNode.setDeveloperFeedback(new SimpleDeveloperFeedback(bean.getName() + ".", null, null)); //$NON-NLS-1$
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
					uiActivator.loadImageFromBundle("element.gif")); //$NON-NLS-1$
				node.setDeveloperFeedback(new SimpleDeveloperFeedback(bean.getName() + ".", null, null)); //$NON-NLS-1$
			}

			// add returnTypes if needed
			IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(beanClass);
			if (scriptObject != null)
			{
				addReturnTypeNodes(node, scriptObject.getAllReturnedTypes());
			}
		}
		catch (Throwable e)
		{
			ServoyLog.logWarning("Solution explorer cannot create bean " + bean.getName(), e); //$NON-NLS-1$
			node = new PlatformSimpleUserNode(bean.getName(), UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { bean, null }, bean.getParent(),
				uiActivator.loadImageFromBundle("element.gif")); //$NON-NLS-1$
			node.setDeveloperFeedback(new SimpleDeveloperFeedback(bean.getName() + ".", null, null)); //$NON-NLS-1$
		}
		return node;
	}

	private void addGlobalRelationsNodeChildren(PlatformSimpleUserNode globalRelations)
	{
		Solution solution = (Solution)globalRelations.getRealObject();
		try
		{
			List<PlatformSimpleUserNode> rels = new ArrayList<PlatformSimpleUserNode>();
			Iterator<Relation> it = solution.getRelations(null, true, true); // returns all global relations
			while (it.hasNext())
			{
				Relation r = it.next();
				PlatformSimpleUserNode un = new PlatformSimpleUserNode(r.getName(), UserNodeType.RELATION, r,
					uiActivator.loadImageFromBundle("global_relation.gif")); //$NON-NLS-1$
				un.parent = globalRelations;
				rels.add(un);
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
			ServoyLog.logInfo("Cannot create " + formRelationsNode.getName() + " node: " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
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
				PlatformSimpleUserNode un = new PlatformSimpleUserNode(r.getName(), type, r, solution, uiActivator.loadImageFromBundle("relation.gif")); //$NON-NLS-1$
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

	/**
	 * Decides whether or not the given persist changing affects the currently cached tree content, and if it does - it will refresh that content.
	 * 
	 * @param persist the persist that changed.
	 */
	public void refreshContent(IPersist persist)
	{
		if (persist instanceof IFormElement)
		{
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
			IRootObject root = persist.getRootObject();
			if (root instanceof Solution)
			{
				Solution s = (Solution)root;
				PlatformSimpleUserNode node = getSolutionNode(s.getName());

				if (node != null)
				{
					// find the node and refresh it
					if (persist instanceof Relation && ((Relation)persist).getName() != null)
					{
						// refresh global relations node - if possible
						node = (PlatformSimpleUserNode)findChildNode(node, Messages.TreeStrings_Globals);
						if (node != null)
						{
							node = (PlatformSimpleUserNode)findChildNode(node, Messages.TreeStrings_relations);
							if (node != null)
							{
								addGlobalRelationsNodeChildren(node);
								view.refreshTreeNodeFromModel(node);
							}
						}
						try
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
							ServoyLog.logWarning("Exception while trying to refresh relation: " + persist, e); //$NON-NLS-1$
						}

					}
					else if (persist instanceof Form)
					{
						node = (PlatformSimpleUserNode)findChildNode(node, Messages.TreeStrings_Forms);
						if (node != null)
						{
							PlatformSimpleUserNode formNode = (PlatformSimpleUserNode)findChildNode(node, ((Form)persist).getName());
							if (formNode == null)
							{
								addFormsNodeChildren(node);
							}
							else
							{
								node = formNode;
								node.children = null;
							}
							view.refreshTreeNodeFromModel(node);
						}
					}
					else if (persist instanceof Solution)
					{
						node = (PlatformSimpleUserNode)findChildNode(node, Messages.TreeStrings_Forms);
						if (node != null)
						{
							addFormsNodeChildren(node);
							view.refreshTreeNodeFromModel(node);
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
					ServoyLog.logWarning("Cycle in form hierarchy at form " + childForm.getName(), null); //$NON-NLS-1$
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

	private SimpleUserNode findChildNode(SimpleUserNode node, String name)
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
		else
		{
			return (PlatformSimpleUserNode)findChildNode(modulesOfActiveSolution, solutionName);
		}
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

	public PlatformSimpleUserNode getForms()
	{
		return forms;
	}

	public PlatformSimpleUserNode getGlobalsFolder()
	{
		return globalsFolder;
	}

	public PlatformSimpleUserNode getRelations()
	{
		return allRelations;
	}

	public PlatformSimpleUserNode getValuelists()
	{
		return valuelists;
	}

	public PlatformSimpleUserNode getMedia()
	{
		return media;
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

	/**
	 * @param persist
	 * @return
	 */
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

	/**
	 * @param startNode
	 * @param uuid
	 * @return
	 */
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

	public Object getNodeForPersist(IPersist currentActiveEditorPersist)
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
				}
			}
		}
		return un;
	}

}