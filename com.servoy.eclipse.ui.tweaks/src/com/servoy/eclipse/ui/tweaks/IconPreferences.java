/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.eclipse.ui.tweaks;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

/**
 * @author emera
 */
public class IconPreferences
{

	private static IconPreferences instance;
	protected final IEclipsePreferences eclipsePreferences;

	private static final String USE_DARK_THEME_ICONS_SETTING = "useDarkThemeIcons";
	public static final boolean USE_DARK_THEME_ICONS_DEFAULT = false;

	public IconPreferences()
	{
		eclipsePreferences = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
	}


	public static IconPreferences getInstance()
	{
		if (instance == null)
		{
			instance = new IconPreferences();
		}
		return instance;
	}


	public void setUseDarkThemeIcons(boolean value)
	{
		eclipsePreferences.putBoolean(USE_DARK_THEME_ICONS_SETTING, value);
	}

	public void save()
	{
		try
		{
			eclipsePreferences.flush();
		}
		catch (BackingStoreException e)
		{
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
		}
	}

	public boolean getUseDarkThemeIcons()
	{
		return eclipsePreferences.getBoolean(USE_DARK_THEME_ICONS_SETTING, USE_DARK_THEME_ICONS_DEFAULT);
	}
}
