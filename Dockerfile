FROM openjdk:11.0.7-jdk
ARG JAR_FILE=build/libs/taxi-0.0.1-SNAPSHOT.jar

# cd /opt/app
WORKDIR /opt/app

COPY ${JAR_FILE} app.jar
# java -jar /opt/app/app.jar
ENTRYPOINT ["java", "-jar","/opt/app/app.jar"]

