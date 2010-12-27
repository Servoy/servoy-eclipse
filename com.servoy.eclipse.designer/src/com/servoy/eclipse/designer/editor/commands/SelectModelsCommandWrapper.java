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

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;

/**
 * Wrapper for commands, set the selection after execute/undo.
 * 
 * @author rgansevles
 *
 */
public class SelectModelsCommandWrapper extends CompoundCommand
{
	private final EditPartViewer viewer;

	public SelectModelsCommandWrapper(EditPartViewer viewer, Command command)
	{
		this.viewer = viewer;
		add(command);
	}

	@Override
	public void execute()
	{
		super.execute();
		selectModels();
	}

	@Override
	public void redo()
	{
		super.redo();
		selectModels();
	}

	@Override
	public void undo()
	{
		super.undo();
		selectModels();
	}

	protected void selectModels()
	{
		if (viewer == null)
		{
			return;
		}
		// select the models later in the display thread, some editparts may not have been created yet.
		final List<Object> models = getModels(this, new ArrayList<Object>());
		if (models.size() > 0)
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					List<Object> parts = new ArrayList<Object>(models.size());
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
			});
		}
	}

	/** 
	 * Get all models found in the command
	 * 
	 * @param models
	 * @return models
	 */
	private static List<Object> getModels(Command command, List<Object> models)
	{
		if (command instanceof ISupportModels)
		{
			Object[] cmodels = ((ISupportModels)command).getModels();
			if (cmodels != null)
			{
				for (Object model : cmodels)
				{
					models.add(model);
				}
			}
		}
		else if (command instanceof CompoundCommand)
		{
			for (Object cmd : ((CompoundCommand)command).getCommands())
			{
				getModels((Command)cmd, models);
			}
		}
		else if (command instanceof ICommandWrapper)
		{
			getModels(((ICommandWrapper)command).getCommand(), models);
		}

		return models;
	}

	@Override
	public Command unwrap()
	{
		Command unwrapped = super.unwrap();
		return unwrapped == this ? this : new SelectModelsCommandWrapper(viewer, unwrapped);
	}
}
