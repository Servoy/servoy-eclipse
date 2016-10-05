/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.designer.editor;

import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * @author rgansevles
 *
 */
public abstract class BaseGraphicalEditPart extends AbstractGraphicalEditPart
{

	@Override
	public void setSelected(int value)
	{
		// only selectable edit parts may get selected.
		// log and ignore, do not throw an exception because on macos this may cause a freeze,
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=395381
		if (!isSelectable() && value != SELECTED_NONE)
		{
			ServoyLog.logError(new IllegalArgumentException("An EditPart has to be selectable (isSelectable() == true) in order to get selected."));
			return;
		}

		super.setSelected(value);
	}
}
