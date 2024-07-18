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

import static com.servoy.eclipse.designer.util.DesignerUtil.getActiveEditor;

import java.util.function.Function;

import org.eclipse.gef.commands.Command;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.CreateOverrideIfNeeededCommandWrapper;
import com.servoy.eclipse.designer.editor.commands.FormElementDeleteCommand;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.CreateComponentCommand;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.CreateComponentCommand.CreateComponentOptions;
import com.servoy.eclipse.ui.EditorActionsRegistry.EditorComponentActionHandler;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.UUID;

/**
 * Handler to create/delete a component (or subcomponent in array property) using commands from the designer.
 *
 * @author rgansevles
 */
public class EditorComponentActionHandlerImpl implements EditorComponentActionHandler
{
	public static EditorComponentActionHandlerImpl EDITOR_COMPONENT_ACTION_HANDLER = new EditorComponentActionHandlerImpl();

	private EditorComponentActionHandlerImpl()
	{
	}

	@Override
	public void createComponent(IPropertySource persistPropertySource, UUID uuid, String propertyName, String type, boolean prepend,
		boolean dropTargetIsSibling)
	{
		executeCommandOnForm(formEditor -> {
			if (persistPropertySource instanceof PersistPropertySource)
			{
				var commandCreater = commandCreater(formEditor, propertyName, type, prepend, dropTargetIsSibling);
				return new CreateOverrideIfNeeededCommandWrapper((PersistPropertySource)persistPropertySource, uuid, commandCreater);
			}
			return null;
		});
	}

	/**
	 * Create a command to create the component, the uuid may be different from the one supplied above
	 */
	private static Function<UUID, Command> commandCreater(BaseVisualFormEditor formEditor, String propertyName, String type,
		boolean prepend, boolean dropTargetIsSibling)
	{
		return (uuid) -> {
			CreateComponentOptions args = new CreateComponentOptions();
			args.setDropTargetUUID(uuid.toString());
			args.setGhostPropertyName(propertyName);
			args.setDropTargetIsSibling(dropTargetIsSibling);
			args.setPrepend(prepend);
			args.setType(type);
			return new CreateComponentCommand(formEditor, args, null);
		};
	}

	@Override
	public void deleteComponent(IPersist persist)
	{
		executeCommandOnForm(formEditor -> new FormElementDeleteCommand(persist));
	}

	private static void executeCommandOnForm(Function<BaseVisualFormEditor, Command> buildCommand)
	{
		BaseVisualFormEditor formEditor = getActiveEditor();
		if (formEditor != null)
		{
			Command command = buildCommand.apply(formEditor);
			formEditor.getCommandStack().execute(command);
		}
	}
}