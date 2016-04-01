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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.dltk.core.IMethod;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.core.search.SearchEngine;
import org.eclipse.dltk.internal.launching.IPathEquality;
import org.eclipse.dltk.internal.launching.PathEqualityUtils;
import org.eclipse.dltk.testing.AbstractTestRunnerUI;
import org.eclipse.dltk.testing.ITestElementResolver;
import org.eclipse.dltk.testing.ITestRunnerUIExtension;
import org.eclipse.dltk.testing.ITestingEngine;
import org.eclipse.dltk.testing.TestElementResolution;
import org.eclipse.dltk.testing.model.ITestCaseElement;
import org.eclipse.dltk.testing.model.ITestElement;
import org.eclipse.dltk.testing.model.ITestSuiteElement;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.jsunit.actions.OpenEditorAtLineAction;
import com.servoy.eclipse.jsunit.launch.ITestLaunchConfigurationProvider;
import com.servoy.eclipse.jsunit.mobile.MobileStackOpenEditorAction;
import com.servoy.eclipse.model.test.TestTarget;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;

/**
 * Handles callbacks from scriptUnit view  (ex dobleclick on stac trace to go to that file in createOpenEditorAction(line) )
 *
 * @author obuligan
 */
public class JSUnitTestRunnerUI extends AbstractTestRunnerUI implements ITestElementResolver, ITestRunnerUIExtension
{

	private static final String SUPPORTED_LAUNCH_CONFIGURATION_ID = "launchConfigurationID";

	public static final String LAUNCH_CONFIGURATION_FINDER_EXTENSION = "com.servoy.eclipse.jsunit.launchConfigurationProvider";

	protected static final Pattern STACK_FRAME_PATTERN = Pattern.compile("(.*)\\.method\\(file:(\\d*).*");

	// lines like "scopes.globals.testRoundingFailure1(f:0)"
	protected static final Pattern MOBILE_STACK_FRAME_PATTERN = Pattern.compile("(?:(?:(.*)\\.(.*)\\.(.*))|javascript\\..*)?\\((.+):(\\d+)\\)");

	protected static final String FORM_TEST_ELEMENT_PATTERN = "Form '([\\w\\s]+)' tests";

	protected static final String SOLUTION_TEST_ELEMENT_PATTERN = "Solution '([\\w\\s]+)' tests";

	protected static final String SCOPE_TESTS_PATTERN = "Scope tests";

	final FlattenedSolution fl = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();
	protected final IScriptProject project;
	protected final JSUnitTestingEngine testingEngine;
	protected IPathEquality pathEquality;

	private static class JSUnitUIUtils
	{

		static boolean isSolutionSuite(ITestSuiteElement element)
		{
			return element.getSuiteTypeName().matches(SOLUTION_TEST_ELEMENT_PATTERN);
		}

		/**
		 * returns the solutoin name from a form suite
		 */
		static String getSolutionOfForm(ITestSuiteElement element)
		{
			return ((ITestSuiteElement)element.getParentContainer()).getSuiteTypeName().replaceAll(SOLUTION_TEST_ELEMENT_PATTERN, "$1");
		}

		/**
		 * returns the solution name from a scope suite
		 */
		static String getSolutionOfScope(ITestSuiteElement element)
		{
			return ((ITestSuiteElement)element.getParentContainer().getParentContainer()).getSuiteTypeName().replaceAll(SOLUTION_TEST_ELEMENT_PATTERN, "$1");
		}

		static boolean isScopeSuite(ITestSuiteElement element)
		{
			if (element.getParentContainer() != null && element.getParentContainer().getParentContainer() != null)
			{
				if (((ITestSuiteElement)element.getParentContainer()).getSuiteTypeName().matches(SCOPE_TESTS_PATTERN))
				{
					return true;
				}
			}
			return false;
		}

		/**
		 * returns the solution name from a form test
		 */
		static String getSolutionOfFormTest(ITestCaseElement element)
		{
			return ((ITestSuiteElement)element.getParentContainer().getParentContainer()).getSuiteTypeName().replaceAll(SOLUTION_TEST_ELEMENT_PATTERN, "$1");
		}

		static String getSolutionOfScopeTest(ITestCaseElement element)
		{
			return ((ITestSuiteElement)element.getParentContainer().getParentContainer().getParentContainer()).getSuiteTypeName().replaceAll(
				SOLUTION_TEST_ELEMENT_PATTERN, "$1");
		}

		/**
		 *  @return true is form test , false if Scope test
		 */
		static boolean isFormTest(ITestCaseElement element)
		{
			return ((ITestSuiteElement)element.getParentContainer()).getSuiteTypeName().matches(FORM_TEST_ELEMENT_PATTERN);
		}

		static boolean isFormSuite(ITestSuiteElement element)
		{
			return element.getSuiteTypeName().matches(FORM_TEST_ELEMENT_PATTERN);
		}

	}

	/**
	 * @param testingEngine
	 * @param project
	 */
	public JSUnitTestRunnerUI(JSUnitTestingEngine testingEngine, IScriptProject project)
	{
		this.testingEngine = testingEngine;
		this.project = project;
		// TODO use project environment specific entry
		this.pathEquality = PathEqualityUtils.getInstance();
	}

	/*
	 * @see org.eclipse.dltk.testing.ITestRunnerUI#getDisplayName()
	 */
	public String getDisplayName()
	{
		return testingEngine.getName();
	}

	@Override
	public boolean isStackFrame(String line)
	{
		return STACK_FRAME_PATTERN.matcher(line).matches() || MOBILE_STACK_FRAME_PATTERN.matcher(line).matches();
	}

	@Override
	public IAction createOpenEditorAction(String line)
	{
		Matcher matcher = STACK_FRAME_PATTERN.matcher(line);
		if (matcher.matches())
		{
			return new OpenEditorAtLineAction(matcher.group(1), Integer.parseInt(matcher.group(2)));
		}

		matcher = MOBILE_STACK_FRAME_PATTERN.matcher(line);
		if (matcher.matches())
		{
			return new MobileStackOpenEditorAction(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), Integer.parseInt(matcher.group(5)));
		}

		return null;
	}

	/*
	 * @see AbstractTestRunnerUI#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter)
	{
		if (ITestElementResolver.class.equals(adapter))
		{
			return this;
		}
		else
		{
			return super.getAdapter(adapter);
		}
	}

	protected final IDLTKSearchScope getSearchScope()
	{
		return SearchEngine.createSearchScope(project);
	}

	public TestElementResolution resolveElement(ITestElement element)
	{
		if (element instanceof ITestCaseElement)
		{
			return resolveTestCase((ITestCaseElement)element);
		}
		else if (element instanceof ITestSuiteElement)
		{
			return resolveTestSuite((ITestSuiteElement)element);
		}
		return null;
	}


	protected TestElementResolution resolveTestSuite(ITestSuiteElement element)
	{
		String suiteName = element.getSuiteTypeName();
		String solution = null;
		IFile scriptFile = null;
		//is it a form testSuite, ex:   - "Form 'name' test" - element
		if (JSUnitUIUtils.isFormSuite(element))
		{
			solution = JSUnitUIUtils.getSolutionOfForm(element);
			String formName = element.getSuiteTypeName().replaceAll(FORM_TEST_ELEMENT_PATTERN, "$1");
			scriptFile = ServoyModel.getWorkspace().getRoot().getFile(new Path(solution + "/forms/" + formName + ".js"));
		}
		//only if it is a scope suite
		else if (JSUnitUIUtils.isScopeSuite(element))
		{
			solution = JSUnitUIUtils.getSolutionOfScope(element);
			scriptFile = ServoyModel.getWorkspace().getRoot().getFile(new Path(solution + "/" + suiteName + ".js"));
		}
		if (scriptFile != null)
		{
			ISourceModule scriptModel = DLTKUIPlugin.getEditorInputModelElement(new org.eclipse.ui.part.FileEditorInput(scriptFile));
			return new TestElementResolution(scriptModel, null);
		}
		//IScriptProject scriptProject = DLTKCore.create(ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getProject());
		//return new TestElementResolution(scriptProject, null);
		return null;
	}

	protected TestElementResolution resolveTestCase(ITestCaseElement element)
	{

		String testMethod = element.getTestName();
		if (testMethod.equals("testSystemInitFailed")) return null;
		String formOrScope = ((ITestSuiteElement)element.getParentContainer()).getSuiteTypeName();
		String solution = null;
		IFile scriptFile = null;
		if (JSUnitUIUtils.isFormTest(element))
		{
			//->parent->parent->getSuiteTypeName()
			solution = JSUnitUIUtils.getSolutionOfFormTest(element);
			formOrScope = formOrScope.replaceAll(FORM_TEST_ELEMENT_PATTERN, "$1");
			scriptFile = ServoyModel.getWorkspace().getRoot().getFile(new Path(solution + "/forms/" + formOrScope + ".js"));
		}
		else
		{ // scope test
			//->parent->parent->parent->getSuiteTypeName()
			solution = JSUnitUIUtils.getSolutionOfScopeTest(element);
			scriptFile = ServoyModel.getWorkspace().getRoot().getFile(new Path(solution + "/" + formOrScope + ".js"));
		}

		IMethod methodModel = DLTKUIPlugin.getEditorInputModelElement(new org.eclipse.ui.part.FileEditorInput(scriptFile)).getMethod(testMethod);
		try
		{
			return new TestElementResolution(methodModel, methodModel.getSourceRange());
		}
		catch (ModelException e)
		{
			ServoyLog.logError(e);
			return null;
		}

	}

	@Override
	protected IPreferenceStore getPreferenceStore()
	{
		return com.servoy.eclipse.jsunit.Activator.getDefault().getPreferenceStore();
	}

	/*
	 * @see org.eclipse.dltk.testing.AbstractTestRunnerUI#canFilterStack()
	 */
	@Override
	public boolean canFilterStack()
	{
		return false;
	}

	@Override
	public String filterStackTrace(String trace)
	{
		BufferedReader reader = new BufferedReader(new StringReader(trace));
		try
		{
			StringWriter stringWriter = new StringWriter();
			PrintWriter printer = new PrintWriter(stringWriter);
			String line;
			// first line contains the thrown exception
			line = reader.readLine();
			if (line != null)
			{
				printer.println(line);
				// the stack frames of the trace
				while ((line = reader.readLine()) != null)
				{
					if (isStackFrame(line))
					{
						if (selectLine(line))
						{
							printer.println(line);
						}
					}
					else
					{
						printer.println(line);
					}
				}
			}
			return stringWriter.toString();
		}
		catch (IOException e)
		{
			// should not happen actually
			return trace;
		}
	}

	/**
	 * Tests if the specified line should pass thru the filter.
	 *
	 * @param line
	 * @return
	 */
	protected boolean selectLine(String line)
	{
		return true;
	}

	@Override
	public boolean canRerun(ITestElement testElement)
	{
		if (testElement instanceof ITestCaseElement)
		{
			return true;
		}
		else if (testElement instanceof ITestSuiteElement)
		{
			ITestSuiteElement suite = (ITestSuiteElement)testElement;
			if (JSUnitUIUtils.isFormSuite(suite))
			{
				return true;
			}
			else if (JSUnitUIUtils.isScopeSuite(suite))
			{
				return true;
			}
			else if (JSUnitUIUtils.isSolutionSuite(suite))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * ITestRunnerUIExtension's can Rerun an certain selected test.
	 * Calls  ITestRunnerUI canRerun(ITestElement)
	 */
	@Override
	public boolean canRerun(ITestElement testElement, String launchMode)
	{
		return canRerun(testElement);
	}

	@Override
	public boolean rerunTest(ILaunch launch, ITestElement element, String launchMode) throws CoreException
	{
		TestTarget target = null;
		if (element instanceof ITestCaseElement)
		{
			ITestCaseElement testCase = (ITestCaseElement)element;
			if (JSUnitUIUtils.isFormTest(testCase))
			{
				String formName = ((ITestSuiteElement)element.getParentContainer()).getSuiteTypeName().replaceAll(FORM_TEST_ELEMENT_PATTERN, "$1");
				Form form = fl.getForm(formName);
				target = new TestTarget(form, form.getScriptMethod(testCase.getTestName()));
			}
			else
			{// scope test case
				String scopeName = ((ITestSuiteElement)element.getParentContainer()).getSuiteTypeName().replaceAll(SCOPE_TESTS_PATTERN, "$1");
				target = new TestTarget(scopeName, fl.getScriptMethod(scopeName, testCase.getTestName()));
			}
		}
		else if (element instanceof ITestSuiteElement)
		{
			ITestSuiteElement suite = (ITestSuiteElement)element;
			if (JSUnitUIUtils.isFormSuite(suite))
			{
				String formName = suite.getSuiteTypeName().replaceAll(FORM_TEST_ELEMENT_PATTERN, "$1");
				Form form = fl.getForm(formName);
				target = new TestTarget(form);

			}
			else if (JSUnitUIUtils.isScopeSuite(suite))
			{
				String solName = JSUnitUIUtils.getSolutionOfScope(suite);
				Solution solution = TestTarget.findSolution(solName);
				Pair<Solution, String> pair = new Pair<Solution, String>(solution, suite.getSuiteTypeName());
				target = new TestTarget(pair);
			}
			else if (JSUnitUIUtils.isSolutionSuite(suite))
			{// if it is a module it will only test the module and not its submodules
				String solName = suite.getSuiteTypeName().replaceAll(SOLUTION_TEST_ELEMENT_PATTERN, "$1");
				Solution solution = TestTarget.findSolution(solName);
				target = new TestTarget(solution);
			}
		}
		if (target != null) relaunchTestTarget(target, launchMode, launch);

		return true;
	}

	private void relaunchTestTarget(TestTarget target, String launchMode, ILaunch launch)
	{
		// find the correct launch configuration using the providers extension point and use that
		try
		{
			ITestLaunchConfigurationProvider provider;
			ILaunchConfiguration launchConfiguration = null;
			String launchConfigurationType = launch.getLaunchConfiguration().getType().getIdentifier();

			IExtensionRegistry reg = Platform.getExtensionRegistry();
			IExtensionPoint ep = reg.getExtensionPoint(LAUNCH_CONFIGURATION_FINDER_EXTENSION);
			IExtension[] extensions = ep.getExtensions();

			if (extensions != null && extensions.length > 0)
			{
				for (IExtension extension : extensions)
				{
					IConfigurationElement[] ces = extension.getConfigurationElements();
					if (ces != null)
					{
						for (IConfigurationElement ce : ces)
						{
							if (launchConfigurationType.equals(ce.getAttribute(SUPPORTED_LAUNCH_CONFIGURATION_ID)))
							{
								try
								{
									provider = (ITestLaunchConfigurationProvider)ce.createExecutableExtension("class");
									launchConfiguration = provider.findOrCreateLaunchConfiguration(target, launchMode, launchConfigurationType, launch);
									if (launchConfiguration != null) break;
								}
								catch (CoreException e)
								{
									ServoyLog.logError(e);
								}
							}
						}
					}
					if (launchConfiguration != null) break;
				}
			}

			if (launchConfiguration != null)
			{
				String newMode = null;
				Set<Set<String>> modeCombinations = launchConfiguration.getType().getSupportedModeCombinations();
				Iterator<Set<String>> it = modeCombinations.iterator();
				Set<String> modes = null;
				while (it.hasNext() && newMode == null)
				{
					modes = it.next();
					if (modes.contains(launchMode)) newMode = launchMode;
				}
				if (newMode == null)
				{
					// well try anyway
					if (modes == null) newMode = launchMode;
					else newMode = modes.iterator().next(); // pick one of the sets, one of the modes...
				}
				DebugUITools.launch(launchConfiguration, newMode); // this actually doesn't support mode sets, so it's a bit strange
			}
			else ServoyLog.logError("Cannot find/create appropriate launch configuration for re-run.", null);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	public ITestingEngine getTestingEngine()
	{
		return testingEngine;
	}

	public IScriptProject getProject()
	{
		return project;
	}

}
