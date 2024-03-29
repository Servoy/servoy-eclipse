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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.DeletingPathVisitor;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.Debug;
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
	public boolean belongsTo(Object family)
	{
		return CopySourceFolderAction.JOB_FAMILY.equals(family);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor)
	{
		IStatus jobStatus = Status.OK_STATUS;

		boolean executeNpmInstall = false;
		File fullyGenerated = new File(nodeFolder.getParent(), ".fullygenerated");
		ConsoleFactory consoleTiNG = new ConsoleFactory();
		consoleTiNG.openConsole();
		StringOutputStream console = Activator.getInstance().getConsole().outputStream();
		try
		{
			if (new File(nodeFolder.getParent(), "src").exists())
			{
				try
				{
					Files.walkFileTree(nodeFolder.getParentFile().toPath(), DeletingPathVisitor.withLongCounters());
				}
				catch (IOException e)
				{
					Debug.error(e);
				}
			}
			long startTime = System.currentTimeMillis();
			long time = startTime;
			writeConsole(console, "---- Started to check for changes in the Titanium NG sources; will copy new sources if necessary...");
			if (!nodeFolder.exists())
			{
				createFolder(nodeFolder);
			}
			boolean codeChanged = true;
			boolean mainPackageJsonChanged = false;
			File packageJsonFile = new File(nodeFolder.getParent(), "package.json");
			File packageCopyJsonFile = new File(nodeFolder.getParent(), "package_copy.json");
			Bundle bundle = Activator.getInstance().getBundle();
			URL packageJsonUrl = bundle.getEntry("/node/package.json");
			String bundleContent = Utils.getURLContent(packageJsonUrl);
			if (packageCopyJsonFile.exists() && !force && fullyGenerated.exists())
			{
				try
				{
					String fileContent = FileUtils.readFileToString(packageCopyJsonFile, "UTF-8");
					codeChanged = !fileContent.equals(bundleContent);
					mainPackageJsonChanged = codeChanged;
				}
				catch (IOException e)
				{
					ServoyLog.logError(e);
				}
			}
			File projectsFolder = new File(nodeFolder, "projects");
			File srcFolder = new File(nodeFolder, "src");
			if (!codeChanged)
			{
				if (srcFolder.exists())
				{
					Optional<File> srcMax = FileUtils.listFiles(srcFolder, TrueFileFilter.TRUE, TrueFileFilter.TRUE).stream()
						.max((file1, file2) -> {
							long tm = file1.lastModified() - file2.lastModified();
							return tm < 0 ? -1 : tm > 0 ? 1 : 0;
						});
					Optional<File> projectsMax = FileUtils.listFiles(projectsFolder, TrueFileFilter.TRUE, TrueFileFilter.TRUE).stream()
						.max((file1, file2) -> {
							long tm = file1.lastModified() - file2.lastModified();
							return tm < 0 ? -1 : tm > 0 ? 1 : 0;
						});

					long timestamp = Math.max(srcMax.isPresent() ? srcMax.get().lastModified() : 0,
						projectsMax.isPresent() ? projectsMax.get().lastModified() : 0);

					codeChanged = checkForHigherTimestamp("/node", false, timestamp, console);
				}
				else
				{
					// this is a new solution dir; just make sure we copy it
					codeChanged = true;
				}
			}
			if (codeChanged)
			{
				// delete the full parent dir because the main package json is changed
				fullyGenerated.delete();
				if (mainPackageJsonChanged)
				{
					try
					{
						Files.walkFileTree(nodeFolder.getParentFile().toPath(), DeletingPathVisitor.withLongCounters());
					}
					catch (IOException e)
					{
						Debug.error(e);
					}
					writeConsole(console, "- deleted main target folder, because the root package.json is changed (" +
						Math.round((System.currentTimeMillis() - time) / 1000) + " s)\r\n- copying the new sources...");
				}
				else
				{
					// delete only the source dirs, so we start clean
					try
					{
						if (srcFolder.exists()) Files.walkFileTree(srcFolder.toPath(), DeletingPathVisitor.withLongCounters());
					}
					catch (IOException e)
					{
						Debug.error(e);
					}
					try
					{
						if (projectsFolder.exists()) Files.walkFileTree(projectsFolder.toPath(), DeletingPathVisitor.withLongCounters());
					}
					catch (IOException e)
					{
						Debug.error(e);
					}
					writeConsole(console, "- the solution's sources were changed\r\n- deleted old sources (" +
						Math.round((System.currentTimeMillis() - time) / 1000) + " s)\r\n- copying the new sources...");
				}

				time = System.currentTimeMillis();

				// copy over the latest resources
				// first level do findEntries(.. false) to avoid a long running operation in case a node_modules or dist is present in sources (when running from sources)
				// then after first level contents were ignored, look deep in remaining subdirs with findEntries(.. true)
				copyAllEntries("/node", false);

				try
				{
					FileUtils.copyFile(new File(nodeFolder, "package.json"), packageJsonFile);
					FileUtils.copyFile(new File(nodeFolder, "package.json"), packageCopyJsonFile);
					FileUtils.copyFile(new File(nodeFolder, "package_solution.json"), new File(nodeFolder, "package.json"));

					executeNpmInstall = true;
				}
				catch (IOException e)
				{
					writeConsole(console, "\r\n" + "Exception when creating node/ng folder and moving the package json files: " + e.getMessage() + "\r\n");
					ServoyLog.logError(e);
				}

				writeConsole(console, "- the new sources were copied (" + Math.round((System.currentTimeMillis() - time) / 1000) + " s)");
			}
			if (createWatcher) createFileWatcher(nodeFolder, null);
			writeConsole(console, "Total time (check/copy operation): " + Math.round((System.currentTimeMillis() - startTime) / 1000) + " s.\r\n");
		}
		catch (RuntimeException e)
		{
			writeConsole(console, "\r\n" + "Exception when creating node/ng folder: " + e.getMessage() + "\r\n");
			ServoyLog.logError(e);
		}
		finally
		{
			if (executeNpmInstall) try
			{
				// now do an npm install on the main, parent folder
				RunNPMCommand npmUninstallPublicRunner = Activator.getInstance().createNPMCommand(nodeFolder.getParentFile(),
					Arrays.asList("uninstall", "@servoy/public"));
				npmUninstallPublicRunner.runCommand(monitor);

				if (npmUninstallPublicRunner.getExitCode() == 0)
				{
					RunNPMCommand npmInstallRunner = Activator.getInstance().createNPMCommand(nodeFolder.getParentFile(),
						Arrays.asList("install", "--legacy-peer-deps"));
					npmInstallRunner.runCommand(monitor);
					if (npmInstallRunner.getExitCode() == 0) fullyGenerated.createNewFile();
					else
					{
						writeConsole(console,
							"\r\n" + "Unexpected EXIT_CODE calling install on the parent root folder: " + npmInstallRunner.getExitCode() + "\r\n");
						jobStatus = new Status(IStatus.WARNING, getClass(),
							"Unexpected EXIT_CODE calling install on the parent root folder: " + npmInstallRunner.getExitCode());
					}
				}
				else
				{
					writeConsole(console,
						"\r\n" + "Unexpected EXIT_CODE calling uninstall on @servoy/public: " + npmUninstallPublicRunner.getExitCode() + "\r\n");
					jobStatus = new Status(IStatus.WARNING, getClass(),
						"Unexpected EXIT_CODE calling uninstall on @servoy/public: " + npmUninstallPublicRunner.getExitCode());
				}
			}
			catch (IOException | InterruptedException e1)
			{
				writeConsole(console, "\r\n" + "Exception when calling install on the parent root folder: " + e1.getMessage() + "\r\n");
				jobStatus = new Status(IStatus.WARNING, getClass(),
					"Exception when calling install on the parent root folder: " + e1.getMessage());
				ServoyLog.logError(e1);
			}

			try
			{
				console.close();
			}
			catch (IOException e)
			{
			}
		}
		return jobStatus;
	}

	private void writeConsole(StringOutputStream console, String message)
	{
		try
		{
			console.write(message + "\n");
		}
		catch (IOException e2)
		{
		}
	}

	private boolean checkForHigherTimestamp(String entryPath, boolean deepFindEntries, long timestamp, StringOutputStream console)
	{
		boolean higherFound = false;
		Enumeration<URL> entries = Activator.getInstance().getBundle().findEntries(entryPath, "*", deepFindEntries);
		while (entries.hasMoreElements() && !higherFound)
		{
			URL entry = entries.nextElement();
			String filename = entry.getFile();
			if (filename.startsWith("/node/")) filename = filename.substring("/node".length());
			else filename = filename.substring("node".length());
			if (!ignoredResource(filename) && !filename.endsWith("package-lock.json"))
			{
				try
				{
					if (filename.endsWith("/"))
					{
						// if it was not a deep findEntries then sub-entries must be deep-checked
						if (!deepFindEntries) higherFound = checkForHigherTimestamp(entry.getFile(), true, timestamp, console);
					}
					else
					{
						long lm = entry.openConnection().getLastModified();
						if (lm > timestamp)
						{
							writeConsole(console, "- core source changed: " + entry.getFile() + "; build will be triggered.");
							higherFound = true;
						}
					}
				}
				catch (Exception e)
				{
					Activator.getInstance().getLog().error("Error checking timestamp for  file " + filename + "for node folder " + nodeFolder, e);
				}
			}
		}
		return higherFound;
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
		return filename.startsWith("/scripts") || filename.startsWith("/.vscode") || filename.startsWith("/e2e/") || filename.indexOf("/node_modules/") != -1 ||
			filename.startsWith("/node/") || filename.startsWith("/dist/") || filename.endsWith(".spec.ts") ||
			filename.startsWith("/.gitignore") || filename.startsWith("/.angular/");
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
										try
										{
											if (filename.toFile().exists())
											{
												Files.walkFileTree(target.toPath(), DeletingPathVisitor.withLongCounters());
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
											if (filename.toFile().exists())
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

	static void copyOrCreateFile(String filename, File nodeFolder, InputStream is) throws IOException
	{
		File file = new File(nodeFolder, filename);
		FileUtils.copyInputStreamToFile(is, file);
	}

}
