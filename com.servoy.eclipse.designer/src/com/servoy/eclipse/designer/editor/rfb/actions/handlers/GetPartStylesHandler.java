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

import java.io.StringWriter;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.server.ngclient.template.PartWrapper;

/**
 * @author user
 *
 */
public class GetPartStylesHandler implements IServerService
{

	private final BaseVisualFormEditor editorPart;

	public GetPartStylesHandler(BaseVisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
	}

	/**
	 * @param methodName
	 * @param args
	 * @return
	 * @throws JSONException
	 */
	public Object executeMethod(String methodName, JSONObject args) throws JSONException
	{
		StringWriter stringWriter = new StringWriter();
		final JSONWriter writer = new JSONWriter(stringWriter);

		writer.array();

		Iterator<Part> partsIte = editorPart.getForm().getParts();
		while (partsIte.hasNext())
		{
			PartWrapper partWrapper = new PartWrapper(partsIte.next(), editorPart.getForm(), null);
			writer.object();
			writer.key("name").value(partWrapper.getName());
			writer.key("style").value(new JSONObject(partWrapper.getStyle()));
			writer.endObject();
		}

		writer.endArray();

		return new JSONArray(stringWriter.getBuffer().toString());
	}
}
