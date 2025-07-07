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

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.dialogs.ServoySearchDialog;
import com.servoy.eclipse.ui.dialogs.ServoySearchDialog.Column;
import com.servoy.eclipse.ui.dialogs.ServoySearchDialog.Scope;
import com.servoy.eclipse.ui.dialogs.ServoySearchDialog.Table;
import com.servoy.eclipse.ui.preferences.SolutionExplorerPreferences;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;

public class SearchServoyAction implements IWorkbenchWindowActionDelegate
{

	private IWorkbenchWindow window;

	public void dispose()
	{
	}

	public void init(IWorkbenchWindow window)
	{
		this.window = window;
	}

	public void run(IAction action)
	{
		ServoySearchDialog ssd = new ServoySearchDialog(window.getShell());

		final int resultCode = ssd.open();
		if (resultCode == IDialogConstants.OK_ID)
		{
			final Object result = ssd.getFirstResult();
			if (result instanceof Form)
			{
				IEclipsePreferences store = InstanceScope.INSTANCE.getNode(Activator.getDefault().getBundle().getSymbolicName());
				String formDblClickOption = store.get(SolutionExplorerPreferences.FORM_DOUBLE_CLICK_ACTION,
					SolutionExplorerPreferences.DOUBLE_CLICK_OPEN_FORM_EDITOR);
				boolean showFormDesigner = SolutionExplorerPreferences.DOUBLE_CLICK_OPEN_FORM_EDITOR.equals(formDblClickOption);
				if (ssd.isAltKeyPressed()) showFormDesigner = !showFormDesigner;
				if (showFormDesigner)
				{
					EditorUtil.openFormDesignEditor((Form)result);
				}
				else
				{
					EditorUtil.openScriptEditor((Form)result, null, true);
				}
			}
			else if (result instanceof IPersist)
			{
				EditorUtil.openPersistEditor((IPersist)result);
			}
			else if (result instanceof Table)
			{
				Table table = (Table)result;
				EditorUtil.openTableEditor(table.getDataSource());
			}
			else if (result instanceof Column)
			{
				EditorUtil.openTableEditor(((Column)result).getDataSource());
			}
			else if (result instanceof Scope)
			{
				Scope scope = (Scope)result;
				EditorUtil.openScriptEditor(ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(scope.getSolutionName()).getSolution(),
					scope.getScopeName(), true);
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection)
	{
	}

}
