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

import de.berlios.jsunit.JsUnitException;
import de.berlios.jsunit.JsUnitRuntimeException;


/**
 * Class that creates a bridge between JSUnit and Java. It loads needed javascript libraries, the javascript tests, then it can run the JSUnit tests and give a tree-like representation of the JSUnit test suite.
 * @author acostescu
 */
@SuppressWarnings("nls")
public class JSUnitToJavaRunner
{

	public final static Object NEXT_CHILD_GROUP = null;
	public final static String ASSERTION_EXCEPTION_MESSAGE = "just for stack";
	private final static String testListenerName = "javaTestListener";

	private Scriptable baseScope;
	private Scriptable testCodeScope;
	private static final String jsUnit;
	private static final String jsUtil;
	private static final String jsUnitToJava;
	static
	{
		StringWriter writer = new StringWriter(125 * 1024);
		writer.append("if (!this.JsUtil) {\n");
		loadScriptFromResource(JsUnitException.class, "/JsUtil.js", writer);
		writer.append("\n}");
		jsUtil = writer.toString();

		writer = new StringWriter(125 * 1024);
		writer.append("if (!this.TestCase) {\n");
		loadScriptFromResource(JsUnitException.class, "/JsUnit.js", writer);
		writer.append("\n}");
		jsUnit = writer.toString();

		writer = new StringWriter(10 * 1024);
		writer.append("if (!this.JsUnitToJava) {\n");
		loadScriptFromResource(JSUnitToJavaRunner.class, "JsUnitToJava.js", writer);
		writer.append("\n}");
		jsUnitToJava = writer.toString();
	}

	private static void loadScriptFromResource(Class locatorClass, final String name, final Writer writer)
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

	private class TestScope extends ScriptableObject
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
	public JSUnitToJavaRunner(Scriptable scope, boolean createSeparateScopeForTestCode)
	{
		Context context = Context.enter();
		if (scope == null)
		{
			this.baseScope = context.initStandardObjects();
			testCodeScope = baseScope;
		}
		else
		{
			this.baseScope = scope;
			if (createSeparateScopeForTestCode)
			{
				this.testCodeScope = new TestScope(baseScope);
			}
		}
		try
		{
			context.evaluateString(this.baseScope, jsUtil, "JsUtil.js", 0, null);
			context.evaluateString(this.baseScope, jsUnit, "JsUnit.js", 0, null);
			context.evaluateString(this.baseScope, jsUnitToJava, "JsUnitToJava.js", 0, null);
		}
		catch (final JavaScriptException e)
		{
			throw new JsUnitRuntimeException("Cannot evaluate JavaScript code of JsUnit", e);
		}
		finally
		{
			Context.exit();
		}
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
	public List<Object> getTestTree(String suiteName) throws JsUnitException
	{
		List<Object> testTree = new ArrayList<Object>();
		if (suiteName != null)
		{
			Object result = evaluateString("JsUnitToJava.prototype.getTestTree(" + suiteName + ".prototype.suite())", "Getting test tree");

			if (result instanceof NativeArray)
			{
				testTree = new ArrayList<Object>();
				NativeArray nativeTestTree = (NativeArray)result;
				Object element;
				for (int i = 0; i < nativeTestTree.getLength(); i++)
				{
					element = nativeTestTree.get(i, testCodeScope);
					if (element instanceof CharSequence)
					{
						element = element.toString();
					}
					testTree.add(element);
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
	public Object evaluateReader(final Reader reader, String name) throws JsUnitException, IOException
	{
		if (reader == null)
		{
			throw new IllegalArgumentException("The reader is null");
		}
		if (name == null)
		{
			name = "anonymous";
		}
		Context context = Context.enter();
		try
		{
			return context.evaluateReader(testCodeScope, reader, name, 1, null);
		}
		catch (final JavaScriptException e)
		{
			throw new JsUnitException("Cannot evaluate JavaScript code of " + name, e);
		}
		finally
		{
			close(reader);
			Context.exit();
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
	public Object evaluateString(final String code, String name) throws JsUnitException
	{
		if (code == null)
		{
			throw new IllegalArgumentException("The code is null");
		}
		if (name == null)
		{
			name = "anonymous";
		}
		Context context = Context.enter();
		try
		{
			return context.evaluateString(testCodeScope, code, name, 1, null);
		}
		catch (final JavaScriptException e)
		{
			throw new JsUnitException("Cannot evaluate JavaScript code of " + name, e);
		}
		finally
		{
			Context.exit();
		}
	}

	public void runSuite(JSUnitTestListener testListener, String suiteClassName)
	{
		Context context = Context.enter();

		Object wrappedOut = Context.javaToJS(testListener, testCodeScope);
		ScriptableObject.putProperty(testCodeScope, testListenerName, wrappedOut);

		try
		{
			try
			{
				context.evaluateString(testCodeScope, "var result = new TestResult();\nresult.addListener(" + testListenerName + ");\n" + suiteClassName +
					".prototype.suite().run(result)", "suiteName", 1, null);
			}
			catch (final EcmaError e)
			{
				throw new JsUnitRuntimeException("JavaScript error running tests", e);
			}
			catch (final JavaScriptException e)
			{
				throw new JsUnitRuntimeException("Cannot evaluate internal JavaScript code", e);
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