/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.ngclient.startup.resourceprovider;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.servoy.eclipse.ngclient.startup.Activator;

/**
 * Used in war Exporter to copy resources.
 * @author emera
 */
public class ComponentResourcesExporter
{

	/**
	 * Copy the default component packages to war.
	 * @param path
	 * @throws IOException
	 * @throws Exception
	 */
	public static void copyDefaultComponentsAndServices(File tmpWarDir, Set<String> exportedPackages,
		Map<String, File> allTemplates) throws IOException
	{
		copy(Activator.getNClientBundle().getEntryPaths("/war/"), tmpWarDir, exportedPackages, allTemplates);
	}

	/**
	 * Used in war export to create a components.properties file which is needed to load the components specs in war.
	 * @return the locations of components folders relative to the war dir.
	 */
	public static String getDefaultComponentDirectoryNames(Set<String> exportedPackages)
	{
		StringBuilder locations = new StringBuilder();
		Enumeration<String> paths = Activator.getNClientBundle().getEntryPaths("/war/");
		while (paths.hasMoreElements())
		{
			String name = paths.nextElement().replace("war/", "");
			if (name.endsWith("/") && !name.equals("js/") && !name.equals("css/") && !name.equals("templates/") && !name.endsWith("services/"))
			{
				String packageName = name.substring(0, name.length() - 1);
				if (exportedPackages.contains(packageName))
				{
					locations.append("/" + name + ";");
				}
			}
		}
		return locations.toString();
	}

	/**
	 * Used in war export to create a services.properties file, which is needed to load services specs in the war.
	 * @return the locations of services folders relative to the war dir.
	 */
	public static String getDefaultServicesDirectoryNames(Set<String> exportedPackages)
	{
		StringBuilder locations = new StringBuilder();
		Enumeration<String> paths = Activator.getNClientBundle().getEntryPaths("/war/");
		while (paths.hasMoreElements())
		{
			String name = paths.nextElement().replace("war/", "");
			if (name.endsWith("services/"))
			{
				String packageName = name.substring(0, name.length() - 1);
				if (exportedPackages.contains(packageName))
				{
					locations.append("/" + name + ";");
				}
			}
		}
		return locations.toString();
	}

	/**
	 * @param path
	 * @param tmpWarDir
	 * @throws IOException
	 */
	private static void copy(Enumeration<String> paths, File destDir, Set<String> exportedPackages, Map<String, File> allTemplates)
		throws IOException
	{
		if (paths != null)
		{
			while (paths.hasMoreElements())
			{
				String path = paths.nextElement();
				if (path.endsWith("/"))
				{
					String packageName = path.substring("war/".length(), path.length() - 1);
					if (packageName.startsWith("templates") ||
						exportedPackages.contains(packageName.split("/")[0]))
					{
						File targetDir = new File(destDir, FilenameUtils.getName(path.substring(0, path.lastIndexOf("/"))));
						copy(Activator.getNClientBundle().getEntryPaths(path), targetDir, exportedPackages, allTemplates);
					}
				}
				else
				{
					URL entry = Activator.getNClientBundle().getEntry(path);
					File newFile = new File(destDir, FilenameUtils.getName(path));
					if (!path.startsWith("war/templates") || !newFile.exists())
					{
						FileUtils.copyInputStreamToFile(entry.openStream(), newFile);
					}
					if (newFile.getName().endsWith(".html"))
					{
						allTemplates.put(path.substring("war/".length()), newFile);
					}
				}
			}
		}
	}
}
