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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
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
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.wizards.NewSolutionWizard;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;

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
		final String selectedSolution = pck.optString("activeSolution", null) != null ? pck.optString("activeSolution", null)
			: ServoyModelFinder.getServoyModel().getFlattenedSolution().getName();
		Map<String, Pair<String, InputStream>> solutionsWithDependencies = new HashMap<String, Pair<String, InputStream>>();
		Map<String, Pair<String, InputStream>> webpackagesWithDependencies = new HashMap<String, Pair<String, InputStream>>();
		Map<String, String> packagesInstalledResources = new HashMap<String, String>();
		getPackageWithDependencies(pck, selectedVersion, selectedSolution, solutionsWithDependencies, webpackagesWithDependencies, packagesInstalledResources);

		IFolder componentsFolder = RemoveWebPackageHandler.checkPackagesFolderCreated(selectedSolution, SolutionSerializer.NG_PACKAGES_DIR_NAME);
		for (String packageName : webpackagesWithDependencies.keySet())
		{
			String installedResource = packagesInstalledResources.get(packageName);
			importZipFileComponent(componentsFolder, webpackagesWithDependencies.get(packageName).getRight(), packageName, installedResource);
		}
		if (solutionsWithDependencies.size() > 0)
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					IRunnableWithProgress importSolutionsRunnable = NewSolutionWizard.importSolutions(solutionsWithDependencies, "Import solution",
						selectedSolution,
						false);
					try
					{
						IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
						progressService.run(true, false, importSolutionsRunnable);
						for (String packageName : solutionsWithDependencies.keySet())
						{
							NewSolutionWizard.addAsModule(packageName, selectedSolution, null);
						}
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
			});
		}
	}

	private static void getPackageWithDependencies(JSONObject pck, String selectedVersion, String selectedSolution,
		Map<String, Pair<String, InputStream>> solutionsWithDependencies, Map<String, Pair<String, InputStream>> webpackagesWithDependencies,
		Map<String, String> packagesInstalledResources)
		throws IOException
	{
		JSONArray jsonArray = pck.getJSONArray("releases");
		String packageName = pck.getString("name");
		String packageType = pck.getString("packageType");
		String urlString = null;
		String dependency = null;
		String pckVersion = null;
		for (int i = 0; i < jsonArray.length(); i++)
		{
			JSONObject release = jsonArray.optJSONObject(i);
			if (selectedVersion == null || selectedVersion.equals(release.optString("version", "")))
			{
				urlString = release.optString("url");
				dependency = release.optString("dependency", null);
				pckVersion = release.optString("version", "");
				break;
			}
		}

		if (urlString != null)
		{
			URL url = new URL(urlString);
			URLConnection conn = url.openConnection();
			if ("Solution".equals(packageType))
			{
				solutionsWithDependencies.put(packageName, new Pair<String, InputStream>(pckVersion, conn.getInputStream()));
			}
			else
			{
				webpackagesWithDependencies.put(packageName, new Pair<String, InputStream>(pckVersion, conn.getInputStream()));
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

					if (!"Solution".equals(pck.getString("packageType")))
					{
						ServoyProject activeSolutionProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(selectedSolution);
						// if active solution has an ng-package-project with the same name, skip downloading from WPM
						boolean hasNGPackageProject = false;
						for (ServoyNGPackageProject ngProject : activeSolutionProject.getNGPackageProjects())
						{
							DirPackageReader dirPackageReader = new DirPackageReader(ngProject.getProject().getLocation().toFile());
							if (nameAndVersion[0].equals(dirPackageReader.getPackageName()))
							{
								hasNGPackageProject = true;
								break;
							}
						}
						if (hasNGPackageProject)
						{
							continue;
						}
					}

					if (allInstalledPackages == null)
					{
						allInstalledPackages = GetAllInstalledPackages.getAllInstalledPackages();
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
													Display.getDefault().syncExec(new Runnable()
													{
														public void run()
														{
															response[0] = new MessageDialog(Display.getDefault().getActiveShell(), "Servoy Package Manager",
																null,
																"'" + packageName + "' requires '" + nameAndVersion[0] + "' version " + installVersion +
																	", but you already have version " + installedVersion +
																	" installed. Do you want to overwrite the installed one?",
																MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
																0).open();
														}
													});
												}
												if (response[0] == Window.OK)
												{
													getPackageWithDependencies(pckObject, installVersion, selectedSolution, solutionsWithDependencies,
														webpackagesWithDependencies, packagesInstalledResources);
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
											solutionsWithDependencies, webpackagesWithDependencies, packagesInstalledResources);
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
				return version1.equals(version2);
			case ">=" :
				return version1.compareTo(version2) >= 0;
			case ">" :
				return version1.compareTo(version2) > 0;
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
					e.printStackTrace();
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
				e.printStackTrace();
			}
		}
		eclipseFile = componentsFolder.getFile(name + ".zip");
		try
		{
			eclipseFile.create(in, IResource.NONE, new NullProgressMonitor());
		}
		catch (CoreException e)
		{
			Debug.log(e);
		}
	}

	@Override
	public void dispose()
	{
	}
}
