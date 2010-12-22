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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewValueListAction;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.util.IdentDocumentValidator;

public class NewValueListWizard extends Wizard implements INewWizard
{
	public static final String ID = "com.servoy.eclipse.ui.NewValueListWizard"; //$NON-NLS-1$

	private ValueListNameSolutionPage valuelistPage;
	private WizardPage errorPage;
	private final String activeSolutionName;
	private final ServoyModel servoyModel;


	public NewValueListWizard(String activeSolutionName)
	{
		super();
		this.activeSolutionName = activeSolutionName;
		servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
	}

	public NewValueListWizard()
	{
		super();
		this.activeSolutionName = "";
		servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{

		if (servoyModel.getActiveProject() == null)
		{
			errorPage = new WizardPage("No active Servoy solution project found")
			{

				public void createControl(Composite parent)
				{
					setControl(new Composite(parent, SWT.NONE));
				}

			};
			errorPage.setTitle("No active Servoy solution project found");
			errorPage.setErrorMessage("Please activate a Servoy solution project before trying to create a new style");
			errorPage.setPageComplete(false);
			valuelistPage = null;
		}
		else
		{
			valuelistPage = new ValueListNameSolutionPage("New valuelist creation");
		}

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
			// add pages to this wizard
			addPage(valuelistPage);
		}
	}

	@Override
	public boolean performFinish()
	{
		String[] params = valuelistPage.getSelectedItems();
		if (params != null)
		{
			if (params[0].equals("") == false && params[1].equals("") == false)
			{
				ServoyProject servoyProject = servoyModel.getServoyProject(params[0]);
				Solution editingSolution = servoyProject.getEditingSolution();
				NewValueListAction.createValueList(params[1], editingSolution);
			}
		}
		return true;
	}


	public class ValueListNameSolutionPage extends WizardPage implements Listener
	{

		private Combo solutionsCombo;
		private String[] currentSolutionNames;

		private String selectedSolutionName = ""; //$NON-NLS-1$
		private String selectedvaluelistName = ""; //$NON-NLS-1$
		private Text valuelistNameText;

		protected ValueListNameSolutionPage(String pageName)
		{
			super(pageName);
			setTitle("Select the solution and set the value-list name.");
			setDialogSettings(Activator.getDefault().getDialogSettings());

			retrieveCurrentSolutionNames();
		}

		private void retrieveCurrentSolutionNames()
		{
			ServoyProject servoyProject = servoyModel.getActiveProject();
			Solution solution = servoyProject.getSolution();
			ServoyProject projects[] = servoyModel.getModulesOfActiveProject();

			List<String> list = new ArrayList<String>();

			for (ServoyProject project : projects)
			{
				if (list.contains(project.getSolution().getName()) == false)
				{
					list.add(project.getSolution().getName());
				}
			}

			currentSolutionNames = new String[list.size()];
			list.toArray(currentSolutionNames);

		}

		/**
		 * 
		 * @return a string[] with the selected combo items
		 */
		public String[] getSelectedItems()
		{
			selectedSolutionName = solutionsCombo.getItem(solutionsCombo.getSelectionIndex());
			selectedvaluelistName = valuelistNameText.getText();
			return new String[] { selectedSolutionName, selectedvaluelistName };
		}

		public void createControl(Composite parent)
		{
			initializeDialogUnits(parent);
			// top level group
			Composite topLevel = new Composite(parent, SWT.NONE);
			topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

			setControl(topLevel);

			// Source server
			Label solutionNameLabel = new Label(topLevel, SWT.NONE);
			solutionNameLabel.setText("Solution name");

			solutionsCombo = new Combo(topLevel, SWT.DROP_DOWN | SWT.READ_ONLY);
			UIUtils.setDefaultVisibleItemCount(solutionsCombo);

			solutionsCombo.setItems(currentSolutionNames);
			int counter = 0;
			if (activeSolutionName.equals("") == false)
			{
				for (int i = 0; i < currentSolutionNames.length; i++)
				{
					if (currentSolutionNames[i].equals(activeSolutionName))
					{
						counter = i;
						break;
					}
				}
			}
			solutionsCombo.select(counter);


			solutionsCombo.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent event)
				{
					selectedSolutionName = solutionsCombo.getItem(solutionsCombo.getSelectionIndex());
				}
			});


			//Source table
			Label valuelistNameLabel = new Label(topLevel, SWT.NONE);
			valuelistNameLabel.setText("Value-list name");

			valuelistNameText = new Text(topLevel, SWT.BORDER);
			valuelistNameText.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent event)
				{
					selectedvaluelistName = valuelistNameText.getText();
				}
			});

			valuelistNameText.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					setPageComplete(validatePage());
				}
			});

			//Define the layout and place the components
			FormLayout formLayout = new FormLayout();
			formLayout.spacing = 10;
			formLayout.marginWidth = formLayout.marginHeight = 15;
			topLevel.setLayout(formLayout);

			//label
			FormData formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(solutionsCombo, 0, SWT.CENTER);
			solutionNameLabel.setLayoutData(formData);

			//combo
			formData = new FormData();
			formData.left = new FormAttachment(solutionNameLabel, 0);
			formData.top = new FormAttachment(0, 0);
			formData.right = new FormAttachment(100, 0);
			solutionsCombo.setLayoutData(formData);

			//label
			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(valuelistNameText, 0, SWT.CENTER);
			valuelistNameLabel.setLayoutData(formData);

			//text
			formData = new FormData();
			formData.left = new FormAttachment(solutionsCombo, 0, SWT.LEFT);
			formData.top = new FormAttachment(solutionsCombo, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			valuelistNameText.setLayoutData(formData);

		}

		@Override
		public void setVisible(boolean visible)
		{
			super.setVisible(visible);

			if (visible)
			{
				setPageComplete(validatePage());

			}
		}


		protected boolean validatePage()
		{
			String error = null;
			if (valuelistNameText.getText().trim().length() == 0)
			{
				error = "Enter a valuelist name";
			}
			else if (!IdentDocumentValidator.isJavaIdentifier(valuelistNameText.getText()))
			{
				error = "Invalid relation name";
			}
			else
			{
				String valName = valuelistNameText.getText();
				String solName = solutionsCombo.getItem(solutionsCombo.getSelectionIndex());
				String searchResult = valueListNameOk(valName, solName);
				if (!searchResult.equals(""))
				{
					setErrorMessage(searchResult);
					return false;
				}
			}

			setErrorMessage(error);
			return error == null;
		}

		protected String valueListNameOk(String valueListName, String solutionName)
		{

			IValidateName validator = servoyModel.getNameValidator();
			ServoyProject servoyProject = servoyModel.getServoyProject(solutionName);

			ValidatorSearchContext searchContext = null;
			if (servoyProject != null)
			{
				searchContext = new ValidatorSearchContext(servoyProject.getEditingSolution(), IRepository.VALUELISTS);
			}

			try
			{
				validator.checkName(valueListName, 0, null, false);
			}
			catch (RepositoryException e)
			{
				return e.getMessage();
			}

			return "";
		}

		public void handleEvent(Event event)
		{
			setPageComplete(validatePage());
		}

	}

}
