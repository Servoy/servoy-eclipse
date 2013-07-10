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

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.model.mobile.exporter.MobileExporter;

/**
 * @author lvostinar
 *
 */
public class ExportOptionsPage extends WizardPage
{
	public static String SERVER_URL_KEY = "serverURL";
	public static String TIMEOUT_KEY = "timeout";

	private Text serverURL;
	private Text timeout;
	private String solution;
	private final WizardPage nextPage;
	private final MobileExporter mobileExporter;

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
		solutionName.setText(solution);

		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING, false).add(solutionLabel).add(serverURLLabel).add(timeoutLabel)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(solutionName).add(serverURL, GroupLayout.PREFERRED_SIZE, 400, Short.MAX_VALUE).add(
					timeout, GroupLayout.PREFERRED_SIZE, 400, Short.MAX_VALUE)).addContainerGap()));

		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(solutionName).add(solutionLabel)).add(7).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(serverURL, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(serverURLLabel)).add(7).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(timeout, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(timeoutLabel)).add(10)));

		container.setLayout(groupLayout);

		String defaultServerURL = getDialogSettings().get(SERVER_URL_KEY);
		if (defaultServerURL == null)
		{
			defaultServerURL = MobileExporter.DEFAULT_SERVER_URL;
		}
		serverURL.setText(defaultServerURL);

		serverURL.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				setErrorMessage(null);
				ExportOptionsPage.this.getContainer().updateMessage();
				ExportOptionsPage.this.getContainer().updateButtons();
			}
		});

		String defaultTimeout = getDialogSettings().get(TIMEOUT_KEY);
		if (defaultTimeout == null)
		{
			defaultTimeout = "30";
		}

		timeout.setText(defaultTimeout);

		timeout.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				setErrorMessage(null);
				ExportOptionsPage.this.getContainer().updateMessage();
				ExportOptionsPage.this.getContainer().updateButtons();
			}
		});
	}

	private String getServerURL()
	{
		return serverURL.getText();
	}

	private String getTimeout()
	{
		return timeout.getText();
	}

	public void setSolution(String solution)
	{
		this.solution = solution;
	}

	public String getSolution()
	{
		return solution;
	}

	@Override
	public String getErrorMessage()
	{
		if (getSolution() == null || "".equals(getSolution()))
		{
			return "No solution specified";
		}
		if (getServerURL() == null || "".equals(getServerURL()))
		{
			return "No server URL specified";
		}
		if (getTimeout() == null || "".equals(getTimeout()) || (getTimeout() != null && !getTimeout().matches("\\d+")))
		{
			return "No valid timeout specified";
		}
		return super.getErrorMessage();
	}

	@Override
	public IWizardPage getNextPage()
	{
		mobileExporter.setSolutionName(getSolution());
		mobileExporter.setServerURL(getServerURL());
		mobileExporter.setTimeout(Integer.parseInt(getTimeout()));
		return nextPage;
	}

	@Override
	public boolean canFlipToNextPage()
	{
		return getErrorMessage() == null;
	}

}
