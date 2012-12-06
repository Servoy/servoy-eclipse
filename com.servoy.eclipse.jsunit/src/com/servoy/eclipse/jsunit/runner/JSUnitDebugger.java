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
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.Debugger;

/**
 * @author jcompagner
 *
 */
public class JSUnitDebugger implements Debugger
{
	private final Debugger wrapper;
	private final Map<String, Throwable> exceptions = new HashMap<String, Throwable>();

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
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.debug.Debugger#getFrame(org.mozilla.javascript.Context, org.mozilla.javascript.debug.DebuggableScript)
	 */
	public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript)
	{
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

}
