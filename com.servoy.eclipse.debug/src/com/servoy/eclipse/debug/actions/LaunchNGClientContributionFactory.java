package com.servoy.eclipse.debug.actions;


/**
 *  The ngclient toolbar menu item.
 *
 * @author jcompagner
 * @since 8.2
 */
public class LaunchNGClientContributionFactory extends LaunchClientContributionFactory
{
	public LaunchNGClientContributionFactory()
	{
		super("Launch NG Client", "com.servoy.eclipse.ui.StartNGClient", "com.servoy.eclipse.core.ngClientState", "icons/launch_ng.png");
	}
}
