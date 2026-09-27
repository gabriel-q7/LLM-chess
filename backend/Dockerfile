FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -q -B package -DskipTests && cp target/chess-*.jar /app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app.jar /app/app.jar
# /app/data holds the SQLite database; compose mounts the repository's data/ directory here.
RUN mkdir -p /app/data
ENV DATABASE_PATH=/app/data/chess.db
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
