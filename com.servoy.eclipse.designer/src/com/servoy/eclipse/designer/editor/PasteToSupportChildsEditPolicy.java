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

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.AbstractEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.swt.graphics.Point;

import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.designer.actions.PasteCommand;
import com.servoy.eclipse.designer.editor.commands.FormPlaceElementCommand;
import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.eclipse.ui.util.DefaultFieldPositioner;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportFormElement;
import com.servoy.j2db.persistence.Solution;

/**
 * This edit policy enables pasting to a form or tab panel.
 * Also handles cloning (ctl-select).
 *
 * @author rgansevles
 */
class PasteToSupportChildsEditPolicy extends AbstractEditPolicy
{
	public static String PASTE_ROLE = "Paste Policy";

	private final IFieldPositioner fieldPositioner;

	private final IApplication application;

	public PasteToSupportChildsEditPolicy(IApplication application, IFieldPositioner fieldPositioner)
	{
		this.application = application;
		this.fieldPositioner = fieldPositioner;
	}

	@Override
	public Command getCommand(Request request)
	{
		IPersist persist = (IPersist)getHost().getModel();
		EditPart formEditPart = getHost().getParent();
		while (formEditPart != null && !(formEditPart.getModel() instanceof Form))
		{
			formEditPart = formEditPart.getParent();
		}
		if (BaseVisualFormEditor.REQ_PASTE.equals(request.getType()) && persist instanceof ISupportChilds)
		{
			return new PasteCommand(application, (ISupportChilds)persist, request.getExtendedData(), (IPersist)(formEditPart == null ? null
				: formEditPart.getModel()), fieldPositioner);
		}
		if (RequestConstants.REQ_CLONE.equals(request.getType()) && request instanceof ChangeBoundsRequest)
		{
			List< ? extends EditPart> editParts = ((GroupRequest)request).getEditParts();
			List<IPersist> models = new ArrayList<IPersist>();
			int minx = Integer.MAX_VALUE, miny = Integer.MAX_VALUE;
			for (EditPart editPart : editParts)
			{
				if (editPart instanceof GraphicalEditPart)
				{
					Rectangle bounds = ((GraphicalEditPart)editPart).getFigure().getBounds();
					if (minx > bounds.x) minx = bounds.x;
					if (miny > bounds.y) miny = bounds.y;
				}
				if (editPart.getModel() instanceof IPersist)
				{
					models.add((IPersist)editPart.getModel());
				}
				else if (editPart.getModel() instanceof FormElementGroup)
				{
					Iterator<ISupportFormElement> elements = ((FormElementGroup)editPart.getModel()).getElements();
					while (elements.hasNext())
					{
						ISupportFormElement element = elements.next();
						if (element instanceof IPersist)
						{
							models.add(element);
						}
					}
				}
			}

			if (models.size() == 0 || minx == Integer.MAX_VALUE || miny == Integer.MAX_VALUE)
			{
				return null;
			}
			Object[] objects = new Object[models.size()];
			for (int i = 0; i < models.size(); i++)
			{
				objects[i] = new PersistDragData(((Solution)models.get(i).getAncestor(IRepository.SOLUTIONS)).getName(), models.get(i).getUUID(),
					models.get(i).getTypeID(), 0, 0, -1);
			}
			Point location = new Point(minx + ((ChangeBoundsRequest)request).getMoveDelta().x, miny + ((ChangeBoundsRequest)request).getMoveDelta().y);
			return new FormPlaceElementCommand(application, (ISupportChilds)persist.getAncestor(IRepository.FORMS), objects, request.getType(),
				request.getExtendedData(), new DefaultFieldPositioner(location), null, null, (IPersist)(formEditPart == null ? null : formEditPart.getModel()));
		}
		return null;
	}

	@Override
	public boolean understandsRequest(Request request)
	{
		return BaseVisualFormEditor.REQ_PASTE.equals(request.getType()) || RequestConstants.REQ_CLONE.equals(request.getType());
	}
}
