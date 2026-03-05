#!/bin/bash
set -e

CONTAINER_NAME="watch-dev-x86"

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Container '${CONTAINER_NAME}' is not running. Start it with ./run.sh first."
    exit 1
fi

if xhost &>/dev/null 2>&1; then
    xhost +local:root > /dev/null
fi

docker exec \
    -u "$(whoami)" \
    -e "DISPLAY=${DISPLAY:-:0}" \
    -it "${CONTAINER_NAME}" \
    bash
