// True when a newer build of this job is waiting in the queue (e.g. a push during this build).
// NonCPS: the Jenkins queue objects are not serializable.
@NonCPS
boolean newerBuildQueued(String jobName) {
    return jenkins.model.Jenkins.get().queue.items.any { it.task.ownerTask?.fullName == jobName }
}

pipeline {
    agent any
    
    options {
        // Quiet period en log-rotatie volledig in code gevangen
        quietPeriod(120)
        buildDiscarder(logRotator(daysToKeepStr: '40', numToKeepStr: '70'))
        
        // Nooit twee builds tegelijk (gedeelde workspace). Een nieuwe build wacht in de queue tot de
        // lopende klaar is; we breken de lopende NIET meer automatisch af (abortPrevious), want dat
        // gebeurde ook als hij al bijna klaar was (bv. #517 tijdens de post-acties). Alleen tijdens
        // 'Build with Tycho' (het begin) maakt de lopende build plaats voor een nieuwere, zie daar.
        disableConcurrentBuilds()
    }
    
    triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'ref', value: '$.ref']
            ],
            token: 'servoy-eclipse',
            regexpFilterText: '$ref',
            regexpFilterExpression: "^refs/heads/${env.BRANCH}\$"
        )
    }
    
    parameters {
        // New boolean toggle for manual workspace wiping
        booleanParam(name: 'WIPE_WORKSPACE', defaultValue: false, description: 'Check this box to completely wipe the workspace BEFORE running the build.')

        string(name: 'goals', defaultValue: 'clean install', trim: false)
    }
    
    environment {
        NODE_OPTIONS = '--max_old_space_size=4096'
        TEAMS_WEBHOOK = credentials('servoy-teams-webhook')
        SERVOY_INSTALL_TARBALL_FOR_APPSERVER_ARG = "${env.SERVOY_INSTALL_TARBALL_FOR_APPSERVER ? "-Dservoy.install.tarball.for.appserver=${env.SERVOY_INSTALL_TARBALL_FOR_APPSERVER}" : ''}"
        SERVOY_TEST_HTTP_PORT_ARG = "${env.SERVOY_TEST_HTTP_PORT ? "-Dservoy.test.http.port=${env.SERVOY_TEST_HTTP_PORT}" : ''}"
        SERVOY_TEST_SHUTDOWN_PORT_ARG = "${env.SERVOY_TEST_SHUTDOWN_PORT ? "-Dservoy.test.shutdown.port=${env.SERVOY_TEST_SHUTDOWN_PORT}" : ''}"
    }
    
    tools {
        jdk 'Java 21'
        maven 'Maven 3.9.16'
    }
    
    stages {
        stage('Clear Queued Builds') {
            steps {
                script {
                    // Annuleer builds die in de queue wachten op de quietPeriod timer voor dit specifieke pad
                    def currentJob = env.JOB_NAME
                    def queue = jenkins.model.Jenkins.get().queue
                    
                    queue.items.each { item ->
                        def queuedJobName = item.task.ownerTask?.fullName
                        if (queuedJobName == currentJob) {
                            echo "Removing pending queued build for ${currentJob} (Queue ID #${item.id})..."
                            queue.cancel(item)
                        }
                    }
                }
            }
        }

        // Clean workspace if requested, then automatically re-trigger without wipe
        stage('Manual UI Workspace Wipe') {
            when {
                expression { params.WIPE_WORKSPACE }
            }
            steps {
                echo "Manual workspace wipe requested via UI toggle. Cleaning up..."
                cleanWs()
                
                echo "Re-triggering ${env.JOB_NAME} with WIPE_WORKSPACE = false..."
                build job: env.JOB_NAME, wait: false, parameters: [
                    booleanParam(name: 'WIPE_WORKSPACE', value: false),
                    string(name: 'goals', value: params.goals)
                ]
            }
        }

        // Remove stale test reports so tests that were moved/removed are not re-published
        stage('Clean Test Reports') {
            when {
                expression { !params.WIPE_WORKSPACE }
            }
            steps {
                sh 'find . -path "*/target/surefire-reports" -type d -prune -exec rm -rf {} +'
            }
        }

        // Only runs if WIPE_WORKSPACE is FALSE
        // While the main Maven build runs, a 'supersede' branch watches the queue: if a newer build of this
        // job gets queued (a new push), this build is still early and stops itself as NOT_BUILT so the newer
        // one can start. Once 'Build with Tycho' is done the build is far along and always finishes
        // (integration tests, coverage, post actions); a newer build then simply waits in the queue.
        stage('Build with Tycho') {
            when {
                expression { !params.WIPE_WORKSPACE }
            }
            steps {
                script {
                    def tychoDone = false
                    parallel(
                        failFast: true,
                        'maven build': {
                            wrap([$class: 'Xvfb', installationName: 'xvfb', autoDisplayName: true]) {
                                configFileProvider([
                                    configFile(fileId: 'master_mvn_repo', variable: 'MAVEN_SETTINGS'),
                                    configFile(fileId: 'maven_toolchain', variable: 'TOOLCHAIN')
                                ]) {
                                    sh 'mvn -B -fae -s "$MAVEN_SETTINGS" -t "$TOOLCHAIN" $goals'
                                }
                            }
                            tychoDone = true
                        },
                        'supersede': {
                            // polls with a growing interval (max 15 s)
                            waitUntil(initialRecurrencePeriod: 15000, quiet: true) {
                                tychoDone || newerBuildQueued(env.JOB_NAME)
                            }
                            if (!tychoDone) {
                                // NOT_BUILT (like the former abortPrevious); failFast then stops the maven branch
                                currentBuild.result = 'NOT_BUILT'
                                error('Superseded by a newer queued build while still in Build with Tycho')
                            }
                        }
                    )
                }
            }
        }

        stage('Integration Tests') {
            when {
                expression { !params.WIPE_WORKSPACE }
            }
            steps {
                wrap([$class: 'Xvfb', installationName: 'xvfb', autoDisplayName: true]) {
                    configFileProvider([
                        configFile(fileId: 'master_mvn_repo', variable: 'MAVEN_SETTINGS'),
                        configFile(fileId: 'maven_toolchain', variable: 'TOOLCHAIN')
                    ]) {
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            // -fae: a failing .tests project must not SKIP the remaining ones
                            sh 'mvn -B -fae -s "$MAVEN_SETTINGS" -t "$TOOLCHAIN" verify -Pintegration -pl com.servoy.eclipse.cypress.tests,com.servoy.eclipse.model.tests,com.servoy.eclipse.designer.tests,com.servoy.eclipse.ngclient.ui.tests,com.servoy.eclipse.ui.tests -am $SERVOY_INSTALL_TARBALL_FOR_APPSERVER_ARG $SERVOY_TEST_HTTP_PORT_ARG $SERVOY_TEST_SHUTDOWN_PORT_ARG'
                        }
                    }
                }
            }
        }

        // JaCoCo report for ALL Java tests (plainjunit + integrationpde), same recipe as Servoy-Copilot.
        // Runs AFTER 'Integration Tests' on purpose: 'Build with Tycho' wrote target/jacoco.exec (plainjunit) in
        // every .tests module, the integration run wrote target/jacoco-it.exec. The jacoco 'report' goal can only
        // analyze the current module's own classes, so it runs in each PRODUCTION bundle (-Pcoverage-report, root
        // pom), which first merges the exec files of all .tests modules into target/jacoco-merged.exec and then
        // reports its own classes against it. -Pplugins: activating a profile deactivates the activeByDefault one.
        stage('Coverage Report') {
            when {
                expression { !params.WIPE_WORKSPACE }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'master_mvn_repo', variable: 'MAVEN_SETTINGS'),
                    configFile(fileId: 'maven_toolchain', variable: 'TOOLCHAIN')
                ]) {
                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                        sh 'mvn -B -fae -s "$MAVEN_SETTINGS" -t "$TOOLCHAIN" -Pplugins,coverage-report org.jacoco:jacoco-maven-plugin:merge org.jacoco:jacoco-maven-plugin:report -pl com.servoy.eclipse.model,com.servoy.eclipse.model.exporter.mobile,com.servoy.eclipse.core,com.servoy.eclipse.cloud,com.servoy.eclipse.ui,com.servoy.eclipse.ngclient,com.servoy.eclipse.debug,com.servoy.eclipse.designer,com.servoy.eclipse.designer.rfb,com.servoy.eclipse.designer.wpm,com.servoy.eclipse.jsunit,com.servoy.eclipse.exporter.ngdesktop,com.servoy.eclipse.exporter.mobile,com.servoy.eclipse.exporter.solution,com.servoy.eclipse.exporter.war,com.servoy.eclipse.extension.compatibility,com.servoy.eclipse.profiler,com.servoy.eclipse.notification,com.servoy.eclipse.ui.tweaks,j2db_documentation,com.servoy.eclipse.ngclient.ui,com.servoy.eclipse.cypress'
                    }
                }
            }
        }

    }
    
    post {
        always {
            script {
                if (!params.WIPE_WORKSPACE) {
                    // Vitest unit test reports (ngclient.ui per-project files, designer.rfb incl. browser, designer.wpm)
                    junit allowEmptyResults: true, testResults: '**/target/vitest-*.xml'
                    
                    // Tycho/Surefire Java test reports
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'
                    
                    // Angular Vitest code coverage (Cobertura) for the three Angular projects.
                    // Each project's frontend-maven-plugin run merges its suites into
                    // target/coverage/combined/cobertura-coverage.xml (ngclient.ui + designer.rfb)
                    // or target/coverage/cobertura-coverage.xml (designer.wpm).
                    //
                    // ONE recordCoverage PER PROJECT, each with its own id/name: every Cobertura
                    // report presents itself as the same unnamed single module, so listing all three
                    // as 'tools' of a single recordCoverage made the last-parsed report (wpm) overwrite
                    // the others in the merged tree. Separate steps give three independent coverage
                    // results, each with its own entry in the build's left menu and its own trend -
                    // mirroring the three HTML tabs.
                    //
                    // ignoreParsingErrors: the istanbul cobertura reporter can still emit odd elements
                    // (e.g. a stray duplicate <method>) that the coverage-model parser rejects;
                    // coverage-merge.js de-duplicates method names per class, but this keeps a single
                    // malformed entry from failing the build.
                    recordCoverage(
                        tools: [[parser: 'COBERTURA', pattern: 'com.servoy.eclipse.ngclient.ui/target/coverage/combined/cobertura-coverage.xml']],
                        id: 'vitest-ngclient-ui',
                        name: 'Vitest Coverage - TiNG (ngclient.ui)',
                        ignoreParsingErrors: true,
                        sourceCodeRetention: 'EVERY_BUILD'
                    )
                    recordCoverage(
                        tools: [[parser: 'COBERTURA', pattern: 'com.servoy.eclipse.designer.rfb/target/coverage/combined/cobertura-coverage.xml']],
                        id: 'vitest-designer-rfb',
                        name: 'Vitest Coverage - Designer RFB',
                        ignoreParsingErrors: true,
                        sourceCodeRetention: 'EVERY_BUILD'
                    )
                    recordCoverage(
                        tools: [[parser: 'COBERTURA', pattern: 'com.servoy.eclipse.designer.wpm/target/coverage/cobertura-coverage.xml']],
                        id: 'vitest-designer-wpm',
                        name: 'Vitest Coverage - WPM',
                        ignoreParsingErrors: true,
                        sourceCodeRetention: 'EVERY_BUILD'
                    )
                    
                    // Browsable HTML coverage reports (one tab per Angular project).
                    // publishHTML copies its whole reportDir into the build. With reportDir '.' that was the
                    // entire workspace (all node_modules, target folders, ...), which took minutes per build.
                    // So first stage only the three HTML report folders into one small directory.
                    sh '''
                        rm -rf angular-coverage-html
                        mkdir -p angular-coverage-html
                        stage_report() { if [ -d "$1" ]; then cp -r "$1" "angular-coverage-html/$2"; fi; }
                        stage_report com.servoy.eclipse.ngclient.ui/target/coverage/combined ting
                        stage_report com.servoy.eclipse.designer.rfb/target/coverage/combined rfb
                        stage_report com.servoy.eclipse.designer.wpm/target/coverage/lcov-report wpm
                    '''
                    publishHTML([
                        allowMissing: true, 
                        alwaysLinkToLastBuild: false, 
                        keepAll: true, 
                        reportDir: 'angular-coverage-html', 
                        reportFiles: 'ting/index.html,rfb/index.html,wpm/index.html', 
                        reportName: 'Angular Coverage', 
                        reportTitles: 'TiNG,Designer RFB,WPM'
                    ])
                    
                    // Java (JaCoCo) code coverage of the plainjunit + integrationpde tests (merged), one
                    // jacoco.xml per production bundle written by the 'Coverage Report' stage, aggregated
                    // here into a single Java coverage trend. EVERY_BUILD keeps the sources so the Coverage
                    // view shows covered/missed lines per .java file.
                    // sourceDirectories: jacoco.xml only holds package-relative file names
                    // (com/servoy/eclipse/...), and the plugin otherwise only looks in src/main/java, which
                    // these Eclipse bundles do not use (build 518: "found: 0, not found: 1706"). List each
                    // production bundle's src folder so the line view can show the sources.
                    recordCoverage(
                        tools: [[parser: 'JACOCO', pattern: '**/target/site/jacoco/jacoco.xml']],
                        id: 'java-jacoco',
                        name: 'Java Coverage (JaCoCo, unit + integration)',
                        sourceCodeRetention: 'EVERY_BUILD',
                        sourceDirectories: [
                            'com.servoy.eclipse.model', 'com.servoy.eclipse.model.exporter.mobile', 'com.servoy.eclipse.core',
                            'com.servoy.eclipse.cloud', 'com.servoy.eclipse.ui', 'com.servoy.eclipse.ngclient',
                            'com.servoy.eclipse.debug', 'com.servoy.eclipse.designer', 'com.servoy.eclipse.designer.rfb',
                            'com.servoy.eclipse.designer.wpm', 'com.servoy.eclipse.jsunit', 'com.servoy.eclipse.exporter.ngdesktop',
                            'com.servoy.eclipse.exporter.mobile', 'com.servoy.eclipse.exporter.solution', 'com.servoy.eclipse.exporter.war',
                            'com.servoy.eclipse.extension.compatibility', 'com.servoy.eclipse.profiler', 'com.servoy.eclipse.notification',
                            'com.servoy.eclipse.ui.tweaks', 'j2db_documentation', 'com.servoy.eclipse.ngclient.ui', 'com.servoy.eclipse.cypress'
                        ].collect { [path: "${it}/src"] }
                    )

                    // Archive the coverage reports so they can be read per build (also via the Jenkins MCP).
                    // Jenkins keeps the workspace-relative path, so each file shows up under <module>/target/.
                    // jacoco.xml = the per-bundle JaCoCo report (per class/method/line/branch counters, what the
                    // published number reflects); the cobertura files are the Angular Vitest coverage.
                    // The binary jacoco*.exec files are deliberately NOT archived: they can only be turned into a
                    // report again together with the exact class files of this build (which are not archived),
                    // and everything readable is already in jacoco.xml.
                    archiveArtifacts artifacts: '**/target/site/jacoco/jacoco.xml, com.servoy.eclipse.ngclient.ui/target/coverage/combined/cobertura-coverage.xml, com.servoy.eclipse.designer.rfb/target/coverage/combined/cobertura-coverage.xml, com.servoy.eclipse.designer.wpm/target/coverage/cobertura-coverage.xml', allowEmptyArchive: true, fingerprint: false
                }
            }
        }
        
        changed {
                script {
                    def statusMap = [
                        'SUCCESS' : 'Back to Normal',
                        'FAILURE' : 'Failed',
                        'UNSTABLE': 'Unstable',
                        'ABORTED' : 'Aborted'
                    ]
                    
                    def statusText = statusMap.get(currentBuild.currentResult, currentBuild.currentResult)
        
                    office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: statusText, adaptiveCards: true
                }
        }
        
        success {
            script {
                if (!params.WIPE_WORKSPACE) {
                    build job: 'build', wait: false
                }
            }
        }
    
        unstable {
            script {
                if (!params.WIPE_WORKSPACE) {
                    build job: 'build', wait: false
                }
            }
        }
    }
}
