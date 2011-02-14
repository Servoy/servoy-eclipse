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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Base class for actions based on the selection in form designer.
 * 
 * @author rgansevles
 */

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

		Map<EditPart, Request> requests = createRequests(objects);
		CompoundCommand compoundCmd = null;
		EditPartViewer viewer = null;
		if (requests != null)
		{
			for (Entry<EditPart, Request> entry : requests.entrySet())
			{
				if (viewer == null) viewer = entry.getKey().getViewer();
				Command cmd = entry.getKey().getCommand(entry.getValue());
				if (cmd != null)
				{
					if (compoundCmd == null)
					{
						compoundCmd = new CompoundCommand();
					}
					compoundCmd.add(cmd);
				}
			}
		}
		if (compoundCmd == null)
		{
			return null;
		}
		return new SelectModelsCommandWrapper(viewer, getToRefresh(requests.keySet()), compoundCmd.unwrap());
	}

	/**
	 * @param keySet
	 * @return
	 */
	protected Iterable<EditPart> getToRefresh(Iterable<EditPart> affected)
	{
		return affected;
	}

	protected Map<EditPart, Request> createRequests(List<EditPart> selected)
	{
		Map<EditPart, Request> requests = null;
		for (EditPart editPart : selected)
		{
			if (requests == null)
			{
				requests = new HashMap<EditPart, Request>(selected.size());
			}
			requests.put(editPart, createRequest(editPart));
		}
		return requests;
	}

	/**
	 * @param editPart  
	 */
	public Request createRequest(EditPart editPart)
	{
		return new Request(getRequestType());
	}

	/**
	 * @return the requestType
	 */
	public Object getRequestType()
	{
		return requestType;
	}

	/**
	 * This may be overridden in subclasses to change the selected set
	 */
	protected List<EditPart> getSelectedElements(List<EditPart> selected)
	{
		return selected;
	}


	public Shell getShell()
	{
		return getWorkbenchPart().getSite().getShell();
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
