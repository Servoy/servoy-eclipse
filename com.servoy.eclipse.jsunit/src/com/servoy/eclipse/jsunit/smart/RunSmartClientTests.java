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

package com.servoy.eclipse.jsunit.smart;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.ui.progress.WorkbenchJob;

import com.servoy.eclipse.core.repository.SwitchableEclipseUserManager;
import com.servoy.eclipse.core.util.SerialRule;
import com.servoy.eclipse.jsunit.Activator;
import com.servoy.eclipse.jsunit.scriptunit.RunJSUnitTests;
import com.servoy.eclipse.model.test.TestTarget;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.IDebugJ2DBClient;
import com.servoy.j2db.debug.RemoteDebugScriptEngine;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * @author acostescu
 */
public class RunSmartClientTests extends RunJSUnitTests
{

	private IDebugJ2DBClient testApp;
	private JSUnitUserManager testUserManager;

	public RunSmartClientTests(TestTarget testTarget, ILaunch launch, IProgressMonitor monitor)
	{
		super(testTarget, launch, monitor);
	}

	@Override
	protected void prepareForTesting()
	{
		// in case debug smart client was already used, start clean
		testApp = Activator.getDefault().getJSUnitJ2DBClient();
		if (!testApp.isShutDown())
		{
			// terminate script engine (because it might be suspended (at breakpoint for example), blocking AWT)
			// and we want to be able to stop the client right away
			IExecutingEnviroment scriptEngine = testApp.getScriptEngine();
			if (scriptEngine != null) scriptEngine.destroy();
			cancelCleanupShutDown = true;
		}
		testUserManager = ((JSUnitUserManager)testApp.getUserManager());
		testUserManager.reloadFromWorkspace();
	}

	@Override
	protected void initializeAndRun(final int port)
	{
		// start tests in AWT thread so that debugger gets attached
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
										return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
											"Timeout or cancel while waiting for solution to be loaded in test app.");
									}
								}

								monitor.setTaskName("testing...");
								SwingUtilities.invokeAndWait(new Runnable()
								{
									public void run()
									{
										SmartClientTestSuite.setTestTarget(testApp, testTarget);
										runJUnitClass(port, SmartClientTestSuite.class);
									}

								});
							}
							catch (InterruptedException e)
							{
								ServoyLog.logError(e);
								return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Problem encountered when trying to run tests.", e);
							}
							catch (InvocationTargetException e)
							{
								ServoyLog.logError(e);
								ServoyLog.logError("Caused by:", e.getTargetException());
								return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Problem encountered when trying to run tests.", e.getTargetException());
							}
							finally
							{
								monitor.done();
								cleanUpAfterPrepare();
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
								((SwitchableEclipseUserManager)ApplicationServerRegistry.get().getUserManager()).switchTo(testUserManager); // use testUserManager in app. server code as well
								try
								{
									startJsUnitClientAction.execute((ExecutionEvent)null);
								}
								catch (ExecutionException e)
								{
									ServoyLog.logError(e);
								}

								if (startJsUnitClientAction.clientStartSucceeded())
								{
									waitForSolutionToLoad.schedule(); // second job - will be canceled if first fails
								}
								else
								{
									// test client start aborted by user or test client could not be started
									cleanUpAfterPrepare();
									return Status.CANCEL_STATUS;
								}
							}
							catch (RuntimeException e)
							{
								ServoyLog.logError(e);
								cleanUpAfterPrepare();
								return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Cannot start unit test SmartClient", e);
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
						cleanUpAfterPrepare();
					}
				}
			}
		});
	}

	@Override
	protected void cleanUpAfterPrepare()
	{
		((SwitchableEclipseUserManager)ApplicationServerRegistry.get().getUserManager()).switchTo(null); // restore use of EclipseUserManager in app. server code
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

}
