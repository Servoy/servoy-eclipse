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
package com.servoy.eclipse.designer.editor.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.ui.IWorkbenchPart;

public abstract class DesignerSelectionAction extends SelectionAction
{
	protected final Object requestType;

	public DesignerSelectionAction(IWorkbenchPart part, Object requestType)
	{
		super(part);
		this.requestType = requestType;
		setLazyEnablementCalculation(false);
	}

	/**
	 * Returns <code>true</code> if the selected objects can be handled. Returns <code>false</code> if there are no objects selected or the selected objects
	 * are not {@link EditPart}s.
	 * 
	 * @return <code>true</code> if the command should be enabled
	 */
	@Override
	protected boolean calculateEnabled()
	{
		Command cmd = createCommand(getSelectedObjects());
		return cmd != null && cmd.canExecute();
	}

	/**
	 * Create a command to work the selected objects.
	 * 
	 * @param objects The objects selected.
	 * @return The command to work the selected objects.
	 */
	protected final Command createCommand(List objects)
	{
		if (objects.isEmpty()) return null;
		if (!(objects.get(0) instanceof EditPart)) return null;

		Map<EditPart, List<EditPart>> parentsMap = new HashMap<EditPart, List<EditPart>>();
		for (int i = 0; i < objects.size(); i++)
		{
			EditPart object = (EditPart)objects.get(i);
			EditPart parent = object.getParent();
			if (parent != null)
			{
				if (parentsMap.get(parent) == null)
				{
					parentsMap.put(parent, new ArrayList<EditPart>());
				}
				parentsMap.get(parent).add(object);
			}
		}

		// First try the command on the parents
		CompoundCommand compoundCmd = new CompoundCommand();
		for (Map.Entry<EditPart, List<EditPart>> entry : parentsMap.entrySet())
		{
			EditPart parent = entry.getKey();
			List<EditPart> children = entry.getValue();
			GroupRequest request = createRequest(children);
			if (request != null)
			{
				Command cmd = parent.getCommand(request);
				if (cmd != null) compoundCmd.add(cmd);
			}
		}

		if (!compoundCmd.canExecute())
		{
			// command cannot be executed on the parents, try the children edit parts
			GroupRequest request = createRequest(objects);
			if (request != null)
			{
				compoundCmd = new CompoundCommand();
				for (int i = 0; i < objects.size(); i++)
				{
					EditPart object = (EditPart)objects.get(i);
					Command cmd = object.getCommand(request);
					if (cmd != null) compoundCmd.add(cmd);
				}
			}
		}
		return compoundCmd.unwrap();
	}

	protected GroupRequest createRequest(List<EditPart> selected)
	{
		GroupRequest groupRequest = new GroupRequest(requestType);
		groupRequest.setEditParts(selected);
		return groupRequest;
	}

	/**
	 * Initializes this action's text and images.
	 */
	@Override
	protected void init()
	{
		super.init();
		setEnabled(false);
	}

	/**
	 * Performs the action on the selected objects.
	 */
	@Override
	public void run()
	{
		execute(createCommand(getSelectedObjects()));
	}
}
