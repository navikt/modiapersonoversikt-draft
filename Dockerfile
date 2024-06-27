FROM gcr.io/distroless/java17-debian12
ENV JAVA_OPTS="${JAVA_OPTS} -Xms256m -Xmx512m"

USER nonroot

COPY build/libs/app.jar app.jar

CMD ["app.jar"]
