---
description: Sync the Servoy Copilot MCP bundle's ported files with upstream AssistAI changes (one-way, upstream → Servoy). For SVY-21303.
agent: build
---

Load the `assistai-sync` skill (call the `skill` tool with id `assistai-sync`) and run the
one-way AssistAI upstream sync for this Servoy Copilot repository.

The skill fetches new commits from the upstream AssistAI project
(github.com/gradusnikov/eclipse-chatgpt-plugin) since the baseline recorded in
`docs/assistai-sync.md`, reviews each change that touches a ported MCP service file WITH you,
and applies the approved updates — preserving the deliberate port differences (no JDT, no
AiIgnoreService, no editor-UI refresh). Direction is strictly upstream → Servoy; it never
opens pull requests on the upstream repo.

On the first run (no baseline recorded) it only establishes the baseline at the current
upstream HEAD and changes no code, per the SVY-21303 bootstrap decision.

User input (optional upstream commit SHA / ref to use as the baseline for this run; empty
otherwise): $ARGUMENTS
