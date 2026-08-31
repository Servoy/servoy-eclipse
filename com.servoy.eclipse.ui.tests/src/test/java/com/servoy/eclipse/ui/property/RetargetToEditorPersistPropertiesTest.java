package com.servoy.eclipse.ui.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.junit.Before;
import org.junit.Test;

/**
 * SVY-21338: Unit tests for {@link RetargetToEditorPersistProperties}.
 *
 * Tests the pure-logic delegation methods and short-circuit paths that do not
 * require a live SWT Display. The synchronous execution tests (proving syncExec
 * behavior) are in {@code RetargetToEditorPersistPropertiesIntegrationTest}.
 *
 * Written against JUnit 4 (rather than JUnit Jupiter) because this test
 * fragment inherits com.servoy.eclipse.ui's full dependency graph (via
 * Fragment-Host), which transitively pulls in mismatched JUnit 5/6 jars and
 * breaks the JUnit 5 test engine in this workspace. JUnit 4 has no such
 * conflict here.
 */
public class RetargetToEditorPersistPropertiesTest {
	private StubPropertySource delegate;
	private RetargetToEditorPersistProperties retarget;

	@Before
	public void setUp() {
		delegate = new StubPropertySource();
		retarget = new RetargetToEditorPersistProperties(delegate);
	}

	// --- Delegation ---

	@Test
	public void getDelegateReturnsWrappedSource() {
		assertSame(delegate, retarget.getDelegate());
	}

	@Test
	public void getEditableValueDelegates() {
		delegate.editableValue = "editable";
		assertEquals("editable", retarget.getEditableValue());
	}

	@Test
	public void getPropertyDescriptorsDelegates() {
		IPropertyDescriptor[] descriptors = new IPropertyDescriptor[0];
		delegate.descriptors = descriptors;
		assertSame(descriptors, retarget.getPropertyDescriptors());
	}

	@Test
	public void getPropertyValueDelegates() {
		delegate.propertyValues.put("key1", "value1");
		assertEquals("value1", retarget.getPropertyValue("key1"));
	}

	@Test
	public void isPropertySetDelegates() {
		delegate.setProperties.add("key1");
		assertTrue(retarget.isPropertySet("key1"));
		assertFalse(retarget.isPropertySet("nonexistent"));
	}

	@Test
	public void toStringDelegates() {
		delegate.toStringValue = "StubToString";
		assertEquals("StubToString", retarget.toString());
	}

	// --- getAdapter behavior ---

	@Test
	public void getAdapterReturnsNullForNonAdaptable() {
		assertNull(retarget.getAdapter(String.class));
	}

	@Test
	public void getAdapterDelegatesToAdaptable() {
		Object adapted = "adapted-object";
		AdaptablePropertySource adaptableDelegate = new AdaptablePropertySource(adapted);
		RetargetToEditorPersistProperties adaptableRetarget = new RetargetToEditorPersistProperties(
				adaptableDelegate);

		assertSame(adapted, adaptableRetarget.getAdapter(String.class));
	}

	@Test
	public void getAdapterReturnsNullFromAdaptableWhenNotSupported() {
		AdaptablePropertySource adaptableDelegate = new AdaptablePropertySource(null);
		RetargetToEditorPersistProperties adaptableRetarget = new RetargetToEditorPersistProperties(
				adaptableDelegate);

		assertNull(adaptableRetarget.getAdapter(Integer.class));
	}

	// --- openPersistEditor (static) ---

	@Test
	public void openPersistEditorReturnsNullForPlainPropertySource() {
		assertNull(RetargetToEditorPersistProperties.openPersistEditor(delegate, false));
	}

	@Test
	public void openPersistEditorReturnsNullForPlainPropertySourceActivate() {
		assertNull(RetargetToEditorPersistProperties.openPersistEditor(delegate, true));
	}

	// --- setPropertyValue short-circuit ---

	@Test
	public void setPropertyValueSkipsWhenValueUnchanged() {
		delegate.propertyValues.put("prop", "sameValue");
		// If setPropertyValue did NOT short-circuit, it would call Display.getCurrent()
		// which is null outside SWT, causing a NullPointerException.
		retarget.setPropertyValue("prop", "sameValue");
	}

	@Test
	public void setPropertyValueSkipsWhenBothNull() {
		delegate.propertyValues.put("prop", null);
		retarget.setPropertyValue("prop", null);
	}

	@Test
	public void setPropertyValueSkipsForIntegerEquality() {
		delegate.propertyValues.put("count", Integer.valueOf(42));
		retarget.setPropertyValue("count", Integer.valueOf(42));
	}

	// --- Test doubles ---

	/**
	 * Minimal IPropertySource stub for testing delegation.
	 */
	private static class StubPropertySource implements IPropertySource {
		Object editableValue;
		IPropertyDescriptor[] descriptors = new IPropertyDescriptor[0];
		final java.util.Map<Object, Object> propertyValues = new java.util.HashMap<>();
		final java.util.Set<Object> setProperties = new java.util.HashSet<>();
		String toStringValue = "StubPropertySource";
		final java.util.List<Object> resetCalls = new java.util.ArrayList<>();
		final java.util.List<Object[]> setCalls = new java.util.ArrayList<>();

		@Override
		public Object getEditableValue() {
			return editableValue;
		}

		@Override
		public IPropertyDescriptor[] getPropertyDescriptors() {
			return descriptors;
		}

		@Override
		public Object getPropertyValue(Object id) {
			return propertyValues.get(id);
		}

		@Override
		public boolean isPropertySet(Object id) {
			return setProperties.contains(id);
		}

		@Override
		public void resetPropertyValue(Object id) {
			resetCalls.add(id);
		}

		@Override
		public void setPropertyValue(Object id, Object value) {
			setCalls.add(new Object[] { id, value });
			propertyValues.put(id, value);
		}

		@Override
		public String toString() {
			return toStringValue;
		}
	}

	/**
	 * IPropertySource that also implements IAdaptable for getAdapter tests.
	 */
	private static class AdaptablePropertySource implements IPropertySource, IAdaptable {
		private final Object adaptedObject;

		AdaptablePropertySource(Object adaptedObject) {
			this.adaptedObject = adaptedObject;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return (T) adaptedObject;
		}

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
			return null;
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
		}
	}
}
