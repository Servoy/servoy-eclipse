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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.internal.GEFMessages;
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
 * An action to delete selected objects.
 */
public class CopyAction extends DesignerSelectionAction
{

	/**
	 * Constructs a <code>CopyAction</code> using the specified part.
	 * 
	 * @param part The part for this action
	 */
	public CopyAction(IWorkbenchPart part)
	{
		super(part, VisualFormEditor.REQ_COPY);
	}

	/**
	 * Initializes this action's text and images.
	 */
	@Override
	protected void init()
	{
		super.init();
		setText(GEFMessages.CopyAction_Label);
		setToolTipText(GEFMessages.CopyAction_Tooltip);
		setId(ActionFactory.COPY.getId());
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		setDisabledImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
		setEnabled(false);
	}

	/**
	 * When a parent edit part is selected select its all children also (if none of the children were selected).
	 */
	@Override
	protected List<EditPart> getSelectedElements(List<EditPart> selected)
	{
		if (selected == null)
		{
			return null;
		}
		Set<Object> selectedModels = new HashSet<Object>();
		for (EditPart editPart : selected)
		{
			selectedModels.add(editPart.getModel());
		}
		List<EditPart> newSelected = new ArrayList<EditPart>();
		for (EditPart editPart : selected)
		{
			newSelected.add(editPart);

			// add children if no children were selected
			Object model = editPart.getModel();
			if (model instanceof ISupportChilds && !(model instanceof Form))
			{
				Map editPartRegistry = ((GraphicalViewer)getWorkbenchPart().getAdapter(GraphicalViewer.class)).getEditPartRegistry();

				List<EditPart> childEditParts = new ArrayList<EditPart>();
				Iterator<IPersist> children = ((ISupportChilds)model).getAllObjects();
				while (children != null && children.hasNext())
				{
					IPersist child = children.next();
					if (selectedModels.contains(child))
					{
						// parent and child was selected, do not automatically add all children
						childEditParts = null;
						break;
					}

					EditPart childEditPart = (EditPart)editPartRegistry.get(child);
					if (childEditPart != null)
					{
						childEditParts.add(childEditPart);
					}
				}
				if (childEditParts != null)
				{
					newSelected.addAll(childEditParts);
				}
			}
		}
		return newSelected;
	}

	@Override
	protected void execute(Command command)
	{
		if (command == null || !command.canExecute()) return;
		// cannot undo (like copy command), run outside the command stack (will not make editor dirty)
		command.execute();
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

	@Override
	protected boolean calculateEnabled()
	{
		if (DesignerUtil.containsInheritedElement(getSelectedObjects())) return false;
		return super.calculateEnabled();
	}
}
