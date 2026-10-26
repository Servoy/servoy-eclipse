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
                            sh 'mvn -B -s "$MAVEN_SETTINGS" -t "$TOOLCHAIN" verify -Pintegration -pl com.servoy.eclipse.cypress.tests,com.servoy.eclipse.model.tests,com.servoy.eclipse.designer.tests,com.servoy.eclipse.ngclient.ui.tests,com.servoy.eclipse.ui.tests -am $SERVOY_INSTALL_TARBALL_FOR_APPSERVER_ARG $SERVOY_TEST_HTTP_PORT_ARG $SERVOY_TEST_SHUTDOWN_PORT_ARG'
                        }
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
                    
                    // HTML Publisher voor Coverage rapportages
                    publishHTML([
                        allowMissing: true, 
                        alwaysLinkToLastBuild: false, 
                        keepAll: true, 
                        reportDir: 'com.servoy.eclipse.ngclient.ui/target/coverage', 
                        reportFiles: 'app/index.html,servoy-public/index.html', 
                        reportName: 'Coverage', 
                        reportTitles: ''
                    ])
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
