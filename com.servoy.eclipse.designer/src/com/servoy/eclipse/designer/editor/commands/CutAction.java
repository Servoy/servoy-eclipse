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

import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.IPersist;

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

	@Override
	protected boolean calculateEnabled()
	{
		List selected = getSelectedObjects();
		if (selected != null && !selected.isEmpty() && selected.get(0) instanceof EditPart)
		{
			for (int i = 0; i < selected.size(); i++)
			{
				EditPart object = (EditPart)selected.get(i);
				EditPart parent = object.getParent();
				if (parent != null && parent.getModel() instanceof IPersist &&
					ElementUtil.isReadOnlyFormElement((IPersist)parent.getModel(), object.getModel())) return false;
			}
		}
		return super.calculateEnabled();
	}
}
