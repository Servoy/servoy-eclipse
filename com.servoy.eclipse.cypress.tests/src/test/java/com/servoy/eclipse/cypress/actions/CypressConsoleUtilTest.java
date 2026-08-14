package com.servoy.eclipse.cypress.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CypressConsoleUtil")
public class CypressConsoleUtilTest {

	@Nested
	@DisplayName("class structure")
	class ClassStructure {
		@Test
		@DisplayName("is final utility class")
		void isFinalUtilityClass() {
			assertTrue(Modifier.isFinal(CypressConsoleUtil.class.getModifiers()));
		}

		@Test
		@DisplayName("has private constructor")
		void hasPrivateConstructor() throws NoSuchMethodException {
			Constructor<?> ctor = CypressConsoleUtil.class.getDeclaredConstructor();
			assertTrue(Modifier.isPrivate(ctor.getModifiers()));
		}

		@Test
		@DisplayName("private constructor can be invoked via reflection")
		void privateConstructorInvocable() throws Exception {
			Constructor<?> ctor = CypressConsoleUtil.class.getDeclaredConstructor();
			ctor.setAccessible(true);
			Object instance = ctor.newInstance();
			assertNotNull(instance);
		}

		@Test
		@DisplayName("has findOrCreateConsole static method")
		void hasFindOrCreateConsoleMethod() throws NoSuchMethodException {
			Method m = CypressConsoleUtil.class.getMethod("findOrCreateConsole");
			assertNotNull(m);
			assertTrue(Modifier.isStatic(m.getModifiers()));
			assertEquals(MessageConsole.class, m.getReturnType());
		}

		@Test
		@DisplayName("has showConsole static method")
		void hasShowConsoleMethod() throws NoSuchMethodException {
			Method m = CypressConsoleUtil.class.getMethod("showConsole", MessageConsole.class);
			assertNotNull(m);
			assertTrue(Modifier.isStatic(m.getModifiers()));
		}

		@Test
		@DisplayName("has getConsoleName static method")
		void hasGetConsoleNameMethod() throws NoSuchMethodException {
			Method m = CypressConsoleUtil.class.getDeclaredMethod("getConsoleName");
			assertNotNull(m);
			assertTrue(Modifier.isStatic(m.getModifiers()));
			assertEquals(String.class, m.getReturnType());
		}

		@Test
		@DisplayName("has isMatchingConsole static method")
		void hasIsMatchingConsoleMethod() throws NoSuchMethodException {
			Method m = CypressConsoleUtil.class.getDeclaredMethod("isMatchingConsole", IConsole.class);
			assertNotNull(m);
			assertTrue(Modifier.isStatic(m.getModifiers()));
			assertEquals(boolean.class, m.getReturnType());
		}
	}

	@Nested
	@DisplayName("getConsoleName")
	class GetConsoleName {
		@Test
		@DisplayName("returns non-null value")
		void returnsNonNull() {
			assertNotNull(CypressConsoleUtil.getConsoleName());
		}

		@Test
		@DisplayName("returns 'Cypress Form Tests'")
		void returnsExpectedName() {
			assertEquals("Cypress Form Tests", CypressConsoleUtil.getConsoleName());
		}

		@Test
		@DisplayName("CONSOLE_NAME constant matches getConsoleName()")
		void constantMatchesGetter() {
			assertEquals(CypressConsoleUtil.CONSOLE_NAME, CypressConsoleUtil.getConsoleName());
		}

		@Test
		@DisplayName("console name is not empty")
		void isNotEmpty() {
			assertFalse(CypressConsoleUtil.getConsoleName().isEmpty());
		}
	}

	// isMatchingConsole is tested in CypressConsoleUtilIntegrationTest
	// because MessageConsole cannot be instantiated without a running ConsolePlugin.
}

