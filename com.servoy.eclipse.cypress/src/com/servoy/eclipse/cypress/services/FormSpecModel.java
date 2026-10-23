package com.servoy.eclipse.cypress.services;

import java.util.List;
import java.util.Set;

/**
 * Neutral, immutable description of a form and the handlers bound to its elements. It contains no Servoy or Eclipse types, so it can be built by
 * any caller (for example the MCP bundle) and handed to {@link FormSpecGenerator#generateSpec(String, FormSpecModel)}.
 * <p>
 * The allowed effects are exactly the values of {@link EffectKind}; there is deliberately no way to express bgcolor/fgcolor/font/border.
 */
public record FormSpecModel(String formName, List<Element> elements)
{
	public FormSpecModel
	{
		elements = elements == null ? List.of() : List.copyOf(elements);
	}

	/**
	 * What an element is for the generated spec: something that is interacted with, something that holds a value, a container or static content.
	 */
	public enum Kind
	{
		CONTROL, INPUT, CONTAINER, STATIC
	}

	/**
	 * The user action that triggers a handler in the generated spec. {@link #NONE} means there is no safe gesture, the case is emitted as skipped.
	 */
	public enum Gesture
	{
		CLICK, RIGHT_CLICK, CHANGE, NONE;

		/** Web component types that are text inputs: onAction fires on Enter (not on click), onDataChange fires after type + blur. */
		private static final Set<String> TEXT_ENTRY_TYPES = Set.of("bootstrapcomponents-textbox", "bootstrapcomponents-textarea", "servoydefault-textfield",
			"servoydefault-textarea");

		/**
		 * Legacy persist type name (simple Java class name, as reported for non web components) of a field. A legacy {@code Field} can be a text
		 * field, but also a combobox, calendar, checkbox, ... depending on its display type, which the model does not carry.
		 */
		private static final String LEGACY_FIELD_TYPE = "Field";

		/**
		 * The single shared table that maps a handler key (exact match) on an element to the gesture that triggers it.
		 * <p>
		 * The two text-entry rules deliberately differ. {@code onAction*} on an element is skipped ({@link #NONE}) when it is one of the four web text
		 * inputs or a legacy {@code Field}: a text field fires onAction on Enter, and for the other legacy display types a click is not known to be
		 * right either, so a skipped case is safer than a wrong one. {@code onDataChange*} becomes {@link #CHANGE} only for the four web text inputs
		 * (with a dataprovider), because {@code clear().type().blur()} fails on a legacy {@code Field} that is a combobox, calendar or checkbox and the
		 * model has no display type to tell them apart; there it stays {@link #NONE}.
		 * </p>
		 */
		public static Gesture forHandler(String handlerKey, String elementType, boolean hasDataProvider)
		{
			if (handlerKey == null)
			{
				return NONE;
			}
			boolean textEntry = elementType != null && TEXT_ENTRY_TYPES.contains(elementType);
			boolean skipOnAction = textEntry || LEGACY_FIELD_TYPE.equals(elementType);
			switch (handlerKey)
			{
				case "onActionMethodID" :
				case "onAction" :
					return skipOnAction ? NONE : CLICK;
				case "onRightClickMethodID" :
				case "onRightClick" :
					return RIGHT_CLICK;
				case "onDataChangeMethodID" :
				case "onDataChange" :
					return hasDataProvider && textEntry ? CHANGE : NONE;
				default :
					return NONE;
			}
		}
	}

	/**
	 * The closed set of effects a handler can have that the generator knows how to assert, plus {@link #OFF_FORM} for everything else.
	 */
	public enum EffectKind
	{
		VISIBLE, ENABLED, TEXT, ADD_STYLE_CLASS, REMOVE_STYLE_CLASS, OFF_FORM
	}

	/**
	 * One effect of a handler method.
	 *
	 * @param kind what kind of effect it is
	 * @param target element name (null for OFF_FORM)
	 * @param value "true"/"false" for VISIBLE and ENABLED, the text literal for TEXT, the class name for the style class kinds, or for OFF_FORM the
	 *            single-line source statement
	 */
	public record Effect(EffectKind kind, String target, String value)
	{
	}

	/**
	 * A handler bound to an element.
	 *
	 * @param key the handler key on the element, for example {@code onActionMethodID}
	 * @param method the name of the bound method
	 * @param owner the owner of the method ({@code forms.<name>} or {@code scopes.<name>}), may be null
	 * @param gesture the gesture that triggers it
	 * @param unresolved true when the bound method could not be found
	 * @param branching true when the method body is conditional, so its effects are not unconditional
	 * @param effects the effects found in the method body, in source order
	 */
	public record Handler(String key, String method, String owner, Gesture gesture, boolean unresolved, boolean branching, List<Effect> effects)
	{
		public Handler
		{
			effects = effects == null ? List.of() : List.copyOf(effects);
		}
	}

	/**
	 * An element of the form that can be targeted by its {@code data-cy} selector.
	 *
	 * @param name the element name
	 * @param type the component type (web component name or legacy persist type name)
	 * @param kind what the element is for the generated spec
	 * @param hasDataProvider true when the element is bound to a dataprovider
	 * @param visible false when the element is hidden in the form (visible=false on the element or on a container around it); such an element
	 *            cannot be asserted visible or clicked, so it is left out of the visibility baseline and its handlers become skipped cases
	 * @param handlers the handlers bound to the element
	 */
	public record Element(String name, String type, Kind kind, boolean hasDataProvider, boolean visible, List<Handler> handlers)
	{
		public Element
		{
			handlers = handlers == null ? List.of() : List.copyOf(handlers);
		}

		/** An element that is visible in the form, which is what almost every element is. */
		public Element(String name, String type, Kind kind, boolean hasDataProvider, List<Handler> handlers)
		{
			this(name, type, kind, hasDataProvider, true, handlers);
		}
	}
}
