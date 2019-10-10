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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.servoy.eclipse.ui.node.UserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;

/**
 * @author Diana
 *
 */
public class OpenEditorAction extends Action
{

	protected IStructuredSelection selection;

	/**
	 * @param selection
	 */
	public void setSelection(IStructuredSelection selection)
	{
		this.selection = selection;
		Iterator< ? > it = selection.iterator();
		boolean state = it.hasNext();
		while (it.hasNext())
		{
			Object sel = it.next();
			if (sel instanceof UserNode)
			{
				UserNodeType type = ((UserNode)sel).getType();
				if (type != UserNodeType.FORM_METHOD && type != UserNodeType.GLOBAL_METHOD_ITEM && type != UserNodeType.GLOBAL_VARIABLE_ITEM &&
					type != UserNodeType.FORM_VARIABLE_ITEM && type != UserNodeType.CALCULATIONS_ITEM && type != UserNodeType.GLOBALS_ITEM &&
					type != UserNodeType.FORM_VARIABLES && type != UserNodeType.GLOBAL_VARIABLES)
				{
					state = false;
				}
			}
			else if (!isEnabledFor(sel))
			{
				state = false;
			}
		}
		setEnabled(state);
	}

	private boolean isEnabledFor(Object sel)
	{
		return (sel instanceof Form) || (sel instanceof ScriptMethod) || (sel instanceof ScriptVariable);
	}

}
