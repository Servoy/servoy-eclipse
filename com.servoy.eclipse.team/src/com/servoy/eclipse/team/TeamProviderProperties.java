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
import java.io.FileOutputStream;
import java.util.Properties;

import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.util.Debug;

public class TeamProviderProperties
{
	private static final String FILE_NAME = ".teamprovider";

	private static final String SERVER_ADDRESS_KEY = "SERVER_ADDRESS";
	private static final String USER_KEY = "USER";
	private static final String PASSWORD_HASH_KEY = "PASSWORD_HASH";
	private static final String SOLUTION_NAME_KEY = "SOLUTION_NAME";
	private static final String SOLUTION_VERSION_KEY = "SOLUTION_VERSION";
	private static final String PROTECTION_PASSWORD_HASH_KEY = "PROTECTION_PASSWORD_HASH";

	private final File projectFolder;
	private final File teamProviderPropertyFile;
	private String serverAddress;
	private String user;
	private String passwordHash;
	private String solutionName;
	private int solutionVersion;

	private String repositoryUUID;

	private String protectionPasswordHash;

	public TeamProviderProperties(File projectFolder)
	{
		this.projectFolder = projectFolder;
		teamProviderPropertyFile = new File(projectFolder, FILE_NAME);
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

	public String getPasswordHash()
	{
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash)
	{
		this.passwordHash = passwordHash;
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
		Properties prop = new Properties();
		try
		{
			prop.load(new FileInputStream(teamProviderPropertyFile));
			setServerAddress(prop.getProperty(SERVER_ADDRESS_KEY, "localhost"));
			String uuid = prop.getProperty(IRepository.REPOSITORY_UUID_PROPERTY_NAME);
			if (uuid == null)
			{
				//backwards compatible get
				uuid = prop.getProperty(IRepository.REPOSITORY_UUID_PROPERTY_NAME.toUpperCase());
			}
			setRepositoryUUID(uuid);
			setUser(prop.getProperty(USER_KEY));
			setPasswordHash(prop.getProperty(PASSWORD_HASH_KEY));
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
		Properties prop = new Properties();

		prop.setProperty(SERVER_ADDRESS_KEY, getServerAddress());
		prop.setProperty(IRepository.REPOSITORY_UUID_PROPERTY_NAME, getRepositoryUUID());
		prop.setProperty(USER_KEY, getUser());
		prop.setProperty(PASSWORD_HASH_KEY, getPasswordHash());
		prop.setProperty(SOLUTION_NAME_KEY, getSolutionName());
		prop.setProperty(SOLUTION_VERSION_KEY, String.valueOf(getSolutionVersion()));

		String ph = getProtectionPasswordHash();
		if (ph != null) prop.setProperty(PROTECTION_PASSWORD_HASH_KEY, getProtectionPasswordHash());
		else prop.remove(PROTECTION_PASSWORD_HASH_KEY);

		try
		{
			prop.store(new FileOutputStream(teamProviderPropertyFile), "Servoy team file");
		}
		catch (Exception ex)
		{
			Debug.error("Error saving team provider properties " + FILE_NAME, ex);
		}
	}

	public void delete()
	{
		teamProviderPropertyFile.delete();
	}
}
