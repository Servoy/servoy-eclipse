/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * @author emera
 */
public class GenerateSolutionWizardPage extends WizardPage implements ICheckBoxView
{

	private String solutionName;
	private Text solutionNameField;

	private CheckboxTableViewer checkboxTableViewer;
	private Button isAdvancedCheck;
	private boolean isAdvanced = false;
	private SelectAllButtonsBar selectAllButtons;

	protected static final String SECURITY = "security";
	protected static final String UTILS = "utils";
	protected static final String SEARCH = "search";

	private static String[] toImport = new String[] { SECURITY, UTILS, SEARCH };

	protected GenerateSolutionWizardPage(String pageName)
	{
		super(pageName);
	}


	@Override
	public void createControl(Composite parent)
	{
		initializeDialogUnits(parent);

		// top level group
		Composite topLevel = new Composite(parent, SWT.NONE);
		topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		setControl(topLevel);

		// solution name UI
		Label solutionLabel = new Label(topLevel, SWT.NONE);
		solutionLabel.setText("Solution name ");

		solutionNameField = new Text(topLevel, SWT.BORDER);
		solutionNameField.addVerifyListener(DocumentValidatorVerifyListener.IDENT_SERVOY_VERIFIER);
		solutionNameField.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				handleSolutionNameChanged();
			}
		});

		Label configLabel = new Label(topLevel, SWT.NONE);
		configLabel.setText("Configure the new solution with ");

		checkboxTableViewer = CheckboxTableViewer.newCheckList(topLevel, SWT.BORDER | SWT.FULL_SELECTION);
		checkboxTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		checkboxTableViewer.setInput(toImport);
		checkboxTableViewer.setAllChecked(true);

		selectAllButtons = new SelectAllButtonsBar(this, topLevel, false);
		selectAllButtons.disableSelectAll();

		isAdvancedCheck = new Button(topLevel, SWT.CHECK);
		isAdvancedCheck.setText("I am an advanced Servoy User");
		isAdvancedCheck.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				isAdvanced = Boolean.valueOf(isAdvancedCheck.getSelection());
				getWizard().getContainer().updateButtons();
			}
		});
		isAdvancedCheck.setSelection(false);


		// layout of the page
		GridLayout gridLayout = new GridLayout(2, false);
		topLevel.setLayout(gridLayout);
		topLevel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;

		solutionLabel.setLayoutData(gridData);
		solutionNameField.setLayoutData(gridData);

		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		configLabel.setLayoutData(gridData);

		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 2;
		checkboxTableViewer.getTable().setLayoutData(gridData);

		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		isAdvancedCheck.setLayoutData(gridData);

	}

	/**
	 * Returns the name of the new solution.
	 *
	 * @return the name of the new solution.
	 */
	public String getNewSolutionName()
	{
		return solutionName;
	}

	private void handleSolutionNameChanged()
	{
		solutionName = solutionNameField.getText();
		setPageComplete(validatePage());
	}

	protected boolean validatePage()
	{
		String error = null;
		if (solutionNameField.getText().trim().length() == 0)
		{
			error = "Please give a name for the new solution";
		}
		if (error == null)
		{
			// see if solution name is OK
			if (!IdentDocumentValidator.isJavaIdentifier(solutionName))
			{
				error = "Solution name has unsupported characters";
			}
			else if (solutionName.length() > IRepository.MAX_ROOT_OBJECT_NAME_LENGTH)
			{
				error = "Solution name is too long";
			}
			else if (ServoyModel.getWorkspace().getRoot().getProject(solutionName).exists())
			{
				error = "A project with this name already exists in the workspace";
			}
			else
			{
				IStatus validationResult = ServoyModel.getWorkspace().validateName(solutionName, IResource.PROJECT);
				if (!validationResult.isOK())
				{
					error = "The name of the solution project to be created is not valid: " + validationResult.getMessage();
				}
			}
		}
		setErrorMessage(error);
		return error == null;
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		if (visible)
		{
			solutionNameField.setFocus();
			setPageComplete(validatePage());
		}
	}

	public boolean isChecked(String config)
	{
		return checkboxTableViewer.getChecked(config);
	}

	public List<String> getSolutionsToImport()
	{
		List<String> result = new ArrayList<>();
		if (isChecked(SECURITY)) result.add("svySecurity");
		if (isChecked(UTILS)) result.add("svyUtils");
		if (isChecked(SEARCH)) result.add("svySearch");
		return result;
	}

	public List<String> getWebPackagesToImport()
	{
		List<String> result = new ArrayList<>();
		result.add("12grid");
		result.add("aggrid");
		result.add("bootstrapcomponents");
		result.add("servoy-extra-components");
		return result;
	}

	@Override
	public IWizardPage getNextPage()
	{
		if (isAdvanced)
		{
			return getWizard().getNextPage(this);
		}
		return null;
	}


	@Override
	public void selectAll()
	{
		checkboxTableViewer.setAllChecked(true);
	}

	@Override
	public void deselectAll()
	{
		checkboxTableViewer.setAllChecked(false);
	}
}
