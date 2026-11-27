# SVY-21631 peer review summary

**Risk: MODERATE.** The new `tone` / `toneStyle` badge and progress bar properties are opt-in and safe. The risk sits in existing untoned badges, which look different after a package upgrade:

- The package's badge link rule and bubble rule lost `!important`, and the link rule also lost almost all of its specificity.
- The default BUTTON bubble goes from #212529 to #777777.

This is already shipped in bootstrapextracomponents `v2026.9.1`.

Reviewed scope (all on `master`):
- bootstrapextracomponents: `0a935d2`, `7132c09` (tagged SVY-21632)
- skill4servoy: `a1aef8d` (SVY-21632), `3625255` (SVY-21631)

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

- The hover/focus link rule weighs (0,1,1), so it still beats a solution's single class (0,1,0). That contradicts both the CSS comment and the skill text "your own styleClass beats it".
- The exact failure from the issue still reproduces: a solution `color: inherit !important` still cancels the tone colour. Only the agent guidance prevents it. Ask 2 of the issue (the Reviewer measures colour) is said to have shipped under SVY-21611/SVY-21612. This review did not verify that.
- An open `tone` name with no CSS, such as `attention`, renders uncoloured and also loses the link look. This should be documented for developers.
- The legacy and Angular layers disagree on the bubble for numeric `0` and whitespace-only `badgeText`.
- `0a935d2` says "29 tests"; the diff adds 19. Its diagnosis ("lost to the package rule") also contradicts the author's own Jira comment.
- The visual change on upgrade belongs in the package release notes.

## Release target: INDETERMINATE

- Fix Version 2026.12 (core) vs bootstrapextracomponents: this is an extension package, and no comment names a package version. Both commits first shipped in `v2026.9.1`.
- Fix Version 2026.12 vs skill4servoy: the repo has a single `master` branch and no version source.
