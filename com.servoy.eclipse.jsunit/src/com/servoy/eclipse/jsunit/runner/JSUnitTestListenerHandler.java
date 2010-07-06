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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeError;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Class that makes the conversion between JSUnit test result and JUnit test result.
 * 
 * @author Andrei Costescu
 * 
 */
public class JSUnitTestListenerHandler
{

	private final TestResult result;
	private final List<Test> testList;
	private final Stack<Test> testStack = new Stack<Test>();
	private int startedTestsCount;
	private final boolean useFileInStackQualifiedName;

	public JSUnitTestListenerHandler(TestResult result, List<Test> testList, boolean useFileInStackQualifiedName)
	{
		this.result = result;
		this.testList = testList;
		this.useFileInStackQualifiedName = useFileInStackQualifiedName;
	}

	// JS parameters (Test, Error)
	public void addError(Object test, Object throwable)
	{
		String testName = null;
		String errorMsg = null;
		StackTraceElement[] stackTrace = null;
		Context context = Context.enter();
		try
		{
			if (test instanceof Scriptable)
			{
				testName = context.evaluateString((Scriptable)test, "this.getName()", "Get test name", 1, null).toString();
			}
			if (throwable instanceof Scriptable)
			{
				errorMsg = getMessage(context, (Scriptable)throwable);
				stackTrace = getStackTrace(context, (Scriptable)throwable);
			}
		}
		finally
		{
			Context.exit();
		}
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
	public void addFailure(Object test, Object assertionfailederror)
	{
		String testName = null;
		String failureMsg = null;
		StackTraceElement[] stackTrace = null;
		Context context = Context.enter();
		try
		{
			if (test instanceof Scriptable)
			{
				testName = context.evaluateString((Scriptable)test, "this.getName()", "Get test name", 1, null).toString();
			}
			if (assertionfailederror instanceof Scriptable)
			{
				failureMsg = getMessage(context, (Scriptable)assertionfailederror);
				stackTrace = getStackTrace(context, (Scriptable)assertionfailederror);
			}
		}
		finally
		{
			Context.exit();
		}
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

	private String getMessage(Context context, Scriptable throwable)
	{
		return context.evaluateString(throwable, "this.toString()", "Get error/failure message", 1, null).toString();
	}

	private StackTraceElement[] getStackTrace(Context context, Scriptable throwable)
	{
		StackTraceElement[] stackTrace = null;
		Object stackObj = context.evaluateString(throwable, "this.rhinoException ? this.rhinoException : (this.javaException ? this.javaException : null)",
			"Get error/failure stack", 1, null);
//		Object stackObj = context.evaluateString(throwable, "JsUnitError.prototype", "Get error/failure stack", 1, null);
		if (stackObj instanceof NativeJavaObject)
		{
			stackObj = ((NativeJavaObject)stackObj).unwrap();
		}
		if (stackObj instanceof RhinoException)
		{
			RhinoException re = (RhinoException)stackObj;
			StringWriter sr = new StringWriter();
			re.printStackTrace(new PrintWriter(sr));
			// This exception is either automatically attached by the RHINO engine in case of errors generated by the engine itself,
			// or it is an exception interposed by JsUnitToJava.js into the JsUnit standard errors - thrown by JsUnit code.
			// In the second case, the stack elements that lead to the creation of this RhinoException must be ignored (as they are not part
			// of the useful client stack)
			// boolean ignoreAllButClientStackTraces = (JSUnitToJavaRunner.ASSERTION_EXCEPTION_MESSAGE.equals(re.getMessage())); // this line can be uncommented to show stack inside testing code

			// cannot use re.getStackTrace() instead of re.printStackTrace() for nicer stack parsing because interpreted JS stack frames would be missing
			// - RhinoException.printStackTrace() adds some more stack info to the existing stack frames in case of interpreted JS
			StringTokenizer completeStack = new StringTokenizer(sr.toString(), "\n");
			List<StackTraceElement> filteredStack = new ArrayList<StackTraceElement>();
			while (completeStack.hasMoreTokens())
			{
				String line = completeStack.nextToken();
				int openBracketOffset = line.lastIndexOf('(');
				int closeBracketOffset = line.lastIndexOf(')');
				if (openBracketOffset < closeBracketOffset && openBracketOffset != -1)
				{
					String fileOrMethodAndLine = line.substring(openBracketOffset + 1, closeBracketOffset);
					// in case of compiled runtime script, this will be "methodName:lineNumberInFile";
					// for uncompiled/developer scripts it will be "fileName:lineNumber"
					String fileOrMethodName = null;
					int lineNumber = -1;

					int delim = fileOrMethodAndLine.lastIndexOf(':');
					if (delim != -1)
					{
						fileOrMethodName = fileOrMethodAndLine.substring(0, delim);
						try
						{
							lineNumber = Integer.parseInt(fileOrMethodAndLine.substring(delim + 1));
						}
						catch (NumberFormatException e)
						{
						}
					}
					else
					{
						fileOrMethodName = fileOrMethodAndLine;
					}
					boolean isFile = (fileOrMethodName.endsWith(".js"));
					boolean isMethod = ((!isFile) && line.contains("org.mozilla.javascript.gen."));
					if (fileOrMethodName != null &&
						lineNumber > -1 &&
						(isFile || isMethod) &&
						!/* (ignoreAllButClientStackTraces && */(fileOrMethodName.equals("JsUnit.js") || fileOrMethodName.equals("suiteName") ||
							fileOrMethodName.equals("JsUtil.js") || fileOrMethodName.equals("JsUnitToJava.js"))/* ) */) // this line can be uncommented to show stack inside testing code
					{
						// ignoreAllButClientStackTraces = false; // this line can be uncommented to show stack inside testing code
						if (useFileInStackQualifiedName)
						{
							filteredStack.add(new StackTraceElement(isFile ? getFileNameAsJavaType(fileOrMethodName) : "javascript", isMethod
								? fileOrMethodName : "method", "file", lineNumber));
						}
						else
						{
							filteredStack.add(new StackTraceElement("javascript", isMethod ? fileOrMethodName : "method", isFile ? fileOrMethodName : "file",
								lineNumber));
						}
					}
				}
			}
			stackTrace = filteredStack.toArray(new StackTraceElement[filteredStack.size()]);
		}
		else if (throwable instanceof NativeError)
		{
			stackTrace = new StackTraceElement[1];
			if (useFileInStackQualifiedName)
			{
				stackTrace[0] = new StackTraceElement(getFileNameAsJavaType(getScriptablePropertyAsString(throwable, "fileName")), "javascript", "file",
					getScriptablePropertyAsInt(throwable, "lineNumber"));
			}
			else
			{
				stackTrace[0] = new StackTraceElement("java", "script", getScriptablePropertyAsString(throwable, "fileName"), getScriptablePropertyAsInt(
					throwable, "lineNumber"));
			}
		}
		else
		{
			stackTrace = new StackTraceElement[0];
		}
		return stackTrace;
	}

	public static String getFileNameAsJavaType(String fileOrMethodName)
	{
		return fileOrMethodName.replace(".", "..").replace(":\\", "...").replace("/", ".").replace("\\", ".");
	}

	public static String getFileNameFromJavaType(String javaType)
	{
		return javaType.replace("...", ":/").replace("..", ".").replace(".", "/");
	}

	private String getScriptablePropertyAsString(Scriptable s, String property)
	{
		Object value = ScriptableObject.getProperty(s, property);
		if (value == Scriptable.NOT_FOUND) return "";
		return ScriptRuntime.toString(value);
	}

	private int getScriptablePropertyAsInt(Scriptable s, String property)
	{
		String stringValue = getScriptablePropertyAsString(s, property);
		int value;
		try
		{
			value = Integer.parseInt(stringValue);
		}
		catch (NumberFormatException e)
		{
			value = 0;
		}
		return value;
	}

	// JS parameters (Test)
	public void endTest(Object test)
	{
		String testName = null;
		Context context = Context.enter();
		try
		{
			if (test instanceof Scriptable)
			{
				testName = context.evaluateString((Scriptable)test, "this.getName()", "Get test name", 1, null).toString();
			}
		}
		finally
		{
			Context.exit();
		}
		Test currentTest = testStack.pop();
		if (!sameName(currentTest, testName))
		{
			System.err.println("End test " + testName + " while another test end was expected:" + getTestName(currentTest));
		}
		if (!(currentTest instanceof TestSuite)) // for some reason JUnit normally doesn't signal the start/stop of test suites
		{
			result.endTest(currentTest);
		}
	}

	// JS parameters (Test)
	public void startTest(Object test)
	{
		String testName = null;
		Context context = Context.enter();
		try
		{
			if (test instanceof Scriptable)
			{
				testName = context.evaluateString((Scriptable)test, "this.getName()", "Get test name", 1, null).toString();
			}
		}
		finally
		{
			Context.exit();
		}
		Test currentTest = testList.get(startedTestsCount);
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

	private boolean sameName(Test test, String testName)
	{
		String name = getTestName(test);
		return (name == testName) || (name != null && name.equals(testName));
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

}