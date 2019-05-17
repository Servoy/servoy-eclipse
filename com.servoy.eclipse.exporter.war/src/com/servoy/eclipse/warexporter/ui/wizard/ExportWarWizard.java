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


import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.BuilderUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel.License;
import com.servoy.eclipse.model.war.exporter.ExportException;
import com.servoy.eclipse.model.war.exporter.ServerConfiguration;
import com.servoy.eclipse.model.war.exporter.WarExporter;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.wizards.ICopyWarToCommandLineWizard;
import com.servoy.eclipse.ui.wizards.IRestoreDefaultWizard;
import com.servoy.eclipse.warexporter.Activator;
import com.servoy.eclipse.warexporter.export.ExportWarModel;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;


/**
 *
 * @author jcompagner
 * @since 6.1
 */
public class ExportWarWizard extends Wizard implements IExportWizard, IRestoreDefaultWizard, ICopyWarToCommandLineWizard
{

	private FileSelectionPage fileSelectionPage;

	private DirectorySelectionPage pluginSelectionPage;
	private DirectorySelectionPage beanSelectionPage;

	private ExportWarModel exportModel;

	private volatile boolean errorFlag;

	private ServersSelectionPage serversSelectionPage;

	private DirectorySelectionPage driverSelectionPage;

	private DefaultAdminConfigurationPage defaultAdminConfigurationPage;

	private ServoyPropertiesSelectionPage servoyPropertiesSelectionPage;

	private DirectorySelectionPage lafSelectionPage;

	private ServoyPropertiesConfigurationPage servoyPropertiesConfigurationPage;

	private AbstractComponentsSelectionPage componentsSelectionPage;

	private WizardPage errorPage;

	private ServicesSelectionPage servicesSelectionPage;

	private LicensePage licenseConfigurationPage;

	private DeployConfigurationPage userHomeSelectionPage;

	private boolean isNGExport;

	private ListSelectionPage noneActiveSolutionPage;

	public ExportWarWizard()
	{
		setWindowTitle("War Export");
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		if (activeProject != null)
		{
			IDialogSettings section = DialogSettings.getOrCreateSection(workbenchSettings, "WarExportWizard:" + activeProject.getSolution().getName());
			setDialogSettings(section);
		}
		else
		{
			IDialogSettings section = DialogSettings.getOrCreateSection(workbenchSettings, "WarExportWizard");
			setDialogSettings(section);
		}
		setNeedsProgressMonitor(true);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		ServoyProject activeProject;
		if ((activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject()) == null)
		{
			createErrorPage("No active Servoy solution project found", "No active Servoy solution project found",
				"Please activate a Servoy solution project before trying to export");
		}
		else if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject() == null)
		{
			createErrorPage("No active Resource project found", "No active Resource project found",
				"Please activate a Resource project before trying to export");
		}
		else if (BuilderUtils.getMarkers(activeProject) == BuilderUtils.HAS_ERROR_MARKERS)
		{
			createErrorPage("Solution with errors", "Solution with errors", "Cannot export solution with errors, please fix them first");
		}
		else
		{
			int solutionType = activeProject.getSolutionMetaData().getSolutionType();
			isNGExport = solutionType != SolutionMetaData.WEB_CLIENT_ONLY && solutionType != SolutionMetaData.SMART_CLIENT_ONLY;
			exportModel = new ExportWarModel(getDialogSettings(), isNGExport);
		}
	}

	private void createErrorPage(String pageName, String title, String errorMessage)
	{
		errorPage = new WizardPage(pageName)
		{
			public void createControl(Composite parent)
			{
				setControl(new Composite(parent, SWT.NONE));
			}
		};
		errorPage.setTitle(title);
		errorPage.setErrorMessage(errorMessage);
		errorPage.setPageComplete(false);
	}

	@Override
	public boolean performFinish()
	{
		// check if finish is pressed on the first page, to ask if the file can be overwritten or not.
		if (getContainer().getCurrentPage() instanceof FileSelectionPage && getContainer().getCurrentPage().getNextPage() == null)
		{
			return false;
		}
		noneActiveSolutionPage.storeInput();
		driverSelectionPage.storeInput();
		pluginSelectionPage.storeInput();
		beanSelectionPage.storeInput();
		lafSelectionPage.storeInput();
		serversSelectionPage.storeInput();

		if (exportModel.getServoyPropertiesFileName() != null)
		{
			String checkFile = exportModel.checkServoyPropertiesFileExists();
			if (checkFile == null)
			{
				final Object[] upgrade = exportModel.checkAndAutoUpgradeLicenses();
				if (upgrade != null && upgrade.length >= 3)
				{
					if (!Utils.getAsBoolean(upgrade[0]))
					{
						getContainer().showPage(servoyPropertiesSelectionPage);
						servoyPropertiesSelectionPage.setErrorMessage(
							"License code '" + upgrade[1] + "' defined in the selected properties file is invalid." + (upgrade[2] != null ? upgrade[2] : ""));
						return false;
					}
					else
					{
						Display.getDefault().asyncExec(() -> {
							String message = "License code '" + upgrade[1] + "' was auto upgraded to '" + upgrade[2] +
								"'. The export contains the new license code, but the changes could not be written to the selected properties file. Please adjust the '" +
								exportModel.getServoyPropertiesFileName() + "' file manually.";
							ServoyLog.logInfo(message);
							MessageDialog.openWarning(getShell(), "Could not save changes to the properties file", message);
						});
					}
				}
			}
			else
			{
				Display.getDefault().asyncExec(() -> {
					MessageDialog.openError(getShell(), "Error creating the WAR file", checkFile);
				});
			}
		}
		else
		{
			String code = null;
			if ((code = checkAndAutoUpgradeLicenses()) != null)
			{
				getContainer().showPage(licenseConfigurationPage);
				licenseConfigurationPage.setErrorMessage("License " + code + " is not valid and cannot be auto upgraded.");
				return false;
			}
		}

		exportModel.saveSettings(getDialogSettings());
		errorFlag = false;
		IRunnableWithProgress job = new IRunnableWithProgress()
		{
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				try
				{
					final WarExporter exporter = new WarExporter(exportModel);
					final boolean[] cancel = new boolean[] { false };
					Display.getDefault().syncExec(new Runnable()
					{
						public void run()
						{
							String missingJarName = exporter.searchExportedPlugins();
							while (missingJarName != null)
							{
								DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.OPEN);
								dialog.setMessage(
									"Please select the directory where " + missingJarName + " is located (usually your servoy developer/plugins directory)");
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
							exportModel.saveSettings(getDialogSettings());
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
		catch (

		Exception e)
		{
			Debug.error(e);
			return false;
		}
		return !errorFlag;
	}


	private String checkAndAutoUpgradeLicenses()
	{
		IApplicationServerSingleton server = ApplicationServerRegistry.get();
		for (License l : exportModel.getLicenses())
		{
			if (!server.checkClientLicense(l.getCompanyKey(), l.getCode(), l.getNumberOfLicenses()))
			{
				//try to auto upgrade
				Pair<Boolean, String> code = server.upgradeLicense(l.getCompanyKey(), l.getCode(), l.getNumberOfLicenses());
				if (code == null || !code.getLeft().booleanValue())
				{
					if (code != null)
					{
						Display.getDefault().asyncExec(new Runnable()
						{
							public void run()
							{
								MessageDialog.openError(getShell(), "Error creating the WAR file",
									"License " + l.getCode() + (!code.getLeft().booleanValue() ? " error: " + code.getRight() : " is not valid."));
							}
						});
					}
					return l.getCode();
				}
				else if (code.getLeft().booleanValue() && !l.getCode().equals(code.getRight()))
				{
					exportModel.replaceLicenseCode(l, code.getRight());
				}
			}
		}
		return null;
	}

	@Override
	public void addPages()
	{
		if (errorPage != null)
		{
			addPage(errorPage);
		}
		else
		{
			HashMap<String, IWizardPage> serverConfigurationPages = new HashMap<String, IWizardPage>();

			serversSelectionPage = new ServersSelectionPage("serverspage", "Choose the database servernames to export",
				"Select the database server names that will be used on the application server", exportModel.getSelectedServerNames(),
				new String[] { IServer.REPOSITORY_SERVER }, serverConfigurationPages);
			licenseConfigurationPage = new LicensePage("licensepage", "Enter license key",
				"Please enter the Servoy client license key(s), or leave empty for running the solution in trial mode.", exportModel);
			servoyPropertiesConfigurationPage = new ServoyPropertiesConfigurationPage("propertiespage", exportModel);
			userHomeSelectionPage = new DeployConfigurationPage("userhomepage", exportModel);
			servoyPropertiesSelectionPage = new ServoyPropertiesSelectionPage(exportModel, this);
			if (isNGExport)
			{
				componentsSelectionPage = new ComponentsSelectionPage(exportModel, WebComponentSpecProvider.getSpecProviderState(), "componentspage",
					"Select components to export", "View the components used and select others which you want to export.");
				servicesSelectionPage = new ServicesSelectionPage(exportModel, WebServiceSpecProvider.getSpecProviderState(), "servicespage",
					"Select services to export", "View the services used and select others which you want to export.");
			}
			defaultAdminConfigurationPage = new DefaultAdminConfigurationPage("defaultAdminPage", exportModel);
			driverSelectionPage = new DirectorySelectionPage("driverpage", "Choose the jdbc drivers to export",
				"Select the jdbc drivers that you want to use in the war (if the app server doesn't provide them)",
				ApplicationServerRegistry.get().getServerManager().getDriversDir(), exportModel.getDrivers(), new String[] { "hsqldb.jar" },
				getDialogSettings().get("export.drivers") == null, false, "export_war_drivers");
			lafSelectionPage = new DirectorySelectionPage("lafpage", "Choose the lafs to export", "Select the lafs that you want to use in the war",
				ApplicationServerRegistry.get().getLafManager().getLAFDir(), exportModel.getLafs(), null, getDialogSettings().get("export.lafs") == null, false,
				"export_war_lafs");
			beanSelectionPage = new DirectorySelectionPage("beanpage", "Choose the beans to export", "Select the beans that you want to use in the war",
				ApplicationServerRegistry.get().getBeanManager().getBeansDir(), exportModel.getBeans(), null, getDialogSettings().get("export.beans") == null,
				false, "export_war_beans");
			pluginSelectionPage = new DirectorySelectionPage("pluginpage", "Choose the plugins to export", "Select the plugins that you want to use in the war",
				ApplicationServerRegistry.get().getPluginManager().getPluginsDir(), exportModel.getPlugins(), null,
				getDialogSettings().get("export.plugins") == null, true, "export_war_plugins");

			ArrayList<String> tmp = new ArrayList<>();
			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			String activeResourcesProjectName = servoyModel.getActiveResourcesProject().getProject().getName();
			List<ServoyProject> activeProjects = Arrays.asList(servoyModel.getModulesOfActiveProject());

			ServoyProject[] servoyProjects = servoyModel.getServoyProjects();
			for (ServoyProject servoyProject : servoyProjects)
			{
				if (!activeProjects.contains(servoyProject) && servoyProject.getResourcesProject() != null &&
					servoyProject.getResourcesProject().getProject().getName().equals(activeResourcesProjectName))
				{
					tmp.add(servoyProject.getProject().getName());
				}
			}
			noneActiveSolutionPage = new ListSelectionPage("noneactivesolutions", "Choose the non-active solutions",
				"Select the solutions that you want to include in this WAR. Be aware that these solutions are not checked for builder markers!", tmp,
				exportModel.getNoneActiveSolutions(), false, "export_war_none_active_solutions");
			fileSelectionPage = new FileSelectionPage(exportModel);

			addPage(fileSelectionPage);
			addPage(noneActiveSolutionPage);
			addPage(pluginSelectionPage);
			addPage(beanSelectionPage);
			addPage(lafSelectionPage);
			addPage(driverSelectionPage);
			if (isNGExport)
			{
				addPage(componentsSelectionPage);
				addPage(servicesSelectionPage);
			}
			addPage(defaultAdminConfigurationPage);
			addPage(servoyPropertiesSelectionPage);
			addPage(servoyPropertiesConfigurationPage);
			addPage(licenseConfigurationPage);
			addPage(serversSelectionPage);
			addPage(userHomeSelectionPage);

			String[] serverNames = ApplicationServerRegistry.get().getServerManager().getServerNames(true, true, true, false);
			ArrayList<String> srvNames = new ArrayList<String>(Arrays.asList(serverNames));
			if (!srvNames.contains(IServer.REPOSITORY_SERVER))
			{
				srvNames.add(IServer.REPOSITORY_SERVER);
			}
			for (String serverName : srvNames)
			{
				ServerConfiguration serverConfiguration = exportModel.getServerConfiguration(serverName);
				ServerConfigurationPage configurationPage = new ServerConfigurationPage("serverconf:" + serverName, serverConfiguration,
					exportModel.getSelectedServerNames(), serverConfigurationPages, this);
				addPage(configurationPage);
				serverConfigurationPages.put(serverName, configurationPage);
			}
		}
	}

	@Override
	public boolean canFinish()
	{
		IWizardPage[] allPages = getPages();
		for (IWizardPage page : allPages)
		{
			if (page instanceof DeployConfigurationPage) continue;
			if (!page.canFlipToNextPage())
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page)
	{
		if (page.equals(fileSelectionPage) && !exportModel.isExportNoneActiveSolutions())
		{
			return super.getNextPage(noneActiveSolutionPage);
		}
		if (page.equals(componentsSelectionPage))
		{
			if (!exportModel.isReady())
			{
				try
				{
					getContainer().run(true, true, new IRunnableWithProgress()
					{
						public void run(IProgressMonitor monitor) throws InterruptedException
						{
							monitor.beginTask("Searching for used components and services", 100);
							while (!exportModel.isReady())
							{
								Thread.sleep(100);
								monitor.worked(1);
							}
							Display.getDefault().syncExec(new Runnable()
							{
								public void run()
								{
									setupComponentsPages();
								}
							});
							monitor.done();
						}
					});
				}
				catch (InvocationTargetException e)
				{
					ServoyLog.logError(e);
				}
				catch (InterruptedException e)
				{
					ServoyLog.logError(e);
				}
			}
			else
			{
				setupComponentsPages();
			}

		}
		return super.getNextPage(page);
	}

	private void setupComponentsPages()
	{
		if (exportModel.hasSearchError())
		{
			componentsSelectionPage.setMessage("There was a problem finding used components, please select them manually.", IMessageProvider.WARNING);
			servicesSelectionPage.setMessage("There was a problem finding used services, please select them manually.", IMessageProvider.WARNING);
		}
		componentsSelectionPage.setComponentsUsed(exportModel.getUsedComponents());
		servicesSelectionPage.setComponentsUsed(exportModel.getUsedServices());
	}

	public IWizardPage getLastPage()
	{
		return userHomeSelectionPage;
	}

	@Override
	public void restoreDefaults()
	{
		if (getContainer().getCurrentPage() instanceof IRestoreDefaultPage)
		{
			((IRestoreDefaultPage)getContainer().getCurrentPage()).restoreDefaults();
		}
	}

	@Override
	public void copyWarToCommandLine()
	{
		noneActiveSolutionPage.storeInput();
		driverSelectionPage.storeInput();
		pluginSelectionPage.storeInput();
		beanSelectionPage.storeInput();
		lafSelectionPage.storeInput();
		serversSelectionPage.storeInput();

		StringBuilder sb = new StringBuilder("./war_export.");
		if (System.getProperty("os.name").toLowerCase().indexOf("win") > -1) sb.append("bat");
		else sb.append("sh");

		sb.append(" -s ").append(ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getSolution().getName());

		String warFilePath = !exportModel.getWarFileName().endsWith(".war") ? exportModel.getWarFileName() + ".war" : exportModel.getWarFileName();
		File warFile = new File(warFilePath);
		appendToBuilder(sb, " -o ", warFile.getParentFile() != null ? warFile.getParentFile().getPath() : " . ");

		sb.append(" -data ").append(ServoyModel.getWorkspace().getRoot().getLocation());
		appendToBuilder(sb, " -defaultAdminUser ", exportModel.getDefaultAdminUser());
		appendToBuilder(sb, " -defaultAdminPassword ", exportModel.getDefaultAdminPassword());
		appendToBuilder(sb, " -p ", exportModel.getServoyPropertiesFileName());
		appendToBuilder(sb, " -as ", exportModel.getServoyApplicationServerDir());
		appendToBuilder(sb, " -dbi ", exportModel.isExportUsingDbiFileInfoOnly());

		if (!exportModel.isExportActiveSolution()) appendToBuilder(sb, " -active ", exportModel.isExportActiveSolution());

		appendToBuilder(sb, " -b ", exportModel.getBeans(), beanSelectionPage.getCheckboxesNumber(), " ");
		appendToBuilder(sb, " -l ", exportModel.getLafs(), lafSelectionPage.getCheckboxesNumber(), " ");
		appendToBuilder(sb, " -d ", exportModel.getDrivers(), driverSelectionPage.getCheckboxesNumber(), " ");
		appendToBuilder(sb, " -pi ", exportModel.getPlugins(), pluginSelectionPage.getCheckboxesNumber(), " ");

		if (exportModel.isExportNoneActiveSolutions()) appendToBuilder(sb, " -nas ", exportModel.getNoneActiveSolutions(), -1, " ");

		if (exportModel.getPluginLocations().size() > 1 && !exportModel.getPluginLocations().get(0).equals("plugins/"))
			appendToBuilder(sb, " -pluginLocations ", exportModel.getPluginLocations(), pluginSelectionPage.getCheckboxesNumber(), " ");

		if (exportModel.getUsedComponents().size() == exportModel.getExportedComponents().size()) sb.append(" -crefs ");
		else if (componentsSelectionPage.getAvailableItems().size() != 0)
		{
			List<String> notUsedComponents = exportModel.getExportedComponents().stream().filter(
				comp -> !exportModel.getUsedComponents().contains(comp)).collect(Collectors.toList());
			appendToBuilder(sb, " -crefs ", notUsedComponents, -1, " ");
		}

		if (exportModel.getUsedServices().size() == exportModel.getExportedServices().size()) sb.append(" -srefs ");
		else if (servicesSelectionPage.getAvailableItems().size() != 0)
		{
			List<String> notUsedServices = exportModel.getExportedServices().stream().filter(svc -> !exportModel.getUsedServices().contains(svc)).collect(
				Collectors.toList());
			appendToBuilder(sb, " -srefs ", notUsedServices, -1, " ");
		}

		appendToBuilder(sb, " -md ", exportModel.isExportMetaData());
		appendToBuilder(sb, " -checkmd ", exportModel.isExportMetaData() && exportModel.isCheckMetadataTables());
		appendToBuilder(sb, " -sd ", exportModel.isExportSampleData());
		if (exportModel.isExportSampleData()) appendToBuilder(sb, " -sdcount ", String.valueOf(exportModel.getNumberOfSampleDataExported()));

		appendToBuilder(sb, " -i18n ", exportModel.isExportI18NData());
		appendToBuilder(sb, " -users ", exportModel.isExportUsers());
		appendToBuilder(sb, " -tables ", exportModel.isExportAllTablesFromReferencedServers());

		if (exportModel.getWarFileName() != null)
		{
			String solutionName = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getSolution().getName();
			if (!"".equals(warFile.getName()) && !warFile.getName().replace(".war", "").equals(solutionName))
				appendToBuilder(sb, " -warFileName ", warFile.getName());
		}

		appendToBuilder(sb, " -overwriteGroups ", exportModel.isOverwriteGroups());
		appendToBuilder(sb, " -allowSQLKeywords ", exportModel.isExportSampleData());
		appendToBuilder(sb, " -allowDataModelChanges ", exportModel.getAllowDataModelChanges());
		appendToBuilder(sb, " -skipDatabaseViewsUpdate ", exportModel.isSkipDatabaseViewsUpdate());
		appendToBuilder(sb, " -overrideSequenceTypes ", exportModel.isOverrideSequenceTypes());
		appendToBuilder(sb, " -overrideDefaultValues ", exportModel.isOverrideDefaultValues());
		appendToBuilder(sb, " -insertNewI18NKeysOnly ", exportModel.isInsertNewI18NKeysOnly());

		if (exportModel.getImportUserPolicy() != 1) appendToBuilder(sb, " -importUserPolicy ", String.valueOf(exportModel.getImportUserPolicy()));

		appendToBuilder(sb, " -addUsersToAdminGroup ", exportModel.isAddUsersToAdminGroup());
		appendToBuilder(sb, " -updateSequence ", exportModel.isUpdateSequences());
		appendToBuilder(sb, " -upgradeRepository ", exportModel.isAutomaticallyUpgradeRepository());

		if (exportModel.isCreateTomcatContextXML())
		{
			appendToBuilder(sb, " -antiResourceLocking ", exportModel.isAntiResourceLocking());
			appendToBuilder(sb, " -clearReferencesStatic ", exportModel.isClearReferencesStatic());
			appendToBuilder(sb, " -clearReferencesStopThreads ", exportModel.isClearReferencesStopThreads());
			appendToBuilder(sb, " -clearReferencesStopTimerThreads ", exportModel.isClearReferencesStopTimerThreads());
		}

		appendToBuilder(sb, " -useAsRealAdminUser ", exportModel.isUseAsRealAdminUser());
		appendToBuilder(sb, " -minimize ", exportModel.isMinimizeJsCssResources());

		if (exportModel.getLicenses() != null && exportModel.getLicenses().size() > 0)
		{
			int i = 1;
			for (License license : exportModel.getLicenses())
			{
				sb.append(" -license." + i + ".company_name " + license.getCompanyKey());
				sb.append(" -license." + i + ".code " + license.getCode());
				sb.append(" -license." + i + ".licenses " + license.getNumberOfLicenses());
				i++;
			}
		}

		if (exportModel.getUserHome() != null && !exportModel.getUserHome().equals("") && !exportModel.getUserHome().equals(System.getProperty("user.home")))
			appendToBuilder(sb, " -userHomeDirectory ", exportModel.getUserHome());

		appendToBuilder(sb, " -overwriteDBServerProperties ", exportModel.isOverwriteDeployedDBServerProperties());
		appendToBuilder(sb, " -overwriteAllProperties ", exportModel.isOverwriteDeployedServoyProperties());
		appendToBuilder(sb, " -log4jConfigurationFile ", exportModel.getLog4jConfigurationFile());
		appendToBuilder(sb, " -webXmlFileName ", exportModel.getWebXMLFileName());

		StringSelection selection = new StringSelection(sb.toString());
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, selection);
	}

	public void appendToBuilder(StringBuilder sb, String argument, String property)
	{
		if (property != null) sb.append(argument).append(property);
	}

	public void appendToBuilder(StringBuilder sb, String argument, boolean property)
	{
		if (property) appendToBuilder(sb, argument, "");
	}

	public void appendToBuilder(StringBuilder sb, String argument, Collection< ? > property, int totalItems, String separator)
	{

		if (property.size() > 0)
		{
			if (property.size() != totalItems)
			{
				sb.append(argument);
				property.forEach(prop -> sb.append(prop).append(separator));
				sb.replace(sb.length() - 1, sb.length(), "");
			}

		}
		else if (argument.equals(" -b ") || argument.equals(" -pi ") || argument.equals(" -l ") || argument.equals(" -d "))
			sb.append(argument).append("<none>");
	}

	@Override
	public void setContainer(IWizardContainer wizardContainer)
	{
		super.setContainer(wizardContainer);
		//when cancel is pressed on the wizard dialog - this method is called again setContainer(null) so
		//avoid second call to saveDirtyEditors()
		if (wizardContainer != null && EditorUtil.saveDirtyEditors(getShell(), true))
		{
			((WizardDialog)wizardContainer).close();
		}
	}

}
