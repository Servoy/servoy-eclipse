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

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;

import com.servoy.eclipse.designer.editor.commands.DesignerActionFactory;


/**
 * Provides context menu actions for the VisualFormEditor.
 */
class VisualFormEditorContextMenuProvider extends MenuManager implements IMenuListener
{
	private final EditPartViewer viewer;

	/** The editor's action registry. */
	private final ActionRegistry actionRegistry;

	/**
	 * Instantiate a new menu context provider for the specified EditPartViewer and ActionRegistry.
	 * 
	 * @param viewer the editor's graphical viewer
	 * @param registry the editor's action registry
	 * @throws IllegalArgumentException if registry is <tt>null</tt>.
	 */
	public VisualFormEditorContextMenuProvider(String id, EditPartViewer viewer, ActionRegistry registry)
	{
		super(id, id);
		this.viewer = viewer;
		addMenuListener(this);
		setRemoveAllWhenShown(true);
		if (registry == null)
		{
			throw new IllegalArgumentException();
		}
		actionRegistry = registry;
	}

	/**
	 * Returns the EditPartViewer
	 * 
	 * @return the viewer
	 */
	protected EditPartViewer getViewer()
	{
		return viewer;
	}

	/**
	 * @see IMenuListener#menuAboutToShow(IMenuManager)
	 */
	public void menuAboutToShow(IMenuManager menu)
	{
		buildContextMenu(menu);
	}


	/**
	 * Called when the context menu is about to show. Actions, whose state is enabled, will appear in the context menu.
	 * 
	 * @see org.eclipse.gef.ContextMenuProvider#buildContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void buildContextMenu(IMenuManager menu)
	{
		// Add standard action groups to the menu, but in the order we want to.
		menu.add(new Separator(GEFActionConstants.GROUP_UNDO));
		menu.add(new Separator(GEFActionConstants.GROUP_COPY));
		menu.add(new Separator(DesignerActionFactory.GROUP_Z_ORDER));
		menu.add(new Separator(DesignerActionFactory.GROUP_GROUPING));
		menu.add(new Separator(DesignerActionFactory.GROUP_ANCHORING));
		menu.add(new Separator(GEFActionConstants.GROUP_REST));
		menu.add(new Separator(IWorkbenchActionConstants.SAVE_EXT));
		// Placeholder for contributions from other plugins
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(new Separator(IWorkbenchActionConstants.SHOW_EXT));
		menu.add(new Separator(IWorkbenchActionConstants.OPEN_EXT));

		// Add actions to the menu
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, actionRegistry.getAction(ActionFactory.UNDO.getId()));
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, actionRegistry.getAction(ActionFactory.REDO.getId()));
		menu.appendToGroup(GEFActionConstants.GROUP_COPY, actionRegistry.getAction(ActionFactory.CUT.getId()));
		menu.appendToGroup(GEFActionConstants.GROUP_COPY, actionRegistry.getAction(ActionFactory.COPY.getId()));
		menu.appendToGroup(GEFActionConstants.GROUP_COPY, actionRegistry.getAction(ActionFactory.PASTE.getId()));
		menu.appendToGroup(GEFActionConstants.GROUP_COPY, actionRegistry.getAction(ActionFactory.DELETE.getId()));
		menu.appendToGroup(DesignerActionFactory.GROUP_Z_ORDER, actionRegistry.getAction(DesignerActionFactory.BRING_TO_FRONT.getId()));
		menu.appendToGroup(DesignerActionFactory.GROUP_Z_ORDER, actionRegistry.getAction(DesignerActionFactory.SEND_TO_BACK.getId()));
		menu.appendToGroup(DesignerActionFactory.GROUP_GROUPING, actionRegistry.getAction(DesignerActionFactory.GROUP.getId()));
		menu.appendToGroup(DesignerActionFactory.GROUP_GROUPING, actionRegistry.getAction(DesignerActionFactory.UNGROUP.getId()));
		menu.appendToGroup(DesignerActionFactory.GROUP_ANCHORING, actionRegistry.getAction(DesignerActionFactory.ANCHOR_TOP_TOGGLE.getId()));
		menu.appendToGroup(DesignerActionFactory.GROUP_ANCHORING, actionRegistry.getAction(DesignerActionFactory.ANCHOR_RIGHT_TOGGLE.getId()));
		menu.appendToGroup(DesignerActionFactory.GROUP_ANCHORING, actionRegistry.getAction(DesignerActionFactory.ANCHOR_BOTTOM_TOGGLE.getId()));
		menu.appendToGroup(DesignerActionFactory.GROUP_ANCHORING, actionRegistry.getAction(DesignerActionFactory.ANCHOR_LEFT_TOGGLE.getId()));
		menu.appendToGroup(IWorkbenchActionConstants.SAVE_EXT, actionRegistry.getAction(DesignerActionFactory.SAVE_AS_TEMPLATE.getId()));
		menu.appendToGroup(GEFActionConstants.GROUP_REST, actionRegistry.getAction(DesignerActionFactory.SET_TAB_SEQUENCE.getId()));
	}
}
