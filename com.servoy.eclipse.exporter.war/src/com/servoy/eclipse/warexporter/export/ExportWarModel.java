/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
package com.servoy.eclipse.warexporter.export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.jface.dialogs.IDialogSettings;

import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel;
import com.servoy.eclipse.model.war.exporter.ServerConfiguration;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.xmlxport.IXMLImportUserChannel;

/**
 * Model holding all the data for the export.
 *
 * @author jcompagner
 * @since 6.1
 */
public class ExportWarModel extends AbstractWarExportModel
{

	private String fileName;
	private final List<String> plugins = new ArrayList<String>();
	private final List<String> beans = new ArrayList<String>();
	private final List<String> lafs = new ArrayList<String>();
	private final List<String> drivers = new ArrayList<String>();
	private final TreeMap<String, ServerConfiguration> servers = new TreeMap<String, ServerConfiguration>();
	private final SortedSet<String> selectedServerNames = new TreeSet<String>();
	private String servoyPropertiesFileName;
	private String startRMIPort = "1099";
	private boolean startRMI = true;
	private boolean exportActiveSolution;
	private boolean overwriteSocketFactoryProperties;
	private final List<String> pluginLocations;
	private boolean exportAllTablesFromReferencedServers;
	private boolean exportI18NData;
	private int sampleRows = 5000;
	private boolean exportSampleData;
	private boolean checkMetadataTables;
	private boolean exportMetaData;
	private boolean usingDbiFileInfoOnly;
	private boolean allRows;
	private String warFileName;
	private boolean allowDataModelChanges = true;
	private boolean allowSQLKeywords;
	private boolean updateSequences;
	private boolean overrideSequenceTypes;
	private boolean overrideDefaultValues;
	private boolean insertNewI18NKeysOnly;
	private boolean overwriteGroups;
	private boolean addUsersToAdminGroup;
	private int importUserPolicy;
	private Set<String> exportedComponents;
	private Set<String> exportedServices;
	private boolean upgradeRepository;
	private boolean createTomcatContextXML;
	private boolean antiResourceLocking;
	private boolean clearReferencesStatic;
	private boolean clearReferencesStopThreads;
	private boolean clearReferencesStopTimerThreads;
	private String defaultAdminUser;
	private String defaultAdminPassword;

	/**
	 * @param dialogSettings
	 */
	public ExportWarModel(IDialogSettings settings)
	{
		warFileName = settings.get("export.warfilename");
		servoyPropertiesFileName = settings.get("export.servoyPropertiesFileName");
		exportActiveSolution = Utils.getAsBoolean(settings.get("export.exportActiveSolution"));
		if (settings.get("export.startRMIPort") != null) startRMIPort = settings.get("export.startRMIPort");
		if (settings.get("export.startRMI") != null) startRMI = Utils.getAsBoolean(settings.get("export.startRMI"));
		exportAllTablesFromReferencedServers = Utils.getAsBoolean(settings.get("export.exportAllTablesFromReferencedServers"));
		importUserPolicy = Utils.getAsInteger(settings.get("export.importUserPolicy"));
		exportI18NData = Utils.getAsBoolean(settings.get("export.exportI18NData"));
		exportSampleData = Utils.getAsBoolean(settings.get("export.exportSampleData"));
		if (settings.get("export.sampleRows") != null) sampleRows = Utils.getAsInteger(settings.get("export.sampleRows"));
		allRows = Utils.getAsBoolean(settings.get("export.allRows"));
		checkMetadataTables = Utils.getAsBoolean(settings.get("export.checkMetadataTables"));
		exportMetaData = Utils.getAsBoolean(settings.get("export.exportMetaData"));
		usingDbiFileInfoOnly = Utils.getAsBoolean(settings.get("export.usingDbiFileInfoOnly"));
		if (settings.get("export.allowDataModelChanges") != null) allowDataModelChanges = Utils.getAsBoolean(settings.get("export.allowDataModelChanges"));
		allowSQLKeywords = Utils.getAsBoolean(settings.get("export.allowSQLKeywords"));
		updateSequences = Utils.getAsBoolean(settings.get("export.updateSequences"));
		overrideDefaultValues = Utils.getAsBoolean(settings.get("export.overrideDefaultValues"));
		overrideSequenceTypes = Utils.getAsBoolean(settings.get("export.overrideSequenceTypes"));
		insertNewI18NKeysOnly = Utils.getAsBoolean(settings.get("export.insertNewI18NKeysOnly"));
		overwriteGroups = Utils.getAsBoolean(settings.get("export.overwriteGroups"));
		addUsersToAdminGroup = Utils.getAsBoolean(settings.get("export.addUsersToAdminGroup"));
		upgradeRepository = Utils.getAsBoolean(settings.get("export.upgradeRepository"));

		createTomcatContextXML = Utils.getAsBoolean(settings.get("export.tomcat.createTomcatContextXML"));
		antiResourceLocking = Utils.getAsBoolean(settings.get("export.tomcat.antiResourceLocking"));
		clearReferencesStatic = Utils.getAsBoolean(settings.get("export.tomcat.clearReferencesStatic"));
		clearReferencesStopThreads = Utils.getAsBoolean(settings.get("export.tomcat.clearReferencesStopThreads"));
		clearReferencesStopTimerThreads = Utils.getAsBoolean(settings.get("export.tomcat.clearReferencesStopTimerThreads"));

		if (settings.getArray("export.components") != null)
		{
			exportedComponents = new TreeSet<String>(Arrays.asList(settings.getArray("export.components")));
		}
		if (settings.getArray("export.services") != null)
		{
			exportedServices = new TreeSet<String>(Arrays.asList(settings.getArray("export.services")));
		}
		pluginLocations = new ArrayList<String>();
		String[] array = settings.getArray("plugin.locations");
		if (array != null && array.length > 1)
		{
			for (String loc : array)
			{
				pluginLocations.add(loc);
			}
		}
		else
		{
			pluginLocations.add("plugins/");
		}

		if (settings.get("export.plugins") != null)
		{
			StringTokenizer st = new StringTokenizer(settings.get("export.plugins"), ";");
			while (st.hasMoreTokens())
			{
				plugins.add(st.nextToken());
			}
		}

		if (settings.get("export.beans") != null)
		{
			StringTokenizer st = new StringTokenizer(settings.get("export.beans"), ";");
			while (st.hasMoreTokens())
			{
				beans.add(st.nextToken());
			}
		}

		if (settings.get("export.lafs") != null)
		{
			StringTokenizer st = new StringTokenizer(settings.get("export.lafs"), ";");
			while (st.hasMoreTokens())
			{
				lafs.add(st.nextToken());
			}
		}

		if (settings.get("export.drivers") != null)
		{
			StringTokenizer st = new StringTokenizer(settings.get("export.drivers"), ";");
			while (st.hasMoreTokens())
			{
				drivers.add(st.nextToken());
			}
		}

		if (settings.get("export.servers") != null)
		{
			StringTokenizer st = new StringTokenizer(settings.get("export.servers"), ";");
			while (st.hasMoreTokens())
			{
				String name = st.nextToken();
				ServerConfiguration sc = getServerConfiguration(name);
				if (sc != null)
				{
					selectedServerNames.add(name);
					sc.setCatalog(settings.get("export.servers." + name + ".catalog"));
					sc.setDataModelCloneFrom(settings.get("export.servers." + name + ".clone"));
					sc.setDriver(settings.get("export.servers." + name + ".driver"));
					sc.setPassword(settings.get("export.servers." + name + ".password"));
					sc.setSchema(settings.get("export.servers." + name + ".schema"));
					sc.setServerUrl(settings.get("export.servers." + name + ".serverurl"));
					sc.setUserName(settings.get("export.servers." + name + ".username"));
					sc.setValidationQuery(settings.get("export.servers." + name + ".validationquery"));
					sc.setConnectionValidationType(Utils.getAsInteger(settings.get("export.servers." + name + ".validationtype")));
					sc.setMaxActive(Utils.getAsInteger(settings.get("export.servers." + name + ".maxactive")));
					sc.setMaxIdle(Utils.getAsInteger(settings.get("export.servers." + name + ".maxidle")));
					sc.setMaxPreparedStatementsIdle(Utils.getAsInteger(settings.get("export.servers." + name + ".maxstatements")));
					sc.setSkipSysTables(Utils.getAsBoolean(settings.get("export.servers." + name + ".skipsystables")));
				}
			}
		}
		overwriteSocketFactoryProperties = false;
	}

	public void saveSettings(IDialogSettings settings)
	{
		settings.put("export.warfilename", warFileName);
		settings.put("export.exportActiveSolution", exportActiveSolution);
		settings.put("export.servoyPropertiesFileName", servoyPropertiesFileName);

		settings.put("export.startRMIPort", startRMIPort);
		settings.put("export.startRMI", startRMI);
		settings.put("export.exportAllTablesFromReferencedServers", exportAllTablesFromReferencedServers);
		settings.put("export.importUserPolicy", importUserPolicy);
		settings.put("export.exportI18NData", exportI18NData);
		settings.put("export.exportSampleData", exportSampleData);
		settings.put("export.checkMetadataTables", checkMetadataTables);
		settings.put("export.exportMetaData", exportMetaData);
		settings.put("export.sampleRows", sampleRows);
		settings.put("export.allRows", allRows);
		settings.put("export.usingDbiFileInfoOnly", usingDbiFileInfoOnly);
		settings.put("export.allowDataModelChanges", allowDataModelChanges);
		settings.put("export.allowSQLKeywords", allowSQLKeywords);
		settings.put("export.updateSequences", updateSequences);
		settings.put("export.overrideDefaultValues", overrideDefaultValues);
		settings.put("export.overrideSequenceTypes", overrideSequenceTypes);
		settings.put("export.insertNewI18NKeysOnly", insertNewI18NKeysOnly);
		settings.put("export.overwriteGroups", overwriteGroups);
		settings.put("export.addUsersToAdminGroup", addUsersToAdminGroup);
		settings.put("export.upgradeRepository", upgradeRepository);
		settings.put("export.tomcat.createTomcatContextXML", createTomcatContextXML);
		settings.put("export.tomcat.antiResourceLocking", antiResourceLocking);
		settings.put("export.tomcat.clearReferencesStatic", clearReferencesStatic);
		settings.put("export.tomcat.clearReferencesStopThreads", clearReferencesStopThreads);
		settings.put("export.tomcat.clearReferencesStopTimerThreads", clearReferencesStopTimerThreads);


		if (exportedComponents != null) settings.put("export.components", exportedComponents.toArray(new String[exportedComponents.size()]));
		if (exportedServices != null) settings.put("export.services", exportedServices.toArray(new String[exportedServices.size()]));

		if (pluginLocations.size() > 1)
		{
			settings.put("plugin.locations", pluginLocations.toArray(new String[pluginLocations.size()]));
		}


		if (plugins.size() > 0)
		{
			StringBuilder sb = new StringBuilder(128);
			for (String plugin : plugins)
			{
				sb.append(plugin);
				sb.append(';');
			}
			settings.put("export.plugins", sb.toString());
		}
		else settings.put("export.plugins", "");
		if (beans.size() > 0)
		{
			StringBuilder sb = new StringBuilder(128);
			for (String plugin : beans)
			{
				sb.append(plugin);
				sb.append(';');
			}
			settings.put("export.beans", sb.toString());
		}
		else settings.put("export.beans", "");

		if (lafs.size() > 0)
		{
			StringBuilder sb = new StringBuilder(128);
			for (String plugin : lafs)
			{
				sb.append(plugin);
				sb.append(';');
			}
			settings.put("export.lafs", sb.toString());
		}
		else settings.put("export.lafs", "");

		if (drivers.size() > 0)
		{
			StringBuilder sb = new StringBuilder(128);
			for (String plugin : drivers)
			{
				sb.append(plugin);
				sb.append(';');
			}
			settings.put("export.drivers", sb.toString());
		}
		else settings.put("export.drivers", "");

		if (servers.size() > 0)
		{
			StringBuilder sb = new StringBuilder(128);
			for (String name : selectedServerNames)
			{
				sb.append(name);
				sb.append(';');
				ServerConfiguration sc = getServerConfiguration(name);
				if (sc.getCatalog() != null) settings.put("export.servers." + name + ".catalog", sc.getCatalog());
				settings.put("export.servers." + name + ".clone", sc.getDataModelCloneFrom());
				settings.put("export.servers." + name + ".driver", sc.getDriver());
				settings.put("export.servers." + name + ".password", sc.getPassword());
				if (sc.getSchema() != null) settings.put("export.servers." + name + ".schema", sc.getSchema());
				settings.put("export.servers." + name + ".serverurl", sc.getServerUrl());
				settings.put("export.servers." + name + ".username", sc.getUserName());
				settings.put("export.servers." + name + ".validationquery", sc.getValidationQuery());
				settings.put("export.servers." + name + ".validationtype", sc.getConnectionValidationType());
				settings.put("export.servers." + name + ".maxactive", sc.getMaxActive());
				settings.put("export.servers." + name + ".maxidle", sc.getMaxIdle());
				settings.put("export.servers." + name + ".maxstatements", sc.getMaxPreparedStatementsIdle());
				settings.put("export.servers." + name + ".skipsystables", sc.isSkipSysTables());
			}
			settings.put("export.servers", sb.toString());
		}
	}

	public String getFileName()
	{
		return fileName;
	}

	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	/**
	 * @return the warFileName
	 */
	public String getWarFileName()
	{
		return warFileName;
	}

	public void setWarFileName(String warFileName)
	{
		this.warFileName = warFileName;
	}


	/**
	 * @return
	 */
	public String getServoyPropertiesFileName()
	{
		return servoyPropertiesFileName;
	}

	/**
	 * @param servoyPropertiesFileName the servoyPropertiesFileName to set
	 */
	public void setServoyPropertiesFileName(String servoyPropertiesFileName)
	{
		this.servoyPropertiesFileName = servoyPropertiesFileName;
		if (this.servoyPropertiesFileName != null && this.servoyPropertiesFileName.trim().length() == 0)
		{
			this.servoyPropertiesFileName = null;
		}
	}


	/**
	 * @return the exportActiveSolution
	 */
	public boolean isExportActiveSolution()
	{
		return exportActiveSolution;
	}

	/**
	 * @param exportActiveSolution the exportActiveSolution to set
	 */
	public void setExportActiveSolution(boolean exportActiveSolution)
	{
		this.exportActiveSolution = exportActiveSolution;
	}

	/**
	 * @return the plugins
	 */
	public List<String> getPlugins()
	{
		return plugins;
	}

	/**
	 * @return the beans
	 */
	public List<String> getBeans()
	{
		return beans;
	}

	/**
	 * @return
	 */
	public List<String> getLafs()
	{
		return lafs;
	}

	/**
	 * @return
	 */
	public List<String> getDrivers()
	{
		return drivers;
	}

	/**
	 * @return
	 */
	public SortedSet<String> getSelectedServerNames()
	{
		return selectedServerNames;
	}

	/**
	 * @param serverName
	 * @return
	 */
	public ServerConfiguration getServerConfiguration(String serverName)
	{
		ServerConfiguration serverConfiguration = servers.get(serverName);
		if (serverConfiguration == null)
		{
			IServerInternal server = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(serverName);
			if (server != null)
			{
				serverConfiguration = new ServerConfiguration(serverName, server.getConfig());
				servers.put(serverName, serverConfiguration);
			}
			else if (serverName.equals(IServer.REPOSITORY_SERVER))
			{
				serverConfiguration = new ServerConfiguration(serverName);
				servers.put(serverName, serverConfiguration);
			}
		}
		return serverConfiguration;
	}

	public String getStartRMIPort()
	{
		return startRMIPort;
	}

	public void setStartRMIPort(String port)
	{
		startRMIPort = port;
	}

	public boolean getStartRMI()
	{
		return startRMI;
	}

	public void setStartRMI(boolean startRMI)
	{
		this.startRMI = startRMI;
	}

	public void setOverwriteSocketFactoryProperties(boolean overwriteProperties)
	{
		this.overwriteSocketFactoryProperties = overwriteProperties;
	}

	public boolean allowOverwriteSocketFactoryProperties()
	{
		return overwriteSocketFactoryProperties;
	}

	@Override
	public String getServoyApplicationServerDir()
	{
		return ApplicationServerRegistry.get().getServoyApplicationServerDirectory();
	}

	/**
	 * @param chosenFileName
	 */
	public void addPluginLocations(String pluginsDir)
	{
		pluginLocations.add(pluginsDir);
	}

	@Override
	public List<String> getPluginLocations()
	{
		return pluginLocations;
	}

	/**
	 * @return
	 */
	public boolean isExportAllTablesFromReferencedServers()
	{
		return exportAllTablesFromReferencedServers;
	}

	/**
	 * @param exportAllTablesFromReferencedServers the exportAllTablesFromReferencedServers to set
	 */
	public void setExportAllTablesFromReferencedServers(boolean exportAllTablesFromReferencedServers)
	{
		this.exportAllTablesFromReferencedServers = exportAllTablesFromReferencedServers;
	}

	/**
	 * @return
	 */
	public boolean isExportMetaData()
	{
		return exportMetaData;
	}

	/**
	 * @param exportMetaData the exportMetaData to set
	 */
	public void setExportMetaData(boolean exportMetaData)
	{
		this.exportMetaData = exportMetaData;
	}

	/**
	 * @return
	 */
	public boolean isCheckMetadataTables()
	{
		return checkMetadataTables;
	}

	/**
	 * @param checkMetadataTables the checkMetadataTables to set
	 */
	public void setCheckMetadataTables(boolean checkMetadataTables)
	{
		this.checkMetadataTables = checkMetadataTables;
	}

	/**
	 * @return
	 */
	public boolean isExportSampleData()
	{
		return exportSampleData;
	}

	/**
	 * @param exportSampleData the exportSampleData to set
	 */
	public void setExportSampleData(boolean exportSampleData)
	{
		this.exportSampleData = exportSampleData;
	}


	/**
	 * @param maxRowToRetrieve
	 */
	public void setNumberOfSampleDataExported(int sampleRows)
	{
		this.sampleRows = sampleRows;
	}

	/**
	 * @return the sampleRows
	 */
	public int getNumberOfSampleDataExported()
	{
		return sampleRows;
	}

	/**
	 * @return
	 */
	public boolean isExportI18NData()
	{
		return exportI18NData;
	}

	/**
	 * @param exportI18NData the exportI18NData to set
	 */
	public void setExportI18NData(boolean exportI18NData)
	{
		this.exportI18NData = exportI18NData;
	}

	/**
	 * @return
	 */
	public boolean isExportUsingDbiFileInfoOnly()
	{
		return usingDbiFileInfoOnly;
	}

	/**
	 * @param usingDbiFileInfoOnly the usingDbiFileInfoOnly to set
	 */
	public void setExportUsingDbiFileInfoOnly(boolean usingDbiFileInfoOnly)
	{
		this.usingDbiFileInfoOnly = usingDbiFileInfoOnly;
	}

	/**
	 * @return
	 */
	public boolean isAllRows()
	{
		return allRows;
	}

	/**
	 * @param allRows the allRows to set
	 */
	public void setAllRows(boolean allRows)
	{
		this.allRows = allRows;
	}

	/**
	 * @return
	 */
	public boolean isAllowDataModelChanges()
	{
		return allowDataModelChanges;
	}

	/**
	 * @param selection
	 */
	public void setAllowDataModelChanges(boolean allowDataModelChanges)
	{
		this.allowDataModelChanges = allowDataModelChanges;
	}

	/**
	 * @return
	 */
	public boolean isAllowSQLKeywords()
	{
		return allowSQLKeywords;
	}

	/**
	 * @param allowKeywords the allowKeywords to set
	 */
	public void setAllowSQLKeywords(boolean allowSQLKeywords)
	{
		this.allowSQLKeywords = allowSQLKeywords;
	}

	/**
	 * @return
	 */
	public boolean isUpdateSequences()
	{
		return updateSequences;
	}

	/**
	 * @param selection
	 */
	public void setUpdateSequences(boolean updateSequences)
	{
		this.updateSequences = updateSequences;
	}

	/**
	 * @return
	 */
	public boolean isOverrideSequenceTypes()
	{
		return overrideSequenceTypes;
	}

	/**
	 * @param overrideSequenceTypes the overrideSequenceTypes to set
	 */
	public void setOverrideSequenceTypes(boolean overrideSequenceTypes)
	{
		this.overrideSequenceTypes = overrideSequenceTypes;
	}

	/**
	 * @return
	 */
	public boolean isOverrideDefaultValues()
	{
		return overrideDefaultValues;
	}

	/**
	 * @param overrideDefaultValues the overrideDefaultValues to set
	 */
	public void setOverrideDefaultValues(boolean overrideDefaultValues)
	{
		this.overrideDefaultValues = overrideDefaultValues;
	}

	/**
	 * @return
	 */
	public boolean isInsertNewI18NKeysOnly()
	{
		return insertNewI18NKeysOnly;
	}

	/**
	 * @param insertNewI18NKeysOnly the insertNewI18NKeysOnly to set
	 */
	public void setInsertNewI18NKeysOnly(boolean insertNewI18NKeysOnly)
	{
		this.insertNewI18NKeysOnly = insertNewI18NKeysOnly;
	}

	/**
	 * @return the overwriteGroups
	 */
	public boolean isOverwriteGroups()
	{
		return overwriteGroups;
	}

	/**
	 * @param selection
	 */
	public void setOverwriteGroups(boolean overwriteGroups)
	{
		this.overwriteGroups = overwriteGroups;
	}

	/**
	 * @return
	 */
	public boolean isAddUsersToAdminGroup()
	{
		return addUsersToAdminGroup;
	}

	/**
	 * @param addUsersToAdminGroup the addUsersToAdminGroup to set
	 */
	public void setAddUsersToAdminGroup(boolean addUsersToAdminGroup)
	{
		this.addUsersToAdminGroup = addUsersToAdminGroup;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.wizards.IExportSolutionModel#isExportUsers()
	 */
	@Override
	public boolean isExportUsers()
	{
		return importUserPolicy > IXMLImportUserChannel.IMPORT_USER_POLICY_DONT;
	}


	/**
	 * @return
	 */
	public int getImportUserPolicy()
	{
		return importUserPolicy;
	}

	/**
	 * @param createNoneExistingUsers the createNoneExistingUsers to set
	 */
	public void setImportUserPolicy(int importUserPolicy)
	{
		this.importUserPolicy = importUserPolicy;
	}

	/**
	 * @param selectedComponents
	 */
	public void setExportedComponents(Set<String> selectedComponents)
	{
		this.exportedComponents = selectedComponents;
	}

	/**
	 * @param selectedServices
	 */
	public void setExportedServices(Set<String> selectedServices)
	{
		this.exportedServices = selectedServices;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.model.war.exporter.IWarExportModel#getExportedComponents()
	 */
	@Override
	public Set<String> getExportedComponents()
	{
		return exportedComponents;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.model.war.exporter.IWarExportModel#getExportedServices()
	 */
	@Override
	public Set<String> getExportedServices()
	{
		return exportedServices;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.model.war.exporter.AbstractWarExportModel#setAutomaticallyUpgradeRepository()
	 */
	public void setAutomaticallyUpgradeRepository()
	{
		upgradeRepository = true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.model.war.exporter.IWarExportModel#isAutomaticallyUpgradeRepository()
	 */
	@Override
	public boolean isAutomaticallyUpgradeRepository()
	{
		return upgradeRepository;
	}

	/**
	 * @return the createTomcatContextXML
	 */
	public boolean isCreateTomcatContextXML()
	{
		return createTomcatContextXML;
	}

	/**
	 * @param createTomcatContextXML the createTomcatContextXML to set
	 */
	public void setCreateTomcatContextXML(boolean createTomcatContextXML)
	{
		this.createTomcatContextXML = createTomcatContextXML;
	}

	/**
	 * @return the clearReferencesStatic
	 */
	public boolean isClearReferencesStatic()
	{
		return clearReferencesStatic;
	}

	/**
	 * @param clearReferencesStatic the clearReferencesStatic to set
	 */
	public void setClearReferencesStatic(boolean clearReferencesStatic)
	{
		this.clearReferencesStatic = clearReferencesStatic;
	}

	/**
	 * @return the clearReferencesStopThreads
	 */
	public boolean isClearReferencesStopThreads()
	{
		return clearReferencesStopThreads;
	}

	/**
	 * @param clearReferencesStopThreads the clearReferencesStopThreads to set
	 */
	public void setClearReferencesStopThreads(boolean clearReferencesStopThreads)
	{
		this.clearReferencesStopThreads = clearReferencesStopThreads;
	}

	/**
	 * @return the clearReferencesStopTimerThreads
	 */
	public boolean isClearReferencesStopTimerThreads()
	{
		return clearReferencesStopTimerThreads;
	}

	/**
	 * @param clearReferencesStopTimerThreads the clearReferencesStopTimerThreads to set
	 */
	public void setClearReferencesStopTimerThreads(boolean clearReferencesStopTimerThreads)
	{
		this.clearReferencesStopTimerThreads = clearReferencesStopTimerThreads;
	}

	/**
	 * @return the clearReferencesStopTimerThreads
	 */
	public boolean isAntiResourceLocking()
	{
		return antiResourceLocking;
	}

	/**
	 * @param clearReferencesStopTimerThreads the clearReferencesStopTimerThreads to set
	 */
	public void setAntiResourceLocking(boolean antiResourceLocking)
	{
		this.antiResourceLocking = antiResourceLocking;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.model.war.exporter.IWarExportModel#getExcludedComponentPackages()
	 */
	@Override
	public List<String> getExcludedComponentPackages()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.model.war.exporter.IWarExportModel#getExcludedServicePackages()
	 */
	@Override
	public List<String> getExcludedServicePackages()
	{
		return null;
	}

	/**
	 * @return the defaultAdminUser
	 */
	public String getDefaultAdminUser()
	{
		return defaultAdminUser;
	}

	/**
	 * @param defaultAdminPassword the defaultAdminPassword to set
	 */
	public void setDefaultAdminPassword(String defaultAdminPassword)
	{
		this.defaultAdminPassword = defaultAdminPassword;
	}

	/**
	 * @return the defaultAdminPassword
	 */
	public String getDefaultAdminPassword()
	{
		return defaultAdminPassword;
	}

	/**
	 * @param defaultAdminUser the defaultAdminUser to set
	 */
	public void setDefaultAdminUser(String defaultAdminUser)
	{
		this.defaultAdminUser = defaultAdminUser;
	}
}
