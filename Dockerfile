FROM navikt/java:11
COPY build/libs/*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback-remote.xml"
LABEL org.opencontainers.image.source=https://github.com/navikt/syfobrukertilgang
