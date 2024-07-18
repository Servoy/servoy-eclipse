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
import java.util.Iterator;
import java.util.List;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.DirectEditRequest;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.TraverseEvent;

import com.servoy.eclipse.designer.editor.commands.SelectModelsCommandWrapper;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.property.FormValueEditor;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.Tab;

/**
 * Selection tool in form designer, handles keyboard-arrow actions, optionally with shift/control.
 *
 * @author rgansevles
 */

public class FormSelectionTool extends PanningSelectionTool
{
	private final BaseVisualFormEditor editorPart;

	/**
	 * @param editorPart
	 */
	public FormSelectionTool(BaseVisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
	}

	@Override
	protected boolean handleViewerExited()
	{
		// on mac, the viewer-exited event kills selecting elements when marquee goes outside viewer.
		return false;
	}

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
			FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(tab);
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

	@Override
	protected void handleKeyTraversed(TraverseEvent event)
	{
		if (performElementTraverse(event))
		{
			event.doit = false;
		}
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
		boolean resize = (e.stateMask & SWT.SHIFT) != 0;

		final EditPartViewer viewer = getCurrentViewer();

		int magnitude = 1;
		if ((e.stateMask & SWT.ALT) != 0)
		{
			magnitude = new DesignerPreferences().getLargeStepSize();
		}
		else if ((e.stateMask & SWT.MOD1) != 0)
		{
			magnitude = new DesignerPreferences().getStepSize();
		}

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

		ChangeBoundsRequest request = new ChangeBoundsRequest(resize ? RequestConstants.REQ_RESIZE : RequestConstants.REQ_MOVE);
		request.setMoveDelta(new Point(x, y));
		request.setSizeDelta(new Dimension(width, height));

		// check the selected edit parts if they understand the request.
		List< ? extends EditPart> selectedEditParts = viewer.getSelectedEditParts();
		List<EditPart> applicableEditParts = new ArrayList<EditPart>(selectedEditParts.size());
		for (EditPart editPart : selectedEditParts)
		{
			if (editPart.understandsRequest(request))
			{
				applicableEditParts.add(editPart);
			}
		}
		// Que?
		if (applicableEditParts.size() == 0)
		{
			return false;
		}

		applicableEditParts = DesignerUtil.removeChildEditParts(applicableEditParts);

		// create a command for moving / resizing
		CompoundCommand command = new CompoundCommand();
		for (EditPart editPart : applicableEditParts)
		{
			command.add(editPart.getCommand(request));
		}

		// execute on the command stack
		viewer.getEditDomain().getCommandStack().execute(new SelectModelsCommandWrapper(getCurrentViewer(), applicableEditParts, command.unwrap()));
		return true;
	}

	/**
	 * Traverse selected elements according to tab sequence.
	 * <p>
	 * shift key held: traverse backward
	 *
	 * @param e event
	 * @return applicability
	 */
	protected boolean performElementTraverse(KeyEvent e)
	{
		if (e.keyCode != SWT.TAB)
		{
			return false;
		}

		// find a ISupportTabSeq in the selection, when multiple found, just pick one.
		ISupportTabSeq field = null;
		for (EditPart editPart : ((List<EditPart>)getCurrentViewer().getSelectedEditParts()))
		{
			if (editPart.getModel() instanceof ISupportTabSeq)
			{
				field = (ISupportTabSeq)editPart.getModel();
			}
		}
		if (field == null)
		{
			return false;
		}

		ISupportTabSeq next = findNextField(field,
			ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).getFlattenedForm(editorPart.getForm()).getTabSeqElementsByTabOrder(),
			(e.stateMask & SWT.SHIFT) == 0);
		if (next == null)
		{
			return false;
		}

		Object nextEditPart = getCurrentViewer().getEditPartRegistry().get(next);
		if (nextEditPart == null)
		{
			return false;
		}

		getCurrentViewer().setSelection(new StructuredSelection(nextEditPart));

		return true;
	}

	protected static ISupportTabSeq findNextField(ISupportTabSeq field, Iterator<ISupportTabSeq> fieldsByTabOrder, boolean forward)
	{
		ISupportTabSeq prev = null;
		ISupportTabSeq first = null;
		while (fieldsByTabOrder.hasNext())
		{
			ISupportTabSeq next = fieldsByTabOrder.next();
			if (next.equals(field))
			{
				if (forward)
				{
					if (fieldsByTabOrder.hasNext())
					{
						return fieldsByTabOrder.next();
					}
					return first;
				}

				// backward
				if (prev != null)
				{
					return prev;
				}

				// go to last
				ISupportTabSeq last = null;
				while (fieldsByTabOrder.hasNext())
				{
					last = fieldsByTabOrder.next();
				}
				return last;
			}

			// search further
			if (first == null)
			{
				first = next;
			}
			prev = next;
		}

		// not found
		return null;
	}
}
