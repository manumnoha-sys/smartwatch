#!/bin/bash
set -e

CONTAINER_NAME="watch-dev-x86"
IMAGE_NAME="watch-dev-x86"
CONTAINER_USER="$(whoami)"
PROJECTS_DIR="${HOME}/watch-projects"

# Create persistent dirs on host if they don't exist
mkdir -p "${PROJECTS_DIR}"
mkdir -p "${HOME}/.android-docker"
mkdir -p "${HOME}/.gradle-docker"

# Allow local X11 connections
if xhost &>/dev/null 2>&1; then
    xhost +local:root > /dev/null
fi

# Remove old container if stopped
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    STATUS=$(docker inspect -f '{{.State.Status}}' "${CONTAINER_NAME}")
    if [ "${STATUS}" != "running" ]; then
        echo "Removing stopped container: ${CONTAINER_NAME}"
        docker rm "${CONTAINER_NAME}"
    else
        echo "Container '${CONTAINER_NAME}' is already running. Use into.sh to get a shell."
        exit 0
    fi
fi

echo "Starting container: ${CONTAINER_NAME}"

# KVM flags for Android emulator acceleration
KVM_FLAGS=()
if [ -e /dev/kvm ] && getent group kvm &>/dev/null; then
    KVM_GID=$(getent group kvm | cut -d: -f3)
    KVM_FLAGS=(--device /dev/kvm --group-add "${KVM_GID}")
    echo "KVM acceleration enabled (GID ${KVM_GID})"
else
    echo "KVM not available — emulator will run without hardware acceleration"
fi

docker run -d \
    --name "${CONTAINER_NAME}" \
    --network host \
    -e DISPLAY="${DISPLAY:-:0}" \
    -v /tmp/.X11-unix:/tmp/.X11-unix:rw \
    -v "${PROJECTS_DIR}:/home/${CONTAINER_USER}/projects" \
    -v "${HOME}/.android-docker:/home/${CONTAINER_USER}/.android" \
    -v "${HOME}/.gradle-docker:/home/${CONTAINER_USER}/.gradle" \
    "${KVM_FLAGS[@]}" \
    "${IMAGE_NAME}" \
    sleep infinity

echo ""
echo "Container '${CONTAINER_NAME}' started."
echo "  Shell:      bash infra/x86/into.sh"
echo "  IntelliJ:   docker exec -it ${CONTAINER_NAME} idea.sh"
