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

import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.ui.property.AnchorPropertyController.AnchorPropertySource;

/**
 * An action to change the anchoring of selected objects.
 */
public class ToggleAnchoringAction extends ToggleCheckboxAction
{
	public ToggleAnchoringAction(IWorkbenchPart part, String anchoringProperty)
	{
		super(part, "anchors." + anchoringProperty, "toggle anchoring");
		init(anchoringProperty);
	}

	/**
	 * Initializes this action's text and images.
	 */
	protected void init(String anchoringProperty)
	{
		if (AnchorPropertySource.TOP.equals(anchoringProperty))
		{
			setText(DesignerActionFactory.ANCHOR_TOP_TOGGLE_TEXT);
			setToolTipText(DesignerActionFactory.ANCHOR_TOP_TOGGLE_TOOLTIP);
			setId(DesignerActionFactory.ANCHOR_TOP_TOGGLE.getId());
		}
		if (AnchorPropertySource.RIGHT.equals(anchoringProperty))
		{
			setText(DesignerActionFactory.ANCHOR_RIGHT_TOGGLE_TEXT);
			setToolTipText(DesignerActionFactory.ANCHOR_RIGHT_TOGGLE_TOOLTIP);
			setId(DesignerActionFactory.ANCHOR_RIGHT_TOGGLE.getId());
		}
		if (AnchorPropertySource.BOTTOM.equals(anchoringProperty))
		{
			setText(DesignerActionFactory.ANCHOR_BOTTOM_TOGGLE_TEXT);
			setToolTipText(DesignerActionFactory.ANCHOR_BOTTOM_TOGGLE_TOOLTIP);
			setId(DesignerActionFactory.ANCHOR_BOTTOM_TOGGLE.getId());
		}
		if (AnchorPropertySource.LEFT.equals(anchoringProperty))
		{
			setText(DesignerActionFactory.ANCHOR_LEFT_TOGGLE_TEXT);
			setToolTipText(DesignerActionFactory.ANCHOR_LEFT_TOGGLE_TOOLTIP);
			setId(DesignerActionFactory.ANCHOR_LEFT_TOGGLE.getId());
		}
	}
}
