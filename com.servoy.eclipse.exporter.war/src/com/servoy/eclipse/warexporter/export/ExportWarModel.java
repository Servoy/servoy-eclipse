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
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.jface.dialogs.IDialogSettings;

import com.servoy.eclipse.warexporter.ui.wizard.ServerConfiguration;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Utils;

/**
 * Model holding all the data for the export.
 * 
 * @author jcompagner
 * @since 6.1
 */
public class ExportWarModel
{

	private String fileName;
	private final List<String> plugins = new ArrayList<String>();
	private final List<String> beans = new ArrayList<String>();
	private final List<String> lafs = new ArrayList<String>();
	private final List<String> drivers = new ArrayList<String>();
	private final TreeMap<String, ServerConfiguration> servers = new TreeMap<String, ServerConfiguration>();
	private final SortedSet<String> selectedServerNames = new TreeSet<String>();
	private String servoyPropertiesFileName;
	private String startRMIPort;
	private boolean startRMI;
	private boolean exportActiveSolutionOnly;

	/**
	 * @param dialogSettings
	 */
	public ExportWarModel(IDialogSettings settings)
	{
		fileName = settings.get("export.filename");
		servoyPropertiesFileName = settings.get("export.servoyPropertiesFileName");
		exportActiveSolutionOnly = Utils.getAsBoolean(settings.get("export.exportActiveSolutionOnly"));

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
	}

	public void saveSettings(IDialogSettings settings)
	{
		if (fileName != null) settings.put("export.filename", fileName);
		if (exportActiveSolutionOnly) settings.put("export.exportActiveSolutionOnly", Boolean.TRUE.toString());
		if (servoyPropertiesFileName != null) settings.put("export.servoyPropertiesFileName", servoyPropertiesFileName);
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

		if (servers.size() > 0)
		{
			StringBuilder sb = new StringBuilder(128);
			for (String name : selectedServerNames)
			{
				sb.append(name);
				sb.append(';');
				ServerConfiguration sc = getServerConfiguration(name);
				settings.put("export.servers." + name + ".catalog", sc.getCatalog());
				settings.put("export.servers." + name + ".clone", sc.getDataModelCloneFrom());
				settings.put("export.servers." + name + ".driver", sc.getDriver());
				settings.put("export.servers." + name + ".password", sc.getPassword());
				settings.put("export.servers." + name + ".schema", sc.getSchema());
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
	 * @return the exportActiveSolutionOnly
	 */
	public boolean isExportActiveSolutionOnly()
	{
		return exportActiveSolutionOnly;
	}

	/**
	 * @param exportActiveSolutionOnly the exportActiveSolutionOnly to set
	 */
	public void setExportActiveSolutionOnly(boolean exportActiveSolutionOnly)
	{
		this.exportActiveSolutionOnly = exportActiveSolutionOnly;
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
}
