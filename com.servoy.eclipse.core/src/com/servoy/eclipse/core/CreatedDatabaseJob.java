/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.eclipse.core;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.serverconfigtemplates.PostgresTemplate;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * @since 2019.06
 */
public class CreatedDatabaseJob implements IRunnableWithProgress
{

	private final File batchFile;
	private final String[] serverNames;
	private final IApplicationServerSingleton appServer;

	/**
	 * @param appServer
	 * @param name
	 */
	public CreatedDatabaseJob(File batchFile, String[] serverNames, IApplicationServerSingleton appServer)
	{
		super();
		File canonical = batchFile;
		try
		{
			canonical = batchFile.getCanonicalFile();
		}
		catch (IOException e1)
		{
		}
		this.batchFile = canonical;
		this.serverNames = serverNames;
		this.appServer = appServer;
		try
		{
			this.batchFile.setExecutable(true);
		}
		catch (Exception e)
		{
			ServoyLog.logError("Error setting executable bits on " + this.batchFile.getAbsolutePath(), e);
		}

		if (Utils.getPlatform() != Utils.PLATFORM_WINDOWS)
		{
			try
			{
				File postgresBin = new File(this.batchFile.getParentFile(), "bin");
				new File(postgresBin, "initdb").setExecutable(true);
				new File(postgresBin, "pg_ctl").setExecutable(true);
				new File(postgresBin, "postgres").setExecutable(true);
				new File(postgresBin, "createdb").setExecutable(true);
				new File(postgresBin, "pg_restore").setExecutable(true);
				new File(postgresBin, "pg_dump").setExecutable(true);
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error setting executable bits on postgresql", e);
			}
		}
	}


	@Override
	public void run(IProgressMonitor monitor)
	{
		monitor.beginTask("Creating PostgreSQL database", 3);
		String quote = "";
		if (Utils.getPlatform() == Utils.PLATFORM_WINDOWS)
		{
			quote = "\"";
		}
		try
		{
			ProcessBuilder pb = new ProcessBuilder(quote + batchFile.getAbsolutePath() + quote);
			pb.directory(batchFile.getParentFile());
			Process process = pb.start();
			process.waitFor();
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		monitor.worked(1);
		monitor.subTask("Starting PostgreSQL");

		try
		{
			ProcessBuilder pb = null;
			File startPostgres = new File(batchFile.getParentFile().getParentFile(), "startpostgres.bat");
			if (Utils.getPlatform() != Utils.PLATFORM_WINDOWS)
			{
				startPostgres = new File(batchFile.getParentFile().getParentFile(), "startpostgres.sh");
			}
			pb = new ProcessBuilder(quote + startPostgres.getAbsolutePath() + quote);
			pb.directory(startPostgres.getParentFile());
			Process process = pb.start();
			process.waitFor();
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}

		monitor.worked(1);
		monitor.subTask("Creating server configurations");

		// add start postgres to the servoy properties file.
		Settings settings = appServer.getService(Settings.class);
		if (Utils.getPlatform() == Utils.PLATFORM_WINDOWS)
		{
			settings.setProperty("nativeStartupLauncher", "%%user.dir%%/startpostgres.bat");
		}
		else
		{
			settings.setProperty("nativeStartupLauncher", "%%user.dir%%/postgres_db/bin/pg_ctl|start|-D|database|-l|postgres_db/postgres_log.txt");
		}
		settings.setProperty("waitForNativeStartup", "false");
		settings.setProperty("nativeShutdownLauncher", "%%user.dir%%/postgres_db/bin/pg_ctl|stop|-D|database|-l|postgres_db/postgres_log.txt");
		IServerManagerInternal serverManager = appServer.getServerManager();

		PostgresTemplate pt = new PostgresTemplate();
		for (String serverName : serverNames)
		{
			ServerConfig sc = pt.getTemplate();
			String url = pt.getUrlForValues(new String[] { "localhost", checkReplacement(serverName) }, null);

			ServerConfig newServerConfig = sc.newBuilder()
				.setServerName(serverName)
				.setServerUrl(url)
				.setEnabled(true)
				.setQueryProcedures(false)
				.build();

			try
			{
				serverManager.saveServerConfig(null, newServerConfig);
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
		try
		{
			settings.save();
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		Activator.getDefault().setPostgresChecked();
		monitor.worked(1);
	}

	/**
	 * @param serverName
	 * @return
	 */
	private String checkReplacement(String serverName)
	{
		switch (serverName)
		{
			case "repository_server" :
				return "servoy_repository";
			case "example_data" :
				return "example";
		}
		return serverName;
	}

}
