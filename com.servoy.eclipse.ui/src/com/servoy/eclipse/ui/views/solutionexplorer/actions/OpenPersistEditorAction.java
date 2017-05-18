/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IPersist;

/**
 * @author emera
 */
public class OpenPersistEditorAction extends Action implements ISelectionChangedListener
{

	protected IStructuredSelection selection;

	public OpenPersistEditorAction()
	{
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("form.png"));
		setText("Open Persist Editor");
		setToolTipText(getText());
	}

	@Override
	public void run()
	{
		if (selection != null)
		{
			Iterator< ? > it = selection.iterator();
			while (it.hasNext())
			{
				IPersist persist = (IPersist)it.next();
				if (persist != null)
				{
					EditorUtil.openPersistEditor(persist, false);
				}
			}
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		if (event.getSelection() instanceof IStructuredSelection)
		{
			this.selection = (IStructuredSelection)event.getSelection();
			Iterator< ? > it = selection.iterator();
			boolean state = it.hasNext();
			while (it.hasNext())
			{
				state = it.next() instanceof IPersist;
			}
			setEnabled(state);
		}
		else setEnabled(false);
	}
}