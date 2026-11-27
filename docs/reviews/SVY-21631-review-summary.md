# SVY-21631 peer review summary

**Risk: MODERATE.** The new `tone` / `toneStyle` badge and progress bar properties are opt-in and safe. The risk sits in existing untoned badges, which look different after a package upgrade:

- The package's badge link rule and bubble rule lost `!important`, and the link rule also lost almost all of its specificity.
- The default BUTTON bubble goes from #212529 to #777777.

This is already shipped in bootstrapextracomponents `v2026.9.1`.

Reviewed scope (all on `master`):
- bootstrapextracomponents: `0a935d2`, `7132c09` (tagged SVY-21632)
- skill4servoy: `a1aef8d` (SVY-21632), `3625255` (SVY-21631)

**Verdict: OK to resolve.** All three asks of the issue are covered:
1. Agent guidance: skill4servoy `a1aef8d` and `3625255`.
2. The Reviewer measures colour before PASS: `servoy-reviewer/context/correctness.md:278-305`, shipped as `5fde662` (SVY-21611) and `171ed17` (SVY-21612). Verified in the source.
3. The link look is not applied: toned badges are excluded from the link rule, and untoned badges are overridable by a solution class.

The follow-ups below are non-blocking.

## Servoy AI replay (2026-11-27)

The replay ran in Servoy Developer 2026.12 (`closePopup` solution) with bootstrapextracomponents 2026.9.1, through the Orchestrator agent. The prompt asked for six display-only LABEL badges with exact colour pairs, including a sixth "Attention" purple, and no link look, including on hover. The badges went on both a responsive form (`badge_status`) and an absolute form (`badge_status_abs`), and the agent had to measure computed values in the NG client.

Result: **12/12 badges measured exactly at the requested colours, with `text-decoration: none`, in normal and hover state, on both form types.** The values were measured through the DevTools protocol in headless Edge, with a real mouse hover where `:hover` matched. The agent reported focus, active and other viewports as unmeasured instead of claiming them.

- **No trap:** the agent used solution classes (`a.badge-status.badge-status-<x>` with every link state) and no `color: inherit`.
- **No `!important`:** the agent's rules needed none.
- **Same in both form types:** the absolute and responsive forms render identically, which confirms that the lower specificity of the package rules works.
- **`tone` not used:** the agent skipped it because the built-in tones do not give the exact requested hex values. This replay therefore does not exercise the tone path.
- **Not exercised: single-class hover.** The agent's selectors (0,2,1 + `:hover`) are stronger than the package hover rule, so the single-class hover concern below is still untested.
- **Pack defects seen in the run:**
  - The agent wrote its measuring script to `%TEMP%\opencode` instead of `~/.servoy/opencode/tmp`. That triggered an `external_directory` permission prompt. The tmp-dir swap in `servoy-skill-systemprompt.js` did not take effect.
  - `showFormInBrowser` generated two Cypress spec stubs with no assertions as a side effect.

## Manual test plan

Verifying the fix:
1. Use package v2026.9.1 or master. On a responsive form, add badges with every `tone` (success/info/warning/danger/neutral), each with `toneStyle` soft and solid. The computed `color` and `background` on the inner `<a>`/`<button>` must match the fallbacks in `svy_bootstrapextracomponents.css`.
2. Repeat on an absolute form. The colours must be identical.
3. Redefine `--svy-success-soft-fg` in the solution `.less`. The badge colour must follow without `!important`.
4. Progress bar with `tone = danger` and `type = info`: the track shows the danger tone, and there is no `text-bg-info` class.
5. In the properties view, `tone` must offer the five names as typeahead proposals and accept a free value.

Regression checks:
1. Default BUTTON badge with `badgeText = "5"`, v2026.9.0 vs v2026.9.1: check the bubble colour change.
2. Absolute-form LABEL badge with no `styleClass`: #337ab7 with no underline. On hover: #23527c with an underline.
3. Absolute-form LABEL badge with `styleClass = "btn btn-primary"`, and one inside a container whose theme styles `a`: compare the colour before and after the upgrade.
4. Absolute-form LABEL badge with a solution class that sets `color` and `text-decoration: none`: check the normal, hover and focus states.
5. `badgeText` unset, `""`, `" "`, `"0"` and numeric `0`, on Titanium and on the legacy AngularJS runtime.
6. Untoned progress bar with `type = success`: unchanged.
7. `tone = attention` with no solution CSS: check how it renders.

## Possible improvements / follow-ups

- The hover/focus link rule weighs (0,1,1), so it still beats a solution's single class (0,1,0). That contradicts both the CSS comment and the skill text "your own styleClass beats it". The replay did not test this, and it is a candidate follow-up case.
- The exact failure from the issue is still possible: a solution `color: inherit !important` still cancels the tone colour. Only the agent guidance prevents it, and the replay showed the guidance holding.
- skill4servoy: the tmp-dir swap in `servoy-skill-systemprompt.js` did not take effect in the replay, and `showFormInBrowser` leaves Cypress stubs behind.
- An open `tone` name with no CSS, such as `attention`, renders uncoloured and also loses the link look. This should be documented for developers.
- The legacy and Angular layers disagree on the bubble for numeric `0` and whitespace-only `badgeText`.
- `0a935d2` says "29 tests"; the diff adds 19. Its diagnosis ("lost to the package rule") also contradicts the author's own Jira comment.
- The visual change on upgrade belongs in the package release notes.

## Release target

Fix Version 2026.12 was confirmed by the reviewer. The bootstrapextracomponents commits first shipped in package `v2026.9.1`. The skill4servoy guidance ships with the Developer pack from `master`.
