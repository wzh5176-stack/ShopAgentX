FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd --gid 10001 shopagent \
    && useradd --uid 10001 --gid shopagent --no-create-home shopagent

COPY --chown=shopagent:shopagent target/shopagent-backend-1.0-SNAPSHOT.jar app.jar

USER shopagent

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/app/app.jar"]