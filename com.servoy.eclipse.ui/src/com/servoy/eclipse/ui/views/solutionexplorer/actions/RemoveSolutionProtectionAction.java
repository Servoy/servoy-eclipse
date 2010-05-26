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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ApplicationServerSingleton;

public class RemoveSolutionProtectionAction extends Action implements ISelectionChangedListener
{
	private final Shell shell;
	private ServoyProject selectedSolutionProject;

	public RemoveSolutionProtectionAction(Shell shell)
	{
		this.shell = shell;
		setText("Remove protection");
		setToolTipText(getText());
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		boolean enabled = true;
		ISelection sel = event.getSelection();
		if (sel instanceof IStructuredSelection)
		{
			IStructuredSelection s = (IStructuredSelection)sel;
			enabled = (s.size() == 1);
			if (enabled)
			{
				SimpleUserNode node = (SimpleUserNode)s.getFirstElement();
				UserNodeType type = node.getType();
				if (((type == UserNodeType.SOLUTION) || (type == UserNodeType.SOLUTION_ITEM)) && (node.getRealObject() instanceof ServoyProject))
				{
					selectedSolutionProject = (ServoyProject)node.getRealObject();
					if (selectedSolutionProject.getEditingSolution().getSolutionMetaData().isProtected())
					{
						enabled = false;
					}
				}
				else
				{
					enabled = false;
				}
			}
		}
		else
		{
			enabled = false;
		}
		if (!enabled)
		{
			selectedSolutionProject = null;
		}
		setEnabled(enabled);
	}

	@Override
	public void run()
	{
		if (selectedSolutionProject != null && !selectedSolutionProject.getEditingSolution().getSolutionMetaData().isProtected())
		{
			Solution solution = selectedSolutionProject.getEditingSolution();
			String inputPassword = UIUtils.showPasswordDialog(shell, "Solution '" + solution.getName() + "' is password protected",
				"Please enter protection password for solution : '" + solution.getName() + "'", "", null);
			inputPassword = ApplicationServerSingleton.get().calculateProtectionPasswordHash(solution.getName(), solution.getUUID().toString(), inputPassword);
			if (inputPassword.equals(solution.getProtectionPassword()))
			{
				solution.getSolutionMetaData().setProtectionPassword(
					ApplicationServerSingleton.get().calculateProtectionPasswordHash(solution.getName(), solution.getUUID().toString(), null));
				try
				{
					selectedSolutionProject.saveEditingSolutionNodes(new IPersist[] { solution }, false);
					setEnabled(false);
				}
				catch (RepositoryException ex)
				{
					MessageDialog.openError(shell, "Writing on disk failed", ex.getMessage());
				}
			}
			else
			{
				MessageDialog.openError(shell, "Wrong Password", "Password is not correct.");
			}

		}
	}
}
