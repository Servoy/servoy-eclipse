package com.servoy.eclipse.model.tests.plainjunit.war;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.war.exporter.ServerConfiguration;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.persistence.ServerSettings;


class ServerConfigurationTest
{
	@Test
	@DisplayName("the no-config constructor keeps the name and the NONE defaults for schema and catalog")
	void nameOnly()
	{
		ServerConfiguration sc = new ServerConfiguration("srv");
		assertEquals("srv", sc.getName());
		assertEquals(ServerConfig.NONE, sc.getSchema());
		assertEquals(ServerConfig.NONE, sc.getCatalog());
		assertEquals(0, sc.getConnectionValidationType());
		assertEquals(0, sc.getMaxActive());
	}

	@Test
	@DisplayName("the full constructor copies every field from ServerConfig and ServerSettings (with defaults applied)")
	void full()
	{
		ServerConfig config = mock(ServerConfig.class);
		when(config.getServerUrl()).thenReturn("jdbc:hsqldb:mem:test");
		when(config.getUserName()).thenReturn("sa");
		when(config.getPassword()).thenReturn("secret");
		when(config.getDriver()).thenReturn("org.hsqldb.jdbcDriver");
		when(config.getSchema()).thenReturn("public");
		when(config.getCatalog()).thenReturn("");
		when(config.getConnectionValidationType()).thenReturn(2);
		when(config.getValidationQuery()).thenReturn("SELECT 1");
		when(config.getDataModelCloneFrom()).thenReturn("other");
		when(config.getMaxActive()).thenReturn(10);
		when(config.getMaxIdle()).thenReturn(5);
		when(config.getMaxPreparedStatementsIdle()).thenReturn(3);
		when(config.getSkipSysTables()).thenReturn(true);
		when(config.getPrefixTables()).thenReturn(false);
		when(config.getQueryProcedures()).thenReturn(Boolean.TRUE);
		when(config.isClientOnlyConnections()).thenReturn(true);

		ServerSettings settings = new ServerSettings(false, null, null, null);

		ServerConfiguration sc = new ServerConfiguration("srv", config, settings);
		assertEquals("srv", sc.getName());
		assertEquals("jdbc:hsqldb:mem:test", sc.getServerUrl());
		assertEquals("sa", sc.getUserName());
		assertEquals("secret", sc.getPassword());
		assertEquals("org.hsqldb.jdbcDriver", sc.getDriver());
		assertEquals("public", sc.getSchema());
		assertEquals(ServerConfig.EMPTY, sc.getCatalog());
		assertEquals(2, sc.getConnectionValidationType());
		assertEquals("SELECT 1", sc.getValidationQuery());
		assertEquals("other", sc.getDataModelCloneFrom());
		assertEquals(10, sc.getMaxActive());
		assertEquals(5, sc.getMaxIdle());
		assertEquals(3, sc.getMaxPreparedStatementsIdle());
		assertEquals(true, sc.isSkipSysTables());
		assertEquals(false, sc.isPrefixTables());
		assertEquals(true, sc.isQueryProcedures());
		assertEquals(true, sc.isClientOnlyConnections());
	}

	@Test
	@DisplayName("a null schema/catalog becomes NONE; a blank one becomes EMPTY")
	void displayValue()
	{
		ServerConfig config = mock(ServerConfig.class);
		when(config.getServerUrl()).thenReturn(null);
		when(config.getUserName()).thenReturn(null);
		when(config.getPassword()).thenReturn(null);
		when(config.getDriver()).thenReturn(null);
		when(config.getSchema()).thenReturn(null);
		when(config.getCatalog()).thenReturn("   ");
		when(config.getConnectionValidationType()).thenReturn(0);
		when(config.getValidationQuery()).thenReturn(null);
		when(config.getDataModelCloneFrom()).thenReturn(null);
		when(config.getMaxActive()).thenReturn(0);
		when(config.getMaxIdle()).thenReturn(0);
		when(config.getMaxPreparedStatementsIdle()).thenReturn(0);
		when(config.getSkipSysTables()).thenReturn(false);
		when(config.getPrefixTables()).thenReturn(false);
		when(config.getQueryProcedures()).thenReturn(Boolean.FALSE);
		when(config.isClientOnlyConnections()).thenReturn(false);

		ServerConfiguration sc = new ServerConfiguration("n", config, new ServerSettings(false, null, null, null));
		assertEquals(ServerConfig.NONE, sc.getSchema());
		assertEquals(ServerConfig.EMPTY, sc.getCatalog());
	}

	@Test
	@DisplayName("setters update the corresponding fields")
	void setters()
	{
		ServerConfiguration sc = new ServerConfiguration("n");
		sc.setServerUrl("u");
		sc.setUserName("u");
		sc.setDriver("d");
		sc.setCatalog("");
		sc.setPassword("p");
		sc.setConnectionValidationType(1);
		sc.setValidationQuery("q");
		sc.setDataModelCloneFrom("f");
		sc.setMaxActive(2);
		sc.setMaxIdle(3);
		sc.setMaxPreparedStatementsIdle(4);
		sc.setSchema("s");
		sc.setSkipSysTables(true);
		sc.setPrefixTables(true);
		sc.setQueryProcedures(true);
		sc.setClientOnlyConnections(true);

		assertEquals("u", sc.getServerUrl());
		assertEquals("u", sc.getUserName());
		assertEquals("d", sc.getDriver());
		assertEquals(ServerConfig.EMPTY, sc.getCatalog());
		assertEquals("p", sc.getPassword());
		assertEquals(1, sc.getConnectionValidationType());
		assertEquals("q", sc.getValidationQuery());
		assertEquals("f", sc.getDataModelCloneFrom());
		assertEquals(2, sc.getMaxActive());
		assertEquals(3, sc.getMaxIdle());
		assertEquals(4, sc.getMaxPreparedStatementsIdle());
		assertEquals("s", sc.getSchema());
		assertEquals(true, sc.isSkipSysTables());
		assertEquals(true, sc.isPrefixTables());
		assertEquals(true, sc.isQueryProcedures());
		assertEquals(true, sc.isClientOnlyConnections());
	}
}
