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

package com.servoy.eclipse.designer.editor.mobile.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;

import com.servoy.eclipse.designer.editor.commands.AbstractModelsCommand;
import com.servoy.eclipse.designer.editor.commands.ISupportModels;
import com.servoy.eclipse.designer.editor.commands.RefreshingCommand;
import com.servoy.eclipse.designer.editor.mobile.MobileVisualFormEditorHtmlDesignPage;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * Apply element order after delegate command has executed.
 *
 * @author rgansevles
 *
 */
public class ApplyMobileFormElementOrderCommand extends RefreshingCommand<AbstractModelsCommand> implements ISupportModels
{
	private final int zoneIndex;
	private final IPersist parent;
	private final Form form;
	private final List<Object> createdModels;
	private List<Object> elements;

	/**
	 * @param command
	 * @param zoneIndex
	 * @param parent
	 * @param form
	 * @param createdModels
	 */
	public ApplyMobileFormElementOrderCommand(AbstractModelsCommand command, int zoneIndex, IPersist parent, Form form, List<Object> createdModels)
	{
		super(command);
		this.zoneIndex = zoneIndex;
		this.parent = parent;
		this.form = form;
		this.createdModels = createdModels;
	}

	@Override
	public void refresh(boolean haveExecuted)
	{
		elements = null;
		Command reorderCommand = null;
		if (haveExecuted)
		{
			Object[] models = getCommand().getModels();
			if (models != null && models.length > 0)
			{
				if (createdModels != null) createdModels.add(models[0]);
				Object model = MobileVisualFormEditorHtmlDesignPage.getModelObject(form, models[0]);
				elements = new ArrayList<Object>(MobileVisualFormEditorHtmlDesignPage.getElementsForParent(form, parent));
				elements.remove(model); // new elements is already added
				elements.add(zoneIndex < 0 ? 0 : zoneIndex > elements.size() ? elements.size() : zoneIndex, model);
				reorderCommand = applyElementOrder(elements);
			}
		}
		else if (elements != null)
		{
			// undo
			elements.remove(zoneIndex < 0 ? 0 : zoneIndex > elements.size() - 1 ? elements.size() - 1 : zoneIndex);
			reorderCommand = applyElementOrder(elements);
			elements = null;
		}
		if (reorderCommand != null && reorderCommand.canExecute())
		{
			reorderCommand.execute();
		}
	}

	@Override
	public Object[] getModels()
	{
		return getCommand().getModels();
	}

	/**
	 * @param elements
	 */
	public static Command applyElementOrder(List< ? > elements)
	{
		CompoundCommand command = null;
		if (elements.size() > 1)
		{
			int d = 0;
			for (Object elem : elements)
			{
				Command setValueCommand = MobileVisualFormEditorHtmlDesignPage.createSetValueCommand(elem,
					StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), new java.awt.Point(d, d));
				if (setValueCommand != null)
				{
					if (command == null)
					{
						command = new CompoundCommand();
					}
					command.add(setValueCommand);
				}
				d += 100;
			}
		}

		return command;
	}

}