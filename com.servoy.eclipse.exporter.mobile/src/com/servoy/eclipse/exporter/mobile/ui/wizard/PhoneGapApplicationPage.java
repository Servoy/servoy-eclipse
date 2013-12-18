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

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
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
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.servoy.eclipse.model.mobile.exporter.MobileExporter;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class PhoneGapApplicationPage extends WizardPage
{
	private WarExportPage warExportPage;
	private final PhoneGapConnector connector;
	private Combo applicationNameCombo;
	private Text txtVersion;
	private Text txtDescription;
	private Button btnPublic;
	private Text iconPath;
	private Text configPath;
	private Button configBrowseButton;
	private Button iconBrowseButton;
	private CheckboxTableViewer certificatesViewer;

	private final MobileExporter exporter;
	private Button btnUseConfigXml;
	private Button btnUsePhonegap;
	private Button btnOpenPhonegapLink;

	public static final String HAS_CONFIG_FILE_KEY = "hasConfigFile_";
	public static final String CONFIG_FILE_PATH_KEY = "configFilePath_";
	public static final String APPLICATION_NAME_KEY = "applicationName_";
	public static final String APPLICATION_DESCRIPTION_KEY = "applicationDescription_";
	public static final String APPLICATION_VERSION_KEY = "applicationVersion_";
	public static final String IS_APPLICATION_PUBLIC_KEY = "isApplicationPublic_";
	public static final String APPLICATION_ICON_KEY = "applicationIcon_";
	public static final String CERTIFICATES_KEY = "certificates_";
	public static final String OPEN_PHONEGAP_BUILD_PAGE_KEY = "openPhoneGapBuild_";

	public PhoneGapApplicationPage(String name, MobileExporter mobileExporter)
	{
		super(name);
		this.connector = new PhoneGapConnector();
		this.exporter = mobileExporter;
		setTitle("PhoneGap Application");
	}

	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);

		Label lblConfigurationType = new Label(container, SWT.NONE);
		lblConfigurationType.setText("Configuration type");

		btnUseConfigXml = new Button(container, SWT.RADIO);
		SelectionAdapter configSelection = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				selectConfigType();
			}
		};
		btnUseConfigXml.addSelectionListener(configSelection);
		btnUseConfigXml.setText("Use config.xml");

		btnUsePhonegap = new Button(container, SWT.RADIO);
		btnUsePhonegap.addSelectionListener(configSelection);
		btnUsePhonegap.setText("Load existing settings from PhoneGap");

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

		Link configLabel = new Link(container, SWT.NONE);
		configLabel.setText("PhoneGap <A>Config File</A>");
		configLabel.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				try
				{
					IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
					IWebBrowser browser = support.getExternalBrowser();
					browser.openURL(new URL("https://build.phonegap.com/docs/config-xml"));
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
		});

		configPath = new Text(container, SWT.BORDER);
		configPath.setToolTipText("PhoneGap config file location. Settings from config file (if present) have higher priority than settings from this form.");
		configBrowseButton = new Button(container, SWT.NONE);
		configBrowseButton.setText("Browse");

		configBrowseButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				selectConfigFile(outputBrowseShell);
				getWizard().getContainer().updateButtons();
			}
		});

		btnPublic = new Button(container, SWT.CHECK);
		btnPublic.setText("Public Application");

		btnOpenPhonegapLink = new Button(container, SWT.CHECK);
		btnOpenPhonegapLink.setText("Open PhoneGap build page at finish.");

		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING, false).add(
					groupLayout.createSequentialGroup().add(161).add(configLabel).add(6).add(configPath, GroupLayout.PREFERRED_SIZE, 351,
						GroupLayout.PREFERRED_SIZE).add(configBrowseButton, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)).add(
					groupLayout.createSequentialGroup().add(161).add(lblApplicationName).add(34).add(applicationNameCombo, GroupLayout.PREFERRED_SIZE, 431,
						GroupLayout.PREFERRED_SIZE)).add(
					groupLayout.createSequentialGroup().add(161).add(lblVersion).add(82).add(txtVersion, GroupLayout.PREFERRED_SIZE, 431,
						GroupLayout.PREFERRED_SIZE)).add(
					groupLayout.createSequentialGroup().add(161).add(lblDescription).add(61).add(txtDescription, GroupLayout.PREFERRED_SIZE, 431,
						GroupLayout.PREFERRED_SIZE)).add(
					groupLayout.createSequentialGroup().add(23).add(
						groupLayout.createParallelGroup(GroupLayout.LEADING, false).add(
							groupLayout.createSequentialGroup().add(138).add(iconLabel).add(98).add(iconPath, GroupLayout.PREFERRED_SIZE, 351,
								GroupLayout.PREFERRED_SIZE).add(iconBrowseButton, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)).add(
							certificatesLabel, GroupLayout.PREFERRED_SIZE, 103, GroupLayout.PREFERRED_SIZE).add(
							groupLayout.createSequentialGroup().add(lblConfigurationType).add(11).add(
								groupLayout.createParallelGroup(GroupLayout.LEADING).add(btnUsePhonegap, GroupLayout.PREFERRED_SIZE, 256,
									GroupLayout.PREFERRED_SIZE).add(btnUseConfigXml, GroupLayout.PREFERRED_SIZE, 217, GroupLayout.PREFERRED_SIZE).add(
									btnPublic, GroupLayout.PREFERRED_SIZE, 431, GroupLayout.PREFERRED_SIZE).add(certificatesViewer.getTable(),
									GroupLayout.DEFAULT_SIZE, 576, Short.MAX_VALUE))).add(btnOpenPhonegapLink)))).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().add(1).add(lblConfigurationType)).add(
					btnUseConfigXml)).add(8).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(configLabel).add(
					groupLayout.createSequentialGroup().add(2).add(configPath, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).add(
					configBrowseButton)).add(13).add(btnUsePhonegap).addPreferredGap(LayoutStyle.UNRELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().add(3).add(lblApplicationName)).add(
					applicationNameCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).add(15).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().add(3).add(lblVersion)).add(txtVersion,
					GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).add(8).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().add(3).add(lblDescription)).add(txtDescription,
					GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)).add(8).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().add(1).add(iconLabel)).add(
					groupLayout.createSequentialGroup().add(1).add(iconPath, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).add(
					iconBrowseButton)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(certificatesLabel, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE).add(
					certificatesViewer.getTable(), 80, 80, 80)).addPreferredGap(LayoutStyle.RELATED).add(btnPublic).add(25).add(btnOpenPhonegapLink).addContainerGap()));
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
				getWizard().getContainer().updateButtons();
			}
		});
		configPath.addModifyListener(errorMessageDetecter);
		setDefaultPageData();
	}


	private void setDefaultPageData()
	{
		String solutionName = exporter.getSolutionName();
		String hasConfig = getDialogSettings().get(HAS_CONFIG_FILE_KEY + solutionName);
		boolean hasConfigXml = hasConfig != null ? Utils.getAsBoolean(hasConfig) : true;
		if (hasConfigXml)
		{
			btnUseConfigXml.setSelection(true);
			String config_path = getDialogSettings().get(CONFIG_FILE_PATH_KEY + solutionName);
			if (config_path != null) configPath.setText(config_path);
			enableConfigXmlFields();
		}
		else
		{
			btnUsePhonegap.setSelection(true);
			applicationNameCombo.setText(getDialogSettings().get(APPLICATION_NAME_KEY + solutionName));
			txtDescription.setText(getDialogSettings().get(APPLICATION_DESCRIPTION_KEY + solutionName));
			txtVersion.setText(getDialogSettings().get(APPLICATION_VERSION_KEY + solutionName));
			btnPublic.setSelection(getDialogSettings().getBoolean(IS_APPLICATION_PUBLIC_KEY + solutionName));
			iconPath.setText(getDialogSettings().get(APPLICATION_ICON_KEY + solutionName));
			enablePhoneGapApplicationFields();
		}
		String[] selectedCertificates = loadSelectedCertificates();
		certificatesViewer.setInput(selectedCertificates);
		setSelectedCertificates(selectedCertificates);
		btnOpenPhonegapLink.setSelection(getDialogSettings().getBoolean(OPEN_PHONEGAP_BUILD_PAGE_KEY));
	}

	private String[] loadSelectedCertificates()
	{
		String certificates = getDialogSettings().get(CERTIFICATES_KEY + exporter.getSolutionName());
		if (certificates != null)
		{
			return certificates.split(",");
		}
		return null;
	}

	private void enableConfigXmlFields()
	{
		configPath.setEnabled(true);
		configBrowseButton.setEnabled(true);

		applicationNameCombo.setEnabled(false);
		txtVersion.setEnabled(false);
		txtDescription.setEnabled(false);
		iconPath.setEnabled(false);
		iconBrowseButton.setEnabled(false);
	}

	private void enablePhoneGapApplicationFields()
	{
		configPath.setEnabled(false);
		configBrowseButton.setEnabled(false);

		applicationNameCombo.setEnabled(true);
		txtVersion.setEnabled(true);
		txtDescription.setEnabled(true);
		iconPath.setEnabled(true);
		iconBrowseButton.setEnabled(true);
	}


	@Override
	public boolean canFlipToNextPage()
	{
		return false;
	}


	@Override
	public boolean isPageComplete()
	{
		return getErrorMessage() == null;
	}

	public PhoneGapApplication getPhoneGapApplication() throws Exception
	{
		super.setErrorMessage(null);
		if (isPageComplete())
		{
			saveDefaultData();
			EditorUtil.saveDirtyEditors(getShell(), true);
			final String[] errorMessage = new String[1];

			final File configFile = getConfigFile();

			String appName = null;
			String appVersion = null;
			String appDescription = null;
			String path = null;
			if (btnUsePhonegap.getSelection())
			{
				appName = applicationNameCombo.getText();
				appVersion = txtVersion.getText();
				appDescription = txtDescription.getText();
				path = iconPath.getText();
			}
			else
			{
				appName = getConfigApplicationName(errorMessage, configFile);
				if (errorMessage[0] != null)
				{
					throw new Exception(errorMessage[0]);
				}
			}

			String[] selectedCertificates = getSelectedCerticates();
			boolean appPublic = btnPublic.getSelection();

			return new PhoneGapApplication(appName, appVersion, appDescription, appPublic, path, selectedCertificates);
		}
		return null;
	}

	private String getConfigApplicationName(final String[] errorMessage, final File configFile)
	{
		try
		{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			Document document = dbf.newDocumentBuilder().parse(configFile);
			XPathFactory xpf = XPathFactory.newInstance();
			XPath xpath = xpf.newXPath();
			XPathExpression expression = xpath.compile("/widget/name");

			Node node = (Node)expression.evaluate(document, XPathConstants.NODE);
			if (node == null || node.getTextContent() == null || "".equals(node.getTextContent()))
			{
				errorMessage[0] = "The XML configuration file does not specify the application name.";
			}
			else
			{
				return node.getTextContent();
			}
		}
		catch (SAXException e)
		{
			errorMessage[0] = "The XML configuration file cannot be parsed: " + e.getMessage();
		}
		catch (Exception e)
		{
			errorMessage[0] = e.getMessage();
		}

		return null;
	}


	private void saveDefaultData()
	{
		String solutionName = exporter.getSolutionName();
		getDialogSettings().put(HAS_CONFIG_FILE_KEY + solutionName, btnUseConfigXml.getSelection());
		getDialogSettings().put(CONFIG_FILE_PATH_KEY + solutionName, configPath.getText());
		getDialogSettings().put(APPLICATION_NAME_KEY + solutionName, applicationNameCombo.getText());
		getDialogSettings().put(APPLICATION_DESCRIPTION_KEY + solutionName, txtDescription.getText());
		getDialogSettings().put(APPLICATION_VERSION_KEY + solutionName, txtVersion.getText());
		getDialogSettings().put(IS_APPLICATION_PUBLIC_KEY + solutionName, btnPublic.getSelection());
		getDialogSettings().put(APPLICATION_ICON_KEY + solutionName, iconPath.getText());
		storeSelectedCertificates();
		getDialogSettings().put(OPEN_PHONEGAP_BUILD_PAGE_KEY, btnOpenPhonegapLink.getSelection());
	}

	private void storeSelectedCertificates()
	{
		String[] selectedCertificates = getSelectedCerticates();
		String certificates = "";
		for (int i = 0; i < selectedCertificates.length; i++)
		{
			certificates += selectedCertificates[i];
			if (i < selectedCertificates.length - 1) certificates += ",";
		}
		if (!certificates.equals(""))
		{
			getDialogSettings().put(CERTIFICATES_KEY + exporter.getSolutionName(), certificates);
		}
	}

	@Override
	public String getErrorMessage()
	{
		//in case the export type is war, we don't care about these errors (to enable the 'Finish' button)
		if (warExportPage.isWarExport()) return null;

		if (btnUsePhonegap.getSelection() && (applicationNameCombo.getText() == null || "".equals(applicationNameCombo.getText())))
		{
			return "No PhoneGap application name specified.";
		}
		if (btnUseConfigXml.getSelection() && (configPath.getText() == null || "".equals(configPath.getText())))
		{
			return "No config.xml file specified.";
		}
		return super.getErrorMessage();
	}

	public PhoneGapConnector getConnector()
	{
		return connector;
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

	private void selectConfigType()
	{
		super.setErrorMessage(null);
		if (btnUseConfigXml.getSelection())
		{
			enableConfigXmlFields();
		}
		else
		{
			enablePhoneGapApplicationFields();
		}
		getWizard().getContainer().updateButtons();
	}

	private void selectConfigFile(final Shell outputBrowseShell)
	{
		super.setErrorMessage(null);
		FileDialog fileDialog = new FileDialog(outputBrowseShell, SWT.NONE);
		fileDialog.setFilterExtensions(new String[] { "*.xml" });
		if (fileDialog.open() != null)
		{
			configPath.setText(fileDialog.getFilterPath() + File.separator + fileDialog.getFileName());
		}
	}

	public File getConfigFile()
	{
		return (btnUseConfigXml.getSelection() && "".equals(configPath.getText())) ? null : new File(configPath.getText());
	}

	/**
	 * @return true if open phonegap page checkbox is checked
	 */
	public boolean openPhoneGapUrl()
	{
		return btnOpenPhonegapLink.getSelection();
	}

	public void setWarExportPage(WarExportPage warExportPage)
	{
		this.warExportPage = warExportPage;
	}
}
