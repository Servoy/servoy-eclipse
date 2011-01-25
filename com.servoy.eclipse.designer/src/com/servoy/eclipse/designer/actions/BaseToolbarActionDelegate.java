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
package com.servoy.eclipse.designer.actions;

import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

import com.servoy.eclipse.designer.editor.commands.DesignerToolbarAction;

/**
 * Base class for EditpartActionDelegate actions who are wrappers around actions on the form designer toolbar.
 * 
 * @author rgansevles
 * 
 */
public abstract class BaseToolbarActionDelegate extends AbstractEditpartActionDelegate
{
	private DesignerToolbarAction designerToolbarAction;

	public BaseToolbarActionDelegate()
	{
		super(null);
	}

	@Override
	public void init(IWorkbenchWindow window)
	{
		designerToolbarAction = createToolbarAction(window.getActivePage().getActivePart());
		super.init(window);
	}

	protected abstract DesignerToolbarAction createToolbarAction(IWorkbenchPart part);

	@Override
	public boolean calculateEnabled(List<EditPart> editParts)
	{
		return designerToolbarAction.calculateEnabled(editParts);
	}

	@Override
	protected Request createRequest(EditPart editPart)
	{
		return designerToolbarAction.createRequest(editPart);
	}

	@Override
	public void dispose()
	{
		designerToolbarAction.dispose();
		designerToolbarAction = null;
	}

}
