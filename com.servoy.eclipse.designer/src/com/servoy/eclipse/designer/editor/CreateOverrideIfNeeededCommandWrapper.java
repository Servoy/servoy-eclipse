/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

import java.util.function.Function;

import org.eclipse.gef.commands.Command;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.UUID;

/**
 * Command wrapper to create an override element if needed.
 *
 * @author rgansevles
 *
 */
public class CreateOverrideIfNeeededCommandWrapper extends Command
{
	private final PersistPropertySource propertySource;
	private final Function<UUID, Command> commandCreater;

	private PersistContext savedPersistContext;
	private Command command;
	private final UUID origUUID;
	private UUID newUUID;

	public CreateOverrideIfNeeededCommandWrapper(PersistPropertySource propertySource, UUID origUUID, Function<UUID, Command> commandCreater)
	{
		super(null);
		this.propertySource = propertySource;
		this.origUUID = origUUID;
		this.commandCreater = commandCreater;
	}

	@Override
	public void execute()
	{
		savedPersistContext = null;
		PersistContext persistContextBefore = propertySource.getPersistContext();
		try
		{
			if (propertySource.createOverrideElementIfNeeded())
			{
				savedPersistContext = persistContextBefore;

				// find the uuid of the element that is being extended the original element
				newUUID = persistContextBefore.getPersist().searchChild(origUUID)
					.map(IPersist::getID)
					.flatMap(propertySource.getPersistContext().getPersist()::searchForExtendsId)
					.map(IPersist::getUUID)
					// fall back to the original uuid if we can't find it
					.orElse(null);
			}

			// command can only be created after createOverrideElementIfNeeded because a new persistContext may have been created
			command = commandCreater.apply(newUUID == null ? origUUID : newUUID);

			setLabel(command.getLabel());
			command.execute();
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
	}

	@Override
	public void undo()
	{
		if (command != null)
		{
			command.undo();
			if (savedPersistContext != null)
			{
				// remove the newly created override persist from its parent
				if (newUUID != null)
				{
					propertySource.getPersistContext().getPersist().searchChild(newUUID)
						.ifPresent(overriddenPersist -> overriddenPersist.getParent().removeChild(overriddenPersist));
					newUUID = null;
				}
				propertySource.setPersistContext(savedPersistContext);
				savedPersistContext = null;
			}
		}
		command = null;
	}
}
