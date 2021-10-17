nano .ssh/authorized_keys
#added by OTO
ssh-rsa _key_ key-test
------
pscp -i .ssh/key-test.ppk project/yusuf-simple-rest/target/Yusuf-Simple-Rest-0.0.1-jar-with-dependencies.jar support@35.192.170.10:/home/support/yusuf-rest.jar

---service--
[Unit]
Description=Yusuf rest service
StartLimitBurst=5
StartLimitIntervalSec=0
[Service]
Type=simple
User=yusuf
ExecStart=/usr/bin/java -jar /opt/test/yusuf-rest.jar 80
SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5
[Install]
WantedBy=multi-user.target
----


---- DockerfileYusufRest ----
FROM openjdk:8-jdk-alpine

ARG JAR_FILE=yusuf-rest.jar

ENV LISTEN_PORT=8080

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","app.jar","${LISTEN_PORT}"]
---- DockerfileYusufRest ----


docker build -f DockerfileYusufRest -t yusuf-rest:latest .

docker run yusuf-rest:1.0 -e LISTEN_PORT=17777

gcloud auth activate-service-account yusuf-service-account@short-test-1.iam.gserviceaccount.com --key-file=short-test-1-e5eff0343d2e.json
gcloud auth activate-service-account yusuf-service-account@short-test-1.iam.gserviceaccount.com --key-file=short-test-1-a3c127b4b514.json

docker tag yusuf-rest:latest gcr.io/short-test-1/yusuf-rest:latest

docker push gcr.io/short-test-1/yusuf-rest


--rest-token----
docker build -t rest-token:latest .
docker tag rest-token:latest gcr.io/short-test-1/rest-token:latest
docker push gcr.io/short-test-1/rest-token


--- GOOGLE COMMAND ---  
gcloud run deploy oto-url-shortener --project oto-rest-api --source . --region europe-west1
gcloud run deploy oto-url-shortener --project oto-stage-api --source . --region europe-west1
