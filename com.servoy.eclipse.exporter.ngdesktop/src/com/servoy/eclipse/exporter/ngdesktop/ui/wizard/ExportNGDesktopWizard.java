package com.servoy.eclipse.exporter.ngdesktop.ui.wizard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
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

	public static final String NGDESKTOP_SERVICE_PROTOCOL = "http://";

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

	private byte[] getReplacement(TarArchiveInputStream tarIS, ArchiveEntry entry, String url)
	{
		if (entry != null && entry.getName().endsWith("/servoy.json"))
		{
			String jsonFile = Utils.getTXTFileContent(tarIS, Charset.forName("UTF-8"), false);
			JSONObject configFile = new JSONObject(jsonFile);
			JSONObject options = (JSONObject)configFile.get("options");
			//put url and other options in servoy.json(we can put image also here, check servoy.json to see available options.
			options.put("url", url);
			configFile.put("options", options);
			return configFile.toString().getBytes(Charset.forName("UTF-8"));
		}
		return null;
	}

	private void createZip(InputStream is, File destination, String archiveName, String url)
	{
		try (TarArchiveInputStream tarIS = new TarArchiveInputStream(new GzipCompressorInputStream(is));
			OutputStream fileOutputStream = new FileOutputStream(destination);
			ArchiveOutputStream os = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, fileOutputStream);)
		{
			ArchiveEntry entry = tarIS.getNextTarEntry();
			while (entry != null)
			{
				// for windows need to use zip entries and not tar entries
				entry = tarIS.available() > 0 ? new ZipArchiveEntry(entry.getName().substring(entry.getName().indexOf(archiveName))) : null; // use relative path for entry
				if (entry != null)
				{
					os.putArchiveEntry(entry);
					byte[] bytes = getReplacement(tarIS, entry, url);
					if (bytes != null)
					{
						os.write(bytes, 0, bytes.length);
					}
					else
					{
						IOUtils.copy(tarIS, os);
					}
					os.closeArchiveEntry();
				}
				entry = tarIS.getNextTarEntry();
			}
		}
		catch (IOException | ArchiveException e)
		{
			ServoyLog.logError(e);
		}
	}

	private void createTar(InputStream is, File destination, String url)
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
				byte[] bytes = getReplacement(tarIS, entry, url);
				if (bytes != null)
				{
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
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
		}
	}


	private Map getRebrandingData(String filePath) throws Exception
	{
		String line;
		File rebrandingFile = new File(filePath);
		if (!rebrandingFile.exists())
		{
			throw new Exception("File does not exists:" + filePath);
		}
		Map<String, String> result = new HashMap<String, String>();
		BufferedReader reader = new BufferedReader(new FileReader(rebrandingFile));
		while ((line = reader.readLine()) != null)
		{
			if (line.startsWith("#") || line.trim().length() == 0) continue;
			String[] parts = line.split("=");
			if (parts.length != 2) throw new Exception("Invalid data format: " + filePath);
			String key = parts[0];
			String value = parts[1];
			result.put(key, value);
		}
		return result;
	}

	@Override
	public boolean performFinish()
	{
		exportPage.saveState();
		List<String> selectedPlatforms = exportPage.getSelectedPlatforms();
		final String saveDir = exportPage.getSaveDir().endsWith(File.separator) ? exportPage.getSaveDir() : exportPage.getSaveDir() + File.separator;
		final String appUrl = exportPage.getApplicationURL();

		final StringBuilder errorMsg = new StringBuilder();

		boolean downloadInstallers = false;

		//TODO: refactoring after getting data through exportPage
		//need declarations here - cause they will be reassigned to final vars for lamba processing below
		Map rebrandingData = null;
		String rebrIconPath = null;
		String rebrImagePath = null;
		String rebrCopyrightStr = null;
		InetAddress serviceAddress = null;
		int servicePort = 0;

		String rebrandingPath = System.getProperty("ngclient.rebranding.data", null);
		if (rebrandingPath != null)
		{
			rebrandingPath = rebrandingPath.replaceAll("\\\\", "/");
			downloadInstallers = true;
			try
			{
				rebrandingData = getRebrandingData(rebrandingPath);
				rebrIconPath = (String)rebrandingData.get("iconPath");
				rebrImagePath = (String)rebrandingData.get("imagePath");
				rebrCopyrightStr = (String)rebrandingData.get("copyrightStr");
				serviceAddress = InetAddress.getByName(((String)rebrandingData.get("serviceAddress")).trim());
				servicePort = Integer.parseInt((String)rebrandingData.get("servicePort"));
			}
			catch (Exception e)
			{
				errorMsg.append(e.getMessage());
			}

		}
		else
		{
			errorMsg.append("No rebranding data found");
		}

		//need final variables to be used in below interface implementation
		final String iconPath = rebrIconPath != null ? rebrIconPath : null;
		final String imagePath = rebrImagePath != null ? rebrImagePath : null;
		final String copyrightInfo = rebrCopyrightStr != null ? rebrCopyrightStr : null;
		final boolean downloadNgDesktopInstaller = downloadInstallers;
		final InetAddress ngDesktopService = serviceAddress;
		final int ngDesktopPort = servicePort;

		//TODO END refactoring

		IRunnableWithProgress job = new IRunnableWithProgress()
		{
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				if (errorMsg.length() > 0) return;
				selectedPlatforms.forEach((platform) -> {

					if (downloadNgDesktopInstaller && platform.equals(ExportPage.WINDOWS_PLATFORM)) //skip mac for now
					{
						try
						{
							NgDesktopClientConnection serviceConn = new NgDesktopClientConnection(NGDESKTOP_SERVICE_PROTOCOL, ngDesktopService, ngDesktopPort);

							String tokenId = serviceConn.startBuild(platform, iconPath, imagePath, copyrightInfo, appUrl);
							int status = serviceConn.getStatus(tokenId);
							while (NgDesktopClientConnection.READY != status)
							{//TODO: handling waiting times due to unresponsive server
								Thread.sleep(POLLING_INTERVAL);
								status = serviceConn.getStatus(tokenId);

								//TODO: progress bar
								setInstallerStatus(serviceConn.getStatusMessage());
								//end TODO

								if (NgDesktopClientConnection.WAITING == status || NgDesktopClientConnection.PROCESSING == status) continue;
								if (NgDesktopClientConnection.ERROR == status)
								{
									String errorMessage = serviceConn.getStatusMessage();
									errorMsg.append(errorMessage);
									setInstallerStatus(status + ": " + errorMessage);
									break;
								}
								if (NgDesktopClientConnection.NOT_FOUND == status)
								{
									errorMsg.append("Build does not exist: " + tokenId);
									setInstallerStatus("Build does not exist: " + tokenId);
									break;
								}
							}
							if (NgDesktopClientConnection.READY == status)
							{
								String binaryName = serviceConn.getBinaryName(tokenId);
								setInstallerStatus("downloading " + binaryName);
								serviceConn.download(tokenId, saveDir);
								setInstallerStatus("Done: " + saveDir + File.pathSeparator + binaryName);
							}
						}
						catch (IOException | InterruptedException e)
						{
							errorMsg.append(e.getMessage());
						}
					}
					else
					{//download the tar.gz archives

						String archiveName = StartNGDesktopClientHandler.NG_DESKTOP_APP_NAME + "-" + StartNGDesktopClientHandler.NGDESKTOP_VERSION + "-" +
							platform;
						String extension = ((platform.equals(ExportPage.WINDOWS_PLATFORM)) ? ".zip" : ".tar.gz");
						File archiveFile = new File(saveDir + archiveName + extension);
						archiveFile.getParentFile().mkdirs();
						try
						{
							if (archiveFile.exists()) archiveFile.delete();
							URL fileUrl = new URL(StartNGDesktopClientHandler.DOWNLOAD_URL + archiveName + ".tar.gz");
							if (platform.contentEquals(ExportPage.WINDOWS_PLATFORM))
							{
								createZip(fileUrl.openStream(), archiveFile, archiveName, appUrl);
							}
							else
							{
								createTar(fileUrl.openStream(), archiveFile, appUrl);
							}
						}
						catch (IOException e)
						{
							ServoyLog.logError(e);
						}
					}
				});
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

	/* Temporary hack to display a status. This will be delete after a progress bar will be implemented (IProgressMonitor) */
	public void setInstallerStatus(String status)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				Date myDate = new Date(System.currentTimeMillis());
				int sec = myDate.getSeconds();
				String strSec = sec < 10 ? "0" + sec : Integer.toString(sec);
				exportPage.tempLabelStatus.setText("Status (debug mode): " + status + " - " + myDate.getHours() + ":" + myDate.getMinutes() + ":" + strSec);
			}
		});
	}
}