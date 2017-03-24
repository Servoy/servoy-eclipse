package com.servoy.eclipse.ui.tweaks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;

public class Activator extends AbstractUIPlugin
{

	/**
	 * The PLUGIN_ID for com.servoy.eclipse.ui.tweaks
	 */
	public static final String PLUGIN_ID = "com.servoy.eclipse.ui.tweaks";

	// The shared instance
	private static Activator plugin;

	private final Properties initProperties = new Properties();

	@Override
	public void start(BundleContext context) throws Exception
	{
		ModelUtils.assertUINotDisabled(PLUGIN_ID);

		super.start(context);
		plugin = this;

		URL initPropertiesFile = Activator.getDefault().getBundle().getResource("init.properties");
		if (initPropertiesFile != null)
		{
			try (InputStream in = initPropertiesFile.openStream())
			{
				initProperties.load(in);
			}
			catch (IOException e)
			{
				ServoyLog.logWarning("Error reading initial properties for tweak ui plugin. Using defaults.", e);
			}
		}
	}

	public Properties getInitProperties()
	{
		return initProperties;
	}

	@Override
	public void stop(BundleContext context) throws Exception
	{
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault()
	{
		return plugin;
	}

}
