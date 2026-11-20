pipeline {
    agent any
    
    options {
        // Quiet period en log-rotatie volledig in code gevangen
        quietPeriod(120)
        buildDiscarder(logRotator(daysToKeepStr: '40', numToKeepStr: '70'))
        
        // Annuleer een eventueel al LOPENDE build als er een nieuwe start
        disableConcurrentBuilds(abortPrevious: true)
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
        stage('Build with Tycho') {
            when {
                expression { !params.WIPE_WORKSPACE }
            }
            steps {
                wrap([$class: 'Xvfb', installationName: 'xvfb', autoDisplayName: true]) {
                    configFileProvider([
                        configFile(fileId: 'master_mvn_repo', variable: 'MAVEN_SETTINGS'),
                        configFile(fileId: 'maven_toolchain', variable: 'TOOLCHAIN')
                    ]) {
                        sh 'mvn -B -fae -s "$MAVEN_SETTINGS" -t "$TOOLCHAIN" $goals'
                    }
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
                    publishHTML([
                        allowMissing: true, 
                        alwaysLinkToLastBuild: false, 
                        keepAll: true, 
                        reportDir: '.', 
                        reportFiles: 'com.servoy.eclipse.ngclient.ui/target/coverage/combined/index.html,com.servoy.eclipse.designer.rfb/target/coverage/combined/index.html,com.servoy.eclipse.designer.wpm/target/coverage/lcov-report/index.html', 
                        reportName: 'Angular Coverage', 
                        reportTitles: 'TiNG,Designer RFB,WPM'
                    ])
                    
                    // Java (JaCoCo) code coverage of the plainjunit + integrationpde tests (merged), one
                    // jacoco.xml per production bundle written by the 'Coverage Report' stage, aggregated
                    // here into a single Java coverage trend. EVERY_BUILD keeps the sources so the Coverage
                    // view shows covered/missed lines per .java file.
                    recordCoverage(
                        tools: [[parser: 'JACOCO', pattern: '**/target/site/jacoco/jacoco.xml']],
                        id: 'java-jacoco',
                        name: 'Java Coverage (JaCoCo, unit + integration)',
                        sourceCodeRetention: 'EVERY_BUILD'
                    )

                    // Archive the raw coverage data so it can be read per build (also via the Jenkins MCP).
                    // The exec files are kept separately so unit vs integration can be split offline:
                    // jacoco.exec = plainjunit, jacoco-it.exec = integrationpde, jacoco-merged.exec = both
                    // (what the published number reflects). The cobertura files are the Angular Vitest coverage.
                    archiveArtifacts artifacts: '**/target/site/jacoco/jacoco.xml, **/target/jacoco.exec, **/target/jacoco-it.exec, **/target/jacoco-merged.exec, com.servoy.eclipse.ngclient.ui/target/coverage/combined/cobertura-coverage.xml, com.servoy.eclipse.designer.rfb/target/coverage/combined/cobertura-coverage.xml, com.servoy.eclipse.designer.wpm/target/coverage/cobertura-coverage.xml', allowEmptyArchive: true, fingerprint: false
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
