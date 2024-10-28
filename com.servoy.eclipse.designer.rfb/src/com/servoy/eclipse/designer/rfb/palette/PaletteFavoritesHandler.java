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

package com.servoy.eclipse.designer.rfb.palette;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;

public class PaletteFavoritesHandler
{
	private static PaletteFavoritesHandler instance;
	private final Map<String, Boolean> configMap;
	private final File configPath;
	private final JSONObject favoritesCategory;

	private static final String PACKAGE_NAME = "favorites";
	private static final String PACKAGE_DISPLAY_NAME = "Favorites Components";
	private static final String CONFIG_FILE_NAME = "fav.obj";
	private static final String PALETTE_RESOURCES_DIR = "palette_resources";

	private PaletteFavoritesHandler()
	{
		IPath stateLocation = Activator.getDefault().getStateLocation();
		// take a sub dir where we store the files
		IPath paletteResourcesPath = stateLocation.append(PALETTE_RESOURCES_DIR);
		// make sure it does exists.
		paletteResourcesPath.toFile().mkdirs();

		configPath = paletteResourcesPath.append(CONFIG_FILE_NAME).toFile();
		configMap = loadConfigMap(configPath);
		favoritesCategory = new JSONObject();
		favoritesCategory.put("packageName", PACKAGE_NAME);
		favoritesCategory.put("packageDisplayname", PACKAGE_DISPLAY_NAME);
	}

	public static PaletteFavoritesHandler getInstance()
	{
		if (instance == null)
		{
			instance = new PaletteFavoritesHandler();
		}
		return instance;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Boolean> loadConfigMap(File filePath)
	{
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath)))
		{
			return (Map<String, Boolean>)ois.readObject();
		}
		catch (IOException | ClassNotFoundException e)
		{
			return new HashMap<>();
		}
	}

	private void saveMapToFile(Map<String, Boolean> map, File filePath)
	{
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath)))
		{
			oos.writeObject(map);
		}
		catch (IOException e)
		{
			ServoyLog.logError("Can't save file: " + filePath, e);
		}
	}

	@SuppressWarnings("boxing")
	public void updateFavorite(String componentName)
	{
		ICoreRunnable runnable = (monitor) -> {
			Boolean bool = configMap.get(componentName);
			bool = (bool == null) ? true : !bool;
			if (!bool)
			{
				configMap.remove(componentName);
			}
			else
			{
				configMap.put(componentName, bool);
			}
			saveMapToFile(configMap, configPath);
		};
		Job job = Job.create("Saving the favorites components", runnable);
		job.setSystem(true);
		job.setRule(SchedulingRule.INSTANCE);
		job.schedule();
	}

	public synchronized JSONArray insertFavoritesCategory(JSONArray packages)
	{
		JSONArray componentsArray = new JSONArray();
		for (int i = 0; i < packages.length(); i++)
		{
			JSONObject packageJSON = packages.getJSONObject(i);
			if (!packageJSON.has("components") || "commons".equals(packageJSON.get("packageName"))) continue;
			JSONArray components = packageJSON.getJSONArray("components");
			addComponents(components, componentsArray);
			if (!packageJSON.has("categories")) continue;
			JSONObject categories = packageJSON.getJSONObject("categories");
			Iterator< ? > keys = categories.keys();
			while (keys.hasNext())
			{
				String key = keys.next().toString();
				JSONArray catComponents = categories.getJSONArray(key);
				addComponents(catComponents, componentsArray);
			}
		}
		JSONArray favoritesComponents = getFavoritesComponents(componentsArray);
		favoritesCategory.put("components", favoritesComponents);
		JSONArray result = new JSONArray();
		result.put(favoritesCategory);
		packages.forEach(result::put);
		return result;
	}

	private void addComponents(JSONArray components, JSONArray componentsArray)
	{
		for (int j = 0; j < components.length(); j++)
		{
			JSONObject component = components.getJSONObject(j);
			component.put("isFav", false);
			componentsArray.put(component);
		}
	}

	private JSONArray getFavoritesComponents(JSONArray components)
	{
		JSONArray componentsArray = new JSONArray();
		for (Map.Entry<String, Boolean> entry : configMap.entrySet())
		{
			for (int i = 0; i < components.length(); i++)
			{
				JSONObject component = components.getJSONObject(i);
				if (component.get("name").equals(entry.getKey()))
				{
					component.put("isFav", true);
					componentsArray.put(component);
				}
			}
		}
		return sortJSONArray(componentsArray);
	}

	private JSONArray sortJSONArray(JSONArray jsonArray)
	{
		List<JSONObject> list = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++)
		{
			list.add(jsonArray.getJSONObject(i));
		}
		list.sort(Comparator.comparing(a -> a.getString("displayName")));
		return new JSONArray(list);
	}

	private static class SchedulingRule implements ISchedulingRule
	{
		static SchedulingRule INSTANCE = new SchedulingRule();

		@Override
		public boolean contains(ISchedulingRule rule)
		{
			return rule == this;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule)
		{
			return rule == this;
		}
	}
}
