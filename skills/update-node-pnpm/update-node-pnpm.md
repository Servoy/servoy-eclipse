---
description: Update the bundled Node.js and pnpm runtime versions across the Servoy Developer IDE — the five com.servoy.eclipse.nodejs.* plugins, the Angular-build poms, and feature.xml.
---

Load the `update-node-pnpm` skill and run the Node/pnpm runtime update pipeline.

User input: $ARGUMENTS

The argument may name a target Node version (e.g. "24.22.0"), a target pnpm version,
or be empty.

When no target is given, auto-detect: bump Node to the latest patch of the current LTS
line and pnpm to the latest release. A new Node LTS **major** is only proposed after
confirming the repo's Angular version supports it (Phase 1 Angular compatibility gate),
and never applied without the user's explicit confirmation. Always present the proposed
`old → new` versions and wait for confirmation before editing any file, and ask again
before committing.
