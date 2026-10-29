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
package com.servoy.eclipse.jsunit.tests.plainjunit.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.servoy.eclipse.jsunit.runner.JSUnitCoverageWriter;
import com.servoy.eclipse.jsunit.runner.JSUnitDebugger;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.util.Pair;

/**
 * SVY-21638: lines inside anonymous callbacks and inside methods of object literals were never recorded as hit, because
 * {@code JSUnitDebugFrame} derived the Servoy scope from the "this" object of the frame (the top-level scope for a
 * callback, the object itself for an object literal method).
 * <p>
 * These tests execute real JavaScript with Rhino (interpreted, with debug info) inside a real {@link GlobalScope}, with a
 * {@link JSUnitDebugger} attached, exactly like a JSUnit run does, and then assert the exact hit counts that the debugger
 * recorded per line.
 */
public class JSUnitDebuggerNestedFunctionCoverageTest
{
	private static final String SOLUTION = "mySolution";
	private static final String SCOPE = "utils";

	private Context cx;
	private JSUnitDebugger debugger;
	private GlobalScope globalScope;

	@BeforeEach
	public void setUp()
	{
		cx = Context.enter();
		cx.setInterpretedMode(true);
		cx.setGeneratingDebug(true);
		debugger = new JSUnitDebugger(null);
		cx.setDebugger(debugger, null);

		Scriptable topLevel = cx.initStandardObjects();

		FlattenedSolution fs = mock(FlattenedSolution.class);
		doReturn(Collections.emptyIterator()).when(fs).getScriptMethods(SCOPE, false);
		IServiceProvider application = mock(IServiceProvider.class);
		doReturn(fs).when(application).getFlattenedSolution();

		globalScope = new GlobalScope(topLevel, SCOPE, null, application);
	}

	@AfterEach
	public void tearDown()
	{
		Context.exit();
	}

	/** Compiles a scope function the way ScriptEngine.compileFunction names it: solution/scopes/scope/function. */
	private Function compileScopeFunction(String functionName, String source)
	{
		return cx.compileFunction(globalScope, source, SOLUTION + "/scopes/" + SCOPE + '/' + functionName, 1, null);
	}

	private Map<Integer, Integer> hitsOf(String functionName)
	{
		Map<String, Map<Integer, Integer>> scopeHits = debugger.getLineNumbers().get("scopes").get(SCOPE);
		assertNotNull(scopeHits, "no hits recorded for scope " + SCOPE);
		Map<Integer, Integer> hits = scopeHits.get(functionName);
		assertNotNull(hits, "no hits recorded for function '" + functionName + "', recorded functions: " + scopeHits.keySet());
		return new TreeMap<>(hits);
	}

	@Test
	public void linesOfAnonymousCallbackAreCountedForTheEnclosingFunction()
	{
		Function doubleAll = compileScopeFunction("doubleAll",
			"function doubleAll() {\n" + //  1
				"    var input = [1, 2, 3];\n" + //  2
				"    var result = input.map(function(x) {\n" + //  3
				"        var doubled = x * 2;\n" + //  4
				"        return doubled;\n" + //  5
				"    });\n" + //  6
				"    return result;\n" + //  7
				"}"); //  8

		NativeArray result = (NativeArray)doubleAll.call(cx, globalScope, globalScope, new Object[0]);
		assertEquals(3, result.size());
		assertEquals(6, ((Number)result.get(2)).intValue());

		Map<Integer, Integer> hits = hitsOf("doubleAll");
		// the callback body ran once per array element
		assertEquals(Integer.valueOf(3), hits.get(4), "hits of line 4 (inside the callback), all hits: " + hits);
		assertEquals(Integer.valueOf(3), hits.get(5), "hits of line 5 (inside the callback), all hits: " + hits);
		assertEquals(Integer.valueOf(1), hits.get(2), "hits of line 2, all hits: " + hits);
		assertEquals(Integer.valueOf(1), hits.get(7), "hits of line 7, all hits: " + hits);
		// the callback must not show up as a separate (nameless) function of the scope
		assertEquals(Set.of("doubleAll"), debugger.getLineNumbers().get("scopes").get(SCOPE).keySet());

		// the callback lines are reachable lines of doubleAll, so they can be reported as uncovered when not hit
		Set<Integer> reachable = new TreeSet<>(debugger.getReachableLines().get(SOLUTION + "/scopes/" + SCOPE + "/doubleAll").get("doubleAll"));
		assertEquals(true, reachable.containsAll(Set.of(2, 3, 4, 5, 7)), "reachable lines of doubleAll: " + reachable);
	}

	@Test
	public void linesOfObjectLiteralMethodAreCountedForThatMethod()
	{
		// a scope variable initializer is evaluated by ScriptVariableScope with the scope's .js file as source name
		Scriptable handler = (Scriptable)cx.evaluateString(globalScope,
			"({\n" + //  1
				"    counter: 0,\n" + //  2
				"    increment: function() {\n" + //  3
				"        this.counter++;\n" + //  4
				"        if (this.counter > 1) {\n" + //  5
				"            return 'many';\n" + //  6
				"        }\n" + //  7
				"        return 'one';\n" + //  8
				"    }\n" + //  9
				"})", //  10
			SOLUTION + '/' + SCOPE + ".js", 1, null);

		assertEquals("one", ScriptableObject.callMethod(cx, handler, "increment", new Object[0]));
		assertEquals("many", ScriptableObject.callMethod(cx, handler, "increment", new Object[0]));

		Map<Integer, Integer> hits = hitsOf("increment");
		assertEquals(Integer.valueOf(2), hits.get(4), "all hits: " + hits);
		assertEquals(Integer.valueOf(2), hits.get(5), "all hits: " + hits);
		assertEquals(Integer.valueOf(1), hits.get(6), "all hits: " + hits);
		assertEquals(Integer.valueOf(1), hits.get(8), "all hits: " + hits);
	}

	@Test
	public void namedFunctionWithoutNestedFunctionsIsUnchanged()
	{
		Function plainAdd = compileScopeFunction("plainAdd",
			"function plainAdd(a, b) {\n" + //  1
				"    var sum = a + b;\n" + //  2
				"    return sum;\n" + //  3
				"}"); //  4

		assertEquals(5, ((Number)plainAdd.call(cx, globalScope, globalScope, new Object[] { 2, 3 })).intValue());

		Map<Integer, Integer> hits = hitsOf("plainAdd");
		assertEquals(Integer.valueOf(1), hits.get(2), "all hits: " + hits);
		assertEquals(Integer.valueOf(1), hits.get(3), "all hits: " + hits);
	}

	@Test
	public void linesOfCallbackThatNeverRanAreReportedAsUncovered()
	{
		Function filterNone = compileScopeFunction("filterNone",
			"function filterNone() {\n" + //  1
				"    var empty = [];\n" + //  2
				"    var kept = empty.filter(function(x) {\n" + //  3
				"        var keep = x > 0;\n" + //  4
				"        return keep;\n" + //  5
				"    });\n" + //  6
				"    return kept;\n" + //  7
				"}"); //  8

		// filtering an empty array never calls the callback
		assertEquals(0, ((NativeArray)filterNone.call(cx, globalScope, globalScope, new Object[0])).size());

		String json = buildJson();
		assertEquals(List.of(1, 2, 3, 7), coveredLines(json, "filterNone"), json);
		assertEquals(List.of(4, 5), uncoveredLines(json, "filterNone"), json);
	}

	@Test
	public void anonymousFunctionInScopeVariableIsReportedWithItsOwnNameAndUncoveredLines()
	{
		// "var onDone = function(flag) {...}" in a scope: ScriptVariableScope evaluates the value as "(...)" so the
		// function has no name and is not nested in another function
		Function onDone = (Function)cx.evaluateString(globalScope,
			"(function(flag) {\n" + //  1
				"    var result = 1;\n" + //  2
				"    if (flag) {\n" + //  3
				"        result = 2;\n" + //  4
				"    }\n" + //  5
				"    return result;\n" + //  6
				"})", //  7
			SOLUTION + '/' + SCOPE + ".js", 1, null);

		assertEquals(1, ((Number)onDone.call(cx, globalScope, globalScope, new Object[] { Boolean.FALSE })).intValue());

		// named after its first line, like the profiler names anonymous functions (ScriptEngine.getSourceName)
		Map<Integer, Integer> hits = hitsOf("(anon:1)");
		assertEquals(Map.of(1, 1, 2, 1, 3, 1, 6, 1), hits);

		String json = buildJson();
		assertEquals(List.of(1, 2, 3, 6), coveredLines(json, "(anon:1)"), json);
		assertEquals(List.of(4), uncoveredLines(json, "(anon:1)"), json);
	}

	private String buildJson()
	{
		Solution solution = mock(Solution.class);
		doReturn(SOLUTION).when(solution).getName();
		FlattenedSolution fs = mock(FlattenedSolution.class);
		doReturn(List.of(new Pair<String, IRootObject>(SCOPE, solution))).when(fs).getScopes();
		return JSUnitCoverageWriter.buildJson(debugger.getLineNumbers(), debugger.getReachableLines(), SOLUTION, fs);
	}

	private static List<Integer> coveredLines(String json, String functionName)
	{
		return lineList(functionEntry(json, functionName).group(1));
	}

	private static List<Integer> uncoveredLines(String json, String functionName)
	{
		return lineList(functionEntry(json, functionName).group(2));
	}

	private static Matcher functionEntry(String json, String functionName)
	{
		Matcher function = Pattern.compile("\"name\": \"" + Pattern.quote(functionName) +
			"\",\\s*\"coveredLines\": \\[([^\\]]*)\\],\\s*\"uncoveredLines\": \\[([^\\]]*)\\]").matcher(json);
		assertEquals(true, function.find(), "no entry for " + functionName + " in:\n" + json);
		return function;
	}

	private static List<Integer> lineList(String commaSeparated)
	{
		if (commaSeparated.isBlank()) return List.of();
		return Arrays.stream(commaSeparated.split(",")).map(String::trim).map(Integer::valueOf).toList();
	}
}
