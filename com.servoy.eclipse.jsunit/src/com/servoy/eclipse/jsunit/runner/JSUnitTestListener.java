/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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

import java.util.List;

import junit.framework.Test;
import junit.framework.TestResult;

import org.mozilla.javascript.Scriptable;

/**
 * This object is wrapped into a javascript object so as to be seen by JSUnit as a test result listener. It only contains the interface methods that will be
 * available to JS, and forwards requests to it's JSUnitTestListenerHandler so that the obfuscation of test result conversion logic is not obstructed.
 * 
 * @author acostescu
 * 
 */
public class JSUnitTestListener
{

	private final JSUnitTestListenerHandler handler;

	public JSUnitTestListener(TestResult result, List<Test> testList, boolean useFileInStackQualifiedName)
	{
		this(result, testList, useFileInStackQualifiedName, null);
	}

	public JSUnitTestListener(TestResult result, List<Test> testList, boolean useFileInStackQualifiedName, String[] stackElementFilters)
	{
		handler = new JSUnitTestListenerHandler(result, testList, useFileInStackQualifiedName, stackElementFilters);
	}

	// JS parameters (Test, Error)
	public void addError(Object test, Object throwable)
	{
		handler.addError(test, throwable);
	}

	// JS parameters (Test, AssertionFailedError)
	public void addFailure(Object test, Object assertionfailederror)
	{
		handler.addFailure(test, assertionfailederror);
	}

	// JS parameters (Test)
	public void endTest(Object test)
	{
		handler.endTest(test);
	}

	// JS parameters (Test)
	public void startTest(Object test)
	{
		handler.startTest(test);
	}

	public void setResult(Object result)
	{
		if (result instanceof Scriptable) handler.setJSResult((Scriptable)result);
	}

}