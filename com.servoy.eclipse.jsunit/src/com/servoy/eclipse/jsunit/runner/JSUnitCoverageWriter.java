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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;

/**
 * Serialises JSUnit line-coverage data to a JSON file after a test run.
 * <p>
 * The output format is:
 * <pre>
 * {
 *   "solution": "A",
 *   "timestamp": "2026-06-05T10:00:00Z",
 *   "summary": { "coveredLines": 142, "uncoveredLines": 38 },
 *   "scopes": [
 *     { "name": "globals",      "solution": "A", "functions": [ { "name": "fn",  "coveredLines": [1,2], "uncoveredLines": [3] } ] },
 *     { "name": "globals",      "solution": "B", "functions": [ { "name": "foo", "coveredLines": [6,7], "uncoveredLines": []  } ] },
 *     { "name": "myScope",      "solution": "A", "functions": [ ... ] }
 *   ],
 *   "forms":  [ { "name": "myForm", "solution": "A", "functions": [ ... ] } ]
 * }
 * </pre>
 * Every <code>scopes[]</code> and <code>forms[]</code> entry carries a <code>solution</code> field naming the
 * {@link Solution} (active/main solution or one of its modules) that declares that scope or form. The entry's
 * <code>name</code> always stays the real scope/form name (<code>globals</code>, a named scope, a form name) and is
 * never a solution name. The runtime merges every solution's globals into a single <code>globals</code> scope, so the
 * writer splits that bucket into one <code>{ "name": "globals", "solution": &lt;declaring solution&gt; }</code> entry
 * per declaring solution (parity with the SVY-21607 LCOV <code>SF:&lt;solution&gt;/globals.js</code>).
 * <p>
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
	 * @param solutionName  name of the active/main solution under test (used as the top-level metadata in the JSON, and
	 *                      as the fallback <code>solution</code> value for any scope/form whose declaring solution cannot
	 *                      be resolved)
	 * @param flattenedSolution the active {@link FlattenedSolution}, used to resolve the declaring solution of every
	 *                      scope and form (set on each entry's new <code>solution</code> field) and to split the merged
	 *                      <code>globals</code> bucket into one entry per declaring solution. May be <code>null</code>:
	 *                      when <code>null</code> no per-solution resolution is done, every entry is attributed to
	 *                      <code>solutionName</code> and the global functions are kept in a single <code>"globals"</code>
	 *                      entry (a warning is logged).
	 * @param outputPath    absolute path for the output .json file
	 */
	public static void write(
		Map<String, Map<String, Map<String, Map<Integer, Integer>>>> lineNumbers,
		Map<String, Map<String, Set<Integer>>> reachableLines,
		String solutionName,
		FlattenedSolution flattenedSolution,
		String outputPath)
	{
		if (outputPath == null || outputPath.isEmpty()) return;
		if (lineNumbers == null && reachableLines == null) return;

		try
		{
			String json = buildJson(lineNumbers, reachableLines, solutionName, flattenedSolution);
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

	// package-visibility widened to public (from private) so the plainjunit test can drive the per-module global
	// grouping through this entry point (with crafted lineNumbers maps + a mocked FlattenedSolution) without starting
	// an OSGi runtime and without touching the private FunctionEntry/ScopeEntry holders - the test parses the JSON
	// output and asserts on its structure (mirrors how SVY-21607's test exercises appendGlobalScopeCoverage). The
	// method has no side effects beyond producing the JSON string, so widening it is behavior-neutral.
	public static String buildJson(
		Map<String, Map<String, Map<String, Map<Integer, Integer>>>> lineNumbers,
		Map<String, Map<String, Set<Integer>>> reachableLines,
		String solutionName,
		FlattenedSolution flattenedSolution)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("  \"solution\": ").append(jsonString(solutionName)).append(",\n");
		sb.append("  \"timestamp\": ").append(jsonString(Instant.now().toString())).append(",\n");

		// ---- collect per-type data ----
		List<ScopeEntry> scopes = new ArrayList<>();
		List<ScopeEntry> forms = new ArrayList<>();

		// scope name -> declaring solution name, resolved once from the persist model (empty when no flattened solution)
		Map<String, String> scopeToSolution = buildScopeToSolutionMap(flattenedSolution);

		if (lineNumbers != null)
		{
			for (Map.Entry<String, Map<String, Map<String, Map<Integer, Integer>>>> typeEntry : lineNumbers.entrySet())
			{
				String type = typeEntry.getKey(); // "scopes" or "forms"
				for (Map.Entry<String, Map<String, Map<Integer, Integer>>> scopeEntry : typeEntry.getValue().entrySet())
				{
					String scopeName = scopeEntry.getKey();
					if (EXCLUDED_SOURCES.contains(scopeName)) continue;
					Map<String, Set<Integer>> reachableForSource = findReachableForScope(reachableLines, type, scopeName);

					List<FunctionEntry> functions = new ArrayList<>();
					for (Map.Entry<String, Map<Integer, Integer>> funcEntry : scopeEntry.getValue().entrySet())
					{
						String funcName = funcEntry.getKey();
						if (EXCLUDED_SOURCES.contains(funcName)) continue;
						Set<Integer> hitLines = funcEntry.getValue().keySet();

						Set<Integer> reachableForFunc = reachableForSource != null ? reachableForSource.get(funcName) : null;
						if (reachableForFunc == null) reachableForFunc = findReachableForFunction(reachableLines, type, scopeName, funcName);
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

					boolean isGlobals = "scopes".equals(type) && ScriptVariable.GLOBAL_SCOPE.equals(scopeName);
					if (isGlobals && flattenedSolution != null)
					{
						// the runtime merges every solution's globals into one "globals" scope; split them back into
						// one entry per declaring solution - each KEEPING name == "globals", with the resolved solution
						// in the new "solution" field (parity with the SVY-21607 LCOV fix)
						scopes.addAll(splitGlobalsBySolution(functions, flattenedSolution, solutionName));
					}
					else if (isGlobals)
					{
						// no flattened solution available: cannot attribute globals per solution, keep the single
						// "globals" entry attributed to the active solution
						ServoyLog.logWarning(
							"JSUnit coverage: no flattened solution available, global functions are reported under a single " +
								"'globals' entry attributed to the active solution '" + solutionName + "' instead of per declaring solution.",
							null);
						scopes.add(new ScopeEntry(scopeName, solutionName, functions));
					}
					else if ("forms".equals(type))
					{
						forms.add(new ScopeEntry(scopeName, resolveFormSolution(flattenedSolution, scopeName, solutionName), functions));
					}
					else
					{
						scopes.add(new ScopeEntry(scopeName, resolveScopeSolution(scopeToSolution, scopeName, solutionName), functions));
					}
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

	/**
	 * Splits the single merged <code>"globals"</code> bucket's functions into one {@link ScopeEntry} per declaring
	 * solution. Each global function is resolved to its declaring solution via
	 * {@link FlattenedSolution#getScriptMethod(String, String)} and {@link ScriptMethod#getRootObject()}. Each resulting
	 * entry keeps <code>name == "globals"</code> ({@link ScriptVariable#GLOBAL_SCOPE}) and carries the resolved solution
	 * name in its {@link ScopeEntry#solution} field. Functions whose declaring solution cannot be resolved are
	 * attributed to the active solution and a warning is logged, so no coverage is silently dropped.
	 *
	 * @param globalFunctions    the global scope's already-built, per-function-sorted function entries
	 * @param flattenedSolution  the active flattened solution used to resolve each function's solution (not null)
	 * @param activeSolutionName the active solution name, used as the fallback solution for unresolved functions
	 * @return one scope entry per declaring solution, each named <code>"globals"</code>, in deterministic
	 *         solution-name order
	 */
	private static List<ScopeEntry> splitGlobalsBySolution(
		List<FunctionEntry> globalFunctions, FlattenedSolution flattenedSolution, String activeSolutionName)
	{
		Map<String, List<FunctionEntry>> solutionToFunctions = new TreeMap<>();
		for (FunctionEntry fe : globalFunctions)
		{
			String declaringSolution = activeSolutionName;
			ScriptMethod sm = flattenedSolution.getScriptMethod(ScriptVariable.GLOBAL_SCOPE, fe.name);
			if (sm != null && sm.getRootObject() instanceof Solution declaring)
			{
				declaringSolution = declaring.getName();
			}
			else
			{
				ServoyLog.logWarning("JSUnit coverage: could not resolve declaring solution for global function '" + fe.name +
					"', attributing it to the active solution '" + activeSolutionName + "'.", null);
			}
			solutionToFunctions.computeIfAbsent(declaringSolution, k -> new ArrayList<>()).add(fe);
		}

		List<ScopeEntry> result = new ArrayList<>(solutionToFunctions.size());
		for (Map.Entry<String, List<FunctionEntry>> solutionEntry : solutionToFunctions.entrySet())
		{
			// name stays the real scope name "globals"; the declaring solution goes into the "solution" field
			result.add(new ScopeEntry(ScriptVariable.GLOBAL_SCOPE, solutionEntry.getKey(), solutionEntry.getValue()));
		}
		return result;
	}

	/**
	 * Builds a scope-name &rarr; declaring-solution-name map once per {@link #buildJson} call from
	 * {@link FlattenedSolution#getScopes()} (each {@link Pair}'s left is the scope name, its right the declaring
	 * {@link IRootObject}/{@link Solution}). Named scopes are 1:1 with a single declaring solution, so there is no
	 * collision. Returns an empty map when no flattened solution is available.
	 */
	private static Map<String, String> buildScopeToSolutionMap(FlattenedSolution flattenedSolution)
	{
		Map<String, String> map = new HashMap<>();
		if (flattenedSolution == null) return map;
		Collection<Pair<String, IRootObject>> scopes = flattenedSolution.getScopes();
		if (scopes == null) return map;
		for (Pair<String, IRootObject> scope : scopes)
		{
			if (scope == null) continue;
			String scopeName = scope.getLeft();
			IRootObject rootObject = scope.getRight();
			if (scopeName != null && rootObject instanceof Solution declaring)
			{
				map.put(scopeName, declaring.getName());
			}
		}
		return map;
	}

	/**
	 * Resolves the declaring solution of a named (non-globals) scope from the pre-built scope&rarr;solution map. Falls
	 * back to the active solution name and logs a warning when the scope has no mapping, so no coverage is dropped.
	 */
	private static String resolveScopeSolution(Map<String, String> scopeToSolution, String scopeName, String activeSolutionName)
	{
		String resolved = scopeToSolution.get(scopeName);
		if (resolved != null) return resolved;
		ServoyLog.logWarning("JSUnit coverage: could not resolve declaring solution for scope '" + scopeName +
			"', attributing it to the active solution '" + activeSolutionName + "'.", null);
		return activeSolutionName;
	}

	/**
	 * Resolves the declaring solution of a form via {@link FlattenedSolution#getForm(String)} and
	 * {@link Form#getRootObject()}. Falls back to the active solution name and logs a warning when the form or its
	 * declaring solution cannot be resolved, so no coverage is dropped.
	 */
	private static String resolveFormSolution(FlattenedSolution flattenedSolution, String formName, String activeSolutionName)
	{
		if (flattenedSolution != null)
		{
			Form form = flattenedSolution.getForm(formName);
			if (form != null && form.getRootObject() instanceof Solution declaring)
			{
				return declaring.getName();
			}
		}
		ServoyLog.logWarning("JSUnit coverage: could not resolve declaring solution for form '" + formName +
			"', attributing it to the active solution '" + activeSolutionName + "'.", null);
		return activeSolutionName;
	}

	private static void appendScopeArray(StringBuilder sb, List<ScopeEntry> entries)
	{
		sb.append("[\n");
		for (int i = 0; i < entries.size(); i++)
		{
			ScopeEntry se = entries.get(i);
			sb.append("    {\n");
			sb.append("      \"name\": ").append(jsonString(se.name)).append(",\n");
			sb.append("      \"solution\": ").append(jsonString(se.solution)).append(",\n");
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
			candidates = new String[] { "forms/" + scopeName + ".js", scopeName + ".js", "forms/" + scopeName, scopeName
			};
		}
		else
		{
			candidates = new String[] { scopeName + ".js", "scopes/" + scopeName + ".js", scopeName
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
	 * Finds the reachable lines of one function when Rhino compiled it as its own source, named
	 * "&lt;solution&gt;/scopes/&lt;scope&gt;/&lt;function&gt;" or "&lt;solution&gt;/forms/&lt;form&gt;/&lt;function&gt;" (see
	 * ScriptEngine.compileFunction). The lines of all nested functions of that source (callbacks, inner functions) are
	 * merged in, as their hits are recorded under the enclosing function (SVY-21638).
	 */
	private static Set<Integer> findReachableForFunction(
		Map<String, Map<String, Set<Integer>>> reachableLines, String type, String scopeName, String funcName)
	{
		if (reachableLines == null || funcName == null || funcName.isEmpty()) return null;
		String suffix = '/' + ("forms".equals(type) ? "forms" : "scopes") + '/' + scopeName + '/' + funcName;
		for (Map.Entry<String, Map<String, Set<Integer>>> entry : reachableLines.entrySet())
		{
			if (entry.getKey().endsWith(suffix) && entry.getValue() != null)
			{
				Set<Integer> merged = new TreeSet<>();
				entry.getValue().values().forEach(merged::addAll);
				return merged;
			}
		}
		return null;
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
		/** Name of the {@link Solution} (active/main solution or a module) that declares this scope or form. */
		final String solution;
		final List<FunctionEntry> functions;

		ScopeEntry(String name, String solution, List<FunctionEntry> functions)
		{
			this.name = name;
			this.solution = solution;
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
