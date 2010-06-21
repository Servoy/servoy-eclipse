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

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.internal.GEFMessages;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

import com.servoy.eclipse.designer.editor.VisualFormEditor;

/**
 * An action to delete selected objects.
 */
public class CopyAction extends DesignerSelectionAction
{

	/**
	 * Constructs a <code>DeleteAction</code> using the specified part.
	 * 
	 * @param part The part for this action
	 */
	public CopyAction(IWorkbenchPart part)
	{
		super(part, VisualFormEditor.REQ_COPY);
	}

	/**
	 * Initializes this action's text and images.
	 */
	@Override
	protected void init()
	{
		super.init();
		setText(GEFMessages.CopyAction_Label);
		setToolTipText(GEFMessages.CopyAction_Tooltip);
		setId(ActionFactory.COPY.getId());
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		setDisabledImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
		setEnabled(false);
	}

	@Override
	protected void execute(Command command)
	{
		if (command == null || !command.canExecute()) return;
		// cannot undo (like copy command), run outside the command stack (will not make editor dirty)
		command.execute();
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
