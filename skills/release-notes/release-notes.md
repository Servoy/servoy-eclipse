---
description: Generate Servoy product release notes across the five-repo checkout between two tags — Bug Fixes / Other Changes / Dependency Updates tables and a case list.
agent: general
---

Load the `release-notes` skill and run the release-notes pipeline.

User input: $ARGUMENTS

The argument may name the previous tag and/or the new version (e.g. "2025.3.7", or
"from 2025.3.6 to 2025.3.7"), or be empty.

Auto-infer the previous tag (latest release tag) and the new version (next bump) when not
given, but ALWAYS confirm the final range with the user via the question tool before
collecting anything — a wrong tag silently produces wrong notes.

This skill writes `release_notes/RELEASE_NOTES_<version>.md`. It does not commit or push
unless you explicitly ask.
