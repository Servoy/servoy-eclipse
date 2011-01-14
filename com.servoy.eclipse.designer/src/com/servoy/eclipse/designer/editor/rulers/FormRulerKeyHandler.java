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

package com.servoy.eclipse.designer.editor.rulers;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.internal.ui.rulers.GuideEditPart;
import org.eclipse.gef.internal.ui.rulers.RulerEditPart;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;

/**
 * Key handler when form ruler has focus.
 * 
 * @author rgansevles
 *
 */
public class FormRulerKeyHandler extends GraphicalViewerKeyHandler
{
	/**
	 * Constructor
	 * 
	 * @param viewer
	 *            The viewer for which this handler processes keyboard
	 *            input
	 */
	public FormRulerKeyHandler(GraphicalViewer viewer)
	{
		super(viewer);
	}

	/**
	 * @see org.eclipse.gef.KeyHandler#keyPressed(org.eclipse.swt.events.KeyEvent)
	 */
	@Override
	public boolean keyPressed(KeyEvent event)
	{
		if (event.keyCode == SWT.DEL)
		{
			// If a guide has focus, delete it
			if (getFocusEditPart() instanceof GuideEditPart)
			{
				RulerEditPart parent = (RulerEditPart)getFocusEditPart().getParent();
				Command deleteGuideCommand = parent.getRulerProvider().getDeleteGuideCommand(getFocusEditPart().getModel());
				if (deleteGuideCommand != null && deleteGuideCommand.canExecute())
				{
					deleteGuideCommand.execute();
				}
				event.doit = false;
				return true;
			}
			return false;
		}
		else if (event.stateMask == 0 && getFocusEditPart() instanceof GuideEditPart &&
			(event.keyCode == SWT.ARROW_LEFT || event.keyCode == SWT.ARROW_RIGHT || event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN))
		{
			// move the ruler 1 position
			RulerEditPart parent = (RulerEditPart)getFocusEditPart().getParent();
			Command moveGuideCommand = parent.getRulerProvider().getMoveGuideCommand(getFocusEditPart().getModel(),
				(event.keyCode == SWT.ARROW_LEFT || event.keyCode == SWT.ARROW_UP) ? -1 : 1);
			if (moveGuideCommand != null && moveGuideCommand.canExecute())
			{
				moveGuideCommand.execute();
			}
			event.doit = false;
			return true;
		}
		else if (((event.stateMask & SWT.ALT) != 0) && (event.keyCode == SWT.ARROW_UP))
		{
			// ALT + UP_ARROW pressed
			// If a guide has focus, give focus to the ruler
			EditPart parent = getFocusEditPart().getParent();
			if (parent instanceof RulerEditPart) navigateTo(getFocusEditPart().getParent(), event);
			return true;
		}
		return super.keyPressed(event);
	}
}