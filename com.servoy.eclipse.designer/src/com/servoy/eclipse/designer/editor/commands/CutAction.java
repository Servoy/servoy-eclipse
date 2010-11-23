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
package com.servoy.eclipse.designer.editor.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;

/**
 * An action to cut selected objects.
 */
public class CutAction extends DesignerSelectionAction
{
	/**
	 * Constructs a <code>CutAction</code> using the specified part.
	 * 
	 * @param part The part for this action
	 */
	public CutAction(IWorkbenchPart part)
	{
		super(part, VisualFormEditor.REQ_CUT);
	}

	/**
	 * Initializes this action's text and images.
	 */
	@Override
	protected void init()
	{
		super.init();
		setText("Cut");
		setToolTipText("Cut selected objects");
		setId(ActionFactory.CUT.getId());
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT));
		setDisabledImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT_DISABLED));
	}

	/**
	 * When a parent edit part is selected select its all children also.
	 */
	@Override
	protected List<EditPart> getSelectedElements(List<EditPart> selected)
	{
		if (selected == null)
		{
			return null;
		}
		List<EditPart> newSelected = new ArrayList<EditPart>();
		for (EditPart editPart : selected)
		{
			if (!newSelected.contains(editPart))
			{
				newSelected.add(editPart);
			}

			// add children
			Object model = editPart.getModel();
			if (model instanceof ISupportChilds && !(model instanceof Form))
			{
				Map editPartRegistry = ((GraphicalViewer)getWorkbenchPart().getAdapter(GraphicalViewer.class)).getEditPartRegistry();

				Iterator<IPersist> children = ((ISupportChilds)model).getAllObjects();
				while (children != null && children.hasNext())
				{
					EditPart childEditPart = (EditPart)editPartRegistry.get(children.next());
					if (childEditPart != null && !newSelected.contains(childEditPart))
					{
						newSelected.add(childEditPart);
					}
				}
			}
		}
		return newSelected;
	}

	@Override
	protected boolean calculateEnabled()
	{
		if (DesignerUtil.containsInheritedElement(getSelectedObjects())) return false;
		return super.calculateEnabled();
	}

	@Override
	public boolean isHandled()
	{
		if (getWorkbenchPart() instanceof VisualFormEditor)
		{
			return ((VisualFormEditor)getWorkbenchPart()).isDesignerContextActive();
		}
		return true;
	}
}
