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


import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.util.UIUtils.ExtendedInputDialog;
import com.servoy.eclipse.core.util.UIUtils.InputAndListDialog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.util.IdentDocumentValidator;

/**
 * Action for duplicating the selected persist(s).
 * 
 * @author Andrei Costescu
 */
public class DuplicatePersistAction extends AbstractMovePersistAction
{

	/**
	 * Creates a new "duplicate form" action.
	 */
	public DuplicatePersistAction(Shell shell)
	{
		super(shell);
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("duplicate.gif"));
		setText(Messages.DuplicateFormAction_duplicateForm);
		setToolTipText(Messages.DuplicateFormAction_duplicateForm);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		super.selectionChanged(event);
		setText("Duplicate " + persistString);
		setToolTipText("Duplicates the " + persistString + " to a different solution/module");
	}

	@Override
	protected ExtendedInputDialog createDialog(final IPersist persist, final IValidateName nameValidator, String[] solutionNames, int initialSolutionIndex)
	{
		String newName = null;
		String oldName = ((ISupportName)persist).getName();
		if (persist instanceof Media) newName = "copy_" + oldName;
		else newName = oldName + "_copy";
		// prepare dialog
		InputAndListDialog dialog = new InputAndListDialog(shell, "Duplicate " + persistString + ((ISupportName)persist).getName(), "Name of the duplicated " +
			persistString + ": ", newName, new IInputValidator()
		{
			public String isValid(String newText)
			{
				String message = null;
				String checkText = newText;
				if (persist instanceof Media) checkText = checkText.replace(".", "");
				message = IdentDocumentValidator.isJavaIdentifier(checkText) ? null : (newText.length() == 0 ? "" : "Invalid name");
				if (message == null)
				{
					try
					{
						nameValidator.checkName(newText, -1, new ValidatorSearchContext(getPersistType()), false);
					}
					catch (RepositoryException e)
					{
						message = e.getMessage();
						if (message == null) message = "Invalid name";
					}
				}
				return message;
			}

		}, solutionNames, "Please select the destination solution:", initialSolutionIndex)
		{
			@Override
			protected void validateInput()
			{
				super.validateInput();
				if (getExtendedValue() == null)
				{
					setErrorMessage("Select a module");
				}
			}
		};
		return dialog;
	}

	/**
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.AbstractMovePersistAction#doWork(com.servoy.j2db.persistence.Form, java.lang.Object[],
	 *      com.servoy.j2db.persistence.IValidateName)
	 */
	@Override
	protected void doWork(IPersist persist, Location location, IValidateName nameValidator) throws RepositoryException
	{
		IPersist duplicate = intelligentClonePersist(persist, location.getPersistName(), location.getServoyProject(), nameValidator);
		EditorUtil.openPersistEditor(duplicate);
	}
}