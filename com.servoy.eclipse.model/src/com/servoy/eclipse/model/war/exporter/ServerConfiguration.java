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

package com.servoy.eclipse.model.war.exporter;

import com.servoy.j2db.persistence.ServerConfig;

public class ServerConfiguration
{
	private String serverUrl;
	private String userName;
	private String driver;
	private String catalog;
	private String password;
	private int connectionValidationType;
	private String validationQuery;
	private String dataModelCloneFrom;
	private int maxActive;
	private int maxIdle;
	private int maxPreparedStatementsIdle;
	private String schema;
	private boolean skipSysTables;
	private final String name;

	public ServerConfiguration(String name)
	{
		this.name = name;
		schema = ServerConfig.NONE;
		catalog = ServerConfig.NONE;
	}

	public ServerConfiguration(String name, ServerConfig config)
	{
		this.name = name;
		serverUrl = config.getServerUrl();
		userName = config.getUserName();
		password = config.getPassword();
		driver = config.getDriver();
		schema = getDisplayValue(config.getSchema());
		catalog = getDisplayValue(config.getCatalog());
		connectionValidationType = config.getConnectionValidationType();
		validationQuery = config.getValidationQuery();
		dataModelCloneFrom = config.getDataModelCloneFrom();
		maxActive = config.getMaxActive();
		maxIdle = config.getMaxIdle();
		maxPreparedStatementsIdle = config.getMaxPreparedStatementsIdle();
		skipSysTables = config.getSkipSysTables();
	}

	private String getDisplayValue(String value)
	{
		if (value == null)
		{
			return ServerConfig.NONE;
		}
		else if (value.trim().length() == 0)
		{
			return ServerConfig.EMPTY;
		}
		return value;
	}

	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return the serverUrl
	 */
	public String getServerUrl()
	{
		return serverUrl;
	}

	/**
	 * @return the userName
	 */
	public String getUserName()
	{
		return userName;
	}

	/**
	 * @return the driver
	 */
	public String getDriver()
	{
		return driver;
	}

	/**
	 * @return the catalog
	 */
	public String getCatalog()
	{
		return catalog;
	}

	/**
	 * @return the password
	 */
	public String getPassword()
	{
		return password;
	}

	/**
	 * @return the connectionValidationType
	 */
	public int getConnectionValidationType()
	{
		return connectionValidationType;
	}

	/**
	 * @return the validationQuery
	 */
	public String getValidationQuery()
	{
		return validationQuery;
	}

	/**
	 * @return the dataModelCloneFrom
	 */
	public String getDataModelCloneFrom()
	{
		return dataModelCloneFrom;
	}

	/**
	 * @return the maxActive
	 */
	public int getMaxActive()
	{
		return maxActive;
	}

	/**
	 * @return the maxIdle
	 */
	public int getMaxIdle()
	{
		return maxIdle;
	}

	/**
	 * @return the maxPreparedStatementsIdle
	 */
	public int getMaxPreparedStatementsIdle()
	{
		return maxPreparedStatementsIdle;
	}

	/**
	 * @return the schema
	 */
	public String getSchema()
	{
		return schema;
	}

	/**
	 * @return the skipSysTables
	 */
	public boolean isSkipSysTables()
	{
		return skipSysTables;
	}

	/**
	 * @param serverUrl the serverUrl to set
	 */
	public void setServerUrl(String serverUrl)
	{
		this.serverUrl = serverUrl;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName)
	{
		this.userName = userName;
	}

	/**
	 * @param driver the driver to set
	 */
	public void setDriver(String driver)
	{
		this.driver = driver;
	}

	/**
	 * @param catalog the catalog to set
	 */
	public void setCatalog(String catalog)
	{
		this.catalog = getDisplayValue(catalog);
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password)
	{
		this.password = password;
	}

	/**
	 * @param connectionValidationType the connectionValidationType to set
	 */
	public void setConnectionValidationType(int connectionValidationType)
	{
		this.connectionValidationType = connectionValidationType;
	}

	/**
	 * @param validationQuery the validationQuery to set
	 */
	public void setValidationQuery(String validationQuery)
	{
		this.validationQuery = validationQuery;
	}

	/**
	 * @param dataModelCloneFrom the dataModelCloneFrom to set
	 */
	public void setDataModelCloneFrom(String dataModelCloneFrom)
	{
		this.dataModelCloneFrom = dataModelCloneFrom;
	}

	/**
	 * @param maxActive the maxActive to set
	 */
	public void setMaxActive(int maxActive)
	{
		this.maxActive = maxActive;
	}

	/**
	 * @param maxIdle the maxIdle to set
	 */
	public void setMaxIdle(int maxIdle)
	{
		this.maxIdle = maxIdle;
	}

	/**
	 * @param maxPreparedStatementsIdle the maxPreparedStatementsIdle to set
	 */
	public void setMaxPreparedStatementsIdle(int maxPreparedStatementsIdle)
	{
		this.maxPreparedStatementsIdle = maxPreparedStatementsIdle;
	}

	/**
	 * @param schema the schema to set
	 */
	public void setSchema(String schema)
	{
		this.schema = getDisplayValue(schema);
	}

	/**
	 * @param skipSysTables the skipSysTables to set
	 */
	public void setSkipSysTables(boolean skipSysTables)
	{
		this.skipSysTables = skipSysTables;
	}

}