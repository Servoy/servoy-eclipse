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

import java.util.stream.Collectors;

import org.eclipse.gef.ui.actions.SelectAllAction;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.RfbVisualFormEditorDesignPage;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.LayoutContainer;
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

		if (part.getForm().isResponsiveLayout())
		{
			// Get the last zoomed container and select all its children if it is a CSSPositionContainer
			LayoutContainer lc = (LayoutContainer)((RfbVisualFormEditorDesignPage)part.getGraphicaleditor()).getShowedContainer();
			if (lc != null && "csspositioncontainer".equals(lc.getSpecName()))
			{
				selectionProvider.setSelection(new StructuredSelection(
					lc.getAllObjectsAsList()
						.stream()
						.map(persist -> PersistContext.create(persist, part.getForm())).collect(Collectors.toList())));
			}
		}
		else
		{
			// select them all.
			selectionProvider.setSelection(new StructuredSelection(
				Utils.asList(ModelUtils.getEditingFlattenedSolution(part.getForm()).getFlattenedForm(part.getForm()).getFormElementsSortedByFormIndex())
					.stream()
					.map(persist -> PersistContext.create(persist, part.getForm())).collect(Collectors.toList())));
		}
	}

	@Override
	public boolean isHandled()
	{
		return part.isDesignerContextActive();
	}
}
