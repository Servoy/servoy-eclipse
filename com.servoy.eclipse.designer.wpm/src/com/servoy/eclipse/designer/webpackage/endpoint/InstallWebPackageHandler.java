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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.specification.Package.DirPackageReader;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.wizards.ImportSolutionWizard;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * @author gganea
 *
 */
public class InstallWebPackageHandler implements IDeveloperService
{

	private final IWPMController resourceListenerSwitch;

	public InstallWebPackageHandler(IWPMController resourceListenerSwitch)
	{
		this.resourceListenerSwitch = resourceListenerSwitch;
	}

	@Override
	public JSONObject executeMethod(JSONObject msg)
	{
		JSONObject pck = msg.getJSONObject("package");
		String selected = pck.optString("selected");
		if (selected == null) return null;
		importPackage(pck, selected, resourceListenerSwitch);
		return null;
	}


	public static void importPackage(JSONObject pck, String selectedVersion, IWPMController resourceListenerSwitch)
	{
		importPackage(pck, selectedVersion, null, resourceListenerSwitch);
	}

	private static void importPackage(JSONObject pck, String selectedVersion, String selectedSolution, IWPMController resourceListenerSwitch)
	{
		String urlString = null;
		JSONArray jsonArray = pck.getJSONArray("releases");
		String dependency = null;
		for (int i = 0; i < jsonArray.length(); i++)
		{
			JSONObject release = jsonArray.optJSONObject(i);
			if (selectedVersion == null || selectedVersion.equals(release.optString("version", "")))
			{
				urlString = release.optString("url");
				dependency = release.optString("dependency", null);
				break;
			}
		}
		try
		{

			URL url = new URL(urlString);
			URLConnection conn = url.openConnection();

			String packageName = pck.getString("name");
			String packageType = pck.getString("packageType");
			String packageVersion = pck.optString("selected");
			String solutionName = pck.optString("activeSolution", null);
			String installedResource = pck.optString("installedResource", null);

			if (solutionName == null)
			{
				solutionName = selectedSolution != null ? selectedSolution : ServoyModelFinder.getServoyModel().getFlattenedSolution().getName();
			}

			try (InputStream in = conn.getInputStream())
			{
				if ("Solution".equals(packageType))
				{
					importSolution(in, packageName, packageVersion, solutionName, resourceListenerSwitch);
				}
				else
				{
					IFolder componentsFolder = RemoveWebPackageHandler.checkPackagesFolderCreated(solutionName, SolutionSerializer.NG_PACKAGES_DIR_NAME);
					importZipFileComponent(componentsFolder, in, packageName, installedResource);
				}
			}

			if (dependency != null)
			{
				try
				{
					ServoyProject activeSolutionProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
					JSONArray allInstalledPackages = null;
					String[] packages = dependency.split(",");
					for (String dependendPck : packages)
					{
						String[] nameAndVersion = dependendPck.split("#");

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

						if (allInstalledPackages == null)
						{
							allInstalledPackages = GetAllInstalledPackages.getAllInstalledPackages(null);
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
										if (installedVersion.isEmpty() || !versionCheck(installedVersion, version, prefix))
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
																response[0] = new MessageDialog(Display.getDefault().getActiveShell(), "Web Package Manager",
																	null,
																	"'" + packageName + "' requires '" + nameAndVersion[0] + "' version " + installVersion +
																		", but you already have version " + installedVersion +
																		" installed. Do you want to overwrite the installed one?",
																	MessageDialog.QUESTION,
																	new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0).open();
															}
														});
													}
													if (response[0] == Window.OK)
													{
														importPackage(pckObject, installVersion, solutionName, resourceListenerSwitch);
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
											importPackage(pckObject, releases.getJSONObject(0).optString("version"), solutionName, resourceListenerSwitch);
										}
									}
									break;
								}
							}
						}

					}
				}
				catch (Exception e)
				{
					Debug.log(e);
				}
			}
		}
		catch (IOException e)
		{
			Debug.log(e);
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

	private static void importSolution(InputStream is, final String name, final String version, final String targetSolution,
		IWPMController resourceListenerSwitch) throws IOException, FileNotFoundException
	{
		if (name.equals(targetSolution)) return; // import solution and target can't be the same
		final File importSolutionFile = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(), name + ".servoy");
		if (importSolutionFile.exists())
		{
			importSolutionFile.delete();
		}
		try (FileOutputStream fos = new FileOutputStream(importSolutionFile))
		{
			if (resourceListenerSwitch != null)
			{
				resourceListenerSwitch.resourceListenersOff();
			}
			Utils.streamCopy(is, fos);
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					ImportSolutionWizard importSolutionWizard = new ImportSolutionWizard();
					importSolutionWizard.setSolutionFilePath(importSolutionFile.getAbsolutePath());
					importSolutionWizard.setAllowSolutionFilePathSelection(false);
					importSolutionWizard.setActivateSolution(false);
					importSolutionWizard.init(PlatformUI.getWorkbench(), null);
					WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), importSolutionWizard);
					if (dialog.open() == Window.OK)
					{
						ServoyProject targetServoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(targetSolution);
						if (targetServoyProject != null)
						{
							Solution editingSolution = targetServoyProject.getEditingSolution();
							if (editingSolution != null)
							{
								String[] modules = Utils.getTokenElements(editingSolution.getModulesNames(), ",", true);
								List<String> modulesList = new ArrayList<String>(Arrays.asList(modules));
								if (!modulesList.contains(name))
								{
									modulesList.add(name);
								}
								String modulesTokenized = ModelUtils.getTokenValue(modulesList.toArray(new String[] { }), ",");
								editingSolution.setModulesNames(modulesTokenized);

								try
								{
									targetServoyProject.saveEditingSolutionNodes(new IPersist[] { editingSolution }, false);
								}
								catch (RepositoryException e)
								{
									ServoyLog.logError("Cannot save new module list for active module " + targetServoyProject.getProject().getName(), e);
								}

								// save version
								Properties wpmProperties = new Properties();
								wpmProperties.put("version", version);

								try (ByteArrayOutputStream wpmbos = new ByteArrayOutputStream())
								{
									wpmProperties.store(wpmbos, "");
									byte[] wpmPropertiesBytes = wpmbos.toByteArray();
									IProject importedProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(name).getProject();
									WorkspaceFileAccess importedProjectFA = new WorkspaceFileAccess(importedProject.getWorkspace());
									importedProjectFA.setContents(importedProject.getFullPath().append("wpm.properties").toOSString(), wpmPropertiesBytes);
								}
								catch (Exception ex)
								{
									Debug.log(ex);
								}
							}
						}
					}

				}
			});
		}
		finally
		{
			if (resourceListenerSwitch != null)
			{
				resourceListenerSwitch.resourceListenersOn();
				resourceListenerSwitch.reloadPackages();
			}
			if (importSolutionFile.exists())
			{
				importSolutionFile.delete();
			}
		}
	}

	@Override
	public void dispose()
	{
	}

}
