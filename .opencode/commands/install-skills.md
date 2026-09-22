---
description: Install Servoy global skills and commands into your opencode config.
---

Install the Servoy global skills and commands from this repository into the user's opencode configuration. This makes skills like `servoy-component-migration` and `case-review`, and commands like `/migrate` and `/case-review`, available in any project.

Steps to perform:

1. Determine the absolute path to the `skills/` directory in this repository (relative to the current working directory).

2. Read `~/.config/opencode/opencode.json` (create it if it doesn't exist). The file must be valid JSON with `"$schema": "https://opencode.ai/config.json"`.

3. Ensure `skills.paths` contains the absolute path to this repo's `skills/` directory. If already present, skip. If not, add it.

4. Install the command files. A command file is a `.md` file sitting **directly inside a skill directory** (`skills/<skill-name>/*.md`) that is **not** named `SKILL.md`. For each one, compare it against the copy already in `~/.config/opencode/commands/` (create that directory if needed) and classify it:
   - **new** — no file with that name exists in the commands dir yet. Copy it.
   - **updated** — a file exists but its contents differ from the source. Overwrite it.
   - **unchanged** — a file exists and its contents are byte-for-byte identical. Skip the write.

   **Only direct children count.** Do NOT recurse into nested directories such as `skills/<skill-name>/phases/` or `skills/<skill-name>/reference/` — those hold supporting instruction files that the skill reads at runtime. They are not commands, and copying them into `commands/` would create bogus slash commands.

5. Report what was installed, grouped as **new**, **updated**, and **unchanged**, alongside the skills now on the path — then remind the user to restart opencode. If everything was unchanged and the path was already registered, say so explicitly (nothing to do).

Do NOT use platform-specific commands. Use the built-in file read/write tools which work on any OS.
