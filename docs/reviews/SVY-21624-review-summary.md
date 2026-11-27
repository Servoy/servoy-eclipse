# Peer-review summary — SVY-21624: getFormLayout render does not show CSS grid; agent concluded a Servoy limitation and rewrote correct CSS

**Risk: MODERATE.** The source premise holds: the design render gets the same solution and
`svy-<formname>`-rewritten form CSS as the NG client. But the corrected fact was not fixed at every
occurrence. `servoy-reviewer/context/correctness.md:256` still says `getFormLayout` "skips the
rewrite", and `servoy-developer/context/ui/look.md:308,332` still says the render "hides" or "cannot
show" injected-markup styling, which the Reviewer now accepts from the render.

**Reviewed scope:** `skill4servoy` commit `547c585191e1f39c64f18a97ad6c318fbaa9f0cb` on `master`.
It changes `look.md`, `styling.spec.md` and `correctness.md` under `.opencode-shippable/skills/`
(+25/-11). No sibling-repo commits.

**Release target:** Fix Version `2026.12`, **INDETERMINATE**. skill4servoy has only `master`, no tags
and no version source, so we don't know which Servoy Developer 2026.12 build (Servoy-Copilot skills
bundle) picks up this commit.

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
   `htmlFile` with the tab's `innerHTML` in the running client. If the render strips the style, the
   Reviewer rule needs a "trust set by code" exception.
4. Theme-override task where the theme itself uses `!important`. Expected: the Developer still uses
   the step-4 `!important` escape.

## Possible improvements / follow-ups
- Remove the leftover "skips the rewrite" sentence at `correctness.md:255-256`.
- Align `look.md` § Markup injected through an HTML property (l.307-308, 331-332) with the Reviewer
  rule: render `appearance` is valid for property-driven markup.
- Add "trust set by form code (`onShow`)" to the exceptions of the relaxed Reviewer rule
  (`correctness.md:281-283`) if regression check 3 confirms the render sanitizes it.
- Confirm with the author of `5dedc71` (which called the opposite claim "source-verified") that the
  reversal is agreed.
- No `s4s.py validate` run or run-008 replay is recorded for this commit.
- Link the planned jlessc `/`-division fix case from SVY-21624.
