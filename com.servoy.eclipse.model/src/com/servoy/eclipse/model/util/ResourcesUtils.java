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
import java.net.URI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.IScriptProjectFilenames;

import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Utils;

/**
 * Utility class for eclipse IResource operations.
 *
 * @author acostescu
 */
public class ResourcesUtils
{

	public static String BUILDPATH_FILE = IScriptProjectFilenames.BUILDPATH_FILENAME;
	public static String STP_DIR = ".stp";
	public static String NODE_DIR = ".node";

	/**
	 * Creates the given file, and it's parent containers (if they do not already exist).
	 *
	 * @param file the file to be created.
	 * @param source the content used to fill up the file.
	 * @param force see {@link IFile#create(InputStream, boolean, org.eclipse.core.runtime.IProgressMonitor)}.
	 * @throws CoreException if the file or it's parent container cannot be created.
	 */
	public static void createFileAndParentContainers(IFile file, InputStream source, boolean force) throws CoreException
	{
		createParentContainers(file.getParent(), force);
		file.create(source, force, null);
	}

	/**
	 * Creates or overwrites the given file and set the contents, and it's parent containers (if they do not already exist).
	 *
	 * @param file the file to be created.
	 * @param source the content used to fill up the file.
	 * @param force see {@link IFile#create(InputStream, boolean, org.eclipse.core.runtime.IProgressMonitor)}.
	 * @throws CoreException if the file or it's parent container cannot be created.
	 */
	public static void createOrWriteFile(IFile file, InputStream source, boolean force) throws CoreException
	{
		if (file.exists())
		{
			file.setContents(source, true, false, null);
		}
		else
		{
			createFileAndParentContainers(file, source, force);
		}
	}

	/**
	 * Creates or overwrites the given file and set the contents, and it's parent containers (if they do not already exist).
	 *
	 * @param file the file to be created.
	 * @param source the content used to fill up the file.
	 * @param force see {@link IFile#create(InputStream, boolean, org.eclipse.core.runtime.IProgressMonitor)}.
	 * @throws CoreException if the file or it's parent container cannot be created.
	 */
	public static void createOrWriteFileUTF8(IFile file, String contents, boolean force) throws CoreException
	{
		InputStream source = Utils.getUTF8EncodedStream(contents);
		if (file.exists())
		{
			file.setContents(source, force, false, null);
		}
		else
		{
			createFileAndParentContainers(file, source, force);
		}
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

	/**
	 * Reads the handles of all files that are mapped to the given URI and returns only the file in the nested-most-project (the one with the least path segments in it).
	 * <br>
	 * There are scenarios when more than one file will be returned by IWorkspaceRoot.findFilesForLocationURI(location),
	 * in this case only the location in the most specific project should be used (in case there are nested locations imported as projects in the workspace).
	 * <br>
	 * For example:
	 * <br>- You have Servoy project "testtwo" which is part of "servoy_test" repository that is imported as a project and you have "servoy_test" itself imported as a project;
	 * <br>- Then you create a new solution in which you add this "testtwo" project as a reference project;
	 * <br>- In this case all the components found in "testtwo" will have more files returned by the method "IWorkspaceRoot.findFilesForLocationURI(location)";
	 * <br>- IFile[] array returned :"[L/servoy_test/testtwo/button2/button2.spec, L/testtwo/button2/button2.spec]";
	 * <br>- In this scenario we would need "L/testtwo/button2/button2.spec]" (the file with the least path segments in it).
	 *
	 * @param location
	 * @return the file in the nested-most-project or null if no file was found for the given URI.
	 */
	public static IFile findFileWithShortestPathForLocationURI(URI location)
	{
		IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(location);
		IFile mostNestedFile = null;
		int min = -1;
		for (IFile file : files)
		{
			if (file.getFullPath().segments().length < min || min == -1)
			{
				min = file.getFullPath().segments().length;
				mostNestedFile = file;
			}
		}
		return mostNestedFile;
	}

	public static String getParentDatasource(IFile file, boolean tableEditing)
	{
		String serverName;
		String tableName;
		String[] segments = file.getProjectRelativePath().segments();
		// dbi files
		if (segments.length >= 2 && segments[segments.length - 1].endsWith(DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT))
		{
			serverName = segments[segments.length - 2];
			tableName = segments[segments.length - 1].substring(0,
				segments[segments.length - 1].length() - DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT.length());
		}
		// obj files: datasources: table nodes
		else if (segments.length >= 3 && segments[segments.length - 3].equals(SolutionSerializer.DATASOURCES_DIR_NAME) &&
			segments[segments.length - 1].endsWith(SolutionSerializer.TABLENODE_FILE_EXTENSION))
		{
			serverName = segments[segments.length - 2];
			tableName = segments[segments.length - 1].substring(0, segments[segments.length - 1].length() - SolutionSerializer.JSON_FILE_EXTENSION_SIZE);
		}
		// obj files: datasources: aggregates
		else if (segments.length >= 4 && segments[segments.length - 4].equals(SolutionSerializer.DATASOURCES_DIR_NAME) &&
			segments[segments.length - 1].endsWith(SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION))
		{
			serverName = segments[segments.length - 3];
			tableName = segments[segments.length - 2];
		}
		else if (!tableEditing && segments.length >= 3 && segments[segments.length - 3].equals(SolutionSerializer.DATASOURCES_DIR_NAME) &&
			(segments[segments.length - 1].endsWith(SolutionSerializer.CALCULATIONS_POSTFIX) ||
				segments[segments.length - 1].endsWith(SolutionSerializer.FOUNDSET_POSTFIX)))
		{
			serverName = segments[segments.length - 2];
			String postfix = segments[segments.length - 1].endsWith(SolutionSerializer.CALCULATIONS_POSTFIX) ? SolutionSerializer.CALCULATIONS_POSTFIX
				: SolutionSerializer.FOUNDSET_POSTFIX;
			tableName = segments[segments.length - 1].substring(0, segments[segments.length - 1].length() - postfix.length());

		}
		else
		{
			return null;
		}
		String dataSource = null;
		if (DataSourceUtils.INMEM_DATASOURCE.equals(serverName))
		{
			dataSource = DataSourceUtils.createInmemDataSource(tableName);
		}
		else if (DataSourceUtils.VIEW_DATASOURCE.equals(serverName))
		{
			dataSource = DataSourceUtils.createViewDataSource(tableName);
		}
		else
		{
			dataSource = DataSourceUtils.createDBTableDataSource(serverName, tableName);
		}
		return dataSource;
	}

}
