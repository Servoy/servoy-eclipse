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
                        sh 'export MAVEN_OPTS="-Dmaven.test.failure.ignore=true" && mvn -B -s "$MAVEN_SETTINGS" -t "$TOOLCHAIN" $goals'
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                if (!params.WIPE_WORKSPACE) {
                    // Vitest unit test reports (ngclient.ui + designer.rfb)
                    junit allowEmptyResults: true, testResults: '**/target/vitest-results.xml'
                    
                    // Tycho/Surefire Java test reports
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'
                    
                    // ESLint reports (ngclient.ui + designer.rfb)
                    recordIssues(
                        tools: [esLint(pattern: '**/target/eslint-checkstyle.xml')],
                        qualityGates: [[threshold: 1, type: 'NEW', unstable: true]],
                        enabledForFailure: true
                    )
                    
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
        
        failure {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Failed', adaptiveCards: true
        }
        
        unstable {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Unstable', adaptiveCards: true
            script {
                if (!params.WIPE_WORKSPACE) {
                    build job: 'build', wait: false
                }
            }
        }
        
        fixed {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Back to Normal', adaptiveCards: true
        }
        
        success {
            script {
                if (!params.WIPE_WORKSPACE) {
                    // Downstream project triggeren bij succes
                    build job: 'build', wait: false
                }
            }
        }
    }
}