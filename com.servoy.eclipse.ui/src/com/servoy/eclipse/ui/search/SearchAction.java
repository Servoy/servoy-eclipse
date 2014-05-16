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
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;

import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.ValueList;

/**
 * Action that is displayed in the context menu of the supported {@link IPersist} instances, currently {@link Relation} and {@link ValueList}
 * 
 * @author jcompagner
 * @since 6.0
 */
public class SearchAction extends Action implements ISelectionChangedListener
{
	private Object selectedObject;

	/**
	 * @param solutionExplorerView
	 */
	public SearchAction()
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
		ISearchQuery query = null;
		if (selectedObject instanceof ValueList)
		{
			query = new ValueListSearch((ValueList)selectedObject);
		}
		else if (selectedObject instanceof Relation)
		{
			query = new RelationSearch((Relation)selectedObject);
		}
		else if (selectedObject instanceof String) // when it is a string it is the plugin name.
		{
			query = new PluginSearch((String)selectedObject);
		}
		else if (selectedObject instanceof Form)
		{
			query = new FormSearch((Form)selectedObject);
		}
		else if (selectedObject instanceof ScriptMethod)
		{
			query = new ScriptMethodSearch((ScriptMethod)selectedObject);
		}
		else if (selectedObject instanceof ScriptVariable)
		{
			query = new ScriptVariableSearch((ScriptVariable)selectedObject);
		}
		else if (selectedObject instanceof IServer)
		{
			query = new ServerSearch((IServer)selectedObject);
		}
		else if (selectedObject instanceof TableWrapper)
		{
			query = new TableSearch((TableWrapper)selectedObject);
		}
		else if (selectedObject instanceof BaseComponent)
		{
			query = new ElementSearch((BaseComponent)selectedObject);
		}
		else if (selectedObject instanceof IColumn)
		{
			query = new DataProviderSearch((IColumn)selectedObject);
		}
		else if (selectedObject instanceof ColumnWrapper)
		{
			if (((ColumnWrapper)selectedObject).getColumn() instanceof Column) query = new DataProviderSearch(((ColumnWrapper)selectedObject).getColumn());
		}
		else if (selectedObject instanceof Media)
		{
			query = new MediaSearch((Media)selectedObject);
		}
		if (query != null) NewSearchUI.runQueryInBackground(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event)
	{
		selectedObject = null;

		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		if (sel.size() == 1)
		{
			SimpleUserNode node = ((SimpleUserNode)sel.getFirstElement());
			if (node.getType() == UserNodeType.VALUELIST_ITEM || node.getType() == UserNodeType.RELATION || node.getType() == UserNodeType.FORM ||
				node.getType() == UserNodeType.FORM_METHOD || node.getType() == UserNodeType.GLOBAL_METHOD_ITEM ||
				node.getType() == UserNodeType.GLOBAL_VARIABLE_ITEM || node.getType() == UserNodeType.FORM_VARIABLE_ITEM ||
				node.getType() == UserNodeType.SERVER || node.getType() == UserNodeType.TABLE || node.getType() == UserNodeType.FORM_ELEMENTS_ITEM ||
				node.getType() == UserNodeType.BEAN || node.getType() == UserNodeType.VIEW || node.getType() == UserNodeType.TABLE_COLUMNS_ITEM ||
				node.getType() == UserNodeType.RELATION_COLUMN || node.getType() == UserNodeType.CALCULATIONS_ITEM ||
				node.getType() == UserNodeType.MEDIA_IMAGE)
			{
				selectedObject = node.getRealObject();
				if (selectedObject instanceof Object[])
				{
					selectedObject = ((Object[])selectedObject)[0];
				}
			}
			else if (node.getType() == UserNodeType.PLUGIN)
			{
				selectedObject = node.getName();
			}
		}
		setEnabled(selectedObject != null);

	}
}
