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
package com.servoy.eclipse.designer.dnd;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.swt.dnd.DropTargetEvent;

import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.CreateDropRequest;
import com.servoy.eclipse.designer.editor.commands.DataRequest;
import com.servoy.eclipse.designer.editor.palette.RequestTypeCreationFactory;
import com.servoy.eclipse.dnd.FormElementTransfer;
import com.servoy.eclipse.ui.node.UserNodeListDragSourceListener;

/**
 * Drop target for elements from the solution explorer tree element list.
 *  
 * @author jcompagner, rgansevles
 * 
 */
public class FormElementTransferDropTarget extends ElementTransferDropTarget
{
	public FormElementTransferDropTarget(EditPartViewer viewer)
	{
		super(viewer, FormElementTransfer.getInstance());
	}

	@Override
	public boolean isEnabled(DropTargetEvent event)
	{
		return UserNodeListDragSourceListener.dragObjects != null && super.isEnabled(event);
	}

	@Override
	protected Request createTargetRequest()
	{
		if (UserNodeListDragSourceListener.dragObjects != null && getCurrentEvent() != null && getTransfer().isSupportedType(getCurrentEvent().currentDataType))
		{
			org.eclipse.swt.graphics.Point swtPoint = getViewer().getControl().toControl(getCurrentEvent().x, getCurrentEvent().y);
			Point point = new Point(swtPoint.x, swtPoint.y);
			EditPart editPart = getViewer().findObjectAt(point);
			if (editPart == getViewer().getRootEditPart())
			{
				editPart = getViewer().getContents();
			}
			// note. if setDropObject() call can be avoided, we do not need the UserNodeListDragSourceListener.dragObjects hack!
			DataRequest dropReq = new DataRequest(VisualFormEditor.REQ_DROP_LINK, point, UserNodeListDragSourceListener.dragObjects);
			if (editPart.understandsRequest(dropReq))
			{
				return dropReq;
			}

			RequestTypeCreationFactory factory = new RequestTypeCreationFactory(VisualFormEditor.REQ_DROP_COPY);
			CreateDropRequest createRequest = new CreateDropRequest(VisualFormEditor.REQ_DROP_COPY, factory, UserNodeListDragSourceListener.dragObjects);
			createRequest.setLocation(point);
			createRequest.setSize(factory.getDefaultElementSize(UserNodeListDragSourceListener.dragObjects[0]));
			return createRequest;
		}

		return null;
	}
}
