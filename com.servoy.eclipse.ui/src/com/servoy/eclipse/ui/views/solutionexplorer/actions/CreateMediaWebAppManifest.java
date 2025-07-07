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
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class CreateMediaWebAppManifest extends CreateMediaFileAction
{
	public static final String FILE_NAME = "manifest.json";
	public static final String ICON_NAME = "webapp144.png";

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
		return FILE_NAME;
	}

	@Override
	public void run()
	{
		Solution thisSolution = getSolution();
		if (thisSolution != null)
		{
			WorkspaceFileAccess wsa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
			try
			{
				wsa.createFolder(thisSolution.getName() + "/" + SolutionSerializer.MEDIAS_DIR);
				IPath path = new Path(thisSolution.getName() + "/" + SolutionSerializer.MEDIAS_DIR + "/" + ICON_NAME);
				IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				if (file != null && !file.exists())
				{
					InputStream inputStream = new ByteArrayInputStream(getIcon());
					file.create(inputStream, false, null);
					inputStream.close();
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}
		}
		super.run();
	}

	@Override
	protected InputStream getContentInputStream()
	{
		return new ByteArrayInputStream(createManifest(solution.getName()));
	}

	public static byte[] createManifest(String solutionName)
	{
		StringBuffer initialContent = new StringBuffer();
		initialContent.append("{\n");
		initialContent.append("\t\"name\": \"Servoy Web Application\",\n");
		initialContent.append("\t\"short_name\": \"" + solutionName + "\",\n");
		initialContent.append("\t\"display\": \"standalone\",\n");
		initialContent.append("\t\"orientation\": \"portrait\",\n");
		initialContent.append("\t\"icons\": [{\n");
		initialContent.append("\t\t\"src\": \"" + ICON_NAME + "\",\n");
		initialContent.append("\t\t\"sizes\": \"144x144\",\n");
		initialContent.append("\t\t\"type\": \"image/png\"\n");
		initialContent.append("\t}],\n");
		initialContent.append("\t\"start_url\": \"../../../\"\n");
		initialContent.append("}\n");
		return initialContent.toString().getBytes();
	}

	public static byte[] getIcon() throws IOException
	{
		return Utils.getBytesFromInputStream(Activator.getDefault().getBundle().getResource("/icons/" + ICON_NAME).openStream());
	}
}
