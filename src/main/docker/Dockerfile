FROM docker.io/amazoncorretto:25

ENV JAVA_MAX_MEM=1024m \
    JAVA_MAX_PERM_MEM=128m \
    SERVICE_8080_NAME=vb-back \
    TZ=Europe/Stockholm

# fabric8 docker-maven-plugin places the assembly under /maven inside the
# build context, so the fat JAR (with the bundled Vue SPA) is copied from there.
COPY maven/@project.artifactId@-@project.version@.jar /app.jar

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]

EXPOSE 8080
