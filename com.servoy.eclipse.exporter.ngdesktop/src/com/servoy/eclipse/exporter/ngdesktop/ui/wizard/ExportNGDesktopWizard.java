package com.servoy.eclipse.exporter.ngdesktop.ui.wizard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

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
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.debug.handlers.StartNGDesktopClientHandler;
import com.servoy.eclipse.exporter.ngdesktop.Activator;
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
	private static int BUFFER_SIZE = 1024;

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

	@Override
	public boolean performFinish()
	{
		exportPage.saveState();
		List<String> selectedPlatforms = exportPage.getSelectedPlatforms();
		final String saveDir = exportPage.getSaveDir().endsWith(File.separator) ? exportPage.getSaveDir() : exportPage.getSaveDir() + File.separator;
		final String appUrl = exportPage.getApplicationURL();
		final StringBuilder errorMsg = new StringBuilder();
		IRunnableWithProgress job = new IRunnableWithProgress()
		{
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				selectedPlatforms.forEach((platform) -> {
					String archiveName = StartNGDesktopClientHandler.NG_DESKTOP_APP_NAME + "-" + StartNGDesktopClientHandler.NGDESKTOP_VERSION + "-" + platform;
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
}