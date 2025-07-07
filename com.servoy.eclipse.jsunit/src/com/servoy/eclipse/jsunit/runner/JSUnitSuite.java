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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Scriptable;

import com.servoy.eclipse.model.util.ServoyLog;

import de.berlios.jsunit.JsUnitException;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * This class runs a JSUnit testsuite wrapped as a JUnit TestSuite.
 * @author acostescu
 */
public class JSUnitSuite extends TestSuite
{

	private JSUnitToJavaRunner runner;
	protected List<Test> testList = null;
	private String jsSuiteClassName;

	private String testFileName;
	private boolean createSeparateScopeForTestCode;
	private boolean useFileInStackQualifiedName = false;
	private String[] stackElementFilters;
	private boolean useDebugger;

	public static Test suite()
	{
		InputStream is = JSUnitSuite.class.getResourceAsStream("/com/servoy/j2db/jsunit/jsTest.js");
		Reader jsTestFileReader = (is == null) ? null : new InputStreamReader(is);
		return jsTestFileReader == null ? new TestSuite() : new JSUnitSuite(jsTestFileReader, "JsUnitTestSuite", "jsTest.js", null, false, false);
	}

	/**
	 * Creates a JUnit test suite that really executes the JSUnit test suite <code>jsSuiteName</code> that can be found in the given <code>jsTestCode</code>.
	 * JSUnit code and test code will be loaded/interpreted in the provided scope.
	 *
	 * @param jsTestCode the JS test code (suites/test cases).
	 * @param jsSuiteClassName the name of the JS test suite class to execute that is declared in <code>jsTestCode</code>.
	 * @param testFileName the name of the test file name (used when reporting call stacks.
	 * @param scope the scope in which the JSUnit code and test code (if createSeparateScopeForTestCode is false) will be loaded and interpreted. If this is
	 *            <code>null</code> a new scope will be created.
	 * @param createSeparateScopeForTestCode if this is true, the test code will not be loaded in <code>scope</code>; another child scope will be created in
	 *            which the tests reside.<br>
	 *            This can be helpful if you want to reuse the <code>scope</code> and not keep adding code to it... A separate test scope can be disposed of
	 *            easier. If <code>scope</code> this should be false. If this is true and you are using glue() to associate object functions to the prototype,
	 *            make sure you call <code>FunctionName.glue(this)</code> instead of <code>FunctionName.glue()</code>, because it needs the correct scope to
	 *            work with.
	 * @param useDebugger if this is true a Rhino debugger will be used for better stack trace reporting. This means interpreted mode (not compiled) will be used.
	 * If it is false, compiled mode will be used without a debugger, resulting in not-so-useful stack traces.
	 */
	public JSUnitSuite(String jsTestCode, String jsSuiteClassName, String testFileName, Scriptable scope, boolean createSeparateScopeForTestCode,
		boolean useDebugger)
	{
		this(new StringReader(jsTestCode), jsSuiteClassName, testFileName, scope, createSeparateScopeForTestCode, useDebugger);
	}

	/**
	 * Creates a JUnit test suite that really executes the JSUnit test suite <code>jsSuiteName</code> that can be found in the given <code>jsTestCode</code>.
	 * JSUnit code and test code will be loaded/interpreted in the provided scope.
	 *
	 * @param jsTestCode the JS test code (suites/test cases) reader. The reader will be closed after reading.
	 * @param jsSuiteClassName the name of the JS test suite class to execute that is declared in <code>jsTestCode</code>.
	 * @param testFileName the name of the test file name (used when reporting call stacks.
	 * @param scope the scope in which the JSUnit code and test code (if createSeparateScopeForTestCode is false) will be loaded and interpreted. If this is
	 *            <code>null</code> a new scope will be created.
	 * @param createSeparateScopeForTestCode if this is true, the test code will not be loaded in <code>scope</code>; another child scope will be created in
	 *            which the tests reside.<br>
	 *            This can be helpful if you want to reuse the <code>scope</code> and not keep adding code to it... A separate test scope can be disposed of
	 *            easier. If <code>scope</code> this should be false. If this is true and you are using glue() to associate object functions to the prototype,
	 *            make sure you call <code>FunctionName.glue(this)</code> instead of <code>FunctionName.glue()</code>, because it needs the correct scope to
	 *            work with.
	 * @param useDebugger if this is true a Rhino debugger will be used for better stack trace reporting. This means interpreted mode (not compiled) will be used.
	 * If it is false, compiled mode will be used without a debugger, resulting in not-so-useful stack traces.
	 */
	public JSUnitSuite(Reader jsTestCode, String jsSuiteClassName, String testFileName, Scriptable scope, boolean createSeparateScopeForTestCode,
		boolean useDebugger)
	{
		super();
		init(jsTestCode, jsSuiteClassName, testFileName, scope, createSeparateScopeForTestCode, useDebugger);
	}

	/**
	 * Constructs a un-initialized instance. You <b>MUST</b> call <code>init()</code> before using this suite.
	 */
	protected JSUnitSuite()
	{
	}

	/**
	 * Prepares a JUnit test suite that really executes the JSUnit test suite <code>jsSuiteName</code> that can be found in the given <code>jsTestCode</code>.
	 * JSUnit code and test code will be loaded/interpreted in the provided scope.<br>
	 * This method <b>MUST</b> be called when protected constructor is used!
	 *
	 * @param jsTestCode the JS test code (suites/test cases) reader. The reader will be closed after reading.
	 * @param jsSuiteClassName the name of the JS test suite class to execute that is declared in <code>jsTestCode</code>.
	 * @param testFileName the name of the test file name (used when reporting call stacks.
	 * @param scope the scope in which the JSUnit code and test code (if createSeparateScopeForTestCode is false) will be loaded and interpreted. If this is
	 *            <code>null</code> a new scope will be created.
	 * @param createSeparateScopeForTestCode if this is true, the test code will not be loaded in <code>scope</code>; another child scope will be created in
	 *            which the tests reside.<br>
	 *            This can be helpful if you want to reuse the <code>scope</code> and not keep adding code to it... A separate test scope can be disposed of
	 *            easier. If <code>scope</code> this should be false. If this is true and you are using glue() to associate object functions to the prototype,
	 *            make sure you call <code>FunctionName.glue(this)</code> instead of <code>FunctionName.glue()</code>, because it needs the correct scope to
	 *            work with.
	 * @param useDebugger if this is true a Rhino debugger will be used for better stack trace reporting. This means interpreted mode (not compiled) will be used.
	 * If it is false, compiled mode will be used without a debugger, resulting in not-so-useful stack traces.
	 */
	protected void init(Reader jsTestCode, String jsSuiteClassName, String testFileName, Scriptable scope, boolean createSeparateScopeForTestCode,
		boolean useDebugger)
	{
		setName(jsSuiteClassName);

		this.testFileName = testFileName;
		this.createSeparateScopeForTestCode = createSeparateScopeForTestCode;
		this.useDebugger = useDebugger;

		this.jsSuiteClassName = jsSuiteClassName;
		runner = new JSUnitToJavaRunner(scope, createSeparateScopeForTestCode, useDebugger);

		try
		{
			runner.evaluateReader(jsTestCode, testFileName);
			List<String> testTree = runner.getTestTree(jsSuiteClassName);

			TestTreeHandler treeHandler = new TestTreeHandler(testTree.toArray(new String[testTree.size()]), this);
			treeHandler.createDummyTestTree();

			testList = new ArrayList<Test>();
			testList.add(this);
			treeHandler.fillTestListSequencialOrder(testList);
		}
		catch (JsUnitException | IOException e)
		{
			ServoyLog.logError(e);
		}
	}

	/**
	 * When Java exception stacks are generated from javascript stacks, if this is set to true, the javascript filename is transformed into a Java package like string and
	 * set to the "java type" part of the stack trace (this is less user-readeable, but makes integrating with some existing java tools easier - such as eclipse test runner view).
	 * @param useFileInStackQualifiedName set it to true if you want stack traces to show java type like string based on javascript file; default value is false.
	 */
	public void setUseFileForJavaQualifiedNameInStack(boolean useFileInStackQualifiedName)
	{
		this.useFileInStackQualifiedName = useFileInStackQualifiedName;
	}

	/**
	 * Set this to hide stack elements that match the given filters.
	 * @param stackElementFilters a list of regex strings (see {@link String#matches(String)}). If any of these match the file/method name in a stack element of a failure/error, that stack element
	 * will be ignored.
	 */
	public void setStackElementFilters(String[] stackElementFilters)
	{
		this.stackElementFilters = stackElementFilters;
	}

	/**
	 * Call this if you do not want references to used scopes to prevent garbage collection, but if you still plan to use this test suite
	 * later. When you do, you MUST call {@link #changeScope(Scriptable, Reader)} to reinitialise the test scope before using the suite again.<BR>
	 *
	 * This is useful in some cases where creating the test suite hierarchy is needed before unit tests start running (such as UI/ant reporting).
	 * After you create the test suite hierarchy, the suite might not be used for a long while, and you might want to free up some memory, which
	 * will be re-allocated when the test is run.
	 *
	 * This method is automatically called after the suite runs.
	 */
	protected void releaseScopes()
	{
		runner = null;
	}

	protected boolean areScopesReleased()
	{
		return runner == null;
	}

	/**
	 * Can be called if the test scopes need to be re-initialized. See {@link #releaseScopes()}.
	 * @param scope new scope.
	 * @param jsTestCode the code to be tested.
	 */
	protected void changeScope(Scriptable scope, Reader jsTestCode)
	{
		runner = new JSUnitToJavaRunner(scope, createSeparateScopeForTestCode, useDebugger);

		try
		{
			runner.evaluateReader(jsTestCode, testFileName);
		}
		catch (JsUnitException | IOException e)
		{
			ServoyLog.logError(e);
		}
	}

	@Override
	public void run(TestResult result)
	{
		JSUnitTestListener testListener = new JSUnitTestListener(result, testList, useFileInStackQualifiedName, stackElementFilters);
		try
		{
			runner.runSuite(testListener, jsSuiteClassName);
			if (result.shouldStop())
			{
				Test runningTest = testListener.popLastStartedTest();
				result.addError(runningTest != null ? runningTest : this,
					new Exception("Unit tests stopped by user..."));
				testListener.stopAllStartedSuites();
			}
		}
		catch (RuntimeException e)
		{
			// finish nicely - if jUnit tests result in an exception being thrown instead of normally,
			// the tools that display results might end up in an un-determined state (for example the jUnit eclipse result view)
			if (!"Current script terminated".equals(e.getMessage()) && !"Script execution stopped".equals(e.getMessage()))
			{
				Test runningTest = testListener.popLastStartedTest();
				result.addError(runningTest != null ? runningTest : this, e);
				ServoyLog.logError(e);
			}
			else
			{
				// else intentional shut down of the javascript engine, probably due to an user action
				Test runningTest = testListener.popLastStartedTest();
				result.addError(runningTest != null ? runningTest : this,
					new Exception("Unit tests stopped by user..."));
			}
			testListener.stopAllStartedSuites();
		}
		catch (ThreadDeath e)
		{ // don't catch ThreadDeath by accident (this is the same as in junit.framework.TestResult.runProtected(Test, Protectable))
			throw e;
		}
		catch (Throwable e)
		{
			Test runningTest = testListener.popLastStartedTest();
			result.addError(runningTest != null ? runningTest : this, new RuntimeException(
				"A throwable was caught that interrupted normal test flow;\nTHIS WILL STOP ANY OF THE FOLLOWING TESTS FROM BEING RUN, SO YOU ARE PROBABLY SEEING JUST PARTIAL TEST RESULTS\nPlease fix this error in order to allow all jsunit tests to run as expected. It can happen if for example some jar is missing from the classpath, but other causes can appear as well.\nSee \"Caused by\" throwable below:",
				e));
			testListener.stopAllStartedSuites();
		}
		finally
		{
			releaseScopes();
		}
	}

}