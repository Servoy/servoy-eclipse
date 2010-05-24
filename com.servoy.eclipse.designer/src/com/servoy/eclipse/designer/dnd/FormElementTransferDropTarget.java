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
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;

import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.DataRequest;
import com.servoy.eclipse.dnd.FormElementTransfer;
import com.servoy.eclipse.ui.node.UserNodeListDragSourceListener;

/**
 * @author jcompagner
 * 
 */
public class FormElementTransferDropTarget extends TransferDropTargetAdapter
{
	private final GraphicalViewer graphicalViewer;

	public FormElementTransferDropTarget(GraphicalViewer graphicalViewer)
	{
		this.graphicalViewer = graphicalViewer;
	}

	/**
	 * @see org.eclipse.jface.util.TransferDropTargetListener#getTransfer()
	 */
	public Transfer getTransfer()
	{
		return FormElementTransfer.getInstance();
	}

	/**
	 * @see org.eclipse.swt.dnd.DropTargetListener#drop(org.eclipse.swt.dnd.DropTargetEvent)
	 */
	public void drop(DropTargetEvent event)
	{
		org.eclipse.swt.graphics.Point swtPoint = graphicalViewer.getControl().toControl(event.x, event.y);
		Point point = new Point(swtPoint.x, swtPoint.y);
		EditPart editPart = graphicalViewer.findObjectAt(point);

		if (editPart == graphicalViewer.getRootEditPart())
		{
			editPart = graphicalViewer.getContents();
		}

		// first try REQ_DROP_LINK
		DataRequest dropReq = new DataRequest(VisualFormEditor.REQ_DROP_LINK, point, UserNodeListDragSourceListener.dragObjects);
		Command command = editPart.getCommand(dropReq);
		if (command == null || !command.canExecute())
		{
			// then try REQ_DROP_COPY
			dropReq.setType(VisualFormEditor.REQ_DROP_COPY);
			command = editPart.getCommand(dropReq);
		}
		if (command == null || !command.canExecute())
		{
			event.detail = DND.DROP_NONE;
		}
		else
		{
			graphicalViewer.getEditDomain().getCommandStack().execute(command);
		}

	}

	@Override
	public void dragOver(DropTargetEvent event)
	{
		if (FormElementTransfer.getInstance().isSupportedType(event.currentDataType))
		{
			org.eclipse.swt.graphics.Point swtPoint = graphicalViewer.getControl().toControl(event.x, event.y);
			Point point = new Point(swtPoint.x, swtPoint.y);
			EditPart editPart = graphicalViewer.findObjectAt(point);
			if (editPart == graphicalViewer.getRootEditPart())
			{
				editPart = graphicalViewer.getContents();
			}
			// note. if setDropObject() call can be avoided, we do not need the UserNodeListDragSourceListener.dragObjects hack!
			DataRequest dropReq = new DataRequest(VisualFormEditor.REQ_DROP_LINK, point, UserNodeListDragSourceListener.dragObjects);
			if (editPart.understandsRequest(dropReq))
			{
				event.detail = DND.DROP_LINK;
			}
			else
			{
				dropReq.setType(VisualFormEditor.REQ_DROP_COPY);
				if (editPart.understandsRequest(dropReq))
				{
					event.detail = DND.DROP_COPY;
				}
				else
				{
					event.detail = DND.DROP_NONE;
				}
			}
		}
	}

	/**
	 * @see org.eclipse.jface.util.TransferDropTargetListener#isEnabled(org.eclipse.swt.dnd.DropTargetEvent)
	 */
	public boolean isEnabled(DropTargetEvent event)
	{
		return UserNodeListDragSourceListener.dragObjects != null;
	}

}
