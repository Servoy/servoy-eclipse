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
import org.eclipse.gef.requests.CreationFactory;

import com.servoy.eclipse.designer.actions.DistributeRequest;
import com.servoy.eclipse.designer.actions.DistributeRequest.Distribution;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor.RequestType;
import com.servoy.eclipse.designer.editor.commands.ChangeBoundsCommand;
import com.servoy.eclipse.designer.editor.commands.FormPlaceElementCommand;
import com.servoy.eclipse.designer.editor.commands.FormPlaceFieldCommand;
import com.servoy.eclipse.designer.editor.commands.FormPlacePortalCommand;
import com.servoy.eclipse.designer.editor.commands.SelectModelsCommandWrapper;
import com.servoy.eclipse.designer.editor.palette.RequestTypeCreationFactory;
import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
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

	private AlignmentFeedbackHelper alignmentFeedbackHelper;

	private final IApplication application;

	public FormXYLayoutPolicy(IApplication application, FormGraphicalEditPart parent)
	{
		this.application = application;
		this.parent = parent;
	}

	@Override
	protected Command createChangeConstraintCommand(ChangeBoundsRequest request, EditPart childEditPart, Object constraint)
	{
		if (childEditPart instanceof GraphicalEditPart && constraint instanceof Rectangle)
		{
			if (childEditPart.getModel() instanceof ISupportBounds && constraint instanceof Rectangle)
			{
				CompoundCommand compoundCommand = new CompoundCommand();

				Rectangle oldBounds = ((GraphicalEditPart)childEditPart).getFigure().getBounds();
				Rectangle newBounds = (Rectangle)constraint;

				compoundCommand.add(new ChangeBoundsCommand(childEditPart, new Point(newBounds.x - oldBounds.x, newBounds.y - oldBounds.y), new Dimension(
					newBounds.width - oldBounds.width, newBounds.height - oldBounds.height)));

				// set properties via request.extendedData
				Map<Object, Object> objectProperties = request.getExtendedData();
				if (childEditPart.getModel() instanceof IPersist && objectProperties != null && objectProperties.size() > 0)
				{
					IPersist persist = (IPersist)childEditPart.getModel();
					compoundCommand.add(SetValueCommand.createSetPropertiesCommand(
						PersistPropertySource.createPersistPropertySource(persist, parent.getPersist(), false), objectProperties));
				}
				return compoundCommand.unwrap();
			}
		}
		return super.createChangeConstraintCommand(request, childEditPart, constraint);
	}

	@Override
	protected Command getCreateCommand(final CreateRequest request)
	{
		Command command = null;
		if (request.getNewObjectType() instanceof RequestType)
		{
			RequestType requestType = (RequestType)request.getNewObjectType();

			Map<Object, Object> extendedData = request.getExtendedData();

			Object data = null;
			if (request instanceof CreateElementRequest)
			{
				CreationFactory factory = ((CreateElementRequest)request).getFactory();
				if (factory instanceof RequestTypeCreationFactory)
				{
					data = ((RequestTypeCreationFactory)factory).getData();
					extendedData.putAll(((RequestTypeCreationFactory)factory).getExtendedData());
				}
			}

			Point loc = request.getLocation();
			if (loc == null)
			{
				return null;
			}
			loc = loc.getCopy();
			getHostFigure().translateToRelative(loc);
			Dimension size = request.getSize();
			if (size != null)
			{
				size = size.getCopy();
				getHostFigure().translateToRelative(size);
			}

			Form form = ((FormGraphicalEditPart)getHost()).getPersist();
			if (requestType.type == RequestType.TYPE_BUTTON || requestType.type == RequestType.TYPE_LABEL || requestType.type == RequestType.TYPE_TEMPLATE ||
				requestType.type == RequestType.TYPE_BEAN || requestType.type == RequestType.TYPE_TAB || requestType.type == RequestType.TYPE_SHAPE)
			{
				command = new FormPlaceElementCommand(application, form, data, requestType, extendedData, null, loc.getSWTPoint(), size, parent.getPersist());
			}
			else if (requestType.type == RequestType.TYPE_PORTAL)
			{
				command = new FormPlacePortalCommand(application, form, data, requestType, extendedData, null, loc.getSWTPoint(), size, false, false, false,
					false, parent.getPersist());
			}
			else if (requestType.type == RequestType.TYPE_FIELD)
			{
				command = new FormPlaceFieldCommand(application, form, form, data, requestType, extendedData, null, loc.getSWTPoint(), size, false, false,
					false, false, false, parent.getPersist());
			}


			// set the created object in the CreateRequest, so it can be selected afterwards
			if (request instanceof CreateElementRequest)
			{
				command = ((CreateElementRequest)request).chainSetFactoryObjectCommand(command);

			}

			if (command != null)
			{
				command = new SelectModelsCommandWrapper(parent.getViewer(), (EditPart)null, command);
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

		if (BaseVisualFormEditor.REQ_DROP_COPY.equals(request.getType()) && request instanceof CreateElementRequest &&
			((CreateRequest)request).getLocation() != null)
		{
			RequestTypeCreationFactory factory = (RequestTypeCreationFactory)((CreateElementRequest)request).getFactory();
			if (factory.getData() instanceof Object[])
			{
				Point loc = ((CreateRequest)request).getLocation().getCopy();
				getHostFigure().translateToRelative(loc);
				Command command = new CompoundCommand();
				for (Object o : (Object[])factory.getData())
				{
					FormPlaceElementCommand placeElementCommand = new FormPlaceElementCommand(application, ((FormGraphicalEditPart)getHost()).getPersist(),
						new Object[] { o }, request.getType(), request.getExtendedData(), null, loc.getSWTPoint(), null, parent.getPersist());
					((CompoundCommand)command).add(((CreateElementRequest)request).chainSetFactoryObjectCommand(placeElementCommand));
				}
				return command;
			}
		}

		return super.getCommand(request);
	}

	public static List<java.awt.Point> getDistributeChildrenDeltas(List<ISupportBounds> elements, final Distribution distribution)
	{
		if (elements == null || elements.size() < 3)
		{
			return null;
		}

		int packGap = new DesignerPreferences().getAlignmentDistances()[0]; // small step
		int size = elements.size();

		// find the bbox of all selected objects
		Rectangle _bbox = null;
		int leftMostCenter = Integer.MAX_VALUE;
		int rightMostCenter = 0;
		int topMostCenter = Integer.MAX_VALUE;
		int bottomMostCenter = 0;

		int totalWidth = 0, totalHeight = 0;

		for (ISupportBounds supportBounds : elements)
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

		switch (distribution)
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
		ISupportBounds[] eps = elements.toArray(new ISupportBounds[elements.size()]);
		Arrays.sort(eps, new Comparator<ISupportBounds>()
		{
			public int compare(ISupportBounds o1, ISupportBounds o2)
			{
				int a, b;
				if (distribution == DistributeRequest.Distribution.HORIZONTAL_SPACING || distribution == DistributeRequest.Distribution.HORIZONTAL_PACK)
				{
					a = o1.getLocation().x;
					b = o2.getLocation().x;
				}
				else if (distribution == DistributeRequest.Distribution.VERTICAL_SPACING || distribution == DistributeRequest.Distribution.VERTICAL_PACK)
				{
					a = o1.getLocation().y;
					b = o2.getLocation().y;
				}
				else if (distribution == DistributeRequest.Distribution.HORIZONTAL_CENTERS)
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

		List<java.awt.Point> moveDeltas = new ArrayList<java.awt.Point>();
		for (ISupportBounds supportBounds : elements)
		{
			java.awt.Point moveDelta = null;
			switch (distribution)
			{
				case HORIZONTAL_SPACING :
				case HORIZONTAL_PACK :
					moveDelta = new java.awt.Point((int)xNext - supportBounds.getLocation().x, 0);
					xNext += supportBounds.getSize().width + gap;
					break;
				case HORIZONTAL_CENTERS :
					moveDelta = new java.awt.Point((int)xNext - supportBounds.getSize().width / 2 - supportBounds.getLocation().x, 0);
					xNext += oncenter;
					break;
				case VERTICAL_SPACING :
				case VERTICAL_PACK :
					moveDelta = new java.awt.Point(0, (int)yNext - supportBounds.getLocation().y);
					yNext += supportBounds.getSize().height + gap;
					break;
				case VERTICAL_CENTERS :
					moveDelta = new java.awt.Point(0, (int)yNext - supportBounds.getSize().height / 2 - supportBounds.getLocation().y);
					yNext += oncenter;
					break;
			}
			moveDeltas.add(moveDelta);
		}

		return moveDeltas;
	}

	protected Command getDistributeChildrenCommand(final DistributeRequest request)
	{
		List<ISupportBounds> elements = new ArrayList<ISupportBounds>();
		List<EditPart> editParts = request.getEditParts();
		for (EditPart editPart : editParts)
		{
			Object model = editPart.getModel();
			if (!(model instanceof ISupportBounds))
			{
				return null;
			}
			else
			{
				elements.add((ISupportBounds)model);
			}
		}
		List<java.awt.Point> moveDeltas = getDistributeChildrenDeltas(elements, request.getDistribution());

		if (moveDeltas == null) return null;

		CompoundCommand distributeCommand = new CompoundCommand("distribute");
		for (EditPart ep : editParts)
		{
			java.awt.Point moveDelta = moveDeltas.get(editParts.indexOf(ep));
			Point gefPoint = new Point(moveDelta.x, moveDelta.y);
			distributeCommand.add(new ChangeBoundsCommand(ep, gefPoint, null));
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
		if (child instanceof PersistGraphicalEditPart || child instanceof GroupGraphicalEditPart || child instanceof TabFormGraphicalEditPart)
		{
			return new AlignmentfeedbackEditPolicy(parent);
		}
		return null;
	}

	/**
	 * @return the alignmentFeedbackHelper
	 */
	public AlignmentFeedbackHelper getAlignmentFeedbackHelper()
	{
		if (alignmentFeedbackHelper == null)
		{
			alignmentFeedbackHelper = new AlignmentFeedbackHelper(getFeedbackLayer());
		}
		return alignmentFeedbackHelper;
	}

	@Override
	public EditPart getTargetEditPart(Request request)
	{
		if (understandsRequest(request))
		{
			return getHost();
		}
		return super.getTargetEditPart(request);
	}

	@Override
	public boolean understandsRequest(Request request)
	{
		if (BaseVisualFormEditor.REQ_DROP_COPY.equals(request.getType()) && request instanceof CreateElementRequest)
		{
			RequestTypeCreationFactory factory = (RequestTypeCreationFactory)((CreateElementRequest)request).getFactory();
			if (factory.getData() instanceof Object[])
			{
				for (Object o : (Object[])factory.getData())
				{
					if (o instanceof PersistDragData && !DesignerUtil.isDropFormAllowed((IPersist)getHost().getModel(), (PersistDragData)o))
					{
						return false;
					}
				}
			}
			return true;
		}

		return false;
	}

	@Override
	public void showTargetFeedback(Request request)
	{
		super.showTargetFeedback(request);
		if (BaseVisualFormEditor.REQ_DROP_COPY.equals(request.getType()))
		{
			CreateRequest createReq = (CreateRequest)request;
			if (createReq.getSize() != null)
			{
				showSizeOnDropFeedback(createReq);
			}
		}
	}

	@Override
	public void eraseTargetFeedback(Request request)
	{
		super.eraseTargetFeedback(request);
		// always erase here, the request is the new request
		eraseSizeOnDropFeedback(request);
	}

	@Override
	protected void showSizeOnDropFeedback(CreateRequest request)
	{
		super.showSizeOnDropFeedback(request);
		getAlignmentFeedbackHelper().showElementAlignmentFeedback(request);
	}

	@Override
	protected void eraseSizeOnDropFeedback(Request request)
	{
		getAlignmentFeedbackHelper().eraseElementAlignmentFeedback();
		super.eraseSizeOnDropFeedback(request);
	}

	@Override
	protected Dimension getMinimumSizeFor(GraphicalEditPart child)
	{
		if (child.getModel() instanceof GraphicalComponent) return new Dimension(1, 1);
		return new Dimension(8, 8);
	}
}
