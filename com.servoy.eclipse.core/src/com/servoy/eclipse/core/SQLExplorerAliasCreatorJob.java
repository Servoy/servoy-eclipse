package com.servoy.eclipse.core;

import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.WorkbenchJob;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

import net.sourceforge.sqlexplorer.ExplorerException;
import net.sourceforge.sqlexplorer.dbproduct.Alias;
import net.sourceforge.sqlexplorer.dbproduct.AliasManager;
import net.sourceforge.sqlexplorer.dbproduct.ManagedDriver;
import net.sourceforge.sqlexplorer.dbproduct.User;
import net.sourceforge.sqlexplorer.plugin.SQLExplorerPlugin;


/**
 * @author jcompagner
 */
public final class SQLExplorerAliasCreatorJob extends WorkbenchJob
{
	SQLExplorerAliasCreatorJob(String name)
	{
		super(name);
	}

	@Override
	public IStatus runInUIThread(IProgressMonitor monitor)
	{
		IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();

		String[] serverNames = serverManager.getServerNames(true, true, false, false);
		AliasManager aliasManager = SQLExplorerPlugin.getDefault().getAliasManager();

		try
		{
			aliasManager.loadAliases();
		}
		catch (ExplorerException e1)
		{
			ServoyLog.logError(e1);
		}

		for (String serverName : serverNames)
		{
			generateSQLExplorerAlias(serverName);
		}
		try
		{
			aliasManager.saveAliases();
		}
		catch (ExplorerException e)
		{
			ServoyLog.logError(e);
		}
		return Status.OK_STATUS;
	}

	public static Alias generateSQLExplorerAlias(String serverName)
	{
		AliasManager aliasManager = SQLExplorerPlugin.getDefault().getAliasManager();
		Alias alias = new Alias(serverName)
		{
			ManagedDriver driver = new ManagedDriver(getName())
			{
				@Override
				public net.sourceforge.sqlexplorer.dbproduct.SQLConnection getConnection(User user) throws java.sql.SQLException
				{
					IServerInternal server = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(getId());
					try
					{
						return new net.sourceforge.sqlexplorer.dbproduct.SQLConnection(user, server.getRawConnection(), this, "Servoy server: " + getId());
					}
					catch (RepositoryException e)
					{
						throw new SQLException(e.getMessage());
					}
				}
			};

			/**
			 * @see net.sourceforge.sqlexplorer.dbproduct.Alias#getDriver()
			 */
			@Override
			public ManagedDriver getDriver()
			{
				return driver;
			}
		};
		alias.setAutoLogon(true);
		alias.setConnectAtStartup(false);
		alias.setHasNoUserName(true);
		try
		{
			aliasManager.addAlias(alias);
		}
		catch (ExplorerException e)
		{
			ServoyLog.logError(e);
		}
		return alias;
	}

}