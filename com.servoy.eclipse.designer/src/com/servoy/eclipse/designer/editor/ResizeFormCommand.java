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

package com.servoy.eclipse.designer.editor;

import java.util.List;

import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;

import com.servoy.eclipse.designer.editor.commands.ChangeBoundsCommand;
import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * Command to resize a form in form editor.
 * Move/resize elements also when control is pressed.
 *
 * @since 6.0
 *
 * @author rgansevles
 *
 */
public class ResizeFormCommand extends Command
{
	private static final String PROPERTY_WIDTH = StaticContentSpecLoader.PROPERTY_WIDTH.getPropertyName();

	private final int resizeDirection;
	private final int delta;
	private final boolean controlPressed;
	private CompoundCommand resizeCommand;

	private final FormGraphicalEditPart formEditPart;

	/**
	 * @param formEditPart
	 * @param resizeDirection
	 * @param sizeDelta
	 * @param controlPressed
	 */
	public ResizeFormCommand(FormGraphicalEditPart formEditPart, int resizeDirection, int delta, boolean controlPressed)
	{
		this.formEditPart = formEditPart;
		this.resizeDirection = resizeDirection;
		this.delta = delta;
		this.controlPressed = controlPressed;
	}

	@Override
	public void execute()
	{
		Form form = (Form)formEditPart.getModel();

		resizeCommand = new CompoundCommand();
		if (delta != 0)
		{
			// resize form
			resizeCommand.add(SetValueCommand.createSetvalueCommand("Resize form", PersistPropertySource.createPersistPropertySource(form, form, false),
				PROPERTY_WIDTH, new Integer(form.getWidth() + delta)));

			// move/resize all right-anchored elements, when control is pressed
			List< ? extends GraphicalEditPart> children = formEditPart.getChildren();
			for (EditPart editPart : children)
			{
				Object model = editPart.getModel();
				if (model instanceof Part || !(model instanceof IPersist))
				{
					continue;
				}
				int anchors = (model instanceof ISupportAnchors) ? ((ISupportAnchors)model).getAnchors() : 0;

				boolean resize = false;
				boolean move = controlPressed && (resizeDirection & PositionConstants.EAST) != 0 && (anchors & IAnchorConstants.EAST) != 0;
				if (move)
				{
					// resize right side of form and control is pressed, move/resize anchored elements
					resize = (anchors & IAnchorConstants.WEST) != 0;
				}
				else
				{
					move = !controlPressed && (resizeDirection & PositionConstants.WEST) != 0;
					// resize left side of form and control is NOT pressed, move/resize anchored elements
					resize = move && (anchors & IAnchorConstants.EAST) != 0;
				}
				if (resize)
				{
					resizeCommand.add(new ChangeBoundsCommand(editPart, null, new Dimension(delta, 0)));
				}
				else if (move)
				{
					resizeCommand.add(new ChangeBoundsCommand(editPart, new Point(delta, 0), null));
				}
			}
		}

		resizeCommand.execute();
	}

	@Override
	public boolean canUndo()
	{
		return resizeCommand != null && resizeCommand.canUndo();
	}

	@Override
	public void undo()
	{
		resizeCommand.undo();
	}
}
