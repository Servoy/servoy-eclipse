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

package com.servoy.eclipse.model.test;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.servoy.j2db.ClientState;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectReference;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.SolutionScope;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * Given a solution and it's corresponding flattened solution, this class is able
 * to build the js test suite structure & code.
 * @author acostescu
 */
public class SolutionJSUnitSuiteCodeBuilder
{

	private static final String INVALID_APP_SUITE = "CannotRunJSUnitTests";
	public static final String TEST_METHOD_PREFIX = "test";
	public static final String SET_UP_METHOD = "setUp";
	public static final String TEAR_DOWN_METHOD = "tearDown";

	// if this is set after a solution test method exits, it will fail that test (so at the end); it can be set by anyone that needs such behavior.
	// when set it must be an error object; it will get thrown using JS "throw e;" it would be nice if it's a JSUnitError
	// (+ if a jsunit.failAfterTest object is detected before any test started to run - so before a jsunit value is even set - it will be considered
	// the same - an error that happened even before tests started to run - and will be reported as such, failing the first test)
	public static final String FAIL_AFTER_CURRENT_TEST_KEY = "failAfterTest";

	protected boolean initialized = false;
	protected String code;
	protected String rootTestClassName;

	/**
	 * Returns the js unit suite javascript code that was constructed from initialization.
	 * @return the js unit suite javascript code that was constructed from initialization.
	 * @throws IllegalStateException if the builder was is not yet initialized.
	 */
	public String getCode()
	{
		return code;
	}

	/**
	 * Returns the root js unit suite suite name that can be used for starting the tests.
	 * @return the root js unit suite suite name that can be used for starting the tests.
	 * @throws IllegalStateException is the builder is not yet initialized.
	 */
	public String getRootTestClassName()
	{
		return rootTestClassName;
	}

	public void initializeWithSolution(Solution solution, FlattenedSolution flattenedSolution, TestTarget target)
	{
		if (solution == null || flattenedSolution == null)
		{
			initializeWithError("No solution loaded in test client.");
			return;
		}
		// build solution test suite code; the structure will look like this:
		//
		// Solution suite
		//   globals test case suite
		//   forms.form1Name test case suite
		//   ...
		//   forms.formNName test case suite
		//   Modules suite
		//     Module1Name solution suite
		//       ... same as Solution suite structure
		//     ModuleNName solution suite
		//       ... same as Solution suite structure
		StringBuffer testCode = new StringBuffer(1024);
		HashSet<Solution> inspectedModules = new HashSet<Solution>();
		TestIdentifier testIdentifier = appendSolutionTestCode(solution, target, testCode, inspectedModules, flattenedSolution,
			//* If Test target is null that means the whole active solution.
			//* If Test target's module to test is the same as active solution that means the whole active solution again.
			target == null || target.getActiveSolution().getName().equals(target.getModuleToTest() == null ? "" : target.getModuleToTest().getName()));

		if (testIdentifier == null)
		{
			initializeWithError(
				"Th" + (target == null ? "is solution" : "e selection") + " does not have jsunit tests" + (target == null ? ": " + solution.getName() : "."));
		}
		else
		{
			initialized = true;
			this.code = testCode.toString();
			this.rootTestClassName = testIdentifier.getTestClassName();
		}
	}

	private TestIdentifier appendSolutionTestCode(Solution solution, TestTarget target, StringBuffer testCode, HashSet<Solution> inspectedModules,
		FlattenedSolution flattenedSolution, boolean partOfTargetModuleSubtree)
	{
		TestIdentifier resultingSuiteId = null;
		if (solution != null && !inspectedModules.contains(solution))
		{
			String moduleToTestName = target == null ? "" : (target.getModuleToTest() == null ? "" : target.getModuleToTest().getName());
			boolean thisSolutionShouldBeTested = (partOfTargetModuleSubtree || target.getModuleToTest() == null || moduleToTestName.equals(solution.getName()));
			boolean addedTestCode = false;
			inspectedModules.add(solution);

			TestIdentifier modulesTestSuiteId = null;
			// inspect direct child modules and generate test code for them
			try
			{
				List<RootObjectReference> modules = solution.getReferencedModules(null);
				List<TestIdentifier> modulesThatAddedTests = new ArrayList<TestIdentifier>();
				for (RootObjectReference ref : modules)
				{
					if (ref.getMetaData() != null)
					{
						Solution module = (Solution)solution.getRepository().getActiveRootObject(ref.getMetaData().getRootObjectId());
						TestIdentifier tmp = appendSolutionTestCode(module, target, testCode, inspectedModules, flattenedSolution,
							(partOfTargetModuleSubtree || module.getName().equals(moduleToTestName)));
						if (tmp != null)
						{
							modulesThatAddedTests.add(tmp);
							addedTestCode = true;
						}
					}
				}
				// if some modules have created test code, we must bundle the modules in a "Modules" suite
				if (addedTestCode)
				{
					modulesTestSuiteId = addModuleSuite(modulesThatAddedTests, testCode);
				}
			}
			catch (RepositoryException e)
			{
				Debug.log(e);
			}
			catch (RemoteException e)
			{
				Debug.log(e);
			}

			TestIdentifier globalSuiteId = null;
			List<TestIdentifier> formTestIds = new ArrayList<TestIdentifier>();
			if (thisSolutionShouldBeTested)
			{
				// really create the solution test code now that module test code has been created
				// first globals/form testcases
				if (target == null || target.getFormToTest() == null) globalSuiteId = addGlobalTests(solution, target, testCode);
				if (target == null || target.getGlobalScopeToTest() == null) formTestIds.addAll(addAllFormTests(solution, target, flattenedSolution, testCode));
				addedTestCode = addedTestCode || (formTestIds.size() > 0) || globalSuiteId != null;
			}

			// second create the solution suite
			if (addedTestCode)
			{
				resultingSuiteId = addSolutionSuite(solution, globalSuiteId, formTestIds, modulesTestSuiteId, testCode);
			}
		} // else this module was already inspected before - either loop in module hierarchy or the module is a child module of 2 different parent modules
		return resultingSuiteId;
	}

	private TestIdentifier addGlobalTests(Solution solution, TestTarget target, StringBuffer testCode)
	{
		// create scope test suites
		List<TestIdentifier> allGlobalIdentifiers = new ArrayList<TestIdentifier>();
		for (String scopeName : solution.getScopeNames())
		{
			if (target == null || target.getGlobalScopeToTest() == null ||
				(target.getGlobalScopeToTest().getRight().equals(scopeName) && solution.getName().equals(target.getGlobalScopeToTest().getLeft().getName())))
			{
				Iterator<ScriptMethod> it = solution.getScriptMethods(scopeName, true);
				// prefix the name so that we have no name conflicts with other form/module/global tests
				List<ScriptMethod> list = Utils.asList(it);
				Collections.reverse(list);
				TestIdentifier tmp = addTestCaseIfNecessary(list.iterator(), target, scopeName, "scopes", testCode);
				if (tmp != null) allGlobalIdentifiers.add(tmp);
			}
		}

		// add "Scopes" suite
		TestIdentifier suiteId = null;
		if (allGlobalIdentifiers.size() > 0)
		{
			suiteId = new TestIdentifier("Scope tests");
			testCode.append("function ");
			testCode.append(suiteId.getTestClassName());
			testCode.append("() {\n\tTestSuite.call(this, null);\n\tthis.setName(\"");
			testCode.append(suiteId.getTestName());
			testCode.append("\");\n\tvar ts;\n");
			for (TestIdentifier testCasesId : allGlobalIdentifiers)
			{
				testCode.append("\tts = new TestSuite(");
				testCode.append(testCasesId.getTestClassName());
				testCode.append(");\n\tts.setName(\"");
				testCode.append(testCasesId.getTestName());
				testCode.append("\");\n\tthis.addTest(ts);\n");
			}
			testCode.append("}\n");
			testCode.append(suiteId.getTestClassName());
			testCode.append(".prototype = new TestSuite();\n");
			testCode.append(suiteId.getTestClassName());
			testCode.append(".prototype.suite = function () { return new ");
			testCode.append(suiteId.getTestClassName());
			testCode.append("(); }\n\n");
		}

		return suiteId;
	}

	private List<TestIdentifier> addAllFormTests(Solution solution, TestTarget target, FlattenedSolution flattenedSolution, StringBuffer testCode)
	{
		List<TestIdentifier> allFormTestNames = new ArrayList<TestIdentifier>();
		Iterator<Form> it = solution.getForms(null, true);
		while (it.hasNext())
		{
			Form form = it.next();
			if (target == null || target.getFormToTest() == null || target.getFormToTest().getName().equals(form.getName()))
			{
				TestIdentifier formTestIdentifier = addFormTests(flattenedSolution.getFlattenedForm(form), target, testCode);
				if (formTestIdentifier != null)
				{
					allFormTestNames.add(formTestIdentifier);
				}
			}
		}
		return allFormTestNames;
	}

	private TestIdentifier addFormTests(Form form, TestTarget target, StringBuffer testCode)
	{
		Iterator<ScriptMethod> it = form.getScriptMethods(true);
		// prefix the name so that we have no name conflicts with other form/module/global tests
		List<ScriptMethod> list = Utils.asList(it);
		Collections.reverse(list);
		return addTestCaseIfNecessary(list.iterator(), target, "Form '" + form.getName() + "' tests", "forms." + form.getName(), testCode);
	}

	private TestIdentifier addTestCaseIfNecessary(Iterator<ScriptMethod> it, TestTarget target, String nameOfTest, String callPrefix, StringBuffer testCode)
	{
		TestIdentifier testIdentifier = null;
		boolean testMethodsFound = false;
		StringBuffer tmp = new StringBuffer();
		while (it.hasNext())
		{
			ScriptMethod method = it.next();
			if (method.getName().equals(SET_UP_METHOD) || method.getName().equals(TEAR_DOWN_METHOD) ||
				((target == null || target.getTestMethodToTest() == null || target.getTestMethodToTest().getID() == method.getID()) &&
					method.getName().startsWith(TEST_METHOD_PREFIX)))
			{
				if (!testMethodsFound && method.getName().startsWith(TEST_METHOD_PREFIX))
				{
					testMethodsFound = true;
					testCode.append(tmp);
					tmp = testCode;
				}

				if (testIdentifier == null)
				{
					// create globals TestCase class
					testIdentifier = new TestIdentifier(nameOfTest);
					tmp.append("function ");
					tmp.append(testIdentifier.getTestClassName());
					tmp.append("(name) { TestCase.call(this, name); }\n");
				}
				tmp.append("function ");
				tmp.append(testIdentifier.getTestClassName());
				tmp.append("_");
				tmp.append(method.getName());

				// if something bad happened even before tests got to execute, error out (for example if during solution open or first form load, an unhandled (so not handled by solution onError) global error was detected)
				tmp.append("() { if (typeof ");
				tmp.append(IExecutingEnviroment.TOPLEVEL_JSUNIT);
				tmp.append(" != 'undefined' && ");
				tmp.append(IExecutingEnviroment.TOPLEVEL_JSUNIT);
				tmp.append(" != null && typeof ");
				tmp.append(IExecutingEnviroment.TOPLEVEL_JSUNIT);
				tmp.append(".");
				tmp.append(SolutionJSUnitSuiteCodeBuilder.FAIL_AFTER_CURRENT_TEST_KEY);
				tmp.append(" != 'undefined' && ");
				tmp.append(IExecutingEnviroment.TOPLEVEL_JSUNIT);
				tmp.append(".");
				tmp.append(SolutionJSUnitSuiteCodeBuilder.FAIL_AFTER_CURRENT_TEST_KEY);
				tmp.append(" != null) throw ");
				tmp.append(IExecutingEnviroment.TOPLEVEL_JSUNIT);
				tmp.append(".");
				tmp.append(SolutionJSUnitSuiteCodeBuilder.FAIL_AFTER_CURRENT_TEST_KEY);
				tmp.append("; ");

				// else continue; prepare for testing and execute test method
				tmp.append(IExecutingEnviroment.TOPLEVEL_JSUNIT);
				tmp.append(" = this; ");
				tmp.append(callPrefix);
				tmp.append(".");
				if (method.getParent() instanceof Solution)
				{
					tmp.append(method.getScopeName()).append('.');
				}
				tmp.append(method.getName());
				tmp.append("(); ");
				tmp.append(IExecutingEnviroment.TOPLEVEL_JSUNIT);

				// if someone wants this test to fail after it finished running do so here (for example if during current test, an unhandled (so not handled by solution onError) global error was detected)
				tmp.append(" = null; if (this.").append(SolutionJSUnitSuiteCodeBuilder.FAIL_AFTER_CURRENT_TEST_KEY).append(") { var te = this.").append(
					SolutionJSUnitSuiteCodeBuilder.FAIL_AFTER_CURRENT_TEST_KEY).append("; this.").append(
						SolutionJSUnitSuiteCodeBuilder.FAIL_AFTER_CURRENT_TEST_KEY).append(" = null; throw te; } }\n");
			}
		}
		if (testMethodsFound && testIdentifier != null)
		{
			testCode.append(testIdentifier.getTestClassName());
			testCode.append(".prototype = new TestCase();\n");
			testCode.append(testIdentifier.getTestClassName());
			testCode.append(".glue(this);\n\n");
		}
		return testMethodsFound ? testIdentifier : null;
	}

	private TestIdentifier addModuleSuite(List<TestIdentifier> modulesThatAddedTests, StringBuffer testCode)
	{
		TestIdentifier suiteId = new TestIdentifier("Module tests");
		testCode.append("function ");
		testCode.append(suiteId.getTestClassName());
		testCode.append("() {\n\tTestSuite.call(this, null);\n\tthis.setName(\"");
		testCode.append(suiteId.getTestName());
		testCode.append("\");\n");
		for (TestIdentifier moduleSuiteId : modulesThatAddedTests)
		{
			testCode.append("\tthis.addTest(");
			testCode.append(moduleSuiteId.getTestClassName());
			testCode.append(".prototype.suite());\n");
		}
		testCode.append("}\n");
		testCode.append(suiteId.getTestClassName());
		testCode.append(".prototype = new TestSuite();\n");
		testCode.append(suiteId.getTestClassName());
		testCode.append(".prototype.suite = function () { return new ");
		testCode.append(suiteId.getTestClassName());
		testCode.append("(); }\n\n");
		return suiteId;
	}

	private TestIdentifier addSolutionSuite(Solution solution, TestIdentifier globalSuiteId, List<TestIdentifier> formTestCaseIds,
		TestIdentifier modulesTestSuiteId, StringBuffer testCode)
	{
		TestIdentifier suiteId = new TestIdentifier("Solution '" + solution.getName() + "' tests");
		testCode.append("function ");
		testCode.append(suiteId.getTestClassName());
		testCode.append("() {\n\tTestSuite.call(this, null);\n\tthis.setName(\"");
		testCode.append(suiteId.getTestName());
		testCode.append("\");\n");
		if (globalSuiteId != null)
		{
			testCode.append("\tthis.addTest(");
			testCode.append(globalSuiteId.getTestClassName());
			testCode.append(".prototype.suite());\n");
		}

		if (formTestCaseIds.size() > 0)
		{
			testCode.append("\tvar ts;\n");
			for (TestIdentifier testCasesId : formTestCaseIds)
			{
				testCode.append("\tts = new TestSuite(");
				testCode.append(testCasesId.getTestClassName());
				testCode.append(");\n\tts.setName(\"");
				testCode.append(testCasesId.getTestName());
				testCode.append("\");\n\tthis.addTest(ts);\n");
			}
		}

		if (modulesTestSuiteId != null)
		{
			testCode.append("\tthis.addTest(");
			testCode.append(modulesTestSuiteId.getTestClassName());
			testCode.append(".prototype.suite());\n");
		}
		testCode.append("}\n");
		testCode.append(suiteId.getTestClassName());
		testCode.append(".prototype = new TestSuite();\n");
		testCode.append(suiteId.getTestClassName());
		testCode.append(".prototype.suite = function () { return new ");
		testCode.append(suiteId.getTestClassName());
		testCode.append("(); }\n\n");
		return suiteId;
	}

	public void initializeWithError(String msg)
	{
		StringBuffer error = new StringBuffer(300);
		error.append("function ");
		error.append(INVALID_APP_SUITE);
		error.append("(name) { TestCase.call(this, name); }\nfunction ");
		error.append(INVALID_APP_SUITE);
		error.append("_testSystemInitFailed() { this.fail(\"");

		error.append(msg);

		error.append("\"); }\n");
		error.append(INVALID_APP_SUITE);
		error.append(".prototype = new TestCase();\n");
		error.append(INVALID_APP_SUITE);
		error.append(".glue(this);\n");

		error.append(INVALID_APP_SUITE);
		error.append(".prototype.suite = function () { return new TestSuite(");
		error.append(INVALID_APP_SUITE);
		error.append("); }\n");

		initialized = true;
		this.code = error.toString();
		this.rootTestClassName = INVALID_APP_SUITE;
	}

	public static void failAfterCurrentTestWithError(SolutionScope solutionScope, String message, Object error)
	{
		Context context = Context.enter();
		try
		{
			Object jsuAO = solutionScope.get(IExecutingEnviroment.TOPLEVEL_JSUNIT, solutionScope); // this is the actual jsunit test object; so it must be a scriptable
			Scriptable jsunitAssertObject;
			if (jsuAO == null || Scriptable.NOT_FOUND == jsuAO || Context.getUndefinedValue().equals(jsuAO))
			{
				jsunitAssertObject = new NativeObject();
				ScriptableObject.putProperty(solutionScope, IExecutingEnviroment.TOPLEVEL_JSUNIT, jsunitAssertObject);
			}
			else
			{
				jsunitAssertObject = (Scriptable)jsuAO;
			}
			Object failAfterTest = jsunitAssertObject.get(SolutionJSUnitSuiteCodeBuilder.FAIL_AFTER_CURRENT_TEST_KEY, jsunitAssertObject); // can be null/undefined or an error object

			if (failAfterTest == null || Scriptable.NOT_FOUND == failAfterTest || Context.getUndefinedValue().equals(failAfterTest))
			{
				Object scriptException;
				if (error instanceof Exception && (scriptException = ClientState.getScriptException((Exception)error)) != null)
				{
					ScriptableObject.putProperty(jsunitAssertObject, SolutionJSUnitSuiteCodeBuilder.FAIL_AFTER_CURRENT_TEST_KEY,
						Context.javaToJS(scriptException, solutionScope));
				}
				else
				{
					String adjustedMessage = ((message != null) ? message + ". " : "") +
						(error != null && !Utils.equalObjects(message, String.valueOf(error)) ? "Details: " + String.valueOf(error) : "");

					context.evaluateString(solutionScope,
						IExecutingEnviroment.TOPLEVEL_JSUNIT + "." + SolutionJSUnitSuiteCodeBuilder.FAIL_AFTER_CURRENT_TEST_KEY + " = new JsUnitError();",
						"unhandled error detected", 1, null);
					failAfterTest = jsunitAssertObject.get(SolutionJSUnitSuiteCodeBuilder.FAIL_AFTER_CURRENT_TEST_KEY, jsunitAssertObject); // should now be the JSUnitError
					ScriptableObject.putProperty((Scriptable)failAfterTest, "message", Context.javaToJS(adjustedMessage, solutionScope));
				}
			} // else it would have already failed; first reason is the most important one so leave that and discard this one (we can record multiple reasons in the future if needed)
		}
		finally
		{
			Context.exit();
		}
	}

	private static class TestIdentifier
	{
		private static long currentGeneratedClass = 0;

		public static synchronized String getNextGeneratedTestClass()
		{
			if (currentGeneratedClass == Long.MAX_VALUE) currentGeneratedClass = 0;
			return "TestClass" + (currentGeneratedClass++);
		}

		public TestIdentifier(String testName)
		{
			className = getNextGeneratedTestClass();
			name = testName;
		}

		private final String className;
		private final String name;

		public String getTestClassName()
		{
			return className;
		}

		public String getTestName()
		{
			return name;
		}
	}

}
