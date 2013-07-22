package com.servoy.eclipse.exporter.mobile.launch;

import java.io.File;
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
import com.servoy.eclipse.exporter.mobile.Activator;
import com.servoy.eclipse.model.mobile.exporter.MobileExporter;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;


public class MobileLaunchConfigurationDelegate extends LaunchConfigurationDelegate
{

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException
	{
		String browserID = configuration.getAttribute(IMobileLaunchConstants.BROWSER_ID, "");
		boolean nodebug = Boolean.valueOf(configuration.getAttribute(IMobileLaunchConstants.NODEBUG, "true")).booleanValue();

		MobileExporter exporter = new MobileExporter();
		prepareExporter(exporter, configuration, launch, monitor);
		if (monitor != null && monitor.isCanceled()) return;

		if (monitor != null) monitor.subTask("exporting mobile solution");
		exporter.doExport(false);
		if (monitor != null && monitor.isCanceled()) return;
		if (monitor != null) monitor.subTask("deploying mobile solution");

		int waitTime;
		try
		{
			waitTime = Integer.parseInt(configuration.getAttribute(IMobileLaunchConstants.WAR_DEPLOYMENT_TIME,
				IMobileLaunchConstants.DEFAULT_WAR_DEPLOYMENT_TIME));
		}
		catch (NumberFormatException ex)
		{
			waitTime = Integer.parseInt(IMobileLaunchConstants.DEFAULT_WAR_DEPLOYMENT_TIME);
		}

		try
		{
			for (int i = 0; i < waitTime; i++)
			{
				// wait for mobile war to be deployed , TODO listen for deployment event from tomcat if embedded Tomcat install is targeted
				Thread.sleep(1000);
				if (monitor != null && monitor.isCanceled()) return;
			}
		}
		catch (InterruptedException e)
		{
			ServoyLog.logError(e);
		}

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
		//open browser
		IWebBrowser webBrowser = getBrowser(browserID);
		if (webBrowser == null)
		{
			webBrowser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
		}
		openBrowser(webBrowser, getBrowserDescriptor(browserID), launch, configuration, monitor);
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

	protected void prepareExporter(MobileExporter exporter, ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) throws CoreException
	{
		String warLocation = configuration.getAttribute(IMobileLaunchConstants.WAR_LOCATION, "");
		if (warLocation.length() == 0) throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Invalid war export path"));
		String solutionName = configuration.getAttribute(IMobileLaunchConstants.SOLUTION_NAME, "");
		String serverUrl = configuration.getAttribute(IMobileLaunchConstants.SERVER_URL, "");
		String serviceSolutionName = configuration.getAttribute(IMobileLaunchConstants.SERVICE_SOLUTION, (String)null);
		int timeout = Integer.valueOf(configuration.getAttribute(IMobileLaunchConstants.TIMEOUT, IMobileLaunchConstants.DEFAULT_TIMEOUT)).intValue();
		String company = configuration.getAttribute("company", "");
		String license = configuration.getAttribute("license", "");
		boolean validLicense = ApplicationServerSingleton.get().checkMobileLicense(company, license);

		exporter.setSolutionName(solutionName);
		exporter.setOutputFolder(new File(warLocation));
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