# =============================================================================
# ms-digitalfix-bff
# =============================================================================
# Mismo patron que el microservicio de catalogo, a proposito: dos imagenes que
# se construyen y se operan igual son dos imagenes que nadie tiene que aprender
# por separado.
# =============================================================================

# ------------------------------------------------------------------ compilar
FROM maven:3.9-eclipse-temurin-17 AS construccion

WORKDIR /construccion

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ------------------------------------------------------------------ ejecutar
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S digitalfix && adduser -S digitalfix -G digitalfix

WORKDIR /app
COPY --from=construccion /construccion/target/*.jar app.jar
RUN chown -R digitalfix:digitalfix /app

USER digitalfix

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD wget -q -O - http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
