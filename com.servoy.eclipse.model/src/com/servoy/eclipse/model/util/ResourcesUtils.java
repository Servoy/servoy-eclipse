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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.IScriptProjectFilenames;

import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.j2db.util.DataSourceUtils;

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

	public static String getParentDatasource(IFile file)
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
		else if (segments.length >= 3 && segments[segments.length - 3].equals(SolutionSerializer.DATASOURCES_DIR_NAME) &&
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
