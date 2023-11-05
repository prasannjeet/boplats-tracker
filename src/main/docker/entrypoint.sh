#!/bin/bash

# Copy the configuration file from the NFS volume to an internal directory
cp /config/homeSearchConfig.json /app/homeSearchConfig.json

# Start the Java application
exec java -Djava.security.egd=file:/dev/./urandom -jar /app.jar
