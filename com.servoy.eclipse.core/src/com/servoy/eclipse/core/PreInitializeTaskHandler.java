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

package com.servoy.eclipse.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * This class is in charge of running the pre-initialized tasks registered through extensions.
 * @author acostescu
 */
public class PreInitializeTaskHandler
{

	public static final String PRE_INITIALIZE_EXTENSION_ID = Activator.PLUGIN_ID + ".preInitializeJob"; //$NON-NLS-1$

	private static boolean tasksRan = false;
	private static boolean preInitializeRunning = false;

	public static void runTasksIfNeeded()
	{
		if (!tasksRan)
		{
			synchronized (PreInitializeTaskHandler.class)
			{
				if (!tasksRan)
				{
					try
					{
						// notify pre initialize extensions and avoid initialization cycles
						if (!preInitializeRunning)
						{
							preInitializeRunning = true;
							notifyPreInitializeExtensions();
						}
						else
						{
							throw new RuntimeException("Detected pre initialize cycle..."); //$NON-NLS-1$
						}

					}
					finally
					{
						tasksRan = true;
					}
				}
			}
		}
	}

	private static void notifyPreInitializeExtensions()
	{
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IExtensionPoint ep = reg.getExtensionPoint(PRE_INITIALIZE_EXTENSION_ID);
		IExtension[] extensions = ep.getExtensions();

		if (extensions != null && extensions.length > 0)
		{
			for (IExtension extension : extensions)
			{
				IConfigurationElement[] ce = extension.getConfigurationElements();
				if (ce != null && ce.length > 0)
				{
					try
					{
						Runnable job = (Runnable)ce[0].createExecutableExtension("class"); //$NON-NLS-1$
						if (job != null)
						{
							try
							{
								job.run();
							}
							catch (Throwable e)
							{
								ServoyLog.logError(e);
							}
						}
					}
					catch (CoreException e)
					{
						ServoyLog.logError("Failed to run a pre-initialize job.", e); //$NON-NLS-1$
					}
				}
			}
		}
	}

}
