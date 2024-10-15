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

package com.servoy.eclipse.designer.rfb.palette;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;

/**
 * @author vidmarian
 *
 */
public class PaletteCommonsHandler
{
	private static PaletteCommonsHandler instance;

	private TreeMap<Long, HashMap<String, Integer>> historyCfgMap;
	private TreeMap<Long, HashMap<String, Long>> historyTsMap;
	private HashMap<String, Integer> configMap;
	private HashMap<String, Long> tsMap;

	private final File historyCfgPath;
	private final File historyTsPath;
	private final File configPath;
	private final File tsPath;

	//todo: load from preferences
	private final int historySize = 14;
	private final int saveInterval = 24; //hours

	private final JSONObject commonsCategory;
	private final SortedSet<Map.Entry<String, Double>> commonsPercentage;

	private final DesignerPreferences prefs;

	private long historyTimestamp;


	public static PaletteCommonsHandler getInstance()
	{
		if (instance == null)
		{
			instance = new PaletteCommonsHandler();
		}
		return instance;
	}

	@SuppressWarnings("unchecked")
	private PaletteCommonsHandler()
	{
		IPath stateLocation = Activator.getDefault().getStateLocation();
		// take a sub dir where we store the files
		IPath paletteResoucesPath = stateLocation.append("palette_resources");
		// make sure it does exists.
		paletteResoucesPath.toFile().mkdirs();
		prefs = new DesignerPreferences();

		historyCfgPath = paletteResoucesPath.append("hstCfg.obj").toFile();
		historyTsPath = paletteResoucesPath.append("hstTs.obj").toFile();
		configPath = paletteResoucesPath.append("cfg.obj").toFile();
		tsPath = paletteResoucesPath.append("ts.obj").toFile();


		historyCfgMap = (TreeMap<Long, HashMap<String, Integer>>)getMapFromFile(historyCfgPath);
		historyTsMap = (TreeMap<Long, HashMap<String, Long>>)getMapFromFile(historyTsPath);
		configMap = (HashMap<String, Integer>)getMapFromFile(configPath);
		tsMap = (HashMap<String, Long>)getMapFromFile(tsPath);

		historyCfgMap = historyCfgMap != null ? historyCfgMap : new TreeMap<Long, HashMap<String, Integer>>();
		historyTsMap = historyTsMap != null ? historyTsMap : new TreeMap<Long, HashMap<String, Long>>();
		configMap = configMap != null ? configMap : new HashMap<String, Integer>();
		tsMap = tsMap != null ? tsMap : new HashMap<String, Long>();

		//init historyTimestamp
		historyTimestamp = historyCfgMap.size() > 0 ? historyCfgMap.descendingKeySet().first().longValue() : ZonedDateTime.now().toInstant().toEpochMilli();

		commonsCategory = new JSONObject();
		commonsCategory.put("packageName", "commons");
		commonsCategory.put("packageDisplayname", "Commonly Used");
		commonsPercentage = new TreeSet<>(new Comparator<Map.Entry<String, Double>>()
		{
			@Override
			public int compare(Map.Entry<String, Double> e1, Map.Entry<String, Double> e2)
			{
				int result = e1.getValue().compareTo(e2.getValue());
				if (result == 0)
				{
					//the key which came first in alphabetical order gets higher priority
					result = e1.getKey().compareTo(e2.getKey()) * -1;
				}
				return result;
			}
		});
	}

	private Map< ? , ? > getMapFromFile(File filePath)
	{
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath)))
		{
			return (Map< ? , ? >)ois.readObject();
		}
		catch (IOException | ClassNotFoundException e)
		{
			return null;
		}

	}

	private void saveMapToFile(Map< ? , ? > map, File filePath)
	{
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath)))
		{
			oos.writeObject(map);
		}
		catch (IOException e)
		{
			ServoyLog.logError("Cant save file " + filePath, e);
		}

	}

	public void updateComponentCounter(String componentName)
	{
		ICoreRunnable runnable = (monitor) -> {
			Integer counter = configMap.get(componentName);
			if (counter == null) counter = 0;
			counter++;
			configMap.put(componentName, counter);
			saveMapToFile(configMap, configPath);

			long utcTimestamp = ZonedDateTime.now().toInstant().toEpochMilli();
			tsMap.put(componentName, utcTimestamp);
			saveMapToFile(tsMap, tsPath);

			if (((utcTimestamp - historyTimestamp) > saveInterval * 60 * 60 * 1000))
			{
				if (historyTsMap.size() > this.historySize)
				{
					Long lastKey = historyTsMap.descendingMap().lastKey();//oldest record
					historyTsMap.remove(lastKey);
				}
				HashMap<String, Long> tsCopy = new HashMap<>();
				tsMap.forEach((k, v) -> tsCopy.put(k, v));
				historyTsMap.put(utcTimestamp, tsCopy);
				historyTimestamp = utcTimestamp;
				saveMapToFile(historyTsMap, historyTsPath);

				if (historyCfgMap.size() > this.historySize)
				{
					Long lastKey = historyCfgMap.descendingMap().lastKey();//oldest record
					historyCfgMap.remove(lastKey);
				}
				HashMap<String, Integer> cfgCopy = new HashMap<>();//clone config
				configMap.forEach((k, v) -> cfgCopy.put(k, v));
				historyCfgMap.put(utcTimestamp, cfgCopy);
				saveMapToFile(historyCfgMap, historyCfgPath);
			}
		};
		Job job = Job.create("Saving the commonly used palette data", runnable);
		job.setSystem(true);
		job.setRule(SchedulingRule.INSTANCE);
		job.schedule();
		//printMaps();
	}

	public synchronized JSONArray insertcommonsCategory(JSONArray packages)
	{
		long utcTimestamp = ZonedDateTime.now().toInstant().toEpochMilli();
		commonsPercentage.clear();
		for (int i = 0; i < packages.length(); i++)
		{
			JSONObject packageJSON = packages.getJSONObject(i);
			if (!packageJSON.keySet().contains("components")) continue;
			JSONArray components = packageJSON.getJSONArray("components");
			for (int j = 0; j < components.length(); j++)
			{
				JSONObject component = components.getJSONObject(j);
				double usagePercentage = getUsagePercentage(utcTimestamp, component.getString("name"));
				addComponentKeyToCommonsPercentageList(component.getString("name"), usagePercentage);
			}
			if (!packageJSON.has("categories")) continue;
			JSONObject categories = packageJSON.getJSONObject("categories");
			Iterator< ? > iterator = categories.keys();
			while (iterator.hasNext())
			{
				Object element = iterator.next();
				JSONArray catComponents = categories.getJSONArray(element.toString());
				for (int k = 0; k < catComponents.length(); k++)
				{
					JSONObject component = catComponents.getJSONObject(k);
					double usagePercentage = getUsagePercentage(utcTimestamp, component.getString("name"));
					addComponentKeyToCommonsPercentageList(component.getString("name"), usagePercentage);
				}
			}
		}
		JSONArray commonsComponents = getCommonsComponents(packages);
		commonsCategory.put("components", commonsComponents);
		JSONArray result = new JSONArray();
		result.put(commonsCategory);
		packages.forEach((value) -> {
			result.put(value);
		});

//		printMaps();

		return result;
	}

	private JSONArray getCommonsComponents(JSONArray packages)
	{
		List<String> keysList = new ArrayList<String>();
		commonsPercentage.forEach((value) -> {
			keysList.add(value.getKey());
		});
		JSONArray componentsArray = new JSONArray();
		for (int i = 0; i < packages.length(); i++)
		{
			JSONObject packageJSON = packages.getJSONObject(i);
			if (!packageJSON.keySet().contains("components")) continue;
			JSONArray components = packageJSON.getJSONArray("components");
			for (int j = 0; j < components.length(); j++)
			{
				JSONObject component = components.getJSONObject(j);
				if (keysList.contains(component.getString("name")))
				{
					componentsArray.put(component);
				}
			}
			if (!packageJSON.has("categories")) continue;
			JSONObject categories = packageJSON.getJSONObject("categories");
			Iterator< ? > iterator = categories.keys();
			while (iterator.hasNext())
			{
				Object element = iterator.next();
				JSONArray catComponents = categories.getJSONArray(element.toString());
				for (int k = 0; k < catComponents.length(); k++)
				{
					JSONObject component = catComponents.getJSONObject(k);
					if (keysList.contains(component.getString("name")))
					{
						componentsArray.put(component);
					}
				}
			}
		}
		return sortJSONArray(componentsArray);
	}

	private JSONArray sortJSONArray(JSONArray jsonArray)
	{
		List<JSONObject> list = new ArrayList<JSONObject>();
		for (int i = 0; i < jsonArray.length(); i++)
		{
			list.add(jsonArray.getJSONObject(i));
		}
		Collections.sort(list, new Comparator<JSONObject>()
		{
			@Override
			public int compare(JSONObject a, JSONObject b)
			{
				return a.getString("displayName").compareTo(b.getString("displayName"));
			}
		});
		return new JSONArray(list);
	}

	private double getUsagePercentage(long currentTimestamp, String key)
	{
		Integer counter = configMap.get(key);
		Long timestamp = tsMap.get(key);
		if (counter == null)
		{
			for (Entry<Long, HashMap<String, Integer>> entry : historyCfgMap.descendingMap().entrySet())
			{
				counter = entry.getValue().get(key);
				if (counter != null)
				{
					timestamp = historyTsMap.get(entry.getKey()).get(key);
					break;
				}
			}
		}
		counter = counter == null ? 0 : counter;
		timestamp = timestamp == null ? 0 : timestamp;

		//timestamp is in miliseconds, converting to hours
		double interval = (((currentTimestamp - timestamp) / 1000.0) / 60.0) / 60.0;

		return counter / interval;
	}

	private void addComponentKeyToCommonsPercentageList(String key, double usagePercentage)
	{
		if (commonsPercentage.size() < prefs.getCommonlyUsedSize())
		{
			boolean added = commonsPercentage.add(Map.entry(key, usagePercentage));
		}
		else
		{
			if (usagePercentage > commonsPercentage.first().getValue())
			{
				Map.Entry<String, Double> entry = commonsPercentage.first();
				commonsPercentage.removeIf(value -> value.getKey().equals(entry.getKey()));
				commonsPercentage.add(Map.entry(key, usagePercentage));
			}
		}
	}

	//use only for debug logics in java code
	private void printMaps()
	{
		System.out.println("########################");
		System.out.println("HISTORY");
		int hsIndex = 0;
		for (Entry<Long, HashMap<String, Integer>> entry : historyCfgMap.descendingMap().entrySet())
		{
			Long hashmapTS = entry.getKey();
			System.out.println("   HS[" + hsIndex++ + "]" + hashmapTS);
			for (Entry<String, Integer> entry1 : entry.getValue().entrySet())
			{
				System.out.println("        " + historyTsMap.get(entry.getKey()).get(entry1.getKey()) + " - " + entry1.getKey() + " - " + entry1.getValue());

			}
			System.out.println();
		}
		System.out.println("-----------------");
		System.out.println();
		System.out.println("LAST CONFIG");
		for (Entry<String, Integer> entry : configMap.entrySet())
		{
			System.out.println("        " + tsMap.get(entry.getKey()) + " - " + entry.getKey() + " - " + entry.getValue());
		}
		System.out.println("########################");
		System.out.println();
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
