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
import org.eclipse.gef.Request;
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
		super(part, null);
		this.sameWidth = sameWidth;
	}

	@Override
	protected Map<EditPart, Request> createRequests(List<EditPart> selected)
	{
		return createSameSizeRequests(sameWidth, selected);
	}

	public static Map<EditPart, Request> createSameSizeRequests(boolean sameWidth, List<EditPart> selected)
	{
		if (selected == null)
		{
			return null;
		}

		Rectangle bounds = null;

		Map<EditPart, Request> requests = null;
		for (EditPart editPart : selected)
		{
			if (editPart instanceof GraphicalEditPart)
			{
				if (bounds == null)
				{
					// match with first
					bounds = ((GraphicalEditPart)editPart).getFigure().getBounds();
				}
				else
				{
					// second or more
					if (requests == null)
					{
						requests = new HashMap<EditPart, Request>(selected.size());
					}
					// set new size
					Dimension value;
					if (sameWidth)
					{
						value = new Dimension(bounds.width, ((GraphicalEditPart)editPart).getFigure().getBounds().height);
					}
					else
					{
						value = new Dimension(((GraphicalEditPart)editPart).getFigure().getBounds().width, bounds.height);
					}
					requests.put(editPart, new SetPropertyRequest(VisualFormEditor.REQ_SET_PROPERTY, "size", value, "set same " +
						(sameWidth ? "width" : "height")));
				}
			}
		}

		return requests;
	}
}
