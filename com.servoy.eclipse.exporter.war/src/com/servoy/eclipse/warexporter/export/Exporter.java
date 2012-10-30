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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.servoy.eclipse.warexporter.ui.wizard.ServerConfiguration;
import com.servoy.j2db.IBeanManagerInternal;
import com.servoy.j2db.ILAFManagerInternal;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;


/**
 * Class that creates the WAR file.
 * 
 * @author jcompagner
 * @since 6.1
 */
public class Exporter
{

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
	@SuppressWarnings("nls")
	public void doExport(IProgressMonitor monitor) throws ExportException
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
		String appServerDir = ApplicationServerSingleton.get().getServoyApplicationServerDirectory();

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
		monitor.subTask("Copy beans");
		// copy the beans
		File beanSourceDir = new File(appServerDir, "beans");
		File beanTargetDir = new File(tmpWarDir, "beans");
		beanTargetDir.mkdirs();
		IBeanManagerInternal beanManager = ApplicationServerSingleton.get().getBeanManager();
		Map<String, Object> loadedBeanDefs = beanManager.getLoadedBeanDefs();
		List<String> beans = exportModel.getBeans();
		File beanProperties = new File(beanTargetDir, "beans.properties");
		Writer fw = null;
		try
		{
			fw = new FileWriter(beanProperties);
			Set<File> writtenFiles = new HashSet<File>();
			for (String beanName : beans)
			{
				String[] fileNames = getExtensions(loadedBeanDefs, beanName);
				if (fileNames != null)
				{
					for (String filename : fileNames)
					{
						File sourceFile = new File(beanSourceDir, filename);
						copyFile(sourceFile, new File(beanTargetDir, filename));
						writeFileEntry(fw, sourceFile, filename, writtenFiles);
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
		monitor.subTask("Copy plugins");
		// copy the plugins
		File pluginsDir = new File(tmpWarDir, "plugins");
		pluginsDir.mkdirs();
		List<String> plugins = exportModel.getPlugins();
		File pluginProperties = new File(pluginsDir, "plugins.properties");
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
		monitor.subTask("Copy lafs");
		// copy the lafs
		File lafSourceDir = new File(appServerDir, "lafs");
		File lafTargetDir = new File(tmpWarDir, "lafs");
		lafTargetDir.mkdirs();
		ILAFManagerInternal lafManager = ApplicationServerSingleton.get().getLafManager();
		Map<String, Object> loadedLafDefs = lafManager.getLoadedLAFDefs();
		List<String> lafs = exportModel.getLafs();
		File lafProperties = new File(lafTargetDir, "lafs.properties");
		fw = null;
		try
		{
			fw = new FileWriter(lafProperties);
			Set<File> writtenFiles = new HashSet<File>();
			for (String lafName : lafs)
			{
				String[] fileNames = getExtensions(loadedLafDefs, lafName);
				if (fileNames != null)
				{
					for (String filename : fileNames)
					{
						File sourceFile = new File(lafSourceDir, filename);
						copyFile(sourceFile, new File(lafTargetDir, filename));
						writeFileEntry(fw, sourceFile, filename, writtenFiles);
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
		// delete the tomcat boostrapper, also not needed in a war file
		new File(targetLibDir, "server-bootstrap.jar").delete();


		monitor.worked(1);
		monitor.subTask("Copy Drivers");

		List<String> drivers = exportModel.getDrivers();
		File srcDriverDir = new File(appServerDir, "drivers");
		for (String driverFileName : drivers)
		{
			copyFile(new File(srcDriverDir, driverFileName), new File(targetLibDir, driverFileName));
		}
		monitor.worked(1);


		// copy lib/images dir 
		File libImagesDir = new File(appServerDir, "lib/images");
		File targetLibImagesDir = new File(tmpWarDir, "lib/images");
		targetLibImagesDir.mkdirs();
		copyDir(libImagesDir, targetLibImagesDir, false);

		// move the slf4j outside of the WEB-INF/lib to /lib/, its only used in the client
		File slf4j = new File(targetLibDir, "slf4j-jdk14.jar");
		copyFile(slf4j, new File(tmpWarDir, "lib/slf4j-jdk14.jar"));
		slf4j.delete();

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

		if (exportModel.getServoyPropertiesFileName() == null)
		{
			// create the (sorted) properties file.
			Properties properties = new Properties()
			{
				@Override
				public synchronized Enumeration<Object> keys()
				{
					return Collections.enumeration(new TreeSet<Object>(super.keySet()));
				}
			};
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
				properties.put("server." + i + ".enabled", Boolean.toString(true)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		else
		{
			File sourceFile = new File(exportModel.getServoyPropertiesFileName());
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
		monitor.worked(1);
		monitor.subTask("Zipping the war file");
		// zip it
		zipDirectory(tmpWarDir, warFile);

		deleteDirectory(tmpWarDir);

		monitor.worked(2);
		monitor.done();
		return;
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

	private void zipDirectory(File directory, File zip) throws ExportException
	{
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

	@SuppressWarnings("unchecked")
	private static String[] getExtensions(Map<String, Object> definitions, String filename)
	{
		String jarFileName = filename;
		int index = jarFileName.lastIndexOf('/');
		if (index != -1)
		{
			jarFileName = jarFileName.substring(index + 1);
		}
		Iterator<Object> it = definitions.values().iterator();
		while (it.hasNext())
		{
			Object value = it.next();
			String[] names = null;
			if (value instanceof String)
			{
				names = new String[] { (String)value };
			}
			else if (value instanceof List)
			{
				List<String> lst = (List<String>)value;
				names = lst.toArray(new String[lst.size()]);
			}
			if (names != null)
			{
				for (String name : names)
				{
					index = name.lastIndexOf('/');
					if (index == -1) index = 0;
					else index++;
					name = name.substring(index);
					if (jarFileName.equals(name))
					{
						return names;
					}
				}
			}
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
