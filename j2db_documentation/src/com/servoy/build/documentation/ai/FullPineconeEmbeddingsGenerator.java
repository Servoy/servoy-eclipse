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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
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
public class FullPineconeEmbeddingsGenerator
{

	private static final String STORED_PINECONE_STRING_TO_EMBEDDING_FILENAME = "stored_pinecone_str_to_embedding.sobj";
	public static final String STORED_PINECONE_ID_TO_STRING_FILENAME = "stored_pinecone_id_to_str.sobj";

	public static final String PINECONE_METADATA_CLIENT_TAG_NG_ONLY = "ng_only";

	// 2 maps that are stored to disk/serialized
	private static HashMap<String, List<Float>> stringForAIToVectorEmbedding; // used in order to not ask OpenAI for embedding vectors multiple times for the same string
	private static List<PineconeItem> pineconeItemsToUpsert = new ArrayList<>(1024);

	/**
	 * It will generate and upsert all the needed pinecone embeddings related to Servoy (at least for APIs).
	 * These are things that are injected afterwards based on a similarity search into the chat prompt for the GPT-x LLM model to have more info when generating an answer.
	 *
	 * @param args 0 - uri to "servoydoc_jslib.xml"
	 *             1 - uri to "servoydoc.xml"
	 *             2 - uri to "servoydoc_design.xml"
	 *             3 - uri to the plugin dir of an installed application server (for java plugins)
	 *             4 - path to the text file that contains on each line one location of one component/service/layout (ng) package dir - to generate the info for
	 *             5 - boolean "reEmbedAllStrings" if this is set to true (defaults to false), it will ask the OpenAI (ada) model to re-embed all strings before
	 *                 upsertion; it will not reuse any previously generated embeddings even if the string is identical to what we had previously. This is useful
	 *                 for when the LLM model that we use to generate the embeddings changes.
	 * @throws TemplateException
	 * @throws JSONException
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, URISyntaxException, JSONException, TemplateException
	{
		String jsLibURI = args[0];
		String servoyDocURI = args[1];
		String designDocURI = args[2];
		String pluginDirURI = args[3];
		String ngPackagesFileLocationsURI = args[4];

		boolean reEmbedAllStrings = false; // normally Strings that were already embedded by openAI's ada model don't need to be re-encoded each run
		if (args.length > 4) reEmbedAllStrings = Boolean.getBoolean(args[5]);

		System.out.println("Vectorized (to embeddings) file location is: " + new File(STORED_PINECONE_STRING_TO_EMBEDDING_FILENAME).getAbsolutePath());
		System.out.println("IDs (from pinecone) to initial strings (that were turned into embeddings) file location is: " +
			new File(STORED_PINECONE_ID_TO_STRING_FILENAME).getAbsolutePath());
		System.out.println(reEmbedAllStrings ? "\nAs requested: all strings will be re-embedded using OpenAI's 'ada' model before being upserted to Pinecone."
			: "\nExisting embeddings (for identical source strings) will be reused.");

		if (reEmbedAllStrings) stringForAIToVectorEmbedding = new HashMap<>(0);
		else readAlreadyEmbeddedStrings();

		List<String> newStringForAIToVectorEmbedding = new ArrayList<>(500);
		Consumer<String> registerNewEmbedding = (newStringToEmbed) -> {
			newStringForAIToVectorEmbedding.add(newStringToEmbed);
		};

		Configuration cfg;

		cfg = new Configuration(Configuration.VERSION_2_3_31);
		cfg.setTemplateLoader(new ClassTemplateLoader(FullPineconeEmbeddingsGenerator.class, "template"));
		cfg.setDefaultEncoding("UTF-8");

		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setLogTemplateExceptions(false);
		cfg.setWrapUncheckedExceptions(true);
		cfg.setFallbackOnNullLoopVariable(false);
//		ConfluenceGenerator.fillStaticParents(returnTypesToParentName);

		// Servoy core and java plug-in -> info source text (to be embedded)
		CoreAndJavaPluginsInfoGenerator.initTemplates(cfg);
		// FIXME uncomment the following line
//		MarkdownGenerator.generateCoreAndPluginDocs(jsLibURI, servoyDocURI, designDocURI, pluginDirURI, new PineconeInfoFromXMLGenerator(registerNewEmbedding));

		// ng package dirs listed in text file ngPackagesFileLocationsURI -> info source text (to be embedded)
		List<String> ngPackageDirsToScan = Files.readAllLines(Paths.get(ngPackagesFileLocationsURI));
		SpecMarkdownGenerator.generateNGComponentOrServicePackageContentForDir(true, ngPackageDirsToScan.toArray(new String[ngPackageDirsToScan.size()]),
			new PineconeInfoFromNGPackagesGenerator(cfg, registerNewEmbedding));

		// ask OpenAI ada model to create embeddings for all/unknown source texts and reuse the ones that can be reused if we are allowed to by args
		makeTheNeededEmbeddings(newStringForAIToVectorEmbedding);

		// now clear the pinecone embedding vector index and upsert the new vectors for future similarity searches
		upsertEmbeddingsToPineCone();

		// write back the two files that we will need later to identify source text based on IDs and to not re-create embeddings when it is not needed
		writeAlreadyEmbeddedStrings();
	}

	protected static void makeTheNeededEmbeddings(List<String> newStringForAIToVectorEmbedding)
	{
		// FIXME implement
		System.out.println("\n\n# of strings to be embedded if needed & upserted: " + newStringForAIToVectorEmbedding.size() + "\n\n");
		newStringForAIToVectorEmbedding.stream().map(a -> a + "\n").forEach(System.out::println);
	}

	protected static void upsertEmbeddingsToPineCone()
	{
		// FIXME implement

	}

	private static void writeAlreadyEmbeddedStrings() throws IOException
	{
		// store the strings that we have embeddings for in the STORED_PINECONE_STRING_TO_EMBEDDING_FILENAME file
		// these are useful next time we want to upsert if most of the strings remain the same (we save lots of OpenAI embed queries)
		if (stringForAIToVectorEmbedding != null)
			try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(STORED_PINECONE_STRING_TO_EMBEDDING_FILENAME)))
		{
			out.writeObject(stringForAIToVectorEmbedding);
		}

		// store the id's that were upserted to pinecone to their string mapping in the STORED_PINECONE_ID_TO_STRING_FILENAME file
		// this is used at developer runtime to translate ids returned by pinecone similarity searches into actual strings to feed into the chat
		HashMap<Integer, String> pineconeIDToItemText = new HashMap<>(pineconeItemsToUpsert.size());
		pineconeItemsToUpsert.forEach(item -> pineconeIDToItemText.put(Integer.valueOf(item.getId()), item.getText()));

		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(STORED_PINECONE_ID_TO_STRING_FILENAME)))
		{
			out.writeObject(pineconeIDToItemText);
		}
	}

	@SuppressWarnings("unchecked")
	private static void readAlreadyEmbeddedStrings() throws IOException, ClassNotFoundException
	{
		// we stored the strings that we had embeddings for last time in the "STORED_PINECONE_STRING_TO_EMBEDDING_FILENAME" file; read that in
		File f = new File(STORED_PINECONE_STRING_TO_EMBEDDING_FILENAME);
		if (f.exists())
		{
			try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f)))
			{
				stringForAIToVectorEmbedding = (HashMap<String, List<Float>>)in.readObject();
				System.out
					.println("Read from disk a number of " + stringForAIToVectorEmbedding.size() + " strings for which we already have generated embeddings.");
			}
		}
		else
		{
			System.out.println("No previously vectorized (to embeddings) strings found; file '" +
				new File(STORED_PINECONE_STRING_TO_EMBEDDING_FILENAME).getAbsolutePath() + "' does not exist.");
		}
	}

	private static class PineconeInfoFromXMLGenerator implements IDocFromXMLGenerator
	{

		private final Consumer<String> registerNewEmbedding;

		public PineconeInfoFromXMLGenerator(Consumer<String> registerNewEmbedding)
		{
			this.registerNewEmbedding = registerNewEmbedding;
		}

		public void generateDocsFromXML(DocumentationManager manager, String path, boolean ngOnly)
			throws MalformedURLException, ClassNotFoundException, IOException
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

	}

	private static class PineconeInfoFromNGPackagesGenerator implements INGPackageInfoGenerator
	{

		private final Consumer<String> registerNewEmbedding;

		private final Template packageTemplate;
		private final Template webObjectTemplate;
		private final Template methodTemplate;
		private final Template propertyTemplate;
		private final Template deprecatedWebObjectTemplate;
		private final Template typeTemplate;

		private final List<Map<String, String>> allWebObjectsOfCurrentPackage = new ArrayList<>(10);


		public PineconeInfoFromNGPackagesGenerator(Configuration cfg, Consumer<String> registerNewEmbedding)
			throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException
		{
			this.registerNewEmbedding = registerNewEmbedding;

			// webObject means NG component or NG service
			this.packageTemplate = cfg.getTemplate("pinecone_ng_package_template.md");
			this.webObjectTemplate = cfg.getTemplate("pinecone_ng_webobject_template.md");
			this.methodTemplate = cfg.getTemplate("pinecone_ng_webobject_method_template.md");
			this.propertyTemplate = cfg.getTemplate("pinecone_ng_webobject_property_template.md");
			this.deprecatedWebObjectTemplate = cfg.getTemplate("pinecone_ng_webobject_deprecated_template.md");
			this.typeTemplate = cfg.getTemplate("pinecone_ng_webobject_type_template.md");
		}

		@Override
		public void generateComponentOrServiceInfo(Map<String, Object> root, File userDir, String displayName, String categoryName, boolean service)
			throws TemplateException, IOException
		{
			allWebObjectsOfCurrentPackage.add(Map.of("name", (String)root.get("componentname"), "internalName", (String)root.get("componentinternalname")));

			root.put("category_name", categoryName);
			StringWriter out = new StringWriter();
			webObjectTemplate.process(root, out);

			registerNewEmbedding.accept(out.toString());

			@SuppressWarnings("unchecked")
			Map<String, Function> apis = (Map<String, Function>)root.get("api");
			if (apis != null)
			{
				for (Entry<String, Function> api : apis.entrySet())
				{
					root.put("apiName", api.getKey());

					// String name, List<Parameter> parameters, String returnValue, String doc
					out = new StringWriter();
					methodTemplate.process(root, out);

					registerNewEmbedding.accept(out.toString());
				}
				root.remove("apiName");
			}
			// FIXME fully implement it!
//
//			File file = new File(userDir,
//				(service ? "service/" : "components/") + categoryName.trim().replace(' ', '-').replace("&", "and").toLowerCase() + "/" +
//					displayName.trim().replace(' ', '-').toLowerCase() +
//					".md");
//			// TODO Auto-generated method stub
//			try
//			{
//				file.getParentFile().mkdirs();
//				FileWriter out = new FileWriter(file, Charset.forName("UTF-8"));
//				temp.process(root, out);
//			}
//			catch (TemplateException | IOException e)
//			{
//				e.printStackTrace();
//			}
		}

		@Override
		public void generateNGPackageInfo(String packageName, String packageDisplayName, String packageDescription, String packageType)
			throws TemplateException, IOException
		{
			StringWriter out = new StringWriter();
			HashMap<String, Object> root = new HashMap<>();
			root.put("packageName", packageName);
			root.put("packageDisplayName", packageDisplayName);
			root.put("packageDescription", packageDescription);
			root.put("packageType", packageType);

			// TODO this might be too much (list of all comps/services in the package) info for pinecone
			// but it might be useful for fine tuning
			root.put("allWebObjectsOfCurrentPackage", allWebObjectsOfCurrentPackage);

			packageTemplate.process(root, out);
			registerNewEmbedding.accept(out.toString());
		}

		@Override
		public void currentPackageWasProcessed()
		{
			allWebObjectsOfCurrentPackage.clear();
		}

		@Override
		public boolean shouldTurnAPIDocsIntoMarkdown()
		{
			return false;
		}

	}

}
