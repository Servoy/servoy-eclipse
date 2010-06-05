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
package com.servoy.eclipse.core.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.css.core.internal.contentproperties.CSSContentProperties;
import org.json.JSONException;

import com.servoy.eclipse.core.IFileAccess;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.StringResource;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Class for serializing/deserializing string resources.
 */
public class StringResourceDeserializer
{
	/*
	 * name of subdir where the styles are serialized
	 */
	public static final String STYLES_DIR_NAME = "styles";
	public static final String TEMPLATES_DIR_NAME = "templates";
	public static final String CSS1_PROFILE = "org.eclipse.wst.css.core.cssprofile.css1";

	/**
	 * Deserialize string resource metadatas.
	 * 
	 * @param eclipseRepository the repository
	 * @param wsd the workspace directory (directory that contains the resources project).
	 * @param resourcesProjectName the name of the resources project to use.
	 * @return a list of root object meta datas deserialized from the given project (an empty array if no resources are found).
	 */
	public static RootObjectMetaData[] deserializeMetadatas(IDeveloperRepository eclipseRepository, File wsd, String resourcesProjectName, int objectTypeId)
		throws RepositoryException
	{
		List<RootObjectMetaData> solutionMetadatas = new ArrayList<RootObjectMetaData>();

		if (wsd != null && resourcesProjectName != null)
		{
			String dirName;
			switch (objectTypeId)
			{
				case IRepository.STYLES :
					dirName = STYLES_DIR_NAME;
					break;
				case IRepository.TEMPLATES :
					dirName = TEMPLATES_DIR_NAME;
					break;
				default :
					throw new IllegalArgumentException("Unknown resource type: " + objectTypeId);

			}
			File[] files = new File(new File(wsd, resourcesProjectName), dirName).listFiles();
			if (files != null)
			{
				for (File resourceFile : files)
				{
					RootObjectMetaData md = deserializeMetadata(eclipseRepository, resourceFile, objectTypeId);
					if (md != null)
					{
						solutionMetadatas.add(md);
					}
				}
			}
		}

		return solutionMetadatas.toArray(new RootObjectMetaData[solutionMetadatas.size()]);
	}

	/**
	 * Deserialize metadata for one string resource specified by it's .obj file.
	 * 
	 * @param eclipseRepository the repository.
	 * @param resourceObjFile the .obj file for the string resource.
	 * @return the meta data for this string resource.
	 * @throws RepositoryException if the .obj file cannot be parsed or a repository exception occurs.
	 */
	public static RootObjectMetaData deserializeMetadata(IDeveloperRepository eclipseRepository, File objFile, int objectTypeId) throws RepositoryException
	{
		if (objFile != null && objFile.exists() && objFile.getName().endsWith(SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION))
		{
			int dot = objFile.getName().lastIndexOf('.');
			if (dot <= 0) return null;
			String name = objFile.getName().substring(0, dot);

			String metadataContent = Utils.getTXTFileContent(objFile);
			if (metadataContent != null)
			{
				try
				{
					ServoyJSONObject obj = new ServoyJSONObject(metadataContent, true);
					UUID rootObjectUuid = UUID.fromString(obj.getString(SolutionSerializer.PROP_UUID));
					if (objectTypeId == obj.getInt(SolutionSerializer.PROP_TYPEID))
					{
						int id = eclipseRepository.getElementIdForUUID(rootObjectUuid);
						RootObjectMetaData metadata = eclipseRepository.createRootObjectMetaData(id, rootObjectUuid, name, objectTypeId, 1, 1);
						return metadata;
					}
				}
				catch (JSONException e)
				{
					throw new RepositoryException(e);
				}
			}
		}
		return null;
	}

	public static String getStringResourceContentFilePath(String resourcesProjectName, String name, int objectTypeId)
	{
		switch (objectTypeId)
		{
			case IRepository.STYLES :
				return resourcesProjectName + IPath.SEPARATOR + STYLES_DIR_NAME + IPath.SEPARATOR + name + SolutionSerializer.STYLE_FILE_EXTENSION;
			case IRepository.TEMPLATES :
				return resourcesProjectName + IPath.SEPARATOR + TEMPLATES_DIR_NAME + IPath.SEPARATOR + name + SolutionSerializer.TEMPLATE_FILE_EXTENSION;

			default :
				throw new IllegalArgumentException("Unknown resource type: " + objectTypeId);
		}
	}

	public static String getStringResourceObjFilePath(String resourcesProjectName, String name, int objectTypeId)
	{
		switch (objectTypeId)
		{
			case IRepository.STYLES :
				return resourcesProjectName + IPath.SEPARATOR + STYLES_DIR_NAME + IPath.SEPARATOR + name + SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION;
			case IRepository.TEMPLATES :
				return resourcesProjectName + IPath.SEPARATOR + TEMPLATES_DIR_NAME + IPath.SEPARATOR + name + SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION;

			default :
				throw new IllegalArgumentException("Unknown resource type: " + objectTypeId);
		}
	}

	public static void deleteStringResource(StringResource resource, IFileAccess workspace, String resourcesProjectName) throws IOException
	{
		IOException exception = null;
		try
		{
			workspace.delete(getStringResourceContentFilePath(resourcesProjectName, resource.getName(), resource.getTypeID()));
		}
		catch (IOException e)
		{
			exception = e;
			ServoyLog.logError(e);
		}
		try
		{
			workspace.delete(getStringResourceObjFilePath(resourcesProjectName, resource.getName(), resource.getTypeID()));
		}
		catch (IOException e)
		{
			exception = e;
			ServoyLog.logError(e);
		}
		if (exception != null)
		{
			throw exception;
		}
	}

	public static StringResource readStringResource(IDeveloperRepository eclipseRepository, File wsd, String resourcesProjectName, RootObjectMetaData romd)
		throws RepositoryException
	{
		StringResource resource = (StringResource)eclipseRepository.createRootObject(romd);
		File contentFile = new File(wsd, getStringResourceContentFilePath(resourcesProjectName, resource.getName(), romd.getObjectTypeId()));
		if (contentFile.exists())
		{
			resource.loadFromFile(contentFile);
		}
		resource.setChangeHandler(new EclipseChangeHandler(eclipseRepository));
		return resource;
	}

	public static void writeStringResource(StringResource resource, IFileAccess fileAccess, String resourcesProjectName) throws RepositoryException
	{
		try
		{
			RootObjectMetaData metaData = resource.getRootObjectMetaData();
			ServoyJSONObject obj = new ServoyJSONObject();
			obj.put(SolutionSerializer.PROP_UUID, metaData.getRootObjectUuid().toString());
			obj.put(SolutionSerializer.PROP_TYPEID, new Integer(metaData.getObjectTypeId()));//just to be sure
			obj.put(SolutionSerializer.PROP_NAME, metaData.getName());

			String subdir;
			String extension;
			switch (metaData.getObjectTypeId())
			{
				case IRepository.STYLES :
					subdir = STYLES_DIR_NAME;
					extension = SolutionSerializer.STYLE_FILE_EXTENSION;
					break;

				case IRepository.TEMPLATES :
					subdir = TEMPLATES_DIR_NAME;
					extension = SolutionSerializer.TEMPLATE_FILE_EXTENSION;
					break;

				default :
					throw new IllegalArgumentException("Unsupported object type id: " + metaData.getObjectTypeId());

			}
			String path = resourcesProjectName + '/' + subdir + '/' + resource.getName();
			fileAccess.setUTF8Contents(path + SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION, obj.toString(true));
			fileAccess.setUTF8Contents(path + extension, resource.getContent() == null ? "" : resource.getContent());
		}
		catch (JSONException e)
		{
			throw new RepositoryException(e);
		}
		catch (IOException e)
		{
			throw new RepositoryException(e);
		}
	}

	public static void fixStyleCssProfile(String resourcesProjectName, Style style)
	{
		try
		{
			String relativePath = getStringResourceContentFilePath(resourcesProjectName, style.getName(), IRepository.STYLES);
			IFile resource = ServoyModel.getWorkspace().getRoot().getFile(new Path(relativePath));
			if (resource.exists() && CSSContentProperties.getProperty(CSSContentProperties.CSS_PROFILE, resource, false) == null) CSSContentProperties.setProperty(
				CSSContentProperties.CSS_PROFILE, resource, CSS1_PROFILE);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

}
