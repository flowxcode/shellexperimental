// stub for succf project runner conf
pipeline {
    agent { label "${AGENT}" }
    options {
        buildDiscarder(logRotator(numToKeepStr: '48'))
        skipDefaultCheckout(true)
        copyArtifactPermission('/Runners/*, /xyz/*')
    }
    parameters {
        choice(name: 'AGENT', choices: ['GRZ-3', 'GRZ-4'], description: 'Select the agent ')
    }

    stages {
        stage('Start testsuite') {
            steps {
                script {
                    // remove all files (empty folders will stay)
                    bat 'del /s /q *'

                    bat 'nuget install nunit.consolerunner -version 3.11.1'
                    bat 'nuget install ReportUnit -version 1.2.1'

                    copyArtifacts(fingerprintArtifacts: true, projectName: "${params.UPSTREAM_PROJECT}", selector: lastSuccessful())

                    // remove the results
                    bat 'del /Q results'

                    categories = "${TESTGROUPS}".split(',')
                    categories.each {
                        stage("Testing ${it}") {
                            // Run NUnit tests
                            def fltr = readFile "NunitConfigurations\\${params.FILTER}.filter"
                            bat(script: "NUnit.ConsoleRunner.3.11.1\\tools\\nunit3-console.exe output\\${it}2Tests.dll --result=.\\results\\${it}.xml ${fltr}", returnStatus: true)
                            bat(script: "ReportUnit.1.2.1\\tools\\ReportUnit.exe results\\${it}.xml")
                            archiveArtifacts(artifacts: 'results/*.*', fingerprint: true, onlyIfSuccessful:true, allowEmptyArchive:true)

                        // Upload results to testrail ..
                        }
                    }
                }
            }
        }
    }
}
