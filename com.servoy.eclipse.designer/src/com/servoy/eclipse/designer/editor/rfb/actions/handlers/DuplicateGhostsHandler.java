/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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
import java.util.List;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.AddContainerCommand;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.IBasicWebComponent;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistIdentifier;

public class DuplicateGhostsHandler implements IServerService
{
	private final BaseVisualFormEditor editorPart;
	private final ISelectionProvider selectionProvider;

	public DuplicateGhostsHandler(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
	}

	@Override
	public Object executeMethod(String methodName, final JSONObject args)
	{
		Display.getDefault().asyncExec(() -> {
			editorPart.getCommandStack().execute(new BaseRestorableCommand("duplicateGhosts")
			{
				private final List<IPersist> createdPersists = new ArrayList<>();

				@Override
				public void execute()
				{
					createdPersists.clear();
					try
					{
						JSONArray uuids = args.getJSONArray("uuids");
						String parentUuid = args.getString("parentUuid");
						int dropIndex = args.getInt("dropIndex");

						IPersist parentPersist = findPersistByUUID(parentUuid);
						if (parentPersist == null)
						{
							Debug.error("DuplicateGhostsHandler: parent persist not found for UUID " + parentUuid);
							return;
						}

						int insertOffset = 0;
						for (int i = 0; i < uuids.length(); i++)
						{
							String ghostUuidStr = uuids.getString(i);
							IPersist ghostPersist = findGhostPersist(ghostUuidStr);
							if (ghostPersist == null)
							{
								Debug.error("DuplicateGhostsHandler: ghost persist not found for UUID " + ghostUuidStr);
								continue;
							}

							if (!isChildOf(ghostPersist, parentPersist))
							{
								Debug.error("DuplicateGhostsHandler: ghost does not belong to specified parent");
								continue;
							}

							if (isInherited(ghostPersist))
							{
								Debug.error("DuplicateGhostsHandler: cannot duplicate inherited ghost");
								continue;
							}

							IPersist cloned = duplicateGhost(ghostPersist, parentPersist, dropIndex + insertOffset);
							if (cloned != null)
							{
								createdPersists.add(cloned);
								insertOffset++;
							}
						}

						if (!createdPersists.isEmpty())
						{
							List<IPersist> changed = new ArrayList<>(createdPersists);
							changed.add(parentPersist);
							ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, changed);

							selectionProvider.setSelection(new StructuredSelection(
								createdPersists.stream()
									.map(p -> PersistContext.create(p, editorPart.getForm()))
									.toArray()));
						}
					}
					catch (Exception ex)
					{
						Debug.error(ex);
					}
				}

				@Override
				public void undo()
				{
					try
					{
						for (IPersist persist : createdPersists)
						{
							((IDeveloperRepository)persist.getRootObject().getRepository()).deleteObject(persist);
						}
						if (!createdPersists.isEmpty())
						{
							ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, createdPersists);
						}
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError("Could not undo duplicate ghosts", e);
					}
				}
			});
		});
		return null;
	}

	private IPersist findPersistByUUID(String uuidStr)
	{
		return (IPersist)ModelUtils.getEditingFlattenedSolution(editorPart.getForm())
			.getFlattenedForm(editorPart.getForm())
			.acceptVisitor(o -> {
				if (o.getUUID() != null && o.getUUID().toString().equals(uuidStr))
				{
					return o;
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			});
	}

	private IPersist findGhostPersist(String ghostUuidStr)
	{
		try
		{
			PersistIdentifier pid = PersistIdentifier.fromJSONString(ghostUuidStr);
			return com.servoy.eclipse.model.util.PersistFinder.INSTANCE.searchForPersist(editorPart.getForm(), pid);
		}
		catch (Exception e)
		{
			return findPersistByUUID(ghostUuidStr);
		}
	}

	private boolean isChildOf(IPersist child, IPersist parent)
	{
		IPersist p = child.getParent();
		while (p != null)
		{
			if (p.getUUID() != null && parent.getUUID() != null && p.getUUID().equals(parent.getUUID()))
			{
				return true;
			}
			p = p.getParent();
		}
		return false;
	}

	private boolean isInherited(IPersist persist)
	{
		if (persist instanceof WebCustomType wct)
		{
			return wct.getExtendsID() != null;
		}
		if (persist instanceof Tab)
		{
			IPersist parent = persist.getParent();
			if (parent instanceof TabPanel)
			{
				return com.servoy.j2db.util.Utils.isInheritedFormElement((TabPanel)parent, editorPart.getForm()) &&
					editorPart.getForm().getChild(parent.getUUID()) == null;
			}
		}
		return false;
	}

	private IPersist duplicateGhost(IPersist ghost, IPersist parent, int targetIndex)
	{
		try
		{
			if (ghost instanceof WebCustomType originalCustomType)
			{
				return duplicateCustomType(originalCustomType, parent, targetIndex);
			}
			else if (ghost instanceof Tab originalTab && parent instanceof TabPanel tabPanel)
			{
				return duplicateTab(originalTab, tabPanel);
			}
		}
		catch (Exception e)
		{
			Debug.error("DuplicateGhostsHandler: error duplicating ghost", e);
		}
		return null;
	}

	private WebCustomType duplicateCustomType(WebCustomType original, IPersist parent, int targetIndex)
	{
		IBasicWebComponent parentComponent;
		if (parent instanceof IBasicWebComponent)
		{
			parentComponent = (IBasicWebComponent)parent;
		}
		else
		{
			parentComponent = (IBasicWebComponent)original.getParent();
		}

		String propertyName = original.getJsonKey();
		String copyName = original.getName() != null ? original.getName() + "_copy" : null;
		WebCustomType clone = AddContainerCommand.addCustomType(parentComponent, propertyName, copyName, targetIndex, original);
		return clone;
	}

	private Tab duplicateTab(Tab original, TabPanel tabPanel) throws RepositoryException
	{
		ISupportChilds overrideParent = (ISupportChilds)com.servoy.eclipse.ui.util.ElementUtil.getOverridePersist(
			PersistContext.create(tabPanel, editorPart.getForm()));
		if (overrideParent == null)
		{
			overrideParent = tabPanel;
		}


		Tab clone = (Tab)original.cloneObj(overrideParent, true, null, false, false, true);
		if (original.getText() != null)
		{
			clone.setText(original.getText() + " copy");
		}
		return clone;
	}
}
