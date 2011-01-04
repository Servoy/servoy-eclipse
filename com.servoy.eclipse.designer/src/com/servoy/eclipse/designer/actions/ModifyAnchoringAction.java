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

import com.servoy.eclipse.ui.property.AnchorPropertyController.AnchorPropertySource;

/**
 * Action to turn anchoring on/off for 1 direction.
 * 
 * @author rgansevles
 *
 */
public class ModifyAnchoringAction extends SetPropertyAction
{
	public ModifyAnchoringAction(IWorkbenchPart part, EditPart editPart, String anchoringProperty)
	{
		super(part, AS_CHECK_BOX, editPart, "anchors." + anchoringProperty, "toggle anchoring", null);
		init(anchoringProperty);
	}

	@Override
	protected Object getNewValue()
	{
		return Boolean.valueOf(!isChecked());
	}

	protected void init(String anchoringProperty)
	{
		if (AnchorPropertySource.TOP.equals(anchoringProperty))
		{
			setText("Top");
			setToolTipText("Anchor " + getText());
		}
		if (AnchorPropertySource.RIGHT.equals(anchoringProperty))
		{
			setText("Right");
			setToolTipText("Anchor " + getText());
		}
		if (AnchorPropertySource.BOTTOM.equals(anchoringProperty))
		{
			setText("Bottom");
			setToolTipText("Anchor " + getText());
		}
		if (AnchorPropertySource.LEFT.equals(anchoringProperty))
		{
			setText("Left");
			setToolTipText("Anchor " + getText());
		}
	}

}
