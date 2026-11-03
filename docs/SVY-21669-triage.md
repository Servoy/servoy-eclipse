# Triage Report — SVY-21669

**Verdict:** NEEDS_INPUT

## Reported problem

While an AI coding agent rebuilt an HTML mockup as Servoy CSS-position forms (ServoyAI
evaluation, "run 011"), it wrote several components whose two position representations in the
`.frm` file disagree:

- the **compact `cssPosition` string** at item level, e.g. `"8,-1,12,-1,100,20"`, and
- the **expanded `json.cssPosition` object**, e.g.
  `{ "top":"8", "right":"12", "bottom":"-1", "left":"-1", "width":"100", "height":"20" }`.

Servoy renders from the compact string, which the agent wrote with `right` and `bottom`
swapped. Two right-anchored labels (`lbl_customerid`, `lbl_ordercount` in `fc_customer_row.frm`)
therefore landed at the left edge, on top of the company/contact text, garbling every row.
The reporter says this happened in 17 elements across six forms (every right-anchored element,
plus one list container with width/height swapped), with no warning anywhere.

The attachment `run-011_fc_customer_row.frm` confirms it exactly: `lbl_customerid` has
`"cssPosition": "8,-1,12,-1,100,20"` at item level but `json.cssPosition` of
`{top:8, right:12, bottom:-1, left:-1, width:100, height:20}` — the compact string decodes to
`top=8, right=-1, bottom=12, left=-1`, i.e. right/bottom swapped versus the JSON.

**Proposed solution in the ticket** (three options, explicitly not settled): (1) state the
compact order with a worked example on the first screen of the authoring file the agent reads;
(2) a reviewer/generator check that decodes the compact string and compares it to the JSON,
failing on any mismatch; (3) optional platform-side warning when the two disagree.

## Root-cause assessment

The actual defect is **in the AI authoring artifacts, not in Servoy's platform code.**

1. **Servoy's behaviour is correct and intentional.** The compact string is the authoritative
   stored property. `PersistHelper.createCSSPosition(String)` (servoy-client,
   `servoy_shared/src/com/servoy/j2db/util/PersistHelper.java:456-481`) tokenizes the string in
   the order **top, right, bottom, left, width, height**; `createCSSPositionString(CSSPosition)`
   (lines 438-453) writes it back in the same order. `CSSPosition`'s constructor
   (`servoy_shared/.../persistence/CSSPosition.java:39`) is
   `CSSPosition(top, right, bottom, left, width, height)`. The developer loader
   (`SolutionDeserializer.getPropertyValuesForJsonObject`,
   `com.servoy.eclipse.model/.../repository/SolutionDeserializer.java:2216-2251`) reads the
   item-level content-spec property (the compact string) via
   `AbstractPersistFactory` → `PersistHelper.createCSSPosition`. The `json` block is the
   web-component model echo used by the designer; for the stored persist the item-level string
   is what wins. So "Servoy uses the compact string" is accurate and by design — nothing is
   malfunctioning.

2. **The agent simply emitted the wrong order.** Per Sean Devlin's comment, the agent wrote its
   own guide comment `cssPosition: "top,bottom,right,left,width,height"` (wrong) and generated
   every compact string in that order, producing the top/right vs top/bottom swap seen in the
   file. The agent only read the spec up to ~line 490 and never reached the authoritative
   statement of the order.

3. **The spec already states the correct order — just too late in the file.** In
   `skill4servoy/.opencode-shippable/skills/servoy-platform/context/specs/frm.spec.md`:
   - line 617: `The compact string format: "top,right,bottom,left,width,height"` (correct),
   - lines 621-634: a worked decode and the mirrored JSON object,
   - but the Authoring Checklist (lines 94-104) and the first Web Component example (lines
     405-424) do **not** restate the order or the "both must agree" rule, so an agent that
     stops reading early never sees it.

**Key scoping finding for the verdict:** the skill and spec that are the natural fix target
(`frm.spec.md`, the authoring checklist) live in the **`skill4servoy` repository**
(`C:\Users\vosti\git_master\skill4servoy\...`), **not** in this `servoy-eclipse` repository.
This triage is running against `servoy-eclipse`. The only part of the ticket that touches
`servoy-eclipse` code is the *optional* platform-side mismatch warning (option 3), and that is
explicitly the weakest of the three options.

## Ticket premise check

The premise "the two representations disagree and Servoy silently uses the compact string" is
**correct and verified against the attached file and the platform source**. But the ticket's
framing as something to fix mixes two very different targets:

- Options 1 and 2 (skill wording + reviewer check) are the right fixes and address the real
  root cause — but they belong to `skill4servoy`, a different repo, so they are out of scope for
  a `servoy-eclipse` change.
- Option 3 (platform warning) is the only `servoy-eclipse`/servoy-client-shaped change, and it
  is questionable: the `json` block is a designer-side model echo, the compact string is the
  stored truth, and the two are not generally required to be byte-identical by any existing
  contract. Adding a load-time warning would be a new behaviour with unclear placement
  (developer loader? NG form generator?) and risk of false positives on legitimately
  partially-specified json blocks. There is no existing validation hook that compares them.

So the premise holds, but *which repository and which option* is a decision a human must make —
the ticket deliberately left it open ("We want a check (skill rule and/or reviewer check)...").

## Approaches considered

1. **Skill/spec fix in `skill4servoy` (ticket options 1 + 2)** — hoist the compact order and a
   worked right-anchored example into the Authoring Checklist / first web-component example of
   `frm.spec.md`, and add a reviewer/generator rule that decodes the compact string and compares
   it field-by-field with `json.cssPosition`, failing on mismatch.
   - Pros: addresses the real root cause (agent never saw the order); cheap; no platform risk;
     catches all 17-element-class mistakes before they ship.
   - Cons: **not in this repository** — cannot be delivered as a `servoy-eclipse` change.

2. **Platform mismatch warning in servoy-client/servoy-eclipse (ticket option 3)** — log a
   warning when a loaded element's compact `cssPosition` and `json.cssPosition` disagree.
   - Pros: in-scope for the Servoy codebase; a safety net independent of who authored the file.
   - Cons: the two are not contractually required to match (json is a designer echo of the model,
     the string is the stored property); risk of false-positive warnings on partially-specified
     json; unclear correct insertion point; weakest option per the ticket itself; does not
     prevent the bad form, only logs after the fact.

3. **No code change** — the platform is correct; the defect is an agent authoring mistake whose
   real fix lives in `skill4servoy`.
   - Pros: honest about where the bug is; avoids adding speculative platform validation.
   - Cons: leaves the ServoyAI evaluation gap unaddressed *from this repo's side*; but that gap
     is addressable in the other repo.

## Recommendation

A human decision is required because the strongest fixes (options 1 and 2) target a **different
repository** (`skill4servoy`) than the one this triage/pipeline runs against (`servoy-eclipse`),
and the only in-repo option (3, platform warning) is explicitly the weakest and carries
false-positive risk against a representation that is not contractually required to match.

My assessment: the correct fix is **options 1 + 2 in `skill4servoy`** (hoist the
`top,right,bottom,left,width,height` order and a worked right-anchored example into the
Authoring Checklist, plus a reviewer check that decodes and compares the two forms). The
`servoy-eclipse` platform should likely do **nothing** (option 3 not recommended) — the platform
is behaving correctly and the `json` echo is not a guaranteed mirror.

But I will not silently redirect the work to another repo or silently drop option 3, so this is
`NEEDS_INPUT` rather than `NO_ACTION`.

## Git history findings

- `PersistHelper.createCSSPosition` / `createCSSPositionString` order was deliberately set by
  commit **`ae4f6aeb2`** (servoy-client, Wed Oct 17 2018, lvostinar):
  *"SVY-12927 change css position order to follow real css order"* — the
  `top,right,bottom,left,width,height` order is an intentional 2018 decision to match real CSS
  (`top right bottom left`). Any "fix" that changes the platform parse order would revert this
  and is wrong.
- No dependency/target-platform version change is involved; this is not a regression — the
  order has been stable since 2018.
- The spec file `frm.spec.md` (in `skill4servoy`) already carries the correct order at line 617;
  the gap is purely that it is not surfaced early enough for an agent that stops reading.

## Questions for the reporter (NEEDS_INPUT)

1. The strongest fixes — stating the compact `top,right,bottom,left,width,height` order (with a
   worked right-anchored example) up front in the Authoring Checklist, and a reviewer/generator
   check that decodes the compact string and compares it to `json.cssPosition` — apply to the
   **`skill4servoy`** repository (`frm.spec.md` and the review/generation logic), not to
   `servoy-eclipse`. Should this case be re-pointed at / implemented in `skill4servoy`? (This
   pipeline is currently scoped to `servoy-eclipse`.)
2. Do you also want the **optional platform-side warning** (ticket option 3) in
   servoy-client/servoy-eclipse — a log warning when a loaded element's compact `cssPosition`
   and `json.cssPosition` disagree? Note the `json` block is a designer-side echo of the model,
   not a contractually required mirror of the stored string, so a load-time comparison may
   produce false positives on legitimately partial `json` blocks. My recommendation is to skip
   it, but it is the only part that would be a `servoy-eclipse`/servoy-client change.
3. If you do want option 3, where should the warning live — the developer-side loader
   (`SolutionDeserializer`) when reading `.frm` files, or the NG form generator
   (`AngularFormGenerator.writeCSSPosition`) at render time?
