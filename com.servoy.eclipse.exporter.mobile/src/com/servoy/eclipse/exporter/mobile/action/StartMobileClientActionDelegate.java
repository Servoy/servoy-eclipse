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
package com.servoy.eclipse.exporter.mobile.action;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.exporter.mobile.launch.IMobileLaunchConstants;
import com.servoy.eclipse.exporter.mobile.launch.MobileLaunchConfigurationDelegate;
import com.servoy.eclipse.exporter.mobile.launch.MobileLaunchUtils;
import com.servoy.eclipse.exporter.mobile.launch.test.IMobileTestLaunchConstants;
import com.servoy.eclipse.jsunit.launch.JSUnitLaunchConfigurationDelegate;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * 
 */
public class StartMobileClientActionDelegate implements IWorkbenchWindowPulldownDelegate2
{

	private static final String DASH = " - ";

	IWorkbenchWindow window = null;
	ServoyProject activeProject = null;

	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action)
	{
		//make sure the plugins are loaded
		DebugPlugin.getDefault();
		StartMobileClientActionDelegate.this.run();
	}

	public void run()
	{
		DebugUITools.launch(getCurrentLaunchConfig(), ILaunchManager.RUN_MODE);
	}

	private ILaunchConfiguration getCurrentLaunchConfig()
	{
		if (configToLaunch == null)
		{// initialize button push action
			ILaunchConfiguration[] defaultCfgs = getLaunchConfigsForCurrentProject(true, IMobileLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);
			configToLaunch = defaultCfgs[0];
		}
		return configToLaunch;
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection)
	{
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		final ServoyProject activeProject = servoyModel.getActiveProject();
		boolean enabled = false;
		if (activeProject != null && activeProject.getSolution() != null)
		{
			final Solution solution = activeProject.getSolution();
			this.activeProject = activeProject;
			if (solution.getSolutionType() == SolutionMetaData.MOBILE) enabled = true;
		}
		else
		{
			enabled = false;
		}
		action.setEnabled(enabled);
	}

	private Menu configsListMenu;
	private ILaunchConfiguration configToLaunch;
	private HashMap<String, Image> browsersImagesList;

	public Menu getMenu(Control parent)
	{
		return sharedGetMenu(parent);
	}

	private Image getImageForName(String name)
	{
		String browserImgFileName = ""; //$NON-NLS-1$
		String browserName = (name != null ? name.toLowerCase() : ""); //$NON-NLS-1$
		if (browserName.contains("ie")) browserImgFileName = "explorer.png"; //$NON-NLS-1$ //$NON-NLS-2$ 
		else if (browserName.contains("firefox")) browserImgFileName = "firefox.png"; //$NON-NLS-1$ //$NON-NLS-2$ 
		else if (browserName.contains("chrome")) browserImgFileName = "chrome.png"; //$NON-NLS-1$ //$NON-NLS-2$ 
		else if (browserName.contains("safari")) browserImgFileName = "safari.png"; //$NON-NLS-1$ //$NON-NLS-2$ 
		else if (browserName.contains("opera")) browserImgFileName = "opera.png"; //$NON-NLS-1$ //$NON-NLS-2$  
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
		if (configsListMenu != null) configsListMenu.dispose();
		if (browsersImagesList != null)
		{
			Iterator<String> browserNames = browsersImagesList.keySet().iterator();
			while (browserNames.hasNext())
				browsersImagesList.get(browserNames.next()).dispose();
			browsersImagesList.clear();
		}
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
		if (configsListMenu != null) configsListMenu.dispose();

		if (parent instanceof Control)
		{
			configsListMenu = new Menu((Control)parent);
		}
		if (parent instanceof Menu)
		{
			configsListMenu = new Menu((Menu)parent);
		}
		try
		{

			SelectionAdapter selectionAdapter = new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					try
					{
						configToLaunch = (ILaunchConfiguration)(((MenuItem)e.getSource()).getData());
						StartMobileClientActionDelegate.this.run();
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
				}
			};
			ILaunchConfiguration[] configs = getLaunchConfigsForCurrentProject(true, IMobileLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);
			addLaunchConfigurations(selectionAdapter, configs, true);
			if (configs.length > 0) new MenuItem(configsListMenu, SWT.SEPARATOR);

			configs = getLaunchConfigsForCurrentProject(true, IMobileTestLaunchConstants.LAUNCH_TEST_CONFIGURATION_TYPE_ID);
			addLaunchConfigurations(selectionAdapter, configs, true);
			if (configs.length > 0) new MenuItem(configsListMenu, SWT.SEPARATOR);

			configs = getLaunchConfigsForCurrentProject(false, IMobileLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);
			addLaunchConfigurations(selectionAdapter, configs, false);
			if (configs.length > 0) new MenuItem(configsListMenu, SWT.SEPARATOR);

			configs = getLaunchConfigsForCurrentProject(false, IMobileTestLaunchConstants.LAUNCH_TEST_CONFIGURATION_TYPE_ID);
			addLaunchConfigurations(selectionAdapter, configs, false);
			if (configs.length > 0) new MenuItem(configsListMenu, SWT.SEPARATOR);

			// open launch preference page
			MenuItem menuItem = new MenuItem(configsListMenu, SWT.PUSH);
			menuItem.setText("Organize ...");
			menuItem.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					configToLaunch = null;
					String groupId = DebugUITools.getLaunchGroup(getCurrentLaunchConfig(), "run").getIdentifier();
					try
					{
						DebugUITools.openLaunchConfigurationDialogOnGroup(window.getShell(), new StructuredSelection(), groupId);
					}
					catch (Exception ex)
					{
						/*
						 * in linux it throws a null pointer exception : java.lang.NullPointerException at
						 * org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsDialog.close(LaunchConfigurationsDialog.java:350)
						 */
						ServoyLog.logError(ex);
					}
				}
			});

		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return configsListMenu;
	}

	private void addLaunchConfigurations(SelectionAdapter selectionAdapter, ILaunchConfiguration[] configs, boolean defaults) throws CoreException
	{
		for (ILaunchConfiguration cfg : configs)
		{
			MenuItem menuItem = new MenuItem(configsListMenu, SWT.PUSH);
			menuItem.setText(defaults ? cfg.getName().replace(DASH + activeProject.getSolution().getName(), "") : cfg.getName());
			menuItem.setData(cfg);
			Image img = getImageForName(cfg.getAttribute(IMobileLaunchConstants.BROWSER_ID, ""));
			if (img != null) menuItem.setImage(img);
			menuItem.addSelectionListener(selectionAdapter);
		}
	}

	private ILaunchConfiguration[] getLaunchConfigsForCurrentProject(boolean defaultLaunchConfigs, String launchConfigurationID)
	{
		return getLaunchConfigsForProject(activeProject, defaultLaunchConfigs, launchConfigurationID);
	}

	public static ILaunchConfiguration[] getLaunchConfigsForProject(ServoyProject project, boolean defaultLaunchConfigs, String launchConfigurationID)
	{
		if (project.getSolution().getSolutionType() != SolutionMetaData.MOBILE)
		{
			return new ILaunchConfiguration[0];
		}
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ArrayList<ILaunchConfiguration> activeProjectCfgs = new ArrayList<ILaunchConfiguration>();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(launchConfigurationID);
		ILaunchConfiguration[] configurations = null;
		try
		{
			configurations = manager.getLaunchConfigurations(type);
			if (defaultLaunchConfigs)
			{
				ILaunchConfiguration nodebug = null, debug = null;
				for (ILaunchConfiguration config : configurations)
				{
					if (config.getAttribute(IMobileLaunchConstants.SOLUTION_NAME, "").equals(project.getSolution().getName()) &&
						isDefaultConfigName(config, launchConfigurationID, project.getSolution().getName()))
					{
						if (config.getAttribute(IMobileLaunchConstants.NODEBUG, "true").equals("true"))
						{
							nodebug = config;
						}
						else
						{
							debug = config;
						}
					}
				}

				// create missing default configurations if necessary
				if (nodebug == null) nodebug = createDefaultLaunchConfig(project, true, launchConfigurationID);
				if (debug == null) debug = createDefaultLaunchConfig(project, false, launchConfigurationID);

				activeProjectCfgs.add(nodebug);
				activeProjectCfgs.add(debug);
			}
			else
			{ // custom configs
				for (ILaunchConfiguration config : configurations)
				{
					if (config.getAttribute(IMobileLaunchConstants.SOLUTION_NAME, "").equals(project.getSolution().getName()) &&
						!isDefaultConfigName(config, launchConfigurationID, project.getSolution().getName()))
					{
						activeProjectCfgs.add(config);
					}
				}

			}

		}
		catch (CoreException e)
		{
			ServoyLog.logError("cannot get default mobile Launch configurations for currrent project", e);
		}

		return Utils.asArray(activeProjectCfgs.iterator(), ILaunchConfiguration.class);
	}

	private static boolean isDefaultConfigName(ILaunchConfiguration config, String launchConfigurationID, String solutionName) throws CoreException
	{
		String defaultName = getDefaultConfigName(solutionName, launchConfigurationID,
			"true".equals(config.getAttribute(IMobileLaunchConstants.NODEBUG, "true")));
		return config.getName().startsWith(defaultName) &&
			(config.getName().length() == defaultName.length() || (config.getName().substring(defaultName.length()).matches("_\\p{Digit}*")));
	}

	public static String getDefaultConfigName(String solutionName, String launchConfigurationID, boolean nodebug)
	{
		return (launchConfigurationID.equals(IMobileLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID) ? "Run mobile client" : "Run mobile JS Unit Tests") + DASH +
			solutionName + (nodebug ? "" : " and activate service solution (for debug)");
	}

	public static ILaunchConfiguration createDefaultLaunchConfig(ServoyProject project, boolean nodebug, String launchConfigurationID) throws CoreException
	{
		return createDefaultLaunchConfig(project, nodebug, launchConfigurationID,
			getDefaultConfigName(project.getSolution().getName(), launchConfigurationID, nodebug));
	}

	public static ILaunchConfiguration createDefaultLaunchConfig(ServoyProject project, boolean nodebug, String launchConfigurationID, String launchConfigName)
		throws CoreException
	{
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(launchConfigurationID);

		String initialName = launchConfigName;
		int i = 0;
		while (manager.isExistingLaunchConfigurationName(launchConfigName))
		{
			// another launch config with this name but different type (can't be used as default) exists; generate another name
			launchConfigName = initialName + " _" + (++i);
		}

		ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, launchConfigName);

		workingCopy.setAttribute(IMobileLaunchConstants.SOLUTION_NAME, project.getSolution().getName());
		File webappsFolder = new File(ApplicationServerSingleton.get().getServoyApplicationServerDirectory(), "server/webapps");

		workingCopy.setAttribute(IMobileLaunchConstants.WAR_LOCATION, webappsFolder.getAbsolutePath());
		String appUrl = MobileLaunchUtils.getDefaultApplicationURL(
			MobileLaunchUtils.getWarFileName(project.getSolution().getName(),
				launchConfigurationID.equals(IMobileTestLaunchConstants.LAUNCH_TEST_CONFIGURATION_TYPE_ID)),
			ApplicationServerSingleton.get().getWebServerPort());

		workingCopy.setAttribute(IMobileLaunchConstants.SERVER_URL, IMobileLaunchConstants.DEFAULT_SERVICE_URL);
		workingCopy.setAttribute(IMobileLaunchConstants.APPLICATION_URL, appUrl);
		workingCopy.setAttribute("company", "");
		workingCopy.setAttribute("license", "");
		String browser = MobileLaunchConfigurationDelegate.getBrowser("org.eclipse.ui.browser.chrome") != null ? "org.eclipse.ui.browser.chrome" : "default";
		workingCopy.setAttribute(IMobileLaunchConstants.BROWSER_ID, browser);
		workingCopy.setAttribute(IMobileLaunchConstants.NODEBUG, nodebug ? "true" : "false");

		JSUnitLaunchConfigurationDelegate.prepareLaunchConfigForTesting(workingCopy);

		return workingCopy.doSave();
	}

	public static String getDefaultApplicationURL(boolean testURL)
	{
		String solutionName = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution().getName();
		return MobileLaunchUtils.getDefaultApplicationURL(MobileLaunchUtils.getWarFileName(solutionName, testURL),
			ApplicationServerSingleton.get().getWebServerPort());
	}

	public void init(IWorkbenchWindow window)
	{
		this.window = window;
	}

}
