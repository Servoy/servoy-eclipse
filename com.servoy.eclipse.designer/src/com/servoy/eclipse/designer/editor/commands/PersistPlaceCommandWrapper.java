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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.viewers.StructuredSelection;

/**
 * Select the persists after executing or undoing of the command, optionally refresh the host.
 * 
 * @author rgansevles
 * 
 */
public class PersistPlaceCommandWrapper extends RefreshingCommand
{
	private final EditPart host;
	private final boolean refreshHost;

	public PersistPlaceCommandWrapper(EditPart host, Command command, boolean refreshHost)
	{
		super(command);
		this.host = host;
		this.refreshHost = refreshHost;
	}

	@Override
	public void refresh(boolean haveExecuted)
	{
		if (refreshHost && host.isActive())
		{
			host.refresh();
		}

		// set the focus
		if (haveExecuted && command instanceof ISupportModels)
		{
			Object[] models = ((ISupportModels)command).getModels();
			if (models != null && models.length > 0)
			{
				EditPartViewer viewer = host.getViewer();
				List<Object> parts = new ArrayList<Object>(models.length);
				for (Object model : models)
				{
					Object editPart = viewer.getEditPartRegistry().get(model);
					if (editPart != null)
					{
						parts.add(editPart);
					}
				}
				if (parts.size() > 0)
				{
					viewer.setSelection(new StructuredSelection(parts));
				}
			}
		}
	}
}
