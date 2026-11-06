# SVY-21734 — Peer-review summary: pnpm mode, Titanium NG build silently "succeeds" without doing anything

**Risk: LOW.** Developer-IDE and WAR-export build path only, and only in the opt-in pnpm mode
(`servoy.jsRuntime=pnpm`); npm mode is unchanged. The change turns two silent failures into visible
aborts. The one thing worth knowing: some pnpm-mode builds that used to "pass" silently now stop with a
message (see test 6). No runtime, data or security exposure.

**Scope reviewed:** servoy-eclipse, uncommitted on `master`: `Activator.java` and `WebPackagesListener.java`
in `com.servoy.eclipse.ngclient.ui`, new `PnpmRuntimeResolutionTest` in `com.servoy.eclipse.ngclient.ui.tests`,
plus `docs/SVY-21734-triage.md` and `docs/SVY-21734-pnpm-silent-noop-build.spec.md`.
Not part of the change: the two `.launch` files, `node/pnpm-workspace.yaml`, `node/pnpm-lock.yaml`.

Verified: 0 compile errors in both projects; the whole `plainjunit` package of `ngclient.ui.tests` passes
(145 of 145, 14 of them new). Not verified: any real pnpm build in Developer (see the test plan).

## Manual test plan

Run in Servoy Developer with `-Dservoy.jsRuntime=pnpm`, watching the NG Build Console.

1. **Configured pnpm wins.** Set only `-Dservoy.pnpmPath=<valid pnpm>` (no `servoy.nodePath`/`servoy.npmPath`)
   on a source checkout. Expect `---- Running pnpm command:` lines and a real install and build. Repeat on an
   install with a bundled pnpm: the configured one must be used.
2. **Nothing configured.** Leave `servoy.pnpmPath` unset with a bundled pnpm. The bundled pnpm is used, as before.
3. **Missing runtime.** Set `-Dservoy.pnpmPath=C:\does\not\exist\pnpm.exe`; then leave it unset on a checkout
   with no bundled pnpm; then set a bogus `servoy.nodePath`/`servoy.npmPath`. Expect
   `ERROR: servoy.jsRuntime=pnpm is set but node (...) or pnpm (...) is not available; ...` within seconds and
   no pnpm process.
4. **Tarball failure.** Make `pnpm pack` fail for one component (for example break that package's
   `package.json`), and separately remove `node/tarballs/servoy-public-<version>.tgz`. Expect
   `- ERROR: 'pnpm pack' for <pkg> ...`, then `ERROR: pnpm mode: could not create the tarball(s) for [<pkg>]; ...
   aborted`, and no install or build.
5. **Regressions.** A normal pnpm build with everything present completes as before. An npm-mode build, and an
   npm-mode WAR export, show none of the new messages.
6. **Source checkout without the Maven build.** Without `node/tarballs/servoy-public-<version>.tgz` the pnpm
   build now aborts naming `@servoy/public`; it used to fall back to a directory install. Confirm the message
   is shown clearly in the console and in the final dialog.
7. **Dialogs.** Steps 3 and 4 give exactly one build-failure dialog, not one per failure.
8. **WAR export (added after the review).** With a `<solution>_dist/dist/app/browser` left from an earlier export,
   make the production build fail (bogus `-Dservoy.pnpmPath`, or a broken component `package.json`). The export must
   fail with the console message and produce no WAR. A normal WAR export (pnpm and npm) still succeeds, and cancelling
   from the progress dialog still ends it quietly.
9. **Source `node/` folder (no Developer).** From a clean checkout, remove the `@servoy/public` line from
   `node/package.json`, run `pnpm install`, put the line back, then `npm run build_lib`. Then confirm
   `npx tsc --noEmit -p src/tsconfig.app.json` is clean.

## Possible improvements / follow-ups

- **WAR export falls through on WARNING: fixed in this change.** The review found that `exportNG2ToWar` only threw
  for `IStatus.ERROR`, so a WARNING abort continued to the `dist/app/browser` check and a stale `<solution>_dist`
  could be packaged. Every non-OK status the job can return means the Angular build did not complete (checked one by
  one, see the spec 3.3b), so `exportNG2ToWar` now throws for any non-OK status, except a `CANCEL` that was not a
  user cancel (the build disabled for junit runs). This also applies to npm-mode exports and to `MobileExporter`,
  which calls the same method. Test 8 below covers it.
- **`NoOpNPMCommand` still reports success.** The root of the silent behaviour: when Node is unavailable it logs only
  to the Eclipse error log and reports exit code 0. This change guards the pnpm path in `WebPackagesListener`
  instead. A non-zero exit code would affect `NoOpNPMCommandTest`, the integration test and the
  tests-with-Titanium-disabled flow, so it needs its own case.
- **Dead code: removed in this change.** The "falling back to directory install" branch dated from SVY-21456 and
  was already wrong after SVY-21502 (`dist-public` is not built or copied in pnpm mode), so it could only have failed
  later. It and the stale Javadoc are gone; the shipped tarball is now copied for every pnpm run so the install can
  rely on it (spec 3.3c).
- **`@servoy/public` self-dependency in `node/package.json`.** Left as `file:dist-public`. `link:` and
  `optionalDependencies` break `npm install` (`EUNSUPPORTEDPROTOCOL`), and removing it breaks the type-check of the
  app (294 `TS2307` errors) because `tsconfig` has `paths: {}`. For pnpm in the source folder: remove the line once,
  install, put it back. Revisit when the repo moves fully to pnpm.
- **Test hardening (optional).** `packFailuresMessage` has no single-element or order test; low value.

Security relevance: NONE. No auth, input handling or network surface touched.
