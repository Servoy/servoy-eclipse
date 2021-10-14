package com.servoy.eclipse.ngclient.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.osgi.framework.BundleContext;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.ngclient.ui.utils.NGClientConstants;
import com.servoy.eclipse.ngclient.ui.utils.ZipUtils;

public class Activator extends Plugin
{
	private final String PLUGIN_ID = "com.servoy.eclipse.ngclient.ui";
	private final String NODEJS_EXTENSION = "nodejs";

	private final CountDownLatch nodeReady = new CountDownLatch(2);

	// The shared instance
	private static Activator plugin;

	private File nodePath;
	private File npmPath;
	private RunNPMCommand buildCommand;
	private File projectFolder;
	private IConsole console;


	public static Activator getInstance()
	{
		return plugin;
	}

	@Override
	public void start(BundleContext context) throws Exception
	{
		plugin = this;
		com.servoy.eclipse.model.Activator.getDefault().setNG2WarExporter(WebPackagesListener::exportNG2ToWar);
		File stateLocation = Activator.getInstance().getStateLocation().toFile();
		this.projectFolder = new File(stateLocation, "target");
//		new DistFolderCreatorJob(projectFolder, true).schedule();
		extractNode();
	}

	public synchronized IConsole getConsole()
	{
		if (console == null)
		{
			URL imageUrl = Activator.getInstance().getBundle().getEntry("/images/npmconsole.png");
			EclipseIOConsole eclipseConsole = new EclipseIOConsole("NG2 Build Console", "ng2console", ImageDescriptor.createFromURL(imageUrl));
			IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
			consoleManager.addConsoles(new IOConsole[] { eclipseConsole });
			consoleManager.showConsoleView(eclipseConsole);
			console = eclipseConsole;
		}
		return console;
	}

	public synchronized void setConsole(IConsole console)
	{
		this.console = console;
	}

	void countDown()
	{
		if (nodeReady.getCount() > 0)
		{
			nodeReady.countDown();
			if (nodeReady.getCount() == -0)
			{
				ServoyModelFinder.getServoyModel().getNGPackageManager().addLoadedNGPackagesListener(new WebPackagesListener());
			}
		}
	}

	public void copyNodeFolder()
	{
		new NodeFolderCreatorJob(this.projectFolder, true, false).schedule();
	}

	private String getSystemOrEvironmentProperty(String propertyName)
	{
		String value = System.getProperty(propertyName);
		if (value == null)
		{
			value = System.getenv(propertyName);
		}
		return value;
	}

	private void extractNode()
	{
		String nodePth = getSystemOrEvironmentProperty("servoy.nodePath");
		String npmPth = getSystemOrEvironmentProperty("servoy.npmPath");
		if (nodePth != null && npmPth != null)
		{
			nodePath = new File(nodePth);
			npmPath = new File(npmPth);
			countDown();
		}
		else
		{
			Job extractingNode = new Job("extracting nodejs")
			{

				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					IExtensionRegistry registry = Platform.getExtensionRegistry();
					IConfigurationElement[] cf = registry.getConfigurationElementsFor(PLUGIN_ID, NODEJS_EXTENSION);
					File node = null;
					File npm = null;
					if (cf.length > 0)
					{
						node = extractPath(cf[0], "nodePath");
						node.setExecutable(true);
						npm = extractPath(cf[0], "npmPath");
					}
					nodePath = node;
					npmPath = npm;
					countDown();
					return Status.OK_STATUS;
				}
			};
			extractingNode.schedule();
		}
	}

	/**
	 * @return the projectFolder
	 */
	public File getProjectFolder()
	{
		return projectFolder;
	}

	private static File extractPath(IConfigurationElement element, String attribute)
	{
		String pluginId = element.getNamespaceIdentifier();
		String path = element.getAttribute(attribute);
		IPath stateLocation = plugin.getStateLocation();
		File baseDir = stateLocation.toFile();
		File file = new File(baseDir, path);
		if (!file.exists())
		{
			String archive = element.getAttribute("archive");
			URL archiveUrl = Platform.getBundle(pluginId).getResource(archive);
			if (archiveUrl != null)
			{
				try
				{
					if (ZipUtils.isZipFile(archiveUrl))
					{
						ZipUtils.extractZip(archiveUrl, baseDir);
					}
					else if (ZipUtils.isTarGZFile(archiveUrl))
					{
						ZipUtils.extractTarGZ(archiveUrl, baseDir);
					}
					else if (ZipUtils.isTarXZFile(archiveUrl))
					{
						ZipUtils.extractTarXZ(archiveUrl, baseDir);
					}
				}
				catch (IOException e)
				{
					getInstance().getLog().error("Error extracting path from " + archiveUrl, e);
				}
			}
			else
			{
				getInstance().getLog().info("couldn't create nodejs install from plugin " + pluginId + " and archive: " + archive);
				return null;
			}
		}
		return file;
	}

	public RunNPMCommand createNPMCommand(List<String> commandArguments)
	{
		waitForNodeExtraction();
		return new RunNPMCommand(nodePath, npmPath, projectFolder, commandArguments);
	}

	public void executeNPMInstall()
	{
		RunNPMCommand installCommand = createNPMCommand(NGClientConstants.NPM_INSTALL);
		installCommand.setUser(false);
		createBuildCommand();
		installCommand.setNextJob(buildCommand);
		installCommand.schedule();

	}

	public void executeNPMBuild()
	{
		if (buildCommand != null) return; // already started?
		waitForNodeExtraction();
		createBuildCommand();
		buildCommand.schedule();
	}

	/**
	 *
	 */
	private void createBuildCommand()
	{
		buildCommand = new RunNPMCommand(NGClientConstants.NPM_BUILD_JOB, nodePath, npmPath, projectFolder, NGClientConstants.NG_BUILD_COMMAND);
		buildCommand.setUser(false);
		buildCommand.setSystem(true);
	}

	void waitForNodeExtraction()
	{
		try
		{
			nodeReady.await();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception
	{
		if (buildCommand != null) buildCommand.cancel();
	}
}
