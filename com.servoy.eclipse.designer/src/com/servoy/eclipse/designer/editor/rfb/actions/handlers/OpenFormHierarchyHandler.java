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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.views.solutionexplorer.FormHierarchyView;
import com.servoy.j2db.persistence.IPersist;

/**
 * Open the Form Hierarchy View.
 * @author emera
 */
public class OpenFormHierarchyHandler implements IServerService
{
	private final ISelectionProvider selectionProvider;

	public OpenFormHierarchyHandler(ISelectionProvider selectionProvider)
	{
		this.selectionProvider = selectionProvider;
	}

	@Override
	public Object executeMethod(String methodName, JSONObject args) throws Exception
	{
		IStructuredSelection select = (IStructuredSelection)selectionProvider.getSelection();
		FormHierarchyView view = (FormHierarchyView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(FormHierarchyView.ID);
		IPersist persist = select.getFirstElement() instanceof PersistContext ? ((PersistContext)select.getFirstElement()).getPersist()
			: (IPersist)select.getFirstElement();
		view.setSelection(persist);
		return null;
	}

}
