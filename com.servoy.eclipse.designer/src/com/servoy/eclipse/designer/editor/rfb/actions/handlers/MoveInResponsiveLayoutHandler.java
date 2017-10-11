/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

import java.util.Iterator;

import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 *
 */
public class MoveInResponsiveLayoutHandler implements IServerService
{
	private final BaseVisualFormEditor editorPart;

	/**
	 * @param editorPart
	 */
	public MoveInResponsiveLayoutHandler(BaseVisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
	}

	@Override
	public Object executeMethod(String methodName, final JSONObject args) throws Exception
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
					if (persist instanceof AbstractBase)
					{
						JSONObject properties = args.optJSONObject(uuid);

						String dropTarget = properties.optString("dropTargetUUID", null);
						String rightSibling = properties.optString("rightSibling", null);

						ISupportFormElements parent = editorPart.getForm();
						IPersist searchForPersist = PersistFinder.INSTANCE.searchForPersist(editorPart, dropTarget);

						if (searchForPersist != null)
						{
							IPersist p = searchForPersist;
							while (!(p instanceof ISupportFormElements) && p != null)
							{
								p = p.getParent();
							}
							if (p instanceof ISupportFormElements)
							{
								parent = (ISupportFormElements)p;
							}
						}
						else
						{
							Debug.error("drop target with uuid: " + dropTarget + " not found in form: " + parent);
						}

						try
						{
							if (!persist.getParent().equals(parent) && (((ISupportExtendsID)persist).getExtendsID() > 0 ||
								!persist.equals(ElementUtil.getOverridePersist(PersistContext.create(persist, editorPart.getForm())))))
							{
								//do not allow changing the parent for inherited elements
								continue;
							}
						}
						catch (Exception e)
						{
							Debug.error(e);
						}
						IPersist rightSiblingPersist = PersistFinder.INSTANCE.searchForPersist(editorPart, rightSibling);
						cc.add(new ChangeParentCommand(persist, parent, rightSiblingPersist, editorPart.getForm(), false));
					}
				}
				if (!cc.isEmpty()) editorPart.getCommandStack().execute(cc);
			}
		});
		return null;
	}

}
