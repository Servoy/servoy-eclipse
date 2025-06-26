/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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
import java.util.Iterator;
import java.util.List;

import org.eclipse.gef.commands.Command;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportFormElement;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.Utils;

/**
 * @author Diana
 *
 */
public class FormElementGroupDeleteCommand extends Command
{
	private final List<FormElementGroup> groupsToDelete;
	private final List<IPersist> removedElements = new ArrayList<>();
	private boolean executed = false;
	private final BaseVisualFormEditor editor;

	public FormElementGroupDeleteCommand(List<FormElementGroup> groups, BaseVisualFormEditor editor)
	{
		this.groupsToDelete = groups;
		this.editor = editor;
		setLabel("Delete Group");
	}

	@Override
	public boolean canExecute()
	{
		return groupsToDelete != null && !groupsToDelete.isEmpty();
	}

	@Override
	public void execute()
	{
		for (FormElementGroup group : groupsToDelete)
		{
			if (group != null)
			{
				Iterator<ISupportFormElement> members = group.getElements();
				if (members != null)
				{
					removedElements.addAll(Utils.asList(group.getElements()));
					members.forEachRemaining(obj -> {
						if (obj instanceof IPersist persist)
						{
							setElementProperty(persist, StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName(), null, group.getParent());
						}
					});
				}
			}
		}
		executed = true;
	}

	protected void setElementProperty(IPersist element, String propertyName, Object propertyValue, IPersist context)
	{
		PersistPropertySource elementPropertySource = PersistPropertySource.createPersistPropertySource(element, context == null ? element : context,
			false);
		elementPropertySource.setPropertyValue(propertyName, propertyValue);
	}

	@Override
	public void redo()
	{
		execute();
	}
}