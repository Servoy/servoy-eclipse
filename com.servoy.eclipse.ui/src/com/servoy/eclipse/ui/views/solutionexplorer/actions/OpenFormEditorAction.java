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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;

/**
 * @author gerzse
 */
public class OpenFormEditorAction extends Action implements ISelectionChangedListener
{

	protected IStructuredSelection selection;

	public OpenFormEditorAction()
	{
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("designer.gif")); //$NON-NLS-1$
		setText("Open in Form Editor"); //$NON-NLS-1$
		setToolTipText(getText());
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		selection = (IStructuredSelection)event.getSelection();
		Iterator< ? > it = selection.iterator();
		boolean state = it.hasNext();
		while (it.hasNext())
		{
			UserNodeType type = ((SimpleUserNode)it.next()).getType();
			if (type != UserNodeType.FORM)
			{
				state = false;
			}
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		if (selection != null)
		{
			Iterator< ? > it = selection.iterator();
			while (it.hasNext())
			{
				SimpleUserNode node = ((SimpleUserNode)it.next());
				if (node != null)
				{
					Object obj = node.getRealObject();
					if (obj instanceof Form)
					{
						EditorUtil.openFormDesignEditor((Form)obj);
					}
				}
			}
		}
	}

}
