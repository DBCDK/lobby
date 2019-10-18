#!groovy

def workerNode = "devel9"

pipeline {
	agent {label workerNode}
	tools {
		// refers to the name set in manage jenkins -> global tool configuration
		maven "Maven 3"
	}
	environment {
		GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
	}
	triggers {
		pollSCM("H/03 * * * *")
		upstream(upstreamProjects: "Docker-payara5-bump-trigger",
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
				sh "mvn -D sourcepath=src/main/java verify pmd:pmd javadoc:aggregate"
				junit "target/surefire-reports/TEST-*.xml"
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
		stage("docker push") {
			when {
                branch "master"
            }
			steps {
				script {
					docker.image("docker-io.dbc.dk/lobby-service:${env.BRANCH_NAME}-${env.BUILD_NUMBER}").push()
				}
			}
		}
		stage("bump docker tag in lobby-secrets") {
			agent {
				docker {
					label workerNode
					image "docker.dbc.dk/build-env:latest"
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
