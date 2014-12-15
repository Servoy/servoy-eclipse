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

import java.awt.Dimension;
import java.awt.Point;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.GhostBean;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Tab;

/**
 * @author user
 *
 */
public class SetPropertiesHandler implements IServerService
{

	private final BaseVisualFormEditor editorPart;

	/**
	 * @param editorPart
	 */
	public SetPropertiesHandler(BaseVisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
	}

	/**
	 * @param methodName
	 * @param args
	 */
	public Object executeMethod(String methodName, final JSONObject args)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				CompoundCommand cc = new CompoundCommand();
				Iterator keys = args.keys();
				while (keys.hasNext())
				{
					String uuid = (String)keys.next();
					final IPersist persist = PersistFinder.INSTANCE.searchForPersist(editorPart, uuid);
					if ((persist instanceof BaseComponent || persist instanceof Tab) && !(persist instanceof GhostBean))
					{
						JSONObject properties = args.optJSONObject(uuid);
						Iterator it = properties.keys();
						while (it.hasNext())
						{
							String propertyName = (String)it.next();
							if (!Arrays.asList("x", "y", "width", "height").contains(propertyName))
							{
								cc.add(new SetPropertyCommand("propertyName", PersistPropertySource.createPersistPropertySource(persist, false), propertyName,
									properties.opt(propertyName)));
							}
						}
						if (properties.has("x") && properties.has("y"))
						{
							cc.add(new SetPropertyCommand("move", PersistPropertySource.createPersistPropertySource(persist, false),
								StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), new Point(properties.optInt("x"), properties.optInt("y"))));
						}
						if (properties.has("width") && properties.has("height"))
						{
							cc.add(new SetPropertyCommand("resize", PersistPropertySource.createPersistPropertySource(persist, false),
								StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), new Dimension(properties.optInt("width"), properties.optInt("height"))));
						}
					}
					else if (persist instanceof Part)
					{
						JSONObject properties = args.optJSONObject(uuid);
						cc = new CompoundCommand();
						if (properties.has("y"))
						{
							cc.add(new SetPropertyCommand("resize", PersistPropertySource.createPersistPropertySource(persist, false),
								StaticContentSpecLoader.PROPERTY_HEIGHT.getPropertyName(), new Integer(properties.optInt("y"))));
						}
					}
					else if (persist instanceof Form)
					{
						JSONObject properties = args.optJSONObject(uuid);
						cc = new CompoundCommand();
						if (properties.has("width"))
						{
							cc.add(new SetPropertyCommand("formwidth", PersistPropertySource.createPersistPropertySource(persist, false),
								StaticContentSpecLoader.PROPERTY_WIDTH.getPropertyName(), new Integer(properties.optInt("width"))));
						}

					}
				}
				if (!cc.isEmpty()) editorPart.getCommandStack().execute(cc);
			}
		});
		return null;
	}
}
