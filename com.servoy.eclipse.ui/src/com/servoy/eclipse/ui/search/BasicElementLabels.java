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

package com.servoy.eclipse.ui.search;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.TextProcessor;

/**
 * A label provider for basic elements like paths. The label provider will make sure that the labels are correctly
 * shown in RTL environments.
 *
 * @author jcompagner
 * @since 6.0
 */
public class BasicElementLabels
{

	private BasicElementLabels()
	{
	}

	/**
	 * Adds special marks so that that the given string is readable in a BIDI environment.
	 *
	 * @param string the string
	 * @param delimiters the additional delimiters
	 * @return the processed styled string
	 * @since 3.4
	 */
	private static String markLTR(String string, String delimiters)
	{
		return TextProcessor.process(string, delimiters);
	}

	/**
	 * Returns the label of a path.
	 *
	 * @param path the path
	 * @param isOSPath if <code>true</code>, the path represents an OS path, if <code>false</code> it is a workspace path.
	 * @return the label of the path to be used in the UI.
	 */
	public static String getPathLabel(IPath path, boolean isOSPath)
	{
		String label;
		if (isOSPath)
		{
			label = path.toOSString();
		}
		else
		{
			label = path.makeRelative().toString();
		}
		return markLTR(label, "/\\:.");
	}

	/**
	 * Returns the label for a file pattern like '*.java'
	 *
	 * @param name the pattern
	 * @return the label of the pattern.
	 */
	public static String getFilePattern(String name)
	{
		return markLTR(name, "*.?/\\:.");
	}

	/**
	 * Returns the label for a URL, URI or URL part. Example is 'http://www.x.xom/s.html#1'
	 *
	 * @param name the URL string
	 * @return the label of the URL.
	 */
	public static String getURLPart(String name)
	{
		return markLTR(name, ":@?-#/\\:.");
	}

	/**
	 * Returns a label for a resource name.
	 *
	 * @param resource the resource
	 * @return the label of the resource name.
	 */
	public static String getResourceName(IResource resource)
	{
		return markLTR(resource.getName(), ":.");
	}

	/**
	 * Returns a label for a resource name.
	 *
	 * @param resourceName the resource name
	 * @return the label of the resource name.
	 */
	public static String getResourceName(String resourceName)
	{
		return markLTR(resourceName, ":.");
	}

	/**
	 * Returns a label for a version name. Example is '1.4.1'
	 *
	 * @param name the version string
	 * @return the version label
	 */
	public static String getVersionName(String name)
	{
		return markLTR(name, ":.");
	}
}
