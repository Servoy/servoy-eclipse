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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.designer.actions.SetPropertyRequest;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.GroupGraphicalEditPart;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * An action to ungroup selected objects.
 */
public class UngroupAction extends DesignerSelectionAction
{
	public UngroupAction(IWorkbenchPart part)
	{
		super(part, null);
	}

	/**
	 * Initializes this action's text and images.
	 */
	@Override
	protected void init()
	{
		super.init();
		setText(DesignerActionFactory.UNGROUP_TEXT);
		setToolTipText(DesignerActionFactory.UNGROUP_TOOLTIP);
		setId(DesignerActionFactory.UNGROUP.getId());
		setImageDescriptor(DesignerActionFactory.UNGROUP_IMAGE);
	}

	@Override
	protected Map<EditPart, Request> createRequests(List<EditPart> selected)
	{
		return createUngroupingRequests(selected);
	}

	protected static Map<EditPart, Request> createUngroupingRequests(List<EditPart> selected)
	{
		if (selected == null) return null;
		// TODO remove this workaround required by case SVY-7590
		if (selected.size() > 0 && !(selected.get(0) instanceof EditPart)) return null;

		List<EditPart> children = new ArrayList<EditPart>();

		for (EditPart editPart : selected)
		{
			if (editPart instanceof GroupGraphicalEditPart)
			{
				children.addAll(editPart.getChildren());
			}
		}

		Map<EditPart, Request> requests = null;
		for (EditPart child : children)
		{
			if (requests == null)
			{
				requests = new HashMap<EditPart, Request>(selected.size());
			}
			requests.put(child, new SetPropertyRequest(BaseVisualFormEditor.REQ_SET_PROPERTY, StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName(), null,
				"ungroup"));
		}

		return requests;
	}

}
