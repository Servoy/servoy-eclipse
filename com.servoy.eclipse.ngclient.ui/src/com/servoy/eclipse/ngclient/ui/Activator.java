package com.servoy.eclipse.ngclient.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import com.servoy.eclipse.model.util.ModelUtils;
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

	/**
	 * When true, run() of some affected jobs returns CANCELLED immediately, without doing
	 * anything else. Some jobs might not even be started.
	 *
	 * In normal operation this will always be false (so everything is enabled).
	 *
	 * When running junit tests this will be true initially (because most tests only test the java/persist/developer part),
	 * and then each test class can change it at will.
	 */
	private static volatile boolean nodeExtractionAndTitaniumBuildDisabled = ModelUtils.isTestRunning();

	private File nodePath;
	private File npmPath;
	private File pnpmPath;
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
			if (nodeReady.getCount() == 0 && nodePath != null && ServoyModelFinder.getServoyModel() != null &&
				ServoyModelFinder.getServoyModel().getNGPackageManager() != null)
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
		if (isNodeExtractionAndTitaniumBuildDisabled()) return;

		String nodePth = getSystemOrEvironmentProperty("servoy.nodePath");
		String npmPth = getSystemOrEvironmentProperty("servoy.npmPath");
		String pnpmPth = getSystemOrEvironmentProperty("servoy.pnpmPath");
		if (nodePth != null && npmPth != null)
		{
			nodePath = new File(nodePth);
			npmPath = new File(npmPth);
			if (pnpmPth != null)
			{
				pnpmPath = new File(pnpmPth);
			}
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
					File pnpm = null;
					if (cf.length > 0)
					{
						node = extractPath(cf[0], "nodePath", true);
						node.setExecutable(true);
						npm = extractPath(cf[0], "npmPath", false);
						// in pnpm mode also extract the bundled pnpm binary next to node so pnpm can find the bundled node via PATH;
						// pnpm uses its own extraction marker so its extract/skip decision is independent of node's .fullygenerated
						if (isPnpmMode() && cf[0].getAttribute("pnpmPath") != null)
						{
							pnpm = extractPnpmPath(cf[0]);
							if (pnpm != null)
							{
								pnpm.setExecutable(true);
							}
						}
					}
					else
					{
						ServoyLog.logWarning("No Node.js plugin found, npm commands will be unavailable", null);
					}
					nodePath = node;
					npmPath = npm;
					pnpmPath = pnpm;
					countDown();
					return Status.OK_STATUS;
				}
			};
			extractingNode.schedule();
		}
	}

	/**
	 * @return true if Servoy Developer runs in pnpm mode (servoy.jsRuntime=pnpm), false otherwise (npm is the default).
	 */
	public static boolean isPnpmMode()
	{
		Activator instance = getInstance();
		if (instance == null) return false;
		String jsRuntime = instance.getSystemOrEvironmentProperty("servoy.jsRuntime");
		return "pnpm".equalsIgnoreCase(jsRuntime);
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

	/**
	 * @return the bundled Node executable, or <code>null</code> if Node has not been extracted yet.
	 */
	public File getNodePath()
	{
		return nodePath;
	}

	/**
	 * @return the bundled pnpm executable (only relevant in pnpm mode), or <code>null</code> if pnpm is not available.
	 */
	public File getPnpmPath()
	{
		return pnpmPath;
	}

	private static File extractPath(IConfigurationElement element, String attribute, boolean deletePreviousPaths)
	{
		return extractPath(element, attribute, "archive", deletePreviousPaths);
	}

	private static File extractPath(IConfigurationElement element, String attribute, String archiveAttribute, boolean deletePreviousPaths)
	{
		String pluginId = element.getNamespaceIdentifier();
		String path = element.getAttribute(attribute);
		IPath stateLocation = plugin.getStateLocation();
		File baseDir = stateLocation.toFile();
		File file = new File(baseDir, path);
		File fullyGenerated = new File(baseDir, ".fullygenerated");
		if (!file.exists() || !fullyGenerated.exists())
		{
			String archive = element.getAttribute(archiveAttribute);
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

	/**
	 * Extracts the bundled pnpm archive into a version-qualified sub-directory of the plugin state location
	 * (e.g. <code>pnpm-12.5.1/</code>), keeping it out of the state-location root where the TiNG
	 * <code>target/</code> folder lives. pnpm uses a pnpm-specific <code>.pnpmgenerated</code> marker so that its
	 * extract/skip decision is fully independent of node's <code>.fullygenerated</code> marker.
	 * <p>
	 * The pnpm archive contains the pnpm binary plus a sibling <code>dist/</code> directory; both are extracted
	 * into the version sub-dir so a single <code>pnpm-&lt;version&gt;/</code> folder holds the whole install (the
	 * binary runs standalone, but keeping <code>dist/</code> beside it is tidy and self-contained). The version
	 * token is the contributing bundle's version (or an optional <code>pnpmVersion</code> attribute override) and
	 * doubles as the sub-dir name, so shipping a new pnpm version extracts into a fresh
	 * <code>pnpm-&lt;newVersion&gt;/</code> dir and the stale one is removed.
	 * <p>
	 * Re-extraction is triggered when the pnpm binary is missing, the <code>.pnpmgenerated</code> marker is missing,
	 * OR the marker's stored token differs from the current token. pnpm never touches the node install or the
	 * <code>NG2_FOLDER</code>; on a version change it only removes its own previous <code>pnpm-&lt;oldVersion&gt;/</code>
	 * directory. This keeps the two extractions fully independent and future-proof.
	 *
	 * @param element the <code>nodejs</code> extension configuration element carrying <code>pnpmPath</code> /
	 *            <code>pnpmArchive</code>
	 * @return the pnpm binary {@link File} inside its version sub-dir, or <code>null</code> if the pnpm archive
	 *         could not be resolved
	 */
	private static File extractPnpmPath(IConfigurationElement element)
	{
		String pluginId = element.getNamespaceIdentifier();
		String path = element.getAttribute("pnpmPath");
		IPath stateLocation = plugin.getStateLocation();
		File baseDir = stateLocation.toFile();

		// The pnpm version identity: the contributing bundle's version bumps whenever the shipped pnpm payload
		// changes, so it is a stable token for "which pnpm is on disk". An optional pnpmVersion attribute may
		// override it if ever declared in plugin.xml.
		String currentToken = element.getAttribute("pnpmVersion");
		if (currentToken == null || currentToken.isBlank())
		{
			currentToken = Platform.getBundle(pluginId).getVersion().toString();
		}

		// extract into a version-qualified sub-dir (e.g. pnpm-12.5.1/) so pnpm and its dist/ stay out of the
		// state-location root that holds the TiNG target/ folder
		File pnpmDir = new File(baseDir, "pnpm-" + currentToken);
		File file = new File(pnpmDir, path);
		File pnpmGenerated = new File(baseDir, ".pnpmgenerated");

		String storedToken = readPnpmMarkerToken(pnpmGenerated);

		boolean needsExtract = !file.exists() || !pnpmGenerated.exists() || !currentToken.equals(storedToken);
		if (needsExtract)
		{
			String archive = element.getAttribute("pnpmArchive");
			URL archiveUrl = Platform.getBundle(pluginId).getResource(archive);
			if (archiveUrl != null)
			{
				// clean up the previous pnpm version dir (targeted: only our own pnpm-* dirs, never node dirs or
				// the NG2_FOLDER)
				File[] stalePnpmDirs = baseDir.listFiles(
					oldFile -> oldFile.isDirectory() && oldFile.getName().startsWith("pnpm-") && !oldFile.getName().equals(pnpmDir.getName()));
				if (stalePnpmDirs != null)
				{
					for (File stale : stalePnpmDirs)
					{
						try
						{
							FileUtils.deleteDirectory(stale);
						}
						catch (IOException e)
						{
							getInstance().getLog().error("Error deleting old pnpm dir: " + stale.getAbsolutePath(), e);
						}
					}
				}
				if (pnpmGenerated.exists())
				{
					pnpmGenerated.delete();
				}
				pnpmDir.mkdirs();
				try
				{
					if (ZipUtils.isZipFile(archiveUrl))
					{
						ZipUtils.extractZip(archiveUrl, pnpmDir);
					}
					else if (ZipUtils.isTarGZFile(archiveUrl))
					{
						ZipUtils.extractTarGZ(archiveUrl, pnpmDir);
					}
					else if (ZipUtils.isTarXZFile(archiveUrl))
					{
						ZipUtils.extractTarXZ(archiveUrl, pnpmDir);
					}
					// record the version token so a later pnpm version change forces re-extraction
					Files.writeString(pnpmGenerated.toPath(), currentToken, StandardCharsets.UTF_8);
				}
				catch (IOException e)
				{
					getInstance().getLog().error("Error extracting pnpm from " + archiveUrl, e);
				}
			}
			else
			{
				getInstance().getLog().info("couldn't extract pnpm from plugin " + pluginId + " and archive: " + archive);
				return null;
			}
		}
		return file;
	}

	/**
	 * Reads the version token previously stored in the <code>.pnpmgenerated</code> marker. A missing, empty or
	 * unreadable marker yields <code>null</code> (treated as "no token" -> re-extract).
	 */
	private static String readPnpmMarkerToken(File pnpmGenerated)
	{
		if (!pnpmGenerated.exists())
		{
			return null;
		}
		try
		{
			String token = Files.readString(pnpmGenerated.toPath(), StandardCharsets.UTF_8).trim();
			return token.isEmpty() ? null : token;
		}
		catch (IOException e)
		{
			getInstance().getLog().error("Error reading pnpm marker: " + pnpmGenerated.getAbsolutePath(), e);
			return null;
		}
	}

	public IRunNPMCommand createNPMCommand(File folder, List<String> commandArguments)
	{
		waitForNodeExtraction();
		if (nodePath == null)
		{
			return new NoOpNPMCommand(commandArguments);
		}
		if (isPnpmMode())
		{
			if (pnpmPath == null)
			{
				getInstance().getLog().warn("servoy.jsRuntime=pnpm is set but no bundled pnpm binary was found; no npm/pnpm command will run.");
				return new NoOpNPMCommand(commandArguments);
			}
			return new RunPNPMCommand(pnpmPath, nodePath, folder, commandArguments);
		}
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

	/**
	 * Disables or not the node install extraction / node source folder copy/npm angular titanium build cycle for the duration of tests that
	 * do not need the NG client node folder. Call this before activating any
	 * solution in test setup. Re-enable with {@link #setDisabled(boolean) setDisabled(false)}
	 * in teardown if a subsequent test class requires it.<br/><br/>
	 *
	 * Also trigger a manual build when you re-enable it - at the point you need the angular build result.<br/>
	 * So you can use {@link com.servoy.eclipse.ngclient.ui.CopySourceFolderAction#startTitaniumNGBuild(int)}.<br/><br/>
	 *
	 * A call to {@link com.servoy.eclipse.ngclient.ui.Activator#extractNode()} + wait for it will be done directly by this method when it
	 * switches from true to false, in order to make sure node installation is ready to use.
	 *
	 * @param value true to skip the copy/titanium build cycle, false to re-enable it
	 */
	public static void setNodeExtractionAndTitaniumBuildDisabled(boolean value)
	{
		boolean previousDisabledValue = nodeExtractionAndTitaniumBuildDisabled;
		nodeExtractionAndTitaniumBuildDisabled = value;
		if (previousDisabledValue && !nodeExtractionAndTitaniumBuildDisabled)
		{
			long x = System.currentTimeMillis();
			System.out.println("*** starting node extraction");

			Activator.getInstance().extractNode(); // just to be sure node installation is extracted/present before any builds run
			Activator.getInstance().waitForNodeExtraction(); // TODO: is this ok when running junit tests or should we add that job
			// to a job group and join just as we do for the eclipse build jobs and titanium build jobs?
			// so similar to how TestUtilitiesClass.waitForWorkspaceBuildJobs() and waitForTitaniumuildJobs() do it

			System.out.println("*** node extraction took: " + String.format("%.2f", ((System.currentTimeMillis() - x) / 1000d)) + " s");
		}
	}

	/**
	 * @return true if the node install extraction / node src folder copy / titanium build cycle is currently disabled
	 */
	public static boolean isNodeExtractionAndTitaniumBuildDisabled()
	{
		return nodeExtractionAndTitaniumBuildDisabled;
	}

	@Override
	public void stop(BundleContext context) throws Exception
	{
		if (buildCommand != null) buildCommand.cancel();
	}
}
