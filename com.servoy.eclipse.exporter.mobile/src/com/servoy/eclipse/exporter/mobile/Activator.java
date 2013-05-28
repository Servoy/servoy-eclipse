package com.servoy.eclipse.exporter.mobile;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.servoy.eclipse.core.DesignApplication;
import com.servoy.j2db.IApplication;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin
{

	// The plug-in ID
	public static final String PLUGIN_ID = "com.servoy.eclipse.exporter.mobile"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception
	{
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception
	{
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault()
	{
		return plugin;
	}

	private IApplication exportClient;

	public IApplication getExportClient()
	{
		if (exportClient == null)
		{
			exportClient = new DesignApplication()
			{
				@Override
				public String getI18NMessage(String i18nKey)
				{
					return i18nKey;
				}

				@Override
				public String getI18NMessage(String i18nKey, Object[] array)
				{
					return i18nKey;
				}

				@Override
				public String getI18NMessageIfPrefixed(String i18nKey)
				{
					return i18nKey;
				}
			};
		}
		return exportClient;
	}

}
