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
package com.servoy.eclipse.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;

/**
 * Action for showing form designer in javascript editor.
 * 
 * @author jcompagner
 */

public class ShowDesignerAction implements IEditorActionDelegate
{
	public void run(IAction action)
	{
		Form form = EditorUtil.getForm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor());
		if (form != null)
		{
			EditorUtil.openFormDesignEditor(form);
		}
	}

	public void selectionChanged(IAction action, ISelection selection)
	{
	}

	/**
	 * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor)
	{
		if (targetEditor != null)
		{
			action.setEnabled(EditorUtil.getForm(targetEditor) != null);
		}
		else
		{
			action.setEnabled(false);
		}
	}
}
