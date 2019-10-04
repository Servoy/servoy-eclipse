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

import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.ui.property.AnchorPropertyController.AnchorPropertySource;
import com.servoy.j2db.persistence.IAnchorConstants;

/**
 * An action to change the anchoring of selected objects.
 *
 * @author rgansevles
 */
public abstract class ToggleAnchoringActionDelegateHandler extends ToggleCheckboxActionDelegateHandler
{
	public ToggleAnchoringActionDelegateHandler(String anchoringProperty, int flag)
	{
		super("anchors." + anchoringProperty, "toggle anchoring", flag);
	}

	@Override
	protected boolean calculateEnabled()
	{
		if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null &&
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor() instanceof BaseVisualFormEditor)
		{
			BaseVisualFormEditor editor = (BaseVisualFormEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			if (editor.getForm().getUseCssPosition())
			{
				return false;
			}
		}
		return super.calculateEnabled();
	}

	public static class ToggleAnchoringTop extends ToggleAnchoringActionDelegateHandler
	{
		public static final String TOGGLE_ANCHORING_TOP_ID = "com.servoy.eclipse.designer.rfb.anchorTop";

		public ToggleAnchoringTop()
		{
			super(AnchorPropertySource.TOP, IAnchorConstants.NORTH);
		}
	}
	public static class ToggleAnchoringRight extends ToggleAnchoringActionDelegateHandler
	{
		public static final String TOGGLE_ANCHORING_RIGHT_ID = "com.servoy.eclipse.designer.rfb.anchorRight";

		public ToggleAnchoringRight()
		{
			super(AnchorPropertySource.RIGHT, IAnchorConstants.EAST);
		}
	}
	public static class ToggleAnchoringBottom extends ToggleAnchoringActionDelegateHandler
	{
		public static final String TOGGLE_ANCHORING_BOTTOM_ID = "com.servoy.eclipse.designer.rfb.anchorBottom";

		public ToggleAnchoringBottom()
		{
			super(AnchorPropertySource.BOTTOM, IAnchorConstants.SOUTH);
		}
	}
	public static class ToggleAnchoringLeft extends ToggleAnchoringActionDelegateHandler
	{
		public static final String TOGGLE_ANCHORING_LEFT_ID = "com.servoy.eclipse.designer.rfb.anchorLeft";

		public ToggleAnchoringLeft()
		{
			super(AnchorPropertySource.LEFT, IAnchorConstants.WEST);
		}
	}
}
