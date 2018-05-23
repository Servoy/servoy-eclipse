package com.servoy.eclipse.ngclient.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.BundleContext;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ngclient.ui.utils.ZipUtils;

public class Activator extends Plugin
{
	private final String PLUGIN_ID = "com.servoy.eclipse.ngclient.ui";
	private final String NODEJS_EXTENSION = "nodejs";

	// The shared instance
	private static Activator plugin;

	private File npmPath;
	private Job extractingNode;
	private RunNPMCommand buildCommand;
	private File nodeFolder;

	public static Activator getInstance()
	{
		return plugin;
	}

	@Override
	public void start(BundleContext context) throws Exception
	{
		plugin = this;
		synchronized (plugin)
		{
			extractingNode = new Job("extracting nodejs")
			{

				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					IExtensionRegistry registry = Platform.getExtensionRegistry();
					IConfigurationElement[] cf = registry.getConfigurationElementsFor(PLUGIN_ID, NODEJS_EXTENSION);
					File npm = null;
					if (cf.length > 0)
					{
						npm = extractNodeJS(cf[0]);
					}
					synchronized (plugin)
					{
						npmPath = npm;
						extractingNode = null;
						plugin.notifyAll();
					}
					return Status.OK_STATUS;
				}
			};
		}
		extractingNode.schedule();

		File stateLocation = Activator.getInstance().getStateLocation().toFile();
		this.nodeFolder = new File(stateLocation, "target");

		new NodeFolderCreatorJob(this.nodeFolder).schedule();
	}

	/**
	 * @return the nodeFolder
	 */
	public File getNodeFolder()
	{
		return nodeFolder;
	}

	/**
	 * @param cf
	 */
	private static File extractNodeJS(IConfigurationElement element)
	{
		String pluginId = element.getNamespaceIdentifier();
		String path = element.getAttribute("path");
		IPath stateLocation = plugin.getStateLocation();
		File baseDir = stateLocation.toFile();
		File npm = new File(baseDir, path);
		if (!npm.exists())
		{
			String zip = element.getAttribute("zip");
			URL zipUrl = Platform.getBundle(pluginId).getResource(zip);
			if (zipUrl != null)
			{
				try
				{
					if (ZipUtils.isZipFile(zipUrl))
					{
						ZipUtils.extractZip(zipUrl, baseDir);
					}
					else if (ZipUtils.isTarGZFile(zipUrl))
					{
						ZipUtils.extractTarGZ(zipUrl, baseDir);
					}
					else if (ZipUtils.isTarXZFile(zipUrl))
					{
						ZipUtils.extractTarXZ(zipUrl, baseDir);
					}
				}
				catch (IOException e)
				{
					ServoyLog.logError(e);
				}

				if (npm.exists())
				{
					npm.setExecutable(true);
				}
			}
			else
			{
				ServoyLog.logWarning("couldn't create nodejs install from plugin " + pluginId + " and zip: " + zip, null);
				return null;
			}
		}
		return npm;
	}

	public void executeNPMInstall()
	{
		waitFormNodeExtraction();
		RunNPMCommand installCommand = new RunNPMCommand(npmPath, nodeFolder, "install");
		installCommand.setUser(false);
		createBuildCommand();
		installCommand.setNextJob(buildCommand);
		installCommand.schedule();

	}

	public void executeNPMBuild()
	{
		if (buildCommand != null) return; // already started?
		waitFormNodeExtraction();
		createBuildCommand();
		buildCommand.schedule();
	}

	/**
	 *
	 */
	private void createBuildCommand()
	{
		buildCommand = new RunNPMCommand(npmPath, nodeFolder, "run-script build_debug --scripts-prepend-node-path");
		buildCommand.setUser(false);
		buildCommand.setSystem(true);
	}

	/**
	 *
	 */
	private void waitFormNodeExtraction()
	{
		synchronized (plugin)
		{
			while (extractingNode != null)
			{
				try
				{
					plugin.wait();
				}
				catch (InterruptedException e)
				{
				}
			}
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception
	{
		if (buildCommand != null) buildCommand.cancel();
	}
}
