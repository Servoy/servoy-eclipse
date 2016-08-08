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
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.model.ServoyModelFinder;
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
		importPackage(pck, selected);
		return null;
	}


	public static void importPackage(JSONObject pck, String selectedVersion)
	{
		String urlString = null;
		JSONArray jsonArray = pck.getJSONArray("releases");
		String dependency = null;
		for (int i = 0; i < jsonArray.length(); i++)
		{
			JSONObject release = jsonArray.optJSONObject(i);
			if (selectedVersion == null || selectedVersion.equals(release.optString("version", "")))
			{
				urlString = release.optString("url");
				dependency = release.optString("dependency", null);
				break;
			}
		}
		try
		{
			URL url = new URL(urlString);
			URLConnection conn = url.openConnection();
			InputStream in = conn.getInputStream();
			String packageName = pck.getString("name");
			String solutionName = pck.optString("activeSolution", null);
			if (solutionName == null)
			{
				solutionName = ServoyModelFinder.getServoyModel().getFlattenedSolution().getName();
			}
			IFolder componentsFolder = RemoveWebPackageHandler.checkPackagesFolderCreated(solutionName, SolutionSerializer.NG_PACKAGES_DIR_NAME);

			importZipFileComponent(componentsFolder, in, packageName);
			in.close();

			if (dependency != null)
			{
				try
				{
					List<JSONObject> remotePackages = GetAllInstalledPackages.getRemotePackages();
					String[] packages = dependency.split(",");
					for (String dependendPck : packages)
					{
						String[] nameAndVersion = dependendPck.split("#");
						for (JSONObject pckObject : remotePackages)
						{
							if (pckObject.get("name").equals(nameAndVersion[0]))
							{
								JSONArray releases = pckObject.getJSONArray("releases");
								if (nameAndVersion.length > 1)
								{
									String version = "";
									String prefix = "=";
									if (nameAndVersion[1].startsWith(">="))
									{
										prefix = nameAndVersion[1].substring(0, 2);
										version = nameAndVersion[1].substring(2);
									}
									else if (nameAndVersion[1].startsWith(">"))
									{
										prefix = nameAndVersion[1].substring(0, 1);
										version = nameAndVersion[1].substring(1);
									}
									for (int j = 0; j < releases.length(); j++)
									{
										if (versionCheck(releases.getJSONObject(j).optString("version"), version, prefix))
										{
											importPackage(pckObject, releases.getJSONObject(j).optString("version"));
											break;
										}
									}
								}
								else
								{
									importPackage(pckObject, releases.getJSONObject(0).optString("version"));
								}
								break;
							}
						}

					}
				}
				catch (Exception e)
				{
					Debug.log(e);
				}
			}
		}
		catch (IOException e)
		{
			Debug.log(e);
		}
	}

	private static boolean versionCheck(String version1, String version2, String prefix)
	{
		if (version1 == null) return false;
		switch (prefix)
		{
			case "=" :
				return version1.equals(version2);
			case ">=" :
				return version1.compareTo(version2) >= 0;
			case ">" :
				return version1.compareTo(version2) > 0;
		}
		return false;
	}

	private static void importZipFileComponent(IFolder componentsFolder, InputStream in, String name)
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
