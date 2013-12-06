/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.jsunit.mobile;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestResult;

import com.servoy.eclipse.jsunit.runner.JSUnitTestListenerHandler;

/**
 * Test listener handler for mobile client test-suites.
 * 
 * @author acostescu
 */
public class JSUnitMobileTestListenerHandler extends JSUnitTestListenerHandler<String, Throwable>
{

	public JSUnitMobileTestListenerHandler(TestResult result, List<Test> testList)
	{
		this(
			result,
			testList,
			new String[] { "\\Ajavascript\\..*\\(f:0\\)", "\\ATestClass", "\\ATestCase_", "\\ATestResult_", "\\ATestSuite_", "\\ATestDecorator_", "\\ATestSetup_" });
	}

	public JSUnitMobileTestListenerHandler(TestResult result, List<Test> testList, String[] stackElementFilters)
	{
		super(result, testList, stackElementFilters);
	}

	@Override
	protected String getTestObjectName(String test)
	{
		return test;
	}

	@Override
	protected String getThrowableMsg(String testName, Throwable throwable)
	{
		return throwable.getMessage();
	}

	@Override
	protected StackTraceElement[] getStackTrace(String testName, Throwable throwable)
	{
		StackTraceElement[] st = throwable.getStackTrace();
		ArrayList<StackTraceElement> filtered = new ArrayList<StackTraceElement>(st.length);

		for (StackTraceElement element : st)
		{
			if (!isStackElementFilterMatch(element.getMethodName()) && !isStackElementFilterMatch(element.toString())) filtered.add(element);
		}
		return filtered.toArray(new StackTraceElement[filtered.size()]);
	}

	@Override
	protected void applyShouldStop()
	{
		// do nothing; it's already done via polling shouldStop()
	}

}