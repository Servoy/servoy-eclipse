package com.servoy.eclipse.cypress.actions;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.servoy.eclipse.cypress.actions.CypressTestResult.TestStatus;
import com.servoy.eclipse.cypress.actions.CypressTestResult.TestType;
import com.servoy.eclipse.cypress.services.FormSpecRunner;

public final class CypressTestSessionManager {
	public static final String PROP_SESSION_STARTED = "sessionStarted";
	public static final String PROP_RESULT_UPDATED = "resultUpdated";

	private static final int MAX_HISTORY = 50;
	private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

	private static final CypressTestSessionManager INSTANCE = new CypressTestSessionManager();

	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private final Map<String, CypressTestResult> results = new LinkedHashMap<>();
	private final LinkedList<HistoryEntry> history = new LinkedList<>();
	private volatile boolean running;
	private HistoryEntry activeHistoryEntry;
	private volatile FormSpecRunner activeRunner;

	public static class HistoryEntry {
		private final String label;
		private final LocalDateTime timestamp;
		private List<CypressTestResult> results;

		public HistoryEntry(String label, LocalDateTime timestamp, List<CypressTestResult> results) {
			this.label = label;
			this.timestamp = timestamp;
			this.results = results;
		}

		public String label() {
			return label;
		}

		public LocalDateTime timestamp() {
			return timestamp;
		}

		public List<CypressTestResult> results() {
			return results;
		}

		void updateResults(List<CypressTestResult> results) {
			this.results = results;
		}

		@Override
		public String toString() {
			long passed = results.stream().filter(r -> r.status() == TestStatus.PASSED).count();
			long failed = results.stream()
					.filter(r -> r.status() == TestStatus.FAILED || r.status() == TestStatus.ERROR).count();
			long pending = results.stream()
					.filter(r -> r.status() == TestStatus.PENDING || r.status() == TestStatus.RUNNING).count();
			String suffix = pending > 0 ? " [running]" : "";
			return timestamp.format(TIME_FMT) + " - " + label + " (" + passed + " passed, " + failed + " failed)"
					+ suffix;
		}
	}

	private CypressTestSessionManager() {
	}

	public static CypressTestSessionManager getInstance() {
		return INSTANCE;
	}

	static CypressTestSessionManager createForTesting() {
		return new CypressTestSessionManager();
	}

	public synchronized void startSession(List<String> testNames, TestType type) {
		if (running) {
			stop();
		}

		results.clear();
		for (String name : testNames) {
			results.put(name, new CypressTestResult(name, type, TestStatus.PENDING, null, null, 0));
		}
		running = true;

		String label = (type == TestType.FORM ? "Form Tests" : "E2E Tests") + " (" + testNames.size() + " tests)";
		activeHistoryEntry = new HistoryEntry(label, LocalDateTime.now(), new ArrayList<>(results.values()));
		history.addFirst(activeHistoryEntry);
		while (history.size() > MAX_HISTORY) {
			history.removeLast();
		}

		pcs.firePropertyChange(PROP_SESSION_STARTED, null, getResults());
	}

	public synchronized void updateResult(String testName, CypressTestResult result) {
		if (!running) {
			return;
		}
		results.put(testName, result);
		if (result.status() != TestStatus.RUNNING) {
			boolean allDone = results.values().stream()
					.noneMatch(r -> r.status() == TestStatus.PENDING || r.status() == TestStatus.RUNNING);
			if (allDone) {
				running = false;
			}
		}
		if (activeHistoryEntry != null) {
			activeHistoryEntry.updateResults(new ArrayList<>(results.values()));
		}
		pcs.firePropertyChange(PROP_RESULT_UPDATED, null, result);
	}

	public synchronized void markRunning(String testName, TestType type) {
		results.put(testName, new CypressTestResult(testName, type, TestStatus.RUNNING, null, null, 0));
		if (activeHistoryEntry != null) {
			activeHistoryEntry.updateResults(new ArrayList<>(results.values()));
		}
		pcs.firePropertyChange(PROP_RESULT_UPDATED, null, results.get(testName));
	}

	public synchronized List<CypressTestResult> getResults() {
		return Collections.unmodifiableList(new ArrayList<>(results.values()));
	}

	public boolean isRunning() {
		return running;
	}

	public synchronized void stop() {
		running = false;
		FormSpecRunner runner = activeRunner;
		if (runner != null) {
			runner.cancel();
		}
		for (Map.Entry<String, CypressTestResult> entry : results.entrySet()) {
			CypressTestResult r = entry.getValue();
			if (r.status() == TestStatus.PENDING || r.status() == TestStatus.RUNNING) {
				results.put(entry.getKey(),
						new CypressTestResult(r.testName(), r.testType(), TestStatus.ERROR, "Cancelled", null, 0));
			}
		}
		if (activeHistoryEntry != null) {
			activeHistoryEntry.updateResults(new ArrayList<>(results.values()));
		}
		pcs.firePropertyChange(PROP_RESULT_UPDATED, null, null);
	}

	public void setActiveRunner(FormSpecRunner runner) {
		this.activeRunner = runner;
	}

	/**
	 * Clears the active runner only if it is still {@code runner}, so a run
	 * finishing its {@code finally} block cannot null out a runner that a newer run
	 * has already installed.
	 */
	public synchronized void clearActiveRunner(FormSpecRunner runner) {
		if (this.activeRunner == runner) {
			this.activeRunner = null;
		}
	}

	public synchronized void clear() {
		results.clear();
		running = false;
		pcs.firePropertyChange(PROP_SESSION_STARTED, null, getResults());
	}

	public synchronized int getTotalCount() {
		return results.size();
	}

	public synchronized int getCompletedCount() {
		return (int) results.values().stream()
				.filter(r -> r.status() != TestStatus.PENDING && r.status() != TestStatus.RUNNING).count();
	}

	public synchronized int getFailureCount() {
		return (int) results.values().stream().filter(r -> r.status() == TestStatus.FAILED).count();
	}

	public synchronized int getErrorCount() {
		return (int) results.values().stream().filter(r -> r.status() == TestStatus.ERROR).count();
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	public synchronized List<HistoryEntry> getHistory() {
		return Collections.unmodifiableList(new ArrayList<>(history));
	}

	public synchronized void restoreFromHistory(HistoryEntry entry) {
		results.clear();
		for (CypressTestResult r : entry.results()) {
			results.put(r.testName(), r);
		}
		running = false;
		activeHistoryEntry = null;
		pcs.firePropertyChange(PROP_SESSION_STARTED, null, getResults());
	}
}
