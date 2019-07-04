package com.servoy.eclipse.exporter.electron.ui.wizard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.debug.handlers.StartNGDesktopClientHandler;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.Debug;

/**
 * @author gboros
 */
public class ExportElectronWizard extends Wizard implements IExportWizard
{	
	private ExportPage exportPage;	
	private static int BUFFER_SIZE = 1024;	
	
	public ExportElectronWizard()
	{
		super();
		setWindowTitle("NG Desktop Export");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection)
	{

	}
	
	private CustomArchiveObject customizeEntry(TarArchiveInputStream tarIS, ArchiveEntry entry, String url, String platform) throws IOException{
		//TarArchiveInputStream doesn't implement reset(). So - we are forced to 
		//set the size for entry and write output with one single read from tarIS.
		if (entry == null) return null;
		if (entry.getName().endsWith("/servoy.json")) {
			List<String> lines = IOUtils.readLines(tarIS, StandardCharsets.UTF_8);
			int entrySize = 0;
			for (int index = 0; index < lines.size(); index ++) {
				String line = lines.get(index);
				if (line.trim().startsWith("\"url\":")) {
					line = "    \"url\": \"" + url + "\",";
					lines.set(index, line);
				}
				entrySize += line.getBytes().length + 1; 
			}
			if (!platform.equals(ExportPage.WINDOWS_PLATFORM))
				((TarArchiveEntry)entry).setSize(entrySize);
			return new CustomArchiveObject(entry, lines);
		}
		return new CustomArchiveObject(entry, null);
	}
	
	private void updateUrl(File source, File destination, String url, String platform, String archiveName) throws IOException, ArchiveException {
		FileInputStream fis = new FileInputStream(source);
		TarArchiveInputStream tarIS = new TarArchiveInputStream(new GzipCompressorInputStream(fis));
		 
		OutputStream fileOutputStream = new FileOutputStream(destination);
	    ArchiveOutputStream os = null;;
	    
	    if (!platform.contentEquals(ExportPage.WINDOWS_PLATFORM)) {
	    	os = new TarArchiveOutputStream(new GzipCompressorOutputStream(fileOutputStream));
	    	((TarArchiveOutputStream)os).setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
	    	((TarArchiveOutputStream)os).setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
	    } else {
	    	os = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, fileOutputStream);
	    }
		 
	    ArchiveEntry entry = tarIS.getNextTarEntry();
	    while (entry != null) {
	    	if (platform.equals("win")) {
   				entry = tarIS.available() > 0 ? new ZipArchiveEntry(entry.getName().substring(entry.getName().indexOf(archiveName))) : null;
    		}
	    	CustomArchiveObject caro = customizeEntry(tarIS, entry, url, platform);
	    	if (caro != null && caro.getEntry() != null) {
	    		os.putArchiveEntry(caro.getEntry());
	    		if (caro.getLines() != null) {
	    			for (String line: caro.getLines()) {
		    			IOUtils.write(line.getBytes(), os);
		    			IOUtils.write("\n".getBytes(), os);
	    			}
	    		} else {
	    			IOUtils.copy(tarIS, os);
	    		}
	    		os.closeArchiveEntry();
	    	}
	    	entry = tarIS.getNextTarEntry();
	    }
	    tarIS.close();
	    os.close();
	}
	
	@Override
	public boolean performFinish()
	{
		List<String> selectedPlatforms = exportPage.getSelectedPlatforms();
		final String saveDir = exportPage.getSaveDir().endsWith(File.separator) ? exportPage.getSaveDir() : exportPage.getSaveDir() + File.separator;
		final String appUrl = exportPage.getApplicationURL();
		final StringBuilder errorMsg = new StringBuilder();
		IRunnableWithProgress job = new IRunnableWithProgress()
		{	
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				selectedPlatforms.forEach((platform) -> {
					String archiveName = StartNGDesktopClientHandler.NG_DESKTOP_APP_NAME + "-" + StartNGDesktopClientHandler.NGDESKTOP_VERSION + "-" + platform ;
					String extension = ((platform.equals(ExportPage.WINDOWS_PLATFORM)) ? ".zip" : ".tar.gz.tmp");
					File sourceArchiveFile = new File(saveDir + archiveName + ".tar.gz");
					File targetArchiveFile = new File(saveDir + archiveName + extension);
					
					sourceArchiveFile.getParentFile().mkdirs();

					try {
						//download sources
						if (sourceArchiveFile.exists()) sourceArchiveFile.delete();
						URL fileUrl = new URL(StartNGDesktopClientHandler.DOWNLOAD_URL + archiveName + ".tar.gz");
						InputStream is = fileUrl.openStream();
						FileOutputStream os = new FileOutputStream(sourceArchiveFile);
						int n = 0;
						byte buffer[] = new byte[BUFFER_SIZE];
						while ((n = is.read(buffer, 0, BUFFER_SIZE)) != -1) {
							os.write(buffer, 0, n);
						}
						is.close();
						os.close();
						
						//update archive
						updateUrl(sourceArchiveFile, targetArchiveFile, appUrl, platform, archiveName);
						sourceArchiveFile.delete();
						if (!platform.equals(ExportPage.WINDOWS_PLATFORM))
							targetArchiveFile.renameTo(sourceArchiveFile);//for windows this has no effect - tmpArchiveFile does not exists
					} catch (IOException | ArchiveException e) {
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
		if(errorMsg.length() > 0)
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
	
	class CustomArchiveObject {
		private List<String> lines = null;
		private ArchiveEntry entry = null;
		
		protected ArchiveEntry getEntry() {
			return entry;
		}

		public CustomArchiveObject(ArchiveEntry entry, List<String> lines) {
			super();
			this.lines = lines;
			this.entry = entry;
		}
				
		protected List<String> getLines() {
			return lines;
		}
	}

}