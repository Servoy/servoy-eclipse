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

import java.util.List;

import org.eclipse.gef.EditPart;
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
		return selectedObjects != null && selectedObjects.size() == 1 && selectedObjects.get(0) instanceof EditPart &&
			getModel((EditPart)selectedObjects.get(0), IRepository.FORMS) != null;
	}

	protected IPersist getModel(EditPart editPart, int typeId)
	{
		EditPart ep = editPart;
		while (!(ep instanceof IPersistEditPart))
		{
			if (ep == null)
			{
				return null;

			}
			ep = ep.getParent();
		}
		IPersist persist = ((IPersistEditPart)ep).getPersist();
		if (persist == null)
		{
			return null;
		}
		return persist.getAncestor(typeId);
	}

}
