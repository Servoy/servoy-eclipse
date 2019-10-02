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
import com.servoy.j2db.persistence.IAnchorConstants;

/**
 * An action to set the anchoring of selected objects.
 *
 * @author rgansevles
 */
public abstract class SetAnchoringActionDelegateHandler extends SetPropertyActionDelegateHandler
{
	public SetAnchoringActionDelegateHandler(int anchoring)
	{
		super("anchors", "set anchoring", Integer.valueOf(anchoring));
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

	public static class SetAnchoringTopLeft extends SetAnchoringActionDelegateHandler
	{
		public SetAnchoringTopLeft()
		{
			super(IAnchorConstants.NORTH | IAnchorConstants.WEST);
		}
	}

	public static class SetAnchoringTopRight extends SetAnchoringActionDelegateHandler
	{
		public SetAnchoringTopRight()
		{
			super(IAnchorConstants.NORTH | IAnchorConstants.EAST);
		}
	}

	public static class SetAnchoringRightBottom extends SetAnchoringActionDelegateHandler
	{
		public SetAnchoringRightBottom()
		{
			super(IAnchorConstants.SOUTH | IAnchorConstants.EAST);
		}
	}

	public static class SetAnchoringLeftBottom extends SetAnchoringActionDelegateHandler
	{
		public SetAnchoringLeftBottom()
		{
			super(IAnchorConstants.SOUTH | IAnchorConstants.WEST);
		}
	}

	public static class SetAnchoringTopRightLeftBottom extends SetAnchoringActionDelegateHandler
	{
		public SetAnchoringTopRightLeftBottom()
		{
			super(IAnchorConstants.ALL);
		}
	}
}
