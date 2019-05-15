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
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
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
import com.servoy.eclipse.ui.wizards.ProjectLocationComposite;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 */
public class ImportZipPackageAsProjectAction extends ImportZipPackageAction
{
	private String projectLocation;

	public ImportZipPackageAsProjectAction(SolutionExplorerView viewer)
	{
		super(viewer, "Import zip Servoy package as project",
			"Imports a zip Servoy package (component/service/layout package) into the workspace as a separate (expanded) project and references it from the solution. This is useful if you want to be able to change/extend the package.");
	}

	@Override
	public void run()
	{
		Shell shell = viewer.getSite().getShell();
		ImportPackageDialog dialog = new ImportPackageDialog(shell);
		dialog.open();
		if (dialog.getReturnCode() == Window.OK)
		{
			checkForDefaultPackageNameConflict(dialog.fileNames);
			projectLocation = dialog.projectLocationComposite.getProjectLocation();
			doImport(dialog.fileNames, dialog.filterPath);
		}
	}

	private static class ImportPackageDialog extends Dialog
	{
		private final Shell shell;
		private List fileNamesList;
		private Button selectButton;
		public String filterPath;
		public String[] fileNames;
		public ProjectLocationComposite projectLocationComposite;

		private ImportPackageDialog(Shell parentShell)
		{
			super(parentShell);
			this.shell = parentShell;
		}

		@Override
		protected void configureShell(Shell sh)
		{
			super.configureShell(sh);
			sh.setText("Import Zip Package as Project");
		}

		@Override
		protected Control createContents(Composite c)
		{
			Composite parent = new Composite(c, SWT.NONE);
			Label selected = new Label(parent, SWT.NONE);
			selected.setText("The follwing packages will be imported: ");
			selectButton = new Button(parent, SWT.PUSH);
			selectButton.setText("Select zip package(s)");
			selectButton.addListener(SWT.Selection, new Listener()
			{
				@Override
				public void handleEvent(Event event)
				{
					FileDialog fd = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
					fd.open();
					fileNames = fd.getFileNames();
					filterPath = fd.getFilterPath();
					fileNamesList.setItems(fileNames);
				}
			});
			fileNamesList = new List(parent, SWT.BORDER);
			fileNamesList.setEnabled(false);

			projectLocationComposite = new ProjectLocationComposite(parent, SWT.NONE, this.getClass().getName());

			FormLayout formLayout = new FormLayout();
			formLayout.spacing = 5;
			formLayout.marginWidth = formLayout.marginHeight = 20;
			parent.setLayout(formLayout);

			FormData formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(0, 0);
			formData.right = new FormAttachment(100, 0);
			selected.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(selected, 0);
			formData.right = new FormAttachment(100, 0);
			fileNamesList.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(50, 100);
			formData.right = new FormAttachment(100, 0);
			formData.top = new FormAttachment(fileNamesList, 10);
			selectButton.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.right = new FormAttachment(100, 0);
			formData.top = new FormAttachment(selectButton, 10);
			formData.bottom = new FormAttachment(100, 0);
			projectLocationComposite.setLayoutData(formData);

			return super.createContents(c);
		}
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
							IProject newProject = NGPackageManager.createNGPackageProject(packageNameToImport, projectLocation);

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
			java.util.List<Pair<String, File>> loaded = ServoyModelFinder.getServoyModel().getNGPackageManager().getReferencingProjectsThatLoaded(
				packageNameToBeLoaded);
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
