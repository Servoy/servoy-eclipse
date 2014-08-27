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

package com.servoy.eclipse.designer.editor.rfb;

import java.awt.Point;
import java.util.Iterator;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.UUID;

/**
 * Handle requests from the rfb html editor.
 *
 * @author rgansevles
 *
 */
public class EditorServiceHandler implements IServerService
{
	private final BaseVisualFormEditor editorPart;
	private final ISelectionProvider selectionProvider;

	public EditorServiceHandler(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
	}

	@Override
	public Object executeMethod(String methodName, JSONObject args)
	{
		try
		{
			if ("getFormLayoutGrid".equals(methodName))
			{
				return editorPart.getForm().getLayoutGrid();
			}

			// void methods
			if ("setSelection".equals(methodName))
			{
				JSONArray json = args.getJSONArray("selection");
				final Object[] selection = new Object[json.length()];
				for (int i = 0; i < json.length(); i++)
				{
					selection[i] = ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).searchPersist(UUID.fromString(json.getString(i)));
				}
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						selectionProvider.setSelection(selection.length == 0 ? null : new StructuredSelection(selection));
					}
				});
			}
			else if ("setPosition".equals(methodName))
			{
				Iterator keys = args.keys();
				while (keys.hasNext())
				{
					String uuid = (String)keys.next();
					IPersist persist = ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).searchPersist(UUID.fromString(uuid));
					if (persist instanceof BaseComponent)
					{
						JSONObject position = args.getJSONObject(uuid);
						((BaseComponent)persist).setLocation(new Point(position.getInt("x"), position.getInt("y")));
					}
				}

			}
		}
		catch (JSONException e)
		{
			ServoyLog.logError(e);
		}

		return null;
	}
}
