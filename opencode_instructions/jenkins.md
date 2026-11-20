## Jenkins links (build.servoy.com)

- Any URL on `build.servoy.com` is ALWAYS our Jenkins server (jobs, builds, console output, test reports, etc.).
- For such a URL, NEVER use `webfetch` (or the browser tools). Use the `jenkins` MCP tools instead, called through `execute` (e.g. `tools.jenkins.<tool>(...)`). If the tool name or signature is unknown, find it with `search({ namespace: "jenkins" })` inside `execute`.
- Derive the job name / build number from the URL path and pass those to the Jenkins tools. Every `/job/<x>` segment is one level of the folder path, so `https://build.servoy.com/job/master/job/servoy-eclipse/514/` → `jobFullName: "master/servoy-eclipse"`, `buildNumber: 514`. (The real server URLs contain an extra `/jenkins/` prefix, ignore it.)

### Job layout

Top-level folders = branch / release line. Each folder has (roughly) the same jobs:
- `master` = current development, `release` = release branch, `lts_2026`, `lts_2025`, `lts_2024` = LTS lines, `7.4_jobs` = very old 7.4 branch.
- Per-repo build jobs: `sablo`, `server`, `servoy-client`, `servoy-eclipse`, `servoy-eclipse-tomcat`, `servoy-extensions`, `servoy-js-engine`, `servoy-mobile`, `servoy-plugins`, `servoy-doc`, `docgenerator-ui`.
- `build` = the overall build job of that line.
- Installers: `make_installer` (Servoy installer) and `make_installer_eclipse` (Servoy Developer / Eclipse installer).
- Tests: `servoy_test_e2e`, `servoy_test_jsunit`, `test_run_java_db_test`, `test_create_jar`; also `snyk_test` (security scan) and `version_bumb` (version bump multijob).
- `lts_2024` is older and differs: most jobs are freestyle/multijob (not pipelines) and it has extra jobs like `servoy-runtime-client`, `servoy-client-tunnel`, `servoy_test`, `servoy_test_mobile`, `test_run_test`, `trigger_mobile_test`.

So "does the installer of lts2024 build fine" → `lts_2024/make_installer` (and `lts_2024/make_installer_eclipse`); use `getBuild` for the last build, `getBuildLog`/`searchBuildLog` for failures. Folder names are the `jobFullName` prefix, so you can go straight to e.g. `lts_2026/servoy-eclipse` without searching. If a job is not in this list, use `getJobs` (with `parentFullName`) to look.
