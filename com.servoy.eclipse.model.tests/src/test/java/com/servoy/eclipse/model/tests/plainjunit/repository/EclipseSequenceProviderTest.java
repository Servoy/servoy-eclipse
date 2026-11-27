/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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

package com.servoy.eclipse.model.tests.plainjunit.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.servoy.eclipse.model.ModelTestAccess;
import com.servoy.eclipse.model.repository.EclipseSequenceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.SQLGenerator;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInfoManager;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.persistence.QueryString;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.ITransactionConnection;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.ServoyException;

/**
 * {@link EclipseSequenceProvider}: the next value of a servoy sequence, computed from the max value in the database (through the app
 * server's data server) and cached per column, plus synchronizing the sequence with the database.
 */
class EclipseSequenceProviderTest
{
	private MockedStatic<SQLGenerator> sqlGenerator;
	private MockedStatic<J2DBGlobals> j2dbGlobals;
	private IDataServer dataServer;
	private IServerInternal tableServer;
	private IServerInfoManager serverInfoManager;
	private EclipseSequenceProvider provider;
	private ITable table;
	private Column column;
	private ColumnInfo columnInfo;

	@BeforeEach
	void setUp() throws Exception
	{
		IApplicationServerSingleton applicationServer = ModelTestAccess.installMockApplicationServer();
		dataServer = mock(IDataServer.class);
		when(applicationServer.getDataServer()).thenReturn(dataServer);
		when(applicationServer.getClientId()).thenReturn("client");
		IServerManagerInternal serverManager = mock(IServerManagerInternal.class);
		when(applicationServer.getServerManager()).thenReturn(serverManager);
		tableServer = (IServerInternal)mock(IServer.class, Mockito.withSettings().extraInterfaces(IServerInternal.class));
		when(tableServer.getName()).thenReturn("srv");
		when(serverManager.getServer("srv", false, true)).thenReturn((IServer)tableServer);
		QuerySet querySet = new QuerySet();
		querySet.setSelect(new QueryString("select max(seq) from t", false));
		when(tableServer.getSQLQuerySet(any(QuerySelect.class), any(), anyInt(), anyInt(), anyBoolean(), anyBoolean())).thenReturn(querySet);

		sqlGenerator = Mockito.mockStatic(SQLGenerator.class);
		sqlGenerator.when(() -> SQLGenerator.createAggregateSelect(anyInt(), any(ITable.class), any())).thenReturn(mock(QuerySelect.class));
		j2dbGlobals = Mockito.mockStatic(J2DBGlobals.class);
		j2dbGlobals.when(J2DBGlobals::getServiceProvider).thenReturn(null);

		serverInfoManager = mock(IServerInfoManager.class);
		provider = new EclipseSequenceProvider(serverInfoManager);
		table = mock(ITable.class);
		when(table.getServerName()).thenReturn("srv");
		when(table.getName()).thenReturn("t");
		column = mock(Column.class);
		when(column.getTable()).thenReturn(table);
		columnInfo = new ColumnInfo(true);
		columnInfo.setAutoEnterType(ColumnInfo.SEQUENCE_AUTO_ENTER);
		columnInfo.setSequenceStepSize(1);
		when(column.getColumnInfo()).thenReturn(columnInfo);
	}

	@AfterEach
	void tearDown()
	{
		j2dbGlobals.close();
		sqlGenerator.close();
		ModelTestAccess.reset();
	}

	private void maxValue(String value, int rowCount) throws Exception
	{
		IDataSet dataSet = mock(IDataSet.class);
		when(dataSet.getRowCount()).thenReturn(Integer.valueOf(rowCount));
		when(dataSet.getRow(0)).thenReturn(new Object[] { value });
		when(dataServer.performCustomQuery(anyString(), anyString(), anyString(), any(), any(), any(), anyInt(), anyInt())).thenReturn(dataSet);
	}

	@Test
	@DisplayName("a null column or a column without sequence settings is an error")
	void invalidColumns()
	{
		assertAll(() -> assertEquals("Invalid column", assertThrows(RepositoryException.class, () -> provider.getNextSequence(null, true)).getMessage()),
			() -> {
				columnInfo.setAutoEnterType(ColumnInfo.NO_AUTO_ENTER);
				// the RepositoryException of the provider is itself a ServoyException, so it is wrapped
				assertEquals("error getting next sequence for " + column,
					assertThrows(RepositoryException.class, () -> provider.getNextSequence(column, true)).getMessage());
			});
	}

	@Test
	@DisplayName("without column info the sequence starts at 1")
	void noColumnInfo() throws Exception
	{
		when(column.getColumnInfo()).thenReturn(null);

		assertEquals(Long.valueOf(1), provider.getNextSequence(column, true));
	}

	@Test
	@DisplayName("the next value is the max value in the db plus the step, with the pre/post sequence chars around it")
	void nextValue() throws Exception
	{
		maxValue("ORD-42", 1);

		assertEquals(Long.valueOf(43), provider.getNextSequence(column, false));

		columnInfo.setSequenceStepSize(5);
		columnInfo.setPreSequenceChars("PRE");
		columnInfo.setPostSequenceChars("POST");
		assertEquals("PRE47POST", provider.getNextSequence(column, false));
	}

	@Test
	@DisplayName("a value without a number or without rows gives 1")
	void noNumber() throws Exception
	{
		maxValue(null, 1);
		assertEquals(Long.valueOf(1), provider.getNextSequence(column, false));

		maxValue("no number", 1);
		assertEquals(Long.valueOf(1), provider.getNextSequence(column, false));

		maxValue("42", 0);
		assertEquals(Long.valueOf(1), provider.getNextSequence(column, false));
	}

	@Test
	@DisplayName("the database max value wins over a smaller cached value; a larger cached value wins over the database")
	void cache() throws Exception
	{
		maxValue("10", 1);
		assertEquals(Long.valueOf(11), provider.getNextSequence(column, true));
		// the cache is used: 12, 13
		assertEquals(Long.valueOf(12), provider.getNextSequence(column, true));
		assertEquals(Long.valueOf(13), provider.getNextSequence(column, true));

		// the database moved on: the cache is replaced
		maxValue("100", 1);
		assertEquals(Long.valueOf(101), provider.getNextSequence(column, true));
		assertEquals(Long.valueOf(102), provider.getNextSequence(column, true));

		// a step size change drops the cache
		columnInfo.setSequenceStepSize(2);
		assertEquals(Long.valueOf(105), provider.getNextSequence(column, true));
	}

	@Test
	@DisplayName("a database error while reading the max value becomes a RepositoryException")
	void databaseError() throws Exception
	{
		when(dataServer.performCustomQuery(anyString(), anyString(), anyString(), any(), any(), any(), anyInt(), anyInt()))
			.thenThrow(new ServoyException(ServoyException.BAD_SQL_SYNTAX));

		RepositoryException e = assertThrows(RepositoryException.class, () -> provider.getNextSequence(column, true));
		assertEquals("error getting next sequence for " + column, e.getMessage());
	}

	@Test
	@DisplayName("syncSequence sets the next sequence, the pre and post chars from the max value in the db and saves the column info")
	void syncSequence() throws Exception
	{
		ITransactionConnection connection = mock(ITransactionConnection.class);
		PreparedStatement statement = mock(PreparedStatement.class);
		ResultSet resultSet = mock(ResultSet.class);
		when(tableServer.getUnmanagedConnection()).thenReturn(connection);
		when(connection.prepareStatement("select max(seq) from t")).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(Boolean.TRUE);
		when(resultSet.getString(1)).thenReturn("PRE-42-POST");
		maxValue("PRE-42-POST", 1);

		provider.syncSequence(column);

		assertAll(() -> assertEquals("PRE-", columnInfo.getPreSequenceChars()), () -> assertEquals("-POST", columnInfo.getPostSequenceChars()),
			() -> assertEquals(43, columnInfo.getNextSequence()));
		verify(serverInfoManager).updateAllColumnInfo(table);
	}

	@Test
	@DisplayName("syncSequence without a max value starts at 1; a null column or a sql error is a RepositoryException")
	void syncSequenceEdgeCases() throws Exception
	{
		assertEquals("Can't sync on null column", assertThrows(RepositoryException.class, () -> provider.syncSequence(null)).getMessage());

		when(tableServer.getUnmanagedConnection()).thenThrow(new SQLException("no connection"));
		RepositoryException e = assertThrows(RepositoryException.class, () -> provider.syncSequence(column));
		assertEquals("error synchronizing sequence for " + column, e.getMessage());
	}
}
