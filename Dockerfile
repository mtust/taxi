FROM gradle:jdk21 as gradleimage
COPY . /home/gradle/source
WORKDIR /home/gradle/source

ARG DB_URI
ARG DB_USER
ARG DB_PASSWORD
ARG TWILIO_SID
ARG TWILIO_TOKEN
ARG TWILIO_ID

ENV DB_URI=$DB_URI
ENV DB_PASSWORD=$DB_PASSWORD
ENV DB_USER=$DB_USER
ENV TWILIO_SID=$TWILIO_SID
ENV TWILIO_TOKEN=$TWILIO_TOKEN
ENV TWILIO_ID=$TWILIO_ID

RUN ./gradlew build -x test
FROM eclipse-temurin:21

EXPOSE 8080
#COPY --from=gradleimage /home/gradle/source/build/libs/*.jar app.jar
#WORKDIR /opt/app
#
#ENV PORT 8080
#ENV HOST 0.0.0.0
#ENTRYPOINT ["java", "-jar", "/opt/app/app.jar"]

COPY --from=gradleimage /home/gradle/source/build/libs/* /app.jar

# Run the web service on container startup.
CMD ["java", "-jar", "/app.jar"]

#FROM openjdk:11.0.7-jdk
#ARG JAR_FILE=build/libs/taxi-0.0.1-SNAPSHOT.jar
#
## cd /opt/app
#WORKDIR /opt/app
#
#COPY ${JAR_FILE} app.jar
## java -jar /opt/app/app.jar
#ENTRYPOINT ["java", "-jar","/opt/app/app.jar"]

