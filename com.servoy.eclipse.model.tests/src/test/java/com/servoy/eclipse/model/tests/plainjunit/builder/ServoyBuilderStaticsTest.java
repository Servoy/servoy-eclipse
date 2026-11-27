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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.servoy.eclipse.model.ModelTestAccess;
import com.servoy.eclipse.model.builder.MarkerMessages;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.builder.ServoyBuilder.Problem;
import com.servoy.eclipse.model.extensions.IDataSourceManager;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.tests.plainjunit.utils.SeverityPreferences;
import com.servoy.eclipse.model.tests.plainjunit.utils.TestPersists;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.UUID;

/**
 * Static helpers of {@link ServoyBuilder} that the persist builders share: severity translation and the sort options check.
 */
class ServoyBuilderStaticsTest
{
	@ParameterizedTest(name = "{0} with default {1} -> {2}")
	@CsvSource({ "WARNING,ERROR,WARNING", "ERROR,WARNING,ERROR", "INFO,ERROR,INFO", "garbage,WARNING,WARNING", "garbage,ERROR,ERROR",
		"garbage,INFO,INFO", "IGNORE,ERROR,ERROR" })
	@DisplayName("getTranslatedSeverity maps the preference to a marker severity, falling back to the default for unknown values")
	void translatedSeverity(String preference, ProblemSeverity def, String expected)
	{
		int markerSeverity = switch (expected)
		{
			case "ERROR" -> IMarker.SEVERITY_ERROR;
			case "WARNING" -> IMarker.SEVERITY_WARNING;
			default -> IMarker.SEVERITY_INFO;
		};
		assertEquals(markerSeverity, ServoyBuilder.getTranslatedSeverity(preference, def));
	}

	@Nested
	@DisplayName("checkSortOptions")
	class CheckSortOptions
	{
		private SeverityPreferences preferences;
		private IDataSourceManager dsm;
		private FlattenedSolution fs;
		private ITable orders;
		private ITable lines;
		private ValueList valuelist;

		@BeforeEach
		void setUp()
		{
			preferences = new SeverityPreferences();
			IServoyModel model = ModelTestAccess.installMockServoyModel();
			dsm = mock(IDataSourceManager.class);
			when(model.getDataSourceManager()).thenReturn(dsm);
			fs = mock(FlattenedSolution.class);
			orders = mock(ITable.class);
			lines = mock(ITable.class);
			when(dsm.getDataSource("db:/d/orders")).thenReturn(orders);
			when(dsm.getDataSource("db:/d/lines")).thenReturn(lines);
			when(orders.getColumn("order_date")).thenReturn(mock(Column.class));
			when(lines.getColumn("qty")).thenReturn(mock(Column.class));
			valuelist = TestPersists.create(mock(ISupportChilds.class), IRepository.VALUELISTS, UUID.randomUUID());
			valuelist.setName("vl1");
		}

		@AfterEach
		void tearDown()
		{
			ModelTestAccess.reset();
			preferences.close();
		}

		private Relation relation(String name, String primary, String foreign)
		{
			Relation relation = TestPersists.relation(UUID.randomUUID());
			relation.setName(name);
			relation.setPrimaryDataSource(primary);
			relation.setForeignDataSource(foreign);
			when(fs.getRelation(name)).thenReturn(relation);
			return relation;
		}

		private List<String> messages(List<Problem> problems)
		{
			return problems.stream().map(p -> p.message).toList();
		}

		@Test
		@DisplayName("null persist or sort options give null")
		void nullInput()
		{
			assertAll(() -> assertNull(ServoyBuilder.checkSortOptions(orders, "a asc", null, fs)),
				() -> assertNull(ServoyBuilder.checkSortOptions(orders, null, valuelist, fs)));
		}

		@Test
		@DisplayName("existing columns, direct and over a matching relation, give no problems")
		void validSortOptions()
		{
			relation("orders_to_lines", "db:/d/orders", "db:/d/lines");

			assertEquals(List.of(), ServoyBuilder.checkSortOptions(orders, "order_date desc, orders_to_lines.qty asc", valuelist, fs));
		}

		@Test
		@DisplayName("a missing or excluded column is reported per sort option, with the element kind and name")
		void missingColumn()
		{
			Column excluded = mock(Column.class);
			ColumnInfo info = mock(ColumnInfo.class);
			when(info.isExcluded()).thenReturn(true);
			when(excluded.getColumnInfo()).thenReturn(info);
			when(orders.getColumn("secret")).thenReturn(excluded);

			List<Problem> problems = ServoyBuilder.checkSortOptions(orders, "nope asc,secret", valuelist, fs);

			assertAll(
				() -> assertEquals(List.of(MarkerMessages.InvalidSortOptionsColumnNotFound.fill("Valuelist", "vl1", "nope asc,secret", "nope").getText(),
					MarkerMessages.InvalidSortOptionsColumnNotFound.fill("Valuelist", "vl1", "nope asc,secret", "secret").getText()), messages(problems)),
				() -> assertEquals(IMarker.SEVERITY_WARNING, problems.get(0).severity));
		}

		@Test
		@DisplayName("an unknown relation in the path is reported and stops the check of that option")
		void relationNotFound()
		{
			Form form = TestPersists.create(mock(ISupportChilds.class), IRepository.FORMS, UUID.randomUUID());
			form.setName("orders_form");

			assertEquals(List.of(MarkerMessages.InvalidSortOptionsRelationNotFound.fill("Form", "orders_form", "missing.qty", "missing").getText()),
				messages(ServoyBuilder.checkSortOptions(orders, "missing.qty", form, fs)));
		}

		@Test
		@DisplayName("a relation whose primary datasource is not the sorted table is reported, the column is checked on its foreign table")
		void relationWithOtherPrimary()
		{
			relation("lines_to_lines", "db:/d/lines", "db:/d/lines");

			// getSeverity(persist) asks the persist for its root object, so it needs a parent
			Relation sorted = TestPersists.create(mock(ISupportChilds.class), IRepository.RELATIONS, UUID.randomUUID());
			sorted.setName("rel1");
			assertEquals(List.of(MarkerMessages.InvalidSortOptionsRelationDifferentPrimaryDatasource
				.fill("Relation", "rel1", "lines_to_lines.qty", "lines_to_lines").getText()),
				messages(ServoyBuilder.checkSortOptions(orders, "lines_to_lines.qty", sorted, fs)));
		}

		@Test
		@DisplayName("a persist that is not a form, relation or valuelist is named Element")
		void otherPersistKind()
		{
			var media = TestPersists.create(mock(ISupportChilds.class), IRepository.MEDIA, UUID.randomUUID());

			assertEquals(List.of(MarkerMessages.InvalidSortOptionsColumnNotFound.fill("Element", null, "nope", "nope").getText()),
				messages(ServoyBuilder.checkSortOptions(orders, "nope", media, fs)));
		}

		@Test
		@DisplayName("IGNORE in the preferences suppresses the sort option problem")
		void ignored()
		{
			preferences.set(ServoyBuilder.INVALID_SORT_OPTIONS_COLUMN_NOT_FOUND.getLeft(), "IGNORE");

			assertEquals(List.of(), ServoyBuilder.checkSortOptions(orders, "nope", valuelist, fs));
		}
	}
}
