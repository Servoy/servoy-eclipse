package com.servoy.eclipse.exporter.mobile.launch.test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.exporter.mobile.export.MobileExporter;
import com.servoy.eclipse.exporter.mobile.launch.IMobileLaunchConstants;
import com.servoy.eclipse.exporter.mobile.launch.MobileLaunchConfigurationDelegate;
import com.servoy.eclipse.jsunit.launch.JSUnitLaunchConfigurationDelegate;
import com.servoy.eclipse.jsunit.mobile.RunMobileClientTests;
import com.servoy.eclipse.jsunit.runner.TestTarget;
import com.servoy.eclipse.jsunit.scriptunit.RunJSUnitTests;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;

public class MobileTestLaunchConfigurationDelegate extends MobileLaunchConfigurationDelegate
{

	private IWebBrowser webBrowser;
	protected int bridgeID;
	private TestTarget testTarget;

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException
	{
		boolean nodebug = Boolean.valueOf(configuration.getAttribute(IMobileLaunchConstants.NODEBUG, "true")).booleanValue();
		if (webBrowser != null)
		{
			try
			{
				webBrowser.close(); // just to be sure no old browser is still open
				webBrowser = null;
			}
			catch (Exception e)
			{
				ServoyLog.logError(e); // log it but just continue anyway - maybe another browser window will start correctly
			}
		}
		super.launch(configuration, mode, launch, monitor);

		if (!nodebug)
		{
			// re-activate the mobile solution after tests finish their execution if needed
			ServoyModel model = ServoyModelManager.getServoyModelManager().getServoyModel();
			String solutionName = configuration.getAttribute(IMobileLaunchConstants.SOLUTION_NAME, "");
			ServoyProject mobileProject = model.getServoyProject(solutionName);
			if (mobileProject != null && mobileProject != model.getActiveProject())
			{
				model.setActiveProject(mobileProject, false);
				RunJSUnitTests.showTestRunnerViewPartInActivePage(0); // TODO maybe also restore editors that were open?
			}
		}
	}

	@Override
	protected void prepareExporter(MobileExporter exporter, ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) throws CoreException
	{
		super.prepareExporter(exporter, configuration, launch, monitor);

		exporter.useTestWar(testTarget);

		if (monitor != null && monitor.isCanceled()) return;

		// prepare javascript suite and link launcher to dltk unit test view
		testTarget = JSUnitLaunchConfigurationDelegate.prepareForLaunch(configuration, launch);
	}

	@Override
	protected void openBrowser(final IWebBrowser webBrowser, final IBrowserDescriptor browserDescriptor, final ILaunch launch,
		final ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException
	{
		if (monitor != null) monitor.subTask("starting test session");
		this.webBrowser = webBrowser;

		int clientConnectTimeout;
		String userName, password;
		userName = configuration.getAttribute(IMobileTestLaunchConstants.USERNAME, IMobileTestLaunchConstants.DEFAULT_USERNAME);
		password = configuration.getAttribute(IMobileTestLaunchConstants.PASSWORD, IMobileTestLaunchConstants.DEFAULT_PASSWORD);
		try
		{
			clientConnectTimeout = Integer.parseInt(configuration.getAttribute(IMobileTestLaunchConstants.CLIENT_CONNECT_TIMEOUT,
				IMobileTestLaunchConstants.DEFAULT_CLIENT_CONNECT_TIMEOUT));
		}
		catch (NumberFormatException ex)
		{
			clientConnectTimeout = Integer.parseInt(IMobileTestLaunchConstants.DEFAULT_CLIENT_CONNECT_TIMEOUT);
		}

		if (monitor != null && monitor.isCanceled()) return;

		RunMobileClientTests runner = new RunMobileClientTests(testTarget, launch, clientConnectTimeout, monitor)
		{
			@Override
			protected void prepareForTesting()
			{
				super.prepareForTesting();
				MobileTestLaunchConfigurationDelegate.this.bridgeID = getBridgeId();
				try
				{
					MobileTestLaunchConfigurationDelegate.super.openBrowser(webBrowser, browserDescriptor, launch, configuration, getLaunchMonitor()); // non-blocking
					if (getLaunchMonitor() != null) getLaunchMonitor().subTask("connecting to mobile test client"); //$NON-NLS-1$
				}
				catch (CoreException e)
				{
					throw new RuntimeException(e);
				}
			}
		};
		runner.setCredentials(userName, password);

		try
		{
			runner.run();
		}
		catch (RuntimeException e)
		{
			if (e.getCause() instanceof CoreException) throw (CoreException)e.getCause();
			throw e;
		}
		finally
		{
			if (Boolean.parseBoolean(configuration.getAttribute(IMobileTestLaunchConstants.CLOSE_BROWSER_WHEN_DONE,
				IMobileTestLaunchConstants.DEFAULT_CLOSE_BROWSER_WHEN_DONE)))
			{
				if (monitor != null) monitor.subTask("test session finished - closing browser"); //$NON-NLS-1$
				webBrowser.close(); // when tests are done, close the browser
			}
			this.webBrowser = null;
		}
	}

	@Override
	protected String getApplicationURL(ILaunchConfiguration configuration) throws CoreException
	{
		String appURL = configuration.getAttribute(IMobileLaunchConstants.APPLICATION_URL, "");
		if (appURL == null) return null;
		boolean nodebug = Boolean.valueOf(configuration.getAttribute(IMobileLaunchConstants.NODEBUG, "true")).booleanValue();

		boolean hasArgs = (appURL.indexOf("?") != -1);
		appURL = appURL + (hasArgs ? "&" : "?") + "log_level=DEBUG&noinitsmc=true&bid=" + bridgeID;
		if (nodebug && !appURL.contains("nodebug=true"))
		{
			appURL = appURL + "&nodebug=true";
		}

		return appURL;
	}
}
