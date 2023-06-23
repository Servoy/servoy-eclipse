/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

package com.servoy.eclipse.ui.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IWebComponent;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class RemoveJSONPropertyQuickFix implements IMarkerResolution
{
	private final String uuid;
	private final String solutionName;
	private final String propertyName;

	public RemoveJSONPropertyQuickFix(String solutionName, String uuid, String propertyName)
	{
		this.solutionName = solutionName;
		this.uuid = uuid;
		this.propertyName = propertyName;
	}

	public String getLabel()
	{
		String[] parts = propertyName.split("\\.");
		return "Clear property " + parts[parts.length - 1];
	}

	public void run(IMarker marker)
	{
		if (solutionName != null && uuid != null && propertyName != null)
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			if (servoyProject != null)
			{
				IPersist persist = servoyProject.getEditingPersist(UUID.fromString(uuid));
				if (persist instanceof IWebComponent)
				{
					removeProperty((IWebComponent)persist);
					try
					{
						servoyProject.saveEditingSolutionNodes(new IPersist[] { persist }, true);
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}
	}

	private void removeProperty(IWebComponent webComponent)
	{
		JSONObject json = (JSONObject)webComponent.getProperty(
			StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
		if (json != null && propertyName != null)
		{
			JSONObject currentJSON = json;
			String[] parts = propertyName.split("\\."); //$NON-NLS-1$
			for (int i = 0; i < parts.length; i++)
			{
				if (currentJSON == null)
				{
					break;
				}
				if (i == parts.length - 1)
				{
					currentJSON.remove(parts[i]);
				}
				else
				{
					int index = -1;
					String key = parts[i];
					int indexPosition = key.indexOf("_arrindex");
					if (indexPosition > 0)
					{
						key = key.substring(0, indexPosition);
						index = Utils.getAsInteger(parts[i].substring(indexPosition + 9));
					}
					Object part = currentJSON.opt(key);
					if (part instanceof JSONObject)
					{
						currentJSON = (JSONObject)part;
					}
					else if (part instanceof JSONArray)
					{
						if (index >= 0 && index < ((JSONArray)part).length())
						{
							part = ((JSONArray)part).get(index);
							if (part instanceof JSONObject)
							{
								currentJSON = (JSONObject)part;
							}
							else
							{
								break;
							}
						}
						else
						{
							break;
						}
					}
					else
					{
						break;
					}
				}
			}
			webComponent.flagChanged();
			if (webComponent instanceof WebComponent)
			{
				((WebComponent)webComponent).setJson(json);
			}
		}
	}
}
