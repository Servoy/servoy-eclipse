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

import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.gef.AutoexposeHelper;
import org.eclipse.gef.editparts.FreeformGraphicalRootEditPart;

/**
 * Root pane for forms, add datarender-layer.
 * 
 * @author rgansevles
 * 
 */
public class BaseFormGraphicalRootEditPart extends FreeformGraphicalRootEditPart
{
	private final BaseVisualFormEditor editorPart;

	public BaseFormGraphicalRootEditPart(BaseVisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
	}

	public BaseVisualFormEditor getEditorPart()
	{
		return editorPart;
	}

	@Override
	public Object getAdapter(Class adapter)
	{
		if (adapter == AutoexposeHelper.class) return new CustomViewportAutoexposeHelper(this, new Insets(100), true);
		return super.getAdapter(adapter);
	}
}
