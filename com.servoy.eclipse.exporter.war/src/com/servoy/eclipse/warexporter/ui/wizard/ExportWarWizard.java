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
import java.io.FileWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.model.war.exporter.ExportException;
import com.servoy.eclipse.model.war.exporter.ServerConfiguration;
import com.servoy.eclipse.model.war.exporter.WarExporter;
import com.servoy.eclipse.ui.export.ExportSolutionJob;
import com.servoy.eclipse.warexporter.Activator;
import com.servoy.eclipse.warexporter.export.ExportWarModel;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
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
		setWindowTitle("War Export");
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("WarExportWizard");
		if (section == null)
		{
			section = workbenchSettings.addNewSection("WarExportWizard");
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
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				final WarExporter exporter = new WarExporter(exportModel)
				{
					@Override
					protected void copyActiveSolution(java.io.File tmpWarDir) throws ExportException
					{
						super.copyActiveSolution(tmpWarDir);
						try
						{
							exportModel.setFileName(new File(tmpWarDir, "WEB-INF/solution.servoy").getCanonicalPath());
							FlattenedSolution solution = ServoyModelFinder.getServoyModel().getActiveProject().getFlattenedSolution();
							ExportSolutionJob exportSolutionJob = new ExportSolutionJob("export solution", exportModel, solution.getSolution(), false, false,
								new WorkspaceFileAccess(ResourcesPlugin.getWorkspace()));
							SubMonitor subMonitor = SubMonitor.convert(monitor);
							exportSolutionJob.runInWorkspace(subMonitor.newChild(10));

							File importProperties = new File(tmpWarDir, "WEB-INF/import.properties");
							Properties prop = new Properties();
							prop.setProperty("overwriteGroups", Boolean.toString(exportModel.isOverwriteGroups()));
							prop.setProperty("allowSQLKeywords", Boolean.toString(exportModel.isAllowSQLKeywords()));
							prop.setProperty("overrideSequenceTypes", Boolean.toString(exportModel.isOverrideSequenceTypes()));
							prop.setProperty("overrideDefaultValues", Boolean.toString(exportModel.isOverrideDefaultValues()));
							prop.setProperty("insertNewI18NKeysOnly", Boolean.toString(exportModel.isInsertNewI18NKeysOnly()));
							prop.setProperty("importUserPolicy", Integer.toString(exportModel.getImportUserPolicy()));
							prop.setProperty("addUsersToAdminGroup", Boolean.toString(exportModel.isAddUsersToAdminGroup()));
							prop.setProperty("allowDataModelChange", Boolean.toString(exportModel.isAllowDataModelChanges()));
							prop.setProperty("updateSequences", Boolean.toString(exportModel.isUpdateSequences()));
							FileWriter writer = new FileWriter(importProperties);
							try
							{
								prop.store(writer, "import properties");
							}
							finally
							{
								writer.close();
							}
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
				};
				try
				{
					final boolean[] cancel = new boolean[] { false };
					Display.getDefault().syncExec(new Runnable()
					{
						public void run()
						{
							String missingJarName = exporter.searchExportedPlugins();
							while (missingJarName != null)
							{
								DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.OPEN);
								dialog.setMessage("Please select the directory where " + missingJarName + " is located");
								String chosenDirName = dialog.open();
								if (chosenDirName != null)
								{
									exportModel.addPluginLocations(chosenDirName);
									missingJarName = exporter.searchExportedPlugins();
								}
								else
								{
									cancel[0] = true;
									return;
								}
							}
						}
					});
					if (!cancel[0]) exporter.doExport(monitor);
				}
				catch (final ExportException e)
				{
					errorFlag = true;
					Debug.error(e);
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							MessageDialog.openError(getShell(), "Error creating the WAR file", e.getMessage());
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

	@Override
	public void addPages()
	{
		HashMap<String, IWizardPage> serverConfigurationPages = new HashMap<String, IWizardPage>();

		serversSelectionPage = new ServersSelectionPage("serverspage", "Choose the database servernames to export",
			"Select the database server names that will be used on the application server", exportModel.getSelectedServerNames(),
			new String[] { IServer.REPOSITORY_SERVER }, serverConfigurationPages);
		servoyPropertiesConfigurationPage = new ServoyPropertiesConfigurationPage("propertiespage", exportModel, serversSelectionPage);
		servoyPropertiesSelectionPage = new ServoyPropertiesSelectionPage(exportModel, servoyPropertiesConfigurationPage);
		driverSelectionPage = new DirectorySelectionPage("driverpage", "Choose the jdbc drivers to export",
			"Select the jdbc drivers that you want to use in the war (if the app server doesn't provide them)",
			ApplicationServerRegistry.get().getServerManager().getDriversDir(), exportModel.getDrivers(), new String[] { "hsqldb.jar" },
			servoyPropertiesSelectionPage);
		lafSelectionPage = new DirectorySelectionPage("lafpage", "Choose the lafs to export", "Select the lafs that you want to use in the war",
			ApplicationServerRegistry.get().getLafManager().getLAFDir(), exportModel.getLafs(), null, driverSelectionPage);
		beanSelectionPage = new DirectorySelectionPage("beanpage", "Choose the beans to export", "Select the beans that you want to use in the war",
			ApplicationServerRegistry.get().getBeanManager().getBeansDir(), exportModel.getBeans(), null, lafSelectionPage);
		pluginSelectionPage = new DirectorySelectionPage("pluginpage", "Choose the plugins to export", "Select the plugins that you want to use in the war",
			ApplicationServerRegistry.get().getPluginManager().getPluginsDir(), exportModel.getPlugins(), null, beanSelectionPage);
		fileSelectionPage = new FileSelectionPage(exportModel, pluginSelectionPage);
		addPage(fileSelectionPage);
		addPage(pluginSelectionPage);
		addPage(beanSelectionPage);
		addPage(lafSelectionPage);
		addPage(driverSelectionPage);
		addPage(servoyPropertiesSelectionPage);
		addPage(servoyPropertiesConfigurationPage);
		addPage(serversSelectionPage);

		String[] serverNames = ApplicationServerRegistry.get().getServerManager().getServerNames(true, true, true, false);
		ArrayList<String> srvNames = new ArrayList<String>(Arrays.asList(serverNames));
		boolean repositoryServerPresent = true;
		if (!srvNames.contains(IServer.REPOSITORY_SERVER))
		{
			repositoryServerPresent = false;
			srvNames.add(IServer.REPOSITORY_SERVER);
		}
		for (String serverName : srvNames)
		{
			ServerConfiguration serverConfiguration = exportModel.getServerConfiguration(serverName);
			//handle required repository_server if not present in the servers list
			if (serverName.equals(IServer.REPOSITORY_SERVER) && !repositoryServerPresent)
			{
				//set some default configuration
				serverConfiguration.setDriver((exportModel.getServerConfiguration(srvNames.get(0))).getDriver());
				serverConfiguration.setUserName((exportModel.getServerConfiguration(srvNames.get(0))).getUserName());
				serverConfiguration.setPassword((exportModel.getServerConfiguration(srvNames.get(0))).getPassword());
				serverConfiguration.setMaxActive((exportModel.getServerConfiguration(srvNames.get(0))).getMaxActive());
				serverConfiguration.setMaxIdle((exportModel.getServerConfiguration(srvNames.get(0))).getMaxIdle());
				serverConfiguration.setMaxPreparedStatementsIdle((exportModel.getServerConfiguration(srvNames.get(0))).getMaxPreparedStatementsIdle());
			}
			ServerConfigurationPage configurationPage = new ServerConfigurationPage("serverconf:" + serverName, serverConfiguration,
				exportModel.getSelectedServerNames(), serverConfigurationPages);
			addPage(configurationPage);
			serverConfigurationPages.put(serverName, configurationPage);
		}

	}

	@Override
	public boolean canFinish()
	{
		IWizardPage currentPage = getContainer().getCurrentPage();
		if (currentPage instanceof ServoyPropertiesSelectionPage && ((ServoyPropertiesSelectionPage)currentPage).getMessageType() == IMessageProvider.WARNING)
		{
			return false; //if any warning about the selected properties file, disable finish
		}
		if (currentPage instanceof ServersSelectionPage || currentPage instanceof ServerConfigurationPage ||
			currentPage instanceof ServoyPropertiesSelectionPage)
		{
			return currentPage.getNextPage() == null;
		}
		return false;
	}
}
