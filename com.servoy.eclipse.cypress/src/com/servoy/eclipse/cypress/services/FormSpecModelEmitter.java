package com.servoy.eclipse.cypress.services;

import java.util.Comparator;
import java.util.List;

import com.servoy.eclipse.cypress.services.FormSpecModel.Effect;
import com.servoy.eclipse.cypress.services.FormSpecModel.Element;
import com.servoy.eclipse.cypress.services.FormSpecModel.Gesture;
import com.servoy.eclipse.cypress.services.FormSpecModel.Handler;
import com.servoy.eclipse.cypress.services.FormSpecModel.Kind;

/**
 * Pure, stateless emitter that turns a {@link FormSpecModel} into the content of a <code>.spec.cy.js</code> file. Output is deterministic: elements
 * are sorted by name, handlers by key, effects keep the model order, and there are no caps, timestamps or random values.
 */
final class FormSpecModelEmitter
{
	private static final String ERROR_OVERLAY_CHECK = "cy.get('.svy-error, .error-overlay').should('not.exist');";
	private static final int MAX_COMMENT_LENGTH = 160;

	private FormSpecModelEmitter()
	{
	}

	static String emit(String formUrl, FormSpecModel model)
	{
		String form = model.formName();
		List<Element> elements = model.elements().stream().sorted(Comparator.comparing(Element::name, Comparator.nullsFirst(Comparator.naturalOrder())))
			.toList();

		StringBuilder sb = new StringBuilder();
		sb.append("describe('").append(js(form)).append("', () => {\n\n");

		sb.append("  beforeEach(() => {\n");
		sb.append("    cy.visit('").append(js(formUrl)).append("');\n");
		if (elements.isEmpty())
		{
			sb.append("    cy.get('.svy-form', { timeout: 30000 }).should('exist');\n");
		}
		else
		{
			sb.append("    cy.get('").append(js("[data-cy^=\"" + selectorEscape(form) + ".\"]")).append("', { timeout: 30000 }).should('exist');\n");
		}
		sb.append("  });\n\n");

		sb.append("  it('loads without errors and all elements are visible', () => {\n");
		sb.append("    ").append(ERROR_OVERLAY_CHECK).append("\n");
		for (Element element : elements)
		{
			if (isAsserted(element))
			{
				sb.append("    cy.get('").append(js(selector(form, element.name()))).append("').should('be.visible');\n");
			}
		}
		sb.append("  });\n\n");

		for (Element element : elements)
		{
			List<Handler> handlers = element.handlers().stream().sorted(Comparator.comparing(Handler::key, Comparator.nullsFirst(Comparator.naturalOrder())))
				.toList();
			for (Handler handler : handlers)
			{
				appendHandlerCase(sb, form, element, handler);
			}
		}

		sb.append("});\n");
		return sb.toString();
	}

	/**
	 * Number of elements that get a <code>be.visible</code> assertion in the baseline case.
	 */
	static int countVisibilityAssertions(FormSpecModel model)
	{
		return (int)model.elements().stream().filter(FormSpecModelEmitter::isAsserted).count();
	}

	/**
	 * Number of handler cases (including skipped ones) in the model.
	 */
	static int countHandlerCases(FormSpecModel model)
	{
		return model.elements().stream().mapToInt(e -> e.handlers().size()).sum();
	}

	/** A control or input that is shown in the form: the only elements that can be asserted visible. */
	private static boolean isAsserted(Element element)
	{
		return element.visible() && (element.kind() == Kind.CONTROL || element.kind() == Kind.INPUT);
	}

	private static void appendHandlerCase(StringBuilder sb, String form, Element element, Handler handler)
	{
		String title = nz(element.name()) + ": " + nz(handler.key());
		if (!element.visible())
		{
			// a hidden element is not in the page, so it can neither be clicked nor typed into
			sb.append("  it.skip('").append(js(title + " (element is hidden by design)")).append("', () => {});\n\n");
			return;
		}
		if (handler.unresolved())
		{
			sb.append("  it.skip('").append(js(title + " (handler method could not be resolved)")).append("', () => {});\n\n");
			return;
		}
		Gesture gesture = handler.gesture() == null ? Gesture.NONE : handler.gesture();
		if (gesture == Gesture.NONE)
		{
			sb.append("  it.skip('").append(js(title + " (no gesture defined)")).append("', () => {});\n\n");
			return;
		}

		String reference = reference(handler);
		sb.append("  // ").append(oneLine(nz(element.name()) + "." + nz(handler.key()) + " -> " + reference)).append("\n");
		sb.append("  it('").append(js(title + " (" + nz(handler.method()) + ")")).append("', () => {\n");

		String elementSelector = "    cy.get('" + js(selector(form, element.name())) + "')";
		sb.append(elementSelector).append(gestureCall(gesture)).append("\n");

		if (handler.branching())
		{
			sb.append("    // BRANCHING: one case per branch needed (").append(oneLine(reference)).append(")\n");
		}
		else
		{
			for (Effect effect : handler.effects())
			{
				appendEffect(sb, form, effect);
			}
		}

		sb.append("    ").append(ERROR_OVERLAY_CHECK).append("\n");
		sb.append("  });\n\n");
	}

	private static String gestureCall(Gesture gesture)
	{
		switch (gesture)
		{
			case RIGHT_CLICK :
				return ".rightclick();";
			case CHANGE :
				return ".clear().type('1').blur();";
			default :
				return ".click();";
		}
	}

	private static void appendEffect(StringBuilder sb, String form, Effect effect)
	{
		if (effect.kind() == null)
		{
			return;
		}
		if (effect.kind() == FormSpecModel.EffectKind.OFF_FORM)
		{
			sb.append("    // OFF-FORM (not asserted): ").append(oneLine(effect.value())).append("\n");
			return;
		}
		if (effect.target() == null)
		{
			return;
		}
		if (effect.kind() == FormSpecModel.EffectKind.TEXT && (effect.value() == null || effect.value().isBlank()))
		{
			// contain.text with an empty string can never fail, so it is reported instead of asserted
			sb.append("    // OFF-FORM (not asserted): ").append(oneLine("elements." + effect.target() + ".text = <empty>")).append("\n");
			return;
		}

		String assertion;
		switch (effect.kind())
		{
			case VISIBLE :
				assertion = "false".equals(effect.value()) ? ".should('not.be.visible');" : ".should('be.visible');";
				break;
			case ENABLED :
				assertion = "false".equals(effect.value()) ? ".should('be.disabled');" : ".should('be.enabled');";
				break;
			case ADD_STYLE_CLASS :
				assertion = ".should('have.class', '" + js(effect.value()) + "');";
				break;
			case REMOVE_STYLE_CLASS :
				assertion = ".should('not.have.class', '" + js(effect.value()) + "');";
				break;
			case TEXT :
				assertion = ".should('contain.text', '" + js(effect.value()) + "');";
				break;
			default :
				return;
		}
		sb.append("    cy.get('").append(js(selector(form, effect.target()))).append("')").append(assertion).append("\n");
	}

	private static String reference(Handler handler)
	{
		String owner = handler.owner();
		if (owner == null || owner.isEmpty())
		{
			return nz(handler.method());
		}
		return owner + "." + nz(handler.method());
	}

	/**
	 * Same escaping as the element service selector: backslash and double quote.
	 */
	private static String selectorEscape(String value)
	{
		return nz(value).replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static String selector(String form, String element)
	{
		return "[data-cy=\"" + selectorEscape(nz(form) + "." + nz(element)) + "\"]";
	}

	/**
	 * Escapes text for use inside a JS single-quoted string.
	 */
	private static String js(String value)
	{
		StringBuilder sb = new StringBuilder();
		for (char c : nz(value).toCharArray())
		{
			switch (c)
			{
				case '\\' :
					sb.append("\\\\");
					break;
				case '\'' :
					sb.append("\\'");
					break;
				case '\r' :
					sb.append("\\r");
					break;
				case '\n' :
					sb.append("\\n");
					break;
				case '\u2028' :
					sb.append("\\u2028");
					break;
				case '\u2029' :
					sb.append("\\u2029");
					break;
				default :
					sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Collapses text to a single line for use in a line comment: whitespace runs become one space, trailing semicolons are removed, and the result is
	 * truncated to {@value #MAX_COMMENT_LENGTH} characters (ending with "...").
	 */
	private static String oneLine(String value)
	{
		String text = nz(value).replaceAll("[\\s\\u0085\\u2028\\u2029]+", " ").trim();
		while (text.endsWith(";"))
		{
			text = text.substring(0, text.length() - 1).trim();
		}
		if (text.length() > MAX_COMMENT_LENGTH)
		{
			text = text.substring(0, MAX_COMMENT_LENGTH - 3) + "...";
		}
		return text;
	}

	private static String nz(String value)
	{
		return value == null ? "" : value;
	}
}
