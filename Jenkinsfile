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
		K8S_CLUSTER_NAMESPACE = ""
		SLACK_MESSAGE_PREFIX = "[CORE-Fineract]: "

		FINERACT_DEFAULT_TENANTDB_HOSTNAME=""
		FINERACT_HIKARI_JDBC=""
		FINERACT_TENANTS=""
	}

	stages {
		stage('Continuos Integration (CI)') {
			steps {
				script {
					if(env.BRANCH_NAME != 'main' && env.BRANCH_NAME != 'qa' && env.BRANCH_NAME != 'development') {
						error "Error la rama ${env.BRANCH_NAME} no está contemplada en Pipiline"
					}

					if(env.BRANCH_NAME == 'main') {
						FINERACT_DEFAULT_TENANTDB_HOSTNAME=""
						K8S_CLUSTER_NAMESPACE=""
						FINERACT_HIKARI_JDBC=""
						FINERACT_TENANTS=""
					} else if (env.BRANCH_NAME == 'qa') {
						FINERACT_DEFAULT_TENANTDB_HOSTNAME="SERLINCOREBDDEV.gco.com.co"
						K8S_CLUSTER_NAMESPACE="fineract-qa"
						FINERACT_HIKARI_JDBC="fineract_tenants_qa"
						FINERACT_TENANTS="fineract_tenants_qa"
						PLAYBOOK_NAME = "backend-deployment-${env.BRANCH_NAME}.yaml"
					} else if (env.BRANCH_NAME == 'development') {
						FINERACT_DEFAULT_TENANTDB_HOSTNAME="SERLINCOREBDDEV.gco.com.co"
						K8S_CLUSTER_NAMESPACE="fineract-dev"
						FINERACT_HIKARI_JDBC="fineract_tenants"
						PLAYBOOK_NAME = "backend-deployment-${env.BRANCH_NAME}.yaml"
						FINERACT_TENANTS="fineract_tenants"
					}

					git branch: env.BRANCH_NAME, credentialsId: 'jenkins_gitlab_integration', url: CODE_REPOSITORY
          sh "git rev-parse --short HEAD > .git/commit_id"
          COMMIT_ID = readFile('.git/commit_id').trim()
          IMAGE = "${REGISTRY_URL}/${SERVICE_NAME}:${COMMIT_ID}"
					slackSend(channel: "integrations-ci-cd", color: "good", message: "${SLACK_MESSAGE_PREFIX} Incio de integración Continua (CI) del servicio de Fineract para el commit ${COMMIT_ID}")
					def previousCommit = env.GIT_PREVIOUS_COMMIT
					def shortPreviousCommit = previousCommit.substring(0, 7)
					PREVIOUS_IMAGE = "${REGISTRY_URL}/${SERVICE_NAME}:${shortPreviousCommit}"
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
						/*Explorar la posibilidad de tambien clonar el repositorio en cada uno de los ambientes*/
						git branch: 'main', credentialsId: 'jenkins_gitlab_integration', url: K8S_MANIFESTS_CODE_REPOSITORY
						def lineToReplace = sh(script: "grep fineract: fineract/backend-deployment.yaml | awk '{print \$2}'", returnStdout: true).trim()
						sh "sed -i 's_${lineToReplace}_${IMAGE}_g' fineract/backend-deployment.yaml"
						sh "sed -i 's/fineract_tenants_prod/${FINERACT_TENANTS}/g' fineract/backend-deployment.yaml"
						sh "sed -i 's_ns-fineract-prod_${K8S_CLUSTER_NAMESPACE}_g' fineract/backend-deployment.yaml"
						sh "cp fineract/backend-deployment.yaml ../"
						if(env.BRANCH_NAME == 'main') {
							withCredentials([string(credentialsId: 'gitlab_jenkins_access_token', variable: 'SECRET')]) {
								sh "git add fineract/backend-deployment.yaml"
								sh "git commit -m \"fineract/backend-deployment.yaml file updated ${IMAGE} #1\""
								sh "git push http://amgoez:Angel%20Goez1@10.66.154.26/core/kubernetes-manifests.git main"
							}
						}
					}

					dir('scripts') {
            sh "sudo chmod +x generate-playbook.sh"
            sh "sudo ./generate-playbook.sh ${PLAYBOOK_NAME}"
            sh "sudo cp deploy-fineract-backend-deployment-playbook.yaml ${PLAYBOOKS_LOCATION}"
          }

					sh "sudo cp backend-deployment.yaml ${PLAYBOOKS_LOCATION}/${PLAYBOOK_NAME}"

					sshPublisher(
						publishers: [
							sshPublisherDesc(
								configName: 'Jenkins', transfers: [
									sshTransfer(
										cleanRemote: false, 
										execCommand: "ansible-playbook ${PLAYBOOKS_LOCATION}/deploy-fineract-backend-deployment-playbook.yaml", 
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