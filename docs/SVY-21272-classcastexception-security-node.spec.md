# Spec: SVY-21272 â ClassCastException, methods under Security node are not loaded

## 1. Goal

Fix a `ClassCastException` in the Solution Explorer tree that prevents methods from being displayed under the Security, Application, SolutionModel, and EventsManager nodes. The exception is caused by an operator precedence bug in `SolutionExplorerTreeContentProvider.hasChildren()` that performs an unsafe cast to `UserNode` without a proper `instanceof` guard.

## 2. Background

### 2.1 Solution Explorer architecture

The Solution Explorer (Solex) has two panels:
- **Tree panel (left):** Shows a hierarchy of nodes (`PlatformSimpleUserNode` instances) including API Explorer children like Security, Application, SolutionModel, etc.
- **List panel (right):** When a tree node is selected, `SolutionExplorerListContentProvider` populates methods/properties for that node.

### 2.2 Node class hierarchy

```
SimpleUserNode implements IAdaptable
âââ UserNode extends SimpleUserNode          (used for list items â methods, properties)
âââ PlatformSimpleUserNode extends SimpleUserNode  (used for tree nodes)
```

Key point: `UserNode` and `PlatformSimpleUserNode` are **sibling** subclasses â neither extends the other.

### 2.3 Lazy-loading with placeholder nodes

Tree nodes that have return-type sub-nodes (Security, Application, SolutionModel, etc.) use a lazy-loading pattern via `addReturnTypeNodesPlaceHolder()` (line 2548):

```java
node.children = new SimpleUserNode[] {
    new SimpleUserNode("placeholder", UserNodeType.RETURNTYPEPLACEHOLDER, clss, null)
};
```

This placeholder is a **plain `SimpleUserNode`** â not a `PlatformSimpleUserNode`, not a `UserNode`. When the tree viewer calls `getChildren()` on the parent, the placeholder is detected and replaced with proper `PlatformSimpleUserNode` children via `addReturnTypeNodes()`.

### 2.4 The bug

In `SolutionExplorerTreeContentProvider.hasChildren()` at lines 1993â1995:

```java
else if (parent instanceof UserNode &&
    (((UserNode)parent).getType() == UserNodeType.TABLE || ((UserNode)parent).getType() == UserNodeType.INMEMORY_DATASOURCE) ||
    ((UserNode)parent).getType() == UserNodeType.VIEW_FOUNDSET) return false;
```

Due to Java operator precedence (`&&` binds tighter than `||`), this is parsed as:

```
(parent instanceof UserNode && (type == TABLE || type == INMEMORY_DATASOURCE))
||
((UserNode)parent).getType() == VIEW_FOUNDSET
```

The third condition (`VIEW_FOUNDSET` check) is **not guarded** by the `instanceof UserNode` check. When `parent` is a plain `SimpleUserNode` (e.g., the RETURNTYPEPLACEHOLDER), the cast `(UserNode)parent` throws `ClassCastException`.

### 2.5 Reproduction scenario

1. New workspace, import cloudSampleModel from SPM
2. Open Solution Explorer, navigate to Security (or Application, SolutionModel, EventsManager)
3. The tree expansion or selection triggers `hasChildren()` on a `SimpleUserNode`
4. ClassCastException is thrown, disrupting tree viewer state
5. The list panel fails to display methods ("shows only a placeholder")

### 2.6 Git history (regression analysis)

This is a **regression** introduced in commit `512bcb1e959258176d6381d9f8f25a126e776ca2` (2019-03-04) by emera for **SVY-12725** ("View Foundset (readonly) in the developer — small fix, view foundset datasources nodes do not have children").

Before that commit, the code correctly had:
```java
else if (parent instanceof UserNode &&
    (((UserNode)parent).getType() == UserNodeType.TABLE || ((UserNode)parent).getType() == UserNodeType.INMEMORY_DATASOURCE)) return false;
```

The `VIEW_FOUNDSET` condition was appended with incorrect parenthesization, leaving the cast unguarded by the `instanceof` check.

## 3. Design

### 3.1 Fix operator precedence in `hasChildren`

The fix adds proper parentheses so the `instanceof UserNode` check guards all three type comparisons:

```java
else if (parent instanceof UserNode &&
    (((UserNode)parent).getType() == UserNodeType.TABLE || ((UserNode)parent).getType() == UserNodeType.INMEMORY_DATASOURCE ||
    ((UserNode)parent).getType() == UserNodeType.VIEW_FOUNDSET)) return false;
```

This ensures the cast to `UserNode` is never reached unless `parent instanceof UserNode` is true.

### 3.2 Defensive handling for SimpleUserNode

As an additional safety measure, when `parent` is a plain `SimpleUserNode` that is neither `PlatformSimpleUserNode` nor `UserNode`, the method should fall through to `return true` (line 1996) without attempting any cast. The operator precedence fix in 3.1 achieves this automatically, since the `else if` condition will correctly evaluate to `false` and execution continues to `return true`.

## 4. Implementation plan

1. **Fix `SolutionExplorerTreeContentProvider.hasChildren()`** (`com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/views/solutionexplorer/SolutionExplorerTreeContentProvider.java`, line 1993â1995):
   - Move the closing parenthesis of the inner group to include `UserNodeType.VIEW_FOUNDSET`
   - Change: `...INMEMORY_DATASOURCE) || ((UserNode)parent).getType() == UserNodeType.VIEW_FOUNDSET)`
   - To: `...INMEMORY_DATASOURCE || ((UserNode)parent).getType() == UserNodeType.VIEW_FOUNDSET))`

2. **Verify no compilation errors** using `eclipse-ide_getCompilationErrors`.

3. **Test** that the Solution Explorer correctly displays methods under Security, Application, SolutionModel, and EventsManager nodes without ClassCastException.

## 5. Acceptance criteria

- [ ] No `ClassCastException` when navigating to Security, Application, SolutionModel, or EventsManager in Solution Explorer
- [ ] Methods are correctly loaded and displayed in the list panel when these nodes are selected
- [ ] Tree expand/collapse works correctly for nodes with return-type sub-nodes
- [ ] Existing behavior for TABLE, INMEMORY_DATASOURCE, and VIEW_FOUNDSET nodes is preserved (hasChildren returns false for them)
- [ ] Reproducible on both Windows and Mac

## 6. Out of scope

- Refactoring the `SimpleUserNode`/`UserNode`/`PlatformSimpleUserNode` class hierarchy
- Changes to the lazy-loading placeholder mechanism itself
- Performance improvements to tree content provider

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Is the placeholder `SimpleUserNode` ever intentionally visible in the tree, or should `getChildren` always replace it before display? | Developer | open |
| Should `hasChildren` be made more defensive with an explicit `else if (parent instanceof SimpleUserNode) return true` before the UserNode check? | Developer | open |
