package com.servoy.eclipse.aibridge;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;

import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.servoy.eclipse.mcp.ai.ServoyEmbeddingService;
import com.servoy.eclipse.model.util.ServoyLog;
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

		// Pre-load embedding service and knowledge base in background
		initializeEmbeddingService();
	}

	/**
	 * Initialize the embedding service asynchronously to pre-load knowledge base
	 */
	private void initializeEmbeddingService()
	{
		Job job = new Job("Loading Servoy AI Knowledge Base")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				try
				{
					monitor.beginTask("Initializing embedding service...", IProgressMonitor.UNKNOWN);
					ServoyLog.logInfo("[Activator] Pre-loading Servoy AI knowledge base...");

					// This will trigger singleton initialization and load the knowledge base
					ServoyEmbeddingService.getInstance();

					return Status.OK_STATUS;
				}
				catch (Exception e)
				{
					ServoyLog.logError("[Activator] Failed to pre-load knowledge base: " + e.getMessage());
					e.printStackTrace();
					// Don't fail plugin startup if embedding service fails
					return Status.OK_STATUS;
				}
				finally
				{
					monitor.done();
				}
			}
		};

		// Run as system job (won't show in progress view unless it takes long)
		job.setSystem(true);
		job.schedule();
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
