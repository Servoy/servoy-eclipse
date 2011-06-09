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
package com.servoy.eclipse.model.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.dltk.core.IScriptProjectFilenames;

/**
 * Utility class for eclipse IResource operations.
 * 
 * @author acostescu
 */
public class ResourcesUtils
{

	public static String BUILDPATH_FILE = IScriptProjectFilenames.BUILDPATH_FILENAME;
	public static String STP_DIR = ".stp"; //$NON-NLS-1$

	/**
	 * Creates the given file, and it's parent containers (if they do not already exist).
	 * 
	 * @param file the file to be created.
	 * @param source the content used to fill up the file.
	 * @param force see {@link IFile#create(InputStream, boolean, org.eclipse.core.runtime.IProgressMonitor)}.
	 * @throws CoreException if the file or it's parent container cannot be created.
	 */
	public static void createFileAndParentContainers(final IFile file, final InputStream source, final boolean force) throws CoreException
	{
		createParentContainers(file.getParent(), force);
		file.create(source, force, null);
	}

	public static void createParentContainers(IContainer parent, boolean force) throws CoreException
	{
		if (parent == null || parent instanceof IProject || parent.exists())
		{
			return;
		}
		createParentContainers(parent.getParent(), force);
		if (parent instanceof IFolder)
		{
			((IFolder)parent).create(force, true, null);
		}
		else
		{
			parent.getFolder(new Path("")).create(force, true, null);
		}
	}

	public static <T> List<T> getExtensions(String extensionID)
	{
		List<T> ts = new ArrayList<T>();
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IExtensionPoint ep = reg.getExtensionPoint(extensionID);
		IExtension[] extensions = ep.getExtensions();

		for (IExtension extension : extensions)
		{
			IConfigurationElement[] ces = extension.getConfigurationElements();
			for (IConfigurationElement ce : ces)
			{
				try
				{
					T t = (T)ce.createExecutableExtension("class"); //$NON-NLS-1$
					if (t != null)
					{
						ts.add(t);
					}
				}
				catch (CoreException e)
				{
					ServoyLog.logError("Could not load extension (extension point " + extensionID + ", " + ce.getAttribute("class") + ")", e);
				}
				catch (ClassCastException e)
				{
					ServoyLog.logError("Extension class has wrong type (extension point " + extensionID + ", " + ce.getAttribute("class") + ")", e);
				}
			}
		}
		return ts;
	}
}
