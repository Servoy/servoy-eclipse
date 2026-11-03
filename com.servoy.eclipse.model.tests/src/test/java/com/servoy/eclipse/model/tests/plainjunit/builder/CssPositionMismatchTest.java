/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.model.tests.plainjunit.builder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.servoy.eclipse.model.builder.ServoyFormBuilder;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.util.PersistHelper;

/**
 * SVY-21669 - plain-logic test for the cssPosition mismatch detection used by
 * the new ServoyBuilder warning marker.
 *
 * <p>
 * The marker fires when a web component's authoritative compact
 * {@code cssPosition} string decodes to values that disagree with the present
 * fields of its {@code json.cssPosition} echo object. The
 * comparison/normalization logic lives in the public static method
 * {@link ServoyFormBuilder#cssPositionDisagrees(CSSPosition, JSONObject)} - a
 * pure function with no OSGi/workspace dependency - so it is called here
 * directly as a {@code plainjunit} test.
 * </p>
 *
 * <p>
 * <b>Compact-string order:</b> compact strings are decoded with
 * {@link PersistHelper#createCSSPosition(String)}, which tokenizes in the
 * intentional, stable order <i>top, right, bottom, left, width, height</i>
 * (SVY-12927 / commit {@code ae4f6aeb2}). Tests therefore encode their input
 * strings in exactly that order, so that a right/bottom swap in the string is a
 * genuine swap and not an artifact of the test setup.
 * </p>
 */
class CssPositionMismatchTest {

	private static boolean disagrees(CSSPosition stringPos, JSONObject jsonPos) {
		return ServoyFormBuilder.cssPositionDisagrees(stringPos, jsonPos);
	}

	/**
	 * Build the authoritative CSSPosition from a compact string in
	 * top,right,bottom,left,width,height order.
	 */
	private static CSSPosition pos(String compact) {
		return PersistHelper.createCSSPosition(compact);
	}

	private static JSONObject json(int top, int right, int bottom, int left, int width, int height) {
		return new JSONObject().put("top", top).put("right", right).put("bottom", bottom).put("left", left)
				.put("width", width).put("height", height);
	}

	@Nested
	@DisplayName("mismatches are flagged")
	class Mismatches {

		@Test
		@DisplayName("run-011 lbl_customerid: right/bottom swapped in the json echo is flagged")
		void rightBottomSwapIsFlagged() {
			// compact string decodes to top=8, right=-1, bottom=12, left=-1, width=100,
			// height=20
			CSSPosition stringPos = pos("8,-1,12,-1,100,20");
			// json swaps right<->bottom: right=12, bottom=-1
			JSONObject jsonPos = json(8, 12, -1, -1, 100, 20);
			assertTrue(disagrees(stringPos, jsonPos),
					"a json echo that swaps right and bottom relative to the authoritative string must be flagged");
		}

		@Test
		@DisplayName("width/height swapped (list-container case) is flagged")
		void widthHeightSwapIsFlagged() {
			CSSPosition stringPos = pos("0,-1,-1,0,300,500");
			// json swaps width<->height: width=500, height=300
			JSONObject jsonPos = json(0, -1, -1, 0, 500, 300);
			assertTrue(disagrees(stringPos, jsonPos), "a json echo that swaps width and height must be flagged");
		}

		@Test
		@DisplayName("a single disagreeing field is enough to flag")
		void singleFieldDisagreementIsFlagged() {
			CSSPosition stringPos = pos("8,-1,12,-1,100,20");
			// only 'top' differs (9 vs 8); everything else matches the decoded string
			JSONObject jsonPos = json(9, -1, 12, -1, 100, 20);
			assertTrue(disagrees(stringPos, jsonPos), "a single present field that disagrees must flag the mismatch");
		}

		@ParameterizedTest(name = "field \"{0}\" disagreeing is flagged")
		@CsvSource({ "top", "right", "bottom", "left", "width", "height" })
		@DisplayName("each compared field, when it alone disagrees, is flagged")
		void eachFieldDisagreementIsFlagged(String field) {
			CSSPosition stringPos = pos("8,10,12,14,100,20");
			JSONObject jsonPos = json(8, 10, 12, 14, 100, 20); // all matching to start
			jsonPos.put(field, 999); // perturb exactly one field
			assertTrue(disagrees(stringPos, jsonPos), "perturbing only '" + field + "' must flag the mismatch");
		}
	}

	@Nested
	@DisplayName("matching representations are not flagged")
	class NoMismatch {

		@Test
		@DisplayName("identical values (all six fields) are not flagged")
		void identicalValuesNotFlagged() {
			CSSPosition stringPos = pos("8,-1,12,-1,100,20");
			JSONObject jsonPos = json(8, -1, 12, -1, 100, 20);
			assertFalse(disagrees(stringPos, jsonPos),
					"a json echo that matches the string on every field must not flag");
		}

		@Test
		@DisplayName("numeric normalization: string \"8\" equals json string \"8\" and number 8")
		void numericNormalizationNotFlagged() {
			CSSPosition stringPos = pos("8,-1,12,-1,100,20");
			// mix json number 8 and json string "8" / "100" - must normalize to equal
			JSONObject jsonPos = new JSONObject().put("top", "8").put("right", -1).put("bottom", "12").put("left", -1)
					.put("width", 100).put("height", "20");
			assertFalse(disagrees(stringPos, jsonPos),
					"8 vs \"8\" (and 20 vs \"20\") must be treated as equal, not a mismatch");
		}

		@Test
		@DisplayName("numeric normalization: 8 vs 8.0 is not flagged")
		void decimalNormalizationNotFlagged() {
			CSSPosition stringPos = pos("8,-1,12,-1,100,20");
			JSONObject jsonPos = new JSONObject().put("top", 8.0d).put("right", -1).put("bottom", 12).put("left", -1)
					.put("width", 100).put("height", 20);
			assertFalse(disagrees(stringPos, jsonPos), "8 vs 8.0 must be treated as numerically equal");
		}

		@Test
		@DisplayName("a null string field matched by a null json field is not flagged")
		void bothNullFieldNotFlagged() {
			// authoritative right is null; json right is also JSON null -> they agree
			CSSPosition stringPos = new CSSPosition("8", null, "12", "-1", "100", "20");
			JSONObject jsonPos = new JSONObject().put("top", 8).put("right", JSONObject.NULL).put("bottom", 12)
					.put("left", -1).put("width", 100).put("height", 20);
			assertFalse(disagrees(stringPos, jsonPos),
					"a null string field matched by a null json field must not flag");
		}
	}

	@Nested
	class OutOfSyncShape {

		@Test
		@DisplayName("partial json (a field the string has is missing from json) is flagged")
		void partialJsonIsFlagged() {
			CSSPosition stringPos = pos("8,-1,12,-1,100,20");
			// only top and width present; the string has real values for the other four, so
			// the json is out of sync
			JSONObject jsonPos = new JSONObject().put("top", 8).put("width", 100);
			assertTrue(disagrees(stringPos, jsonPos),
					"a json missing fields that the authoritative string has must be flagged");
		}

		@Test
		@DisplayName("empty json cssPosition object is flagged")
		void emptyJsonIsFlagged() {
			CSSPosition stringPos = pos("8,-1,12,-1,100,20");
			assertTrue(disagrees(stringPos, new JSONObject()),
					"an empty json cssPosition while the string has values must be flagged");
		}

		@Test
		@DisplayName("a json field set to JSON null while the string has a value is flagged")
		void jsonNullFieldIsFlagged() {
			CSSPosition stringPos = pos("8,-1,12,-1,100,20");
			// right is JSON null but the string has -1 for it -> out of sync
			JSONObject jsonPos = new JSONObject().put("top", 8).put("right", JSONObject.NULL).put("bottom", 12)
					.put("left", -1).put("width", 100).put("height", 20);
			assertTrue(disagrees(stringPos, jsonPos),
					"a json field set to null while the authoritative string has a value must be flagged");
		}

		@Test
		@DisplayName("an extra key beyond the six cssPosition fields is ignored")
		void extraKeyIsIgnored() {
			CSSPosition stringPos = pos("8,-1,12,-1,100,20");
			// the six fields all agree; a stray extra key is not part of the comparison
			JSONObject jsonPos = json(8, -1, 12, -1, 100, 20).put("zIndex", 5);
			assertFalse(disagrees(stringPos, jsonPos),
					"a key beyond the six cssPosition fields must be ignored, not flagged");
		}
	}

	@Nested
	@DisplayName("guards against false positives on absent representations")
	class NullGuards {

		@Test
		@DisplayName("legacy element with no CSSPosition (null string side) is not flagged")
		void nullStringPositionNotFlagged() {
			// legacy size/location element: BaseComponent.getCssPosition() returns null
			assertFalse(disagrees(null, json(8, -1, 12, -1, 100, 20)),
					"a null authoritative CSSPosition (legacy element) must never flag");
		}

		@Test
		@DisplayName("no json cssPosition object (null json side) is not flagged")
		void nullJsonPositionNotFlagged() {
			CSSPosition stringPos = pos("8,-1,12,-1,100,20");
			assertFalse(disagrees(stringPos, null), "a null json cssPosition (no echo) must never flag");
		}

		@Test
		@DisplayName("both sides null is not flagged")
		void bothNullNotFlagged() {
			assertFalse(disagrees(null, null), "null on both sides has nothing to compare, so must not flag");
		}
	}
}
