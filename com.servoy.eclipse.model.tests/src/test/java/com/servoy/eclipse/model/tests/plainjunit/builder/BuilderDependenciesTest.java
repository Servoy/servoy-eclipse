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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.builder.BuilderDependencies;
import com.servoy.eclipse.model.tests.plainjunit.utils.TestPersists;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.UUID;

/**
 * {@link BuilderDependencies}: the reverse lookups the incremental builder uses (which forms to rebuild when a media, valuelist, relation,
 * scope, datasource or form changes) and how removing a form cleans them up.
 */
class BuilderDependenciesTest
{
	private final BuilderDependencies deps = BuilderDependencies.getInstance();

	private Form form1;
	private Form form2;
	private ValueList valuelist;
	private Relation relation;
	private Media media;

	@BeforeEach
	void setUp()
	{
		deps.clear();
		form1 = TestPersists.create(null, IRepository.FORMS, UUID.randomUUID());
		form2 = TestPersists.create(null, IRepository.FORMS, UUID.randomUUID());
		valuelist = TestPersists.create(null, IRepository.VALUELISTS, UUID.randomUUID());
		relation = TestPersists.relation(UUID.randomUUID());
		media = TestPersists.create(null, IRepository.MEDIA, UUID.randomUUID());
	}

	@AfterEach
	void tearDown()
	{
		deps.clear();
	}

	@Test
	@DisplayName("a cleared instance knows nothing and is not initialized")
	void clearedIsEmpty()
	{
		assertAll(() -> assertFalse(deps.isInitialized()), () -> assertNull(deps.getValuelistDependencies(valuelist)),
			() -> assertNull(deps.getMediaDependencies(media)), () -> assertNull(deps.getRelationDependencies(relation)),
			() -> assertNull(deps.getFormDependencies(form1)), () -> assertNull(deps.getScopeDependency("globals")),
			() -> assertNull(deps.getDatasourceDependency("db:/a/b")), () -> assertNull(deps.getVariantDependencies()));
	}

	@Test
	@DisplayName("initialize marks the instance initialized, clear resets it")
	void initializeAndClear()
	{
		deps.initialize();
		assertTrue(deps.isInitialized());
		deps.clear();
		assertFalse(deps.isInitialized());
	}

	@Test
	@DisplayName("each dependency kind is looked up from the dependency to the dependents, without duplicates")
	void reverseLookups()
	{
		deps.addDependency(form1, valuelist);
		deps.addDependency(form1, valuelist);
		deps.addDependency(form2, valuelist);
		deps.addDependency(form1, media);
		deps.addDependency(form1, relation);
		deps.addDependency(valuelist, relation);
		deps.addDependency(form1, form2);
		deps.addDependency("globals", form1);
		deps.addDependency("globals", form1);
		deps.addDatasourceDependency("db:/a/b", form1);
		deps.addVariantDependency(form1);
		deps.addVariantDependency(form1);

		assertAll(() -> assertEquals(List.of(form1, form2), deps.getValuelistDependencies(valuelist)),
			() -> assertEquals(List.of(form1), deps.getMediaDependencies(media)),
			() -> assertEquals(List.of(form1, valuelist), deps.getRelationDependencies(relation)),
			() -> assertEquals(List.of(form1), deps.getFormDependencies(form2), "form2 is used by form1"),
			() -> assertNull(deps.getFormDependencies(form1), "nothing uses form1"),
			() -> assertEquals(List.of(form1), deps.getScopeDependency("globals")),
			() -> assertEquals(List.of(form1), deps.getDatasourceDependency("db:/a/b")),
			() -> assertEquals(List.of(form1), deps.getVariantDependencies()));
	}

	@Test
	@DisplayName("null scope or datasource names are not recorded")
	void nullNamesIgnored()
	{
		deps.addDependency((String)null, form1);
		deps.addDatasourceDependency(null, form1);

		assertAll(() -> assertNull(deps.getScopeDependency(null)), () -> assertNull(deps.getDatasourceDependency(null)));
	}

	@Test
	@DisplayName("removeForm drops the form from every dependency list; emptied scope and datasource entries are removed")
	void removeForm()
	{
		deps.initialize();
		deps.addDependency(form1, valuelist);
		deps.addDependency(form2, valuelist);
		deps.addDependency(form1, media);
		deps.addDependency(form1, relation);
		deps.addDependency(form1, form2);
		deps.addDependency("globals", form1);
		deps.addDependency("scope2", form1);
		deps.addDependency("scope2", form2);
		deps.addDatasourceDependency("db:/a/b", form1);
		deps.addVariantDependency(form1);

		deps.removeForm(form1);

		assertAll(() -> assertEquals(List.of(form2), deps.getValuelistDependencies(valuelist)),
			() -> assertEquals(List.of(), deps.getMediaDependencies(media)), () -> assertEquals(List.of(), deps.getRelationDependencies(relation)),
			() -> assertEquals(List.of(), deps.getFormDependencies(form2)), () -> assertNull(deps.getScopeDependency("globals")),
			() -> assertEquals(List.of(form2), deps.getScopeDependency("scope2")), () -> assertNull(deps.getDatasourceDependency("db:/a/b")),
			() -> assertEquals(List.of(), deps.getVariantDependencies()));
	}

	@Test
	@DisplayName("removing a form that has no dependencies is a no-op")
	void removeUnknownForm()
	{
		deps.addDependency(form2, valuelist);

		deps.removeForm(form1);

		assertEquals(List.of(form2), deps.getValuelistDependencies(valuelist));
	}

	@Test
	@DisplayName("removeScopeDependencies and removeDatasourceDependencies drop one entry")
	void removeScopeAndDatasource()
	{
		deps.addDependency("globals", form1);
		deps.addDependency("other", form1);
		deps.addDatasourceDependency("db:/a/b", form1);
		deps.addDatasourceDependency("db:/a/c", form1);

		deps.removeScopeDependencies("globals");
		deps.removeDatasourceDependencies("db:/a/b");

		assertAll(() -> assertNull(deps.getScopeDependency("globals")), () -> assertEquals(List.of(form1), deps.getScopeDependency("other")),
			() -> assertNull(deps.getDatasourceDependency("db:/a/b")), () -> assertEquals(List.of(form1), deps.getDatasourceDependency("db:/a/c")));
	}
}
