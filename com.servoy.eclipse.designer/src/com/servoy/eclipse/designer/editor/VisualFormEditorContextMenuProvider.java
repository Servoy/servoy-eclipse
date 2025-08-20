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
package com.servoy.eclipse.designer.editor;

import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;

import com.servoy.eclipse.designer.editor.commands.DesignerActionFactory;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.OpenScriptHandler;
import com.servoy.eclipse.designer.editor.rfb.menu.OpenSuperformsInScriptEditor;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.persistence.Form;


/**
 * Provides context menu actions for the VisualFormEditor.
 */
class VisualFormEditorContextMenuProvider extends BaseVisualFormEditorContextMenuProvider
{
	/**
	 * Instantiate a new menu context provider for the specified EditPartViewer and ActionRegistry.
	 *
	 * @param viewer the editor's graphical viewer
	 * @param registry the editor's action registry
	 * @throws IllegalArgumentException if registry is <tt>null</tt>.
	 */
	public VisualFormEditorContextMenuProvider(String id, ActionRegistry registry)
	{
		super(id, registry);
	}

	@Override
	public void addContextmenuActions(IMenuManager menu)
	{
		// Add actions to the menu
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, actionRegistry.getAction(ActionFactory.UNDO.getId()));
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, actionRegistry.getAction(ActionFactory.REDO.getId()));
		menu.appendToGroup(GEFActionConstants.GROUP_COPY, actionRegistry.getAction(ActionFactory.CUT.getId()));
		menu.appendToGroup(GEFActionConstants.GROUP_COPY, actionRegistry.getAction(ActionFactory.COPY.getId()));
		menu.appendToGroup(GEFActionConstants.GROUP_COPY, actionRegistry.getAction(ActionFactory.PASTE.getId()));
		menu.appendToGroup(GEFActionConstants.GROUP_COPY, actionRegistry.getAction(ActionFactory.DELETE.getId()));
		menu.appendToGroup(IWorkbenchActionConstants.SAVE_EXT, actionRegistry.getAction(DesignerActionFactory.SAVE_AS_TEMPLATE.getId()));

		//we need to set the action definition id for "Open super in script editor" because it doesn't work setting it in plugin.xml
		Form form = DesignerUtil.getActiveEditor().getForm();
		if (form != null && form.getExtendsID() != null)
		{
			MenuManager openSubmenu = new MenuManager(OpenSuperformsInScriptEditor.OPEN_SUPER_SCRIPT_MENU_LABEL,
				OpenSuperformsInScriptEditor.OPEN_SUPER_SCRIPT_MENU_ID);
			openSubmenu.setActionDefinitionId(OpenScriptHandler.OPEN_SUPER_SCRIPT_ID);
			openSubmenu.setImageDescriptor(Activator.loadImageDescriptorFromBundle("js.png"));
			menu.add(openSubmenu);
		}
	}
}
