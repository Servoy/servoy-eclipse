---
description: Review an issue implemented by someone else — change narrative, regression blast radius, security assessment, and a guided walkthrough with a manual test plan.
agent: general
---

Load the `case-review` skill and run the peer-review pipeline.

User input: $ARGUMENTS

The argument is an issue key, an issue URL, or a diff range, optionally followed by a scope
hint (`branch <name>`, `range <A>..<B>`, or `working tree`).

If no arguments are given, review the changes on the current branch relative to its merge
base with the mainline, and say so explicitly before starting.

This aid never pushes and never edits source files. It may, only with your explicit
confirmation at the end, commit a single condensed review summary (and delete the scratch
phase reports), and set the Jira issue's assignee/status.
