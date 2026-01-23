/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.websocket.utils.PropertyUtils;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.AddContainerCommand;
import com.servoy.eclipse.model.util.PersistFinder;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.util.Utils;

/**
 * @author emera
 */
public class SetCustomArrayPropertiesCommand extends BaseRestorableCommand
{
	private final Set<Object> previousItemsUUIDS;
	private final String propertyName;
	private final PersistContext persistContext;
	private final List<Map<String, Object>> result;
	private final BaseVisualFormEditor editorPart;
	private final AtomicInteger id = new AtomicInteger();

	public SetCustomArrayPropertiesCommand(String propertyName,
		PersistContext persistContext, List<Map<String, Object>> result, Set<Object> previousItemsUUIDS, BaseVisualFormEditor editorPart)
	{
		super("setCustomArrayProperties");
		this.previousItemsUUIDS = previousItemsUUIDS;
		this.propertyName = propertyName;
		this.persistContext = persistContext;
		this.result = result;
		this.editorPart = editorPart;
	}

	@Override
	public void execute()
	{
		WebComponent parentWebComponent = (WebComponent)persistContext.getPersist();
		saveState(parentWebComponent);

		WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(parentWebComponent.getTypeName());
		PropertyDescription targetPD = spec.getProperty(propertyName);
		String typeName = PropertyUtils.getSimpleNameOfCustomJSONTypeProperty(targetPD.getType());
		WebComponent webComponent = parentWebComponent;
		try
		{
			webComponent = (WebComponent)ElementUtil.getOverridePersist(persistContext);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		// do not modify original state, needed for redo
		Set<Object> previousItemsUUIDSCopy = previousItemsUUIDS != null ? new HashSet<>(previousItemsUUIDS) : new HashSet<>();

		for (int i = 0; i < result.size(); i++)
		{
			Map<String, Object> row = result.get(i);
			WebCustomType[] customType = new WebCustomType[1];
			Object uuid = row.get("svyUUID");
			if (uuid == null)
			{
				String name = typeName + "_" + id.incrementAndGet();
				while (!PersistFinder.INSTANCE.checkName(editorPart.getForm(), name))
				{
					name = typeName + "_" + id.incrementAndGet();
				}
				customType[0] = AddContainerCommand.addCustomType(webComponent, propertyName, name, i, null);
			}
			else
			{
				customType[0] = (WebCustomType)webComponent.getChild(Utils.getAsUUID(uuid, false));
				if (customType[0] == null && webComponent != parentWebComponent)
				{
					customType[0] = (WebCustomType)parentWebComponent.getChild(Utils.getAsUUID(uuid, false));
					if (customType[0] != null)
					{
						Object webComponents = webComponent.getProperty(customType[0].getJsonKey());
						if (webComponents instanceof Object[])
						{
							// inheritance by index for custom types
							customType[0] = (WebCustomType)((Object[])webComponents)[customType[0].getIndex()];
						}
					}
				}
				previousItemsUUIDSCopy.remove(uuid); //it was updated, remove it from the set
			}
			if (customType[0] != null)
			{
				row.forEach((key, value) -> {
					if (!"svyUUID".equals(key)) customType[0].setProperty(key, value);
				});
			}
			else
			{
				ServoyLog.logWarning("Cannot find a custom type for uuid: " + uuid + ", in component:" + webComponent, new RuntimeException());
			}
		}

		if (!previousItemsUUIDSCopy.isEmpty())
		{
			//didn't get an update for some items, which means they are deleted
			for (Object uuid : previousItemsUUIDSCopy)
			{
				WebCustomType customType = (WebCustomType)webComponent.getChild(Utils.getAsUUID(uuid, false));
				webComponent.removeChild(customType);
			}
		}
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, webComponent, true);
	}
}