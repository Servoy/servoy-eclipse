/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import org.eclipse.core.resources.IProject;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.servoy.eclipse.model.Activator;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Base class for prefs stored at project level.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public abstract class ProjectPreferences
{
	private final Preferences eclipsePreferences;

	public ProjectPreferences(IProject project, String path)
	{
		eclipsePreferences = Activator.getDefault().getEclipsePreferences(project).node(path);
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
