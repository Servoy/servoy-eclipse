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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.dltk.testing.ITestRunnerUI;
import org.eclipse.dltk.testing.ITestingEngine;
import org.eclipse.dltk.testing.TestElementResolution;
import org.eclipse.dltk.testing.model.ITestCaseElement;
import org.eclipse.dltk.testing.model.ITestElement;
import org.eclipse.dltk.testing.model.ITestSuiteElement;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.jsunit.actions.OpenEditorAtLineAction;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Handles callbacks from scriptUnit view  (ex dobleclick on stac trace to go to that file in createOpenEditorAction(line) )
 * 
 * @author obuligan
 */
public class JSUnitTestRunnerUI extends AbstractTestRunnerUI implements ITestRunnerUI, ITestElementResolver
{

	protected static final Pattern STACK_FRAME_PATTERN = Pattern.compile("(.*)\\.method\\(file:(\\d*).*"); //$NON-NLS-1$

	protected static final String FORM_TEST_ELEMENT_PATTERN = "Form '([\\w\\s]+)' tests";

	protected static final String SOLUTION_TEST_ELEMENT_PATTERN = "Solution '([\\w\\s]+)' tests";

	protected static final String SCOPE_TESTS_PATTERN = "Scope tests";


	protected final IScriptProject project;
	protected final JSUnitTestingEngine testingEngine;
	protected IPathEquality pathEquality;

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
		return STACK_FRAME_PATTERN.matcher(line).matches();
	}

	@Override
	public IAction createOpenEditorAction(String line)
	{
		Matcher matcher = STACK_FRAME_PATTERN.matcher(line);
		if (matcher.matches())
		{
			return new OpenEditorAtLineAction(matcher.group(1), Integer.parseInt(matcher.group(2)));
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
		//is it a form testSuite:  "Form 'name' test" element
		if (suiteName.matches(FORM_TEST_ELEMENT_PATTERN))
		{
			solution = ((ITestSuiteElement)element.getParentContainer()).getSuiteTypeName().replaceAll(SOLUTION_TEST_ELEMENT_PATTERN, "$1");
			String formName = suiteName.replaceAll(FORM_TEST_ELEMENT_PATTERN, "$1");
			scriptFile = ServoyModel.getWorkspace().getRoot().getFile(new Path(solution + "/forms/" + formName + ".js"));
		}
		//only if it is a scope suite
		else if (element.getParentContainer() != null && element.getParentContainer().getParentContainer() != null)
		{
			if (((ITestSuiteElement)element.getParentContainer()).getSuiteTypeName().matches(SCOPE_TESTS_PATTERN))
			{
				solution = ((ITestSuiteElement)element.getParentContainer().getParentContainer()).getSuiteTypeName().replaceAll(SOLUTION_TEST_ELEMENT_PATTERN,
					"$1");
				scriptFile = ServoyModel.getWorkspace().getRoot().getFile(new Path(solution + "/" + suiteName + ".js"));
			}
		}
		if (scriptFile != null)
		{
			ISourceModule scriptModel = DLTKUIPlugin.getEditorInputModelElement(new org.eclipse.ui.part.FileEditorInput(scriptFile));
			return new TestElementResolution(scriptModel, null);
		}
		return null;
	}

	protected TestElementResolution resolveTestCase(ITestCaseElement element)
	{

		String testMethod = element.getTestName();
		if (testMethod.equals("testReason")) return null;
		String formOrScope = ((ITestSuiteElement)element.getParentContainer()).getSuiteTypeName();
		String solution = null;
		IFile scriptFile = null;
		boolean isFormTest = formOrScope.matches(FORM_TEST_ELEMENT_PATTERN);
		if (isFormTest)
		{
			//->parent->parent->getSuiteTypeName()
			solution = ((ITestSuiteElement)element.getParentContainer().getParentContainer()).getSuiteTypeName().replaceAll(SOLUTION_TEST_ELEMENT_PATTERN, "$1");
			formOrScope = formOrScope.replaceAll(FORM_TEST_ELEMENT_PATTERN, "$1");
			scriptFile = ServoyModel.getWorkspace().getRoot().getFile(new Path(solution + "/forms/" + formOrScope + ".js"));
		}
		else
		{ //->parent->parent->parent->getSuiteTypeName()
			solution = ((ITestSuiteElement)element.getParentContainer().getParentContainer().getParentContainer()).getSuiteTypeName().replaceAll(
				SOLUTION_TEST_ELEMENT_PATTERN, "$1");
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

	protected String extractFileName(String line)
	{
		Matcher matcher = STACK_FRAME_PATTERN.matcher(line);
		boolean matches = matcher.matches();
		if (!matches)
		{
			matcher = STACK_FRAME_PATTERN.matcher(line);
			matches = matcher.matches();
		}
		if (matches)
		{
			return matcher.group(1);
		}
		else
		{
			return null;
		}
	}

	/*
	 * @see org.eclipse.dltk.testing.ITestRunnerUI#getTestingEngine()
	 */
	public ITestingEngine getTestingEngine()
	{
		return testingEngine;
	}

	/*
	 * @see org.eclipse.dltk.testing.ITestRunnerUI#getProject()
	 */
	public IScriptProject getProject()
	{
		return project;
	}

}
