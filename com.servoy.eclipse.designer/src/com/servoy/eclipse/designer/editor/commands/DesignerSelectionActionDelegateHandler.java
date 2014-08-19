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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.actions.AbstractEditorActionDelegateHandler;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistChangeListener;

/**
 * Base class for actions based on the selection in form designer.
 * 
 * @author rgansevles
 */

public abstract class DesignerSelectionActionDelegateHandler extends AbstractEditorActionDelegateHandler
{
	protected final Object requestType;

	private final IPersistChangeListener persistChangeListener;

	public DesignerSelectionActionDelegateHandler(Object requestType)
	{
		this.requestType = requestType;
		ServoyModelManager.getServoyModelManager().getServoyModel().addPersistChangeListener(false, persistChangeListener = createPersistChangeListener());
	}

	protected IPersistChangeListener createPersistChangeListener()
	{
		return new IPersistChangeListener()
		{
			public void persistChanges(Collection<IPersist> changes)
			{
				IAction action = getCurrentAction();
				if (action != null)
				{
					Boolean checked = calculateChecked();
					if (checked != null)
					{
						action.setChecked(checked.booleanValue());
					}
				}
			}
		};
	}

	/**
	 * Create a command to work the selected objects.
	 * 
	 * @return The command to work the selected objects.
	 */
	@Override
	protected Command createCommand()
	{
		IStructuredSelection objects = getSelection();
		if (objects.isEmpty()) return null;

		Map<EditPart, Request> requests = createRequests(objects.toList());
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
			requests.put(editPart, new Request(requestType));
		}
		return requests;
	}

	/**
	 * This may be overridden in subclasses to change the selected set
	 */
	protected List<EditPart> getSelectedElements(List<EditPart> selected)
	{
		return selected;
	}

	@Override
	public void dispose()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel().removePersistChangeListener(false, persistChangeListener);
		super.dispose();
	}
}
