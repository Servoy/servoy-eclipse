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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.sablo.specification.Package.IPackageReader;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ngpackages.NGPackageManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 */
public class ImportZipPackageAsProjectAction extends ImportZipPackageAction
{
	public ImportZipPackageAsProjectAction(SolutionExplorerView viewer)
	{
		super(viewer, "Import zip web package as project",
			"Imports a zip web package (component/service/layout package) into the workspace as a separate (expanded) project and references it from the solution. This is useful if you want to be able to change/extend the package.");
	}

	@Override
	protected void doImport(final String[] fileNames, final String filterPath)
	{
		final SimpleUserNode selectedTreeNode = viewer.getSelectedTreeNode();
		Job job = new WorkspaceJob("Importing webpackage as project")
		{
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				for (String zipFile : fileNames)
				{
					File file = new File(filterPath + File.separator + zipFile);
					if (!checkManifestLocation(file)) continue;

					IPackageReader reader = new org.sablo.specification.Package.ZipPackageReader(file, zipFile);
					String packageNameToImport = reader.getPackageName();
					if (packageNameToImport != null)
					{
						ZipInputStream zis = null;
						try
						{
							if (!checkForExistingProjectOrLoadedPackage(packageNameToImport)) continue;
							IProject newProject = NGPackageManager.createNGPackageProject(packageNameToImport, null);

							zis = new ZipInputStream(new FileInputStream(filterPath + File.separator + zipFile));
							ZipEntry ze = zis.getNextEntry();
							while (ze != null)
							{
								String fileName = ze.getName();
								if (!".project".equals(fileName))
								{
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
								}
								ze = zis.getNextEntry();
							}
							if (selectedTreeNode.getType() == UserNodeType.SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES)
							{
								IProject project = ((ServoyProject)selectedTreeNode.parent.getRealObject()).getProject();
								IProjectDescription solutionProjectDescription = project.getDescription();
								AddAsWebPackageAction.addReferencedProjectToDescription(newProject, solutionProjectDescription);
								project.setDescription(solutionProjectDescription, new NullProgressMonitor());
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
					else
					{
						UIUtils.reportError("Import ng package zip as a project",
							"The selected file doesn't seem to be an ng package zip.\nCannot read it's package name.\n\nSkipping import.");
					}
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	protected boolean checkForExistingProjectOrLoadedPackage(String packageNameToBeLoaded)
	{
		if (ServoyModel.getWorkspace().getRoot().getProject(packageNameToBeLoaded).exists()) // we will create a new project using the packageName if no such project exists
		{
			UIUtils.reportError("Cannot import zip package as project",
				"Project with name : '" + packageNameToBeLoaded + "' already exist in the current workspace.\n\nSkipping import.");
			return false;
		}
		else
		{
			// see if an active zip or package project with the same package name exists (so we don't end up having two separate locations the same package can be loaded from in the active solution)
			List<Pair<String, File>> loaded = ServoyModelFinder.getServoyModel().getNGPackageManager().getReferencingProjectsThatLoaded(packageNameToBeLoaded);
			if (loaded.size() > 0)
			{
				UIUtils.reportError("Cannot import zip package as project",
					"The package with name: '" + packageNameToBeLoaded + "' is already used/loaded in currently active solution.\n(by project '" +
						loaded.get(0).getLeft() + "' from location '" + loaded.get(0).getRight() + "')\n\nSkipping import.");
				return false;

			}
		}
		return true;
	}

	@Override
	public boolean isEnabled()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		return node.getType() == UserNodeType.ALL_WEB_PACKAGE_PROJECTS || node.getType() == UserNodeType.SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES;
	}
}
