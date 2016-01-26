/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.BundleContext;

import com.servoy.j2db.IApplication;

/**
 * The activator class controls the plug-in life cycle
 * @author acostescu
 */
public class Activator extends Plugin
{

	private IApplication mobileExportClient;

	private DesignApplication designClient;

	// The plug-in ID
	public static final String PLUGIN_ID = "com.servoy.eclipse.model";

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator()
	{
	}

	@Override
	public void start(BundleContext context) throws Exception
	{
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception
	{
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault()
	{
		return plugin;
	}

	/**
	 * Project level preferences
	 */
	public IEclipsePreferences getEclipsePreferences(IProject project)
	{
		return new ProjectScope(project).getNode(PLUGIN_ID);
	}

	public DesignApplication getDesignClient()
	{
		if (designClient == null)
		{
			designClient = new DesignApplication();
		}
		return designClient;
	}

	public IApplication getMobileExportClient()
	{
		if (mobileExportClient == null)
		{
			mobileExportClient = new DesignApplication()
			{
				@Override
				public String getI18NMessage(String i18nKey)
				{
					return i18nKey;
				}

				@Override
				public String getI18NMessage(String i18nKey, Object[] array)
				{
					return i18nKey;
				}

				@Override
				public String getI18NMessageIfPrefixed(String i18nKey)
				{
					return i18nKey;
				}
			};
		}
		return mobileExportClient;
	}

}
