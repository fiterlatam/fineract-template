pipeline {
  agent any

  options {
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  stages {
    stages('Preparing') {
      steps {
        scripts {

        }
      }
    }
  }
}

stage('Run Docker Container') {
  steps {
    script {
      slackSend channel: '#core-ci-cd',
      color: '#81db02',
      message: "Inició el procesode integración continua",
      teamDomain: 'su-fiter',
      token: 'N3PfCrZCxpFh0WHgWCEXbU26',
      webhookUrl: 'https://hooks.slack.com/services/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'
    }
  }
}