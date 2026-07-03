# Étape 1 : build — la couche dependency:go-offline est mise en cache tant que le pom ne change pas
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw --batch-mode dependency:go-offline
COPY src src
RUN ./mvnw --batch-mode package -DskipTests -Dspotless.check.skip=true

# Étape 2 : image d'exécution minimale, utilisateur non-root
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S belay && adduser -S belay -G belay
USER belay
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
