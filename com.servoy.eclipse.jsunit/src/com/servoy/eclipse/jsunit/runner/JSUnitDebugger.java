/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.Debugger;

/**
 * @author jcompagner
 */
public class JSUnitDebugger implements Debugger
{
	private final Debugger wrapper;
	private final Map<String, Throwable> exceptions = new HashMap<String, Throwable>();
	// type -> scopeName -> functionName -> lineNumber -> hits
	private final Map<String, Map<String, Map<String, Map<Integer, Integer>>>> lineNumbers = new HashMap<String, Map<String, Map<String, Map<Integer, Integer>>>>();
	// sourceName -> functionName -> reachable line numbers (all executable lines in the compiled source)
	private final Map<String, Map<String, Set<Integer>>> reachableLines = new HashMap<String, Map<String, Set<Integer>>>();

	public JSUnitDebugger(Debugger wrapper)
	{
		this.wrapper = wrapper;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.debug.Debugger#handleCompilationDone(org.mozilla.javascript.Context, org.mozilla.javascript.debug.DebuggableScript,
	 * java.lang.String)
	 */
	public void handleCompilationDone(Context cx, DebuggableScript fnOrScript, String source)
	{
		if (wrapper != null) wrapper.handleCompilationDone(cx, fnOrScript, source);
		collectReachableLines(fnOrScript);
	}

	/**
	 * Recursively walks the DebuggableScript tree and records all executable line numbers
	 * per source file and function name. Called from handleCompilationDone before any test
	 * execution so that uncovered lines can be computed after the run.
	 */
	private void collectReachableLines(DebuggableScript script)
	{
		String sourceName = script.getSourceName();
		if (sourceName == null) sourceName = "unknown";

		String functionName = script.getFunctionName();
		if (functionName == null || functionName.isEmpty()) functionName = "<top-level>";

		int[] lines = script.getLineNumbers();
		if (lines != null && lines.length > 0)
		{
			Set<Integer> lineSet = new HashSet<Integer>();
			for (int line : lines)
			{
				lineSet.add(line);
			}
			reachableLines.computeIfAbsent(sourceName, k -> new HashMap<String, Set<Integer>>())
				.merge(functionName, lineSet, (existing, newSet) -> {
					existing.addAll(newSet);
					return existing;
				});
		}

		for (int i = 0; i < script.getFunctionCount(); i++)
		{
			collectReachableLines(script.getFunction(i));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.debug.Debugger#getFrame(org.mozilla.javascript.Context, org.mozilla.javascript.debug.DebuggableScript)
	 */
	public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript)
	{
		collectReachableLines(fnOrScript);
		if (wrapper != null) return new JSUnitDebugFrame(this, fnOrScript.getFunctionName(), wrapper.getFrame(cx, fnOrScript));
		return new JSUnitDebugFrame(this, fnOrScript.getFunctionName(), null);
	}

	/**
	 * @param name
	 * @param ex
	 */
	public void addException(String name, Throwable ex)
	{
		exceptions.put(name, ex);
	}

	/**
	 * @param testName
	 * @return
	 */
	public Object getException(String testName)
	{
		return exceptions.get(testName);
	}

	public void addLineNumberHit(String type, String scopeName, String functionName, int lineNumber)
	{
		lineNumbers.computeIfAbsent(type, k -> new HashMap<String, Map<String, Map<Integer, Integer>>>())
			.computeIfAbsent(scopeName, k -> new HashMap<String, Map<Integer, Integer>>())
			.computeIfAbsent(functionName, k -> new HashMap<Integer, Integer>())
			.merge(lineNumber, 1, Integer::sum);
	}

	public Map<String, Map<String, Map<String, Map<Integer, Integer>>>> getLineNumbers()
	{
		return lineNumbers;
	}

	/**
	 * Returns all executable lines per source file and function name, as collected at compile time.
	 * Keys are source names (e.g. "globals.js", "forms/myForm.js") and function names.
	 * This data is available before any test executes and is used to compute uncovered lines.
	 *
	 * @return map of sourceName -> functionName -> set of reachable line numbers
	 */
	public Map<String, Map<String, Set<Integer>>> getReachableLines()
	{
		return reachableLines;
	}
}
