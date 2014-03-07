/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.designer.actions;

import java.util.Map;

import org.eclipse.gef.commands.Command;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.AbstractModelsCommand;
import com.servoy.eclipse.designer.editor.commands.FormPlaceElementCommand;
import com.servoy.eclipse.designer.editor.commands.ICommandWrapper;
import com.servoy.eclipse.designer.editor.commands.ISupportModels;
import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.eclipse.dnd.FormElementTransfer;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.TabPanel;

public class PasteCommand extends AbstractModelsCommand implements ICommandWrapper<Command>
{
	private final IPersist parent;
	private final IPersist context;
	private final Map<Object, Object> requestData;
	private Command subCommand;
	private final IFieldPositioner fieldPositioner;
	private final IApplication application;

	public PasteCommand(IApplication application, ISupportChilds parent, Map<Object, Object> requestData, IPersist context, IFieldPositioner fieldPositioner)
	{
		this.application = application;
		this.parent = parent;
		this.requestData = requestData;
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

		return new FormPlaceElementCommand(application, (ISupportChilds)pasteParent, clipboardContents, BaseVisualFormEditor.REQ_PASTE, requestData,
			fieldPositioner, null, null, context);
	}

	public Command getCommand()
	{
		return subCommand;
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

	@Override
	public Object[] getModels()
	{
		return subCommand instanceof ISupportModels ? ((ISupportModels)subCommand).getModels() : null;
	}
}