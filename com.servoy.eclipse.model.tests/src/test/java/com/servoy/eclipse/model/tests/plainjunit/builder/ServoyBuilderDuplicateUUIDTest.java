package com.servoy.eclipse.model.tests.plainjunit.builder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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
 * really a duplicate (in-memory index, or the other file recorded by the
 * deserializer). The helper is package-private, so it is invoked reflectively.
 */
class ServoyBuilderDuplicateUUIDTest {
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

	private static boolean confirmed(IPersist persist, Map<UUID, List<IPersist>> indexDuplicates,
			Predicate<String> otherFileExists) throws Exception {
		Method m = ServoyBuilder.class.getDeclaredMethod("isDuplicateConfirmedInMemory", IPersist.class, Map.class,
				Predicate.class);
		m.setAccessible(true);
		try {
			return ((Boolean) m.invoke(null, persist, indexDuplicates, otherFileExists)).booleanValue();
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof Error error)
				throw error;
			throw e;
		}
	}

	@Test
	void twoDistinctPersistsInIndexAreConfirmedWithoutLookingAtFiles() throws Exception {
		UUID uuid = UUID.randomUUID();
		Relation a = relation(uuid);
		Relation b = relation(uuid);
		assertTrue(confirmed(a, index(uuid, a, b), path -> {
			throw new AssertionError("must not consult files when the index already confirms the duplicate");
		}));
	}

	@Test
	void indexListingOnlyThePersistItselfIsNotADuplicate() throws Exception {
		UUID uuid = UUID.randomUUID();
		Relation a = relation(uuid);
		assertFalse(confirmed(a, index(uuid, a), path -> false));
	}

	@Test
	void otherFileRecordedAndStillExistingIsConfirmed() throws Exception {
		Relation a = relation(UUID.randomUUID());
		a.setRuntimeProperty(SolutionDeserializer.DUPLICATE_UUID_OTHER_FILE, "/ws/sol/forms/copy.frm");
		assertTrue(confirmed(a, null, path -> path.equals("/ws/sol/forms/copy.frm")));
	}

	@Test
	void otherFileRecordedButDeletedIsNotConfirmedAnymore() throws Exception {
		// the clear-after-fix path: the copy was deleted/renamed, so the stale flag
		// must not keep the marker alive
		Relation a = relation(UUID.randomUUID());
		a.setRuntimeProperty(SolutionDeserializer.DUPLICATE_UUID_OTHER_FILE, "/ws/sol/forms/copy.frm");
		assertFalse(confirmed(a, null, path -> false));
	}

	@Test
	void noIndexEntryAndNoOtherFileIsNotADuplicate() throws Exception {
		Relation a = relation(UUID.randomUUID());
		assertFalse(confirmed(a, new HashMap<>(), path -> true));
	}
}
