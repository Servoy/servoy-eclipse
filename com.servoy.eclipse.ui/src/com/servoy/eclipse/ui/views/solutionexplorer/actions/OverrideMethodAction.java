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

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ScriptMethod;

public class OverrideMethodAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;

	/**
	 * Creates a new "create new method" action for the given solution view.
	 * 
	 * @param sev the solution view to use.
	 */
	public OverrideMethodAction(SolutionExplorerView sev)
	{
		viewer = sev;
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
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			if (type == UserNodeType.FORM_METHOD && ((SimpleUserNode)sel.getFirstElement()).getRealObject() instanceof ScriptMethod)
			{
				SimpleUserNode node = (SimpleUserNode)sel.getFirstElement();
				ScriptMethod method = (ScriptMethod)node.getRealObject();
				SimpleUserNode formNode = node.getAncestorOfType(Form.class);
				Form parent = (Form)method.getAncestor(IRepository.FORMS);
				if (parent != null && parent.getName() != null && formNode != null && formNode.getRealObject() instanceof Form &&
					!parent.getName().equals(((Form)formNode.getRealObject()).getName()))
				{
					state = true;
				}
			}
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node != null)
		{
			ServoyProject pr = (ServoyProject)node.getAncestorOfType(ServoyProject.class).getRealObject();
			if (pr != null)
			{
				if (node.getType() == UserNodeType.FORM)
				{
					SimpleUserNode listNode = viewer.getSelectedListNode();
					if (listNode != null && listNode.getRealObject() instanceof ScriptMethod)
					{
						NewMethodAction.createNewMethod(viewer.getSite().getShell(), (Form)node.getRealObject(), null, true,
							((ScriptMethod)listNode.getRealObject()).getName(), null);
					}
				}
			}
			else
			{
				ServoyLog.logWarning("Cannot find servoy project", null);
			}
		}
	}
}
