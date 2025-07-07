/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.warexporter.ui.wizard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.warexporter.export.ExportWarModel;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.util.Utils;

/**
 *
 * @author jcompagner
 * @since 6.1
 */
public class ServoyPropertiesSelectionPage extends WizardPage implements Listener, IRestoreDefaultPage
{

	private final ExportWarModel exportModel;
	private Text fileNameText;
	private Button browseButton;
	private Text log4jConfigurationFileText;
	private Button browseLog4jConfigurationFileButton;
	private Text fileWebXmlNameText;
	private Button browseWebXmlButton;

	public ServoyPropertiesSelectionPage(ExportWarModel exportModel)
	{
		super("servoypropertyselection");
		this.exportModel = exportModel;
		setTitle("Choose an existing servoy properties file, log4j configuration file or web.xml (skip to generate default)");
		setDescription("Select the servoy properties file, log4j configuration file or web.xml that you want to use, skip if default should be generated");
	}

	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout(2, false);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);

		// servoy.properties
		Label propertiesText = new Label(composite, NONE);
		propertiesText.setText("Servoy properties file:");
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		propertiesText.setLayoutData(gd);
		fileNameText = new Text(composite, SWT.BORDER);
		fileNameText.addListener(SWT.KeyUp, this);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fileNameText.setLayoutData(gd);
		if (exportModel.getServoyPropertiesFileName() != null) fileNameText.setText(exportModel.getServoyPropertiesFileName());

		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addListener(SWT.Selection, this);

		// Log4jConfigurationFile
		Label log4jConfigurationText = new Label(composite, NONE);
		log4jConfigurationText.setText("Apache Log4j2 configuration file:");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		log4jConfigurationText.setLayoutData(gd);

		log4jConfigurationFileText = new Text(composite, SWT.BORDER);
		log4jConfigurationFileText.addListener(SWT.KeyUp, this);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		log4jConfigurationFileText.setLayoutData(gd);
		if (exportModel.getLog4jConfigurationFile() != null) log4jConfigurationFileText.setText(exportModel.getLog4jConfigurationFile());

		browseLog4jConfigurationFileButton = new Button(composite, SWT.PUSH);
		browseLog4jConfigurationFileButton.setText("Browse...");
		browseLog4jConfigurationFileButton.addListener(SWT.Selection, this);

		// web.xml
		Label webXmlText = new Label(composite, NONE);
		webXmlText.setText(
			"Take a web.xml from a generated war for adjustment and include it here, it must be a servoy war generated web.xml file to begin with:");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		webXmlText.setLayoutData(gd);


		fileWebXmlNameText = new Text(composite, SWT.BORDER);
		fileWebXmlNameText.addListener(SWT.KeyUp, this);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fileWebXmlNameText.setLayoutData(gd);
		if (exportModel.getWebXMLFileName() != null) fileWebXmlNameText.setText(exportModel.getWebXMLFileName());

		browseWebXmlButton = new Button(composite, SWT.PUSH);
		browseWebXmlButton.setText("Browse...");
		browseWebXmlButton.addListener(SWT.Selection, this);

		Button restoreDefaults = new Button(composite, SWT.PUSH);
		restoreDefaults.setText("Restore Defaults");
		restoreDefaults.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				restoreDefaults();
			}
		});

		setControl(composite);
		checkPageComplete();
	}

	public void handleEvent(Event event)
	{
		if (event.widget == fileNameText)
		{
			String potentialFileName = fileNameText.getText();
			exportModel.setServoyPropertiesFileName(potentialFileName);
			exportModel.setOverwriteSocketFactoryProperties(false);
		}
		else if (event.widget == log4jConfigurationFileText)
		{
			String potentialFileName = log4jConfigurationFileText.getText();
			exportModel.setLog4jConfigurationFile(potentialFileName);
		}
		else if (event.widget == fileWebXmlNameText)
		{
			String potentialFileName = fileWebXmlNameText.getText();
			exportModel.setWebXMLFileName(potentialFileName);
		}
		else if (event.widget == browseButton || event.widget == browseLog4jConfigurationFileButton || event.widget == browseWebXmlButton)
		{
			Shell shell = new Shell();
			GridLayout gridLayout = new GridLayout();
			shell.setLayout(gridLayout);
			FileDialog dlg = new FileDialog(shell, SWT.OPEN);
			if (exportModel.getWarFileName() != null)
			{
				String fileName = null;
				if (event.widget == browseButton)
				{
					fileName = exportModel.getServoyPropertiesFileName();
					if (fileName == null) fileName = "servoy.properties";
				}
				else if (event.widget == browseLog4jConfigurationFileButton)
				{
					fileName = exportModel.getLog4jConfigurationFile();
					if (fileName == null) fileName = "log4j.xml";

				}
				else if (event.widget == browseWebXmlButton)
				{
					fileName = exportModel.getWebXMLFileName();
					if (fileName == null) fileName = "web.xml";

				}
				File f = new File(fileName);
				if (f.isDirectory())
				{
					dlg.setFilterPath(f.getAbsolutePath());
					dlg.setFileName(null);
				}
				else
				{
					dlg.setFilterPath(f.getParent());
					dlg.setFileName(f.getName());
				}
			}
			if (event.widget == browseButton)
			{
				String[] extensions = { "*.properties" };
				dlg.setFilterExtensions(extensions);
			}
			else if (event.widget == browseLog4jConfigurationFileButton)
			{
				String[] extensions = { "*.xml", "*.json", "*.jsn", "*.yaml", "*.yml", "*.properties" };
				dlg.setFilterExtensions(extensions);
			}
			else
			{
				String[] extensions = { "*.xml" };
				dlg.setFilterExtensions(extensions);
			}
			String chosenFileName = dlg.open();
			if (chosenFileName != null)
			{
				if (event.widget == browseButton)
				{
					exportModel.setServoyPropertiesFileName(chosenFileName);
					fileNameText.setText(chosenFileName);
				}
				else if (event.widget == browseLog4jConfigurationFileButton)
				{
					exportModel.setLog4jConfigurationFile(chosenFileName);
					log4jConfigurationFileText.setText(chosenFileName);
				}
				else if (event.widget == browseWebXmlButton)
				{
					exportModel.setWebXMLFileName(chosenFileName);
					fileWebXmlNameText.setText(chosenFileName);
				}
			}
		}
		checkPageComplete();

		getWizard().getContainer().updateButtons();
		getWizard().getContainer().updateMessage();
	}

	private void checkPageComplete()
	{
		exportModel.setServoyPropertiesFileName(fileNameText.getText());
		boolean messageSet = false;
		if (exportModel.getServoyPropertiesFileName() != null) checkLicenses(); // this can set WARNING message on wizard page

		boolean servoyPropertiesFileIsGiven = (exportModel.getServoyPropertiesFileName() != null);
		if (servoyPropertiesFileIsGiven)
		{
			String checkFileMessage = exportModel.checkServoyPropertiesFileExists();
			if (checkFileMessage != null)
			{
				setMessage(checkFileMessage, IMessageProvider.ERROR);
				messageSet = true;
			}
			else
			{
				try (FileInputStream fis = new FileInputStream(new File(exportModel.getServoyPropertiesFileName())))
				{
					Properties prop = new Properties();
					prop.load(fis);

					// just make sure the model is now set to follow the given servoy properties file
					exportModel.setStartRMI(false);

					String numberOfServers = prop.getProperty("ServerManager.numberOfServers");
					if (numberOfServers != null)
					{
						int nrOfServers = Utils.getAsInteger(numberOfServers.trim(), false);
						boolean repositoryExists = false;
						for (int i = 0; i < nrOfServers && !repositoryExists; i++)
						{
							String serverName = prop.getProperty("server." + i + ".serverName");
							if (serverName.equals(IServer.REPOSITORY_SERVER)) repositoryExists = true;
						}
						if (!repositoryExists)
						{
							setMessage("Servoy properties file '" + exportModel.getServoyPropertiesFileName() +
								"' is not valid because it doesn't contain 'repository_server' database (which is required).", IMessageProvider.ERROR);
							messageSet = true;
						}

					}
					else
					{
						setMessage("File '" + exportModel.getServoyPropertiesFileName() +
							"' doesn't look like a valid servoy properties file; no database servers are defined (ServerManager.numberOfServers is not defined).",
							IMessageProvider.ERROR);
						messageSet = true;
					}
				}
				catch (IOException e)
				{
					setMessage("Couldn't load the servoy properties file: " + exportModel.getServoyPropertiesFileName() + ", error: " + e.getMessage(),
						IMessageProvider.ERROR);
					messageSet = true;
				}

			}
		}

		if (!messageSet)
		{
			exportModel.setLog4jConfigurationFile(StringUtils.defaultIfBlank(log4jConfigurationFileText.getText(), null));
			String message = exportModel.checkLog4jConfigurationFile();
			if (message != null)
			{
				setMessage(message, ERROR);
				messageSet = true;
			}
		}

		if (!messageSet)
		{
			exportModel.setWebXMLFileName(StringUtils.defaultIfBlank(fileWebXmlNameText.getText(), null));
			String message = exportModel.checkWebXML();
			if (message != null)
			{
				setMessage(message, ERROR);
				messageSet = true;
			}
		}

		if (!messageSet)
		{
			setMessage(null);
		}

		setPageComplete(getMessageType() != IMessageProvider.ERROR);
	}

	protected void checkLicenses()
	{
		final Object[] upgrade = exportModel.checkAndAutoUpgradeLicenses();
		if (upgrade != null && upgrade.length >= 3)
		{
			if (!Utils.getAsBoolean(upgrade[0]))
			{
				setMessage("License code '" + upgrade[1] + "' defined in the selected properties file is invalid." + (upgrade[2] != null ? upgrade[2] : ""),
					WARNING);
			}
			else
			{
				Display.getDefault().asyncExec(() -> {
					String message = "License code '" + upgrade[1] + "' was auto upgraded to '" + upgrade[2] +
						"' but the changes could not be written to the selected properties file. Please adjust the '" +
						exportModel.getServoyPropertiesFileName() + "' file manually.";
					setMessage(message, WARNING);
					ServoyLog.logInfo(message);
					MessageDialog.openWarning(getShell(), "Could not save changes to the properties file", message);
				});
			}
		}
	}

	@Override
	public IWizardPage getNextPage()
	{
		if (exportModel.getServoyPropertiesFileName() == null)
		{
			return super.getNextPage();
		}
		return null;
	}

	@Override
	public void restoreDefaults()
	{
		fileNameText.setText("");
		log4jConfigurationFileText.setText("");
		fileWebXmlNameText.setText("");
		exportModel.setServoyPropertiesFileName(null);
		exportModel.setLog4jConfigurationFile(null);
		exportModel.setWebXMLFileName(null);
		checkPageComplete();

		getWizard().getContainer().updateButtons();
		getWizard().getContainer().updateMessage();
	}

	@Override
	public void performHelp()
	{
		PlatformUI.getWorkbench().getHelpSystem().displayHelp("com.servoy.eclipse.exporter.war.export_war_properties");
	}
}