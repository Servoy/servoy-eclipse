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

import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.SelectModelsCommandWrapper;

/**
 * Base drop target for elements in the form editor.
 *  
 * @author rgansevles
 * 
 */
public class ElementTransferDropTarget extends AbstractTransferDropTargetListener
{
	private SnapToHelper helper;
	private final IWorkbenchPart workbenchPart;

	public ElementTransferDropTarget(EditPartViewer viewer, Transfer xfer, IWorkbenchPart workbenchPart)
	{
		super(viewer, xfer);
		this.workbenchPart = workbenchPart;
	}

	@Override
	protected void unload()
	{
		helper = null;
		super.unload();
	}

	@Override
	protected void handleDragOver()
	{
		getCurrentEvent().detail = VisualFormEditor.REQ_DROP_LINK.equals(getTargetRequest().getType()) ? DND.DROP_LINK : DND.DROP_COPY;
		super.handleDragOver();
	}

	@Override
	protected void handleDrop()
	{
		super.handleDrop();
		// active the part that was dropped on
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(workbenchPart);
	}

	@Override
	public void dragLeave(DropTargetEvent event)
	{
		setCurrentEvent(event);
		// do not call unload() here because dragLeave is also called when the drop is done
		eraseTargetFeedback();
	}

	@Override
	protected void updateTargetRequest()
	{
		Request targetRequest = getTargetRequest();
		if (targetRequest instanceof CreateRequest)
		{
			CreateRequest request = (CreateRequest)targetRequest;
			Point loq = getDropLocation();
			Rectangle bounds = new Rectangle(loq, request.getSize());
			request.setLocation(loq);
			request.getExtendedData().clear();
			if (!isAltKeyDown() && getSnapToHelper() != null)
			{
				PrecisionRectangle baseRect = new PrecisionRectangle(bounds);
				PrecisionRectangle result = baseRect.getPreciseCopy();
				getSnapToHelper().snapRectangle(request, PositionConstants.NSEW, baseRect, result);
				request.setLocation(result.getLocation());
				request.setSize(result.getSize());
			}
		}
	}

	/**
	 * @return
	 */
	protected SnapToHelper getSnapToHelper()
	{
		if (helper == null && getTargetEditPart() != null)
		{
			EditPart editPart = getTargetEditPart();
			while (helper == null)
			{
				helper = (SnapToHelper)editPart.getAdapter(SnapToHelper.class);
				if (helper == null)
				{
					editPart = editPart.getParent();
				}
			}

		}
		return helper;
	}

	public boolean isAltKeyDown()
	{
		return false; // TODO: investigate how alt-key down can be detected
	}

	@Override
	protected Command getCommand()
	{
		Command command = super.getCommand();
		return command == null ? null : new SelectModelsCommandWrapper(getViewer(), getTargetEditPart(), command);
	}
}
