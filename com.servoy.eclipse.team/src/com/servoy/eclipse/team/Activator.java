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
package com.servoy.eclipse.team;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

import com.servoy.eclipse.model.util.ModelUtils;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin
{
	// The plug-in ID
	public static final String PLUGIN_ID = "com.servoy.eclipse.team";

	// The shared instance
	private static Activator plugin;

	// if you change this please CHANGE TeamShareMonitor.SERVOY_TEAM_PROVIDER_ID to the same value
	public static final String NATURE_ID = "com.servoy.eclipse.team.servoynature";

	/**
	 * The constructor
	 */
	public Activator()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception
	{
		ModelUtils.assertUINotDisabled(PLUGIN_ID);

		super.start(context);
		plugin = this;

		ServoyTeamProvider.startup();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception
	{
		ServoyTeamProvider.cleanup();

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
	 * Answers the repository provider type id for the servoy plugin
	 */
	public static String getTypeId()
	{
		return NATURE_ID;
	}
}
