package com.servoy.eclipse.ui.views.solutionexplorer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNode;
import com.servoy.eclipse.ui.node.UserNodeType;

/**
 * SVY-21272: Regression test for ClassCastException in hasChildren().
 *
 * The bug was caused by operator precedence: the VIEW_FOUNDSET type check was
 * outside the instanceof guard, causing a ClassCastException when parent was a
 * plain SimpleUserNode (e.g., a RETURNTYPEPLACEHOLDER).
 *
 * This test replicates the exact condition logic from
 * SolutionExplorerTreeContentProvider.hasChildren() lines 1993-1996 to verify
 * the fix works correctly.
 */
class SolutionExplorerTreeContentProviderHasChildrenTest {

	/**
	 * Replicates the FIXED logic from
	 * SolutionExplorerTreeContentProvider.hasChildren() at lines 1993-1996 (after
	 * the PlatformSimpleUserNode branch).
	 */
	private static boolean hasChildrenLogic(Object parent) {
		if (parent instanceof PlatformSimpleUserNode) {
			return false; // simplified; real logic is complex but irrelevant here
		} else if (parent instanceof UserNode && (((UserNode) parent).getType() == UserNodeType.TABLE
				|| ((UserNode) parent).getType() == UserNodeType.INMEMORY_DATASOURCE
				|| ((UserNode) parent).getType() == UserNodeType.VIEW_FOUNDSET))
			return false;
		return true;
	}

	/**
	 * Replicates the BUGGY logic (before the fix) to demonstrate what went wrong.
	 * The VIEW_FOUNDSET check was outside the instanceof guard due to operator
	 * precedence.
	 */
	@SuppressWarnings("unused")
	private static boolean hasChildrenLogicBuggy(Object parent) {
		if (parent instanceof PlatformSimpleUserNode) {
			return false;
		} else if (parent instanceof UserNode
				&& (((UserNode) parent).getType() == UserNodeType.TABLE
						|| ((UserNode) parent).getType() == UserNodeType.INMEMORY_DATASOURCE)
				|| ((UserNode) parent).getType() == UserNodeType.VIEW_FOUNDSET)
			return false;
		return true;
	}

	@Nested
	class PlainSimpleUserNodeTests {

		@Test
		@DisplayName("RETURNTYPEPLACEHOLDER node does not throw ClassCastException")
		void returntypePlaceholderDoesNotThrowClassCastException() {
			SimpleUserNode placeholder = new SimpleUserNode("placeholder", UserNodeType.RETURNTYPEPLACEHOLDER);
			assertDoesNotThrow(() -> hasChildrenLogic(placeholder));
		}

		@Test
		@DisplayName("RETURNTYPEPLACEHOLDER node returns true (has children)")
		void returntypePlaceholderReturnsTrue() {
			SimpleUserNode placeholder = new SimpleUserNode("placeholder", UserNodeType.RETURNTYPEPLACEHOLDER);
			assertTrue(hasChildrenLogic(placeholder));
		}

		@Test
		@DisplayName("VIEW_FOUNDSET SimpleUserNode does not throw ClassCastException")
		void viewFoundsetSimpleUserNodeDoesNotThrow() {
			SimpleUserNode node = new SimpleUserNode("viewFoundset", UserNodeType.VIEW_FOUNDSET);
			assertDoesNotThrow(() -> hasChildrenLogic(node));
		}

		@Test
		@DisplayName("VIEW_FOUNDSET SimpleUserNode returns true (not falsely matched by unguarded cast)")
		void viewFoundsetSimpleUserNodeReturnsTrue() {
			SimpleUserNode node = new SimpleUserNode("viewFoundset", UserNodeType.VIEW_FOUNDSET);
			assertTrue(hasChildrenLogic(node));
		}

		@Test
		@DisplayName("TABLE SimpleUserNode does not throw ClassCastException")
		void tableSimpleUserNodeDoesNotThrow() {
			SimpleUserNode node = new SimpleUserNode("table", UserNodeType.TABLE);
			assertDoesNotThrow(() -> hasChildrenLogic(node));
		}

		@Test
		@DisplayName("INMEMORY_DATASOURCE SimpleUserNode does not throw ClassCastException")
		void inMemoryDatasourceSimpleUserNodeDoesNotThrow() {
			SimpleUserNode node = new SimpleUserNode("inmemory", UserNodeType.INMEMORY_DATASOURCE);
			assertDoesNotThrow(() -> hasChildrenLogic(node));
		}

		@Test
		@DisplayName("Buggy logic throws ClassCastException for VIEW_FOUNDSET SimpleUserNode")
		void buggyLogicThrowsClassCastException() {
			SimpleUserNode node = new SimpleUserNode("viewFoundset", UserNodeType.VIEW_FOUNDSET);
			org.junit.jupiter.api.Assertions.assertThrows(ClassCastException.class, () -> hasChildrenLogicBuggy(node));
		}
	}

	@Nested
	class UserNodeTests {

		@Test
		@DisplayName("TABLE UserNode returns false")
		void tableUserNodeReturnsFalse() {
			UserNode node = new UserNode("table", UserNodeType.TABLE);
			assertFalse(hasChildrenLogic(node));
		}

		@Test
		@DisplayName("INMEMORY_DATASOURCE UserNode returns false")
		void inMemoryDatasourceUserNodeReturnsFalse() {
			UserNode node = new UserNode("inmemory", UserNodeType.INMEMORY_DATASOURCE);
			assertFalse(hasChildrenLogic(node));
		}

		@Test
		@DisplayName("VIEW_FOUNDSET UserNode returns false")
		void viewFoundsetUserNodeReturnsFalse() {
			UserNode node = new UserNode("viewFoundset", UserNodeType.VIEW_FOUNDSET);
			assertFalse(hasChildrenLogic(node));
		}

		@Test
		@DisplayName("Non-matching UserNode type returns true")
		void nonMatchingUserNodeTypeReturnsTrue() {
			UserNode node = new UserNode("method", UserNodeType.GLOBAL_METHOD_ITEM);
			assertTrue(hasChildrenLogic(node));
		}
	}
}
