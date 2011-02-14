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

import org.eclipse.gef.EditPart;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.util.DesignerUtil;

/**
 * An action to change the z-order of selected objects.
 */
public class SendToBackAction extends DesignerSelectionAction
{

	public SendToBackAction(IWorkbenchPart part)
	{
		super(part, VisualFormEditor.REQ_SEND_TO_BACK);
	}

	@Override
	protected Iterable<EditPart> getToRefresh(Iterable<EditPart> affected)
	{
		return DesignerUtil.getFormEditparts(affected);
	}

	/**
	 * Initializes this action's text and images.
	 */
	@Override
	protected void init()
	{
		super.init();
		setText(DesignerActionFactory.SEND_TO_BACK_TEXT);
		setToolTipText(DesignerActionFactory.SEND_TO_BACK_TOOLTIP);
		setId(DesignerActionFactory.SEND_TO_BACK.getId());
		setImageDescriptor(DesignerActionFactory.SEND_TO_BACK_IMAGE);
	}
}
