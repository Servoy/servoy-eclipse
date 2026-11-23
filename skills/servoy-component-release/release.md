---
description: Draft or publish a GitHub release for a Servoy component package — tags, builds the release zip, generates a changelog, publishes via gh.
---

Load the `servoy-component-release` skill and execute the release pipeline for this component package.

User input: $ARGUMENTS

Behavior:
- No arguments → create a **draft** release (default, reversible).
- `publish` → detect the state of the release at the target tag, tell the user exactly what will happen, ask for confirmation, then promote an existing draft, run a full publish if no release exists, or hard-stop if it is already published. After a promote or full publish, offer to bump the version in the 3 version files.

The version is always read from the 3 version files (`package.json`, `META-INF/MANIFEST.MF`, `projects/*/package.json`) — never passed as an argument.

Pre-release versions: between releases the files hold the next version with a `-pre` suffix (e.g. `2026.9.1-pre`). Before building, the skill removes the `-pre` in the 3 files (commit `release <version> [skip ci] [ai]`, pushed after confirmation), and the post-publish version bump proposes `<next>-pre`.
