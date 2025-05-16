/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.designer.webpackage.endpoint;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.specification.Package.DirPackageReader;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.SemVerComparator;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener;
import com.servoy.eclipse.ui.wizards.NewSolutionWizard;
import com.servoy.eclipse.ui.wizards.NewSolutionWizard.SolutionPackageInstallInfo;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * @author gganea
 *
 */
public class InstallWebPackageHandler implements IDeveloperService
{
	@Override
	public JSONObject executeMethod(JSONObject msg)
	{
		JSONObject pck = msg.getJSONObject("package");
		String selected = pck.optString("selected");
		if (selected == null) return null;
		try
		{
			importPackage(pck, selected);
		}
		catch (IOException ex)
		{
			Debug.error("Error installing package", ex);
			JSONObject err = new JSONObject();
			err.put("err", ex.toString());
			return err;
		}
		return null;
	}

	public static void importPackage(JSONObject pck, String selectedVersion) throws IOException
	{
		WebPackagesListener.setIgnore(true);
		try
		{
			final boolean isMainSolutionInstall = "Solution-Main".equals(pck.getString("packageType"));
			final String selectedSolution = isMainSolutionInstall ? pck.getString("name") : pck.optString("activeSolution", null) != null
				? pck.optString("activeSolution", null) : ServoyModelFinder.getServoyModel().getFlattenedSolution().getName();
			Map<String, SolutionPackageInstallInfo> solutionsWithDependencies = new LinkedHashMap<String, SolutionPackageInstallInfo>();
			Map<String, Pair<String, File>> webpackagesWithDependencies = new HashMap<String, Pair<String, File>>();
			Map<String, String> packagesInstalledResources = new HashMap<String, String>();
			getPackageWithDependencies(pck, selectedVersion, selectedSolution, solutionsWithDependencies, webpackagesWithDependencies,
				packagesInstalledResources,
				new ArrayList<String>(), isMainSolutionInstall);

			final Boolean installingSolutions = new Boolean(solutionsWithDependencies.size() > 0);
			if (installingSolutions.booleanValue())
			{
				if (isMainSolutionInstall)
				{
					SolutionPackageInstallInfo lastSolutionPackageInstallInfo = null;
					for (SolutionPackageInstallInfo element : solutionsWithDependencies.values())
					{
						lastSolutionPackageInstallInfo = element;
					}
					lastSolutionPackageInstallInfo.keepResourcesProjectOpen = false;
				}
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						try
						{
							IRunnableWithProgress importSolutionsRunnable = NewSolutionWizard.importSolutions(solutionsWithDependencies, "Import solution",
								isMainSolutionInstall ? null : selectedSolution, false, true);

							IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
							progressService.run(true, false, importSolutionsRunnable);
							if (!isMainSolutionInstall) NewSolutionWizard.addAsModule(pck.getString("name"), selectedSolution, null);
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
						synchronized (installingSolutions)
						{
							installingSolutions.notify();
						}
					}
				});
			}
			// wait for the solutions to be installed
			if (installingSolutions.booleanValue())
			{
				synchronized (installingSolutions)
				{
					try
					{
						installingSolutions.wait();
					}
					catch (InterruptedException ex)
					{
						ServoyLog.logError(ex);
					}
				}
			}
			IFolder componentsFolder = RemoveWebPackageHandler.checkPackagesFolderCreated(selectedSolution, SolutionSerializer.NG_PACKAGES_DIR_NAME);
			for (String packageName : webpackagesWithDependencies.keySet())
			{
				String installedResource = packagesInstalledResources.get(packageName);
				importZipFileComponent(componentsFolder, new FileInputStream(webpackagesWithDependencies.get(packageName).getRight()), packageName,
					installedResource);
			}

			if (isMainSolutionInstall)
			{
				ServoyModelManager.getServoyModelManager().getServoyModel()
					.setActiveProject(ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(selectedSolution), true);
			}
		}
		finally
		{
			WebPackagesListener.setIgnore(false);
		}
	}

	private static void getPackageWithDependencies(JSONObject pck, String selectedVersion, String selectedSolution,
		Map<String, SolutionPackageInstallInfo> solutionsWithDependencies, Map<String, Pair<String, File>> webpackagesWithDependencies,
		Map<String, String> packagesInstalledResources, List<String> installActions, boolean isMainSolutionInstall)
		throws IOException
	{
		JSONArray jsonArray = pck.getJSONArray("releases");
		String packageName = pck.getString("name");
		String packageType = pck.getString("packageType");
		String urlString = null;
		String dependency = null;
		String pckVersion = null;
		String currentServoyVersion = ClientVersion.getPureVersion();
		for (int i = jsonArray.length() - 1; i >= 0; i--)
		{
			JSONObject release = jsonArray.optJSONObject(i);
			if (selectedVersion == null && release.has("servoy-version"))
			{
				//if version is not specified, then we search for the latest compatible version
				String servoyVersion = release.getString("servoy-version");
				String[] minAndMax = servoyVersion.split(" - ");
				if (SemVerComparator.compare(minAndMax[0], currentServoyVersion) <= 0 && (pckVersion == null ||
					SemVerComparator.compare(pckVersion, release.optString("version", "")) <= 0))
				{
					urlString = release.optString("url");
					dependency = release.optString("dependency", null);
					pckVersion = release.optString("version", "");
				}
			}
			else if (pckVersion == null || release.optString("version", "").equals(selectedVersion) ||
				SemVerComparator.compare(pckVersion, release.optString("version", "")) <= 0)
			{
				//the selectedVersion or the latest version we found so far
				urlString = release.optString("url");
				dependency = release.optString("dependency", null);
				pckVersion = release.optString("version", "");
			}

			if (release.optString("version", "").equals(selectedVersion))
			{
				//we have found the specific version we were looking for
				break;
			}
		}

		if (urlString != null)
		{
			File dataFile = null;
			try
			{

				dataFile = Utils.downloadUrlPackage(urlString);
				if (dataFile == null)
				{
					throw new IOException("Download error: " + urlString.substring(urlString.lastIndexOf("/") + 1));
				}
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						MessageDialog.openError(UIUtils.getActiveShell(), "Error", e.getMessage());
					}
				});
				throw e;
			}

			if ("Solution".equals(packageType) || "Solution-Main".equals(packageType))
			{
				solutionsWithDependencies.put(packageName,
					new SolutionPackageInstallInfo(pckVersion, dataFile, "Solution-Main".equals(packageType), true));
			}
			else
			{
				webpackagesWithDependencies.put(packageName, new Pair<String, File>(pckVersion, dataFile));
			}
			String installedResource = pck.optString("installedResource", null);
			if (installedResource != null) packagesInstalledResources.put(packageName, installedResource);

			if (dependency != null)
			{
				JSONArray allInstalledPackages = null;
				String[] packages = dependency.split(",");
				for (String dependendPck : packages)
				{
					String[] nameAndVersion = dependendPck.split("#");

					if (!"Solution".equals(pck.getString("packageType")) && !"Solution-Main".equals(pck.getString("packageType")))
					{
						ServoyProject activeSolutionProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(selectedSolution);
						// if active solution has an ng-package-project with the same name, skip downloading from WPM
						boolean hasNGPackageProject = false;
						if (activeSolutionProject != null)
						{
							for (ServoyNGPackageProject ngProject : activeSolutionProject.getNGPackageProjects())
							{
								DirPackageReader dirPackageReader = new DirPackageReader(ngProject.getProject().getLocation().toFile());
								if (nameAndVersion[0].equals(dirPackageReader.getPackageName()))
								{
									hasNGPackageProject = true;
									break;
								}
							}
						}
						if (hasNGPackageProject)
						{
							continue;
						}
					}

					if (allInstalledPackages == null)
					{
						allInstalledPackages = GetAllInstalledPackages.getAllInstalledPackages(isMainSolutionInstall, true);
					}

					for (Object pckObj : allInstalledPackages)
					{
						if (pckObj instanceof JSONObject)
						{
							JSONObject pckObject = (JSONObject)pckObj;
							if (pckObject.get("name").equals(nameAndVersion[0]))
							{
								String installedVersion = pckObject.optString("installed");
								JSONArray releases = pckObject.getJSONArray("releases");
								if (nameAndVersion.length > 1)
								{
									String version = "";
									String prefix = "=";
									if (nameAndVersion[1].startsWith(">="))
									{
										prefix = nameAndVersion[1].substring(0, 2);
										version = nameAndVersion[1].substring(2);
									}
									else if (nameAndVersion[1].startsWith(">"))
									{
										prefix = nameAndVersion[1].substring(0, 1);
										version = nameAndVersion[1].substring(1);
									}

									// if no compatible version already installed try to install one
									if (installedVersion.isEmpty() || installedVersion.equals(GetAllInstalledPackages.UNKNOWN_VERSION) ||
										!versionCheck(installedVersion, version, prefix))
									{
										for (int j = 0; j < releases.length(); j++)
										{
											if (versionCheck(releases.getJSONObject(j).optString("version"), version, prefix))
											{
												String installVersion = releases.getJSONObject(j).optString("version");
												int[] response = { Window.OK };
												if (!installedVersion.isEmpty())
												{
													if (installActions.contains(nameAndVersion[0] + installVersion))
													{
														// already handled
														response[0] = Window.CANCEL;
													}
													else
													{
														Display.getDefault().syncExec(() -> {

															response[0] = new MessageDialog(UIUtils.getActiveShell(), "Servoy Package Manager",
																null,
																"'" + packageName + "' requires '" + nameAndVersion[0] + "' version " + installVersion +
																	", but you have version " + installedVersion +
																	" installed. Do you want to overwrite the installed one?",
																MessageDialog.QUESTION,
																new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
																0).open();
														});
														installActions.add(nameAndVersion[0] + installVersion);
													}
												}
												if (response[0] == Window.OK)
												{
													getPackageWithDependencies(pckObject, installVersion, selectedSolution, solutionsWithDependencies,
														webpackagesWithDependencies, packagesInstalledResources, installActions, isMainSolutionInstall);
												}
												break;
											}
										}
									}
								}
								else
								{
									// if not installed, install the latest release
									if (installedVersion.isEmpty())
									{
										getPackageWithDependencies(pckObject, releases.getJSONObject(0).optString("version"), selectedSolution,
											solutionsWithDependencies, webpackagesWithDependencies, packagesInstalledResources, installActions,
											isMainSolutionInstall);
									}
								}
								break;
							}
						}
					}
				}
			}
		}
	}

	private static boolean versionCheck(String version1, String version2, String prefix)
	{
		if (version1 == null) return false;
		switch (prefix)
		{
			case "=" :
				return SemVerComparator.compare(version1, version2) == 0;
			case ">=" :
				return SemVerComparator.compare(version1, version2) >= 0;
			case ">" :
				return SemVerComparator.compare(version1, version2) > 0;
		}
		return false;
	}

	private static void importZipFileComponent(IFolder componentsFolder, InputStream in, String name, String installedResource)
	{
		if (installedResource != null)
		{
			IFile installedResourceFile = componentsFolder.getFile(installedResource);
			if (installedResourceFile.exists())
			{
				try
				{
					installedResourceFile.delete(true, new NullProgressMonitor());
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
			}
		}

		IFile eclipseFile = componentsFolder.getFile(name + ".zip");

		if (eclipseFile.exists())
		{
			try
			{
				eclipseFile.delete(true, new NullProgressMonitor());
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
		eclipseFile = componentsFolder.getFile(name + ".zip");
		try
		{
			eclipseFile.create(in, IResource.NONE, new NullProgressMonitor());
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	@Override
	public void dispose()
	{
	}
}
