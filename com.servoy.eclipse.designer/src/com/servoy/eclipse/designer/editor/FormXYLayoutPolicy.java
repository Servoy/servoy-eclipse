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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;

import com.servoy.eclipse.designer.actions.DistributeRequest;
import com.servoy.eclipse.designer.editor.VisualFormEditor.RequestType;
import com.servoy.eclipse.designer.editor.commands.ChangeBoundsCommand;
import com.servoy.eclipse.designer.editor.commands.FormPlaceElementCommand;
import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
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
	public static final String REQUEST_PROPERTY_PREFIX = "property:";

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
				CompoundCommand compoundCommand = new CompoundCommand();

				compoundCommand.add(new ChangeBoundsCommand((ISupportBounds)childEditPart.getModel(), (Rectangle)constraint));

				// set properties via request.extendedData
				Map<Object, Object> objectProperties = request.getExtendedData();
				if (childEditPart.getModel() instanceof IPersist && objectProperties != null && objectProperties.size() > 0)
				{
					IPersist persist = (IPersist)childEditPart.getModel();
					PersistPropertySource persistPropertySource = new PersistPropertySource(persist, persist, false);
					for (Map.Entry<Object, Object> entry : objectProperties.entrySet())
					{
						Object key = entry.getKey();
						if (key instanceof String && ((String)key).startsWith(REQUEST_PROPERTY_PREFIX))
						{
							SetValueCommand setCommand = new SetValueCommand();
							setCommand.setTarget(persistPropertySource);
							setCommand.setPropertyId(((String)key).substring(REQUEST_PROPERTY_PREFIX.length()));
							setCommand.setPropertyValue(entry.getValue());
							compoundCommand.add(setCommand);
						}
					}
				}
				return compoundCommand.unwrap();
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
	protected Command getCreateCommand(final CreateRequest request)
	{
		Command command = null;
		if (request.getNewObjectType() instanceof RequestType)
		{
			RequestType requestType = (RequestType)request.getNewObjectType();

			Map<Object, Object> extendedData = request.getExtendedData();
			if (request.getSize() != null)
			{
				extendedData.put("size", new java.awt.Dimension(request.getSize().width, request.getSize().height));
			}

			if (requestType.type == RequestType.TYPE_BUTTON)
			{
				command = new FormPlaceElementCommand(((FormGraphicalEditPart)getHost()).getPersist(), "button", requestType, extendedData, null,
					request.getLocation().getSWTPoint());
			}
			// TODO: add more

			// set the created object in the CreateRequest, so it can be selected afterwards
			if (request instanceof CreateElementRequest)
			{
				command = ((CreateElementRequest)request).chainSetFactoryObjectCommand(command);
			}
		}
		return command;
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
		//Sorting is also done according to leftmost, rightmost, topmost, bottommost and center point
		Arrays.sort(figs, new Comparator<ISupportBounds>()
		{
			public int compare(ISupportBounds o1, ISupportBounds o2)
			{
				int a, b;
				if (request.getDistribution() == DistributeRequest.Distribution.HORIZONTAL_SPACING ||
					request.getDistribution() == DistributeRequest.Distribution.HORIZONTAL_PACK)
				{
					a = o1.getLocation().x;
					b = o2.getLocation().x;
				}
				else if (request.getDistribution() == DistributeRequest.Distribution.VERTICAL_SPACING ||
					request.getDistribution() == DistributeRequest.Distribution.VERTICAL_PACK)
				{
					a = o1.getLocation().y;
					b = o2.getLocation().y;
				}
				else if (request.getDistribution() == DistributeRequest.Distribution.HORIZONTAL_CENTERS)
				{
					a = o1.getLocation().x + o1.getSize().width / 2;
					b = o2.getLocation().x + o2.getSize().width / 2;
				}
				else
				//VERTICAL_CENTERS
				{
					a = o1.getLocation().y + o1.getSize().height / 2;
					b = o2.getLocation().y + o2.getSize().height / 2;
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
		return new AlignmentfeedbackEditPolicy(parent);
	}
}
