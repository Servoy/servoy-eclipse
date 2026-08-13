---
description: Run the Servoy component package migration pipeline — upgrades Angular, tests, lint, standalone, signals.
agent: general
---

Load the `servoy-component-migration` skill and execute the full migration pipeline for this component package.

User input: $ARGUMENTS

If no arguments are given, run the status detection phase first and present a summary of what needs to be done. If the user provides a specific phase name (e.g. "angular", "eslint", "vitest", "standalone", "signals"), run only that phase.
