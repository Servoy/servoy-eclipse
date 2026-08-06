package com.servoy.eclipse.designer.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.j2db.persistence.CSSPositionLayoutContainer;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.util.UUID;

class DesignerUtilGetLayoutContainerAsStringTest {

	private LayoutContainer createLayoutContainer() throws Exception {
		Constructor<LayoutContainer> ctor = LayoutContainer.class
				.getDeclaredConstructor(com.servoy.j2db.persistence.ISupportChilds.class, UUID.class);
		ctor.setAccessible(true);
		return ctor.newInstance(null, UUID.randomUUID());
	}

	private CSSPositionLayoutContainer createCSSPositionLayoutContainer() throws Exception {
		Constructor<CSSPositionLayoutContainer> ctor = CSSPositionLayoutContainer.class
				.getDeclaredConstructor(com.servoy.j2db.persistence.ISupportChilds.class, UUID.class);
		ctor.setAccessible(true);
		return ctor.newInstance(null, UUID.randomUUID());
	}

	@Nested
	class BasicLayoutContainer {

		private LayoutContainer container;

		@BeforeEach
		void setUp() throws Exception {
			container = createLayoutContainer();
		}

		@Test
		@DisplayName("default tag type is div with no attributes")
		void defaultTagType() {
			String result = DesignerUtil.getLayoutContainerAsString(container);
			assertEquals("<div>", result);
		}

		@Test
		@DisplayName("includes class attribute after property change")
		void includesClassAttribute() {
			container.setCssClasses("col-md-6");
			String result = DesignerUtil.getLayoutContainerAsString(container);
			assertTrue(result.contains("class=\"col-md-6\""));
			assertTrue(result.startsWith("<div"));
			assertTrue(result.endsWith(">"));
		}

		@Test
		@DisplayName("reflects updated class attribute value after resize")
		void reflectsUpdatedClassValue() {
			container.setCssClasses("col-md-6");
			String before = DesignerUtil.getLayoutContainerAsString(container);
			assertTrue(before.contains("col-md-6"));

			container.setCssClasses("col-md-8");
			String after = DesignerUtil.getLayoutContainerAsString(container);
			assertTrue(after.contains("col-md-8"));
			assertFalse(after.contains("col-md-6"));
		}

		@Test
		@DisplayName("includes custom tag type")
		void customTagType() {
			container.setTagType("section");
			container.setCssClasses("row");
			String result = DesignerUtil.getLayoutContainerAsString(container);
			assertTrue(result.startsWith("<section"));
			assertTrue(result.contains("class=\"row\""));
		}

		@Test
		@DisplayName("includes name in brackets when set")
		void includesNameWhenSet() {
			container.setName("myColumn");
			String result = DesignerUtil.getLayoutContainerAsString(container);
			assertTrue(result.endsWith("[myColumn]"));
		}

		@Test
		@DisplayName("no name brackets when name is null")
		void noNameBracketsWhenNull() {
			String result = DesignerUtil.getLayoutContainerAsString(container);
			assertFalse(result.contains("["));
			assertFalse(result.contains("]"));
		}

		@Test
		@DisplayName("multiple attributes are included")
		void multipleAttributes() {
			container.putAttribute("class", "col-md-4");
			container.putAttribute("data-testid", "col1");
			String result = DesignerUtil.getLayoutContainerAsString(container);
			assertTrue(result.contains("class=\"col-md-4\""));
			assertTrue(result.contains("data-testid=\"col1\""));
		}
	}

	@Nested
	class CSSPositionContainer {

		private CSSPositionLayoutContainer container;

		@BeforeEach
		void setUp() throws Exception {
			container = createCSSPositionLayoutContainer();
		}

		@Test
		@DisplayName("includes ResponsiveContainer marker")
		void includesResponsiveContainerMarker() {
			String result = DesignerUtil.getLayoutContainerAsString(container);
			assertTrue(result.contains("[ResponsiveContainer]"));
		}

		@Test
		@DisplayName("includes class and ResponsiveContainer marker together")
		void classAndResponsiveMarker() {
			container.setCssClasses("col-md-12");
			container.setName("responsiveCol");
			String result = DesignerUtil.getLayoutContainerAsString(container);
			assertTrue(result.contains("class=\"col-md-12\""));
			assertTrue(result.contains("[ResponsiveContainer]"));
			assertTrue(result.endsWith("[responsiveCol]"));
		}
	}
}
