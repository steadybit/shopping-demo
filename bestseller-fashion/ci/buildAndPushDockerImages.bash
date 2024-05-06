#!/usr/bin/env bash

#
# Copyright 2024 steadybit GmbH. All rights reserved.
#

set -eo pipefail

tag="$1"
branch="$2"

docker buildx create --use
docker buildx build \
  --platform "linux/arm64,linux/amd64" \
  --push \
  -t "steadybit/bestseller-fashion:$tag" \
  -t "steadybit/bestseller-fashion:$branch" \
  --build-arg "JAR_FILE=target/*.jar" \
  .

if [ $branch = "main" ]; then
  echo "Snyk container monitor steadybit/bestseller-fashion:$branch"
  snyk container monitor "steadybit/bestseller-fashion:$branch"
fi
