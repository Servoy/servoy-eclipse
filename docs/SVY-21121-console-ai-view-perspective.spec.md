# Spec: SVY-21121 — Console view is not in the Servoy Perspective when that is reset

## 1. Goal

When the user resets the Servoy Design perspective (Window > Perspective > Reset Perspective), the Console view and the Servoy Pilot (AI) view should appear by default in the bottom panel. Currently the Console view is only registered as a placeholder (meaning it is not visible after a reset), and the AI view is not referenced at all in the perspective layout.

## 2. Background

### 2.1 Perspective layout mechanism

The Servoy Design perspective is defined in `com.servoy.eclipse.ui.DesignPerspective` which implements `IPerspectiveFactory`. The `createInitialLayout` method defines the default view arrangement when the perspective is first opened or reset.

- `IFolderLayout.addView(id)` — the view is visible by default after a perspective reset.
- `IFolderLayout.addPlaceholder(id)` — a slot is reserved for the view, but it is not visible until the user manually opens it.

### 2.2 Current state

In `DesignPerspective.java:60`, the Console view is added as a **placeholder**:
```java
bottom.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);
```
This means it does not appear when the perspective is reset. The comment `//move to debug perspective only` is outdated — the Console view is useful in the Servoy Design perspective for showing server output, build logs, etc.

### 2.3 Servoy AI view

The Servoy AI view is registered in the `com.servoy.eclipse.opencode` bundle (from the `Servoy-Copilot` repository, bundled with the product via the `servoypilot.feature`). Its view ID is `com.servoy.eclipse.opencode.OpenCodeView` and is named "Servoy AI" in the UI.

## 3. Design

### 3.1 Console view

Change `addPlaceholder` to `addView` for the Console view in the `bottom` folder. This makes the Console visible by default when the Servoy Design perspective is reset or opened for the first time.

### 3.2 AI (Servoy AI) view

Add the Servoy AI view (`com.servoy.eclipse.opencode.OpenCodeView`) to the `right` folder using `addView`, as a tab alongside Outline and Properties. Since the opencode plugin is an external dependency (always bundled with the product), using `addView` is safe — Eclipse gracefully handles the case where a view ID is not resolvable (it simply won't show).

### 3.3 Remove outdated comment

Remove the comment `//move to debug perspective only` on the Console view line, as this change deliberately adds it to the Servoy perspective.

## 4. Implementation plan

1. Edit `com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/DesignPerspective.java`:
   - Line 60: Change `bottom.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW)` to `bottom.addView(IConsoleConstants.ID_CONSOLE_VIEW)` and remove the trailing comment.
   - After the Properties view line in the `right` folder: Add `right.addView("com.servoy.eclipse.opencode.OpenCodeView");`

2. Verify there are no compilation errors.

3. Manual testing: Launch the IDE, reset the Servoy Design perspective, and confirm both the Console tab and the Servoy AI view tab appear in the bottom panel.

## 5. Acceptance criteria

- [ ] After resetting the Servoy Design perspective, the Console view is visible in the bottom panel.
- [ ] After resetting the Servoy Design perspective, the Servoy AI view is visible in the right panel (alongside Outline and Properties).
- [ ] The Console view appears as a tab in the bottom folder alongside Problems, Tasks, Bookmarks, Scripting Console, and Search.
- [ ] No compilation errors introduced.

## 6. Out of scope

- Changing the position/order of existing views in the perspective.
- Modifying the Debug perspective layout.
- Changes to the Servoy Pilot / opencode plugin itself.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| (none)   |       |        |
