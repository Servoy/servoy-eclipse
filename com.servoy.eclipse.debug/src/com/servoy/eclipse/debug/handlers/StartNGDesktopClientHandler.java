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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
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
import org.json.JSONObject;

import com.servoy.base.util.ITagResolver;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.debug.Activator;
import com.servoy.eclipse.debug.NGClientStarter;
import com.servoy.eclipse.debug.actions.IDebuggerStartListener;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Utils;

/**
 * @author costinchiulan
 * @since 2019.06
 */
public class StartNGDesktopClientHandler extends StartDebugHandler implements IRunnableWithProgress, IDebuggerStartListener, NGClientStarter
{

	static final String NGDESKTOP_MAJOR_VERSION = Integer.toString(ClientVersion.getMajorVersion());
	static final String NGDESKTOP_MINOR_VERSION = ClientVersion.getMiddleVersion() < 10 ? "0" + Integer.toString(ClientVersion.getMiddleVersion())
		: Integer.toString(ClientVersion.getMiddleVersion());

	static final int BUFFER_SIZE = 16 * 1024;
	static final String MAC_EXTENSION = ".app";
	static final String WINDOWS_EXTENSION = ".exe";
	//Linux doesn't have any extension

	public static final String WINDOWS_BUILD_PLATFORM = "win";
	public static final String MAC_BUILD_PLATFORM = "mac";
	public static final String LINUX_BUILD_PLATFORM = "linux";

	public static final String NG_DESKTOP_APP_NAME = "servoyngdesktop";
	public static String DOWNLOAD_URL = System.getProperty("ngdesktop.download.url", "http://download.servoy.com/ngdesktop/");
	public static final String NGDESKTOP_VERSION = NGDESKTOP_MAJOR_VERSION + "." + NGDESKTOP_MINOR_VERSION;

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

	@Override
	public Object execute(ExecutionEvent event)
	{

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

	public String getStartTitle()
	{
		return "NGDesktop client launch";
	}


	/**
	 * Method for writing into servoy.json details about electron app.
	 * Here can be also changed icon, url, or used modules.
	 */

	private void writeElectronJsonFile(Solution solution, String localPath, String fileExtension, IProgressMonitor monitor)
	{

		String solutionUrl = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/solutions/" + solution.getName() + "/index.html";

		String osxContent = Utils.isAppleMacOS() ? File.separator + "Contents" : "";

		//Mac folder structure is different, we should adapt url to that.
		String fileUrl = osxContent + File.separator + (Utils.isAppleMacOS() ? "Resources" : "resources") + File.separator + "app.asar.unpacked" +
			File.separator + "config" + File.separator + "servoy.json";

		String fPath = localPath;
		if (Utils.isAppleMacOS()) fPath += File.separator + StartNGDesktopClientHandler.NG_DESKTOP_APP_NAME + StartNGDesktopClientHandler.MAC_EXTENSION;

		File f = new File(fPath + File.separator + fileUrl);

		//Store servoy.json file as a JSONObject
		String jsonFile = Utils.getTXTFileContent(f, Charset.forName("UTF-8"));
		JSONObject configFile = new JSONObject(jsonFile);

		JSONObject options = (JSONObject)configFile.get("options");
		//put url and other options in servoy.json(we can put image also here, check servoy.json to see available options.
		options.put("url", solutionUrl);
		options.put("showMenu", true);
		configFile.put("options", options);

		try (FileWriter file = new FileWriter(f))
		{
			BufferedWriter out = new BufferedWriter(file);
			out.write(configFile.toString());
			out.close();
		}
		catch (IOException e1)
		{
			ServoyLog.logError("Error writing  in servoy.json file " + fileUrl, e1);
		}

		//Now try opening servoyNGDesktop app.
		try
		{
			String[] command;
			if (Utils.isAppleMacOS())
			{
				command = new String[] { "/usr/bin/open", localPath + File.separator + NG_DESKTOP_APP_NAME + fileExtension };
			}
			else
			{//windows || linux
				command = new String[] { localPath + File.separator + NG_DESKTOP_APP_NAME + fileExtension };
			}
			monitor.beginTask("Open NGDesktop", 3);
			Runtime.getRuntime().exec(command);
		}
		catch (IOException e)
		{
			ServoyLog.logError("Cannot find servoy NGDesktop executable", e);
		}

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

	/*
	 * Compare the remote NGDesktop version with current version and delete current if it's the case. Deleting current version will enforce remote version
	 * download
	 */
	private void checkForHigherVersion(File location)
	{
		try
		{
			File parentFile = location.getParentFile();
			URL fileUrl = new URL(
				DOWNLOAD_URL + "version" + StartNGDesktopClientHandler.NGDESKTOP_MAJOR_VERSION + StartNGDesktopClientHandler.NGDESKTOP_MINOR_VERSION + ".txt");
			File currentVersionFile = new File(parentFile.getAbsolutePath() + File.separator + "version" + StartNGDesktopClientHandler.NGDESKTOP_MAJOR_VERSION +
				StartNGDesktopClientHandler.NGDESKTOP_MINOR_VERSION + ".txt");

			byte[] remoteBuf = getBytes(fileUrl.openStream());
			byte[] currentBuf = currentVersionFile.exists() ? getBytes(new FileInputStream(currentVersionFile)) : null;
			if (!Arrays.equals(remoteBuf, currentBuf))
			{
				//TODO: notify user. if (user decide to download higher version) {
				if (location.exists())
				{
					Files.walk(location.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
				}
				OutputStream versionStream = new FileOutputStream(currentVersionFile);
				versionStream.write(remoteBuf); //this will overwrite the old content
				versionStream.close();
				//} TODO: end
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError("Exception while checking for higher version: ", e);
		}
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
	{
		runProgressBarAndNGClient(monitor);
	}

	@Override
	public void startNGClient(IProgressMonitor monitor)

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
		if (testAndStartDebugger())
		{
			String extension = Utils.isAppleMacOS() ? MAC_EXTENSION : (Utils.isWindowsOS() ? WINDOWS_EXTENSION : "");
			String platform = Utils.isAppleMacOS() ? MAC_BUILD_PLATFORM : (Utils.isWindowsOS()) ? WINDOWS_BUILD_PLATFORM : LINUX_BUILD_PLATFORM;
			String appName = NG_DESKTOP_APP_NAME + "-" + NGDESKTOP_VERSION;
			String localPath = Activator.getDefault().getStateLocation().append(appName + "-" + platform).toOSString();
			String pathToExecutable = localPath + File.separator + NG_DESKTOP_APP_NAME + extension;

			try
			{
				URL archiveUrl = new URL(StartNGDesktopClientHandler.DOWNLOAD_URL + appName + "-" + platform + ".tar.gz");
				File executable = new File(pathToExecutable);
				checkForHigherVersion(executable.getParentFile());
				boolean downloadCancelled = false;
				if (!executable.exists())
				{
					downloadCancelled = downloadArchive(archiveUrl, localPath);
				}
				if (!downloadCancelled)
				{
					writeElectronJsonFile(solution, localPath, extension, monitor);
					monitor.worked(2);
				}
			}
			catch (MalformedURLException e)
			{
			}
		}
		monitor.done();
	}

	private boolean isSmartClientType(Solution solution)
	{
		if (solution.getSolutionType() == SolutionMetaData.SMART_CLIENT_ONLY)
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					org.eclipse.jface.dialogs.MessageDialog.openError(Display.getDefault().getActiveShell(), "Solution type problem",
						"Cant open this solution type in this client");
				}
			});
			return true;
		}
		return false;
	}

	private boolean downloadArchive(URL url, String localPath)
	{
		final boolean[] cancelled = { false };
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				ProgressMonitorDialog dialog = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
				try
				{
					dialog.run(true, true, new DownloadElectron(url, localPath));
				}
				catch (InvocationTargetException | InterruptedException e)
				{
					System.out.println("Interrupted");
				}
				if (dialog.getProgressMonitor().isCanceled())
				{
					System.out.println("Cancelled");
					deleteVersionFile(localPath);
					cancelled[0] = true;
				}
			}
		});
		return cancelled[0];
	}

	private void deleteVersionFile(String localPath)
	{//this will enforce a new download
		String parentPath = new File(localPath).getParentFile().getAbsolutePath();
		File currentVersionFile = new File(parentPath + File.separator + "version" + StartNGDesktopClientHandler.NGDESKTOP_MAJOR_VERSION +
			StartNGDesktopClientHandler.NGDESKTOP_MINOR_VERSION + ".txt");
		if (currentVersionFile.exists())
		{
			currentVersionFile.delete();
		}
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


class DownloadElectron implements IRunnableWithProgress
{
	private final URL url;
	private final String localPath;

	public DownloadElectron(URL url, String localPath)
	{
		super();
		this.url = url;
		this.localPath = localPath;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InterruptedException
	{
		try
		{
			extractTarGz(url, monitor);
		}
		catch (IOException e)
		{
			ServoyLog.logError("Cannot find Electron in download center", e);
			throw new InterruptedException();
		}

		monitor.done();
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

	private Path makeDirs()
	{
		String fString = Activator.getDefault().getStateLocation().toOSString();
		if (Utils.isAppleMacOS())
		{
			fString += File.separator + StartNGDesktopClientHandler.NG_DESKTOP_APP_NAME + "-" + StartNGDesktopClientHandler.NGDESKTOP_VERSION + "-" +
				StartNGDesktopClientHandler.MAC_BUILD_PLATFORM;
		}
		File f = new File(fString);
		f.mkdirs();
		return f.toPath().toAbsolutePath().normalize();
	}

	private void extractTarGz(URL tarGzUrl, IProgressMonitor monitor) throws IOException, InterruptedException
	{
		int fileSize = getUrlSize(tarGzUrl);
		String fString = Activator.getDefault().getStateLocation().toOSString();
		if (Utils.isAppleMacOS())
		{
			fString += File.separator + StartNGDesktopClientHandler.NG_DESKTOP_APP_NAME + "-" + StartNGDesktopClientHandler.NGDESKTOP_VERSION + "-" +
				StartNGDesktopClientHandler.MAC_BUILD_PLATFORM;
		}
		File f = new File(fString);
		f.mkdirs();
		Path outputPath = makeDirs();

		int mbSize = Math.round((float)fileSize / (1024 * 1024)) * 2; //not sure why but TarArchive methods call stream methods twice
		monitor.beginTask("Downloading NGDesktop executable", mbSize);

		try
		{

			MonitorInputStream monInputStream = new MonitorInputStream(tarGzUrl.openStream(), StartNGDesktopClientHandler.BUFFER_SIZE, monitor);
			TarArchiveInputStream archIS = new TarArchiveInputStream(
				new GzipCompressorInputStream(monInputStream));
			TarArchiveEntry entry = null;
			while ((entry = archIS.getNextTarEntry()) != null)
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
						IOUtils.copy(archIS, out);//copy current archIS entry
					}
				}
				if (!Files.isSymbolicLink(path) && !Utils.isWindowsOS())
				{
					Files.setPosixFilePermissions(path, getPosixFilePermissions(entry));
				}
			}
			if (monInputStream.getCurrentStep() < mbSize)
			{
				monitor.worked(mbSize - monInputStream.getCurrentStep());
				Thread.sleep(500);//let UI some time for UI update
			}
			monInputStream.close();
			monitor.done();
		}
		catch (CancellationException e)
		{
			throw new InterruptedException();
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
			conn.getInputStream();
			return conn.getContentLength();
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
