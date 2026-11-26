# Imagen de cualquiera de los tres servicios.
#   docker build --build-arg SERVICIO=emision -t brujula/emision .
# Etapa 1: compila con Maven. Etapa 2: solo el JRE y el jar, usuario sin privilegios.

FROM maven:3.9-eclipse-temurin-21 AS compilacion
ARG SERVICIO
WORKDIR /src
COPY pom.xml .
COPY servicios servicios
RUN mvn -B -q -pl servicios/${SERVICIO} -am -DskipTests package

FROM eclipse-temurin:21-jre
ARG SERVICIO
WORKDIR /app
RUN useradd --system --uid 10001 brujula
COPY --from=compilacion /src/servicios/${SERVICIO}/target/${SERVICIO}-1.0.0.jar app.jar
USER 10001
EXPOSE 8080
# MaxRAMPercentage: la JVM respeta el límite de memoria del contenedor (capítulo 6).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
