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
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.Pair;

/**
 * This action opens in the editor the user script element currently selected in the outline of the solution view.
 * 
 * @author acostescu
 */
public class OpenScriptAction extends Action implements ISelectionChangedListener
{

	protected IStructuredSelection selection;

	/**
	 * Creates a new open script action.
	 */
	public OpenScriptAction()
	{
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("open.gif")); //$NON-NLS-1$
		setText("Open in Script Editor");
		setToolTipText(getText());
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		selection = (IStructuredSelection)event.getSelection();
		Iterator it = selection.iterator();
		boolean state = it.hasNext();
		while (it.hasNext())
		{
			UserNodeType type = ((SimpleUserNode)it.next()).getType();
			if (type != UserNodeType.FORM_METHOD && type != UserNodeType.GLOBAL_METHOD_ITEM && type != UserNodeType.GLOBAL_VARIABLE_ITEM &&
				type != UserNodeType.FORM_VARIABLE_ITEM && type != UserNodeType.CALCULATIONS_ITEM && type != UserNodeType.GLOBALS_ITEM &&
				type != UserNodeType.FORM_VARIABLES && type != UserNodeType.GLOBAL_VARIABLES)
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
			Iterator it = selection.iterator();
			while (it.hasNext())
			{
				SimpleUserNode node = ((SimpleUserNode)it.next());
				if (node != null)
				{
					String scopeName = null;
					Object obj = node.getRealObject();
					if (obj instanceof ColumnWrapper)
					{
						obj = ((ColumnWrapper)obj).getColumn();
					}
					else if (obj instanceof Pair< ? , ? >) // Pair<Solution, Scopename>
					{
						if (((Pair< ? , ? >)obj).getRight() instanceof String)
						{
							scopeName = ((Pair< ? , String>)obj).getRight();
						}
						obj = ((Pair< ? , ? >)obj).getLeft();

					}
					if (obj instanceof IPersist)
					{
						EditorUtil.openScriptEditor((IPersist)obj, scopeName, true);
					}
				}
			}
		}
	}
}