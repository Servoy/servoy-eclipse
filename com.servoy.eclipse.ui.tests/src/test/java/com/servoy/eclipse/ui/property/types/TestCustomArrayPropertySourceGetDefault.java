package com.servoy.eclipse.ui.property.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.CustomJSONObjectType;
import org.sablo.specification.property.types.StringPropertyType;

import com.servoy.eclipse.ui.property.ArrayTypePropertyController.ArrayPropertyChildId;
import com.servoy.eclipse.ui.property.ComplexProperty;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.types.CustomArrayTypePropertyController.CustomArrayPropertySource;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.util.UUID;

/**
 * Tests for SVY-21056: Restore default value on tabpanel tab element should
 * create a default WebCustomType instance instead of inserting null into the
 * array.
 */
public class TestCustomArrayPropertySourceGetDefault {
	private static final String TABS_PROPERTY = "tabs";
	private static final String TAB_TYPE_NAME = "tabpanel.tab";

	private CustomJSONObjectType<Object, Object> tabObjectType;
	private CustomJSONArrayType<Object, Object> tabsArrayType;
	private PropertyDescription webComponentPD;
	private PropertyDescription componentSpec;
	private TestableWebComponent webComponent;
	private PersistContext persistContext;
	private CustomArrayTypePropertyController controller;

	@BeforeEach
	public void setUp() {
		tabObjectType = new CustomJSONObjectType<>(TAB_TYPE_NAME, null);
		PropertyDescription tabObjectDef = new PropertyDescriptionBuilder().withName(TAB_TYPE_NAME)
				.withType(StringPropertyType.INSTANCE).build();
		tabObjectType.setCustomJSONDefinition(tabObjectDef);

		PropertyDescription arrayElementPD = new PropertyDescriptionBuilder().withName("tab").withType(tabObjectType)
				.build();

		tabsArrayType = new CustomJSONArrayType<>(arrayElementPD);

		webComponentPD = new PropertyDescriptionBuilder().withName(TABS_PROPERTY).withType(tabsArrayType).build();

		PropertyDescription tabsPropInSpec = new PropertyDescriptionBuilder().withName(TABS_PROPERTY)
				.withType(tabsArrayType).build();

		componentSpec = new PropertyDescriptionBuilder().withName("testTabpanel").withType(StringPropertyType.INSTANCE)
				.withProperty(TABS_PROPERTY, tabsPropInSpec).build();

		webComponent = new TestableWebComponent(null, UUID.randomUUID(), componentSpec);
		persistContext = PersistContext.create(webComponent);

		controller = new CustomArrayTypePropertyController(TABS_PROPERTY, "Tabs", null, persistContext, webComponentPD);
	}

	@Test
	public void testGetDefaultElementPropertyReturnsWebCustomTypeForArrayChildId() {
		ComplexProperty<Object> complexProperty = new ComplexProperty<>(new Object[] { null, null, null });
		CustomArrayPropertySource source = controller.new CustomArrayPropertySource(complexProperty);

		ArrayPropertyChildId id = new ArrayPropertyChildId(TABS_PROPERTY, 1);
		Object result = source.getDefaultElementProperty(id);

		assertNotNull(result, "should not return null for custom type array element");
		assertTrue(result instanceof WebCustomType, "should return a WebCustomType instance");
		assertEquals("tab", ((WebCustomType) result).getTypeName(), "should set the correct type name");
		assertEquals(webComponent, ((WebCustomType) result).getParent(), "parent should be the webComponent");
	}

	@Test
	public void testGetDefaultElementPropertyReturnsDistinctInstancesForDifferentIndices() {
		ComplexProperty<Object> complexProperty = new ComplexProperty<>(new Object[] { null, null, null });
		CustomArrayPropertySource source = controller.new CustomArrayPropertySource(complexProperty);

		ArrayPropertyChildId id0 = new ArrayPropertyChildId(TABS_PROPERTY, 0);
		ArrayPropertyChildId id2 = new ArrayPropertyChildId(TABS_PROPERTY, 2);
		Object result0 = source.getDefaultElementProperty(id0);
		Object result2 = source.getDefaultElementProperty(id2);

		assertNotNull(result0, "result at index 0 should not be null");
		assertNotNull(result2, "result at index 2 should not be null");
		assertTrue(result0 instanceof WebCustomType, "result at index 0 should be WebCustomType");
		assertTrue(result2 instanceof WebCustomType, "result at index 2 should be WebCustomType");
		assertTrue(result0 != result2, "results should be distinct instances");
	}

	@Test
	public void testGetDefaultElementPropertyReturnsNullForNonArrayChildId() {
		ComplexProperty<Object> complexProperty = new ComplexProperty<>(new Object[] { null });
		CustomArrayPropertySource source = controller.new CustomArrayPropertySource(complexProperty);

		Object result = source.getDefaultElementProperty("someStringId");

		assertNull(result, "should return null when id is not ArrayPropertyChildId");
	}

	@Test
	public void testGetDefaultElementPropertyReturnsNonNullDefault() {
		Object defaultValue = "defaultTabValue";
		PropertyDescription elementPDWithDefault = new PropertyDescriptionBuilder().withName("tab")
				.withType(tabObjectType).withDefaultValue(defaultValue).build();

		CustomJSONArrayType<Object, Object> arrayTypeWithDefault = new CustomJSONArrayType<>(elementPDWithDefault);

		PropertyDescription pdWithDefault = new PropertyDescriptionBuilder().withName(TABS_PROPERTY)
				.withType(arrayTypeWithDefault).build();

		CustomArrayTypePropertyController controllerWithDefault = new CustomArrayTypePropertyController(TABS_PROPERTY,
				"Tabs", null, persistContext, pdWithDefault);

		ComplexProperty<Object> complexProperty = new ComplexProperty<>(new Object[] { null });
		CustomArrayPropertySource source = controllerWithDefault.new CustomArrayPropertySource(complexProperty);

		ArrayPropertyChildId id = new ArrayPropertyChildId(TABS_PROPERTY, 0);
		Object result = source.getDefaultElementProperty(id);

		assertEquals(defaultValue, result, "should return the declared default value");
	}

	@Test
	public void testDefaultResetPropertySetsWebCustomTypeIntoArray() {
		Object[] array = new Object[] { "existing0", "existing1", "existing2" };
		ComplexProperty<Object> complexProperty = new ComplexProperty<>(array);
		CustomArrayPropertySource source = controller.new CustomArrayPropertySource(complexProperty);

		ArrayPropertyChildId id = new ArrayPropertyChildId(TABS_PROPERTY, 1);
		source.defaultResetProperty(id);

		assertNotNull(array[1], "array element should not be null after reset");
		assertTrue(array[1] instanceof WebCustomType, "array element should be a WebCustomType after reset");
	}

	@Test
	public void testDefaultResetPropertyAtIndex0() {
		Object[] array = new Object[] { "existing0", "existing1" };
		ComplexProperty<Object> complexProperty = new ComplexProperty<>(array);
		CustomArrayPropertySource source = controller.new CustomArrayPropertySource(complexProperty);

		ArrayPropertyChildId id = new ArrayPropertyChildId(TABS_PROPERTY, 0);
		source.defaultResetProperty(id);

		assertNotNull(array[0], "array element at index 0 should not be null after reset");
		assertTrue(array[0] instanceof WebCustomType, "array element at index 0 should be WebCustomType");
		assertEquals("existing1", array[1], "existing1 should remain unchanged");
	}

	@Test
	public void testWholeArrayResetReturnsNullForNonArrayChildId() {
		ComplexProperty<Object> complexProperty = new ComplexProperty<>(new Object[] { "tab0", "tab1", "tab2" });
		CustomArrayPropertySource source = controller.new CustomArrayPropertySource(complexProperty);

		Object result = source.getDefaultElementProperty(TABS_PROPERTY);

		assertNull(result, "whole-array reset should return null for non-ArrayPropertyChildId");
	}

	@Test
	public void testDefaultResetPropertyReplacesValueNotMutates() {
		Object originalValue = "originalTabValue";
		Object[] array = new Object[] { originalValue, "other" };
		ComplexProperty<Object> complexProperty = new ComplexProperty<>(array);
		CustomArrayPropertySource source = controller.new CustomArrayPropertySource(complexProperty);

		ArrayPropertyChildId id = new ArrayPropertyChildId(TABS_PROPERTY, 0);
		source.defaultResetProperty(id);

		assertTrue(array[0] instanceof WebCustomType, "reset element should be a WebCustomType");
		assertTrue(!originalValue.equals(array[0]),
				"reset value should differ from original (replacement, not mutation)");
	}

	private static class TestableWebComponent extends WebComponent {
		private final PropertyDescription testPropertyDescription;

		TestableWebComponent(ISupportChilds parent, UUID uuid, PropertyDescription propertyDescription) {
			super(parent, uuid);
			this.testPropertyDescription = propertyDescription;
		}

		@Override
		public PropertyDescription getPropertyDescription() {
			return testPropertyDescription;
		}

		@Override
		protected void afterChildWasAdded(IPersist obj) {
			if (obj instanceof AbstractBase && this instanceof ISupportChilds) {
				((AbstractBase) obj).setParent((ISupportChilds) this);
			}
		}
	}
}
