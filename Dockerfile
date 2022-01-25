FROM gradle:jdk11 as gradleimage
COPY . /home/gradle/source
WORKDIR /home/gradle/source
RUN ./gradlew build -x test

FROM openjdk:11-jre-slim

EXPOSE 8080
COPY --from=gradleimage /home/gradle/source/build/libs/*.jar app.jar
WORKDIR /opt/app
ENTRYPOINT ["java", "-jar", "/opt/app/app.jar"]

#FROM openjdk:11.0.7-jdk
#ARG JAR_FILE=build/libs/taxi-0.0.1-SNAPSHOT.jar
#
## cd /opt/app
#WORKDIR /opt/app
#
#COPY ${JAR_FILE} app.jar
## java -jar /opt/app/app.jar
#ENTRYPOINT ["java", "-jar","/opt/app/app.jar"]

