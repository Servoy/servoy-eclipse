/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.MarkerMessages.ServoyMarker;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ResourcesUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.keyword.Ident;

/**
 * @author lvostinar
 *
 */
public class ServoyBuilderUtils
{

	public static boolean canBuildIncremental(List<IResource> resources)
	{
		if (resources.size() == 3 && resources.get(0) instanceof IProject && resources.get(1) instanceof IFolder && resources.get(2) instanceof IFile)
		{
			IFolder folder = (IFolder)resources.get(1);
			IFile file = (IFile)resources.get(2);
			if (folder.getName().equals(SolutionSerializer.FORMS_DIR) &&
				((file.getFileExtension() != null && file.getFileExtension().equals(SolutionSerializer.JS_FILE_EXTENSION_WITHOUT_DOT)) ||
					file.getName().endsWith(SolutionSerializer.FORM_FILE_EXTENSION)))
			{
				return true;
			}
			if (folder.getName().equals(SolutionSerializer.MEDIAS_DIR) || folder.getName().equals(SolutionSerializer.VALUELISTS_DIR) ||
				folder.getName().equals(SolutionSerializer.RELATIONS_DIR))
			{
				return true;
			}
		}
		if (resources.size() == 2 && resources.get(0) instanceof IProject && resources.get(1) instanceof IFile &&
			resources.get(1).getFileExtension() != null &&
			resources.get(1).getFileExtension().equals(SolutionSerializer.JS_FILE_EXTENSION_WITHOUT_DOT))
		{
			return true;
		}
		if (resources.size() == 4 && resources.get(0) instanceof IProject && resources.get(1) instanceof IFolder &&
			resources.get(1).getName().equals(SolutionSerializer.DATASOURCES_DIR_NAME) && resources.get(2) instanceof IFolder &&
			resources.get(3) instanceof IFile)
		{
			return true;
		}
		return false;
	}

	public static boolean checkIncrementalBuild(List<IResource> resources)
	{
		IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
		if (resources.size() == 3 && resources.get(0) instanceof IProject && resources.get(1) instanceof IFolder && resources.get(2) instanceof IFile)
		{
			IProject project = (IProject)resources.get(0);
			IFolder folder = (IFolder)resources.get(1);
			IFile file = (IFile)resources.get(2);
			if (folder.getName().equals(SolutionSerializer.FORMS_DIR) &&
				((file.getFileExtension() != null && file.getFileExtension().equals(SolutionSerializer.JS_FILE_EXTENSION_WITHOUT_DOT)) ||
					file.getName().endsWith(SolutionSerializer.FORM_FILE_EXTENSION)) &&
				servoyModel.isSolutionActive(project.getName()))
			{
				// clear deserializer error for this js file as this change may have fixed it
				if (file.getFileExtension().equals(SolutionSerializer.JS_FILE_EXTENSION_WITHOUT_DOT))
				{
					ServoyBuilder.deleteMarkers(file, ServoyBuilder.PROJECT_DESERIALIZE_MARKER_TYPE);
				}
				// form js file is changed
				// get form
				// get child forms
				ServoyProject servoyProject = servoyModel.getServoyProject(project.getName());
				FlattenedSolution fs = getReferenceFlattenedSolution(servoyProject.getSolution());

				checkServiceSolutionMustAuthenticate(servoyModel, servoyProject.getSolution(), project);
				ServoyBuilder.checkPersistDuplicateName();
				ServoyBuilder.checkPersistDuplicateUUID();
				String formName = file.getName().substring(0, file.getName().length() - file.getFileExtension().length() - 1);
				Form form = fs.getForm(formName);
				if (form != null)
				{
					List<Form> affectedForms = new ArrayList<Form>();
					List<Form> formDependencies = BuilderDependencies.getInstance().getFormDependencies(form);
					if (formDependencies != null)
					{
						affectedForms.addAll(formDependencies);
						for (Form dependency : formDependencies.toArray(new Form[0]))
						{
							ServoyFormBuilder.deleteMarkers(dependency);
							BuilderDependencies.getInstance().removeForm(dependency);
						}
					}
					affectedForms.add(form);
					ServoyFormBuilder.deleteMarkers(form);
					BuilderDependencies.getInstance().removeForm(form);
					Iterator<Form> it = fs.getForms(false);
					while (it.hasNext())
					{
						Form currentForm = it.next();
						Form parentForm = currentForm.getExtendsForm();
						while (parentForm != null)
						{
							if (parentForm == form)
							{
								if (!affectedForms.contains(currentForm)) affectedForms.add(currentForm);
								ServoyFormBuilder.deleteMarkers(currentForm);
								BuilderDependencies.getInstance().removeForm(currentForm);
							}
							parentForm = parentForm.getExtendsForm();
						}
					}
					Set<UUID> methodsParsed = new HashSet<UUID>();
					Map<Form, Boolean> formsAbstractChecked = new HashMap<Form, Boolean>();
					for (Form currentForm : affectedForms)
					{
						ServoyFormBuilder.addFormMarkers(servoyProject, currentForm, methodsParsed, formsAbstractChecked);
					}
					return true;
				}

			}
			if (folder.getName().equals(SolutionSerializer.MEDIAS_DIR) &&
				servoyModel.isSolutionActive(project.getName()))
			{
				return ServoyMediaBuilder.addMediaMarkers(project, file);
			}
			if (folder.getName().equals(SolutionSerializer.VALUELISTS_DIR) &&
				servoyModel.isSolutionActive(project.getName()))
			{
				return ServoyValuelistBuilder.addValuelistMarkers(project, file);
			}
			if (folder.getName().equals(SolutionSerializer.RELATIONS_DIR) &&
				servoyModel.isSolutionActive(project.getName()))
			{
				return ServoyRelationBuilder.addRelationMarkers(project, file);
			}
		}
		if (resources.size() == 2 && resources.get(0) instanceof IProject && resources.get(1) instanceof IFile &&
			resources.get(1).getFileExtension() != null &&
			resources.get(1).getFileExtension().equals(SolutionSerializer.JS_FILE_EXTENSION_WITHOUT_DOT) &&
			servoyModel.isSolutionActive(resources.get(0).getName()))
		{
			IProject project = (IProject)resources.get(0);
			IFile file = (IFile)resources.get(1);
			// clear deserializer error for this js file as this change may have fixed it
			ServoyBuilder.deleteMarkers(file, ServoyBuilder.PROJECT_DESERIALIZE_MARKER_TYPE);
			ServoyProject servoyProject = servoyModel.getServoyProject(project.getName());
			String scopeName = file.getName().substring(0, file.getName().length() - SolutionSerializer.JS_FILE_EXTENSION.length());
			List<IPersist> persists = BuilderDependencies.getInstance().getScopeDependency(scopeName);
			BuilderDependencies.getInstance().removeScopeDependencies(scopeName);
			ServoyBuilder.checkPersistDuplicateName();
			ServoyBuilder.checkPersistDuplicateUUID();
			ServoyBuilder.checkDuplicateScopes(file);

			try
			{
				if (file.exists())
				{
					file.deleteMarkers(ServoyBuilder.SCRIPT_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
			Iterator<ScriptMethod> it = servoyModel.getFlattenedSolution().getScriptMethods(scopeName, false);
			while (it.hasNext())
			{
				ScriptMethod scriptMethod = it.next();
				addScriptMethodErrorMarkers(file, scriptMethod);
			}
			if (persists != null)
			{
				Set<UUID> methodsParsed = new HashSet<UUID>();
				Map<Form, Boolean> formsAbstractChecked = new HashMap<Form, Boolean>();
				for (IPersist persist : persists)
				{
					if (persist instanceof Form)
					{
						ServoyFormBuilder.deleteMarkers((Form)persist);
						ServoyFormBuilder.addFormMarkers(servoyProject, (Form)persist, methodsParsed, formsAbstractChecked);
					}
					if (persist instanceof ValueList)
					{
						ServoyValuelistBuilder.addValuelistMarkers(servoyProject, (ValueList)persist, servoyModel.getFlattenedSolution());
					}
					if (persist instanceof Relation)
					{
						ServoyRelationBuilder.addRelationMarkers(servoyProject, (Relation)persist);
					}
				}
			}
			return true;
		}
		if (resources.size() == 4 && resources.get(0) instanceof IProject && resources.get(1) instanceof IFolder &&
			resources.get(1).getName().equals(SolutionSerializer.DATASOURCES_DIR_NAME) && resources.get(2) instanceof IFolder &&
			resources.get(3) instanceof IFile)
		{
			String datasource = ResourcesUtils.getParentDatasource((IFile)resources.get(3), false);
			if (datasource != null)
			{
				List<IPersist> persists = BuilderDependencies.getInstance().getDatasourceDependency(datasource);
				BuilderDependencies.getInstance().removeDatasourceDependencies(datasource);
				ServoyBuilder.checkPersistDuplicateName();
				ServoyBuilder.checkPersistDuplicateUUID();

				if (persists != null)
				{
					Set<UUID> methodsParsed = new HashSet<UUID>();
					Map<Form, Boolean> formsAbstractChecked = new HashMap<Form, Boolean>();
					for (IPersist persist : persists)
					{
						ServoyProject servoyProject = servoyModel.getServoyProject(persist.getRootObject().getName());
						if (persist instanceof Form)
						{
							ServoyFormBuilder.deleteMarkers((Form)persist);
							ServoyFormBuilder.addFormMarkers(servoyProject, (Form)persist, methodsParsed, formsAbstractChecked);
						}
						if (persist instanceof ValueList)
						{
							ServoyValuelistBuilder.addValuelistMarkers(servoyProject, (ValueList)persist, servoyModel.getFlattenedSolution());
						}
						if (persist instanceof Relation)
						{
							ServoyRelationBuilder.addRelationMarkers(servoyProject, (Relation)persist);
						}
					}
				}
				return true;
			}
		}
		return false;
	}

	public static void checkServiceSolutionMustAuthenticate(IServoyModel servoyModel, Solution solution, IProject project)
	{
		try
		{
			project.deleteMarkers(ServoyBuilder.SERVICE_MUST_AUTHENTICATE_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
		if (servoyModel.getActiveProject().getSolution().getName().equals(solution.getName()))
		{
			//skipping modules when checking for web service solutions
			if (solution.getMustAuthenticate())
			{
				//check if solution is used as web service
				Iterator<Form> formsIt = Solution.getForms(solution.getAllObjectsAsList(), null, false);
				boolean isServiceSolution = false;
				while (formsIt.hasNext() && !isServiceSolution)
				{
					Iterator<ScriptMethod> methodIt = formsIt.next().getScriptMethods(false);
					while (methodIt.hasNext())
					{
						String methodName = methodIt.next().getName();
						if (methodName.equals("ws_read") || methodName.equals("ws_create") || methodName.equals("ws_delete") ||
							methodName.equals("ws_update") || methodName.equals("ws_authenticate") || methodName.equals("ws_response_headers"))
						{
							isServiceSolution = true;
							break;
						}
					}
				}
				if (isServiceSolution)
				{
					//create warning marker
					ServoyMarker mk = MarkerMessages.SolutionUsedAsWebServiceMustAuthenticateProblem.fill(solution.getName());
					ServoyBuilder.addMarker(project, mk.getType(), mk.getText(), -1, ServoyBuilder.SOLUTION_USED_AS_WEBSERVICE_MUSTAUTHENTICATE_PROBLEM,
						IMarker.PRIORITY_HIGH,
						null, solution);
				}
			}
		}
	}

	public static void addMobileReservedWordsVariable(IProject project, IPersist o)
	{
		if (o instanceof ScriptVariable &&
			ServoyModelFinder.getServoyModel().getFlattenedSolution().getSolution().getSolutionType() == SolutionMetaData.MOBILE &&
			Ident.checkIfReservedBrowserWindowObjectWord(((ScriptVariable)o).getName()))
		{
			Pair<String, String> pathPair = SolutionSerializer.getFilePath(o, false);
			String path = ((AbstractBase)o).getSerializableRuntimeProperty(IScriptProvider.FILENAME);
			IResource file = project;
			if (path != null && !"".equals(path))
			{
				file = ServoyBuilder.getEclipseResourceFromJavaIO(new java.io.File(path), project);
				if (file != null) path = file.getProjectRelativePath().toString();
			}
			if (path == null || "".equals(path)) path = pathPair.getRight();

			ServoyMarker mk = MarkerMessages.ReservedWindowObjectProperty.fill(((ScriptVariable)o).getName());

			ServoyBuilder.addMarker(file, mk.getType(), mk.getText(), -1, ServoyBuilder.RESERVED_WINDOW_OBJECT_PROPERTY, IMarker.PRIORITY_NORMAL, path, o);
		}
	}

	public static void addNullReferenceMarker(IResource markerResource, IPersist o, IPersist foundPersist, IPersist context, ContentSpec.Element element)
	{
		if (foundPersist == null && !element.getName().equals(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName()))
		{
			String elementName = null;
			String inForm = null;
			if (o instanceof ISupportName && !(o instanceof Form) && (((ISupportName)o).getName() != null))
				elementName = ((ISupportName)o).getName();
			if (context instanceof Form)
			{
				inForm = ((Form)context).getName();
			}

			ServoyMarker mk;
			Pair<String, ProblemSeverity> problemPair;
			boolean addMarker = true;
			if (elementName == null)
			{
				if (inForm == null)
				{
					mk = MarkerMessages.PropertyTargetNotFound.fill(element.getName());
					problemPair = ServoyBuilder.SOLUTION_PROPERTY_TARGET_NOT_FOUND;
				}
				else
				{
					mk = MarkerMessages.PropertyInFormTargetNotFound.fill(element.getName(), inForm);
					problemPair = ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND;
				}
			}
			else
			{
				if (inForm == null)
				{
					mk = MarkerMessages.PropertyOnElementTargetNotFound.fill(element.getName(), elementName);
					problemPair = ServoyBuilder.SOLUTION_PROPERTY_TARGET_NOT_FOUND;
					IMarker marker = ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, problemPair, IMarker.PRIORITY_LOW, null,
						o);
					try
					{
						marker.setAttribute("Uuid", o.getUUID().toString());
						marker.setAttribute("SolutionName", elementName);
						marker.setAttribute("PropertyName", element.getName());
						marker.setAttribute("DisplayName", RepositoryHelper.getDisplayName(element.getName(), o.getClass()));
					}
					catch (CoreException e)
					{
						Debug.error(e);
					}
					addMarker = false;
				}
				else
				{
					mk = MarkerMessages.PropertyOnElementInFormTargetNotFound.fill(element.getName(), elementName, inForm);
					problemPair = ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND;
				}
			}
			if (BaseComponent.isEventProperty(element.getName()) || BaseComponent.isCommandProperty(element.getName()))
			{
				// TODO: this is a place where the same marker appears in more than one category...
				IMarker marker = ServoyBuilder.addMarker(markerResource,
					BaseComponent.isEventProperty(element.getName()) ? ServoyBuilder.INVALID_EVENT_METHOD : ServoyBuilder.INVALID_COMMAND_METHOD,
					mk.getText(), -1, problemPair, IMarker.PRIORITY_LOW, null, o);
				if (marker != null)
				{
					try
					{
						marker.setAttribute("EventName", element.getName());
						if (context instanceof Form)
						{
							marker.setAttribute("DataSource", ((Form)context).getDataSource());
							marker.setAttribute("ContextTypeId", context.getTypeID());
						}
						if (context instanceof TableNode)
						{
							marker.setAttribute("DataSource", ((TableNode)context).getDataSource());
							marker.setAttribute("ContextTypeId", context.getTypeID());
						}
					}
					catch (CoreException e)
					{
						Debug.error(e);
					}
				}
			}
			else if (addMarker)
			{
				ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, problemPair, IMarker.PRIORITY_LOW, null, o);
			}
		}
	}

	public static void addNotAccessibleMethodMarkers(IResource markerResource, IPersist o, IPersist foundPersist, IPersist context, ContentSpec.Element element,
		FlattenedSolution flattenedSolution)
	{
		if (foundPersist instanceof ScriptMethod && context != null)
		{
			ScriptMethod scriptMethod = (ScriptMethod)foundPersist;
			if (scriptMethod.getParent() != context && scriptMethod.isPrivate())
			{
				String elementName = null;
				String inForm = null;
				if (o instanceof ISupportName && !(o instanceof Form) && (((ISupportName)o).getName() != null))
					elementName = ((ISupportName)o).getName();
				if (context instanceof Form) inForm = ((Form)context).getName();
				ServoyMarker mk;
				Pair<String, ProblemSeverity> problemPair;
				String prefix = "";
				if (scriptMethod.getScopeName() != null)
				{
					prefix = scriptMethod.getScopeName() + '.';
				}
				else if (scriptMethod.getParent() instanceof TableNode)
				{
					prefix = ((TableNode)scriptMethod.getParent()).getDataSource() + '/';
				}
				if (elementName == null)
				{
					if (inForm == null)
					{
						mk = MarkerMessages.PropertyTargetNotAccessible.fill(element.getName(),
							prefix + scriptMethod.getName());
						problemPair = ServoyBuilder.SOLUTION_PROPERTY_TARGET_NOT_FOUND;
					}
					else
					{
						mk = MarkerMessages.PropertyInFormTargetNotAccessible.fill(element.getName(), inForm,
							prefix + scriptMethod.getName());
						problemPair = ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND;
					}
				}
				else
				{
					if (inForm == null)
					{
						mk = MarkerMessages.PropertyOnElementTargetNotAccessible.fill(element.getName(), elementName,
							prefix + scriptMethod.getName());
						problemPair = ServoyBuilder.SOLUTION_PROPERTY_TARGET_NOT_FOUND;
					}
					else
					{
						mk = MarkerMessages.PropertyOnElementInFormTargetNotAccessible.fill(element.getName(), elementName,
							inForm, prefix + scriptMethod.getName());
						problemPair = ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND;
					}
				}
				ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, problemPair, IMarker.PRIORITY_LOW, null, o);
			}
			else if (context instanceof Form)
			{
				Form parentForm = (Form)context;
				Form methodForm = (Form)scriptMethod.getAncestor(IRepository.FORMS);
				if (methodForm != null &&
					!ServoyBuilder.getPersistFlattenedSolution(parentForm, flattenedSolution).getFormHierarchy(
						parentForm).contains(methodForm))
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
					ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_PROPERTY_METHOD_NOT_ACCESIBLE,
						IMarker.PRIORITY_LOW,
						null, o);
				}
				else if (scriptMethod.isDeprecated())
				{
					ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedFunction.fill(scriptMethod.getDisplayName() + "()",
						"form " + parentForm.getName(), element.getName());
					ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
						IMarker.PRIORITY_NORMAL, null, o);
				}

			}
			else if (scriptMethod.isDeprecated())
			{
				ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedFunction.fill(scriptMethod.getDisplayName() + "()",
					"solution " + flattenedSolution.getName(), element.getName());
				ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
					IMarker.PRIORITY_NORMAL, null, o);
			}
		}
	}

	public static void addMethodParseErrorMarkers(IResource markerResource, IPersist o, IPersist foundPersist, IPersist context, ContentSpec.Element element,
		Set<UUID> methodsParsed,
		Map<IPersist, Boolean> methodsReferences)
	{
		if (BaseComponent.isEventProperty(element.getName()) && !"onOpenMethodID".equals(element.getName()) &&
			(foundPersist instanceof ScriptMethod) && !methodsParsed.contains(foundPersist.getUUID()))
		{
			methodsParsed.add(foundPersist.getUUID());
			ScriptMethod eventMethod = (ScriptMethod)foundPersist;

			if (eventMethod != null && (eventMethod.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS) == null ||
				eventMethod.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS).length == 0) && eventMethod.getDeclaration().contains("arguments"))
			{
				int offset = ScriptingUtils.getArgumentsUsage(eventMethod.getDeclaration());
				if (offset >= 0)
				{
					ServoyMarker mk = MarkerMessages.MethodEventParameters;
					IMarker marker = ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), eventMethod.getLineNumberOffset() + offset,
						ServoyBuilder.METHOD_EVENT_PARAMETERS,
						IMarker.PRIORITY_NORMAL, null, eventMethod);
					if (marker != null)
					{
						try
						{
							marker.setAttribute("EventName", element.getName());
						}
						catch (Exception ex)
						{
							ServoyLog.logError(ex);
						}
					}
				}
			}
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
					Pair<String, ProblemSeverity> problemSeverity = null;
					if (o instanceof ISupportName)
					{
						elementName = ((ISupportName)o).getName();
						if (elementName == null || "".equals(elementName)) elementName = "<no name>";
						mk = MarkerMessages.PropertyMultipleMethodsOnSameElement.fill(elementName);
						problemSeverity = ServoyBuilder.FORM_PROPERTY_MULTIPLE_METHODS_ON_SAME_ELEMENT;
					}
					else if (o instanceof TableNode)
					{
						elementName = ((TableNode)o).getTableName();
						mk = MarkerMessages.PropertyMultipleMethodsOnSameTable.fill(elementName);
						problemSeverity = ServoyBuilder.PROPERTY_MULTIPLE_METHODS_ON_SAME_TABLE;
					}
					methodsReferences.put(foundPersist, Boolean.FALSE);
					ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, problemSeverity, IMarker.PRIORITY_LOW, null, o);
				}
			}
			else
			{
				methodsReferences.put(foundPersist, Boolean.TRUE);
			}
			if (o instanceof AbstractBase)
			{
				Pair<List<String>, List<Object>> instanceParameters = ((AbstractBase)o).getFlattenedMethodParameters(
					element.getName());
				MethodArgument[] methodArguments = ((ScriptMethod)foundPersist).getRuntimeProperty(
					IScriptProvider.METHOD_ARGUMENTS);
				if (instanceParameters != null && instanceParameters.getRight() != null)
				{
					boolean signatureMismatch = false;
					if (instanceParameters.getLeft() != null)
					{
						// check for parameter name differences
						for (int i = 0; i < instanceParameters.getLeft().size(); i++)
						{
							String name = instanceParameters.getLeft().get(i);
							if (i >= methodArguments.length)
							{
								signatureMismatch = true;
								break;
							}
							else if (!name.equals(methodArguments[i].getName()))
							{
								signatureMismatch = true;
								break;
							}
						}
					}
					if (instanceParameters.getRight().size() > methodArguments.length)
					{
						signatureMismatch = true;
					}
					// add marker if signature mismach
					if (signatureMismatch)
					{
						String handlerName = element.getName().substring(0, (element.getName().indexOf("MethodID") > 0
							? element.getName().indexOf("MethodID") : element.getName().length()));

						String functionDefinitionName = ((ScriptMethod)foundPersist).getName();
						if (((ScriptMethod)foundPersist).getScopeName() != null)
						{
							functionDefinitionName = ((ScriptMethod)foundPersist).getScopeName() + "." + functionDefinitionName;
						}
						else
						{
							functionDefinitionName = "forms." +
								((ISupportName)((ScriptMethod)foundPersist).getParent()).getName() + "." +
								functionDefinitionName;
						}

						String componentName = "";
						if (o instanceof ISupportName && ((ISupportName)o).getName() != null &&
							((ISupportName)o).getName().length() > 1)
						{
							componentName = " \"" + ((ISupportName)o).getName() + "\"";
						}
						ServoyMarker mk = MarkerMessages.EventHandlerSignatureMismatch.fill(functionDefinitionName, handlerName,
							RepositoryHelper.getObjectTypeName(o.getTypeID()), componentName);
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.METHOD_NUMBER_OF_ARGUMENTS_MISMATCH,
							IMarker.PRIORITY_LOW, null, o);
					}
				}
			}
		}
	}

	public static void addScriptMethodErrorMarkers(IResource markerResource, ScriptMethod method)
	{
		if (ScriptingUtils.isMissingReturnDocs(method))
		{
			ServoyMarker mk = MarkerMessages.MethodNoReturn.fill(method.getName());
			IMarker marker = ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), method.getLineNumberOffset(),
				ServoyBuilder.METHOD_NO_RETURN,
				IMarker.PRIORITY_NORMAL, null, method);
			try
			{
				if (marker != null)
				{
					marker.setAttribute("Uuid", method.getUUID().toString());
					marker.setAttribute("SolutionName", method.getRootObject().getName());
				}
			}
			catch (CoreException e)
			{
				Debug.error(e);
			}
		}
	}

	public static boolean formCanBeInstantiated(Form form, FlattenedSolution flattenedSolution, Map<Form, Boolean> checked)
	{
		Boolean canBeInstantiated = checked.get(form);
		if (canBeInstantiated == null)
		{
			canBeInstantiated = Boolean.valueOf(flattenedSolution.formCanBeInstantiated(form));
			checked.put(form, canBeInstantiated);
		}
		return canBeInstantiated.booleanValue();
	}

	public static IResource getPersistResource(IPersist persist)
	{
		Pair<String, String> formFilePath = SolutionSerializer.getFilePath(persist, false);
		return ResourcesPlugin.getWorkspace().getRoot()
			.getFile(new Path(formFilePath.getLeft() + formFilePath.getRight()));
	}

	public static FlattenedSolution getReferenceFlattenedSolution(Solution solution)
	{
		if (SolutionMetaData.isImportHook(solution.getSolutionMetaData()))
		{
			// we have to build a FS for hooks
			ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solution.getName());
			if (servoyProject != null)
			{
				return servoyProject.getFlattenedSolution();
			}
		}
		return ServoyModelFinder.getServoyModel().getFlattenedSolution();
	}
}
