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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ngpackages.NGPackageManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 *
 */
public class ImportComponentAsProjectAction extends ImportComponentAction
{
	public ImportComponentAsProjectAction(SolutionExplorerView viewer, String entity)
	{
		super(viewer, entity);
	}

	/**
	 * @param fileNames
	 * @param filterPath
	 */
	@Override
	protected void doImport(String[] fileNames, String filterPath)
	{
		for (String zipFile : fileNames)
		{
			int extStartIdx = zipFile.indexOf('.');
			String projectName = extStartIdx > 0 ? zipFile.substring(0, extStartIdx) : zipFile;

			ZipInputStream zis = null;
			try
			{
				if (ServoyModel.getWorkspace().getRoot().getProject(projectName).exists())
				{
					UIUtils.reportError("Import component as project", "Project with name : '" + projectName + "' already exist in the current workspace");
					continue;
				}
				IProject newProject = NGPackageManager.createProject(projectName);

				zis = new ZipInputStream(new FileInputStream(filterPath + File.separator + zipFile));
				ZipEntry ze = zis.getNextEntry();
				while (ze != null)
				{
					String fileName = ze.getName();
					if (ze.isDirectory())
					{
						WorkspaceFileAccess.mkdirs(newProject.getFolder(fileName));
					}
					else
					{
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						BufferedInputStream bis = new BufferedInputStream(zis);
						Utils.streamCopy(bis, bos);
						IFile newFile = newProject.getFile(fileName);
						WorkspaceFileAccess.mkdirs(newFile.getParent());
						newFile.create(new ByteArrayInputStream(bos.toByteArray()), true, new NullProgressMonitor());
						bos.close();
					}
					ze = zis.getNextEntry();
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
			finally
			{
				if (zis != null)
				{
					try
					{
						zis.close();
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
				}
			}
		}
	}
}
