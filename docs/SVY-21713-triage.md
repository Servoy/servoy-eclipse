# Triage Report — SVY-21713

**Verdict:** PROCEED

## Reported problem

The Skill4Servoy agent-skill files describe `waitForData` on the `servoycore-formcontainer`
core component as an active model property and show it in a copy-paste example
(`"waitForData": false`). In reality the platform marks that property **deprecated**. The two
skill files also disagree with each other on its default value:

- `frm.spec.md` ("servoycore-formcontainer — embedded form component") says **default `false`**
  and its example JSON sets `"waitForData": false`.
- `servoycore-components.md` ("servoycore-formcontainer - Form Container") says **default
  `true`**.

An agent that copies the example writes a deprecated property into new forms, and the
conflicting defaults can mislead reasoning about render timing.

## Root-cause assessment

The problem is **not in the servoy-eclipse code** and not in the live component spec — it is
stale/incorrect documentation in the **skill4servoy** repository (the shipped agent skill pack).

Live component spec (source of truth), `servoy-client`:
`servoy_ngclient/war/servoycore/formcontainer/formcontainer.spec` (line 17):

```json
"waitForData" : { "type" :"boolean", "default":true,
  "deprecated" : "not used anymore, form visibility is handled server side now",
  "tags": { "doc": "When true, the form is rendered when all its latest data is loaded ..." }}
```

So the authoritative facts are: **default is `true`** and the property is **deprecated**.

The two skill files are both wrong in different ways (verified in
`C:\Users\vosti\git_master\skill4servoy`, branch `master`):

- `.opencode-shippable/skills/servoy-platform/context/specs/frm.spec.md`
  - line 669: `| \`waitForData\` | Boolean | If \`true\`, the embedded form waits until data is loaded before rendering. Default \`false\`. |` — **wrong default**, **no deprecation note**.
  - line 681: example JSON block contains `"waitForData": false` — teaches use of a deprecated property.
- `.opencode-shippable/skills/servoy-platform/context/components/servoycore-components.md`
  - line 43: model list includes `waitForData (boolean, default true)` — correct default, but **lists it as an active property with no deprecation marker**.

The symptom maps to a single, unambiguous root cause: the skill pack documentation drifted from
the live spec. There is no divergence that only the reporter could resolve — the live
`.spec` file settles both the default and the deprecation.

Note on repo location: this triage runs inside `servoy-eclipse`, but the files needing the fix
live in the separate **skill4servoy** repo (`.opencode-shippable/skills/servoy-platform/context/`).
Per the triage instruction the report itself is written to the `servoy-eclipse` repo-root `docs/`,
but the implementing change must be made in `skill4servoy`.

## Ticket premise check

The ticket's premise holds up fully. It correctly separates symptom (skill docs teach a
deprecated property with a wrong/conflicting default) from the live-spec truth (deprecated,
default `true`), and even gives the exact file/line locations and a how-to-check recipe. Its
"Expected / suggested" fixes are sound:

1. Remove `waitForData` from the `frm.spec.md` property table and from the example JSON block.
2. Remove it from the model list in `servoycore-components.md`, or mark it deprecated there.
3. If a mention is kept, state it is deprecated and has no effect.

The only correction to the ticket's own wording: `frm.spec.md`'s "Default `false`" is not just
a copy of the spec — it is **wrong**; the spec default is `true`. `servoycore-components.md`
already has the right default but is missing the deprecation marker. So neither file is correct
as-is.

The ticket's "Not verified" note (whether the property still has any effect in the released
2026.9 build) does not block the fix: the deprecation text — "not used anymore, form visibility
is handled server side now" — is authored in the live spec by the same change that moved form
show/hide server-side (see Git history), so documenting it as deprecated/no-effect is correct
regardless of the exact released build.

## Approaches considered

1. **Fix both skill files to match the live spec (recommended).**
   In `frm.spec.md`: correct the default to `true` and mark the property deprecated with a note
   that it is no longer used (or drop the row and the `"waitForData": false` line from the
   example). In `servoycore-components.md`: mark `waitForData` deprecated in the model list (or
   drop it). This removes the copy-paste hazard and the internal contradiction in one change.
   - Pros: matches source of truth; stops agents emitting a deprecated property; resolves the
     default-value conflict; small, self-contained, no code risk.
   - Cons: in the `skill4servoy` repo, not `servoy-eclipse`; requires the skill pack to be
     re-shipped for the change to reach agents.

2. **Only remove the property entirely from both files.**
   - Pros: simplest; nothing to copy wrongly.
   - Cons: an agent that encounters `waitForData` in an existing form (it is still a valid,
     settable property) gets no explanation. Keeping a short "deprecated, no effect" note is
     more helpful than silent removal.

3. **Only fix the wrong default in `frm.spec.md`, leave the property documented as active.**
   - Pros: trivial.
   - Cons: leaves the real hazard — the example still teaches writing a deprecated property into
     new forms. Does not address the ticket. Rejected.

4. **No code change.**
   - Pros: none meaningful.
   - Cons: the documentation is demonstrably wrong against the live spec (wrong default in one
     file; property presented as active in both; deprecation absent from both; example teaches
     it). This actively misleads AI-authored forms. Rejected.

## Recommendation

**PROCEED** with Approach 1, applied in the **skill4servoy** repo (not servoy-eclipse):

- `.opencode-shippable/skills/servoy-platform/context/specs/frm.spec.md` — in the
  `servoycore-formcontainer` property table change the `waitForData` row to show **default
  `true`** and state it is **deprecated / no longer used (form visibility is handled server
  side)**, and remove the `"waitForData": false` line from the example JSON block (or keep it
  only with an inline "deprecated, no effect" comment). Prefer marking deprecated over silent
  deletion so the term is still explained if an agent meets it in existing forms.
- `.opencode-shippable/skills/servoy-platform/context/components/servoycore-components.md` — mark
  `waitForData` as deprecated in the model list (keep "default true") or drop it from the list.

Source of truth for the exact wording: the `deprecated` string and `"default":true` in
`servoy-client/servoy_ngclient/war/servoycore/formcontainer/formcontainer.spec`.

Alternatives: Approach 2 (remove entirely) is acceptable if the team prefers not to document
deprecated properties at all; Approaches 3 and 4 are rejected because they leave the
copy-paste hazard in place.

## Git history findings

- The deprecation is intentional and current, not accidental. `git blame` on the live spec line
  (`servoy-client/servoy_ngclient/war/servoycore/formcontainer/formcontainer.spec`) points to
  commit **`3d7e4272b`** by `lvostinar`, 2025-03-27:
  *"SVY-19975 Refactor components that can show forms (tabpanels, etc) to do form hide and show
  server-side."* That refactor is exactly why `waitForData` "is not used anymore, form
  visibility is handled server side now." So documenting it as deprecated/no-effect aligns with
  a deliberate platform decision — it is not reverting anything.
- The default has been `true` in the live spec since that commit; `frm.spec.md`'s `false` never
  matched the platform.
- skill4servoy is on branch `master`; the `frm.spec.md` file was last touched by `e608dcba7`
  (SVY-21696, 2026-11-10), unrelated to `waitForData`.
- No dependency / target-platform / version change is involved; this is a documentation drift,
  not a code regression.
