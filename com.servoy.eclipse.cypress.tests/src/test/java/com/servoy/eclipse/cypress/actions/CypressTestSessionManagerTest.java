package com.servoy.eclipse.cypress.actions;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.cypress.actions.CypressTestResult.TestStatus;
import com.servoy.eclipse.cypress.actions.CypressTestResult.TestType;

@DisplayName("CypressTestSessionManager")
class CypressTestSessionManagerTest {

	private CypressTestSessionManager manager;

	@BeforeEach
	void setUp() {
		manager = CypressTestSessionManager.getInstance();
		manager.clear();
	}

	@Nested
	@DisplayName("singleton")
	class Singleton {

		@Test
		@DisplayName("getInstance returns non-null")
		void returnsNonNull() {
			assertNotNull(CypressTestSessionManager.getInstance());
		}

		@Test
		@DisplayName("getInstance always returns same instance")
		void returnsSameInstance() {
			assertSame(CypressTestSessionManager.getInstance(), CypressTestSessionManager.getInstance());
		}
	}

	@Nested
	@DisplayName("startSession")
	class StartSession {

		@Test
		@DisplayName("initializes all tests as PENDING")
		void initializesAsPending() {
			manager.startSession(List.of("formA", "formB", "formC"), TestType.FORM);

			List<CypressTestResult> results = manager.getResults();
			assertAll(() -> assertEquals(3, results.size()),
					() -> assertEquals(TestStatus.PENDING, results.get(0).status()),
					() -> assertEquals(TestStatus.PENDING, results.get(1).status()),
					() -> assertEquals(TestStatus.PENDING, results.get(2).status()));
		}

		@Test
		@DisplayName("preserves test name order")
		void preservesOrder() {
			manager.startSession(List.of("alpha", "beta", "gamma"), TestType.E2E);

			List<CypressTestResult> results = manager.getResults();
			assertAll(() -> assertEquals("alpha", results.get(0).testName()),
					() -> assertEquals("beta", results.get(1).testName()),
					() -> assertEquals("gamma", results.get(2).testName()));
		}

		@Test
		@DisplayName("sets correct test type on all results")
		void setsTestType() {
			manager.startSession(List.of("test1", "test2"), TestType.E2E);

			List<CypressTestResult> results = manager.getResults();
			assertAll(() -> assertEquals(TestType.E2E, results.get(0).testType()),
					() -> assertEquals(TestType.E2E, results.get(1).testType()));
		}

		@Test
		@DisplayName("sets running flag to true")
		void setsRunningTrue() {
			manager.startSession(List.of("test1"), TestType.FORM);

			assertTrue(manager.isRunning());
		}

		@Test
		@DisplayName("clears previous session results")
		void clearsPreviousResults() {
			manager.startSession(List.of("old1", "old2"), TestType.FORM);
			manager.startSession(List.of("new1"), TestType.E2E);

			List<CypressTestResult> results = manager.getResults();
			assertAll(() -> assertEquals(1, results.size()), () -> assertEquals("new1", results.get(0).testName()));
		}

		@Test
		@DisplayName("fires SESSION_STARTED property change event")
		void firesSessionStartedEvent() {
			List<PropertyChangeEvent> events = new ArrayList<>();
			manager.addPropertyChangeListener(events::add);

			manager.startSession(List.of("test1"), TestType.FORM);

			assertEquals(1, events.size());
			assertEquals(CypressTestSessionManager.PROP_SESSION_STARTED, events.get(0).getPropertyName());

			manager.removePropertyChangeListener(events::add);
		}

		@Test
		@DisplayName("handles empty test list")
		void handlesEmptyList() {
			manager.startSession(List.of(), TestType.FORM);

			assertAll(() -> assertEquals(0, manager.getResults().size()), () -> assertTrue(manager.isRunning()));
		}
	}

	@Nested
	@DisplayName("updateResult")
	class UpdateResult {

		@Test
		@DisplayName("updates status for a specific test")
		void updatesStatus() {
			manager.startSession(List.of("formA", "formB"), TestType.FORM);

			manager.updateResult("formA",
					new CypressTestResult("formA", TestType.FORM, TestStatus.PASSED, null, "All tests passed", 1200));

			CypressTestResult result = manager.getResults().get(0);
			assertAll(() -> assertEquals(TestStatus.PASSED, result.status()),
					() -> assertEquals(1200, result.durationMs()),
					() -> assertEquals("All tests passed", result.rawOutput()));
		}

		@Test
		@DisplayName("sets running=false when all tests completed")
		void setsRunningFalseWhenAllDone() {
			manager.startSession(List.of("a", "b"), TestType.FORM);

			manager.updateResult("a", new CypressTestResult("a", TestType.FORM, TestStatus.PASSED, null, "ok", 100));
			assertTrue(manager.isRunning());

			manager.updateResult("b", new CypressTestResult("b", TestType.FORM, TestStatus.FAILED, "err", "fail", 200));
			assertFalse(manager.isRunning());
		}

		@Test
		@DisplayName("does not set running=false when RUNNING status is reported")
		void doesNotClearRunningForRunningStatus() {
			manager.startSession(List.of("a"), TestType.FORM);

			manager.updateResult("a", new CypressTestResult("a", TestType.FORM, TestStatus.RUNNING, null, null, 0));

			assertTrue(manager.isRunning());
		}

		@Test
		@DisplayName("fires RESULT_UPDATED property change event")
		void firesResultUpdatedEvent() {
			manager.startSession(List.of("test1"), TestType.FORM);
			List<PropertyChangeEvent> events = new ArrayList<>();
			manager.addPropertyChangeListener(events::add);

			CypressTestResult result = new CypressTestResult("test1", TestType.FORM, TestStatus.PASSED, null, "ok",
					500);
			manager.updateResult("test1", result);

			assertEquals(1, events.size());
			assertAll(
					() -> assertEquals(CypressTestSessionManager.PROP_RESULT_UPDATED, events.get(0).getPropertyName()),
					() -> assertSame(result, events.get(0).getNewValue()));

			manager.removePropertyChangeListener(events::add);
		}
	}

	@Nested
	@DisplayName("markRunning")
	class MarkRunning {

		@Test
		@DisplayName("sets test status to RUNNING")
		void setsRunning() {
			manager.startSession(List.of("test1"), TestType.FORM);

			manager.markRunning("test1", TestType.FORM);

			assertEquals(TestStatus.RUNNING, manager.getResults().get(0).status());
		}

		@Test
		@DisplayName("fires RESULT_UPDATED event")
		void firesEvent() {
			manager.startSession(List.of("test1"), TestType.FORM);
			List<PropertyChangeEvent> events = new ArrayList<>();
			manager.addPropertyChangeListener(events::add);

			manager.markRunning("test1", TestType.FORM);

			assertEquals(1, events.size());
			assertEquals(CypressTestSessionManager.PROP_RESULT_UPDATED, events.get(0).getPropertyName());

			manager.removePropertyChangeListener(events::add);
		}
	}

	@Nested
	@DisplayName("counting methods")
	class CountingMethods {

		@BeforeEach
		void setUpSession() {
			manager.startSession(List.of("a", "b", "c", "d", "e"), TestType.FORM);
			manager.updateResult("a", new CypressTestResult("a", TestType.FORM, TestStatus.PASSED, null, "ok", 100));
			manager.updateResult("b", new CypressTestResult("b", TestType.FORM, TestStatus.FAILED, "err", "fail", 200));
			manager.updateResult("c",
					new CypressTestResult("c", TestType.FORM, TestStatus.ERROR, "crash", "Error: x", 300));
		}

		@Test
		@DisplayName("getTotalCount returns total number of tests")
		void totalCount() {
			assertEquals(5, manager.getTotalCount());
		}

		@Test
		@DisplayName("getCompletedCount returns non-PENDING non-RUNNING tests")
		void completedCount() {
			assertEquals(3, manager.getCompletedCount());
		}

		@Test
		@DisplayName("getFailureCount returns only FAILED tests")
		void failureCount() {
			assertEquals(1, manager.getFailureCount());
		}

		@Test
		@DisplayName("getErrorCount returns only ERROR tests")
		void errorCount() {
			assertEquals(1, manager.getErrorCount());
		}

		@Test
		@DisplayName("counts are zero after clear")
		void countsZeroAfterClear() {
			manager.clear();

			assertAll(() -> assertEquals(0, manager.getTotalCount()),
					() -> assertEquals(0, manager.getCompletedCount()),
					() -> assertEquals(0, manager.getFailureCount()), () -> assertEquals(0, manager.getErrorCount()));
		}
	}

	@Nested
	@DisplayName("stop")
	class Stop {

		@Test
		@DisplayName("sets running to false")
		void setsRunningFalse() {
			manager.startSession(List.of("test1"), TestType.FORM);
			assertTrue(manager.isRunning());

			manager.stop();

			assertFalse(manager.isRunning());
		}

		@Test
		@DisplayName("marks remaining PENDING tests as ERROR/Cancelled")
		void marksPendingAsCancelled() {
			manager.startSession(List.of("a", "b", "c"), TestType.E2E);
			manager.updateResult("a", new CypressTestResult("a", TestType.E2E, TestStatus.PASSED, null, "ok", 100));

			manager.stop();

			List<CypressTestResult> results = manager.getResults();
			assertAll(() -> assertEquals(TestStatus.PASSED, results.get(0).status()),
					() -> assertEquals(TestStatus.ERROR, results.get(1).status()),
					() -> assertEquals("Cancelled", results.get(1).errorSummary()),
					() -> assertEquals(TestStatus.ERROR, results.get(2).status()));
		}

		@Test
		@DisplayName("marks RUNNING test as ERROR/Cancelled")
		void marksRunningAsCancelled() {
			manager.startSession(List.of("a"), TestType.E2E);
			manager.markRunning("a", TestType.E2E);

			manager.stop();

			CypressTestResult r = manager.getResults().get(0);
			assertAll(() -> assertEquals(TestStatus.ERROR, r.status()),
					() -> assertEquals("Cancelled", r.errorSummary()));
		}

		@Test
		@DisplayName("does not change already-completed results")
		void leavesCompletedResults() {
			manager.startSession(List.of("a", "b"), TestType.FORM);
			manager.updateResult("a", new CypressTestResult("a", TestType.FORM, TestStatus.PASSED, null, "ok", 100));
			manager.updateResult("b", new CypressTestResult("b", TestType.FORM, TestStatus.FAILED, "boom", "fail", 50));

			manager.stop();

			List<CypressTestResult> results = manager.getResults();
			assertAll(() -> assertEquals(TestStatus.PASSED, results.get(0).status()),
					() -> assertEquals(TestStatus.FAILED, results.get(1).status()),
					() -> assertEquals("boom", results.get(1).errorSummary()));
		}
	}

	@Nested
	@DisplayName("startSession auto-stops a running session")
	class AutoStop {

		@Test
		@DisplayName("pending tests of the previous run are cancelled in its history entry")
		void previousRunCancelledOnNewSession() {
			manager.startSession(List.of("a", "b"), TestType.E2E);
			// leave both PENDING, then start a new session while running
			manager.startSession(List.of("x"), TestType.E2E);

			List<CypressTestSessionManager.HistoryEntry> history = manager.getHistory();
			// newest first: history.get(0) is the new run, history.get(1) is the auto-stopped one
			CypressTestSessionManager.HistoryEntry previous = history.get(1);
			assertTrue(previous.results().stream()
					.allMatch(r -> r.status() == TestStatus.ERROR));
		}
	}

	@Nested
	@DisplayName("history")
	class History {

		@Test
		@DisplayName("a history entry is created immediately when a session starts")
		void historyEntryCreatedOnStart() {
			int before = manager.getHistory().size();
			manager.startSession(List.of("a"), TestType.FORM);
			assertEquals(before + 1, manager.getHistory().size());
		}

		@Test
		@DisplayName("newest run is first in history")
		void newestFirst() {
			manager.startSession(List.of("first"), TestType.FORM);
			manager.updateResult("first",
					new CypressTestResult("first", TestType.FORM, TestStatus.PASSED, null, "ok", 10));
			manager.startSession(List.of("second"), TestType.E2E);

			List<CypressTestSessionManager.HistoryEntry> history = manager.getHistory();
			assertAll(() -> assertEquals("second", history.get(0).results().get(0).testName()),
					() -> assertEquals("first", history.get(1).results().get(0).testName()));
		}

		@Test
		@DisplayName("history entry label reflects type and count")
		void labelReflectsTypeAndCount() {
			manager.startSession(List.of("a", "b"), TestType.E2E);
			String label = manager.getHistory().get(0).toString();
			assertAll(() -> assertTrue(label.contains("E2E Tests")), () -> assertTrue(label.contains("2 tests")));
		}

		@Test
		@DisplayName("history toString shows [running] while tests are pending")
		void toStringShowsRunning() {
			manager.startSession(List.of("a"), TestType.FORM);
			assertTrue(manager.getHistory().get(0).toString().contains("[running]"));
		}

		@Test
		@DisplayName("history toString drops [running] once all done")
		void toStringDropsRunningWhenDone() {
			manager.startSession(List.of("a"), TestType.FORM);
			manager.updateResult("a", new CypressTestResult("a", TestType.FORM, TestStatus.PASSED, null, "ok", 10));
			assertFalse(manager.getHistory().get(0).toString().contains("[running]"));
		}

		@Test
		@DisplayName("history entry updates live as results come in")
		void updatesLive() {
			manager.startSession(List.of("a", "b"), TestType.FORM);
			manager.updateResult("a", new CypressTestResult("a", TestType.FORM, TestStatus.PASSED, null, "ok", 10));

			CypressTestSessionManager.HistoryEntry entry = manager.getHistory().get(0);
			long passed = entry.results().stream().filter(r -> r.status() == TestStatus.PASSED).count();
			assertEquals(1, passed);
		}
	}

	@Nested
	@DisplayName("restoreFromHistory")
	class RestoreFromHistory {

		@Test
		@DisplayName("restores results from a past entry and stops running")
		void restoresResults() {
			manager.startSession(List.of("a"), TestType.FORM);
			manager.updateResult("a", new CypressTestResult("a", TestType.FORM, TestStatus.FAILED, "e", "fail", 10));
			CypressTestSessionManager.HistoryEntry entry = manager.getHistory().get(0);

			manager.startSession(List.of("later"), TestType.E2E); // move away
			manager.restoreFromHistory(entry);

			List<CypressTestResult> results = manager.getResults();
			assertAll(() -> assertEquals(1, results.size()),
					() -> assertEquals("a", results.get(0).testName()),
					() -> assertFalse(manager.isRunning()));
		}
	}

	@Nested
	@DisplayName("clear")
	class Clear {

		@Test
		@DisplayName("removes all results")
		void removesAllResults() {
			manager.startSession(List.of("a", "b"), TestType.FORM);

			manager.clear();

			assertTrue(manager.getResults().isEmpty());
		}

		@Test
		@DisplayName("sets running to false")
		void setsRunningFalse() {
			manager.startSession(List.of("a"), TestType.FORM);

			manager.clear();

			assertFalse(manager.isRunning());
		}

		@Test
		@DisplayName("fires SESSION_STARTED event with empty results")
		void firesEvent() {
			manager.startSession(List.of("a"), TestType.FORM);
			List<PropertyChangeEvent> events = new ArrayList<>();
			manager.addPropertyChangeListener(events::add);

			manager.clear();

			assertEquals(1, events.size());
			assertEquals(CypressTestSessionManager.PROP_SESSION_STARTED, events.get(0).getPropertyName());

			manager.removePropertyChangeListener(events::add);
		}
	}

	@Nested
	@DisplayName("property change listeners")
	class PropertyChangeListeners {

		@Test
		@DisplayName("removed listener receives no more events")
		void removedListenerGetsNoEvents() {
			AtomicInteger count = new AtomicInteger();
			PropertyChangeListener listener = e -> count.incrementAndGet();
			manager.addPropertyChangeListener(listener);

			manager.startSession(List.of("a"), TestType.FORM);
			assertEquals(1, count.get());

			manager.removePropertyChangeListener(listener);
			manager.startSession(List.of("b"), TestType.FORM);
			assertEquals(1, count.get());
		}
	}

	@Nested
	@DisplayName("getResults")
	class GetResults {

		@Test
		@DisplayName("returns unmodifiable list")
		void returnsUnmodifiable() {
			manager.startSession(List.of("a"), TestType.FORM);

			List<CypressTestResult> results = manager.getResults();

			org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> results.add(null));
		}

		@Test
		@DisplayName("returns snapshot that is not affected by later updates")
		void returnsSnapshot() {
			manager.startSession(List.of("a"), TestType.FORM);
			List<CypressTestResult> snapshot = manager.getResults();

			manager.updateResult("a", new CypressTestResult("a", TestType.FORM, TestStatus.PASSED, null, "ok", 100));

			assertEquals(TestStatus.PENDING, snapshot.get(0).status());
		}
	}
}

