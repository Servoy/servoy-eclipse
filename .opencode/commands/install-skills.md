---
description: Install Servoy global skills, commands, and plugins into your opencode config.
---

Install the Servoy global skills, commands, and plugins from this repository into the user's opencode configuration. This makes skills like `servoy-component-migration` and `case-review`, commands like `/migrate` and `/case-review`, and shared plugins like `commit-lint`, available in any project.

Steps to perform:

1. Determine the absolute path to the `skills/` directory in this repository (relative to the current working directory). Also determine the absolute path to the `opencode_plugins/` directory in this repository (it sits beside `skills/` at the repo root).

2. Read `~/.config/opencode/opencode.json` (create it if it doesn't exist). The file must be valid JSON with `"$schema": "https://opencode.ai/config.json"`.

3. Ensure `skills.paths` contains the absolute path to this repo's `skills/` directory. If already present, skip. If not, add it.

4. Register the shared plugins. List the `*.ts` (and `*.js`) files directly inside this repo's `opencode_plugins/` directory. Ensure the config has a `plugins` array, then for each plugin file make sure it is referenced by its **absolute path** (use forward slashes; opencode accepts them on Windows):
   - If an existing `plugins` entry is a string ending in `/opencode_plugins/<filename>` (same filename from any checkout path), **replace** it with the freshly resolved absolute path so the reference points at this checkout.
   - Otherwise, append the absolute path.
   - Never add duplicates, and leave every unrelated `plugins` entry (e.g. published packages) untouched.

   This is a reference, not a copy: the plugin source stays in git under `opencode_plugins/`, and the global config only points at it. Because it lives in the global config, it loads for every project and every branch checkout the user opens.

5. Install the command files. A command file is a `.md` file sitting **directly inside a skill directory** (`skills/<skill-name>/*.md`) that is **not** named `SKILL.md`. For each one, compare it against the copy already in `~/.config/opencode/commands/` (create that directory if needed) and classify it:
   - **new** — no file with that name exists in the commands dir yet. Copy it.
   - **updated** — a file exists but its contents differ from the source. Overwrite it.
   - **unchanged** — a file exists and its contents are byte-for-byte identical. Skip the write.

   **Only direct children count.** Do NOT recurse into nested directories such as `skills/<skill-name>/phases/` or `skills/<skill-name>/reference/` — those hold supporting instruction files that the skill reads at runtime. They are not commands, and copying them into `commands/` would create bogus slash commands.

6. Report what was installed, grouped as **new**, **updated**, and **unchanged**, alongside the skills now on the path and the plugins now referenced in `plugins` — then remind the user to restart opencode. If everything was unchanged, the path was already registered, and the plugins were already referenced, say so explicitly (nothing to do).

Do NOT use platform-specific commands. Use the built-in file read/write tools which work on any OS.
