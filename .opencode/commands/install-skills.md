---
description: Install Servoy global skills and commands into your opencode config.
---

Install the Servoy global skills and commands from this repository into the user's opencode configuration. This makes skills like `servoy-component-migration` and commands like `/migrate` available in any project.

Steps to perform:

1. Determine the absolute path to the `skills/` directory in this repository (relative to the current working directory).

2. Read `~/.config/opencode/opencode.json` (create it if it doesn't exist). The file must be valid JSON with `"$schema": "https://opencode.ai/config.json"`.

3. Ensure `skills.paths` contains the absolute path to this repo's `skills/` directory. If already present, skip. If not, add it.

4. Find all `.md` files in `skills/` subdirectories that are NOT named `SKILL.md` — these are command files. Copy each one to `~/.config/opencode/commands/` (create that directory if needed).

5. Report what was installed and remind the user to restart opencode.

Do NOT use platform-specific commands. Use the built-in file read/write tools which work on any OS.
