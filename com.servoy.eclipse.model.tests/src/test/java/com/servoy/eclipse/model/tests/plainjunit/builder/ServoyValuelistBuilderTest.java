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

package com.servoy.eclipse.model.tests.plainjunit.builder;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.eclipse.model.ModelTestAccess;
import com.servoy.eclipse.model.builder.BuilderDependencies;
import com.servoy.eclipse.model.builder.MarkerMessages;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.builder.ServoyBuilder.Problem;
import com.servoy.eclipse.model.builder.ServoyValuelistBuilder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.tests.plainjunit.utils.SeverityPreferences;
import com.servoy.eclipse.model.tests.plainjunit.utils.TestPersists;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.UUID;

/**
 * {@link ServoyValuelistBuilder#checkValuelist(ValueList, FlattenedSolution, IServerManagerInternal, boolean)}: the problems reported
 * for each kind of valuelist misconfiguration, the fixes applied with fixIfPossible and the dependencies recorded for incremental builds.
 * Severities come from {@link SeverityPreferences} (instance preferences), the valuelist's parent is not a Solution so no project scope
 * is consulted.
 */
class ServoyValuelistBuilderTest
{
	private static final String DATASOURCE = "db:/example_data/orders";

	private SeverityPreferences preferences;
	private ISupportChilds parent;
	private FlattenedSolution fs;
	private IServerManagerInternal serverManager;
	private IServoyModel model;

	@BeforeEach
	void setUp()
	{
		preferences = new SeverityPreferences();
		parent = mock(ISupportChilds.class);
		fs = mock(FlattenedSolution.class);
		serverManager = mock(IServerManagerInternal.class);
		model = ModelTestAccess.installMockServoyModel();
		BuilderDependencies.getInstance().clear();
	}

	@AfterEach
	void tearDown()
	{
		preferences.close();
		ModelTestAccess.reset();
		BuilderDependencies.getInstance().clear();
	}

	private ValueList valuelist(int type)
	{
		ValueList vl = TestPersists.create(parent, IRepository.VALUELISTS, UUID.randomUUID());
		vl.setName("vl1");
		vl.setValueListType(type);
		return vl;
	}

	private List<Problem> check(ValueList vl)
	{
		return ServoyValuelistBuilder.checkValuelist(vl, fs, serverManager, false);
	}

	private static void assertSingleProblem(List<Problem> problems, int severity, String message)
	{
		assertEquals(1, problems.size(), () -> "expected exactly one problem, got " + messages(problems));
		Problem problem = problems.get(0);
		assertAll(() -> assertEquals(ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE, problem.type),
			() -> assertEquals(severity, problem.severity), () -> assertEquals(message, problem.message));
	}

	private static String messages(List<Problem> problems)
	{
		return problems.stream().map(p -> p.message).toList().toString();
	}

	/** A db server that the server manager returns for its name (IServer for the lookup, IServerInternal for the cast). */
	private IServerInternal server(String name, ITable table) throws Exception
	{
		IServer server = mock(IServer.class, withSettings().extraInterfaces(IServerInternal.class));
		IServerInternal internal = (IServerInternal)server;
		when(internal.getName()).thenReturn(name);
		when(internal.getTable("orders")).thenReturn(table);
		when(serverManager.getServer("example_data")).thenReturn(server);
		return internal;
	}

	private ITable table()
	{
		ITable table = mock(ITable.class);
		when(table.getName()).thenReturn("orders");
		when(table.getDataSource()).thenReturn(DATASOURCE);
		when(table.getRowIdentColumnsCount()).thenReturn(1);
		return table;
	}

	@Nested
	@DisplayName("custom and global method valuelists")
	class CustomValues
	{
		@Test
		@DisplayName("plain custom values report no problem")
		void validCustomValues()
		{
			ValueList vl = valuelist(IValueListConstants.CUSTOM_VALUES);
			vl.setCustomValues("a\r\nb\r\nc");

			assertEquals(List.of(), check(vl));
		}

		@Test
		@DisplayName("display|real pairs on every line report no problem")
		void validPairs()
		{
			ValueList vl = valuelist(IValueListConstants.CUSTOM_VALUES);
			vl.setCustomValues("A|1\nB|2");

			assertEquals(List.of(), check(vl));
		}

		@Test
		@DisplayName("a line without | among display|real lines is reported as invalid custom values (warning)")
		void mixedPairsAreInvalid()
		{
			ValueList vl = valuelist(IValueListConstants.CUSTOM_VALUES);
			vl.setCustomValues("A|1\nB\nC|3");

			assertSingleProblem(check(vl), IMarker.SEVERITY_WARNING, MarkerMessages.ValuelistInvalidCustomValues.fill("vl1").getText());
		}

		@Test
		@DisplayName("a custom valuelist with a datasource is an error with a fix, the fix clears datasource and relation")
		void customWithDatasource()
		{
			ValueList vl = valuelist(IValueListConstants.CUSTOM_VALUES);
			vl.setCustomValues("a");
			vl.setDataSource(DATASOURCE);

			List<Problem> problems = ServoyValuelistBuilder.checkValuelist(vl, fs, serverManager, true);

			assertSingleProblem(problems, IMarker.SEVERITY_ERROR, MarkerMessages.ValuelistCustomValuesWithDBInfo.fill("vl1").getText());
			assertAll(() -> assertNotNull(problems.get(0).fix, "this problem has a fix description"),
				() -> assertNull(vl.getDataSource(), "fixIfPossible must clear the datasource"));
		}

		@Test
		@DisplayName("without fixIfPossible the datasource of a custom valuelist is kept")
		void customWithDatasourceNoFix()
		{
			ValueList vl = valuelist(IValueListConstants.CUSTOM_VALUES);
			vl.setDataSource(DATASOURCE);

			check(vl);

			assertEquals(DATASOURCE, vl.getDataSource());
		}

		@Test
		@DisplayName("a global method valuelist whose method is missing is an error")
		void globalMethodNotFound()
		{
			ValueList vl = valuelist(IValueListConstants.GLOBAL_METHOD_VALUES);
			vl.setCustomValues("scopes.globals.missing");

			assertSingleProblem(check(vl), IMarker.SEVERITY_ERROR, MarkerMessages.ValuelistGlobalMethodNotFound.fill("vl1").getText());
		}

		private ScriptMethod method(ISupportChilds methodParent, String declaration)
		{
			ScriptMethod method = TestPersists.create(methodParent, IRepository.METHODS, UUID.randomUUID());
			method.setName("getValues");
			method.setScopeName("globals");
			method.setDeclaration(declaration);
			when(fs.getScriptMethod("scopes.globals.getValues")).thenReturn(method);
			return method;
		}

		private ValueList globalMethodValuelist()
		{
			ValueList vl = valuelist(IValueListConstants.GLOBAL_METHOD_VALUES);
			vl.setCustomValues("scopes.globals.getValues");
			return vl;
		}

		@Test
		@DisplayName("a usable global method reports no problem and makes the valuelist depend on the method's scope")
		void globalMethodFound()
		{
			ValueList vl = globalMethodValuelist();
			method(mock(ISupportChilds.class), "function getValues() {}");

			assertAll(() -> assertEquals(List.of(), check(vl)),
				() -> assertEquals(List.of(vl), BuilderDependencies.getInstance().getScopeDependency("globals")));
		}

		@Test
		@DisplayName("a @private global method of another parent is not accessible (error)")
		void privateMethodOfOtherParent()
		{
			ValueList vl = globalMethodValuelist();
			method(mock(ISupportChilds.class), "/** @private */\nfunction getValues() {}");

			assertSingleProblem(check(vl), IMarker.SEVERITY_ERROR, MarkerMessages.ValuelistGlobalMethodNotAccessible.fill("vl1").getText());
		}

		@Test
		@DisplayName("a @private global method with the same parent is accessible")
		void privateMethodOfSameParent()
		{
			ValueList vl = globalMethodValuelist();
			method(parent, "/** @private */\nfunction getValues() {}");

			assertEquals(List.of(), check(vl));
		}

		@Test
		@DisplayName("a @deprecated global method is reported as deprecated usage (warning)")
		void deprecatedMethod()
		{
			ValueList vl = globalMethodValuelist();
			method(mock(ISupportChilds.class), "/** @deprecated */\nfunction getValues() {}");

			List<Problem> problems = check(vl);

			assertEquals(1, problems.size(), () -> messages(problems));
			assertAll(() -> assertEquals(ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE, problems.get(0).type),
				() -> assertEquals(IMarker.SEVERITY_WARNING, problems.get(0).severity),
				() -> assertEquals(MarkerMessages.ElementUsingDeprecatedFunction.fill("getValues()", "valuelist vl1", "Global method").getText(),
					problems.get(0).message));
		}
	}

	@Nested
	@DisplayName("database valuelists")
	class DatabaseValues
	{
		@Test
		@DisplayName("custom values on a database valuelist are an error, the fix removes them")
		void dbWithCustomValues() throws Exception
		{
			ValueList vl = valuelist(IValueListConstants.DATABASE_VALUES);
			vl.setCustomValues("a");
			vl.setDataSource(DATASOURCE);
			ITable table = table();
			when(table.getColumn("order_id")).thenReturn(mock(Column.class));
			vl.setDataProviderID1("order_id");
			server("example_data", table);

			List<Problem> problems = ServoyValuelistBuilder.checkValuelist(vl, fs, serverManager, true);

			assertAll(() -> assertEquals(MarkerMessages.ValuelistDBWithCustomValues.fill("vl1").getText(), problems.get(0).message),
				() -> assertEquals(IMarker.SEVERITY_ERROR, problems.get(0).severity),
				() -> assertNull(vl.getCustomValues(), "fixIfPossible must clear the custom values"));
		}

		@Test
		@DisplayName("neither a datasource nor a relation is an error")
		void noTableNoRelation()
		{
			ValueList vl = valuelist(IValueListConstants.DATABASE_VALUES);

			assertSingleProblem(check(vl), IMarker.SEVERITY_ERROR, MarkerMessages.ValuelistDBNotTableOrRelation.fill("vl1").getText());
		}

		@Test
		@DisplayName("a datasource without a server name is a malformed table definition")
		void malformedDatasource()
		{
			ValueList vl = valuelist(IValueListConstants.DATABASE_VALUES);
			vl.setDataSource("db://orders");

			assertSingleProblem(check(vl), IMarker.SEVERITY_ERROR,
				MarkerMessages.ValuelistDBMalformedTableDefinition.fill("vl1", "db://orders").getText());
		}

		@Test
		@DisplayName("an unknown server is not reported here, but the datasource dependency is recorded")
		void unknownServer()
		{
			ValueList vl = valuelist(IValueListConstants.DATABASE_VALUES);
			vl.setDataSource(DATASOURCE);

			assertAll(() -> assertEquals(List.of(), check(vl)),
				() -> assertEquals(List.of(vl), BuilderDependencies.getInstance().getDatasourceDependency(DATASOURCE)));
		}

		@Test
		@DisplayName("a server that answers under another name is a duplicate server (warning)")
		void serverDuplicate() throws Exception
		{
			ValueList vl = valuelist(IValueListConstants.DATABASE_VALUES);
			vl.setDataSource(DATASOURCE);
			ITable table = table();
			server("Example_Data", table);
			vl.setDataProviderID1("order_id");
			when(table.getColumn("order_id")).thenReturn(mock(Column.class));

			assertSingleProblem(check(vl), IMarker.SEVERITY_WARNING, MarkerMessages.ValuelistDBServerDuplicate.fill("vl1", "example_data").getText());
		}

		@Test
		@DisplayName("a missing table is reported as not accessible")
		void tableNotFound() throws Exception
		{
			ValueList vl = valuelist(IValueListConstants.DATABASE_VALUES);
			vl.setDataSource(DATASOURCE);
			server("example_data", null);

			assertSingleProblem(check(vl), IMarker.SEVERITY_ERROR, MarkerMessages.ValuelistDBTableNotAccessible.fill("vl1", "orders").getText());
		}

		@Test
		@DisplayName("a table hidden in developer is reported with low priority")
		void hiddenTable() throws Exception
		{
			ValueList vl = valuelist(IValueListConstants.DATABASE_VALUES);
			vl.setDataSource(DATASOURCE);
			ITable table = table();
			when(table.isMarkedAsHiddenInDeveloper()).thenReturn(true);
			when(table.getColumn("order_id")).thenReturn(mock(Column.class));
			vl.setDataProviderID1("order_id");
			server("example_data", table);

			List<Problem> problems = check(vl);

			assertEquals(1, problems.size(), () -> messages(problems));
			assertAll(() -> assertEquals(IMarker.PRIORITY_LOW, problems.get(0).priority),
				() -> assertEquals(IMarker.SEVERITY_WARNING, problems.get(0).severity),
				() -> assertEquals(MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(DATASOURCE, "valuelist ", "vl1").getText(), problems.get(0).message));
		}

		@Test
		@DisplayName("a table without primary key is reported with low priority (warning)")
		void tableWithoutPk() throws Exception
		{
			ValueList vl = valuelist(IValueListConstants.DATABASE_VALUES);
			vl.setDataSource(DATASOURCE);
			ITable table = table();
			when(table.getRowIdentColumnsCount()).thenReturn(0);
			when(table.getColumn("order_id")).thenReturn(mock(Column.class));
			vl.setDataProviderID1("order_id");
			server("example_data", table);

			List<Problem> problems = check(vl);

			assertSingleProblem(problems, IMarker.SEVERITY_WARNING, MarkerMessages.ValuelistDBTableNoPk.fill("vl1", "orders").getText());
			assertEquals(IMarker.PRIORITY_LOW, problems.get(0).priority);
		}

		@Test
		@DisplayName("a table valuelist without any dataprovider is an error")
		void noDataproviders() throws Exception
		{
			ValueList vl = valuelist(IValueListConstants.DATABASE_VALUES);
			vl.setDataSource(DATASOURCE);
			server("example_data", table());

			assertSingleProblem(check(vl), IMarker.SEVERITY_ERROR, MarkerMessages.ValuelistDBNoDataproviders.fill("vl1").getText());
		}

		@Test
		@DisplayName("each missing dataprovider (no column, no calculation) is reported once, duplicates are checked once")
		void missingDataproviders() throws Exception
		{
			ValueList vl = valuelist(IValueListConstants.DATABASE_VALUES);
			vl.setDataSource(DATASOURCE);
			ITable table = table();
			server("example_data", table);
			vl.setDataProviderID1("a");
			vl.setDataProviderID2("b");
			vl.setDataProviderID3("a");

			List<Problem> problems = check(vl);

			assertEquals(List.of(MarkerMessages.ValuelistDBDatasourceNotFound.fill("vl1", "a", "orders").getText(),
				MarkerMessages.ValuelistDBDatasourceNotFound.fill("vl1", "b", "orders").getText()), problems.stream().map(p -> p.message).toList());
		}

		@Test
		@DisplayName("a dataprovider of an excluded column is reported as not found")
		void excludedColumn() throws Exception
		{
			ValueList vl = valuelist(IValueListConstants.DATABASE_VALUES);
			vl.setDataSource(DATASOURCE);
			ITable table = table();
			Column column = mock(Column.class);
			ColumnInfo info = mock(ColumnInfo.class);
			when(info.isExcluded()).thenReturn(true);
			when(column.getColumnInfo()).thenReturn(info);
			when(table.getColumn("secret")).thenReturn(column);
			server("example_data", table);
			vl.setDataProviderID2("secret");

			assertSingleProblem(check(vl), IMarker.SEVERITY_ERROR, MarkerMessages.ValuelistDBDatasourceNotFound.fill("vl1", "secret", "orders").getText());
		}

		@Test
		@DisplayName("use table filter needs a valuelist_name column")
		void tableFilterWithoutNameColumn() throws Exception
		{
			ValueList vl = valuelist(IValueListConstants.DATABASE_VALUES);
			vl.setDataSource(DATASOURCE);
			vl.setUseTableFilter(true);
			ITable table = table();
			when(table.getColumn("order_id")).thenReturn(mock(Column.class));
			vl.setDataProviderID1("order_id");
			server("example_data", table);

			assertSingleProblem(check(vl), IMarker.SEVERITY_ERROR,
				MarkerMessages.ValuelistDBDatasourceNotFound.fill("vl1", "valuelist_name", "orders").getText());
		}

		@Test
		@DisplayName("a relation that does not exist is an error")
		void relationNotFound()
		{
			ValueList vl = valuelist(IValueListConstants.DATABASE_VALUES);
			vl.setRelationName("missing_rel");

			assertSingleProblem(check(vl), IMarker.SEVERITY_ERROR, MarkerMessages.ValuelistRelationNotFound.fill("vl1", "missing_rel").getText());
		}

		@Test
		@DisplayName("a related valuelist with a datasource as well is an error; a relation sequence that does not match up is an error")
		void relationWithDatasourceAndInconsistentSequence()
		{
			ValueList vl = valuelist(IValueListConstants.DATABASE_VALUES);
			vl.setRelationName("orders_to_lines");
			vl.setDataSource(DATASOURCE);
			Relation relation = TestPersists.relation(UUID.randomUUID());
			relation.setName("orders_to_lines");
			relation.setForeignDataSource("db:/example_data/order_lines");
			when(fs.getRelation("orders_to_lines")).thenReturn(relation);
			FlattenedSolution modelFs = mock(FlattenedSolution.class);
			when(modelFs.getRelation("orders_to_lines")).thenReturn(relation);
			when(model.getFlattenedSolution()).thenReturn(modelFs);

			List<Problem> problems = check(vl);

			assertAll(
				() -> assertEquals(List.of(MarkerMessages.ValuelistRelationWithDatasource.fill("vl1").getText(),
					MarkerMessages.ValuelistRelationSequenceInconsistent.fill("vl1", "orders_to_lines").getText()),
					problems.stream().map(p -> p.message).toList()),
				() -> assertEquals(List.of(vl), BuilderDependencies.getInstance().getRelationDependencies(relation)),
				() -> assertEquals(List.of(vl), BuilderDependencies.getInstance().getDatasourceDependency("db:/example_data/order_lines")));
		}

		@Test
		@DisplayName("an exception while checking is reported as a generic valuelist error with its message")
		void exceptionBecomesGenericError()
		{
			ValueList vl = valuelist(IValueListConstants.DATABASE_VALUES);
			vl.setDataSource(DATASOURCE);
			when(serverManager.getServer("example_data")).thenThrow(new IllegalStateException("boom"));

			assertSingleProblem(check(vl), IMarker.SEVERITY_ERROR, MarkerMessages.ValuelistGenericErrorWithDetails.fill("vl1", "boom").getText());
		}
	}

	@Nested
	@DisplayName("valuelist type and severity preferences")
	class TypeAndSeverity
	{
		@Test
		@DisplayName("an unknown type is an error, the fix makes it a custom valuelist")
		void unknownType()
		{
			ValueList vl = valuelist(42);

			List<Problem> problems = ServoyValuelistBuilder.checkValuelist(vl, fs, serverManager, true);

			assertSingleProblem(problems, IMarker.SEVERITY_ERROR, "Valuelist \"vl1\" has unknown type: 42.");
			assertEquals(IValueListConstants.CUSTOM_VALUES, vl.getValueListType());
		}

		@Test
		@DisplayName("IGNORE in the preferences suppresses the problem")
		void ignoreSuppresses()
		{
			preferences.set(ServoyBuilder.VALUELIST_TYPE_UNKNOWN.getLeft(), "IGNORE");

			assertEquals(List.of(), check(valuelist(42)));
		}

		@Test
		@DisplayName("a WARNING preference lowers the severity of an error problem")
		void preferenceOverridesSeverity()
		{
			preferences.set(ServoyBuilder.VALUELIST_TYPE_UNKNOWN.getLeft(), "WARNING");

			assertEquals(IMarker.SEVERITY_WARNING, check(valuelist(42)).get(0).severity);
		}

		@Test
		@DisplayName("an invalid preference value falls back to the default severity of the problem")
		void invalidPreferenceFallsBack()
		{
			preferences.set(ServoyBuilder.VALUELIST_TYPE_UNKNOWN.getLeft(), "<<<<<<< conflict");

			assertTrue(check(valuelist(42)).stream().allMatch(p -> p.severity == IMarker.SEVERITY_ERROR));
		}
	}
}
