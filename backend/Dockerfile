# Étape 1 : build (le wrapper télécharge Maven, ~/.m2 mis en cache par BuildKit)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw --batch-mode dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw --batch-mode package -DskipTests -Dspotless.check.skip=true

# Étape 2 : image d'exécution minimale, utilisateur non-root
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S belay && adduser -S belay -G belay
USER belay
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
