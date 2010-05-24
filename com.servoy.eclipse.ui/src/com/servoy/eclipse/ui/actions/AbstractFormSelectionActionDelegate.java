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
package com.servoy.eclipse.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;

/**
 * Abstract base class for menu object contributions and command actions that work on a form.
 * 
 * @author rob
 * 
 */
public abstract class AbstractFormSelectionActionDelegate implements IWorkbenchWindowActionDelegate, IObjectActionDelegate
{
	private final List<Openable> openables = new ArrayList<Openable>();

	public void selectionChanged(IAction action, ISelection selection)
	{
		openables.clear();
		if (isEnabled())
		{
			if (selection instanceof IStructuredSelection)
			{
				Iterator iterator = ((IStructuredSelection)selection).iterator();
				while (iterator.hasNext())
				{
					Openable openable = (Openable)Platform.getAdapterManager().getAdapter(iterator.next(), Openable.class);
					if (openable != null)
					{
						openables.add(openable);
					}
				}
			}
			else
			{
				// see if the current part is an editor
				if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null &&
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null &&
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart() instanceof IEditorPart)
				{
					IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
					Form form = EditorUtil.getForm((IEditorPart)activePart);
					if (form != null)
					{
						openables.add(Openable.getOpenable(form)); // for script editor
					}
				}
			}
		}

		action.setEnabled(checkEnabled(openables));
	}

	protected boolean checkEnabled(List<Openable> lst)
	{
		return lst.size() > 0;
	}

	protected boolean isEnabled()
	{
		return true;
	}

	public final void run(IAction action)
	{
		for (Openable openable : openables)
		{
			open(openable);
		}
	}

	abstract public void open(Openable openable);

	public void dispose()
	{
	}

	public void init(IWorkbenchWindow window)
	{
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart)
	{
	}
}
