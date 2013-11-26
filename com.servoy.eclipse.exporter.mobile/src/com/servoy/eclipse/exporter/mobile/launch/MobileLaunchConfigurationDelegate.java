package com.servoy.eclipse.exporter.mobile.launch;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.internal.browser.BrowserManager;
import org.eclipse.ui.internal.browser.ExternalBrowserInstance;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;
import org.eclipse.ui.internal.browser.Messages;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.exporter.mobile.Activator;
import com.servoy.eclipse.model.mobile.exporter.MobileExporter;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;


public class MobileLaunchConfigurationDelegate extends LaunchConfigurationDelegate
{

	/**
	 * 1000 was too low - for test deployment I had a few times ~1200 sec time between timestamp changes during deployment...
	 */
	private static final int DEPLOYMENT_FINISH_SILENCE_PERIOD = 2000;

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
			if (tmpExportFolder.exists()) FileUtils.deleteQuietly(tmpExportFolder);
			tmpExportFolder.mkdir();

			prepareExporter(exporter, tmpExportFolder, configuration, launch, monitor);
			if (monitor != null && monitor.isCanceled()) return;

			if (monitor != null) monitor.subTask("exporting mobile solution");

			exporter.doExport(false);

			if (monitor != null && monitor.isCanceled()) return;
			if (monitor != null) monitor.subTask("deploying mobile solution");

			File warFinalLocation = getWarExportDir(configuration);
			File tmpWarFile = tmpExportFolder.listFiles()[0]; // the temp dir should now contain a single .war file - otherwise we can except a RuntimeException
			File warContextFolder = new File(warFinalLocation, tmpWarFile.getName().substring(0, tmpWarFile.getName().length() - 4)); // drop the ".war" extension
			File finalWarFile = new File(warFinalLocation, tmpWarFile.getName());

			boolean deployed = false;
			long initialModificationTimestampOfWarContextFolder = warContextFolder.exists() ? warContextFolder.lastModified() : 0;

			// actual deployment/move
			if (finalWarFile.exists()) FileUtils.deleteQuietly(finalWarFile); // because moveFileToDirectory fails otherwise
			FileUtils.moveFileToDirectory(tmpWarFile, warFinalLocation, false);

			beforeWaitForDeployment(monitor, configuration);

			if (monitor != null && monitor.isCanceled()) return;
			if (monitor != null) monitor.subTask("deploying mobile solution; waiting for deployment");

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

			long startTimestamp = System.currentTimeMillis();
			try
			{
				while (!deployed && (System.currentTimeMillis() - startTimestamp) < maxWaitTime * 1000)
				{
					// wait for mobile war to be deployed
					Thread.sleep(200);
					if (monitor != null && monitor.isCanceled()) return;


					long mostRecentChangeTimestamp = warContextFolder.lastModified();
					deployed = (warContextFolder.exists() && mostRecentChangeTimestamp != initialModificationTimestampOfWarContextFolder && (System.currentTimeMillis() -
						mostRecentChangeTimestamp > DEPLOYMENT_FINISH_SILENCE_PERIOD));
//					if (warContextFolder.exists() && mostRecentChangeTimestamp != initialModificationTimestampOfWarContextFolder) System.out.println(mostRecentChangeTimestamp);
				}
				if (!deployed) throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "War deployment timeout after: " + maxWaitTime +
					" seconds."));
			}
			catch (InterruptedException e)
			{
				ServoyLog.logError(e);
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unexpected error: " + e.getMessage()));
		}
		finally
		{
			if (tmpExportFolder != null && tmpExportFolder.exists()) FileUtils.deleteQuietly(tmpExportFolder);
		}

		//open browser
		IWebBrowser webBrowser = getBrowser(browserID);
		if (webBrowser == null)
		{
			webBrowser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
		}
		openBrowser(webBrowser, getBrowserDescriptor(browserID), launch, configuration, monitor);
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
		warExportDir = new File(ApplicationServerSingleton.get().getServoyApplicationServerDirectory(), "server/webapps");
//		}
//		else
//		{
//			warExportDir = new File(warLocation);
//		}
		return warExportDir;
	}

	protected void openBrowser(IWebBrowser webBrowser, IBrowserDescriptor browserDescriptor, ILaunch launch, ILaunchConfiguration configuration,
		IProgressMonitor monitor) throws CoreException
	{
		if (monitor != null) monitor.subTask("opening mobile client in browser");
		EditorUtil.openURL(webBrowser, browserDescriptor, getApplicationURL(configuration));
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
		boolean validLicense = ApplicationServerSingleton.get().checkMobileLicense(company, license);

		exporter.setSolutionName(solutionName);
		exporter.setOutputFolder(exportFolder);
		exporter.setServerURL(serverUrl);
		exporter.setServiceSolutionName(serviceSolutionName);
		exporter.setTimeout(timeout);
		exporter.setSkipConnect(validLicense);
	}

	public static IBrowserDescriptor getBrowserDescriptor(String browserId)
	{
		Iterator iterator = BrowserManager.getInstance().getWebBrowsers().iterator();
		try
		{
			while (iterator.hasNext())
			{
				final IBrowserDescriptor ewb = (IBrowserDescriptor)iterator.next();
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