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

package com.servoy.eclipse.model.war.exporter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Version;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebServiceSpecProvider;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.servoy.eclipse.model.Activator;
import com.servoy.eclipse.model.ING2WarExportModel;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.export.SolutionExporter;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.ngpackages.BaseNGPackageManager;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.EclipseExportI18NHelper;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableDefinitionUtils;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel.License;
import com.servoy.eclipse.ngclient.startup.resourceprovider.ComponentResourcesExporter;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ServerSettings;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.ngclient.less.LessCompiler;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DatabaseUtils;
import com.servoy.j2db.util.JarManager;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SecuritySupport;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.SortedProperties;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;
import com.servoy.j2db.util.xmlxport.IXMLExportUserChannel;
import com.servoy.j2db.util.xmlxport.TableDef;


/**
 * Class that creates the WAR file.
 *
 * @author gboros
 * @since 8.0
 */
public class WarExporter
{
	private static final String[] WAR_LIBS = new String[] { "org.freemarker*.jar", //
		"servoy_ngclient_" + ClientVersion.getPureVersion() + "*.jar", //
		"servoy_base_" + ClientVersion.getPureVersion() + "*.jar", //
		"servoy_shared_" + ClientVersion.getPureVersion() + "*.jar", //
		"servoy_smart_client_" + ClientVersion.getPureVersion() + "*.jar", //
		"servoy_headless_client_" + ClientVersion.getPureVersion() + "*.jar", //
		"j2db_log4j_" + ClientVersion.getPureVersion() + "*.jar", //
		"j2db_server_" + ClientVersion.getPureVersion() + "*.jar", //
		"sablo_" + ClientVersion.getPureVersion() + "*.jar", //
		"org.eclipse.dltk.javascript.rhino_*.jar", //
		"slf4j.api_*.jar", //
		"jabsorb_*.jar", //
		"org.apache.commons.commons-codec_*.jar", //
		"org.apache.commons.commons-io_*.jar", //
		"org.apache.commons.logging_*.jar", //
		"org.apache.commons.commons-collections4_*.jar", //
		"org.apache.commons.commons-dbcp2_*.jar", //
		"org.apache.commons.commons-pool2_*.jar", //
		"org.apache.commons.commons-fileupload2-core_*.jar", //
		"org.apache.commons.commons-fileupload2-jakarta-servlet6_*.jar", //
		"com.google.guava_*.jar", //
		"org.hibernate.orm.core_*.jar", //
		"org.apache.logging.log4j.api_*.jar", //
		"org.apache.logging.log4j.core_*.jar", //
		"org.apache.logging.log4j.slf4j2.impl_*.jar", //
		"org.apache.logging.log4j.jakarta.web_*.jar", //
//		"org.apache.logging.log4j.jcl_*.jar", //
		"org.antlr.runtime_*.jar", //
		"javax.transaction_*.jar", //
		"javax.persistence-api_*.jar", //
		"jakarta.mail-api_*.jar", //
		"jakarta.activation-api_*.jar", //
		"org.apache.james.apache-mime4j_*.jar", //
		"xstream_*.jar", //
		"org.jsoup_*.jar", //
		"com.github.ua-parser.uap-java_*.jar", //
		"org.yaml.snakeyaml_*.jar", //
		"org.jboss.logging.jboss-logging_*.jar", //
		"net.bytebuddy.byte-buddy_*.jar", //
		"org.apache.commons.lang3_*.jar", "org.apache.commons.text_*.jar", "org.apache.commons.commons-compress_*.jar", //
		"de.inetsoftware.jlessc_*.jar", "wrapped.com.servoy.tus-java-server_*.jar", //
		"com.fasterxml.jackson.core.jackson-core_*.jar", "com.fasterxml.jackson.core.jackson-databind_*.jar", //
		"com.fasterxml.jackson.core.jackson-annotations_*.jar", "wrapped.com.auth0.java-jwt*.jar", //
		"wrapped.com.auth0.jwks-rsa_*.jar", "com.github.scribejava.apis_*jar", //
		"com.github.scribejava.core_*.jar", "com.github.scribejava.java8_*.jar", //
		"org.owasp.encoder_*.jar" };

	private static final Set<String> EXCLUDED_RESOURCES_BY_NAME;

	static
	{
		EXCLUDED_RESOURCES_BY_NAME = new HashSet<>();
		EXCLUDED_RESOURCES_BY_NAME.add(".git");
		EXCLUDED_RESOURCES_BY_NAME.add(".gitignore");
		EXCLUDED_RESOURCES_BY_NAME.add(".project");
		EXCLUDED_RESOURCES_BY_NAME.add("angular.json");
		EXCLUDED_RESOURCES_BY_NAME.add("node_modules");
		EXCLUDED_RESOURCES_BY_NAME.add("webpackage.json");
		EXCLUDED_RESOURCES_BY_NAME.add("package.json");
		EXCLUDED_RESOURCES_BY_NAME.add("package-lock.json");
		EXCLUDED_RESOURCES_BY_NAME.add(".sourcepath");
		EXCLUDED_RESOURCES_BY_NAME.add("tsconfig.json");
	}

	public static class VersionComparator implements Comparator<String>
	{
		public static final VersionComparator INSTANCE = new VersionComparator();

		@Override
		public int compare(String o1, String o2)
		{
			Version v1 = new Version(o1);
			Version v2 = new Version(o2);
			return v1.compareTo(v2);
		}
	}

	private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss:S");

	private final IWarExportModel exportModel;
	private final IXMLExportUserChannel userChannel;
	private final SpecProviderState componentsSpecProviderState;
	private final SpecProviderState servicesSpecProviderState;
	private Set<File> pluginFiles = new HashSet<>();
	private File tmpWarDir = null;
	private String activeSolutionName = null;

	public WarExporter(IWarExportModel exportModel, IXMLExportUserChannel userChannel)
	{
		this.exportModel = exportModel;
		this.userChannel = userChannel;

		this.componentsSpecProviderState = WebComponentSpecProvider.getSpecProviderState();
		this.servicesSpecProviderState = WebServiceSpecProvider.getSpecProviderState();
	}

	public void doActiveSolutionExports(IProgressMonitor m) throws ExportException
	{
		SubMonitor monitor = SubMonitor.convert(m, "Creating War File", 8);

		if (tmpWarDir != null)
		{
			try
			{
				FileUtils.deleteDirectory(tmpWarDir);
			}
			catch (IOException e)
			{
				throw new ExportException("Could not delete the temporary war directory " + tmpWarDir, e);
			}
		}
		activeSolutionName = ServoyModelFinder.getServoyModel().getFlattenedSolution().getName();

		tmpWarDir = createTempDir();
		monitor.worked(1);
		copySecAndDBIFiles(tmpWarDir);
		monitor.worked(4);
		if (exportModel.isExportActiveSolution())
		{
			monitor.subTask("Copy the active solution (" + SDF.format(new Date()) + ")");
			copyActiveSolution(monitor.newChild(2));
			// TODO this only compiles the less resources of the active project (and its modules) not for the none active solutions that could also be exported
			monitor.subTask("Compile less resources (" + SDF.format(new Date()) + ")");
			compileLessResources(tmpWarDir);
			monitor.worked(1);
		}
	}

	/**
	 * Export the solution as war.
	 * @param monitor
	 * @throws ExportException if export fails
	 */
	public File doExport(IProgressMonitor m) throws ExportException
	{
		SubMonitor monitor = SubMonitor.convert(m, "Creating War File", 42);
		File warFile = createNewWarFile();
		monitor.worked(2);
		if (tmpWarDir == null)
		{
			doActiveSolutionExports(m);
		}
		monitor.worked(2);
		String appServerDir = exportModel.getServoyApplicationServerDir();
		monitor.subTask("Copy root webapp files (" + SDF.format(new Date()) + ")");
		copyRootWebappFiles(appServerDir);
		monitor.worked(2);
		monitor.subTask("Copy plugins");
		copyPlugins(appServerDir);
		monitor.worked(2);
		monitor.subTask("Copy all standard libraries (" + SDF.format(new Date()) + ")");
		final File targetLibDir = copyStandardLibs(appServerDir);
		monitor.worked(2);
		monitor.subTask("Copy Drivers");
		copyDrivers(appServerDir, targetLibDir);
		monitor.worked(2);
		moveSlf4j(targetLibDir);
		monitor.worked(2);
		monitor.subTask("Creating web.xml");
		copyWebXml();
		monitor.worked(2);
		monitor.subTask("Creating log4j configuration file");
		copyLog4jConfigurationFile();
		monitor.worked(2);
		monitor.subTask("Creating context.xml");
		createTomcatContextXML();
		monitor.worked(2);
		addServoyProperties();
		monitor.worked(2);

		exportAdminUser();

		monitor.setWorkRemaining(11);
		monitor.subTask("Copying NGClient components/services... (" + SDF.format(new Date()) + ")");
		copyComponentsAndServicesPlusLibs(monitor.newChild(2));
		if (monitor.isCanceled()) return null;

		monitor.setWorkRemaining(5);
		monitor.subTask("Copy exported components");
		copyExportedComponentsAndServicesPropertyFile(m);
		monitor.worked(2);
		if (exportModel.exportNG2Mode() == null || !exportModel.exportNG2Mode().equals("false"))
		{
			monitor.subTask("Copy Titanium NGClient resources (" + SDF.format(new Date()) + ")");
			try
			{
				copyNGClient2(monitor);
			}
			catch (RuntimeException e)
			{
				if (monitor.isCanceled()) return null;
				throw new ExportException("could not create/copy Titanium NGClient resources", e);
			}
		}
		monitor.worked(1);
		try
		{
			// just always copy the nglibs to it even if it is just pure smart client
			// the log4j libs are always needed.
			copyWARLibs(targetLibDir);
		}
		catch (IOException e)
		{
			throw new ExportException("Could not copy the libs " + Arrays.toString(WAR_LIBS) + ", " + pluginFiles, e);
		}
		monitor.worked(1);
		monitor.subTask("Creating deploy properties (" + SDF.format(new Date()) + ")");
		createDeployPropertiesFile();
		monitor.worked(1);
		monitor.subTask("Checking war for duplicate jars (" + SDF.format(new Date()) + ")");
		if (monitor.isCanceled()) return null;
		// first check,remove duplicate jars from the plugins dir.
		checkDuplicateJars();
		monitor.worked(1);
		// after that copy or move everything to the WEB-INF/lib dir to have 1 big classpath but only if this is not for a smartclient
		if (!exportModel.getStartRMI())
		{
			File pluginsDir = new File(tmpWarDir, "plugins");
			try
			{
				Files.walk(pluginsDir.toPath()).map(path -> path.toFile()).filter(this::filterPluginFile)
					.forEach(file -> {
						File targetFile = new File(targetLibDir, file.getName());
						if (targetFile.exists())
						{

							// this shouldn't be a duplicate jar of the same thing (checkDuplicatJars should already have handled this)
							// so this is the same name for a different jar, like mail.jar (plugin) and mail.jar (sun mail lib)
							targetFile = new File(targetLibDir, "copy_" + (RandomStringUtils.randomAlphanumeric(3)) + '_' + file.getName());
						}
						try
						{
							FileUtils.moveFile(file, targetFile);
						}
						catch (IOException e)
						{
							throw new RuntimeException(new ExportException("Could not copy the lib from plugins " + file + " to " + targetFile, e));
						}
					});
				FileUtils.deleteDirectory(pluginsDir);
			}
			catch (IOException e)
			{
				throw new ExportException("Couldn't move/copy plugin libs to the WEB-INF/lib", e);
			}
			catch (RuntimeException re)
			{
				if (re.getCause() instanceof ExportException) throw (ExportException)re.getCause();
				else throw re;
			}
		}

		monitor.worked(1);
		if (monitor.isCanceled()) return null;
		monitor.subTask("Creating/zipping the WAR file (" + SDF.format(new Date()) + ")");
		zipDirectory(tmpWarDir, warFile);
		monitor.worked(2);
		deleteDirectory(tmpWarDir);
		monitor.worked(1);
		monitor.subTask("Done (" + SDF.format(new Date()) + ")");
		monitor.done();
		return warFile;
	}

	/**
	 * @return
	 */
	protected boolean filterPluginFile(File file)
	{
		String name = file.getName().toLowerCase();
		return file.isFile() && !name.endsWith(".jnlp") && !name.equals("plugins.properties");
	}

	private void checkDuplicateJars() throws ExportException
	{
		Map<String, TreeMap<String, List<File>>> dependenciesVersions = new HashMap<>();
		TreeMap<String, List<File>> possibleDuplicates = new TreeMap<>();
		Set<File> libs;
		try
		{
			libs = Files.walk(tmpWarDir.toPath()).filter(path -> path.toString().endsWith(".jar"))//
				.map(path -> path.toFile()).collect(Collectors.toSet());
			libs.forEach(jar -> checkDuplicateJar(jar, dependenciesVersions, possibleDuplicates));
		}
		catch (IOException e)
		{
			throw new ExportException("Error checking duplicate jars.", e);
		}


		Properties properties = new Properties();
		File pluginProperties = new File(tmpWarDir, "plugins/plugins.properties");
		try (FileInputStream fis = new FileInputStream(pluginProperties))
		{
			properties.load(fis);
		}
		catch (IOException e)
		{
			throw new ExportException("Error creating plugins dir", e);
		}

		boolean removedJar = false;
		StringBuilder messageBuilder = new StringBuilder();
		try
		{
			for (String jar : dependenciesVersions.keySet())
			{
				String latest = dependenciesVersions.get(jar).lastKey();
				List<File> list = dependenciesVersions.get(jar).get(latest);
				File latestJar = list.get(0);
				String latestJarPath = getRelativePath(tmpWarDir, latestJar);
				if (list.size() > 1 || dependenciesVersions.get(jar).size() > 1)
				{
					Optional<File> lib = dependenciesVersions.get(jar).values().stream()
						.flatMap(Collection::stream).filter(f -> getRelativePath(tmpWarDir, f).startsWith(File.separator + "lib")).findAny(); //there should be max one in lib anyway
					if (lib.isPresent() && !latestJarPath.startsWith(File.separator + "lib"))
					{
						//keep the one in the lib folder, doesn't matter if it's older
						latestJarPath = getRelativePath(tmpWarDir, lib.get());
						latestJar = lib.get();
						String name = latestJar.getName().substring(0, latestJar.getName().indexOf(".jar")).replaceAll("-|_|\\d|\\.", "");
						possibleDuplicates.get(name).add(latestJar);
					}

					for (String version : dependenciesVersions.get(jar).keySet())
					{
						if ("0".equals(version) && dependenciesVersions.get(jar).get(version).size() > 1)
						{
							ServoyLog.logWarning("Duplicate '" + jar + "' jars with unknown versions " + dependenciesVersions.get(jar).values(), null);
						}
						List<File> listToRemove = dependenciesVersions.get(jar).get(version);
						for (File file : listToRemove)
						{
							if (latestJar.equals(file)) continue;
							String path = getRelativePath(tmpWarDir, file);
							if (path.contains("plugins"))
							{
								properties.remove(file.getPath().substring(file.getPath().indexOf("plugins") + "plugins/".length()).replace('\\', '/'));
								removedJar = true;
							}
							String name = file.getName().substring(0, file.getName().indexOf(".jar")).replaceAll("-|_|\\d|\\.", "");
							possibleDuplicates.get(name).remove(file);
							File parent = file.getParentFile();
							file.delete();
							if (parent.list().length == 0)
							{
								parent.delete();
							}

							if (!latest.equals(version))
							{
								if (messageBuilder.length() == 0)
								{
									messageBuilder.append(
										"The following jars are not exported to avoid potential problems due to duplicate jars in the plugins or the Servoy core: \n\n");
								}
								messageBuilder.append("\nDependency '" + path +
									"' is not exported because another " + latestJar.getName().replace("-" + version, "") + " with a higher version (" +
									latest +
									") is already present in '" + latestJarPath + "'. \n");
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		if (removedJar)
		{
			try (FileOutputStream fos = new FileOutputStream(pluginProperties))
			{
				properties.store(fos, "");
			}
			catch (IOException e)
			{
				throw new ExportException("Error creating plugins dir", e);
			}
		}
		if (messageBuilder.length() > 0)
		{
			messageBuilder.append(
				"\n If you are not using the latest versions of the exported plugins, an upgrade might fix the warnings. Otherwise, no action is required.");
			userChannel.displayWarningMessage("Plugin dependencies problem", messageBuilder.toString(), true);
		}

		if (!possibleDuplicates.isEmpty())
		{
			Optional<List<File>> moreJars = possibleDuplicates.values().stream().filter(jars -> jars.size() > 1).findAny();
			if (!moreJars.isPresent()) return;
			messageBuilder = new StringBuilder(
				"The following jars have similar file names so they are possible duplicates, which means the war deployment could fail if the wrong jar is used. \n" +
					"They cannot be checked automatically because some don't provide the bundle symbolic name in the manifest, or it is not exactly the same. \nPlease check and delete the duplicate jars manually.\n\n");
			for (List<File> jars : possibleDuplicates.values())
			{
				if (jars.size() > 1)
				{
					String message = jars.stream().map(file -> getRelativePath(tmpWarDir, file))//
						.collect(Collectors.joining(", "));
					message = message.substring(0, message.lastIndexOf(",")) + message.substring(message.lastIndexOf(",")).replace(",", " and");
					messageBuilder.append("- " + message + "\n");
				}
			}
			userChannel.displayWarningMessage("Possible duplicate jars", messageBuilder.toString(), true);
		}
	}

	private String getRelativePath(File tmpWarDir, File latestJar)
	{
		return latestJar.getPath().replace(tmpWarDir.getPath(), "").replace(File.separator + "WEB-INF", "");
	}

	private void copyNGClient2(IProgressMonitor monitor)
	{
		Activator.getDefault().exportNG2ToWar(new ING2WarExportModel()
		{

			@Override
			public IProgressMonitor getProgressMonitor()
			{
				return monitor;
			}

			@Override
			public IWarExportModel getModel()
			{
				return exportModel;
			}

			@Override
			public File getExportLocation()
			{
				return tmpWarDir;
			}

			public String getSolutionName()
			{
				if (activeSolutionName == null)
				{
					return ServoyModelFinder.getServoyModel().getFlattenedSolution().getName();
				}
				return activeSolutionName;
			}
		});
	}

	/**
	 * @param tmpWarDir
	 */
	public static void compileLessResources(File tmpWarDir)
	{
		// this only compiles the active solution and modules less stuff in a dir
		// not from the none active solutions, problem could be that the none active solutions can have duplicate names..
		IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
		FlattenedSolution fs = servoyModel.getFlattenedSolution();
		Iterator<Media> it = fs.getMedias(false);
		while (it.hasNext())
		{
			Media media = it.next();
			if (media.getName().endsWith(".less"))
			{
				String content = LessCompiler.compileSolutionLessFile(media, fs);
				if (content != null)
				{
					File folder = new File(tmpWarDir, MediaResourcesServlet.SERVOY_SOLUTION_CSS);
					try
					{
						File f = new File(folder, media.getName().replace(".less", ".css"));
						if (!f.getParentFile().exists() && !f.getParentFile().mkdirs())
						{
							ServoyLog.logError("Could not create folder " + f.getParentFile().getName() + " for less media: " + media.getName(),
								new RuntimeException());
							break;
						}
						f.createNewFile();
						try (PrintWriter printWriter = new PrintWriter(f))
						{
							printWriter.println(content);
						}
						catch (FileNotFoundException e)
						{
							ServoyLog.logError(e);
						}
					}
					catch (IOException e)
					{
						ServoyLog.logError("Error creating less file:  " + media.getName(), e);
					}
				}
			}
		}
	}

	/**
	 * Copy to the war the properties file containing the selected NG components and services.
	 * This is needed to optimize the references included in the index.html file.
	 * If no components and services are selected, then all references would be included in the index.
	 */
	private void copyExportedComponentsAndServicesPropertyFile(IProgressMonitor m) throws ExportException
	{
		Set<String> exportedComponents = exportModel.getAllExportedComponents();
		Set<String> exportedServicesWithoutSabloServices = exportModel.getAllExportedServicesWithoutSabloServices();

		if (exportedComponents != null)
		{
			Set<String> componentsExceptUnderTheHoodOnes = new TreeSet<>(exportedComponents);
			componentsExceptUnderTheHoodOnes.removeAll(exportModel.getComponentsNeededUnderTheHood());
			m.subTask("Exporting components: " + Arrays.toString(componentsExceptUnderTheHoodOnes.toArray(new String[0])));
		}

		if (exportedServicesWithoutSabloServices != null)
		{
			Set<String> servicesExceptUnderTheHoodOnes = new TreeSet<>(exportedServicesWithoutSabloServices);
			servicesExceptUnderTheHoodOnes.removeAll(exportModel.getServicesNeededUnderTheHoodWithoutSabloServices());
			m.subTask("Exporting services: " + Arrays.toString(servicesExceptUnderTheHoodOnes.toArray(new String[0])));
		}

		// sablo services are automatically loaded fully from the sablo jar that is included in the war file; they are a bit special
		// and not included in exportedServicesWithoutSabloServices map; we do not need to list these in this properties file as jar loaded webobjects are not affected by this
		if ((exportedComponents == null && exportedServicesWithoutSabloServices == null) ||
			(exportedComponents.size() == componentsSpecProviderState.getAllWebObjectSpecifications().length &&
				exportedServicesWithoutSabloServices.size() + WebServiceSpecProvider.getSpecProviderState().getWebObjectSpecifications().get("sablo")
					.getSpecifications().keySet().size() == servicesSpecProviderState.getAllWebObjectSpecifications().length))
			return; // all of them will be loaded in app; if we do not generate a properties file at all then that will be the case

		File exported = new File(tmpWarDir, "WEB-INF/exported_web_objects.properties");
		Properties properties = new Properties();
		StringBuilder webObjects = new StringBuilder();
		for (String component : exportedComponents)
		{
			webObjects.append(component + ",");
		}

		TreeSet<String> allServices = new TreeSet<String>();
		// append internal servoy services; TODO these should already be included due to exportModel.getServicesNeededUnderTheHood() which get included in exportModel.getAllExportedServices()
		PackageSpecification<WebObjectSpecification> servoyservices = servicesSpecProviderState.getWebObjectSpecifications().get("servoyservices");
		if (servoyservices != null) allServices.addAll(servoyservices.getSpecifications().keySet());
		// append user services
		if (exportedServicesWithoutSabloServices != null) allServices.addAll(exportedServicesWithoutSabloServices);
		for (String service : allServices)
		{
			webObjects.append(service + ",");
		}
		properties.put("usedWebObjects", webObjects.substring(0, webObjects.length() - 1));
		try (FileOutputStream fos = new FileOutputStream(exported))
		{
			properties.store(fos, "");
		}
		catch (Exception e)
		{
			throw new ExportException("Couldn't generate the exported_web_objects.properties file", e);
		}
	}

	/**
	 * Copy to the war all NG components and services (default and user-defined), as well as the jars required by the NGClient.
	 */
	private void copyComponentsAndServicesPlusLibs(IProgressMonitor monitor) throws ExportException
	{
		try
		{
			StringBuilder componentLocations = new StringBuilder();
			StringBuilder servicesLocations = new StringBuilder();

			Map<String, File> allTemplates = new HashMap<String, File>();
			Set<String> exportedPackages = exportModel.getExportedPackagesExceptSablo();
			ComponentResourcesExporter.copyDefaultComponentsAndServices(tmpWarDir, exportedPackages, allTemplates);

			componentLocations.append(ComponentResourcesExporter.getDefaultComponentDirectoryNames(exportedPackages));
			servicesLocations.append(ComponentResourcesExporter.getDefaultServicesDirectoryNames(exportedPackages));

			monitor.worked(1);
			BaseNGPackageManager packageManager = ServoyModelFinder.getServoyModel().getNGPackageManager();

			List<IPackageReader> packageReaders = packageManager.getAllPackageReaders();
			for (IPackageReader packageReader : packageReaders)
			{
				if (monitor.isCanceled()) return;
				File resource = packageReader.getResource();
				if (resource != null)
				{
					String entryDir = packageReader.getManifest().getMainAttributes().getValue("Entry-Point");
					if (entryDir != null)
					{
						int index = entryDir.indexOf('/');
						if (index != -1) entryDir = entryDir.substring(0, index);
					}
					boolean copy = false;
					String name = packageReader.getPackageName();
					if ((IPackageReader.WEB_COMPONENT.equals(packageReader.getPackageType()) ||
						IPackageReader.WEB_SERVICE.equals(packageReader.getPackageType())) && exportedPackages.contains(name))
					{
						if (IPackageReader.WEB_COMPONENT.equals(packageReader.getPackageType()))
						{
							componentLocations.append("/" + name + "/;");
						}
						else
						{
							servicesLocations.append("/" + name + "/;");
						}
						copy = true;
					}
					else if (IPackageReader.WEB_LAYOUT.equals(packageReader.getPackageType()) && exportedPackages.contains(name))
					{
						PackageSpecification<WebLayoutSpecification> spec = componentsSpecProviderState.getLayoutSpecifications().get(name);
						copy = spec != null; /*
												 * && (spec.getCssClientLibrary() != null && !spec.getCssClientLibrary().isEmpty() || spec.getJsClientLibrary()
												 * != null && !spec.getJsClientLibrary().isEmpty());
												 */
						if (copy) componentLocations.append("/" + name + "/;");
					}
					if (copy)
					{
						if (resource.isDirectory())
						{
							Set<String> excludes = EXCLUDED_RESOURCES_BY_NAME;
							if (entryDir != null)
							{
								excludes = new HashSet<String>(EXCLUDED_RESOURCES_BY_NAME);
								excludes.add(entryDir);
							}
							copyDir(resource, new File(tmpWarDir, name), true, allTemplates, excludes, true);
						}
						else
						{
							Set<String> excludes = EXCLUDED_RESOURCES_BY_NAME;
							if (entryDir != null)
							{
								excludes = new HashSet<String>(EXCLUDED_RESOURCES_BY_NAME);
								excludes.add(entryDir + '/'); // extractaJar is startsWith because of the jar entries.
							}
							extractJar(name, resource, allTemplates, excludes);
						}
					}
				}
			}
			monitor.worked(1);

			createSpecLocationsPropertiesFile(new File(tmpWarDir, "WEB-INF/components.properties"), componentLocations.toString());
			createSpecLocationsPropertiesFile(new File(tmpWarDir, "WEB-INF/services.properties"), servicesLocations.toString());

			monitor.worked(1);
		}
		catch (IOException e)
		{
			throw new ExportException("Could not copy the components", e);
		}
	}

	/**
	 * Add all NG_LIBS to the war.
	 * @param targetLibDir
	 * @param includeNGClientLib
	 * @throws ExportException
	 * @throws IOException
	 */
	private void copyWARLibs(File targetLibDir) throws ExportException, IOException
	{
		if (pluginFiles.isEmpty())
		{
			String notFound = searchExportedPlugins();
			if (notFound != null) throw new ExportException(notFound + " was not found. Please specify location");
		}
		for (File file : pluginFiles)
		{
			copyFile(file, new File(targetLibDir, file.getName()));
		}
	}

	/**
	 * @param tmpWarDir
	 * @param locations
	 * @throws ExportException
	 */
	private void createSpecLocationsPropertiesFile(File file, String locations) throws ExportException
	{
		Properties properties = new Properties();
		properties.put("locations", locations);
		try (FileOutputStream fos = new FileOutputStream(file))
		{
			properties.store(fos, "");
		}
		catch (Exception e)
		{
			throw new ExportException("Couldn't generate the components.properties file", e);
		}
	}

	private void extractJar(String dirName, File file, Map<String, File> allTemplates, Set<String> excludedResourcesByName)
	{
		try (JarFile jarfile = new JarFile(file))
		{
			Enumeration<JarEntry> enu = jarfile.entries();
			while (enu.hasMoreElements())
			{
				String destdir = tmpWarDir + "/" + dirName;
				JarEntry je = enu.nextElement();
				if (excludedResourcesByName != null && excludedResourcesByName.stream().anyMatch(item -> je.getName().startsWith(item))) continue;
				if (!je.getName().endsWith(".spec") && !je.getName().endsWith("MANIFEST.MF") && !je.getName().endsWith(".json")) continue;
				File fl = Paths.get(destdir, je.getName()).normalize().toFile();
				if (!fl.exists())
				{
					fl.getParentFile().mkdirs();
				}
				if (je.isDirectory())
				{
					continue;
				}
				try (InputStream is = jarfile.getInputStream(je); FileOutputStream fo = new FileOutputStream(fl))
				{
					while (is.available() > 0)
					{
						fo.write(is.read());
					}
				}
				if (fl.getName().endsWith(".html"))
				{
					allTemplates.put(dirName + "/" + je.getName(), fl);
				}
				if (je.getName().endsWith(".spec"))
				{
					JSONObject json = new JSONObject(Utils.getTXTFileContent(jarfile.getInputStream(je), Charset.forName("UTF8"), true));
					List<String> scripts = new ArrayList<String>();
					if (json.has("serverscript"))
					{
						scripts.add(json.getString("serverscript"));
					}
					if (json.has("ng2Config"))
					{
						JSONObject configJSON = json.getJSONObject("ng2Config");
						if (configJSON.has("dependencies"))
						{
							JSONObject dependenciesJSON = configJSON.getJSONObject("dependencies");
							if (dependenciesJSON.has("serverscript"))
							{
								scripts.add(dependenciesJSON.getString("serverscript"));
							}
						}
					}
					scripts.forEach((String path) -> {
						String serverScriptPath = path.substring(path.indexOf("/") + 1);
						ZipEntry serverScriptEntry = jarfile.getEntry(serverScriptPath);
						File destScriptFile = new File(destdir, serverScriptPath);
						try (InputStream is = jarfile.getInputStream(serverScriptEntry); FileOutputStream fo = new FileOutputStream(destScriptFile))
						{
							while (is.available() > 0)
							{
								fo.write(is.read());
							}
						}
						catch (Exception ex)
						{
							ServoyLog.logError("error extracting serverside script " + path + " from jar/zip: " + file, ex);
						}
					});
				}
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError("IO exception when extracting from file " + file.getAbsolutePath(), e);
		}
	}

	private File createNewWarFile() throws ExportException
	{
		File warFile = new File(exportModel.getWarFileName());
		if (warFile.exists() && !warFile.delete())
		{
			throw new ExportException("Can't delete the existing war file " + exportModel.getWarFileName());
		}

		try
		{
			if (warFile.getParentFile() != null && !warFile.getParentFile().exists()) warFile.getParentFile().mkdirs();
			if (!warFile.createNewFile())
			{
				throw new ExportException("Can't create the file " + exportModel.getWarFileName());
			}
		}
		catch (IOException e)
		{
			throw new ExportException("Can't create the file " + exportModel.getWarFileName(), e);
		}
		return warFile;
	}

	private File createTempDir() throws ExportException
	{
		File tmpDir = new File(System.getProperty("java.io.tmpdir"));

		File tmpWarDir = new File(tmpDir, "warexport" + System.currentTimeMillis() + "/");
		if (!tmpWarDir.mkdirs())
		{
			throw new ExportException("Can't create the temp dir  " + tmpWarDir);
		}
		return tmpWarDir;
	}

	protected void copyActiveSolution(IProgressMonitor monitor) throws ExportException
	{
		try
		{
			IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
			FlattenedSolution solution = servoyModel.getFlattenedSolution();
			Solution[] modules = solution.getModules();
			if (exportModel.isExportNonActiveSolutions() && !exportModel.getNonActiveSolutions().isEmpty())
			{
				List<String> noneActiveSolutions = exportModel.getNonActiveSolutions();
				Solution[] copy = null;
				int start = 0;
				if (modules != null && modules.length > 0)
				{
					start = modules.length;
					copy = new Solution[start + noneActiveSolutions.size()];
					System.arraycopy(modules, 0, copy, 0, modules.length);
				}
				else
				{
					copy = new Solution[noneActiveSolutions.size()];
				}
				for (String name : noneActiveSolutions)
				{
					ServoyProject servoyProject = servoyModel.getServoyProject(name);
					if (servoyProject == null || servoyProject.getSolution() == null)
					{
						throw new ExportException("Can't export non-active soluton with the name: " + name +
							" it couildn't be found in the workspace or the solution couldnt be loaded");
					}
					if (!Utils.equalObjects(servoyProject.getResourcesProject() != null ? servoyProject.getResourcesProject().getProject().getName() : null,
						servoyModel.getActiveResourcesProject() != null ? servoyModel.getActiveResourcesProject().getProject().getName() : null))
					{
						ServoyLog.logWarning("Solution '" + name +
							"' has different resources project than active solution, this could lead to unpredictable behavior at runtime.", null);
					}
					copy[start++] = servoyProject.getSolution();
				}
				modules = copy;
			}
			SolutionSerializer.writeRuntimeSolution(null, new File(tmpWarDir, "WEB-INF/solution.runtime"), solution.getSolution(),
				ApplicationServerRegistry.get().getDeveloperRepository(), modules);
			exportSolution(monitor, tmpWarDir.getCanonicalPath(), solution.getSolution(), false);

			File preImportFolder = new File(tmpWarDir, "WEB-INF/preImport");
			File postImportFolder = new File(tmpWarDir, "WEB-INF/postImport");
			for (ServoyProject sp : servoyModel.getModulesOfActiveProject())
			{
				File destinationFolder = null;
				if (SolutionMetaData.isPreImportHook(sp.getSolution()))
				{
					destinationFolder = preImportFolder;
				}
				else if (SolutionMetaData.isPostImportHook(sp.getSolution()))
				{
					destinationFolder = postImportFolder;
				}

				if (destinationFolder != null)
				{
					if (!destinationFolder.exists()) destinationFolder.mkdir();
					SolutionSerializer.writeRuntimeSolution(null, new File(destinationFolder, sp.getSolution().getName() + ".runtime"), sp.getSolution(),
						ApplicationServerRegistry.get().getDeveloperRepository(),
						sp.getSolution().getReferencedModulesRecursive(new HashMap<String, Solution>()).values().toArray(new Solution[0]));
				}
			}

		}
		catch (ExportException e)
		{
			throw e;
		}
		catch (Exception ex)
		{
			throw new ExportException("Cannot write the active solution in war file: " + ex.getMessage(), ex);
		}

		try
		{
			File importProperties = new File(tmpWarDir, "WEB-INF/import.properties");
			Properties prop = new Properties();
			prop.setProperty("overwriteGroups", Boolean.toString(exportModel.isOverwriteGroups()));
			prop.setProperty("allowSQLKeywords", Boolean.toString(exportModel.isAllowSQLKeywords()));
			prop.setProperty("overrideSequenceTypes", Boolean.toString(exportModel.isOverrideSequenceTypes()));
			prop.setProperty("overrideDefaultValues", Boolean.toString(exportModel.isOverrideDefaultValues()));
			prop.setProperty("insertNewI18NKeysOnly", Boolean.toString(exportModel.isInsertNewI18NKeysOnly()));
			prop.setProperty("importUserPolicy", Integer.toString(exportModel.getImportUserPolicy()));
			prop.setProperty("addUsersToAdminGroup", Boolean.toString(exportModel.isAddUsersToAdminGroup()));
			prop.setProperty("allowDataModelChange", exportModel.getAllowDataModelChanges());
			prop.setProperty("updateSequences", Boolean.toString(exportModel.isUpdateSequences()));
			prop.setProperty("skipDatabaseViewsUpdate", Boolean.toString(exportModel.isSkipDatabaseViewsUpdate()));

			try (FileWriter writer = new FileWriter(importProperties))
			{
				prop.store(writer, "import properties");
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	/**
	 * @param tmpWarDir
	 */
	private void exportAdminUser()
	{
		if (exportModel.getDefaultAdminUser() != null && exportModel.getDefaultAdminPassword() != null)
		{
			try
			{
				File adminProperties = new File(tmpWarDir, "WEB-INF/admin.properties");
				Properties prop = new Properties();
				prop.setProperty("defaultAdminUser", exportModel.getDefaultAdminUser());
				prop.setProperty("defaultAdminPassword", SecuritySupport.encrypt(exportModel.getDefaultAdminPassword()));
				if (exportModel.isUseAsRealAdminUser())
				{
					prop.setProperty("useAsRealAdminUser", "true");
				}
				try (FileWriter writer = new FileWriter(adminProperties))
				{
					prop.store(writer, "admin properties");
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	/**
	 * @param tmpWarDir
	 */
	private void copySecAndDBIFiles(File tmpWarDir)
	{
		IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
		ServoyProject activeProject = servoyModel.getActiveProject();
		Map<String, List<String>> neededServerTables = TableDefinitionUtils.getNeededServerTables(activeProject == null ? null : activeProject.getSolution(),
			exportModel.isExportReferencedModules(), exportModel.isExportI18NData());

		DataModelManager dataModelManager = servoyModel.getDataModelManager();
		File secDir = new File(tmpWarDir, "WEB-INF/security");
		secDir.mkdirs();
		File dbDir = new File(tmpWarDir, "WEB-INF/db");
		dbDir.mkdirs();

		try
		{
			copyFileIfExists(dataModelManager.getSecurityFile(), new File(secDir, DataModelManager.SECURITY_FILENAME));
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}

		neededServerTables.entrySet().forEach(entry -> {
			String serverName = entry.getKey();
			List<String> tablesNeeded = entry.getValue();
			try
			{
				for (IFile tableSecfile : TableDefinitionUtils.getServerTableinfo(serverName, DataModelManager.SECURITY_FILE_EXTENSION, tablesNeeded,
					exportModel.isExportAllTablesFromReferencedServers()))
				{
					File serverSecDir = new File(secDir, serverName);
					serverSecDir.mkdirs();
					copyFileIfExists(tableSecfile, new File(serverSecDir, tableSecfile.getName()));
				}

				IFile serverDBIFile = dataModelManager.getServerDBIFile(serverName);
				copyFileIfExists(serverDBIFile, new File(dbDir, serverDBIFile.getName()), () -> DatabaseUtils.serializeServerSettings(ServerSettings.DEFAULT));
				File serverDbDir = new File(dbDir, serverName);
				serverDbDir.mkdirs();
				outer : for (IFile tableDBIfile : TableDefinitionUtils.getServerTableinfo(serverName, DataModelManager.COLUMN_INFO_FILE_EXTENSION, tablesNeeded,
					exportModel.isExportAllTablesFromReferencedServers()))
				{
					// test first if this is not a legacy table with a pk that has a servoy sequence.
					TableDef tableDef = TableDefinitionUtils.loadTableDef(tableDBIfile);
					if (tableDef != null)
					{
						for (ColumnInfoDef columnDef : tableDef.columnInfoDefSet)
						{
							if (columnDef.autoEnterType == ColumnInfo.SEQUENCE_AUTO_ENTER && columnDef.autoEnterSubType == ColumnInfo.SERVOY_SEQUENCE)
							{
								continue outer;
							}
						}
					}
					copyFileIfExists(tableDBIfile, new File(serverDbDir, tableDBIfile.getName()));
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		});
	}

	private static void copyFileIfExists(IFile src, File dest) throws IOException, CoreException
	{
		copyFileIfExists(src, dest, null);
	}

	private static void copyFileIfExists(IFile src, File dest, Supplier<String> defaultContents) throws IOException, CoreException
	{
		if (src.exists())
		{
			try (FileOutputStream fos = new FileOutputStream(dest))
			{
				IOUtils.copy(src.getContents(true), fos);
			}
		}
		else if (defaultContents != null)
		{
			try (FileOutputStream fos = new FileOutputStream(dest))
			{
				IOUtils.write(defaultContents.get(), fos, Charset.forName("UTF-8"));
			}
		}
	}

	protected void createTomcatContextXML() throws ExportException
	{
		String fileName = exportModel.getTomcatContextXMLFileName();
		if (fileName != null && !"".equals(fileName.trim()))
		{
			File source = new File(fileName);
			if (source.exists())
			{
				File metaDir = new File(tmpWarDir, "META-INF");
				metaDir.mkdir();
				File contextFile = new File(tmpWarDir, "META-INF/context.xml");
				try
				{
					FileUtils.copyFile(source, contextFile);
				}
				catch (IOException e)
				{
					throw new ExportException("Can't copy tomcat context file: " + fileName, e);
				}
			}
			else
			{
				throw new ExportException("Given tomcat context file does not exists: " + fileName);
			}
		}
		else
		{
			if (!exportModel.isCreateTomcatContextXML()) return;
			try
			{
				File metaDir = new File(tmpWarDir, "META-INF");
				metaDir.mkdir();
				File contextFile = new File(tmpWarDir, "META-INF/context.xml");
				contextFile.createNewFile();
				try (FileWriter writer = new FileWriter(contextFile))
				{
					String fileContent = "<Context ";
					if (exportModel.isAntiResourceLocking()) fileContent += "antiResourceLocking=\"true\" ";
					if (exportModel.isClearReferencesStatic()) fileContent += "clearReferencesStatic=\"true\" ";
					if (exportModel.isClearReferencesStopThreads()) fileContent += "clearReferencesStopThreads=\"true\" ";
					if (exportModel.isClearReferencesStopTimerThreads()) fileContent += "clearReferencesStopTimerThreads=\"true\" ";
					fileContent += "></Context>";
					writer.write(fileContent);
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	private void exportSolution(IProgressMonitor monitor, String tmpWarDir, Solution activeSolution, boolean exportSolution)
		throws CoreException, ExportException
	{
		int totalDuration = IProgressMonitor.UNKNOWN;
		if (exportModel.getModulesToExport() != null) totalDuration = (int)(1.42 * exportModel.getModulesToExport().length); // make the main export be 70% of the time, leave the rest for sample data
		monitor.beginTask("Exporting solution", totalDuration);

		try
		{
			SolutionExporter.exportSolutionToFile(activeSolution, new File(tmpWarDir, "WEB-INF/solution.servoy"), exportModel,
				new EclipseExportI18NHelper(new WorkspaceFileAccess(ResourcesPlugin.getWorkspace())), userChannel,
				null, TableDefinitionUtils.hasDbDownErrorMarkersThatCouldBeIgnoredOnExport(exportModel.getModulesToExport()), false, exportSolution);
			monitor.done();
		}
		catch (RepositoryException e)
		{
			throw new ExportException(e.getMessage(), e);
		}
		catch (JSONException jsonex)
		{
			throw new ExportException("Bad JSON file structure: " + jsonex.getMessage(), jsonex);
		}
		catch (IOException ioex)
		{
			throw new ExportException("Exception getting files: " + ioex.getMessage(), ioex);
		}
	}


	/**
	 * @param tmpWarDir
	 * @throws ExportException
	 */
	private void addServoyProperties() throws ExportException
	{
		if (exportModel.getServoyPropertiesFileName() == null)
		{
			try
			{
				exportModel.generatePropertiesFileContent(new File(tmpWarDir, "WEB-INF/servoy.properties"));
			}
			catch (FileNotFoundException fileNotFoundException)
			{
				throw new ExportException("Couldn't find file " + tmpWarDir.getAbsolutePath() + "/WEB-INF/servoy.properties", fileNotFoundException);
			}
			catch (IOException e)
			{
				throw new ExportException("Couldn't generate the properties file", e);
			}
		}
		else
		{
			File sourceFile = new File(exportModel.getServoyPropertiesFileName());
			changeAndWritePropertiesFile(tmpWarDir, sourceFile);
		}
	}

	private void copyWebXml() throws ExportException
	{
		// copy war web.xml
		File webXMLFile = new File(tmpWarDir, "WEB-INF/web.xml");
		String name = exportModel.getWebXMLFileName();
		InputStream webXmlIS = null;
		if (name == null)
		{
			webXmlIS = WarExporter.class.getResourceAsStream("resources/web.xml");
		}
		else try
		{
			String message = exportModel.checkWebXML();
			if (message != null)
			{
				throw new ExportException(message);
			}
			webXmlIS = new FileInputStream(name);
		}
		catch (FileNotFoundException fnfe)
		{
			throw new ExportException("Can't create the web.xml file, couldn't read" + name, fnfe);
		}
		try (InputStream is = webXmlIS; BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(webXMLFile)))
		{
			copyStream(webXmlIS, bos);
		}
		catch (Exception e)
		{
			throw new ExportException("Can't create the web.xml file: " + webXMLFile.getAbsolutePath(), e);
		}
	}


	private void copyLog4jConfigurationFile() throws ExportException
	{
		// copy war log4j configuration file
		String name = exportModel.getLog4jConfigurationFile();
		InputStream logXmlIS = null;
		if (name == null)
		{
			name = "resources/log4j.xml";
			logXmlIS = WarExporter.class.getResourceAsStream(name);
		}
		else try
		{
			String message = exportModel.checkLog4jConfigurationFile();
			if (message != null)
			{
				throw new ExportException(message);
			}
			logXmlIS = new FileInputStream(name);
		}
		catch (FileNotFoundException fnfe)
		{
			throw new ExportException("Can't create the log4j configuration file, couldn't read" + name, fnfe);
		}

		// Log4j will search for a file that starts with "log4j2" in the WEB-INF directory.
		File log4jConfigurationFile = new File(tmpWarDir, "WEB-INF/log4j2." + FilenameUtils.getExtension(name));
		try (InputStream is = logXmlIS; BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(log4jConfigurationFile)))
		{
			copyStream(is, bos);
		}
		catch (Exception e)
		{
			throw new ExportException("Can't create the log4j configuration file: " + log4jConfigurationFile.getAbsolutePath(), e);
		}
	}

	private static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException
	{
		byte[] buffer = new byte[8096];
		int read = inputStream.read(buffer);
		while (read != -1)
		{
			outputStream.write(buffer, 0, read);
			read = inputStream.read(buffer);
		}
	}

	private void moveSlf4j(final File targetLibDir) throws ExportException
	{
		// move the slf4j outside of the WEB-INF/lib to /lib/, its only used in the client
		File slf4j = new File(targetLibDir, "slf4j-jdk14.jar");
		copyFile(slf4j, new File(tmpWarDir, "lib/slf4j-jdk14.jar"));
		slf4j.delete();
	}

	/**
	 * @param monitor
	 * @param appServerDir
	 * @param targetLibDir
	 * @throws ExportException
	 */
	private void copyDrivers(String appServerDir, final File targetLibDir) throws ExportException
	{
		List<String> drivers = exportModel.getDrivers();
		File srcDriverDir = new File(appServerDir, "drivers");
		for (String driverFileName : drivers)
		{
			copyFile(new File(srcDriverDir, driverFileName), new File(targetLibDir, driverFileName));
		}
	}

	private File copyStandardLibs(String appServerDir) throws ExportException
	{
		// copy lib dir (excluding images)
		final File libDir = new File(appServerDir, "lib");
		final File targetLibDir = new File(tmpWarDir, "WEB-INF/lib");
		targetLibDir.mkdirs();
		copyDir(libDir, targetLibDir, false);

//		// copy the template handler
//		copyFile(new File(appServerDir, "server/lib/template-handler.jar"), new File(targetLibDir, "template-handler.jar"));

		// delete the servlet.jar that one isn't allowed.
		new File(targetLibDir, "servlet-api.jar").delete();
		new File(targetLibDir, "jsp-api.jar").delete();
		// delete the tomcat boostrapper, also not needed in a war file
		new File(targetLibDir, "server-bootstrap.jar").delete();
		new File(targetLibDir, "tomcat-juli.jar").delete();
		return targetLibDir;
	}

	private void copyPlugins(String appServerDir) throws ExportException
	{
		// copy the plugins
		File pluginsDir = new File(tmpWarDir, "plugins");
		pluginsDir.mkdirs();
		List<String> plugins = exportModel.getPlugins();
		File pluginProperties = new File(pluginsDir, "plugins.properties");
		try (Writer fw = new FileWriter(pluginProperties))
		{
			Set<File> writtenFiles = new HashSet<File>();
			for (String plugin : plugins)
			{
				String pluginName = "plugins/" + plugin;
				File pluginFile = new File(appServerDir, pluginName);

				if (pluginFile.isDirectory())
				{
					copyDir(pluginName, pluginFile, tmpWarDir, fw, writtenFiles, true);
				}
				else
				{
					writeFileEntry(fw, pluginFile, plugin, writtenFiles);

					copyFile(pluginFile, new File(tmpWarDir, pluginName));

					copyJnlp(tmpWarDir, appServerDir, pluginName + ".jnlp", fw, writtenFiles);

					if (pluginName.toLowerCase().endsWith(".jar") || pluginName.toLowerCase().endsWith(".zip"))
					{
						String pluginLibDir = pluginName.substring(0, pluginName.length() - 4);
						File pluginLibDirFile = new File(appServerDir, pluginLibDir);
						copyDir(pluginLibDir, pluginLibDirFile, tmpWarDir, fw, writtenFiles, false);

						if (pluginName.toLowerCase().endsWith(".jar"))
						{
							List<String> classPath = JarManager.getManifestClassPath(pluginFile.toURI().toURL());
							if (!classPath.isEmpty())
							{
								ServoyLog.logInfo("Plugin " + pluginName + ", Copy classpath " + classPath);
								copyPluginJars(tmpWarDir, appServerDir, fw, writtenFiles, classPath);
							}
						}
					}
				}
			}
		}
		catch (IOException e1)
		{
			throw new ExportException("Error creating plugins dir", e1);
		}
	}

	private static void copyDir(String dirName, File dirFile, File tmpWarDir, Writer propertiesWriter, Set<File> writtenFiles, boolean recursive)
		throws ExportException, IOException
	{
		if (dirFile.exists() && dirFile.isDirectory())
		{
			Set<File> copiedFiles = copyDir(dirFile, new File(tmpWarDir, dirName), recursive);
			for (File file : copiedFiles)
			{
				String fileName = file.getAbsolutePath().replace('\\', '/');
				int index = fileName.indexOf("plugins/");
				if (index != -1)
				{
					fileName = fileName.substring(index + "plugins/".length());
				}
				writeFileEntry(propertiesWriter, file, fileName, writtenFiles);
			}
		}
	}

	private void copyRootWebappFiles(String appServerDir) throws ExportException
	{
		File webAppDir = new File(appServerDir, "server/webapps/ROOT");
		// copy first the standard webapp dir of the app server
		copyDir(webAppDir, tmpWarDir, true);

	}

	private void changeAndWritePropertiesFile(File tmpWarDir, File sourceFile) throws ExportException
	{
		try (FileInputStream fis = new FileInputStream(sourceFile);
			FileOutputStream fos = new FileOutputStream(new File(tmpWarDir, "WEB-INF/servoy.properties")))
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
							new License(properties.getProperty("license." + i + ".company_name"), code, properties.getProperty("license." + i + ".licenses")));
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

	private static void writeFileEntry(Writer fw, File file, String name, Set<File> writtenFiles) throws IOException
	{

		if (writtenFiles.add(file) && file.exists())
		{
			fw.write(name);
			fw.write('=');
			fw.write(Long.toString(file.lastModified()));
			fw.write(':');
			fw.write(Long.toString(file.length()));
			fw.write('\n');
		}
	}

	private void zipDirectory(File directory, File zip) throws ExportException
	{
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip)))
		{
			zip(directory, directory, zos);
		}
		catch (Exception e)
		{
			throw new ExportException("Can't create the war file " + zip, e);
		}
	}

	private void zip(File directory, File base, ZipOutputStream zos) throws IOException
	{
		File[] files = directory.listFiles();
		byte[] buffer = new byte[8192];
		int read = 0;
		for (File file : files)
		{
			if (file.isDirectory())
			{
				zip(file, base, zos);
			}
			else
			{
				FileInputStream in = new FileInputStream(file);
				ZipEntry entry = new ZipEntry(file.getPath().substring(base.getPath().length() + 1).replace('\\', '/'));
				zos.putNextEntry(entry);
				while (-1 != (read = in.read(buffer)))
				{
					zos.write(buffer, 0, read);
				}
				zos.closeEntry();
				in.close();
			}
		}
	}

	private void copyJnlp(File tmpWarDir, String appServerDir, String pluginJnlpName, Writer fw, Set<File> writtenFiles) throws ExportException, IOException
	{
		if (pluginJnlpName.startsWith("/servoy-client/"))
		{
			pluginJnlpName = pluginJnlpName.substring(15);
		}
		File pluginJarJnlpFile = new File(appServerDir, pluginJnlpName);
		if (pluginJarJnlpFile.exists())
		{
			copyFile(pluginJarJnlpFile, new File(tmpWarDir, pluginJnlpName));
			// parse the jnlp and copy all the referenced jars over.
			Document document = getDocument(pluginJarJnlpFile);
			if (document != null)
			{
				List<String> jarNames = new ArrayList<String>();
				List<String> jnlpNames = new ArrayList<String>();
				parseJarNames(document.getChildNodes(), jarNames, jnlpNames);
				copyPluginJars(tmpWarDir, appServerDir, fw, writtenFiles, jarNames);

				for (String jnlpName : jnlpNames)
				{
					copyJnlp(tmpWarDir, appServerDir, jnlpName, fw, writtenFiles);
				}
			}
			else
			{
				ServoyLog.logError("Plugin jnlp file " + pluginJarJnlpFile + " couldn't be parsed; nothing copied", new RuntimeException());
			}
		}
	}

	private void copyPluginJars(File tmpWarDir, String appServerDir, Writer fw, Set<File> writtenFiles, List<String> jarNames)
		throws ExportException, IOException
	{
		for (String jarName : jarNames)
		{
			// ignore everything copied from lib, those are moved to WEB-INF/lib later on
			if (jarName.startsWith("/lib/")) continue;
			File jarFile = new File(appServerDir, jarName);
			File jarTargetFile = new File(tmpWarDir, jarName);
			jarTargetFile.getParentFile().mkdirs();
			copyFile(jarFile, jarTargetFile);
			int index = jarName.indexOf("plugins/");
			if (index != -1)
			{
				jarName = jarName.substring(index + "plugins/".length());
			}
			writeFileEntry(fw, jarFile, jarName, writtenFiles);
		}
	}

	private void checkDuplicateJar(File jarFile, Map<String, TreeMap<String, List<File>>> dependenciesVersions, TreeMap<String, List<File>> allJars)
	{
		String jarName = null;
		String version = null;
		try
		{
			Pair<String, String> pair = JarManager.getNameAndVersion(jarFile.toURI().toURL());
			if (pair != null)
			{
				jarName = pair.getLeft();
				version = pair.getRight();
			}
		}
		catch (MalformedURLException e)
		{
			ServoyLog.logError("Cannot check jar name and version in the manifest: " + jarName, e);
		}
		if (jarName == null)
		{
			jarName = "";
			String[] jarNameParts = jarFile.getName().substring(0, jarFile.getName().indexOf(".jar")).split("-");
			for (String part : jarNameParts)
			{
				if (part.contains(".") && Character.isDigit(part.charAt(0)))
				{
					//it is the version number
					break;
				}
				else
				{
					jarName += !jarName.isEmpty() ? "-" + part : part;
				}
			}
		}
		version = checkVersionString(version);
		if (!dependenciesVersions.containsKey(jarName))
		{
			dependenciesVersions.put(jarName, new TreeMap<>(VersionComparator.INSTANCE));
		}
		String name = jarFile.getName().substring(0, jarFile.getName().indexOf(".jar")).replaceAll("-|_|\\d|\\.", "");
		if (!allJars.containsKey(name))
		{
			allJars.put(name, new ArrayList<>());
		}
		if (jarFile.getPath().contains("plugins"))
			allJars.get(name).add(jarFile);

		TreeMap<String, List<File>> vFiles = dependenciesVersions.get(jarName);
		if (!vFiles.containsKey(version))
		{
			vFiles.put(version, new ArrayList<File>());
		}
		vFiles.get(version).add(jarFile);
	}

	private String checkVersionString(String v)
	{
		if (v == null)
		{
			return "0";
		}
		String version = v.contains(" ") ? v.split(" ")[0] : v;
		if (version.contains("-"))
		{
			version = version.split("-")[0];
		}
		if (version.split("\\.").length > 4)
		{
			String[] parts = version.split("\\.");
			version = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
		}
		try
		{
			Version.parseVersion(version);
		}
		catch (IllegalArgumentException e)
		{
			version = version.replaceAll("[^\\d.]", "").replaceAll("\\s", "");
			if ("".equals(version))
			{
				return "0";
			}
			else
			{
				try
				{
					//make sure we can parse it
					Version.parseVersion(version);
				}
				catch (IllegalArgumentException ex)
				{
					return "0";
				}
			}
		}
		return version;
	}

	private void parseJarNames(NodeList childNodes, List<String> jarNames, List<String> jnlpNames)
	{
		for (int i = 0; i < childNodes.getLength(); i++)
		{
			Node node = childNodes.item(i);
			if (node.getNodeName().equalsIgnoreCase("jar"))
			{
				NamedNodeMap attributes = node.getAttributes();
				Node href = attributes.getNamedItem("href");
				if (href != null)
				{
					jarNames.add(href.getNodeValue());
				}
			}
			else if (node.getNodeName().equalsIgnoreCase("extension"))
			{
				NamedNodeMap attributes = node.getAttributes();
				Node href = attributes.getNamedItem("href");
				if (href != null)
				{
					jnlpNames.add(href.getNodeValue());
				}
			}
			parseJarNames(node.getChildNodes(), jarNames, jnlpNames);
		}
	}

	private Document getDocument(File jnlpFile)
	{
		try
		{
			String contents = Utils.getTXTFileContent(jnlpFile);
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			return docBuilder.parse(new InputSource(new StringReader(contents)));
		}
		catch (Exception e)
		{
			ServoyLog.logError("Error creating parsing the jnlp file: " + jnlpFile, e);
		}
		return null;
	}

	private static void deleteDirectory(File path)
	{
		if (path.exists())
		{
			// always call delete on exit so it will be tried to delete again later on
			// delete on exit uses reversed order so you have to call it first for the dir then the files.
			path.deleteOnExit();
			File[] files = path.listFiles();
			for (File file : files)
			{
				if (file.isDirectory())
				{
					deleteDirectory(file);
				}
				else
				{
					if (!file.delete())
					{
						file.deleteOnExit();
					}
				}
			}
		}
		path.delete();
	}

	private static Set<File> copyDir(File sourceDir, File destDir, boolean recusive, Map<String, File> allTemplates, Set<String> excludedResourcesByName,
		boolean specFilesOnly)
		throws ExportException
	{
		Set<File> writtenFiles = new HashSet<File>();
		copyDir(sourceDir, destDir, recusive, writtenFiles, allTemplates, excludedResourcesByName, specFilesOnly);
		return writtenFiles;
	}

	private static Set<File> copyDir(File sourceDir, File destDir, boolean recusive) throws ExportException
	{
		return copyDir(sourceDir, destDir, recusive, null, null, false);
	}

	private static void copyDir(File sourceDir, File destDir, boolean recusive, Set<File> writtenFiles, Map<String, File> allTemplates,
		Set<String> excludedResourcesByName, boolean specFilesOnly) throws ExportException
	{
		if (!destDir.exists() && !destDir.mkdirs()) throw new ExportException("Can't create destination dir: " + destDir);
		File[] listFiles = sourceDir.listFiles();
		if (listFiles == null) return;
		for (File file : listFiles)
		{
			if (excludedResourcesByName != null && excludedResourcesByName.contains(file.getName())) continue; // skip it; for example it could be a .git dir that should not get inside .war contents as it is useless there and can have a large size

			if (file.isDirectory())
			{
				if (recusive) copyDir(file, new File(destDir, file.getName()), recusive, writtenFiles, allTemplates, excludedResourcesByName, specFilesOnly);
			}
			else
			{
				if (specFilesOnly && !file.getName().endsWith(".spec") && !file.getName().endsWith("MANIFEST.MF") && !file.getName().endsWith(".json"))
					continue;
				File newFile = new File(destDir, file.getName());
				copyFile(file, newFile);
				if (allTemplates != null && newFile.getName().endsWith(".html"))
				{
					String path = newFile.getPath();
					path = path.replace('\\', '/');
					path = path.substring(path.indexOf("/warexport") + 1);
					path = path.substring(path.indexOf("/") + 1);
					allTemplates.put(path, newFile);
				}
				writtenFiles.add(file);
				if (file.getName().endsWith(".spec"))
				{
					JSONObject json = new JSONObject(Utils.getTXTFileContent(file));
					List<String> scripts = new ArrayList<String>();
					if (json.has("serverscript"))
					{
						scripts.add(json.getString("serverscript"));
					}
					if (json.has("ng2Config"))
					{
						JSONObject configJSON = json.getJSONObject("ng2Config");
						if (configJSON.has("dependencies"))
						{
							JSONObject dependenciesJSON = configJSON.getJSONObject("dependencies");
							if (dependenciesJSON.has("serverscript"))
							{
								scripts.add(dependenciesJSON.getString("serverscript"));
							}
						}
					}
					scripts.forEach((String path) -> {
						String fileName = path.substring(path.lastIndexOf("/") + 1);
						File serverScriptFile = new File(file.getParentFile(), fileName);
						File newServerScriptFile = new File(destDir, fileName);
						try
						{
							copyFile(serverScriptFile, newServerScriptFile);
						}
						catch (ExportException e)
						{
							ServoyLog.logError(e);
						}
						writtenFiles.add(serverScriptFile);
					});
				}
			}
		}
	}

	private static void copyFile(File sourceFile, File destFile) throws ExportException
	{
		if (!sourceFile.exists() || sourceFile.length() == 0)
		{
			return;
		}
		try
		{
			if (!destFile.getParentFile().exists())
			{
				destFile.getParentFile().mkdirs();
			}
			if (!destFile.exists())
			{
				destFile.createNewFile();
			}
			try (FileInputStream fis = new FileInputStream(sourceFile))
			{
				String compileLessWithNashorn = null;
				if (sourceFile.getName().endsWith(".less") && (compileLessWithNashorn = LessCompiler.compileLess(fis)) != null)
				{
					File compiledLessFile = destFile;
					PrintWriter printWriter = new PrintWriter(compiledLessFile);
					printWriter.println(compileLessWithNashorn);
					printWriter.close();
				}
				else
				{
					try (FileChannel source = fis.getChannel();
						FileOutputStream fos = new FileOutputStream(destFile);
						FileChannel destination = fos.getChannel())
					{
						if (destination != null && source != null)
						{
							destination.transferFrom(source, 0, source.size());
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			throw new ExportException("Can't copy file from " + sourceFile + " to " + destFile, e);
		}

	}

	/**
	 * Check if all WAR_LIBS can be found in the specified plugin locations.
	 * @return message to add path to the missing jar
	 */
	public String searchExportedPlugins()
	{
		pluginFiles = new HashSet<File>();
		List<String> pluginLocations = new ArrayList<String>();
		pluginLocations.addAll(exportModel.getPluginLocations());

		File eclipseParent = null;
		File userDir = new File(System.getProperty("user.dir"));
		if (System.getProperty("eclipse.home.location") != null)
		{
			eclipseParent = new File(URI.create(System.getProperty("eclipse.home.location").replaceAll(" ", "%20")));
			if (eclipseParent.exists())
			{
				//first check the plugins folder of eclipse home
				pluginLocations.add(new File(eclipseParent, "/plugins").getAbsolutePath().toString());
			}
		}

		LinkedHashMap<File, String[]> dirListings = new LinkedHashMap<>();
		pluginLocations.forEach(location -> {
			File pluginLocation = new File(location);
			if (!pluginLocation.exists())
			{
				if (!pluginLocation.isAbsolute() && !pluginLocation.exists())
				{
					pluginLocation = new File(userDir, location);
				}
				if (!pluginLocation.exists())
				{
					System.err.println("Trying userDir " + userDir + " as parent for " + location + " but is not found");
				}
			}

			if (!pluginLocation.isDirectory())
			{
				System.err.println(pluginLocation.getAbsolutePath() + " is not a directory.");
			}
			else
			{
				dirListings.put(pluginLocation, pluginLocation.list());
			}
		});
		for (String libName : WAR_LIBS)
		{
			boolean found = false;
			Iterator<Entry<File, String[]>> listings = dirListings.entrySet().iterator();
			while (!found && listings.hasNext())
			{
				Collection<File> libs = new ArrayList<File>();
				Entry<File, String[]> entry = listings.next();
				String[] value = entry.getValue();
				for (String name : value)
				{
					if (FilenameUtils.wildcardMatch(name, libName, IOCase.INSENSITIVE))
					{
						libs.add(new File(entry.getKey(), name));
					}
				}
				Iterator<File> iterator = libs.iterator();
				if (libs != null && libs.size() > 0)
				{
					File f = iterator.next();
					if (libs.size() > 1)
					{
						//in this case we need to copy the newest
						List<File> sortedLibs = new ArrayList<File>(libs);
						Collections.sort(sortedLibs, new Comparator<File>()
						{
							@Override
							public int compare(File file0, File file1)
							{
								return Long.compare(file0.lastModified(), file1.lastModified());
							}
						});
						f = sortedLibs.get(sortedLibs.size() - 1);
						ServoyLog.logInfo("WAR EXPORT: More versions of lib " + libName + " found, will copy " + f.getAbsolutePath() + " to the war file.");
					}
					pluginFiles.add(f);
					found = true;
					break;
				}
			}
			if (!found) return libName;
		}
		return null;
	}

	private void createDeployPropertiesFile() throws ExportException
	{
		File deployPropertiesFile = new File(tmpWarDir, "WEB-INF/deploy.properties");
		Properties properties = new Properties();
		properties.put("isOverwriteDeployedDBServerProperties", String.valueOf(exportModel.isOverwriteDeployedDBServerProperties()));
		properties.put("isOverwriteDeployedServoyProperties", String.valueOf(exportModel.isOverwriteDeployedServoyProperties()));
		properties.setProperty("automaticallyUpgradeRepository", Boolean.toString(exportModel.isAutomaticallyUpgradeRepository()));
		properties.setProperty("serverBuildDate", String.valueOf(System.currentTimeMillis()));
		properties.setProperty("zoneId", ZoneId.systemDefault().getId());
		if (exportModel.isExportActiveSolution())
		{
			IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
			FlattenedSolution solution = servoyModel.getFlattenedSolution();
			properties.setProperty("mainsolution", solution.getName());
		}
		try (FileOutputStream fos = new FileOutputStream(deployPropertiesFile))
		{
			properties.store(fos, "");
		}
		catch (Exception e)
		{
			throw new ExportException("Couldn't generate the deploy.properties file", e);
		}
	}
}
