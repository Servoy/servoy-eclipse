/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;

import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * @author lvostinar
 *
 */
public class ToggleAnchoringCommand extends AbstractEditorAndOutlineActionDelegateHandler
{
	private final int anchorConstant;

	public ToggleAnchoringCommand(int anchorConstant)
	{
		this.anchorConstant = anchorConstant;
	}

	@Override
	protected Command createCommand()
	{
		List< ? > selection = getSelectedObjects();
		if (selection != null && selection.size() > 0 && (selection.get(0) instanceof BaseComponent || selection.get(0) instanceof PersistContext))
		{
			CompoundCommand cc = new CompoundCommand();
			for (Object component : selection)
			{
				BaseComponent comp = component instanceof BaseComponent ? (BaseComponent)component : null;
				if (component instanceof PersistContext && ((PersistContext)component).getPersist() instanceof BaseComponent)
				{
					comp = (BaseComponent)((PersistContext)component).getPersist();
				}
				if (comp == null) continue;

				int anchoring = comp.getAnchors();
				if ((anchoring & anchorConstant) == anchorConstant)
				{
					anchoring = anchoring - anchorConstant;
				}
				else
				{
					anchoring = anchoring + anchorConstant;
				}
				cc.add(new SetPropertyCommand("anchor", PersistPropertySource.createPersistPropertySource(comp, false),
					StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName(), Integer.valueOf(anchoring)));
			}
			return cc;
		}
		return super.createCommand();
	}

	public static class ToggleAnchoringTop extends ToggleAnchoringCommand
	{
		public ToggleAnchoringTop()
		{
			super(IAnchorConstants.NORTH);
		}
	}

	public static class ToggleAnchoringRight extends ToggleAnchoringCommand
	{
		public ToggleAnchoringRight()
		{
			super(IAnchorConstants.EAST);
		}
	}

	public static class ToggleAnchoringBottom extends ToggleAnchoringCommand
	{
		public ToggleAnchoringBottom()
		{
			super(IAnchorConstants.SOUTH);
		}
	}

	public static class ToggleAnchoringLeft extends ToggleAnchoringCommand
	{
		public ToggleAnchoringLeft()
		{
			super(IAnchorConstants.WEST);
		}
	}
}
