#!groovy

def workerNode = "devel12"
def teamSlackNotice = 'de-notifications'
def teamSlackWarning = 'de-notifications'

pipeline {
	agent {label workerNode}
	tools {
		// refers to the name set in manage jenkins -> global tool configuration
		maven "Maven 3"
	}
	environment {
		GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
		SONAR_SCANNER_HOME = tool 'SonarQube Scanner from Maven Central'
		SONAR_SCANNER = "$SONAR_SCANNER_HOME/bin/sonar-scanner"
		SONAR_PROJECT_KEY = "lobby-service"
		SONAR_SOURCES="src"
		SONAR_TESTS="test"
	}
	triggers {
		pollSCM("H/03 * * * *")
		upstream(upstreamProjects: "Docker-payara6-bump-trigger",
            threshold: hudson.model.Result.SUCCESS)
	}
	options {
		timestamps()
		disableConcurrentBuilds()
	}
	stages {
		stage("clear workspace") {
			steps {
				deleteDir()
				checkout scm
			}
		}
		stage("verify") {
			steps {
				sh "mvn -D sourcepath=src/main/java verify pmd:pmd"
				junit "target/surefire-reports/TEST-*.xml"
			}
		}
		stage("sonarqube") {
			steps {
				withSonarQubeEnv(installationName: 'sonarqube.dbc.dk') {
					script {
						def status = 0

						def sonarOptions = "-Dsonar.branch.name=${BRANCH_NAME}"
						if (env.BRANCH_NAME != 'master') {
							sonarOptions += " -Dsonar.newCode.referenceBranch=master"
						}

						// Do sonar via maven
						status += sh returnStatus: true, script: """
                            mvn -B $sonarOptions sonar:sonar
                        """

						if (status != 0) {
							error("build failed")
						}
					}
				}
			}
		}
		stage("quality gate") {
			steps {
				// wait for analysis results
				timeout(time: 1, unit: 'HOURS') {
					waitForQualityGate abortPipeline: true
				}
			}
		}
		stage("docker push") {
			when {
                branch "master"
            }
			steps {
				script {
					docker.image("docker-metascrum.artifacts.dbccloud.dk/lobby-service:${env.BRANCH_NAME}-${env.BUILD_NUMBER}").push()
				}
			}
		}
		stage("Update staging version number") {
                   when {
                       branch "master"
                   }
                   steps {
                       script {
                           withCredentials([sshUserPrivateKey(credentialsId: "gitlab-isworker", keyFileVariable: "sshkeyfile")]) {
                               env.GIT_SSH_COMMAND = "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i ${sshkeyfile}"
                               sh """
                                   nix run --refresh git+https://gitlab.dbc.dk/public-de-team/gitops-secrets-set-variables.git \
                                       metascrum-staging:LOBBY_SERVICE_VERSION=${env.BRANCH_NAME}-${env.BUILD_NUMBER}
                                   nix run --refresh git+https://gitlab.dbc.dk/public-de-team/gitops-secrets-set-variables.git \
                                       fbstest:LOBBY_SERVICE_VERSION=${env.BRANCH_NAME}-${env.BUILD_NUMBER}
                               """
                           }
                       }
                   }
               }
	}
	post {
        failure {
            script {
                if (BRANCH_NAME == "master") {
                    slackSend(channel: "${teamSlackWarning}",
                            color: 'warning',
                            message: "${JOB_NAME} #${BUILD_NUMBER} failed and needs attention: ${BUILD_URL}",
                            tokenCredentialId: 'slack-global-integration-token')
                }
            }
        }
        success {
            script {
                if (BRANCH_NAME == 'master') {
                    slackSend(channel: "${teamSlackNotice}",
                            color: 'good',
                            message: "${JOB_NAME} #${BUILD_NUMBER} completed.",
                            tokenCredentialId: 'slack-global-integration-token')

                }
            }
        }
        fixed {
            script {
                if (BRANCH_NAME == 'master') {
                    slackSend(channel: "${teamSlackWarning}",
                            color: 'good',
                            message: "${JOB_NAME} #${BUILD_NUMBER} back to normal: ${BUILD_URL}",
                            tokenCredentialId: 'slack-global-integration-token')
                }
            }
        }
    }
}
