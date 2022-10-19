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
import com.servoy.eclipse.ui.property.PersistContext;
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
		WebComponent webComponent = (WebComponent)persistContext.getPersist();
		saveState(webComponent);

		WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebComponentSpecification(webComponent.getTypeName());
		PropertyDescription targetPD = spec.getProperty(propertyName);
		String typeName = PropertyUtils.getSimpleNameOfCustomJSONTypeProperty(targetPD.getType());

		for (int i = 0; i < result.size(); i++)
		{
			Map<String, Object> row = result.get(i);
			WebCustomType customType;
			Object uuid = row.get("svyUUID");
			if (uuid == null)
			{
				String name = typeName + "_" + id.incrementAndGet();
				while (!PersistFinder.INSTANCE.checkName(editorPart, name))
				{
					name = typeName + "_" + id.incrementAndGet();
				}
				customType = AddContainerCommand.addCustomType(webComponent, propertyName, name, i, null);
			}
			else
			{
				customType = (WebCustomType)webComponent.getChild(Utils.getAsUUID(uuid, false));
				previousItemsUUIDS.remove(uuid); //it was updated, remove it from the set
			}
			row.remove("svyUUID");
			row.forEach((key, value) -> customType.setProperty(key, value));
		}

		if (!previousItemsUUIDS.isEmpty())
		{
			//didn't get an update for some items, which means they are deleted
			for (Object uuid : previousItemsUUIDS)
			{
				WebCustomType customType = (WebCustomType)webComponent.getChild(Utils.getAsUUID(uuid, false));
				webComponent.removeChild(customType);
			}
		}
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, webComponent, true);
	}
}