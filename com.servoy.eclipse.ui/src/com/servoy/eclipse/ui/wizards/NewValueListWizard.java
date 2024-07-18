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

import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValueList;

public class NewValueListWizard extends Wizard implements INewWizard
{
	public static final String ID = "com.servoy.eclipse.ui.NewValueListWizard";

	private NewValuelistWizardPage valuelistPage;
	private WizardPage errorPage;
	private final String activeSolutionName;
	private final IDeveloperServoyModel servoyModel;

	private ValueList valuelist;


	public NewValueListWizard(String activeSolutionName)
	{
		super();
		this.activeSolutionName = activeSolutionName;
		servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
	}

	public NewValueListWizard()
	{
		super();
		servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		this.activeSolutionName = servoyModel.getActiveProject().getSolution().getName();
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
			valuelistPage = new NewValuelistWizardPage("New valuelist creation", servoyModel, activeSolutionName);
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
	public void createPageControls(Composite pageContainer)
	{
		pageContainer.getShell().setData(CSSSWTConstants.CSS_ID_KEY, "svydialog");
		super.createPageControls(pageContainer);
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
				valuelist = createValueList(params[1], editingSolution);
				if (valuelist == null)
				{
					return false;
				}
			}
		}
		return true;
	}

	public ValueList createValueList(String valueListName, Solution editingSolution)
	{
		try
		{
			IValidateName validator = servoyModel.getNameValidator();
			ValueList vl = editingSolution.createNewValueList(validator, valueListName);
			vl.setAddEmptyValue(IValueListConstants.EMPTY_VALUE_NEVER);
			EditorUtil.openValueListEditor(vl);
			return vl;
		}
		catch (RepositoryException e)
		{
			MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Error", e.getMessage());
		}
		return null;
	}

	public ValueList getCreatedValueList()
	{
		return valuelist;
	}
}
