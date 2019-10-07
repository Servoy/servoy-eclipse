/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * @since 8.5
 */
public class NodeFolderCreatorJob extends Job
{

	private final File nodeFolder;

	/**
	 * @param nodeFolder
	 */
	public NodeFolderCreatorJob(File nodeFolder)
	{
		super("Copy node folder to plugin location");
		this.nodeFolder = nodeFolder;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor)
	{
		long time = System.currentTimeMillis();
		if (!nodeFolder.exists())
		{
			createFolder(nodeFolder);
		}
		boolean executeInstall = true;
		File packageJsonFile = new File(nodeFolder, "package.json");
		if (packageJsonFile.exists())
		{
			try
			{
				String fileContent = FileUtils.readFileToString(packageJsonFile, "UTF-8");
				URL packageJsonUrl = Activator.getInstance().getBundle().getEntry("/node/package.json");
				String bundleContent = Utils.getURLContent(packageJsonUrl);
				executeInstall = !fileContent.equals(bundleContent);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		System.err.println("tested " + (System.currentTimeMillis() - time));
		// copy over the latest resources
		Enumeration<URL> entries = Activator.getInstance().getBundle().findEntries("/node", "*", true);
		while (entries.hasMoreElements())
		{
			URL entry = entries.nextElement();
			String filename = entry.getFile();
			if (filename.startsWith("/node/")) filename = filename.substring("/node".length());
			else filename = filename.substring("node".length());
			if (ignoredResource(filename)) continue;
			try
			{
				if (filename.endsWith("/"))
				{
					File folder = new File(nodeFolder, filename);
					createFolder(folder);
				}
				else
				{
					try (InputStream is = entry.openStream())
					{
						copyOrCreateFile(filename, nodeFolder, is);
					}
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
		System.err.println("copied " + (System.currentTimeMillis() - time));
		if (executeInstall) Activator.getInstance().executeNPMInstall();
		else Activator.getInstance().executeNPMBuild();
		createFileWatcher();
		System.err.println("done " + (System.currentTimeMillis() - time));
		return Status.OK_STATUS;
	}

	private boolean ignoredResource(String filename)
	{
		return filename.startsWith("/node_modules/") || filename.startsWith("/dist/") || filename.startsWith("/node/") || filename.endsWith(".spec.ts");
	}


	/**
	 * This is is really only for using the developer in source mode. so that it will watch the node folder.
	 * @param nodeFolder
	 */
	private void createFileWatcher()
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
								File targetFolder = new File(nodeFolder, dir.relativize(parent).toString());
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
										FileUtils.deleteQuietly(new File(targetFolder, localPath.toString()));
									}
									else if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY)
									{
										if (filename.toFile().isDirectory())
										{
											if (kind == StandardWatchEventKinds.ENTRY_CREATE)
											{
												// new dir, start watching it.
												addAllDirs(filename.toFile(), watchService);
												createFolder(new File(targetFolder, localPath.toString()));
											}
										}
										else
										{
											try (InputStream is = new FileInputStream(filename.toFile()))
											{
												copyOrCreateFile(localPath.toString(), targetFolder, is);
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

	private static void createFolder(File folder)
	{
		if (!folder.exists())
		{
			folder.mkdirs();
		}
	}

	/**
	 * @param file
	 * @param watchService
	 */
	private void addAllDirs(File dir, WatchService watchService)
	{
		String filename = dir.toURI().getPath();
		int index = filename.indexOf("/node/");
		if (index == -1) return;
		filename = filename.substring(index + 5);
		// skip node modules
		if (ignoredResource(filename)) return;

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
	private void copyOrCreateFile(String filename, File nodeFolder, InputStream is)
	{
		File file = new File(nodeFolder, filename);
		try
		{
			FileUtils.copyInputStreamToFile(is, file);
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
		}
	}

}
