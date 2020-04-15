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

package com.servoy.eclipse.designer.webpackage;

import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;

import com.servoy.eclipse.designer.webpackage.endpoint.GetAllInstalledPackages;
import com.servoy.eclipse.designer.webpackage.endpoint.InstallWebPackageHandler;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.IAutomaticImportWPMPackages;

/**
 * @author lvostinar
 *
 */
public class AutomaticInstallWPMPackages implements IAutomaticImportWPMPackages
{

	@Override
	public void importDefaultResponsivePackages()
	{
		importPackageList(Arrays.asList(new String[] { "bootstrapcomponents", "12grid" }));
	}

	@Override
	public void importPackage(String packageName)
	{
		importPackageList(Arrays.asList(new String[] { packageName }));
	}

	private void importPackageList(List<String> packageNames)
	{
		try
		{
			List<JSONObject> packages = GetAllInstalledPackages.getRemotePackages();
			if (packages != null)
			{
				for (JSONObject pck : packages)
				{
					String name = pck.optString("name");
					if (packageNames.contains(name))
					{
						InstallWebPackageHandler.importPackage(pck, null);
					}
				}
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}
}
