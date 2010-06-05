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
package com.servoy.eclipse.designer.editor.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.wizards.NewFormWizard;
import com.servoy.eclipse.ui.wizards.NewMethodWizard;
import com.servoy.eclipse.ui.wizards.NewRelationWizard;
import com.servoy.eclipse.ui.wizards.NewValueListWizard;
import com.servoy.j2db.persistence.Solution;

public class NewWizardCommandHandler extends AbstractHandler
{

	public Object execute(ExecutionEvent arg0) throws ExecutionException
	{
		final String NEW_FORM = "NEW_FORM";
		final String NEW_RELATION = "NEW_RELATION";
		final String NEW_VALUELIST = "NEW_VALUELIST";
		final String NEW_METHOD = "NEW_METHOD";


		NewFormWizard newFormWizard = new NewFormWizard();

		String wizardType = arg0.getParameter("com.servoy.eclipse.designer.wizardType");
		if (wizardType != null)
		{
			if (wizardType.equals("NEW_FORM"))
			{
				openNewFormWizard();
			}
			else if (wizardType.equals("NEW_RELATION"))
			{
				openNewRelationWizard();
			}
			else if (wizardType.equals("NEW_VALUELIST"))
			{
				openNewValueListWizard();
			}
			else if (wizardType.equals("NEW_METHOD"))
			{
				openNewMethodWizard();
			}

		}

		return null;
	}

	private void openNewMethodWizard()
	{
		ISelection wselection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		String solutionName = "";
		if (wselection != null)
		{

			if (wselection instanceof TreeSelection)
			{
				TreeSelection treeSelection = (TreeSelection)wselection;
				Object firstElement = treeSelection.getFirstElement();
				if (firstElement instanceof SimpleUserNode)
				{
					SimpleUserNode simpleUserNode = (SimpleUserNode)firstElement;

					Solution solution = simpleUserNode.getSolution();

					if (solution != null)
					{
						solutionName = solution.getName();
					}
					else if (simpleUserNode.getRealObject() != null && simpleUserNode.getRealObject() instanceof ServoyProject)
					{
						ServoyProject servoyProject = (ServoyProject)simpleUserNode.getRealObject();
						Solution edSolution = servoyProject.getEditingSolution();
						solutionName = edSolution.getName();
					}
				}
			}
		}
		NewMethodWizard newMethodWizard = new NewMethodWizard(solutionName);

		IStructuredSelection selection = StructuredSelection.EMPTY;

		newMethodWizard.init(PlatformUI.getWorkbench(), selection);

		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), newMethodWizard);
		dialog.create();
		dialog.open();

	}

	private void openNewValueListWizard()
	{
		ISelection wselection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		String solutionName = "";
		if (wselection != null)
		{

			if (wselection instanceof TreeSelection)
			{
				TreeSelection treeSelection = (TreeSelection)wselection;
				Object firstElement = treeSelection.getFirstElement();
				if (firstElement instanceof SimpleUserNode)
				{
					SimpleUserNode simpleUserNode = (SimpleUserNode)firstElement;

					Solution solution = simpleUserNode.getSolution();

					if (solution != null)
					{
						solutionName = solution.getName();
					}
					else if (simpleUserNode.getRealObject() != null && simpleUserNode.getRealObject() instanceof ServoyProject)
					{
						ServoyProject servoyProject = (ServoyProject)simpleUserNode.getRealObject();
						Solution edSolution = servoyProject.getEditingSolution();
						solutionName = edSolution.getName();
					}


				}
			}
		}
		NewValueListWizard newValueListWizard = new NewValueListWizard(solutionName);

		IStructuredSelection selection = StructuredSelection.EMPTY;
		newValueListWizard.init(PlatformUI.getWorkbench(), selection);

		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), newValueListWizard);
		dialog.create();
		dialog.open();

	}

	private void openNewRelationWizard()
	{
		ISelection wselection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		String solutionName = "";
		if (wselection != null)
		{

			if (wselection instanceof TreeSelection)
			{
				TreeSelection treeSelection = (TreeSelection)wselection;
				Object firstElement = treeSelection.getFirstElement();
				if (firstElement instanceof SimpleUserNode)
				{
					SimpleUserNode simpleUserNode = (SimpleUserNode)firstElement;

					Solution solution = simpleUserNode.getSolution();

					if (solution != null)
					{
						solutionName = solution.getName();
					}
					else if (simpleUserNode.getRealObject() != null && simpleUserNode.getRealObject() instanceof ServoyProject)
					{
						ServoyProject servoyProject = (ServoyProject)simpleUserNode.getRealObject();
						Solution edSolution = servoyProject.getEditingSolution();
						solutionName = edSolution.getName();
					}


				}
			}
		}
		NewRelationWizard newRelationWizard = new NewRelationWizard(solutionName);

		IStructuredSelection selection = StructuredSelection.EMPTY;
		newRelationWizard.init(PlatformUI.getWorkbench(), selection);

		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), newRelationWizard);
		dialog.create();
		dialog.open();
	}

	private void openNewFormWizard()
	{
		NewFormWizard newFormWizard = new NewFormWizard();

		IStructuredSelection selection = StructuredSelection.EMPTY;
		newFormWizard.init(PlatformUI.getWorkbench(), selection);

		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), newFormWizard);
		dialog.create();
		dialog.open();

	}
}
