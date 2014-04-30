/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.eclipse.exporter.mobile.ui.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.exporter.mobile.ui.wizard.ExportMobileWizard.IMobileExportPropertiesPage;
import com.servoy.eclipse.model.mobile.exporter.MobileExporter;

/**
 * @author lvostinar
 *
 */
public class ExportOptionsPage extends WizardPage implements IMobileExportPropertiesPage
{
	public static String SERVER_URL_KEY = "serverURL";
	public static String SERVICE_SOLUTION_KEY_PREFIX = "serviceSolution_";
	public static String TIMEOUT_KEY = "timeout";
	public static String DEBUG_MODE_KEY = "debug_mode";

	private Text serverURL;
	private Text serviceSolutionName;
	private Text timeout;
	private final WizardPage nextPage;
	private final MobileExporter mobileExporter;
	private Button debugCheck;

	public ExportOptionsPage(String pageName, WizardPage nextPage, MobileExporter mobileExporter)
	{
		super(pageName);
		this.nextPage = nextPage;
		this.mobileExporter = mobileExporter;
		setTitle("Export Options");
	}

	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);

		Label serverURLLabel = new Label(container, SWT.NONE);
		serverURLLabel.setText("Application Server URL");

		serverURL = new Text(container, SWT.BORDER);
		serverURL.setToolTipText("This is the URL of Servoy Application Server used by mobile client to synchronize data");

		Label timeoutLabel = new Label(container, SWT.NONE);
		timeoutLabel.setText("Sync Timeout (in seconds)");

		timeout = new Text(container, SWT.BORDER);
		timeout.setToolTipText("This is the sync timeout, provided in seconds, used by the mobile client when waiting for the request to complete");

		Label solutionLabel = new Label(container, SWT.NONE);
		solutionLabel.setText("Solution");

		Label solutionName = new Label(container, SWT.NONE);
		solutionName.setText(mobileExporter.getSolutionName());

		Label serviceSolutionLabel = new Label(container, SWT.NONE);
		serviceSolutionLabel.setText("Service Solution Name");

		serviceSolutionName = new Text(container, SWT.BORDER);
		serviceSolutionName.setToolTipText("This is the name of the service solution mobile clients connects to (must be available at server URL).");

		Label debugLabel = new Label(container, SWT.NONE);
		debugLabel.setText("Export in debug mode"); //, .

		final Label label = new Label(container, SWT.NONE);
		label.setText("Starts a connection between the mobile device and the developer.\nSo the developer must run and the mobile solution must be active.\nThen connect chrome to: localhost:8889 on the developer machine after the mobile solution is started");
		label.setVisible(false);

		debugCheck = new Button(container, SWT.CHECK);
		debugCheck.setSelection(getDialogSettings().getBoolean(DEBUG_MODE_KEY));
		mobileExporter.setDebugMode(debugCheck.getSelection());
		debugCheck.addSelectionListener(new SelectionListener()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				label.setVisible(debugCheck.getSelection());
				mobileExporter.setDebugMode(debugCheck.getSelection());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
				label.setVisible(debugCheck.getSelection());
				mobileExporter.setDebugMode(debugCheck.getSelection());
			}
		});

		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING, false).add(solutionLabel).add(serverURLLabel).add(serviceSolutionLabel).add(timeoutLabel).add(
					debugLabel)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(solutionName).add(serverURL, GroupLayout.PREFERRED_SIZE, 400, Short.MAX_VALUE).add(
					serviceSolutionName, GroupLayout.PREFERRED_SIZE, 400, Short.MAX_VALUE).add(timeout, GroupLayout.PREFERRED_SIZE, 400, Short.MAX_VALUE).add(
					debugCheck, GroupLayout.PREFERRED_SIZE, 400, Short.MAX_VALUE)).addContainerGap()).add(
			groupLayout.createSequentialGroup().addContainerGap().add(label)));

		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(solutionName).add(solutionLabel)).add(7).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(serverURL, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(serverURLLabel)).add(7).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(serviceSolutionName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(serviceSolutionLabel)).add(7).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(timeout, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(timeoutLabel)).add(7).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(debugCheck, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(debugLabel)).add(10).add(label)));

		container.setLayout(groupLayout);

		String defaultServerURL = getDialogSettings().get(SERVER_URL_KEY);
		if (defaultServerURL == null)
		{
			defaultServerURL = MobileExporter.getDefaultServerURL();
		}
		serverURL.setText(defaultServerURL);
		mobileExporter.setServerURL(getServerURL());
		serverURL.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				mobileExporter.setServerURL(getServerURL());
			}
		});

		String defaultServiceSolutionName = getDialogSettings().get(SERVICE_SOLUTION_KEY_PREFIX + mobileExporter.getSolutionName());
		if (defaultServiceSolutionName == null)
		{
			defaultServiceSolutionName = mobileExporter.getSolutionName() + "_service";
		}
		serviceSolutionName.setText(defaultServiceSolutionName);
		mobileExporter.setServiceSolutionName(getServiceSolutionName());
		serviceSolutionName.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				setErrorMessage(null);
				ExportOptionsPage.this.getContainer().updateMessage();
				ExportOptionsPage.this.getContainer().updateButtons();
				mobileExporter.setServiceSolutionName(getServiceSolutionName());
			}
		});

		String defaultTimeout = getDialogSettings().get(TIMEOUT_KEY);
		if (defaultTimeout == null)
		{
			defaultTimeout = "30";
		}

		timeout.setText(defaultTimeout);
		mobileExporter.setTimeout(Integer.parseInt(getTimeout()));
		timeout.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				setErrorMessage(null);
				ExportOptionsPage.this.getContainer().updateMessage();
				ExportOptionsPage.this.getContainer().updateButtons();
				mobileExporter.setTimeout(Integer.parseInt(getTimeout()));
			}
		});
	}

	private String getServerURL()
	{
		String url = serverURL.getText();
		return "".equals(url) ? null : url;
	}

	private String getServiceSolutionName()
	{
		return serviceSolutionName.getText();
	}

	public boolean useDebugMode()
	{
		return debugCheck.getSelection();
	}

	private String getTimeout()
	{
		return timeout.getText();
	}

	@Override
	public String getErrorMessage()
	{
		if (getServiceSolutionName() == null || "".equals(getServiceSolutionName()))
		{
			return "No service solution specified";
		}
		if (getTimeout() == null || "".equals(getTimeout()) || (getTimeout() != null && !getTimeout().matches("\\d+")))
		{
			return "No valid timeout specified";
		}
		return super.getErrorMessage();
	}

	@Override
	public boolean canFlipToNextPage()
	{
		return getErrorMessage() == null;
	}

	@Override
	public boolean saveProperties()
	{
		getDialogSettings().put(ExportOptionsPage.SERVER_URL_KEY, serverURL.getText());
		getDialogSettings().put(ExportOptionsPage.SERVICE_SOLUTION_KEY_PREFIX + mobileExporter.getSolutionName(), getServiceSolutionName());
		getDialogSettings().put(ExportOptionsPage.TIMEOUT_KEY, getTimeout());
		getDialogSettings().put(ExportOptionsPage.DEBUG_MODE_KEY, useDebugMode());
		return true;
	}

}
