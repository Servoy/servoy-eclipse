/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.exporter.apps.war;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.app.IApplicationContext;
import org.sablo.specification.WebComponentPackage;
import org.sablo.specification.WebComponentPackage.IPackageReader;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.exporter.apps.common.AbstractWorkspaceExporter;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel;
import com.servoy.eclipse.model.war.exporter.ExportException;
import com.servoy.eclipse.model.war.exporter.ServerConfiguration;
import com.servoy.eclipse.model.war.exporter.WarExporter;
import com.servoy.j2db.server.ngclient.startup.resourceprovider.ResourceProvider;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * Eclipse application that can be used for exporting servoy solutions in .war format.
 * @author gboros
 */
public class WarWorkspaceExporter extends AbstractWorkspaceExporter<WarArgumentChest>
{

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.exporter.apps.common.AbstractWorkspaceExporter#createArgumentChest(org.eclipse.equinox.app.IApplicationContext)
	 */
	@Override
	protected WarArgumentChest createArgumentChest(IApplicationContext context)
	{
		return new WarArgumentChest((String[])context.getArguments().get(IApplicationContext.APPLICATION_ARGS));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.exporter.apps.common.AbstractWorkspaceExporter#exportActiveSolution(com.servoy.eclipse.exporter.apps.common.IArgumentChest)
	 */
	@Override
	protected void exportActiveSolution(final WarArgumentChest configuration)
	{
		initComponentProviders();
		WarExporter warExporter = new WarExporter(new AbstractWarExportModel()
		{
			@Override
			public boolean isExportActiveSolution()
			{
				return configuration.isExportActiveSolutionOnly();
			}

			@Override
			public String getStartRMIPort()
			{
				return null;
			}

			@Override
			public boolean getStartRMI()
			{
				return false;
			}

			@Override
			public String getServoyPropertiesFileName()
			{
				String servoyPropertiesFileName = configuration.getSettingsFileName();
				if (servoyPropertiesFileName == null)
				{
					servoyPropertiesFileName = getServoyApplicationServerDir() + File.separator + "servoy.properties";
				}
				return servoyPropertiesFileName;
			}

			@Override
			public ServerConfiguration getServerConfiguration(String serverName)
			{
				return null;
			}

			@Override
			public SortedSet<String> getSelectedServerNames()
			{
				return null;
			}

			@Override
			public List<String> getPlugins()
			{
				Set<String> names = null;
				if (configuration.getPlugins() != null)
				{
					names = new HashSet<String>(Arrays.asList(configuration.getPlugins().toLowerCase().split(" ")));
				}
				return getFiles(ApplicationServerRegistry.get().getPluginManager().getPluginsDir(), names);
			}

			@Override
			public List<String> getLafs()
			{
				Set<String> names = null;
				if (configuration.getLafs() != null)
				{
					names = new HashSet<String>(Arrays.asList(configuration.getLafs().toLowerCase().split(" ")));
				}
				return getFiles(ApplicationServerRegistry.get().getLafManager().getLAFDir(), names);
			}

			@Override
			public String getWarFileName()
			{
				String warFileName = configuration.getWarFileName();
				if (warFileName == null)
				{
					ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
					warFileName = activeProject.getProject().getName();
				}
				if (!warFileName.endsWith(".war")) warFileName += ".war";
				return configuration.getExportFilePath() + File.separator + warFileName;
			}

			@Override
			public List<String> getDrivers()
			{
				Set<String> names = null;
				if (configuration.getDrivers() != null)
				{
					names = new HashSet<String>(Arrays.asList(configuration.getDrivers().toLowerCase().split(" ")));
				}
				return getFiles(ApplicationServerRegistry.get().getServerManager().getDriversDir(), names);
			}

			@Override
			public List<String> getBeans()
			{
				Set<String> names = null;
				if (configuration.getBeans() != null)
				{
					names = new HashSet<String>(Arrays.asList(configuration.getBeans().toLowerCase().split(" ")));
				}
				return getFiles(ApplicationServerRegistry.get().getBeanManager().getBeansDir(), names);
			}

			@Override
			public boolean allowOverwriteSocketFactoryProperties()
			{
				return false;
			}

			@Override
			public String getServoyApplicationServerDir()
			{
				return configuration.getAppServerDir();
			}

			List<String> getFiles(File dir, final Set<String> fileNames)
			{
				return Arrays.asList(dir.list(new FilenameFilter()
				{
					public boolean accept(File dir, String name)
					{
						boolean accept = fileNames != null ? fileNames.contains(name.toLowerCase()) : true;
						return accept && (name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".zip"));
					}
				}));
			}

			@Override
			public List<String> getPluginLocations()
			{
				return Arrays.asList(configuration.getPluginLocations().split(" "));
			}

			@Override
			public Set<String> getExportedComponents()
			{
				if (configuration.getSelectedComponents() == null) return configuration.getSelectedServices() != null ? super.getUsedComponents() : null;
				if (configuration.getSelectedComponents().equals("")) return super.getUsedComponents();

				WebComponentSpecProvider provider = WebComponentSpecProvider.getInstance();
				Set<String> set = new HashSet<String>();
				if (configuration.getSelectedComponents().trim().equalsIgnoreCase("all"))
				{
					for (WebComponentSpecification spec : provider.getWebComponentSpecifications())
					{
						set.add(spec.getName());
					}
				}
				else
				{
					set.addAll(Arrays.asList(configuration.getSelectedComponents().split(" ")));
					for (String componentName : set)
					{
						if (provider.getWebComponentSpecification(componentName) == null)
						{
							System.out.println(componentName + " is not a valid component name.");
							set.remove(componentName);
						}
					}
					set.addAll(super.getUsedComponents());
				}
				return set;

			}

			@Override
			public Set<String> getExportedServices()
			{
				if (configuration.getSelectedServices() == null) return configuration.getSelectedComponents() != null ? super.getUsedServices() : null;
				if (configuration.getSelectedServices().equals("")) return super.getUsedServices();
				Set<String> set = new HashSet<String>();
				WebServiceSpecProvider provider = WebServiceSpecProvider.getInstance();
				if (configuration.getSelectedServices().trim().equalsIgnoreCase("all"))
				{
					for (WebComponentSpecification spec : provider.getWebServiceSpecifications())
					{
						set.add(spec.getName());
					}
				}
				else
				{
					set.addAll(Arrays.asList(configuration.getSelectedServices().split(" ")));
					for (String serviceName : set)
					{
						if (provider.getWebServiceSpecification(serviceName) == null)
						{
							System.out.println(serviceName + " is not a valid service name.");
							set.remove(serviceName);
						}
					}
					set.addAll(super.getUsedServices());
				}
				return set;
			}


			@Override
			public String getFileName()
			{
				return null;
			}

			@Override
			public boolean isExportMetaData()
			{
				return configuration.shouldExportMetadata();
			}

			@Override
			public boolean isExportSampleData()
			{
				return configuration.isExportSampleData();
			}

			@Override
			public boolean isExportI18NData()
			{
				return configuration.isExportI18NData();
			}

			@Override
			public int getNumberOfSampleDataExported()
			{
				return configuration.getNumberOfSampleDataExported();
			}

			@Override
			public boolean isExportAllTablesFromReferencedServers()
			{
				return configuration.isExportAllTablesFromReferencedServers();
			}

			@Override
			public boolean isCheckMetadataTables()
			{
				return configuration.checkMetadataTables();
			}

			@Override
			public boolean isExportUsingDbiFileInfoOnly()
			{
				return configuration.shouldExportUsingDbiFileInfoOnly();
			}

			@Override
			public boolean isExportUsers()
			{
				return configuration.exportUsers();
			}


			@Override
			public boolean isOverwriteGroups()
			{
				return configuration.isOverwriteGroups();
			}

			@Override
			public boolean isAllowSQLKeywords()
			{
				return configuration.isAllowSQLKeywords();
			}

			@Override
			public boolean isOverrideSequenceTypes()
			{
				return configuration.isOverwriteGroups();
			}

			@Override
			public boolean isInsertNewI18NKeysOnly()
			{
				return configuration.isInsertNewI18NKeysOnly();
			}

			@Override
			public boolean isOverrideDefaultValues()
			{
				return configuration.isOverrideDefaultValues();
			}

			@Override
			public int getImportUserPolicy()
			{
				return configuration.getImportUserPolicy();
			}

			@Override
			public boolean isAddUsersToAdminGroup()
			{
				return configuration.isAddUsersToAdminGroup();
			}

			@Override
			public boolean isAllowDataModelChanges()
			{
				return true;
			}

			@Override
			public boolean isUpdateSequences()
			{
				return configuration.isUpdateSequences();
			}
		});
		try
		{
			warExporter.doExport(new IProgressMonitor()
			{
				@Override
				public void worked(int work)
				{
				}

				@Override
				public void subTask(String name)
				{
					outputExtra(name);
				}

				@Override
				public void setTaskName(String name)
				{
					outputExtra(name);
				}

				@Override
				public void setCanceled(boolean value)
				{
				}

				@Override
				public boolean isCanceled()
				{
					return false;
				}

				@Override
				public void internalWorked(double work)
				{
				}

				@Override
				public void done()
				{
				}

				@Override
				public void beginTask(String name, int totalWork)
				{
					outputExtra(name);
				}
			});
		}
		catch (ExportException ex)
		{
			ServoyLog.logError("Failed to export solution.", ex);
			outputError("Exception while exporting solution. EXPORT FAILED for this solution. Check workspace log.");
			exitCode = EXIT_EXPORT_FAILED;
		}
	}

	private void initComponentProviders()
	{
		Map<String, IPackageReader> componentReaders = new HashMap<String, IPackageReader>();
		Map<String, IPackageReader> serviceReaders = new HashMap<String, IPackageReader>();

		ServoyResourcesProject activeResourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
		if (activeResourcesProject != null)
		{
			if (componentReaders.size() > 0)
			{
				ResourceProvider.refreshComponentResources(componentReaders.values());
				componentReaders.clear();
			}
			if (serviceReaders.size() > 0)
			{
				ResourceProvider.refreshServiceResources(serviceReaders.values());
				serviceReaders.clear();
			}
			componentReaders.putAll(readDir(new NullProgressMonitor(), activeResourcesProject, SolutionSerializer.COMPONENTS_DIR_NAME));
			serviceReaders.putAll(readDir(new NullProgressMonitor(), activeResourcesProject, SolutionSerializer.SERVICES_DIR_NAME));

			ResourceProvider.addComponentResources(componentReaders.values());
			ResourceProvider.addServiceResources(serviceReaders.values());
		}
	}

	private Map<String, IPackageReader> readDir(IProgressMonitor monitor, ServoyResourcesProject activeResourcesProject, String folderName)
	{
		Map<String, IPackageReader> readers = new HashMap<String, IPackageReader>();
		IFolder folder = activeResourcesProject.getProject().getFolder(folderName);
		if (folder.exists())
		{
			try
			{
				folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				IResource[] members = folder.members();
				for (IResource resource : members)
				{
					String name = resource.getName();
					int index = name.lastIndexOf('.');
					if (index != -1)
					{
						name = name.substring(0, index);
					}
					if (resource instanceof IFolder)
					{
						IFolder folderResource = (IFolder)resource;
						if ((folderResource).getFile("META-INF/MANIFEST.MF").exists())
						{
							File f = new File(resource.getRawLocationURI());
							readers.put(name, new WebComponentPackage.DirPackageReader(f));
						}
					}
					else if (resource instanceof IFile)
					{
						readers.put(name, new WebComponentPackage.JarPackageReader(new File(resource.getRawLocationURI())));
					}
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
		return readers;
	}
}
