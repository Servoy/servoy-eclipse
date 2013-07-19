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
package com.servoy.eclipse.debug.actions;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import javax.swing.SwingUtilities;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.dltk.debug.ui.DLTKDebugUIPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.internal.browser.BrowserManager;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.debug.FlattenedSolutionDebugListener;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.IDebugWebClient;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;

/**
 * @author jcompagner
 * 
 */
public class StartWebClientActionDelegate extends StartDebugAction implements IRunnableWithProgress
{
	private IWorkbenchWindow window;

	/**
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
	public void init(IWorkbenchWindow window)
	{
		this.window = window;
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action)
	{
		//make sure the plugins are loaded
		DLTKDebugUIPlugin.getDefault();
		DebugPlugin.getDefault();

		try
		{
			window.getWorkbench().getProgressService().run(true, false, this);
		}
		catch (InvocationTargetException e)
		{
			ServoyLog.logError(e);
		}
		catch (InterruptedException e)
		{
			ServoyLog.logError(e);
		}
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
	{
		monitor.beginTask("Web client start", 5); //$NON-NLS-1$
		monitor.worked(1);
		try
		{
			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ServoyProject activeProject = servoyModel.getActiveProject();
			if (activeProject != null && activeProject.getSolution() != null)
			{
				final Solution solution = activeProject.getSolution();
				if (solution.getSolutionType() == SolutionMetaData.SMART_CLIENT_ONLY)
				{
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							MessageDialog.openError(Display.getDefault().getActiveShell(),
								"Solution type problem", "Cant open this solution type in this client"); //$NON-NLS-1$
						}
					});
					return;
				}
				monitor.worked(2);
				if (testAndStartDebugger())
				{
					monitor.worked(3);
					final IDebugWebClient debugWebClient = Activator.getDefault().getDebugWebClient();
					if (debugWebClient != null && debugWebClient.getFlattenedSolution().getDebugListener() == null)
					{
						debugWebClient.getFlattenedSolution().registerDebugListener(new FlattenedSolutionDebugListener());
					}
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							if (debugWebClient != null)
							{
								debugWebClient.shutDown(true);
							}
							try
							{
								String url = "http://localhost:" + ApplicationServerSingleton.get().getWebServerPort() + "/servoy-webclient/solutions/solution/" + solution.getName(); //$NON-NLS-1$ //$NON-NLS-2$
								IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
								IWebBrowser browser = support.getExternalBrowser();
								IBrowserDescriptor browserDescriptor = BrowserManager.getInstance().getCurrentWebBrowser();
								// temporary implementation until we upgrade to eclipse 4.3
								// see https://bugs.eclipse.org/bugs/show_bug.cgi?format=multiple&id=405942
								if (browserDescriptor != null && browserDescriptor.getLocation() != null && browserDescriptor.getLocation().contains(" "))
								{
									String[] command = new String[2];
									command[0] = browserDescriptor.getLocation();
									command[1] = url;
									Runtime.getRuntime().exec(command);
								}
								else
								{
									browser.openURL(new URL(url));
								}
							}
							catch (final Throwable e)//catch all for apple mac
							{
								ServoyLog.logError("Cant open external browser", e); //$NON-NLS-1$
								Display.getDefault().asyncExec(new Runnable()
								{
									public void run()
									{
										MessageDialog.openError(Display.getDefault().getActiveShell(), "Cant open external browser", e.getLocalizedMessage()); //$NON-NLS-1$
									}
								});
							}
						}
					});
					monitor.worked(4);
				}
			}
		}
		finally
		{
			monitor.done();
		}
	}


	@Override
	public void selectionChanged(IAction action, ISelection selection)
	{
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		final ServoyProject activeProject = servoyModel.getActiveProject();
		boolean enabled = true;
		if (activeProject != null && activeProject.getSolution() != null)
		{
			final Solution solution = activeProject.getSolution();
			if (solution.getSolutionType() == SolutionMetaData.SMART_CLIENT_ONLY) enabled = false;
		}
		else
		{
			enabled = false;
		}
		action.setEnabled(enabled);
	}

}
