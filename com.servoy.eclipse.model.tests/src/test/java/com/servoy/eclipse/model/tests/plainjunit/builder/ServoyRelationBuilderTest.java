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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.servoy.eclipse.model.ModelTestAccess;
import com.servoy.eclipse.model.builder.BuilderDependencies;
import com.servoy.eclipse.model.builder.MarkerMessages;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.builder.ServoyRelationBuilder;
import com.servoy.eclipse.model.extensions.IDataSourceManager;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.tests.plainjunit.utils.SeverityPreferences;
import com.servoy.eclipse.model.tests.plainjunit.utils.TestPersists;
import com.servoy.eclipse.model.tests.plainjunit.utils.WorkspaceMarkers;
import com.servoy.eclipse.model.tests.plainjunit.utils.WorkspaceMarkers.RecordedMarker;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.util.UUID;

/**
 * {@link ServoyRelationBuilder#checkRelation(Relation)}: the problem markers created for each kind of broken relation, the valid flag and
 * the datasource dependencies. The workspace is {@link WorkspaceMarkers}, severities come from {@link SeverityPreferences}, the data source
 * manager and flattened solution are mocks.
 */
class ServoyRelationBuilderTest
{
	private static final String ORDERS = "db:/example_data/orders";
	private static final String LINES = "db:/example_data/order_lines";

	private SeverityPreferences preferences;
	private WorkspaceMarkers workspace;
	private IDataSourceManager dsm;
	private FlattenedSolution fs;
	private ISupportChilds parent;

	@BeforeEach
	void setUp()
	{
		preferences = new SeverityPreferences();
		workspace = new WorkspaceMarkers();
		IServoyModel model = ModelTestAccess.installMockServoyModel();
		dsm = mock(IDataSourceManager.class);
		fs = mock(FlattenedSolution.class);
		when(model.getDataSourceManager()).thenReturn(dsm);
		when(model.getFlattenedSolution()).thenReturn(fs);
		ServoyProject activeProject = mock(ServoyProject.class);
		when(activeProject.getProject()).thenReturn(mock(IProject.class));
		when(model.getActiveProject()).thenReturn(activeProject);
		parent = mock(ISupportChilds.class);
		when(parent.getRootObject()).thenReturn(mock(IRootObject.class));
		BuilderDependencies.getInstance().clear();
	}

	@AfterEach
	void tearDown()
	{
		BuilderDependencies.getInstance().clear();
		ModelTestAccess.reset();
		workspace.close();
		preferences.close();
	}

	private Relation relation(String primary, String foreign)
	{
		Relation relation = TestPersists.create(parent, IRepository.RELATIONS, UUID.randomUUID());
		relation.setName("orders_to_lines");
		relation.setPrimaryDataSource(primary);
		relation.setForeignDataSource(foreign);
		return relation;
	}

	private RelationItem item(Relation relation, String primary, String foreign)
	{
		RelationItem item = TestPersists.create(relation, IRepository.RELATION_ITEMS, UUID.randomUUID());
		item.setPrimaryDataProviderID(primary);
		item.setForeignColumnName(foreign);
		relation.addChild(item);
		return item;
	}

	private IServerInternal server(String datasource, String name)
	{
		IServerInternal server = mock(IServerInternal.class);
		when(server.getName()).thenReturn(name);
		when(dsm.getServer(datasource)).thenReturn(server);
		return server;
	}

	private ITable table(IServerInternal server, String name, String datasource) throws Exception
	{
		ITable table = mock(ITable.class);
		when(table.getName()).thenReturn(name);
		when(table.getDataSource()).thenReturn(datasource);
		when(table.getRowIdentColumnsCount()).thenReturn(1);
		when(server.getTable(name)).thenReturn(table);
		return table;
	}

	/** Both servers and tables exist, with a primary key. */
	private ITable[] validTables() throws Exception
	{
		IServerInternal server = server(ORDERS, "example_data");
		when(dsm.getServer(LINES)).thenReturn(server);
		return new ITable[] { table(server, "orders", ORDERS), table(server, "order_lines", LINES) };
	}

	private RecordedMarker single()
	{
		assertEquals(1, workspace.markers().size(), () -> "expected exactly one marker, got " + workspace.messages());
		return workspace.markers().get(0);
	}

	private void assertSingleMarker(String message, int severity)
	{
		RecordedMarker marker = single();
		assertAll(() -> assertEquals(ServoyBuilder.PROJECT_RELATION_MARKER_TYPE, marker.type()), () -> assertEquals(message, marker.message()),
			() -> assertEquals(Integer.valueOf(severity), marker.severity()), () -> assertEquals("/relations/orders_to_lines.rel", marker.path()));
	}

	@Test
	@DisplayName("an unknown (non db/mem/view) datasource is skipped: no markers, no dependencies, valid flag untouched")
	void unknownDatasourceIsSkipped()
	{
		Relation relation = relation("foo:/bar", LINES);
		relation.setValid(false);

		ServoyRelationBuilder.checkRelation(relation);

		assertAll(() -> assertEquals(List.of(), workspace.markers()), () -> assertEquals(Boolean.FALSE, relation.valid),
			() -> assertNull(BuilderDependencies.getInstance().getDatasourceDependency(LINES)));
	}

	@Test
	@DisplayName("a missing primary server makes the relation invalid with an error marker; both datasources become dependencies")
	void primaryServerMissing()
	{
		Relation relation = relation(ORDERS, LINES);

		ServoyRelationBuilder.checkRelation(relation);

		assertSingleMarker(MarkerMessages.RelationPrimaryServerWithProblems.fill("orders_to_lines", "example_data").getText(), IMarker.SEVERITY_ERROR);
		assertAll(() -> assertEquals(Boolean.FALSE, relation.valid),
			() -> assertEquals(List.of(relation), BuilderDependencies.getInstance().getDatasourceDependency(ORDERS)),
			() -> assertEquals(List.of(relation), BuilderDependencies.getInstance().getDatasourceDependency(LINES)));
	}

	@Test
	@DisplayName("a missing primary table is an error and makes the relation invalid")
	void primaryTableMissing()
	{
		Relation relation = relation(ORDERS, LINES);
		server(ORDERS, "example_data");

		ServoyRelationBuilder.checkRelation(relation);

		assertSingleMarker(MarkerMessages.RelationPrimaryTableNotFound.fill("orders_to_lines", "orders", "example_data").getText(),
			IMarker.SEVERITY_ERROR);
		assertEquals(Boolean.FALSE, relation.valid);
	}

	@Test
	@DisplayName("a primary table without primary key is an error")
	void primaryTableWithoutPk() throws Exception
	{
		Relation relation = relation(ORDERS, LINES);
		ITable table = table(server(ORDERS, "example_data"), "orders", ORDERS);
		when(table.getRowIdentColumnsCount()).thenReturn(0);

		ServoyRelationBuilder.checkRelation(relation);

		assertSingleMarker(MarkerMessages.RelationPrimaryTableWithoutPK.fill("orders_to_lines", "orders", "example_data").getText(),
			IMarker.SEVERITY_ERROR);
		assertEquals(Boolean.FALSE, relation.valid);
	}

	@Test
	@DisplayName("a missing foreign server is an error")
	void foreignServerMissing() throws Exception
	{
		Relation relation = relation(ORDERS, LINES);
		IServerInternal server = mock(IServerInternal.class);
		when(server.getName()).thenReturn("example_data");
		when(dsm.getServer(ORDERS)).thenReturn(server);
		table(server, "orders", ORDERS);

		ServoyRelationBuilder.checkRelation(relation);

		assertSingleMarker(MarkerMessages.RelationForeignServerWithProblems.fill("orders_to_lines", "example_data").getText(),
			IMarker.SEVERITY_ERROR);
		assertEquals(Boolean.FALSE, relation.valid);
	}

	@Test
	@DisplayName("a missing foreign table is an error")
	void foreignTableMissing() throws Exception
	{
		Relation relation = relation(ORDERS, LINES);
		IServerInternal server = server(ORDERS, "example_data");
		when(dsm.getServer(LINES)).thenReturn(server);
		table(server, "orders", ORDERS);

		ServoyRelationBuilder.checkRelation(relation);

		assertSingleMarker(MarkerMessages.RelationForeignTableNotFound.fill("orders_to_lines", "order_lines", "example_data").getText(),
			IMarker.SEVERITY_ERROR);
	}

	@Test
	@DisplayName("a foreign table without primary key is an error")
	void foreignTableWithoutPk() throws Exception
	{
		Relation relation = relation(ORDERS, LINES);
		when(validTables()[1].getRowIdentColumnsCount()).thenReturn(0);

		ServoyRelationBuilder.checkRelation(relation);

		assertSingleMarker(MarkerMessages.RelationForeignTableWithoutPK.fill("orders_to_lines", "order_lines", "example_data").getText(),
			IMarker.SEVERITY_ERROR);
	}

	@Test
	@DisplayName("a relation without items between different tables is empty (error)")
	void emptyRelation() throws Exception
	{
		Relation relation = relation(ORDERS, LINES);
		validTables();

		ServoyRelationBuilder.checkRelation(relation);

		assertSingleMarker(MarkerMessages.RelationEmpty.fill("orders_to_lines").getText(), IMarker.SEVERITY_ERROR);
		assertEquals(Boolean.FALSE, relation.valid);
	}

	@Test
	@DisplayName("servers answering under another name give a duplicate server warning for primary and foreign")
	void duplicateServers() throws Exception
	{
		Relation relation = relation(ORDERS, LINES);
		IServerInternal server = server(ORDERS, "Example_Data");
		when(dsm.getServer(LINES)).thenReturn(server);
		table(server, "orders", ORDERS);
		table(server, "order_lines", LINES);

		ServoyRelationBuilder.checkRelation(relation);

		assertEquals(List.of(MarkerMessages.RelationPrimaryServerDuplicate.fill("orders_to_lines", "example_data").getText(),
			MarkerMessages.RelationForeignServerDuplicate.fill("orders_to_lines", "example_data").getText(),
			MarkerMessages.RelationEmpty.fill("orders_to_lines").getText()), workspace.messages());
		assertEquals(Integer.valueOf(IMarker.SEVERITY_WARNING), workspace.markers().get(0).severity());
	}

	@Test
	@DisplayName("a hidden primary table is reported once with low priority, the hidden foreign table is then not reported again")
	void hiddenTables() throws Exception
	{
		Relation relation = relation(ORDERS, LINES);
		ITable[] tables = validTables();
		when(tables[0].isMarkedAsHiddenInDeveloper()).thenReturn(true);
		when(tables[1].isMarkedAsHiddenInDeveloper()).thenReturn(true);

		ServoyRelationBuilder.checkRelation(relation);

		RecordedMarker hidden = workspace.markers().get(0);
		assertAll(() -> assertEquals(List.of(MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(ORDERS, "relation ", "orders_to_lines").getText(),
			MarkerMessages.RelationEmpty.fill("orders_to_lines").getText()), workspace.messages()),
			() -> assertEquals(Integer.valueOf(IMarker.PRIORITY_LOW), hidden.priority()),
			() -> assertEquals(Integer.valueOf(IMarker.SEVERITY_WARNING), hidden.severity()));
	}

	@Test
	@DisplayName("a hidden foreign table alone is reported")
	void hiddenForeignTable() throws Exception
	{
		Relation relation = relation(ORDERS, LINES);
		when(validTables()[1].isMarkedAsHiddenInDeveloper()).thenReturn(true);

		ServoyRelationBuilder.checkRelation(relation);

		assertEquals(MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(LINES, "relation ", "orders_to_lines").getText(), workspace.messages().get(0));
	}

	@Test
	@DisplayName("items without primary or foreign dataprovider, or with unknown ones, are errors and make the relation invalid")
	void brokenItems() throws Exception
	{
		Relation relation = relation(ORDERS, LINES);
		validTables();
		item(relation, null, "order_id");
		item(relation, "order_id", "");
		item(relation, "missing_pk", "missing_fk");
		when(fs.getDataProviderForTable(Mockito.any(ITable.class), Mockito.eq("order_id"))).thenReturn(mock(Column.class));

		ServoyRelationBuilder.checkRelation(relation);

		assertAll(() -> assertEquals(List.of(MarkerMessages.RelationItemNoPrimaryDataprovider.fill("orders_to_lines").getText(),
			MarkerMessages.RelationItemNoForeignDataprovider.fill("orders_to_lines").getText(),
			MarkerMessages.RelationItemPrimaryDataproviderNotFound.fill("orders_to_lines", "missing_pk").getText(),
			MarkerMessages.RelationItemForeignDataproviderNotFound.fill("orders_to_lines", "missing_fk").getText()), workspace.messages()),
			() -> assertEquals(Boolean.FALSE, relation.valid));
	}

	@Test
	@DisplayName("a literal primary dataprovider is not looked up")
	void literalPrimaryIsNotLookedUp() throws Exception
	{
		Relation relation = relation(ORDERS, LINES);
		validTables();
		item(relation, "LITERAL:'x'", "missing_fk");

		ServoyRelationBuilder.checkRelation(relation);

		assertEquals(List.of(MarkerMessages.RelationItemForeignDataproviderNotFound.fill("orders_to_lines", "missing_fk").getText()),
			workspace.messages());
	}

	@Test
	@DisplayName("an exception while checking becomes a generic relation error with its message and makes the relation invalid")
	void exceptionBecomesGenericError() throws Exception
	{
		Relation relation = relation(ORDERS, LINES);
		IServerInternal server = server(ORDERS, "example_data");
		when(server.getTable("orders")).thenThrow(new IllegalStateException("boom"));

		ServoyRelationBuilder.checkRelation(relation);

		assertSingleMarker(MarkerMessages.RelationGenericErrorWithDetails.fill("orders_to_lines", "boom").getText(), IMarker.SEVERITY_ERROR);
		assertEquals(Boolean.FALSE, relation.valid);
	}

	@Test
	@DisplayName("IGNORE in the preferences suppresses the marker, the relation still becomes invalid")
	void ignoreSuppressesMarker()
	{
		preferences.set(ServoyBuilder.RELATION_PRIMARY_SERVER_WITH_PROBLEMS.getLeft(), "IGNORE");
		Relation relation = relation(ORDERS, LINES);

		ServoyRelationBuilder.checkRelation(relation);

		assertAll(() -> assertEquals(List.of(), workspace.markers()), () -> assertEquals(Boolean.FALSE, relation.valid));
	}

	@Test
	@DisplayName("a view primary datasource is checked under the view server name, with its own table name, also when the foreign datasource is a db one")
	void viewPrimaryWithDbForeign()
	{
		Relation relation = relation("view:orders_view", LINES);
		server("view:orders_view", "view");

		ServoyRelationBuilder.checkRelation(relation);

		assertSingleMarker(MarkerMessages.RelationPrimaryTableNotFound.fill("orders_to_lines", "orders_view", "view").getText(), IMarker.SEVERITY_ERROR);
	}

	@Test
	@DisplayName("a view primary datasource with a view foreign datasource looks up the primary view table, not null")
	void viewPrimaryWithViewForeign() throws Exception
	{
		Relation relation = relation("view:orders_view", "view:lines_view");
		IServerInternal server = server("view:orders_view", "view");
		when(dsm.getServer("view:lines_view")).thenReturn(server);
		table(server, "orders_view", "view:orders_view");
		table(server, "lines_view", "view:lines_view");

		ServoyRelationBuilder.checkRelation(relation);

		assertSingleMarker(MarkerMessages.RelationEmpty.fill("orders_to_lines").getText(), IMarker.SEVERITY_ERROR);
	}

	@Test
	@DisplayName("an in-memory primary datasource is checked under the mem server name")
	void inmemPrimary()
	{
		Relation relation = relation("mem:orders", LINES);

		ServoyRelationBuilder.checkRelation(relation);

		assertTrue(single().message().contains("\"mem\""), () -> single().message());
	}
}
