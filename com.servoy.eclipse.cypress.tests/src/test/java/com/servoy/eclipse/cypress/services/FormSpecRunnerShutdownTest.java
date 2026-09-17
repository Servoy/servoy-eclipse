package com.servoy.eclipse.cypress.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

public class FormSpecRunnerShutdownTest {
	@Test
	public void testHasShutdownFormPreviewClientsMethod() {
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods) {
			if ("shutdownFormPreviewClients".equals(m.getName())) {
				found = true;
				break;
			}
		}
		assertTrue(found, "FormSpecRunner must have shutdownFormPreviewClients method");
	}

	@Test
	public void testShutdownFormPreviewClientsIsPrivate() {
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		for (Method m : methods) {
			if ("shutdownFormPreviewClients".equals(m.getName())) {
				assertTrue(java.lang.reflect.Modifier.isPrivate(m.getModifiers()),
						"shutdownFormPreviewClients must be private");
				return;
			}
		}
		assertTrue(false, "shutdownFormPreviewClients method not found");
	}

	@Test
	public void testShutdownFormPreviewClientsReturnsVoid() {
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		for (Method m : methods) {
			if ("shutdownFormPreviewClients".equals(m.getName())) {
				assertTrue(m.getReturnType() == void.class, "shutdownFormPreviewClients must return void");
				return;
			}
		}
		assertTrue(false, "shutdownFormPreviewClients method not found");
	}

	@Test
	public void testShutdownFormPreviewClientsHasNoParams() {
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		for (Method m : methods) {
			if ("shutdownFormPreviewClients".equals(m.getName())) {
				assertTrue(m.getParameterCount() == 0, "shutdownFormPreviewClients must have zero parameters");
				return;
			}
		}
		assertTrue(false, "shutdownFormPreviewClients method not found");
	}

	@Test
	public void testFormPreviewNGClientHasShutdownExistingMethod() throws Exception {
		Method method = com.servoy.eclipse.ngclient.startup.FormPreviewNGClient.class
				.getDeclaredMethod("shutdownExisting");
		assertNotNull(method, "FormPreviewNGClient must have shutdownExisting method");
		assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()), "shutdownExisting must be public");
		assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()), "shutdownExisting must be static");
		assertTrue(method.getReturnType() == void.class, "shutdownExisting must return void");
	}

	@Test
	public void testShutdownFormPreviewClientsDoesNotThrowWithNoServer() {
		FormSpecRunner runner = new FormSpecRunner();
		try {
			Method m = FormSpecRunner.class.getDeclaredMethod("shutdownFormPreviewClients");
			m.setAccessible(true);
			m.invoke(runner);
		} catch (java.lang.reflect.InvocationTargetException e) {
			assertTrue(false, "shutdownFormPreviewClients should not throw even without a server: " + e.getCause());
		} catch (Exception e) {
			assertTrue(true, "Expected graceful handling: " + e.getMessage());
		}
	}
}
