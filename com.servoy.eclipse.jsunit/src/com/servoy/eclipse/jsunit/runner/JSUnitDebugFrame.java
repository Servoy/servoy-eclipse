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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeWith;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.debug.DebugFrame;

/**
 * @author jcompagner
 *
 */
public class JSUnitDebugFrame implements DebugFrame
{
	private final DebugFrame wrapper;
	private final JSUnitDebugger debugger;
	private final String name;

	public JSUnitDebugFrame(JSUnitDebugger debugger, String name, DebugFrame frame)
	{
		this.debugger = debugger;
		this.name = name;
		this.wrapper = frame;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.debug.DebugFrame#onEnter(org.mozilla.javascript.Context, org.mozilla.javascript.Scriptable,
	 * org.mozilla.javascript.Scriptable, java.lang.Object[])
	 */
	public void onEnter(Context cx, Scriptable activation, Scriptable thisObj, Object[] args)
	{
		if (wrapper != null) wrapper.onEnter(cx, activation, thisObj, args);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.debug.DebugFrame#onLineChange(org.mozilla.javascript.Context, int)
	 */
	public void onLineChange(Context cx, int lineNumber)
	{
		if (wrapper != null) wrapper.onLineChange(cx, lineNumber);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.debug.DebugFrame#onExceptionThrown(org.mozilla.javascript.Context, java.lang.Throwable)
	 */
	public void onExceptionThrown(Context cx, Throwable ex)
	{
		/**
		* This code keeps track of the latest exception thrown in the user code.
		*  Background for rules of the uncaught exception mechanism:
		* -Only one exception can reach the jsunit fail mechanism(and that one is stored in the debugger exception map with the test case name).
		* -The user can throw , catch and throw  again an exception triggering onExceptionThrown each time overriding the previews exception in the map (which means it was caught)
		* -if you throw an exception in a test case and it is not treated in the user code ,
		*   the exception is caught by the JsUnit code, calls tearDown() for the testcase and rethrows it causing  the stack trace dropped down to jsunit's "run_test" base method . we must ignore this case.
		*
		*/
		if (wrapper != null) wrapper.onExceptionThrown(cx, ex);
		if (ex instanceof JavaScriptException)
		{
			String stack = ((org.mozilla.javascript.JavaScriptException)ex).getScriptStackTrace();
			// this throw was from jsUnit code , skip  last call is "at JsUnit.js:789 (TestCase_runBare)"
			if (!stack.matches("^\\sat JsUnit.js\\:\\d+ \\(TestCase_runBare\\)(?s).*"))
			{
				String testName = name;
				if (JSUnitToJavaRunner.getCurentlyExecutingTest() != null)
				{
					testName = JSUnitToJavaRunner.getCurentlyExecutingTest();
				}
				debugger.addException(testName, ex);
			}
		}

	}

	public void onExit(Context cx, boolean byThrow, Object resultOrException)
	{
		if (wrapper != null) wrapper.onExit(cx, byThrow, resultOrException);

	}

	public void onDebuggerStatement(Context cx)
	{
		if (wrapper != null) wrapper.onDebuggerStatement(cx);

	}

	@Override
	public void onNativeWithEnter(Context cx, NativeWith withScope)
	{
		if (wrapper != null) wrapper.onNativeWithEnter(cx, withScope);

	}

	@Override
	public void onNativeWithExit(Context cx, NativeWith withScope)
	{
		if (wrapper != null) wrapper.onNativeWithExit(cx, withScope);

	}

}
