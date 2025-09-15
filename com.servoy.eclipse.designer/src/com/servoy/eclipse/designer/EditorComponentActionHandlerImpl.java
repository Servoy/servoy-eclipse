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

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.designer.editor.CreateOverrideIfNeeededCommandWrapper;
import com.servoy.eclipse.designer.editor.commands.FormElementDeleteCommand;
import com.servoy.eclipse.designer.editor.commands.RefreshingCommand;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.CreateComponentCommand;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.CreateComponentCommand.CreateComponentOptions;
import com.servoy.eclipse.ui.EditorActionsRegistry.EditorComponentActionHandler;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.RetargetToEditorPersistProperties;
import com.servoy.j2db.dataprocessing.IModificationSubject;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.dataprocessing.ModificationSubject;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.server.ngclient.template.PersistIdentifier;
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
		if (propertySource instanceof PersistPropertySource persistPropertySource && persistPropertySource.getContext() instanceof Form form)
		{
			var commandCreater = createCommandCreater(form, propertyName, type, prepend, dropTargetIsSibling);
			executeCommandOnForm(propertySource, new CreateOverrideIfNeeededCommandWrapper(persistPropertySource, uuid, commandCreater), "createComponent");
		}
	}

	/**
	 * Create a command to create the component, the uuid may be different from the one supplied above
	 */
	private static Function<UUID, Command> createCommandCreater(Form form, String propertyName, String type,
		boolean prepend, boolean dropTargetIsSibling)
	{
		return (uuid) -> {
			CreateComponentOptions args = new CreateComponentOptions();
			args.setDropTarget(PersistIdentifier.fromSimpleUUID(uuid));
			args.setGhostPropertyName(propertyName);
			args.setDropTargetIsSibling(dropTargetIsSibling);
			args.setPrepend(prepend);
			args.setType(type);
			return new CreateComponentCommand(form, args, null);
		};
	}

	@Override
	public void deleteComponent(IPropertySource propertySource, UUID uuid)
	{
		if (propertySource instanceof PersistPropertySource persistPropertySource)
		{
			// only delete an element when it can be found in the context (form), otherwise it is a override
			persistPropertySource.getContext().searchChild(uuid).map(FormElementDeleteCommand::new)
				.ifPresent(command -> executeCommandOnForm(propertySource, command, "deleteComponent"));
		}
	}

	@Override
	public IModificationSubject getModificationSubject()
	{
		return modificationSubject;
	}

	private void executeCommandOnForm(IPropertySource persistPropertySource, Command command, String eventName)
	{
		IEditorPart editor = RetargetToEditorPersistProperties.openPersistEditor(persistPropertySource, false);
		if (editor != null)
		{
			CommandStack commandStack = editor.getAdapter(CommandStack.class);
			if (commandStack != null)
			{
				commandStack.execute(new RefreshingCommand<>(command,
					() -> modificationSubject.fireModificationEvent(new ModificationEvent(eventName, null, persistPropertySource))));
			}
		}
	}
}