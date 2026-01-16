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

package com.servoy.eclipse.model.mobile.exporter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.sql.Types;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jshybugger.instrumentation.DebugInstrumentator;
import org.jshybugger.instrumentation.JsCodeLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.ast.AstRoot;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectHandlerFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.base.nongwt.test.LineMapper;
import com.servoy.base.persistence.constants.IComponentConstants;
import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.base.util.I18NProvider;
import com.servoy.eclipse.model.Activator;
import com.servoy.eclipse.model.ING2WarExportModel;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.test.SolutionJSUnitSuiteCodeBuilder;
import com.servoy.eclipse.model.test.TestTarget;
import com.servoy.eclipse.model.util.IValueFilter;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.war.exporter.ITiNGExportModel;
import com.servoy.eclipse.model.war.exporter.WarExporter;
import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.i18n.I18NMessagesModel;
import com.servoy.j2db.i18n.I18NMessagesModel.I18NMessagesModelEntry;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractScriptProvider;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.ContentSpec.Element;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.I18NUtil.MessageEntry;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportScriptProviders;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.scripting.ScriptEngine;
import com.servoy.j2db.server.ngclient.AngularFormGenerator;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.DeletePathVisitor;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class MobileExporter
{

	public static final String TEST_WAR_SUFFIX = "_TEST"; // do not change this; or if you do update Jenkins scripts both in R&D Jenkins and on "Building a Software Factory" wiki page

	public static final int DEFAULT_SYNC_TIMEOUT = 30;

	private static final String RELATIVE_TEMPLATE_PATH = "resources/solution.js";
	private static final String RELATIVE_WAR_PATH = "resources/servoy_mobile.war";
	private static final String HTML_FILE = "servoy_mobile.html";
	private static final String MOBILE_MODULE_NAME = "mobileclient";

	private static final String RELATIVE_TEST_WAR_PATH = "resources/servoy_mobile_jsunit.war";
	private static final String HTML_TEST_FILE = "servoy_mobile_jsunit.html";
	private static final String MOBILE_TEST_MODULE_NAME = "mobiletestclient";

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

	private static final String JQUERY_MOBILE_THEME_FILE = "jquery.mobile.theme.css";
	private static final String JQUERY_MOBILE_ICONS_FILE = "jquery.mobile.icons.css";

	// this constant is also defined in testing.js inside servoy_mobile_testing and TestSuiteController.java; please update those as well if you change the value
	private static final String SCOPE_NAME_SEPARATOR = "_sNS_";

	private File outputFolder;
	private String serverURL;
	private String solutionName;
	private String serviceSolutionName;
	private int timeout = DEFAULT_SYNC_TIMEOUT;
	private File configFile = null;
	private boolean skipConnect = false;
	private boolean useTestWar = false;
	private String testSuiteCode;
	private boolean debugMode = false;
	private final Map<String, Integer> filenameEndings = new HashMap<String, Integer>();

	private FlattenedSolution fs;
	private LineMapper lineMapper;

	private List<String> mediaOrder;

	private final ITiNGExportModel model;

	public static String getDefaultServerURL()
	{
		return "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort();
	}

	public MobileExporter(ITiNGExportModel model)
	{
		this.model = model != null ? model : new ITiNGExportModel()
		{
			// a bit of hack that we have a set that just says true for contains so the ting export will build with all component and services (just like if the model was null)
			AbstractSet<String> set = new AbstractSet<String>()
			{
				@Override
				public boolean contains(Object o)
				{
					return true;
				}

				@Override
				public int size()
				{
					return 0;
				}

				@Override
				public Iterator<String> iterator()
				{
					return Collections.emptyIterator();
				}
			};

			@Override
			public String exportNG2Mode()
			{
				// with a null this is in developer so run it in debug mode.
				return "build_mobile_debug";
			}

			@Override
			public Set<String> getExportedPackagesExceptSablo()
			{
				return set;
			}

			@Override
			public Set<String> getAllExportedComponents()
			{
				return set;
			}

			@Override
			public Set<String> getAllExportedServicesWithoutSabloServices()
			{
				return set;
			}
		};
	}

	private Pair<String, Boolean> doMediaExport(ZipOutputStream zos, File outputFolder) throws IOException
	{
		StringBuilder headerJS = new StringBuilder("var mediaJS = ["); //$NON-NLS-1$
		StringBuilder headerCSS = new StringBuilder("var mediaCSS = ["); //$NON-NLS-1$

		boolean isJqueryThemeOverwriten = false;
		FlattenedSolution flattenedSolution = getFlattenedSolution();
		if (flattenedSolution != null)
		{
			boolean headerJSEmpty = true, headerCSSEmpty = true;
			for (String mediaName : getMediaOrder())
			{
				Media media = flattenedSolution.getMedia(mediaName);
				byte[] content = media.getMediaData();
				boolean isThemeCSS = mediaName.equals(JQUERY_MOBILE_THEME_FILE) || mediaName.equals(JQUERY_MOBILE_ICONS_FILE);

				if (!isThemeCSS)
				{
					if (MIME_JS.equals(media.getMimeType()))
					{
						if (headerJSEmpty) headerJSEmpty = false;
						else headerJS.append(',');
						headerJS.append('"').append(media.getName()).append('"');
					}
					else if (MIME_CSS.equals(media.getMimeType()))
					{
						if (headerCSSEmpty) headerCSSEmpty = false;
						else headerCSS.append(',');
						headerCSS.append('"').append(media.getName()).append('"');
					}
				}
				else if (!isJqueryThemeOverwriten)
				{
					isJqueryThemeOverwriten = true;
				}

				String mediaOutputFolder = isThemeCSS ? (useTestWar ? MOBILE_TEST_MODULE_NAME : MOBILE_MODULE_NAME) : "media";
				addZipEntry(mediaOutputFolder + "/" + media.getName(), zos, new ByteArrayInputStream(content)); //$NON-NLS-1$
				if (outputFolder != null)
				{
					File outputFolderJS = new File(outputFolder, mediaOutputFolder);
					outputFolderJS.mkdirs();
					Utils.writeFile(new File(outputFolderJS, media.getName()), content);
				}
			}
		}

		return new Pair<String, Boolean>(headerJS.append("];\n").append(headerCSS).append("];").toString(), Boolean.valueOf(isJqueryThemeOverwriten)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public String doPersistExport()
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
						ApplicationServerRegistry.get().getDeveloperRepository(), true, new IValueFilter()
						{

							public Object getFilteredValue(IPersist persist, Map<String, Object> property_values, String key, String value)
							{
								Element contentSpec = StaticContentSpecLoader.getContentSpec().getPropertyForObjectTypeByName(persist.getTypeID(), key);
								if (contentSpec != null && contentSpec.getTypeID() == IRepository.ELEMENTS &&
									(BaseComponent.isEventProperty(contentSpec.getName()) || BaseComponent.isCommandProperty(contentSpec.getName())))
								{
									try
									{
										Object methodID = ApplicationServerRegistry.get().getDeveloperRepository().convertArgumentStringToObject(
											contentSpec.getTypeID(), value);
										if (methodID instanceof String methodUUIDString)
										{
											return generateMethodCall(form, persist, key, methodUUIDString);
										}
									}
									catch (Exception e)
									{
										ServoyLog.logError(e);
									}
								}
								if (persist instanceof GraphicalComponent gc && !property_values.containsKey(IComponentConstants.VIEW_TYPE_ATTR))
								{
									property_values.put(IComponentConstants.VIEW_TYPE_ATTR, ComponentFactory.isButton(gc)
										? IComponentConstants.VIEW_TYPE_BUTTON : IComponentConstants.VIEW_TYPE_LABEL);
								}
								if (value != null && contentSpec != null)
								{
									if (contentSpec.getName().equals(StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES.getPropertyName()))
									{
										try
										{
											ServoyJSONObject customProperties = new ServoyJSONObject(value, false, false, false);
											return ServoyJSONObject.toString(customProperties, false, false, false);
										}
										catch (Exception ex)
										{
											ServoyLog.logError(ex);
										}

									}
									else if (contentSpec.getName().equals(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName()))
								{
									try
									{
										ServoyJSONObject json = new ServoyJSONObject(value, false, false, false);

										if (persist instanceof WebComponent wc)
										{
											WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState()
												.getWebObjectSpecification(wc.getTypeName());
											if (spec != null)
											{
												Map<String, WebObjectHandlerFunctionDefinition> handlers = spec.getHandlers();
												for (WebObjectHandlerFunctionDefinition handler : handlers.values())
												{
													String name = handler.getName();
													String handlerValue = json.optString(name, null);
													if (handlerValue != null)
													{
														String call = generateMethodCall(form, persist, name, handlerValue);
														json.put(name, call);
													}
												}
											}
										}

										return json;
									}
									catch (Exception ex)
									{
										ServoyLog.logError(ex);
									}
								}
								}
								return value;
							}
						});
					if (form.getUUID().toString().equals(flattenedSolution.getSolution().getFirstFormID()))
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
							ApplicationServerRegistry.get().getDeveloperRepository(), true, null);
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
						IValueList realValueList = ComponentFactory.getRealValueList(Activator.getDefault().getMobileExportClient(), valuelist, true,
							Types.OTHER, null, null);
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
			if (serverURL != null) solutionModel.put("serverURL", serverURL);
			if (serviceSolutionName != null) solutionModel.put("serviceSolutionName", serviceSolutionName);
			solutionModel.put("timeout", timeout);
			solutionModel.put("skipConnect", Boolean.valueOf(skipConnect));
			solutionModel.put("mustAuthenticate", Boolean.valueOf(solution.getMustAuthenticate()));
			Media media = flattenedSolution.getMedia(solution.getStyleSheetID());
			if (media != null)
			{
				solutionModel.put("styleSheet", MediaResourcesServlet.SERVOY_SOLUTION_CSS + media.getName().replace(".less", ".css"));
			}

			String onOpenMethodUUID = solution.getOnOpenMethodID();
			if (onOpenMethodUUID != null)
			{
				solutionModel.put("onSolutionOpen",
					generateMethodCall(solution, solution, StaticContentSpecLoader.PROPERTY_ONOPENMETHODID.getPropertyName(), onOpenMethodUUID));
			}

			if (solution.getLoginFormID() != null)
			{
				solutionModel.put("loginForm", flattenedSolution.getForm(solution.getLoginFormID()).getName());
			}
			I18NMessagesModel i18nModel = new I18NMessagesModel(solution.getI18nDataSource(), null, null, null, null);
			// TODO why only german here?
			// load default and german translations for now
			i18nModel.setLanguage(Locale.GERMANY);
			Map<String, I18NMessagesModelEntry> defaultProperties = i18nModel.getDefaultMap();
			TreeMap<String, String> allI18nData = new TreeMap<String, String>();
			for (Entry<String, I18NMessagesModelEntry> entry : defaultProperties.entrySet())
			{
				if (entry.getKey().toLowerCase().startsWith(I18NProvider.MOBILE_KEY_PREFIX))
				{
					allI18nData.put("." + entry.getKey(), (entry.getValue().defaultvalue == null ? "" : entry.getValue().defaultvalue));
					allI18nData.put("de." + entry.getKey(), (entry.getValue().localeValue == null ? "" : entry.getValue().localeValue));
				}
			}
			if (solution.getI18nDataSource() != null)
			{
				EclipseMessages messagesManager = ServoyModelFinder.getServoyModel().getMessagesManager();
				TreeMap<String, MessageEntry> allFromDataSource = messagesManager.getDatasourceMessages(solution.getI18nDataSource());
				if (allFromDataSource != null)
				{
					for (Entry<String, MessageEntry> e : allFromDataSource.entrySet())
					{
						allI18nData.put(e.getKey(), e.getValue().getValue());
					}
				}
			}
			if (allI18nData.size() > 0)
			{
				solutionModel.put("i18n", allI18nData);
			}
			ServoyJSONObject jsonObject = new ServoyJSONObject(solutionModel);
			jsonObject.setNoQuotes(false);
			return ("var _solutiondata_ = " + jsonObject.toString());
		}
		return null;

	}

	/**
	 * @return
	 */
	private FlattenedSolution getFlattenedSolution()
	{
		if (fs == null)
		{
			fs = ServoyModelFinder.getServoyModel().getFlattenedSolution();
			if (solutionName != null && !fs.getSolution().getName().equals(solutionName))
			{
				ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solutionName);
				if (servoyProject == null)
					throw new RuntimeException("Mobile Exporter tries to export: " + solutionName + " but that is not a project in the current workspace");
				fs = new FlattenedSolution();
				try
				{
					IApplicationServer as = ApplicationServerRegistry.getService(IApplicationServer.class);
					fs.setSolution(servoyProject.getSolutionMetaData(), true, true, new AbstractActiveSolutionHandler(as)
					{
						@Override
						public IRepository getRepository()
						{
							return ApplicationServerRegistry.get().getDeveloperRepository();
						}

						@Override
						protected Solution loadSolution(RootObjectMetaData solutionDef) throws RemoteException, RepositoryException
						{
							ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solutionDef.getName());
							if (servoyProject != null)
							{
								return servoyProject.getSolution();
							}
							return null;
						}

						@Override
						protected Solution loadLoginSolution(SolutionMetaData mainSolutionDef, SolutionMetaData loginSolutionDef)
							throws RemoteException, RepositoryException
						{
							return loadSolution(loginSolutionDef);
						}
					});
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}
		return fs;
	}

	public String doScriptingExport()
	{
		FlattenedSolution flattenedSolution = getFlattenedSolution();
		if (flattenedSolution != null)
		{
			String template = Utils.getTXTFileContent(getClass().getResourceAsStream(RELATIVE_TEMPLATE_PATH), Charset.forName("UTF8"));
			ScriptStringBuilder builder = new ScriptStringBuilder(useTestWar); // when creating test war code we need to map line numbers so as to show correct stacks for failures/errors

			if (debugMode)
			{
				String url = serverURL;
				int port = url.lastIndexOf(':');
				if (port > 7)
				{ // ship the http://
					url = url.substring(0, port);
				}
				builder.append("JsHybuggerConfig = {\n");
				builder.append("endpoint: '");
				builder.append(url);
				builder.append(":8889/jshybugger/'\n"); // for now hard coded 8889 port
				builder.append("};\n");

				InputStream resourceAsStream = JsCodeLoader.class.getResourceAsStream("/jshybugger.js");
				String txtFileContent = Utils.getTXTFileContent(resourceAsStream, Charset.forName("UTF8"), true);
				builder.append(txtFileContent);
				builder.append("\nDebugger.setScriptSource = function(params) {\n");
				builder.append("eval(params.source);\n");
				builder.append("}\n");

			}


			int formsLoopStartIndex = template.indexOf(FORM_LOOP_START);
			int formsLoopEndIndex = template.indexOf(FORM_LOOP_END);
			builder.append(template.substring(0, formsLoopStartIndex));
			replaceFormsScripting(builder, template.substring(formsLoopStartIndex + FORM_LOOP_START.length(), formsLoopEndIndex), null);

			int scopesLoopStartIndex = template.indexOf(SCOPES_LOOP_START);
			int scopesLoopEndIndex = template.indexOf(SCOPES_LOOP_END);
			builder.append(template.substring(formsLoopEndIndex + FORM_LOOP_END.length(), scopesLoopStartIndex));
			replaceScopesScripting(builder, template.substring(scopesLoopStartIndex + SCOPES_LOOP_START.length(), scopesLoopEndIndex), null);

			int allVariablesLoopStartIndex = template.indexOf(VARIABLES_LOOP_START, scopesLoopEndIndex);
			int allVariablesLoopEndIndex = template.indexOf(VARIABLES_LOOP_END, scopesLoopEndIndex);
			builder.append(template.substring(scopesLoopEndIndex + SCOPES_LOOP_END.length(), allVariablesLoopStartIndex));
			builder.append(replaceVariablesScripting(template.substring(allVariablesLoopStartIndex + VARIABLES_LOOP_START.length(), allVariablesLoopEndIndex),
				flattenedSolution, null));

			formsLoopStartIndex = template.indexOf(FORM_LOOP_START, allVariablesLoopEndIndex);
			formsLoopEndIndex = template.indexOf(FORM_LOOP_END, allVariablesLoopEndIndex);
			builder.append(template.substring(allVariablesLoopEndIndex + VARIABLES_LOOP_END.length(), formsLoopStartIndex));
			replaceFormsScripting(builder, template.substring(formsLoopStartIndex + FORM_LOOP_START.length(), formsLoopEndIndex), ",\n");

			scopesLoopStartIndex = template.indexOf(SCOPES_LOOP_START, formsLoopStartIndex);
			scopesLoopEndIndex = template.indexOf(SCOPES_LOOP_END, formsLoopStartIndex);
			builder.append(template.substring(formsLoopEndIndex + FORM_LOOP_END.length(), scopesLoopStartIndex));
			replaceScopesScripting(builder, template.substring(scopesLoopStartIndex + SCOPES_LOOP_START.length(), scopesLoopEndIndex), ",\n");

			builder.append(template.substring(scopesLoopEndIndex + SCOPES_LOOP_END.length()));
			if (debugMode && filenameEndings.size() > 0)
			{
				for (Entry<String, Integer> entry : filenameEndings.entrySet())
				{
					builder.append("\nJsHybugger.loadFile('");
					builder.append(entry.getKey());
					builder.append("', ");
					builder.append(String.valueOf(entry.getValue()));
					builder.append(");");
				}
			}
			return builder.toString();
		}
		return null;
	}

	private File createTempDir() throws IOException
	{
		File tmpDir = new File(System.getProperty("java.io.tmpdir"));

		File tmpWarDir = new File(tmpDir, "warexport" + System.currentTimeMillis() + "/");
		if (!tmpWarDir.mkdirs())
		{
			throw new IOException("Can't create the temp dir  " + tmpWarDir);
		}
		return tmpWarDir;
	}

	public File doExport(boolean exportAsZip, IProgressMonitor monitor) throws IOException
	{
		lineMapper = useTestWar ? new LineMapper() : null;
		// first generate the dist TiNG folder:
		File tmpWarDir = createTempDir();
		Activator.getDefault().exportNG2ToWar(new ING2WarExportModel()
		{

			@Override
			public IProgressMonitor getProgressMonitor()
			{
				return monitor;
			}

			@Override
			public ITiNGExportModel getModel()
			{
				return model;
			}

			@Override
			public File getExportLocation()
			{
				return tmpWarDir;
			}

			public String getSolutionName()
			{
				IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
				FlattenedSolution solution = servoyModel.getFlattenedSolution();
				return solution.getName();
			}
		});

		WarExporter.compileLessResources(tmpWarDir);

		String formJson = doPersistExport();
		String solutionJavascript = doScriptingExport();

		StringBuilder modelData = new StringBuilder();
		modelData.append("var _formdata_ = [");
		FlattenedSolution flattenedSolution = getFlattenedSolution();

		String prevValue = Settings.getInstance().getProperty(Settings.TESTING_MODE, "false");
		try
		{
			Settings.getInstance().setProperty(Settings.TESTING_MODE, "true");
			flattenedSolution.getForms(false).forEachRemaining(form -> {
				AngularFormGenerator generator = new AngularFormGenerator(flattenedSolution, form, form.getName(), false, null);
				try
				{
					String formModel = generator.generateJS();
					modelData.append(formModel);
					modelData.append(",\n");
				}
				catch (IOException e)
				{
					Debug.error("Can't generate angular model for form " + form, e);
				}
			});
			modelData.append("]");
		}
		finally
		{
			Settings.getInstance().setProperty(Settings.TESTING_MODE, prevValue);
		}
		String modelDataString = modelData.toString();

		JSONObject spec = new JSONObject();

		WebObjectSpecification[] allWebObjectSpecifications = WebComponentSpecProvider.getSpecProviderState().getAllWebObjectSpecifications();
		for (WebObjectSpecification webObjectSpecification : allWebObjectSpecifications)
		{
			try (InputStream stream = webObjectSpecification.getSpecURL().openStream())
			{
				String content = IOUtils.toString(stream, Charset.forName("UTF8"));
				JSONObject json = new JSONObject(content);
				JSONObject properties = json.getJSONObject("model");
				// strip it a bit
				properties.keys().forEachRemaining(key -> {
					if (properties.get(key) instanceof JSONObject property)
					{
						property.remove("default");
						property.remove("tags");
						property.remove("values");
					}
					else
					{
						// replace the type string with a json object for easer use in the client
						JSONObject property = new JSONObject();
						property.put("type", properties.get(key));
						properties.put(key, property);
					}
				});


				JSONObject api = json.optJSONObject("api");
				JSONObject component = new JSONObject();
				component.put("model", properties);
				component.put("api", api);

				spec.put(webObjectSpecification.getName(), component);
			}
		}

		String specDataString = "var _specdata_ = " + spec.toString();


		// Write files for running from java source
		File tmpP = new File(outputFolder.getParent() + "/src/com/servoy/mobile/public");
		boolean developmentWorkspaceExport = "war".equals(outputFolder.getName()) && outputFolder.getParent() != null && tmpP.exists();

		if (developmentWorkspaceExport)
		{
			File outputFile = new File(tmpP, "solution.js");
			Utils.writeTXTFile(outputFile, solutionJavascript);
			Utils.writeTXTFile(new File(outputFolder, MOBILE_MODULE_NAME + "/solution.js"), solutionJavascript);

			outputFile = new File(tmpP, "solution_json.js");
			Utils.writeTXTFile(outputFile, formJson);
			Utils.writeTXTFile(new File(outputFolder, MOBILE_MODULE_NAME + "/solution_json.js"), formJson);

			outputFile = new File(tmpP, "form_json.js");
			Utils.writeTXTFile(outputFile, modelDataString);
			Utils.writeTXTFile(new File(outputFolder, MOBILE_MODULE_NAME + "/form_json.js"), modelDataString);

			outputFile = new File(tmpP, "spec_json.js");
			Utils.writeTXTFile(outputFile, modelDataString);
			Utils.writeTXTFile(new File(outputFolder, MOBILE_MODULE_NAME + "/spec_json.js"), specDataString);
		}

		File exportedFile = null;
		InputStream is = this.getClass().getResourceAsStream(useTestWar ? RELATIVE_TEST_WAR_PATH : RELATIVE_WAR_PATH);
		if (is != null)
		{
			String moduleName = useTestWar ? MOBILE_TEST_MODULE_NAME : MOBILE_MODULE_NAME;
			String htmlFile = useTestWar ? HTML_TEST_FILE : HTML_FILE;
			Map<String, String> renameMap = new HashMap<String, String>();
			buildRenameEntriesList(renameMap);
			String fileNameWithoutExtension = solutionName + (useTestWar ? "_TEST" : "");
			exportedFile = new File(outputFolder, fileNameWithoutExtension + (exportAsZip ? ".zip" : ".war"));
			exportedFile.getParentFile().mkdirs();
			final ZipOutputStream warStream = new ZipOutputStream(new FileOutputStream(exportedFile));
			try
			{
				Pair<String, Boolean> doMediaExportReturn = doMediaExport(warStream, developmentWorkspaceExport ? outputFolder : null);
				String mediaExport = doMediaExportReturn.getLeft();
				boolean isJqueryThemeOverwriten = doMediaExportReturn.getRight().booleanValue();

				ZipInputStream zipStream = new ZipInputStream(is);
				ZipEntry entry = zipStream.getNextEntry();
				while (entry != null)
				{
					InputStream contentStream = null;
					String entryName = entry.getName();
					if (entryName.equals(htmlFile) || entryName.equals(moduleName + "/" + moduleName + ".nocache.js"))
					{
						String fileContent = Utils.getTXTFileContent(zipStream, Charset.forName("UTF8"), false);
						for (String key : renameMap.keySet())
						{
							if (debugMode && (key.equals("solution.js") || key.equals("solution_json.js")))
							{
								String url = serverURL;
								int port = url.lastIndexOf(':');
								if (port > 7)
								{ // ship the http://
									url = url.substring(0, port);
								}
								String params = "?t=" + timeout + "&sc=" + skipConnect + "&s=" + getFlattenedSolution().getSolution().getName() + "&u=" +
									serverURL;
								if (serviceSolutionName != null) params += "&ss=" + serviceSolutionName;

								fileContent = fileContent.replaceAll(Pattern.quote("' + base + '" + key), url + ":8889/" + key + params);
							}
							else
							{
								fileContent = fileContent.replaceAll(Pattern.quote(key), renameMap.get(key));
							}
						}
						if (entryName.equals(htmlFile))
						{
							String angularIndex = Utils.getTXTFileContent(new File(tmpWarDir, "WEB-INF/angular-index.html"), Charset.forName("UTF8"));

							angularIndex = angularIndex.replaceAll("<base href=\"/\">", "");
							int headIndex = angularIndex.indexOf("</head>");


							fileContent = fileContent.replace("<!--SOLUTION_NAME-->", solutionName);
							fileContent = fileContent.replaceAll(Pattern.quote("<!--SOLUTION_MEDIA_JS_PLACEHOLDER-->"), mediaExport);
							fileContent = fileContent.replaceAll(Pattern.quote("<!--PHONEGAP_JS_PLACEHOLDER-->"),
								exportAsZip ? "<script src=\"cordova.js\"></script>" : "");
							fileContent = angularIndex.substring(0, headIndex) + fileContent + angularIndex.substring(headIndex);
							if (developmentWorkspaceExport)
							{
								String indexContent = Utils.getTXTFileContent(new FileInputStream(new File(outputFolder, htmlFile)), Charset.forName("UTF8"),
									false);
								File outputFile = new File(outputFolder, "index.html");
								indexContent = indexContent.replace("<!--SOLUTION_NAME-->", solutionName);
								indexContent = indexContent.replaceAll(Pattern.quote("<!--SOLUTION_MEDIA_JS_PLACEHOLDER-->"), mediaExport);
								indexContent = indexContent.replaceAll(Pattern.quote("<!--PHONEGAP_JS_PLACEHOLDER-->"), "");
								indexContent = angularIndex.substring(0, headIndex) + indexContent + angularIndex.substring(headIndex);
								Utils.writeTXTFile(outputFile, indexContent);
							}
						}
						contentStream = Utils.getUTF8EncodedStream(fileContent);
					}
					else if (useTestWar && entryName.equals("WEB-INF/web.xml"))
					{
						String fileContent = Utils.getTXTFileContent(zipStream, Charset.forName("UTF8"), false);
						fileContent = fileContent.replaceAll(Pattern.quote("___DEPLOYED_CONTEXT_NAME___"), fileNameWithoutExtension);
						contentStream = Utils.getUTF8EncodedStream(fileContent);
					}

					if (renameMap.containsKey(entryName))
					{
						entryName = renameMap.get(entryName);
					}

					if (!(isJqueryThemeOverwriten &&
						(entryName.equals(moduleName + "/" + JQUERY_MOBILE_THEME_FILE) || entryName.equals(moduleName + "/" + JQUERY_MOBILE_ICONS_FILE))) &&
						(!exportAsZip || !entryName.startsWith("WEB-INF")))
					{
						addZipEntry(entryName, warStream, contentStream != null ? contentStream : zipStream);
					}
					entry = zipStream.getNextEntry();
				}
				Utils.closeInputStream(zipStream);

				addZipEntry(moduleName + "/" + renameMap.get("spec_json.js"), warStream, Utils.getUTF8EncodedStream(specDataString));
				addZipEntry(moduleName + "/" + renameMap.get("form_json.js"), warStream, Utils.getUTF8EncodedStream(modelDataString));
				addZipEntry(moduleName + "/" + renameMap.get("solution_json.js"), warStream, Utils.getUTF8EncodedStream(formJson));
				addZipEntry(moduleName + "/" + renameMap.get("solution.js"), warStream, Utils.getUTF8EncodedStream(solutionJavascript));

				// for unit test client
				if (useTestWar && testSuiteCode != null)
				{
					String generatedJSLocation = renameMap.get("testSuite_generatedCode.js");
					addZipEntry(moduleName + "/" + generatedJSLocation, warStream, Utils.getUTF8EncodedStream(testSuiteCode));
					addZipEntry(moduleName + "/" + renameMap.get("testSuite_generatedCodeLocation.js"), warStream,
						Utils.getUTF8EncodedStream("var __generatedCodeLocation = '" + generatedJSLocation + "';"));
					addZipEntry(moduleName + "/lineMapping.properties", warStream, lineMapper.toProperties());
				}

				if (exportAsZip && configFile != null && configFile.exists())
				{
					InputStream configStream = new FileInputStream(configFile);
					addZipEntry("config.xml", warStream, configStream);
					Utils.closeInputStream(configStream);
				}

				// add TiNG code to the WAR
				Arrays.asList(tmpWarDir.listFiles()).forEach(f -> handleWarEntry(f, warStream, tmpWarDir));
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
				throw e;
			}
			finally
			{
				Utils.closeOutputStream(warStream);
			}
			try
			{
				Files.walkFileTree(tmpWarDir.toPath(), DeletePathVisitor.INSTANCE);
			}
			catch (IOException e)
			{
				// just catch and log here, no need to fail the export if this dir can't be deleted.
				ServoyLog.logError(e);
			}
		}
		else
		{
			ServoyLog.logError("servoy_mobile.war file was not found in com.servoy.eclipse.model.exporter.mobile project", null);
			throw new RuntimeException("War file was not found inside com.servoy.eclipse.model.exporter.mobile project");
		}
		return exportedFile;
	}

	private void handleWarEntry(File file, ZipOutputStream warStream, File tmpWarDir)
	{
		if (file.isDirectory())
		{
			Arrays.asList(file.listFiles()).forEach(f -> handleWarEntry(f, warStream, tmpWarDir));
		}
		else
		{
			try (FileInputStream is = new FileInputStream(file))
			{
				String name = file.getAbsolutePath();
				name = name.substring(tmpWarDir.getAbsolutePath().length()).replace('\\', '/');
				if (name.startsWith("/")) name = name.substring(1);
				addZipEntry(name, warStream, is);
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	private void buildRenameEntriesList(Map<String, String> renameMap)
	{
		String moduleName = useTestWar ? MOBILE_TEST_MODULE_NAME : MOBILE_MODULE_NAME;

		String htmlFile = useTestWar ? HTML_TEST_FILE : HTML_FILE;
		renameMap.put(htmlFile, "index.html");

		addRenameEntries(renameMap, moduleName + "/", "form_json", ".js");
		addRenameEntries(renameMap, moduleName + "/", "spec_json", ".js");
		addRenameEntries(renameMap, moduleName + "/", "solution_json", ".js");
		addRenameEntries(renameMap, moduleName + "/", "solution", ".js");
		addRenameEntries(renameMap, moduleName + "/", "servoy_utils", ".js");
		addRenameEntries(renameMap, moduleName + "/", "servoy", ".css");
		addRenameEntries(renameMap, moduleName + "/", moduleName + ".nocache", ".js");

		if (useTestWar && testSuiteCode != null)
		{
			addRenameEntries(renameMap, moduleName + "/", "testSuite_generatedCode", ".js");
			addRenameEntries(renameMap, moduleName + "/", "testSuite_generatedCodeLocation", ".js");
		}
	}

	private void addRenameEntries(Map<String, String> renameMap, String prefixLocation, String name, String subfix)
	{
		String mobileClientFileName = name + /* '_' + System.currentTimeMillis() + */ subfix;
		renameMap.put(prefixLocation + name + subfix, prefixLocation + mobileClientFileName); // for zip entry path replace
		renameMap.put(name + subfix, mobileClientFileName); // for content replacement
	}

	private void addZipEntry(String entryName, ZipOutputStream stream, InputStream inputStream) throws IOException
	{
		stream.putNextEntry(new ZipEntry(entryName));
		Utils.streamCopy(inputStream, stream);
		stream.closeEntry();
	}

	private void replaceFormsScripting(ScriptStringBuilder scriptResult, String template, String separator)
	{
		FlattenedSolution flattenedSolution = getFlattenedSolution();
		if (flattenedSolution != null)
		{
			Iterator<Form> formIterator = flattenedSolution.getForms(true);
			while (formIterator.hasNext())
			{
				Form form = formIterator.next();
				addVariablesAndFunctionsScripting(template.replace(PROPERTY_FORM_NAME, form.getName()), scriptResult, form, null);
				if (separator != null && formIterator.hasNext())
				{
					scriptResult.append(separator);
				}
			}
		}
	}

	private void addVariablesAndFunctionsScripting(String template, ScriptStringBuilder scriptResult, ISupportScriptProviders parent, String scopeName)
	{
		int functionsLoopStartIndex = template.indexOf(FUNCTIONS_LOOP_START);
		int functionsLoopEndIndex = template.indexOf(FUNCTIONS_LOOP_END, functionsLoopStartIndex);
		if (functionsLoopStartIndex >= 0)
		{
			scriptResult.append(template.substring(0, functionsLoopStartIndex));
			replaceFunctionsScripting(scriptResult, template.substring(functionsLoopStartIndex + FUNCTIONS_LOOP_START.length(), functionsLoopEndIndex), parent,
				scopeName);
		}

		int variablesLoopStartIndex = template.indexOf(VARIABLES_LOOP_START, functionsLoopEndIndex);
		int variablesLoopEndIndex = template.indexOf(VARIABLES_LOOP_END, functionsLoopEndIndex);
		if (variablesLoopStartIndex >= 0)
		{
			scriptResult.append(template.substring(functionsLoopEndIndex + FUNCTIONS_LOOP_END.length(), variablesLoopStartIndex));
			scriptResult.append(replaceVariablesScripting(template.substring(variablesLoopStartIndex + VARIABLES_LOOP_START.length(), variablesLoopEndIndex),
				parent, scopeName));
		}
		scriptResult.append(variablesLoopEndIndex >= 0 ? template.substring(variablesLoopEndIndex + VARIABLES_LOOP_END.length()) : template);
	}

	private void replaceScopesScripting(ScriptStringBuilder scriptResult, String template, String separator)
	{
		FlattenedSolution flattenedSolution = getFlattenedSolution();
		if (flattenedSolution != null)
		{
			Iterator<Pair<String, IRootObject>> scopeIterator = flattenedSolution.getScopes().iterator();
			while (scopeIterator.hasNext())
			{
				Pair<String, IRootObject> scope = scopeIterator.next();
				if (((Solution)scope.getRight()).getSolutionType() == SolutionMetaData.MOBILE)
				{
					addVariablesAndFunctionsScripting(template.replace(PROPERTY_SCOPE_NAME, scope.getLeft()), scriptResult, flattenedSolution, scope.getLeft());
					if (separator != null && scopeIterator.hasNext()) scriptResult.append(separator);
				}
			}
		}
	}

	private String replaceVariablesScripting(String template, ISupportScriptProviders parent, String scopeName)
	{
		StringBuffer variablesScript = new StringBuffer();
		if (parent instanceof Form)
		{
			variablesScript.append(buildVariablesScripting(template, parent.getScriptVariables(false), ",\n"));
		}
		if (parent instanceof FlattenedSolution)
		{
			if (scopeName != null)
			{
				variablesScript.append(buildVariablesScripting(template, ((FlattenedSolution)parent).getScriptVariables(scopeName, false), ",\n"));
			}
			else
			{
				// allvariables
				Iterator<Form> formIterator = ((FlattenedSolution)parent).getForms(false);
				while (formIterator.hasNext())
				{
					Form form = formIterator.next();
					variablesScript.append(buildVariablesScripting(template, form.getScriptVariables(false), null));
				}
				variablesScript.append(buildVariablesScripting(template, ((FlattenedSolution)parent).getScriptVariables(false), null));
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
				if (((Solution)variable.getRootObject()).getSolutionType() == SolutionMetaData.MOBILE)
				{
					String variableScripting = template;
					variableScripting = variableScripting.replace(PROPERTY_VARIABLE_NAME, variable.getName());
					variableScripting = variableScripting.replace(PROPERTY_VARIABLE_DEFAULT_VALUE,
						variable.getDefaultValue() == null ? "\"null\"" : JSONObject.quote(variable.getDefaultValue()));
					variableScripting = variableScripting.replace(PROPERTY_VARIABLE_TYPE, String.valueOf(variable.getVariableType()));
					variablesScript.append(variableScripting);
					if (separator != null && variableIterator.hasNext()) variablesScript.append(separator);
				}
			}
		}
		return variablesScript.toString();
	}

	private void replaceFunctionsScripting(ScriptStringBuilder scriptResult, String template, ISupportScriptProviders parent, String scopeName)
	{
		Iterator< ? extends IScriptProvider> methodIterator = null;
		if (parent instanceof Form)
		{
			methodIterator = parent.getScriptMethods(false);
		}
		else if (parent instanceof FlattenedSolution)
		{
			methodIterator = ((FlattenedSolution)parent).getScriptMethods(scopeName, false);
		}
		if (methodIterator != null)
		{
			while (methodIterator.hasNext())
			{
				IScriptProvider method = methodIterator.next();
				String methodScripting = template;
				methodScripting = methodScripting.replace(PROPERTY_FUNCTION_NAME, method.getName());
				int functionCodeIdx = methodScripting.indexOf(PROPERTY_FUNCTION_CODE);
				scriptResult.append(methodScripting.substring(0, functionCodeIdx));
				scriptResult.append("\n");
				getAnonymousScripting(scriptResult, method);
				scriptResult.append(methodScripting.substring(functionCodeIdx + PROPERTY_FUNCTION_CODE.length()));
				if (methodIterator.hasNext())
				{
					scriptResult.append("\n");
				}
			}
		}
	}

	private void getAnonymousScripting(ScriptStringBuilder scriptResult, IScriptProvider method)
	{
		String functionAndName = "function";
		if (useTestWar)
		{
			// see also testing.js -> this function name helps show failed functions in test failures/errors
			// "function " + scopes/forms + _ServoyTesting_.SCOPE_NAME_SEPARATOR + s2Name + _ServoyTesting_.SCOPE_NAME_SEPARATOR + fName;
			functionAndName += " " + (method.getParent().getTypeID() == IRepository.FORMS
				? "forms" + SCOPE_NAME_SEPARATOR + ((Form)method.getParent()).getName() : "scopes" + SCOPE_NAME_SEPARATOR + method.getScopeName()) +
				SCOPE_NAME_SEPARATOR + method.getName();
		}
		String code = ScriptEngine.extractFunction(method.getDeclaration(), functionAndName);

		if (debugMode)
		{
			try
			{
				String workspaceRelativePath = SolutionSerializer.getScriptPath(method, false);
				String scriptPath = serverURL + "/" + workspaceRelativePath;
				byte[] bytes = code.getBytes(Charset.forName("UTF8"));
				ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length * 2);
				ServoyDebugInstrumentator instrumenator = new ServoyDebugInstrumentator();
				JsCodeLoader.instrumentFile(scriptPath, bais, baos, new HashMap<String, Object>(), method.getLineNumberOffset() - 1, instrumenator, false);
				code = new String(baos.toByteArray(), Charset.forName("UTF8"));
				if (useTestWar) lineMapper.mapFunctionDebugMode(scriptResult.getCurrentLineNumber(), workspaceRelativePath, code);

				Integer linenr = filenameEndings.get(scriptPath);
				if (linenr == null || linenr.intValue() < instrumenator.endLine) filenameEndings.put(scriptPath, Integer.valueOf(instrumenator.endLine));
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
			scriptResult.append(code);
		}
		else
		{
			if (useTestWar)
			{
				long beginLineNo = scriptResult.getCurrentLineNumber();
				scriptResult.append(code);
				lineMapper.mapFunction(beginLineNo, scriptResult.getCurrentLineNumber(), SolutionSerializer.getScriptPath(method, false),
					method.getLineNumberOffset());
			}
			else
			{
				scriptResult.append(code);
			}
		}
	}

	public void setConfigFile(File configFile)
	{
		this.configFile = configFile;
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
	 * @return the outputFolder
	 */
	public File getOutputFolder()
	{
		return outputFolder;
	}

	/**
	 * @param timeout the request timeout interval
	 */
	public void setTimeout(int timeout)
	{
		this.timeout = timeout;
	}

	public void setDebugMode(boolean debugMode)
	{
		this.debugMode = debugMode;
	}

	/**
	 * @return the serverURL
	 */
	public String getServerURL()
	{
		return serverURL;
	}

	/**
	 * @return the serviceSolutionName
	 */
	public String getServiceSolutionName()
	{
		return serviceSolutionName;
	}

	/**
	 * @param serviceSolutionName the serviceSolutionName to set
	 */
	public void setServiceSolutionName(String serviceSolutionName)
	{
		this.serviceSolutionName = serviceSolutionName;
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
	private String generateMethodCall(final IPersist parent, IPersist persist, String key, String methodUUID)
	{
		IScriptProvider sm = getFlattenedSolution().getScriptMethod(methodUUID);
		return generateMethodCall(persist, key, sm);
	}

	/**
	 * @param persist
	 * @param key
	 * @param sm
	 * @return
	 */
	private String generateMethodCall(IPersist persist, String key, IScriptProvider sm)
	{
		if (sm != null)
		{
			List<Object> arguments = ((AbstractBase)persist).getFlattenedMethodArguments(key);
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

	/**
	 * By default this is false. If set to true, a unit test - mobile client war will be exported instead of the normal mobile client war.
	 */
	public void useTestWar(TestTarget testTarget, boolean spamInTestNamesAsFullTreePathsForDumbToolsThatAreUnAwareOfTestSuiteHierarchy)
	{
		this.useTestWar = true;

		// include js code needed for testing inside the .war file
		// this means the actual JS unit test suite code for the solution & the starting test suite class to be given to the jsunit runner


		// actual custom test suite code
		StringBuffer testCode = new StringBuffer(512);
		IServoyModel model = ServoyModelFinder.getServoyModel();
		ServoyProject sp = model.getActiveProject();
		Solution s;
		FlattenedSolution flattenedSolution = model.getFlattenedSolution();
		SolutionJSUnitSuiteCodeBuilder builder = new SolutionJSUnitSuiteCodeBuilder();
		if (sp == null || (s = sp.getSolution()) == null || flattenedSolution == null)
		{
			builder.initializeWithError("Cannot create JS Unit suite. Can't find active solution.");
		}
		else
		{
			builder.initializeWithSolution(s, flattenedSolution, testTarget, spamInTestNamesAsFullTreePathsForDumbToolsThatAreUnAwareOfTestSuiteHierarchy);
		}

		testCode.append("if (typeof(this.__customTestSuiteCodeLoaded) == 'undefined') {\nthis.__customTestSuiteCodeLoaded = 1;\n");
		testCode.append("    var __rootTestSuiteClassName = '" + builder.getRootTestClassName() + "';\n");
		testCode.append(builder.getCode());
		testCode.append("    __solutionTestSuite.sendTestTreeAndRun();\n}");

		this.testSuiteCode = testCode.toString();
	}

	private class ServoyDebugInstrumentator extends DebugInstrumentator
	{
		private int endLine;

		@Override
		protected void loadFile(AstRoot node)
		{
			endLine = node.getEndLineno();
		}
	}

	/**
	 * @param input
	 */
	public void setMediaOrder(List<String> mediaOrder)
	{
		ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solutionName);
		Solution editingSolution = servoyProject.getEditingSolution();
		editingSolution.putCustomProperty(new String[] { "mobileexport", "mediaorder" }, mediaOrder);
		try
		{
			servoyProject.saveEditingSolutionNodes(new IPersist[] { editingSolution }, false);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		this.mediaOrder = mediaOrder;
	}

	public List<String> getMediaOrder()
	{
		if (mediaOrder == null)
		{
			FlattenedSolution fs = getFlattenedSolution();
			java.util.List<String> lst = (java.util.List<String>)fs.getSolution().getCustomProperty(new String[] { "mobileexport", "mediaorder" });
			if (lst == null)
			{
				lst = new ArrayList<String>();
			}
			else
			{
				lst = new ArrayList<String>(lst);
			}
			ArrayList<String> allMedia = new ArrayList<String>();
			Iterator<Media> medias = fs.getMedias(true);
			while (medias.hasNext())
			{
				Media m = medias.next();
				allMedia.add(m.getName());
			}
			lst.retainAll(allMedia);
			for (String mediaName : allMedia)
			{
				if (!lst.contains(mediaName))
				{
					lst.add(mediaName);
				}
			}
			mediaOrder = lst;
		}
		return mediaOrder;
	}
}
