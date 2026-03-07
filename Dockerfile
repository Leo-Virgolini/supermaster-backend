# --- Stage 1: Build ---
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# Copiar archivos de Maven
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Dar permisos al wrapper
RUN chmod +x mvnw

# Descargar dependencias (cacheado si pom.xml no cambia)
RUN ./mvnw dependency:go-offline -B

# Copiar el codigo fuente
COPY src src

# Compilar sin tests
RUN ./mvnw package -DskipTests -B

# --- Stage 2: Runtime ---
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copiar el JAR compilado
COPY --from=build /app/target/*.jar app.jar

# Puerto de la aplicacion
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
