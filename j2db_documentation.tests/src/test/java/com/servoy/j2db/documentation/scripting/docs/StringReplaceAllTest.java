package com.servoy.j2db.documentation.scripting.docs;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StringReplaceAllTest {
	private List<Method> replaceAllMethods;

	@BeforeEach
	void setUp() {
		replaceAllMethods = Arrays.stream(String.class.getDeclaredMethods())
				.filter(m -> "js_replaceAll".equals(m.getName())).collect(Collectors.toList());
	}

	@Nested
	class OverloadCount {
		@Test
		@DisplayName("has exactly 4 js_replaceAll overloads")
		void hasExactlyFourOverloads() {
			assertEquals(4, replaceAllMethods.size(),
					"Expected 4 js_replaceAll overloads but found " + replaceAllMethods.size());
		}
	}

	@Nested
	class RegExpStringOverload {
		@Test
		@DisplayName("js_replaceAll(RegExp, String) exists with correct signature")
		void regexpStringOverloadExists() {
			Method method = assertDoesNotThrow(
					() -> String.class.getDeclaredMethod("js_replaceAll", RegExp.class, String.class),
					"js_replaceAll(RegExp, String) should exist");
			assertAll(() -> assertNotNull(method),
					() -> assertEquals(String.class, method.getReturnType(), "Return type should be String"));
		}
	}

	@Nested
	class RegExpFunctionOverload {
		@Test
		@DisplayName("js_replaceAll(RegExp, Function) exists with correct signature")
		void regexpFunctionOverloadExists() {
			Method method = assertDoesNotThrow(
					() -> String.class.getDeclaredMethod("js_replaceAll", RegExp.class, Function.class),
					"js_replaceAll(RegExp, Function) should exist");
			assertAll(() -> assertNotNull(method),
					() -> assertEquals(String.class, method.getReturnType(), "Return type should be String"));
		}
	}

	@Nested
	class StringStringOverload {
		@Test
		@DisplayName("js_replaceAll(String, String) exists with correct signature")
		void stringStringOverloadExists() {
			Method method = assertDoesNotThrow(
					() -> String.class.getDeclaredMethod("js_replaceAll", String.class, String.class),
					"js_replaceAll(String, String) should exist");
			assertAll(() -> assertNotNull(method),
					() -> assertEquals(String.class, method.getReturnType(), "Return type should be String"));
		}
	}

	@Nested
	class StringFunctionOverload {
		@Test
		@DisplayName("js_replaceAll(String, Function) exists with correct signature")
		void stringFunctionOverloadExists() {
			Method method = assertDoesNotThrow(
					() -> String.class.getDeclaredMethod("js_replaceAll", String.class, Function.class),
					"js_replaceAll(String, Function) should exist");
			assertAll(() -> assertNotNull(method),
					() -> assertEquals(String.class, method.getReturnType(), "Return type should be String"));
		}
	}

	@Nested
	class ReturnTypeConsistency {
		@Test
		@DisplayName("all js_replaceAll overloads return String")
		void allOverloadsReturnString() {
			assertAll(replaceAllMethods.stream().map(m -> () -> assertEquals(String.class, m.getReturnType(),
					"Overload with params " + Arrays.toString(m.getParameterTypes()) + " should return String")));
		}
	}

	@Nested
	class ParameterCount {
		@Test
		@DisplayName("all js_replaceAll overloads take exactly 2 parameters")
		void allOverloadsTakeTwoParameters() {
			assertAll(replaceAllMethods.stream().map(m -> () -> assertEquals(2, m.getParameterCount(),
					"Overload " + m + " should take exactly 2 parameters")));
		}
	}
}
