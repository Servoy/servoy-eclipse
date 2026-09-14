# Spec: SVY-21411 — Zoom in throws ClassCastException when there is nothing selected

## 1. Goal
When a user opens a responsive form in the form designer and presses the Zoom In button without first selecting an element, the designer throws a `ClassCastException` instead of doing nothing (or zooming appropriately). This spec describes two changes: (a) a guard in the `zoomIn` server-service handler so it fails gracefully when the current selection is not a `PersistContext` (e.g. it is the `Form` itself), and (b) disabling the Zoom In toolbar button unless a zoomable layout container is selected, so the crash path cannot be triggered from the toolbar in the first place. Both match the intent of "zoom into the selected layout container".

## 2. Background

### 2.1 The failure
Reproduction: open a responsive form, select nothing, click Zoom In. The following exception is logged:

```
java.lang.ClassCastException: class com.servoy.j2db.persistence.Form cannot be cast to
class com.servoy.eclipse.ui.property.PersistContext
	at com.servoy.eclipse.designer.editor.rfb.EditorServiceHandler$5.executeMethod(EditorServiceHandler.java:217)
	at com.servoy.eclipse.designer.editor.rfb.EditorServiceHandler$30.call(EditorServiceHandler.java:703)
	...
```

### 2.2 Where the bug lives
`com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/editor/rfb/EditorServiceHandler.java`, in the `zoomIn` handler registered around line 207:

```java
PersistContext selection = null;
if (selectionProvider != null && selectionProvider.getSelection() instanceof IStructuredSelection &&
	((IStructuredSelection)selectionProvider.getSelection()).size() == 1)
{
	selection = (PersistContext)((IStructuredSelection)selectionProvider.getSelection()).getFirstElement();  // line 217 — unchecked cast
}
if (selection != null)
{
	IPersist currentPersist = selection.getPersist();
	while (currentPersist != null && !(currentPersist instanceof LayoutContainer))
	{
		currentPersist = currentPersist.getParent();
	}
	if (currentPersist instanceof LayoutContainer)
	{
		((RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor()).zoomIn((LayoutContainer)currentPersist);
	}
}
```

The code checks that the selection is a single-element `IStructuredSelection` but assumes that single element is always a `PersistContext`. When nothing has been selected by the user, the designer's selection defaults to the `Form` object (`com.servoy.j2db.persistence.Form`), which is not a `PersistContext`, so the cast at line 217 throws.

### 2.3 Related work
This is linked (Relates) to **SVY-21405** ("Zoom in column/row is disabled in responsive form toolbar"), which enabled the Zoom In toolbar button in responsive forms. Enabling that button surfaced this pre-existing crash path when the button is pressed with no selection.

## 3. Design

### 3.1 Guard the cast with instanceof
Replace the unchecked cast with an `instanceof` pattern check so the handler only proceeds when the selected element is actually a `PersistContext`. When it is not (Form selected / nothing selected), `selection` stays `null` and the handler does nothing — no exception, no zoom.

Proposed handler body:

```java
public Object executeMethod(String methodName, JSONObject args)
{
	PersistContext selection = null;
	if (selectionProvider != null && selectionProvider.getSelection() instanceof IStructuredSelection structuredSelection &&
		structuredSelection.size() == 1 && structuredSelection.getFirstElement() instanceof PersistContext persistContext)
	{
		selection = persistContext;
	}
	if (selection != null)
	{
		IPersist currentPersist = selection.getPersist();
		while (currentPersist != null && !(currentPersist instanceof LayoutContainer))
		{
			currentPersist = currentPersist.getParent();
		}
		if (currentPersist instanceof LayoutContainer)
		{
			((RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor()).zoomIn((LayoutContainer)currentPersist);
		}
	}
	return null;
}
```

### 3.2 Behaviour when nothing is selected
The server-side handler is a graceful no-op: even if `zoomIn` is invoked with no element (or a non-`PersistContext`) selected, it simply does nothing. This preserves the existing "zoom into the layout container of the selected element" semantics without introducing new behaviour, and acts as a defensive backstop.

### 3.3 Disable the Zoom In toolbar button unless a layout container is selected
The Zoom In toolbar button (`btnZoomIn` in `com.servoy.eclipse.designer.rfb/node/src/designer/toolbar/toolbar.component.ts`) previously enabled whenever exactly one element was selected in a responsive form (`selectionChanged`), which is what let the user trigger the crash path with the `Form` selected. It is now enabled only when the single selected element is a zoomable layout container — i.e. it has the `svy-layoutcontainer` class, is not the main container (`data-maincontainer`), and is not a responsive container (`svy-responsivecontainer`). This mirrors the gating already used by the context menu Zoom In entry and by the mouse-selection decorator logic.

The check is extracted into a private helper `isZoomableLayoutContainerSelected(selection: string[])` that resolves the selected element via `editorContentService.getContentElement(...)` and inspects its classes/attributes. With nothing selected (or a non-container element / the `Form`), the button is disabled and the `zoomIn` action can no longer be dispatched from the toolbar.

The same helper is also used to seed the **initial** button state in `setupItems()` (run on session open), not only in `selectionChanged`. Otherwise the button keeps its constructor default and appears enabled until the first selection change fires — so opening a form with nothing selected would show Zoom In as enabled until the user clicks the form.

## 4. Implementation plan

1. In `com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/editor/rfb/EditorServiceHandler.java`, update the `zoomIn` handler (registered at ~line 207) to use an `instanceof PersistContext` pattern check instead of the unchecked cast at line 217, per §3.1.
2. Organize imports and format the file.
3. Run `eclipse-ide_getCompilationErrors` on `com.servoy.eclipse.designer` and resolve any issues; fix any high-severity SpotBugs findings in the modified code.
4. Extract the selection-resolution logic into package-visible helper methods (`getSinglePersistContextSelection`, `findLayoutContainer`) so the guard is verifiable, but do not add a dedicated `com.servoy.eclipse.designer.tests` test class: that module is packaged as an `eclipse-test-plugin` and the JUnit 5 platform bundles on the launch classpath are mismatched in this workspace (`NoSuchMethodError: CollectionUtils.toUnmodifiableList()`), so such a test cannot be run reliably. The primary regression coverage lives on the frontend (§4 step 6), where the button-enablement change prevents the crash path from being triggered at all.
5. In `com.servoy.eclipse.designer.rfb/node/src/designer/toolbar/toolbar.component.ts`, change the responsive branch of `selectionChanged` to set `btnZoomIn.enabled` from a new `isZoomableLayoutContainerSelected(selection)` helper instead of `selection.length == 1`, per §3.3.
6. Run `npm run lint` (zero warnings) and `npm run build_debug_nowatch` in `com.servoy.eclipse.designer.rfb/node`, then add/extend Vitest coverage in `toolbar.component.spec.ts` for the enabled/disabled cases (layout container selected → enabled; nothing selected, non-container element, and main container → disabled).

## 5. Acceptance criteria
- [ ] Opening a responsive form, selecting nothing, and pressing Zoom In no longer logs a `ClassCastException`.
- [ ] Pressing Zoom In with no selection performs a graceful no-op (nothing zooms, no error).
- [ ] Pressing Zoom In with a single element selected whose persist chain contains a `LayoutContainer` still zooms into that container (existing behaviour preserved).
- [ ] No new compilation errors or high/second-highest severity SpotBugs findings in the modified code.
- [ ] The Zoom In toolbar button is disabled when nothing is selected, when the selected element is not a layout container, and for the main container; it is enabled when a single zoomable layout container is selected.
- [ ] `com.servoy.eclipse.designer.rfb/node` passes `npm run lint` with zero warnings and `npm run build_debug_nowatch`, and Vitest covers the button enable/disable cases.

## 6. Out of scope
- Any change to the `zoomOut` handler or the underlying `RfbVisualFormEditorDesignPage.zoomIn` implementation.
- Changing what the designer selects by default when a form opens.
- Any new UX such as showing a message/hint when Zoom In is pressed with nothing selected.

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| Is a silent no-op the desired UX when Zoom In is pressed with nothing selected, or should the button be disabled / a hint shown until a layout container is selectable? | Product | resolved — button is disabled unless a zoomable layout container is selected (§3.3); server handler is a defensive no-op backstop |
| Can the `zoomIn` selection-resolution logic be extracted to a testable helper, or must the test drive it through the full `EditorServiceHandler`? | Dev | resolved — extracted to package-visible `getSinglePersistContextSelection` / `findLayoutContainer` helpers |
