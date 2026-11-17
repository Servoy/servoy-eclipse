# Spec: SVY-21713 — Remove `waitForData` documentation on `servoycore-formcontainer` in Skill4Servoy

> **Implementation repo:** the changes in this spec are applied in the **`skill4servoy`**
> repository (`C:\Users\vosti\git_master\skill4servoy`, branch `master`), **not** in
> `servoy-eclipse`. This spec file itself lives in the `servoy-eclipse` repo's `docs/` per
> convention; only the file edits target the other repo.
>
> **Approach decision:** Per human decision, **Approach 2 — remove `waitForData` entirely** was
> chosen over Approach 1 (keep the property and mark it deprecated). The property is no longer
> used, so it is deleted outright from both skill files; no deprecation note is kept.

## 1. Goal

The Skill4Servoy agent-skill pack documents the `waitForData` property of the
`servoycore-formcontainer` core component incorrectly and inconsistently: one file states the
default is `false` (wrong) and shows it in a copy-paste JSON example, another states the default
is `true` (correct), and **neither** mentions that the platform has **deprecated** the property.
An AI agent that copies the example writes a deprecated property into new forms, and the
conflicting defaults mislead reasoning about render timing. Because the property is no longer
used (form visibility is handled server-side now), the goal is to **remove `waitForData`
entirely** from both skill files — from the property table and example in `frm.spec.md`, and from
the model list in `servoycore-components.md` — so agents neither copy it nor reason about it.

## 2. Background

### 2.1 Source of truth — the live component spec

The authoritative definition of `waitForData` is the live component spec in the `servoy-client`
repo, `servoy_ngclient/war/servoycore/formcontainer/formcontainer.spec` (line 17):

```json
"waitForData" : { "type" :"boolean", "default":true,
  "deprecated" : "not used anymore, form visibility is handled server side now",
  "tags": { "doc": "When true, the form is rendered when all its latest data is loaded ..." }}
```

So the live spec marks the property **deprecated** ("not used anymore, form visibility is handled
server side now") with default `true`. Since it is deprecated and no longer used, the skill pack
should not document it at all.

### 2.2 Why the property is deprecated

The deprecation is intentional and current. Per the triage's git-history findings, `git blame`
on the live spec line points to commit `3d7e4272b` (lvostinar, 2025-03-27, *"SVY-19975 Refactor
components that can show forms (tabpanels, etc) to do form hide and show server-side."*). That
refactor is exactly why `waitForData` "is not used anymore, form visibility is handled server
side now." Removing it from the agent docs aligns with a deliberate platform decision — it is not
reverting anything. The default has been `true` in the live spec since that commit;
`frm.spec.md`'s `false` never matched the platform.

### 2.3 Where the stale documentation lives

Two files in the shipped skill pack (verified in `C:\Users\vosti\git_master\skill4servoy`,
branch `master`) document the property:

- `.opencode-shippable/skills/servoy-platform/context/specs/frm.spec.md`
  - The `servoycore-formcontainer — embedded form component` property table has a `waitForData`
    row with the **wrong default (`false`)** and **no deprecation note**.
  - The example JSON block below it contains `"waitForData": false`, teaching use of a deprecated
    property.
- `.opencode-shippable/skills/servoy-platform/context/components/servoycore-components.md`
  - The `servoycore-formcontainer — Form Container` model list includes
    `waitForData (boolean, default true)` — presented as an active property.

This is documentation drift, not a code regression. No dependency, target-platform, or version
change is involved.

## 3. Design

### 3.1 Approach (approved — Approach 2: full removal)

Remove every reference to `waitForData` from both skill files. The property is deprecated and
unused, so deleting it outright is the simplest correct outcome: nothing to copy wrongly, and no
conflicting default left behind. Concretely:

- In `frm.spec.md`: delete the `waitForData` row from the property table, and remove the
  `"waitForData": false` line from the example JSON block.
- In `servoycore-components.md`: delete the `waitForData` entry from the model list.

### 3.2 Exact edits to apply

**`frm.spec.md` — property table row.** Remove this row entirely:

```
| `waitForData` | Boolean | If `true`, the embedded form waits until data is loaded before rendering. Default `false`. |
```

**`frm.spec.md` — example JSON block.** Remove the `"waitForData": false` line and fix the
trailing comma on the preceding `"tabSeq": -2` line so the JSON stays valid. After the edit the
block reads:

```json
{
    "json": {
        "containedForm": "UUID-of-the-embedded-form",
        "location": { "x": 1, "y": 1 },
        "relationName": "parent_to_child_relation",
        "tabSeq": -2
    },
    "location": "1,1",
    "name": "detailPanel",
    "typeName": "servoycore-formcontainer",
    "typeid": 47,
    "uuid": "UUID-v4"
}
```

**`servoycore-components.md` — model list.** Remove the `waitForData` entry from the
`servoycore-formcontainer — Form Container` model line. Before:

```
**Model** — `animation` (string, default "none"), `containedForm` (form), `relationName` (relation), `waitForData` (boolean, default true), `styleClass` (styleclass), `height` (string, default "0"), `tabSeq` (tabseq), `visible` (visible)
```

After (the `waitForData (boolean, default true), ` segment dropped):

```
**Model** — `animation` (string, default "none"), `containedForm` (form), `relationName` (relation), `styleClass` (styleclass), `height` (string, default "0"), `tabSeq` (tabseq), `visible` (visible)
```

### 3.3 Git history

(Carried from the triage report — no further dig required.)

- Live-spec `waitForData` deprecation + `"default":true` traces to commit `3d7e4272b`
  (SVY-19975, lvostinar, 2025-03-27), the server-side form show/hide refactor.
- `skill4servoy` is on branch `master`; `frm.spec.md` was last touched by `e608dcba7`
  (SVY-21696, 2026-11-10), unrelated to `waitForData`.
- No regression — pure documentation drift against the live spec.

## 4. Implementation plan

All edits are in the **`skill4servoy`** repo (`C:\Users\vosti\git_master\skill4servoy`, branch
`master`).

1. **`C:\Users\vosti\git_master\skill4servoy\.opencode-shippable\skills\servoy-platform\context\specs\frm.spec.md`**
   - In the `servoycore-formcontainer — embedded form component` property table, **delete the
     entire `waitForData` row** (the row currently showing default `false`).
   - In the example JSON block immediately below that table, **remove the `"waitForData": false`
     line** and fix the preceding `"tabSeq": -2` line's trailing comma so the block remains valid
     JSON. (See §3.2.)
2. **`C:\Users\vosti\git_master\skill4servoy\.opencode-shippable\skills\servoy-platform\context\components\servoycore-components.md`**
   - In the `servoycore-formcontainer — Form Container` model list, **delete the
     `waitForData (boolean, default true)` entry** (and its trailing separator) from the
     `**Model**` line. (See §3.2.)
3. Verify no other skill files reference `waitForData`: `grep -ri waitForData` across the
   `skill4servoy` repo. Triage found only these two files; re-confirm before finishing and, if
   any other occurrence exists, remove it too.
4. Commit the change in the `skill4servoy` repo (its own commit conventions / case reference
   `SVY-21713`). The skill pack must be re-shipped for the corrected docs to reach agents.

## 5. Acceptance criteria

- [ ] In `frm.spec.md`, the `servoycore-formcontainer` property table has **no `waitForData`
      row**.
- [ ] In `frm.spec.md`, the `servoycore-formcontainer` example JSON block has **no `waitForData`
      line**, and the block is still valid JSON.
- [ ] In `servoycore-components.md`, the `servoycore-formcontainer` model list has **no
      `waitForData` entry**.
- [ ] A repo-wide `grep` for `waitForData` in `skill4servoy` returns **no matches** (or none that
      document it as a formcontainer property).

## 6. Out of scope

- Any change to the live component spec in `servoy-client` — it is already correct (default
  `true`, deprecated) and is the source of truth.
- Any change in the `servoy-eclipse` codebase — no Java/Angular/platform code is affected.
- Changing the runtime behaviour of `waitForData` or un-deprecating it.
- Keeping a deprecation note in the skill docs — rejected per the human decision to remove the
  property entirely (Approach 2 over Approach 1).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Does the `skill4servoy` repo have a required commit-message / case-reference convention (e.g. `SVY-21713`) to follow for this edit? | skill4servoy maintainer | open |
