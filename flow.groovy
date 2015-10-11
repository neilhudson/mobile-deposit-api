def buildVersion = null
stage 'Build App'
node('docker') {
//    docker.withServer('tcp://docker.local:1234'){
            docker.image('kmadel/maven:3.3.3-jdk-8').inside('-v /data:/data') {
                sh 'rm -rf *'
                checkout([$class: 'GitSCM', branches: [[name: '*/master']], clean: true, doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'ab2d3ee0-76a0-4da3-a86d-7e2574a861bd', url: 'https://github.com/harniman/mobile-deposit-api.git']]])
                sh 'git checkout master'
                sh 'git config user.email "nigel@harniman.net"'
                sh 'git config user.name "nharniman"'
                sh 'git remote set-url origin git@github.com:harniman/mobile-deposit-api.git'
                sh "mkdir -p /data/mvn"
                writeFile file: 'settings.xml', text: "<settings><localRepository>/data/.m2repo</localRepository></settings>"

                sh 'mvn -s settings.xml clean package'
                


                stage 'Sonar analysis'
                //sh 'mvn -s settings.xml -Dsonar.scm.disabled=True sonar:sonar'
                echo "would run sonar here"
                
                stage 'Integration-test'
                sh 'mvn -s settings.xml  verify'

                step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])

                stage 'Prepare release'
                def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
                if (matcher) {
                    buildVersion = matcher[0][1]
                    echo "Releaed version ${buildVersion}"
                }
                matcher = null
            }

            docker.withServer('tcp://192.168.99.100:2376', 'slave-docker-us-east-1-tls'){

                stage 'Build Docker image'
                def mobileDepositApiImage
                dir('.docker') {
                    sh "ls -l ../target"
                    sh "mv ../target/*-SNAPSHOT.jar  mobile-deposit-api.jar"
                    sh "ls -l "
                    mobileDepositApiImage = docker.build "harniman/mobile-deposit-api:${buildVersion}"
                }
    
                stage 'Test Docker image'
                try{
                  sh "docker stop mobile-deposit-api"
                  sh "docker rm mobile-deposit-api"
                } catch (Exception _) {
                   echo "no container to stop"        
                }
                mobileDepositApiImage.run("--name mobile-deposit-api -p 8080:8080")
    
                stage 'Publish Docker image'
                withDockerRegistry(registry: [credentialsId: 'dockerhub-harniman']) {
                    mobileDepositApiImage.push()
                }
            }
//  }
}