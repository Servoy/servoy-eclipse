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
import java.util.Stack;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.jar.Manifest;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
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
import org.sablo.util.TextUtils;
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

	private String processFunctionJSDoc(String jsDocComment)
	{
		if (jsDocComment == null) return null;

		return processDescription(0, jsDocComment);
	}

	/**
	 * Same as {@link #turnJSDocIntoMarkdown(String, int)} with indentSpaces set to 0.
	 */
	public static String turnJSDocIntoMarkdown(String doc)
	{
		return turnJSDocIntoMarkdown(doc, 0);
	}

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
	 * <p>Also things such as @param or @return need to be styled properly. @example and it's content as well...</p>
	 * @param indentLevel the number of indent levels (1 lvl -> 4 spaces) that each line should be indented with (in case this content will be used as part of a
	 *                    list item, 4 spaces means one level on indentation in the lists (so according to one of the items); this does
	 *                    NOT affect the first line, as the caller uses that as a pre-indented list item
	 */
	public static String turnJSDocIntoMarkdown(String doc, int indentLevel)
	{
		// DO ADD NEW UNIT tests to SpecMarkdownGeneratorTest from the test repo for every new bug encountered / tweak made

		if (doc == null) return null;

		Pattern splitPattern = Pattern.compile(
			"(?<splitToken>\\@param|\\@example|\\@return|\\@deprecated|<br>|<br/>|<pre>|</pre>|<code>|</code>|<b>|</b>|<i>|</i>|<ul>|</ul>|<ol>|</ol>|<li>|</li>|<p>|</p>)");
		Matcher matcher = splitPattern.matcher(doc);

		List<String> matchedTokens = new ArrayList<>();
		List<String> betweenMatches = new ArrayList<>();
		int lastGroupMatchEndIndex = 0;
		while (matcher.find())
		{
			MatchResult matchResult = matcher.toMatchResult();
			betweenMatches.add(doc.substring(lastGroupMatchEndIndex, matchResult.start()));
			matchedTokens.add(matchResult.group());
			lastGroupMatchEndIndex = matchResult.end();
		}

		betweenMatches.add(doc.substring(lastGroupMatchEndIndex));

		int currentIndentLevel = indentLevel;

		// we could have used 3rd party libs for this translation... but those don't have this "indentLevel" that we need when indenting sub-properties of custom types in the docs...

		MarkdownContentAppender result = new MarkdownContentAppender(doc.length());

		boolean shouldTrimLeadingTheInBetweenContent = (result.length() == 0); // so if it has a non-whitespace char already in the first line, then no left trimming needs to happen here anymore
		Stack<Boolean> listLevelOrderedOrNot = new Stack<>();
		boolean insideExampleSectionThatAutoAddsCodeBlock = false;
		CodeBacktickState codeBacktickState = null;
		boolean firstAtSomething = true; // it refers to whether or not an @param, @return, @example, @... was processed yet or not; we want a bit of space between the description of an API an the @ things

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
			// NOTE: remember neither the content before this token not the token itself are yet added; token is checked again later,
			// after preceding content is actually added
			switch (token)
			{
				case "<br>", "<br/>" :
				case "@param", "@return", "@deprecated", "@exampleDoNotAutoAddCodeBlock" :
				case "<p>", "</p>" :
				case "<ul>", "</ul>", "<ol>", "</ol>", "<li>", "</li>" :
					trimTrailingInPrecedingInbetweenContent = true;
					break;
				case "</pre>" :
				case "</code>" :
					preserveMultipleWhiteSpaces = true;
					break;
				case "<pre>", "@example" :
					trimTrailingInPrecedingInbetweenContent = true; // INTENTIONAL fall-through to next 'case'; so no break here
					//$FALL-THROUGH$
				case "<code>" :
					// any tokens inside these blocks need to go into "betweenMatches"
					// "i" also advances as needed
					String closingTagStartsWith = ("@example".equals(token) ? "@" : "</" + token.substring(1));
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
							token = "<pre>";
							if ("</code>".equals(matchedTokens.get(i + 1))) matchedTokens.set(i + 1, "</pre>"); // otherwise it's malformed HTML I guess

							// now do what this switch would have done for <code>
							trimTrailingInPrecedingInbetweenContent = true;
						}
					}
					break;
				default :
			}

			if (token.startsWith("@") && insideExampleSectionThatAutoAddsCodeBlock) preserveMultipleWhiteSpaces = true; // auto generated <code> equivalent inside example section is ending now after content will be added to it

			// ok now do add the "betweenMatches" content that was before 'token'
			if (!preserveMultipleWhiteSpaces) inbetween = inbetween.replaceAll("\s+", " ");

			if (shouldTrimLeadingTheInBetweenContent)
			{
				inbetween = inbetween.stripLeading();
				shouldTrimLeadingTheInBetweenContent = (inbetween.length() == 0);
			}

			if (trimTrailingInPrecedingInbetweenContent)
			{
				inbetween = inbetween.stripTrailing();
			}
			result.appendInBetweenTagContent(inbetween, codeBacktickState != null);

			if (token.startsWith("@") && insideExampleSectionThatAutoAddsCodeBlock)
			{
				// the example section has finished
				insideExampleSectionThatAutoAddsCodeBlock = false;

				if (codeBacktickState.maxContinousBacktickCount > 2)
					result.appendWithoutEscaping("\n" + "`".repeat(codeBacktickState.maxContinousBacktickCount + 1));
				else result.appendWithoutEscaping("\n```");
				codeBacktickState = null;

				nextLinePlusIndent(result, currentIndentLevel);
				shouldTrimLeadingTheInBetweenContent = true;
			}

			// now append anything that the token needs to add
			switch (token)
			{
				case "<br>", "<br/>" :
					result.appendWithoutEscaping("  ");
					nextLinePlusIndent(result, currentIndentLevel);
					shouldTrimLeadingTheInBetweenContent = true;
					break;
				case "@param", "@example", "@return", "@deprecated", "@exampleDoNotAutoAddCodeBlock" :
					if (firstAtSomething)
					{
						// add an empty line - to have a bit of visual separation between the description of an API and the @param, @return etc.
						if (result.length() >= 2 && !"\n\n".equals(result.substring(result.length() - 2)))
						{
							result.appendWithoutEscaping("\n");
							nextLinePlusIndent(result, currentIndentLevel);
						}
						firstAtSomething = false;
						shouldTrimLeadingTheInBetweenContent = true;
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
						shouldTrimLeadingTheInBetweenContent = true;
					}
					else shouldTrimLeadingTheInBetweenContent = false;

					if ("@example".equals(token))
					{
						nextLinePlusIndent(result, currentIndentLevel);
						String contentAfterExampleTag = betweenMatches.get(i + 1);

						// prepare to 'escape' backticks in code/pre content; in markdown this means modifying the start and end special meaning backtick char count to a higher value then the one in the pre/code content...
						// also if the code / pre content starts or ends with backtick, then some spaces need to be added between that and the special meaning backticks (start/end tags of the code/pre section)
						codeBacktickState = countContinuousBackticks(contentAfterExampleTag);

						if (codeBacktickState.maxContinousBacktickCount > 2)
							result.appendWithoutEscaping("`".repeat(codeBacktickState.maxContinousBacktickCount + 1) + "js\n");
						else result.appendWithoutEscaping("```js\n");

						insideExampleSectionThatAutoAddsCodeBlock = true;

						// if it's like '@example a = a + 1' then we want to left trim what follows after the @example tag (so that one space) before adding the rest in a code block
						// but if it's a multi-line code example
						// '@example
						//     a = a + 1;
						//     callMe(a);
						//  @return something
						// then we don't want to left trim what follows after "@example" (that would remove the 4 spaces as well), just the \n
						int firstBackslashNPosition = contentAfterExampleTag.indexOf('\n');
						if (firstBackslashNPosition == contentAfterExampleTag.length() - 1)
						{
							// one line
							shouldTrimLeadingTheInBetweenContent = true;
						}
						else
						{
							if (firstBackslashNPosition >= 0) betweenMatches.set(i + 1, contentAfterExampleTag.substring(firstBackslashNPosition + 1));
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
					shouldTrimLeadingTheInBetweenContent = true;
					break;
				case "</p>" :
					if (i < matchedTokens.size() - 1 || betweenMatches.get(betweenMatches.size() - 1).trim().length() > 0)
					{
						nextLinePlusIndent(result, currentIndentLevel);
						nextLinePlusIndent(result, currentIndentLevel);
					}
					shouldTrimLeadingTheInBetweenContent = true;
					break;
				case "<ul>" :
					if (!endsWithNewline(result, currentIndentLevel)) nextLinePlusIndent(result, currentIndentLevel);
					listLevelOrderedOrNot.push(Boolean.FALSE);
					shouldTrimLeadingTheInBetweenContent = true;
					break;
				case "<ol>" :
					if (!endsWithNewline(result, currentIndentLevel)) nextLinePlusIndent(result, currentIndentLevel);
					listLevelOrderedOrNot.push(Boolean.TRUE);
					shouldTrimLeadingTheInBetweenContent = true;
					break;
				case "<li>" :
					if (!endsWithNewline(result, currentIndentLevel)) nextLinePlusIndent(result, currentIndentLevel);
					result.appendWithoutEscaping((listLevelOrderedOrNot.peek() != null && listLevelOrderedOrNot.peek().booleanValue() ? "1. " : " - "));
					currentIndentLevel++;
					shouldTrimLeadingTheInBetweenContent = true;
					break;
				case "</li>" :
					currentIndentLevel--;
					break;
				case "</ul>", "</ol>" :
					if ((i + 1 < matchedTokens.size() && !"</li>".equals(matchedTokens.get(i + 1)) && !"</ul>".equals(matchedTokens.get(i + 1)) &&
						!"</ol>".equals(matchedTokens.get(i + 1))) || (i + 1 == matchedTokens.size()))
					{
						nextLinePlusIndent(result, currentIndentLevel);
						shouldTrimLeadingTheInBetweenContent = true;
					}
					listLevelOrderedOrNot.pop();
					break;
				case "<code>" :
					shouldTrimLeadingTheInBetweenContent = false;

					// prepare to 'escape' backticks in code/pre content; in markdown this means modifying the start and end special meaning backtick char count to a higher value then the one in the pre/code content...
					// also if the code / pre content starts or ends with backtick, then some spaces need to be added between that and the special meaning backticks (start/end tags of the code/pre section)
					codeBacktickState = countContinuousBackticks(betweenMatches.get(i + 1));

					if (codeBacktickState.maxContinousBacktickCount > 0)
						result.appendWithoutEscaping(
							"`".repeat(codeBacktickState.maxContinousBacktickCount + 1) + (codeBacktickState.startsWithBacktick ? " " : ""));
					else result.appendWithoutEscaping("`");

					break;
				case "</code>" :
					if (codeBacktickState.maxContinousBacktickCount > 0)
						result.appendWithoutEscaping(
							(codeBacktickState.endsWithBacktick ? " " : "") + "`".repeat(codeBacktickState.maxContinousBacktickCount + 1));
					else result.appendWithoutEscaping("`");
					codeBacktickState = null;
					break;
				case "<pre>" :
					if (!endsWithMarkdownNewline(result, currentIndentLevel)) nextLinePlusIndent(result, currentIndentLevel);

					// prepare to 'escape' backticks in code/pre content; in markdown this means modifying the start and end special meaning backtick char count to a higher value then the one in the pre/code content...
					// also if the code / pre content starts or ends with backtick, then some spaces need to be added between that and the special meaning backticks (start/end tags of the code/pre section)
					String contentAfterPre = betweenMatches.get(i + 1);
					codeBacktickState = countContinuousBackticks(contentAfterPre);
					if (codeBacktickState.maxContinousBacktickCount > 2)
						result.appendWithoutEscaping("`".repeat(codeBacktickState.maxContinousBacktickCount + 1) + "js\n");
					else result.appendWithoutEscaping("```js\n");

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

					shouldTrimLeadingTheInBetweenContent = false;
					break;
				case "</pre>" :
					if (codeBacktickState.maxContinousBacktickCount > 2)
						result.appendWithoutEscaping("\n" + "`".repeat(codeBacktickState.maxContinousBacktickCount + 1));
					else result.appendWithoutEscaping("\n```");
					codeBacktickState = null;

					nextLinePlusIndent(result, currentIndentLevel);
					break;
				case "<i>", "</i>" :
					result.appendWithoutEscaping("_");
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
		if (!insideExampleSectionThatAutoAddsCodeBlock) inbetween = inbetween.replaceAll("\s+", " ");
		if (shouldTrimLeadingTheInBetweenContent)
		{
			inbetween = inbetween.stripLeading();
			shouldTrimLeadingTheInBetweenContent = (inbetween.length() == 0);
		}
		if (!insideExampleSectionThatAutoAddsCodeBlock) inbetween = inbetween.stripTrailing();

		result.appendInBetweenTagContent(inbetween, codeBacktickState != null);

		if (insideExampleSectionThatAutoAddsCodeBlock)
		{
			// the example section has finished
			insideExampleSectionThatAutoAddsCodeBlock = false;

			if (codeBacktickState.maxContinousBacktickCount > 2)
				result.appendWithoutEscaping("\n" + "`".repeat(codeBacktickState.maxContinousBacktickCount + 1));
			else result.appendWithoutEscaping("\n```");
			codeBacktickState = null;

			nextLinePlusIndent(result, currentIndentLevel);
		}

		return result.toString();
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
		if (docGenerator.shouldTurnAPIDocsIntoMarkdown()) doc = turnJSDocIntoMarkdown(doc, indentLevel);
		return doc;
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
				map.put(type, makeMap(types.optJSONObject(type), this::createPropertyIndented));
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
		return "../../packages/" + (service ? "services/" : "components/") + packageDisplayName.trim().replace("&", "and").replace(' ', '-').toLowerCase() +
			".md";
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
				case "tagstring" -> "../../../servoy-developer/property\\_types.md#tagstring";
				case "titlestring" -> "../../../servoy-developer/property\\_types.md#titlestring";
				case "styleclass" -> "../../../servoy-developer/property\\_types.md#styleclass";
				case "protected" -> "../../../servoy-developer/property\\_types.md#protected";
				case "enabled" -> "../../../servoy-developer/property\\_types.md#protected";
				case "readonly" -> "../../../servoy-developer/property\\_types.md#protected";
				case "variant" -> "../../../servoy-developer/property\\_types.md#variant";
				case "visible" -> "../../../servoy-developer/property\\_types.md#visible";
				case "tabseq" -> "../../../servoy-developer/property\\_types.md#tabseq";
				case "format" -> "../../../servoy-developer/property\\_types.md#format";
				case "color" -> "../../../servoy-developer/property\\_types.md#color";
				case "map" -> "../../../servoy-developer/property\\_types.md#map";
				case "scrollbars" -> "../../../servoy-developer/property\\_types.md#scrollbars";
				case "dataprovider" -> "../../../servoy-developer/property\\_types.md#dataprovider";
				case "${dataprovidertype}" -> "../../../servoy-developer/property\\_types.md#dataprovider";
				case "relation" -> "../../../servoy-developer/property\\_types.md#relation";
				case "form" -> "../../../servoy-developer/property\\_types.md#form";
				case "formscope" -> "../../../servoy-developer/property\\_types.md#form";
				case "formcomponent" -> "../../../servoy-developer/property\\_types.md#formcomponent";
				case "record" -> "../../../servoy-developer/property\\_types.md#record";
				case "foundset" -> "../../../servoy-developer/property\\_types.md#foundset";
				case "foundsetref" -> "../../../servoy-developer/property\\_types.md#foundsetref";
				case "dataset" -> "../../../servoy-developer/property\\_types.md#dataset";
				case "function" -> "../../../servoy-developer/property\\_types.md#function";
				case "clientfunction" -> "../../../servoy-developer/property\\_types.md#clientfunction";
				case "media" -> "../../../servoy-developer/property\\_types.md#media";
				case "valuelist" -> "../../../servoy-developer/property\\_types.md#valuelist";
				case "labelfor" -> "../../../servoy-developer/property\\_types.md#labelfor";

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
