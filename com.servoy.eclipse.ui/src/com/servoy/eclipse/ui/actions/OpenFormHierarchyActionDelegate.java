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

package com.servoy.eclipse.ui.actions;

import java.util.List;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.ui.views.solutionexplorer.FormHierarchyView;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.util.Debug;

/**
 * @author emera
 */
public class OpenFormHierarchyActionDelegate extends AbstractFormSelectionActionDelegate
{

	public static final String OPEN_FORM_HIERARCHY_ID = "com.servoy.eclipse.ui.OpenFormHierarchyAction";

	@Override
	protected boolean checkEnabled(List<Openable> lst)
	{
		if (lst.size() == 1)
		{
			Object obj = lst.get(0).getData();
			if (obj instanceof Form || obj instanceof ScriptMethod || obj instanceof BaseComponent)
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public void open(Openable openable)
	{
		Object obj = openable.getData();
		if (obj instanceof Form || obj instanceof ScriptMethod || obj instanceof BaseComponent)
		{
			try
			{
				FormHierarchyView view = (FormHierarchyView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(FormHierarchyView.ID);
				view.setSelection(obj);
			}
			catch (PartInitException e)
			{
				Debug.error(e);
			}
		}
	}
}
