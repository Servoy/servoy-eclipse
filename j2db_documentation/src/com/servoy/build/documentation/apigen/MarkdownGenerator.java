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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
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
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Collectors;
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

	private static Map<String, String> specialTypePaths = new HashMap<>();
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
	private static final List<String> nonDefaultPluginJarNamesThatWeDoGenerateDocsFor = Arrays.asList("servoy_jasperreports.jar"); // we check that these were found so that we don't forget by mistake to generate the docs for them

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

	private String getPublicName()
	{
		return (String)root.get("classname");
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


		String jsLib = args[0];
		String servoyDoc = args[1];
		String designDoc = args[2];
		String pluginDir = args[3];
		boolean generateForAI = "forAI".equals(args[4]);

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
//
//		Class< ? > elusiveClass = targetInstallClassLoader.loadClass("com.servoy.extensions.plugins.jwt.client.Builder");
//		System.out.println("Class that does new instance: " + elusiveClass.getCanonicalName());
//		System.out.println("Class that does new instance was loaded by class loader: " + elusiveClass.getClassLoader());
//
//		elusiveClass = targetInstallClassLoader.loadClass("com.fasterxml.jackson.databind.json.JsonMapper");
//		System.out.println("\nElusive class is not so elusive: " + elusiveClass.getCanonicalName());
//		System.out.println("Elusive class was loaded by class loader: " + elusiveClass.getClassLoader());
//
//		elusiveClass = targetInstallClassLoader.loadClass("com.auth0.jwt.JWTCreator");
//		System.out.println("\nElusive class is not so elusive: " + elusiveClass.getCanonicalName());
//		System.out.println("Elusive class was loaded by class loader: " + elusiveClass.getClassLoader());
//
//		System.out.println("\nCan the class that does new instance be loaded by default classloader?");
//		elusiveClass = MarkdownGenerator.class.getClassLoader().loadClass("com.servoy.extensions.plugins.jwt.client.Builder");
//		System.out.println("Class that does new instance: " + elusiveClass.getCanonicalName());
//		System.out.println("Class that does new instance was loaded by class loader: " + elusiveClass.getClassLoader());
//
//		System.exit(1);

		boolean ngOnly = false;

		URL jsLibURLObject = new File(jsLibURL).toURI().toURL();
		URL servoyDocURLObject = new File(servoyDocURL).toURI().toURL();
		URL designDocURLObject = new File(designDocURL).toURI().toURL();


		do
		{
			ngOnly = !ngOnly;
			final boolean ng = ngOnly;

			System.err.println("Generating core and java plugin content (ngOnly = " + ngOnly + ")");

			DocumentationManager manager;

			if (!generateForAI)
			{
				// TODO when the object model / persists should also be generated for AI, the if can be removed and this code will always execute
				System.err.println("  - " + jsLibURLObject);
				manager = DocumentationManager.fromXML(jsLibURLObject, targetInstallClassLoader);
				docGenerator.processDocObjectToPathAndOtherMaps(manager, "/reference/servoycore/dev-api", null);
				docGenerator.generateDocsFromXML(manager, "/reference/servoycore/dev-api", ngOnly);
			}

			System.err.println("  - " + servoyDocURLObject);
			manager = DocumentationManager.fromXML(servoyDocURLObject, targetInstallClassLoader);
			docGenerator.processDocObjectToPathAndOtherMaps(manager, "/reference/servoycore/dev-api", null);
			docGenerator.generateDocsFromXML(manager, "/reference/servoycore/dev-api", ngOnly);

			if (!generateForAI)
			{
				System.err.println("  - " + designDocURLObject);
				manager = DocumentationManager.fromXML(designDocURLObject, targetInstallClassLoader);
				docGenerator.processDocObjectToPathAndOtherMaps(manager, "/reference/servoycore/object-model/solution", null);
				docGenerator.generateDocsFromXML(manager, "/reference/servoycore/object-model/solution", ngOnly);
			}

			System.err.println("  - plugins (from " + pluginDir + "):");
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
											System.err.println("    * " + jar.getName());

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
				}
				for (String nonDefaultPluginThatShouldHaveBeenFound : nonDefaultPluginJarNamesThatWeDoGenerateDocsFor)
					if (!foundJars.contains(nonDefaultPluginThatShouldHaveBeenFound)) throw new RuntimeException(
						"Cannot find (explicitly required) plugin '" + nonDefaultPluginThatShouldHaveBeenFound + "' in dir: " + pluginDir +
							"\nYou have to manually copy a release of that plugin into the plugins dir...");
			}

			docGenerator.writeAggregatedOutput(ngOnly);
		}
		while (ngOnly);
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
		addAllNestedJarFilesOfDir(Path.of(pluginDir, "..", "..", "developer", "plugins").normalize().toString(), jarURLsFromInstall);

//		List<String> listOfJarURLs;/* = jarURLsFromInstall; */ // new File(jarFile.toURI())
//		String launcherCP = "E:\\GitHome_master\\git\\servoy-eclipse\\j2db_documentation\\bin;E:\\ExportedTargetPlatforms\\master\\plugins\\org.osgi.annotation.versioning_1.1.2.202109301733.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.osgi.annotation.bundle_2.0.0.202202082230.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.osgi.service.component.annotations_1.5.1.202212101352.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.osgi.service.metatype.annotations_1.4.1.202109301733.jar;E:\\GitHome_master\\git\\rhino\\bin;E:\\GitHome_master\\git\\servoy-client\\servoy_base\\bin;E:\\ExportedTargetPlatforms\\master\\plugins\\log4j-api-2.23.1.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\log4j-core-2.23.1.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.apache.commons.codec_1.14.0.v20221112-0806.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\jabsorb-1.3.2.s7.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\commons-fileupload-1.5.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\xstream-1.4.20.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\com.google.guava_33.2.0.jre.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.apache.commons.commons-codec_1.17.0.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.jetty.servlet-api_4.0.6.jar;E:\\GitHome_master\\git\\sablo\\sablo\\bin;E:\\ExportedTargetPlatforms\\master\\plugins\\javax.websocket-1.1.0.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.apache.commons.commons-io_2.16.1.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\slf4j.api_2.0.13.jar;e:\\Maven\\Andrei\\.m2\\repository\\org\\slf4j\\slf4j-api\\2.0.7\\slf4j-api-2.0.7.jar;e:\\Maven\\Andrei\\.m2\\repository\\org\\apache\\logging\\log4j\\log4j-api\\2.22.1\\log4j-api-2.22.1.jar;e:\\Maven\\Andrei\\.m2\\repository\\org\\apache\\logging\\log4j\\log4j-core\\2.22.1\\log4j-core-2.22.1.jar;e:\\Maven\\Andrei\\.m2\\repository\\org\\apache\\logging\\log4j\\log4j-slf4j2-impl\\2.22.1\\log4j-slf4j2-impl-2.22.1.jar;e:\\Maven\\Andrei\\.m2\\repository\\org\\mockito\\mockito-core\\3.12.4\\mockito-core-3.12.4.jar;e:\\Maven\\Andrei\\.m2\\repository\\junit\\junit\\4.13.1\\junit-4.13.1.jar;e:\\Maven\\Andrei\\.m2\\repository\\org\\skyscreamer\\jsonassert\\1.5.1\\jsonassert-1.5.1.jar;e:\\Maven\\Andrei\\.m2\\repository\\org\\hamcrest\\hamcrest-core\\1.3\\hamcrest-core-1.3.jar;e:\\Maven\\Andrei\\.m2\\repository\\net\\bytebuddy\\byte-buddy-agent\\1.12.4\\byte-buddy-agent-1.12.4.jar;e:\\Maven\\Andrei\\.m2\\repository\\org\\objenesis\\objenesis\\3.2\\objenesis-3.2.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\dom4j-2.1.4.jar;E:\\GitHome_master\\git\\servoy-client\\servoy_shared\\bin;E:\\GitHome_master\\git\\servoy-client\\servoy_shared\\lib\\fs-commons.jar;E:\\GitHome_master\\git\\servoy-client\\servoy_shared\\lib\\fs-parser.jar;E:\\GitHome_master\\git\\servoy-client\\servoy_shared\\lib\\PBKDF2.jar;E:\\GitHome_master\\git\\servoy-client\\servoy_smart_client\\bin;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.core.runtime_3.31.100.v20240524-2010.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.osgi_3.20.0.v20240509-1421.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.osgi.compatibility.state_1.2.1000.v20240213-1057.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.equinox.common_3.19.100.v20240524-2011.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.core.jobs_3.15.300.v20240418-0734.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.equinox.registry_3.12.100.v20240524-2011.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.equinox.preferences_3.11.100.v20240327-0645.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.osgi.service.prefs_1.1.2.202109301733.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.core.contenttype_3.9.400.v20240507-1301.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.equinox.app_1.7.100.v20240321-1445.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.ui_3.206.0.v20240524-2010.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.swt_3.126.0.v20240528-0813.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.swt.win32.win32.x86_64_3.126.0.v20240528-0813.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.pde.api.tools.annotations_1.3.0.v20240207-2106.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.core.commands_3.12.100.v20240424-0956.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.equinox.bidi_1.5.100.v20240321-1445.jar;E:\\GitHome_master\\git\\servoy-eclipse\\org.eclipse.jface\\bin;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.ui.workbench_3.132.0.v20240524-2010.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.e4.ui.workbench3_0.17.400.v20240321-1245.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.jsoup_1.18.1.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\uap-java-1.6.1-SERVOY.jar;E:\\GitHome_master\\git\\servoy-client\\servoy_headless_client\\bin;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\bin;E:\\ExportedTargetPlatforms\\master\\plugins\\com.sun.el.javax.el_3.0.4.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\jakarta.servlet.jsp-api-2.3.6.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\annotations-api.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\catalina.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\catalina-ant.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\catalina-ha.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\catalina-ssi.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\catalina-storeconfig.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\catalina-tribes.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\ecj-4.20.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\jasper.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\jasper-el.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\jaspic-api.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-api.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-coyote.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-dbcp.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-i18n-cs.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-i18n-de.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-i18n-es.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-i18n-fr.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-i18n-ja.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-i18n-ko.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-i18n-pt-BR.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-i18n-ru.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-i18n-zh-CN.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-jdbc.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-jni.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-juli.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-util.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-util-scan.jar;E:\\GitHome_master\\git\\servoy-eclipse-tomcat\\org.apache.tomcat\\lib\\tomcat-websocket.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\jlessc-1.10.0.s4.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.apache.commons.lang3_3.15.0.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\commons-text-1.12.0.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\freemarker-2.3.33.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\tus-java-server-2.0.0.s2.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\java-jwt-4.4.0.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\httpclient5-5.3.1.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\httpcore5-5.2.5.jar;E:\\GitHome_master\\git\\servoy-client\\servoy_ngclient\\bin;E:\\GitHome_master\\git\\servoy-client\\servoy_debug\\bin;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.core.resources_3.20.200.v20240513-1323.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.wst.css.core_1.3.400.v202308160453.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.apache.xerces_2.12.2.v20230928-1306.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.javascript.parser_5.2.0.202407231204.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.core_5.1.1.202407231120.jar;E:\\GitHome_master\\git\\servoy-eclipse\\com.servoy.eclipse.ngclient\\bin;E:\\GitHome_master\\git\\server\\j2db_log4j\\bin;E:\\ExportedTargetPlatforms\\master\\plugins\\javax.transaction-1.1.0.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\hibernate-core-5.6.15.Servoy3.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\commons-dbcp2-2.12.0.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\commons-pool2-2.12.0.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\javax.persistence-api-2.2.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\jboss-logging-3.6.0.Final.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\byte-buddy-1.14.18.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\com.google.gson_2.11.0.jar;E:\\GitHome_master\\git\\server\\j2db_server\\bin;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.annotations_5.1.1.202407231120.jar;E:\\GitHome_master\\git\\servoy-eclipse\\com.servoy.eclipse.model\\bin;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.javascript.core_5.1.1.202407231204.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.ui.ide_3.22.200.v20240524-2010.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.e4.ui.ide_3.17.200.v20231201-1637.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.core.filesystem_1.10.400.v20240426-1040.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.ui.intro_3.7.400.v20240414-0828.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.ui.cheatsheets_3.8.400.v20240414-1916.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.help.ui_4.7.0.v20240414-1916.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.help.base_4.4.400.v20240601-0610.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.help_3.10.400.v20240415-0528.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.core.expressions_3.9.400.v20240413-1529.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.team.core_3.10.400.v20240413-1529.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.ui.workbench.texteditor_3.17.400.v20240524-2010.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.debug.ui_5.1.1.202407231120.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.debug_5.1.1.202407231120.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.debug.core_3.21.400.v20240415-0528.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.wst.css.ui_1.2.200.v202308160453.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.core.filebuffers_3.8.300.v20240207-1054.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.jface.text_3.25.100.v20240524-2010.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.text_3.14.100.v20240524-2010.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.ui_5.1.1.202407231120.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.ui.views_3.12.300.v20240524-2010.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.javascript.ui_5.1.1.202407231204.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.ui.editors_3.17.300.v20240524-2010.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.ui.forms_3.13.300.v20240424-0956.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.e4.ui.css.swt.theme_0.14.400.v20240424-0956.jar;E:\\GitHome_master\\git\\servoy-eclipse\\com.servoy.eclipse.model.exporter.mobile\\bin;E:\\GitHome_master\\git\\servoy-eclipse\\com.servoy.eclipse.model.exporter.mobile\\lib\\js.jar;E:\\GitHome_master\\git\\servoy-eclipse\\com.servoy.eclipse.model.exporter.mobile\\lib\\jshybugger.jar;E:\\GitHome_master\\git\\servoy-eclipse\\com.servoy.eclipse.model.exporter.mobile\\lib\\netty-3.6.5.Final.jar;E:\\GitHome_master\\git\\servoy-eclipse\\com.servoy.eclipse.model.exporter.mobile\\lib\\webbit-0.4.15.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\net.sourceforge.sqlexplorer_3.6.2.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.external_libraries\\net.sourceforge.sqlexplorer_3.6.2\\sqlexplorer.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.external_libraries\\net.sourceforge.sqlexplorer_3.6.2\\lib\\log4j.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.external_libraries\\net.sourceforge.sqlexplorer_3.6.2\\lib\\rowset.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.external_libraries\\net.sourceforge.sqlexplorer_3.6.2\\lib\\dom4j.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.external_libraries\\net.sourceforge.sqlexplorer_3.6.2\\lib\\fw.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.external_libraries\\net.sourceforge.sqlexplorer_3.6.2\\lib\\hibernate3.2.4.sp1.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.external_libraries\\net.sourceforge.sqlexplorer_3.6.2\\lib\\poi-3.5-FINAL-20090928.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.external_libraries\\net.sourceforge.sqlexplorer_3.6.2\\lib\\commons-logging.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\sj.jsonschemavalidation-2.0.1.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.external_libraries\\sj.jsonschemavalidation_2.0.1\\lib\\jackson-core-2.4.3.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.external_libraries\\sj.jsonschemavalidation_2.0.1\\lib\\jackson-databind-2.4.3.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.external_libraries\\sj.jsonschemavalidation_2.0.1\\lib\\json-schema-validator-2.2.5-lib.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.external_libraries\\sj.jsonschemavalidation_2.0.1\\lib\\jackson-annotations-2.4.3.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.core.manipulation_5.1.1.202407231120.jar;E:\\GitHome_master\\git\\servoy-eclipse\\com.servoy.eclipse.core\\bin;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.ui.console_3.14.100.v20240429-1358.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.debug.ui_3.18.400.v20240516-0857.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.console.ui_5.1.1.202407231120.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.console_5.1.1.202407231120.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.javascript.launching_5.1.1.202407231204.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.launching_5.1.1.202407231120.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.emf.ecore_2.36.0.v20240203-0859.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.emf.common_2.30.0.v20240314-0928.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.javascript.jsjdtdebugger_5.1.1.202407231204.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.javascript.debug_5.1.1.202407231204.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.javascript.debug.ui_5.1.1.202407231204.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\com.ibm.icu_75.1.0.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.search_3.16.200.v20240426-0859.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.search.core_3.16.200.v20240502-1134.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.core.databinding_1.13.300.v20240424-0444.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.core.databinding.observable_1.13.300.v20240424-0444.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.core.databinding.beans_1.10.300.v20240321-1245.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.core.databinding.property_1.10.300.v20240424-0444.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.jface.databinding_1.15.300.v20240424-0444.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.javascript.formatter_5.1.1.202407231204.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.dltk.formatter_5.1.1.202407231120.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.wst.sse.ui_1.7.1000.v202404170147.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.wst.sse.core_1.2.1400.v202405130132.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.wst.xml.core_1.2.900.v202405130132.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.wst.xml.ui_1.2.701.v202308160453.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.wst.validation_1.3.0.v202308161955.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.wst.validation.ui_1.3.100.v202405020134.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.wst.html.core_1.4.400.v202308160453.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.wst.html.ui_1.1.801.v202308160453.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.ui.browser_3.8.300.v20240524-2010.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.ui.externaltools_3.6.400.v20240416-0654.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.equinox.p2.core_2.12.0.v20240515-1919.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.equinox.p2.repository_2.9.100.v20240511-1722.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.equinox.p2.metadata_2.9.100.v20240416-0654.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.equinox.p2.ui_2.8.400.v20240511-1722.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.equinox.p2.operations_2.7.400.v20240425-0751.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.team.ui_3.10.400.v20240416-0654.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.ltk.core.refactoring_3.14.400.v20240321-1245.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.ltk.ui.refactoring_3.13.400.v20240321-1245.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.maven.ide.eclipse.grouplayout-1.1.0.201005260935.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.external_libraries\\org.maven.ide.eclipse.grouplayout_1.1.0.201005260935\\jars\\swt-grouplayout-7.4.0-r35.jar;E:\\GitHome_master\\git\\servoy-eclipse\\com.servoy.eclipse.ui.tweaks\\bin;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.equinox.security_1.4.300.v20240419-2334.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.tukaani.xz_1.9.0.jar;E:\\GitHome_master\\git\\servoy-eclipse\\com.servoy.eclipse.ngclient.ui\\bin;E:\\ExportedTargetPlatforms\\master\\plugins\\com.equo.chromium_124.0.1.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.nebula.widgets.nattable.core_2.4.0.202405230453.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.nebula.widgets.nattable.extension.nebula_2.4.0.202405230453.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.e4.ui.css.swt_0.15.400.v20240321-1245.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.nebula.widgets.nattable.extension.e4_2.4.0.202405230453.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.jgit_6.10.0.202406032230-r.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.jgit.gpg.bc_6.10.0.202406032230-r.jar;E:\\ExportedTargetPlatforms\\master\\plugins\\org.eclipse.egit.core_6.10.0.202406032230-r.jar;E:\\GitHome_master\\git\\servoy-eclipse\\com.servoy.eclipse.cloud\\bin;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.tm4e.ui_0.12.0.202405210827.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.tm4e.core_0.12.0.202405210827.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.tm4e.languageconfiguration_0.12.0.202405210827.jar;E:\\Workspaces\\master\\.metadata\\.plugins\\org.eclipse.pde.core\\.bundle_pool\\plugins\\org.eclipse.ui.genericeditor_1.3.400.v20240511-1105.jar;E:\\GitHome_master\\git\\servoy-eclipse\\com.servoy.eclipse.ui\\bin;E:\\ExportedTargetPlatforms\\master\\plugins\\org.apache.commons.commons-compress_1.26.2.jar;E:\\GitHome_master\\git\\servoy-eclipse\\com.servoy.eclipse.debug\\bin";
//		String[] entries = launcherCP.split(";");
//		listOfJarURLs = List.of(entries);
//		System.out.println(listOfJarURLs);
//		for (String jarFile : listOfJarURLs)
//		{
//			if (new File(jarFile).isFile())
//			{
//				ZipFile zip = null;
//				try
//				{
//					zip = new ZipFile(jarFile);
//					zip.stream().filter((entry) -> entry.isDirectory() && entry.toString()
//						.contains("com/auth0/jwt"/* "com/fasterxml/jackson/databind/json" */))
//						.forEach((e) -> System.out.println("Jar " + jarFile + " contains entry: " + e));
//				}
//				catch (IOException e)
//				{
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				finally
//				{
//					if (zip != null) zip.close();
//				}
//			}
//		}
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

	private void table(String name, List<IFunctionDocumentation> functions, Class< ? > cls, boolean ngOnly)
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
				FunctionTemplateModel ftm = new FunctionTemplateModel(fd, MarkdownGenerator::getPublicName, cls, ngOnly,
					htmlToMarkdownConverter);
				models.add(ftm);
//			if ("void".equals(ftm.getReturnType()) || ftm.getReturnType() == null)
//			{
//				start = start.t("void").up(2);
//			}
//			else
//			{
//				start = start.e(LNK).e(PG).a(CT, ftm.getReturnType()).up(4);
//			}
//
//			String functionName = getFullFunctionName(fd, cls);
//			start = start.e(MCR).a(NM, "td").e(RTB).e(LNK).a(ANC, functionName).e(PTLB).cdata(functionName).up(4).e(MCR).a(NM, "td").e(RTB).t(
//				ftm.getSummary()).up(4);
			}
			root.put(name, models);
		}
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
		// TODO Auto-generated method stub
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
				System.err.println("public name not found for " + type);
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
//				returnTypesToParentName.put(doc.getPublicName(), doc.getPublicName());
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
				cg.table("constants", constants, cls, ngOnly);
				if (properties != null) properties = properties.stream().filter(node -> node.getReturnedType() != void.class).collect(Collectors.toList());
				cg.table("properties", properties, cls, ngOnly);
				cg.table("commands", commands, cls, ngOnly);
				cg.table("events", events, cls, ngOnly);
				cg.table("methods", methods, cls, ngOnly);
//			if (events != null && events.size() > 0) System.err.println(events.size() + value.getPublicName());

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
					if (aggregatedOutput != null) aggregatedOutput.append(output);
				}

//			file = new File(userDir, "generated_old/" + value.getPublicName() + ".html");
//			if (file.exists())
//			{
//				if (file.length() == output.length())
//				{
//					// check if it is still the same if the number of bytes are equal.
//					try (FileReader reader = new FileReader(file))
//					{
//						char[] buf = new char[(int)file.length()];
//						reader.read(buf);
//						String old = new String(buf);
//						if (old.equals(output))
//						{
//							System.out.println("not updating remote content because the file is the same as the old value " + file);
//							continue;
//						}
//					}
//				}
//			}
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
				}

				aggregatedOutput.setLength("Servoy scripting is based on javascript. Here is the API:\n\n".length());
			}
		}

	}

	private static record PluginDocumentationPreparated(DocumentationManager docManager, String pluginPath)
	{
	}

}
