/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV
 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
*/

package com.servoy.eclipse.debug.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.base.util.ITagResolver;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.debug.Activator;
import com.servoy.eclipse.debug.actions.IDebuggerStartListener;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.preferences.NGDesktopConfiguration;
import com.servoy.eclipse.ui.preferences.NgDesktopPreferences;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Utils;

/**
 * @author costinchiulan
 * @since 2019.06
 */
public class StartNGDesktopClientHandler extends StartDebugHandler implements IRunnableWithProgress, IDebuggerStartListener
{

	public static String NGDESKTOP_VERSION = "2021.09.0";
	public static final String NGDESKTOP_APP_NAME = "servoyngdesktop";
	public static String DOWNLOAD_URL = System.getProperty("ngdesktop.download.url", "https://download.servoy.com/ngdesktop/");
	protected static String PLATFORM = Utils.isAppleMacOS() ? "-mac" : (Utils.isWindowsOS()) ? "-win" : "-linux";
	protected static String ARCHITECTURE = Utils.isArmArchitecture() ? "-arm64" : "";
	protected static String LOCAL_PATH = Activator.getDefault().getStateLocation().toOSString() + File.separator;
	protected static String NGDESKTOP_PREFIX = NGDESKTOP_APP_NAME + "-" + NGDESKTOP_VERSION;

	protected static final int BUFFER_SIZE = 16 * 1024;

	static
	{
		if (!DOWNLOAD_URL.endsWith("/")) DOWNLOAD_URL += "/";
	}

	public static ITagResolver noReplacementResolver = new ITagResolver()
	{
		public String getStringValue(String name)
		{
			return "%%" + name + "%%";
		}
	};

	public static ITagResolver simpleReplacementResolver = new ITagResolver()
	{
		public String getStringValue(String name)
		{
			if ("prefix".equals(name)) return "forms.customer.";
			else if ("elementName".equals(name)) return ".elements.customer_id";
			else return "%%" + name + "%%";
		}
	};

	public static String getNgDesktopVersion(String requestedVersion)
	{
		String result = null;
		if ("latest".equals(requestedVersion))
		{
			List<String> versions = NGDesktopConfiguration.getAvailableVersions(); //sorted list - natural order
			if (versions.size() > 0)
			{
				result = versions.get(versions.size() - 1);//the last one is the latest
			}
			else
			{
				result = NGDESKTOP_VERSION; //manually set to the latest
			}
		}
		else
		{
			result = requestedVersion;
		}
		final String srcNumbers[] = result.split("\\.");
		if (srcNumbers.length < 3)
		{
			result += ".0";
		}
		return result;
	}

	@Override
	public Object execute(ExecutionEvent event)
	{
		// UI display version may have a two numbers format (i.e. 2020.12) while the real version is ALWAYS three
		// numbers format (i.e. 2020.12.0
		NgDesktopPreferences prefs = new NgDesktopPreferences();
		NGDESKTOP_VERSION = getNgDesktopVersion(prefs.getNgDesktopVersionKey());
		NGDESKTOP_PREFIX = NGDESKTOP_APP_NAME + "-" + NGDESKTOP_VERSION;

		Job job = new Job("NGDesktop client launch")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				try
				{
					StartNGDesktopClientHandler.this.run(monitor);
				}
				catch (InvocationTargetException e)
				{
					ServoyLog.logError(e);
				}
				catch (InterruptedException e)
				{
					ServoyLog.logError(e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
		return null;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
	{
		StartClientHandler.setLastCommand(StartClientHandler.START_NG_DESKTOP_CLIENT);
		monitor.beginTask(getStartTitle(), 5);
		monitor.worked(1);

		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject activeProject = servoyModel.getActiveProject();

		if (activeProject == null || activeProject.getSolution() == null) return;
		final Solution solution = activeProject.getSolution();
		if (isSmartClientType(solution)) return;

		monitor.worked(2);
		boolean downloadCancelled = false;
		if (testAndStartDebugger())
		{
			try
			{
				if (archiveUpdateNeeded())
				{
					URL archiveUrl = new URL(DOWNLOAD_URL + NGDESKTOP_VERSION + "/" + NGDESKTOP_PREFIX + PLATFORM + ARCHITECTURE + ".tar.gz");
					String savePath = Utils.isAppleMacOS() ? LOCAL_PATH + NGDESKTOP_PREFIX + PLATFORM + ARCHITECTURE : LOCAL_PATH;
					deleteVersionFile();
					downloadCancelled = downloadArchive(archiveUrl, savePath);
				}
				if (!downloadCancelled)
				{
					downloadVersionFile();
					updateJsonFile(solution);
					monitor.worked(2);
					runNgDesktop(monitor);
				}
			}
			catch (IllegalStateException | IOException e)
			{
				ServoyLog.logError(e.getMessage(), e);
			}
		}
		monitor.done();
	}

	private boolean archiveUpdateNeeded() throws IOException
	{
		String versionFilename = NGDESKTOP_PREFIX + "-archive.version";
		File currentVersionFile = Paths.get(LOCAL_PATH + versionFilename).normalize().toFile();
		if (!currentVersionFile.exists())
			return true;
		URL remoteVersionURL = new URL(DOWNLOAD_URL + NGDESKTOP_VERSION + "/" + versionFilename);
		try (InputStream remoteStream = remoteVersionURL.openStream();
			InputStream localStream = new FileInputStream(currentVersionFile))
		{

			byte[] remoteBuf = getBytes(remoteStream);
			byte[] currentBuf = getBytes(localStream);
			if (!Arrays.equals(remoteBuf, currentBuf))
			{
				return true;
			}
		}
		return false;
	}

	private byte[] getBytes(InputStream in) throws IOException
	{
		try
		{
			byte versionBuffer[] = new byte[BUFFER_SIZE]; //default initialize to '0'
			int bytesRead = in.read(versionBuffer, 0, BUFFER_SIZE);
			return bytesRead != -1 ? Arrays.copyOf(versionBuffer, bytesRead) : null;
		}
		finally
		{
			in.close();
		}
	}

	private boolean downloadArchive(URL archiveUrl, String savePath)
	{
		final boolean[] cancelled = { false };
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				ProgressMonitorDialog dialog = new ProgressMonitorDialog(UIUtils.getActiveShell());
				try
				{
					dialog.run(true, true, new DownloadNgDesktop(archiveUrl, savePath));
				}
				catch (InvocationTargetException | InterruptedException e)
				{
					ServoyLog.logError(e);
				}
				if (dialog.getProgressMonitor().isCanceled())
				{
					deleteVersionFile();
					cancelled[0] = true;
				}
			}
		});

		return cancelled[0];
	}

	private void downloadVersionFile() throws IOException
	{
		File versionFilename = new File(NGDESKTOP_PREFIX + "-archive.version");
		File currentVersionFile = Paths.get(LOCAL_PATH + versionFilename).normalize().toFile();
		URL remoteVersionURL = new URL(DOWNLOAD_URL + NGDESKTOP_VERSION + "/" + versionFilename);
		try (InputStream remoteStream = remoteVersionURL.openStream();
			OutputStream localStream = new FileOutputStream(currentVersionFile))
		{

			byte[] remoteBuf = getBytes(remoteStream);
			localStream.write(remoteBuf); //this will overwrite the old content

		}
	}

	private void deleteVersionFile()
	{//this will enforce a new download
		File currentVersionFile = Paths.get(LOCAL_PATH + NGDESKTOP_PREFIX + "-archive.version").normalize().toFile();
		if (currentVersionFile.exists())
		{
			currentVersionFile.delete();
		}
	}

	private JSONObject getJsonObj(File configFile, String url)
	{
		String jsonFile = Utils.getTXTFileContent(configFile, Charset.forName("UTF-8"));
		JSONObject configObject = new JSONObject(jsonFile);

		//put url and other options in servoy.json (we can put image also here, check servoy.json to see available options.
		try
		{ //newer ngdesktop no longer have the "options" section in config file
			JSONObject options = configObject.getJSONObject("options");
			options.put("url", url);
			options.put("showMenu", true);
			configObject.put("options", options);
		}
		catch (JSONException e)
		{//options not found
			configObject.put("url", url);
			configObject.put("showMenu", true);
		}

		return configObject;
	}

	/**
	 * Method for writing into servoy.json details about NgDesktop app.
	 * Here can be also changed icon, url, or used modules.
	 */

	private void updateJsonFile(Solution solution)
	{
		String solutionUrl = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/solution/" + solution.getName() + "/index.html";
		String resourceStr = Utils.isAppleMacOS() ? "/" + NGDESKTOP_APP_NAME + ".app/Contents/Resources" : File.separator + "resources";
		String configLocation = resourceStr + File.separator + "app.asar.unpacked" + File.separator + "config" +
			File.separator + "servoy.json";

		File configFile = Paths.get(LOCAL_PATH + NGDESKTOP_PREFIX + PLATFORM + ARCHITECTURE + configLocation).normalize().toFile();// + fileUrl);
		JSONObject configObject = getJsonObj(configFile, solutionUrl);

		try (FileWriter file = new FileWriter(configFile);
			BufferedWriter out = new BufferedWriter(file);)
		{
			out.write(configObject.toString());
		}
		catch (IOException e1)
		{
			ServoyLog.logError("Error writing  in servoy.json file " + configFile.getAbsolutePath(), e1);
		}
	}

	private void runNgDesktop(IProgressMonitor monitor)
	{
		//Now try opening servoyNGDesktop app.
		try
		{
			String extension = Utils.isAppleMacOS() ? ".app" : Utils.isWindowsOS() ? ".exe" : "";
			String command = LOCAL_PATH + NGDESKTOP_PREFIX + PLATFORM + ARCHITECTURE + File.separator + NGDESKTOP_APP_NAME + extension;
			monitor.beginTask("Open NGDesktop", 3);
			String[] cmdArgs = Utils.isAppleMacOS() ? new String[] { "/usr/bin/open", command } : new String[] { command };
			Runtime.getRuntime().exec(cmdArgs);
		}
		catch (IOException e)
		{
			ServoyLog.logError("Cannot find servoy NGDesktop executable", e);
		}
	}

	public String getStartTitle()
	{
		return "NGDesktop client launch";
	}


	private boolean isSmartClientType(Solution solution)
	{
		if (solution.getSolutionType() == SolutionMetaData.SMART_CLIENT_ONLY)
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					org.eclipse.jface.dialogs.MessageDialog.openError(UIUtils.getActiveShell(), "Solution type problem",
						"Cant open this solution type in this client");
				}
			});
			return true;
		}
		return false;
	}

	@Override
	protected IDebuggerStartListener getDebuggerAboutToStartListener()
	{
		return this;
	}

	public void aboutToStartDebugClient()
	{

	}
}

class DownloadNgDesktop implements IRunnableWithProgress
{
	private final URL url;
	private final String savePath;

	public DownloadNgDesktop(URL url, String savePath)
	{
		super();
		this.url = url;
		this.savePath = savePath;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InterruptedException
	{
		try
		{
			deletePreviousNgDesktop();
			Path outputPath = makeDirs();

			int mbSize = Math.round((float)getUrlSize(url) / (1024 * 1024)) * 2; //not sure why but TarArchive methods call stream methods twice
			monitor.beginTask("Downloading NGDesktop executable", mbSize);

			try (MonitorInputStream monInputStream = new MonitorInputStream(url.openStream(), StartNGDesktopClientHandler.BUFFER_SIZE, monitor);
				TarArchiveInputStream archInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(monInputStream));)
			{
				TarArchiveEntry entry = null;
				while ((entry = archInputStream.getNextTarEntry()) != null)
				{
					processEntry(outputPath, entry, archInputStream);
				}
				if (monInputStream.getCurrentStep() < mbSize)
				{
					monitor.worked(mbSize - monInputStream.getCurrentStep());
					Thread.sleep(500);//give the user enough time to visually observe the completed (100%) progress bar
				}
				monInputStream.close();
				monitor.done();
			}
			catch (CancellationException e)
			{
				throw new InterruptedException();
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError("Cannot find NgDesktop in download center", e);
			throw new InterruptedException();
		}
		monitor.done();
	}

	private void deletePreviousNgDesktop() throws IOException
	{
		Path localPath = Paths.get(StartNGDesktopClientHandler.LOCAL_PATH).normalize();
		if (Files.exists(localPath, LinkOption.NOFOLLOW_LINKS)) //delete previous version
		{
			Files.walk(localPath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
	}

	private Path makeDirs()
	{
		String fString = Activator.getDefault().getStateLocation().toOSString();
		if (Utils.isAppleMacOS())
		{
			fString += File.separator + "servoyngdesktop" + "-" + StartNGDesktopClientHandler.NGDESKTOP_VERSION + "-mac" +
				StartNGDesktopClientHandler.ARCHITECTURE;
		}
		File f = new File(fString);
		f.mkdirs();
		return f.toPath().toAbsolutePath().normalize();
	}

	private void processEntry(Path outputPath, TarArchiveEntry entry, InputStream in) throws IOException
	{
		Path path = outputPath.resolve(entry.getName()).normalize();
		if (entry.isDirectory())
		{
			Files.createDirectories(path);
		}
		else if (entry.isSymbolicLink())
		{
			Files.createDirectories(path.getParent());
			String dest = entry.getLinkName();
			Files.createSymbolicLink(path, Paths.get(dest));
		}
		else
		{
			Files.createDirectories(path.getParent());
			try (OutputStream out = Files.newOutputStream(path))
			{
				IOUtils.copy(in, out);//copy current archIS entry
			}
		}
		if (!Files.isSymbolicLink(path) && !Utils.isWindowsOS())
		{
			Files.setPosixFilePermissions(path, getPosixFilePermissions(entry));
		}
	}

	private Set<PosixFilePermission> getPosixFilePermissions(TarArchiveEntry entry)
	{
		int mode = entry.getMode();
		Set<PosixFilePermission> perms = new HashSet<>();
		if ((mode & 0400) != 0)
		{
			perms.add(PosixFilePermission.OWNER_READ);
		}
		if ((mode & 0200) != 0)
		{
			perms.add(PosixFilePermission.OWNER_WRITE);
		}
		if ((mode & 0100) != 0)
		{
			perms.add(PosixFilePermission.OWNER_EXECUTE);
		}
		if ((mode & 0040) != 0)
		{
			perms.add(PosixFilePermission.GROUP_READ);
		}
		if ((mode & 0020) != 0)
		{
			perms.add(PosixFilePermission.GROUP_WRITE);
		}
		if ((mode & 0010) != 0)
		{
			perms.add(PosixFilePermission.GROUP_EXECUTE);
		}
		if ((mode & 0004) != 0)
		{
			perms.add(PosixFilePermission.OTHERS_READ);
		}
		if ((mode & 0002) != 0)
		{
			perms.add(PosixFilePermission.OTHERS_WRITE);
		}
		if ((mode & 0001) != 0)
		{
			perms.add(PosixFilePermission.OTHERS_EXECUTE);
		}
		return perms;
	}

	private int getUrlSize(URL fileUrl)
	{
		URLConnection conn = null;
		try
		{
			conn = fileUrl.openConnection();
			if (conn instanceof HttpURLConnection)
			{
				((HttpURLConnection)conn).setRequestMethod("HEAD");
			}
			try (InputStream is = conn.getInputStream())
			{
				return conn.getContentLength();
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		finally
		{
			if (conn instanceof HttpURLConnection)
			{
				((HttpURLConnection)conn).disconnect();
			}
		}
	}
}

class MonitorInputStream extends BufferedInputStream
{
	IProgressMonitor monitor;
	int bytesCount = 0;
	int mbCount = 0;
	int currentStep;

	public MonitorInputStream(InputStream in, int size)
	{
		super(in, size);
	}

	public MonitorInputStream(InputStream in, int size, IProgressMonitor monitor)
	{
		super(in, size);
		this.monitor = monitor;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		int n = super.read(b, off, len);
		bytesCount += n;

		int bytesToMegaBytes = Math.round((float)bytesCount / (1024 * 1024));// bytes => MB
		if (bytesToMegaBytes > 0)
		{
			currentStep += bytesToMegaBytes;
			monitor.worked(bytesToMegaBytes);
			bytesCount = 0;
			if (monitor.isCanceled())
			{
				throw new CancellationException();
			}
		}
		return n;
	}

	@Override
	public void close() throws IOException
	{
		super.close();
	}

	public int getCurrentStep()
	{
		return currentStep;
	}
}
