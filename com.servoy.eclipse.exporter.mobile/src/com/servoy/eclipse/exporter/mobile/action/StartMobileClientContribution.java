package com.servoy.eclipse.exporter.mobile.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.services.IServiceLocator;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.exporter.mobile.Activator;
import com.servoy.eclipse.exporter.mobile.launch.IMobileLaunchConstants;
import com.servoy.eclipse.exporter.mobile.launch.MobileLaunchConfigurationDelegate;
import com.servoy.eclipse.exporter.mobile.launch.MobileLaunchUtils;
import com.servoy.eclipse.exporter.mobile.launch.test.IMobileTestLaunchConstants;
import com.servoy.eclipse.jsunit.launch.JSUnitLaunchConfigurationDelegate;
import com.servoy.eclipse.model.mobile.exporter.MobileExporter;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Utils;

public class StartMobileClientContribution extends CompoundContributionItem implements IWorkbenchContribution
{
	public static final String ORGANIZE = "Organize...";
	private IServiceLocator mServiceLocator;
	private static final String DASH = " - ";
	private static final String COMMAND_ID = "com.servoy.eclipse.ui.StartMobileClient";

	public StartMobileClientContribution()
	{
	}

	public StartMobileClientContribution(String id)
	{
		super(id);
	}

	@Override
	public void initialize(IServiceLocator serviceLocator)
	{
		this.mServiceLocator = serviceLocator;
	}

	public static ILaunchConfiguration[] getLaunchConfigsForProject(ServoyProject project, boolean defaultLaunchConfigs, String launchConfigurationID)
	{
		if (project == null || project.getSolution().getSolutionType() != SolutionMetaData.MOBILE)
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

		// this will happen anyway at each launch to make sure the right one is used - until we decide to add export war path to the launcher config. page
		// default is "null" and the following code should be used when it's not set to get a value for it
		// File webappsFolder = new File(ApplicationServerSingleton.get().getServoyApplicationServerDirectory(), "server/webapps");
		// workingCopy.setAttribute(IMobileLaunchConstants.WAR_LOCATION, webappsFolder.getAbsolutePath());

		String appUrl = MobileLaunchUtils.getDefaultApplicationURL(
			MobileLaunchUtils.getWarFileName(project.getSolution().getName(),
				launchConfigurationID.equals(IMobileTestLaunchConstants.LAUNCH_TEST_CONFIGURATION_TYPE_ID)),
			ApplicationServerRegistry.get().getWebServerPort());

		workingCopy.setAttribute(IMobileLaunchConstants.SERVER_URL, MobileExporter.getDefaultServerURL());
		workingCopy.setAttribute(IMobileLaunchConstants.SERVICE_SOLUTION, project.getSolutionMetaData().getName() + "_service");
		workingCopy.setAttribute(IMobileLaunchConstants.APPLICATION_URL, appUrl);
		workingCopy.setAttribute(IMobileLaunchConstants.COMPANY_NAME, "");
		workingCopy.setAttribute(IMobileLaunchConstants.LICENSE_CODE, "");
		String browser = MobileLaunchConfigurationDelegate.getBrowser("org.eclipse.ui.browser.chrome") != null ? "org.eclipse.ui.browser.chrome" : "default";
		workingCopy.setAttribute(IMobileLaunchConstants.BROWSER_ID, browser);
		workingCopy.setAttribute(IMobileLaunchConstants.NODEBUG, nodebug ? "true" : "false");

		JSUnitLaunchConfigurationDelegate.prepareLaunchConfigForTesting(workingCopy);

		return workingCopy.doSave();
	}

	public static Collection<ILaunchConfiguration> getAllLaunchConfigurations(String solutionName)
	{
		ServoyProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
		Collection<ILaunchConfiguration> configs = new ArrayList<>();
		configs.addAll(Arrays.asList(getLaunchConfigsForProject(project, true, IMobileLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID)));
		configs.addAll(Arrays.asList(getLaunchConfigsForProject(project, true, IMobileTestLaunchConstants.LAUNCH_TEST_CONFIGURATION_TYPE_ID)));
		configs.addAll(Arrays.asList(getLaunchConfigsForProject(project, false, IMobileLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID)));
		configs.addAll(Arrays.asList(getLaunchConfigsForProject(project, false, IMobileTestLaunchConstants.LAUNCH_TEST_CONFIGURATION_TYPE_ID)));
		return configs;
	}

	public static ILaunchConfiguration getLaunchConfigByName(String solutionName, String name)
	{
		for (ILaunchConfiguration config : getAllLaunchConfigurations(solutionName))
		{
			if (config.getName().equals(name)) return config;
		}
		return null;
	}

	@Override
	protected IContributionItem[] getContributionItems()
	{
		ServoyProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		List<IContributionItem> contributions = new ArrayList<IContributionItem>();

		addContributions(contributions, getLaunchConfigsForProject(project, true, IMobileLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID));
		addContributions(contributions, getLaunchConfigsForProject(project, true, IMobileTestLaunchConstants.LAUNCH_TEST_CONFIGURATION_TYPE_ID));
		addContributions(contributions, getLaunchConfigsForProject(project, false, IMobileLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID));
		addContributions(contributions, getLaunchConfigsForProject(project, false, IMobileTestLaunchConstants.LAUNCH_TEST_CONFIGURATION_TYPE_ID));

		//Organize...
		final CommandContributionItemParameter contributionParameter = new CommandContributionItemParameter(mServiceLocator, null, COMMAND_ID,
			CommandContributionItem.STYLE_PUSH);
		contributionParameter.label = ORGANIZE;
		contributionParameter.visibleEnabled = true;
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("com.servoy.eclipse.mobile.launch.config", ORGANIZE);
		contributionParameter.parameters = parameters;
		CommandContributionItem theContribution = new CommandContributionItem(contributionParameter);
		contributions.add(theContribution);

		return contributions.toArray(new IContributionItem[contributions.size()]);
	}

	private void addContributions(List<IContributionItem> contributions, ILaunchConfiguration[] launchConfigurations)
	{
		for (ILaunchConfiguration config : launchConfigurations)
		{
			final CommandContributionItemParameter contributionParameter = new CommandContributionItemParameter(mServiceLocator, null, COMMAND_ID,
				CommandContributionItem.STYLE_PUSH);
			String label = config.getName().split(DASH)[0].trim();
			try
			{
				if (Boolean.valueOf(config.getAttribute("nodebug", "true")) == Boolean.FALSE) label = label.replace("Run", "Debug");
			}
			catch (CoreException e)
			{
				ServoyLog.logError("Could not get launch configuration 'nodebug' attribute value.", e);
			}
			if (label.length() > 40)
			{
				//if it's too long, limit it to max 40 chars
				label = label.substring(0, 37);
				label += "...";
			}
			contributionParameter.label = label;
			contributionParameter.visibleEnabled = true;
			contributionParameter.icon = getIcon(config);

			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("com.servoy.eclipse.mobile.launch.config", config.getName());
			contributionParameter.parameters = parameters;

			CommandContributionItem theContribution = new CommandContributionItem(contributionParameter);
			contributions.add(theContribution);
		}
		if (launchConfigurations.length > 0) contributions.add(new Separator());
	}

	private ImageDescriptor getIcon(ILaunchConfiguration config)
	{
		String imageName = null;
		if (config.getName().contains("mobile client"))
		{
			imageName = "icons/launch_mobile.png";
		}
		if (config.getName().contains("JS Unit"))
		{
			imageName = "icons/jsunit.png";
		}
		return imageName == null ? null : AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, imageName);
	}
}
