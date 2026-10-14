package com.servoy.eclipse.ui.tests.integrationpde.dialogs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.servoy.eclipse.core.tests.integration.utils.DialogGuardRule;
import com.servoy.eclipse.core.tests.integration.utils.TestUtilitiesClass;
import com.servoy.eclipse.ui.dialogs.ServoyLoginDialog;

/**
 * The part of the cloud state tests that needs a real SWT Display: {@code notifyCloudRestored} runs the listeners
 * through {@code Display.getDefault().asyncExec(...)}. In a plain (no display) JVM that call kills the forked VM, so
 * this lives in the integrationpde package instead of next to the other cloud state tests.
 */
@ExtendWith(DialogGuardRule.class)
@DisplayName("ServoyLoginDialog — cloud restored notification (AC7)")
class ServoyLoginDialogCloudStateIntegrationTest {

	// the running workbench registers its own listeners (ServoyLoginStatus), so restore them instead of clearing the list
	private List<Runnable> originalListeners;

	@BeforeEach
	void saveListeners() throws Exception {
		originalListeners = new ArrayList<>(getCloudRestoredListeners());
	}

	@AfterEach
	void restoreListeners() throws Exception {
		CopyOnWriteArrayList<Runnable> listeners = getCloudRestoredListeners();
		listeners.clear();
		listeners.addAll(originalListeners);
	}

	@Test
	@DisplayName("AC7: cloud restored listeners are notified when cloud comes back")
	void testCloudRestoredListenersNotified() throws Exception {
		AtomicBoolean called = new AtomicBoolean(false);
		ServoyLoginDialog.addCloudRestoredListener(() -> called.set(true));

		Method notifyMethod = ServoyLoginDialog.class.getDeclaredMethod("notifyCloudRestored");
		notifyMethod.setAccessible(true);
		notifyMethod.invoke(null);

		// the listeners are run with asyncExec on the UI thread
		TestUtilitiesClass.pumpEventsUntil(5000, () -> assertTrue(called.get(), "cloud restored listener MUST be called"));
	}

	@SuppressWarnings("unchecked")
	private static CopyOnWriteArrayList<Runnable> getCloudRestoredListeners() throws Exception {
		Field field = ServoyLoginDialog.class.getDeclaredField("cloudRestoredListeners");
		field.setAccessible(true);
		return (CopyOnWriteArrayList<Runnable>) field.get(null);
	}
}
