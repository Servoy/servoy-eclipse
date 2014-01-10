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

import java.sql.PreparedStatement;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.util.ITransactionConnection;
import com.servoy.j2db.util.Utils;

public class NewPostgresDbAction extends AbstractNewDbAction
{
	/**
	 * Creates a new action for the given solution view.
	 * @param sev the solution view to use.
	 */
	public NewPostgresDbAction(SolutionExplorerView sev)
	{
		super("PostgreSQL", sev);
	}

	/**
	 * @param serverPrototype
	 * @param name
	 * @param monitor
	 */
	@Override
	protected boolean createDatabase(final IServerInternal serverPrototype, final String name, IProgressMonitor monitor)
	{
		ITransactionConnection connection = null;
		PreparedStatement ps = null;
		monitor.beginTask("Creating PostgreSQL database...", 1);
		try
		{
			connection = serverPrototype.getUnmanagedConnection();
			ps = connection.prepareStatement("CREATE DATABASE \"" + name + "\" WITH ENCODING 'UNICODE';");
			ps.execute();
			ps.close();
			ps = null;
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					MessageDialog.openInformation(viewer.getSite().getShell(), "Info", "PostgreSQL database created.");
				}
			});
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
			displayError("Could not create database: " + e.getMessage());
			return false;
		}
		finally
		{
			Utils.closeConnection(connection);
			Utils.closeStatement(ps);
		}
		return true;
	}

	/**
	 * @param serverPrototype
	 * @param name
	 */
	@Override
	protected void setDefaultConfig(final IServerInternal serverPrototype, final String name)
	{
		try
		{
			ServerConfig origConfig = serverPrototype.getConfig();
			String dbname = null;
			String serverUrl = origConfig.getServerUrl();
			int startIndex = serverUrl.lastIndexOf("/");
			int endIndex = serverUrl.indexOf("?", startIndex);
			if (endIndex == -1) endIndex = serverUrl.length();
			dbname = serverUrl.substring(startIndex + 1, endIndex);
			if (dbname != null) serverUrl = serverUrl.replaceFirst("/" + dbname, "/" + name); //$NON-NLS-1$ //$NON-NLS-2$
			if (serverUrl.equals(origConfig.getServerUrl()))
			{
				// hmm, no replace, fall back to default
				serverUrl = "jdbc:postgresql://localhost/" + name;
			}
			final IServerManagerInternal serverManager = ServoyModel.getServerManager();
			String configName = name;
			for (int i = 1; serverManager.getServerConfig(configName) != null && i < 100; i++)
			{
				configName = name + i;
			}
			serverManager.getServerConfig(name);

			saveAndOpenDefaultConfig(origConfig, serverUrl, serverManager, configName);

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
		if (serverURL.contains("postgresql") && sc.isEnabled())
		{
			serverURL = serverURL.replaceFirst(".*//", "");
			serverURL = serverURL.replaceFirst("/.*", "");
			return serverURL;
		}
		return null;
	}

	@Override
	protected boolean isDbTypeDriver(ServerConfig sc)
	{
		return sc.isPostgresDriver();
	}
}
