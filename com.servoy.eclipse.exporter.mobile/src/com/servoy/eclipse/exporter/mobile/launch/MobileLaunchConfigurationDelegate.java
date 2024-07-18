package com.servoy.eclipse.exporter.mobile.launch;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.DeletingPathVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.internal.browser.BrowserManager;
import org.eclipse.ui.internal.browser.ExternalBrowserInstance;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;
import org.eclipse.ui.internal.browser.Messages;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.exporter.mobile.Activator;
import com.servoy.eclipse.model.mobile.exporter.MobileExporter;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;


public class MobileLaunchConfigurationDelegate extends LaunchConfigurationDelegate
{

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException
	{
		String browserID = configuration.getAttribute(IMobileLaunchConstants.BROWSER_ID, "");

		MobileExporter exporter = new MobileExporter();

		File tmpExportFolder = null;
		try
		{
			// temporary directory where the .war file will be exported; it should be moved afterwards to IMobileLaunchConstants.WAR_LOCATION / Tomcat webapps.
			// this is needed because otherwise Tomcat can detect and try to deploy a partially written .war file giving invalid archive exceptions
			tmpExportFolder = File.createTempFile("servoytempwd", "");
			if (tmpExportFolder.exists())
			{
				try
				{
					Files.walkFileTree(tmpExportFolder.toPath(), DeletingPathVisitor.withLongCounters());
				}
				catch (IOException e)
				{
					Debug.error(e);
				}
			}
			tmpExportFolder.mkdir();

			prepareExporter(exporter, tmpExportFolder, configuration, launch, monitor);
			if (monitor != null && monitor.isCanceled()) return;

			if (monitor != null) monitor.subTask("exporting mobile solution");

			exporter.doExport(false);

			if (monitor != null && monitor.isCanceled()) return;
			if (monitor != null) monitor.subTask("deploying mobile solution)");

			// there are Tomcat deploy/undeploy ant tasks that use the Tomcat Manager URLs to do the job, and we could do the same here
			// but that would mean adding a user with the manager rights to Tomcat user list, so currently we do it by looking at the file-system
			// and doing a delete/copy to undeploy and then redeploy (initially we did only copy for redeploy but it wasn't clear enough then starting with Tomcat 7 when the deployment was done)

			// we don't just copy but also undeploy first as in Tomcat >= 7 that sometimes resulted in partially deletion of contents and failed deployment - probably due to also accessing contents during deployment as timeouts could not be tuned ok

			File warFinalLocation = getWarExportDir(configuration);
			File tmpWarFile = tmpExportFolder.listFiles()[0]; // the temp dir should now contain a single .war file - otherwise we can accept a RuntimeException

			String dirName = tmpWarFile.getName().substring(0, tmpWarFile.getName().length() - 4); // drop the ".war" extension
			File finalWarFile = new File(warFinalLocation, tmpWarFile.getName());
			File finalWarDir = new File(warFinalLocation, dirName);

			// we initially traced actual deployment folder, but since Tomcat 7 that was not enough, there was an additional delay until app. was accessible, so
			// now we track the internal Tomcat created one;
			File warDeploymentTrackingDir = new File(warFinalLocation, "../work/Catalina/localhost/" + dirName);

			beforeWaitForDeployment(monitor, configuration);

			// actual deployment/move
			long startTimestamp = System.currentTimeMillis();

			int maxWaitTime;
			try
			{
				maxWaitTime = Integer.parseInt(configuration.getAttribute(IMobileLaunchConstants.MAX_WAR_DEPLOYMENT_TIME,
					IMobileLaunchConstants.DEFAULT_MAX_WAR_DEPLOYMENT_TIME));
			}
			catch (NumberFormatException ex)
			{
				maxWaitTime = Integer.parseInt(IMobileLaunchConstants.DEFAULT_MAX_WAR_DEPLOYMENT_TIME);
			}
			maxWaitTime = maxWaitTime * 1000;

			// undeploy if needed
			if (finalWarFile.exists() || finalWarDir.exists())
			{
				if (monitor != null && monitor.isCanceled()) return;
				if (monitor != null) monitor.subTask("deploying mobile solution; undeploying previous version");
				if (finalWarFile.exists())
				{
					try
					{
						Files.walkFileTree(finalWarFile.toPath(), DeletingPathVisitor.withLongCounters());
					}
					catch (IOException e)
					{
						Debug.error(e);
					}
				}
				else
				{
					try
					{
						// this could happen if we deleted the war file but undeploy failed once
						Files.walkFileTree(finalWarDir.toPath(), DeletingPathVisitor.withLongCounters());
					}
					catch (IOException e)
					{
						Debug.error(e);
					}
				}

				while ((warDeploymentTrackingDir.exists() || finalWarDir.exists()) && (System.currentTimeMillis() - startTimestamp) < maxWaitTime)
				{
					// wait for mobile war to be deployed
					try
					{
						Thread.sleep(50);
					}
					catch (InterruptedException e)
					{
						ServoyLog.logError(e);
					}
					if (monitor != null && monitor.isCanceled()) return;
				}
				if (warDeploymentTrackingDir.exists()) throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Timed out undeploying old war - after: " + (maxWaitTime / 1000) + " seconds."));
			}

			// deploy
			if (monitor != null && monitor.isCanceled()) return;
			if (monitor != null) monitor.subTask("deploying mobile solution; waiting for deployment");

			FileUtils.moveFileToDirectory(tmpWarFile, warFinalLocation, false);

			while (!warDeploymentTrackingDir.exists() && (System.currentTimeMillis() - startTimestamp) < maxWaitTime)
			{
				// wait for mobile war to be deployed
				try
				{
					Thread.sleep(50);
				}
				catch (InterruptedException e)
				{
					ServoyLog.logError(e);
				}
				if (monitor != null && monitor.isCanceled()) return;
			}
			if (!warDeploymentTrackingDir.exists()) throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "War deployment timeout after: " +
				(maxWaitTime / 1000) + " seconds."));

			// make sure deployed war is available over HTTP - the file system checks are not enough...
			if (!httpCheckOK(monitor, startTimestamp, maxWaitTime, getApplicationURL(configuration)) && monitor == null || monitor.isCanceled())
				throw new CoreException(
					new Status(IStatus.ERROR, Activator.PLUGIN_ID, "War deployment http check failed after: " + (maxWaitTime / 1000) + " seconds."));
			if (monitor != null && monitor.isCanceled()) return;
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unexpected error: " + e.getMessage()));
		}
		finally
		{
			if (tmpExportFolder != null && tmpExportFolder.exists())
			{
				try
				{
					// this could happen if we deleted the war file but undeploy failed once
					Files.walkFileTree(tmpExportFolder.toPath(), DeletingPathVisitor.withLongCounters());
				}
				catch (IOException e)
				{
					Debug.error(e);
				}

			}
		}

		//open browser
		IWebBrowser webBrowser = getBrowser(browserID);
		if (webBrowser == null)
		{
			webBrowser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
		}
		openBrowser(webBrowser, launch, configuration, monitor);
	}

	protected boolean httpCheckOK(IProgressMonitor monitor, long startTimestamp, int maxWaitTime, String url)
	{
		if (monitor != null && monitor.isCanceled()) return false;
		if (monitor != null) monitor.subTask("deploying mobile solution; verifying deployment");
		boolean httpOK = testHTTPOKCode(url);

		while (!httpOK && (System.currentTimeMillis() - startTimestamp) < maxWaitTime)
		{
			// wait for mobile war to be deployed
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				ServoyLog.logError(e);
			}
			if (monitor != null && monitor.isCanceled()) return false;
			httpOK = testHTTPOKCode(url);
		}

		return httpOK;
	}

	private boolean testHTTPOKCode(String url)
	{
		HttpURLConnection httpConnection = null;
		try
		{
			URLConnection conn = (new URL(url)).openConnection();
			httpConnection = (HttpURLConnection)conn;

			// Set up standard connection characteristics
			httpConnection.setAllowUserInteraction(false);
			httpConnection.setDoInput(true);
			httpConnection.setUseCaches(false);
			httpConnection.setDoOutput(false);
			httpConnection.setRequestMethod("GET");
			httpConnection.setRequestProperty("User-Agent", "Servoy Availibility Checker 0.1");
			httpConnection.setConnectTimeout(5000);
			httpConnection.connect();
			return httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK;
		}
		catch (IOException e)
		{
			ServoyLog.logWarning("While checking for HTTP availability of war deployment: ", e);
		}
		finally
		{
			if (httpConnection != null) httpConnection.disconnect();
		}
		return false;
	}

	protected void beforeWaitForDeployment(IProgressMonitor monitor, ILaunchConfiguration configuration) throws CoreException
	{
		boolean nodebug = Boolean.valueOf(configuration.getAttribute(IMobileLaunchConstants.NODEBUG, "true")).booleanValue();
		if (!nodebug)
		{
			if (monitor != null) monitor.subTask("switching to service solution (so that it can be debugged)");
			String solutionName = configuration.getAttribute(IMobileLaunchConstants.SOLUTION_NAME, "");
			String serviceSolutionName = configuration.getAttribute(IMobileLaunchConstants.SERVICE_SOLUTION, solutionName + "_service");
			ServoyProject serviceProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(serviceSolutionName);
			if (serviceProject != null)
			{
				ServoyModelManager.getServoyModelManager().getServoyModel().setActiveProject(serviceProject, true);
			}
		}
	}

	private File getWarExportDir(ILaunchConfiguration configuration) throws CoreException
	{
		File warExportDir = null;
//		String warLocation = configuration.getAttribute(IMobileLaunchConstants.WAR_LOCATION, ""); // this is currently never set; if we want to add it to the launcher organize dialog in the future, it might be set; see StartMobileClientActionDelegate
//		if (warLocation.length() == 0)
//		{
		warExportDir = new File(ApplicationServerRegistry.get().getServoyApplicationServerDirectory(), "server/webapps");
//		}
//		else
//		{
//			warExportDir = new File(warLocation);
//		}
		return warExportDir;
	}

	protected void openBrowser(IWebBrowser webBrowser, ILaunch launch, ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException
	{
		if (monitor != null) monitor.subTask("opening mobile client in browser");
		try
		{
			EditorUtil.openURL(webBrowser, getApplicationURL(configuration));
		}
		catch (final Throwable e)
		{
			ServoyLog.logError("Cant open external browser", e);
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					MessageDialog.openError(UIUtils.getActiveShell(), "Cant open external browser", e.getLocalizedMessage());
				}
			});
		}
	}

	protected String getApplicationURL(ILaunchConfiguration configuration) throws CoreException
	{
		boolean nodebug = Boolean.valueOf(configuration.getAttribute(IMobileLaunchConstants.NODEBUG, "true")).booleanValue();
		String mobileClientURL = configuration.getAttribute(IMobileLaunchConstants.APPLICATION_URL, "");
		if (nodebug && !mobileClientURL.contains("nodebug=true"))
		{
			mobileClientURL = mobileClientURL + ((mobileClientURL.indexOf('?') != -1) ? '&' : '?') + "nodebug=true";
		}
		return mobileClientURL;
	}

	protected void prepareExporter(MobileExporter exporter, File exportFolder, ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor)
		throws CoreException
	{
		String solutionName = configuration.getAttribute(IMobileLaunchConstants.SOLUTION_NAME, "");
		String serverUrl = configuration.getAttribute(IMobileLaunchConstants.SERVER_URL, "");
		String serviceSolutionName = configuration.getAttribute(IMobileLaunchConstants.SERVICE_SOLUTION, (String)null);
		int timeout = Integer.valueOf(configuration.getAttribute(IMobileLaunchConstants.TIMEOUT, IMobileLaunchConstants.DEFAULT_TIMEOUT)).intValue();
		String company = configuration.getAttribute(IMobileLaunchConstants.COMPANY_NAME, "");
		String license = configuration.getAttribute(IMobileLaunchConstants.LICENSE_CODE, "");
		boolean validLicense = ApplicationServerRegistry.get().checkMobileLicense(company, license);

		exporter.setSolutionName(solutionName);
		exporter.setOutputFolder(exportFolder);
		exporter.setServerURL(serverUrl);
		exporter.setServiceSolutionName(serviceSolutionName);
		exporter.setTimeout(timeout);
		exporter.setSkipConnect(validLicense);
	}

	private static IBrowserDescriptor getBrowserDescriptor(String browserId)
	{
		try
		{
			for (IBrowserDescriptor ewb : BrowserManager.getInstance().getWebBrowsers())
			{
				org.eclipse.ui.internal.browser.IBrowserExt ext = null;
				if (ewb != null && !ewb.getName().equals(Messages.prefSystemBrowser))
				{
					//ext := "org.eclipse.ui.browser." + specifiId
					ext = org.eclipse.ui.internal.browser.WebBrowserUIPlugin.findBrowsers(ewb.getLocation());
					if (ext != null && ext.getId().equals(browserId))
					{
						return ewb;
					}
					else
					{

						if (ewb != null && ewb.getLocation() != null)
						{
							String name = ewb.getName().toLowerCase().replace(" ", "_");
							if (browserId.contains(name)) return ewb;
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	public static IWebBrowser getBrowser(String browserId)
	{
		IWebBrowser webBrowser = null;
		IBrowserDescriptor browserDescriptor = getBrowserDescriptor(browserId);
		if (browserDescriptor != null)
		{
			org.eclipse.ui.internal.browser.IBrowserExt ext = org.eclipse.ui.internal.browser.WebBrowserUIPlugin.findBrowsers(browserDescriptor.getLocation());
			if (ext != null && ext.getId().equals(browserId))
			{
				webBrowser = ext.createBrowser(ext.getId(), browserDescriptor.getLocation(), browserDescriptor.getParameters());
				if (webBrowser == null) webBrowser = new ExternalBrowserInstance(ext.getId(), browserDescriptor);
			}
			else
			{

				if (browserDescriptor.getLocation() != null)
				{
					String name = browserDescriptor.getName().toLowerCase().replace(" ", "_");
					if (browserId.contains(name)) webBrowser = new ExternalBrowserInstance("org.eclipse.ui.browser." + name, browserDescriptor);
				}
			}
		}
		return webBrowser;
	}
}