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
package com.servoy.eclipse.debug.handlers;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.dltk.debug.ui.DLTKDebugUIPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.internal.browser.BrowserManager;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;
import org.eclipse.ui.internal.browser.Messages;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.IDebugWebClient;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * @author jcompagner
 *
 */
public class StartWebClientHandler extends StartDebugHandler implements IRunnableWithProgress, IHandler
{


	private IWebBrowser webBrowser;
	private IEclipsePreferences eclipsePreferences;

	protected static final String COMMAND_PARAMETER = "com.servoy.eclipse.debug.browser";
	protected static final String LAST_USED_BROWSER = "com.servoy.eclipse.debug.browser.last";

	//private HashMap<String, Image> browsersImagesList;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		//make sure the plugins are loaded
		DLTKDebugUIPlugin.getDefault();
		DebugPlugin.getDefault();

		initBrowser(event.getParameter(COMMAND_PARAMETER));

		Job job = new Job(getStartTitle())
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				try
				{
					StartWebClientHandler.this.run(monitor);
				}
				catch (InvocationTargetException itex)
				{
					ServoyLog.logError(itex);
				}
				catch (InterruptedException intex)
				{
					ServoyLog.logError(intex);
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
		return null;
	}

	public String getStartTitle()
	{
		return "Web client launch";
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
	{
		StartClientHandler.setLastCommand(StartClientHandler.START_WEB_CLIENT);
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
					final IDebugWebClient debugWebClient = Activator.getDefault().getDebugWebClient();
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
								String url = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() +
									"/servoy-webclient/solutions/solution/" + solution.getName();
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

	/**
	 * @return the webBrowser
	 * @throws PartInitException
	 */
	protected IWebBrowser getWebBrowser() throws PartInitException
	{
		return webBrowser == null ? PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser() : webBrowser;
	}

	public void initBrowser(String name)
	{
		String lastUsedBrowser = getLastUsedBrowserName();
		String ewbName = name == null ? lastUsedBrowser : name;
		try
		{
			for (IBrowserDescriptor ewb : BrowserManager.getInstance().getWebBrowsers())
			{
				if (ewb.getName().equals(ewbName))
				{
					IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
					org.eclipse.ui.internal.browser.IBrowserExt ext = null;
					if (ewb != null && !ewb.getName().equals(Messages.prefSystemBrowser))
					{
						//ext := "org.eclipse.ui.browser." + specifiId
						ext = org.eclipse.ui.internal.browser.WebBrowserUIPlugin.findBrowsers(ewb.getLocation());
						if (ext != null)
						{
							webBrowser = ext.createBrowser(ext.getId(), ewb.getLocation(), ewb.getParameters());
							if (webBrowser == null) webBrowser = new ExternalBrowserInstance(ext.getId(), ewb);
						}
						else
						{
							if (ewb.getLocation() != null)
								webBrowser = new ExternalBrowserInstance("org.eclipse.ui.browser." + ewb.getName().toLowerCase().replace(" ", "_"), ewb);
						}
					}

					if (webBrowser == null || ((ewb != null && ewb.getName().equals(Messages.prefSystemBrowser)) &&
						(webBrowser != null && !webBrowser.equals(support.getExternalBrowser()))))
					{

						webBrowser = support.getExternalBrowser();
					}
				}
			}
			if (name != null && !name.equals(lastUsedBrowser)) setLastUsedBrowserName(name);
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}

	private String getLastUsedBrowserName()
	{
		if (eclipsePreferences == null)
		{
			eclipsePreferences = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		}
		return eclipsePreferences.get(LAST_USED_BROWSER, Messages.prefSystemBrowser);
	}

	private void setLastUsedBrowserName(String browserName)
	{
		if (eclipsePreferences == null)
		{
			eclipsePreferences = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		}
		eclipsePreferences.put(LAST_USED_BROWSER, browserName);
		try
		{
			eclipsePreferences.flush();
		}
		catch (BackingStoreException e)
		{
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
		}
	}
}
