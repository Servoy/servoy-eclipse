package com.servoy.eclipse.model.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.servoy.j2db.persistence.AbstractScriptProvider;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;

@DisplayName("SolutionSerializer")
@ExtendWith(MockitoExtension.class)
class SolutionSerializerTest {
	private Method generateParamsMethod;

	@BeforeEach
	void setUp() throws Exception {
		generateParamsMethod = SolutionSerializer.class.getDeclaredMethod("generateParams", StringBuilder.class,
				AbstractScriptProvider.class);
		generateParamsMethod.setAccessible(true);
	}

	@Nested
	@DisplayName("generateParams")
	class GenerateParams {
		@Test
		@DisplayName("returns false for null input")
		void returnsFalseForNull() throws Exception {
			StringBuilder sb = new StringBuilder();
			boolean result = (boolean) generateParamsMethod.invoke(null, sb, null);
			assertFalse(result);
		}

		@Test
		@DisplayName("does not throw ClassCastException for ScriptCalculation")
		void doesNotThrowForScriptCalculation() throws Exception {
			ScriptCalculation calc = mock(ScriptCalculation.class);
			when(calc.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS)).thenReturn(null);

			StringBuilder sb = new StringBuilder();
			assertDoesNotThrow(() -> generateParamsMethod.invoke(null, sb, calc));
		}

		@Test
		@DisplayName("returns false for ScriptCalculation with no arguments")
		void returnsFalseForCalcWithNoArgs() throws Exception {
			ScriptCalculation calc = mock(ScriptCalculation.class);
			when(calc.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS)).thenReturn(null);

			StringBuilder sb = new StringBuilder();
			boolean result = (boolean) generateParamsMethod.invoke(null, sb, calc);
			assertFalse(result);
		}

		@Test
		@DisplayName("generates params for ScriptCalculation with arguments")
		void generatesParamsForCalcWithArgs() throws Exception {
			ScriptCalculation calc = mock(ScriptCalculation.class);
			MethodArgument arg = new MethodArgument("myParam", null, null);
			when(calc.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS)).thenReturn(new MethodArgument[] { arg });

			StringBuilder sb = new StringBuilder();
			boolean result = (boolean) generateParamsMethod.invoke(null, sb, calc);
			assertAll(
					() -> assertTrue(result),
					() -> assertTrue(sb.toString().contains("@param")),
					() -> assertTrue(sb.toString().contains("myParam")));
		}

		@Test
		@DisplayName("generates params for ScriptMethod with arguments")
		void generatesParamsForScriptMethod() throws Exception {
			ScriptMethod method = mock(ScriptMethod.class);
			MethodArgument arg = new MethodArgument("testParam", null, null);
			when(method.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS)).thenReturn(new MethodArgument[] { arg });

			StringBuilder sb = new StringBuilder();
			boolean result = (boolean) generateParamsMethod.invoke(null, sb, method);
			assertAll(
					() -> assertTrue(result),
					() -> assertTrue(sb.toString().contains("@param")),
					() -> assertTrue(sb.toString().contains("testParam")));
		}

		@Test
		@DisplayName("returns false for ScriptMethod with no arguments")
		void returnsFalseForScriptMethodWithNoArgs() throws Exception {
			ScriptMethod method = mock(ScriptMethod.class);
			when(method.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS)).thenReturn(null);

			StringBuilder sb = new StringBuilder();
			boolean result = (boolean) generateParamsMethod.invoke(null, sb, method);
			assertFalse(result);
		}
	}
}
