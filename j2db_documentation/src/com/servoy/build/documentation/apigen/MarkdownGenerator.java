/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

package com.servoy.build.documentation.apigen;

import static com.servoy.build.documentation.apigen.MarkdownGenerator.Platform.detectPlatform;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.json.JSONObject;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.Scriptable;

import com.servoy.base.scripting.api.IJSController;
import com.servoy.base.scripting.api.IJSDataSet;
import com.servoy.base.scripting.api.IJSFoundSet;
import com.servoy.base.scripting.api.IJSRecord;
import com.servoy.base.solutionmodel.IBaseSMComponent;
import com.servoy.base.solutionmodel.IBaseSMField;
import com.servoy.base.solutionmodel.IBaseSMForm;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.build.documentation.DocumentationManager;
import com.servoy.j2db.dataprocessing.DataException;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.documentation.IFunctionDocumentation;
import com.servoy.j2db.documentation.IObjectDocumentation;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.plugins.IPlugin;
import com.servoy.j2db.plugins.IServerPlugin;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.JSMap;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.annotations.JSRealClass;
import com.servoy.j2db.scripting.solutionmodel.ICSSPosition;
import com.servoy.j2db.server.servlets.ConfigServlet;
import com.servoy.j2db.solutionmodel.ISMPart;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyJSONObject;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.utility.DeepUnwrap;

/**
 * @author jcompagner
 */
public class MarkdownGenerator
{
	enum Platform
	{
		Windows, MacOS, Linux;

		static Platform detectPlatform()
		{
			var osName = System.getProperty("os.name");
			if (osName.startsWith("Windows")) return Windows;
			if ("linux".equalsIgnoreCase(osName)) return Linux;
			return MacOS;
		}
	}

	private static final String EXTENDS_CLASS_SECTION_IN_TEMPLATE = "extends";
	private static final String RETURN_TYPES_SECTION_IN_TEMPLATE = "returnTypes";

	private static URLClassLoader targetInstallClassLoader;

	private static Configuration cfg;
	private static Template template;

	private static final DuplicateTracker duplicateTracker = DuplicateTracker.getInstance();

	private static Map<String, String> specialTypePaths = new HashMap<>();
	private static Platform platform = detectPlatform();
	private static int missingReturnAnnotationCount = 0;
	private static Set<String> undocumentedReturnTypeFunctions = new HashSet<>();
	private static Set<String> uniqueClasses = new HashSet<>();
	private static Set<String> undocumentedTypes = new HashSet<>();
	private static List<String> missingMdFiles = new ArrayList<>();
	private static Map<String, String> referenceTypes = new HashMap<>();
	private static Set<String> summaryPaths = new HashSet<>();
	private static String summaryMdFilePath;
	private static Map<String, String> classNameMap = null;

	private static final boolean TRACK_REFERENCES = false; //used to turn relative references to full urls toward docs.servoy.com
	private static final boolean GENERATE_PGADMIN_DOCS = false; //properties from admin page
	private static final boolean GENERATE_SERVER_DOCS = true; //properties from server plugins

	private static final Set<String> IGNORED_UNDOCUMENTED_TYPES = Set.of(
		"org.mozilla.javascript.IdScriptableObject",
		"org.json.JSONObject",
		"java.util.Map",
		"com.servoy.j2db.scripting.JSMap",
		"com.servoy.j2db.documentation.scripting.docs.Object",
		"java.lang.Object",
		"com.servoy.j2db.util.ServoyJSONObject",
		"org.json.JSONArray",
		"java.awt.print.PrinterJob");

	private static final Set<String> SKIP_MISSING_RETURN_FOR_CLASS = Set.of(
		"com.servoy.j2db.documentation.scripting.docs.Array",
		"com.servoy.j2db.documentation.scripting.docs.BigInt",
		"com.servoy.j2db.documentation.scripting.docs.Date",
		"com.servoy.j2db.documentation.scripting.docs.JSON",
		"com.servoy.j2db.documentation.scripting.docs.Math",
		"com.servoy.j2db.documentation.scripting.docs.Function",
		"com.servoy.j2db.documentation.scripting.docs.String",
		"com.servoy.j2db.documentation.scripting.docs.XML",
		"com.servoy.j2db.documentation.scripting.docs.Map");

	private static final Set<String> STD_DOC_LINKS = Set.of(
		"https://developer.mozilla.org/",
		"https://ecma-international.org/");

	static
	{
		// special return types that are used by the maintenance plugin but they are implemented&come from servoy_shared/core classes
		// but we do want them under the maintenance plugin... not in core docs
		specialTypePaths.put("JSServer", "/reference/servoyextensions/server-plugins/maintenance/");
		specialTypePaths.put("JSTableObject", "/reference/servoyextensions/server-plugins/maintenance/");
		specialTypePaths.put("JSColumnObject", "/reference/servoyextensions/server-plugins/maintenance/");

		// the following ones are not generated .md files; they were manually created at that path; so we have to hardcode them
		specialTypePaths.put("enum", "/reference/servoycore/dev-api/");
		specialTypePaths.put("Exception", "/reference/servoycore/dev-api/");
	}

	private static final HashMap<String, String> qualifiedToName = new HashMap<>();
	private static final HashMap<String, String> publicToRootPath = new HashMap<>();
	private static final HashMap<String, String> returnTypesToParentName = new HashMap<>();
	private static final Map<String, String> publicNameToPluginProviderClassPublicName = new HashMap<>(); // plugins already have the plugin name as dir in their root path; do not generate another dir; it is null for non-plugin docs

	private static final Set<String> storeAsReadMe = new HashSet<>();
	private static final Set<String> doNotStoreAsReadMe = new HashSet<>();

	private static final Set<String> excludedPluginJarNames = Set.of("aibridge.jar");
	private static final List<String> nonDefaultPluginJarNamesThatWeDoGenerateDocsFor = Arrays.asList("servoy_jasperreports.jar");
	private static String gitLocation;

	/**
	 * Mapping between server plugin class names and their documentation provider class names.
	 * Key: Full class name of the plugin
	 * Value: Full class name of the documentation provider
	 */
	private static final Map<String, String> PLUGIN_TO_DOCUMENTATION_PROVIDER = new HashMap<>();

	/**
	 * Stores plugin configuration properties for documentation generation.
	 * Key: Full class name of the documentation provider
	 * Value: Map of property names to their descriptions
	 */
	private static final Map<String, Map<String, String>> PLUGIN_PROPERTIES = new HashMap<>();

	static
	{
		// Initialize the plugin to documentation provider mapping
		PLUGIN_TO_DOCUMENTATION_PROVIDER.put("com.servoy.extensions.plugins.clientmanager.ClientManagerServer",
			"com.servoy.extensions.plugins.clientmanager.ClientManagerProvider");
		PLUGIN_TO_DOCUMENTATION_PROVIDER.put("com.servoy.extensions.plugins.mail.MailServer", "com.servoy.extensions.plugins.mail.client.MailProvider");
		PLUGIN_TO_DOCUMENTATION_PROVIDER.put("com.servoy.extensions.plugins.maintenance.MaintenanceServer",
			"com.servoy.extensions.plugins.maintenance.MaintenanceProvider");
		PLUGIN_TO_DOCUMENTATION_PROVIDER.put("com.servoy.extensions.plugins.pdf_output.PDFFormsPlugin",
			"com.servoy.extensions.plugins.pdf_output.PDFFormsPlugin");
		PLUGIN_TO_DOCUMENTATION_PROVIDER.put("com.servoy.extensions.plugins.file.FileServerPlugin", "com.servoy.extensions.plugins.file.FileProvider");
		PLUGIN_TO_DOCUMENTATION_PROVIDER.put("com.servoy.extensions.plugins.oauth.OAuthPlugin", "com.servoy.extensions.plugins.oauth.OAuthProvider");
		PLUGIN_TO_DOCUMENTATION_PROVIDER.put("com.servoy.extensions.plugins.scheduler.SchedulerServerPlugin",
			"com.servoy.extensions.plugins.scheduler.SchedulerProvider");
		PLUGIN_TO_DOCUMENTATION_PROVIDER.put("com.servoy.extensions.plugins.mobile.service.MobileServicePlugin",
			"com.servoy.extensions.plugins.mobile.service.MobileProvider");
		PLUGIN_TO_DOCUMENTATION_PROVIDER.put("com.servoy.extensions.plugins.mobile.MobilePlugin", "com.servoy.extensions.plugins.mobile.MobileMockupProvider");
		PLUGIN_TO_DOCUMENTATION_PROVIDER.put("com.servoy.extensions.plugins.headlessclient.HeadlessServerPlugin",
			"com.servoy.extensions.plugins.headlessclient.HeadlessClientProvider");
		PLUGIN_TO_DOCUMENTATION_PROVIDER.put("com.servoy.extensions.plugins.rawSQL.SQLProcessor", "com.servoy.extensions.plugins.rawSQL.RawSQLProvider");
		PLUGIN_TO_DOCUMENTATION_PROVIDER.put("com.servoy.extensions.plugins.rest_ws.RestWSPlugin", "com.servoy.extensions.plugins.rest_ws.RestWSPlugin");
		PLUGIN_TO_DOCUMENTATION_PROVIDER.put("com.servoy.extensions.plugins.jwt.JWTServer", "com.servoy.extensions.plugins.jwt.client.JWTProvider");
		PLUGIN_TO_DOCUMENTATION_PROVIDER.put("com.servoy.extensions.plugins.broadcaster.DataNotifyBroadCaster",
			"com.servoy.extensions.plugins.broadcaster.DataNotifyBroadCaster");
	}

	static
	{
		// we always say it's "storeAsReadme" if it has return types - automatically
		// so here we add those public names that don't list return types in the .java files

		storeAsReadMe.add("Forms");
		storeAsReadMe.add("RuntimeForm");
		storeAsReadMe.add("Solution");
		storeAsReadMe.add("elements");
		storeAsReadMe.add("containers");

		// exceptions from the rule that stuff with valid return types are dirs and generate a README.md in that dir:
		// otherwise for example both ServoyException and DataException that extends ServoyException will be seen as having return types and thus generating folders, and we do not want that for DataException...
		doNotStoreAsReadMe.add("DataException");
	}

	private static String servoySourceRoot = "";

	private final Map<String, Object> root;
	private final Path path;
	private final String rootPath;
	private final static java.util.function.Function<String, String> htmlToMarkdownConverter = (initialDescription) -> {
		if (initialDescription == null) return null;

		String convertedDesc = HtmlUtils.applyDescriptionMagic(initialDescription.replace("%%prefix%%", "").replace("%%elementName%%",
			"myElement"));
		convertedDesc = convertedDesc.trim(); // probably not needed
		return SpecMarkdownGenerator.turnHTMLJSDocIntoMarkdown(convertedDesc);
	};

	public MarkdownGenerator(String publicName, String scriptingName, String description,
		String parentPath, List<String> publicNamesOfReturnTypes)
	{
		root = new HashMap<>();
		root.put("classname", publicName);

		/**
		 * Quick convert of HTML to String for use in the template.
		 */
		root.put("MD", (TemplateMethodModelEx)((params) -> htmlToMarkdownConverter.apply((String)DeepUnwrap.unwrap((TemplateModel)params.get(0)))));

		this.rootPath = parentPath;

		if (scriptingName != null && !scriptingName.equals(publicName)) root.put("scriptingname", scriptingName);
		if (description != null) root.put("description", htmlToMarkdownConverter.apply(description));
		String classNoSpace = publicName.replace(" ", "%20").toLowerCase();

		classList(RETURN_TYPES_SECTION_IN_TEMPLATE, publicNamesOfReturnTypes);

		if (storeAsReadMe.contains(publicName))
		{
			classNoSpace = "README";
		}
		root.put("classname_nospace", classNoSpace);
		root.put("instance", this);

		// Check if this is a plugin provider class and inject plugin properties if available
		injectPluginProperties(scriptingName);

		if ("/reference/servoycore/object-model/solution".equals(parentPath))
		{
			String parentOfReturnedType = returnTypesToParentName.get(publicName);
			if (parentOfReturnedType != null)
			{
				path = Paths.get(parentPath, parentOfReturnedType);
			}
			else
			{
				path = Paths.get(parentPath);
			}
		}
		else
		{
			path = Paths.get(generatePathInLauncherOutputDir(publicName));
		}
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException, URISyntaxException, InstantiationException, IllegalAccessException
	{
		cfg = new Configuration(Configuration.VERSION_2_3_31);
		cfg.setTemplateLoader(new ClassTemplateLoader(MarkdownGenerator.class, "template"));
		cfg.setDefaultEncoding("UTF-8");

		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setLogTemplateExceptions(false);
		cfg.setWrapUncheckedExceptions(true);
		cfg.setFallbackOnNullLoopVariable(false);

		fillStaticParents(returnTypesToParentName);


		boolean analyzeOutput = false;
		boolean deleteDuplicates = false;
		if (args.length == 1)
		{
			analyzeOutput = "analyzeOutput".equals(args[0]);
			deleteDuplicates = "deleteDuplicates".equals(args[0]);
		}

		if (analyzeOutput)
		{
			MarkdownOutputAnalyzer.analyze();
			return;
		}

		if (deleteDuplicates)
		{
			MarkdownDeleteDuplicates.deleteDuplicates();
			return;
		}

		String jsLib = args[0];
		String servoyDoc = args[1];
		String designDoc = args[2];
		String pluginDir = args[3];
		gitLocation = args[4];
		summaryMdFilePath = gitLocation + "/gitbook/SUMMARY.md";
		boolean generateForAI = "forAI".equals(args[5]);
		servoySourceRoot = args.length > 6 ? args[6] : "";

		duplicateTracker.init(); // avoid init prior to analyze params; you may overwrite unintentionaly some usefull content for analyzing

		if (generateForAI) template = cfg.getTemplate("ai_template.md");
		else template = cfg.getTemplate("markdown_template.md");

		generateCoreAndPluginDocs(jsLib, servoyDoc, designDoc, pluginDir, generateForAI, new MarkdownDocFromXMLGenerator(generateForAI));
	}

	public static List<String> getSupportedClientsList(ClientSupport clientSupport)
	{
		List<String> support = new ArrayList<>();
		if (clientSupport != null)
		{
			if (clientSupport.hasSupport(ClientSupport.sc))
			{
				support.add("SmartClient");
			}
			if (clientSupport.hasSupport(ClientSupport.wc))
			{
				support.add("WebClient");
			}
			if (clientSupport.hasSupport(ClientSupport.ng))
			{
				support.add("NGClient");
			}
			if (clientSupport.hasSupport(ClientSupport.mc))
			{
				support.add("MobileClient");
			}
		}
		return support;
	}

	public static void fillStaticParents(HashMap<String, String> retTypesToParentName)
	{
		retTypesToParentName.put("DataException", "ServoyException");
		retTypesToParentName.put("RuntimeForm", "Forms");
		retTypesToParentName.put("RuntimeContainer", "RuntimeForm");
		retTypesToParentName.put("containers", "RuntimeForm");
		retTypesToParentName.put("elements", "RuntimeForm");
		retTypesToParentName.put("controller", "RuntimeForm");

		retTypesToParentName.put("RuntimeContainer", "containers");

		retTypesToParentName.put("Component", "elements");
		retTypesToParentName.put("RuntimeAccordionPanel", "elements");
		retTypesToParentName.put("RuntimeDataLabel", "elements");
		retTypesToParentName.put("RuntimeInsetList", "elements");
		retTypesToParentName.put("RuntimeBean", "elements");
		retTypesToParentName.put("RuntimePortal", "elements");
		retTypesToParentName.put("RuntimeLabel", "elements");
		retTypesToParentName.put("RuntimeSplitPane", "elements");
		retTypesToParentName.put("RuntimeTabPanel", "elements");
		retTypesToParentName.put("RuntimeButton", "elements");
		retTypesToParentName.put("RuntimeCalendar", "elements");
		retTypesToParentName.put("RuntimeCheck", "elements");
		retTypesToParentName.put("RuntimeChecks", "elements");
		retTypesToParentName.put("RuntimeCombobox", "elements");
		retTypesToParentName.put("RuntimeComponent", "elements");
		retTypesToParentName.put("RuntimeDataButton", "elements");
		retTypesToParentName.put("RuntimeGroup", "elements");
		retTypesToParentName.put("RuntimeHtmlArea", "elements");
		retTypesToParentName.put("RuntimeImageMedia", "elements");
		retTypesToParentName.put("RuntimeListBox", "elements");
		retTypesToParentName.put("RuntimePassword", "elements");
		retTypesToParentName.put("RuntimeRadio", "elements");
		retTypesToParentName.put("RuntimeRadios", "elements");
		retTypesToParentName.put("RuntimeRectangle", "elements");
		retTypesToParentName.put("RuntimeRtfArea", "elements");
		retTypesToParentName.put("RuntimeSpinner", "elements");
		retTypesToParentName.put("RuntimeTextArea", "elements");
		retTypesToParentName.put("RuntimeTextField", "elements");
		retTypesToParentName.put("RuntimeWebComponent", "elements");


		retTypesToParentName.put("Layout Container", "Form");

		retTypesToParentName.put("RelationItem", "Relation");
	}

	static List<IFunctionDocumentation> getEvents(SortedSet<IFunctionDocumentation> functions)
	{
		return getFilteredList(IFunctionDocumentation.TYPE_EVENT, functions);
	}


	static List<IFunctionDocumentation> getCommands(SortedSet<IFunctionDocumentation> functions)
	{
		return getFilteredList(IFunctionDocumentation.TYPE_COMMAND, functions);
	}


	static List<IFunctionDocumentation> getProperties(SortedSet<IFunctionDocumentation> functions)
	{
		return getFilteredList(IFunctionDocumentation.TYPE_PROPERTY, functions);
	}


	static List<IFunctionDocumentation> getConstants(SortedSet<IFunctionDocumentation> functions)
	{
		return getFilteredList(IFunctionDocumentation.TYPE_CONSTANT, functions);
	}

	static List<IFunctionDocumentation> getMethods(SortedSet<IFunctionDocumentation> functions)
	{
		return getFilteredList(IFunctionDocumentation.TYPE_FUNCTION, functions);
	}

	private static List<IFunctionDocumentation> getFilteredList(Integer typeEvent, SortedSet<IFunctionDocumentation> functions)
	{
		List<IFunctionDocumentation> retValue = new ArrayList<IFunctionDocumentation>();
		ClientSupport fdCs;
		for (IFunctionDocumentation fd : functions)
		{
			fdCs = fd.getClientSupport();
			if (fdCs == null) fdCs = ClientSupport.Default;
			if (fdCs.hasSupport(ClientSupport.Default))
			{
				if (fd.getType().intValue() == typeEvent.intValue())
				{
					retValue.add(fd);
				}
			}
		}
		return retValue.size() > 0 ? retValue : null;
	}

	/**
	 * Loads import statements from ConfigServlet to build a mapping of simple class names to fully qualified names.
	 *
	 * @param map The map to populate with class name mappings
	 */
	private static void loadImportsFromConfigServlet(Map<String, String> map)
	{
		try
		{
			// Get the location of the ConfigServlet class file
			Class< ? > configServletClass = ConfigServlet.class;
			String className = configServletClass.getName();
			String classAsPath = className.replace('.', '/') + ".java";

			// Try to find the source file
			String sourceRoot = "/Users/marianvid/Servoy/git/master/server/j2db_server/src/";
			File sourceFile = new File(sourceRoot, classAsPath);

			if (!sourceFile.exists())
			{
				// Fallback to hardcoded mappings if source file not found
				map.put("Settings", "com.servoy.j2db.util.Settings");
				map.put("ContentSecurityPolicyConfig", "org.sablo.security.ContentSecurityPolicyConfig");
				System.out.println("Using fallback class mappings as ConfigServlet source file not found");
			}
			else
			{
				// Parse the source file to extract imports
				BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
				String line;
				Pattern importPattern = Pattern.compile("import\\s+([\\w\\.]+)\\.([\\w]+);\\s*$");

				while ((line = reader.readLine()) != null)
				{
					Matcher matcher = importPattern.matcher(line);
					if (matcher.matches())
					{
						String packageName = matcher.group(1);
						String simpleClassName = matcher.group(2);
						String fullClassName = packageName + "." + simpleClassName;

						// Add to map
						map.put(simpleClassName, fullClassName);
					}
				}
				reader.close();

				// Add the classes from the same package as ConfigServlet
				String configServletPackage = configServletClass.getPackage().getName();
				File packageDir = new File(sourceRoot, configServletPackage.replace('.', '/'));
				if (packageDir.exists() && packageDir.isDirectory())
				{
					for (File file : packageDir.listFiles())
					{
						if (file.isFile() && file.getName().endsWith(".java"))
						{
							String simpleClassName = file.getName().substring(0, file.getName().length() - 5);
							map.put(simpleClassName, configServletPackage + "." + simpleClassName);
						}
					}
				}
			}

			// Add common classes that might be referenced without imports
			map.put("String", "java.lang.String");
			map.put("Object", "java.lang.Object");
			map.put("Integer", "java.lang.Integer");
			map.put("Boolean", "java.lang.Boolean");
		}
		catch (Exception e)
		{
			Debug.error("Error loading imports from ConfigServlet", e);

			// Fallback to hardcoded mappings if an error occurs
			map.put("Settings", "com.servoy.j2db.util.Settings");
			map.put("ContentSecurityPolicyConfig", "org.sablo.security.ContentSecurityPolicyConfig");
		}
	}

	/**
	 * Resolves a static field reference to its actual value using reflection.
	 * Uses a dynamic approach to find the fully qualified class name based on imports from ConfigServlet.
	 *
	 * @param fieldReference The reference in the format "ClassName.FIELD_NAME"
	 * @return The actual value of the static field, or the original reference if it couldn't be resolved
	 */
	private static String resolveStaticFieldReference(String fieldReference)
	{
		if (fieldReference == null || !fieldReference.contains("."))
		{
			return fieldReference;
		}

		// Initialize class name map if needed
		if (classNameMap == null)
		{
			classNameMap = new HashMap<>();
			loadImportsFromConfigServlet(classNameMap);
		}

		try
		{
			String[] parts = fieldReference.split("\\.", 2);
			String className = parts[0];
			String fieldName = parts[1];

			// Look up the fully qualified class name in our map
			String fullClassName = classNameMap.get(className);

			if (fullClassName == null)
			{
				// If not found in map, try common packages as a fallback
				String[] commonPackages = { "com.servoy.j2db.util", "com.servoy.j2db", "org.sablo.security", "com.servoy.j2db.server", "java.lang"
				};

				for (String pkg : commonPackages)
				{
					try
					{
						String tryClassName = pkg + "." + className;
						Class.forName(tryClassName);
						// If we get here, the class exists
						fullClassName = tryClassName;
						// Add it to our map for future use
						classNameMap.put(className, fullClassName);
						break;
					}
					catch (ClassNotFoundException e)
					{
						// Class not found in this package, continue to next
					}
				}
			}

			if (fullClassName != null)
			{
				Class< ? > clazz = Class.forName(fullClassName);
				Field field = clazz.getField(fieldName);
				Object value = field.get(null); // null for static fields
				return value != null ? value.toString() : fieldReference;
			}
		}
		catch (Exception e)
		{
			Debug.error("Error resolving static field reference: " + fieldReference, e);
		}

		return fieldReference;
	}

	/**
	 * Process a method call string and extract the appropriate value for documentation.
	 * This uses reflection to dynamically invoke the method in the ConfigServlet class.
	 *
	 * @param methodCall The method call string to process
	 * @return The processed value suitable for documentation
	 */
	private static String processMethodCall(String methodCall)
	{
		if (methodCall == null)
		{
			return "";
		}

		// Extract the method name and arguments
		int openParenIndex = methodCall.indexOf('(');
		int closeParenIndex = methodCall.lastIndexOf(')');

		if (openParenIndex == -1 || closeParenIndex == -1 || openParenIndex >= closeParenIndex)
		{
			// Invalid method call format
			return methodCall;
		}

		String methodName = methodCall.substring(0, openParenIndex).trim();
		String argsString = methodCall.substring(openParenIndex + 1, closeParenIndex).trim();

		try
		{
			// Parse the arguments
			List<Object> args = new ArrayList<>();
			List<Class< ? >> argTypes = new ArrayList<>();

			if (!argsString.isEmpty())
			{
				String[] argStrings = argsString.split(",");
				for (String arg : argStrings)
				{
					arg = arg.trim();

					// Check if the argument is a static field reference
					if (arg.contains("."))
					{
						arg = resolveStaticFieldReference(arg);
					}

					// Determine the argument type and convert the string value
					if (arg.equals("true") || arg.equals("false"))
					{
						args.add(Boolean.valueOf(arg));
						argTypes.add(boolean.class);
					}
					else if (arg.matches("-?\\d+"))
					{
						try
						{
							args.add(Integer.valueOf(arg));
							argTypes.add(int.class);
						}
						catch (NumberFormatException e)
						{
							// If it's too large for an int, try long
							args.add(Long.valueOf(arg));
							argTypes.add(long.class);
						}
					}
					else if (arg.matches("-?\\d+\\.\\d+"))
					{
						args.add(Double.valueOf(arg));
						argTypes.add(double.class);
					}
					else
					{
						// Remove quotes if present
						if (arg.startsWith("\"") && arg.endsWith("\""))
						{
							arg = arg.substring(1, arg.length() - 1);
						}
						args.add(arg);
						argTypes.add(String.class);
					}
				}
			}

			// Try to find the method in ConfigServlet
			Class< ? > configServletClass = ConfigServlet.class;
			Method method = null;

			try
			{
				// First try to find the exact method with matching parameter types
				method = configServletClass.getDeclaredMethod(methodName, argTypes.toArray(new Class< ? >[0]));
			}
			catch (NoSuchMethodException e)
			{
				// If exact match not found, try to find a method with the same name and number of parameters
				for (Method m : configServletClass.getDeclaredMethods())
				{
					if (m.getName().equals(methodName) && m.getParameterCount() == args.size())
					{
						method = m;
						break;
					}
				}
			}

			if (method != null)
			{
				// Make the method accessible if it's private
				method.setAccessible(true);

				// Invoke the method
				Object result = method.invoke(null, args.toArray());

				// Convert the result to a string for documentation
				if (result != null)
				{
					return result.toString();
				}
				else
				{
					return "null";
				}
			}
			else
			{
				// Method not found, just return the original method call for documentation
				return methodCall;
			}
		}
		catch (Exception e)
		{
			// Log the error but don't fail the documentation generation
			Debug.error("Error processing method call " + methodCall, e);

			// For documentation purposes, just return a representation of the method call
			return methodName + "(" + argsString + ")";
		}
	}

	/**
	 * Processes a description string to resolve any static field references within it.
	 *
	 * @param description The description string that might contain static field references
	 * @return The processed description with resolved references
	 */
	private static String processDescription(String description)
	{
		if (description == null)
		{
			return null;
		}

		// Replace escaped newlines with HTML breaks
		description = description.replace("\\n", "<br/>");

		// Find and replace patterns like escape(ContentSecurityPolicyConfig.DEFAULT_FRAME_SRC_DIRECTIVE_VALUE)
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("escape\\(([\\w\\.]+)\\)");
		java.util.regex.Matcher matcher = pattern.matcher(description);
		StringBuffer sb = new StringBuffer();

		while (matcher.find())
		{
			String fieldReference = matcher.group(1);
			String resolvedValue = resolveStaticFieldReference(fieldReference);
			matcher.appendReplacement(sb, "'" + resolvedValue + "'");
		}
		matcher.appendTail(sb);

		return sb.toString();
	}

	/**
	 * Generates documentation for server configuration properties from ConfigServlet.generalProperties.
	 *
	 * @param outputPath The path where the configuration documentation should be saved
	 * @throws IOException If there's an error writing the file
	 * @throws TemplateException If there's an error processing the template
	 */
	public static void generateConfigurationDocumentation(String outputPath) throws IOException, TemplateException
	{
		System.out.println("Generating server configuration properties documentation");

		// Create data structure for the template
		Map<String, Object> root = new HashMap<>();
		List<Map<String, Object>> sections = new ArrayList<>();

		String currentSectionTitle = null;
		List<Map<String, Object>> currentSectionProperties = null;

		// Process generalProperties
		for (String[] property : ConfigServlet.generalProperties)
		{
			if (property[0] == null && property.length > 1)
			{
				// This is a section header
				if (currentSectionTitle != null && currentSectionProperties != null && !currentSectionProperties.isEmpty())
				{
					// Add the previous section
					Map<String, Object> section = new HashMap<>();
					section.put("title", currentSectionTitle);
					section.put("properties", currentSectionProperties);
					sections.add(section);
				}

				// Start a new section
				currentSectionTitle = property[1];
				currentSectionProperties = new ArrayList<>();
			}
			else if (property.length >= 3)
			{
				// This is a property entry
				Map<String, Object> prop = new HashMap<>();

				// Resolve any static field references in the property name
				String propertyName = resolveStaticFieldReference(property[0]);
				prop.put("name", propertyName);

				// Handle the default value which might be null or a method call result
				String defaultValue = null;
				if (property[1] != null)
				{
					// Check if the value is a method call (contains parentheses)
					if (property[1].contains("(") && property[1].contains(")"))
					{
						defaultValue = processMethodCall(property[1]);
					}
					else
					{
						// Check if this is a result from getBooleanOptions that was already processed
						if (property[1].startsWith("=+"))
						{
							// Format is like "=+true|false|" or "=+false|true|"
							String[] parts = property[1].substring(2).split("\\|");
							if (parts.length > 0)
							{
								defaultValue = parts[0]; // First part is the default value
							}
							else
							{
								defaultValue = property[1];
							}
						}
						else
						{
							defaultValue = property[1];
						}
					}
				}
				prop.put("defaultValue", defaultValue);

				// Process the description to resolve any static field references
				String description = processDescription(property[2]);
				prop.put("description", description);

				if (currentSectionProperties != null)
				{
					currentSectionProperties.add(prop);
				}
			}
		}

		// Add the last section
		if (currentSectionTitle != null && currentSectionProperties != null && !currentSectionProperties.isEmpty())
		{
			Map<String, Object> section = new HashMap<>();
			section.put("title", currentSectionTitle);
			section.put("properties", currentSectionProperties);
			sections.add(section);
		}

		root.put("sections", sections);

		// Apply template
		Template template = cfg.getTemplate("config_properties_template.md");

		// Ensure directory exists - prepend "ng_generated/" to the output path
		// String fullOutputPath = "ng_generated/" + outputPath;
		File dir = new File(outputPath);
		if (!dir.exists())
		{
			dir.mkdirs();
		}

		// Write output file
		try (FileWriter writer = new FileWriter(new File(dir, "README.md")))
		{
			template.process(root, writer);
			System.out.println("Server configuration properties documentation generated at: " + new File(dir, "README.md").getAbsolutePath());
		}
	}

	public static void generateCoreAndPluginDocs(String jsLibURL, String servoyDocURL, String designDocURL, String pluginDir, boolean generateForAI,
		IDocFromXMLGenerator docGenerator)
		throws MalformedURLException, ClassNotFoundException, IOException, URISyntaxException, ZipException, InstantiationException, IllegalAccessException
	{
		// Ensure we start with a fresh references.json
		File refFile = new File("references.json");
		if (refFile.exists())
		{
			if (refFile.delete())
			{
				System.out.println("Deleted old references.json: " + refFile.getAbsolutePath());
			}
		}

		URL[] urls;
		URL jsLibURLObject;
		URL servoyDocURLObject;
		URL designDocURLObject;

		switch (platform)
		{
			case Windows :
				System.out.println("Running on Windows");
				urls = findJarURLsFromServoyInstall(new File(pluginDir).getAbsolutePath());

				jsLibURLObject = new URL(jsLibURL);
				servoyDocURLObject = new URL(servoyDocURL);
				designDocURLObject = new URL(designDocURL);
				break;

			case MacOS :
			case Linux :
			default :
				System.out.println("Running on Mac/Linux");
				urls = findJarURLsFromServoyInstall(new File(pluginDir).toURI().normalize().getPath()); // Ensure absolute path is used; this is not working on windows

				jsLibURLObject = new File(jsLibURL).toURI().toURL();
				servoyDocURLObject = new File(servoyDocURL).toURI().toURL();
				designDocURLObject = new File(designDocURL).toURI().toURL();
				break;
		}

		targetInstallClassLoader = new URLClassLoader("Target Servoy installation classloader",
			urls,
			MarkdownGenerator.class.getClassLoader());

		boolean ngOnly = false;

		loadSummary();

		// Scan for server plugins first to ensure plugin properties are available for documentation generation
		if (GENERATE_SERVER_DOCS)
		{
			findServerPlugins(pluginDir);
		}

		do
		{
			ngOnly = !ngOnly;
			final boolean ng = ngOnly;

			System.out.println("Generating core and java plugin content (ngOnly = " + ngOnly + ")");

			DocumentationManager manager;

			if (!generateForAI)
			{
				// TODO when the object model / persists should also be generated for AI, the if can be removed and this code will always execute
				System.out.println("  - " + jsLibURLObject);
				manager = DocumentationManager.fromXML(jsLibURLObject, targetInstallClassLoader);
				docGenerator.processDocObjectToPathAndOtherMaps(manager, "/reference/servoycore/dev-api", null);

				docGenerator.generateDocsFromXML(manager, "/reference/servoycore/dev-api", ngOnly);
			}

			System.out.println("  - " + servoyDocURLObject);
			manager = DocumentationManager.fromXML(servoyDocURLObject, targetInstallClassLoader);
			docGenerator.processDocObjectToPathAndOtherMaps(manager, "/reference/servoycore/dev-api", null);
			docGenerator.generateDocsFromXML(manager, "/reference/servoycore/dev-api", ngOnly);

			if (!generateForAI)
			{
				System.out.println("  - " + designDocURLObject);
				manager = DocumentationManager.fromXML(designDocURLObject, targetInstallClassLoader);
				docGenerator.processDocObjectToPathAndOtherMaps(manager, "/reference/servoycore/object-model/solution", null);
				docGenerator.generateDocsFromXML(manager, "/reference/servoycore/object-model/solution", ngOnly);
			}

			System.out.println("  - plugins (from " + pluginDir + "):");
			File file2 = new File(new File(pluginDir).toURI().normalize());


			if (file2.isDirectory())
			{
				// this is an directory with jars
				File[] jars = file2.listFiles((child) -> child.getName().toLowerCase().endsWith(".jar"));
				Set<String> foundJars = new HashSet<>();

				for (File jar : jars)
				{
					foundJars.add(jar.getName());
					if (!excludedPluginJarNames.contains(jar.getName()))
					{
						try (ZipFile zf = new ZipFile(jar))
						{
							zf.stream().filter(entry -> entry.getName().toLowerCase().endsWith("servoy-extension.xml"))
								.map(entry -> {
									try
									{
										return zf.getInputStream(entry);
									}
									catch (IOException e)
									{
										Debug.error("Error reading entry " + entry.getName(), e);
									}
									return null;
								})
								.filter(Objects::nonNull)
								.map(is -> {
									// As some types can be accessed between plugins, we need to separate this processDocObjectToPathMaps(...) out of the main
									// generateDocsFromXML(...) so that we could identify all types from all plugins (by calling only this method for each plugin)
									// before generating the actual docs for plugins (via generateDocsFromXML(...)).
									//
									// For example http plugin's putrequest used the JS file from the file plugin.

									DocumentationManager docManager = DocumentationManager.fromXML(is, targetInstallClassLoader);
									IObjectDocumentation pluginProvider = null;
									for (IObjectDocumentation docObj : docManager.getObjects().values())
										if (docObj.getScriptingName() != null &&
											docObj.getScriptingName().startsWith("plugins."))
										{
											pluginProvider = docObj;
											break;
										}

									String pluginPath;
									String pluginProviderPublicNameOrJarName;
									if (pluginProvider != null)
									{
										// for example plugins.http
										pluginPath = "/reference/servoyextensions/server-" + pluginProvider.getScriptingName().replace('.', '/');
										pluginProviderPublicNameOrJarName = pluginProvider.getPublicName();
									}
									else
									{
										pluginProviderPublicNameOrJarName = jar.getName().substring(0, jar.getName().length() - 4);
										pluginPath = "/reference/servoyextensions/server-plugins/" + pluginProviderPublicNameOrJarName;
									}

									try
									{
										docGenerator.processDocObjectToPathAndOtherMaps(docManager, pluginPath, pluginProviderPublicNameOrJarName);
										return new PluginDocumentationPreparated(docManager, pluginPath);
									}
									catch (ClassNotFoundException | InstantiationException | IllegalAccessException e)
									{
										Debug.error("Error reading plugin doc for " + pluginPath, e);
										return null;
									}
								})
								.filter(Objects::nonNull)
								.forEach(pluginDocumentationPreparated -> {
									try
									{
										System.out.println("    * " + jar.getName());

										docGenerator.generateDocsFromXML(pluginDocumentationPreparated.docManager, pluginDocumentationPreparated.pluginPath,
											ng);
									}
									catch (ClassNotFoundException | IOException e)
									{
										Debug.error("Error generating docs from xml in " + jar.getName(), e);
									}
								});
						}
					}
					else
					{
						System.out.println("JAR EXCLUDED: " + jar.getAbsolutePath());
					}
				}
//				for (String nonDefaultPluginThatShouldHaveBeenFound : nonDefaultPluginJarNamesThatWeDoGenerateDocsFor)
//					if (!foundJars.contains(nonDefaultPluginThatShouldHaveBeenFound)) throw new RuntimeException(
//						"Cannot find (explicitly required) plugin '" + nonDefaultPluginThatShouldHaveBeenFound + "' in dir: " + pluginDir +
//							"\nYou have to manually copy a release of that plugin into the plugins dir...");
			}

			docGenerator.writeAggregatedOutput(ngOnly);
		}
		while (ngOnly);

		// Generate server configuration properties documentation
		if (GENERATE_PGADMIN_DOCS)
		{
			try
			{
				generateConfigurationDocumentation("ng_generated/reference/servoycore/server-api/configuration");
			}
			catch (Exception e)
			{
				Debug.error("Error generating admin page documentation ", e);
			}
		}

		printSummary();
		// Write out all collected reference links to JSON
		ReferencesTracker.getInstance().writeResults("references.json");
	}

	/**
	 * Scans for server plugins using ServiceLoader and finds their source file locations.
	 * This follows the approach used in WarServerPluginManager for plugin discovery.
	 *
	 * @param pluginsDir The directory containing plugin JAR files
	 */
	public static void findServerPlugins(String pluginsDir)
	{
		System.out.println("Scanning for server plugins in: " + pluginsDir);
		System.out.println("Git location: " + gitLocation);

		try
		{
			// Create a list to collect JAR URLs
			List<URL> jarUrls = new ArrayList<>();

			// Find all JAR files in the plugins directory
			File pluginsSourceDir = new File(pluginsDir);
			if (!pluginsSourceDir.exists() || !pluginsSourceDir.isDirectory())
			{
				System.err.println("Plugin source directory does not exist or is not a directory: " + pluginsDir);
				return;
			}

			// Collect all JAR files recursively
			java.nio.file.Files.walk(pluginsSourceDir.toPath())
				.filter(path -> path.toString().toLowerCase().endsWith(".jar"))
				.forEach(path -> {
					try
					{
						jarUrls.add(path.toUri().toURL());
					}
					catch (MalformedURLException e)
					{
						Debug.error("Error creating URL for JAR: " + path, e);
					}
				});

			// Create a ClassLoader with all the JAR files
			URL[] urls = jarUrls.toArray(new URL[0]);
			URLClassLoader classLoader = new URLClassLoader(urls, MarkdownGenerator.class.getClassLoader());

			// Use ServiceLoader to find all IPlugin implementations
			ServiceLoader<IPlugin> pluginsLoader = ServiceLoader.load(IPlugin.class, classLoader);

			int pluginCount = 0;

			// Iterate through the plugins
			for (IPlugin plugin : pluginsLoader)
			{
				try
				{
					if (plugin instanceof IServerPlugin)
					{
						IServerPlugin serverPlugin = (IServerPlugin)plugin;
						String pluginClassName = plugin.getClass().getName();

						// Extract just the simple class name (last part after the last dot)
						String simpleName = pluginClassName.substring(pluginClassName.lastIndexOf('.') + 1);

						// Find the source file location
						String sourceLocation = findSourceFileLocation(gitLocation, pluginClassName);

						// Print plugin name and source location
						System.out.println("\n----------------------------------------");
						System.out.println(simpleName + " -> " + (sourceLocation != null ? sourceLocation : "Source file not found"));

						// Get the plugin's properties
						Map<String, String> properties = serverPlugin.getRequiredPropertyNames();
						if (properties != null && !properties.isEmpty())
						{
							// Find the documentation provider class for this plugin
							String docProviderClassName = PLUGIN_TO_DOCUMENTATION_PROVIDER.get(pluginClassName);
							String targetClassName = docProviderClassName != null ? docProviderClassName : pluginClassName;

							// Store properties in the map for later use during documentation generation
							PLUGIN_PROPERTIES.put(targetClassName, new HashMap<>(properties));
							System.out.println("Stored " + properties.size() + " properties for: " + targetClassName);
						}

						pluginCount++;
					}
				}
				catch (Exception e)
				{
					Debug.error("Error processing plugin " + plugin, e);
				}
			}

			System.out.println("\n----------------------------------------");
			System.out.println("Found " + pluginCount + " server plugins");

			// Close the classloader when done
			classLoader.close();
		}
		catch (Exception e)
		{
			Debug.error("Error scanning for plugins", e);
		}
		finally
		{
			System.out.println("Server plugin scanning complete");
		}
	}

	/**
	 * Updates the class-level documentation of a plugin provider class to include its properties.
	 *
	 * @param sourceFilePath The path to the source file
	 * @param properties The properties map with keys and descriptions
	 */
	/**
	 * Injects plugin configuration properties into the documentation model.
	 * This method checks if the current class has associated properties in the PLUGIN_PROPERTIES map
	 * and adds them to the documentation model.
	 *
	 * @param scriptingName The scripting name of the class being documented
	 */
	private void injectPluginProperties(String scriptingName)
	{
		if (!GENERATE_SERVER_DOCS || scriptingName == null)
		{
			return;
		}

		// Try to find properties for this class by its scripting name
		Map<String, String> properties = null;
		String matchedClassName = null;

		// First try with the scripting name directly
		for (Map.Entry<String, Map<String, String>> entry : PLUGIN_PROPERTIES.entrySet())
		{
			String className = entry.getKey();
			if (className.endsWith(scriptingName))
			{
				properties = entry.getValue();
				matchedClassName = className;
				break;
			}
		}

		// If no properties found and this is a provider class, try with the class name
		if (properties == null)
		{
			for (String className : PLUGIN_PROPERTIES.keySet())
			{
				if (className.contains(scriptingName))
				{
					properties = PLUGIN_PROPERTIES.get(className);
					matchedClassName = className;
					break;
				}
			}
		}

		// If properties were found, add them to the documentation model
		if (properties != null && !properties.isEmpty())
		{
			// Create a list of property entries for the template
			List<Map<String, String>> propertyList = new ArrayList<>();
			for (Map.Entry<String, String> entry : properties.entrySet())
			{
				Map<String, String> propertyMap = new HashMap<>();
				propertyMap.put("name", entry.getKey());
				propertyMap.put("description", entry.getValue());
				propertyList.add(propertyMap);
			}

			// Add the properties to the template model
			root.put("configProperties", propertyList);
		}
	}

	/**
	 * This method is kept for backward compatibility but no longer modifies source files.
	 * Instead, properties are stored in memory and injected during documentation generation.
	 *
	 * @param sourceFilePath The path to the source file (not used)
	 * @param properties The properties map with keys and descriptions (not used)
	 */
	private static void updatePluginDocumentationWithProperties(String sourceFilePath, Map<String, String> properties)
	{
		// This method is kept for backward compatibility but no longer does anything
		// Properties are now stored in the PLUGIN_PROPERTIES map and injected during documentation generation
		System.out.println("Source files are no longer modified. Properties will be injected during documentation generation.");
	}

	/**
	 * Finds the source file location for a plugin class.
	 *
	 * @param gitLocation The git repository root location
	 * @param pluginClassName The fully qualified class name of the plugin
	 * @return The source file location or null if not found
	 */
	private static String findSourceFileLocation(String gitLocation, String pluginClassName)
	{
		// Convert the class name to a path
		String classPath = pluginClassName.replace('.', File.separatorChar) + ".java";

		// Check in servoy-extensions (with intermediary com.servoy.extensions directory)
		String extensionsPath = gitLocation + File.separator + servoySourceRoot + "servoy-extensions" + File.separator + "com.servoy.extensions" +
			File.separator + "src" +
			File.separator + classPath;
		File extensionsFile = new File(extensionsPath);
		if (extensionsFile.exists())
		{
			return extensionsPath;
		}

		// Check in servoy-plugins/j2db_plugins
		String pluginsPath = gitLocation + File.separator + "servoy-plugins" + File.separator + "j2db_plugins" + File.separator + "src" + File.separator +
			classPath;
		File pluginsFile = new File(pluginsPath);
		if (pluginsFile.exists())
		{
			return pluginsPath;
		}

		return null;
	}

	private static void loadSummary()
	{
		referenceTypes.clear();
		try (BufferedReader br = new BufferedReader(new FileReader(summaryMdFilePath, Charset.forName("UTF-8"))))
		{
			String line;
			while ((line = br.readLine()) != null)
			{
				// Extract paths ending with .md from markdown links
				int start = line.indexOf("[");
				int middle = line.indexOf("](", start);
				int end = line.indexOf(')', middle);
				if (start != -1 && middle != -1 && end != -1)
				{
					String key = line.substring(start + 1, middle).trim();
					String value = line.substring(middle + 2, end).trim().replace("\\", "");
					summaryPaths.add(value);

					//types from plugins / extensions, javascript classes may overlap with other references ( [key](reference) ).
					// For example:
					// [controller](reference/servoycore/dev-api/forms/runtimeform/controller.md)
					// [Controller](reference/servoy-developer/solution-explorer/all-solutions/active-solution/forms/form/controller.md)
					//
					// The second line will overwrite the first one - leading to incorrect report for this code
					if ((value.startsWith("reference/servoycore") || value.startsWith("reference/servoyextensions")) && value.endsWith(".md"))
					{
						// Normalize the path to use '/' as separator
						referenceTypes.put(key.toLowerCase(), value);
					}
				}
			}
		}
		catch (IOException e)
		{
			Debug.error("Failed to load summary file: " + summaryMdFilePath, e);
		}
		return;
	}

	private static void printSummary()
	{
		System.out.println(undocumentedReturnTypeFunctions.size() + " functions must be checked in " + uniqueClasses.size() + " classes.");
		if (undocumentedTypes.size() > 0)
		{
			System.out.println("\033[38;5;27mThe following types are not documented: ");
			undocumentedTypes.forEach(type -> {
				System.out.print(type + ", ");
			});
			System.out.println("\033[0m");
		}
		if (uniqueClasses.size() > 0)
		{
			System.out.println("\033[38;5;39mThe following classes contain function having undocumented returns: ");
			uniqueClasses.forEach(type -> {
				System.out.print(type + ", ");
			});
			System.out.println("\033[0m");
		}

		if (missingMdFiles.size() > 0)
		{
			System.out.println("\033[38;5;39m\n\nThe following files are missing from summary.md (gitbook): ");
			missingMdFiles.forEach(filePath -> {
				System.out.println(filePath + ", ");
			});
			System.out.println("\033[0m");
		}
	}

	private static URL[] findJarURLsFromServoyInstall(String pluginDir) throws IOException
	{
		List<URL> jarURLsFromInstall = new ArrayList<>();
		// install-dir
		//     application_server
		//         plugins
		//             *.jar (nested)
		//     developer
		//         plugins
		//             *.jar (nested)
		addAllNestedJarFilesOfDir(pluginDir, jarURLsFromInstall);
		switch (platform)
		{
			case Windows :
			case Linux :
				addAllNestedJarFilesOfDir(Path.of(pluginDir, "..", "..", "developer", "plugins").normalize().toString(), jarURLsFromInstall);
				break;

			case MacOS :
				addAllNestedJarFilesOfDir(Path.of(pluginDir, "..", "..", "eclipse", "plugins").normalize().toString(), jarURLsFromInstall);
				break;

		}
		return jarURLsFromInstall.toArray(new URL[jarURLsFromInstall.size()]);
	}

	private static void addAllNestedJarFilesOfDir(String pluginDir, List<URL> jarURLsFromInstall) throws IOException
	{
		try (Stream<Path> fileStream = Files.find(Path.of(pluginDir), 500 /* meant to be a really big number */,
			(filePath, basicFileAttributes) -> filePath.toString().endsWith(".jar"), FileVisitOption.FOLLOW_LINKS))
		{
			fileStream.forEach((filePath) -> {
				try
				{
					jarURLsFromInstall.add(filePath.normalize().toUri().toURL());
				}
				catch (MalformedURLException e)
				{
					Debug.error("Failed to normalize filePath " + filePath, e);
				}
			});
		}
	}

	private void table(String name, List<IFunctionDocumentation> functions, Class< ? > cls, boolean ngOnly, boolean checkReturnAnnotation)
	{
		if (functions != null && functions.size() > 0)
		{
			List<FunctionTemplateModel> models = new ArrayList<>();
			ClientSupport fdCs;

			for (IFunctionDocumentation fd : functions)
			{
				if (fd.isDeprecated()) continue;

				fdCs = fd.getClientSupport();
				if (fdCs == null) fdCs = ClientSupport.Default;

				if (ngOnly && !fdCs.hasSupport(ClientSupport.ng)) continue;

				String functionName = fd.getMainName();
				Class< ? > returnType = fd.getReturnedType();
				String publicName = MarkdownGenerator.getPublicName(returnType);

				// Check if the function has a return type but is missing a @return annotation
				if (!SKIP_MISSING_RETURN_FOR_CLASS.contains(cls.getName()) &&
					checkReturnAnnotation &&
					returnType != null &&
					returnType != void.class &&
					!hasReturnAnnotation(fd) &&
					!hasStdDocLinks(fd) &&
					!fd.isDeprecated() &&
					undocumentedReturnTypeFunctions.add(functionName))
				{
					System.err.println("\033[32m" + missingReturnAnnotationCount + " Function " + functionName + " in class " + cls.getName() +
						" returns " + returnType.getName() + " but is missing @return annotation in JSDoc. \033[0m");
					uniqueClasses.add(cls.getName());
				}

				if ("Object".equals(publicName) &&
					!IGNORED_UNDOCUMENTED_TYPES.contains(returnType.getName()))
				{
					System.err.println("Function " + functionName + " in class " + cls.getName() +
						" has undocumented return type: " + returnType.getName());
					undocumentedTypes.add(returnType.getName());
				}

				FunctionTemplateModel ftm = new FunctionTemplateModel(fd, MarkdownGenerator::getPublicName, cls, ngOnly, htmlToMarkdownConverter);
				models.add(ftm);
			}
			root.put(name, models);
		}
	}

	private boolean hasStdDocLinks(IFunctionDocumentation fd)
	{
		String jsDoc = fd.getDescription(fd.getClientSupport());
		SortedMap<String, String> links = fd.getLinks();

		if (jsDoc != null && links != null)
		{
			//System.out.println("#######%%%%%%%%%%%%%%links found: " + fd.getMainName());
			for (Map.Entry<String, String> entry : links.entrySet())
			{
				String link = entry.getKey();
				for (String prefix : STD_DOC_LINKS)
				{
					if (link.startsWith(prefix))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean hasReturnAnnotation(IFunctionDocumentation fd)
	{

		return fd.getReturnDescription() != null;
	}

	private void generateClientSupport(IObjectDocumentation value)
	{
		ClientSupport clientSupport = value.getClientSupport();
		List<String> support = getSupportedClientsList(clientSupport);
		root.put("supportedClients", support);
	}

	private void classList(String section, List<String> publicNamesOfTypes)
	{
		if (publicNamesOfTypes != null) root.put(section, publicNamesOfTypes);
	}

	private String generate()
	{
		StringWriter out = new StringWriter();
		try
		{
			template.process(root, out);
		}
		catch (TemplateException | IOException e)
		{
			Debug.error("Failed process template", e);
		}
		return out.toString();
	}

	public String getExtendsPath(String childClass, String parentClass)
	{
		if (childClass != null && parentClass != null)
		{
			String parentPath = referenceTypes.get(parentClass.toLowerCase());
			if (parentPath == null)
			{
				System.err.println("\033[35mPath reference not found for the class: " + parentClass + " (referenced from: " + childClass + ")\033[0m");
			}
			String childPath = referenceTypes.get(childClass.toLowerCase());
			if (childPath == null)
			{
				System.err.println("\033[35mPath reference not found for the class: " + childClass + ". Please update the summary.md file...!\033[0m");
				return "";
			}
			try
			{
				// Convert to Path objects
				Path child = Paths.get(childPath).normalize();
				Path parent = Paths.get(parentPath).normalize();

				// Get relative path from child to parent
				Path relativePath = child.getParent().relativize(parent);
				return relativePath.toString();
			}
			catch (Exception e)
			{
				Debug.error("Error calculating relative path", e);
				return "";
			}
		}
		return "";

	}

	public String getReturnTypePath(String publicName)
	{
		Path p1 = Paths.get(generatePathInLauncherOutputDir(publicName));
		Path relativize = path.relativize(p1);
		String relativePath = relativize.toString().replace('\\', '/').replace(" ", "%20");
		return ((relativePath.isBlank() ? "." : relativePath) + "/" + (storeAsReadMe.contains(publicName) ? "" : publicName + ".md")).toLowerCase();
	}

	public Path getPath()
	{
		return path;
	}

	/**
	 * Generates the path where this publicName's .md file should be generated relative to the launcher's output dir (so not to the current generateDocsFromXML call's path, but it includes that).
	 */
	private String generatePathInLauncherOutputDir(String publicName)
	{
		String specialTypePath = specialTypePaths.get(publicName);
		if (specialTypePath != null) return specialTypePath;

		String publicNameOfItsPluginProvider = publicNameToPluginProviderClassPublicName.get(publicName); // so if this is a type from a plugin, get the public name of the plugin's script provider

		// NOTE: nestedPartOfPath does not include the .md filename, just the dir path to it
		// if it's supposed to be a folder add folder name to path;
		// but if it is the main provider of a plugin with return types, the plugin folder is already it's folder, so don't add it twice like ...server-plugins/file/file/.. for example
		String nestedPartOfPath = (storeAsReadMe.contains(publicName) &&
			!publicName.equals(publicNameOfItsPluginProvider) ? "/" + publicName : "");

		String publicNameToSearchFor = publicName;

		// check nesting
		String parent = returnTypesToParentName.get(publicName);
		while (parent != null)
		{
			publicNameToSearchFor = parent; // the location of the .md file will be in the same dir as the parent's README.md file

			// if a parent of it is the main provider of a plugin with or without return types, that is located in the main plugin dir directly without additional nesting in case if has return types, for example jsfile has "file" as parent but it is inside the file plugin directly not in ..server-plugins/file/file/..
			nestedPartOfPath = (parent.equals(publicNameOfItsPluginProvider) ? "" : parent + "/") + nestedPartOfPath;
			parent = returnTypesToParentName.get(parent);
		}

		// it is a type returned by a parent node (for example JSWindow - who's parent is application)
		// find the parent's path and concatenate?
		String rootPathToSearchFor = publicToRootPath.get(publicNameToSearchFor);
		return ((rootPathToSearchFor != null ? rootPathToSearchFor : rootPath) + "/" + nestedPartOfPath).replace(' ', '-');
	}

	public static String getPublicName(Class< ? > type)
	{
		if (type != null)
		{
			if (type == NativePromise.class)
			{
				return "Promise";
			}
			else if (type == boolean.class || type == Boolean.class)
			{
				return "Boolean";
			}
			else if (type == void.class)
			{
				return "void";
			}
			else if (type.isPrimitive() || Number.class.isAssignableFrom(type) || com.servoy.j2db.documentation.scripting.docs.Number.class == type)
			{
				return "Number";
			}
			else if (type == String.class || com.servoy.j2db.documentation.scripting.docs.String.class == type)
			{
				return "String";
			}
			else if (Date.class.isAssignableFrom(type))
			{
				return "Date";
			}
			else if (type == Object.class || type == Scriptable.class || type == IdScriptableObject.class)
			{
				return "Object";
			}
			else if (type.isArray() || type == NativeArray.class || List.class.isAssignableFrom(type))
			{
				return "Array";
			}
			else if (IJSController.class.isAssignableFrom(type))
			{
				return "controller";
			}
			else if (IJSRecord.class.isAssignableFrom(type) || IRecord.class.isAssignableFrom(type))

			{
				return "JSRecord";
			}
			else if (type == FormScope.class)
			{
				return "RuntimeForm";
			}
			else if (IJSFoundSet.class.isAssignableFrom(type) || IFoundSet.class.isAssignableFrom(type))
			{
				return "JSFoundSet";
			}
			else if (IJSDataSet.class.isAssignableFrom(type) || IDataSet.class.isAssignableFrom(type))
			{
				return "JSDataSet";
			}
			else if (ServoyException.class.isAssignableFrom(type))
			{
				return "ServoyException";
			}
			else if (DataException.class.isAssignableFrom(type))
			{
				return "DataException";
			}
			else if (Exception.class.isAssignableFrom(type))
			{
				return "Exception";
			}
			else if (IBaseSMField.class.isAssignableFrom(type))
			{
				return "JSField";
			}
			else if (IBaseSMComponent.class.isAssignableFrom(type))
			{
				return "JSComponent";
			}
			else if (IBaseSMMethod.class.isAssignableFrom(type))
			{
				return "JSMethod";
			}
			else if (ISMPart.class.isAssignableFrom(type))
			{
				return "JSPart";
			}
			else if (IBaseSMForm.class.isAssignableFrom(type))
			{
				return "JSForm";
			}
			else if (Function.class.isAssignableFrom(type))
			{
				return "Function";
			}
			else if (ICSSPosition.class.isAssignableFrom(type) || CSSPosition.class == type)
			{
				return "CSSPosition";
			}
			else if (Color.class.isAssignableFrom(type) || Point.class.isAssignableFrom(type) || Dimension.class.isAssignableFrom(type) ||
				Insets.class.isAssignableFrom(type))
			{// special tabs that in scripting just map on a String
				return "String";
			}
			else if (type == Map.class || type == ServoyJSONObject.class || type == JSONObject.class || type == JSMap.class || type == NativeObject.class)
			{
				return "Object";
			}
			else if (type.isEnum())
			{
				return "enum";
			}
			String name = qualifiedToName.get(type.getCanonicalName());
			if (name == null)
			{
				JSRealClass annotation = type.getAnnotation(JSRealClass.class);
				if (annotation != null)
				{
					Class< ? > value = annotation.value();
					name = qualifiedToName.get(value.getCanonicalName());
				}
				if (name == null)
				{
					name = "Object";
				}
			}
			return name;
		}
		return "void";
	}

	private static class MarkdownDocFromXMLGenerator implements IDocFromXMLGenerator
	{

		private StringBuilder aggregatedOutput = null;

		private Map<String, List<String>> computedReturnTypes = null;

		private MarkdownDocFromXMLGenerator(boolean generateForAI)
		{
			if (generateForAI) aggregatedOutput = new StringBuilder("Servoy scripting is based on javascript. Here is the API:\n\n");
		}

		/**
		 * As some types can be accessed between plugins, we need to separate this processDocObjectToPathMaps(...) out of the main
		 * generateDocsFromXML(...) so that we could identify all types from all plugins (by calling only this method for each plugin)
		 * before generating the actual docs for plugins (via generateDocsFromXML(...)).<br/><br/>
		 *
		 * For example http plugin's putrequest used the JS file from the file plugin.
		 */
		public void processDocObjectToPathAndOtherMaps(DocumentationManager manager, String path, String pluginProviderPublicName)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException
		{
			SortedMap<String, IObjectDocumentation> objects = manager.getObjects();

			// first loop sets the qualifiedToName for all and fixes returnTypes for all
			for (IObjectDocumentation doc : objects.values())
			{
				qualifiedToName.put(doc.getQualifiedName(), doc.getPublicName());
				if (pluginProviderPublicName != null) publicNameToPluginProviderClassPublicName.put(doc.getPublicName(), pluginProviderPublicName);
				if (path != null) publicToRootPath.put(doc.getPublicName(), path);

				Class< ? > cls = targetInstallClassLoader.loadClass(doc.getQualifiedName());
				IReturnedTypesProvider returnTypes = ScriptObjectRegistry.getScriptObjectForClass(cls);

				if (returnTypes == null)
				{
					// in case the class does not statically register return type to ScriptObjectRegistry, do register them
					// now if it has returnTypes; the xml doc generated from annotations seems to miss this info...

					// add returnTypes if needed; this code is the same as the one in SolutionExplorerTreeContentProvider.addBeanAndBeanChildNodes(Bean)
					IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(cls);
					if ((scriptObject == null || scriptObject.getAllReturnedTypes() == null) && IReturnedTypesProvider.class.isAssignableFrom(cls))
					{
						final Class< ? >[] allReturnedTypes = ((IReturnedTypesProvider)cls.newInstance()).getAllReturnedTypes();
						ScriptObjectRegistry.registerReturnedTypesProviderForClass(cls, new IReturnedTypesProvider()
						{

							public Class< ? >[] getAllReturnedTypes()
							{
								return allReturnedTypes;
							}
						});
						returnTypes = ScriptObjectRegistry.getScriptObjectForClass(cls);
					}
				}
			}

			computedReturnTypes = new HashMap<>();

			// second loop uses previously set qualifiedToName and return types
			for (IObjectDocumentation doc : objects.values())
			{
				Class< ? > cls = targetInstallClassLoader.loadClass(doc.getQualifiedName());
				IReturnedTypesProvider returnTypes = ScriptObjectRegistry.getScriptObjectForClass(cls);
				if (returnTypes != null && returnTypes.getAllReturnedTypes() != null && returnTypes.getAllReturnedTypes().length > 0)
				{
					for (Class< ? > retCls : returnTypes.getAllReturnedTypes())
					{
						String qname = qualifiedToName.get(retCls.getCanonicalName());
						if (qname != null)
						{
							returnTypesToParentName.put(qname, doc.getPublicName());
						}
						else
						{
							System.err.println(" qname not found for " + retCls);
						}
					}
				}

				ArrayList<Class< ? >> filteredReturnTypes = null;

				if (returnTypes != null && returnTypes.getAllReturnedTypes() != null)
				{
					Class< ? >[] allReturnedTypes = returnTypes.getAllReturnedTypes();
					filteredReturnTypes = new ArrayList<>();
					for (Class< ? > clsReturn : allReturnedTypes)
					{
						IObjectDocumentation retDoc = objects.get(clsReturn.getCanonicalName());
						if (retDoc == null || !retDoc.isDeprecated())
						{
							filteredReturnTypes.add(clsReturn);
						}
					}
				}

				List<String> usableReturnTypes = getUsablePublicNamesFromClassList(filteredReturnTypes);
				if (usableReturnTypes != null)
				{
					computedReturnTypes.put(doc.getPublicName(), usableReturnTypes);
					if (!doNotStoreAsReadMe.contains(doc.getPublicName()))
					{
						storeAsReadMe.add(doc.getPublicName());
					}
				}
			}
		}

		public void generateDocsFromXML(DocumentationManager manager, String path, boolean ngOnly)
			throws ClassNotFoundException, IOException
		{
			SortedMap<String, IObjectDocumentation> objects = manager.getObjects();

			File userDir = new File(System.getProperty("user.dir"));

			if (TRACK_REFERENCES)
			{
				ReferencesTracker.getInstance().trackReferences(manager, summaryPaths, referenceTypes, ngOnly, computedReturnTypes);
			}

			for (Entry<String, IObjectDocumentation> entry : objects.entrySet())
			{
				IObjectDocumentation value = entry.getValue();

				String description = value.getDescription(value.getClientSupport());
				if (value.isDeprecated() || value.getPublicName().equals("PrinterJob") ||
					(value.getFunctions().size() == 0 && (description == null || description.trim().length() == 0) && computedReturnTypes.size() == 0))
					continue;
				if (ngOnly && !(value.getClientSupport() == null ? ClientSupport.Default : value.getClientSupport()).hasSupport(ClientSupport.ng)) continue;

				MarkdownGenerator cg = new MarkdownGenerator(value.getPublicName(), value.getScriptingName(), description,
					path, computedReturnTypes.get(value.getPublicName()));

				if (!ngOnly) cg.generateClientSupport(value);

				if (value.getExtendsClass() != null)
				{
					cg.classList(EXTENDS_CLASS_SECTION_IN_TEMPLATE,
						getUsablePublicNamesFromClassList(Arrays.asList(targetInstallClassLoader.loadClass(value.getExtendsClass()))));
				}

				Class< ? > cls = targetInstallClassLoader.loadClass(value.getQualifiedName());

				SortedSet<IFunctionDocumentation> functions = value.getFunctions();
				List<IFunctionDocumentation> constants = getConstants(functions);
				List<IFunctionDocumentation> properties = getProperties(functions);
				List<IFunctionDocumentation> commands = getCommands(functions);
				List<IFunctionDocumentation> events = getEvents(functions);
				List<IFunctionDocumentation> methods = getMethods(functions);
				cg.table("constants", constants, cls, ngOnly, false);
				if (properties != null) properties = properties.stream().filter(node -> node.getReturnedType() != void.class).collect(Collectors.toList());
				cg.table("properties", properties, cls, ngOnly, false);
				cg.table("commands", commands, cls, ngOnly, false);
				cg.table("events", events, cls, ngOnly, false);
				cg.table("methods", methods, cls, ngOnly, true);

				String output = cg.generate();
				String parent = cg.getPath().toString();
				String publicName = value.getPublicName();
				if (storeAsReadMe.contains(value.getPublicName()))
				{
					publicName = "README";
				}
				else
				{
					publicName = publicName.toLowerCase();
				}
				File file = new File(userDir, (ngOnly ? "ng_generated/" : "generated/") + (parent.toLowerCase() + '/' + publicName + ".md").replace(' ', '-'));

				file.getParentFile().mkdirs();
				try (FileWriter writer = new FileWriter(file, Charset.forName("UTF-8")))
				{
					writer.write(output);
					duplicateTracker.trackFile(file.getName(), file.toString());
					if (aggregatedOutput != null) aggregatedOutput.append(output);
				}

				file.getParentFile().mkdirs();
				try (FileWriter writer = new FileWriter(file, Charset.forName("UTF-8")))
				{
					writer.write(output);
					duplicateTracker.trackFile(file.getName(), file.toString());
					if (aggregatedOutput != null) aggregatedOutput.append(output);

					// Log plugin documentation file generation if it contains config properties
					if (GENERATE_SERVER_DOCS && cg.root.containsKey("configProperties"))
					{
						System.out.println("[PLUGIN DOCS] Generated markdown file with properties: " + file.getAbsolutePath());
					}

					if (ngOnly)
					{
						String relativePath = file.getPath().substring(file.getPath().indexOf("ng_generated") + "ng_generated/".length());
						relativePath = relativePath.replace('\\', '/');

						// Check if the relative path is in the summary but not in generated files
						if (!summaryPaths.contains(relativePath))
						{
							missingMdFiles.add(relativePath);
							// System.err.println("\033[38;5;214mMissing file in summary: " + relativePath + " ::: " + cls.getName() + "\033[0m");
						}
					}
				}
			}
		}

		private List<String> getUsablePublicNamesFromClassList(List<Class< ? >> filteredReturnTypes)
		{
			if (filteredReturnTypes != null && filteredReturnTypes.size() > 0)
			{
				List<String> publicNames = new ArrayList<>(filteredReturnTypes.size());
				for (Class< ? > alltype : filteredReturnTypes)
				{
					String publicName = getPublicName(alltype);
					if (publicName != null && !"Object".equals(publicName))
					{
						publicNames.add(publicName);
					}
					else
					{
						System.err.println("WARN: Public name not found for type: " + alltype.getName() + " in filtered return types.");
					}
				}
				if (publicNames.size() > 0) return publicNames;
			}
			return null;
		}

		@Override
		public void writeAggregatedOutput(boolean ngOnly) throws IOException
		{
			if (aggregatedOutput != null)
			{
				File userDir = new File(System.getProperty("user.dir"));
				File file = new File(userDir, (ngOnly ? "ng_generated/" : "generated/") + "aggregated_output.md");
				file.getParentFile().mkdirs();
				try (FileWriter writer = new FileWriter(file, Charset.forName("UTF-8")))
				{
					writer.write(aggregatedOutput.toString());
					duplicateTracker.trackFile(file.getName(), file.toString());
				}

				aggregatedOutput.setLength("Servoy scripting is based on javascript. Here is the API:\n\n".length());
			}
		}

	}

	private static record PluginDocumentationPreparated(DocumentationManager docManager, String pluginPath)
	{
	}
}
