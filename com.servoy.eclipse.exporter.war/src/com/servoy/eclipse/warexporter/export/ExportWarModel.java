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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.Cipher;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.export.IExportSolutionModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel;
import com.servoy.eclipse.model.war.exporter.IWarExportModel;
import com.servoy.eclipse.model.war.exporter.ServerConfiguration;
import com.servoy.eclipse.ngclient.startup.resourceprovider.ResourceProvider;
import com.servoy.eclipse.warexporter.Activator;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SecuritySupport;
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

	public static IDialogSettings getDialogSettings()
	{
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		IDialogSettings section = activeProject != null
			? DialogSettings.getOrCreateSection(workbenchSettings, "WarExportWizard:" + activeProject.getSolution().getName())
			: DialogSettings.getOrCreateSection(workbenchSettings, "WarExportWizard");
		return section;
	}

	private String fileName;
	private final List<String> plugins = new ArrayList<String>();
	private final List<String> beans = new ArrayList<String>();
	private final List<String> lafs = new ArrayList<String>();
	private final List<String> drivers = new ArrayList<String>();
	private final List<String> noneActiveSolutions = new ArrayList<String>();
	private final TreeMap<String, ServerConfiguration> servers = new TreeMap<String, ServerConfiguration>();
	private final SortedSet<String> selectedServerNames = new TreeSet<String>();
	private String servoyPropertiesFileName;
	private String startRMIPort = "1099";
	private boolean startRMI = false;
	private boolean exportActiveSolution;
	private String exportNG2 = "true";
	private boolean overwriteSocketFactoryProperties;
	private final List<String> pluginLocations;
	private boolean exportAllTablesFromReferencedServers;
	private boolean exportI18NData;
	private int sampleRows = IExportSolutionModel.DEFAULT_NUMBER_OF_SAMPLE_DATA_ROWS_IF_DATA_IS_EXPORTED;
	private boolean exportSampleData;
	private boolean checkMetadataTables;
	private boolean exportMetaData;
	private boolean usingDbiFileInfoOnly;
	private boolean allRows;
	private String warFileName;
	private String allowDataModelChanges = "true";
	private boolean allowSQLKeywords = true;
	private boolean updateSequences;
	private boolean overrideSequenceTypes;
	private boolean overrideDefaultValues;
	private boolean insertNewI18NKeysOnly;
	private boolean overwriteGroups;
	private boolean addUsersToAdminGroup;
	private int importUserPolicy;
	private Set<String> componentsToExportWithoutUnderTheHoodOnes;
	private Set<String> servicesToExportWithoutUnderTheHoodOnes;
	private boolean upgradeRepository;
	private boolean createTomcatContextXML;
	private boolean antiResourceLocking;
	private boolean clearReferencesStatic;
	private boolean clearReferencesStopThreads;
	private boolean clearReferencesStopTimerThreads;
	private String defaultAdminUser;
	private String defaultAdminPassword;

	private final List<String> preferencesExcludedDefaultComponentPackages = new ArrayList<>();

	private boolean ready = false;
	private boolean useAsRealAdminUser;
	private boolean searchProblem = false;
	private String webXMLFileName;
	private String log4jConfigurationFile;
	private boolean exportNoneActiveSolutions;
	private String contextFileName;
	private WorkspaceJob searchForUsedAndUnderTheHoodWebObjectsJob;
	private String generateExportCommandLinePropertiesFileSavePath;

	public ExportWarModel(IDialogSettings settings)
	{
		super();
		searchForUsedAndUnderTheHoodWebObjectsJob = new WorkspaceJob("Searching used components and services data")
		{

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				try
				{
					searchForComponentsAndServicesBothDefaultAndInSolution();

					// at one point we were showing under-the-hood components that are always needed to the user in the pick services/components dialog;
					// that is no longer the case, but old exports might have saved those in settings; update the model to not include setComponentsToExportWithoutUnderTheHoodOnes
					// they were initially set in code below, outside of this runInWorkspace
					if (componentsToExportWithoutUnderTheHoodOnes != null)
					{
						componentsToExportWithoutUnderTheHoodOnes.removeAll(ExportWarModel.super.getComponentsNeededUnderTheHood());
					}
					if (servicesToExportWithoutUnderTheHoodOnes != null)
					{
						servicesToExportWithoutUnderTheHoodOnes.removeAll(ExportWarModel.super.getServicesNeededUnderTheHoodWithoutSabloServices());
					}
				}
				catch (final Exception e)
				{
					Debug.error(e);
					searchProblem = true;
				}
				finally
				{
					ready = true;
				}
				return Status.OK_STATUS;
			}
		};
		searchForUsedAndUnderTheHoodWebObjectsJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
		searchForUsedAndUnderTheHoodWebObjectsJob.setUser(false);
		searchForUsedAndUnderTheHoodWebObjectsJob.schedule();

		Cipher desCipher = null;
		try
		{
			Cipher cipher = Cipher.getInstance("DESede"); //$NON-NLS-1$
			cipher.init(Cipher.DECRYPT_MODE, SecuritySupport.getCryptKey());
			desCipher = cipher;
		}
		catch (Exception e)
		{
			Debug.error("Cannot load encrypted previous export passwords", e);
		}

		warFileName = settings.get("export.warfilename");
		setUserHome(settings.get("export.userHome"));
		webXMLFileName = settings.get("export.webxmlfilename");
		log4jConfigurationFile = settings.get("export.log4jConfigurationFile");
		servoyPropertiesFileName = settings.get("export.servoyPropertiesFileName");
		exportActiveSolution = Utils.getAsBoolean(settings.get("export.exportActiveSolution"));
		exportNG2 = settings.get("export.ng2");
		exportNoneActiveSolutions = Utils.getAsBoolean(settings.get("export.exportNoneActiveSolutions"));
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
		if (settings.get("export.allowDataModelChanges") != null) allowDataModelChanges = settings.get("export.allowDataModelChanges");
		allowSQLKeywords = Utils.getAsBoolean(settings.get("export.allowSQLKeywords"), true);
		updateSequences = Utils.getAsBoolean(settings.get("export.updateSequences"));
		overrideDefaultValues = Utils.getAsBoolean(settings.get("export.overrideDefaultValues"));
		overrideSequenceTypes = Utils.getAsBoolean(settings.get("export.overrideSequenceTypes"));
		insertNewI18NKeysOnly = Utils.getAsBoolean(settings.get("export.insertNewI18NKeysOnly"));
		overwriteGroups = Utils.getAsBoolean(settings.get("export.overwriteGroups"));
		addUsersToAdminGroup = Utils.getAsBoolean(settings.get("export.addUsersToAdminGroup"));
		upgradeRepository = Utils.getAsBoolean(settings.get("export.upgradeRepository"));

		contextFileName = settings.get("export.tomcat.contextFileName");
		createTomcatContextXML = Utils.getAsBoolean(settings.get("export.tomcat.createTomcatContextXML"));
		antiResourceLocking = Utils.getAsBoolean(settings.get("export.tomcat.antiResourceLocking"));
		clearReferencesStatic = Utils.getAsBoolean(settings.get("export.tomcat.clearReferencesStatic"));
		clearReferencesStopThreads = Utils.getAsBoolean(settings.get("export.tomcat.clearReferencesStopThreads"));
		clearReferencesStopTimerThreads = Utils.getAsBoolean(settings.get("export.tomcat.clearReferencesStopTimerThreads"));

		generateExportCommandLinePropertiesFileSavePath = settings.get("ui.cmdgenerator.propertiesSavePath");

		if (settings.get("export.overwriteDBServerProperties") != null)
		{
			setOverwriteDeployedDBServerProperties(Utils.getAsBoolean(settings.get("export.overwriteDBServerProperties")));
		}
		else
		{
			setOverwriteDeployedDBServerProperties(true);
		}

		if (settings.get("export.overwriteAllProperties") != null)
		{
			setOverwriteDeployedServoyProperties(Utils.getAsBoolean(settings.get("export.overwriteAllProperties")));
		}
		else
		{
			setOverwriteDeployedServoyProperties(false);
		}

		if (settings.getArray("export.components") != null)
		{
			setComponentsToExportWithoutUnderTheHoodOnes(new TreeSet<String>(Arrays.asList(settings.getArray("export.components"))));
		}
		if (settings.getArray("export.services") != null)
		{
			setServicesToExportWithoutUnderTheHoodOnes(new TreeSet<String>(Arrays.asList(settings.getArray("export.services"))));
		}
		pluginLocations = new ArrayList<String>();
		String[] array = settings.getArray("plugin.locations");
		if (array != null && array.length > 0)
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

		if (settings.get("export.noneactivesolutions") != null)
		{
			StringTokenizer st = new StringTokenizer(settings.get("export.noneactivesolutions"), ";");
			while (st.hasMoreTokens())
			{
				noneActiveSolutions.add(st.nextToken());
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
					sc.setPassword(decryptPassword(settings, desCipher, "export.servers." + name + ".password"));
					sc.setSchema(settings.get("export.servers." + name + ".schema"));
					sc.setServerUrl(settings.get("export.servers." + name + ".serverurl"));
					sc.setUserName(settings.get("export.servers." + name + ".username"));
					sc.setValidationQuery(settings.get("export.servers." + name + ".validationquery"));
					sc.setConnectionValidationType(Utils.getAsInteger(settings.get("export.servers." + name + ".validationtype")));
					sc.setMaxActive(Utils.getAsInteger(settings.get("export.servers." + name + ".maxactive")));
					sc.setMaxIdle(Utils.getAsInteger(settings.get("export.servers." + name + ".maxidle")));
					sc.setMaxPreparedStatementsIdle(Utils.getAsInteger(settings.get("export.servers." + name + ".maxstatements")));
					sc.setSkipSysTables(Utils.getAsBoolean(settings.get("export.servers." + name + ".skipsystables")));
					sc.setPrefixTables(Utils.getAsBoolean(settings.get("export.servers." + name + ".prefixTables")));
					sc.setQueryProcedures(Utils.getAsBoolean(settings.get("export.servers." + name + ".queryProcedures")));
					sc.setClientOnlyConnections(Utils.getAsBoolean(settings.get("export.servers." + name + ".clientOnlyConnections")));
				}
			}
		}
		overwriteSocketFactoryProperties = false;
		defaultAdminUser = settings.get("export.defaultAdminUser");
		if (settings.get("export.defaultAdminPassword") != null)
		{
			defaultAdminPassword = decryptPassword(settings, desCipher, "export.defaultAdminPassword");
			useAsRealAdminUser = settings.getBoolean("export.useAsRealAdminUser");
		}


		Set<String> defaultPackageNames = ResourceProvider.getDefaultPackageNames();
		for (String packageName : defaultPackageNames)
		{
			if (!PlatformUI.getPreferenceStore().getBoolean("com.servoy.eclipse.designer.rfb.packages.enable." + packageName))
			{
				preferencesExcludedDefaultComponentPackages.add(packageName);
			}
		}

		if (settings.get("export.totalLicenses") != null)
		{
			int totalLicenses = settings.getInt("export.totalLicenses");
			for (int i = 1; i <= totalLicenses; i++)
			{
				String code = decryptPassword(settings, desCipher, "export.license." + i + ".code");
				licenses.put(code, new License(settings.get("export.license." + i + ".company_name"), code, settings.get("export.license." + i + ".licenses")));
			}
		}
	}

	/**
	 * ExportWarModel constructor starts a workspace job the searches in the active solution for used services/components.<br/>
	 * Call this method to make sure that job finished before trying to use services/components used by solution & under the hood components/services.
	 */
	public void waitForSearchJobToFinish()
	{
		try
		{
			if (searchForUsedAndUnderTheHoodWebObjectsJob != null) searchForUsedAndUnderTheHoodWebObjectsJob.join();
		}
		catch (InterruptedException e)
		{
			ServoyLog.logError("Error waiting for job that searches solution for used components/services & under the hood webobjects...", e);
		}
	}

	private String decryptPassword(IDialogSettings settings, Cipher desCipher, String propertyName)
	{
		return decryptPassword(desCipher, settings.get(propertyName));
	}

	public void saveSettings(IDialogSettings settings)
	{
		Cipher desCipher = null;
		try
		{
			Cipher cipher = Cipher.getInstance("DESede"); //$NON-NLS-1$
			cipher.init(Cipher.ENCRYPT_MODE, SecuritySupport.getCryptKey());
			desCipher = cipher;
		}
		catch (Exception e)
		{
			Debug.error("Cannot save encrypted export passwords", e);
		}

		settings.put("export.warfilename", warFileName);
		settings.put("export.ng2", exportNG2Mode());
		settings.put("export.userHome", getUserHome());
		settings.put("export.webxmlfilename", webXMLFileName);
		settings.put("export.log4jConfigurationFile", log4jConfigurationFile);
		settings.put("export.exportActiveSolution", exportActiveSolution);
		settings.put("export.exportNoneActiveSolutions", exportNoneActiveSolutions);
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
		settings.put("export.tomcat.contextFileName", contextFileName);
		settings.put("export.tomcat.createTomcatContextXML", createTomcatContextXML);
		settings.put("export.tomcat.antiResourceLocking", antiResourceLocking);
		settings.put("export.tomcat.clearReferencesStatic", clearReferencesStatic);
		settings.put("export.tomcat.clearReferencesStopThreads", clearReferencesStopThreads);
		settings.put("export.tomcat.clearReferencesStopTimerThreads", clearReferencesStopTimerThreads);
		settings.put("export.defaultAdminUser", defaultAdminUser);

		settings.put("ui.cmdgenerator.propertiesSavePath", generateExportCommandLinePropertiesFileSavePath);

		if (defaultAdminPassword != null)
			settings.put("export.defaultAdminPassword", encryptPassword(desCipher, "export.defaultAdminPassword", defaultAdminPassword));
		settings.put("export.useAsRealAdminUser", useAsRealAdminUser);

		settings.put("export.overwriteDBServerProperties", isOverwriteDeployedDBServerProperties());
		settings.put("export.overwriteAllProperties", isOverwriteDeployedServoyProperties());

		waitForSearchJobToFinish();
		if (componentsToExportWithoutUnderTheHoodOnes != null)
			settings.put("export.components", componentsToExportWithoutUnderTheHoodOnes.toArray(new String[componentsToExportWithoutUnderTheHoodOnes.size()]));
		if (servicesToExportWithoutUnderTheHoodOnes != null)
			settings.put("export.services", servicesToExportWithoutUnderTheHoodOnes.toArray(new String[servicesToExportWithoutUnderTheHoodOnes.size()]));

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

		if (noneActiveSolutions.size() > 0)
		{
			StringBuilder sb = new StringBuilder(128);
			for (String solName : noneActiveSolutions)
			{
				sb.append(solName);
				sb.append(';');
			}
			settings.put("export.noneactivesolutions", sb.toString());
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
				settings.put("export.servers." + name + ".password", encryptPassword(desCipher, "export.servers." + name + ".password", sc.getPassword()));
				if (sc.getSchema() != null) settings.put("export.servers." + name + ".schema", sc.getSchema());
				settings.put("export.servers." + name + ".serverurl", sc.getServerUrl());
				settings.put("export.servers." + name + ".username", sc.getUserName());
				settings.put("export.servers." + name + ".validationquery", sc.getValidationQuery());
				settings.put("export.servers." + name + ".validationtype", sc.getConnectionValidationType());
				settings.put("export.servers." + name + ".maxactive", sc.getMaxActive());
				settings.put("export.servers." + name + ".maxidle", sc.getMaxIdle());
				settings.put("export.servers." + name + ".maxstatements", sc.getMaxPreparedStatementsIdle());
				settings.put("export.servers." + name + ".skipsystables", sc.isSkipSysTables());
				settings.put("export.servers." + name + ".prefixTables", sc.isPrefixTables());
				settings.put("export.servers." + name + ".queryProcedures", sc.isQueryProcedures());
				settings.put("export.servers." + name + ".clientOnlyConnections", sc.isClientOnlyConnections());
			}
			settings.put("export.servers", sb.toString());
		}
		if (!licenses.isEmpty())
		{
			settings.put("export.totalLicenses", licenses.size());
			int i = 1;
			for (License license : licenses.values())
			{
				settings.put("export.license." + i + ".company_name", license.getCompanyKey());
				settings.put("export.license." + i + ".code", encryptPassword(desCipher, "export.license." + i + ".code", license.getCode()));
				settings.put("export.license." + i + ".licenses", license.getNumberOfLicenses());
				i++;
			}
		}
	}

	private String encryptPassword(Cipher desCipher, String propertyName, String password)
	{
		String val = password;
		if (val != null && !val.startsWith(IWarExportModel.enc_prefix) &&
			(propertyName.toLowerCase().indexOf("password") != -1 || propertyName.toLowerCase().startsWith("export.license"))) //$NON-NLS-1$
		{
			try
			{
				byte[] array_val = password.getBytes();
				String new_val = Utils.encodeBASE64(desCipher.doFinal(array_val));
				val = IWarExportModel.enc_prefix + new_val;
			}
			catch (Exception e)
			{
				Debug.error("Could not encrypt password " + propertyName, e);
			}
		}
		return val;
	}

	public String getFileName()
	{
		return fileName;
	}

	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	public String getWarFileName()
	{
		return warFileName;
	}

	public void setWarFileName(String warFileName)
	{
		this.warFileName = warFileName;
	}

	public String getServoyPropertiesFileName()
	{
		return servoyPropertiesFileName;
	}

	public void setServoyPropertiesFileName(String servoyPropertiesFileName)
	{
		this.servoyPropertiesFileName = nonEmpty(servoyPropertiesFileName);
	}

	public boolean isExportNonActiveSolutions()
	{
		return exportNoneActiveSolutions;
	}

	public void setExportNonActiveSolutions(boolean exportNoneActiveSolutions)
	{
		this.exportNoneActiveSolutions = exportNoneActiveSolutions;
	}

	public boolean isExportActiveSolution()
	{
		return exportActiveSolution;
	}

	public void setExportActiveSolution(boolean exportActiveSolution)
	{
		this.exportActiveSolution = exportActiveSolution;
	}

	@Override
	public String exportNG2Mode()
	{
		return exportNG2;
	}

	public void setExportNG2Mode(String exportNG2)
	{
		this.exportNG2 = exportNG2;
	}

	public List<String> getPlugins()
	{
		return plugins;
	}

	public List<String> getBeans()
	{
		return beans;
	}

	public List<String> getLafs()
	{
		return lafs;
	}

	public List<String> getDrivers()
	{
		return drivers;
	}

	public SortedSet<String> getSelectedServerNames()
	{
		return selectedServerNames;
	}

	public ServerConfiguration getServerConfiguration(String serverName)
	{
		ServerConfiguration serverConfiguration = servers.get(serverName);
		if (serverConfiguration == null)
		{
			IServerInternal server = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(serverName);
			if (server != null)
			{
				serverConfiguration = new ServerConfiguration(serverName, server.getConfig(), server.getSettings());
				servers.put(serverName, serverConfiguration);
			}
			else if (serverName.equals(IServer.REPOSITORY_SERVER))
			{
				server = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(serverName, false, true);
				serverConfiguration = server != null ? new ServerConfiguration(serverName, server.getConfig(), server.getSettings())
					: new ServerConfiguration(serverName);
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

	public void addPluginLocations(String pluginsDir)
	{
		pluginLocations.add(pluginsDir);
	}

	@Override
	public List<String> getPluginLocations()
	{
		return pluginLocations;
	}

	public boolean isExportAllTablesFromReferencedServers()
	{
		return exportAllTablesFromReferencedServers;
	}

	public void setExportAllTablesFromReferencedServers(boolean exportAllTablesFromReferencedServers)
	{
		this.exportAllTablesFromReferencedServers = exportAllTablesFromReferencedServers;
	}

	public boolean isExportMetaData()
	{
		return exportMetaData;
	}

	public void setExportMetaData(boolean exportMetaData)
	{
		this.exportMetaData = exportMetaData;
	}

	public boolean isCheckMetadataTables()
	{
		return checkMetadataTables;
	}

	public void setCheckMetadataTables(boolean checkMetadataTables)
	{
		this.checkMetadataTables = checkMetadataTables;
	}

	public boolean isExportSampleData()
	{
		return exportSampleData;
	}

	public void setExportSampleData(boolean exportSampleData)
	{
		this.exportSampleData = exportSampleData;
	}

	public void setNumberOfSampleDataExported(int sampleRows)
	{
		this.sampleRows = sampleRows;
	}

	public int getNumberOfSampleDataExported()
	{
		return sampleRows;
	}

	public boolean isExportI18NData()
	{
		return exportI18NData;
	}

	public void setExportI18NData(boolean exportI18NData)
	{
		this.exportI18NData = exportI18NData;
	}

	public boolean isExportUsingDbiFileInfoOnly()
	{
		return usingDbiFileInfoOnly;
	}

	public void setExportUsingDbiFileInfoOnly(boolean usingDbiFileInfoOnly)
	{
		this.usingDbiFileInfoOnly = usingDbiFileInfoOnly;
	}

	public String getAllowDataModelChanges()
	{
		return allowDataModelChanges;
	}

	public void setAllowDataModelChanges(String allowDataModelChanges)
	{
		this.allowDataModelChanges = allowDataModelChanges;
	}

	public boolean isAllowSQLKeywords()
	{
		return allowSQLKeywords;
	}

	public void setAllowSQLKeywords(boolean allowSQLKeywords)
	{
		this.allowSQLKeywords = allowSQLKeywords;
	}

	public boolean isUpdateSequences()
	{
		return updateSequences;
	}

	public void setUpdateSequences(boolean updateSequences)
	{
		this.updateSequences = updateSequences;
	}

	public boolean isOverrideSequenceTypes()
	{
		return overrideSequenceTypes;
	}

	public void setOverrideSequenceTypes(boolean overrideSequenceTypes)
	{
		this.overrideSequenceTypes = overrideSequenceTypes;
	}

	public boolean isOverrideDefaultValues()
	{
		return overrideDefaultValues;
	}

	public void setOverrideDefaultValues(boolean overrideDefaultValues)
	{
		this.overrideDefaultValues = overrideDefaultValues;
	}

	public boolean isInsertNewI18NKeysOnly()
	{
		return insertNewI18NKeysOnly;
	}

	public void setInsertNewI18NKeysOnly(boolean insertNewI18NKeysOnly)
	{
		this.insertNewI18NKeysOnly = insertNewI18NKeysOnly;
	}

	public boolean isOverwriteGroups()
	{
		return overwriteGroups;
	}

	public void setOverwriteGroups(boolean overwriteGroups)
	{
		this.overwriteGroups = overwriteGroups;
	}

	public boolean isAddUsersToAdminGroup()
	{
		return addUsersToAdminGroup;
	}

	public void setAddUsersToAdminGroup(boolean addUsersToAdminGroup)
	{
		this.addUsersToAdminGroup = addUsersToAdminGroup;
	}

	@Override
	public boolean isExportUsers()
	{
		return importUserPolicy > IXMLImportUserChannel.IMPORT_USER_POLICY_DONT;
	}


	public int getImportUserPolicy()
	{
		return importUserPolicy;
	}

	public void setImportUserPolicy(int importUserPolicy)
	{
		this.importUserPolicy = importUserPolicy;
	}

	public void setComponentsToExportWithoutUnderTheHoodOnes(Set<String> selectedComponents)
	{
		this.componentsToExportWithoutUnderTheHoodOnes = selectedComponents;
	}

	public void setServicesToExportWithoutUnderTheHoodOnes(Set<String> selectedServices)
	{
		this.servicesToExportWithoutUnderTheHoodOnes = selectedServices;
	}

	@Override
	public Set<String> getComponentsNeededUnderTheHood()
	{
		waitForSearchJobToFinish();
		return super.getComponentsNeededUnderTheHood();
	}

	@Override
	public Set<String> getServicesNeededUnderTheHoodWithoutSabloServices()
	{
		waitForSearchJobToFinish();
		return super.getServicesNeededUnderTheHoodWithoutSabloServices();
	}

	public Set<String> getComponentsToExportWithoutUnderTheHoodOnes()
	{
		waitForSearchJobToFinish();
		return componentsToExportWithoutUnderTheHoodOnes != null ? componentsToExportWithoutUnderTheHoodOnes : new TreeSet<String>();
	}

	public Set<String> getServicesToExportWithoutUnderTheHoodOnes()
	{
		waitForSearchJobToFinish();
		return servicesToExportWithoutUnderTheHoodOnes != null ? servicesToExportWithoutUnderTheHoodOnes : new TreeSet<String>();
	}

	public void setAutomaticallyUpgradeRepository(boolean upgrade)
	{
		upgradeRepository = upgrade;
	}

	@Override
	public boolean isAutomaticallyUpgradeRepository()
	{
		return upgradeRepository;
	}

	@Override
	public String getTomcatContextXMLFileName()
	{
		return contextFileName;
	}

	/**
	 * @param contextFileName the contextFileName to set
	 */
	public void setTomcatContextXMLFileName(String contextFileName)
	{
		this.contextFileName = contextFileName;
		if (this.createTomcatContextXML) this.createTomcatContextXML = contextFileName != null;
	}

	public boolean isCreateTomcatContextXML()
	{
		return createTomcatContextXML;
	}

	public void setCreateTomcatContextXML(boolean createTomcatContextXML)
	{
		this.createTomcatContextXML = createTomcatContextXML;
	}

	public boolean isClearReferencesStatic()
	{
		return clearReferencesStatic;
	}

	public void setClearReferencesStatic(boolean clearReferencesStatic)
	{
		this.clearReferencesStatic = clearReferencesStatic;
	}

	public boolean isClearReferencesStopThreads()
	{
		return clearReferencesStopThreads;
	}

	public void setClearReferencesStopThreads(boolean clearReferencesStopThreads)
	{
		this.clearReferencesStopThreads = clearReferencesStopThreads;
	}

	public boolean isClearReferencesStopTimerThreads()
	{
		return clearReferencesStopTimerThreads;
	}

	public void setClearReferencesStopTimerThreads(boolean clearReferencesStopTimerThreads)
	{
		this.clearReferencesStopTimerThreads = clearReferencesStopTimerThreads;
	}

	public boolean isAntiResourceLocking()
	{
		return antiResourceLocking;
	}

	public void setAntiResourceLocking(boolean antiResourceLocking)
	{
		this.antiResourceLocking = antiResourceLocking;
	}

	public List<String> getPreferencesExcludedDefaultComponentPackages()
	{
		return preferencesExcludedDefaultComponentPackages;
	}

	public List<String> getPreferencesExcludedDefaultServicePackages()
	{
		return preferencesExcludedDefaultComponentPackages;
	}

	public String getDefaultAdminUser()
	{
		return defaultAdminUser;
	}

	public void setDefaultAdminPassword(String defaultAdminPassword)
	{
		this.defaultAdminPassword = defaultAdminPassword;
	}

	public String getDefaultAdminPassword()
	{
		return defaultAdminPassword;
	}

	public void setDefaultAdminUser(String defaultAdminUser)
	{
		this.defaultAdminUser = defaultAdminUser;
	}

	public void setUseAsRealAdminUser(boolean useAsRealAdminUser)
	{
		this.useAsRealAdminUser = useAsRealAdminUser;
	}

	public boolean isUseAsRealAdminUser()
	{
		return useAsRealAdminUser;
	}

	public boolean isReady()
	{
		return ready;
	}

	public void removeLicense(String code)
	{
		licenses.remove(code);
	}

	public boolean hasSearchError()
	{
		return searchProblem;
	}

	public String getWebXMLFileName()
	{
		return webXMLFileName;
	}

	public void setWebXMLFileName(String webXMLFileName)
	{
		this.webXMLFileName = nonEmpty(webXMLFileName);
	}

	public String getLog4jConfigurationFile()
	{
		return log4jConfigurationFile;
	}

	public void setLog4jConfigurationFile(String log4jConfigurationFile)
	{
		this.log4jConfigurationFile = log4jConfigurationFile;
	}

	public void clearLicenses()
	{
		licenses.clear();
	}

	public List<String> getNonActiveSolutions()
	{
		return noneActiveSolutions;
	}

	private static String nonEmpty(String name)
	{
		if (name != null && name.trim().length() == 0)
		{
			return null;
		}
		return name;
	}

	@Override
	public Set<String> getExportedPackagesExceptSablo()
	{
		Set<String> allExportedServicePackages = getAllExportedServicesWithoutSabloServices().stream()
			.filter(service -> servicesSpecProviderState.getWebObjectSpecification(service) != null)
			.map(service -> servicesSpecProviderState.getWebObjectSpecification(service).getPackageName())
			.collect(Collectors.toSet());

		Set<String> allExportedComponentPackages = getAllExportedComponents().stream()
			.filter(component -> componentsSpecProviderState.getWebObjectSpecification(component) != null)
			.map(component -> componentsSpecProviderState.getWebObjectSpecification(component).getPackageName())
			.collect(Collectors.toSet());

		return Stream
			.of(allExportedComponentPackages, allExportedServicePackages, exportedLayoutPackages)
			.flatMap(Set::stream)
			.collect(Collectors.toSet());
	}

	@Override
	public Set<String> getAllExportedComponents()
	{
		Set<String> allComponentsThatShouldBeExported = new HashSet<>(getComponentsNeededUnderTheHood());
		allComponentsThatShouldBeExported.addAll(getComponentsToExportWithoutUnderTheHoodOnes());
		return allComponentsThatShouldBeExported;
	}

	@Override
	public Set<String> getAllExportedServicesWithoutSabloServices()
	{
		Set<String> allServicesThatShouldBeExportedExceptSabloServices = new HashSet<>(getServicesNeededUnderTheHoodWithoutSabloServices());
		allServicesThatShouldBeExportedExceptSabloServices.addAll(getServicesToExportWithoutUnderTheHoodOnes());
		return allServicesThatShouldBeExportedExceptSabloServices;
	}

	public String getGenerateExportCommandLinePropertiesFileSavePath()
	{
		return generateExportCommandLinePropertiesFileSavePath;
	}

	public void setGenerateExportCommandLinePropertiesFileSavePath(String path)
	{
		generateExportCommandLinePropertiesFileSavePath = path;
	}

}