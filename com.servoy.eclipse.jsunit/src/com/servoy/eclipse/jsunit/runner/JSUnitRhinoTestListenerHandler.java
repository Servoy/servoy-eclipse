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
import java.util.StringTokenizer;

import junit.framework.Test;
import junit.framework.TestResult;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeError;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Class that makes the conversion between JSUnit test result and JUnit test result.
 * It is aware of Rhino details for better interpretation of stack traces for example.
 * 
 * @author acostescu
 * 
 */
@SuppressWarnings("nls")
public class JSUnitRhinoTestListenerHandler extends JSUnitTestListenerHandler<Object, Object>
{

	private static final String[] DEFAULT_STACK_ELEMENT_FILTERS = new String[] { "\\AJsUnit.js\\z", "\\AsuiteName\\z", "\\AJsUtil.js\\z", "\\AJsUnitToJava.js\\z" };

	private final boolean useFileInStackQualifiedName;
	private Scriptable jsResult;

	public JSUnitRhinoTestListenerHandler(TestResult result, List<Test> testList, boolean useFileInStackQualifiedName)
	{
		this(result, testList, useFileInStackQualifiedName, null);
	}

	/**
	 * @param stackElementFilters a list of regex strings (see {@link String#matches(String)}). If any of these match the file/method name in a stack element of a failure/error, that stack element
	 * will be ignored. 
	 */
	public JSUnitRhinoTestListenerHandler(TestResult result, List<Test> testList, boolean useFileInStackQualifiedName, String[] stackElementFilters)
	{
		super(result, testList, stackElementFilters);
		this.useFileInStackQualifiedName = useFileInStackQualifiedName;
	}

	// JS parameters (Test, Error)
	@Override
	public void addError(final Object test, final Object throwable)
	{
		wrapInContext(new Runnable()
		{
			@Override
			public void run()
			{
				JSUnitRhinoTestListenerHandler.super.addError(test, throwable);
			}
		});
	}

	// JS parameters (Test, AssertionFailedError)
	@Override
	public void addFailure(final Object test, final Object assertionfailederror)
	{
		wrapInContext(new Runnable()
		{
			@Override
			public void run()
			{
				JSUnitRhinoTestListenerHandler.super.addFailure(test, assertionfailederror);
			}
		});
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
			stackTrace = getRhinoExceptionStackTrace((RhinoException)stackObj);
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

	private StackTraceElement[] getRhinoExceptionStackTrace(RhinoException re)
	{
		StackTraceElement[] stackTrace;
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
				boolean isMethod = ((!isFile) && (line.contains("org.mozilla.javascript.gen.") || line.contains(" script.")));
				if (fileOrMethodName != null && lineNumber > -1 && (isFile || isMethod) &&
					!/* (ignoreAllButClientStackTraces && */isStackElementFilterMatch(fileOrMethodName)/* ) */) // this line can be uncommented to show stack inside testing code
				{
					// ignoreAllButClientStackTraces = false; // this line can be uncommented to show stack inside testing code
					if (useFileInStackQualifiedName)
					{
						filteredStack.add(new StackTraceElement(isFile ? getFileNameAsJavaType(fileOrMethodName) : "javascript", isMethod ? fileOrMethodName
							: "method", "file", lineNumber));
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
		return stackTrace;
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
	@Override
	public void endTest(final Object test)
	{
		wrapInContext(new Runnable()
		{
			@Override
			public void run()
			{
				JSUnitRhinoTestListenerHandler.super.endTest(test);
			}
		});
	}

	// JS parameters (Test)
	@Override
	public void startTest(final Object test)
	{
		wrapInContext(new Runnable()
		{
			@Override
			public void run()
			{
				JSUnitRhinoTestListenerHandler.super.startTest(test);
			}
		});
	}

	private void wrapInContext(Runnable r)
	{
		boolean contextEntered = false;
		if (!shouldStop())
		{
			Context.enter();
			contextEntered = true;
		}
		try
		{
			r.run();
		}
		finally
		{
			if (contextEntered) Context.exit();
		}
	}

	@Override
	protected String getTestObjectName(Object test)
	{
		String name = null;
		if (test instanceof Scriptable)
		{
			name = Context.getCurrentContext().evaluateString((Scriptable)test, "this.getName()", "Get test name", 1, null).toString();
		}
		return name;
	}

	@Override
	protected String getThrowableMsg(String testName, Object throwable)
	{
		String errorMsg = null;
		Context context = Context.getCurrentContext();
		JSUnitDebugger jsUnitDebugger = (JSUnitDebugger)context.getDebugger();
		if (jsUnitDebugger != null)
		{
			Object exception = jsUnitDebugger.getException(testName);
			if (exception instanceof Scriptable)
			{
				errorMsg = getMessage(context, (Scriptable)exception);
			}
			else if (exception instanceof RhinoException)
			{
				errorMsg = ((RhinoException)exception).getMessage();
			}
		}
		if (errorMsg == null && throwable instanceof Scriptable)
		{
			errorMsg = getMessage(context, (Scriptable)throwable);
		}
		return errorMsg;
	}

	@Override
	protected StackTraceElement[] getStackTrace(String testName, Object throwable)
	{
		StackTraceElement[] stackTrace = null;
		Context context = Context.getCurrentContext();
		JSUnitDebugger jsUnitDebugger = (JSUnitDebugger)context.getDebugger();
		if (jsUnitDebugger != null)
		{
			Object exception = jsUnitDebugger.getException(testName);
			if (exception instanceof Scriptable)
			{
				stackTrace = getStackTrace(context, (Scriptable)exception);
			}
			else if (exception instanceof RhinoException)
			{
				stackTrace = getRhinoExceptionStackTrace((RhinoException)exception);
			}
		}
		if ((stackTrace == null || stackTrace.length == 0) && throwable instanceof Scriptable)
		{
			stackTrace = getStackTrace(context, (Scriptable)throwable);
		}
		return stackTrace;
	}

	public void setJSResult(Scriptable jsResult)
	{
		this.jsResult = jsResult;
	}

	@Override
	public void applyShouldStop()
	{
		if (jsResult != null)
		{
			Context.getCurrentContext().evaluateString(jsResult, "this.stop()", "Stopped by user", 1, null);
			jsResult = null; // stopping it once is enough
		}
	}

}