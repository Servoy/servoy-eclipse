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
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseMessages;

public class I18NReadFromDBAction extends Action
{
	/**
	 * Creates a new open action that uses the given solution view.
	 * 
	 * @param sev the solution view to use.
	 */
	public I18NReadFromDBAction()
	{
		setText("Read from DB"); //$NON-NLS-1$
		setToolTipText("Read messages files from the i18n database tables"); //$NON-NLS-1$
	}

	@Override
	public void run()
	{
		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		if (activeProject != null)
		{


			MessageDialogWithToggle dlg = MessageDialogWithToggle.open(MessageDialog.CONFIRM, UIUtils.getActiveShell(), "Read I18N from DB",
				"This will insert new and replace exiting keys from the database into the workspace.",
				"Delete keys from workspace that are not in the database", false, null, null, SWT.NONE);
			if (dlg.getReturnCode() == Window.OK)
			{
				boolean deleteNonExistingKeys = dlg.getToggleState();
				EclipseMessages.writeProjectI18NFiles(activeProject, true, deleteNonExistingKeys);
			}
		}
	}
}
