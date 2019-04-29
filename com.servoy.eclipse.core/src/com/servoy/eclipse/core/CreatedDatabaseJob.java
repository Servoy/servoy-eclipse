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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.serverconfigtemplates.PostgresTemplate;
import com.servoy.j2db.util.Settings;

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
		this.batchFile = batchFile;
		this.serverNames = serverNames;
		this.appServer = appServer;
	}


	@Override
	public void run(IProgressMonitor monitor)
	{
		monitor.beginTask("Creating postgresql database", 3);
		try
		{
			ProcessBuilder pb = new ProcessBuilder(batchFile.getCanonicalPath());
			pb.directory(batchFile.getParentFile().getCanonicalFile());
			Process process = pb.start();
			process.waitFor();
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		monitor.worked(1);
		monitor.subTask("Starting postgresql");

		try
		{
			File startPostgres = new File(batchFile.getParentFile().getParentFile().getCanonicalFile(), "startpostgres.bat");
			ProcessBuilder pb = new ProcessBuilder(startPostgres.getCanonicalPath()); // TODO should be changed for OSX
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
		settings.setProperty("nativeStartupLauncher", "%%user.dir%%/startpostgres.bat"); // should be sh for none windows
		settings.setProperty("waitForNativeStartup", "false");
		settings.setProperty("nativeShutdownLauncher", "%%user.dir%%/postgres_db/bin/pg_ctl|stop|-D|database|-l|postgres_db/postgres_log.txt");
		IServerManagerInternal serverManager = appServer.getServerManager();

		PostgresTemplate pt = new PostgresTemplate();
		for (String serverName : serverNames)
		{
			ServerConfig sc = pt.getTemplate();
			String url = pt.getUrlForValues(new String[] { "localhost", checkReplacement(serverName) }, null);
			ServerConfig newServerConfig = new ServerConfig(serverName, sc.getUserName(), sc.getPassword(), url, sc.getConnectionProperties(), sc.getDriver(),
				sc.getCatalog(), sc.getSchema(), sc.getMaxActive(), sc.getMaxIdle(), sc.getMaxPreparedStatementsIdle(), sc.getConnectionValidationType(),
				sc.getValidationQuery(), sc.getDataModelCloneFrom(), true, sc.getSkipSysTables(), sc.getPrefixTables(), false, sc.getIdleTimeout(),
				sc.getSelectINValueCountLimit(), sc.getDialectClass());
			serverManager.createServer(newServerConfig);
			try
			{
				serverManager.saveServerConfig(null, newServerConfig);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
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
		}
		return serverName;
	}

}
