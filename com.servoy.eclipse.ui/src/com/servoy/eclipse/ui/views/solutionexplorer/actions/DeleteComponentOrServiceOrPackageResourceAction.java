/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.RunInWorkspaceJob;
import com.servoy.eclipse.core.util.UIUtils.MessageAndCheckBoxDialog;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerTreeContentProvider;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * Deletes the selected components or services or packages of components or services.
 *
 * @author gganea
 */
public class DeleteComponentOrServiceOrPackageResourceAction extends Action implements ISelectionChangedListener
{

	private IStructuredSelection selection;
	private final Shell shell;
	private boolean deletePackageProjectsFromDisk;
	private final SolutionExplorerView viewer;

	private static Map<UserNodeType, String> nodeTypesToUIText = new HashMap<>();

	static
	{
		nodeTypesToUIText.put(UserNodeType.COMPONENTS_NONPROJECT_PACKAGE, "component package");
		nodeTypesToUIText.put(UserNodeType.LAYOUT_NONPROJECT_PACKAGE, "layout package");
		nodeTypesToUIText.put(UserNodeType.SERVICES_NONPROJECT_PACKAGE, "service package");
		nodeTypesToUIText.put(UserNodeType.COMPONENTS_PROJECT_PACKAGE, "component package project");
		nodeTypesToUIText.put(UserNodeType.LAYOUT_PROJECT_PACKAGE, "layout package project");
		nodeTypesToUIText.put(UserNodeType.SERVICES_PROJECT_PACKAGE, "service package project");
		nodeTypesToUIText.put(UserNodeType.WEB_PACKAGE_PROJECT_IN_WORKSPACE, "package project"); // not necessarily used by active solution
		nodeTypesToUIText.put(UserNodeType.WEB_OBJECT_FOLDER, "folder");
		nodeTypesToUIText.put(UserNodeType.COMPONENT_RESOURCE, "file");
	}


	public DeleteComponentOrServiceOrPackageResourceAction(Shell shell, SolutionExplorerView viewer)
	{
		this.shell = shell;
		this.viewer = viewer;
	}

	@Override
	public void run()
	{
		if (selection != null)
		{
			// first save the current selection so that he user can't change it while the job is running
			List<SimpleUserNode> savedSelection = new ArrayList<SimpleUserNode>();
			Iterator<SimpleUserNode> it = selection.iterator();
			int packageProjectSelected = 0;
			while (it.hasNext())
			{
				SimpleUserNode next = it.next();
				savedSelection.add(next);
				if (next.getRealObject() instanceof IPackageReader &&
					SolutionExplorerTreeContentProvider.getResource((IPackageReader)next.getRealObject()) instanceof IProject) packageProjectSelected++;
			}
			boolean proceed = true;
			if (packageProjectSelected > 0)
			{
				MessageAndCheckBoxDialog dialog = new MessageAndCheckBoxDialog(shell, "Delete Package Project" + (packageProjectSelected > 1 ? "s" : ""), null,
					"Remove the selected package project" + (packageProjectSelected > 1 ? "s" : "") +
						" from the workspace?",
					"delete " + (packageProjectSelected > 1 ? "them" : "it") + " from disk as well (cannot be undone)\n", false, MessageDialog.QUESTION,
					new String[] { "Ok", "Cancel" }, 0);
				if (dialog.open() == 0)
				{
					deletePackageProjectsFromDisk = dialog.isChecked();
				}
				else proceed = false;
			}

			if (proceed && (packageProjectSelected == 0 || packageProjectSelected < selection.size()))
			{
				proceed = MessageDialog.openConfirm(shell, getText(),
					"Are you sure you want to continue" +
						(packageProjectSelected == 0 ? " with the delete?" : "?\nAll other selected resources will be deleted as well."));
			}

			if (proceed) startDeleteJob(savedSelection);
		}
	}

	private void startDeleteJob(List<SimpleUserNode> saveTheSelection)
	{
		// start the delete job
		RunInWorkspaceJob deleteJob = new RunInWorkspaceJob(new DeleteComponentOrServiceResourcesWorkspaceJob(saveTheSelection));
		deleteJob.setName("Deleting component or service resources");
		deleteJob.setRule(ServoyModel.getWorkspace().getRoot());
		deleteJob.setUser(false);
		deleteJob.schedule();
	}

	private class DeleteComponentOrServiceResourcesWorkspaceJob implements IWorkspaceRunnable
	{

		private final List<SimpleUserNode> savedSelection;

		public DeleteComponentOrServiceResourcesWorkspaceJob(List<SimpleUserNode> selection)
		{
			savedSelection = selection;
		}

		@Override
		public void run(IProgressMonitor monitor) throws CoreException
		{
			ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
			IProject resourcesProject = activeProject == null || activeProject.getResourcesProject() == null ? null
				: activeProject.getResourcesProject().getProject();
			List<IResource> resourcesToDelete = new ArrayList<>();
			for (SimpleUserNode selected : savedSelection)
			{
				Object realObject = selected.getRealObject();
				IResource resource = null;
				if (realObject instanceof IResource)
				{
					resource = (IResource)realObject;
				}
				else if (realObject instanceof IPackageReader)
				{
					resource = SolutionExplorerTreeContentProvider.getResource((IPackageReader)realObject);
				}
				else if (resourcesProject != null &&
					(selected.getType() == UserNodeType.COMPONENT || selected.getType() == UserNodeType.SERVICE || selected.getType() == UserNodeType.LAYOUT))
				{
					resource = getComponentFolderToDelete(resourcesProject, selected);
				}

				if (resource != null) resourcesToDelete.add(resource);
			}

			for (IResource resource : resourcesToDelete)
			{
				if (!resource.exists()) continue; // in case multiple nodes in Solex were selected for delete it might happen that parent and child resources were selected; and then a previous delete might have deleted this one

				try
				{
					if (resource instanceof IFolder)
					{
						if (resourcesProject != null) resourcesProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
						deleteFolder((IFolder)resource);
					}
					else
					{
						if (resource instanceof IProject)
						{
							IProject[] referencingProjects = ((IProject)resource).getReferencingProjects();
							for (IProject iProject : referencingProjects)
							{
								RemovePackageProjectReferenceAction.removeProjectReference(iProject, (IProject)resource);
							}
							((IProject)resource).delete(deletePackageProjectsFromDisk, true, monitor);
						}
						else
						{
							resource.delete(true, new NullProgressMonitor());
						}
						if (resourcesProject != null) resourcesProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
					}
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
			}

			viewer.refreshTreeCompletely();
		}

		private void deleteFolder(IContainer folder) throws CoreException
		{
			for (IResource resource : folder.members())
			{
				if (resource instanceof IFolder)
				{
					deleteFolder((IFolder)resource);
				}
				else
				{
					try
					{
						resource.delete(true, new NullProgressMonitor());
					}
					catch (CoreException e)
					{
						// can go wrong once, try again with a refresh
						resource.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
						resource.delete(true, new NullProgressMonitor());
					}
				}
			}
			folder.delete(true, new NullProgressMonitor());
		}

		private IResource getComponentFolderToDelete(IProject resources, SimpleUserNode next)
		{
			WebObjectSpecification spec = (WebObjectSpecification)next.getRealObject();
			IContainer[] dirResource;
			IResource resource = null;
			OutputStream out = null;
			InputStream in = null;
			InputStream contents = null;
			try
			{
				dirResource = resources.getWorkspace().getRoot().findContainersForLocationURI(spec.getSpecURL().toURI());
				if (dirResource.length == 1 && dirResource[0].getParent().exists()) resource = dirResource[0].getParent();

				if (resource != null)
				{
					IResource parent = resource.getParent();

					IFile m = getFile(parent);
					m.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
					contents = m.getContents();
					Manifest manifest = new Manifest(contents);
					manifest.getEntries().remove(resource.getName() + "/" + resource.getName() + ".spec");
					out = new ByteArrayOutputStream();
					manifest.write(out);
					in = new ByteArrayInputStream(out.toString().getBytes());
					m.setContents(in, IResource.FORCE, new NullProgressMonitor());
				}
			}
			catch (URISyntaxException e)
			{
				ServoyLog.logError(e);
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
			finally
			{
				try
				{
					if (out != null) out.close();
					if (in != null) in.close();
					if (contents != null) contents.close();
				}
				catch (IOException e)
				{
					ServoyLog.logError(e);
				}
			}
			return resource;
		}

		private IFile getFile(IResource resource)
		{
			String manifest = "META-INF/MANIFEST.MF";
			if (resource instanceof IFolder) return ((IFolder)resource).getFile(manifest);
			if (resource instanceof IProject) return ((IProject)resource).getFile(manifest);
			return null;

		}

	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		// allow multiple selection
		selection = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = true;
		Iterator<SimpleUserNode> it = sel.iterator();
		String textForStuffToDelete = null;
		while (it.hasNext() && state)
		{
			SimpleUserNode node = it.next();
			String uiTextForNodeType = nodeTypesToUIText.get(node.getRealType());
			state = (uiTextForNodeType != null); // this type of node can be processed if found in nodeTypesToUIText map

			if (textForStuffToDelete == null) textForStuffToDelete = uiTextForNodeType; // 1 node to delete so far
			else if (!uiTextForNodeType.equals(textForStuffToDelete)) textForStuffToDelete = "resource"; // more then 1 type of node to delete; so call them generic "resources"
			// else same type of nodes to delete so far

			if (node.getType() == UserNodeType.COMPONENT || node.getType() == UserNodeType.SERVICE || node.getType() == UserNodeType.LAYOUT)
			{
				if (node.getRealObject() instanceof WebObjectSpecification)
				{
					state = !((WebObjectSpecification)node.getRealObject()).getSpecURL().getProtocol().equals("jar");
				}
				else state = node.parent.getRealObject() instanceof IFolder || node.parent.getRealObject() instanceof IProject;
			}
			else if (node.getRealObject() instanceof IFolder)
			{
				state = state && !"META-INF".equals(((IFolder)node.getRealObject()).getName());
			}
		}
		if (state)
		{
			selection = sel;
			boolean multiple = (sel.size() > 1);
			String text = "Delete " + (multiple ? "multiple " : "") + textForStuffToDelete + (multiple ? "s" : "");
			setText(text);
			setToolTipText(text);
		}

		setEnabled(state);
	}

}
