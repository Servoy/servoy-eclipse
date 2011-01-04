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

import com.servoy.eclipse.ui.property.AnchorPropertyController.AnchorPropertySource;

/**
 * An action to change the anchoring of selected objects.
 * 
 * @author rgansevles
 */
public abstract class ToggleAnchoringActionDelegateHandler extends ToggleCheckboxActionDelegateHandler
{
	public ToggleAnchoringActionDelegateHandler(String anchoringProperty)
	{
		super("anchors." + anchoringProperty, "toggle anchoring");
	}

	public static class ToggleAnchoringTop extends ToggleAnchoringActionDelegateHandler
	{
		public ToggleAnchoringTop()
		{
			super(AnchorPropertySource.TOP);
		}
	}
	public static class ToggleAnchoringRight extends ToggleAnchoringActionDelegateHandler
	{
		public ToggleAnchoringRight()
		{
			super(AnchorPropertySource.RIGHT);
		}
	}
	public static class ToggleAnchoringBottom extends ToggleAnchoringActionDelegateHandler
	{
		public ToggleAnchoringBottom()
		{
			super(AnchorPropertySource.BOTTOM);
		}
	}
	public static class ToggleAnchoringLeft extends ToggleAnchoringActionDelegateHandler
	{
		public ToggleAnchoringLeft()
		{
			super(AnchorPropertySource.LEFT);
		}
	}
}
