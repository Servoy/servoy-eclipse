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

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.designer.editor.commands.DataFieldRequest;
import com.servoy.eclipse.designer.editor.commands.DataRequest;
import com.servoy.eclipse.designer.editor.commands.FormElementCopyCommand;
import com.servoy.eclipse.designer.editor.commands.FormElementDeleteCommand;
import com.servoy.eclipse.designer.editor.commands.FormPlaceElementCommand;
import com.servoy.eclipse.designer.editor.commands.FormPlaceFieldCommand;
import com.servoy.eclipse.designer.editor.commands.FormPlacePortalCommand;
import com.servoy.eclipse.designer.editor.commands.PersistPlaceCommandWrapper;
import com.servoy.eclipse.dnd.FormElementTransfer;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.Part;

/**
 * This edit policy enables edit actions on a Form.
 */
public class FormEditPolicy extends ComponentEditPolicy
{
	private final IFieldPositioner fieldPositioner;

	public FormEditPolicy(IFieldPositioner fieldPositioner)
	{
		this.fieldPositioner = fieldPositioner;
	}

	@Override
	public Command getCommand(final Request request)
	{
		Command command = null;
		if (VisualFormEditor.REQ_PLACE_TAB.equals(request.getType()) || VisualFormEditor.REQ_PLACE_MEDIA.equals(request.getType()) ||
			VisualFormEditor.REQ_PLACE_BEAN.equals(request.getType()) || VisualFormEditor.REQ_PLACE_BUTTON.equals(request.getType()) ||
			VisualFormEditor.REQ_PLACE_LABEL.equals(request.getType()) || VisualFormEditor.REQ_PLACE_RECT_SHAPE.equals(request.getType()) ||
			VisualFormEditor.REQ_PLACE_TEMPLATE.equals(request.getType()))
		{
			Object data = request instanceof DataRequest ? ((DataRequest)request).getData() : null;
			final org.eclipse.draw2d.geometry.Point location = request instanceof DataRequest ? ((DataRequest)request).getlocation() : null;
			command = new FormPlaceElementCommand(((FormGraphicalEditPart)getHost()).getPersist(), data, request.getType(), request.getExtendedData(),
				fieldPositioner, location == null ? null : location.getSWTPoint(), ((FormGraphicalEditPart)getHost()).getPersist());
		}
		else if (VisualFormEditor.REQ_PLACE_PORTAL.equals(request.getType()) && request instanceof DataFieldRequest)
		{
			DataFieldRequest dataFieldRequest = ((DataFieldRequest)request);
			command = new FormPlacePortalCommand(((FormGraphicalEditPart)getHost()).getPersist(), dataFieldRequest.getData(), dataFieldRequest.getType(),
				dataFieldRequest.getExtendedData(), fieldPositioner, dataFieldRequest.getlocation() == null ? null
					: dataFieldRequest.getlocation().getSWTPoint(), dataFieldRequest.fillText, dataFieldRequest.fillName,
				((FormGraphicalEditPart)getHost()).getPersist());
		}
		else if (VisualFormEditor.REQ_PLACE_FIELD.equals(request.getType()) && request instanceof DataFieldRequest)
		{
			DataFieldRequest dataFieldRequest = ((DataFieldRequest)request);
			command = new FormPlaceFieldCommand(((FormGraphicalEditPart)getHost()).getPersist(), dataFieldRequest.getData(), dataFieldRequest.getType(),
				dataFieldRequest.getExtendedData(), fieldPositioner, dataFieldRequest.getlocation() == null ? null
					: dataFieldRequest.getlocation().getSWTPoint(), dataFieldRequest.placeAsLabels, dataFieldRequest.placeWithLabels,
				dataFieldRequest.placeHorizontal, dataFieldRequest.fillText, dataFieldRequest.fillName, ((FormGraphicalEditPart)getHost()).getPersist());
		}
		else if ((VisualFormEditor.REQ_COPY.equals(request.getType()) || VisualFormEditor.REQ_CUT.equals(request.getType())) && request instanceof GroupRequest)
		{
			List<IPersist> models = new ArrayList<IPersist>();
			for (Object editPart : ((GroupRequest)request).getEditParts())
			{
				if (editPart instanceof EditPart && !(((EditPart)editPart).getModel() instanceof Form) && !(((EditPart)editPart).getModel() instanceof Part))
				{
					if (((EditPart)editPart).getModel() instanceof IPersist)
					{
						models.add((IPersist)((EditPart)editPart).getModel());
					}
					if (((EditPart)editPart).getModel() instanceof FormElementGroup)
					{
						Iterator<IFormElement> elements = ((FormElementGroup)((EditPart)editPart).getModel()).getElements();
						while (elements.hasNext())
						{
							IFormElement next = elements.next();
							if (next instanceof IPersist)
							{
								models.add((IPersist)next);
							}
						}
					}
				}
			}

			if (models.size() > 0)
			{
				CompoundCommand compoundCommand = new CompoundCommand();

				// find the minimum x and y (upper-left corner of the original selection)
				int minx = Integer.MAX_VALUE;
				int miny = Integer.MAX_VALUE;
				for (IPersist model : models)
				{
					if (model instanceof ISupportBounds)
					{
						Point location = ((ISupportBounds)model).getLocation();
						minx = minx < location.x ? minx : location.x;
						miny = miny < location.y ? miny : location.y;
					}
				}
				if (minx < Integer.MAX_VALUE && miny < Integer.MAX_VALUE)
				{
					final org.eclipse.swt.graphics.Point point = new org.eclipse.swt.graphics.Point(minx, miny);
					compoundCommand.add(new Command()
					{
						@Override
						public void execute()
						{
							// set the location to copy for next paste
							fieldPositioner.setDefaultLocation(point);
							if (VisualFormEditor.REQ_COPY.equals(request.getType()))
							{
								fieldPositioner.getNextLocation(null); // move a bit according to the copy/paste offset
							}
						}
					});
				}
				Command copyCommand = new FormElementCopyCommand(models.toArray());
				if (VisualFormEditor.REQ_COPY.equals(request.getType()))
				{
					compoundCommand.add(copyCommand);
					compoundCommand.setLabel("Copy objects");
				}
				else if (VisualFormEditor.REQ_CUT.equals(request.getType()))
				{
					final CompoundCommand deleteCommand = new CompoundCommand();
					for (IPersist persist : models)
					{
						deleteCommand.add(new FormElementDeleteCommand(persist));
					}
					CompoundCommand cutCommand = new CompoundCommand()
					{
						// copy command is not applicable in undo
						@Override
						public boolean canUndo()
						{
							return deleteCommand.canUndo();
						}

						@Override
						public void undo()
						{
							deleteCommand.undo();
						}
					};
					cutCommand.add(copyCommand);
					cutCommand.add(deleteCommand);
					compoundCommand.setLabel("Cut objects");
					compoundCommand.add(cutCommand);
				}
				command = compoundCommand.unwrap();
			}
		}

		if (command == null)
		{
			return super.getCommand(request);
		}
		return new PersistPlaceCommandWrapper(getHost(), command, true);
	}

	@Override
	public boolean understandsRequest(Request request)
	{
		return VisualFormEditor.REQ_PLACE_TAB.equals(request.getType()) || VisualFormEditor.REQ_PLACE_PORTAL.equals(request.getType()) ||
			VisualFormEditor.REQ_PLACE_MEDIA.equals(request.getType()) || VisualFormEditor.REQ_PLACE_BEAN.equals(request.getType()) ||
			VisualFormEditor.REQ_PLACE_BUTTON.equals(request.getType()) || VisualFormEditor.REQ_PLACE_LABEL.equals(request.getType()) ||
			VisualFormEditor.REQ_PLACE_RECT_SHAPE.equals(request.getType()) || VisualFormEditor.REQ_PLACE_FIELD.equals(request.getType()) ||
			VisualFormEditor.REQ_PLACE_TEMPLATE.equals(request.getType()) || VisualFormEditor.REQ_COPY.equals(request.getType()) ||
			VisualFormEditor.REQ_CUT.equals(request.getType()) || super.understandsRequest(request);
	}

	public static Object getClipboardContents()
	{
		Object clip = null;
		Clipboard cb = new Clipboard(Display.getDefault());
		TransferData[] transferTypes = cb.getAvailableTypes();
		for (TransferData transferData : transferTypes)
		{
			if (FormElementTransfer.getInstance().isSupportedType(transferData))
			{
				clip = cb.getContents(FormElementTransfer.getInstance());
				break;
			}
			if (clip == null && TextTransfer.getInstance().isSupportedType(transferData))
			{
				clip = cb.getContents(TextTransfer.getInstance());
				continue; // prefer FormElementTransfer
			}
		}
		cb.dispose();

		return clip;
	}

}
