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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.ide.ResourceUtil;

import com.servoy.eclipse.designer.editor.VisualFormEditor.RequestType;
import com.servoy.eclipse.designer.property.IPersistEditPart;
import com.servoy.j2db.persistence.IPersist;

/**
 * Abstract action delegate for actions on graphical edit parts.
 * <p>
 * The action delegate subclasses collect data for the command (typically via a dialog). The command is performed by the selected edit parts' edit policy using
 * a DataRequest request.
 * 
 * @author rgansevles
 * 
 */
public abstract class AbstractEditpartActionDelegate implements IWorkbenchWindowActionDelegate
{
	private ISelection fSelection;
	private Shell fCurrentShell;
	protected final RequestType requestType;
	protected final Map<Object, Object> extendedData = new HashMap<Object, Object>();

	public AbstractEditpartActionDelegate(RequestType requestType)
	{
		this.requestType = requestType;
	}

	public void dispose()
	{
	}

	public void init(IWorkbenchWindow window)
	{
		fCurrentShell = window.getShell();
	}

	public RequestType getRequestType()
	{
		return requestType;
	}

	public Shell getShell()
	{
		return fCurrentShell;
	}

	public final void run(IAction action)
	{
		EditPart editPart = getEditpart();
		if (editPart == null)
		{
			// no edit  part selected
			return;
		}

		Request request = getRequest(editPart);
		if (request == null)
		{
			// cannot find data or user canceled action
			return;
		}

		Command command = editPart.getCommand(request);
		if (command != null)
		{
			// execute the command on the command stack (supports undo/redo)
			editPart.getViewer().getEditDomain().getCommandStack().execute(command);
		}
	}

	private Request getRequest(EditPart editPart)
	{
		Request request = createRequest(editPart);
		if (request != null)
		{
			request.setExtendedData(extendedData);
		}
		return request;
	}

	/**
	 * Collect the request data, typically via a dialog to the user.
	 * 
	 * @param editPart
	 * @return
	 */
	protected Request createRequest(EditPart editPart)
	{
		return new Request(getRequestType());
	}

	/**
	 * Put some extra data in the request, to be interpreted by the executing command.
	 * 
	 * @param key
	 * @param value
	 */
	protected void addExtendedData(Object key, Object value)
	{
		extendedData.put(key, value);
	}

	/**
	 * Override for extra checks on the edit part
	 * 
	 * @param editPart
	 * @return
	 */
	protected boolean checkApplicable(EditPart editPart)
	{
		return true;
	}

	/**
	 * Enable or disable the action based on the selection.
	 */
	public void selectionChanged(IAction action, ISelection selection)
	{
		fSelection = selection;

		boolean enabled = false;

		EditPart editPart = getEditpart();
		if (editPart != null)
		{
			enabled = editPart.understandsRequest(new Request(requestType)) && checkApplicable(editPart);
		}
		action.setEnabled(enabled);
	}

	/**
	 * Get the currently selected edit part.
	 * 
	 */
	protected EditPart getEditpart()
	{
		if (fSelection instanceof IStructuredSelection && ((IStructuredSelection)fSelection).size() == 1)
		{
			return (EditPart)ResourceUtil.getAdapter(((IStructuredSelection)fSelection).getFirstElement(), EditPart.class, true);
		}
		return null;
	}

	/**
	 * Get the model parent with the given class from the edit part. Return null if not found.
	 */
	protected IPersist getModel(EditPart editPart, int typeId)
	{
		EditPart ep = editPart;
		while (!(ep instanceof IPersistEditPart))
		{
			if (ep == null)
			{
				return null;

			}
			ep = ep.getParent();
		}
		IPersist persist = ((IPersistEditPart)ep).getPersist();
		if (persist == null)
		{
			return null;
		}
		return persist.getAncestor(typeId);
	}
}
