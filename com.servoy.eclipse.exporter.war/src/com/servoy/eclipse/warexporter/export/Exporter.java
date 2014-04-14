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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.warexporter.ui.wizard.ServerConfiguration;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IBeanManagerInternal;
import com.servoy.j2db.ILAFManagerInternal;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator;
import com.servoy.j2db.server.ngclient.startup.resourceprovider.ComponentResourcesExporter;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.JarManager;
import com.servoy.j2db.util.JarManager.ExtensionResource;
import com.servoy.j2db.util.SortedProperties;
import com.servoy.j2db.util.Utils;


/**
 * Class that creates the WAR file.
 * 
 * @author jcompagner
 * @since 6.1
 */
public class Exporter
{

	private static final String COMPONENTS_DIR_NAME = "components";
	private final ExportWarModel exportModel;

	/**
	 * @param exportModel
	 */
	public Exporter(ExportWarModel exportModel)
	{
		this.exportModel = exportModel;
	}

	/**
	 * @param monitor 
	 * @return
	 */
	public void doExport(IProgressMonitor monitor) throws ExportException
	{
		File warFile = createNewWarFile(monitor);
		File tmpWarDir = createTempDir(monitor);

		String appServerDir = ApplicationServerRegistry.get().getServoyApplicationServerDirectory();
		copyRootWebappFiles(monitor, tmpWarDir, appServerDir);
		copyBeans(monitor, tmpWarDir, appServerDir);
		copyPlugins(monitor, tmpWarDir, appServerDir);
		copyLafs(monitor, tmpWarDir, appServerDir);

		final File targetLibDir = copyStandardLibs(monitor, tmpWarDir, appServerDir);
		copyDrivers(monitor, appServerDir, targetLibDir);
		copyLibImages(tmpWarDir, appServerDir);
		moveSlf4j(tmpWarDir, targetLibDir);

		copyWebXml(tmpWarDir);
		addServoyProperties(tmpWarDir);
		copyActiveSolution(tmpWarDir, monitor);

		copyComponents(monitor, tmpWarDir, targetLibDir);

		zipDirectory(tmpWarDir, warFile, monitor);
		deleteDirectory(tmpWarDir);

		monitor.worked(2);
		monitor.done();
		return;
	}

	/**
	 * @param monitor
	 * @param tmpWarDir
	 * @param targetLibDir
	 * @throws ExportException
	 */
	private void copyComponents(IProgressMonitor monitor, File tmpWarDir, final File targetLibDir) throws ExportException
	{
		StringBuilder locations = new StringBuilder();
		try
		{
			locations.append(ComponentResourcesExporter.copyComponents(tmpWarDir));
		}
		catch (IOException e)
		{
			throw new ExportException("Could not copy default components", e);
		}
		locations.append(copyNGComponents(tmpWarDir, monitor));
		createComponentsPropertiesFile(tmpWarDir, locations.toString());

		try
		{
			ComponentResourcesExporter.copyLibs(targetLibDir);
		}
		catch (IOException e)
		{
			Debug.error(e);
			throw new ExportException("Could not copy default components", e);
		}
	}

	/**
	 * @param tmpWarDir 
	 * @param locations
	 * @throws ExportException 
	 */
	private void createComponentsPropertiesFile(File tmpWarDir, String locations) throws ExportException
	{
		Properties properties = new Properties();
		properties.put("locations", locations);
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(new File(tmpWarDir, "WEB-INF/components.properties"));
			properties.store(fos, "");
		}
		catch (Exception e)
		{
			throw new ExportException("Couldn't generate the components.properties file", e);
		}
		finally
		{
			if (fos != null) try
			{
				fos.close();
			}
			catch (IOException e)
			{
			}
		}
	}

	/**
	 * @param tmpWarDir
	 * @param monitor 
	 * @param locations 
	 * @throws ExportException 
	 */
	private String copyNGComponents(File tmpWarDir, IProgressMonitor monitor) throws ExportException
	{
		monitor.subTask("Copy NG components");
		StringBuilder locations = new StringBuilder();
		ServoyResourcesProject activeResourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
		try
		{
			activeResourcesProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
		catch (CoreException e1)
		{
			Debug.error(e1);
		}
		if (activeResourcesProject != null)
		{
			IFolder folder = activeResourcesProject.getProject().getFolder(COMPONENTS_DIR_NAME);
			if (folder.exists())
			{
				try
				{
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
							IFolder resourceFolder = (IFolder)resource;
							if ((resourceFolder).getFile("META-INF/MANIFEST.MF").exists())
							{
								locations.append(";/" + resourceFolder.getName().split("\\.")[0]);
								copyDir(new File(resource.getRawLocationURI()), tmpWarDir, true);
							}
						}
						else if (resource instanceof IFile)
						{
							locations.append(";/" + resource.getName().split("\\.")[0]);
							extractJar(new File(resource.getRawLocationURI()), tmpWarDir);
						}
					}
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
			}
		}
		monitor.worked(1);
		return locations.toString();
	}

	/**
	 * @param file
	 * @param tmpWarDir
	 * @throws ExportException 
	 */
	private void extractJar(File file, File tmpWarDir) throws ExportException
	{
		try
		{
			JarFile jarfile = new JarFile(file);
			Enumeration<JarEntry> enu = jarfile.entries();
			while (enu.hasMoreElements())
			{
				String destdir = tmpWarDir + "/" + file.getName().split("\\.")[0];
				JarEntry je = enu.nextElement();
				File fl = new File(destdir, je.getName());
				if (!fl.exists())
				{
					fl.getParentFile().mkdirs();
					fl = new File(destdir, je.getName());
				}
				if (je.isDirectory())
				{
					continue;
				}
				InputStream is = jarfile.getInputStream(je);
				FileOutputStream fo = new FileOutputStream(fl);
				while (is.available() > 0)
				{
					fo.write(is.read());
				}
				fo.close();
				is.close();
			}
		}
		catch (IOException e)
		{
			Debug.error(e);
			throw new ExportException(e.getMessage());
		}
	}

	/**
	 * @param monitor
	 * @return
	 * @throws ExportException
	 */
	private File createNewWarFile(IProgressMonitor monitor) throws ExportException
	{
		monitor.beginTask("Creating War File", 11);
		File warFile = new File(exportModel.getFileName());
		if (warFile.exists() && !warFile.delete())
		{
			throw new ExportException("Can't delete the existing war file " + exportModel.getFileName());
		}

		try
		{
			if (!warFile.createNewFile())
			{
				throw new ExportException("Can't create the file " + exportModel.getFileName());
			}
		}
		catch (IOException e)
		{
			throw new ExportException("Can't create the file " + exportModel.getFileName(), e);
		}

		monitor.worked(1);
		return warFile;
	}

	/**
	 * @param monitor
	 * @return
	 * @throws ExportException
	 */
	private File createTempDir(IProgressMonitor monitor) throws ExportException
	{
		File tmpDir = new File(System.getProperty("java.io.tmpdir"));

		File tmpWarDir = new File(tmpDir, "warexport/");
		if (tmpWarDir.exists())
		{
			if (!deleteDirectory(tmpWarDir))
			{
				throw new ExportException("Can't delete the temp dir  " + tmpWarDir);
			}
		}
		if (!tmpWarDir.mkdirs())
		{
			throw new ExportException("Can't create the temp dir  " + tmpWarDir);
		}

		monitor.worked(1);
		return tmpWarDir;
	}

	/**
	 * @param tmpWarDir
	 * @param monitor 
	 * @throws ExportException
	 */
	private void copyActiveSolution(File tmpWarDir, IProgressMonitor monitor) throws ExportException
	{
		if (exportModel.isExportActiveSolutionOnly())
		{
			try
			{
				FlattenedSolution solution = ServoyModelFinder.getServoyModel().getActiveProject().getFlattenedSolution();
				SolutionSerializer.writeRuntimeSolution(null, new File(tmpWarDir, "WEB-INF/solution.runtime"), solution.getSolution(),
					ApplicationServerRegistry.get().getDeveloperRepository(), solution.getModules());
			}
			catch (Exception ex)
			{
				throw new ExportException("Cannot write the active solution in war file", ex);
			}
		}
		monitor.worked(1);
	}

	/**
	 * @param tmpWarDir
	 * @throws ExportException
	 */
	private void addServoyProperties(File tmpWarDir) throws ExportException
	{
		if (exportModel.getServoyPropertiesFileName() == null)
		{
			generatePropertiesFile(tmpWarDir);
		}
		else
		{
			File sourceFile = new File(exportModel.getServoyPropertiesFileName());
			if (exportModel.allowOverwriteSocketFactoryProperties())
			{
				changeAndWritePropertiesFile(tmpWarDir, sourceFile);
			}
			else
			{
				copyPropertiesFileToWar(tmpWarDir, sourceFile);
			}
		}
	}

	/**
	 * @param tmpWarDir
	 * @throws ExportException
	 */
	private void copyWebXml(File tmpWarDir) throws ExportException
	{
		// copy war web.xml
		File webXMLFile = new File(tmpWarDir, "WEB-INF/web.xml");
		BufferedOutputStream bos = null;
		InputStream webXmlIS = null;
		try
		{
			webXmlIS = getClass().getResourceAsStream("resources/web.xml");
			bos = new BufferedOutputStream(new FileOutputStream(webXMLFile));

			byte[] buffer = new byte[8096];
			int read = webXmlIS.read(buffer);
			while (read != -1)
			{
				bos.write(buffer, 0, read);
				read = webXmlIS.read(buffer);
			}
		}
		catch (Exception e)
		{
			throw new ExportException("Can't create the web.xml file: " + webXMLFile, e);
		}
		finally
		{
			if (bos != null) try
			{
				bos.close();
			}
			catch (IOException e)
			{
			}
			if (webXmlIS != null) try
			{
				webXmlIS.close();
			}
			catch (IOException e)
			{
			}
		}
	}

	/**
	 * @param tmpWarDir
	 * @param targetLibDir
	 * @throws ExportException
	 */
	private void moveSlf4j(File tmpWarDir, final File targetLibDir) throws ExportException
	{
		// move the slf4j outside of the WEB-INF/lib to /lib/, its only used in the client
		File slf4j = new File(targetLibDir, "slf4j-jdk14.jar");
		copyFile(slf4j, new File(tmpWarDir, "lib/slf4j-jdk14.jar"));
		slf4j.delete();
	}

	/**
	 * @param tmpWarDir
	 * @param appServerDir
	 * @throws ExportException
	 */
	private void copyLibImages(File tmpWarDir, String appServerDir) throws ExportException
	{
		// copy lib/images dir 
		File libImagesDir = new File(appServerDir, "lib/images");
		File targetLibImagesDir = new File(tmpWarDir, "lib/images");
		targetLibImagesDir.mkdirs();
		copyDir(libImagesDir, targetLibImagesDir, false);
	}

	/**
	 * @param monitor
	 * @param appServerDir
	 * @param targetLibDir
	 * @throws ExportException
	 */
	private void copyDrivers(IProgressMonitor monitor, String appServerDir, final File targetLibDir) throws ExportException
	{
		monitor.subTask("Copy Drivers");

		List<String> drivers = exportModel.getDrivers();
		File srcDriverDir = new File(appServerDir, "drivers");
		for (String driverFileName : drivers)
		{
			copyFile(new File(srcDriverDir, driverFileName), new File(targetLibDir, driverFileName));
		}
		monitor.worked(1);
	}

	/**
	 * @param monitor
	 * @param tmpWarDir
	 * @param appServerDir
	 * @return
	 * @throws ExportException
	 */
	private File copyStandardLibs(IProgressMonitor monitor, File tmpWarDir, String appServerDir) throws ExportException
	{
		monitor.subTask("Copy all standard libraries");
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


		monitor.worked(1);
		return targetLibDir;
	}

	/**
	 * @param monitor
	 * @param tmpWarDir
	 * @param appServerDir
	 * @throws ExportException
	 */
	private void copyLafs(IProgressMonitor monitor, File tmpWarDir, String appServerDir) throws ExportException
	{
		Writer fw;
		monitor.subTask("Copy lafs");
		// copy the lafs
		File lafSourceDir = new File(appServerDir, "lafs");
		File lafTargetDir = new File(tmpWarDir, "lafs");
		lafTargetDir.mkdirs();
		ILAFManagerInternal lafManager = ApplicationServerRegistry.get().getLafManager();
		Map<String, List<ExtensionResource>> loadedLafDefs = lafManager.getLoadedLAFDefs();
		List<String> lafs = exportModel.getLafs();
		File lafProperties = new File(lafTargetDir, "lafs.properties");
		fw = null;
		try
		{
			fw = new FileWriter(lafProperties);
			Set<File> writtenFiles = new HashSet<File>();
			for (String lafName : lafs)
			{
				List<ExtensionResource> fileNames = JarManager.getExtensions(loadedLafDefs, lafName);
				if (fileNames != null)
				{
					for (ExtensionResource ext : fileNames)
					{
						File sourceFile = new File(lafSourceDir, ext.jarFileName);
						copyFile(sourceFile, new File(lafTargetDir, ext.jarFileName));
						writeFileEntry(fw, sourceFile, ext.jarFileName, writtenFiles);
					}
				}
			}
		}
		catch (IOException e2)
		{
			throw new ExportException("Error creating laf dir", e2);
		}
		finally
		{
			if (fw != null) try
			{
				fw.close();
				fw = null;
			}
			catch (IOException e)
			{
			}
		}


		monitor.worked(1);
	}

	/**
	 * @param monitor
	 * @param tmpWarDir
	 * @param appServerDir
	 * @param fw
	 * @throws ExportException
	 */
	private void copyPlugins(IProgressMonitor monitor, File tmpWarDir, String appServerDir) throws ExportException
	{
		monitor.subTask("Copy plugins");
		// copy the plugins
		File pluginsDir = new File(tmpWarDir, "plugins");
		pluginsDir.mkdirs();
		List<String> plugins = exportModel.getPlugins();
		File pluginProperties = new File(pluginsDir, "plugins.properties");
		Writer fw = null;
		try
		{
			fw = new FileWriter(pluginProperties);
			Set<File> writtenFiles = new HashSet<File>();
			for (String plugin : plugins)
			{
				String pluginName = "plugins/" + plugin;
				File pluginJarFile = new File(appServerDir, pluginName);

				writeFileEntry(fw, pluginJarFile, plugin, writtenFiles);

				copyFile(pluginJarFile, new File(tmpWarDir, pluginName));

				copyJnlp(tmpWarDir, appServerDir, pluginName + ".jnlp", fw, writtenFiles);

				if (pluginName.toLowerCase().endsWith(".jar") || pluginName.toLowerCase().endsWith(".zip"))
				{
					String pluginLibDir = pluginName.substring(0, pluginName.length() - 4);
					File pluginLibDirFile = new File(appServerDir, pluginLibDir);
					if (pluginLibDirFile.exists() && pluginLibDirFile.isDirectory())
					{
						Set<File> copiedFiles = copyDir(pluginLibDirFile, new File(tmpWarDir, pluginLibDir), false);
						for (File file : copiedFiles)
						{
							String fileName = file.getAbsolutePath().replace('\\', '/');
							int index = fileName.indexOf("plugins/");
							if (index != -1)
							{
								fileName = fileName.substring(index + "plugins/".length());
							}
							writeFileEntry(fw, file, fileName, writtenFiles);
						}
					}
				}

			}
		}
		catch (IOException e1)
		{
			throw new ExportException("Error creating plugins dir", e1);
		}
		finally
		{
			if (fw != null) try
			{
				fw.close();
				fw = null;
			}
			catch (IOException e)
			{
			}
		}

		monitor.worked(1);
	}

	/**
	 * @param monitor
	 * @param tmpWarDir
	 * @param appServerDir
	 * @return
	 * @throws ExportException
	 */
	private void copyBeans(IProgressMonitor monitor, File tmpWarDir, String appServerDir) throws ExportException
	{
		monitor.subTask("Copy beans");
		// copy the beans
		File beanSourceDir = new File(appServerDir, "beans");
		File beanTargetDir = new File(tmpWarDir, "beans");
		beanTargetDir.mkdirs();
		IBeanManagerInternal beanManager = ApplicationServerRegistry.get().getBeanManager();
		Map<String, List<ExtensionResource>> loadedBeanDefs = beanManager.getLoadedBeanDefs();
		List<String> beans = exportModel.getBeans();
		File beanProperties = new File(beanTargetDir, "beans.properties");
		Writer fw = null;
		try
		{
			fw = new FileWriter(beanProperties);
			Set<File> writtenFiles = new HashSet<File>();
			for (String beanName : beans)
			{
				List<ExtensionResource> fileNames = JarManager.getExtensions(loadedBeanDefs, beanName);
				if (fileNames != null)
				{
					for (ExtensionResource ext : fileNames)
					{
						File sourceFile = new File(beanSourceDir, ext.jarFileName);
						copyFile(sourceFile, new File(beanTargetDir, ext.jarFileName));
						writeFileEntry(fw, sourceFile, ext.jarFileName, writtenFiles);
					}
				}
			}
		}
		catch (IOException e2)
		{
			throw new ExportException("Error creating beans dir", e2);
		}
		finally
		{
			if (fw != null) try
			{
				fw.close();
				fw = null;
			}
			catch (IOException e)
			{
			}
		}

		monitor.worked(1);
	}

	/**
	 * @param monitor
	 * @param tmpWarDir
	 * @throws ExportException
	 */
	private void copyRootWebappFiles(IProgressMonitor monitor, File tmpWarDir, String appServerDir) throws ExportException
	{
		File webAppDir = new File(appServerDir, "server/webapps/ROOT");

		monitor.subTask("Copy root webapp files");
		// copy first the standard webapp dir of the app server
		copyDir(webAppDir, tmpWarDir, true);

		File defaultCss = new File(tmpWarDir, "/servoy-webclient/templates/default/servoy_web_client_default.css");
		if (!defaultCss.exists())
		{
			try
			{
				String styleCSS = TemplateGenerator.getStyleCSS("servoy_web_client_default.css");
				OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(defaultCss), "utf8");
				fw.write(styleCSS);
				fw.close();
			}
			catch (Exception e)
			{
				throw new ExportException("Error default servoy css file (servoy_web_client_default.css)", e);
			}
		}

		monitor.worked(1);
	}

	/**
	 * @param tmpWarDir
	 * @param sourceFile
	 * @throws ExportException
	 */
	private void changeAndWritePropertiesFile(File tmpWarDir, File sourceFile) throws ExportException
	{
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try
		{
			fis = new FileInputStream(sourceFile);
			Properties properties = new Properties();
			properties.load(fis);

			properties.setProperty("SocketFactory.rmiServerFactory", "com.servoy.j2db.server.rmi.tunnel.ServerTunnelRMISocketFactoryFactory");
			properties.setProperty("SocketFactory.tunnelConnectionMode", "http&socket");
			if (properties.containsKey("SocketFactory.useTwoWaySocket")) properties.remove("SocketFactory.useTwoWaySocket");

			fos = new FileOutputStream(new File(tmpWarDir, "WEB-INF/servoy.properties"));
			properties.store(fos, "");
		}
		catch (IOException e)
		{
			throw new ExportException("Failed to overwrite properties file", e);
		}
		finally
		{
			try
			{
				if (fis != null) fis.close();
				if (fos != null) fos.close();
			}
			catch (IOException e)
			{
				// ignore
			}
		}
	}

	/**
	 * @param tmpWarDir
	 * @param sourceFile
	 * @throws ExportException
	 */
	private void copyPropertiesFileToWar(File tmpWarDir, File sourceFile) throws ExportException
	{
		File destFile = new File(tmpWarDir, "WEB-INF/servoy.properties");
		try
		{
			if (destFile.createNewFile())
			{
				FileInputStream fis = null;
				FileOutputStream fos = null;
				FileChannel sourceChannel = null;
				FileChannel destinationChannel = null;


				try
				{
					fis = new FileInputStream(sourceFile);
					fos = new FileOutputStream(destFile);
					sourceChannel = fis.getChannel();
					destinationChannel = fos.getChannel();
					// Copy source file to destination file
					destinationChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
				}
				finally
				{
					if (sourceChannel != null) sourceChannel.close();
					if (destinationChannel != null) destinationChannel.close();
					if (fis != null) fis.close();
					if (fos != null) fos.close();
				}
			}
		}
		catch (IOException e)
		{
			throw new ExportException("Couldn't copy the servoy properties file", e);
		}
	}

	/**
	 * @param tmpWarDir
	 * @throws ExportException
	 */
	private void generatePropertiesFile(File tmpWarDir) throws ExportException
	{
		// create the (sorted) properties file.
		Properties properties = new SortedProperties();
		properties.setProperty("SocketFactory.rmiServerFactory", "com.servoy.j2db.server.rmi.tunnel.ServerTunnelRMISocketFactoryFactory");
		properties.setProperty("SocketFactory.tunnelConnectionMode", "http&socket");
		properties.setProperty("SocketFactory.compress", "true");
		properties.setProperty("java.rmi.server.hostname", "127.0.0.1");

		// TODO ask for a keystore?
		properties.setProperty("SocketFactory.useSSL", "true");
		properties.setProperty("SocketFactory.tunnelUseSSLForHttp", "false");
		//{ "SocketFactory.SSLKeystorePath", "", "The SSL keystore path on the server", "text" }, // 
		//{ "SocketFactory.SSLKeystorePassphrase", "", "The SSL passphrase to access the keystore", "password" }, //

		properties.setProperty("servoy.use.client.timezone", "true");

		// TODO ask for all kinds of other stuff like branding?
		properties.setProperty("servoy.server.start.rmi", Boolean.toString(exportModel.getStartRMI()));
		properties.setProperty("servoy.rmiStartPort", exportModel.getStartRMIPort());

		// store the servers
		SortedSet<String> selectedServerNames = exportModel.getSelectedServerNames();
		properties.setProperty("ServerManager.numberOfServers", Integer.toString(selectedServerNames.size()));
		int i = 0;
		for (String serverName : selectedServerNames)
		{
			ServerConfiguration sc = exportModel.getServerConfiguration(serverName);

			properties.put("server." + i + ".serverName", sc.getName()); //$NON-NLS-1$ //$NON-NLS-2$
			properties.put("server." + i + ".userName", sc.getUserName()); //$NON-NLS-1$ //$NON-NLS-2$
			properties.put("server." + i + ".password", sc.getPassword()); //$NON-NLS-1$ //$NON-NLS-2$
			properties.put("server." + i + ".URL", sc.getServerUrl()); //$NON-NLS-1$ //$NON-NLS-2$
//			Map<String, String> connectionProperties = sc.getConnectionProperties();
//			if (connectionProperties == null)
//			{
//				Settings.removePrefixedProperties(properties, "server." + i + ".property."); //$NON-NLS-1$ //$NON-NLS-2$
//			}
//			else
//			{
//				for (Entry<String, String> entry : connectionProperties.entrySet())
//				{
//					properties.put("server." + i + ".property." + entry.getKey(), entry.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
//				}
//			}
			properties.put("server." + i + ".driver", sc.getDriver()); //$NON-NLS-1$ //$NON-NLS-2$
			properties.put("server." + i + ".skipSysTables", "" + sc.isSkipSysTables()); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			String catalog = sc.getCatalog();
			if (catalog == null)
			{
				catalog = "<none>"; //$NON-NLS-1$
			}
			else if (catalog.trim().length() == 0)
			{
				catalog = "<empty>"; //$NON-NLS-1$
			}
			properties.put("server." + i + ".catalog", catalog); //$NON-NLS-1$ //$NON-NLS-2$
			String schema = sc.getSchema();
			if (schema == null)
			{
				schema = "<none>"; //$NON-NLS-1$
			}
			else if (schema.trim().length() == 0)
			{
				schema = "<empty>"; //$NON-NLS-1$
			}
			properties.put("server." + i + ".schema", schema); //$NON-NLS-1$ //$NON-NLS-2$
			properties.put("server." + i + ".maxConnectionsActive", String.valueOf(sc.getMaxActive())); //$NON-NLS-1$ //$NON-NLS-2$
			properties.put("server." + i + ".maxConnectionsIdle", String.valueOf(sc.getMaxIdle())); //$NON-NLS-1$ //$NON-NLS-2$
			properties.put("server." + i + ".maxPreparedStatementsIdle", String.valueOf(sc.getMaxPreparedStatementsIdle())); //$NON-NLS-1$ //$NON-NLS-2$
			properties.put("server." + i + ".connectionValidationType", String.valueOf(sc.getConnectionValidationType())); //$NON-NLS-1$ //$NON-NLS-2$
			if (sc.getValidationQuery() != null)
			{
				properties.put("server." + i + ".validationQuery", sc.getValidationQuery()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (sc.getDataModelCloneFrom() != null && !"".equals(sc.getDataModelCloneFrom())) //$NON-NLS-1$
			{
				properties.put("server." + i + ".dataModelCloneFrom", sc.getDataModelCloneFrom()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			properties.put("server." + i + ".enabled", Boolean.toString(true)); //$NON-NLS-1$ //$NON-NLS-2$ 
//			if (sc.getDialectClass() != null)
//			{
//				properties.put("server." + i + ".dialect", sc.getDialectClass()); //$NON-NLS-1$ //$NON-NLS-2$
//			}
//			else
//			{
//				properties.remove("server." + i + ".dialect"); //$NON-NLS-1$//$NON-NLS-2$
//			}
			i++;
		}

		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(new File(tmpWarDir, "WEB-INF/servoy.properties"));
			properties.store(fos, "");
		}
		catch (Exception e)
		{
			throw new ExportException("Couldn't generate the properties file", e);
		}
		finally
		{
			if (fos != null) try
			{
				fos.close();
			}
			catch (IOException e)
			{
			}
		}
	}

	/**
	 * @param fw
	 * @param filename
	 * @param pluginJarFile
	 * @throws IOException
	 */
	private void writeFileEntry(Writer fw, File file, String name, Set<File> writtenFiles) throws IOException
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

	private void zipDirectory(File directory, File zip, IProgressMonitor monitor) throws ExportException
	{
		monitor.subTask("Zipping the war file");
		ZipOutputStream zos = null;
		try
		{
			zos = new ZipOutputStream(new FileOutputStream(zip));
			zip(directory, directory, zos);
		}
		catch (Exception e)
		{
			throw new ExportException("Can't create the war file " + zip, e);
		}
		finally
		{
			if (zos != null) try
			{
				zos.close();
			}
			catch (IOException e)
			{
			}
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

	/**
	 * @param tmpWarDir
	 * @param appServerDir
	 * @param pluginJnlpName
	 * @param fw 
	 * @throws ExportException
	 */
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

				for (String jnlpName : jnlpNames)
				{
					copyJnlp(tmpWarDir, appServerDir, jnlpName, fw, writtenFiles);
				}
			}
			else
			{
				Debug.error("jnlp file " + pluginJarJnlpFile + " couldn't be parsed, nothing copied");
			}
		}
	}

	/**
	 * @param childNodes
	 * @param jarNames
	 */
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
			Debug.error("Error creating parsing the jnlp file: " + jnlpFile, e);
		}
		return null;
	}

	private static boolean deleteDirectory(File path)
	{
		if (path.exists())
		{
			File[] files = path.listFiles();
			for (File file : files)
			{
				if (file.isDirectory())
				{
					deleteDirectory(file);
				}
				else
				{
					file.delete();
				}
			}
		}
		return (path.delete());
	}

	private static Set<File> copyDir(File sourceDir, File destDir, boolean recusive) throws ExportException
	{
		Set<File> writtenFiles = new HashSet<File>();
		copyDir(sourceDir, destDir, recusive, writtenFiles);
		return writtenFiles;
	}

	private static void copyDir(File sourceDir, File destDir, boolean recusive, Set<File> writtenFiles) throws ExportException
	{
		if (!destDir.exists() && !destDir.mkdirs()) throw new ExportException("Can't create destination dir: " + destDir); //$NON-NLS-1$
		File[] listFiles = sourceDir.listFiles();
		for (File file : listFiles)
		{
			if (file.isDirectory())
			{
				if (recusive) copyDir(file, new File(destDir, file.getName()), recusive, writtenFiles);
			}
			else
			{
				copyFile(file, new File(destDir, file.getName()));
				writtenFiles.add(file);
			}
		}
	}

	private static void copyFile(File sourceFile, File destFile) throws ExportException
	{
		if (!sourceFile.exists())
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
			FileChannel source = null;
			FileChannel destination = null;
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			if (destination != null && source != null)
			{
				destination.transferFrom(source, 0, source.size());
			}
			if (source != null)
			{
				source.close();
			}
			if (destination != null)
			{
				destination.close();
			}
		}
		catch (Exception e)
		{
			throw new ExportException("Cant'copy file from " + sourceFile + " to " + destFile, e);
		}

	}

}
