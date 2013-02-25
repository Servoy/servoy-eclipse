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

package com.servoy.eclipse.jsunit.scriptunit;

import java.util.List;
import java.util.Stack;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.dltk.testing.ITestingClient;
import org.eclipse.dltk.testing.model.ITestRunSession;

import com.servoy.eclipse.jsunit.runner.JSUnitTestListenerHandler;

/**
 * This class is used by {@link JSUnitTestListenerHandler}  and instantiated for each test run .
 * It forwards the test Started ,test failed , etc events that come from js code  to dltk.testing session which in term updates its model and it's view.
 * 
 * @author obuligan
 *
 */
public class ScriptUnitTestRunNotifier
{
	List<Test> testList = null;
	ITestingClient testingClient = null;
	Stack<Test> testStack = null;
	long startTime = 0;

	public ScriptUnitTestRunNotifier(List<Test> testList, Stack<Test> teststack)
	{
		this.testList = testList;
		this.testingClient = getScriptTestRunnerClient();
		this.testStack = teststack;
	}

	public void sendStartRun()
	{
		if (testingClient != null)
		{
			testingClient.testRunStart(testList.get(0).countTestCases());
			startTime = System.currentTimeMillis();
		}
	}

	public void sendTestTree()
	{
		if (testingClient != null)
		{
			for (Test test : testList)
			{
				if (test instanceof TestSuite)
				{
					testingClient.testTree(testList.indexOf(test), ((TestSuite)test).getName(), true, ((TestSuite)test).testCount());
				}
				else
				{
					testingClient.testTree(testList.indexOf(test), ((TestCase)test).getName(), false, 1);
				}
			}
		}
	}

	public void sendTestStarted(Test test)
	{
		if (testingClient != null)
		{
			testingClient.testStarted(testList.indexOf(test), getTestName(test));
		}
	}

	/**
	 * also automatically sends end of test run if it is the last test
	 * @param test
	 */
	public void sendTestEnded(Test test)
	{
		if (testingClient != null)
		{

			testingClient.testEnded(testList.indexOf(test), getTestName(test));
			if (testStack.size() == 0) testingClient.testTerminated((int)(System.currentTimeMillis() - startTime));
		}
	}

	/**
	 * Used both to send failure and send error depending on argument type Error or  AssertionFailedError 
	 * @param test
	 * @param failure
	 */
	public void sendTestFailure(Test test, Error failure)
	{
		if (testingClient != null)
		{
			if (failure instanceof AssertionFailedError)
			{
				AssertionFailedError fail = (AssertionFailedError)failure;
				testingClient.testFailed(ITestingClient.FAILED, testList.indexOf(test), ((TestCase)test).getName());
				String asertionFailedPattern = "AssertionFailedError: Expected:<(.+)>, but was:<(.+)>";
				testingClient.testExpected(fail.getMessage().replaceAll(asertionFailedPattern, "$1"));
				testingClient.testActual(fail.getMessage().replaceAll(asertionFailedPattern, "$2"));
			}
			else
			{
				testingClient.testError(testList.indexOf(test), ((TestCase)test).getName());
			}
			//send traces
			testingClient.traceStart();
			testingClient.traceMessage(failure.getMessage());
			for (StackTraceElement frame : failure.getStackTrace())
			{
				testingClient.traceMessage(JSUnitTestListenerHandler.getFileNameFromJavaType(frame.toString()));
			}
			testingClient.traceEnd();
		}
	}

	public boolean isStopRequested()
	{
		if (testingClient instanceof RemoteScriptUnitRunnerClient)
		{
			return ((RemoteScriptUnitRunnerClient)testingClient).isStopRequested();
		}
		else
		{
			return false;
		}
	}

	private String getTestName(Test test)
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


	private ITestingClient getScriptTestRunnerClient()
	{
		ITestRunSession testRunSession = org.eclipse.dltk.testing.DLTKTestingPlugin.getModel().getTestRunSession(
			com.servoy.eclipse.jsunit.launch.JSUnitLaunchConfigurationDelegate.getCurrentLaunch());
		if (testRunSession != null)
		{
			return testRunSession.getTestRunnerClient();
		}
		else
		{
			return null;
		}
	}


}
