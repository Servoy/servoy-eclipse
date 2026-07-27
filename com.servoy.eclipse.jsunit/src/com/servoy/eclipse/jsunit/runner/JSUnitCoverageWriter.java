/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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
package com.servoy.eclipse.jsunit.runner;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Serialises JSUnit line-coverage data to a JSON file after a test run.
 * <p>
 * The output format is:
 * <pre>
 * {
 *   "solution": "mySolution",
 *   "timestamp": "2026-06-05T10:00:00Z",
 *   "summary": { "coveredLines": 142, "uncoveredLines": 38 },
 *   "scopes": [ { "name": "myScope", "functions": [ { "name": "fn", "coveredLines": [1,2], "uncoveredLines": [3] } ] } ],
 *   "forms":  [ { "name": "myForm", "functions": [ ... ] } ]
 * }
 * </pre>
 * JsUnit internal library scripts (JsUtil.js, JsUnit.js, JsUnitToJava.js, solutionTestSuite.js)
 * are excluded from the output.
 *
 * @author SVY-21131
 */
public class JSUnitCoverageWriter
{
	/** Source file names belonging to JsUnit infrastructure â excluded from coverage output. */
	private static final Set<String> EXCLUDED_SOURCES = Set.of(
		"JsUtil.js", "JsUnit.js", "JsUnitToJava.js",
		ApplicationJSTestSuite.SOLUTION_TEST_JS_NAME,
		"suiteName", "anonymous", "Getting test tree");

	/**
	 * Writes coverage data to a JSON file at the given path.
	 *
	 * @param lineNumbers   hit-count map from {@link JSUnitSuite#getLineNumbers()} â may be null
	 * @param reachableLines reachable-line map from {@link JSUnitSuite#getReachableLines()} â may be null
	 * @param solutionName  name of the solution under test (used as metadata in the JSON)
	 * @param outputPath    absolute path for the output .json file
	 */
	public static void write(
		Map<String, Map<String, Map<String, Map<Integer, Integer>>>> lineNumbers,
		Map<String, Map<String, Set<Integer>>> reachableLines,
		String solutionName,
		String outputPath)
	{
		if (outputPath == null || outputPath.isEmpty()) return;
		if (lineNumbers == null && reachableLines == null) return;

		try
		{
			String json = buildJson(lineNumbers, reachableLines, solutionName);
			try (BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(outputPath), StandardCharsets.UTF_8)))
			{
				writer.write(json);
			}
			ServoyLog.logInfo("JSUnit coverage written to: " + outputPath);
		}
		catch (IOException e)
		{
			ServoyLog.logError("Failed to write JSUnit coverage JSON to " + outputPath, e);
		}
	}

	private static String buildJson(
		Map<String, Map<String, Map<String, Map<Integer, Integer>>>> lineNumbers,
		Map<String, Map<String, Set<Integer>>> reachableLines,
		String solutionName)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("  \"solution\": ").append(jsonString(solutionName)).append(",\n");
		sb.append("  \"timestamp\": ").append(jsonString(Instant.now().toString())).append(",\n");

		// ---- collect per-type data ----
		List<ScopeEntry> scopes = new ArrayList<>();
		List<ScopeEntry> forms = new ArrayList<>();

		if (lineNumbers != null)
		{
			for (Map.Entry<String, Map<String, Map<String, Map<Integer, Integer>>>> typeEntry : lineNumbers.entrySet())
			{
				String type = typeEntry.getKey(); // "scopes" or "forms"
				for (Map.Entry<String, Map<String, Map<Integer, Integer>>> scopeEntry : typeEntry.getValue().entrySet())
				{
					String scopeName = scopeEntry.getKey();
					Map<String, Set<Integer>> reachableForSource = findReachableForScope(reachableLines, type, scopeName);

					List<FunctionEntry> functions = new ArrayList<>();
					for (Map.Entry<String, Map<Integer, Integer>> funcEntry : scopeEntry.getValue().entrySet())
					{
						String funcName = funcEntry.getKey();
						Set<Integer> hitLines = funcEntry.getValue().keySet();

						Set<Integer> reachableForFunc = reachableForSource != null ? reachableForSource.get(funcName) : null;
						List<Integer> coveredList = new ArrayList<>(hitLines);
						Collections.sort(coveredList);

						List<Integer> uncoveredList = new ArrayList<>();
						if (reachableForFunc != null)
						{
							for (Integer line : reachableForFunc)
							{
								if (!hitLines.contains(line))
								{
									uncoveredList.add(line);
								}
							}
							Collections.sort(uncoveredList);
						}
						functions.add(new FunctionEntry(funcName, coveredList, uncoveredList));
					}
					functions.sort((a, b) -> a.name.compareTo(b.name));

					ScopeEntry entry = new ScopeEntry(scopeName, functions);
					if ("forms".equals(type)) forms.add(entry);
					else scopes.add(entry);
				}
			}
		}

		scopes.sort((a, b) -> a.name.compareTo(b.name));
		forms.sort((a, b) -> a.name.compareTo(b.name));

		// ---- summary ----
		int totalCovered = 0, totalUncovered = 0;
		for (ScopeEntry se : scopes)
		{
			for (FunctionEntry fe : se.functions)
			{
				totalCovered += fe.coveredLines.size();
				totalUncovered += fe.uncoveredLines.size();
			}
		}
		for (ScopeEntry se : forms)
		{
			for (FunctionEntry fe : se.functions)
			{
				totalCovered += fe.coveredLines.size();
				totalUncovered += fe.uncoveredLines.size();
			}
		}

		sb.append("  \"summary\": { \"coveredLines\": ").append(totalCovered)
			.append(", \"uncoveredLines\": ").append(totalUncovered).append(" },\n");

		// ---- debug: raw reachable source names for diagnostics ----
		sb.append("  \"_debug_reachableSources\": [");
		if (reachableLines != null)
		{
			boolean first = true;
			for (String srcName : reachableLines.keySet())
			{
				if (!first) sb.append(", ");
				sb.append(jsonString(srcName));
				first = false;
			}
		}
		sb.append("],\n");

		// ---- scopes array ----
		sb.append("  \"scopes\": ");
		appendScopeArray(sb, scopes);
		sb.append(",\n");

		// ---- forms array ----
		sb.append("  \"forms\": ");
		appendScopeArray(sb, forms);
		sb.append("\n");

		sb.append("}\n");
		return sb.toString();
	}

	private static void appendScopeArray(StringBuilder sb, List<ScopeEntry> entries)
	{
		sb.append("[\n");
		for (int i = 0; i < entries.size(); i++)
		{
			ScopeEntry se = entries.get(i);
			sb.append("    {\n");
			sb.append("      \"name\": ").append(jsonString(se.name)).append(",\n");
			sb.append("      \"functions\": [\n");
			for (int j = 0; j < se.functions.size(); j++)
			{
				FunctionEntry fe = se.functions.get(j);
				sb.append("        {\n");
				sb.append("          \"name\": ").append(jsonString(fe.name)).append(",\n");
				sb.append("          \"coveredLines\": ").append(intListJson(fe.coveredLines)).append(",\n");
				sb.append("          \"uncoveredLines\": ").append(intListJson(fe.uncoveredLines)).append("\n");
				sb.append("        }");
				if (j < se.functions.size() - 1) sb.append(",");
				sb.append("\n");
			}
			sb.append("      ]\n");
			sb.append("    }");
			if (i < entries.size() - 1) sb.append(",");
			sb.append("\n");
		}
		sb.append("  ]");
	}

	/**
	 * Finds the reachable lines map entry for a given scope name by trying multiple
	 * source name patterns. Rhino source names may vary (e.g. "scopeName.js", "forms/formName.js",
	 * full paths, or just the scope name).
	 */
	private static Map<String, Set<Integer>> findReachableForScope(
		Map<String, Map<String, Set<Integer>>> reachableLines, String type, String scopeName)
	{
		if (reachableLines == null || reachableLines.isEmpty()) return null;

		// try exact matches with common patterns
		String[] candidates;
		if ("forms".equals(type))
		{
			candidates = new String[] {
				"forms/" + scopeName + ".js",
				scopeName + ".js",
				"forms/" + scopeName,
				scopeName
			};
		}
		else
		{
			candidates = new String[] {
				scopeName + ".js",
				"scopes/" + scopeName + ".js",
				scopeName
			};
		}

		for (String candidate : candidates)
		{
			Map<String, Set<Integer>> result = reachableLines.get(candidate);
			if (result != null) return result;
		}

		// fallback: search for any key that ends with scopeName or scopeName.js
		for (Map.Entry<String, Map<String, Set<Integer>>> entry : reachableLines.entrySet())
		{
			String key = entry.getKey();
			if (key.endsWith("/" + scopeName + ".js") || key.endsWith("\\" + scopeName + ".js") ||
				key.equals(scopeName) || key.equals(scopeName + ".js"))
			{
				return entry.getValue();
			}
		}

		return null;
	}

	/**
	 * Derives the Rhino source name from a scope/form name and type.
	 * Servoy compiles global scope scripts as "scopeName.js" and form scripts as "forms/formName.js".
	 */
	private static String resolveSourceName(String type, String scopeName)
	{
		if ("forms".equals(type))
		{
			return "forms/" + scopeName + ".js";
		}
		return scopeName + ".js";
	}

	private static String jsonString(String s)
	{
		if (s == null) return "null";
		return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
	}

	private static String intListJson(List<Integer> list)
	{
		if (list == null || list.isEmpty()) return "[]";
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < list.size(); i++)
		{
			if (i > 0) sb.append(", ");
			sb.append(list.get(i));
		}
		sb.append("]");
		return sb.toString();
	}

	// ---- simple data holders ----

	private static class ScopeEntry
	{
		final String name;
		final List<FunctionEntry> functions;

		ScopeEntry(String name, List<FunctionEntry> functions)
		{
			this.name = name;
			this.functions = functions;
		}
	}

	private static class FunctionEntry
	{
		final String name;
		final List<Integer> coveredLines;
		final List<Integer> uncoveredLines;

		FunctionEntry(String name, List<Integer> coveredLines, List<Integer> uncoveredLines)
		{
			this.name = name;
			this.coveredLines = coveredLines;
			this.uncoveredLines = uncoveredLines;
		}
	}
}
