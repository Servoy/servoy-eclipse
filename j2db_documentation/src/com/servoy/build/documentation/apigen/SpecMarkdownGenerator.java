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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.jar.Manifest;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.dltk.javascript.parser.jsdoc.JSDocTag;
import org.eclipse.dltk.javascript.parser.jsdoc.JSDocTags;
import org.eclipse.dltk.javascript.parser.jsdoc.SimpleJSDocParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Comment;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.PropertyGet;
import org.sablo.specification.Package;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.util.ValueReference;
import org.sablo.websocket.impl.ClientService;

import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.TextUtils;
import com.servoy.j2db.util.Utils;

import freemarker.cache.ClassTemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.utility.DeepUnwrap;

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

	private static int totalFunctionsWithIssues = 0;
	private static int totalComponentsWithIssues = 0;
	private static int totalPackagesWithIssues = 0;
	private static int totalComponentsWithoutDoc = 0;
	private static final Set<String> processedPackages = new HashSet<>();
	private static final Set<String> processedComponents = new HashSet<>();

	private static final DuplicateTracker duplicateTracker = DuplicateTracker.getInstance();
	private static String summaryMdFilePath;
	private static List<String> summaryPaths;
	private static List<String> missingMdFiles = new ArrayList<>();
	private static SimpleJSDocParser jsDocParser = new SimpleJSDocParser();

	private final static java.util.function.Function<String, String> htmlToMarkdownConverter = (initialDescription) -> {
		if (initialDescription == null) return null;

		String convertedDesc = HtmlUtils.applyDescriptionMagic(initialDescription.replace("%%prefix%%", "").replace("%%elementName%%",
			"myElement"));
		convertedDesc = convertedDesc.trim(); // probably not needed
		return SpecMarkdownGenerator.turnHTMLJSDocIntoMarkdown(convertedDesc);
	};

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
		SpecMarkdownGenerator.summaryMdFilePath = gitBookRepoDir.getAbsolutePath() + "/SUMMARY.md";
		SpecMarkdownGenerator.summaryPaths = SpecMarkdownGenerator.loadSummary();

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

		// Print the summary after all validations
		printSummary();

		System.out.println("\nDONE.");
	}

	public static List<String> loadSummary()
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

	public static void printSummary()
	{
		System.out.println("\033[38;5;39m\n\nSUMMARY:\n");
		System.out.println(totalFunctionsWithIssues + " functions in " +
			totalComponentsWithIssues + " components and " +
			totalPackagesWithIssues + " packages need verifications.\n" +
			totalComponentsWithoutDoc + " components has no associated documents.\n");

		if (missingMdFiles.size() > 0)
		{
			System.out.println("\nThe following files are missing from summary.md (gitbook): ");
			missingMdFiles.forEach(filePath -> {
				System.out.println(filePath + ", ");
			});
		}
		System.out.println("\033[0m");
	}

	private static String clearGeneratedDocsDirOnGitbookRepo(File dirWithGeneratedDocs, boolean onlySubDirs) throws IOException
	{
		System.out.println("Spec: clearGeneratedDocsDirOnGitbookRepo(" + dirWithGeneratedDocs.getPath() + ")");
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
		System.out.println("Generating NG package content");
		for (String dirname : webPackageDirs)
		{
			File dir = new File(dirname);
			if (!dir.exists())
			{
				throw new RuntimeException("Given NG package dir " + dirname + " doesn't exist!");
			}
			else
			{
				System.out.println("NG package dir " + dirname);
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
					System.err.println(e.getMessage());
					throw e;
				}
			}).forEach(generator -> {
				try
				{
					if (!generator.isDeprecated() && !generator.isEmptyApi())
					{
						generator.validateSpecAndDoc(generator.getTypes(), generator.getSpecFile(), generator.getDocFile(), generator.getDisplayName(),
							generator.getComponentName());
					}
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
//			else System.err.println("    * cannot find the package's webpackage.json; skipping information about the package...");

			docGenerator.currentPackageWasProcessed();
		}
	}

	private final Map<String, Object> root;
	private final JSONObject jsonObject;
	private final Map<String, String> apiDoc = new HashMap<>();
	private boolean service;
	private final INGPackageInfoGenerator docGenerator;
	private final File mySpecFile;
	private File myDocFile;
	private final boolean deprecatedContent;
	private final JSONObject packageTypes;
	private boolean emptyApi = false;

	public SpecMarkdownGenerator(String packageName, String packageDisplayName, String packageType,
		JSONObject jsonObject, File specFile, boolean generateComponentExtendsAsWell,
		INGPackageInfoGenerator docGenerator, Map<String, Object> globalRootEntries)
	{
		this.mySpecFile = specFile;
		this.jsonObject = jsonObject;
		this.docGenerator = docGenerator;
		this.packageTypes = jsonObject.optJSONObject("types");

		deprecatedContent = jsonObject.optBoolean("deprecated", false);
		JSONObject myApi = jsonObject.optJSONObject("api");
		if (myApi == null || myApi.keySet().isEmpty())
		{
			emptyApi = true;
		}

		root = new HashMap<>();

		/**
		 * Quick convert of HTML to String for use in the template.
		 */
		root.put("MD", (TemplateMethodModelEx)((params) -> htmlToMarkdownConverter.apply((String)DeepUnwrap.unwrap((TemplateModel)params.get(0)))));

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
				myDocFile = docFile;
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
							if (fn.getBody().hasChildren())
							{
								fn.getBody().forEach(node -> {
									if (node instanceof ExpressionStatement es && es.getExpression() instanceof Assignment as &&
										as.getLeft() instanceof PropertyGet pg)
									{
										String jsDoc = as.getJsDoc();
										if (jsDoc != null)
										{
											String propName = pg.getRight().toSource();
											apiDoc.put(name + "." + propName, processFunctionJSDoc(jsDoc));
										}

									}
								});
							}
						}
						else if (prevComment == null)
						{
							// it's the top-most comment that gives a short description of the purpose of the whole component/service
							apiDoc.put(WEB_OBJECT_OVERVIEW_KEY, processFunctionJSDoc(comment.toSource()));
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
				myDocFile = null;
				System.err
					.println("\u001B[32m    * docfile: " + docFileName + " doesn't exist in the parent structure of the spec file " + specFile + " !\u001B[0m");
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
		Object types = makeTypes(jsonObject.optJSONObject("types"));
		root.put("types", types);

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

	public boolean isEmptyApi()
	{
		return emptyApi;
	}

	public JSONObject getTypes()
	{
		return packageTypes;
	}

	public boolean isDeprecated()
	{
		return deprecatedContent;
	}

	public File getSpecFile()
	{
		return mySpecFile;
	}

	public File getDocFile()
	{
		return myDocFile;
	}

	public String getDisplayName()
	{
		return jsonObject.optString("displayName");
	}

	public String getComponentName()
	{
		return jsonObject.optString("name");
	}

	private String processFunctionJSDoc(String jsDocComment)
	{
		if (jsDocComment == null) return null;

		return processDescription(0, jsDocComment);
	}

	/**
	 * Same as {@link #turnHTMLJSDocIntoMarkdown(String, int, boolean)} with indentSpaces set to 0.
	 */
	public static String turnHTMLJSDocIntoMarkdown(String doc)
	{
		return turnHTMLJSDocIntoMarkdown(doc, 0);
	}

	private static final Pattern splitPatternForHTMLToMarkdownConversion = Pattern.compile(
		"(^\\h*(\\@param|\\@example|\\@return|(?<tag>\\@\\p{Alpha}+)))|<br>|<br/>|<pre data-puremarkdown>|<pre text>|<pre>|</pre>|<code>|</code>|<a href=\"|</a>|<b>|</b>|<i>|</i>|<ul>|</ul>|<ol>|</ol>|<li>|</li>|<p>|</p>|<h1>|</h1>|<h2>|</h2>|<h3>|</h3>|<h4>|</h4>",
		Pattern.MULTILINE);
	// IMPORTANT - if you add or remove groups, so (), make sure that the matchedTokensIsNonSpecialAtThing code below keeps using the correct group index

	private final static String AT_SOMETHING_WITHOUT_SPECIAL_MEANING_MARKER = "an@Somethingwithoutspecialmeaning";

	/**
	 * IMPORTANT: this method expects that all newlines in "doc" are '\n'. Make sure that is the case before calling this method.
	 *
	 * <p>JSDoc can contain html tags (we use that and support it in developer tooltips as well). Those tags need to be turned into markdown syntax:
	 * <ul>
	 *     <li>bold tags</li>
	 *     <li>italic tags</li>
	 *     <li>lists</li>
	 *     <li>pre</li>
	 *     <li>...</li>
	 * </ul></p>
	 * <p>If it doesn't contain html tags, we have to pay attention anyway to newlines for example so that they are correct according to markdown syntax.</p>
	 * <p>Also things such as @param or @return will be styled properly - if present. @example and it's content as well...</p>
	 *
	 * @param indentLevel the number of indent levels (1 lvl -> 4 spaces) that each line should be indented with (in case this content will be used as part of a
	 *                    list item, 4 spaces means one level on indentation in the lists (so according to one of the items); this does
	 *                    NOT affect the first line, as the caller uses that as a pre-indented list item
	 * @param applyDescriptionMagicOnDescriptions if it should call HtmlUtils.applyDescriptionMagic(doc) on main description and param/return/... descriptions as well. So it expects that the given string might have the param, return etc. docs included in it as well.
	 */
	public static String turnHTMLJSDocIntoMarkdown(String htmlDoc, int indentLevel)
	{
		// DO ADD NEW UNIT tests to SpecMarkdownGeneratorTest from the test repo for every new bug encountered / tweak made
		if (htmlDoc == null) return null;

		Matcher matcher = splitPatternForHTMLToMarkdownConversion.matcher(htmlDoc);

		List<String> matchedTokens = new ArrayList<>();
		List<Boolean> matchedTokensIsNonSpecialAtThing = new ArrayList<>();
		List<String> betweenMatches = new ArrayList<>();
		int lastGroupMatchEndIndex = 0;
		while (matcher.find())
		{
			MatchResult matchResult = matcher.toMatchResult();
			betweenMatches.add(htmlDoc.substring(lastGroupMatchEndIndex, matchResult.start()));
			matchedTokens.add(matchResult.group().trim()); // trim() is used here to get rid of leading spaces in @something tokens; the rest of the tokens that can match from the regex. don't have white space in them anyway
			matchedTokensIsNonSpecialAtThing.add(Boolean.valueOf(matchResult.group(3) != null));
			lastGroupMatchEndIndex = matchResult.end();
		}

		betweenMatches.add(htmlDoc.substring(lastGroupMatchEndIndex));

		int currentIndentLevel = indentLevel;

		// we could have used 3rd party libs for this translation... but those don't have this "indentLevel" that we need when indenting sub-properties of custom types in the docs...

		MarkdownContentAppender result = new MarkdownContentAppender(htmlDoc.length());

		ValueReference<Boolean> shouldTrimLeadingTheInBetweenContent = new ValueReference<>(Boolean.TRUE);
		Stack<Boolean> listLevelOrderedOrNot = new Stack<>();
		ValueReference<Boolean> insideExampleSectionThatAutoAddsCodeBlock = new ValueReference<>(Boolean.FALSE);
		ValueReference<CodeBacktickState> codeBacktickState = new ValueReference<>(null);
		boolean firstAtSomething = true; // it refers to whether or not an @param, @return, @example, @... was processed yet or not; we want a bit of space between the description of an API an the @ things
		boolean addInBetweenAsRawMarkdown = false;

		for (int i = 0; i < matchedTokens.size(); i++)
		{
			String token = matchedTokens.get(i);
			String inbetween = betweenMatches.get(i);

			if ("@example".equals(token))
			{
				// if this @example tag has any "pre" or "code" in it, use that as it is, do not add automatically code blocks; if not, then assume code follows and automatically add code blocks
				// so handle '@example <pre> .... </pre> correctly
				int j = i + 1;
				boolean preOrCodeTagFound = false;
				while (!preOrCodeTagFound && j < matchedTokens.size() && !matchedTokens.get(j).startsWith("@"))
				{
					if (matchedTokens.get(j).equals("<pre>") || matchedTokens.get(j).equals("<code>")) preOrCodeTagFound = true;
					j++;
				}

				if (preOrCodeTagFound) token = "@exampleDoNotAutoAddCodeBlock"; // make it behave just like a @param or @return in how it processes it's content
			}

			boolean trimTrailingInPrecedingInbetweenContent = false; // so we know we have to trimTrailing;

			boolean preserveMultipleWhiteSpaces = false; // in case of code/pre

			// before we add betweenMatches.get(i) to the result, see what token follows it - so we know what kind of processing it needs
			// NOTE: remember neither the content before this token nor the token itself are yet added; token is checked again later,
			// after preceding content is actually added

			String tokenForSwitchChecks = (matchedTokensIsNonSpecialAtThing.get(i).booleanValue() ? AT_SOMETHING_WITHOUT_SPECIAL_MEANING_MARKER : token); // this is a bit of a hack to be able to use the value of matchedTokensIsNonSpecialAtThing directly in the case statement
			switch (tokenForSwitchChecks)
			{
				case "</h1>" :
					result.appendWithoutEscaping("# ");
					shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
					break;
				case "</h2>" :
					result.appendWithoutEscaping("## ");
					shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
					break;
				case "</h3>" :
					result.appendWithoutEscaping("### ");
					shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
					break;
				case "</h4>" :
					result.appendWithoutEscaping("#### ");
					shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
					break;

				case "<br>", "<br/>" :
				case "@param", "@return", "@exampleDoNotAutoAddCodeBlock", AT_SOMETHING_WITHOUT_SPECIAL_MEANING_MARKER :
				case "<p>", "</p>" :
				case "<ul>", "</ul>", "<ol>", "</ol>", "<li>", "</li>" :
					trimTrailingInPrecedingInbetweenContent = true;
					break;
				case "</pre>" :
				case "</code>" :
					preserveMultipleWhiteSpaces = true;
					break;
				case "<pre data-puremarkdown>" :
					// special tag that we use in the docs of some java classes in order to leave the contents between those tags untouched in markdown (so you can write for example markdown tables directly in the HTML javadoc which will see it as a normal <pre> tag)

					// INTENTIONAL fall-through to next 'case'; so no break here
					// $FALL-THROUGH$
				case "<pre>", "<pre text>", "@example" : // for "<pre text>" see HtmlUtils.applyDescriptionMagic(...) docs; it's meant for descriptions that expect monospaced font and keeping spaces/newlines
					trimTrailingInPrecedingInbetweenContent = true;
					// INTENTIONAL fall-through to next 'case'; so no break here
					//$FALL-THROUGH$
				case "<code>" :
					// any tokens inside these blocks need to go into "betweenMatches"
					// "i" also advances as needed

					int indexOfSpaceBeforeTagAttrs = token.indexOf(" "); // for example -1 for <pre> but 4 for <pre data-puremarkdown> or <pre text>; we use it to generate search tag </pre> in both cases below
					String closingTagStartsWith = ("@example".equals(token) ? "@"
						: "</" + (indexOfSpaceBeforeTagAttrs >= 0 ? token.subSequence(1, indexOfSpaceBeforeTagAttrs) + ">" : token.substring(1)));
					StringBuilder fullTextToClosingCodeOrPreTag = new StringBuilder();
					while ((i + 1 < matchedTokens.size()) && !matchedTokens.get(i + 1).startsWith(closingTagStartsWith))
					{
						fullTextToClosingCodeOrPreTag.append(betweenMatches.get(i + 1) + matchedTokens.get(i + 1));
						i++;
					}
					if (fullTextToClosingCodeOrPreTag.length() > 0)
						betweenMatches.set(i + 1, fullTextToClosingCodeOrPreTag.append(betweenMatches.get(i + 1)).toString());

					// if a <code> is multi - line, do not use simple ` in markdown as that doesn't work correctly, switch
					// automatically (using <code> for multi-line is incorrect in HTML as well, but we work around that here)
					// to <pre> which generates ```js in markdown
					if ("<code>".equals(token))
					{
						String contentAfterPreTag = betweenMatches.get(i + 1);

						// see if content in the <code> is multi-line or not
						if (contentAfterPreTag.indexOf('\n') >= 0)
						{
							// ok, it is multi-line, switch to <pre>
							token = tokenForSwitchChecks = "<pre>";
							if ("</code>".equals(matchedTokens.get(i + 1))) matchedTokens.set(i + 1, "</pre>"); // otherwise it's malformed HTML I guess

							// now do what this switch would have done for <code>
							trimTrailingInPrecedingInbetweenContent = true;
						}
					}
					break;
				case "<a href=\"" :
					// put the link part into the inBetween section but swap it with the one just before the </a> token
					// because in html it is link first, in markdown it is text first; so we have to swap them
					//
					// <a href="https://bla?a=3&b=4"> Some   text</a>
					//       to
					// [Some text](https://bla?a=3&b=4)

					String afterAHref = betweenMatches.get(i + 1); // something like << https://bla?a=3&b=4"> Some   text >>
					int indexOfEndOfLink = afterAHref.indexOf("\"");
					String link = afterAHref.substring(0, indexOfEndOfLink);
					String inBetweenTextThatIsAfterLink = afterAHref.substring(afterAHref.indexOf(">", indexOfEndOfLink) + 1);

					// ok link is removed; add what is left of that inBetween text back
					betweenMatches.set(i + 1, inBetweenTextThatIsAfterLink);

					// add a fictional token (that we can use to end the text part of the link in the switch statement that follows)
					// and the link has in-between just before the </a> token (that can then generate the actual link part)

					int j = i + 1;
					while (j < matchedTokens.size() && !matchedTokens.get(j).equals("</a>"))
						j++;

					if (j < matchedTokens.size())
					{
						matchedTokens.add(j, "<a link part follows>");
						matchedTokensIsNonSpecialAtThing.add(j, Boolean.FALSE);
						betweenMatches.add(j + 1, link);
					}
					break;
				case "<a link part follows>" :
					trimTrailingInPrecedingInbetweenContent = true;
					break;
				default :
			}

			if (token.startsWith("@") && insideExampleSectionThatAutoAddsCodeBlock.value.booleanValue()) preserveMultipleWhiteSpaces = true; // auto generated <code> equivalent inside example section is ending now after content will be added to it
			addInBetweenContent(result, inbetween, preserveMultipleWhiteSpaces,
				shouldTrimLeadingTheInBetweenContent, trimTrailingInPrecedingInbetweenContent,
				addInBetweenAsRawMarkdown, currentIndentLevel, codeBacktickState, token, insideExampleSectionThatAutoAddsCodeBlock);

			// now append anything that the token needs to add
			switch (tokenForSwitchChecks)
			{
				case "</h1>" :
				case "</h2>" :
				case "</h3>" :
				case "</h4>" :
					nextLinePlusIndent(result, currentIndentLevel);
					break;
				case "<br>", "<br/>" :
					result.appendWithoutEscaping("  ");
					nextLinePlusIndent(result, currentIndentLevel);
					shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
					break;
				case "@param", "@example", "@return", "@exampleDoNotAutoAddCodeBlock", AT_SOMETHING_WITHOUT_SPECIAL_MEANING_MARKER :
					if (firstAtSomething)
					{
						// add an empty line - to have a bit of visual separation between the description of an API and the @param, @return etc.
						if (result.length() >= 2 && !"\n\n".equals(result.substring(result.length() - 2)))
						{
							result.appendWithoutEscaping("\n");
							nextLinePlusIndent(result, currentIndentLevel);
						}
						firstAtSomething = false;
						shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
					}
					if (!endsWithMarkdownNewline(result, currentIndentLevel))
					{
						result.appendWithoutEscaping("  "); // markdown equivalent of visual "newline"
						nextLinePlusIndent(result, currentIndentLevel);
					}
					result.appendWithoutEscaping("**").appendWithoutEscaping("@exampleDoNotAutoAddCodeBlock".equals(token) ? "@example" : token)
						.appendWithoutEscaping("**");
					if ("@exampleDoNotAutoAddCodeBlock".equals(token))
					{
						result.appendWithoutEscaping(" ");
						shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
					}
					else shouldTrimLeadingTheInBetweenContent.value = Boolean.FALSE;

					if ("@example".equals(token))
					{
						nextLinePlusIndent(result, currentIndentLevel);
						String contentAfterExampleTag = betweenMatches.get(i + 1);

						// prepare to 'escape' backticks in code/pre content; in markdown this means modifying the start and end special meaning backtick char count to a higher value then the one in the pre/code content...
						// also if the code / pre content starts or ends with backtick, then some spaces need to be added between that and the special meaning backticks (start/end tags of the code/pre section)
						codeBacktickState.value = countContinuousBackticks(contentAfterExampleTag);

						if (codeBacktickState.value.maxContinousBacktickCount > 2)
							result.appendWithoutEscaping("`".repeat(codeBacktickState.value.maxContinousBacktickCount + 1) + "js\n");
						else result.appendWithoutEscaping("```js\n");

						insideExampleSectionThatAutoAddsCodeBlock.value = Boolean.TRUE;

						// if it's like '@example a = a + 1' then we want to left trim what follows after the @example tag (so that one space) before adding the rest in a code block
						// but if it's a multi-line code example
						// '@example
						//     a = a + 1;
						//     callMe(a);
						//  @return something
						// then we don't want to left trim what follows after "@example" (that would remove the 4 spaces as well), just the \n
						int firstBackslashNPosition = contentAfterExampleTag.indexOf('\n');
						if (firstBackslashNPosition >= 0 && contentAfterExampleTag.substring(firstBackslashNPosition).trim().length() == 0)
						{
							// one line
							// so the if above should identify both
							// -----
							// @example i = i + 1
							// return something
							// -----
							// and
							// -----
							// @example i = i + 1
							//
							// @return something
							// -----
							shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
							betweenMatches.set(i + 1, contentAfterExampleTag.substring(0, firstBackslashNPosition));
						}
						else
						{
							// if we have something like
							//
							// @example     a = a + 1;
							//     output(a);
							//
							// then first \n is not the one where the multi-line code starts; then it starts after the first space
							if (firstBackslashNPosition >= 0 && contentAfterExampleTag.substring(0, firstBackslashNPosition + 1).trim().length() == 0)
								betweenMatches.set(i + 1, StringUtils.stripEnd(contentAfterExampleTag.substring(firstBackslashNPosition + 1), null));
							else if (contentAfterExampleTag.charAt(0) == ' ')
								betweenMatches.set(i + 1, StringUtils.stripEnd(contentAfterExampleTag.substring(1), null));
							else betweenMatches.set(i + 1, StringUtils.stripEnd(contentAfterExampleTag, null));
						}
					}
					break;
				case "<p>" :
					if (result.length() != 0)
					{
						if (result.length() < (1 + currentIndentLevel * 4) * 2 ||
							!("\n" + "    ".repeat(currentIndentLevel)).repeat(2).equals(result.substring(result.length() - 2 * (1 + 4 * currentIndentLevel))))
						{
							if (result.length() < (1 + currentIndentLevel * 4) ||
								!("\n" + "    ".repeat(currentIndentLevel)).equals(result.substring(result.length() - 1 - 4 * currentIndentLevel)))
							{
								nextLinePlusIndent(result, currentIndentLevel);
								nextLinePlusIndent(result, currentIndentLevel);
							}
							else nextLinePlusIndent(result, currentIndentLevel);
						}
					}
					shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
					break;
				case "</p>" :
					if (i < matchedTokens.size() - 1 || betweenMatches.get(betweenMatches.size() - 1).trim().length() > 0)
					{
						nextLinePlusIndent(result, currentIndentLevel);
						nextLinePlusIndent(result, currentIndentLevel);
					}
					shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
					break;
				case "<ul>" :
					if (!endsWithNewline(result, currentIndentLevel)) nextLinePlusIndent(result, currentIndentLevel);
					listLevelOrderedOrNot.push(Boolean.FALSE);
					shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
					break;
				case "<ol>" :
					if (!endsWithNewline(result, currentIndentLevel)) nextLinePlusIndent(result, currentIndentLevel);
					listLevelOrderedOrNot.push(Boolean.TRUE);
					shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
					break;
				case "<li>" :
					if (!endsWithNewline(result, currentIndentLevel)) nextLinePlusIndent(result, currentIndentLevel);
					result.appendWithoutEscaping((listLevelOrderedOrNot.peek() != null && listLevelOrderedOrNot.peek().booleanValue() ? "1. " : " - "));
					currentIndentLevel++;
					shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
					break;
				case "</li>" :
					currentIndentLevel--;
					break;
				case "</ul>", "</ol>" :
					if ((i + 1 < matchedTokens.size() && !"</li>".equals(matchedTokens.get(i + 1)) && !"</ul>".equals(matchedTokens.get(i + 1)) &&
						!"</ol>".equals(matchedTokens.get(i + 1))) || (i + 1 == matchedTokens.size()))
					{
						nextLinePlusIndent(result, currentIndentLevel);
						shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
					}
					listLevelOrderedOrNot.pop();
					break;
				case "<code>" :
					shouldTrimLeadingTheInBetweenContent.value = Boolean.FALSE;

					// prepare to 'escape' backticks in code/pre content; in markdown this means modifying the start and end special meaning backtick char count to a higher value then the one in the pre/code content...
					// also if the code / pre content starts or ends with backtick, then some spaces need to be added between that and the special meaning backticks (start/end tags of the code/pre section)
					codeBacktickState.value = countContinuousBackticks(betweenMatches.get(i + 1));

					if (codeBacktickState.value.maxContinousBacktickCount > 0)
						result.appendWithoutEscaping(
							"`".repeat(codeBacktickState.value.maxContinousBacktickCount + 1) + (codeBacktickState.value.startsWithBacktick ? " " : ""));
					else result.appendWithoutEscaping("`");

					break;
				case "</code>" :
					if (codeBacktickState.value != null && codeBacktickState.value.maxContinousBacktickCount > 0)
						result.appendWithoutEscaping(
							(codeBacktickState.value.endsWithBacktick ? " " : "") + "`".repeat(codeBacktickState.value.maxContinousBacktickCount + 1));
					else result.appendWithoutEscaping("`");
					codeBacktickState.value = null;
					break;
				case "<pre data-puremarkdown>" :
					// special tag that we use in the docs of some java classes in order to leave the contents between those tags untouched in markdown (so you can write for example markdown tables directly in the HTML javadoc which will see it as a normal <pre> tag)
					addInBetweenAsRawMarkdown = true;
					// INTENTIONAL fall-through to next 'case'; so no break here
					// $FALL-THROUGH$
				case "<pre>", "<pre text>" :
					if (!endsWithMarkdownNewline(result, currentIndentLevel)) nextLinePlusIndent(result, currentIndentLevel);

					// prepare to 'escape' backticks in code/pre content; in markdown this means modifying the start and end special meaning backtick char count to a higher value then the one in the pre/code content...
					// also if the code / pre content starts or ends with backtick, then some spaces need to be added between that and the special meaning backticks (start/end tags of the code/pre section)
					String contentAfterPre = betweenMatches.get(i + 1);

					if ("<pre>".equals(token) || "<pre text>".equals(token))
					{
						String syntaxHighlight = "<pre>".equals(token) ? "js" : "";
						codeBacktickState.value = countContinuousBackticks(contentAfterPre);
						if (codeBacktickState.value.maxContinousBacktickCount > 2)
							result.appendWithoutEscaping("`".repeat(codeBacktickState.value.maxContinousBacktickCount + 1) + syntaxHighlight + "\n");
						else result.appendWithoutEscaping("```" + syntaxHighlight + "\n");
					}

					if (contentAfterPre.startsWith("\n") || contentAfterPre.endsWith("\n"))
					{
						// if there is something like
						// <pre>
						//   a = 1;
						//   b = 2;
						// </pre>
						// then we must ignore the new lines after start tag and before end tag
						betweenMatches.set(i + 1, contentAfterPre.substring(contentAfterPre.startsWith("\n") ? 1 : 0,
							contentAfterPre.endsWith("\n") ? contentAfterPre.length() - 1 : contentAfterPre.length()));
					}

					shouldTrimLeadingTheInBetweenContent.value = Boolean.FALSE;
					break;
				case "</pre>" :
					if (addInBetweenAsRawMarkdown) addInBetweenAsRawMarkdown = false;
					else
					{
						if (codeBacktickState.value != null && codeBacktickState.value.maxContinousBacktickCount > 2)
							result.appendWithoutEscaping("\n" + "`".repeat(codeBacktickState.value.maxContinousBacktickCount + 1));
						else result.appendWithoutEscaping("\n```");
						codeBacktickState.value = null;
						nextLinePlusIndent(result, currentIndentLevel);
					}
					shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
					break;

				case "<a href=\"" :
					// the switch above (before this one - the pre-processing switch) would have reordered/swapped
					// the anchor's link with the text to match markdown order and
					// should have added the "<a link part follows>" internal token as well
					// [Some text](https://bla?a=3&b=4)
					result.appendWithoutEscaping("[");
					shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
					break;
				case "<a link part follows>" :
					result.appendWithoutEscaping("](");
					break;
				case "</a>" :
					result.appendWithoutEscaping(")");
					break;

				case "<i>", "</i>" :
					result.appendWithoutEscaping("*");
					break;
				case "<b>", "</b>" :
					result.appendWithoutEscaping("**");
					break;
				default :
			}
		}

		// add text that is after last token; this is similar to code above that adds the inBetween content between matches,
		// where 'preserveMultipleWhiteSpaces' and 'trimTrailingInPrecedingInbetweenContent' would be !insideExampleSection
		String inbetween = betweenMatches.get(betweenMatches.size() - 1);
		addInBetweenContent(result, inbetween, insideExampleSectionThatAutoAddsCodeBlock.value.booleanValue(),
			shouldTrimLeadingTheInBetweenContent, !insideExampleSectionThatAutoAddsCodeBlock.value.booleanValue(),
			addInBetweenAsRawMarkdown, currentIndentLevel, codeBacktickState, null, insideExampleSectionThatAutoAddsCodeBlock);

		return sanitizeReferences(result.toString());
	}

	//regular point char is escaped - this is creating problems in recognizing references.
	// for example in markdown I get: \.\./\.\./myreference.md instead of ../../myReference.md
	// while some markdown viewers can handle this, others can;t
	// make sure of unescaped points inside references
	private static String sanitizeReferences(String markdownContent)
	{
		Pattern pattern = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)");
		Matcher matcher = pattern.matcher(markdownContent);

		StringBuffer sanitizedContent = new StringBuffer();

		while (matcher.find())
		{
			// Capture groups for text and reference
			String text = matcher.group(1);
			String reference = matcher.group(2).replace("\\.", "."); // Remove escaping for periods in reference only

			// Append the sanitized replacement to the result
			matcher.appendReplacement(sanitizedContent, "[" + text + "](" + reference + ")");
		}
		matcher.appendTail(sanitizedContent); // Append the remaining content

		return sanitizedContent.toString();
	}

	private static void addInBetweenContent(MarkdownContentAppender result,
		String inbetweenUnprocessed,
		boolean preserveMultipleWhiteSpaces,
		ValueReference<Boolean> shouldTrimLeadingTheInBetweenContent,
		boolean trimTrailingInPrecedingInbetweenContent,
		boolean addInBetweenAsRawMarkdown,
		int currentIndentLevel,
		ValueReference<CodeBacktickState> codeBacktickState,
		String token,
		ValueReference<Boolean> insideExampleSectionThatAutoAddsCodeBlock)
	{
		// first get rid of the &nbsp; &gt; &#21; etc - turn them into UTF8 chars
		String inbetween = StringEscapeUtils.unescapeHtml4(inbetweenUnprocessed);

		// ok now do add the "betweenMatches" content that was before 'token'
		if (!preserveMultipleWhiteSpaces) inbetween = inbetween.replaceAll("\\s+", " ");

		if (shouldTrimLeadingTheInBetweenContent.value.booleanValue())
		{
			inbetween = inbetween.stripLeading();
			shouldTrimLeadingTheInBetweenContent.value = Boolean.valueOf((inbetween.length() == 0));
		}

		if (trimTrailingInPrecedingInbetweenContent) inbetween = inbetween.stripTrailing();

		if (addInBetweenAsRawMarkdown)
		{
			// this is for a "<pre data-puremarkdown>" tag content

			// indent the raw markdown as needed
			String[] lines = inbetween.split("\n");
			inbetween = "";

			for (String line : lines)
			{
				result.appendWithoutEscaping(line);
				nextLinePlusIndent(result, currentIndentLevel);
			}
		}
		else
		{
			// the usual case
			result.appendInBetweenTagContent(inbetween, codeBacktickState.value != null);
		}

		if ((token == null || token.startsWith("@")) && insideExampleSectionThatAutoAddsCodeBlock.value.booleanValue())
		{
			// the example section has finished
			insideExampleSectionThatAutoAddsCodeBlock.value = Boolean.FALSE;

			if (codeBacktickState.value.maxContinousBacktickCount > 2)
				result.appendWithoutEscaping("\n" + "`".repeat(codeBacktickState.value.maxContinousBacktickCount + 1));
			else result.appendWithoutEscaping("\n```");
			codeBacktickState.value = null;

			nextLinePlusIndent(result, currentIndentLevel);
			shouldTrimLeadingTheInBetweenContent.value = Boolean.TRUE;
		}
	}

	private static CodeBacktickState countContinuousBackticks(String contentOfPreOrCodeSection)
	{
		int maxContinousBacktickCount = 0;
		int currentContinousBacktickCount = 0;
		for (int i = 0; i < contentOfPreOrCodeSection.length() - 1; i++)
		{
			if (contentOfPreOrCodeSection.charAt(i) == '`') currentContinousBacktickCount++;
			else if (currentContinousBacktickCount > 0)
			{
				maxContinousBacktickCount = Math.max(currentContinousBacktickCount, maxContinousBacktickCount);
				currentContinousBacktickCount = 0;
			}
		}
		maxContinousBacktickCount = Math.max(currentContinousBacktickCount, maxContinousBacktickCount);

		return new CodeBacktickState(maxContinousBacktickCount, contentOfPreOrCodeSection.startsWith("`"), contentOfPreOrCodeSection.endsWith("`"));
	}

	private static boolean endsWithMarkdownNewline(MarkdownContentAppender result, int currentIndentLevel)
	{
		return result.length() == 0 ||
			(result.length() >= 2 && result.substring(result.length() - 2).equals("\n\n")) ||
			(result.length() >= 3 && result.substring(result.length() - 3).equals("  \n")) ||
			(result.length() >= (3 + currentIndentLevel * 4) &&
				result.substring(result.length() - (3 + currentIndentLevel * 4)).equals("  \n" + " ".repeat(currentIndentLevel * 4))) ||
			(result.length() >= (2 + currentIndentLevel * 4) &&
				result.substring(result.length() - (2 + currentIndentLevel * 4)).equals("\n\n" + " ".repeat(currentIndentLevel * 4))) ||
			(result.length() >= (4 + currentIndentLevel * 4) &&
				result.substring(result.length() - (4 + currentIndentLevel * 4)).equals("```\n" + " ".repeat(currentIndentLevel * 4)));
	}

	/**
	 * Some code needs to check just that the markdown source has a new line, not the markdown output (which would mean either two new lines in source or double space at the end of line + new line)
	 * For example if what follows next is a list, then a simple new line in the source code is enough to make the list work.
	 */
	private static boolean endsWithNewline(MarkdownContentAppender result, int currentIndentLevel)
	{
		return result.length() == 0 ||
			result.length() >= 1 && result.charAt(result.length() - 1) == '\n' ||
			result.length() >= (1 + currentIndentLevel * 4) &&
				result.substring(result.length() - (1 + currentIndentLevel * 4)).equals("\n" + " ".repeat(currentIndentLevel * 4));
	}

	private static void nextLinePlusIndent(MarkdownContentAppender result, int currentIndentLevel)
	{
		result.appendWithoutEscaping("\n");

		// so that they are aligned with some list item in a previously started list
		indentAtLevel(result, currentIndentLevel);
	}

	private static void indentAtLevel(MarkdownContentAppender result, int currentIndentLevel)
	{
		result.appendWithoutEscaping("    ".repeat(currentIndentLevel));
	}

	private Record createProperty(String name, JSONObject specEntry)
	{
		return createPropertyWithIndent(name, specEntry, 0);
	}

	private Record createPropertyIndented(String name, JSONObject specEntry)
	{
		return createPropertyWithIndent(name, specEntry, 2);
	}

	private Record createPropertyWithIndent(String name, JSONObject specEntry, int indentLevel)
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
			if (doc != null)
			{
				doc = processDescription(indentLevel, doc);
			}
		}

		return new Property(name, type, dflt, doc, deprecationString);
	}

	private String processDescription(int indentLevel, String initialDescription)
	{
		String doc = TextUtils.newLinesToBackslashN(initialDescription).replace("%%prefix%%", "").replace("%%elementName%%", "myElement");

		if (docGenerator.shouldTurnAPIDocsIntoMarkdown()) doc = TextUtils.stripCommentStartMiddleAndEndChars(doc);

		doc = doc.trim();
		if (docGenerator.shouldTurnAPIDocsIntoMarkdown()) doc = turnHTMLJSDocIntoMarkdown(applyDescriptionMagicOnDescriptions(doc), indentLevel);
		return doc;
	}

	private final static Pattern descriptionSplitPattern = Pattern.compile(
		"(^\\h*(\\@param|\\@return)(\\h+\\p{Alnum}*\\h*\\{.+\\})?\\h*)|(^\\h*\\@\\p{Alpha}+\\h*)", Pattern.MULTILINE);

	/**
	 * As when we read the _doc.js files of components/services, the "doc" will
	 * contain the main description as well as potentially tags such as @param, @return, ... with their own descriptions,
	 * and HtmlUtils.applyDescriptionMagic(...) does not work with that, but only with single descriptions (otherwise for example blank new lines
	 * before a @param tag might confuse it), here we:
	 * <ul>
	 *   <li>split the doc into it's independent single descriptions</li>
	 *   <li>apply HtmlUtils.applyDescriptionMagic(...)</li>
	 *   <li>add everything back in it's place</li>
	 * </ul>
	 *
	 * @param doc a doc that potentially has @param, @return, ... tags in it
	 * @return a doc where all independent descriptions (main, the ones of params, return values etc) were processed by HtmlUtils.applyDescriptionMagic(...)
	 */
	public static String applyDescriptionMagicOnDescriptions(String htmlDoc)
	{
		// DO ADD NEW UNIT tests to SpecMarkdownGeneratorTest from the test repo for every new bug encountered / tweak made
		if (htmlDoc == null) return null;

		StringBuilder sb = new StringBuilder(htmlDoc.length() + htmlDoc.length() / 4);

		Matcher matcher = descriptionSplitPattern.matcher(htmlDoc);

		List<String> matchedTokens = new ArrayList<>();
		List<String> betweenMatches = new ArrayList<>();
		int lastGroupMatchEndIndex = 0;
		while (matcher.find())
		{
			MatchResult matchResult = matcher.toMatchResult();
			betweenMatches.add(htmlDoc.substring(lastGroupMatchEndIndex, matchResult.start()));
			matchedTokens.add(matchResult.group());

			lastGroupMatchEndIndex = matchResult.end();
		}

		betweenMatches.add(htmlDoc.substring(lastGroupMatchEndIndex));

		for (int i = 0; i < matchedTokens.size(); i++)
		{
			String tokenProcessed = matchedTokens.get(i).trim();
			handleInBetweenProcessedForApplyDescriptionsMagic(sb, betweenMatches, matchedTokens, i);

			sb.append(tokenProcessed);
			if (betweenMatches.get(i + 1).trim().length() > 0) sb.append(" ");
		}
		handleInBetweenProcessedForApplyDescriptionsMagic(sb, betweenMatches, matchedTokens, matchedTokens.size());

		return sb.toString();
	}

	private static void handleInBetweenProcessedForApplyDescriptionsMagic(StringBuilder sb, List<String> betweenMatches, List<String> matchedTokens, int i)
	{
		boolean isAfterExampleToken = (i > 0 && matchedTokens.get(i - 1).contains("@example"));

		String inbetweenProcessed = (isAfterExampleToken ? StringUtils.stripEnd(betweenMatches.get(i), null)
			: HtmlUtils.applyDescriptionMagic(betweenMatches.get(i).trim()));

		if (inbetweenProcessed.length() > 0)
		{
			sb.append(inbetweenProcessed);
		}
		if (sb.length() > 0 && matchedTokens.size() != i /* but not if nothing follows */)
		{
			sb.append("\n");
			if (i == 0 /* an extra new line after description */) sb.append("\n");
		}
	}

	private Record createFunction(String name, JSONObject specEntry)
	{
		return createFunction(name, specEntry, name);
	}

	private Record createFunction(String name, JSONObject specEntry, String apiDocName)
	{
		String returnType = specEntry.optString("returns", null);
		String docReturnType = specEntry.optString("docReturns", null);
		JSONArray parameters = specEntry.optJSONArray("parameters");
		String deprecationString = specEntry.optString("deprecated", null);
		List<Parameter> params = createParameters(parameters);
		JSONObject fullReturnType = specEntry.optJSONObject("returns");
		if (fullReturnType != null)
		{
			returnType = fullReturnType.optString("type", "");
		}
		else if (returnType != null)
		{
			fullReturnType = new JSONObject();
			fullReturnType.put("description", "");
			fullReturnType.put("type", returnType);
			if (docReturnType != null)
			{
				fullReturnType.put("docType", docReturnType.replace("\\", ""));
			}
		}
		String myApiDocName = apiDocName;
		if (specEntry.optBoolean("overload"))
		{
			JSONArray specParams = specEntry.optJSONArray("parameters");
			if (specParams != null && specParams.length() > 0)
			{
				for (int index = 0; index < specParams.length(); index++)
				{
					JSONObject param = specParams.getJSONObject(index);
					myApiDocName += "_" + param.getString("name");
				}
			}
		}

		String jsDocEquivalent = apiDoc.get(myApiDocName);
		if (jsDocEquivalent == null)
		{
			jsDocEquivalent = processFunctionJSDoc(createFunctionDocFromSpec(specEntry, params, returnType, fullReturnType,
				docGenerator.shouldSetJSDocGeneratedFromSpecEvenIfThereIsNoDescriptionInIt()));
		}

		if (jsDocEquivalent != null)
		{
			//at this point all special characters like {,},(,}, ., [, ], etc are getting escaped; the regex must take into account this
//			Pattern pattern = Pattern.compile("^\\s*(\\*{1,2}\\s*)?(\\\\\\{[^}]+\\\\\\})?\\s*(\\\\\\[[^\\]]+\\\\\\])?");

			jsDocParser = new SimpleJSDocParser();
			JSDocTags jsDocTags = jsDocParser.parse(jsDocEquivalent, 0);
			int paramIndex = 0;
			StringBuilder updatedJsDocEquivalent = new StringBuilder(jsDocEquivalent);
			int delta = 0;
			for (int index = 0; index < jsDocTags.size(); index++)
			{
				JSDocTag jsDocTag = jsDocTags.get(index);
				int tagStart = jsDocTag.start();
				int tagEnd = jsDocTag.end();
				if (jsDocTag.name().equals(JSDocTag.PARAM) || jsDocTag.name().equals(JSDocTag.RETURN))
				{
					Parameter param = null;
					if (paramIndex < params.size()) param = params.get(paramIndex);

					if (jsDocTag.name().equals(JSDocTag.PARAM))
					{
						Pattern pattern = Pattern.compile("^(?:\\*\\*\\s*)?\\\\\\{([^}]+)\\\\\\}\\s+(\\S+)\\s*(.*)$");
						JSONObject tagElements = extractParamTagElements(jsDocTag.value(), pattern);
						if (params != null)
						{
							if (paramIndex > params.size() - 1)
							{
								System.err.println("Wrong number of parameters in JSDoc. Please check the documentation and spec for: " + apiDocName);
								paramIndex++;
								continue;
							}

							//do not get confused at this point param.docType() is the type loaded from _doc.js
							//after validation that loaded type is correct
							Parameter newParam = new Parameter(param.name(), param.type(), param.docType(), tagElements.optString("doc", ""), param.optional());
							params.set(paramIndex, newParam);
							paramIndex++;
						}
					}
					else if (jsDocTag.name().equals(JSDocTag.RETURN))
					{
						Pattern pattern = Pattern.compile("^(?:\\*\\*\\s*)?\\\\\\{([^}]+)\\\\\\}\\s*(.*)$");
						JSONObject tagElements = extractReturnTagElements(jsDocTag.value(), pattern);

						if (fullReturnType == null)
						{
							fullReturnType = new JSONObject();
						}
						fullReturnType.put("description", tagElements.optString("doc", ""));
						fullReturnType.put("type", returnType);
					}

					try
					{
						if (tagStart != -1 && (tagEnd > tagStart))
						{
							while (tagStart > 0 && (updatedJsDocEquivalent.charAt(tagStart - delta - 1) == '*'))
								tagStart--;
							while (tagEnd < (updatedJsDocEquivalent.length() + delta) &&
								Character.isWhitespace(updatedJsDocEquivalent.charAt(tagEnd - delta + 1)))
								tagEnd++;
							if (tagEnd < updatedJsDocEquivalent.length() + delta) tagEnd++; // end of line
							updatedJsDocEquivalent = updatedJsDocEquivalent.replace(tagStart - delta, tagEnd - delta, "");
							delta += (tagEnd - tagStart);

						}
					}
					catch (IndexOutOfBoundsException e)
					{
						e.printStackTrace();
					}
				}
				else if (jsDocTag.name().equals(JSDocTag.EXAMPLE))
				{
					int exIndex = updatedJsDocEquivalent.indexOf("@example");
					updatedJsDocEquivalent = updatedJsDocEquivalent.replace(exIndex, exIndex + 8, "Example:");

				}
			}
			jsDocEquivalent = updatedJsDocEquivalent.toString();
		}
		//make sure the code template generation is not crash
		if (fullReturnType == null)
		{
			//create an empty one
			fullReturnType = new JSONObject();
			fullReturnType.put("description", "");
			fullReturnType.put("type", "");
			fullReturnType.put("docType", "");
		}
		return new Function(name, params, fullReturnType, jsDocEquivalent, deprecationString);
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
			//spec.schema is using "description" for "returns" and not "doc"
			if (shouldSetJSDocGeneratedFromSpecEvenIfThereIsNoDescriptionInIt ||
				(fullReturnType != null && fullReturnType.optString("description", null) != null))
			{

				somethingWasWritten = true;
				generatedJSDocCommentFromSpec.append("\n * @return {").append(returnType).append('}');
				if (fullReturnType != null && fullReturnType.optString("description", null) != null)
				{
					someDocEntryFromSpecWasWritten = true;
					generatedJSDocCommentFromSpec.append(fullReturnType.optString("description"));
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
				String docType = param.optString("docType", "");
				if (docType != null)
				{
					docType.replace("\\", "");
				}
				JSONObject fullType = param.optJSONObject("type");
				if (fullType != null)
				{
					type = fullType.optString("type", "");
				}
				boolean optional = param.optBoolean("optional", false);
				String doc = param.optString("doc", "");
				params.add(new Parameter(paramName, type, docType, doc, optional));
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
				JSONObject typeObject = types.optJSONObject(type);
				if (typeObject.has("tags") && "private".equals(typeObject.getJSONObject("tags").optString("scope"))) continue;

				if (typeObject.has("model"))
				{
					String extend = typeObject.optString("extends", null);
					if (extend != null)
					{
						extend = '[' + extend + "](#" + extend.toLowerCase() + ')';
					}
					else extend = "";
					Map<String, Object> modelMap = makeMap(typeObject.getJSONObject("model"), this::createPropertyIndented);
					if (typeObject.has("serversideapi"))
					{
						var x = new BiFunction<String, JSONObject, Record>()
						{
							@Override
							public Record apply(String functionName, JSONObject object)
							{
								return createFunction(functionName, object, type + "." + functionName);
							}
						};
						Map<String, Object> apiMap = makeMap(typeObject.getJSONObject("serversideapi"), x);
						map.put(type, Map.of("model", modelMap, "serversideapi", apiMap, "extends", extend));
					}
					else map.put(type, Map.of("model", modelMap, "extends", extend));
				}
				else map.put(type, Map.of("model", makeMap(typeObject, this::createPropertyIndented)));
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
				if (value instanceof JSONObject json)
				{
					Object tags = json.opt("tags");
					if (tags instanceof JSONObject)
					{
						String scope = ((JSONObject)tags).optString("scope");
						if ("private".equals(scope)) continue;
					}

					json = replaceTypes(json);
					json = updateParametersWithDocType(json, key);
					Record record = transformer.apply(key, json);

					map.put(record.toString(), record);
					JSONArray overloads = json.optJSONArray("overloads");
					if (overloads != null)
					{
						overloads.forEach(overload -> {
							((JSONObject)overload).put("overload", true);
							Record r = transformer.apply(key, (JSONObject)overload);
							map.put(r.toString(), r);
						});
					}
				}
				else map.put(key, transformer.apply(key, new JSONObject(new JSONStringer().object().key("type").value(value).endObject().toString())));
			}
			return map;
		}
		return null;
	}

	private JSONObject updateParametersWithDocType(JSONObject apiFunction, String functionName)
	{
		JSONArray parametersList = apiFunction.optJSONArray("parameters");
		String returnValue = apiFunction.optString("returns", null);
		String jsDoc = apiDoc.get(functionName);

		if (jsDoc != null && jsDoc.trim().length() > 0)
		{
			JSDocTags jsDocTags = jsDocParser.parse(jsDoc, 0);

			for (int i = 0; i < jsDocTags.size(); i++)
			{
				JSDocTag tag = jsDocTags.get(i);
				if (returnValue != null && tag.name().equals(JSDocTag.RETURN))
				{
					String type = extractType(tag.value());
					if (type != null && !type.trim().isEmpty())
						apiFunction.put("docReturns", type);
				}
				else if (tag.name().equals(JSDocTag.PARAM))
				{
					Pattern pattern = Pattern.compile("\\{([^}]+)\\}\\s+(\\S+)\\s*(.*)");
					JSONObject tagElements = extractParamTagElements(tag.value(), pattern);
					String paramType = tagElements.optString("type", null);
					String paramName = tagElements.optString("name", null);

					paramType = paramType != null ? paramType.replace("\\", "").trim() : null;
					paramName = paramName != null ? paramName.replace("\\", "").replace("[", "").replace("]", "").trim() : null;

					if (parametersList != null && paramType != null && paramName != null)
					{
						for (int index = 0; index < parametersList.length(); index++)
						{
							JSONObject param = parametersList.getJSONObject(index);
							if (paramName.equals(param.optString("name")))
							{
								param.put("docType", paramType); // Store docType at parameter level
								break;
							}
						}
					}
				}
			}
		}

		return apiFunction;

	}

	//value has the following format: "** \{type\} [name] description"
	//we need to extract the type
	private String extractType(String value)
	{
		String cleanValue = value.replaceAll("\\\\([{}])", "$1").trim();
		cleanValue = cleanValue.replace("**", "").trim();

		// Ensure the string starts with { and has a closing }
		int startIndex = cleanValue.indexOf("{");
		int endIndex = cleanValue.indexOf("}");

		if (startIndex != -1 && endIndex != -1 && endIndex > startIndex)
		{
			return cleanValue.substring(startIndex + 1, endIndex).trim();
		}
		return null;
	}

	private JSONObject replaceTypes(JSONObject inputJson)
	{
		// Replace types in "parameters"
		JSONArray parameters = inputJson.optJSONArray("parameters");
		if (parameters != null)
		{
			for (int i = 0; i < parameters.length(); i++)
			{
				JSONObject param = parameters.getJSONObject(i);
				replaceType(param, "type");
			}
		}

		// Replace type in "returns" if it exists
		JSONObject returns = inputJson.optJSONObject("returns");
		if (returns != null)
		{
			replaceType(returns, "type");
		}

		return inputJson;
	}

	private void replaceType(JSONObject obj, String key)
	{
		Object type = obj.opt(key);
		if (type instanceof String s)
		{
			obj.put(key, getReplacedType(s));
		}
		else if (type instanceof JSONObject nestedObj)
		{
			String nestedType = nestedObj.optString("type", null);
			if (nestedType != null)
			{
				nestedObj.put("type", getReplacedType(nestedType));
			}
		}
		else if (type != null)
		{
			System.err.println("Unknown type: " + type);
		}
	}

	private String getReplacedType(String originalType)
	{
		boolean isArray = originalType.endsWith("[]");
		String baseType = isArray ? originalType.substring(0, originalType.length() - 2) : originalType;

		return isArray ? "Array<" + mapType(baseType) + ">" : mapType(baseType);
	}

	private String mapType(String baseType)
	{
		return switch (baseType)
		{
			case "record" -> "JSRecord";
			case "dataset" -> "JSDataset";
			case "foundset" -> "JSFoundset";
			case "event" -> "JSEvent";
			default -> baseType;
		};
	}

	public String getPackagePath(String packageDisplayName)
	{
		return "../../packages/" + (service ? "services/" : "components/") + packageDisplayName.trim().replace("&", "and").replace(' ', '-').toLowerCase() +
			".md";
	}

	public String getDocType(Object typeContainer, Map<String, Object> customTypes, String packageName)
	{
		String type = null;
		if (typeContainer instanceof Property prop)
		{
			type = prop.type();
		}
		else if (typeContainer instanceof Parameter param)
		{
			type = param.type();
		}
		else if (typeContainer instanceof Function func)
		{
			type = func.returnValue().getString("docType");
			if (type != null && type.trim().length() > 0)
			{
				return type; //this is the doctype from _doc.js; at this point must be already processed (else the error is signalled prior to this point
			}
			type = func.returnValue().getString("type");
		}
		else if (typeContainer instanceof JSONObject jsonObj)
		{
			type = jsonObj.optString("docType");
			if (type != null && type.trim().length() > 0)
			{
				return type; //this is the doctype from _doc.js; at this point must be already processed (else the error is signalled prior to this point
			}
			type = jsonObj.optString("type");
		}
		if (type != null)
		{
			boolean isArray = false;


			if (type.endsWith("[]"))
			{
				isArray = true;
				type = type.substring(0, type.length() - 2);
			}
			if (customTypes != null && customTypes.containsKey(type))
			{
				type = "CustomType<" + packageName + "." + type + ">";
			}
			else
			{
				type = type.toLowerCase().substring(0, 1).toUpperCase() + type.substring(1); //capitalize type
				type = normalizeType(null, type, null);
			}
			if (isArray)
			{
				type = "Array<" + type + ">";
			}
			return type;
		}
		return "";
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
			type = ((Function)rcd).returnValue().getString("type");
		}
		if (type != null)
		{
			if (type.endsWith("[]")) type = type.substring(0, type.length() - 2);
			else if (type.startsWith("Array<") && type.endsWith(">")) type = type.substring(6, type.length() - 1);

			@SuppressWarnings("unchecked")
			Map<String, Record> types = (Map<String, Record>)root.get("types");
			if (types != null && types.get(type) != null) return "#" + type.toLowerCase();
			return switch (type.toLowerCase()) // TODO why don't we do an exact case match here? for example "foUnDSET" is allowed or just "foundset"?
			{
				case "object" -> "../../../servoycore/dev-api/js-lib/object.md";
				case "string..." -> "../../../servoycore/dev-api/js-lib/string.md";
				case "string" -> "../../../servoycore/dev-api/js-lib/string.md";
				case "boolean" -> "../../../servoycore/dev-api/js-lib/boolean.md";
				case "int" -> "../../../servoycore/dev-api/js-lib/number.md";
				case "long" -> "../../../servoycore/dev-api/js-lib/number.md";
				case "double" -> "../../../servoycore/dev-api/js-lib/number.md";
				case "byte" -> "../../../servoycore/dev-api/js-lib/number.md";
				case "float" -> "../../../servoycore/dev-api/js-lib/number.md";
				case "json" -> "../../../servoycore/dev-api/js-lib/json.md";
				case "dimension" -> "../../../servoycore/dev-api/js-lib/dimension.md";
				case "point" -> "../../../servoycore/dev-api/js-lib/point.md";
				case "date" -> "../../../servoycore/dev-api/js-lib/date.md";
				case "jsevent" -> "../../../servoycore/dev-api/application/jsevent.md";
				case "jsupload" -> "../../../servoycore/dev-api/application/jsupload.md";
				case "tagstring" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#tagstring";
				case "titlestring" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#titlestring";
				case "styleclass" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#styleclass";
				case "protected" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#protected";
				case "enabled" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#protected";
				case "readonly" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#protected";
				case "variant" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#variant";
				case "visible" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#visible";
				case "tabseq" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#tabseq";
				case "format" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#format";
				case "color" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#color";
				case "map" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#map";
				case "scrollbars" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#scrollbars";
				case "dataprovider" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#dataprovider";
				case "${dataprovidertype}" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#dataprovider";
				case "relation" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#relation";
				case "form" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#form";
				case "formscope" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#form";
				case "formcomponent" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#formcomponent";
				case "record" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#record";
				case "foundset" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#foundset";
				case "foundsetinitialpagesize" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#foundsetinitialpagesize";
				case "foundsetref" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#foundsetref";
				case "dataset" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#dataset";
				case "function" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#function";
				case "clientfunction" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#clientfunction";
				case "media" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#media";
				case "valuelist" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#valuelist";
				case "labelfor" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#labelfor";
				case "modifiable" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#modifiable";
				case "valuelistconfig" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#valuelistConfig";
				case "border" -> "../../../servoy-developer/component\\_and\\_service\\_property\\_types.md#border";
				case "jsdndevent" -> "../../../servoycore/dev-api/application/jsdndevent.md";
				case "jsmenu" -> "../../../servoycore/dev-api/menus/jsmenu.md";
				case "jsmenuitem" -> "../../../servoycore/dev-api/menus/jsmenuitem.md";
				case "runtimecomponent" -> "../../../servoycore/dev-api/forms/runtimeform/elements/runtimecomponent.md";
				case "runtimewebcomponent" -> "../../../servoycore/dev-api/forms/runtimeform/elements/runtimewebcomponent.md";
				case "jswebcomponent" -> "../../../servoycore/dev-api/solutionmodel/jswebcomponent.md";
				case "jsrecord" -> "../../../servoycore/dev-api/database-manager/jsrecord.md";
				case "jsfoundset" -> "../../../servoycore/dev-api/database-manager/jsfoundset.md";
				case "jsdataset" -> "../../../servoycore/dev-api/database-manager/jsdataset.md";
				default -> {
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
	public record Function(String name, List<Parameter> parameters, JSONObject returnValue, String doc, String deprecationString)
	{
		@Override
		public String toString()
		{
			return name + '(' + parameters.stream().map(parameter -> parameter.name).collect(Collectors.joining(",")) + ')';
		}

		public String returnType()
		{
			return returnValue != null ? returnValue.optString("type", null) : null;
		}

		@Override
		public JSONObject returnValue()
		{
			if (returnValue != null)
			{
				String type = returnValue.optString("type", null);
				if (type == null || type != null && type.isBlank())
				{
					return null;
				}
			}
			return returnValue;
		}

	}

	public record Parameter(String name, String type, String docType, String doc, boolean optional)
	{
		@Override
		public String toString()
		{
			return name;
		}
	}

	/**
	 * @param deprecationString null if not deprecated, "true" or a deprecation explanation if deprecated...
	 */
	public record Property(String name, String type, String defaultValue, String doc, String deprecationString)
	{
		@Override
		public String toString()
		{
			return name;
		}
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

	private void validateSpecAndDoc(JSONObject types, File specFile, File docFile, String displayName, String componentName)
	{
		Map<String, FunctionInfo> specFunctions = getSpecFunctions(specFile);
		Map<String, FunctionInfo> docFunctions = getDocFunctions(docFile);

		boolean componentHasIssues = false;
		String packageName = root.get("package_name").toString();
		System.out.println("    - Validate: " + componentName);
		for (String functionName : specFunctions.keySet())
		{
			boolean functionHasIssues = false;
			FunctionInfo specInfo = specFunctions.get(functionName);
			if (specInfo.isDeprecated()) continue;

			FunctionInfo docInfo = docFunctions.get(functionName);

			if (docInfo == null)
			{
				functionHasIssues = true;
				System.err.println("\u001B[32m" + packageName + " ::: " + displayName + " ::: " + functionName +
					" ::: Missing documentation for this function.\u001B[0m");
				continue;
			}

			// Validate parameters
			List<Parameter> specParams = specInfo.getParameters();
			List<Parameter> docParams = docInfo.getParameters();
			if (specParams.size() != docParams.size())
			{
				functionHasIssues = true;
				System.err.println(packageName + " ::: " + displayName + " ::: " + functionName +
					" ::: Parameter count mismatch. Spec: " + specParams.size() + ", Doc: " + docParams.size());
			}

			for (int i = 0; i < specParams.size(); i++)
			{
				Parameter specParam = specParams.get(i);
				Parameter docParam = null;
				String docParamName = null;
				if (i < docParams.size())
				{
					docParam = docParams.get(i);
					docParamName = docParam.name();
				}
				String specParamName = specParam.name();
				// Use accessor methods provided by the Parameter record
				if (specParam.optional && docParamName != null)
				{
					if (!docParamName.startsWith("[") || !docParamName.endsWith("]"))
					{
						functionHasIssues = true;
						System.err.println(packageName + " ::: " + displayName + " ::: " + functionName +
							" ::: Parameter optional brackets are missing: " + specParam.name());
					}
				}
				if (specParam.optional && docParam != null && (docParamName.startsWith("[") && docParamName.endsWith("]")))
				{
					docParamName = docParamName.substring(1, docParamName.length() - 1);
				}
				if (docParamName != null && !docParamName.equals(specParamName))
				{
					functionHasIssues = true;
					System.err.println(packageName + " ::: " + displayName + " ::: " + functionName +
						" ::: Parameter name mismatch. Spec: " + specParam.name() + ", Doc: " + docParam.name());
				}

				if (docParam != null && !areTypesEquivalent(types, specParam.type(), docParam.type(), componentName))
				{
					functionHasIssues = true;
					System.err.println(packageName + " ::: " + displayName + " ::: " + functionName +
						" ::: Parameter type mismatch. Spec: " + specParam.type() + ", Doc: " + docParam.type());
				}

				if (docParam != null && (docParam.doc() == null || docParam.doc().isEmpty()))
				{
					functionHasIssues = true;
					System.err.println(packageName + " ::: " + displayName + " ::: " + functionName +
						" ::: Parameter description mismatch (custom type in doc?). Spec: " + specParam.type() + ", Doc: " + docParam.type());
				}
			}

			// Validate return type
			String specReturn = specInfo.getReturnType();
			String returnType = docInfo.getReturnType();
			String returnDescription = docInfo.getReturnDoc();

			if ((returnType == null || returnType.isEmpty()) && (specReturn != null && !specReturn.isEmpty() && !"void".equals(specReturn)))
			{
				functionHasIssues = true;
				System.err.println(packageName + " ::: " + displayName + " ::: " + functionName +
					" ::: return - Missing return type");
			}
			if ((returnType != null) && !"void".equals(returnType) && (returnDescription == null || returnDescription.isEmpty()))
			{
				functionHasIssues = true;
				System.err.println(packageName + " ::: " + displayName + " ::: " + functionName +
					" ::: return - Missing return description");
			}
			if (!areTypesEquivalent(types, specReturn, returnType, componentName))
			{
				functionHasIssues = true;
				System.err.println(packageName + " ::: " + displayName + " ::: " + functionName +
					" ::: return type mismatch (custom type in doc?). Spec: " + specReturn + ", Doc: " + returnType);
			}

			if (functionHasIssues)
			{
				componentHasIssues = true;
				totalFunctionsWithIssues++;
			}
		}

		// Increment counters if there were issues
		if (componentHasIssues)
		{
			if (processedComponents.add(displayName))
			{
				totalComponentsWithIssues++;
			}
			if (processedPackages.add(packageName))
			{
				totalPackagesWithIssues++;
			}
		}
	}

	private boolean areTypesEquivalent(JSONObject myTypes, String specType, String docType, String componentName)
	{
		if (specType == null || docType == null) return false;

		String normalizedSpecType = normalizeType(myTypes, specType, componentName);

		if ("Object".equalsIgnoreCase(normalizedSpecType) && (isObject(docType) || isJSObject(docType)))
		{
			return true;
		}

		if (!normalizedSpecType.contains("CustomType") && (docType.contains("|")))
		{
			boolean result = true;
			String normalizedDocType = docType;

			if (normalizedDocType.startsWith("Array<") && normalizedDocType.endsWith(">"))
			{
				normalizedDocType = normalizedDocType.substring(6, normalizedDocType.length() - 1);
				if (!(normalizedSpecType.startsWith("Array<") && normalizedSpecType.endsWith(">")))
				{
					return false;
				}
				else
				{
					normalizedSpecType = normalizedSpecType.substring(6, normalizedSpecType.length() - 1);
					if (!normalizedSpecType.equals("Object"))
					{
						return false; //when doc show multiple object type options (Strin|Number) the (normalized) spec type must be Object
					}
				}
			}

			String[] docTypes = normalizedDocType.split("\\|");

			for (String type : docTypes)
			{
				if (!isObject(type))
				{
					return false;
				}
			}
			return result;
		}
		return normalizedSpecType.equalsIgnoreCase(docType);
	}

	private boolean isObject(String type)
	{
		if (type == null || type.isEmpty())
		{
			return false;
		}

		// List of standard JavaScript object types
		Set<String> standardTypes = Set.of(
			"String", "Number", "Boolean", "Object", "Array", "Date", "RegExp", "Function", "Symbol", "BigInt", "null");

		if (type.startsWith("CustomType")) return true;

		return standardTypes.contains(type.trim());
	}


	private boolean isJSObject(String type)
	{
		if (type == null || type.isEmpty())
		{
			return false;
		}

		// List of standard JavaScript object types
		Set<String> standardTypes = Set.of("JSUpload"); // add other types as needed

		if (type.startsWith("CustomType")) return true;

		return standardTypes.contains(type.trim());
	}

	private String normalizeType(JSONObject myTypes, String type, String componentName)
	{
		boolean isArray = false;
		String normalizedSpecType = type;
		if (normalizedSpecType.endsWith("[]"))
		{
			isArray = true;
			normalizedSpecType = normalizedSpecType.substring(0, normalizedSpecType.length() - 2);
		}

		switch (normalizedSpecType.toLowerCase())
		{
			case "int" :
			case "integer" :
			case "float" :
			case "double" :
				normalizedSpecType = "Number";
				break;
			case "boolean" :
			case "bool" :
				normalizedSpecType = "Boolean";
				break;
			case "record" :
				normalizedSpecType = "JSRecord";
				break;
			case "foundset" :
				normalizedSpecType = "JSFoundset";
				break;
			case "dataset" :
				normalizedSpecType = "JSDataset";
				break;
			case "event" :
			case "jsevent" :
				normalizedSpecType = "JSEvent";
				break;
			case "tagstring" :
				normalizedSpecType = "String";
				break;
			case "string..." :
				normalizedSpecType = "String...";
				break;
			default :
				if (componentName != null && myTypes != null)
				{
					normalizedSpecType = normalizeTypeForComponent(myTypes, normalizedSpecType, componentName);
				}
				else if (type.length() > 1)
					normalizedSpecType = normalizedSpecType.substring(0, 1).toUpperCase() + normalizedSpecType.substring(1);
				else
					normalizedSpecType = type;
		}


		if (!normalizedSpecType.startsWith("CustomType"))
		{
			normalizedSpecType = normalizedSpecType.substring(0, 1).toUpperCase() + normalizedSpecType.substring(1);
		}
		if (isArray)
		{
			normalizedSpecType = "Array<" + normalizedSpecType + ">";
		}

		return normalizedSpecType;
	}

	private String normalizeTypeForComponent(JSONObject myTypes, String type, String componentName)
	{
		JSONObject customType = myTypes.optJSONObject(type);
		if (customType != null)
		{
			return "CustomType<" + componentName + "." + type + ">";
		}
		return type;
	}

	private Map<String, FunctionInfo> getSpecFunctions(File specFile)
	{
		Map<String, FunctionInfo> specFunctions = new HashMap<>();

		try
		{
			String specContent = Files.readString(specFile.toPath());
			JSONObject specJson = new JSONObject(specContent);
			JSONObject api = specJson.optJSONObject("api");

			if (api != null)
			{
				for (String functionName : api.keySet())
				{
					JSONObject functionSpec = api.getJSONObject(functionName);
					JSONArray paramsArray = functionSpec.optJSONArray("parameters");
					String returnType = functionSpec.optString("returns", "void");
					String deprecatedStr = functionSpec.optString("deprecated", "");
					boolean deprecated = deprecatedStr.isEmpty() ? false : true;

					List<Parameter> parameters = new ArrayList<>();
					if (paramsArray != null)
					{
						for (int i = 0; i < paramsArray.length(); i++)
						{
							JSONObject param = paramsArray.getJSONObject(i);
							Object typeObj = param.get("type");
							String type;
							if (typeObj instanceof String)
							{
								type = (String)typeObj;
							}
							else if (typeObj instanceof JSONObject)
							{
								// For JSONObject, extract and format as needed (e.g., {"type": "dataset"} becomes "dataset")
								type = ((JSONObject)typeObj).optString("type", "unknown");
							}
							else
							{
								type = "unknown"; // Fallback for unexpected formats
							}

							parameters.add(new Parameter(
								param.getString("name"),
								type,
								null,
								param.optString("doc", ""),
								param.optBoolean("optional", false)));
						}
					}

					specFunctions.put(functionName, new FunctionInfo(parameters, returnType, "", deprecated));
				}
			}
		}
		catch (IOException | JSONException e)
		{
			System.err.println("Error reading spec file: " + specFile.getName() + " - " + e.getMessage());
		}

		return specFunctions;
	}


	private Map<String, FunctionInfo> getDocFunctions(File docFile)
	{

		Map<String, FunctionInfo> docFunctions = new HashMap<>();
		if (docFile == null)
		{
			System.err.println("\u001B[33mDoc file is null. Skipping...\u001B[0m");
			totalComponentsWithoutDoc++;
			return Collections.emptyMap();
		}
		try
		{
			String docContents = FileUtils.readFileToString(docFile, Charset.forName("UTF8"));
			Pattern functionPattern = Pattern.compile("/\\*\\*([\\s\\S]*?)\\*/\\s*function\\s+(\\w+)\\s*\\(");
			Matcher matcher = functionPattern.matcher(docContents);

			while (matcher.find())
			{
				String jsDoc = matcher.group(1).trim();
				String functionName = matcher.group(2).trim();

				JSDocTags jsDocTags = jsDocParser.parse(jsDoc, 0);
				String returnType = "void"; // Default return type
				String returnDoc = null; // No default description here
				List<Parameter> parameters = new ArrayList<>();

				for (int i = 0; i < jsDocTags.size(); i++)
				{
					JSDocTag tag = jsDocTags.get(i);
					if (tag.name().equals(JSDocTag.RETURN))
					{
						//here the extractJSTagElements is considering parameter {Type} name description
						Pattern pattern = Pattern.compile("^\\{([^}]+)\\}\\s*(.*)$");
						JSONObject tagElements = extractReturnTagElements(tag.value(), pattern);
						returnType = tagElements.optString("type", "void");
						returnDoc = (tagElements.optString("name", "") + " " + tagElements.optString("doc", "")).trim();
						returnDoc = returnDoc.length() > 0 ? returnDoc : null;
					}
					else if (tag.name().equals(JSDocTag.PARAM))
					{
						Pattern pattern = Pattern.compile("\\{([^}]+)\\}\\s+(\\S+)\\s*(.*)");
						JSONObject tagElements = extractParamTagElements(tag.value(), pattern);
						parameters.add(new Parameter(
							tagElements.optString("name"),
							tagElements.optString("type"),
							tagElements.optString("docType"),
							tagElements.optString("doc", ""),
							tagElements.optBoolean("optional", false)));
					}

				}
				// Add the parsed function to the map
				docFunctions.put(functionName, new FunctionInfo(parameters, returnType, returnDoc, false));
			}
		}
		catch (IOException e)
		{
			System.err.println("Error reading doc file: " + docFile.getAbsolutePath() + " - " + e.getMessage());
		}

		return docFunctions;
	}

	private JSONObject extractReturnTagElements(String input, Pattern pattern)
	{

		// Define a regex pattern to match the optional type and description
//		 // Matches {type} followed by description


		Matcher matcher = pattern.matcher(input.trim());

		String type = "";
		String doc = "";

		if (matcher.find())
		{
			// Extract the type and description if present
			type = matcher.group(1) != null ? matcher.group(1).trim() : "";
			doc = matcher.group(2) != null ? matcher.group(2).trim() : "";
		}
		else
		{
			// Fallback: If no type is found, the entire input is treated as description
			doc = input.trim();
		}

		// Capitalize the first letter of the description if it exists
		if (!doc.isEmpty())
		{
			doc = doc.substring(0, 1).toUpperCase() + doc.substring(1);
		}

		// Create a JSON object to return the results
		JSONObject result = new JSONObject();
		result.put("type", type);
		result.put("doc", doc);

		return result;
	}

	private JSONObject extractParamTagElements(String input, Pattern pattern)
	{

//		 // Match {type} name description


		Matcher matcher = pattern.matcher(input.trim());
		JSONObject result = new JSONObject();
		String type = "";
		String name = "";
		String doc = "";

		if (matcher.find())
		{
			// Extract matches safely
			type = matcher.group(1) != null ? matcher.group(1).trim() : "";
			name = matcher.group(2) != null ? matcher.group(2).trim() : "";
			doc = matcher.group(3) != null ? matcher.group(3).trim() : "";
		}
		else
		{
			// Handle cases where {type} is missing
			String[] parts = input.trim().split("\\s+", 2); // Split into at most 2 parts
			if (parts.length > 0)
			{
				name = parts[0].trim(); // First word is the name
			}
			if (parts.length > 1)
			{
				doc = parts[1].trim(); // Remaining is the documentation
			}
		}

		// Capitalize the first letter of the documentation
		if (!doc.isEmpty())
		{
			doc = doc.substring(0, 1).toUpperCase() + doc.substring(1);
		}

		result.put("name", name);
		result.put("type", type);
		result.put("doc", doc);

		return result;
	}

	public class FunctionInfo
	{
		private final List<Parameter> parameters;
		private final String returnType;
		private final String returnDoc; // Added for return description
		private final boolean deprecated;

		public FunctionInfo(List<Parameter> parameters, String returnType, String returnDoc, boolean deprecated)
		{
			this.parameters = parameters;
			this.returnType = returnType;
			this.returnDoc = returnDoc; // Set return description
			this.deprecated = deprecated;
		}

		public List<Parameter> getParameters()
		{
			return parameters;
		}

		public String getReturnType()
		{
			return returnType;
		}

		public String getReturnDoc()
		{ // Getter for return description
			return returnDoc;
		}

		public boolean isDeprecated()
		{
			return deprecated;
		}

		@Override
		public String toString()
		{
			return "Parameters: " + parameters + ", Return Type: " + returnType + ", Return Doc: " + returnDoc;
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
//				System.err.println("* skipping " + (service ? "service" : "component") + " " + displayName + " because it is deprecated.");
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

				String relativePath = file.getPath().substring(file.getPath().indexOf("reference/"));
				relativePath = relativePath.replace('\\', '/');

				// Check if the relative path is in the summary but not in generated files
				if (!summaryPaths.contains(relativePath))
				{
					missingMdFiles.add(relativePath);
					//System.err.println("\033[38;5;214mMissing file in summary: " + relativePath + "\033[0m");
				}


				componentTemplate.process(root, out);
				duplicateTracker.trackFile(file.getName(), file.toString());
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

				/**
				 * Quick convert of HTML to String for use in the template.
				 */
				root.put("MD", (TemplateMethodModelEx)((params) -> htmlToMarkdownConverter.apply((String)DeepUnwrap.unwrap((TemplateModel)params.get(0)))));

				root.put("packageName", packageName);
				root.put("instance", this);
				root.put("packageDisplayName", packageDisplayName);
				root.put("packageDescription", packageDescription);
				root.put("packageType", packageType);
				root.put("allWebObjectsOfCurrentPackage", allWebObjectsOfCurrentPackage);

				packageTemplate.process(root, out);
				duplicateTracker.trackFile(file.getName(), file.toString());

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

	private static class MarkdownContentAppender
	{
		@SuppressWarnings("boxing")
		private static final java.util.Set<Character> specialChars = java.util.Set.of(
			'\\', '`', '*', '_', '{', '}', '[', ']', '<', '>', '(', ')', '#', '+', '-', '.', '!', '|');
		private final StringBuilder result;

		MarkdownContentAppender(int estimatedContentLength)
		{
			result = new StringBuilder(estimatedContentLength);
		}

		/**
		 * Adds content that is plain (no markdown or special syntax in it on purpose; but it might have chars that need to be escaped...).<br/><br/>
		 *
		 * If isCodeOrPreContent = true then it will not escape special characters when adding the inbetweenTagsContent (only backticks would need escaping there, but that is already done elsewhere by modifying the start/end backtick count of the block).<br/>
		 * Otherwise, the inbetweenTagsContent will be escaped.
		 *
		 * @param inbetweenTagsContent
		 * @param isCodeOrPreContent
		 */
		public void appendInBetweenTagContent(String inbetweenTagsContent, boolean isCodeOrPreContent)
		{
			if (isCodeOrPreContent) appendWithoutEscaping(inbetweenTagsContent);
			else appendAfterEscapingMarkdownSpecialChars(inbetweenTagsContent);
		}

		public char charAt(int index)
		{
			return result.charAt(index);
		}

		public Object substring(int start)
		{
			return result.substring(start);
		}

		public int length()
		{
			return result.length();
		}

		public MarkdownContentAppender appendWithoutEscaping(String contentToAppend)
		{
			result.append(contentToAppend);
			return this;
		}

		public MarkdownContentAppender appendAfterEscapingMarkdownSpecialChars(String contentToAppend)
		{
			result.append(escapeMarkdownSpecialChars(contentToAppend));
			return this;
		}

		private Object escapeMarkdownSpecialChars(String contentToAppend)
		{
			StringBuilder escaped = null;
			for (int i = 0; i < contentToAppend.length(); i++)
			{
				if (specialChars.contains(Character.valueOf(contentToAppend.charAt(i))))
				{
					if (escaped == null)
					{
						escaped = new StringBuilder(contentToAppend.length() + 10);
						escaped.append(contentToAppend.subSequence(0, i));
					}
					escaped.append('\\');
					escaped.append(contentToAppend.charAt(i));
				}
				else if (escaped != null) escaped.append(contentToAppend.charAt(i));
			}

			return escaped == null ? contentToAppend : escaped.toString();
		}

		@Override
		public String toString()
		{
			return result.toString();
		}

	}

	private static class CodeBacktickState
	{

		public final int maxContinousBacktickCount;
		public final boolean startsWithBacktick;
		public final boolean endsWithBacktick;

		public CodeBacktickState(int maxContinousBacktickCount, boolean startsWithBacktick, boolean endsWithBacktick)
		{
			this.maxContinousBacktickCount = maxContinousBacktickCount;
			this.startsWithBacktick = startsWithBacktick;
			this.endsWithBacktick = endsWithBacktick;
		}

	}

}
