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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.ide.ResourceUtil;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor.RequestType;
import com.servoy.eclipse.designer.editor.commands.SelectModelsCommandWrapper;
import com.servoy.eclipse.designer.property.SetValueCommand;

/**
 * Abstract action delegate for actions on graphical edit parts.
 * <p>
 * The action delegate subclasses collect data for the command (typically via a dialog). The command is performed by the selected edit parts' edit policy using
 * a DataRequest request.
 *
 * @author rgansevles
 *
 */
public abstract class AbstractEditpartActionDelegate implements IWorkbenchWindowActionDelegate, IActionDelegate2
{
	private ISelection fSelection;
	private Shell fCurrentShell;
	protected IAction fAction;
	protected final RequestType requestType;
	protected Map<Object, Object> extendedData = null;

	public AbstractEditpartActionDelegate(RequestType requestType)
	{
		this.requestType = requestType;
	}

	public void dispose()
	{
	}

	public void init(IAction action)
	{
		fAction = action;
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

	public void runWithEvent(IAction action, Event event)
	{
		run(action);
	}

	public final void run(IAction action)
	{
		List<EditPart> editParts = getEditparts();
		if (editParts.size() == 0)
		{
			// no edit part selected
			return;
		}

		// run the action on the first one if multiple are selected (for example, multiple labels are selected,
		// add tab should be done once only because it actually works on the parent)
		EditPart editPart = editParts.get(0);
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
			editPart.getViewer().getEditDomain().getCommandStack().execute(
				new SelectModelsCommandWrapper(editPart.getViewer(), getToRefresh(editPart), command));
		}
	}

	protected EditPart getToRefresh(EditPart affected)
	{
		return affected == null ? null : affected.getParent();
	}

	@SuppressWarnings("unchecked")
	private Request getRequest(EditPart editPart)
	{
		Request request = createRequest(editPart);
		if (request != null)
		{
			if (extendedData == null) fillExtendedData();
			if (extendedData != null)
				request.getExtendedData().putAll(extendedData);
		}
		return request;
	}

	/**
	 * @return
	 */
	protected void fillExtendedData()
	{
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
	protected void addSetPropertyValue(String key, Object value)
	{
		if (extendedData == null)
		{
			extendedData = new HashMap<Object, Object>();
		}
		extendedData.put(SetValueCommand.REQUEST_PROPERTY_PREFIX + key, value);
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
		action.setEnabled(calculateEnabled(getEditparts()));
	}

	public boolean calculateEnabled(List<EditPart> editParts)
	{
		boolean enabled = editParts.size() > 0;

		for (int i = 0; enabled && i < editParts.size(); i++)
		{
			EditPart editPart = editParts.get(i);
			enabled = editPart.understandsRequest(new Request(requestType)) && checkApplicable(editPart);
		}
		return enabled;
	}

	/**
	 * Get the currently selected edit parts.
	 *
	 */
	protected List<EditPart> getEditparts()
	{
		if (!(fSelection instanceof IStructuredSelection) || fSelection.isEmpty())
		{
			return Collections.<EditPart> emptyList();
		}

		List<EditPart> editParts = new ArrayList<EditPart>(((IStructuredSelection)fSelection).size());
		Iterator< ? > elements = ((IStructuredSelection)fSelection).iterator();
		while (elements.hasNext())
		{
			EditPart editPart = ResourceUtil.getAdapter(elements.next(), EditPart.class, true);
			if (editPart != null)
			{
				editParts.add(editPart);
			}
		}
		return editParts;
	}
}
