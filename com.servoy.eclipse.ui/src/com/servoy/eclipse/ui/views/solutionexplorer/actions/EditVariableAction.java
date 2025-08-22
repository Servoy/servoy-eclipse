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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IViewPart;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewVariableAction.VariableEditDialog;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * An action that is able to edit variables.
 *
 * @author acostescu
 */
public class EditVariableAction extends Action implements ISelectionChangedListener
{

	private ScriptVariable variable = null;
	private final IViewPart viewPart;

	/**
	 * Creates a new edit variable action that will use the given shell to show the edit variable dialog.
	 *
	 * @param shell used to show a dialog.
	 */
	public EditVariableAction(IViewPart viewPart)
	{
		this.viewPart = viewPart;

		setText(Messages.EditVariableAction_editVariable);
		setToolTipText(Messages.EditVariableAction_editVariable);
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		variable = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean ok = (sel.size() == 1);
		if (ok)
		{
			SimpleUserNode un = (SimpleUserNode)sel.getFirstElement();
			if (un.getType() == UserNodeType.FORM_VARIABLE_ITEM || un.getType() == UserNodeType.GLOBAL_VARIABLE_ITEM)
			{
				ok = true;
				variable = (ScriptVariable)un.getRealObject();
			}
			else
			{
				ok = false;
				variable = null;
			}
		}

		setEnabled(ok);
	}

	@Override
	public void run()
	{
		if (variable != null)
		{
			if (DeleteScriptAction.scriptsAreEditedByEditors(new IPersist[] { variable }, viewPart.getSite().getPage().getDirtyEditors()))
			{
				MessageDialog.openWarning(viewPart.getSite().getShell(), "Cannot delete",
					"There are unsaved open editors that would be affected by this edit.\nPlease save or discard changes in these editors first.");
			}
			else
			{
				// show the edit variable dialog
				IDeveloperServoyModel sm = ServoyModelManager.getServoyModelManager().getServoyModel();
				final IValidateName nameValidator = sm.getNameValidator();
				VariableEditDialog askUserDialog = new VariableEditDialog(viewPart.getSite().getShell(), "Editing variable '" + variable.getName() + "'",
					new IInputValidator()
					{
						public String isValid(String newText)
						{
							String message = null;
							if (newText.length() == 0)
							{
								message = "";
							}
							else if (!IdentDocumentValidator.isJavaIdentifier(newText))
						{
							message = "Invalid variable name";
						}
							else if (!newText.equals(variable.getName()))
						{
							try
							{
								nameValidator.checkName(newText, null, new ValidatorSearchContext(
									variable.getScopeName() != null ? variable.getScopeName() : variable.getParent(), IRepository.SCRIPTVARIABLES), false);
							}
							catch (RepositoryException e)
							{
								message = e.getMessage();
							}
						}
							return message;
						}
					}, variable.getName(), Column.mapToDefaultType(variable.getDataProviderType()), variable.getDefaultValue());
				askUserDialog.open();
				if (askUserDialog.getVariableName() != null && (variable.getName() != askUserDialog.getVariableName() ||
					variable.getDataProviderType() != askUserDialog.getVariableType() || variable.getDefaultValue() != askUserDialog.getVariableDefaultValue()))
				{
					Solution rootObject = (Solution)variable.getRootObject();

					ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(rootObject.getName());
					try
					{
						ScriptVariable editingNode = (ScriptVariable)servoyProject.getEditingPersist(variable.getUUID());
						editingNode.setDefaultValue(askUserDialog.getVariableDefaultValue());
						editingNode.updateName(nameValidator, askUserDialog.getVariableName());
						editingNode.setVariableType(askUserDialog.getVariableType());
						servoyProject.saveEditingSolutionNodes(new IPersist[] { editingNode }, true);
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}
	}
}
