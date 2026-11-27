# Peer-review summary — SVY-21624: getFormLayout render does not show CSS grid; agent concluded a Servoy limitation and rewrote correct CSS

**Risk: MODERATE at review, addressed by a follow-up fix.** The source premise holds: the design
render gets the same solution and `svy-<formname>`-rewritten form CSS as the NG client. The original
commit did not fix the corrected fact at every occurrence (Reviewer `correctness.md` still said
`getFormLayout` "skips the rewrite"; Developer `look.md` still said the render "cannot show"
injected-markup styling). The follow-up commit `22efdda` corrects both and adds a "trust set in
`onShow`" exception to the Reviewer's relaxed evidence rule. Issue resolved as Fixed.

**Reviewed scope:** `skill4servoy` on `master`:
- `547c585191e1f39c64f18a97ad6c318fbaa9f0cb` (author's fix): `look.md`, `styling.spec.md`,
  `correctness.md` under `.opencode-shippable/skills/` (+25/-11).
- `22efdda6069cb18a9e0b197571e08337986c3731` (review follow-up): `look.md`, `correctness.md`
  (+13/-6).

No sibling-repo commits.

**Release target:** Fix Version `2026.12`. skill4servoy has only `master` and no version source; the
reviewer confirmed the pack commits ship with 2026.12.

## Manual test plan

### Verifying the fix
1. In a scratch solution, give a responsive form this form `.less`:
   `.sc-grid{display:grid;grid-template-columns:1fr 1fr}` with three cards, the third
   `.sc-card--wide{grid-column:1 / -1}` (bare). Call `getFormLayout`, then open the form in a
   running NG client. Expected: both show the same broken one-row layout.
2. Change it to `grid-column: ~"1 / -1"`. Expected: two columns plus a full-width card in both.
3. Replay run 008 (trace and inputs on SVY-21622) against a synced pack. Expected: the Developer keeps
   `display:grid`, applies the § 12c escape and does not switch to flex. Neither the Developer nor the
   Orchestrator records a "Servoy cannot do grid" fact.

### Regression checks
1. Ask the Reviewer to grade a form-LESS `.svy-respform { padding: ... }` rule. Expected: MAJOR, with
   no "render skips the rewrite" reasoning.
2. Look task with a class-only badge inside tab `text`. Expected: the Developer does not open a "needs
   running client" gap when the render's `appearance` map shows the computed style, and the Reviewer
   PASSes.
3. Set tab `text` to `<span style="color:red">x</span>`, trusted via
   `putClientProperty(UICONSTANTS.TRUST_DATA_AS_HTML, true)` in `onShow`. Compare the `getFormLayout`
   `htmlFile` with the tab's `innerHTML` in the running client. Expected: the render strips the style
   and the client keeps it, which is what the new `22efdda` exception assumes. If the render keeps it,
   the exception can be dropped.
4. Theme-override task where the theme itself uses `!important`. Expected: the Developer still uses
   the step-4 `!important` escape.

## Possible improvements / follow-ups
- Regression check 3 is still unverified. The `onShow` trust exception in `correctness.md` and
  `look.md` (`22efdda`) rests on reasoning ("the render runs no form code"), not on an observed render.
- Confirm with the author of `5dedc71` (which called the opposite claim "source-verified") that the
  reversal is agreed.
- Link the planned jlessc `/`-division fix case from SVY-21624.
