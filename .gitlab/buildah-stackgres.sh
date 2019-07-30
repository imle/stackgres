#!/bin/bash -x

CONTAINER_BASE=$(buildah from "registry.access.redhat.com/ubi8-minimal:8.0")

# Maintainance tasks
buildah run --net "host" "$CONTAINER_BASE" -- microdnf update -y
buildah run --net "host" "$CONTAINER_BASE" -- microdnf install -y shadow-utils
buildah run "$CONTAINER_BASE" -- useradd stackgres
buildah run --net "host" "$CONTAINER_BASE" -- microdnf remove -y shadow-utils libsemanage
buildah run "$CONTAINER_BASE" -- microdnf clean all

# Include binaries
buildah config --workingdir='/app/' "$CONTAINER_BASE"
buildah copy --chown stackgres:stackgres "$CONTAINER_BASE" 'operator/target/*-runner' '/app/stackgres-operator'
buildah copy "$CONTAINER_BASE" 'operator/target/libsunec.so' '/app/libsunec.so'
buildah copy "$CONTAINER_BASE" 'operator/target/cacerts' '/app/cacerts'
buildah run "$CONTAINER_BASE" -- chmod 775 '/app'

## Run our server and expose the port
buildah config --cmd "./stackgres-operator -Dquarkus.http.host=0.0.0.0 -Djavax.net.ssl.trustStore=/app/cacerts" "$CONTAINER_BASE"
buildah config --port 8080 "$CONTAINER_BASE"
buildah config --user stackgres:stackgres "$CONTAINER_BASE"

## Commit this container to an image name
buildah commit --squash "$CONTAINER_BASE" "${1:-"stackgres/operator:test"}"