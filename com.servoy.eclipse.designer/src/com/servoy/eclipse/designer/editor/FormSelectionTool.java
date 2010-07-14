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
package com.servoy.eclipse.designer.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.requests.DirectEditRequest;
import org.eclipse.gef.tools.SelectionTool;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.commands.ChangeBoundsCommand;
import com.servoy.eclipse.designer.editor.commands.RefreshingCommand;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.property.FormValueEditor;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.Tab;

/**
 * Selection tool in form designer, handles keyboard-arrow actions, optionally with shift/control.
 * 
 * @author rgansevles
 */

public class FormSelectionTool extends SelectionTool
{
	/**
	 * Handle double click.
	 * <p>
	 * On tab: open tab form in editor.
	 */
	@Override
	protected boolean handleDoubleClick(int button)
	{
		EditPart editpart = getTargetEditPart();
		if (editpart instanceof TabFormGraphicalEditPart)
		{
			Tab tab = (Tab)((TabFormGraphicalEditPart)editpart).getModel();
			FlattenedSolution flattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(tab);
			FormValueEditor formValueEditor = new FormValueEditor(flattenedSolution);
			if (formValueEditor.canEdit(new Integer(tab.getContainsFormID())))
			{
				formValueEditor.openEditor(new Integer(tab.getContainsFormID()));
			}
			return true;
		}

		Request directEditRequest = new DirectEditRequest();
		if (editpart != null && editpart.understandsRequest(directEditRequest))
		{
			editpart.performRequest(directEditRequest);
			return true;
		}

		return super.handleDoubleClick(button);
	}

	@Override
	protected boolean handleKeyDown(KeyEvent e)
	{
		if (performNudgeOrResize(e))
		{
			return true;
		}
		return super.handleKeyDown(e);
	}

	/**
	 * Nudge or resize edit parts by using the arrow keys.
	 * <p>
	 * shift key held: resize
	 * <p>
	 * control key held: magnitude 10
	 * 
	 * @param e event
	 * @return applicability
	 */
	protected boolean performNudgeOrResize(KeyEvent e)
	{
		switch (e.keyCode)
		{
			case SWT.ARROW_LEFT :
			case SWT.ARROW_RIGHT :
			case SWT.ARROW_UP :
			case SWT.ARROW_DOWN :
				// ok
				break;
			default :
				return false;
		}

		// shift -> resize, else move
		Request request;
		boolean resize = (e.stateMask & SWT.SHIFT) != 0;
		if (resize)
		{
			request = new Request(RequestConstants.REQ_RESIZE);
		}
		else
		{
			request = new Request(RequestConstants.REQ_MOVE);
		}

		final EditPartViewer viewer = getCurrentViewer();
		// check the selected edit parts if they understand the request.
		final List<EditPart> selectedEditParts = viewer.getSelectedEditParts();
		List<EditPart> applicableEditParts = new ArrayList<EditPart>(selectedEditParts.size());
		for (EditPart editPart : selectedEditParts)
		{
			if (editPart.understandsRequest(request) && editPart.getModel() instanceof ISupportBounds)
			{
				applicableEditParts.add(editPart);
			}
		}
		// Que?
		if (applicableEditParts.size() == 0)
		{
			return false;
		}

		int magnitude = 1;
		if ((e.stateMask & SWT.MOD1) != 0)
		{
			ServoyModelManager.getServoyModelManager().getServoyModel();
			DesignerPreferences designerPreferences = new DesignerPreferences(ServoyModel.getSettings());
			magnitude = designerPreferences.getStepSize();
		}

		// create a command for moving / resizing
		CompoundCommand command = new CompoundCommand();
		int x = 0;
		int y = 0;
		int width = 0;
		int height = 0;
		switch (e.keyCode)
		{
			case SWT.ARROW_LEFT :
				if (resize) width -= magnitude;
				else x -= magnitude;
				break;
			case SWT.ARROW_RIGHT :
				if (resize) width += magnitude;
				else x += magnitude;
				break;
			case SWT.ARROW_UP :
				if (resize) height -= magnitude;
				else y -= magnitude;
				break;
			case SWT.ARROW_DOWN :
				if (resize) height += magnitude;
				else y += magnitude;
				break;
		}

		applicableEditParts = DesignerUtil.removeChildEditParts(applicableEditParts);

		for (EditPart editPart : applicableEditParts)
		{
			ISupportBounds supportBounds = (ISupportBounds)editPart.getModel();
			command.add(new ChangeBoundsCommand(supportBounds, new Point(x, y), new Dimension(width, height)));
		}

		// execute on the command stack
		viewer.getEditDomain().getCommandStack().execute(new RefreshingCommand(command.unwrap())
		{
			@Override
			public void refresh(boolean haveExecuted)
			{
				viewer.setSelection(new StructuredSelection(selectedEditParts.toArray()));
			}

		});
		return true;
	}
}
