package com.servoy.eclipse.exporter.setuppipeline;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.Cipher;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.json.JSONObject;

import com.servoy.eclipse.exporter.setuppipeline.SetupPipelineDetailsPage.GitInfo;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel;
import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel.License;
import com.servoy.eclipse.model.war.exporter.ExportException;
import com.servoy.eclipse.model.war.exporter.IWarExportModel;
import com.servoy.eclipse.model.war.exporter.ServerConfiguration;
import com.servoy.eclipse.model.war.exporter.WarExporter;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenWizardAction;
import com.servoy.eclipse.ui.wizards.IExportSolutionWizardProvider;
import com.servoy.eclipse.warexporter.Activator;
import com.servoy.eclipse.warexporter.export.ExportWarModel;
import com.servoy.eclipse.warexporter.ui.wizard.DirectorySelectionPage;
import com.servoy.eclipse.warexporter.ui.wizard.LicensePage;
import com.servoy.eclipse.warexporter.ui.wizard.ServerConfigurationPage;
import com.servoy.eclipse.warexporter.ui.wizard.ServersSelectionPage;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.SecuritySupport;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.SortedProperties;

/**
 *
 */
public class SetupPipelineWizard extends Wizard implements IWorkbenchWizard, IExportSolutionWizardProvider
{
	private SetupPipelineDetailsPage detailsPage;
	private SetupPipelineJenkinsCustomPage jenkinsPage;
	private SetupPipelineInfoPage infoPage;
	private SetupPipelineSolutionsPage solutionsPage;
	private LicensePage licenseConfigurationPage;
	private ServersSelectionPage serversSelectionPage;
	private DirectorySelectionPage driverSelectionPage;
	private DirectorySelectionPage pluginSelectionPage;
	//private DatabaseImportPropertiesPage databaseImportProperties;

	public static final String SETUP_PIPELINE_URL = System.getProperty("Dservoy.api.url", "https://middleware-prod.unifiedui.servoy-cloud.eu") +
		"/servoy-service/rest_ws/api/developer/setupPipeline";

	private ExportWarModel exportModel;

	List<String> defaultPluginsList = Arrays.asList(
		"amortization.jar",
		"broadcaster.jar",
		"clientmanager.jar",
		"converters.jar",
		"default_validators.jar",
		"excexport.jar",
		"file.jar",
		"headlessclient.jar",
		"http.jar",
		"images.jar",
		"jwt.jar",
		"mail.jar",
		"maintenance.jar",
		"mobile.jar",
		"mobileservice.jar",
		"oauth.jar",
		"pdf_forms.jar",
		"pdf_output.jar",
		"rawSQL.jar",
		"rest_ws.jar",
		"scheduler.jar",
		"serialize.jar",
		"tabxport.jar",
		"udp.jar",
		"xmlreader.jar",
		"adobe_pdf_forms",
		"jakarta-poi");


	List<String> defaultJdbcDrivers = Arrays.asList(
		"hsqldb.jar",
		"DBF_JDBC41.jar",
		"jaybird-full.jar",
		"jconn3.jar",
		"jtds.jar",
		"mssql-jdbc-12.6.2.jre11.jar",
		"mysql-connector-j-8.3.0.jar",
		"postgresql.jar");


	public SetupPipelineWizard()
	{
		//super();
		setWindowTitle("Servoy Cloud Pipeline Setup Wizard");
		setDialogSettings(ExportWarModel.getDialogSettings());
	}

	@Override
	public void addPages()
	{
		HashMap<String, IWizardPage> serverConfigurationPages = new HashMap<String, IWizardPage>();

		detailsPage = new SetupPipelineDetailsPage("Pipeline Details");
		jenkinsPage = new SetupPipelineJenkinsCustomPage("Jenkins-Custom Setup");
		infoPage = new SetupPipelineInfoPage("Setup Pipeline Info");
		solutionsPage = new SetupPipelineSolutionsPage("Solutions Setup");
		driverSelectionPage = new DirectorySelectionPage("driverpage", "Choose the jdbc drivers to export",
			"Select the jdbc drivers that you want to use in the war (if the app server doesn't provide them)",
			ApplicationServerRegistry.get().getServerManager().getDriversDir(), exportModel.getDrivers(), new String[] { "hsqldb.jar" },
			getDialogSettings().get("export.drivers") == null, false, "export_war_drivers");
		pluginSelectionPage = new DirectorySelectionPage("pluginpage", "Choose the plugins to export", "Select the plugins that you want to use in the war",
			ApplicationServerRegistry.get().getPluginManager().getPluginsDir(), exportModel.getPlugins(), null,
			getDialogSettings().get("export.plugins") == null, true, "export_war_plugins");
		serversSelectionPage = new ServersSelectionPage("serverspage", "Choose the database servernames to export",
			"Select the database server names that will be used on the application server", exportModel.getSelectedServerNames(),
			new String[] { IServer.REPOSITORY_SERVER }, serverConfigurationPages);
		licenseConfigurationPage = new LicensePage("licensepage", "Enter license key",
			"Please enter the Servoy client license key(s), or leave empty for running the solution in trial mode.", exportModel);

		addPage(infoPage);
		addPage(detailsPage);
		addPage(solutionsPage);
		addPage(pluginSelectionPage);
		addPage(driverSelectionPage);
		addPage(jenkinsPage);
		addPage(licenseConfigurationPage);
		addPage(serversSelectionPage);

		String[] serverNames = ApplicationServerRegistry.get().getServerManager().getServerNames(true, true, true, false);
		ArrayList<String> srvNames = new ArrayList<String>(Arrays.asList(serverNames));
		if (!srvNames.contains(IServer.REPOSITORY_SERVER))
		{
			srvNames.add(IServer.REPOSITORY_SERVER);
		}
		for (String serverName : srvNames)
		{
			ServerConfiguration serverConfiguration = exportModel.getServerConfiguration(serverName);
			ServerConfigurationPage configurationPage = new ServerConfigurationPage("serverconf:" + serverName, serverConfiguration,
				exportModel.getSelectedServerNames(), serverConfigurationPages, this);
			addPage(configurationPage);
			serverConfigurationPages.put(serverName, configurationPage);
		}

	}

	@Override
	public boolean performFinish()
	{
		try
		{
			driverSelectionPage.storeInput();
			pluginSelectionPage.storeInput();
			serversSelectionPage.storeInput();

			// 1. Collect data from wizard pages
			final String namespace = detailsPage.getNamespace();
			final String applicationJobName = detailsPage.getApplicationJobName();
			String appUrl = String.format("https://%s-dev.%s.servoy-cloud.eu", applicationJobName, namespace);

			final String gitUsername = detailsPage.getGitUsername();
			final String gitPassword = detailsPage.getGitPassword();
			final String gitUrl = detailsPage.getGitUrl();

			if (pipelineExists(namespace, applicationJobName))
			{
				org.eclipse.jface.dialogs.MessageDialog.openInformation(getShell(), "Pipeline Exists",
					"A pipeline with this job name already exists in the namespace.");
				return false;
			}

			String[] selectedNonActiveSolutions = solutionsPage.getSelectedSolutions();
			boolean includeNonActive = solutionsPage.isIncludeNonActiveSelected();

			String solutionName = solutionsPage.getActiveSolutionName();

			createJenkinsCustomDir();

			String servoyVersion = ClientVersion.getPureVersion();

			GitInfo gitInfo = detailsPage.getGitInfo();

			final JSONObject setupPipelineJson = PipelineJsonBuilder.build(
				namespace, applicationJobName, "jobName", appUrl, gitUsername, gitPassword, gitUrl, gitInfo.host, servoyVersion, solutionName,
				includeNonActive, Arrays.asList(selectedNonActiveSolutions), exportModel.getPlugins(), exportModel.getDrivers());


			// Create HTTP client and POST request
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(SETUP_PIPELINE_URL))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(setupPipelineJson.toString()))
				.build();

			// Send the request
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			// Optional: handle response
			if (response.statusCode() == 200)
			{
				System.out.println("post response: " + response.body());
			}
			else
			{
				org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Pipeline Error", "Failed to set up pipeline:\n" + response.body());
				return false;
			}

			org.eclipse.jface.dialogs.MessageDialog.openInformation(getShell(), "Pipeline Created", "Servoy Cloud pipeline has been successfully set up.");
			return true;

		}
		catch (final Exception e)
		{
			org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Error", "Failed to set up pipeline: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean canFinish()
	{
		String propsPath = jenkinsPage.getServoyPropsPath().trim();
		if (!propsPath.isEmpty())
		{
			File props = new File(propsPath);
			if (props.exists())
			{
				return true; // Allow Finish even if Next is disabled
			}
			else
			{
				return false;
			}
		}

		if (detailsPage.isPageComplete() && jenkinsPage.isPageComplete() && solutionsPage.isPageComplete())
		{
			return true;
		}

		return false;
	}

	private void createJenkinsCustomDir() throws ExportException
	{
		Path solutionParentDir = solutionsPage.getActiveSolutionParentDir();
		Path jenkinsCustomPath = solutionParentDir.resolve("jenkins-custom");
		Path rootPath = jenkinsCustomPath.resolve("server").resolve("webapps").resolve("ROOT");

		try
		{
			Path customJarsPath = jenkinsCustomPath.resolve("custom-jars");
			Files.createDirectories(customJarsPath);

			copyPlugins(customJarsPath);
			copyDrivers(customJarsPath);
			copyBeans(customJarsPath);

			Files.createDirectories(rootPath);

			copyConfigFiles(jenkinsCustomPath,
				jenkinsPage.getLog4jXmlPath(),
				jenkinsPage.getWebXmlPath(),
				jenkinsPage.getContextXmlPath());

			Path faviconSourceDir = Paths.get(jenkinsPage.getFaviconsDir());
			copyFaviconFiles(faviconSourceDir, rootPath);

			if (jenkinsPage.getServoyPropsPath() != null && !jenkinsPage.getServoyPropsPath().isEmpty())
			{
				Path originalFile = Paths.get(jenkinsPage.getServoyPropsPath());

				Path templateFile = jenkinsCustomPath.resolve("servoy.properties.template");
				Files.copy(originalFile, templateFile, StandardCopyOption.REPLACE_EXISTING);
				changeAndWritePropertiesFile(jenkinsCustomPath.toFile(), templateFile.toFile());
			}
			else
			{
				try
				{
					exportModel.generatePropertiesFileContent(new File(jenkinsCustomPath.toFile().getAbsolutePath(), "servoy.properties.template"));
				}
				catch (FileNotFoundException fileNotFoundException)
				{
					throw new ExportException("Couldn't find file " + jenkinsCustomPath.toFile().getAbsolutePath() + "servoy.properties.template",
						fileNotFoundException);
				}
				catch (IOException e)
				{
					throw new ExportException("Couldn't generate the properties file", e);
				}
			}

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * @param customJarsPath
	 */
	private void copyBeans(Path customJarsPath)
	{
		String appServerDir = exportModel.getServoyApplicationServerDir();
		Path beansPath = Paths.get(appServerDir, "beans");
		Path targetPath = customJarsPath.resolve("beans");

		try
		{
			if (Files.exists(beansPath) && Files.isDirectory(beansPath))
			{
				// Create target directory if it doesn't exist
				if (!Files.exists(targetPath))
				{
					Files.createDirectories(targetPath);
				}

				// Copy all files and subdirectories from beans to target
				Files.walk(beansPath).forEach(source -> {
					Path destination = targetPath.resolve(beansPath.relativize(source));
					try
					{
						Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				});
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void changeAndWritePropertiesFile(File jenkinsCustomDir, File sourceFile) throws ExportException
	{
		try (FileInputStream fis = new FileInputStream(sourceFile);
			FileOutputStream fos = new FileOutputStream(new File(jenkinsCustomDir, "servoy.properties.template")))
		{
			Properties properties = new SortedProperties();
			properties.load(fis);

			for (Object k : properties.keySet())
			{
				if (k instanceof String)
				{
					String key = (String)k;
					if (shouldEncrypt(key) && !properties.getProperty(key, "").startsWith(IWarExportModel.enc_prefix))
					{
						try
						{
							String password = IWarExportModel.enc_prefix + SecuritySupport.encrypt(properties.getProperty(key, ""));
							properties.put(k, password);
						}
						catch (Exception e)
						{
							ServoyLog.logError("Could not encrypt property " + key, e);
						}
					}
				}
			}

			if (exportModel.getUserHome() != null && exportModel.getUserHome().trim().length() > 0)
			{
				properties.setProperty(Settings.USER_HOME, exportModel.getUserHome());
			}

			if (exportModel.allowOverwriteSocketFactoryProperties())
			{
				// TODO currently command line always returns false for allowOverwriteSocketFactoryProperties(); it does not provide that as command line args;
				// that means that UI wizard will not be able to give an accurate command line equivalent if that one is set and the properties fie is also set
				properties.setProperty("SocketFactory.rmiServerFactory", "com.servoy.j2db.server.rmi.tunnel.ServerTunnelRMISocketFactoryFactory");
				properties.setProperty("SocketFactory.tunnelConnectionMode", "http&socket");
				if (properties.containsKey("SocketFactory.useTwoWaySocket")) properties.remove("SocketFactory.useTwoWaySocket");
			}

			Map<String, String> upgradedLicenses = exportModel.getUpgradedLicenses();
			if (upgradedLicenses != null)
			{
				if (!exportModel.getLicenses().isEmpty() || !upgradedLicenses.isEmpty())
				{
					List<License> licenses = new ArrayList<>();
					Cipher desCipher = null;
					try
					{
						Cipher cipher = Cipher.getInstance("DESede"); //$NON-NLS-1$
						cipher.init(Cipher.DECRYPT_MODE, SecuritySupport.getCryptKey());
						desCipher = cipher;
					}
					catch (Exception e)
					{
						ServoyLog.logError("Cannot load encrypted previous export passwords", e);
					}

					Set<String> codes = new HashSet<String>();
					if (properties.get("licenseManager.numberOfLicenses") != null)
					{
						int totalLicenses = Integer.parseInt(properties.getProperty("licenseManager.numberOfLicenses"));
						for (int i = 0; i < totalLicenses; i++)
						{
							String code = properties.getProperty("license." + i + ".code", "");
							if (code.startsWith(IWarExportModel.enc_prefix)) code = exportModel.decryptPassword(desCipher, code);
							codes.add(code);
							licenses.add(
								new License(properties.getProperty("license." + i + ".company_name"), code,
									properties.getProperty("license." + i + ".licenses")));
						}
					}

					for (License license : exportModel.getLicenses())
					{
						if (ApplicationServerRegistry.get().checkClientLicense(license.getCompanyKey(), license.getCode(), license.getNumberOfLicenses()))
						{
							if (codes.contains(license.getCode()))
							{
								ServoyLog.logInfo("Duplicate license for license key " + license.getCode().substring(0, 4) +
									"**-******-******. Please check the servoy.properties file");
								continue;
							}
							licenses.add(license);
						}
						else
						{
							ServoyLog.logError(new Exception("The license \"" + license.getCompanyKey() + ", " + license.getCode().substring(0, 4) +
								"**-******-******," + license.getNumberOfLicenses() + "\" is not valid"));
						}
					}

					for (License license : licenses)
					{
						if (upgradedLicenses.containsKey(license.getCode()))
						{
							license.setCode(upgradedLicenses.get(license.getCode()));
						}
					}

					AbstractWarExportModel.writeLicenses(properties, licenses);

				}
			}

			properties.store(fos, "");
		}
		catch (IOException e)
		{
			throw new ExportException("Failed to overwrite properties file", e);
		}
	}

	private boolean shouldEncrypt(String key)
	{
		String lcKey = key.toLowerCase();
		return lcKey.indexOf("password") != -1 || lcKey.indexOf("passphrase") != -1;
	}


	private void copyDrivers(Path customJarDirPath)
	{
		String appServerDir = exportModel.getServoyApplicationServerDir();
		Path driversTargetDir = customJarDirPath.resolve("drivers");
		try
		{
			List<String> drivers = exportModel.getDrivers();

			List<String> nondefaultDrivers = drivers.stream()
				.filter(p -> !defaultJdbcDrivers.contains(p))
				.collect(Collectors.toList());

			if (nondefaultDrivers.isEmpty())
			{
				return;
			}

			Files.createDirectories(driversTargetDir);

			for (String driverFileName : nondefaultDrivers)
			{
				Path sourceDriver = Paths.get(appServerDir, "drivers", driverFileName);
				Path targetDriver = driversTargetDir.resolve(driverFileName);

				if (Files.exists(sourceDriver))
				{
					Files.copy(sourceDriver, targetDriver, StandardCopyOption.REPLACE_EXISTING);
				}
			}

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * @param jenkinsCustomDir
	 */
	private void copyPlugins(Path customJarDirPath)
	{
		String appServerDir = exportModel.getServoyApplicationServerDir();
		Path pluginsTargetDir = customJarDirPath.resolve("plugins");

		try
		{
			List<String> plugins = exportModel.getPlugins();

			List<String> nonDefaultPlugins = plugins.stream()
				.filter(p -> !defaultPluginsList.contains(p))
				.collect(Collectors.toList());

			if (nonDefaultPlugins.isEmpty()) return;

			// Create plugins directory
			Files.createDirectories(pluginsTargetDir);

			for (String plugin : nonDefaultPlugins)
			{
				Path sourcePlugin = Paths.get(appServerDir, "plugins", plugin);
				Path targetPlugin = pluginsTargetDir.resolve(plugin);

				if (Files.isDirectory(sourcePlugin))
				{
					// Copy entire plugin folder
					Files.walk(sourcePlugin).forEach(source -> {
						try
						{
							Path destination = targetPlugin.resolve(sourcePlugin.relativize(source));
							if (Files.isDirectory(source))
							{
								Files.createDirectories(destination);
							}
							else
							{
								Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
							}
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					});
				}
				else
				{
					// Copy single plugin file
					Files.copy(sourcePlugin, targetPlugin, StandardCopyOption.REPLACE_EXISTING);
				}
			}

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void copyConfigFiles(Path jenkinsCustomPath, String log4j, String webxml, String contextxml) throws IOException
	{
		// log4j.xml
		Path log4jTarget = jenkinsCustomPath.resolve("log4j.xml");
		if (log4j != null && !log4j.isEmpty())
		{
			Files.copy(Paths.get(log4j), log4jTarget, StandardCopyOption.REPLACE_EXISTING);
		}
		else
		{
			try (InputStream is = WarExporter.class.getResourceAsStream("resources/log4j.xml"))
			{
				Files.copy(is, log4jTarget, StandardCopyOption.REPLACE_EXISTING);
			}
		}

		// web.xml
		Path webXmlTarget = jenkinsCustomPath.resolve("web.xml");
		if (webxml != null && !webxml.isEmpty())
		{
			Files.copy(Paths.get(webxml), webXmlTarget, StandardCopyOption.REPLACE_EXISTING);
		}
		else
		{
			try (InputStream is = WarExporter.class.getResourceAsStream("resources/web.xml"))
			{
				Files.copy(is, webXmlTarget, StandardCopyOption.REPLACE_EXISTING);
			}
		}

		// context.xml
		Path contextXmlTarget = jenkinsCustomPath.resolve("context.xml");
		if (contextxml != null && !contextxml.isEmpty())
		{
			Files.copy(Paths.get(contextxml), contextXmlTarget, StandardCopyOption.REPLACE_EXISTING);
		}
		else
		{
			String defaultContent = buildDefaultContextXml();
			Files.writeString(contextXmlTarget, defaultContent);
		}
	}


	private String buildDefaultContextXml()
	{
		StringBuilder sb = new StringBuilder("<Context ");
		sb.append("antiResourceLocking=\"true\" ");
		sb.append("clearReferencesStatic=\"true\" ");
		sb.append("clearReferencesStopThreads=\"true\" ");
		sb.append("clearReferencesStopTimerThreads=\"true\" ");
		sb.append(">\n</Context>");
		return sb.toString();
	}

	private void copyFaviconFiles(Path sourceDir, Path targetDir) throws IOException
	{
		String[] requiredFavicons = { "favicon.ico", "favicon32x32.png", "favicon192x192.png" };

		for (String fileName : requiredFavicons)
		{
			Path sourceFile = sourceDir.resolve(fileName);
			Path targetFile = targetDir.resolve(fileName);

			if (Files.exists(sourceFile))
			{
				Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	/**
	 * @param namespace
	 * @param jobName
	 * @return
	 */
	private boolean pipelineExists(String namespace, String jobName)
	{
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		exportModel = new ExportWarModel(ExportWarModel.getDialogSettings());
	}

	@Override
	public void createPageControls(Composite pageContainer)
	{
		super.createPageControls(pageContainer);
		getShell().setMinimumSize(500, 600); // Adjust width and height as needed
	}


	@Override
	public IAction getExportAction()
	{
		final ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
		if (activeProject != null && (activeProject.getSolutionMetaData().getSolutionType() == SolutionMetaData.SOLUTION ||
			activeProject.getSolutionMetaData().getSolutionType() == SolutionMetaData.NG_CLIENT_ONLY))
			return new OpenWizardAction(SetupPipelineWizard.class,
				ResourceLocator.imageDescriptorFromBundle(Activator.PLUGIN_ID, "$nl$/icons/ng_export.png").orElse(null),
				"Setup Pipeline");
		return null;
	}
}

class PipelineJsonBuilder
{
	public static JSONObject build(
		String namespace,
		String applicationName,
		String jobName,
		String appUrl,
		String gitUsername,
		String gitPassword,
		String gitUrl,
		String repositoryHost,
		String servoyVersion,
		String activeSolutionName,
		boolean includeNonActive,
		List<String> nonActiveSolutions,
		List<String> includedPlugins,
		List<String> includedDrivers)
	{

		// Exporter settings
		JSONObject exporterSettings = new JSONObject();
		exporterSettings.put("active_solution", activeSolutionName);
		if (includeNonActive)
		{
			exporterSettings.put("non_activeSolutions", nonActiveSolutions);
		}
		exporterSettings.put("servoy_version", servoyVersion);
		exporterSettings.put("includedPlugins", includedPlugins);
		exporterSettings.put("includedDrivers", includedDrivers);

		// Main JSON
		JSONObject setupPipelineJson = new JSONObject();
		setupPipelineJson.put("namespace", namespace);
		setupPipelineJson.put("applicationName", applicationName);
		setupPipelineJson.put("jobName", jobName);
		setupPipelineJson.put("appUrl", appUrl);
		setupPipelineJson.put("gitUsername", gitUsername);
		setupPipelineJson.put("gitPassword", gitPassword);
		setupPipelineJson.put("gitUrl", gitUrl);
		setupPipelineJson.put("exporterSettings", exporterSettings);
		setupPipelineJson.put("repositoryHost", repositoryHost); // Example additional setting

		return setupPipelineJson;
	}
}