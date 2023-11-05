#!/bin/bash

# Check if version argument is provided
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <version>"
    exit 1
fi

VERSION=$1

# Name of the Docker image
IMAGE_NAME="prasannjeet/vb-back"

# Create a new builder instance
docker buildx create --name mymultiarchbuilder --use

# Start up the builder instance
docker buildx use mymultiarchbuilder

# Inspect the builder instance to ensure it's correctly set up
docker buildx inspect --bootstrap

# Build and push the image for the desired platforms
docker buildx build --push \
  --platform linux/arm64/v8,linux/amd64 \
  --tag "${IMAGE_NAME}:${VERSION}" .

# Check if the build was successful
if [ $? -eq 0 ]; then
    echo "Build and push successful"
else
    echo "Build or push failed"
    exit 1
fi
