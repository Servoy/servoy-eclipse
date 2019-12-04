package com.servoy.eclipse.exporter.ngdesktop.ui.wizard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;

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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.debug.handlers.StartNGDesktopClientHandler;
import com.servoy.eclipse.exporter.ngdesktop.Activator;
import com.servoy.eclipse.exporter.ngdesktop.wsclient.NgDesktopClientConnection;
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
	private static int POLLING_INTERVAL = 1000;
	public final static int LOGO_SIZE = 256; //KB;
	public final static int IMG_SIZE = 512;  //KB;
	public final static int COPYRIGHT_LENGTH = 128; //chars

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

	private byte[] updateSettings(TarArchiveInputStream tarIS, IDialogSettings exportSettings)
	{	//this is just for /servoy.son file
		//the exportSettings is already validated at this point;
		String url = exportSettings.get("app_url");
		String width = exportSettings.get("ngdesktop_width");
		String height = exportSettings.get("ngdesktop_height");
		String jsonFile = Utils.getTXTFileContent(tarIS, Charset.forName("UTF-8"), false);
		JSONObject configFile = new JSONObject(jsonFile);
		JSONObject options = (JSONObject)configFile.get("options");
		//put url and other options
		options.put("url", url);
		if (width.length() > 0) options.put("width", width);
		if (height.length() > 0) options.put("height", height);
		configFile.put("options", options);
		return configFile.toString().getBytes(Charset.forName("UTF-8"));
	}

	private void createTar(InputStream is, File destination, IDialogSettings exportSettings)
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
				if (entry.getName().endsWith("/servoy.json")) {
					byte[] bytes = updateSettings(tarIS, exportSettings);
					((TarArchiveEntry)entry).setSize(bytes.length);
					os.putArchiveEntry(entry);
					os.write(bytes, 0, bytes.length);
				} else {
					os.putArchiveEntry(entry);
					IOUtils.copy(tarIS, os);
				}
				os.closeArchiveEntry();
				entry = tarIS.getNextTarEntry();
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
		}
	}

	
	private StringBuilder validate(IDialogSettings settings) {
		//at this point there is no null value; not specified means we are receiving empty strings;
		//at least one platform must be selected
		//save path must to be specified
		//files and copyright (if specified) and exists - must not exceed the limits
		//the other unspecified setting will remain to ngdesktop service defaults
		StringBuilder errorMsg = new StringBuilder();
		boolean winPlatform = settings.getBoolean("win_export");
		boolean osxPlatform = settings.getBoolean("osx_export");
		boolean linuxPlatform = settings.getBoolean("linux_export");
		if (!(winPlatform || osxPlatform || linuxPlatform)) {
			errorMsg.append ("At least one platform must be selected");
		}
		String value = settings.get("save_dir");
		if (value.length() == 0) {
			errorMsg.append ("Export path must to be specified");
			return errorMsg;
		}
		
		File myFile = new File(settings.get("icon_path"));
		if (myFile.exists() && myFile.isFile() && myFile.length() > LOGO_SIZE * 1024) {
			errorMsg.append ("Logo file exceeds the maximum allowed limit (" + LOGO_SIZE * 1024 + " KB): " + myFile.length());
			return errorMsg;
		}
		
		myFile = new File(settings.get("image_path"));
		if (myFile.exists() && myFile.isFile() && myFile.length() > LOGO_SIZE * 1024) {
			errorMsg.append ("Image file exceeds the maximum allowed limit (" + IMG_SIZE * 1024 + " KB): " + myFile.length());
			return errorMsg;
		}
		
		value = settings.get("copyright");
		if (value.toCharArray().length > COPYRIGHT_LENGTH) {
			errorMsg.append ("Copyright string exceeds the maximum allowed limit (" + COPYRIGHT_LENGTH + " chars): " + value.toCharArray().length);
			return errorMsg;
		} 
		
		try {
			int intValue;
			value = settings.get("ngdesktop_width");
			if (value.length() > 0) {
				intValue = Integer.parseInt(value);
				if (intValue <= 0) {
					errorMsg.append("Invalid width size: " + value);
					return errorMsg;
				}
			}
			value = settings.get("ngdesktop_height");
			if (value.length() > 0) {
				intValue = Integer.parseInt(value);
				if (intValue <= 0) {
					errorMsg.append("Invalid height size: " + value);
					return errorMsg;
				}
			}
		} catch (NumberFormatException e) {
			errorMsg.append("NumberFormatException: " + e.getMessage());
		}
		return errorMsg;
	}

	@Override
	public boolean performFinish()
	{
		exportPage.saveState();
		IDialogSettings ngdesktopSettings = this.getDialogSettings();
		final StringBuilder errorMsg = validate(ngdesktopSettings);
		if (errorMsg.length() > 0) {
			MessageDialog.openError(UIUtils.getActiveShell(), "NG Desktop Export", errorMsg.toString());
			return false;
		}
		

		final IDialogSettings exportSettings = ngdesktopSettings;
		
		//TODO END refactoring

		IRunnableWithProgress job = new IRunnableWithProgress()
		{
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				if (errorMsg.length() > 0) return;
				String tmpDir = exportSettings.get("save_dir").replaceAll("\\\\", "/");
				final String saveDir = tmpDir.endsWith("/") ? tmpDir : tmpDir + "/";				
				exportPage.getSelectedPlatforms().forEach((platform) -> {

					if (platform.equals(ExportPage.WINDOWS_PLATFORM)) //skip mac and linux for now
					{
						NgDesktopClientConnection serviceConn = null;
						try
						{
							serviceConn = new NgDesktopClientConnection();
							String tokenId = serviceConn.startBuild(platform, exportSettings);
							int status = serviceConn.getStatus(tokenId);
							while (NgDesktopClientConnection.READY != status)
							{//TODO: handling waiting times due to unresponsive server
								Thread.sleep(POLLING_INTERVAL);
								status = serviceConn.getStatus(tokenId);

								//TODO: progress bar
								setInstallerStatus(serviceConn.getStatusMessage(), true);
								//end TODO

								if (NgDesktopClientConnection.WAITING == status || NgDesktopClientConnection.PROCESSING == status) continue;
								if (NgDesktopClientConnection.ERROR == status)
								{
									String errorMessage = serviceConn.getStatusMessage();
									errorMsg.append(errorMessage);
									setInstallerStatus(status + ": " + errorMessage, false);
									break;
								}
								if (NgDesktopClientConnection.NOT_FOUND == status)
								{
									errorMsg.append("Build does not exist: " + tokenId);
									setInstallerStatus("Build does not exist: " + tokenId, false);
									break;
								}
							}
							if (NgDesktopClientConnection.READY == status)
							{
								String binaryName = serviceConn.getBinaryName(tokenId);
								setInstallerStatus("Exporting " + binaryName, true);
								serviceConn.download(tokenId, saveDir);
							}
						}
						catch (IOException | InterruptedException e) {
							errorMsg.append(e.getMessage());
						} 
						finally {
							if (serviceConn != null) {
								try { //(try to) close the connection
									serviceConn.closeConnection();
								} catch (IOException e) {
									errorMsg.append(e.getMessage());
								}
							}
						}
					}
					else
					{//download the tar.gz archives

						String archiveName = StartNGDesktopClientHandler.NG_DESKTOP_APP_NAME + "-" + StartNGDesktopClientHandler.NGDESKTOP_VERSION + "-" +
							platform;
						String extension = ".tar.gz"; //for windows we have a binary installer
						File archiveFile = new File(saveDir + archiveName + extension);
						archiveFile.getParentFile().mkdirs();
						try
						{
							if (archiveFile.exists()) archiveFile.delete();
							URL fileUrl = new URL(StartNGDesktopClientHandler.DOWNLOAD_URL + archiveName + extension);
							
							setInstallerStatus("Exporting: " + archiveName + extension + " ...", false);
							
							createTar(fileUrl.openStream(), archiveFile, exportSettings);
						}
						catch (IOException e)
						{
							ServoyLog.logError(e);
						}
					}
				});
				setInstallerStatus("Done...", false);
				monitor.worked(1);
			}
		};
		try
		{
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
			return false;
		}
		else
		{
			MessageDialog.openInformation(UIUtils.getActiveShell(), "NG Desktop Export", "Export done successfully!");
			return true;
		}

	}
	
	@Override
	public void addPages()
	{
		exportPage = new ExportPage(this);
		addPage(exportPage);
	}

	/* TODO: delete after a progress bar will be in place */
	public void setInstallerStatus(String status, boolean displayTime)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			@SuppressWarnings("deprecation")
			public void run()
			{
				String timeStr = "";
				if (displayTime) {
					Date myDate = new Date(System.currentTimeMillis());
					int sec = myDate.getSeconds();
					String strSec = sec < 10 ? "0" + sec : Integer.toString(sec);
					timeStr = " - " + myDate.getHours() + ":" + myDate.getMinutes() + ":" + strSec;
				}
				exportPage.statusLabel.setText("Status (beta): " + status + timeStr);
			}
		});
	}
}