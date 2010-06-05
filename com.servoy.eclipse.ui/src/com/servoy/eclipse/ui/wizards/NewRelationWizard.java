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
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewRelationAction;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.util.IdentDocumentValidator;

public class NewRelationWizard extends Wizard implements INewWizard
{

	private RelationNameSolutionPage relationPage;
	private WizardPage errorPage;
	private final String activeSolutionName;
	private final ServoyModel servoyModel;


	public NewRelationWizard(String activeSolutionName)
	{
		super();
		this.activeSolutionName = activeSolutionName;
		servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
	}

	public NewRelationWizard()
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
			relationPage = null;
		}
		else
		{
			relationPage = new RelationNameSolutionPage("New relation creation");
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
			addPage(relationPage);
		}
	}

	@Override
	public boolean performFinish()
	{
		String[] params = relationPage.getSelectedItems();
		if (params != null)
		{
			if (params[0].equals("") == false && params[1].equals("") == false)
			{
				NewRelationAction.createRelation(params[0], params[1], null);
			}
		}
		return true;
	}


	public class RelationNameSolutionPage extends WizardPage implements Listener
	{

		private Combo solutionsCombo;
		private String[] currentSolutionNames;

		private String selectedSolutionName = ""; //$NON-NLS-1$
		private String selectedRelationName = ""; //$NON-NLS-1$
		private Text relationNameText;

		protected RelationNameSolutionPage(String pageName)
		{
			super(pageName);
			setTitle("Select the solution and set the relation name.");
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
			selectedRelationName = relationNameText.getText();
			return new String[] { selectedSolutionName, selectedRelationName };
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
			Label relationNameLabel = new Label(topLevel, SWT.NONE);
			relationNameLabel.setText("Relation name");

			relationNameText = new Text(topLevel, SWT.BORDER);
			relationNameText.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent event)
				{
					selectedRelationName = relationNameText.getText();
				}
			});

			relationNameText.addModifyListener(new ModifyListener()
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
			formData.top = new FormAttachment(relationNameText, 0, SWT.CENTER);
			relationNameLabel.setLayoutData(formData);

			//text
			formData = new FormData();
			formData.left = new FormAttachment(solutionsCombo, 0, SWT.LEFT);
			formData.top = new FormAttachment(solutionsCombo, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			relationNameText.setLayoutData(formData);

		}

		@Override
		public void setVisible(boolean visible)
		{
			super.setVisible(visible);

			if (visible)
			{
				setPageComplete(validatePage());
				//validatePage();
			}
		}

		protected String relationExists(String relName, String solName)
		{
			ServoyProject project = servoyModel.getServoyProject(solName);

			ValidatorSearchContext validatorSearchContext = new ValidatorSearchContext(project.getEditingSolution(), IRepository.RELATIONS);
			IValidateName validator = servoyModel.getNameValidator();
			try
			{
				validator.checkName(relName, 0, validatorSearchContext, false);
			}
			catch (RepositoryException e)
			{
				return e.getMessage();
			}

			return "";
		}

		protected boolean validatePage()
		{
			String error = null;
			if (relationNameText.getText().trim().length() == 0)
			{
				error = "Enter a relation name";
			}
			else if (!IdentDocumentValidator.isJavaIdentifier(relationNameText.getText()))
			{
				error = "Invalid relation name";
			}
			else
			{
				String relName = relationNameText.getText();
				String solName = solutionsCombo.getItem(solutionsCombo.getSelectionIndex());

				String message = relationExists(relName, solName);
				if (message.equals("") == false)
				{
					error = "Name error encountered due to following reason: " + message;
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
