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
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.BuilderUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableDefinitionUtils;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.export.ExportSolutionJob;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.dataprocessing.IDataServerInternal;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SecuritySupport;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

public class ExportSolutionWizard extends Wizard implements IExportWizard
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

	/**
	 *
	 */
	private static final String PROTECT_WITH_PASSWORD = "protectWithPassword";

	/**
	 *
	 */
	private static final String INITIAL_FILE_NAME = "initialFileName";

	private static final String DB_DOWN_WARNING = "Error markers will be ignored because the DB seems to be offline (.dbi files will be used instead).";

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

	private Solution activeSolution;
	private ExportSolutionModel exportModel;
	private FileSelectionPage fileSelectionPage;
	private ExportOptionsPage exportOptionsPage;
	private ModulesSelectionPage modulesSelectionPage;
	private PasswordPage passwordPage;
	private ImportSettingsPage importPage;
	private DeployPage deployPage;

	private final IFileAccess workspace;

	private boolean activeSolutionDbDownErrors = false;

	private Font labelBoldFont;

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
		EditorUtil.saveDirtyEditors(getShell(), true);
		IDialogSettings dialogSettings = getDialogSettings();
		dialogSettings.put(INITIAL_FILE_NAME, exportModel.getFileName());
		dialogSettings.put(PROTECT_WITH_PASSWORD, exportModel.isProtectWithPassword());
		dialogSettings.put(EXPORT_REFERENCED_MODULES, exportModel.isExportReferencedModules());
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
			dialogSettings.put(DEPLOY_PASSWORD, SecuritySupport.encrypt(Settings.getInstance(), deployPassword));
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}


		IWizardPage currentPage = this.getContainer().getCurrentPage();
		if (currentPage != deployPage)
		{
			doExport(null);
		}

		return true;
	}

	private void doExport(IJobChangeListener jobChangeListener)
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
			String dir = ServoyModel.getSettings().getProperty(J2DBGlobals.SERVOY_APPLICATION_SERVER_DIRECTORY_KEY);
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
			activeSolutionDbDownErrors = TableDefinitionUtils.hasDbDownErrorMarkers(
				new String[] { ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getProject().getName() });
		}
		else
		{
			activeSolutionDbDownErrors = false;
		}

		exportModel.setUseImportSettings(dialogSettings.getBoolean(USE_IMPORT_SETTINGS));
		String dlgImportSettings = dialogSettings.get(IMPORT_SETTINGS);
		exportModel.setImportSettings(dlgImportSettings == null ? null : new JSONObject(dlgImportSettings));

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
			deployPassword = SecuritySupport.decrypt(Settings.getInstance(), dialogSettings.get(DEPLOY_PASSWORD));
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		if (deployPassword == null)
		{
			deployPassword = "";
		}
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
						modulesSelectionPage.checkStateChanged(null);
					}
					if (event.getSelectedPage() == passwordPage)
					{
						passwordPage.passwordText.setFocus();
					}
				}
			});
		}

	}

	private ExportConfirmationPage exportConfirmationPage;

	@Override
	public void addPages()
	{
		modulesSelectionPage = new ModulesSelectionPage();
		if (activeSolutionDbDownErrors)
		{
			exportConfirmationPage = new ExportConfirmationPage();
			addPage(exportConfirmationPage);
		}
		fileSelectionPage = new FileSelectionPage();
		addPage(fileSelectionPage);
		exportOptionsPage = new ExportOptionsPage();
		addPage(exportOptionsPage);
		addPage(modulesSelectionPage);
		passwordPage = new PasswordPage();
		addPage(passwordPage);
		importPage = new ImportSettingsPage();
		addPage(importPage);
		deployPage = new DeployPage();
		addPage(deployPage);
	}

	@Override
	public boolean canFinish()
	{
		IWizardPage currentPage = this.getContainer().getCurrentPage();
		if (currentPage == fileSelectionPage) return false;
		if (currentPage == exportConfirmationPage) return false;
		if (exportModel.isExportReferencedModules() && currentPage == exportOptionsPage)
		{
			modulesSelectionPage.checkStateChanged(null);
			if (modulesSelectionPage.projectProblemsType == BuilderUtils.HAS_ERROR_MARKERS && !modulesSelectionPage.hasDBDownErrors()) return false;
		}
		if (currentPage == modulesSelectionPage)
		{
			if (modulesSelectionPage.projectProblemsType == BuilderUtils.HAS_ERROR_MARKERS && !modulesSelectionPage.hasDBDownErrors()) return false;
		}
		if (exportModel.useImportSettings() && (currentPage != importPage) && (currentPage != deployPage)) return false;
		if (currentPage == importPage && deployToApplicationServer) return false;
		return exportModel.canFinish();
	}

	private class ExportConfirmationPage extends WizardPage
	{
		private Text message;

		public ExportConfirmationPage()
		{
			super("export confirmation page");
			setTitle("Export solution confirmation");
			setErrorMessage("One or more databases used in the solution are unreacheable");
		}

		public void createControl(Composite parent)
		{
			Composite container = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			container.setLayout(layout);
			layout.numColumns = 1;

			message = new Text(container, SWT.WRAP | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
			GridData gridData = new GridData();
			gridData.horizontalAlignment = GridData.FILL;
			gridData.verticalAlignment = GridData.FILL;
			gridData.grabExcessHorizontalSpace = true;
			gridData.grabExcessVerticalSpace = true;
			gridData.horizontalSpan = 1;
			message.setLayoutData(gridData);
			message.setEditable(false);
			message.setText("Are you sure you want to proceed with the export?  \nYou can continue to export based on dbi(database information) files only.");

			setControl(container);
			setPageComplete(true);
		}

		@Override
		public IWizardPage getPreviousPage()
		{
			return null;
		}
	}

	private class FileSelectionPage extends WizardPage implements Listener
	{

		private Text fileNameText;
		private Button browseButton;
		private int projectProblemsType;

		public FileSelectionPage()
		{
			super("page1");
			setTitle("Choose the destination file");
			setDescription("Select the file where you want your solution exported to");
			projectProblemsType = BuilderUtils.getMarkers(
				new String[] { ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getProject().getName() });
			if (projectProblemsType == BuilderUtils.HAS_ERROR_MARKERS)
			{
				if (modulesSelectionPage.hasDBDownErrors())
				{
					projectProblemsType = BuilderUtils.HAS_WARNING_MARKERS;
					setMessage(DB_DOWN_WARNING, IMessageProvider.WARNING);
				}
				else setErrorMessage("Errors in the solution will prevent it from functioning well. Please solve errors (problems view) first.");
			}
			else if (projectProblemsType == BuilderUtils.HAS_WARNING_MARKERS)
			{
				setMessage("Warnings in the solution may prevent it from functioning well. You may want to solve warnings (problems view) first.",
					IMessageProvider.WARNING);
			}
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
			if (exportModel.getFileName() != null) fileNameText.setText(exportModel.getFileName());

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
				exportModel.setFileName(potentialFileName);
			}
			else if (event.widget == browseButton)
			{
				Shell shell = new Shell();
				GridLayout gridLayout = new GridLayout();
				shell.setLayout(gridLayout);
				FileDialog dlg = new FileDialog(shell, SWT.SAVE);
				if (exportModel.getFileName() != null)
				{
					File f = new File(exportModel.getFileName());
					if (f.isDirectory())
					{
						dlg.setFilterPath(f.getAbsolutePath());
						dlg.setFileName(null);
					}
					else
					{
						dlg.setFilterPath(f.getParent());
						dlg.setFileName(f.getName());
						String[] extensions = { "*.servoy" };
						dlg.setFilterExtensions(extensions);
					}
				}
				String chosenFileName = dlg.open();
				if (chosenFileName != null)
				{
					exportModel.setFileName(chosenFileName);
					fileNameText.setText(chosenFileName);
				}
			}
			getWizard().getContainer().updateButtons();
			getWizard().getContainer().updateMessage();
		}

		@Override
		public boolean canFlipToNextPage()
		{
			boolean result = true;
			if (projectProblemsType == BuilderUtils.HAS_ERROR_MARKERS && !activeSolutionDbDownErrors) return false;

			boolean messageSet = (projectProblemsType == BuilderUtils.HAS_WARNING_MARKERS);
			if (exportModel.getFileName() == null) return false;
			if (fileNameText.getText().length() == 0)
			{
				result = false;
			}
			else
			{
				File f = new File(exportModel.getFileName());
				if (f.exists())
				{
					if (f.isDirectory())
					{
						if (!messageSet) setMessage("Specified path points to an existing folder.", IMessageProvider.WARNING);
						result = false;
						messageSet = true;
					}
					else
					{
						if (!messageSet) setMessage("Specified path points to an existing file.", IMessageProvider.INFORMATION);
						messageSet = true;
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
			if (exportModel.getFileName() == null) return null;

			File f = new File(exportModel.getFileName());
			if (f.exists())
			{
				if (!f.getAbsolutePath().equals(exportModel.getUserAcknowledgedFileToOverwrite()))
				{
					MessageBox msg = new MessageBox(this.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
					msg.setText("File already exists");
					msg.setMessage("The file you selected already exists on disk. Do you want to overwrite it?");
					if (msg.open() == SWT.YES)
					{
						exportModel.setUserAcknowledgedFileToOverwrite(f.getAbsolutePath());
						return exportOptionsPage;
					}
					else
					{
						return null;
					}
				}
				// User already acknowledged overwriting this file, and the file name was not
				// changed in the meantime.
				else
				{
					return exportOptionsPage;
				}
			}
			else
			{
				return exportOptionsPage;
			}
		}

	}

	private class ExportOptionsPage extends WizardPage implements Listener
	{
		private Button protectWithPasswordButton;
		private Button exportReferencedModulesButton;
		private Button exportAllTablesFromReferencedServers;
		private Button exportSampleDataButton;
		private Button exportMetadataTablesButton;
		private Button checkMetadataTablesButton;
		private Button exportI18NDataButton;
		private Button exportUsersButton;
		private Spinner nrOfExportedSampleDataSpinner;
		private Button rowsPerTableRadioButton;
		private Button allRowsRadioButton;
		private Button exportUsingDbiFileInfoOnlyButton;
		private final int resourcesProjectProblemsType;
		private Button useImportSettingsButton;

		public ExportOptionsPage()
		{
			super("page2");
			setTitle("Choose export options");
			setDescription("Specify the options for your export");

			ServoyModel model = ServoyModelManager.getServoyModelManager().getServoyModel();
			resourcesProjectProblemsType = model.getActiveResourcesProject() != null
				? BuilderUtils.getMarkers(new String[] { model.getActiveResourcesProject().getProject().getName() }) : BuilderUtils.HAS_ERROR_MARKERS;

			updateMessages();
		}

		@Override
		public boolean canFlipToNextPage()
		{
			return (activeSolutionDbDownErrors || (exportUsingDbiFileInfoOnlyButton != null && exportUsingDbiFileInfoOnlyButton.getSelection()) ||
				resourcesProjectProblemsType == BuilderUtils.HAS_NO_MARKERS || resourcesProjectProblemsType == BuilderUtils.HAS_WARNING_MARKERS) &&
				super.canFlipToNextPage();
		}

		private void updateMessages()
		{
			setMessage(null);
			setErrorMessage(null);
			if (resourcesProjectProblemsType == BuilderUtils.HAS_ERROR_MARKERS && exportUsingDbiFileInfoOnlyButton != null &&
				exportUsingDbiFileInfoOnlyButton.getSelection())
			{
				// if this is selected ignore error markers (to allow exporting out-of-sync dbi files) // TODO limit marker check to all but dbi? (the same must happen in command line exporter then)
				setMessage("Error markers will be ignored in resources project to allow .dbi based export.", IMessageProvider.WARNING);
			}
			else
			{
				if (resourcesProjectProblemsType == BuilderUtils.HAS_ERROR_MARKERS)
				{
					if (modulesSelectionPage.hasDBDownErrors())
					{
						setMessage(DB_DOWN_WARNING, IMessageProvider.WARNING);
					}
					else setErrorMessage("Errors in the resources project will make the solution misbehave. Please solve errors (problems view) first.");
				}
				else if (resourcesProjectProblemsType == BuilderUtils.HAS_WARNING_MARKERS)
				{
					setMessage("Warnings in the resources project may make the solution misbehave. You may want to solve warnings (problems view) first.",
						IMessageProvider.WARNING);
				}
			}
			if (isCurrentPage()) getWizard().getContainer().updateButtons();
		}

		public void createControl(Composite parent)
		{
			GridLayout gridLayout = new GridLayout();
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(gridLayout);

			protectWithPasswordButton = new Button(composite, SWT.CHECK);
			protectWithPasswordButton.setText("Protect solution with password");
			protectWithPasswordButton.setSelection(exportModel.isProtectWithPassword());
			protectWithPasswordButton.addListener(SWT.Selection, this);

			exportReferencedModulesButton = new Button(composite, SWT.CHECK);
			exportReferencedModulesButton.setText("Export referenced modules");
			exportReferencedModulesButton.setSelection(exportModel.isExportReferencedModules());
			exportReferencedModulesButton.addListener(SWT.Selection, this);

			exportAllTablesFromReferencedServers = new Button(composite, SWT.CHECK);
			exportAllTablesFromReferencedServers.setText("Export all tables from referenced servers");
			exportAllTablesFromReferencedServers.setSelection(exportModel.isExportAllTablesFromReferencedServers());
			exportAllTablesFromReferencedServers.addListener(SWT.Selection, this);

			exportMetadataTablesButton = new Button(composite, SWT.CHECK);
			exportMetadataTablesButton.setText("Export metadata tables (from database)");
			exportMetadataTablesButton.setSelection(exportModel.isExportMetaData());
			exportMetadataTablesButton.addListener(SWT.Selection, this);

			checkMetadataTablesButton = new Button(composite, SWT.CHECK);
			checkMetadataTablesButton.setSelection(exportModel.isCheckMetadataTables());
			checkMetadataTablesButton.addListener(SWT.Selection, this);

			exportSampleDataButton = new Button(composite, SWT.CHECK);
			exportSampleDataButton.setSelection(exportModel.isExportSampleData());
			exportSampleDataButton.addListener(SWT.Selection, this);

			Composite horizontalComposite = new Composite(composite, SWT.None);
			GridLayout hcGridLayout = new GridLayout();
			hcGridLayout.numColumns = 4;
			hcGridLayout.marginHeight = 0;
			hcGridLayout.marginWidth = 0;
			horizontalComposite.setLayout(hcGridLayout);

			GridData data1 = new GridData();
			Button emptyCBButton = new Button(horizontalComposite, SWT.CHECK);
			emptyCBButton.setVisible(false);
			emptyCBButton.setLayoutData(data1);

			GridData data2 = new GridData();
			rowsPerTableRadioButton = new Button(horizontalComposite, SWT.RADIO);
			rowsPerTableRadioButton.setEnabled(false);
			rowsPerTableRadioButton.setLayoutData(data2);
			rowsPerTableRadioButton.addListener(SWT.Selection, this);
			rowsPerTableRadioButton.setEnabled(exportModel.isExportSampleData());
			rowsPerTableRadioButton.setSelection(
				exportModel.isExportSampleData() && exportModel.getNumberOfSampleDataExported() != IDataServerInternal.MAX_ROWS_TO_RETRIEVE);


			GridData data3 = new GridData();
			Label textLabel = new Label(horizontalComposite, SWT.NONE);
			textLabel.setText("Rows per table: ");
			textLabel.setLayoutData(data3);

			GridData data4 = new GridData();
			nrOfExportedSampleDataSpinner = new Spinner(horizontalComposite, SWT.BORDER);
			nrOfExportedSampleDataSpinner.setMinimum(1);
			nrOfExportedSampleDataSpinner.setMaximum(IDataServerInternal.MAX_ROWS_TO_RETRIEVE);
			nrOfExportedSampleDataSpinner.setSelection(10000);
			nrOfExportedSampleDataSpinner.setIncrement(100);
			nrOfExportedSampleDataSpinner.setEnabled(
				exportModel.isExportSampleData() && exportModel.getNumberOfSampleDataExported() != IDataServerInternal.MAX_ROWS_TO_RETRIEVE);

			nrOfExportedSampleDataSpinner.setLayoutData(data4);

			nrOfExportedSampleDataSpinner.addModifyListener(new ModifyListener()
			{

				public void modifyText(ModifyEvent e)
				{
					int maxRowToRetrieve = nrOfExportedSampleDataSpinner.getSelection();
					if (maxRowToRetrieve == 0)
					{
						maxRowToRetrieve = IDataServerInternal.MAX_ROWS_TO_RETRIEVE;
					}
					exportModel.setNumberOfSampleDataExported(maxRowToRetrieve);
				}
			});

			GridData data5 = new GridData();
			Button emptyCBButton2 = new Button(horizontalComposite, SWT.CHECK);
			emptyCBButton2.setVisible(false);
			emptyCBButton2.setLayoutData(data5);

			GridData data6 = new GridData();
			allRowsRadioButton = new Button(horizontalComposite, SWT.RADIO);
			allRowsRadioButton.setEnabled(exportModel.isExportSampleData());
			allRowsRadioButton.setSelection(
				exportModel.isExportSampleData() && exportModel.getNumberOfSampleDataExported() == IDataServerInternal.MAX_ROWS_TO_RETRIEVE);
			allRowsRadioButton.setLayoutData(data6);
			allRowsRadioButton.addListener(SWT.Selection, this);


			GridData data7 = new GridData();
			Label textLabel4 = new Label(horizontalComposite, SWT.NONE);
			textLabel4.setText("All rows.");
			textLabel4.setLayoutData(data7);

			exportI18NDataButton = new Button(composite, SWT.CHECK);
			exportI18NDataButton.setText("Export i18n data");
			exportI18NDataButton.setSelection(exportModel.isExportI18NData());
			exportI18NDataButton.addListener(SWT.Selection, this);

			exportUsersButton = new Button(composite, SWT.CHECK);
			exportUsersButton.setText("Export users");
			exportUsersButton.setSelection(exportModel.isExportUsers());
			exportUsersButton.addListener(SWT.Selection, this);

			exportUsingDbiFileInfoOnlyButton = new Button(composite, SWT.CHECK);
			exportUsingDbiFileInfoOnlyButton.setText("Export based on DBI files only");
			exportUsingDbiFileInfoOnlyButton.addListener(SWT.Selection, this);

			refreshDBIDownFlag(exportModel.isExportReferencedModules() && modulesSelectionPage.hasDBDownErrors());

			useImportSettingsButton = new Button(composite, SWT.CHECK);
			useImportSettingsButton.setText("Create import settings");
			useImportSettingsButton.setSelection(exportModel.useImportSettings());
			useImportSettingsButton.addListener(SWT.Selection, this);

			setControl(composite);
		}

		private void refreshDBIDownFlag(boolean dbiDown)
		{
			exportUsingDbiFileInfoOnlyButton.setEnabled(!dbiDown);
			exportUsingDbiFileInfoOnlyButton.setSelection(dbiDown ? true : exportModel.isExportUsingDbiFileInfoOnly());
			if (dbiDown)
			{
				exportUsingDbiFileInfoOnlyButton.setText("Export based on DBI files only (one or more used databases is unreacheable)");
			}
			else
			{
				exportUsingDbiFileInfoOnlyButton.setText("Export based on DBI files only");
			}

			exportSampleDataButton.setEnabled(!dbiDown);
			exportSampleDataButton.setSelection(dbiDown ? false : exportModel.isExportSampleData());
			if (dbiDown)
			{
				exportSampleDataButton.setText("Export solution sample data (one or more used databases is unreacheable)");
			}
			else
			{
				exportSampleDataButton.setText("Export solution sample data");
			}
			checkMetadataTablesButton.setEnabled(!dbiDown);
			checkMetadataTablesButton.setSelection(dbiDown ? false : exportModel.isCheckMetadataTables());
			if (dbiDown)
			{
				checkMetadataTablesButton.setText("Check metadata tables (one or more used databases is unreacheable)");
			}
			else
			{
				checkMetadataTablesButton.setText("Check metadata tables (compare workspace and database table)");
			}
		}

		public void handleEvent(Event event)
		{
			if (event.widget == protectWithPasswordButton) exportModel.setProtectWithPassword(protectWithPasswordButton.getSelection());
			else if (event.widget == useImportSettingsButton) exportModel.setUseImportSettings(useImportSettingsButton.getSelection());
			else if (event.widget == exportReferencedModulesButton)
			{
				exportModel.setExportReferencedModules(exportReferencedModulesButton.getSelection());
				refreshDBIDownFlag(exportModel.isExportReferencedModules() && modulesSelectionPage.hasDBDownErrors());
			}
			else if (event.widget == checkMetadataTablesButton) exportModel.setCheckMetadataTables(checkMetadataTablesButton.getSelection());
			else if (event.widget == exportMetadataTablesButton)
			{
				exportModel.setExportMetaData(exportMetadataTablesButton.getSelection());
				checkMetadataTablesButton.setEnabled(exportMetadataTablesButton.getSelection());
			}
			else if (event.widget == exportSampleDataButton)
			{
				exportModel.setExportSampleData(exportSampleDataButton.getSelection());
				exportModel.setNumberOfSampleDataExported(10000);

				nrOfExportedSampleDataSpinner.setEnabled(exportSampleDataButton.getSelection());
				allRowsRadioButton.setEnabled(exportSampleDataButton.getSelection());
				allRowsRadioButton.setSelection(false);
				rowsPerTableRadioButton.setEnabled(exportSampleDataButton.getSelection());
				rowsPerTableRadioButton.setSelection(exportSampleDataButton.getSelection());
			}
			else if (event.widget == allRowsRadioButton)
			{
				nrOfExportedSampleDataSpinner.setSelection(IDataServerInternal.MAX_ROWS_TO_RETRIEVE);
				nrOfExportedSampleDataSpinner.setEnabled(!allRowsRadioButton.getSelection());

				rowsPerTableRadioButton.setSelection(!allRowsRadioButton.getSelection());

				exportModel.setExportSampleData(exportSampleDataButton.getSelection());
				exportModel.setNumberOfSampleDataExported(IDataServerInternal.MAX_ROWS_TO_RETRIEVE);
			}
			else if (event.widget == rowsPerTableRadioButton)
			{
				allRowsRadioButton.setSelection(!rowsPerTableRadioButton.getSelection());
				nrOfExportedSampleDataSpinner.setSelection(10000);
			}
			else if (event.widget == exportI18NDataButton) exportModel.setExportI18NData(exportI18NDataButton.getSelection());
			else if (event.widget == exportUsersButton) exportModel.setExportUsers(exportUsersButton.getSelection());
			else if (event.widget == exportAllTablesFromReferencedServers)
				exportModel.setExportAllTablesFromReferencedServers(exportAllTablesFromReferencedServers.getSelection());
			else if (event.widget == exportUsingDbiFileInfoOnlyButton)
			{
				exportModel.setExportUsingDbiFileInfoOnly(exportUsingDbiFileInfoOnlyButton.getSelection());
				if (!modulesSelectionPage.hasDBDownErrors())
				{
					updateMessages();
				}
				if (exportUsingDbiFileInfoOnlyButton.getSelection())
				{
					exportMetadataTablesButton.setText("Export metadata tables(from workspace)");
				}
				else
				{
					exportMetadataTablesButton.setText("Export metadata tables(from database)");
				}
			}
			getWizard().getContainer().updateButtons();
		}

		@Override
		public IWizardPage getNextPage()
		{
			if (exportModel.isExportReferencedModules()) return modulesSelectionPage;
			else if (exportModel.isProtectWithPassword()) return passwordPage;
			else if (exportModel.useImportSettings()) return importPage;
			else return null;
		}
	}

	private class ModulesSelectionPage extends WizardPage implements ICheckStateListener, ICheckBoxView
	{
		private CheckboxTreeViewer treeViewer;
		public int projectProblemsType = BuilderUtils.HAS_NO_MARKERS;
		private boolean moduleDbDownErrors = false;
		private SelectAllButtonsBar selectAllButtons;

		protected ModulesSelectionPage()
		{
			super("page3");
			setTitle("Choose modules to export");
			setDescription("Select additional modules that you want to have exported too");
		}

		public void createControl(Composite parent)
		{
			GridLayout gridLayout = new GridLayout();
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(gridLayout);

			String[] moduleNames = null;
			try
			{
				Map<String, Solution> modules = new HashMap<String, Solution>();
				activeSolution.getReferencedModulesRecursive(modules);
				if (modules.containsKey(activeSolution.getName())) modules.remove(activeSolution.getName());
				moduleNames = modules.keySet().toArray(new String[modules.keySet().size()]);
			}
			catch (Exception e)
			{
				Debug.error("Failed to retrieve referenced modules for solution.", e);
			}
			Arrays.sort(moduleNames);
			final String[] moduleNamesFinal = moduleNames;

			treeViewer = new CheckboxTreeViewer(composite);
			gridLayout.numColumns = 2;
			GridData gridData = new GridData();
			gridData.horizontalAlignment = GridData.FILL;
			gridData.verticalAlignment = GridData.FILL;
			gridData.grabExcessVerticalSpace = true;
			gridData.grabExcessHorizontalSpace = true;
			gridData.horizontalSpan = 2;
			treeViewer.getTree().setLayoutData(gridData);
			treeViewer.setContentProvider(new ITreeContentProvider()
			{
				public Object[] getChildren(Object parentElement)
				{
					return null;
				}

				public Object getParent(Object element)
				{
					return null;
				}

				public boolean hasChildren(Object element)
				{
					return false;
				}

				public Object[] getElements(Object inputElement)
				{
					return moduleNamesFinal;
				}

				public void dispose()
				{
				}

				public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
				{
				}
			});
			treeViewer.setInput(moduleNames);
			treeViewer.setCheckedElements(moduleNames);
			selectAllButtons = new SelectAllButtonsBar(this, composite);
			if (treeViewer.getTree().getItemCount() == 0)
			{
				selectAllButtons.disableButtons();
			}
			else
			{
				selectAllButtons.enableAll();
			}
			treeViewer.addCheckStateListener(this);
			setControl(composite);
		}

		public void checkStateChanged(CheckStateChangedEvent event)
		{
			initializeModulesToExport();
			projectProblemsType = BuilderUtils.getMarkers(exportModel.getModulesToExport());
			if (projectProblemsType == BuilderUtils.HAS_ERROR_MARKERS)
			{
				moduleDbDownErrors = TableDefinitionUtils.hasDbDownErrorMarkers(exportModel.getModulesToExport());
			}
			else
			{
				moduleDbDownErrors = false;
			}

			setErrorMessage(null);
			setMessage(null);
			if (projectProblemsType == BuilderUtils.HAS_ERROR_MARKERS)
			{
				if (hasDBDownErrors())
				{
					projectProblemsType = BuilderUtils.HAS_WARNING_MARKERS;
					setMessage(DB_DOWN_WARNING, IMessageProvider.WARNING);
				}
				else setErrorMessage(
					"There are errors in the modules that will prevent the solution from functioning well. Please solve errors (problems view) first.");
			}
			else if (projectProblemsType == BuilderUtils.HAS_WARNING_MARKERS)
			{
				setMessage(
					"There are warnings in the modules that may prevent the solution from functioning well. You may want to solve warnings (problems view) first.",
					IMessageProvider.WARNING);
			}

			if (isCurrentPage()) getWizard().getContainer().updateButtons();

			exportOptionsPage.refreshDBIDownFlag(hasDBDownErrors());

			if (treeViewer.getCheckedElements().length == treeViewer.getTree().getItemCount() && treeViewer.getCheckedElements().length == 0)
			{
				selectAllButtons.disableButtons();
			}
			else if (treeViewer.getCheckedElements().length < treeViewer.getTree().getItemCount())
			{
				selectAllButtons.enableAll();
			}
			else
			{
				selectAllButtons.disableSelectAll();
			}
		}

		/**
		 * True if either ACTIVE solution or MODULES have db down error markers.
		 */
		public boolean hasDBDownErrors()
		{
			return activeSolutionDbDownErrors || moduleDbDownErrors;
		}

		@Override
		public IWizardPage getNextPage()
		{
			if (exportModel.isProtectWithPassword()) return passwordPage;
			else if (exportModel.useImportSettings()) return importPage;
			else return null;
		}

		protected void initializeModulesToExport()
		{
			Object[] currentSelection = treeViewer.getCheckedElements();
			if (currentSelection.length > 0)
			{
				String[] moduleNames = new String[currentSelection.length];
				for (int i = 0; i < currentSelection.length; i++)
					moduleNames[i] = ((String)currentSelection[i]);
				exportModel.setModulesToExport(moduleNames);
			}
			else exportModel.setModulesToExport(null);
		}

		@Override
		public boolean canFlipToNextPage()
		{
			return (projectProblemsType == BuilderUtils.HAS_NO_MARKERS || projectProblemsType == BuilderUtils.HAS_WARNING_MARKERS) && super.canFlipToNextPage();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.eclipse.ui.wizards.ICheckBoxView#selectAll()
		 */
		@Override
		public void selectAll()
		{
			treeViewer.setAllChecked(true);
		}


		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.eclipse.ui.wizards.ICheckBoxView#deselectAll()
		 */
		@Override
		public void deselectAll()
		{
			treeViewer.setAllChecked(false);
		}
	}

	private class PasswordPage extends WizardPage implements Listener
	{
		Text passwordText;

		public PasswordPage()
		{
			super("page4");
			setTitle("Choose a password");
			setDescription("Provide the password that will be used to protect the exported solution");
		}

		public void createControl(Composite parent)
		{
			GridLayout gridLayout = new GridLayout();
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(gridLayout);

			// On MacOS, SWT 3.5 does not send events to listeners on password fields.
			// See: http://www.eclipse.org/forums/index.php?t=msg&goto=508058&
			int style = SWT.BORDER;
			if (!Utils.isAppleMacOS()) style |= SWT.PASSWORD;
			passwordText = new Text(composite, style);
			if (Utils.isAppleMacOS()) passwordText.setEchoChar('\u2022');
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			passwordText.setLayoutData(gd);
			passwordText.addListener(SWT.KeyUp, this);

			setControl(composite);
		}

		public void handleEvent(Event event)
		{
			if (event.widget == passwordText) exportModel.setPassword(passwordText.getText());
			getWizard().getContainer().updateButtons();
		}

		@Override
		public IWizardPage getNextPage()
		{
			if (exportModel.useImportSettings()) return importPage;
			return null;
		}
	}

	private class ImportSettingsPage extends WizardPage implements Listener
	{
		private static final String ENTER_MAINTENANCE_MODE = "emm";
		private static final String ACTIVATE_NEW_RELEASE = "ac";
		private static final String COMPACT_BEFORE_INPUT = "cmpt";
		private static final String OVERWRITE_STYLE = "os";
		private static final String OVERWRITE_GROUP_SECURITY = "og";
		private static final String CLEAN_IMPORT = "clean";

		private static final String NEW_SOLUTION_NAME = "newname";

		private static final String OVERRIDE_SEQUENCES = "fs";
		private static final String UPDATE_SEQUENCES = "useq";
		private static final String OVERRIDE_DEFAUL_VALUES = "fd";

		private static final String ALLOW_RESERVED_SQL_KEYWORDS = "ak";
		private static final String ALLOW_DATA_MODEL_CHANGES = "dm";
		private static final String SKIP_VIEWS = "sv";
		private static final String DISPLAY_DATA_MODEL_CHANGES = "dmc";
		private static final String IMPORT_META_DATA = "md";
		private static final String IMPORT_SAMPLE_DATA = "sd";
		private static final String IMPORT_I18N_DATA = "id";
		private static final String INSERT_NEW_I18N_DATA = "io";

		private static final String USER_IMPORT = "up";

		private static final String ALLOW_ADMIN_USER = "aa";

		private JSONObject importSettings;

		public ImportSettingsPage()
		{
			super("page5");
			setTitle("Choose import settings");
			setDescription("Specify the settings for your import");

			importSettings = exportModel.getImportSettings();
			if (importSettings == null)
			{
				importSettings = new JSONObject();
			}
		}


		private Button createCheckbox(String label, String property, Composite parent)
		{
			GridData gd = new GridData();
			gd.horizontalSpan = 3;
			Button checkbox = new Button(parent, SWT.CHECK);
			checkbox.setLayoutData(gd);
			checkbox.setText(label);
			checkbox.setSelection(importSettings.optBoolean(property));
			checkbox.setData("importProperty", property);
			checkbox.addListener(SWT.Selection, this);
			return checkbox;
		}

		private Button createRadio(String label, String property, Composite parent, int value)
		{
			GridData gd = new GridData();
			gd.horizontalSpan = 3;
			Button radio = new Button(parent, SWT.RADIO);
			radio.setLayoutData(gd);
			radio.setText(label);
			radio.setSelection(importSettings.optInt(property) == value);
			radio.setData("importProperty", property);
			radio.setData("importPropertyValue", Integer.toString(value));
			radio.addListener(SWT.Selection, this);
			return radio;
		}

		private Label createNewLine(Composite parent)
		{
			GridData gd = new GridData();
			gd.horizontalSpan = 3;
			Label newLine = new Label(parent, SWT.NONE);
			newLine.setLayoutData(gd);
			return newLine;
		}

		private Label createHeader(String text, Composite parent)
		{
			GridData gd = new GridData();
			gd.horizontalSpan = 3;
			Label header = new Label(parent, SWT.NONE);
			header.setLayoutData(gd);
			header.setText(text);
			header.setFont(getBoldFont(header));
			return header;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
		 */
		@Override
		public void createControl(Composite parent)
		{
			ScrolledComposite myScrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
			myScrolledComposite.setExpandHorizontal(true);
			myScrolledComposite.setExpandVertical(true);

			GridLayout gridLayout = new GridLayout(3, false);

			Composite composite = new Composite(myScrolledComposite, SWT.NONE);
			composite.setLayout(gridLayout);
			myScrolledComposite.setContent(composite);

			createHeader("General options...", composite);

			createCheckbox("Enter maintenance mode", ENTER_MAINTENANCE_MODE, composite);
			createCheckbox("Activate new release of imported solution and modules", ACTIVATE_NEW_RELEASE, composite);
			createCheckbox("Compact all the solutions/modules first before import", COMPACT_BEFORE_INPUT, composite);
			createCheckbox("Overwrite repository styles with import version", OVERWRITE_STYLE, composite);
			createCheckbox("Overwrite repository group security settings with import version", OVERWRITE_GROUP_SECURITY, composite);


			GridData gd = new GridData();
			gd.horizontalSpan = 1;
			gd.horizontalAlignment = GridData.FILL;
			gd.verticalAlignment = GridData.FILL;
			createCheckbox("Clean import  -  New solution name", CLEAN_IMPORT, composite).setLayoutData(gd);

			gd = new GridData();
			gd.horizontalSpan = 1;
			gd.horizontalAlignment = GridData.FILL;
			gd.verticalAlignment = GridData.FILL;
			gd.widthHint = 250;

			final Text newSolutionNameField = new Text(composite, SWT.BORDER);
			newSolutionNameField.setLayoutData(gd);
			newSolutionNameField.setText(importSettings.optString(NEW_SOLUTION_NAME));
			newSolutionNameField.addModifyListener(new ModifyListener()
			{
				@Override
				public void modifyText(ModifyEvent e)
				{
					importSettings.put(NEW_SOLUTION_NAME, newSolutionNameField.getText());
					exportModel.setImportSettings(importSettings);
				}
			});

			gd = new GridData();
			gd.horizontalSpan = 1;
			gd.horizontalAlignment = GridData.FILL;
			gd.verticalAlignment = GridData.FILL;

			Label newSolutionWarningLabel = new Label(composite, SWT.NONE);
			newSolutionWarningLabel.setText("WARNING: Styles will be overwritten!");
			newSolutionWarningLabel.setLayoutData(gd);


			createNewLine(composite);

			createCheckbox("Override existing sequence type definitions (in repository) with the sequence types contained in the import file",
				OVERRIDE_SEQUENCES, composite);
			createCheckbox("Update sequences for all tables on all servers used by the imported solution and modules", UPDATE_SEQUENCES, composite);
			createCheckbox("Override existing default values (in repository) with the default values contained in the import file", OVERRIDE_DEFAUL_VALUES,
				composite);

			gd = new GridData();
			gd.horizontalSpan = 3;
			Label overrideDefaultWarningLabel = new Label(composite, SWT.NONE);
			overrideDefaultWarningLabel.setLayoutData(gd);
			overrideDefaultWarningLabel.setText(
				"WARNING: This may break other solutions using the same tables, or cause tables to use nonexistent dbidentity or dbsequence sequences or other database auto enter types!");

			createNewLine(composite);

			createCheckbox("Allow reserved SQL keywords as table or column names (will fail unless supported by the backend database)",
				ALLOW_RESERVED_SQL_KEYWORDS, composite);
			createCheckbox("Allow data model (database) changes", ALLOW_DATA_MODEL_CHANGES, composite);
			createCheckbox("Skip database views import", SKIP_VIEWS, composite);
			createCheckbox("Display data model (database) changes", DISPLAY_DATA_MODEL_CHANGES, composite);
			createCheckbox("Import solution meta data", IMPORT_META_DATA, composite);
			createCheckbox("Import solution sample data", IMPORT_SAMPLE_DATA, composite);
			createCheckbox("Import internationalization (i18n) data (inserts and updates)", IMPORT_I18N_DATA, composite);
			createCheckbox("Insert new internationalization (i18n) keys only(inserts only, no updates)", INSERT_NEW_I18N_DATA, composite);

			createNewLine(composite);

			createHeader("User import options...", composite);

			createRadio("Do not import users contained in import", USER_IMPORT, composite, 0);
			createRadio("Create nonexisting users and add existing users to groups specified in import", USER_IMPORT, composite, 1);
			createRadio("Overwrite existing users completely (USE WITH CARE)", USER_IMPORT, composite, 2);

			createCheckbox("Allow users to be added to the Administrators group", ALLOW_ADMIN_USER, composite);

			createNewLine(composite);

			createHeader("Other options...", composite);

			gd = new GridData();
			gd.horizontalSpan = 3;
			final Button deployButton = new Button(composite, SWT.CHECK);
			deployButton.setLayoutData(gd);
			deployButton.setText("Deploy to Servoy application server");
			deployButton.setSelection(deployToApplicationServer);
			deployButton.addListener(SWT.Selection, new Listener()
			{
				@Override
				public void handleEvent(Event event)
				{
					deployToApplicationServer = deployButton.getSelection();
					getWizard().getContainer().updateButtons();
				}
			});

			myScrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

			setControl(myScrolledComposite);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
		 */
		@Override
		public void handleEvent(Event event)
		{
			if (event.widget instanceof Button)
			{
				String importProperty = (String)event.widget.getData("importProperty");
				if (importProperty != null)
				{
					String importPropertyValue = (String)event.widget.getData("importPropertyValue");
					importSettings.put(importProperty,
						importPropertyValue != null ? importPropertyValue : Boolean.valueOf(((Button)event.widget).getSelection()));
					exportModel.setImportSettings(importSettings);
				}
			}
		}

		@Override
		public IWizardPage getNextPage()
		{
			if (deployToApplicationServer) return deployPage;
			return null;
		}
	}


	private class DeployPage extends WizardPage implements IJobChangeListener
	{
		private Button deploy;
		private Text deployOutput;

		public DeployPage()
		{
			super("page6");
			setTitle("Deploy");
			setDescription("Deploy to Servoy application server");
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
		 */
		@Override
		public void createControl(Composite parent)
		{
			GridLayout gridLayout = new GridLayout(2, false);
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(gridLayout);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

			Label lbl = new Label(composite, SWT.NONE);
			lbl.setText("Deploy URL");
			final Text deployURLTxt = new Text(composite, SWT.BORDER);
			deployURLTxt.setText(deployURL);
			deployURLTxt.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

			lbl = new Label(composite, SWT.NONE);
			lbl.setText("Username");
			final Text usernameTxt = new Text(composite, SWT.BORDER);
			usernameTxt.setText(deployUsername);
			usernameTxt.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

			lbl = new Label(composite, SWT.NONE);
			lbl.setText("Password");
			// On MacOS, SWT 3.5 does not send events to listeners on password fields.
			// See: http://www.eclipse.org/forums/index.php?t=msg&goto=508058&
			int style = SWT.BORDER;
			if (!Utils.isAppleMacOS()) style |= SWT.PASSWORD;
			final Text passwordTxt = new Text(composite, style);
			passwordTxt.setText(deployPassword);
			passwordTxt.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
			if (Utils.isAppleMacOS()) passwordTxt.setEchoChar('\u2022');

			GridData gd = new GridData();
			gd.horizontalSpan = 2;
			deploy = new Button(composite, SWT.PUSH);
			deploy.setLayoutData(gd);
			deploy.setText("Deploy");
			deploy.addSelectionListener(new SelectionListener()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					deployURL = deployURLTxt.getText();
					deployUsername = usernameTxt.getText();
					deployPassword = passwordTxt.getText();
					deployOutput.setText("");
					doDeploy(deployURL, deployUsername, deployPassword, exportModel.getFileName());
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e)
				{
					// TODO Auto-generated method stub
				}
			});

			gd = new GridData();
			gd.horizontalSpan = 2;
			lbl = new Label(composite, SWT.NONE);
			lbl.setLayoutData(gd);
			lbl.setText("Response");

			gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.horizontalSpan = 2;
			deployOutput = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
			deployOutput.setEditable(false);
			deployOutput.setLayoutData(gd);
			setControl(composite);
		}

		private void doDeploy(final String url, final String username, final String password, final String exportFile)
		{
			deploy.setEnabled(false);
			Job job = new Job("Deploying to Servoy application server")
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					final StringBuilder responseMessage = new StringBuilder();
					// the file we want to upload
					File inFile = new File(exportFile);
					try
					{
						HttpClient httpclient = HttpClients.createDefault();

						CredentialsProvider credsProvider = new BasicCredentialsProvider();
						credsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
							new UsernamePasswordCredentials(username, password));

						HttpPost httppost = new HttpPost(url);


						MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
						multipartEntityBuilder.addPart("if", new FileBody(inFile));
						if (exportModel.isProtectWithPassword())
						{
							multipartEntityBuilder.addPart("solution_password", new StringBody(exportModel.getPassword(), ContentType.MULTIPART_FORM_DATA));
						}
						httppost.setEntity(multipartEntityBuilder.build());

						// execute the request
						HttpClientContext context = HttpClientContext.create();
						context.setCredentialsProvider(credsProvider);
						HttpResponse response = httpclient.execute(httppost, context);

						if (response.getStatusLine().getStatusCode() == 200)
						{
							HttpEntity responseEntity = response.getEntity();
							String responseString = EntityUtils.toString(responseEntity);
							String[] responses = responseString.split("\n");

							for (String s : responses)
							{
								responseMessage.append(s.trim()).append('\n');
							}
						}
						else
						{
							responseMessage.append("HTTP ERROR : ").append(response.getStatusLine().getStatusCode()).append(' ').append(
								response.getStatusLine().getReasonPhrase());
						}
					}
					catch (ClientProtocolException e)
					{
						String msg = "Unable to make connection";
						System.err.println(msg);
						responseMessage.append(msg);
						e.printStackTrace();
					}
					catch (IOException e)
					{
						String msg = "Unable to read file";
						System.err.println(msg);
						responseMessage.append(msg);
						e.printStackTrace();
					}
					finally
					{
						Display.getDefault().syncExec(new Runnable()
						{
							@Override
							public void run()
							{
								deployOutput.setText(responseMessage.toString());
							}
						});
					}

					return Status.OK_STATUS;
				}
			};
			job.addJobChangeListener(this);
			job.schedule();
		}

		@Override
		public void setVisible(boolean visible)
		{
			super.setVisible(visible);
			if (visible)
			{
				deployOutput.setText("");
				deploy.setEnabled(false);
				doExport(this);
			}
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#aboutToRun(org.eclipse.core.runtime.jobs.IJobChangeEvent)
		 */
		@Override
		public void aboutToRun(IJobChangeEvent event)
		{
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#awake(org.eclipse.core.runtime.jobs.IJobChangeEvent)
		 */
		@Override
		public void awake(IJobChangeEvent event)
		{
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#done(org.eclipse.core.runtime.jobs.IJobChangeEvent)
		 */
		@Override
		public void done(IJobChangeEvent event)
		{
			Display.getDefault().syncExec(new Runnable()
			{
				@Override
				public void run()
				{
					if (!deploy.isDisposed())
					{
						deploy.setEnabled(true);
					}
				}
			});
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#running(org.eclipse.core.runtime.jobs.IJobChangeEvent)
		 */
		@Override
		public void running(IJobChangeEvent event)
		{
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#scheduled(org.eclipse.core.runtime.jobs.IJobChangeEvent)
		 */
		@Override
		public void scheduled(IJobChangeEvent event)
		{
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#sleeping(org.eclipse.core.runtime.jobs.IJobChangeEvent)
		 */
		@Override
		public void sleeping(IJobChangeEvent event)
		{
			// TODO Auto-generated method stub

		}
	}


	private Font getBoldFont(Label label)
	{
		if (labelBoldFont == null)
		{
			FontDescriptor boldDescriptor = FontDescriptor.createFrom(label.getFont()).setStyle(SWT.BOLD);
			labelBoldFont = boldDescriptor.createFont(label.getDisplay());
		}
		return labelBoldFont;
	}

	@Override
	public void dispose()
	{
		if (labelBoldFont != null)
		{
			labelBoldFont.dispose();
		}
		super.dispose();
	}
}
