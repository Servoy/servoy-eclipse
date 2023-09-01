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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IViewPart;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.ScriptMethod;

/**
 * An action that is able to edit variables.
 *
 * @author acostescu
 */
public class DebugMethodAction extends Action implements ISelectionChangedListener
{
	private ScriptMethod method = null;
	private final IViewPart viewPart;

	/**
	 * Creates a new edit variable action that will use the given shell to show the edit variable dialog.
	 *
	 * @param shell used to show a dialog.
	 */
	public DebugMethodAction(IViewPart viewPart)
	{
		this.viewPart = viewPart;

		setText("Debug Method");
		setToolTipText("Debug this method in the Smart/Webclient");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		method = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean ok = (sel.size() == 1);
		if (ok)
		{
			SimpleUserNode un = (SimpleUserNode)sel.getFirstElement();
			if (un.getType() == UserNodeType.FORM_METHOD || un.getType() == UserNodeType.GLOBAL_METHOD_ITEM)
			{
				ok = true;
				method = (ScriptMethod)un.getRealObject();
			}
			else
			{
				ok = false;
				method = null;
			}
		}

		setEnabled(ok);
	}

	public boolean isMethodSelected()
	{
		return method != null;
	}

	@Override
	public void run()
	{
		if (method != null)
		{
			if (Activator.getDefault().getDebugClientHandler().getDebugReadyClient() != null) Activator.getDefault().getDebugClientHandler().executeMethod(
				method.getParent(), method.getScopeName(), method.getName());
			else MessageDialog.openError(UIUtils.getActiveShell(),
				"Debug Method Problem", "Cannot debug method; please start a debug client first.");
		}
	}
}
