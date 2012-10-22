/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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

package com.servoy.eclipse.designer.editor.palette;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteRoot;

import com.servoy.eclipse.designer.editor.IPaletteFactory;
import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.ui.preferences.DesignerPreferences.PaletteCustomization;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.util.Utils;


/**
 * Utility class that can create a GEF Palette for the Visual Form Editor.
 * 
 * @author rgansevles
 * @since 6.0
 */
public abstract class BaseVisualFormEditorPaletteFactory implements IPaletteFactory
{
	protected void addFixedEntries(String id, String label, String[] entries, List<String> drawers, Map<String, List<String>> drawerEntries,
		Map<String, Object> entryProperties)
	{
		drawers.add(id);
		entryProperties.put(id + '.' + PaletteCustomization.PROPERTY_LABEL, label);
		drawerEntries.put(id, Arrays.asList(entries));
		for (String itemId : entries)
		{
			entryProperties.put(id + '.' + itemId + '.' + PaletteCustomization.PROPERTY_LABEL, Utils.stringInitCap(itemId));
			entryProperties.put(id + '.' + itemId + '.' + PaletteCustomization.PROPERTY_DESCRIPTION, "Create a " + itemId);
		}
	}

	protected void setProperty(Map<String, Object> map, TypedProperty< ? > property, Object value)
	{
		map.put(SetValueCommand.REQUEST_PROPERTY_PREFIX + property.getPropertyName(), value);
	}

	/**
	 * Creates the PaletteRoot and adds all palette elements. Use this factory
	 * method to create a new palette for your graphical editor.
	 * 
	 * @return a new PaletteRoot
	 */
	public PaletteRoot createPalette()
	{
		PaletteRoot palette = new PaletteRoot();
		fillPalette(palette);
		return palette;
	}

	protected abstract void fillPalette(PaletteRoot palette);

	protected void applyPaletteCustomization(Map<String, Object> map, String key, PaletteEntry entry, Map<String, Object> defaults)
	{
		entry.setVisible(!getValueWithDefaults(map, key + '.' + PaletteCustomization.PROPERTY_HIDDEN, defaults, Boolean.FALSE).booleanValue());
		entry.setLabel(getValueWithDefaults(map, key + '.' + PaletteCustomization.PROPERTY_LABEL, defaults, ""));
		entry.setDescription(getValueWithDefaults(map, key + '.' + PaletteCustomization.PROPERTY_DESCRIPTION, defaults, ""));

		if (entry instanceof PaletteDrawer)
		{
			((PaletteDrawer)entry).setInitialState(getValueWithDefaults(map, key + '.' + PaletteCustomization.PROPERTY_INITIAL_STATE, defaults,
				Integer.valueOf(PaletteDrawer.INITIAL_STATE_OPEN)).intValue());
		}
	}

	private static <T> T getValueWithDefaults(Map<String, Object> map, String key, Map<String, Object> defaults, T defval)
	{
		if (map != null && map.containsKey(key))
		{
			return (T)map.get(key);
		}
		if (defaults != null && defaults.containsKey(key))
		{
			return (T)defaults.get(key);
		}
		return defval;
	}

	public void refreshPalette(PaletteRoot palette)
	{
		palette.setChildren(new ArrayList<PaletteEntry>());
		fillPalette(palette);
	}

	public PaletteCustomization createPaletteCustomization(PaletteRoot palette)
	{
		// get the current and the default drawers and entries, save where they differ
		PaletteCustomization defaultPaletteCustomization = getDefaultPaletteCustomization();

		Map<String, List<String>> drawerEntries = new HashMap<String, List<String>>();
		Map<String, Object> entryProperties = new HashMap<String, Object>();
		List<String> drawerIds = new ArrayList<String>();

		for (PaletteEntry drawer : ((List<PaletteEntry>)palette.getChildren()))
		{
			if (drawer instanceof PaletteDrawer)
			{
				String drawerId = drawer.getId();
				drawerIds.add(drawerId);
				addPaletteEntryProperties(entryProperties, drawerId, drawer, defaultPaletteCustomization.entryProperties);

				List<String> itemIds = new ArrayList<String>();
				for (PaletteEntry entry : (List<PaletteEntry>)((PaletteDrawer)drawer).getChildren())
				{
					itemIds.add(entry.getId());
					addPaletteEntryProperties(entryProperties, drawerId + '.' + entry.getId(), entry, defaultPaletteCustomization.entryProperties);
				}
				List<String> defaultItemIds = defaultPaletteCustomization.drawerEntries.get(drawerId);
				if (!itemIds.equals(defaultItemIds))
				{
					drawerEntries.put(drawerId, itemIds);
				}
				// else do not save
			}
		}

		if (defaultPaletteCustomization.drawers.equals(drawerIds))
		{
			drawerIds = null; // do not save
		}

		return new PaletteCustomization(drawerIds, drawerEntries, entryProperties);
	}

	protected abstract PaletteCustomization getDefaultPaletteCustomization();

	/**
	 * @param entryProperties
	 * @param id
	 * @param entry
	 */
	private static void addPaletteEntryProperties(Map<String, Object> map, String key, PaletteEntry entry, Map<String, Object> defaults)
	{
		putIfNotDefault(map, key + '.' + PaletteCustomization.PROPERTY_HIDDEN, Boolean.valueOf(!entry.isVisible()), defaults, Boolean.FALSE);
		putIfNotDefault(map, key + '.' + PaletteCustomization.PROPERTY_LABEL, entry.getLabel(), defaults, null);
		putIfNotDefault(map, key + '.' + PaletteCustomization.PROPERTY_DESCRIPTION, entry.getDescription(), defaults, "");
		if (entry instanceof PaletteDrawer)
		{
			putIfNotDefault(map, key + '.' + PaletteCustomization.PROPERTY_INITIAL_STATE, Integer.valueOf(((PaletteDrawer)entry).getInitialState()), defaults,
				Integer.valueOf(PaletteDrawer.INITIAL_STATE_OPEN));
		}
	}

	private static void putIfNotDefault(Map<String, Object> map, String key, Object value, Map<String, Object> defaults, Object defval)
	{
		Object def = defaults.containsKey(key) ? defaults.get(key) : defval;
		if (def != null ? !def.equals(value) : value != null)
		{
			map.put(key, value);
		}
	}
}
