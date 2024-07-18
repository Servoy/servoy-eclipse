/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.eclipse.ui.wizards.exportsolution.pages;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.BuilderUtils;
import com.servoy.eclipse.model.export.IExportSolutionModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.wizards.ExportSolutionWizard;
import com.servoy.j2db.dataprocessing.IDataServerInternal;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 *
 */
public class ExportOptionsPage extends WizardPage implements Listener
{
	private final ExportSolutionWizard exportSolutionWizard;
	private Button protectWithPasswordButton;
	private Button exportReferencedModulesButton;
	private Button exportReferencedWebPackagesButton;
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
	private Text mainSolutionVersion;

	public ExportOptionsPage(ExportSolutionWizard exportSolutionWizard)
	{
		super("page2");
		setTitle("Choose export options");
		setDescription("Specify the options for your export");
		this.exportSolutionWizard = exportSolutionWizard;

		IDeveloperServoyModel model = ServoyModelManager.getServoyModelManager().getServoyModel();
		resourcesProjectProblemsType = model.getActiveResourcesProject() != null
			? BuilderUtils.getMarkers(new String[] { model.getActiveResourcesProject().getProject().getName() }) : 0;

		updateMessages();
	}

	@Override
	public boolean canFlipToNextPage()
	{
		return (exportSolutionWizard.hasActiveSolutionDbDownErrors() ||
			(exportUsingDbiFileInfoOnlyButton != null && exportUsingDbiFileInfoOnlyButton.getSelection()) ||
			resourcesProjectProblemsType == BuilderUtils.HAS_NO_MARKERS || resourcesProjectProblemsType == BuilderUtils.HAS_WARNING_MARKERS) &&
			!"".equals(mainSolutionVersion.getText().trim()) && super.canFlipToNextPage();
	}

	private void updateMessages()
	{
		setMessage(null);
		setErrorMessage(null);
		if (Utils.stringIsEmpty(exportSolutionWizard.getActiveSolution().getVersion()))
		{
			setMessage("Please set a version number for the main solution to be able to complete the export.", IMessageProvider.WARNING);
		}
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
				if (exportSolutionWizard.getModulesSelectionPage().hasDBDownErrors())
				{
					setMessage(ModulesSelectionPage.DB_DOWN_WARNING, IMessageProvider.WARNING);
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

		Composite comp = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		comp.setLayout(layout);
		Label mainSolution = new Label(comp, SWT.NONE);
		mainSolution.setText("Solution version");
		mainSolutionVersion = new Text(comp, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 200;
		mainSolutionVersion.setLayoutData(gd);
		Label warnLabel = new Label(comp, SWT.NONE);
		String version = exportSolutionWizard.getActiveSolution().getVersion();
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		EclipseRepository repository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
		ServoyProject servoyProject = servoyModel.getServoyProject(exportSolutionWizard.getActiveSolution().getName());
		Solution editingSolution = servoyProject.getEditingSolution();
		if (version == null || "".equals(version))
		{
			version = "1.0"; //default version
			mainSolutionVersion.setText(version);
			setMainSolutionVersion(mainSolutionVersion, repository, warnLabel, editingSolution);
			updateMessages();
		}
		else
		{
			mainSolutionVersion.setText(version);
		}

		mainSolutionVersion.addModifyListener(event -> setMainSolutionVersion(mainSolutionVersion, repository, warnLabel, editingSolution));
		mainSolutionVersion.addListener(SWT.FocusOut, event -> setMainSolutionVersion(mainSolutionVersion, repository, warnLabel, editingSolution));
		mainSolutionVersion.addListener(SWT.CR, event -> setMainSolutionVersion(mainSolutionVersion, repository, warnLabel, editingSolution));

		warnLabel.setImage(Activator.getDefault().loadImageFromBundle("warning.png"));
		warnLabel.setToolTipText("Please set a version for the main solution.");
		warnLabel.setVisible(Utils.stringIsEmpty(mainSolutionVersion.getText()));

		protectWithPasswordButton = new Button(composite, SWT.CHECK);
		protectWithPasswordButton.setText("Protect solution with password");
		protectWithPasswordButton.setSelection(exportSolutionWizard.getModel().isProtectWithPassword());
		protectWithPasswordButton.addListener(SWT.Selection, this);

		exportReferencedModulesButton = new Button(composite, SWT.CHECK);
		exportReferencedModulesButton.setText("Export referenced modules");
		exportReferencedModulesButton.setSelection(exportSolutionWizard.getModel().isExportReferencedModules());
		exportReferencedModulesButton.addListener(SWT.Selection, this);
		exportReferencedModulesButton.setEnabled(exportSolutionWizard.getActiveSolution().getModulesNames() != null);

		exportReferencedWebPackagesButton = new Button(composite, SWT.CHECK);
		exportReferencedWebPackagesButton.setText("Export referenced Servoy packages ( for importing ONLY into developer)");
		exportReferencedWebPackagesButton.setSelection(exportSolutionWizard.getModel().isExportReferencedWebPackages());
		exportReferencedWebPackagesButton.addListener(SWT.Selection, this);

		exportAllTablesFromReferencedServers = new Button(composite, SWT.CHECK);
		exportAllTablesFromReferencedServers.setText("Export all tables from referenced servers");
		exportAllTablesFromReferencedServers.setSelection(exportSolutionWizard.getModel().isExportAllTablesFromReferencedServers());
		exportAllTablesFromReferencedServers.addListener(SWT.Selection, this);

		exportMetadataTablesButton = new Button(composite, SWT.CHECK);
		exportMetadataTablesButton.setText("Export metadata tables (from database)");
		exportMetadataTablesButton.setSelection(exportSolutionWizard.getModel().isExportMetaData());
		exportMetadataTablesButton.addListener(SWT.Selection, this);

		checkMetadataTablesButton = new Button(composite, SWT.CHECK);
		checkMetadataTablesButton.setSelection(exportSolutionWizard.getModel().isCheckMetadataTables());
		checkMetadataTablesButton.addListener(SWT.Selection, this);

		exportSampleDataButton = new Button(composite, SWT.CHECK);
		exportSampleDataButton.setSelection(exportSolutionWizard.getModel().isExportSampleData());
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
		rowsPerTableRadioButton.setEnabled(exportSolutionWizard.getModel().isExportSampleData());
		rowsPerTableRadioButton.setSelection(exportSolutionWizard.getModel().getNumberOfSampleDataExported() != IDataServerInternal.MAX_ROWS_TO_RETRIEVE);

		GridData data3 = new GridData();
		Label textLabel = new Label(horizontalComposite, SWT.NONE);
		textLabel.setText("Rows per table: ");
		textLabel.setLayoutData(data3);

		GridData data4 = new GridData();
		nrOfExportedSampleDataSpinner = new Spinner(horizontalComposite, SWT.BORDER);
		nrOfExportedSampleDataSpinner.setMinimum(1);
		nrOfExportedSampleDataSpinner.setMaximum(IDataServerInternal.MAX_ROWS_TO_RETRIEVE);
		nrOfExportedSampleDataSpinner.setSelection(exportSolutionWizard.getModel().getNumberOfSampleDataExported() != IDataServerInternal.MAX_ROWS_TO_RETRIEVE
			? exportSolutionWizard.getModel().getNumberOfSampleDataExported() : IExportSolutionModel.DEFAULT_NUMBER_OF_SAMPLE_DATA_ROWS_IF_DATA_IS_EXPORTED);
		nrOfExportedSampleDataSpinner.setIncrement(100);
		nrOfExportedSampleDataSpinner.setEnabled(exportSolutionWizard.getModel().isExportSampleData() &&
			exportSolutionWizard.getModel().getNumberOfSampleDataExported() != IDataServerInternal.MAX_ROWS_TO_RETRIEVE);

		nrOfExportedSampleDataSpinner.setLayoutData(data4);

		nrOfExportedSampleDataSpinner.addModifyListener(new ModifyListener()
		{

			public void modifyText(ModifyEvent e)
			{
				applyNrOfExportedSampleDataSpinnerValue();
			}

		});

		GridData data5 = new GridData();
		Button emptyCBButton2 = new Button(horizontalComposite, SWT.CHECK);
		emptyCBButton2.setVisible(false);
		emptyCBButton2.setLayoutData(data5);

		GridData data6 = new GridData();
		allRowsRadioButton = new Button(horizontalComposite, SWT.RADIO);
		allRowsRadioButton.setEnabled(exportSolutionWizard.getModel().isExportSampleData());
		allRowsRadioButton.setSelection(exportSolutionWizard.getModel().getNumberOfSampleDataExported() == IDataServerInternal.MAX_ROWS_TO_RETRIEVE);
		allRowsRadioButton.setLayoutData(data6);
		allRowsRadioButton.addListener(SWT.Selection, this);
		allRowsRadioButton.setToolTipText(
			"As this is not meant as a DB export/import tool, the number if exported rows will still be limited but to a very high number (" +
				IDataServerInternal.MAX_ROWS_TO_RETRIEVE + ")");

		GridData data7 = new GridData();
		Label textLabel4 = new Label(horizontalComposite, SWT.NONE);
		textLabel4.setText("All rows.");
		textLabel4.setLayoutData(data7);

		exportI18NDataButton = new Button(composite, SWT.CHECK);
		exportI18NDataButton.setText("Export i18n data");
		exportI18NDataButton.setSelection(exportSolutionWizard.getModel().isExportI18NData());
		exportI18NDataButton.addListener(SWT.Selection, this);

		exportUsersButton = new Button(composite, SWT.CHECK);
		exportUsersButton.setText("Export users");
		exportUsersButton.setSelection(exportSolutionWizard.getModel().isExportUsers());
		exportUsersButton.addListener(SWT.Selection, this);

		exportUsingDbiFileInfoOnlyButton = new Button(composite, SWT.CHECK);
		exportUsingDbiFileInfoOnlyButton.setText("Export based on DBI files only");
		exportUsingDbiFileInfoOnlyButton.addListener(SWT.Selection, this);

		refreshDBIDownFlag(exportSolutionWizard.getModel().isExportReferencedModules() && exportSolutionWizard.getModulesSelectionPage().hasDBDownErrors());

		useImportSettingsButton = new Button(composite, SWT.CHECK);
		useImportSettingsButton.setText("Create import settings / Deploy to Servoy application server");
		useImportSettingsButton.setSelection(exportSolutionWizard.getModel().useImportSettings());
		useImportSettingsButton.addListener(SWT.Selection, this);

		setControl(composite);
	}

	protected void setMainSolutionVersion(Text mainSolutionVersion, EclipseRepository repository, Label warnLabel, Solution editingSolution)
	{
		if (mainSolutionVersion.getText().trim().equals(editingSolution.getVersion())) return;
		editingSolution.setVersion(mainSolutionVersion.getText());
		repository.updateNodesInWorkspace(new IPersist[] { editingSolution }, false);
		warnLabel.setVisible(Utils.stringIsEmpty(mainSolutionVersion.getText()));
		if (isCurrentPage()) getWizard().getContainer().updateButtons();
		PersistPropertySource.refreshPropertiesView();
	}

	private void applyNrOfExportedSampleDataSpinnerValue()
	{
		int maxRowToRetrieve = nrOfExportedSampleDataSpinner.getSelection();
		if (maxRowToRetrieve == 0) // spinner has minimum of 1 so how can this be 0?
		{
			maxRowToRetrieve = IDataServerInternal.MAX_ROWS_TO_RETRIEVE;
		}
		exportSolutionWizard.getModel().setNumberOfSampleDataExported(maxRowToRetrieve);
	}

	public void refreshDBIDownFlag(boolean dbiDown)
	{
		exportUsingDbiFileInfoOnlyButton.setEnabled(!dbiDown);
		exportUsingDbiFileInfoOnlyButton.setSelection(dbiDown ? true : exportSolutionWizard.getModel().isExportUsingDbiFileInfoOnly());
		if (dbiDown)
		{
			exportUsingDbiFileInfoOnlyButton.setText("Export based on DBI files only (one or more used databases is unreacheable)");
		}
		else
		{
			exportUsingDbiFileInfoOnlyButton.setText("Export based on DBI files only");
		}

		exportSampleDataButton.setEnabled(!dbiDown);
		exportSampleDataButton.setSelection(dbiDown ? false : exportSolutionWizard.getModel().isExportSampleData());
		if (dbiDown)
		{
			exportSampleDataButton.setText("Export solution sample data (one or more used databases is unreacheable)");
		}
		else
		{
			exportSampleDataButton.setText("Export solution sample data");
		}
		checkMetadataTablesButton.setEnabled(!dbiDown && exportMetadataTablesButton.getSelection());
		checkMetadataTablesButton.setSelection(dbiDown ? false : exportSolutionWizard.getModel().isCheckMetadataTables());
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
		if (event.widget == protectWithPasswordButton) exportSolutionWizard.getModel().setProtectWithPassword(protectWithPasswordButton.getSelection());
		else if (event.widget == useImportSettingsButton) exportSolutionWizard.getModel().setUseImportSettings(useImportSettingsButton.getSelection());
		else if (event.widget == exportReferencedModulesButton)
		{
			exportSolutionWizard.getModel().setExportReferencedModules(exportReferencedModulesButton.getSelection());
			refreshDBIDownFlag(exportSolutionWizard.getModel().isExportReferencedModules() && exportSolutionWizard.getModulesSelectionPage().hasDBDownErrors());
		}
		else if (event.widget == exportReferencedWebPackagesButton)
		{
			exportSolutionWizard.getModel().setExportReferencedWebPackages(exportReferencedWebPackagesButton.getSelection());
		}
		else if (event.widget == checkMetadataTablesButton) exportSolutionWizard.getModel().setCheckMetadataTables(checkMetadataTablesButton.getSelection());
		else if (event.widget == exportMetadataTablesButton)
		{
			exportSolutionWizard.getModel().setExportMetaData(exportMetadataTablesButton.getSelection());
			checkMetadataTablesButton.setEnabled(exportMetadataTablesButton.getSelection() && exportMetadataTablesButton.getEnabled());
		}
		else if (event.widget == exportSampleDataButton)
		{
			exportSolutionWizard.getModel().setExportSampleData(exportSampleDataButton.getSelection());

			nrOfExportedSampleDataSpinner.setEnabled(exportSampleDataButton.getSelection() && !allRowsRadioButton.getSelection());
			allRowsRadioButton.setEnabled(exportSampleDataButton.getSelection());
			rowsPerTableRadioButton.setEnabled(exportSampleDataButton.getSelection());
		}
		else if (event.widget == allRowsRadioButton)
		{
			nrOfExportedSampleDataSpinner.setEnabled(!allRowsRadioButton.getSelection());
			rowsPerTableRadioButton.setSelection(!allRowsRadioButton.getSelection());

			exportSolutionWizard.getModel().setExportSampleData(exportSampleDataButton.getSelection());
			exportSolutionWizard.getModel().setNumberOfSampleDataExported(IDataServerInternal.MAX_ROWS_TO_RETRIEVE);
		}
		else if (event.widget == rowsPerTableRadioButton)
		{
			allRowsRadioButton.setSelection(!rowsPerTableRadioButton.getSelection());
			applyNrOfExportedSampleDataSpinnerValue();
		}
		else if (event.widget == exportI18NDataButton) exportSolutionWizard.getModel().setExportI18NData(exportI18NDataButton.getSelection());
		else if (event.widget == exportUsersButton) exportSolutionWizard.getModel().setExportUsers(exportUsersButton.getSelection());
		else if (event.widget == exportAllTablesFromReferencedServers)
			exportSolutionWizard.getModel().setExportAllTablesFromReferencedServers(exportAllTablesFromReferencedServers.getSelection());
		else if (event.widget == exportUsingDbiFileInfoOnlyButton)
		{
			exportSolutionWizard.getModel().setExportUsingDbiFileInfoOnly(exportUsingDbiFileInfoOnlyButton.getSelection());
			if (!exportSolutionWizard.getModulesSelectionPage().hasDBDownErrors())
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
		if (exportSolutionWizard.getModel().isExportReferencedModules() && exportSolutionWizard.getActiveSolution().getModulesNames() != null)
			return exportSolutionWizard.getModulesSelectionPage();
		else if (exportSolutionWizard.getModel().isProtectWithPassword()) return exportSolutionWizard.getPasswordPage();
		else if (exportSolutionWizard.getModel().useImportSettings()) return exportSolutionWizard.getImportPage();
		else return null;
	}

	@Override
	public void performHelp()
	{
		PlatformUI.getWorkbench().getHelpSystem().displayHelp("com.servoy.eclipse.ui.export_solution_export_options");
	}
}