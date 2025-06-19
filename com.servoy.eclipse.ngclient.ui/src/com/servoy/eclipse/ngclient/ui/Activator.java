package com.servoy.eclipse.ngclient.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
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
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ngclient.ui.utils.ZipUtils;

public class Activator extends Plugin
{
	private final String PLUGIN_ID = "com.servoy.eclipse.ngclient.ui";
	private final String NODEJS_EXTENSION = "nodejs";
	private final static String NG2_FOLDER = "target";

	private final CountDownLatch nodeReady = new CountDownLatch(1);

	// The shared instance
	private static Activator plugin;

	private File nodePath;
	private File npmPath;
	private RunNPMCommand buildCommand;
	private File mainTargetFolder;
	private File solutionProjectFolder;
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

		String targetFolder = getSystemOrEvironmentProperty("servoy.ng2.target.folder");
		if (targetFolder != null)
		{
			this.mainTargetFolder = new File(targetFolder).getCanonicalFile();
		}
		else
		{
			File stateLocation = Activator.getInstance().getStateLocation().toFile();
			this.mainTargetFolder = new File(stateLocation, NG2_FOLDER);
		}
//		new DistFolderCreatorJob(projectFolder, true).schedule();
//		extractNode();
	}

	public synchronized IConsole getConsole()
	{
		if (console == null)
		{
			URL imageUrl = Activator.getInstance().getBundle().getEntry("/images/npmconsole.png");
			EclipseIOConsole eclipseConsole = new EclipseIOConsole("Titanium NG Build Console", "ng2console", ImageDescriptor.createFromURL(imageUrl));
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
			if (nodeReady.getCount() == 0 && ServoyModelFinder.getServoyModel() != null && ServoyModelFinder.getServoyModel().getNGPackageManager() != null)
			{
				ServoyModelFinder.getServoyModel().getNGPackageManager().addLoadedNGPackagesListener(new WebPackagesListener());
			}
		}
	}

	public void setActiveSolution(String solutionName)
	{
		if (solutionName == null)
		{
			solutionProjectFolder = null;
		}
		else
		{
			this.solutionProjectFolder = new File(mainTargetFolder, solutionName);
		}
	}

	private String getSystemOrEvironmentProperty(String propertyName)
	{
		String value = System.getProperty(propertyName);
		if (value == null)
		{
			value = System.getenv(propertyName);
		}
		return value != null ? Paths.get(StringEscapeUtils.escapeJava(value)).normalize().toString() : null;
	}

	public void extractNode()
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
						node = extractPath(cf[0], "nodePath", true);
						node.setExecutable(true);
						npm = extractPath(cf[0], "npmPath", false);
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
	public File getSolutionProjectFolder()
	{
		return solutionProjectFolder;
	}

	/**
	 * @return the projectFolder
	 */
	public File getMainTargetFolder()
	{
		return mainTargetFolder;
	}

	private static File extractPath(IConfigurationElement element, String attribute, boolean deletePreviousPaths)
	{
		String pluginId = element.getNamespaceIdentifier();
		String path = element.getAttribute(attribute);
		IPath stateLocation = plugin.getStateLocation();
		File baseDir = stateLocation.toFile();
		File file = new File(baseDir, path);
		File fullyGenerated = new File(baseDir, ".fullygenerated");
		if (!file.exists() || !fullyGenerated.exists())
		{
			String archive = element.getAttribute("archive");
			URL archiveUrl = Platform.getBundle(pluginId).getResource(archive);
			if (archiveUrl != null)
			{
				if (deletePreviousPaths || !fullyGenerated.exists())
				{
					if (fullyGenerated.exists())
					{
						fullyGenerated.delete();
					}
					File[] dirs = baseDir.listFiles(oldFile -> oldFile.isDirectory() && !oldFile.getName().equals(NG2_FOLDER));
					if (dirs != null)
					{
						for (File oldDir : dirs)
						{
							try
							{
								FileUtils.deleteDirectory(oldDir);
							}
							catch (IOException e)
							{
								getInstance().getLog().error("Error deleting old node install path:" + oldDir.getAbsolutePath(), e);
							}
						}
					}
				}
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
					if (!fullyGenerated.exists())
					{
						fullyGenerated.createNewFile();
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

	public RunNPMCommand createNPMCommand(File folder, List<String> commandArguments)
	{
		waitForNodeExtraction();
		return new RunNPMCommand(nodePath, npmPath, folder, commandArguments);
	}

	/*
	 * public void executeNPMInstall() { RunNPMCommand installCommand = createNPMCommand(NGClientConstants.NPM_INSTALL); installCommand.setUser(false);
	 * createBuildCommand(); installCommand.setNextJob(buildCommand); installCommand.schedule();
	 *
	 * }
	 *
	 * public void executeNPMBuild() { if (buildCommand != null) return; // already started? waitForNodeExtraction(); createBuildCommand();
	 * buildCommand.schedule(); }
	 *
	 * private void createBuildCommand() { buildCommand = new RunNPMCommand(NGClientConstants.NPM_BUILD_JOB, nodePath, npmPath, projectFolder,
	 * NGClientConstants.NG_BUILD_COMMAND); buildCommand.setUser(false); buildCommand.setSystem(true); }
	 */

	void waitForNodeExtraction()
	{
		try
		{
			nodeReady.await();
		}
		catch (InterruptedException e)
		{
			ServoyLog.logError(e);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception
	{
		if (buildCommand != null) buildCommand.cancel();
	}
}
