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
public class CreateMediaHeadIndexContributions extends CreateMediaFileAction
{
	public CreateMediaHeadIndexContributions(SolutionExplorerView viewer)
	{
		super(viewer);
		setText("Create media Head Index-Contribution");
		setToolTipText(
			"Creates a head-index-contributions.html file which should contain meta tags to be added to the index.html file of the application. Can be used to configure Web Application for IPad/IPhone.");
	}

	@Override
	protected String getFileName(String pathString)
	{
		return "head-index-contributions.html";
	}

	@Override
	protected InputStream getContentInputStream()
	{
		StringBuffer initialContent = new StringBuffer();
		initialContent.append(
			"<!--This is a sample contributions file for configuring web applications on iOS. For full documentation see: https://developer.apple.com/library/ios/documentation/AppleApplications/Reference/SafariWebContent/ConfiguringWebApplications/ConfiguringWebApplications.html\n ");
		initialContent.append(
			"Each meta tag in this file should occupy a row, then will be inserted in index.html of NG Application. Other content will be ignored.\n");
		initialContent.append(
			"This example assumes the existance of bigicon.png and startup.png as media. These icons will be used when choosing Add to Home Screen from Safari menu.-->\n");
		String baseURL = "/resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + solution.getName() + "/";
		initialContent.append("<link rel=\"apple-touch-icon\" href=\"" + baseURL + "bigicon.png\">\n");
		initialContent.append("<link rel=\"apple-touch-startup-image\" href=\"" + baseURL + "startup.png\">\n");
		initialContent.append("<meta name=\"apple-mobile-web-app-capable\" content=\"yes\">");
		return new ByteArrayInputStream(initialContent.toString().getBytes());
	}
}
