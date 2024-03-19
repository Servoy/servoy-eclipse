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
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import com.servoy.j2db.scripting.JSMap;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.solutionmodel.ICSSPosition;
import com.servoy.j2db.solutionmodel.ISMPart;
import com.servoy.j2db.util.ServoyJSONObject;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

/**
 * @author jcompagner
 */
public class MarkdownGenerator
{
	private static Configuration cfg;
	private static Template temp;

	private static Map<String, String> defaultTypePath = new HashMap<>();
	static
	{
		defaultTypePath.put("Boolean", "/js-lib");
		defaultTypePath.put("String", "/js-lib");
		defaultTypePath.put("Date", "/js-lib");
		defaultTypePath.put("Number", "/js-lib");
		defaultTypePath.put("Array", "/js-lib");
		defaultTypePath.put("Object", "/js-lib");
		defaultTypePath.put("Function", "/js-lib");
		defaultTypePath.put("IterableValue", "/js-lib");
		defaultTypePath.put("Iterator", "/js-lib");
		defaultTypePath.put("JSON", "/js-lib");
		defaultTypePath.put("JS Lib", "/js-lib");
		defaultTypePath.put("Map", "/js-lib");
		defaultTypePath.put("Math", "/js-lib");
		defaultTypePath.put("Namespace", "/js-lib");
		defaultTypePath.put("QName", "/js-lib");
		defaultTypePath.put("RegExp", "/js-lib");
		defaultTypePath.put("Set", "/js-lib");
		defaultTypePath.put("Special Operators", "/js-lib");
		defaultTypePath.put("Statements", "/js-lib");
		defaultTypePath.put("XML", "/js-lib");
		defaultTypePath.put("XMLList", "/js-lib");
		defaultTypePath.put("Exception", "/");
		// special types
		defaultTypePath.put("JSServer", "/plugins/maintenance/");
		defaultTypePath.put("JSTableObject", "/plugins/maintenance/");
		defaultTypePath.put("JSColumnObject", "/plugins/maintenance/");
		defaultTypePath.put("Component", "/forms/runtimeform/elements");


	}
	private static final HashMap<String, String> qualifiedToName = new HashMap<>();
	private static final HashMap<String, String> publicToRootPath = new HashMap<>();
	private static final HashMap<String, String> returnTypesToParentName = new HashMap<>();
	private static final Set<String> storeAsReadMe = new HashSet<>();

	static
	{
		storeAsReadMe.add("Application");
		storeAsReadMe.add("Database Manager");
		storeAsReadMe.add("SolutionModel");
		storeAsReadMe.add("Datasources");
		storeAsReadMe.add("Forms");
		storeAsReadMe.add("RuntimeForm");
		storeAsReadMe.add("JS Lib");
		storeAsReadMe.add("ServoyException");
		storeAsReadMe.add("Solution");

	}

	private final Map<String, Object> root;
	private final Path path;

	public MarkdownGenerator(String publicName, String parentPath)
	{
		root = new HashMap<>();
		root.put("classname", publicName);
		String classNoSpace = publicName.replace(" ", "%20").toLowerCase();
		if (storeAsReadMe.contains(publicName))
		{
			classNoSpace = "README";
		}
		root.put("classname_nospace", classNoSpace);
		root.put("instance", this);

		if ("/design-api".equals(parentPath))
		{
			String returnType = returnTypesToParentName.get(publicName);
			if (returnType != null)
			{
				path = Paths.get(parentPath, returnType);
			}
			else
			{
				path = Paths.get(parentPath);
			}
		}
		else
		{
			path = parentPath != null ? Paths.get(parentPath) : Paths.get(generatePath(publicName));
		}
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException, URISyntaxException
	{
		cfg = new Configuration(Configuration.VERSION_2_3_31);
		cfg.setTemplateLoader(new ClassTemplateLoader(MarkdownGenerator.class, "template"));
		cfg.setDefaultEncoding("UTF-8");

		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setLogTemplateExceptions(false);
		cfg.setWrapUncheckedExceptions(true);
		cfg.setFallbackOnNullLoopVariable(false);
		fillStaticParents(returnTypesToParentName);


		temp = cfg.getTemplate("markdown_template.md");

		String jsLib = args[0];
		String servoyDoc = args[1];
		String designDoc = args[2];
		String pluginDir = args[3];

		generateCoreAndPluginDocs(jsLib, servoyDoc, designDoc, pluginDir, new MarkdownDocFromXMLGenerator());
	}

	public static List<String> getSupportedClientsList(ClientSupport clientSupport)
	{
		List<String> support = new ArrayList<>();
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
		return support;
	}

	public static void fillStaticParents(HashMap<String, String> retTypesToParentName)
	{
		retTypesToParentName.put("DataException", "ServoyException");
//		JSColumnObject");
//		JSServer");
//		JSTableObject");
		retTypesToParentName.put("RuntimeForm", "Forms");
		retTypesToParentName.put("RuntimeContainer", "RuntimeForm");
		retTypesToParentName.put("containers", "RuntimeForm");
		retTypesToParentName.put("elements", "RuntimeForm");
		retTypesToParentName.put("controller", "RuntimeForm");

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


		retTypesToParentName.put("Calendar", "Form");
		retTypesToParentName.put("Footer", "Form");
		retTypesToParentName.put("Header", "Form");
		retTypesToParentName.put("HeaderTitle", "Form");
		retTypesToParentName.put("InsetList", "Form");
		retTypesToParentName.put("Layout Container", "Form");
		retTypesToParentName.put("ListForm", "Form");
		retTypesToParentName.put("Bean", "Form");
		retTypesToParentName.put("Button", "Form");
		retTypesToParentName.put("CheckBoxes", "Form");
		retTypesToParentName.put("ComboBox", "Form");
		retTypesToParentName.put("Image", "Form");
		retTypesToParentName.put("Label", "Form");
		retTypesToParentName.put("Part", "Form");
		retTypesToParentName.put("Password", "Form");
		retTypesToParentName.put("Portal", "Form");
		retTypesToParentName.put("RadioButtons", "Form");
		retTypesToParentName.put("Rectangle", "Form");
		retTypesToParentName.put("TabPanel", "Form");
		retTypesToParentName.put("Tab", "Form");
		retTypesToParentName.put("Table", "Form");
		retTypesToParentName.put("TextArea", "Form");
		retTypesToParentName.put("TextField", "Form");

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
		for (IFunctionDocumentation fd : functions)
		{
			if (fd.getClientSupport().hasSupport(ClientSupport.Default))
			{
				if (fd.getType().intValue() == typeEvent.intValue())
				{
					retValue.add(fd);
				}
			}
		}
		return retValue.size() > 0 ? retValue : null;
	}

	public static void generateCoreAndPluginDocs(String jsLibURL, String servoyDocURL, String designDocURL, String pluginDir, IDocFromXMLGenerator docGenerator)
		throws MalformedURLException, ClassNotFoundException, IOException, URISyntaxException, ZipException
	{
		boolean ngOnly = false;

		do
		{
			ngOnly = !ngOnly;
			final boolean ng = ngOnly;

			System.err.println("Generating core and java plugin content (ngOnly = " + ngOnly + ")");

			System.err.println("  - " + jsLibURL);
			DocumentationManager manager = DocumentationManager.fromXML(new URL(jsLibURL), MarkdownGenerator.class.getClassLoader());
			docGenerator.generateDocsFromXML(manager, null, ngOnly);

			System.err.println("  - " + servoyDocURL);
			manager = DocumentationManager.fromXML(new URL(servoyDocURL), MarkdownGenerator.class.getClassLoader());
			docGenerator.generateDocsFromXML(manager, null, ngOnly);

			System.err.println("  - " + designDocURL);
			manager = DocumentationManager.fromXML(new URL(designDocURL), MarkdownGenerator.class.getClassLoader());
			docGenerator.generateDocsFromXML(manager, "/design-api", ngOnly);

			System.err.println("  - " + designDocURL);
			manager = DocumentationManager.fromXML(new URL(designDocURL), MarkdownGenerator.class.getClassLoader());
			docGenerator.generateDocsFromXML(manager, "/design-api", ngOnly);

			System.err.println("  - plugins (from " + pluginDir + "):");
			File file2 = new File(new URI(pluginDir).normalize());
			if (file2.isDirectory())
			{
				// this is an directory with jars
				File[] jars = file2.listFiles((child) -> child.getName().toLowerCase().endsWith(".jar"));
				for (File jar : jars)
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
							}).filter(is -> is != null)
							.forEach(is -> {
								try
								{
									System.err.println("    * " + jar.getName());
									docGenerator.generateDocsFromXML(DocumentationManager.fromXML(is, MarkdownGenerator.class.getClassLoader()),
										"/plugins/" + jar.getName().substring(0, jar.getName().length() - 4), ng);
								}
								catch (ClassNotFoundException | IOException e)
								{
									e.printStackTrace();
								}
							});

					}
				}
			}
		}
		while (ngOnly);
	}

	private void table(String name, List<IFunctionDocumentation> functions, Class< ? > cls, boolean ngOnly)
	{
		if (functions != null && functions.size() > 0)
		{
			List<FunctionTemplateModel> models = new ArrayList<>();
			for (IFunctionDocumentation fd : functions)
			{
				if (fd.isDeprecated()) continue;
				if (ngOnly && !fd.getClientSupport().hasSupport(ClientSupport.ng)) continue;
				FunctionTemplateModel ftm = new FunctionTemplateModel(fd, MarkdownGenerator::getPublicName, cls, ngOnly);
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

	private void classList(String section, Class< ? >[] alltypes)
	{
		if (alltypes != null && alltypes.length > 0)
		{
			List<String> publicNames = new ArrayList<>(alltypes.length);
			for (Class< ? > alltype : alltypes)
			{
				String publicName = getPublicName(alltype);
				if (publicName != null && !"Object".equals(publicName))
				{
					publicNames.add(publicName);
				}
			}
			root.put(section, publicNames);
		}
	}

	private String generate()
	{
		// TODO Auto-generated method stub
		StringWriter out = new StringWriter();
		try
		{
			temp.process(root, out);
		}
		catch (TemplateException | IOException e)
		{
			e.printStackTrace();
		}
		return out.toString();
	}

	public String getReturnTypePath(String publicName)
	{
		Path p1 = Paths.get(generatePath(publicName));
		Path relativize = path.relativize(p1);
		String relativePath = relativize.toString().replace('\\', '/').replace(" ", "%20");
		return ((relativePath.isBlank() ? "." : relativePath) + "/" + publicName + ".md").toLowerCase();
	}

	public Path getPath()
	{
		return path;
	}

	private String generatePath(String publicName)
	{
		String p = defaultTypePath.get(publicName);
		if (p == null)
		{
			String parent = returnTypesToParentName.get(publicName);
			if (parent == null)
			{
				if (storeAsReadMe.contains(publicName))
				{
					p = "/" + publicName;
				}
				else p = "/";
			}
			else
			{
				String rootPath = publicToRootPath.get(parent);
				if (rootPath != null)
				{
					return rootPath;
				}
				else
				{
					String parent2 = returnTypesToParentName.get(parent);
					while (parent2 != null)
					{
						parent = parent2 + "/" + parent;
						parent2 = returnTypesToParentName.get(parent2);
					}
				}
				p = "/" + parent;
			}
		}
		return p;
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

		public void generateDocsFromXML(DocumentationManager manager, String path, boolean ngOnly)
			throws MalformedURLException, ClassNotFoundException, IOException
		{
			SortedMap<String, IObjectDocumentation> objects = manager.getObjects();
			for (IObjectDocumentation doc : objects.values())
			{
				qualifiedToName.put(doc.getQualifiedName(), doc.getPublicName());
				if (path != null) publicToRootPath.put(doc.getPublicName(), path);
			}
			for (IObjectDocumentation doc : objects.values())
			{
				Class< ? > cls = Class.forName(doc.getQualifiedName());
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
			}
			File userDir = new File(System.getProperty("user.dir"));
			for (Entry<String, IObjectDocumentation> entry : objects.entrySet())
			{
				IObjectDocumentation value = entry.getValue();
				if (value.isDeprecated() || value.getPublicName().equals("PrinterJob") || value.getFunctions().size() == 0) continue;
				MarkdownGenerator cg = new MarkdownGenerator(value.getPublicName(), path);
				Class< ? > cls = Class.forName(value.getQualifiedName());
				IReturnedTypesProvider returnTypes = ScriptObjectRegistry.getScriptObjectForClass(cls);
				if (returnTypes != null && returnTypes.getAllReturnedTypes() != null)
				{
					Class< ? >[] allReturnedTypes = returnTypes.getAllReturnedTypes();
					ArrayList<Class< ? >> filterReturnTypes = new ArrayList<>();
					for (Class< ? > clsReturn : allReturnedTypes)
					{
						IObjectDocumentation retDoc = objects.get(clsReturn.getCanonicalName());
						if (retDoc == null || !retDoc.isDeprecated())
						{
							filterReturnTypes.add(clsReturn);
						}
					}
					cg.classList("returnTypes", filterReturnTypes.toArray(new Class< ? >[filterReturnTypes.size()]));
				}

				if (!ngOnly) cg.generateClientSupport(value);

				if (value.getExtendsClass() != null)
				{
					cg.classList("extends", new Class[] { Class.forName(value.getExtendsClass()) });
				}


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
	}

}
