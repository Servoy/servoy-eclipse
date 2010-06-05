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
package com.servoy.eclipse.core.repository;

import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.ITeamRepository;
import com.servoy.j2db.server.IApplicationServer;
import com.servoy.j2db.server.IApplicationServerAccess;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.LocalhostRMIRegistry;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.rmi.IRMIClientSocketFactoryFactory;
import com.servoy.j2db.util.rmi.IReconnectListener;

public class RepositoryAccessPoint
{
	private static final AtomicReference<RepositoryAccessPoint> instanceRef = new AtomicReference<RepositoryAccessPoint>();

	private final String serverAddress;
	private final String user;
	private final String passwordHash;

	private int usedRMIPort;

	private String clientID;
	private IApplicationServerAccess applicationServerAccess;
	private ITeamRepository repository;
	private boolean isInprocessApplicationServer;

	private IRMIClientSocketFactoryFactory rmiFactoryFactory;

	private RepositoryAccessPoint(String serverAddress, String user, String passwordHash)
	{
		if (serverAddress == null) serverAddress = "localhost";
		int dblDotIdx = serverAddress.indexOf(":");
		if (dblDotIdx != -1)
		{
			this.serverAddress = serverAddress.substring(0, dblDotIdx);
			if (dblDotIdx < serverAddress.length() - 1)
			{
				String port = serverAddress.substring(dblDotIdx + 1);
				try
				{
					this.usedRMIPort = Integer.parseInt(port);
				}
				catch (NumberFormatException ex)
				{
					// ignore;
				}
			}
		}
		else this.serverAddress = serverAddress;
		this.user = user;
		this.passwordHash = passwordHash;


		if (usedRMIPort == 0)
		{
			Properties settings = getServoySettings();
			usedRMIPort = Utils.getAsInteger(settings.getProperty("usedRMIRegistryPort", "1099")); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (serverAddress.equals("localhost"))
		{
			isInprocessApplicationServer = true;
		}
	}

	public static RepositoryAccessPoint getInstance(String serverAddress, String user, String passwordHash)
	{
		synchronized (instanceRef)
		{
			RepositoryAccessPoint rap = new RepositoryAccessPoint(serverAddress, user, passwordHash);
			RepositoryAccessPoint oldRAp = instanceRef.get();
			if (rap.equals(oldRAp))
			{
				return oldRAp;
			}
			if (oldRAp != null)
			{
				oldRAp.close();
			}
			instanceRef.set(rap);
			return rap;
		}
	}

	public static synchronized void clear()
	{
		synchronized (instanceRef)
		{
			RepositoryAccessPoint rap = instanceRef.getAndSet(null);
			if (rap != null)
			{
				rap.close();
			}
		}
	}


	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((passwordHash == null) ? 0 : passwordHash.hashCode());
		result = prime * result + ((serverAddress == null) ? 0 : serverAddress.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		RepositoryAccessPoint other = (RepositoryAccessPoint)obj;
		if (passwordHash == null)
		{
			if (other.passwordHash != null) return false;
		}
		else if (!passwordHash.equals(other.passwordHash)) return false;
		if (serverAddress == null)
		{
			if (other.serverAddress != null) return false;
		}
		else if (!serverAddress.equals(other.serverAddress)) return false;
		if (user == null)
		{
			if (other.user != null) return false;
		}
		else if (!user.equals(other.user)) return false;
		return true;
	}

	protected void close()
	{
		if (rmiFactoryFactory != null)
		{
			rmiFactoryFactory.close();
			rmiFactoryFactory = null;
		}

	}


	private boolean checkParameters(String srv, String usr, String pwdHash)
	{
		return Utils.stringSafeEquals(serverAddress, srv) && Utils.stringSafeEquals(user, usr) && Utils.stringSafeEquals(passwordHash, pwdHash);
	}

	public ITeamRepository getRepository()
	{
		if (repository == null)
		{
			try
			{
				repository = createRepository();
			}
			catch (Exception e)
			{
				Debug.error("Could not create repository", e);
			}
		}
		return repository;
	}

	protected ITeamRepository createRepository() throws ApplicationServerAccessException, RepositoryAccessException, RemoteException
	{
		IApplicationServerAccess asa = getApplicationServerAccess();
		if (asa != null)
		{
			return asa.getTeamRepository();
		}
		return null;
	}

	public boolean isInprocessApplicationServer()
	{
		return isInprocessApplicationServer;
	}

	public IUserManager getUserManager() throws RepositoryAccessException, ApplicationServerAccessException
	{
		getApplicationServerAccess(); // checks login

		try
		{
			return getApplicationServerAccess().getUserManager(clientID);
		}
		catch (Exception e)
		{
			Debug.error("Could not get user manager", e);
			return null;
		}
	}

	public IDataServer getDataServer() throws RepositoryAccessException
	{
		try
		{
			return getApplicationServerAccess().getDataServer();
		}
		catch (RepositoryAccessException ex)
		{
			throw ex;
		}
		catch (Exception ex)
		{
			ServoyLog.logError("Cannot get data server access", ex);
			throw new RepositoryAccessException("Cannot get data server access.\n" + ex.getMessage());
		}
	}

	public String getClientID()
	{
		return clientID;
	}

	private IApplicationServerAccess getApplicationServerAccess() throws ApplicationServerAccessException, RepositoryAccessException
	{
		if (applicationServerAccess == null)
		{
			applicationServerAccess = createApplicationServerAccess();
		}

		return applicationServerAccess;
	}

	/*
	 * Checks if the remote repository version is the same with the eclipse repository version
	 */
	public void checkRemoteRepositoryVersion() throws RemoteException, RepositoryAccessException
	{
		int remoteRepositoryVersion = getRepository().getRepositoryVersion();
		int localRepository = AbstractRepository.repository_version;
		if (localRepository != remoteRepositoryVersion) throw new RepositoryAccessException("Incompatible repository versions, on the server: " +
			remoteRepositoryVersion + " locally: " + localRepository);
	}

	private IApplicationServerAccess createApplicationServerAccess() throws ApplicationServerAccessException, RepositoryAccessException
	{
		IApplicationServerAccess asa = null;

		if (serverAddress.equals("localhost"))//try inproces //$NON-NLS-1$
		{
			IApplicationServer as = (IApplicationServer)LocalhostRMIRegistry.getService(IApplicationServer.NAME);
			try
			{
				clientID = ApplicationServerSingleton.get().getClientId();//only works for inprocess!
				asa = as.getApplicationServerAccess(clientID); // LocalApplicationServer
			}
			catch (RemoteException e)
			{
				throw new ApplicationServerAccessException("Error getting localhost repository", e); //$NON-NLS-1$
			}
		}
		if (asa == null)
		{
			try
			{
				URL url = new URL("http", serverAddress, "");
				rmiFactoryFactory = createRMIClientSocketFactoryFactory(url, null, getServoySettings(), null);

				IApplicationServer as = (IApplicationServer)LocateRegistry.getRegistry(serverAddress, usedRMIPort,
					rmiFactoryFactory.getRemoteClientSocketFactory()).lookup(IApplicationServer.NAME);

				clientID = as.getClientID(user, passwordHash); // RemoteApplicationServer
				if (clientID != null)
				{
					asa = as.getApplicationServerAccess(clientID);
				}
			}
			catch (Exception e)
			{
				Debug.error("Error getting the repository from host " + serverAddress + ":" + usedRMIPort, e);
				throw new ApplicationServerAccessException("Error getting the repository from host " + serverAddress + ":" + usedRMIPort, e);
			}
			if (clientID == null) throw new RepositoryAccessException("Invalid username or password.");
		}

		return asa;
	}

	public IRMIClientSocketFactoryFactory createRMIClientSocketFactoryFactory(URL url, IApplication application, Properties settings,
		IReconnectListener reconnectListener) throws ApplicationServerAccessException
	{
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IExtensionPoint ep = reg.getExtensionPoint(IRMIClientFactoryProvider.EXTENSION_ID);
		IExtension[] extensions = ep.getExtensions();

		if (extensions == null || extensions.length == 0)
		{
			ServoyLog.logWarning("Could not find rmi client factory provider server starter plugin (extension point " + //$NON-NLS-1$
				IRMIClientFactoryProvider.EXTENSION_ID + ")", null); //$NON-NLS-1$
			return null;
		}
		if (extensions.length > 1)
		{
			ServoyLog.logWarning("Multiple rmi client factory plugins found (extension point " + //$NON-NLS-1$
				IRMIClientFactoryProvider.EXTENSION_ID + ")", null); //$NON-NLS-1$
		}
		IConfigurationElement[] ce = extensions[0].getConfigurationElements();
		if (ce == null || ce.length == 0)
		{
			ServoyLog.logWarning("Could not read rmi client factory provider plugin (extension point " + IRMIClientFactoryProvider.EXTENSION_ID + ")", null); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
		if (ce.length > 1)
		{
			ServoyLog.logWarning("Multiple extensions for rmi client factory plugins found (extension point " + //$NON-NLS-1$
				IRMIClientFactoryProvider.EXTENSION_ID + ")", null); //$NON-NLS-1$
		}
		IRMIClientFactoryProvider rmiClientFactoryProvider;
		try
		{
			rmiClientFactoryProvider = (IRMIClientFactoryProvider)ce[0].createExecutableExtension("class"); //$NON-NLS-1$
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Could not create rmi client factory provider plugin (extension point " + IRMIClientFactoryProvider.EXTENSION_ID + ")", e); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
		try
		{
			return rmiClientFactoryProvider.createRMIClientSocketFactoryFactory(url, application, settings, reconnectListener);
		}
		catch (Exception e)
		{
			Debug.error("couldn't instantiate the rmi socketfactory", e); //$NON-NLS-1$
			throw new ApplicationServerAccessException("Error getting remote repository", e); //$NON-NLS-1$

		}
	}


	private Properties getServoySettings()
	{
		return ServoyModel.getSettings();
	}

}
