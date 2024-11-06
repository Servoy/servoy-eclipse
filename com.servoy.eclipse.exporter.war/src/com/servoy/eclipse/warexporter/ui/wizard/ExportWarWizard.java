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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.BuilderUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseExportUserChannel;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableDefinitionUtils;
import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel.License;
import com.servoy.eclipse.model.war.exporter.ExportException;
import com.servoy.eclipse.model.war.exporter.ServerConfiguration;
import com.servoy.eclipse.model.war.exporter.WarExporter;
import com.servoy.eclipse.ui.wizards.DirtySaveExportWizard;
import com.servoy.eclipse.ui.wizards.ICopyWarToCommandLineWizard;
import com.servoy.eclipse.ui.wizards.IRestoreDefaultWizard;
import com.servoy.eclipse.ui.wizards.exportsolution.pages.ExportConfirmationPage;
import com.servoy.eclipse.warexporter.export.ExportWarModel;
import com.servoy.j2db.persistence.IServer;
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
public class ExportWarWizard extends DirtySaveExportWizard implements IExportWizard, IRestoreDefaultWizard, ICopyWarToCommandLineWizard
{

	private static final String DEFAULT_VALIDATORS_JAR = "default_validators.jar";
	private static final String CONVERTERS_JAR = "converters.jar";

	private FileSelectionPage fileSelectionPage;

	private DirectorySelectionPage pluginSelectionPage;

	private ExportWarModel exportModel;

	private volatile boolean errorFlag;

	private ServersSelectionPage serversSelectionPage;

	private DirectorySelectionPage driverSelectionPage;

	private DefaultAdminConfigurationPage defaultAdminConfigurationPage;

	private ServoyPropertiesSelectionPage servoyPropertiesSelectionPage;

	private AbstractWebObjectSelectionPage componentsSelectionPage;

	private WizardPage errorPage;

	private ServicesSelectionPage servicesSelectionPage;

	private LicensePage licenseConfigurationPage;

	private DeployConfigurationPage userHomeSelectionPage;

	private ListSelectionPage nonActiveSolutionPage;

	private DatabaseImportPropertiesPage databaseImportProperties;

	private ExportConfirmationPage exportConfirmationPage;

	private boolean componentAndServicePagesWereInitialized = false;

	private boolean exportNonActiveSolutionsDialog;

	private boolean disableFinishButton;

	public ExportWarWizard()
	{
		setWindowTitle("War Export");
		setDialogSettings(ExportWarModel.getDialogSettings());
		setNeedsProgressMonitor(true);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		if (activeProject == null)
		{
			createErrorPage("No active Servoy solution project found", "No active Servoy solution project found",
				"Please activate a Servoy solution project before trying to export");
			return;
		}

		int solutionType = activeProject.getSolutionMetaData().getSolutionType();
		exportModel = new ExportWarModel(getDialogSettings());
		exportNonActiveSolutionsDialog = true;
		disableFinishButton = false;
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject() == null)
		{
			createErrorPage("No active Resource project found", "No active Resource project found",
				"Please activate a Resource project before trying to export");
		}
		else if (BuilderUtils.getMarkers(activeProject) == BuilderUtils.HAS_ERROR_MARKERS)
		{
			if (TableDefinitionUtils.hasDbDownErrorMarkersThatCouldBeIgnoredOnExport(exportModel.getModulesToExport()))
			{
				exportConfirmationPage = new ExportConfirmationPage();
			}
			else
			{
				createErrorPage("Solution with errors", "Solution with errors", "Cannot export solution with errors, please fix them first");
			}
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
	public boolean canFinish()
	{
		if (disableFinishButton)
		{
			return false;
		}
		else
		{
			return super.canFinish();
		}
	}

	@Override
	public boolean performFinish()
	{
		// check if finish is pressed on the first page, to ask if the file can be overwritten or not.
		if (getContainer().getCurrentPage() instanceof FileSelectionPage && getContainer().getCurrentPage().getNextPage() == null)
		{
			return false;
		}
		nonActiveSolutionPage.storeInput();
		driverSelectionPage.storeInput();
		pluginSelectionPage.storeInput();
		serversSelectionPage.storeInput();

		exportModel.waitForSearchJobToFinish();

		// check if we automatically export components for non active solutions.
		if (exportModel.isExportNonActiveSolutions() && exportNonActiveSolutionsDialog)
		{
			exportNonActiveSolutionsDialog = false;
			if (componentsSelectionPage.availableWebObjectsList.getItems().length > 0 || servicesSelectionPage.availableWebObjectsList.getItems().length > 0)
			{
				final boolean[] exportNonActiveSolutionsComponents = new boolean[] { true };
				Display.getDefault().syncExec(() -> {
					exportNonActiveSolutionsComponents[0] = MessageDialog.openQuestion(getShell(), "Non Active Solutions Components",
						"Should we export all the components for non active solutions or do you want to manually select them?\nPress 'Yes' -> we automatically export all the components which are available in the active solution.");
				});

				if (exportNonActiveSolutionsComponents[0])
				{
					// components
					for (String componentName : componentsSelectionPage.availableWebObjectsList.getItems())
					{
						componentsSelectionPage.selectedWebObjectsList.add(componentName);
					}
					String[] selectedComp = componentsSelectionPage.selectedWebObjectsList.getItems();
					Arrays.sort(selectedComp);
					componentsSelectionPage.selectedWebObjectsList.setItems(selectedComp);
					componentsSelectionPage.availableWebObjectsList.removeAll();

					// services
					for (String componentName : servicesSelectionPage.availableWebObjectsList.getItems())
					{
						servicesSelectionPage.selectedWebObjectsList.add(componentName);
					}
					String[] selectedServ = servicesSelectionPage.selectedWebObjectsList.getItems();
					Arrays.sort(selectedServ);
					servicesSelectionPage.selectedWebObjectsList.setItems(selectedServ);
					servicesSelectionPage.availableWebObjectsList.removeAll();
				}
				else
				{
					disableFinishButton = true;
					getContainer().showPage(componentsSelectionPage);
					return false;
				}
			}
		}

		storeInputForComponentAndServicePages();

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

		boolean alreadyAsked = getDialogSettings().getBoolean("export.no_validators_or_converters.question");
		if (!alreadyAsked)
		{
			boolean noConvertorsOrValidators = !exportModel.getPlugins().contains(CONVERTERS_JAR) ||
				!exportModel.getPlugins().contains(DEFAULT_VALIDATORS_JAR);
			final boolean[] exportWithoutConvOrVal = new boolean[] { true };
			if (noConvertorsOrValidators)
			{
				Display.getDefault().syncExec(() -> {
					exportWithoutConvOrVal[0] = MessageDialog.openQuestion(getShell(), "No Column Convertors or Validators are exported",
						"Are you sure you want to export without 'converters.jar' or 'default_validators.jar' from the plugins? Press 'No' to add them automatically to the current export.");
				});
			}
			if (!exportWithoutConvOrVal[0])
			{
				if (!exportModel.getPlugins().contains(CONVERTERS_JAR)) exportModel.getPlugins().add(CONVERTERS_JAR);
				if (!exportModel.getPlugins().contains(DEFAULT_VALIDATORS_JAR)) exportModel.getPlugins().add(DEFAULT_VALIDATORS_JAR);
			}
			else getDialogSettings().put("export.no_validators_or_converters.question", true);
		}

		exportModel.saveSettings(getDialogSettings());
		errorFlag = false;
		IRunnableWithProgress job = new IRunnableWithProgress()
		{
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				try
				{
					final WarExporter exporter = new WarExporter(exportModel, new EclipseExportUserChannel(exportModel, monitor));
					final boolean[] cancel = { false };
					monitor.setTaskName("Scanning for plugins in the developer directory");
					String missingJarName = exporter.searchExportedPlugins();
					while (missingJarName != null)
					{
						String jarName = missingJarName;
						Display.getDefault().syncExec(new Runnable()
						{
							public void run()
							{
								MessageDialog.openWarning(getShell(), "Warning",
									"Please select the directory where " + jarName + " is located (usually your servoy developer/plugins directory)");
								DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.OPEN);
								String chosenDirName = dialog.open();
								if (chosenDirName != null)
								{
									exportModel.addPluginLocations(chosenDirName);
								}
								else
								{
									cancel[0] = true;
									return;
								}
								exportModel.saveSettings(getDialogSettings());
							}
						});
						if (!cancel[0]) missingJarName = exporter.searchExportedPlugins();
						else missingJarName = null;
					}
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
			getContainer().run(true, true, job);
		}
		catch (Exception e)
		{
			Debug.error(e);
			return false;
		}
		return !errorFlag;
	}

	private void storeInputForComponentAndServicePages()
	{
		ensureComponentAndServicePagesAreInitialized();
		componentsSelectionPage.storeInput();
		servicesSelectionPage.storeInput();
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
	public void createPageControls(Composite pageContainer)
	{
		//CSSSWTConstants.CSS_ID_KEY
		pageContainer.getShell().setData("org.eclipse.e4.ui.css.id", "svydialog");
		super.createPageControls(pageContainer);
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
			userHomeSelectionPage = new DeployConfigurationPage("userhomepage", exportModel);
			servoyPropertiesSelectionPage = new ServoyPropertiesSelectionPage(exportModel);
			componentsSelectionPage = new ComponentsSelectionPage(exportModel, WebComponentSpecProvider.getSpecProviderState(), "componentspage",
				"Select components to export", "View the components used and select others which you want to export.");
			servicesSelectionPage = new ServicesSelectionPage(exportModel, WebServiceSpecProvider.getSpecProviderState(), "servicespage",
				"Select services to export", "View the services used and select others which you want to export.");
			defaultAdminConfigurationPage = new DefaultAdminConfigurationPage("defaultAdminPage", exportModel);
			driverSelectionPage = new DirectorySelectionPage("driverpage", "Choose the jdbc drivers to export",
				"Select the jdbc drivers that you want to use in the war (if the app server doesn't provide them)",
				ApplicationServerRegistry.get().getServerManager().getDriversDir(), exportModel.getDrivers(), new String[] { "hsqldb.jar" },
				getDialogSettings().get("export.drivers") == null, false, "export_war_drivers");
			pluginSelectionPage = new DirectorySelectionPage("pluginpage", "Choose the plugins to export", "Select the plugins that you want to use in the war",
				ApplicationServerRegistry.get().getPluginManager().getPluginsDir(), exportModel.getPlugins(), null,
				getDialogSettings().get("export.plugins") == null, true, "export_war_plugins");

			ArrayList<String> tmp = new ArrayList<>();
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			String activeResourcesProjectName = servoyModel.getActiveResourcesProject() != null ? servoyModel.getActiveResourcesProject().getProject().getName()
				: null;
			List<ServoyProject> activeProjects = Arrays.asList(servoyModel.getModulesOfActiveProject());

			ServoyProject[] servoyProjects = servoyModel.getServoyProjects();
			if (activeResourcesProjectName != null)
			{
				for (ServoyProject servoyProject : servoyProjects)
				{
					if (!activeProjects.contains(servoyProject) && servoyProject.getResourcesProject() != null &&
						servoyProject.getResourcesProject().getProject().getName().equals(activeResourcesProjectName))
					{
						tmp.add(servoyProject.getProject().getName());
					}
				}
			}
			nonActiveSolutionPage = new ListSelectionPage("noneactivesolutions", "Choose the non-active solutions",
				"Select the solutions that you want to include in this WAR. Be aware that these solutions are not checked for builder markers!", tmp,
				exportModel.getNonActiveSolutions(), false, "export_war_none_active_solutions");
			fileSelectionPage = new FileSelectionPage(exportModel);
			databaseImportProperties = new DatabaseImportPropertiesPage(exportModel);

			if (exportConfirmationPage != null) addPage(exportConfirmationPage);
			addPage(fileSelectionPage);
			addPage(databaseImportProperties);
			addPage(nonActiveSolutionPage);
			addPage(userHomeSelectionPage);
			addPage(pluginSelectionPage);
			addPage(driverSelectionPage);
			addPage(componentsSelectionPage);
			addPage(servicesSelectionPage);
			addPage(defaultAdminConfigurationPage);
			addPage(servoyPropertiesSelectionPage);
			addPage(licenseConfigurationPage);
			addPage(serversSelectionPage);

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
	public IWizardPage getNextPage(IWizardPage page)
	{
		if (page.equals(fileSelectionPage) && !exportModel.isExportActiveSolution())
		{
			return !exportModel.isExportNonActiveSolutions() ? super.getNextPage(nonActiveSolutionPage) : super.getNextPage(databaseImportProperties);
		}
		if (page.equals(databaseImportProperties) && !exportModel.isExportNonActiveSolutions())
		{
			return super.getNextPage(nonActiveSolutionPage);
		}
		IWizardPage supersNextPage = super.getNextPage(page);
		if (componentsSelectionPage == supersNextPage)
		{
			ensureComponentAndServicePagesAreInitialized();
		}
		if (page.equals(componentsSelectionPage) && !exportNonActiveSolutionsDialog)
		{
			disableFinishButton = false;
		}
		return supersNextPage;
	}

	private void ensureComponentAndServicePagesAreInitialized()
	{
		if (!componentAndServicePagesWereInitialized)
		{
			componentAndServicePagesWereInitialized = true;

			// make sure solution explicitly used components/services are added to these pages
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
									setupComponentAndServicePages();
								}
							});
							monitor.done();
						}
					});
					exportModel.waitForSearchJobToFinish(); // just to make sure - as getContainer().run javadoc do not guarantee that the call above is blocking
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
				setupComponentAndServicePages();
			}
		}
	}

	private void setupComponentAndServicePages()
	{
		if (exportModel.hasSearchError())
		{
			componentsSelectionPage.setMessage("There was a problem finding used components, please select them manually.", IMessageProvider.WARNING);
			servicesSelectionPage.setMessage("There was a problem finding used services, please select them manually.", IMessageProvider.WARNING);
		}
		componentsSelectionPage.initialize(exportModel.getComponentsUsedExplicitlyBySolution());
		servicesSelectionPage.initialize(exportModel.getServicesUsedExplicitlyBySolution());
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
	public String copyWarToCommandLine()
	{
		String extraInfoForTheUser = null;

		nonActiveSolutionPage.storeInput();
		driverSelectionPage.storeInput();
		pluginSelectionPage.storeInput();
		serversSelectionPage.storeInput();

		storeInputForComponentAndServicePages();

		StringBuilder sb = new StringBuilder(".\\war_export.");
		if (System.getProperty("os.name").toLowerCase().indexOf("win") > -1) sb.append("bat");
		else sb.append("sh");

		sb.append(" -data \"").append(ServoyModel.getWorkspace().getRoot().getLocation()); // quote it in case of spaces in workspace location

		// the rest of the arguments starting with -s are quoted completely due to SVY-15773: command line export failed due to -b <none> -l <none> if the quotes were not there
		sb.append("\" \"-s ").append(ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getSolution().getName());

		String warFilePath = !exportModel.getWarFileName().endsWith(".war") ? exportModel.getWarFileName() + ".war" : exportModel.getWarFileName();
		File warFile = new File(warFilePath);
		appendToBuilder(sb, " -o ", warFile.getParentFile() != null ? warFile.getParentFile().getPath() : ".");

		if (!"".equals(warFile.getName()) &&
			!(ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getSolution().getName() + ".war").equals(warFile.getName()))
		{
			appendToBuilder(sb, " -warFileName ", warFile.getName());
		}

		if (exportModel.getServoyPropertiesFileName() != null)
		{
			appendToBuilder(sb, " -pfw ", exportModel.getServoyPropertiesFileName());
		}
		else
		{
			// as command line doesn't support all options that are available in UI if a properties file is not set, for command line
			// we should generate a properties file and offer to save it together with the copied-to-clipboard command line
			FileDialog fileSaveDialog = new FileDialog(getShell(), SWT.SAVE);
			fileSaveDialog.setFileName("servoy.properties");
			fileSaveDialog.setOverwrite(true);
			fileSaveDialog.setFilterExtensions(new String[] { "*.properties" });
			fileSaveDialog.setFilterIndex(0);
			fileSaveDialog.setFilterNames(new String[] { "Servoy properties file (*.properties)" });

			String pathOfFileToSave = exportModel.getGenerateExportCommandLinePropertiesFileSavePath();
			if (pathOfFileToSave != null)
			{
				File previouslyUsedSaveLocation = new File(pathOfFileToSave);
				if (previouslyUsedSaveLocation.exists()) fileSaveDialog.setFilterPath(previouslyUsedSaveLocation.getAbsolutePath());
			}

			extraInfoForTheUser = "Please point (using the -pfw arg) to the location of the previously saved servoy.properties file when executing this command.";

			fileSaveDialog.setText(
				"Please choose a location where the generated servoy.properties file will be saved");
			pathOfFileToSave = fileSaveDialog.open();
			if (pathOfFileToSave != null)
			{
				try
				{
					exportModel.generatePropertiesFileContent(new File(pathOfFileToSave));
				}
				catch (IOException e)
				{
					ServoyLog.logError("Cannot save to disk the servoy.properties file that is to be used together with the command line copied to clipboard.",
						e);
				}
			}
			else pathOfFileToSave = "servoy.properties";

			appendToBuilder(sb, " -pfw ", pathOfFileToSave);
		}

		appendToBuilder(sb, " -as ", exportModel.getServoyApplicationServerDir());

		appendToBuilder(sb, " -defaultAdminUser ", exportModel.getDefaultAdminUser());
		appendToBuilder(sb, " -defaultAdminPassword ", exportModel.getDefaultAdminPassword());

		appendToBuilder(sb, " -dbi", exportModel.isExportUsingDbiFileInfoOnly());

		if (!exportModel.isExportActiveSolution()) appendToBuilder(sb, " -active ", exportModel.isExportActiveSolution());

		appendToBuilder(sb, " -d", exportModel.getDrivers(), driverSelectionPage.getCheckboxesNumber());
		appendToBuilder(sb, " -pi", exportModel.getPlugins(), pluginSelectionPage.getCheckboxesNumber());

		if (exportModel.isExportNonActiveSolutions()) appendToBuilder(sb, " -nas", exportModel.getNonActiveSolutions(), -1);

		if (exportModel.getPluginLocations().size() > 1 && !exportModel.getPluginLocations().get(0).equals("plugins/"))
			appendToBuilder(sb, " -pluginLocations", exportModel.getPluginLocations(), pluginSelectionPage.getCheckboxesNumber());

		// see how components are to be exported
		if (exportModel.getComponentsNeededUnderTheHood().size() + exportModel.getComponentsUsedExplicitlyBySolution().size() == exportModel
			.getAllExportedComponents().size()) sb.append(" -crefs"); // so nothing extra selected by user; and the user cannot select anything less then this
		else if (componentsSelectionPage.checkThatAllPickableArePresentIn(exportModel.getAllExportedComponents()))
		{
			// all is exported
			sb.append(" -crefs all");
		}
		else
		{
			// not all is exported and not just defaults, so specify what are the extra/optional components to be exported
			List<String> userPickedComponents = exportModel.getComponentsToExportWithoutUnderTheHoodOnes().stream().filter(
				comp -> !exportModel.getComponentsUsedExplicitlyBySolution().contains(comp)).collect(Collectors.toList());
			appendToBuilder(sb, " -crefs", userPickedComponents, -1);
		}

		// see how services are to be exported
		if (exportModel.getServicesNeededUnderTheHoodWithoutSabloServices().size() + exportModel.getServicesUsedExplicitlyBySolution().size() == exportModel
			.getAllExportedServicesWithoutSabloServices().size()) sb.append(" -srefs"); // so nothing extra selected by user; and the user cannot select anything less then this
		else if (servicesSelectionPage.checkThatAllPickableArePresentIn(exportModel.getAllExportedServicesWithoutSabloServices()))
		{
			// all is exported
			sb.append(" -srefs all");
		}
		else
		{
			// not all is exported and not just defaults, so specify what are the extra/optional services to be exported
			List<String> userPickedServices = exportModel.getServicesToExportWithoutUnderTheHoodOnes().stream().filter(
				svc -> !exportModel.getServicesUsedExplicitlyBySolution().contains(svc)).collect(Collectors.toList());
			appendToBuilder(sb, " -srefs", userPickedServices, -1);
		}

		appendToBuilder(sb, " -md", exportModel.isExportMetaData());
		appendToBuilder(sb, " -checkmd", exportModel.isExportMetaData() && exportModel.isCheckMetadataTables());
		appendToBuilder(sb, " -sd", exportModel.isExportSampleData());
		if (exportModel.isExportSampleData()) appendToBuilder(sb, " -sdcount ", String.valueOf(exportModel.getNumberOfSampleDataExported()));

		appendToBuilder(sb, " -i18n", exportModel.isExportI18NData());
		appendToBuilder(sb, " -users", exportModel.isExportUsers());
		appendToBuilder(sb, " -tables", exportModel.isExportAllTablesFromReferencedServers());

		appendToBuilder(sb, " -overwriteGroups", exportModel.isOverwriteGroups());
		appendToBuilder(sb, " -allowSQLKeywords", exportModel.isExportSampleData());
		appendToBuilder(sb, " -allowDataModelChanges ", exportModel.getAllowDataModelChanges());
		appendToBuilder(sb, " -skipDatabaseViewsUpdate", exportModel.isSkipDatabaseViewsUpdate());
		appendToBuilder(sb, " -overrideSequenceTypes", exportModel.isOverrideSequenceTypes());
		appendToBuilder(sb, " -overrideDefaultValues", exportModel.isOverrideDefaultValues());
		appendToBuilder(sb, " -insertNewI18NKeysOnly", exportModel.isInsertNewI18NKeysOnly());

		if (exportModel.getImportUserPolicy() != 1) appendToBuilder(sb, " -importUserPolicy ", String.valueOf(exportModel.getImportUserPolicy()));

		appendToBuilder(sb, " -addUsersToAdminGroup", exportModel.isAddUsersToAdminGroup());
		appendToBuilder(sb, " -updateSequence", exportModel.isUpdateSequences());
		appendToBuilder(sb, " -upgradeRepository", exportModel.isAutomaticallyUpgradeRepository());

		if (exportModel.getTomcatContextXMLFileName() != null)
		{
			appendToBuilder(sb, " -contextFileName ", exportModel.getTomcatContextXMLFileName());
		}
		else if (exportModel.isCreateTomcatContextXML())
		{
			appendToBuilder(sb, " -antiResourceLocking", exportModel.isAntiResourceLocking());
			appendToBuilder(sb, " -clearReferencesStatic", exportModel.isClearReferencesStatic());
			appendToBuilder(sb, " -clearReferencesStopThreads", exportModel.isClearReferencesStopThreads());
			appendToBuilder(sb, " -clearReferencesStopTimerThreads", exportModel.isClearReferencesStopTimerThreads());
		}

		appendToBuilder(sb, " -useAsRealAdminUser", exportModel.isUseAsRealAdminUser());

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

		appendToBuilder(sb, " -doNotOverwriteDBServerProperties", !exportModel.isOverwriteDeployedDBServerProperties());
		appendToBuilder(sb, " -overwriteAllProperties", exportModel.isOverwriteDeployedServoyProperties());
		appendToBuilder(sb, " -log4jConfigurationFile ", exportModel.getLog4jConfigurationFile());
		appendToBuilder(sb, " -webXmlFileName ", exportModel.getWebXMLFileName());
		appendToBuilder(sb, " -ng2", exportModel.exportNG2Mode());
		sb.append("\"");

		StringSelection selection = new StringSelection(sb.toString());
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, selection);

		return extraInfoForTheUser;
	}

	public void appendToBuilder(StringBuilder sb, String argument, String property)
	{
		if (property != null) sb.append(argument).append(property);
	}

	public void appendToBuilder(StringBuilder sb, String argument, boolean property)
	{
		if (property) appendToBuilder(sb, argument, "");
	}

	public void appendToBuilder(StringBuilder sb, String argument, Collection< ? > property, int totalItems)
	{

		if (property.size() > 0)
		{
			if (property.size() != totalItems)
			{
				sb.append(argument);
				property.forEach(prop -> sb.append(" ").append(prop));
			}

		}
		else if (argument.equals(" -b") || argument.equals(" -pi") || argument.equals(" -l") || argument.equals(" -d")) sb.append(argument).append(" <none>");
	}

}
