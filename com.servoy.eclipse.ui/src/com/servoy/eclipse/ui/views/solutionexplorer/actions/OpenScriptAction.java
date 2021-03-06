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

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.Pair;

/**
 * This action opens in the editor the user script element currently selected in the outline of the solution view.
 *
 * @author acostescu
 */
public class OpenScriptAction extends OpenEditorAction implements ISelectionChangedListener
{

	public static final String OPEN_SCRIPT_ID = "com.servoy.eclipse.ui.OpenFormJsAction";

	/**
	 * Creates a new open script action.
	 */
	public OpenScriptAction()
	{
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("open.png"));
		setText("Open in Script Editor");
		setToolTipText(getText());
		setId(OPEN_SCRIPT_ID);
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		setSelection((IStructuredSelection)event.getSelection());
	}

	@Override
	public void run()
	{
		if (selection != null)
		{
			Iterator< ? > it = selection.iterator();
			while (it.hasNext())
			{
				Object obj = it.next();
				String scopeName = null;
				if (obj instanceof SimpleUserNode)
				{
					SimpleUserNode node = ((SimpleUserNode)obj);
					obj = node.getRealObject();
				}

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