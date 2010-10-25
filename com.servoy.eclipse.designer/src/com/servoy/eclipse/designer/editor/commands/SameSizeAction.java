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

import java.awt.Dimension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.designer.actions.SetPropertyRequest;
import com.servoy.eclipse.designer.editor.VisualFormEditor;

/**
 * Base action to set the same width or height to selected elements.
 * 
 * @author rgansevles
 *
 */
public abstract class SameSizeAction extends DesignerSelectionAction
{
	private final boolean sameWidth;

	public SameSizeAction(IWorkbenchPart part, boolean sameWidth)
	{
		super(part, VisualFormEditor.REQ_SET_PROPERTY);
		this.sameWidth = sameWidth;
	}

	@Override
	protected GroupRequest createRequest(List<EditPart> selected)
	{
		if (selected == null || selected.size() < 2)
		{
			return null;
		}

		EditPart first = selected.get(0);
		if (!(first instanceof GraphicalEditPart))
		{
			return null;
		}

		Rectangle bounds = ((GraphicalEditPart)first).getFigure().getBounds();

		Map<EditPart, Object> values = new HashMap<EditPart, Object>(selected.size());
		for (EditPart editPart : selected)
		{
			if (editPart instanceof GraphicalEditPart)
			{
				// set new size
				values.put(editPart, sameWidth ? new Dimension(bounds.width, ((GraphicalEditPart)editPart).getFigure().getBounds().height) : new Dimension(
					((GraphicalEditPart)editPart).getFigure().getBounds().width, bounds.height));
			}
		}

		SetPropertyRequest setPropertyRequest = new SetPropertyRequest(requestType, "size", values, "set same " + (sameWidth ? "width" : "height"));
		setPropertyRequest.setEditParts(selected);
		return setPropertyRequest;
	}
}
