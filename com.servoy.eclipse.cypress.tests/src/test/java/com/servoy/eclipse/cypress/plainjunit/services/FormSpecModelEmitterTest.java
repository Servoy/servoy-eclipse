package com.servoy.eclipse.cypress.plainjunit.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.servoy.eclipse.cypress.services.FormSpecModel;
import com.servoy.eclipse.cypress.services.FormSpecModel.Effect;
import com.servoy.eclipse.cypress.services.FormSpecModel.EffectKind;
import com.servoy.eclipse.cypress.services.FormSpecModel.Element;
import com.servoy.eclipse.cypress.services.FormSpecModel.Gesture;
import com.servoy.eclipse.cypress.services.FormSpecModel.Handler;
import com.servoy.eclipse.cypress.services.FormSpecModel.Kind;

class FormSpecModelEmitterTest
{
	private static final String FORM = "myForm";
	private static final String URL = "solution/mySolution/index.html?formpreview=myForm&svy_testmode=true";
	private static final String OVERLAY = "    cy.get('.svy-error, .error-overlay').should('not.exist');";
	private static final String FOOTER = "});\n";
	private static final String OFF_FORM_PREFIX = "    // OFF-FORM (not asserted): ";
	private static final String OUTER = "com.servoy.eclipse.cypress.plainjunit.services.FormSpecModelEmitterTest";

	// ------------------------------------------------------------ helpers

	private static String lines(String... values)
	{
		return String.join("\n", values) + "\n";
	}

	/** Lines followed by an empty line, which is how every case block ends. */
	private static String block(String... values)
	{
		return lines(values) + "\n";
	}

	private static String emit(FormSpecModel model)
	{
		return FormSpecModelEmitterAccess.emit(URL, model);
	}

	private static String prefix(String form, String... visibleElementNames)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("describe('").append(form).append("', () => {\n\n");
		sb.append("  beforeEach(() => {\n");
		sb.append("    cy.visit('").append(URL).append("');\n");
		sb.append("    cy.get('[data-cy^=\"").append(form).append(".\"]', { timeout: 30000 }).should('exist');\n");
		sb.append("  });\n\n");
		sb.append("  it('loads without errors and all elements are visible', () => {\n");
		sb.append(OVERLAY).append("\n");
		for (String name : visibleElementNames)
		{
			sb.append("    cy.get('[data-cy=\"").append(form).append(".").append(name).append("\"]').should('be.visible');\n");
		}
		sb.append("  });\n\n");
		return sb.toString();
	}

	private static Element element(String name, Kind kind, Handler... handlers)
	{
		return new Element(name, "bootstrapcomponents-button", kind, false, List.of(handlers));
	}

	private static Handler handler(String key, Gesture gesture, Effect... effects)
	{
		return new Handler(key, "hide", "forms.myForm", gesture, false, false, List.of(effects));
	}

	private static FormSpecModel model(Element... elements)
	{
		return new FormSpecModel(FORM, List.of(elements));
	}

	private static FormSpecModel singleButton(Gesture gesture, Effect... effects)
	{
		return model(element("btn", Kind.CONTROL, handler("onActionMethodID", gesture, effects)));
	}

	/** The expected text of the one handler case produced for {@link #singleButton}. */
	private static String expectedCase(String gestureCall, String... effectLines)
	{
		List<String> all = new ArrayList<>();
		all.add("  // btn.onActionMethodID -> forms.myForm.hide");
		all.add("  it('btn: onActionMethodID (hide)', () => {");
		all.add("    cy.get('[data-cy=\"myForm.btn\"]')" + gestureCall);
		all.addAll(List.of(effectLines));
		all.add(OVERLAY);
		all.add("  });");
		return block(all.toArray(String[]::new));
	}

	private static List<String> titleLines(String output)
	{
		return output.lines().filter(l -> l.startsWith("  it")).toList();
	}

	static Stream<Arguments> gestureRows()
	{
		return Stream.of(Arguments.of(Gesture.CLICK, ".click();"), Arguments.of(Gesture.RIGHT_CLICK, ".rightclick();"),
			Arguments.of(Gesture.CHANGE, ".clear().type('1').blur();"));
	}

	static Stream<Arguments> effectRows()
	{
		String lbl = "    cy.get('[data-cy=\"myForm.lbl\"]')";
		return Stream.of(Arguments.of(new Effect(EffectKind.VISIBLE, "lbl", "true"), lbl + ".should('be.visible');"),
			Arguments.of(new Effect(EffectKind.VISIBLE, "lbl", "false"), lbl + ".should('not.be.visible');"),
			Arguments.of(new Effect(EffectKind.ENABLED, "lbl", "true"), lbl + ".should('be.enabled');"),
			Arguments.of(new Effect(EffectKind.ENABLED, "lbl", "false"), lbl + ".should('be.disabled');"),
			Arguments.of(new Effect(EffectKind.ADD_STYLE_CLASS, "lbl", "active"), lbl + ".should('have.class', 'active');"),
			Arguments.of(new Effect(EffectKind.REMOVE_STYLE_CLASS, "lbl", "active"), lbl + ".should('not.have.class', 'active');"),
			Arguments.of(new Effect(EffectKind.TEXT, "lbl", "Hi"), lbl + ".should('contain.text', 'Hi');"),
			Arguments.of(new Effect(EffectKind.TEXT, "lbl", "it's"), lbl + ".should('contain.text', 'it\\'s');"),
			Arguments.of(new Effect(EffectKind.TEXT, "lbl", null), OFF_FORM_PREFIX + "elements.lbl.text = <empty>"),
			Arguments.of(new Effect(EffectKind.TEXT, "lbl", ""), OFF_FORM_PREFIX + "elements.lbl.text = <empty>"),
			Arguments.of(new Effect(EffectKind.TEXT, "lbl", "   "), OFF_FORM_PREFIX + "elements.lbl.text = <empty>"),
			Arguments.of(new Effect(EffectKind.OFF_FORM, null, "scopes.orders.load(id)"), OFF_FORM_PREFIX + "scopes.orders.load(id)"),
			Arguments.of(new Effect(EffectKind.OFF_FORM, null, "scopes.orders.load(id);"), OFF_FORM_PREFIX + "scopes.orders.load(id)"));
	}

	// ------------------------------------------------------------ baseline

	@Nested
	class Baseline
	{
		@Test
		@DisplayName("two asserted elements, a static and a container element and no handlers produce exactly the baseline")
		void golden_baselineOnly()
		{
			FormSpecModel model = model(element("txt", Kind.INPUT), element("lbl", Kind.STATIC), element("btn", Kind.CONTROL),
				element("box", Kind.CONTAINER));

			String expected = lines("describe('myForm', () => {", "", "  beforeEach(() => {", "    cy.visit('" + URL + "');",
				"    cy.get('[data-cy^=\"myForm.\"]', { timeout: 30000 }).should('exist');", "  });", "",
				"  it('loads without errors and all elements are visible', () => {", OVERLAY,
				"    cy.get('[data-cy=\"myForm.btn\"]').should('be.visible');", "    cy.get('[data-cy=\"myForm.txt\"]').should('be.visible');",
				"  });", "", "});");

			assertEquals(expected, emit(model));
		}

		@Test
		@DisplayName("a model without elements waits on .svy-form and only checks the error overlay")
		void golden_emptyElementList()
		{
			String expected = lines("describe('myForm', () => {", "", "  beforeEach(() => {", "    cy.visit('" + URL + "');",
				"    cy.get('.svy-form', { timeout: 30000 }).should('exist');", "  });", "",
				"  it('loads without errors and all elements are visible', () => {", OVERLAY, "  });", "", "});");

			assertEquals(expected, emit(model()));
		}

		@Test
		@DisplayName("static and container elements are not asserted visible")
		void staticAndContainer_notAsserted()
		{
			assertEquals(prefix(FORM) + FOOTER, emit(model(element("lbl", Kind.STATIC), element("box", Kind.CONTAINER))));
		}

		@Test
		@DisplayName("a quote in the form name is escaped in describe and in the wait selector")
		void formName_isEscaped()
		{
			List<String> out = emit(new FormSpecModel("my'Form", List.of(element("btn", Kind.CONTROL)))).lines().toList();

			assertAll(() -> assertEquals("describe('my\\'Form', () => {", out.get(0)),
				() -> assertEquals("    cy.get('[data-cy^=\"my\\'Form.\"]', { timeout: 30000 }).should('exist');", out.get(4)));
		}

		@Test
		@DisplayName("a quote and a newline in the url are escaped in cy.visit")
		void url_isEscaped()
		{
			List<String> out = FormSpecModelEmitterAccess.emit("a'b\nc", model(element("btn", Kind.CONTROL))).lines().toList();

			assertEquals("    cy.visit('a\\'b\\nc');", out.get(3));
		}
	}

	// ------------------------------------------------------------ gestures

	@Nested
	class Gestures
	{
		@ParameterizedTest
		@MethodSource(OUTER + "#gestureRows")
		@DisplayName("each gesture emits its call, then the effect, then the error overlay check as the last line")
		void gesture_emitsCall(Gesture gesture, String call)
		{
			FormSpecModel model = singleButton(gesture, new Effect(EffectKind.VISIBLE, "lbl", "false"));

			String expected = prefix(FORM, "btn") +
				expectedCase(call, "    cy.get('[data-cy=\"myForm.lbl\"]').should('not.be.visible');") + FOOTER;

			assertEquals(expected, emit(model));
		}

		@Test
		@DisplayName("a click case with a visible=false effect is exactly click, not.be.visible, error overlay")
		void click_withVisibleFalse()
		{
			FormSpecModel model = model(element("btn", Kind.CONTROL, handler("onActionMethodID", Gesture.CLICK,
				new Effect(EffectKind.VISIBLE, "lbl", "false"))), element("lbl", Kind.STATIC));

			String expected = prefix(FORM, "btn") + block("  // btn.onActionMethodID -> forms.myForm.hide",
				"  it('btn: onActionMethodID (hide)', () => {", "    cy.get('[data-cy=\"myForm.btn\"]').click();",
				"    cy.get('[data-cy=\"myForm.lbl\"]').should('not.be.visible');", OVERLAY, "  });") + FOOTER;

			assertEquals(expected, emit(model));
		}

		@Test
		@DisplayName("a handler without effects is the gesture followed by the error overlay check")
		void noEffects()
		{
			assertEquals(prefix(FORM, "btn") + expectedCase(".click();") + FOOTER, emit(singleButton(Gesture.CLICK)));
		}
	}

	// ------------------------------------------------------------ effects

	@Nested
	class Effects
	{
		@ParameterizedTest
		@MethodSource(OUTER + "#effectRows")
		@DisplayName("each effect row of the spec table produces exactly its line")
		void effect_emitsLine(Effect effect, String expectedLine)
		{
			assertEquals(prefix(FORM, "btn") + expectedCase(".click();", expectedLine) + FOOTER, emit(singleButton(Gesture.CLICK, effect)));
		}

		@Test
		@DisplayName("an off-form effect is a comment only and produces no cy.get for it")
		void offForm_hasNoCyGet()
		{
			String out = emit(singleButton(Gesture.CLICK, new Effect(EffectKind.OFF_FORM, null, "scopes.orders.load(id)")));

			assertAll(() -> assertEquals(1, out.lines().filter(l -> l.equals("    // OFF-FORM (not asserted): scopes.orders.load(id)")).count()),
				() -> assertEquals(1,
					out.lines().filter(l -> l.contains("cy.get('[data-cy=\"myForm.btn\"]')")).filter(l -> !l.contains("be.visible")).count()));
		}

		@Test
		@DisplayName("a blank text effect emits the OFF-FORM comment and no contain.text line")
		void blankText_hasNoContainText()
		{
			String out = emit(singleButton(Gesture.CLICK, new Effect(EffectKind.TEXT, "x", "  ")));

			assertAll(() -> assertEquals(1, out.lines().filter(l -> l.equals("    // OFF-FORM (not asserted): elements.x.text = <empty>")).count()),
				() -> assertFalse(out.contains("contain.text")));
		}

		@Test
		@DisplayName("effects keep the order of the model")
		void effects_keepModelOrder()
		{
			Effect enabled = new Effect(EffectKind.ENABLED, "other", "true");
			Effect visible = new Effect(EffectKind.VISIBLE, "lbl", "false");
			String enabledLine = "    cy.get('[data-cy=\"myForm.other\"]').should('be.enabled');";
			String visibleLine = "    cy.get('[data-cy=\"myForm.lbl\"]').should('not.be.visible');";

			assertAll(
				() -> assertEquals(prefix(FORM, "btn") + expectedCase(".click();", enabledLine, visibleLine) + FOOTER,
					emit(singleButton(Gesture.CLICK, enabled, visible))),
				() -> assertEquals(prefix(FORM, "btn") + expectedCase(".click();", visibleLine, enabledLine) + FOOTER,
					emit(singleButton(Gesture.CLICK, visible, enabled))));
		}

		@Test
		@DisplayName("an effect without a kind, and a non off-form effect without a target, emit nothing")
		void incompleteEffects_emitNothing()
		{
			FormSpecModel model = singleButton(Gesture.CLICK, new Effect(null, "lbl", "true"), new Effect(EffectKind.VISIBLE, null, "true"));

			assertEquals(prefix(FORM, "btn") + expectedCase(".click();") + FOOTER, emit(model));
		}
	}

	// ------------------------------------------------------------ branching and skipped

	@Nested
	class BranchingAndSkipped
	{
		@Test
		@DisplayName("a branching handler gets the gesture, the BRANCHING comment and the overlay check, and none of its effects")
		void branching()
		{
			Handler branching = new Handler("onActionMethodID", "newOrder", "forms.orders_list", Gesture.CLICK, false, true,
				List.of(new Effect(EffectKind.VISIBLE, "lbl", "false"), new Effect(EffectKind.OFF_FORM, null, "scopes.a.b()")));

			String expected = prefix(FORM, "btn") + block("  // btn.onActionMethodID -> forms.orders_list.newOrder",
				"  it('btn: onActionMethodID (newOrder)', () => {", "    cy.get('[data-cy=\"myForm.btn\"]').click();",
				"    // BRANCHING: one case per branch needed (forms.orders_list.newOrder)", OVERLAY, "  });") + FOOTER;

			assertEquals(expected, emit(model(element("btn", Kind.CONTROL, branching))));
		}

		@ParameterizedTest
		@NullAndEmptySource
		@DisplayName("without an owner the reference is just the method name")
		void ownerMissing(String owner)
		{
			Handler handler = new Handler("onActionMethodID", "hide", owner, Gesture.CLICK, false, true, List.of());

			String expected = prefix(FORM, "btn") + block("  // btn.onActionMethodID -> hide", "  it('btn: onActionMethodID (hide)', () => {",
				"    cy.get('[data-cy=\"myForm.btn\"]').click();", "    // BRANCHING: one case per branch needed (hide)", OVERLAY, "  });") +
				FOOTER;

			assertEquals(expected, emit(model(element("btn", Kind.CONTROL, handler))));
		}

		@Test
		@DisplayName("a NONE gesture emits exactly the no-gesture skipped case and no click")
		void noneGesture()
		{
			FormSpecModel model = model(element("btn", Kind.CONTROL, handler("onCellClick", Gesture.NONE, new Effect(EffectKind.VISIBLE, "lbl", "false"))));

			assertEquals(prefix(FORM, "btn") + block("  it.skip('btn: onCellClick (no gesture defined)', () => {});") + FOOTER, emit(model));
		}

		@Test
		@DisplayName("a null gesture is treated as NONE")
		void nullGesture()
		{
			FormSpecModel model = model(element("btn", Kind.CONTROL, handler("onActionMethodID", null)));

			assertEquals(prefix(FORM, "btn") + block("  it.skip('btn: onActionMethodID (no gesture defined)', () => {});") + FOOTER, emit(model));
		}

		@Test
		@DisplayName("an unresolved handler emits exactly the could-not-be-resolved skipped case")
		void unresolved()
		{
			Handler unresolved = new Handler("onActionMethodID", null, null, Gesture.CLICK, true, false, List.of());

			assertEquals(prefix(FORM, "btn") + block("  it.skip('btn: onActionMethodID (handler method could not be resolved)', () => {});") + FOOTER,
				emit(model(element("btn", Kind.CONTROL, unresolved))));
		}

		@Test
		@DisplayName("an unresolved handler with a NONE gesture reports the unresolved reason")
		void unresolved_winsOverNone()
		{
			Handler unresolved = new Handler("onCellClick", null, null, Gesture.NONE, true, false, List.of());

			assertEquals(prefix(FORM, "btn") + block("  it.skip('btn: onCellClick (handler method could not be resolved)', () => {});") + FOOTER,
				emit(model(element("btn", Kind.CONTROL, unresolved))));
		}
	}

	// ------------------------------------------------------------ ordering and determinism

	@Nested
	class OrderingAndDeterminism
	{
		private Handler named(String key, Gesture gesture)
		{
			return new Handler(key, "m", "forms.myForm", gesture, false, false, List.of());
		}

		@Test
		@DisplayName("elements are emitted by name and handlers by key, whatever the input order")
		void ordering()
		{
			FormSpecModel model = model(element("b", Kind.CONTROL, named("onActionMethodID", Gesture.CLICK)),
				element("a", Kind.CONTROL, named("onRightClickMethodID", Gesture.RIGHT_CLICK), named("onActionMethodID", Gesture.CLICK)));

			assertEquals(List.of("  it('loads without errors and all elements are visible', () => {", "  it('a: onActionMethodID (m)', () => {",
				"  it('a: onRightClickMethodID (m)', () => {", "  it('b: onActionMethodID (m)', () => {"), titleLines(emit(model)));
		}

		@Test
		@DisplayName("names are ordered by String.compareTo, so an upper case name comes before a lower case one")
		void ordering_isCaseSensitive()
		{
			FormSpecModel model = model(element("a", Kind.CONTROL), element("B", Kind.CONTROL));

			String expected = prefix(FORM, "B", "a") + FOOTER;

			assertEquals(expected, emit(model));
		}

		@Test
		@DisplayName("the same elements and handlers in a different input order give the same output")
		void determinism_inputOrder()
		{
			Element a = element("a", Kind.CONTROL, named("onActionMethodID", Gesture.CLICK), named("onRightClickMethodID", Gesture.RIGHT_CLICK));
			Element aReversed = element("a", Kind.CONTROL, named("onRightClickMethodID", Gesture.RIGHT_CLICK), named("onActionMethodID", Gesture.CLICK));
			Element b = element("b", Kind.INPUT, named("onDataChangeMethodID", Gesture.CHANGE));

			assertEquals(emit(model(a, b)), emit(model(b, aReversed)));
		}

		@Test
		@DisplayName("emitting the same model twice gives equal strings")
		void determinism_repeat()
		{
			FormSpecModel model = singleButton(Gesture.CLICK, new Effect(EffectKind.VISIBLE, "lbl", "false"));

			assertEquals(emit(model), emit(model));
		}

		@Test
		@DisplayName("different effect order is a different output, so the sorting is not applied to effects")
		void effectsAreNotSorted()
		{
			Effect one = new Effect(EffectKind.VISIBLE, "a", "true");
			Effect two = new Effect(EffectKind.VISIBLE, "b", "true");

			assertNotEquals(emit(singleButton(Gesture.CLICK, one, two)), emit(singleButton(Gesture.CLICK, two, one)));
		}
	}

	// ------------------------------------------------------------ escaping and truncation

	@Nested
	class EscapingAndTruncation
	{
		@Test
		@DisplayName("an element name with a single quote is escaped in every JS string")
		void elementName_singleQuote()
		{
			FormSpecModel model = model(element("o'k", Kind.CONTROL, handler("onActionMethodID", Gesture.CLICK)));

			String expected = prefix(FORM).replace("loads without errors and all elements are visible', () => {\n" + OVERLAY + "\n",
				"loads without errors and all elements are visible', () => {\n" + OVERLAY + "\n" +
					"    cy.get('[data-cy=\"myForm.o\\'k\"]').should('be.visible');\n") +
				block("  // o'k.onActionMethodID -> forms.myForm.hide", "  it('o\\'k: onActionMethodID (hide)', () => {",
					"    cy.get('[data-cy=\"myForm.o\\'k\"]').click();", OVERLAY, "  });") +
				FOOTER;

			assertEquals(expected, emit(model));
		}

		@Test
		@DisplayName("a double quote in an element name is escaped for the selector and then for the JS string")
		void elementName_doubleQuote()
		{
			List<String> out = emit(model(element("a\"b", Kind.CONTROL))).lines().toList();

			assertEquals("    cy.get('[data-cy=\"myForm.a\\\\\"b\"]').should('be.visible');", out.get(9));
		}

		@Test
		@DisplayName("a newline in the method name stays on one line in the title and in the comment")
		void methodName_newline()
		{
			Handler handler = new Handler("onActionMethodID", "do\nIt", "forms.myForm", Gesture.CLICK, false, false, List.of());

			String expected = prefix(FORM, "btn") + block("  // btn.onActionMethodID -> forms.myForm.do It",
				"  it('btn: onActionMethodID (do\\nIt)', () => {", "    cy.get('[data-cy=\"myForm.btn\"]').click();", OVERLAY, "  });") + FOOTER;

			assertEquals(expected, emit(model(element("btn", Kind.CONTROL, handler))));
		}

		@Test
		@DisplayName("an off-form value with a newline and 300 characters becomes one comment line of 160 characters ending with ...")
		void offForm_longValue_truncated()
		{
			String value = "scopes.a.b(\n" + "x".repeat(300) + ")";

			String expectedLine = OFF_FORM_PREFIX + "scopes.a.b( " + "x".repeat(145) + "...";

			assertEquals(prefix(FORM, "btn") + expectedCase(".click();", expectedLine) + FOOTER,
				emit(singleButton(Gesture.CLICK, new Effect(EffectKind.OFF_FORM, null, value))));
		}

		@ParameterizedTest
		@ValueSource(ints = { 159, 160 })
		@DisplayName("an off-form value of at most 160 characters is not truncated")
		void offForm_upToLimit_notTruncated(int length)
		{
			String value = "x".repeat(length);

			assertEquals(prefix(FORM, "btn") + expectedCase(".click();", OFF_FORM_PREFIX + value) + FOOTER,
				emit(singleButton(Gesture.CLICK, new Effect(EffectKind.OFF_FORM, null, value))));
		}

		@Test
		@DisplayName("an off-form value of 161 characters is cut to 157 characters plus ...")
		void offForm_oneOverLimit_truncated()
		{
			String value = "x".repeat(161);

			assertEquals(prefix(FORM, "btn") + expectedCase(".click();", OFF_FORM_PREFIX + "x".repeat(157) + "...") + FOOTER,
				emit(singleButton(Gesture.CLICK, new Effect(EffectKind.OFF_FORM, null, value))));
		}

		@Test
		@DisplayName("all line separators in an off-form value collapse to one space and trailing semicolons are removed")
		void offForm_lineSeparators_collapsed()
		{
			String value = "  x\u2028y\u0085z\r\nw\u2029v  foo(1) ;; ";

			assertEquals(prefix(FORM, "btn") + expectedCase(".click();", OFF_FORM_PREFIX + "x y z w v foo(1)") + FOOTER,
				emit(singleButton(Gesture.CLICK, new Effect(EffectKind.OFF_FORM, null, value))));
		}

		@Test
		@DisplayName("a text literal with a backslash, a line feed and a carriage return is escaped in contain.text")
		void textLiteral_isEscaped()
		{
			String expectedLine = "    cy.get('[data-cy=\"myForm.lbl\"]').should('contain.text', 'a\\\\b\\nc\\rd');";

			assertEquals(prefix(FORM, "btn") + expectedCase(".click();", expectedLine) + FOOTER,
				emit(singleButton(Gesture.CLICK, new Effect(EffectKind.TEXT, "lbl", "a\\b\nc\rd"))));
		}
	}

	// ------------------------------------------------------------ allowlist and counts

	@Nested
	class AllowlistAndCounts
	{
		private static final Pattern FORBIDDEN = Pattern.compile("bgcolor|fgcolor|font|border");

		@Test
		@DisplayName("a model using every effect kind, gesture and element kind never emits bgcolor, fgcolor, font or border")
		void noStyleLeakage()
		{
			List<Effect> effects = new ArrayList<>();
			for (EffectKind kind : EffectKind.values())
			{
				effects.add(new Effect(kind, kind == EffectKind.OFF_FORM ? null : "lbl", kind == EffectKind.VISIBLE ? "false" : "v"));
			}
			List<Element> elements = new ArrayList<>();
			for (Kind kind : Kind.values())
			{
				List<Handler> handlers = new ArrayList<>();
				for (Gesture gesture : Gesture.values())
				{
					handlers.add(new Handler("on" + gesture.name(), "m", "forms.myForm", gesture, false, false, effects));
				}
				elements.add(new Element("el_" + kind.name(), "bootstrapcomponents-button", kind, true, handlers));
			}

			String out = emit(new FormSpecModel(FORM, elements));

			assertFalse(FORBIDDEN.matcher(out).find(), "output must not contain bgcolor, fgcolor, font or border:\n" + out);
		}

		@Test
		@DisplayName("countVisibilityAssertions counts only CONTROL and INPUT elements")
		void countVisibilityAssertions()
		{
			FormSpecModel model = model(element("a", Kind.CONTROL), element("b", Kind.INPUT), element("c", Kind.STATIC), element("d", Kind.CONTAINER));

			assertEquals(2, FormSpecModelEmitterAccess.countVisibilityAssertions(model));
		}

		@Test
		@DisplayName("countHandlerCases counts every handler including skipped ones")
		void countHandlerCases()
		{
			FormSpecModel model = model(
				element("a", Kind.CONTROL, handler("onActionMethodID", Gesture.CLICK), handler("onCellClick", Gesture.NONE)),
				element("b", Kind.STATIC, new Handler("onActionMethodID", null, null, Gesture.CLICK, true, false, List.of())), element("c", Kind.INPUT));

			assertEquals(3, FormSpecModelEmitterAccess.countHandlerCases(model));
		}
	}
}
