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
package com.servoy.eclipse.ui.views.solutionexplorer;

import java.util.ArrayList;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This class is able to filter the part listener events and provide support for ActiveEditorListeners.
 * 
 * @author Andrei Costescu
 */
public class ActiveEditorTracker implements IPartListener
{

	private IEditorPart currentActiveEditor;
	private final ArrayList<ActiveEditorListener> listeners;

	/**
	 * Creates and initializes (the currently active editor) a new ActiveEditorTracker.
	 */
	public ActiveEditorTracker(IEditorPart currentActiveEditor)
	{
		listeners = new ArrayList<ActiveEditorListener>();
		this.currentActiveEditor = currentActiveEditor;
	}

	/**
	 * Returns the currently active editor (as calculated by this tracker).
	 * 
	 * @return the currently active editor (as calculated by this tracker).
	 */
	public IEditorPart getActiveEditor()
	{
		return currentActiveEditor;
	}

	public void addActiveEditorListener(ActiveEditorListener l)
	{
		if (!listeners.contains(l))
		{
			listeners.add(l);
		}
	}

	public void removeActiveEditorListener(ActiveEditorListener l)
	{
		listeners.remove(l);
	}

	public void partActivated(IWorkbenchPart part)
	{
		if (part instanceof IEditorPart && currentActiveEditor != part)
		{
			currentActiveEditor = (IEditorPart)part;
			fireActiveEditorChanged(currentActiveEditor);
		}
	}

	private void fireActiveEditorChanged(IEditorPart part)
	{
		for (ActiveEditorListener l : listeners)
		{
			l.activeEditorChanged(part);
		}
	}

	public void partBroughtToTop(IWorkbenchPart part)
	{
	}

	public void partClosed(IWorkbenchPart part)
	{
		if (part instanceof IEditorPart && currentActiveEditor == part)
		{
			currentActiveEditor = null;
			fireActiveEditorChanged(currentActiveEditor);
		}
	}

	public void partDeactivated(IWorkbenchPart part)
	{
	}

	public void partOpened(IWorkbenchPart part)
	{
	}

}
