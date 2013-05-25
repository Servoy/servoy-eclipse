package com.servoy.eclipse.exporter.mobile.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

public class MobileLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup
{

	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode)
	{
		setTabs(new ILaunchConfigurationTab[] { new MobileLaunchConfigurationTab(), new CommonTab() }); // added "common" so that the launch configuration can be shared; although the other attributes are not used
	}

}