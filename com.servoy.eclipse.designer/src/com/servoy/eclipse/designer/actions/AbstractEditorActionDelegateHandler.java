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

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

/**
 * Base class for actions that can be used in both as command handler (extension points org.eclipse.ui.commands/org.eclipse.ui.handlers) and 
 * as viewer contribution class (extension point org.eclipse.ui.popupMenus).
 * 
 * @author rgansevles
 *
 */

public abstract class AbstractEditorActionDelegateHandler extends AbstractHandler implements IEditorActionDelegate
{
	private IAction currentAction;
	private IStructuredSelection selection = StructuredSelection.EMPTY;

	public void run()
	{
		execute(createCommand());
	}

	protected Command createCommand()
	{
		return null;
	}

	public Object execute()
	{
		run();
		return null;
	}

	/**
	 * Called from command (via shortcut)
	 */
	public final Object execute(ExecutionEvent event) throws ExecutionException
	{
		selectionChanged(null,
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getSite().getSelectionProvider().getSelection());
		return execute();
	}

	public final void run(IAction action)
	{
		execute();
	}

	public void selectionChanged(IAction action, ISelection sel)
	{
		this.selection = sel instanceof IStructuredSelection ? (IStructuredSelection)sel : StructuredSelection.EMPTY;
		if (action != null)
		{
			this.currentAction = action;
			action.setEnabled(calculateEnabled());
			Boolean checked = calculateChecked();
			if (checked != null)
			{
				action.setChecked(checked.booleanValue());
			}
		}
	}

	public void setActiveEditor(IAction action, IEditorPart targetEditor)
	{
	}

	/**
	 * Returns <code>true</code> if the selected objects can be handled. Returns <code>false</code> if there are no objects selected or the selected objects
	 * are not {@link EditPart}s.
	 * 
	 * @return <code>true</code> if the command should be enabled
	 */
	protected boolean calculateEnabled()
	{
		Command cmd = createCommand();
		return cmd != null && cmd.canExecute();
	}

	protected Boolean calculateChecked()
	{
		return null; // means checked is managed by the action
	}

	/**
	 * Executes the given {@link Command} using the command stack. The stack is
	 * obtained by calling {@link #getCommandStack()}, which uses
	 * <code>IAdapatable</code> to retrieve the stack from the workbench part.
	 * 
	 * @param command
	 *            the command to execute
	 */
	protected void execute(Command command)
	{
		if (command == null || !command.canExecute()) return;
		getCommandStack().execute(command);
	}

	/**
	 * Returns the editor's command stack. This is done by asking the workbench
	 * part for its CommandStack via
	 * {@link org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)}.
	 * 
	 * @return the command stack
	 */
	protected CommandStack getCommandStack()
	{
		return (CommandStack)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getAdapter(CommandStack.class);
	}

	/**
	 * @return the selection
	 */
	public IStructuredSelection getSelection()
	{
		return selection;
	}

	protected List< ? > getSelectedObjects()
	{
		return getSelection().toList();
	}

	/**
	 * @return the currentAction
	 */
	public IAction getCurrentAction()
	{
		return currentAction;
	}

	@Override
	public void dispose()
	{
		selection = StructuredSelection.EMPTY;
		currentAction = null;
		super.dispose();
	}
}
