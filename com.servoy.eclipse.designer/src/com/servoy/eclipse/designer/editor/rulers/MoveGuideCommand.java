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

import org.eclipse.gef.commands.Command;

/**
 * Move guide command.
 * 
 * @author rgansevles
 *
 */
public class MoveGuideCommand extends Command
{
	private final FormRulerProvider parent;
	private final int positionDelta;
	private final RulerGuide guide;

	public MoveGuideCommand(FormRulerProvider parent, RulerGuide guide, int positionDelta)
	{
		super("Move guide");
		this.parent = parent;
		this.guide = guide;
		this.positionDelta = positionDelta;
	}

	@Override
	public void execute()
	{
		move(positionDelta);
	}

	@Override
	public void undo()
	{
		move(-positionDelta);
	}

	/**
	 * @param delta
	 */
	protected void move(int delta)
	{
		guide.setPosition(guide.getPosition() + delta);
		parent.refreshGuide(guide);
	}
}
