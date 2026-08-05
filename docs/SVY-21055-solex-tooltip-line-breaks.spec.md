# Spec: SVY-21055 — Solution Explorer tooltip line breaks for component API methods

## 1. Goal

Fix the Solution Explorer tooltip rendering for bootstrap component API methods (e.g., Tabpanel's `addTab`, Button's `requestFocus`) so that doc tags (`@param`, `@return`, `@example`, `@element`) are displayed on separate lines instead of running together as a single block of text.

## 2. Background

### 2.1 Tooltip rendering pipeline

Solution Explorer tooltips are rendered as HTML via `HTMLToolTipSupport`. The method `SolutionExplorerListContentProvider.getParsedComment()` transforms raw documentation strings into HTML-ready text by inserting `<br/>` tags for line breaks.

### 2.2 Two code paths in `getParsedComment()`

The method (at line 2155 of `SolutionExplorerListContentProvider.java`) has two branches in the `!toHTML` path:

1. **`elementName == null`** (form/scope methods): Converts `\n` to `<br/>` and inserts `<br/>` before the first `@param`, `@return`, `@example`, and `@properties` tags. This path works correctly.

2. **`elementName != null`** (component API methods): Skips all newline-to-`<br/>` conversion and tag line-breaking. The code splits by `System.getProperty("line.separator")` (which is `\r\n` on Windows), but JSDoc-sourced documentation uses plain `\n`. The result is that the split is ineffective, `\n` characters remain unconverted, and the HTML renderer collapses them into whitespace.

### 2.3 Call chain

`getApiNode()` (line 2857) → `getParsedComment(api.getDocumentation(), elementName, false, false)` → raw text without `<br/>` before tags → `MethodFeedback.getToolTipText()` (line 3350) bolds the tags but doesn't add missing line breaks.

### 2.4 Git history

The comment at line 2197 ("if the element name is null it means we are parsing the documentation of a method from a form or scope") confirms the `elementName != null` branch was intentionally left without tag-breaking — likely an oversight from when component API documentation was first added to Solution Explorer tooltips.

## 3. Design

### 3.1 Add `else` block for `elementName != null`

After the existing `if (elementName == null) { ... }` block at line 2205, add an `else` block that:

1. **Converts `\n` to `<br/>`** — handles docs that contain proper newlines from JSDoc.
2. **Inserts `<br/>` before doc tags** — uses `replaceAll` with a negative lookbehind regex `(?<!<br/>)` to insert `<br/>` before each occurrence of `@param`, `@return`, `@example`, `@element` that isn't already preceded by `<br/>`.

### 3.2 Regex pattern

```java
c = c.replaceAll("\n", "<br/>");
c = c.replaceAll("(?<!<br/>)(@param|@return|@example|@element)", "<br/>$1");
```

Key differences from the `elementName == null` path:
- Uses `replaceAll` (all occurrences) instead of `replaceFirst` — component methods often have multiple `@param` tags.
- Includes `@element` in addition to `@param`, `@return`, `@example` — as the ticket specifically requests.
- Uses negative lookbehind to avoid inserting double `<br/>` when one already exists from the `\n` → `<br/>` conversion.

## 4. Implementation plan

1. In `com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/views/solutionexplorer/SolutionExplorerListContentProvider.java`, add an `else` block after line 2205 (after the closing `}` of the `if (elementName == null)` block):

```java
else
{
    c = c.replaceAll("\n", "<br/>");
    c = c.replaceAll("(?<!<br/>)(@param|@return|@example|@element)", "<br/>$1");
}
```

2. Verify no compilation errors.
3. Manually test by hovering over bootstrap Tabpanel `addTab` and Button `requestFocus` in the Solution Explorer to confirm tags appear on separate lines.

## 5. Acceptance criteria

- [ ] Tooltip for bootstrap Tabpanel `addTab` method displays `@param`, `@return`, `@example` on separate lines
- [ ] Tooltip for Button `requestFocus` method displays `@element` on a separate line
- [ ] Existing form/scope method tooltips (the `elementName == null` path) are unchanged
- [ ] No double `<br/>` is inserted when a `\n` already precedes a doc tag
- [ ] The fix handles all four doc tags: `@param`, `@return`, `@example`, `@element`

## 6. Out of scope

- Refactoring the `elementName == null` and `elementName != null` paths into a unified approach (can be done as a follow-up)
- Fixing the system line separator mismatch in the `split()` call at line 2189 (harmless for this fix since we convert `\n` to `<br/>` afterward)
- Changing the doc format in upstream bootstrap component packages

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should `@properties` also be included in the `else` branch regex (as it is in the `elementName == null` path)? | Dev | open |
