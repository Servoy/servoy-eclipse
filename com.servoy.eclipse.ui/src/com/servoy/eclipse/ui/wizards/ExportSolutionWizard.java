/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.ui.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.json.JSONObject;
import org.sablo.specification.Package.DirPackageReader;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.BuilderUtils;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableDefinitionUtils;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.export.ExportSolutionJob;
import com.servoy.eclipse.ui.wizards.exportsolution.pages.DeployPage;
import com.servoy.eclipse.ui.wizards.exportsolution.pages.DeployProgressPage;
import com.servoy.eclipse.ui.wizards.exportsolution.pages.ExportConfirmationPage;
import com.servoy.eclipse.ui.wizards.exportsolution.pages.ExportOptionsPage;
import com.servoy.eclipse.ui.wizards.exportsolution.pages.FileSelectionPage;
import com.servoy.eclipse.ui.wizards.exportsolution.pages.ImportSettingsPage;
import com.servoy.eclipse.ui.wizards.exportsolution.pages.ModulesSelectionPage;
import com.servoy.eclipse.ui.wizards.exportsolution.pages.PasswordPage;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.SecuritySupport;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

public class ExportSolutionWizard extends DirtySaveExportWizard implements IExportWizard
{

	/**
	 *
	 */
	private static final String EXPORT_USING_DBI_FILE_INFO_ONLY = "exportUsingDbiFileInfoOnly";

	/**
	 *
	 */
	private static final String EXPORT_USERS = "exportUsers";

	/**
	 *
	 */
	private static final String EXPORTI18N_DATA = "exporti18nData";

	/**
	 *
	 */
	private static final String NUMBER_OF_SAMPLE_DATA_EXPORTED = "numberOfSampleDataExported";

	/**
	 *
	 */
	private static final String EXPORT_SAMPLE_DATA = "exportSampleData";

	/**
	 *
	 */
	private static final String CHECK_METADATA_TABLES = "checkMetadataTables";

	/**
	 *
	 */
	private static final String EXPORT_META_DATA = "exportMetaData";

	/**
	 *
	 */
	private static final String EXPORT_ALL_TABLES_FROM_REFERENCED_SERVERS = "exportAllTablesFromReferencedServers";

	/**
	 *
	 */
	private static final String EXPORT_REFERENCED_MODULES = "exportReferencedModules";
	private static final String EXPORTED_MODULES = "exportedModules";

	/**
	 *
	 */
	private static final String EXPORT_REFERENCED_WEB_PACKAGES = "exportReferencedWebPackages";

	/**
	 *
	 */
	private static final String PROTECT_WITH_PASSWORD = "protectWithPassword";

	/**
	 *
	 */
	private static final String INITIAL_FILE_NAME = "initialFileName";


	/**
	 *
	 */
	private static final String USE_IMPORT_SETTINGS = "useImportSettings";

	/**
	 *
	 */
	private static final String IMPORT_SETTINGS = "importSettings";

	/**
	 *
	 */
	private static final String DEPLOY_TO_APPLICATION_SERVER = "deployToApplicationServer";
	/**
	 *
	 */
	private static final String DEPLOY_URL = "deployURL";
	/**
	 *
	 */
	private static final String DEPLOY_USERNAME = "deployUsername";
	/**
	 *
	 */
	private static final String DEPLOY_PASSWORD = "deployPassword";

	/**
	 *
	 */
	private static final String SAVE_IMPORT_SETTINGS_TO_DISK = "saveImportSettingsToDisk";


	private Solution activeSolution;
	private ExportSolutionModel exportModel;
	private FileSelectionPage fileSelectionPage;
	private ExportOptionsPage exportOptionsPage;
	private ModulesSelectionPage modulesSelectionPage;
	private PasswordPage passwordPage;
	private ImportSettingsPage importPage;
	private DeployPage deployPage;
	private DeployProgressPage deployProgressPage;
	private ExportConfirmationPage exportConfirmationPage;

	private final IFileAccess workspace;

	private boolean activeSolutionDbDownErrors = false;

	private Font labelBoldFont;
	private FontDescriptor labelBoldFontDescriptor;

	private boolean deployToApplicationServer;
	private String deployURL;
	private String deployUsername;
	private String deployPassword;

	public ExportSolutionWizard()
	{
		super();
		setWindowTitle("Solution Export");
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("SolutionExportWizard");
		if (section == null)
		{
			section = workbenchSettings.addNewSection("SolutionExportWizard");
		}
		setDialogSettings(section);
		workspace = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
	}


	@Override
	public boolean performFinish()
	{
		if (exportModel.isExportReferencedWebPackages())
		{
			Map<String, List<String>> modulesNGPackageProjects = getModulesNGPackageProjects();
			if (modulesNGPackageProjects.size() > 0)
			{
				StringBuilder modulesNGPackageProjectsAsString = new StringBuilder();
				for (String module : modulesNGPackageProjects.keySet())
				{
					modulesNGPackageProjectsAsString.append("\n\n").append(module).append(": ");
					Iterator<String> modulesNGPackageProjectIte = modulesNGPackageProjects.get(module).iterator();
					while (modulesNGPackageProjectIte.hasNext())
					{
						modulesNGPackageProjectsAsString.append(modulesNGPackageProjectIte.next());
						if (modulesNGPackageProjectIte.hasNext()) modulesNGPackageProjectsAsString.append(", ");
					}
				}
				if (!MessageDialog.openQuestion(UIUtils.getActiveShell(), "Cannot export referenced package projects",
					"The following solutions have referenced package projects that cannot be exported:" + modulesNGPackageProjectsAsString +
						"\n\nDo you want to continue by skiping exporting the referenced package projects?"))
				{
					return false;
				}
			}
		}

		IDialogSettings dialogSettings = getDialogSettings();
		dialogSettings.put(INITIAL_FILE_NAME, exportModel.getFileName());
		dialogSettings.put(PROTECT_WITH_PASSWORD, exportModel.isProtectWithPassword());
		dialogSettings.put(EXPORT_REFERENCED_MODULES, exportModel.isExportReferencedModules());
		dialogSettings.put(EXPORT_REFERENCED_WEB_PACKAGES, exportModel.isExportReferencedWebPackages());
		if (exportModel.isExportReferencedModules())
		{
			dialogSettings.put(EXPORTED_MODULES,
				exportModel.getModulesToExport() == null ? null : Arrays.stream(exportModel.getModulesToExport()).collect(Collectors.joining(",")));
		}
		dialogSettings.put(EXPORT_ALL_TABLES_FROM_REFERENCED_SERVERS, exportModel.isExportAllTablesFromReferencedServers());
		dialogSettings.put(EXPORT_META_DATA, exportModel.isExportMetaData());
		dialogSettings.put(CHECK_METADATA_TABLES, exportModel.isCheckMetadataTables());
		dialogSettings.put(EXPORT_SAMPLE_DATA, exportModel.isExportSampleData());
		dialogSettings.put(NUMBER_OF_SAMPLE_DATA_EXPORTED, exportModel.getNumberOfSampleDataExported());
		dialogSettings.put(EXPORTI18N_DATA, exportModel.isExportI18NData());
		dialogSettings.put(EXPORT_USERS, exportModel.isExportUsers());
		dialogSettings.put(EXPORT_USING_DBI_FILE_INFO_ONLY, exportModel.isExportUsingDbiFileInfoOnly());
		dialogSettings.put(USE_IMPORT_SETTINGS, exportModel.useImportSettings());
		JSONObject importSettings = exportModel.getImportSettings();
		if (importSettings != null)
		{
			dialogSettings.put(IMPORT_SETTINGS, importSettings.toString());
		}
		dialogSettings.put(DEPLOY_TO_APPLICATION_SERVER, deployToApplicationServer);
		dialogSettings.put(DEPLOY_URL, deployURL);
		dialogSettings.put(DEPLOY_USERNAME, deployUsername);
		try
		{
			dialogSettings.put(DEPLOY_PASSWORD, SecuritySupport.encrypt(deployPassword));
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		dialogSettings.put(SAVE_IMPORT_SETTINGS_TO_DISK, exportModel.isSaveImportSettingsToDisk());

		IWizardPage currentPage = this.getContainer().getCurrentPage();
		if (currentPage != deployProgressPage)
		{
			doExport(null);
		}

		return true;
	}

	public void doExport(IJobChangeListener jobChangeListener)
	{
		WorkspaceJob exportJob = new ExportSolutionJob("Exporting solution '" + activeSolution.getName() + "'", exportModel, activeSolution,
			modulesSelectionPage.hasDBDownErrors(), true, workspace);

		exportJob.setUser(true); // we want the progress to be visible in a dialog, not to stay in the status bar
		if (jobChangeListener != null) exportJob.addJobChangeListener(jobChangeListener);
		exportJob.schedule();
	}


	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		if (activeProject != null) activeSolution = activeProject.getSolution();

		exportModel = new ExportSolutionModel();
		IDialogSettings dialogSettings = getDialogSettings();
		String initialFileName = dialogSettings.get(INITIAL_FILE_NAME);
		if (initialFileName == null)
		{
			ServoyModelManager.getServoyModelManager().getServoyModel();
			String dir = Settings.getInstance().getProperty(J2DBGlobals.SERVOY_APPLICATION_SERVER_DIRECTORY_KEY);
			initialFileName = new File(dir, "solutions/" + activeSolution.getName() + ".servoy").getAbsolutePath();
		}
		else
		{
			// use the previous export directory, with the name of the active solution
			initialFileName = new File(new File(initialFileName).getParent(), activeSolution.getName() + ".servoy").getAbsolutePath();
		}
		exportModel.setFileName(initialFileName);

		exportModel.setProtectWithPassword(dialogSettings.getBoolean(PROTECT_WITH_PASSWORD));

		boolean exportModules = activeSolution.getModulesNames() != null;
		if (dialogSettings.get(EXPORT_REFERENCED_MODULES) != null)
		{
			exportModules = exportModules && dialogSettings.getBoolean(EXPORT_REFERENCED_MODULES);
		}
		exportModel.setExportReferencedModules(exportModules);
		exportModel
			.setModulesToExport(dialogSettings.get(EXPORTED_MODULES) != null ? dialogSettings.get(EXPORTED_MODULES).split(",") : null);

		exportModel.setExportReferencedWebPackages(dialogSettings.getBoolean(EXPORT_REFERENCED_WEB_PACKAGES));
		exportModel.setExportAllTablesFromReferencedServers(dialogSettings.getBoolean(EXPORT_ALL_TABLES_FROM_REFERENCED_SERVERS));
		exportModel.setExportMetaData(dialogSettings.getBoolean(EXPORT_META_DATA));
		exportModel.setCheckMetadataTables(dialogSettings.getBoolean(CHECK_METADATA_TABLES));
		exportModel.setExportSampleData(dialogSettings.getBoolean(EXPORT_SAMPLE_DATA));
		if (dialogSettings.get(NUMBER_OF_SAMPLE_DATA_EXPORTED) != null)
		{
			exportModel.setNumberOfSampleDataExported(dialogSettings.getInt(NUMBER_OF_SAMPLE_DATA_EXPORTED));
		}
		exportModel.setExportI18NData(dialogSettings.getBoolean(EXPORTI18N_DATA));
		exportModel.setExportUsers(dialogSettings.getBoolean(EXPORT_USERS));
		exportModel.setExportUsingDbiFileInfoOnly(dialogSettings.getBoolean(EXPORT_USING_DBI_FILE_INFO_ONLY));

		int hasErrs = BuilderUtils.getMarkers(
			new String[] { ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getProject().getName() });
		if (hasErrs == BuilderUtils.HAS_ERROR_MARKERS)
		{
			activeSolutionDbDownErrors = TableDefinitionUtils.hasDbDownErrorMarkersThatCouldBeIgnoredOnExport(
				new String[] { ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getProject().getName() });
		}
		else
		{
			activeSolutionDbDownErrors = false;
		}

		exportModel.setUseImportSettings(dialogSettings.getBoolean(USE_IMPORT_SETTINGS));
		String dlgImportSettings = dialogSettings.get(IMPORT_SETTINGS);
		exportModel.setImportSettings(dlgImportSettings == null ? new JSONObject() : new JSONObject(dlgImportSettings));

		deployToApplicationServer = dialogSettings.getBoolean(DEPLOY_TO_APPLICATION_SERVER);
		deployURL = dialogSettings.get(DEPLOY_URL);
		if (deployURL == null)
		{
			deployURL = "http://localhost:8080/servoy-admin/solutions/deploy";
		}
		deployUsername = dialogSettings.get(DEPLOY_USERNAME);
		if (deployUsername == null)
		{
			deployUsername = "";
		}
		try
		{
			deployPassword = SecuritySupport.decrypt(dialogSettings.get(DEPLOY_PASSWORD));
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		if (deployPassword == null)
		{
			deployPassword = "";
		}

		exportModel.setSaveImportSettingsToDisk(dialogSettings.getBoolean(SAVE_IMPORT_SETTINGS_TO_DISK));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.wizard.Wizard#setContainer(org.eclipse.jface.wizard.IWizardContainer)
	 */
	@Override
	public void setContainer(IWizardContainer wizardContainer)
	{
		super.setContainer(wizardContainer);
		// needed for setting modules to export in case of File -> Export
		if (wizardContainer instanceof WizardDialog)
		{
			((WizardDialog)wizardContainer).addPageChangedListener(new IPageChangedListener()
			{
				public void pageChanged(PageChangedEvent event)
				{
					if (event.getSelectedPage() == modulesSelectionPage)
					{
						modulesSelectionPage.handleEvent(null);
					}
					if (event.getSelectedPage() == passwordPage)
					{
						passwordPage.requestPasswordFieldFocus();
					}
				}
			});
		}
	}

	@Override
	public void addPages()
	{
		modulesSelectionPage = new ModulesSelectionPage(this);
		if (activeSolutionDbDownErrors && !exportModel.isExportUsingDbiFileInfoOnly())
		{
			exportConfirmationPage = new ExportConfirmationPage();
			addPage(exportConfirmationPage);
		}
		fileSelectionPage = new FileSelectionPage(this);
		addPage(fileSelectionPage);
		exportOptionsPage = new ExportOptionsPage(this);
		addPage(exportOptionsPage);
		addPage(modulesSelectionPage);
		passwordPage = new PasswordPage(this);
		addPage(passwordPage);
		importPage = new ImportSettingsPage(this);
		addPage(importPage);
		deployPage = new DeployPage(this);
		addPage(deployPage);
		deployProgressPage = new DeployProgressPage(this);
		addPage(deployProgressPage);
	}

	@Override
	public boolean canFinish()
	{
		IWizardPage currentPage = this.getContainer().getCurrentPage();
		if (currentPage == fileSelectionPage) return false;
		if (currentPage == exportConfirmationPage) return false;
		if (exportModel.isExportReferencedModules() && currentPage == exportOptionsPage)
		{
			modulesSelectionPage.handleEvent(null);
			if (modulesSelectionPage.projectProblemsType == BuilderUtils.HAS_ERROR_MARKERS && !modulesSelectionPage.hasDBDownErrors()) return false;
		}
		if (currentPage == modulesSelectionPage)
		{
			if (modulesSelectionPage.projectProblemsType == BuilderUtils.HAS_ERROR_MARKERS && !modulesSelectionPage.hasDBDownErrors()) return false;
		}
		if (exportModel.useImportSettings() && (currentPage != importPage) && (currentPage != deployProgressPage)) return false;
		if (currentPage == importPage && deployToApplicationServer) return false;
		if (Utils.stringIsEmpty(getActiveSolution().getVersion()) ||
			exportModel.isExportReferencedModules() && !modulesSelectionPage.solutionVersionsPresent)
			return false;
		return exportModel.canFinish();
	}


	public Font getBoldFont(Label label)
	{
		if (labelBoldFont == null)
		{
			labelBoldFontDescriptor = FontDescriptor.createFrom(label.getFont()).setStyle(SWT.BOLD);
			labelBoldFont = labelBoldFontDescriptor.createFont(label.getDisplay());
		}
		return labelBoldFont;
	}

	public ExportSolutionModel getModel()
	{
		return exportModel;
	}

	public String getDeployURL()
	{
		return deployURL;
	}

	public void setDeployURL(String deployURL)
	{
		this.deployURL = deployURL;
	}

	public String getDeployUsername()
	{
		return deployUsername;
	}

	public void setDeployUsername(String deployUsername)
	{
		this.deployUsername = deployUsername;
	}

	public String getDeployPassword()
	{
		return deployPassword;
	}

	public void setDeployPassword(String deployPassword)
	{
		this.deployPassword = deployPassword;
	}

	public Solution getActiveSolution()
	{
		return activeSolution;
	}

	public boolean hasActiveSolutionDbDownErrors()
	{
		return activeSolutionDbDownErrors;
	}

	public boolean isDeployToApplicationServer()
	{
		return deployToApplicationServer;
	}

	public void setDeployToApplicationServer(boolean deployToApplicationServer)
	{
		this.deployToApplicationServer = deployToApplicationServer;
	}

	public ImportSettingsPage getImportPage()
	{
		return importPage;
	}

	public PasswordPage getPasswordPage()
	{
		return passwordPage;
	}

	public ExportOptionsPage getExportOptionsPage()
	{
		return exportOptionsPage;
	}

	public DeployPage getDeployPage()
	{
		return deployPage;
	}

	public DeployProgressPage getDeployProgressPage()
	{
		return deployProgressPage;
	}

	public ModulesSelectionPage getModulesSelectionPage()
	{
		return modulesSelectionPage;
	}

	@Override
	public void dispose()
	{
		if (labelBoldFont != null)
		{
			labelBoldFontDescriptor.destroyFont(labelBoldFont);
			labelBoldFontDescriptor = null;
			labelBoldFont = null;
		}
		super.dispose();
	}

	private Map<String, List<String>> getModulesNGPackageProjects()
	{
		Map<String, List<String>> modulesNGPackageProjects = new TreeMap<String, List<String>>();

		ArrayList<String> allModules = new ArrayList<String>();
		allModules.add(activeSolution.getName());
		if (exportModel.isExportReferencedModules() && exportModel.getModulesToExport() != null && exportModel.getModulesToExport().length > 0)
		{
			allModules.addAll(Arrays.asList(exportModel.getModulesToExport()));
		}

		for (String module : allModules)
		{
			ServoyProject moduleProject = ServoyModelFinder.getServoyModel().getServoyProject(module);
			if (moduleProject != null)
			{
				ArrayList<String> webPackages = new ArrayList<String>();
				for (ServoyNGPackageProject ngPackageProject : moduleProject.getNGPackageProjects())
				{
					DirPackageReader dirPackageReader = new DirPackageReader(ngPackageProject.getProject().getLocation().toFile());
					webPackages.add(dirPackageReader.getPackageDisplayname());
				}
				if (webPackages.size() > 0)
				{
					modulesNGPackageProjects.put(module, webPackages);
				}
			}
		}

		return modulesNGPackageProjects;
	}
}
