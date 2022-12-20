FROM navikt/java:11-appdynamics

ENV APPD_ENABLED=true
ENV JAVA_OPTS="${JAVA_OPTS} -Xms256m -Xmx512m"
COPY java-debug.sh /init-scripts/08-java-debug.sh

COPY build/libs/app.jar app.jar
