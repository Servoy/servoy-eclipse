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
import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;

import com.servoy.base.test.IJSUnitSuiteHandler;
import com.servoy.eclipse.jsunit.runner.JSUnitTestListenerHandler;
import com.servoy.eclipse.jsunit.runner.JSUnitToJavaRunner;
import com.servoy.eclipse.jsunit.runner.TestTreeHandler;

import de.berlios.jsunit.JsUnitException;

/**
 * A simulated JUnit test suite helper that is driven by something else behind the scenes. (eg. a jsUnit mobile suite running in a browser)
 * 
 * @author acostescu
 */
public class SuiteBridge implements IJSUnitSuiteHandler
{

	public final static int DEFAULT_TEST_TREE_WAIT_TIMEOUT = 30 * 1000;
	private final static int DEFAULT_STOP_REQUESTED_WAIT = 25 * 1000;

	private static int idCount = 1;
	private static final Log log = LogFactory.getLog(JSUnitToJavaRunner.class);

	private final int id;

	private final Object testTreeLock = new Object();
	private String[] testTree = null;
	private int testTreeWaitTimeout = DEFAULT_TEST_TREE_WAIT_TIMEOUT;
	private int stopRequestedWait = DEFAULT_STOP_REQUESTED_WAIT;
	private List<Test> testList;
	private JSUnitTestListenerHandler<String, Throwable> junitHandler;

	private final Object runLock = new Object();
	private Throwable unexpectedRunThrowable = null;
	private boolean doneTesting = false;
	private TestCycleListener testCycleListener;
	private String solutionSuiteName;
	private String solutionSuiteJSCode;

	public SuiteBridge()
	{
		this.id = idCount++;
//		this.id = 0; // use this one instead for debugging test client
	}

	/**
	 * @param treeWaitTimeout in seconds; the time to wait for mobile client to connect with tests. After it expires tests will fail and stop execution. -1 will fall back to default value.
	 * @param stopRequestedWait in seconds; the time to wait for client tests to finish nicely when the user presses the "stop" buton on tests; after it expires the tests will be terminated anyway. -1 will fallback to default value.
	 */
	public SuiteBridge(int treeWaitTimeout, int stopRequestedWait)
	{
		this();
		this.testTreeWaitTimeout = treeWaitTimeout > 0 ? treeWaitTimeout * 1000 : DEFAULT_TEST_TREE_WAIT_TIMEOUT;
//		this.testTreeWaitTimeout = 600000; // use this one instead for debugging test client
		this.stopRequestedWait = stopRequestedWait > 0 ? stopRequestedWait * 1000 : DEFAULT_STOP_REQUESTED_WAIT;
	}

	/**
	 * Call this if the javascript js unit suite code is generated serverside and should be pushed to the client - instead
	 * of letting the client create it.
	 * @param suiteName root suite class name.
	 * @param code the complete js unit suite code.
	 */
	public void setSolutionSuiteJSCode(String suiteName, String code)
	{
		this.solutionSuiteJSCode = code;
		this.solutionSuiteName = suiteName;
	}

	/**
	 * This id identifies the JUnit run session. It helps to make sure the right bridge instance is used in case of multiple executions...
	 */
	public int getId()
	{
		return id;
	}

	public String[] getJsUnitJavascriptCode()
	{
		log.info("[.......] Getting javascript library code"); //$NON-NLS-1$
		String[] libs = new String[3];
		libs[0] = JSUnitToJavaRunner.getScriptAsStringFromResource("this.JsUtilLoaded", JsUnitException.class, "/JsUtil.js").replace( //$NON-NLS-1$//$NON-NLS-2$
			"var r = /function (\\w+)(", "var r = /function *(\\w*)(\\("); // if you had "function(){}" with no space after "function", a wrong function name could appear in the call stack //$NON-NLS-1$//$NON-NLS-2$ 
		libs[1] = JSUnitToJavaRunner.getScriptAsStringFromResource("this.TestCaseLoaded", JsUnitException.class, "/JsUnit.js"); //$NON-NLS-1$//$NON-NLS-2$
		libs[2] = JSUnitToJavaRunner.getScriptAsStringFromResource("this.JsUnitToJavaLoaded", JSUnitToJavaRunner.class, "JsUnitToJava.js"); //$NON-NLS-1$//$NON-NLS-2$
		return libs;
	}

	/**
	 * Waits for the client to transmit the flattened test tree then reconstructs it as a JUnit test suite hierarchy.
	 * @param testSuite the root test-suite to use.
	 * @param staticLaunchMonitor launch monitor that can be used to check for stop requests from the user.
	 */
	public void createTestTree(TestSuite testSuite, IProgressMonitor staticLaunchMonitor)
	{
		synchronized (testTreeLock)
		{
			long ct = System.currentTimeMillis();
			while (testTree == null && (System.currentTimeMillis() - ct) < testTreeWaitTimeout && unexpectedRunThrowable == null)
			{
				try
				{
					testTreeLock.wait(1000);
					if (staticLaunchMonitor != null && staticLaunchMonitor.isCanceled())
					{
						// if the user is in a hurry or not going to wait for "testTreeWaitTimeout" when something went wrong
						testSuite.setName("Stop requested"); //$NON-NLS-1$
						unexpectedRunThrowable = new Throwable(
							"Stopped manually after " + ((System.currentTimeMillis() - ct) / 1000 + " sec of waiting for mobile client to connect... ")); //$NON-NLS-1$ //$NON-NLS-2$
						unexpectedRunThrowable.setStackTrace(new StackTraceElement[0]);
						break;
					}
				}
				catch (InterruptedException e)
				{
					log.error(e);
				}
			}
			if (testTree == null && unexpectedRunThrowable == null)
			{
				testSuite.setName("Connection problem"); //$NON-NLS-1$
				unexpectedRunThrowable = new Throwable(
					"Timed out - " + ((System.currentTimeMillis() - ct) / 1000 + " sec - waiting for mobile client to connect... ")); //$NON-NLS-1$ //$NON-NLS-2$
				unexpectedRunThrowable.setStackTrace(new StackTraceElement[0]);
			}
			if (unexpectedRunThrowable != null)
			{
				testSuite.addTest(new TestCase(unexpectedRunThrowable.getMessage())
				{
				});
			}
		}
		testList = new ArrayList<Test>();
		testList.add(testSuite);

		TestTreeHandler tth = new TestTreeHandler(testTree, testSuite);
		if (testTree != null) tth.createDummyTestTree();
		tth.fillTestListSequencialOrder(testList);
	}

	/**
	 * Uses the given result object to "run" the dummy JUnit test suite controlled remotely by the bridge. It will return only after remote tests are over (either passed, failure or error).
	 */
	public void runClientTests(TestResult result)
	{
		junitHandler = new JSUnitMobileTestListenerHandler(result, testList);

		if (testCycleListener != null) testCycleListener.started();
		else log.info("When starting test run, runStartListener is null (in bridge)."); //$NON-NLS-1$

		synchronized (runLock)
		{
			if (unexpectedRunThrowable != null)
			{
				// if it has already errored out, we need to fake some starts to simulate the dummy testcase start
				junitHandler.startTest(((TestSuite)testList.get(0)).getName());
				junitHandler.startTest(unexpectedRunThrowable.getMessage());
			}
			else
			{
				while (!doneTesting && unexpectedRunThrowable == null)
				{
					try
					{
						runLock.wait(1000);
						if (junitHandler.shouldStop())
						{
							// normally when this happens a "done" should be generated clientside;
							// but if something bad happened and the client is no longer available, just end it after a reasonable amount of time
							runLock.wait(stopRequestedWait);
							if (!doneTesting && unexpectedRunThrowable == null)
							{
								log.warn("Stop requested; Shutting down server side because client side didn't report as stopped in under " + (stopRequestedWait / 1000) + " seconds."); //$NON-NLS-1$//$NON-NLS-2$
								break; // end anyway
							}
						}
					}
					catch (InterruptedException e)
					{
						log.error(e);
					}
				}
			}
		}
		showUnexpectedThrowableIfNeeded();
		if (testCycleListener != null) testCycleListener.finished();
		log.info("Test session finished."); //$NON-NLS-1$
	}

	private void showUnexpectedThrowableIfNeeded()
	{
		if (unexpectedRunThrowable != null)
		{
			junitHandler.addError(unexpectedRunThrowable.getMessage(), unexpectedRunThrowable);
		}
	}

	@Override
	public void setFlattenedTestTree(String[] testTree)
	{
		log.info("[.......] setFlattenedTestTree - " + Arrays.asList(testTree).toString()); //$NON-NLS-1$
		synchronized (testTreeLock)
		{
			this.testTree = testTree;
			testTreeLock.notifyAll();
		}
	}

	public void addError(final String testName, final Throwable throwable)
	{
		log.info("[.......] addError - " + testName + "; throwable: " + throwable); //$NON-NLS-1$ //$NON-NLS-2$
		junitHandler.addError(testName, throwable);
	}

	public void addFailure(final String testName, final Throwable throwable)
	{
		log.info("[.......] addFailure - " + testName + "; throwable: " + throwable); //$NON-NLS-1$ //$NON-NLS-2$
		junitHandler.addFailure(testName, throwable);
	}

	public void startTest(final String testName)
	{
		log.info("[.......] startTest - " + testName); //$NON-NLS-1$
		junitHandler.startTest(testName);
	}

	public void endTest(final String testName)
	{
		log.info("[.......] endTest - " + testName); //$NON-NLS-1$
		junitHandler.endTest(testName);
	}

	public boolean isStopped()
	{
		return junitHandler != null ? junitHandler.shouldStop() : false;
	}

	@Override
	public void doneTesting()
	{
		log.info("[.......] DONE TESTING."); //$NON-NLS-1$
		synchronized (runLock)
		{
			doneTesting = true;
			runLock.notifyAll();
		}
	}

	public void reportUnexpectedThrowable(String msg, Throwable t)
	{
		log.error("[.......] unexpected throwable: ", t); //$NON-NLS-1$

		synchronized (runLock)
		{
			synchronized (testTreeLock)
			{
				unexpectedRunThrowable = t;
				runLock.notifyAll();
				testTreeLock.notifyAll();
			}
		}
	}

	@Override
	public void registerRunStartListener(TestCycleListener l)
	{
		this.testCycleListener = l;
	}

	public List<Test> getTestList()
	{
		return testList;
	}

	@Override
	public String[] getSolutionJsUnitJavascriptCode()
	{
		return (solutionSuiteJSCode != null && solutionSuiteName != null) ? new String[] { solutionSuiteName, solutionSuiteJSCode } : null;
	}

}
