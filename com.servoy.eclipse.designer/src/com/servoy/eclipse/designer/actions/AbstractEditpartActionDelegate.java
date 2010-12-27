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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.ide.ResourceUtil;

import com.servoy.eclipse.designer.editor.VisualFormEditor.RequestType;
import com.servoy.eclipse.designer.editor.commands.SelectModelsCommandWrapper;
import com.servoy.eclipse.designer.property.IPersistEditPart;
import com.servoy.eclipse.designer.property.SetValueCommand;
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
public abstract class AbstractEditpartActionDelegate implements IWorkbenchWindowActionDelegate, IActionDelegate2
{

	private static List<IActionAddedListener> actionListeners = new ArrayList<AbstractEditpartActionDelegate.IActionAddedListener>();
	protected final static List<IAction> editPartActions = new ArrayList<IAction>();

	private ISelection fSelection;
	private Shell fCurrentShell;
	protected IAction fAction;
	protected final RequestType requestType;
	protected final Map<Object, Object> extendedData = new HashMap<Object, Object>();


	public AbstractEditpartActionDelegate(RequestType requestType)
	{
		this.requestType = requestType;
	}

	public void dispose()
	{
		editPartActions.remove(fAction);
	}

	public void init(IAction action)
	{
		fAction = action;
		editPartActions.add(action);
		fireActionAdded(action);
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
			editPart.getViewer().getEditDomain().getCommandStack().execute(new SelectModelsCommandWrapper(editPart.getViewer(), command));
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
	protected void addSetPropertyValue(String key, Object value)
	{
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

		List<EditPart> editParts = getEditparts();
		boolean enabled = editParts.size() > 0;

		for (int i = 0; enabled && i < editParts.size(); i++)
		{
			EditPart editPart = editParts.get(i);
			enabled = editPart.understandsRequest(new Request(requestType)) && checkApplicable(editPart);
		}
		action.setEnabled(enabled);
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
			EditPart editPart = (EditPart)ResourceUtil.getAdapter(elements.next(), EditPart.class, true);
			if (editPart != null)
			{
				editParts.add(editPart);
			}
		}
		return editParts;
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

	public static List<IAction> getEditPartActions()
	{
		return new ArrayList<IAction>(editPartActions);
	}

	public static void addActionAddedListener(IActionAddedListener listener)
	{
		synchronized (actionListeners)
		{
			if (!actionListeners.contains(listener))
			{
				actionListeners.add(listener);
			}
		}
	}

	public static void removeActionAddedListener(IActionAddedListener listener)
	{
		synchronized (actionListeners)
		{
			actionListeners.remove(listener);
		}
	}

	protected void fireActionAdded(final IAction action)
	{
		if (actionListeners.size() > 0)
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				// fire later, the action is not fully initialized
				public void run()
				{
					IActionAddedListener[] array;
					synchronized (actionListeners)
					{
						array = actionListeners.toArray(new IActionAddedListener[actionListeners.size()]);
					}

					for (IActionAddedListener element : array)
					{
						element.editorActionCreated(action);
					}
				}
			});
		}
	}

	/** Listener interface for actions added from plugin.xml
	 * 
	 * @author rgansevles
	 *
	 */
	public interface IActionAddedListener
	{
		void editorActionCreated(IAction action);
	}
}
