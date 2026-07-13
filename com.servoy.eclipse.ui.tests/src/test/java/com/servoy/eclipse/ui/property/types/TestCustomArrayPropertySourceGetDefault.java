package com.servoy.eclipse.ui.property.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
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

	@Before
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

		assertNotNull("should not return null for custom type array element", result);
		assertTrue("should return a WebCustomType instance", result instanceof WebCustomType);
		assertEquals("should set the correct type name", "tab", ((WebCustomType) result).getTypeName());
		assertEquals("parent should be the webComponent", webComponent, ((WebCustomType) result).getParent());
	}

	@Test
	public void testGetDefaultElementPropertyReturnsDistinctInstancesForDifferentIndices() {
		ComplexProperty<Object> complexProperty = new ComplexProperty<>(new Object[] { null, null, null });
		CustomArrayPropertySource source = controller.new CustomArrayPropertySource(complexProperty);

		ArrayPropertyChildId id0 = new ArrayPropertyChildId(TABS_PROPERTY, 0);
		ArrayPropertyChildId id2 = new ArrayPropertyChildId(TABS_PROPERTY, 2);
		Object result0 = source.getDefaultElementProperty(id0);
		Object result2 = source.getDefaultElementProperty(id2);

		assertNotNull("result at index 0 should not be null", result0);
		assertNotNull("result at index 2 should not be null", result2);
		assertTrue("result at index 0 should be WebCustomType", result0 instanceof WebCustomType);
		assertTrue("result at index 2 should be WebCustomType", result2 instanceof WebCustomType);
		assertTrue("results should be distinct instances", result0 != result2);
	}

	@Test
	public void testGetDefaultElementPropertyReturnsNullForNonArrayChildId() {
		ComplexProperty<Object> complexProperty = new ComplexProperty<>(new Object[] { null });
		CustomArrayPropertySource source = controller.new CustomArrayPropertySource(complexProperty);

		Object result = source.getDefaultElementProperty("someStringId");

		assertNull("should return null when id is not ArrayPropertyChildId", result);
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

		assertEquals("should return the declared default value", defaultValue, result);
	}

	@Test
	public void testDefaultResetPropertySetsWebCustomTypeIntoArray() {
		Object[] array = new Object[] { "existing0", "existing1", "existing2" };
		ComplexProperty<Object> complexProperty = new ComplexProperty<>(array);
		CustomArrayPropertySource source = controller.new CustomArrayPropertySource(complexProperty);

		ArrayPropertyChildId id = new ArrayPropertyChildId(TABS_PROPERTY, 1);
		source.defaultResetProperty(id);

		assertNotNull("array element should not be null after reset", array[1]);
		assertTrue("array element should be a WebCustomType after reset", array[1] instanceof WebCustomType);
	}

	@Test
	public void testDefaultResetPropertyAtIndex0() {
		Object[] array = new Object[] { "existing0", "existing1" };
		ComplexProperty<Object> complexProperty = new ComplexProperty<>(array);
		CustomArrayPropertySource source = controller.new CustomArrayPropertySource(complexProperty);

		ArrayPropertyChildId id = new ArrayPropertyChildId(TABS_PROPERTY, 0);
		source.defaultResetProperty(id);

		assertNotNull("array element at index 0 should not be null after reset", array[0]);
		assertTrue("array element at index 0 should be WebCustomType", array[0] instanceof WebCustomType);
		assertEquals("existing1 should remain unchanged", "existing1", array[1]);
	}

	@Test
	public void testWholeArrayResetReturnsNullForNonArrayChildId() {
		ComplexProperty<Object> complexProperty = new ComplexProperty<>(new Object[] { "tab0", "tab1", "tab2" });
		CustomArrayPropertySource source = controller.new CustomArrayPropertySource(complexProperty);

		Object result = source.getDefaultElementProperty(TABS_PROPERTY);

		assertNull("whole-array reset should return null for non-ArrayPropertyChildId", result);
	}

	@Test
	public void testDefaultResetPropertyReplacesValueNotMutates() {
		Object originalValue = "originalTabValue";
		Object[] array = new Object[] { originalValue, "other" };
		ComplexProperty<Object> complexProperty = new ComplexProperty<>(array);
		CustomArrayPropertySource source = controller.new CustomArrayPropertySource(complexProperty);

		ArrayPropertyChildId id = new ArrayPropertyChildId(TABS_PROPERTY, 0);
		source.defaultResetProperty(id);

		assertTrue("reset element should be a WebCustomType", array[0] instanceof WebCustomType);
		assertTrue("reset value should differ from original (replacement, not mutation)",
				!originalValue.equals(array[0]));
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
