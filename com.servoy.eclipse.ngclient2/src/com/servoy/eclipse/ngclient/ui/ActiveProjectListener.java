package com.servoy.eclipse.ngclient.ui;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.SolutionMetaData;

public class ActiveProjectListener implements IActiveProjectListener
{
	@Override
	public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
	{
		return true;
	}

	@Override
	public void activeProjectChanged(final ServoyProject activeProject)
	{
		if (SolutionMetaData.isServoyNGSolution(activeProject.getSolution()))
		{
			Activator.getInstance().setActiveProject(activeProject);
			WorkspaceJob job = new WorkspaceJob("creating node folder in main active ngclient solution")
			{
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					IProject project = activeProject.getProject();
					IFolder nodeFolder = project.getFolder("/.node");

					if (!nodeFolder.exists())
					{
						try
						{
							createFolder(nodeFolder, monitor);
						}
						catch (CoreException e)
						{
							ServoyLog.logError("Error create the node folder in the active solution: " + project.getName() + " ," + project.getLocation(), e);
							return Status.CANCEL_STATUS;
						}
					}
					// copy over the latest resources
					Enumeration<URL> entries = Activator.getInstance().getBundle().findEntries("/node", "*", true);
					while (entries.hasMoreElements())
					{
						URL entry = entries.nextElement();
						String filename = entry.getFile();
						if (filename.startsWith("/node/")) filename = filename.substring("/node".length());
						else filename = filename.substring("node".length());

						try
						{
							if (filename.endsWith("/"))
							{
								IFolder folder = nodeFolder.getFolder(filename);
								createFolder(folder, monitor);
							}
							else
							{
								IFile file = nodeFolder.getFile(filename);
								if (!file.exists())
								{
									if (file.getParent() instanceof IFolder) createFolder((IFolder)file.getParent(), monitor);
									try (InputStream is = entry.openStream())
									{
										file.create(is, false, monitor);
										file.setDerived(true, monitor);
									}
								}
								else try (InputStream is = entry.openStream())
								{
									file.setContents(is, true, false, monitor);
								}
							}
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
					Activator.getInstance().executeNPMCommand("install", nodeFolder);
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}
	}

	private static void createFolder(IFolder folder, IProgressMonitor monitor) throws CoreException
	{
		if (!folder.exists())
		{
			if (folder.getParent() instanceof IFolder) createFolder((IFolder)folder.getParent(), monitor);
			folder.create(true, true, monitor);
			folder.setDerived(true, monitor);
		}
	}

	@Override
	public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
	{
	}

}
