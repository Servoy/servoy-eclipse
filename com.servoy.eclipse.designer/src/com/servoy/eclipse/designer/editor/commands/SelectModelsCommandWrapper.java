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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;

/**
 * Wrapper for commands, set the selection after execute/undo.
 * 
 * @author rgansevles
 *
 */
public class SelectModelsCommandWrapper extends CompoundCommand
{
	private final EditPartViewer viewer;
	private final Iterable<EditPart> toRefresh;

	public SelectModelsCommandWrapper(EditPartViewer viewer, Iterable<EditPart> toRefresh, Command command)
	{
		this.viewer = viewer;
		this.toRefresh = toRefresh;
		add(command);
	}

	public SelectModelsCommandWrapper(EditPartViewer viewer, EditPart toRefresh, Command command)
	{
		this(viewer, toRefresh == null ? null : Collections.singletonList(toRefresh), command);
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

		if (toRefresh != null)
		{
			for (EditPart editPart : toRefresh)
			{
				editPart.refresh();
			}
		}

		// select the models later in the display thread, some editparts may not have been created yet.
		final List<Object> models = getModels();
		if (models.size() > 0)
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					List<Object> editParts = new ArrayList<Object>(models.size());
					for (Object model : models)
					{
						EditPart editPart = (EditPart)viewer.getEditPartRegistry().get(model);
						if (editPart != null && editPart.isSelectable())
						{
							editParts.add(editPart);
						}
					}
					if (editParts.size() > 0)
					{
						viewer.setSelection(new StructuredSelection(editParts));
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
	private List<Object> getModels()
	{
		List<Object> models = new ArrayList<Object>();
		Set<FormElementGroup> groups = new HashSet<FormElementGroup>();
		for (Object model : getModels(this, new ArrayList<Object>()))
		{
			// replace group elements model with group model
			if (model instanceof FormElementGroup)
			{
				groups.add((FormElementGroup)model);
			}
			else if (model instanceof IFormElement && ((IFormElement)model).getGroupID() != null)
			{
				Form form = (Form)viewer.getContents().getModel();
				groups.add(new FormElementGroup(((IFormElement)model).getGroupID(), ModelUtils.getEditingFlattenedSolution(form), form));
			}
			else
			{
				models.add(model);
			}
		}
		models.addAll(groups);
		return models;
	}

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
		return unwrapped == this ? this : new SelectModelsCommandWrapper(viewer, toRefresh, unwrapped);
	}
}
