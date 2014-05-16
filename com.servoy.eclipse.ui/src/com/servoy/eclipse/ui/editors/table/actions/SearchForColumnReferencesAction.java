/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.ui.editors.table.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;

import com.servoy.eclipse.ui.search.DataProviderSearch;
import com.servoy.eclipse.ui.search.SearchAction;
import com.servoy.j2db.persistence.Column;

/**
 * Implementation of {@link SearchAction} for the table editor's column page (ColumnComposite).
 * 
 * @author acostache
 *
 */
public class SearchForColumnReferencesAction extends Action implements ISelectionChangedListener
{
	private Column selectedColumn;

	public SearchForColumnReferencesAction()
	{
		setText("Search for References");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run()
	{
		ISearchQuery query = new DataProviderSearch(selectedColumn);
		if (query != null) NewSearchUI.runQueryInBackground(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event)
	{
		selectedColumn = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		if (sel.size() == 1 && sel.getFirstElement() instanceof Column) selectedColumn = (Column)sel.getFirstElement();
		setEnabled(selectedColumn != null);

	}
}
