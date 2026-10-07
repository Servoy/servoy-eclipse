# SVY-21274 — upgrade core to Angular 22 — Peer-Review Summary

**Risk: LOW.** A planned, lead-prescribed `ng update` of the three core Angular workspaces
with conservative behaviour-preserving pins; the upgrade has since merged and moved on to
Angular 22.1.0 on `release`, so its build/lint/test verification has been exercised
continuously by CI since.

**Scope reviewed:** servoy-eclipse @ `2cebe283f0` (main Angular 21.2.16→22.0.8 upgrade,
95 files) + `f0c5ca21a8` (3 poms, frontend-maven-plugin `<nodeVersion>` 24.11.1→24.18.0),
authored on `origin/hibernate7`. `b88f9db197` is a superseded duplicate, out of scope.
This case covers **core only** — the component repositories (bootstrap, bs-extra,
servoy-extra, nggrids) were upgraded under their own separate tickets.

## What the change does

Moves NG Client (TiNG), the RFB form designer and the WPM from Angular 21.2.16 to 22.0.8,
pulling the toolchain in lockstep (TypeScript 6, ESLint 10, ng-packagr 22, jasmine 6,
@angular-eslint 22) plus two runtime libraries the issue explicitly scoped in (ag-grid
35→36, bignumber.js 10→11), and raises the bundled Node runtime 24.14→24.18 across the
five platform bundles, `feature.xml` and the three Maven poms.

The hand edits are the conservative kind: `withXhr()` pins the Angular-21 XHR HttpClient
backend, and 51 `ChangeDetectionStrategy.Eager` decorators pin each pre-existing component
back to classic check-always change detection so Angular 22's new lazier default does not
alter refresh behaviour. No application logic changed.

## Verification status

- **Build / lint / test on Angular 22** — effectively confirmed: `release` now runs Angular
  22.1.0, past the version reviewed here, so every CI build since has exercised this.
- **Runtime behaviour of the two major bumps** (ag-grid grids; number/currency formatting
  via `projects/servoy-public/src/lib/format/formatting.service.ts`) — normal QA
  smoke-test territory for a major-version upgrade, not a reviewer gate.
- **`strict: false`** — was added to two root tsconfigs by this upgrade, and has since been
  fully removed on `release` by `a6e8005ce1`, `9e1b868e31` and the accompanying type-error
  fixes (`87bcdcad49`, `b507c69c86`). Only the Angular template-strict flags remain, which
  is the desired state. Closed, no follow-up needed.
- **Supply chain** — all direct dependencies remain exactly pinned; no typosquats; one new
  dev-only dependency (`istanbul-lib-instrument` 6.0.3). Security relevance: none — no new
  network path, auth/authz, data handling or server endpoint.

## Follow-ups

None outstanding.

## Reviewer's note on an earlier draft

An earlier version of this review rated the case ELEVATED on a "merge-forward atomicity"
concern — that commit `2cebe283f0` in isolation carries a peer-dependency split and only one
of five Node-bundle bumps. That was a misreading: the completing commits (`c248d1b782`,
`a571849cbd`, `73b46503c8`) are already ancestors of the branch tip and the upgrade has long
since merged to `release`. Reviewing a single commit out of its branch context is not a
property of this change. The rating above supersedes it.
