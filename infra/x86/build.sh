#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

IMAGE_NAME="watch-dev-x86"
USER_NAME="$(whoami)"
USER_UID="$(id -u)"
USER_GID="$(id -g)"

echo "Building x86 dev image: ${IMAGE_NAME}"
echo "  User: ${USER_NAME} (${USER_UID}:${USER_GID})"

docker build \
    --build-arg USER_NAME="${USER_NAME}" \
    --build-arg USER_UID="${USER_UID}" \
    --build-arg USER_GID="${USER_GID}" \
    -t "${IMAGE_NAME}" \
    "${SCRIPT_DIR}"

echo "Build complete: ${IMAGE_NAME}"
