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
import org.eclipse.ui.console.IOConsoleOutputStream;

import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * @since 2020.03
 */
public class NodeFolderCreatorJob extends Job
{

	private final File nodeFolder;
	private final boolean createWatcher;
	private final boolean force;

	public NodeFolderCreatorJob(File nodeFolder, boolean createWatcher, boolean force)
	{
		super("Copy node folder to plugin location");
		this.nodeFolder = nodeFolder;
		this.createWatcher = createWatcher;
		this.force = force;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor)
	{
		IOConsoleOutputStream console = Activator.getInstance().getConsole().newOutputStream();
		try
		{
			long startTime = System.currentTimeMillis();
			long time = startTime;
			writeConsole(console, "Start to copy the NG2 sources");
			if (!nodeFolder.exists())
			{
				createFolder(nodeFolder);
			}
			boolean packageJsonChanged = true;
			File packageJsonFile = new File(nodeFolder, "package_original.json");
			URL packageJsonUrl = Activator.getInstance().getBundle().getEntry("/node/package.json");
			String bundleContent = Utils.getURLContent(packageJsonUrl);
			if (packageJsonFile.exists() && !force)
			{
				try
				{
					String fileContent = FileUtils.readFileToString(packageJsonFile, "UTF-8");
					packageJsonChanged = !fileContent.equals(bundleContent);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			if (packageJsonChanged)
			{
				try
				{
					// create the package_original.json
					FileUtils.writeStringToFile(packageJsonFile, bundleContent, "UTF-8");

					// delete the source dirs so we start clean
					FileUtils.deleteQuietly(new File(nodeFolder, "src"));
					FileUtils.deleteQuietly(new File(nodeFolder, "projects"));
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

				writeConsole(console, "Tested package.json which is changed, starting to copy " + Math.round((System.currentTimeMillis() - time) / 1000) + "s");
				time = System.currentTimeMillis();

				// copy over the latest resources
				// first level do findEntries(.. false) to avoid a long running operation in case a node_modules or dist is present in sources (when running from sources)
				// then after first level contents were ignored, look deep in remaining subdirs with findEntries(.. true)
				copyAllEntries("/node", false);

				writeConsole(console, "Copied all the sources " + Math.round((System.currentTimeMillis() - time) / 1000) + "s");
			}
			if (createWatcher) createFileWatcher(nodeFolder, null);
			writeConsole(console, "Total time to copy done " + Math.round((System.currentTimeMillis() - startTime) / 1000) + "s\n");
			Activator.getInstance().countDown();
		}
		finally
		{
			try
			{
				console.close();
			}
			catch (IOException e)
			{
			}
		}
		return Status.OK_STATUS;
	}

	private void writeConsole(IOConsoleOutputStream console, String message)
	{
		try
		{
			console.write(message + "\n");
		}
		catch (IOException e2)
		{
		}
	}

	private void copyAllEntries(String entryPath, boolean deepFindEntries)
	{
		Enumeration<URL> entries = Activator.getInstance().getBundle().findEntries(entryPath, "*", deepFindEntries);
		while (entries.hasMoreElements())
		{
			URL entry = entries.nextElement();
			String filename = entry.getFile();
			if (filename.startsWith("/node/")) filename = filename.substring("/node".length());
			else filename = filename.substring("node".length());
			if (!ignoredResource(filename))
			{
				try
				{
					if (filename.endsWith("/"))
					{
						File folder = new File(nodeFolder, filename);
						createFolder(folder);

						// if it was not a deep findEntries then sub-entries must be deep-copied
						if (!deepFindEntries) copyAllEntries(entry.getFile(), true);
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
					Activator.getInstance().getLog().error("Error copy file " + filename + "to node folder " + nodeFolder, e);
				}
			}
		}
	}

	private static boolean ignoredResource(String filename)
	{
		return filename.startsWith("/scripts") || filename.startsWith("/.vscode") || filename.startsWith("/e2e/") || filename.startsWith("/node_modules/") ||
			filename.startsWith("/node/") || filename.startsWith("/dist/") || filename.endsWith(".spec.ts") ||
			filename.startsWith("/.gitignore");
	}


	/**
	 * This is is really only for using the developer in source mode. so that it will watch the node folder.
	 * @param nodeFolder
	 */
	static void createFileWatcher(File nodeFolder, String filter)
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
					addAllDirs(file, watchService, filter);
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

									File target = new File(targetFolder, localPath.toString());
									if (kind == StandardWatchEventKinds.ENTRY_DELETE)
									{
										FileUtils.deleteQuietly(target);
									}
									else if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY)
									{
										if (filename.toFile().isDirectory())
										{
											if (kind == StandardWatchEventKinds.ENTRY_CREATE)
											{
												System.out.println("recreating folder " + filename);
												// new dir, start watching it.
												addAllDirs(filename.toFile(), watchService, filter);
												createFolder(target);
												try
												{
													FileUtils.copyDirectory(filename.toFile(), target);
												}
												catch (IOException e)
												{
													Activator.getInstance().getLog().error("Error copy dir " + filename + " to " + target, e);
												}
											}
										}
										else
										{
											System.out.println("copy changed file " + filename);
											try (InputStream is = new FileInputStream(filename.toFile()))
											{
												copyOrCreateFile(localPath.toString(), targetFolder, is);
											}
											catch (IOException e)
											{
												Activator.getInstance().getLog().error("Error copying file " + filename, e);
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
				Activator.getInstance().getLog().error("Error creating a file watcher for location " + location, e);
			}
		}
	}

	static void createFolder(File folder)
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
	private static boolean addAllDirs(File dir, WatchService watchService, String filter)
	{
		String filename = dir.toURI().getPath();
		int index = filename.indexOf("/node/");
		if (index == -1) return false;
		filename = filename.substring(index + 5);
		// skip node modules
		if (ignoredResource(filename)) return false;

		boolean registerWatch = filter == null || filename.startsWith(filter);
		try
		{
			if (registerWatch)
			{
				dir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_MODIFY);
			}
		}
		catch (IOException e1)
		{
			Activator.getInstance().getLog().error("Error registering a watch on dir " + dir, e1);
		}

		File[] dirs = dir.listFiles(new FileFilter()
		{
			@Override
			public boolean accept(File pathname)
			{
				return pathname.isDirectory();
			}
		});
		if (dirs == null) return registerWatch;

		boolean childrenWatch = false;
		for (File subDir : dirs)
		{
			childrenWatch = addAllDirs(subDir, watchService, filter) || childrenWatch;
		}
		// if a child did register then the parent needs to also register it.
		if (childrenWatch && !registerWatch)
		{
			try
			{
				dir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_MODIFY);
				registerWatch = true;
			}
			catch (IOException e)
			{
				Activator.getInstance().getLog().error("Error registering a watch on dir " + dir, e);
			}
		}
		return registerWatch;
	}

	/**
	 * @param monitor
	 * @param nodeFolder
	 * @param entry
	 * @param filename
	 * @throws CoreException
	 * @throws IOException
	 */
	static void copyOrCreateFile(String filename, File nodeFolder, InputStream is) throws IOException
	{
		File file = new File(nodeFolder, filename);
		FileUtils.copyInputStreamToFile(is, file);
	}

}
