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

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.warexporter.export.ExportWarModel;
import com.servoy.j2db.persistence.IServer;

/**
 *
 * @author jcompagner
 * @since 6.1
 */
public class ServoyPropertiesSelectionPage extends WizardPage implements Listener
{
	/**
	 *
	 */
	private final ExportWarModel exportModel;
	private Text fileNameText;
	private Button browseButton;
	private final IWizardPage nextPage;

	public ServoyPropertiesSelectionPage(ExportWarModel exportModel, IWizardPage nextPage)
	{
		super("servoypropertyselection");
		this.exportModel = exportModel;
		this.nextPage = nextPage;
		setTitle("Choose an existing servoy properties file (skip to generate default)");
		setDescription("Select the servoy properties file that you want to use, skip if default should be generated");
	}

	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout(2, false);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);

		fileNameText = new Text(composite, SWT.BORDER);
		fileNameText.addListener(SWT.KeyUp, this);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		fileNameText.setLayoutData(gd);
		if (exportModel.getServoyPropertiesFileName() != null) fileNameText.setText(exportModel.getServoyPropertiesFileName());

		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addListener(SWT.Selection, this);

		setControl(composite);
	}

	public void handleEvent(Event event)
	{
		if (event.widget == fileNameText)
		{
			String potentialFileName = fileNameText.getText();
			exportModel.setServoyPropertiesFileName(potentialFileName);
			exportModel.setOverwriteSocketFactoryProperties(false);
		}
		else if (event.widget == browseButton)
		{
			Shell shell = new Shell();
			GridLayout gridLayout = new GridLayout();
			shell.setLayout(gridLayout);
			FileDialog dlg = new FileDialog(shell, SWT.SAVE);
			if (exportModel.getWarFileName() != null)
			{
				String fileName = exportModel.getServoyPropertiesFileName();
				if (fileName == null) fileName = "servoy.properties";
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
			String[] extensions = { "*.properties" };
			dlg.setFilterExtensions(extensions);
			String chosenFileName = dlg.open();
			if (chosenFileName != null)
			{
				exportModel.setServoyPropertiesFileName(chosenFileName);
				fileNameText.setText(chosenFileName);
			}
		}
		canFlipToNextPage();
		getWizard().getContainer().updateButtons();
		getWizard().getContainer().updateMessage();
	}

	@Override
	public boolean canFlipToNextPage()
	{
		exportModel.setServoyPropertiesFileName(fileNameText.getText());
		boolean messageSet = false;
		boolean result = exportModel.getServoyPropertiesFileName() == null;
		if (!result)
		{
			File f = new File(exportModel.getServoyPropertiesFileName());
			if (!f.exists())
			{
				setMessage("Specified servoy properties file doesn't exist.", IMessageProvider.WARNING);
				messageSet = true;
			}
			else if (f.isDirectory())
			{
				setMessage("Specified servoy properties file is a folder.", IMessageProvider.WARNING);
				messageSet = true;
			}
			else
			{
				Properties prop = new Properties();
				FileInputStream fis = null;
				try
				{
					fis = new FileInputStream(f);
					prop.load(fis);

					String numberOfServers = prop.getProperty("ServerManager.numberOfServers");
					if (numberOfServers != null)
					{
						int nrOfServers = Integer.parseInt(numberOfServers);
						boolean repositoryExists = false;
						for (int i = 0; i < nrOfServers && !repositoryExists; i++)
						{
							String serverName = prop.getProperty("server." + i + ".serverName");
							if (serverName.equals(IServer.REPOSITORY_SERVER)) repositoryExists = true;
						}
						if (!repositoryExists)
						{
							setMessage("Servoy properties file: " + exportModel.getServoyPropertiesFileName() +
								" is not valid because it doesn't contain repository_server database which is required.", IMessageProvider.WARNING);
							messageSet = true;
						}

					}
					else
					{
						setMessage("Servoy properties file: " + exportModel.getServoyPropertiesFileName() +
							" doesn't look like a valid servoy properties file, no servers configured", IMessageProvider.WARNING);
						messageSet = true;
					}

					String rmiServerFactory = prop.getProperty("SocketFactory.rmiServerFactory");
					if (exportModel.getStartRMI() && !exportModel.allowOverwriteSocketFactoryProperties() &&
						(rmiServerFactory == null || !rmiServerFactory.equals("com.servoy.j2db.server.rmi.tunnel.ServerTunnelRMISocketFactoryFactory")))
					{
						final Shell shell = getShell();
						final boolean[] ok = new boolean[1];
						Display.getDefault().syncExec(new Runnable()
						{
							public void run()
							{
								ok[0] = MessageDialog.openConfirm(
									shell,
									"Overwrite SocketFactory properties",
									"In the selected properties file SocketFactory.rmiServerFactory is not set to 'com.servoy.j2db.server.rmi.tunnel.ServerTunnelRMISocketFactoryFactory'. Please allow exporter to overwrite properties or cancel the export.");
							}
						});
						if (ok[0])
						{
							exportModel.setOverwriteSocketFactoryProperties(true);
						}
						else
						{
							Display.getDefault().asyncExec(new Runnable()
							{
								public void run()
								{
									getWizard().getContainer().getShell().close();
								}
							});
						}
					}
				}
				catch (IOException e)
				{
					setMessage("Couldn't load the servoy properties file: " + exportModel.getServoyPropertiesFileName() + ", error: " + e.getMessage(),
						IMessageProvider.WARNING);
					messageSet = true;
				}
				finally
				{
					try
					{
						if (fis != null) fis.close();
					}
					catch (IOException e)
					{
						// ignore
					}
				}

			}
		}
		if (!messageSet)
		{
			setMessage(null);
		}
		return result;
	}

	@Override
	public IWizardPage getNextPage()
	{
		if (exportModel.getServoyPropertiesFileName() == null) return nextPage;
		return null;
	}
}