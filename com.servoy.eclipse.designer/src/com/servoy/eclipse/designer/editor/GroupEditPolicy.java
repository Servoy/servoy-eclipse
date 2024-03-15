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

import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.editpolicies.AbstractEditPolicy;

import com.servoy.eclipse.designer.editor.commands.RefreshingCommand;

/**
 * Edit policy for GroupEditParts
 *
 * @author rgansevles
 *
 */
public class GroupEditPolicy extends AbstractEditPolicy
{

	@Override
	public Command getCommand(Request request)
	{
		CompoundCommand compoundCommand = null;
		for (EditPart editPart : (List<EditPart>)getHost().getChildren())
		{
			Command command = editPart.getCommand(request);
			if (command != null)
			{
				if (compoundCommand == null)
				{
					compoundCommand = new CompoundCommand();
				}
				compoundCommand.add(command);
				// when creating paste command we don't need to create it for all the
				// subelements of the group because PasteToSupportChildsEditPolicy responsible
				// for creating the PasteCommand will create it passing the Form as the persist
				// object. This is similar to the situation in PasteAction.createPasteCommand
				if (request.getType() == BaseVisualFormEditor.REQ_PASTE) break;
			}
		}

		if (compoundCommand == null)
		{
			return null;
		}
		return new RefreshingCommand<>(compoundCommand.unwrap(), () -> {
			// refresh the parent, the group may have changed
			if (getHost().getParent() != null) getHost().getParent().refresh();
		});
	}
}