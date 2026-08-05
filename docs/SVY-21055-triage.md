# Triage Report — SVY-21055

**Verdict:** PROCEED

## Reported problem

Tooltips in the Solution Explorer for bootstrap component API methods (e.g., Tabpanel's `addTab`, Button's `requestFocus`) do not render correctly. Doc tags like `@param`, `@return`, `@example`, and `@element` appear inline on the same line as the description text instead of on separate lines. The tooltips are hard to read because everything runs together.

## Root-cause assessment

The root cause is in `SolutionExplorerListContentProvider.getParsedComment()` at line 2197 (`com.servoy.eclipse.ui`). The method has two code paths:

1. **`elementName == null`** (form/scope methods): Converts `\n` to `<br/>` at lines 2199-2200, then adds extra `<br/>` before the first `@param`, `@return`, `@example` at lines 2201-2204.

2. **`elementName != null`** (component API methods like bootstrap tabpanel): **Skips all tag-line-breaking logic**. The code at lines 2188-2196 splits by `System.getProperty("line.separator")` (which is `\r\n` on Windows), but the documentation extracted from JSDoc comments uses `\n`. This means:
   - The split produces a single element (the entire string)
   - The `\n` characters are never converted to `<br/>`
   - No `<br/>` is inserted before `@param`, `@return`, `@example`, `@element` tags
   - The HTML tooltip renderer (via `HTMLToolTipSupport`) treats bare `\n` as whitespace, so everything appears on one line

The tooltip is rendered as HTML (confirmed by `HTMLToolTipSupport.enableFor(list, ToolTip.NO_RECREATE)` at `SolutionExplorerView.java:1611`), so `<br/>` tags are required for line breaks.

Call chain: `getApiNode()` (line 2857) → `getParsedComment(api.getDocumentation(), elementName, false, false)` → returns text without `<br/>` before tags → `MethodFeedback.getToolTipText()` (line 3350) bolds the tags but doesn't add line breaks.

## Ticket premise check

The ticket says "check methods from bootstrap components for solex tooltip display" and "@element needs a new line before". This is correct — the problem is that doc tags need line breaks inserted before them in the HTML tooltip output. The ticket doesn't propose a specific implementation approach.

## Approaches considered

1. **Add `\n` → `<br/>` conversion and tag line-breaking for `elementName != null` in `getParsedComment()`** — Add an `else` block after line 2205 that converts `\n` to `<br/>` and ensures `<br/>` before each occurrence of `@param`, `@return`, `@example`, `@element`.
   - Pros: Minimal change, mirrors existing logic for `elementName == null`, fixes both cases (docs with `\n` before tags and docs without)
   - Cons: Slightly duplicates logic (could be refactored to share)

2. **Unify the `elementName == null` and `elementName != null` paths** — Remove the `if (elementName == null)` guard entirely and apply tag-breaking to all cases.
   - Pros: DRY, single code path
   - Cons: Higher risk of unintended side effects on existing form/scope method tooltips; the `elementName == null` path uses `replaceFirst` (only first occurrence) while component methods need `replaceAll` (all occurrences)

3. **Fix the line separator mismatch only** — Change the split at line 2189 to use `\n` or `\\r?\\n` regex instead of system separator.
   - Pros: Fixes the case where docs DO have `\n` before tags
   - Cons: Doesn't fix cases where documentation is genuinely on one line (no `\n` before tags)

4. **No code change** — The component documentation files should include proper formatting themselves.
   - Pros: No code risk
   - Cons: Bootstrap components are external packages; we can't control their doc format. The IDE should handle reasonable doc formats gracefully.

## Recommendation

**Approach 1**: Add an `else` block in `getParsedComment()` for the `elementName != null` case. The fix should:
1. Convert `\n` to `<br/>` (handles docs with proper newlines from JSDoc)
2. Use `replaceAll` with a negative lookbehind to insert `<br/>` before each `@param`, `@return`, `@example`, `@element` that isn't already preceded by `<br/>`

This is minimal, safe, and directly addresses the symptom without affecting the existing `elementName == null` path.

## Git history findings

Unable to run git CLI (not on PATH in this environment). The comment at line 2197 says "if the element name is null it means we are parsing the documentation of a method from a form or scope" — confirming the `elementName != null` branch was intentionally left without tag-breaking, likely an oversight when component API documentation was added to the Solution Explorer tooltips.
