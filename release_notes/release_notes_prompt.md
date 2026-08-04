# Release Notes Generation Prompt

We are in the parent directory of multiple git repository subdirectories that together make up our Servoy LTS product.

Collect all commits from the following 5 repositories between tag `<PREVIOUS_TAG>` and `HEAD` (these release notes are for `<NEW_VERSION>`):
- `sablo`
- `server`
- `servoy-client`
- `servoy-eclipse`
- `servoy-extensions`

Run `git log --oneline "<PREVIOUS_TAG>..HEAD"` in each repo.

Then do the following:

1. **Bug fixes & changes**: Extract all commits that contain a case reference (`SVY-xxx` or `SVYX-xxx`) and list them in a bug fixes table with columns: Case, Description, Component. List non-case commits that are still meaningful (skip merge commits, version bumps and jenkins/build file changes) in an "Other Changes" table with columns: Component, Description.

2. **Dependency updates**: Compare `<PREVIOUS_TAG>..HEAD` for the following files and list any version changes or newly added dependencies:
   - `servoy-extensions/com.servoy.extensions/pom.xml`
   - All `pom.xml` files in `servoy-eclipse` (use `git diff "<PREVIOUS_TAG>..HEAD" -- "*/pom.xml"`, skip pure version bump changes)
   - `servoy-eclipse/launch_targets/com.servoy.eclipse.target.target`

3. **Output**: Write the full release notes in Markdown format to `servoy-eclipse/release_notes/RELEASE_NOTES_<NEW_VERSION>.md`. Create the `release_notes` directory if it does not exist. The document should contain:
   - A "Bug Fixes" table
   - An "Other Changes" table
   - A "Dependency Updates" section (split per repo/file, with From/To columns)
   - A plain "Case List" code block with all SVY-xxx and SVYX-xxx case numbers

---

> Replace `<PREVIOUS_TAG>` and `<NEW_VERSION>` before using. For example, for the next release: `<PREVIOUS_TAG>` = `2025.3.6` and `<NEW_VERSION>` = `2025.3.7`.
