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
package com.servoy.eclipse.model.repository;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;

public class RepositorySettingsDeserializer
{
	private static final String FILE_NAME = "repository_settings";

	public static void writeRepositoryUUID(IFileAccess fileAccess, String resourcesProjectName, UUID repositoryUUID) throws RepositoryException
	{
		try
		{
			ServoyJSONObject obj = new ServoyJSONObject();
			obj.put(SolutionSerializer.PROP_UUID, repositoryUUID.toString());
			String path = resourcesProjectName + '/' + FILE_NAME;
			fileAccess.setUTF8Contents(path + SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION, obj.toString(true));
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

	public static UUID readRepositoryUUID(IFileAccess fileAccess, String resourcesProjectName) throws RepositoryException
	{
		try
		{
			String path = resourcesProjectName + '/' + FILE_NAME + SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION;
			String content = fileAccess.getUTF8Contents(path);
			JSONObject json_obj = new ServoyJSONObject(content, true);
			String rep_uuid = (String)json_obj.get(SolutionSerializer.PROP_UUID);

			if (rep_uuid == null) throw new RepositoryException("Cannot read repository uuid from the resource project");

			return UUID.fromString(rep_uuid);
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

	public static boolean existsSettings(IFileAccess fileAccess, String resourcesProjectName)
	{
		String path = resourcesProjectName + '/' + FILE_NAME + SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION;
		return fileAccess.exists(path);
	}

	public static void deleteSettings(IFileAccess fileAccess, String resourcesProjectName) throws RepositoryException
	{
		String path = resourcesProjectName + '/' + FILE_NAME + SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION;
		if (fileAccess.exists(path))
		{
			try
			{
				fileAccess.delete(path);
			}
			catch (IOException ex)
			{
				throw new RepositoryException(ex);
			}
		}
	}
}
