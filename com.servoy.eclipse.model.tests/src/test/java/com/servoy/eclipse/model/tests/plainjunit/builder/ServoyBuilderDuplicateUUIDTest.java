package com.servoy.eclipse.model.tests.plainjunit.builder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.util.UUID;

/**
 * SVY-21431 - ServoyBuilder.isDuplicateConfirmedInMemory decides, without any
 * sibling-file reads, whether a persist flagged POSSIBLE_DUPLICATE_UUID is
 * really a duplicate: the in-memory duplicate index, or the "other file"
 * recorded by the deserializer, which must still exist and must not be the
 * persist's own file.
 */
class ServoyBuilderDuplicateUUIDTest {
	// only absolute paths are compared, no file is created
	private final File dir = new File(System.getProperty("java.io.tmpdir"), "svy21431");
	private final String ownFile = new File(dir, "orig.frm").getAbsolutePath();
	private final String copyFile = new File(dir, "copy.frm").getAbsolutePath();

	private static Relation relation(UUID uuid) throws Exception {
		Constructor<Relation> ctor = Relation.class.getDeclaredConstructor(ISupportChilds.class, UUID.class);
		ctor.setAccessible(true);
		return ctor.newInstance(null, uuid);
	}

	private static Map<UUID, List<IPersist>> index(UUID uuid, IPersist... persists) {
		Map<UUID, List<IPersist>> map = new HashMap<>();
		List<IPersist> list = new ArrayList<>();
		for (IPersist p : persists)
			list.add(p);
		map.put(uuid, list);
		return map;
	}

	@Test
	void twoDistinctPersistsInIndexAreConfirmedWithoutLookingAtFiles() throws Exception {
		UUID uuid = UUID.randomUUID();
		Relation a = relation(uuid);
		Relation b = relation(uuid);
		assertTrue(ServoyBuilder.isDuplicateConfirmedInMemory(a, index(uuid, a, b), ownFile, path -> {
			throw new AssertionError("must not consult files when the index already confirms the duplicate");
		}));
	}

	@Test
	void indexListingOnlyThePersistItselfIsNotADuplicate() throws Exception {
		UUID uuid = UUID.randomUUID();
		Relation a = relation(uuid);
		assertFalse(ServoyBuilder.isDuplicateConfirmedInMemory(a, index(uuid, a), ownFile, path -> false));
	}

	@Test
	void recordedCopyThatStillExistsIsConfirmed() throws Exception {
		Relation a = relation(UUID.randomUUID());
		a.setRuntimeProperty(SolutionDeserializer.DUPLICATE_UUID_OTHER_FILE, copyFile);
		assertTrue(ServoyBuilder.isDuplicateConfirmedInMemory(a, null, ownFile, path -> path.equals(copyFile)));
	}

	@Test
	void recordedCopyThatWasDeletedIsNotConfirmedAnymore() throws Exception {
		Relation a = relation(UUID.randomUUID());
		a.setRuntimeProperty(SolutionDeserializer.DUPLICATE_UUID_OTHER_FILE, copyFile);
		assertFalse(ServoyBuilder.isDuplicateConfirmedInMemory(a, null, ownFile, path -> false));
	}

	@Test
	void recordedFileThatIsThePersistsOwnFileIsNotADuplicate() throws Exception {
		// review R1: after an external rename the "other" file is the persist's own new
		// file; the old scan skipped it too
		Relation a = relation(UUID.randomUUID());
		a.setRuntimeProperty(SolutionDeserializer.DUPLICATE_UUID_OTHER_FILE, ownFile);
		assertFalse(ServoyBuilder.isDuplicateConfirmedInMemory(a, null, ownFile, path -> true));
	}

	@Test
	void noIndexEntryAndNoOtherFileIsNotADuplicate() throws Exception {
		Relation a = relation(UUID.randomUUID());
		assertFalse(ServoyBuilder.isDuplicateConfirmedInMemory(a, new HashMap<>(), ownFile, path -> true));
	}
}
