package com.servoy.eclipse.model.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.servoy.j2db.persistence.AbstractScriptProvider;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.ServoyJSONObject;

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
			assertAll(() -> assertTrue(result), () -> assertTrue(sb.toString().contains("@param")),
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
			assertAll(() -> assertTrue(result), () -> assertTrue(sb.toString().contains("@param")),
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

	/**
	 * SVY-21469 §5.2 - the migration write path
	 * (SolutionSerializer.generateJSONObject, reached via ConvertToNewFormatAction
	 * -> writePersist) must normalize a form-component child's legacy loose-string
	 * customProperties (nested inside a WebComponent's json blob) into a pure json
	 * object, so the rewritten .frm no longer carries the fragile legacy string.
	 */
	@Nested
	@DisplayName("normalizeFormComponentChildCustomProperties")
	class NormalizeFormComponentChildCustomProperties {
		private static final String CUSTOM_PROPERTIES = StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES
				.getPropertyName();
		private static final String LEGACY_CUSTOM_PROPERTIES = "attributes:{ data-Target:\"dashboard-health\" }";

		private Method normalizeMethod;

		@BeforeEach
		void setUpNormalize() throws Exception {
			normalizeMethod = SolutionSerializer.class.getDeclaredMethod("normalizeFormComponentChildCustomProperties",
					JSONObject.class);
			normalizeMethod.setAccessible(true);
		}

		private JSONObject invokeNormalize(JSONObject jsonValue) throws Exception {
			return (JSONObject) normalizeMethod.invoke(null, jsonValue);
		}

		@Test
		@DisplayName("converts a nested form-component child's legacy string customProperties to a json object")
		void convertsNestedLegacyStringToObject() throws Exception {
			JSONObject child = new JSONObject();
			child.put(CUSTOM_PROPERTIES, LEGACY_CUSTOM_PROPERTIES);
			JSONObject webComponentJson = new JSONObject();
			webComponentJson.put("n1", child);

			invokeNormalize(webComponentJson);

			Object customProperties = child.opt(CUSTOM_PROPERTIES);
			assertAll(
					() -> assertFalse(customProperties instanceof String,
							"legacy string customProperties must NOT be left as a String after migration"),
					() -> assertInstanceOf(JSONObject.class, customProperties,
							"nested customProperties must be rewritten as a json object"),
					() -> assertEquals("dashboard-health",
							((JSONObject) customProperties).getJSONObject(IContentSpecConstants.PROPERTY_ATTRIBUTES)
									.getString("data-Target"),
							"nested attributes.data-Target must survive the migration"));
		}

		@Test
		@DisplayName("leaves an already-object nested customProperties unchanged (idempotent)")
		void isIdempotentForObjectValue() throws Exception {
			JSONObject attributes = new JSONObject();
			attributes.put("data-Target", "dashboard-status");
			JSONObject alreadyObject = new JSONObject();
			alreadyObject.put(IContentSpecConstants.PROPERTY_ATTRIBUTES, attributes);
			JSONObject child = new JSONObject();
			child.put(CUSTOM_PROPERTIES, alreadyObject);
			JSONObject webComponentJson = new JSONObject();
			webComponentJson.put("status_icon", child);

			invokeNormalize(webComponentJson);

			Object customProperties = child.opt(CUSTOM_PROPERTIES);
			assertAll(
					() -> assertInstanceOf(JSONObject.class, customProperties,
							"an already-object customProperties must stay a json object"),
					() -> assertSame(alreadyObject, customProperties,
							"an already-object customProperties must not be re-wrapped"),
					() -> assertEquals("dashboard-status",
							((JSONObject) customProperties).getJSONObject(IContentSpecConstants.PROPERTY_ATTRIBUTES)
									.getString("data-Target"),
							"an already-object customProperties value must be untouched"));
		}

		@Test
		@DisplayName("is null-safe (a null json value is returned as-is without throwing)")
		void isNullSafe() {
			assertDoesNotThrow(() -> assertNull(invokeNormalize(null)));
		}

		@Test
		@DisplayName("is a no-op for a web component json with no form-component children")
		void isNoOpWithoutFormComponentChildren() throws Exception {
			JSONObject webComponentJson = new JSONObject();
			webComponentJson.put("someProp", "someValue");
			webComponentJson.put("dataProviderID", "myColumn");

			invokeNormalize(webComponentJson);

			assertAll(() -> assertEquals("someValue", webComponentJson.opt("someProp")),
					() -> assertEquals("myColumn", webComponentJson.opt("dataProviderID")));
		}

		@Test
		@DisplayName("is a no-op for a child object that has no customProperties")
		void isNoOpForChildWithoutCustomProperties() throws Exception {
			JSONObject child = new JSONObject();
			child.put("text", "click me");
			JSONObject webComponentJson = new JSONObject();
			webComponentJson.put("n1", child);

			invokeNormalize(webComponentJson);

			assertAll(() -> assertFalse(child.has(CUSTOM_PROPERTIES), "no customProperties must be introduced"),
					() -> assertEquals("click me", child.opt("text")));
		}

		@Test
		@DisplayName("recurses into a nested form component's child (one extra level of FC nesting)")
		void recursesIntoNestedFormComponentChild() throws Exception {
			// n1.formComponent2.n2.customProperties is a legacy string one level deeper
			// than n1
			JSONObject innerChild = new JSONObject();
			innerChild.put(CUSTOM_PROPERTIES, LEGACY_CUSTOM_PROPERTIES);
			JSONObject nestedFormComponentProperty = new JSONObject();
			nestedFormComponentProperty.put("n2", innerChild);
			JSONObject outerChild = new JSONObject();
			outerChild.put("formComponent2", nestedFormComponentProperty);
			JSONObject webComponentJson = new JSONObject();
			webComponentJson.put("n1", outerChild);

			invokeNormalize(webComponentJson);

			Object customProperties = innerChild.opt(CUSTOM_PROPERTIES);
			assertAll(
					() -> assertInstanceOf(JSONObject.class, customProperties,
							"a legacy string customProperties in a nested form component must also be normalized"),
					() -> assertEquals("dashboard-health",
							((JSONObject) customProperties).getJSONObject(IContentSpecConstants.PROPERTY_ATTRIBUTES)
									.getString("data-Target"),
							"the deeply nested attributes.data-Target must survive the migration"));
		}

		@Test
		@DisplayName("the loose 'attributes:{ data-Target:\"x\" }' fragment parses to nested attributes.data-Target")
		void looseFragmentParsesToNestedKeys() {
			ServoyJSONObject parsed = new ServoyJSONObject(LEGACY_CUSTOM_PROPERTIES, false, false, true);
			assertEquals("dashboard-health",
					parsed.getJSONObject(IContentSpecConstants.PROPERTY_ATTRIBUTES).getString("data-Target"),
					"the ServoyJSONObject(str,false,false,true) parse must yield attributes.data-Target");
		}
	}
}
