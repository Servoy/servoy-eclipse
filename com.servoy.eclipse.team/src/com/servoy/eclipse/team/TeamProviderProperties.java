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
package com.servoy.eclipse.team;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.util.Debug;

public class TeamProviderProperties
{
	private static final String FILE_NAME = ".teamprovider";
	private static final String SECURE_STORAGE_NODE = "STP";

	private static final String SERVER_ADDRESS_KEY = "SERVER_ADDRESS";
	private static final String USER_KEY = "USER";
	private static final String PASSWORD_KEY = "PASSWORD_HASH";
	private static final String SOLUTION_NAME_KEY = "SOLUTION_NAME";
	private static final String SOLUTION_VERSION_KEY = "SOLUTION_VERSION";
	private static final String PROTECTION_PASSWORD_HASH_KEY = "PROTECTION_PASSWORD_HASH";

	private final File projectFolder;
	private final File teamProviderPropertyFile;
	private String serverAddress;
	private String user;
	private String password;
	private String solutionName;
	private int solutionVersion;

	private String repositoryUUID;

	private String protectionPasswordHash;

	private final String secureStorageNode;

	public TeamProviderProperties(File projectFolder)
	{
		this.projectFolder = projectFolder;
		teamProviderPropertyFile = new File(projectFolder, FILE_NAME);
		secureStorageNode = projectFolder.toString().replace(File.separatorChar, '_');
	}

	public String getServerAddress()
	{
		return serverAddress;
	}

	public String getRepositoryUUID()
	{
		return repositoryUUID;
	}

	public void setServerAddress(String serverAddress)
	{
		this.serverAddress = serverAddress;
	}

	public void setRepositoryUUID(String repositoryUUID)
	{
		this.repositoryUUID = repositoryUUID;
	}

	public String getUser()
	{
		return user;
	}

	public void setUser(String user)
	{
		this.user = user;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public String getSolutionName()
	{
		return solutionName;
	}

	public void setSolutionName(String solutionName)
	{
		this.solutionName = solutionName;
	}

	public int getSolutionVersion()
	{
		return solutionVersion;
	}

	public void setSolutionVersion(int solutionVersion)
	{
		this.solutionVersion = solutionVersion;
	}

	public void setProtectionPasswordHash(String protectionPasswordHash)
	{
		this.protectionPasswordHash = protectionPasswordHash;
	}

	public String getProtectionPasswordHash()
	{
		return this.protectionPasswordHash;
	}

	public boolean load()
	{
		// check for secure storage
		ISecurePreferences securePreferences = null;
		try
		{
			securePreferences = SecurePreferencesFactory.getDefault().node(SECURE_STORAGE_NODE).node(secureStorageNode);
			String[] keys = securePreferences.keys();
			if (keys != null && keys.length > 0)
			{
				setServerAddress(securePreferences.get(SERVER_ADDRESS_KEY, RepositoryAccessPoint.LOCALHOST));
				String uuid = securePreferences.get(IRepository.REPOSITORY_UUID_PROPERTY_NAME, null);
				if (uuid == null)
				{
					//backwards compatible get
					uuid = securePreferences.get(IRepository.REPOSITORY_UUID_PROPERTY_NAME.toUpperCase(), null);
				}
				setRepositoryUUID(uuid);
				setUser(securePreferences.get(USER_KEY, null));
				setPassword(securePreferences.get(PASSWORD_KEY, null));
				setSolutionName(securePreferences.get(SOLUTION_NAME_KEY, null));
				setSolutionVersion(Integer.parseInt(securePreferences.get(SOLUTION_VERSION_KEY, null)));
				setProtectionPasswordHash(securePreferences.get(PROTECTION_PASSWORD_HASH_KEY, null));
				return true;
			}
			else
			{
				// load from old prop file
				if (loadV1())
				{
					// clear hash password & write prop to secure storage
					setPassword(null);
					save();
					deleteV1();
					return true;
				}
				else return false;
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}

		return false;
	}

	private boolean loadV1()
	{
		Properties prop = new Properties();
		try
		{
			prop.load(new FileInputStream(teamProviderPropertyFile));
			setServerAddress(prop.getProperty(SERVER_ADDRESS_KEY, RepositoryAccessPoint.LOCALHOST));
			setRepositoryUUID(prop.getProperty(IRepository.REPOSITORY_UUID_PROPERTY_NAME));
			setUser(prop.getProperty(USER_KEY));
			setPassword(prop.getProperty(PASSWORD_KEY));
			setSolutionName(prop.getProperty(SOLUTION_NAME_KEY));
			setSolutionVersion(Integer.parseInt(prop.getProperty(SOLUTION_VERSION_KEY)));
			setProtectionPasswordHash(prop.getProperty(PROTECTION_PASSWORD_HASH_KEY));
		}
		catch (Exception ex)
		{
			Debug.error("Error loading team provider properties " + FILE_NAME, ex);
			return false;
		}

		return true;
	}

	public void save()
	{
		ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault().node(SECURE_STORAGE_NODE).node(secureStorageNode);
		try
		{
			securePreferences.put(SERVER_ADDRESS_KEY, getServerAddress(), false);
			securePreferences.put(IRepository.REPOSITORY_UUID_PROPERTY_NAME, getRepositoryUUID(), false);
			securePreferences.put(USER_KEY, getUser(), false);
			try
			{
				// only secure pwd for remote connections, as for localhost it is not used/needed
				securePreferences.put(PASSWORD_KEY, getPassword(), !RepositoryAccessPoint.LOCALHOST.equals(getServerAddress()));
			}
			catch (StorageException ex1)
			{
				// error during encryption of passwd,
				// skip saving this then
				Debug.error(ex1);
			}
			securePreferences.put(SOLUTION_NAME_KEY, getSolutionName(), false);
			securePreferences.put(SOLUTION_VERSION_KEY, String.valueOf(getSolutionVersion()), false);
			String ph = getProtectionPasswordHash();
			if (ph != null) securePreferences.put(PROTECTION_PASSWORD_HASH_KEY, getProtectionPasswordHash(), false);
			else securePreferences.remove(PROTECTION_PASSWORD_HASH_KEY);
			securePreferences.flush();
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}

	public void delete()
	{
		deleteV1();
		ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault().node(SECURE_STORAGE_NODE).node(secureStorageNode);
		securePreferences.removeNode();
	}

	private void deleteV1()
	{
		if (teamProviderPropertyFile.exists()) teamProviderPropertyFile.delete();
	}
}
