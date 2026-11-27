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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Types;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.servoy.eclipse.model.Activator;
import com.servoy.eclipse.model.ModelTestAccess;
import com.servoy.eclipse.model.builder.MarkerMessages;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.tests.plainjunit.utils.SeverityPreferences;
import com.servoy.eclipse.model.tests.plainjunit.utils.WorkspaceMarkers;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IServerInfoManager;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.persistence.ServerSettings;
import com.servoy.j2db.persistence.TableMetaInfo;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.DatabaseUtils;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;
import com.servoy.j2db.util.xmlxport.TableDef;

/**
 * {@link DataModelManager}: .dbi file names and locations, (de)serializing table information, loading column info from .dbi files and the
 * differences between the .dbi files and the database. The resources project is a {@link WorkspaceMarkers} project.
 * <p>
 * The problem markers themselves are created in a workspace job, and there is no workspace outside Eclipse, so these tests keep the markers
 * suppressed (as during a solution import) and assert the recorded differences - the markers are just a rendering of them.
 */
class DataModelManagerTest
{
	private static final String DBI = "/res/datasources/example_data/orders.dbi";

	private SeverityPreferences preferences;
	private WorkspaceMarkers workspace;
	private MockedStatic<Activator> activator;
	private IProject resources;
	private IServerManagerInternal serverManager;
	private DataModelManager manager;
	private IServerInternal server;

	@BeforeEach
	void setUp()
	{
		preferences = new SeverityPreferences();
		workspace = new WorkspaceMarkers();
		// DbiPreferences reads the model plugin's instance preferences: answer with the defaults
		Activator plugin = mock(Activator.class);
		IEclipsePreferences pluginPreferences = mock(IEclipsePreferences.class);
		when(pluginPreferences.get(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(1));
		when(plugin.getEclipsePreferences()).thenReturn(pluginPreferences);
		activator = Mockito.mockStatic(Activator.class);
		activator.when(Activator::getDefault).thenReturn(plugin);

		IServoyModel model = ModelTestAccess.installMockServoyModel();
		when(model.getModulesOfActiveProject()).thenReturn(new ServoyProject[0]);
		resources = workspace.project("res");
		serverManager = mock(IServerManagerInternal.class);
		when(serverManager.getServerNames(false, false, true, true)).thenReturn(new String[0]);
		// the server manager hands out IServer, the manager casts it to IServerInternal
		server = (IServerInternal)mock(com.servoy.j2db.persistence.IServer.class, Mockito.withSettings().extraInterfaces(IServerInternal.class));
		ServerConfig config = mock(ServerConfig.class);
		when(config.isEnabled()).thenReturn(Boolean.TRUE);
		when(server.getConfig()).thenReturn(config);
		when(server.isValid()).thenReturn(Boolean.TRUE);
		try
		{
			when(server.getName()).thenReturn("example_data");
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
		when(serverManager.getServer("example_data")).thenReturn((com.servoy.j2db.persistence.IServer)server);
		manager = new DataModelManager(resources, serverManager);
		// no workspace for the marker jobs in a plain unit test; the differences are recorded all the same
		manager.suppressProblemMarkers(true);
	}

	@AfterEach
	void tearDown()
	{
		workspace.waitForJobs();
		ModelTestAccess.reset();
		activator.close();
		workspace.close();
		preferences.close();
	}

	/** A column mock that keeps the column info that is set on it. */
	private static Column column(String name, int sqlType, int length, boolean allowNull, boolean pk)
	{
		Column column = mock(Column.class);
		ColumnType type = ColumnType.getInstance(sqlType, length, 0);
		ColumnInfo[] info = new ColumnInfo[1];
		when(column.getName()).thenReturn(name);
		when(column.getColumnType()).thenReturn(type);
		when(column.getConfiguredColumnType()).thenReturn(type);
		when(column.getAllowNull()).thenReturn(Boolean.valueOf(allowNull));
		when(column.isDatabasePK()).thenReturn(Boolean.valueOf(pk));
		when(column.getFlags()).thenReturn(Integer.valueOf(pk ? IColumn.PK_COLUMN : IColumn.NORMAL_COLUMN));
		when(column.getExistInDB()).thenReturn(Boolean.TRUE);
		when(column.getColumnInfo()).thenAnswer(invocation -> info[0]);
		doAnswer(invocation -> {
			info[0] = invocation.getArgument(0);
			return null;
		}).when(column).setColumnInfo(any());
		return column;
	}

	private static ITable table(String name, Column... columns)
	{
		ITable table = mock(ITable.class);
		when(table.getName()).thenReturn(name);
		when(table.getServerName()).thenReturn("example_data");
		when(table.getDataSource()).thenReturn("db:/example_data/" + name);
		when(table.getExistInDB()).thenReturn(Boolean.TRUE);
		when(table.getRowIdentColumnsCount()).thenReturn(Integer.valueOf(1));
		when(table.getColumns()).thenReturn(List.of(columns));
		when(table.getColumnsSortedByName()).thenAnswer(invocation -> List.of(columns).iterator());
		for (Column column : columns)
		{
			when(table.getColumn(column.getName())).thenReturn(column);
			when(column.getTable()).thenReturn(null);
		}
		return table;
	}

	private static String dbi(String tableName, String columnsJson)
	{
		return "{name:\"" + tableName + "\",tableType:0,columns:[" + columnsJson + "]}";
	}

	private static String dbiColumn(String name, int sqlType, int length, boolean allowNull, int flags)
	{
		return "{name:\"" + name + "\",dataType:" + sqlType + ",length:" + length + ",allowNull:" + allowNull + ",flags:" + flags +
			",creationOrderIndex:0}";
	}

	@Nested
	@DisplayName("file names and locations")
	class Files
	{
		@Test
		@DisplayName("dbi, security, server and metadata files are in datasources/<server> of the resources project")
		void locations()
		{
			assertAll(() -> assertEquals("orders.dbi", DataModelManager.getDBIFileName("orders")),
				() -> assertEquals("orders.sec", DataModelManager.getSecurityFileName("orders")),
				() -> assertEquals("datasources/example_data", DataModelManager.getRelativeServerPath("example_data")),
				() -> assertEquals("", DataModelManager.getRelativeServerPath(null)),
				() -> assertEquals(DBI, manager.getDBIFile("example_data", "orders").getFullPath().toString()),
				() -> assertEquals(DBI, manager.getDBIFile("db:/example_data/orders").getFullPath().toString()),
				() -> assertNull(manager.getDBIFile("mem:orders")),
				() -> assertEquals("/res/datasources/example_data/orders.sec", manager.getSecurityFile("example_data", "orders").getFullPath().toString()),
				() -> assertEquals("/res/datasources/example_data.dbi", manager.getServerDBIFile("example_data").getFullPath().toString()),
				() -> assertEquals("/res/security/security.sec", manager.getSecurityFile().getFullPath().toString()),
				() -> assertEquals("/res/datasources/example_data/orders.data", manager.getMetaDataFile("db:/example_data/orders").getFullPath().toString()),
				() -> assertNull(manager.getMetaDataFile("mem:orders")),
				() -> assertEquals("/res/datasources/example_data", manager.getServerInformationFolder("example_data").getFullPath().toString()));
		}

		@Test
		@DisplayName("without resources project there are no files")
		void noResourcesProject()
		{
			DataModelManager noProject = new DataModelManager(null, serverManager);

			assertAll(() -> assertNull(noProject.getDBIFile("example_data", "orders")), () -> assertNull(noProject.getSecurityFile("example_data", "orders")),
				() -> assertNull(noProject.getServerDBIFile("example_data")), () -> assertNull(noProject.getSecurityFile()),
				() -> assertNull(noProject.getMetaDataFile("db:/example_data/orders")), () -> assertNull(noProject.getServerInformationFolder("example_data")));
		}

		@Test
		@DisplayName("getColumnInfoManager finds the DataModelManager among the server info managers")
		void columnInfoManager()
		{
			when(serverManager.getServerInfoManagers()).thenReturn(new IServerInfoManager[] { mock(IServerInfoManager.class), manager });
			IServerManagerInternal none = mock(IServerManagerInternal.class);

			assertAll(() -> assertSame(manager, DataModelManager.getColumnInfoManager(serverManager)),
				() -> assertNull(DataModelManager.getColumnInfoManager(none)));
		}
	}

	@Nested
	@DisplayName("serializing table information")
	class Serialize
	{
		@Test
		@DisplayName("getColumnInfoDef copies the column info; without column info only for non-stored columns")
		void columnInfoDef()
		{
			Column withInfo = column("order_id", Types.INTEGER, 0, false, true);
			ColumnInfo info = new ColumnInfo(true);
			info.setTitleText("Order");
			info.setAutoEnterType(ColumnInfo.SEQUENCE_AUTO_ENTER);
			withInfo.setColumnInfo(info);
			Column withoutInfo = column("note", Types.VARCHAR, 50, true, false);

			ColumnInfoDef def = DataModelManager.getColumnInfoDef(withInfo, 3);

			assertAll(() -> assertEquals("order_id", def.name), () -> assertEquals("Order", def.titleText), () -> assertEquals(3, def.creationOrderIndex),
				() -> assertEquals(ColumnInfo.SEQUENCE_AUTO_ENTER, def.autoEnterType), () -> assertEquals(IColumn.PK_COLUMN, def.flags),
				() -> assertNull(DataModelManager.getColumnInfoDef(withoutInfo, 0)),
				() -> assertEquals("note", DataModelManager.getColumnInfoDef(withoutInfo, 1, false).name));
		}

		@Test
		@DisplayName("serializeTableInfo writes what deserializeTableInfo reads back")
		void roundTrip() throws Exception
		{
			TableDef def = new TableDef();
			def.name = "orders";
			def.hiddenInDeveloper = true;
			def.isMetaData = Boolean.TRUE;
			ColumnInfoDef column = new ColumnInfoDef();
			column.name = "order_id";
			column.columnType = ColumnType.getInstance(Types.NUMERIC, 10, 2);
			column.flags = IColumn.PK_COLUMN;
			column.autoEnterType = ColumnInfo.SEQUENCE_AUTO_ENTER;
			column.autoEnterSubType = ColumnInfo.SERVOY_SEQUENCE;
			column.sequenceStepSize = 5;
			column.titleText = "Order";
			column.converterProperties = "{\"a\":\"b\"}";
			column.sortIgnorecase = Boolean.TRUE;
			def.columnInfoDefSet.add(column);

			TableDef back = DatabaseUtils.deserializeTableInfo(manager.serializeTableInfo(def));
			ColumnInfoDef backColumn = back.columnInfoDefSet.get(0);

			assertAll(() -> assertEquals("orders", back.name), () -> assertTrue(back.hiddenInDeveloper), () -> assertEquals(Boolean.TRUE, back.isMetaData),
				() -> assertEquals("order_id", backColumn.name), () -> assertEquals(column.columnType, backColumn.columnType),
				() -> assertEquals(IColumn.PK_COLUMN, backColumn.flags), () -> assertEquals(ColumnInfo.SEQUENCE_AUTO_ENTER, backColumn.autoEnterType),
				() -> assertEquals(5, backColumn.sequenceStepSize), () -> assertEquals("Order", backColumn.titleText),
				() -> assertEquals(Boolean.TRUE, backColumn.sortIgnorecase), () -> assertTrue(backColumn.converterProperties.contains("\"a\"")));
		}

		@Test
		@DisplayName("serializeTable writes the stored columns sorted by name")
		void serializeTable() throws Exception
		{
			Column a = column("a_col", Types.INTEGER, 0, true, false);
			Column b = column("b_col", Types.VARCHAR, 20, true, false);
			a.setColumnInfo(new ColumnInfo(true));
			b.setColumnInfo(new ColumnInfo(true));
			ITable orders = table("orders", a, b);

			TableDef back = DatabaseUtils.deserializeTableInfo(manager.serializeTable(orders));

			assertEquals(List.of("a_col", "b_col"), back.columnInfoDefSet.stream().map(cid -> cid.name).toList());
		}
	}

	@Nested
	@DisplayName("loading column info and difference markers")
	class Load
	{
		@Test
		@DisplayName("a matching .dbi file sets the column info and gives no difference")
		void matchingDbi() throws Exception
		{
			Column id = column("order_id", Types.INTEGER, 0, false, true);
			ITable orders = table("orders", id);
			workspace.withFile(DBI, dbi("orders", dbiColumn("order_id", Types.INTEGER, 0, false, IColumn.PK_COLUMN)));

			manager.loadAllColumnInfo(orders);

			assertAll(() -> assertTrue(id.getColumnInfo() != null),
				() -> assertNull(manager.getColumnDifference("example_data", "orders", "order_id")));
			verify(server).setTableMarkedAsHiddenInDeveloper(orders, false);
		}

		@Test
		@DisplayName("a different allowNull is a column conflict (warning); a db pk that the .dbi does not know as pk is an error")
		void conflicts() throws Exception
		{
			Column note = column("note", Types.VARCHAR, 50, true, false);
			Column id = column("order_id", Types.INTEGER, 0, false, true);
			ITable orders = table("orders", note, id);
			workspace.withFile(DBI, dbi("orders", dbiColumn("note", Types.VARCHAR, 50, false, 0) + "," + dbiColumn("order_id", Types.INTEGER, 0, false, 0)));

			manager.loadAllColumnInfo(orders);

			TableDifference noteDiff = manager.getColumnDifference("example_data", "orders", "note");
			TableDifference idDiff = manager.getColumnDifference("example_data", "orders", "order_id");
			assertAll(() -> assertEquals(TableDifference.COLUMN_CONFLICT, noteDiff.getType()),
				() -> assertEquals(IMarker.SEVERITY_WARNING, noteDiff.getSeverity()), () -> assertEquals(IMarker.SEVERITY_ERROR, idDiff.getSeverity()),
				() -> assertEquals(MarkerMessages.DBIColumnConflict
					.fill("example_data->orders->note", "(TEXT(id:12), length: 50, allowNull: true)", "(TEXT(id:12), length: 50, allowNull: false)")
					.getText(), noteDiff.getUserFriendlyMessage()),
				() -> assertEquals(MarkerMessages.DBIColumnConflict.fill("example_data->orders->order_id", "(pk, INTEGER(id:4), length: 0, allowNull: false)",
					"(INTEGER(id:4), length: 0, allowNull: false)").getText(), idDiff.getUserFriendlyMessage()));
		}

		@Test
		@DisplayName("a .dbi column missing in the db and a db column missing in the .dbi are reported; the latter gets new column info")
		void missingColumns() throws Exception
		{
			Column extra = column("extra", Types.INTEGER, 0, true, false);
			ITable orders = table("orders", extra);
			workspace.withFile(DBI, dbi("orders", dbiColumn("gone", Types.INTEGER, 0, true, 0)));

			manager.loadAllColumnInfo(orders);

			assertAll(() -> assertEquals(TableDifference.COLUMN_MISSING_FROM_DB, manager.getColumnDifference("example_data", "orders", "gone").getType()),
				() -> assertEquals(TableDifference.COLUMN_MISSING_FROM_DBI_FILE, manager.getColumnDifference("example_data", "orders", "extra").getType()),
				() -> assertTrue(extra.getColumnInfo() != null),
				() -> assertEquals(MarkerMessages.DBIColumnMissingFromDB.fill("example_data->orders->gone").getText(),
					manager.getColumnDifference("example_data", "orders", "gone").getUserFriendlyMessage()),
				() -> assertEquals(MarkerMessages.DBIColumnMissingFromDBIFile.fill("example_data->orders->extra").getText(),
					manager.getColumnDifference("example_data", "orders", "extra").getUserFriendlyMessage()));
		}

		@Test
		@DisplayName("a table without row ident columns is marked invalid")
		void noPk() throws Exception
		{
			Column note = column("note", Types.VARCHAR, 50, true, false);
			ITable orders = table("orders", note);
			when(orders.getRowIdentColumnsCount()).thenReturn(Integer.valueOf(0));
			workspace.withFile(DBI, dbi("orders", dbiColumn("note", Types.VARCHAR, 50, true, 0)));

			manager.loadAllColumnInfo(orders);

			verify(orders).setTableInvalidInDeveloperBecauseNoPk(true);
		}

		@Test
		@DisplayName("a .dbi for another table name or broken json is a deserialize problem")
		void badDbi() throws Exception
		{
			ITable orders = table("orders", column("note", Types.VARCHAR, 50, true, false));
			workspace.withFile(DBI, dbi("other", ""));

			RepositoryException wrongName = assertThrows(RepositoryException.class, () -> manager.loadAllColumnInfo(orders));
			TableDifference nameDifference = manager.getColumnDifference("example_data", "orders", null);
			assertAll(() -> assertEquals("Table name does not match dbi file name for orders", wrongName.getMessage()),
				() -> assertEquals(TableDifference.DESERIALIZE_PROBLEM, nameDifference.getType()),
				() -> assertEquals(wrongName.getMessage(), nameDifference.getCustomMessage()));

			// a new manager, so that the difference of the first .dbi file does not hide the second one
			DataModelManager brokenManager = new DataModelManager(resources, serverManager);
			workspace.withFile(DBI, "{name:");
			RepositoryException brokenJson = assertThrows(RepositoryException.class, () -> brokenManager.loadAllColumnInfo(orders));
			TableDifference jsonDifference = brokenManager.getColumnDifference("example_data", "orders", null);
			assertAll(() -> assertEquals(TableDifference.DESERIALIZE_PROBLEM, jsonDifference.getType()),
				() -> assertEquals(brokenJson.getCause().getMessage(), jsonDifference.getCustomMessage()),
				() -> assertTrue(jsonDifference.getCustomMessage().contains("character"), jsonDifference.getCustomMessage()));
		}

		@Test
		@DisplayName("without a .dbi file a missing dbi marker goes on the resources project and the columns get new column info")
		void missingDbi() throws Exception
		{
			Column id = column("order_id", Types.INTEGER, 0, false, true);
			ITable orders = table("orders", id);
			when(orders.getPKColumnTypeRowIdentCount()).thenReturn(Integer.valueOf(1));
			when(id.getRowIdentType()).thenReturn(Integer.valueOf(IColumn.PK_COLUMN));
			when(id.getSequenceType()).thenReturn(Integer.valueOf(ColumnInfo.NO_SEQUENCE_SELECTED));
			ModelTestAccess.installMockApplicationServer();

			manager.loadAllColumnInfo(orders);

			TableDifference difference = manager.getColumnDifference("example_data", "orders", null);
			assertAll(() -> assertEquals(TableDifference.MISSING_DBI_FILE, difference.getType()),
				() -> assertEquals(IMarker.SEVERITY_ERROR, difference.getSeverity()),
				() -> assertEquals(MarkerMessages.DBIFileMissing.fill("example_data->orders").getText(), difference.getUserFriendlyMessage()),
				() -> assertEquals(ColumnInfo.SEQUENCE_AUTO_ENTER, id.getColumnInfo().getAutoEnterType(), "a single integer pk gets a servoy sequence"));
		}

		@Test
		@DisplayName("a .dbi file for a table that is not in the db gives a missing table marker on the .dbi file")
		void missingTable()
		{
			workspace.withFile(DBI, dbi("orders", ""));

			manager.updateMarkerStatesForMissingTable(null, "example_data", "orders");

			TableDifference difference = manager.getColumnDifference("example_data", "orders", null);
			assertAll(() -> assertEquals(TableDifference.MISSING_TABLE, difference.getType()),
				() -> assertEquals(MarkerMessages.DBITableMissing.fill("example_data->orders").getText(), difference.getUserFriendlyMessage()));
		}

		@Test
		@DisplayName("differences of TEMP_ tables are warnings; IGNORE suppresses the difference")
		void severities()
		{
			manager.addMissingDBIMarker("example_data", "temp_orders", false);
			preferences.set(ServoyBuilder.DBI_FILE_MISSING.getLeft(), "IGNORE");
			manager.addMissingDBIMarker("example_data", "orders", false);

			assertAll(() -> assertEquals(IMarker.SEVERITY_WARNING, manager.getColumnDifference("example_data", "temp_orders", null).getSeverity()),
				() -> assertNull(manager.getColumnDifference("example_data", "orders", null)));
		}

		@Test
		@DisplayName("suppressing the problem markers keeps the differences")
		void suppress()
		{
			manager.suppressProblemMarkers(true);
			manager.addMissingDBIMarker("example_data", "orders", false);
			assertEquals(TableDifference.MISSING_DBI_FILE, manager.getColumnDifference("example_data", "orders", null).getType());

			manager.suppressProblemMarkers(false);
			assertEquals(TableDifference.MISSING_DBI_FILE, manager.getColumnDifference("example_data", "orders", null).getType(),
				"switching the markers back on only restores the markers of the known differences");
		}

		@Test
		@DisplayName("getTableMetainfo reads hidden and metadata flags from the .dbi file")
		void tableMetaInfo()
		{
			workspace.withFile(DBI, "{name:\"orders\",tableType:0,hiddenInDeveloper:true,isMetaData:true,columns:[]}");

			TableMetaInfo info = manager.getTableMetainfo(server, "orders");

			assertAll(() -> assertTrue(info.hiddenInDeveloper), () -> assertTrue(info.isMetaData),
				() -> assertSame(TableMetaInfo.DEFAULT, manager.getTableMetainfo(server, "other")),
				() -> assertSame(TableMetaInfo.DEFAULT, manager.getTableMetainfo(null, "orders")));
		}

		@Test
		@DisplayName("server settings are read from the server .dbi file, DEFAULT without it")
		void serverSettings() throws Exception
		{
			assertSame(ServerSettings.DEFAULT, manager.loadServerSettings("example_data"));

			workspace.withFile("/res/datasources/example_data.dbi", DatabaseUtils.serializeServerSettings(new ServerSettings(true, null, null, null)));
			assertTrue(manager.loadServerSettings("example_data").isSortIgnorecase());
		}
	}
}
