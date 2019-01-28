package com.servoy.eclipse.solution.integration;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

	/**
	 * The PLUGIN_ID for com.servoy.eclipse.solution.integration
	 */
	public static final String PLUGIN_ID = "com.servoy.eclipse.solution.integration";

	// The shared instance
	private static Activator plugin;
	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}
	
	public static Activator getInstance() {
		return plugin;
	}
}
