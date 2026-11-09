FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21
ENV JAVA_OPTS="-Xms256m -Xmx512m"

COPY build/install/*/lib /lib

ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.modiapersonoversikt.MainKt"]
