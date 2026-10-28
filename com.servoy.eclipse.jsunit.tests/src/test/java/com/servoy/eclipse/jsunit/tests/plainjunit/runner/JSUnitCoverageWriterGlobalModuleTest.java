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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.servoy.eclipse.jsunit.runner.JSUnitCoverageWriter;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;

/**
 * Unit tests for {@link JSUnitCoverageWriter#buildJson} - the per-solution
 * attribution of scope/form coverage in the JSON coverage report (SVY-21680,
 * the JSON-side counterpart of the SVY-21607 LCOV fix).
 *
 * <p>
 * The corrected design (spec {@code docs/SVY-21680-...spec.md} AC 1-8) keeps
 * the JSON <em>flat</em> (top-level {@code scopes[]}/ {@code forms[]}) and adds
 * a single new {@code solution} field to <em>every</em> scope and form entry,
 * naming the declaring {@link Solution} (the active/main solution or one of its
 * modules). Each entry's {@code name} always stays the real scope/form name
 * ({@code globals}, a named scope, a form name) - <b>never</b> a solution name.
 * </p>
 *
 * <p>
 * At runtime every solution's global functions are merged into a single
 * {@code "globals"} scope, so {@code buildJson} must split that merged bucket
 * into one {@code { name:"globals", solution:X }} entry per declaring solution
 * - resolving each global function via
 * {@link FlattenedSolution#getScriptMethod(String, String)} and
 * {@link ScriptMethod#getRootObject()}. Named scopes resolve via
 * {@link FlattenedSolution#getScopes()} (a {@code Pair} of scope name to
 * declaring root object) and forms via
 * {@link FlattenedSolution#getForm(String)} and {@link Form#getRootObject()}.
 * </p>
 *
 * <p>
 * These tests drive {@code buildJson} with crafted hit/reachable maps and a
 * mocked {@link FlattenedSolution}, then parse the emitted JSON so the
 * assertions are on its <em>structure</em> (entry names, the new {@code
 * solution} field, per-function covered/uncovered line lists, summary totals),
 * never on brittle substring matches. Reverting the production code to the
 * old/wrong {@code name = <solution>} behaviour makes the AC1/AC2 assertions
 * (which require {@code name == "globals"} with the solution carried in the
 * {@code solution} field, and no {@code { name:"B" }} entry) fail.
 * </p>
 */
public class JSUnitCoverageWriterGlobalModuleTest {
	private static final String GLOBALS = ScriptVariable.GLOBAL_SCOPE; // "globals"

	// ======================================================================
	// tiny JSON model + hand parser (no external JSON lib on the test classpath)
	// so assertions are on structure, not substrings. The scope-record model
	// carries the new "solution" field (AC3/AC8), and the parser rejects any
	// unexpected key so a schema drift surfaces as a parse failure here.
	// ======================================================================

	/** A function entry: name + its covered/uncovered line lists. */
	private static final class Func {
		String name;
		List<Integer> covered = new ArrayList<>();
		List<Integer> uncovered = new ArrayList<>();
	}

	/**
	 * A scope/form entry: name, the new declaring-solution field + its functions.
	 */
	private static final class Scope {
		String name;
		String solution;
		final Map<String, Func> functions = new LinkedHashMap<>();
	}

	/**
	 * The parsed coverage report. {@code scopes}/ {@code forms} are kept as flat
	 * lists (not keyed by name) because two entries may share the same {@code name}
	 * ("globals" for two different solutions) - they are distinguished by the new
	 * {@code solution} field.
	 */
	private static final class Report {
		String solution;
		int summaryCovered = -1;
		int summaryUncovered = -1;
		final List<Scope> scopes = new ArrayList<>();
		final List<Scope> forms = new ArrayList<>();

		/** All scope entries with the given name (0, 1 or many). */
		List<Scope> scopesNamed(String name) {
			List<Scope> out = new ArrayList<>();
			for (Scope s : scopes)
				if (name.equals(s.name))
					out.add(s);
			return out;
		}

		/** The single scope entry matching (name, solution), or null. */
		Scope scope(String name, String solution) {
			for (Scope s : scopes)
				if (name.equals(s.name) && solution.equals(s.solution))
					return s;
			return null;
		}

		/** The single form entry matching (name, solution), or null. */
		Scope form(String name, String solution) {
			for (Scope s : forms)
				if (name.equals(s.name) && solution.equals(s.solution))
					return s;
			return null;
		}

		boolean anyScopeNamed(String name) {
			return !scopesNamed(name).isEmpty();
		}
	}

	/**
	 * A deliberately small recursive-descent parser for the exact JSON shape
	 * {@link JSUnitCoverageWriter} emits. It is intentionally not a general JSON
	 * parser - it validates the schema it expects (top-level keys, scope/function
	 * field names including the new {@code solution} field) so a schema drift (AC8)
	 * would surface as a parse failure here.
	 */
	private static final class JsonParser {
		private final String s;
		private int i;

		JsonParser(String s) {
			this.s = s;
		}

		Report parse() {
			Report r = new Report();
			expect('{');
			boolean first = true;
			while (true) {
				skipWs();
				if (peek() == '}') {
					i++;
					break;
				}
				if (!first)
					expect(',');
				first = false;
				String key = readString();
				expect(':');
				switch (key) {
				case "solution":
					r.solution = readStringOrNull();
					break;
				case "timestamp":
					readStringOrNull();
					break;
				case "summary":
					parseSummary(r);
					break;
				case "scopes":
					parseScopeArray(r.scopes);
					break;
				case "forms":
					parseScopeArray(r.forms);
					break;
				default:
					throw new IllegalStateException("unexpected top-level key: " + key);
				}
			}
			return r;
		}

		private void parseSummary(Report r) {
			expect('{');
			boolean first = true;
			while (true) {
				skipWs();
				if (peek() == '}') {
					i++;
					break;
				}
				if (!first)
					expect(',');
				first = false;
				String key = readString();
				expect(':');
				int v = readInt();
				if ("coveredLines".equals(key))
					r.summaryCovered = v;
				else if ("uncoveredLines".equals(key))
					r.summaryUncovered = v;
				else
					throw new IllegalStateException("unexpected summary key: " + key);
			}
		}

		private void parseScopeArray(List<Scope> into) {
			expect('[');
			skipWs();
			if (peek() == ']') {
				i++;
				return;
			}
			boolean first = true;
			while (true) {
				skipWs();
				if (peek() == ']') {
					i++;
					break;
				}
				if (!first)
					expect(',');
				first = false;
				into.add(parseScope());
			}
		}

		private Scope parseScope() {
			Scope sc = new Scope();
			expect('{');
			boolean first = true;
			while (true) {
				skipWs();
				if (peek() == '}') {
					i++;
					break;
				}
				if (!first)
					expect(',');
				first = false;
				String key = readString();
				expect(':');
				switch (key) {
				case "name":
					sc.name = readStringOrNull();
					break;
				case "solution":
					sc.solution = readStringOrNull();
					break;
				case "functions":
					parseFunctions(sc);
					break;
				default:
					throw new IllegalStateException("unexpected scope key: " + key);
				}
			}
			return sc;
		}

		private void parseFunctions(Scope sc) {
			expect('[');
			skipWs();
			if (peek() == ']') {
				i++;
				return;
			}
			boolean first = true;
			while (true) {
				skipWs();
				if (peek() == ']') {
					i++;
					break;
				}
				if (!first)
					expect(',');
				first = false;
				Func f = parseFunc();
				sc.functions.put(f.name, f);
			}
		}

		private Func parseFunc() {
			Func f = new Func();
			expect('{');
			boolean first = true;
			while (true) {
				skipWs();
				if (peek() == '}') {
					i++;
					break;
				}
				if (!first)
					expect(',');
				first = false;
				String key = readString();
				expect(':');
				switch (key) {
				case "name":
					f.name = readStringOrNull();
					break;
				case "coveredLines":
					f.covered = readIntArray();
					break;
				case "uncoveredLines":
					f.uncovered = readIntArray();
					break;
				default:
					throw new IllegalStateException("unexpected function key: " + key);
				}
			}
			return f;
		}

		private List<Integer> readIntArray() {
			List<Integer> out = new ArrayList<>();
			expect('[');
			skipWs();
			if (peek() == ']') {
				i++;
				return out;
			}
			boolean first = true;
			while (true) {
				skipWs();
				if (peek() == ']') {
					i++;
					break;
				}
				if (!first)
					expect(',');
				first = false;
				out.add(Integer.valueOf(readInt()));
			}
			return out;
		}

		private String readStringOrNull() {
			skipWs();
			if (s.startsWith("null", i)) {
				i += 4;
				return null;
			}
			return readString();
		}

		private String readString() {
			skipWs();
			expect('"');
			StringBuilder sb = new StringBuilder();
			while (true) {
				char c = s.charAt(i++);
				if (c == '\\') {
					char n = s.charAt(i++);
					switch (n) {
					case 'n':
						sb.append('\n');
						break;
					case 'r':
						sb.append('\r');
						break;
					case 't':
						sb.append('\t');
						break;
					default:
						sb.append(n);
					}
				} else if (c == '"')
					break;
				else
					sb.append(c);
			}
			return sb.toString();
		}

		private int readInt() {
			skipWs();
			int start = i;
			if (peek() == '-')
				i++;
			while (i < s.length() && Character.isDigit(s.charAt(i)))
				i++;
			return Integer.parseInt(s.substring(start, i));
		}

		private void expect(char c) {
			skipWs();
			char actual = s.charAt(i++);
			if (actual != c)
				throw new IllegalStateException("expected '" + c + "' at " + (i - 1) + " but found '" + actual + "'");
		}

		private char peek() {
			skipWs();
			return s.charAt(i);
		}

		private void skipWs() {
			while (i < s.length() && Character.isWhitespace(s.charAt(i)))
				i++;
		}
	}

	private static Report build(Map<String, Map<String, Map<String, Map<Integer, Integer>>>> lineNumbers,
			Map<String, Map<String, Set<Integer>>> reachableLines, String solutionName, FlattenedSolution fs) {
		String json = JSUnitCoverageWriter.buildJson(lineNumbers, reachableLines, solutionName, fs);
		return new JsonParser(json).parse();
	}

	// ======================================================================
	// fixtures
	// ======================================================================

	/** A solution (module or main) mock whose getName() is {@code name}. */
	private static Solution solution(String name) {
		Solution sol = mock(Solution.class);
		// doReturn(...).when(...) rather than when(...).thenReturn(...): getName()/
		// getRootObject() live on AbstractBase/AbstractRootObject, whose heavy static
		// init can make a when(mock.call()) stubbing invoke the real method; the
		// doReturn form never calls the real method, so the stubbing is robust.
		lenient().doReturn(name).when(sol).getName();
		return sol;
	}

	/** A ScriptMethod mock whose owning root object is {@code owner}. */
	private static ScriptMethod declaredIn(IRootObject owner) {
		ScriptMethod sm = mock(ScriptMethod.class);
		lenient().doReturn(owner).when(sm).getRootObject();
		return sm;
	}

	/** A Form mock whose owning root object is {@code owner}. */
	private static Form formDeclaredIn(IRootObject owner) {
		Form form = mock(Form.class);
		lenient().doReturn(owner).when(form).getRootObject();
		return form;
	}

	/**
	 * Builds a {@code type -> scopeName -> funcName -> line -> hits} hit map.
	 * {@code funcHits} is a list of (funcName, int[]{line, hits, line, hits, ...})
	 * pairs.
	 */
	@SafeVarargs
	private static Map<String, Map<String, Map<String, Map<Integer, Integer>>>> hitMap(String type, String scope,
			Object... funcHits) {
		Map<String, Map<Integer, Integer>> funcMap = new LinkedHashMap<>();
		for (int k = 0; k < funcHits.length; k += 2) {
			String fn = (String) funcHits[k];
			int[] lh = (int[]) funcHits[k + 1];
			Map<Integer, Integer> lines = new LinkedHashMap<>();
			for (int p = 0; p < lh.length; p += 2)
				lines.put(Integer.valueOf(lh[p]), Integer.valueOf(lh[p + 1]));
			funcMap.put(fn, lines);
		}
		Map<String, Map<String, Map<Integer, Integer>>> scopeMap = new LinkedHashMap<>();
		scopeMap.put(scope, funcMap);
		Map<String, Map<String, Map<String, Map<Integer, Integer>>>> out = new LinkedHashMap<>();
		out.put(type, scopeMap);
		return out;
	}

	private static Set<String> nameSet(String... names) {
		return new LinkedHashSet<>(Arrays.asList(names));
	}

	/**
	 * Stubs {@code fs.getScopes()} to map each (scopeName -> declaring solution).
	 */
	@SafeVarargs
	private static void stubScopes(FlattenedSolution fs, Pair<String, IRootObject>... pairs) {
		Collection<Pair<String, IRootObject>> coll = Arrays.asList(pairs);
		lenient().doReturn(coll).when(fs).getScopes();
	}

	private static Pair<String, IRootObject> scopePair(String scopeName, IRootObject declaring) {
		return new Pair<>(scopeName, declaring);
	}

	// ======================================================================
	// AC1: main solution A declares test_globals; module B's globals.foo is a
	// { name:"globals", solution:"B" } entry - NEVER { name:"B" }
	// ======================================================================

	@Test
	public void ac1_globalsKeepNameSolutionCarriesModule_noSolutionNamedEntry() {
		Solution solA = solution("A");
		Solution moduleB = solution("B");

		FlattenedSolution fs = mock(FlattenedSolution.class);
		lenient().doReturn(declaredIn(solA)).when(fs).getScriptMethod(GLOBALS, "test_fooReturnsStringFoo");
		lenient().doReturn(declaredIn(moduleB)).when(fs).getScriptMethod(GLOBALS, "foo");

		Map<String, Map<String, Map<String, Map<Integer, Integer>>>> lineNumbers = hitMap("scopes", GLOBALS,
				"test_fooReturnsStringFoo", new int[] { 9, 1, 10, 1, 11, 1 }, "foo", new int[] { 6, 1, 7, 1 });

		Report r = build(lineNumbers, null, "A", fs);

		// both entries keep name "globals"; the module identity is in the solution
		// field
		Scope globalsA = r.scope(GLOBALS, "A");
		Scope globalsB = r.scope(GLOBALS, "B");
		assertNotNull(globalsA, "A's globals must be a { name:'globals', solution:'A' } entry");
		assertNotNull(globalsB, "B's globals must be a { name:'globals', solution:'B' } entry");

		// the entry name is NEVER a solution name
		assertFalse(r.anyScopeNamed("A"), "no scope entry may be named after a solution 'A'");
		assertFalse(r.anyScopeNamed("B"), "no scope entry may be named after a solution 'B'");

		assertTrue(globalsA.functions.containsKey("test_fooReturnsStringFoo"),
				"A's global function lands under solution A");
		assertEquals(Arrays.asList(6, 7), globalsB.functions.get("foo").covered,
				"B's foo covered lines preserved under solution B");
	}

	// ======================================================================
	// AC2: same-named globals in two modules -> two { name:"globals" } entries
	// with distinct solutions, functions NOT merged
	// ======================================================================

	@Test
	public void ac2_sameNamedGlobalsAcrossModulesAreDistinguishableBySolution() {
		Solution moduleB = solution("B");
		Solution moduleC = solution("C");

		FlattenedSolution fs = mock(FlattenedSolution.class);
		// both modules declare a global named "bar" - they must stay separate
		lenient().doReturn(declaredIn(moduleB)).when(fs).getScriptMethod(GLOBALS, "barB");
		lenient().doReturn(declaredIn(moduleC)).when(fs).getScriptMethod(GLOBALS, "barC");

		Map<String, Map<String, Map<String, Map<Integer, Integer>>>> lineNumbers = hitMap("scopes", GLOBALS, "barB",
				new int[] { 5, 1 }, "barC", new int[] { 8, 1 });

		Report r = build(lineNumbers, null, "A", fs);

		List<Scope> globals = r.scopesNamed(GLOBALS);
		assertEquals(2, globals.size(), "two declaring solutions -> two 'globals' entries");
		assertEquals(nameSet("B", "C"), nameSet(globals.get(0).solution, globals.get(1).solution),
				"the two 'globals' entries carry the distinct solutions B and C");

		Scope b = r.scope(GLOBALS, "B");
		Scope c = r.scope(GLOBALS, "C");
		assertTrue(b.functions.containsKey("barB") && !b.functions.containsKey("barC"),
				"B's globals holds only its own function");
		assertTrue(c.functions.containsKey("barC") && !c.functions.containsKey("barB"),
				"C's globals holds only its own function - functions are not merged");
	}

	// ======================================================================
	// AC3: the solution field is present and correct on a NAMED scope and a FORM
	// ======================================================================

	@Test
	public void ac3_namedScopeAndForm_carryCorrectSolution() {
		Solution solA = solution("A");
		Solution moduleB = solution("B");

		FlattenedSolution fs = mock(FlattenedSolution.class);
		// named scope "utils" is declared in module B
		stubScopes(fs, scopePair("utils", moduleB));
		// form "myForm" is declared in main solution A
		lenient().doReturn(formDeclaredIn(solA)).when(fs).getForm("myForm");

		Map<String, Map<String, Map<String, Map<Integer, Integer>>>> lineNumbers = new TreeMap<>();
		lineNumbers.putAll(hitMap("scopes", "utils", "u", new int[] { 1, 1, 2, 1 }));
		lineNumbers.putAll(hitMap("forms", "myForm", "onShow", new int[] { 3, 1 }));

		Report r = build(lineNumbers, null, "A", fs);

		Scope utils = r.scope("utils", "B");
		assertNotNull(utils, "named scope 'utils' must carry its declaring solution 'B'");
		assertEquals(Arrays.asList(1, 2), utils.functions.get("u").covered);

		Scope myForm = r.form("myForm", "A");
		assertNotNull(myForm, "form 'myForm' must carry its declaring solution 'A'");
		assertEquals(Arrays.asList(3), myForm.functions.get("onShow").covered);
	}

	// ======================================================================
	// AC4: unresolved entries fall back to the active solution; nothing dropped;
	// null FlattenedSolution -> single globals entry at the active solution
	// ======================================================================

	@Test
	public void ac4_unresolvedGlobal_fallsBackToActiveSolution_nothingDropped() {
		Solution moduleB = solution("B");

		FlattenedSolution fs = mock(FlattenedSolution.class);
		lenient().doReturn(declaredIn(moduleB)).when(fs).getScriptMethod(GLOBALS, "resolved");
		lenient().doReturn(null).when(fs).getScriptMethod(GLOBALS, "orphan"); // cannot resolve

		Map<String, Map<String, Map<String, Map<Integer, Integer>>>> lineNumbers = hitMap("scopes", GLOBALS, "resolved",
				new int[] { 1, 1 }, "orphan", new int[] { 99, 1 });

		Report r = build(lineNumbers, null, "ActiveSol", fs);

		Scope resolved = r.scope(GLOBALS, "B");
		assertNotNull(resolved, "the resolved global lands under solution B, keeping name 'globals'");
		assertTrue(resolved.functions.containsKey("resolved"));

		Scope orphanScope = r.scope(GLOBALS, "ActiveSol");
		assertNotNull(orphanScope, "the orphan falls back to the active solution, keeping name 'globals'");
		Func orphan = orphanScope.functions.get("orphan");
		assertNotNull(orphan, "the orphan function must not be dropped");
		assertEquals(Arrays.asList(99), orphan.covered, "orphan's covered line is preserved");
		// the fallback is carried by the solution field, never by renaming the entry
		assertFalse(r.anyScopeNamed("ActiveSol"), "no scope entry may be named after the active solution");
	}

	@Test
	public void ac4_nullFlattenedSolution_singleGlobalsEntryAtActiveSolution() {
		Map<String, Map<String, Map<String, Map<Integer, Integer>>>> lineNumbers = hitMap("scopes", GLOBALS, "foo",
				new int[] { 1, 1 }, "bar", new int[] { 2, 1 });

		Report r = build(lineNumbers, null, "A", null);

		List<Scope> globals = r.scopesNamed(GLOBALS);
		assertEquals(1, globals.size(), "with no FlattenedSolution the globals stay in a single 'globals' entry");
		assertEquals("A", globals.get(0).solution, "the single globals entry is attributed to the active solution");
		assertEquals(nameSet("foo", "bar"), globals.get(0).functions.keySet(),
				"both global functions are kept (none dropped) in the single 'globals' entry");
	}

	// ======================================================================
	// AC7: summary totals are identical whether or not the split happens
	// ======================================================================

	@Test
	public void ac7_summaryTotalsInvariantToSplit() {
		Solution moduleB = solution("B");
		Solution moduleC = solution("C");

		FlattenedSolution fs = mock(FlattenedSolution.class);
		lenient().doReturn(declaredIn(moduleB)).when(fs).getScriptMethod(GLOBALS, "b1");
		lenient().doReturn(declaredIn(moduleC)).when(fs).getScriptMethod(GLOBALS, "c1");

		Map<String, Map<String, Map<String, Map<Integer, Integer>>>> lineNumbers = hitMap("scopes", GLOBALS, "b1",
				new int[] { 1, 1, 2, 1 }, "c1", new int[] { 3, 1 });
		Report split = build(lineNumbers, null, "A", fs);

		// same data, no flattened solution -> single merged globals entry
		Map<String, Map<String, Map<String, Map<Integer, Integer>>>> lineNumbers2 = hitMap("scopes", GLOBALS, "b1",
				new int[] { 1, 1, 2, 1 }, "c1", new int[] { 3, 1 });
		Report merged = build(lineNumbers2, null, "A", null);

		assertEquals(merged.summaryCovered, split.summaryCovered,
				"summary covered total is unchanged by the per-solution split");
		assertEquals(merged.summaryUncovered, split.summaryUncovered,
				"summary uncovered total is unchanged by the per-solution split");
		assertEquals(3, split.summaryCovered, "3 covered lines across the two modules");

		// cross-check the split total equals the sum over every entry's functions
		int manualCovered = 0, manualUncovered = 0;
		for (Scope sc : split.scopes)
			for (Func f : sc.functions.values()) {
				manualCovered += f.covered.size();
				manualUncovered += f.uncovered.size();
			}
		assertEquals(manualCovered, split.summaryCovered);
		assertEquals(manualUncovered, split.summaryUncovered);
	}

	// ======================================================================
	// AC8: schema - every scope/form entry has exactly name, solution, functions;
	// functions have name, coveredLines, uncoveredLines; top-level keys preserved.
	// The strict parser throws on any unexpected key.
	// ======================================================================

	@Test
	public void ac8_jsonSchemaShapePreserved_withSolutionField() {
		Solution solA = solution("A");
		Solution moduleB = solution("B");

		FlattenedSolution fs = mock(FlattenedSolution.class);
		lenient().doReturn(declaredIn(moduleB)).when(fs).getScriptMethod(GLOBALS, "glob");
		stubScopes(fs, scopePair("named", solA));
		lenient().doReturn(formDeclaredIn(solA)).when(fs).getForm("myForm");

		// a global, a named scope and a form in one report
		Map<String, Map<String, Map<String, Map<Integer, Integer>>>> lineNumbers = new TreeMap<>();
		lineNumbers.putAll(hitMap("scopes", GLOBALS, "glob", new int[] { 1, 1 }));
		lineNumbers.get("scopes").put("named",
				hitMap("scopes", "named", "nf", new int[] { 2, 1 }).get("scopes").get("named"));
		lineNumbers.putAll(hitMap("forms", "myForm", "ff", new int[] { 3, 1 }));

		String json = JSUnitCoverageWriter.buildJson(lineNumbers, null, "A", fs);
		// the strict parser validates top-level
		// solution/timestamp/summary/scopes/forms,
		// scope { name, solution, functions:[{name, coveredLines, uncoveredLines}] } -
		// any schema drift (extra/missing key) throws here
		Report r = new JsonParser(json).parse();

		assertEquals("A", r.solution, "top-level solution field preserved");
		assertTrue(r.summaryCovered >= 0 && r.summaryUncovered >= 0,
				"summary object with coveredLines/uncoveredLines preserved");

		Scope globB = r.scope(GLOBALS, "B");
		assertNotNull(globB, "the global's entry keeps name 'globals' with solution 'B'");
		assertEquals(Arrays.asList(1), globB.functions.get("glob").covered);
		assertEquals(Arrays.asList(), globB.functions.get("glob").uncovered);

		assertNotNull(r.scope("named", "A"), "the named scope entry carries solution 'A'");
		assertNotNull(r.form("myForm", "A"), "the form entry carries solution 'A'");
	}
}
