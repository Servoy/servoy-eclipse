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
package com.servoy.eclipse.core.builder;

import java.awt.Point;
import java.awt.print.PageFormat;
import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.ASTVisitor;
import org.eclipse.dltk.compiler.problem.IProblem;
import org.eclipse.dltk.compiler.problem.IProblemReporter;
import org.eclipse.dltk.javascript.ast.Expression;
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.GetArrayItemExpression;
import org.eclipse.dltk.javascript.ast.Identifier;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.Statement;
import org.eclipse.dltk.javascript.ast.VoidExpression;
import org.eclipse.dltk.javascript.parser.JavaScriptParser;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.ServoyResourcesProject;
import com.servoy.eclipse.core.repository.EclipseRepository;
import com.servoy.eclipse.core.repository.SolutionDeserializer;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.core.util.CoreUtils;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.DBValueList;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractScriptProvider;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportSize;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IVariable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IdentDocumentValidator;
import com.servoy.j2db.util.IntHashMap;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.RoundHalfUpDecimalFormat;
import com.servoy.j2db.util.ScriptingUtils;
import com.servoy.j2db.util.StateFullSimpleDateFormat;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Builds Servoy projects. Adds problem markers where needed.
 */
public class ServoyBuilder extends IncrementalProjectBuilder
{
	public static int MAX_EXCEPTIONS = 25;
	public static int exceptionCount = 0;
	private final ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
	private final JavaScriptParser javascriptParser = new JavaScriptParser();
	private final IProblemReporter dummyReporter = new IProblemReporter()
	{
		public void reportProblem(IProblem problem)
		{
			// do nothing
		}

		public Object getAdapter(Class adapter)
		{
			return null;
		}

	};

	class ServoyDeltaVisitor implements IResourceDeltaVisitor
	{
		public boolean visit(IResourceDelta delta) throws CoreException
		{
			IResource resource = delta.getResource();
			switch (delta.getKind())
			{
				case IResourceDelta.ADDED :
					// handle added resource
					checkResource(resource);
					break;
				case IResourceDelta.REMOVED :
					// handle removed resource
					checkResource(resource);
					break;
				case IResourceDelta.CHANGED :
					// handle changed resource
					checkResource(resource);
					break;
			}
			//return true to continue visiting children.
			return true;
		}
	}

	class ServoyResourceVisitor implements IResourceVisitor
	{
		public boolean visit(IResource resource)
		{
			checkResource(resource);
			//return true to continue visiting children.
			return true;
		}
	}

	class XMLErrorHandler extends DefaultHandler
	{

		private final IFile file;

		public XMLErrorHandler(IFile file)
		{
			this.file = file;
		}

		private void addMarker(SAXParseException e, int severity)
		{
			ServoyBuilder.addMarker(file, XML_MARKER_TYPE, e.getMessage(), e.getLineNumber(), severity, IMarker.PRIORITY_NORMAL, null, null);
		}

		@Override
		public void error(SAXParseException exception) throws SAXException
		{
			addMarker(exception, IMarker.SEVERITY_ERROR);
		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException
		{
			addMarker(exception, IMarker.SEVERITY_ERROR);
		}

		@Override
		public void warning(SAXParseException exception) throws SAXException
		{
			addMarker(exception, IMarker.SEVERITY_WARNING);
		}
	}

	public static final String BUILDER_ID = "com.servoy.eclipse.core.servoyBuilder"; //$NON-NLS-1$
	public static final String SERVOY_BUILDER_MARKER_TYPE = Activator.PLUGIN_ID + ".builderProblem"; //$NON-NLS-1$

	public static final String XML_MARKER_TYPE = Activator.PLUGIN_ID + ".xmlProblem"; //$NON-NLS-1$
	public static final String PROJECT_DESERIALIZE_MARKER_TYPE = Activator.PLUGIN_ID + ".deserializeProblem"; //$NON-NLS-1$
	public static final String SOLUTION_PROBLEM_MARKER_TYPE = Activator.PLUGIN_ID + ".solutionProblem"; //$NON-NLS-1$
	public static final String BAD_STRUCTURE_MARKER_TYPE = Activator.PLUGIN_ID + ".badStructure"; //$NON-NLS-1$
	public static final String MISSING_MODULES_MARKER_TYPE = Activator.PLUGIN_ID + ".missingModulesProblem"; //$NON-NLS-1$
	public static final String MULTIPLE_RESOURCES_PROJECTS_MARKER_TYPE = Activator.PLUGIN_ID + ".multipleResourcesProblem"; //$NON-NLS-1$
	public static final String NO_RESOURCES_PROJECTS_MARKER_TYPE = Activator.PLUGIN_ID + ".noResourcesProblem"; //$NON-NLS-1$
	public static final String DIFFERENT_RESOURCES_PROJECTS_MARKER_TYPE = Activator.PLUGIN_ID + ".differentResourcesProblem"; //$NON-NLS-1$
	public static final String PROJECT_RELATION_MARKER_TYPE = Activator.PLUGIN_ID + ".relationProblem"; //$NON-NLS-1$
	public static final String MEDIA_MARKER_TYPE = Activator.PLUGIN_ID + ".mediaProblem"; //$NON-NLS-1$
	public static final String CALCULATION_MARKER_TYPE = Activator.PLUGIN_ID + ".calculationProblem"; //$NON-NLS-1$
	public static final String SCRIPT_MARKER_TYPE = Activator.PLUGIN_ID + ".scriptProblem"; //$NON-NLS-1$
	public static final String EVENT_METHOD_MARKER_TYPE = Activator.PLUGIN_ID + ".eventProblem"; //$NON-NLS-1$
	public static final String USER_SECURITY_MARKER_TYPE = Activator.PLUGIN_ID + ".userSecurityProblem"; //$NON-NLS-1$
	public static final String DATABASE_INFORMATION_MARKER_TYPE = Activator.PLUGIN_ID + ".databaseInformationProblem"; //$NON-NLS-1$
	public static final String PROJECT_FORM_MARKER_TYPE = Activator.PLUGIN_ID + ".formProblem"; //$NON-NLS-1$
	public static final String INVALID_TABLE_NODE_PROBLEM = Activator.PLUGIN_ID + ".invalidTableNodeProblem"; //$NON-NLS-1$
	public static final String PROJECT_VALUELIST_MARKER_TYPE = Activator.PLUGIN_ID + ".valuelistProblem"; //$NON-NLS-1$
	public static final String DUPLICATE_UUID = Activator.PLUGIN_ID + ".duplicateUUID"; //$NON-NLS-1$
	public static final String DUPLICATE_SIBLING_UUID = Activator.PLUGIN_ID + ".duplicateSiblingUUID"; //$NON-NLS-1$
	public static final String DUPLICATE_NAME_MARKER_TYPE = Activator.PLUGIN_ID + ".duplicateNameProblem"; //$NON-NLS-1$
	public static final String INVALID_SORT_OPTION = Activator.PLUGIN_ID + ".invalidSortOption"; //$NON-NLS-1$
	public static final String PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE = Activator.PLUGIN_ID + ".differentRelationName"; //$NON-NLS-1$
	public static final String MISSING_SERVER = Activator.PLUGIN_ID + ".missingServer"; //$NON-NLS-1$
	public static final String MISSING_STYLE = Activator.PLUGIN_ID + ".missingStyle"; //$NON-NLS-1$
	public static final String I18N_MARKER_TYPE = Activator.PLUGIN_ID + ".i18nProblem"; //$NON-NLS-1$
	public static final String COLUMN_MARKER_TYPE = Activator.PLUGIN_ID + ".columnProblem"; //$NON-NLS-1$
	public static final String INVALID_EVENT_METHOD = Activator.PLUGIN_ID + ".invalidEventMethod";
	public static final String DEPRECATED_PROPERTY_USAGE = Activator.PLUGIN_ID + ".deprecatedPropertyUsage"; //$NON-NLS-1$
	public static final String FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION = Activator.PLUGIN_ID + ".formWithDatasourceInLoginSolution"; //$NON-NLS-1$

	private SAXParserFactory parserFactory;
	private final HashSet<String> referencedProjectsSet = new HashSet<String>();
	private final HashSet<String> moduleProjectsSet = new HashSet<String>();

	private IProgressMonitor monitor;

	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor progressMonitor) throws CoreException
	{
		referencedProjectsSet.clear();
		moduleProjectsSet.clear();

		IProject[] referencedProjects = getProject().getReferencedProjects();
		ServoyProject sp = getServoyProject(getProject());
		ArrayList<IProject> moduleAndModuleReferencedProjects = null;
		if (sp != null)
		{
			// we are interested in showing module error markers only if the project is in use (active prj or active module)
			if (isActiveSolutionOrModule(sp))
			{
				Solution sol = sp.getSolution();
				if (sol != null)
				{
					String moduleNames = sol.getModulesNames();
					if (moduleNames != null)
					{
						StringTokenizer st = new StringTokenizer(moduleNames, ";,"); //$NON-NLS-1$
						String moduleName;
						IProject moduleProject;
						while (st.hasMoreTokens())
						{
							if (moduleAndModuleReferencedProjects == null)
							{
								moduleAndModuleReferencedProjects = new ArrayList<IProject>();
							}
							moduleName = st.nextToken();
							moduleProject = ServoyModel.getWorkspace().getRoot().getProject(moduleName);
							moduleAndModuleReferencedProjects.add(moduleProject);
							moduleProjectsSet.add(moduleName);

							if (moduleProject.exists() && moduleProject.isOpen() && moduleProject.hasNature(ServoyProject.NATURE_ID))
							{
								IProject[] moduleReferencedProjects = moduleProject.getReferencedProjects();
								if (moduleReferencedProjects.length > 0)
								{
									for (IProject mrp : moduleReferencedProjects)
									{
										referencedProjectsSet.add(mrp.getName());
										if (!moduleAndModuleReferencedProjects.contains(mrp))
										{
											moduleAndModuleReferencedProjects.add(mrp);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		IProject[] monitoredProjects;
		if (moduleAndModuleReferencedProjects != null)
		{
			// we now add all the remaining referenced projects to be monitored inside the moduleProjects (in order to create an array out of them)
			for (IProject p : referencedProjects)
			{
				if (!moduleAndModuleReferencedProjects.contains(p))
				{
					moduleAndModuleReferencedProjects.add(p);
				}
				referencedProjectsSet.add(p.getName());
			}

			monitoredProjects = moduleAndModuleReferencedProjects.toArray(new IProject[moduleAndModuleReferencedProjects.size()]);
		}
		else
		{
			for (IProject p : referencedProjects)
			{
				referencedProjectsSet.add(p.getName());
			}
			monitoredProjects = referencedProjects;
		}
		if (kind == FULL_BUILD)
		{
			fullBuild(progressMonitor);
		}
		else
		{
			try
			{
				for (IProject p : monitoredProjects)
				{
					IResourceDelta delta = getDelta(p);
					if (delta != null)
					{
						incrementalBuild(delta, progressMonitor);
					}
				}
				IResourceDelta delta = getDelta(getProject());
				if (delta != null)
				{
					incrementalBuild(delta, progressMonitor);
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
		return monitoredProjects;
	}

	@Override
	protected void clean(IProgressMonitor progressMonitor) throws CoreException
	{
		getProject().deleteMarkers(SERVOY_BUILDER_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
	}

	void checkResource(IResource resource)
	{
		try
		{
			if (resource instanceof IFile && resource.getName().endsWith(".xml")) //$NON-NLS-1$
			{
				checkXML((IFile)resource);
			}
			else if (resource instanceof IProject)
			{
				IProject project = (IProject)resource;
				if (!project.exists() || (project.isOpen() && project.hasNature(ServoyProject.NATURE_ID) && project == getProject()))
				{
					// a project that this builder in interested in was deleted (so a module or the resources proj.)
					// or something has changed in the active solution project
					checkServoyProject(getProject());
					checkModules(getProject());
					checkResourcesForServoyProject(getProject());
					checkResourcesForModules(getProject());
					ServoyProject servoyProject = getServoyProject(project);
					if (isActiveSolutionOrModule(servoyProject))
					{
						checkColumns(project);
					}
				}
				else
				{
					if (project.hasNature(ServoyResourcesProject.NATURE_ID))
					{
						final ServoyProject servoyProject = getServoyProject(getProject());
						if (isActiveSolutionOrModule(servoyProject))
						{
							checkStyles(getProject());
							checkColumns(getProject());
						}
						IProject[] projects = project.getReferencingProjects();
						if (projects != null)
						{
							for (IProject p : projects)
							{
								if (isActiveSolutionOrModule(getServoyProject(p)))
								{
									deleteMarkers(p, PROJECT_RELATION_MARKER_TYPE);
									checkRelations(p, new HashMap<String, IPersist>());
								}
							}
						}
					}
					if (referencedProjectsSet.contains(resource.getName()))
					{
						// a referenced project has changed... check
						checkResourcesForServoyProject(getProject());
						checkResourcesForModules(getProject());
					}
					if (moduleProjectsSet.contains(resource.getName()))
					{
						// a module project has changed (maybe it was deleted/added); check the modules list
						checkModules(getProject());
						checkResourcesForModules(getProject());
					}
				}
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Exception while performing build", e); //$NON-NLS-1$
		}
	}

	private void checkResourcesForModules(IProject project)
	{
		deleteMarkers(project, DIFFERENT_RESOURCES_PROJECTS_MARKER_TYPE);

		// check this solution project and it's modules to see that they use the same resources project
		ServoyProject servoyProject = getServoyProject(project);
		boolean active = isActiveSolutionOrModule(servoyProject);

		if (servoyProject != null)
		{
			ServoyResourcesProject resourcesProject = servoyProject.getResourcesProject();
			if (active && servoyProject.getSolution() != null && resourcesProject != null)
			{
				// check if all modules are checked out
				String modulesNames = servoyProject.getSolution().getModulesNames();
				ServoyResourcesProject moduleResourcesProject;
				if (modulesNames != null)
				{
					StringTokenizer st = new StringTokenizer(modulesNames, ";,"); //$NON-NLS-1$
					while (st.hasMoreTokens())
					{
						String name = st.nextToken().trim();
						ServoyProject module = servoyModel.getServoyProject(name);
						if (module != null)
						{
							moduleResourcesProject = module.getResourcesProject();
							if (moduleResourcesProject != null && (!moduleResourcesProject.equals(resourcesProject)))
							{
								// this module has a resources project different than the one of the main solution
								String message = MarkerMessages.getMessage(MarkerMessages.Marker_Resources_ModuleDifferentResourceProject, name,
									project.getName());
								addMarker(project, DIFFERENT_RESOURCES_PROJECTS_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL,
									null, null);
							}
						}
					}
				}
			}
		}
	}

	private void checkModules(IProject project)
	{
		deleteMarkers(project, MISSING_MODULES_MARKER_TYPE);

		final ServoyProject servoyProject = getServoyProject(project);
		boolean active = isActiveSolutionOrModule(servoyProject);

		if (servoyProject != null && active && servoyProject.getSolution() != null)
		{
			// check if all modules are checked out
			String[] modulesNames = CoreUtils.getTokenElements(servoyProject.getSolution().getModulesNames(), ",", true); //$NON-NLS-1$
			if (modulesNames != null)
			{
				for (String name : modulesNames)
				{
					ServoyProject module = servoyModel.getServoyProject(name);
					if (module == null)
					{
						String message = MarkerMessages.getMessage(MarkerMessages.Marker_Module_ModuleNotFound, name, servoyProject.getSolution().getName());
						IMarker marker = addMarker(project, MISSING_MODULES_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH, null, null);
						try
						{
							marker.setAttribute("moduleName", name); //$NON-NLS-1$
							marker.setAttribute("solutionName", servoyProject.getSolution().getName()); //$NON-NLS-1$
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
				}
			}
		}
	}

	private boolean isActiveSolutionOrModule(ServoyProject servoyProject)
	{
		boolean found = false;
		ServoyProject activeProject = servoyModel.getActiveProject();
		if (activeProject != null)
		{
			ArrayList<String> projectNames = new ArrayList<String>();
			projectNames.add(activeProject.getSolution().getName());
			if (activeProject.equals(servoyProject)) found = true;
			if (!found)
			{
				ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
				if (modules != null)
				{
					for (ServoyProject module : modules)
					{
						if (module.equals(servoyProject))
						{
							found = true;
							if (module.getSolution() != null) projectNames.add(module.getSolution().getName());
						}
					}
				}
			}
		}
		return found;
	}

	private static final Integer METHOD_DUPLICATION = new Integer(1);
	private static final Integer FORM_DUPLICATION = new Integer(2);
	private static final Integer RELATION_DUPLICATION = new Integer(3);

	private void addDuplicatePersist(IPersist persist, Map<String, Map<Integer, Set<ISupportChilds>>> duplicationMap, IProject project)
	{
		if (persist instanceof IScriptProvider || persist instanceof ScriptVariable)
		{
			String name = ((ISupportName)persist).getName();
			if (name != null)
			{
				ArrayList<ISupportChilds> duplicatedParents = new ArrayList<ISupportChilds>(3);
				Map<Integer, Set<ISupportChilds>> persistSet = duplicationMap.get(name);
				if (persistSet == null)
				{
					persistSet = new HashMap<Integer, Set<ISupportChilds>>();
					duplicationMap.put(name, persistSet);
				}
				Set<ISupportChilds> parentSet = persistSet.get(METHOD_DUPLICATION);
				if (parentSet != null && parentSet.contains(persist.getParent()))
				{
					duplicatedParents.add(persist.getParent());
				}
				else if (parentSet != null && persist.getParent() instanceof Solution)
				{
					for (ISupportChilds supportChilds : parentSet)
					{
						if (supportChilds instanceof Solution)
						{
							duplicatedParents.add(supportChilds);
						}
					}
				}
				for (ISupportChilds duplicatedParent : duplicatedParents)
				{
					String duplicateParentsName = ""; //$NON-NLS-1$
					if (duplicatedParent instanceof ISupportName)
					{
						duplicateParentsName = ((ISupportName)duplicatedParent).getName();
					}
					String parentsName = ""; //$NON-NLS-1$
					if (persist.getParent() instanceof ISupportName)
					{
						parentsName = ((ISupportName)persist.getParent()).getName();
					}
					String type = "method"; //$NON-NLS-1$
					if (persist instanceof ScriptVariable)
					{
						type = "variable"; //$NON-NLS-1$
					}
					int severity = IMarker.SEVERITY_ERROR;
					String otherChildsType = "method"; //$NON-NLS-1$
					Iterator<IPersist> allObjects = duplicatedParent.getAllObjects();
					while (allObjects.hasNext())
					{
						IPersist child = allObjects.next();
						if ((child instanceof IScriptProvider || child instanceof ScriptVariable) && ((ISupportName)child).getName().equals(name))
						{
							Integer lineNumber = Integer.valueOf(0);
							if (child instanceof ScriptVariable)
							{
								otherChildsType = "variable"; //$NON-NLS-1$
								lineNumber = ((AbstractBase)child).getSerializableRuntimeProperty(IScriptProvider.LINENUMBER);
							}
							else if (child instanceof AbstractScriptProvider)
							{
								lineNumber = Integer.valueOf(((AbstractScriptProvider)child).getLineNumberOffset());
							}
							if (persist instanceof ScriptVariable && otherChildsType.equals("variable") && !duplicateParentsName.equals(parentsName)) //$NON-NLS-1$
							{
								severity = IMarker.SEVERITY_WARNING;
							}
							String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Duplicate_DuplicateEntityFound, type, name, parentsName);
							addMarker(project, DUPLICATE_NAME_MARKER_TYPE, msg, lineNumber == null ? -1 : lineNumber.intValue(), severity,
								IMarker.PRIORITY_NORMAL, null, child);
							break;
						}
					}
					Integer lineNumber = Integer.valueOf(0);
					if (persist instanceof ScriptVariable)
					{
						otherChildsType = "variable"; //$NON-NLS-1$
						lineNumber = ((AbstractBase)persist).getSerializableRuntimeProperty(IScriptProvider.LINENUMBER);
					}
					else if (persist instanceof AbstractScriptProvider)
					{
						lineNumber = new Integer(((AbstractScriptProvider)persist).getLineNumberOffset());
					}
					String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Duplicate_DuplicateEntityFound, otherChildsType, name, duplicateParentsName);
					addMarker(project, DUPLICATE_NAME_MARKER_TYPE, msg, lineNumber == null ? -1 : lineNumber.intValue(), severity, IMarker.PRIORITY_NORMAL,
						null, persist);

				}
				Set<ISupportChilds> parents = parentSet;
				if (parents == null)
				{
					parents = new HashSet<ISupportChilds>();
					persistSet.put(METHOD_DUPLICATION, parents);
				}
				parents.add(persist.getParent());
			}
		}
		if (persist instanceof Form)
		{
			String name = ((ISupportName)persist).getName();
			if (name != null)
			{
				Map<Integer, Set<ISupportChilds>> persistSet = duplicationMap.get(name);
				if (persistSet == null)
				{
					persistSet = new HashMap<Integer, Set<ISupportChilds>>();
					duplicationMap.put(name, persistSet);
				}
				Set<ISupportChilds> parentSet = persistSet.get(FORM_DUPLICATION);
				if (parentSet != null)
				{
					String parentsName = ""; //$NON-NLS-1$
					if (persist.getParent() instanceof ISupportName)
					{
						parentsName = ((ISupportName)persist.getParent()).getName();
					}
					for (ISupportChilds parent : parentSet)
					{
						if (parent instanceof Solution)
						{
							Solution solution = (Solution)parent;
							Form duplicateForm = solution.getForm(name);
							if (duplicateForm != null)
							{
								String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Duplicate_DuplicateEntityFound, "form", name, parentsName); //$NON-NLS-1$
								addMarker(project, DUPLICATE_NAME_MARKER_TYPE, msg, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, duplicateForm);
							}
							String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Duplicate_DuplicateEntityFound, "form", name, solution.getName()); //$NON-NLS-1$
							addMarker(project, DUPLICATE_NAME_MARKER_TYPE, msg, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, persist);
						}
					}
				}
				Set<ISupportChilds> parents = parentSet;
				if (parents == null)
				{
					parents = new HashSet<ISupportChilds>();
					persistSet.put(FORM_DUPLICATION, parents);
				}
				parents.add(persist.getParent());
			}
		}
		if (persist instanceof Relation)
		{
			String name = ((ISupportName)persist).getName();
			if (name != null)
			{
				Map<Integer, Set<ISupportChilds>> persistSet = duplicationMap.get(name);
				if (persistSet == null)
				{
					persistSet = new HashMap<Integer, Set<ISupportChilds>>();
					duplicationMap.put(name, persistSet);
				}
				Set<ISupportChilds> parentSet = persistSet.get(RELATION_DUPLICATION);
				if (parentSet != null)
				{
					String parentsName = ""; //$NON-NLS-1$
					if (persist.getParent() instanceof ISupportName)
					{
						parentsName = ((ISupportName)persist.getParent()).getName();
					}
					for (ISupportChilds parent : parentSet)
					{
						if (parent instanceof Solution)
						{
							Solution solution = (Solution)parent;
							Relation duplicateRelation = solution.getRelation(name);
							if (!((Relation)persist).contentEquals(duplicateRelation))
							{
								if (duplicateRelation != null)
								{
									String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Duplicate_DuplicateEntityFound, "relation", name, parentsName); //$NON-NLS-1$
									addMarker(project, DUPLICATE_NAME_MARKER_TYPE, msg, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null,
										duplicateRelation);
								}
								String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Duplicate_DuplicateEntityFound,
									"relation", name, solution.getName()); //$NON-NLS-1$								
								addMarker(project, DUPLICATE_NAME_MARKER_TYPE, msg, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, persist);
							}
						}
					}
				}
				Set<ISupportChilds> parents = parentSet;
				if (parents == null)
				{
					parents = new HashSet<ISupportChilds>();
					persistSet.put(RELATION_DUPLICATION, parents);
				}
				parents.add(persist.getParent());
			}
		}
	}

	private void checkPersistDuplication()
	{
		// this is a special case
		ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
		final Map<String, Map<Integer, Set<ISupportChilds>>> duplicationMap = new HashMap<String, Map<Integer, Set<ISupportChilds>>>();
		if (modules != null)
		{
			for (ServoyProject module : modules)
			{
				deleteMarkers(module.getProject(), DUPLICATE_NAME_MARKER_TYPE);
			}
			for (final ServoyProject module : modules)
			{
				module.getSolution().acceptVisitor(new IPersistVisitor()
				{
					public Object visit(IPersist o)
					{
						addDuplicatePersist(o, duplicationMap, module.getProject());
						return CONTINUE_TRAVERSAL;
					}

				});
			}
		}
	}

	private void checkDeprecatedPropertyUsage(IPersist persist, IProject project)
	{
		if (persist instanceof Solution)
		{
			Solution solution = (Solution)persist;

			// loginForm is deprecated, use loginSolution (not needed for WebClient)
			if (solution.getLoginFormID() > 0)
			{
				try
				{
					if (solution.getLoginSolutionName() != null)
					{
						// login form will be ignored
						addDeprecatedPropertyUsageMarker(persist, project, "loginFormID", "Solution '" + solution.getName() +
							"' has a loginForm property set which is overridden by the loginSolutionName property.");
					}
					else if (solution.getSolutionType() != SolutionMetaData.WEB_CLIENT_ONLY)
					{
						// loginForm is deprecated
						addDeprecatedPropertyUsageMarker(
							persist,
							project,
							"loginFormID", "Solution '" + solution.getName() + "' has a loginForm property set which is deprecated, use loginSolutionName property instead."); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}
	}

	private void addDeprecatedPropertyUsageMarker(IPersist persist, IProject project, String propertName, String message) throws CoreException
	{
		if (message != null)
		{
			IMarker marker = addMarker(project, DEPRECATED_PROPERTY_USAGE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, persist);
			if (marker != null)
			{
				marker.setAttribute("Uuid", persist.getUUID().toString()); //$NON-NLS-1$
				marker.setAttribute("SolutionName", project.getName()); //$NON-NLS-1$
				marker.setAttribute("PropertyName", propertName); //$NON-NLS-1$
				marker.setAttribute("DisplayName", RepositoryHelper.getDisplayName(propertName, persist.getClass())); //$NON-NLS-1$
			}
		}
	}

	private void addMissingServer(IPersist persist, Map<String, IPersist> missingServers, List<String> goodServers)
	{
		String serverName = null;
		if (persist instanceof Form)
		{
			serverName = ((Form)persist).getServerName();
		}
		else if (persist instanceof ValueList)
		{
			serverName = ((ValueList)persist).getServerName();
		}
		else if (persist instanceof TableNode)
		{
			serverName = ((TableNode)persist).getServerName();
		}
		else if (persist instanceof Relation)
		{
			serverName = ((Relation)persist).getPrimaryServerName();
			String foreignServer = ((Relation)persist).getForeignServerName();
			if (foreignServer != null && !missingServers.containsKey(foreignServer) && !goodServers.contains(foreignServer))
			{
				IServer server = ServoyModel.getServerManager().getServer(foreignServer);
				if (server != null) goodServers.add(foreignServer);
				else missingServers.put(foreignServer, persist);
			}
		}
		if (serverName != null && !missingServers.containsKey(serverName) && !goodServers.contains(serverName))
		{
			IServer server = ServoyModel.getServerManager().getServer(serverName);
			if (server != null) goodServers.add(serverName);
			else missingServers.put(serverName, persist);
		}
	}

	private void checkDuplicateUUID(IPersist persist, IProject project)
	{
		boolean found = false;

		if (Utils.getAsBoolean(((AbstractBase)persist).getRuntimeProperty(SolutionDeserializer.POSSIBLE_DUPLICATE_UUID)))
		{
			UUID uuid = persist.getUUID();
			Pair<String, String> pathPair = SolutionSerializer.getFilePath(persist, true);
			IPath path = new Path(pathPair.getLeft());
			String location = null;
			if (path.segmentCount() == 1)
			{
				IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(pathPair.getLeft());
				location = p.getLocation().toOSString();
			}
			else
			{
				IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
				location = folder.getLocation().toOSString();

			}
			java.io.File file = new File(location);
			File[] files = file.listFiles(new FileFilter()
			{
				public boolean accept(File pathname)
				{
					return SolutionSerializer.isJSONFile(pathname.getName()) && pathname.isFile();
				}
			});
			String persistFile = ((AbstractBase)persist).getSerializableRuntimeProperty(IScriptProvider.FILENAME);
			if (files != null)
			{
				for (File f : files)
				{
					UUID newUUID = SolutionDeserializer.getUUID(f);
					if (newUUID != null && newUUID.equals(uuid) && !pathPair.getRight().equals(f.getName()))
					{
						found = true;
						IFile fileForLocation = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(
							Path.fromPortableString(persistFile.replace('\\', '/')));
						String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Duplicate_UUIDDuplicate, persist.getUUID());
						addMarker(project, DUPLICATE_SIBLING_UUID, msg, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH, fileForLocation.toString(), persist);
						break; // only 1 marker has to be set for this persist.
					}
				}
			}
		}
		if (!found)
		{
			((AbstractBase)persist).setRuntimeProperty(SolutionDeserializer.POSSIBLE_DUPLICATE_UUID, null);
		}
	}

	private void checkServoyProject(final IProject project)
	{
		// only log exceptions to max count
		exceptionCount = 0;
		deleteMarkers(project, PROJECT_DESERIALIZE_MARKER_TYPE);
		deleteMarkers(project, SOLUTION_PROBLEM_MARKER_TYPE);
		deleteMarkers(project, PROJECT_RELATION_MARKER_TYPE);
		deleteMarkers(project, MEDIA_MARKER_TYPE);
		deleteMarkers(project, CALCULATION_MARKER_TYPE);
		deleteMarkers(project, PROJECT_FORM_MARKER_TYPE);
		deleteMarkers(project, INVALID_TABLE_NODE_PROBLEM);
		deleteMarkers(project, PROJECT_VALUELIST_MARKER_TYPE);
		deleteMarkers(project, DUPLICATE_UUID);
		deleteMarkers(project, DUPLICATE_SIBLING_UUID);
		deleteMarkers(project, DUPLICATE_NAME_MARKER_TYPE);
		deleteMarkers(project, MISSING_SERVER);
		deleteMarkers(project, BAD_STRUCTURE_MARKER_TYPE);
		deleteMarkers(project, MISSING_STYLE);
		deleteMarkers(project, SCRIPT_MARKER_TYPE);
		deleteMarkers(project, EVENT_METHOD_MARKER_TYPE);
		deleteMarkers(project, I18N_MARKER_TYPE);
		deleteMarkers(project, INVALID_SORT_OPTION);
		deleteMarkers(project, PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE);
		deleteMarkers(project, INVALID_EVENT_METHOD);
		deleteMarkers(project, DEPRECATED_PROPERTY_USAGE);

		final ServoyProject servoyProject = getServoyProject(project);
		boolean active = isActiveSolutionOrModule(servoyProject);

		final Map<String, IPersist> missingServers = new HashMap<String, IPersist>();
		final List<String> goodServers = new ArrayList<String>();
		if (servoyProject != null)
		{
			if (active && servoyProject.getSolution() != null)
			{
				addDeserializeProblemMarkers(servoyProject);
				checkPersistDuplication();
				servoyProject.getSolution().acceptVisitor(new IPersistVisitor()
				{
					private final ServoyProject[] modules = getSolutionModules(servoyProject);
					private final FlattenedSolution flattenedSolution = servoyModel.getFlattenedSolution();
					private final Solution solution = servoyProject.getSolution();
					private IntHashMap<IPersist> elementIdPersistMap = null;
					private final Map<UUID, List<IPersist>> theMakeSureNoDuplicateUUIDsAreFound = new HashMap<UUID, List<IPersist>>();
					private final Map<Form, Boolean> formsAbstractChecked = new HashMap<Form, Boolean>();
					private final Set<UUID> methodsParsed = new HashSet<UUID>();

					public Object visit(IPersist o)
					{
						checkCancel();

						Form parentForm = (Form)o.getAncestor(IRepository.FORMS);
						try
						{
							final Map<String, Method> methods = ((EclipseRepository)ServoyModel.getDeveloperRepository()).getGettersViaIntrospection(o);
							Iterator<ContentSpec.Element> iterator = ((EclipseRepository)ServoyModel.getDeveloperRepository()).getContentSpec().getPropertiesForObjectType(
								o.getTypeID());
							while (iterator.hasNext())
							{
								final ContentSpec.Element element = iterator.next();
								// Don't set meta data properties.
								if (element.isMetaData() || element.isDeprecated()) continue;
								// Get default property value as an object.
								final int typeId = element.getTypeID();

								if (typeId == IRepository.ELEMENTS)
								{
									final Method method = methods.get(element.getName());
									Object property_value = method.invoke(o, new Object[] { });
									final int element_id = Utils.getAsInteger(property_value);
									if (element_id > 0)
									{
										if (elementIdPersistMap == null)
										{
											missingServers.clear();
											goodServers.clear();
											elementIdPersistMap = new IntHashMap<IPersist>();
											solution.acceptVisitor(new IPersistVisitor()
											{
												public Object visit(IPersist p)
												{
													elementIdPersistMap.put(p.getID(), p);
													List<IPersist> lst = theMakeSureNoDuplicateUUIDsAreFound.get(p.getUUID());
													if (lst == null)
													{
														lst = new ArrayList<IPersist>(3);
														lst.add(p);
														theMakeSureNoDuplicateUUIDsAreFound.put(p.getUUID(), lst);

														if (((AbstractBase)p).getRuntimeProperty(SolutionDeserializer.POSSIBLE_DUPLICATE_UUID) != null)
														{
															checkDuplicateUUID(p, project);
														}
													}
													else
													{
														IPersist other = lst.get(0);

														// for now only add it on both if there is 1, just skip the rest.
														if (lst.size() == 1)
														{
															String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Duplicate_UUIDDuplicateIn,
																other.getUUID(),
																SolutionSerializer.getRelativePath(p, false) + SolutionSerializer.getFileName(p, false));
															addMarker(project, DUPLICATE_UUID, msg, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH, null,
																other);
														}
														String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Duplicate_UUIDDuplicateIn, p.getUUID(),
															SolutionSerializer.getRelativePath(other, false) + SolutionSerializer.getFileName(other, false));
														addMarker(project, DUPLICATE_UUID, msg, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH, null, p);
													}
													return IPersistVisitor.CONTINUE_TRAVERSAL;
												}
											});
											if (modules != null)
											{
												for (ServoyProject module : modules)
												{
													if (module.getSolution() != null && !module.equals(servoyProject))
													{
														final IProject moduleProject = ResourcesPlugin.getWorkspace().getRoot().getProject(
															module.getSolution().getName());
														deleteMarkers(moduleProject, DUPLICATE_UUID);
														deleteMarkers(moduleProject, DUPLICATE_SIBLING_UUID);

														module.getSolution().acceptVisitor(new IPersistVisitor()
														{
															public Object visit(IPersist p)
															{
																elementIdPersistMap.put(p.getID(), p);

																List<IPersist> lst = theMakeSureNoDuplicateUUIDsAreFound.get(p.getUUID());
																if (lst == null)
																{
																	lst = new ArrayList<IPersist>(3);
																	lst.add(p);
																	theMakeSureNoDuplicateUUIDsAreFound.put(p.getUUID(), lst);
																	if (((AbstractBase)p).getRuntimeProperty(SolutionDeserializer.POSSIBLE_DUPLICATE_UUID) != null)
																	{
																		checkDuplicateUUID(p, moduleProject);
																	}
																}
																else
																{
																	IPersist other = lst.get(0);

																	// for now only add it on both if there is 1, just skip the rest.
																	if (lst.size() == 1)
																	{
																		String msg = MarkerMessages.getMessage(
																			MarkerMessages.Marker_Duplicate_UUIDDuplicateIn,
																			other.getUUID(),
																			SolutionSerializer.getRelativePath(p, false) +
																				SolutionSerializer.getFileName(p, false));
																		addMarker(moduleProject, DUPLICATE_UUID, msg, -1, IMarker.SEVERITY_ERROR,
																			IMarker.PRIORITY_HIGH, null, other);
																	}
																	String msg = MarkerMessages.getMessage(
																		MarkerMessages.Marker_Duplicate_UUIDDuplicateIn,
																		p.getUUID(),
																		SolutionSerializer.getRelativePath(other, false) +
																			SolutionSerializer.getFileName(other, false));
																	addMarker(moduleProject, DUPLICATE_UUID, msg, -1, IMarker.SEVERITY_ERROR,
																		IMarker.PRIORITY_HIGH, null, p);
																}
																return IPersistVisitor.CONTINUE_TRAVERSAL;
															}
														});
													}
												}
											}
										}
										final IPersist foundPersist = elementIdPersistMap.get(element_id);
										if (foundPersist == null)
										{
											String elementName = null;
											String inForm = null;
											if (o instanceof ISupportName && !(o instanceof Form) && (((ISupportName)o).getName() != null)) elementName = ((ISupportName)o).getName();
											if (parentForm != null) inForm = parentForm.getName();
											String msg;
											if (elementName == null)
											{
												if (inForm == null) msg = MarkerMessages.getMessage(MarkerMessages.Marker_Form_Solution_PropertyTargetNotFound,
													element.getName());
												else msg = MarkerMessages.getMessage(MarkerMessages.Marker_Form_Solution_PropertyInFormTargetNotFound,
													element.getName(), inForm);
											}
											else
											{
												if (inForm == null) msg = MarkerMessages.getMessage(
													MarkerMessages.Marker_Form_Solution_PropertyOnElementTargetNotFound, element.getName(), elementName);
												else msg = MarkerMessages.getMessage(MarkerMessages.Marker_Form_Solution_PropertyOnElementInFormTargetNotFound,
													element.getName(), elementName, inForm);
											}
											if (BaseComponent.isEventProperty(element.getName()))
											{
												IMarker marker = addMarker(project, INVALID_EVENT_METHOD, msg, -1, IMarker.SEVERITY_WARNING,
													IMarker.PRIORITY_LOW, null, o);
												marker.setAttribute("EventName", element.getName()); //$NON-NLS-1$
											}
											else if (parentForm != null)
											{
												addMarker(project, PROJECT_FORM_MARKER_TYPE, msg, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
											}
											else
											{
												addMarker(project, SOLUTION_PROBLEM_MARKER_TYPE, msg, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null,
													o);
											}

										}
										else if (foundPersist instanceof ScriptMethod && parentForm != null)
										{
											Form methodForm = (Form)foundPersist.getAncestor(IRepository.FORMS);
											if (methodForm != null && !flattenedSolution.getFormHierarchy(parentForm).contains(methodForm))
											{
												String message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_PropertyMethodNotAccessible,
													element.getName(), parentForm.getName(), methodForm.getName());
												if (o instanceof ISupportName && !(o instanceof Form) && (((ISupportName)o).getName() != null))
												{
													message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_PropertyOnElementMethodNotAccessible,
														element.getName(), ((ISupportName)o).getName(), parentForm.getName(), methodForm.getName());
												}
												addMarker(project, PROJECT_FORM_MARKER_TYPE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null,
													o);
											}
										}
										else if (foundPersist instanceof Form && !"extendsFormID".equals(element.getName()) && //$NON-NLS-1$
											!formCanBeInstantiated(((Form)foundPersist), flattenedSolution, formsAbstractChecked))
										{
											String msg = MarkerMessages.getMessage(MarkerMessages.Marker_PropertyFormCannotBeInstantiated, element.getName());
											addMarker(project, PROJECT_FORM_MARKER_TYPE, msg, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
										}
										if (BaseComponent.isEventProperty(element.getName()) && !skipEventMethod(element.getName()) &&
											(foundPersist instanceof ScriptMethod) && !methodsParsed.contains(foundPersist.getUUID()))
										{
											methodsParsed.add(foundPersist.getUUID());
											parseEventMethod(project, (ScriptMethod)foundPersist, element.getName());
										}
									}
								}
							}
						}
						catch (Exception e)
						{
							throw new RuntimeException(e);
						}
						checkCancel();
						addMissingServer(o, missingServers, goodServers);
						checkCancel();
						if (o instanceof ValueList && !missingServers.containsKey(((ValueList)o).getServerName()))
						{
							ValueList vl = (ValueList)o;
							addMarkers(project, checkValuelist(vl, flattenedSolution, ServoyModel.getServerManager(), false), vl);
						}
						checkCancel();
						if (o instanceof Media)
						{
							Media oMedia = (Media)o;
							if (oMedia.getName().toLowerCase().endsWith(".tiff") || oMedia.getName().toLowerCase().endsWith(".tif")) //$NON-NLS-1$ //$NON-NLS-2$
							{
								Pair<String, String> path = SolutionSerializer.getFilePath(oMedia, false);
								String message = MarkerMessages.getMessage(MarkerMessages.Marker_Media_TIFF, path.getLeft() + path.getRight());
								addMarker(project, MEDIA_MARKER_TYPE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, oMedia);
							}
						}
						checkCancel();
						if (o instanceof ISupportDataProviderID)
						{
							try
							{
								String id = ((ISupportDataProviderID)o).getDataProviderID();
								if (id != null && !"".equals(id)) //$NON-NLS-1$
								{
									if (parentForm == null)
									{
										ServoyLog.logError("Could not find parent form for element " + o, null); //$NON-NLS-1$
									}
									else
									{
										if (!missingServers.containsKey(parentForm.getServerName()))
										{
											IDataProvider dataProvider = flattenedSolution.getDataProviderForTable(parentForm.getTable(), id);
											if (dataProvider == null)
											{
												Form flattenedForm = flattenedSolution.getFlattenedForm(o);
												if (flattenedForm != null)
												{
													dataProvider = flattenedForm.getScriptVariable(id);
												}
											}
											if (dataProvider == null)
											{
												try
												{
													dataProvider = flattenedSolution.getGlobalDataProvider(id);
												}
												catch (Exception e)
												{
													exceptionCount++;
													if (exceptionCount < MAX_EXCEPTIONS) ServoyLog.logError(e);
												}
											}

											String elementName = null;
											String inForm = null;
											if (o instanceof ISupportName && ((ISupportName)o).getName() != null)
											{
												elementName = ((ISupportName)o).getName();
											}
											inForm = parentForm.getName();

											if (o instanceof Field && dataProvider != null)
											{
												Field field = (Field)o;
												if (field.getFormat() != null &&
													field.getFormat().length() > 0 &&
													(field.getDisplayType() == Field.TEXT_FIELD || field.getDisplayType() == Field.TYPE_AHEAD || field.getDisplayType() == Field.CALENDAR))
												{
													String displayFormat = field.getFormat();
													if (!displayFormat.startsWith("i18n:") && !displayFormat.equals("|U") && !displayFormat.equals("|L") &&
														!displayFormat.equals("|#") && !displayFormat.equals("converter"))
													{
														String editFormat = field.getFormat();
														int index = field.getFormat().indexOf("|"); //$NON-NLS-1$
														if (index != -1)
														{
															displayFormat = field.getFormat().substring(0, index);
															editFormat = field.getFormat().substring(index + 1);
														}
														else editFormat = null;
														if ("raw".equals(editFormat))
														{
															editFormat = null;
														}
														int dataType = dataProvider.getDataProviderType();
														try
														{
															if (dataType == IColumnTypes.DATETIME)
															{
																SimpleDateFormat dateFormat = new StateFullSimpleDateFormat(displayFormat, false);
																if (editFormat != null) dateFormat = new StateFullSimpleDateFormat(editFormat, false);
															}
															else if (dataType == IColumnTypes.INTEGER || dataType == IColumnTypes.NUMBER)
															{
																DecimalFormat decimalFormat = new RoundHalfUpDecimalFormat(displayFormat, Locale.getDefault());
																if (editFormat != null) decimalFormat = new RoundHalfUpDecimalFormat(editFormat,
																	Locale.getDefault());
															}
														}
														catch (Exception ex)
														{
															Debug.trace(ex);
															String msg;
															if (elementName == null)
															{
																msg = MarkerMessages.getMessage(MarkerMessages.Marker_Form_FormatInvalid, inForm,
																	field.getFormat());
															}
															else
															{
																msg = MarkerMessages.getMessage(MarkerMessages.Marker_Form_FormatOnElementInvalid, elementName,
																	inForm, field.getFormat());
															}
															addMarker(project, PROJECT_FORM_MARKER_TYPE, msg, -1, IMarker.SEVERITY_WARNING,
																IMarker.PRIORITY_NORMAL, null, o);
														}
													}
												}
											}

											if (dataProvider == null)
											{
												String msg;
												if (elementName == null)
												{
													msg = MarkerMessages.getMessage(MarkerMessages.Marker_Form_DataproviderNotFound, inForm, id);
												}
												else
												{
													msg = MarkerMessages.getMessage(MarkerMessages.Marker_Form_DataproviderOnElementNotFound, elementName,
														inForm, id);
												}
												addMarker(project, PROJECT_FORM_MARKER_TYPE, msg, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
											}
											if (parentForm.getDataSource() != null && dataProvider instanceof ColumnWrapper)
											{
												Relation[] relations = ((ColumnWrapper)dataProvider).getRelations();
												if (relations != null && !relations[0].isGlobal() &&
													!parentForm.getDataSource().equals(relations[0].getPrimaryDataSource()))
												{
													String msg;
													if (elementName == null)
													{
														msg = MarkerMessages.getMessage(MarkerMessages.Marker_Form_DataproviderNotBasedOnFormTable, inForm, id);
													}
													else
													{
														msg = MarkerMessages.getMessage(MarkerMessages.Marker_Form_DataproviderOnElementNotBasedOnFormTable,
															elementName, inForm, id);
													}
													addMarker(project, PROJECT_FORM_MARKER_TYPE, msg, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null,
														o);
												}
											}
											if (dataProvider instanceof AggregateVariable && o instanceof Field && ((Field)o).getEditable())
											{
												String msg;
												if (elementName == null)
												{
													msg = MarkerMessages.getMessage(MarkerMessages.Marker_Form_DataproviderAggregateNotEditable, inForm, id);
												}
												else
												{
													msg = MarkerMessages.getMessage(MarkerMessages.Marker_Form_DataproviderOnElementAggregateNotEditable,
														elementName, inForm, id);
												}
												addMarker(project, PROJECT_FORM_MARKER_TYPE, msg, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
											}
										}
									}
								}
							}
							catch (Exception e)
							{
								exceptionCount++;
								if (exceptionCount < MAX_EXCEPTIONS) ServoyLog.logError(e);
							}
						}
						checkCancel();
						if (o instanceof ISupportBounds && o instanceof ISupportSize)
						{
							// check if not outside form
							Form form = (Form)o.getAncestor(IRepository.FORMS);
							if (form != null)
							{
								Point location = ((ISupportBounds)o).getLocation();
								if (location != null)
								{
									Iterator<com.servoy.j2db.persistence.Part> parts = form.getParts();
									while (parts.hasNext())
									{
										com.servoy.j2db.persistence.Part part = parts.next();
										int startPos = form.getPartStartYPos(part.getID());
										int endPos = part.getHeight();
										if (startPos <= location.y && endPos > location.y)
										{
											// found the part
											int height = ((ISupportSize)o).getSize().height;
											if (location.y + height > endPos)
											{
												String elementName = null;
												String inForm = null;
												String partName = com.servoy.j2db.persistence.Part.getDisplayName(part.getPartType());
												if (o instanceof ISupportName && ((ISupportName)o).getName() != null) elementName = ((ISupportName)o).getName();
												inForm = form.getName();
												String msg;
												if (elementName == null)
												{
													msg = MarkerMessages.getMessage(MarkerMessages.Marker_Form_UnnamedElementOutsideBoundsOfPart, inForm,
														partName);
												}
												else
												{
													msg = MarkerMessages.getMessage(MarkerMessages.Marker_Form_NamedElementOutsideBoundsOfPart, elementName,
														inForm, partName);
												}
												addMarker(project, PROJECT_FORM_MARKER_TYPE, msg, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
											}
											int width = form.getWidth();
											if (part.getPartType() == Part.TITLE_HEADER || part.getPartType() == Part.HEADER ||
												part.getPartType() == Part.FOOTER || part.getPartType() == Part.TITLE_FOOTER)
											{
												String defaultPageFormat = form.getDefaultPageFormat();
												PageFormat currentPageFormat = null;
												try
												{
													currentPageFormat = PersistHelper.createPageFormat(defaultPageFormat);
													if (currentPageFormat == null)
													{
														defaultPageFormat = ServoyModel.getSettings().getProperty("pageformat");
														currentPageFormat = PersistHelper.createPageFormat(defaultPageFormat);
													}
													if (currentPageFormat == null)
													{
														currentPageFormat = new PageFormat();
													}
												}
												catch (NoSuchElementException e)
												{
													ServoyLog.logWarning("Could not parse page format '" + defaultPageFormat + '\'', null);
												}
												if (currentPageFormat != null)
												{
													int w = (int)(currentPageFormat.getImageableWidth() * (form.getPaperPrintScale() / 100d));
													if (width < w)
													{
														width = w;
													}
												}
											}
											if (width < location.x + ((ISupportSize)o).getSize().width)
											{
												String elementName = null;
												String inForm = null;
												if (o instanceof ISupportName && ((ISupportName)o).getName() != null) elementName = ((ISupportName)o).getName();
												if (parentForm != null) inForm = form.getName();
												String msg;
												if (elementName == null)
												{
													msg = MarkerMessages.getMessage(MarkerMessages.Marker_Form_UnnamedElementOutsideBoundsOfForm, inForm);
												}
												else
												{
													msg = MarkerMessages.getMessage(MarkerMessages.Marker_Form_NamedElementOutsideBoundsOfForm, elementName,
														inForm);
												}
												addMarker(project, PROJECT_FORM_MARKER_TYPE, msg, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
											}
											break;
										}
									}
								}
							}
						}
						if (o instanceof ISupportName && !(o instanceof Media))
						{
							String name = ((ISupportName)o).getName();
							if (name != null && !"".equals(name) && !IdentDocumentValidator.isJavaIdentifier(name))
							{
								String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Solution_ElementNameInvalidIdentifier, name);
								addMarker(project, SOLUTION_PROBLEM_MARKER_TYPE, msg, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
							}
						}
						checkCancel();
						if (o instanceof TableNode)
						{
							TableNode node = (TableNode)o;
							if (!missingServers.containsKey(node.getServerName()))
							{
								Table table = null;
								try
								{
									table = node.getTable();
								}
								catch (Exception e)
								{
								}
								if (table == null)
								{
									Iterator<IPersist> iterator = node.getAllObjects();
									while (iterator.hasNext())
									{
										IPersist persist = iterator.next();
										String what;
										if (persist instanceof AggregateVariable) what = "Aggregation"; //$NON-NLS-1$
										else what = "Calculation"; //$NON-NLS-1$
										String message = MarkerMessages.getMessage(MarkerMessages.Marker_Table_ItemReferencesInvalidTable, what,
											((ISupportName)persist).getName(), node.getTableName());
										addMarker(project, INVALID_TABLE_NODE_PROBLEM, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null,
											persist);
									}
								}
							}
						}
						checkCancel();
						if (o instanceof Form)
						{
							Form form = (Form)o;
							Table table = null;
							if (!missingServers.containsKey(form.getServerName()))
							{
								try
								{
									table = form.getTable();
									if (table != null && !table.getExistInDB())
									{
										// the table was probably deleted - update the form table as well 
										form.clearTable();
										table = form.getTable();
									}

									if (table == null && form.getDataSource() != null)
									{
										String message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_TableNotAccessible, form.getName(),
											form.getTableName());
										addMarker(project, PROJECT_FORM_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH, null, form);
									}
									else if (table != null && table.getRowIdentColumnsCount() == 0)
									{
										String message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_TableNoPK, form.getName(), form.getTableName());
										addMarker(project, PROJECT_FORM_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH, null, form);
									}
									else if (table != null && form.getInitialSort() != null)
									{
										addMarkers(project, checkSortOptions(table, form.getInitialSort(), form, flattenedSolution), form);
									}
								}
								catch (Exception e)
								{
									exceptionCount++;
									if (exceptionCount < MAX_EXCEPTIONS) ServoyLog.logError(e);
									String message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_TableNotAccessible, form.getName(),
										form.getTableName());
									addMarker(project, PROJECT_FORM_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH, null, form);
								}
							}
							try
							{
								if (table != null)
								{
									Iterator<ScriptVariable> iterator = form.getScriptVariables(false);
									while (iterator.hasNext())
									{
										ScriptVariable var = iterator.next();
										if (table.getColumn(var.getName()) != null)
										{
											String message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_VariableTableCol, form.getName(),
												var.getName(), form.getTableName());
											addMarker(project, PROJECT_FORM_MARKER_TYPE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null,
												var);
										}
									}
								}
							}
							catch (Exception ex)
							{
								exceptionCount++;
								if (exceptionCount < MAX_EXCEPTIONS) ServoyLog.logError(ex);
							}
							if (form.getExtendsFormID() > 0 && flattenedSolution != null)
							{
								Form superForm = flattenedSolution.getForm(form.getExtendsFormID());
								if (superForm != null)
								{
									if (form.getDataSource() != null && superForm.getDataSource() != null &&
										!form.getDataSource().equals(superForm.getDataSource()))
									{
										String message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_DerivedFormDifferentTable, form.getName(),
											superForm.getName());
										addMarker(project, PROJECT_FORM_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, form);
									}
									Iterator<ScriptVariable> iterator = form.getScriptVariables(false);
									ScriptVariable childVariable = null;
									ScriptVariable parentVariable = null;
									while (iterator.hasNext())
									{
										childVariable = iterator.next();
										boolean redefined = false;

										List<Integer> forms = new ArrayList<Integer>();
										forms.add(Integer.valueOf(form.getID()));
										Form superFormVisitor = superForm;
										while (!redefined && superFormVisitor != null)
										{
											if (forms.contains(Integer.valueOf(superFormVisitor.getID()))) break;
											forms.add(Integer.valueOf(superFormVisitor.getID()));

											Iterator<ScriptVariable> superIterator = superFormVisitor.getScriptVariables(false);
											while (superIterator.hasNext())
											{
												parentVariable = superIterator.next();
												if (childVariable.getName().equals(parentVariable.getName()))
												{
													redefined = true;
													break;
												}
											}

											if (superFormVisitor.getExtendsFormID() > 0)
											{
												superFormVisitor = flattenedSolution.getForm(superFormVisitor.getExtendsFormID());
											}
											else
											{
												superFormVisitor = null;
											}
										}

										if (redefined)
										{
											String message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_DerivedFormRedefinedVariable, form.getName(),
												childVariable.getName());
											addMarker(project, PROJECT_FORM_MARKER_TYPE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null,
												childVariable);
										}
									}

									List<Integer> forms = new ArrayList<Integer>();
									forms.add(Integer.valueOf(form.getID()));
									forms.add(Integer.valueOf(superForm.getID()));
									while (superForm != null)
									{
										if (superForm.getExtendsFormID() > 0)
										{
											superForm = flattenedSolution.getForm(superForm.getExtendsFormID());
											if (superForm != null)
											{
												if (forms.contains(Integer.valueOf(superForm.getID())))
												{
													// a cycle detected
													String message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_ExtendsCycle, form.getName());
													addMarker(project, PROJECT_FORM_MARKER_TYPE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW,
														null, form);
													break;
												}
												else
												{
													forms.add(Integer.valueOf(superForm.getID()));
												}
											}
										}
										else
										{
											superForm = null;
										}
									}
								}

							}
							// check for duplicate parts
							Map<Integer, Boolean> parts = new HashMap<Integer, Boolean>();
							Iterator<com.servoy.j2db.persistence.Part> it = form.getParts();
							while (it.hasNext())
							{
								com.servoy.j2db.persistence.Part part = it.next();
								if (!part.canBeMoved() && parts.containsKey(Integer.valueOf(part.getPartType())))
								{
									if (parts.get(Integer.valueOf(part.getPartType())) != null && parts.get(Integer.valueOf(part.getPartType())).booleanValue())
									{
										String message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_DuplicatePart, form.getName(),
											com.servoy.j2db.persistence.Part.getDisplayName(part.getPartType()));
										addMarker(project, PROJECT_FORM_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, form);
										parts.put(Integer.valueOf(part.getPartType()), Boolean.FALSE);
									}
								}
								else
								{
									parts.put(Integer.valueOf(part.getPartType()), Boolean.TRUE);
								}
							}

							Map<Integer, Boolean> tabSequences = new HashMap<Integer, Boolean>();
							Iterator<IPersist> iterator = form.getAllObjects();
							while (iterator.hasNext())
							{
								IPersist persist = iterator.next();
								if (persist instanceof ISupportTabSeq && ((ISupportTabSeq)persist).getTabSeq() > 0)
								{
									int tabSeq = ((ISupportTabSeq)persist).getTabSeq();
									if (tabSequences.containsKey(Integer.valueOf(tabSeq)))
									{
										String message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_UnnamedElementDuplicateTabSequence,
											form.getName());
										if (persist instanceof ISupportName && ((ISupportName)persist).getName() != null)
										{
											message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_NamedElementDuplicateTabSequence, form.getName(),
												((ISupportName)persist).getName());
										}
										addMarker(project, PROJECT_FORM_MARKER_TYPE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null,
											persist);
									}
									else
									{
										tabSequences.put(Integer.valueOf(tabSeq), null);
									}
								}
							}
							if (form.getRowBGColorCalculation() != null)
							{
								ScriptMethod scriptMethod = null;
								boolean unresolved = true;
								if (form.getRowBGColorCalculation().startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
								{
									String methodName = form.getRowBGColorCalculation().substring(ScriptVariable.GLOBAL_DOT_PREFIX.length());
									scriptMethod = flattenedSolution.getScriptMethod(methodName);
								}
								if (scriptMethod == null)
								{
									if (table != null)
									{
										Iterator<TableNode> tableNodes = null;
										try
										{
											tableNodes = flattenedSolution.getTableNodes(table);
										}
										catch (RepositoryException e)
										{
											ServoyLog.logError(e);
										}
										if (tableNodes != null)
										{
											while (tableNodes.hasNext())
											{
												ScriptCalculation calc = AbstractBase.selectByName(tableNodes.next().getScriptCalculations().iterator(),
													form.getRowBGColorCalculation());
												if (calc != null)
												{
													unresolved = false;
													break;
												}
											}
										}
									}
								}
								else
								{
									unresolved = false;
								}
								if (unresolved)
								{
									String message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_RowBGCalcTargetNotFound, form.getName());
									addMarker(project, PROJECT_FORM_MARKER_TYPE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, form);
								}
							}
						}
						checkCancel();
						if (o instanceof ScriptCalculation)
						{
							ScriptCalculation calc = (ScriptCalculation)o;
							if (calc.getMethodCode() != null)
							{
								String text = calc.getMethodCode().toLowerCase();
								if (text.contains("forms.")) //$NON-NLS-1$
								{
									String[] s = text.split("forms."); //$NON-NLS-1$
									for (int i = 0; i < s.length - 1; i++)
									{
										if (s[i] == null || s[i].length() == 0 || Character.isWhitespace(s[i].charAt(s[i].length() - 1)))
										{
											Pair<String, String> pathPair = SolutionSerializer.getFilePath(o, true);
											Path path = new Path(pathPair.getLeft() + pathPair.getRight());
											String message = MarkerMessages.getMessage(MarkerMessages.Marker_Calculation_FormAccess, calc.getName());
											try
											{
												Table table = calc.getTable();
												if (table != null)
												{
													message = MarkerMessages.getMessage(MarkerMessages.Marker_Calculation_InTableFormAccess, calc.getName(),
														table.getName());
												}
												addMarker(project, CALCULATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL,
													path.toString(), calc);
											}
											catch (RepositoryException e)
											{
												Debug.log("table not found for calc: " + calc, e); //$NON-NLS-1$
											}
											break;
										}
									}

								}
								StringBuilder sb = new StringBuilder();
								sb.append(SolutionSerializer.FUNCTION_KEYWORD);
								sb.append(' ');
								sb.append(calc.getDisplayName());
								sb.append("()\n{\n"); //$NON-NLS-1$
								String source = calc.getSource();
								if (source != null && source.trim().length() > 0)
								{
									sb.append(source);
									sb.append('\n');
								}
								sb.append("}\n"); //$NON-NLS-1$
								String[] variables = ScriptingUtils.getGlobalVariables(sb.toString());
								if (variables != null && variables.length > 0)
								{
									try
									{
										Table table = calc.getTable();
										for (String name : variables)
										{
											if (table.getColumn(name) == null)
											{
												Integer lineNumber = Integer.valueOf(calc.getLineNumberOffset());
												String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Calculation_UndeclaredVariable, calc.getName(),
													name);
												addMarker(project, CALCULATION_MARKER_TYPE, msg, lineNumber == null ? -1 : lineNumber.intValue(),
													IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, calc);
											}

										}
									}
									catch (Exception ex)
									{
									}
								}
							}

						}
						checkCancel();
						if (o instanceof Tab)
						{
							Tab tab = (Tab)o;
							if (tab.getRelationName() != null)
							{
								Relation[] relations = servoyModel.getFlattenedSolution().getRelationSequence(tab.getRelationName());
								if (relations == null)
								{
									String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Form_RelatedTabUnsolvedRelation, tab.getRelationName());
									addMarker(project, PROJECT_FORM_MARKER_TYPE, msg, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, tab);
								}
								else
								{
									Relation relation = relations[0];
									if (!relation.isGlobal() && relation.getPrimaryServerName() != null && relation.getPrimaryTableName() != null)
									{
										if (parentForm != null && (!relation.getPrimaryDataSource().equals(parentForm.getDataSource())))
										{
											String message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_RelatedTabDifferentTable,
												parentForm.getName(), relation.getName());
											addMarker(project, PROJECT_FORM_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null,
												tab);
										}
									}
									relation = relations[relations.length - 1];
									if (!relation.isGlobal() && relation.getPrimaryServerName() != null && relation.getPrimaryTableName() != null)
									{
										Form form = servoyModel.getFlattenedSolution().getForm(tab.getContainsFormID());
										if (form != null &&
											(!relation.getForeignServerName().equals(form.getServerName()) || !relation.getForeignTableName().equals(
												form.getTableName())))
										{
											String message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_RelatedTabDifferentTable, form.getName(),
												relation.getName());
											addMarker(project, PROJECT_FORM_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null,
												tab);
										}
									}
								}
							}
						}
						checkCancel();
						if (o instanceof Field)
						{
							Field field = (Field)o;
							if (field.getDisplayType() == Field.COMBOBOX && field.getEditable() && field.getValuelistID() > 0)
							{
								ValueList vl = flattenedSolution.getValueList(field.getValuelistID());
								if (vl != null)
								{
									boolean showWarning = false;
									if (vl.getValueListType() == ValueList.DATABASE_VALUES && vl.getReturnDataProviders() != vl.getShowDataProviders())
									{
										showWarning = true;
									}
									if (vl.getValueListType() == ValueList.CUSTOM_VALUES && vl.getCustomValues() != null && vl.getCustomValues().contains("|"))
									{
										showWarning = true;
									}
									if (showWarning)
									{
										String message;
										if (field.getName() != null)
										{
											message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_EditableNamedComboboxCustomValuelist,
												field.getName());
										}
										else
										{
											message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_EditableUnnamedComboboxCustomValuelist);
										}
										addMarker(project, PROJECT_FORM_MARKER_TYPE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null,
											field);
									}
								}
							}
							if (field.getValuelistID() > 0 && (field.getDisplayType() == Field.TEXT_FIELD || field.getDisplayType() == Field.TYPE_AHEAD))
							{
								ValueList vl = flattenedSolution.getValueList(field.getValuelistID());
								if (vl != null && vl.getValueListType() == ValueList.DATABASE_VALUES)
								{
									try
									{
										Table table = (Table)vl.getTable();
										ScriptCalculation calc = null;
										boolean errorFound = false;
										if (vl.getDataProviderID1() != null)
										{
											try
											{
												calc = flattenedSolution.getScriptCalculation(vl.getDataProviderID1(), table);
												if (calc != null)
												{
													Column column = table.getColumn(vl.getDataProviderID1());
													if (column == null) errorFound = true;
												}
											}
											catch (RepositoryException e)
											{
												ServoyLog.logError(e);
											}
										}
										if (vl.getDataProviderID2() != null && !errorFound)
										{
											try
											{
												calc = flattenedSolution.getScriptCalculation(vl.getDataProviderID2(), table);
												if (calc != null)
												{
													Column column = table.getColumn(vl.getDataProviderID2());
													if (column == null) errorFound = true;
												}
											}
											catch (RepositoryException e)
											{
												ServoyLog.logError(e);
											}
										}
										if (vl.getDataProviderID3() != null && !errorFound)
										{
											try
											{
												calc = flattenedSolution.getScriptCalculation(vl.getDataProviderID3(), table);
												if (calc != null)
												{
													Column column = table.getColumn(vl.getDataProviderID3());
													if (column == null) errorFound = true;
												}
											}
											catch (RepositoryException e)
											{
												ServoyLog.logError(e);
											}
										}
										if (errorFound)
										{
											String message;
											if (field.getName() != null)
											{
												message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_TypeAheadNamedUnstoredCalculation,
													field.getName());
											}
											else
											{
												message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_TypeAheadUnnamedUnstoredCalculation);
											}
											addMarker(project, PROJECT_FORM_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null,
												field);
										}
									}
									catch (Exception e)
									{
										ServoyLog.logError(e);
									}
								}
							}
						}
						if (o instanceof Portal && ((Portal)o).getRelationName() != null)
						{
							Portal portal = (Portal)o;
							Relation[] relations = flattenedSolution.getRelationSequence(portal.getRelationName());
							if (relations == null)
							{
								String message;
								if (portal.getName() != null)
								{
									message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_PortalNamedInvalidName, portal.getName());
								}
								else
								{
									message = MarkerMessages.getMessage(MarkerMessages.Marker_Form_PortalUnnamedInvalidName);
								}
								addMarker(project, PROJECT_FORM_MARKER_TYPE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, o);
							}
							else
							{
								for (IPersist child : portal.getAllObjectsAsList())
								{
									if (child instanceof ISupportDataProviderID)
									{
										try
										{
											String id = ((ISupportDataProviderID)child).getDataProviderID();
											if (id != null)
											{
												int indx = id.lastIndexOf('.');
												if (indx > 0)
												{
													String rel_name = id.substring(0, indx);
													if (!portal.getRelationName().equals(rel_name))
													{
														String message;
														String elementName = null;
														if (child instanceof ISupportName && ((ISupportName)child).getName() != null)
														{
															elementName = ((ISupportName)child).getName();
														}
														if (portal.getName() != null)
														{
															if (elementName != null)
															{
																message = MarkerMessages.getMessage(
																	MarkerMessages.Marker_Form_PortalNamedElementNamedMismatchedRelation, elementName,
																	portal.getName(), rel_name, portal.getRelationName());
															}
															else
															{
																message = MarkerMessages.getMessage(
																	MarkerMessages.Marker_Form_PortalNamedElementUnnamedMismatchedRelation, portal.getName(),
																	rel_name, portal.getRelationName());
															}
														}
														else
														{
															if (elementName != null)
															{
																message = MarkerMessages.getMessage(
																	MarkerMessages.Marker_Form_PortalUnnamedElementNamedMismatchedRelation, elementName,
																	rel_name, portal.getRelationName());
															}
															else
															{
																message = MarkerMessages.getMessage(
																	MarkerMessages.Marker_Form_PortalUnnamedElementUnnamedMismatchedRelation, rel_name,
																	portal.getRelationName());
															}
														}
														addMarker(project, PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR,
															IMarker.PRIORITY_NORMAL, null, child);
													}
												}
											}
										}
										catch (Exception ex)
										{
											ServoyLog.logError(ex);
										}
									}
								}
							}
						}
						checkDeprecatedPropertyUsage(o, project);
						ISupportChilds parent = o.getParent();
						if (o.getTypeID() == IRepository.SOLUTIONS && parent != null)
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.MEDIA && (parent == null || parent.getTypeID() != IRepository.SOLUTIONS))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.SCRIPTVARIABLES &&
							(parent == null || (parent.getTypeID() != IRepository.SOLUTIONS && parent.getTypeID() != IRepository.FORMS)))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.METHODS &&
							(parent == null || (parent.getTypeID() != IRepository.SOLUTIONS && parent.getTypeID() != IRepository.FORMS)))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.FORMS && (parent == null || parent.getTypeID() != IRepository.SOLUTIONS))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.RELATIONS && (parent == null || parent.getTypeID() != IRepository.SOLUTIONS))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.TABLENODES && (parent == null || parent.getTypeID() != IRepository.SOLUTIONS))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.VALUELISTS && (parent == null || parent.getTypeID() != IRepository.SOLUTIONS))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.PORTALS && (parent == null || parent.getTypeID() != IRepository.FORMS))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.TABPANELS && (parent == null || parent.getTypeID() != IRepository.FORMS))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.BEANS && (parent == null || parent.getTypeID() != IRepository.FORMS))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.RECTSHAPES &&
							(parent == null || (parent.getTypeID() != IRepository.FORMS && parent.getTypeID() != IRepository.PORTALS)))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.GRAPHICALCOMPONENTS &&
							(parent == null || (parent.getTypeID() != IRepository.FORMS && parent.getTypeID() != IRepository.PORTALS)))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.SHAPES &&
							(parent == null || (parent.getTypeID() != IRepository.FORMS && parent.getTypeID() != IRepository.PORTALS)))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.PARTS && (parent == null || parent.getTypeID() != IRepository.FORMS))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.FIELDS &&
							(parent == null || (parent.getTypeID() != IRepository.FORMS && parent.getTypeID() != IRepository.PORTALS)))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.RELATION_ITEMS && (parent == null || parent.getTypeID() != IRepository.RELATIONS))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.TABS && (parent == null || parent.getTypeID() != IRepository.TABPANELS))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.SCRIPTCALCULATIONS && (parent == null || parent.getTypeID() != IRepository.TABLENODES))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (o.getTypeID() == IRepository.AGGREGATEVARIABLES && (parent == null || parent.getTypeID() != IRepository.TABLENODES))
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (!(o instanceof IVariable) && !(o instanceof IScriptProvider) &&
							!Utils.getAsBoolean(((AbstractBase)o).getRuntimeProperty(SolutionDeserializer.POSSIBLE_DUPLICATE_UUID)))
						{
							// remove this property as it takes too much memory
							// debugging engine needs this info for scriptproviders !!
							((AbstractBase)o).setSerializableRuntimeProperty(IScriptProvider.FILENAME, null);
						}
						return IPersistVisitor.CONTINUE_TRAVERSAL;
					}
				});
				checkRelations(project, missingServers);
				checkCancel();
				checkStyles(project);
				checkI18n(project);
				checkLoginSolution(project);
			}


			for (String missingServer : missingServers.keySet())
			{
				IPersist persist = missingServers.get(missingServer);
				String message = MarkerMessages.getMessage(MarkerMessages.Marker_Solution_ServerNotAccessibleFirstOccurence, project.getName(), missingServer);
				IMarker marker = addMarker(project, MISSING_SERVER, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH, null, persist);
				try
				{
					marker.setAttribute("missingServer", missingServer); //$NON-NLS-1$
					marker.setAttribute("Uuid", persist.getUUID().toString()); //$NON-NLS-1$
					marker.setAttribute("SolutionName", project.getName()); //$NON-NLS-1$
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}

			}

		}
		else
		{
			ServoyLog.logError("Servoy project is null for a eclipse project with correct nature", null); //$NON-NLS-1$
		}
	}

	private boolean formCanBeInstantiated(Form form, FlattenedSolution flattenedSolution, Map<Form, Boolean> checked)
	{
		Boolean canBeInstantiated = checked.get(form);
		if (canBeInstantiated == null)
		{
			canBeInstantiated = new Boolean(flattenedSolution.formCanBeInstantiated(form));
			checked.put(form, canBeInstantiated);
		}
		return canBeInstantiated.booleanValue();
	}

	/**
	 * Checks the state of a valueList, and is able to automatically correct a few problems - but with the possibility of loosing info.
	 * 
	 * @param vl the valueList to be checked.
	 * @param flattenedSolution the flattened solution used to get relations if needed.
	 * @param sm the server manager to use.
	 * @param fixIfPossible if this is true, it will try to fix a few problems, but be careful - some invalid info in the valueList will probably be lost.
	 * @return a list of problems. Each element in the list represents a problem, and is a 3 object array like [ (int)severity, (String)problemMessage,
	 *         (String)fixDescription ]. "fixDescription" is the description of the fix that would be applied if fixIfPossible is true; null if no fix would be
	 *         applied.
	 */
	public static List<Problem> checkValuelist(ValueList vl, FlattenedSolution flattenedSolution, IServerManagerInternal sm, boolean fixIfPossible)
	{
		List<Problem> problems = new ArrayList<Problem>();
		try
		{
			if (vl.getValueListType() == ValueList.DATABASE_VALUES)
			{
				if (vl.getCustomValues() != null)
				{
					// this is not a custom valuelist
					String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_DBWithCustomValues, vl.getName());
					problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_ERROR, msg, MarkerMessages.Marker_Fix_RemoveCustomValues));
					if (fixIfPossible) vl.setCustomValues(null);
				}
				String dataSource = null;
				Table table = null;
				if (vl.getRelationName() != null)
				{
					// vl. based on relation; make sure table name/server name are not specified
					if (vl.getTableName() != null || vl.getServerName() != null)
					{
						String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_RelationWithDatasource, vl.getName());
						problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_ERROR, msg, MarkerMessages.Marker_Fix_RemoveDatasource));
						if (fixIfPossible) vl.setDataSource(null);
					}
					String[] parts = vl.getRelationName().split("\\."); //$NON-NLS-1$
					for (String relName : parts)
					{
						Relation relation = flattenedSolution.getRelation(relName);
						if (relation == null)
						{
							String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_RelationNotFound, vl.getName(), relName);
							problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_ERROR, msg));
						}
						else
						{
							dataSource = relation.getForeignDataSource();
						}
					}
					if (dataSource != null)
					{
						// check if the relations match up (check foreign/primary tables)
						if (flattenedSolution.getRelationSequence(vl.getRelationName()) == null)
						{
							String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_RelationSequenceInconsistent, vl.getName(),
								vl.getRelationName());
							problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_ERROR, msg));
						}
					}
				}
				else if (vl.getDataSource() != null)
				{
					// this is table based...
					dataSource = vl.getDataSource();
				}
				else
				{
					String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_DBNotTableOrRelation, vl.getName());
					problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_ERROR, msg));
				}
				if (dataSource != null)
				{
					String[] stn = DataSourceUtils.getDBServernameTablename(dataSource);
					if (stn == null || (stn != null && (stn.length == 0 || (stn.length > 0 && stn[0] == null))))
					{
						String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_DBMalformedTableDefinition, vl.getName(), dataSource);
						problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_ERROR, msg));
					}
					else
					{
						IServerInternal server = (IServerInternal)sm.getServer(stn[0]);
						if (server != null)
						{
							if (!server.getName().equals(stn[0]))
							{
								String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_DBServerDuplicate, vl.getName(), stn[0]);
								problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_WARNING, msg));
							}
							table = server.getTable(stn[1]);
							if (table == null)
							{
								String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_DBTableNotAccessible, vl.getName(), stn[1]);
								problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_ERROR, msg));
							}
						} // server not found is reported elsewhere
					}
				}
				if (table != null)
				{
					if (vl.getDataProviderID1() != null && !"".equals(vl.getDataProviderID1())) //$NON-NLS-1$
					{
						Column column = table.getColumn(vl.getDataProviderID1());
						if (column == null)
						{
							if (flattenedSolution.getScriptCalculation(vl.getDataProviderID1(), table) == null)
							{
								String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_DBDatasourceNotFound, vl.getName(),
									vl.getDataProviderID1(), table.getName());
								problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_ERROR, msg));
							}
						}
					}
					if (vl.getDataProviderID2() != null && !vl.getDataProviderID2().equals(vl.getDataProviderID1()) && !"".equals(vl.getDataProviderID2()))//$NON-NLS-1$
					{
						Column column = table.getColumn(vl.getDataProviderID2());
						if (column == null)
						{
							if (flattenedSolution.getScriptCalculation(vl.getDataProviderID2(), table) == null)
							{
								String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_DBDatasourceNotFound, vl.getName(),
									vl.getDataProviderID2(), table.getName());
								problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_ERROR, msg));
							}
						}
					}
					if (vl.getDataProviderID3() != null && !vl.getDataProviderID3().equals(vl.getDataProviderID1()) &&
						!vl.getDataProviderID3().equals(vl.getDataProviderID2()) && !"".equals(vl.getDataProviderID3()))//$NON-NLS-1$
					{
						Column column = table.getColumn(vl.getDataProviderID3());
						if (column == null)
						{
							if (flattenedSolution.getScriptCalculation(vl.getDataProviderID3(), table) == null)
							{
								String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_DBDatasourceNotFound, vl.getName(),
									vl.getDataProviderID3(), table.getName());
								problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_ERROR, msg));
							}
						}
					}
					if (vl.getUseTableFilter() && vl.getValueListType() == ValueList.DATABASE_VALUES && vl.getDatabaseValuesType() == ValueList.TABLE_VALUES)
					{
						Column column = table.getColumn(DBValueList.NAME_COLUMN);
						if (column == null)
						{
							String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_DBDatasourceNotFound, vl.getName(), DBValueList.NAME_COLUMN,
								table.getName());
							problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_ERROR, msg));
						}
					}

					if (vl.getSortOptions() != null)
					{
						List<Problem> sortProblems = checkSortOptions(table, vl.getSortOptions(), vl, flattenedSolution);
						if (sortProblems != null)
						{
							problems.addAll(sortProblems);
						}
					}
				}
			}
			else if (vl.getValueListType() == ValueList.CUSTOM_VALUES || vl.getValueListType() == ValueList.GLOBAL_METHOD_VALUES)
			{
				// custom value list; make sure it does not specify table/server/relation
				if (vl.getTableName() != null || vl.getServerName() != null || vl.getRelationName() != null)
				{
					String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_CustomValuesWithDBInfo, vl.getName());
					problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_ERROR, msg, MarkerMessages.Marker_Fix_RemovedDBInfo));
					if (fixIfPossible)
					{
						vl.setDataSource(null);
						vl.setRelationName(null);
					}
				}
				if (vl.getValueListType() == ValueList.GLOBAL_METHOD_VALUES)
				{
					String methodName = vl.getCustomValues().substring(ScriptVariable.GLOBAL_DOT_PREFIX.length());
					ScriptMethod scriptMethod = flattenedSolution.getScriptMethod(methodName);
					if (scriptMethod == null)
					{
						String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_GlobalMethodNotFound, vl.getName());
						problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_ERROR, msg));
					}
				}
				if (vl.getValueListType() == ValueList.CUSTOM_VALUES)
				{
					String values = vl.getCustomValues();
					boolean invalidValues = false;
					if (values != null && values.contains("|"))
					{
						StringTokenizer tk = new StringTokenizer(values.trim(), "\r\n"); //$NON-NLS-1$
						while (tk.hasMoreTokens())
						{
							String line = tk.nextToken();
							if (!line.contains("|"))
							{
								invalidValues = true;
								break;
							}
						}
					}
					if (invalidValues)
					{
						String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_InvalidCustomValues, vl.getName());
						problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_WARNING, msg));
					}
				}
			}
			else
			{
				String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_TypeUnknown, vl.getName(), vl.getValueListType());
				problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_ERROR, msg));
				if (fixIfPossible) vl.setValueListType(ValueList.CUSTOM_VALUES);
			}
		}
		catch (Exception ex)
		{
			exceptionCount++;
			if (exceptionCount < MAX_EXCEPTIONS) ServoyLog.logError(ex);
			String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Valuelist_GenericError, vl.getName());
			if (ex.getMessage() != null)
			{
				msg += " " + ex.getMessage();
			}
			problems.add(new Problem(PROJECT_VALUELIST_MARKER_TYPE, IMarker.SEVERITY_ERROR, msg));
		}
		return problems;
	}

	public static ServoyProject[] getSolutionModules(ServoyProject project)
	{
		List<ServoyProject> modules = new ArrayList<ServoyProject>();
		addModules(modules, project);
		return modules.toArray(new ServoyProject[] { });
	}

	private static void addModules(List<ServoyProject> modules, ServoyProject servoyProject)
	{
		String modulesNames = null;
		if (servoyProject.getSolution() != null) modulesNames = servoyProject.getSolution().getModulesNames();
		if (modulesNames != null && !"".equals(modulesNames)) //$NON-NLS-1$
		{
			StringTokenizer st = new StringTokenizer(modulesNames, ";,"); //$NON-NLS-1$
			while (st.hasMoreTokens())
			{
				String name = st.nextToken().trim();
				ServoyProject module = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(name);
				if (module != null && !modules.contains(module))
				{
					modules.add(module);
					addModules(modules, module);
				}
			}
		}
	}

	private void addDeserializeProblemMarkers(ServoyProject servoyProject)
	{
		HashMap<File, Exception> deserializeExceptionMessages = servoyProject.getDeserializeExceptions();
		String deserializeExceptionMessage = null;
		for (Map.Entry<File, Exception> entry : deserializeExceptionMessages.entrySet())
		{
			IResource file = getEclipseResourceFromJavaIO(entry.getKey(), servoyProject.getProject());
			if (file == null) file = servoyProject.getProject();
			addDeserializeProblemMarker(file, entry.getValue().getMessage(), servoyProject.getProject().getName());
		}

		if (deserializeExceptionMessage == null && servoyProject.getSolution() == null)
		{
			addDeserializeProblemMarker(servoyProject.getProject(), "Probably some corrupted file(s). Please check solution metadata file.", //$NON-NLS-1$
				servoyProject.getProject().getName());
			ServoyLog.logError("No solution in a servoy project that has no deserialize problems", null); //$NON-NLS-1$
		}
	}

	private void addDeserializeProblemMarker(IResource resource, String deserializeExceptionMessage, String solutionName)
	{
		String message;
		int charNo = -1;
		if (deserializeExceptionMessage == null)
		{
			message = MarkerMessages.getMessage(MarkerMessages.Marker_Deserialize_Error, solutionName, "Errors in file content.");
		}
		else
		{
			message = MarkerMessages.getMessage(MarkerMessages.Marker_Deserialize_Error, solutionName, deserializeExceptionMessage);
			// find out where the error occurred if possible... this could work for JSON errors
			int idx = deserializeExceptionMessage.indexOf("character"); //$NON-NLS-1$
			if (idx >= 0)
			{
				StringTokenizer st = new StringTokenizer(deserializeExceptionMessage.substring(idx + 9), " "); //$NON-NLS-1$
				if (st.hasMoreTokens())
				{
					String charNoString = st.nextToken();
					try
					{
						charNo = Integer.parseInt(charNoString);
					}
					catch (NumberFormatException e)
					{
						// cannot find character number... this is not a tragedy
					}
				}
			}
		}
		addMarker(resource, PROJECT_DESERIALIZE_MARKER_TYPE, message, charNo, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null);
		ServoyLog.logWarning(message, null);
	}

	private void parseEventMethod(final IProject project, final ScriptMethod eventMethod, final String eventName)
	{
		if (eventMethod != null &&
			(eventMethod.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS) == null || eventMethod.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS).length == 0) &&
			eventMethod.getDeclaration().contains("arguments"))
		{
			final Script script = javascriptParser.parse(eventMethod.getDeclaration(), dummyReporter);
			List<Statement> statements = script.getStatements();
			if (statements != null && statements.size() == 1 && (statements.get(0) instanceof VoidExpression))
			{
				Expression exp = ((VoidExpression)statements.get(0)).getExpression();
				if (exp instanceof FunctionStatement)
				{
					FunctionStatement function = (FunctionStatement)exp;
					final int functionStart = function.sourceStart();
					try
					{
						final String functionCode = eventMethod.getDeclaration();
						function.getBody().traverse(new ASTVisitor()
						{
							@Override
							public boolean visitGeneral(ASTNode node) throws Exception
							{
								if (node instanceof GetArrayItemExpression && (((GetArrayItemExpression)node).getArray() instanceof Identifier) &&
									"arguments".equals(((Identifier)((GetArrayItemExpression)node).getArray()).getName()))
								{
									String message = MarkerMessages.getMessage(MarkerMessages.Marker_Method_EventParameters);
									int offset = 0;
									for (int i = functionStart; i < node.sourceStart(); i++)
									{
										if (functionCode.charAt(i) == '\n') offset++;
									}
									IMarker marker = addMarker(project, EVENT_METHOD_MARKER_TYPE, message, eventMethod.getLineNumberOffset() + offset,
										IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, eventMethod);
									marker.setAttribute("EventName", eventName); //$NON-NLS-1$
								}
								return true;
							}
						});
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}
	}

	private boolean skipEventMethod(String name)
	{
		if ("onOpenMethodID".equals(name)) return true;
		return false;
	}

	private IResource getEclipseResourceFromJavaIO(File javaIOFile, IProject project)
	{
		IWorkspace workspace = ServoyModel.getWorkspace();
		IPath location = Path.fromOSString(javaIOFile.getAbsolutePath());
		IResource resource = workspace.getRoot().getFileForLocation(location);
		if (resource == null)
		{
			resource = workspace.getRoot().getContainerForLocation(location);
		}
		return (resource.exists() && resource.getProject() == project) ? resource : null;
	}

	private void addBadStructureMarker(IPersist o, ServoyProject servoyProject, IProject project)
	{
		String message = MarkerMessages.getMessage(MarkerMessages.Marker_Structure_BadStructure, servoyProject.getSolution().getName());
		Pair<String, String> pathPair = SolutionSerializer.getFilePath(o, true);
		String path = ((AbstractBase)o).getSerializableRuntimeProperty(IScriptProvider.FILENAME);
		IResource file = project;
		if (path != null && !"".equals(path)) //$NON-NLS-1$
		{
			file = getEclipseResourceFromJavaIO(new java.io.File(path), project);
			if (file != null) path = file.getProjectRelativePath().toString();
		}
		if (path == null || "".equals(path)) path = pathPair.getRight(); //$NON-NLS-1$
		if (o instanceof ISupportName && ((ISupportName)o).getName() != null)
		{
			message += " " + MarkerMessages.getMessage(MarkerMessages.Marker_Advice_EntityManuallyMoved, ((ISupportName)o).getName()); //$NON-NLS-1$
		}
		addMarker(project, BAD_STRUCTURE_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_LOW, path, o);
	}

	private ServoyProject getServoyProject(IProject project)
	{
		ServoyProject sp = null;
		try
		{
			sp = (ServoyProject)project.getNature(ServoyProject.NATURE_ID);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
		return sp;
	}

	private void checkI18n(IProject project)
	{
		ServoyProject servoyProject = servoyModel.getServoyProject(project.getName());
		Solution solution = servoyProject.getSolution();
		if (solution.getI18nTableName() != null && solution.getI18nServerName() != null)
		{
			ServoyProject[] modules = getSolutionModules(servoyProject);
			if (modules != null)
			{
				for (ServoyProject module : modules)
				{
					Solution mod = module.getSolution();
					if (mod != null && mod.getI18nServerName() != null && mod.getI18nTableName() != null &&
						(!mod.getI18nServerName().equals(solution.getI18nServerName()) || !mod.getI18nTableName().equals(solution.getI18nTableName())))
					{
						String message = MarkerMessages.getMessage(MarkerMessages.Marker_i18n_ModuleDifferentI18NTable, mod.getName(), solution.getName());
						addMarker(project, I18N_MARKER_TYPE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, null);
					}
				}
			}
		}
	}

	private void checkColumns(final IProject project)
	{
		deleteMarkers(project, COLUMN_MARKER_TYPE);
		try
		{
			if (project.getReferencedProjects() != null)
			{
				for (IProject referenced : project.getReferencedProjects())
				{
					if (referenced.hasNature(ServoyResourcesProject.NATURE_ID))
					{
						deleteMarkers(referenced, COLUMN_MARKER_TYPE);
					}
				}
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		String[] array = ServoyModel.getServerManager().getServerNames(true, true, false, true);
		for (String server_name : array)
		{
			try
			{
				IServerInternal server = (IServerInternal)ServoyModel.getServerManager().getServer(server_name, true, true);
				Iterator<String> tables = server.getTableAndViewNames().iterator();
				while (tables.hasNext())
				{
					String tableName = tables.next();
					if (server.isTableLoaded(tableName))
					{
						Table table = server.getTable(tableName);
						for (Column column : table.getColumns())
						{
							if (column.getColumnInfo() != null && column.getSequenceType() == ColumnInfo.UUID_GENERATOR &&
								!column.getColumnInfo().hasFlag(Column.UUID_COLUMN))
							{
								String message = MarkerMessages.getMessage(MarkerMessages.Marker_Column_UUIDFlagNotSet, tableName, column.getName());
								IResource res = project;
								if (servoyModel.getDataModelManager() != null && servoyModel.getDataModelManager().getDBIFile(server_name, tableName).exists())
								{
									res = servoyModel.getDataModelManager().getDBIFile(server_name, tableName);
								}
								addMarker(res, COLUMN_MARKER_TYPE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, null);
							}
							if (column.getColumnInfo() != null && column.getColumnInfo().getAutoEnterType() == ColumnInfo.LOOKUP_VALUE_AUTO_ENTER)
							{
								String lookup = column.getColumnInfo().getLookupValue();
								if (lookup != null && !"".equals(lookup)) //$NON-NLS-1$
								{
									boolean invalid = false;
									if (lookup.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
									{
										IDataProvider dataProvider = servoyModel.getFlattenedSolution().getGlobalDataProvider(lookup);
										if (dataProvider == null)
										{
											invalid = true;
										}
									}
									else
									{
										Table lookupTable = table;
										int indx = lookup.lastIndexOf('.');
										if (indx > 0)
										{
											String rel_name = lookup.substring(0, indx);
											Relation[] relations = servoyModel.getFlattenedSolution().getRelationSequence(rel_name);
											if (relations == null)
											{
												invalid = true;
											}
											else if (relations.length > 0)
											{
												Relation r = relations[relations.length - 1];
												lookupTable = r.getForeignTable();
											}
										}
										String col = lookup.substring(indx + 1);
										if (lookupTable != null && lookupTable.getColumn(col) == null)
										{
											invalid = true;
										}
									}
									if (invalid)
									{
										String message = MarkerMessages.getMessage(MarkerMessages.Marker_Column_LookupInvalid, tableName, column.getName());
										IResource res = project;
										if (servoyModel.getDataModelManager() != null &&
											servoyModel.getDataModelManager().getDBIFile(server_name, tableName).exists())
										{
											res = servoyModel.getDataModelManager().getDBIFile(server_name, tableName);
										}
										addMarker(res, COLUMN_MARKER_TYPE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, null);
									}
								}
							}
						}
					}
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}

	private void checkStyles(final IProject project)
	{
		deleteMarkers(project, MISSING_STYLE);
		ServoyProject servoyProject = servoyModel.getServoyProject(project.getName());
		FlattenedSolution flattenedSolution = servoyModel.getFlattenedSolution();
		if (servoyProject != null)
		{
			Iterator<Form> it = servoyProject.getSolution().getForms(null, false);
			while (it.hasNext())
			{
				final Form form = it.next();
				String styleName = form.getStyleName();
				if (styleName == null && form.getExtendsFormID() > 0)
				{
					List<Form> forms = flattenedSolution.getFormHierarchy(form);
					for (Form parentForm : forms)
					{
						if (parentForm.getStyleName() != null)
						{
							styleName = parentForm.getStyleName();
							break;
						}
					}
				}
				if (styleName != null)
				{
					Style style = null;
					try
					{
						style = (Style)ServoyModel.getDeveloperRepository().getActiveRootObject(styleName, IRepository.STYLES);
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
					final Style finalStyle = style;
					if (style == null)
					{
						String message = MarkerMessages.getMessage(MarkerMessages.Marker_Style_NotFound, styleName, form.getName());
						IMarker marker = addMarker(project, MISSING_STYLE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, form);
						try
						{
							marker.setAttribute("clearStyle", true);
						}
						catch (CoreException e)
						{
							ServoyLog.logError(e);
						}
						continue;
					}
					form.acceptVisitor(new IPersistVisitor()
					{
						public Object visit(IPersist o)
						{
							if (o instanceof BaseComponent || o instanceof Form)
							{
								String styleClass = null;
								if (o instanceof BaseComponent) styleClass = ((BaseComponent)o).getStyleClass();
								else if (o instanceof Form) styleClass = ((Form)o).getStyleClass();
								if (styleClass != null)
								{
									String lookupName = null;
									if (o instanceof BaseComponent)
									{
										lookupName = ComponentFactory.getLookupName((BaseComponent)o);
									}
									else if (o instanceof Form)
									{
										lookupName = "form"; //$NON-NLS-1$
									}
									String[] classes = CoreUtils.getStyleClasses(finalStyle, lookupName);
									List<String> styleClasses = Arrays.asList(classes);
									if (!styleClasses.contains(styleClass))
									{
										String message = MarkerMessages.getMessage(MarkerMessages.Marker_Style_FormClassNotFound, styleClass, form.getName());
										if (o instanceof ISupportName && !(o instanceof Form) && (((ISupportName)o).getName() != null))
										{
											message = MarkerMessages.getMessage(MarkerMessages.Marker_Style_ElementClassNotFound, styleClass,
												((ISupportName)o).getName(), form.getName());
										}
										addMarker(project, MISSING_STYLE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, o);
									}
								}
							}
							return IPersistVisitor.CONTINUE_TRAVERSAL;
						}
					});
				}
				else
				{
					form.acceptVisitor(new IPersistVisitor()
					{
						public Object visit(IPersist o)
						{
							if (o instanceof BaseComponent || o instanceof Form)
							{
								String styleClass = null;
								if (o instanceof BaseComponent) styleClass = ((BaseComponent)o).getStyleClass();
								else if (o instanceof Form) styleClass = ((Form)o).getStyleClass();
								if (styleClass != null)
								{
									String message = MarkerMessages.getMessage(MarkerMessages.Marker_Style_FormClassNoStyle, styleClass, form.getName());
									if (o instanceof ISupportName && !(o instanceof Form) && (((ISupportName)o).getName() != null))
									{
										message = MarkerMessages.getMessage(MarkerMessages.Marker_Style_ElementClassNoStyle, styleClass,
											((ISupportName)o).getName(), form.getName());
									}
									addMarker(project, MISSING_STYLE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, o);
								}
							}
							return IPersistVisitor.CONTINUE_TRAVERSAL;
						}
					});
				}
			}
		}
	}

	private void checkLoginSolution(IProject project)
	{
		ServoyProject servoyProject = servoyModel.getServoyProject(project.getName());
		if (servoyProject != null)
		{
			boolean isLoginSolution = servoyProject.getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION;
			ServoyProject[] modules = getSolutionModules(servoyProject);
			ServoyProject[] projectWithModules = new ServoyProject[modules.length + 1];
			projectWithModules[0] = servoyProject;
			System.arraycopy(modules, 0, projectWithModules, 1, modules.length);

			for (ServoyProject sp : projectWithModules)
			{
				final IProject prj = sp.getProject();
				deleteMarkers(prj, FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION);
				if (isLoginSolution)
				{
					sp.getSolution().acceptVisitor(new IPersistVisitor()
					{
						public Object visit(IPersist o)
						{
							if (o.getTypeID() == IRepository.FORMS)
							{
								Form form = (Form)o;
								if (((Form)o).getDataSource() != null) // login solution cannot have forms with datasource
								{
									String message = "Form '" + form.getName() + "' is part of a login solution and it must not have the datasource property set; its current datasource is : '" + form.getDataSource() + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
									IMarker marker = addMarker(prj, FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION, message, -1, IMarker.SEVERITY_WARNING,
										IMarker.PRIORITY_HIGH, null, form);
									if (marker != null)
									{
										try
										{
											marker.setAttribute("Uuid", o.getUUID().toString()); //$NON-NLS-1$
											marker.setAttribute("SolutionName", form.getSolution().getName()); //$NON-NLS-1$
											marker.setAttribute("PropertyName", "dataSource"); //$NON-NLS-1$ //$NON-NLS-2$
											marker.setAttribute("DisplayName", RepositoryHelper.getDisplayName("dataSource", o.getClass())); //$NON-NLS-1$ //$NON-NLS-2$
										}
										catch (CoreException ex)
										{
											ServoyLog.logError(ex);
										}
									}
								}
								return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
							}
							return IPersistVisitor.CONTINUE_TRAVERSAL;
						}

					});
				}
			}
		}
	}

	private static List<Problem> checkSortOptions(Table table, String sortOptions, IPersist persist, FlattenedSolution flattenedSolution)
	{
		if (persist == null || sortOptions == null) return null;
		List<Problem> problems = new ArrayList<Problem>();

		String elementName = null;
		if (persist instanceof Form)
		{
			elementName = "Form";//$NON-NLS-1$ 
		}
		else if (persist instanceof Relation)
		{
			elementName = "Relation";//$NON-NLS-1$ 
		}
		else if (persist instanceof ValueList)
		{
			elementName = "Valuelist";//$NON-NLS-1$ 
		}
		else
		{
			elementName = "Element";//$NON-NLS-1$ 
		}
		String name = null;
		if (persist instanceof ISupportName) name = ((ISupportName)persist).getName();
		StringTokenizer tk = new StringTokenizer(sortOptions, ",");//$NON-NLS-1$ 
		while (tk.hasMoreTokens())
		{
			String columnName = null;
			String def = tk.nextToken().trim();
			int index = def.indexOf(" "); //$NON-NLS-1$
			if (index != -1)
			{
				columnName = def.substring(0, index);
			}
			else
			{
				columnName = def;
			}
			try
			{
				Table lastTable = table;
				String[] split = columnName.split("\\."); //$NON-NLS-1$
				for (int i = 0; i < split.length - 1; i++)
				{
					Relation relation = flattenedSolution.getRelation(split[i]);
					if (relation == null)
					{
						String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Sort_InvalidSortOptionsRelationNotFound, elementName, name, sortOptions,
							split[i]);
						problems.add(new Problem(INVALID_SORT_OPTION, IMarker.SEVERITY_WARNING, msg));
						lastTable = null;
						break;
					}
					else
					{
						if (!lastTable.equals(relation.getPrimaryTable()))
						{
							String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Sort_InvalidSortOptionsRelationDifferentPrimaryDatasource,
								elementName, name, sortOptions, relation.getName());
							problems.add(new Problem(INVALID_SORT_OPTION, IMarker.SEVERITY_WARNING, msg));
						}
						lastTable = relation.getForeignTable();
					}
				}
				if (lastTable != null)
				{
					String colName = split[split.length - 1];
					Column c = lastTable.getColumn(colName);
					if (c == null)
					{
						String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Sort_InvalidSortOptionsColumnNotFound, elementName, name, sortOptions,
							colName);
						problems.add(new Problem(INVALID_SORT_OPTION, IMarker.SEVERITY_WARNING, msg));
					}
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
		return problems;
	}

	private void checkRelations(IProject project, Map<String, IPersist> missingServers)
	{
		ServoyProject servoyProject = servoyModel.getServoyProject(project.getName());
		if (servoyProject != null)
		{
			String message = null;

			IServerManagerInternal sm = ServoyModel.getServerManager();
			Iterator<Relation> it = servoyProject.getSolution().getRelations(false);
			while (it.hasNext())
			{
				checkCancel();
				Relation element = it.next();
				if (!missingServers.containsKey(element.getPrimaryServerName()) && !missingServers.containsKey(element.getForeignServerName()))
				{
					element.setValid(true);//if is reload
					try
					{
						IServerInternal pserver = (IServerInternal)sm.getServer(element.getPrimaryServerName());
						if (pserver == null)
						{
							message = MarkerMessages.getMessage(MarkerMessages.Marker_Relation_PrimaryServerWithProblems, element.getName(),
								element.getPrimaryServerName());
							element.setValid(false);
							addMarker(project, PROJECT_RELATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}
						else
						{
							if (!pserver.getName().equals(element.getPrimaryServerName()))
							{
								message = MarkerMessages.getMessage(MarkerMessages.Marker_Relation_PrimaryServerDuplicate, element.getName(),
									element.getPrimaryServerName());
								addMarker(project, PROJECT_RELATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, element);
							}
						}
						ITable ptable = pserver.getTable(element.getPrimaryTableName());
						if (ptable == null)
						{
							message = MarkerMessages.getMessage(MarkerMessages.Marker_Relation_PrimaryTableNotFound, element.getName(),
								element.getPrimaryTableName(), element.getPrimaryServerName());
							element.setValid(false);
							addMarker(project, PROJECT_RELATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}
						else if (((Table)ptable).getRowIdentColumnsCount() == 0)
						{
							message = MarkerMessages.getMessage(MarkerMessages.Marker_Relation_PrimaryTableWithoutPK, element.getName(),
								element.getPrimaryTableName(), element.getPrimaryServerName());
							element.setValid(false);
							addMarker(project, PROJECT_RELATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}

						IServerInternal fserver = (IServerInternal)sm.getServer(element.getForeignServerName());
						if (fserver == null)
						{
							message = MarkerMessages.getMessage(MarkerMessages.Marker_Relation_ForeignServerWithProblems, element.getName(),
								element.getForeignServerName());
							element.setValid(false);
							addMarker(project, PROJECT_RELATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}
						else if (!fserver.getName().equals(element.getForeignServerName()))
						{
							message = MarkerMessages.getMessage(MarkerMessages.Marker_Relation_ForeignServerDuplicate, element.getName(),
								element.getForeignServerName());
							addMarker(project, PROJECT_RELATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, element);
						}

						ITable ftable = fserver.getTable(element.getForeignTableName());
						if (ftable == null)
						{
							message = MarkerMessages.getMessage(MarkerMessages.Marker_Relation_ForeignTableNotFound, element.getName(),
								element.getForeignTableName(), element.getForeignServerName());
							element.setValid(false);
							addMarker(project, PROJECT_RELATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}
						else if (((Table)ftable).getRowIdentColumnsCount() == 0)
						{
							message = MarkerMessages.getMessage(MarkerMessages.Marker_Relation_ForeignTableWithoutPK, element.getName(),
								element.getForeignTableName(), element.getForeignServerName());
							element.setValid(false);
							addMarker(project, PROJECT_RELATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}

						if (!element.isParentRef() && element.getSize() == 0)
						{
							message = MarkerMessages.getMessage(MarkerMessages.Marker_Relation_Empty, element.getName());
							element.setValid(false);
							addMarker(project, PROJECT_RELATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}
						if (element.getInitialSort() != null)
						{
							addMarkers(project, checkSortOptions((Table)ftable, element.getInitialSort(), element, servoyModel.getFlattenedSolution()), element);
						}
						Iterator<RelationItem> items = element.getObjects(IRepository.RELATION_ITEMS);
						boolean errorsFound = false;
						while (items.hasNext())
						{
							RelationItem item = items.next();
							String primaryDataProvider = item.getPrimaryDataProviderID();
							String foreignColumn = item.getForeignColumnName();
							IDataProvider dataProvider = null;
							IDataProvider column = null;
							if (primaryDataProvider == null || "".equals(primaryDataProvider))//$NON-NLS-1$ 
							{
								message = MarkerMessages.getMessage(MarkerMessages.Marker_Relation_ItemNoPrimaryDataprovider, element.getName());
								errorsFound = true;
								addMarker(project, PROJECT_RELATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
							}
							else
							{
								if (primaryDataProvider.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
								{
									dataProvider = servoyModel.getFlattenedSolution().getGlobalDataProvider(primaryDataProvider);
								}
								else
								{
									dataProvider = servoyModel.getFlattenedSolution().getDataProviderForTable((Table)ptable, primaryDataProvider);
								}
								if (dataProvider == null)
								{
									message = MarkerMessages.getMessage(MarkerMessages.Marker_Relation_ItemPrimaryDataproviderNotFound, element.getName(),
										primaryDataProvider);
									errorsFound = true;
									addMarker(project, PROJECT_RELATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null,
										element);
								}
							}
							if (foreignColumn == null || "".equals(foreignColumn))//$NON-NLS-1$ 
							{
								message = MarkerMessages.getMessage(MarkerMessages.Marker_Relation_ItemNoForeignDataprovider, element.getName());
								errorsFound = true;
								addMarker(project, PROJECT_RELATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
							}
							else
							{
								column = servoyModel.getFlattenedSolution().getDataProviderForTable((Table)ftable, foreignColumn);
								if (column == null)
								{
									message = MarkerMessages.getMessage(MarkerMessages.Marker_Relation_ItemForeignDataproviderNotFound, element.getName(),
										foreignColumn);
									errorsFound = true;
									addMarker(project, PROJECT_RELATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null,
										element);
								}
							}
							if (dataProvider != null && column != null && dataProvider instanceof Column && column instanceof Column &&
								((Column)dataProvider).getColumnInfo() != null && ((Column)column).getColumnInfo() != null)
							{
								boolean primaryColumnUuidFlag = ((Column)dataProvider).getColumnInfo().hasFlag(Column.UUID_COLUMN);
								boolean foreignColumnUuidFlag = ((Column)column).getColumnInfo().hasFlag(Column.UUID_COLUMN);
								if ((primaryColumnUuidFlag && !foreignColumnUuidFlag) || (!primaryColumnUuidFlag && foreignColumnUuidFlag))
								{
									message = MarkerMessages.getMessage(MarkerMessages.Marker_Relation_ItemUUIDProblem, element.getName(), primaryDataProvider,
										foreignColumn);
									errorsFound = true;
									addMarker(project, PROJECT_RELATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null,
										element);
								}
							}
						}
						if (errorsFound)
						{
							element.setValid(false);
							continue;
						}
						if (servoyModel.getActiveProject() == servoyProject)
						{
							String typeMismatchWarning = element.checkKeyTypes(servoyModel.getFlattenedSolution());
							if (typeMismatchWarning != null)
							{
								message = MarkerMessages.getMessage(MarkerMessages.Marker_Relation_ItemTypeProblem, element.getName(), typeMismatchWarning);
								addMarker(project, PROJECT_RELATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, element);
							}
						}
					}
					catch (Exception ex)
					{
						exceptionCount++;
						if (exceptionCount < MAX_EXCEPTIONS) ServoyLog.logError(ex);
						element.setValid(false);
						message = MarkerMessages.getMessage(MarkerMessages.Marker_Relation_GenericError, element.getName());
						if (ex.getMessage() != null) message += " " + ex.getMessage(); //$NON-NLS-1$
						addMarker(project, PROJECT_RELATION_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
					}
				}
			}
		}
	}

	private void checkResourcesForServoyProject(IProject project)
	{
		deleteMarkers(project, MULTIPLE_RESOURCES_PROJECTS_MARKER_TYPE);
		deleteMarkers(project, NO_RESOURCES_PROJECTS_MARKER_TYPE);
		try
		{
			// check if this project references more than one or no resources projects
			final IProject[] referencedProjects = project.getReferencedProjects();
			int count = 0;
			for (IProject p : referencedProjects)
			{
				if (p.exists() && p.isOpen() && p.hasNature(ServoyResourcesProject.NATURE_ID))
				{
					count++;
				}
			}

			if (count > 1)
			{
				// > 1 => multiple referenced resources projects; error; quick fix would be choose one of them
				String message = MarkerMessages.getMessage(MarkerMessages.Marker_Resources_ReferencesToMultipleResources, project.getName());
				addMarker(project, MULTIPLE_RESOURCES_PROJECTS_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, null);
			}
			else if (count == 0)
			{
				// 0 => no referenced resources projects; error; quick fix would be choose one of the resources projects in the work space
				String message = MarkerMessages.getMessage(MarkerMessages.Marker_Resources_NoResourceReference, project.getName());
				addMarker(project, NO_RESOURCES_PROJECTS_MARKER_TYPE, message, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, null);
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError("Exception while reading referenced projects for " + project.getName(), e); //$NON-NLS-1$
		}
	}

	void checkXML(IFile file)
	{
		deleteMarkers(file, XML_MARKER_TYPE);
		XMLErrorHandler reporter = new XMLErrorHandler(file);
		try
		{
			getParser().parse(file.getContents(true), reporter);
		}
		catch (Exception e)
		{
		}
	}

	public static void addMarkers(IResource resource, List<Problem> problems, IPersist persist)
	{
		if (problems != null)
		{
			for (Problem problem : problems)
			{
				addMarker(resource, problem.type, problem.message, -1, problem.severity, IMarker.PRIORITY_NORMAL, null, persist);
			}
		}
	}

	public static IMarker addMarker(IResource resource, String type, String message, int lineNumber, int severity, int priority, String location,
		IPersist persist)
	{
		try
		{
			IMarker marker = null;
			if (persist != null)
			{
				Pair<String, String> pathPair = SolutionSerializer.getFilePath(persist, true);
				Path path = new Path(pathPair.getLeft() + pathPair.getRight());
				IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				if (file.exists())
				{
					marker = file.createMarker(type);
				}
				else
				{
					marker = resource.createMarker(type);
				}
				if (location == null)
				{
					marker.setAttribute(IMarker.LOCATION, path.toString());
				}
			}
			else
			{
				marker = resource.createMarker(type);
			}
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			marker.setAttribute(IMarker.PRIORITY, priority);
			int lineNmbr = lineNumber;
			if (lineNmbr == -1)
			{
				if (persist instanceof IVariable)
				{
					Integer line = ((AbstractBase)persist).getSerializableRuntimeProperty(IScriptProvider.LINENUMBER);
					lineNmbr = line != null ? line.intValue() : -1;
				}
				else if (persist instanceof IScriptProvider)
				{
					lineNmbr = ((IScriptProvider)persist).getLineNumberOffset();
				}
			}
			if (lineNmbr > 0)
			{
				marker.setAttribute(IMarker.LINE_NUMBER, lineNmbr);
				if (location == null)
				{
					marker.setAttribute(IMarker.LOCATION, "Line " + lineNmbr); //$NON-NLS-1$
				}
			}
			if (location != null)
			{
				marker.setAttribute(IMarker.LOCATION, location);
			}
			if (type.equals(PROJECT_RELATION_MARKER_TYPE))
			{
				String id = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
					Platform.getContentTypeManager().getContentType(PersistEditorInput.RELATION_RESOURCE_ID)).getId();
				marker.setAttribute(IDE.EDITOR_ID_ATTR, id);
			}
			else if (type.equals(PROJECT_FORM_MARKER_TYPE))
			{
				String id = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
					Platform.getContentTypeManager().getContentType(PersistEditorInput.FORM_RESOURCE_ID)).getId();
				marker.setAttribute(IDE.EDITOR_ID_ATTR, id);
				if (persist != null)
				{
					marker.setAttribute("elementUuid", persist.getUUID().toString()); //$NON-NLS-1$
				}
			}
			else if (type.equals(PROJECT_VALUELIST_MARKER_TYPE))
			{
				String id = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
					Platform.getContentTypeManager().getContentType(PersistEditorInput.VALUELIST_RESOURCE_ID)).getId();
				marker.setAttribute(IDE.EDITOR_ID_ATTR, id);
			}
			else if (type.equals(INVALID_TABLE_NODE_PROBLEM))
			{
				marker.setAttribute("Uuid", persist.getUUID().toString()); //$NON-NLS-1$
				marker.setAttribute("Name", ((ISupportName)persist).getName()); //$NON-NLS-1$
				marker.setAttribute("SolutionName", resource.getName()); //$NON-NLS-1$
			}
			else if (type.equals(DUPLICATE_UUID) || type.equals(DUPLICATE_SIBLING_UUID) || type.equals(BAD_STRUCTURE_MARKER_TYPE) ||
				type.equals(INVALID_SORT_OPTION) || type.equals(EVENT_METHOD_MARKER_TYPE) || type.equals(PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE) ||
				type.equals(INVALID_EVENT_METHOD) || type.equals(MISSING_STYLE))
			{
				marker.setAttribute("Uuid", persist.getUUID().toString()); //$NON-NLS-1$
				marker.setAttribute("SolutionName", resource.getName()); //$NON-NLS-1$
			}
			else if (type.equals(DUPLICATE_NAME_MARKER_TYPE))
			{
				if (persist instanceof Relation || persist instanceof Form)
				{
					marker.setAttribute("Uuid", persist.getUUID().toString()); //$NON-NLS-1$
					marker.setAttribute("SolutionName", persist.getRootObject().getName()); //$NON-NLS-1$
				}
			}

			return marker;
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Cannot create problem marker", e); //$NON-NLS-1$
		}
		return null;
	}

	public static IMarker addMarker(IResource resource, String type, String message, int charNumber, int severity, int priority, String location)
	{
		try
		{
			IMarker marker = resource.createMarker(type);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			marker.setAttribute(IMarker.PRIORITY, priority);
			if (charNumber > 0)
			{
				marker.setAttribute(IMarker.CHAR_START, charNumber);
				marker.setAttribute(IMarker.CHAR_END, charNumber);
				if (location == null)
				{
					marker.setAttribute(IMarker.LOCATION, "Character " + charNumber); //$NON-NLS-1$
				}
			}
			if (location != null)
			{
				marker.setAttribute(IMarker.LOCATION, location);
			}
			return marker;
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Cannot create problem marker", e); //$NON-NLS-1$
		}
		return null;
	}

	public static void deleteMarkers(IResource file, String type)
	{
		try
		{
			file.deleteMarkers(type, true, IResource.DEPTH_INFINITE);
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Cannot delete problem marker", e); //$NON-NLS-1$
		}
	}

	public static void deleteAllMarkers(IResource file)
	{
		try
		{
			file.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Cannot delete problem marker", e); //$NON-NLS-1$
		}
	}

	public static void deleteAllBuilderMarkers()
	{
		try
		{
			ServoyModel.getWorkspace().getRoot().deleteMarkers(SERVOY_BUILDER_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Cannot delete problem marker", e); //$NON-NLS-1$
		}
	}

	private SAXParser getParser() throws ParserConfigurationException, SAXException
	{
		if (parserFactory == null)
		{
			parserFactory = SAXParserFactory.newInstance();
		}
		return parserFactory.newSAXParser();
	}

	protected void fullBuild(final IProgressMonitor progressMonitor)
	{
		try
		{
			this.monitor = progressMonitor;
			getProject().accept(new ServoyResourceVisitor());
			this.monitor = null;
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Full Servoy build failed", e); //$NON-NLS-1$
		}
	}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor progressMonitor) throws CoreException
	{
		// the visitor does the work.
		this.monitor = progressMonitor;
		delta.accept(new ServoyDeltaVisitor());
		this.monitor = null;
	}

	protected void checkCancel()
	{
		if (monitor != null && monitor.isCanceled())
		{
			forgetLastBuiltState();
			throw new OperationCanceledException();
		}
	}

	/**
	 * Container class for problem and optional fix.
	 * 
	 * @author rob
	 *
	 */
	public static class Problem
	{
		public final String type;
		public final int severity;
		public final String message;
		public final String fix;

		public Problem(String type, int severity, String message, String fix)
		{
			this.type = type;
			this.severity = severity;
			this.message = message;
			this.fix = fix;
		}

		public Problem(String type, int severity, String message)
		{
			this(type, severity, message, null);
		}
	}

}
