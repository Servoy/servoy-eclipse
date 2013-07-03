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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.debug.Debugger;

import de.berlios.jsunit.JsUnitException;
import de.berlios.jsunit.JsUnitRuntimeException;


/**
 * Class that creates a bridge between JSUnit and Java. It loads needed javascript libraries, the javascript tests, then it can run the JSUnit tests and give a tree-like representation of the JSUnit test suite.
 * @author acostescu
 */
@SuppressWarnings("nls")
public class JSUnitToJavaRunner
{
	public final static String ASSERTION_EXCEPTION_MESSAGE = "just for stack";
	private final static String TEST_LISTENER_NAME = "javaTestListener";

	private Scriptable baseScope;
	private Scriptable testCodeScope;
	private final boolean useDebugMode;
	private static final String jsUnit;
	private static final String jsUtil;
	private static final String jsUnitToJava;
	private static String curentlyExecutingTest = null;

	protected static interface RhinoContextRunnable<T, X extends Exception>
	{
		T run(Context context) throws X;
	}

	protected abstract static class SilentRhinoContextRunnable implements RhinoContextRunnable<Void, RuntimeException>
	{
		public Void run(Context context) throws RuntimeException
		{
			runSilent(context);
			return null;
		}

		public abstract void runSilent(Context context);

	}

	public static String getCurentlyExecutingTest()
	{
		return curentlyExecutingTest;
	}

	public static void setCurentlyExecutingTest(String curentlyExecutingTest)
	{
		JSUnitToJavaRunner.curentlyExecutingTest = curentlyExecutingTest;
	}

	static
	{
		jsUtil = getScriptAsStringFromResource("this.JsUtilLoaded", JsUnitException.class, "/JsUtil.js");
		jsUnit = getScriptAsStringFromResource("this.TestCaseLoaded", JsUnitException.class, "/JsUnit.js");
		jsUnitToJava = getScriptAsStringFromResource("this.JsUnitToJavaLoaded", JSUnitToJavaRunner.class, "JsUnitToJava.js");
	}

	public static String getScriptAsStringFromResource(String repeatSafeguardId, Class< ? > locatorClass, String name)
	{
		StringWriter writer = new StringWriter(125 * 1024);
		writer.append("if (typeof(" + repeatSafeguardId + ") == 'undefined') {\n" + repeatSafeguardId + " = 1;\n");
		loadScriptFromResource(locatorClass, name, writer);
		writer.append("\n}");
		return writer.toString();
	}

	private static void loadScriptFromResource(Class< ? > locatorClass, final String name, final Writer writer)
	{
		final InputStream is = locatorClass.getResourceAsStream(name);
		if (is != null)
		{
			try
			{
				final Reader reader = new InputStreamReader(is, "ISO-8859-1");
				copy(reader, writer);
			}
			catch (final UnsupportedEncodingException e)
			{
				throw new InternalError("Missing standard character set ISO-8859-1");
			}
			catch (final IOException e)
			{
				throw new InternalError("Cannot load resource " + name);
			}
			finally
			{
				close(is);
			}
		}
		else
		{
			throw new InternalError("Cannot find resource " + name);
		}
	}

	public static class TestScope extends ScriptableObject
	{

		public TestScope(Scriptable parentScope)
		{
			super(parentScope, null);
		}

		@Override
		public String getClassName()
		{
			return getClass().getName();
		}

	}

	/**
	 * Creates a new JSUnit runner using the given scope. If scope is null a new scope will be created.
	 * 
	 * @param scope the scope to be used by this runner.
	 * @param createSeparateScopeForTestCode
	 */
	public JSUnitToJavaRunner(final Scriptable scope, final boolean createSeparateScopeForTestCode, boolean useDebugMode)
	{
		this.useDebugMode = useDebugMode;

		runInRhino(new SilentRhinoContextRunnable()
		{
			@Override
			public void runSilent(Context context)
			{
				if (scope == null)
				{
					JSUnitToJavaRunner.this.baseScope = context.initStandardObjects();
					testCodeScope = baseScope;
				}
				else
				{
					JSUnitToJavaRunner.this.baseScope = scope;
					if (createSeparateScopeForTestCode)
					{
						JSUnitToJavaRunner.this.testCodeScope = new TestScope(baseScope);
					}
				}

				context.evaluateString(JSUnitToJavaRunner.this.baseScope, jsUtil, "JsUtil.js", 0, null);
				context.evaluateString(JSUnitToJavaRunner.this.baseScope, jsUnit, "JsUnit.js", 0, null);
				context.evaluateString(JSUnitToJavaRunner.this.baseScope, jsUnitToJava, "JsUnitToJava.js", 0, null);
			}
		}, "Cannot evaluate JavaScript code of JsUnit");
	}

	/**
	 * Generates an array representing the test name tree (test suite/test hierarchy). If argument is null, returns an empty array.<br>
	 * <br>
	 * First element is the name of the given test suite. Next element is {@link #NEXT_CHILD_GROUP}. For each of the elements a list of child elements ended
	 * with {@link #NEXT_CHILD_GROUP} will follow.<br>
	 * <br>
	 * For example ["1" NEXT_CHILD_GROUP "2" "3" NEXT_CHILD_GROUP "4" NEXT_CHILD_GROUP "5" "6" NEXT_CHILD_GROUP NEXT_CHILD_GROUP NEXT_CHILD_GROUP
	 * NEXT_CHILD_GROUP] will stand for the tree:<br>
	 * 
	 * <pre>
	 *     &quot;1&quot;
	 *  |---+---|
	 * &quot;2&quot;     &quot;3&quot;
	 *  |     |-+-|
	 * &quot;4&quot;   &quot;5&quot; &quot;6&quot;
	 * </pre>
	 * 
	 * @param suiteName the name of the javaScript test suite who's structure will be inspected.
	 * @return an array representing the test tree as described above. It will only contain String objects and NEXT_CHILD_GROUP values.
	 * @throws JsUnitException when the javaScript that inspects the test suite structure fails for some reason.
	 */
	public List<String> getTestTree(String suiteName) throws JsUnitException
	{
		List<String> testTree = new ArrayList<String>();
		if (suiteName != null)
		{
			Object result = evaluateString("JsUnitToJava.prototype.getTestTree(" + suiteName + ".prototype.suite())", "Getting test tree");

			if (result instanceof NativeArray)
			{
				testTree = new ArrayList<String>();
				NativeArray nativeTestTree = (NativeArray)result;
				Object element;
				for (int i = 0; i < nativeTestTree.getLength(); i++)
				{
					element = nativeTestTree.get(i, testCodeScope);
					if (element instanceof CharSequence)
					{
						element = element.toString();
					}
					testTree.add((String)element);
				}
			}
		}
		return testTree;
	}

	/**
	 * Load additional code into the JavaScript context. The provided reader is read until execution and closed afterwards.
	 * 
	 * @param reader the reader providing the code
	 * @param name an identifying name of the code (normally the file name)
	 * @return the evaluated value
	 * @throws JsUnitException
	 * @throws IOException
	 */
	public Object evaluateReader(final Reader reader, String n) throws JsUnitException, IOException
	{
		if (reader == null)
		{
			throw new IllegalArgumentException("The reader is null");
		}
		if (n == null)
		{
			n = "anonymous";
		}
		final String name = n;
		try
		{
			return runInRhino(new RhinoContextRunnable<Object, IOException>()
			{
				@Override
				public Object run(Context context) throws IOException
				{
					return context.evaluateReader(testCodeScope, reader, name, 1, null);
				}
			}, "Cannot evaluate JavaScript code of " + name);
		}
		finally
		{
			close(reader);
		}
	}

	/**
	 * Evaluate the given JavaScript in the current context.
	 * 
	 * @param code the JavaScript
	 * @param name the name of the script (may be null)
	 * @return the evaluated value
	 * @throws JsUnitException if the code was not valid
	 * @throws IllegalArgumentException if <code>code</code>is <code>null</code>
	 */
	public Object evaluateString(final String code, String n) throws JsUnitException
	{
		if (code == null)
		{
			throw new IllegalArgumentException("The code is null");
		}
		if (n == null)
		{
			n = "anonymous";
		}
		final String name = n;
		return runInRhino(new RhinoContextRunnable<Object, RuntimeException>()
		{
			@Override
			public Object run(Context context)
			{
				return context.evaluateString(testCodeScope, code, name, 1, null);
			}
		}, "Cannot evaluate JavaScript code of " + name);
	}

	public void runSuite(final JSUnitTestListener testListener, final String suiteClassName)
	{
		runInRhino(new SilentRhinoContextRunnable()
		{
			@Override
			public void runSilent(Context context)
			{
				Object wrappedOut = Context.javaToJS(testListener, testCodeScope);
				ScriptableObject.putProperty(testCodeScope, TEST_LISTENER_NAME, wrappedOut);

				context.evaluateString(testCodeScope, "var result = new TestResult();\n" + TEST_LISTENER_NAME + ".setResult(result);\nresult.addListener(" +
					TEST_LISTENER_NAME + ");\n" + suiteClassName + ".prototype.suite().run(result)", "suiteName", 1, null);
			}
		}, "Cannot evaluate internal JavaScript code");
	}

	protected <T, X extends Exception> T runInRhino(RhinoContextRunnable<T, X> rhinoContextRunnable, String exceptionMessage) throws X
	{
		Context context = Context.enter();
		try
		{
			Debugger oldDebugger = context.getDebugger();
			if (useDebugMode && !(oldDebugger instanceof JSUnitDebugger))
			{
				context.setGeneratingDebug(true);
				context.setOptimizationLevel(-1);
				context.setDebugger(new JSUnitDebugger(oldDebugger), null);
			}
			try
			{
				return rhinoContextRunnable.run(context);
			}
			catch (final EcmaError e)
			{
				throw new JsUnitRuntimeException("JavaScript error running/preparing tests", e);
			}
			catch (final JavaScriptException e)
			{
				throw new JsUnitRuntimeException(exceptionMessage, e);
			}
			finally
			{
				if (useDebugMode) context.setDebugger(oldDebugger, null);
			}
		}
		finally
		{
			Context.exit();
		}
	}

	private static void close(final Writer writer)
	{
		try
		{
			writer.close();
		}
		catch (final IOException e)
		{
			// ignore
		}
	}

	private static void close(final Reader reader)
	{
		try
		{
			reader.close();
		}
		catch (final IOException e)
		{
			// ignore
		}
	}

	private static void close(final InputStream is)
	{
		try
		{
			is.close();
		}
		catch (final IOException e)
		{
			// ignore
		}
	}

	private static void copy(final Reader reader, final Writer writer) throws IOException
	{
		final Reader in = new BufferedReader(reader, 1024 * 16);
		int i;
		while ((i = in.read()) != -1)
		{
			writer.write(i);
		}
	}

//	/**
//	 * Runs the TestSuite &quot;AllTests&quot;. The result of the test is written in XML format into the given writer. Since the result is a complete XML
//	 * document, the writer is closed by the method (even in case of an exception).
//	 * 
//	 * @param writer the writer receiving the result
//	 * @throws IOException if writing to the <code>writer</code> fails
//	 * @throws IllegalArgumentException if <code>writer</code>is <code>null</code>
//	 * @throws JsUnitRuntimeException if the JavaScript code of the method itself fails
//	 * @since upcoming
//	 */
//	public void runAllTests(final Writer writer) throws IOException
//	{
//		if (writer == null)
//		{
//			throw new IllegalArgumentException("The writer is null");
//		}
//		context = Context.enter(context);
//		try
//		{
//			try
//			{
//				final String xml = context.evaluateString(
//					baseScope,
//					"" + "var stringWriter = new StringWriter();\n" + "var runner = new EmbeddedTextTestRunner(new XMLResultPrinter(stringWriter));\n"
//						+ "var collector = new AllTestsCollector(this);\n" + "runner.run(collector.collectTests());\n" + "stringWriter.get();\n", "AllTests",
//					1, null).toString();
//				writer.write(xml);
//			}
//			catch (final EcmaError e)
//			{
//				throw new JsUnitRuntimeException("JavaScript error running tests", e);
//			}
//			catch (final JavaScriptException e)
//			{
//				throw new JsUnitRuntimeException("Cannot evaluate internal JavaScript code", e);
//			}
//		}
//		finally
//		{
//			Context.exit();
//			close(writer);
//		}
//	}
//
//	/**
//	 * Runs all JavaScript TestSuites in the context. The method will collect any JavaScript <code>TestSuite</code> in the context in a collecting
//	 * <code>TestSuite</code> and run it. The result of the test is written in XML format into the given writer. Since the result is a complete XML document,
//	 * the writer is closed by the method (even in case of an exception).
//	 * 
//	 * @param writer the writer receiving the result
//	 * @param name the name of the collecting <code>TestSuite</code> (may be null)
//	 * @throws IOException if writing to the <code>writer</code> fails
//	 * @throws IllegalArgumentException if <code>writer</code>is <code>null</code>
//	 * @throws JsUnitRuntimeException if the JavaScript code of the method itself fails
//	 * @since upcoming
//	 */
//	public void runTestSuites(final Writer writer, String name) throws IOException
//	{
//		if (writer == null)
//		{
//			throw new IllegalArgumentException("The writer is null");
//		}
//		name = name == null ? "AllTestSuites" : name;
//		context = Context.enter(context);
//		try
//		{
//			try
//			{
//				final String xml = context.evaluateString(
//					baseScope,
//					"" + "var stringWriter = new StringWriter();\n" + "var runner = new EmbeddedTextTestRunner(new XMLResultPrinter(stringWriter));\n" +
//						"var collector = new TestSuiteCollector(this);\n" + "runner.run(collector.collectTests(), \"" + name + "\");\n" +
//						"stringWriter.get();\n", name, 1, null).toString();
//				writer.write(xml);
//			}
//			catch (final EcmaError e)
//			{
//				throw new JsUnitRuntimeException("JavaScript error running tests", e);
//			}
//			catch (final JavaScriptException e)
//			{
//				throw new JsUnitRuntimeException("Cannot evaluate internal JavaScript code", e);
//			}
//		}
//		finally
//		{
//			Context.exit();
//			close(writer);
//		}
//	}
//
//	/**
//	 * Runs all JavaScript TestCases in the context. The method will collect any JavaScript <code>TestCase</code> in the context in a collecting
//	 * <code>TestSuite</code> and run it. The result of the test is written in XML format into the given writer. Since the result is a complete XML document,
//	 * the writer is closed by the method (even in case of an exception).
//	 * 
//	 * @param writer the writer receiving the result
//	 * @param name the name of the collecting <code>TestSuite</code> (may be null)
//	 * @throws IOException if writing to the <code>writer</code> fails
//	 * @throws IllegalArgumentException if <code>writer</code>is <code>null</code>
//	 * @throws JsUnitRuntimeException if the JavaScript code of the method itself fails
//	 * @since upcoming
//	 */
//	public void runTestCases(final Writer writer, String name) throws IOException
//	{
//		if (writer == null)
//		{
//			throw new IllegalArgumentException("The writer is null");
//		}
//		name = name == null ? "AllTestCases" : name;
//		context = Context.enter(context);
//		try
//		{
//			try
//			{
//				final String xml = context.evaluateString(
//					baseScope,
//					"" + "var stringWriter = new StringWriter();\n" + "var runner = new EmbeddedTextTestRunner(new ResultPrinter(stringWriter));\n" +
//						"var collector = new TestCaseCollector(this);\n" + "runner.run(collector.collectTests(), \"" + name + "\");\n" +
//						"stringWriter.get();\n", name, 1, null).toString();
//				writer.write(xml);
//			}
//			catch (final EcmaError e)
//			{
//				throw new JsUnitRuntimeException("JavaScript error running tests", e);
//			}
//			catch (final JavaScriptException e)
//			{
//				throw new JsUnitRuntimeException("Cannot evaluate internal JavaScript code", e);
//			}
//		}
//		finally
//		{
//			Context.exit();
//			close(writer);
//		}
//	}

}