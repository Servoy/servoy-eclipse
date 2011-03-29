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
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
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
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewMethodAction;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

public class NewMethodWizard extends Wizard implements INewWizard
{
	public static final String ID = "com.servoy.eclipse.ui.NewMethodWizard"; //$NON-NLS-1$

	private MethodFormsSolutionPage methodPage;
	private WizardPage errorPage;
	private final String activeSolutionName;
	private final ServoyModel servoyModel;


	public NewMethodWizard(String activeSolutionName)
	{
		super();
		this.activeSolutionName = activeSolutionName;
		servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
	}

	public NewMethodWizard()
	{
		super();
		this.activeSolutionName = "";
		servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{

		if (servoyModel.getActiveProject() == null)
		{
			errorPage = new WizardPage("No active Servoy solution project found") //$NON-NLS-1$
			{

				public void createControl(Composite parent)
				{
					setControl(new Composite(parent, SWT.NONE));
				}

			};
			errorPage.setTitle("No active Servoy solution project found");
			errorPage.setErrorMessage("Please activate a Servoy solution project before trying to create a new style");
			errorPage.setPageComplete(false);
			methodPage = null;
		}
		else
		{
			methodPage = new MethodFormsSolutionPage("New method creation");
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
			addPage(methodPage);
		}
	}

	@Override
	public boolean performFinish()
	{
		String[] params = methodPage.getSelectedItems();
		if (params != null)
		{
			if (params[0].equals("") == false && params[1].equals("") == false && params[2].equals("") == false)
			{
				ServoyProject servoyProject = servoyModel.getServoyProject(params[0]);
				if (servoyProject != null)
				{
					Solution editingSolution = servoyProject.getEditingSolution();
					if (editingSolution != null)
					{
						Form form = editingSolution.getForm(params[1]);
						NewMethodAction.createNewMethod(null, form, null, true, params[2]);
					}
				}

			}
			else if (params[0].equals("") == false && params[1].equals("") == true && params[2].equals("") == false)
			{
				ServoyProject servoyProject = servoyModel.getServoyProject(params[0]);
				if (servoyProject != null)
				{
					Solution editingSolution = servoyProject.getEditingSolution();
					if (editingSolution != null)
					{
						NewMethodAction.createNewMethod(null, editingSolution, null, true, params[2]);
					}
				}
			}
		}
		return true;
	}


	public class MethodFormsSolutionPage extends WizardPage implements Listener
	{
		private static final String NO_FORM_PRESENT = "-- No form present --"; //$NON-NLS-1$

		private Combo solutionsCombo;
		private String[] currentSolutionNames;

		private String selectedSolutionName = ""; //$NON-NLS-1$
		private Combo formsCombo;
		private Text methodNameText;
		private String selectedFormName;
		private String selectedMethodName;
		private Button checkButton;
		private boolean globalSelected;

		protected MethodFormsSolutionPage(String pageName)
		{
			super(pageName);
			setTitle("Choose the method's location and name."); //$NON-NLS-1$
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
			selectedFormName = formsCombo.getItem(formsCombo.getSelectionIndex());
			selectedMethodName = methodNameText.getText();
			globalSelected = checkButton.getSelection();
			if (globalSelected == true || selectedFormName.equals(NO_FORM_PRESENT))
			{
				return new String[] { selectedSolutionName, "", selectedMethodName };
			}
			return new String[] { selectedSolutionName, selectedFormName, selectedMethodName };
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
					updateFormsCombo(selectedSolutionName);
				}
			});


			//Source table
			Label formsNameLabel = new Label(topLevel, SWT.NONE);
			formsNameLabel.setText("Forms name");

			formsCombo = new Combo(topLevel, SWT.DROP_DOWN | SWT.READ_ONLY);
			UIUtils.setDefaultVisibleItemCount(formsCombo);

			Label methodNameLabel = new Label(topLevel, SWT.NONE);
			methodNameLabel.setText("Method name");

			methodNameText = new Text(topLevel, SWT.BORDER);

			methodNameText.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					setPageComplete(validatePage());
				}
			});


			Label globalLabel = new Label(topLevel, SWT.NONE);
			globalLabel.setText("Global");

			checkButton = new Button(topLevel, SWT.CHECK);
			checkButton.setSelection(false);
			checkButton.addSelectionListener(new SelectionListener()
			{

				public void widgetDefaultSelected(SelectionEvent e)
				{
					// TODO Auto-generated method stub

				}

				public void widgetSelected(SelectionEvent e)
				{
					boolean result = !checkButton.getSelection();
					formsCombo.setEnabled(result);

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
			formData.top = new FormAttachment(formsCombo, 0, SWT.CENTER);
			formsNameLabel.setLayoutData(formData);

			//text
			formData = new FormData();
			formData.left = new FormAttachment(solutionsCombo, 0, SWT.LEFT);
			formData.top = new FormAttachment(solutionsCombo, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			formsCombo.setLayoutData(formData);

			//label
			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(checkButton, 0, SWT.CENTER);
			globalLabel.setLayoutData(formData);

			//text
			formData = new FormData();
			formData.left = new FormAttachment(formsCombo, 0, SWT.LEFT);
			formData.top = new FormAttachment(formsCombo, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			checkButton.setLayoutData(formData);

			//label
			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(methodNameText, 0, SWT.CENTER);
			methodNameLabel.setLayoutData(formData);

			//text
			formData = new FormData();
			formData.left = new FormAttachment(checkButton, 0, SWT.LEFT);
			formData.top = new FormAttachment(checkButton, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			methodNameText.setLayoutData(formData);

			String currentSolutionSelected = solutionsCombo.getItem(solutionsCombo.getSelectionIndex());
			updateFormsCombo(currentSolutionSelected);


		}

		protected void updateFormsCombo(String solutionName)
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			Iterator<Form> formsIterator = servoyProject.getEditingSolution().getForms(null, true);
			List formNameList = new ArrayList();
			while (formsIterator.hasNext())
			{
				Form form = formsIterator.next();
				formNameList.add(form.getName());
			}
			if (formNameList.size() == 0)
			{
				formNameList.add(NO_FORM_PRESENT);
			}
			String[] items = new String[formNameList.size()];
			formNameList.toArray(items);
			formsCombo.setItems(items);
			formsCombo.select(0);

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


		protected String methodExists(String methodName, String solutionName, String formName)
		{
			ServoyProject servoyProject = servoyModel.getServoyProject(solutionName);
			if (formName.trim().length() == 0)
			{
				Solution solution = servoyProject.getSolution();
				ValidatorSearchContext validatorSearchContext = new ValidatorSearchContext(solution, IRepository.METHODS);
				IValidateName validator = servoyModel.getNameValidator();
				try
				{
					validator.checkName(methodName, 0, validatorSearchContext, false);
				}
				catch (RepositoryException e)
				{
					return e.getMessage();
				}

			}
			else
			{
				Form form = servoyProject.getEditingSolution().getForm(formName);
				ValidatorSearchContext validatorSearchContext = new ValidatorSearchContext(form, IRepository.METHODS);
				IValidateName validator = servoyModel.getNameValidator();
				try
				{
					validator.checkName(methodName, 0, validatorSearchContext, false);
				}
				catch (RepositoryException e)
				{
					return e.getMessage();
				}
			}
			return "";
		}

		protected boolean validatePage()
		{
			String error = null;
			if (methodNameText.getText().trim().length() == 0)
			{
				error = "Enter a method name";
			}
			else if (!IdentDocumentValidator.isJavaIdentifier(methodNameText.getText()))
			{
				error = "Invalid relation name";
			}
			else
			{
				String methodName = methodNameText.getText();
				String solutionName = solutionsCombo.getItem(solutionsCombo.getSelectionIndex());
				String formName = formsCombo.getItem(formsCombo.getSelectionIndex());

				boolean selected = checkButton.getSelection();
				if (selected == true)
				{
					formName = "";
				}

				String errMessage = methodExists(methodName, solutionName, formName);
				if (errMessage.equals("") == false)
				{
					error = "Name error encountered due to following reason: " + errMessage;
				}

			}

			setErrorMessage(error);
			return error == null;
		}

		public void handleEvent(Event event)
		{
			setPageComplete(validatePage());
		}

	}

}