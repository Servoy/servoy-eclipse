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

package com.servoy.eclipse.designer.editor.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IFlattenedPersistWrapper;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author gboros
 *
 */
public abstract class MoveCommand extends ContentOutlineCommand implements IServerService
{
	private final BaseVisualFormEditor editorPart;
	private final ISelectionProvider selectionProvider;

	public MoveCommand(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
	}

	/*
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		return execute();
	}

	/*
	 * @see org.sablo.websocket.IServerService#executeMethod(java.lang.String, org.json.JSONObject)
	 */
	@Override
	public Object executeMethod(String methodName, JSONObject args) throws Exception
	{
		return execute();
	}

	public abstract Object execute();

	@Override
	protected BaseVisualFormEditor getEditorPart()
	{
		return editorPart != null ? editorPart : super.getEditorPart();
	}

	@Override
	protected List<Object> getSelectionList()
	{
		if (selectionProvider != null)
		{
			return ((IStructuredSelection)selectionProvider.getSelection()).toList();
		}
		else
		{
			return super.getSelectionList();
		}
	}

	protected ArrayList<IPersist> getSortedChildren(ISupportChilds parent)
	{
		if (parent instanceof AbstractBase)
		{
			ISupportChilds persist = parent;
			if (parent instanceof LayoutContainer)
			{
				persist = PersistHelper.getFlattenedPersist(ModelUtils.getEditingFlattenedSolution(parent), editorPart.getForm(), parent);
			}
			ArrayList<IPersist> children = new ArrayList<>();
			Iterator<IPersist> it = persist.getAllObjects();
			while (it.hasNext())
			{
				IPersist p = it.next();
				children.add(p instanceof IFlattenedPersistWrapper< ? > ? ((IFlattenedPersistWrapper< ? >)p).getWrappedPersist() : p);
			}
			Collections.sort(children, PositionComparator.XY_PERSIST_COMPARATOR);

			return children;
		}
		return null;

	}

	protected IPersist getSingleSelection()
	{
		List<IPersist> selection = getSelection();
		if (selection.size() == 1)
		{
			return selection.get(0);
		}
		return null;
	}
}
