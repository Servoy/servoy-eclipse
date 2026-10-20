package com.servoy.eclipse.ui.tests.integrationpde.property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.ui.property.RetargetToEditorPersistProperties;

/**
 * SVY-21338: Integration tests for {@link RetargetToEditorPersistProperties}.
 *
 * These tests verify the critical fix: {@code resetPropertyValue()} must use
 * {@code syncExec} (not {@code asyncExec}) so that the model mutation completes
 * before the method returns. This matches the pattern already used by
 * {@code setPropertyValue()} since SVY-18860.
 *
 * <p>
 * The tests use a testable subclass that overrides {@code updateProperty()} to
 * record calls without needing a real editor. The key assertion is that after
 * calling {@code resetPropertyValue()}, the recorded call list is immediately
 * populated — which only happens with {@code syncExec}. With {@code asyncExec},
 * the call would be deferred to the event queue and the list would be empty.
 *
 * <p>
 * {@code RetargetToEditorPersistProperties.setPropertyValue()}/{@code resetPropertyValue()}
 * call {@code Display.getCurrent()}, which requires a live SWT Display on the
 * current thread. A Display is created once for the whole class (SWT only
 * allows one Display per thread) and disposed afterwards.
 *
 */
public class RetargetToEditorPersistPropertiesIntegrationTest {
	private static Display display;

	private StubPropertySource delegate;
	private TestableRetargetProperties retarget;

	@AfterAll
	public static void tearDownDisplay() {
		if (display != null && !display.isDisposed()) {
			display.dispose();
		}
		display = null;
	}

	@BeforeEach
	public void setUp() {
		// RetargetToEditorPersistProperties.setPropertyValue()/resetPropertyValue() call
		// Display.getCurrent(), which is thread-local: it only finds a Display that was
		// created on this exact thread. Create it here (in @BeforeEach, run on the same
		// thread as the @Test method) rather than in @BeforeAll, since some JUnit
		// runners are not guaranteed to run class-level fixtures on the same thread as
		// the test method itself.
		if (Display.getCurrent() == null) {
			try {
				display = new Display();
			} catch (Throwable t) {
				throw new RuntimeException("Display creation failed on thread "
						+ Thread.currentThread().getName() + " (id=" + Thread.currentThread().getId() + ")", t);
			}
			if (Display.getCurrent() == null) {
				throw new IllegalStateException(
						"Display was created but Display.getCurrent() is still null on thread "
								+ Thread.currentThread().getName() + " (id=" + Thread.currentThread().getId()
								+ "); created display=" + display + " current-thread-of-display="
								+ (display == null ? "n/a" : display.getThread()));
			}
		}
		delegate = new StubPropertySource();
		retarget = new TestableRetargetProperties(delegate);
	}

	// --- resetPropertyValue ---

	@Test
	public void resetPropertyValue_updatePropertyCalledSynchronously() {
		retarget.resetPropertyValue("dataSource");

		// With syncExec, the call list is populated BEFORE resetPropertyValue returns.
		// With asyncExec, this list would be empty here (call deferred to event queue).
		assertFalse(retarget.updateCalls.isEmpty(), "updateProperty must be called synchronously (syncExec); with asyncExec it would be deferred");
	}

	@Test
	public void resetPropertyValue_passesSetFalse() {
		retarget.resetPropertyValue("dataSource");

		assertEquals(1, retarget.updateCalls.size());
		assertFalse(retarget.updateCalls.get(0).set, "resetPropertyValue must pass set=false to updateProperty");
	}

	@Test
	public void resetPropertyValue_passesCorrectId() {
		retarget.resetPropertyValue("dataSource");

		assertEquals("dataSource", retarget.updateCalls.get(0).id);
	}

	@Test
	public void resetPropertyValue_passesNullValue() {
		retarget.resetPropertyValue("dataSource");

		assertNull(retarget.updateCalls.get(0).value);
	}

	@Test
	public void resetPropertyValue_propagatesDifferentIds() {
		retarget.resetPropertyValue("name");
		retarget.resetPropertyValue("size");

		assertEquals(2, retarget.updateCalls.size());
		assertEquals("name", retarget.updateCalls.get(0).id);
		assertEquals("size", retarget.updateCalls.get(1).id);
	}

	// --- setPropertyValue ---

	@Test
	public void setPropertyValue_updatePropertyCalledSynchronously() {
		delegate.propertyValues.put("dataSource", "old_ds");
		retarget.setPropertyValue("dataSource", "new_ds");

		assertFalse(retarget.updateCalls.isEmpty(), "updateProperty must be called synchronously (syncExec); with asyncExec it would be deferred");
	}

	@Test
	public void setPropertyValue_passesSetTrue() {
		delegate.propertyValues.put("dataSource", "old_ds");
		retarget.setPropertyValue("dataSource", "new_ds");

		assertTrue(retarget.updateCalls.get(0).set, "setPropertyValue must pass set=true to updateProperty");
	}

	@Test
	public void setPropertyValue_passesCorrectIdAndValue() {
		delegate.propertyValues.put("dataSource", "old_ds");
		retarget.setPropertyValue("dataSource", "new_ds");

		assertEquals("dataSource", retarget.updateCalls.get(0).id);
		assertEquals("new_ds", retarget.updateCalls.get(0).value);
	}

	@Test
	public void setPropertyValue_skipsWhenValueUnchanged() {
		delegate.propertyValues.put("dataSource", "same_value");
		retarget.setPropertyValue("dataSource", "same_value");

		assertTrue(retarget.updateCalls.isEmpty(), "setPropertyValue must short-circuit when value equals current value");
	}

	@Test
	public void setPropertyValue_skipsWhenBothNull() {
		delegate.propertyValues.put("dataSource", null);
		retarget.setPropertyValue("dataSource", null);

		assertTrue(retarget.updateCalls.isEmpty(), "setPropertyValue must short-circuit when both values are null");
	}

	// --- Symmetry between reset and set ---

	@Test
	public void bothResetAndSetUseSyncExec() {
		// Reset
		retarget.resetPropertyValue("prop1");
		int resetCallCount = retarget.updateCalls.size();

		// Set (with changed value)
		delegate.propertyValues.put("prop2", "old");
		retarget.setPropertyValue("prop2", "new");
		int totalCallCount = retarget.updateCalls.size();

		assertEquals(1, resetCallCount, "resetPropertyValue must call updateProperty synchronously");
		assertEquals(2, totalCallCount, "setPropertyValue must call updateProperty synchronously");
		assertFalse(retarget.updateCalls.get(0).set, "first call (reset) must have set=false");
		assertTrue(retarget.updateCalls.get(1).set, "second call (set) must have set=true");
	}

	// --- Test infrastructure ---

	/**
	 * Records an updateProperty invocation.
	 */
	static class UpdateCall {
		final boolean set;
		final Object id;
		final Object value;

		UpdateCall(boolean set, Object id, Object value) {
			this.set = set;
			this.id = id;
			this.value = value;
		}
	}

	/**
	 * Subclass that overrides {@code updateProperty()} to record calls instead of
	 * opening an editor. This isolates the syncExec/asyncExec behavior from the
	 * editor infrastructure.
	 */
	static class TestableRetargetProperties extends RetargetToEditorPersistProperties {
		final List<UpdateCall> updateCalls = new ArrayList<>();

		TestableRetargetProperties(IPropertySource delegate) {
			super(delegate);
		}

		@Override
		protected void updateProperty(boolean set, Object id, Object value) {
			updateCalls.add(new UpdateCall(set, id, value));
		}
	}

	/**
	 * Minimal IPropertySource stub.
	 */
	private static class StubPropertySource implements IPropertySource {
		final Map<Object, Object> propertyValues = new HashMap<>();

		@Override
		public Object getEditableValue() {
			return null;
		}

		@Override
		public IPropertyDescriptor[] getPropertyDescriptors() {
			return new IPropertyDescriptor[0];
		}

		@Override
		public Object getPropertyValue(Object id) {
			return propertyValues.get(id);
		}

		@Override
		public boolean isPropertySet(Object id) {
			return false;
		}

		@Override
		public void resetPropertyValue(Object id) {
		}

		@Override
		public void setPropertyValue(Object id, Object value) {
			propertyValues.put(id, value);
		}
	}
}
