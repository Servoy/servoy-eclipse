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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Handle;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.editpolicies.ResizableEditPolicy;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.handles.MoveHandle;
import org.eclipse.gef.handles.ResizeHandle;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.tools.ResizeTracker;

import com.servoy.eclipse.designer.actions.DistributeRequest;
import com.servoy.eclipse.designer.editor.commands.ChangeBoundsCommand;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.Part;

/**
 * layout policy for move/resize in form designer.
 * 
 * @author rgansevles
 */

public class FormXYLayoutPolicy extends XYLayoutEditPolicy
{
	private final FormGraphicalEditPart parent;

	public FormXYLayoutPolicy(FormGraphicalEditPart parent)
	{
		this.parent = parent;
	}

	@Override
	protected Command createChangeConstraintCommand(ChangeBoundsRequest request, EditPart childEditPart, Object constraint)
	{
		if (ElementUtil.isReadOnlyFormElement((Form)parent.getModel(), childEditPart.getModel()))
		{
			return null;
		}
		if (childEditPart instanceof GraphicalEditPart && constraint instanceof Rectangle)
		{
			if (childEditPart.getModel() instanceof ISupportBounds && constraint instanceof Rectangle)
			{
				return new ChangeBoundsCommand((ISupportBounds)childEditPart.getModel(), (Rectangle)constraint);
			}
		}
		return super.createChangeConstraintCommand(request, childEditPart, constraint);
	}

	@Override
	protected Command createChangeConstraintCommand(EditPart child, Object constraint)
	{
		return null;
	}

	@Override
	protected Command getCreateCommand(CreateRequest request)
	{
		return null;
	}

	@Override
	public Command getCommand(Request request)
	{
		if (VisualFormEditor.REQ_DISTRIBUTE.equals(request.getType()))
		{
			return getDistributeChildrenCommand((DistributeRequest)request);
		}
		return super.getCommand(request);
	}

	protected Command getDistributeChildrenCommand(final DistributeRequest request)
	{
		List editParts = request.getEditParts();
		if (editParts == null || editParts.size() < 3)
		{
			return null;
		}

		int packGap = 8; // TODO: preferences
		int size = editParts.size();

		ISupportBounds[] figs = new ISupportBounds[size];
		int e = 0;
		for (Object editPart : editParts)
		{
			Object model = ((EditPart)editPart).getModel();
			if (!(model instanceof ISupportBounds))
			{
				return null;
			}
			figs[e++] = (ISupportBounds)model;
		}

		// find the bbox of all selected objects
		Rectangle _bbox = null;
		int leftMostCenter = Integer.MAX_VALUE;
		int rightMostCenter = 0;
		int topMostCenter = Integer.MAX_VALUE;
		int bottomMostCenter = 0;

		int totalWidth = 0, totalHeight = 0;

		for (ISupportBounds supportBounds : figs)
		{
			Rectangle r = new Rectangle(supportBounds.getLocation().x, supportBounds.getLocation().y, supportBounds.getSize().width,
				supportBounds.getSize().height);
			_bbox = _bbox == null ? r : _bbox.getUnion(r);
			leftMostCenter = Math.min(leftMostCenter, r.x + r.width / 2);
			rightMostCenter = Math.max(rightMostCenter, r.x + r.width / 2);
			topMostCenter = Math.min(topMostCenter, r.y + r.height / 2);
			bottomMostCenter = Math.max(bottomMostCenter, r.y + r.height / 2);

			// find the sum of the widths and heights of all selected objects
			totalWidth += supportBounds.getSize().width;
			totalHeight += supportBounds.getSize().height;
		}

		float gap = 0, oncenter = 0;
		float xNext = 0, yNext = 0;

		switch (request.getDistribution())
		{
			case HORIZONTAL_SPACING :
				xNext = _bbox.x;
				gap = (_bbox.width - totalWidth) / Math.max(size - 1, 1);
				break;
			case HORIZONTAL_CENTERS :
				xNext = leftMostCenter;
				oncenter = (rightMostCenter - leftMostCenter) / Math.max(size - 1, 1);
				break;
			case HORIZONTAL_PACK :
				xNext = _bbox.x;
				gap = packGap;
				break;
			case VERTICAL_SPACING :
				yNext = _bbox.y;
				gap = (_bbox.height - totalHeight) / Math.max(size - 1, 1);
				break;
			case VERTICAL_CENTERS :
				yNext = topMostCenter;
				oncenter = (bottomMostCenter - topMostCenter) / Math.max(size - 1, 1);
				break;
			case VERTICAL_PACK :
				yNext = _bbox.y;
				gap = packGap;
				break;
		}

		//sort top-to-bottom or left-to-right, this maintains visual order when we set the coordinates
		Arrays.sort(figs, new Comparator<ISupportBounds>()
		{
			public int compare(ISupportBounds o1, ISupportBounds o2)
			{
				int a, b;
				if (request.getDistribution() == DistributeRequest.Distribution.HORIZONTAL_SPACING ||
					request.getDistribution() == DistributeRequest.Distribution.HORIZONTAL_CENTERS ||
					request.getDistribution() == DistributeRequest.Distribution.HORIZONTAL_PACK)
				{
					a = o1.getLocation().x;
					b = o2.getLocation().x;
				}
				else
				{
					a = o1.getLocation().y;
					b = o2.getLocation().y;
				}
				if (a > b) return 1;
				if (a < b) return -1;
				return 0;
			}
		});

		Dimension noResize = new Dimension(0, 0);
		CompoundCommand distributeCommand = new CompoundCommand("distribute");
		for (ISupportBounds supportBounds : figs)
		{
			Point moveDelta = null;
			switch (request.getDistribution())
			{
				case HORIZONTAL_SPACING :
				case HORIZONTAL_PACK :
					moveDelta = new Point((int)xNext - supportBounds.getLocation().x, 0);
					xNext += supportBounds.getSize().width + gap;
					break;
				case HORIZONTAL_CENTERS :
					moveDelta = new Point((int)xNext - supportBounds.getSize().width / 2 - supportBounds.getLocation().x, 0);
					xNext += oncenter;
					break;
				case VERTICAL_SPACING :
				case VERTICAL_PACK :
					moveDelta = new Point(0, (int)yNext - supportBounds.getLocation().y);
					yNext += supportBounds.getSize().height + gap;
					break;
				case VERTICAL_CENTERS :
					moveDelta = new Point(0, (int)yNext - supportBounds.getSize().height / 2 - supportBounds.getLocation().y);
					yNext += oncenter;
					break;
			}
			distributeCommand.add(new ChangeBoundsCommand(supportBounds, moveDelta, noResize));
		}

		return distributeCommand.unwrap();
	}

	@Override
	protected EditPolicy createChildEditPolicy(EditPart child)
	{
		if (child.getModel() instanceof Part)
		{
			return new DragFormPartPolicy();
		}
		return new ResizableEditPolicy()
		{
			@Override
			protected List<Handle> createSelectionHandles()
			{
				List<Handle> list = new ArrayList<Handle>();
				GraphicalEditPart part = (GraphicalEditPart)getHost();
				list.add(new MoveHandle(part));
				list.add(createResizeHandle(part, PositionConstants.EAST));
				list.add(createResizeHandle(part, PositionConstants.SOUTH_EAST));
				list.add(createResizeHandle(part, PositionConstants.SOUTH));
				list.add(createResizeHandle(part, PositionConstants.SOUTH_WEST));
				list.add(createResizeHandle(part, PositionConstants.WEST));
				list.add(createResizeHandle(part, PositionConstants.NORTH_WEST));
				list.add(createResizeHandle(part, PositionConstants.NORTH));
				list.add(createResizeHandle(part, PositionConstants.NORTH_EAST));

				return list;
			}

			protected Handle createResizeHandle(GraphicalEditPart owner, final int direction)
			{
				return new ResizeHandle(owner, direction)
				{
					@Override
					protected DragTracker createDragTracker()
					{
						return new ResizeTracker(getOwner(), direction)
						{
							@Override
							protected void updateSourceRequest()
							{
								super.updateSourceRequest();
								BasePersistGraphicalEditPart.limitChangeBoundsRequest((ChangeBoundsRequest)getSourceRequest());
							}
						};
					}
				};
			}
		};
	}
}
