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
package com.servoy.eclipse.designer.editor.commands;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.RetargetAction;

import com.servoy.eclipse.designer.Activator;
import com.servoy.eclipse.designer.actions.ZOrderAction;


/**
 * Factory for actions in form designer.
 *
 * @author rgansevles
 */

public abstract class DesignerActionFactory extends ActionFactory
{
	public static final String GROUP_ID = "com.servoy.eclipse.designer.rfb.group";
	public static final String GROUP_ELEMENTS = "group.elements";
	public static final String GROUP_ACTIONS = "group.actions";
	public static final String GROUP_REFACTOR = "group.refactor";

	protected DesignerActionFactory(String actionId)
	{
		super(actionId);
	}

	public static final String BRING_TO_FRONT_TEXT = "Bring to front";
	public static final String BRING_TO_FRONT_TOOLTIP = BRING_TO_FRONT_TEXT;
	public static final ImageDescriptor BRING_TO_FRONT_IMAGE = Activator.loadImageDescriptorFromBundle("bring_to_front.png");
	public static final String BRING_TO_FRONT_ID = "com.servoy.eclipse.designer.rfb.bringtofront";
	public static final ActionFactory BRING_TO_FRONT = new ActionFactory(ZOrderAction.ID_Z_ORDER_BRING_TO_FRONT)
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), BRING_TO_FRONT_TEXT);
			action.setToolTipText(BRING_TO_FRONT_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(BRING_TO_FRONT_IMAGE);
			return action;
		}
	};

	public static final String SEND_TO_BACK_TEXT = "Send to back";
	public static final String SEND_TO_BACK_TOOLTIP = SEND_TO_BACK_TEXT;
	public static final ImageDescriptor SEND_TO_BACK_IMAGE = Activator.loadImageDescriptorFromBundle("send_to_back.png");
	public static final String SEND_TO_BACK_ID = "com.servoy.eclipse.designer.rfb.sendtoback";
	public static final ActionFactory SEND_TO_BACK = new ActionFactory(ZOrderAction.ID_Z_ORDER_SEND_TO_BACK)
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), SEND_TO_BACK_TEXT);
			action.setToolTipText(SEND_TO_BACK_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(SEND_TO_BACK_IMAGE);
			return action;
		}
	};

	public static final String BRING_TO_FRONT_ONE_STEP_TEXT = "Bring forward";
	public static final String BRING_TO_FRONT_ONE_STEP_TOOLTIP = BRING_TO_FRONT_ONE_STEP_TEXT;
	public static final ImageDescriptor BRING_TO_FRONT_ONE_STEP_IMAGE = Activator.loadImageDescriptorFromBundle("bring_forward.png");
	public static final String BRING_TO_FRONT_ONE_STEP_ID = "com.servoy.eclipse.designer.rfb.bringtofrontonestep";
	public static final ActionFactory BRING_TO_FRONT_ONE_STEP = new ActionFactory(ZOrderAction.ID_Z_ORDER_BRING_TO_FRONT_ONE_STEP)
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), BRING_TO_FRONT_ONE_STEP_TEXT);
			action.setToolTipText(BRING_TO_FRONT_ONE_STEP_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(BRING_TO_FRONT_ONE_STEP_IMAGE);
			return action;
		}
	};

	public static final String SEND_TO_BACK_ONE_STEP_TEXT = "Send backward";
	public static final String SEND_TO_BACK_ONE_STEP_TOOLTIP = SEND_TO_BACK_ONE_STEP_TEXT;
	public static final ImageDescriptor SEND_TO_BACK_ONE_STEP_IMAGE = Activator.loadImageDescriptorFromBundle("send_backward.png");
	public static final String SEND_TO_BACK_ONE_STEP_ID = "com.servoy.eclipse.designer.rfb.sendtobackonestep";
	public static final ActionFactory SEND_TO_BACK_ONE_STEP = new ActionFactory(ZOrderAction.ID_Z_ORDER_SEND_TO_BACK_ONE_STEP)
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), SEND_TO_BACK_ONE_STEP_TEXT);
			action.setToolTipText(SEND_TO_BACK_ONE_STEP_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(SEND_TO_BACK_ONE_STEP_IMAGE);
			return action;
		}
	};

	public static final String SELECT_FEEDBACK_TEXT = "Visual Feedback Options";
	public static final String SELECT_FEEDBACK_TOOLTIP = SELECT_FEEDBACK_TEXT;
	public static final ImageDescriptor SELECT_FEEDBACK_IMAGE = Activator.loadImageDescriptorFromBundle("grid.gif");
	public static final ActionFactory SELECT_FEEDBACK = new ActionFactory("select-feedback-mode")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), SELECT_FEEDBACK_TEXT, IAction.AS_DROP_DOWN_MENU);
			action.setToolTipText(SELECT_FEEDBACK_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(SELECT_FEEDBACK_IMAGE);
			return action;
		}
	};

	public static final String TOGGLE_HIDE_INHERITED_TEXT = "Hide inherited elements";
	public static final String TOGGLE_HIDE_INHERITED_TOOLTIP = TOGGLE_HIDE_INHERITED_TEXT;
	public static final ImageDescriptor TOGGLE_HIDE_INHERITED_IMAGE = Activator.loadImageDescriptorFromBundle("hide_inherited.gif");
	public static final ActionFactory TOGGLE_HIDE_INHERITED = new ActionFactory("toggle-hide-inherited")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), TOGGLE_HIDE_INHERITED_TEXT, IAction.AS_PUSH_BUTTON);
			action.setToolTipText(TOGGLE_HIDE_INHERITED_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(TOGGLE_HIDE_INHERITED_IMAGE);
			return action;
		}
	};

	public static final String SELECT_SNAPMODE_TEXT = "Element Placement Guide Options";
	public static final String SELECT_SNAPMODE_TOOLTIP = SELECT_SNAPMODE_TEXT;
	public static final ImageDescriptor SELECT_SNAPTMODE_IMAGE = Activator.loadImageDescriptorFromBundle("snaptogrid.gif");
	public static final ActionFactory SELECT_SNAPMODE = new ActionFactory("select-snapmode")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), SELECT_SNAPMODE_TEXT, IAction.AS_DROP_DOWN_MENU);
			action.setToolTipText(SELECT_SNAPMODE_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(SELECT_SNAPTMODE_IMAGE);
			return action;
		}
	};

	public static final String DISTRIBUTE_HORIZONTAL_SPACING_TEXT = "Horizontal Spacing";
	public static final String DISTRIBUTE_HORIZONTAL_SPACING_TOOLTIP = DISTRIBUTE_HORIZONTAL_SPACING_TEXT;
	public static final ImageDescriptor DISTRIBUTE_HORIZONTAL_SPACING_IMAGE = Activator.loadImageDescriptorFromBundle("distribute_hspace.gif");
	public static final ActionFactory DISTRIBUTE_HORIZONTAL_SPACING = new ActionFactory("distribute-horizontal-spacing")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), DISTRIBUTE_HORIZONTAL_SPACING_TEXT);
			action.setToolTipText(DISTRIBUTE_HORIZONTAL_SPACING_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(DISTRIBUTE_HORIZONTAL_SPACING_IMAGE);
			return action;
		}
	};

	public static final String DISTRIBUTE_HORIZONTAL_CENTER_TEXT = "Horizontal Centers";
	public static final String DISTRIBUTE_HORIZONTAL_CENTER_TOOLTIP = DISTRIBUTE_HORIZONTAL_CENTER_TEXT;
	public static final ImageDescriptor DISTRIBUTE_HORIZONTAL_CENTER_IMAGE = Activator.loadImageDescriptorFromBundle("distribute_hcenters.gif");
	public static final ActionFactory DISTRIBUTE_HORIZONTAL_CENTER = new ActionFactory("distribute-horizontal-center")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), DISTRIBUTE_HORIZONTAL_CENTER_TEXT);
			action.setToolTipText(DISTRIBUTE_HORIZONTAL_CENTER_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(DISTRIBUTE_HORIZONTAL_CENTER_IMAGE);
			return action;
		}
	};

	public static final String DISTRIBUTE_HORIZONTAL_PACK_TEXT = "Leftward";
	public static final String DISTRIBUTE_HORIZONTAL_PACK_TOOLTIP = DISTRIBUTE_HORIZONTAL_PACK_TEXT;
	public static final ImageDescriptor DISTRIBUTE_HORIZONTAL_PACK_IMAGE = Activator.loadImageDescriptorFromBundle("distribute_leftward.gif");
	public static final ActionFactory DISTRIBUTE_HORIZONTAL_PACK = new ActionFactory("distribute-horizontal-pack")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), DISTRIBUTE_HORIZONTAL_PACK_TEXT);
			action.setToolTipText(DISTRIBUTE_HORIZONTAL_PACK_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(DISTRIBUTE_HORIZONTAL_PACK_IMAGE);
			return action;
		}
	};

	public static final String DISTRIBUTE_VERTICAL_SPACING_TEXT = "Vertical Spacing";
	public static final String DISTRIBUTE_VERTICAL_SPACING_TOOLTIP = DISTRIBUTE_VERTICAL_SPACING_TEXT;
	public static final ImageDescriptor DISTRIBUTE_VERTICAL_SPACING_IMAGE = Activator.loadImageDescriptorFromBundle("distribute_vspace.gif");
	public static final ActionFactory DISTRIBUTE_VERTICAL_SPACING = new ActionFactory("distribute-vertical-spacing")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), DISTRIBUTE_VERTICAL_SPACING_TEXT);
			action.setToolTipText(DISTRIBUTE_VERTICAL_SPACING_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(DISTRIBUTE_VERTICAL_SPACING_IMAGE);
			return action;
		}
	};

	public static final String DISTRIBUTE_VERTICAL_CENTER_TEXT = "Vertical Centers";
	public static final String DISTRIBUTE_VERTICAL_CENTER_TOOLTIP = DISTRIBUTE_VERTICAL_CENTER_TEXT;
	public static final ImageDescriptor DISTRIBUTE_VERTICAL_CENTER_IMAGE = Activator.loadImageDescriptorFromBundle("distribute_vcenters.gif");
	public static final ActionFactory DISTRIBUTE_VERTICAL_CENTER = new ActionFactory("distribute-vertical-center")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), DISTRIBUTE_VERTICAL_CENTER_TEXT);
			action.setToolTipText(DISTRIBUTE_VERTICAL_CENTER_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(DISTRIBUTE_VERTICAL_CENTER_IMAGE);
			return action;
		}
	};

	public static final String DISTRIBUTE_VERTICAL_PACK_TEXT = "Upward";
	public static final String DISTRIBUTE_VERTICAL_PACK_TOOLTIP = DISTRIBUTE_VERTICAL_PACK_TEXT;
	public static final ImageDescriptor DISTRIBUTE_VERTICAL_PACK_IMAGE = Activator.loadImageDescriptorFromBundle("distribute_upward.gif");
	public static final ActionFactory DISTRIBUTE_VERTICAL_PACK = new ActionFactory("distribute-vertical-pack")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), DISTRIBUTE_VERTICAL_PACK_TEXT);
			action.setToolTipText(DISTRIBUTE_VERTICAL_PACK_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(DISTRIBUTE_VERTICAL_PACK_IMAGE);
			return action;
		}
	};

	public static final String SET_TAB_SEQUENCE_TEXT = "Set tab sequence";
	public static final String SET_TAB_SEQUENCE_TOOLTIP = SET_TAB_SEQUENCE_TEXT;
	public static final ImageDescriptor SET_TAB_SEQUENCE_IMAGE = Activator.loadImageDescriptorFromBundle("th_horizontal.gif"); // TODO create proper icon
	public static final String SET_TAB_SEQUENCE_ID = "com.servoy.eclipse.designer.rfb.settabseq";
	public static final ActionFactory SET_TAB_SEQUENCE = new ActionFactory("set-tab-sequence")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), SET_TAB_SEQUENCE_TEXT);
			action.setToolTipText(SET_TAB_SEQUENCE_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(SET_TAB_SEQUENCE_IMAGE);
			return action;
		}
	};

	public static final String GROUP_TEXT = "Group";
	public static final String GROUP_TOOLTIP = GROUP_TEXT;
	public static final ImageDescriptor GROUP_IMAGE = Activator.loadImageDescriptorFromBundle("group.gif");
	public static final ActionFactory GROUP = new ActionFactory("set-group-id")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), GROUP_TEXT);
			action.setToolTipText(GROUP_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(GROUP_IMAGE);
			return action;
		}
	};

	public static final String UNGROUP_ID = "com.servoy.eclipse.designer.rfb.ungroup";
	public static final String UNGROUP_TEXT = "Ungroup";
	public static final String UNGROUP_TOOLTIP = UNGROUP_TEXT;
	public static final ImageDescriptor UNGROUP_IMAGE = Activator.loadImageDescriptorFromBundle("ungroup.gif");
	public static final ActionFactory UNGROUP = new ActionFactory("clear-group-id")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), UNGROUP_TEXT);
			action.setToolTipText(UNGROUP_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(UNGROUP_IMAGE);
			return action;
		}
	};

	public static final String SAVE_AS_TEMPLATE_TEXT = "Save as template...";
	public static final String SAVE_AS_TEMPLATE_TOOLTIP = SAVE_AS_TEMPLATE_TEXT;
	public static final ImageDescriptor SAVE_AS_TEMPLATE_IMAGE = Activator.loadImageDescriptorFromBundle("template.gif");
	public static final ActionFactory SAVE_AS_TEMPLATE = new ActionFactory("save-as-template-id")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), SAVE_AS_TEMPLATE_TEXT);
			action.setToolTipText(SAVE_AS_TEMPLATE_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(SAVE_AS_TEMPLATE_IMAGE);
			return action;
		}
	};

	public static final String SWITCH_TO_RFB_EDITOR_TEXT = "Switch to HTML5 form editor";
	public static final String SWITCH_TO_RFB_EDITOR_TOOLTIP = SWITCH_TO_RFB_EDITOR_TEXT;
	public static final ImageDescriptor SWITCH_TO_RFB_EDITOR_IMAGE = Activator.loadImageDescriptorFromBundle("html5.png");
	public static final ActionFactory SWITCH_TO_RFB_EDITOR = new ActionFactory("switch-to-rfb-editor-id")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), SWITCH_TO_RFB_EDITOR_TEXT);
			action.setToolTipText(SWITCH_TO_RFB_EDITOR_TOOLTIP);
			window.getPartService().addPartListener(action);
//			action.setActionDefinitionId("org.eclipse.ui.edit." + getId());
			action.setImageDescriptor(SWITCH_TO_RFB_EDITOR_IMAGE);
			return action;
		}
	};

	public static final String SAME_WIDTH_TEXT = "Same width";
	public static final String SAME_WIDTH_TOOLTIP = SAME_WIDTH_TEXT;
	public static final ImageDescriptor SAME_WIDTH_IMAGE = Activator.loadImageDescriptorFromBundle("same_width.gif");
	public static final String SAME_WIDTH_ID = "com.servoy.eclipse.designer.rfb.samewidth";
	public static final ActionFactory SAME_WIDTH = new ActionFactory("same-width")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), SAME_WIDTH_TEXT);
			action.setToolTipText(SAME_WIDTH_TOOLTIP);
			window.getPartService().addPartListener(action);
			action.setImageDescriptor(SAME_WIDTH_IMAGE);
			return action;
		}
	};

	public static final String SAME_HEIGHT_TEXT = "Same height";
	public static final String SAME_HEIGHT_TOOLTIP = SAME_HEIGHT_TEXT;
	public static final ImageDescriptor SAME_HEIGHT_IMAGE = Activator.loadImageDescriptorFromBundle("same_height.gif");
	public static final String SAME_HEIGHT_ID = "com.servoy.eclipse.designer.rfb.sameheight";
	public static final ActionFactory SAME_HEIGHT = new ActionFactory("same-height")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), SAME_HEIGHT_TEXT);
			action.setToolTipText(SAME_HEIGHT_TOOLTIP);
			window.getPartService().addPartListener(action);
			action.setImageDescriptor(SAME_HEIGHT_IMAGE);
			return action;
		}
	};

	public static final String ADD_TAB_TEXT = "Place TabPanel Wizard";
	public static final String ADD_TAB_TOOLTIP = ADD_TAB_TEXT;
	public static final ImageDescriptor ADD_TAB_IMAGE = Activator.loadImageDescriptorFromBundle("tab.png");
	public static final ActionFactory ADD_TAB = new ActionFactory("place-tabpanel")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), ADD_TAB_TEXT);
			action.setToolTipText(ADD_TAB_TOOLTIP);
			window.getPartService().addPartListener(action);
			action.setImageDescriptor(ADD_TAB_IMAGE);
			return action;
		}
	};

	public static final String ADD_SPLITPANE_TEXT = "Place SplitPane Wizard";
	public static final String ADD_SPLITPANE_TOOLTIP = ADD_SPLITPANE_TEXT;
	public static final ImageDescriptor ADD_SPLITPANE_IMAGE = Activator.loadImageDescriptorFromBundle("split.png");
	public static final ActionFactory ADD_SPLITPANE = new ActionFactory("place-splitpane")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), ADD_SPLITPANE_TEXT);
			action.setToolTipText(ADD_SPLITPANE_TOOLTIP);
			window.getPartService().addPartListener(action);
			action.setImageDescriptor(ADD_SPLITPANE_IMAGE);
			return action;
		}
	};

	public static final String ADD_ACCORDION_TEXT = "Place Accordion Panel Wizard";
	public static final String ADD_ACCORDION_TOOLTIP = ADD_ACCORDION_TEXT;
	public static final ImageDescriptor ADD_ACCORDION_IMAGE = Activator.loadImageDescriptorFromBundle("accordion.jpg");
	public static final ActionFactory ADD_ACCORDION = new ActionFactory("place-accordionpanel")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), ADD_ACCORDION_TEXT);
			action.setToolTipText(ADD_ACCORDION_TOOLTIP);
			window.getPartService().addPartListener(action);
			action.setImageDescriptor(ADD_ACCORDION_IMAGE);
			return action;
		}
	};

	public static final String ADD_PORTAL_TEXT = "Place Portal Wizard";
	public static final String ADD_PORTAL_TOOLTIP = ADD_PORTAL_TEXT;
	public static final ImageDescriptor ADD_PORTAL_IMAGE = Activator.loadImageDescriptorFromBundle("portal.png");
	public static final ActionFactory ADD_PORTAL = new ActionFactory("place-portal")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), ADD_PORTAL_TEXT);
			action.setToolTipText(ADD_PORTAL_TOOLTIP);
			window.getPartService().addPartListener(action);
			action.setImageDescriptor(ADD_PORTAL_IMAGE);
			return action;
		}
	};

	public static final String ADD_FIELD_TEXT = "Place Field Wizard";
	public static final String ADD_FIELD_TOOLTIP = ADD_FIELD_TEXT;
	public static final ImageDescriptor ADD_FIELD_IMAGE = Activator.loadImageDescriptorFromBundle("textfield.png");
	public static final ActionFactory ADD_FIELD = new ActionFactory("place-field")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), ADD_FIELD_TEXT);
			action.setToolTipText(ADD_FIELD_TOOLTIP);
			window.getPartService().addPartListener(action);
			action.setImageDescriptor(ADD_FIELD_IMAGE);
			return action;
		}
	};

	public static final String ADD_MEDIA_TEXT = "Place Image Wizard";
	public static final String ADD_MEDIA_TOOLTIP = ADD_MEDIA_TEXT;
	public static final ImageDescriptor ADD_MEDIA_IMAGE = Activator.loadImageDescriptorFromBundle("media.png");
	public static final ActionFactory ADD_MEDIA = new ActionFactory("place-image")
	{

		@Override
		public IWorkbenchAction create(IWorkbenchWindow window)
		{
			if (window == null)
			{
				throw new IllegalArgumentException();
			}
			RetargetAction action = new RetargetAction(getId(), ADD_MEDIA_TEXT);
			action.setToolTipText(ADD_MEDIA_TOOLTIP);
			window.getPartService().addPartListener(action);
			action.setImageDescriptor(ADD_MEDIA_IMAGE);
			return action;
		}
	};
}
