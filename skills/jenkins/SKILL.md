---
name: jenkins
description: "Use when working with our Jenkins server build.servoy.com: why a job/build failed, its console log, test results, which job builds what (branch folders master/release/lts_*, installers, test jobs), and reading a build's archived artifacts such as the JaCoCo jacoco.xml / Vitest cobertura coverage reports. Triggered by any build.servoy.com URL, 'jenkins', 'why does the build fail', 'build log', 'does the installer build', 'coverage of build N', 'jacoco.xml', 'build artifacts', 'which classes are not covered'."
---

# Skill: jenkins

Everything about our Jenkins server `build.servoy.com`: finding the right job, reading builds,
logs and test results through the `jenkins` MCP, and downloading a build's archived artifacts
(coverage reports) through the Jenkins REST API.

The always-on rules (a `build.servoy.com` URL is Jenkins, never `webfetch` it, never ask for a
Jenkins credential) live in the global instruction `opencode_instructions/jenkins.md`.

## 1. URL → job and build

Every `/job/<x>` segment of a URL is one level of the folder path:
`https://build.servoy.com/job/master/job/servoy-eclipse/514/` → `jobFullName: "master/servoy-eclipse"`,
`buildNumber: 514`. Real server URLs carry an extra `/jenkins/` prefix; ignore it for the MCP
tools (but the REST API needs it, see section 4).

## 2. Job layout

Top-level folders = branch / release line. Each folder has (roughly) the same jobs:
- `master` = current development, `release` = release branch, `lts_2026`, `lts_2025`, `lts_2024`
  = LTS lines, `7.4_jobs` = very old 7.4 branch.
- Per-repo build jobs: `sablo`, `server`, `servoy-client`, `servoy-eclipse`, `servoy-eclipse-tomcat`,
  `servoy-extensions`, `servoy-js-engine`, `servoy-mobile`, `servoy-plugins`, `servoy-doc`,
  `docgenerator-ui`.
- `build` = the overall build job of that line.
- Installers: `make_installer` (Servoy installer) and `make_installer_eclipse` (Servoy Developer /
  Eclipse installer).
- Tests: `servoy_test_e2e`, `servoy_test_jsunit`, `test_run_java_db_test`, `test_create_jar`; also
  `snyk_test` (security scan) and `version_bumb` (version bump multijob).
- `lts_2024` is older and differs: most jobs are freestyle/multijob (not pipelines) and it has extra
  jobs like `servoy-runtime-client`, `servoy-client-tunnel`, `servoy_test`, `servoy_test_mobile`,
  `test_run_test`, `trigger_mobile_test`.
- Outside the branch folders: `developer_ai_plugin` = the Servoy-Copilot build.

So "does the installer of lts2024 build fine" → `lts_2024/make_installer` (and
`lts_2024/make_installer_eclipse`). Folder names are the `jobFullName` prefix, so go straight to
e.g. `lts_2026/servoy-eclipse` without searching. If a job is not in this list, use `getJobs`
(with `parentFullName`) to look.

## 3. The `jenkins` MCP (builds, logs, tests)

Only reachable through Code Mode: call `tools.jenkins.<tool>(...)` inside `execute`. Find an exact
signature with `search({ namespace: "jenkins" })` (synchronous, inside `execute`).

- `getJob` → last build / last completed build / health. `getBuild` → result, `building`,
  duration; pass a `tree` such as `number,result,building,artifacts[relativePath]` to keep it small.
- Failures: `searchBuildLog` with a regex first (e.g. `Failed to execute goal|BUILD FAILURE|\[ERROR\]`,
  `useRegex: true`), then `getBuildLog` with `skip`/`limit` around the matched line numbers.
  `skip` negative counts from the end. Read the `Reactor Summary` to see which module failed and
  which were SKIPPED because of it.
- `getTestResults` / `getFlakyFailures` for test reports; `getBuildChangeSets` for the commits.
- A running build: check `building` and `estimatedDuration`; do not poll tightly.

The MCP has **no tool to download artifacts**. Use section 4 for that.

## 4. Build artifacts via the REST API (same credential as the MCP)

Call the Jenkins REST API from the **`shell`** tool, authenticated with the credential the MCP
itself uses:

- `JENKINS_MCP` is already in the environment (referenced from `~/.config/opencode/opencode.json`)
  and is `base64("user:apitoken")`. Send it as `Authorization: Basic $env:JENKINS_MCP`. Never ask
  the user for a Jenkins user, password or token, and never print the value. If `JENKINS_MCP` is
  empty, tell the user it is missing.
- Do NOT use `fetch` inside `execute`: it carries no credentials and gets a `403` with a redirect
  to the login page.
- REST URLs need the real `/jenkins/` prefix:
  `https://build.servoy.com/jenkins/job/<folder>/job/<job>/<build>/...`
  - list: `.../<build>/api/json?tree=building,result,artifacts[relativePath]`
  - download: `.../<build>/artifact/<relativePath>`
- Artifacts are archived at the very end of the build: check `building` is `false` first, an
  unfinished build lists none.

```powershell
$h = @{ Authorization = "Basic $($env:JENKINS_MCP)" }
$base = "https://build.servoy.com/jenkins/job/master/job/servoy-eclipse/517"
$b = Invoke-RestMethod -Uri "$base/api/json?tree=building,result,artifacts[relativePath]" -Headers $h
$xml = (Invoke-WebRequest -Uri "$base/artifact/com.servoy.eclipse.model/target/site/jacoco/jacoco.xml" -Headers $h).Content
```

## 5. Coverage reports

What the builds archive (the artifact path is the workspace-relative path, hence `<module>/target/...`):
- **servoy-eclipse** (`<line>/servoy-eclipse`): `<bundle>/target/site/jacoco/jacoco.xml`, one per
  production bundle (Java, plainjunit + integrationpde merged), plus the Angular Vitest
  `com.servoy.eclipse.ngclient.ui/target/coverage/combined/cobertura-coverage.xml`,
  `com.servoy.eclipse.designer.rfb/target/coverage/combined/cobertura-coverage.xml`,
  `com.servoy.eclipse.designer.wpm/target/coverage/cobertura-coverage.xml`.
- **Servoy-Copilot** (`developer_ai_plugin`): `<bundle>/target/site/jacoco/jacoco.xml` for
  `developer.mcp` and `opencode`, plus the webui `cobertura-coverage.xml`.
- The binary `jacoco*.exec` files are intentionally not archived; they are useless without the
  exact class files of that build and everything readable is in `jacoco.xml`.

Reading them:
- Only the totals: no download needed, the Coverage plugin writes them to the build log. Use
  `searchBuildLog` for `\[Coverage\] (Successfully parsed file|Line Coverage|Branch Coverage|Lines of Code)`.
- Per bundle with the least-covered classes: run `scripts/jacoco-baseline.ps1` from this skill
  directory. `jacoco.xml` declares a DTD that is not reachable, the script strips `<!DOCTYPE ...>`
  before parsing (do the same if you parse it yourself).

```powershell
pwsh -NoProfile -File <skill-dir>/scripts/jacoco-baseline.ps1 -Job master/servoy-eclipse -BuildNumber 517 -Out D:\TEMP\opencode\coverage-517.json
```

It prints per bundle `lines`, `linePct`, `branchPct` and `linesToTarget` (covered lines still
needed for `-TargetPct`, default 70) and writes the same plus the `-TopClasses` (default 8)
classes with the most missed lines to the JSON file.

**On Linux / macOS without `pwsh`:** do not install PowerShell. Generate a temporary bash script
(e.g. under `/tmp`) that does the same as `jacoco-baseline.ps1`, using it as the example: same
parameters (job, build number, output file, target %, top classes), same `JENKINS_MCP` Basic auth
(never print it), `curl -sSf` for the REST calls, and `python3` (standard library only, for the JSON
and the XML parsing) instead of `[xml]`. Keep the same checks (build still running, no
`jacoco.xml` found), strip the `<!DOCTYPE ...>` before parsing, and produce the same JSON and table
so the results are comparable across platforms. Delete the temporary script afterwards.
