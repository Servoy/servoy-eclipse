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
package com.servoy.eclipse.designer.actions;

import java.util.List;

import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.ui.actions.AbstractFormSelectionActionDelegate;
import com.servoy.eclipse.ui.actions.Openable;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;

/**
 * Action delegate form opening form in designer.
 * 
 * Used for both menu object contribution and action handler.
 * 
 * @author rob
 * 
 */
public class OpenFormInDesignerActionDelegate extends AbstractFormSelectionActionDelegate
{
	@Override
	protected boolean checkEnabled(List<Openable> lst)
	{
		for (Openable openable : lst)
		{
			if (openable.getData() instanceof Form)
			{
				// at least 1 form
				return true;
			}
		}
		return false;
	}

	@Override
	public void open(Openable openable)
	{
		if (openable.getData() instanceof Form)
		{
			EditorUtil.openFormDesignEditor((Form)openable.getData());
		}
	}

	@Override
	protected boolean isEnabled()
	{
		if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null &&
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart() instanceof VisualFormEditor) return false;
		return true;
	}
}
