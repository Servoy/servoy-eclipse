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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
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
	private final static String SERVICES_DIR_IN_DOCS = "browser-plugins/";
	private final static String COMPONENTS_DIR_IN_DOCS = "ui-components/";

	private final static String PATH_TO_NG_SERVICE_DOCS = "reference/servoyextensions/" + SERVICES_DIR_IN_DOCS;
	private final static String PATH_TO_NG_COMPONENT_DOCS = "reference/servoyextensions/" + COMPONENTS_DIR_IN_DOCS; // /someCategory/someComp.md

	private final static String PATH_TO_NG_SERVICE_PACKAGE_DOCS = "reference/servoyextensions/packages/services/";
	private final static String PATH_TO_NG_COMPONENT_PACKAGE_DOCS = "reference/servoyextensions/packages/components/";

	private final static String WEB_OBJECT_OVERVIEW_KEY = null;

	private static Configuration cfg;
	private static Template componentTemplate, packageTemplate;

	private static File servicesRootDir;
	private static File componentsRootDir;
	private static File servicePackagesDir;
	private static File componentPackagesDir;

	/**
	 * It will generate the reference docs markdown for NG components and services.
	 *
	 * @param args 0 -> required; path to the "gitbook" repository
	 *             1 -> required; path to the text file that contains on each line one location of one component/service/layout (ng) package dir - to generate the info for; it can start at the dir that has the webpackage.json
	 *             2 -> optional; default: true; "generateComponentExtendsAsWell" if it should add the runtime extends and designtime extends lines in component docs
	 *             3 -> optional; default: false; try to clear/delete generated content; review changes carefully before pushing if you use this option.
	 *
	 * @throws TemplateNotFoundException
	 * @throws MalformedTemplateNameException
	 * @throws ParseException
	 * @throws IOException
	 * @throws JSONException
	 * @throws TemplateException
	 */
	public static void main(String[] args)
		throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, JSONException, TemplateException
	{
		if (args.length < 2)
		{
			System.out
				.println(
					"give at least the location of the gitbook repository and path of text file containing a list of directories of a component/service package where .specs can be found.");
			System.exit(1);
		}

		File gitBookRepoDir = new File(args[0]);
		servicesRootDir = new File(gitBookRepoDir, PATH_TO_NG_SERVICE_DOCS);
		componentsRootDir = new File(gitBookRepoDir, PATH_TO_NG_COMPONENT_DOCS);
		servicePackagesDir = new File(gitBookRepoDir, PATH_TO_NG_SERVICE_PACKAGE_DOCS);
		componentPackagesDir = new File(gitBookRepoDir, PATH_TO_NG_COMPONENT_PACKAGE_DOCS);

		if (!gitBookRepoDir.exists() || !gitBookRepoDir.isDirectory())
		{
			System.out.println("the given '" + gitBookRepoDir.getAbsolutePath() + "'gitbook repository dir (first arg) has to exist and be a directory.");
			System.exit(1);
		}

		if (args.length > 3 && "true".equals(args[3]))
		{
			String err = clearGeneratedDocsDirOnGitbookRepo(servicesRootDir, true);
			if (err == null) err = clearGeneratedDocsDirOnGitbookRepo(componentsRootDir, true);
			if (err == null) err = clearGeneratedDocsDirOnGitbookRepo(servicePackagesDir, false);
			if (err == null) err = clearGeneratedDocsDirOnGitbookRepo(componentPackagesDir, false);

			if (err != null)
			{
				System.out.println(err);
				System.exit(1);
			}
		}

		cfg = new Configuration(Configuration.VERSION_2_3_31);
		cfg.setTemplateLoader(new ClassTemplateLoader(MarkdownGenerator.class, "template"));
		cfg.setDefaultEncoding("UTF-8");

		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setLogTemplateExceptions(false);
		cfg.setWrapUncheckedExceptions(true);
		cfg.setFallbackOnNullLoopVariable(false);
//		ConfluenceGenerator.fillStaticParents(returnTypesToParentName);


		componentTemplate = cfg.getTemplate("component_template.md");
		packageTemplate = cfg.getTemplate("package_template.md");

		boolean generateComponentExtendsAsWell = true;
		// ng package dirs listed in text file ngPackagesFileLocationsURI -> info source text (to be embedded)
		List<String> ngPackageDirsToScan = Files.readAllLines(Paths.get(args[1]).normalize());
		if ("false".equals(args[2]))
		{
			generateComponentExtendsAsWell = false;
		}

		generateNGComponentOrServicePackageContentForDir(generateComponentExtendsAsWell, ngPackageDirsToScan, new NGPackageMarkdownDocGenerator(),
			new HashMap<>());

		System.out.println("\nDONE.");
	}

	private static String clearGeneratedDocsDirOnGitbookRepo(File dirWithGeneratedDocs, boolean onlySubDirs) throws IOException
	{
		if (onlySubDirs)
		{
			if (!dirWithGeneratedDocs.exists() || !dirWithGeneratedDocs.isDirectory())
			{
				return "'" + dirWithGeneratedDocs.getAbsolutePath() + "' dir has to exist and be a directory.";
			}
			// this doc dir also has some manually written .md files in it; just the subdirs are generated
			for (File d : dirWithGeneratedDocs.listFiles(f -> f.isDirectory()))
			{
				if (!d.getName().startsWith("home-")) // try to avoid deleting messy manual kept contents that are in the same parent dir
					FileUtils.deleteDirectory(d);
			}
		}
		else FileUtils.deleteDirectory(dirWithGeneratedDocs);

		return null;
	}

	public static void generateNGComponentOrServicePackageContentForDir(boolean generateComponentExtendsAsWell, List<String> webPackageDirs,
		INGPackageInfoGenerator docGenerator, Map<String, Object> globalRootEntries)
		throws JSONException, TemplateException, IOException
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
						generateComponentExtendsAsWell,
						docGenerator, globalRootEntries);
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

			// generate package info last as the generators remembered all the services/components in the package by now
			File packageInfoFile = new File(dir, "webpackage.json");
			while (!packageInfoFile.exists() && packageInfoFile.getParentFile().getParentFile() != null)
				packageInfoFile = new File(packageInfoFile.getParentFile().getParentFile(), "webpackage.json");

			if (packageInfoFile.exists())
			{
				docGenerator.generateNGPackageInfo(packageName, packageDisplayName,
					new JSONObject(Utils.getTXTFileContent(packageInfoFile)).optString("description", null),
					packageType, new HashMap<String, Object>(globalRootEntries));
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
		JSONObject jsonObject, File specFile, boolean generateComponentExtendsAsWell,
		INGPackageInfoGenerator docGenerator, Map<String, Object> globalRootEntries)
	{
		this.jsonObject = jsonObject;
		this.docGenerator = docGenerator;

		root = new HashMap<>();
		if (globalRootEntries != null) root.putAll(globalRootEntries);

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
					Comment prevComment = null;
					if (comments != null) for (Comment comment : comments)
					{
						if (comment.getNext() instanceof FunctionNode fn)
						{
							String name = fn.getFunctionName().toSource();
							apiDoc.put(name, processFunctionJSDoc(comment.toSource()));
						}
						else if (prevComment == null)
						{
							// it's the top-most comment that give a short description of the purpose of the whole component/service
							apiDoc.put(WEB_OBJECT_OVERVIEW_KEY, processMainDoc(comment.toSource()));
						}
						prevComment = comment;
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
		root.put("category_name", jsonObject.optString("categoryName", null));
		root.put("instance", this);
		root.put("overview", apiDoc.get(WEB_OBJECT_OVERVIEW_KEY));
		root.put("properties", makeMap(jsonObject.optJSONObject("model"), this::createProperty));
		root.put("events", makeMap(jsonObject.optJSONObject("handlers"), this::createFunction));
		Map<String, Object> api = makeMap(jsonObject.optJSONObject("api"), this::createFunction);
		root.put("api", api);
		root.put("types", makeTypes(jsonObject.optJSONObject("types")));

		service = false;
		JSONObject ng2Config = jsonObject.optJSONObject("ng2Config");
		if (ng2Config != null)
		{
			service = ng2Config.has("serviceName");
		}
		if (IPackageReader.WEB_SERVICE.equals(packageType))
		{
			service = true;
			if (api != null && api.size() > 0)
				root.put("service_scripting_name", ClientService.convertToJSName(jsonObject.optString("name")));
		}
		root.put("service", Boolean.valueOf(service));
		if (generateComponentExtendsAsWell && !service)
		{
			root.put("designtimeExtends", new Property("JSWebComponent", "JSWebComponent", null, null, null));
			root.put("runtimeExtends", new Property("RuntimeWebComponent", "RuntimeWebComponent", null, null, null));
		}
	}

	private String processMainDoc(String jsDocComment)
	{
		if (jsDocComment == null) return null;

		String doc = jsDocComment.replace("\r\n", "\n").replace("%%prefix%%", "");
		if (docGenerator.shouldTurnAPIDocsIntoMarkdown()) doc = stripCommentStartMiddleAndEndChars(doc);
		doc = doc.trim();

		return doc;
	}

	private static Pattern regexP1 = Pattern.compile("(?m:^\\s*/\\*\\* )"); // /**space
	private static Pattern regexP2 = Pattern.compile("(?m:^\\s*/\\*\\*)"); // /**
	private static Pattern regexP3 = Pattern.compile("(?m:^\\s*/\\* )"); // /*space
	private static Pattern regexP4 = Pattern.compile("(?m:^\\s*/\\*)"); // /*
	private static Pattern regexP5 = Pattern.compile("(?m:\\s*\\*/)"); // */
	private static Pattern regexP6 = Pattern.compile("(?m:^\\s*\\* )"); // *space
	private static Pattern regexP7 = Pattern.compile("(?m:^\\s*\\*)"); // *

	/**
	 * This only strips down some whitespace as well as start/end block comment chars. It does not look for one line comments, so //.
	 */
	private String stripCommentStartMiddleAndEndChars(String doc)
	{
		String stripped = doc;
		stripped = regexP1.matcher(stripped).replaceAll("");
		stripped = regexP2.matcher(stripped).replaceAll("");
		stripped = regexP3.matcher(stripped).replaceAll("");
		stripped = regexP4.matcher(stripped).replaceAll("");
		stripped = regexP5.matcher(stripped).replaceAll("");
		stripped = regexP6.matcher(stripped).replaceAll("");
		return regexP7.matcher(stripped).replaceAll("");
	}

	private String processFunctionJSDoc(String jsDocComment)
	{
		if (jsDocComment == null) return null;

		String doc = jsDocComment.replace("\r\n", "\n").replace("%%prefix%%", "");
		if (docGenerator.shouldTurnAPIDocsIntoMarkdown()) doc = stripCommentStartMiddleAndEndChars(doc);
		doc = doc.trim();
		if (docGenerator.shouldTurnAPIDocsIntoMarkdown()) doc = "```\n" + doc + "\n```\n";

		return doc;
	}

	private Record createProperty(String name, JSONObject specEntry)
	{
		Object d = specEntry.opt("default");
		String dflt = (d != null ? JSONObject.valueToString(d) : null);
		String type = specEntry.optString("type", "");
		String deprecationString = specEntry.optString("deprecated", null);
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

		return new Property(name, type, dflt, doc, deprecationString);
	}

	private Record createFunction(String name, JSONObject specEntry)
	{
		String returnType = specEntry.optString("returns", null);
		JSONArray parameters = specEntry.optJSONArray("parameters");
		String deprecationString = specEntry.optString("deprecated", null);
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
		return new Function(name, params, returnType, jsDocEquivalent, deprecationString);
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
				else map.put(key, transformer.apply(key, new JSONObject(new JSONStringer().object().key("type").value(value).endObject().toString())));
			}
			return map;
		}
		return null;
	}

	public String getPackagePath(String packageDisplayName)
	{
		return "../../packages/" + (service ? "services/" : "components/") + packageDisplayName.trim().replace("&", "and").replace(' ', '-').toLowerCase();
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

			@SuppressWarnings("unchecked")
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
				case "tagstring" -> "../../../../extension-dev/component\\_services/property\\_types.md#tagstring";
				case "titlestring" -> "../../../../extension-dev/component\\_services/property\\_types.md#titlestring";
				case "styleclass" -> "../../../../extension-dev/component\\_services/property\\_types.md#styleclass";
				case "protected" -> "../../../../extension-dev/component\\_services/property\\_types.md#protected";
				case "enabled" -> "../../../../extension-dev/component\\_services/property\\_types.md#protected";
				case "readonly" -> "../../../../extension-dev/component\\_services/property\\_types.md#protected";
				case "variant" -> "../../../../extension-dev/component\\_services/property\\_types.md#variant";
				case "visible" -> "../../../../extension-dev/component\\_services/property\\_types.md#visible";
				case "tabseq" -> "../../../../extension-dev/component\\_services/property\\_types.md#tabseq";
				case "format" -> "../../../../extension-dev/component\\_services/property\\_types.md#format";
				case "color" -> "../../../../extension-dev/component\\_services/property\\_types.md#color";
				case "map" -> "../../../../extension-dev/component\\_services/property\\_types.md#map";
				case "scrollbars" -> "../../../../extension-dev/component\\_services/property\\_types.md#scrollbars";
				case "dataprovider" -> "../../../../extension-dev/component\\_services/property\\_types.md#dataprovider";
				case "${dataprovidertype}" -> "../../../../extension-dev/component\\_services/property\\_types.md#dataprovider";
				case "relation" -> "../../../../extension-dev/component\\_services/property\\_types.md#relation";
				case "form" -> "../../../../extension-dev/component\\_services/property\\_types.md#form";
				case "formscope" -> "../../../../extension-dev/component\\_services/property\\_types.md#form";
				case "formcomponent" -> "../../../../extension-dev/component\\_services/property\\_types.md#formcomponent";
				case "record" -> "../../../../extension-dev/component\\_services/property\\_types.md#record";
				case "foundset" -> "../../../../extension-dev/component\\_services/property\\_types.md#foundset";
				case "foundsetref" -> "../../../../extension-dev/component\\_services/property\\_types.md#foundsetref";
				case "dataset" -> "../../../../extension-dev/component\\_services/property\\_types.md#dataset";
				case "function" -> "../../../../extension-dev/component\\_services/property\\_types.md#function";
				case "clientfunction" -> "../../../../extension-dev/component\\_services/property\\_types.md#clientfunction";
				case "media" -> "../../../../extension-dev/component\\_services/property\\_types.md#media";
				case "valuelist" -> "../../../../extension-dev/component\\_services/property\\_types.md#valuelist";
				case "labelfor" -> "../../../../extension-dev/component\\_services/property\\_types.md#labelfor";

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
		File userDir = new File(System.getProperty("user.dir"));
		String displayName = jsonObject.optString("displayName", "component");
		String categoryName = jsonObject.optString("categoryName", null);

		docGenerator.generateComponentOrServiceInfo(root, userDir, displayName, categoryName, service, jsonObject.optString("deprecated", null),
			jsonObject.optString("replacement", null));
	}

	/**
	 * @param deprecationString null if not deprecated, "true" or a deprecation explanation if deprecated...
	 */
	public record Function(String name, List<Parameter> parameters, String returnValue, String doc, String deprecationString)
	{
	}

	public record Parameter(String name, String type, String doc, boolean optional)
	{
	}

	/**
	 * @param deprecationString null if not deprecated, "true" or a deprecation explanation if deprecated...
	 */
	public record Property(String name, String type, String defaultValue, String doc, String deprecationString)
	{
	}

	public abstract static class NGPackageInfoGenerator implements INGPackageInfoGenerator
	{

		/**
		 * List of maps of component display names -> internal names. So [ { name: ..., internalName: ..., category: ... } ]
		 */
		protected final List<Map<String, String>> allWebObjectsOfCurrentPackage = new ArrayList<>(10);

		@Override
		public void generateComponentOrServiceInfo(Map<String, Object> root, File userDir, String displayName, String categoryName, boolean service,
			String deprecationString, String replacementInCaseOfDeprecation) throws TemplateException, IOException
		{
			if (!allWebObjectsOfCurrentPackage.stream().anyMatch(it -> ((String)root.get("componentname")).equals(it.get("name"))))
			{
				String parentFolderName = categoryName;
				if (parentFolderName == null)
				{
					parentFolderName = (String)root.get("package_display_name");
				}

				allWebObjectsOfCurrentPackage.add(
					Map.of("name", (String)root.get("componentname"), "internalName", (String)root.get("componentinternalname"), "parentFolderName",
						parentFolderName));
			}
		}

		@Override
		public void currentPackageWasProcessed()
		{
			allWebObjectsOfCurrentPackage.clear();
		}

	}

	public static class NGPackageMarkdownDocGenerator extends NGPackageInfoGenerator
	{

		private boolean isService;

		@Override
		public void generateComponentOrServiceInfo(Map<String, Object> root, File userDir, String displayName, String categoryNameStrict, boolean service,
			String deprecationString, String replacementInCaseOfDeprecation) throws TemplateException, IOException
		{
			isService = service;
			if (deprecationString != null || replacementInCaseOfDeprecation != null)
			{
				System.err.println("    * skipping " + (service ? "service" : "component") + " " + displayName + " because it is deprecated.");
				return;
			}

			super.generateComponentOrServiceInfo(root, userDir, displayName, categoryNameStrict, service, deprecationString, replacementInCaseOfDeprecation);

			String categoryName = categoryNameStrict;
			if (categoryName == null)
			{
				categoryName = (String)root.get("package_display_name");
			}

			File file = new File(service ? servicesRootDir : componentsRootDir,
				categoryName.trim().replace(' ', '-').replace("&", "and").toLowerCase() + "/" +
					displayName.trim().replace(' ', '-').toLowerCase() + ".md");
			try
			{
				file.getParentFile().mkdirs();
				FileWriter out = new FileWriter(file, Charset.forName("UTF-8"));
				componentTemplate.process(root, out);
			}
			catch (TemplateException | IOException e)
			{
				e.printStackTrace();
			}
		}

		@Override
		public void generateNGPackageInfo(String packageName, String packageDisplayName, String packageDescription, String packageType,
			Map<String, Object> root)
		{
			File file = new File(IPackageReader.WEB_SERVICE.equals(packageType) ? servicePackagesDir : componentPackagesDir,
				packageDisplayName.trim().replace("&", "and").replace(' ', '-').toLowerCase() + ".md");
			try
			{
				file.getParentFile().mkdirs();
				FileWriter out = new FileWriter(file, Charset.forName("UTF-8"));

				root.put("packageName", packageName);
				root.put("instance", this);
				root.put("packageDisplayName", packageDisplayName);
				root.put("packageDescription", packageDescription);
				root.put("packageType", packageType);
				root.put("allWebObjectsOfCurrentPackage", allWebObjectsOfCurrentPackage);

				packageTemplate.process(root, out);
			}
			catch (TemplateException | IOException e)
			{
				e.printStackTrace();
			}
		}

		public String getWebObjectPath(String webObjectDisplayName, String parentFolderName)
		{
			// category is the category if available for components, otherwise the package display name
			return "../../" + (isService ? SERVICES_DIR_IN_DOCS : COMPONENTS_DIR_IN_DOCS) +
				parentFolderName.trim().replace(' ', '-').replace("&", "and").toLowerCase() +
				"/" + webObjectDisplayName.trim().replace(' ', '-').toLowerCase() + ".md";
		}

		@Override
		public void currentPackageWasProcessed()
		{
			super.currentPackageWasProcessed();
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
