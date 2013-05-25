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

import com.servoy.eclipse.jsunit.launch.JSUnitLaunchConfigurationDelegate;
import com.servoy.eclipse.model.util.ServoyLog;

public class MobileTestLaunchConfigurationTab extends AbstractLaunchConfigurationTab
{

	protected Button closeBrowserWhenDone;
	protected Spinner fldClientConnectTimeout;

	@Override
	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		container.setLayout(new FormLayout());

		Label lblClientConnectTimeout = new Label(container, SWT.NONE);
		lblClientConnectTimeout.setText("Client connect timeout (sec)");
		String toolTip = "Time to wait for the mobile test application (browser) to connect to the developer. If this time expires without the client connecting, the tests will fail.";
		lblClientConnectTimeout.setToolTipText(toolTip);
		fldClientConnectTimeout = new Spinner(container, SWT.BORDER);
		fldClientConnectTimeout.setValues(Integer.parseInt(IMobileTestLaunchConstants.DEFAULT_CLIENT_CONNECT_TIMEOUT), 5, Integer.MAX_VALUE, 0, 1, 10);
		fldClientConnectTimeout.setToolTipText(toolTip);
		ModifyListener modifyListener = new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				updateLaunchConfigurationDialog();
			}
		};
		fldClientConnectTimeout.addModifyListener(modifyListener);

		closeBrowserWhenDone = new Button(container, SWT.CHECK);
		closeBrowserWhenDone.setText("Try to close browser window when done");
		closeBrowserWhenDone.setSelection(Boolean.parseBoolean(IMobileTestLaunchConstants.DEFAULT_CLOSE_BROWSER_WHEN_DONE));

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
		int columnSeparatorSpace = 10, rowSeparatorSpace = 10;
		Label dummy = new Label(container, SWT.NONE);

		FormData fd = new FormData();
		fd.top = new FormAttachment(fldClientConnectTimeout, 0, SWT.CENTER);
		fd.left = new FormAttachment(0, formMargins);
		lblClientConnectTimeout.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(0, formMargins);
		fd.left = new FormAttachment(lblClientConnectTimeout, columnSeparatorSpace);
		fd.right = new FormAttachment(100, -formMargins);
		fldClientConnectTimeout.setLayoutData(fd);

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