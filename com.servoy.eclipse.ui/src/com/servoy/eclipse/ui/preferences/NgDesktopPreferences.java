/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.eclipse.ui.preferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.model.Activator;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * @author vidmarian
 *
 */
public class NgDesktopPreferences
{

	public static final String NGDESKTOP_VERSION = "ngdesktopVersion";
	public static final String NGDESKTOP_VERSION_DEFAULT = "2020.12";

	private final IEclipsePreferences eclipsePreferences;

	public NgDesktopPreferences()
	{
		eclipsePreferences = Activator.getDefault().getEclipsePreferences();
	}

	public String getNgDesktopVersionKey()
	{
		return getProperty(NGDESKTOP_VERSION, NGDESKTOP_VERSION_DEFAULT);
	}

	public void setNgDesktopVersion(String value)
	{
		setProperty(NGDESKTOP_VERSION, value);
	}


	protected String getProperty(String key, String defaultValue)
	{
		return eclipsePreferences.get(key, defaultValue);
	}

	protected void setProperty(String key, String value)
	{
		if (value == null || value.length() == 0)
		{
			removeProperty(key);
		}
		else
		{
			eclipsePreferences.put(key, value);
		}
	}

	protected void removeProperty(String key)
	{
		eclipsePreferences.remove(key);
	}

	public void save()
	{
		try
		{
			eclipsePreferences.flush();
		}
		catch (BackingStoreException e)
		{
			ServoyLog.logError(e);
		}
	}


}
