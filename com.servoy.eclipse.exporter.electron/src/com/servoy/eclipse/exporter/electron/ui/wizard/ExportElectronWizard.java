package com.servoy.eclipse.exporter.electron.ui.wizard;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 */
public class ExportElectronWizard extends Wizard implements IExportWizard
{
	private ExportPage exportPage;
	
	public ExportElectronWizard()
	{
		super();
		setWindowTitle("Electron Export");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		// TODO Auto-generated method stub	
	}

	@Override
	public boolean performFinish()
	{
		String solutionName = "NGDesktop_Installer"; //ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution().getName();
		final File outputFile = new File(exportPage.getSaveDir(), solutionName + ".exe");
		final String applicationURL = exportPage.getApplicationURL();
		final StringBuilder errorMsg = new StringBuilder();
		IRunnableWithProgress job = new IRunnableWithProgress()
		{
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				monitor.beginTask("Generating and downloading electron application ...", 3);
//				HttpClient httpclient = HttpClients.createDefault();
//				HttpGet httpget = new HttpGet("https://s3.eu-central-1.amazonaws.com/s3-artifactory-jenkins-team3/windows/NGDesktop_Installer.exe");

				// execute the request
				try
				{
//					HttpResponse response = httpclient.execute(httpget);
					monitor.worked(1);
	
//					HttpEntity responseEntity = response.getEntity();
//					if (response.getStatusLine().getStatusCode() == 200)
//					{
//						
//						FileOutputStream fos = new FileOutputStream(outputFile);
//						Utils.streamCopy(responseEntity.getContent(), fos);
//						fos.flush();
//						Utils.close(responseEntity.getContent());
						monitor.worked(1);
						ExportElectronWizard.insertApplicationURL(outputFile, applicationURL);
						monitor.worked(1);
						
//					}
//					else
//					{
//						errorMsg.append("HTTP error code : " + response.getStatusLine().getStatusCode());
//					}
				}
				catch(Exception ex)
				{
					errorMsg.append(ex.toString());
				}
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
			MessageDialog.openError(UIUtils.getActiveShell(), "Electron Export", errorMsg.toString());
			return false;
		}
		else
		{
			MessageDialog.openInformation(UIUtils.getActiveShell(), "Electron Export", "Export done successfully!");
			return true;
		}
	}
	
	@Override
	public void addPages()
	{
		exportPage = new ExportPage(this);
		addPage(exportPage);
	}
	
	
	private static void insertApplicationURL(File outputFile, String applicationURL) throws IOException
	{
	    Path zipFilePath = Paths.get(outputFile.getAbsolutePath());
	    try (FileSystem fs = FileSystems.newFileSystem(zipFilePath, null))
	    {
	        Path source = fs.getPath("/resources/app/config/servoy.json");
	        Path temp = fs.getPath("/resources/app/config/servoy.json.temp");
	        if (Files.exists(temp))
	        {
	            throw new IOException("temp file exists, generate another name");
	        }
	        Files.move(source, temp);
	        streamCopy(temp, source, applicationURL);
	        Files.delete(temp);
	    }
	}

	private static void streamCopy(Path src, Path dst, String applicationURL) throws IOException
	{
		 try (BufferedReader br = new BufferedReader(
		            new InputStreamReader(Files.newInputStream(src)));
		         BufferedWriter bw = new BufferedWriter(
		            new OutputStreamWriter(Files.newOutputStream(dst))))
		 {
		        String line;
		        while ((line = br.readLine()) != null)
		        {
		            line = line.replace("https://demo.servoy.com/solutions/sampleGallery/index.html", applicationURL);
		            bw.write(line);
		            bw.newLine();
		        }
		  }
	}
}