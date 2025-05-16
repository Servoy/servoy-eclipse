/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.designer.editor.commands.FormElementDeleteCommand;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.Utils;

/** Command to delete selected editor models.
 *
 * @author rgansevles
 *
 */
public class DeleteAction extends org.eclipse.gef.ui.actions.DeleteAction
{
	/**
	 * Constructs a <code>DeleteAction</code> using the specified part.
	 *
	 * @param part
	 *            The part for this action
	 */
	public DeleteAction(IWorkbenchPart part)
	{
		super(part);
		setEnabled(true);
	}

	/**
	 * Returns <code>true</code> if the selected objects can be deleted. Returns
	 * <code>false</code> if there are no objects selected
	 *
	 * @return <code>true</code> if the command should be enabled
	 */
	protected boolean calculateEnabledOldImplementation()
	{
		Command cmd = createDeleteCommand(getSelectedObjects());
		if (cmd == null)
		{
			return false;
		}
		return cmd.canExecute();
	}

	@Override
	public boolean calculateEnabled()
	{
		return calculateEnabledOldImplementation() && !DesignerUtil.containsInheritedElement(getSelectedObjects()) &&
			!DesignerUtil.containsFormComponentElement(getSelectedObjects());
	}

	/**
	 * Create a command to remove the selected objects.
	 *
	 * @param objects The objects to be deleted.
	 * @return The command to remove the selected objects.
	 */
	@Override
	public Command createDeleteCommand(List objects)
	{
		if (objects.isEmpty()) return null;

		final List<IPersist> toDelete = new ArrayList<IPersist>();
		for (Object modelObject : objects)
		{
			if (modelObject instanceof Form || (modelObject instanceof PersistContext && ((PersistContext)modelObject).getPersist() instanceof Form)) continue; // do not delete entire form here

			if (modelObject instanceof FormElementGroup)
			{
				toDelete.addAll(Utils.asList(((FormElementGroup)modelObject).getElements()));
			}
			else if (modelObject instanceof PersistContext)
			{
				toDelete.add(((PersistContext)modelObject).getPersist());
			}
		}

		if (toDelete.size() == 0)
		{
			// nothing to delete
			return null;
		}
		return new FormElementDeleteCommand(toDelete.toArray(new IPersist[toDelete.size()]));
	}

	/**
	 * Performs the delete action on the selected objects.
	 */
	@Override
	public void run()
	{
		execute(createDeleteCommand(getSelectedObjects()));
	}
}