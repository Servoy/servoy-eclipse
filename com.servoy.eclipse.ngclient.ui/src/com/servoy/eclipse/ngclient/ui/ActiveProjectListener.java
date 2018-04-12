package com.servoy.eclipse.ngclient.ui;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Enumeration;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
						createFolder(nodeFolder, monitor);
					}
					// copy over the latest resources
					Enumeration<URL> entries = Activator.getInstance().getBundle().findEntries("/node", "*", true);
					while (entries.hasMoreElements())
					{
						URL entry = entries.nextElement();
						String filename = entry.getFile();
						if (filename.startsWith("/node/")) filename = filename.substring("/node".length());
						else filename = filename.substring("node".length());
						if (filename.startsWith("/node_modules/")) continue;
						try
						{
							if (filename.endsWith("/"))
							{
								IFolder folder = nodeFolder.getFolder(filename);
								createFolder(folder, monitor);
							}
							else
							{
								try (InputStream is = entry.openStream())
								{
									copyOrCreateFile(filename, nodeFolder, is, monitor);
								}
							}
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
					Activator.getInstance().executeNPMCommands(nodeFolder);
					createFileWatcher(nodeFolder);
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}
	}

	private static void createFolder(IFolder folder, IProgressMonitor monitor)
	{
		if (!folder.exists())
		{
			if (folder.getParent() instanceof IFolder) createFolder((IFolder)folder.getParent(), monitor);
			try
			{
				folder.create(true, true, monitor);
				folder.setDerived(true, monitor);
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	@Override
	public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
	{
	}

	/**
	 * This is is really only for using the developer in source mode. so that it will watch the node folder.
	 * @param nodeFolder
	 */
	private void createFileWatcher(final IFolder nodeFolder)
	{
		String location = Activator.getInstance().getBundle().getLocation();
		int index = location.indexOf("file:/");
		if (index != -1)
		{
			try
			{
				File file = new File(new File(new URI(location.substring(index))), "node");
				if (file.exists())
				{
					final WatchService watchService = FileSystems.getDefault().newWatchService();
					final Path dir = file.toPath();
					addAllDirs(file, watchService);
					new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							while (true)
							{
								// wait for key to be signaled
								WatchKey key;
								try
								{
									key = watchService.take();
								}
								catch (InterruptedException x)
								{
									continue;
								}
								Path parent = (Path)key.watchable();
								IFolder targetFolder = nodeFolder.getFolder(dir.relativize(parent).toString());
								for (WatchEvent< ? > event : key.pollEvents())
								{
									WatchEvent.Kind< ? > kind = event.kind();

									if (kind == StandardWatchEventKinds.OVERFLOW)
									{
										continue;
									}
									Path localPath = (Path)event.context();
									Path filename = parent.resolve(localPath);

									if (kind == StandardWatchEventKinds.ENTRY_DELETE)
									{
										IFolder folder = targetFolder.getFolder(localPath.toString());
										if (folder != null && folder.exists())
										{
											deleteDir(folder);
										}
										else
										{
											IFile wsFile = targetFolder.getFile(localPath.toString());
											if (wsFile != null)
											{
												try
												{
													wsFile.refreshLocal(IResource.DEPTH_ZERO, null);
													wsFile.delete(true, null);
												}
												catch (CoreException e)
												{
													ServoyLog.logError(e);
												}
											}
										}
									}
									else if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY)
									{
										if (filename.toFile().isDirectory())
										{
											if (kind == StandardWatchEventKinds.ENTRY_CREATE)
											{
												// new dir, start watching it.
												addAllDirs(filename.toFile(), watchService);
												createFolder(targetFolder.getFolder(localPath.toString()), null);
											}
										}
										else
										{
											try (InputStream is = new FileInputStream(filename.toFile()))
											{
												copyOrCreateFile(localPath.toString(), targetFolder, is, null);
											}
											catch (IOException e)
											{
												ServoyLog.logError(e);
											}
										}
									}

								}

								// Reset the key -- this step is critical if you want to receive further watch events.
								if (key.isValid())
								{
									key.reset();
								}
							}
						}
					}).start();
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	/**
	 * @param folder
	 */
	private void deleteDir(IFolder folder)
	{
		try
		{
			folder.refreshLocal(IResource.DEPTH_INFINITE, null);
			IResource[] members = folder.members();
			for (IResource resource : members)
			{
				if (resource instanceof IFolder)
				{
					deleteDir(folder);
				}
				else
				{
					resource.delete(true, null);
				}
			}
			folder.delete(true, false, null);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	/**
	 * @param file
	 * @param watchService
	 */
	private void addAllDirs(File dir, WatchService watchService)
	{
		// skip node modules
		if (dir.getName().equals("node_modules") || dir.getName().equals("dist")) return;

		try
		{
			dir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
				StandardWatchEventKinds.ENTRY_MODIFY);
		}
		catch (IOException e1)
		{
			ServoyLog.logError(e1);
		}

		File[] dirs = dir.listFiles(new FileFilter()
		{
			@Override
			public boolean accept(File pathname)
			{
				return pathname.isDirectory();
			}
		});
		if (dirs == null) return;

		for (File subDir : dirs)
		{
			addAllDirs(subDir, watchService);
		}
	}

	/**
	 * @param monitor
	 * @param nodeFolder
	 * @param entry
	 * @param filename
	 * @throws CoreException
	 * @throws IOException
	 */
	private void copyOrCreateFile(String filename, IFolder nodeFolder, InputStream is, IProgressMonitor monitor)
	{
		IFile file = nodeFolder.getFile(filename);
		try
		{
			if (!file.exists())
			{
				if (file.getParent() instanceof IFolder) createFolder((IFolder)file.getParent(), monitor);

				file.create(is, false, monitor);
				file.setDerived(true, monitor);
			}
			else
			{
				file.setContents(is, true, false, monitor);
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

}
