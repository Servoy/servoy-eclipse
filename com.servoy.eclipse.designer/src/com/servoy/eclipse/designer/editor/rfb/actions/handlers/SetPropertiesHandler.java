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

import com.servoy.eclipse.core.util.PersistFinder;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.util.SnapToComponentUtil;
import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportCSSPosition;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.server.ngclient.template.PersistIdentifier;

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
				ReorderCustomTypesCommand reorderCustomTypesCommand = null;
				Iterator keys = args.keys();
				while (keys.hasNext())
				{
					String persistIdentifierAsString = (String)keys.next();
					IPersist persist = PersistFinder.INSTANCE.searchForPersist(editorPart.getForm(),
						PersistIdentifier.fromJSONString(persistIdentifierAsString));
					if (persist != null)
					{
						PersistContext context = PersistContext.create(persist, editorPart.getForm());
						if (persist instanceof IFormElement || persist instanceof Tab || persist instanceof WebCustomType ||
							persist instanceof WebFormComponentChildType || persist instanceof ISupportCSSPosition)
						{
							JSONObject properties = args.optJSONObject(persistIdentifierAsString);
							Iterator it = properties.keys();
							while (it.hasNext())
							{
								String propertyName = (String)it.next();
								if (!Arrays.asList("x", "y", "width", "height", "move").contains(propertyName))
								{
									cc.add(new SetPropertyCommand("propertyName", PersistPropertySource.createPersistPropertySource(context, false),
										propertyName, properties.opt(propertyName)));
								}
							}
							if (persist instanceof WebFormComponentChildType)
							{
								persist = ((WebFormComponentChildType)persist).getElement();
							}
							if (persist instanceof ISupportCSSPosition && CSSPositionUtils.useCSSPosition(persist) &&
								(properties.has("x") || properties.has("y") ||
									properties.has("width") || properties.has("height")))
							{
								Point oldLocation = CSSPositionUtils.getLocation((ISupportCSSPosition)persist);
								Dimension oldSize = CSSPositionUtils.getSize((ISupportCSSPosition)persist);
								// we need to calculate the new cssposition
								CSSPosition newPosition = null;
								if (properties.has("cssPos"))
								{
									newPosition = SnapToComponentUtil.cssPositionFromJSON(editorPart.getForm(), persist, properties);
								}
								else
								{
									newPosition = CSSPositionUtils.adjustCSSPosition((ISupportCSSPosition)persist,
										properties.optInt("x", oldLocation.x), properties.optInt("y", oldLocation.y),
										properties.optInt("width", oldSize.width),
										properties.optInt("height", oldSize.height), properties.optBoolean("move", false));
								}
								cc.add(new SetPropertyCommand("resize", PersistPropertySource.createPersistPropertySource(context, false),
									StaticContentSpecLoader.PROPERTY_CSS_POSITION.getPropertyName(),
									newPosition));
							}
							else
							{
								if (properties.has("x") && properties.has("y"))
								{
									if (persist instanceof WebCustomType)
									{
										if (reorderCustomTypesCommand == null)
											reorderCustomTypesCommand = new ReorderCustomTypesCommand((WebCustomType)persist);
										cc.add(new MoveCustomTypeCommand((WebCustomType)persist, new Point(properties.optInt("x"), properties.optInt("y"))));
									}
									else
									{
										cc.add(new SetPropertyCommand("move", PersistPropertySource.createPersistPropertySource(context, false),
											StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(),
											new Point(properties.optInt("x"), properties.optInt("y"))));
									}

								}
								if (properties.has("width") && properties.has("height"))
								{
									cc.add(new SetPropertyCommand("resize", PersistPropertySource.createPersistPropertySource(context, false),
										StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(),
										new Dimension(properties.optInt("width"), properties.optInt("height"))));
								}
							}
						}
						else if (persist instanceof Part)
						{
							JSONObject properties = args.optJSONObject(persistIdentifierAsString);
							if (properties.has("y"))
							{
								cc.add(new SetPropertyCommand("resize", PersistPropertySource.createPersistPropertySource(context, false),
									StaticContentSpecLoader.PROPERTY_HEIGHT.getPropertyName(), new Integer(properties.optInt("y"))));
							}
							if (properties.has("x"))
							{
								cc.add(new SetPropertyCommand("resize", PersistPropertySource.createPersistPropertySource(context, false),
									StaticContentSpecLoader.PROPERTY_WIDTH.getPropertyName(), new Integer(properties.optInt("x"))));
							}
						}
						else if (persist instanceof Form)
						{
							JSONObject properties = args.optJSONObject(persistIdentifierAsString);
							if (properties.has("width"))
							{
								cc.add(new SetPropertyCommand("formwidth", PersistPropertySource.createPersistPropertySource(context, false),
									StaticContentSpecLoader.PROPERTY_WIDTH.getPropertyName(), new Integer(properties.optInt("width"))));
							}
						}
					}
				}

				if (reorderCustomTypesCommand != null) cc.add(reorderCustomTypesCommand);

				if (!cc.isEmpty()) editorPart.getCommandStack().execute(cc);
			}
		});
		return null;
	}
}
