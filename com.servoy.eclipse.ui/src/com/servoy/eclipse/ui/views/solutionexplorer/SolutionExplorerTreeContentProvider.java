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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.node.SimpleDeveloperFeedback;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.scripting.CalculationModeHandler;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.EnableServerAction;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.dataprocessing.Record;
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
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.scripting.IDeprecated;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.JSApplication;
import com.servoy.j2db.scripting.JSI18N;
import com.servoy.j2db.scripting.JSSecurity;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;
import com.servoy.j2db.smart.dataui.SwingItemFactory;
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

	private PlatformSimpleUserNode allRelations;

	private PlatformSimpleUserNode valuelists;

	private PlatformSimpleUserNode media;

	private final SolutionExplorerView view;

	private Solution solutionOfCalculation;

	private ITable tableOfCalculation;

	private final PlatformSimpleUserNode modulesOfActiveSolution;

	private final Object pluginsBackgroundLoadLock = new Object();

	private final List<String> unreachableServers = new ArrayList();

	private final com.servoy.eclipse.ui.Activator uiActivator = com.servoy.eclipse.ui.Activator.getDefault();

	SolutionExplorerTreeContentProvider(SolutionExplorerView v)
	{
		view = v;
		invisibleRootNode = new PlatformSimpleUserNode("root", UserNodeType.ARRAY);

		PlatformSimpleUserNode jslib = new PlatformSimpleUserNode(Messages.TreeStrings_JSLib, UserNodeType.JSLIB, null,
			uiActivator.loadImageFromBundle("jslibfolder.gif"));

		PlatformSimpleUserNode jsarray = new PlatformSimpleUserNode(Messages.TreeStrings_Array, UserNodeType.ARRAY, null,
			uiActivator.loadImageFromBundle("jslibarray.gif"));
		PlatformSimpleUserNode jsdate = new PlatformSimpleUserNode(Messages.TreeStrings_Date, UserNodeType.DATE, null, null,
			uiActivator.loadImageFromOldLocation("day_obj.gif"));
		PlatformSimpleUserNode jsstring = new PlatformSimpleUserNode(Messages.TreeStrings_String, UserNodeType.STRING, null,
			uiActivator.loadImageFromBundle("jslibstring.gif"));
		PlatformSimpleUserNode jsmath = new PlatformSimpleUserNode(Messages.TreeStrings_Math, UserNodeType.FUNCTIONS, null,
			uiActivator.loadImageFromBundle("sum.gif"));
		PlatformSimpleUserNode jsstatements = new PlatformSimpleUserNode(Messages.TreeStrings_Statements, UserNodeType.STATEMENTS, null,
			uiActivator.loadImageFromBundle("statements.gif"));
		PlatformSimpleUserNode jsspecialops = new PlatformSimpleUserNode(Messages.TreeStrings_SpecialOperators, UserNodeType.SPECIAL_OPERATORS, null,
			uiActivator.loadImageFromBundle("special_operators.gif"));
		PlatformSimpleUserNode jsxml = new PlatformSimpleUserNode(Messages.TreeStrings_XMLMethods, UserNodeType.XML_METHODS, null,
			uiActivator.loadImageFromBundle("xml_image.gif"));
		PlatformSimpleUserNode jsxmllist = new PlatformSimpleUserNode(Messages.TreeStrings_XMLListMethods, UserNodeType.XML_LIST_METHODS, null,
			uiActivator.loadImageFromBundle("xmlList_image.gif"));
		PlatformSimpleUserNode jsregexp = new PlatformSimpleUserNode(Messages.TreeStrings_RegExp, UserNodeType.REGEXP, null,
			uiActivator.loadImageFromBundle("regExp_image.gif"));
		PlatformSimpleUserNode jsnumber = new PlatformSimpleUserNode(Messages.TreeStrings_Number, UserNodeType.NUMBER, null,
			uiActivator.loadImageFromBundle("number.gif"));

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
			uiActivator.loadImageFromBundle("application.gif"));
		addReturnTypeNodes(application, ScriptObjectRegistry.getScriptObjectForClass(JSApplication.class).getAllReturnedTypes());

		resources = new PlatformSimpleUserNode(Messages.TreeStrings_Resources, UserNodeType.RESOURCES, null, uiActivator.loadImageFromBundle("resources.png"));

		stylesNode = new PlatformSimpleUserNode(Messages.TreeStrings_Styles, UserNodeType.STYLES, null, uiActivator.loadImageFromBundle("styles.gif"));
		stylesNode.parent = resources;

		userGroupSecurityNode = new PlatformSimpleUserNode(Messages.TreeStrings_UserGroupSecurity, UserNodeType.USER_GROUP_SECURITY, null,
			uiActivator.loadImageFromBundle("lock.gif"));
		userGroupSecurityNode.parent = resources;

		i18nFilesNode = new PlatformSimpleUserNode(Messages.TreeStrings_I18NFiles, UserNodeType.I18N_FILES, null, uiActivator.loadImageFromBundle("i18n.gif"));
		i18nFilesNode.parent = resources;

		templatesNode = new PlatformSimpleUserNode(Messages.TreeStrings_Templates, UserNodeType.TEMPLATES, null,
			uiActivator.loadImageFromBundle("template.gif"));
		templatesNode.parent = resources;

		activeSolutionNode = new PlatformSimpleUserNode(Messages.TreeStrings_NoActiveSolution, UserNodeType.SOLUTION, null,
			Messages.SolutionExplorerView_activeSolution, null, uiActivator.loadImageFromBundle("solution.gif"));
		modulesOfActiveSolution = new PlatformSimpleUserNode(Messages.TreeStrings_Modules, UserNodeType.MODULES, null,
			uiActivator.loadImageFromBundle("modules.gif"));

		allSolutionsNode = new PlatformSimpleUserNode(Messages.TreeStrings_AllSolutions, UserNodeType.ALL_SOLUTIONS, null,
			PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));

		databaseManager = new PlatformSimpleUserNode(Messages.TreeStrings_DatabaseManager, UserNodeType.FOUNDSET_MANAGER, null,
			uiActivator.loadImageFromBundle("server.gif"));
		addReturnTypeNodes(databaseManager, ScriptObjectRegistry.getScriptObjectForClass(JSDatabaseManager.class).getAllReturnedTypes());
		PlatformSimpleUserNode[] children = (PlatformSimpleUserNode[])databaseManager.children;
		PlatformSimpleUserNode[] newChildren = new PlatformSimpleUserNode[children.length + 2];
		System.arraycopy(children, 0, newChildren, 0, children.length);
		newChildren[children.length] = new PlatformSimpleUserNode(FoundSet.JS_FOUNDSET, UserNodeType.RETURNTYPE, FoundSet.class, null);
		newChildren[children.length].parent = databaseManager;
		newChildren[children.length + 1] = new PlatformSimpleUserNode(Record.JS_RECORD, UserNodeType.RETURNTYPE, Record.class, null);
		newChildren[children.length + 1].parent = databaseManager;
		databaseManager.children = newChildren;

		PlatformSimpleUserNode utils = new PlatformSimpleUserNode(Messages.TreeStrings_Utils, UserNodeType.UTILS, null,
			uiActivator.loadImageFromOldLocation("toolbox.gif"));

		PlatformSimpleUserNode jsunit = new PlatformSimpleUserNode(Messages.TreeStrings_JSUnit, UserNodeType.JSUNIT, null,
			uiActivator.loadImageFromBundle("jsunit.png"));

		solutionModel = new PlatformSimpleUserNode("SolutionModel", UserNodeType.SOLUTION_MODEL, null, uiActivator.loadImageFromBundle("blueprint.gif"));

		addReturnTypeNodes(solutionModel, ScriptObjectRegistry.getScriptObjectForClass(JSSolutionModel.class).getAllReturnedTypes());

		history = new PlatformSimpleUserNode(Messages.TreeStrings_History, UserNodeType.HISTORY, null, uiActivator.loadImageFromBundle("history.gif"));

		security = new PlatformSimpleUserNode(Messages.TreeStrings_Security, UserNodeType.SECURITY, null, uiActivator.loadImageFromBundle("lock.gif"));
		addReturnTypeNodes(security, ScriptObjectRegistry.getScriptObjectForClass(JSSecurity.class).getAllReturnedTypes());

		i18n = new PlatformSimpleUserNode(Messages.TreeStrings_i18n, UserNodeType.I18N, null, uiActivator.loadImageFromBundle("i18n.gif"));
		addReturnTypeNodes(i18n, ScriptObjectRegistry.getScriptObjectForClass(JSI18N.class).getAllReturnedTypes());

		PlatformSimpleUserNode exceptions = new PlatformSimpleUserNode(Messages.TreeStrings_ServoyException, UserNodeType.EXCEPTIONS, null,
			uiActivator.loadImageFromBundle("exception.gif"));
		addReturnTypeNodes(exceptions, new ServoyException(0).getAllReturnedTypes());

		servers = new PlatformSimpleUserNode(Messages.TreeStrings_DBServers, UserNodeType.SERVERS, null, uiActivator.loadImageFromBundle("database_srv.gif"));
		final PlatformSimpleUserNode plugins = new PlatformSimpleUserNode(Messages.TreeStrings_Plugins, UserNodeType.PLUGINS, null,
			uiActivator.loadImageFromBundle("plugin.gif"));


		resources.children = new PlatformSimpleUserNode[] { servers, stylesNode, userGroupSecurityNode, i18nFilesNode, templatesNode };

		invisibleRootNode.children = new PlatformSimpleUserNode[] { resources, allSolutionsNode, activeSolutionNode, jslib, application, solutionModel, databaseManager, utils, history, security, i18n, exceptions, jsunit, plugins };
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
		exceptions.parent = invisibleRootNode;
		jsunit.parent = invisibleRootNode;
		servers.parent = resources;
		plugins.parent = invisibleRootNode;
		modulesOfActiveSolution.parent = activeSolutionNode;

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

	private void addSolutionProjects(ServoyProject[] projects)
	{
		Image imgActive = uiActivator.loadImageFromBundle("solution.gif");
		Image imgActiveGrayedOut = uiActivator.loadImageFromBundle("solution.gif", true);
		if (projects.length == 0)
		{
			activeSolutionNode.children = new PlatformSimpleUserNode[0];
			activeSolutionNode.setIcon(imgActiveGrayedOut);
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
		Image imgOpen = uiActivator.loadImageFromBundle("module.gif");
		Image imgOpenGrayedOut = uiActivator.loadImageFromBundle("module.gif", true);
		Image imgClosed = uiActivator.loadImageFromBundle("closed_project.gif", true);

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
						node = new PlatformSimpleUserNode(displayValue, UserNodeType.SOLUTION_ITEM, servoyProject, imgActiveGrayedOut);
					}
					else
					{
						node = new PlatformSimpleUserNode(displayValue, UserNodeType.SOLUTION_ITEM, servoyProject, expandable ? imgOpen : imgOpenGrayedOut);
						node.setEnabled(expandable);
						modulesNodeChildren.add(node);
						node.parent = modulesOfActiveSolution;

						node = new PlatformSimpleUserNode(displayValue, UserNodeType.SOLUTION_ITEM, servoyProject, imgOpenGrayedOut);
					}

					node.setEnabled(false);
					allSolutionChildren.add(node);
					node.parent = allSolutionsNode;
				}
				else
				{
					PlatformSimpleUserNode node = new PlatformSimpleUserNode(displayValue, UserNodeType.SOLUTION_ITEM_NOT_ACTIVE_MODULE, servoyProject,
						imgClosed);
					node.setEnabled(false);
					allSolutionChildren.add(node);
					node.parent = allSolutionsNode;
				}
			}
		}

		// set active solution node to usable/unusable
		activeSolutionNode.setRealObject(servoyModel.getActiveProject());
		if (activeSolutionNode.getRealObject() != null)
		{
			boolean isLoginSolution = ((ServoyProject)activeSolutionNode.getRealObject()).getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION;

			if (isLoginSolution) imgActive = uiActivator.loadImageFromBundle("login_solution.gif");
			activeSolutionNode.setIcon(imgActive);
			String name = ((ServoyProject)activeSolutionNode.getRealObject()).getProject().getName();
			if (solutionOfCalculation != null)
			{
				name += " (calculation mode)";
			}
			else
			{
				// databaseManager not allowed in login solution
				if (isLoginSolution) databaseManager.hide();
				else databaseManager.unhide();
			}
			activeSolutionNode.setDisplayName(name);
		}
		else
		{
			activeSolutionNode.setIcon(imgActiveGrayedOut);
			activeSolutionNode.setDisplayName(Messages.TreeStrings_NoActiveSolution);
		}

		// add children to nodes...
		modulesOfActiveSolution.children = modulesNodeChildren.toArray(new PlatformSimpleUserNode[modulesNodeChildren.size()]);
		allSolutionsNode.children = allSolutionChildren.toArray(new PlatformSimpleUserNode[allSolutionChildren.size()]);
		Arrays.sort(modulesOfActiveSolution.children, StringComparator.INSTANCE);
		Arrays.sort(allSolutionsNode.children, StringComparator.INSTANCE);
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
					ServoyLog.logWarning("Cannot create the children of node " + un.getName(), e);
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
		ArrayList<PlatformSimpleUserNode> servers = new ArrayList<PlatformSimpleUserNode>();
		IServerManagerInternal handler = ServoyModel.getServerManager();
		String[] array = handler.getServerNames(false, false, true, true);
		for (String server_name : array)
		{
			IServerInternal serverObj = (IServerInternal)handler.getServer(server_name, false, false);
			Pair serverInfo = new Pair(server_name, serverObj);

			Object image = uiActivator.loadImageFromBundle("server.gif");
			String tooltip = serverObj.toHTML();


			if (serverObj.getConfig().isEnabled())
			{
				if (serverObj.isValid())
				{
					if (serverObj.getName().equals(serverInfo.getLeft()))
					{
						image = uiActivator.loadImageFromBundle("server.gif");
					}
					else
					{
						image = uiActivator.loadImageFromBundle("serverDuplicate.gif");
						tooltip = "Duplicate of " + serverObj.getName(); //$NON-NLS-1$
					}
				}
				else
				{
					image = uiActivator.loadImageFromBundle("serverError.gif");
				}
			}
			else
			{
				image = uiActivator.loadImageFromBundle("serverDisabled.gif");
			}


			PlatformSimpleUserNode node = new PlatformSimpleUserNode(server_name, UserNodeType.SERVER, "", tooltip, serverObj, image);
			servers.add(node);
			node.parent = serversNode;
			if (serverObj.getConfig().isEnabled() && serverObj.isValid())
			{
				List<String> views = null;
				try
				{
					views = serverObj.getViewNames();
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
					IScriptObject scriptObject = plugin.getScriptObject();
					if (scriptObject != null)
					{
						Icon icon = plugin.getImage();
						Object image = null; // will need SWT image
						if (icon != null)
						{
							image = UIUtils.getSWTImageFromSwingIcon(icon, view.getSite().getShell().getDisplay());
						}
						if (image == null)
						{
							image = uiActivator.loadImageFromBundle("plugin_conn.gif");
						}

						PlatformSimpleUserNode node = new PlatformSimpleUserNode(plugin.getName(), UserNodeType.PLUGIN, scriptObject, image);
						if (view.isNonEmptyPlugin(node))
						{
							plugins.add(node);
							node.parent = pluginNode;
							Class< ? >[] clss = scriptObject.getAllReturnedTypes();
							addReturnTypeNodes(node, clss);
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
		}
	}

	private void addReturnTypeNodes(PlatformSimpleUserNode node, Class[] clss)
	{
		if (clss != null)
		{
			List<PlatformSimpleUserNode> children = new ArrayList<PlatformSimpleUserNode>();
			List<PlatformSimpleUserNode> constantsChildren = new ArrayList<PlatformSimpleUserNode>();
			for (Class cls : clss)
			{
				if (cls != null && !IDeprecated.class.isAssignableFrom(cls))
				{
					int index = cls.getName().lastIndexOf("."); //$NON-NLS-1$
					int index2 = cls.getName().indexOf("$", index); //$NON-NLS-1$
					if (index2 != -1)
					{
						index = index2;
					}
					PlatformSimpleUserNode n = new PlatformSimpleUserNode(cls.getName().substring(index + 1), UserNodeType.RETURNTYPE, cls, null);
					JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(cls, null);
					if (IPrefixedConstantsObject.class.isAssignableFrom(cls) &&
						!(javaMembers instanceof InstanceJavaMembers && javaMembers.getMethodIds(false).size() > 0))
					{
						constantsChildren.add(n);
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
				children.add(0, constants = new PlatformSimpleUserNode("Constants", UserNodeType.RETURNTYPE_CONSTANT));
			}
			node.children = children.toArray(new PlatformSimpleUserNode[children.size()]);
			if (constants != null)
			{
				constants.children = constantsChildren.toArray(new PlatformSimpleUserNode[constantsChildren.size()]);
			}
		}
	}

	private void addSolutionNodeChildren(PlatformSimpleUserNode projectNode)
	{
		ServoyProject servoyProject = (ServoyProject)projectNode.getRealObject();
		Solution solution = servoyProject.getSolution();
		if (solution != null)
		{
			PlatformSimpleUserNode globalsFolder;
			if (solutionOfCalculation == null)
			{
				globalsFolder = new PlatformSimpleUserNode(Messages.TreeStrings_Globals, UserNodeType.GLOBALS_ITEM, solution,
					uiActivator.loadImageFromBundle("globe.gif"));
			}
			else
			{
				globalsFolder = new PlatformSimpleUserNode(Messages.TreeStrings_Globals, UserNodeType.GLOBALS_ITEM_CALCULATION_MODE, solution,
					uiActivator.loadImageFromBundle("globe.gif"));
			}
			addGlobalsNodeChildren(globalsFolder, solution);

			allRelations = null;
			forms = new PlatformSimpleUserNode(Messages.TreeStrings_Forms, UserNodeType.FORMS, solution, uiActivator.loadImageFromBundle("forms.gif"));
			forms.parent = projectNode;
			if (solutionOfCalculation == null)
			{
				allRelations = new PlatformSimpleUserNode(Messages.TreeStrings_Relations, UserNodeType.ALL_RELATIONS, solution,
					uiActivator.loadImageFromOldLocation("relationsoverview.gif"));
				allRelations.parent = projectNode;
			}
			valuelists = new PlatformSimpleUserNode(Messages.TreeStrings_ValueLists, UserNodeType.VALUELISTS, solution,
				uiActivator.loadImageFromBundle("valuelists.gif"));
			media = new PlatformSimpleUserNode(Messages.TreeStrings_Media, UserNodeType.MEDIA, solution, uiActivator.loadImageFromBundle("image.gif"));

			globalsFolder.parent = projectNode;
			valuelists.parent = projectNode;
			media.parent = projectNode;

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
			uiActivator.loadImageFromBundle("global_variabletree.gif"));
		globalVariables.parent = globalsFolder;

		PlatformSimpleUserNode currentForm = new PlatformSimpleUserNode(Messages.TreeStrings_currentcontroller, UserNodeType.CURRENT_FORM, null,
			uiActivator.loadImageFromBundle("formula.gif"));
		if (solutionOfCalculation != null) currentForm.hide();
		currentForm.parent = globalsFolder;

		PlatformSimpleUserNode globalRelations = new PlatformSimpleUserNode(Messages.TreeStrings_relations, UserNodeType.GLOBALRELATIONS, solution,
			uiActivator.loadImageFromOldLocation("relationsoverview.gif"));
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
		Iterator it = solution.getForms(null, true);
		while (it.hasNext())
		{
			Form f = (Form)it.next();
			PlatformSimpleUserNode node = new PlatformSimpleUserNode(f.getName(), UserNodeType.FORM, f.getName(), f.getDataSource() == null
				? "No table" : ("Server: " + f.getServerName() + ", Table: " + f.getTableName()), f, uiActivator.loadImageFromBundle("designer.gif")); //$NON-NLS-1$
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
					ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
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
		if (ancestorForm.getExtendsFormID() != 0)
		{
			Form parentForm = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getForm(ancestorForm.getExtendsFormID());
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
						node = new PlatformSimpleUserNode(groupLabel, UserNodeType.FORM_ELEMENTS_GROUP, new Object[] { new FormElementGroup(
							element.getGroupID(), persist.getParent()), null }, originalForm, uiActivator.loadImageFromBundle("group.gif")); //$NON-NLS-1$
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
					Iterator portalElementIterator = ((Portal)element).getAllObjects();
					while (portalElementIterator.hasNext())
					{
						Object oo = portalElementIterator.next();
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

			Class< ? > beanClass = SwingItemFactory.getPersistClass(Activator.getDefault().getDesignClient(), bean);
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
				node = new PlatformSimpleUserNode(bean.getName() + " (" + className + ")", UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { bean, beanClass },
					bean.getParent(), uiActivator.loadImageFromBundle("element.gif"));
				node.setDeveloperFeedback(new SimpleDeveloperFeedback(bean.getName() + ".", null, null)); //$NON-NLS-1$

				Class< ? > superClass = beanClass.getSuperclass();
				PlatformSimpleUserNode parentClassNode = node;
				PlatformSimpleUserNode currentClassNode;
				while (superClass != null)
				{
					className = superClass.getSimpleName();
					currentClassNode = new PlatformSimpleUserNode(className, UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { bean, superClass },
						bean.getParent(), uiActivator.loadImageFromBundle("element.gif"));
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
					uiActivator.loadImageFromBundle("element.gif"));
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
			ServoyLog.logWarning("Solution explorer cannot create bean " + bean.getName(), e);
			node = new PlatformSimpleUserNode(bean.getName(), UserNodeType.FORM_ELEMENTS_ITEM, new Object[] { bean, null }, bean.getParent(),
				uiActivator.loadImageFromBundle("element.gif"));
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
			Iterator it = solution.getRelations(null, true, true); // returns all global relations
			while (it.hasNext())
			{
				Relation r = (Relation)it.next();
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
							node = (PlatformSimpleUserNode)findChildNode(node, Messages.TreeStrings_Forms);
							if (node != null && node.children != null)
							{
								PlatformSimpleUserNode relationsNode;
								Form form;
								for (int i = node.children.length - 1; i >= 0; i--)
								{
									form = (Form)node.children[i].getRealObject();
									if (form.getTable() != null && form.getTable().equals(((Relation)persist).getPrimaryTable()))
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
								node = (PlatformSimpleUserNode)findChildNode(node, Messages.TreeStrings_Relations);
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

			CalculationModeHandler cm = CalculationModeHandler.getInstance();
			for (int i = invisibleRootNode.children.length - 1; i >= 0; i--)
			{
				SimpleUserNode node = invisibleRootNode.children[i];
				node.unhide();
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
			ArrayList al = new ArrayList();
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
	private boolean findNode(PlatformSimpleUserNode startNode, UUID uuid, List lst)
	{
		Object realObject = startNode.getRealObject();

		if (realObject instanceof IPersist)
		{
			if (((IPersist)realObject).getUUID().equals(uuid))
			{
				lst.add(startNode);
				return true;
			}
		}
		Object[] elements = getElements(startNode);
		if (elements != null)
		{
			for (Object element : elements)
			{
				if (element instanceof PlatformSimpleUserNode)
				{
					if (findNode((PlatformSimpleUserNode)element, uuid, lst))
					{
						return true;
					}
				}
			}
		}
		return false;
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