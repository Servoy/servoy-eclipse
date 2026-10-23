package com.servoy.eclipse.cypress.plainjunit.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.servoy.eclipse.cypress.services.FormSpecModel;
import com.servoy.eclipse.cypress.services.FormSpecModel.Effect;
import com.servoy.eclipse.cypress.services.FormSpecModel.EffectKind;
import com.servoy.eclipse.cypress.services.FormSpecModel.Element;
import com.servoy.eclipse.cypress.services.FormSpecModel.Gesture;
import com.servoy.eclipse.cypress.services.FormSpecModel.Handler;
import com.servoy.eclipse.cypress.services.FormSpecModel.Kind;

class FormSpecModelTest
{
	@Nested
	class GestureTable
	{
		@ParameterizedTest
		@CsvSource({ "onActionMethodID,servoydefault-button,true,CLICK", "onAction,bootstrapcomponents-button,false,CLICK", "onActionMethodID,GraphicalComponent,false,CLICK", "onActionMethodID,bootstrapcomponents-textbox,true,NONE", "onActionMethodID,bootstrapcomponents-textarea,true,NONE", "onActionMethodID,servoydefault-textfield,true,NONE", "onActionMethodID,servoydefault-textarea,true,NONE", "onAction,servoydefault-textfield,false,NONE", "onActionMethodID,Field,true,NONE", "onActionMethodID,Field,false,NONE", "onRightClickMethodID,servoydefault-label,false,RIGHT_CLICK", "onRightClick,bootstrapcomponents-textbox,true,RIGHT_CLICK", "onRightClickMethodID,Field,true,RIGHT_CLICK", "onDataChangeMethodID,bootstrapcomponents-textbox,true,CHANGE", "onDataChangeMethodID,bootstrapcomponents-textarea,true,CHANGE", "onDataChange,servoydefault-textfield,true,CHANGE", "onDataChangeMethodID,servoydefault-textarea,true,CHANGE", "onDataChangeMethodID,bootstrapcomponents-textbox,false,NONE", "onDataChange,servoydefault-checkbox,true,NONE", "onDataChangeMethodID,Field,true,NONE", "onDataChangeMethodID,bootstrapcomponents-combobox,true,NONE", "onCellClick,aggrid-groupingtable,false,NONE", "onColumnDataChange,bootstrapcomponents-textbox,true,NONE", "onFocusGainedMethodID,bootstrapcomponents-textbox,true,NONE", "onRender,servoydefault-button,false,NONE", "onactionmethodid,servoydefault-button,false,NONE", "xonActionMethodID,servoydefault-button,false,NONE" })
		@DisplayName("forHandler maps handler key, element type and dataprovider to exactly one gesture")
		void forHandler_table(String key, String type, boolean hasDataProvider, Gesture expected)
		{
			assertEquals(expected, Gesture.forHandler(key, type, hasDataProvider));
		}

		@Test
		@DisplayName("forHandler of a null handler key is NONE")
		void forHandler_nullKey()
		{
			assertEquals(Gesture.NONE, Gesture.forHandler(null, "x", false));
		}

		@Test
		@DisplayName("forHandler with a null element type clicks onAction and ignores onDataChange")
		void forHandler_nullType()
		{
			assertAll(() -> assertEquals(Gesture.CLICK, Gesture.forHandler("onActionMethodID", null, false)),
				() -> assertEquals(Gesture.NONE, Gesture.forHandler("onDataChangeMethodID", null, true)));
		}
	}

	@Nested
	class ClosedSets
	{
		@Test
		@DisplayName("EffectKind holds exactly the allowlist plus OFF_FORM, so bgcolor/fgcolor/font/border cannot be expressed")
		void effectKind_isClosed()
		{
			assertEquals(List.of("VISIBLE", "ENABLED", "TEXT", "ADD_STYLE_CLASS", "REMOVE_STYLE_CLASS", "OFF_FORM"),
				Arrays.stream(EffectKind.values()).map(Enum::name).toList());
		}

		@Test
		@DisplayName("Gesture holds exactly CLICK, RIGHT_CLICK, CHANGE and NONE")
		void gesture_isClosed()
		{
			assertEquals(List.of("CLICK", "RIGHT_CLICK", "CHANGE", "NONE"), Arrays.stream(Gesture.values()).map(Enum::name).toList());
		}

		@Test
		@DisplayName("Kind holds exactly CONTROL, INPUT, CONTAINER and STATIC")
		void kind_isClosed()
		{
			assertEquals(List.of("CONTROL", "INPUT", "CONTAINER", "STATIC"), Arrays.stream(Kind.values()).map(Enum::name).toList());
		}
	}

	@Nested
	class Immutability
	{
		private final Effect effect = new Effect(EffectKind.VISIBLE, "lbl", "false");

		private Handler handler(List<Effect> effects)
		{
			return new Handler("onActionMethodID", "hide", "forms.f", Gesture.CLICK, false, false, effects);
		}

		@Test
		@DisplayName("mutating the source element list after construction does not change the model")
		void elements_areCopied()
		{
			List<Element> source = new ArrayList<>();
			source.add(new Element("a", "t", Kind.CONTROL, false, List.of()));
			FormSpecModel model = new FormSpecModel("f", source);

			source.add(new Element("b", "t", Kind.CONTROL, false, List.of()));
			source.remove(0);

			assertEquals(List.of("a"), model.elements().stream().map(Element::name).toList());
		}

		@Test
		@DisplayName("mutating the source handler list after construction does not change the element")
		void handlers_areCopied()
		{
			List<Handler> source = new ArrayList<>();
			source.add(handler(List.of()));
			Element element = new Element("a", "t", Kind.CONTROL, false, source);

			source.clear();

			assertEquals(1, element.handlers().size());
		}

		@Test
		@DisplayName("mutating the source effect list after construction does not change the handler")
		void effects_areCopied()
		{
			List<Effect> source = new ArrayList<>();
			source.add(effect);
			Handler handler = handler(source);

			source.clear();

			assertEquals(List.of(effect), handler.effects());
		}

		@Test
		@DisplayName("the exposed lists reject modification")
		void exposedLists_areUnmodifiable()
		{
			FormSpecModel model = new FormSpecModel("f", List.of(new Element("a", "t", Kind.CONTROL, false, List.of(handler(List.of(effect))))));
			Element element = model.elements().get(0);
			assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> model.elements().clear()),
				() -> assertThrows(UnsupportedOperationException.class, () -> element.handlers().clear()),
				() -> assertThrows(UnsupportedOperationException.class, () -> element.handlers().get(0).effects().clear()));
		}

		@Test
		@DisplayName("null lists become empty lists")
		void nullLists_becomeEmpty()
		{
			assertAll(() -> assertEquals(List.of(), new FormSpecModel("f", null).elements()),
				() -> assertEquals(List.of(), new Element("a", "t", Kind.STATIC, false, null).handlers()),
				() -> assertEquals(List.of(), handler(null).effects()));
		}

		@ParameterizedTest
		@ValueSource(strings = { "a", "b" })
		@DisplayName("records with equal components are equal")
		void records_haveValueEquality(String name)
		{
			assertEquals(new Element(name, "t", Kind.INPUT, true, List.of()), new Element(name, "t", Kind.INPUT, true, List.of()));
		}
	}
}
