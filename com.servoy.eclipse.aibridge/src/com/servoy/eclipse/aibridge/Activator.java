package com.servoy.eclipse.aibridge;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;

import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin
{

	public static final String PLUGIN_ID = "com.servoy.eclipse.aibridge"; //$NON-NLS-1$

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
		JSONObject json = null;
		if (mcpServers != null && mcpServers.isBlank())
		{
			try
			{
				json = new JSONObject(mcpServers);
			}
			catch (Exception e)
			{
				// ignore invalid json
			}
		}
		JSONObject servers = null;
		if (json == null) json = new JSONObject();
		if (!json.has("servers")) json.put("servers", servers = new JSONObject());
		else servers = json.getJSONObject("servers");

		JSONObject servoy = new JSONObject();
		servoy.put("url", "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/mcp");

		servers.put("servoy", servoy);

		CopilotUi.getPlugin().getPreferenceStore().putValue("mcp", json.toString(1));

		// Knowledge base initialization is handled by com.servoy.eclipse.knowledgebase plugin
		// The knowledgebase plugin manages:
		// - Embedding service initialization
		// - Loading knowledge bases from SPM packages
		// - Solution activation triggers
		// See: KnowledgeBaseManager in the knowledgebase plugin
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
