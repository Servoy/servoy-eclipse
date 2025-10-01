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

package com.servoy.build.documentation.ai;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.json.JSONException;

import com.servoy.build.documentation.DocumentationManager;
import com.servoy.build.documentation.apigen.IDocFromXMLGenerator;
import com.servoy.build.documentation.apigen.INGPackageInfoGenerator;
import com.servoy.build.documentation.apigen.SpecMarkdownGenerator;
import com.servoy.build.documentation.apigen.SpecMarkdownGenerator.Function;
import com.servoy.build.documentation.apigen.SpecMarkdownGenerator.NGPackageInfoGenerator;
import com.servoy.build.documentation.apigen.SpecMarkdownGenerator.Property;

import freemarker.cache.ClassTemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateNotFoundException;

/**
 * @author acostescu
 */
public abstract class AbstractAIInfoGenerator
{

	protected static final String DATE_OF_REFERENCE_FORMATTED = "Jan 1, 2025";

	/**
	 * USE "" for production training!!!
	 * It's just a marker that can be used when generating training jsonl, so that we see how well the training catches on. It will be added in various places in the training file. Then when you start chatting with the trained model you can see if it answers based on that training or based on something else.
	 *
	 * This might get included in parameter names, method names, property names or various other identifiers. So keep it clean: no spaces etc.
	 */
	protected static final String TRAINING_FOOTPRINT = "ftpt531";

	private final Configuration cfg;

	public AbstractAIInfoGenerator()
	{
		cfg = new Configuration(Configuration.VERSION_2_3_31);
		cfg.setTemplateLoader(new ClassTemplateLoader(AbstractAIInfoGenerator.class, "template"));
		cfg.setDefaultEncoding("UTF-8");

		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setLogTemplateExceptions(false);
		cfg.setWrapUncheckedExceptions(true);
		cfg.setFallbackOnNullLoopVariable(false);
	}

	protected Configuration getFTLCfg()
	{
		return cfg;
	}

	/**
	 * It will generate all the needed info related to Servoy core, plugins and package APIs.
	 * These are things that are injected afterwards based on a similarity search into the chat prompt for the GPT-x LLM model to have more info when generating an answer.
	 * @param infoFromNGPackagesGenerator
	 *
	 * @param args 0 - uri to "servoydoc_jslib.xml"
	 *             1 - uri to "servoydoc.xml"
	 *             2 - uri to "servoydoc_design.xml"
	 *             3 - uri to the plugin dir of an installed application server (for java plugins)
	 *             4 - path to the text file that contains on each line one location of one component/service/layout (ng) package dir - to generate the info for
	 * @throws TemplateException
	 * @throws JSONException
	 */
	public void generate(String jsLibURI, String servoyDocURI, String designDocURI, String pluginDirURI, String ngPackagesFileLocationsURI,
		INGPackageInfoGenerator infoFromNGPackagesGenerator, Object utilityObjectForTemplates) throws Exception
	{
//		ConfluenceGenerator.fillStaticParents(returnTypesToParentName);

		// Servoy core and java plug-in -> info source text (to be embedded)
		CoreAndJavaPluginsInfoGenerator.initTemplates(cfg);
		// FIXME uncomment the following line
//		MarkdownGenerator.generateCoreAndPluginDocs(jsLibURI, servoyDocURI, designDocURI, pluginDirURI, infoFromJavaAPIXMLsGenerator);

		// ng package dirs listed in text file ngPackagesFileLocationsURI -> info source text (to be embedded)
		List<String> ngPackageDirsToScan = Files.readAllLines(Paths.get(ngPackagesFileLocationsURI).normalize());
		SpecMarkdownGenerator.generateNGComponentOrServicePackageContentForDir(true, false, ngPackageDirsToScan,
			infoFromNGPackagesGenerator,
			Map.of("referenceDate", DATE_OF_REFERENCE_FORMATTED, "trainingFootprint", TRAINING_FOOTPRINT, "utils", utilityObjectForTemplates));
	}

	protected static class InfoFromXMLGenerator implements IDocFromXMLGenerator
	{

		private final Consumer<String> registerNewInfo;

		public InfoFromXMLGenerator(Consumer<String> registerNewInfo)
		{
			this.registerNewInfo = registerNewInfo;
		}

		public void processDocObjectToPathAndOtherMaps(DocumentationManager manager, String path, String pluginProviderPublicName)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException
		{
			// anything here?
		}

		public void generateDocsFromXML(DocumentationManager manager, String path, boolean ngOnly)
			throws ClassNotFoundException, IOException
		{
			// FIXME implememnt
//
//			SortedMap<String, IObjectDocumentation> objects = manager.getObjects();
//			for (IObjectDocumentation doc : objects.values())
//			{
//				qualifiedToName.put(doc.getQualifiedName(), doc.getPublicName());
//				if (path != null) publicToRootPath.put(doc.getPublicName(), path);
//			}
//			for (IObjectDocumentation doc : objects.values())
//			{
//				Class< ? > cls = Class.forName(doc.getQualifiedName());
//				IReturnedTypesProvider returnTypes = ScriptObjectRegistry.getScriptObjectForClass(cls);
//				if (returnTypes != null && returnTypes.getAllReturnedTypes() != null)
//				{
//					for (Class< ? > retCls : returnTypes.getAllReturnedTypes())
//					{
//						String qname = qualifiedToName.get(retCls.getCanonicalName());
//						if (qname != null)
//						{
//							returnTypesToParentName.put(qname, doc.getPublicName());
//						}
//						else
//						{
//							System.err.println(" qname not found for " + retCls);
//						}
//					}
//				}
//			}
//			File userDir = new File(System.getProperty("user.dir"));
//			for (Entry<String, IObjectDocumentation> entry : objects.entrySet())
//			{
//				IObjectDocumentation value = entry.getValue();
//				if (value.isDeprecated() || value.getPublicName().equals("PrinterJob") || value.getFunctions().size() == 0) continue;
//				FullPineconeEmbeddingsGenerator cg = new CoreAndJavaPluginsInfoGenerator(value.getPublicName(), path);
//				Class< ? > cls = Class.forName(value.getQualifiedName());
//				IReturnedTypesProvider returnTypes = ScriptObjectRegistry.getScriptObjectForClass(cls);
//				if (returnTypes != null && returnTypes.getAllReturnedTypes() != null)
//				{
//					Class< ? >[] allReturnedTypes = returnTypes.getAllReturnedTypes();
//					ArrayList<Class< ? >> filterReturnTypes = new ArrayList<>();
//					for (Class< ? > clsReturn : allReturnedTypes)
//					{
//						IObjectDocumentation retDoc = objects.get(clsReturn.getCanonicalName());
//						if (retDoc == null || !retDoc.isDeprecated())
//						{
//							filterReturnTypes.add(clsReturn);
//						}
//					}
//					cg.classList("returnTypes", filterReturnTypes.toArray(new Class< ? >[filterReturnTypes.size()]));
//				}
//
//				if (!ngOnly) cg.generateClientSupport(value);
//
//				if (value.getExtendsClass() != null)
//				{
//					cg.classList("extends", new Class[] { Class.forName(value.getExtendsClass()) });
//				}
//
//
//				SortedSet<IFunctionDocumentation> functions = value.getFunctions();
//				List<IFunctionDocumentation> constants = ConfluenceGenerator.getConstants(functions);
//				List<IFunctionDocumentation> properties = ConfluenceGenerator.getProperties(functions);
//				List<IFunctionDocumentation> commands = ConfluenceGenerator.getCommands(functions);
//				List<IFunctionDocumentation> events = ConfluenceGenerator.getEvents(functions);
//				List<IFunctionDocumentation> methods = ConfluenceGenerator.getMethods(functions);
//				cg.table("constants", constants, cls, ngOnly);
//				if (properties != null) properties = properties.stream().filter(node -> node.getReturnedType() != void.class).collect(Collectors.toList());
//				cg.table("properties", properties, cls, ngOnly);
//				cg.table("commands", commands, cls, ngOnly);
//				cg.table("events", events, cls, ngOnly);
//				cg.table("methods", methods, cls, ngOnly);
			////			if (events != null && events.size() > 0) System.err.println(events.size() + value.getPublicName());
//
//				String output = cg.generate();
//				String parent = cg.getPath().toString();
//				File file = new File(userDir, (ngOnly ? "ng_generated/" : "generated/") + (parent + '/' + value.getPublicName() + ".md").toLowerCase());
//				file.getParentFile().mkdirs();
//				try (FileWriter writer = new FileWriter(file))
//				{
//					writer.write(output);
//				}
//
			////			file = new File(userDir, "generated_old/" + value.getPublicName() + ".html");
////			if (file.exists())
////			{
////				if (file.length() == output.length())
////				{
////					// check if it is still the same if the number of bytes are equal.
////					try (FileReader reader = new FileReader(file))
////					{
////						char[] buf = new char[(int)file.length()];
////						reader.read(buf);
////						String old = new String(buf);
////						if (old.equals(output))
////						{
////							System.out.println("not updating remote content because the file is the same as the old value " + file);
////							continue;
////						}
////					}
////				}
////			}
//			}
		}

		@Override
		public void writeAggregatedOutput(boolean ngOnly) throws IOException
		{
			// TODO Auto-generated method stub
		}

	}

	protected static interface IInfoKeeper
	{
		public void addInfoAboutWebObjectsInAPackage(String content);

		public void addInfoAboutProcessedPackage(String content);
	}

	protected static class InfoFromNGPackagesGenerator extends NGPackageInfoGenerator
	{

		protected final IInfoKeeper registerNewInfo;

		private final Template packageTemplate;
		private final Template webObjectTemplate;
		private final Template methodTemplate;
		private final Template propertyTemplate;
		private final Template typeTemplate;

		public InfoFromNGPackagesGenerator(Configuration cfg, IInfoKeeper registerNewInfo,
			String packageTemplateFilename, String webObjectTemplateFilename, String methodTemplateFilename,
			String propertyTemplateFilename, String typeTemplateTemplateFilename)
			throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException
		{
			this.registerNewInfo = registerNewInfo;

			// webObject means NG component or NG service
			this.packageTemplate = cfg.getTemplate(packageTemplateFilename);
			this.webObjectTemplate = cfg.getTemplate(webObjectTemplateFilename);
			this.methodTemplate = cfg.getTemplate(methodTemplateFilename);
			this.propertyTemplate = cfg.getTemplate(propertyTemplateFilename);
			this.typeTemplate = cfg.getTemplate(typeTemplateTemplateFilename);
		}

		@Override
		public void generateComponentOrServiceInfo(Map<String, Object> root, File userDir, String displayName, String categoryName, boolean service,
			String deprecationString, String replacementInCaseOfDeprecation)
			throws TemplateException, IOException
		{
			super.generateComponentOrServiceInfo(root, userDir, displayName, categoryName, service, deprecationString, replacementInCaseOfDeprecation);

			if (deprecationString != null || replacementInCaseOfDeprecation != null)
			{
				StringBuilder deprecationMessage = new StringBuilder("@deprecated");
				if (deprecationString != null && !deprecationString.equals("true"))
				{
					deprecationMessage.append(' ').append(deprecationString);
					if (replacementInCaseOfDeprecation != null) deprecationMessage.append("\n          ");
				}
				if (replacementInCaseOfDeprecation != null)
					deprecationMessage.append(" it should be replaced with '").append(replacementInCaseOfDeprecation).append("'.");
				root.put("deprecationMessage", deprecationMessage);
			}
			StringWriter out = new StringWriter();
			webObjectTemplate.process(root, out);

			registerNewInfo.addInfoAboutWebObjectsInAPackage(out.toString());

			if (root.get("api") != null && ((Map<String, Function>)root.get("api")).size() > 0) generateMethods(root, "api", "API method");
			if (root.get("events") != null && ((Map<String, Function>)root.get("events")).size() > 0) generateMethods(root, "events", "event handler");
			if (root.get("properties") != null && ((Map<String, Property>)root.get("properties")).size() > 0) generateProperties(root);
			if (root.get("types") != null && ((Map<String, Map<String, Property>>)root.get("types")).size() > 0) generateCustomTypes(root);
		}

		protected void generateCustomTypes(Map<String, Object> root) throws TemplateException, IOException
		{
			StringWriter out;
			@SuppressWarnings("unchecked")
			Map<String, Map<String, Property>> types = (Map<String, Map<String, Property>>)root.get("types");
			if (types != null)
			{
				for (Entry<String, Map<String, Property>> type : types.entrySet())
				{
					root.put("typeName", type.getKey());

					// String name, String type, String defaultValue, String doc
					out = new StringWriter();
					typeTemplate.process(root, out);

					registerNewInfo.addInfoAboutWebObjectsInAPackage(out.toString());
				}
				root.remove("typeName");
			}
		}

		protected void generateProperties(Map<String, Object> root) throws TemplateException, IOException
		{
			StringWriter out;
			@SuppressWarnings("unchecked")
			Map<String, Property> properties = (Map<String, Property>)root.get("properties");
			if (properties != null)
			{
				for (Entry<String, Property> property : properties.entrySet())
				{
					root.put("propertyName", property.getKey());

					// String name, String type, String defaultValue, String doc
					out = new StringWriter();
					propertyTemplate.process(root, out);

					registerNewInfo.addInfoAboutWebObjectsInAPackage(out.toString());
				}
				root.remove("propertyName");
			}
		}

		protected void generateMethods(Map<String, Object> root, String rootMapKey, String methodTypeInOutput) throws TemplateException, IOException
		{
			StringWriter out;
			@SuppressWarnings("unchecked")
			Map<String, Function> methods = (Map<String, Function>)root.get(rootMapKey);
			if (methods != null)
			{
				root.put("methodType", methodTypeInOutput);
				root.put("methodMapName", rootMapKey);
				for (Entry<String, Function> method : methods.entrySet())
				{
					root.put("methodName", method.getKey());

					// String name, List<Parameter> parameters, String returnValue, String doc
					out = new StringWriter();
					methodTemplate.process(root, out);

					registerNewInfo.addInfoAboutWebObjectsInAPackage(out.toString());
				}
				root.remove("methodName");
				root.remove("methodType");
			}
		}

		@Override
		public void generateNGPackageInfo(String packageName, String packageDisplayName, String packageDescription, String packageType,
			Map<String, Object> root)
			throws TemplateException, IOException
		{
			StringWriter out = new StringWriter();
			root.put("packageName", packageName);
			root.put("packageDisplayName", packageDisplayName);
			root.put("packageDescription", packageDescription);
			root.put("packageType", packageType);
			root.put("referenceDate", DATE_OF_REFERENCE_FORMATTED);

			// TODO this might be too much (list of all comps/services in the package) info for pinecone
			// but it might be useful for fine tuning
			root.put("allWebObjectsOfCurrentPackage", allWebObjectsOfCurrentPackage);

			packageTemplate.process(root, out);
			registerNewInfo.addInfoAboutProcessedPackage(out.toString());
		}

		@Override
		public void currentPackageWasProcessed()
		{
			super.currentPackageWasProcessed();
		}

		@Override
		public boolean shouldTurnAPIDocsIntoMarkdown()
		{
			return false;
		}

		@Override
		public boolean shouldSetJSDocGeneratedFromSpecEvenIfThereIsNoDescriptionInIt()
		{
			return true;
		}

	}

}
