package com.servoy.eclipse.debug.actions;


/**
 *  The webclient toolbar menu item.
 *
 * @author jcompagner
 * @since 8.2
 */
public class LaunchWebClientContributionFactory extends LaunchClientContributionFactory
{
	public LaunchWebClientContributionFactory()
	{
		super("Launch Web Client", "com.servoy.eclipse.ui.StartWebClient", "com.servoy.eclipse.core.webClientState", "icons/launch_web.png");
	}
}
