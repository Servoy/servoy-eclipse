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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.eclipse.ui.wizards.LoadRelationsWizard;
import com.servoy.j2db.persistence.IServerInternal;

public class LoadRelationsAction extends Action implements ISelectionChangedListener
{

	private final SolutionExplorerView viewer;

	/**
	 * Creates a new "duplicate form" action.
	 */
	public LoadRelationsAction(SolutionExplorerView sev)
	{
		viewer = sev;
		setText("Load relations");
		setToolTipText("Load relations");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = false;
		if (sel.size() == 1)
		{
			SimpleUserNode node = (SimpleUserNode)sel.getFirstElement();
			if (node.getRealObject() instanceof IServerInternal)
			{
				IServerInternal s = (IServerInternal)node.getRealObject();
				if (s.isValid() && s.getConfig().isEnabled() && ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != null)
				{
					state = true;
				}
			}
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node.getRealObject() instanceof IServerInternal)
		{
			show();
		}
	}

	private void show()
	{
		LoadRelationsWizard loadRelationsWizard = new LoadRelationsWizard();

		IStructuredSelection selection = StructuredSelection.EMPTY;
		loadRelationsWizard.init(PlatformUI.getWorkbench(), viewer.getTreeSelection());

		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), loadRelationsWizard);
		dialog.create();
		dialog.open();
	}
}
