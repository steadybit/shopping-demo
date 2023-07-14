#!/usr/bin/env bash

set -eo pipefail

tag="$1"

docker buildx create --use
docker buildx build \
  --platform "linux/arm64,linux/amd64" \
  --push \
  -t "steadybit/bestseller-fashion:$tag" \
  --build-arg "JAR_FILE=target/*.jar" \
  .