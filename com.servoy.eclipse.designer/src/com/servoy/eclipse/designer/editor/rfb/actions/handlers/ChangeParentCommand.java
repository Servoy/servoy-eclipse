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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.rfb.RfbVisualFormEditorDesignPage;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.FlattenedLayoutContainer;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IFlattenedPersistWrapper;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author jcompagner
 *
 */
public class ChangeParentCommand extends Command
{
	private IPersist child;
	private final IPersist targetChild;
	private ISupportChilds newParent;
	private final Form form;
	private int oldIndex;
	private final boolean insertAfterTarget;

	private final boolean hasChildPositionSupport;
	private final Class< ? > childPositionClass;
	private ISupportChilds oldParent;
	private IPersist oldChild;
	private int insertIdx;

	public ChangeParentCommand(IPersist child, ISupportChilds newParent, IPersist targetChild, Form form, boolean insertAfterTarget)
	{
		super("Change Parent");

		this.form = form;
		this.child = child instanceof IFlattenedPersistWrapper< ? > ? ((IFlattenedPersistWrapper< ? >)child).getWrappedPersist() : child;
		this.targetChild = targetChild instanceof IFlattenedPersistWrapper< ? > ? ((IFlattenedPersistWrapper< ? >)targetChild).getWrappedPersist()
			: targetChild;

		this.newParent = newParent;
		this.insertAfterTarget = insertAfterTarget;

		this.hasChildPositionSupport = child instanceof ISupportBounds || child instanceof IChildWebObject;
		this.childPositionClass = child instanceof ISupportBounds ? ISupportBounds.class : IChildWebObject.class;
	}

	@Override
	public void execute()
	{
		ISupportChilds initialParent = child.getParent();
		oldChild = child;

		oldParent = getFlattenedPersist(child.getParent());
		child = getOverridePersist(child);
		this.newParent = getFlattenedPersist(newParent == null ? child.getParent() : (ISupportChilds)getOverridePersist(newParent));
		IPersist superPersist = PersistHelper.getSuperPersist((ISupportExtendsID)newParent);

		if (hasChildPositionSupport)
		{
			ArrayList<IPersist> children = getChildrenSortedOnType(oldParent);
			oldIndex = children.indexOf(oldChild);
		}

		if (!initialParent.equals(superPersist))
		{
			oldParent.removeChild(oldChild);
		}

		if (hasChildPositionSupport)
		{
			ArrayList<IPersist> children = getChildrenSortedOnType(newParent);
			newParent.removeChild(child);
			children.remove(child);
			insertIdx = childPositionClass.isInstance(targetChild) ? children.indexOf(targetChild) : -1;
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
		ArrayList<IPersist> changes = new ArrayList<>();
		if (initialParent.equals(superPersist))
		{
			oldParent = newParent;//if it's an override, then on undo we just want to change the position
			//the following code can be improved if overriding the entire hierarchy is not needed
			//changed persist should be the parent and its children (it is necessary to also send the children, otherwise the model is gone)
			//changedPersist = newParent;
			//if we don't override the whole hierarchy this full refresh is not needed anymore
			IEditorReference[] editorRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
			for (IEditorReference editorRef : editorRefs)
			{
				IEditorPart editor = editorRef.getEditor(false);
				if (editor instanceof BaseVisualFormEditor)
				{
					BaseVisualFormEditorDesignPage activePage = ((BaseVisualFormEditor)editor).getGraphicaleditor();
					if (activePage instanceof RfbVisualFormEditorDesignPage) ((RfbVisualFormEditorDesignPage)activePage).refreshBrowserUrl(true);
					break;
				}
			}
			changes.add((((IFlattenedPersistWrapper< ? >)newParent).getWrappedPersist()));
			changes.addAll(getChildrenSortedOnType(newParent));
		}
		else
		{
			changes.add(child);
		}
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, changes);
	}

	private ISupportChilds getFlattenedPersist(ISupportChilds persist)
	{
		ISupportChilds flattenedPersist = persist;
		if (flattenedPersist instanceof Form)
		{
			FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(persist);
			flattenedPersist = flattenedSolution.getFlattenedForm(flattenedPersist);
		}
		if (flattenedPersist instanceof LayoutContainer && !(flattenedPersist instanceof FlattenedLayoutContainer))
		{
			flattenedPersist = new FlattenedLayoutContainer(ModelUtils.getEditingFlattenedSolution(persist), (LayoutContainer)flattenedPersist);
		}
		return flattenedPersist;
	}

	private IPersist getOverridePersist(IPersist persist)
	{
		if (form != null)
		{
			try
			{
				return ElementUtil.getOverridePersist(PersistContext.create(persist, form));
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
		return persist;
	}

	private void updateWithOrderedPosition(ArrayList<IPersist> children)
	{
		int counter = child instanceof ISupportBounds ? 1 : 0;
		for (IPersist p : children)
		{
			p = getOverridePersist(p);
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
			children.remove(insertIdx);
			if (oldIndex < children.size())
			{
				children.add(oldIndex, child);
			}
			else
			{
				children.add(child);
			}
			oldParent.addChild(child);
			updateWithOrderedPosition(children);
		}
		else
		{
			oldParent.addChild(child);
		}
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
