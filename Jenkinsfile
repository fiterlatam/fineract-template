pipeline {
	agent any

	environment {
		COMMIT_ID = ""
		SERVICE = "Fineract"
		SERVICE_NAME = "fineract"
		PREVIOUS_IMAGE = ""
		REGISTRY_URL = "10.66.166.18:8123"
		IMAGE = ""
		CODE_REPOSITORY = "http://10.66.154.26/core/fineract.git"
		K8S_MANIFESTS_CODE_REPOSITORY = "http://10.66.154.26/core/kubernetes-manifests.git"
		PLAYBOOKS_LOCATION = "/opt/playbooks/manager"
		PLAYBOOK_NAME = "backend-deployment.yaml"
		K8S_CLUSTER = ""
		SLACK_MESSAGE_PREFIX = "[CORE-Fineract]: "
	}

	stages {
		stage('Continuos Integration (CI)') {
			steps {
				script {
					git branch: 'main', credentialsId: 'jenkins_gitlab_integration', url: CODE_REPOSITORY
          sh "git rev-parse --short HEAD > .git/commit_id"
          COMMIT_ID = readFile('.git/commit_id').trim()
          IMAGE = "${REGISTRY_URL}/${SERVICE_NAME}:${COMMIT_ID}"
					slackSend(channel: "integrations-ci-cd", color: "good", message: "${SLACK_MESSAGE_PREFIX} Incio de integración Continua (CI) del servicio de Fineract para el commit ${COMMIT_ID}")
				}
			}
		}

		stage('Building') {
      steps {
				script {
					slackSend(channel: "integrations-ci-cd", color: "good", message: "${SLACK_MESSAGE_PREFIX} Incio de construcción de la imagen de Fineract para el commit ${COMMIT_ID}")
          dockerImage = docker.build "${IMAGE}"
					slackSend(channel: "integrations-ci-cd", color: "good", message: "${SLACK_MESSAGE_PREFIX} Construcción de la imagen de Fineract para el commit ${COMMIT_ID} finalizada con éxito")
				}
			}
    }

		 stage('Publish') {
			steps {
				script {
          slackSend(channel: "integrations-ci-cd", color: "good", message: "${SLACK_MESSAGE_PREFIX} Incio de publicación de la imagen de Fineract para el commit ${COMMIT_ID}")
					docker.withRegistry("http://10.66.166.18:8123", "inter-registry-user"){
						dockerImage.push()
					}
          slackSend(channel: "integrations-ci-cd", color: "good", message: "${SLACK_MESSAGE_PREFIX} Publicación de la imagen de Fineract para el commit ${COMMIT_ID} finalizada con éxito")
				}
			}
		}

		stage('Prune') {
			steps {
				script {
          slackSend(channel: "integrations-ci-cd", color: "good", message: "${SLACK_MESSAGE_PREFIX} Depuración de la imagen de Fineract para el commit ${COMMIT_ID}")
					sh "docker rmi ${dockerImage.id}"
				}
			}
		}

		stage('Continuos Delivery (CI)') {
      steps {
        script {
          slackSend(channel: "integrations-ci-cd", color: "good", message: "${SLACK_MESSAGE_PREFIX} Incio del proceso de Entrega Continua (CD) de la imagen de Fineract para el commit ${COMMIT_ID}")
					
					dir('kubernetes-manifests') {
						git branch: 'main', credentialsId: 'jenkins_gitlab_integration', url: K8S_MANIFESTS_CODE_REPOSITORY
						def lineToReplace = sh(script: "grep fineract: fineract/backend-deployment.yaml | awk '{print \$2}'", returnStdout: true).trim()
						sh "sed -i 's_${lineToReplace}_${IMAGE}_g' fineract/backend-deployment.yaml"
						sh "cp fineract/backend-deployment.yaml ../"
						withCredentials([string(credentialsId: 'gitlab_jenkins_access_token', variable: 'SECRET')]) {
              sh "git add fineract/backend-deployment.yaml"
              sh "git commit -m \"fineract/backend-deployment.yaml file updated ${IMAGE} #1\""
              sh "git push http://amgoez:Angel%20Goez1@10.66.154.26/core/kubernetes-manifests.git main"
            }
					}

					dir('scripts') {
            sh "sudo chmod +x generate-playbook.sh"
            sh "sudo ./generate-playbook.sh"
            sh "sudo cp deploy-fineract-backend-deployment-playbook.yaml ${PLAYBOOKS_LOCATION}"
          }

					sh "sudo cp backend-deployment.yaml ${PLAYBOOKS_LOCATION}"

					sshPublisher(
						publishers: [
							sshPublisherDesc(
								configName: 'Jenkins', transfers: [
									sshTransfer(
										cleanRemote: false, 
										execCommand: "ansible-playbook ${PLAYBOOKS_LOCATION}/${PLAYBOOK_NAME}", 
										execTimeout: 240000, 
									)
								], 
								usePromotionTimestamp: false, 
								useWorkspaceInPromotion: false, 
								verbose: false
							)
						]
					)

					slackSend(channel: "integrations-ci-cd", color: "good", message: "${SLACK_MESSAGE_PREFIX} Proceso de Entrega Continua (CD) de la imagen de Mifos para el commit ${COMMIT_ID} finalizado correctamente")
				}
			}
		}

	}

}