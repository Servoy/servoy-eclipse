/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import org.eclipse.gef.commands.Command;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IFlattenedPersistWrapper;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.PositionComparator;

/**
 * @author jcompagner
 *
 */
public class ChangeParentCommand extends Command
{
	private final IPersist child, targetChild;
	private final ISupportChilds newParent;
	private ISupportChilds oldParent;
	private int oldIndex;
	private final boolean insertAfterTarget;

	private final boolean hasChildPositionSupport;
	private final Class< ? > childPositionClass;

	public ChangeParentCommand(IPersist child, ISupportChilds newParent)
	{
		this(child, newParent, null, false);
	}

	public ChangeParentCommand(IPersist child, ISupportChilds newParent, IPersist targetChild, boolean insertAfterTarget)
	{
		super("Change Parent");

		this.child = child;
		this.targetChild = targetChild;

		this.newParent = newParent == null ? getFlattenedParent(child) : newParent;
		this.insertAfterTarget = insertAfterTarget;

		this.hasChildPositionSupport = child instanceof ISupportBounds || child instanceof IChildWebObject;
		this.childPositionClass = child instanceof ISupportBounds ? ISupportBounds.class : IChildWebObject.class;
	}

	@Override
	public void execute()
	{
		oldParent = getFlattenedParent(child);

		if (hasChildPositionSupport)
		{
			ArrayList<IPersist> children = getChildrenSortedOnType(oldParent);
			oldIndex = children.indexOf(child);
		}

		oldParent.removeChild(child);

		if (hasChildPositionSupport)
		{
			ArrayList<IPersist> children = getChildrenSortedOnType(newParent);

			int insertIdx = childPositionClass.isInstance(targetChild) ? children.indexOf(targetChild) : -1;
			if (insertIdx == -1) children.add(child);
			else
			{
				if (insertAfterTarget) insertIdx++;
				if (insertIdx < children.size()) children.add(insertIdx, child);
				else children.add(child);
			}

			updateWithOrderedPosition(children);
		}

		newParent.addChild(child);

		IPersist changedChild = child instanceof IFlattenedPersistWrapper< ? > ? ((IFlattenedPersistWrapper< ? >)child).getWrappedPersist() : child;
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, Arrays.asList(new IPersist[] { changedChild }));
	}

	private ISupportChilds getFlattenedParent(IPersist persist)
	{
		ISupportChilds flattenedParent = persist.getParent();
		if (flattenedParent instanceof Form)
		{
			FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(persist);
			flattenedParent = flattenedSolution.getFlattenedForm(flattenedParent);
		}

		return flattenedParent;
	}

	private void updateWithOrderedPosition(ArrayList<IPersist> children)
	{
		int counter = child instanceof ISupportBounds ? 1 : 0;
		for (IPersist p : children)
		{
			if (child instanceof ISupportBounds)
			{
				((ISupportBounds)p).setLocation(new Point(counter, counter));
			}
			else if (child instanceof IChildWebObject)
			{
				((IChildWebObject)p).setIndex(counter);
			}
			counter++;
		}
	}

	private ArrayList<IPersist> getChildrenSortedOnType(ISupportChilds parent)
	{
		ArrayList<IPersist> children = new ArrayList<IPersist>();
		Iterator<IPersist> it = parent.getAllObjects();
		while (it.hasNext())
		{
			IPersist persist = it.next();

			if (childPositionClass.isInstance(persist))
			{
				children.add(persist);
			}
		}
		IPersist[] sortedChildArray = children.toArray(new IPersist[0]);
		if (childPositionClass == ISupportBounds.class)
		{
			Arrays.sort(sortedChildArray, PositionComparator.XY_PERSIST_COMPARATOR);
		}
		else if (childPositionClass == IChildWebObject.class)
		{
			Arrays.sort(sortedChildArray, new Comparator<IPersist>()
			{
				@Override
				public int compare(IPersist o1, IPersist o2)
				{
					return ((IChildWebObject)o1).getIndex() - ((IChildWebObject)o2).getIndex();
				}
			});
		}
		return new ArrayList<IPersist>(Arrays.asList(sortedChildArray));
	}

	@Override
	public void undo()
	{
		ArrayList<IPersist> changes = new ArrayList<IPersist>();
		changes.add(child.getParent());
		changes.add(oldParent);
		IPersist changedChild = child instanceof IFlattenedPersistWrapper< ? > ? ((IFlattenedPersistWrapper< ? >)child).getWrappedPersist() : child;
		changes.add(changedChild);
		child.getParent().removeChild(child);

		if (hasChildPositionSupport)
		{
			ArrayList<IPersist> children = getChildrenSortedOnType(oldParent);
			if (oldIndex < children.size())
			{
				children.add(oldIndex, child);
			}
			else
			{
				children.add(child);
			}
			updateWithOrderedPosition(children);
		}

		oldParent.addChild(child);
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, changes);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	@Override
	public void redo()
	{
		ArrayList<IPersist> changes = new ArrayList<IPersist>();
		changes.add(newParent);
		changes.add(oldParent);
		super.redo();
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, changes);
	}
}
