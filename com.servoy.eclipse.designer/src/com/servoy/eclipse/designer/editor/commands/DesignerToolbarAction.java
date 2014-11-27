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
package com.servoy.eclipse.designer.editor.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.designer.property.IPersistEditPart;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;

/**
 * Base class for actions on the form designer elements toolbar.
 *
 * @author rgansevles
 *
 */
public abstract class DesignerToolbarAction extends DesignerSelectionAction
{
	public DesignerToolbarAction(IWorkbenchPart part, Object requestType)
	{
		super(part, requestType);
	}

	@Override
	public boolean calculateEnabled()
	{
		return calculateEnabled(getSelectedObjects());
	}

	public boolean calculateEnabled(List< ? > selectedObjects)
	{
		return selectedObjects != null && selectedObjects.size() >= 1 && selectedObjects.get(0) instanceof EditPart &&
			getContext((EditPart)selectedObjects.get(0), IRepository.FORMS) != null;
	}

	/*
	 * When multiple objects are selected on a form, the AddFieldAction, AddMediaAction, AddPortalAction, AddTabPanelAction and AddSplitPaneAction should be
	 * executed once, no matter how many items are selected on the form.
	 *
	 * @see com.servoy.eclipse.designer.editor.commands.DesignerSelectionAction#createRequests(java.util.List)
	 */
	@Override
	protected Map<EditPart, Request> createRequests(List<EditPart> selected)
	{
		// TODO remove this workaround required by case SVY-7590
		if (selected != null && selected.size() > 0 && !(selected.get(0) instanceof EditPart)) return null;

		Map<EditPart, Request> requests = null;
		for (EditPart editPart : selected)
		{
			Request request = createRequest(editPart);
			if (request != null)
			{
				if (requests == null)
				{
					requests = new HashMap<EditPart, Request>(selected.size());
				}
				requests.put(editPart, request);
				break;
			}
		}
		return requests;
	}

	/**
	 * Get context model of selected type.
	 *
	 * @param editPart
	 * @param typeId
	 * @return
	 */
	protected IPersist getContext(EditPart editPart, int typeId)
	{
		// Follow editpart hierarchy, not persist hierarchy, in case of an inherited element persist.getParent() is the
		// super form but we are looking for the form that the editpart is shown in (the subform)
		EditPart ep = editPart;
		while (ep != null)
		{
			if (ep instanceof IPersistEditPart)
			{
				IPersist persist = ((IPersistEditPart)ep).getPersist();
				if (persist != null && persist.getTypeID() == typeId)
				{
					return persist;
				}
			}
			ep = ep.getParent();
		}

		return null;
	}

}
