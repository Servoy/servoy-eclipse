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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.servoy.eclipse.ui.views.solutionexplorer.ITreeListView;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ScriptMethod;

public class OverrideMethodAction extends Action implements ISelectionChangedListener
{
	private final ITreeListView viewer;

	/**
	 * Creates a new "create new method" action for the given solution view.
	 *
	 * @param sev the solution view to use.
	 */
	public OverrideMethodAction(ITreeListView viewer)
	{
		this.viewer = viewer;
		setText("Override method");
		setToolTipText("Override method");
		setEnabled(false);
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			state = false;
			Object selectedListElement = viewer.getSelectedListElement();
			if (selectedListElement instanceof ScriptMethod)
			{
				ISupportChilds parent = ((ScriptMethod)selectedListElement).getParent();
				if (parent instanceof Form)
				{
					Form scriptForm = (Form)parent;
					Object selectedTreeElement = viewer.getSelectedTreeElement();
					if (selectedTreeElement instanceof Form && !selectedTreeElement.equals(scriptForm))
					{
						state = true;
					}
				}
			}

		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		ScriptMethod selectedScriptMethod = (ScriptMethod)viewer.getSelectedListElement();
		Form targetForm = (Form)viewer.getSelectedTreeElement();
		NewMethodAction.createNewMethod(viewer.getSite().getShell(), targetForm, null, true, selectedScriptMethod.getName(), null,
			selectedScriptMethod.getRuntimeProperty(IScriptProvider.METHOD_RETURN_TYPE));
	}
}
