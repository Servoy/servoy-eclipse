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

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.TableDefinitionUtils;
import com.servoy.eclipse.warexporter.export.ExportWarModel;
import com.servoy.j2db.dataprocessing.IDataServerInternal;
import com.servoy.j2db.util.xmlxport.IXMLImportUserChannel;

/**
 *
 * @author jcompagner
 * @since 6.1
 */
public class FileSelectionPage extends WizardPage implements Listener
{
	/**
	 *
	 */
	private final ExportWarModel exportModel;
	private Text fileNameText;
	private Button browseButton;
	private Button exportActiveSolution;
	private final IWizardPage nextPage;
	private Button allRowsRadioButton;
	private Button exportI18NDataButton;
	private Button exportUsersButton;
	private Button exportUsingDbiFileInfoOnlyButton;
	private Button exportAllTablesFromReferencedServers;
	private Button exportMetadataTablesButton;
	private Button checkMetadataTablesButton;
	private Button exportSampleDataButton;
	private Button rowsPerTableRadioButton;
	private Spinner nrOfExportedSampleDataSpinner;


	int importUserPolicy = IXMLImportUserChannel.IMPORT_USER_POLICY_CREATE_U_UPDATE_G; // get from user.

	private Button allowDataModelChangeButton;
	private Button allowKeywordsButton;
	private Button updateSequencesButton;
	private Button overrideSequenceTypesButton;
	private Button overrideDefaultValuesButton;
	private Button insertNewI18NKeysOnlyButton;
	private Button overwriteGroupsButton;
	private Button addUsersToAdminGroupButton;
	private Button createNoneExistingUsersButton;
	private Button overwriteExistingUsersButton;

	public FileSelectionPage(ExportWarModel exportModel, IWizardPage nextPage)
	{
		super("warfileselection");
		this.exportModel = exportModel;
		this.nextPage = nextPage;
		setTitle("Choose the destination file");
		setDescription("Select the file where you want your solution exported to");
	}

	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout(1, false);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);

		Composite fileBrowsePanel = new Composite(composite, SWT.NONE);
		fileBrowsePanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fileBrowsePanel.setLayout(new GridLayout(2, false));
		fileNameText = new Text(fileBrowsePanel, SWT.BORDER);
		fileNameText.addListener(SWT.KeyUp, this);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		fileNameText.setLayoutData(gd);
		if (exportModel.getWarFileName() != null) fileNameText.setText(exportModel.getWarFileName());

		browseButton = new Button(fileBrowsePanel, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addListener(SWT.Selection, this);

		exportActiveSolution = new Button(composite, SWT.CHECK);
		exportActiveSolution.setText("Include active solution and modules");
		exportActiveSolution.setSelection(exportModel.isExportActiveSolution());
		exportActiveSolution.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setExportActiveSolution(exportActiveSolution.getSelection());

				enableSolutionExportData();
			}
		});

		allowDataModelChangeButton = new Button(composite, SWT.CHECK);
		allowDataModelChangeButton.setText("Allow data model changes");
		allowDataModelChangeButton.setSelection(exportModel.isAllowDataModelChanges());
		allowDataModelChangeButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setAllowDataModelChanges(allowDataModelChangeButton.getSelection());
			}
		});

		exportUsingDbiFileInfoOnlyButton = new Button(composite, SWT.CHECK);
		exportUsingDbiFileInfoOnlyButton.setText("Export based on DBI files only");
		exportUsingDbiFileInfoOnlyButton.setSelection(exportModel.isExportUsingDbiFileInfoOnly());
		exportUsingDbiFileInfoOnlyButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setExportUsingDbiFileInfoOnly(exportUsingDbiFileInfoOnlyButton.getSelection());
			}
		});

		exportAllTablesFromReferencedServers = new Button(composite, SWT.CHECK);
		exportAllTablesFromReferencedServers.setText("Export all tables from referenced servers");
		exportAllTablesFromReferencedServers.setSelection(exportModel.isExportAllTablesFromReferencedServers());
		exportAllTablesFromReferencedServers.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setExportAllTablesFromReferencedServers(exportAllTablesFromReferencedServers.getSelection());
			}
		});
		allowKeywordsButton = new Button(composite, SWT.CHECK);
		allowKeywordsButton.setText("Allow sql keywords");
		allowKeywordsButton.setSelection(exportModel.isAllowSQLKeywords());
		allowKeywordsButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setAllowSQLKeywords(allowKeywordsButton.getSelection());
			}
		});

		updateSequencesButton = new Button(composite, SWT.CHECK);
		updateSequencesButton.setText("Update sequences");
		updateSequencesButton.setSelection(exportModel.isUpdateSequences());
		updateSequencesButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setUpdateSequences(updateSequencesButton.getSelection());
			}
		});

		overrideSequenceTypesButton = new Button(composite, SWT.CHECK);
		overrideSequenceTypesButton.setText("Override sequence types");
		overrideSequenceTypesButton.setSelection(exportModel.isOverrideSequenceTypes());
		overrideSequenceTypesButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setOverrideSequenceTypes(overrideSequenceTypesButton.getSelection());
			}
		});

		overrideDefaultValuesButton = new Button(composite, SWT.CHECK);
		overrideDefaultValuesButton.setText("Override default values");
		overrideDefaultValuesButton.setSelection(exportModel.isOverrideDefaultValues());
		overrideDefaultValuesButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setOverrideDefaultValues(overrideDefaultValuesButton.getSelection());
			}
		});

		exportMetadataTablesButton = new Button(composite, SWT.CHECK);
		exportMetadataTablesButton.setText("Export metadata tables");
		exportMetadataTablesButton.setSelection(exportModel.isExportMetaData());
		exportMetadataTablesButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setExportMetaData(exportMetadataTablesButton.getSelection());
			}
		});

		checkMetadataTablesButton = new Button(composite, SWT.CHECK);
		checkMetadataTablesButton.setSelection(exportModel.isCheckMetadataTables());
		checkMetadataTablesButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setCheckMetadataTables(checkMetadataTablesButton.getSelection());
			}
		});

		exportSampleDataButton = new Button(composite, SWT.CHECK);
		exportSampleDataButton.setSelection(exportModel.isExportSampleData());
		exportSampleDataButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setExportSampleData(exportSampleDataButton.getSelection());
				nrOfExportedSampleDataSpinner.setEnabled(exportSampleDataButton.getSelection());
				allRowsRadioButton.setEnabled(exportSampleDataButton.getSelection());
				rowsPerTableRadioButton.setEnabled(exportSampleDataButton.getSelection());
			}
		});

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
		rowsPerTableRadioButton.setSelection(!exportModel.isAllRows());
		rowsPerTableRadioButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				allRowsRadioButton.setSelection(!rowsPerTableRadioButton.getSelection());
			}
		});

		GridData data3 = new GridData();
		Label textLabel = new Label(horizontalComposite, SWT.NONE);
		textLabel.setText("Rows per table: ");
		textLabel.setLayoutData(data3);

		GridData data4 = new GridData();
		nrOfExportedSampleDataSpinner = new Spinner(horizontalComposite, SWT.BORDER);
		nrOfExportedSampleDataSpinner.setMinimum(1);
		nrOfExportedSampleDataSpinner.setMaximum(IDataServerInternal.MAX_ROWS_TO_RETRIEVE);
		nrOfExportedSampleDataSpinner.setIncrement(100);
		nrOfExportedSampleDataSpinner.setEnabled(false);
		nrOfExportedSampleDataSpinner.setSelection(exportModel.getNumberOfSampleDataExported());

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
		allRowsRadioButton.setSelection(exportModel.isAllRows());
		allRowsRadioButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				nrOfExportedSampleDataSpinner.setEnabled(!allRowsRadioButton.getSelection());
				rowsPerTableRadioButton.setSelection(!allRowsRadioButton.getSelection());

				exportModel.setAllRows(allRowsRadioButton.getSelection());
			}
		});


		GridData data7 = new GridData();
		Label textLabel4 = new Label(horizontalComposite, SWT.NONE);
		textLabel4.setText("All rows.");
		textLabel4.setLayoutData(data7);

		exportI18NDataButton = new Button(composite, SWT.CHECK);
		exportI18NDataButton.setText("Export i18n data");
		exportI18NDataButton.setSelection(exportModel.isExportI18NData());
		exportI18NDataButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				insertNewI18NKeysOnlyButton.setEnabled(exportI18NDataButton.getSelection());
				exportModel.setExportI18NData(exportI18NDataButton.getSelection());
			}
		});

		horizontalComposite = new Composite(composite, SWT.None);
		hcGridLayout = new GridLayout();
		hcGridLayout.numColumns = 4;
		hcGridLayout.marginHeight = 0;
		hcGridLayout.marginWidth = 0;
		horizontalComposite.setLayout(hcGridLayout);

		new Label(horizontalComposite, SWT.NONE);
		insertNewI18NKeysOnlyButton = new Button(horizontalComposite, SWT.CHECK);
		insertNewI18NKeysOnlyButton.setText("Insert new keys only (don't update)");
		insertNewI18NKeysOnlyButton.setSelection(exportModel.isInsertNewI18NKeysOnly());
		insertNewI18NKeysOnlyButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setInsertNewI18NKeysOnly(exportUsersButton.getSelection());
			}
		});

		overwriteGroupsButton = new Button(composite, SWT.CHECK);
		overwriteGroupsButton.setText("Overwrite repository group security settings with import version");
		overwriteGroupsButton.setSelection(exportModel.isOverwriteGroups());
		overwriteGroupsButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setOverwriteGroups(overwriteGroupsButton.getSelection());
			}
		});

		exportUsersButton = new Button(composite, SWT.CHECK);
		exportUsersButton.setText("Export users");
		exportUsersButton.setSelection(exportModel.getImportUserPolicy() > IXMLImportUserChannel.IMPORT_USER_POLICY_DONT);
		exportUsersButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				addUsersToAdminGroupButton.setEnabled(exportUsersButton.getSelection());
				createNoneExistingUsersButton.setEnabled(exportUsersButton.getSelection());
				overwriteExistingUsersButton.setEnabled(exportUsersButton.getSelection());
				exportModel.setImportUserPolicy(exportUsersButton.getSelection() ? IXMLImportUserChannel.IMPORT_USER_POLICY_CREATE_U_UPDATE_G
					: IXMLImportUserChannel.IMPORT_USER_POLICY_DONT);
				createNoneExistingUsersButton.setSelection(exportModel.getImportUserPolicy() == IXMLImportUserChannel.IMPORT_USER_POLICY_CREATE_U_UPDATE_G);
				overwriteExistingUsersButton.setSelection(exportModel.getImportUserPolicy() == IXMLImportUserChannel.IMPORT_USER_POLICY_OVERWRITE_COMPLETELY);
			}
		});

		horizontalComposite = new Composite(composite, SWT.None);
		hcGridLayout = new GridLayout();
		hcGridLayout.numColumns = 4;
		hcGridLayout.marginHeight = 0;
		hcGridLayout.marginWidth = 0;
		horizontalComposite.setLayout(hcGridLayout);


		new Label(horizontalComposite, SWT.NONE);
		createNoneExistingUsersButton = new Button(horizontalComposite, SWT.CHECK);
		createNoneExistingUsersButton.setText("Create nonexisting users and add existing users to groups specified in import");
		createNoneExistingUsersButton.setSelection(exportModel.getImportUserPolicy() == IXMLImportUserChannel.IMPORT_USER_POLICY_CREATE_U_UPDATE_G);
		createNoneExistingUsersButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (createNoneExistingUsersButton.getSelection())
				{
					overwriteExistingUsersButton.setSelection(false);
					exportModel.setImportUserPolicy(IXMLImportUserChannel.IMPORT_USER_POLICY_CREATE_U_UPDATE_G);
				}
			}
		});
		new Label(horizontalComposite, SWT.NONE);
		new Label(horizontalComposite, SWT.NONE);
		new Label(horizontalComposite, SWT.NONE);
		overwriteExistingUsersButton = new Button(horizontalComposite, SWT.CHECK);
		overwriteExistingUsersButton.setText("Overwrite existing users completely (USE WITH CARE)");
		overwriteExistingUsersButton.setSelection(exportModel.getImportUserPolicy() == IXMLImportUserChannel.IMPORT_USER_POLICY_OVERWRITE_COMPLETELY);
		overwriteExistingUsersButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (overwriteExistingUsersButton.getSelection())
				{
					createNoneExistingUsersButton.setSelection(false);
					exportModel.setImportUserPolicy(IXMLImportUserChannel.IMPORT_USER_POLICY_OVERWRITE_COMPLETELY);
				}
			}
		});
		new Label(horizontalComposite, SWT.NONE);
		new Label(horizontalComposite, SWT.NONE);
		new Label(horizontalComposite, SWT.NONE);
		addUsersToAdminGroupButton = new Button(horizontalComposite, SWT.CHECK);
		addUsersToAdminGroupButton.setText("Allow users to be added to the Administrators group");
		addUsersToAdminGroupButton.setSelection(exportModel.isAddUsersToAdminGroup());
		addUsersToAdminGroupButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setAddUsersToAdminGroup(addUsersToAdminGroupButton.getSelection());
			}
		});


		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.verticalIndent = 5;
		exportActiveSolution.setLayoutData(gd);
		if (exportModel.isExportActiveSolution())
		{
			ServoyProject[] modulesOfActiveProject = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
			String[] modules = new String[modulesOfActiveProject.length];
			for (int i = 0; i < modulesOfActiveProject.length; i++)
			{
				modules[i] = modulesOfActiveProject[i].getProject().getName();
			}
			refreshDBIDownFlag(exportModel.isExportActiveSolution() && TableDefinitionUtils.hasDbDownErrorMarkers(modules));
		}
		else
		{
			refreshDBIDownFlag(false);
		}
		enableSolutionExportData();
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


	/**
	 *
	 */
	private void enableSolutionExportData()
	{
		allowDataModelChangeButton.setEnabled(exportActiveSolution.getSelection());
		exportAllTablesFromReferencedServers.setEnabled(exportActiveSolution.getSelection());
		allowKeywordsButton.setEnabled(exportActiveSolution.getSelection());
		updateSequencesButton.setEnabled(exportActiveSolution.getSelection());
		overrideSequenceTypesButton.setEnabled(exportActiveSolution.getSelection());
		overrideDefaultValuesButton.setEnabled(exportActiveSolution.getSelection());
		exportMetadataTablesButton.setEnabled(exportActiveSolution.getSelection());
		checkMetadataTablesButton.setEnabled(exportActiveSolution.getSelection());
		exportI18NDataButton.setEnabled(exportActiveSolution.getSelection());
		insertNewI18NKeysOnlyButton.setEnabled(exportActiveSolution.getSelection() && exportI18NDataButton.getSelection());
		overwriteGroupsButton.setEnabled(exportActiveSolution.getSelection());
		exportUsersButton.setEnabled(exportActiveSolution.getSelection());
		addUsersToAdminGroupButton.setEnabled(exportActiveSolution.getSelection() && exportUsersButton.getSelection());
		createNoneExistingUsersButton.setEnabled(exportActiveSolution.getSelection() && exportUsersButton.getSelection());
		overwriteExistingUsersButton.setEnabled(exportActiveSolution.getSelection() && exportUsersButton.getSelection());
		exportUsingDbiFileInfoOnlyButton.setEnabled(exportActiveSolution.getSelection());
		exportSampleDataButton.setEnabled(exportActiveSolution.getSelection());

		rowsPerTableRadioButton.setEnabled(exportActiveSolution.getSelection() && exportSampleDataButton.getSelection());
		nrOfExportedSampleDataSpinner.setEnabled(exportActiveSolution.getSelection() && exportSampleDataButton.getSelection());
		allRowsRadioButton.setEnabled(exportActiveSolution.getSelection() && exportSampleDataButton.getSelection());
	}

	public void handleEvent(Event event)
	{
		if (event.widget == fileNameText)
		{
			String potentialFileName = fileNameText.getText();
			exportModel.setWarFileName(potentialFileName);
		}
		else if (event.widget == browseButton)
		{
			Shell shell = new Shell();
			GridLayout gridLayout = new GridLayout();
			shell.setLayout(gridLayout);
			FileDialog dlg = new FileDialog(shell, SWT.SAVE);
			if (exportModel.getWarFileName() != null)
			{
				File f = new File(exportModel.getWarFileName());
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
			String[] extensions = { "*.war" };
			dlg.setFilterExtensions(extensions);
			String chosenFileName = dlg.open();
			if (chosenFileName != null)
			{
				exportModel.setWarFileName(chosenFileName);
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
		boolean messageSet = false;
		if (exportModel.getWarFileName() == null) return false;
		if (exportActiveSolution.getSelection() && ServoyModelFinder.getServoyModel().getActiveProject() == null)
		{
			setMessage("There is no active solution.", IMessageProvider.WARNING);
			result = false;
			messageSet = true;
		}
		if (fileNameText.getText().length() == 0)
		{
			result = false;
		}
		else
		{
			File f = new File(exportModel.getWarFileName());
			if (f.exists())
			{
				if (f.isDirectory())
				{
					setMessage("Specified path points to an existing folder.", IMessageProvider.WARNING);
					result = false;
					messageSet = true;
				}
				else
				{
					setMessage("Specified path points to an existing file.", IMessageProvider.INFORMATION);
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
		if (exportModel.getWarFileName() == null) return null;

		File f = new File(exportModel.getWarFileName());
		if (f.exists())
		{
			MessageBox msg = new MessageBox(this.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
			msg.setText("File already exists");
			msg.setMessage("The file you selected already exists on disk. Do you want to overwrite it?");
			if (msg.open() == SWT.YES)
			{
				return nextPage;
			}
			else
			{
				return null;
			}
		}
		else
		{
			return nextPage;
		}
	}
}