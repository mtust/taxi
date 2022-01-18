FROM gradle:jdk11 as gradleimage
COPY . /home/gradle/source
WORKDIR /home/gradle/source
RUN ./gradlew build -x test

FROM openjdk:11-jre-slim
COPY --from=gradleimage /home/gradle/source/build/libs/taxi-0.0.1-SNAPSHOT.jar /app/
WORKDIR /app
ENTRYPOINT ["java", "-jar", "taxi-0.0.1-SNAPSHOT.jar"]

#FROM openjdk:11.0.7-jdk
#ARG JAR_FILE=build/libs/taxi-0.0.1-SNAPSHOT.jar
#
## cd /opt/app
#WORKDIR /opt/app
#
#COPY ${JAR_FILE} app.jar
## java -jar /opt/app/app.jar
#ENTRYPOINT ["java", "-jar","/opt/app/app.jar"]
