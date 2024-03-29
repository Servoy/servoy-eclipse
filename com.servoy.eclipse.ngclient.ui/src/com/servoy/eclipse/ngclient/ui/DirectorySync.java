/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;

import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 * @since 2021.06
 *
 */
public class DirectorySync
{
	private final Predicate<String> ignoreFilter;
	private volatile WatchService watchService;
	private volatile boolean destroy = false;
	private volatile Thread thread;

	public DirectorySync(File srcRoot, File targetDir, Predicate<String> ignoreFilter)
	{
		this.ignoreFilter = ignoreFilter;

		try
		{
			watchService = FileSystems.getDefault().newWatchService();
			final Path dir = srcRoot.toPath();
			addAllDirs(srcRoot);
			thread = new Thread(() -> {
				while (!destroy)
				{
					// wait for key to be signaled
					WatchKey key;
					try
					{
						key = watchService.take();
						if (destroy) break;

					}
					catch (InterruptedException x)
					{
						continue;
					}
					Path parent = (Path)key.watchable();
					File targetFolder = new File(targetDir, dir.relativize(parent).toString());
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
							try
							{
								if (filename.toFile().exists())
								{
									Files.walkFileTree(target.toPath(), DeletePathVisitor.INSTANCE);
								}
							}
							catch (IOException e)
							{
								Debug.error(e);
							}
						}
						else if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY)
						{
							if (filename.toFile().isDirectory())
							{
								if (kind == StandardWatchEventKinds.ENTRY_CREATE)
								{
									System.out.println("recreating folder " + filename);
									// new dir, start watching it.
									addAllDirs(filename.toFile());
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
								if (filename.toFile().exists())
								{
									System.out.println("copy changed file " + filename);
									try (InputStream is = new FileInputStream(filename.toFile()))
									{
										copyOrCreateFile(localPath.toString(), targetFolder, is);
									}
									catch (IOException e)
									{
										if (filename.toFile().exists())
										{
											Activator.getInstance().getLog().error("Error copying file " + filename, e);
										}
									}
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
				try
				{
					if (this.watchService != null) this.watchService.close();
				}
				catch (IOException e)
				{
					Debug.error(e);
				}

			}, "DirectorySync on " + srcRoot.getCanonicalPath());
			thread.setDaemon(true);
			thread.start();
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	public void destroy()
	{
		this.destroy = true;
		thread.interrupt();
	}

	private boolean addAllDirs(File dir)
	{
		String filename = dir.toURI().getPath();
//		int index = filename.indexOf("/node/");
//		if (index == -1) return false;
//		filename = filename.substring(index + 5);
		// skip node modules
		if (this.ignoreFilter != null && this.ignoreFilter.test(filename)) return false;

		try
		{
			dir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
				StandardWatchEventKinds.ENTRY_MODIFY);
		}
		catch (IOException e1)
		{
			Activator.getInstance().getLog().error("Error registering a watch on dir " + dir, e1);
			return false;
		}

		File[] dirs = dir.listFiles(new FileFilter()
		{
			@Override
			public boolean accept(File pathname)
			{
				return pathname.isDirectory();
			}
		});
		if (dirs == null) return true;

		boolean childrenWatch = false;
		for (File subDir : dirs)
		{
			childrenWatch = addAllDirs(subDir) || childrenWatch;
		}
		// if a child did register then the parent needs to also register it.
		if (childrenWatch)
		{
			try
			{
				dir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_MODIFY);
			}
			catch (IOException e)
			{
				Activator.getInstance().getLog().error("Error registering a watch on dir " + dir, e);
				return false;
			}
		}
		return true;
	}

	static void createFolder(File folder)
	{
		if (!folder.exists())
		{
			folder.mkdirs();
		}
	}

	static void copyOrCreateFile(String filename, File nodeFolder, InputStream is) throws IOException
	{
		File file = new File(nodeFolder, filename);
		FileUtils.copyInputStreamToFile(is, file);
	}
}
