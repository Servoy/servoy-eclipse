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

import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.servoy.eclipse.core.IDeveloperServoyModel;
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

	public static final String ID = "com.servoy.eclipse.ui.NewMethodWizard";

	private MethodFormsSolutionPage methodPage;
	private WizardPage errorPage;
	private final String activeSolutionName;

	public NewMethodWizard(String activeSolutionName)
	{
		super();
		this.activeSolutionName = activeSolutionName;
	}

	public NewMethodWizard()
	{
		this("");
	}


	public void init(IWorkbench workbench, IStructuredSelection selection)
	{

		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() == null)
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
	public void createPageControls(Composite pageContainer)
	{
		pageContainer.getShell().setData(CSSSWTConstants.CSS_ID_KEY, "svydialog");
		super.createPageControls(pageContainer);
	}

	@Override
	public boolean performFinish()
	{
		SelectedMethod selectedMethod = methodPage.getSelectedItems();
		if (selectedMethod != null && !selectedMethod.selectedMethodName.equals(""))
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(selectedMethod.selectedSolutionName);
			if (servoyProject != null)
			{
				if (selectedMethod.formMethod)
				{
					Solution editingSolution = servoyProject.getEditingSolution();
					if (editingSolution != null)
					{
						NewMethodAction.createNewMethod(null, editingSolution.getForm(selectedMethod.context), null, true, selectedMethod.selectedMethodName,
							null, null);
					}
				}
				else
				{
					Solution editingSolution = servoyProject.getEditingSolution();
					if (editingSolution != null)
					{
						NewMethodAction.createNewMethod(null, editingSolution, null, true, selectedMethod.selectedMethodName, selectedMethod.context, null);
					}
				}
			}
		}
		return true;
	}

	public class MethodFormsSolutionPage extends WizardPage implements Listener
	{
		private static final String NO_FORM_PRESENT = "-- No form present --";

		private Combo solutionsCombo;
		private String[] currentSolutionNames;

		private Combo formsCombo;
		private Combo scopesCombo;
		private Text methodNameText;
		private Button checkButton;

		protected MethodFormsSolutionPage(String pageName)
		{
			super(pageName);
			setTitle("Choose the method's location and name.");
			setDialogSettings(Activator.getDefault().getDialogSettings());

			retrieveCurrentSolutionNames();
		}

		private void retrieveCurrentSolutionNames()
		{
			ServoyProject projects[] = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();

			List<String> list = new ArrayList<String>();

			for (ServoyProject project : projects)
			{
				if (!list.contains(project.getSolution().getName()))
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
		public SelectedMethod getSelectedItems()
		{
			String selectedSolutionName = solutionsCombo.getItem(solutionsCombo.getSelectionIndex());
			String selectedFormName = formsCombo.getItem(formsCombo.getSelectionIndex());
			String selectedMethodName = methodNameText.getText();
			boolean globalSelected = checkButton.getSelection();
			if (globalSelected || selectedFormName.equals(NO_FORM_PRESENT))
			{
				String selectedScopeName = scopesCombo.getItem(scopesCombo.getSelectionIndex());
				return new SelectedMethod(false, selectedSolutionName, selectedScopeName, selectedMethodName);
			}
			return new SelectedMethod(true, selectedSolutionName, selectedFormName, selectedMethodName);
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
			if (!activeSolutionName.equals(""))
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
					updateCombos(solutionsCombo.getItem(solutionsCombo.getSelectionIndex()));
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

			scopesCombo = new Combo(topLevel, SWT.DROP_DOWN | SWT.READ_ONLY);
			UIUtils.setDefaultVisibleItemCount(scopesCombo);
			scopesCombo.setEnabled(false);

			Label globalLabel = new Label(topLevel, SWT.NONE);
			globalLabel.setText("Global");

			checkButton = new Button(topLevel, SWT.CHECK);
			checkButton.setSelection(false);
			checkButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					scopesCombo.setEnabled(checkButton.getSelection());
					formsCombo.setEnabled(!checkButton.getSelection());
				}
			});


			//Define the layout and place the components
			FormLayout formLayout = new FormLayout();
			formLayout.spacing = 10;
			formLayout.marginWidth = formLayout.marginHeight = 15;
			topLevel.setLayout(formLayout);

			// solution name label
			FormData formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(solutionsCombo, 0, SWT.CENTER);
			solutionNameLabel.setLayoutData(formData);

			// solutions combo
			formData = new FormData();
			formData.left = new FormAttachment(solutionNameLabel, 0);
			formData.top = new FormAttachment(0, 0);
			formData.right = new FormAttachment(100, 0);
			solutionsCombo.setLayoutData(formData);

			// forms name label
			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(formsCombo, 0, SWT.CENTER);
			formsNameLabel.setLayoutData(formData);

			// forms combo
			formData = new FormData();
			formData.left = new FormAttachment(solutionsCombo, 0, SWT.LEFT);
			formData.top = new FormAttachment(solutionsCombo, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			formsCombo.setLayoutData(formData);

			// global method label
			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(checkButton, 0, SWT.CENTER);
			globalLabel.setLayoutData(formData);

			// global method check
			formData = new FormData();
			formData.left = new FormAttachment(formsCombo, 0, SWT.LEFT);
			formData.top = new FormAttachment(formsCombo, 0, SWT.BOTTOM);
			checkButton.setLayoutData(formData);

			// scopenames combo
			formData = new FormData();
			formData.left = new FormAttachment(checkButton, 0, SWT.RIGHT);
			formData.top = new FormAttachment(formsCombo, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			scopesCombo.setLayoutData(formData);

			// method name label
			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(methodNameText, 0, SWT.CENTER);
			methodNameLabel.setLayoutData(formData);

			// method name text
			formData = new FormData();
			formData.left = new FormAttachment(checkButton, 0, SWT.LEFT);
			formData.top = new FormAttachment(checkButton, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			methodNameText.setLayoutData(formData);

			updateCombos(solutionsCombo.getItem(solutionsCombo.getSelectionIndex()));
		}

		protected void updateCombos(String solutionName)
		{
			// update forms combo
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			Iterator<Form> formsIterator = servoyProject.getEditingSolution().getForms(null, true);
			List<String> formNameList = new ArrayList<String>();
			while (formsIterator.hasNext())
			{
				formNameList.add(formsIterator.next().getName());
			}
			if (formNameList.size() == 0)
			{
				formNameList.add(NO_FORM_PRESENT);
			}
			formsCombo.setItems(formNameList.toArray(new String[formNameList.size()]));
			formsCombo.select(0);

			// update scopes combo
			String scopeNames[] = servoyProject.getEditingSolution().getRuntimeProperty(Solution.SCOPE_NAMES);
			scopesCombo.setItems(scopeNames);
			scopesCombo.select(0);

			if (0 == scopeNames.length)
			{
				checkButton.setEnabled(false);
			}

			if (formNameList.get(0).equals(NO_FORM_PRESENT) && 0 == scopeNames.length)
			{
				setPageComplete(validatePage());
			}
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
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
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
				boolean globalSelected = checkButton.getSelection();
				String formName = globalSelected ? "" : formsCombo.getItem(formsCombo.getSelectionIndex());
				String errMessage = methodExists(methodName, solutionName, formName);
				if (!errMessage.equals(""))
				{
					error = "Name error encountered due to following reason: " + errMessage;
				}
				if (!globalSelected && formName.equals(NO_FORM_PRESENT))
				{
					if (-1 == scopesCombo.getSelectionIndex())
					{
						error = "Create a form";
					}
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

	public static class SelectedMethod
	{
		public final String selectedSolutionName;
		public final String context;
		public final String selectedMethodName;
		public final boolean formMethod;

		public SelectedMethod(boolean formMethod, String selectedSolutionName, String context, String selectedMethodName)
		{
			this.formMethod = formMethod;
			this.selectedSolutionName = selectedSolutionName;
			this.context = context;
			this.selectedMethodName = selectedMethodName;
		}
	}

}