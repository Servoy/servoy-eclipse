/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.designer.webpackage.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.j2db.util.Debug;

/**
 * @author gganea
 *
 */
public class InstallWebPackageHandler implements IDeveloperService
{

	@Override
	public JSONObject executeMethod(JSONObject msg)
	{
		JSONObject pck = msg.getJSONObject("package");
		String selected = pck.optString("selected");
		if (selected == null) return null;
		String urlString = null;
		JSONArray jsonArray = pck.getJSONArray("releases");
		for (int i = 0; i < jsonArray.length(); i++)
		{
			JSONObject release = jsonArray.optJSONObject(i);
			if (release.optString("version", "").equals(selected))
			{
				urlString = release.optString("url");
				break;
			}
		}
		try
		{
			URL url = new URL(urlString);
			URLConnection conn = url.openConnection();
			InputStream in = conn.getInputStream();
			String packageName = pck.getString("name");
			IFolder componentsFolder = RemoveWebPackageHandler.checkPackagesFolderCreated(SolutionSerializer.NG_PACKAGES_DIR_NAME);

			importZipFileComponent(componentsFolder, in, packageName);
			in.close();
		}
		catch (IOException e)
		{
			Debug.log(e);
		}
		return null;
	}


	private void importZipFileComponent(IFolder componentsFolder, InputStream in, String name)
	{
		IFile eclipseFile = componentsFolder.getFile(name + ".zip");

		if (eclipseFile.exists())
		{
			try
			{
				eclipseFile.delete(true, new NullProgressMonitor());
			}
			catch (CoreException e)
			{
				e.printStackTrace();
			}
		}
		eclipseFile = componentsFolder.getFile(name + ".zip");
		try
		{
			eclipseFile.create(in, IResource.NONE, new NullProgressMonitor());
		}
		catch (CoreException e)
		{
			Debug.log(e);
		}
	}

	@Override
	public void dispose()
	{
	}

}
