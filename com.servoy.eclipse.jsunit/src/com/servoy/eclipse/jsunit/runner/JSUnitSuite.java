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
import java.util.Enumeration;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.mozilla.javascript.Scriptable;

import de.berlios.jsunit.JsUnitException;

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

	public static Test suite()
	{
		InputStream is = JSUnitSuite.class.getResourceAsStream("/com/servoy/j2db/jsunit/jsTest.js");
		Reader jsTestFileReader = (is == null) ? null : new InputStreamReader(is);
		return jsTestFileReader == null ? new TestSuite() : new JSUnitSuite(jsTestFileReader, "JsUnitTestSuite", "jsTest.js", null, false);
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
	 */
	public JSUnitSuite(String jsTestCode, String jsSuiteClassName, String testFileName, Scriptable scope, boolean createSeparateScopeForTestCode)
	{
		this(new StringReader(jsTestCode), jsSuiteClassName, testFileName, scope, createSeparateScopeForTestCode);
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
	 */
	public JSUnitSuite(Reader jsTestCode, String jsSuiteClassName, String testFileName, Scriptable scope, boolean createSeparateScopeForTestCode)
	{
		super();
		init(jsTestCode, jsSuiteClassName, testFileName, scope, createSeparateScopeForTestCode);
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
	 */
	protected void init(Reader jsTestCode, String jsSuiteClassName, String testFileName, Scriptable scope, boolean createSeparateScopeForTestCode)
	{
		setName(jsSuiteClassName);

		this.testFileName = testFileName;
		this.createSeparateScopeForTestCode = createSeparateScopeForTestCode;

		this.jsSuiteClassName = jsSuiteClassName;
		runner = new JSUnitToJavaRunner(scope, createSeparateScopeForTestCode);

		try
		{
			runner.evaluateReader(jsTestCode, testFileName);
			List<Object> testTree = runner.getTestTree(jsSuiteClassName);

			// we must create a hierarchy of JUnit test cases/test suites to match the JSUnit ones that are now in the tree;
			// this is only needed for the presentation or running tests/available tests done by the JUnit Eclipse plugin,
			// not for the actual run of the tests

			// first find out which one is a test case and which one is a test suite
			boolean[] isTestSuite = new boolean[testTree.size()];
			int currentParent = 0;
			boolean hasChildren = true; // always mark this as test suite
			for (int i = 2; i < testTree.size(); i++)
			{
				Object currentElement = testTree.get(i);
				if (currentElement == JSUnitToJavaRunner.NEXT_CHILD_GROUP)
				{
					isTestSuite[currentParent++] = hasChildren;
					while ((currentParent < testTree.size()) && (testTree.get(currentParent) == JSUnitToJavaRunner.NEXT_CHILD_GROUP))
					{
						currentParent++;
					}
					hasChildren = false;
				}
				else
				{
					hasChildren = true;
				}
			}

			// create & link test cases/test suites
			TestSuite[] suites = new TestSuite[testTree.size()];
			suites[0] = this;
			if (testTree.size() > 0)
			{
				setName((String)testTree.get(0));
			}
			currentParent = 0;
			for (int i = 2; i < testTree.size(); i++)
			{
				Object currentElement = testTree.get(i);
				if (currentElement == JSUnitToJavaRunner.NEXT_CHILD_GROUP)
				{
					currentParent++;
					while ((currentParent < testTree.size()) && (testTree.get(currentParent) == JSUnitToJavaRunner.NEXT_CHILD_GROUP))
					{
						currentParent++;
					}
				}
				else
				{
					Test currentTest = null;
					if (isTestSuite[i])
					{
						currentTest = new TestSuite((String)currentElement);
						suites[i] = (TestSuite)currentTest;
					}
					else
					{
						currentTest = new DummyTestCase((String)currentElement);
					}
					suites[currentParent].addTest(currentTest);
				}
			}

			testList = new ArrayList<Test>();
			testList.add(this);
			fillTestListSequencialOrder(this, testList);
		}
		catch (JsUnitException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
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
		runner = new JSUnitToJavaRunner(scope, createSeparateScopeForTestCode);

		try
		{
			runner.evaluateReader(jsTestCode, testFileName);
		}
		catch (JsUnitException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void fillTestListSequencialOrder(TestSuite suite, List<Test> list)
	{
		Enumeration<Test> children = suite.tests();
		while (children.hasMoreElements())
		{
			Test child = children.nextElement();
			list.add(child);
			if (child instanceof TestSuite)
			{
				fillTestListSequencialOrder((TestSuite)child, list);
			}
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
				result.addError(this, new Exception("Unit tests stopped by user..."));
			}
		}
		catch (RuntimeException e)
		{
			// finish nicely - if jUnit tests result in an exception being thrown instead of normally,
			// the tools that display results might end up in an un-determined state (for example the jUnit eclipse result view)
			if (!"Current script terminated".equals(e.getMessage()) && !"Script execution stopped".equals(e.getMessage()))
			{
				result.addError(this, e);
				e.printStackTrace();
			}
			else
			{
				// else intentional shut down of the javascript engine, probably due to an user action
				result.addError(this, new Exception("Unit tests stopped by user..."));
			}
		}
		finally
		{
			releaseScopes();
		}
	}

}