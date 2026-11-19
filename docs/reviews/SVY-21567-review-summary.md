# Review summary: SVY-21567 Update Tomcat to 11.0.26+ (follow-up for SVY-21304)

**Risk: MODERATE.** Two Tomcat patch releases (11.0.25 to 11.0.27) land at once in the embedded server, and the commits carry no smoke-test evidence. No Servoy source changed.

**Reviewed scope**
- servoy-eclipse: `0cd04982b7` (lts_2026), `7122cb81a9` (master/release)
- servoy-eclipse-tomcat: `788a206`
- build: `d2fa20e0`

**Release target:** MATCH. Fix Versions 2026.3.2 LTS and 2026.12 both contain the fix.

## Manual test plan

Verifying the fix
1. Launch Servoy Developer from the branch. Check that the `org.apache.tomcat` plug-in is 11.0.27.
2. Start a solution in the NG client. Log in, open forms, and check that WebSocket traffic works and reconnects after a network drop.
3. Open the form designer (RFB) and the WPM. Both use WebSockets.
4. Check the server startup log for Tomcat version 11.0.27.

Regression checks
1. WAR export of a solution, deploy it to a standalone Tomcat, and run it.
2. File upload and download in the NG client.
3. A JSP or other page that Jasper compiles at runtime (ecj 4.41).
4. If any setup uses HTTP/2 or an AJP connector, test it.

## Possible improvements / follow-ups
- Confirm that `org.apache.tomcat` 11.0.27 is published where the product build resolves it. `launch_targets/*.target` has no reference to it.
- Confirm that the build repo's download of `org.apache.tomcat_11.0.27.jar` resolves.
- The commits record no checksums for the jars. Compare them with the Apache 11.0.27 distribution if wanted.
- Confirm whether an AJP connector is configured anywhere. The issue said this needs confirming.
- 11.0.27 fixes more than 11.0.26 does (HTTP/2 and AJP response mix-ups, JNDIRealm), so targeting it rather than 11.0.26 is justified.
