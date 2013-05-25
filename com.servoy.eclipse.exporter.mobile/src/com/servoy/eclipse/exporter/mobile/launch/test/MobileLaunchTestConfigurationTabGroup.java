package com.servoy.eclipse.exporter.mobile.launch.test;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

import com.servoy.eclipse.exporter.mobile.action.StartMobileClientActionDelegate;
import com.servoy.eclipse.exporter.mobile.launch.MobileLaunchConfigurationTab;

public class MobileLaunchTestConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup
{

	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode)
	{
		setTabs(new ILaunchConfigurationTab[] { new MobileTestLaunchConfigurationTab(), new MobileLaunchConfigurationTab()
		{

			@Override
			protected String getDefaultApplicationURL()
			{
				return StartMobileClientActionDelegate.getDefaultApplicationURL(true);
			}

		}, new CommonTab() }); // added "common" so that the launch configuration can be shared; although the other attributes are not used
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration)
	{
		if (configuration.isDirty()) configuration.removeAttribute(IMobileTestLaunchConstants.AUTO_GENERATED);
		super.performApply(configuration);
	}

}
