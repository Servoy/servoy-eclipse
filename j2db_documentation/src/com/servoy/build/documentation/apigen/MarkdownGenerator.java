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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

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
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Stream;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.json.JSONObject;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
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
import com.servoy.j2db.querybuilder.IQueryBuilderColumn;
import com.servoy.j2db.querybuilder.IQueryBuilderCondition;
import com.servoy.j2db.querybuilder.IQueryBuilderLogicalCondition;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.JSMap;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.solutionmodel.ICSSPosition;
import com.servoy.j2db.solutionmodel.ISMPart;
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

	private static final String EXTENDS_CLASS_SECTION_IN_TEMPLATE = "extends";
	private static final String RETURN_TYPES_SECTION_IN_TEMPLATE = "returnTypes";

	private static URLClassLoader targetInstallClassLoader;

	private static Configuration cfg;
	private static Template template;

	private static final DuplicateTracker duplicateTracker = DuplicateTracker.getInstance();

	private static Map<String, String> specialTypePaths = new HashMap<>();
	private static boolean isMacOS = false;
	private static int missingReturnAnnotationCount = 0;
	private static Set<String> undocumentedReturnTypeFunctions = new HashSet<>();
	private static Set<String> uniqueClasses = new HashSet<>();
	private static Set<String> undocumentedTypes = new HashSet<>();

	private static String summaryMdFilePath;

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

		String osName = System.getProperty("os.name");

		if (osName.equalsIgnoreCase("Mac OS X"))
		{
			isMacOS = true;
		}
	}

	private static final HashMap<String, String> qualifiedToName = new HashMap<>();
	private static final HashMap<String, String> publicToRootPath = new HashMap<>();
	private static final HashMap<String, String> returnTypesToParentName = new HashMap<>();
	private static final Map<String, String> publicNameToPluginProviderClassPublicName = new HashMap<>(); // plugins already have the plugin name as dir in their root path; do not generate another dir; it is null for non-plugin docs

	private static final Set<String> storeAsReadMe = new HashSet<>();
	private static final Set<String> doNotStoreAsReadMe = new HashSet<>();

	private static final Set<String> excludedPluginJarNames = Set.of("aibridge.jar");
	private static final List<String> nonDefaultPluginJarNamesThatWeDoGenerateDocsFor = Arrays.asList(/* RAGTEST "servoy_jasperreports.jar" */); // we check that these were found so that we don't forget by mistake to generate the docs for them

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
		summaryMdFilePath = args[4];
		boolean generateForAI = "forAI".equals(args[5]);

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

	public static void generateCoreAndPluginDocs(String jsLibURL, String servoyDocURL, String designDocURL, String pluginDir, boolean generateForAI,
		IDocFromXMLGenerator docGenerator)
		throws MalformedURLException, ClassNotFoundException, IOException, URISyntaxException, ZipException, InstantiationException, IllegalAccessException
	{
		targetInstallClassLoader = new URLClassLoader("Target Servoy installation classloader",
			findJarURLsFromServoyInstall(new File(pluginDir).toURI().normalize().getPath()), // Ensure absolute path is used
			MarkdownGenerator.class.getClassLoader());

		boolean ngOnly = false;

		URL jsLibURLObject = new File(jsLibURL).toURI().toURL();
		URL servoyDocURLObject = new File(servoyDocURL).toURI().toURL();
		URL designDocURLObject = new File(designDocURL).toURI().toURL();


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
										e.printStackTrace();
									}
									return null;
								})
								.filter(is -> is != null)
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
										e.printStackTrace();
										return null;
									}
								})
								.forEach(pluginDocumentationPreparated -> {
									if (pluginDocumentationPreparated != null)
									{
										try
										{
											System.out.println("    * " + jar.getName());

											docGenerator.generateDocsFromXML(pluginDocumentationPreparated.docManager, pluginDocumentationPreparated.pluginPath,
												ng);
										}
										catch (ClassNotFoundException | IOException e)
										{
											e.printStackTrace();
										}
									}
								});
						}
					}
					else
					{
						System.out.println("JAR EXCLUDED: " + jar.getAbsolutePath());
					}
				}
				for (String nonDefaultPluginThatShouldHaveBeenFound : nonDefaultPluginJarNamesThatWeDoGenerateDocsFor)
					if (!foundJars.contains(nonDefaultPluginThatShouldHaveBeenFound)) throw new RuntimeException(
						"Cannot find (explicitly required) plugin '" + nonDefaultPluginThatShouldHaveBeenFound + "' in dir: " + pluginDir +
							"\nYou have to manually copy a release of that plugin into the plugins dir...");
			}

			docGenerator.writeAggregatedOutput(ngOnly);
		}
		while (ngOnly);

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
		String osName = System.getProperty("os.name").toLowerCase();
		if (isMacOS)
		{
			addAllNestedJarFilesOfDir(Path.of(pluginDir, "..", "..", "eclipse", "plugins").normalize().toString(), jarURLsFromInstall);
		}
		else
		{
			addAllNestedJarFilesOfDir(Path.of(pluginDir, "..", "..", "developer", "plugins").normalize().toString(), jarURLsFromInstall);
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
					e.printStackTrace();
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
			e.printStackTrace();
		}
		return out.toString();
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
			if (type == boolean.class || type == Boolean.class)
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
			else if (IQueryBuilderLogicalCondition.class.isAssignableFrom(type))
			{
				return "QBLogicalCondition";
			}
			else if (IQueryBuilderCondition.class.isAssignableFrom(type))
			{
				return "QBCondition";
			}
			else if (IQueryBuilderCondition.class.isAssignableFrom(type))
			{
				return "QBCondition";
			}
			else if (IQueryBuilderColumn.class.isAssignableFrom(type))
			{
				return "QBColumn";
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
//				System.err.println("public name not found for " + type);
				name = "Object";
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

//			var realClassMapping = objects.values().stream().filter(doc -> doc.getRealClass() != null)
//				.collect(toMap(IObjectDocumentation::getRealClass, identity(), (doc1, doc2) -> {
//					System.err.println("Multiple classes (" + doc1.getQualifiedName() +
//						", " + doc2.getQualifiedName() + ") referring to the same realClass " + doc1.getRealClass());
//					return doc1;
//				}));
			var realClassMapping = objects.values().stream().filter(doc -> doc.getRealClass() != null)
				.collect(toMap(IObjectDocumentation::getQualifiedName, IObjectDocumentation::getRealClass));

			File userDir = new File(System.getProperty("user.dir"));
			for (IObjectDocumentation doc : objects.values())
			{
				if (doc.getRealClass() != null)
				{
					// Use documentation of real class
					continue;
				}

				String description = doc.getDescription(doc.getClientSupport());
				if (doc.isDeprecated() || doc.getPublicName().equals("PrinterJob") ||
					(doc.getFunctions().size() == 0 && (description == null || description.trim().length() == 0) && computedReturnTypes.size() == 0))
					continue;
				if (ngOnly && !(doc.getClientSupport() == null ? ClientSupport.Default : doc.getClientSupport()).hasSupport(ClientSupport.ng)) continue;

				MarkdownGenerator cg = new MarkdownGenerator(doc.getPublicName(), doc.getScriptingName(), description,
					path, computedReturnTypes.get(doc.getPublicName()));

				if (!ngOnly) cg.generateClientSupport(doc);

				if (doc.getExtendsClass() != null)
				{
					cg.classList(EXTENDS_CLASS_SECTION_IN_TEMPLATE,
						getUsablePublicNamesFromClassList(Arrays.asList(targetInstallClassLoader.loadClass(doc.getExtendsClass()))));
				}

				Class< ? > cls = targetInstallClassLoader.loadClass(doc.getQualifiedName());

				SortedSet<IFunctionDocumentation> functions = doc.getFunctions();
				List<IFunctionDocumentation> constants = getConstants(functions);
				List<IFunctionDocumentation> properties = getProperties(functions);
				List<IFunctionDocumentation> commands = getCommands(functions);
				List<IFunctionDocumentation> events = getEvents(functions);
				List<IFunctionDocumentation> methods = getMethods(functions);
				cg.table("constants", constants, cls, ngOnly, false);
				if (properties != null) properties = properties.stream().filter(node -> node.getReturnedType() != void.class).collect(toList());
				cg.table("properties", properties, cls, ngOnly, false);
				cg.table("commands", commands, cls, ngOnly, false);
				cg.table("events", events, cls, ngOnly, false);
				cg.table("methods", methods, cls, ngOnly, true);

				String output = cg.generate();
				String parent = cg.getPath().toString();
				String publicName = doc.getPublicName();
				if (storeAsReadMe.contains(doc.getPublicName()))
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

				List<String> summaryPaths = loadSummary();

				file.getParentFile().mkdirs();
				try (FileWriter writer = new FileWriter(file, Charset.forName("UTF-8")))
				{
					writer.write(output);
					duplicateTracker.trackFile(file.getName(), file.toString());
					if (aggregatedOutput != null) aggregatedOutput.append(output);

					if (ngOnly)
					{
						String relativePath = file.getPath().substring(file.getPath().indexOf("ng_generated/") + "ng_generated/".length());
						relativePath = relativePath.replace('\\', '/');

						// Check if the relative path is in the summary but not in generated files
						if (!summaryPaths.contains(relativePath))
						{
							System.err.println("\033[38;5;214mMissing file in summary: " + relativePath + " ::: " + cls.getName() + "\033[0m");
						}
					}
				}
			}
		}

		private List<String> loadSummary()
		{
			List<String> paths = new ArrayList<String>();
			try (BufferedReader br = new BufferedReader(new FileReader(summaryMdFilePath, Charset.forName("UTF-8"))))
			{
				String line;
				while ((line = br.readLine()) != null)
				{
					// Extract paths ending with .md from markdown links
					int start = line.indexOf("](");
					int end = line.indexOf(')', start);
					if (start != -1 && end != -1)
					{
						String mdPath = line.substring(start + 2, end).trim(); // Trim leading/trailing spaces
						if (mdPath.endsWith(".md"))
						{
							// Normalize the path to use '/' as separator
							mdPath = mdPath.replace('\\', '/');
							paths.add(mdPath);
						}
					}
				}
			}
			catch (IOException e)
			{
				System.err.println("\033[38;5;202mFailed to load summary file: " + summaryMdFilePath + "\033[0m");
				e.printStackTrace();
			}
			return paths;

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
