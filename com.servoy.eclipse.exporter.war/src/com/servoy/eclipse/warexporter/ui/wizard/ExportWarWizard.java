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


import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import com.servoy.eclipse.warexporter.Activator;
import com.servoy.eclipse.warexporter.export.ExportException;
import com.servoy.eclipse.warexporter.export.ExportWarModel;
import com.servoy.eclipse.warexporter.export.Exporter;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.Debug;


/**
 * 
 * @author jcompagner
 * @since 6.1
 */
public class ExportWarWizard extends Wizard implements IExportWizard
{

	private FileSelectionPage fileSelectionPage;

	private DirectorySelectionPage pluginSelectionPage;
	private DirectorySelectionPage beanSelectionPage;

	private ExportWarModel exportModel;

	private volatile boolean errorFlag;

	private ServersSelectionPage serversSelectionPage;

	private DirectorySelectionPage driverSelectionPage;

	private ServoyPropertiesSelectionPage servoyPropertiesSelectionPage;

	private DirectorySelectionPage lafSelectionPage;

	private ServoyPropertiesConfigurationPage servoyPropertiesConfigurationPage;

	public ExportWarWizard()
	{
		setWindowTitle("War Export"); //$NON-NLS-1$
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("WarExportWizard");//$NON-NLS-1$
		if (section == null)
		{
			section = workbenchSettings.addNewSection("WarExportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
		setNeedsProgressMonitor(true);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		exportModel = new ExportWarModel(getDialogSettings());

	}

	@Override
	public boolean performFinish()
	{
		driverSelectionPage.storeInput();
		pluginSelectionPage.storeInput();
		beanSelectionPage.storeInput();
		lafSelectionPage.storeInput();
		serversSelectionPage.storeInput();

		exportModel.saveSettings(getDialogSettings());
		errorFlag = false;
		IRunnableWithProgress job = new IRunnableWithProgress()
		{
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				Exporter exporter = new Exporter(exportModel);
				try
				{
					exporter.doExport(monitor);
				}
				catch (final ExportException e)
				{
					errorFlag = true;
					Debug.error(e);
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							MessageDialog.openError(getShell(), "Error creating the WAR file", e.getMessage()); //$NON-NLS-1$
						}
					});
				}
			}
		};
		try
		{
			getContainer().run(true, false, job);
		}
		catch (Exception e)
		{
			Debug.error(e);
			return false;
		}
		return !errorFlag;
	}

	@SuppressWarnings("nls")
	@Override
	public void addPages()
	{
		HashMap<String, IWizardPage> serverConfigurationPages = new HashMap<String, IWizardPage>();

		serversSelectionPage = new ServersSelectionPage("serverspage", "Choose the database servernames to export",
			"Select the database server names that will be used on the application server", exportModel.getSelectedServerNames(), serverConfigurationPages);
		servoyPropertiesConfigurationPage = new ServoyPropertiesConfigurationPage("propertiespage", exportModel, serversSelectionPage);
		servoyPropertiesSelectionPage = new ServoyPropertiesSelectionPage(exportModel, servoyPropertiesConfigurationPage);
		driverSelectionPage = new DirectorySelectionPage("driverpage", "Choose the jdbc drivers to export",
			"Select the jdbc drivers that you want to use in the war (if the app server doesn't provide them)",
			ApplicationServerSingleton.get().getServerManager().getDriverDir(), exportModel.getDrivers(), new String[] { "hsqldb.jar" },
			servoyPropertiesSelectionPage);
		lafSelectionPage = new DirectorySelectionPage("lafpage", "Choose the lafs to export", "Select the lafs that you want to use in the war",
			ApplicationServerSingleton.get().getLafManager().getLAFDir(), exportModel.getLafs(), null, driverSelectionPage);
		beanSelectionPage = new DirectorySelectionPage("beanpage", "Choose the beans to export", "Select the beans that you want to use in the war",
			ApplicationServerSingleton.get().getBeanManager().getBeanDir(), exportModel.getBeans(), null, lafSelectionPage);
		pluginSelectionPage = new DirectorySelectionPage("pluginpage", "Choose the plugins to export", "Select the plugins that you want to use in the war",
			ApplicationServerSingleton.get().getPluginManager().getPluginDir(), exportModel.getPlugins(), null, beanSelectionPage);
		fileSelectionPage = new FileSelectionPage(exportModel, pluginSelectionPage);
		addPage(fileSelectionPage);
		addPage(pluginSelectionPage);
		addPage(beanSelectionPage);
		addPage(lafSelectionPage);
		addPage(driverSelectionPage);
		addPage(servoyPropertiesSelectionPage);
		addPage(servoyPropertiesConfigurationPage);
		addPage(serversSelectionPage);

		String[] serverNames = ApplicationServerSingleton.get().getServerManager().getServerNames(true, true, true, false);
		for (String serverName : serverNames)
		{
			ServerConfigurationPage configurationPage = new ServerConfigurationPage("serverconf:" + serverName, exportModel.getServerConfiguration(serverName),
				exportModel.getSelectedServerNames(), serverConfigurationPages);
			addPage(configurationPage);
			serverConfigurationPages.put(serverName, configurationPage);
		}

	}

	@Override
	public boolean canFinish()
	{
		IWizardPage currentPage = getContainer().getCurrentPage();
		if (currentPage instanceof ServersSelectionPage || currentPage instanceof ServerConfigurationPage ||
			currentPage instanceof ServoyPropertiesSelectionPage)
		{
			return currentPage.getNextPage() == null;
		}
		return false;
	}
}
