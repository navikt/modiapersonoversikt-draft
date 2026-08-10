FROM gcr.io/distroless/java21-debian12
ENV JAVA_OPTS="${JAVA_OPTS} -Xms256m -Xmx512m"

USER nonroot

COPY build/install/*/lib /lib

ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.modiapersonoversikt.MainKt"]
