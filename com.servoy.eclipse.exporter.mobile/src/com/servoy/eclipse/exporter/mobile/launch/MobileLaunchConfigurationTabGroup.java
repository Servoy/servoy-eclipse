package com.servoy.eclipse.exporter.mobile.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

public class MobileLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup
{


	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode)
	{

		MobileLaunchConfigurationTab main = new MobileLaunchConfigurationTab();
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { main };
		setTabs(tabs);
	}
}