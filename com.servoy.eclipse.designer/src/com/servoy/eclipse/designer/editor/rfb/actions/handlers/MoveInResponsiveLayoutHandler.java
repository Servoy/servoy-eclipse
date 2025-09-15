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

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IFlattenedPersistWrapper;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.server.ngclient.template.PersistIdentifier;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;

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
					String persistIdentifierString = (String)keys.next();
					PersistIdentifier persistIdentifier = PersistIdentifier.fromJSONString(persistIdentifierString);
					final IPersist persist = PersistFinder.INSTANCE.searchForPersist(editorPart.getForm(), persistIdentifier);
					if (persist instanceof AbstractBase)
					{
						JSONObject properties = args.optJSONObject(persistIdentifierString);

						PersistIdentifier dropTarget = PersistIdentifier.fromJSONString(properties.optString("dropTargetUUID", null));
						PersistIdentifier rightSibling = PersistIdentifier.fromJSONString(properties.optString("rightSibling", null));

						ISupportFormElements parent = editorPart.getForm();
						IPersist searchForPersist = PersistFinder.INSTANCE.searchForPersist(editorPart.getForm(), dropTarget);

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
						IPersist rightSiblingPersist = PersistFinder.INSTANCE.searchForPersist(editorPart.getForm(), rightSibling);
						ISupportChilds initialParent = persist instanceof ISupportExtendsID ? ((ISupportExtendsID)persist).getRealParent()
							: persist.getParent();
						if (initialParent == parent)
						{
							FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(initialParent);
							ISupportChilds flattenedParent = PersistHelper.getFlattenedPersist(flattenedSolution, editorPart.getForm(), initialParent);
							Class< ? > childPositionClass = persist instanceof ISupportBounds ? ISupportBounds.class
								: (persist instanceof IChildWebObject ? IChildWebObject.class : null);
							if (childPositionClass != null)
							{
								ArrayList<IPersist> children = new ArrayList<IPersist>();
								Iterator<IPersist> it = flattenedParent.getAllObjects();
								while (it.hasNext())
								{
									IPersist child = it.next();
									if (childPositionClass.isInstance(persist))
									{
										children.add(
											child instanceof IFlattenedPersistWrapper ? ((IFlattenedPersistWrapper< ? >)child).getWrappedPersist() : child);
									}
								}
								if (children.size() == 1)
								{
									continue;
								}
							}
						}
						cc.add(new ChangeParentCommand(persist, parent, rightSiblingPersist, editorPart.getForm(), false));
					}
				}
				if (!cc.isEmpty()) editorPart.getCommandStack().execute(cc);
			}
		});
		return null;
	}

}
