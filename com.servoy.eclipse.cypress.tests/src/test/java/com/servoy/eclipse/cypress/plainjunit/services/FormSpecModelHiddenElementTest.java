package com.servoy.eclipse.cypress.plainjunit.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.servoy.eclipse.cypress.services.FormSpecModel;
import com.servoy.eclipse.cypress.services.FormSpecModel.Effect;
import com.servoy.eclipse.cypress.services.FormSpecModel.EffectKind;
import com.servoy.eclipse.cypress.services.FormSpecModel.Element;
import com.servoy.eclipse.cypress.services.FormSpecModel.Gesture;
import com.servoy.eclipse.cypress.services.FormSpecModel.Handler;
import com.servoy.eclipse.cypress.services.FormSpecModel.Kind;

/**
 * SVY-21517: an element that is hidden in the form (visible=false) is not in the page, so it can be neither asserted visible nor clicked. The
 * baseline leaves it out and its handlers become skipped cases.
 */
class FormSpecModelHiddenElementTest
{
	private static final String URL = "solution/mySolution/index.html?formpreview=myForm&svy_testmode=true";
	private static final String OVERLAY = "    cy.get('.svy-error, .error-overlay').should('not.exist');";
	private static final String BTN = "bootstrapcomponents-button";

	private static Handler click(String method, Effect... effects)
	{
		return new Handler("onActionMethodID", method, "forms.myForm", Gesture.CLICK, false, false, List.of(effects));
	}

	private static Element shown(String name, Handler... handlers)
	{
		return new Element(name, BTN, Kind.CONTROL, false, true, List.of(handlers));
	}

	private static Element hidden(String name, Handler... handlers)
	{
		return new Element(name, BTN, Kind.CONTROL, false, false, List.of(handlers));
	}

	private static FormSpecModel model(Element... elements)
	{
		return new FormSpecModel("myForm", List.of(elements));
	}

	private static String emit(FormSpecModel model)
	{
		return FormSpecModelEmitterAccess.emit(URL, model);
	}

	@Test
	void elementWithoutTheVisibleFlagIsVisible()
	{
		assertTrue(new Element("a", BTN, Kind.CONTROL, false, List.of()).visible());
	}

	@Test
	void hiddenElementIsLeftOutOfTheBaselineAndItsHandlerIsASkippedCase()
	{
		String expected = String.join("\n", "describe('myForm', () => {", "", "  beforeEach(() => {", "    cy.visit('" + URL + "');",
			"    cy.get('[data-cy^=\"myForm.\"]', { timeout: 30000 }).should('exist');", "  });", "",
			"  it('loads without errors and all elements are visible', () => {", OVERLAY,
			"    cy.get('[data-cy=\"myForm.btn\"]').should('be.visible');", "  });", "",
			"  // btn.onActionMethodID -> forms.myForm.hide", "  it('btn: onActionMethodID (hide)', () => {",
			"    cy.get('[data-cy=\"myForm.btn\"]').click();", OVERLAY, "  });", "",
			"  it.skip('secret: onActionMethodID (element is hidden by design)', () => {});", "", "});", "");

		assertEquals(expected, emit(model(shown("btn", click("hide")), hidden("secret", click("reveal")))));
	}

	@Test
	void hiddenElementIsNeverClickedOrAsserted()
	{
		String output = emit(model(hidden("secret", click("reveal", new Effect(EffectKind.VISIBLE, "secret", "true")))));

		assertAll(() -> assertFalse(output.contains("myForm.secret"), "a hidden element must not be selected at all: " + output),
			() -> assertFalse(output.contains(".click()"), "a hidden element must not be clicked: " + output));
	}

	@Test
	void hiddenWinsOverNoGestureAndUnresolved()
	{
		Handler noGesture = new Handler("onCellClick", "m", "forms.myForm", Gesture.NONE, false, false, List.of());
		Handler unresolved = new Handler("onActionMethodID", null, null, Gesture.CLICK, true, false, List.of());

		String output = emit(model(hidden("secret", noGesture, unresolved)));

		assertAll(() -> assertTrue(output.contains("  it.skip('secret: onActionMethodID (element is hidden by design)', () => {});\n")),
			() -> assertTrue(output.contains("  it.skip('secret: onCellClick (element is hidden by design)', () => {});\n")),
			() -> assertFalse(output.contains("no gesture defined")), () -> assertFalse(output.contains("could not be resolved")));
	}

	@Test
	void anEffectOnAHiddenElementFromAVisibleButtonIsStillAsserted()
	{
		// the button is shown, and after the click it makes the hidden element visible: that is exactly what the handler does
		String output = emit(model(shown("btn", click("show", new Effect(EffectKind.VISIBLE, "secret", "true"))), hidden("secret")));

		assertTrue(output.contains("    cy.get('[data-cy=\"myForm.secret\"]').should('be.visible');\n"), output);
	}

	@Test
	void hiddenElementIsNotCountedAsAVisibilityAssertionButItsHandlerIsStillCounted()
	{
		FormSpecModel model = model(shown("btn", click("hide")), hidden("secret", click("reveal")));

		assertAll(() -> assertEquals(1, FormSpecModelEmitterAccess.countVisibilityAssertions(model)),
			() -> assertEquals(2, FormSpecModelEmitterAccess.countHandlerCases(model)));
	}

	@Test
	void formWithOnlyHiddenElementsStillWaitsForItsDataCyElements()
	{
		String output = emit(model(hidden("secret")));

		assertAll(() -> assertTrue(output.contains("cy.get('[data-cy^=\"myForm.\"]', { timeout: 30000 }).should('exist');")),
			() -> assertFalse(output.contains("should('be.visible')")));
	}
}
