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
import java.util.List;

import org.eclipse.gef.commands.Command;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IFlattenedPersistWrapper;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 *
 */
public class ChangeParentCommand extends Command
{
	private final IPersist child;
	private final IPersist targetChild;
	private ISupportChilds newParent;
	private final Form form;
	private final boolean insertAfterTarget;
	private final Class< ? > childPositionClass;

	//undo information
	private int oldIndex;
	private ISupportChilds oldParent;

	public ChangeParentCommand(IPersist child, ISupportChilds newParent, IPersist targetChild, Form form, boolean insertAfterTarget)
	{
		super("Change Parent");

		this.form = form;
		this.child = child instanceof IFlattenedPersistWrapper< ? > ? ((IFlattenedPersistWrapper< ? >)child).getWrappedPersist() : child;
		this.targetChild = targetChild instanceof IFlattenedPersistWrapper< ? > ? ((IFlattenedPersistWrapper< ? >)targetChild).getWrappedPersist()
			: targetChild;

		this.newParent = newParent;
		this.insertAfterTarget = insertAfterTarget;
		this.childPositionClass = child instanceof ISupportBounds ? ISupportBounds.class : (child instanceof IChildWebObject ? IChildWebObject.class : null);
	}

	@Override
	public boolean canExecute()
	{
		boolean canExecute = super.canExecute();
		if (canExecute)
		{
			ISupportChilds initialParent = child instanceof ISupportExtendsID ? ((ISupportExtendsID)child).getRealParent() : child.getParent();
			ISupportChilds possibleNewParent = newParent;
			if (possibleNewParent == null) possibleNewParent = initialParent;

			if (possibleNewParent != initialParent)
			{
				// different parent
				if (PersistHelper.isOverrideElement(child) || child.getAncestor(IRepository.FORMS) != form)
				{
					// cannot modify structure for inherited or override element
					return false;
				}

				// adding an element into its own child is not allowed
				if (child instanceof LayoutContainer && isChildOf(possibleNewParent, (LayoutContainer)child))
				{
					return false;
				}
			}
		}
		return canExecute;
	}

	@Override
	public void execute()
	{
		List<IPersist> changes = new ArrayList<>();

		FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(child.getParent());

		ISupportChilds initialParent = child instanceof ISupportExtendsID ? ((ISupportExtendsID)child).getRealParent() : child.getParent();
		boolean sameParent = false;
		if (newParent == null)
		{
			newParent = initialParent;
			sameParent = true;
		}
		if (!form.equals(newParent.getAncestor(IRepository.FORMS)))
		{
			try
			{
				newParent = (ISupportChilds)ElementUtil.getOverridePersist(PersistContext.create(newParent, form));
				changes.add(newParent);
			}
			catch (RepositoryException e)
			{
				Debug.error("Cannot move " + child.getUUID() + " to " + newParent.getUUID() + ". Cause: " + e);
			}
		}

		if (childPositionClass != null)
		{
			ISupportChilds flattenedOldParent = PersistHelper.getFlattenedPersist(flattenedSolution, form, initialParent);
			ArrayList<IPersist> sortedChildren = getChildrenSortedOnType(flattenedOldParent,
				child instanceof IChildWebObject ? ((IChildWebObject)child).getJsonKey() : null);
			oldIndex = sortedChildren.indexOf(child);
		}
		ISupportChilds flattenedNewParent = PersistHelper.getFlattenedPersist(flattenedSolution, form, newParent);
		if (newParent == initialParent || sameParent)
		{
			oldParent = null;
			//same parent
			updateChildPosition(flattenedNewParent, changes, false);
		}
		else
		{
			oldParent = initialParent;
			initialParent.removeChild(child);
			newParent.addChild(child);
			child.flagChanged();
			updateChildPosition(flattenedNewParent, changes, false);
		}
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, changes);
	}

	@Override
	public void undo()
	{
		ArrayList<IPersist> changes = new ArrayList<IPersist>();
		// undo hierarchy change
		if (oldParent != null)
		{
			newParent.removeChild(child);
			oldParent.addChild(child);
			child.flagChanged();
		}
		//undo position change
		ISupportChilds flattenedNewParent = PersistHelper.getFlattenedPersist(ModelUtils.getEditingFlattenedSolution(child.getParent()), form,
			oldParent != null ? oldParent : newParent);
		updateChildPosition(flattenedNewParent, changes, true);
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, changes);
	}

	private static boolean isChildOf(IPersist persist, ISupportChilds parent)
	{
		boolean isChildOf = false;
		IPersist persistParent = persist;

		do
		{
			persistParent = persistParent.getParent();
			isChildOf = persistParent == parent;
		}
		while (persistParent != null && !isChildOf);

		return isChildOf;
	}

	private ArrayList<IPersist> getChildrenSortedOnType(ISupportChilds parent, String jsonKey)
	{
		ArrayList<IPersist> children = new ArrayList<IPersist>();
		Iterator<IPersist> it = parent.getAllObjects();
		while (it.hasNext())
		{
			IPersist persist = it.next();

			if (childPositionClass.isInstance(persist) &&
				(jsonKey == null || (persist instanceof IChildWebObject && Utils.equalObjects(jsonKey, ((IChildWebObject)persist).getJsonKey()))))
			{
				children.add(persist instanceof IFlattenedPersistWrapper ? ((IFlattenedPersistWrapper< ? >)persist).getWrappedPersist() : persist);
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

	private void updateChildPosition(ISupportChilds flattenedNewParent, List<IPersist> changes, boolean useOldIndex)
	{
		if (childPositionClass != null)
		{
			ArrayList<IPersist> sortedChildren = getChildrenSortedOnType(flattenedNewParent,
				child instanceof IChildWebObject ? ((IChildWebObject)child).getJsonKey() : null);
			sortedChildren.remove(child);
			int insertIndex = -1;
			if (useOldIndex)
			{
				insertIndex = oldIndex;
			}
			else
			{
				if (targetChild == null)
				{
					insertIndex = sortedChildren.size() > 0 ? sortedChildren.size() : 0;
				}
				else
				{
					insertIndex = sortedChildren.indexOf(targetChild);
					if (insertIndex != -1 && insertAfterTarget)
					{
						int count = 0;
						Iterator<IPersist> it = newParent.getAllObjects();

						while (it.hasNext())
						{
							IPersist persist = it.next();
							if (persist != null)
							{
								count++;
							}
						}

						insertIndex = count - 1;
					}
				}
			}
			if (insertIndex >= 0)
			{
				sortedChildren.add(insertIndex, child);
				for (int i = 0; i < sortedChildren.size(); i++)
				{
					IPersist persist = sortedChildren.get(i);
					try
					{
						persist = ElementUtil.getOverridePersist(PersistContext.create(persist, form));
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
					if (persist instanceof ISupportBounds)
					{
						((ISupportBounds)persist).setLocation(new Point(i + 1, i + 1));
					}
					else if (persist instanceof IChildWebObject)
					{
						((IChildWebObject)persist).setIndex(i);
					}
					if (!changes.contains(newParent)) changes.add(persist);
				}
				if (flattenedNewParent instanceof IBasicWebObject && child instanceof WebCustomType)
				{
					((IBasicWebObject)flattenedNewParent).setProperty(((WebCustomType)child).getJsonKey(), sortedChildren.toArray(new IChildWebObject[0]));
				}
			}
		}
	}
}
