---
description: Contribute Servoy-side fixes to the ported MCP files back to upstream AssistAI as draft pull requests (one-way, Servoy → upstream). For SVY-21303.
agent: build
---

Load the `assistai-contribute` skill (call the `skill` tool with id `assistai-contribute`) and
run the one-way Servoy → upstream contribution flow for this Servoy Copilot repository.

The skill selects Servoy-side fixes to the ported MCP service files since the contribute
baseline recorded in `docs/assistai-sync.md`, excludes anything that came in via a forward
sync or was already contributed, triages each with you (general vs Servoy-specific), re-ports
the general fixes into the Java/JDT upstream (re-adding the JDT / AiIgnoreService /
UISynchronize scaffolding the Servoy port stripped), builds and tests the upstream reactor as
a mandatory gate, and opens DRAFT pull requests from the Servoy fork
(github.com/Servoy/eclipse-chatgpt-plugin) against upstream
(github.com/gradusnikov/eclipse-chatgpt-plugin).

Direction is strictly Servoy → upstream. PRs are always draft and never marked ready
automatically; pushing a branch and opening a PR each require your confirmation. No PR is
opened unless the upstream build and tests pass.

On the first run (no contribute baseline recorded) it only establishes the baseline at the
current Copilot HEAD and opens no PRs, per the SVY-21303 bootstrap decision.

User input (optional Servoy commit SHA to use as the contribute baseline for this run; empty
otherwise): $ARGUMENTS
