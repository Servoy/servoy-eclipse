/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

package com.servoy.eclipse.designer;

import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.designer.editor.CreateOverrideIfNeeededCommandWrapper;
import com.servoy.eclipse.designer.editor.IRAGTEST;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.CreateComponentCommand;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.CreateComponentCommand.CreateComponentOptions;
import com.servoy.eclipse.ui.EditorActionsRegistry.EditorComponentActionHandler;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.RetargetToEditorPersistProperties;
import com.servoy.j2db.dataprocessing.IModificationSubject;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.dataprocessing.ModificationSubject;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.UUID;

/**
 * Handler to create/delete a component (or subcomponent in array property) using commands from the designer.
 *
 * @author rgansevles
 */
public class EditorComponentActionHandlerImpl implements EditorComponentActionHandler
{
	public static EditorComponentActionHandler EDITOR_COMPONENT_ACTION_HANDLER = new EditorComponentActionHandlerImpl();


	private final IModificationSubject modificationSubject = new ModificationSubject();

	private EditorComponentActionHandlerImpl()
	{
	}

	@Override
	public void createComponent(IPropertySource propertySource, UUID uuid, String propertyName, String type, boolean prepend,
		boolean dropTargetIsSibling)
	{
		executeCommandOnForm(propertySource, () -> {
			if (propertySource instanceof PersistPropertySource)
			{
				PersistPropertySource persistPropertySource = (PersistPropertySource)propertySource;
				IPersist context = persistPropertySource.getContext();
				if (context instanceof Form)
				{
					var commandCreater = commandCreater((Form)context, propertyName, type, prepend, dropTargetIsSibling);
					return new CreateOverrideIfNeeededCommandWrapper(persistPropertySource, uuid, commandCreater);
				}
			}
			return null;
		}, "createComponent");
	}

	/**
	 * Create a command to create the component, the uuid may be different from the one supplied above
	 */
	private static Function<UUID, Command> commandCreater(Form form, String propertyName, String type,
		boolean prepend, boolean dropTargetIsSibling)
	{
		return (uuid) -> {
			CreateComponentOptions args = new CreateComponentOptions();
			args.setDropTargetUUID(uuid.toString());
			args.setGhostPropertyName(propertyName);
			args.setDropTargetIsSibling(dropTargetIsSibling);
			args.setPrepend(prepend);
			args.setType(type);
			return new CreateComponentCommand(form, args, null);
		};
	}

	@Override
	public void deleteComponent(IPersist persist)
	{
		//RAGTESTexecuteCommandOnForm(formEditor -> new FormElementDeleteCommand(persist));
	}

	@Override
	public IModificationSubject getModificationSubject()
	{
		return modificationSubject;
	}

	private Object executeCommandOnForm(IPropertySource persistPropertySource, Supplier<Command> buildCommand, String eventName)
	{
		IEditorPart editor = RetargetToEditorPersistProperties.openPersistEditor(persistPropertySource, false);
		if (editor != null)
		{
			CommandStack commandStack = editor.getAdapter(CommandStack.class);
			if (commandStack != null)
			{
				Command command = buildCommand.get();
				commandStack.execute(command);
				modificationSubject.fireModificationEvent(new ModificationEvent(eventName, null, persistPropertySource));
				if (command instanceof IRAGTEST)
				{
					return ((IRAGTEST< ? >)command).getRagtest();
				}
			}
		}
		return null;

	}
}