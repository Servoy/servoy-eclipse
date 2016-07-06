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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;

/**
 * @author lvostinar
 *
 */
public class CreateMediaWebAppManifest extends CreateMediaFileAction
{
	public CreateMediaWebAppManifest(SolutionExplorerView viewer)
	{
		super(viewer);
		setText("Create media Web App Manifest");
		setToolTipText(
			"Creates a manifest.json file which will be used for progressive web app (web applications that can be installed to the homescreen of a device without needing the user to go through an app store )");
	}

	@Override
	protected String getFileName(String pathString)
	{
		return "manifest.json";
	}

	@Override
	protected InputStream getContentInputStream()
	{
		StringBuffer initialContent = new StringBuffer();
		initialContent.append("{\n");
		initialContent.append("\t\"name\": \"Servoy Web Application Manifest Example\",\n");
		initialContent.append("\t\"short_name\": \"SvyApp\",\n");
		initialContent.append("\t\"display\": \"standalone\",\n");
		initialContent.append("\t\"orientation\": \"portrait\",\n");
		initialContent.append("\t\"icons\": [{\n");
		String url = "/resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + solution.getName() + "/bigicon.png";
		initialContent.append("\t\t\"src\": \"" + url + "\",\n");
		initialContent.append("\t\t\"sizes\": \"192x192\",\n");
		initialContent.append("\t\t\"type\": \"image/png\"\n");
		initialContent.append("\t}]\n");
		initialContent.append("}\n");
		return new ByteArrayInputStream(initialContent.toString().getBytes());
	}
}
