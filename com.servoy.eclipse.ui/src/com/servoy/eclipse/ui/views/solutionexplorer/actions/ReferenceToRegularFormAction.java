/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * @author gboros
 *
 */
public class ReferenceToRegularFormAction extends Action
{
	private final SolutionExplorerView viewer;

	public ReferenceToRegularFormAction(SolutionExplorerView viewer)
	{
		this.viewer = viewer;
		setText("Transform to regular form");
		setToolTipText("Transform reference form to regular form");
	}

	@Override
	public boolean isEnabled()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node == null || !(node.getRealObject() instanceof Form)) return false;
		return (node.getRealObject() instanceof Form) && ((Form)node.getRealObject()).getReferenceForm().booleanValue();
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node == null || !(node.getRealObject() instanceof Form)) return;

		Form form = (Form)node.getRealObject();
		ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(form.getSolution().getName());
		Form editingForm = (Form)servoyProject.getEditingPersist(form.getUUID());
		editingForm.setReferenceForm(Boolean.FALSE);

		try
		{
			servoyProject.saveEditingSolutionNodes(new IPersist[] { editingForm }, true, false);
		}
		catch (RepositoryException ex)
		{
			ServoyLog.logError(ex);
		}
	}
}
