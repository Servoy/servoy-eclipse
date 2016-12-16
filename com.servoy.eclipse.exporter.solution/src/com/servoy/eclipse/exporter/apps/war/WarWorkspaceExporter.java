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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.app.IApplicationContext;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.exporter.apps.common.AbstractWorkspaceExporter;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel;
import com.servoy.eclipse.model.war.exporter.ExportException;
import com.servoy.eclipse.model.war.exporter.ServerConfiguration;
import com.servoy.eclipse.model.war.exporter.WarExporter;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * Eclipse application that can be used for exporting servoy solutions in .war format.
 * @author gboros
 */
public class WarWorkspaceExporter extends AbstractWorkspaceExporter<WarArgumentChest>
{

	private final class CommandLineWarExportModel extends AbstractWarExportModel
	{
		private final WarArgumentChest configuration;

		/**
		 * @param configuration
		 * @param servicesSpecProviderState
		 * @param componentsSpecProviderState
		 */
		private CommandLineWarExportModel(WarArgumentChest configuration, SpecProviderState componentsSpecProviderState, SpecProviderState servicesSpecProviderState)
		{
			super(componentsSpecProviderState, servicesSpecProviderState);
			this.configuration = configuration;
			search();
		}

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
			String warSettingsFileName = configuration.getWarSettingsFileName();
			if (warSettingsFileName == null)
			{
				String servoyPropertiesFileName = configuration.getSettingsFileName();
				if (servoyPropertiesFileName == null)
				{
					servoyPropertiesFileName = getServoyApplicationServerDir() + File.separator + "servoy.properties";
				}
				return servoyPropertiesFileName;
			}
			return warSettingsFileName;
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
			String[] list = dir.list(new FilenameFilter()
			{
				public boolean accept(File d, String name)
				{
					boolean accept = fileNames != null ? fileNames.contains(name.toLowerCase()) : true;
					return accept && (name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".zip"));
				}
			});
			if (list == null || list.length == 0) return Collections.emptyList();
			return Arrays.asList(list);
		}

		@Override
		public List<String> getPluginLocations()
		{
			return Arrays.asList(configuration.getPluginLocations().split(" "));
		}

		@Override
		public Set<String> getExportedComponents()
		{
			if (configuration.getSelectedComponents() == null) return configuration.getSelectedServices() != null ? getUsedComponents() : null;
			if (configuration.getSelectedComponents().equals("")) return getUsedComponents();

			Set<String> set = new HashSet<String>();
			if (configuration.getSelectedComponents().trim().equalsIgnoreCase("all"))
			{
				for (WebObjectSpecification spec : componentsSpecProviderState.getAllWebComponentSpecifications())
				{
					set.add(spec.getName());
				}
			}
			else
			{
				set.addAll(Arrays.asList(configuration.getSelectedComponents().split(" ")));
				for (String componentName : set)
				{
					if (componentsSpecProviderState.getWebComponentSpecification(componentName) == null)
					{
						// TODO shouldn't this be an error and shouldn't the exporter fail nicely with a new exit code? I'm thinking now of Jenkins usage and failing sooner rather then later (at export rather then when testing)
						output("'" + componentName + "' is not a valid component name or it could not be found. Ignoring.");
						set.remove(componentName);
					}
				}
				set.addAll(getUsedComponents());
			}
			return set;
		}

		@Override
		public Set<String> getExportedServices()
		{
			if (configuration.getSelectedServices() == null) return configuration.getSelectedComponents() != null ? getUsedServices() : null;
			if (configuration.getSelectedServices().equals("")) return getUsedServices();
			Set<String> set = new HashSet<String>();
			if (configuration.getSelectedServices().trim().equalsIgnoreCase("all"))
			{
				for (WebObjectSpecification spec : NGUtils.getAllWebServiceSpecificationsThatCanBeUncheckedAtWarExport(servicesSpecProviderState))
				{
					set.add(spec.getName());
				}
			}
			else
			{
				set.addAll(Arrays.asList(configuration.getSelectedServices().split(" ")));
				for (String serviceName : set)
				{
					if (servicesSpecProviderState.getWebComponentSpecification(serviceName) == null)
					{
						// TODO shouldn't this be an error and shouldn't the exporter fail nicely with a new exit code? I'm thinking now of Jenkins usage and failing sooner rather then later (at export rather then when testing)
						output("'" + serviceName + "' is not a valid service name or it could not be found. Ignoring.");
						set.remove(serviceName);
					}
				}
				set.addAll(getUsedServices());
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
			return !configuration.isStopOnAllowDataModelChanges();
		}

		@Override
		public boolean isUpdateSequences()
		{
			return configuration.isUpdateSequences();
		}

		@Override
		public boolean isAutomaticallyUpgradeRepository()
		{
			return configuration.automaticallyUpdateRepository();
		}

		@Override
		public boolean isCreateTomcatContextXML()
		{
			return configuration.isCreateTomcatContextXML();
		}

		@Override
		public boolean isClearReferencesStatic()
		{
			return configuration.isClearReferencesStatic();
		}

		@Override
		public boolean isClearReferencesStopThreads()
		{
			return configuration.isClearReferencesStopThreads();
		}

		@Override
		public boolean isClearReferencesStopTimerThreads()
		{
			return configuration.isClearReferencesStopTimerThreads();
		}

		@Override
		public boolean isAntiResourceLocking()
		{
			return configuration.isAntiResourceLocking();
		}

		@Override
		public List<String> getExcludedComponentPackages()
		{
			return configuration.getExcludedComponentPackages() == null ? null : Arrays.asList(configuration.getExcludedComponentPackages().split(" "));
		}

		@Override
		public List<String> getExcludedServicePackages()
		{
			return configuration.getExcludedServicePackages() == null ? null : Arrays.asList(configuration.getExcludedServicePackages().split(" "));
		}

		@Override
		public String getDefaultAdminUser()
		{
			return configuration.getDefaultAdminUser();
		}

		@Override
		public String getDefaultAdminPassword()
		{
			return configuration.getDefaultAdminPassword();
		}

		@Override
		public boolean isMinimizeJsCssResources()
		{
			return configuration.isMinimizeJsCssResources();
		}

		@Override
		public Collection<License> getLicenses()
		{
			return configuration.getLicenses().values();
		}
	}

	@Override
	protected WarArgumentChest createArgumentChest(IApplicationContext context)
	{
		return new WarArgumentChest((String[])context.getArguments().get(IApplicationContext.APPLICATION_ARGS));
	}

	@Override
	protected void exportActiveSolution(final WarArgumentChest configuration)
	{
		SpecProviderState componentsSpecProviderState = WebComponentSpecProvider.getSpecProviderState();
		SpecProviderState servicesSpecProviderState = WebServiceSpecProvider.getSpecProviderState();
		WarExporter warExporter = new WarExporter(new CommandLineWarExportModel(configuration, componentsSpecProviderState, servicesSpecProviderState), componentsSpecProviderState,
			servicesSpecProviderState);
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
}
