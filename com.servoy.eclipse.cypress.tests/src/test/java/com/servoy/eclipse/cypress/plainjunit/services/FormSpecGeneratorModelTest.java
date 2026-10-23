package com.servoy.eclipse.cypress.plainjunit.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
	 * Characterisation of the unchanged legacy path (spec 4, step 1, item 2): the exact output of {@code parseFrmFile} followed by
	 * {@code generateCypressSpecContent} for a fixed .frm string. The model path must not alter it.
	 */
	@Nested
	class LegacyCharacterisation
	{
		private static final String FRM = "{\n\"dataSource\":\"db:/servoy_test/orders\",\n\"items\":[\n" +
			"{\n\"name\":\"btn_save\",\n\"onActionMethodID\":\"abc\",\n\"typeid\":7\n},\n" +
			"{\n\"name\":\"lbl_title\",\n\"typeid\":7\n},\n" +
			"{\n\"name\":\"txt_name\",\n\"typeName\":\"bootstrapcomponents-textbox\",\n\"typeid\":47\n}\n" +
			"],\n\"name\":\"orderForm\",\n\"typeid\":3\n}";

		private String legacyContent(String frm, String formName, String solutionName) throws Exception
		{
			FormSpecGenerator generator = new FormSpecGenerator();
			Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
			parseFrmFile.setAccessible(true);
			Method generate = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent", parseFrmFile.getReturnType());
			generate.setAccessible(true);

			Object metadata = parseFrmFile.invoke(generator, frm, formName);
			Field solution = metadata.getClass().getDeclaredField("solutionName");
			solution.setAccessible(true);
			solution.set(metadata, solutionName);
			return (String)generate.invoke(generator, metadata);
		}

		@Test
		@DisplayName("the legacy generator output for a fixed .frm is byte for byte the visibility-only spec")
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

			assertEquals(expected, legacyContent(FRM, "orderForm", "mySolution"));
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

			String frm = "{\n\"items\":[],\n\"name\":\"emptyForm\",\n\"typeid\":3\n}";

			assertAll(() -> assertEquals(expected, legacyContent(frm, "emptyForm", "mySolution")));
		}
	}
}
