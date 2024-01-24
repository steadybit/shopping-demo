#!/usr/bin/env bash

#
# Copyright 2024 steadybit GmbH. All rights reserved.
#

set -eo pipefail

tag="$1"
branch="$2"

cp ../shopping-ui/target/licenses/THIRD-PARTY-UI.csv ./target/licenses/THIRD-PARTY-UI.csv

docker buildx create --use
docker buildx build \
  --platform "linux/arm64,linux/amd64" \
  --push \
  -t "steadybit/gateway:$tag" \
  -t "steadybit/gateway:$branch" \
  --build-arg "JAR_FILE=target/*.jar" \
  .
