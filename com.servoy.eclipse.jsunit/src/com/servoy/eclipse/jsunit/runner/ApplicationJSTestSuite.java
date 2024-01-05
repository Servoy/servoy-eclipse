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

import java.io.StringReader;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.servoy.eclipse.model.test.SolutionJSUnitSuiteCodeBuilder;
import com.servoy.eclipse.model.test.TestTarget;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.ScriptEngine;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

import junit.framework.Test;
import junit.framework.TestResult;

/**
 * This class generates a javascript test suite from an existing initialized Servoy application, with a loaded Servoy solution.
 * @author acostescu
 */
public class ApplicationJSTestSuite extends JSUnitSuite
{
	public static final String SOLUTION_TEST_JS_NAME = "solutionTestSuite.js";

	protected static IApplication staticSuiteApplication;
	protected static TestTarget staticTarget;

	private String jsTestCode;

	/**
	 * Creates a new application test Suite.
	 * @param target
	 *
	 * @param app the application that will be used to create a JSUnit test suite. The application must have a loaded solution in order for the tests to be
	 *            performed.
	 */
	public ApplicationJSTestSuite(IApplication application, TestTarget target,
		boolean spamInTestNamesAsFullTreePathsForDumbToolsThatAreUnAwareOfTestSuiteHierarchy)
	{
		this();
		setUseFileForJavaQualifiedNameInStack(true);
		init(application, target, spamInTestNamesAsFullTreePathsForDumbToolsThatAreUnAwareOfTestSuiteHierarchy);
	}

	/**
	 * Constructs a un-initialized instance. You <b>MUST</b> call <code>init()</code> before using this suite.
	 */
	protected ApplicationJSTestSuite()
	{
		// make sure that the solution scripts are compiled in interpreted mode by default
		if (System.getProperty(ScriptEngine.SERVOY_DISABLE_SCRIPT_COMPILE_PROPERTY) == null)
		{
			System.setProperty(ScriptEngine.SERVOY_DISABLE_SCRIPT_COMPILE_PROPERTY, "true");
		}

		setStackElementFilters(new String[] { "\\A" + SOLUTION_TEST_JS_NAME + "\\z" });
	}

	protected void initWithError(String errorMessage)
	{
		Context.enter();
		try
		{
			SolutionJSUnitSuiteCodeBuilder suiteBuilder = new SolutionJSUnitSuiteCodeBuilder();
			suiteBuilder.initializeWithError(errorMessage);
			jsTestCode = suiteBuilder.getCode();
			super.init(new StringReader(suiteBuilder.getCode()), suiteBuilder.getRootTestClassName(), SOLUTION_TEST_JS_NAME, null, false, isDebugModeOn());
		}
		finally
		{
			Context.exit();
		}
	}

	protected void init(IApplication application, TestTarget target, boolean spamInTestNamesAsFullTreePathsForDumbToolsThatAreUnAwareOfTestSuiteHierarchy)
	{
		if (application == null)
		{
			initWithError("Cannot find the test client");
		}
		else
		{
			try
			{
				application.getRuntimeProperties().put("JSUnit", Boolean.TRUE);
				SolutionJSUnitSuiteCodeBuilder suiteBuilder = new SolutionJSUnitSuiteCodeBuilder();
				suiteBuilder.initializeWithSolution(application.getSolution(), application.getFlattenedSolution(), target,
					spamInTestNamesAsFullTreePathsForDumbToolsThatAreUnAwareOfTestSuiteHierarchy);
				Scriptable scope = initScope(application);
				jsTestCode = suiteBuilder.getCode();
				super.init(new StringReader(jsTestCode), suiteBuilder.getRootTestClassName(), SOLUTION_TEST_JS_NAME, scope, scope != null, isDebugModeOn());
			}
			catch (NoClassDefFoundError e)
			{
				// show nice message when SolutionJSUnitSuiteCodeBuilder is not found - cause classpath needs to be updated since 7.3
				String msg = "Please include latest 'com.servoy.eclipse.model*.jar' in classpath (required since 7.3).";
				Debug.log(msg, e);
				System.err.println(msg);
				throw e;
			}
			catch (Exception ex)
			{
				Debug.error(ex);
				throw ex;
			}
		}
	}

	private boolean isDebugModeOn()
	{
		return Utils.getAsBoolean(System.getProperty(ScriptEngine.SERVOY_DISABLE_SCRIPT_COMPILE_PROPERTY)); // prop. can't be null here, default would be set by constructor
	}

	/**
	 * Call this only if you called <code>init(IApplication application)</code> in the past, but scope has changed.
	 */
	protected void reinitializeTestScope(IApplication application)
	{
		super.changeScope(initScope(application), new StringReader(jsTestCode));
	}

	private Scriptable initScope(IApplication application)
	{
		Scriptable scope = getTestScope(application);
		// define the jsUnit property in scope so it can be used within solution methods with jsUnit.Assert...
		if (scope != null)
		{
			// set it here (to null even) to be sure it will be referenced in the right scope
			// in some cases it can already be set (unhandled errors when loading solution or first form) in which case we don't want to clear it
			Object tmp = scope.get(IExecutingEnviroment.TOPLEVEL_JSUNIT, scope);
			if (tmp == null || Scriptable.NOT_FOUND == tmp || Context.getUndefinedValue().equals(tmp))
			{
				scope.put(IExecutingEnviroment.TOPLEVEL_JSUNIT, scope, null);
			}
		}
		return scope;
	}

	public static Test suite()
	{
		IServiceProvider prevServiceProvider = J2DBGlobals.setSingletonServiceProvider(staticSuiteApplication);
		try
		{
			return new ApplicationJSTestSuite(staticSuiteApplication, staticTarget, false);
		}
		finally
		{
			J2DBGlobals.setSingletonServiceProvider(prevServiceProvider);
		}
	}

	/**
	 * Sets the application that will be used to create a JSUnit test suite. The application must have a loaded solution in order for the tests to be performed.
	 *
	 * @param app the application that has the solution to be tested already loaded.
	 * @param target specifies what test sub-tree should be run from the solution loaded in app.
	 */
	public static void setTestTarget(IApplication app, TestTarget target)
	{
		staticSuiteApplication = app;
		staticTarget = target;
	}

	private Scriptable getTestScope(IApplication application)
	{
		if (application == null || application.getSolution() == null)
		{
			return null;
		}
		return application.getScriptEngine().getSolutionScope();
	}


	@Override
	public void run(final TestResult result)
	{
		staticSuiteApplication.invokeAndWait(() -> {
			super.run(result);
		});
	}

}