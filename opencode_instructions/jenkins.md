## Jenkins links (build.servoy.com)

- Any URL on `build.servoy.com` is ALWAYS our Jenkins server. NEVER use `webfetch` or the browser tools for it.
- Load the `jenkins` skill before working with it: it explains the job layout, the `jenkins` MCP tools (Code Mode only, via `execute`), and how to download build artifacts such as coverage reports.
- Never ask the user for a Jenkins username, password or token: the credential is already in the `JENKINS_MCP` environment variable.
