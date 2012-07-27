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
package com.servoy.eclipse.jsunit.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.SwingUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
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
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.progress.WorkbenchJob;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.SerialRule;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.debug.actions.StartJsUnitClientActionDelegate;
import com.servoy.eclipse.jsunit.SolutionRemoteTestRunner;
import com.servoy.eclipse.jsunit.SolutionUnitTestTarget;
import com.servoy.eclipse.jsunit.runner.ApplicationJSTestSuite;
import com.servoy.eclipse.jsunit.runner.JSUnitTestListenerHandler;
import com.servoy.eclipse.jsunit.runner.TestTarget;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.IDebugJ2DBClient;
import com.servoy.j2db.debug.RemoteDebugScriptEngine;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.scripting.IExecutingEnviroment;

/**
 * Runs the JS unit tests on active Servoy solution using the existing JUnit runner view.<br>
 * This class contains a lot of hacks and not so nice class accesses in order to reuse the JUnit runner view, as this view was only designed to work on an existing JavaModel from the
 * running workspace.<br>
 * If we would want to avoid this, we would either have to somehow simulate a complete Java Model in the workspace, or rewrite a similar view from scratch.
 * @author acostescu
 */
public class RunJSTests implements IObjectActionDelegate, IWorkbenchWindowActionDelegate
{

	private IWorkbenchWindow window;
	private boolean cancelCleanupShutDown = false;
	private IStructuredSelection structuredSelection;

//	/* Here is what this method is supposed to do (without details):
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
	public void run(IAction action)
	{
		cancelCleanupShutDown = false;
		boolean skipCleanup1 = false;
		final ServoyProject sp = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		if (sp != null)
		{
			// until more authentication options are supported, just warn the user about what won't work
			try
			{
				if (sp.getSolution() != null && sp.getSolution().getLoginSolutionName() != null)
				{
					UIUtils.runInUI(new Runnable()
					{
						public void run()
						{
							MessageDialog.openWarning(window == null ? Display.getCurrent().getActiveShell() : window.getShell(),
								"Unable to run unit tests", //$NON-NLS-1$
								"Running unit tests for solutions that require authentication through a login/authenticator solution is not currently supported.\n\nTo run unit tests for such a solution, create a new solution that does not require authentication and add the solution that requires authentication to it as a module.\nThis way you will be able to run tests without authenticating."); //$NON-NLS-1$
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
				// in case debug smart client was already used, start clean
				final IDebugJ2DBClient testApp = Activator.getDefault().getJSUnitJ2DBClient();
				if (!testApp.isShutDown())
				{
					// terminate script engine (because it might be suspended (at breakpoint for example), blocking AWT)
					// and we want to be able to stop the client right away
					IExecutingEnviroment scriptEngine = testApp.getScriptEngine();
					if (scriptEngine != null) scriptEngine.destroy();
					cancelCleanupShutDown = true;
				}

				final int port = SocketUtil.findFreePort();
				if (port == -1)
				{
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Cannot run solution JSUnit tests",
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

					// show the JUnit test view part
					showTestRunnerViewPartInActivePage(findTestRunnerViewPartInActivePage());

					final Launch launch = new Launch(null, ILaunchManager.RUN_MODE, null);

					try
					{
						// start junit test run session
						DebugPlugin.getDefault().getLaunchManager().addLaunch(launch);
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

						// start tests in AWT thread so that debugger gets attached
						skipCleanup1 = true;
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								boolean skipCleanup2 = false;
								try
								{
									testApp.shutDown(true);
									testApp.getClientInfo().clearUserInfo();

									final Job waitForSolutionToLoad = new Job("Running unit tests")
									{
										@Override
										protected IStatus run(IProgressMonitor monitor)
										{
											try
											{
												monitor.beginTask("waiting for debugger to start...", IProgressMonitor.UNKNOWN);

												int timeout = 1;// * 10; // there are lots of waitings inside "isConnected", so only try it once
												while (!RemoteDebugScriptEngine.isConnected() && timeout > 0 && !monitor.isCanceled())
												{
													timeout--;
													try
													{
														Thread.sleep(100);
													}
													catch (InterruptedException e)
													{
														ServoyLog.logError(e);
													}
												}
												if (!RemoteDebugScriptEngine.isConnected())
												{
													// log this but continue anyway (but without debugging working probably
													ServoyLog.logWarning("Debugger start timeout while running jsunit tests.", null);
												}

												monitor.setTaskName("waiting for solution to be loaded in client...");

												// wait for solution to load
												timeout = 60 * 10; // 60 sec max wait for sol. to load
												while (!testApp.isDoneLoading() && timeout > 0 && !monitor.isCanceled())
												{
													timeout--;
													try
													{
														Thread.sleep(100);
													}
													catch (InterruptedException e)
													{
														ServoyLog.logError(e);
													}
												}
												if (!testApp.isDoneLoading())
												{
													ServoyLog.logError("Timeout/cancel while waiting for solution to be loaded in test app.", null);
													if (monitor.isCanceled())
													{
														return Status.CANCEL_STATUS;
													}
													else
													{
														return new Status(IStatus.ERROR, com.servoy.eclipse.jsunit.Activator.PLUGIN_ID,
															"Timeout or cancel while waiting for solution to be loaded in test app.");
													}
												}

												monitor.setTaskName("testing...");
												SwingUtilities.invokeAndWait(new Runnable()
												{
													public void run()
													{

														ApplicationJSTestSuite.setTestTarget(testApp, getTestTarget());
														try
														{
															SolutionRemoteTestRunner.main(new String[] { "-version", "3", "-port", String.valueOf(port), "-testLoaderClass", "org.eclipse.jdt.internal.junit.runner.junit3.JUnit3TestLoader", "loaderpluginname", "org.eclipse.jdt.junit.runtime", "-classNames", ApplicationJSTestSuite.class.getCanonicalName() });
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

												});
											}
											catch (InterruptedException e)
											{
												ServoyLog.logError(e);
												return new Status(IStatus.ERROR, com.servoy.eclipse.jsunit.Activator.PLUGIN_ID,
													"Problem encountered when trying to run tests.", e);
											}
											catch (InvocationTargetException e)
											{
												ServoyLog.logError(e);
												ServoyLog.logError("Caused by:", e.getTargetException());
												return new Status(IStatus.ERROR, com.servoy.eclipse.jsunit.Activator.PLUGIN_ID,
													"Problem encountered when trying to run tests.", e.getTargetException());
											}
											finally
											{
												monitor.done();
												cleanUp1(launch);
												cleanUp2();
											}
											return Status.OK_STATUS;
										}
									};

									// start smart client
									Job startSmartClientJob = new WorkbenchJob("Starting unit test client")
									{
										@Override
										public IStatus runInUIThread(IProgressMonitor monitor)
										{
											try
											{
												cancelCleanupShutDown = false; // because of the rule, last run session should have already finished by now, so no more danger in old cleanup shutting down newly open client
												StartJsUnitClientActionDelegate startJsUnitClientAction = new StartJsUnitClientActionDelegate();
												startJsUnitClientAction.init(window);
												startJsUnitClientAction.run((IAction)null);

												if (startJsUnitClientAction.clientStartSucceeded())
												{
													waitForSolutionToLoad.schedule(); // second job - will be canceled if first fails
												}
												else
												{
													// test client start aborted by user or test client could not be started
													cleanUp1(launch);
													cleanUp2();
													return Status.CANCEL_STATUS;
												}
											}
											catch (RuntimeException e)
											{
												ServoyLog.logError(e);
												cleanUp1(launch);
												cleanUp2();
												return new Status(IStatus.ERROR, com.servoy.eclipse.jsunit.Activator.PLUGIN_ID,
													"Cannot start unit test SmartClient", e);
											}
											return Status.OK_STATUS;
										}
									};
									waitForSolutionToLoad.setRule(SerialRule.INSTANCE);
									startSmartClientJob.setRule(SerialRule.INSTANCE);
									skipCleanup2 = true;
									startSmartClientJob.schedule(); // first job
								}
								finally
								{
									if (!skipCleanup2)
									{
										cleanUp1(launch);
										cleanUp2();
									}
								}
							}
						});
					}
					finally
					{
						if (!skipCleanup1)
						{
							cleanUp1(launch);
						}
					}
				}
			}
			finally
			{
				if (!skipCleanup1)
				{
					cleanUp2();
				}
			}
		}
	}

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

	private void cleanUp1(Launch launch)
	{
		// TestRunSession expects to be notified by launch manager that the launch ended in order for
		// it's RemoteTestRunnerClient to stop waiting for clients connections - so this has to be handled somehow
		DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
	}

	private void cleanUp2()
	{
		if (!cancelCleanupShutDown)
		{
			Activator plugin = Activator.getDefault();
			if (plugin != null)
			{
				plugin.getJSUnitJ2DBClient().invokeLater(new Runnable()
				{
					public void run()
					{
						Activator plugin = Activator.getDefault();
						if (plugin != null)
						{
							plugin.getJSUnitJ2DBClient().shutDown(false);
							try
							{
								Thread.sleep(1000);
								plugin = Activator.getDefault();
								if (plugin != null)
								{
									plugin.getJSUnitJ2DBClient().shutDown(true);
								}
							}
							catch (InterruptedException e)
							{
							}
							plugin = Activator.getDefault();
							if (plugin != null)
							{
								plugin.getJSUnitJ2DBClient().getClientInfo().clearUserInfo();
							}
						}
					}
				});
			}
		}
	}

	private TestRunnerViewPart showTestRunnerViewPartInActivePage(TestRunnerViewPart testRunner)
	{
		IWorkbenchPart activePart = null;
		IWorkbenchPage page = null;
		try
		{
			// TODO: have to force the creation of view part contents 
			// otherwise the UI will not be updated
			if (testRunner != null) // TODO disabled this in 3.6.1: && testRunner.isCreated()
			{
				testRunner.showTestResultsView(); // TODO check: added this for 3.6.1
				return testRunner;
			}
			page = JUnitPlugin.getActivePage();
			if (page == null) return null;
			activePart = page.getActivePart();
			//	show the result view if it isn't shown yet
			return (TestRunnerViewPart)page.showView(TestRunnerViewPart.NAME);
		}
		catch (PartInitException pie)
		{
			JUnitPlugin.log(pie);
			return null;
		}
		finally
		{
			//restore focus stolen by the creation of the result view
			if (page != null && activePart != null) page.activate(activePart);
		}
	}

	private TestRunnerViewPart findTestRunnerViewPartInActivePage()
	{
		IWorkbenchPage page = JUnitPlugin.getActivePage();
		if (page == null) return null;
		return (TestRunnerViewPart)page.findView(TestRunnerViewPart.NAME);
	}

	public void selectionChanged(IAction action, ISelection selection)
	{
		if (selection instanceof IStructuredSelection)
		{
			this.structuredSelection = (IStructuredSelection)selection;
		}
	}

	protected TestTarget getTestTarget()
	{
		if (structuredSelection != null)
		{
			if (structuredSelection.size() == 1)
			{
				Object fe = structuredSelection.getFirstElement();
				if (fe instanceof SolutionUnitTestTarget)
				{
					return ((SolutionUnitTestTarget)fe).getTestTarget();
				}
			}
		}
		return null;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart)
	{
		// not interested
		window = targetPart.getSite().getWorkbenchWindow();
	}

	public void dispose()
	{
		structuredSelection = null;
	}

	public void init(IWorkbenchWindow window)
	{
		this.window = window;
	}

}
