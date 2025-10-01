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
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.app.IApplicationContext;

import com.servoy.eclipse.exporter.apps.common.AbstractWorkspaceExporter;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.export.IExportSolutionModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.AbstractEclipseExportUserChannel;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel;
import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel.License;
import com.servoy.eclipse.model.war.exporter.ExportException;
import com.servoy.eclipse.model.war.exporter.ServerConfiguration;
import com.servoy.eclipse.model.war.exporter.WarExporter;
import com.servoy.eclipse.ngclient.ui.Activator;
import com.servoy.eclipse.ngclient.ui.StringOutputStream;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.xmlxport.IXMLExportUserChannel;

/**
 * Eclipse application that can be used for exporting servoy solutions in .war format from command line.
 *
 * @author gboros
 */
public class WarWorkspaceExporter extends AbstractWorkspaceExporter<WarArgumentChest>
{

	public class CommandLineExportProgressMonitor implements IProgressMonitor
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
	}

	public class CommandLineExportUserChannel extends AbstractEclipseExportUserChannel
	{
		public CommandLineExportUserChannel(IExportSolutionModel exportModel, IProgressMonitor monitor)
		{
			super(exportModel, monitor);
		}

		@Override
		public void info(String message, int priority)
		{
			if (priority == ILogLevel.DEBUG)
			{
				outputExtra(message);
			}
			else if (priority < ILogLevel.ERROR)
			{
				output(message);
			}
			else
			{
				outputError(message);
			}
		}

		@Override
		public void displayWarningMessage(String title, String message, boolean scrollableDialog)
		{
			output(title + " " + message);
		}
	}

	private final class CommandLineWarExportModel extends AbstractWarExportModel
	{
		private final WarArgumentChest cmdLineArguments;
		private Set<String> exportedPackages;

		private CommandLineWarExportModel(WarArgumentChest configuration)
		{
			super();
			this.cmdLineArguments = configuration;
			searchForComponentsAndServicesBothDefaultAndInSolution();
		}

		@Override
		public boolean isExportActiveSolution()
		{
			return cmdLineArguments.isExportActiveSolutionOnly();
		}

		@Override
		public String getStartRMIPort()
		{
			return null;
		}

		@Override
		public boolean getStartRMI()
		{
			String servoyPropertiesFileName = getServoyPropertiesFileName();
			if (servoyPropertiesFileName != null)
			{
				try (FileInputStream fis = new FileInputStream(new File(servoyPropertiesFileName)))
				{
					Properties prop = new Properties();
					prop.load(fis);
					return Utils.getAsBoolean(prop.getProperty("servoy.server.start.rmi", "false"));
				}
				catch (IOException e)
				{
					// just ignore and return false
				}
			}
			return false;
		}

		@Override
		public String getServoyPropertiesFileName()
		{
			String warSettingsFileName = cmdLineArguments.getWarSettingsFileName();
			if (warSettingsFileName == null)
			{
				String servoyPropertiesFileName = cmdLineArguments.getSettingsFileName();
				if (servoyPropertiesFileName == null)
				{
					servoyPropertiesFileName = getServoyApplicationServerDir() + File.separator + "servoy.properties";
				}
				return servoyPropertiesFileName;
			}
			return warSettingsFileName;
		}

		@Override
		public String getWebXMLFileName()
		{
			return cmdLineArguments.getWebXMLFileName();
		}

		@Override
		public String getLog4jConfigurationFile()
		{
			return cmdLineArguments.getLog4jConfigurationFile();
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
		public boolean isExportNonActiveSolutions()
		{
			return cmdLineArguments.getNoneActiveSolutions() != null;
		}

		public List<String> getNonActiveSolutions()
		{
			if (cmdLineArguments.getNoneActiveSolutions() != null)
			{
				return Arrays.asList(cmdLineArguments.getNoneActiveSolutions().split(" "));
			}
			return Collections.emptyList();
		}

		@Override
		public List<String> getPlugins()
		{
			return getFilteredFileNames(ApplicationServerRegistry.get().getPluginManager().getPluginsDir(), cmdLineArguments.getExcludedPlugins(),
				cmdLineArguments.getPlugins());
		}

		@Override
		public String getWarFileName()
		{
			String warFileName = cmdLineArguments.getWarFileName();
			if (warFileName == null)
			{
				ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
				warFileName = activeProject.getProject().getName();
			}
			if (!warFileName.endsWith(".war")) warFileName += ".war";
			return cmdLineArguments.getExportFilePath() + File.separator + warFileName;
		}

		@Override
		public List<String> getDrivers()
		{
			return getFilteredFileNames(ApplicationServerRegistry.get().getServerManager().getDriversDir(), cmdLineArguments.getExcludedDrivers(),
				cmdLineArguments.getDrivers());
		}

		@Override
		public boolean allowOverwriteSocketFactoryProperties()
		{
			return false;
		}

		@Override
		public String getServoyApplicationServerDir()
		{
			return cmdLineArguments.getAppServerDir();
		}

		List<String> getFilteredFileNames(File folder, String excluded, String included)
		{
			Set<String> names = null;
			if (excluded != null)
			{
				//if <none> it will not match anything and return all files
				return getFiles(folder, new HashSet<String>(Arrays.asList(excluded.toLowerCase().split(" "))), true);
			}
			if (included != null)
			{
				if ("<none>".equals(included.toLowerCase()))
				{
					return Collections.emptyList();
				}
				names = new HashSet<String>(Arrays.asList(included.toLowerCase().split(" ")));
			}
			return getFiles(folder, names, false);
		}

		List<String> getFiles(File dir, final Set<String> fileNames, boolean exclude)
		{
			String[] list = dir.list(new FilenameFilter()
			{
				public boolean accept(File d, String name)
				{
					boolean accept = fileNames != null ? (exclude ? !fileNames.contains(name.toLowerCase()) : fileNames.contains(name.toLowerCase())) : true;

					return accept &&
						(name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".zip") || (new File(d.getPath(), name).isDirectory()));
				}
			});
			if (list == null || list.length == 0) return Collections.emptyList();
			return Arrays.asList(list);
		}

		@Override
		public List<String> getPluginLocations()
		{
			return Arrays.asList(cmdLineArguments.getPluginLocations().split(" "));
		}

		@Override
		public Set<String> getAllExportedComponents()
		{
			if (cmdLineArguments.getExcludedComponentPackages() != null)
			{
				Set<String> internallyNeededComponents = getComponentsNeededUnderTheHood();
				List<String> excludedPackages = Arrays.asList(cmdLineArguments.getExcludedComponentPackages().split(" "));
				return Arrays.stream(componentsSpecProviderState.getAllWebObjectSpecifications())
					.filter(spec -> (internallyNeededComponents.contains(spec.getName()) || !excludedPackages.contains(spec.getPackageName())))
					.map(spec -> spec.getName())
					.collect(Collectors.toSet());
			}
			else
			{
				if (cmdLineArguments.getSelectedComponents() == null || cmdLineArguments.getSelectedComponents().equals(""))
				{
					// auto-export components explicitly used by solution and under-the-hood-components
					Set<String> defaultExportedComponents = new HashSet<>(getComponentsUsedExplicitlyBySolution());
					defaultExportedComponents.addAll(getComponentsNeededUnderTheHood());
					return defaultExportedComponents;
				}

				Set<String> set = new HashSet<String>();
				if (cmdLineArguments.getSelectedComponents().trim().equalsIgnoreCase("all"))
				{
					Arrays.stream(componentsSpecProviderState.getAllWebObjectSpecifications()).map(spec -> spec.getName()).forEach(set::add);
				}
				else
				{
					set.addAll(Arrays.asList(cmdLineArguments.getSelectedComponents().split(" ")));
					for (String componentName : set)
					{
						if (componentsSpecProviderState.getWebObjectSpecification(componentName) == null)
						{
							// TODO shouldn't this be an error and shouldn't the exporter fail nicely with a new exit code? I'm thinking now of Jenkins usage and failing sooner rather then later (at export rather then when testing)
							output("'" + componentName + "' is not a valid component name or it could not be found. Ignoring.");
							set.remove(componentName);
						}
					}
					set.addAll(getComponentsUsedExplicitlyBySolution());
					set.addAll(getComponentsNeededUnderTheHood());
				}
				return set;
			}
		}

		@Override
		public Set<String> getAllExportedServicesWithoutSabloServices()
		{
			if (cmdLineArguments.getExcludedServicePackages() != null)
			{
				Set<String> internallyNeededServices = getServicesNeededUnderTheHoodWithoutSabloServices();
				List<String> excludedPackages = Arrays.asList(cmdLineArguments.getExcludedServicePackages().split(" "));
				return Arrays.stream(servicesSpecProviderState.getAllWebObjectSpecifications()) //
					.filter(spec -> (internallyNeededServices.contains(spec.getName()) ||
						(!excludedPackages.contains(spec.getPackageName()) && !"sablo".equals(spec.getPackageName()))))
					.map(spec -> spec.getName()).collect(Collectors.toSet());
			}
			else
			{
				if (cmdLineArguments.getSelectedServices() == null || cmdLineArguments.getSelectedServices().equals(""))
				{
					// auto-export services explicitly used by solution and under-the-hood-services
					Set<String> defaultExportedServices = new HashSet<>(getServicesUsedExplicitlyBySolution());
					defaultExportedServices.addAll(getServicesNeededUnderTheHoodWithoutSabloServices());
					return defaultExportedServices;
				}

				Set<String> set = new HashSet<String>();
				if (cmdLineArguments.getSelectedServices().trim().equalsIgnoreCase("all"))
				{
					Arrays.stream(servicesSpecProviderState.getAllWebObjectSpecifications()).filter(spec -> !"sablo".equals(spec.getPackageName()))
						.map(spec -> spec.getName()).forEach(set::add);
				}
				else
				{
					set.addAll(Arrays.asList(cmdLineArguments.getSelectedServices().split(" ")));
					for (String serviceName : set)
					{
						if (servicesSpecProviderState.getWebObjectSpecification(serviceName) == null)
						{
							// TODO shouldn't this be an error and shouldn't the exporter fail nicely with a new exit code? I'm thinking now of Jenkins usage and failing sooner rather then later (at export rather then when testing)
							output("'" + serviceName + "' is not a valid service name or it could not be found. Ignoring.");
							set.remove(serviceName);
						}
					}
					set.addAll(getServicesUsedExplicitlyBySolution());
					set.addAll(getServicesNeededUnderTheHoodWithoutSabloServices());
				}
				return set;
			}
		}

		@Override
		public String getFileName()
		{
			return null;
		}

		@Override
		public String exportNG2Mode()
		{
			return cmdLineArguments.exportNG2Mode();
		}

		@Override
		public boolean isExportMetaData()
		{
			return cmdLineArguments.shouldExportMetadata();
		}

		@Override
		public boolean isExportSampleData()
		{
			return cmdLineArguments.isExportSampleData();
		}

		@Override
		public boolean isExportI18NData()
		{
			return cmdLineArguments.isExportI18NData();
		}

		@Override
		public int getNumberOfSampleDataExported()
		{
			return cmdLineArguments.getNumberOfSampleDataExported();
		}

		@Override
		public boolean isExportAllTablesFromReferencedServers()
		{
			return cmdLineArguments.isExportAllTablesFromReferencedServers();
		}

		@Override
		public boolean isCheckMetadataTables()
		{
			return cmdLineArguments.checkMetadataTables();
		}

		@Override
		public boolean isExportUsingDbiFileInfoOnly()
		{
			return cmdLineArguments.shouldExportUsingDbiFileInfoOnly();
		}

		@Override
		public boolean isExportUsers()
		{
			return cmdLineArguments.exportUsers();
		}

		@Override
		public boolean isOverwriteGroups()
		{
			return cmdLineArguments.isOverwriteGroups();
		}

		@Override
		public boolean isAllowSQLKeywords()
		{
			return cmdLineArguments.isAllowSQLKeywords();
		}

		@Override
		public boolean isOverrideSequenceTypes()
		{
			return cmdLineArguments.isOverrideSequenceTypes();
		}

		@Override
		public boolean isInsertNewI18NKeysOnly()
		{
			return cmdLineArguments.isInsertNewI18NKeysOnly();
		}

		@Override
		public boolean isOverrideDefaultValues()
		{
			return cmdLineArguments.isOverrideDefaultValues();
		}

		@Override
		public int getImportUserPolicy()
		{
			return cmdLineArguments.getImportUserPolicy();
		}

		@Override
		public boolean isAddUsersToAdminGroup()
		{
			return cmdLineArguments.isAddUsersToAdminGroup();
		}

		@Override
		public String getAllowDataModelChanges()
		{
			if (cmdLineArguments.getAllowDataModelChanges() != null)
			{
				return cmdLineArguments.getAllowDataModelChanges();
			}
			return Boolean.toString(!cmdLineArguments.isStopOnAllowDataModelChanges());
		}

		@Override
		public boolean isUpdateSequences()
		{
			return cmdLineArguments.isUpdateSequences();
		}

		@Override
		public boolean isAutomaticallyUpgradeRepository()
		{
			return cmdLineArguments.automaticallyUpdateRepository();
		}

		@Override
		public String getTomcatContextXMLFileName()
		{
			return cmdLineArguments.getTomcatContextXMLFileName();
		}

		@Override
		public boolean isCreateTomcatContextXML()
		{
			return cmdLineArguments.isCreateTomcatContextXML();
		}

		@Override
		public boolean isClearReferencesStatic()
		{
			return cmdLineArguments.isClearReferencesStatic();
		}

		@Override
		public boolean isClearReferencesStopThreads()
		{
			return cmdLineArguments.isClearReferencesStopThreads();
		}

		@Override
		public boolean isClearReferencesStopTimerThreads()
		{
			return cmdLineArguments.isClearReferencesStopTimerThreads();
		}

		@Override
		public boolean isAntiResourceLocking()
		{
			return cmdLineArguments.isAntiResourceLocking();
		}

		@Override
		public String getDefaultAdminUser()
		{
			return cmdLineArguments.getDefaultAdminUser();
		}

		@Override
		public String getDefaultAdminPassword()
		{
			return cmdLineArguments.getDefaultAdminPassword();
		}

		@Override
		public boolean isUseAsRealAdminUser()
		{
			return cmdLineArguments.isUseAsRealAdminUser();
		}

		@Override
		public Collection<License> getLicenses()
		{
			return cmdLineArguments.getLicenses().values();
		}

		@Override
		public boolean isOverwriteDeployedDBServerProperties()
		{
			return cmdLineArguments.isOverwriteDeployedDBServerProperties();
		}

		@Override
		public boolean isOverwriteDeployedServoyProperties()
		{
			return cmdLineArguments.isOverwriteDeployedServoyProperties();
		}

		@Override
		public String getUserHome()
		{
			return cmdLineArguments.getUserHome();
		}

		@Override
		public boolean isSkipDatabaseViewsUpdate()
		{
			return cmdLineArguments.skipDatabaseViewsUpdate();
		}

		@Override
		public Set<String> getExportedPackagesExceptSablo()
		{
			if (exportedPackages == null)
			{
				exportedPackages = Stream.of(getAllExportedComponents().stream() //
					.map(comp -> componentsSpecProviderState.getWebObjectSpecification(comp).getPackageName()) //
					.collect(Collectors.toSet()), //
					getAllExportedServicesWithoutSabloServices().stream() //
						.map(comp -> servicesSpecProviderState.getWebObjectSpecification(comp).getPackageName()) //
						.collect(Collectors.toSet())) //
					.flatMap(Set::stream) //
					.collect(Collectors.toSet());
				exportedPackages.addAll(exportedLayoutPackages);
			}
			return exportedPackages;
		}
	}

	@Override
	protected WarArgumentChest createArgumentChest(IApplicationContext context)
	{
		return new WarArgumentChest((String[])context.getArguments().get(IApplicationContext.APPLICATION_ARGS));
	}

	private void checkAndAutoUpgradeLicenses(CommandLineWarExportModel exportModel) throws ExportException
	{
		IApplicationServerSingleton server = ApplicationServerRegistry.get();
		for (License l : exportModel.getLicenses())
		{
			if (!server.checkClientLicense(l.getCompanyKey(), l.getCode(), l.getNumberOfLicenses()))
			{
				//try to auto upgrade
				Pair<Boolean, String> code = server.upgradeLicense(l.getCompanyKey(), l.getCode(), l.getNumberOfLicenses());
				if (code == null || !code.getLeft().booleanValue())
				{
					throw new ExportException("Cannot export! License '" + l.getCompanyKey() + "' with code " + l.getCode() +
						(code != null && !code.getLeft().booleanValue() ? " error: " + code.getRight() : " is not valid."));
				}
				else if (code.getLeft().booleanValue() && !l.getCode().equals(code.getRight()))
				{
					output("License '" + l.getCompanyKey() + "' with code " + l.getCode() + " was auto upgraded to " + code.getRight() +
						". Please change it to the new code in future exports.");
					exportModel.replaceLicenseCode(l, code.getRight());
				}
			}
		}
		String checkFile = exportModel.checkServoyPropertiesFileExists();
		if (checkFile == null)
		{
			final Object[] upgrade = exportModel.checkAndAutoUpgradeLicenses();
			if (upgrade != null && upgrade.length >= 3)
			{
				if (!Utils.getAsBoolean(upgrade[0]))
				{
					throw new ExportException(
						"License code '" + upgrade[1] + "' defined in the selected properties file is invalid." + (upgrade[2] != null ? upgrade[2] : ""));
				}
				else
				{
					output("Could not save changes to the properties file. License code '" + upgrade[1] + "' was auto upgraded to '" + upgrade[2] +
						"'. The export contains the new license code, but the changes could not be written to the selected properties file. Please adjust the '" +
						exportModel.getServoyPropertiesFileName() + "' file manually.");
				}
			}
		}
		else
		{
			throw new ExportException("Error creating the WAR file. " + checkFile);
		}
	}

	@Override
	protected void checkAndExportSolutions(WarArgumentChest configuration)
	{
		if (configuration.exportNG2Mode() != null && !configuration.exportNG2Mode().equals("false"))
		{
			WebPackagesListener.setIgnore(true);
			Activator.getInstance().setConsole(() -> new StringOutputStream()
			{
				@Override
				public void write(CharSequence chars) throws IOException
				{
					outputExtra(chars.toString().trim());
				}

				@Override
				public void close() throws IOException
				{
				}
			});
			Activator.getInstance().extractNode();
		}
		super.checkAndExportSolutions(configuration);
	}

	@Override
	protected void exportActiveSolution(final WarArgumentChest configuration)
	{
		try
		{
			CommandLineWarExportModel exportModel = new CommandLineWarExportModel(configuration);
			if (exportModel.isExportNonActiveSolutions() &&
				(Utils.stringIsEmpty(configuration.getSelectedComponents()) || Utils.stringIsEmpty(configuration.getSelectedServices())))
			{
				output(
					"\nThe arguments contains a non active solution/s, be aware that all the components and services for that solution/s are not exported into the .war file.\nPlease use -help to update your syntax, if you need all those components and services.\n");
			}
			checkAndAutoUpgradeLicenses(exportModel);
			CommandLineExportProgressMonitor monitor = new CommandLineExportProgressMonitor();
			CommandLineExportUserChannel userChannel = new CommandLineExportUserChannel(exportModel, monitor);
			checkMissingPlugins(exportModel, userChannel);
			WarExporter warExporter = new WarExporter(exportModel, userChannel);
			warExporter.doExport(monitor);
		}
		catch (ExportException ex)
		{
			ServoyLog.logError("Failed to export solution.", ex);
			outputError("Exception while exporting solution: " + ex.getMessage() + ".  EXPORT FAILED for this solution. Check workspace log for more info.");
			exitCode = EXIT_EXPORT_FAILED;
		}
	}

	private void checkMissingPlugins(CommandLineWarExportModel exportModel, IXMLExportUserChannel userChannel)
	{
		List<String> plugins = exportModel.getPlugins();
		boolean noConvertorsOrValidators = !plugins.contains("converters.jar") || !plugins.contains("default_validators.jar");
		if (noConvertorsOrValidators)
		{
			// print to system out for the command line exporter.
			userChannel.info("converter.jar or default_validators.jar not exported so column converters or validators don't work", ILogLevel.WARNING);
		}
	}
}
