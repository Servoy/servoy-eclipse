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
package com.servoy.eclipse.model.builder;

import java.awt.Point;
import java.awt.print.PageFormat;
import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
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
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.MarkerMessages.ServoyMarker;
import com.servoy.eclipse.model.extensions.IMarkerAttributeContributor;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ResourcesUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.dataprocessing.DBValueList;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractScriptProvider;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.DataSourceCollectorVisitor;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.FlattenedPortal;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportScope;
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
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser;
import com.servoy.j2db.util.FormatParser.ParsedFormat;
import com.servoy.j2db.util.IntHashMap;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.RoundHalfUpDecimalFormat;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * Builds Servoy projects. Adds problem markers where needed.
 */
public class ServoyBuilder extends IncrementalProjectBuilder
{
	public static int MAX_EXCEPTIONS = 25;
	public static int MIN_FIELD_LENGTH = 1000;
	public static int exceptionCount = 0;

	private static final int LIMIT_FOR_PORTAL_TABPANEL_COUNT_ON_FORM = 3;
	private static final int LIMIT_FOR_FIELD_COUNT_ON_TABLEVIEW_FORM = 20;

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

	static class XMLErrorHandler extends DefaultHandler
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
	private static final String _PREFIX = "com.servoy.eclipse.core"; //$NON-NLS-1$
	public static final String SERVOY_MARKER_TYPE = _PREFIX + ".servoyProblem"; //$NON-NLS-1$
	public static final String SERVOY_BUILDER_MARKER_TYPE = _PREFIX + ".builderProblem"; //$NON-NLS-1$

	public static final String XML_MARKER_TYPE = _PREFIX + ".xmlProblem"; //$NON-NLS-1$
	public static final String PROJECT_DESERIALIZE_MARKER_TYPE = _PREFIX + ".deserializeProblem"; //$NON-NLS-1$
	public static final String SOLUTION_PROBLEM_MARKER_TYPE = _PREFIX + ".solutionProblem"; //$NON-NLS-1$
	public static final String BAD_STRUCTURE_MARKER_TYPE = _PREFIX + ".badStructure"; //$NON-NLS-1$
	public static final String MISSING_MODULES_MARKER_TYPE = _PREFIX + ".missingModulesProblem"; //$NON-NLS-1$
	public static final String MISPLACED_MODULES_MARKER_TYPE = _PREFIX + ".misplacedModulesProblem"; //$NON-NLS-1$
	public static final String MULTIPLE_RESOURCES_PROJECTS_MARKER_TYPE = _PREFIX + ".multipleResourcesProblem"; //$NON-NLS-1$
	public static final String NO_RESOURCES_PROJECTS_MARKER_TYPE = _PREFIX + ".noResourcesProblem"; //$NON-NLS-1$
	public static final String DIFFERENT_RESOURCES_PROJECTS_MARKER_TYPE = _PREFIX + ".differentResourcesProblem"; //$NON-NLS-1$
	public static final String PROJECT_RELATION_MARKER_TYPE = _PREFIX + ".relationProblem"; //$NON-NLS-1$
	public static final String MEDIA_MARKER_TYPE = _PREFIX + ".mediaProblem"; //$NON-NLS-1$
	public static final String CALCULATION_MARKER_TYPE = _PREFIX + ".calculationProblem"; //$NON-NLS-1$
	public static final String SCRIPT_MARKER_TYPE = _PREFIX + ".scriptProblem"; //$NON-NLS-1$
	public static final String EVENT_METHOD_MARKER_TYPE = _PREFIX + ".eventProblem"; //$NON-NLS-1$
	public static final String USER_SECURITY_MARKER_TYPE = _PREFIX + ".userSecurityProblem"; //$NON-NLS-1$
	public static final String DATABASE_INFORMATION_MARKER_TYPE = _PREFIX + ".databaseInformationProblem"; //$NON-NLS-1$
	public static final String PROJECT_FORM_MARKER_TYPE = _PREFIX + ".formProblem"; //$NON-NLS-1$
	public static final String INVALID_TABLE_NODE_PROBLEM = _PREFIX + ".invalidTableNodeProblem"; //$NON-NLS-1$
	public static final String PROJECT_VALUELIST_MARKER_TYPE = _PREFIX + ".valuelistProblem"; //$NON-NLS-1$
	public static final String DUPLICATE_UUID = _PREFIX + ".duplicateUUID"; //$NON-NLS-1$
	public static final String DUPLICATE_SIBLING_UUID = _PREFIX + ".duplicateSiblingUUID"; //$NON-NLS-1$
	public static final String DUPLICATE_NAME_MARKER_TYPE = _PREFIX + ".duplicateNameProblem"; //$NON-NLS-1$
	public static final String INVALID_SORT_OPTION = _PREFIX + ".invalidSortOption"; //$NON-NLS-1$
	public static final String PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE = _PREFIX + ".differentRelationName"; //$NON-NLS-1$
	public static final String MISSING_SERVER = _PREFIX + ".missingServer"; //$NON-NLS-1$
	public static final String MISSING_STYLE = _PREFIX + ".missingStyle"; //$NON-NLS-1$
	public static final String I18N_MARKER_TYPE = _PREFIX + ".i18nProblem"; //$NON-NLS-1$
	public static final String COLUMN_MARKER_TYPE = _PREFIX + ".columnProblem"; //$NON-NLS-1$
	public static final String INVALID_EVENT_METHOD = _PREFIX + ".invalidEventMethod"; //$NON-NLS-1$
	public static final String INVALID_COMMAND_METHOD = _PREFIX + ".invalidCommandMethod"; //$NON-NLS-1$
	public static final String INVALID_DATAPROVIDERID = _PREFIX + ".invalidDataProviderID"; //$NON-NLS-1$
	public static final String DEPRECATED_PROPERTY_USAGE = _PREFIX + ".deprecatedPropertyUsage"; //$NON-NLS-1$
	public static final String FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION = _PREFIX + ".formWithDatasourceInLoginSolution"; //$NON-NLS-1$
	public static final String MULTIPLE_METHODS_ON_SAME_ELEMENT = _PREFIX + ".multipleMethodsInfo"; //$NON-NLS-1$
	public static final String UNRESOLVED_RELATION_UUID = _PREFIX + ".unresolvedRelationUuid"; //$NON-NLS-1$
	public static final String CONSTANTS_USED_MARKER_TYPE = _PREFIX + ".constantsUsed"; //$NON-NLS-1$
	public static final String MISSING_DRIVER = _PREFIX + ".missingDriver"; //$NON-NLS-1$
	public static final String OBSOLETE_ELEMENT = _PREFIX + ".obsoleteElement"; //$NON-NLS-1$
	public static final String HIDDEN_TABLE_STILL_IN_USE = _PREFIX + ".hiddenTableInUse"; //$NON-NLS-1$

	private SAXParserFactory parserFactory;
	private final HashSet<String> referencedProjectsSet = new HashSet<String>();
	private final HashSet<String> moduleProjectsSet = new HashSet<String>();
	private IServoyModel servoyModel;
	private static IMarkerAttributeContributor[] markerContributors;

	private IProgressMonitor monitor;

	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor progressMonitor) throws CoreException
	{
		// make sure the IServoyModel is initialized
		getServoyModel();

		referencedProjectsSet.clear();
		moduleProjectsSet.clear();

		IProject[] referencedProjects = getProject().getReferencedProjects();
		ArrayList<IProject> moduleAndModuleReferencedProjects = null;
		// we are interested in showing module error markers only if the project is in use (active prj or active module)
		if (servoyModel.isSolutionActive(getProject().getName()))
		{
			ServoyProject sp = getServoyProject(getProject());
			if (sp != null)
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
							moduleProject = ResourcesPlugin.getWorkspace().getRoot().getProject(moduleName);
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
					// or something has changed in this builder's solution project
					checkServoyProject(getProject());
					checkModules(getProject());
					checkResourcesForServoyProject(getProject());
					checkResourcesForModules(getProject());
					if (project.exists())
					{
						if (servoyModel.isSolutionActive(project.getName()))
						{
							checkColumns(project);
						}
					}
				}
				else
				{
					if (project.isOpen() && project.hasNature(ServoyResourcesProject.NATURE_ID))
					{
						if (servoyModel.isSolutionActive(getProject().getName()))
						{
							checkStyles(getProject());
							checkColumns(getProject());

							deleteMarkers(getProject(), HIDDEN_TABLE_STILL_IN_USE);
							deleteMarkers(getProject(), I18N_MARKER_TYPE);
							checkI18n(getProject()); // maybe hidden tables changed
							deleteMarkers(getProject(), PROJECT_RELATION_MARKER_TYPE);
							checkRelations(getProject(), new HashMap<String, IPersist>());
						}
						IProject[] projects = project.getReferencingProjects();
						if (projects != null)
						{
							for (IProject p : projects)
							{
								if (servoyModel.isSolutionActive(p.getName()) && getProject() != p)
								{
									deleteMarkers(p, PROJECT_RELATION_MARKER_TYPE);
									deleteMarkers(p, HIDDEN_TABLE_STILL_IN_USE);
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
		boolean active = servoyModel.isSolutionActive(project.getName());

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
						ServoyProject module = getServoyModel().getServoyProject(name);
						if (module != null)
						{
							moduleResourcesProject = module.getResourcesProject();
							if (moduleResourcesProject != null && (!moduleResourcesProject.equals(resourcesProject)))
							{
								// this module has a resources project different than the one of the main solution
								ServoyMarker mk = MarkerMessages.ModuleDifferentResourceProject.fill(name, project.getName());
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, null);
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
		deleteMarkers(project, MISPLACED_MODULES_MARKER_TYPE);

		final ServoyProject servoyProject = getServoyProject(project);
		boolean active = servoyModel.isSolutionActive(project.getName());

		if (servoyProject != null && active && servoyProject.getSolution() != null)
		{
			// check if all modules are checked out
			String[] modulesNames = Utils.getTokenElements(servoyProject.getSolution().getModulesNames(), ",", true); //$NON-NLS-1$
			if (modulesNames != null)
			{
				for (String name : modulesNames)
				{
					ServoyProject module = getServoyModel().getServoyProject(name);
					if (module == null)
					{
						ServoyMarker mk = MarkerMessages.ModuleNotFound.fill(name, servoyProject.getSolution().getName());
						IMarker marker = addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH, null, null);
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

				// import hook modules should not contain other modules
				if (SolutionMetaData.isImportHook(servoyProject.getSolution().getSolutionMetaData()) && modulesNames.length > 0)
				{
					String message = "Module " + servoyProject.getSolution().getName() + " is a solution import hook, so it should not contain any modules."; //$NON-NLS-1$//$NON-NLS-2$
					addMarker(project, MISPLACED_MODULES_MARKER_TYPE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, null);
				}
			}
		}
	}

	private static final Integer METHOD_DUPLICATION = Integer.valueOf(1);
	private static final Integer FORM_DUPLICATION = Integer.valueOf(2);
	private static final Integer RELATION_DUPLICATION = Integer.valueOf(3);
	private static final Integer VALUELIST_DUPLICATION = Integer.valueOf(4);
	private static final Integer MEDIA_DUPLICATION = Integer.valueOf(5);

	private void addDuplicatePersist(final IPersist persist, Map<String, Map<Integer, Set<Pair<String, ISupportChilds>>>> duplicationMap, final IProject project)
	{
		if (persist instanceof IScriptProvider || persist instanceof ScriptVariable)
		{
			String name = ((ISupportName)persist).getName();
			if (name != null)
			{
				String scopeName = null;
				if (persist instanceof ISupportScope && persist.getParent() instanceof Solution)
				{
					scopeName = ((ISupportScope)persist).getScopeName();
					if (scopeName == null) scopeName = ScriptVariable.GLOBAL_SCOPE;
				}
				List<Pair<String, ISupportChilds>> duplicatedParents = new ArrayList<Pair<String, ISupportChilds>>(3);
				Map<Integer, Set<Pair<String, ISupportChilds>>> persistSet = duplicationMap.get(name);
				if (persistSet == null)
				{
					persistSet = new HashMap<Integer, Set<Pair<String, ISupportChilds>>>();
					duplicationMap.put(name, persistSet);
				}
				Set<Pair<String, ISupportChilds>> parentScopeSet = persistSet.get(METHOD_DUPLICATION);
				Pair<String, ISupportChilds> scopedParent = new Pair<String, ISupportChilds>(scopeName, persist.getParent());
				if (parentScopeSet != null && parentScopeSet.contains(scopedParent))
				{
					duplicatedParents.add(scopedParent);
				}
				else if (parentScopeSet != null && persist.getParent() instanceof Solution)
				{
					for (Pair<String, ISupportChilds> supportChilds : parentScopeSet)
					{
						if (supportChilds.getRight() instanceof Solution && (scopeName == null || scopeName.equals(supportChilds.getLeft())))
						{
							duplicatedParents.add(supportChilds);
						}
					}
				}

				for (Pair<String, ISupportChilds> duplicatedParent : duplicatedParents)
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
					Iterator<IPersist> allObjects = duplicatedParent.getRight().getAllObjects();
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
							ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill(type, name, parentsName);
							addMarker(project, mk.getType(), mk.getText(), lineNumber == null ? -1 : lineNumber.intValue(), severity, IMarker.PRIORITY_NORMAL,
								null, child);
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
						lineNumber = Integer.valueOf(((AbstractScriptProvider)persist).getLineNumberOffset());
					}
					ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill(otherChildsType, name, duplicateParentsName);
					addMarker(project, mk.getType(), mk.getText(), lineNumber == null ? -1 : lineNumber.intValue(), severity, IMarker.PRIORITY_NORMAL, null,
						persist);

				}
				Set<Pair<String, ISupportChilds>> parents = parentScopeSet;
				if (parents == null)
				{
					parents = new HashSet<Pair<String, ISupportChilds>>();
					persistSet.put(METHOD_DUPLICATION, parents);
				}
				parents.add(new Pair<String, ISupportChilds>(scopeName, persist.getParent()));
			}
		}
		if (persist instanceof Form)
		{
			String name = ((ISupportName)persist).getName();
			if (name != null)
			{
				Map<Integer, Set<Pair<String, ISupportChilds>>> persistSet = duplicationMap.get(name);
				if (persistSet == null)
				{
					persistSet = new HashMap<Integer, Set<Pair<String, ISupportChilds>>>();
					duplicationMap.put(name, persistSet);
				}
				Set<Pair<String, ISupportChilds>> parentSet = persistSet.get(FORM_DUPLICATION);
				if (parentSet != null)
				{
					String parentsName = ""; //$NON-NLS-1$
					if (persist.getParent() instanceof ISupportName)
					{
						parentsName = ((ISupportName)persist.getParent()).getName();
					}
					for (Pair<String, ISupportChilds> parent : parentSet)
					{
						if (parent.getRight() instanceof Solution)
						{
							Solution solution = (Solution)parent.getRight();
							Form duplicateForm = solution.getForm(name);
							if (duplicateForm != null)
							{
								ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("form", name, parentsName); //$NON-NLS-1$
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, duplicateForm);
							}
							ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("form", name, solution.getName()); //$NON-NLS-1$
							addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, persist);
						}
					}
				}
				Set<Pair<String, ISupportChilds>> parents = parentSet;
				if (parents == null)
				{
					parents = new HashSet<Pair<String, ISupportChilds>>();
					persistSet.put(FORM_DUPLICATION, parents);
				}
				parents.add(new Pair<String, ISupportChilds>(null, persist.getParent()));
			}
			final Map<String, Set<IPersist>> formElementsByName = new HashMap<String, Set<IPersist>>();
			persist.acceptVisitor(new IPersistVisitor()
			{
				public Object visit(IPersist o)
				{
					if (!(o instanceof ScriptVariable) && !(o instanceof ScriptMethod) && !(o instanceof Form) && o instanceof ISupportName &&
						((ISupportName)o).getName() != null)
					{
						Set<IPersist> duplicates = formElementsByName.get(((ISupportName)o).getName());
						if (duplicates != null)
						{
							for (IPersist duplicatePersist : duplicates)
							{
								ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill(
									"form element", ((ISupportName)o).getName(), ((Form)persist).getName()); //$NON-NLS-1$
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, duplicatePersist);

								mk = MarkerMessages.DuplicateEntityFound.fill("form element", ((ISupportName)o).getName(), ((Form)persist).getName()); //$NON-NLS-1$								
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, o);
							}
						}
						else
						{
							duplicates = new HashSet<IPersist>();
							duplicates.add(o);
						}
						formElementsByName.put(((ISupportName)o).getName(), duplicates);
					}
					if (o instanceof Form) return IPersistVisitor.CONTINUE_TRAVERSAL;
					else return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
				}
			});
		}
		if (persist instanceof Relation || persist instanceof ValueList || persist instanceof Media)
		{
			String name = ((ISupportName)persist).getName();
			if (name != null)
			{
				Map<Integer, Set<Pair<String, ISupportChilds>>> persistSet = duplicationMap.get(name);
				if (persistSet == null)
				{
					persistSet = new HashMap<Integer, Set<Pair<String, ISupportChilds>>>();
					duplicationMap.put(name, persistSet);
				}
				Integer type = RELATION_DUPLICATION;
				if (persist instanceof ValueList)
				{
					type = VALUELIST_DUPLICATION;
				}
				else if (persist instanceof Media)
				{
					type = MEDIA_DUPLICATION;
				}
				Set<Pair<String, ISupportChilds>> parentSet = persistSet.get(type);
				if (parentSet != null)
				{
					String parentsName = ""; //$NON-NLS-1$
					if (persist.getParent() instanceof ISupportName)
					{
						parentsName = ((ISupportName)persist.getParent()).getName();
					}
					for (Pair<String, ISupportChilds> parent : parentSet)
					{
						if (parent.getRight() instanceof Solution)
						{
							Solution solution = (Solution)parent.getRight();
							if (persist instanceof Relation)
							{
								Relation duplicateRelation = solution.getRelation(name);
								if (!((Relation)persist).contentEquals(duplicateRelation))
								{
									if (duplicateRelation != null)
									{
										ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("relation", name, parentsName); //$NON-NLS-1$
										addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null,
											duplicateRelation);
									}
									ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("relation", name, solution.getName()); //$NON-NLS-1$								
									addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, persist);
								}
							}
							else if (persist instanceof ValueList)
							{
								ValueList duplicateValuelist = solution.getValueList(name);
								if (duplicateValuelist != null)
								{
									ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("valuelist", name, parentsName); //$NON-NLS-1$
									addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null,
										duplicateValuelist);
								}
								ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("valuelist", name, solution.getName()); //$NON-NLS-1$								
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, persist);
							}
							else if (persist instanceof Media)
							{
								Media duplicateMedia = solution.getMedia(name);
								if (duplicateMedia != null)
								{
									ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("media", name, parentsName); //$NON-NLS-1$
									addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, duplicateMedia);
								}
								ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("media", name, solution.getName()); //$NON-NLS-1$								
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, persist);
							}
						}
					}
				}
				Set<Pair<String, ISupportChilds>> parents = parentSet;
				if (parents == null)
				{
					parents = new HashSet<Pair<String, ISupportChilds>>();
					persistSet.put(type, parents);
				}
				parents.add(new Pair<String, ISupportChilds>(null, persist.getParent()));
			}
		}
	}

	private void checkPersistDuplication()
	{
		// this is a special case
		ServoyProject[] modules = getServoyModel().getModulesOfActiveProject();
		final Map<String, Map<Integer, Set<Pair<String, ISupportChilds>>>> duplicationMap = new HashMap<String, Map<Integer, Set<Pair<String, ISupportChilds>>>>();
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
						addDeprecatedPropertyUsageMarker(persist, project, StaticContentSpecLoader.PROPERTY_LOGINFORMID.getPropertyName(), "Solution '" +
							solution.getName() + "' has a loginForm property set which is overridden by the loginSolutionName property.");
					}
					else if (solution.getSolutionType() != SolutionMetaData.WEB_CLIENT_ONLY)
					{
						// loginForm is deprecated
						addDeprecatedPropertyUsageMarker(persist, project, StaticContentSpecLoader.PROPERTY_LOGINFORMID.getPropertyName(),
							"Solution '" + solution.getName() + "' has a loginForm property set which is deprecated, use loginSolutionName property instead."); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}

		if (persist instanceof Form || persist instanceof Portal)
		{
			String rowBgColorCalculation = null;
			String type = "Form"; //$NON-NLS-1$
			if (persist instanceof Form)
			{
				rowBgColorCalculation = ((Form)persist).getRowBGColorCalculation();
			}
			else
			{
				rowBgColorCalculation = ((Portal)persist).getRowBGColorCalculation();
				type = "Portal";//$NON-NLS-1$
			}
			if (rowBgColorCalculation != null)
			{
				try
				{
					addDeprecatedPropertyUsageMarker(
						persist,
						project,
						StaticContentSpecLoader.PROPERTY_ROWBGCOLORCALCULATION.getPropertyName(),
						type +
							" '" + ((ISupportName)persist).getName() + "' has rowBGColorCalculation property set which is deprecated, use CSS (odd/even/selected) or onRender event instead."); //$NON-NLS-1$ //$NON-NLS-2$
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}
	}

	private void addDeprecatedPropertyUsageMarker(IPersist persist, IProject project, String propertyName, String message) throws CoreException
	{
		if (message != null)
		{
			IMarker marker = addMarker(project, DEPRECATED_PROPERTY_USAGE, message, -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, persist);
			if (marker != null)
			{
				marker.setAttribute("Uuid", persist.getUUID().toString()); //$NON-NLS-1$
				marker.setAttribute("SolutionName", project.getName()); //$NON-NLS-1$
				marker.setAttribute("PropertyName", propertyName); //$NON-NLS-1$
				marker.setAttribute("DisplayName", RepositoryHelper.getDisplayName(propertyName, persist.getClass())); //$NON-NLS-1$
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
				IServer server = ApplicationServerSingleton.get().getServerManager().getServer(foreignServer);
				if (server != null) goodServers.add(foreignServer);
				else missingServers.put(foreignServer, persist);
			}
		}
		if (serverName != null && !missingServers.containsKey(serverName) && !goodServers.contains(serverName))
		{
			IServer server = ApplicationServerSingleton.get().getServerManager().getServer(serverName);
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
					return SolutionSerializer.isJSONFile(pathname.getName()) && pathname.isFile() && !pathname.getName().equals(SolutionSerializer.MEDIAS_FILE);
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
						ServoyMarker mk = MarkerMessages.UUIDDuplicate.fill(persist.getUUID());
						addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH, fileForLocation.toString(), persist);
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
		deleteMarkers(project, INVALID_DATAPROVIDERID);
		deleteMarkers(project, INVALID_COMMAND_METHOD);
		deleteMarkers(project, DEPRECATED_PROPERTY_USAGE);
		deleteMarkers(project, MULTIPLE_METHODS_ON_SAME_ELEMENT);
		deleteMarkers(project, UNRESOLVED_RELATION_UUID);
		deleteMarkers(project, MISSING_DRIVER);
		deleteMarkers(project, OBSOLETE_ELEMENT);
		deleteMarkers(project, HIDDEN_TABLE_STILL_IN_USE);

		final ServoyProject servoyProject = getServoyProject(project);
		boolean active = servoyModel.isSolutionActive(project.getName());

		final Map<String, IPersist> missingServers = new HashMap<String, IPersist>();
		final List<String> goodServers = new ArrayList<String>();
		if (servoyProject != null)
		{
			if (active)
			{
				addDeserializeProblemMarkersIfNeeded(servoyProject);
				refreshDBIMarkers();
				checkPersistDuplication();
				addDriverProblemMarker(project);
				servoyProject.getSolution().acceptVisitor(new IPersistVisitor()
				{
					private final ServoyProject[] modules = getSolutionModules(servoyProject);
					private final FlattenedSolution flattenedSolution = getServoyModel().getFlattenedSolution();
					private final Solution solution = servoyProject.getSolution();
					private IntHashMap<IPersist> elementIdPersistMap = null;
					private final Map<UUID, List<IPersist>> theMakeSureNoDuplicateUUIDsAreFound = new HashMap<UUID, List<IPersist>>();
					private final Map<Form, Boolean> formsAbstractChecked = new HashMap<Form, Boolean>();
					private final Set<UUID> methodsParsed = new HashSet<UUID>();

					public Object visit(final IPersist o)
					{
						checkCancel();

						IPersist context = o.getAncestor(IRepository.FORMS);
						if (context == null)
						{
							context = o.getAncestor(IRepository.TABLENODES);
							if (context == null)
							{
								context = o.getAncestor(IRepository.SOLUTIONS);
							}
						}

						Map<IPersist, Boolean> methodsReferences = new HashMap<IPersist, Boolean>();
						try
						{
							final Map<String, Method> methods = ((EclipseRepository)ApplicationServerSingleton.get().getDeveloperRepository()).getGettersViaIntrospection(o);
							Iterator<ContentSpec.Element> iterator = ((EclipseRepository)ApplicationServerSingleton.get().getDeveloperRepository()).getContentSpec().getPropertiesForObjectType(
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
															ServoyMarker mk = MarkerMessages.UUIDDuplicateIn.fill(other.getUUID(),
																SolutionSerializer.getRelativePath(p, false) + SolutionSerializer.getFileName(p, false));
															addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH,
																null, other);
														}
														ServoyMarker mk = MarkerMessages.UUIDDuplicateIn.fill(p.getUUID(),
															SolutionSerializer.getRelativePath(other, false) + SolutionSerializer.getFileName(other, false));
														addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH, null,
															p);
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
																		ServoyMarker mk = MarkerMessages.UUIDDuplicateIn.fill(
																			other.getUUID(),
																			SolutionSerializer.getRelativePath(p, false) +
																				SolutionSerializer.getFileName(p, false));
																		addMarker(moduleProject, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR,
																			IMarker.PRIORITY_HIGH, null, other);
																	}
																	ServoyMarker mk = MarkerMessages.UUIDDuplicateIn.fill(
																		p.getUUID(),
																		SolutionSerializer.getRelativePath(other, false) +
																			SolutionSerializer.getFileName(other, false));
																	addMarker(moduleProject, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR,
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
										if (foundPersist == null && !element.getName().equals(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName()))
										{
											String elementName = null;
											String inForm = null;
											if (o instanceof ISupportName && !(o instanceof Form) && (((ISupportName)o).getName() != null)) elementName = ((ISupportName)o).getName();
											if (context instanceof Form) inForm = ((Form)context).getName();
											ServoyMarker mk;
											if (elementName == null)
											{
												if (inForm == null) mk = MarkerMessages.PropertyTargetNotFound.fill(element.getName());
												else mk = MarkerMessages.PropertyInFormTargetNotFound.fill(element.getName(), inForm);
											}
											else
											{
												if (inForm == null) mk = MarkerMessages.PropertyOnElementTargetNotFound.fill(element.getName(), elementName);
												else mk = MarkerMessages.PropertyOnElementInFormTargetNotFound.fill(element.getName(), elementName, inForm);
											}
											if (BaseComponent.isEventProperty(element.getName()) || BaseComponent.isCommandProperty(element.getName()))
											{
												// TODO: this is a place where the same marker appears in more than one category...
												IMarker marker = addMarker(project, BaseComponent.isEventProperty(element.getName()) ? INVALID_EVENT_METHOD
													: INVALID_COMMAND_METHOD, mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
												marker.setAttribute("EventName", element.getName()); //$NON-NLS-1$
												if (context instanceof Form)
												{
													marker.setAttribute("DataSource", ((Form)context).getDataSource()); //$NON-NLS-1$
												}
											}
											else
											{
												addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
											}
										}
										else if (foundPersist instanceof ScriptMethod && context != null)
										{
											ScriptMethod scriptMethod = (ScriptMethod)foundPersist;
											if (scriptMethod.getParent() != context && scriptMethod.isPrivate())
											{
												String elementName = null;
												String inForm = null;
												if (o instanceof ISupportName && !(o instanceof Form) && (((ISupportName)o).getName() != null)) elementName = ((ISupportName)o).getName();
												if (context instanceof Form) inForm = ((Form)context).getName();
												ServoyMarker mk;
												if (elementName == null)
												{
													if (inForm == null) mk = MarkerMessages.PropertyTargetNotAccessible.fill(element.getName());
													else mk = MarkerMessages.PropertyInFormTargetNotAccessible.fill(element.getName(), inForm);
												}
												else
												{
													if (inForm == null) mk = MarkerMessages.PropertyOnElementTargetNotAccessible.fill(element.getName(),
														elementName);
													else mk = MarkerMessages.PropertyOnElementInFormTargetNotAccessible.fill(element.getName(), elementName,
														inForm);
												}
												addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
											}
											else if (context instanceof Form)
											{
												Form parentForm = (Form)context;
												Form methodForm = (Form)scriptMethod.getAncestor(IRepository.FORMS);
												if (methodForm != null && !flattenedSolution.getFormHierarchy(parentForm).contains(methodForm))
												{
													ServoyMarker mk;
													if (!(o instanceof ISupportName) || o instanceof Form || ((ISupportName)o).getName() == null)
													{
														mk = MarkerMessages.FormPropertyMethodNotAccessible.fill(element.getName(), parentForm.getName(),
															methodForm.getName());
													}
													else
													{
														mk = MarkerMessages.FormPropertyOnElementMethodNotAccessible.fill(element.getName(),
															((ISupportName)o).getName(), parentForm.getName(), methodForm.getName());
													}
													addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
												}
											}
										}
										else if (foundPersist instanceof Form &&
											!StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName().equals(element.getName()) &&
											!formCanBeInstantiated(((Form)foundPersist), flattenedSolution, formsAbstractChecked))
										{
											ServoyMarker mk = MarkerMessages.PropertyFormCannotBeInstantiated.fill(element.getName());
											addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
										}
										if (BaseComponent.isEventProperty(element.getName()) && !skipEventMethod(element.getName()) &&
											(foundPersist instanceof ScriptMethod) && !methodsParsed.contains(foundPersist.getUUID()))
										{
											methodsParsed.add(foundPersist.getUUID());
											parseEventMethod(project, (ScriptMethod)foundPersist, element.getName());
										}
										if ((foundPersist instanceof ScriptMethod) &&
											(BaseComponent.isEventProperty(element.getName()) || BaseComponent.isCommandProperty(element.getName())))
										{
											if (methodsReferences.containsKey(foundPersist))
											{
												if (methodsReferences.get(foundPersist).booleanValue())
												{
													String elementName = "";
													ServoyMarker mk = null;
													if (o instanceof ISupportName)
													{
														elementName = ((ISupportName)o).getName();
														if (elementName == null || "".equals(elementName)) elementName = "<no name>";
														mk = MarkerMessages.PropertyMultipleMethodsOnSameElement.fill(elementName);
													}
													else if (o instanceof TableNode)
													{
														elementName = ((TableNode)o).getTableName();
														mk = MarkerMessages.PropertyMultipleMethodsOnSameTable.fill(elementName);
													}
													methodsReferences.put(foundPersist, Boolean.FALSE);
													addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_INFO, IMarker.PRIORITY_LOW, null, o);
												}
											}
											else
											{
												methodsReferences.put(foundPersist, Boolean.TRUE);
											}
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
							addMarkers(project, checkValuelist(vl, flattenedSolution, ApplicationServerSingleton.get().getServerManager(), false), vl);
						}
						checkCancel();
						if (o instanceof Media)
						{
							Media oMedia = (Media)o;
							if (oMedia.getName().toLowerCase().endsWith(".tiff") || oMedia.getName().toLowerCase().endsWith(".tif")) //$NON-NLS-1$ //$NON-NLS-2$
							{
								Pair<String, String> path = SolutionSerializer.getFilePath(oMedia, false);
								ServoyMarker mk = MarkerMessages.MediaTIFF.fill(path.getLeft() + path.getRight());
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, oMedia);
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
									if (!(context instanceof Form))
									{
										ServoyLog.logError("Could not find parent form for element " + o, null); //$NON-NLS-1$
									}
									else
									{
										Form parentForm = (Form)context;
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

											if ((o instanceof Field || o instanceof GraphicalComponent) && dataProvider != null)
											{
												String format = (o instanceof Field) ? ((Field)o).getFormat() : ((GraphicalComponent)o).getFormat();
												if (o instanceof Field && ((Field)o).getDisplayType() != Field.TEXT_FIELD &&
													((Field)o).getDisplayType() != Field.TYPE_AHEAD && ((Field)o).getDisplayType() != Field.CALENDAR)
												{
													format = null;
												}
												if (format != null && format.length() > 0)
												{
													ParsedFormat parsedFormat = FormatParser.parseFormatProperty(format);
													if (parsedFormat.getDisplayFormat() != null && !parsedFormat.getDisplayFormat().startsWith("i18n:"))
													{
														// TODO: check type defined by column converter
														int dataType = dataProvider.getDataProviderType();
														try
														{
															if (dataType == IColumnTypes.DATETIME)
															{
																new SimpleDateFormat(parsedFormat.getDisplayFormat());
																if (parsedFormat.getEditFormat() != null) new SimpleDateFormat(parsedFormat.getEditFormat());
															}
															else if (dataType == IColumnTypes.INTEGER || dataType == IColumnTypes.NUMBER)
															{
																new RoundHalfUpDecimalFormat(parsedFormat.getDisplayFormat(), Locale.getDefault());
																if (parsedFormat.getEditFormat() != null) new RoundHalfUpDecimalFormat(
																	parsedFormat.getEditFormat(), Locale.getDefault());
															}
														}
														catch (Exception ex)
														{
															Debug.trace(ex);

															ServoyMarker mk;
															if (elementName == null)
															{
																mk = MarkerMessages.FormFormatInvalid.fill(inForm, parsedFormat.toFormatProperty());
															}
															else
															{
																mk = MarkerMessages.FormFormatOnElementInvalid.fill(elementName, inForm,
																	parsedFormat.toFormatProperty());
															}
															addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING,
																IMarker.PRIORITY_NORMAL, null, o);
														}
													}
												}
											}
											if (o instanceof Field && dataProvider != null)
											{
												Field field = (Field)o;
												if (field.getEditable() &&
													(field.getDisplayType() == Field.HTML_AREA || field.getDisplayType() == Field.RTF_AREA) &&
													dataProvider.getColumnWrapper() != null && dataProvider.getColumnWrapper().getColumn() instanceof Column)
												{
													Column column = (Column)dataProvider.getColumnWrapper().getColumn();
													if (column.getLength() < MIN_FIELD_LENGTH && column.getLength() > 0)
													{
														ServoyMarker mk = MarkerMessages.FormColumnLengthTooSmall.fill(elementName != null ? elementName : "",
															inForm);
														addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL,
															null, o);
													}
												}
												if (((dataProvider instanceof ScriptVariable &&
													((ScriptVariable)dataProvider).getVariableType() == IColumnTypes.MEDIA && ((ScriptVariable)dataProvider).getSerializableRuntimeProperty(IScriptProvider.TYPE) == null) ||
													(dataProvider instanceof AggregateVariable &&
														((AggregateVariable)dataProvider).getType() == IColumnTypes.MEDIA && ((AggregateVariable)dataProvider).getSerializableRuntimeProperty(IScriptProvider.TYPE) == null) ||
													(dataProvider instanceof ScriptCalculation &&
														((ScriptCalculation)dataProvider).getType() == IColumnTypes.MEDIA && ((ScriptCalculation)dataProvider).getSerializableRuntimeProperty(IScriptProvider.TYPE) == null) || (dataProvider instanceof Column && Column.mapToDefaultType(((Column)dataProvider).getType()) == IColumnTypes.MEDIA) &&
													((Column)dataProvider).getColumnInfo() != null &&
													((Column)dataProvider).getColumnInfo().getConverterName() == null) &&
													field.getDisplayType() != Field.IMAGE_MEDIA)
												{
													ServoyMarker mk = MarkerMessages.FormIncompatibleElementType.fill(elementName != null ? elementName : "",
														inForm);
													addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null,
														o);
												}
											}

											if (dataProvider == null && (parentForm.getDataSource() != null || ScopesUtils.isVariableScope(id)))
											{
												ServoyMarker mk;
												if (elementName == null)
												{
													mk = MarkerMessages.FormDataproviderNotFound.fill(inForm, id);
												}
												else
												{
													mk = MarkerMessages.FormDataproviderOnElementNotFound.fill(elementName, inForm, id);
												}
												addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
											}
											if (parentForm.getDataSource() != null && dataProvider instanceof ColumnWrapper)
											{
												Relation[] relations = ((ColumnWrapper)dataProvider).getRelations();
												if (relations != null && !relations[0].isGlobal() &&
													!parentForm.getDataSource().equals(relations[0].getPrimaryDataSource()))
												{
													ServoyMarker mk;
													if (elementName == null)
													{
														mk = MarkerMessages.FormDataproviderNotBasedOnFormTable.fill(inForm, id);
													}
													else
													{
														mk = MarkerMessages.FormDataproviderOnElementNotBasedOnFormTable.fill(elementName, inForm, id);
													}
													addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
												}
											}
											if (dataProvider instanceof AggregateVariable && o instanceof Field && ((Field)o).getEditable())
											{
												ServoyMarker mk;
												if (elementName == null)
												{
													mk = MarkerMessages.FormDataproviderAggregateNotEditable.fill(inForm, id);
												}
												else
												{
													mk = MarkerMessages.FormDataproviderOnElementAggregateNotEditable.fill(elementName, inForm, id);
												}
												addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
											}
											if (dataProvider != null && dataProvider instanceof Column && ((Column)dataProvider).getColumnInfo() != null)
											{
												if (((Column)dataProvider).getColumnInfo().isExcluded())
												{
													ServoyMarker mk;
													if (elementName == null)
													{
														mk = MarkerMessages.FormDataproviderNotFound.fill(inForm, id);
													}
													else
													{
														mk = MarkerMessages.FormDataproviderOnElementNotFound.fill(elementName, inForm, id);
													}
													addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
												}
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
						if (o instanceof BaseComponent && ((BaseComponent)o).getVisible())
						{
							// check if not outside form
							Form form = (Form)o.getAncestor(IRepository.FORMS);
							form = flattenedSolution.getFlattenedForm(form);
							if (form != null)
							{
								Point location = ((BaseComponent)o).getLocation();
								if (location != null)
								{
									boolean outsideForm = false;
									Iterator<com.servoy.j2db.persistence.Part> parts = form.getParts();
									while (parts.hasNext())
									{
										com.servoy.j2db.persistence.Part part = parts.next();
										int startPos = form.getPartStartYPos(part.getID());
										int endPos = part.getHeight();
										if (startPos <= location.y && endPos > location.y)
										{
											// found the part
											int height = ((BaseComponent)o).getSize().height;
											if (location.y + height > endPos)
											{
												String elementName = null;
												String inForm = null;
												String partName = com.servoy.j2db.persistence.Part.getDisplayName(part.getPartType());
												if (o instanceof ISupportName && ((ISupportName)o).getName() != null) elementName = ((ISupportName)o).getName();
												inForm = form.getName();
												ServoyMarker mk;
												if (elementName == null)
												{
													mk = MarkerMessages.FormUnnamedElementOutsideBoundsOfPart.fill(inForm, partName);
												}
												else
												{
													mk = MarkerMessages.FormNamedElementOutsideBoundsOfPart.fill(elementName, inForm, partName);
												}
												addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
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
														defaultPageFormat = Settings.getInstance().getProperty("pageformat");
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
											if (width < location.x + ((BaseComponent)o).getSize().width)
											{
												outsideForm = true;
											}
											break;
										}
									}
									if (location.y > form.getSize().height && form.getParts().hasNext())
									{
										outsideForm = true;
									}
									if (outsideForm)
									{
										String elementName = null;
										if (o instanceof ISupportName && ((ISupportName)o).getName() != null) elementName = ((ISupportName)o).getName();
										ServoyMarker mk;
										if (elementName == null)
										{
											mk = MarkerMessages.FormUnnamedElementOutsideBoundsOfForm.fill(form.getName());
										}
										else
										{
											mk = MarkerMessages.FormNamedElementOutsideBoundsOfForm.fill(elementName, form.getName());
										}
										addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
									}
								}
							}
						}
						if (o instanceof ISupportName && !(o instanceof Media))
						{
							String name = ((ISupportName)o).getName();
							if (name != null && !"".equals(name) && !IdentDocumentValidator.isJavaIdentifier(name))
							{
								ServoyMarker mk = MarkerMessages.ElementNameInvalidIdentifier.fill(name);
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, o);
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
								if (table == null || table.isMarkedAsHiddenInDeveloper())
								{
									Iterator<IPersist> iterator = node.getAllObjects();
									while (iterator.hasNext())
									{
										IPersist persist = iterator.next();
										String what;
										if (persist instanceof AggregateVariable) what = "Aggregation"; //$NON-NLS-1$
										else what = "Calculation"; //$NON-NLS-1$
										ServoyMarker mk;
										if (table != null)
										{
											mk = MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(table.getDataSource(), what + " ",
												((ISupportName)persist).getName());
										}
										else
										{
											mk = MarkerMessages.ItemReferencesInvalidTable.fill(what, ((ISupportName)persist).getName(), node.getTableName());
										}
										addMarker(project, mk.getType(), mk.getText(), -1, table != null ? IMarker.SEVERITY_WARNING : IMarker.SEVERITY_ERROR,
											table != null ? IMarker.PRIORITY_LOW : IMarker.PRIORITY_NORMAL, null, persist);
									}
								}
							}
						}
						checkCancel();
						if (o instanceof Form)
						{
							Form form = (Form)o;
							Table table = null;
							String path = form.getSerializableRuntimeProperty(IScriptProvider.FILENAME);
							if (path != null && !path.endsWith(SolutionSerializer.getFileName(form, false)))
							{
								ServoyMarker mk = MarkerMessages.FormFileNameInconsistent.fill(form.getName(), path);
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, form);
							}
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
										ServoyMarker mk = MarkerMessages.FormTableNotAccessible.fill(form.getName(), form.getTableName());
										addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH, null, form);
									}
									else if (table != null && table.getRowIdentColumnsCount() == 0)
									{
										ServoyMarker mk = MarkerMessages.FormTableNoPK.fill(form.getName(), form.getTableName());
										addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH, null, form);
									}
									else if (table != null && form.getInitialSort() != null)
									{
										addMarkers(project, checkSortOptions(table, form.getInitialSort(), form, flattenedSolution), form);
									}
									if (table != null && table.isMarkedAsHiddenInDeveloper())
									{
										ServoyMarker mk = MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(table.getDataSource(), "form ", form.getName());
										addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, form);
									}
								}
								catch (Exception e)
								{
									exceptionCount++;
									if (exceptionCount < MAX_EXCEPTIONS) ServoyLog.logError(e);
									ServoyMarker mk = MarkerMessages.FormTableNotAccessible.fill(form.getName(), form.getTableName());
									addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH, null, form);
								}
							}

							// if form uses global relation named foundset, check to see that it is valid
							String namedFoundset = form.getNamedFoundSet();
							if (namedFoundset != null && !namedFoundset.equals(Form.NAMED_FOUNDSET_EMPTY) &&
								!namedFoundset.equals(Form.NAMED_FOUNDSET_SEPARATE))
							{
								// it must be a global relation then
								if (flattenedSolution.getRelation(form.getGlobalRelationNamedFoundset()) == null)
								{
									// what is this then?
									ServoyMarker mk = MarkerMessages.PropertyInFormTargetNotFound.fill("namedFoundset", form.getName());
									addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, form);
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
											ServoyMarker mk = MarkerMessages.FormVariableTableCol.fill(form.getName(), var.getName(), form.getTableName());
											addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, var);
										}
									}
								}
							}
							catch (Exception ex)
							{
								exceptionCount++;
								if (exceptionCount < MAX_EXCEPTIONS) ServoyLog.logError(ex);
							}
							if (form.getExtendsID() > 0 && flattenedSolution != null)
							{
								Form superForm = flattenedSolution.getForm(form.getExtendsID());
								if (superForm != null)
								{
									if (form.getDataSource() != null && superForm.getDataSource() != null &&
										!form.getDataSource().equals(superForm.getDataSource()))
									{
										ServoyMarker mk = MarkerMessages.FormDerivedFormDifferentTable.fill(form.getName(), superForm.getName());
										addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, form);
									}

									List<Integer> forms = new ArrayList<Integer>();
									forms.add(Integer.valueOf(form.getID()));
									forms.add(Integer.valueOf(superForm.getID()));
									while (superForm != null)
									{
										if (superForm.getExtendsID() > 0)
										{
											superForm = flattenedSolution.getForm(superForm.getExtendsID());
											if (superForm != null)
											{
												if (forms.contains(Integer.valueOf(superForm.getID())))
												{
													// a cycle detected
													ServoyMarker mk = MarkerMessages.FormExtendsCycle.fill(form.getName());
													addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null,
														form);
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
								if (!part.isOverrideElement())
								{
									if (!part.canBeMoved() && parts.containsKey(Integer.valueOf(part.getPartType())))
									{
										if (parts.get(Integer.valueOf(part.getPartType())) != null &&
											parts.get(Integer.valueOf(part.getPartType())).booleanValue())
										{
											ServoyMarker mk = MarkerMessages.FormDuplicatePart.fill(form.getName(),
												com.servoy.j2db.persistence.Part.getDisplayName(part.getPartType()));
											addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, form);
											parts.put(Integer.valueOf(part.getPartType()), Boolean.FALSE);
										}
									}
									else
									{
										parts.put(Integer.valueOf(part.getPartType()), Boolean.TRUE);
									}
								}
							}

							// check to see if there are too many portals/tab panels for an acceptable slow WAN SC deployment
							int portalAndTabPanelCount = 0;
							// check to see if there are too many columns in a table view form (that could result in poor WC performance when selecting rows for example)
							int fieldCount = 0;
							// also check tab sequences
							Map<Integer, Boolean> tabSequences = new HashMap<Integer, Boolean>();
							Iterator<IPersist> iterator = form.getAllObjects();
							while (iterator.hasNext())
							{
								IPersist persist = iterator.next();
								if (persist.getTypeID() == IRepository.TABPANELS || persist.getTypeID() == IRepository.PORTALS) portalAndTabPanelCount++;
								else if (persist.getTypeID() == IRepository.FIELDS ||
									(persist.getTypeID() == IRepository.GRAPHICALCOMPONENTS && ((GraphicalComponent)persist).getLabelFor() == null)) fieldCount++;

								if (persist instanceof ISupportTabSeq && ((ISupportTabSeq)persist).getTabSeq() > 0)
								{
									int tabSeq = ((ISupportTabSeq)persist).getTabSeq();
									if (tabSequences.containsKey(Integer.valueOf(tabSeq)))
									{
										ServoyMarker mk = MarkerMessages.FormUnnamedElementDuplicateTabSequence.fill(form.getName());
										if (persist instanceof ISupportName && ((ISupportName)persist).getName() != null)
										{
											mk = MarkerMessages.FormNamedElementDuplicateTabSequence.fill(form.getName(), ((ISupportName)persist).getName());
										}
										addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, persist);
									}
									else
									{
										tabSequences.put(Integer.valueOf(tabSeq), null);
									}
								}
							}
							if (portalAndTabPanelCount > LIMIT_FOR_PORTAL_TABPANEL_COUNT_ON_FORM)
							{
								ServoyMarker mk = MarkerMessages.FormHasTooManyThingsAndProbablyLowPerformance.fill(
									String.valueOf(LIMIT_FOR_PORTAL_TABPANEL_COUNT_ON_FORM), "portals/tab panels", ""); //$NON-NLS-1$ //$NON-NLS-2$
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, form);
							}
							if (fieldCount > LIMIT_FOR_FIELD_COUNT_ON_TABLEVIEW_FORM &&
								(form.getView() == FormController.LOCKED_TABLE_VIEW || form.getView() == FormController.TABLE_VIEW))
							{
								ServoyMarker mk = MarkerMessages.FormHasTooManyThingsAndProbablyLowPerformance.fill(
									String.valueOf(LIMIT_FOR_FIELD_COUNT_ON_TABLEVIEW_FORM), "fields", " table view"); //$NON-NLS-1$ //$NON-NLS-2$
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, form);
							}

							if (form.getRowBGColorCalculation() != null)
							{
								ScriptMethod scriptMethod = null;
								boolean unresolved = true;
								Pair<String, String> scope = ScopesUtils.getVariableScope(form.getRowBGColorCalculation());
								if (scope.getLeft() != null)
								{
									scriptMethod = flattenedSolution.getScriptMethod(scope.getLeft(), scope.getRight());
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
												ScriptCalculation calc = AbstractBase.selectByName(tableNodes.next().getScriptCalculations(),
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
									ServoyMarker mk = MarkerMessages.FormRowBGCalcTargetNotFound.fill(form.getName());
									addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, form);
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
											ServoyMarker mk = MarkerMessages.CalculationFormAccess.fill(calc.getName());
											try
											{
												Table table = calc.getTable();
												if (table != null)
												{
													mk = MarkerMessages.CalculationInTableFormAccess.fill(calc.getName(), table.getName());
												}
												addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL,
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
							}

						}
						checkCancel();
						if (o instanceof Tab)
						{
							Tab tab = (Tab)o;
							if (tab.getRelationName() != null)
							{
								Relation[] relations = getServoyModel().getFlattenedSolution().getRelationSequence(tab.getRelationName());
								if (relations == null)
								{
									if (Utils.getAsUUID(tab.getRelationName(), false) != null)
									{
										// relation name was not resolved from uuid to relation name during import
										ServoyMarker mk = MarkerMessages.FormRelatedTabUnsolvedUuid.fill(tab.getRelationName());
										IMarker marker = addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL,
											null, tab);
										if (marker != null)
										{
											try
											{
												marker.setAttribute("Uuid", o.getUUID().toString()); //$NON-NLS-1$
												marker.setAttribute("SolutionName", ((Solution)tab.getAncestor(IRepository.SOLUTIONS)).getName()); //$NON-NLS-1$
												marker.setAttribute("PropertyName", "relationName"); //$NON-NLS-1$ //$NON-NLS-2$
												marker.setAttribute("DisplayName", RepositoryHelper.getDisplayName("relationName", o.getClass())); //$NON-NLS-1$ //$NON-NLS-2$
											}
											catch (Exception e)
											{
												ServoyLog.logError(e);
											}
										}
									}
									else
									{
										ServoyMarker mk = MarkerMessages.FormRelatedTabUnsolvedRelation.fill(tab.getRelationName());
										addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, tab);
									}
								}
								else
								{
									Relation relation = relations[0];
									if (!relation.isGlobal() && relation.getPrimaryServerName() != null && relation.getPrimaryTableName() != null)
									{
										if (context instanceof Form && (!relation.getPrimaryDataSource().equals(((Form)context).getDataSource())))
										{
											ServoyMarker mk = MarkerMessages.FormRelatedTabDifferentTable.fill(((Form)context).getName(), relation.getName());
											addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, tab);
										}
									}
									relation = relations[relations.length - 1];
									if (!relation.isGlobal() && relation.getPrimaryServerName() != null && relation.getPrimaryTableName() != null)
									{
										Form form = getServoyModel().getFlattenedSolution().getForm(tab.getContainsFormID());
										if (form != null &&
											(!relation.getForeignServerName().equals(form.getServerName()) || !relation.getForeignTableName().equals(
												form.getTableName())))
										{
											ServoyMarker mk = MarkerMessages.FormRelatedTabDifferentTable.fill(form.getName(), relation.getName());
											addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, tab);
										}
									}
								}
							}
						}
						checkCancel();
						if (o instanceof Field)
						{
							Field field = (Field)o;
							if (field.getValuelistID() > 0)
							{
								ValueList vl = flattenedSolution.getValueList(field.getValuelistID());
								if (vl != null)
								{
									if (field.getDisplayType() == Field.COMBOBOX && field.getEditable())
									{

										boolean showWarning = false;
										if (vl.getValueListType() == ValueList.DATABASE_VALUES && vl.getReturnDataProviders() != vl.getShowDataProviders())
										{
											showWarning = true;
										}
										if (vl.getValueListType() == ValueList.CUSTOM_VALUES && vl.getCustomValues() != null &&
											vl.getCustomValues().contains("|"))
										{
											showWarning = true;
										}
										if (showWarning)
										{
											ServoyMarker mk;
											if (field.getName() != null)
											{
												mk = MarkerMessages.FormEditableNamedComboboxCustomValuelist.fill(field.getName());
											}
											else
											{
												mk = MarkerMessages.FormEditableUnnamedComboboxCustomValuelist;
											}
											addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, field);
										}
									}
									if ((field.getDisplayType() == Field.TEXT_FIELD || field.getDisplayType() == Field.TYPE_AHEAD) &&
										vl.getValueListType() == ValueList.DATABASE_VALUES)
									{
										try
										{
											Table table = (Table)vl.getTable();
											ScriptCalculation calc = null;
											boolean errorFound = false;
											if (vl.getDataProviderID1() != null)
											{
												calc = flattenedSolution.getScriptCalculation(vl.getDataProviderID1(), table);
												if (calc != null)
												{
													Column column = table.getColumn(vl.getDataProviderID1());
													if (column == null) errorFound = true;
												}
											}
											if (vl.getDataProviderID2() != null && !errorFound)
											{
												calc = flattenedSolution.getScriptCalculation(vl.getDataProviderID2(), table);
												if (calc != null)
												{
													Column column = table.getColumn(vl.getDataProviderID2());
													if (column == null) errorFound = true;
												}
											}
											if (vl.getDataProviderID3() != null && !errorFound)
											{
												calc = flattenedSolution.getScriptCalculation(vl.getDataProviderID3(), table);
												if (calc != null)
												{
													Column column = table.getColumn(vl.getDataProviderID3());
													if (column == null) errorFound = true;
												}
											}
											if (errorFound)
											{
												ServoyMarker mk;
												if (field.getName() != null)
												{
													mk = MarkerMessages.FormTypeAheadNamedUnstoredCalculation.fill(field.getName());
												}
												else
												{
													mk = MarkerMessages.FormTypeAheadUnnamedUnstoredCalculation;
												}
												addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, field);
											}
										}
										catch (Exception e)
										{
											ServoyLog.logError(e);
										}
									}
									if (vl.getValueListType() == ValueList.DATABASE_VALUES && vl.getRelationName() != null)
									{
										Form form = (Form)o.getAncestor(IRepository.FORMS);
										String[] parts = vl.getRelationName().split("\\."); //$NON-NLS-1$
										Relation relation = flattenedSolution.getRelation(parts[0]);
										if (!relation.getPrimaryDataSource().equals(form.getDataSource()))
										{
											ServoyMarker mk;
											if (field.getName() != null)
											{
												mk = MarkerMessages.FormNamedFieldRelatedValuelist.fill(field.getName(), vl.getName(), form.getName());
											}
											else
											{
												mk = MarkerMessages.FormUnnamedFieldRelatedValuelist.fill(vl.getName(), form.getName());
											}
											addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, field);
										}
									}
									if (vl.getFallbackValueListID() > 0)
									{
										ValueList fallback = flattenedSolution.getValueList(vl.getFallbackValueListID());
										if (fallback != null && fallback.getValueListType() == ValueList.DATABASE_VALUES && fallback.getRelationName() != null)
										{
											Form form = (Form)o.getAncestor(IRepository.FORMS);
											String[] parts = fallback.getRelationName().split("\\."); //$NON-NLS-1$
											Relation relation = flattenedSolution.getRelation(parts[0]);
											if (!relation.getPrimaryDataSource().equals(form.getDataSource()))
											{
												ServoyMarker mk;
												if (field.getName() != null)
												{
													mk = MarkerMessages.FormNamedFieldFallbackRelatedValuelist.fill(field.getName(), vl.getName(),
														fallback.getName(), form.getName());
												}
												else
												{
													mk = MarkerMessages.FormUnnamedFieldFallbackRelatedValuelist.fill(vl.getName(), fallback.getName(),
														form.getName());
												}
												addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null,
													field);
											}
										}
									}
								}
							}
						}
						checkCancel();
						if (o instanceof Portal && ((Portal)o).getRelationName() != null)
						{
							Portal portal = (Portal)o;
							Relation[] relations = flattenedSolution.getRelationSequence(portal.getRelationName());
							if (relations == null)
							{
								ServoyMarker mk;
								if (portal.getName() != null)
								{
									mk = MarkerMessages.FormPortalNamedInvalidRelationName.fill(portal.getName());
								}
								else
								{
									mk = MarkerMessages.FormPortalUnnamedInvalidRelationName;
								}
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, o);
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
													if (!rel_name.startsWith(portal.getRelationName()))
													{
														ServoyMarker mk;
														String elementName = null;
														if (child instanceof ISupportName && ((ISupportName)child).getName() != null)
														{
															elementName = ((ISupportName)child).getName();
														}
														if (portal.getName() != null)
														{
															if (elementName != null)
															{
																mk = MarkerMessages.FormPortalNamedElementNamedMismatchedRelation.fill(elementName,
																	portal.getName(), rel_name, portal.getRelationName());
															}
															else
															{
																mk = MarkerMessages.FormPortalNamedElementUnnamedMismatchedRelation.fill(portal.getName(),
																	rel_name, portal.getRelationName());
															}
														}
														else
														{
															if (elementName != null)
															{
																mk = MarkerMessages.FormPortalUnnamedElementNamedMismatchedRelation.fill(elementName, rel_name,
																	portal.getRelationName());
															}
															else
															{
																mk = MarkerMessages.FormPortalUnnamedElementUnnamedMismatchedRelation.fill(rel_name,
																	portal.getRelationName());
															}
														}
														addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL,
															null, child);
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
						checkCancel();
						if (o instanceof GraphicalComponent && ((GraphicalComponent)o).getLabelFor() != null &&
							!"".equals(((GraphicalComponent)o).getLabelFor()))
						{
							IPersist parent = null;
							if (o.getParent() instanceof Form)
							{
								parent = flattenedSolution.getFlattenedForm(o);
							}
							else if (o.getParent() instanceof Portal)
							{
								parent = new FlattenedPortal((Portal)o.getParent());
							}
							if (parent != null)
							{
								final IPersist finalParent = parent;
								Object labelFor = parent.acceptVisitor(new IPersistVisitor()
								{
									public Object visit(IPersist persist)
									{
										if (persist instanceof ISupportName && ((GraphicalComponent)o).getLabelFor().equals(((ISupportName)persist).getName())) return persist;
										if (persist == finalParent)
										{
											return IPersistVisitor.CONTINUE_TRAVERSAL;
										}
										else
										{
											return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
										}
									}
								});
								if (labelFor == null)
								{
									ServoyMarker mk = MarkerMessages.FormLabelForElementNotFound.fill(((Form)o.getAncestor(IRepository.FORMS)).getName(),
										((GraphicalComponent)o).getLabelFor());
									addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, o);
								}
							}
						}
						checkCancel();
						if (o.getTypeID() == IRepository.SHAPES)
						{
							ServoyMarker mk = MarkerMessages.ObsoleteElement.fill(((Form)o.getAncestor(IRepository.FORMS)).getName());
							addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, o);
						}
						checkDeprecatedPropertyUsage(o, project);
						ISupportChilds parent = o.getParent();
						if (o.getTypeID() == IRepository.SOLUTIONS && parent != null)
						{
							// solution should have no parent
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (parent == null)
						{
							// only a solution have no parents the rest should have a parent.
							if (o.getTypeID() != IRepository.SOLUTIONS) addBadStructureMarker(o, servoyProject, project);
						}
						else if (parent.getTypeID() == IRepository.SOLUTIONS)
						{

							switch (o.getTypeID())
							{
								case IRepository.MEDIA :
								case IRepository.FORMS :
								case IRepository.RELATIONS :
								case IRepository.TABLENODES :
								case IRepository.VALUELISTS :
								case IRepository.SCRIPTVARIABLES :
								case IRepository.METHODS :
									break;
								default :
									addBadStructureMarker(o, servoyProject, project);
							}

						}
						else if (parent.getTypeID() == IRepository.FORMS)
						{
							switch (o.getTypeID())
							{
								case IRepository.SCRIPTVARIABLES :
								case IRepository.PORTALS :
								case IRepository.METHODS :
								case IRepository.TABPANELS :
								case IRepository.BEANS :
								case IRepository.RECTSHAPES :
								case IRepository.SHAPES :
								case IRepository.GRAPHICALCOMPONENTS :
								case IRepository.PARTS :
								case IRepository.FIELDS :
									break;
								default :
									addBadStructureMarker(o, servoyProject, project);
							}
						}
						else if (parent.getTypeID() == IRepository.TABLENODES)
						{
							switch (o.getTypeID())
							{
								case IRepository.AGGREGATEVARIABLES :
								case IRepository.SCRIPTCALCULATIONS :
								case IRepository.METHODS :
									break;
								default :
									addBadStructureMarker(o, servoyProject, project);
							}
						}
						else if (parent.getTypeID() == IRepository.PORTALS)
						{
							switch (o.getTypeID())
							{
								case IRepository.RECTSHAPES :
								case IRepository.SHAPES :
								case IRepository.GRAPHICALCOMPONENTS :
								case IRepository.FIELDS :
									break;
								default :
									addBadStructureMarker(o, servoyProject, project);
							}
						}
						else if (parent.getTypeID() == IRepository.RELATIONS && o.getTypeID() != IRepository.RELATION_ITEMS)
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (parent.getTypeID() == IRepository.TABPANELS && o.getTypeID() != IRepository.TABS)
						{
							addBadStructureMarker(o, servoyProject, project);
						}

						if (!(o instanceof IVariable) && !(o instanceof IScriptProvider) &&
							!Utils.getAsBoolean(((AbstractBase)o).getRuntimeProperty(SolutionDeserializer.POSSIBLE_DUPLICATE_UUID)))
						{
							// remove this property as it takes too much memory
							// debugging engine needs this info for scriptproviders !!
							((AbstractBase)o).setSerializableRuntimeProperty(IScriptProvider.FILENAME, null);
						}
						checkCancel();
						return IPersistVisitor.CONTINUE_TRAVERSAL;
					}
				});
				checkRelations(project, missingServers);
				checkCancel();
				checkStyles(project);
				checkI18n(project);
				checkLoginSolution(project);
			}
			else if (servoyModel.shouldBeModuleOfActiveSolution(project.getName()))
			{
				// so we have an actual Servoy project that is not active, but it should be active
				addDeserializeProblemMarkersIfNeeded(servoyProject);
				if (servoyProject.getDeserializeExceptions().size() == 0 && servoyProject.getSolution() == null)
				{
					addDeserializeProblemMarker(servoyProject.getProject(), "Probably some corrupted file(s). Please check solution metadata file.", //$NON-NLS-1$
						servoyProject.getProject().getName());
					ServoyLog.logError("No solution in a servoy project that has no deserialize problems", null); //$NON-NLS-1$
				}
			}

			for (Entry<String, IPersist> entry : missingServers.entrySet())
			{
				String missingServer = entry.getKey();
				IPersist persist = entry.getValue();
				ServoyMarker mk = MarkerMessages.ServerNotAccessibleFirstOccurence.fill(project.getName(), missingServer);
				IMarker marker = addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH, null, persist);
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
			canBeInstantiated = Boolean.valueOf(flattenedSolution.formCanBeInstantiated(form));
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
					ServoyMarker marker = MarkerMessages.ValuelistDBWithCustomValues.fill(vl.getName());
					problems.add(new Problem(marker.getType(), IMarker.SEVERITY_ERROR, marker.getText(), marker.getFix()));
					if (fixIfPossible) vl.setCustomValues(null);
				}
				String dataSource = null;
				Table table = null;
				if (vl.getRelationName() != null)
				{
					// vl. based on relation; make sure table name/server name are not specified
					if (vl.getTableName() != null || vl.getServerName() != null)
					{
						ServoyMarker mk = MarkerMessages.ValuelistRelationWithDatasource.fill(vl.getName());
						problems.add(new Problem(mk.getType(), IMarker.SEVERITY_ERROR, mk.getText(), mk.getFix()));
						if (fixIfPossible) vl.setDataSource(null);
					}
					String[] parts = vl.getRelationName().split("\\."); //$NON-NLS-1$
					for (String relName : parts)
					{
						Relation relation = flattenedSolution.getRelation(relName);
						if (relation == null)
						{
							ServoyMarker mk = MarkerMessages.ValuelistRelationNotFound.fill(vl.getName(), relName);
							problems.add(new Problem(mk.getType(), IMarker.SEVERITY_ERROR, mk.getText()));
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
							ServoyMarker mk = MarkerMessages.ValuelistRelationSequenceInconsistent.fill(vl.getName(), vl.getRelationName());
							problems.add(new Problem(mk.getType(), IMarker.SEVERITY_ERROR, mk.getText()));
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
					ServoyMarker mk = MarkerMessages.ValuelistDBNotTableOrRelation.fill(vl.getName());
					problems.add(new Problem(mk.getType(), IMarker.SEVERITY_ERROR, mk.getText()));
				}
				if (dataSource != null)
				{
					String[] stn = DataSourceUtils.getDBServernameTablename(dataSource);
					if (stn == null || (stn != null && (stn.length == 0 || (stn.length > 0 && stn[0] == null))))
					{
						ServoyMarker mk = MarkerMessages.ValuelistDBMalformedTableDefinition.fill(vl.getName(), dataSource);
						problems.add(new Problem(mk.getType(), IMarker.SEVERITY_ERROR, mk.getText()));
					}
					else
					{
						IServerInternal server = (IServerInternal)sm.getServer(stn[0]);
						if (server != null)
						{
							if (!server.getName().equals(stn[0]))
							{
								ServoyMarker mk = MarkerMessages.ValuelistDBServerDuplicate.fill(vl.getName(), stn[0]);
								problems.add(new Problem(mk.getType(), IMarker.SEVERITY_WARNING, mk.getText()));
							}
							table = server.getTable(stn[1]);
							if (table == null)
							{
								ServoyMarker mk = MarkerMessages.ValuelistDBTableNotAccessible.fill(vl.getName(), stn[1]);
								problems.add(new Problem(mk.getType(), IMarker.SEVERITY_ERROR, mk.getText()));
							}
							else if (table.isMarkedAsHiddenInDeveloper())
							{
								ServoyMarker mk = MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(table.getDataSource(), "valuelist ", vl.getName());
								problems.add(new Problem(mk.getType(), IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, mk.getText(), null));
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
								ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID1(), table.getName());
								problems.add(new Problem(mk.getType(), IMarker.SEVERITY_ERROR, mk.getText()));
							}
						}
						else if (column.getColumnInfo() != null && column.getColumnInfo().isExcluded())
						{
							ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID1(), table.getName());
							problems.add(new Problem(mk.getType(), IMarker.SEVERITY_WARNING, mk.getText()));
						}
					}
					if (vl.getDataProviderID2() != null && !vl.getDataProviderID2().equals(vl.getDataProviderID1()) && !"".equals(vl.getDataProviderID2()))//$NON-NLS-1$
					{
						Column column = table.getColumn(vl.getDataProviderID2());
						if (column == null)
						{
							if (flattenedSolution.getScriptCalculation(vl.getDataProviderID2(), table) == null)
							{
								ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID2(), table.getName());
								problems.add(new Problem(mk.getType(), IMarker.SEVERITY_ERROR, mk.getText()));
							}
						}
						else if (column.getColumnInfo() != null && column.getColumnInfo().isExcluded())
						{
							ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID2(), table.getName());
							problems.add(new Problem(mk.getType(), IMarker.SEVERITY_WARNING, mk.getText()));
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
								ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID3(), table.getName());
								problems.add(new Problem(mk.getType(), IMarker.SEVERITY_ERROR, mk.getText()));
							}
						}
						else if (column.getColumnInfo() != null && column.getColumnInfo().isExcluded())
						{
							ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID3(), table.getName());
							problems.add(new Problem(mk.getType(), IMarker.SEVERITY_WARNING, mk.getText()));
						}
					}
					if (vl.getUseTableFilter() && vl.getValueListType() == ValueList.DATABASE_VALUES && vl.getDatabaseValuesType() == ValueList.TABLE_VALUES)
					{
						Column column = table.getColumn(DBValueList.NAME_COLUMN);
						if (column == null)
						{
							ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), DBValueList.NAME_COLUMN, table.getName());
							problems.add(new Problem(mk.getType(), IMarker.SEVERITY_ERROR, mk.getText()));
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
					ServoyMarker marker = MarkerMessages.ValuelistCustomValuesWithDBInfo.fill(vl.getName());
					problems.add(new Problem(marker.getType(), IMarker.SEVERITY_ERROR, marker.getText(), marker.getFix()));
					if (fixIfPossible)
					{
						vl.setDataSource(null);
						vl.setRelationName(null);
					}
				}
				if (vl.getValueListType() == ValueList.GLOBAL_METHOD_VALUES)
				{
					ScriptMethod scriptMethod = flattenedSolution.getScriptMethod(null, vl.getCustomValues());
					if (scriptMethod == null)
					{
						ServoyMarker mk = MarkerMessages.ValuelistGlobalMethodNotFound.fill(vl.getName());
						problems.add(new Problem(mk.getType(), IMarker.SEVERITY_ERROR, mk.getText()));
					}
					else if (scriptMethod.getParent() != vl.getParent() && scriptMethod.isPrivate())
					{
						ServoyMarker mk = MarkerMessages.ValuelistGlobalMethodNotAccessible.fill(vl.getName());
						problems.add(new Problem(mk.getType(), IMarker.SEVERITY_WARNING, mk.getText()));
					}
				}
				if (vl.getValueListType() == ValueList.CUSTOM_VALUES)
				{
					String values = vl.getCustomValues();
					boolean invalidValues = false;
					if (values != null && values.contains("|")) //$NON-NLS-1$
					{
						StringTokenizer tk = new StringTokenizer(values.trim(), "\r\n"); //$NON-NLS-1$
						while (tk.hasMoreTokens())
						{
							String line = tk.nextToken();
							if (!line.contains("|")) //$NON-NLS-1$
							{
								invalidValues = true;
								break;
							}
						}
					}
					if (invalidValues)
					{
						ServoyMarker mk = MarkerMessages.ValuelistInvalidCustomValues.fill(vl.getName());
						problems.add(new Problem(mk.getType(), IMarker.SEVERITY_WARNING, mk.getText()));
					}
				}
			}
			else
			{
				ServoyMarker mk = MarkerMessages.ValuelistTypeUnknown.fill(vl.getName(), vl.getValueListType());
				problems.add(new Problem(mk.getType(), IMarker.SEVERITY_ERROR, mk.getText()));
				if (fixIfPossible) vl.setValueListType(ValueList.CUSTOM_VALUES);
			}
		}
		catch (Exception ex)
		{
			exceptionCount++;
			if (exceptionCount < MAX_EXCEPTIONS) ServoyLog.logError(ex);
			ServoyMarker mk;
			if (ex.getMessage() != null) mk = MarkerMessages.ValuelistGenericErrorWithDetails.fill(vl.getName(), ex.getMessage());
			else mk = MarkerMessages.ValuelistGenericError.fill(vl.getName());
			problems.add(new Problem(mk.getType(), IMarker.SEVERITY_ERROR, mk.getText()));
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
				ServoyProject module = ServoyModelFinder.getServoyModel().getServoyProject(name);
				if (module != null && !modules.contains(module))
				{
					modules.add(module);
					addModules(modules, module);
				}
			}
		}
	}

	private void addDeserializeProblemMarkersIfNeeded(ServoyProject servoyProject)
	{
		HashMap<File, Exception> deserializeExceptionMessages = servoyProject.getDeserializeExceptions();
		for (Map.Entry<File, Exception> entry : deserializeExceptionMessages.entrySet())
		{
			IResource file = getEclipseResourceFromJavaIO(entry.getKey(), servoyProject.getProject());
			if (file == null) file = servoyProject.getProject();
			addDeserializeProblemMarker(file, entry.getValue().getMessage(), servoyProject.getProject().getName());
		}
	}

	private void addDriverProblemMarker(IProject project)
	{
		ServoyProject activeProject = getServoyModel().getActiveProject();
		if (activeProject != null && activeProject.getProject().getName().equals(project.getName()))
		{
			String[] array = ApplicationServerSingleton.get().getServerManager().getServerNames(true, false, false, true);
			for (String server_name : array)
			{
				IServerInternal server = (IServerInternal)ApplicationServerSingleton.get().getServerManager().getServer(server_name, true, false);
				boolean existing = false;
				for (String name : ApplicationServerSingleton.get().getServerManager().getKnownDriverClassNames())
				{
					if (name.equals(server.getConfig().getDriver()))
					{
						existing = true;
						break;
					}
				}
				if (!existing)
				{
					ServoyMarker mk = MarkerMessages.MissingDriver.fill(server_name, server.getConfig().getDriver());
					IMarker marker = addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, null);
					try
					{
						marker.setAttribute("serverName", server_name); //$NON-NLS-1$
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}
			}

		}
	}

	public void refreshDBIMarkers()
	{
		// do not delete or add dbi marker here
		DataSourceCollectorVisitor datasourceCollector = new DataSourceCollectorVisitor();
		for (ServoyProject sp : getServoyModel().getModulesOfActiveProject())
		{
			sp.getSolution().acceptVisitor(datasourceCollector);
		}

		ServoyResourcesProject resourcesProject = getServoyModel().getActiveResourcesProject();
		if (resourcesProject != null && resourcesProject.getProject() != null)
		{
			try
			{
				IMarker[] markers = resourcesProject.getProject().findMarkers(DATABASE_INFORMATION_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
				if (markers != null && markers.length > 0)
				{
					for (IMarker marker : markers)
					{
						String serverName = marker.getAttribute(TableDifference.ATTRIBUTE_SERVERNAME, null);
						String tableName = marker.getAttribute(TableDifference.ATTRIBUTE_TABLENAME, null);
						if (serverName != null && tableName != null)
						{
							String datasource = DataSourceUtils.createDBTableDataSource(serverName, tableName);
							if (!datasourceCollector.getDataSources().contains(datasource))
							{
								marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
							}
							else
							{
								String columnName = marker.getAttribute(TableDifference.ATTRIBUTE_COLUMNNAME, null);
								if (getServoyModel().getDataModelManager() != null)
								{
									TableDifference tableDifference = getServoyModel().getDataModelManager().getColumnDifference(serverName, tableName,
										columnName);
									if (tableDifference != null)
									{
										int severity = tableDifference.getSeverity();
										if (severity >= 0)
										{
											marker.setAttribute(IMarker.SEVERITY, severity);
										}
									}
								}
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}

	}


	private void addDeserializeProblemMarker(IResource resource, String deserializeExceptionMessage, String solutionName)
	{
		ServoyMarker mk;
		int charNo = -1;
		if (deserializeExceptionMessage == null)
		{
			mk = MarkerMessages.SolutionDeserializeError.fill(solutionName, "Errors in file content.");
		}
		else
		{
			mk = MarkerMessages.SolutionDeserializeError.fill(solutionName, deserializeExceptionMessage);
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
		addMarker(resource, mk.getType(), mk.getText(), charNo, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null);
		ServoyLog.logWarning(mk.getText(), null);
	}

	private void parseEventMethod(final IProject project, final ScriptMethod eventMethod, final String eventName)
	{
		if (eventMethod != null &&
			(eventMethod.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS) == null || eventMethod.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS).length == 0) &&
			eventMethod.getDeclaration().contains("arguments"))
		{
			int offset = ScriptingUtils.getArgumentsUsage(eventMethod.getDeclaration());
			if (offset >= 0)
			{
				ServoyMarker mk = MarkerMessages.MethodEventParameters;
				IMarker marker = addMarker(project, mk.getType(), mk.getText(), eventMethod.getLineNumberOffset() + offset, IMarker.SEVERITY_WARNING,
					IMarker.PRIORITY_NORMAL, null, eventMethod);
				try
				{
					marker.setAttribute("EventName", eventName); //$NON-NLS-1$
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
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
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
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
		ServoyMarker mk;
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
			mk = MarkerMessages.SolutionBadStructure_EntityManuallyMoved.fill(servoyProject.getSolution().getName(), ((ISupportName)o).getName());
		}
		else
		{
			mk = MarkerMessages.SolutionBadStructure.fill(servoyProject.getSolution().getName());
		}
		addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_LOW, path, o);
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
		ServoyProject servoyProject = getServoyModel().getServoyProject(project.getName());
		Solution solution = servoyProject.getSolution();
		if (solution.getI18nTableName() != null && solution.getI18nServerName() != null)
		{
			// is this table actually hidden in developer? If yes, show a warning. (developer would work even if table is not there based on resources files, but if it was
			// hidden on purpose, it is probably meant as deprecated and we should issue a warning)
			IServerInternal s = (IServerInternal)ApplicationServerSingleton.get().getServerManager().getServer(solution.getI18nServerName());
			if (s != null && s.isValid() && s.getConfig().isEnabled())
			{
				if (s.isTableMarkedAsHiddenInDeveloper(solution.getI18nTableName()))
				{
					ServoyMarker mk = MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(
						DataSourceUtils.createDBTableDataSource(solution.getI18nServerName(), solution.getI18nTableName()), "i18n for solution ",
						solution.getName());
					addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, solution);
				}
			}

			ServoyProject[] modules = getSolutionModules(servoyProject);
			if (modules != null)
			{
				for (ServoyProject module : modules)
				{
					Solution mod = module.getSolution();
					if (mod != null && mod.getI18nServerName() != null && mod.getI18nTableName() != null &&
						(!mod.getI18nServerName().equals(solution.getI18nServerName()) || !mod.getI18nTableName().equals(solution.getI18nTableName())))
					{
						ServoyMarker mk = MarkerMessages.ModuleDifferentI18NTable.fill(mod.getName(), solution.getName());
						addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, null);
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
		String[] array = ApplicationServerSingleton.get().getServerManager().getServerNames(true, true, false, true);
		for (String server_name : array)
		{
			try
			{
				IServerInternal server = (IServerInternal)ApplicationServerSingleton.get().getServerManager().getServer(server_name, true, true);
				List<String> tableNames = server.getTableAndViewNames(true);
				Iterator<String> tables = tableNames.iterator();
				while (tables.hasNext())
				{
					String tableName = tables.next();
					if (server.isTableLoaded(tableName))
					{
						Table table = server.getTable(tableName);
						IResource res = project;
						if (getServoyModel().getDataModelManager() != null &&
							getServoyModel().getDataModelManager().getDBIFile(server_name, tableName).exists())
						{
							res = getServoyModel().getDataModelManager().getDBIFile(server_name, tableName);
						}
						Map<String, Column> columnsByName = new HashMap<String, Column>();
						Map<String, Column> columnsByDataProviderID = new HashMap<String, Column>();
						for (Column column : table.getColumns())
						{
							if (column.getColumnInfo() != null && column.getSequenceType() == ColumnInfo.UUID_GENERATOR &&
								!column.getColumnInfo().hasFlag(Column.UUID_COLUMN))
							{
								ServoyMarker mk = MarkerMessages.ColumnUUIDFlagNotSet.fill(tableName, column.getName());
								addMarker(res, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, null);
							}
							// TODO: check type defined by column converter
							if ((column.getSequenceType() == ColumnInfo.UUID_GENERATOR && (column.getDataProviderType() != IColumnTypes.TEXT && column.getDataProviderType() != IColumnTypes.MEDIA)) ||
								(column.getSequenceType() == ColumnInfo.SERVOY_SEQUENCE && (column.getDataProviderType() != IColumnTypes.INTEGER && column.getDataProviderType() != IColumnTypes.NUMBER)))
							{
								ServoyMarker mk = MarkerMessages.ColumnIncompatibleTypeForSequence.fill(tableName, column.getName());
								addMarker(res, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, null);
							}
							else if (column.getSequenceType() == ColumnInfo.UUID_GENERATOR && column.getLength() > 0 && column.getLength() < 36)
							{
								ServoyMarker mk = MarkerMessages.ColumnInsufficientLengthForSequence.fill(tableName, column.getName());
								addMarker(res, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, null);
							}
							if (column.getSequenceType() == ColumnInfo.DATABASE_IDENTITY && !column.isDatabasePK())
							{
								ServoyMarker mk = MarkerMessages.ColumnDatabaseIdentityProblem.fill(tableName, column.getName());
								addMarker(res, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, null);
							}
							if (column.getColumnInfo() != null && column.getColumnInfo().getForeignType() != null &&
								!tableNames.contains(column.getColumnInfo().getForeignType()))
							{
								ServoyMarker mk = MarkerMessages.ColumnForeignTypeProblem.fill(tableName, column.getName(),
									column.getColumnInfo().getForeignType());
								addMarker(res, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, null);
							}
							if (column.getColumnInfo() != null && column.getColumnInfo().getAutoEnterType() == ColumnInfo.LOOKUP_VALUE_AUTO_ENTER)
							{
								String lookup = column.getColumnInfo().getLookupValue();
								if (lookup != null && !"".equals(lookup)) //$NON-NLS-1$
								{
									boolean invalid = false;
									if (ScopesUtils.isVariableScope(lookup))
									{
										if (getServoyModel().getFlattenedSolution().getGlobalDataProvider(lookup) == null &&
											getServoyModel().getFlattenedSolution().getScriptMethod(null, lookup) == null)
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
											Relation[] relations = getServoyModel().getFlattenedSolution().getRelationSequence(rel_name);
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
										ServoyMarker mk = MarkerMessages.ColumnLookupInvalid.fill(tableName, column.getName());
										addMarker(res, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, null);
									}
								}
							}
							String columnName = column.getName();
							String columnDataProviderID = column.getDataProviderID();
							if (columnsByName.containsKey(columnName) || columnsByName.containsKey(columnDataProviderID) ||
								columnsByDataProviderID.containsKey(columnName) || columnsByDataProviderID.containsKey(columnDataProviderID))
							{
								Column otherColumn = columnsByName.get(columnName);
								if (otherColumn == null)
								{
									otherColumn = columnsByName.get(columnDataProviderID);
								}
								if (otherColumn == null)
								{
									otherColumn = columnsByDataProviderID.get(columnDataProviderID);
								}
								if (otherColumn == null)
								{
									otherColumn = columnsByDataProviderID.get(columnName);
								}
								ServoyMarker mk = MarkerMessages.ColumnDuplicateNameDPID.fill(tableName, column.getName(), otherColumn.getName());
								addMarker(res, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, null);
							}
							columnsByName.put(columnName, column);
							columnsByDataProviderID.put(columnDataProviderID, column);
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
		ServoyProject servoyProject = getServoyModel().getServoyProject(project.getName());
		FlattenedSolution flattenedSolution = getServoyModel().getFlattenedSolution();
		if (servoyProject != null)
		{
			Iterator<Form> it = servoyProject.getSolution().getForms(null, false);
			while (it.hasNext())
			{
				final Form form = it.next();
				String styleName = form.getStyleName();
				if (styleName == null && form.getExtendsID() > 0)
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
						style = (Style)ApplicationServerSingleton.get().getDeveloperRepository().getActiveRootObject(styleName, IRepository.STYLES);
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
					final Style finalStyle = style;
					if (style == null)
					{
						ServoyMarker mk = MarkerMessages.StyleNotFound.fill(styleName, form.getName());
						IMarker marker = addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, form);
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
									String[] classes = ModelUtils.getStyleClasses(finalStyle, ModelUtils.getStyleLookupname(o), form.getStyleClass());
									List<String> styleClasses = Arrays.asList(classes);
									if (!styleClasses.contains(styleClass))
									{
										ServoyMarker mk = MarkerMessages.StyleFormClassNotFound.fill(styleClass, form.getName());
										if (o instanceof ISupportName && !(o instanceof Form) && (((ISupportName)o).getName() != null))
										{
											mk = MarkerMessages.StyleElementClassNotFound.fill(styleClass, ((ISupportName)o).getName(), form.getName());
										}
										addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, o);
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
									ServoyMarker mk = MarkerMessages.StyleFormClassNoStyle.fill(styleClass, form.getName());
									if (o instanceof ISupportName && !(o instanceof Form) && (((ISupportName)o).getName() != null))
									{
										mk = MarkerMessages.StyleElementClassNoStyle.fill(styleClass, ((ISupportName)o).getName(), form.getName());
									}
									addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, o);
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
		ServoyProject servoyProject = getServoyModel().getServoyProject(project.getName());
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
						ServoyMarker mk = MarkerMessages.InvalidSortOptionsRelationNotFound.fill(elementName, name, sortOptions, split[i]);
						problems.add(new Problem(mk.getType(), IMarker.SEVERITY_WARNING, mk.getText()));
						lastTable = null;
						break;
					}
					else
					{
						if (!lastTable.equals(relation.getPrimaryTable()))
						{
							ServoyMarker mk = MarkerMessages.InvalidSortOptionsRelationDifferentPrimaryDatasource.fill(elementName, name, sortOptions,
								relation.getName());
							problems.add(new Problem(mk.getType(), IMarker.SEVERITY_WARNING, mk.getText()));
						}
						lastTable = relation.getForeignTable();
					}
				}
				if (lastTable != null)
				{
					String colName = split[split.length - 1];
					Column c = lastTable.getColumn(colName);
					if (c == null || (c.getColumnInfo() != null && c.getColumnInfo().isExcluded()))
					{
						ServoyMarker mk = MarkerMessages.InvalidSortOptionsColumnNotFound.fill(elementName, name, sortOptions, colName);
						problems.add(new Problem(mk.getType(), IMarker.SEVERITY_WARNING, mk.getText()));
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
		ServoyProject servoyProject = getServoyModel().getServoyProject(project.getName());
		if (servoyProject != null)
		{
			ServoyMarker mk = null;

			IServerManagerInternal sm = ApplicationServerSingleton.get().getServerManager();
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
							mk = MarkerMessages.RelationPrimaryServerWithProblems.fill(element.getName(), element.getPrimaryServerName());
							element.setValid(false);
							addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}
						else
						{
							if (!pserver.getName().equals(element.getPrimaryServerName()))
							{
								mk = MarkerMessages.RelationPrimaryServerDuplicate.fill(element.getName(), element.getPrimaryServerName());
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, element);
							}
						}
						ITable ptable = pserver.getTable(element.getPrimaryTableName());
						boolean usingHiddenTableInPrimary = false;
						if (ptable == null)
						{
							mk = MarkerMessages.RelationPrimaryTableNotFound.fill(element.getName(), element.getPrimaryTableName(),
								element.getPrimaryServerName());
							element.setValid(false);
							addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}
						else
						{
							if (((Table)ptable).isMarkedAsHiddenInDeveloper())
							{
								usingHiddenTableInPrimary = true;
								mk = MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(ptable.getDataSource(), "relation ", element.getName());
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, element);
							}
							if (((Table)ptable).getRowIdentColumnsCount() == 0)
							{
								mk = MarkerMessages.RelationPrimaryTableWithoutPK.fill(element.getName(), element.getPrimaryTableName(),
									element.getPrimaryServerName());
								element.setValid(false);
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
								continue;
							}
						}

						IServerInternal fserver = (IServerInternal)sm.getServer(element.getForeignServerName());
						if (fserver == null)
						{
							mk = MarkerMessages.RelationForeignServerWithProblems.fill(element.getName(), element.getForeignServerName());
							element.setValid(false);
							addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}
						else if (!fserver.getName().equals(element.getForeignServerName()))
						{
							mk = MarkerMessages.RelationForeignServerDuplicate.fill(element.getName(), element.getForeignServerName());
							addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, element);
						}

						ITable ftable = fserver.getTable(element.getForeignTableName());
						if (ftable == null)
						{
							mk = MarkerMessages.RelationForeignTableNotFound.fill(element.getName(), element.getForeignTableName(),
								element.getForeignServerName());
							element.setValid(false);
							addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}
						else
						{
							if (!usingHiddenTableInPrimary && ((Table)ftable).isMarkedAsHiddenInDeveloper())
							{
								mk = MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(ftable.getDataSource(), "relation ", element.getName());
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, element);
							}
							if (((Table)ftable).getRowIdentColumnsCount() == 0)
							{
								mk = MarkerMessages.RelationForeignTableWithoutPK.fill(element.getName(), element.getForeignTableName(),
									element.getForeignServerName());
								element.setValid(false);
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
								continue;
							}
						}
						if (!element.isParentRef() && element.getItemCount() == 0)
						{
							mk = MarkerMessages.RelationEmpty.fill(element.getName());
							element.setValid(false);
							addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}
						if (element.getInitialSort() != null)
						{
							addMarkers(project, checkSortOptions((Table)ftable, element.getInitialSort(), element, getServoyModel().getFlattenedSolution()),
								element);
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
								mk = MarkerMessages.RelationItemNoPrimaryDataprovider.fill(element.getName());
								errorsFound = true;
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
							}
							else
							{
								if (ScopesUtils.isVariableScope(primaryDataProvider))
								{
									dataProvider = getServoyModel().getFlattenedSolution().getGlobalDataProvider(primaryDataProvider);
								}
								else
								{
									dataProvider = getServoyModel().getFlattenedSolution().getDataProviderForTable((Table)ptable, primaryDataProvider);
								}
								if (dataProvider == null)
								{
									mk = MarkerMessages.RelationItemPrimaryDataproviderNotFound.fill(element.getName(), primaryDataProvider);
									errorsFound = true;
									addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
								}
							}
							if (foreignColumn == null || "".equals(foreignColumn))//$NON-NLS-1$ 
							{
								mk = MarkerMessages.RelationItemNoForeignDataprovider.fill(element.getName());
								errorsFound = true;
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
							}
							else
							{
								column = getServoyModel().getFlattenedSolution().getDataProviderForTable((Table)ftable, foreignColumn);
								if (column == null)
								{
									mk = MarkerMessages.RelationItemForeignDataproviderNotFound.fill(element.getName(), foreignColumn);
									errorsFound = true;
									addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
								}
							}
							if (dataProvider != null && column != null && dataProvider instanceof Column && column instanceof Column &&
								((Column)dataProvider).getColumnInfo() != null && ((Column)column).getColumnInfo() != null)
							{
								boolean primaryColumnUuidFlag = ((Column)dataProvider).getColumnInfo().hasFlag(Column.UUID_COLUMN);
								boolean foreignColumnUuidFlag = ((Column)column).getColumnInfo().hasFlag(Column.UUID_COLUMN);
								if ((primaryColumnUuidFlag && !foreignColumnUuidFlag) || (!primaryColumnUuidFlag && foreignColumnUuidFlag))
								{
									mk = MarkerMessages.RelationItemUUIDProblem.fill(element.getName(), primaryDataProvider, foreignColumn);
									errorsFound = true;
									addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
								}
								if (((Column)dataProvider).getColumnInfo().isExcluded())
								{
									mk = MarkerMessages.RelationItemPrimaryDataproviderNotFound.fill(element.getName(), primaryDataProvider);
									addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, element);
								}
								if (((Column)column).getColumnInfo().isExcluded())
								{
									mk = MarkerMessages.RelationItemForeignDataproviderNotFound.fill(element.getName(), foreignColumn);
									addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_NORMAL, null, element);
								}
							}
						}
						if (errorsFound)
						{
							element.setValid(false);
							continue;
						}
						if (getServoyModel().getActiveProject() == servoyProject)
						{
							String typeMismatchWarning = element.checkKeyTypes(getServoyModel().getFlattenedSolution());
							if (typeMismatchWarning != null)
							{
								mk = MarkerMessages.RelationItemTypeProblem.fill(element.getName(), typeMismatchWarning);
								addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_WARNING, IMarker.PRIORITY_LOW, null, element);
							}
						}
					}
					catch (Exception ex)
					{
						exceptionCount++;
						if (exceptionCount < MAX_EXCEPTIONS) ServoyLog.logError(ex);
						element.setValid(false);
						if (ex.getMessage() != null) mk = MarkerMessages.RelationGenericErrorWithDetails.fill(element.getName(), ex.getMessage());
						else mk = MarkerMessages.RelationGenericError.fill(element.getName());
						addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, element);
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
			final IProject[] referencedProjects = project.getDescription().getReferencedProjects();
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
				ServoyMarker mk = MarkerMessages.ReferencesToMultipleResources.fill(project.getName());
				addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, null);
			}
			else if (count == 0)
			{
				// 0 => no referenced resources projects; error; quick fix would be choose one of the resources projects in the work space
				ServoyMarker mk = MarkerMessages.NoResourceReference.fill(project.getName());
				addMarker(project, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, null, null);
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
				addMarker(resource, problem.type, problem.message, -1, problem.severity, problem.priority, null, persist);
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

			if (persist != null || type.equals(MISSING_DRIVER))
			{
				addExtensionMarkerAttributes(marker, persist);
			}

			if (type.equals(INVALID_TABLE_NODE_PROBLEM))
			{
				marker.setAttribute("Uuid", persist.getUUID().toString()); //$NON-NLS-1$
				marker.setAttribute("Name", ((ISupportName)persist).getName()); //$NON-NLS-1$
				marker.setAttribute("SolutionName", resource.getName()); //$NON-NLS-1$
			}
			else if (type.equals(DUPLICATE_UUID) || type.equals(DUPLICATE_SIBLING_UUID) || type.equals(BAD_STRUCTURE_MARKER_TYPE) ||
				type.equals(INVALID_SORT_OPTION) || type.equals(EVENT_METHOD_MARKER_TYPE) || type.equals(PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE) ||
				type.equals(INVALID_EVENT_METHOD) || type.equals(MISSING_STYLE) || type.equals(INVALID_COMMAND_METHOD) || type.equals(INVALID_DATAPROVIDERID) ||
				type.equals(OBSOLETE_ELEMENT) || type.equals(HIDDEN_TABLE_STILL_IN_USE))
			{
				marker.setAttribute("Uuid", persist.getUUID().toString()); //$NON-NLS-1$
				marker.setAttribute("SolutionName", resource.getName()); //$NON-NLS-1$
				if (type.equals(INVALID_DATAPROVIDERID) && persist instanceof ISupportDataProviderID)
				{
					marker.setAttribute("DataProviderID", ((ISupportDataProviderID)persist).getDataProviderID()); //$NON-NLS-1$
				}
			}
			else if (type.equals(DUPLICATE_NAME_MARKER_TYPE))
			{
				marker.setAttribute("Uuid", persist.getUUID().toString()); //$NON-NLS-1$
				marker.setAttribute("SolutionName", persist.getRootObject().getName()); //$NON-NLS-1$
			}

			return marker;
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Cannot create problem marker", e); //$NON-NLS-1$
		}
		return null;
	}

	// Extensions that want to add marker attributes based on persists will do that here (for example preferred editor to open). 
	private static void addExtensionMarkerAttributes(IMarker marker, IPersist persist)
	{
		if (markerContributors == null)
		{
			List<IMarkerAttributeContributor> contributors = ResourcesUtils.getExtensions(IMarkerAttributeContributor.EXTENSION_ID);
			markerContributors = contributors.toArray(new IMarkerAttributeContributor[contributors.size()]);
		}

		for (IMarkerAttributeContributor markerContributor : markerContributors)
		{
			markerContributor.contributeToMarker(marker, persist);
		}
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
			if (file.getProject().isOpen()) file.deleteMarkers(type, true, IResource.DEPTH_INFINITE);
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
			ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(SERVOY_BUILDER_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
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
	 * @return the servoyModel
	 */
	private IServoyModel getServoyModel()
	{
		if (servoyModel == null)
		{
			servoyModel = ServoyModelFinder.getServoyModel();
		}
		return servoyModel;
	}

	/**
	 * Container class for problem and optional fix.
	 * 
	 * @author rgansevles
	 *
	 */
	public static class Problem
	{
		public final String type;
		public final int severity;
		public final String message;
		public final String fix;
		public final int priority;

		public Problem(String type, int severity, int priority, String message, String fix)
		{
			this.type = type;
			this.severity = severity;
			this.priority = priority;
			this.message = message;
			this.fix = fix;
		}

		public Problem(String type, int severity, String message, String fix)
		{
			this(type, severity, IMarker.PRIORITY_NORMAL, message, fix);
		}

		public Problem(String type, int severity, String message)
		{
			this(type, severity, IMarker.PRIORITY_NORMAL, message, null);
		}
	}

}
