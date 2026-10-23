package com.servoy.eclipse.cypress.plainjunit.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.cypress.services.FormSpecGenerator;
import com.servoy.eclipse.cypress.services.FormSpecModel;
import com.servoy.eclipse.cypress.services.FormSpecModel.Element;
import com.servoy.eclipse.cypress.services.FormSpecModel.Kind;

class FormSpecGeneratorModelTest
{
	private static FormSpecModel modelFor(String formName)
	{
		return new FormSpecModel(formName, List.of(new Element("btn", "bootstrapcomponents-button", Kind.CONTROL, false, List.of())));
	}

	@Nested
	class ModelOverload
	{
		@Test
		@DisplayName("generateSpec(String, FormSpecModel) is public and returns a String")
		void overloadExists() throws NoSuchMethodException
		{
			Method method = FormSpecGenerator.class.getMethod("generateSpec", String.class, FormSpecModel.class);

			assertEquals(String.class, method.getReturnType());
		}

		@Test
		@DisplayName("generateSpec(String) is still public")
		void legacyOverloadStays() throws NoSuchMethodException
		{
			assertNotNull(FormSpecGenerator.class.getMethod("generateSpec", String.class));
		}

		@Test
		@DisplayName("a model for another form is rejected before the workspace is touched")
		void modelNameMismatch_returnsErrorBeforeWorkspaceAccess()
		{
			assertEquals("Error: Model is for form 'b' but spec was requested for form 'a'", new FormSpecGenerator().generateSpec("a", modelFor("b")));
		}

		@Test
		@DisplayName("the mismatch check is case sensitive")
		void modelNameMismatch_isCaseSensitive()
		{
			assertEquals("Error: Model is for form 'Orders' but spec was requested for form 'orders'",
				new FormSpecGenerator().generateSpec("orders", modelFor("Orders")));
		}
	}

	/**
	 * Characterisation of the unchanged legacy path (spec 4, step 1, item 2): the exact output of {@code generateCypressSpecContent} for
	 * a fixed {@code FormMetadata}. The model path must not alter it. The metadata is built through reflection because since SVY-21514
	 * it is read from the {@code Form} persist by {@code buildMetadata(Form)}, which needs a live Servoy model.
	 */
	@Nested
	class LegacyCharacterisation
	{
		/** {name, typeName, isWebComponent, isButton, isLabel} */
		private final Object[][] ELEMENTS = { { "btn_save", null, false, true, false }, { "lbl_title", null, false, false, true },
			{ "txt_name", "bootstrapcomponents-textbox", true, false, false } };

		private void set(Object target, String fieldName, Object value) throws Exception
		{
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		}

		@SuppressWarnings("unchecked")
		private String legacyContent(Object[][] elements, String formName, String solutionName) throws Exception
		{
			FormSpecGenerator generator = new FormSpecGenerator();
			Class< ? > metadataClass = Class.forName("com.servoy.eclipse.cypress.services.FormSpecGenerator$FormMetadata");
			Class< ? > elementClass = Class.forName("com.servoy.eclipse.cypress.services.FormSpecGenerator$ElementInfo");
			Constructor< ? > metadataCtor = metadataClass.getDeclaredConstructor();
			metadataCtor.setAccessible(true);
			Constructor< ? > elementCtor = elementClass.getDeclaredConstructor();
			elementCtor.setAccessible(true);

			Object metadata = metadataCtor.newInstance();
			set(metadata, "formName", formName);
			set(metadata, "solutionName", solutionName);
			set(metadata, "dataSource", "db:/servoy_test/orders");
			Field namedElements = metadataClass.getDeclaredField("namedElements");
			namedElements.setAccessible(true);
			for (Object[] def : elements)
			{
				Object element = elementCtor.newInstance();
				set(element, "name", def[0]);
				set(element, "typeName", def[1]);
				set(element, "isWebComponent", def[2]);
				set(element, "isButton", def[3]);
				set(element, "isLabel", def[4]);
				((List<Object>)namedElements.get(metadata)).add(element);
			}

			Method generate = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent", metadataClass);
			generate.setAccessible(true);
			return (String)generate.invoke(generator, metadata);
		}

		@Test
		@DisplayName("the legacy generator output for fixed form metadata is byte for byte the visibility-only spec")
		void legacyOutput_isUnchanged() throws Exception
		{
			String expected = String.join("\n", "describe('orderForm', () => {", "", "  beforeEach(() => {",
				"    cy.visit('solution/mySolution/index.html?formpreview=orderForm&svy_testmode=true');",
				"    cy.get('[data-cy^=\"orderForm.\"', { timeout: 30000 }).should('exist');", "  });", "",
				"  it('loads without errors and all elements are visible', () => {",
				"    cy.get('.svy-error, .error-overlay').should('not.exist');",
				"    cy.get('[data-cy=\"orderForm.btn_save\"]').should('be.visible');",
				"    cy.get('[data-cy=\"orderForm.lbl_title\"]').should('be.visible');",
				"    cy.get('[data-cy=\"orderForm.txt_name\"]').should('be.visible');", "  });", "", "  it('buttons are clickable', () => {",
				"    cy.get('[data-cy=\"orderForm.btn_save\"]').should('be.visible').and('be.enabled');", "  });", "", "});") + "\n";

			assertEquals(expected, legacyContent(ELEMENTS, "orderForm", "mySolution"));
		}

		@Test
		@DisplayName("the legacy generator output for a form without elements waits on .svy-form")
		void legacyOutput_emptyForm() throws Exception
		{
			String expected = String.join("\n", "describe('emptyForm', () => {", "", "  beforeEach(() => {",
				"    cy.visit('solution/mySolution/index.html?formpreview=emptyForm&svy_testmode=true');",
				"    cy.get('.svy-form', { timeout: 30000 }).should('exist');", "  });", "",
				"  it('loads without errors and all elements are visible', () => {",
				"    cy.get('.svy-error, .error-overlay').should('not.exist');", "  });", "", "});") + "\n";

			assertAll(() -> assertEquals(expected, legacyContent(new Object[0][], "emptyForm", "mySolution")));
		}
	}
}
