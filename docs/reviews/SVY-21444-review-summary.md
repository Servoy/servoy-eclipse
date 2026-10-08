# SVY-21444 — Peer-Review Summary

**Risk: LOW–MODERATE.** A contained single-file TiNG client adaptation; the only real exposure is the server→client media-URL contract coupling across the four back-port lines — if any line gets this client fix without the matching server-side change (or vice versa), imagemedia rendering can regress on that line.

**Scope reviewed:** servoy-eclipse @ `2b36731aee` (identical duplicate `3800322016` on another branch) — one file, +8/−6: `com.servoy.eclipse.ngclient.ui/node/projects/servoydefault/src/lib/imagemedia/imagemedia.ts`. Already on `master` and `release`; back-port fix versions `2026.9.0`, `2026.3.2 LTS`, `2025.3.7 LTS`, `2024.3.12 LTS`.

**What changed:** `updateImageURL()` now branches on whether `dataProviderID().url` exists. If it does, the existing content-type-sniffing path runs unchanged (image → real url, otherwise the `NOT_EMPTY` placeholder); if it does not, the raw dataprovider value is assigned directly to `imageURL`, which binds to `<img [src]='imageURL'>`. The null/empty guard and its `EMPTY` reset are untouched. The sibling `downloadMedia()` already used this exact dual-shape idiom, so the author mirrors an assumption that already shipped. Nothing is reverted — this is a forward adaptation to a new server send-format.

## Manual test plan

**Verifying the fix**
1. On a current server build, open a Titanium (TiNG) solution with a `servoydefault-imagemedia` field bound to a media/blob dataprovider holding an image. Confirm the image renders (the reported broken case; compare with the issue's broken-render screenshot).

**Regression checks**
1. Object-form image DP (`{ url, contentType: 'image/png' }`): image still renders from `.url`.
2. Object-form non-image DP (e.g. `{ url, contentType: 'application/pdf' }`): still shows the `NOT_EMPTY` placeholder.
3. Null / empty / cleared DP and the Remove-media button: still reset to the `EMPTY` placeholder (SVY-19869 behaviour must be preserved).
4. Save-media (`downloadMedia`) and Load-media (upload) buttons still work with both value shapes.
5. Edge: object with empty-string `.url` (`{ url: '', contentType: 'image/png' }`) — observe what renders and decide whether the result is acceptable.
6. Upgraded / converted solution (the "smart client → Titanium converter" path called out in the issue): confirm imagemedia fields render after upgrade, not just in new solutions.

**Automated checks (not run this session)**
- `npm run lint` and the servoydefault build before merge (TiNG requires zero lint warnings).
- `imagemedia.spec.ts` passes but does **not** exercise the new `else` branch.

**Surfaces to cover:** Titanium (TiNG) client only — where the regression lived. NG1 and other media-consuming components were already correct per the commit message; a quick confirm they still render is cheap insurance.

## Possible improvements / follow-ups
- Add a unit test over `updateImageURL` for the plain-URL case and the empty-`.url` case to lock in the new dual-shape contract (the existing spec does not cover the new branch).
- Confirm the matching server-side media-URL send-format change is present on **each** back-port line (`2026.9.0`, `2026.3.2`, `2025.3.7`, `2024.3.12`) so client and server stay paired on every line — the single most consequential item, not settleable from this diff.
- Confirm with the author that the plain (non-`.url`) value is always a URL string in every case the server now sends.
- Security note (no action): the `<img [src]>` sink's input set widened, but Angular's default `SecurityContext.URL` sanitization still applies and no `innerHTML`/`bypassSecurityTrust*` is used — not a reportable injection. Keep the sanitized-context assumption in mind if a future refactor moves this value into a different sink.
