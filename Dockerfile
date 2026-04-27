ARG BASE_IMAGE=ghcr.io/ministryofjustice/hmpps-eclipse-temurin:25-jre-jammy
FROM --platform=$BUILDPLATFORM ${BASE_IMAGE} AS builder

ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}

WORKDIR /builder
COPY approved-premises-api-${BUILD_NUMBER}.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM ${BASE_IMAGE}

ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}

WORKDIR /app
COPY --chown=appuser:appgroup applicationinsights.json ./
COPY --chown=appuser:appgroup applicationinsights.nonprod.json ./
COPY --chown=appuser:appgroup applicationinsights-agent*.jar ./agent.jar

COPY --from=builder --chown=appuser:appgroup /builder/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /builder/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /builder/extracted/application/ ./

COPY --chown=appuser:appgroup /script/run_seed_job ./
COPY --chown=appuser:appgroup /script/run_seed_from_excel_job ./
COPY --chown=appuser:appgroup /script/run_seed_from_excel_directory_job ./
COPY --chown=appuser:appgroup /script/run_migration_job ./
COPY --chown=appuser:appgroup /script/hard_delete ./
COPY --chown=appuser:appgroup /script/clear_cache ./
RUN mkdir /tmp/seed && chown appuser:appgroup /tmp/seed && chmod +x /app/run_seed_job && chmod +x /app/run_seed_from_excel_job && chmod +x /app/run_seed_from_excel_directory_job && chmod +x /app/run_migration_job && chmod +x /app/hard_delete && chmod +x /app/clear_cache

ENTRYPOINT ["java", "-XX:+ExitOnOutOfMemoryError", "-XX:+AlwaysActAsServerClassMachine", "-javaagent:agent.jar", "-jar", "app.jar"]