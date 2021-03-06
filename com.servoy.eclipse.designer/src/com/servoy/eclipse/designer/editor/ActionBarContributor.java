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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.draw2d.PositionConstants;
import org.eclipse.gef.ui.actions.AlignmentRetargetAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.RetargetAction;

import com.servoy.eclipse.designer.editor.commands.DesignerActionFactory;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;

/**
 * ActionBarContributor, builds actions for form designer.
 * Actions are contributed to the toolbar and are declared as global action keys. 
 * 
 * @author rgansevles
 */

public class ActionBarContributor extends org.eclipse.gef.ui.actions.ActionBarContributor
{
	public ActionBarContributor()
	{
	}

	List<IWorkbenchAction> myActions = new ArrayList<IWorkbenchAction>();
	Set<Object> notOnToolbar = new HashSet<Object>();

	/**
	 * @see org.eclipse.gef.ui.actions.ActionBarContributor#createActions()
	 */
	@Override
	protected void buildActions()
	{
		boolean formToolsInFormWindow = !new DesignerPreferences().getFormToolsOnMainToolbar();

		addMyAction(ActionFactory.CUT.create(getPage().getWorkbenchWindow()), false);
		addMyAction(ActionFactory.DELETE.create(getPage().getWorkbenchWindow()), false);
		addMyAction(ActionFactory.UNDO.create(getPage().getWorkbenchWindow()), false);
		addMyAction(ActionFactory.REDO.create(getPage().getWorkbenchWindow()), false);
		addMyAction(ActionFactory.COPY.create(getPage().getWorkbenchWindow()), false);
		addMyAction(ActionFactory.PASTE.create(getPage().getWorkbenchWindow()), false);

		addMyAction(ActionFactory.SELECT_ALL.create(getPage().getWorkbenchWindow()), false);
		addMyAction(DesignerActionFactory.BRING_TO_FRONT.create(getPage().getWorkbenchWindow()), formToolsInFormWindow);
		addMyAction(DesignerActionFactory.SEND_TO_BACK.create(getPage().getWorkbenchWindow()), formToolsInFormWindow);
		addMyAction(DesignerActionFactory.GROUP.create(getPage().getWorkbenchWindow()), formToolsInFormWindow);
		addMyAction(DesignerActionFactory.UNGROUP.create(getPage().getWorkbenchWindow()), formToolsInFormWindow);
		addMyAction(DesignerActionFactory.SELECT_FEEDBACK.create(getPage().getWorkbenchWindow()), formToolsInFormWindow);
		addMyAction(DesignerActionFactory.SELECT_SNAPMODE.create(getPage().getWorkbenchWindow()), formToolsInFormWindow);
		addMyAction(new AlignmentRetargetAction(PositionConstants.LEFT), formToolsInFormWindow);
		addMyAction(new AlignmentRetargetAction(PositionConstants.RIGHT), formToolsInFormWindow);
		addMyAction(new AlignmentRetargetAction(PositionConstants.TOP), formToolsInFormWindow);
		addMyAction(new AlignmentRetargetAction(PositionConstants.BOTTOM), formToolsInFormWindow);
		addMyAction(new AlignmentRetargetAction(PositionConstants.CENTER), formToolsInFormWindow);
		addMyAction(new AlignmentRetargetAction(PositionConstants.MIDDLE), formToolsInFormWindow);
		addMyAction(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_SPACING.create(getPage().getWorkbenchWindow()), formToolsInFormWindow);
		addMyAction(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_CENTER.create(getPage().getWorkbenchWindow()), formToolsInFormWindow);
		addMyAction(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_PACK.create(getPage().getWorkbenchWindow()), formToolsInFormWindow);
		addMyAction(DesignerActionFactory.DISTRIBUTE_VERTICAL_SPACING.create(getPage().getWorkbenchWindow()), formToolsInFormWindow);
		addMyAction(DesignerActionFactory.DISTRIBUTE_VERTICAL_CENTER.create(getPage().getWorkbenchWindow()), formToolsInFormWindow);
		addMyAction(DesignerActionFactory.DISTRIBUTE_VERTICAL_PACK.create(getPage().getWorkbenchWindow()), formToolsInFormWindow);
		addMyAction(DesignerActionFactory.SAME_WIDTH.create(getPage().getWorkbenchWindow()), formToolsInFormWindow);
		addMyAction(DesignerActionFactory.SAME_HEIGHT.create(getPage().getWorkbenchWindow()), formToolsInFormWindow);
		addMyAction(DesignerActionFactory.SET_TAB_SEQUENCE.create(getPage().getWorkbenchWindow()), false);
		addMyAction(DesignerActionFactory.SAVE_AS_TEMPLATE.create(getPage().getWorkbenchWindow()), false);

		for (IWorkbenchAction action : myActions)
		{
			if (action instanceof RetargetAction)
			{
				addRetargetAction((RetargetAction)action);
			}
			else
			{
				addAction(action);
			}
		}
	}

	protected void addMyAction(IWorkbenchAction action, boolean onToolBar)
	{
		myActions.add(action);
		if (!onToolBar) notOnToolbar.add(action.getId());
	}

	/**
	 * @see org.eclipse.gef.ui.actions.ActionBarContributor#declareGlobalActionKeys()
	 */
	@Override
	protected void declareGlobalActionKeys()
	{
		for (IWorkbenchAction action : myActions)
		{
			addGlobalActionKey(action.getId());
		}
	}

	/**
	 * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToToolBar(IToolBarManager)
	 */
	@Override
	public void contributeToToolBar(IToolBarManager tbm)
	{
		for (IWorkbenchAction action : myActions)
		{
			if (!notOnToolbar.contains(action.getId()))
			{
				tbm.add(action);
			}
		}


//		TODO Need to revisit how these actions work.
//		tbm.add(getAction(ZoomAction.ACTION_ID));
//		tbm.add(getAction(ZoomInAction.ACTION_ID));
//		tbm.add(getAction(ZoomOutAction.ACTION_ID));

//		tbm.add(getAction(ShowGridAction.ACTION_ID));
//		tbm.add(getAction(SnapToGridAction.ACTION_ID));
//		tbm.add(getAction(GridPropertiesAction.ACTION_ID));

//		tbm.add(getAction(AlignmentAction.getActionId(AlignmentCommandRequest.LEFT_ALIGN)));
//		tbm.add(getAction(AlignmentAction.getActionId(AlignmentCommandRequest.CENTER_ALIGN)));
//		tbm.add(getAction(AlignmentAction.getActionId(AlignmentCommandRequest.RIGHT_ALIGN)));
//		tbm.add(getAction(AlignmentAction.getActionId(AlignmentCommandRequest.TOP_ALIGN)));
//		tbm.add(getAction(AlignmentAction.getActionId(AlignmentCommandRequest.MIDDLE_ALIGN)));
//		tbm.add(getAction(AlignmentAction.getActionId(AlignmentCommandRequest.BOTTOM_ALIGN)));
//		tbm.add(getAction(AlignmentAction.getActionId(AlignmentCommandRequest.MATCH_WIDTH)));
//		tbm.add(getAction(AlignmentAction.getActionId(AlignmentCommandRequest.MATCH_HEIGHT)));

//		tbm.add(getAction(ShowDistributeBoxAction.ACTION_ID));
//		tbm.add(getAction(DistributeAction.getActionId(DistributeCommandRequest.HORIZONTAL)));
//		tbm.add(getAction(DistributeAction.getActionId(DistributeCommandRequest.VERTICAL)));
	}
}
