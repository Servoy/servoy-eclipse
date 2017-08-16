/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

import org.eclipse.dltk.internal.ui.editor.ScriptEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.FormHierarchyView;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.Debug;

/**
 * Used to open the form hierarchy view from the script editor.
 * @author emera
 */
public class OpenFormHierarchyAction implements IEditorActionDelegate
{
	private ScriptEditor editorPart;

	@Override
	public void run(IAction action)
	{
		StyledText st = (StyledText)editorPart.getAdapter(Control.class);
		Form form = EditorUtil.getForm(editorPart);
		String text = null;
		if (editorPart.getElementAt(st.getCaretOffset()) != null)
		{
			text = editorPart.getElementAt(st.getCaretOffset()).getElementName();
		}
		IPersist persist = text != null ? form.getScriptMethod(text) : form;
		if (persist != null)
		{
			try
			{
				FormHierarchyView view = (FormHierarchyView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(FormHierarchyView.ID);
				view.open(persist);
			}
			catch (PartInitException e)
			{
				Debug.error(e);
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection)
	{
	}

	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor)
	{
		editorPart = targetEditor instanceof ScriptEditor ? (ScriptEditor)targetEditor : null;
		if (editorPart != null)
		{
			action.setEnabled(EditorUtil.getForm(editorPart) != null);
		}
		else
		{
			action.setEnabled(false);
		}
	}

}
