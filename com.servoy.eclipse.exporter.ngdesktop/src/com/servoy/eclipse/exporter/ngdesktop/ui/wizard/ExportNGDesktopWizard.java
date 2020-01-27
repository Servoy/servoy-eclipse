package com.servoy.eclipse.exporter.ngdesktop.ui.wizard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.debug.handlers.StartNGDesktopClientHandler;
import com.servoy.eclipse.exporter.ngdesktop.Activator;
import com.servoy.eclipse.exporter.ngdesktop.utils.NgDesktopClientConnection;
import com.servoy.eclipse.exporter.ngdesktop.utils.NgDesktopServiceMonitor;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 */
public class ExportNGDesktopWizard extends Wizard implements IExportWizard
{
	private ExportPage exportPage;
	private static int POLLING_INTERVAL = 5000;
	public final static int LOGO_SIZE = 256; // KB;
	public final static int IMG_SIZE = 512; // KB;
	public final static int COPYRIGHT_LENGTH = 128; // chars
	private final static int PROCESS_CANCELLED = 1;
	private final static int PROCESS_FINISHED = 0;
	private final boolean[] cancel = { false };

	public ExportNGDesktopWizard()
	{
		super();
		setWindowTitle("NG Desktop Export");
		setNeedsProgressMonitor(true);
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		if (activeProject != null)
		{
			IDialogSettings section = DialogSettings.getOrCreateSection(workbenchSettings, "NGDesktopExportWizard:" + activeProject.getSolution().getName());
			setDialogSettings(section);
		}
		else
		{
			IDialogSettings section = DialogSettings.getOrCreateSection(workbenchSettings, "NGDesktopExportWizard");
			setDialogSettings(section);
		}

	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
	}

	@Override
	public boolean performFinish()
	{
		exportPage.saveState();
		final IDialogSettings exportSettings = this.getDialogSettings();
		final StringBuilder errorMsg = validate(exportSettings);
		if (errorMsg.length() > 0)
		{
			MessageDialog.openError(UIUtils.getActiveShell(), "NG Desktop Export", errorMsg.toString());
			return false;
		}


		IRunnableWithProgress job = new IRunnableWithProgress()
		{
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				if (errorMsg.length() > 0) return;
				final NgDesktopServiceMonitor serviceMonitor = new NgDesktopServiceMonitor(monitor);
				exportPage.getSelectedPlatforms().forEach((platform) -> {
					int retCode = processPlatform(platform, exportSettings, serviceMonitor, errorMsg, cancel[0]);
					if (retCode == PROCESS_CANCELLED)
					{
						cancel[0] = true;
					}
				});
			}

		};
		return runContainer(job, errorMsg);
	}

	private boolean runContainer(IRunnableWithProgress job, StringBuilder errorMsg)
	{
		try
		{
			cancel[0] = false;
			getContainer().run(true, true, job);
		}
		catch (Exception e)
		{
			Debug.error(e);
			errorMsg.append(e.toString());
		}
		if (errorMsg.length() > 0)
		{
			MessageDialog.openError(UIUtils.getActiveShell(), "NG Desktop Export", errorMsg.toString());
		}
		else if (!cancel[0])
		{
			MessageDialog.openInformation(UIUtils.getActiveShell(), "NG Desktop Export", "Export done successfully!");
		}
		return true;
	}

	private int processPlatform(String platform, IDialogSettings settings, NgDesktopServiceMonitor monitor, StringBuilder errorMsg,
		boolean processAlreadyCancelled)
	{
		if (processAlreadyCancelled)
		{
			return PROCESS_CANCELLED;
		}
		if (platform.equals(ExportPage.MACOS_PLATFORM) || platform.equals(ExportPage.LINUX_PLATFORM))
		{
			String tmpDir = settings.get("save_dir").replaceAll("\\\\", "/");
			String saveDir = tmpDir.endsWith("/") ? tmpDir : tmpDir + "/";
			String archiveName = StartNGDesktopClientHandler.NG_DESKTOP_APP_NAME + "-" + StartNGDesktopClientHandler.NGDESKTOP_VERSION + "-" + platform +
				".tar.gz";
			File archiveFile = new File(saveDir + archiveName);
			downloadArchive(archiveFile, monitor, errorMsg);
		}
		else
		{
			processWindowsPlatform(monitor, settings, errorMsg);
		}
		if (monitor.isCanceled())
		{
			monitor.done();
			return PROCESS_CANCELLED;
		}
		return PROCESS_FINISHED;
	}

	private void downloadArchive(File archiveFile, IProgressMonitor monitor, StringBuilder errorMsg)
	{
		archiveFile.getParentFile().mkdirs();
		try
		{
			if (archiveFile.exists()) archiveFile.delete();
			URL fileUrl = new URL(StartNGDesktopClientHandler.DOWNLOAD_URL + archiveFile.getName());
			monitor.beginTask("Exporting " + archiveFile.getName() + "...", getRemoteSize(fileUrl));
			createTar(fileUrl.openStream(), archiveFile, this.getDialogSettings(), monitor);
			if (monitor.isCanceled())
			{
				archiveFile.delete();
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
			errorMsg.append(e.getMessage());
		}
	}

	private void processWindowsPlatform(NgDesktopServiceMonitor monitor, IDialogSettings settings, StringBuilder errorMsg)
	{
		NgDesktopClientConnection serviceConn = null;
		try
		{
			String tmpDir = settings.get("save_dir").replaceAll("\\\\", "/");
			String saveDir = tmpDir.endsWith("/") ? tmpDir : tmpDir + "/";
			serviceConn = new NgDesktopClientConnection();
			String tokenId = serviceConn.startBuild(ExportPage.WINDOWS_PLATFORM, settings);
			int status = NgDesktopClientConnection.OK;
			monitor.startChase("Waiting...", serviceConn.getNgDesktopBuildRefSize(), serviceConn.getNgDesktopBuildRefDuration());
			while (!monitor.isCanceled())
			{
				Thread.sleep(POLLING_INTERVAL);
				status = getStatus(serviceConn, monitor, tokenId, errorMsg);
				if (status == NgDesktopClientConnection.ERROR || status == NgDesktopClientConnection.NOT_FOUND || status == NgDesktopClientConnection.READY)
				{
					break;
				}
			}
			if (monitor.isCanceled())
			{
				serviceConn.cancel(tokenId);
				monitor.endChase();
			}
			if (!monitor.isCanceled() && NgDesktopClientConnection.READY == status)
			{
				serviceConn.download(tokenId, saveDir, monitor);
				serviceConn.delete(tokenId);
				monitor.done();
			}
		}
		catch (IOException | InterruptedException e)
		{
			errorMsg.append(e.getMessage());
		}
		finally
		{
			closeConnection(serviceConn);
		}
	}

	private int getStatus(NgDesktopClientConnection conn, NgDesktopServiceMonitor monitor, String tokenId, StringBuilder errorMsg) throws IOException
	{
		int status = conn.getStatus(tokenId);
		switch (status)
		{
			case NgDesktopClientConnection.WAITING :
			case NgDesktopClientConnection.PROCESSING :
				monitor.setTargetStep(conn.getNgDesktopBuildCurrentSize());
				monitor.setTaskName(conn.getStatusMessage());
				break;
			case NgDesktopClientConnection.ERROR :
				String errorMessage = conn.getStatusMessage();
				errorMsg.append(errorMessage);
				break;
			case NgDesktopClientConnection.NOT_FOUND :
				errorMsg.append("Build does not exist: " + tokenId);
				break;
			case NgDesktopClientConnection.READY :
				monitor.endChase();
				monitor.done();
				break;
			default :
				return -1;// unknown status
		}
		return status;
	}

	private void closeConnection(NgDesktopClientConnection serviceConn)
	{
		if (serviceConn != null)
		{
			try
			{
				serviceConn.closeConnection();
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);// this was the last attempt to close
			}
		}
	}

	private int getRemoteSize(URL url)
	{
		// TODO: this will dissapear when the NGDesktop Service will handle tar.zip / installers for Mac/Linux
		// for linux / mac; for now just hardcoding the number of tar entries in the remote archive
		if (url.toString().indexOf("linux") > 0)
		{
			return 78;
		}
		return 334; // Mac tar entries;
	}

	private void createTar(InputStream is, File destination, IDialogSettings exportSettings, IProgressMonitor monitor)
	{
		try (TarArchiveInputStream tarIS = new TarArchiveInputStream(new GzipCompressorInputStream(is));
			OutputStream fileOutputStream = new FileOutputStream(destination);
			ArchiveOutputStream os = new TarArchiveOutputStream(new GzipCompressorOutputStream(fileOutputStream));)
		{
			((TarArchiveOutputStream)os).setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
			((TarArchiveOutputStream)os).setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

			ArchiveEntry entry = tarIS.getNextTarEntry();
			while (entry != null)
			{
				monitor.worked(1);
				if (entry.getName().endsWith("/servoy.json"))
				{
					byte[] bytes = updateSettings(tarIS, exportSettings);
					((TarArchiveEntry)entry).setSize(bytes.length);
					os.putArchiveEntry(entry);
					os.write(bytes, 0, bytes.length);
				}
				else
				{
					os.putArchiveEntry(entry);
					IOUtils.copy(tarIS, os);
				}
				os.closeArchiveEntry();
				entry = tarIS.getNextTarEntry();
				if (monitor.isCanceled())
				{
					break;
				}
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
		}
	}

	private byte[] updateSettings(TarArchiveInputStream tarIS, IDialogSettings exportSettings)
	{
		// the exportSettings is already validated at this point;
		String url = exportSettings.get("app_url");
		String width = exportSettings.get("ngdesktop_width");
		String height = exportSettings.get("ngdesktop_height");
		String jsonFile = Utils.getTXTFileContent(tarIS, Charset.forName("UTF-8"), false);
		JSONObject configFile = new JSONObject(jsonFile);
		JSONObject options = (JSONObject)configFile.get("options");
		options.put("url", url);
		if (width.length() > 0) options.put("width", width);
		if (height.length() > 0) options.put("height", height);
		configFile.put("options", options);
		return configFile.toString().getBytes(Charset.forName("UTF-8"));
	}

	@Override
	public void addPages()
	{
		exportPage = new ExportPage(this);
		addPage(exportPage);
	}

	private StringBuilder validate(IDialogSettings settings)
	{
		StringBuilder errorMsg = new StringBuilder();
		boolean winPlatform = settings.getBoolean("win_export");
		boolean osxPlatform = settings.getBoolean("osx_export");
		boolean linuxPlatform = settings.getBoolean("linux_export");
		if (!(winPlatform || osxPlatform || linuxPlatform))
		{
			errorMsg.append("At least one platform must be selected");
		}
		String value = settings.get("save_dir");
		if (value.length() == 0)
		{
			errorMsg.append("Export path must to be specified");
			return errorMsg;
		}

		File myFile = new File(settings.get("icon_path"));
		if (myFile.exists() && myFile.isFile() && myFile.length() > LOGO_SIZE * 1024)
		{
			errorMsg.append("Logo file exceeds the maximum allowed limit (" + LOGO_SIZE * 1024 + " KB): " + myFile.length());
			return errorMsg;
		}

		myFile = new File(settings.get("image_path"));
		if (myFile.exists() && myFile.isFile() && myFile.length() > LOGO_SIZE * 1024)
		{
			errorMsg.append("Image file exceeds the maximum allowed limit (" + IMG_SIZE * 1024 + " KB): " + myFile.length());
			return errorMsg;
		}

		value = settings.get("copyright");
		if (value.toCharArray().length > COPYRIGHT_LENGTH)
		{
			errorMsg.append("Copyright string exceeds the maximum allowed limit (" + COPYRIGHT_LENGTH + " chars): " + value.toCharArray().length);
			return errorMsg;
		}

		try
		{
			int intValue;
			value = settings.get("ngdesktop_width");
			if (value.length() > 0)
			{
				intValue = Integer.parseInt(value);
				if (intValue <= 0)
				{
					errorMsg.append("Invalid width size: " + value);
					return errorMsg;
				}
			}
			value = settings.get("ngdesktop_height");
			if (value.length() > 0)
			{
				intValue = Integer.parseInt(value);
				if (intValue <= 0)
				{
					errorMsg.append("Invalid height size: " + value);
					return errorMsg;
				}
			}
		}
		catch (NumberFormatException e)
		{
			errorMsg.append("NumberFormatException: " + e.getMessage());
		}
		return errorMsg;
	}
}