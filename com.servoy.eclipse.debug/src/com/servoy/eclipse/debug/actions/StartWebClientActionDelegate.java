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
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.SwingUtilities;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.dltk.debug.ui.DLTKDebugUIPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.internal.browser.BrowserManager;
import org.eclipse.ui.internal.browser.ExternalBrowserInstance;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;
import org.eclipse.ui.internal.browser.Messages;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.debug.FlattenedSolutionDebugListener;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.IDebugWebClient;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;

/**
 * @author jcompagner
 * 
 */
public class StartWebClientActionDelegate extends StartDebugAction implements IRunnableWithProgress, IWorkbenchWindowPulldownDelegate2
{
	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action)
	{
		//make sure the plugins are loaded
		DLTKDebugUIPlugin.getDefault();
		DebugPlugin.getDefault();

		Job job = new Job("Web client start") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				try
				{
					StartWebClientActionDelegate.this.run(monitor);
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
								if (webBrowser == null) webBrowser = support.getExternalBrowser();
								if (browserDescriptor == null) browserDescriptor = BrowserManager.getInstance().getCurrentWebBrowser();
								EditorUtil.openURL(webBrowser, browserDescriptor, url);
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
			if (solution.getSolutionType() == SolutionMetaData.SMART_CLIENT_ONLY || solution.getSolutionType() == SolutionMetaData.MOBILE_MODULE ||
				solution.getSolutionType() == SolutionMetaData.MOBILE) enabled = false;
		}
		else
		{
			enabled = false;
		}
		action.setEnabled(enabled);
	}

	private Menu broswersListMenu;
	private IWebBrowser webBrowser;
	private IBrowserDescriptor browserDescriptor;
	private HashMap<String, Image> browsersImagesList;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWindowPulldownDelegate#getMenu(org.eclipse.swt.widgets.Control)
	 */
	public Menu getMenu(Control parent)
	{
		return sharedGetMenu(parent);
	}

	private Image getImageForName(String name, String location)
	{
		String browserImgFileName = ""; //$NON-NLS-1$
		String browserName = (name != null ? name.toLowerCase() : ""); //$NON-NLS-1$
		String browserLocation = (location != null ? location.toLowerCase() : ""); //$NON-NLS-1$
		if (browserLocation.contains("iexplore") || browserName.contains("explorer")) browserImgFileName = "explorer.png"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		else if (browserLocation.contains("firefox") || browserName.contains("firefox")) browserImgFileName = "firefox.png"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		else if (browserLocation.contains("chrome") || browserName.contains("chrome")) browserImgFileName = "chrome.png"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		else if (browserLocation.contains("safari") || browserName.contains("safari")) browserImgFileName = "safari.png"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		else if (browserLocation.contains("opera") || browserName.contains("opera")) browserImgFileName = "opera.png"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return getImageForBrowser(browserImgFileName);
	}

	private Image getImageForBrowser(String name)
	{
		if (browsersImagesList == null) browsersImagesList = new HashMap<String, Image>();
		if (!name.equals("") && !browsersImagesList.containsKey(name)) //$NON-NLS-1$
		{
			ImageDescriptor id = AbstractUIPlugin.imageDescriptorFromPlugin(com.servoy.eclipse.ui.Activator.PLUGIN_ID, "icons/" + name); //$NON-NLS-1$
			browsersImagesList.put(name, id.createImage());
		}
		return browsersImagesList.get(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.debug.actions.StartDebugAction#dispose()
	 */
	@Override
	public void dispose()
	{
		if (broswersListMenu != null) broswersListMenu.dispose();
		if (browsersImagesList != null)
		{
			Iterator<String> browserNames = browsersImagesList.keySet().iterator();
			while (browserNames.hasNext())
				browsersImagesList.get(browserNames.next()).dispose();
			browsersImagesList.clear();
		}
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWindowPulldownDelegate2#getMenu(org.eclipse.swt.widgets.Menu)
	 */
	public Menu getMenu(Menu parent)
	{
		return sharedGetMenu(parent);

	}

	private Menu sharedGetMenu(Widget parent)
	{
		if (broswersListMenu != null) broswersListMenu.dispose();

		if (parent instanceof Control)
		{
			broswersListMenu = new Menu((Control)parent);
		}
		if (parent instanceof Menu)
		{
			broswersListMenu = new Menu((Menu)parent);
		}
		try
		{
			Iterator iterator = BrowserManager.getInstance().getWebBrowsers().iterator();
			while (iterator.hasNext())
			{
				final IBrowserDescriptor ewb = (IBrowserDescriptor)iterator.next();
				MenuItem menuItem = new MenuItem(broswersListMenu, SWT.PUSH);
				menuItem.setText(ewb.getName());
				menuItem.setImage(getImageForName(ewb.getName(), ewb.getLocation()));
				menuItem.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent e)
					{
						try
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
									if (ewb.getLocation() != null) webBrowser = new ExternalBrowserInstance("org.eclipse.ui.browser." +
										ewb.getName().toLowerCase().replace(" ", "_"), ewb);
								}
							}

							if (webBrowser == null ||
								((ewb != null && ewb.getName().equals(Messages.prefSystemBrowser)) && (webBrowser != null && !webBrowser.equals(support.getExternalBrowser()))))
							{
								webBrowser = support.getExternalBrowser(); //default to system web browser
							}

							browserDescriptor = ewb;
							Job job = new Job("Web client start") //$NON-NLS-1$
							{
								@Override
								protected IStatus run(IProgressMonitor monitor)
								{
									try
									{
										StartWebClientActionDelegate.this.run(monitor);
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
						}
						catch (Exception ex)
						{
							ServoyLog.logError(ex);
						}
					}
				});
			}

		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return broswersListMenu;
	}
}
