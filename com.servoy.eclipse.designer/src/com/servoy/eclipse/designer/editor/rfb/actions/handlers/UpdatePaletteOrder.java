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

import java.util.Iterator;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.prefs.BackingStoreException;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.rfb.startup.DesignerFilter;
import com.servoy.j2db.util.Debug;

/**
 * @author gganea@servoy.com
 *
 */
public class UpdatePaletteOrder implements IServerService
{

	public Object executeMethod(String methodName, JSONObject args)
	{
		IEclipsePreferences preferenceStore = InstanceScope.INSTANCE.getNode("com.servoy.eclipse.designer.rfb");
		String json = preferenceStore.get(DesignerFilter.PREFERENCE_KEY, "{}");
		if (json.length() == 0)
		{
			json = "{}";
		}
		try
		{
			JSONObject jsonObject = new JSONObject(json);
			Iterator keys = args.keys();
			while (keys.hasNext())
			{
				String key = (String)keys.next();
				jsonObject.remove(key);//make sure its removed
				jsonObject.put(key, args.get(key));
			}
			preferenceStore.put(DesignerFilter.PREFERENCE_KEY, jsonObject.toString(4));
			preferenceStore.flush();
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
		catch (BackingStoreException e)
		{
			Debug.error(e);
		}
		return null;
	}

}
