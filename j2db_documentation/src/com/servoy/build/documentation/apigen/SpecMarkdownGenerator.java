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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Comment;
import org.mozilla.javascript.ast.FunctionNode;
import org.sablo.specification.Package;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.websocket.impl.ClientService;

import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

import freemarker.cache.ClassTemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateNotFoundException;

/**
 * @author jcompagner
 *
 */
public class SpecMarkdownGenerator
{

	private static Configuration cfg;
	private static Template temp;

	public static void main(String[] args)
		throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, JSONException, TemplateException
	{
		if (args.length == 0)
		{
			System.out.println("give a directory of a component/service package where specs can be found");
			return;
		}
		cfg = new Configuration(Configuration.VERSION_2_3_31);
		cfg.setTemplateLoader(new ClassTemplateLoader(MarkdownGenerator.class, "template"));
		cfg.setDefaultEncoding("UTF-8");

		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setLogTemplateExceptions(false);
		cfg.setWrapUncheckedExceptions(true);
		cfg.setFallbackOnNullLoopVariable(false);
//		ConfluenceGenerator.fillStaticParents(returnTypesToParentName);


		temp = cfg.getTemplate("component_template.md");

		boolean componentGeneration = true;
		String[] webPackageDirs = args;
		if ("false".equals(args[args.length - 1]))
		{
			componentGeneration = false;
			webPackageDirs = Arrays.copyOf(args, args.length - 1);
		}

		generateNGComponentOrServicePackageContentForDir(componentGeneration, webPackageDirs, new NGPackageMarkdownDocGenerator());
	}

	public static void generateNGComponentOrServicePackageContentForDir(boolean componentGeneration, String[] webPackageDirs,
		INGPackageInfoGenerator docGenerator) throws JSONException, TemplateException, IOException
	{
		System.err.println("Generating NG package content");
		for (String dirname : webPackageDirs)
		{
			File dir = new File(dirname);
			if (!dir.exists())
			{
				throw new RuntimeException("Given NG package dir " + dirname + " doesn't exist!");
			}
			else
			{
				System.err.println("  - NG package dir " + dirname);
			}

			// get package name / display name
			String packageName;
			String packageDisplayName;
			String packageType;
			try (InputStream is = FileUtils.openInputStream(new File(dir, "META-INF/MANIFEST.MF")))
			{
				Manifest mf = new Manifest(is);
				packageName = Package.getPackageName(mf);
				packageDisplayName = Package.getPackageDisplayname(mf);
				packageType = Package.getPackageType(mf);
			}
			catch (IOException e)
			{
				throw new RuntimeException("Cannot access/read META-INF/MANIFEST.MF of NG package dir " + dirname + "!", e);
			}

			Collection<File> specFiles = FileUtils.listFiles(dir, new AbstractFileFilter()
			{
				@Override
				public boolean accept(File file)
				{
					return file.getName().toLowerCase().endsWith(".spec");
				}

			}, new AbstractFileFilter()
			{
				@Override
				public boolean accept(File file)
				{
					String name = file.getName();
					return !(name.equals("node_modules") || name.equals(".angular") || name.equals(".git") || name.equals("dist"));
				}

			});
			specFiles.stream().map(file -> {
				try
				{
					return new Pair<File, String>(file, FileUtils.readFileToString(file, Charset.forName("UTF8")));
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				return null;
			}).filter(Objects::nonNull).map(contents -> {
				try
				{
					return new SpecMarkdownGenerator(packageName, packageDisplayName, packageType, new JSONObject(contents.getRight()), contents.getLeft(),
						componentGeneration,
						docGenerator);
				}
				catch (RuntimeException e)
				{
					System.err.println(contents);
					throw e;
				}
			}).forEach(generator -> {
				try
				{
					generator.save();
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			});

			File packageInfoFile = new File(dir, "webpackage.json");
			while (!packageInfoFile.exists() && packageInfoFile.getParentFile().getParentFile() != null)
				packageInfoFile = new File(packageInfoFile.getParentFile().getParentFile(), "webpackage.json");

			if (packageInfoFile.exists())
			{
				docGenerator.generateNGPackageInfo(packageName, packageDisplayName,
					new JSONObject(Utils.getTXTFileContent(packageInfoFile)).optString("description", null),
					packageType);
			}
			else System.err.println("    * cannot find the package's webpackage.json; skipping information about the package...");

			docGenerator.currentPackageWasProcessed();
		}
	}

	private final Map<String, Object> root;
	private final JSONObject jsonObject;
	private final Map<String, String> apiDoc = new HashMap<>();
	private boolean service;
	private final INGPackageInfoGenerator docGenerator;

	public SpecMarkdownGenerator(String packageName, String packageDisplayName, String packageType,
		JSONObject jsonObject, File specFile, boolean componentGeneration,
		INGPackageInfoGenerator docGenerator)
	{
		this.jsonObject = jsonObject;
		this.docGenerator = docGenerator;

		root = new HashMap<>();
		String docFileName = jsonObject.optString("doc", null);
		if (docFileName != null)
		{
			File parent = specFile.getParentFile();
			File docFile = new File(parent, docFileName);
			while (!docFile.exists())
			{
				parent = parent.getParentFile();
				if (parent == null) break;
				docFile = new File(parent, docFileName);
			}
			if (!docFile.exists())
			{
				docFile = new File(specFile.getParentFile(), docFile.getName());
			}
			if (docFile.exists())
			{
				try
				{
					String docContents = FileUtils.readFileToString(docFile, Charset.forName("UTF8"));
					CompilerEnvirons env = new CompilerEnvirons();
					env.setRecordingComments(true);
					env.setRecordingLocalJsDocComments(true);
					org.mozilla.javascript.Parser parser = new org.mozilla.javascript.Parser(env);
					AstRoot parse = parser.parse(docContents, "", 0);
					SortedSet<Comment> comments = parse.getComments();
					if (comments != null) for (Comment comment : comments)
					{
						if (comment.getNext() instanceof FunctionNode fn)
						{
							String name = fn.getFunctionName().toSource();
							apiDoc.put(name, processFunctionJSDoc(comment.toSource()));
						}
					}


				}
				catch (IOException e)
				{
					throw new RuntimeException("Cannot parse docfile: " + docFileName, e);
				}
			}
			else
			{
				System.err.println("    * docfile: " + docFileName + " doesn't exist in the parent structure of the spec file " + specFile + " !");
			}
		}

		root.put("package_name", packageName);
		root.put("package_display_name", packageDisplayName);
		root.put("package_type", packageType);
		root.put("componentname", jsonObject.optString("displayName"));
		root.put("componentinternalname", jsonObject.optString("name"));
		root.put("componentname_nospace", jsonObject.optString("displayName").replace(" ", "%20"));
		root.put("instance", this);
		root.put("properties", makeMap(jsonObject.optJSONObject("model"), this::createProperty));
		root.put("events", makeMap(jsonObject.optJSONObject("handlers"), this::createFunction));
		Map<String, Object> api = makeMap(jsonObject.optJSONObject("api"), this::createFunction);
		root.put("api", api);
		root.put("types", makeTypes(jsonObject.optJSONObject("types")));
		if (IPackageReader.WEB_SERVICE.equals(packageType) && api != null && api.size() > 0)
			root.put("service_scripting_name", ClientService.convertToJSName(jsonObject.optString("name")));

		service = false;
		JSONObject ng2Config = jsonObject.optJSONObject("ng2Config");
		if (ng2Config != null)
		{
			service = ng2Config.has("serviceName");
		}
		if (componentGeneration && !service)
		{
			root.put("designtimeExtends", new Property("JSWebComponent", "JSWebComponent", null, null));
			root.put("runtimeExtends", new Property("RuntimeWebComponent", "RuntimeWebComponent", null, null));
		}
	}

	private String processFunctionJSDoc(String jsDocComment)
	{
		if (jsDocComment == null) return null;

		String doc = jsDocComment.replace("\r\n", "\n").replace("%%prefix%%", "");
		if (docGenerator.shouldTurnAPIDocsIntoMarkdown())
			doc = doc.replace("/**", "").replace("*/", "").replace(" *", "").replace("*", "");
		doc = doc.trim();
		if (docGenerator.shouldTurnAPIDocsIntoMarkdown()) doc = "```\n" + doc + "\n```\n";

		return doc;
	}

	private Record createProperty(String name, JSONObject specEntry)
	{
		String dflt = specEntry.optString("default", null);
		String type = specEntry.optString("type", "");
		JSONObject fullType = specEntry.optJSONObject("type");
		if (fullType != null)
		{
			type = fullType.optString("type", "");
		}
		String doc = null;
		JSONObject optJSONObject = specEntry.optJSONObject("tags");
		if (optJSONObject != null)
		{
			doc = optJSONObject.optString("doc", null);
		}

		return new Property(name, type, dflt, doc);
	}

	private Record createFunction(String name, JSONObject specEntry)
	{
		String returnType = specEntry.optString("returns", null);
		JSONArray parameters = specEntry.optJSONArray("parameters");
		List<Parameter> params = createParameters(parameters);
		JSONObject fullReturnType = specEntry.optJSONObject("returns");
		if (fullReturnType != null)
		{
			returnType = fullReturnType.optString("type", "");
		}

		String jsDocEquivalent = apiDoc.get(name);
		if (jsDocEquivalent == null)
		{
			jsDocEquivalent = processFunctionJSDoc(createFunctionDocFromSpec(specEntry, params, returnType, fullReturnType,
				docGenerator.shouldSetJSDocGeneratedFromSpecEvenIfThereIsNoDescriptionInIt()));
		}
		return new Function(name, params, returnType, jsDocEquivalent);
	}

	/**
	 * @return a Pair who's left value is "someDocEntryFromSpecWasWritten" and right value is the generated JSDoc string.
	 */
	private String createFunctionDocFromSpec(JSONObject specEntry, List<Parameter> params, String returnType, JSONObject fullReturnType,
		boolean shouldSetJSDocGeneratedFromSpecEvenIfThereIsNoDescriptionInIt)
	{
		boolean someDocEntryFromSpecWasWritten = false;
		boolean somethingWasWritten = false;
		StringBuilder generatedJSDocCommentFromSpec = new StringBuilder("/**");

		String functionDescription = specEntry.optString("doc", null);
		if (functionDescription != null)
		{
			someDocEntryFromSpecWasWritten = true;
			somethingWasWritten = true;
			generatedJSDocCommentFromSpec.append("\n * ").append(functionDescription.replaceAll("\n", "\n * "));
		}

		for (Parameter param : params)
		{
			if (!shouldSetJSDocGeneratedFromSpecEvenIfThereIsNoDescriptionInIt && (param.doc() == null || param.doc().length() == 0)) continue; // in markdown generator/docs.servoy.com, parameters are written separately anyway so if we don't have more info, don't write more info

			somethingWasWritten = true;
			generatedJSDocCommentFromSpec.append("\n * @param ").append(param.name());
			if (param.type() != null) generatedJSDocCommentFromSpec.append(" [").append(param.type()).append(']');
			if (param.optional()) generatedJSDocCommentFromSpec.append(" (optional)");
			if (param.doc() != null && param.doc().length() > 0)
			{
				someDocEntryFromSpecWasWritten = true;
				generatedJSDocCommentFromSpec.append(' ').append(param.doc().replaceAll("\n", "\n *        "));
			}
		}

		if (returnType != null)
		{
			if (shouldSetJSDocGeneratedFromSpecEvenIfThereIsNoDescriptionInIt || (fullReturnType != null && fullReturnType.optString("doc", null) != null))
			{

				somethingWasWritten = true;
				generatedJSDocCommentFromSpec.append("\n * @return {").append(returnType).append('}');
				if (fullReturnType != null && fullReturnType.optString("doc", null) != null)
				{
					someDocEntryFromSpecWasWritten = true;
					generatedJSDocCommentFromSpec.append(fullReturnType.optString("doc"));
				}
			} // else, in markdown generator/docs.servoy.com, parameters are written separately anyway so if we don't have more info, don't write more info
		}

		generatedJSDocCommentFromSpec.append("\n */");
		return somethingWasWritten ? (someDocEntryFromSpecWasWritten || shouldSetJSDocGeneratedFromSpecEvenIfThereIsNoDescriptionInIt
			? generatedJSDocCommentFromSpec.toString() : null) : null;
	}

	private List<Parameter> createParameters(JSONArray parameters)
	{
		if (parameters != null)
		{
			List<Parameter> params = new ArrayList<>();
			Iterator<Object> keys = parameters.iterator();
			while (keys.hasNext())
			{
				JSONObject param = (JSONObject)keys.next();
				String paramName = param.optString("name", "");
				String type = param.optString("type", "");
				JSONObject fullType = param.optJSONObject("type");
				if (fullType != null)
				{
					type = fullType.optString("type", "");
				}
				boolean optional = param.optBoolean("optional", false);
				String doc = param.optString("doc", "");
				params.add(new Parameter(paramName, type, doc, optional));
			}
			return params;
		}
		return Collections.emptyList();
	}

	private Object makeTypes(JSONObject types)
	{
		if (types != null)
		{
			Map<String, Object> map = new TreeMap<>();
			Iterator<String> keys = types.keys();
			while (keys.hasNext())
			{
				String type = keys.next();
				map.put(type, makeMap(types.optJSONObject(type), this::createProperty));
			}
			return map.size() > 0 ? map : null;
		}
		return null;
	}

	private Map<String, Object> makeMap(JSONObject properties, BiFunction<String, JSONObject, Record> transformer)
	{
		if (properties != null)
		{
			Map<String, Object> map = new TreeMap<>();
			Iterator<String> keys = properties.keys();
			while (keys.hasNext())
			{
				String key = keys.next();
				if ("size".equals(key)) continue;
				Object value = properties.get(key);
				if (value instanceof JSONObject)
				{
					Object tags = ((JSONObject)value).opt("tags");
					if (tags instanceof JSONObject)
					{
						String scope = ((JSONObject)tags).optString("scope");
						if ("private".equals(scope)) continue;
					}
					map.put(key, transformer.apply(key, (JSONObject)value));
				}
			}
			return map;
		}
		return null;
	}

	public String getReturnTypePath(Record rcd)
	{
		String type = null;
		if (rcd instanceof Property)
		{
			type = ((Property)rcd).type();
		}
		else if (rcd instanceof Parameter)
		{
			type = ((Parameter)rcd).type();
		}
		else if (rcd instanceof Function)
		{
			type = ((Function)rcd).returnValue();
		}
		if (type != null)
		{
			if (type.endsWith("[]")) type = type.substring(0, type.length() - 2);

			Map<String, Record> types = (Map<String, Record>)root.get("types");
			if (types != null && types.get(type) != null) return "#" + type;
			return switch (type.toLowerCase())
			{
				case "object" -> "../../../servoycore/dev-api/js-lib/object.md";
				case "string" -> "../../../servoycore/dev-api/js-lib/string.md";
				case "boolean" -> "../../../servoycore/dev-api/js-lib/boolean.md";
				case "int" -> "../../../servoycore/dev-api/js-lib/number.md";
				case "long" -> "../../../servoycore/dev-api/js-lib/number.md";
				case "double" -> "../../../servoycore/dev-api/js-lib/number.md";
				case "float" -> "../../../servoycore/dev-api/js-lib/number.md";
				case "json" -> "../../../servoycore/dev-api/js-lib/json.md";
				case "dimension" -> "../../../servoycore/dev-api/js-lib/dimension.md";
				case "point" -> "../../../servoycore/dev-api/js-lib/point.md";
				case "date" -> "../../../servoycore/dev-api/js-lib/date.md";
				case "jsevent" -> "../../../servoycore/dev-api/application/jsevent.md";
				case "jsupload" -> "../../../servoycore/dev-api/application/jsupload.md";
				case "tagstring" -> "../../../servoycore/dev-api/property\\_types.md#tagstring";
				case "titlestring" -> "../../../servoycore/dev-api/property\\_types.md#titlestring";
				case "styleclass" -> "../../../servoycore/dev-api/property\\_types.md#styleclass";
				case "protected" -> "../../../servoycore/dev-api/property\\_types.md#protected";
				case "enabled" -> "../../../servoycore/dev-api/property\\_types.md#protected";
				case "readonly" -> "../../../servoycore/dev-api/property\\_types.md#protected";
				case "variant" -> "../../../servoycore/dev-api/property\\_types.md#variant";
				case "visible" -> "../../../servoycore/dev-api/property\\_types.md#visible";
				case "tabseq" -> "../../../servoycore/dev-api/property\\_types.md#tabseq";
				case "format" -> "../../../servoycore/dev-api/property\\_types.md#format";
				case "color" -> "../../../servoycore/dev-api/property\\_types.md#color";
				case "map" -> "../../../servoycore/dev-api/property\\_types.md#map";
				case "scrollbars" -> "../../../servoycore/dev-api/property\\_types.md#scrollbars";
				case "dataprovider" -> "../../../servoycore/dev-api/property\\_types.md#dataprovider";
				case "${dataprovidertype}" -> "../../../servoycore/dev-api/property\\_types.md#dataprovider";
				case "relation" -> "../../../servoycore/dev-api/property\\_types.md#relation";
				case "form" -> "../../../servoycore/dev-api/property\\_types.md#form";
				case "formscope" -> "../../../servoycore/dev-api/property\\_types.md#form";
				case "formcomponent" -> "../../../servoycore/dev-api/property\\_types.md#formcomponent";
				case "record" -> "../../../servoycore/dev-api/property\\_types.md#record";
				case "foundset" -> "../../../servoycore/dev-api/property\\_types.md#foundset";
				case "foundsetref" -> "../../../servoycore/dev-api/property\\_types.md#foundsetref";
				case "dataset" -> "../../../servoycore/dev-api/property\\_types.md#dataset";
				case "function" -> "../../../servoycore/dev-api/property\\_types.md#function";
				case "clientfunction" -> "../../../servoycore/dev-api/property\\_types.md#clientfunction";
				case "media" -> "../../../servoycore/dev-api/property\\_types.md#media";
				case "valuelist" -> "../../../servoycore/dev-api/property\\_types.md#valuelist";
				case "labelfor" -> "../../../servoycore/dev-api/property\\_types.md#labelfor";

				case "runtimewebcomponent" -> "../../../servoycore/dev-api/forms/runtimeform/elements/runtimewebcomponent.md";
				case "jswebcomponent" -> "../../../servoycore/dev-api/solutionmodel/jswebcomponent.md";

				default ->
				{
					System.err.println("    * cannot map type '" + type + "' to a path!");
					yield "";
				}
			};
		}
		return "";
	}

	private void save() throws TemplateException, IOException
	{
		if (jsonObject.optString("deprecated", null) != null)
		{
			System.err.println("    * skipping " + jsonObject.optString("displayName", "component") + " because it is deprecated");
			return;
		}
		File userDir = new File(System.getProperty("user.dir"));
		String displayName = jsonObject.optString("displayName", "component");
		String categoryName = jsonObject.optString("categoryName", null);

		docGenerator.generateComponentOrServiceInfo(root, userDir, displayName, categoryName, service);
	}

	public record Function(String name, List<Parameter> parameters, String returnValue, String doc)
	{
	}

	public record Parameter(String name, String type, String doc, boolean optional)
	{
	}

	public record Property(String name, String type, String defaultValue, String doc)
	{
	}

	public static class NGPackageMarkdownDocGenerator implements INGPackageInfoGenerator
	{

		@Override
		public void generateComponentOrServiceInfo(Map<String, Object> root, File userDir, String displayName, String categoryNameStrict, boolean service)
		{
			String categoryName = categoryNameStrict;
			if (categoryName == null)
			{
				categoryName = (String)root.get("package_display_name");
			}

			File file = new File(userDir,
				(service ? "service/" : "components/") + categoryName.trim().replace(' ', '-').replace("&", "and").toLowerCase() + "/" +
					displayName.trim().replace(' ', '-').toLowerCase() +
					".md");
			// TODO Auto-generated method stub
			try
			{
				file.getParentFile().mkdirs();
				FileWriter out = new FileWriter(file, Charset.forName("UTF-8"));
				temp.process(root, out);
			}
			catch (TemplateException | IOException e)
			{
				e.printStackTrace();
			}
		}

		@Override
		public void generateNGPackageInfo(String packageName, String packageDisplayName, String packageDescription, String packageType)
		{
			// we don't write the package description currently in markdown
		}

		@Override
		public void currentPackageWasProcessed()
		{
			// nothing to do here currently
		}

		@Override
		public boolean shouldTurnAPIDocsIntoMarkdown()
		{
			return true;
		}

		@Override
		public boolean shouldSetJSDocGeneratedFromSpecEvenIfThereIsNoDescriptionInIt()
		{
			return false;
		}

	}

}
