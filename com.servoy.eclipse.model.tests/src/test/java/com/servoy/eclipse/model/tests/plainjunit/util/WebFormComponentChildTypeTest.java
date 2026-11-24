package com.servoy.eclipse.model.tests.plainjunit.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.ICommonWebComponent;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;

class WebFormComponentChildTypeTest {
	@Nested
	class ImplementsICommonWebComponent {
		@Test
		@DisplayName("WebFormComponentChildType is assignable to ICommonWebComponent")
		void isAssignableToICommonWebComponent() {
			assertTrue(ICommonWebComponent.class.isAssignableFrom(WebFormComponentChildType.class));
		}

		@Test
		@DisplayName("casting WebFormComponentChildType to ICommonWebComponent does not throw ClassCastException")
		void castDoesNotThrowClassCastException() {
			WebFormComponentChildType mock = Mockito.mock(WebFormComponentChildType.class);
			assertDoesNotThrow(() -> {
				ICommonWebComponent casted = (ICommonWebComponent) mock;
				assertNotNull(casted);
			});
		}

		@Test
		@DisplayName("mock instance passes instanceof check for ICommonWebComponent")
		void instanceOfCheckPasses() {
			WebFormComponentChildType mock = Mockito.mock(WebFormComponentChildType.class);
			assertInstanceOf(ICommonWebComponent.class, mock);
		}
	}

	@Nested
	class GetPropertyDescriptionContract {
		@Test
		@DisplayName("getPropertyDescription method exists and matches ICommonWebComponent contract")
		void methodMatchesInterfaceContract() throws NoSuchMethodException {
			Method interfaceMethod = ICommonWebComponent.class.getMethod("getPropertyDescription");
			Method classMethod = WebFormComponentChildType.class.getMethod("getPropertyDescription");

			assertAll(
					() -> assertNotNull(classMethod, "getPropertyDescription must exist on WebFormComponentChildType"),
					() -> assertTrue(interfaceMethod.getReturnType().isAssignableFrom(classMethod.getReturnType()),
							"return type must be compatible with ICommonWebComponent.getPropertyDescription()"));
		}

		@Test
		@DisplayName("mocked getPropertyDescription returns value accessible via ICommonWebComponent reference")
		void getPropertyDescriptionAccessibleViaInterface() {
			WebFormComponentChildType mock = Mockito.mock(WebFormComponentChildType.class);
			PropertyDescription pd = Mockito.mock(PropertyDescription.class);
			Mockito.when(mock.getPropertyDescription()).thenReturn(pd);

			ICommonWebComponent asInterface = mock;
			assertNotNull(asInterface.getPropertyDescription());
		}
	}

	/**
	 * SVY-21469 §5.1 - the flattened, non-mutation read path (getJson(false, true),
	 * reached via getProperty(PROPERTY_JSON)) must normalize a form-component
	 * child's legacy loose-string customProperties into a ServoyJSONObject, so
	 * downstream BaseComponent.getAttributes() sees a Map and delivers data-Target
	 * to onAction.
	 */
	@Nested
	class LegacyStringCustomPropertiesNormalization {
		private static final String CUSTOM_PROPERTIES = StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES
				.getPropertyName();
		private static final String LEGACY_CUSTOM_PROPERTIES = "attributes:{ data-Target:\"dashboard-health\" }";

		/**
		 * Builds a WebFormComponentChildType instance without running the heavy
		 * constructor (Objenesis via Mockito CALLS_REAL_METHODS), wires the private
		 * fcCompAndPropPath and the protected AbstractBase.parent to a mocked parent
		 * web component whose json blob is {@code parentBlob}, and leaves the
		 * {@code element} field null so getJson takes the simple, non-merge branch.
		 * Real methods (including the private getJson) then run.
		 */
		private WebFormComponentChildType newChildOnParentBlob(JSONObject parentBlob) throws Exception {
			IBasicWebObject parent = Mockito.mock(IBasicWebObject.class);
			// fcCompAndPropPath = [rootUuid, rootFCPropertyName, childName]
			Mockito.when(parent.getProperty("containedForm")).thenReturn(parentBlob);

			WebFormComponentChildType child = Mockito.mock(WebFormComponentChildType.class, Mockito.CALLS_REAL_METHODS);

			Field pathField = WebFormComponentChildType.class.getDeclaredField("fcCompAndPropPath");
			pathField.setAccessible(true);
			pathField.set(child, new String[] { "7C783D6E-8E26-40B9-8BDA-E2DC4F2ECDF8", "containedForm", "n1" });

			Field parentField = com.servoy.j2db.persistence.AbstractBase.class.getDeclaredField("parent");
			parentField.setAccessible(true);
			parentField.set(child, parent);

			return child;
		}

		private JSONObject invokeGetJson(WebFormComponentChildType child, boolean forMutation, boolean flattened)
				throws Exception {
			Method getJson = WebFormComponentChildType.class.getDeclaredMethod("getJson", boolean.class, boolean.class);
			getJson.setAccessible(true);
			return (JSONObject) getJson.invoke(child, Boolean.valueOf(forMutation), Boolean.valueOf(flattened));
		}

		@Test
		@DisplayName("flattened read converts legacy string customProperties to a json object with attributes.data-Target")
		void flattenedReadConvertsLegacyStringToObject() throws Exception {
			JSONObject childBlob = new JSONObject();
			childBlob.put(CUSTOM_PROPERTIES, LEGACY_CUSTOM_PROPERTIES);
			JSONObject parentBlob = new JSONObject();
			parentBlob.put("n1", childBlob);

			WebFormComponentChildType child = newChildOnParentBlob(parentBlob);
			JSONObject json = invokeGetJson(child, false, true);

			Object customProperties = json.opt(CUSTOM_PROPERTIES);
			assertAll(() -> assertNotNull(json, "flattened json blob for the child must not be null"),
					() -> assertFalse(customProperties instanceof String,
							"legacy string customProperties must NOT be left as a String on the flattened read"),
					() -> assertInstanceOf(JSONObject.class, customProperties,
							"customProperties must be normalized to a json object on the flattened read"),
					() -> assertEquals("dashboard-health",
							((JSONObject) customProperties).getJSONObject(IContentSpecConstants.PROPERTY_ATTRIBUTES)
									.getString("data-Target"),
							"nested attributes.data-Target must survive the normalization"));
		}

		@Test
		@DisplayName("normalized customProperties is delivered by BaseComponent.getAttributes as a non-empty map")
		void normalizedCustomPropertiesReachesGetAttributes() throws Exception {
			JSONObject childBlob = new JSONObject();
			childBlob.put(CUSTOM_PROPERTIES, LEGACY_CUSTOM_PROPERTIES);
			JSONObject parentBlob = new JSONObject();
			parentBlob.put("n1", childBlob);

			WebFormComponentChildType child = newChildOnParentBlob(parentBlob);
			JSONObject json = invokeGetJson(child, false, true);
			JSONObject normalized = (JSONObject) json.opt(CUSTOM_PROPERTIES);

			// feed the normalized customProperties into a real BaseComponent and read it
			// back the
			// exact way onAction does: getAttributes() ->
			// getCustomProperty(["attributes"]).
			TestableBean bean = new TestableBean();
			bean.setCustomProperties(
					new ServoyJSONObject(normalized, ServoyJSONObject.getNames(normalized), false, true));

			assertAll(() -> assertFalse(bean.getAttributes().isEmpty(), "getAttributes() must be a non-empty map"),
					() -> assertTrue(bean.getAttributes().containsKey("data-Target"),
							"getAttributes() must contain the data-Target key"),
					() -> assertEquals("dashboard-health", bean.getAttributes().get("data-Target"),
							"getAttributes() must deliver the data-Target value"));
		}

		@Test
		@DisplayName("flattened read leaves an already-object customProperties unchanged (idempotent)")
		void flattenedReadIsIdempotentForObjectValue() throws Exception {
			JSONObject attributes = new JSONObject();
			attributes.put("data-Target", "dashboard-status");
			JSONObject alreadyObject = new JSONObject();
			alreadyObject.put(IContentSpecConstants.PROPERTY_ATTRIBUTES, attributes);
			JSONObject childBlob = new JSONObject();
			childBlob.put(CUSTOM_PROPERTIES, alreadyObject);
			JSONObject parentBlob = new JSONObject();
			parentBlob.put("n1", childBlob);

			WebFormComponentChildType child = newChildOnParentBlob(parentBlob);
			JSONObject json = invokeGetJson(child, false, true);

			Object customProperties = json.opt(CUSTOM_PROPERTIES);
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
		@DisplayName("plain non-mutation non-flattened read does not mutate the shared parent json (regression guard)")
		void plainReadDoesNotMutateSharedParentJson() throws Exception {
			JSONObject childBlob = new JSONObject();
			childBlob.put(CUSTOM_PROPERTIES, LEGACY_CUSTOM_PROPERTIES);
			JSONObject parentBlob = new JSONObject();
			parentBlob.put("n1", childBlob);

			WebFormComponentChildType child = newChildOnParentBlob(parentBlob);
			JSONObject json = invokeGetJson(child, false, false);

			assertAll(
					() -> assertInstanceOf(String.class, json.opt(CUSTOM_PROPERTIES),
							"the non-flattened non-mutation read must NOT normalize (guard against over-widening)"),
					() -> assertInstanceOf(String.class, childBlob.opt(CUSTOM_PROPERTIES),
							"the shared parent json blob must be left untouched by a plain read"));
		}

		/**
		 * A BaseComponent with a public no-arg constructor so a real getAttributes()
		 * can be exercised without a workspace/repository (the Bean constructors are
		 * protected).
		 */
		static class TestableBean extends Bean {
			TestableBean() {
				super(null, UUID.randomUUID());
			}
		}
	}

	/**
	 * SVY-21714 - the parent's root form-component property can hold a bare
	 * form-name/UUID String in a hand-written .frm. WebFormComponentChildType used
	 * to cast it to JSONObject in initializeElement() (run by the constructor) and
	 * in getJson(...), so building one from the Outline, PersistFinder or the tab
	 * sequence page threw a ClassCastException.
	 */
	@Nested
	class MalformedRootFormComponentValue {
		private static final String ROOT_PROPERTY = "containedForm";

		private WebComponent parent;

		private WebFormComponentChildType newChildOnParentValue(Object rootValue) throws Exception {
			parent = Mockito.mock(WebComponent.class);
			Mockito.when(parent.getProperty(ROOT_PROPERTY)).thenReturn(rootValue);
			Mockito.when(parent.getName()).thenReturn("fc1");

			WebFormComponentChildType child = Mockito.mock(WebFormComponentChildType.class, Mockito.CALLS_REAL_METHODS);

			Field pathField = WebFormComponentChildType.class.getDeclaredField("fcCompAndPropPath");
			pathField.setAccessible(true);
			pathField.set(child, new String[] { "7C783D6E-8E26-40B9-8BDA-E2DC4F2ECDF8", ROOT_PROPERTY, "n1" });

			Field parentField = com.servoy.j2db.persistence.AbstractBase.class.getDeclaredField("parent");
			parentField.setAccessible(true);
			parentField.set(child, parent);

			return child;
		}

		private Object invokeInitializeElement(WebFormComponentChildType child) throws Exception {
			Method initializeElement = WebFormComponentChildType.class.getDeclaredMethod("initializeElement");
			initializeElement.setAccessible(true);
			return initializeElement.invoke(child);
		}

		private JSONObject invokeGetJson(WebFormComponentChildType child, boolean forMutation, boolean flattened)
				throws Exception {
			Method getJson = WebFormComponentChildType.class.getDeclaredMethod("getJson", boolean.class, boolean.class);
			getJson.setAccessible(true);
			return (JSONObject) getJson.invoke(child, Boolean.valueOf(forMutation), Boolean.valueOf(flattened));
		}

		@Test
		@DisplayName("initializeElement on a bare-string root value returns null instead of throwing ClassCastException")
		void initializeElementRejectsBareString() throws Exception {
			WebFormComponentChildType child = newChildOnParentValue("orders_fc");

			Object result = assertDoesNotThrow(() -> invokeInitializeElement(child));

			assertNull(result, "a malformed root value must yield no child property description");
		}

		@Test
		@DisplayName("initializeElement on a missing root value returns null instead of throwing")
		void initializeElementToleratesMissingValue() throws Exception {
			WebFormComponentChildType child = newChildOnParentValue(null);

			Object result = assertDoesNotThrow(() -> invokeInitializeElement(child));

			assertNull(result, "a missing root value must yield no child property description");
		}

		@Test
		@DisplayName("plain read on a bare-string root value returns an empty json instead of throwing ClassCastException")
		void plainReadRejectsBareString() throws Exception {
			WebFormComponentChildType child = newChildOnParentValue("orders_fc");

			JSONObject json = assertDoesNotThrow(() -> invokeGetJson(child, false, false));

			assertAll(() -> assertNotNull(json, "getJson must never return null"),
					() -> assertEquals(0, json.length(), "a malformed root value must read as an empty json"));
		}

		@Test
		@DisplayName("mutation read on a bare-string inherited root value returns an empty json and does not rewrite the parent")
		void mutationReadRejectsBareStringWithoutTouchingParent() throws Exception {
			WebFormComponentChildType child = newChildOnParentValue("orders_fc");

			JSONObject json = assertDoesNotThrow(() -> invokeGetJson(child, true, false));

			assertAll(() -> assertEquals(0, json.length(), "a malformed root value must read as an empty json"),
					() -> Mockito.verify(parent, Mockito.never()).setProperty(Mockito.eq(ROOT_PROPERTY),
							Mockito.any()));
		}

		@Test
		@DisplayName("mutation read on a bare string inside the parent's own json returns an empty json instead of throwing JSONException")
		void mutationReadRejectsBareStringInOwnJson() throws Exception {
			WebFormComponentChildType child = newChildOnParentValue("orders_fc");
			JSONObject ownJson = new JSONObject();
			ownJson.put(ROOT_PROPERTY, "orders_fc");
			java.util.Map<String, Object> propertiesMap = new java.util.HashMap<>();
			propertiesMap.put(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName(), ownJson);
			Mockito.when(parent.getPropertiesMap()).thenReturn(propertiesMap);

			JSONObject json = assertDoesNotThrow(() -> invokeGetJson(child, true, false));

			assertAll(() -> assertEquals(0, json.length(), "a malformed root value must read as an empty json"),
					() -> assertEquals("orders_fc", ownJson.get(ROOT_PROPERTY),
							"the parent's own json must be left untouched"));
		}

		@Test
		@DisplayName("setProperty on a bare-string root value is refused: the form is not flagged as changed and the parent is not rewritten")
		void setPropertyRefusedOnBareString() throws Exception {
			WebFormComponentChildType child = newChildOnParentValue("orders_fc");

			assertDoesNotThrow(() -> child.setProperty("text", "hello"));

			assertAll(() -> Mockito.verify(parent, Mockito.never()).flagChanged(), () -> Mockito
					.verify(parent, Mockito.never()).setProperty(Mockito.eq(ROOT_PROPERTY), Mockito.any()));
		}

		@Test
		@DisplayName("removeProperty on a bare-string root value is refused: the form is not flagged as changed")
		void removePropertyRefusedOnBareString() throws Exception {
			WebFormComponentChildType child = newChildOnParentValue("orders_fc");

			assertDoesNotThrow(() -> child.removeProperty());

			Mockito.verify(parent, Mockito.never()).flagChanged();
		}

		@Test
		@DisplayName("clearProperty on a bare-string root value is refused: the form is not flagged as changed")
		void clearPropertyRefusedOnBareString() throws Exception {
			WebFormComponentChildType child = newChildOnParentValue("orders_fc");

			assertDoesNotThrow(() -> child.clearProperty("text"));

			Mockito.verify(parent, Mockito.never()).flagChanged();
		}

		@Test
		@DisplayName("clearCustomProperty on a bare-string root value is refused: the form is not flagged as changed")
		void clearCustomPropertyRefusedOnBareString() throws Exception {
			WebFormComponentChildType child = newChildOnParentValue("orders_fc");

			assertDoesNotThrow(() -> child.clearCustomProperty("styles", "color", -1));

			Mockito.verify(parent, Mockito.never()).flagChanged();
		}

		@Test
		@DisplayName("setProperty on a valid root value writes into the parent's json and flags the form as changed")
		void setPropertyWritesIntoParentOnValidValue() throws Exception {
			JSONObject rootValue = new JSONObject();
			rootValue.put("svy_form", "7C783D6E-8E26-40B9-8BDA-E2DC4F2ECDF9");
			WebFormComponentChildType child = newChildOnParentValue(rootValue);

			child.setProperty("text", "hello");

			org.mockito.ArgumentCaptor<Object> written = org.mockito.ArgumentCaptor.forClass(Object.class);
			Mockito.verify(parent).setProperty(Mockito.eq(ROOT_PROPERTY), written.capture());
			JSONObject writtenValue = assertInstanceOf(JSONObject.class, written.getValue(),
					"the parent must receive a json object for its form component property");
			assertAll(
					() -> assertEquals("hello", writtenValue.getJSONObject("n1").getString("text"),
							"the edit must land in the child's json inside the parent's value"),
					() -> Mockito.verify(parent).flagChanged());
		}

		@Test
		@DisplayName("plain read on a valid root value still returns the child's json (pass-through)")
		void plainReadPassesThroughValidValue() throws Exception {
			JSONObject childBlob = new JSONObject();
			childBlob.put("text", "hello");
			JSONObject rootValue = new JSONObject();
			rootValue.put("svy_form", "7C783D6E-8E26-40B9-8BDA-E2DC4F2ECDF9");
			rootValue.put("n1", childBlob);
			WebFormComponentChildType child = newChildOnParentValue(rootValue);

			JSONObject json = invokeGetJson(child, false, false);

			assertSame(childBlob, json, "a valid root value must resolve to the child's own json object");
		}
	}
}
