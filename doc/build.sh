#!/bin/sh

set -e

rm -Rf "$(dirname "$0")/data/crds"
mkdir -p "$(dirname "$0")/data/crds"
find "$(dirname "$0")/../stackgres-k8s/install/helm/stackgres-operator/crds" -name '*.yaml' \
  | tr '\n' '\0' | xargs -0 -r -n 1 -I % cp % "$(dirname "$0")/data/crds"