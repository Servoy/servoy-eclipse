/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

import org.eclipse.gef.ui.actions.EditorPartAction;
import org.eclipse.ui.PartInitException;

import com.servoy.eclipse.core.resource.DesignPagetype;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * An action to save some group of elements as template.
 */
public class SwitchToRfbEditorAction extends EditorPartAction
{
	public SwitchToRfbEditorAction(BaseVisualFormEditor editor)
	{
		super(editor);
	}

	@Override
	protected BaseVisualFormEditor getWorkbenchPart()
	{
		return (BaseVisualFormEditor)super.getWorkbenchPart();
	}

	@Override
	protected boolean calculateEnabled()
	{
		return true;
	}

	/**
	 * Initializes this action's text and images.
	 */
	@Override
	protected void init()
	{
		super.init();
		setText(DesignerActionFactory.SWITCH_TO_RFB_EDITOR_TEXT);
		setToolTipText(DesignerActionFactory.SWITCH_TO_RFB_EDITOR_TOOLTIP);
		setId(DesignerActionFactory.SWITCH_TO_RFB_EDITOR.getId());
		setImageDescriptor(DesignerActionFactory.SWITCH_TO_RFB_EDITOR_IMAGE);
	}

	@Override
	public void run()
	{
		try
		{
			getWorkbenchPart().setDesignPageType(DesignPagetype.Rfb);
		}
		catch (PartInitException e)
		{
			ServoyLog.logError(e);
		}
	}
}
