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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

public class RenameSolutionAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;

	/**
	 * Creates a new action for the given solution view.
	 *
	 * @param sev the solution view to use.
	 */
	public RenameSolutionAction(SolutionExplorerView sev)
	{
		viewer = sev;

		setText("Rename solution");
		setToolTipText("Rename solution");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			state = false;
			if (((SimpleUserNode)sel.getFirstElement()).getRealObject() instanceof ServoyProject)
			{
				state = true;
			}
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node.getRealObject() instanceof ServoyProject)
		{
			final ServoyProject servoyProject = (ServoyProject)node.getRealObject();
			final Solution editingSolution = servoyProject.getEditingSolution();

			final String oldName = servoyProject.getProject().getName();
			InputDialog nameDialog = new InputDialog(viewer.getViewSite().getShell(), "Rename solution", "Rename solution", oldName, new IInputValidator()
			{
				public String isValid(String newText)
				{
					boolean valid = IdentDocumentValidator.isJavaIdentifier(newText);
					return valid ? (newText.equalsIgnoreCase(oldName) ? "" : null) : (newText.length() == 0 ? "" : "Invalid solution name");
				}
			});
			int res = nameDialog.open();
			if (res == Window.OK)
			{
				final String name = nameDialog.getValue();
				ServoyProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(name);
				if (project == null)
				{
					WorkspaceJob saveJob = new WorkspaceJob("Renaming solution...")
					{
						@Override
						public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
						{
							try
							{
								ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
								boolean isActive = servoyModel.isSolutionActiveImportHook(oldName) || servoyModel.isSolutionActive(oldName);
								ServoyProject activeProject = servoyModel.getActiveProject();
								if (isActive)
								{
									servoyModel.setActiveProject(null, false);
								}
								servoyProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
								editingSolution.updateName(servoyModel.getNameValidator(), name);
								IProjectDescription description = servoyProject.getProject().getDescription();
								description.setName(name);
								description.setLocation(servoyProject.getProject().getLocation().removeLastSegments(1).append(name));
								servoyProject.getProject().move(description, false, null);
								EclipseRepository repository = (EclipseRepository)ServoyModel.getDeveloperRepository();
								String protectionPassword = ApplicationServerRegistry.get().calculateProtectionPassword(editingSolution.getSolutionMetaData(),
									null);
								editingSolution.getSolutionMetaData().setProtectionPassword(protectionPassword);
								repository.updateNodes(new IPersist[] { editingSolution }, true);
								servoyProject.getSolution().getSolutionMetaData().setProtectionPassword(protectionPassword);

								List<IPersist> toUpdate = new ArrayList<>();
								for (ServoyProject solution : ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProjects())
								{
									Solution editingSol = solution.getEditingSolution();
									String[] modulesNames = Utils.getTokenElements(editingSol.getModulesNames(), ",", true);
									if (modulesNames != null && modulesNames.length > 0)
									{
										for (int i = 0; i < modulesNames.length; i++)
										{
											if (oldName.equals(modulesNames[i]))
											{
												modulesNames[i] = name;
												String modulesTokenized = ModelUtils.getTokenValue(modulesNames, ",");
												editingSol.setModulesNames(modulesTokenized);
												toUpdate.add(editingSol);
												break;
											}
										}
									}
								}
								repository.updateNodes(toUpdate.toArray(new IPersist[toUpdate.size()]), false);
								if (isActive)
								{
									servoyProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
									IJobManager jobManager = Job.getJobManager();
									try
									{
										jobManager.join(ResourcesPlugin.FAMILY_MANUAL_BUILD, new NullProgressMonitor());
										jobManager.join(ResourcesPlugin.FAMILY_AUTO_BUILD, new NullProgressMonitor());
										ServoyModel.getDeveloperRepository().flushAllCachedData();
										servoyModel = (ServoyModel)servoyModel.refreshServoyProjects();
										ServoyProject svyProject = activeProject.getEditingSolution().getName().equals(name)
											? servoyModel.getServoyProject(name) : servoyModel.getServoyProject(activeProject.getProject().getName());
										servoyModel.setActiveProject(svyProject, true);
									}
									catch (Exception e)
									{
										ServoyLog.logError(e);
										MessageDialog.openError(viewer.getViewSite().getShell(), "Could not set active solution ", e.getMessage());
									}
								}
							}
							catch (RepositoryException e)
							{
								ServoyLog.logError(e);
								MessageDialog.openError(viewer.getViewSite().getShell(), "Rename failed", e.getMessage());
							}
							catch (CoreException e)
							{
								ServoyLog.logError(e);
								MessageDialog.openError(viewer.getViewSite().getShell(), "Rename failed", "Could not move the project to new directory");
							}
							return Status.OK_STATUS;
						}
					};
					saveJob.setUser(false);
					saveJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
					saveJob.schedule();
				}
				else
				{
					MessageDialog.openError(viewer.getViewSite().getShell(), "Rename failed", "A project with name '" + name + "' already exists.");
				}
			}

		}
	}
}
