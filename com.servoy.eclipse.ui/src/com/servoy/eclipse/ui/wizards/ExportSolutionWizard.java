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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
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

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.BuilderUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.TableDefinitionUtils;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.export.ExportSolutionJob;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.dataprocessing.IDataServerInternal;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

public class ExportSolutionWizard extends Wizard implements IExportWizard
{

	private static final String DB_DOWN_WARNING = "Error markers will be ignored because the DB seems to be offline (.dbi files will be used instead).";

	private Solution activeSolution;
	private ExportSolutionModel exportModel;
	private FileSelectionPage fileSelectionPage;
	private ExportOptionsPage exportOptionsPage;
	private ModulesSelectionPage modulesSelectionPage;
	private PasswordPage passwordPage;

	private final IFileAccess workspace;

	private boolean activeSolutionDbDownErrors = false;

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
		getDialogSettings().put("initialFileName", exportModel.getFileName());

		WorkspaceJob exportJob = new ExportSolutionJob("Exporting solution '" + activeSolution.getName() + "'", exportModel, activeSolution,
			modulesSelectionPage.hasDBDownErrors(), true, workspace);

		exportJob.setUser(true); // we want the progress to be visible in a dialog, not to stay in the status bar
		exportJob.schedule();

		return true;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		if (activeProject != null) activeSolution = activeProject.getSolution();

		exportModel = new ExportSolutionModel();
		String initialFileName = getDialogSettings().get("initialFileName");
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

		exportModel.setExportReferencedModules(activeSolution.getModulesNames() != null);


		int hasErrs = BuilderUtils.getMarkers(new String[] { ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getProject().getName() });
		if (hasErrs == BuilderUtils.HAS_ERROR_MARKERS)
		{
			activeSolutionDbDownErrors = TableDefinitionUtils.hasDbDownErrorMarkers(new String[] { ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getProject().getName() });
		}
		else
		{
			activeSolutionDbDownErrors = false;
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
			projectProblemsType = BuilderUtils.getMarkers(new String[] { ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getProject().getName() });
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
			exportMetadataTablesButton.setText("Export metadata tables");
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
			nrOfExportedSampleDataSpinner.setEnabled(false);

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
			allRowsRadioButton.setEnabled(false);
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
			checkMetadataTablesButton.setSelection(dbiDown ? false : exportModel.isExportMetaData());
			if (dbiDown)
			{
				checkMetadataTablesButton.setText("Check metadata tables (one or more used databases is unreacheable)");
			}
			else
			{
				checkMetadataTablesButton.setText("Check metadata tables");
			}
		}

		public void handleEvent(Event event)
		{
			if (event.widget == protectWithPasswordButton) exportModel.setProtectWithPassword(protectWithPasswordButton.getSelection());
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
			else if (event.widget == exportAllTablesFromReferencedServers) exportModel.setExportAllTablesFromReferencedServers(exportAllTablesFromReferencedServers.getSelection());
			else if (event.widget == exportUsingDbiFileInfoOnlyButton)
			{
				exportModel.setExportUsingDbiFileInfoOnly(exportUsingDbiFileInfoOnlyButton.getSelection());
				if (!modulesSelectionPage.hasDBDownErrors())
				{
					updateMessages();
				}
			}
			getWizard().getContainer().updateButtons();
		}

		@Override
		public IWizardPage getNextPage()
		{
			if (exportModel.isExportReferencedModules()) return modulesSelectionPage;
			else if (exportModel.isProtectWithPassword()) return passwordPage;
			else return null;
		}
	}

	private class ModulesSelectionPage extends WizardPage implements ICheckStateListener
	{
		CheckboxTreeViewer treeViewer;
		public int projectProblemsType = BuilderUtils.HAS_NO_MARKERS;
		private boolean moduleDbDownErrors = false;

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
			treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
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
				else setErrorMessage("There are errors in the modules that will prevent the solution from functioning well. Please solve errors (problems view) first.");
			}
			else if (projectProblemsType == BuilderUtils.HAS_WARNING_MARKERS)
			{
				setMessage(
					"There are warnings in the modules that may prevent the solution from functioning well. You may want to solve warnings (problems view) first.",
					IMessageProvider.WARNING);
			}

			if (isCurrentPage()) getWizard().getContainer().updateButtons();

			exportOptionsPage.refreshDBIDownFlag(hasDBDownErrors());
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
			return null;
		}
	}
}
