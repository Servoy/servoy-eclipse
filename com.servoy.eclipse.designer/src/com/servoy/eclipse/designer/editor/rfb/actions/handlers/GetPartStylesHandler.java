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

import org.eclipse.swt.widgets.Display;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Form;
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
		final JSONObject[] getPartsStylesReturn = new JSONObject[1];
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				StringWriter stringWriter = new StringWriter();
				final JSONWriter writer = new JSONWriter(stringWriter);

				Form f = ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).getFlattenedForm(editorPart.getForm());
				try
				{
					writer.object();
					writer.key("formSize").object().key("width").value(f.getWidth()).key("height").value(f.getSize().getHeight()).endObject();
					writer.key("parts");
					writer.array();

					Iterator<Part> partsIte = f.getParts();
					while (partsIte.hasNext())
					{
						PartWrapper partWrapper = new PartWrapper(partsIte.next(), editorPart.getForm(), null, true);
						writer.object();
						writer.key("name").value(partWrapper.getName());
						writer.key("style").value(new JSONObject(partWrapper.getStyle()));
						writer.endObject();
					}

					writer.endArray();
					writer.endObject();

					getPartsStylesReturn[0] = new JSONObject(stringWriter.getBuffer().toString());
				}
				catch (JSONException ex)
				{
					ServoyLog.logError("Could not get parts style", ex);
				}
			}
		});

		return getPartsStylesReturn[0];
	}
}
