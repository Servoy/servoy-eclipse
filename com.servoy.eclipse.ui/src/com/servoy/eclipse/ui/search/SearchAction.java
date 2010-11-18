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

package com.servoy.eclipse.ui.search;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.search.ui.NewSearchUI;

import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ValueList;

/**
 * Action that is displayed in the context menu of the supported {@link IPersist} instances, currently {@link Relation} and {@link ValueList}
 * 
 * @author jcompagner
 * @since 6.0
 */
public class SearchAction extends Action implements ISelectionChangedListener
{
	private ValueList valueList;
	private Relation relation;
	private Form form;

	/**
	 * @param solutionExplorerView
	 */
	public SearchAction()
	{
		setText("Search References"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run()
	{
		if (valueList != null)
		{
			NewSearchUI.runQueryInBackground(new ValueListSearch(valueList), NewSearchUI.activateSearchResultView());
		}
		else if (relation != null)
		{
			NewSearchUI.runQueryInBackground(new RelationSearch(relation), NewSearchUI.activateSearchResultView());
		}
		else if (form != null)
		{
			NewSearchUI.runQueryInBackground(new FormSearch(form), NewSearchUI.activateSearchResultView());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event)
	{
		relation = null;
		valueList = null;
		form = null;

		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		if (sel.size() == 1)
		{
			SimpleUserNode node = ((SimpleUserNode)sel.getFirstElement());
			if ((((SimpleUserNode)sel.getFirstElement()).getType() == UserNodeType.VALUELIST_ITEM))
			{
				valueList = (ValueList)node.getRealObject();
			}
			if ((((SimpleUserNode)sel.getFirstElement()).getType() == UserNodeType.RELATION))
			{
				relation = (Relation)node.getRealObject();
			}
			if ((((SimpleUserNode)sel.getFirstElement()).getType() == UserNodeType.FORM))
			{
				form = (Form)node.getRealObject();
			}
		}
		setEnabled(valueList != null || relation != null || form != null);

	}
}
