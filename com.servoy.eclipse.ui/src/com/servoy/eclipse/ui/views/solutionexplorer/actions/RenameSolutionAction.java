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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.RunInWorkspaceJob;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.FlattenedSolution;
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
		if (node.getRealObject() instanceof ServoyProject servoyProject)
		{
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
				renameSolution(servoyProject, nameDialog.getValue(), viewer.getViewSite().getShell(), false);
			}
		}
	}

	public static void renameSolution(ServoyProject servoyProject, String newSolutionName, Shell shell, boolean waitForItToFinish)
	{
		final Solution editingSolution = servoyProject.getEditingSolution();
		final String oldName = servoyProject.getProject().getName();

		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != null)
		{
			FlattenedSolution flattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getFlattenedSolution();
			// avoid NPE by closing all editors before deactivating the solution
			if (flattenedSolution.getName() == editingSolution.getName())
			{
				EditorUtil.getActivePage().closeAllEditors(false);
			}
		}
		ServoyProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(newSolutionName);
		if (project == null)
		{
			IWorkspaceRunnable runble = new IWorkspaceRunnable()
			{
				@Override
				public void run(IProgressMonitor monitor) throws CoreException
				{
					try
					{
						IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
						boolean isActive = servoyModel.isSolutionActive(oldName);
						ServoyProject activeProject = servoyModel.getActiveProject();
						if (isActive)
						{
							servoyModel.setActiveProject(null, false);
						}
						servoyProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
						editingSolution.updateName(servoyModel.getNameValidator(), newSolutionName);
						IProjectDescription description = servoyProject.getProject().getDescription();
						description.setName(newSolutionName);
						description.setLocation(servoyProject.getProject().getLocation().removeLastSegments(1).append(newSolutionName));
						servoyProject.getProject().move(description, false, null);
						EclipseRepository repository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
						String protectionPassword = ApplicationServerRegistry.get().calculateProtectionPassword(editingSolution.getSolutionMetaData(),
							null);
						editingSolution.getSolutionMetaData().setProtectionPassword(protectionPassword);
						repository.updateNodes(new IPersist[] { editingSolution }, true);
						servoyProject.getSolution().getSolutionMetaData().setProtectionPassword(protectionPassword);

						List<IPersist> toUpdate = new ArrayList<>();
						for (ServoyProject solution : ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProjects())
						{
							Solution editingSol = solution.getEditingSolution();
							if (editingSol != null)
							{
								String[] modulesNames = Utils.getTokenElements(editingSol.getModulesNames(), ",", true);
								if (modulesNames != null && modulesNames.length > 0)
								{
									for (int i = 0; i < modulesNames.length; i++)
									{
										if (oldName.equals(modulesNames[i]))
										{
											modulesNames[i] = newSolutionName;
											String modulesTokenized = Utils.getTokenValue(modulesNames, ",");
											editingSol.setModulesNames(modulesTokenized);
											toUpdate.add(editingSol);
											break;
										}
									}
								}
							}
						}
						repository.updateNodes(toUpdate.toArray(new IPersist[toUpdate.size()]), false);
						if (isActive)
						{
							servoyProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
							IWorkingSet[] allWorkingSets = PlatformUI.getWorkbench().getWorkingSetManager().getAllWorkingSets();
							IJobManager jobManager = Job.getJobManager();
							try
							{
								jobManager.join(ResourcesPlugin.FAMILY_MANUAL_BUILD, new NullProgressMonitor());
								jobManager.join(ResourcesPlugin.FAMILY_AUTO_BUILD, new NullProgressMonitor());
								repository.flush();
								servoyModel = (ServoyModel)servoyModel.refreshServoyProjects();
								ServoyProject svyProject = activeProject.getEditingSolution().getName().equals(newSolutionName)
									? servoyModel.getServoyProject(newSolutionName) : servoyModel.getServoyProject(activeProject.getProject().getName());
								if (allWorkingSets != null)
								{
									for (IWorkingSet ws : allWorkingSets)
									{
										if (ServoyModel.SERVOY_WORKING_SET_ID.equals(ws.getId()))
										{
											List<String> paths = new ArrayList<String>();
											IAdaptable[] resources = ws.getElements();
											if (resources != null && resources.length > 0)
											{
												for (IAdaptable resource : resources)
												{
													IPath fullPath = ((IResource)resource).getFullPath();
													if (oldName.equals(fullPath.segment(0)))
													{
														IFile file = ServoyModel.getWorkspace().getRoot()
															.getFile(new Path(newSolutionName + "/" + fullPath.removeFirstSegments(1).toString()));
														fullPath = file.getFullPath();
														paths.add(fullPath.toString());
													}
													else
													{
														paths.add(fullPath.toString());
													}
												}
												svyProject.getResourcesProject().addWorkingSet(
													new WorkspaceFileAccess(ResourcesPlugin.getWorkspace()),
													ws.getName(), paths);
											}
										}
									}
								}
								servoyModel.setActiveProject(svyProject, true);
							}
							catch (Exception e)
							{
								ServoyLog.logError(e);
								Display.getDefault().asyncExec(
									() -> MessageDialog.openError(shell, "Could not set active solution ", e.getMessage()));
							}
						}
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
						Display.getDefault().asyncExec(() -> MessageDialog.openError(shell, "Rename failed", e.getMessage()));
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
						Display.getDefault().asyncExec(() -> MessageDialog.openError(shell, "Rename failed",
							"Could not move the project to new directory"));
					}
				}
			};
			if (!waitForItToFinish)
			{
				RunInWorkspaceJob saveJob = new RunInWorkspaceJob("Renaming solution...", runble);
				saveJob.setUser(false);
				saveJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
				saveJob.schedule();
			}
			else try
			{
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, false, (monitor) -> {
					try
					{
						runble.run(monitor);
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				});
			}
			catch (InvocationTargetException | InterruptedException e)
			{
				ServoyLog.logError(e);
			}
		}
		else
		{
			MessageDialog.openError(shell, "Rename failed", "A project with name '" + newSolutionName + "' already exists.");
		}
	}
}
