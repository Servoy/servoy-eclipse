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

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
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

						Point point = getResponsiveLocationInParent(editorPart, parent, rightSibling, cc);
						if (point != null)
						{
							cc.add(new SetPropertyCommand("move", PersistPropertySource.createPersistPropertySource(persist, false),
								StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), point));

							cc.add(new ChangeParentCommand(persist, parent));
						}
					}
				}
				if (!cc.isEmpty()) editorPart.getCommandStack().execute(cc);
			}
		});
		return null;
	}

	public static Point getResponsiveLocationInParent(BaseVisualFormEditor editorPart, ISupportFormElements parent, String rightSiblingId, CompoundCommand cc)
	{
		List<IPersist> children = new ArrayList<IPersist>();
		Iterator<IPersist> it = parent.getAllObjects();
		while (it.hasNext())
		{
			IPersist persist = it.next();
			if (persist instanceof ISupportBounds)
			{
				children.add(persist);
			}
		}

		// default place it as the first element.
		int x = 1;
		int y = 1;
		if (children.size() > 0)
		{
			IPersist[] childArray = children.toArray(new IPersist[0]);
			Arrays.sort(childArray, PositionComparator.XY_PERSIST_COMPARATOR);
			if (rightSiblingId != null)
			{
				IPersist rightSibling = PersistFinder.INSTANCE.searchForPersist(editorPart, rightSiblingId);
				int counter = 1;

				for (IPersist element : childArray)
				{
					if (element == rightSibling)
					{
						x = counter;
						y = counter;
						counter++;
					}
					if (cc != null)
					{
						cc.add(new SetPropertyCommand("move", PersistPropertySource.createPersistPropertySource(element, false),
							StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), new Point(counter, counter)));
					}
					else
					{
						((ISupportBounds)element).setLocation(new Point(counter, counter));
					}
					counter++;
				}
			}
			else
			{
				// insert as last element in flow layout because no right/bottom sibling was given
				Point location = ((ISupportBounds)childArray[childArray.length - 1]).getLocation();
				x = location.x + 1;
				y = location.y + 1;
			}
		}

		return new Point(x, y);

	}
}
