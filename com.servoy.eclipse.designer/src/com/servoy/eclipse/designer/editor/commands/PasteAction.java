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
package com.servoy.eclipse.designer.editor.commands;

import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.internal.GEFMessages;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

import com.servoy.eclipse.designer.editor.VisualFormEditor;

/**
 * An action to paste the clipboard contents.
 */
public class PasteAction extends SelectionAction
{

	/**
	 * Constructs a <code>PasteAction</code> using the specified part.
	 * 
	 * @param part The part for this action
	 */
	public PasteAction(IWorkbenchPart part)
	{
		super(part);
		setLazyEnablementCalculation(false);
	}

	/**
	 * Returns <code>true</code> if the clipboard contents can be pasted.
	 * 
	 * @return <code>true</code> if the command should be enabled
	 */
	@Override
	protected boolean calculateEnabled()
	{
		Command cmd = createPasteCommand(getSelectedObjects());
		return (cmd != null) && cmd.canExecute();
	}

	/**
	 * Create a command to paste to the selected object.
	 */
	public Command createPasteCommand(List objects)
	{
		if (objects.size() == 0 || !(objects.get(0) instanceof EditPart))
		{
			return null;
		}

		return ((EditPart)objects.get(0)).getCommand(new Request(VisualFormEditor.REQ_PASTE));
	}

	/**
	 * Initializes this action's text and images.
	 */
	@Override
	protected void init()
	{
		super.init();
		setText(GEFMessages.PasteAction_Label);
		setToolTipText(GEFMessages.PasteAction_Tooltip);
		setId(ActionFactory.PASTE.getId());
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		setDisabledImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED));
		setEnabled(true);
	}

	/**
	 * Performs the delete action on the selected objects.
	 */
	@Override
	public void run()
	{
		execute(createPasteCommand(getSelectedObjects()));
	}

	@Override
	public boolean isHandled()
	{
		if (getWorkbenchPart() instanceof VisualFormEditor)
		{
			return ((VisualFormEditor)getWorkbenchPart()).isDesignerContextActive();
		}
		return true;
	}
}
