/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;

/**
 * @author user
 *
 */
public class UpdateFieldPositioner implements IServerService
{
	private final IFieldPositioner fieldPositioner;
	private final BaseVisualFormEditor editorPart;

	/**
	 * @param editorPart
	 * @param selectionListener
	 * @param selectionProvider
	 */
	public UpdateFieldPositioner(BaseVisualFormEditor editorPart, IFieldPositioner fieldPositioner)
	{
		this.fieldPositioner = fieldPositioner;
		this.editorPart = editorPart;
	}

	/**
	 * @param methodName
	 * @param args
	 */
	public Object executeMethod(String methodName, final JSONObject args)
	{
		JSONObject location = args.optJSONObject("location");
		if (location.optInt("x") >= editorPart.getForm().getWidth() || location.optInt("y") >= editorPart.getForm().getHeight())
		{
			fieldPositioner.setDefaultLocation(new org.eclipse.swt.graphics.Point(40, 50));
			return null;
		}
		fieldPositioner.setDefaultLocation(new org.eclipse.swt.graphics.Point(location.optInt("x"), location.optInt("y")));
		return null;
	}
}
