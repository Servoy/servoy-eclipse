/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
package com.servoy.eclipse.ui.tests.plainjunit.editors.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.servoy.eclipse.ui.editors.table.CalculationUuidMarker;

/**
 * SVY-21710 - reading and writing the UUID marker (<code>@type {UUID}</code> /
 * <code>@return {UUID}</code>) in the first JSDoc block of a calculation
 * declaration.
 */
@SuppressWarnings("nls")
class CalculationUuidMarkerTest {
	private static final String PROPERTIES = " * @properties={type:-4,typeid:36,uuid:\"2D0B5A0C-8E1B-4C55-9A53-7C7B2E3D1A11\"}";
	private static final String FUNCTION = "function calc_uuid() {\n\treturn null;\n}";

	private static String declaration(String... commentLines) {
		return "/**\n" + String.join("\n", commentLines) + "\n */\n" + FUNCTION;
	}

	private static String crlf(String s) {
		return s.replace("\n", "\r\n");
	}

	@Nested
	class HasUuidMarker {
		@ParameterizedTest
		@ValueSource(strings = { " * @type {UUID}", " * @return {UUID}", " * @returns {UUID}",
				" * @return {UUID} the unique id", " * @returns {UUID} the unique id",
				" * @type {UUID} the unique id" })
		@DisplayName("a UUID @type or @return line in the first block is detected")
		void detected(String line) {
			assertTrue(CalculationUuidMarker.hasUuidMarker(declaration(line, PROPERTIES)));
		}

		@Test
		@DisplayName("a UUID @type tag on the same line as the opening /** is detected")
		void detectedOnOpenerLine() {
			assertTrue(CalculationUuidMarker.hasUuidMarker("/** @type {UUID}\n" + PROPERTIES + "\n */\n" + FUNCTION));
		}

		@ParameterizedTest
		@ValueSource(strings = { " * @type {String}", " * @return {String}", " * @returns {String}",
				" * @return {Number}", " * some text mentioning {UUID} but no tag" })
		@DisplayName("a non-UUID tag is not detected")
		void notDetected(String line) {
			assertFalse(CalculationUuidMarker.hasUuidMarker(declaration(line, PROPERTIES)));
		}

		@Test
		@DisplayName("null and a declaration without JSDoc have no marker")
		void nullAndNoJsDoc() {
			assertFalse(CalculationUuidMarker.hasUuidMarker(null));
			assertFalse(CalculationUuidMarker.hasUuidMarker(FUNCTION));
		}

		@Test
		@DisplayName("a UUID tag outside the first JSDoc block is not detected")
		void notDetectedOutsideFirstBlock() {
			String decl = "/**\n" + PROPERTIES
					+ "\n */\nfunction calc_uuid() {\n\t/** @return {UUID} */\n\treturn null;\n}";
			assertFalse(CalculationUuidMarker.hasUuidMarker(decl));
		}
	}

	@Nested
	class AddMarker {
		@Test
		@DisplayName("adds @type {UUID} before the @properties line")
		void addBeforeProperties() {
			assertEquals("/**\n * @type {UUID}\n *\n" + PROPERTIES + "\n */\n" + FUNCTION,
					CalculationUuidMarker.setUuidMarker(declaration(PROPERTIES), true));
		}

		@Test
		@DisplayName("adds @type {UUID} before @properties and keeps the existing description and tags")
		void addKeepsExistingContent() {
			String decl = declaration(" * the description", " *", " * @param {String} a", PROPERTIES);
			assertEquals(declaration(" * the description", " *", " * @param {String} a", " * @type {UUID}", " *",
					PROPERTIES), CalculationUuidMarker.setUuidMarker(decl, true));
		}

		@Test
		@DisplayName("adds @type {UUID} before the closing */ when there is no @properties")
		void addBeforeBlockEnd() {
			assertEquals(declaration(" * the description", " * @type {UUID}"),
					CalculationUuidMarker.setUuidMarker(declaration(" * the description"), true));
		}

		@Test
		@DisplayName("creates a JSDoc block when the declaration has none")
		void addWithoutJsDoc() {
			assertEquals("/**\n * @type {UUID}\n */\n" + FUNCTION, CalculationUuidMarker.setUuidMarker(FUNCTION, true));
		}

		@Test
		@DisplayName("a null declaration gets a block with only the marker")
		void addToNull() {
			assertEquals("/**\n * @type {UUID}\n */\n", CalculationUuidMarker.setUuidMarker(null, true));
		}

		@Test
		@DisplayName("adds the marker to a single line JSDoc block that has content")
		void addToSingleLineBlock() {
			String result = CalculationUuidMarker.setUuidMarker("/** the description */\n" + FUNCTION, true);
			assertEquals("/**\n * the description\n * @type {UUID}\n */\n" + FUNCTION, result);
		}

		@ParameterizedTest
		@ValueSource(strings = { " * @type {UUID}", " * @return {UUID}" })
		@DisplayName("adding is idempotent: an existing marker returns the very same declaration")
		void addIsIdempotent(String line) {
			String decl = declaration(line, " *", PROPERTIES);
			assertSame(decl, CalculationUuidMarker.setUuidMarker(decl, true));
		}

		@Test
		@DisplayName("adding twice gives the same result as adding once")
		void addTwice() {
			String once = CalculationUuidMarker.setUuidMarker(declaration(PROPERTIES), true);
			assertEquals(once, CalculationUuidMarker.setUuidMarker(once, true));
		}

		@Test
		@DisplayName("the added marker is detected afterwards")
		void addedMarkerIsDetected() {
			assertTrue(CalculationUuidMarker
					.hasUuidMarker(CalculationUuidMarker.setUuidMarker(declaration(PROPERTIES), true)));
			assertTrue(CalculationUuidMarker.hasUuidMarker(CalculationUuidMarker.setUuidMarker(FUNCTION, true)));
		}

		@Test
		@DisplayName("a non-UUID @type {String} does not count as marker, the UUID marker is still added")
		void addWithNonUuidType() {
			String decl = declaration(" * @type {String}", PROPERTIES);
			assertEquals(declaration(" * @type {String}", " * @type {UUID}", " *", PROPERTIES),
					CalculationUuidMarker.setUuidMarker(decl, true));
		}

		@Test
		@DisplayName("CRLF is kept when adding to a declaration with @properties")
		void addKeepsCrlf() {
			assertEquals(crlf("/**\n * @type {UUID}\n *\n" + PROPERTIES + "\n */\n" + FUNCTION),
					CalculationUuidMarker.setUuidMarker(crlf(declaration(PROPERTIES)), true));
		}

		@Test
		@DisplayName("@properties on the opening /** line: the marker goes on its own line before the closing */")
		void addWithPropertiesOnOpenerLine() {
			String original = "/** @properties={type:-4}\n */\n" + FUNCTION;
			String result = CalculationUuidMarker.setUuidMarker(original, true);
			assertEquals("/** @properties={type:-4}\n * @type {UUID}\n */\n" + FUNCTION, result);
			assertTrue(CalculationUuidMarker.hasUuidMarker(result));
			assertEquals(original, CalculationUuidMarker.setUuidMarker(result, false));
		}

		@Test
		@DisplayName("@properties on the opening /** line with CRLF: the marker goes before the closing */ and CRLF is kept")
		void addWithPropertiesOnOpenerLineCrlf() {
			String original = crlf("/** @properties={type:-4}\n */\n" + FUNCTION);
			String result = CalculationUuidMarker.setUuidMarker(original, true);
			assertEquals(crlf("/** @properties={type:-4}\n * @type {UUID}\n */\n" + FUNCTION), result);
			assertTrue(CalculationUuidMarker.hasUuidMarker(result));
			assertEquals(original, CalculationUuidMarker.setUuidMarker(result, false));
		}

		@Test
		@DisplayName("single line block holding @properties is expanded, the marker is detected and removing it keeps the expanded block")
		void addWithPropertiesInSingleLineBlock() {
			String original = "/** @properties={type:-4} */\n" + FUNCTION;
			String result = CalculationUuidMarker.setUuidMarker(original, true);
			assertEquals("/**\n * @properties={type:-4}\n * @type {UUID}\n */\n" + FUNCTION, result);
			assertTrue(CalculationUuidMarker.hasUuidMarker(result));
			assertEquals("/**\n * @properties={type:-4}\n */\n" + FUNCTION,
					CalculationUuidMarker.setUuidMarker(result, false));
		}

		@Test
		@DisplayName("single line block holding @properties with CRLF is expanded with CRLF, the marker is detected and removing it keeps the expanded block")
		void addWithPropertiesInSingleLineBlockCrlf() {
			String original = crlf("/** @properties={type:-4} */\n" + FUNCTION);
			String result = CalculationUuidMarker.setUuidMarker(original, true);
			assertEquals(crlf("/**\n * @properties={type:-4}\n * @type {UUID}\n */\n" + FUNCTION), result);
			assertTrue(CalculationUuidMarker.hasUuidMarker(result));
			assertEquals(crlf("/**\n * @properties={type:-4}\n */\n" + FUNCTION),
					CalculationUuidMarker.setUuidMarker(result, false));
		}

		@Test
		@DisplayName("CRLF is used for the new block when the declaration has none")
		void addCreatesBlockWithCrlf() {
			assertEquals(crlf("/**\n * @type {UUID}\n */\n" + FUNCTION),
					CalculationUuidMarker.setUuidMarker(crlf(FUNCTION), true));
		}
	}

	@Nested
	class RemoveMarker {
		@Test
		@DisplayName("removes @type {UUID} together with the blank line that separated it from @properties")
		void removeTypeTag() {
			assertEquals(declaration(PROPERTIES),
					CalculationUuidMarker.setUuidMarker(declaration(" * @type {UUID}", " *", PROPERTIES), false));
		}

		@Test
		@DisplayName("removes @return {UUID}")
		void removeReturnTag() {
			assertEquals(declaration(PROPERTIES),
					CalculationUuidMarker.setUuidMarker(declaration(" * @return {UUID}", PROPERTIES), false));
		}

		@Test
		@DisplayName("removes @returns {UUID} together with its description as a whole line")
		void removeReturnsTagWithDescription() {
			assertEquals(declaration(PROPERTIES), CalculationUuidMarker
					.setUuidMarker(declaration(" * @returns {UUID} the unique id", PROPERTIES), false));
		}

		@Test
		@DisplayName("removes a UUID tag that is the last line of the block and keeps the description")
		void removeLastLine() {
			assertEquals(declaration(" * the description"),
					CalculationUuidMarker.setUuidMarker(declaration(" * the description", " * @type {UUID}"), false));
		}

		@Test
		@DisplayName("removes a UUID tag on the same line as the opening /** and keeps the rest of the block")
		void removeOnOpenerLine() {
			assertEquals(declaration(PROPERTIES), CalculationUuidMarker
					.setUuidMarker("/** @type {UUID}\n" + PROPERTIES + "\n */\n" + FUNCTION, false));
		}

		@Test
		@DisplayName("add followed by remove gives back the original declaration")
		void addThenRemoveRoundTrip() {
			String original = declaration(" * the description", " *", PROPERTIES);
			String added = CalculationUuidMarker.setUuidMarker(original, true);
			assertEquals(original, CalculationUuidMarker.setUuidMarker(added, false));
		}

		@Test
		@DisplayName("the removed marker is no longer detected")
		void removedMarkerIsNotDetected() {
			assertFalse(CalculationUuidMarker.hasUuidMarker(
					CalculationUuidMarker.setUuidMarker(declaration(" * @type {UUID}", PROPERTIES), false)));
			assertFalse(CalculationUuidMarker.hasUuidMarker(
					CalculationUuidMarker.setUuidMarker(declaration(" * @return {UUID}", PROPERTIES), false)));
		}

		@ParameterizedTest
		@ValueSource(strings = { " * @type {String}", " * @return {String}", " * @returns {String}" })
		@DisplayName("a non-UUID @type, @return or @returns is not removed")
		void nonUuidTagIsKept(String line) {
			String decl = declaration(line, PROPERTIES);
			assertEquals(decl, CalculationUuidMarker.setUuidMarker(decl, false));
		}

		@Test
		@DisplayName("removing from a declaration without marker or without JSDoc changes nothing")
		void nothingToRemove() {
			String decl = declaration(PROPERTIES);
			assertEquals(decl, CalculationUuidMarker.setUuidMarker(decl, false));
			assertEquals(FUNCTION, CalculationUuidMarker.setUuidMarker(FUNCTION, false));
		}

		@Test
		@DisplayName("unrelated tags (@param, @properties) are preserved byte for byte")
		void unrelatedTagsPreserved() {
			String decl = declaration(" * @param {String} a the first parameter", " * @type {UUID}",
					" * @return {UUID}", " * @private", PROPERTIES);
			assertEquals(declaration(" * @param {String} a the first parameter", " * @private", PROPERTIES),
					CalculationUuidMarker.setUuidMarker(decl, false));
		}

		@Test
		@DisplayName("a UUID tag in a later comment (outside the first block) is not removed")
		void laterCommentIsKept() {
			String decl = "/**\n" + PROPERTIES
					+ "\n */\nfunction calc_uuid() {\n\t/** @return {UUID} */\n\treturn null;\n}";
			assertEquals(decl, CalculationUuidMarker.setUuidMarker(decl, false));
		}

		@Test
		@DisplayName("CRLF is kept when removing the marker")
		void removeKeepsCrlf() {
			assertEquals(crlf(declaration(PROPERTIES)),
					CalculationUuidMarker.setUuidMarker(crlf(declaration(" * @type {UUID}", " *", PROPERTIES)), false));
		}
	}
}
