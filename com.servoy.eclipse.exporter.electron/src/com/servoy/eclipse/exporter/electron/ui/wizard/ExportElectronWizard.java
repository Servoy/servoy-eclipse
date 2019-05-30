package com.servoy.eclipse.exporter.electron.ui.wizard;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.apache.http.client.methods.HttpPost;
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
		final File outputFile = new File(exportPage.getSaveDir(), solutionName);
		final String packageType = exportPage.getSelectedPackageType();

		final String platformType = exportPage.getSelectedPlatform();
		final boolean isPermanent = exportPage.getIsPermanent();
		final StringBuilder errorMsg = new StringBuilder();
		IRunnableWithProgress job = new IRunnableWithProgress()
		{
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				
				String buildOptionsParams = platformType.charAt(0) + "/" +  packageType + "/" + String.valueOf(isPermanent);
				monitor.beginTask("Generating and downloading electron application ...", 3);
				HttpClient httpclient = HttpClients.createDefault();
				HttpPost httpPost = new HttpPost("http://localhost:8091/NGDesktopWS/build/" + buildOptionsParams);

//				HttpGet httpget = new HttpGet("https://s3.eu-central-1.amazonaws.com/s3-artifactory-jenkins-team3/windows/NGDesktop_Installer.exe");

				// execute the request
				try
				{
					HttpResponse response = httpclient.execute(httpPost);
					monitor.worked(1);
	
					HttpEntity responseEntity = response.getEntity();
					if (response.getStatusLine().getStatusCode() == 200)
					{	
						waitForEndpointsAndDownload(getStringFromInputStream(responseEntity.getContent(), errorMsg), errorMsg, outputFile);
					}
					else
					{
						errorMsg.append("HTTP error code : " + response.getStatusLine().getStatusCode());
					}
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
	
	private String getStringFromInputStream(InputStream is, StringBuilder sb){
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;
		
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is))) {	
			try {
				while ((line = bufferedReader.readLine()) != null) {
					stringBuilder.append(line);
				}
			}
			catch (IOException e) {
				sb.append(e.getMessage());
				e.printStackTrace();
			}
		}
		catch (IOException e1) {
			sb.append(e1.getMessage());
			e1.printStackTrace();
		}
		return stringBuilder.toString();
	}
	
	private void waitForEndpointsAndDownload(String token, StringBuilder errorMsg, File outputFile ) {
		
		HttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet("http://localhost:8091/NGDesktopWS/status/" + token);

		try
		{
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity responseEntity = response.getEntity();

			if (response.getStatusLine().getStatusCode() == 200)
			{
				String resp = getStringFromInputStream(responseEntity.getContent(), errorMsg);
				if(resp.equals("Build not finished yet"))
				{
					System.out.println("Wait 5 sec then check again...");
					wait(5000);
					System.out.println("Checking again...");
					waitForEndpointsAndDownload(token, errorMsg, outputFile);
					return;
				}
				else {
					//resp == endpoint, we have to download it.
					FileOutputStream fos = new FileOutputStream(outputFile);
					
					Utils.streamCopy(responseEntity.getContent(), fos);
					fos.flush();
					Utils.close(responseEntity.getContent());
					ExportElectronWizard.insertApplicationURL(outputFile, exportPage.getApplicationURL());
				}
			}
		}
		catch(Exception ex)
		{
			errorMsg.append(ex.toString());
		}

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