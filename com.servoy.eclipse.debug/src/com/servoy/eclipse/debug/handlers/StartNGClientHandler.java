/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.debug.handlers;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.debug.NGClientStarter;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.preferences.Ng2DesignerPreferences;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.IDebugClient;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * @author jcompagner
 *
 */
public class StartNGClientHandler extends StartWebClientHandler implements NGClientStarter
{
	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.debug.actions.StartWebClientActionDelegate#getStartTitle()
	 */
	@Override
	public String getStartTitle()
	{
		return "NGClient launch";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.debug.actions.StartWebClientActionDelegate#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
	{
		runProgressBarAndNGClient(monitor);
	}

	/**
	 * @param monitor
	 */
	@Override
	public void startNGClient(IProgressMonitor monitor)
	{
		StartClientHandler.setLastCommand(StartClientHandler.START_NG_CLIENT);
		monitor.beginTask(getStartTitle(), 5);
		monitor.worked(1);
		try
		{
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
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
							MessageDialog.openError(UIUtils.getActiveShell(), "Solution type problem",
								"Cant open this solution type in this client");
						}
					});
					return;
				}
				monitor.worked(2);
				if (testAndStartDebugger())
				{
					monitor.worked(3);
					final String solution_path = (new Ng2DesignerPreferences()).launchNG2() ? "/solution/" : "/solutions/";
					final IDebugClient debugNGClient = Activator.getDefault().getDebugNGClient();
					if (debugNGClient != null && debugNGClient.getSolution() != null)
					{
						debugNGClient.shutDown(true);
					}
					try
					{
						String url = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + solution_path + solution.getName() +
							"/index.html";
						EditorUtil.openURL(getWebBrowser(), url);
					}
					catch (final Throwable e)//catch all for apple mac
					{
						ServoyLog.logError("Cant open external browser", e);
						Display.getDefault().asyncExec(new Runnable()
						{
							public void run()
							{
								MessageDialog.openError(UIUtils.getActiveShell(), "Cant open external browser", e.getLocalizedMessage());
							}
						});
					}
					monitor.worked(4);
				}
			}
		}
		finally
		{
			monitor.done();
		}
	}
}
