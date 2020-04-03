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

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.ICustomType;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.MarkerMessages.ServoyMarker;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.FormElementHelper.FormComponentCache;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedPropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.ValueListConfig;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.server.ngclient.property.types.TagStringPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser;
import com.servoy.j2db.util.FormatParser.ParsedFormat;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.RoundHalfUpDecimalFormat;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.keyword.Ident;

/**
 * @author lvostinar
 *
 */
public class ServoyBuilderUtils
{

	public static boolean checkIncrementalBuild(List<IResource> resources)
	{
		if (checkFormScripting(resources))
		{
			return true;
		}
		return false;
	}

	public static boolean checkFormScripting(List<IResource> resources)
	{
		if (resources.size() == 3 && resources.get(0) instanceof IProject && resources.get(1) instanceof IFolder && resources.get(2) instanceof IFile)
		{
			IProject project = (IProject)resources.get(0);
			IFolder folder = (IFolder)resources.get(1);
			IFile file = (IFile)resources.get(2);
			IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
			if (folder.getName().equals(SolutionSerializer.FORMS_DIR) && file.getFileExtension().equals(SolutionSerializer.JS_FILE_EXTENSION_WITHOUT_DOT) &&
				servoyModel.isSolutionActive(project.getName()))
			{
				// form js file is changed
				// get form
				// get child forms
				ServoyProject servoyProject = servoyModel.getServoyProject(project.getName());
				FlattenedSolution fs = servoyProject.getFlattenedSolution();

				checkServiceSolutionMustAuthenticate(servoyModel, servoyProject.getSolution(), project);

				String formName = file.getName().substring(0, file.getName().length() - SolutionSerializer.JS_FILE_EXTENSION.length());
				Form form = fs.getForm(formName);
				List<Form> affectedForms = new ArrayList<Form>();
				affectedForms.add(form);

				Iterator<Form> it = fs.getForms(false);
				while (it.hasNext())
				{
					Form currentForm = it.next();
					Form parentForm = currentForm.getExtendsForm();
					while (parentForm != null)
					{
						if (parentForm == form)
						{
							affectedForms.add(currentForm);
						}
						parentForm = parentForm.getExtendsForm();
					}
				}
				Set<UUID> methodsParsed = new HashSet<UUID>();
				for (Form currentForm : affectedForms)
				{
					currentForm.acceptVisitor(new IPersistVisitor()
					{
						@Override
						public Object visit(IPersist o)
						{
							Map<IPersist, Boolean> methodsReferences = new HashMap<IPersist, Boolean>();
							IPersist context = o.getAncestor(IRepository.FORMS);
							try
							{
								final Map<String, Method> methods = ((EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository())
									.getGettersViaIntrospection(
										o);
								for (ContentSpec.Element element : Utils.iterate(
									((EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository()).getContentSpec().getPropertiesForObjectType(
										o.getTypeID())))
								{
									// Don't set meta data properties.
									if (element.isMetaData() || element.isDeprecated()) continue;

									if (o instanceof AbstractBase && !((AbstractBase)o).hasProperty(element.getName()))
									{
										// property is not defined on object itself, check will be done on super element that defines the property
										continue;
									}

									if (!BaseComponent.isEventOrCommandProperty(element.getName()))
									{
										// only a method reference could have changed
										continue;
									}
									// Get default property value as an object.
									final int typeId = element.getTypeID();

									if (typeId == IRepository.ELEMENTS)
									{
										final Method method = methods.get(element.getName());
										Object property_value = method.invoke(o, new Object[] { });
										final int element_id = Utils.getAsInteger(property_value);
										if (element_id > 0)
										{
											ScriptMethod foundPersist = fs.getScriptMethod(element_id);
											ServoyBuilderUtils.addNullReferenceMarker(project, o, foundPersist, context, element);
											ServoyBuilderUtils.addNotAccessibleMethodMarkers(project, o, foundPersist, context, element, fs);
											ServoyBuilderUtils.addMethodParseErrorMarkers(project, o, foundPersist, element, methodsParsed,
												methodsReferences);
										}
									}
								}
							}
							catch (Exception e)
							{
								throw new RuntimeException(e);
							}

							addWebComponentMissingHandlers(project, fs, o);

							if (o instanceof WebComponent)
							{
								WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState()
									.getWebComponentSpecification(((WebComponent)o).getTypeName());
								if (spec != null)
								{
									Collection<PropertyDescription> properties = spec.getProperties(FormComponentPropertyType.INSTANCE);
									if (properties.size() > 0)
									{
										FormElement formComponentEl = FormElementHelper.INSTANCE.getFormElement((WebComponent)o, fs, null, true);
										for (PropertyDescription pd : properties)
										{
											String datasource = null;
											Object propertyValue = formComponentEl.getPropertyValue(pd.getName());
											Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, fs);
											if (frm == null) continue;

											FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(formComponentEl, pd,
												(JSONObject)propertyValue, frm, fs);
											for (FormElement element : cache.getFormComponentElements())
											{
												checkDataProviders(servoyProject, element.getPersistIfAvailable(), context, datasource, fs);
											}
										}
									}
								}
							}
							checkDataProviders(servoyProject, o, context, null, fs);
							if (o instanceof Form)
							{
								addFormVariablesHideTableColumn(project, (Form)o, fs.getTable(((Form)o).getDataSource()));
							}
							addMobileReservedWordsVariable(project, o);
							return IPersistVisitor.CONTINUE_TRAVERSAL;
						}
					});
				}
				return true;
			}
		}
		return false;
	}

	public static void checkServiceSolutionMustAuthenticate(IServoyModel servoyModel, Solution solution, IProject project)
	{
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

	public static void addNullReferenceMarker(IProject project, IPersist o, IPersist foundPersist, IPersist context, ContentSpec.Element element)
	{
		if (foundPersist == null && !element.getName().equals(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName()))
		{
			String elementName = null;
			String inForm = null;
			if (o instanceof ISupportName && !(o instanceof Form) && (((ISupportName)o).getName() != null))
				elementName = ((ISupportName)o).getName();
			if (context instanceof Form) inForm = ((Form)context).getName();
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
					IMarker marker = ServoyBuilder.addMarker(project, mk.getType(), mk.getText(), -1, problemPair, IMarker.PRIORITY_LOW, null,
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
				IMarker marker = ServoyBuilder.addMarker(project,
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
				ServoyBuilder.addMarker(project, mk.getType(), mk.getText(), -1, problemPair, IMarker.PRIORITY_LOW, null, o);
			}
		}
	}

	public static void addNotAccessibleMethodMarkers(IProject project, IPersist o, IPersist foundPersist, IPersist context, ContentSpec.Element element,
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
				ServoyBuilder.addMarker(project, mk.getType(), mk.getText(), -1, problemPair, IMarker.PRIORITY_LOW, null, o);
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
					ServoyBuilder.addMarker(project, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_PROPERTY_METHOD_NOT_ACCESIBLE, IMarker.PRIORITY_LOW,
						null, o);
				}
				else if (scriptMethod.isDeprecated())
				{
					ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedFunction.fill(scriptMethod.getDisplayName() + "()",
						"form " + parentForm.getName(), element.getName());
					ServoyBuilder.addMarker(project, mk.getType(), mk.getText(), -1, ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
						IMarker.PRIORITY_NORMAL, null, o);
				}

			}
			else if (scriptMethod.isDeprecated())
			{
				ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedFunction.fill(scriptMethod.getDisplayName() + "()",
					"solution " + project.getName(), element.getName());
				ServoyBuilder.addMarker(project, mk.getType(), mk.getText(), -1, ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
					IMarker.PRIORITY_NORMAL, null, o);
			}
		}
	}

	public static void addMethodParseErrorMarkers(IProject project, IPersist o, IPersist foundPersist, ContentSpec.Element element, Set<UUID> methodsParsed,
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
					IMarker marker = ServoyBuilder.addMarker(project, mk.getType(), mk.getText(), eventMethod.getLineNumberOffset() + offset,
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
					ServoyBuilder.addMarker(project, mk.getType(), mk.getText(), -1, problemSeverity, IMarker.PRIORITY_LOW, null, o);
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
						ServoyBuilder.addMarker(project, mk.getType(), mk.getText(), -1, ServoyBuilder.METHOD_NUMBER_OF_ARGUMENTS_MISMATCH,
							IMarker.PRIORITY_LOW, null, o);
					}
				}
			}
		}
	}

	public static void addWebComponentMissingHandlers(IProject project, FlattenedSolution flattenedSolution, IPersist o)
	{
		if (o instanceof WebComponent)
		{
			WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebComponentSpecification(((WebComponent)o).getTypeName());
			if (spec != null && spec.getHandlers() != null)
			{
				for (String handler : spec.getHandlers().keySet())
				{
					UUID uuid = Utils.getAsUUID(((WebComponent)o).getProperty(handler), false);
					if (uuid != null)
					{
						ScriptMethod scriptMethod = flattenedSolution.getScriptMethod(uuid.toString());
						if (scriptMethod == null)
						{
							ServoyMarker mk = MarkerMessages.PropertyOnElementInFormTargetNotFound.fill(handler, ((WebComponent)o).getName(),
								o.getAncestor(IRepository.FORMS));
							IMarker marker = ServoyBuilder.addMarker(project, ServoyBuilder.INVALID_EVENT_METHOD, mk.getText(), -1,
								ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND,
								IMarker.PRIORITY_LOW, null, o);
							if (marker != null)
							{
								try
								{
									marker.setAttribute("EventName", handler);
								}
								catch (Exception ex)
								{
									ServoyLog.logError(ex);
								}
							}
						}
					}
					WebObjectFunctionDefinition handlerDefinition = spec.getHandler(handler);
					List<Object> instanceMethodArguments = ((WebComponent)o).getFlattenedMethodArguments(handlerDefinition.getName());
					if (instanceMethodArguments != null && instanceMethodArguments.size() > 0 &&
						handlerDefinition.getParameters().size() >= instanceMethodArguments.size())
					{
						ServoyMarker mk = MarkerMessages.Parameters_Mismatch.fill(((WebComponent)o).getName(), handler);
						ServoyBuilder.addMarker(project, mk.getType(), mk.getText(), -1, ServoyBuilder.PARAMETERS_MISMATCH_SEVERITY, IMarker.PRIORITY_NORMAL,
							null, o);
					}
				}

			}
		}
	}

	public static void addFormVariablesHideTableColumn(IProject project, Form form, ITable table)
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
					ServoyBuilder.addMarker(project, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_VARIABLE_TYPE_COL, IMarker.PRIORITY_NORMAL, null, var);
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

	public static void checkDataProviders(ServoyProject project, final IPersist o, IPersist context, String datasource, FlattenedSolution flattenedSolution)
	{
		String id = null;
		if (o instanceof ISupportDataProviderID)
		{
			id = ((ISupportDataProviderID)o).getDataProviderID();
		}
		else if (o instanceof WebComponent || o instanceof WebCustomType)
		{
			Collection<PropertyDescription> dpProperties = new ArrayList<PropertyDescription>();

			if (o instanceof WebComponent)
			{
				WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebComponentSpecification(((WebComponent)o).getTypeName());
				if (spec != null)
				{
					dpProperties.addAll(spec.getProperties().values());
				}
			}
			else
			{
				WebCustomType customType = (WebCustomType)o;
				WebComponent parent = (WebComponent)customType.getAncestor(IRepository.WEBCOMPONENTS);
				WebObjectSpecification parentSpec = WebComponentSpecProvider.getSpecProviderState().getWebComponentSpecification(parent.getTypeName());
				if (parentSpec != null)
				{
					PropertyDescription cpd = ((ICustomType< ? >)parentSpec.getDeclaredCustomObjectTypes().get(
						customType.getTypeName())).getCustomJSONTypeDefinition();
					if (cpd != null)
					{
						dpProperties.addAll(cpd.getProperties().values());
					}
				}
			}

			for (PropertyDescription pd : dpProperties)
			{
				if (pd.getType() instanceof DataproviderPropertyType || pd.getType() instanceof FoundsetLinkedPropertyType< ? , ? > ||
					pd.getType() instanceof FoundsetPropertyType)
				{

					Object propertyValue = ((IBasicWebObject)o).getProperty(pd.getName());
					if (propertyValue != null)
					{
						if ((pd.getType() instanceof FoundsetLinkedPropertyType< ? , ? > &&
							((FoundsetLinkedConfig)pd.getConfig()).getWrappedPropertyDescription().getType() instanceof TagStringPropertyType))
						{
							TagStringPropertyType wrappedPd = (TagStringPropertyType)((FoundsetLinkedConfig)pd.getConfig())
								.getWrappedPropertyDescription().getType();
							TargetDataLinks links = wrappedPd.getDataLinks((String)propertyValue, pd, flattenedSolution,
								new FormElement((IFormElement)o.getParent(), flattenedSolution, new PropertyPath(), true));
							if (!TargetDataLinks.NOT_LINKED_TO_DATA.equals(links))
							{
								for (String dp : links.dataProviderIDs)
								{
									checkDataProvider(project, flattenedSolution, o, context, dp, pd, datasource);
								}
							}
							continue;
						}
						else if (pd.getType() instanceof FoundsetPropertyType)
						{
							if (propertyValue instanceof JSONObject)
							{
								JSONObject val = (JSONObject)propertyValue;
								FlattenedSolution persistFlattenedSolution = ServoyBuilder.getPersistFlattenedSolution(o, flattenedSolution);

								//first check if the foundset is valid
								boolean invalid = false;
								String fs = val.optString(FoundsetPropertyType.FOUNDSET_SELECTOR);
								if (!"".equals(fs)) //Form foundset, no need to check
								{
									if (DataSourceUtils.isDatasourceUri(fs))
									{
										ITable table = persistFlattenedSolution.getTable(fs);
										invalid = table == null;
										if (table != null && context instanceof Form)
										{
											Form form = (Form)context;
											Iterator<ScriptVariable> iterator = form.getScriptVariables(false);
											while (iterator.hasNext())
											{
												ScriptVariable var = iterator.next();
												if (table.getColumn(var.getName()) != null)
												{
													ServoyMarker mk = MarkerMessages.FormVariableTableColFromComponent.fill(form.getName(),
														var.getName(), table.getName(),
														((ISupportName)o).getName() != null ? ((ISupportName)o).getName() : "", pd.getName());
													ServoyBuilder.addMarker(project.getProject(), mk.getType(), mk.getText(), -1,
														ServoyBuilder.FORM_VARIABLE_TYPE_COL,
														IMarker.PRIORITY_NORMAL, null, o);
												}
											}
										}
									}
									else
									{
										invalid = persistFlattenedSolution.getRelationSequence(fs) == null;
										if (invalid)
										{
											IServiceProvider serviceProvider = ServoyModelFinder.getServiceProvider();
											try
											{
												if (serviceProvider != null &&
													serviceProvider.getFoundSetManager().getNamedFoundSet(fs) != null)
												{
													invalid = false;
												}
											}
											catch (ServoyException e)
											{
												ServoyLog.logError(e);
											}
										}
									}
									if (invalid)
									{
										String comp_name = ((ISupportName)o).getName() != null ? ((ISupportName)o).getName() : "";
										ServoyMarker mk = MarkerMessages.ComponentInvalidFoundset.fill(fs, comp_name);
										ServoyBuilder.addMarker(project.getProject(), mk.getType(), mk.getText(), -1, ServoyBuilder.COMPONENT_FOUNDSET_INVALID,
											IMarker.PRIORITY_NORMAL,
											null, o);
										continue;
									}
								}
								if (val.has("dataproviders"))
								{
									JSONObject dataproviders = val.getJSONObject("dataproviders");
									for (String dp : dataproviders.keySet())
									{
										checkDataProvider(project, flattenedSolution, o, context, dataproviders.optString(dp), pd, datasource);
									}
								}
							}
						}
						else if (pd.getType() instanceof FoundsetLinkedPropertyType< ? , ? > &&
							pd.getConfig() instanceof FoundsetLinkedConfig &&
							(((FoundsetLinkedConfig)pd.getConfig()).getWrappedPropertyDescription().getType() instanceof ValueListPropertyType))
						{
							continue;
						}
						else
						{
							checkDataProvider(project, flattenedSolution, o, context, (String)propertyValue, pd, datasource);
						}
					}
				}
			}

		}
		checkDataProvider(project, flattenedSolution, o, context, id, null, datasource);

	}

	private static void checkDataProvider(ServoyProject project, FlattenedSolution flattenedSolution, final IPersist o, IPersist context, String id,
		PropertyDescription pd, String datasource)
	{
		try
		{
			if (id != null && !"".equals(id))
			{
				if (!(context instanceof Form))
				{
					ServoyLog.logError("Could not find parent form for element " + o, null);
				}
				else
				{
					Form parentForm = (Form)context;
					FlattenedSolution persistFlattenedSolution = ServoyBuilder.getPersistFlattenedSolution(context, flattenedSolution);
					IDataProvider dataProvider = persistFlattenedSolution.getDataProviderForTable(
						ServoyModelFinder.getServoyModel().getDataSourceManager()
							.getDataSource(datasource != null ? datasource : parentForm.getDataSource()),
						id);
					if (dataProvider == null)
					{
						Form flattenedForm = persistFlattenedSolution.getFlattenedForm(context);
						if (flattenedForm != null)
						{
							dataProvider = flattenedForm.getScriptVariable(id);
						}
					}
					if (dataProvider == null)
					{
						try
						{
							dataProvider = persistFlattenedSolution.getGlobalDataProvider(id, false);
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
					if (dataProvider == null && o.getParent() instanceof WebComponent)
					{
						WebComponent parent = (WebComponent)o.getParent();
						dataProvider = checkComponentDataproviders(id, persistFlattenedSolution, parent);
					}
					if (dataProvider == null && o instanceof WebComponent)
					{
						dataProvider = checkComponentDataproviders(id, persistFlattenedSolution, (WebComponent)o);
					}

					String elementName = null;
					String inForm = null;
					if (o instanceof ISupportName && ((ISupportName)o).getName() != null)
					{
						elementName = ((ISupportName)o).getName();
					}
					inForm = parentForm.getName();


					// check for valuelist type matching dataprovider type in web components
					if ((o instanceof WebComponent || o instanceof WebCustomType) && dataProvider != null)
					{
						Collection<PropertyDescription> dpProperties = new ArrayList<PropertyDescription>();

						if (o instanceof WebComponent)
						{
							WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebComponentSpecification(
								((WebComponent)o).getTypeName());
							if (spec != null)
							{
								dpProperties.addAll(spec.getProperties().values());
							}
						}
						else
						{
							WebCustomType customType = (WebCustomType)o;
							WebComponent parent = (WebComponent)customType.getAncestor(IRepository.WEBCOMPONENTS);
							WebObjectSpecification parentSpec = WebComponentSpecProvider.getSpecProviderState().getWebComponentSpecification(
								parent.getTypeName());
							if (parentSpec != null)
							{
								PropertyDescription cpd = ((ICustomType< ? >)parentSpec.getDeclaredCustomObjectTypes().get(
									customType.getTypeName())).getCustomJSONTypeDefinition();
								if (cpd != null)
								{
									dpProperties.addAll(cpd.getProperties().values());
								}
							}
						}

						for (PropertyDescription pd1 : dpProperties)
						{
							if (pd1.getType() instanceof ValueListPropertyType &&
								pd.getName().equals(((ValueListConfig)pd1.getConfig()).getFor()))
							{
								Object valuelistUUID = ((AbstractBase)o).getProperty(pd1.getName());
								if (valuelistUUID != null)
								{
									ValueList valuelist = (ValueList)flattenedSolution.searchPersist(valuelistUUID.toString());
									if (valuelist != null)
									{
										checkValueListRealValueToDataProviderTypeMatch(project.getProject(), valuelist, dataProvider, elementName, inForm,
											project.getSolution(), valuelistUUID);
									}
								}
							}
						}

					}
					if ((o instanceof Field || o instanceof GraphicalComponent) && dataProvider != null)
					{
						// check for valuelist type matching dataprovider type
						int valuelistID = o instanceof Field ? ((Field)o).getValuelistID() : ((GraphicalComponent)o).getValuelistID();
						ValueList valuelist = persistFlattenedSolution.getValueList(valuelistID);
						if (valuelist != null)
						{
							checkValueListRealValueToDataProviderTypeMatch(project.getProject(), valuelist, dataProvider, elementName, inForm,
								project.getSolution(),
								valuelist.getUUID());
						}

						String format = (o instanceof Field) ? ((Field)o).getFormat() : ((GraphicalComponent)o).getFormat();
						if (o instanceof Field && ((Field)o).getDisplayType() != Field.TEXT_FIELD &&
							((Field)o).getDisplayType() != Field.TYPE_AHEAD && ((Field)o).getDisplayType() != Field.CALENDAR)
						{
							format = null;
						}
						if (format != null && format.length() > 0)
						{
							ParsedFormat parsedFormat = FormatParser.parseFormatProperty(format);
							int dataType = ServoyBuilder.getDataType(project.getProject(), dataProvider, parsedFormat, o);
							if (parsedFormat.getDisplayFormat() != null && !parsedFormat.getDisplayFormat().startsWith("i18n:"))
							{
								try
								{
									if (dataType == IColumnTypes.DATETIME)
									{
										new SimpleDateFormat(parsedFormat.getDisplayFormat());
										if (parsedFormat.getEditFormat() != null) new SimpleDateFormat(parsedFormat.getEditFormat());
									}
									else if (dataType == IColumnTypes.INTEGER || dataType == IColumnTypes.NUMBER)
									{
										new DecimalFormat(parsedFormat.getDisplayFormat(),
											RoundHalfUpDecimalFormat.getDecimalFormatSymbols(Locale.getDefault()));
										if (parsedFormat.getEditFormat() != null) new DecimalFormat(parsedFormat.getEditFormat(),
											RoundHalfUpDecimalFormat.getDecimalFormatSymbols(Locale.getDefault()));
									}
								}
								catch (Exception ex)
								{
									Debug.trace(ex);

									ServoyMarker mk;
									if (elementName == null)
									{
										mk = MarkerMessages.FormFormatInvalid.fill(inForm, parsedFormat.getFormatString());
									}
									else
									{
										mk = MarkerMessages.FormFormatOnElementInvalid.fill(elementName, inForm,
											parsedFormat.getFormatString());
									}
									ServoyBuilder.addMarker(project.getProject(), mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_FORMAT_INVALID,
										IMarker.PRIORITY_NORMAL, null,
										o);
								}
							}
						}
					}
					if (o instanceof Field &&
						(((Field)o).getDisplayType() == Field.TYPE_AHEAD || ((Field)o).getDisplayType() == Field.TEXT_FIELD) &&
						((Field)o).getValuelistID() > 0 && ((Field)o).getFormat() != null)
					{
						boolean showWarning = false;
						ValueList vl = ServoyBuilder.getPersistFlattenedSolution(o, flattenedSolution).getValueList(
							((Field)o).getValuelistID());
						if (vl != null && vl.getValueListType() == IValueListConstants.CUSTOM_VALUES && vl.getCustomValues() != null &&
							(vl.getCustomValues() == null || vl.getCustomValues().contains("|")))
						{
							showWarning = true;
						}
						if (vl != null && vl.getValueListType() == IValueListConstants.DATABASE_VALUES &&
							vl.getReturnDataProviders() != vl.getShowDataProviders())
						{
							showWarning = true;
						}
						if (showWarning)
						{
							ServoyMarker mk;
							if (elementName == null)
							{
								mk = MarkerMessages.FormFormatIncompatible.fill(inForm);
							}
							else
							{
								mk = MarkerMessages.FormFormatOnElementIncompatible.fill(elementName, inForm);
							}
							ServoyBuilder.addMarker(project.getProject(), mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_FORMAT_INVALID,
								IMarker.PRIORITY_NORMAL,
								null, o);
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
							if (column.getLength() < ServoyBuilder.MIN_FIELD_LENGTH && column.getLength() > 0)
							{
								ServoyMarker mk = MarkerMessages.FormColumnLengthTooSmall.fill(elementName != null ? elementName : "",
									inForm);
								ServoyBuilder.addMarker(project.getProject(), mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_COLUMN_LENGTH_TOO_SMALL,
									IMarker.PRIORITY_NORMAL,
									null, o);
							}
						}
						if (((dataProvider instanceof ScriptVariable &&
							((ScriptVariable)dataProvider).getVariableType() == IColumnTypes.MEDIA &&
							((ScriptVariable)dataProvider).getSerializableRuntimeProperty(IScriptProvider.TYPE) == null) ||
							(dataProvider instanceof AggregateVariable &&
								((AggregateVariable)dataProvider).getType() == IColumnTypes.MEDIA &&
								((AggregateVariable)dataProvider).getSerializableRuntimeProperty(IScriptProvider.TYPE) == null) ||
							(dataProvider instanceof ScriptCalculation &&
								((ScriptCalculation)dataProvider).getType() == IColumnTypes.MEDIA &&
								((ScriptCalculation)dataProvider).getSerializableRuntimeProperty(IScriptProvider.TYPE) == null) ||
							(dataProvider instanceof Column &&
								Column.mapToDefaultType(((Column)dataProvider).getType()) == IColumnTypes.MEDIA) &&
								((Column)dataProvider).getColumnInfo() != null &&
								((Column)dataProvider).getColumnInfo().getConverterName() == null) &&
							field.getDisplayType() != Field.IMAGE_MEDIA)
						{
							ServoyMarker mk = MarkerMessages.FormIncompatibleElementType.fill(
								elementName != null ? elementName : field.getUUID(), inForm);
							ServoyBuilder.addMarker(project.getProject(), mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_INCOMPATIBLE_ELEMENT_TYPE,
								IMarker.PRIORITY_NORMAL,
								null, o);
						}
						if (dataProvider instanceof ScriptVariable && ((ScriptVariable)dataProvider).isDeprecated())
						{
							ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedVariable.fill(((ScriptVariable)dataProvider).getName(),
								"form " + inForm, "dataProvider");
							ServoyBuilder.addMarker(project.getProject(), mk.getType(), mk.getText(), -1, ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
								IMarker.PRIORITY_NORMAL, null, o);
						}
						else if (dataProvider instanceof ScriptCalculation && ((ScriptCalculation)dataProvider).isDeprecated())
						{
							ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedCalculation.fill(
								((ScriptCalculation)dataProvider).getName(), "form " + inForm, "dataProvider");
							ServoyBuilder.addMarker(project.getProject(), mk.getType(), mk.getText(), -1, ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
								IMarker.PRIORITY_NORMAL, null, o);
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
						ServoyBuilder.addMarker(project.getProject(), mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_INVALID_DATAPROVIDER,
							IMarker.PRIORITY_LOW,
							null, o);
					}

					boolean checkIfDataproviderIsBasedOnFormTable = true;
					if (o instanceof IBasicWebObject && pd != null)
					{
						String foundsetSelector = getWebBaseObjectPropertyFoundsetSelector((IBasicWebObject)o, pd);
						if (!"".equals(foundsetSelector))
						{
							// it is not form foundset based
							checkIfDataproviderIsBasedOnFormTable = false;
						}
					}

					if (checkIfDataproviderIsBasedOnFormTable && parentForm.getDataSource() != null &&
						dataProvider instanceof ColumnWrapper)
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
							ServoyBuilder.addMarker(project.getProject(), mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_INVALID_DATAPROVIDER,
								IMarker.PRIORITY_LOW,
								null, o);
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
						ServoyBuilder.addMarker(project.getProject(), mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_DATAPROVIDER_AGGREGATE_NOT_EDITABLE,
							IMarker.PRIORITY_LOW,
							null, o);
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
							ServoyBuilder.addMarker(project.getProject(), mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_INVALID_DATAPROVIDER,
								IMarker.PRIORITY_LOW,
								null, o);
						}
					}
					if (dataProvider instanceof ColumnWrapper)
					{
						Relation[] relations = ((ColumnWrapper)dataProvider).getRelations();
						if (relations != null)
						{
							for (Relation r : relations)
							{
								ServoyBuilder.addEncapsulationMarker(project.getProject(), o, r, (Form)context);
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


	private static void checkValueListRealValueToDataProviderTypeMatch(IProject project, ValueList valuelist, IDataProvider dataProvider, String elementName,
		String inForm, IPersist o, Object valuelistUUID) throws CoreException
	{
		int realValueType = valuelist.getRealValueType();
		if (realValueType != 0 && realValueType != dataProvider.getDataProviderType())
		{
			boolean isValidNumberVariable = dataProvider instanceof ScriptVariable &&
				((realValueType == IColumnTypes.INTEGER && dataProvider.getDataProviderType() == IColumnTypes.NUMBER) ||
					(realValueType == IColumnTypes.NUMBER && dataProvider.getDataProviderType() == IColumnTypes.INTEGER));

			if (!isValidNumberVariable)
			{
				ServoyMarker mk = MarkerMessages.ValuelistDataproviderTypeMismatch.fill(valuelist.getName(),
					elementName != null ? elementName : "", inForm);
				IMarker marker = ServoyBuilder.addMarker(project, mk.getType(), mk.getText(), -1, ServoyBuilder.VALUELIST_DATAPROVIDER_TYPE_MISMATCH,
					IMarker.PRIORITY_NORMAL, null, o);
				marker.setAttribute("Uuid", valuelistUUID.toString());
				marker.setAttribute("SolutionName", valuelist.getRootObject().getName());
			}
		}
	}

	private static String getWebBaseObjectPropertyFoundsetSelector(IBasicWebObject webObject, PropertyDescription pd)
	{
		if (pd.getType() instanceof FoundsetLinkedPropertyType< ? , ? >)
		{
			String forFoundset = ((FoundsetLinkedConfig)pd.getConfig()).getForFoundsetName();
			IBasicWebObject parent = webObject;
			while (parent.getParent() instanceof IBasicWebObject)
			{
				parent = (IBasicWebObject)parent.getParent();
			}
			Object forFoundsetValue = parent.getProperty(forFoundset);
			if (forFoundsetValue instanceof JSONObject)
			{
				return ((JSONObject)forFoundsetValue).optString(FoundsetPropertyType.FOUNDSET_SELECTOR);
			}
		}
		return "";
	}

	private static IDataProvider checkComponentDataproviders(String id, FlattenedSolution persistFlattenedSolution, WebComponent component)
		throws RepositoryException
	{
		IDataProvider dataProvider = null;
		WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebComponentSpecification(component.getTypeName());
		if (spec != null)
		{
			Collection<PropertyDescription> fsPD = spec.getProperties(FoundsetPropertyType.INSTANCE);
			for (PropertyDescription pd : fsPD)
			{
				Object relatedFS = component.getProperty(pd.getName());
				if (relatedFS instanceof JSONObject)
				{
					String fs = ((JSONObject)relatedFS).optString(FoundsetPropertyType.FOUNDSET_SELECTOR);
					if ("".equals(fs)) //Form foundset
					{
						Form f = (Form)component.getAncestor(IRepository.FORMS);
						fs = f.getDataSource();
					}
					if (fs == null) break;
					if (DataSourceUtils.isDatasourceUri(fs))
					{
						ITable table = persistFlattenedSolution.getTable(fs);
						if (table != null)
						{
							dataProvider = persistFlattenedSolution.getDataProviderForTable(table, id);
						}
					}
					else
					{
						Relation[] relations = persistFlattenedSolution.getRelationSequence(fs);
						if (relations != null)
						{
							Relation r = relations[relations.length - 1];
							dataProvider = getDataProvider(persistFlattenedSolution, id, r.getPrimaryServerName(), r.getPrimaryTableName());
							if (dataProvider == null)
								dataProvider = getDataProvider(persistFlattenedSolution, id, r.getForeignServerName(), r.getForeignTableName());
						}
					}
					if (dataProvider != null) break;
				}
			}
		}
		return dataProvider;
	}

	private static IDataProvider getDataProvider(FlattenedSolution fs, String id, String serverName, String tableName) throws RepositoryException
	{
		IServerInternal server = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(serverName, true, true);
		if (server != null)
		{
			ITable table = server.getTable(tableName);
			if (table != null)
			{
				return fs.getDataProviderForTable(table, id);
			}
		}
		return null;
	}
}
