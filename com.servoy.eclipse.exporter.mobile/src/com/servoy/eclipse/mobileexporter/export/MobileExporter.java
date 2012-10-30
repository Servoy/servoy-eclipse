/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.mobileexporter.export;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.IValueFilter;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractScriptProvider;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.ContentSpec.Element;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class MobileExporter
{
	private static final String RELATIVE_TEMPLATE_PATH = "resources/solution.js";
	private static final String RELATIVE_WAR_PATH = "resources/servoy_mobile.war";

	private static final String FORM_LOOP_START = "${loop_forms}";
	private static final String FORM_LOOP_END = "${endloop_forms}";

	private static final String SCOPES_LOOP_START = "${loop_scopes}";
	private static final String SCOPES_LOOP_END = "${endloop_scopes}";

	private static final String VARIABLES_LOOP_START = "${loop_variables}";
	private static final String VARIABLES_LOOP_END = "${endloop_variables}";

	private static final String FUNCTIONS_LOOP_START = "${loop_functions}";
	private static final String FUNCTIONS_LOOP_END = "${endloop_functions}";

	private static final String PROPERTY_FORM_NAME = "${formName}";
	private static final String PROPERTY_VARIABLE_NAME = "${variableName}";
	private static final String PROPERTY_FUNCTION_NAME = "${functionName}";
	private static final String PROPERTY_SCOPE_NAME = "${scopeName}";

	private static final String PROPERTY_FUNCTION_CODE = "${functionCode}";
	private static final String PROPERTY_VARIABLE_DEFAULT_VALUE = "${defaultValue}";
	private static final String PROPERTY_VARIABLE_TYPE = "${variableType}";

	@SuppressWarnings("restriction")
	private String doFormsExport()
	{
		ServoyProject project = ServoyModelFinder.getServoyModel().getServoyProject(solutionName);
		if (project != null)
		{
			Iterator<Form> formIterator = project.getSolution().getForms(null, true);
			List<ServoyJSONObject> formJSons = new ArrayList<ServoyJSONObject>();
			while (formIterator.hasNext())
			{
				final Form form = formIterator.next();
				try
				{
					ServoyJSONObject formJSon = SolutionSerializer.generateJSONObject(form, true, true,
						ApplicationServerSingleton.get().getDeveloperRepository(), true, new IValueFilter()
						{

							public String getFilteredValue(IPersist persist, Map<String, Object> property_values, String key, String value)
							{
								Element contentSpec = StaticContentSpecLoader.getContentSpec().getPropertyForObjectTypeByName(persist.getTypeID(), key);
								if (contentSpec != null && contentSpec.getTypeID() == IRepository.ELEMENTS &&
									(BaseComponent.isEventProperty(contentSpec.getName()) || BaseComponent.isCommandProperty(contentSpec.getName())))
								{
									try
									{
										Object methodID = ApplicationServerSingleton.get().getDeveloperRepository().convertArgumentStringToObject(
											contentSpec.getTypeID(), value);
										if (methodID instanceof Integer)
										{
											IScriptProvider sm = ModelUtils.getScriptMethod(form, form, null, ((Integer)methodID).intValue());
											if (sm != null)
											{
												List<Object> arguments = ((AbstractBase)persist).getInstanceMethodArguments(key);
												StringBuilder sb = new StringBuilder(ScopesUtils.getScopeString(((AbstractScriptProvider)sm).getScopeName(),
													sm.getDataProviderID()));
												sb.append('(');
												if (arguments != null && arguments.size() > 0)
												{
													for (Object argument : arguments)
													{
														sb.append(argument);
														sb.append(',');
													}
													sb.setLength(sb.length() - 1);
												}
												sb.append(')');
												return sb.toString();
											}
											else
											{
												return null;
											}
										}
									}
									catch (Exception e)
									{
										ServoyLog.logError(e);
									}
								}
								if (persist instanceof GraphicalComponent && !property_values.containsKey("viewType"))
								{
									String labelViewType = "label";
									if (((GraphicalComponent)persist).getOnActionMethodID() != 0 && ((GraphicalComponent)persist).getShowClick())
									{
										labelViewType = "button";
									}
									property_values.put("viewType", labelViewType);
								}
								if (value != null && contentSpec != null &&
									contentSpec.getName().equals(StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES.getPropertyName()))
								{
									try
									{
										ServoyJSONObject customProperties = new ServoyJSONObject(value, true, false, false);
										return ServoyJSONObject.toString(customProperties, false, false, false);
									}
									catch (Exception ex)
									{
										ServoyLog.logError(ex);
									}
								}
								return value;
							}
						});
					if (project.getSolution().getFirstFormID() == form.getID())
					{
						formJSons.add(0, formJSon);
					}
					else
					{
						formJSons.add(formJSon);
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
			ServoyJSONArray flattenedJSon = new ServoyJSONArray(formJSons);
			Map<String, Object> solutionModel = new HashMap<String, Object>();
			solutionModel.put("forms", flattenedJSon);
			solutionModel.put("solutionName", project.getSolution().getName());
			solutionModel.put("serverURL", serverURL);
			solutionModel.put("skipConnect", skipConnect);
			ServoyJSONObject jsonObject = new ServoyJSONObject(solutionModel);
			jsonObject.setNoQuotes(false);
			return ("var _solutiondata_ = " + jsonObject.toString());
		}
		return null;
	}

	private String doScriptingExport(String solutionName)
	{
		ServoyProject project = ServoyModelFinder.getServoyModel().getServoyProject(solutionName);
		if (project != null)
		{
			String template = Utils.getTXTFileContent(getClass().getResourceAsStream(RELATIVE_TEMPLATE_PATH), Charset.forName("UTF8")); //$NON-NLS-1$
			StringBuilder builder = new StringBuilder();

			int formsLoopStartIndex = template.indexOf(FORM_LOOP_START);
			int formsLoopEndIndex = template.indexOf(FORM_LOOP_END);
			builder.append(template.substring(0, formsLoopStartIndex));
			builder.append(replaceFormsScripting(template.substring(formsLoopStartIndex + FORM_LOOP_START.length(), formsLoopEndIndex)));

			int scopesLoopStartIndex = template.indexOf(SCOPES_LOOP_START);
			int scopesLoopEndIndex = template.indexOf(SCOPES_LOOP_END);
			builder.append(template.substring(formsLoopEndIndex + FORM_LOOP_END.length(), scopesLoopStartIndex));
			builder.append(replaceScopesScripting(template.substring(scopesLoopStartIndex + SCOPES_LOOP_START.length(), scopesLoopEndIndex)));

			int allVariablesLoopStartIndex = template.indexOf(VARIABLES_LOOP_START, scopesLoopEndIndex);
			int allVariablesLoopEndIndex = template.indexOf(VARIABLES_LOOP_END, scopesLoopEndIndex);
			builder.append(template.substring(scopesLoopEndIndex + SCOPES_LOOP_END.length(), allVariablesLoopStartIndex));
			builder.append(replaceVariablesScripting(template.substring(allVariablesLoopStartIndex + VARIABLES_LOOP_START.length(), allVariablesLoopEndIndex),
				project.getSolution(), null));

			formsLoopStartIndex = template.indexOf(FORM_LOOP_START, allVariablesLoopEndIndex);
			formsLoopEndIndex = template.indexOf(FORM_LOOP_END, allVariablesLoopEndIndex);
			builder.append(template.substring(allVariablesLoopEndIndex + VARIABLES_LOOP_END.length(), formsLoopStartIndex));
			builder.append(replaceFormsScripting(template.substring(formsLoopStartIndex + FORM_LOOP_START.length(), formsLoopEndIndex)));

			scopesLoopStartIndex = template.indexOf(SCOPES_LOOP_START, formsLoopStartIndex);
			scopesLoopEndIndex = template.indexOf(SCOPES_LOOP_END, formsLoopStartIndex);
			builder.append(template.substring(formsLoopEndIndex + FORM_LOOP_END.length(), scopesLoopStartIndex));
			builder.append(replaceScopesScripting(template.substring(scopesLoopStartIndex + SCOPES_LOOP_START.length(), scopesLoopEndIndex)));

			builder.append(template.substring(scopesLoopEndIndex + SCOPES_LOOP_END.length()));
			return builder.toString();
		}
		return null;
	}

	private File outputFolder;
	private String serverURL;
	private String solutionName;

	public File doExport(boolean exportAsZip)
	{
		String formJson = doFormsExport();
		String solutionJavascript = doScriptingExport(solutionName);

		//TODO remove these lines
		File outputFile = new File(outputFolder, "solution.js"); //$NON-NLS-1$
		Utils.writeTXTFile(outputFile, solutionJavascript);

		outputFile = new File(outputFolder, "solution.json");
		Utils.writeTXTFile(outputFile, formJson);

		File exportedFile = null;
		InputStream is = this.getClass().getResourceAsStream(RELATIVE_WAR_PATH);
		if (is != null)
		{
			ZipOutputStream warStream = null;
			try
			{
				exportedFile = new File(outputFolder, solutionName + (exportAsZip ? ".zip" : ".war"));
				warStream = new ZipOutputStream(new FileOutputStream(exportedFile));
				ZipInputStream zipStream = new ZipInputStream(is);
				ZipEntry entry = zipStream.getNextEntry();
				while (entry != null)
				{
					String entryName = entry.getName();
					if (entryName.equals("servoy_mobile.html"))
					{
						entryName = "index.html";
					}
					if (!exportAsZip || !entryName.startsWith("WEB-INF"))
					{
						addZipEntry(entryName, warStream, zipStream);
					}
					entry = zipStream.getNextEntry();
				}
				addZipEntry("solution.json", warStream, new ByteArrayInputStream(formJson.getBytes("UTF8")));
				addZipEntry("solution.js", warStream, new ByteArrayInputStream(solutionJavascript.getBytes("UTF8")));
				Utils.closeInputStream(zipStream);
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}
			finally
			{
				Utils.closeOutputStream(warStream);
			}
		}
		else
		{
			ServoyLog.logError("mobile.war file was not found in exporter project", null);
		}
		return exportedFile;
	}

	private void addZipEntry(String entryName, ZipOutputStream stream, InputStream inputStream) throws IOException
	{
		stream.putNextEntry(new ZipEntry(entryName));
		Utils.streamCopy(inputStream, stream);
		stream.closeEntry();
	}

	private String replaceFormsScripting(String template)
	{
		StringBuffer formsScript = new StringBuffer();
		ServoyProject project = ServoyModelFinder.getServoyModel().getActiveProject();
		if (project != null)
		{
			Iterator<Form> formIterator = project.getSolution().getForms(null, true);
			while (formIterator.hasNext())
			{
				Form form = formIterator.next();
				addVariablesAndFunctionsScripting(template.replace(PROPERTY_FORM_NAME, form.getName()), formsScript, form, null);
				if (formIterator.hasNext())
				{
					formsScript.append("\n"); //$NON-NLS-1$
				}
			}
		}
		return formsScript.toString();
	}

	private void addVariablesAndFunctionsScripting(String template, StringBuffer appender, IPersist parent, String scopeName)
	{
		int functionsLoopStartIndex = template.indexOf(FUNCTIONS_LOOP_START);
		int functionsLoopEndIndex = template.indexOf(FUNCTIONS_LOOP_END);
		if (functionsLoopStartIndex >= 0)
		{
			appender.append(template.substring(0, functionsLoopStartIndex));
			appender.append(replaceFunctionsScripting(template.substring(functionsLoopStartIndex + FUNCTIONS_LOOP_START.length(), functionsLoopEndIndex),
				parent, scopeName));
		}

		int variablesLoopStartIndex = template.indexOf(VARIABLES_LOOP_START, functionsLoopEndIndex);
		int variablesLoopEndIndex = template.indexOf(VARIABLES_LOOP_END, functionsLoopEndIndex);
		if (variablesLoopStartIndex >= 0)
		{
			appender.append(template.substring(functionsLoopEndIndex + FUNCTIONS_LOOP_END.length(), variablesLoopStartIndex));
			appender.append(replaceVariablesScripting(template.substring(variablesLoopStartIndex + VARIABLES_LOOP_START.length(), variablesLoopEndIndex),
				parent, scopeName));
		}
		appender.append(variablesLoopEndIndex >= 0 ? template.substring(variablesLoopEndIndex + VARIABLES_LOOP_END.length()) : template);
	}

	private String replaceScopesScripting(String template)
	{
		StringBuffer scopesScript = new StringBuffer();
		ServoyProject project = ServoyModelFinder.getServoyModel().getActiveProject();
		if (project != null)
		{
			Iterator<String> scopeIterator = project.getSolution().getScopeNames().iterator();
			while (scopeIterator.hasNext())
			{
				String scopeName = scopeIterator.next();
				addVariablesAndFunctionsScripting(template.replace(PROPERTY_SCOPE_NAME, scopeName), scopesScript, project.getSolution(), scopeName);
				if (scopeIterator.hasNext()) scopesScript.append("\n"); //$NON-NLS-1$
			}
		}
		return scopesScript.toString();
	}

	private String replaceVariablesScripting(String template, IPersist parent, String scopeName)
	{
		StringBuffer variablesScript = new StringBuffer();
		if (parent instanceof Form)
		{
			variablesScript.append(buildVariablesScripting(template, ((Form)parent).getScriptVariables(true)));
		}
		if (parent instanceof Solution)
		{
			if (scopeName != null)
			{
				variablesScript.append(buildVariablesScripting(template, ((Solution)parent).getScriptVariables(scopeName, true)));
			}
			else
			{
				// allvariables
				Iterator<Form> formIterator = ((Solution)parent.getRootObject()).getForms(null, true);
				while (formIterator.hasNext())
				{
					Form form = formIterator.next();
					variablesScript.append(buildVariablesScripting(template, form.getScriptVariables(true)));
				}
				variablesScript.append(buildVariablesScripting(template, ((Solution)parent).getScriptVariables(true)));
			}
		}
		return variablesScript.toString();
	}

	private String buildVariablesScripting(String template, Iterator<ScriptVariable> variableIterator)
	{
		StringBuffer variablesScript = new StringBuffer();
		if (variableIterator != null)
		{
			while (variableIterator.hasNext())
			{
				ScriptVariable variable = variableIterator.next();
				String variableScripting = template;
				variableScripting = variableScripting.replace(PROPERTY_VARIABLE_NAME, variable.getName());
				variableScripting = variableScripting.replace(PROPERTY_VARIABLE_DEFAULT_VALUE, "" + variable.getDefaultValue());
				variableScripting = variableScripting.replace(PROPERTY_VARIABLE_TYPE, String.valueOf(variable.getTypeID()));
				variablesScript.append(variableScripting);
			}
		}
		return variablesScript.toString();
	}

	private String replaceFunctionsScripting(String template, IPersist parent, String scopeName)
	{
		StringBuffer functionsScript = new StringBuffer();
		Iterator<ScriptMethod> methodIterator = null;
		if (parent instanceof Form)
		{
			methodIterator = ((Form)parent).getScriptMethods(true);
		}
		if (parent instanceof Solution)
		{
			methodIterator = ((Solution)parent).getScriptMethods(scopeName, true);
		}
		if (methodIterator != null)
		{
			while (methodIterator.hasNext())
			{
				ScriptMethod method = methodIterator.next();
				String methodScripting = template;
				methodScripting = methodScripting.replace(PROPERTY_FUNCTION_NAME, method.getName());
				methodScripting = methodScripting.replace(PROPERTY_FUNCTION_CODE, getAnonymousScripting(method));
				functionsScript.append(methodScripting);
				if (methodIterator.hasNext())
				{
					functionsScript.append("\n"); //$NON-NLS-1$
				}
			}
		}
		return functionsScript.toString();
	}

	private String getAnonymousScripting(ScriptMethod method)
	{
		String scripting = method.getDeclaration();
		scripting = scripting.replaceAll("function " + method.getName(), "function ");
		int index = scripting.indexOf("function ");
		scripting = scripting.substring(index).trim();
		return scripting;
	}

	/**
	 * @param outputFolder the outputFolder to set
	 */
	public void setOutputFolder(File outputFolder)
	{
		this.outputFolder = outputFolder;
	}

	/**
	 * @param serverURL the serverURL to set
	 */
	public void setServerURL(String serverURL)
	{
		this.serverURL = serverURL;
	}

	/**
	 * @param solutionName the solutionName to set
	 */
	public void setSolutionName(String solutionName)
	{
		this.solutionName = solutionName;
	}

	/**
	 * @return the serverURL
	 */
	public String getServerURL()
	{
		return serverURL;
	}

	/**
	 * @return the solutionName
	 */
	public String getSolutionName()
	{
		return solutionName;
	}

	private boolean skipConnect = false;

	public void setSkipConnect(boolean connect)
	{
		this.skipConnect = connect;
	}

}
