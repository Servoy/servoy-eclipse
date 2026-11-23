// Helper for the Jenkinsfile of a Servoy repo: pushes the coverage reports of the current build to the
// combined coverage job (Jenkinsfile.coverage in servoy-eclipse) and starts it.
//
// It copies the given files to the staging folder of the combined job,
//     $JENKINS_HOME/coverage-staging/<line>/<source>/<path in the repo>
// (<line> = folder of the calling job: master, lts_2026, ...), replacing the previous push of that source, and
// then starts '<line>/coverage' without waiting. Jobs that live outside a branch folder (developer_ai_plugin)
// pass the line explicitly. This works because all jobs run on the controller (Built-In Node), which shares
// $JENKINS_HOME; a job on another agent must push through the REST API like the GitHub action does.
//
// It is copied into each Jenkinsfile (there is no shared library); keep the copies the same.
//
// Usage, in post { always { ... } } after the coverage reports were written:
//     pushCoverage('servoy-client', '**/target/site/jacoco/jacoco.xml')
//     pushCoverage('servoy-copilot', '**/target/site/jacoco/jacoco.xml, bundles/com.servoy.eclipse.opencode/target/coverage/cobertura-coverage.xml', 'master')
def pushCoverage(String source, String includes, String line = null) {
    // only builds whose tests ran to the end count; a failed build keeps the previous push
    if (currentBuild.currentResult != 'SUCCESS' && currentBuild.currentResult != 'UNSTABLE') {
        echo "pushCoverage: build is ${currentBuild.currentResult}, coverage of ${source} not pushed"
        return
    }
    if (!(source ==~ /[A-Za-z0-9][A-Za-z0-9._-]{0,79}/)) {
        error "pushCoverage: invalid source name '${source}'"
    }
    def branchLine = line ?: env.JOB_NAME.contains('/') ? env.JOB_NAME.substring(0, env.JOB_NAME.lastIndexOf('/')) : null
    if (!branchLine) {
        echo "pushCoverage: job ${env.JOB_NAME} is not in a branch folder and no line was given, coverage not pushed"
        return
    }
    // Only master feeds the combined report. The same Jenkinsfile also runs in release / lts_* folders after a
    // branch is cut; those builds must not push (there is no combined job for them).
    if (branchLine != 'master') {
        echo "pushCoverage: ${env.JOB_NAME} is on line '${branchLine}', only master pushes coverage"
        return
    }
    def files = findFiles(glob: includes.split(',').collect { it.trim() }.join(','))
    if (files.length == 0) {
        echo "pushCoverage: no files match '${includes}', coverage of ${source} not pushed"
        return
    }
    catchError(buildResult: null, stageResult: null, message: 'coverage push failed') {
        withEnv(["COVERAGE_SOURCE=${source}", "COVERAGE_LINE=${branchLine}"]) {
            writeFile file: '.coverage-push-files', text: files.collect { it.path }.join('\n') + '\n'
            sh '''
                set -eu
                staging="$JENKINS_HOME/coverage-staging/$COVERAGE_LINE"
                new="$staging/$COVERAGE_SOURCE.new.$$"
                rm -rf "$new"
                mkdir -p "$new"
                while IFS= read -r f; do
                    [ -n "$f" ] || continue
                    mkdir -p "$new/$(dirname "$f")"
                    cp "$f" "$new/$f"
                done < .coverage-push-files
                rm -f .coverage-push-files
                rm -rf "$staging/$COVERAGE_SOURCE.old"
                if [ -d "$staging/$COVERAGE_SOURCE" ]; then mv "$staging/$COVERAGE_SOURCE" "$staging/$COVERAGE_SOURCE.old"; fi
                mv "$new" "$staging/$COVERAGE_SOURCE"
                rm -rf "$staging/$COVERAGE_SOURCE.old"
                echo "pushed $(find "$staging/$COVERAGE_SOURCE" -type f | wc -l) coverage file(s) of $COVERAGE_SOURCE to $staging"
            '''
        }
        build job: "/${branchLine}/coverage", wait: false, propagate: false
    }
}
