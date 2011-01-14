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
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.AbstractEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.designer.editor.commands.FormPlaceElementCommand;
import com.servoy.eclipse.designer.editor.commands.ICommandWrapper;
import com.servoy.eclipse.designer.editor.commands.PersistPlaceCommandWrapper;
import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.eclipse.dnd.FormElementTransfer;
import com.servoy.eclipse.ui.util.DefaultFieldPositioner;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TabPanel;

/**
 * This edit policy enables pasting to a form or tab panel.
 * Also handles cloning (ctl-select).
 * 
 * @author rgansevles
 */
class PasteToSupportChildsEditPolicy extends AbstractEditPolicy
{
	public static String PASTE_ROLE = "Paste Policy"; //$NON-NLS-1$

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
		if (VisualFormEditor.REQ_PASTE.equals(request.getType()) && persist instanceof ISupportChilds)
		{
			return new PasteCommand(application, (ISupportChilds)persist, request, getHost().getViewer(), (IPersist)(formEditPart == null ? null
				: formEditPart.getModel()), fieldPositioner);
		}
		if (RequestConstants.REQ_CLONE.equals(request.getType()) && request instanceof ChangeBoundsRequest)
		{
			List<EditPart> editParts = ((GroupRequest)request).getEditParts();
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
					Iterator<IFormElement> elements = ((FormElementGroup)editPart.getModel()).getElements();
					while (elements.hasNext())
					{
						IFormElement element = elements.next();
						if (element instanceof IPersist)
						{
							models.add((IPersist)element);
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
					models.get(i).getTypeID(), 0, 0, null);
			}
			Point location = new Point(minx + ((ChangeBoundsRequest)request).getMoveDelta().x, miny + ((ChangeBoundsRequest)request).getMoveDelta().y);
			Command command = new FormPlaceElementCommand(application, (ISupportChilds)persist.getAncestor(IRepository.FORMS), objects, request.getType(),
				request.getExtendedData(), new DefaultFieldPositioner(location), null, (IPersist)(formEditPart == null ? null : formEditPart.getModel()));
			// Refresh the form
			return new PersistPlaceCommandWrapper((EditPart)getHost().getViewer().getEditPartRegistry().get(persist.getAncestor(IRepository.FORMS)), command,
				true);
		}
		return null;
	}

	@Override
	public boolean understandsRequest(Request request)
	{
		return VisualFormEditor.REQ_PASTE.equals(request.getType()) || RequestConstants.REQ_CLONE.equals(request.getType());
	}

	public static Object getClipboardContents()
	{
		Object clip = null;
		Clipboard cb = new Clipboard(Display.getDefault());
		try
		{
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
		}
		finally
		{
			cb.dispose();
		}
		return clip;
	}

	public static class PasteCommand extends Command implements ICommandWrapper
	{
		private final IPersist parent;
		private final IPersist context;
		private final Request request;
		private Command subCommand;
		private final EditPartViewer editPartViewer;
		private final IFieldPositioner fieldPositioner;
		private final IApplication application;

		public PasteCommand(IApplication application, ISupportChilds parent, Request request, EditPartViewer editPartViewer, IPersist context,
			IFieldPositioner fieldPositioner)
		{
			this.application = application;
			this.parent = parent;
			this.request = request;
			this.editPartViewer = editPartViewer;
			this.fieldPositioner = fieldPositioner;
			this.context = context;
		}

		@Override
		public void execute()
		{
			if (subCommand == null)
			{
				subCommand = createSubCommand();
			}
			if (subCommand != null && subCommand.canExecute())
			{
				subCommand.execute();
			}
		}

		@Override
		public boolean canUndo()
		{
			return subCommand != null && subCommand.canUndo();
		}

		@Override
		public void undo()
		{
			if (canUndo()) subCommand.undo();
		}

		protected Command createSubCommand()
		{
			Object clipboardContents = getClipboardContents();
			// tabs can only be pasted to tab panels
			IPersist pasteParent = parent;
			if (clipboardContents instanceof Object[])
			{
				for (int i = 0; i < ((Object[])clipboardContents).length; i++)
				{
					Object o = ((Object[])clipboardContents)[i];
					if (parent instanceof TabPanel && (!(o instanceof PersistDragData) || ((PersistDragData)o).type != IRepository.TABS))
					{
						// paste something else then a tab into a tabpanel? in stead paste to form 
						pasteParent = parent.getAncestor(IRepository.FORMS);
						break;
					}
				}
			}

			Command command = new FormPlaceElementCommand(application, (ISupportChilds)pasteParent, clipboardContents, request.getType(),
				request.getExtendedData(), fieldPositioner, null, context);
			// Refresh the form
			return new PersistPlaceCommandWrapper((EditPart)editPartViewer.getEditPartRegistry().get(pasteParent.getAncestor(IRepository.FORMS)), command, true);
		}

		public Command getCommand()
		{
			return subCommand;
		}
	}
}
