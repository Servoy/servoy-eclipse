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

package com.servoy.eclipse.model.preferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.model.Activator;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * @author vidmarian
 *
 */
public class Ng2DesignerPreferences
{
	public static final String DESIGNER_SETTINGS_PREFIX = "designer.";
	public static final String LAUNCH_NG2 = "launchNG2";
	public static final boolean LAUNCH_NG2_DEFAULT = true;
	public static final String NG2_DESIGNER = "ng2Designer";
	public static final boolean NG2_DESIGNER_DEFAULT = true;


	private final IEclipsePreferences eclipsePreferences;

	public Ng2DesignerPreferences()
	{
		eclipsePreferences = Activator.getDefault().getEclipsePreferences();
	}

	public boolean launchNG2()
	{
		return getProperty(LAUNCH_NG2, LAUNCH_NG2_DEFAULT);
	}

	public void setLaunchNG2(boolean launchNG2)
	{
		setProperty(LAUNCH_NG2, launchNG2);
	}

	public boolean showNG2Designer()
	{
		return getProperty(NG2_DESIGNER, NG2_DESIGNER_DEFAULT);
	}

	public void setShowNG2Designer(boolean showNG2)
	{
		setProperty(NG2_DESIGNER, showNG2);
	}


	protected Boolean getProperty(String key, boolean defaultValue)
	{
		return eclipsePreferences.getBoolean(key, defaultValue);
	}

	protected void setProperty(String key, boolean value)
	{
		eclipsePreferences.putBoolean(key, value);
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
