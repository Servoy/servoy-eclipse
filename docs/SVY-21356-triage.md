# Triage Report — SVY-21356

**Verdict:** NO_ACTION

## Reported problem

Artifacts (forms, relations, valuelists) created through the AI MCP tools or by direct
file editing bypass the UI wizard validation that normally prevents invalid
configurations. The ticket asks to audit `NewFormWizard`, `NewRelationWizard`,
`NewValueListWizard` against `ServoyBuilder`/`ServoyFormBuilder`/`ServoyRelationBuilder`/
`ServoyValuelistBuilder` and add error markers for any gap found.

The ticket cites one gap already fixed (DB valuelist with no dataproviders, SVY-21281)
and lists four more as "potential gaps to investigate":
- Form with no Body part
- Form elements with invalid/non-existent valuelist references
- Relation items with incompatible column types that the wizard would reject
- Valuelist with invalid `databaseValuesType` value
- Form with `dataSource` set to a datasource format the wizard wouldn't allow

## Root-cause assessment

I read the four wizards (`NewFormWizard`, `NewRelationWizard`/`NewRelationAction`,
`NewValueListWizard`) and the four builder classes
(`com.servoy.eclipse.model.builder.ServoyBuilder`, `ServoyFormBuilder`,
`ServoyRelationBuilder`, `ServoyValuelistBuilder`), plus the MCP artifact-creation
path (`ServoyArtifactCreationService` in `com.servoy.eclipse.developer.mcp`), and
checked each of the four "potential gaps" against the actual builder code:

1. **Form with no Body part** — `NewFormWizard.performFinish()` (line ~385) only
   creates a `Part.BODY` for a concrete (non-abstract, non-responsive) form when it
   has no super form supplying parts. If a form genuinely ends up with zero parts,
   the *runtime* already handles it: `WebFormController.initFormUI()`
   (`servoy_ngclient/.../WebFormController.java:114-117`) falls back to record view
   and calls `reportJSWarning(...)`. This is a deliberate, graceful degradation, not
   silent corruption — the developer is warned when the form is actually opened.
   There is currently no build-time marker for it, but the runtime behavior is by
   design (see the existing code comment in `SwingForm.java:709`: "form with no body
   part - used only for printing probably"), so it's not a bug being silently masked.

2. **Form elements with invalid/non-existent valuelist references** — **already
   checked.** `Field.PROPERTY_VALUELISTID` is declared as `IRepository.ELEMENTS` type
   in `StaticContentSpecLoader.java:377`. `ServoyFormBuilder.addFormMarkers()`
   (lines 236-303) generically iterates every `ELEMENTS`-typed property via
   reflection, resolves the UUID with `fs.searchPersist(element_uuid)`, and calls
   `ServoyBuilderUtils.addNullReferenceMarker(...)` when the target is missing. A
   field whose `valuelistID` points at a deleted/non-existent valuelist already gets
   a `PropertyOnElementTargetNotFound`/`PropertyOnElementInFormTargetNotFound` marker
   through this generic mechanism — no valuelist-specific code needed.

3. **Relation items with incompatible column types that the wizard would reject** —
   **already checked.** `ServoyRelationBuilder.checkRelation()` (line 406) calls
   `Relation.checkKeyTypes(...)` and raises `RELATION_ITEM_TYPE_PROBLEM`
   (`ServoyBuilder.java:535`, `ProblemSeverity.WARNING`) exactly like
   `RelationEditor.createAndcheck()` does for the wizard/editor path
   (`RelationEditor.java:891`). Same check, same code path, already wired into the
   builder.

4. **Valuelist with invalid `databaseValuesType` value** — **not applicable.**
   `databaseValuesType` is not a persisted/settable property. `ValueList.java:128-131`
   defines it as a pure derived getter: `return getRelationName() == null ?
   TABLE_VALUES : RELATED_VALUES;`. There is no `setDatabaseValuesType`, and it isn't
   registered in `StaticContentSpecLoader`. It is structurally impossible to store an
   "invalid" value for it — the premise of this example is incorrect.

5. **Form with `dataSource` set to a datasource format the wizard wouldn't allow** —
   **already checked.** `ServoyFormBuilder.addFormMarkers()` (lines 553-587) resolves
   `form.getDataSource()` via `formFlattenedSolution.getTable(...)`; when the table
   can't be resolved (bad format, unknown server/table) it raises
   `FormTableNotAccessible` / `FORM_INVALID_TABLE` (`IMarker.PRIORITY_HIGH`). The MCP
   `createFormWithFields` tool additionally validates the datasource up front and
   throws before ever creating the form; the plain `createForm` tool doesn't
   pre-validate, but the builder catches a bad result on the next build regardless.

The one gap the ticket describes as already found (DB valuelist with no
dataproviders) was fixed in commit `fc046f588a` (SVY-21281, "flag DB valuelist with
no dataproviders as error marker"), which is already merged and present in the
current `ServoyValuelistBuilder.checkValuelist()`.

## Ticket premise check

The ticket's premise — that MCP/direct-edit artifacts can silently bypass wizard
validation with no builder-side detection — does not hold up for any of the four
concrete examples it lists as needing investigation. Three of the four are already
covered by existing, general-purpose builder mechanisms (the generic `ELEMENTS`
dangling-reference check, `Relation.checkKeyTypes`, and the form-datasource/table
resolution check). The fourth (`databaseValuesType`) targets a computed property that
can never hold an invalid stored value. The only item with no current build-time
check (missing Body part) is intentionally handled at runtime with a graceful
fallback and a JS warning, not a silent failure.

## Approaches considered

1. **Add a build-time warning marker for "form has no Body part / no parts at all"**
   (mirroring the existing runtime `reportJSWarning`) — Pros: makes a currently
   runtime-only signal visible in the Problems view without running the client;
   matches the existing marker pattern (`MarkerMessages`/`ServoyBuilder` pair +
   preference page entry). Cons: the condition is not actually broken behavior (the
   client already degrades gracefully), so this is a nice-to-have visibility
   improvement rather than a bug fix; scope/severity is debatable (would it apply to
   forms mid-edit that temporarily have no parts before a part is added?).
2. **Do a full generic audit of every wizard field for a matching builder check** —
   Pros: thorough, addresses the ticket's literal ask. Cons: the four "potential
   gaps" it explicitly named turned out to already be covered or inapplicable; a
   further open-ended audit with no specific target risks being unbounded busywork
   with no concrete finding to act on.
3. **No code change** — Pros: matches what the investigation actually found: the
   cited gaps are already resolved, already caught generically, or not real. Cons:
   none identified; if the reporter has a concrete reproduction case that isn't one
   of the four listed, it isn't covered by this triage.

## Recommendation

No code change is warranted right now. Every concrete example the ticket raises
for further investigation is either already caught by an existing marker/generic
mechanism, or targets a property that can't actually be invalid. The one item with
no build-time equivalent (missing Body part) is intentional, graceful, already-
communicated (runtime JS warning) behavior, not a masked bug — adding a builder
marker for it would be a minor visibility nicety, not a fix, and can be picked up
separately and narrowly scoped if the team wants it (approach 1 above), rather than
under this ticket's broad "audit everything" framing.

## Git history findings

- `fc046f588a` (SVY-21281, this repo's `servoy-eclipse`): "flag DB valuelist with no
  dataproviders as error marker" — implements exactly the one gap the ticket lists
  as already found; confirms the fix is in place and touches
  `ServoyValuelistBuilder.java`, `ServoyBuilder.java`, `MarkerMessages.java`.
- No other commits reference SVY-21356 or the remaining four "potential gaps"; no
  prior attempt/spec exists for those in `docs/`.
