package com.servoy.eclipse.exporter.mobile.launch.test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.jsunit.launch.JSUnitLaunchConfigurationDelegate;
import com.servoy.eclipse.model.util.ServoyLog;

public class MobileTestLaunchConfigurationTab extends AbstractLaunchConfigurationTab
{

	protected Button closeBrowserWhenDone;
	protected Spinner fldClientConnectTimeout;
	protected Text fldUser;
	protected Text fldPassword;

	@Override
	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		container.setLayout(new FormLayout());

		Label lblClientConnectTimeout = new Label(container, SWT.NONE);
		lblClientConnectTimeout.setAlignment(SWT.TRAIL);
		lblClientConnectTimeout.setText("Client connect timeout");
		Label lblClientConnectTimeoutUnits = new Label(container, SWT.NONE);
		lblClientConnectTimeoutUnits.setAlignment(SWT.LEAD);
		lblClientConnectTimeoutUnits.setText("seconds");
		fldClientConnectTimeout = new Spinner(container, SWT.BORDER);
		fldClientConnectTimeout.setValues(Integer.parseInt(IMobileTestLaunchConstants.DEFAULT_CLIENT_CONNECT_TIMEOUT), 5, Integer.MAX_VALUE, 0, 1, 10);
		String toolTip = "Time to wait for the mobile test application (running in a browser) to connect to Servoy Developer.\nIf this time expires without the client connecting, the tests will fail.";
		lblClientConnectTimeout.setToolTipText(toolTip);
		lblClientConnectTimeoutUnits.setToolTipText(toolTip);
		fldClientConnectTimeout.setToolTipText(toolTip);

		Label lblUser = new Label(container, SWT.NONE);
		lblUser.setAlignment(SWT.TRAIL);
		lblUser.setText("Username");
		toolTip = "These credentials will be used to automatically authenticate with in mobile test client (stored unencrypted).";
		lblUser.setToolTipText(toolTip);
		fldUser = new Text(container, SWT.BORDER);
		fldUser.setText(IMobileTestLaunchConstants.DEFAULT_USERNAME);
		fldUser.setToolTipText(toolTip);

		Label lblPassword = new Label(container, SWT.NONE);
		lblPassword.setAlignment(SWT.TRAIL);
		lblPassword.setText("Password");
		lblPassword.setToolTipText(toolTip);
		fldPassword = new Text(container, SWT.BORDER | SWT.PASSWORD);
		fldPassword.setText(IMobileTestLaunchConstants.DEFAULT_PASSWORD);
		fldPassword.setToolTipText(toolTip);

		ModifyListener modifyListener = new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				updateLaunchConfigurationDialog();
			}
		};
		fldClientConnectTimeout.addModifyListener(modifyListener);
		fldUser.addModifyListener(modifyListener);

		closeBrowserWhenDone = new Button(container, SWT.CHECK);
		closeBrowserWhenDone.setText("Try to close browser window when done");
		closeBrowserWhenDone.setSelection(Boolean.parseBoolean(IMobileTestLaunchConstants.DEFAULT_CLOSE_BROWSER_WHEN_DONE));
		closeBrowserWhenDone.setToolTipText("After running the tests, Servoy developer will try to close the browser window that ran the mobile test client.\nBrowsers might or might not allow this.");

		SelectionAdapter selectionAdapter = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updateLaunchConfigurationDialog();
			}
		};
		closeBrowserWhenDone.addSelectionListener(selectionAdapter);

		int formMargins = 15;
		int columnSeparatorSpace = 15, rowSeparatorSpace = 8;
		Label dummy = new Label(container, SWT.NONE);

		FormData fd = new FormData();
		fd.top = new FormAttachment(fldUser, rowSeparatorSpace, SWT.CENTER);
		fd.left = new FormAttachment(0, formMargins);
		fd.right = new FormAttachment(fldUser, -columnSeparatorSpace);
		lblUser.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(0, formMargins);
		fd.left = new FormAttachment(lblClientConnectTimeout, columnSeparatorSpace);
		fd.right = new FormAttachment(100, -formMargins);
		fldUser.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(fldPassword, rowSeparatorSpace, SWT.CENTER);
		fd.left = new FormAttachment(0, formMargins);
		fd.right = new FormAttachment(fldPassword, -columnSeparatorSpace);
		lblPassword.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(fldUser, rowSeparatorSpace);
		fd.left = new FormAttachment(lblClientConnectTimeout, columnSeparatorSpace);
		fd.right = new FormAttachment(100, -formMargins);
		fldPassword.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(fldClientConnectTimeout, 0, SWT.CENTER);
		fd.left = new FormAttachment(0, formMargins);
		lblClientConnectTimeout.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(fldPassword, rowSeparatorSpace);
		fd.left = new FormAttachment(lblClientConnectTimeout, columnSeparatorSpace);
		fldClientConnectTimeout.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(fldClientConnectTimeout, 0, SWT.CENTER);
		fd.left = new FormAttachment(fldClientConnectTimeout, 5);
		fd.right = new FormAttachment(100, -formMargins);
		lblClientConnectTimeoutUnits.setLayoutData(fd);

		fd = new FormData(0, 0);
		fd.top = new FormAttachment(fldClientConnectTimeout, 0);
		fd.bottom = new FormAttachment(closeBrowserWhenDone, -rowSeparatorSpace);
		dummy.setLayoutData(fd);

		fd = new FormData();
		fd.bottom = new FormAttachment(100, -formMargins);
		fd.left = new FormAttachment(0, formMargins);
		closeBrowserWhenDone.setLayoutData(fd);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration)
	{
		JSUnitLaunchConfigurationDelegate.prepareLaunchConfigForTesting(configuration);
		// other defaults are interpreted correctly if nothing is set here...
	}


	@Override
	public void initializeFrom(ILaunchConfiguration configuration)
	{
		try
		{
			closeBrowserWhenDone.setSelection(Boolean.parseBoolean(configuration.getAttribute(IMobileTestLaunchConstants.CLOSE_BROWSER_WHEN_DONE,
				IMobileTestLaunchConstants.DEFAULT_CLOSE_BROWSER_WHEN_DONE)));
			fldClientConnectTimeout.setSelection(Integer.parseInt(configuration.getAttribute(IMobileTestLaunchConstants.CLIENT_CONNECT_TIMEOUT,
				IMobileTestLaunchConstants.DEFAULT_CLIENT_CONNECT_TIMEOUT)));
			fldUser.setText(configuration.getAttribute(IMobileTestLaunchConstants.USERNAME, IMobileTestLaunchConstants.DEFAULT_USERNAME));
			fldPassword.setText(configuration.getAttribute(IMobileTestLaunchConstants.PASSWORD, IMobileTestLaunchConstants.DEFAULT_PASSWORD));
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration)
	{
		try
		{
			setAttribute(configuration, IMobileTestLaunchConstants.CLOSE_BROWSER_WHEN_DONE, Boolean.valueOf(closeBrowserWhenDone.getSelection()).toString(),
				IMobileTestLaunchConstants.DEFAULT_CLOSE_BROWSER_WHEN_DONE);
			setAttribute(configuration, IMobileTestLaunchConstants.CLIENT_CONNECT_TIMEOUT, fldClientConnectTimeout.getText(),
				IMobileTestLaunchConstants.DEFAULT_CLIENT_CONNECT_TIMEOUT);

			// these 2 could be stored in Eclipse secure store, but we'd then need to detect launch config deletions - so that we won't let stale data in there...
			// for now just store as normal attributes (the tooltip mentions this)
			setAttribute(configuration, IMobileTestLaunchConstants.USERNAME, fldUser.getText(), IMobileTestLaunchConstants.DEFAULT_USERNAME);
			setAttribute(configuration, IMobileTestLaunchConstants.PASSWORD, fldPassword.getText(), IMobileTestLaunchConstants.DEFAULT_PASSWORD);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	private void setAttribute(ILaunchConfigurationWorkingCopy configuration, String attr, String value, String defaultvalue) throws CoreException
	{
		if (!defaultvalue.equals(value))
		{
			configuration.setAttribute(attr, value);
		}
		else if (configuration.hasAttribute(attr))
		{
			configuration.removeAttribute(attr);
		}
	}

	@Override
	public String getName()
	{
		return "Test Runner Configuration";
	}


	@Override
	public boolean isValid(ILaunchConfiguration launchConfig)
	{
		return true;
	}

}