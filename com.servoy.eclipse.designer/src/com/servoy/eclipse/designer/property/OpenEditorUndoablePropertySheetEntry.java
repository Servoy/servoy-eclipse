/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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
package com.servoy.eclipse.designer.property;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.ui.IEditorPart;

import com.servoy.eclipse.ui.EditorActionsRegistry;
import com.servoy.eclipse.ui.EditorActionsRegistry.EditorComponentActions;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.properties.PropertySheetEntry;
import com.servoy.j2db.dataprocessing.IModificationListener;

/**
 * UndoablePropertySheetEntry that opens the editor when needed.
 */
public final class OpenEditorUndoablePropertySheetEntry extends UndoablePropertySheetEntry
{
	private final IModificationListener editorComponentActionListener;

	/** The command stack we currently have a listener on (may change if the editor is closed and reopened). */
	private CommandStack listenerStack;
	private CommandStackListener undoRefreshListener;

	public OpenEditorUndoablePropertySheetEntry()
	{
		if (getParent() == null)
		{
			editorComponentActionListener = null;
		}
		else
		{
			// root entry
			editorComponentActionListener = EditorActionsRegistry.addComponentActionListener(EditorComponentActions.CREATE_CUSTOM_COMPONENT,
				event -> refreshFromRoot());
		}
	}

	/**
	 * @see org.eclipse.ui.views.properties.PropertySheetEntry#createChildEntry()
	 */
	@Override
	protected PropertySheetEntry createChildEntry()
	{
		return new OpenEditorUndoablePropertySheetEntry();
	}

	@Override
	protected CommandStack getCommandStack()
	{
		// only the root has, and is listening too, the command stack
		if (getParent() != null) return ((OpenEditorUndoablePropertySheetEntry)getParent()).getCommandStack();
		if (stack == null && getValues().length > 0)
		{
			IEditorPart persistEditor = EditorUtil.openPersistEditor(getValues()[0], false);
			if (persistEditor != null)
			{
				// Don't cache the stack here, if the form is closed and we edit again a new command stack is created
				return persistEditor.getAdapter(CommandStack.class);
			}
		}
		return stack;
	}

	/**
	 * Returns the resolved command stack. Public accessor so the property sheet page
	 * can wire undo/redo actions to it.
	 */
	public CommandStack resolveCommandStack()
	{
		return getCommandStack();
	}

	@Override
	protected void executeCommand(Command command)
	{
		// Ensure we have a CommandStackListener registered on the current stack
		// so that undo/redo triggers a refresh in the Properties view.
		// The stack may change if the editor is closed and reopened, so re-register if needed.
		CommandStack cs = getCommandStack();
		if (cs != null && cs != listenerStack)
		{
			if (listenerStack != null && undoRefreshListener != null)
			{
				listenerStack.removeCommandStackListener(undoRefreshListener);
			}
			undoRefreshListener = (e) -> refreshFromRoot();
			cs.addCommandStackListener(undoRefreshListener);
			listenerStack = cs;
		}
		super.executeCommand(command);
	}

	@Override
	public void dispose()
	{
		if (listenerStack != null && undoRefreshListener != null)
		{
			listenerStack.removeCommandStackListener(undoRefreshListener);
			listenerStack = null;
			undoRefreshListener = null;
		}
		if (editorComponentActionListener != null)
		{
			EditorActionsRegistry.removeComponentActionListener(EditorComponentActions.CREATE_CUSTOM_COMPONENT, editorComponentActionListener);
		}

		super.dispose();
	}
}
