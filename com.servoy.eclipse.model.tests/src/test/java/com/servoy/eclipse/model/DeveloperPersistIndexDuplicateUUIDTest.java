/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.eclipse.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.UUID;

/**
 * SVY-21431 - the improved ServoyBuilder.checkDuplicateUUID no longer scans
 * sibling files from disk; it relies on the in-memory duplicate-UUID index
 * maintained by DeveloperPersistIndex (exposed via getDuplicateUUIDList). These
 * tests pin the contract that method depends on:
 *
 * <ul>
 * <li>two different persists that share a UUID are recorded as a duplicate
 * group (&gt;= 2 entries),
 * <li>a persist whose UUID is unique is NOT reported,
 * <li>re-caching the same persist instance does not fabricate a duplicate,
 * <li>the returned map is keyed by the shared UUID.
 * </ul>
 *
 * These guard the data source the new implementation reads from - if this
 * contract breaks, the builder's duplicate-UUID marker silently disappears.
 */
class DeveloperPersistIndexDuplicateUUIDTest {

	private static DeveloperPersistIndex newEmptyIndex() {
		// an index over no solutions: createDatasources() is a no-op, so we can drive
		// putInCache directly
		return new DeveloperPersistIndex(Collections.<Solution>emptyList());
	}

	private static void putInCache(DeveloperPersistIndex index, IPersist persist) throws Exception {
		Method m = com.servoy.j2db.PersistIndex.class.getDeclaredMethod("putInCache", IPersist.class);
		m.setAccessible(true);
		m.invoke(index, persist);
	}

	private static Relation relationWithUUID(UUID uuid) throws Exception {
		// parent null, not a clone: the putInCache clone-guards pass and the persist
		// carries the given
		// UUID. The Relation(ISupportChilds, UUID) constructor is package-private, so
		// build it reflectively.
		Constructor<Relation> ctor = Relation.class.getDeclaredConstructor(ISupportChilds.class, UUID.class);
		ctor.setAccessible(true);
		return ctor.newInstance(null, uuid);
	}

	@Test
	void sameUUIDOnTwoDifferentPersistsIsReportedAsDuplicate() throws Exception {
		DeveloperPersistIndex index = newEmptyIndex();
		UUID shared = UUID.randomUUID();
		Relation first = relationWithUUID(shared);
		Relation second = relationWithUUID(shared);

		putInCache(index, first);
		putInCache(index, second);

		Map<UUID, List<IPersist>> duplicates = index.getDuplicateUUIDList();
		assertNotNull(duplicates, "duplicate map must not be null");
		List<IPersist> group = duplicates.get(shared);
		assertAll(() -> assertNotNull(group, "the shared UUID must have a duplicate group"),
				() -> assertTrue(group.size() >= 2, "duplicate group must contain both colliding persists"),
				() -> assertTrue(group.contains(first) && group.contains(second),
						"duplicate group must contain both persists that share the UUID"));
	}

	@Test
	void uniqueUUIDIsNotReportedAsDuplicate() throws Exception {
		DeveloperPersistIndex index = newEmptyIndex();
		UUID unique = UUID.randomUUID();
		Relation only = relationWithUUID(unique);

		putInCache(index, only);

		Map<UUID, List<IPersist>> duplicates = index.getDuplicateUUIDList();
		assertNotNull(duplicates);
		assertNull(duplicates.get(unique), "a persist with a unique UUID must not appear in the duplicate map");
	}

	@Test
	void reCachingSameInstanceDoesNotCreateDuplicate() throws Exception {
		DeveloperPersistIndex index = newEmptyIndex();
		UUID uuid = UUID.randomUUID();
		Relation persist = relationWithUUID(uuid);

		putInCache(index, persist);
		putInCache(index, persist); // same instance again - must not be treated as a collision

		Map<UUID, List<IPersist>> duplicates = index.getDuplicateUUIDList();
		assertNull(duplicates.get(uuid), "re-caching the same persist instance must not fabricate a duplicate");
	}

	@Test
	void duplicateMapIsKeyedBySharedUUID() throws Exception {
		DeveloperPersistIndex index = newEmptyIndex();
		UUID shared = UUID.randomUUID();
		UUID other = UUID.randomUUID();
		Relation a = relationWithUUID(shared);
		Relation b = relationWithUUID(shared);
		Relation c = relationWithUUID(other);

		putInCache(index, a);
		putInCache(index, b);
		putInCache(index, c);

		Map<UUID, List<IPersist>> duplicates = index.getDuplicateUUIDList();
		assertAll(() -> assertTrue(duplicates.containsKey(shared), "shared UUID must be a key in the duplicate map"),
				() -> assertFalse(duplicates.containsKey(other), "the unique UUID must not be a key"),
				() -> assertEquals(shared, duplicates.get(shared).get(0).getUUID(),
						"entries under the shared key must carry that UUID"));
	}

	/**
	 * SVY-21431 - the single-reused-instance set-sites (SolutionDeserializer Site
	 * 1/Site 3) keep only one instance for the UUID, so the in-memory duplicate
	 * index does NOT see them. Deserialization instead records the other file name
	 * in DUPLICATE_UUID_OTHER_FILE, and checkDuplicateUUID confirms the duplicate
	 * from that flag - still without any sibling-file reads. These tests pin that
	 * flag contract.
	 */
	@Nested
	class OtherFileFlag {
		@Test
		void otherFileFlagIsReadableAndClearable() throws Exception {
			Relation persist = relationWithUUID(UUID.randomUUID());
			// deserialization set-site behaviour: record the conflicting file name
			persist.setRuntimeProperty(SolutionDeserializer.DUPLICATE_UUID_OTHER_FILE, "formA.frm");
			assertEquals("formA.frm", persist.getRuntimeProperty(SolutionDeserializer.DUPLICATE_UUID_OTHER_FILE),
					"the recorded other-file name must be readable by the builder");

			// builder clears both flags when nothing is found on a later clean build
			persist.setRuntimeProperty(SolutionDeserializer.DUPLICATE_UUID_OTHER_FILE, null);
			assertNull(persist.getRuntimeProperty(SolutionDeserializer.DUPLICATE_UUID_OTHER_FILE),
					"clearing the other-file flag must stick");
		}

		@Test
		void unsetOtherFileFlagIsNull() throws Exception {
			Relation persist = relationWithUUID(UUID.randomUUID());
			assertNull(persist.getRuntimeProperty(SolutionDeserializer.DUPLICATE_UUID_OTHER_FILE),
					"a persist with no recorded conflict must report null (builder treats null as 'no duplicate')");
		}
	}
}
