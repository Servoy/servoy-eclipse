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
import java.util.Stack;
import java.util.regex.Pattern;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * Class that makes the conversion between JSUnit test result and JUnit test result.
 * 
 * @author acostescu
 */
@SuppressWarnings("nls")
public abstract class JSUnitTestListenerHandler<T, E>
{

	protected static final String[] DEFAULT_STACK_ELEMENT_FILTERS = new String[] { "\\AJsUnit.js\\z", "\\AsuiteName\\z", "\\AJsUtil.js\\z", "\\AJsUnitToJava.js\\z" };

	private final TestResult result;
	private final List<Test> testList;
	private final Stack<Test> testStack = new Stack<Test>();
	private int startedTestsCount;
	private final Pattern[] stackElementFilters;

	public JSUnitTestListenerHandler(TestResult result, List<Test> testList)
	{
		this(result, testList, null);
	}

	/**
	 * @param stackElementFilters a list of regex strings (see {@link String#matches(String)}). If any of these match the file/method name in a stack element of a failure/error, that stack element
	 * will be ignored. 
	 */
	public JSUnitTestListenerHandler(TestResult result, List<Test> testList, String[] stackElementFilters)
	{
		this.result = result;
		this.testList = testList;

		final int filtersSize = (stackElementFilters == null ? 0 : stackElementFilters.length);
		final int defaultFiltersSize = DEFAULT_STACK_ELEMENT_FILTERS.length;
		this.stackElementFilters = new Pattern[defaultFiltersSize + filtersSize];
		int i = 0;
		for (; i < defaultFiltersSize; i++)
		{
			this.stackElementFilters[i] = Pattern.compile(DEFAULT_STACK_ELEMENT_FILTERS[i]);
		}
		for (; i < filtersSize + defaultFiltersSize; i++)
		{
			this.stackElementFilters[i] = Pattern.compile(stackElementFilters[i - defaultFiltersSize]);
		}
	}

	// JS parameters (Test, Error)
	public void addError(T test, E throwable)
	{
		applyShouldStopToJSIfNeeded();

		String testName = getTestObjectName(test);
		String errorMsg = getThrowableMsg(testName, throwable);
		StackTraceElement[] stackTrace = getStackTrace(testName, throwable);

		Test currentTest = testStack.peek();
		if (!sameName(currentTest, testName))
		{
			System.err.println("Error for test " + testName + " while another test is running:" + getTestName(currentTest));
		}

		Error err = new Error(errorMsg);
		if (stackTrace == null) stackTrace = new StackTraceElement[0];
		err.setStackTrace(stackTrace);
		result.addError(currentTest, err);
	}

	// JS parameters (Test, AssertionFailedError)
	public void addFailure(T test, E assertionfailederror)
	{
		applyShouldStopToJSIfNeeded();

		String testName = getTestObjectName(test);
		String failureMsg = getThrowableMsg(testName, assertionfailederror);
		StackTraceElement[] stackTrace = getStackTrace(testName, assertionfailederror);

		Test currentTest = testStack.peek();
		if (!sameName(currentTest, testName))
		{
			System.err.println("Failure for test " + testName + " while another test is running:" + getTestName(currentTest));
		}

		AssertionFailedError failure = new AssertionFailedError(failureMsg);
		if (stackTrace == null) stackTrace = new StackTraceElement[0];
		failure.setStackTrace(stackTrace);
		result.addFailure(currentTest, failure);
	}

	protected boolean isStackElementFilterMatch(String fileOrMethodName)
	{
		boolean match = false;
		if (stackElementFilters != null)
		{
			for (int i = 0; i < (stackElementFilters.length) && !match; i++)
			{
				match = stackElementFilters[i].matcher(fileOrMethodName).find();
			}
		}
		return match;
	}

	public static String getFileNameAsJavaType(String fileOrMethodName)
	{
		return fileOrMethodName.replace(".", "._").replace(":\\", "...").replace("/", "..").replace("\\", "..");
	}

	public static String getFileNameFromJavaType(String javaType)
	{
		return javaType.replaceFirst("\\.\\.\\.", ":/").replace("..", "/").replace("._", ".");
	}

	// JS parameters (Test)
	public void endTest(T test)
	{
		applyShouldStopToJSIfNeeded();
		String testName = getTestObjectName(test);

		Test currentTest = testStack.pop();
		if (!sameName(currentTest, testName))
		{
			System.err.println("End test " + testName + " while another test end was expected:" + getTestName(currentTest));
		}
		if (!(currentTest instanceof TestSuite)) // for some (BAD) reason JUnit normally doesn't signal the start/stop of test suites; so we'll do the same
		{
			result.endTest(currentTest);
		}
	}

	// JS parameters (Test)
	public void startTest(T test)
	{
		String testName = getTestObjectName(test);
		applyShouldStopToJSIfNeeded();

		Test currentTest = testList.get(startedTestsCount);
		testStarted(currentTest);

		if (sameName(currentTest, testName))
		{
			startedTestsCount++;
		}
		else
		{
			currentTest = new DummyTestCase(testName);
		}
		testStack.push(currentTest);
		if (!(currentTest instanceof TestSuite)) // for some reason JUnit normally doesn't signal the start/stop of test suites
		{
			result.startTest(currentTest);
		}
	}

	/**
	 * Subclasses can override when they need to do something extra.
	 */
	protected void testStarted(@SuppressWarnings("unused")
	Test currentTest)
	{
		// nothing to do here
	}

	private boolean sameName(Test test, String testName)
	{
		String name = getTestName(test);
		return (name == testName) || (name != null && name.equals(testName));
	}

	protected String getTestName(Test test)
	{
		String name = null;
		if (test instanceof TestCase)
		{
			name = ((TestCase)test).getName();
		}
		else if (test instanceof TestSuite)
		{
			name = ((TestSuite)test).getName();
		}
		return name;
	}

	private void applyShouldStopToJSIfNeeded()
	{
		if (result.shouldStop())
		{
			applyShouldStop();
		}
	}

	public boolean shouldStop()
	{
		return result.shouldStop();
	}

	protected abstract String getTestObjectName(T test);

	protected abstract String getThrowableMsg(String testName, E throwable);

	protected abstract StackTraceElement[] getStackTrace(String testName, E throwable);

	protected abstract void applyShouldStop();

}