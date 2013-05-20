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

package com.servoy.eclipse.mobileexporter.ui.wizard;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.mobileexporter.export.PhoneGapApplication;
import com.servoy.eclipse.mobileexporter.export.PhoneGapConnector;
import com.servoy.eclipse.mobileexporter.ui.wizard.ExportMobileWizard.CustomizedFinishPage;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;

/**
 * @author lvostinar
 *
 */
public class PhoneGapApplicationPage extends WizardPage
{
	private final CustomizedFinishPage finishPage;
	private final PhoneGapConnector connector;
	private Combo applicationNameCombo;
	private Text txtVersion;
	private Text txtDescription;
	private Button btnPublic;
	private Text iconPath;
	private Button iconBrowseButton;
	private CheckboxTableViewer certificatesViewer;

	private String solutionName;
	private String serverURL;
	private int timeout;

	public PhoneGapApplicationPage(String name, CustomizedFinishPage finishPage)
	{
		super(name);
		this.finishPage = finishPage;
		this.connector = new PhoneGapConnector();
		setTitle("PhoneGap Application");
	}

	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);

		Label lblApplicationName = new Label(container, SWT.NONE);
		lblApplicationName.setText("Application Title");

		applicationNameCombo = new Combo(container, SWT.BORDER);

		Label lblVersion = new Label(container, SWT.NONE);
		lblVersion.setText("Version");

		txtVersion = new Text(container, SWT.BORDER);

		Label lblDescription = new Label(container, SWT.NONE);
		lblDescription.setText("Description");

		txtDescription = new Text(container, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);

		Label iconLabel = new Label(container, SWT.NONE);
		iconLabel.setText("Icon");

		iconPath = new Text(container, SWT.BORDER);
		iconBrowseButton = new Button(container, SWT.NONE);
		iconBrowseButton.setText("Browse");

		final Shell outputBrowseShell = new Shell();
		iconBrowseButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				FileDialog fileDialog = new FileDialog(outputBrowseShell, SWT.NONE);
				fileDialog.setFilterExtensions(new String[] { "*.png" });
				if (fileDialog.open() != null)
				{
					iconPath.setText(fileDialog.getFilterPath() + File.separator + fileDialog.getFileName());
				}
			}
		});

		Label certificatesLabel = new Label(container, SWT.NONE);
		certificatesLabel.setText("Certificates");

		certificatesViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		certificatesViewer.setLabelProvider(new LabelProvider());
		certificatesViewer.setContentProvider(new ArrayContentProvider());

		btnPublic = new Button(container, SWT.CHECK);
		btnPublic.setText("Public Application");

		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING, false).add(lblApplicationName).add(lblVersion).add(lblDescription).add(iconLabel).add(
					certificatesLabel)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(applicationNameCombo, GroupLayout.DEFAULT_SIZE, 342, Short.MAX_VALUE).add(txtVersion,
					GroupLayout.PREFERRED_SIZE, 276, Short.MAX_VALUE).add(txtDescription, GroupLayout.PREFERRED_SIZE, 276, Short.MAX_VALUE).add(
					groupLayout.createSequentialGroup().add(iconPath, GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE).add(iconBrowseButton,
						GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)).add(certificatesViewer.getTable(), GroupLayout.DEFAULT_SIZE, 342,
					Short.MAX_VALUE).add(btnPublic, GroupLayout.PREFERRED_SIZE, 276, Short.MAX_VALUE)).addContainerGap()));

		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(applicationNameCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(lblApplicationName)).add(7).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(txtVersion, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(lblVersion)).add(7).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(txtDescription, 80, 80, 80).add(lblDescription)).add(7).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(iconBrowseButton).add(iconPath, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(iconLabel)).add(7).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(certificatesViewer.getTable(), 80, 80, 80).add(certificatesLabel)).add(10).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(btnPublic))));

		container.setLayout(groupLayout);

		ModifyListener errorMessageDetecter = new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				PhoneGapApplicationPage.this.setErrorMessage(null);
				PhoneGapApplicationPage.this.getContainer().updateMessage();
				PhoneGapApplicationPage.this.getContainer().updateButtons();
			}
		};
		applicationNameCombo.addModifyListener(errorMessageDetecter);
		applicationNameCombo.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				PhoneGapApplication app = connector.getApplication(applicationNameCombo.getText());
				if (app != null)
				{
					txtDescription.setText(app.getDescription());
					txtVersion.setText(app.getVersion());
					btnPublic.setSelection(app.isPublicApplication());
					iconPath.setText(app.getIconPath());
					setSelectedCertificates(app.getCertificates());
				}
			}
		});
	}

	@Override
	public boolean canFlipToNextPage()
	{
		return getErrorMessage() == null;
	}

	@Override
	public IWizardPage getNextPage()
	{
		super.setErrorMessage(null);
		if (canFlipToNextPage())
		{
			EditorUtil.saveDirtyEditors(getShell(), true);
			final String[] errorMessage = new String[1];
			final String appName = applicationNameCombo.getText();
			final String appVersion = txtVersion.getText();
			final String appDescription = txtDescription.getText();
			final boolean appPublic = btnPublic.getSelection();
			final String path = iconPath.getText();
			final String[] selectedCertificates = getSelectedCerticates();
			try
			{
				PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress()
				{
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
					{
						errorMessage[0] = getConnector().createOrUpdatePhoneGapApplication(
							new PhoneGapApplication(appName, appVersion, appDescription, appPublic, path, selectedCertificates), solutionName, serverURL,
							timeout);

					}
				});
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
				errorMessage[0] = ex.getMessage();
			}
			if (errorMessage[0] != null)
			{
				setErrorMessage(errorMessage[0]);
				return null;
			}
			finishPage.setApplicationURL("https://build.phonegap.com/people/sign_in", "Open PhoneGap build page at finish.", true);
			finishPage.createControl(PhoneGapApplicationPage.this.getControl().getParent());
			finishPage.setTextMessage("Solution exported to PhoneGap application.");
			finishPage.getControl().getParent().layout(true);
			return finishPage;
		}
		return null;
	}

	@Override
	public String getErrorMessage()
	{
		if (applicationNameCombo.getText() == null || "".equals(applicationNameCombo.getText()))
		{
			return "No PhoneGap application name specified.";
		}
		return super.getErrorMessage();
	}

	public PhoneGapConnector getConnector()
	{
		return connector;
	}

	/**
	 * @param solutionName the solutionName to set
	 */
	public void setSolutionName(String solutionName)
	{
		this.solutionName = solutionName;
	}

	/**
	 * @param serverURL the serverURL to set
	 */
	public void setServerURL(String serverURL)
	{
		this.serverURL = serverURL;
	}

	/**
	 * @param timeout the timeout to set
	 */
	public void setTimeout(int timeout)
	{
		this.timeout = timeout;
	}

	public void populateExistingApplications()
	{
		applicationNameCombo.setItems(connector.getExistingApps());
		certificatesViewer.setInput(getConnector().getCertificates());
	}

	public String[] getSelectedCerticates()
	{
		return Arrays.asList(certificatesViewer.getCheckedElements()).toArray(new String[0]);
	}

	private void setSelectedCertificates(String[] certificates)
	{
		certificatesViewer.setCheckedElements((certificates != null) ? certificates : new String[] { });
	}
}
