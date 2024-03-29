/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 * @since 2021.03
 */
public class CopySourceFolderAction extends Action
{
	public static final String JOB_FAMILY = "Copy_Build_Sources";
	public static int NORMAL_BUILD = 0;
	public static int CLEAN_BUILD = 1;

	public CopySourceFolderAction()
	{
		setText("Copy the Titanium NGClient sources");
		setToolTipText("Copies the ngclient sources to the workspace/.metadata/.plugins/com.servoy.eclipse.ngclient.ui/target/ folder");
	}

	@Override
	public boolean isEnabled()
	{
		if (super.isEnabled())
		{
			return !isTitaniumNGBuildRunning();
		}
		return false;
	}

	public static boolean isTitaniumNGBuildRunning()
	{
		Job[] jobs = Job.getJobManager().find(JOB_FAMILY);
		return jobs.length != 0;
	}

	@Override
	public void run()
	{
		if (!isEnabled())
		{
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Copy and Build TiNG",
				"Build is already running, wait for that one to finish");
			return;
		}
		File solutionProjectFolder = Activator.getInstance().getSolutionProjectFolder();
		if (solutionProjectFolder == null) return;

		final int choice = MessageDialog.open(MessageDialog.QUESTION_WITH_CANCEL, Display.getCurrent().getActiveShell(), "Copy the Titanium NGClient sources",
			"This action will perform an npm install/ng build as well.\n" +
				"Should we do a normal install or a clean install (npm ci)?\n\n" +

				"Choosing 'Copy && Build' will copy sources and run a normal ng build.\n" +
				"Choosing 'Copy && Clean build' will clean out the 'node_modules' dir and do a full npm -ci. (do this if there are problems when building (see 'Titanium NG Build Console' in the 'Console' view).",
			SWT.NONE, new String[] { "Copy && Build", "Copy && Clean build", "Cancel" });

		if (choice < 0 || choice == 2) return; // cancel

		startTitaniumNGBuild(choice);
	}

	/**
	 * @param choice can be {@link #NORMAL_BUILD} or {@link #CLEAN_BUILD}
	 */
	public static void startTitaniumNGBuild(final int typeOfBuild)
	{
		File solutionProjectFolder = Activator.getInstance().getSolutionProjectFolder();
		if (solutionProjectFolder == null)
		{
			ServoyLog.logError("Cannot start a Titanium NG Build because the solution target folder is null.", null);
			return;
		}

		Job deleteJob = null;
		if (typeOfBuild == CLEAN_BUILD)
		{
			deleteJob = new Job("delete .angular and packages cache")
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					StringOutputStream console = Activator.getInstance().getConsole().outputStream();
					try
					{
						long time = System.currentTimeMillis();
						console.write("Starting to delete the main target folder: " + Activator.getInstance().getMainTargetFolder() + "\n");
						WebPackagesListener.setIgnore(true);
						Path path = Activator.getInstance().getMainTargetFolder().toPath();

						int counter = 0;
						while (true)
						{
							try
							{
								Files.walkFileTree(path, new SimpleFileVisitor<Path>()
								{
									@Override
									public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
									{
										return FileVisitResult.CONTINUE;
									}

									@Override
									public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
									{
										Files.deleteIfExists(file);
										return FileVisitResult.CONTINUE;
									}

									@Override
									public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
									{
										Files.deleteIfExists(file);
										return FileVisitResult.CONTINUE;
									}

									@Override
									public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
									{
										Files.deleteIfExists(dir);
										return FileVisitResult.CONTINUE;
									};
								});
								break;
							}
							catch (Exception e)
							{
								if (counter++ > 2) break;
							}
						}

						console.write("Done deleting the main target folder: " + Math.round((System.currentTimeMillis() - time) / 1000) + "s\n");
					}
					catch (IOException e)
					{
						Debug.error(e);
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

				@Override
				public boolean belongsTo(Object family)
				{
					return CopySourceFolderAction.JOB_FAMILY.equals(family);
				}
			};
		}
		NodeFolderCreatorJob copySources = new NodeFolderCreatorJob(solutionProjectFolder, false, true);
		copySources.setUser(true);
		copySources.addJobChangeListener(new JobChangeAdapter()
		{
			@Override
			public void done(IJobChangeEvent event)
			{
				if (typeOfBuild == CLEAN_BUILD) WebPackagesListener.setIgnoreAndCheck(false, false);
				WebPackagesListener.checkPackages(typeOfBuild == CLEAN_BUILD);
			}
		});
		if (typeOfBuild == CLEAN_BUILD)
		{
			deleteJob.addJobChangeListener(new JobChangeAdapter()
			{
				@Override
				public void done(IJobChangeEvent event)
				{
					copySources.schedule();
				}
			});
			deleteJob.schedule();
		}
		else
		{
			copySources.schedule();
		}
	}
}
