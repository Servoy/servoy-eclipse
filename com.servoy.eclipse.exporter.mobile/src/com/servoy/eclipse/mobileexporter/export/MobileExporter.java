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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.base.persistence.constants.IComponentConstants;
import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.IValueFilter;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractScriptProvider;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.ContentSpec.Element;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.I18NUtil;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.ScriptEngine;
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


	private static final String MIME_JS = "text/javascript";
	private static final String MIME_CSS = "text/css";
	private static final String TAG_SCRIPT = "<script type=\"text/javascript\" language=\"javascript\" src=\"media/";
	private static final String TAG_CSS = "<link rel=\"stylesheet\" type=\"text/css\" href=\"media/";
	private static final String TAG_SCRIPT_END = "\"></script>\n";
	private static final String TAG_CSS_END = "\"/>\n";


	private String doMediaExport(ZipOutputStream zos, File outputFolder) throws IOException
	{
		StringBuilder headerText = new StringBuilder();

		FlattenedSolution flattenedSolution = ServoyModelFinder.getServoyModel().getFlattenedSolution();
		if (flattenedSolution != null)
		{
			Iterator<Media> mediasIte = flattenedSolution.getMedias(false);
			Media media;
			byte[] content;
			boolean isTXTContent;
			while (mediasIte.hasNext())
			{
				media = mediasIte.next();
				content = media.getMediaData();
				isTXTContent = false;
				if (MIME_JS.equals(media.getMimeType()))
				{
					headerText.append(TAG_SCRIPT).append(media.getName()).append(TAG_SCRIPT_END);
					isTXTContent = true;
				}
				else if (MIME_CSS.equals(media.getMimeType()))
				{
					headerText.append(TAG_CSS).append(media.getName()).append(TAG_CSS_END);
					isTXTContent = true;
				}

				addZipEntry("media/" + media.getName(), zos, isTXTContent ? Utils.getUTF8EncodedStream(new String(content)) : new ByteArrayInputStream(content));
				if (outputFolder != null)
				{
					File outputFolderJS = new File(outputFolder, "media");
					outputFolderJS.mkdirs();
					if (isTXTContent) Utils.writeTXTFile(new File(outputFolderJS, media.getName()), new String(content));
					else Utils.writeFile(new File(outputFolderJS, media.getName()), content);
				}
			}
		}

		return headerText.toString();
	}

	private String doPersistExport()
	{
		FlattenedSolution flattenedSolution = ServoyModelFinder.getServoyModel().getFlattenedSolution();
		if (flattenedSolution != null)
		{
			Iterator<Form> formIterator = flattenedSolution.getForms(true);
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
											return generateMethodCall(form, persist, key, ((Integer)methodID).intValue());
										}
									}
									catch (Exception e)
									{
										ServoyLog.logError(e);
									}
								}
								if (persist instanceof GraphicalComponent && !property_values.containsKey(IComponentConstants.VIEW_TYPE_ATTR))
								{
									property_values.put(IComponentConstants.VIEW_TYPE_ATTR, ComponentFactory.isButton(((GraphicalComponent)persist))
										? IComponentConstants.VIEW_TYPE_BUTTON : IComponentConstants.VIEW_TYPE_LABEL);
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
					if (flattenedSolution.getSolution().getFirstFormID() == form.getID())
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
			// export relations
			List<ServoyJSONObject> relationJSons = new ArrayList<ServoyJSONObject>();
			try
			{
				Iterator<Relation> relationIterator = flattenedSolution.getRelations(true);
				while (relationIterator.hasNext())
				{
					final Relation relation = relationIterator.next();
					try
					{
						ServoyJSONObject relationJSON = SolutionSerializer.generateJSONObject(relation, true, true,
							ApplicationServerSingleton.get().getDeveloperRepository(), true, null);
						relationJSons.add(relationJSON);
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
			}
			catch (RepositoryException ex)
			{
				ServoyLog.logError(ex);
			}
			// export valuelists
			Iterator<ValueList> valuelistIterator = flattenedSolution.getValueLists(false);
			List<JSONObject> valuelistJSons = new ArrayList<JSONObject>();
			while (valuelistIterator.hasNext())
			{
				final ValueList valuelist = valuelistIterator.next();
				// for now only the custom valuelist.
				// and we really push the RuntimeValueList not the actual persist value list
				// so with real/display values already, instead of the "customvalues" string.
				if (valuelist.getValueListType() == IValueListConstants.CUSTOM_VALUES)
				{
					try
					{
						JSONObject json = new JSONObject();

						json.put("name", valuelist.getName());
						json.put("uuid", valuelist.getUUID().toString());
						JSONArray displayValues = new JSONArray();
						JSONArray realValues = new JSONArray();
						IValueList realValueList = ComponentFactory.getRealValueList(com.servoy.eclipse.core.Activator.getDefault().getDesignClient(),
							valuelist, true, Types.OTHER, null, null);
						for (int i = 0; i < realValueList.getSize(); i++)
						{
							displayValues.put(realValueList.getElementAt(i));
							realValues.put(realValueList.getRealElementAt(i));
						}
						json.put("displayValues", displayValues);
						json.put("realValues", realValues);


						valuelistJSons.add(json);
					}
					catch (JSONException e)
					{
						ServoyLog.logError(e);
					}
				}
			}

			Solution solution = flattenedSolution.getSolution();
			ServoyJSONArray flattenedJSon = new ServoyJSONArray(formJSons);
			Map<String, Object> solutionModel = new HashMap<String, Object>();
			solutionModel.put("forms", flattenedJSon);
			flattenedJSon = new ServoyJSONArray(relationJSons);
			solutionModel.put("relations", flattenedJSon);
			flattenedJSon = new ServoyJSONArray(valuelistJSons);
			solutionModel.put("valuelists", flattenedJSon);
			solutionModel.put("solutionName", solution.getName());
			solutionModel.put("serverURL", serverURL);
			solutionModel.put("timeout", timeout);
			solutionModel.put("skipConnect", Boolean.valueOf(skipConnect));
			solutionModel.put("mustAuthenticate", Boolean.valueOf(solution.getMustAuthenticate()));

			int onOpenMethodID = solution.getOnOpenMethodID();
			if (onOpenMethodID > 0)
			{
				solutionModel.put("onSolutionOpen",
					generateMethodCall(solution, solution, StaticContentSpecLoader.PROPERTY_ONOPENMETHODID.getPropertyName(), onOpenMethodID));
			}

			if (flattenedSolution.getSolution().getI18nDataSource() != null)
			{
				EclipseMessages messagesManager = ServoyModelManager.getServoyModelManager().getServoyModel().getMessagesManager();
				TreeMap<String, I18NUtil.MessageEntry> i18nData = messagesManager.getDatasourceMessages(solution.getI18nDataSource());
				if (i18nData.size() > 0)
				{
					solutionModel.put("i18n", i18nData);
				}
			}
			ServoyJSONObject jsonObject = new ServoyJSONObject(solutionModel);
			jsonObject.setNoQuotes(false);
			return ("var _solutiondata_ = " + jsonObject.toString());
		}
		return null;
	}

	private String doScriptingExport()
	{
		FlattenedSolution flattenedSolution = ServoyModelFinder.getServoyModel().getFlattenedSolution();
		if (flattenedSolution != null)
		{
			String template = Utils.getTXTFileContent(getClass().getResourceAsStream(RELATIVE_TEMPLATE_PATH), Charset.forName("UTF8")); //$NON-NLS-1$
			StringBuilder builder = new StringBuilder();

			int formsLoopStartIndex = template.indexOf(FORM_LOOP_START);
			int formsLoopEndIndex = template.indexOf(FORM_LOOP_END);
			builder.append(template.substring(0, formsLoopStartIndex));
			builder.append(replaceFormsScripting(template.substring(formsLoopStartIndex + FORM_LOOP_START.length(), formsLoopEndIndex), null));

			int scopesLoopStartIndex = template.indexOf(SCOPES_LOOP_START);
			int scopesLoopEndIndex = template.indexOf(SCOPES_LOOP_END);
			builder.append(template.substring(formsLoopEndIndex + FORM_LOOP_END.length(), scopesLoopStartIndex));
			builder.append(replaceScopesScripting(template.substring(scopesLoopStartIndex + SCOPES_LOOP_START.length(), scopesLoopEndIndex), null));

			int allVariablesLoopStartIndex = template.indexOf(VARIABLES_LOOP_START, scopesLoopEndIndex);
			int allVariablesLoopEndIndex = template.indexOf(VARIABLES_LOOP_END, scopesLoopEndIndex);
			builder.append(template.substring(scopesLoopEndIndex + SCOPES_LOOP_END.length(), allVariablesLoopStartIndex));
			builder.append(replaceVariablesScripting(template.substring(allVariablesLoopStartIndex + VARIABLES_LOOP_START.length(), allVariablesLoopEndIndex),
				flattenedSolution.getSolution(), null));

			formsLoopStartIndex = template.indexOf(FORM_LOOP_START, allVariablesLoopEndIndex);
			formsLoopEndIndex = template.indexOf(FORM_LOOP_END, allVariablesLoopEndIndex);
			builder.append(template.substring(allVariablesLoopEndIndex + VARIABLES_LOOP_END.length(), formsLoopStartIndex));
			builder.append(replaceFormsScripting(template.substring(formsLoopStartIndex + FORM_LOOP_START.length(), formsLoopEndIndex), ",\n"));

			scopesLoopStartIndex = template.indexOf(SCOPES_LOOP_START, formsLoopStartIndex);
			scopesLoopEndIndex = template.indexOf(SCOPES_LOOP_END, formsLoopStartIndex);
			builder.append(template.substring(formsLoopEndIndex + FORM_LOOP_END.length(), scopesLoopStartIndex));
			builder.append(replaceScopesScripting(template.substring(scopesLoopStartIndex + SCOPES_LOOP_START.length(), scopesLoopEndIndex), ",\n"));

			builder.append(template.substring(scopesLoopEndIndex + SCOPES_LOOP_END.length()));
			return builder.toString();
		}
		return null;
	}

	private File outputFolder;
	private String serverURL;
	private String solutionName;
	private int timeout;

	public File doExport(boolean exportAsZip)
	{
		String formJson = doPersistExport();
		String solutionJavascript = doScriptingExport();

		// Write files for running from java source
		File tmpP = new File(outputFolder.getParent() + "/src/com/servoy/mobile/public");
		boolean developmentWorkspaceExport = "war".equals(outputFolder.getName()) && outputFolder.getParent() != null && tmpP.exists();

		if (developmentWorkspaceExport)
		{
			File outputFile = new File(tmpP, "solution.js"); //$NON-NLS-1$
			Utils.writeTXTFile(outputFile, solutionJavascript);
			Utils.writeTXTFile(new File(outputFolder, "mobileclient/solution.js"), solutionJavascript);

			outputFile = new File(tmpP, "solution_json.js");
			Utils.writeTXTFile(outputFile, formJson);
			Utils.writeTXTFile(new File(outputFolder, "mobileclient/solution_json.js"), formJson);
		}

		File exportedFile = null;
		InputStream is = this.getClass().getResourceAsStream(RELATIVE_WAR_PATH);
		if (is != null)
		{
			ZipOutputStream warStream = null;
			try
			{
				Map<String, String> renameMap = new HashMap<String, String>();
				buildRenameEntriesList(renameMap);
				exportedFile = new File(outputFolder, solutionName + (exportAsZip ? ".zip" : ".war"));
				warStream = new ZipOutputStream(new FileOutputStream(exportedFile));

				String mediaExport = doMediaExport(warStream, developmentWorkspaceExport ? outputFolder : null);

				ZipInputStream zipStream = new ZipInputStream(is);
				ZipEntry entry = zipStream.getNextEntry();
				while (entry != null)
				{
					InputStream contentStream = null;
					String entryName = entry.getName();
					if (entryName.equals("servoy_mobile.html") || entryName.equals("mobileclient/mobileclient.nocache.js"))
					{
						String fileContent = Utils.getTXTFileContent(zipStream, Charset.forName("UTF8"), false);
						for (String key : renameMap.keySet())
						{
							fileContent = fileContent.replaceAll(Pattern.quote(key), renameMap.get(key));
						}
						if (entryName.equals("servoy_mobile.html"))
						{
							fileContent = fileContent.replaceAll(Pattern.quote("<!--SOLUTION_MEDIA_PLACEHOLDER-->"), mediaExport);
							if (developmentWorkspaceExport)
							{
								String indexContent = Utils.getTXTFileContent(new FileInputStream(new File(outputFolder, "servoy_mobile.html")),
									Charset.forName("UTF8"), false);
								File outputFile = new File(outputFolder, "index.html"); //$NON-NLS-1$
								indexContent = indexContent.replaceAll(Pattern.quote("<!--SOLUTION_MEDIA_PLACEHOLDER-->"), mediaExport);
								Utils.writeTXTFile(outputFile, indexContent);
							}
						}
						contentStream = Utils.getUTF8EncodedStream(fileContent);
					}
					if (renameMap.containsKey(entryName))
					{
						entryName = renameMap.get(entryName);
					}

					if (!exportAsZip || !entryName.startsWith("WEB-INF"))
					{
						addZipEntry(entryName, warStream, contentStream != null ? contentStream : zipStream);
					}
					entry = zipStream.getNextEntry();
				}
				addZipEntry("mobileclient/" + renameMap.get("solution_json.js"), warStream, Utils.getUTF8EncodedStream(formJson));
				addZipEntry("mobileclient/" + renameMap.get("solution.js"), warStream, Utils.getUTF8EncodedStream(solutionJavascript));
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

	private void buildRenameEntriesList(Map<String, String> renameMap)
	{
		renameMap.put("servoy_mobile.html", "index.html");

		addRenameEntries(renameMap, "mobileclient/", "solution_json", ".js");
		addRenameEntries(renameMap, "mobileclient/", "solution", ".js");
		addRenameEntries(renameMap, "mobileclient/", "servoy_utils", ".js");
		addRenameEntries(renameMap, "mobileclient/", "servoy", ".css");
		addRenameEntries(renameMap, "mobileclient/", "mobileclient.nocache", ".js");
	}

	private void addRenameEntries(Map<String, String> renameMap, String prefixLocation, String name, String subfix)
	{
		String mobileClientFileName = name + '_' + System.currentTimeMillis() + subfix;
		renameMap.put(prefixLocation + name + subfix, prefixLocation + mobileClientFileName); // for zip entry path replace
		renameMap.put(name + subfix, mobileClientFileName); // for content replacement
	}

	private void addZipEntry(String entryName, ZipOutputStream stream, InputStream inputStream) throws IOException
	{
		stream.putNextEntry(new ZipEntry(entryName));
		Utils.streamCopy(inputStream, stream);
		stream.closeEntry();
	}

	private String replaceFormsScripting(String template, String separator)
	{
		StringBuffer formsScript = new StringBuffer();
		FlattenedSolution flattenedSolution = ServoyModelFinder.getServoyModel().getFlattenedSolution();
		if (flattenedSolution != null)
		{
			Iterator<Form> formIterator = flattenedSolution.getForms(true);
			while (formIterator.hasNext())
			{
				Form form = formIterator.next();
				addVariablesAndFunctionsScripting(template.replace(PROPERTY_FORM_NAME, form.getName()), formsScript, form, null);
				if (separator != null && formIterator.hasNext())
				{
					formsScript.append(separator);
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

	private String replaceScopesScripting(String template, String separator)
	{
		StringBuffer scopesScript = new StringBuffer();
		FlattenedSolution flattenedSolution = ServoyModelFinder.getServoyModel().getFlattenedSolution();
		if (flattenedSolution != null)
		{
			Iterator<String> scopeIterator = flattenedSolution.getScopeNames().iterator();
			while (scopeIterator.hasNext())
			{
				String scopeName = scopeIterator.next();
				addVariablesAndFunctionsScripting(template.replace(PROPERTY_SCOPE_NAME, scopeName), scopesScript, flattenedSolution.getSolution(), scopeName);
				if (separator != null && scopeIterator.hasNext()) scopesScript.append(separator);
			}
		}
		return scopesScript.toString();
	}

	private String replaceVariablesScripting(String template, IPersist parent, String scopeName)
	{
		StringBuffer variablesScript = new StringBuffer();
		if (parent instanceof Form)
		{
			variablesScript.append(buildVariablesScripting(template, ((Form)parent).getScriptVariables(false), ",\n")); //$NON-NLS-1$
		}
		if (parent instanceof Solution)
		{
			if (scopeName != null)
			{
				variablesScript.append(buildVariablesScripting(template, ((Solution)parent).getScriptVariables(scopeName, false), ",\n")); //$NON-NLS-1$
			}
			else
			{
				// allvariables
				Iterator<Form> formIterator = ((Solution)parent.getRootObject()).getForms(null, true);
				while (formIterator.hasNext())
				{
					Form form = formIterator.next();
					variablesScript.append(buildVariablesScripting(template, form.getScriptVariables(false), null));
				}
				variablesScript.append(buildVariablesScripting(template, ((Solution)parent).getScriptVariables(false), null));
			}
		}
		return variablesScript.toString();
	}

	private String buildVariablesScripting(String template, Iterator<ScriptVariable> variableIterator, String separator)
	{
		StringBuffer variablesScript = new StringBuffer();
		if (variableIterator != null)
		{
			while (variableIterator.hasNext())
			{
				ScriptVariable variable = variableIterator.next();
				String variableScripting = template;
				variableScripting = variableScripting.replace(PROPERTY_VARIABLE_NAME, variable.getName());
				variableScripting = variableScripting.replace(PROPERTY_VARIABLE_DEFAULT_VALUE,
					variable.getDefaultValue() == null ? "\"null\"" : JSONObject.quote(variable.getDefaultValue()));
				variableScripting = variableScripting.replace(PROPERTY_VARIABLE_TYPE, String.valueOf(variable.getVariableType()));
				variablesScript.append(variableScripting);
				if (separator != null && variableIterator.hasNext()) variablesScript.append(separator);
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
					functionsScript.append(",\n"); //$NON-NLS-1$
				}
			}
		}
		return functionsScript.toString();
	}

	private String getAnonymousScripting(ScriptMethod method)
	{
		String declaration = method.getDeclaration();

		declaration = ScriptEngine.docStripper.matcher(declaration).replaceFirst("");
		// convert to JSON escaped string
		return JSONObject.quote(declaration);
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
	 * @param timeout the request timeout interval
	 */
	public void setTimeout(int timeout)
	{
		this.timeout = timeout;
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

	public int getTimeout()
	{
		return timeout;
	}

	private boolean skipConnect = false;

	public void setSkipConnect(boolean connect)
	{
		this.skipConnect = connect;
	}

	/**
	 * @param form
	 * @param persist
	 * @param key
	 * @param methodID
	 * @return
	 */
	private String generateMethodCall(final IPersist parent, IPersist persist, String key, int methodID)
	{
		IScriptProvider sm = ModelUtils.getScriptMethod(parent, parent, null, methodID);
		if (sm != null)
		{
			List<Object> arguments = ((AbstractBase)persist).getInstanceMethodArguments(key);
			StringBuilder sb = new StringBuilder(ScopesUtils.getScopeString(((AbstractScriptProvider)sm).getScopeName(), sm.getDataProviderID()));
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
