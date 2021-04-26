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
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.json.JSONObject;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.model.ngpackages.ILoadedNGPackagesListener;

/**
 * @author jcompager
 * @since 2021.06
 */
public class WebPackagesListener implements ILoadedNGPackagesListener
{
	private static final AtomicReference<Job> currentJob = new AtomicReference<>();

	public WebPackagesListener()
	{
		if (WebServiceSpecProvider.isLoaded())
			checkPackages();
	}

	@Override
	public void ngPackagesChanged(CHANGE_REASON changeReason, boolean loadedPackagesAreTheSameAlthoughReferencingModulesChanged)
	{
		checkPackages();
	}

	public static void checkPackages()
	{
		Job job = new Job("Checking/Installing NGClient2 Components and Services")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				try
				{
					Map<WebObjectSpecification, String> ng2Services = new HashMap<>();
					WebObjectSpecification[] allServices = WebServiceSpecProvider.getSpecProviderState().getAllWebComponentSpecifications();
					for (WebObjectSpecification webObjectSpecification : allServices)
					{
						if (!webObjectSpecification.getNG2Config().isNull("packageName"))
						{
							IPackageReader packageReader = WebServiceSpecProvider.getSpecProviderState()
								.getPackageReader(webObjectSpecification.getPackageName());
							ng2Services.put(webObjectSpecification, packageReader.getVersion());
						}
					}
					File projectFolder = Activator.getInstance().getProjectFolder();
					Set<String> packageToInstall = new HashSet<>();
					boolean sourceChanged = false;
					if (ng2Services.size() > 0)
					{
						try
						{
							IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

							File packageJson = new File(projectFolder, "package.json");
							String json = FileUtils.readFileToString(packageJson, "UTF-8");
							JSONObject jsonObject = new JSONObject(json);
							JSONObject dependencies = jsonObject.optJSONObject("dependencies");
							ng2Services.entrySet().forEach(entry -> {
								boolean sourceAdded = false;
								WebObjectSpecification spec = entry.getKey();
								JSONObject ng2Config = spec.getNG2Config();
								if ("file".equals(spec.getSpecURL().getProtocol()))
								{
									// its a file based service
									String entryPoint = ng2Config.optString("entryPoint");
									if (entryPoint != null)
									{
										try
										{
											IContainer[] containers = root.findContainersForLocationURI(spec.getSpecURL().toURI());
											if (containers != null && containers.length == 1)
											{
												IFolder file = containers[0].getProject().getFolder(entryPoint);
												if (file.exists())
												{
													String location = file.getRawLocation().toString();
													sourceAdded = true;
													String packageName = ng2Config.optString("packageName");
													String installedVersion = dependencies.optString(packageName);
													if (!installedVersion.endsWith(location))
													{
														packageToInstall.add(location);
													}
												}
											}
										}
										catch (URISyntaxException e)
										{
											e.printStackTrace();
										}
									}
								}
								if (!sourceAdded)
								{
									String packageName = ng2Config.optString("packageName");
									String packageVersion = entry.getValue();
									// check for prerelease (npm uses x.y.z-rc1 manifest: x.y.z.rc1)
									String[] split = packageVersion.split("\\.");
									if (split.length == 4)
									{
										packageVersion = split[0] + '.' + split[1] + '.' + split[2] + '-' + split[3];
									}
									String installedVersion = dependencies.optString(packageName);
									if (!installedVersion.contains(packageVersion))
									{
										packageToInstall.add(packageName + '@' + packageVersion);
									}
								}
							});
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}

					// first exeuted npm install with all the packages.
					StringBuilder command = new StringBuilder();
					command.append("install ");
					packageToInstall.forEach(packageName -> command.append(packageName).append(' '));
					command.append("--force");
					RunNPMCommand npmCommand = Activator.getInstance().createNPMCommand(command.toString());
					try
					{
						npmCommand.runCommands();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					// adjust the allservices.sevice.ts
					try
					{
						StringBuilder imports = new StringBuilder("// generated imports start\n");
						StringBuilder services = new StringBuilder(" // generated services start\n");
						StringBuilder providers = new StringBuilder("// generated providers start\n");
						StringBuilder modules = new StringBuilder("// generated modules start\n");
						ng2Services.keySet().forEach(service -> {
							if (service.getNG2Config().has("serviceName"))
							{
								// import the service
								imports.append("import { ");
								String serviceName = service.getNG2Config().optString("serviceName");
								imports.append(serviceName);
								imports.append(" } from '");
								imports.append(service.getNG2Config().optString("packageName"));
								imports.append("';\n");
								// add it to the service declartions in the constructor
								services.append("private ");
								services.append(service.getName());
								services.append(": ");
								services.append(serviceName);
								services.append(",\n");
								// add it to the providers (if it is not a module)
								providers.append(serviceName);
								providers.append(",\n");
							}
						});
						imports.append("// generated imports end");
						services.append(" // generated services end");
						providers.append("// generated providers end");
						modules.append("// generated modules end");

						String content = FileUtils.readFileToString(new File(projectFolder, "src/ngclient/allservices.service.ts"), "UTF-8");
						String old = content;
						content = replace(content, "// generated imports start", "// generated imports end", imports);
						content = replace(content, "// generated services start", "// generated services end", services);
						content = replace(content, "// generated providers start", "// generated providers end", providers);
						content = replace(content, "// generated modules start", "// generated modules end", modules);
						if (!old.equals(content))
						{
							sourceChanged = true;
							FileUtils.writeStringToFile(new File(projectFolder, "src/ngclient/allservices.service.ts"), content, "UTF-8");
						}
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (packageToInstall.size() > 0 || sourceChanged || !new File(projectFolder, "dist").exists())
					{

						npmCommand = Activator.getInstance().createNPMCommand("run build_debug_nowatch");
						try
						{
							npmCommand.runCommands();
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
					return Status.OK_STATUS;
				}
				finally
				{
					currentJob.set(null);
				}
			}

			private String replace(String content, String start, String end, StringBuilder toInsert)
			{
				int startIndex = content.indexOf(start);
				int endIndex = content.indexOf(end) + end.length();
				return content.substring(0, startIndex) + toInsert + content.substring(endIndex);
			}
		};
		// only schedule 1 and a bit later to relax first the system
		if (currentJob.compareAndSet(null, job))
			job.schedule(500);
	}

}
