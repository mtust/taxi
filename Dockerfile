# Stage 1: Build Stage
FROM gradle:jdk21 as build
WORKDIR /home/gradle/source
COPY . .

# Build the application and pass necessary build arguments if needed
ARG DB_URI
ARG DB_USER
ARG DB_PASSWORD
ARG TWILIO_SID
ARG TWILIO_TOKEN
ARG TWILIO_ID

# Debugging: Ensure build arguments are received
RUN echo "DB_USER=$DB_USER DB_PASSWORD=$DB_PASSWORD"

RUN ./gradlew build -x test

# Stage 2: Runtime Stage
FROM openjdk:21
WORKDIR /opt/app

# Expose the application port
EXPOSE 8080

# Copy the built application from the build stage
COPY --from=build /home/gradle/source/build/libs/*.jar app.jar

# Set runtime environment variables
ENV DB_URI=${DB_URI}
ENV DB_USER=${DB_USER}
ENV DB_PASSWORD=${DB_PASSWORD}
ENV TWILIO_SID=${TWILIO_SID}
ENV TWILIO_TOKEN=${TWILIO_TOKEN}
ENV TWILIO_ID=${TWILIO_ID}

# Run the application
CMD ["java", "-jar", "app.jar"]
