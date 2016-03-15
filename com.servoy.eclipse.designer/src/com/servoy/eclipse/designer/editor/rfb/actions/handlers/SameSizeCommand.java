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

import java.awt.Dimension;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;

import com.servoy.eclipse.designer.actions.AbstractEditorActionDelegateHandler;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * @author lvostinar
 *
 */
public class SameSizeCommand extends AbstractEditorActionDelegateHandler
{

	private final boolean sameWidth;

	public SameSizeCommand(boolean sameWidth)
	{
		this.sameWidth = sameWidth;
	}

	@Override
	protected Command createCommand()
	{
		List< ? > selection = getSelectedObjects();
		if (selection != null && selection.size() > 1 && (selection.get(0) instanceof BaseComponent || selection.get(0) instanceof PersistContext))
		{
			CompoundCommand cc = new CompoundCommand();
			Dimension size = null;
			for (Object component : selection)
			{
				BaseComponent comp = component instanceof BaseComponent ? (BaseComponent)component : null;
				if (component instanceof PersistContext && ((PersistContext)component).getPersist() instanceof BaseComponent)
				{
					comp = (BaseComponent)((PersistContext)component).getPersist();
				}
				if (comp == null) continue;

				if (size == null)
				{
					size = comp.getSize();
				}
				else
				{
					Dimension oldSize = comp.getSize();
					cc.add(new SetPropertyCommand("resize", PersistPropertySource.createPersistPropertySource(comp, false),
						StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(),
						new Dimension(sameWidth ? size.width : oldSize.width, sameWidth ? oldSize.height : size.height)));
				}
			}
			return cc;
		}
		return super.createCommand();
	}

	public static class Width extends SameSizeCommand
	{
		public Width()
		{
			super(true);
		}
	}
	public static class Height extends SameSizeCommand
	{
		public Height()
		{
			super(false);
		}
	}
}
