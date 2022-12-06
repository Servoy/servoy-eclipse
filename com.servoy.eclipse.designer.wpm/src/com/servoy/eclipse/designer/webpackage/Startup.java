/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.eclipse.designer.webpackage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.specification.Package.IPackageReader;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.SemVerComparator;
import com.servoy.eclipse.designer.webpackage.endpoint.GetAllInstalledPackages;
import com.servoy.eclipse.designer.webpackage.endpoint.InstallWebPackageHandler;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.ngpackages.BaseNGPackageManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.notification.INotification;
import com.servoy.eclipse.notification.NotificationPopUpUI;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.wizards.NewSolutionWizardDefaultPackages;
import com.servoy.j2db.util.Pair;

/**
 * Used for caching wpm entries, download New Solution Wizard default packages and check for
 * updated packages in the active solution
 *
 * @author gboros
 *
 */
public class Startup implements IStartup
{

	@Override
	public void earlyStartup()
	{
		try
		{
			NewSolutionWizardDefaultPackages.getInstance().setup(GetAllInstalledPackages.getRemotePackages());
			checkForUpdatedPackages(ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject());
			ServoyModelManager.getServoyModelManager().getServoyModel().addActiveProjectListener(new IActiveProjectListener()
			{

				@Override
				public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
				{
					return true;
				}

				@Override
				public void activeProjectChanged(ServoyProject activeProject)
				{
					checkForUpdatedPackages(activeProject);
				}

				@Override
				public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
				{
				}

			});
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}

	private void checkForUpdatedPackages(final ServoyProject servoyProject)
	{
		if (servoyProject != null)
		{
			Job checkForUpdatedPackagesJob = new Job("Checking for updated packages on '" + servoyProject.getProject().getName() + "'")
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					try
					{
						BaseNGPackageManager packageManager = ServoyModelFinder.getServoyModel().getNGPackageManager();
						List<IPackageReader> packageReaders = packageManager.getAllPackageReaders();
						TreeMap<String, Pair<String, String>> projectPackages = new TreeMap<String, Pair<String, String>>();
						for (IPackageReader packageReader : packageReaders)
						{
							// skip package projects
							if (packageReader instanceof BaseNGPackageManager.ContainerPackageReader)
							{
								continue;
							}
							// skip older versions
							if (projectPackages.containsKey(packageReader.getPackageName()) &&
								packageReader.getVersion().compareTo(projectPackages.get(packageReader.getPackageName()).getRight()) < 0)
								continue;

							projectPackages.put(packageReader.getPackageName(),
								new Pair<String, String>(packageReader.getPackageDisplayname(), packageReader.getVersion()));
						}

						File SPMNotifications = new File(Activator.getDefault().getStateLocation().toFile(), "SPMNotifications");
						if (!SPMNotifications.exists()) SPMNotifications.mkdir();
						File solutionSPMNotifications = new File(SPMNotifications, servoyProject.getProject().getName());
						Properties solutionSPMNotificationsVersions = new Properties();
						if (solutionSPMNotifications.exists())
						{
							try (InputStreamReader is = new InputStreamReader(solutionSPMNotifications.toURI().toURL().openStream()))
							{
								solutionSPMNotificationsVersions.load(is);
							}
						}

						StringBuilder mustUpdatePackagesNames = new StringBuilder();
						List<JSONObject> mustUpdatePackages = new ArrayList<JSONObject>();
						StringBuilder updatedPackagesNames = new StringBuilder();
						Set<String> projectPackagesNames = projectPackages.keySet();
						List<JSONObject> remotePackages = GetAllInstalledPackages.getRemotePackages();
						for (JSONObject p : remotePackages)
						{
							String name = p.optString("name");
							if (name != null)
							{
								if (projectPackagesNames.contains(name))
								{
									JSONArray releases = p.optJSONArray("releases");
									if (releases != null && releases.length() > 0)
									{
										String projectVersion = projectPackages.get(name).getRight();

										// check for must update packages
										boolean mustUpdate = true;
										for (int i = releases.length() - 1; i >= 0; i--)
										{
											JSONObject releasePackage = releases.optJSONObject(i);
											if (releasePackage != null)
											{
												String version = releasePackage.optString("version");
												if (version != null && (SemVerComparator.compare(version, projectVersion) == 0))
												{
													mustUpdate = false;
													break;
												}
											}
										}
										if (mustUpdate)
										{
											mustUpdatePackagesNames.append(projectPackages.get(name).getLeft()).append(" - ")
												.append(projectVersion).append('\n');
											mustUpdatePackages.add(p);
										}

										// add notification for latest release
										JSONObject latestRelease = releases.optJSONObject(0);
										if (latestRelease != null)
										{
											String version = latestRelease.optString("version");
											if (version != null && SemVerComparator.compare(version, projectVersion) > 0)
											{
												String alreadyNotifiedVersion = solutionSPMNotificationsVersions.getProperty(name);
												if (alreadyNotifiedVersion == null || !alreadyNotifiedVersion.equals(version))
												{
													updatedPackagesNames.append(projectPackages.get(name).getLeft()).append(" - ")
														.append(projectVersion).append(" -> ").append(version).append('\n');
													solutionSPMNotificationsVersions.setProperty(name, version);
												}
											}
										}
									}
								}
							}
						}

						// the following packages needs to be updated
						if (mustUpdatePackages.size() > 0)
						{
							final boolean doMustUpdate[] = { false };
							PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
								ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
								if (servoyProject == activeProject)
								{
									doMustUpdate[0] = MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), "SPM",
										"The following packages are not compatible with this Servoy version and must be updated.\n\n" +
											mustUpdatePackagesNames +
											"\nClick Yes to update them now.");
								}
							});
							if (doMustUpdate[0])
							{
								// do update
								for (JSONObject p : mustUpdatePackages)
								{
									try
									{
										InstallWebPackageHandler.importPackage(p, null);
									}
									catch (IOException ex)
									{
										ServoyLog.logError(ex);
									}
								}
							}
							return Status.OK_STATUS;
						}

						if (updatedPackagesNames.length() > 0)
						{
							final ArrayList<INotification> notifications = new ArrayList<INotification>();
							notifications.add(new INotification()
							{
								public String getTitle()
								{
									return "Open SPM";
								}

								public String getDescription()
								{
									return "The following packages have updates.\n\n" + updatedPackagesNames + "\nOpen SPM to update them.";
								}

								public String getLink()
								{
									return "com.servoy.eclipse.designer.wpm.open";
								}

								public Date getDate()
								{
									return null;
								}

								public boolean isCommand()
								{
									return true;
								}
							});

							PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
								ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
								if (servoyProject == activeProject)
								{
									NotificationPopUpUI notificationPopUpUI = new NotificationPopUpUI(Display.getCurrent(), notifications, null);
									notificationPopUpUI.setDelayClose(0);
									notificationPopUpUI.open();
								}
							});
						}

						// save shown versions
						try (FileOutputStream fos = new FileOutputStream(solutionSPMNotifications))
						{
							solutionSPMNotificationsVersions.store(fos, "SPM notifications last shown versions");
						}

						// remove saved notifications for already deleted projects
						for (File file : SPMNotifications.listFiles())
						{
							if (file.isFile())
							{
								ServoyProject p = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(file.getName());
								if (p == null)
								{
									file.delete();
								}
							}
						}
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}

					return Status.OK_STATUS;
				}

			};
			checkForUpdatedPackagesJob.schedule();
		}
	}
}
