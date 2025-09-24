package com.servoy.eclipse.aibridge;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;

import com.microsoft.copilot.eclipse.ui.CopilotUi;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin
{

	// The plug-in ID
	public static final String PLUGIN_ID = "com.servoy.eclipse.aibridge"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator()
	{
		super();
	}

	@Override
	public void start(BundleContext context) throws Exception
	{
		super.start(context);
		plugin = this;
		String mcpServers = CopilotUi.getPlugin().getPreferenceStore().getString("mcp");
		boolean addServoyServer = mcpServers == null || mcpServers.trim().length() == 0;
		JSONObject json = null;
		if (!addServoyServer)
		{
			json = new JSONObject(mcpServers);
			if (json.has("servers") && json.getJSONObject("servers").has("servoy"))
			{
				addServoyServer = false;
			}
		}
		if (addServoyServer)
		{
			JSONObject servers = null;
			if (json == null) json = new JSONObject();
			if (!json.has("servers")) json.put("servers", servers = new JSONObject());
			else servers = json.getJSONObject("servers");

			JSONObject servoy = new JSONObject();
			servoy.put("url", "http://localhost:8183/mcp");

			servers.put("servoy", servoy);

			CopilotUi.getPlugin().getPreferenceStore().putValue("mcp", json.toString());
		}

	}

	@Override
	public void stop(BundleContext context) throws Exception
	{
		AiBridgeManager.getInstance().saveData(AiBridgeView.getSolutionName());
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

}
