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
package com.servoy.eclipse.designer.editor.rfb.actions;

import org.eclipse.gef.ui.actions.SelectAllAction;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.j2db.util.Utils;

/**
 * Select-all action with logic for selecting elements based on type of the already selected elements.
 *
 * @author jcompagner
 */

public class FixedSelectAllAction extends SelectAllAction
{
	private final BaseVisualFormEditor part;
	private final ISelectionProvider selectionProvider;

	public FixedSelectAllAction(BaseVisualFormEditor part, ISelectionProvider selectionProvider)
	{
		super(part);
		this.part = part;
		this.selectionProvider = selectionProvider;
	}

	@Override
	public void run()
	{
		IStructuredSelection selectedElements = (IStructuredSelection)selectionProvider.getSelection();
		if (!selectedElements.isEmpty())
		{
			// TODO select same type of component/bean

//			for (IPersist persist : Utils.<IPersist> iterate(selectedElements.iterator()))
//			{
//				for (int i = 0; i < selectedElements.size(); i++)
//				{
//					if (selectedElements.get(i) instanceof PersistGraphicalEditPart && persist != null)
//					{
//						if (persist.getTypeID() != ((IPersist)((PersistGraphicalEditPart)selectedElements.get(i)).getModel()).getTypeID())
//						{
//							persist = null;
//						}
//					}
//				}
//				List selection = new ArrayList();
//				if (persist != null)
//				{
//					for (int i = 0; i < elements.size(); i++)
//					{
//						if (elements.get(i) instanceof PersistGraphicalEditPart &&
//							persist.getTypeID() == ((IPersist)((PersistGraphicalEditPart)elements.get(i)).getModel()).getTypeID())
//						{
//							if (persist.getTypeID() == IRepository.GRAPHICALCOMPONENTS)
//							{
//								IPersist currentPersist = (IPersist)((PersistGraphicalEditPart)elements.get(i)).getModel();
//								int id1 = ((GraphicalComponent)persist).getOnActionMethodID();
//								int id2 = ((GraphicalComponent)currentPersist).getOnActionMethodID();
//								if (!((id1 == 0 && id2 == 0) || ((id1 > 0 || id1 == -1) && (id2 > 0 || id2 == -1))))
//								{
//									continue;
//								}
//							}
//							selection.add(elements.get(i));
//						}
//					}
//					viewer.setSelection(new StructuredSelection(selection));
//					return;
//				}
//			}
		}

		// select them all.
		selectionProvider.setSelection(new StructuredSelection(Utils.asList(part.getForm().getFormElementsSortedByFormIndex())));
	}

	@Override
	public boolean isHandled()
	{
		return part.isDesignerContextActive();
	}
}
