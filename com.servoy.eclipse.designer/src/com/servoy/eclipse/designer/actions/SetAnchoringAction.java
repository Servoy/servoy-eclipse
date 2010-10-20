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

import org.eclipse.gef.EditPart;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.j2db.util.IAnchorConstants;

/**
 * Action to set Anchoring to specific value.
 * 
 * @author rgansevles
 *
 */
public class SetAnchoringAction extends SetPropertyAction
{
	public SetAnchoringAction(IWorkbenchPart part, EditPart editPart, int anchors)
	{
		super(part, AS_PUSH_BUTTON, editPart, "anchors", "set anchoring", Integer.valueOf(anchors));
		init(anchors);
	}

	protected void init(int anchors)
	{
		String text = "Anchor ";
		if ((anchors & IAnchorConstants.NORTH) != 0)
		{
			text = extendText(text, "Top");
		}
		if ((anchors & IAnchorConstants.EAST) != 0)
		{
			text = extendText(text, "Right");
		}
		if ((anchors & IAnchorConstants.SOUTH) != 0)
		{
			text = extendText(text, "Bottom");
		}
		if ((anchors & IAnchorConstants.WEST) != 0)
		{
			text = extendText(text, "Left");
		}
		setText(text);
		// TODO: set image
	}

	private static String extendText(String text, String string)
	{
		if (text.endsWith(" "))
		{
			return text + string;
		}
		return text + "," + string;
	}

}
