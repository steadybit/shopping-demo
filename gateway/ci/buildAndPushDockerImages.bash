#!/usr/bin/env bash

set -eo pipefail

tag="$1"

cp ../shopping-ui/target/licenses/THIRD-PARTY-UI.csv ./target/licenses/THIRD-PARTY-UI.csv

docker buildx create --use
docker buildx build \
  --platform "linux/arm64,linux/amd64" \
  --push=false \
  -t "steadybit/gateway:$tag" \
  --build-arg "JAR_FILE=target/*.jar" \
  .
