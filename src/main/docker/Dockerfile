FROM docker.io/amazoncorretto:17.0.6

ENV JAVA_MAX_MEM=1024m \
    JAVA_MAX_PERM_MEM=128m \
    SERVICE_8080_NAME=timetable-core \
    TZ=Europe/Stockholm

COPY application.yml ${APP_FOLDER}/
COPY @project.artifactId@-@project.version@.jar ${APP_FOLDER}/app.jar

RUN mkdir -p /app

# Copy the entrypoint script to the container
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]

EXPOSE 8080