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
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.eclipse.ui.wizards.SuggestForeignTypesWizard;
import com.servoy.j2db.persistence.IServerInternal;

public class SuggestForeignTypesAction extends Action implements ISelectionChangedListener
{

	private final SolutionExplorerView viewer;

	public SuggestForeignTypesAction(SolutionExplorerView sev)
	{
		viewer = sev;

		setText("Suggest Foreign Types"); //$NON-NLS-1$
		setToolTipText("Suggest Foreign Types"); //$NON-NLS-1$
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			state = type == UserNodeType.SERVER;
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node.getRealObject() instanceof IServerInternal)
		{
			IServerInternal s = (IServerInternal)node.getRealObject();
			SuggestForeignTypesWizard suggestForeignTypesWizard = new SuggestForeignTypesWizard(s.getName());
			suggestForeignTypesWizard.init(PlatformUI.getWorkbench(), StructuredSelection.EMPTY);
			WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), suggestForeignTypesWizard);
			dialog.create();
			dialog.open();
		}
	}

	@Override
	public boolean isEnabled()
	{
		// if not active solution then it is not enabled
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() == null)
		{
			return false;
		}

		// if the server is not enabled or invalid
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node.getRealObject() instanceof IServerInternal)
		{
			IServerInternal s = (IServerInternal)node.getRealObject();
			if (!s.getConfig().isEnabled() || !s.isValid()) return false;
		}

		return super.isEnabled();
	}
}
