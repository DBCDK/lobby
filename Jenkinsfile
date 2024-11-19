#!groovy

def workerNode = "devel12"

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
		stage("warnings") {
			agent {label workerNode}
			steps {
				warnings consoleParsers: [
					[parserName: "Java Compiler (javac)"],
					[parserName: "JavaDoc Tool"]
				],
					unstableTotalAll: "0",
					failedTotalAll: "0"
			}
		}
		stage("pmd") {
			agent {label workerNode}
			steps {
				step([$class: 'hudson.plugins.pmd.PmdPublisher',
					  pattern: '**/target/pmd.xml',
					  unstableTotalAll: "0",
					  failedTotalAll: "0"])
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
		stage("bump docker tag in lobby-secrets") {
			agent {
				docker {
					label workerNode
					image "docker-dbc.artifacts.dbccloud.dk/build-env:latest"
					alwaysPull true
				}
			}
			when {
				branch "master"
			}
			steps {
				script {
					sh """  
            set-new-version services/lobby.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/lobby-secrets  ${env.BRANCH_NAME}-${env.BUILD_NUMBER} -b staging
          """
				}
			}
		}
	}
}
