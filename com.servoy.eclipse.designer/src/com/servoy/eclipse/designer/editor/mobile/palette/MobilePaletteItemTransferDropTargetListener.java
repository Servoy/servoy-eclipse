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

package com.servoy.eclipse.designer.editor.mobile.palette;

import java.util.List;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.dnd.TemplateTransfer;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor.RequestType;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileFormGraphicalEditPart;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileListGraphicalEditPart;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobilePartGraphicalEditPart;
import com.servoy.eclipse.designer.editor.palette.PaletteItemTransferDropTargetListener;
import com.servoy.eclipse.designer.editor.palette.RequestTypeCreationFactory;

/**
 * Drop target for elements from the palette.
 * 
 * @author rgansevles
 *
 */
public class MobilePaletteItemTransferDropTargetListener extends PaletteItemTransferDropTargetListener
{

	/**
	 * @param viewer
	 */
	public MobilePaletteItemTransferDropTargetListener(EditPartViewer viewer, IWorkbenchPart workbenchPart)
	{
		super(viewer, workbenchPart);
	}

	@Override
	public boolean isEnabled(DropTargetEvent event)
	{
		boolean enabled = super.isEnabled(event);
		if (enabled)
		{
			CreationFactory factory = getFactory(TemplateTransfer.getInstance().getTemplate());
			if (factory instanceof RequestTypeCreationFactory && ((RequestTypeCreationFactory)factory).getObjectType() instanceof RequestType)
			{
				int type = ((RequestType)((RequestTypeCreationFactory)factory).getObjectType()).type;
				if (type == RequestType.TYPE_PART)
				{
					return true;
				}
			}
			EditPart ep = getViewer().findObjectAt(getDropLocation());
			if (!(ep instanceof MobileFormGraphicalEditPart))
			{
				EditPart tempPart = ep;
				while (tempPart != null)
				{
					if (tempPart.getParent() instanceof MobileFormGraphicalEditPart)
					{
						ep = tempPart.getParent();
						break;
					}
					tempPart = tempPart.getParent();
				}
				if (tempPart == null && ep.getChildren().size() > 0)
				{
					ep = (EditPart)ep.getChildren().get(0);
				}
			}
			if (ep instanceof MobileFormGraphicalEditPart)
			{
				boolean hasListForm = false;
				Point dropLocation = getDropLocation();
				for (EditPart editPart : (List<EditPart>)ep.getChildren())
				{
					if (editPart instanceof MobilePartGraphicalEditPart)
					{
						if (((MobilePartGraphicalEditPart)editPart).getFigure().getBounds().contains(dropLocation))
						{
							return true;
						}
					}
					else if (editPart instanceof MobileListGraphicalEditPart && !((MobileListGraphicalEditPart)editPart).isInsetList())
					{
						hasListForm = true;
					}
				}
				if (hasListForm) return false;
			}
		}
		return enabled;
	}
}
