FROM appdynamics/java-agent:21.1.1 as appdynamics
FROM navikt/java:11-appdynamics

ENV APPD_ENABLED=true
COPY --from=appdynamics /opt/appdynamics /opt/appdynamics
USER root
RUN chown -R apprunner /opt/appdynamics
USER apprunner

COPY build/libs/app.jar app.jar
