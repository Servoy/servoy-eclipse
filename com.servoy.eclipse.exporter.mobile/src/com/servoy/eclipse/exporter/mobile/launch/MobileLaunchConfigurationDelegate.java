package com.servoy.eclipse.exporter.mobile.launch;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

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
import com.servoy.eclipse.mobileexporter.Activator;
import com.servoy.eclipse.mobileexporter.export.MobileExporter;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;


public class MobileLaunchConfigurationDelegate extends LaunchConfigurationDelegate
{

	public static final String LAUNCH_CONFIG_INSTANCE = "com.servoy.eclipse.mobile.launch.configurationInstance";

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException
	{

		String warLocation = configuration.getAttribute(IMobileLaunchConstants.WAR_LOCATION, "");
		if (warLocation.length() == 0) throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Invalid war export path"));
		String solutionName = configuration.getAttribute(IMobileLaunchConstants.SOLUTION_NAME, "");
		String serverUrl = configuration.getAttribute(IMobileLaunchConstants.SERVER_URL, "");
		String appUrl = configuration.getAttribute(IMobileLaunchConstants.APPLICATION_URL, "");
		String company = configuration.getAttribute("company", "");
		String license = configuration.getAttribute("license", "");
		String browserID = configuration.getAttribute(IMobileLaunchConstants.BROWSER_ID, "");
		boolean nodebug = Boolean.valueOf(configuration.getAttribute(IMobileLaunchConstants.NODEBUG, "true"));
		boolean validLicense = ApplicationServerSingleton.get().checkMobileLicense(company, license);

		MobileExporter exporter = new MobileExporter();
		exporter.setSolutionName(solutionName);
		exporter.setOutputFolder(new File(warLocation));
		exporter.setServerURL(serverUrl);
		exporter.setSkipConnect(validLicense);

		monitor.subTask("exporting mobile solution");
		exporter.doExport(false);
		monitor.subTask("deploying mobile solution");
		try
		{
			//wait for mobile war to be deployed , TODO listen for deployment event from tomcat
			Thread.sleep(5000);
		}
		catch (InterruptedException e)
		{
			ServoyLog.logError(e);
		}

		try
		{
			if (!nodebug)
			{
				ServoyProject serviceProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName + "_service");
				ServoyModelManager.getServoyModelManager().getServoyModel().setActiveProject(serviceProject, true);
			}
			//open browser
			IWebBrowser webBrowser = getBrowserToLaunch(browserID);
			webBrowser.openURL(new URL(appUrl));
		}
		catch (MalformedURLException e)
		{
			ServoyLog.logError(e);
		}
	}

	private IWebBrowser getBrowserToLaunch(String browserId)
	{
		IWebBrowser webBrowser = null;
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
						webBrowser = ext.createBrowser(ext.getId(), ewb.getLocation(), ewb.getParameters());
						if (webBrowser == null) webBrowser = new ExternalBrowserInstance(ext.getId(), ewb);
						break;
					}
					else
					{
						if (ewb.getLocation() != null) webBrowser = new ExternalBrowserInstance("org.eclipse.ui.browser." +
							ewb.getName().toLowerCase().replace(" ", "_"), ewb);
					}
				}
			}
			if (webBrowser == null)
			{
				webBrowser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		return webBrowser;
	}
}