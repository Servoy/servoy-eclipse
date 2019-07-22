/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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
import java.awt.Point;

import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.IPersist;

/**
 * @author emera
 */
public class SetCssAnchoringCommand extends BaseRestorableCommand
{
	private final String top;
	private final String left;
	private final IPersist persist;
	private final String bottom;
	private final String right;


	public SetCssAnchoringCommand(String top, String right, String bottom, String left, IPersist persist)
	{
		super("cssPosition");
		this.top = top;
		this.left = left;
		this.persist = persist;
		this.bottom = bottom;
		this.right = right;
	}

	@Override
	public void execute()
	{
		BaseComponent component = (BaseComponent)persist;
		saveState(component);
		CSSPosition position = component.getCssPosition();
		Point location = CSSPositionUtils.getLocation(component);
		Dimension size = CSSPositionUtils.getSize(component);
		String _top = top;
		String _bottom = bottom;
		String _left = left;
		String _right = right;
		//make sure we have at least one of the opposite anchors set
		if (!CSSPositionUtils.isSet(top) && !CSSPositionUtils.isSet(bottom))
		{
			_top = !CSSPositionUtils.isSet(position.top) && !CSSPositionUtils.isSet(position.bottom) ? "0" : position.top;
			_bottom = position.bottom;
		}
		if (!CSSPositionUtils.isSet(left) && !CSSPositionUtils.isSet(right))
		{
			_left = !CSSPositionUtils.isSet(position.left) && !CSSPositionUtils.isSet(position.right) ? "0" : position.left;
			_right = position.right;
		}

		CSSPosition newPosition = new CSSPosition(_top, _right, _bottom, _left, position.width, position.height);
		component.setCssPosition(newPosition);
		CSSPositionUtils.setLocation(component, location.x, location.y);
		CSSPositionUtils.setSize(component, size.width, size.height);
		super.execute();
	}
}