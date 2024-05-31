package com.servoy.eclipse.model.war.exporter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.crypto.Cipher;

import com.servoy.eclipse.model.export.IExportSolutionModel;
import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel.License;


/**
 * WAR export model
 *
 * @author gboros
 * @since 8.0
 */
public interface IWarExportModel extends IExportSolutionModel
{
	public String enc_prefix = "encrypted:";//keep in sync with Settings.enc_prefix

	public boolean isExportActiveSolution();

	public String getWarFileName();

	public String getServoyPropertiesFileName();

	public String getServoyApplicationServerDir();

	public boolean allowOverwriteSocketFactoryProperties();

	public List<String> getDrivers();

	public List<String> getPlugins();

	public boolean getStartRMI();

	public String getStartRMIPort();

	public SortedSet<String> getSelectedServerNames();

	public ServerConfiguration getServerConfiguration(String serverName);

	public List<String> getPluginLocations();

	public Set<String> getComponentsUsedExplicitlyBySolution();

	public Set<String> getComponentsNeededUnderTheHood();

	public Set<String> getServicesUsedExplicitlyBySolution();

	/**
	 * NOTE: this currently won't include any sablo content (which is also under-the-hood) as all needed sablo js files are referenced statically from the page (pointing to jar file inside war) when serving ng clients.<br/>
	 * And here we want only the contents that will be wro grouped/optimized to be included.<br/><br/>
	 *
	 * {@link #getComponentsNeededUnderTheHood()} would be the same if sablo would have components, but it does not have any so sablo does not matter there...
	 */
	public Set<String> getServicesNeededUnderTheHoodWithoutSabloServices();

	public boolean isOverwriteGroups();

	public boolean isAllowSQLKeywords();

	public boolean isOverrideSequenceTypes();

	public boolean isInsertNewI18NKeysOnly();

	public boolean isOverrideDefaultValues();

	public int getImportUserPolicy();

	public boolean isAddUsersToAdminGroup();

	public String getAllowDataModelChanges();

	public boolean isUpdateSequences();

	boolean isAutomaticallyUpgradeRepository();

	String getTomcatContextXMLFileName();

	boolean isCreateTomcatContextXML();

	boolean isAntiResourceLocking();

	boolean isClearReferencesStatic();

	boolean isClearReferencesStopThreads();

	boolean isClearReferencesStopTimerThreads();

	/**
	 * admin properties
	 */
	public String getDefaultAdminUser();

	public String getDefaultAdminPassword();

	public boolean isUseAsRealAdminUser();

	Collection<License> getLicenses();

	String decryptPassword(Cipher desCipher, String code);

	public void setUserHome(String userHome);

	public String getUserHome();

	public void setOverwriteDeployedDBServerProperties(boolean isOverwriteDeployedDBServerProperties);

	public boolean isOverwriteDeployedDBServerProperties();

	public void setOverwriteDeployedServoyProperties(boolean isOverwriteDeployedServoyProperties);

	public boolean isOverwriteDeployedServoyProperties();

	public String getWebXMLFileName();

	public String getLog4jConfigurationFile();

	public String checkWebXML();

	public String checkLog4jConfigurationFile();

	public List<String> getNonActiveSolutions();

	public boolean isExportNonActiveSolutions();

	public Map<String, String> getUpgradedLicenses();

	public boolean isSkipDatabaseViewsUpdate();

	public void setSkipDatabaseViewsUpdate(boolean skip);

	public Set<String> getExportedPackagesExceptSablo();

	/**
	 * Gets all components that are to be exported. This includes under-the-hood components, components that are explicitly used by solution and any
	 * optional components that the user picked during export.
	 */
	Set<String> getAllExportedComponents();

	/**
	 * Gets almost all services that are to be exported. This includes under-the-hood services, services that are explicitly used by solution and any
	 * optional services that the user picked during export.<br/><br/>
	 *
	 * This currently won't include any sablo content as all needed sablo js files are referenced statically from the page (pointing to jar file inside war) when serving ng clients.<br/>
	 * And here we want only the contents that will be wro grouped/optimized to be included.<br/><br/>
	 *
	 * {@link #getComponentsNeededUnderTheHood()} would be the same if sablo would have components, but it does not have any so sablo does not matter there...
	 */
	Set<String> getAllExportedServicesWithoutSabloServices();

	public String exportNG2Mode();

	/**
	 * If a property file {@link #getServoyPropertiesFileName()} was not specified then a properties file will be generated using this method and other
	 * information provided by this export model. {for example @link #getStartRMI() or db settings which will be used to generate this content}
	 * @throws FileNotFoundsIOException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void generatePropertiesFileContent(File tmpWarDir) throws FileNotFoundException, IOException;

}
