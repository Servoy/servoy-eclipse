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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.tools.DragEditPartsTracker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.designer.property.IPersistEditPart;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Part;

public class FormPartGraphicalEditPart extends AbstractGraphicalEditPart implements IPersistEditPart, IPersistChangeListener
{
	protected IApplication application;

	int formWidth;
	private final boolean readonly;
	private final VisualFormEditor editorPart;

	public FormPartGraphicalEditPart(IApplication application, VisualFormEditor editorPart, Part part, boolean readonly)
	{
		this.application = application;
		this.editorPart = editorPart;
		this.readonly = readonly;
		setModel(part);
	}

	@Override
	protected List getModelChildren()
	{
		return Collections.EMPTY_LIST;
	}

	@Override
	protected void createEditPolicies()
	{
		if (!readonly)
		{
			installEditPolicy(EditPolicy.COMPONENT_ROLE, new FormPartEditPolicy());
		}
	}

	@Override
	protected IFigure createFigure()
	{
		Part part = getPersist();
		PartFigure fig = new PartFigure(editorPart);
		fig.setFont(FontResource.getDefaultFont(SWT.NONE, 0));
		if (readonly)
		{
			fig.setForegroundColor(ColorConstants.red);
		}

		updateFigure(part, fig);
		return fig;
	}

	protected void updateFigure(Part part, PartFigure fig)
	{
		Form flattenedForm = editorPart.getFlattenedForm();
		if (flattenedForm == null) return;

		formWidth = flattenedForm.getWidth();
		fig.setBounds(new Rectangle(0, part.getHeight(), formWidth, 20));
		fig.setText(part.getEditorName());
	}

	/**
	 * Form part has no children
	 */
	@Override
	protected EditPart createChild(Object child)
	{
		return null;
	}

	@Override
	protected void refreshVisuals()
	{
		super.refreshVisuals();
		updateFigure(getPersist(), (PartFigure)getFigure());
	}

	@Override
	public void activate()
	{
		// listen to changes to the form
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyModel.addPersistChangeListener(false, this);

		super.activate();
	}

	@Override
	public void deactivate()
	{
		// stop listening to changes to the form
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyModel.removePersistChangeListener(false, this);

		super.deactivate();
	}

	// If the form changed width we have to refresh
	public void persistChanges(Collection<IPersist> changes)
	{
		Form flattenedForm = editorPart.getFlattenedForm();
		if (flattenedForm != null && formWidth != flattenedForm.getWidth())
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					refresh();
				}
			});
		}
	}

	public Part getPersist()
	{
		return (Part)getModel();
	}

	public boolean isReadOnly()
	{
		return readonly;
	}

	public IFieldPositioner getFieldPositioner()
	{
		if (getParent() instanceof IPersistEditPart)
		{
			return ((IPersistEditPart)getParent()).getFieldPositioner();
		}
		return null;
	}


	@Override
	public DragTracker getDragTracker(Request request)
	{
		return new DragEditPartsTracker(this)
		{
			@Override
			protected void updateTargetRequest()
			{
				FormPartGraphicalEditPart currentPart = FormPartGraphicalEditPart.this;
				EditPart parentPart = currentPart.getParent();
				List<FormPartGraphicalEditPart> neighboursNode = new ArrayList<FormPartGraphicalEditPart>();

				for (Object children : parentPart.getChildren())
				{
					if (children instanceof FormPartGraphicalEditPart && children != currentPart)
					{
						neighboursNode.add((FormPartGraphicalEditPart)children);
					}
				}

				super.updateTargetRequest();
				limitChangeBoundsRequest((ChangeBoundsRequest)getTargetRequest(), neighboursNode);

			}

			@Override
			protected List createOperationSet()
			{
				List editParts = super.createOperationSet();
				List<Object> newEditParts = new ArrayList<Object>(editParts.size());
				for (Object editPart : editParts)
				{
					boolean foundParent = false;
					if (editPart instanceof IPersistEditPart)
					{
						IPersistEditPart persistEditpart = (IPersistEditPart)editPart;
						if (persistEditpart.isReadOnly())
						{
							continue;
						}
						IPersist persist = persistEditpart.getPersist();
						for (Object editPart2 : editParts)
						{
							if (editPart2 instanceof IPersistEditPart && ((IPersistEditPart)editPart2).getPersist() == persist.getParent())
							{
								foundParent = true;
							}
						}
					}
					if (!foundParent) newEditParts.add(editPart);
				}
				return newEditParts;
			}

			@Override
			protected void performDirectEdit()
			{
				return;
				// Disabled direct edit via drag tracker, it activates direct edit on single click on selected	  element;
				// direct edit is handled in FormSelectionTool on double-click 
			}

			@Override
			protected void performSelection()
			{
				super.performSelection();

			}
		};
	}

	/**
	 * Limit a change bounds request.
	 * 
	 * <p>
	 * Update the request when it updates the edit parts to move/resize beyond (0, 0)
	 * 
	 * @param targetRequest
	 * @param neighboursNode
	 * @param currentPart
	 */

	public static void limitChangeBoundsRequest(ChangeBoundsRequest targetRequest, List<FormPartGraphicalEditPart> neighboursNode)
	{

		Point moveDelta = targetRequest.getMoveDelta();
		Dimension sizeDelta = targetRequest.getSizeDelta();
		List editParts = targetRequest.getEditParts();
		FormPartGraphicalEditPart currentPart = null;
		if ((moveDelta != null && (moveDelta.x != 0 || moveDelta.y != 0)))
		{
			int minX = Integer.MAX_VALUE;
			int minY = Integer.MAX_VALUE;
			int yCorrection = 0;
			int xCorrection = 0;

			boolean checkComplete = true;
			for (Object editPart : targetRequest.getEditParts())
			{
				boolean localCheck = true;
				if (editPart instanceof FormPartGraphicalEditPart)
				{
					Rectangle bounds = ((FormPartGraphicalEditPart)editPart).getFigure().getBounds();
					if (bounds.x < minX) minX = bounds.x;
					if (bounds.y < minY) minY = bounds.y;

					xCorrection = (minX != Integer.MAX_VALUE && minX + moveDelta.x < 0) ? -minX - moveDelta.x : 0;
					yCorrection = (minY != Integer.MAX_VALUE && minY + moveDelta.y < 0) ? -minY - moveDelta.y : 0;
					localCheck = checkMovement(((FormPartGraphicalEditPart)editPart), neighboursNode, moveDelta.y);
				}

				checkComplete = checkComplete && localCheck;
			}

			if (checkComplete == true)
			{
				if (xCorrection != 0 || yCorrection != 0)
				{
					targetRequest.setMoveDelta(new Point(moveDelta.x + xCorrection, moveDelta.y + yCorrection));
					// do not allow resize beyond (0,0) 
					if (sizeDelta != null && (sizeDelta.width != 0 || sizeDelta.height != 0))
					{
						targetRequest.setSizeDelta(new Dimension(sizeDelta.width - xCorrection, sizeDelta.height - yCorrection));
					}
				}
			}
			else
			{
				targetRequest.setMoveDelta(new Point(0, 0));
			}

		}
	}

	private static boolean checkMovement(FormPartGraphicalEditPart currentPart, List<FormPartGraphicalEditPart> neighboursNode, int deltaY)
	{
		int part_type = currentPart.getPersist().getPartType();
		if (Part.BODY == part_type)
		{
			for (FormPartGraphicalEditPart nPart : neighboursNode)
			{
				int neighbourType = nPart.getPersist().getPartType();
				if (neighbourType == Part.HEADER || neighbourType == Part.TITLE_HEADER || neighbourType == Part.LEADING_GRAND_SUMMARY ||
					neighbourType == Part.LEADING_SUBSUMMARY)
				{
					if (nPart.getFigure().getBounds().y > currentPart.getFigure().getBounds().y + deltaY)
					{
						return false;
					}
				}

				else if (neighbourType == Part.TRAILING_SUBSUMMARY || neighbourType == Part.TRAILING_GRAND_SUMMARY || neighbourType == Part.FOOTER ||
					neighbourType == Part.TITLE_FOOTER)
				{
					if (nPart.getFigure().getBounds().y < currentPart.getFigure().getBounds().y + deltaY)
					{
						return false;
					}
				}

			}
		}
		else if (Part.TITLE_HEADER == part_type)
		{
			for (FormPartGraphicalEditPart nPart : neighboursNode)
			{
				int neighbourType = nPart.getPersist().getPartType();
				if (neighbourType == Part.HEADER || neighbourType == Part.LEADING_GRAND_SUMMARY || neighbourType == Part.LEADING_SUBSUMMARY ||
					neighbourType == Part.BODY || neighbourType == Part.TRAILING_SUBSUMMARY || neighbourType == Part.TRAILING_GRAND_SUMMARY ||
					neighbourType == Part.FOOTER || neighbourType == Part.TITLE_FOOTER)
				{
					if (nPart.getFigure().getBounds().y < currentPart.getFigure().getBounds().y + deltaY)
					{
						return false;
					}
				}
			}
		}
		else if (Part.HEADER == part_type)
		{
			for (FormPartGraphicalEditPart nPart : neighboursNode)
			{
				int neighbourType = nPart.getPersist().getPartType();
				if (neighbourType == Part.TITLE_HEADER)
				{
					if (nPart.getFigure().getBounds().y > currentPart.getFigure().getBounds().y + deltaY)
					{
						return false;
					}
				}
				else if (neighbourType == Part.LEADING_GRAND_SUMMARY || neighbourType == Part.LEADING_SUBSUMMARY || neighbourType == Part.BODY ||
					neighbourType == Part.TRAILING_SUBSUMMARY || neighbourType == Part.TRAILING_GRAND_SUMMARY || neighbourType == Part.FOOTER ||
					neighbourType == Part.TITLE_FOOTER)
				{
					if (nPart.getFigure().getBounds().y < currentPart.getFigure().getBounds().y + deltaY)
					{
						return false;
					}
				}
			}

		}
		else if (Part.LEADING_GRAND_SUMMARY == part_type)
		{
			for (FormPartGraphicalEditPart nPart : neighboursNode)
			{
				int neighbourType = nPart.getPersist().getPartType();
				if (neighbourType == Part.HEADER || neighbourType == Part.TITLE_HEADER)
				{
					if (nPart.getFigure().getBounds().y > currentPart.getFigure().getBounds().y + deltaY)
					{
						return false;
					}
				}
				else if (neighbourType == Part.LEADING_SUBSUMMARY || neighbourType == Part.BODY || neighbourType == Part.TRAILING_SUBSUMMARY ||
					neighbourType == Part.TRAILING_GRAND_SUMMARY || neighbourType == Part.FOOTER || neighbourType == Part.TITLE_FOOTER)
				{
					if (nPart.getFigure().getBounds().y < currentPart.getFigure().getBounds().y + deltaY)
					{
						return false;
					}
				}
			}

		}

		else if (Part.LEADING_SUBSUMMARY == part_type)
		{
			for (FormPartGraphicalEditPart nPart : neighboursNode)
			{
				int neighbourType = nPart.getPersist().getPartType();
				if (neighbourType == Part.HEADER || neighbourType == Part.TITLE_HEADER || neighbourType == Part.LEADING_GRAND_SUMMARY)
				{
					if (nPart.getFigure().getBounds().y > currentPart.getFigure().getBounds().y + deltaY)
					{
						return false;
					}
				}
				else if (neighbourType == Part.BODY || neighbourType == Part.TRAILING_SUBSUMMARY || neighbourType == Part.TRAILING_GRAND_SUMMARY ||
					neighbourType == Part.FOOTER || neighbourType == Part.TITLE_FOOTER)
				{
					if (nPart.getFigure().getBounds().y < currentPart.getFigure().getBounds().y + deltaY)
					{
						return false;
					}
				}
			}

		}

		else if (Part.TRAILING_SUBSUMMARY == part_type)
		{
			for (FormPartGraphicalEditPart nPart : neighboursNode)
			{
				int neighbourType = nPart.getPersist().getPartType();
				if (neighbourType == Part.HEADER || neighbourType == Part.TITLE_HEADER || neighbourType == Part.LEADING_GRAND_SUMMARY ||
					neighbourType == Part.LEADING_SUBSUMMARY || neighbourType == Part.BODY)
				{
					if (nPart.getFigure().getBounds().y > currentPart.getFigure().getBounds().y + deltaY)
					{
						return false;
					}
				}
				else if (neighbourType == Part.TRAILING_GRAND_SUMMARY || neighbourType == Part.FOOTER || neighbourType == Part.TITLE_FOOTER)
				{
					if (nPart.getFigure().getBounds().y < currentPart.getFigure().getBounds().y + deltaY)
					{
						return false;
					}
				}
			}

		}

		else if (Part.TRAILING_GRAND_SUMMARY == part_type)
		{
			for (FormPartGraphicalEditPart nPart : neighboursNode)
			{
				int neighbourType = nPart.getPersist().getPartType();
				if (neighbourType == Part.HEADER || neighbourType == Part.TITLE_HEADER || neighbourType == Part.LEADING_GRAND_SUMMARY ||
					neighbourType == Part.LEADING_SUBSUMMARY || neighbourType == Part.BODY || neighbourType == Part.TRAILING_SUBSUMMARY)
				{
					if (nPart.getFigure().getBounds().y > currentPart.getFigure().getBounds().y + deltaY)
					{
						return false;
					}
				}
				else if (neighbourType == Part.FOOTER || neighbourType == Part.TITLE_FOOTER)
				{
					if (nPart.getFigure().getBounds().y < currentPart.getFigure().getBounds().y + deltaY)
					{
						return false;
					}
				}
			}

		}

		else if (Part.FOOTER == part_type)
		{
			for (FormPartGraphicalEditPart nPart : neighboursNode)
			{
				int neighbourType = nPart.getPersist().getPartType();
				if (neighbourType == Part.HEADER || neighbourType == Part.TITLE_HEADER || neighbourType == Part.LEADING_GRAND_SUMMARY ||
					neighbourType == Part.LEADING_SUBSUMMARY || neighbourType == Part.BODY || neighbourType == Part.TRAILING_SUBSUMMARY ||
					neighbourType == Part.TRAILING_GRAND_SUMMARY)
				{

					if (nPart.getFigure().getBounds().y > currentPart.getFigure().getBounds().y + deltaY)
					{
						return false;
					}
				}
				else if (neighbourType == Part.TITLE_FOOTER)
				{
					if (nPart.getFigure().getBounds().y < currentPart.getFigure().getBounds().y + deltaY)
					{
						return false;
					}
				}
			}

		}

		else if (Part.TITLE_FOOTER == part_type)
		{
			for (FormPartGraphicalEditPart nPart : neighboursNode)
			{
				int neighbourType = nPart.getPersist().getPartType();
				if (neighbourType == Part.HEADER || neighbourType == Part.TITLE_HEADER || neighbourType == Part.LEADING_GRAND_SUMMARY ||
					neighbourType == Part.LEADING_SUBSUMMARY || neighbourType == Part.BODY || neighbourType == Part.TRAILING_SUBSUMMARY ||
					neighbourType == Part.TRAILING_GRAND_SUMMARY || neighbourType == Part.FOOTER)
				{

					if (nPart.getFigure().getBounds().y > currentPart.getFigure().getBounds().y + deltaY)
					{
						return false;
					}
				}

			}
		}

		return true;
	}
}
