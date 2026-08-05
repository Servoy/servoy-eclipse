# Spec: SVY-21263 – IllegalArgumentException in TreeItem.setImage during Solution Explorer filter expansion

## 1. Goal

Fix an `IllegalArgumentException` ("Argument not valid") thrown from `TreeItem.setImage` when the Solution Explorer tree expands nodes after a text filter search. The crash occurs because a disposed or invalid `Image` is passed to SWT's `TreeItem.setImage()` during the `setExpandedElements` call in the filter update routine.

## 2. Background

### 2.1 Stack trace analysis

```
java.lang.IllegalArgumentException: Argument not valid
    at org.eclipse.swt.widgets.TreeItem.setImage(TreeItem.java:1633)
    at org.eclipse.jface.viewers.TreeViewerRow.setImage(TreeViewerRow.java:110)
    at org.eclipse.jface.viewers.ViewerCell.setImage(ViewerCell.java:171)
    at org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.update(DelegatingStyledCellLabelProvider.java:125)
    at org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider.update(DecoratingStyledCellLabelProvider.java:136)
    ...
    at com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView$34.run(SolutionExplorerView.java:3209)
```

SWT's `TreeItem.setImage` throws `IllegalArgumentException` when the image argument is disposed. The JFace framework does not guard against this — it passes whatever `getImage()` returns directly to the SWT widget.

### 2.2 Code flow

1. User types a filter string in the Solution Explorer search box.
2. `filterTree()` (line 3160) creates a `TextFilter`, finds matching nodes, then posts a UI `Runnable` (line 3190).
3. The runnable at line 3209 calls `tree.setExpandedElements(expandedElements.toArray())`.
4. Expanding elements triggers the tree viewer to create/update `TreeItem` widgets for each visible node.
5. For each node, the label provider chain is invoked:
   - `DeprecationDecoratingStyledCellLabelProvider` → `DecoratingStyledCellLabelProvider.update()` → calls `DelegatingStyledCellLabelProvider.update()` → calls `ViewerCell.setImage(getImage(element))`
   - `DecoratingColumnLabelProvider.getImage(element)` → delegates to `ViewLabelProvider.getImage(obj)` (line 685)
6. `ViewLabelProvider.getImage()` calls `((SimpleUserNode)obj).getIcon()` which returns the `Image` stored on the node.
7. If that image is disposed (e.g., because the Activator's image cache disposed it via `putImageInCache`, or because the image was loaded from a media file that was subsequently evicted from the SWT image cache), the SWT `TreeItem.setImage()` throws `IllegalArgumentException`.

### 2.3 Root cause

The `ViewLabelProvider.getImage()` method (line 685) returns whatever `SimpleUserNode.getIcon()` holds without checking if the `Image` is disposed. Its fallback `null_image` (a `gray_dot.png` loaded at construction time) is returned only when `getIcon()` returns `null` — not when it returns a disposed image.

Additionally, `DecoratingColumnLabelProvider.getImage()` (line 106) passes the image to the decorator without verifying disposal state. The decorator chain (`ViewLabelDecorator.decorateImage`, `ProblemDecorator`, platform `LabelDecorator`) can also return disposed overlay images from the `Activator` image cache if `putImageInCache` was called concurrently (which disposes the previous image).

### 2.4 Reproduction scenario

1. Open a workspace with a solution containing media files (images).
2. Open the Solution Explorer.
3. Type a filter string that matches nodes deep in the tree (e.g., a method name, media file name, or component name).
4. The `setExpandedElements` call triggers expansion of multiple tree levels simultaneously.
5. If any expanded node has an image that was disposed between the time it was assigned to the node and the time the tree viewer renders it, the crash occurs.
6. This is more likely during concurrent operations (e.g., project rebuild, solution import, decorator refresh) that can cause image cache eviction.

## 3. Design

### 3.1 Guard in `ViewLabelProvider.getImage()`

Add a disposed-image check in `ViewLabelProvider.getImage()` (SolutionExplorerView.java, line 685). If `SimpleUserNode.getIcon()` returns a disposed image, treat it the same as `null` and return the fallback `null_image`.

```java
@Override
public Image getImage(Object obj)
{
    Image retval = null;
    if (obj instanceof SimpleUserNode)
    {
        retval = ((SimpleUserNode)obj).getIcon();
    }
    if (retval == null || retval.isDisposed())
    {
        retval = null_image;
    }
    return retval;
}
```

### 3.2 Guard in `DecoratingColumnLabelProvider.getImage()`

Add a disposed-image check in `DecoratingColumnLabelProvider.getImage()` (DecoratingColumnLabelProvider.java, line 106). If the provider returns a disposed image, return `null` (which SWT handles gracefully — it simply shows no image) rather than passing it to the decorator.

```java
@Override
public Image getImage(Object element)
{
    Image image = provider.getImage(element);

    if (image != null && image.isDisposed())
    {
        return null;
    }

    if (element instanceof PlatformSimpleUserNode)
    {
        if (((PlatformSimpleUserNode)element).getType() == UserNodeType.SOLUTION_ITEM_NOT_ACTIVE_MODULE) return image;
    }

    if (decorator != null)
    {
        if (decorator instanceof LabelDecorator)
        {
            LabelDecorator ld2 = (LabelDecorator)decorator;
            Image decorated = ld2.decorateImage(image, element, getDecorationContext());
            if (decorated != null)
            {
                return decorated;
            }
        }
        else
        {
            Image decorated = decorator.decorateImage(image, element);
            if (decorated != null)
            {
                return decorated;
            }
        }
    }
    return image;
}
```

### 3.3 Guard in `ViewLabelDecorator.decorateImage()`

At the end of `ViewLabelDecorator.decorateImage()` (line 4540), check that `resultImage` is not disposed before returning it. Also check that images returned by the `defaultSystemDecorator` are not disposed.

```java
if (defaultSystemDecorator != null)
{
    if (defaultSystemDecorator instanceof LabelDecorator)
    {
        LabelDecorator ld2 = (LabelDecorator)defaultSystemDecorator;
        Image decorated = ld2.decorateImage(resultImage != null ? resultImage : image, element, DecorationContext.DEFAULT_CONTEXT);
        if (decorated != null && !decorated.isDisposed())
        {
            return decorated;
        }
    }
    else
    {
        Image decorated = defaultSystemDecorator.decorateImage(resultImage != null ? resultImage : image, element);
        if (decorated != null && !decorated.isDisposed())
        {
            return decorated;
        }
    }
}
return (resultImage != null && !resultImage.isDisposed()) ? resultImage : null;
```

### 3.4 Rationale

The primary fix is 3.1 — guarding `ViewLabelProvider.getImage()` — because this is the innermost provider and the most common path where a disposed node icon reaches SWT. Fixes 3.2 and 3.3 are defense-in-depth to protect against disposed images from decorators and overlay compositing.

## 4. Implementation plan

1. **Edit `SolutionExplorerView.java` — `ViewLabelProvider.getImage()`** (line 692):
   - Change `if (retval == null)` to `if (retval == null || retval.isDisposed())`

2. **Edit `DecoratingColumnLabelProvider.java` — `getImage()`** (line 108):
   - After `Image image = provider.getImage(element);`, add: `if (image != null && image.isDisposed()) return null;`

3. **Edit `SolutionExplorerView.java` — `ViewLabelDecorator.decorateImage()`** (line 4525–4540):
   - Add `!decorated.isDisposed()` checks on returned decorated images
   - Guard `resultImage` return with `!resultImage.isDisposed()`

4. **Organize imports** (no new imports needed).

5. **Verify no compilation errors** using `eclipse-ide_getCompilationErrors`.

6. **Test** — reproduce the scenario by filtering the Solution Explorer while images are being loaded/disposed concurrently; verify no `IllegalArgumentException` in the error log.

## 5. Acceptance criteria

- [ ] No `IllegalArgumentException` from `TreeItem.setImage` when filtering in the Solution Explorer
- [ ] Nodes with disposed icons display the fallback gray dot image instead of crashing
- [ ] Nodes with valid icons continue to display correctly
- [ ] Label decorations (error/warning overlays, deprecation strikethrough) continue to work correctly for nodes with valid images
- [ ] No image memory leaks introduced (disposed images are not re-created unnecessarily)

## 6. Out of scope

- Root-cause investigation of *why* images get disposed prematurely (likely a separate concurrency/lifecycle bug in the image cache)
- Refactoring the image caching strategy in `Activator.putImageInCache` to prevent premature disposal
- Changes to the SWT/JFace framework's handling of disposed images
- Media image scaling/caching in the Solution Explorer list panel

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Is there a specific scenario (e.g., project rebuild, SPM import) that reliably triggers the image disposal before the filter expansion runs? | Developer | open |
| Should `SimpleUserNode.setIcon()` defensively reject disposed images at assignment time? | Developer | open |
| Should `Activator.putImageInCache()` avoid disposing the old image if it might still be referenced by tree nodes? | Developer | open |
