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
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportUpdateableName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

public class RenamePersistAction extends Action implements ISelectionChangedListener
{
	private IPersist persist;

	public RenamePersistAction()
	{

	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean enabled = false;
		if (sel.size() == 1 && (((SimpleUserNode)sel.getFirstElement()).getType() == UserNodeType.FORM))
		{
			SimpleUserNode node = ((SimpleUserNode)sel.getFirstElement());
			persist = (IPersist)node.getRealObject();
			enabled = true;
			if (persist instanceof Form && ((Form)persist).isFormComponent())
			{
				setText("Rename form component");
				setToolTipText("Rename form component");
			}
			else
			{
				setText("Rename form");
				setToolTipText("Rename form");
			}
		}
		setEnabled(enabled);
	}

	@Override
	public void run()
	{
		final Form form = (Form)persist;
		InputDialog nameDialog = new InputDialog(UIUtils.getActiveShell(), "Rename form", "Supply a new form name", form.getName(),
			new IInputValidator()
			{
				public String isValid(String newText)
				{
					return IdentDocumentValidator.isJavaIdentifier(newText) ? (newText.equals(form.getName()) ? "" : null)
						: (newText.length() == 0 ? "" : "Invalid form name");
				}
			});
		int res = nameDialog.open();
		if (res == Window.OK)
		{
			String name = nameDialog.getValue();
			ServoyProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(persist.getRootObject().getName());
			try
			{
				IPersist editingPersist = project.getEditingPersist(persist.getUUID());
				if (editingPersist instanceof ISupportUpdateableName)
				{
					((ISupportUpdateableName)editingPersist).updateName(ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(), name);
					project.saveEditingSolutionNodes(new IPersist[] { editingPersist }, true);
					if (form.isFormComponent().booleanValue())
					{
						ServoyModelFinder.getServoyModel().fireFormComponentChanged();
					}
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
				MessageDialog.openError(UIUtils.getActiveShell(), "Cannot rename", e.getMessage());
			}

		}
	}
}
