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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.dltk.internal.testing.ui.TestRunnerViewPart;
import org.eclipse.dltk.testing.DLTKTestingPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.JUnitPreferencesConstants;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.jsunit.SolutionRemoteTestRunner;
import com.servoy.eclipse.jsunit.launch.JSUnitLaunchConfigurationDelegate;
import com.servoy.eclipse.jsunit.runner.JSUnitTestListenerHandler;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.test.TestTarget;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * Runs the servoy unit tests on the active solution using the java JUunit runner and dltk.testing Script Unit view.
 * The hacks are still present because one can switch to junit view from open view menu and using the junit view go to the stack line as before.
 * @author obuligan
 */
public abstract class RunJSUnitTests implements Runnable
{

	private RemoteScriptUnitRunnerClient scriptUnitRunnerClient;

	private final IProgressMonitor monitor;
	protected final boolean debugMode;

	public RunJSUnitTests(TestTarget testTarget, ILaunch launch, IProgressMonitor monitor, boolean debugMode)
	{
		this.monitor = monitor;
		this.testTarget = testTarget;
		this.launch = launch;
		this.debugMode = debugMode;
		Display.getDefault().syncExec(new Runnable()
		{
			@Override
			public void run()
			{
				window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			}
		});
		sp = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
	}

	private ILaunch launch = null;

	protected TestTarget testTarget = null;
	protected final ServoyProject sp;
	protected IWorkbenchWindow window;
	protected boolean cancelCleanupShutDown = false;

//	/* Here is what this method is supposed to do for SmartClient runner (without details and might be obsolete):
//	 *    - if SmartClient is open then close it.
//	 *    - start SmartClient using standard run action
//	 *    - wait for solution to be loaded
//	 *    - start running unit tests
//	 * Because of the way SmartClient opens/closes (using lots of invokeLater on AWT thread), we do this in 5 steps:
//	 *    1. on current thread see if client is open and stop script engine if necessary
//	 *    2. later on AWT thread shutDown client and log-out
//	 *    3. later in a job, start client again
//	 *    4. later in a job, wait for the solution to be loaded into the SmartClient
//	 *    5. later on AWT run the tests.
//	 * 1 starts 2, 2 starts 3 and 4 (but 4 will wait for 3 to finish), 4 starts 5.
//	 */
	@SuppressWarnings("restriction")
	public void run()
	{
		cancelCleanupShutDown = false;
		boolean skipCleanup1 = false;
		if (sp != null)
		{
			// until more authentication options are supported, just warn the user about what won't work
			try
			{
				if (sp.getSolution() != null && (sp.getSolution().getMustAuthenticate() || sp.getSolution().getLoginSolutionName() != null))
				{
					UIUtils.runInUI(new Runnable()
					{
						public void run()
						{
							MessageDialog.openWarning(window == null ? UIUtils.getActiveShell() : window.getShell(), "Unable to run unit tests",
								"Running unit tests for solutions that require authentication through a login/authenticator form/solution is not currently supported.\n\nTo run unit tests for such a solution, create a new solution that does not require authentication and add the solution that requires authentication to it as a module.\nThis way you will be able to run tests without authenticating.");
						}
					}, false);
					return;
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}

			try
			{
				prepareForTesting();
				if (getLaunchMonitor() != null && getLaunchMonitor().isCanceled()) return;

				final int port = SocketUtil.findFreePort();
				if (port == -1)
				{
					MessageDialog.openError(UIUtils.getActiveShell(), "Cannot run solution JSUnit tests",
						"No free port for JUnit launch was found.");
				}
				else
				{
					// clear history if needed to obey JUnitPreferencesConstants.MAX_TEST_RUNS
					int maxCount = JUnitPlugin.getDefault().getPreferenceStore().getInt(JUnitPreferencesConstants.MAX_TEST_RUNS);
					List<TestRunSession> testRunSessions = JUnitCorePlugin.getModel().getTestRunSessions();
					int toDelete = testRunSessions.size() - maxCount;
					while (toDelete > 0)
					{
						toDelete--;
						TestRunSession session = testRunSessions.remove(testRunSessions.size() - 1);
						JUnitCorePlugin.getModel().removeTestRunSession(session);
					}

					removeDLTKTestSessions();
					// show the ScriptUnit test view part
					showTestRunnerViewPartInActivePage(0);
					if (getLaunchMonitor() != null && getLaunchMonitor().isCanceled()) return;

					//final Launch launch = new Launch(null, ILaunchManager.RUN_MODE, null);

					// start junit test run session
					//DebugPlugin.getDefault().getLaunchManager().addLaunch(launch);
					//DebugUITools.launch(launch, ILaunchManager.DEBUG_MODE);
					JUnitCorePlugin.getModel().addTestRunSession(new TestRunSession(launch, new JavaProject(sp.getProject(), null)
					{
						@Override
						public IType findType(String fullyQualifiedName, IProgressMonitor progressMonitor) throws JavaModelException
						{
							return getJavaType(fullyQualifiedName);
						}

						@Override
						public String[] getRequiredProjectNames() throws JavaModelException
						{
							return new String[0];
						}
					}, port));
					addDLTKTestRunSession();

					skipCleanup1 = true;
					if (getLaunchMonitor() != null && getLaunchMonitor().isCanceled()) return;
					initializeAndRun(port);
				}
			}
			finally
			{
				if (!skipCleanup1)
				{
					cleanUpAfterPrepare();
				}
			}
		}
	}

	protected void runJUnitClass(final int port, Class< ? > suiteClass)
	{
		try
		{
			showTestRunnerViewPartInActivePage(200); // try to avoid JUnit view getting on top of a view stack by itself in case it was open by the user in the past - we still run the suite using JUnit implementation
			SolutionRemoteTestRunner.main(new String[] { "-version", "3", "-port", String.valueOf(
				port), "-testLoaderClass", "org.eclipse.jdt.internal.junit.runner.junit3.JUnit3TestLoader", "loaderpluginname", "org.eclipse.jdt.junit.runtime", "-classNames", suiteClass.getCanonicalName() });
			showTestRunnerViewPartInActivePage(200); // try to avoid JUnit view getting on top of a view stack by itself in case it was open by the user in the past - we still run the suite using JUnit implementation
		}
		catch (RuntimeException e)
		{
			if (cancelCleanupShutDown)
			{
				ServoyLog.logInfo("Exception while running unit tests - probably because of force stop to restart them...");
			}
			else
			{
				throw e;
			}
		}
	}

	protected abstract void prepareForTesting();

	protected abstract void initializeAndRun(int port);

	/**
	 * Called for cleanup if {@link #prepareForTesting()} got called but some exception resulted in {@link #initializeAndRun(int)} not being called.
	 * This should also be called probably after test run ends (successfully or not) - controlled by code inside {@link #initializeAndRun(int)}. (which will probably run async)
	 */
	protected abstract void cleanUpAfterPrepare();

	// return a reference to the js file as a Java IType so that it is handled correctly by the JUnit view when presenting error/failure stacks
	private IType getJavaType(String fullyQualifiedName)
	{
		if (fullyQualifiedName != null)
		{
			String fileName = JSUnitTestListenerHandler.getFileNameFromJavaType(fullyQualifiedName);
			IPath pathToFile = new Path(fileName);
			IPath workspacePath = ServoyModel.getWorkspace().getRoot().getLocation();
			if (workspacePath.isPrefixOf(pathToFile))
			{
				pathToFile = pathToFile.makeRelativeTo(workspacePath);
				String[] path = pathToFile.segments();
				if (path.length >= 2)
				{
					// first element is project then the rest are the path to a js file; get them as Java model ITypes
					try
					{
						IProject p = ServoyModel.getWorkspace().getRoot().getProject(path[0]);
						IResource pfrResource = (path.length == 2) ? p : p.getFolder(pathToFile.removeFirstSegments(1).removeLastSegments(1));
						IJavaProject javaProject = JavaCore.create(p);
						IPackageFragmentRoot pfr = javaProject.getPackageFragmentRoot(pfrResource);
						IPackageFragment pf = pfr.getPackageFragment("");

						return pf.getCompilationUnit(path[path.length - 1]).getType(path[path.length - 1].substring(0, path[path.length - 1].length() - 3));
					}
					catch (Exception e)
					{
						ServoyLog.logWarning("Cannot jump from stack trace into JS editor because of exception", e);
					}
				}
			}
		}

		return null;
	}

//	/**
//	 * @deprecated no longer needed
//	 * @param launch
//	 */
//	@Deprecated
//	private void cleanUp1(ILaunch launch)
//	{
//		// TestRunSession expects to be notified by launch manager that the launch ended in order for
//		// it's RemoteTestRunnerClient to stop waiting for clients connections - so this has to be handled somehow
//		//DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
//	}

	public static void showTestRunnerViewPartInActivePage(final int t)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				Runnable r = new Runnable()
				{
					@Override
					public void run()
					{
						IWorkbenchPage page = null;
						IWorkbenchPartReference ap = null;
						try
						{
							page = DLTKTestingPlugin.getActivePage();
							if (page == null) return;
							ap = page.getActivePartReference();
							// show the result view if it isn't shown yet
							page.showView(TestRunnerViewPart.NAME);
						}
						catch (PartInitException pie)
						{
							ServoyLog.logWarning("Cannot show unit test view", pie);
						}
						finally
						{
							if (ap != null && !ap.getId().equals(org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart.NAME)) page.activate(ap.getPart(true));
						}
					}
				};
				if (t == 0) r.run();
				else Display.getCurrent().timerExec(t, r);
			}
		});

	}

	private void addDLTKTestRunSession()
	{
		//must be run on the UI thread because the TestView part only attaches as a listener to the test session if it is the UI thread
		Display.getDefault().syncExec(new Runnable()
		{

			@Override
			public void run()
			{
				DLTKTestingPlugin.getModel().addTestRunSession(new org.eclipse.dltk.internal.testing.model.TestRunSession(launch,
					org.eclipse.dltk.core.DLTKCore.create(sp.getProject()), scriptUnitRunnerClient = new RemoteScriptUnitRunnerClient()));
			}
		});
	}

	protected RemoteScriptUnitRunnerClient getScriptUnitRunnerClient()
	{
		return scriptUnitRunnerClient;
	}

	protected IProgressMonitor getLaunchMonitor()
	{
		return monitor;
	}

	private void removeDLTKTestSessions()
	{
		List<org.eclipse.dltk.internal.testing.model.TestRunSession> testRunSessions = DLTKTestingPlugin.getModel().getTestRunSessions();
		int toDelete = testRunSessions.size() - JSUnitLaunchConfigurationDelegate.MAX_CONFIG_INSTANCES;
		//max TestRunSession history size was chosen based on max config instance size . 1 config instance can be run 10 times and still fill the history.
		// The edge case to cover would be 10 different consecutive runs each based on a different target.
		while (toDelete > 0)
		{
			toDelete--;
			org.eclipse.dltk.internal.testing.model.TestRunSession session = testRunSessions.remove(testRunSessions.size() - 1);
			final org.eclipse.dltk.internal.testing.model.TestRunSession tempTestRunSession = session;
			Display.getDefault().syncExec(new Runnable()
			{
				@Override
				public void run()
				{
					DLTKTestingPlugin.getModel().removeTestRunSession(tempTestRunSession);
				}
			});
		}
	}
}
