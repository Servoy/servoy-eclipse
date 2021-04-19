/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISaveablePart;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;

/**
 * @author Diana
 *
 */
public class RfbDirtyListener implements IPropertyListener
{
	private final BaseVisualFormEditor editorPart;
	private final EditorWebsocketSession editorWebsocketSession;

	/**
	 * @param editorPart
	 * @param editorWebsocketSession
	 */
	public RfbDirtyListener(BaseVisualFormEditor editorPart, EditorWebsocketSession editorWebsocketSession)
	{
		super();
		this.editorPart = editorPart;
		this.editorWebsocketSession = editorWebsocketSession;
	}


	@Override
	public void propertyChanged(Object source, int propId)
	{
		if (propId == IEditorPart.PROP_DIRTY)
		{
			editorWebsocketSession.getEventDispatcher().addEvent(new Runnable()
			{
				@Override
				public void run()
				{
					editorWebsocketSession.getClientService(EditorWebsocketSession.EDITOR_SERVICE).executeAsyncServiceCall("setDirty",
						new Object[] { ((ISaveablePart)editorPart).isDirty() });
				}
			});
		}
	}
}
