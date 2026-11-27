package com.servoy.eclipse.model.tests.plainjunit.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.repository.RepositoryTestAccess;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.tests.plainjunit.utils.TestPersists;
import com.servoy.j2db.persistence.AbstractScriptProvider;
import com.servoy.j2db.persistence.ArgumentType;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;

@DisplayName("SolutionSerializer")
class SolutionSerializerTest {
	/**
	 * The generated JSDoc of methods and calculations (getComment, the public entry
	 * of the private generateParams): the @param lines for the method arguments.
	 * Real persists (through the public persist factory) on a mocked repository
	 * with an empty content spec, so only the uuid/type id end up in @properties.
	 */
	@Nested
	@DisplayName("getComment @param generation")
	class GenerateParams {
		private static final String TODO_LINE = " * TODO generated, please specify type and doc for the params\n";

		private IDeveloperRepository repository;

		@BeforeEach
		void setUpRepository() throws Exception {
			repository = mock(IDeveloperRepository.class);
			when(repository.getPersistAsValueMap(any())).thenAnswer(inv -> new HashMap<String, Object>());
			when(repository.getContentSpec()).thenReturn(new ContentSpec());
		}

		private <T extends AbstractScriptProvider> T scriptProvider(int typeId, String declaration,
				MethodArgument... arguments) {
			T provider = TestPersists.create(null, typeId, UUID.randomUUID());
			provider.setDeclaration(declaration);
			if (arguments.length > 0)
				provider.setRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS, arguments);
			return provider;
		}

		private String comment(AbstractScriptProvider provider) {
			return SolutionSerializer.getComment(provider, null, repository);
		}

		@Test
		@DisplayName("a method without arguments gets no @param lines")
		void methodWithoutArgumentsHasNoParams() {
			String comment = comment(scriptProvider(IRepository.METHODS, "function noArgs() {}"));

			assertAll(() -> assertTrue(comment.startsWith("/**\n"), comment),
					() -> assertFalse(comment.contains("@param"), comment),
					() -> assertFalse(comment.contains(TODO_LINE), comment));
		}

		@Test
		@DisplayName("a method gets a @param line per argument, after the TODO line, in argument order")
		void methodArgumentsBecomeParams() {
			String comment = comment(scriptProvider(IRepository.METHODS, "function withArgs(first, second) {}",
					new MethodArgument("first", null, null), new MethodArgument("second", null, null)));

			assertTrue(comment.contains(TODO_LINE + " * @param first\n * @param second\n"), comment);
		}

		@Test
		@DisplayName("the argument type is written in braces, an Any type is left out")
		void argumentTypeIsWritten() {
			String comment = comment(scriptProvider(IRepository.METHODS, "function typed(text, anything) {}",
					new MethodArgument("text", ArgumentType.String, null),
					new MethodArgument("anything", ArgumentType.valueOf("Any"), null)));

			assertTrue(comment.contains(" * @param {String} text\n * @param anything\n"), comment);
		}

		@Test
		@DisplayName("a calculation gets @param lines too (no ClassCastException for ScriptCalculation)")
		void calculationArgumentsBecomeParams() {
			String comment = comment(scriptProvider(IRepository.SCRIPTCALCULATIONS, "function calc(value) {}",
					new MethodArgument("value", null, null)));

			assertTrue(comment.contains(TODO_LINE + " * @param value\n"), comment);
		}

		@Test
		@DisplayName("current behaviour: the argument description is glued to the name without a space")
		void descriptionFollowsTheNameWithoutSeparator() {
			String comment = comment(scriptProvider(IRepository.METHODS, "function described(name) {}",
					new MethodArgument("name", null, "the name")));

			// possible bug: generateParams appends the description right after the name
			assertTrue(comment.contains(" * @param namethe name\n"), comment);
		}

		@Test
		@DisplayName("an existing JSDoc without @param gets the params inserted, one with @param is left as is")
		void existingCommentGetsParamsOnlyWhenItHasNone() {
			String withoutParams = comment(scriptProvider(IRepository.METHODS,
					"/**\n * Does it.\n */\nfunction existing(arg) {}", new MethodArgument("arg", null, null)));
			String withParams = comment(scriptProvider(IRepository.METHODS,
					"/**\n * Does it.\n * @param {Number} arg\n */\nfunction existing(arg) {}",
					new MethodArgument("arg", null, null)));

			assertAll(() -> assertTrue(withoutParams.contains(" * @param arg\n"), withoutParams),
					() -> assertTrue(withoutParams.contains("Does it."), withoutParams),
					() -> assertTrue(withParams.contains("@param {Number} arg"), withParams),
					() -> assertFalse(withParams.contains(TODO_LINE), withParams));
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

		private JSONObject invokeNormalize(JSONObject jsonValue) {
			// package-private in SolutionSerializer (SVY-21469), reached through the same-package bridge
			return RepositoryTestAccess.normalizeFormComponentChildCustomProperties(jsonValue);
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
