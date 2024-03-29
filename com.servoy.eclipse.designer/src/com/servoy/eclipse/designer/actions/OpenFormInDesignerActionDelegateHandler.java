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
package com.servoy.eclipse.designer.actions;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;

/**
 * Command handler form opening form in designer.
 *
 * Used for default command handler.
 *
 * @author rgansevles
 *
 */
public class OpenFormInDesignerActionDelegateHandler extends AbstractHandler
{
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		Form form = null;
		IWorkbenchPage activePage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
		ISelection selection = activePage.getSelection();
		if (selection != null && selection instanceof IStructuredSelection)
		{
			IStructuredSelection strucSelection = (IStructuredSelection)selection;
			Iterator< ? > iterator = strucSelection.iterator();
			while (iterator.hasNext())
			{
				Object element = iterator.next();
				if (element instanceof SimpleUserNode)
				{
					SimpleUserNode node = (SimpleUserNode)element;
					if (node.getRealObject() instanceof Form)
					{
						form = (Form)node.getRealObject();
					}
				}
			}
		}
		else
		{
			form = EditorUtil.getForm(activePage.getActiveEditor());
		}
		if (form != null)
		{
			return EditorUtil.openFormDesignEditor(form);
		}
		return null;
	}
}
