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
package com.servoy.eclipse.ui.actions;

import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.Pair;


/**
 * Open script file for menu item object contribution and action class.
 * 
 * @author rgansevles
 * 
 */
public class OpenInScripteditorActionDelegate extends AbstractFormSelectionActionDelegate
{
	@Override
	public void open(Openable openable)
	{
		if (openable.getData() instanceof IPersist)
		{
			EditorUtil.openScriptEditor((IPersist)openable.getData(), null, true);
		}
		else if (openable.getData() instanceof Pair< ? , ? > && ((Pair< ? , ? >)openable.getData()).getLeft() instanceof IPersist &&
			((Pair< ? , ? >)openable.getData()).getRight() instanceof String)
		{
			EditorUtil.openScriptEditor((IPersist)((Pair< ? , ? >)openable.getData()).getLeft(), (String)((Pair< ? , ? >)openable.getData()).getRight(), true);
		}
	}
}
