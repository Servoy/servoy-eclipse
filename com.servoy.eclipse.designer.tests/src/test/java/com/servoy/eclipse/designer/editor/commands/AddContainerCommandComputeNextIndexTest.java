/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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
package com.servoy.eclipse.designer.editor.commands;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.lang.reflect.Constructor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.j2db.persistence.CSSPositionLayoutContainer;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.util.UUID;

/**
 * SVY-21386: verifies the ordering-counter helper
 * {@link AddContainerCommand#computeNextLayoutContainerIndex(com.servoy.j2db.persistence.IPersist)}
 * that both the Outline-add path and the palette bottom-drop path now share.
 *
 * The palette bottom-drop fix routes a no-right-sibling responsive drop through
 * this exact helper, so a new container receives
 * {@code new Point(index, index)} with {@code index == max(sibling x,y) + 1}
 * instead of a raw pixel coordinate. These tests pin that computation
 * (including the CSS-position-ancestor child-count branch) against the repro
 * coordinates.
 *
 * @author test-gen
 */
class AddContainerCommandComputeNextIndexTest {
	private static LayoutContainer newLayoutContainer(Point location) throws Exception {
		Constructor<LayoutContainer> ctor = LayoutContainer.class.getDeclaredConstructor(ISupportChilds.class,
				UUID.class);
		ctor.setAccessible(true);
		LayoutContainer lc = ctor.newInstance(null, UUID.randomUUID());
		lc.setLocation(location);
		return lc;
	}

	private static CSSPositionLayoutContainer newCSSPositionLayoutContainer() throws Exception {
		Constructor<CSSPositionLayoutContainer> ctor = CSSPositionLayoutContainer.class
				.getDeclaredConstructor(ISupportChilds.class, UUID.class);
		ctor.setAccessible(true);
		return ctor.newInstance(null, UUID.randomUUID());
	}

	private static Form newForm() throws Exception {
		Constructor<Form> ctor = Form.class.getDeclaredConstructor(ISupportChilds.class, UUID.class);
		ctor.setAccessible(true);
		return ctor.newInstance(null, UUID.randomUUID());
	}

	@Nested
	class MaxPlusOneBranch {
		/**
		 * AC1/AC2/AC6: for the repro parent holding three rows with counters (400,364),
		 * (384,248), (392,98) the helper returns 401 = max(all x and y) + 1, and
		 * (index, index) is strictly greater than every sibling's x AND y so the new
		 * row sorts last under either X-first or Y-first ordering.
		 */
		@Test
		@DisplayName("returns max(sibling x,y)+1 for the repro rows and produces a counter above every sibling")
		void returnsMaxPlusOneOverReproRows() throws Exception {
			Form form = newForm();
			LayoutContainer[] rows = { newLayoutContainer(new Point(400, 364)), newLayoutContainer(new Point(384, 248)),
					newLayoutContainer(new Point(392, 98)) };
			for (LayoutContainer row : rows) {
				form.internalAddChild(row);
			}

			int index = AddContainerCommand.computeNextLayoutContainerIndex(form);

			assertEquals(401, index, "index must be max(400,364,384,248,392,98)+1 = 401");
			for (LayoutContainer row : rows) {
				Point loc = row.getLocation();
				assertAll("new counter (" + index + ") strictly above sibling " + loc,
						() -> assertTrue(index > loc.x, "index must exceed sibling x " + loc.x),
						() -> assertTrue(index > loc.y, "index must exceed sibling y " + loc.y));
			}
		}

		/**
		 * A single sibling at (10, 10) yields 11; proves the +1 and the max over both
		 * axes with one child.
		 */
		@Test
		@DisplayName("single sibling yields its max axis + 1")
		void singleSiblingYieldsMaxPlusOne() throws Exception {
			Form form = newForm();
			form.internalAddChild(newLayoutContainer(new Point(7, 10)));

			assertEquals(11, AddContainerCommand.computeNextLayoutContainerIndex(form),
					"index must be max(7,10)+1 = 11");
		}

		/**
		 * An empty parent yields 1 (maxLocation stays 0, +1).
		 */
		@Test
		@DisplayName("empty parent yields 1")
		void emptyParentYieldsOne() throws Exception {
			assertEquals(1, AddContainerCommand.computeNextLayoutContainerIndex(newForm()),
					"an empty parent must yield 1");
		}
	}

	@Nested
	class CssPositionAncestorBranch {
		/**
		 * AC5: when the parent has a CSS-position-layout-container ancestor (here the
		 * parent itself is one) the helper returns the child count instead of max+1,
		 * matching the Outline path's CSS-position special case. The children carry
		 * deliberately large/small counters to prove the max+1 loop is NOT used here.
		 */
		@Test
		@DisplayName("CSS-position container parent returns child count, not max+1")
		void cssPositionParentReturnsChildCount() throws Exception {
			CSSPositionLayoutContainer parent = newCSSPositionLayoutContainer();
			parent.internalAddChild(newLayoutContainer(new Point(900, 900)));
			parent.internalAddChild(newLayoutContainer(new Point(5, 5)));

			int index = AddContainerCommand.computeNextLayoutContainerIndex(parent);

			assertEquals(2, index, "CSS-position ancestor branch must return the child count (2), not max+1 (901)");
		}

		/**
		 * An empty CSS-position container returns 0 children (the first dropped child
		 * gets counter 0).
		 */
		@Test
		@DisplayName("empty CSS-position container returns 0")
		void emptyCssPositionParentReturnsZero() throws Exception {
			assertEquals(0, AddContainerCommand.computeNextLayoutContainerIndex(newCSSPositionLayoutContainer()),
					"an empty CSS-position container must return a child count of 0");
		}
	}
}
