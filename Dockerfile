# Use the official maven/Java 8 image to create a build artifact.
# https://hub.docker.com/_/maven
FROM maven:3.8-jdk-11 as builder

# Copy local code to the container image.
WORKDIR /app
COPY pom.xml .
COPY src ./src
# COPY oto-db.properties oto-db.properties

# Build a release artifact.
RUN mvn package -DskipTests

FROM adoptopenjdk/openjdk11:alpine-jre

# Copy the jar to the production image from the builder stage.
COPY --from=builder /app/target/shortener-0.0.1-jar-with-dependencies.jar /app.jar

ENV PORT 8080
ENV KEY test

# Run the web service on container startup.
CMD java -Djava.security.egd=file:/dev/./urandom -jar /app.jar ${PORT}
# CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar", "echo ${PORT}"]