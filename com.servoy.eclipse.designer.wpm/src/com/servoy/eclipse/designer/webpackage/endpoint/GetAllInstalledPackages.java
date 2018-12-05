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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.SpecReloadSubject.ISpecReloadListener;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.WebPackageManagerEditorInput;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * @author gganea
 *
 */
public class GetAllInstalledPackages implements IDeveloperService, ISpecReloadListener, IActiveProjectListener, IWPMController
{
	public static final String CLIENT_SERVER_METHOD = "requestAllInstalledPackages";
	public static final String MAIN_WEBPACKAGEINDEX = "https://servoy.github.io/webpackageindex/";
	private final WebPackageManagerEndpoint endpoint;
	private static String selectedWebPackageIndex = MAIN_WEBPACKAGEINDEX;

	public GetAllInstalledPackages(WebPackageManagerEndpoint endpoint)
	{
		this.endpoint = endpoint;
		resourceListenersOn();
	}

	public JSONArray executeMethod(JSONObject msg)
	{
		return getAllInstalledPackages(msg);
	}

	public static JSONArray getAllInstalledPackages(JSONObject msg)
	{
		String activeSolutionName = ServoyModelFinder.getServoyModel().getFlattenedSolution().getName();
		ServoyProject[] activeProjecWithModules = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
		SpecProviderState componentSpecProviderState = WebComponentSpecProvider.getSpecProviderState();
		SpecProviderState serviceSpecProviderState = WebServiceSpecProvider.getSpecProviderState();
		JSONArray result = new JSONArray();
		try
		{
			List<JSONObject> remotePackages = getRemotePackages();

			for (JSONObject pack : remotePackages)
			{
				String name = pack.getString("name");
				String type = pack.getString("packageType");

				if ("Solution".equals(type))
				{
					String moduleParent = findModuleParent(ServoyModelFinder.getServoyModel().getFlattenedSolution().getSolution(), name);
					if (moduleParent != null)
					{
						pack.put("activeSolution", moduleParent);

						ServoyProject solutionProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(name);
						if (solutionProject != null)
						{
							File wpmPropertiesFile = new File(solutionProject.getProject().getLocation().toFile(), "wpm.properties");
							if (wpmPropertiesFile.exists())
							{
								Properties wpmProperties = new Properties();

								try (FileInputStream wpmfis = new FileInputStream(wpmPropertiesFile))
								{
									wpmProperties.load(wpmfis);
									String version = wpmProperties.getProperty("version");
									if (version != null)
									{
										pack.put("installed", version);
									}
								}
								catch (Exception ex)
								{
									Debug.log(ex);
								}
							}
						}
					}
					else
					{
						pack.put("activeSolution", activeSolutionName);
					}
				}
				else
				{
					IPackageReader reader = componentSpecProviderState.getPackageReader(name);
					if (reader == null) reader = serviceSpecProviderState.getPackageReader(name);
					if (reader != null)
					{
						pack.put("installed", reader.getVersion());
						pack.put("installedIsWPA", isWebPackageArchive(activeProjecWithModules, reader.getResource()));
						File installedResource = reader.getResource();
						if (installedResource != null) pack.put("installedResource", installedResource.getName());
						String parentSolutionName = getParentProjectNameForPackage(reader.getResource());
						pack.put("activeSolution", parentSolutionName != null ? parentSolutionName : activeSolutionName);
					}
					else
					{
						pack.put("activeSolution", msg != null && msg.has("solution") ? msg.get("solution") : activeSolutionName);
					}
				}

				result.put(pack);
			}
		}
		catch (Exception e)
		{
			Debug.log(e);
		}
		return result;
	}

	private static boolean isWebPackageArchive(ServoyProject[] activeProjecWithModules, File webPackageFile)
	{
		if (webPackageFile != null && webPackageFile.isFile())
		{
			for (ServoyProject sp : activeProjecWithModules)
			{
				File projectPath = sp.getProject().getLocation().toFile();
				if (webPackageFile.getParentFile().equals(new File(projectPath, SolutionSerializer.NG_PACKAGES_DIR_NAME)))
				{
					return true;
				}
			}
		}
		return false;
	}

	private static String getParentProjectNameForPackage(File packageFile)
	{
		if (packageFile != null && packageFile.isFile())
		{
			IWorkspaceRoot root = ServoyModel.getWorkspace().getRoot();
			IFile[] files = root.findFilesForLocationURI(packageFile.toURI());
			if (files.length == 1 && files[0] != null && files[0].exists())
			{
				return files[0].getProject().getName();
			}
		}

		return null;
	}

	private static String findModuleParent(Solution solution, String moduleName)
	{
		String[] modules = Utils.getTokenElements(solution.getModulesNames(), ",", true);
		List<String> modulesList = new ArrayList<String>(Arrays.asList(modules));
		if (modulesList.size() == 0)
		{
			return null;
		}
		else if (modulesList.contains(moduleName))
		{
			return solution.getName();
		}
		else
		{
			for (String module : modulesList)
			{
				ServoyProject solutionProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(module);
				if (solutionProject != null)
				{
					String moduleParent = findModuleParent(solutionProject.getSolution(), moduleName);
					if (moduleParent != null)
					{
						return moduleParent;
					}
				}
			}
		}
		return null;
	}

	public static List<JSONObject> setSelectedWebPackageIndex(String index)
	{
		selectedWebPackageIndex = index;
		try
		{
			return getRemotePackages();
		}
		catch (Exception e)
		{
			Debug.log(e);
		}
		return null;
	}

	public static List<JSONObject> getRemotePackages() throws Exception
	{
		List<JSONObject> result = new ArrayList<>();
		String repositoriesIndex = getUrlContents(selectedWebPackageIndex);

		JSONArray repoArray = new JSONArray(repositoriesIndex);
		for (int i = repoArray.length(); i-- > 0;)
		{
			Object repo = repoArray.get(i);
			if (repo instanceof JSONObject)
			{
				JSONObject repoObject = (JSONObject)repo;
				String packageResponse = getUrlContents(repoObject.getString("url"));
				if (packageResponse == null)
				{
					Debug.log("Couldn't get the package contents of: " + repoObject);
					continue;
				}
				try
				{
					String currentVersion = ClientVersion.getPureVersion();
					JSONObject packageObject = new JSONObject(packageResponse);
					JSONArray jsonArray = packageObject.getJSONArray("releases");
					List<JSONObject> toSort = new ArrayList<>();
					for (int k = jsonArray.length(); k-- > 0;)
					{
						JSONObject jsonObject = jsonArray.getJSONObject(k);
						if (jsonObject.has("servoy-version"))
						{
							String servoyVersion = jsonObject.getString("servoy-version");
							String[] minAndMax = servoyVersion.split(" - ");
							if (minAndMax[0].compareTo(currentVersion) <= 0)
							{
								toSort.add(jsonObject);
							}
						}
						else toSort.add(jsonObject);
					}
					if (toSort.size() > 0)
					{
						Collections.sort(toSort, new Comparator<JSONObject>()
						{
							@Override
							public int compare(JSONObject o1, JSONObject o2)
							{
								return o2.optString("version", "").compareTo(o1.optString("version", ""));
							}
						});

						List<JSONObject> currentVersionReleases = new ArrayList<>();
						for (JSONObject jsonObject : toSort)
						{
							if (jsonObject.has("servoy-version"))
							{
								String servoyVersion = jsonObject.getString("servoy-version");
								String[] minAndMax = servoyVersion.split(" - ");
								if (minAndMax.length > 1 && minAndMax[1].compareTo(currentVersion) <= 0)
								{
									break;
								}
							}
							currentVersionReleases.add(jsonObject);
						}
						if (currentVersionReleases.size() > 0)
						{
							packageObject.put("releases", currentVersionReleases);
							result.add(packageObject);
						}
					}
				}
				catch (Exception e)
				{
					Debug.log("Couldn't get the package contents of: " + repoObject + " error parsing: " + packageResponse, e);
					continue;
				}
			}
		}
		return result;
	}

	private static String getUrlContents(String urlToRead)
	{
		try
		{
			StringBuilder result = new StringBuilder();
			URL url = new URL(urlToRead);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("GET");
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null)
			{
				result.append(line);
			}
			rd.close();
			conn.disconnect();
			return result.toString();
		}
		catch (Exception e)
		{
			Debug.log(e);
		}
		return null;
	}

	@Override
	public void dispose()
	{
		resourceListenersOff();
	}

	private final int[] specReloadsWanted = new int[] { 0 };

	@Override
	public void webObjectSpecificationReloaded()
	{
		// TODO should we use some workspace rule instead or as well here?

		// sometimes for example 3 reloads were triggered in a short amount of time; keep the full reloads to a minimum;
		// only keep 1 reload and if that is in progress and another one is needed remember to trigger it only once
		// the first reload is over
		synchronized (specReloadsWanted)
		{
			if (specReloadsWanted[0] == 0)
			{
				specReloadsWanted[0] = 1;
				Job job = new Job("Reloading Web Package Manager specs...")
				{
					@Override
					protected IStatus run(IProgressMonitor monitor)
					{
						synchronized (specReloadsWanted)
						{
							specReloadsWanted[0] = 1; // we are starting this one reload now; all potential other wanted reloads are satisfied by this one
						}
						JSONArray packages = executeMethod(null);
						JSONObject jsonResult = new JSONObject();
						jsonResult.put("method", CLIENT_SERVER_METHOD);
						jsonResult.put("result", packages);
						endpoint.send(jsonResult.toString());

						synchronized (specReloadsWanted)
						{
							specReloadsWanted[0]--;
							if (specReloadsWanted[0] > 0) schedule(); // schedule reload again as one was needed while current one is in progress
						}
						return Status.OK_STATUS;
					}
				};
				job.schedule(100);
			}
			else
			{
				specReloadsWanted[0]++;
			}
		}
	}

	@Override
	public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
	{
		return true;
	}

	@Override
	public void activeProjectChanged(ServoyProject activeProject)
	{
		if (activeProject == null)
		{
			closeWPMEditors();
			return;
		}
		if (!"import_placeholder".equals(activeProject.getSolution().getName()))

			webObjectSpecificationReloaded();

	}

	private void closeWPMEditors()
	{
		for (IWorkbenchPage page : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages())
		{
			for (IEditorReference editorReference : page.getEditorReferences())
			{
				IEditorPart editor = editorReference.getEditor(false);
				if (editor != null)
				{
					if (editor.getEditorInput() instanceof WebPackageManagerEditorInput)
					{
						page.closeEditor(editor, false);
					}
				}
			}
		}
	}

	@Override
	public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
	{
		if (updateInfo == IActiveProjectListener.MODULES_UPDATED)
		{
			reloadPackages();
		}
	}

	public void resourceListenersOn()
	{
		WebComponentSpecProvider.getSpecReloadSubject().addSpecReloadListener(null, this);
		WebServiceSpecProvider.getSpecReloadSubject().addSpecReloadListener(null, this);
		ServoyModelManager.getServoyModelManager().getServoyModel().addActiveProjectListener(this);
	}

	public void resourceListenersOff()
	{
		WebComponentSpecProvider.getSpecReloadSubject().removeSpecReloadListener(null, this);
		WebServiceSpecProvider.getSpecReloadSubject().removeSpecReloadListener(null, this);
		ServoyModelManager.getServoyModelManager().getServoyModel().removeActiveProjectListener(this);
	}

	public void reloadPackages()
	{
		webObjectSpecificationReloaded();
	}

	public static String getSelectedWebPackageIndex()
	{
		return selectedWebPackageIndex;
	}
}
