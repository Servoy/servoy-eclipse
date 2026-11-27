package com.servoy.eclipse.model.war.exporter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.crypto.Cipher;

import com.servoy.eclipse.model.war.exporter.ServerConfiguration;

/**
 * Minimal concrete subclass of {@link AbstractWarExportModel} for plain unit tests: every abstract method from
 * {@link IWarExportModel} is stubbed with a neutral default so the testable helpers (decryptPassword, writeLicenses, License)
 * can be exercised without an Eclipse/Servoy runtime.
 */
public class TestWarExportModel extends AbstractWarExportModel
{
	public boolean exportActiveSolution = true;
	public String warFileName = "test.war";
	public String servoyPropertiesFileName = "servoy.properties";
	public String servoyApplicationServerDir = "/srv";
	public boolean allowOverwriteSocketFactoryProperties = true;
	public List<String> drivers = new ArrayList<>();
	public List<String> plugins = new ArrayList<>();
	public boolean startRMI = false;
	public String startRMIPort = "1099";
	public SortedSet<String> selectedServerNames = new TreeSet<>();
	public boolean overwriteGroups = false;
	public boolean allowSQLKeywords = false;
	public boolean overrideSequenceTypes = false;
	public boolean insertNewI18NKeysOnly = false;
	public boolean overrideDefaultValues = false;
	public int importUserPolicy = 0;
	public boolean addUsersToAdminGroup = false;
	public String allowDataModelChanges = "";
	public boolean updateSequences = false;
	public boolean automaticallyUpgradeRepository = false;
	public String tomcatContextXMLFileName = "";
	public boolean createTomcatContextXML = false;
	public boolean antiResourceLocking = false;
	public boolean clearReferencesStatic = false;
	public boolean clearReferencesStopThreads = false;
	public boolean clearReferencesStopTimerThreads = false;
	public String defaultAdminUser = "admin";
	public String defaultAdminPassword = "";
	public boolean useAsRealAdminUser = false;
	public String webXMLFileName = "web.xml";
	public String log4jConfigurationFile = "";
	public List<String> nonActiveSolutions = new ArrayList<>();
	public boolean exportNonActiveSolutions = false;
	public Map<String, String> upgradedLicenses = new java.util.HashMap<>();
	public boolean skipDatabaseViewsUpdate = false;
	public Set<String> componentsUsedExplicitlyBySolution = new TreeSet<>();
	public Set<String> componentsNeededUnderTheHood = new TreeSet<>();
	public Set<String> servicesUsedExplicitlyBySolution = new TreeSet<>();
	public Set<String> servicesNeededUnderTheHoodWithoutSabloServices = new TreeSet<>();
	public List<String> pluginLocations = new ArrayList<>();
	public String defaultAdminPassword2 = "";

	@Override public boolean isExportActiveSolution() { return exportActiveSolution; }
	@Override public String getWarFileName() { return warFileName; }
	@Override public String getServoyPropertiesFileName() { return servoyPropertiesFileName; }
	@Override public String getServoyApplicationServerDir() { return servoyApplicationServerDir; }
	@Override public boolean allowOverwriteSocketFactoryProperties() { return allowOverwriteSocketFactoryProperties; }
	@Override public List<String> getDrivers() { return drivers; }
	@Override public List<String> getPlugins() { return plugins; }
	@Override public boolean getStartRMI() { return startRMI; }
	@Override public String getStartRMIPort() { return startRMIPort; }
	@Override public SortedSet<String> getSelectedServerNames() { return selectedServerNames; }
	@Override public ServerConfiguration getServerConfiguration(String serverName) { return null; }
	@Override public List<String> getPluginLocations() { return pluginLocations; }
	@Override public Set<String> getComponentsUsedExplicitlyBySolution() { return componentsUsedExplicitlyBySolution; }
	@Override public Set<String> getComponentsNeededUnderTheHood() { return componentsNeededUnderTheHood; }
	@Override public Set<String> getServicesUsedExplicitlyBySolution() { return servicesUsedExplicitlyBySolution; }
	@Override public Set<String> getServicesNeededUnderTheHoodWithoutSabloServices() { return servicesNeededUnderTheHoodWithoutSabloServices; }
	@Override public boolean isOverwriteGroups() { return overwriteGroups; }
	@Override public boolean isAllowSQLKeywords() { return allowSQLKeywords; }
	@Override public boolean isOverrideSequenceTypes() { return overrideSequenceTypes; }
	@Override public boolean isInsertNewI18NKeysOnly() { return insertNewI18NKeysOnly; }
	@Override public boolean isOverrideDefaultValues() { return overrideDefaultValues; }
	@Override public int getImportUserPolicy() { return importUserPolicy; }
	@Override public boolean isAddUsersToAdminGroup() { return addUsersToAdminGroup; }
	@Override public String getAllowDataModelChanges() { return allowDataModelChanges; }
	@Override public boolean isUpdateSequences() { return updateSequences; }
	@Override public boolean isAutomaticallyUpgradeRepository() { return automaticallyUpgradeRepository; }
	@Override public String getTomcatContextXMLFileName() { return tomcatContextXMLFileName; }
	@Override public boolean isCreateTomcatContextXML() { return createTomcatContextXML; }
	@Override public boolean isAntiResourceLocking() { return antiResourceLocking; }
	@Override public boolean isClearReferencesStatic() { return clearReferencesStatic; }
	@Override public boolean isClearReferencesStopThreads() { return clearReferencesStopThreads; }
	@Override public boolean isClearReferencesStopTimerThreads() { return clearReferencesStopTimerThreads; }
	@Override public String getDefaultAdminUser() { return defaultAdminUser; }
	@Override public String getDefaultAdminPassword() { return defaultAdminPassword; }
	@Override public boolean isUseAsRealAdminUser() { return useAsRealAdminUser; }
	@Override public Collection<License> getLicenses() { return super.getLicenses(); }
	@Override public String decryptPassword(Cipher desCipher, String code) { return super.decryptPassword(desCipher, code); }
	@Override public void setUserHome(String userHome) { super.setUserHome(userHome); }
	@Override public String getUserHome() { return super.getUserHome(); }
	@Override public void setOverwriteDeployedDBServerProperties(boolean b) { super.setOverwriteDeployedDBServerProperties(b); }
	@Override public boolean isOverwriteDeployedDBServerProperties() { return super.isOverwriteDeployedDBServerProperties(); }
	@Override public void setOverwriteDeployedServoyProperties(boolean b) { super.setOverwriteDeployedServoyProperties(b); }
	@Override public boolean isOverwriteDeployedServoyProperties() { return super.isOverwriteDeployedServoyProperties(); }
	@Override public String getWebXMLFileName() { return webXMLFileName; }
	@Override public String getLog4jConfigurationFile() { return log4jConfigurationFile; }
	@Override public String checkWebXML() { return null; }
	@Override public boolean isPrecompressBrotli() { return false; }
	@Override public boolean isPrecompressGzip() { return false; }
	@Override public boolean isInjectTomcatPrecompressed() { return false; }
	@Override public String checkLog4jConfigurationFile() { return null; }
	@Override public List<String> getNonActiveSolutions() { return nonActiveSolutions; }
	@Override public boolean isExportNonActiveSolutions() { return exportNonActiveSolutions; }
	@Override public Map<String, String> getUpgradedLicenses() { return upgradedLicenses; }
	@Override public boolean isSkipDatabaseViewsUpdate() { return skipDatabaseViewsUpdate; }
	@Override public void setSkipDatabaseViewsUpdate(boolean skip) { super.setSkipDatabaseViewsUpdate(skip); }

	// IExportSolutionModel + ITiNGExportModel leftovers
	public String fileName = "export.servoy";
	public boolean protectWithPassword = false;
	public boolean exportReferencedModules = false;
	public boolean exportReferencedWebPackages = false;
	public boolean exportMetaData = false;
	public boolean exportSampleData = false;
	public boolean exportI18NData = false;
	public boolean exportUsers = false;
	public String[] modulesToExport = new String[0];
	public String password = "";
	public int numberOfSampleDataExported = 0;
	public boolean exportAllTablesFromReferencedServers = false;
	public boolean checkMetadataTables = false;
	public boolean exportUsingDbiFileInfoOnly = false;
	public boolean useImportSettings = false;
	public org.json.JSONObject importSettings = null;
	public String exportNG2Mode = "";
	public java.util.Set<String> exportedPackagesExceptSablo = new java.util.TreeSet<>();
	public java.util.Set<String> allExportedComponents = new java.util.TreeSet<>();
	public java.util.Set<String> allExportedServicesWithoutSabloServices = new java.util.TreeSet<>();

	@Override public String getFileName() { return fileName; }
	@Override public boolean isProtectWithPassword() { return protectWithPassword; }
	@Override public boolean isExportReferencedModules() { return exportReferencedModules; }
	@Override public boolean isExportReferencedWebPackages() { return exportReferencedWebPackages; }
	@Override public boolean isExportMetaData() { return exportMetaData; }
	@Override public boolean isExportSampleData() { return exportSampleData; }
	@Override public boolean isExportI18NData() { return exportI18NData; }
	@Override public boolean isExportUsers() { return exportUsers; }
	@Override public String[] getModulesToExport() { return modulesToExport; }
	@Override public String getPassword() { return password; }
	@Override public int getNumberOfSampleDataExported() { return numberOfSampleDataExported; }
	@Override public boolean isExportAllTablesFromReferencedServers() { return exportAllTablesFromReferencedServers; }
	@Override public boolean isCheckMetadataTables() { return checkMetadataTables; }
	@Override public boolean isExportUsingDbiFileInfoOnly() { return exportUsingDbiFileInfoOnly; }
	@Override public boolean useImportSettings() { return useImportSettings; }
	@Override public org.json.JSONObject getImportSettings() { return importSettings; }
	@Override public String exportNG2Mode() { return exportNG2Mode; }
	@Override public java.util.Set<String> getExportedPackagesExceptSablo() { return exportedPackagesExceptSablo; }
	@Override public java.util.Set<String> getAllExportedComponents() { return allExportedComponents; }
	@Override public java.util.Set<String> getAllExportedServicesWithoutSabloServices() { return allExportedServicesWithoutSabloServices; }
}
