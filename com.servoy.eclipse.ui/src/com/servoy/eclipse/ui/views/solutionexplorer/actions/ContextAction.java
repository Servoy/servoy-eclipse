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



import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * Action delegates to other registered action depending on the selection.
 * 
 * @author rob
 */
public class ContextAction extends Action implements ISelectionChangedListener
{
	final private Map<UserNodeType, IAction> registeredActions = new HashMap<UserNodeType, IAction>();

	private IAction currentAction;
	private final ImageDescriptor defaultImageDescriptor;
	private final String defaultText;

	private final SolutionExplorerView viewer;

	public ContextAction(SolutionExplorerView viewer, ImageDescriptor defaultImageDescriptor, String text)
	{
		this.viewer = viewer;
		this.defaultImageDescriptor = defaultImageDescriptor;
		this.defaultText = text;
		setCurrentAction(null);
	}

	private void setCurrentAction(IAction action)
	{
		currentAction = action;
		if (action == null)
		{
			setImageDescriptor(defaultImageDescriptor);
			setText(defaultText);
			setToolTipText(defaultText);
			setEnabled(false);
		}
		else
		{
			ImageDescriptor desc = action.getImageDescriptor();
			if (desc == null)
			{
				desc = defaultImageDescriptor;
			}
			setImageDescriptor(desc);
			setText(action.getText());
			setToolTipText(action.getToolTipText());
			setEnabled(action.isEnabled());
		}
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IAction action = null;
		Iterator<SimpleUserNode> sel = ((IStructuredSelection)event.getSelection()).iterator();
		while (sel.hasNext())
		{
			SimpleUserNode node = sel.next();
			IAction act = registeredActions.get(node.getType());
			if (action == null)
			{
				action = act;
			}
			else if (act != action)
			{
				// multiple actions selected
				setCurrentAction(null);
				return;
			}
		}

		if (action instanceof ISelectionChangedListener)
		{
			((ISelectionChangedListener)action).selectionChanged(event);
		}
		setCurrentAction(action);
	}

	@Override
	public void run()
	{
		if (currentAction != null && currentAction.isEnabled())
		{
			currentAction.run();
			//viewer.refresh();
		}
	}

	public void registerAction(UserNodeType type, IAction action)
	{
		registeredActions.put(type, action);
	}

	public void unregisterAction(UserNodeType type)
	{
		registeredActions.remove(type);
	}
}
