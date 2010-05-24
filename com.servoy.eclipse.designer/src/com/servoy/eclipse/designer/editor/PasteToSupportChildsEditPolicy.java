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

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.AbstractEditPolicy;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.designer.editor.commands.FormPlaceElementCommand;
import com.servoy.eclipse.designer.editor.commands.PersistPlaceCommandWrapper;
import com.servoy.eclipse.dnd.FormElementTransfer;
import com.servoy.eclipse.dnd.FormElementDragData.DataProviderDragData;
import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.TabPanel;

/**
 * This edit policy enables pasting to a form or tab panel.
 */
class PasteToSupportChildsEditPolicy extends AbstractEditPolicy
{
	public static String PASTE_ROLE = "Paste Policy"; //$NON-NLS-1$

	private final IFieldPositioner fieldPositioner;

	public PasteToSupportChildsEditPolicy(IFieldPositioner fieldPositioner)
	{
		this.fieldPositioner = fieldPositioner;
	}

	@Override
	public Command getCommand(Request request)
	{
		IPersist persist = (IPersist)getHost().getModel();
		if (VisualFormEditor.REQ_PASTE.equals(request.getType()) && persist instanceof ISupportChilds)
		{
			return new PasteCommand((ISupportChilds)persist, request);
		}
		return null;
	}

	public class PasteCommand extends Command
	{
		private final IPersist parent;
		private final Request request;
		private Command subCommand;

		public PasteCommand(ISupportChilds parent, Request request)
		{
			this.parent = parent;
			this.request = request;
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
			if (clipboardContents instanceof Object[])
			{
				for (int i = 0; i < ((Object[])clipboardContents).length; i++)
				{
					Object o = ((Object[])clipboardContents)[i];
					if (o instanceof DataProviderDragData && parent instanceof TabPanel)
					{
						return null;
					}
					if (o instanceof PersistDragData)
					{
						PersistDragData dragData = (PersistDragData)o;
						if ((parent instanceof TabPanel && dragData.type != IRepository.TABS) ||
							(!(parent instanceof TabPanel) && dragData.type == IRepository.TABS))
						{
							if (dragData.type == IRepository.TABS)
							{
								int tabIndex = getTabParentIndex((Object[])clipboardContents, dragData);
								if (tabIndex >= 0)
								{
									if (tabIndex > i)
									{
										// switch them, the tab should be placed first
										Object temp = ((Object[])clipboardContents)[i];
										((Object[])clipboardContents)[i] = ((Object[])clipboardContents)[tabIndex];
										((Object[])clipboardContents)[tabIndex] = temp;
									}
									continue;
								}
							}
							return null;
						}
					}
				}
			}

			Command command = new FormPlaceElementCommand(request, (ISupportChilds)parent, clipboardContents, fieldPositioner, null);
			// Refresh the form
			return new PersistPlaceCommandWrapper((EditPart)getHost().getViewer().getEditPartRegistry().get(parent.getAncestor(IRepository.FORMS)), command,
				true);
		}

		protected int getTabParentIndex(Object[] elements, PersistDragData tab)
		{
			if (elements != null && tab.type == IRepository.TABS)
			{
				IPersist tabParent = null;
				ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(tab.solutionName);
				if (servoyProject != null)
				{
					IPersist draggedPersist = AbstractRepository.searchPersist(servoyProject.getEditingSolution(), tab.uuid);
					if (draggedPersist != null)
					{
						tabParent = draggedPersist.getParent();
					}
				}
				int tabIndex = -1;
				for (int i = 0; i < elements.length; i++)
				{
					Object o = elements[i];
					if (o instanceof PersistDragData)
					{
						PersistDragData dragData = (PersistDragData)o;
						if (dragData.type == IRepository.TABPANELS)
						{
							if (tabParent != null && tabParent.getUUID().equals(dragData.uuid)) tabIndex = i;
							else return -1;
						}
					}
				}
				return tabIndex;
			}
			return -1;
		}
	}

	@Override
	public boolean understandsRequest(Request request)
	{
		return VisualFormEditor.REQ_PASTE.equals(request.getType());
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

}
