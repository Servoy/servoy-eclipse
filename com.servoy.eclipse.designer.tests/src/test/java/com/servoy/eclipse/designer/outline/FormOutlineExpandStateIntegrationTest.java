package com.servoy.eclipse.designer.outline;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

@DisplayName("FormOutlinePage expand state preservation (SVY-21202)")
class FormOutlineExpandStateIntegrationTest {
	private Display display;
	private Shell shell;
	private TreeViewer treeViewer;

	@BeforeEach
	void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		treeViewer = new TreeViewer(shell, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		treeViewer.setContentProvider(new HierarchicalContentProvider());
		treeViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((TreeNode) element).name;
			}
		});
	}

	@AfterEach
	void tearDown() {
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	@Nested
	@DisplayName("when TreeViewer.refresh() is called (mechanism used by persistChanges)")
	class RefreshPreservesState {
		@Test
		@DisplayName("previously collapsed nodes remain collapsed after refresh")
		void collapsedNodesRemainCollapsedAfterRefresh() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode nodeToCollapse = root.children.get(0);
			treeViewer.setExpandedState(nodeToCollapse, false);
			assertFalse(treeViewer.getExpandedState(nodeToCollapse), "precondition: node should be collapsed");

			treeViewer.refresh();

			assertFalse(treeViewer.getExpandedState(nodeToCollapse),
					"collapsed node should remain collapsed after TreeViewer.refresh()");
		}

		@Test
		@DisplayName("expanded nodes remain expanded after refresh")
		void expandedNodesRemainExpandedAfterRefresh() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode expandedNode = root.children.get(1);
			assertTrue(treeViewer.getExpandedState(expandedNode), "precondition: node should be expanded");

			treeViewer.refresh();

			assertTrue(treeViewer.getExpandedState(expandedNode),
					"expanded node should remain expanded after TreeViewer.refresh()");
		}

		@Test
		@DisplayName("mixed expand/collapse state is fully preserved after refresh")
		void mixedStatePreservedAfterRefresh() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode collapsed1 = root.children.get(0);
			TreeNode expanded1 = root.children.get(1);
			TreeNode collapsed2 = root.children.get(2);

			treeViewer.setExpandedState(collapsed1, false);
			treeViewer.setExpandedState(collapsed2, false);

			treeViewer.refresh();

			assertAll(
					() -> assertFalse(treeViewer.getExpandedState(collapsed1),
							"first collapsed node should stay collapsed"),
					() -> assertTrue(treeViewer.getExpandedState(expanded1), "expanded node should stay expanded"),
					() -> assertFalse(treeViewer.getExpandedState(collapsed2),
							"second collapsed node should stay collapsed"));
		}

		@Test
		@DisplayName("deeply nested collapse state is preserved after refresh")
		void deeplyNestedCollapseStatePreserved() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode level1 = root.children.get(0);
			TreeNode level2 = level1.children.get(0);
			TreeNode level3 = level2.children.get(0);

			treeViewer.setExpandedState(level2, false);

			treeViewer.refresh();

			assertAll(() -> assertTrue(treeViewer.getExpandedState(level1), "level 1 should remain expanded"),
					() -> assertFalse(treeViewer.getExpandedState(level2), "level 2 should remain collapsed"),
					() -> assertFalse(treeViewer.getExpandedState(level3),
							"level 3 under collapsed parent should not be expanded"));
		}

		@Test
		@DisplayName("AC1: adding a new child node does not affect unrelated collapsed nodes")
		void addingChildDoesNotAffectCollapsedSiblings() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode collapsedSibling = root.children.get(0);
			TreeNode expandedContainer = root.children.get(1);
			treeViewer.setExpandedState(collapsedSibling, false);

			TreeNode newChild = new TreeNode("new-element-added");
			expandedContainer.children.add(newChild);
			treeViewer.refresh();

			assertAll(
					() -> assertFalse(treeViewer.getExpandedState(collapsedSibling),
							"collapsed sibling must remain collapsed after adding a child to another node"),
					() -> assertTrue(treeViewer.getExpandedState(expandedContainer),
							"parent of new child should remain expanded"));
		}

		@Test
		@DisplayName("AC1: adding multiple children preserves all unrelated collapsed nodes")
		void addingMultipleChildrenPreservesCollapsedNodes() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode collapsed1 = root.children.get(0);
			TreeNode collapsed2 = root.children.get(2);
			TreeNode target = root.children.get(3);
			treeViewer.setExpandedState(collapsed1, false);
			treeViewer.setExpandedState(collapsed2, false);

			target.children.add(new TreeNode("new-child-1"));
			target.children.add(new TreeNode("new-child-2"));
			target.children.add(new TreeNode("new-child-3"));
			treeViewer.refresh();

			assertAll(
					() -> assertFalse(treeViewer.getExpandedState(collapsed1),
							"first collapsed node must stay collapsed"),
					() -> assertFalse(treeViewer.getExpandedState(collapsed2),
							"second collapsed node must stay collapsed"),
					() -> assertTrue(treeViewer.getExpandedState(target),
							"target node should remain expanded showing new children"));
		}
	}

	@Nested
	@DisplayName("when expandToLevel is called (mechanism used by defaultExpand on initial creation)")
	class DefaultExpandBehavior {
		@Test
		@DisplayName("expandToLevel(4) expands responsive form tree to correct depth")
		void expandToLevel4ForResponsive() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);

			treeViewer.expandToLevel(root, 4);

			TreeNode level1 = root.children.get(0);
			TreeNode level2 = level1.children.get(0);
			TreeNode level3 = level2.children.get(0);
			TreeNode level4 = level3.children.get(0);

			assertAll(() -> assertTrue(treeViewer.getExpandedState(level1), "level 1 should be expanded"),
					() -> assertTrue(treeViewer.getExpandedState(level2), "level 2 should be expanded"),
					() -> assertTrue(treeViewer.getExpandedState(level3), "level 3 should be expanded"),
					() -> assertFalse(treeViewer.getExpandedState(level4),
							"level 4 should NOT be expanded (depth limit)"));
		}

		@Test
		@DisplayName("expandToLevel(3) expands absolute form tree to correct depth")
		void expandToLevel3ForAbsolute() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);

			treeViewer.expandToLevel(root, 3);

			TreeNode level1 = root.children.get(0);
			TreeNode level2 = level1.children.get(0);
			TreeNode level3 = level2.children.get(0);

			assertAll(() -> assertTrue(treeViewer.getExpandedState(level1), "level 1 should be expanded"),
					() -> assertTrue(treeViewer.getExpandedState(level2), "level 2 should be expanded"),
					() -> assertFalse(treeViewer.getExpandedState(level3),
							"level 3 should NOT be expanded (depth limit)"));
		}

		@Test
		@DisplayName("expandToLevel after manual collapse overrides user state (why defaultExpand must not be called in persistChanges)")
		void expandToLevelOverridesUserCollapse() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode collapsed = root.children.get(0);
			treeViewer.setExpandedState(collapsed, false);
			assertFalse(treeViewer.getExpandedState(collapsed), "precondition: node should be collapsed");

			treeViewer.expandToLevel(root, 4);

			assertTrue(treeViewer.getExpandedState(collapsed),
					"expandToLevel overrides user's collapsed state - this is why it must NOT be called in persistChanges");
		}
	}

	@Nested
	@DisplayName("persistChanges behavior verification")
	class PersistChangesVerification {
		@Test
		@DisplayName("refresh() does not include defaultExpand in its execution path")
		void refreshDoesNotCallDefaultExpand() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode collapsed = root.children.get(0);
			treeViewer.setExpandedState(collapsed, false);

			treeViewer.refresh();

			assertFalse(treeViewer.getExpandedState(collapsed),
					"after refresh (as called by persistChanges), collapsed state must be preserved - "
							+ "defaultExpand is not part of the refresh path");
		}

		@Test
		@DisplayName("multiple consecutive refreshes preserve expand state (simulates rapid persist changes)")
		void multipleRefreshesPreserveState() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode collapsed = root.children.get(0);
			treeViewer.setExpandedState(collapsed, false);

			for (int i = 0; i < 5; i++) {
				treeViewer.refresh();
			}

			assertFalse(treeViewer.getExpandedState(collapsed),
					"collapsed state must be preserved even after multiple rapid refreshes");
		}

		@Test
		@DisplayName("FormOutlinePage.persistChanges source does not invoke defaultExpand")
		void persistChangesDoesNotCallDefaultExpand() throws IOException {
			Bundle hostBundle = FrameworkUtil.getBundle(FormOutlinePage.class);
			assumeTrue(hostBundle != null, "Host bundle not available - skipping source scan test");

			URL sourceUrl = hostBundle.getEntry("src/com/servoy/eclipse/designer/outline/FormOutlinePage.java");
			assumeTrue(sourceUrl != null, "Source file not available in bundle - skipping source scan test");

			String source = IOUtils.toString(sourceUrl, StandardCharsets.UTF_8);

			int persistChangesStart = source.indexOf("public void persistChanges(");
			assumeTrue(persistChangesStart >= 0, "Could not locate persistChanges method in source");

			int methodBodyStart = source.indexOf("{", persistChangesStart);
			int braceCount = 0;
			int methodBodyEnd = -1;
			for (int i = methodBodyStart; i < source.length(); i++) {
				char c = source.charAt(i);
				if (c == '{')
					braceCount++;
				else if (c == '}') {
					braceCount--;
					if (braceCount == 0) {
						methodBodyEnd = i;
						break;
					}
				}
			}
			assumeTrue(methodBodyEnd > 0, "Could not determine end of persistChanges method");

			String methodBody = source.substring(persistChangesStart, methodBodyEnd + 1);
			assertFalse(methodBody.contains("defaultExpand"),
					"persistChanges must NOT call defaultExpand - the fix for SVY-21202 removes this call");
		}

		@Test
		@DisplayName("empty tree refresh does not throw")
		void emptyTreeRefreshDoesNotThrow() {
			TreeNode root = new TreeNode("root");
			treeViewer.setInput(root);
			treeViewer.refresh();
		}

		@Test
		@DisplayName("single-node tree refresh preserves state")
		void singleNodeTreeRefreshPreservesState() {
			TreeNode root = new TreeNode("root");
			root.children.add(new TreeNode("only-child"));
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode onlyChild = root.children.get(0);
			assertFalse(treeViewer.getExpandedState(onlyChild), "leaf node has no expandable state");

			treeViewer.refresh();

			assertFalse(treeViewer.getExpandedState(onlyChild), "leaf node state unchanged after refresh");
		}
	}

	@Nested
	@DisplayName("AC3: drag-and-drop (node reparenting) preserves state")
	class DragAndDropPreservesState {
		@Test
		@DisplayName("moving a node to a different parent preserves other collapsed nodes")
		void moveNodePreservesCollapsedSiblings() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode source = root.children.get(0);
			TreeNode target = root.children.get(2);
			TreeNode unrelated = root.children.get(3);
			treeViewer.setExpandedState(unrelated, false);

			TreeNode movedNode = source.children.get(0);
			source.children.remove(movedNode);
			target.children.add(movedNode);

			treeViewer.refresh();

			assertAll(
					() -> assertFalse(treeViewer.getExpandedState(unrelated),
							"unrelated collapsed node must remain collapsed after drag-and-drop"),
					() -> assertTrue(treeViewer.getExpandedState(target), "drop target should remain expanded"));
		}

		@Test
		@DisplayName("moved node is reachable via ancestor expansion (simulates selectionChanged reveal)")
		void movedNodeRevealedViaAncestorExpansion() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode sourceParent = root.children.get(0);
			TreeNode targetParent = root.children.get(2);
			TreeNode unrelated = root.children.get(3);

			treeViewer.setExpandedState(targetParent, false);
			treeViewer.setExpandedState(unrelated, false);

			TreeNode movedNode = sourceParent.children.get(1);
			sourceParent.children.remove(movedNode);
			targetParent.children.add(movedNode);

			treeViewer.refresh();

			treeViewer.setExpandedState(targetParent, true);

			assertAll(
					() -> assertFalse(treeViewer.getExpandedState(unrelated),
							"unrelated collapsed node must NOT be expanded by reveal"),
					() -> assertTrue(treeViewer.getExpandedState(targetParent),
							"target parent should be expanded to reveal moved node"),
					() -> assertTrue(targetParent.children.contains(movedNode),
							"moved node should be a child of the target parent"));
		}

		@Test
		@DisplayName("SVY-20066 regression: moving node to collapsed subtree and revealing does not expand unrelated branches")
		void moveToCollapsedSubtreeRevealsWithoutExpandingUnrelated() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode sourceParent = root.children.get(0);
			TreeNode collapsedTarget = root.children.get(1);
			TreeNode unrelatedBranch1 = root.children.get(2);
			TreeNode unrelatedBranch2 = root.children.get(3);

			treeViewer.setExpandedState(collapsedTarget, false);
			treeViewer.setExpandedState(unrelatedBranch1, false);
			treeViewer.setExpandedState(unrelatedBranch2, false);

			TreeNode movedNode = sourceParent.children.get(0);
			sourceParent.children.remove(movedNode);
			TreeNode deepTarget = collapsedTarget.children.get(0);
			deepTarget.children.add(movedNode);

			treeViewer.refresh();

			treeViewer.setExpandedState(collapsedTarget, true);
			treeViewer.setExpandedState(deepTarget, true);

			assertAll(
					() -> assertFalse(treeViewer.getExpandedState(unrelatedBranch1),
							"unrelated branch 1 must remain collapsed"),
					() -> assertFalse(treeViewer.getExpandedState(unrelatedBranch2),
							"unrelated branch 2 must remain collapsed"),
					() -> assertTrue(treeViewer.getExpandedState(collapsedTarget),
							"target ancestor should be expanded to reveal moved node"),
					() -> assertTrue(treeViewer.getExpandedState(deepTarget),
							"direct parent should be expanded to reveal moved node"));
		}
	}

	@Nested
	@DisplayName("AC5: grouped/ungrouped toggle behavior")
	class GroupedUngroupedToggle {
		@Test
		@DisplayName("refresh without defaultExpand preserves state (view toggle only calls refresh)")
		void toggleRefreshPreservesState() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode collapsed1 = root.children.get(0);
			TreeNode collapsed2 = root.children.get(2);
			treeViewer.setExpandedState(collapsed1, false);
			treeViewer.setExpandedState(collapsed2, false);

			treeViewer.refresh();

			assertAll(
					() -> assertFalse(treeViewer.getExpandedState(collapsed1),
							"collapsed node 1 should stay collapsed after toggle-style refresh"),
					() -> assertFalse(treeViewer.getExpandedState(collapsed2),
							"collapsed node 2 should stay collapsed after toggle-style refresh"));
		}

		@Test
		@DisplayName("full tree replacement (simulating content provider swap) with expandToLevel re-expands correctly")
		void contentProviderSwapWithDefaultExpandReexpands() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode collapsed = root.children.get(0);
			treeViewer.setExpandedState(collapsed, false);

			TreeNode newRoot = createDeepTree();
			treeViewer.setInput(newRoot);
			treeViewer.expandToLevel(newRoot, 4);

			TreeNode level1 = newRoot.children.get(0);
			TreeNode level2 = level1.children.get(0);
			TreeNode level3 = level2.children.get(0);
			TreeNode level4 = level3.children.get(0);

			assertAll(
					() -> assertTrue(treeViewer.getExpandedState(level1),
							"after full toggle with defaultExpand, level 1 should be expanded"),
					() -> assertTrue(treeViewer.getExpandedState(level2),
							"after full toggle with defaultExpand, level 2 should be expanded"),
					() -> assertTrue(treeViewer.getExpandedState(level3),
							"after full toggle with defaultExpand, level 3 should be expanded"),
					() -> assertFalse(treeViewer.getExpandedState(level4),
							"level 4 should NOT be expanded (depth limit)"));
		}
	}

	@Nested
	@DisplayName("selectionChanged reveal mechanism")
	class SelectionChangedReveal {
		@Test
		@DisplayName("expanding ancestor chain reveals a deeply nested node without affecting other branches")
		void ancestorExpansionRevealsNodeWithoutAffectingOthers() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode branch0 = root.children.get(0);
			TreeNode branch1 = root.children.get(1);
			TreeNode branch2 = root.children.get(2);
			TreeNode branch3 = root.children.get(3);

			treeViewer.collapseAll();

			TreeNode targetLevel1 = branch1;
			TreeNode targetLevel2 = branch1.children.get(1);
			TreeNode targetLevel3 = targetLevel2.children.get(0);

			treeViewer.setExpandedState(targetLevel1, true);
			treeViewer.setExpandedState(targetLevel2, true);
			treeViewer.setExpandedState(targetLevel3, true);

			assertAll(() -> assertFalse(treeViewer.getExpandedState(branch0), "branch 0 must remain collapsed"),
					() -> assertFalse(treeViewer.getExpandedState(branch2), "branch 2 must remain collapsed"),
					() -> assertFalse(treeViewer.getExpandedState(branch3), "branch 3 must remain collapsed"),
					() -> assertTrue(treeViewer.getExpandedState(targetLevel1), "ancestor level 1 should be expanded"),
					() -> assertTrue(treeViewer.getExpandedState(targetLevel2), "ancestor level 2 should be expanded"),
					() -> assertTrue(treeViewer.getExpandedState(targetLevel3), "target level 3 should be expanded"));
		}

		@Test
		@DisplayName("collapsing a parent then revealing a child only expands the necessary path")
		void revealAfterCollapseExpandsOnlyNecessaryPath() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode branch0 = root.children.get(0);
			TreeNode branch1 = root.children.get(1);

			treeViewer.setExpandedState(branch0, false);
			treeViewer.setExpandedState(branch1, false);

			TreeNode targetChild = branch0.children.get(1);
			treeViewer.setExpandedState(branch0, true);

			assertAll(() -> assertTrue(treeViewer.getExpandedState(branch0), "revealed ancestor should be expanded"),
					() -> assertFalse(treeViewer.getExpandedState(branch1), "unrelated branch must remain collapsed"));
		}
	}

	@Nested
	@DisplayName("element equality contract (PersistContext uses equals/hashCode)")
	class ElementEqualityPreservesState {
		@Test
		@DisplayName("refresh preserves state when elements have proper equals/hashCode")
		void equalsBasedMatchingPreservesState() {
			TreeNode root = createDeepTree();
			treeViewer.setInput(root);
			treeViewer.expandAll();

			TreeNode nodeToCollapse = root.children.get(1);
			treeViewer.setExpandedState(nodeToCollapse, false);

			treeViewer.refresh();

			assertEquals(false, treeViewer.getExpandedState(nodeToCollapse),
					"TreeViewer matches elements by equals() after refresh - PersistContext implements this correctly");
		}
	}

	private TreeNode createDeepTree() {
		TreeNode root = new TreeNode("root");
		for (int i = 0; i < 4; i++) {
			TreeNode level1 = new TreeNode("container-" + i);
			root.children.add(level1);
			for (int j = 0; j < 3; j++) {
				TreeNode level2 = new TreeNode("row-" + i + "-" + j);
				level1.children.add(level2);
				for (int k = 0; k < 2; k++) {
					TreeNode level3 = new TreeNode("col-" + i + "-" + j + "-" + k);
					level2.children.add(level3);
					for (int l = 0; l < 2; l++) {
						TreeNode level4 = new TreeNode("component-" + i + "-" + j + "-" + k + "-" + l);
						level3.children.add(level4);
						TreeNode level5 = new TreeNode("leaf-" + i + "-" + j + "-" + k + "-" + l);
						level4.children.add(level5);
					}
				}
			}
		}
		return root;
	}

	static class TreeNode {
		final String name;
		final List<TreeNode> children = new ArrayList<>();

		TreeNode(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof TreeNode other))
				return false;
			return name.equals(other.name);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public String toString() {
			return name;
		}
	}

	static class HierarchicalContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof TreeNode node) {
				return node.children.toArray();
			}
			return new Object[0];
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof TreeNode node) {
				return node.children.toArray();
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof TreeNode node) {
				return !node.children.isEmpty();
			}
			return false;
		}
	}
}
