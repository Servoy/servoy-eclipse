/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.ITransactionConnection;
import com.servoy.j2db.util.Utils;

public class NewSybaseDbAction extends AbstractNewDbAction
{

	/**
	 * Creates a new action for the given solution view.
	 *
	 * @param sev the solution view to use.
	 */
	public NewSybaseDbAction(SolutionExplorerView sev)
	{
		super("Sybase", sev);
	}


	@Override
	protected boolean isDbTypeDriver(ServerConfig sc)
	{
		return sc.isSybaseDriver();
	}

	/**
	 * @param serverPrototype
	 * @param name
	 */
	@Override
	protected void setDefaultConfig(IServerInternal serverPrototype, String name)
	{
		try
		{
			ServerConfig origConfig = serverPrototype.getConfig();
			String serverUrl = origConfig.getServerUrl().replaceFirst("ServiceName=[a-zA-Z_]*", "ServiceName=" + name);
			if (serverUrl.equals(origConfig.getServerUrl()))
			{
				// hmm, no replace, fall back to default
				serverUrl = "jdbc:sybase:Tds:localhost:2638?ServiceName=" + name + "&CHARSET=utf8";
			}

			IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
			String configName = name;
			for (int i = 1; serverManager.getServerConfig(configName) != null && i < 100; i++)
			{
				configName = name + i;
			}
			serverManager.getServerConfig(name);
			saveAndOpenDefaultConfig(origConfig, serverPrototype.getSettings().withDefaults(origConfig), serverUrl, serverManager, configName);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	@Override
	protected String getServerUrl(ServerConfig sc)
	{
		String serverURL = sc.getServerUrl();
		if (serverURL.contains("sybase") && sc.isEnabled())
		{
			serverURL = serverURL.replaceFirst("jdbc:sybase:Tds:", "");
			serverURL = serverURL.replaceFirst("\\" + "?ServiceName=.*", "");
			return serverURL;
		}
		return null;
	}

	/**
	 * @param serverPrototype
	 * @param name
	 * @param monitor
	 */
	@Override
	protected boolean createDatabase(final IServerInternal serverPrototype, final String name, IProgressMonitor monitor)
	{
		String path = null;
		ITransactionConnection connection = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			monitor.beginTask("Find out where " + dbType + " is running", 4);
			//1. find out where ASA is running
			try
			{
				connection = serverPrototype.getUnmanagedConnection();
				ps = connection.prepareStatement("SELECT DB_PROPERTY ( 'File' );");
				rs = ps.executeQuery();
				rs.next();
				path = rs.getString(1);
				int index = path.lastIndexOf("/");
				if (index < 0)
				{
					index = path.lastIndexOf("\\");
				}
				path = path.substring(0, index + 1);
				path += name + ".db";
				rs.close();
				rs = null;
				ps.close();
				ps = null;
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
				displayError("Could not get the path of the server database file: " + e.getMessage());
				return false;
			}
			monitor.worked(1);

			monitor.setTaskName("Attempting to create database...");
			//2. create the database
			File databaseFile = Paths.get(path).normalize().toFile();
			if (!databaseFile.exists())
			{
				try
				{
					ps = connection.prepareStatement("create database ? collation 'UTF8'");
					ps.setString(1, path);
					ps.execute();
					ps.close();
					ps = null;
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
					displayError("Could not create database file: " + e.getMessage());
					return false;
				}
			}
			else
			{
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						MessageDialog.openWarning(viewer.getSite().getShell(), "Warning",
							"A database with that name already exists, will not create a new one.");
					}
				});
				return false;
			}
			monitor.worked(1);

			monitor.setTaskName("Attempting to start database...");
			//3. attempt to start the database
			try
			{
				ps = connection.prepareStatement("start database ?");
				ps.setString(1, path);
				ps.execute();
				ps.close();
				ps = null;
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
				displayError("Could not start database: " + e.getMessage());
				return false;
			}
			monitor.worked(1);

			monitor.setTaskName("Trying to modify 'sybase.config'");
			//4. Edit the sybase config file
			BufferedWriter writer = null;
			try
			{
				// path is string of db file, assume config file is in sibling directory sybase_db
				File dbFile = Paths.get(path).normalize().toFile();
				File databaseDirectory = dbFile.getParentFile();
				File sybaseDirectory = databaseDirectory.getParentFile();
				File sybaseConfig = new File(sybaseDirectory, "sybase_db/sybase.config");
				if (sybaseConfig.exists() && sybaseConfig.canWrite())
				{
					writer = new BufferedWriter(new FileWriter(sybaseConfig, true));
					writer.newLine();
					writer.write(databaseDirectory.getName() + '/' + dbFile.getName());
					writer.close();
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
				displayError("Could not edit sybase.config file: " + e.getMessage());
			}
			finally
			{
				if (writer != null) writer.close();
			}
			monitor.worked(1);
			monitor.done();
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					MessageDialog.openInformation(viewer.getSite().getShell(), "Info",
						"Database successfully created. Sybase and developer restart is required.");
				}
			});

		}
		catch (Exception ex)
		{
			displayError(ex.getMessage());
		}
		finally
		{
			Utils.closeConnection(connection);
			Utils.closeStatement(ps);
			Utils.closeResultSet(rs);
		}
		return true;
	}

	@Override
	public boolean isVisible()
	{
		return !getServerMap().isEmpty();
	}
}
